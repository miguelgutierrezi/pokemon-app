---
description: "Task list for feature 005-hu04-local-update"
---

# Tasks: HU-04 Local Data Modification

**Input**: Design documents from `/specs/005-hu04-local-update/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/,
quickstart.md

**Tests**: Required — constitution II (TDD). Every production change is preceded
by a RED task (failing test for a stated reason), then a GREEN task.
Dependencies are added inside the first RED (or GREEN, when noted) that needs
them — no standalone dependency-only phase.

**Organization**: Grouped by HU-04 (update P1 + delete TR-API-CRUD as one
feature label). Labels are **`[HU04]`** only — never `[USn]` (constitution X).

**Conventions (same as 004)**: sequential RED→GREEN pairs; ⏸ Review +
Conventional Commit message per task; no RED pushes; R6 module coverage
stated per task; `git status` before each task.

## Format: `[ID] [P?] [Label] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[HU04]**: story label from spec.md
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
   task that first needs them. **Forbidden in 005**: Spring Security / JWT,
   frontend collection UI, re-sync/refresh-of-snapshot API, forking a second
   catalog or collection adapter, calling catalog HTTP from update/delete,
   `@Transactional` on adapter methods (use `TransactionTemplate` only),
   relying on manually setting a managed entity’s `@Version` (must compare
   in-tx after load — research R4), new Maven starters/modules.
3. Before marking a task `[x]`: `cd backend && ./mvnw -B verify` must pass
   (with `-Dmaven.compiler.showDeprecation=true`), except RED tasks, where
   the named test MUST fail for the stated reason. If a GREEN task changes a
   port (or other shared type) in a way that breaks compilation elsewhere,
   add **compile-only stubs** in the same task and verify with **full**
   `./mvnw -B verify` (lesson from 003 T013 / 004 T007).
4. Prefer Boot **4.1.1** BOM versions; no Spring 7 / Jackson 3 deprecated
   APIs. It is **Spring Boot** (not Jackson 3) that disables
   `FAIL_ON_UNKNOWN_PROPERTIES` by default — enable via
   `spring.jackson.deserialization.fail-on-unknown-properties=true`.
5. **R6**: each module’s tests cover that module’s code. State the covering
   module in every task. Spring wiring stays under **`web/config/`**.
6. Once the draft PR exists (T003), every push must keep CI green (no RED
   pushes).

---

## Phase 1: Domain (HU-04) — `version` + `withProprietary`

**Goal**: `LocalPokemon.version` (≥ 0); `snapshotOf` → 0;
`withProprietary` preserves snapshot + version; proprietary limits unchanged.

**Independent Test**: `./mvnw -B -pl domain test` green; no Spring on domain
test classpath. 002–004 domain tests stay green.

- [x] T001 [HU04] RED: extend
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/LocalPokemonTest.java`
  — `snapshotOf` → `version == 0`; `withProprietary` returns a new instance
  with updated proprietary fields (trim / blank→null / `Tags` rules),
  unchanged snapshot fields and **same** `version`; invariant
  **`version ≥ 0`** enforced on the record (construction / factory rejects
  negatives). Confirm fail. Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect local pokemon version and withProprietary (red)`

