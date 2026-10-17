# Agent corrections

Log of user corrections for the agent to apply in future sessions. Maintained
by the agent per `.cursor/rules/agent-corrections.mdc`.

## 2026-10-16 — Corrections log language
- **Source**: user
- **Context**: The first version of the agent-corrections rule and log was
  written in Spanish.
- **Correction**: Always write `.cursor/rules/agent-corrections.mdc` and
  `.cursor/agent-corrections.md` in English.

## 2026-10-16 — Constitution precedence and correction source
- **Source**: Miguel's review
- **Context**: The rule said to apply every correction with no check against
  binding project docs, and entries had no origin field.
- **Correction**: (1) Corrections never override the constitution or the
  feature's spec/plan/tasks — on conflict, stop and ask. (2) Every entry
  must include `Source`: `user` or `Miguel's review`.

## 2026-10-16 — Maven module naming and package-info accuracy (T002–T003)
- **Source**: Miguel's review
- **Context**: After T002–T003, package-info Javadoc drifted from the plan
  (messaging; entrypoint attributed to `.web`), internal module versions
  were repeated in each POM, and artifactIds were bare module names.
- **Correction**: (1) infrastructure Javadoc: persistence, PokeAPI client,
  caching and security; web Javadoc: controllers, DTOs and error handling
  (PokemonApplication is in `com.miguel.pokemonapp`). (2) Parent
  `dependencyManagement` for the four internal modules; no `<version>` on
  those deps in child POMs. (3) artifactIds `pokemonapp-{domain,application,
  infrastructure,web}` (directories stay short names).

## 2026-10-16 — Pin GitHub Actions after checking latest releases (T006)
- **Source**: Miguel's review
- **Context**: T006 pinned `actions/checkout@v4`, `actions/setup-java@v4`,
  and `dorny/paths-filter@v3` from memory; current majors are newer.
- **Correction**: Never pin GitHub Actions majors from memory. Before writing
  a `uses:` pin, check each action's latest release notes, note breaking
  input changes, and adapt. For this workflow: `checkout@v7`,
  `setup-java@v6`, `paths-filter@v4`; `cache-dependency-path:
  backend/**/pom.xml`; cancel-in-progress only on `pull_request`;
  `timeout-minutes` 5 (`changes`) / 15 (`backend`).

## 2026-10-16 — ArchUnit package prefixes and BOM (T008)
- **Source**: Miguel's review
- **Context**: ArchitectureTest used generic `..domain..` / `..web..` patterns
  that can match unrelated packages (e.g. `infrastructure.pokeapi.web`); the
  outer-frameworks rule omitted JDBC/HTTP client packages; ArchUnit version
  was pinned only in `web/pom.xml`.
- **Correction**: Use full prefixes `com.miguel.pokemonapp.{domain,
  application,infrastructure,web}..`. Forbid also `java.sql..` and
  `java.net.http..` in the outer-frameworks rule. Manage ArchUnit via
  `archunit.version` + parent `dependencyManagement`; no version in
  `web/pom.xml`. Avoid trailing blank lines in POMs.

## 2026-10-16 — No credential defaults in application.yml (T011)
- **Source**: Miguel's review
- **Context**: `application.yml` defaulted `SPRING_DATASOURCE_USERNAME` /
  `_PASSWORD` to `pokemon`, embedding credentials in source and disagreeing
  with `.env.example`'s `change-me` password.
- **Correction**: Keep only a non-secret URL default. Require username and
  password from the environment (no defaults) so missing credentials fail
  fast at startup (constitution VI). Tests rely on `@ServiceConnection`.

## 2026-10-16 — ImportTestcontainers lifecycle and IT webEnvironment (T012)
- **Source**: Miguel's review
- **Context**: Shared `PostgresContainers` kept `@Container` even though
  `@ImportTestcontainers` (not the JUnit `@Testcontainers` extension)
  manages lifecycle; `FlywayBaselineIntegrationTest` used `RANDOM_PORT`
  despite using only `JdbcTemplate`.
