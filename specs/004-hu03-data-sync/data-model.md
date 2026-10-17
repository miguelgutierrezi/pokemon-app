# Data Model: HU-03 Data Synchronization

**Feature**: `004-hu03-data-sync` | **Date**: 2026-10-17

HTTP DTOs: [contracts/collection-api.yaml](./contracts/collection-api.yaml).
Persistence schema: Flyway V2/V3 (see below). Domain types below; JPA
entities are infrastructure-only and never leave the adapter.

Reuse from 002/003: `DisplayName`, `Category`, `HeightM`, `WeightKg`,
`ImageUrl`, `CatalogDetailEntry` / `CatalogPokemonDetail` /
`CatalogSpeciesDetail`, `Page` / `PageRequest`, `Require`.

## Domain

### Tags (value object)

Ordered for persistence as a set of normalised strings (iteration order
stable after normalisation — e.g. insertion order of the deduped set).

| Rule | Detail |
|------|--------|
| Normalise | trim → lowercase → drop blanks → deduplicate |
| Reject blank | empty after trim → invariant failure |
| Per-tag length | ≤ **32** characters (after trim, before or after lowercase — enforce on normalised form) |
| Max count | ≤ **20** tags per record |
| Empty | allowed (`Tags.empty()` / empty collection) — import starts empty |

### LocalPokemon (aggregate)

Snapshot of catalog identity/presentation at sync time + proprietary fields.

| Field | Type | Rules |
|-------|------|--------|
| `id` | long \| null | Surrogate; null until persisted; assigned by store |
| `nationalNumber` | int | ≥ 1; natural key |
| `name` | `DisplayName` | English display name from catalog |
| `category` | `Category` | English genus; may be empty/absent per 002 rules |
| `types` | list of type slugs | Slot order from catalog (slot 1, 2, …) |
| `heightM` | `HeightM` | Metres, one decimal |
| `weightKg` | `WeightKg` | Kilograms, one decimal |
| `imageUrl` | string \| null | `ImageUrl` preference (official artwork → front sprite → null) |
| `generation` | string | Species generation slug (e.g. `generation-i`) |
| `syncedAt` | `Instant` | Catalog sync timestamp (`Clock`) |
| `localizedName` | string \| null | Proprietary; null on import; length ≤ **100** when set |
| `location` | string \| null | Proprietary free text; null on import; length ≤ **200** when set |
| `tags` | `Tags` | Empty on import; seed may populate |

**Factory**: `LocalPokemon.snapshotOf(CatalogDetailEntry entry, Instant syncedAt)`
— builds snapshot fields from pokemon + species detail; proprietary fields
null/empty; `id` null.

**Invariants** (via `Require` / factory): national number ≥ 1; non-null
name/height/weight/syncedAt/tags; type list non-null (may be empty only if
catalog supplied none — normally 1–2); proprietary length limits when
non-null; tags via `Tags`.

## Application port

### LocalPokemonRepository

| Method | Behaviour |
|--------|-----------|
| `save(LocalPokemon)` | Persist new aggregate; return with id. Adapter implementation: short `@Transactional` + `saveAndFlush`; unique violation → `DuplicateLocalPokemonException` |
| `findByNationalNumber(int)` | Optional local record |
| `findPage(PageRequest)` | `Page<LocalPokemon>` ordered by national number ASC |
| `existsByNationalNumber(int)` | Required first step of import — if true → 409 **before** catalog `fetchDetail` |

### ImportPokemonUseCase flow

```text
existsByNationalNumber(n) == true  → DuplicateLocalPokemonException (no catalog)
fetchDetail(n)                     → catalog 404 / 502 as today
snapshotOf(entry, clock.instant()) → proprietary empty
save(local)                        → unique backstop if concurrent race
```

### Exceptions

| Type | HTTP (web) | When |
|------|------------|------|
| `DuplicateLocalPokemonException` | 409 | Second import / unique violation |
| `LocalPokemonNotFoundException` | 404 | Get/list miss — detail “not in the local collection” |
| `PokemonNotFoundException` | 404 | Catalog miss on import (existing) |
| `CatalogUnavailableException` | 502 | Catalog down/malformed (existing) |

## Persistence (Flyway)

### V2 — `pokemon_record`

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGSERIAL / BIGINT PK | Surrogate |
| `national_number` | INT NOT NULL UNIQUE | Natural key |
| `name` | VARCHAR | English display name |
| `category` | VARCHAR NULLABLE | |
| `height_m` | NUMERIC(4,1) | |
| `weight_kg` | NUMERIC(6,1) | |
| `image_url` | VARCHAR NULLABLE | |
| `generation` | VARCHAR NOT NULL | |
| `synced_at` | TIMESTAMPTZ NOT NULL | |
| `localized_name` | VARCHAR(100) NULLABLE | |
| `location` | VARCHAR(200) NULLABLE | |
| `created_at` | TIMESTAMPTZ NOT NULL | |
| `updated_at` | TIMESTAMPTZ NOT NULL | |

No `version` column in 004 (research R5).

### V2 — `pokemon_record_type`

| Column | Type | Notes |
|--------|------|-------|
| `record_id` | BIGINT FK → `pokemon_record(id)` ON DELETE CASCADE | |
| `slot` | INT NOT NULL | 1-based catalog slot order |
| `type` | VARCHAR NOT NULL | Type slug |
| PK | `(record_id, slot)` | |

### V2 — `pokemon_record_tag`

| Column | Type | Notes |
|--------|------|-------|
| `record_id` | BIGINT FK → `pokemon_record(id)` ON DELETE CASCADE | |
| `tag` | VARCHAR(32) NOT NULL | Normalised tag |
| UNIQUE | `(record_id, tag)` | |

### V3 — seed

Static inserts for national numbers **1, 2, 3** (Bulbasaur line) with
snapshot values and sample proprietary fields. Example tags:
`first-partner`, `field-notes`, `forest`. No catalog HTTP.

## State transitions

```text
[absent] --import success--> [persisted snapshot + empty proprietary]
[persisted] --import again--> 409 (no second row)
[persisted] --005 edit--> proprietary fields change (out of scope)
[persisted] --005 delete--> removed (+ CASCADE children)
```

No refresh-from-catalog transition in 004.

## Relationships

```text
LocalPokemon 1 -- * type slots (ordered)
LocalPokemon 1 -- * tags (set)
```

Catalog types (`CatalogDetailEntry`) are **sources** for the snapshot
factory only; they are not persisted as foreign keys to an external system.
