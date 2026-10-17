# Data Model: HU-01 Pokemon Enumeration

**Feature**: `002-hu01-pokemon-enumeration` | **Date**: 2026-10-16
(revised after Miguel’s review)

No persistence entities. All types below are in-memory domain / application
models (not JPA). HTTP DTOs live in `web` and are defined in
[contracts/pokemon-list-api.yaml](./contracts/pokemon-list-api.yaml).

## Domain entities / value objects

### AbilityRef

| Field | Type | Rules |
|-------|------|--------|
| `name` | string | Catalog slug (e.g. `solar-power`); never title-cased here |
| `hidden` | boolean | From catalog `is_hidden` |
| slot order | — | List order follows catalog `slot` ascending |

### PokemonSummary

| Field | Type | Rules |
|-------|------|--------|
| `nationalNumber` | int | National Dex id (≥ 1) |
| `name` | string | English `names[]` from species, else resource slug (FR-009) |
| `spriteUrl` | string \| null | `sprites.front_default` or null if absent (FR-012) |
| `category` | string \| null | English genus only; null if missing (FR-008) |
| `weightKg` | decimal | `hectograms / 10`, one decimal place (FR-007) |
| `abilities` | list of AbilityRef | Slot order |

**Factory / pure functions (unit-tested in `domain`)**:
- `WeightKg.fromHectograms(int)` → e.g. 69 → `6.9`
- `DisplayName.from(names[], slug)`
- `Category.from(genera[])` → English or null

### PageRequest

| Field | Type | Rules |
|-------|------|--------|
| `page` | int | 1-based; ≥ 1 (web also enforces) |
| `size` | int | 1..20 (web also enforces; default 9 at edge) |

### Page&lt;T&gt;

| Field | Type | Rules |
|-------|------|--------|
| `content` | list of T | May be shorter on last page |
| `page` | int | Echo request page |
| `size` | int | Echo request size |
| `totalElements` | long | From catalog species `count` (never hard-coded) |
| `totalPages` | int | `ceil(totalElements / size)`; 0 if totalElements = 0 |

**Invariant**: given `totalElements` from `speciesPage`, `page` must be ≤
`totalPages` when `totalElements > 0` (and page ≥ 1). Violation → domain
`PageOutOfRangeException` (mapped to HTTP 400).

## Application port

### PokemonCatalogPort

| Operation | Input | Output |
|-----------|--------|--------|
| `speciesPage(offset, limit)` | 0-based offset, limit | ordered national **ids** + **total count** (one PokeAPI list call) |
| `fetchPokemon(id)` | national id | pokemon projection (sprite, weight, abilities) |
| `fetchSpecies(id)` | national id | species projection (names, genera, slug) |

**Assumption**: for National Dex ids **1–1025**, species id == default
Pokémon id (`/pokemon/{id}`).

### CatalogUnavailableException

Lives in **`application`**. Thrown when the catalog is unreachable, times
out, or a required fetch fails such that a complete page cannot be built.
**Web** maps it to HTTP **502** ProblemDetail. Must not be cached.

## Configuration (not persisted)

See [contracts/env-config.md](./contracts/env-config.md).

| Key / property | Purpose |
|----------------|---------|
| `POKEAPI_BASE_URL` | Catalog base; default `https://pokeapi.co/api/v2` |
| connect/read timeouts | RestClient factory |
| cache TTL (default 24h) / max size | Programmatic Caffeine in infrastructure |

## Relationships

```text
PageRequest
    → ListPokemonUseCase
        → PokemonCatalogPort.speciesPage(offset, limit)  // ids + total
        → validate page-beyond-last against total
        → fetchPokemon / fetchSpecies per id (parallel)
        → List<PokemonSummary> + Page metadata
```

No DB relationships in 002.