- **Correction**: On `@ImportTestcontainers` declaration classes, use
  `@ServiceConnection` without `@Container`. Integration tests that do not
  call HTTP should omit `webEnvironment` (default / no web server).

## 2026-10-16 — Clean Vite scaffold leftovers (T015)
- **Source**: Miguel's review
- **Context**: After scaffolding, unused template files remained
  (`public/icons.svg`, `README.md`) and `package.json` kept the generic
  name `frontend`.
- **Correction**: Remove unused Vite template assets and the scaffold
  README (root README is the project docs). Name the npm package
  `pokefolio-frontend` and refresh `package-lock.json` with `npm install`.

## 2026-10-16 — Explicit RTL cleanup and strict TS flags (T017)
- **Source**: Miguel's review
- **Context**: Testing Library auto-cleanup only registers when Vitest
  `globals: true`; this project uses explicit imports. TypeScript 6 enables
  `strict` by default, but the tsconfigs did not state it.
- **Correction**: In `src/test/setup.ts`, call `cleanup()` in `afterEach`
  from `@testing-library/react`. Set `"strict": true` explicitly in
  `tsconfig.app.json` and `tsconfig.node.json`.

## 2026-10-16 — Backend Docker context ignore and JDK build image (T022)
- **Source**: Miguel's review
- **Context**: `backend/.dockerignore` used plain `target/`, which only
  matches the context root, so module `target/` dirs could enter the build
  context and leak stale classes. The build stage used
  `maven:3.9-eclipse-temurin-21` even though the build runs `./mvnw`.
- **Correction**: Ignore `**/target/`, keep `.idea/`, and add `**/*.iml`.
  Build stage base image is `eclipse-temurin:21-jdk`; Maven version comes
  from the wrapper, matching local and CI.

## 2026-10-16 — Nginx SPA root and frontend image smoke test (T023/T025)
- **Source**: Miguel's review
- **Context**: Replacing the image `default.conf` dropped `root`/`index`, so
  SPA routes and `/` failed at runtime (found by running the stack). Nginx
  also fails to start unless the `backend` upstream hostname resolves.
- **Correction**: In `frontend/nginx.conf`, set `root /usr/share/nginx/html;`,
  `index index.html;`, and `try_files $uri $uri/ /index.html;`; keep `/api/`
  proxy unchanged. After `docker build frontend` in CI, tag the image, run it
  detached with `--add-host backend:127.0.0.1`, `curl -f` `/` and assert
  `index.html`, then stop the container.

## 2026-10-16 — README local run commands must be executed (T028)
- **Source**: Miguel's review
- **Context**: `./mvnw -pl web -am spring-boot:run` fails because `-am` also
  runs the goal on the parent POM ("Unable to find a suitable main class").
  Hard-coded DB password in the README diverged from `.env`. Found by running
  the README commands.
- **Correction**: Every README command must be executed before marking the
  docs task done. Local backend: `./mvnw -B install -DskipTests` then
  `./mvnw -pl web spring-boot:run`. Load credentials from `.env` with
  `set -a; . ../.env; set +a` and export
  `SPRING_DATASOURCE_USERNAME="$POSTGRES_USER"` /
  `SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"`.

## 2026-10-16 — 002 plan: programmatic Caffeine and speciesPage (plan review)
- **Source**: Miguel's review
- **Context**: Plan used `@Cacheable`/starter-cache and split
  count+listSpeciesIds; springdoc Jackson note was contradictory; web IT
  risked Jetty clashes; POKEAPI env wording was either/or.
