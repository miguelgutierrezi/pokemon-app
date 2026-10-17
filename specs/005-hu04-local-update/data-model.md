# Data Model: HU-04 Local Data Modification

**Feature**: `005-hu04-local-update` | **Date**: 2026-10-17

Extends [004 data-model](../004-hu03-data-sync/data-model.md). HTTP:
[contracts/collection-api.yaml](./contracts/collection-api.yaml).

## Domain (deltas)

### Tags

Unchanged from 004 (normalise, max 20, ≤ 32 chars, blank rejected).

### LocalPokemon

| Field | Change |
|-------|--------|
| `version` | **New** `long` ≥ 0 — API concurrency token (not the surrogate id) |
| Snapshot fields | Unchanged; immutable via update path |
| Proprietary fields | Updated only through `withProprietary` |

**Factories / methods**:

| Method | Behaviour |
|--------|-----------|
| `snapshotOf(entry, syncedAt)` | Proprietary empty; **`version = 0`** |
| `withProprietary(localizedName, location, tags)` | New instance; copy snapshot + **same** `version`; replace proprietary fields using existing `optionalText` / `Tags` rules |

After a successful JPA flush, the adapter returns the aggregate with the
**incremented** `version` from `@Version`.

## Application

### ProprietaryUpdate (command)

| Field | Type | Notes |
|-------|------|-------|
| `localizedName` | String \| null | Explicit null clears |
| `location` | String \| null | Explicit null clears |
| `tags` | List / Tags input | JSON **array** required; `[]` clears; `"tags": null` → **400** (not clear) |
| `expectedVersion` | long | Must equal current `version` |

### LocalPokemonRepository (deltas)

| Method | Behaviour |
|--------|-----------|
| `save(LocalPokemon)` | Insert **or** update; on update see adapter in-tx version compare; `saveAndFlush`; OOLFE → `StaleLocalPokemonException` |
| `deleteByNationalNumber(int)` | Delete row; cascades types/tags |
| `findByNationalNumber` / `findPage` / `exists…` | Unchanged; returned aggregates include `version` |

### Use-case flows

**Update**

```text
find(n) empty                    → LocalPokemonNotFoundException
expectedVersion ≠ current.version → StaleLocalPokemonException
save(current.withProprietary(...))
```

**Delete**

```text
absent(n) → LocalPokemonNotFoundException
deleteByNationalNumber(n)
```

### Exceptions

| Type | HTTP | `type` | Detail |
|------|------|--------|--------|
| `LocalPokemonNotFoundException` | 404 | `urn:problem-type:not-found` | `Pokémon {n} is not in the local collection` (004) |
| `StaleLocalPokemonException` | 409 | `urn:problem-type:stale-version` | `Pokémon {n} was modified by another request; reload and retry` |
| `DuplicateLocalPokemonException` | 409 | `urn:problem-type:conflict` | unchanged (004) |

## Persistence (Flyway V4)

```sql
ALTER TABLE pokemon_record
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

| Column | Type | Notes |
|--------|------|-------|
| `version` | BIGINT NOT NULL DEFAULT 0 | JPA `@Version`; seed/existing rows → 0 |

No changes to type/tag tables (cascade already in V2).

### Adapter mapping

- Load/save map `version` ↔ entity `@Version` (for reads / insert
  starting at 0). **Do not** assign the domain `version` onto a managed
  entity’s `@Version` field expecting it to drive the WHERE clause — that
  has no effect.
- **Update** (inside `TransactionTemplate`):
  1. Load entity by national number (absent → not-found at port/use-case
     layer as today).
  2. If `entity.getVersion() != domain.version()` →
     `StaleLocalPokemonException` (**closes lost-update hole** between
     application pre-check and flush).
  3. Apply proprietary columns + `updated_at = clock.instant()`; **do not**
     change `synced_at`.
  4. `saveAndFlush`; Hibernate increments `@Version`.
  5. Catch `ObjectOptimisticLockingFailureException` →
     `StaleLocalPokemonException` (commit-time backstop: concurrent writer
     or row deleted mid-write).
- Delete: `delete` by national number (or load + delete); types/tags removed
  via FK `ON DELETE CASCADE` / `orphanRemoval`.
- Concurrent test: force **B loads after A commits** (latches) → one success
  + one stale-version failure.

## HTTP DTO shape (additive)

`LocalPokemon` JSON gains required `version` (integer ≥ 0) on POST import,
GET list items, GET one, and PUT responses — **additive** vs 004 contract.
