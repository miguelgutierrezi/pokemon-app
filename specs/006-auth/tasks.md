---
description: "Task list for feature 006-auth (TR-API-AUTH)"
---

# Tasks: Authentication API (TR-API-AUTH)

**Input**: Design documents from `/specs/006-auth/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/,
quickstart.md

**Tests**: Required — constitution II (TDD). Every production change is preceded
by a RED task (failing test for a stated reason), then a GREEN task — except
where noted (ArchUnit rule-as-test; acceptance IT that is already green).
Dependencies are added inside the first RED (or GREEN, when noted) that needs
them — constitution IX / no standalone dependency-only phase.

**Organization**: Grouped by plan.md TDD layers. Labels are **`[AUTH]`**
only — never `[USn]` (constitution X; scenarios are TR-API-AUTH, not HUs).

**Conventions (same as 004/005)**: sequential RED→GREEN pairs; ⏸ Review +
Conventional Commit message per task; no RED pushes; R6 module coverage
stated per task; `git status` before each task. **The agent never commits**
— each task ends with stop for review/commit; the user makes all commits.

## Format: `[ID] [P?] [Label] Description`

- **[P]**: Can run in parallel (different files, no dependencies) — rare here
- **[AUTH]**: feature label
- **⏸ Review**: stop after the task; the user reviews and makes the commit.
  Suggested commit message given; agent never commits or pushes. RED states
  are not pushed.

## Path Conventions

- Backend: `backend/<module>/src/{main,test}/java/com/miguel/pokemonapp/...`
- Root: `README.md`, `docker-compose.yml`, `.env.example`
- Local (uncommitted): `.env` — never commit secrets

## Global rules for every task

1. **Before starting**: run `git status`. If the previous task’s reviewed
   changes are still uncommitted, **STOP** and ask the user to commit first.
2. Add only dependencies listed in plan.md / research.md, and only inside the
   task that first needs them:
   - `spring-security-crypto` → infrastructure (hasher RED/GREEN)
   - `spring-security-oauth2-jose` → infrastructure (issuer RED/GREEN)
   - `spring-boot-starter-security`,
     `spring-boot-starter-security-oauth2-resource-server`,
     `spring-boot-starter-security-test` → web (filter-chain GREEN that also
     migrates write ITs **and every `@WebMvcTest`** — keep CI green)
3. Before marking a task `[x]`: `cd backend && ./mvnw -B verify` must pass
   (with `-Dmaven.compiler.showDeprecation=true`), except RED tasks, where
   the named test MUST fail for the stated reason. Prefer Boot **4.1.1** /
   Security **7** APIs; no deprecated artifact ids or APIs.
4. **R6**: each module’s tests cover that module’s code. Spring wiring under
   **`web/config/`**. Base package `com.miguel.pokemonapp`.
5. Adapters: `TransactionTemplate` only (no `@Transactional` on adapter
   methods). Duplicate email via constraint name `uq_app_user_email`.
6. Once the draft PR exists (T003), every **push** must keep CI green (no
   RED pushes).
7. **Forbidden in 006**: third-party JWT libs; per-user collection ownership;
   refresh tokens; CORS; frontend auth UI; editing merged 004/005 plans;
   hardcoded production `JWT_SECRET` default; `@Pattern(Email.PATTERN)` on
   raw register JSON; mutating the global `Clock` bean for expired-token
   tests; resetting `app_user` in collection seed helpers; creating
   `backend/web/src/test/resources/application.yml` (would shadow main
   `application.yml`).

---

## Phase 1: Domain — Email / PersonName / RawPassword / UserAccount

**Goal**: Domain VOs + `UserAccount`; `Email.isValid(raw)` normalises then
checks max length + `PATTERN`; register password 8 chars / 72 UTF-8 bytes.

**Independent Test**: `./mvnw -B -pl domain test` green; no Spring on domain
classpath.

- [x] T001 [AUTH] RED: write
  `backend/domain/src/test/java/com/miguel/pokemonapp/domain/user/EmailTest.java`,
  `PersonNameTest.java`, `RawPasswordTest.java`, `UserAccountTest.java` —
  `Email.isValid` / VO: trim+lowercase; max **254**; concrete
  `Email.PATTERN` from data-model.md; accept `"Ash@Pokefolio.dev"` /
  `" ash@pokefolio.dev "` as valid raw inputs that normalise to
  `ash@pokefolio.dev`; reject blank/invalid. `PersonName`: trim,
  `MIN_LENGTH=1`, `MAX_LENGTH=100`. `RawPassword`: min **8** chars; max
  **72** UTF-8 bytes; `"ñ"×40` (80 bytes) rejected. `UserAccount` factory
  wires validated fields + opaque hash (no BCrypt types). Confirm fail
  (types missing). Do not push RED. Coverage: **domain**.
  ⏸ Review — `test(domain): expect user account email name password vos (red)`

- [x] T002 [AUTH] GREEN: implement
  `backend/domain/src/main/java/com/miguel/pokemonapp/domain/user/Email.java`
  (`PATTERN`, `MAX_LENGTH`, `isValid(String raw)`, VO),
  `PersonName.java`, `RawPassword.java`, `UserAccount.java`. Domain tests
  pass. Coverage: **domain**.
  ⏸ Review — `feat(domain): user account email name password vos (green)`

- [x] T003 **User action**: push `006-auth` and open a **draft** PR to
  `main` (`gh pr create --draft --base main`). Confirm CI green on the
  current tip. If red, fix before continuing. ⏸ Review

**Checkpoint**: Domain green; draft PR open.

---

## Phase 2: Application — ports + register/login use cases

**Goal**: `RegisterUserUseCase` → `AuthSession`; `AuthenticateUserUseCase`
with startup dummy hash, &gt;72-byte short-circuit (no hasher call), single
`InvalidCredentialsException`; `DuplicateEmailException`.

**Independent Test**: `./mvnw -B -pl application test` with Mockito; no Spring.

- [x] T004 [AUTH] RED: write
  `backend/application/src/test/java/com/miguel/pokemonapp/application/auth/RegisterUserUseCaseTest.java`
  and
  `AuthenticateUserUseCaseTest.java` —
  Register: happy path hashes + saves + issues token → `AuthSession`;
  repo duplicate → `DuplicateEmailException`. Authenticate: normalises
  email; unknown email → `matches(raw, dummyHash)` then
  `InvalidCredentialsException`; wrong password → same exception; success
  → session. **Password UTF-8 length &gt; 72**: throws
  `InvalidCredentialsException` and **`PasswordHasher` is never
  interacted with** (verifyNoInteractions / zero `matches`/`hash`).
  Confirm fail (types missing). Do not push RED. Coverage: **application**.
  ⏸ Review — `test(application): expect register and authenticate user (red)`

- [x] T005 [AUTH] GREEN: add ports
  `backend/application/src/main/java/com/miguel/pokemonapp/application/port/UserAccountRepository.java`,
  `PasswordHasher.java`,
  `AccessTokenIssuer.java`;
  `application/auth/RegisterUserUseCase.java`,
  `AuthenticateUserUseCase.java`,
  `AuthSession.java`;
  `DuplicateEmailException.java`,
  `InvalidCredentialsException.java`.
  Authenticate takes a **dummy hash string** supplied by the caller
  (computed at startup later via `PasswordHasher.hash` — not hardcoded in
  the use case). Application tests pass. Coverage: **application**.
  ⏸ Review — `feat(application): register and authenticate user use cases (green)`

**Checkpoint**: Application green under Mockito.

---

## Phase 3: Persistence — Flyway V5 `app_user` + JPA adapter

**Goal**: Table `app_user`, `uq_app_user_email`, demo seed row with a
**reproducible** BCrypt hash; `UserAccountJpaAdapter` with
`TransactionTemplate`; duplicate + concurrent register by constraint name;
collection seed reset never touches `app_user`.

**Independent Test**: infrastructure Testcontainers tests green.

- [x] T006 [AUTH] RED: write
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/persistence/user/UserAccountJpaAdapterTest.java`
  — create persists normalised email; second create same email (any casing)
  → `DuplicateEmailException` (constraint **`uq_app_user_email`**); concurrent
  register latch → exactly one success + one `DuplicateEmailException`
  (never other exception); after migrate, demo email
  `demo@pokefolio.dev` is readable (seeded hash present). Confirm fail.
  Do not push RED. Coverage: **infrastructure**.
  ⏸ Review — `test(infrastructure): expect app_user jpa adapter (red)`

