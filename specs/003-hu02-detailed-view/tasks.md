---
description: "Task list for feature 003-hu02-detailed-view"
---

# Tasks: HU-02 Detailed View

**Input**: Design documents from `/specs/003-hu02-detailed-view/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/,
quickstart.md

**Tests**: Required — constitution II (TDD). Every production change is preceded
by a RED task (failing test for a stated reason), then a GREEN task. The
shared-cache migration is a dedicated **REFACTOR** (no behaviour change for
list). Dependencies are added inside the first RED (or GREEN, when noted)
that needs them — no standalone dependency-only phase.

**Organization**: Grouped by HU-02 (P1) and cache reuse (same label). Labels
are **`[HU02]`** only — never `[USn]` (constitution X).

**Conventions (same as 002)**: sequential RED→GREEN pairs; ⏸ Review +
Conventional Commit message per task; no RED pushes; R6 module coverage
stated per task; `git status` before each task.

## Format: `[ID] [P?] [Label] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[HU02]**: story label from spec.md
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
   task that first needs them. **Forbidden in 003**: new Maven libraries
   (unless plan amendment), Spring Security, JWT, `@Cacheable` /
   `spring-boot-starter-cache`, frontend catalog UI, forking a second
   catalog adapter, exposing `fetchPokemonDetail` / `fetchSpeciesDetail` on
   the port.
3. Before marking a task `[x]`: `cd backend && ./mvnw -B verify` must pass
   (with `-Dmaven.compiler.showDeprecation=true`), except RED tasks, where
   the named test MUST fail for the stated reason.
4. Prefer Boot **4.1.1** BOM versions; springdoc **3.1.1**, WireMock
   **3.13.2**, caffeine via BOM **3.2.4** (re-check Central at implement
   time). No Spring 7 / Jackson 3 deprecated APIs.
5. **R6**: each module’s tests cover that module’s code. State the covering
   module in every task. Spring wiring stays under **`web/config/`**.
6. Once the draft PR exists (T003), every push must keep CI green (no RED
   pushes).

---

## Phase 1: Domain (HU-02) — pure Java, tested in `domain`

**Goal**: Height, flavor text, image fallback, stats+total, evolution tree,
catalog detail records + `toSummary()`, `PokemonDetail` assembly.

**Independent Test**: `./mvnw -B -pl domain test` green; no Spring on domain
test classpath. 002 list domain tests stay green.

- [x] T001 [HU02] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/HeightMTest.java`
  — catalog height `7` dm → `0.7` m (scale 1); reject negatives with
  `IllegalArgumentException`. Confirm `./mvnw -B -pl domain test` fails
  (type missing). Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect decimetres to metres (red)`

- [x] T002 [HU02] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/HeightM.java`.
  `./mvnw -B -pl domain test` passes. Coverage: **domain**.
  ⏸ Review — `feat(domain): convert decimetres to metres (green)`

- [x] T003 **User action**: push `003-hu02-detailed-view` and open a
  **draft** PR to `main` (`gh pr create --draft --base main`). Confirm CI
  runs green on the skeleton (or current branch tip). If red, fix before
  continuing. ⏸ Review

- [x] T004 [HU02] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/FlavorTextTest.java`
  and
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/ImageUrlTest.java`
  — first English flavor entry wins; normalise `\f`/`\n`/`\r`/runs of
  whitespace → single spaces + trim; empty after trim → `flavorText` null;
  no English → `flavorText` null. ImageUrl: prefer `officialArtworkSprite`,
  else
  `frontDefaultSprite`, else `null`. Confirm fail. Do not push RED.
  Coverage: **domain**.
  ⏸ Review — `test(domain): expect flavor text and image url rules (red)`

- [x] T005 [HU02] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/FlavorText.java`
  and
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/ImageUrl.java`.
  Tests pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): flavor text normalisation and image url (green)`

- [ ] T006 [HU02] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/PokemonStatsTest.java`
  — six bases ≥ 0; `total` = sum of the six (computed). Confirm fail. Do not
  push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect pokemon stats total (red)`

- [ ] T007 [HU02] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/PokemonStats.java`.
  Tests pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): pokemon stats with computed total (green)`

- [ ] T008 [HU02] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/EvolutionNodeTest.java`
  — tree from catalog chain + `CatalogEntry` lookups; edge metadata from
  **first** `evolution_details` only (empty list → null trigger / minLevel /
  itemSlug); `isCurrent == true` iff `nationalNumber == requestedId`;
  branching (Eevee-scale) and single-stage (no children). Confirm fail. Do
  not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect evolution tree and isCurrent (red)`

