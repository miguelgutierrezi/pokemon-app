# Feature Specification: Frontend (TR-FRONTEND)

**Feature Branch**: `007-frontend`

**Created**: 2026-10-17

**Status**: Draft

**Input**: Feature 007-frontend — TR-FRONTEND, FE-INTEGRATION, FE-RESPONSIVE,
FE-CRUD, FE-ARCH. UI for the existing backend (002–006). Do **not** add,
rename, or merge user stories: label scenarios with the HU they serve
(HU-01 catalog list, HU-02 detail, HU-03 import to local collection,
HU-04 edit local data) or TR-API-CRUD/FE-CRUD (delete) or TR-API-AUTH
(login/register). **No backend changes** in this feature.

## Requirements Traceability *(mandatory — constitution X)*

| Requirement ID (`docs/requirements.md`) | What 007 delivers |
|---|---|
| **TR-FRONTEND** | Front-end that consumes the existing API |
| **FE-INTEGRATION** | Integrates the established backend with a modern frontend |
| **FE-RESPONSIVE** | Responsive, user-centric interface (mobile-first) |
| **FE-CRUD** | Standard CRUD against the defined functional use cases (including delete of a local record) |
| **FE-ARCH** | Clean component organisation and efficient server-state management (constitution VII) |
| **HU-01** | Catalog list UI (paginated National Dex browse) |
| **HU-02** | Detail view UI |
| **HU-03** | Import-to-local-collection UI + collection list (shared store) |
| **HU-04** | Edit local proprietary fields UI |
| **TR-API-CRUD** / **FE-CRUD** | Delete local record UI (not a new user story) |
| **TR-API-AUTH** | Login and register screens; client session for protected writes |

**Explicitly not delivered here**: new or renamed user stories; per-user
collections; roles; refresh tokens; password reset; i18n; dark-mode
toggle; offline mode; **any backend or API contract change**.

## User Scenarios & Testing *(mandatory)*

### TR-API-AUTH — Login (Priority: P1)

A visitor signs in with email and password so they can perform protected
collection writes. Unauthenticated users who hit a login-required action
are sent here and return to the page they came from after success.

**Why this priority**: Protected writes from HU-03/HU-04/FE-CRUD require a
session; login is the returning-user path and the demo path.

**Independent Test**: Open `/login`, sign in with the demo credential →
land on the intended return page authenticated; wrong password → generic
error; expired session on a write → session-expired **dialog**, then
**Sign in again** lands here with `reason=expired`.

**Acceptance Scenarios**:

1. **Given** a visitor on `/login` with a safe internal return path,
   **When** they enter a valid email and password and submit, **Then**
   they receive a session, the password is never shown in the UI after
   submit, and they are redirected to that return path (or `/` if none /
   unsafe).
2. **Given** the demo hint is visible, **When** the visitor reads it,
   **Then** they see `demo@pokefolio.dev` / `Demo-Passw0rd!` clearly
   labelled as a **demo credential** (not a production secret).
3. **Given** wrong credentials, **When** they submit, **Then** they see
   exactly **Invalid email or password** (no indication whether the email
   exists) and remain on `/login`.
4. **Given** a visitor is not signed in, **When** they choose to create an
   account, **Then** they can navigate to `/register` via a link on the
   login screen.

---

### TR-API-AUTH — Register (Priority: P1)

A new visitor creates an account and is signed in immediately (no second
login step).

**Why this priority**: Equal to login for first-time users who need to
write to the collection.

**Independent Test**: Register a unique email with valid fields → signed
in and redirected; duplicate email (any casing) → conflict message;
invalid field → inline error naming that field.

**Acceptance Scenarios**:

1. **Given** a visitor on `/register`, **When** they submit a valid name
   (1–100 characters), valid email, and password (8 characters to 72 UTF-8
   bytes), **Then** they are signed in immediately and redirected as after
   login (no separate login step).
2. **Given** the server rejects a field with **400**, **When** the
   response names a property, **Then** that field shows the error inline.
3. **Given** the email is already registered, **When** they submit,
   **Then** they see **An account with this email already exists** and the
   email address is not echoed back as a leak of server internals beyond
   that message.
4. **Given** client-side rules (name length, email shape, password length
   / UTF-8 byte cap), **When** the visitor submits invalid input before a
   successful round-trip, **Then** the form blocks or explains the rule
   without a confusing empty failure.

---

### HU-01 — Catalog list (Priority: P1)

Visitors browse the National Dex in a paginated grid. The page number lives
in the URL so links are shareable and the browser back button works.

**Why this priority**: Primary browse experience for the exercise.

