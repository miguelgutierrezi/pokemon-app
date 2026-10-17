# Research: Setup (project skeleton)

**Feature**: `001-setup` | **Date**: 2026-10-15 (revised)

## R1. Versions (verified against Maven Central on 2026-10-15)

**Decision**:

| Artifact | Version | Note |
|---|---|---|
| Spring Boot | **4.1.1** | Latest GA; 4.2.0-M2 is milestone only |
| springdoc-openapi (for 002) | 3.1.x | 3.x is the Boot 4 line |
| Testcontainers | 2.x (`org.testcontainers:testcontainers-postgresql`) | Artifact renamed in 2.x; prefer the version managed by the Boot BOM |
| ArchUnit | 1.5.x (`archunit-junit5`) | |

Boot 4 modularised auto-configuration: Flyway needs
`spring-boot-starter-flyway` (plus `flyway-database-postgresql`), not just
`flyway-core`. Re-check patch versions when writing the POMs.

**Alternatives considered**: Spring Boot 3.5.x — maintained but not "latest
stable"; 4.2 milestones — not GA.

## R2. Maven multi-module dependency graph

**Decision**:

| Module | Depends on | Allowed third-party |
|--------|------------|---------------------|
| `domain` | — | Pure Java only |
| `application` | `domain` | Pure Java only (ports/use cases) |
| `infrastructure` | `application`, `domain` | JPA, Flyway, JDBC (later: HTTP client, Caffeine, JWT) |
| `web` | `infrastructure`, `application`, `domain` | Spring Web, Actuator (later: Security, Validation, springdoc) |

Parent POM owns Java 21, Surefire/Failsafe, Spotless, JaCoCo; Spring Boot
plugin only on `web`.

## R3. ArchUnit placement, rules, and TDD

**Decision**: `ArchitectureTest` in `web/src/test` (the bootable module sees
all modules). Rules:

1. `..domain..` depends on nothing outside itself and `java..`
2. `..application..` depends only on itself, `..domain..`, and `java..`
3. Neither depends on `org.springframework..`, `jakarta.persistence..`,
   `jakarta.servlet..`, `..infrastructure..`, `..web..`

**Gotcha**: with empty `domain`/`application` packages ArchUnit fails by
default ("rule failed to check any classes"). Use `.allowEmptyShould(true)` on
these rules in 001 and remove it in 002 once classes exist.

**TDD**: red = add a temporary probe class in `domain` that imports Spring and
watch the rule fail; green = remove the probe. Both commits are kept for the
history.

