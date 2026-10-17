---
description: "Task list for feature 004-hu03-data-sync"
---

# Tasks: HU-03 Data Synchronization

**Input**: Design documents from `/specs/004-hu03-data-sync/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/,
quickstart.md

**Tests**: Required — constitution II (TDD). Every production change is preceded
by a RED task (failing test for a stated reason), then a GREEN task.
Dependencies are added inside the first RED (or GREEN, when noted) that needs
them — no standalone dependency-only phase.

**Organization**: Grouped by HU-03 (import P1 + read/seed P2 as one feature
label). Labels are **`[HU03]`** only — never `[USn]` (constitution X).

**Conventions (same as 003)**: sequential RED→GREEN pairs; ⏸ Review +
Conventional Commit message per task; no RED pushes; R6 module coverage
stated per task; `git status` before each task.

## Format: `[ID] [P?] [Label] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[HU03]**: story label from spec.md
- **⏸ Review**: stop after the task; the user reviews and makes the commit.
  The suggested commit message is given; the agent never commits or pushes.
  RED states are not pushed.

## Path Conventions

- Backend: `backend/<module>/src/{main,test}/java/com/miguel/pokemonapp/...`
- Root: `README.md`

## Global rules for every task

1. **Before starting**: run `git status`. If the previous task’s reviewed
   changes are still uncommitted, **STOP** and ask the user to commit first.
2. Add only dependencies listed in plan.md / research.md, and only inside the
   task that first needs them. **Forbidden in 004**: Spring Security / JWT,
   frontend collection UI, refresh-of-snapshot API, `@Version` column,
   forking a second catalog adapter, calling `fetchEvolutionChain` /
   `fetchEntries` from import, holding a DB transaction during catalog HTTP,
   mismatched-schema startup failure test (not required — see T014).
3. Before marking a task `[x]`: `cd backend && ./mvnw -B verify` must pass
   (with `-Dmaven.compiler.showDeprecation=true`), except RED tasks, where
   the named test MUST fail for the stated reason. If a GREEN task changes a
   port (or other shared type) in a way that breaks compilation elsewhere,
   add **compile-only stubs** in the same task and verify with **full**
   `./mvnw -B verify` (lesson from 003 T013).
4. Prefer Boot **4.1.1** BOM versions; `spring-boot-starter-data-jpa` via BOM
   (re-check Central at implement time). No Spring 7 / Jackson 3 deprecated
   APIs.
5. **R6**: each module’s tests cover that module’s code. State the covering
   module in every task. Spring wiring stays under **`web/config/`**.
6. Once the draft PR exists (T003), every push must keep CI green (no RED
   pushes).

---

## Phase 1: Domain (HU-03) — pure Java, tested in `domain`

**Goal**: `Tags` normalisation/limits; `LocalPokemon` +
`snapshotOf(CatalogDetailEntry, Instant)`.

**Independent Test**: `./mvnw -B -pl domain test` green; no Spring on domain
test classpath. 002/003 domain tests stay green.

- [x] T001 [HU03] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/TagsTest.java`
  — trim / lowercase / dedupe; reject blank; each tag length ≤ **32**; at
  most **20** tags; empty set allowed. Confirm `./mvnw -B -pl domain test`
  fails (type missing). Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect tags normalisation and limits (red)`