**Independent Test**: Open `/` and `/?page=2` → nine cards per page with
sprite, name, category, weight (kg, one decimal), and abilities as returned
by the API; change page → URL updates; loading and upstream-failure states
are visible.

**Acceptance Scenarios**:

1. **Given** the catalog is available, **When** a visitor opens `/` (or
   `/?page=1`), **Then** they see a responsive grid of up to **9** Pokémon
   per page; each card shows sprite, name, category, weight in kg to one
   decimal place, and abilities/skills as returned by the API.
2. **Given** more than one page of results, **When** the visitor goes to
   another page, **Then** the URL reflects `?page=N` (**1-based**), the
   grid updates, and the browser back button restores the previous page.
3. **Given** catalog data is loading, **When** the visitor waits,
   **Then** they see skeleton placeholders (not a blank screen).
4. **Given** the upstream catalog is unavailable (**502**), **When** the
   list fails to load, **Then** they see an unavailable message with a
   **retry** action.
5. **Given** `page` is beyond the last catalog page, **When** the visitor
   opens that URL, **Then** they see an empty state with a link back to
   page 1 (no infinite spinner, no crash).

---

### HU-02 — Pokémon detail (Priority: P1)

Visitors open a single Pokémon’s detail: image, core stats (with total),
flavor text, evolution lineage, and abilities as returned by the API
(name + optional **Hidden ability** label; no descriptions). The page
also checks the shared collection so the UI can show saved vs not-saved
proactively.

**Why this priority**: Completes the catalog browse journey.

**Independent Test**: Open `/pokemon/1` → detail content and collection
probe; unknown id → not-found; **502** → retry; evolution shown as a
sequence for linear chains and as a list when branching.

**Acceptance Scenarios**:

1. **Given** Pokémon *id* exists in the catalog, **When** a visitor opens
   `/pokemon/:id`, **Then** they see image, core stats including **total**,
   flavor text, evolution lineage (linear chain as a sequence; branching
   evolutions as a list), and abilities showing each **name** plus a
   **Hidden ability** label when `hidden` is true (no ability
   descriptions).
2. **Given** the detail page loads, **When** the client calls public
   `GET /api/v1/collection/{id}`, **Then** **404** shows the not-saved
   variant (**Add to collection**) and **200** shows the saved variant
   (**Already in the collection** / **Saved in the shared collection**).
3. **Given** *id* is unknown in the catalog, **When** they open the
   detail route, **Then** they see a not-found state.
4. **Given** the upstream catalog is unavailable, **When** detail fails
   with **502**, **Then** they see an unavailable message with **retry**.

---

### HU-03 — Add to collection from detail (Priority: P1)

From detail, a signed-in user imports the Pokémon into the shared local
collection via the Figma import flow (confirmation → in progress →
success or 409). Guests are sent to login with a return path.

**Why this priority**: Creates the local record that HU-04 and delete act
on.

**Independent Test**: While signed in, import from detail through the
confirm/progress dialogs → success with link to the record; duplicate →
409 dialog; while signed out → login with return to the detail page.

**Acceptance Scenarios**:

1. **Given** the user is signed in and the Pokémon is not in the local
   collection, **When** they choose **Add to collection** and confirm,
   **Then** they see an in-progress state, then a success dialog with a
   link to the new record, and the collection view reflects the record
   (without requiring a full page reload of unrelated screens).
2. **Given** the Pokémon is already in the collection (**409** race or
   retry), **When** import fails with conflict, **Then** they see a
   **409** dialog with **Already in the collection** and a link to that
   local record.
3. **Given** the user is not signed in, **When** they choose **Add to
   collection**, **Then** they are sent to `/login` with a return path
   back to this detail page after success.

---

### HU-03 / HU-04 — Collection list (Priority: P1)

Anyone can view the shared local collection (public read). The API
paginates the list (default **9**, max **20**); the UI uses **9** per page
with `?page=N` in the URL like the catalog. Edit and delete actions
require login.

**Why this priority**: Surfaces local data and entry points to edit/delete.

**Independent Test**: Open `/collection` and `/collection?page=2` signed
out → paginated list visible, write actions prompt login; empty collection
→ empty state; signed in → edit and delete available.

**Acceptance Scenarios**:

1. **Given** the local collection has records, **When** a visitor opens
   `/collection` (or `/collection?page=1`), **Then** they see a paginated
   shared list (default **9** per page) including proprietary fields
   (localized name, location, tags) and other version-independent display
   info needed to recognise each entry (no auth required to **read**).
2. **Given** more than one page of collection results, **When** the
   visitor goes to another page, **Then** the URL reflects
   `/collection?page=N` (**1-based**), the list updates, and the browser
   back button restores the previous page.