- [ ] T009 [HU02] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/EvolutionLink.java`,
  `EvolutionChild.java`, `EvolutionNode.java` (and catalog chain input
  records as needed under `domain/pokemon/`). Tests pass. Coverage:
  **domain**.
  ⏸ Review — `feat(domain): evolution tree with first detail edge (green)`

- [ ] T010 [HU02] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/CatalogPokemonDetailTest.java`
  (and/or `CatalogSpeciesDetailTest.java`, `PokemonDetailTest.java`) —
  `CatalogPokemonDetail.toSummary()` → 002 `CatalogPokemon` (sprite =
  `frontDefaultSprite` only); `CatalogSpeciesDetail.toSummary()` → 002
  `CatalogSpecies` (names + genera only); `CatalogDetailEntry` holds both;
  `PokemonDetail.assemble(CatalogDetailEntry, CatalogEvolutionChain,
  List<CatalogEntry>, requestedId)` wires identity, height/weight, stats,
  flavor, image, generation, evolution with `isCurrent`. Confirm fail. Do
  not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect detail records and pokemon detail assemble (red)`

- [ ] T011 [HU02] GREEN: implement
  `CatalogPokemonDetail.java`, `CatalogSpeciesDetail.java`,
  `CatalogDetailEntry.java`, `CatalogEvolutionChain` (if not already),
  `PokemonDetail.java` under
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/`.
  Keep 002 `CatalogPokemon` / `CatalogSpecies` / list tests green. Coverage:
  **domain**.
  ⏸ Review — `feat(domain): catalog detail records and pokemon detail (green)`

**Checkpoint**: Domain detail rules unit-tested; list types unchanged at the
public API.

---

## Phase 2: Application (HU-02) — use case + port, tested in `application`

**Goal**: `GetPokemonDetailUseCase` — exactly three port calls
(`fetchDetail` → `fetchEvolutionChain` → `fetchEntries`); not-found vs
unavailable.

**Independent Test**: `./mvnw -B -pl application test` with Mockito; no Spring.

- [ ] T012 [HU02] RED: write
  `backend/application/src/test/java/com/miguel/pokemonapp/application/pokemon/GetPokemonDetailUseCaseTest.java`
  — mock `PokemonCatalogPort`: verify **exactly three** calls in order
  `fetchDetail(id)` → `fetchEvolutionChain(chainId)` →
  `fetchEntries(chainIds)`; identity miss → `PokemonNotFoundException`; port
  failure → `CatalogUnavailableException`. Confirm fail (types missing). Do
  not push RED. Coverage: **application**.
  ⏸ Review — `test(application): expect get pokemon detail use case (red)`

- [ ] T013 [HU02] GREEN: extend
  `backend/application/src/main/java/com/miguel/pokemonapp/application/port/PokemonCatalogPort.java`
  with `fetchDetail(id)` → `CatalogDetailEntry` and
  `fetchEvolutionChain(chainId)` (do **not** add `fetchPokemonDetail` /
  `fetchSpeciesDetail`); add
  `PokemonNotFoundException.java`; implement
  `.../application/pokemon/GetPokemonDetailUseCase.java`. Also add temporary
  `UnsupportedOperationException` stubs for `fetchDetail` /
  `fetchEvolutionChain` on
  `backend/infrastructure/src/main/java/com/miguel/pokemonapp/infrastructure/pokeapi/PokeApiCatalogAdapter.java`
  so the whole reactor compiles. Verify with full `./mvnw -B verify` (CI must
  stay green); 002 `ListPokemonUseCaseTest` stays green. Coverage:
  **application**. Note: compile-only stubs in **infrastructure**, no
  behaviour (real impl in T016).
  ⏸ Review — `feat(application): get pokemon detail use case (green)`

**Checkpoint**: Use case green with mocked port (R6 for application).

---

## Phase 3: Shared-cache REFACTOR — before detail adapter work

**Goal**: One cache per upstream URL stores rich detail projections; list
path derives 002 summaries. **No** second cache for the same URL. Keep all
002 list/cache tests green (behaviour unchanged).

**Independent Test**: `./mvnw -B -pl infrastructure,application,domain test`
and existing WireMock list/cache tests still pass with the same request
counts/semantics.

