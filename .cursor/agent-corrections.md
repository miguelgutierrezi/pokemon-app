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