3. **Given** the collection has no records, **When** they open
   `/collection`, **Then** they see an empty-collection state.
4. **Given** `page` is beyond the last collection page, **When** the
   visitor opens that URL, **Then** they see an empty state with a link
   back to page 1 (no infinite spinner, no crash).
5. **Given** the visitor is not signed in, **When** they choose edit or
   delete, **Then** they are sent to `/login` with a return path.
6. **Given** the visitor is signed in, **When** they choose edit,
   **Then** they navigate to `/collection/:id` for that national number.

---

### HU-04 — Edit local proprietary fields (Priority: P2)

A signed-in user edits localized name, location, and tags (add/remove
chips) within API limits, sending the `version` from the last read. No
save confirmation modal.

**Why this priority**: Completes HU-04 once import exists.

**Independent Test**: Edit and save → updated list/detail; stale
**409** → “Modified elsewhere — reload” with reload; **400** → inline
field errors.

**Acceptance Scenarios**:

1. **Given** a signed-in user on `/collection/:id`, **When** they change
   localized name, location, and/or tags within API limits and save,
   **Then** the update succeeds without a confirmation modal and uses the
   `version` previously read from GET.
2. **Given** another change made the record stale (**409**
   stale-version), **When** save fails, **Then** they see **Modified
   elsewhere — reload** with a reload action; nothing is kept silently as
   if save succeeded.
3. **Given** the server returns **400** naming a field, **When** save
   fails, **Then** that field shows the error inline and focus moves to
   an errored field when appropriate.
4. **Given** the national number is not in the local collection,
   **When** they open the edit route, **Then** they see a not-found
   state.

---

### TR-API-CRUD / FE-CRUD — Delete local record (Priority: P2)

A signed-in user deletes a local record after confirming in a dialog, then
returns to the collection list.

**Why this priority**: Completes FE-CRUD; not a new user story.

**Independent Test**: Confirm delete → record gone and user is on
`/collection`; cancel → no delete.

**Acceptance Scenarios**:

1. **Given** a signed-in user on the edit screen (or an equivalent delete
   entry from the collection), **When** they choose delete, **Then** a
   confirmation dialog appears with a title **and** a description.
2. **Given** they confirm, **When** delete succeeds, **Then** they return
   to `/collection` and the record is gone.
3. **Given** they cancel the dialog, **When** the dialog closes,
   **Then** the record remains and they stay on the edit context.

---

### Cross-cutting — Auth session, errors, and quality (Priority: P1)

Session lifetime, 401 handling, shared UI states, accessibility, and
responsive layout apply across screens.

**Why this priority**: Without this, protected flows and quality bars
fail interview criteria (constitution VII, FE-RESPONSIVE).

**Independent Test**: Expire or clear the token → protected write sends
the user to login with session-expired copy; public GETs never attach the
token; console clean on each main screen/state; narrow viewport shows
1-column grid.

**Acceptance Scenarios**:

1. **Given** a stored session with `expiresIn`, **When** the app loads
   after expiry, **Then** the expired token is discarded (user appears
   signed out).
2. **Given** a protected write returns **401**, **When** the client
   handles it, **Then** the session is cleared and a session-expired
   **dialog** is shown; **Sign in again** navigates to
   `/login?redirect=<sanitised current path+search>&reason=expired`;
   **Browse catalog** navigates to `/`.
3. **Given** the user chooses logout, **When** they confirm the control,
   **Then** the client clears the session with **no** server logout call.
4. **Given** any public catalog or collection **read**, **When** the
   request is made, **Then** it never sends or depends on the access
   token.
5. **Given** loading, empty, not-found, upstream **502**, or session
   expired states, **When** they occur on a screen, **Then** each has a
   distinct, understandable presentation (skeletons for loading; retry
   for **502**; not-found for unknown Pokémon, not-in-collection, and
   unknown routes; session expired as the dialog above). A **403**
   access-denied screen is **out of scope** (the UI never calls denied
   routes).
6. **Given** a mobile-width viewport, **When** the visitor views each
   primary screen, **Then** layout follows the corresponding **mobile
   Figma frame** (catalog/collection grid **1 / 2 / 3** columns as width
   grows).
7. **Given** normal use of every screen and primary state, **When** the
   browser console is inspected, **Then** it shows **zero** warnings and
   **zero** errors (including accessible dialogs with title and
   description so overlay libraries do not warn).

---

### Edge Cases

- Unknown application route → not-found (not a blank crash).
- `page=0` or invalid catalog/collection page → handled as a user-visible
  error or redirect consistent with the API’s **400** behaviour (no
  infinite spinner).
