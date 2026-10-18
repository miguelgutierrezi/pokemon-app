# Feature Specification: Authentication API (TR-API-AUTH)

**Feature Branch**: `006-auth`

**Created**: 2026-10-17

**Status**: Draft

**Input**: Feature 006-auth — TR-API-AUTH (auxiliary auth API: registration,
authentication, protected vs public routes), TR-DB (user collection),
SUB-SEED (mock credentials). This is **not** a new user story: do not add,
rename, or merge HU-01..HU-04; label scenarios as **TR-API-AUTH**. Closes the
constitution VI temporary exception recorded in 004/005 Complexity Tracking
for **public write routes**. Collection remains a **single shared** store
(no per-user ownership in this feature).

## Requirements Traceability *(mandatory — constitution X)*

| Requirement ID (`docs/requirements.md`) | What 006 delivers |
|---|---|
| **TR-API-AUTH** | Auxiliary API for registration, authentication (login), and management of protected versus public routes |
| **TR-DB** | Relational persistence for the **user** collection (account entity) |
| **TR-ERRORS** | Auth and authorization failures return RFC 7807 Problem Details via the shared advice (`conflict`, `unauthorized`, `forbidden`, `bad-request`) with no stack/package leaks |
| **TR-UNIT** | Domain, use cases, adapters, and web/security covered per module gate |
| **SUB-SEED** / **DEL-SEED** | One seeded demo user with documented mock credentials (not a production secret) |

**Explicitly not delivered here**: new or renamed user stories (HU-01..HU-04
unchanged); per-user Pokémon collections / ownership; roles or admin;
refresh tokens; password reset; email verification; rate limiting; account
update/delete; CORS (same-origin via nginx / Vite proxy in 007); frontend
(007). Catalog read APIs and collection **GET** remain public.

## User Scenarios & Testing *(mandatory)*

### TR-API-AUTH — Register and receive a session token (Priority: P1)

A new visitor creates an account with name, email, and password. On success
they are signed in immediately (same response shape as login) so they can
call protected write routes without a separate login step.

**Why this priority**: Without registration there is no authenticated
caller for protected writes; this is the primary onboarding path.

**Independent Test**: Register a unique email with a valid password →
**201** and a response containing an access token and user profile; a
second registration with the same email (any casing) → **409** conflict;
invalid body → **400** naming the property.

**Acceptance Scenarios**:

1. **Given** email *e* is not registered, **When** a client registers with
   a valid name, email *e*, and password, **Then** the API responds
   **201** with the same body shape as login (`accessToken`, `tokenType`
   `Bearer`, `expiresIn` in seconds, `user` with `id`, `name`, `email`) and
   the password is never returned.
2. **Given** email *e* is already registered, **When** a client registers
   with the same email ignoring case, **Then** the API responds **409**
   with `type` `urn:problem-type:conflict`.
3. **Given** two concurrent registrations with the same email (any
   casing), **When** both complete, **Then** exactly one receives
   **201** and the other **409** `urn:problem-type:conflict` (never
   **500**), detected via the unique constraint name (same pattern as
   004 duplicate national number).
4. **Given** the body is missing a required property, has wrong types,
   fails validation (name/email/password rules), or includes an unknown
   property, **When** a client registers, **Then** the API responds
   **400** with an RFC 7807 Problem Detail that names the offending field
   (never Jackson/internal class messages).

---

### TR-API-AUTH — Login (Priority: P1)

An existing user signs in with email and password and receives a bearer
access token for protected routes.

**Why this priority**: Equal priority to register for returning users and
for the seeded demo account.

**Independent Test**: Login with the demo (or a registered) user →
**200** with token + user; wrong email, wrong password, or too-short
password → **401** with one generic detail; missing/blank fields or
unknown properties → **400**; timing must not reveal whether the email
exists.

**Acceptance Scenarios**:

1. **Given** a registered user with email *e*, **When** they login with
   correct email (any casing / surrounding whitespace) and password,
   **Then** the API responds **200** with `accessToken`, `tokenType`
   `Bearer`, `expiresIn` (seconds), and `user { id, name, email }` after
   normalising email (trim + lowercase).
2. **Given** an unknown email, a wrong password for a known email, or a
   password that would fail registration length rules (e.g. too short),
   **When** a client attempts login with non-blank email and password,
   **Then** the API responds **401** with a **single** generic detail
   (`Invalid email or password`) — registration length/format rules do
   **not** apply to login.
