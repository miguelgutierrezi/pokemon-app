# Feature Specification: HU-04 Local Data Modification

**Feature Branch**: `005-hu04-local-update`

**Created**: 2026-10-17

**Status**: Draft

**Input**: Implement HU-04 exactly as written in `docs/requirements.md`:
enable update operations for any Pokémon currently stored within the local
database; ensure robust validation (404 for missing records, 400 for
malformed payloads, and further defensive logic as required). Backend only.
Do not add, rename, or extend the user story. Reuse 004's LocalPokemon,
Tags, repository port, JPA adapter, handler, and conventions. Delete of a
local record is delivered here as TR-API-CRUD (technical requirement), not
as a new user story.

## Requirements Traceability *(mandatory — constitution X)*

| Requirement ID (`docs/requirements.md`) | What 005 delivers |
|---|---|
| **HU-04** | Update operations for any Pokémon currently stored in the local database — exactly as written, not renamed, merged, or extended |
| **TR-CORE** | Domain validation for proprietary-field updates (limits, normalisation, snapshot immutability) |
| **TR-ERRORS** | Missing record, malformed payload, duplicate-import conflict, stale-version conflict, and unsupported media type failures return RFC 7807 Problem Detail bodies via the shared advice (two distinct **409** `type` URNs) |
| **TR-UNIT** | Domain, use cases, persistence adapter, and web covered per module gate |
| **TR-API-CRUD** | **Update** (HU-04) and **Delete** of local records — Delete is a technical requirement, not a new user story |

**Explicitly not delivered here**: editing catalog snapshot fields;
re-sync/refresh of an existing snapshot from the catalog; users, ownership,
or authentication (006 — will scope records per user and protect write
routes; until then routes remain **public** and the store is **global**);
collection UI (007); changes to HU-01/HU-02 contracts. **Additive change to
004's local-record contract**: every collection response that returns a
local record (POST import, GET list items, GET one, PUT) MUST include
`version` so clients can read the concurrency token.

## User Scenarios & Testing *(mandatory)*

### HU-04 — Local Data Modification (Priority: P1)

A consumer of the public API updates the **proprietary** fields of a Pokémon
already stored in the local database: localized nomenclature, geographical
metadata, and classification tags. Catalog snapshot fields captured at
import time remain read-only.

**Why this priority**: This is the sole user story of this feature (HU-04).

**Independent Test**: Update a seeded local record with a valid proprietary
payload and current version → **200** with the updated fields and a new
version. Repeat with a stale version → **409**. Update a missing national
number → **404** with the same local-collection wording as 004. Send a
malformed body → **400**.

**Acceptance Scenarios**:

1. **Given** national number *n* is in the local store with version *v*,
   **When** a client replaces the proprietary fields with a complete body
   (`localizedName`, `location`, `tags`, `version` = *v*), **Then** the API
   responds **200** with the updated local record (new version = *v* + 1),
   snapshot fields unchanged, and proprietary fields matching the request
   after domain normalisation.
2. **Given** *n* is in the local store, **When** a client sends
   `localizedName` or `location` as null, or `tags` as `[]`, **Then** those
   fields are cleared on the stored record and the response reflects the
   cleared values.
3. **Given** *n* is not in the local store, **When** a client updates *n*,
   **Then** the API responds **404** with an RFC 7807 Problem Detail whose
   detail is the same local-collection wording as 004
   (`Pokémon {n} is not in the local collection`).
4. **Given** the body is missing a required property, has wrong types,
   exceeds length/count limits after trim, has blank tags, or
   `nationalNumber` in the path is &lt; 1 or non-numeric, **When** a client
   updates, **Then** the API responds **400** with an RFC 7807 Problem
   Detail that names the offending field (never Jackson/internal messages).
5. **Given** two concurrent updates of the same record with the same
   version, **When** both complete, **Then** exactly one succeeds with
   **200** and the other receives **409** with
   `type` `urn:problem-type:stale-version` and a detail that the record
   was modified by another request and must be reloaded and retried
   (distinct from duplicate-import **409**, which keeps
   `urn:problem-type:conflict`).