**Alternatives considered**: per-module ArchUnit (more boilerplate); Maven
Enforcer banned-dependencies (complementary but doesn't check packages).

## R4. Health endpoint

**Decision**:
- Actuator exposes only `health`; set
  `management.endpoint.health.show-components=always` so `components.db` is
  visible (hidden by default).
- Default status mapping: `UP` → 200, `DOWN`/`OUT_OF_SERVICE` → **503**.
- No Spring Security in 001, so health is public without extra config; 006
  will keep it public explicitly.
- Backend listens on 8080; acceptance check is direct:
  `GET http://localhost:8080/actuator/health`.
- Test: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers
  Postgres via `@ServiceConnection`; asserts 200 + `status=UP` +
  `components.db.status=UP`.

## R5. Flyway baseline

**Decision**: `V1__baseline.sql` in
`infrastructure/src/main/resources/db/migration/` with a comment only. The
health integration test also asserts that `flyway_schema_history` contains
version 1.

**Alternatives considered**: `baselineOnMigrate` — less explicit.

## R6. JaCoCo ≥ 80% on a skeleton

**Decision**: JaCoCo `check` (LINE, COVEREDRATIO 0.80) per module, bound to
`verify`, with integration tests included. Exclude only the
`*Application` bootstrap class (its `main` is never run by tests); document
the exclusion in the parent POM. Modules without classes are skipped by
JaCoCo. Do **not** lower the threshold.

**Consequence found in T014 (binding for 002 onwards)**: JaCoCo measures each
module only with that module's own tests. Code in `infrastructure` exercised
only by `web` integration tests counts as 0% in `infrastructure` and fails
the gate. In 001 the gate is effectively vacuous ("Analyzed bundle
'pokemonapp-web' with 0 classes"); it starts biting with the first real code.

Rule: **each module tests its own code**.

| Module | How it is tested |
|---|---|
| `domain` | Pure unit tests (JUnit + AssertJ), no Spring |
| `application` | Use-case unit tests, ports mocked with Mockito, no Spring |
| `infrastructure` | PokeAPI client with WireMock, no Spring context (build the client in the test); JPA adapters (004+) with `@DataJpaTest` + Testcontainers, using an empty `@SpringBootConfiguration` class in `infrastructure/src/test` |
| `web` | Controller tests (MockMvc/RestTestClient, use cases mocked) plus a few end-to-end integration tests |

A `report-aggregate` execution may be added in `web` (002) for a single HTML
report **for visibility only**; the gate stays per module.

**Alternatives considered**: a global gate over an aggregated report —
rejected: JaCoCo `check` cannot run on `report-aggregate` (would need an extra
module or custom parsing), and a global 80% can hide an untested layer.
Lowering the threshold or excluding modules — rejected.

## R7. Frontend placeholder

**Decision**: Vite + React 19 + TypeScript (strict), Tailwind v4 via
`@tailwindcss/vite`, ESLint flat config (typescript-eslint, react-hooks,
react-refresh), Vitest + Testing Library + jsdom. One `App` component
rendering the app name; one test asserting it renders. Scripts: `lint`,
`typecheck` (`tsc -b --noEmit`), `test` (`vitest run`), `build`.

No router, no TanStack Query, no shadcn/ui in 001 (nothing to route or fetch);
they arrive in 007.

**Alternatives considered**: CRA — deprecated; Next.js — unnecessary SSR.

## R8. Docker Compose & Nginx proxy

**Decision**:
- Services `postgres` (16-alpine, `pg_isready` healthcheck), `backend`
  (8080, actuator healthcheck, `depends_on: postgres: service_healthy`),
  `frontend` (3000:80, `depends_on: backend: service_healthy`).
- Required variables use `${VAR:?message}` so Compose fails fast naming the
  missing key.
- Compose builds `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` from the three
  `POSTGRES_*` keys, so `.env` stays minimal.
- Nginx: `try_files $uri /index.html` for the SPA;
  `location /api/ { proxy_pass http://backend:8080; }` — **no trailing slash,
  so the path is forwarded unchanged**. Actuator (`/actuator/**`) is therefore
  not exposed through `:3000`.
- Backend image healthcheck: verify that the chosen JRE base image ships
  `curl`/`wget`; if not, install `curl` in the runtime stage (small, explicit).

**Alternatives considered**: stripping `/api` in Nginx — rejected: backend and
proxied paths diverge, Swagger "Try it out" breaks, and actuator leaks through
the UI origin.

## R9. CI/CD workflow

**Decision**: `.github/workflows/ci.yml` on `pull_request` and `push` to
`main`:

1. `changes` — `dorny/paths-filter` exposes `backend` (`backend/**`) and
   `frontend` (`frontend/**`) outputs (workflow-level `paths` cannot gate
   individual jobs).
2. `backend` — `if: needs.changes.outputs.backend == 'true'`; Temurin 21 with
   Maven cache; `mvn -B verify`; then `docker build backend`.
3. `frontend` — `if: needs.changes.outputs.frontend == 'true'`; Node 24 with
   npm cache; `npm ci`, `lint`, `typecheck`, `test`, `build`; then
   `docker build frontend`.

4. `publish` — only on `push` to `main`; `needs: [backend, frontend]` with
   `if: github.event_name == 'push' && !failure() && !cancelled()` (skipped
   jobs are fine, failed ones block); `permissions: packages: write`;
   `docker/login-action` to `ghcr.io` with `GITHUB_TOKEN`;
   `docker/build-push-action` for both images, tags
   `ghcr.io/<owner>/pokemonapp-backend|frontend:{sha,latest}`.

Jobs 2 and 3 run in parallel. On PRs, images are built inside each job but
never pushed.

**Order of introduction**: the workflow is created right after Spotless
(backend job only, `verify`), so a draft PR gates every push from then on. The
frontend job is added with the frontend (S3); the `docker build` steps with
each Dockerfile (S1); `publish` last (S4).

**Alternatives considered**: deploying to a PaaS (Render/Fly) — rejected for
time and secrets; the PDF only requires a Dockerfile. GHCR keeps CD to the
registry with no extra credentials.

## R10. Formatting

**Decision**: Spotless Maven plugin with google-java-format, `check` bound to
`verify`. Frontend: ESLint only (no Prettier) to avoid two formatters.

## R11. Removed from 001 (was in the first draft)

ProblemDetail advice, springdoc, Spring Security, JWT secret, PokeAPI base
URL, Caffeine, WireMock, TanStack Query, React Router, shadcn/ui. Reason:
constitution IX — each enters with the feature that first exercises and tests
it (002, 006, 007).