- [ ] T014 [HU02] REFACTOR: in
  `backend/infrastructure/src/main/java/com/miguel/pokemonapp/infrastructure/pokeapi/PokeApiCatalogAdapter.java`
  (and tests under `.../infrastructure/pokeapi/`), change pokemon/species
  Caffeine caches to hold `CatalogPokemonDetail` / `CatalogSpeciesDetail`;
  `fetchEntries` / list assembly call `toSummary()` after cache hit or load.
  Leave the T013 `UnsupportedOperationException` stubs for `fetchDetail` /
  `fetchEvolutionChain` in place (real impl in T016). No new caches for the
  same URLs. `./mvnw -B verify` green; 002 list tests unchanged in
  behaviour. Coverage: **infrastructure**.
  ⏸ Review — `refactor(infrastructure): cache catalog detail projections for list (green)`

**Checkpoint**: Shared caches ready; list path still green; detail ops not
yet implemented.

---

## Phase 4: Infrastructure detail adapter (HU-02) — WireMock, no Spring

**Goal**: `fetchDetail` (parallel pokemon+species), `fetchEvolutionChain`
(+ chain cache), identity 404 → `PokemonNotFoundException`, phase (c) cache
reuse (exactly-once identity GETs).

**Independent Test**: `./mvnw -B -pl infrastructure test` green without Boot.

- [ ] T015 [HU02] RED: extend
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/pokeapi/PokeApiCatalogAdapterTest.java`
  (and/or a focused detail test class) — WireMock: happy-path
  `fetchDetail`; Eevee `fetchEvolutionChain` branching; identity upstream
  **404** → `PokemonNotFoundException` (not unavailable); evolution-chain
  cache hit on second call; for one `fetchDetail(id)` followed by
  `fetchEntries` including that id, assert `/pokemon/{id}` and
  `/pokemon-species/{id}` requested **exactly once** each; failures never
  cached. Confirm fail against stubs/missing methods. Do not push RED.
  Coverage: **infrastructure**.
  ⏸ Review — `test(infrastructure): expect fetchDetail and evolution chain (red)`

- [ ] T016 [HU02] GREEN: implement `fetchDetail` (adapter fetches pokemon +
  species **concurrently**, writes both caches) and `fetchEvolutionChain`
  (`getIfPresent` → load → `put`; new chain cache) in
  `PokeApiCatalogAdapter.java`. Tests pass; 002 list/cache tests stay green.
  Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): fetchDetail and evolution-chain cache (green)`

**Checkpoint**: Detail port ops proven with WireMock; one-cache-per-URL
asserted.

---

## Phase 5: Web API (HU-02) — controller then advice

**Goal**: `GET /api/v1/pokemon/{id}` per
`contracts/pokemon-detail-api.yaml`. Controller tests assert **status /
JSON shape**; Problem Detail bodies in the advice pair.

**Independent Test**: `@WebMvcTest` in **web** with mocked use case.

- [ ] T017 [HU02] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/pokemon/PokemonControllerTest.java`
  — `GET /api/v1/pokemon/{id}` happy path → **200** with detail JSON shape
  (identity, stats+total, flavorText, evolution/`isCurrent`, imageUrl);
  `id=0` → **400**. **Do not** assert `problem+json` body here. Also extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/OpenApiDocsIntegrationTest.java`
  to assert `/api/v1/pokemon/{id}` is documented with responses **200**,
  **400**, **404**, and **502**. Confirm fail. Do not push RED. Coverage:
  **web**.
  ⏸ Review — `test(web): expect pokemon detail status codes and openapi (red)`

- [ ] T018 [HU02] GREEN: implement detail mapping on
  `backend/web/src/main/java/com/miguel/pokemonapp/web/pokemon/PokemonController.java`,
  detail request/response DTOs under `.../web/pokemon/`, `@Min(1)` on path
  id, OpenAPI 200/400/404/502 annotations so
  `OpenApiDocsIntegrationTest` passes; register `GetPokemonDetailUseCase`
  bean in existing `web/config/` PokeAPI configuration. Controller + OpenAPI
  tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): GET /api/v1/pokemon/{id} controller (green)`

- [ ] T019 [HU02] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandlerTest.java`
  — `PokemonNotFoundException` → **404** `application/problem+json` (RFC
  7807 fields, no stack trace); re-add type-mismatch coverage for
  `/api/v1/pokemon/abc` → **400** problem+json; also assert `id=0` → **400**
  problem+json whose **detail names the `id` parameter**
  (`HandlerMethodValidationException` path), not just the status. Confirm
  fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect 404 and type-mismatch problem details (red)`

- [ ] T020 [HU02] GREEN: extend
  `backend/web/src/main/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandler.java`
  — map `PokemonNotFoundException` → 404; re-add `handleTypeMismatch` for
  path variables. Advice tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): 404 and type-mismatch problem advice (green)`