- Page beyond the last page (catalog or collection) → empty state with a
  link back to page 1 (no infinite spinner, no crash).
- Login/register return path MUST be an internal relative path starting
  with `/` and not `//`; anything else (absolute URLs, protocol-relative
  `//…`, empty, or missing) falls back to `/` (no open redirect).
- Register/login while already signed in → either complete the new
  session or redirect to the default signed-in landing (no broken double
  session UI).
- Network failure distinct from **502** → user-visible failure with
  retry where the screen already supports retry.
- Tags chips: add/remove within API limits; blank or over-limit tags
  surface as field errors.
- Concurrent edit by another client → stale-version path only (no silent
  overwrite).
- Deep link to `/collection/:id` or `/pokemon/:id` while signed out →
  reads still work; write actions route through login with return path.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The product MUST expose six primary screens: Login
  (`/login`), Register (`/register`), Catalog (`/` and `?page=N`), Detail
  (`/pokemon/:id`), Collection (`/collection` and `/collection?page=N`),
  Edit (`/collection/:id`), plus shared states for loading (skeletons),
  empty collection, not-found, upstream unavailable with retry, import
  confirmation / in-progress / success / duplicate-409 dialogs, delete
  confirmation, and session-expired dialog. A **403** access UI MUST NOT
  be implemented (document only).
- **FR-002**: Login MUST accept email and password; show the demo
  credential hint labelled as demo; show **Invalid email or password** on
  credential failure; link to register; after success redirect to the
  return path that required login. The return path MUST be an internal
  relative path starting with `/` and not `//`; any other value MUST fall
  back to `/` (no open redirect). Register success MUST use the same
  return-path rules.
- **FR-003**: Register MUST collect name, email, and password with the
  same rules as the API (name 1–100, valid email, password 8 chars to 72
  UTF-8 bytes); map server **400** to the named field; show **An account
  with this email already exists** on **409**; on success establish a
  session immediately.
- **FR-004**: Catalog MUST show a paginated grid of **9** per page with
  sprite, name, category, weight (kg, one decimal), and abilities as
  returned by the API (ability **name**; **Hidden ability** label when
  hidden; no descriptions); page MUST be **1-based** and reflected in
  the URL. A page beyond the last page MUST show an empty state with a
  link to page 1.
- **FR-005**: Detail MUST show image, core stats with total, flavor text,
  evolution lineage (linear as sequence, branching as list), and
  abilities (name + **Hidden ability** when applicable; no descriptions).
  Detail MUST call public `GET /api/v1/collection/{id}` to choose the
  saved vs not-saved variant (**404** → not saved; **200** → saved with
  **Already in the collection** / **Saved in the shared collection**).
  Import MUST follow confirmation → in-progress → success (link to
  record) or **409** dialog; CTA copy **Add to collection** (login-gated).
- **FR-006**: Collection list MUST be readable without login; MUST be
  paginated like the catalog (**9** per page by default, API max **20**;
  UI uses **9**) with **1-based** `?page=N` in the URL so links are
  shareable and the browser back button works; a page beyond the last
  page MUST show an empty state with a link to page 1; show proprietary
  fields (localized name, location, tags) and enough identity info to
  recognise entries; edit and delete MUST require login.
- **FR-007**: Edit MUST update localizedName, location, and tags
  (add/remove chips) within API limits; send the `version` from GET; no
  save confirmation modal; **409** stale-version → **Modified elsewhere —
  reload** with reload; **400** field errors inline.
- **FR-008**: Delete MUST use a confirmation dialog (title and
  description) then return to the collection on success.
- **FR-009**: The client MUST keep the access token and its expiry
  (`expiresIn`); discard expired tokens on load; on **401** from a
  protected write clear the session and show the session-expired dialog
  (**Sign in again** →
  `/login?redirect=<sanitised current>&reason=expired`; **Browse
  catalog** → `/`); logout clears the session locally only; public reads
  MUST NOT send or depend on the token. No refresh tokens.
- **FR-010**: UI copy MUST be in English and MUST use shared-collection
  wording (**Collection**, **Add to collection**, **Already in the
  collection**, **Saved in the shared collection** — not “my/your”
  possession). Interactive elements MUST be labelled; keyboard navigation
  MUST work for primary flows; errors MUST be announcable (`aria-live`
  for async errors); images MUST contain appropriate alt text; dialogs
  MUST expose title and description. Login MUST show the demo credential
  hint even though the Figma login frames omit it.
