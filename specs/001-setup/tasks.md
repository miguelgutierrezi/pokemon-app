---
description: "Task list for feature 001-setup"
---

# Tasks: Setup (project skeleton)

**Input**: Design documents from `/specs/001-setup/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Required — constitution II (TDD). Every production change is preceded
by a task that writes a failing test and confirms it fails (RED), followed by
the minimal change that makes it pass (GREEN). Infrastructure files that cannot
be unit-tested (Dockerfiles, Compose, Nginx, CI/CD) are verified with the
command or pipeline run named in the task.

**Organization**: Grouped by setup scenario S1–S5 from spec.md. Labels are
`[S1]`…`[S5]` (not `[USn]`): constitution X reserves "user story" for
HU-01..HU-04.

**Revision 2 (2026-10-16)**: CI introduced early (backend job right after
Spotless, with a draft PR), frontend job added in S3, `docker build` steps
added with each Dockerfile, and CD to GHCR added in S4. Tasks from T006 on were
renumbered.

## Format: `[ID] [P?] [Scenario] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Sn]**: Setup scenario from spec.md
- **⏸ Review**: stop after the task; the user reviews and makes the commit.
  The suggested commit message is given; the agent never commits or pushes.

## Path Conventions

- Backend: `backend/<module>/src/{main,test}/java/com/miguel/pokemonapp/...`
- Frontend: `frontend/src/`
- Root: `docker-compose.yml`, `.env.example`, `.github/workflows/ci.yml`, `README.md`

## Global rules for every task

1. Add only the dependencies listed in plan.md. **Forbidden in 001**: Spring
   Security, JWT, validation, springdoc, Caffeine, WireMock, data-jpa,
   TanStack Query, React Router, shadcn/ui.
2. Before marking a task `[x]`: `./mvnw -B verify` (backend tasks) or
   `npm run lint && npm run typecheck && npm test` (frontend tasks) must pass,
   except in RED tasks, where the named test MUST fail for the stated reason.
3. Prefer versions managed by the Spring Boot 4.1.1 BOM; pin others to the
   latest GA at implementation time.
4. Once the draft PR exists (T007), every push must keep CI green.

---

## Phase 1: Setup (shared infrastructure)

**Purpose**: Repository hygiene and the empty Maven multi-module build.

- [x] T001 Create root `.gitignore` covering `.env`, `backend/**/target/`, `frontend/node_modules/`, `frontend/dist/`, `.idea/`, `.vscode/`, `.DS_Store`. ⏸ Review — `chore: add root gitignore`
- [x] T002 Create parent POM `backend/pom.xml`: groupId `com.miguel`, artifactId `pokemonapp-parent`, packaging `pom`, parent `spring-boot-starter-parent` 4.1.1, `java.version` 21, modules `domain`, `application`, `infrastructure`, `web` (in that order). No dependencies yet.
- [x] T003 Create module POMs with inward-only dependencies (research R2): `backend/domain/pom.xml` (no deps), `backend/application/pom.xml` (→ domain), `backend/infrastructure/pom.xml` (→ application, domain), `backend/web/pom.xml` (→ infrastructure, application, domain). Add `package-info.java` with a one-line Javadoc describing each module's role in `com/miguel/pokemonapp/{domain,application,infrastructure,web}/`. Verify: `cd backend && mvn -B verify` succeeds. ⏸ Review — `chore(backend): add maven multi-module skeleton`
- [x] T004 Add the Maven wrapper (`backend/mvnw`, `backend/.mvn/wrapper/`) via `mvn wrapper:wrapper` so CI and README use `./mvnw`. ⏸ Review — `chore(backend): add maven wrapper`

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The formatting gate and the CI pipeline that guards every later push.

