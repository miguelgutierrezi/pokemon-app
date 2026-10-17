# Feature Specification: HU-01 Pokemon Enumeration

**Feature Branch**: `002-hu01-pokemon-enumeration`

**Created**: 2026-10-16

**Status**: Draft

**Input**: Implement HU-01 exactly as written in `docs/requirements.md`:
paginated Pokémon browse with sprite, category, mass, and skills; nice to
have caching. Backend API only; catalog UI is feature 007.

## Requirements Traceability *(mandatory — constitution X)*

| Requirement ID (`docs/requirements.md`) | What 002 delivers |
|---|---|
| **HU-01** | Paginated Pokémon enumeration with sprite, category, mass, and skills (abilities), exactly as written — not renamed, merged, or extended |
| **FR-POKEAPI** | Backend reads National Pokédex species and per-Pokémon details from the external Pokémon catalog (PokeAPI) |
| **TR-CACHE** | In-memory cache of catalog responses so repeated pages do not re-fetch within TTL (HU-01 nice to have) |
| **TR-ERRORS** | Invalid paging and upstream failures return RFC 7807 Problem Detail bodies for this endpoint (and the shared advice introduced here) |
| **TR-UNIT** | Unit tests for domain rules; per-module coverage gate (see Assumptions / research R6) |

**Explicitly not delivered here**: detail view / stats / description /
evolution (HU-02 / 003); local persistence or sync (HU-03 / 004); local
updates or delete (HU-04 / 005); authentication (006); catalog UI (007);
search, sorting, or filtering (not requested by HU-01).

## User Scenarios & Testing *(mandatory)*

### HU-01 — Pokemon Enumeration (Priority: P1)

A consumer of the public API browses Pokémon as paginated results in National
Pokédex order. Each entry shows the national number, English display name
(e.g. "Mr. Mime", "Farfetch'd"), sprite image URL (nullable), category
(English genus, e.g. "Seed Pokémon"), weight in kilograms, and the collection
of abilities (catalog slug name and whether each is a hidden ability), in
slot order.

**Why this priority**: This is the sole user story of this feature (HU-01).

**Independent Test**: Call the paginated list endpoint with default and
custom page/size; assert entry fields, National Dex ordering, and pagination
metadata whose total comes from the catalog (not a hard-coded constant).

**Acceptance Scenarios**:

1. **Given** the API is available and the external catalog is reachable,
   **When** a client requests the first page with the default page size,
   **Then** the response is successful and contains up to 9 entries in
   National Pokédex order, each with national number, English display name,
   sprite URL (or null), category when an English genus exists, weight in
   kilograms (one decimal), and abilities (slug + hidden flag) in slot order.
2. **Given** the catalog reports a known total number of National Dex
   species, **When** a client requests any valid page, **Then** the response
   includes pagination metadata: page, size, total elements, and total pages,
   and total elements matches the catalog count (never a hard-coded value in
   the application).
3. **Given** a client chooses page and size within allowed bounds,
   **When** they request that page, **Then** the slice of species matches
   National Dex order for that offset and size.
4. **Given** a species has an English name in the catalog species `names[]`
   (e.g. "Mr. Mime", "Farfetch'd"), **When** it appears in a page, **Then**
   the entry name is that English display name; **Given** no English name
   exists, **Then** the name falls back to the resource slug (documented).
5. **Given** a species has no English genus in the catalog, **When** it
   appears in a page, **Then** category is absent/null (documented), not a
   guessed or non-English value.
6. **Given** the catalog omits a front-default (or equivalent) sprite URL for
   a species, **When** that species appears in a page, **Then** the sprite
   field is null (documented) and the entry is still returned with the other
   fields.
7. **Given** invalid paging input (page &lt; 1, size &lt; 1, size above the
   documented maximum of 20, or page beyond the last page), **When** the
   client calls the list endpoint, **Then** the API responds with HTTP 400
   and an RFC 7807 Problem Detail explaining the problem — never a stack
   trace.
8. **Given** the external catalog is unavailable or times out, **When** the
   client requests a page, **Then** the API responds with HTTP 502 and an
   RFC 7807 Problem Detail — never a stack trace and never a partial page
   presented as complete.

---

### HU-01 nice to have — Cached catalog responses (Priority: P2)

Repeated requests for the same page within the cache window are served from
the cache without calling the external catalog again (PokeAPI fair use /
TR-CACHE).

**Why this priority**: Explicit nice to have on HU-01 and TR-CACHE; not a
separate user story.

**Independent Test**: Request the same page twice within the TTL; the second
response succeeds quickly and produces zero additional catalog HTTP calls.

**Acceptance Scenarios**:

1. **Given** a page was already fetched successfully, **When** the same
   page/size is requested again within the cache TTL, **Then** the response
   matches the cached content and the external catalog is not contacted for
   that request.
2. **Given** the cache TTL has expired (or the entry was evicted), **When**
   the same page is requested again, **Then** the catalog may be contacted
   and a complete page is returned (or a 502 Problem Detail if the catalog
   fails — never a partial success).

### Edge Cases

- Page equal to the last page with a short final page (remainder &lt; size).
- Size at the documented maximum of **20** (accepted); size 21 → 400.
  Maximum 20 caps fan-out at 40 catalog calls per uncached page (species +
  Pokémon per entry), keeping SC-004 achievable.
- Sprite URL missing in the catalog → sprite field null; entry still returned.
- Concurrent requests for the same uncached page do not return a partial
  page marked complete if the catalog fails mid-flight.
- Alternate forms (catalog ids 10001+) MUST NOT appear; only National Dex
  species from the species listing are enumerated.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose a public, read-only, paginated list of
  Pokémon in National Pokédex order for API consumers (backend only in this
  feature).
