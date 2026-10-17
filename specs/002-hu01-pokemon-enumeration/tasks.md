---
description: "Task list for feature 002-hu01-pokemon-enumeration"
---

# Tasks: HU-01 Pokemon Enumeration

**Input**: Design documents from `/specs/002-hu01-pokemon-enumeration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/,
quickstart.md

**Tests**: Required — constitution II (TDD). Every production change is preceded
by a RED task (failing test for a stated reason), then a GREEN task. Refactors
are separate tasks. Dependencies are added inside the first RED (or GREEN, when
noted) that needs them — no standalone dependency-only phase.

**Organization**: Grouped by HU-01 (P1) and HU-01 nice-to-have cache (P2).
Labels are **`[HU01]`** and **`[HU01-cache]`** only — never `[USn]`
(constitution X).

**Revision 2 (2026-10-16)**: Dependency tasks folded into first consumer;
domain is strictly sequential RED→GREEN; springdoc spike is RED/GREEN IT;
controller RED is status/shape only; advice owns problem+json; PokeAPI Spring
wiring lives in `web/config/` and was pulled forward into **T021** (existing
`@SpringBootTest` ITs as RED); e2e list IT remains T024/T025; draft PR after
first GREEN.

## Format: `[ID] [P?] [Label] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[HU01]** / **[HU01-cache]**: story label from spec.md
- **⏸ Review**: stop after the task; the user reviews and makes the commit.
  The suggested commit message is given; the agent never commits or pushes.
  RED states are not pushed.

## Path Conventions

- Backend: `backend/<module>/src/{main,test}/java/com/miguel/pokemonapp/...`
- Root: `.env.example`, `docker-compose.yml`, `README.md`

## Global rules for every task

1. Add only dependencies listed in plan.md / research.md, and only inside the
   task that first needs them. **Forbidden in 002**: Spring Security, JWT,
   data-jpa, `@Cacheable` / `spring-boot-starter-cache`, TanStack Query,
   React Router, shadcn/ui, frontend catalog UI.
2. Before marking a task `[x]`: `cd backend && ./mvnw -B verify` must pass,
   except RED tasks, where the named test MUST fail for the stated reason.
3. Prefer Boot **4.1.1** BOM versions; pin springdoc **3.1.1**, WireMock
   **3.13.2**, caffeine via BOM **3.2.4** (re-check Central at implement time).
4. **R6**: each module’s tests cover that module’s code. State the covering
   module in every task. Spring `@Configuration` / `@ConfigurationProperties`
   for PokeAPI live under **`web`** so the web IT covers them.
5. Once the draft PR exists (T003), every push must keep CI green (no RED
   pushes).

---

## Phase 1: Domain (HU-01) — pure Java, tested in `domain`

**Goal**: Weight, English name/genus, PokemonSummary assembly, paging rules.

**Independent Test**: `./mvnw -B -pl domain test` green; no Spring on domain
test classpath.

- [x] T001 [HU01] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/WeightKgTest.java`
  asserting `69` hectograms → `6.9` kg (one decimal) and other fixtures; confirm
  `./mvnw -B -pl domain test` fails (type missing). Do not push RED.
  Coverage: **domain**. ⏸ Review — `test(domain): expect hectograms to kg (red)`

- [x] T002 [HU01] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/WeightKg.java`.
  `./mvnw -B -pl domain test` passes. Coverage: **domain**.
  ⏸ Review — `feat(domain): convert hectograms to kg (green)`

- [x] T003 **User action**: push `002-hu01-pokemon-enumeration` and open a
  **draft** PR to `main` (`gh pr create --draft --base main`). Confirm CI runs
  green on the skeleton. If red, fix before continuing. ⏸ Review

- [x] T004 [HU01] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/DisplayNameTest.java`
  — English `names[]` wins (e.g. "Mr. Mime"); missing English → resource slug
  fallback. Confirm fail. Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect english display name with slug fallback (red)`

- [x] T005 [HU01] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/DisplayName.java`.
  Tests pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): resolve english display name (green)`

- [x] T006 [HU01] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/CategoryTest.java`
  — English genus only; no English → null (not guessed). Confirm fail. Do not
  push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect english genus or null (red)`

- [x] T007 [HU01] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/Category.java`.
  Tests pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): resolve english category or null (green)`

- [x] T008 [HU01] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/pokemon/PokemonSummaryTest.java`
  — assemble summary: abilities sorted by catalog `slot` ascending; `hidden`
  flag preserved; `spriteUrl` null when catalog omits sprite; other fields
  wired. Confirm fail. Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect pokemon summary assembly (red)`

