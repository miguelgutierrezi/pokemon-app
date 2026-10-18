# Research: 006-auth (TR-API-AUTH)

**Feature**: `006-auth` | **Date**: 2026-10-17

Verified against Spring Boot **4.1.1** / Spring Security **7** docs and
Maven Central artifact metadata at plan time. Re-check at implement with
`-Dmaven.compiler.showDeprecation=true`; no deprecated APIs.

---

## R1 — Maven dependencies (constitution IX, first use)

**Decision**:

| Artifact | Module | Role |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-security` | **web** | Security filter chain |
| `org.springframework.boot:spring-boot-starter-security-oauth2-resource-server` | **web** | OAuth2 resource-server JWT (Nimbus) — **Boot 4.1 rename** |
| `org.springframework.security:spring-security-crypto` | **infrastructure** | `BCryptPasswordEncoder` only |
| `org.springframework.security:spring-security-oauth2-jose` | **infrastructure** | `JwtEncoder` / `NimbusJwtEncoder` for `AccessTokenIssuer` adapter (BOM version, no explicit version) |
| `org.springframework.boot:spring-boot-starter-security-test` | **web** (test) | `@WithMockUser`, `jwt()` request post-processors, Security mockMvc |
| Optional umbrella | **web** (test) | `spring-boot-starter-security-oauth2-resource-server-test` — use if convenient |

Versions: Boot BOM **4.1.1** (omit explicit version in child POMs).

**Wiring split**:
- **web/config**: `@ConfigurationProperties(prefix = "pokemonapp.security.jwt")`
  (validated) + **one** config class that builds `SecretKey`, `JwtEncoder`,
  and `JwtDecoder` from those properties and exposes them as `@Bean`s.
- **infrastructure**: `AccessTokenIssuer` adapter injects the shared
  `JwtEncoder` bean (and `Clock` for `iat`/`exp` of issued tokens).

**Rationale**: Constitution VI + IX — Security enters with the feature that
first needs it. Boot 4.1.1 marks
`spring-boot-starter-oauth2-resource-server` **deprecated** in favor of
`spring-boot-starter-security-oauth2-resource-server`. Infra needs
`spring-security-oauth2-jose` for encoder types without pulling the full
web Security starter. Single SecretKey/encoder/decoder construction
avoids key drift between issue and verify.

**Alternatives rejected**:
- Old artifact id `spring-boot-starter-oauth2-resource-server` — deprecated
  in Boot 4.1.1.
- Third-party JWT (jjwt, auth0) — forbidden by spec/constitution.
- Putting `spring-boot-starter-security` on infrastructure — would widen
  the Security footprint and tempt filter-chain code outside web.
- Building a second SecretKey/encoder in infrastructure from raw env —
  duplicates validation and can diverge from the resource-server decoder.

---

## R2 — Domain model and application ports / use cases

**Decision**:
- Domain: `UserAccount` (+ factory) with value objects / validated fields —
  **Email** (public `Email.PATTERN` for normalised form + static
  `Email.isValid(String raw)` = trim + lowercase then max length +
  PATTERN — see data-model), **PersonName** (trim, constants for 1..100),
  **RawPassword** policy object used **only** at register (8 chars min,
  72 UTF-8 bytes max). Stored hash is an opaque string on the account;
  domain never depends on BCrypt types.
- Application ports: `UserAccountRepository`, `PasswordHasher`,
  `AccessTokenIssuer`.
- `RegisterUserUseCase` → creates user, hashes password, returns
  **AuthSession** (same as login — immediate sign-in).
- `AuthenticateUserUseCase` → normalises email; looks up user; if absent,
  compares against a **startup-computed dummy hash** then throws
  `InvalidCredentialsException`; if present, verifies password or throws
  the same exception. **No** password-policy validation on login.
- **Login password &gt; 72 UTF-8 bytes**: short-circuit to
  `InvalidCredentialsException` **without** calling the hasher (BCrypt
  rejects oversize inputs and would otherwise risk **500**). Cover with
  unit + IT.
- Dummy hash for unknown emails: computed **once at startup** via
  `PasswordHasher.hash(...)` (same cost as real hashes), not a hardcoded
  BCrypt literal.
- `DuplicateEmailException` for unique-email conflicts.

**Rationale**: Spec FR-001–FR-003; timing-safe unknown-email path; login
must not leak registration rules via **400**; oversize login must stay
**401** generic.

**Alternatives rejected**:
- Applying register password policy on login — would return **400** for
  short passwords and enumerate/confuse clients (spec iteration 2).
- Returning distinct exceptions for unknown email vs bad password —
  violates SC-003 / generic detail.
- Domain depending on Spring Security crypto types — ArchUnit + Clean
  Architecture.
- Hardcoded dummy BCrypt string — cost/format can drift from live hasher.
- Calling `hasher.matches` on &gt;72-byte login passwords — BCrypt may throw
  → catch-all **500**.

---

## R3 — Persistence: `app_user`, V5, constraint-name duplicates, seed

**Decision**:
- Flyway **V5** creates table **`app_user`** (`user` is reserved in
  Postgres): `id BIGSERIAL PK`, `name VARCHAR(100) NOT NULL`,
  `email VARCHAR(254) NOT NULL`, `password_hash VARCHAR(255) NOT NULL`,
  `created_at TIMESTAMPTZ NOT NULL`, named unique constraint
  **`uq_app_user_email`** on `email` (values stored lowercase).
- JPA adapter uses `TransactionTemplate` (no `@Transactional` on adapter
  methods — 002–005 pattern). Map unique-violation by **constraint name**
  → `DuplicateEmailException` (same pattern as 004
  `uq_pokemon_record_national_number`), including concurrent register.
- Seed **one** demo user: `demo@pokefolio.dev`, BCrypt hash generated at
  **implement** time (`htpasswd` / small Java main / Boot test once);
  plaintext demo password documented in README as a **demo credential**
  (not a secret). Suggested plaintext for docs/quickstart:
  `Demo-Passw0rd!` (finalize at implement when hashing).
- Collection seed **reset** helpers used by ITs must **not** truncate or
  reseed `app_user` (demo user and registered test users stay).

**Rationale**: Spec FR-002 / FR-008; Postgres reserved word; 004 race
pattern; auth ITs must be order-independent of collection cleanup.

**Alternatives rejected**:
- Table name `"user"` quoted — fragile across tools; `app_user` is clear.
- Application-level only uniqueness check — races can still **500**.
- Committing a production password as a “secret” — demo credential is
  intentional and labelled.
- Resetting `app_user` with collection fixtures — breaks demo login and
  concurrent-register isolation.

---

## R4 — JWT issue/verify (Nimbus, Clock, config)

**Decision**:
- **web/config** owns `@ConfigurationProperties(prefix =
  "pokemonapp.security.jwt")` with `secret` (String) and `expiry`
  (`Duration`, default `1h`), validated (secret present, UTF-8 length
  ≥ 32). Bound from env `JWT_SECRET` via `application.yml`. Fail fast at
  bean creation if invalid; cover with a context-load failure test.
- **Same config class** builds one `SecretKey`, one
  `NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build()`,
  and one
  `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()`
  (Security 7 builder names — encoder `algorithm`, decoder
  `macAlgorithm`) and exposes them as `@Bean`s.
- Infrastructure `AccessTokenIssuer` injects that `JwtEncoder` (+
  issuer string, expiry, **`Clock`** for production `iat`/`exp`).
- Claims: `iss` (fixed, e.g. `pokefolio`), `sub` (user id as string),
  `iat`, `exp`.
- **Expired-token tests**: mint the compact JWT **directly** with a test
  `JwtEncoder` (or the app’s encoder bean) setting `exp` **≥ 2 minutes
  in the past**. Do **not** replace or advance the global `Clock` bean —
  `NimbusJwtDecoder` default clock skew is **60 s**, so `exp` must be
  older than that skew window.

**Rationale**: Spec FR-004; HS256 needs ≥ 256-bit key; single key
construction; expired ITs must defeat decoder skew without mutating
app-wide time.

**Alternatives rejected**:
- jjwt / auth0 java-jwt — third-party, forbidden.
- RS256 / JWKS — overkill for single-service HS256.
- Altering the global `Clock` bean to mint expired tokens — flaky with
  skew and affects unrelated beans.
- Minting with `exp` only a few seconds in the past — accepted by the
  60 s default skew → false green.

---

## R5 — Security filter chain, public-token ignore, `/error` dispatch

**Decision**:
- `SecurityFilterChain` `@Bean` in `web/config`: session
  `STATELESS`; CSRF disabled; `httpBasic` / `formLogin` disabled;
  `oauth2ResourceServer(jwt)`; authorize with **method + path** matchers
  (same path can be public for GET and protected for POST):
  - **Public** (FR-005): `GET /api/v1/pokemon/**`,
    `GET /api/v1/collection/**`, `POST /api/v1/auth/register`,
    `POST /api/v1/auth/login`, actuator health, springdoc/swagger.
  - **Protected** (authenticated): `POST /api/v1/collection`,
    `PUT|DELETE /api/v1/collection/{nationalNumber}`.
  - `anyRequest().denyAll()`.
- Define the public `RequestMatcher`s **once** (shared constant / bean /
  helper) and reuse that same set in both `authorizeHttpRequests` and the
  custom `BearerTokenResolver` so “public” never drifts between permitAll
  and token-ignore.
- **Custom `BearerTokenResolver`**: if the request matches the shared
  public matchers, return `null`; otherwise delegate to
  `DefaultBearerTokenResolver`. Stale tokens on public **GETs** stay
  **200**; `POST /api/v1/collection` with a valid token must **not** be
  ignored → **201** (or existing 004 business status). Cover with an IT:
  valid Bearer + POST collection → **201**.
- **ERROR dispatch**:
  `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` so MVC
  business statuses are not rewritten to **401**/**403**.