- **Correction**: Cache with programmatic Caffeine in infrastructure (or a
  port decorator), not `@Cacheable`; never cache failures; default TTL 24h.
  Port: `speciesPage(offset, limit)` → ids + total. First web task is a
  springdoc 3.1.1 spike (`/v3/api-docs` = 200) or stop. Web IT uses
  `wiremock-standalone`. Assume species id == Pokémon id for 1–1025.
  `POKEAPI_BASE_URL` defaults in yml + `.env.example` + Compose
  `${POKEAPI_BASE_URL:-https://pokeapi.co/api/v2}`.
  `CatalogUnavailableException` lives in application (web → 502).

## 2026-10-16 — 002 analyze remediation: plan/tasks/spec sync
- **Source**: Miguel's review
- **Context**: `/speckit-analyze` found plan.md drifted from tasks.md rev. 2
  (wiring, springdoc spike, TDD order); sprite source and HU-01 "skills"
  wording underspecified; SC-003/SC-004 timing not automated.
- **Correction**: Keep plan.md structure + TDD ordering aligned with tasks
  (web/config/ with e2e IT GREEN; springdoc RED/GREEN; advice after
  controller; infra client then cache; PokemonSummary pair). Label springdoc
  tasks `[HU01]`. Sprite = `sprites.front_default` only. Spec Assumptions:
  HU-01 skills ≡ catalog abilities (slugs; UI formats in 007). Timing for
  SC-003/SC-004 stays manual in quickstart T029 (record observed values);
  no timing asserts in automated tests.

## 2026-10-16 — WeightKgTest: exact BigDecimal scale and negative rejection
- **Source**: Miguel's review
- **Context**: T001 used `isEqualByComparingTo`, which ignores scale, so
  `10` and `10.0` would both pass; negative hectograms were untested.
- **Correction**: For WeightKg (and similar one-decimal domain money/measure
  VOs), assert with `isEqualTo(new BigDecimal("…"))` so equals checks scale;
  API must always emit one decimal (e.g. 100 hg → `"10.0"`). Reject negative
  hectograms with `IllegalArgumentException`. Apply in RED before GREEN.

## 2026-10-16 — LocalizedText + shared English language constant
- **Source**: Miguel's review
- **Context**: After T005, `LocalizedName(languageCode, name)` was name-specific
  and duplicated the `"en"` literal; Category (genera) needs the same shape.
- **Correction**: Prefer a package-level `LocalizedText(languageCode, text)` for
  both species `names[]` and `genera[]`. Put the English language code in one
  shared constant in `domain.pokemon`. Refactor with tests green before the
  next RED (Category); no behaviour change in the rename.

## 2026-10-16 — Tests must not reuse production constants under test
- **Source**: Miguel's review
- **Context**: After T006/T007, `DisplayNameTest` and `CategoryTest` used
  `LanguageCodes.ENGLISH` in fixtures that assert English selection.
- **Correction**: Tests must not reuse production constants for the values
  they verify. Pin the real external/code value as a literal (here PokeAPI
  language code `"en"`) so a wrong constant cannot make the suite pass.

## 2026-10-16 — PokemonSummary assemble via CatalogPokemon/CatalogSpecies
- **Source**: Miguel's review
- **Context**: After T008/T009, `PokemonSummary.assemble` took seven loose
  args (names/genera easy to swap) and invariants lived only in the factory,
  so `new PokemonSummary(...)` could bypass them.
- **Correction**: Put summary invariants in the record compact constructor
  (`nationalNumber >= 1`, non-blank `name`, non-null `weightKg`, non-null
  `abilities` → `List.copyOf`). Introduce `CatalogPokemon` and
  `CatalogSpecies` (each validates nulls / `List.copyOf`) as the assemble
  inputs and as future `PokemonCatalogPort` return types. Factory becomes
  `assemble(CatalogPokemon, CatalogSpecies)`.

## 2026-10-16 — Stop if previous reviewed work is uncommitted
- **Source**: Miguel's review
- **Context**: Agents started the next task/refactor while prior reviewed
  changes were still uncommitted, mixing review units.
