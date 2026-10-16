# Implementation Plan: Setup (project skeleton)

**Branch**: `001-setup` | **Date**: 2026-10-15 (revised) | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-setup/spec.md`

## Summary

Deliver a green, runnable monorepo skeleton with no Pokémon capability:
a Maven multi-module Spring Boot backend (Clean Architecture enforced by
ArchUnit), a Vite/React/TypeScript placeholder frontend, Docker Compose
(Postgres + backend + Nginx-served frontend), GitHub Actions CI, a
`.env.example`, and a README skeleton. Acceptance: `docker compose up`,
`GET :8080/actuator/health` → `UP` with `db` UP, UI on `:3000` with zero
console warnings, `/api/**` proxied untouched, `mvn verify` green (Spotless,
ArchUnit, JaCoCo ≥ 80%), Flyway `V1` applied, green CI.

Traceability: see the spec's *Requirements Traceability* table (TR-REPO,
DEL-DOCKER, DEL-README, EVAL-ARCH, TR-TESTS tooling). No HU is delivered.

## Technical Context

**Language/Version**: Java 21 (backend); TypeScript 5.x on Node 24 LTS
(frontend)

**Primary Dependencies** (only what 001 exercises — constitution IX):
- Backend: Spring Boot **4.1.1** (latest GA on Maven Central, verified
  2026-10-15), `spring-boot-starter-web`, `-actuator`, `-data-jpa`,
  `-flyway` + `flyway-database-postgresql`, PostgreSQL JDBC driver
- Frontend: React 19, Vite, Tailwind CSS v4 (`@tailwindcss/vite`), ESLint
  (flat config)
- Ops: Docker Compose, Nginx (frontend image), GitHub Actions

**Deferred on purpose**: Spring Security/JWT → 006; validation, springdoc
(3.x line for Boot 4), ProblemDetail advice, Caffeine, WireMock → 002;
TanStack Query, React Router, shadcn/ui → 007.

**Storage**: PostgreSQL 16 (Compose; Testcontainers in tests)

**Testing**: `spring-boot-starter-test` (JUnit 5, AssertJ, Mockito),
Testcontainers 2.x PostgreSQL module with `@ServiceConnection`, ArchUnit 1.5.x;
Spotless (google-java-format); JaCoCo line gate ≥ 80%. Frontend: Vitest +
Testing Library (jsdom).

**Target Platform**: Linux containers locally and in CI; developers on
macOS/Linux with Docker

**Project Type**: Full-stack web monorepo (`backend/` + `frontend/`)

**Performance Goals**: Stack healthy within 5 minutes after images are cached
(SC-001); no load targets

**Constraints**:
- Inward-only module deps; `domain`/`application` free of Spring/JPA/HTTP;
  ArchUnit enforced
- Secrets only via env; `.env` git-ignored
- Proxy forwards `/api/**` without rewriting; actuator not reachable through
  the proxy
- Zero browser console warnings on the placeholder

**Scale/Scope**: Skeleton only — health endpoint, placeholder UI, Compose,
CI, docs.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture | Four modules; ArchUnit; core free of Spring/JPA/HTTP | PASS — ArchUnit suite in `web` tests (R3) |
| II. TDD | Red → green → refactor commits | PASS — each test is committed failing first; ArchUnit rule proven with a temporary violating probe (R3) |
| III. Ports & Adapters | Ports in application; adapters in infrastructure | N/A in 001 — no ports yet (none needed) |
| IV. API Contract | `/api/v1` in backend, no path rewrite; DTOs; ProblemDetail; springdoc | PASS — proxy does not rewrite (R8); ProblemDetail/springdoc deferred to 002 with the first controller |
| V. Two-Layer Validation | Bean Validation at edge; domain rules inside | N/A in 001 — no input |
| VI. Security | Spring Security + JWT; secrets via env | N/A in 001 — deferred to 006; only DB credentials, via env |
| VII. Frontend Standards | React/TS/Vite; zero warnings | PASS — placeholder; TanStack Query arrives with the first fetch (007) |
| VIII. Quality Gates | CI tests/lint/format/coverage; branch `001-setup` | PASS — R9 |
| IX. Simplicity | Nothing speculative; deps with their first user | PASS — deferred list above |
| X. Traceability | Spec cites requirement IDs; no invented stories | PASS — spec traceability table; scenarios are S1–S5, not user stories |

**Post-design re-check**: PASS. Contracts limited to health + env keys; no
business tables; Complexity Tracking empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-setup/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── health-api.yaml
│   └── env-config.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks — not created yet
```

### Source Code (repository root)

```text
.
├── backend/
│   ├── pom.xml                          # parent: modules, BOM, plugin mgmt (Spotless, JaCoCo)
│   ├── domain/                          # pom only (+ package-info) in 001
│   ├── application/                     # pom only (+ package-info) in 001
│   ├── infrastructure/
│   │   └── src/main/resources/db/migration/V1__baseline.sql
│   ├── web/                             # only bootable module
│   │   ├── src/main/java/.../PokemonApplication.java
│   │   ├── src/main/resources/application.yml
│   │   └── src/test/java/.../           # ArchitectureTest, HealthEndpointIT (Testcontainers)
│   └── Dockerfile                       # multi-stage Maven → JRE 21
├── frontend/
│   ├── package.json                     # scripts: dev, lint, typecheck, test, build
│   ├── vite.config.ts                   # + Vitest config
│   ├── src/main.tsx, src/App.tsx        # placeholder
│   ├── src/App.test.tsx
│   ├── nginx.conf                       # SPA fallback + /api/ proxy (no rewrite)
│   └── Dockerfile                       # multi-stage Node build → Nginx
├── docs/requirements.pdf, docs/requirements.md
├── docker-compose.yml                   # postgres, backend, frontend
├── .env.example
├── .gitignore
├── .github/workflows/ci.yml
└── README.md
```

**Structure Decision**: Monorepo with Clean Architecture modules under
`backend/`. `web` is the only Spring Boot application module. Compose, CI, and
docs live at the root. Base Java package: to be confirmed before tasks
(proposal: `com.pokemonapp`).

## Complexity Tracking

> No constitution violations requiring justification.
