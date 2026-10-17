# Implementation Plan: HU-01 Pokemon Enumeration

**Branch**: `002-hu01-pokemon-enumeration` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-hu01-pokemon-enumeration/spec.md`

## Summary

Deliver the public, read-only, paginated National Dex list API
(`GET /api/v1/pokemon`) backed by PokeAPI, with domain rules for weight,
English name/genus, paging bounds, RFC 7807 errors, springdoc OpenAPI, and
programmatic Caffeine caches reusable by 003. Backend only; UI is 007.

Traceability: HU-01, FR-POKEAPI, TR-CACHE, TR-ERRORS, TR-UNIT.

## Technical Context

**Language/Version**: Java 21; Spring Boot **4.1.1** (project parent; GA on
Maven Central, verified 2026-10-16)

**Primary Dependencies** (verified 2026-10-16 — constitution IX: first use):

| Library | Version / source | Notes |
|---|---|---|
| `spring-boot-starter-webmvc` | BOM 4.1.1 | Already in `web` (not deprecated `-web`) |
| `spring-boot-starter-validation` | BOM 4.1.1 | Bean Validation at controller |
| `com.github.ben-manes.caffeine:caffeine` | BOM **3.2.4** | Programmatic caches in infrastructure — **not** `spring-boot-starter-cache` / `@Cacheable` |
| `springdoc-openapi-starter-webmvc-ui` | **3.1.1** | Boot 4 line; **first web task = spike** (see TDD ordering) |
| `org.wiremock:wiremock` | **3.13.2** | OK for infrastructure (no Spring context) |
| `org.wiremock:wiremock-standalone` | **3.13.2** | **Required for web IT** (avoids Jetty clashes with Boot) |
| Spring `RestClient` | Framework via webmvc | HTTP client for PokeAPI |

**Storage**: No new tables. Postgres remains for health/Flyway only.

**Testing**: JUnit 5, AssertJ, Mockito (application); WireMock without Spring
context (infrastructure — prove cache: two identical calls → one upstream);
`@WebMvcTest` / RestTestClient + mocked use case (web); one
`@SpringBootTest` IT with **wiremock-standalone** as PokeAPI. JaCoCo ≥ 80%
**per module** (R6); `report-aggregate` in `web` for visibility only.

**Target Platform**: Same as 001 (Compose + CI).

**Project Type**: Backend API feature in existing monorepo.

**Performance Goals**: Cached page ≤ 200 ms; first uncached page ≤ 3 s when
PokeAPI is healthy (spec SC-003/SC-004). Max `size=20` → ≤ 40 upstream GETs
per uncached page.

**Constraints**:
- Clean Architecture + ArchUnit (existing rules)
- Ports in `application`; adapter + programmatic Caffeine in `infrastructure`
- Bean Validation at edge; domain rules in domain
- No auth (006); reads public
- `POKEAPI_BASE_URL` default in `application.yml`; Compose
  `${POKEAPI_BASE_URL:-https://pokeapi.co/api/v2}`
- Assumption: for National Dex ids **1–1025**, species id == default Pokémon id

**Scale/Scope**: One list endpoint; ~1025 National Dex species (live count).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture | Four modules; core free of Spring/HTTP | PASS — domain/application pure; adapter in infrastructure |
| II. TDD | Red → green → refactor | PASS — see TDD ordering below / tasks will encode commits |
| III. Ports & Adapters | `PokemonCatalogPort` + `PokeApiCatalogAdapter` (+ optional caching decorator) | PASS |
| IV. API Contract | `/api/v1`, DTOs, ProblemDetail, springdoc 3.1.1 (spike-gated) | PASS |
| V. Two-Layer Validation | `@Min`/`@Max` on query params; domain page-beyond-last + mapping rules | PASS |
| VI. Security | Reads public; no JWT yet | N/A / PASS — 006 keeps reads public |
| VII. Frontend | Out of scope | N/A |
| VIII. Quality Gates | CI + JaCoCo per module | PASS |
| IX. Simplicity | Deps enter with first use (validation, caffeine, springdoc, WireMock) — no starter-cache | PASS |
| X. Traceability | Spec cites HU-01 etc.; no invented stories | PASS |

**Post-design re-check**: PASS — see research R1–R9; contracts cover list API
+ env keys; no persistence schema.

## Project Structure

### Documentation (this feature)

```text
specs/002-hu01-pokemon-enumeration/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── pokemon-list-api.yaml
│   └── env-config.md
└── tasks.md             # /speckit-tasks — not created by /speckit-plan
```

### Source Code (repository root)

```text
backend/
├── domain/src/{main,test}/java/com/miguel/pokemonapp/domain/
│   ├── pokemon/          # PokemonSummary, AbilityRef, weight/name/genus rules
│   └── page/             # PageRequest, Page, PageOutOfRangeException
├── application/src/{main,test}/java/com/miguel/pokemonapp/application/
│   ├── port/PokemonCatalogPort.java
│   ├── pokemon/ListPokemonUseCase.java
│   └── CatalogUnavailableException.java   # web maps → 502
├── infrastructure/src/{main,test}/java/com/miguel/pokemonapp/infrastructure/
│   └── pokeapi/          # RestClient, programmatic Caffeine, adapter
├── web/src/{main,test}/java/com/miguel/pokemonapp/web/
│   ├── pokemon/          # PokemonController, DTOs
│   ├── error/            # ProblemDetail @RestControllerAdvice
│   └── ...
├── pom.xml               # dependencyManagement pins (springdoc, wiremock*)
.env.example              # + POKEAPI_BASE_URL
docker-compose.yml        # POKEAPI_BASE_URL with default
```

**Structure Decision**: Extend existing Clean Architecture modules; no new
Maven modules. Frontend untouched.

## TDD ordering (visible for tasks)

Work red → green per slice (separate commits at implement time):

1. **Domain** — RED: weight / English name / genus / `PageRequest`+beyond-last
   tests → GREEN: pure Java value objects + rules.
2. **Application** — RED: `ListPokemonUseCase` with mocked
   `PokemonCatalogPort` (`speciesPage` + per-id fetches) → GREEN:
   orchestration; `CatalogUnavailableException` in application.
3. **Infrastructure** — RED: WireMock stubs; assert two identical page/id
   requests hit upstream once when cache warm; failures not cached → GREEN:
   RestClient + parallel fetch + programmatic Caffeine (no Spring context).
4. **Web spike (FIRST web task)** — add springdoc **3.1.1**, boot the app,
   assert `GET /v3/api-docs` → **200**. If it fails, **stop and decide**
   before any controller work.
5. **Web** — RED: controller slice (validation 400, happy path DTO) and advice
   (`CatalogUnavailableException` → 502) → GREEN: controller, DTOs, advice.
6. **IT** — RED/GREEN: one end-to-end `@SpringBootTest` with
   **wiremock-standalone**; assert page shape + ProblemDetail on upstream
   failure.
7. **Ops** — `.env.example`, Compose default for `POKEAPI_BASE_URL`, JaCoCo
   `report-aggregate` in `web` (visibility only).

## Complexity Tracking

> No constitution violations requiring justification.
