# Feature Specification: HU-02 Detailed View

**Feature Branch**: `003-hu02-detailed-view`

**Created**: 2026-10-16

**Status**: Draft

**Input**: Implement HU-02 exactly as written in `docs/requirements.md`:
comprehensive data for a chosen Pokémon — image, core statistics, narrative
description, and evolutionary lineage. Backend API only; UI is a later
frontend feature. Reuse 002 domain, port, adapter, caches, ProblemDetail
handler, and conventions.

## Requirements Traceability *(mandatory — constitution X)*

| Requirement ID (`docs/requirements.md`) | What 003 delivers |
|---|---|
| **HU-02** | Access to comprehensive data for a chosen Pokémon: image, core statistics, narrative description, and evolutionary lineage — exactly as written, not renamed, merged, or extended |
| **FR-POKEAPI** | Backend reads Pokémon, species, and evolution-chain resources from the external catalog for the detail view |
| **TR-CACHE** | In-memory cache of catalog responses (reuse 002 caches; add evolution-chain cache) so repeated detail requests within TTL do not re-fetch |
| **TR-ERRORS** | Invalid id, not-found, and upstream failures return RFC 7807 Problem Detail bodies; extend the shared advice from 002 (including type-mismatch → 400 and a new 404 case) |
| **TR-UNIT** | Domain rules (stat total, height, description normalisation, image fallback, evolution tree) unit-tested in domain; per-module coverage gate unchanged |

**Explicitly not delivered here**: ability descriptions, moves, forms/varieties,
type matchups; local persistence or sync (HU-03 / 004); local updates (HU-04 /
005); authentication (006); catalog UI (007); list enumeration changes beyond
reuse (HU-01 / 002 already shipped).

## User Scenarios & Testing *(mandatory)*

### HU-02 — Detailed View (Priority: P1)

A consumer of the public API retrieves comprehensive data for one Pokémon by
National Dex number: identity and physical attributes, preferred artwork
image, six base stats plus their total, an English narrative description with
its game version, and the full evolutionary lineage as a tree (so branching
species such as Eevee and single-stage species are both representable). The
requested Pokémon is identifiable within that tree.

**Why this priority**: This is the sole user story of this feature (HU-02).

**Independent Test**: Call the detail endpoint for a known National Dex id
(e.g. Bulbasaur, Eevee, a single-stage species); assert identity fields,
image rule, stats + total, normalised English flavor text + version, and
evolution tree shape (branching / leaf). Assert 400 / 404 / 502 Problem
Detail cases.

**Acceptance Scenarios**:

1. **Given** the API is available and the external catalog is reachable,
   **When** a client requests detail for a valid National Dex id,
   **Then** the response is successful and includes national number, English
   display name (slug fallback), category (English genus or null), types in
   slot order (slugs), height in metres (one decimal), weight in kilograms
   (one decimal), abilities (slug + hidden) in slot order, and generation
   slug.
2. **Given** a Pokémon has official artwork, **When** its detail is
   requested, **Then** the image URL is that artwork; **Given** artwork is
   missing but the default front sprite exists, **Then** the image URL is
   that sprite; **Given** neither exists, **Then** the image field is null
   (documented) and the rest of the payload is still returned.
3. **Given** a Pokémon has the six standard base stats in the catalog,
   **When** its detail is requested, **Then** the response includes hp,
   attack, defense, special-attack, special-defense, and speed base values,
   plus a **total** equal to the sum of those six (domain-computed).
4. **Given** the species has at least one English flavor text entry,
   **When** detail is requested, **Then** the narrative is the **first**
   English entry in catalog order, normalised (form feed, newlines, carriage
   returns, and repeated whitespace collapsed to single spaces, then
   trimmed), and the response includes the game **version** slug for that
   entry; **Given** no English flavor text, **Then** narrative and version
   are null (documented).
5. **Given** a species with a branching evolution chain (e.g. Eevee),
   **When** detail is requested, **Then** the evolutionary lineage is a
   **tree** of nodes (national number, English display name, image per the
   same image rule, children) and each edge carries trigger / min level /
   item from the **first** catalog `evolution_details` entry (all null when
   that list is empty); the requested Pokémon is identifiable within the
   tree.
6. **Given** a single-stage species, **When** detail is requested, **Then**
   the lineage tree has that species as a node with no children (or an
   equivalent empty-children representation).
7. **Given** id &lt; 1 or a non-numeric path segment, **When** the client
   calls the detail endpoint, **Then** the API responds with HTTP **400**
   and an RFC 7807 Problem Detail — never a stack trace.
8. **Given** a numeric id that the catalog does not resolve as a National
   Dex Pokémon (upstream 404, e.g. beyond species count or an alternate-form
   id), **When** the client requests detail, **Then** the API responds with
   HTTP **404** and an RFC 7807 Problem Detail — distinct from 502.