- `AuthenticationEntryPoint` → **401**
  `urn:problem-type:unauthorized` + `WWW-Authenticate: Bearer` +
  `application/problem+json`.
- `AccessDeniedHandler` → **403** `urn:problem-type:forbidden` +
  problem+json. Same ProblemDetail shape as `GlobalExceptionHandler`.

**Rationale**: Spec FR-005 / FR-007; path-only public matchers would
incorrectly treat `POST /api/v1/collection` as public (and ignore its
Bearer). Shared matcher list prevents authorize vs resolver skew.

**Alternatives rejected**:
- Path-only public ant matchers — GET and POST share `/api/v1/collection`.
- Duplicating matcher lists in authorize and resolver — drift risk.
- Global JWT parse on every route — breaks “stale token must not block
  GET”.
- Relying only on `exceptionHandling` without permitting ERROR —
  `/error` hits `denyAll()` → false **401**/**403**.

---

## R6 — Web AuthController and DTOs

**Decision**:
- `AuthController` under `/api/v1/auth`: `POST /register` → **201**;
  `POST /login` → **200**; consumes/produces JSON.
- Request DTOs: `@JsonProperty(required = true)` on all fields.
- **Register** Bean Validation must **mirror domain exactly** so domain
  `IllegalArgumentException` never reaches the catch-all **500**:
  - name: limits from domain constants (`PersonName` min/max).
  - email: custom **`@ValidEmail`** whose validator delegates to
    `Email.isValid(raw)` (normalise trim+lowercase, then max length +
    `PATTERN`). **Do not** put `@Pattern(regexp = Email.PATTERN)` on the
    raw JSON field — `PATTERN` is lowercase-only for the normalised form;
    raw `"Ash@Pokefolio.dev"` / `" ash@… "` would wrongly **400** and
    break “same email, different casing → **409**”.
  - password: min length from domain + custom **`@MaxUtf8Bytes(72)`**
    (BV `@Size` counts **chars**, not UTF-8 bytes — insufficient alone).
  - Tests: register password = 40× `"ñ"` (80 UTF-8 bytes) → **400** naming
    `password`; register `"Ash@Pokefolio.dev"` → **201** with
    `user.email` `ash@pokefolio.dev`; then `"ASH@pokefolio.dev"` →
    **409**; `" ash2@pokefolio.dev "` → **201** normalised. By
    construction, edge acceptance ⇒ domain acceptance (no “BV ok /
    domain throws” path).
- **Login**: `@NotBlank` only on email and password — **no** `@Size` /
  `@MaxUtf8Bytes` / `@ValidEmail` that would **400** a short or oversize
  password. Oversize login is handled in the use case → **401**.
- Response: `{ accessToken, tokenType: "Bearer", expiresIn, user: {
  id, name, email } }`.
- Unknown properties → **400** via existing Jackson fail-on-unknown.
- Advice: `InvalidCredentialsException` → **401** detail
  `Invalid email or password`; `DuplicateEmailException` → **409**
  `urn:problem-type:conflict`.

**Rationale**: Spec FR-002 / FR-003; constitution V; no domain IAE → 500;
case/whitespace-preserving client input must normalise, not fail BV.

**Alternatives rejected**:
- Shared register/login DTO with full password policy — breaks login
  rules.
- Relying on `@Size(max = 72)` for password — `"ñ"×40` is 40 chars /
  80 bytes and would incorrectly pass BV.
- `@Pattern(Email.PATTERN)` on raw JSON — false **400** on mixed-case /
  padded emails; breaks case-insensitive **409**.
- Divergent edge rules vs `Email.isValid` — domain IAE → **500**.

---

## R7 — OpenAPI

**Decision**: Bearer JWT security scheme (`http` / `bearer` / `JWT`);
`@SecurityRequirement` on protected collection write operations;
document **401** / **403** problem+json; extend
`OpenApiDocsIntegrationTest` for auth paths + security scheme + protected
operation security requirements.

**Alternatives rejected**: Opaque API-key scheme; documenting every GET
as secured (they are public).

---

## R8 — ArchUnit

**Decision**: Add rule: `domain` and `application` packages must not
depend on `org.springframework.security..` (in addition to existing
Spring/JPA/HTTP bans). Infrastructure may depend on
`spring-security-crypto` and `spring-security-oauth2-jose` only as needed
for hasher/issuer adapters.

**Alternatives rejected**: Allowing crypto/JWT types in domain “for
convenience”.

---

## R9 — Tests and updating 004/005 write ITs

**Decision**:
- Domain unit tests; use-case tests with mocked ports (dummy-hash path;
  login password &gt; 72 UTF-8 bytes short-circuits without hasher call).
- JPA adapter + Testcontainers (create, duplicate email, concurrent
  register); collection seed reset **does not touch** `app_user`.
- JWT issuer/decoder round-trip; **expired** tokens minted via test
  `JwtEncoder` with `exp` ≥ 2 minutes past (no global `Clock` mutation);
  tampered token → auth failure.
- `@WebMvcTest` security slices; e2e IT matrix including: public GET with
  stale Bearer → **200**; `POST /api/v1/collection` with **valid** token
  → **201** (token not ignored); `GET /api/v1/does-not-exist` →
  **401**/**403**; concurrent register **201**/**409**; register password
  40×`"ñ"` → **400** naming password; register
  `"Ash@Pokefolio.dev"` → **201** normalised, then
  `"ASH@pokefolio.dev"` → **409**, `" ash2@pokefolio.dev "` → **201**
  normalised;
  login password &gt; 72 bytes → **401** generic; no token → **401**.
- Auth ITs use **unique emails** (`UUID`) so they are order-independent
  (normalisation cases above may use fixed locals under a UUID domain).
- Shared test helper: login as demo **or** `jwt()` post-processor; test
  config supplies non-production ≥32-byte `JWT_SECRET`.
- Update existing 004/005 collection **write** ITs and controller tests
  to authenticate; GET tests remain unauthenticated.

**Alternatives rejected**: Hardcoding the compose secret in tests;
altering global `Clock` for expiry; resetting `app_user` with collection
fixtures; fixed emails that collide across ordered IT classes.

---

## R10 — Delivery (Compose, CI, README)

**Decision**:
- `docker-compose.yml`: `JWT_SECRET=${JWT_SECRET:?}` on backend.
- `.env.example`: documented `JWT_SECRET=` placeholder (generate with
  `openssl rand -base64 48`).
- CI docker smoke: generate a throwaway secret in the workflow env
  (never committed).
- README: auth section — register/login curls, demo user + demo
  password label, protected write example with
  `Authorization: Bearer …`.

**Alternatives rejected**: Default secret in `application.yml` — violates
fail-fast and constitution VI secrets rule.

---

## R11 — Constitution VI closure (Complexity Tracking)

**Decision**: Constitution Check **VI = PASS**. Complexity Tracking in
**006 `plan.md` only** records that this feature **supersedes** the
004/005 temporary public-write exception and **narrows** their
“scope records per user” follow-up: collection stays **shared**
(explicit product decision). **Do not edit** merged 004/005 plans.

**Alternatives rejected**: Retro-editing 004/005 plan history; implementing
per-user ownership in 006 (out of scope).