- [x] T005 Configure Spotless (google-java-format) in `backend/pom.xml` `pluginManagement` + `plugins`, with `check` bound to `verify` (research R10). Verify RED: temporarily mis-indent a `package-info.java`, run `./mvnw -B verify`, confirm it fails on Spotless; revert with `./mvnw spotless:apply`; verify GREEN. ⏸ Review — `build(backend): enforce google-java-format with spotless`
- [x] T006 Create `.github/workflows/ci.yml` with the minimal pipeline (research R9): name `CI`; triggers `pull_request` and `push` to `main`; `concurrency` group per ref with `cancel-in-progress: true`; default `permissions: contents: read`; job `changes` using `dorny/paths-filter` with outputs `backend` (`backend/**`, `.github/workflows/**`) and `frontend` (`frontend/**`, `.github/workflows/**`); job `backend` (`needs: changes`, `if: needs.changes.outputs.backend == 'true'`, `actions/checkout`, `actions/setup-java` Temurin 21 with `cache: maven`, `working-directory: backend`, run `./mvnw -B verify`). No frontend job, no docker build, no publish yet. Pin actions to major versions. Validate the YAML locally (`actionlint` if available, otherwise careful review). ⏸ Review — `ci: add backend verification pipeline`
- [x] T007 **User action**: push `001-setup` and open a **draft** PR to `main` (`gh pr create --draft --base main`). Confirm in the PR checks that `changes` and `backend` run and are green. If red, fix in a follow-up change before continuing. ⏸ Review

**Checkpoint**: The empty build is green, formatting is enforced, and every push to the draft PR runs CI.

---

## Phase 3: Scenario S2 — Backend quality gates (P1)

**Goal**: ArchUnit boundaries, a bootable app with healthy DB, a Flyway baseline, and the coverage gate.

**Independent Test**: `cd backend && ./mvnw -B verify` passes (Docker running for Testcontainers), locally and in the `backend` CI job.

### Architecture boundaries (TDD)

- [x] T008 [S2] Add `archunit-junit5` (test scope) to `backend/web/pom.xml`. Write `backend/web/src/test/java/com/miguel/pokemonapp/architecture/ArchitectureTest.java` with the three rules of research R3 (domain only depends on itself and `java..`; application only on itself, domain and `java..`; neither on `org.springframework..`, `jakarta.persistence..`, `jakarta.servlet..`, `..infrastructure..`, `..web..`), each with `.allowEmptyShould(true)`. RED: add a temporary probe class `backend/domain/src/main/java/com/miguel/pokemonapp/domain/ArchitectureProbe.java` importing `org.springframework.stereotype.Component` (add `spring-context` to domain temporarily) and confirm the test fails. Do not push the RED state. ⏸ Review — `test(backend): add architecture rules (red: probe violates domain purity)`
- [x] T009 [S2] GREEN: delete `ArchitectureProbe.java` and the temporary `spring-context` dependency from `backend/domain/pom.xml`; `./mvnw -B verify` passes. ⏸ Review — `test(backend): remove architecture probe (green)`

### Bootable app + DB health (TDD)

- [x] T010 [S2] Add test dependencies to `backend/web/pom.xml`: `spring-boot-starter-test`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql`, `org.testcontainers:testcontainers-junit-jupiter`. RED: write `backend/web/src/test/java/com/miguel/pokemonapp/web/HealthEndpointIntegrationTest.java` — `@SpringBootTest(webEnvironment = RANDOM_PORT)`, a `@Container @ServiceConnection PostgreSQLContainer` (`postgres:16-alpine`), and a test asserting `GET /actuator/health` returns 200 with JSON `status` = `UP` and `components.db.status` = `UP` (contract `contracts/health-api.yaml`). Confirm it fails (no application class yet). Do not push the RED state. ⏸ Review — `test(web): expect healthy actuator with database (red)`
- [x] T011 [S2] GREEN: add `spring-boot-starter-web` and `spring-boot-starter-actuator` to `backend/web/pom.xml`; add `spring-boot-starter-jdbc` and `org.postgresql:postgresql` (runtime) to `backend/infrastructure/pom.xml`; create `backend/web/src/main/java/com/miguel/pokemonapp/PokemonApplication.java` (`@SpringBootApplication` in the base package so later modules are scanned); create `backend/web/src/main/resources/application.yml` with `spring.application.name: pokemonapp`, datasource from `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` with defaults `jdbc:postgresql://localhost:5432/pokemon` / `pokemon` / `pokemon`, `management.endpoints.web.exposure.include: health`, `management.endpoint.health.show-components: always`. Add `spring-boot-maven-plugin` to `backend/web/pom.xml` only. Test passes locally; push and confirm the `backend` CI job (GitHub runners have Docker for Testcontainers). ⏸ Review — `feat(web): bootable app exposing actuator health (green)`

### Flyway baseline (TDD)