- **Correction**: Before any new task or refactor, run `git status`. If the
  previous task's changes are still uncommitted, stop and ask the user to
  commit first. Never stack new work on uncommitted reviewed work. Also
  recorded as rule 9 in `.cursor/rules/agent-corrections.mdc`.

## 2026-10-16 — Page: computed totalPages, ceilDiv, MAX_SIZE
- **Source**: Miguel's review
- **Context**: After T010/T011, `totalPages` was a record component (could be
  inconsistent), paging used `Math.ceil` on doubles, and max size was a magic
  `20` with no shared constant for web `@Max`.
- **Correction**: Expose `totalPages()` as a computed method
  (`Math.ceilDiv`); reject `totalElements < 0`; publish
  `PageRequest.MAX_SIZE = 20` for web validation. Keep literal fixtures in
  tests (e.g. 1025/9 → 114, size 21 rejected).

## 2026-10-16 — Page compact ctor: reject page/size < 1 via pagesFor
- **Source**: Miguel's review
- **Context**: After the Page refactor, `new Page<>(…, size 0, …)` hit
  `ArithmeticException` from `ceilDiv` instead of a clear domain reject;
  totalPages math was duplicated in the constructor and `totalPages()`.
- **Correction**: Compact constructor must reject `page < 1` and `size < 1`
  before paging math. Extract a shared page-count helper used by both the
  constructor and `totalPages()` (now `PageCounts.pagesFor`).

## 2026-10-16 — Catalog fetchEntries + PageRequest.requireWithin
- **Source**: Miguel's review
- **Context**: After T012/T013, the port exposed per-id `fetchPokemon` /
  `fetchSpecies` (parallelism belongs in the adapter) and the use case
  duplicated page-beyond-last math already owned by `Page`.
- **Correction**: Port API is `fetchEntries(List<Integer>)` →
  `List<CatalogEntry>` (domain record; nulls rejected) in request order.
  Move beyond-last to `PageRequest.requireWithin(totalElements)` using the
  same `PageCounts.pagesFor` as `Page`. Use case stays synchronous and only
  calls `requireWithin` then one `fetchEntries`.

## 2026-10-16 — PokeApiCatalogAdapter: getJson, payload validation, cancel
- **Source**: Miguel's review
- **Context**: After T014/T015, GET/onStatus/null/catch was duplicated; missing
  required JSON fields (e.g. weight) or id mismatches could surface as
  domain `IllegalArgumentException` (HTTP 500) instead of catalog 502; unused
  `org.wiremock:wiremock` remained in parent DM; failed fan-out did not cancel
  sibling futures.
- **Correction**: Extract private `getJson(uriTemplate, vars…)`. Validate
  required upstream fields (`id`, `name`, `weight`; species-page `count`) and
  that pokemon `id` equals the requested national id — throw
  `CatalogUnavailableException("malformed …")`. Cancel remaining futures on
  `fetchEntries` failure. Drop unused `wiremock` from parent
  `dependencyManagement` (keep `wiremock-standalone`).

## 2026-10-16 — Compile with deprecation; prefer Spring 7 / Jackson 3 APIs
- **Source**: Miguel's review
- **Context**: Adapter refactor used deprecated `RestClient.messageConverters`
  and Jackson `JsonNode.asText()`; found with
  `-Dmaven.compiler.showDeprecation=true`.
- **Correction**: Always compile/verify with deprecation warnings on. Never
  use APIs deprecated in the project's Spring 7 / Jackson 3 stack. Prefer
  default RestClient converters or `configureMessageConverters`; use
  `JsonNode.asString()` instead of `asText()`. Include `uriTemplate` in
  `getJson` error messages. Also recorded as rule 10 in
  `.cursor/rules/agent-corrections.mdc`.

## 2026-10-16 — Caffeine AsyncCache to avoid VT carrier pinning
- **Source**: Miguel's review
- **Context**: Sync Caffeine `Cache` + blocking HTTP load on Java 21 virtual
  threads pinned carriers (32×200ms loads ≈ 3264 ms with 2 carriers vs
  ≈ 223 ms with `AsyncCache`).