- [x] T009 [HU01] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/pokemon/AbilityRef.java`
  and `PokemonSummary.java` (nullable `spriteUrl` / `category`). Tests pass.
  Coverage: **domain**.
  ⏸ Review — `feat(domain): pokemon summary and ability ref (green)`

- [x] T010 [HU01] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/page/PageRequestTest.java`
  (and/or `PageTest.java`) — page beyond last given `totalElements` throws
  domain out-of-range; valid last page accepted; `totalPages` =
  ceil(total/size). Confirm fail. Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect page-beyond-last rejected (red)`

- [x] T011 [HU01] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/page/PageRequest.java`,
  `Page.java`, `PageOutOfRangeException.java`. Tests pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): page model and out-of-range (green)`

**Checkpoint**: Domain rules unit-tested in **domain** only (sequential
RED→GREEN pairs).

---

## Phase 2: Application (HU-01) — use case + port, tested in `application`

**Goal**: `ListPokemonUseCase` orchestrates `PokemonCatalogPort`; catalog
failures as `CatalogUnavailableException`.

**Independent Test**: `./mvnw -B -pl application test` with Mockito; no Spring.

- [x] T012 [HU01] RED: write
  `backend/application/src/test/java/com/miguel/pokemonapp/application/pokemon/ListPokemonUseCaseTest.java`
  — mock `PokemonCatalogPort`: `speciesPage` returns ids+total; per-id fetches;
  maps to `Page<PokemonSummary>`; page-beyond-last surfaces domain exception;
  port failure → `CatalogUnavailableException`. Confirm fail (types missing).
  Do not push RED. Coverage: **application**.
  ⏸ Review — `test(application): expect list pokemon use case (red)`

- [x] T013 [HU01] GREEN: implement
  `backend/application/src/main/java/com/miguel/pokemonapp/application/port/PokemonCatalogPort.java`
  (`speciesPage(offset, limit)` → ids + total; `fetchPokemon`; `fetchSpecies`),
  `CatalogUnavailableException.java`,
  `.../application/pokemon/ListPokemonUseCase.java` (species id == pokemon id
  for 1–1025). `./mvnw -B -pl application,domain test` passes.
  Coverage: **application**.
  ⏸ Review — `feat(application): list pokemon use case and catalog port (green)`

**Checkpoint**: Use case green with mocked port (R6 for application).

---

## Phase 3: Infrastructure client (HU-01) — RestClient + WireMock

**Goal**: `PokeApiCatalogAdapter` (no cache yet) against WireMock, no Spring
context.

**Independent Test**: `./mvnw -B -pl infrastructure test` green without Boot.

- [x] T014 [HU01] RED: add test-scoped `org.wiremock:wiremock` **3.13.2** to
  `backend/infrastructure/pom.xml` (and pin in parent
  `dependencyManagement` if not already). Write
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/pokeapi/PokeApiCatalogAdapterTest.java`
  — WireMock stubs for species list (count+results), `/pokemon/{id}`,
  `/pokemon-species/{id}`; assert `speciesPage` ids+total and mapped fields;
  **one per-id fetch failing → whole page `CatalogUnavailableException` (never
  a partial page)**; **WireMock delay greater than read timeout →
  `CatalogUnavailableException`**; upstream 500 on list →
  `CatalogUnavailableException`. Confirm fail (adapter missing). Do not push
  RED. Coverage: **infrastructure**.
  ⏸ Review — `test(infrastructure): expect pokeapi catalog adapter (red)`

- [x] T015 [HU01] GREEN: implement RestClient-based client +
  `backend/infrastructure/src/main/java/com/miguel/pokemonapp/infrastructure/pokeapi/PokeApiCatalogAdapter.java`
  implementing `PokemonCatalogPort`; parallel per-id fetch with virtual
  threads; configurable base URL + connect/read timeouts (constructible in
  tests without Spring). Failures must not return partial success. Tests pass.
  Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): pokeapi RestClient catalog adapter (green)`

**Checkpoint**: Uncached adapter proven with WireMock in **infrastructure**.

---

## Phase 4: Infrastructure cache (HU01-cache) — programmatic Caffeine

**Goal**: Cache successful speciesPage / pokemon / species responses; never
cache failures; default TTL 24h, configurable.

**Independent Test**: Two identical calls → one upstream; failing call not
stored.

- [x] T016 [HU01-cache] RED: extend
  `PokeApiCatalogAdapterTest.java` (or add
  `CachingPokemonCatalogPortTest.java` under
  `.../infrastructure/pokeapi/`) — after one successful `speciesPage` /
  `fetchPokemon` / `fetchSpecies`, a second identical call does not increment
  WireMock request count; after a 500, a retry still hits WireMock (failure
  not cached). Confirm these assertions fail with the uncached adapter. Do not
  push RED. Coverage: **infrastructure**.
  ⏸ Review — `test(infrastructure): expect caffeine cache hit and no failure cache (red)`

- [x] T017 [HU01-cache] GREEN: add `com.github.ben-manes.caffeine:caffeine`
  (BOM-managed) to `backend/infrastructure/pom.xml`. Implement programmatic
  Caffeine caches inside the adapter (or a caching decorator of
  `PokemonCatalogPort`) under
  `backend/infrastructure/src/main/java/com/miguel/pokemonapp/infrastructure/pokeapi/`;
  TTL default 24h + max size via constructor/properties; **no** `@Cacheable`
  / starter-cache. Tests pass. Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): programmatic caffeine caches for pokeapi (green)`