- [x] T012 [S2] RED: add a test to `HealthEndpointIntegrationTest.java` (or a sibling `FlywayBaselineIntegrationTest.java` sharing the container setup) asserting via `JdbcTemplate` that `flyway_schema_history` contains a successful row with `version = '1'` and `description = 'baseline'`. Confirm it fails (table does not exist). Do not push the RED state. ⏸ Review — `test(infrastructure): expect flyway baseline v1 (red)`
- [x] T013 [S2] GREEN: add `spring-boot-starter-flyway` and `org.flywaydb:flyway-database-postgresql` to `backend/infrastructure/pom.xml`; create `backend/infrastructure/src/main/resources/db/migration/V1__baseline.sql` containing only a comment (data-model "BaselineMigration": "creates no tables"). Test passes. ⏸ Review — `feat(infrastructure): add empty flyway baseline (green)`

### Coverage gate

- [x] T014 [S2] Configure JaCoCo in `backend/pom.xml`: `prepare-agent`, `report`, and `check` bound to `verify` with rule `LINE` / `COVEREDRATIO` minimum `0.80`; exclude only `com/miguel/pokemonapp/PokemonApplication.class` with a comment explaining why (research R6). Verify: `./mvnw -B verify` passes and `backend/web/target/site/jacoco/index.html` exists. Temporarily set the minimum to `1.01`, confirm failure, restore `0.80`. ⏸ Review — `build(backend): enforce 80% line coverage with jacoco`

**Checkpoint**: S2 acceptance scenarios 1–5 pass via `./mvnw -B verify` and in CI.

---

## Phase 4: Scenario S3 — Frontend quality gates (P2)

**Goal**: Placeholder React app with lint, typecheck, test, build — and its CI job.

**Independent Test**: `cd frontend && npm ci && npm run lint && npm run typecheck && npm test && npm run build`, locally and in the `frontend` CI job.

- [ ] T015 [P] [S3] Scaffold `frontend/` with `npm create vite@latest frontend -- --template react-ts` (React 19, TS strict). Remove demo assets/content (`src/assets/react.svg`, `public/vite.svg`, counter code, `App.css`). Set `<title>Pokéfolio</title>` in `frontend/index.html`. Add `"engines": { "node": ">=24" }` and an `.nvmrc` with `24`. Verify `npm run build` and `npm run lint` pass. ⏸ Review — `chore(frontend): scaffold vite react-ts app`
- [ ] T016 [S3] Add Vitest + Testing Library: devDependencies `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`; `test` block in `frontend/vite.config.ts` (`environment: 'jsdom'`, `setupFiles: './src/test/setup.ts'`); `frontend/src/test/setup.ts` importing `@testing-library/jest-dom/vitest`; scripts `"test": "vitest run"` and `"typecheck": "tsc -b --noEmit"`. ⏸ Review — `chore(frontend): add vitest and testing library`
- [ ] T017 [S3] RED: write `frontend/src/App.test.tsx` asserting a level-1 heading with accessible name `Pokéfolio` is rendered. Confirm `npm test` fails. Do not push the RED state. ⏸ Review — `test(frontend): expect app shell heading (red)`
- [ ] T018 [S3] GREEN: implement `frontend/src/App.tsx` as a presentational placeholder (`<main>` with `<h1>Pokéfolio</h1>` and one line "Setup complete — features coming soon."); no data fetching. `npm test` passes. ⏸ Review — `feat(frontend): placeholder app shell (green)`
- [ ] T019 [S3] Add Tailwind CSS v4 via `@tailwindcss/vite` (plugin in `frontend/vite.config.ts`, `@import "tailwindcss";` in `frontend/src/index.css`); style the placeholder with a few utility classes. Run `npm run dev`, open the page, confirm zero console warnings. All four scripts pass. ⏸ Review — `chore(frontend): add tailwind css v4`
- [ ] T020 [S3] Add job `frontend` to `.github/workflows/ci.yml`: `needs: changes`, `if: needs.changes.outputs.frontend == 'true'`, `actions/setup-node` with `node-version-file: frontend/.nvmrc`, `cache: npm`, `cache-dependency-path: frontend/package-lock.json`, `working-directory: frontend`, steps `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`. Push and confirm `backend` and `frontend` run in parallel and are green. ⏸ Review — `ci: add frontend quality pipeline`

**Checkpoint**: S3 acceptance scenarios 1–2 pass; S4 scenarios 1–3 pass in the draft PR.

---

## Phase 5: Scenario S1 — Full stack locally (P1)

**Goal**: One `docker compose up` brings Postgres, backend, and Nginx-served frontend up healthy; CI builds both images.

