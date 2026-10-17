# Research: HU-02 Detailed View

**Feature**: `003-hu02-detailed-view` | **Date**: 2026-10-16
(revised after Miguel’s plan review)

Versions and Spring / Jackson APIs must be re-checked against current docs at
implement time (same rule as 002). No new dependencies planned.

## R1. Catalog records + one cache per upstream URL

**Decision**: Caches store the **rich** projections:

| Cache | Key | Value |
|---|---|---|
| `pokeapi-pokemon` | national id | `CatalogPokemonDetail` |
| `pokeapi-species` | national id | `CatalogSpeciesDetail` |
| `pokeapi-species-list` | `offset:limit` | unchanged (002) |
| `pokeapi-evolution-chain` | chain id | `CatalogEvolutionChain` |

`CatalogPokemon` / `CatalogSpecies` (002 list path) are **derived** from the
detail records (e.g. `CatalogPokemonDetail.toSummary()` /
`CatalogSpeciesDetail.toSummary()`), never loaded into a second cache for the
same URL. `fetchEntries` / list assembly call those derivations after a cache
hit or load.

**Rationale**: Same HTTP resource → one cache entry; phase (c) reuses the
detail entry loaded in phase (a) for the requested id (WireMock: exactly one
GET each for `/pokemon/{id}` and `/pokemon-species/{id}` on a detail call).
002 list tests stay green because public list behaviour is unchanged.

**Alternatives considered**: Separate list vs detail caches for the same URL
(rejected: duplicate storage and double upstream on detail→list); keeping
list-shaped cache values and a parallel detail cache (rejected: same
mistake).

## R2. Port operations — three calls, parallelism in the adapter

**Decision**: Extend `PokemonCatalogPort`:

| Operation | Phase | Notes |
|---|---|---|
| `fetchDetail(id)` → `CatalogDetailEntry(CatalogPokemonDetail, CatalogSpeciesDetail)` | (a) | Adapter fetches `/pokemon/{id}` and `/pokemon-species/{id}` **concurrently**; either identity 404 → `PokemonNotFoundException` |
| `fetchEvolutionChain(chainId)` | (b) | New evolution-chain cache |
| `fetchEntries(ids)` | (c) | Unchanged signature; reads pokemon/species caches (detail → `toSummary()`) |

Use case orchestration — **exactly three** sequential port calls:

1. `fetchDetail(id)`
2. `fetchEvolutionChain(entry.species().evolutionChainId())`
3. `fetchEntries(allDistinctChainSpeciesIds)` — requested id already cached
   from (a)

**Rationale**: Repeats the 002 lesson — do **not** expose
`fetchPokemon` + `fetchSpecies` (or detail twins) so the use case fans out;
parallelism belongs in the adapter for one logical “entry” operation.

**Alternatives considered**: `fetchPokemonDetail` + `fetchSpeciesDetail` on
the port (rejected: same 002 port mistake); single mega fan-out including
chain (impossible — chain URL depends on species).

## R3. Marking the current Pokémon in the tree — `isCurrent`

**Decision**: Each `EvolutionNode` carries `boolean isCurrent`, set `true`
iff `nationalNumber == requestedId` when the domain builds the tree.

**Rationale**: UI/clients can highlight any node while rendering the tree
without threading the detail id separately; works for deep trees and for
requesting a mid-chain species (e.g. Ivysaur).

**Alternatives considered**: `currentNationalNumber` only on `PokemonDetail`
(rejected: every consumer must compare); both (redundant).

## R4. Evolution edge — first `evolution_details` only

**Decision**: For each parent→child link, read `evolution_details[0]`:

| Field | Source |
|---|---|
| `trigger` | `trigger.name` slug (or null) |
| `minLevel` | `min_level` (or null) |
| `itemSlug` | `item.name` when present (or null) |

If `evolution_details` is **empty**, all three are **null** (documented).

**Rationale**: Spec / Miguel review; PokeAPI often lists multiple mutually
exclusive conditions — “first” is a stable, testable rule for HU-02.

## R5. Upstream 404 vs 502

**Decision**:

| Upstream | Application signal | HTTP |
|---|---|---|
| 404 on `/pokemon/{id}` or `/pokemon-species/{id}` (inside `fetchDetail`) | `PokemonNotFoundException` | **404** |
| 404 on `/evolution-chain/{id}` after a valid species pointed at it | `CatalogUnavailableException` | **502** |
| Transport / timeout / 5xx / malformed required fields | `CatalogUnavailableException` | **502** |

**Rationale**: Spec FR-008/FR-009; identity 404 is “no such National Dex
Pokémon”; a missing chain after a valid species pointer is catalog integrity
failure, not a client id mistake.

**Alternatives considered**: Mapping chain 404 to 404 (rejected).

## R6. Evolution-chain Caffeine cache

**Decision**: Fourth programmatic cache (see R1 table). Same
`getIfPresent` → load outside lock → `put` on success; failures never
cached; TTL from `pokemonapp.pokeapi.cache.ttl`; max size align with
id-tier (**2048**) unless tasks choose otherwise.

**Rationale**: TR-CACHE; many species share chains.

## R7. Image, height, stats, flavor text (domain)

**Decision**:

- **ImageUrl**: prefer official artwork URL, else `front_default`, else null.
- **HeightM**: `decimetres / 10` → `BigDecimal` scale 1; reject negatives.
- **PokemonStats**: six named base stats + `total` = sum (domain).
- **FlavorText**: first English `flavor_text_entries` entry; normalise
  `\f`, `\n`, `\r`, whitespace runs → single space; trim; null text+version
  if no English entry.

**Rationale**: Spec FR-003–FR-005 / SC-006; unit-testable without HTTP.

## R8. Web errors and OpenAPI

**Decision**:

- Path: `@PathVariable @Min(1) int id` (Bean Validation).
- Re-add `GlobalExceptionHandler.handleTypeMismatch` with
  `@WebMvcTest` for `GET /api/v1/pokemon/abc` → 400 problem+json.
- `@ExceptionHandler(PokemonNotFoundException)` → 404 problem+json
  (generic detail; no catalog URLs).
- OpenAPI: `@ApiResponse` for 200 / 400 / 404 / 502 with
  `application/problem+json` + `ProblemDetail` where applicable.

**Rationale**: Spec FR-007–FR-009 / FR-013.

## R9. Dependencies

**Decision**: **No new Maven dependencies.** Reuse caffeine, wiremock-standalone,
springdoc, validation from 002.

**Alternatives considered**: Graph library for trees (rejected); extra
JSONPath helper (rejected).

## R10. WireMock assertions for cache reuse

**Decision**: Detail IT / adapter tests MUST assert that for requested id
`N`, a single `GET /api/v1/pokemon/N` (app) that loads the full detail
results in **exactly one** WireMock hit to `/pokemon/N` and **exactly one**
to `/pokemon-species/N` (phase a), even though phase (c) also needs that id
via `fetchEntries`.

**Rationale**: Proves one-cache-per-URL and that phase (c) does not re-fetch
the identity pair.