9. **Given** the external catalog is unavailable, times out, or returns
   malformed required data, **When** the client requests detail, **Then**
   the API responds with HTTP **502** and an RFC 7807 Problem Detail with a
   generic detail (no upstream URLs or internal messages leaked).

---

### Cached catalog responses (Priority: P2)

Repeated requests for the same Pokémon detail within the cache window are
served without re-contacting the external catalog (TR-CACHE; same policy as
002).

**Why this priority**: TR-CACHE applies to catalog reads introduced or reused
here; not a separate user story.

**Independent Test**: Request the same id twice within the TTL; the second
response succeeds quickly and produces zero additional catalog HTTP calls
(including evolution-chain and chain-node fetches).

**Acceptance Scenarios**:

1. **Given** detail for an id was already fetched successfully, **When** the
   same id is requested again within the cache TTL, **Then** the response
   matches the cached content and the external catalog is not contacted for
   that request.
2. **Given** the cache TTL has expired (or entries were evicted), **When**
   the same id is requested again, **Then** the catalog may be contacted and
   a complete detail is returned (or a Problem Detail if the catalog fails —
   never a partial success presented as complete).

### Edge Cases

- Id at the boundaries: `1` (accepted when present in the catalog); `0` and
  negative → 400; non-numeric path → 400 (type mismatch).
- Id greater than the live National Dex count, or an alternate-form catalog
  id → **404**, not 502.
- Upstream **404** on Pokémon/species MUST NOT be mapped to “catalog
  unavailable” (502); only transport/timeout/5xx/malformed → 502.
- Evolution chains with multiple branches (Eevee’s eight children) and deep
  linear chains must both serialize as trees with correct edge metadata
  (first `evolution_details` entry only; empty list → null trigger/min
  level/item).
- Flavor text with `\f`, `\n`, `\r`, and repeated spaces → single-space
  normalised English text.
- Height from catalog decimetres → metres with one decimal (e.g. 7 dm →
  0.7 m).
- Concurrent uncached detail requests for the same id do not return a
  partial payload marked complete if the catalog fails mid-flight.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose a public, read-only detail resource for
  one Pokémon identified by National Dex number for API consumers (backend
  only in this feature).
- **FR-002**: The detail payload MUST include identity: national number,
  English display name (slug fallback), category (English genus or null),
  types (slugs, slot order), height in metres (one decimal), weight in
  kilograms (reuse WeightKg rules from 002), abilities (reuse AbilityRef /
  slot order), and generation (slug from species).
- **FR-003**: The image URL MUST prefer official artwork, then the default
  front sprite, else null (documented); the entry MUST still be returned when
  the image is null.
- **FR-004**: Core statistics MUST include the six base stats (hp, attack,
  defense, special-attack, special-defense, speed) and a domain-computed
  **total** equal to their sum.
- **FR-005**: Narrative description MUST be the first English flavor-text
  entry in catalog order, normalised in the domain, plus that entry’s game
  version slug; both MUST be null when no English entry exists.
- **FR-006**: Evolutionary lineage MUST be a **tree** (nodes with children),
  not a flat list; each node MUST include national number, English display
  name, image (same rule as FR-003), and children; each parent→child edge
  MUST take the **first** entry of the catalog `evolution_details` list for
  trigger slug, min level, and item slug; if that list is empty, trigger,
  min level, and item MUST all be null (documented); the requested Pokémon
  MUST be identifiable within the tree.
- **FR-007**: Id &lt; 1 or non-numeric id MUST yield HTTP **400** with an
  RFC 7807 Problem Detail (including type-mismatch handling for the path
  variable).
- **FR-008**: Catalog 404 / unknown National Dex id MUST yield HTTP **404**
  with an RFC 7807 Problem Detail; this MUST be distinct from 502.
- **FR-009**: Catalog unavailable, timeout, or malformed required payloads
  MUST yield HTTP **502** with a generic Problem Detail (no upstream URL or
  internal message leaks).
- **FR-010**: Detail assembly MUST use at most **three sequential phases**,
  each parallel and cached: (a) Pokémon + species in parallel; (b)
  evolution chain (needs the species’ chain URL); (c) `fetchEntries` for
  chain node ids (the requested Pokémon is already cached from phase a).
  MUST NOT walk chain nodes with sequential per-node catalog calls.
- **FR-011**: Successful catalog responses used by this feature MUST be
  cached in memory with bounded size and TTL; failures MUST never be cached
  (TR-CACHE).
- **FR-012**: Domain rules for height conversion, stat total, flavor-text
  normalisation, image fallback, and evolution-tree assembly MUST be
  enforceable in the domain and covered by domain unit tests (TR-UNIT).
- **FR-013**: Error responses MUST use the shared RFC 7807 Problem Detail
  advice from 002 (extended as needed); the detail endpoint MUST appear in
  OpenAPI documentation.