- **Correction**: *(Superseded by the next entry.)* Avoid blocking IO inside
  Caffeine `compute`/`get(key, mappingFunction)` on virtual threads. Keep
  `VirtualThreadCarrierPinningIT` under
  `-Djdk.virtualThreadScheduler.parallelism=2`.

## 2026-10-16 — Caffeine getIfPresent/load/put (not AsyncCache)
- **Source**: Miguel's review
- **Context**: `AsyncCache` + `expireAfterWrite` starts the expiry clock in a
  completion callback after `join()` returns, so
  `CachingPokeApiCatalogAdapterTest.expiredCacheEntryIsRefetched` failed
  10/10 when advancing a manual `Ticker` immediately after the first call.
- **Correction**: Use synchronous Caffeine `Cache` with
  `getIfPresent` → load **outside** any cache lock → `put` on success only.
  No blocking IO inside `compute`. Expiry starts at `put`. Document that
  concurrent misses for the same key are not coalesced (acceptable for
  idempotent GETs). Keep carrier-pinning IT and cache tests.

## 2026-10-16 — springdoc: set enabled flags explicitly
- **Source**: Miguel's review
- **Context**: springdoc 3.1.1 logs WARN when api-docs/swagger-ui are enabled
  by default without explicit properties.
- **Correction**: Set `springdoc.api-docs.enabled=true` and
  `springdoc.swagger-ui.enabled=true` in `application.yml` to express intent
  and silence those default-enabled WARNs.

## 2026-10-16 — T020/T021: real PokeAPI wiring, no shared @Primary mock
- **Source**: Miguel's review
- **Context**: After T020/T021, a `@Primary` mock `ListPokemonUseCase` in
  `PostgresTestcontainersConfiguration` would silently replace the real use
  case in the e2e IT; wiring was deferred to T025; default size was a magic
  `9`; AssertJ used FQCN; `weightKg` scale (`10.0`) is invisible to numeric
  jsonPath.
- **Correction**: Never put `@Primary` mocks of production beans in shared
  test config. Wire `PokeApiProperties` + `PokeApiCatalogAdapter` (close on
  shutdown) + `ListPokemonUseCase` under `web/config` when the controller
  needs them; existing `@SpringBootTest` ITs are the RED for that wiring.
  Use `PageRequest.DEFAULT_SIZE`; static AssertJ imports; assert raw JSON
  contains `"weightKg":10.0` for scale.

## 2026-10-16 — T022/T023: no handleTypeMismatch for record query params
- **Source**: Miguel's review
- **Context**: `GlobalExceptionHandler.handleTypeMismatch` was unreachable
  (JaCoCo uncovered): with record-bound query params, `page=abc` surfaces as
  `MethodArgumentNotValidException`, already handled and tested.
- **Correction**: Do not override `handleTypeMismatch` for HU-01 list query
  binding. Re-add it in 003 with a test for `/api/v1/pokemon/{id}=abc` (path
  variable type mismatch).

## 2026-10-16 — OpenAPI: ParameterObject + problem responses + API info
- **Source**: Miguel's review
- **Context**: Live `/v3/api-docs` exposed a required `request` object instead
  of optional query params `page`/`size`, omitted 400/502 problem+json, and
  lacked Pokéfolio API info metadata.
- **Correction**: Annotate list request with springdoc `@ParameterObject`
  (and Schema defaults/bounds); document 400/502 as
  `application/problem+json` + `ProblemDetail` via `@ApiResponse`; register an
  `OpenAPI` bean (title "Pokéfolio API", version, one-line description). Cover
  with `OpenApiDocsIntegrationTest` against real `/v3/api-docs`.