**Checkpoint**: Controller status/shape + advice problem+json green under
**web**.

---

## Phase 6: End-to-end IT (HU-02) — WireMock-standalone

**Goal**: Full-stack detail IT (Eevee, single-stage, 404, cache reuse).

**Independent Test**: `@SpringBootTest` + WireMock-standalone; existing
Postgres Testcontainers; `POKEAPI_BASE_URL` → WireMock.

- [ ] T021 [HU02] RED: write acceptance IT
  `backend/web/src/test/java/com/miguel/pokemonapp/web/pokemon/PokemonDetailIntegrationTest.java`
  — 200 for stubbed Bulbasaur/Eevee/single-stage shapes; **404** ProblemDetail
  on identity upstream 404; WireMock **5xx** on `/pokemon/{id}` → HTTP
  **502** problem+json on `GET /api/v1/pokemon/{id}` with **no upstream URL**
  in the body; assert `/pokemon/{id}` and `/pokemon-species/{id}` **exactly
  once** for a detail call that also needs phase (c); optional second
  identical detail → zero extra catalog hits. Confirm fail (or note if
  already green — see T022). Do not push RED. Coverage: **web** IT.
  ⏸ Review — `test(web): pokemon detail IT against wiremock (red)`

- [ ] T022 [HU02] GREEN: finish wiring/fixes so T021 passes (reuse
  `web/config/` beans). **Acceptance note**: if T021 already passes with no
  production changes, commit the IT as
  `test(web): pokemon detail IT against wiremock` and mark **T022 skipped**.
  Otherwise `./mvnw -B -pl web -am verify` green; 002
  `PokemonListIntegrationTest` stays green. Coverage: **web** IT.
  ⏸ Review — `feat(web): pokemon detail IT green` (or skipped)

**Checkpoint**: Detail e2e green; list IT unchanged.

---

## Phase 7: Polish

- [ ] T023 [HU02] Update root `README.md` — document
  `GET /api/v1/pokemon/{id}`, 400/404/502 cases, OpenAPI paths; no new env
  keys. Run every new README command before marking done. Coverage: N/A.
  ⏸ Review — `docs: document hu02 pokemon detail in readme`

- [ ] T024 [HU02] Run `specs/003-hu02-detailed-view/quickstart.md` end to end
  (or record deviations and fix). Record observed SC-003/SC-004 timings
  manually; confirm SC-001/SC-002/error behaviours via IT or live curls.
  Coverage: validation run.
  ⏸ Review — `test: validate 003 quickstart`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Domain (Phase 1)** → **Application (Phase 2)** → **Cache REFACTOR
  (Phase 3)** → **Detail adapter (Phase 4)** → **Web controller/advice
  (Phase 5)** → **E2E IT (Phase 6)** → **Polish (Phase 7)**
- **T003** (draft PR) follows the first GREEN (**T002**); then continue
  domain
- **T014** MUST complete before **T015/T016** (shared caches before detail
  ops)
- Advice (**T019/T020**) immediately after controller (**T017/T018**)

### Within phases

- Domain: strictly sequential RED→GREEN pairs (no batch of REDs)
- RED before GREEN for the same behaviour elsewhere
- RED states reviewed locally; not pushed
- One task → one review stop
- `git status` clean of prior uncommitted reviewed work before starting the
  next task

### Parallel Opportunities

- Limited: RED→GREEN pairs are sequential by design
- After T011, application and (later) web cannot start until their
  prerequisites finish; no meaningful multi-agent parallelism intended

---

## Implementation Strategy

### MVP ([HU02])

1. Phases 1–6 (detail API + IT), including T014 cache refactor.
2. README + quickstart (Phase 7).

### Full 003

1. Keep draft PR CI green after T003.
2. T024 quickstart (or IT equivalents) green.
3. 002 list tests remain green throughout.

---

## Notes

- **24 tasks** (T001–T024); every one ends in a review stop
- Labels **`[HU02]`** only — never `[USn]`
- Suggested commit messages follow Conventional Commits; the user commits
- Agent never commits or pushes; do not push RED
- Phase (a) parallelism is **inside** `fetchDetail` (adapter), not the use
  case
- One cache per URL; WireMock exactly-once identity GETs on detail
- Dependency injection map: none new expected; reuse wiremock /
  wiremock-standalone / caffeine / validation / springdoc from 002