3. **Given** an unknown email, **When** login is attempted, **Then** the
   server still performs a password-hash comparison against a dummy hash
   so response timing does not reveal whether the account exists.
4. **Given** the login body omits email or password, sends them blank, or
   includes an unknown property, **When** a client attempts login,
   **Then** the API responds **400** naming the property (login only
   requires non-blank email and password).

---

### TR-API-AUTH — Protected vs public routes (Priority: P1)

Clients can read the catalog and collection without a token. Creating,
updating, or deleting local collection records requires a valid bearer
token. Everything else is denied by default.

**Why this priority**: This closes the constitution VI temporary public-write
exception from 004/005 while keeping reads open for the upcoming UI.

**Independent Test**: GET catalog/collection without token → **200**;
GET with expired/tampered Bearer still → **200** (token ignored);
POST/PUT/DELETE collection without token → **401**; same writes with a
valid token → success; expired or tampered token on protected writes →
**401**; `GET /api/v1/does-not-exist` → **401** without token, **403**
with a valid token.

**Acceptance Scenarios**:

1. **Given** no Authorization header, **When** a client calls any GET
   under `/api/v1/pokemon/**` or `/api/v1/collection/**`, **Then** the
   request succeeds subject to existing 004/005 rules (not blocked by
   auth).
2. **Given** an expired, malformed, or tampered Bearer token, **When** a
   client calls any GET under `/api/v1/pokemon/**` or
   `/api/v1/collection/**`, **Then** the request still succeeds subject to
   existing 004/005 rules — the token is **ignored** on public routes so
   a stale client token never blocks catalog or collection reading.
3. **Given** no Authorization header, **When** a client calls
   `POST /api/v1/collection`, `PUT` or `DELETE`
   `/api/v1/collection/{nationalNumber}`, **Then** the API responds
   **401** with `type` `urn:problem-type:unauthorized` and a
   `WWW-Authenticate: Bearer` header.
4. **Given** a valid access token for an authenticated user, **When**
   they call a protected collection write, **Then** the write proceeds
   under existing 004/005 business rules (collection remains the single
   shared store — not scoped per user).
5. **Given** an expired, malformed, or tampered bearer token, **When** a
   client calls a protected write, **Then** the API responds **401**
   unauthorized (not **500**). Expired/tampered tokens yield **401**
   **only** on protected writes — never on public GETs (see scenario 2).
6. **Given** deny-by-default, **When** a client calls
   `GET /api/v1/does-not-exist` without a token, **Then** the API
   responds **401** unauthorized; **When** the same path is called with
   a valid Bearer token, **Then** the API responds **403** with
   `type` `urn:problem-type:forbidden`.
7. **Given** no token, **When** a client calls register, login, health,
   or OpenAPI/Swagger UI, **Then** those endpoints remain reachable
   (public).

---

### TR-API-AUTH — Seeded demo credentials (Priority: P2)

Operators and reviewers can sign in with a documented demo account without
running a register step (SUB-SEED / DEL-SEED).

**Why this priority**: Required for demos and for updating 004/005 write
ITs; not the primary product path.

**Independent Test**: After a fresh migrate, login with the documented
demo email and mock password → **200** and a usable token.

**Acceptance Scenarios**:

1. **Given** a database migrated through the new user migration, **When**
   a client logs in as `demo@pokefolio.dev` with the documented mock
   password, **Then** login succeeds (**200**) and the token authorizes
   protected writes.
2. **Given** source control and compose files, **When** reviewers inspect
   them, **Then** the demo password is clearly labelled as a **demo
   credential** (not a production secret), and the signing secret is
   supplied only via environment (never hardcoded with a default).

---

### Edge Cases

- Email uniqueness is **case-insensitive**; stored email is normalised to
  lowercase (trim + lowercase on register and login).
- Concurrent register with the same email (any casing) → one **201**, one
  **409** conflict (never **500**), via unique constraint name (004
  pattern).
- Name is required, trimmed, max 100 characters; blank after trim →
  **400** (register only).
- Register password: min 8 characters; max **72 bytes** (hashing limit);
  over-long or under-min → **400**. Login does **not** apply those
  length/format rules: any credential mismatch (including too-short
  password) → **401** `Invalid email or password`.