## 2026-10-16 — 003 spec: three fetch phases + evolution_details first
- **Source**: Miguel's review
- **Context**: Spec 003 said “one parallel fan-out”; evolution edges did not
  define which `evolution_details` entry to use; chain node id semantics
  were underspecified.
- **Correction**: Detail fetch is at most three sequential phases, each
  parallel/cached: (a) pokemon+species, (b) evolution-chain, (c)
  `fetchEntries` for chain ids (requested id already cached). Use the first
  `evolution_details` entry; empty list → null trigger/minLevel/item. Chain
  nodes are species ids; name/image via `fetchEntries` (species id ==
  default Pokémon id for 1–1025).

## 2026-10-16 — 003 plan: fetchDetail + one cache per URL
- **Source**: Miguel's review
- **Context**: Plan repeated the 002 port mistake — exposing
  `fetchPokemonDetail` + `fetchSpeciesDetail` so parallelism lived in the
  use case; risked a second cache for the same upstream URL.
- **Correction**: Phase (a) is **one** port op `fetchDetail(id)` →
  `CatalogDetailEntry` (adapter fetches pokemon+species concurrently). Use
  case = three port calls (a/b/c). Pokemon/species Caffeine caches store
  `CatalogPokemonDetail` / `CatalogSpeciesDetail`; 002 list types are
  **derived** (`toSummary()` / equivalent). WireMock: for a detail call,
  `/pokemon/{id}` and `/pokemon-species/{id}` exactly once (phase c reuses
  phase a). Keep 002 list tests green.

## 2026-10-16 — 003 tasks: stubs, OpenAPI, id=0 detail, acceptance IT
- **Source**: Miguel's review
- **Context**: Tasks review — port stubs timing, OpenAPI coverage, validation
  problem detail, and acceptance IT skip rule.
- **Correction**: T013 adds temporary `UnsupportedOperationException` stubs
  for `fetchDetail` / `fetchEvolutionChain` on the adapter and verifies with
  full `./mvnw -B verify` (Coverage: application; stubs compile-only). T014
  does not invent stubs. T017/T018 extend `OpenApiDocsIntegrationTest` for
  `/api/v1/pokemon/{id}` 200/400/404/502. T019 asserts `id=0` → 400
  problem+json whose detail names the `id` parameter. T021/T022 are
  acceptance ITs; if T021 passes with no production changes, commit as
  `test(web): …` and skip T022.

## 2026-10-16 — 003 analyze remediations (F1–F6, A1, C1)
- **Source**: Miguel's review
- **Context**: `/speckit-analyze` findings applied after Miguel validation.
- **Correction**: Spec FR-010 / Assumptions state one `fetchDetail` port op,
  sequential phases with intra-phase parallelism in (a)/(c), one cache per
  URL holding detail records + `toSummary()` for list, Key Entities include
  `isCurrent` and point to data-model; Edge Cases allow duplicate upstream
  on concurrent misses but never partial success. Plan TDD ordering: shared-
  cache REFACTOR (T014) before detail adapter. T021 asserts WireMock 5xx →
  502 without upstream URL.

## 2026-10-16 — FlavorText: null reference, not null-field pair
- **Source**: Miguel's review
- **Context**: T004/T005 used a compact constructor that allowed
  `FlavorText(null, null)` (“null pair”).
- **Correction**: Absent narrative is **only** a null `FlavorText` (what
  `from()` returns). Constructor requires non-blank `text` and non-null
  `version`. Whitespace: collapse with `\\s+` then trim (`\\s` covers
  `\\f`/`\\n`/`\\r`). Spec/data-model say “`flavorText` is null”, not
  “null pair”. (See 2026-10-17 for the additional `POKéMON` → `Pokémon`
  step.)

## 2026-10-16 — Evolution node image is front sprite thumbnail
- **Source**: Miguel's review
- **Context**: T008/T009 used `ImageUrl.resolve` for evolution nodes,
  conflicting with FR-003 (detail artwork) vs UI tree thumbnails.