- [x] T002 [HU04] GREEN: extend
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/LocalPokemon.java`
  with `version`, `snapshotOf` → 0, `withProprietary(...)`. Tests pass.
  Coverage: **domain**.
  ⏸ Review — `feat(domain): local pokemon version and withProprietary (green)`

- [x] T003 **User action**: push `005-hu04-local-update` and open a **draft**
  PR to `main` (`gh pr create --draft --base main`). Confirm CI runs green
  on the current branch tip. If red, fix before continuing. ⏸ Review

**Checkpoint**: Domain green; draft PR open; ready for application.

---

## Phase 2: Application (HU-04) — update / delete use cases

**Goal**: `ProprietaryUpdate`; `UpdateLocalPokemonUseCase` (404 / stale
before write / save); `DeleteLocalPokemonUseCase` (404 / delete); port
`deleteByNationalNumber`; `StaleLocalPokemonException`.

**Independent Test**: `./mvnw -B -pl application test` with Mockito; no Spring.

- [x] T004 [HU04] RED: write
  `backend/application/src/test/java/com/miguel/pokemonapp/application/pokemon/UpdateLocalPokemonUseCaseTest.java`
  and
  `DeleteLocalPokemonUseCaseTest.java`
  — Update: miss → `LocalPokemonNotFoundException` and **zero** `save`;
  `expectedVersion ≠ current.version` → `StaleLocalPokemonException` and
  **zero** `save`; match → `save(withProprietary(...))` once. Delete: miss
  → `LocalPokemonNotFoundException`; present → `deleteByNationalNumber(n)`
  once. Confirm fail (types missing). Do not push RED. Coverage:
  **application**.
  ⏸ Review — `test(application): expect update and delete local pokemon (red)`

- [x] T005 [HU04] GREEN: add
  `backend/application/src/main/java/com/miguel/pokemonapp/application/pokemon/ProprietaryUpdate.java`,
  `UpdateLocalPokemonUseCase.java`,
  `DeleteLocalPokemonUseCase.java`,
  `StaleLocalPokemonException.java`;
  extend
  `backend/application/src/main/java/com/miguel/pokemonapp/application/port/LocalPokemonRepository.java`
  with `deleteByNationalNumber(int)`. If the port change breaks compilation
  elsewhere, add **compile-only stubs** in the same task and verify with
  full `./mvnw -B verify`. Coverage: **application**.
  ⏸ Review — `feat(application): update and delete local pokemon use cases (green)`

**Checkpoint**: Use cases green with mocked ports (R6 for application).

---

## Phase 3: Infrastructure (HU-04) — V4 + in-tx version compare

**Goal**: Flyway V4 `version`; entity `@Version`; adapter update: load →
**in-tx** `entity.getVersion()` vs domain `version` →
`StaleLocalPokemonException` on mismatch → apply proprietary + `updated_at`
→ `saveAndFlush`; OOLFE → stale; delete cascades; concurrent latch test.

**Independent Test**: `./mvnw -B -pl infrastructure test` with `@DataJpaTest`
+ Testcontainers.

- [x] T006 [HU04] RED: extend
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/persistence/LocalPokemonJpaAdapterTest.java`
  — after V4, loaded aggregates expose `version` (seed → 0); update round
  trip increments `version` and advances `updated_at` without changing
  `syncedAt`; **in-tx stale**: domain aggregate with wrong `version` →
  `StaleLocalPokemonException` (do not rely on setting managed `@Version`);
  **OOLFE path**: load the record (as the use case would), delete the row,
  then `save` → `StaleLocalPokemonException`; **concurrent update**: latch
  ordering forces **B loads after A commits**, then assert exactly **one**
  success and exactly **one** `StaleLocalPokemonException` (real race —
  research R4; HTTP 200/409 are web/IT only); delete removes row +
  types/tags (cascade). Confirm fail for stated reasons. Do not push RED.
  Coverage: **infrastructure**.
  ⏸ Review — `test(infrastructure): expect versioned local pokemon adapter (red)`

- [x] T007 [HU04] GREEN: add Flyway
  `backend/infrastructure/src/main/resources/db/migration/V4__pokemon_record_version.sql`
  (`version BIGINT NOT NULL DEFAULT 0`); annotate entity `@Version`; extend
  `LocalPokemonJpaAdapter` — update path uses `TransactionTemplate`, after
  load compares `entity.getVersion()` with domain `version` and throws
  `StaleLocalPokemonException` on mismatch **before** applying fields;
  map `ObjectOptimisticLockingFailureException` →
  `StaleLocalPokemonException`; implement `deleteByNationalNumber`. Full
  `./mvnw -B verify` green. Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): optimistic version on local pokemon (green)`

**Checkpoint**: In-tx compare + concurrent B-after-A proven in
infrastructure.

---

## Phase 4: Web API (HU-04) — controller then advice

**Goal**: PUT/DELETE per `contracts/collection-api.yaml`; additive
`version` on all local-record responses (update existing 004 collection
tests in the same task); fail-on-unknown (incl. POST import); `"tags": null`
→ 400; stale-version 409 advice.

**Independent Test**: `@WebMvcTest` in **web** with mocked use cases.

- [ ] T008 [HU04] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/collection/CollectionControllerTest.java`
  — PUT → **200** + body whose `version` matches what the **mocked** use
  case returns (real +1 is asserted in T006/T012); DELETE → **204**;
  missing required property / omit vs explicit null for text; **`"tags": null`
  → 400** (must be an array; clear with `[]`); unknown/snapshot props on
  PUT → **400**; **POST import with unknown property → 400** (global
  fail-on-unknown — 004 behaviour change); non-JSON PUT → **415**; path
  validation **400**. In the **same task**, update existing 004 collection
  tests (controller + any response assertions) for the **additive**
  `version` field on import/list/get bodies. Extend
  `OpenApiDocsIntegrationTest` for PUT/DELETE + `version` on schemas.
  Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect collection update delete and version (red)`

- [ ] T009 [HU04] GREEN: implement PUT/DELETE on
  `CollectionController` + `UpdateLocalPokemonRequest` /
  `LocalPokemonResponse.version`;
  `@JsonProperty(required = true)` on all four update properties; set
  `spring.jackson.deserialization.fail-on-unknown-properties=true` in
  `backend/web/src/main/resources/application.yml` (Boot disables this by
  default — not “Jackson 3”); register update/delete use-case beans under
  `web/config/`. Controller + OpenAPI + updated 004 assertions pass.
  Coverage: **web**.
  ⏸ Review — `feat(web): collection update delete and response version (green)`

- [ ] T010 [HU04] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandlerTest.java`
  — `StaleLocalPokemonException` → **409** problem+json with
  `type` `urn:problem-type:stale-version` and detail
  `Pokémon {n} was modified by another request; reload and retry`;
  duplicate import still `urn:problem-type:conflict`; unknown-property /
  malformed-body 400 names the field and must not leak `tools.jackson` /
  `com.fasterxml`. Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect stale-version 409 problem detail (red)`

- [ ] T011 [HU04] GREEN: extend
  `backend/web/src/main/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandler.java`
  for `StaleLocalPokemonException`. Advice tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): stale-version 409 problem advice (green)`