- [x] T007 [AUTH] GREEN: add
  `backend/infrastructure/src/main/resources/db/migration/V5__app_user.sql`
  (`app_user` columns per data-model; `uq_app_user_email`; seed demo user
  `demo@pokefolio.dev`). Generate the BCrypt hash **reproducibly**, e.g.:

  ```bash
  htpasswd -bnBC 10 "" 'Demo-Passw0rd!' | tr -d ':\n'
  ```

  Embed that hash in V5 (document the command in a SQL comment). JPA entity
  + `UserAccountJpaAdapter` (`TransactionTemplate`; map unique violation by
  constraint name). Assert / document that
  `CollectionSeedReset.java` does **not** truncate `app_user`. Adapter
  tests pass. **Seed hash ↔ demo password verification**: after
  `BCryptPasswordHasher` exists (**T009**), extend this adapter test (or
  rely on acceptance IT demo login in **T023**) so
  `BCryptPasswordHasher.matches("Demo-Passw0rd!", seededHash)` is **true**
  — do not leave an unverified hash in V5. Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): app_user flyway v5 and jpa adapter (green)`

**Checkpoint**: Persistence green; demo seed present with reproducible hash.

---

## Phase 4: Crypto + JWT issuer (infrastructure)

**Goal**: BCrypt `PasswordHasher`; `AccessTokenIssuer` via injected
`JwtEncoder` (jose); round-trip / claims; Clock for live `iat`/`exp` only.

**Independent Test**: infrastructure unit tests (encoder built in-test).

- [x] T008 [AUTH] RED: add dependency
  `org.springframework.security:spring-security-crypto` (BOM, no version)
  to `backend/infrastructure/pom.xml`. Write
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/security/BCryptPasswordHasherTest.java`
  — `hash` then `matches` true; wrong password false; hashes for same
  plaintext differ (salt). Confirm fail. Do not push RED. Coverage:
  **infrastructure**.
  ⏸ Review — `test(infrastructure): expect bcrypt password hasher (red)`