- [x] T002 [HU03] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/Tags.java`.
  `./mvnw -B -pl domain test` passes. Coverage: **domain**.
  ⏸ Review — `feat(domain): tags value object (green)`

- [x] T003 **User action**: push `004-hu03-data-sync` and open a **draft** PR
  to `main` (`gh pr create --draft --base main`). Confirm CI runs green on
  the current branch tip. If red, fix before continuing. ⏸ Review

- [x] T004 [HU03] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/LocalPokemonTest.java`
  — `snapshotOf` maps catalog detail → national number, `DisplayName`,
  `Category`, types in slot order, `HeightM`, `WeightKg`, `ImageUrl`,
  generation, `syncedAt`; proprietary `localizedName` / `location` null,
  `tags` empty; no domain surrogate id (identity = `nationalNumber`);
  localized name ≤ **100**, location ≤ **200** when set (invariants).
  Confirm fail. Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect local pokemon snapshot (red)`

- [x] T005 [HU03] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/LocalPokemon.java`
  with `snapshotOf` + `Require` invariants. Tests pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): local pokemon aggregate (green)`

**Checkpoint**: Domain green; ready for application.

---

## Phase 2: Application (HU-03) — port + use cases, tested in `application`

**Goal**: `LocalPokemonRepository`; import order
`existsBy` → 409 (no catalog) → `fetchDetail` → `save` + `Clock`; list + get
local; exceptions.

**Independent Test**: `./mvnw -B -pl application test` with Mockito; no Spring.

- [x] T006 [HU03] RED: write
  `backend/application/src/test/java/com/miguel/pokemonapp/application/pokemon/ImportPokemonUseCaseTest.java`,
  `ListLocalPokemonUseCaseTest.java`,
  `GetLocalPokemonUseCaseTest.java`
  — Import: when `existsByNationalNumber` true →
  `DuplicateLocalPokemonException` and **zero** catalog interactions; else
  `fetchDetail(n)` only (never evolution/entries) → `snapshotOf` + `save`
  with fixed `Clock`; catalog miss → `PokemonNotFoundException`; unavailable
  → `CatalogUnavailableException`. List: `findPage(PageRequest)`; **page
  beyond last** (same rule as HU-01 / `Page.of`) → domain/application
  bad-request signal that web maps to **400**. Get: miss →
  `LocalPokemonNotFoundException`. Confirm fail (types missing). Do not
  push RED. Coverage: **application**.
  ⏸ Review — `test(application): expect local collection use cases (red)`

- [x] T007 [HU03] GREEN: add
  `backend/application/src/main/java/com/miguel/pokemonapp/application/port/LocalPokemonRepository.java`
  (`save`, `findByNationalNumber`, `findPage`, `existsByNationalNumber`);
  `DuplicateLocalPokemonException.java`;
  `LocalPokemonNotFoundException.java`;
  `ImportPokemonUseCase.java`;
  `ListLocalPokemonUseCase.java`;
  `GetLocalPokemonUseCase.java`.
  If anything fails to compile because of the new types, add compile-only
  stubs in the same task and verify with full `./mvnw -B verify`. 002/003
  tests stay green. Coverage: **application**.
  ⏸ Review — `feat(application): local collection use cases (green)`

**Checkpoint**: Use cases green with mocked ports (R6 for application).

---

## Phase 3: Infrastructure (HU-03) — Flyway + JPA adapter

**Goal**: `spring-boot-starter-data-jpa`; V2 schema; V3 seed; JPA entities +
Spring Data repo + adapter (`@Transactional` + `saveAndFlush` → unique →
`DuplicateLocalPokemonException`); `open-in-view=false` /
`ddl-auto=validate`; concurrent insert proves one row.

**Independent Test**: `./mvnw -B -pl infrastructure test` with
`@DataJpaTest` + Testcontainers + empty test `@SpringBootConfiguration`.

- [x] T008 [HU03] RED: add empty
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/InfrastructureTestApplication.java`
  (`@SpringBootConfiguration`) and write
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/persistence/LocalPokemonJpaAdapterTest.java`
  — save/find/page; **concurrent import**: two threads save the same
  national number → exactly **one** row and exactly one
  `DuplicateLocalPokemonException`; after migrations, seeded national
  numbers **1/2/3** present (will fail until V3). Add test deps only as
  needed for the failing compile/test setup
  (`spring-boot-starter-data-jpa`, Testcontainers, etc. — BOM 4.1.1). Confirm
  fail for stated reasons. Do not push RED. Coverage: **infrastructure**.
  ⏸ Review — `test(infrastructure): expect jpa local pokemon adapter (red)`

- [x] T009 [HU03] GREEN: add
  `spring-boot-starter-data-jpa` to
  `backend/infrastructure/pom.xml`; Flyway
  `backend/infrastructure/src/main/resources/db/migration/V2__pokemon_record.sql`
  (`pokemon_record` surrogate id + unique `national_number` + snapshot +
  proprietary + timestamps; `pokemon_record_type` / `pokemon_record_tag`
  with ON DELETE CASCADE; **no** `version` column) and
  `V3__seed_local_collection.sql` with the **exact** seed rows from
  `data-model.md` V3 table (never `favorites`; static, no network); JPA
  entities + Spring Data repository + `LocalPokemonJpaAdapter` implementing
  the port — `save` is short `@Transactional` and uses `saveAndFlush`,
  translating unique violation → `DuplicateLocalPokemonException`; in
  `@DataJpaTest`, construct the adapter with the injected Spring Data repo
  (no separate Spring bean required in the slice). Set in
  `backend/web/src/main/resources/application.yml`:
  `spring.jpa.open-in-view=false` and
  `spring.jpa.hibernate.ddl-auto=validate`. Do **not** add
  `@EnableJpaRepositories` or `@EntityScan` — `PokemonApplication` is in the
  root package so Boot already scans entities and Spring Data repositories
  under `infrastructure`. Full `./mvnw -B verify` green (002/003 ITs still
  pass). Coverage: **infrastructure** (+ yml in **web** resources).
  ⏸ Review — `feat(infrastructure): jpa local collection persistence (green)`

**Checkpoint**: Persistence + seed + concurrent uniqueness proven in
infrastructure.

---

## Phase 4: Web API (HU-03) — controller then advice

**Goal**: Collection routes per `contracts/collection-api.yaml`; Clock bean;
409 + local 404 advice.

**Independent Test**: `@WebMvcTest` in **web** with mocked use cases.

- [x] T010 [HU03] RED: write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/collection/CollectionControllerTest.java`
  — POST import → **201** + `Location: /api/v1/collection/{n}` + body shape;
  GET list pagination defaults; GET one → **200**; invalid body/`nationalNumber`
  → **400** (status only here). Extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/OpenApiDocsIntegrationTest.java`
  for collection ops documenting **200/201/400/404/409/502** as applicable.
  Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect collection controller and openapi (red)`

