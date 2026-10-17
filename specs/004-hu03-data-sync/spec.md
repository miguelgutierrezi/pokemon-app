# Feature Specification: HU-03 Data Synchronization

**Feature Branch**: `004-hu03-data-sync`

**Created**: 2026-10-17

**Status**: Draft

**Input**: Implement HU-03 exactly as written in `docs/requirements.md`:
develop a mechanism to persist Pokémon data into a local relational store;
this replication layer is intended to facilitate the addition of proprietary
fields (localized nomenclature, geographical metadata, or internal
classification tags). Backend only. Do not add, rename, or extend the user
story. Reuse 002/003 domain, ports, catalog adapter + caches, ProblemDetail
handler, and OpenAPI conventions.

## Requirements Traceability *(mandatory — constitution X)*

| Requirement ID (`docs/requirements.md`) | What 004 delivers |
|---|---|
| **HU-03** | Mechanism to persist Pokémon data into a local relational store so proprietary fields can be added later — exactly as written, not renamed, merged, or extended |
| **TR-DB** | Primary entity (local Pokémon record) in a relational store: unique primary key, natural key on national number, and more than two descriptive attributes |
| **TR-DATA** | Specialized data-access layer (port + persistence adapter) for the local store; controllers never touch the store directly |
| **DEL-SEED** | Demo Pokémon collection pre-populated so the solution is usable after compose up (static snapshot rows; no network at seed time) |
| **TR-ERRORS** | Validation, conflict, not-found (catalog vs local), and catalog-unavailable failures return RFC 7807 Problem Detail bodies via the shared advice |
| **TR-UNIT** | Domain invariants (local record, tag normalisation) and use cases unit-tested; infrastructure adapter and web covered per module gate |
| **TR-API-CRUD** | **Create** (import/sync into the local store) and **Read** (list + get by national number) of local records |

**Explicitly not delivered here**: editing proprietary fields or deleting
records (HU-04 / `005-hu04-local-update`); refreshing an existing snapshot
from the catalog; users, ownership, or authentication (006 — will scope
records per user and protect write routes; until then routes are **public**
and the store is **global**); catalog UI (007); changes to HU-01/HU-02 list
or detail contracts beyond reuse.

## User Scenarios & Testing *(mandatory)*

### HU-03 — Data Synchronization (Priority: P1)

A consumer of the public API imports (syncs) one Pokémon from the external
catalog into the local relational store. The local record is a **snapshot**
of catalog identity and presentation fields at sync time, plus proprietary
fields that start empty so later features can add localized nomenclature,
geographical metadata, and classification tags.

**Why this priority**: This is the sole user story of this feature (HU-03).

**Independent Test**: Import a known National Dex id not already seeded;
assert **201** with the local record body and a Location pointing at that
record. Re-import the same id → **409** and still one row. Import an id
unknown to the catalog → **404**; catalog down → **502** and nothing
persisted.

**Acceptance Scenarios**:

1. **Given** the API is available, the catalog is reachable, and national
   number *n* is not already in the local store, **When** a client imports
   *n*, **Then** the API responds **201 Created** with a Location for that
   local record and a body containing the snapshot fields (national number,
   English name, category, types in slot order, height in metres, weight in
   kilograms, image URL using the same artwork-preference rule as HU-02,
   generation, sync timestamp) plus proprietary fields initially empty:
   localized name null, location null, tags empty.
2. **Given** *n* is already in the local store (including after a concurrent
   import race), **When** a client imports *n* again, **Then** the API
   responds **409 Conflict** with an RFC 7807 Problem Detail and **no**
   second row exists for that national number.
3. **Given** *n* is unknown to the catalog, **When** a client imports *n*,
   **Then** the API responds **404** with an RFC 7807 Problem Detail (same
   not-found signal as catalog identity misses) and **nothing** is
   persisted.
4. **Given** the catalog is unavailable, times out, or returns malformed
   required data, **When** a client imports a number, **Then** the API
   responds **502** with a generic Problem Detail (no upstream URLs) and
   **nothing** is persisted.
5. **Given** the request body is missing, `nationalNumber` is missing,
   less than 1, or non-numeric, **When** a client calls import, **Then**
   the API responds **400** with an RFC 7807 Problem Detail.