---

### Delete local record (Priority: P2)

Clients remove a local record so the local lifecycle is complete
(TR-API-CRUD Delete). This is **not** a separate user story and does not
extend HU-04 beyond what the requirements map assigns to this feature.

**Why this priority**: Technical CRUD completeness for local records;
required by the feature map interpretation of TR-API-CRUD.

**Independent Test**: Delete a seeded or imported national number →
**204**; subsequent get → **404** local wording; the same number can be
re-imported from the catalog afterward. Delete a missing number → **404**
local wording. The external catalog is never modified.

**Acceptance Scenarios**:

1. **Given** *n* is in the local store, **When** a client deletes *n*,
   **Then** the API responds **204 No Content**, the row (and its types /
   tags) is gone, and get *n* returns **404** local-collection wording.
2. **Given** *n* is not in the local store, **When** a client deletes *n*,
   **Then** the API responds **404** with the same local-collection
   wording as update/get.
3. **Given** *n* was deleted, **When** a client imports *n* again from the
   catalog, **Then** import succeeds (catalog untouched by delete).

---

### Edge Cases

- PUT body MUST include all four properties; partial bodies are **400**.
- Attempts to supply read-only snapshot fields or unknown properties MUST
  be **400** naming the property (not silently ignored).
- Stale `version` MUST yield **409** with `type`
  `urn:problem-type:stale-version`, not a silent overwrite (and not
  `urn:problem-type:conflict`, which remains for duplicate import only).
- Non-JSON `Content-Type` on PUT MUST yield **415** problem+json.
- Successful update MUST advance the record's update timestamp; sync
  timestamp MUST NOT change.
- Tag normalisation (trim, lowercase, deduplicate; blank rejected) and
  text trim (blank → null) reuse domain rules from 004.
- If the record is missing when the write path reads it → **404**
  local-collection wording. If the record is deleted (or otherwise
  changed) between that read and the write — an optimistic-lock failure
  → **409** with `type` `urn:problem-type:stale-version` (not **404**).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow updating proprietary fields of any
  Pokémon currently stored in the local database, identified by National
  Dex number.
- **FR-002**: An update MUST replace only proprietary fields
  (`localizedName`, `location`, `tags`). Snapshot fields (name, category,
  types, height, weight, image, generation, `syncedAt`) MUST remain
  unchanged.
- **FR-003**: An update request MUST carry a complete proprietary payload
  plus the expected concurrency token (`version`). Null clears a text
  field; an empty tags list clears tags.
- **FR-004**: A successful update MUST return **200** with the full updated
  local record including the new version. The local-record response of
  **all** collection endpoints that return a record (POST import, GET
  list items, GET one, and PUT) MUST include `version` — an **additive**
  change to 004's contract so clients can always read the concurrency
  token.
- **FR-005**: Update or delete of a national number absent from the local
  store MUST return **404** with the same local-collection detail wording
  as 004.
- **FR-006**: Malformed update payloads MUST return **400** with a Problem
  Detail that names the offending field, including: invalid JSON; missing
  property; wrong types; `localizedName` longer than 100 or `location`
  longer than 200 characters after trim; more than 20 tags; a tag longer
  than 32 characters or blank; path `nationalNumber` &lt; 1 or non-numeric.
- **FR-007**: The system MUST reject attempts to change read-only snapshot
  fields and MUST reject unknown properties with **400** naming the
  property.
- **FR-008**: The system MUST use optimistic concurrency: each local record
  carries a version; a PUT with a stale version MUST return **409** with
  RFC 7807 `type` `urn:problem-type:stale-version` and a detail that the
  record was modified by another request and the client should reload and
  retry. Concurrent same-version updates MUST yield one success and one
  stale-version **409**. Duplicate-import **409** MUST keep
  `type` `urn:problem-type:conflict` and its existing detail (already in
  the local collection) — the two conflicts MUST remain distinguishable
  by `type` and detail.
- **FR-009**: Domain normalisation from 004 MUST apply on update (trim;
  blank text → null; tags trimmed, lowercased, deduplicated).
