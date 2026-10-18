# Implementation Plan: HU-04 Local Data Modification

**Branch**: `005-hu04-local-update` | **Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-hu04-local-update/spec.md`

## Summary

Extend the 004 local **collection** API with **Update** (PUT proprietary
fields + optimistic `version`) and **Delete** (204), plus an **additive**
`version` field on every local-record response (import, list, get, update).
Reuse `LocalPokemon`, `Tags`, repository port, JPA adapter, ProblemDetail
handler, and web config — **extend, do not fork**.

Traceability: HU-04, TR-CORE, TR-ERRORS, TR-UNIT, TR-API-CRUD (Update +
Delete). Backend only; UI is 007.

## Technical Context

**Language/Version**: Java 21; Spring Boot **4.1.1** (project parent;
re-verify at implement time against Maven Central / current Spring docs;
compile with `-Dmaven.compiler.showDeprecation=true`; no deprecated Spring 7
/ Jackson 3 APIs)

**Primary Dependencies** (constitution IX — no new starters expected):

| Library | Version / source | Notes |
|---|---|---|
| Existing 004 stack | BOM 4.1.1 | webmvc, validation, data-jpa, Flyway, springdoc, Testcontainers, wiremock-standalone |
| Jackson 3 (via Boot) | annotations `com.fasterxml.jackson.annotation`; databind `tools.jackson.*` | See research R5 for `required` / unknown properties |

**Storage**: PostgreSQL via Flyway **V4** (`version BIGINT NOT NULL DEFAULT
0`). JPA `@Version` on entity. Types/tags cascade unchanged from V2.

**Testing**: JUnit 5, AssertJ, Mockito; domain unit tests; application use
cases with mocked ports; `@DataJpaTest` + Testcontainers (update round
trip, version++, stale → exception, **concurrent update with latches
forcing B-load-after-A-commit → one success + one
`StaleLocalPokemonException`**, OOLFE after mid-write delete → stale,
delete cascades); `@WebMvcTest` (PUT/DELETE + validation + unknown props
+ `"tags": null` → 400 + POST import unknown property → 400); `@SpringBootTest`
IT with Postgres (seed update, sequential same-version PUTs → 200 then
409 + first writer’s values, clear null/`[]`, 400s, 404, delete 204 +
re-import). HTTP **200/409** belong to web/IT; infra asserts exceptions.
JaCoCo ≥ 80% per module.

**Target Platform**: Same as 001–004 (Compose + CI).

**Project Type**: Backend API feature in existing monorepo.

**Performance Goals**: Successful update/delete ≤ **200 ms** locally
(no catalog HTTP on these paths) — SC-003.

**Constraints**:
- Clean Architecture + ArchUnit; JPA never in domain/application
- `LocalPokemon.withProprietary` — new instance; snapshot immutable;
  `version` is a domain `long` ≥ 0 (API token ≠ surrogate id)
- Update use case: find → 404; `expectedVersion ≠ current` →
  `StaleLocalPokemonException` **before** write; else save
- Adapter update (inside `TransactionTemplate`, after load): compare
  `entity.getVersion()` with domain `version` → mismatch
  `StaleLocalPokemonException` (do **not** rely on manually setting
  managed `@Version`); then apply proprietary fields + `updated_at`;
  `saveAndFlush`. `@Version` + OOLFE remain commit-time backstop →
  `StaleLocalPokemonException`. No `@Transactional` on adapter methods
- Two 409 types: `urn:problem-type:conflict` (duplicate import) vs
  `urn:problem-type:stale-version` (optimistic lock)
- Jackson / Boot: `@JsonProperty(required = true)` on all four update
  properties (missing ≠ explicit null for text — test both);
  `"tags": null` → **400** (must be an array; clear with `[]`);
  reject unknown / snapshot fields via
  `spring.jackson.deserialization.fail-on-unknown-properties=true`
  (Boot disables this by default — not “Jackson 3”); same flag makes
  **POST import** reject unknown properties (**004 behaviour change** —
  add a test). PokeAPI `JsonNode` parsing unaffected (research R5)
- Non-JSON PUT → **415** problem+json
- `updated_at` advances on success; `syncedAt` never changes on update
- Public write until 006 — see Complexity Tracking
- No new env keys

**Scale/Scope**: One collection resource (update + delete); seed rows
editable; concurrent same-version update covered by test.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture | Four modules; core free of Spring/JPA/HTTP | PASS — domain/application pure; JPA in infrastructure |
| II. TDD | Red → green → refactor | PASS — see TDD ordering below |
| III. Ports & Adapters | Extend `LocalPokemonRepository`; reuse JPA adapter | PASS |
| IV. API Contract | `/api/v1`, DTOs, ProblemDetail, springdoc | PASS |
| V. Two-Layer Validation | Bean Validation on PUT body / path; domain `withProprietary` / `Tags` | PASS |
| VI. Security | Writes protected by JWT | **VIOLATION — justified** — see Complexity Tracking (temporary until 006; same as 004) |
| VII. Frontend | Out of scope | N/A |
| VIII. Quality Gates | CI + JaCoCo per module | PASS |
| IX. Simplicity | No new Maven modules/starters; V4 column only | PASS |
| X. Traceability | Spec cites HU-04 + TR-*; Delete is TR-API-CRUD not a new story | PASS |

**Post-design re-check**: PASS with the recorded VI exception — research
R1–R8; contracts cover PUT/DELETE + additive `version` + two 409 types;
V4 migration; no new env keys.

## Project Structure

### Documentation (this feature)

```text
specs/005-hu04-local-update/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── collection-api.yaml
│   └── env-config.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── domain/src/{main,test}/java/com/miguel/pokemonapp/domain/pokemon/
│   └── LocalPokemon.java          # + version; withProprietary(+ tests)
├── application/src/{main,test}/java/com/miguel/pokemonapp/application/
│   ├── port/LocalPokemonRepository.java   # + deleteByNationalNumber
│   ├── pokemon/
│   │   ├── UpdateLocalPokemonUseCase.java
│   │   ├── DeleteLocalPokemonUseCase.java
│   │   └── ProprietaryUpdate.java         # command record (optional name)
│   └── StaleLocalPokemonException.java    # → 409 stale-version
├── infrastructure/
│   ├── src/main/java/.../persistence/     # @Version; update/delete mapping
│   └── src/main/resources/db/migration/
│       └── V4__pokemon_record_version.sql
├── web/src/{main,test}/java/com/miguel/pokemonapp/web/
│   ├── collection/       # PUT/DELETE; UpdateLocalPokemonRequest;
│   │                     # LocalPokemonResponse.version
│   ├── error/            # StaleLocalPokemonException → 409 stale-version;
│   │                     # unknown-property naming on 400
│   └── config/           # use-case beans; jackson fail-on-unknown yml
```

**Structure Decision**: Extend existing Clean Architecture modules and the
004 `collection` package; no new Maven modules. Frontend untouched.

## TDD ordering (visible for tasks)

Work red → green per slice (separate commits at implement time):

1. **Domain** — RED→GREEN: `LocalPokemon` gains `version` (≥ 0);
   `snapshotOf` → 0; `withProprietary` preserves snapshot + version;
   limits/normalisation unchanged; unit tests.
2. **Application** — RED→GREEN: `ProprietaryUpdate` +
   `UpdateLocalPokemonUseCase` (404 / stale before write / save);
   `DeleteLocalPokemonUseCase` (404 / delete); port
   `deleteByNationalNumber`; `StaleLocalPokemonException`.
3. **Infrastructure** — RED→GREEN: Flyway V4; entity `@Version`; adapter
   update: load → **in-tx** `entity.getVersion()` vs domain `version` →
   stale exception on mismatch → apply proprietary + `updated_at` →
   `saveAndFlush`; OOLFE → `StaleLocalPokemonException`; delete cascades;
   concurrent-update latch test (**B loads after A commits** → one
   success + one `StaleLocalPokemonException`); OOLFE path when the row
   is deleted after load then `save`. HTTP **200/409** are web/IT only.
4. **Web DTOs / controller / OpenAPI** — RED→GREEN: additive `version` on
   `LocalPokemonResponse` (breaks/fixes existing collection tests);
   `UpdateLocalPokemonRequest` with `@JsonProperty(required = true)` on
   all four (test omit vs explicit null for text; `"tags": null` → 400);
   unknown/snapshot props → 400; **POST import unknown property → 400**
   (global fail-on-unknown); PUT 200; DELETE 204; 415 non-JSON; OpenAPI.
   `@WebMvcTest` asserts the `version` returned by the **mocked** use
   case; real version +1 is asserted in infrastructure / e2e IT.
5. **Web advice** — RED→GREEN: `StaleLocalPokemonException` → 409
   `urn:problem-type:stale-version` + fixed detail; keep duplicate
   `urn:problem-type:conflict`; unknown-property 400 names field without
   Jackson leaks (`tools.jackson` / `com.fasterxml`).
6. **E2E IT** — Postgres: update seed Bulbasaur 200 version+1; two
   sequential PUTs with the same version → first **200**, second **409**
   stale-version and stored values match the first request (no lost
   update; real race stays in infra); PUT null/`[]` clears fields;
   read-only/unknown 400; `"tags": null` / limits 400; missing 404;
   delete 204 → GET 404 → re-import 201.
7. **Ops / polish** — README update/delete + version note; quickstart
   validation; no new env keys.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Constitution **VI** — `PUT` / `DELETE /api/v1/collection/{n}` are **public** and the store is **global** (no JWT / no per-user scope) | Feature ordering: HU-04 (005) ships before auth (006). Spec FR-013 documents this temporary exception (same as 004 FR-010). | Protecting writes now would pull TR-API-AUTH / user entity into 005, merging features and violating “one branch per feature” / YAGNI. **Follow-up (006)**: JWT on writes; scope records per user; assign HU-03 seed rows to the demo user. |
