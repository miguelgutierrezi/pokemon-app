<!--
Sync Impact Report
- Version change: 1.0.0 → 1.1.0 (MINOR: new principle + materially
  expanded sections; no principle removed or redefined incompatibly)
- Modified principles:
  VIII. Quality Gates & Delivery — "one branch/PR per user story" →
        "one branch/PR per feature"; branch naming convention added
  IX. Simplicity — dependencies/config enter only with the feature that
      first uses them
- Added principles:
  X. Requirements Traceability (PDF is source of truth; HU-01..HU-04
     implemented as written)
- Added / expanded sections:
  Technology Stack — full stack listed (was partial)
  Development Workflow — feature map, task-by-task review gate,
     never mark done with failing tests
  Deliverables — README, Dockerfiles, seed data, docs/genai
- Removed sections: N/A
- Templates requiring updates: none (spec/plan templates are generic;
  traceability is enforced via each spec's "Requirements Traceability"
  section)
- Follow-up TODOs: none
-->

# Pokémon App Constitution

## Core Principles

### I. Clean Architecture
The backend MUST be a Maven multi-module project with modules
`domain`, `application`, `infrastructure`, and `web`. Dependencies
MUST point inward only: outer modules may depend on inner ones;
inner modules MUST NOT depend on outer ones. Modules `domain` and
`application` MUST have zero Spring, JPA, or HTTP dependencies.
Architectural boundaries MUST be enforced with ArchUnit tests that
fail the build on violations. Rationale: keeps business logic
framework-agnostic, testable, and replaceable at the edges.

### II. Test-Driven Development (TDD)
Production code MUST follow red–green–refactor: write a failing
test first, implement the minimum code to pass, then refactor.
Each of those steps MUST be committed separately. No production
code MAY be merged without a corresponding test. Rationale:
tests define behavior before implementation and preserve a
reviewable history of design decisions.

### III. Ports & Adapters
Use cases in `application` MUST depend only on interfaces (ports).
Concrete implementations—PokeAPI HTTP client, JPA repositories,
cache, and JWT—MUST live as adapters in `infrastructure`.
Rationale: external systems and frameworks remain swappable
without changing domain or application logic.

### IV. API Contract
The HTTP API MUST be RESTful, using standard HTTP verbs and status
codes. Business endpoints MUST live under the `/api/v1` prefix in
the backend itself (proxies MUST NOT rewrite paths). Controllers
MUST expose and accept DTOs only; JPA entities MUST NEVER leak
outside the persistence adapter. Errors MUST be returned as
RFC 7807 `ProblemDetail` via a global `@RestControllerAdvice`. The
API MUST be documented with OpenAPI through springdoc. Rationale: a
stable, machine-readable contract and consistent error shape for
clients and interview reviewers.

### V. Two-Layer Validation
Input shape and constraints MUST be validated with Bean Validation
at the web boundary. Business invariants and domain rules MUST be
enforced inside the domain model. Rationale: transport concerns
stay at the edge; domain integrity cannot be bypassed by a
different adapter.

### VI. Security
Authentication and authorization MUST use Spring Security with JWT.
Read routes MUST be public; write routes MUST be protected.
Passwords MUST be hashed with BCrypt. Secrets (keys, credentials,
tokens) MUST NOT appear in source code; they MUST come from
environment variables or equivalent external configuration.
Rationale: least privilege by default and no credential leakage
in the repository.

### VII. Frontend Standards
The frontend MUST use React, TypeScript, and Vite. Server state
MUST be managed with TanStack Query. Presentational components
MUST NOT fetch data directly. The UI MUST be responsive and
accessible, and the browser console MUST show zero warnings under
normal use. Rationale: clear separation of concerns, predictable
data flow, and a professional UX baseline for the interview.

### VIII. Quality Gates & Delivery
CI MUST pass—tests, lint, format, and backend line coverage ≥ 80%—
before any merge. Each feature (see the feature map in Development
Workflow) MUST use exactly one branch and one pull request, named
`NNN-<short-name>` and matching its `specs/NNN-<short-name>/`
folder. Merges MUST use merge commits (no squash). Rationale:
quality is automated and feature history remains traceable in
main.