- [x] T009 [AUTH] GREEN: implement
  `backend/infrastructure/src/main/java/com/miguel/pokemonapp/infrastructure/security/BCryptPasswordHasher.java`
  implementing `PasswordHasher`. Hasher unit tests pass. **Also** extend
  `UserAccountJpaAdapterTest` to load the V5 demo row and assert
  `matches("Demo-Passw0rd!", seededHash)` (covers T007 hash verification
  once the hasher exists). Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): bcrypt password hasher (green)`

- [x] T010 [AUTH] RED: add dependency
  `org.springframework.security:spring-security-oauth2-jose` (BOM) to
  `backend/infrastructure/pom.xml`. Write
  `backend/infrastructure/src/test/java/com/miguel/pokemonapp/infrastructure/security/JwtAccessTokenIssuerTest.java`
  — build `NimbusJwtEncoder`/`NimbusJwtDecoder` in-test with HS256 test
  key; issue token with claims `iss`, `sub` (user id), `iat`, `exp`;
  decoder round-trip; issuer uses injected `Clock` for `iat`/`exp` of
  **live** tokens. Confirm fail. Do not push RED. Coverage:
  **infrastructure**.
  ⏸ Review — `test(infrastructure): expect jwt access token issuer (red)`

- [x] T011 [AUTH] GREEN: implement
  `backend/infrastructure/src/main/java/com/miguel/pokemonapp/infrastructure/security/JwtAccessTokenIssuer.java`
  implementing `AccessTokenIssuer` (inject `JwtEncoder`, issuer string,
  expiry `Duration`, `Clock`). Tests pass. Coverage: **infrastructure**.
  ⏸ Review — `feat(infrastructure): jwt access token issuer (green)`

**Checkpoint**: Hasher + issuer green without web filter chain; demo hash
verified against `Demo-Passw0rd!`.

---

## Phase 5: Web JWT properties + shared encoder/decoder beans

**Goal**: Validated `@ConfigurationProperties(prefix =
"pokemonapp.security.jwt")`; one config builds `SecretKey` + `JwtEncoder` +
`JwtDecoder`; fail fast if secret missing or &lt; 32 UTF-8 bytes; test
secret via Maven surefire/failsafe **system properties** (not a test
`application.yml`).

**Independent Test**: `ApplicationContextRunner` fail-fast + happy path;
full verify with system props.

- [x] T012 [AUTH] RED: write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/config/JwtPropertiesValidationTest.java`
  using **`ApplicationContextRunner`** with **explicit** properties —
  missing secret → context fails; secret UTF-8 length &lt; 32 → fails; valid
  ≥32-byte secret → starts. Confirm fail (config missing). Do not push RED.
  Coverage: **web**.
  ⏸ Review — `test(web): expect jwt secret fail-fast (red)`