- **FR-002**: Each list entry MUST include national number, display name,
  sprite image URL (nullable), category (English genus when present), weight
  in kilograms, and abilities (catalog slug and hidden-ability flag) in slot
  order.
- **FR-003**: Pagination MUST expose page, size, total elements, and total
  pages; total elements MUST come from the external catalog’s National Dex
  species count, never a hard-coded application constant.
- **FR-004**: Default page size MUST be 9 when size is omitted; maximum page
  size MUST be **20** (larger → 400 per FR-005).
- **FR-005**: Invalid paging (page &lt; 1, size &lt; 1, size above 20, page
  beyond the last page) MUST yield HTTP 400 with an RFC 7807 Problem Detail.
- **FR-006**: When the external catalog is unavailable or times out, the
  system MUST yield HTTP 502 with an RFC 7807 Problem Detail and MUST NOT
  return a partial page as a successful complete result.
- **FR-007**: Weight MUST be derived from the catalog’s hectogram value as
  kilograms = hectograms / 10, reported to one decimal place (domain rule).
- **FR-008**: Category MUST be the English genus only; if none exists, the
  field MUST be absent/null (documented), never invented (domain rule).
- **FR-009**: Display name MUST be the English name from the species
  resource `names[]` (e.g. "Mr. Mime", "Farfetch'd"); if no English name
  exists, the name MUST fall back to the resource slug (domain rule).
- **FR-010**: Enumeration MUST use the National Pokédex species set (catalog
  species list / count), not the broader Pokémon resource list that includes
  alternate forms.
- **FR-011**: For each species on a page, sprite, weight, and abilities MUST
  come from the Pokémon resource, and English display name and genus from the
  species resource; those two catalog reads for an entry MAY proceed in
  parallel.
- **FR-012**: If the catalog provides no sprite URL for an entry, the sprite
  field MUST be null (documented); the entry MUST still be returned.
- **FR-013**: The system MUST cache external catalog responses in memory with
  a bounded size and TTL so repeated identical page requests within the TTL
  do not re-contact the catalog (HU-01 nice to have / TR-CACHE).
- **FR-014**: Catalog base URL and timeouts MUST be configurable via
  environment (see Assumptions); no secrets or URLs hard-coded for production
  use.
- **FR-015**: Error responses for this feature’s endpoint MUST use a shared
  RFC 7807 Problem Detail mechanism; API documentation for the endpoint MUST
  be available via OpenAPI (deferred from 001 into this feature per
  constitution IV / IX).

### Key Entities

- **PokemonSummary**: A National Dex list entry — national number, English
  display name (slug fallback), optional/nullable sprite URL, optional
  category, weight in kg, ordered abilities.
- **AbilityRef**: Ability identifier as the catalog slug (e.g. `solar-power`)
  and whether it is a hidden ability; ordered by slot.
- **Page&lt;PokemonSummary&gt;**: Page of summaries plus page, size, total
  elements, total pages.
- **Catalog species count**: Authoritative total for pagination; sourced from
  the external catalog’s National Dex species listing.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A consumer can retrieve a valid first page of the National Dex
  list and see English display names, sprite (or null), category (when
  present), mass in kg, and abilities for each entry.
- **SC-002**: Pagination metadata always reflects the catalog’s total species
  count; changing that count at the source changes total elements / total
  pages without an application redeploy of a hard-coded total.
- **SC-003**: A repeated request for the same page within the cache window is
  served in ≤ 200 ms and performs zero additional external catalog calls.
- **SC-004**: The first uncached page for a normal network path to the catalog
  completes in ≤ 3 seconds when the catalog is healthy.
- **SC-005**: 100% of error responses for invalid paging or catalog failure on
  this endpoint are RFC 7807 Problem Detail documents (no raw stack traces to
  clients).
- **SC-006**: Domain rules for weight conversion, English display name (with
  slug fallback), and English-only category are covered by automated unit
  tests in the domain module; overall line coverage remains ≥ 80% per module.

## Assumptions

- **Endpoint**: `GET /api/v1/pokemon?page=&size=` — the backend owns the
  `/api/v1` prefix; the existing reverse proxy forwards `/api/` unchanged.
  The route is public; feature 006 adds auth and keeps reads public.
- **Default / limits**: Default `size` = 9; maximum `size` = **20**
  (documented; larger → 400). Page numbering is **1-based**. Cap of 20
  bounds fan-out to at most 40 catalog HTTP calls per uncached page.
- **National Dex source**: Enumerate via PokeAPI `/pokemon-species` (count
  1025 at spec time; always use live count). Do **not** use `/pokemon` list
  offsets for identity (includes forms 10001+). Each entry combines
  `/pokemon/{id}` (sprite, weight, abilities) and `/pokemon-species/{id}`
  (English `names[]`, genus), fetched in parallel per id.
- **Ability names**: Returned as catalog slugs (e.g. `solar-power`); human
  formatting (title case, spaces) is the UI’s job in feature **007**.
- **Errors & docs**: Global Problem Detail advice and springdoc OpenAPI are
  introduced in this feature (deferred from 001).
- **Caching**: Caffeine in-memory cache for PokeAPI responses, bounded size
  and TTL (exact numbers chosen in the plan).
- **Config**: `POKEAPI_BASE_URL` in `.env.example` and the env contract;
  connect/read timeouts configurable.
- **Testing / coverage (R6)**: Domain and application — pure unit tests;
  PokeAPI client in infrastructure — WireMock without a Spring context; web —
  controller tests plus a few end-to-end integration tests. JaCoCo gate stays
  per module; optional `report-aggregate` for visibility only.
- **UI**: Catalog browsing UI is feature **007**; this feature delivers the
  backend API only.
- **Auth**: Not required for this read endpoint (006).