- **FR-011**: The layout MUST be responsive and MUST follow the Figma
  **mobile** frames for every screen (catalog/collection grid **1 / 2 /
  3** columns as width grows). The browser console MUST show zero
  warnings and zero errors on every screen and primary state under normal
  use.
- **FR-012**: Automated tests MUST cover API problem+json parsing, auth
  / session logic, each page’s main states, and form validation, mocking
  at the API-client boundary. The existing frontend CI job (lint,
  typecheck, test, build, nginx smoke) MUST stay green.
- **FR-013**: This feature MUST NOT change the backend or API contracts.
  This feature MUST **add** a Vite dev-server proxy that forwards `/api`
  unchanged (no path rewrite) to the backend (default
  `http://localhost:8080`). The existing nginx Docker proxy (same
  no-rewrite behaviour) MUST remain as is.
- **FR-014**: Architecture MUST follow constitution VII / FE-ARCH:
  presentational components receive props and never fetch; pages/containers
  wire data loading to those components; server/remote state MUST use a
  single agreed client cache with stable query keys and MUST invalidate
  collection data after import, update, and delete; a typed API client
  MUST parse RFC 7807 problem+json into a typed error (status, type,
  detail, field when present). Concrete libraries are fixed in Assumptions
  (constitution Technology Stack).

### Key Entities *(client-facing)*

- **Session**: Client-held access token, expiry, and public user profile
  (`id`, `name`, `email`) established by login or register.
- **Catalog page**: Paginated list of Pokémon summary cards for HU-01.
- **Collection page**: Paginated list of shared local records for
  HU-03/HU-04 (same paging rules as the catalog UI).
- **Pokémon detail**: Full catalog view for HU-02 including evolution
  lineage.
- **Local collection entry**: Shared local record with proprietary fields
  and optimistic-lock version used by HU-03/HU-04/FE-CRUD.
- **Problem error**: Parsed API failure with status, type, detail, and
  optional field name for inline form mapping.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A visitor can browse at least two catalog pages using only
  the URL (share and browser back) and always see at most nine cards per
  page when the catalog is available.
- **SC-002**: A new user can register, land authenticated, and import a
  Pokémon from detail into the collection in one continuous flow without a
  second login step.
- **SC-003**: A signed-in user can edit proprietary fields and delete a
  local record (with confirmation) so the collection list reflects the
  change without a manual full-app refresh; a visitor can also browse at
  least two collection pages using only `/collection?page=N` (share and
  browser back) with at most nine items per page when records exist.
- **SC-004**: An unauthenticated user can still read catalog and
  collection pages; attempting a write always results in a login redirect
  with a working return path after success.
- **SC-005**: On a phone-sized viewport, the catalog remains usable
  (single-column grid, no horizontal overflow of primary content).
- **SC-006**: Under normal use of every primary screen and state, the
  browser developer console shows zero warnings and zero errors.
- **SC-007**: Stale-version and duplicate-import conflicts show the
  specified English messages and never look like a silent success.
- **SC-008**: Frontend CI (lint, typecheck, unit tests, production build,
  nginx smoke) passes for the feature branch.

## Assumptions

- Backend 002–006 APIs and auth behaviour are already correct; 007 only
  consumes them.
- Demo credential remains `demo@pokefolio.dev` / `Demo-Passw0rd!` as
  documented in the README (demo label required in the UI).
- Default post-login / post-register landing when no safe return path is
  set is the catalog (`/`). Safe return paths are internal relative paths
  starting with `/` and not `//` only.
- Collection remains a **single shared** store (same product decision as
  006); UI copy MUST NOT imply per-user ownership (shared wording wins
  over Figma’s per-user copy — research R11 conflict **1**).
- Vite has **no** dev proxy today; this feature MUST add one that forwards
  `/api` unchanged to the backend (default `http://localhost:8080`). Nginx
  in Docker already proxies `/api` without rewrite and stays unchanged.
- Stack mandated by constitution VII / Technology Stack (and in scope for
  FE-ARCH planning): React, TypeScript, Vite, Tailwind, shadcn/ui where
  helpful, TanStack Query for server state, React Router, Vitest + Testing
  Library; no new mocking library unless the plan justifies it.
- Visual SoT is Figma
  [PokeApp](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp);
  frame map in `contracts/ui-routes.md`. Where Figma conflicts with this
  spec on copy/flows/states, **the spec wins** (R11 conflicts 1–7). File
  has no Figma variables — tokens come from catalog/detail design
  context into `src/index.css`.
- Out of scope: per-user collections, roles, refresh tokens, password
  reset, i18n, dark-mode toggle, offline mode, backend changes, **403
  access UI**, GENAI docs (separate deliverable path per feature map).