- **Correction**: Option A — evolution node `imageUrl` is the catalog
  **front sprite** (`spriteUrl` / `front_default`) directly; detail payload
  image keeps artwork → sprite → null (FR-003). Spec FR-006, data-model, and
  OpenAPI examples must not say “same rule as FR-003”. T017 must assert JSON
  field name `isCurrent`.

## 2026-10-16 — Domain Require helper + AbilityRef.orderedFrom
- **Source**: Miguel's review
- **Context**: T010/T011 duplicated ability ordering and null/blank/`>=`
  validation if/throw blocks across domain records.
- **Correction**: Extract `AbilityRef.orderedFrom` (and type name ordering
  helper) for summary/detail assemble. Add `domain.Require` with
  `nonNull` / `nonBlank` / `atLeast` and use it in domain records; unit-test
  Require thoroughly. Refactor only — no behaviour change beyond shared
  message wording for those helpers.

## 2026-10-16 — List 404 → unavailable; retry once on ResourceAccessException
- **Source**: Miguel's review
- **Context**: T015/T016 mapped identity 404 to `PokemonNotFoundException`
  for all callers; a flaky JDK HttpClient keep-alive close
  (“HTTP/1.1 header parser received no bytes”) was observed once.
- **Correction**: (1) `fetchEntries` (list path) must wrap
  `PokemonNotFoundException` as `CatalogUnavailableException` — a
  species-list id that 404s is catalog inconsistency (502), not “Pokémon
  not found” for `GET /api/v1/pokemon/{id}`. Keep
  `PokemonNotFoundException` only on `fetchDetail`. (2) In `getJson`,
  retry **once** on `ResourceAccessException` for idempotent GETs; never
  retry HTTP status errors. Cover both with WireMock (list 404 →
  unavailable; `Fault.CONNECTION_RESET_BY_PEER` then 200 → exactly 2
  requests; two faults → unavailable).

## 2026-10-16 — Do not retry HttpTimeoutException in getJson
- **Source**: Miguel's review
- **Context**: Blind retry on every `ResourceAccessException` would also
  retry read/connect timeouts, doubling wait (e.g. 5 s → ~10 s) and load.
- **Correction**: Retry once only for connection-level failures (reset /
  closed keep-alive). If the most specific cause of the
  `ResourceAccessException` is `java.net.http.HttpTimeoutException`, do
  **not** retry — map straight to `CatalogUnavailableException`. WireMock:
  fixed delay > read timeout → unavailable and exactly **1** request.

## 2026-10-16 — PokemonNotFoundException carries nationalId
- **Source**: Miguel's review
- **Context**: T019/T020 built the 404 problem detail via
  `URI_TEMPLATE_VARIABLES` / `pathVariable("id")`, coupling the global
  handler to a route variable name.
- **Correction**: `PokemonNotFoundException` carries `int nationalId`
  (constructor + accessor). Keep an internal message with the upstream
  path for logs. Adapter passes the requested id; handler detail uses
  `ex.nationalId()` only — no URI template lookup.

## 2026-10-17 — FlavorText replaces Gen I–III "POKéMON"
- **Source**: Miguel's review
- **Context**: T023 README example disagreed with the live API: Bulbasaur
  red flavor text still contained the catalog quirk `POKéMON`.
- **Correction**: Domain `FlavorText` normalisation also replaces
  `POKéMON` → `Pokémon` (after whitespace collapse). Cover with a unit
  test using the real Bulbasaur red entry. Keep README / contract examples
  aligned with the live API response.

## 2026-10-17 — 004 spec: seed tags, 006 ownership, constitution VI exception
- **Source**: Miguel's review
- **Context**: Spec 004 Assumptions underspecified seed tag vocabulary,
  006 ownership of seed rows, and the constitution VI write-route exception.