**Independent Test**: quickstart.md steps 1–5.

**Why after S2/S3**: Compose builds the backend and frontend images, so both projects must exist first.

- [ ] T021 [P] [S1] Create `.env.example` with exactly `POSTGRES_USER=pokemon`, `POSTGRES_PASSWORD=change-me`, `POSTGRES_DB=pokemon` and a header comment "copy to .env; never commit real secrets" (contracts/env-config.md). ⏸ Review — `chore: add env example`
- [ ] T022 [P] [S1] Create `backend/Dockerfile` (multi-stage): build stage `maven:3.9-eclipse-temurin-21` running `./mvnw -B -DskipTests package` with the Maven repository cached via BuildKit mount; runtime stage `eclipse-temurin:21-jre` copying `web/target/pokemonapp-web-*.jar` as `app.jar`, non-root user, `EXPOSE 8080`. Ensure `curl` is available for the healthcheck (install it if the base image lacks it — research R8). Add `backend/.dockerignore` (`target/`, `.idea/`). Add a final step `docker build backend` to the `backend` job in `.github/workflows/ci.yml`. Verify `docker build backend` locally, then push and confirm CI. ⏸ Review — `build(backend): add multi-stage dockerfile`
- [ ] T023 [P] [S1] Create `frontend/nginx.conf`: `listen 80`; `location / { try_files $uri /index.html; }`; `location /api/ { proxy_pass http://backend:8080; }` — **no trailing slash on proxy_pass so the path is forwarded unchanged** (research R8, contracts/env-config.md); standard `proxy_set_header` for Host and X-Forwarded-*. ⏸ Review — `build(frontend): add nginx config with api proxy`
- [ ] T024 [S1] Create `frontend/Dockerfile` (multi-stage): `node:24-alpine` running `npm ci && npm run build`; runtime `nginx:alpine` copying `dist/` to `/usr/share/nginx/html` and `nginx.conf` to `/etc/nginx/conf.d/default.conf`. Add `frontend/.dockerignore` (`node_modules/`, `dist/`). Add a final step `docker build frontend` to the `frontend` job in `.github/workflows/ci.yml`. Verify `docker build frontend` locally, then push and confirm CI. ⏸ Review — `build(frontend): add multi-stage dockerfile`
- [ ] T025 [S1] Create `docker-compose.yml` (research R8): `postgres` (`postgres:16-alpine`, env `POSTGRES_USER/PASSWORD/DB` with `${VAR:?VAR is required}`, healthcheck `pg_isready`, named volume, port `5432:5432`); `backend` (build `./backend`, env `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}` and username/password from `POSTGRES_*`, `depends_on: postgres: condition: service_healthy`, healthcheck `curl -f http://localhost:8080/actuator/health`, port `8080:8080`); `frontend` (build `./frontend`, `depends_on: backend: condition: service_healthy`, port `3000:80`). Verify quickstart steps 1–5: missing-key failure, all three healthy, health `UP` with db, `503` after `docker compose stop postgres`, UI loads with zero console warnings, `/api/ping` returns the backend JSON 404, `/actuator/health` on :3000 does not return health JSON. ⏸ Review — `build: add docker compose stack`

**Checkpoint**: S1 acceptance scenarios 1–4 and the first two edge cases pass; S4 scenario 4 passes in CI.

---

## Phase 6: Scenario S4 — CI/CD pipeline (P2)

**Goal**: Images are published to GHCR on every merge to `main`; PRs never publish.

**Independent Test**: After merging the 001 PR, both images appear under the repository's Packages with tags `<sha>` and `latest`.

- [ ] T026 [S4] Add job `publish` to `.github/workflows/ci.yml` (research R9, FR-014): `needs: [backend, frontend]`; `if: github.event_name == 'push' && github.ref == 'refs/heads/main' && !failure() && !cancelled()`; job-level `permissions: contents: read, packages: write`; steps `actions/checkout`, `docker/setup-buildx-action`, `docker/login-action` (registry `ghcr.io`, username `${{ github.actor }}`, password `${{ secrets.GITHUB_TOKEN }}`), then `docker/build-push-action` twice with `push: true`, contexts `backend` and `frontend`, tags `ghcr.io/${{ github.repository_owner }}/pokemonapp-backend:${{ github.sha }}` + `:latest` and the same for `pokemonapp-frontend`, and GitHub Actions cache (`cache-from/to: type=gha`). Image names must be lowercase. On the draft PR, confirm `publish` shows as **skipped**. ⏸ Review — `ci: publish images to ghcr on main`
- [ ] T027 [S4] **User action**: mark the PR ready for review once T028–T030 are done, confirm all checks green, merge with a **merge commit**, then confirm the `publish` job ran on `main` and both packages exist in GHCR. Optionally set the packages' visibility to public so the panel can pull them. ⏸ Review