---

### Read local collection (Priority: P2)

Clients list and fetch local records so the replication layer is usable
before proprietary editing exists (TR-API-CRUD Read).

**Why this priority**: Required for a usable store and for DEL-SEED
verification; not a separate user story.

**Independent Test**: After seed (and optional import), list with the same
pagination rules as HU-01; get by national number → **200**; get a number
not in the local store → **404** with a **distinct** detail that the
Pokémon is not in the local collection (not the catalog-not-found wording).

**Acceptance Scenarios**:

1. **Given** local records exist, **When** a client lists the collection
   with page and size, **Then** the response uses the same pagination
   structure and constraints as the catalog list (default page 1, default
   size 9, size 1–20, page ≥ 1), ordered by national number ascending.
2. **Given** national number *n* is in the local store, **When** a client
   requests that local record, **Then** the API responds **200** with the
   full local record (snapshot + proprietary fields).
3. **Given** *n* is not in the local store, **When** a client requests
   that local record, **Then** the API responds **404** with an RFC 7807
   Problem Detail whose detail clearly means “not in the local collection”
   (distinct from catalog-not-found).

---

### Demo seed data (Priority: P2)

The solution ships pre-populated with a small demo collection so reviewers
see local data immediately after compose up (DEL-SEED).

**Why this priority**: Deliverable requirement; supports demonstration of
proprietary field shapes before HU-04 editing.

**Independent Test**: Fresh database after migrations; list collection
includes the seeded species with expected snapshot and proprietary demo
values; no outbound catalog calls are required for seed application.

**Acceptance Scenarios**:

1. **Given** a fresh environment after migrations, **When** a client lists
   the local collection, **Then** a small demo set is present (at least
   Bulbasaur, Ivysaur, and Venusaur) with static snapshot values and
   sample proprietary fields (localized names, locations, and tags such as
   `first-partner`, `field-notes`, `forest` — lowercase, never
   `favorites`) suitable for later UI demonstration.
2. **Given** seed runs at migration time, **When** migrations apply,
   **Then** seed data does not depend on network access to the external
   catalog.

---

### Edge Cases

- Concurrent duplicate imports for the same national number MUST still
  yield at most one local row (uniqueness enforced in the store, not only
  by a pre-check).
- Import failure at any stage MUST leave no partial local row.
- Tags on a local record are modelled as a set of normalised strings
  (trimmed, lowercase, deduplicated, non-blank) even while create leaves
  them empty; invalid tag shapes are rejected by domain invariants when
  present.
- List page beyond the last page follows the same bad-request behaviour as
  the catalog list.
- Path/body type mismatches and Bean Validation failures use the shared
  Problem Detail advice (400).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow importing a Pokémon into the local store
  by National Dex number, creating a local record that snapshots catalog
  fields at sync time and initializes proprietary fields empty
  (localized name null, location null, tags empty).
- **FR-002**: The local record MUST include: national number (natural key),
  English name, category, types in slot order, height (metres, one decimal),
  weight (kilograms, one decimal), image URL (official artwork preferred,
  else front sprite, else null — same rule as HU-02), generation, sync
  timestamp, plus proprietary localized name, location (free text), and
  tags.
- **FR-003**: National number MUST uniquely identify a local Pokémon record
  (one record per Pokémon); the store MUST also assign its own surrogate
  primary key per constitution/TR-DB.
- **FR-004**: The system MUST reject a second import of the same national
  number with **409 Conflict**, including under concurrency.
- **FR-005**: The system MUST expose paginated listing of local records
  using the same page/size rules and the same pagination envelope as HU-01
  (`content`, `page`, `size`, `totalElements`, `totalPages`; default page 1,
  default size 9, size 1–20, page ≥ 1), ordered by national number
  ascending. List **items** are full local records (`LocalPokemon` contract
  shape), **not** catalog `PokemonSummary`. Fetch by national number MUST
  also return a full local record.
- **FR-006**: Get-by-national-number for a Pokémon absent from the local
  store MUST return **404** with a detail distinct from catalog-not-found.
- **FR-007**: Import MUST map catalog-not-found to **404**, catalog
  unavailable/malformed to **502**, and invalid input to **400**; no
  persistence on any failure.
