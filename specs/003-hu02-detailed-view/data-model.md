# Data Model: HU-02 Detailed View

**Feature**: `003-hu02-detailed-view` | **Date**: 2026-10-16

No persistence entities. Domain / application models below. HTTP DTOs live in
`web` and are defined in
[contracts/pokemon-detail-api.yaml](./contracts/pokemon-detail-api.yaml).

Reuse from 002 without breaking the list path: `AbilityRef`, `WeightKg`,
`DisplayName`, `Category`, `LocalizedText`, `CatalogAbility`,
`CatalogPokemon`, `CatalogSpecies`, `CatalogEntry`. List types remain the
public shape of `fetchEntries` / list assembly; they are **derived** from
detail cache values (see below).

## Domain value objects / entities

### HeightM

| Rule | Detail |
|------|--------|
| Input | Catalog height in **decimetres** (`int` ≥ 0) |
| Output | `BigDecimal` metres = dm / 10, **scale 1** |
| Reject | Negatives → `IllegalArgumentException` |

### FlavorText

| Field | Type | Rules |
|-------|------|--------|
| `text` | string | Non-blank normalised English narrative |
| `version` | string | Non-null version slug for that entry |

Absent narrative is represented **only** as a null `FlavorText` reference
(e.g. `FlavorText.from(...)` returns null) — never as a `FlavorText` with
null fields.

**Normalisation** (domain): collapse runs of whitespace (`\s+`, covering
`\f`/`\n`/`\r`/spaces) to a single space; trim. Empty after trim → absent
(`flavorText` is null).

### ImageUrl

Pure function: prefer `officialArtworkSprite`, else `frontDefaultSprite`,
else `null`.

### PokemonStats

| Field | Type | Rules |
|-------|------|--------|
| `hp`, `attack`, `defense`, `specialAttack`, `specialDefense`, `speed` | int | Base stats ≥ 0 |
| `total` | int | **Computed** = sum of the six (not stored independently as input) |

### EvolutionLink (edge metadata)

| Field | Type | Rules |
|-------|------|--------|
| `trigger` | string \| null | Catalog trigger slug from **first** `evolution_details` entry |
| `minLevel` | Integer \| null | From first detail; null if absent |
| `itemSlug` | string \| null | From first detail’s item name; null if absent |

If `evolution_details` is **empty**, all three are null (documented).

### EvolutionNode

| Field | Type | Rules |
|-------|------|--------|
| `nationalNumber` | int | Species id (≥ 1) |
| `name` | string | English display name (slug fallback) via `DisplayName` |
| `imageUrl` | string \| null | Catalog **front sprite** (`CatalogPokemon.spriteUrl` / `front_default`) for tree thumbnails — not the detail `ImageUrl` (artwork) rule |
| `isCurrent` | boolean | `true` iff this node is the requested Pokémon |
| `children` | list of EvolutionChild | Ordered as in the catalog chain |

### EvolutionChild

| Field | Type | Rules |
|-------|------|--------|
| `link` | EvolutionLink | Edge from parent to this child |
| `node` | EvolutionNode | Child subtree |

### PokemonDetail

| Field | Type | Rules |
|-------|------|--------|
| `nationalNumber` | int | Requested National Dex id |
| `name` | string | English / slug fallback |
| `category` | string \| null | English genus |
| `types` | list of string | Type slugs in slot order |
| `heightM` | decimal | `HeightM` |
| `weightKg` | decimal | `WeightKg` |
| `abilities` | list of AbilityRef | Slot order |
| `generation` | string | Generation slug from species |
| `imageUrl` | string \| null | `ImageUrl` rule |
| `stats` | PokemonStats | Six bases + total |
| `flavorText` | FlavorText \| null | Null when no usable English narrative |
| `evolution` | EvolutionNode | Root of the chain tree (with `isCurrent` markers) |

**Factory**: `PokemonDetail.assemble(CatalogDetailEntry, CatalogEvolutionChain,
List<CatalogEntry> chainEntries, int requestedId)` (or equivalent pure
builders unpacking `entry.pokemon()` / `entry.species()`) — unit-tested in
`domain`.

## Catalog input records (domain — adapter fills / caches these)

### CatalogPokemonDetail

Canonical pokemon resource projection cached under `/pokemon/{id}`: national
number, slug, `frontDefaultSprite`, `officialArtworkSprite`, height dm,
weight hg, abilities, types `(slot, name)`, base stats by catalog name.

**Derivation**: `toSummary()` → 002 `CatalogPokemon` (sprite =
`frontDefaultSprite` only, for list FR-012).

### CatalogSpeciesDetail

Canonical species resource projection cached under `/pokemon-species/{id}`:
names, genera, flavor entries `(text, language, versionSlug)`, generation
slug, evolution chain id (`int`).

**Derivation**: `toSummary()` → 002 `CatalogSpecies` (names + genera only).

### CatalogDetailEntry

| Field | Type |
|-------|------|
| `pokemon` | `CatalogPokemonDetail` |
| `species` | `CatalogSpeciesDetail` |

Returned by **one** port operation `fetchDetail(id)` (phase a).

### CatalogEvolutionChain / CatalogEvolutionNode

Tree of species ids + raw `evolution_details` lists for domain edge mapping.
Chain **nodes carry species ids**; name/image come from `fetchEntries` with
the same id (1–1025: species id == default Pokémon id).

## Application

### PokemonCatalogPort

| Operation | Phase | Output |
|-----------|-------|--------|
| `fetchDetail(id)` | (a) | `CatalogDetailEntry` (adapter loads pokemon+species **in parallel**); identity 404 → not-found |
| `fetchEvolutionChain(chainId)` | (b) | `CatalogEvolutionChain` |
| `fetchEntries(ids)` | (c) | `List<CatalogEntry>` (002) — from detail caches via `toSummary()` |
| `speciesPage(offset, limit)` | list | unchanged (002) |

Use case makes **exactly three** port calls for detail (one per phase). Do
**not** expose separate `fetchPokemonDetail` / `fetchSpeciesDetail` on the
port.

### PokemonNotFoundException

Lives in **`application`**. Thrown when identity resources return upstream
404 inside `fetchDetail`. **Web** maps → HTTP **404** ProblemDetail.

### CatalogUnavailableException

Unchanged (002). **Web** maps → HTTP **502**.

### GetPokemonDetailUseCase

```text
entry = catalog.fetchDetail(id)           // (a)
chain = catalog.fetchEvolutionChain(...)  // (b)
nodes = catalog.fetchEntries(chainIds)    // (c) — id already cached from (a)
return PokemonDetail.assemble(entry, chain, nodes, id)
```

## Configuration

No new env keys. See [contracts/env-config.md](./contracts/env-config.md)
and 002’s pokeapi properties (TTL applies to the new chain cache).

## Relationships

```text
GetPokemonDetailUseCase
  → (a) fetchDetail(id) → CatalogDetailEntry   [adapter: parallel GETs]
  → (b) fetchEvolutionChain(species.chainId)
  → (c) fetchEntries(chainSpeciesIds)          [cache hit for requested id]
  → PokemonDetail.assemble(...) → EvolutionNode tree (isCurrent)

Caches: pokemon → CatalogPokemonDetail; species → CatalogSpeciesDetail
List path: detail.toSummary() → CatalogPokemon / CatalogSpecies
```