**Checkpoint**: Cache behavior proven without Spring context (R6).

---

## Phase 5: Web — springdoc spike (RED/GREEN; blocks controllers)

**Goal**: Prove springdoc **3.1.1** serves OpenAPI on Boot 4.1.1. If GREEN
cannot pass, **stop and decide** before Phase 6.

- [x] T018 [HU01] RED: write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/OpenApiDocsIntegrationTest.java`
  (or equivalent `@SpringBootTest`) asserting `GET /v3/api-docs` returns
  **200**. Confirm it fails with **404** (springdoc absent). Do not push RED.
  Coverage: **web**. ⏸ Review — `test(web): expect openapi docs endpoint (red)`

- [x] T019 [HU01] GREEN: add `springdoc-openapi-starter-webmvc-ui` **3.1.1** to
  `backend/web/pom.xml` (pin in parent `dependencyManagement`). Re-run T018;
  must be **200**. If it cannot pass, stop — do not start T020+.
  Coverage: **web**. ⏸ Review — `feat(web): add springdoc 3.1.1 (green)`

---

## Phase 6: Web API (HU-01) — controller then advice

**Goal**: `GET /api/v1/pokemon` per `contracts/pokemon-list-api.yaml`.
Controller tests assert **status codes / JSON shape only**; Problem Detail
bodies are asserted in the advice pair immediately after.

**Independent Test**: `@WebMvcTest` in **web** with mocked use case.

- [x] T020 [HU01] RED: add `spring-boot-starter-validation` to
  `backend/web/pom.xml`. Write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/pokemon/PokemonControllerTest.java`
  (`@WebMvcTest`) — `page=0` and `size=21` → **400**; happy path → **200**
  with page JSON shape (content/page/size/totalElements/totalPages); default
  size behaviour as applicable. **Do not** assert `problem+json` body here.
  Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect pokemon list status codes (red)`

- [x] T021 [HU01] GREEN: implement
  `backend/web/src/main/java/com/miguel/pokemonapp/web/pokemon/PokemonController.java`,
  request/response DTOs under `.../web/pokemon/`, Bean Validation
  (`@Min(1)` page; `@Min(1)` `@Max(PageRequest.MAX_SIZE)` size;
  `PageRequest.DEFAULT_SIZE` = 9); wire `ListPokemonUseCase` (mockable in
  slice). **Also** PokeAPI Spring wiring under
  `backend/web/src/main/java/com/miguel/pokemonapp/web/config/`
  (`@ConfigurationProperties` + `@Bean` adapter/`ListPokemonUseCase`,
  defaults in `application.yml` / `POKEAPI_BASE_URL`) — moved forward from
  T025 so existing `@SpringBootTest` ITs stay green without a shared
  `@Primary` mock. Controller tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): GET /api/v1/pokemon controller (green)`

- [x] T022 [HU01] RED: write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandlerTest.java`
  — `PageOutOfRangeException` → **400** with `application/problem+json` body
  (RFC 7807 fields); `CatalogUnavailableException` → **502** problem+json;
  no stack trace in body. Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect problem detail advice bodies (red)`

- [x] T023 [HU01] GREEN: implement
  `backend/web/src/main/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandler.java`
  (`@RestControllerAdvice`) mapping validation + domain out-of-range → 400 and
  `CatalogUnavailableException` → 502. Advice tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): global ProblemDetail exception advice (green)`

**Checkpoint**: Controller status/shape + advice problem+json green under
**web** slice/unit tests.

---

## Phase 7: End-to-end IT (HU-01) — list IT on existing web config

**Goal**: Full stack IT with WireMock-standalone as PokeAPI. Spring wiring for
PokeAPI already lives in **`web/config`** (from T021); this IT covers it (R6).

- [x] T024 [HU01] RED: add test-scoped `org.wiremock:wiremock-standalone`
  **3.13.2** to `backend/web/pom.xml`. Write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/pokemon/PokemonListIntegrationTest.java`
  — `@SpringBootTest` + existing Postgres Testcontainers;
  `POKEAPI_BASE_URL` → WireMock-standalone; assert 200 page JSON matches
  contract fields; assert 502 ProblemDetail when WireMock returns 500 for
  species list. Confirm fail (IT / advice gaps — wiring already in T021). Do
  not push RED. Coverage: **web** IT.
  ⏸ Review — `test(web): pokemon list IT against wiremock (red)`