- Login body: only non-blank email and password required; missing/blank
  or unknown property → **400**.
- Password is stored only as a one-way hash; never returned in any
  response.
- Public GETs ignore Bearer credentials: expired/malformed/tampered
  tokens MUST NOT turn a public read into **401**.
- Missing/invalid/expired token on protected writes → **401**, never the
  catch-all **500**.
- Access denied on authenticated-but-forbidden routes (e.g.
  `GET /api/v1/does-not-exist` with a valid token) → **403**.
- Business/domain errors on public or protected routes (404, 400, 409,
  415, 502, …) MUST keep their real status as Problem Details — the
  security layer MUST NOT rewrite them to **401**/**403** via `/error`
  dispatch.
- Unknown properties on register/login bodies → **400** (same
  fail-on-unknown posture as 005).
- No refresh tokens and no server-side logout: clients discard the token
  when done.
- Collection write success still means the **shared** global collection
  (not a private per-user bag).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow creating a user account with required
  **name** (trimmed, max 100), **email** (valid format, max 254, unique
  case-insensitively, stored lowercase), and **password** (min 8
  characters, max 72 bytes), persisted with a unique surrogate id and a
  one-way password hash only.
- **FR-002**: `POST /api/v1/auth/register` MUST return **201** and the
  same response body as login (immediate sign-in). Duplicate email
  (including concurrent races, any casing) MUST return **409**
  `urn:problem-type:conflict` (never **500**), detected via the unique
  constraint name like 004. Invalid bodies MUST return **400** naming
  the property.
- **FR-003**: `POST /api/v1/auth/login` MUST return **200** with
  `accessToken`, `tokenType` `"Bearer"`, `expiresIn` (seconds), and
  `user { id, name, email }`. Login validates only that email and
  password are present and non-blank (plus reject unknown properties) —
  **400** if missing/blank/unknown; registration length/format rules do
  **not** apply. Email MUST be normalised (trim + lowercase) on login.
  Wrong email, wrong password, or any other credential mismatch
  (including a too-short password) MUST return **401** with detail
  exactly `Invalid email or password` (one message for all). Login for
  unknown emails MUST still run a hash comparison so timing does not
  reveal account existence.
- **FR-004**: Access tokens MUST be stateless JWTs (HS256) signed with
  secret from env var `JWT_SECRET` (no default; process MUST fail fast at
  startup if missing or shorter than 32 bytes). Token expiry MUST be
  configurable with default **1 hour**. No refresh tokens; no server-side
  logout.
- **FR-005**: Route policy MUST be:
  - **Public**: all GET under `/api/v1/pokemon/**` and
    `/api/v1/collection/**`; `POST /api/v1/auth/register`;
    `POST /api/v1/auth/login`; `/actuator/health`; OpenAPI docs and
    Swagger UI. Public routes MUST **ignore** Bearer credentials
    (expired, malformed, or tampered tokens MUST NOT block the request).
  - **Protected** (valid Bearer token): `POST /api/v1/collection`;
    `PUT` and `DELETE /api/v1/collection/{nationalNumber}`.
  - **Deny by default**: every other route — e.g.
    `GET /api/v1/does-not-exist` → **401** without token, **403**
    `urn:problem-type:forbidden` with a valid token.
  - Errors from public or protected handlers (404, 400, 409, 415, 502,
    …) MUST keep their real status as Problem Details; the security layer
    MUST NOT turn them into **401**/**403** via `/error` dispatch.
- **FR-006**: The local Pokémon collection MUST remain a **single shared**
  store (no per-user ownership or filtering in this feature). Protecting
  writes closes the 004/005 public-write exception to constitution VI;
  ownership scoping is out of scope here.