**Checkpoint**: S4 acceptance scenarios 1–5 pass.

---

## Phase 7: Scenario S5 — Onboard from documentation (P3)

**Goal**: A contributor can run and test everything from the README alone.

**Independent Test**: Follow README on a clean clone.

- [ ] T028 [S5] Write root `README.md`: CI status badge for `ci.yml`; overview (one paragraph, link to `docs/requirements.md`); architecture placeholder (module list + dependency direction); prerequisites (Docker, JDK 21, Node 24); run with Docker (`cp .env.example .env`, `docker compose up --build`, URLs :3000 and :8080/actuator/health); run locally (`docker compose up -d postgres`, `./mvnw -pl web -am spring-boot:run`, `npm run dev`); run tests (`./mvnw -B verify`, frontend scripts); CI/CD (what each job does, images at `ghcr.io/<owner>/pokemonapp-{backend,frontend}`); project workflow (Spec Kit, one branch/PR per feature). ⏸ Review — `docs: add readme skeleton`

**Checkpoint**: S5 acceptance scenarios 1–2 pass.

---

## Phase 8: Polish & cross-cutting

- [ ] T029 Run the whole of `specs/001-setup/quickstart.md` from a clean clone (`docker compose down -v` first) and record any deviation. Fix before continuing. ⏸ Review
- [ ] T030 Re-check the constitution gates in `specs/001-setup/plan.md` against the code (no forbidden dependencies: `grep -rE "security|springdoc|caffeine|wiremock|data-jpa" backend/*/pom.xml` and `grep -E "react-query|react-router|shadcn" frontend/package.json` return nothing). Mark the checklist and this file complete. ⏸ Review — `docs(specs): close 001-setup`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** → **Foundational (Phase 2)**: T006 needs T004 (wrapper) and T005; T007 needs T006
- **S2 (Phase 3)** depends on Phase 2
- **S3 (Phase 4)**: T015–T019 depend only on T001; T020 needs T006 and T019
- **S1 (Phase 5)** depends on S2 (T011, T013) and S3 (T018); T022 and T024 also extend `ci.yml`
- **S4 (Phase 6)**: T026 needs T022 and T024 (both Dockerfiles); T027 needs T028–T030
- **S5 (Phase 7)** depends on S1 and T026 (it documents the commands and images)
- **Polish (Phase 8)** depends on everything except T027 (the merge is last)

### Within each phase

- RED task before its GREEN task (T008→T009, T010→T011, T012→T013, T017→T018)
- RED states are reviewed locally and not pushed, so the draft PR stays green
- One task at a time, with a review stop after each (constitution workflow 3)

### Parallel Opportunities

- T015 (frontend scaffold) can start as soon as T001 is done
- T021, T022, T023 touch different files and can be done together
- In practice the user reviews each task before the next starts, so these are
  opportunities, not the default

## Parallel Example: Scenario S1

```bash
Task: "Create .env.example"                 # T021
Task: "Create backend/Dockerfile"           # T022
Task: "Create frontend/nginx.conf"          # T023
```

## Implementation Strategy

### MVP first

1. Phases 1–2 (T001–T007): green empty build, Spotless, CI on a draft PR.
2. Phase 3 (T008–T014): architecture, health, Flyway, coverage. **Stop and validate.**
3. Phase 4 (T015–T020): frontend gates and their CI job.
4. Phase 5 (T021–T025): the stack runs with Docker Compose. Minimum needed to start 002.
5. Phases 6–8 (T026–T030): CD to GHCR, README, final validation, then merge.

### Time box

Target: the whole of 001 on Friday 2026-10-16. If short on time, T026–T030 can
slip to the start of Saturday; CI (T006–T007) is already guarding pushes.

## Notes

- 30 tasks; every one ends in a review stop
- `[Sn]` labels map to spec.md setup scenarios, never to HU-01..HU-04
- Suggested commit messages follow Conventional Commits; the user commits
- GHCR uses only `GITHUB_TOKEN`; no repository secrets are required