- **Correction**: (1) Seed tags: use lowercase examples such as
  `first-partner`, `field-notes`, `forest` — never `favorites`. (2) Note
  for 006: when records become owned per user, HU-03 seed rows are assigned
  to the demo user (mock credentials seeded in 006). (3) Public write /
  global store is a temporary documented exception to constitution VI;
  `plan.md` MUST record it in Complexity Tracking.

## 2026-10-17 — 004 plan: import order, saveAndFlush, JPA yml
- **Source**: Miguel's review
- **Context**: Plan review for `004-hu03-data-sync` underspecified import
  sequencing, how unique violations surface in the JPA adapter, and JPA
  Boot defaults.
- **Correction**: (1) `ImportPokemonUseCase`: `existsByNationalNumber(n)` →
  409 without catalog call; then `fetchDetail(n)`; then `save`. Unique
  constraint remains the concurrent backstop. (2) Adapter `save` is its
  own short `@Transactional` method and uses `saveAndFlush` so
  `DataIntegrityViolationException` is translated inside the adapter to
  `DuplicateLocalPokemonException` (plain `save()` may fail at commit as
  500). (3) `application.yml`: `spring.jpa.open-in-view=false` and
  `spring.jpa.hibernate.ddl-auto=validate`.

## 2026-10-17 — 004 analyze remediation (I1/U1/G1/A1/I2)
- **Source**: Miguel's review
- **Context**: `/speckit-analyze` findings for `004-hu03-data-sync`.
- **Correction**: (I1) FR-005: same page/size rules and pagination envelope
  as 002; items are `LocalPokemon`, not `PokemonSummary`. (U1) Register the
  JPA adapter as a `@Bean` in `web/config` (PokeApiConfiguration pattern);
  do **not** add `@EnableJpaRepositories` / `@EntityScan` (`PokemonApplication`
  root-package scan covers infrastructure); in `@DataJpaTest`, construct the
  adapter with the injected Spring Data repo. (G1) page-beyond-last → 400 in
  T006 use-case test and T014 e2e IT. (A1) Exact V3 seed strings in
  `data-model.md` table; T009 must match. (I2) Post-design check cites
  R1–R8 + R3b. (G2/G3) Manual quickstart measurement in T017 is enough.

## 2026-10-17 — LocalPokemon: no domain surrogate id
- **Source**: Miguel's review
- **Context**: T004/T005 review — `LocalPokemon` carried `Long id`.
- **Correction**: Do not put the DB surrogate key on the domain aggregate.
  Domain / API identity is `nationalNumber`. The surrogate `id` stays on the
  JPA entity / Flyway table only.

## 2026-10-17 — ImportPokemonUseCase: catch only around snapshotOf
- **Source**: Miguel's review
- **Context**: T006/T007 review — try/catch wrapped `snapshotOf` and `save`.
- **Correction**: Narrow the try/catch to `LocalPokemon.snapshotOf` only.
  `repository.save` stays outside so an `IllegalArgumentException` from
  persistence is not misreported as `CatalogUnavailableException` (502).
  Cover with a test: save throws IAE → propagates as IAE.

## 2026-10-17 — JPA adapter: constraint-specific 409, no @Component, FR seed
- **Source**: Miguel's review
- **Context**: T008/T009 review of `LocalPokemonJpaAdapter` and V3 seed.
- **Correction**: (1) Map `DataIntegrityViolationException` →
  `DuplicateLocalPokemonException` only when the cause chain has a Hibernate
  `ConstraintViolationException` whose constraint name is
  `uq_pokemon_record_national_number`; rethrow other integrity violations.
  Cover with a test that a different violation is not reported as duplicate.
  (2) Do not put `@Component` on the adapter — register it as a `@Bean` in
  `web/config` (T011 / U1). (3) Seed localized names are French nomenclature:
  Bulbizarre, Herbizarre, Florizarre (no "(demo)"). (4) Prefer injecting
  `Clock` for `created_at` / `updated_at`.
