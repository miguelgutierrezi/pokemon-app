# Implementation Plan: HU-03 Data Synchronization

**Branch**: `004-hu03-data-sync` | **Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-hu03-data-sync/spec.md`

## Summary

Deliver the local Pokémon **collection** API: import (sync) a catalog
Pokémon into PostgreSQL (`POST /api/v1/collection`), list and get local
records (`GET /api/v1/collection`, `GET /api/v1/collection/{nationalNumber}`),
with empty proprietary fields ready for HU-04, Flyway schema + seed
(DEL-SEED), and RFC 7807 errors including **409** on duplicate. Reuse
002/003 domain helpers, `PokemonCatalogPort.fetchDetail`, Caffeine caches,
ProblemDetail advice, and web config — **extend, do not fork**.

Traceability: HU-03, TR-DB, TR-DATA, DEL-SEED, TR-ERRORS, TR-UNIT,
TR-API-CRUD (Create + Read). Backend only; UI is 007.

## Technical Context

**Language/Version**: Java 21; Spring Boot **4.1.1** (project parent;
re-verify at implement time against Maven Central / current Spring docs)

**Primary Dependencies** (constitution IX — introduce only what this feature
uses):

| Library | Version / source | Notes |
|---|---|---|
| Existing 002/003 stack | BOM 4.1.1 / pinned in parent | webmvc, validation, caffeine, springdoc, wiremock-standalone, RestClient, Flyway, JDBC |
| `spring-boot-starter-data-jpa` | BOM 4.1.1 | **Verified** on Maven Central for Boot 4.1.1 (`org.springframework.boot:spring-boot-starter-data-jpa`); add to **infrastructure** (and transitively available to web tests). Re-check at implement time. |

If implement time discovers a required dependency, justify in a plan amendment
and introduce it in the first consumer task only.

**Storage**: PostgreSQL via Flyway **V2** (schema) + **V3** (seed). JPA
entities only inside infrastructure. Surrogate PK + unique `national_number`.

**Testing**: JUnit 5, AssertJ, Mockito (application); `@DataJpaTest` +
Testcontainers + empty `@SpringBootConfiguration` in
`infrastructure/src/test` (adapter + concurrent unique-constraint);
`@WebMvcTest` + mocked use cases (web); `@SpringBootTest` IT with
WireMock-standalone + real Postgres (import/list/get/seed/errors). JaCoCo
≥ 80% **per module**; existing `report-aggregate` unchanged.

**Target Platform**: Same as 001–003 (Compose + CI).

**Project Type**: Backend API feature in existing monorepo.

**Performance Goals**: First uncached import ≤ **3 s** when PokeAPI is
healthy (spec SC-001). Catalog HTTP must **not** run inside a DB
transaction.

**Constraints**:
- Clean Architecture + ArchUnit (existing rules); JPA never in
  domain/application
- Import uses **only** `catalog.fetchDetail(n)` (003 phase a) — no
  evolution chain
- Import order: `existsByNationalNumber(n)` → 409 **without** catalog call;
  then `fetchDetail(n)`; then short transactional adapter `save`
  (`saveAndFlush`); unique constraint remains the concurrent backstop →
  `DuplicateLocalPokemonException`
- JPA config: `spring.jpa.open-in-view=false`,
  `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema)
- `java.time.Clock` for `syncedAt` (testable)
- Public write route until 006 — see Complexity Tracking
- No new env keys (reuse Postgres + `POKEAPI_BASE_URL`)
- Seed tags: lowercase examples `first-partner`, `field-notes`, `forest`
  (never `favorites`); 006 will assign seed rows to the demo user

**Scale/Scope**: One collection resource (create + read); small seed set
(Bulbasaur line); concurrent duplicate import covered by test.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture | Four modules; core free of Spring/JPA/HTTP | PASS — domain/application pure; JPA in infrastructure |
| II. TDD | Red → green → refactor | PASS — see TDD ordering below |
| III. Ports & Adapters | `LocalPokemonRepository` port + JPA adapter; reuse catalog port | PASS |
| IV. API Contract | `/api/v1`, DTOs, ProblemDetail, springdoc | PASS |
| V. Two-Layer Validation | Bean Validation on import body / path; domain `Require` + `Tags` | PASS |
| VI. Security | Writes protected by JWT | **VIOLATION — justified** — see Complexity Tracking (temporary until 006) |
| VII. Frontend | Out of scope | N/A |
| VIII. Quality Gates | CI + JaCoCo per module | PASS |
| IX. Simplicity | JPA starter enters here (deferred from 001); no `@Version` yet | PASS — see research R5 |
| X. Traceability | Spec cites HU-03 etc.; no invented stories | PASS |