### Key Entities

- **PokemonDetail**: Full detail for one National Dex Pokémon — identity,
  image, stats (+ total), narrative (+ version), evolution tree, generation.
- **PokemonStats**: The six base stats and computed total.
- **FlavorText**: Normalised English narrative text and version slug
  (nullable as a pair when absent).
- **EvolutionNode**: Tree node — national number, display name, image,
  children (each child link carries trigger, min level, item slug — or all
  null when `evolution_details` is empty).
- **AbilityRef / WeightKg / DisplayName / Category**: Reused from 002 where
  applicable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A consumer can retrieve detail for a valid National Dex id and
  see identity fields, image (or null), six stats plus total, narrative (or
  null), and an evolution tree in which the requested Pokémon is
  identifiable.
- **SC-002**: Branching lineages (e.g. Eevee) and single-stage species both
  return a correct tree shape with edge metadata when the catalog provides
  it.
- **SC-003**: A repeated request for the same id within the cache window is
  served in ≤ 200 ms and performs zero additional external catalog calls.
- **SC-004**: The first uncached detail for a normal network path to the
  catalog (including chain nodes; Eevee as worst case) completes in ≤ 3
  seconds when the catalog is healthy.
- **SC-005**: 100% of error responses for invalid id, not-found, or catalog
  failure on this endpoint are RFC 7807 Problem Detail documents (no raw
  stack traces; no upstream URL leaks on 502).
- **SC-006**: Domain rules for height, stat total, description normalisation,
  image fallback, and evolution-tree building are covered by automated unit
  tests in the domain module; overall line coverage remains ≥ 80% per
  module.

## Assumptions

- **Endpoint**: `GET /api/v1/pokemon/{id}` — `id` is the National Dex number
  (1..catalog species count). Public read; feature 006 keeps reads public.
  The backend owns `/api/v1`; Nginx forwards `/api/` unchanged.
- **Reuse from 002**: Domain helpers (`WeightKg`, display name, category,
  `AbilityRef`), `PokemonCatalogPort` / `PokeApiCatalogAdapter`, Caffeine
  caches for species-list / pokemon / species, `ListPokemonUseCase` patterns,
  `GlobalExceptionHandler`, springdoc, and `POKEAPI_BASE_URL` config — extend,
  do not fork.
- **Catalog resources / fetch phases**: At most three sequential phases, each
  parallel and cached — (a) `/pokemon/{id}` + `/pokemon-species/{id}`
  (names, genera, flavor_text_entries, generation, evolution_chain URL) in
  parallel; (b) `/evolution-chain/{chainId}` (new cache key; needs the
  species’ chain URL from a); (c) `fetchEntries` for all chain node ids
  (requested Pokémon already cached from a). No sequential per-node walks.
- **Chain node ids**: Evolution-chain nodes carry **species** ids. Node
  display name and image come from `fetchEntries` with that same id (002
  assumption: for National Dex 1–1025, species id == default Pokémon id).
- **Evolution edge details**: Catalog `evolution_details` is a list; use the
  **first** entry for trigger / min level / item. If the list is empty,
  trigger, min level, and item are all null (documented).
- **Image rule**: Prefer `sprites.other["official-artwork"].front_default`,
  else `sprites.front_default`, else null.
- **Height**: Catalog height is in decimetres; metres = decimetres / 10, one
  decimal.
- **Stats**: Base stat names match the catalog (`hp`, `attack`, `defense`,
  `special-attack`, `special-defense`, `speed`).
- **Flavor text**: First English entry in the species
  `flavor_text_entries` array order; normalisation collapses `\f`, `\n`,
  `\r`, and runs of whitespace to single spaces, then trims.
- **404 vs 502**: Adapter maps upstream **404** on identity resources to a
  domain/application not-found signal → HTTP 404. Transport/timeout/5xx/
  malformed → existing `CatalogUnavailableException` → 502.
- **Type mismatch**: Re-introduce `handleTypeMismatch` (removed in 002 as
  unreachable for query records) with a dedicated test for
  `/api/v1/pokemon/{id}` = non-numeric.
- **Performance (SC-003 / SC-004)**: Cached ≤ 200 ms and first uncached ≤ 3 s
  validated by **manual** measurement in the quickstart task; automated tests
  MUST NOT assert wall-clock timings (they DO assert zero upstream calls on
  cache hit).
- **Testing**: Domain unit tests for pure rules; application use-case tests
  with a port fake; infrastructure WireMock for adapter/cache including
  evolution-chain and 404 mapping; web `@WebMvcTest` + end-to-end IT with
  WireMock (Eevee branching). JaCoCo remains per-module; aggregate report
  visibility-only.
- **UI**: Detail UI is feature **007**; this feature delivers the backend API
  only.
- **Auth**: Not required for this read endpoint (006).