- [x] T011 [HU03] GREEN: implement
  `backend/web/src/main/java/com/miguel/pokemonapp/web/collection/CollectionController.java`
  + request/DTOs under `.../web/collection/`; Bean Validation; OpenAPI
  annotations; under `web/config/` register `Clock`, collection use-case
  beans, and the JPA adapter as a `@Bean` implementing
  `LocalPokemonRepository` (same pattern as `PokeApiConfiguration` — inject
  the Spring Data repository into the adapter constructor). Do **not** add
  `@EnableJpaRepositories` or `@EntityScan`. Controller + OpenAPI tests
  pass. Coverage: **web**.
  ⏸ Review — `feat(web): collection controller (green)`

- [x] T012 [HU03] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandlerTest.java`
  — `DuplicateLocalPokemonException` → **409** problem+json;
  `LocalPokemonNotFoundException` → **404** problem+json whose detail
  clearly means not in the **local** collection (distinct from catalog
  not-found). Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect 409 and local 404 problem details (red)`

- [x] T013 [HU03] GREEN: extend
  `backend/web/src/main/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandler.java`
  for the two new exceptions. Advice tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): 409 and local 404 problem advice (green)`

**Checkpoint**: Controller status/shape + advice problem+json green under
**web**.

---

## Phase 5: End-to-end IT (HU-03) — WireMock-standalone + Postgres

**Goal**: Full-stack collection IT; assert JPA config (no separate
ddl-mismatch startup test).

**Independent Test**: `@SpringBootTest` + WireMock-standalone; existing
Postgres Testcontainers; `POKEAPI_BASE_URL` → WireMock.

- [x] T014 [HU03] RED: write acceptance IT
  `backend/web/src/test/java/com/miguel/pokemonapp/web/collection/CollectionIntegrationTest.java`
  — seed list includes 1/2/3; import stubbed catalog id → **201** + Location;
  duplicate → **409** and one row; get → **200**; get missing → **404** local
  wording; catalog identity 404 → **404** and nothing saved; catalog 5xx →
  **502** problem+json with **no upstream URL** and nothing saved; list
  **page beyond last** → **400** problem+json; assert
  `spring.jpa.open-in-view` is **false** (e.g. via
  `@SpringBootTest` / `Environment` or equivalent) and that the IT run’s
  captured startup logs contain **no** Spring JPA open-in-view WARN (do
  **not** add a mismatched entity/schema startup-failure test). Confirm
  fail (or note if already green — see T015). Do not push RED. Coverage:
  **web** IT.
  ⏸ Review — `test(web): collection IT against wiremock (red)`

- [x] T015 [HU03] GREEN: **skipped** — T014 passed with no production changes
  (commit IT as `test(web): collection IT against wiremock` when committing).
  ⏸ Review — skipped

**Checkpoint**: Collection e2e green; prior ITs unchanged.

---

## Phase 6: Polish

- [x] T016 [HU03] Update root `README.md` — document
  `POST/GET /api/v1/collection`, 400/404/409/502, temporary public write
  until 006, seed after compose; no new env keys. Run every new README
  command before marking done. Coverage: N/A.
  ⏸ Review — `docs: document hu03 local collection in readme`

- [x] T017 [HU03] Run `specs/004-hu03-data-sync/quickstart.md` end to end
  (or record deviations and fix). Confirm SC-001–SC-005 via IT and/or live
  curls. Coverage: validation run.
  ⏸ Review — `test: validate 004 quickstart`
  — Recorded in `quickstart.md` (real PokeAPI, 2026-10-17).


---

## Dependencies & Execution Order

### Phase Dependencies

- **Domain (Phase 1)** → **Application (Phase 2)** → **Infrastructure
  (Phase 3)** → **Web controller/advice (Phase 4)** → **E2E IT (Phase 5)**
  → **Polish (Phase 6)**
- **T003** (draft PR) follows the first GREEN (**T002**); then continue
  domain
- Advice (**T012/T013**) immediately after controller (**T010/T011**)

### Within phases

- Domain: strictly sequential RED→GREEN pairs (no batch of REDs)
- RED before GREEN for the same behaviour elsewhere
- RED states reviewed locally; not pushed
- One task → one review stop
- `git status` clean of prior uncommitted reviewed work before starting the
  next task

### Parallel Opportunities

- Limited: RED→GREEN pairs are sequential by design
- No meaningful multi-agent parallelism intended

---

## Implementation Strategy

### MVP ([HU03])

1. Phases 1–5 (domain → application → JPA → web → IT).
2. README + quickstart (Phase 6).

### Full 004

1. Keep draft PR CI green after T003.
2. T017 quickstart (or IT equivalents) green.
3. 002/003 tests remain green throughout.
4. Constitution VI public write remains until 006 (documented in README).

---

## Notes

- **17 tasks** (T001–T017); every one ends in a review stop
- Labels **`[HU03]`** only — never `[USn]`
- Suggested commit messages follow Conventional Commits; the user commits
- Agent never commits or pushes; do not push RED
- Import order: `existsBy` → 409 without catalog; then `fetchDetail`; then
  short transactional `saveAndFlush`
- Unique constraint is the concurrent backstop (infrastructure concurrent
  test)
- No `version` column; no ddl-mismatch startup test — assert
  `open-in-view=false` + no JPA WARN in the e2e IT
- Dependency injection map: first use of `spring-boot-starter-data-jpa` (+
  Testcontainers test stack) in T008/T009
