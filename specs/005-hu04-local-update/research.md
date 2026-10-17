# Research: HU-04 Local Data Modification

**Feature**: `005-hu04-local-update` | **Date**: 2026-10-17

All Technical Context items resolved. Extend 004; do not fork modules or
collection resource.

## R1 — Domain: `withProprietary` + `version` on `LocalPokemon`

**Decision**: Add `long version` (≥ 0) to `LocalPokemon` (API concurrency
token; **not** the surrogate DB id). Factory
`withProprietary(String localizedName, String location, Tags tags)` returns a
**new** instance copying all snapshot fields and the current `version`,
replacing only the three proprietary fields (reuse existing
`optionalText` / `Tags` normalisation). `snapshotOf` initialises
`version = 0`.

**Rationale**: Spec requires clients to read `version` on every collection
response; domain carries the token so use cases can compare
`expectedVersion` without leaking JPA. Surrogate `id` stays persistence-only
(004 correction).

**Alternatives considered**: Keep version only on the JPA entity — rejected
(clients/API/use case would need a parallel DTO or re-fetch semantics).
`@Version` as the only check without an application pre-check — rejected
(spec wants clear 404 vs stale-version and an explicit expected version in
the body).

## R2 — Application: update / delete use cases

**Decision**:

```text
UpdateLocalPokemonUseCase(n, ProprietaryUpdate{localizedName, location, tags, expectedVersion}):
  find(n) → empty → LocalPokemonNotFoundException
  if expectedVersion ≠ current.version → StaleLocalPokemonException  // before write
  save(current.withProprietary(...))  // adapter increments version

DeleteLocalPokemonUseCase(n):
  find(n) / exists → absent → LocalPokemonNotFoundException
  delete(n)
```

Port additions: `deleteByNationalNumber(int)` (and `save` already updates).
`ProprietaryUpdate` is an application-level command object (or record)
carrying the four inputs.

**Rationale**: Matches brief; application-level stale check gives a clean
409 before write; JPA `@Version` remains the concurrent backstop.

**Alternatives considered**: Single “upsert” use case — rejected (delete is
separate TR-API-CRUD). Rely only on DB optimistic lock without body
`version` — rejected by spec.

## R3 — Two distinct 409 Problem Detail types

**Decision**:

| Case | Exception | HTTP | `type` | Detail (exact string in contracts) |
|------|-----------|------|--------|-------------------------------------|
| Duplicate import (004) | `DuplicateLocalPokemonException` | 409 | `urn:problem-type:conflict` | `Pokémon {n} is already in the local collection` |
| Stale / lost update | `StaleLocalPokemonException` | 409 | `urn:problem-type:stale-version` | `Pokémon {n} was modified by another request; reload and retry` |

**Rationale**: Spec FR-008 / review feedback — clients must distinguish by
`type` and detail.

## R4 — Infrastructure: Flyway V4 + in-tx version check + `@Version` / OOLFE

**Decision**: Flyway **V4** adds `version BIGINT NOT NULL DEFAULT 0` to
`pokemon_record` (existing/seed rows become 0). Entity field annotated
`@Version`. Adapter update/delete use `TransactionTemplate` +
`saveAndFlush` (no `@Transactional` on adapter methods — 004 correction).

**Lost-update hole (must close)**: `@Version` only guards against the
version Hibernate loaded **in that transaction**. Manually setting a
managed entity's `@Version` field from the domain object has **no effect**.
Therefore the adapter update MUST, **inside its transaction after loading
the entity**, compare `entity.getVersion()` with the domain aggregate's
`version` and throw `StaleLocalPokemonException` on mismatch **before**
applying proprietary fields. Then apply proprietary columns +
`updated_at`; let Hibernate increment `@Version` on flush.

`ObjectOptimisticLockingFailureException` (concurrent writer **or** row
deleted mid-write) → `StaleLocalPokemonException` remains the
**commit-time backstop**. Delete uses repository `delete` with existing
`ON DELETE CASCADE` + `orphanRemoval` for types/tags.

**Concurrent-update test**: latch ordering MUST force **B loads after A
commits** (not merely overlapping saves), then assert **one 200 + one
409** stale-version.

**Rationale**: Application-level `expectedVersion` check alone can race
(both readers see v, both pass, both write). In-tx compare closes that
hole; `@Version`/OOLFE still catch true concurrent flushes.

**Alternatives considered**: Rely only on setting `@Version` from the
domain object — **rejected** (no effect on managed entities). Manual
`UPDATE … WHERE version = ?` without `@Version` — rejected (Boot-native
`@Version` + explicit compare is clearer).

## R5 — Required properties vs explicit null; unknown properties (Boot default)

**Decision** (verified against Jackson 3 annotations + Spring Boot 4
Jackson defaults):

1. **Missing vs null (text fields)**: On a Java `record` (creator
   properties), `@JsonProperty(required = true)` (package
   `com.fasterxml.jackson.annotation`) fails when the property is
   **absent**. An **explicit `null`** is not “missing” and binds as
   `null` — required for clearable `localizedName` / `location`.
   **Tests MUST cover** omitted field → 400 and `"localizedName": null`
   → accepted (then cleared).
2. **`tags`: null → 400**: `tags` MUST be a JSON **array**. Explicit
   `"tags": null` is malformed (not “clear tags” — clearing uses `[]`).
   Cover with a dedicated test → **400** naming `tags`.
3. **Unknown / read-only snapshot fields**: It is **Spring Boot** (not
   Jackson 3 itself) that disables
   `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` by default.
   `@JsonIgnoreProperties(ignoreUnknown = false)` alone does **not** force
   failure when the feature is off. Enable
   `spring.jackson.deserialization.fail-on-unknown-properties=true` in
   `application.yml`. Unknown names including snapshot fields
   (`name`, `types`, …) on PUT → 400 naming the property (no Jackson
   package leaks in the body).
4. **004 behaviour change**: The same global flag makes
   `POST /api/v1/collection` reject unknown properties. Document and
   **add a test** (e.g. body with an extra field → 400). **PokeAPI
   parsing is unaffected** (adapter uses `JsonNode` via its own
   `RestClient`, not MVC request DTOs).
5. Re-verify at implement time against Boot 4.1.1 docs; keep deprecation
   warnings on.

**Alternatives considered**: Per-DTO custom deserializer — YAGNI.
`@JsonAnySetter` rejector on update only — workable but global flag +
explicit POST test is simpler given the small JSON surface.

## R6 — Web: 415, Bean Validation, response `version`

**Decision**: PUT `consumes = APPLICATION_JSON` so non-JSON → **415**
(Spring default / advice if needed for problem+json shape). Bean
Validation on request mirrors domain constants
(`MAX_LOCALIZED_NAME_LENGTH`, `MAX_LOCATION_LENGTH`, `Tags.MAX_*`);
`tags` non-null / array (null → 400 per R5). `LocalPokemonResponse` gains
`version` (all collection endpoints). Register
`UpdateLocalPokemonUseCase` / `DeleteLocalPokemonUseCase` beans in
`CollectionConfiguration` (004 U1 pattern).

**Rationale**: Spec FR-010 / FR-004 additive contract.

## R7 — TDD ordering (visible)

**Decision**: Domain → application → infrastructure (V4 + adapter) → web
controller/DTOs → advice (stale-version) → e2e IT → polish. Each slice
RED→GREEN with separate commits at implement time (constitution II).

**Rationale**: Same cadence as 002–004.

## R8 — Constitution VI exception

**Decision**: Same temporary public write / global store exception as 004;
record in plan Complexity Tracking; 006 removes it.

**Rationale**: Spec FR-013; feature order before auth.