**Post-design re-check**: PASS with the recorded VI exception — research
R1–R8 + R3b; contracts cover collection API; V2/V3 migrations; no new env
keys.

## Project Structure

### Documentation (this feature)

```text
specs/004-hu03-data-sync/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── collection-api.yaml
│   └── env-config.md
└── tasks.md             # /speckit-tasks — not created by /speckit-plan
```

### Source Code (repository root)

```text
backend/
├── domain/src/{main,test}/java/com/miguel/pokemonapp/domain/
│   └── pokemon/          # + LocalPokemon, Tags (+ tests)
├── application/src/{main,test}/java/com/miguel/pokemonapp/application/
│   ├── port/LocalPokemonRepository.java
│   ├── pokemon/
│   │   ├── ImportPokemonUseCase.java
│   │   ├── ListLocalPokemonUseCase.java
│   │   └── GetLocalPokemonUseCase.java
│   ├── DuplicateLocalPokemonException.java   # → 409
│   └── LocalPokemonNotFoundException.java    # → 404 local
├── infrastructure/
│   ├── src/main/java/.../persistence/        # JPA entities, Spring Data
│   │                                         # repo, LocalPokemonJpaAdapter
│   ├── src/main/resources/db/migration/
│   │   ├── V2__pokemon_record.sql
│   │   └── V3__seed_local_collection.sql
│   └── src/test/java/.../                    # empty @SpringBootConfiguration
│                                             # + @DataJpaTest adapter tests
├── web/src/{main,test}/java/com/miguel/pokemonapp/web/
│   ├── collection/       # CollectionController + DTOs / request
│   ├── error/            # + 409 + LocalPokemonNotFoundException
│   └── config/           # Clock bean; register use-case beans
```

**Structure Decision**: Extend existing Clean Architecture modules; no new
Maven modules. Frontend untouched. Spring wiring remains under `web/config/`
(+ infrastructure component scan already used by the app).

## TDD ordering (visible for tasks)

Work red → green per slice (separate commits at implement time):

1. **Domain** — sequential RED→GREEN: `Tags` (normalise + limits) →
   `LocalPokemon` invariants + `LocalPokemon.snapshotOf(CatalogDetailEntry,
   Instant)` reusing `DisplayName` / `Category` / `HeightM` / `WeightKg` /
   `ImageUrl` / type ordering from 002/003.
2. **Application** — RED→GREEN: `LocalPokemonRepository` port;
   `ImportPokemonUseCase` order: `existsByNationalNumber` → 409 (no
   catalog); else `fetchDetail` only + `Clock` → `save`; catalog
   not-found / unavailable propagate; `ListLocalPokemonUseCase`;
   `GetLocalPokemonUseCase` → `LocalPokemonNotFoundException`.
3. **Infrastructure schema + JPA** — RED→GREEN: add
   `spring-boot-starter-data-jpa`; Flyway V2 tables; entities + Spring Data
   repo; adapter `save` is `@Transactional` and uses `saveAndFlush`,
   translating unique-violation → `DuplicateLocalPokemonException`;
   `application.yml` `open-in-view=false` + `ddl-auto=validate`;
   `@DataJpaTest` + Testcontainers + empty test `@SpringBootConfiguration`;
   **concurrent insert** proves one row.
4. **Seed** — Flyway V3 static seed (Bulbasaur/Ivysaur/Venusaur; tags
   `first-partner` / `field-notes` / `forest`); assert via adapter or IT.
5. **Web controller / DTOs / OpenAPI** — RED→GREEN: `CollectionController`
   (POST 201 + Location, GET list, GET one); Bean Validation; OpenAPI
   200/201/400/404/409/502; `Clock` bean.
6. **Web advice** — RED→GREEN: map
   `DuplicateLocalPokemonException` → 409;
   `LocalPokemonNotFoundException` → 404 with distinct detail.
7. **E2E IT** — WireMock catalog + real Postgres: import 201, duplicate 409,
   get 200, list, seed present, catalog 404 → 404, catalog 500 → 502 and
   nothing saved.
8. **Ops / polish** — README collection section; quickstart validation; no
   new env keys.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Constitution **VI** — `POST /api/v1/collection` is **public** and the store is **global** (no JWT / no per-user scope) | Feature ordering: HU-03 (004) ships before auth (006). Spec FR-010 documents this temporary exception. | Protecting writes now would pull TR-API-AUTH / user entity into 004, merging features and violating “one branch per feature” / YAGNI. **Follow-up (006)**: JWT on writes; scope records per user; assign HU-03 seed rows to the demo user seeded with mock credentials. |