- **FR-007**: Missing, invalid, or expired credentials on **protected**
  routes MUST return **401** `urn:problem-type:unauthorized` with
  `WWW-Authenticate: Bearer`. Authenticated callers denied a route MUST
  receive **403** `urn:problem-type:forbidden`. Security failures MUST
  NOT fall through to the catch-all **500** handler. Bodies MUST obey the
  same no-leak rules as 002–005 (no stack traces, `tools.jackson`,
  `com.fasterxml`, or exception class names). No error from a public or
  protected route may be rewritten to **401**/**403** by the security
  layer via `/error` dispatch — business statuses stay as Problem
  Details.
- **FR-008**: Flyway MUST add the next migration after V4 for the user
  table and seed exactly one demo user (e.g. `demo@pokefolio.dev`) with a
  BCrypt password hash. The mock plaintext password MUST be documented in
  the README as a demo credential. No real secrets in source; compose
  MUST require `${JWT_SECRET:?}`.
- **FR-009**: Domain MUST own the user model and validation; application
  MUST expose ports (user repository, password hasher, token issuer) and
  register/login use cases; infrastructure MUST adapt persistence,
  hashing, and token issuing; web MUST host Spring Security configuration
  and controllers. Architecture tests MUST forbid
  `org.springframework.security..` dependencies from domain and
  application packages.
- **FR-010**: OpenAPI MUST document a bearer security scheme, mark
  protected operations as requiring it, and document **401** / **403**
  responses.
- **FR-011**: Automated tests MUST cover domain and use cases; persistence
  adapters with Postgres; web/security slices; and an end-to-end flow
  register → token → protected write succeeds, no token → **401**,
  expired/tampered token → **401**, public GETs work without token.
  Existing 004/005 collection **write** ITs MUST authenticate.
- **FR-012**: README MUST document auth endpoints, curl examples using the
  demo user, and `JWT_SECRET`. The **006** plan Complexity Tracking MUST
  record that this feature **supersedes** the 004/005 constitution VI
  public-write exception and **narrows** their “scope records per user”
  follow-up (shared collection stays — explicit decision). Do **not**
  edit the merged 004/005 plans.

### Key Entities

- **User account**: Authenticated caller identity with surrogate id,
  display name, normalised email, and password hash. No roles beyond
  “authenticated user”.
- **Access token**: Short-lived bearer credential proving authentication
  for protected writes; opaque to clients beyond use in
  `Authorization: Bearer …`.
- **Auth session response**: Token metadata plus public user profile
  (`id`, `name`, `email`) returned by register and login.
- **Local Pokémon collection** (unchanged ownership model): Still one
  shared store; only the **write** routes become authenticated.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can register and immediately perform a protected
  collection write (import or update) using only the token from the
  register response, without a separate login, in one continuous flow.
- **SC-002**: Unauthenticated callers cannot create, update, or delete
  local collection records (**401**); they can still list and get
  catalog/collection data.
- **SC-003**: Login failures never disclose whether an email is
  registered (same status, same detail text for wrong email and wrong
  password).
- **SC-004**: On a fresh local stack, login with the documented demo
  credentials completes in ≤ **1 s** (login request only — no import
  timing).
- **SC-005**: Every authn/authz error body is an RFC 7807 Problem Detail
  with no stack traces or framework package names; security failures
  never surface as undifferentiated **500** responses.

## Assumptions

- Backend-only; login UI hint for the demo password is deferred to 007.
- Demo account email defaults to `demo@pokefolio.dev`; exact mock
  plaintext password is chosen at implement/plan time and documented in
  README (labelled demo credential).
- Token signing uses HS256 with `JWT_SECRET` from the environment;
  docker-compose uses `${JWT_SECRET:?}`; expiry default 3600 seconds,
  overridable by configuration key fixed in plan.
- Password hashing uses BCrypt; password length capped at 72 **bytes** to
  match the hasher’s limit.
- Spring Security with OAuth2 resource-server JWT (Nimbus) is the
  constitution-mandated stack; no third-party JWT library.
- HTTP shapes (for planning):
  - `POST /api/v1/auth/register`
    `{"name","email","password"}` → **201** auth session body.
  - `POST /api/v1/auth/login` `{"email","password"}` → **200** auth
    session body.
- Existing 004/005 write integration tests will obtain a token (demo login
  or register) before POST/PUT/DELETE; GET tests stay unauthenticated.
- Closing constitution VI here means **JWT-protected collection writes**.
  Earlier 004/005 Complexity Tracking text that also mentioned “scope
  records per user” is **narrowed**: per-user ownership remains out of
  scope for 006 (explicit product decision). The **006** plan Complexity
  Tracking records that supersession/narrowing; merged 004/005 plans are
  left unchanged.
- Out of scope: per-user collections, roles/admin, refresh tokens,
  password reset, email verification, rate limiting, account
  update/delete, CORS (same-origin through nginx / Vite proxy in 007),
  frontend.