- **FR-008**: Domain invariants MUST normalise tags (trim, lowercase,
  deduplicate, reject blank) and document length limits for proprietary
  text/tag values (enforced on edit in HU-04; modelled now).
- **FR-009**: Demo seed data MUST pre-populate a small local collection
  with static snapshots (no catalog network during seed).
- **FR-010**: Until authentication ships, import/list/get collection routes
  MUST remain public and the local store MUST be global; this MUST be
  documented for reviewers as a temporary exception to constitution VI
  (006 will protect writes and scope records per user).
- **FR-011**: Persistence adapters MUST keep storage entities inside the
  data-access layer; HTTP layers MUST use DTOs only; errors MUST be RFC
  7807 Problem Details via the shared handler; OpenAPI MUST document the
  new collection operations (200/201/400/404/409/502 as applicable).

### Key Entities

- **Local Pokémon record**: Snapshot of a catalog Pokémon at sync time plus
  proprietary fields. Identified by national number (unique) and an
  internal surrogate id. Includes sync timestamp and audit timestamps as
  needed by the store.
- **Type entry**: Ordered type slug belonging to a local record (slot
  order from catalog at sync).
- **Tag**: Classification string on a local record; normalised set
  semantics (no duplicates after normalisation).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time import of a valid National Dex id completes in
  ≤ **3 seconds** when the external catalog is healthy (uncached catalog
  path).
- **SC-002**: A duplicate import (including concurrent attempts) never
  leaves two local rows for the same national number; the client receives
  **409** when the record already exists.
- **SC-003**: Every error response for these operations is an RFC 7807
  Problem Detail (no stack traces or upstream URLs in the body).
- **SC-004**: Immediately after a successful `docker compose up` (fresh
  volumes), the local collection list shows the seeded demo Pokémon
  without any manual import step.
- **SC-005**: Clients can list and retrieve local records with the same
  pagination ergonomics as the catalog list, and distinguish “not in local
  collection” from “not in catalog” by the 404 detail text.

## Assumptions

- Backend-only feature; UI consumption is a later frontend feature.
- Import orchestration reuses existing catalog read capabilities from
  002/003 (detail-shaped snapshot fields); no refresh-of-existing-snapshot
  API in this feature.
- HTTP shapes (for planning): `POST /api/v1/collection` with body
  `{"nationalNumber": n}` → 201 + `Location: /api/v1/collection/{n}`;
  `GET /api/v1/collection?page=&size=`; `GET /api/v1/collection/{nationalNumber}`.
- Pagination defaults/limits match HU-01 (`page` default 1, `size` default
  9, size 1–20).
- Proprietary field length limits (modelled now, enforced on edit in 005):
  localized name ≤ 100 characters; location ≤ 200 characters; each tag ≤
  32 characters; at most 20 tags per record.
- Seed demo set: Bulbasaur, Ivysaur, Venusaur. Exact snapshot + proprietary
  strings (including tags `first-partner`, `field-notes`, `forest` — never
  `favorites`) are listed in `data-model.md` V3 and MUST match
  `V3__seed_local_collection.sql`.
- When feature 006 introduces per-user ownership, the HU-03 seed rows
  MUST be assigned to the demo user that 006 seeds with mock credentials
  (so the pre-populated collection remains visible after auth).
- Public write on `POST /api/v1/collection` (and an unscoped global store)
  is a **temporary, documented exception to constitution VI** (JWT-protected
  writes). Feature 006 removes the exception. **`plan.md` MUST record this
  exception in Complexity Tracking.**
- Test strategy (R6): domain unit tests; application use-case tests with
  mocked ports; infrastructure persistence tests with an empty test
  Spring configuration in infrastructure so that module covers its own
  code; web controller tests; one end-to-end IT with stubbed catalog +
  real database (import 201, duplicate 409, get 200, list, seed present,
  catalog 404 → 404, catalog 500 → 502 with nothing saved; concurrent
  duplicate coverage).
- Relational store and migration tooling already chosen by the project
  (PostgreSQL + Flyway); spring-boot-starter-data-jpa is introduced by
  this feature (deferred from setup).