- **FR-010**: Non-JSON content type on update MUST return **415**
  problem+json.
- **FR-011**: A successful update MUST change the record's last-updated
  timestamp; the sync timestamp MUST never change on update.
- **FR-012**: The system MUST allow deleting a local record by National Dex
  number with **204 No Content**, cascading removal of associated type and
  tag data, without affecting the external catalog.
- **FR-013**: Until authentication ships, update and delete collection
  routes MUST remain public and the local store MUST remain global; this
  MUST be documented for reviewers as a temporary exception to
  constitution VI (006 will protect writes and scope records per user).
- **FR-014**: Persistence adapters MUST keep storage entities inside the
  data-access layer; HTTP layers MUST use DTOs only; errors MUST be RFC
  7807 Problem Details via the shared handler; OpenAPI MUST document the
  update and delete operations (200/204/400/404/409/415 as applicable)
  and the additive `version` field on local-record schemas used by import,
  list, get, and update.

### Key Entities

- **Local Pokémon record** (from 004): Snapshot plus proprietary fields;
  additionally carries a concurrency **version** used for optimistic
  locking. Update touches proprietary fields and version only.
- **Tag**: Classification string on a local record; normalised set
  semantics (trim, lowercase, deduplicate, non-blank), max 20 per record,
  each ≤ 32 characters.
- **Proprietary update**: The three replaceable fields
  (`localizedName` ≤ 100, `location` ≤ 200, `tags`) plus the expected
  `version`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every error response for update/delete is an RFC 7807 Problem
  Detail (no stack traces, Jackson package names, or internal messages in
  the body).
- **SC-002**: Under concurrent same-version updates of one record, clients
  never observe a lost update: exactly one request succeeds and the other
  receives **409** with `type` `urn:problem-type:stale-version`.
- **SC-003**: A successful update or delete of an existing local record
  completes in ≤ **200 ms** on a local development stack (excluding catalog
  network; catalog is not called).
- **SC-004**: Seeded demo rows remain editable: a proprietary update on a
  seeded Bulbasaur succeeds with **200** and an incremented version.

## Assumptions

- Backend-only feature; UI consumption is a later frontend feature.
- Reuses 004's `LocalPokemon`, `Tags`, repository port, JPA adapter,
  ProblemDetail handler, and OpenAPI conventions; extends them rather than
  replacing the collection API.
- HTTP shapes (for planning):
  - `PUT /api/v1/collection/{nationalNumber}` with JSON body
    `{"localizedName": string|null, "location": string|null,
    "tags": [string], "version": number}` → **200** + updated record.
  - `DELETE /api/v1/collection/{nationalNumber}` → **204**.
- Existing rows (including V3 seed) receive version default **0** when the
  version column is introduced; the first successful update advances it.
  Import/list/get responses expose that `version` (additive vs 004).
- Stale-version **409** (for planning): `type`
  `urn:problem-type:stale-version`; detail conveys that the record was
  modified by another request and the client should reload and retry
  (exact English string fixed in plan/contracts). Duplicate-import **409**
  remains `type` `urn:problem-type:conflict`.
- Public write on PUT/DELETE collection (and an unscoped global store) is a
  **temporary, documented exception to constitution VI**. Feature 006
  removes the exception. **`plan.md` MUST record this exception in
  Complexity Tracking** (same pattern as 004).
- Test strategy (R6): domain (apply proprietary update preserving snapshot;
  limits); application use cases with mocked ports
  (`UpdateLocalPokemonUseCase`, `DeleteLocalPokemonUseCase`);
  infrastructure JPA (update round trip, version increments, stale version
  → exception, concurrent update, delete cascades types/tags); web
  controller tests; one end-to-end IT with Postgres: update seed Bulbasaur
  (**200**, version +1), stale version **409**, read-only/unknown field
  **400**, limits **400**, missing record **404**, delete **204** then GET
  **404** and re-import works.
- Out of scope: editing snapshot fields, re-sync from the catalog, users /
  ownership / auth, UI.