- [x] T025 [HU01] GREEN: finish list IT against the wiring already added in
  T021 under `backend/web/src/main/java/com/miguel/pokemonapp/web/config/`
  (adapter + use-case beans, `application.yml` / `POKEAPI_BASE_URL`). Fix
  anything else needed so T024 passes.
  `./mvnw -B -pl web -am verify` green. Coverage: **web** IT (covers web
  config).
  ⏸ Review — `feat(web): pokeapi config wiring and list IT green`

---

## Phase 8: Ops & coverage visibility

- [ ] T026 Add `POKEAPI_BASE_URL=https://pokeapi.co/api/v2` to root
  `.env.example`; pass
  `POKEAPI_BASE_URL: ${POKEAPI_BASE_URL:-https://pokeapi.co/api/v2}` into the
  `backend` service in root `docker-compose.yml`; confirm alignment with
  `specs/002-hu01-pokemon-enumeration/contracts/env-config.md` (no drift).
  Verify with `docker compose config`. Coverage: N/A.
  ⏸ Review — `chore: wire POKEAPI_BASE_URL in env example and compose`

- [ ] T027 Add JaCoCo `report-aggregate` execution on `backend/web/pom.xml`
  for visibility only — **do not** bind `check` to the aggregate; per-module
  `check` remains the gate (R6). Verify aggregate HTML is produced on
  `./mvnw -B -pl web -am verify`. Coverage: reporting only.
  ⏸ Review — `build(web): add jacoco report-aggregate for visibility`

---

## Phase 9: Polish

- [ ] T028 Update root `README.md` — document `GET /api/v1/pokemon`,
  `POKEAPI_BASE_URL`, OpenAPI URL, and that catalog UI is 007. Run every new
  README command before marking done. Coverage: N/A.
  ⏸ Review — `docs: document hu01 pokemon list in readme`

- [ ] T029 Run `specs/002-hu01-pokemon-enumeration/quickstart.md` end to end
  (or record deviations and fix). Confirm SC-001–SC-005 behaviours locally
  or via IT substitutes. Coverage: validation run.
  ⏸ Review — `test: validate 002 quickstart`

- [ ] T030 Optional refactor only if needed after green (no behaviour change);
  separate commit. Skip if nothing to refactor. Coverage: existing tests stay
  green. ⏸ Review — `refactor: tidy hu01 after green`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Domain (Phase 1)** → **Application (Phase 2)** → **Infrastructure client
  (Phase 3)** → **Cache (Phase 4)** → **springdoc spike (Phase 5)** → **Web
  controller/advice (Phase 6)** → **E2E IT + web config (Phase 7)** → **Ops
  (Phase 8)** → **Polish (Phase 9)**
- **T003** (draft PR) follows the first GREEN (**T002**); then continue domain
- **[HU01-cache]** (Phase 4) depends on the uncached adapter (Phase 3)
- Controllers (**Phase 6**) MUST NOT start before spike GREEN **T019** succeeds
- Web PokeAPI `@Configuration` lives in **`web/config/`** (landed in
  **T021**; list IT finishes it in **T025**), not in infrastructure

### Within phases

- Domain: strictly sequential RED→GREEN pairs (no batch of REDs)
- RED before GREEN for the same behaviour elsewhere
- RED states reviewed locally; not pushed
- One task → one review stop
- Dependencies enter inside the first consumer task (see Global rules)

### Parallel Opportunities

- Limited: domain pairs are sequential by design
- After Phase 5, T020/T021 then T022/T023 are sequential (advice right after
  controller)

---

## Implementation Strategy

### MVP ([HU01])

1. Phases 1–3, 5–7 (listing API + IT). Prefer completing Phase 4
   ([HU01-cache]) before merge — TR-CACHE is in-scope nice to have.
2. Ops + README + quickstart.

### Full 002

1. Complete Phase 4 before merge.
2. T029 quickstart (or IT equivalents) green.
3. Keep draft PR CI green.

---

## Notes

- **30 tasks** (T001–T030); every one ends in a review stop
- Labels `[HU01]` / `[HU01-cache]` only — never `[USn]`
- Suggested commit messages follow Conventional Commits; the user commits
- Agent never commits or pushes; do not push RED
- springdoc GREEN failure → stop (no controller work until decided)
- Dependency injection map: wiremock→T014 RED; caffeine→T017 GREEN;
  validation→T020 RED; wiremock-standalone→T024 RED; springdoc→T019 GREEN