- [x] T013 [AUTH] GREEN: add
  `backend/web/src/main/java/com/miguel/pokemonapp/web/config/JwtProperties.java`
  (validated) and config class that binds
  `pokemonapp.security.jwt.secret: ${JWT_SECRET}` /
  `expiry` default `1h` in
  `backend/web/src/main/resources/application.yml`, builds **one**
  `SecretKey` + `NimbusJwtEncoder` + `NimbusJwtDecoder` (Security 7:
  encoder `algorithm`, decoder `macAlgorithm`), exposes `@Bean`s; wire
  `JwtAccessTokenIssuer` + `BCryptPasswordHasher` + **startup dummy hash**
  bean (`PasswordHasher.hash(...)` once) + use-case `@Bean`s under
  `web/config/`. In `backend/web/pom.xml`, set
  `pokemonapp.security.jwt.secret` (non-production ≥32-byte value) via
  **both** `maven-surefire-plugin` and `maven-failsafe-plugin`
  `systemPropertyVariables` — **do not** create
  `backend/web/src/test/resources/application.yml` (shadows main).
  `ApplicationContextRunner` tests pass; full verify green (**no** Security
  filter chain yet — do not add security starters here). Coverage: **web**.
  ⏸ Review — `feat(web): jwt properties encoder decoder and auth beans (green)`

**Checkpoint**: JWT beans + dummy hash wiring green; collection writes still
public (004/005 ITs unchanged and green).

---

## Phase 6: Advice — InvalidCredentials / DuplicateEmail (before AuthController)

**Goal**: Map auth application exceptions to RFC 7807 **before** any
register/login HTTP path can return catch-all **500** on bad credentials.
Must land before AuthController is pushed.

**Independent Test**: `GlobalExceptionHandlerTest` green.

- [ ] T014 [AUTH] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandlerTest.java`
  — `InvalidCredentialsException` → **401**
  `urn:problem-type:unauthorized`, detail exactly
  `Invalid email or password`; `DuplicateEmailException` → **409**
  `urn:problem-type:conflict`; bodies have no stack / framework package
  leaks. Confirm fail. Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect auth problem advice (red)`

- [ ] T015 [AUTH] GREEN: extend
  `backend/web/src/main/java/com/miguel/pokemonapp/web/error/GlobalExceptionHandler.java`
  for those exceptions. Advice tests pass. Coverage: **web**.
  ⏸ Review — `feat(web): auth conflict and unauthorized advice (green)`

**Checkpoint**: Bad credentials / duplicate email never fall through to 500
advice once AuthController exists.

---

## Phase 7: AuthController + Bean Validation (before filter chain)

**Goal**: `POST /api/v1/auth/register` (**201**) and `/login` (**200**);
`@ValidEmail` → `Email.isValid`; `@MaxUtf8Bytes(72)`; login `@NotBlank`
only.

**Independent Test**: `@WebMvcTest(AuthController)` (security starters not
on classpath yet).

- [ ] T016 [AUTH] RED: write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/auth/AuthControllerTest.java`
  — register happy → **201** session shape; password `"ñ"×40` → **400**
  naming `password`; register `"Ash@Pokefolio.dev"` → **201** with
  `user.email` `ash@pokefolio.dev` (mocked use case returns normalised
  email); login `@NotBlank` only (short password reaches use case, not
  BV **400** for length); unknown JSON property → **400**. Confirm fail.
  Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect auth controller register login (red)`

- [ ] T017 [AUTH] GREEN: implement
  `backend/web/src/main/java/com/miguel/pokemonapp/web/auth/AuthController.java`,
  register/login request DTOs + response DTO,
  `@ValidEmail` (+ validator delegating to `Email.isValid`),
  `@MaxUtf8Bytes` (+ validator); `@JsonProperty(required = true)`.
  Controller tests pass; full verify still green without locking writes.
  Coverage: **web**.
  ⏸ Review — `feat(web): auth register login controller and validation (green)`

