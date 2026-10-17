# Feature Specification: Setup (project skeleton)

**Feature Branch**: `001-setup`

**Created**: 2026-10-15

**Status**: Draft (revised 2026-10-15 against `docs/requirements.pdf`)

**Input**: "Feature 001 - Setup. No business features; only the skeleton so
every later feature starts from a green, runnable baseline. Monorepo with
backend, frontend, Docker Compose, CI, env example, and README skeleton."

## Requirements Traceability *(mandatory — constitution X)*

This is an **enabler feature**. It implements **none** of the user stories
HU-01..HU-04; those are delivered by features 002–005. The scenarios below
are developer-facing setup scenarios (S1–S5), deliberately **not** labelled
"user stories".

| Requirement ID (`docs/requirements.md`) | What 001 delivers | Completed by |
|---|---|---|
| TR-REPO | Public monorepo layout, `.gitignore`, no secrets committed | — |
| DEL-DOCKER | Multi-stage Dockerfile for backend and frontend; root `docker-compose.yml`; images published to GHCR on merge to `main` | — |
| DEL-README / SUB-README | README skeleton: overview, run locally, run with Docker, run tests | Each later feature extends it |
| EVAL-ARCH / TR-CORE (structure only) | Four Maven modules; ArchUnit rules enforcing inward-only dependencies | 002–005 add code |
| TR-TESTS / TR-UNIT / EVAL-TEST (tooling only) | Test tooling, JaCoCo 80% line gate, CI | 002–007 add tests |
| EVAL-FUNC (console) | Placeholder UI with zero browser console warnings | 007 |

**Explicitly not delivered here**: any Pokémon capability (HU-01..HU-04),
PokeAPI client or cache, authentication (TR-API-AUTH), user/Pokémon tables
(TR-DB), seed data (DEL-SEED), error-handling advice (TR-ERRORS), OpenAPI
docs, GenAI section.

## User Scenarios & Testing *(mandatory)*

### Setup Scenario S1 - Start the full stack locally (Priority: P1)

A developer clones the repository and runs the whole stack with one command
to confirm the baseline works before any business feature is built.

**Why this priority**: Every later feature depends on a runnable baseline.

**Independent Test**: From a clean checkout with the example environment
copied, start the composed stack; the API health endpoint reports healthy and
the UI placeholder loads.

**Acceptance Scenarios**:

1. **Given** a clean checkout with configuration copied from the example
   environment file, **When** the developer starts the composed stack,
   **Then** the database, API, and UI services all become available.
2. **Given** the stack is running, **When** the developer requests the API
   health endpoint directly, **Then** it reports status UP, including the
   database component.
3. **Given** the stack is running, **When** the developer opens the UI at the
   documented local address, **Then** a placeholder page loads and the
   browser console shows no warnings.
4. **Given** the UI is served by the composed stack, **When** the browser
   requests a path under `/api/`, **Then** the request reaches the backend
   with its path unchanged (observable as a backend-generated JSON 404 while
   no business endpoints exist).

---

### Setup Scenario S2 - Backend quality gates (Priority: P1)

A developer runs backend verification and confirms that architecture
boundaries, formatting, migrations, and the coverage gate pass.

**Why this priority**: The constitution requires inward-only dependencies
enforced by ArchUnit and ≥ 80% line coverage; the gate must exist before the
first business code.

**Independent Test**: Run full backend verification from the backend root;
it succeeds.

**Acceptance Scenarios**:

1. **Given** a clean backend project, **When** full verification runs,
   **Then** it completes successfully.
2. **Given** the architecture tests, **When** `domain` or `application`
   depends on Spring, JPA, Servlet/HTTP, or on `infrastructure`/`web`,
   **Then** verification fails.
3. **Given** the coverage gate, **When** backend line coverage is below 80%,
   **Then** verification fails.
4. **Given** a fresh PostgreSQL database, **When** the application starts (in
   verification or Compose), **Then** the baseline migration is applied.
5. **Given** a Java source file that is not formatted, **When** verification
   runs, **Then** it fails.

---

### Setup Scenario S3 - Frontend quality gates (Priority: P2)

A developer lints, type-checks, tests, and builds the placeholder frontend.

**Why this priority**: Frontend tooling must exist before UI work in 007;
lower than S1/S2 because there is no behavior yet.

**Independent Test**: Run lint, typecheck, tests, and production build in the
frontend project; all succeed.

**Acceptance Scenarios**:

1. **Given** the frontend project, **When** lint, typecheck, and unit tests
   run, **Then** all succeed.
2. **Given** the frontend project, **When** a production build runs, **Then**
   it succeeds.

---

### Setup Scenario S4 - CI/CD pipeline (Priority: P2)

A developer opens a pull request and automated checks validate the backend,
the frontend, and the container images; once merged to `main`, the images are
published to a container registry.

**Why this priority**: The constitution forbids merging without green CI.

**Independent Test**: Open the 001 PR; all workflow jobs run and pass.

**Acceptance Scenarios**:

1. **Given** a PR changing backend files, **When** CI runs, **Then** a backend
   job runs full verification with dependency caching.
2. **Given** a PR changing frontend files, **When** CI runs, **Then** a
   frontend job runs lint, typecheck, tests, and build with dependency
   caching.
3. **Given** a PR changing both, **When** CI runs, **Then** both jobs run in
   parallel.
