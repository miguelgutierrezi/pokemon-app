# Implementation Plan: HU-02 Detailed View

**Branch**: `003-hu02-detailed-view` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-hu02-detailed-view/spec.md`

## Summary

Deliver the public, read-only Pokémon detail API (`GET /api/v1/pokemon/{id}`)
with identity, preferred artwork image, six base stats + total, normalised
English flavor text + version, and a branching evolution **tree**. Reuse 002’s
domain helpers, `PokemonCatalogPort` / `PokeApiCatalogAdapter`, Caffeine
caches, ProblemDetail advice, and web config — **extend, do not fork**.

Traceability: HU-02, FR-POKEAPI, TR-CACHE, TR-ERRORS, TR-UNIT. Backend only;
UI is 007.

## Technical Context

**Language/Version**: Java 21; Spring Boot **4.1.1** (project parent; re-verify
at implement time against Maven Central / current Spring docs)

**Primary Dependencies** (no new libraries expected — constitution IX):

| Library | Version / source | Notes |
|---|---|---|
| Existing 002 stack | BOM 4.1.1 / pinned in parent | webmvc, validation, caffeine **3.2.4**, springdoc **3.1.1**, wiremock-standalone **3.13.2**, RestClient |
| *(none new)* | — | Evolution-chain cache is another Caffeine instance in the existing adapter |

If implement time discovers a required dependency, justify in a plan amendment
and introduce it in the first consumer task only.

**Storage**: No new tables. Postgres remains for health/Flyway only.

**Testing**: JUnit 5, AssertJ, Mockito (application); WireMock without Spring
context (infrastructure — Eevee branching, 404→not-found, evolution-chain
cache hit); `@WebMvcTest` + mocked use case (web — 400 type mismatch, 404,
502); `@SpringBootTest` IT with **wiremock-standalone** (Eevee + single-stage
+ 404). JaCoCo ≥ 80% **per module**; existing `report-aggregate` unchanged.

**Target Platform**: Same as 001/002 (Compose + CI).

**Project Type**: Backend API feature in existing monorepo.

**Performance Goals**: Cached detail ≤ 200 ms; first uncached detail ≤ 3 s
including chain nodes (Eevee worst case) when PokeAPI is healthy (spec
SC-003/SC-004).

**Constraints**:
- Clean Architecture + ArchUnit (existing rules)
- Extend `PokemonCatalogPort` + `PokeApiCatalogAdapter`; do not fork adapters
- Three sequential phases (a→b→c); **one port call per phase**; phase (a)
  parallelism is **inside the adapter** (`fetchDetail`)
- One Caffeine cache per upstream URL: pokemon → `CatalogPokemonDetail`,
  species → `CatalogSpeciesDetail`; list projections derived from them
- Upstream identity **404** → `PokemonNotFoundException` → HTTP 404 (not 502)
- Re-add `handleTypeMismatch` with a path-variable test
- Assumption: National Dex **1–1025**, species id == default Pokémon id
- No new env keys (reuse `POKEAPI_BASE_URL` / pokeapi properties from 002)

**Scale/Scope**: One detail endpoint; evolution trees up to Eevee-scale
branching (~8 children).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture | Four modules; core free of Spring/HTTP | PASS — domain/application pure; adapter in infrastructure |
| II. TDD | Red → green → refactor | PASS — see TDD ordering below |
| III. Ports & Adapters | Extend `PokemonCatalogPort` + adapter | PASS |
| IV. API Contract | `/api/v1`, DTOs, ProblemDetail, springdoc | PASS |
| V. Two-Layer Validation | `@Min(1)` on path id; domain rules for height/stats/text/tree | PASS |
| VI. Security | Reads public; no JWT yet | N/A / PASS — 006 keeps reads public |
| VII. Frontend | Out of scope | N/A |
| VIII. Quality Gates | CI + JaCoCo per module | PASS |
| IX. Simplicity | No new deps; reuse 002 wiring/caches | PASS |
| X. Traceability | Spec cites HU-02 etc.; no invented stories | PASS |

**Post-design re-check**: PASS — see research R1–R8; contracts cover detail
API; env contract documents “no new keys”; no persistence schema.

## Project Structure

### Documentation (this feature)

```text
specs/003-hu02-detailed-view/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── pokemon-detail-api.yaml
│   └── env-config.md
└── tasks.md             # /speckit-tasks — not created by /speckit-plan
```

### Source Code (repository root)

```text
backend/
├── domain/src/{main,test}/java/com/miguel/pokemonapp/domain/
│   └── pokemon/          # + PokemonDetail, PokemonStats, HeightM, FlavorText,
│                         #   ImageUrl, EvolutionNode/Link, Catalog*Detail,
│                         #   CatalogEvolutionChain (extend; keep 002 list types)
├── application/src/{main,test}/java/com/miguel/pokemonapp/application/
│   ├── port/PokemonCatalogPort.java   # + fetchDetail, fetchEvolutionChain
│   ├── pokemon/GetPokemonDetailUseCase.java  # exactly 3 port calls
│   ├── PokemonNotFoundException.java  # → 404
│   └── CatalogUnavailableException.java  # unchanged → 502
├── infrastructure/.../pokeapi/
│   └── PokeApiCatalogAdapter.java     # detail caches; chain cache; 404;
│                                      # fetchDetail parallel; list via toSummary()
├── web/src/{main,test}/java/com/miguel/pokemonapp/web/
│   ├── pokemon/          # extend PokemonController + detail DTOs
│   ├── error/            # + 404; re-add handleTypeMismatch
│   └── config/           # reuse PokeApiConfiguration (bean registration)
```

**Structure Decision**: Extend existing Clean Architecture modules; no new
Maven modules. Frontend untouched. Spring wiring remains under `web/config/`.

## TDD ordering (visible for tasks)

Work red → green per slice (separate commits at implement time):

1. **Domain** — sequential RED→GREEN pairs: `HeightM` → `FlavorText`
   normalisation → `ImageUrl` fallback → `PokemonStats` (+ total) →
   `EvolutionNode` tree build (first `evolution_details`; empty → nulls;
   `isCurrent`) → `PokemonDetail` assembly; `CatalogDetailEntry` +
   `Catalog*Detail` with `toSummary()` → 002 `CatalogPokemon` /
   `CatalogSpecies` (list behaviour unchanged — keep 002 tests green).
2. **Application** — RED→GREEN: `GetPokemonDetailUseCase` with mocked port
   — **exactly three** calls: `fetchDetail` → `fetchEvolutionChain` →
   `fetchEntries`; `PokemonNotFoundException`; propagate
   `CatalogUnavailableException`; temporary
   `UnsupportedOperationException` stubs on the adapter so `./mvnw verify`
   stays green (T013).
3. **Shared-cache REFACTOR (T014)** — **before** detail adapter work:
   pokemon/species caches store `Catalog*Detail`; list path via
   `toSummary()`; 002 list/cache tests stay green; leave T013 stubs in place.
4. **Infrastructure detail adapter** — RED→GREEN: `fetchDetail` loads
   pokemon+species **concurrently**; identity 404 →
   `PokemonNotFoundException`; evolution-chain cache (`getIfPresent` → load
   → `put`); WireMock: happy path, Eevee, 404, chain cache hit, **exactly
   one** `/pokemon/{id}` and `/pokemon-species/{id}` per detail call (phase
   c reuses phase a), failures never cached; 002 list/cache tests stay
   green.
5. **Web controller / DTOs / OpenAPI** — RED→GREEN: `GET /api/v1/pokemon/{id}`
   (`@Min(1)`), response DTOs, OpenAPI 200/400/404/502 (`@WebMvcTest` +
   mocked use case).
6. **Web advice** — RED→GREEN: `PokemonNotFoundException` → 404 problem+json;
   re-add `handleTypeMismatch` with test for `/api/v1/pokemon/abc` → 400.
7. **E2E IT** — RED→GREEN: WireMock-standalone IT (Eevee tree, single-stage,
   404, WireMock 5xx on `/pokemon/{id}` → HTTP 502 problem+json without
   upstream URL, optional cache zero-extra-calls).
8. **Ops / polish** — README detail section; quickstart timings (manual
   SC-003/SC-004); no new env keys unless forced.

## Complexity Tracking

> No constitution violations requiring justification.