**Checkpoint**: Controller status/shape + two distinct 409s green under
**web**.

---

## Phase 5: End-to-end IT (HU-04) — Postgres acceptance

**Goal**: Full-stack collection IT for update/delete + version + 400/409/404.

**Independent Test**: `@SpringBootTest` + existing Postgres Testcontainers;
WireMock only where re-import needs catalog.

- [ ] T012 [HU04] RED: write acceptance IT coverage in
  `backend/web/src/test/java/com/miguel/pokemonapp/web/collection/CollectionIntegrationTest.java`
  (extend existing) — update seed Bulbasaur → **200**, `version` +1,
  snapshot unchanged; **two sequential PUTs with the same `version`** →
  first **200**, second **409** `urn:problem-type:stale-version`, and
  stored proprietary values match the **first** request (no lost update;
  real concurrent race stays in T006); PUT with `localizedName` /
  `location` **null** and `tags` **`[]`** clears those fields in the DB;
  read-only/unknown field → **400**; `"tags": null` → **400**; limits →
  **400**; missing → **404** local wording; DELETE → **204** then GET
  **404** then re-import **201**. Confirm fail (or note if already green
  — see T013). Do not push RED. Coverage: **web** IT.
  ⏸ Review — `test(web): collection update delete IT (red)`

- [ ] T013 [HU04] GREEN: finish wiring/fixes so T012 passes.
  **Acceptance note**: if T012 already passes with no production changes,
  commit the IT as `test(web): collection update delete IT` and mark
  **T013 skipped**. Otherwise full `./mvnw -B verify` green; prior ITs stay
  green. Coverage: **web** IT.
  ⏸ Review — `feat(web): collection update delete IT green` (or skipped)

**Checkpoint**: Collection update/delete e2e green; prior ITs unchanged.

---

## Phase 6: Polish

- [ ] T014 [HU04] Update root `README.md` — document
  `PUT` / `DELETE /api/v1/collection/{n}`, additive `version` on collection
  responses, 400/404/409 (`conflict` vs `stale-version`)/415, temporary
  public write until 006; no new env keys. Run every new README command
  before marking done. Coverage: N/A.
  ⏸ Review — `docs: document hu04 local update delete in readme`

- [ ] T015 [HU04] Run `specs/005-hu04-local-update/quickstart.md` end to end
  (or record deviations and fix). Confirm SC-001–SC-004 via IT and/or live
  curls; **SC-003** (≤ 200 ms) is a **manual** measurement recorded in
  `quickstart.md` (no automated latency assert). Coverage: validation run.
  ⏸ Review — `test: validate 005 quickstart`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Domain (Phase 1)** → **Application (Phase 2)** → **Infrastructure
  (Phase 3)** → **Web controller/advice (Phase 4)** → **E2E IT (Phase 5)**
  → **Polish (Phase 6)**
- **T003** (draft PR) follows the first GREEN (**T002**); then continue
  application
- Advice (**T010/T011**) immediately after controller (**T008/T009**)

### Within phases

- Strictly sequential RED→GREEN pairs (no batch of REDs)
- RED states reviewed locally; not pushed
- One task → one review stop
- `git status` clean of prior uncommitted reviewed work before starting the
  next task

### Parallel Opportunities

- Limited: RED→GREEN pairs are sequential by design
- No meaningful multi-agent parallelism intended

---

## Implementation Strategy

### MVP ([HU04])

1. Phases 1–5 (domain → application → JPA → web → IT).
2. README + quickstart (Phase 6).

### Full 005

1. Keep draft PR CI green after T003.
2. T015 quickstart (or IT equivalents) green.
3. 002–004 tests remain green throughout.
4. Constitution VI public write remains until 006 (documented in README).

---

## Notes

- **15 tasks** (T001–T015); every one ends in a review stop
- Labels **`[HU04]`** only — never `[USn]`
- Suggested commit messages follow Conventional Commits; the user commits
- Agent never commits or pushes; do not push RED
- Adapter update: in-tx `entity.getVersion()` vs domain version (R4); OOLFE
  is commit-time backstop only
- Concurrent infra test (T006): **B loads after A commits** → one success
  + one `StaleLocalPokemonException`; client-level no-lost-update in T012
  via two sequential same-version PUTs (200 then 409)
- Additive `version` on import/list/get: update existing 004 web tests in
  **T008** (same task as new PUT/DELETE expectations); T008 asserts mocked
  `version`, T006/T012 assert real +1
- Global fail-on-unknown also rejects unknown props on **POST import**
  (test in T008); PokeAPI `JsonNode` parsing unaffected
- `"tags": null` → **400**; clear tags with `[]`
- Two 409 types: `urn:problem-type:conflict` (import) vs
  `urn:problem-type:stale-version` (optimistic lock)