### IX. Simplicity
Features MUST NOT be added speculatively. Prefer readable,
straightforward code over clever abstractions. YAGNI applies:
implement only what the current feature requires. Dependencies,
configuration keys, and scaffolding MUST be introduced by the
feature that first exercises them, not earlier. Rationale:
interview code is judged on clarity and correctness, not on unused
generality.

### X. Requirements Traceability
`docs/requirements.pdf` (transcribed with stable IDs in
`docs/requirements.md`) is the single source of truth for scope.
User stories HU-01..HU-04 MUST be implemented exactly as written:
they MUST NOT be added to, renamed, merged, or extended with
capabilities the document does not ask for. Every spec MUST
include a "Requirements Traceability" section citing the
requirement IDs it covers; developer-facing scenarios in enabler
features (setup, auth, frontend) MUST NOT be labelled "user
stories". Rationale: the evaluation is against that document; scope
drift costs time and credibility.

## Technology Stack

- **Backend**: Java 21, latest GA Spring Boot, Maven multi-module
  (`domain`, `application`, `infrastructure`, `web`)
- **Persistence**: PostgreSQL, Spring Data JPA (adapter only),
  Flyway migrations
- **External data**: PokeAPI via an infrastructure HTTP adapter,
  cached with Caffeine
- **API docs / errors**: springdoc OpenAPI; RFC 7807 ProblemDetail
- **Security**: Spring Security, JWT, BCrypt; secrets via env vars
- **Backend testing & quality**: JUnit 5, Mockito, AssertJ,
  Testcontainers, WireMock, ArchUnit, Spotless, JaCoCo (≥ 80% lines)
- **Frontend**: React, TypeScript, Vite, Tailwind CSS, shadcn/ui,
  TanStack Query, React Router, ESLint, Vitest + Testing Library;
  UI designed in Figma
- **Delivery**: Dockerfile per project (frontend served by Nginx),
  `docker-compose.yml` at the root, GitHub Actions CI with parallel
  jobs and path filters

## Development Workflow

1. Feature map (one branch → one PR → merge commit into main, no
   squash):
   `001-setup`, `002-hu01-pokemon-enumeration`,
   `003-hu02-detailed-view`, `004-hu03-data-sync`,
   `005-hu04-local-update`, `006-auth`, `007-frontend`.
2. Implement with TDD: failing test commit → minimal green commit →
   refactor commit as needed.
3. Work task by task; stop for human review after each task before
   starting the next one.
4. A task MUST NOT be marked done while any test, lint, or format
   check fails.
5. Do not merge until CI is green: tests, lint, format, and
   backend coverage ≥ 80%.
6. PRs MUST be reviewed for constitution compliance (traceability,
   architecture direction, DTO-only API, ports vs adapters, frontend
   data fetching rules, secrets handling).
7. Spec Kit artifacts (`spec.md`, `plan.md`, `tasks.md`) guide
   implementation; this constitution overrides conflicting
   practices.

## Deliverables

- Root `README.md` with environment setup and technical
  documentation (DEL-README).
- A Dockerfile per project and a working `docker-compose.yml`
  (DEL-DOCKER).
- Seeded demonstration data and mock credentials (DEL-SEED).
- `docs/genai/`: the prompt used for the task-management API, a
  representative output sample, and how it was validated and
  corrected (GENAI).

## Governance

This constitution supersedes informal conventions and conflicting
guidance in specs, plans, or ad-hoc decisions. Amendments MUST be
documented in `.specify/memory/constitution.md` with an updated
Sync Impact Report, semantic version bump, and rationale.

Versioning policy:

- **MAJOR**: removal or incompatible redefinition of a principle
- **MINOR**: new principle/section or materially expanded guidance
- **PATCH**: clarifications, wording, or non-semantic refinements

Compliance: every PR and review MUST verify applicable principles.
Complexity beyond these rules MUST be justified in the PR
description. Runtime development guidance comes from Spec Kit
commands (`/speckit-specify`, `/speckit-plan`, `/speckit-tasks`,
`/speckit-implement`) operating under this constitution.

**Version**: 1.1.0 | **Ratified**: 2026-10-15 | **Last Amended**: 2026-10-15