4. **Given** a PR changing a project, **When** CI runs, **Then** that
   project's container image is built successfully.
5. **Given** a push to `main` whose CI jobs passed, **When** the pipeline
   finishes, **Then** backend and frontend images are published to GitHub
   Container Registry tagged with the commit SHA and `latest`. Pull requests
   never publish images.

---

### Setup Scenario S5 - Onboard from documentation (Priority: P3)

A new contributor (or the interview panel) reads the README and can run and
test the project.

**Why this priority**: Required deliverable, but does not block S1–S4.

**Independent Test**: Follow only the README to run the stack and the test
suites.

**Acceptance Scenarios**:

1. **Given** the root README, **When** a contributor reads it, **Then** it
   contains an overview, a link to `docs/requirements.md`, an architecture
   placeholder, how to run locally, how to run with Docker, and how to run
   tests.
2. **Given** the example environment file, **When** a contributor configures
   the environment, **Then** every key needed in this feature is listed with
   placeholder values and no real secret is committed.

---

### Edge Cases

- A required environment variable is missing at startup → Compose refuses to
  start (or the backend fails fast) with a message naming the variable.
- The database is unreachable when the API starts → the API does not report
  healthy and Compose shows the backend as unhealthy/restarting, not a silent
  hang.
- A PR touches only backend or only frontend files → the unrelated job is
  skipped by path filters.
- Someone adds a framework dependency to `domain` or `application` →
  architecture tests fail verification.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The repository MUST be a monorepo with a distinct backend
  project, a distinct frontend project, and root-level Compose, CI, and
  documentation.
- **FR-002**: Backend core modules (`domain`, `application`) MUST NOT depend
  on web, persistence, or infrastructure code; an automated architecture test
  in the verification suite MUST prove it.
- **FR-003**: The backend MUST expose a health endpoint reporting healthy only
  when the application and its database are available. It MUST NOT be
  reachable through the UI's `/api/` proxy.
- **FR-004**: The backend MUST include database migration tooling with one
  baseline migration (empty baseline allowed).
- **FR-005**: Backend verification MUST include tests, a formatting check,
  and a line-coverage gate of at least 80%.
- **FR-006**: The frontend MUST provide a placeholder page that confirms the
  app shell loads, with no business features.
- **FR-007**: The frontend MUST provide lint, typecheck, unit tests, and a
  production build as runnable checks.
- **FR-008**: A single Compose definition MUST start database, backend, and
  frontend; the frontend container MUST serve the built UI and forward
  `/api/` requests to the backend without rewriting the path.
- **FR-009**: Backend and frontend MUST each have a multi-stage container
  build used by Compose and CI.
- **FR-010**: CI MUST run backend and frontend checks as parallel jobs gated
  by path filters, with dependency caching, and MUST build the container
  image of each changed project.
- **FR-011**: An example environment file MUST list every configuration key
  used in this feature (database settings only) with placeholder values; the
  real `.env` MUST be git-ignored.
- **FR-012**: Root documentation MUST cover overview, link to the
  requirements, architecture placeholder, local run, Docker run, and tests.
- **FR-013**: This feature MUST NOT deliver any HU-01..HU-04 capability, nor
  authentication, PokeAPI access, caching, error-handling advice, or API
  documentation; dependencies and configuration for those arrive with the
  feature that first uses them (constitution IX).
- **FR-014**: On pushes to `main`, after the CI jobs pass, the pipeline MUST
  publish the backend and frontend images to GitHub Container Registry using
  only the workflow's built-in token (no additional secrets).

### Key Entities

- **Environment configuration**: database connection settings supplied via
  environment and documented by the example file.
- **Baseline schema migration**: first schema version; creates no business
  tables.
- **Health status**: readiness signal used by operators and Compose health
  checks.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From a clean checkout, one Compose command brings up a healthy
  API and a loadable UI within 5 minutes (excluding first-time image
  downloads).
- **SC-002**: The API health check reports healthy when the stack is up and
  not healthy when the database is down.
- **SC-003**: The placeholder UI loads at the documented local address with
  zero browser console warnings.
- **SC-004**: Backend verification passes on a clean machine, including
  architecture checks, formatting, and the 80% line-coverage gate.
- **SC-005**: Frontend lint, typecheck, tests, and build pass on a clean
  machine.
- **SC-006**: The 001 pull request shows a green CI pipeline, and after its
  merge both images are available in GitHub Container Registry.
- **SC-007**: A new contributor can run the stack and the test suites using
  only the README.

## Assumptions

- Stack per constitution: Java 21 + latest GA Spring Boot, Maven multi-module
  (`web` is the only bootable module); PostgreSQL + Flyway; React +
  TypeScript + Vite + Tailwind, ESLint, Vitest + Testing Library; frontend
  served by Nginx.
- Only dependencies exercised in this feature are added now (web, actuator,
  JDBC, Flyway, PostgreSQL driver; tests: JUnit 5, AssertJ,
  Testcontainers, ArchUnit). Security/JWT (006), validation, springdoc,
  ProblemDetail advice, Caffeine, WireMock (002+), and TanStack Query, React
  Router, shadcn/ui (007) are deliberately deferred.
- Business endpoints will be served by the backend under `/api/v1`
  (constitution IV); the proxy forwards `/api/` untouched.
- Docker and Docker Compose are available locally and on CI runners.
- Merge policy (one branch/PR, merge commits) is a team process; CI only
  reports status.