**Checkpoint**: Auth HTTP API green; writes still open; advice already maps
401/409.

---

## Phase 8: Security filter chain + migrate ITs and every `@WebMvcTest`

**Goal**: Method+path shared public matchers; Bearer ignore on public only;
ERROR dispatch permitted; entry point / access denied problem+json; protect
collection writes. **In the same GREEN task**, migrate collection write ITs
**and every `@WebMvcTest`** so CI never goes red once the security starter
is on the classpath.

**Independent Test**: security slices + full `./mvnw -B verify` green after
GREEN.

- [ ] T018 [AUTH] RED: write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/security/SecurityRoutePolicyTest.java`
  (and/or extend `@WebMvcTest` / `@SpringBootTest` slices) expecting:
  GET catalog/collection with stale/malformed Bearer → **200**;
  `POST /api/v1/collection` **without** token → **401** +
  `WWW-Authenticate: Bearer` + `urn:problem-type:unauthorized` problem+json
  (no stack / `tools.jackson` / `com.fasterxml` leaks);
  `POST /api/v1/collection` **with** valid JWT → proceeds (**201** or
  existing business status — token **not** ignored);
  `GET /api/v1/does-not-exist` → **401** without token, **403**
  `urn:problem-type:forbidden` with valid token;
  `GET /api/v1/pokemon/99999` (or equivalent not-found catalog path) →
  **404** problem+json **not** rewritten to **401** (ERROR dispatch).
  Confirm fail (no filter chain). Do not push RED. Coverage: **web**.
  ⏸ Review — `test(web): expect security route policy and problem json (red)`

- [ ] T019 [AUTH] GREEN: **single commit keeps CI green** — in
  `backend/web/pom.xml` add
  `spring-boot-starter-security`,
  `spring-boot-starter-security-oauth2-resource-server` (Boot 4.1 name),
  `spring-boot-starter-security-test` (test). Implement
  `backend/web/src/main/java/com/miguel/pokemonapp/web/config/SecurityConfiguration.java`
  (or equivalent): shared public `RequestMatcher`s (METHOD+PATH) reused by
  `authorizeHttpRequests` and custom `BearerTokenResolver`;
  `DispatcherType.ERROR` permitAll; entry point **401** +
  `WWW-Authenticate: Bearer`; access denied **403**; both write
  `application/problem+json` same shape as advice; session STATELESS;
  CSRF/httpBasic/formLogin off; `oauth2ResourceServer(jwt)`.
  Add test helper / shared meta-annotation under
  `backend/web/src/test/java/com/miguel/pokemonapp/web/support/`
  (`AuthTestSupport`, and e.g. `@ImportSecurityTestConfig` that
  `@Import`s `SecurityConfiguration` + JWT property/encoder/decoder beans).
  **Why**: `@WebMvcTest` slices do **not** pick up
  `@Configuration`/`@Bean` `SecurityConfiguration` automatically — Boot’s
  default security would make GETs **401** and POSTs **403** (CSRF).
  **Migrate in this same task** so verify stays green:
  - ITs: `CollectionIntegrationTest.java`,
    `CollectionUpdateIntegrationTest.java` — authenticate POST/PUT/DELETE;
    GETs stay anonymous.
  - **Every `@WebMvcTest`**: `PokemonControllerTest.java`,
    `CollectionControllerTest.java`, `GlobalExceptionHandlerTest.java`,
    `AuthControllerTest.java` — each slice imports
    `SecurityConfiguration` + JWT beans (via `@Import` or the shared
    meta-annotation). Writes authenticate (`jwt()` / helper); GETs stay
    anonymous. Security route tests pass; full `./mvnw -B verify` green.
  Coverage: **web**.
  ⏸ Review — `feat(web): security filter chain and authenticate collection writes (green)`

**Checkpoint**: Writes protected; all ITs and `@WebMvcTest` slices green
with security on the classpath.

---

## Phase 9: ArchUnit + OpenAPI

**Goal**: ArchUnit bans `org.springframework.security..` on
domain/application; OpenAPI bearer scheme + `@SecurityRequirement` on
protected ops.

**Independent Test**: ArchUnit + OpenApiDocs IT.

- [ ] T020 [AUTH] Extend
  `backend/web/src/test/java/com/miguel/pokemonapp/architecture/ArchitectureTest.java`
  with a rule that domain and application must not depend on
  `org.springframework.security..` (alongside existing Spring/JPA/HTTP
  bans). Full verify green. Coverage: **web** (ArchUnit).
  ⏸ Review — `test(web): archunit ban spring security on domain application`

- [ ] T021 [AUTH] RED: extend
  `backend/web/src/test/java/com/miguel/pokemonapp/web/OpenApiDocsIntegrationTest.java`
  — document `bearerAuth` (http/bearer/JWT); protected collection write
  operations declare security requirement; auth paths present; 401/403
  documented on protected ops. Confirm fail. Do not push RED. Coverage:
  **web**.
  ⏸ Review — `test(web): expect openapi bearer security scheme (red)`

- [ ] T022 [AUTH] GREEN: update
  `backend/web/src/main/java/com/miguel/pokemonapp/web/config/OpenApiConfiguration.java`
  and collection/auth controller annotations (`@SecurityRequirement`,
  `@SecurityScheme` as needed). OpenAPI IT passes. Coverage: **web**.
  ⏸ Review — `feat(web): openapi bearer scheme and protected operation security (green)`

**Checkpoint**: ArchUnit + OpenAPI green.

---

## Phase 10: Auth end-to-end IT (full spec matrix)

**Goal**: `@SpringBootTest` coverage for every remaining spec scenario with
UUID emails; expired token via test `JwtEncoder` (`exp` ≥ 2 minutes past —
**do not** alter global `Clock`); demo login; normalisation / concurrent /
login &gt;72 bytes.

**Independent Test**: Auth IT class green on Testcontainers Postgres.

- [ ] T023 [AUTH] Write
  `backend/web/src/test/java/com/miguel/pokemonapp/web/auth/AuthIntegrationTest.java`
  (unique emails via `UUID`) covering:
  register → token → protected write succeeds; no token write → **401**;
  register `"Ash@…@uuid.example"` → **201** normalised email; then
  `"ASH@…"` same local → **409**; padded email → **201** normalised;
  register `"ñ"×40` password → **400** naming password; login password
  &gt;72 UTF-8 bytes → **401** generic detail; login unknown email →
  **401** same detail; demo `demo@pokefolio.dev` / `Demo-Passw0rd!` →
  **200** usable token; expired Bearer on protected write → **401**
  (mint with test `JwtEncoder`, `exp` ≥ 2 minutes past); tampered Bearer
  on write → **401**; public GET with stale Bearer → **200**;
  `CollectionSeedReset.restore` leaves `app_user` / demo intact;
  concurrent register → one **201** + one **409**.
  **Commit rule**: if the matrix is green on first run (expected after
  T001–T022), make **one** commit —
  `test(web): auth acceptance test matrix with postgres` — **no fake RED**.
  Only a genuinely failing case gets its own RED/GREEN pair (add a follow-up
  task then; do not invent a failing state). Coverage: **web** IT.
  ⏸ Review — `test(web): auth acceptance test matrix with postgres`

**Checkpoint**: Spec matrix covered by automated IT.

---

## Phase 11: Delivery — Compose, local `.env`, README, quickstart

**Goal**: `${JWT_SECRET:?}` in compose; `.env.example`; local `.env` user
action; README auth section; live quickstart validation.
**CI note**: CI does **not** start the backend (frontend smoke only) — **no
CI throwaway-secret step**; no `ci.yml` change required for `JWT_SECRET`.

- [ ] T024 [AUTH] Update `docker-compose.yml` backend env with
  `JWT_SECRET: ${JWT_SECRET:?}` (fail if unset). Update `.env.example`
  with `JWT_SECRET=` placeholder and `openssl rand -base64 48` hint.
  **No CI workflow change** (backend is not started in CI).
  **User action** (same review stop): add `JWT_SECRET` to the local,
  **uncommitted** `.env` before any `docker compose up`:

  ```bash
  # append to .env (do not commit)
  echo "JWT_SECRET=$(openssl rand -base64 48)" >> .env
  ```

  Coverage: delivery.
  ⏸ Review — `build: require JWT_SECRET in compose and env example`

- [ ] T025 [AUTH] Update root `README.md` — auth section: register/login
  curls, demo user `demo@pokefolio.dev` + plaintext demo password labelled
  **demo credential** (not a production secret), protected write example
  with `Authorization: Bearer …`, `JWT_SECRET` requirement, note that
  collection writes require JWT (closes public-write exception). Run new
  README curls against a local stack before marking done (or record that
  T026 covers them). Coverage: docs.
  ⏸ Review — `docs: document auth api and demo credentials in readme`

- [ ] T026 [AUTH] Run `specs/006-auth/quickstart.md` end to end against the
  real stack (`docker compose` with `JWT_SECRET` in local `.env`): login
  demo → import → PUT → DELETE; no-token **401**; stale Bearer on GET →
  **200**; optional deny-by-default / register checks. Record SC-004 (demo
  login ≤ 1 s) as manual timing in quickstart if measured. Fix deviations
  before done. Coverage: validation run.
  ⏸ Review — `test: validate 006 auth quickstart`

**Checkpoint**: Feature deliverable; ready for PR mark-ready.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Domain (1)** → **Application (2)** → **Persistence (3)** →
  **Crypto/JWT issuer (4)** → **Web JWT beans (5)** →
  **Advice (6)** → **AuthController (7)** →
  **Security filter + migrate slices/ITs (8)** →
  **ArchUnit / OpenAPI (9)** → **Auth e2e IT (10)** →
  **Delivery (11)**
- **T003** (draft PR) after first GREEN (**T002**)
- **T014/T015** (advice) **before** AuthController (**T016/T017**) so no
  pushed state returns **500** on bad credentials / duplicate email
- **T019** MUST migrate collection write ITs **and every `@WebMvcTest`**
  in the **same** GREEN as the filter chain (no intervening commit that
  leaves CI red)
- Demo hash verification: reproducible in **T007**;
  `matches(Demo-Passw0rd!, hash)` in **T009** (adapter test) and/or
  demo login in **T023**

### Within phases

- Strictly sequential RED→GREEN pairs (no batch of REDs), except T020 /
  T023 when already green
- RED states reviewed locally; not pushed
- One task → one review stop; user commits
- `git status` clean of prior uncommitted reviewed work before starting
  the next task

### Parallel Opportunities

- Limited: RED→GREEN pairs are sequential by design
- No meaningful multi-agent parallelism intended

---

## Implementation Strategy

### MVP ([AUTH] register + login + protected writes)

1. Phases 1–8 (domain → application → persistence → crypto → JWT beans →
   advice → AuthController → security + migrate slices/ITs).
2. ArchUnit + OpenAPI + Auth IT (Phases 9–10).
3. Compose / local `.env` / README / quickstart (Phase 11).

### Full 006

1. Keep draft PR CI green after T003; especially after T019.
2. T026 quickstart (or IT equivalents) green.
3. 002–005 tests remain green throughout (writes + slices authenticated
   from T019).
4. Constitution VI = PASS; Complexity Tracking already in plan.md.

---

## Notes

- **26 tasks** (T001–T026); every one ends in a review stop
- Labels **`[AUTH]`** only — never `[USn]`
- Suggested commit messages follow Conventional Commits; the user commits
- Agent never commits or pushes; do not push RED / no fake RED
- Email edge: `@ValidEmail` → `Email.isValid(raw)` — never raw
  `@Pattern(Email.PATTERN)`
- Expired tokens: test `JwtEncoder` + `exp` ≥ 2 minutes past (60 s decoder
  skew); do not mutate global `Clock`
- Dummy hash: `PasswordHasher.hash(...)` once at startup (T013)
- Test JWT secret: surefire **and** failsafe `systemPropertyVariables` in
  `backend/web/pom.xml` (T013) — never `src/test/resources/application.yml`
- Login &gt;72 UTF-8 bytes: use-case short-circuit (T004/T005) + IT (T023)
- Concurrent register: constraint name (T006/T007) + HTTP 201/409 (T023)
- Shared public matchers: METHOD+PATH (T018/T019)
- `@WebMvcTest` + security: import `SecurityConfiguration` + JWT beans
  (T019) — Pokemon / Collection / GlobalExceptionHandler / AuthController
- ArchUnit Security ban: T020 (`test(web): …`, not labelled GREEN)
- Demo hash: `htpasswd -bnBC 10 "" 'Demo-Passw0rd!' | tr -d ':\n'` (T007);
  verify with hasher in T009
- CI: no backend smoke → no throwaway `JWT_SECRET` in `ci.yml` (T024)
