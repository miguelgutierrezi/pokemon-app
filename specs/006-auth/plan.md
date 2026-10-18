# Implementation Plan: Authentication API (TR-API-AUTH)

**Branch**: `006-auth` | **Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-auth/spec.md`

## Summary

Add **TR-API-AUTH**: user registration and login issuing HS256 JWTs, Spring
Security route policy (public GETs + auth endpoints; protected collection
writes; deny-by-default), Flyway **V5** `app_user` + demo seed, and RFC 7807
authn/authz errors — closing the constitution **VI** public-write exception
from 004/005 while keeping a **single shared** collection (no per-user
ownership). Backend only; UI is 007.

Traceability: TR-API-AUTH, TR-DB (user), TR-ERRORS, TR-UNIT, SUB-SEED /
DEL-SEED. No new HU.

## Technical Context

**Language/Version**: Java 21; Spring Boot **4.1.1** / Spring Security **7**
(re-verify at implement against Maven Central / current docs; compile with
`-Dmaven.compiler.showDeprecation=true`; no deprecated APIs)

**Primary Dependencies** (constitution IX — first use in 006; see research
R1):

| Library | Module | Notes |
|---|---|---|
| `spring-boot-starter-security` | web | Filter chain |
| `spring-boot-starter-security-oauth2-resource-server` | web | **Boot 4.1 name** (old `spring-boot-starter-oauth2-resource-server` is deprecated) |
| `spring-security-crypto` | infrastructure | BCrypt hasher |
| `spring-security-oauth2-jose` | infrastructure | `JwtEncoder` / `NimbusJwtEncoder` for `AccessTokenIssuer` (BOM version) |
| `spring-boot-starter-security-test` | web (test) | Boot 4 test starter |
| Existing stack | BOM 4.1.1 | webmvc, validation, data-jpa, Flyway, springdoc, Testcontainers, ArchUnit |

**JWT wiring**: `@ConfigurationProperties(prefix = "pokemonapp.security.jwt")`
(validated) in **web/config**; **one** place builds `SecretKey` +
`JwtEncoder` + `JwtDecoder` from those properties. Infrastructure issuer
injects the shared `JwtEncoder`.

**Storage**: PostgreSQL; Flyway **V5** `app_user` + `uq_app_user_email`;
demo user seed. Existing V1–V4 unchanged. Collection IT seed reset must
**not** touch `app_user`.

**Testing**: JUnit 5, AssertJ, Mockito; domain + use-case unit tests; JPA
adapter + Testcontainers (incl. concurrent register); JWT round-trip;
expired tokens minted via test `JwtEncoder` (`exp` ≥ 2 min past — **no**
global `Clock` mutation); `@WebMvcTest` security slices; `@SpringBootTest`
e2e IT (UUID emails; POST collection with valid token → 201; UTF-8
password BV; login &gt;72 bytes → 401); update 004/005 write ITs via shared
auth helper. JaCoCo ≥ **80%** per module.

**Target Platform**: Same as 001–005 (Compose + CI).

**Project Type**: Backend API feature in existing monorepo
(`com.miguel.pokemonapp`).

**Performance Goals**: Demo login ≤ **1 s** on local stack (SC-004,
login-only).

**Constraints**:
- Clean Architecture + ArchUnit: domain/application **must not** depend on
  `org.springframework.security..`
- Adapters: `TransactionTemplate` only (no `@Transactional` on adapter
  methods)
- Wiring: `@Bean` in `web/config` (004/005 pattern); JWT key/encoder/decoder
  built once there
- Duplicate email: unique constraint name → `DuplicateEmailException`
  (004 pattern); concurrent register → one success + one conflict
- Public matchers are **method + path**; shared `RequestMatcher` set reused
  by `authorizeHttpRequests` and custom `BearerTokenResolver`
- `/error` dispatcher permitted so business statuses are not rewritten to
  401/403
- Edge BV mirrors domain exactly (`@ValidEmail` → `Email.isValid(raw)`,
  name constants, `@MaxUtf8Bytes(72)`); never raw `@Pattern(Email.PATTERN)`;
  domain IAE must not reach catch-all **500**
- Login password &gt; 72 UTF-8 bytes → `InvalidCredentialsException` without
  hasher call
- Dummy hash computed at startup via `PasswordHasher.hash(...)`
- `JWT_SECRET` required, ≥ 32 bytes, no default; Compose `${JWT_SECRET:?}`
- Collection remains **shared** (no ownership column)
- No CORS in 006 (007 same-origin proxy)

**Scale/Scope**: Auth API + security filter + user table + demo seed;
protect three collection write operations; update existing write tests.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture | Four modules; core free of Spring/JPA/HTTP/Security | PASS — Security types only in infrastructure/web; jose only in infra issuer |
| II. TDD | Red → green → refactor | PASS — see TDD ordering |
| III. Ports & Adapters | User repo / hasher / token issuer ports; adapters in infrastructure | PASS |
| IV. API Contract | `/api/v1`, DTOs, ProblemDetail, springdoc + bearer scheme | PASS |
| V. Two-Layer Validation | BV mirrors domain (`@ValidEmail`→`Email.isValid`, `@MaxUtf8Bytes`, name constants); login edge is `@NotBlank` only | PASS |
| VI. Security | JWT on writes; public reads; BCrypt; secrets from env | **PASS** — this feature implements VI |
| VII. Frontend | Out of scope | N/A |
| VIII. Quality Gates | CI + JaCoCo ≥ 80% per module | PASS |
| IX. Simplicity | Security starters + jose enter here; no speculative ownership | PASS |
| X. Traceability | Spec cites TR-API-AUTH / TR-DB / SUB-SEED; scenarios not labelled HU | PASS |

**Post-design re-check**: PASS — research R1–R11 updated (jose in infra;
shared method+path public matchers; edge/domain parity via
`@ValidEmail`→`Email.isValid` not raw `@Pattern`; login &gt;72-byte
short-circuit; startup dummy hash; expired tokens via test encoder + skew;
UUID auth emails; `app_user` untouched by collection reset). Contracts
cover auth + route policy + env; V5; Complexity Tracking below records
supersession of 004/005 VI exception without editing those plans. No new
constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/006-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── auth-api.yaml
│   ├── route-policy.md
│   └── env-config.md
└── tasks.md             # /speckit-tasks (NOT created by this command)
```

### Source Code (repository root)

```text
backend/
├── domain/src/{main,test}/java/com/miguel/pokemonapp/domain/user/
│   ├── UserAccount.java
│   ├── Email.java                 # PATTERN; isValid(raw); VO trim+lowercase
│   ├── PersonName.java            # min/max length constants; trim 1..100
│   └── RawPassword.java           # register-only policy (8 chars / 72 UTF-8 bytes)
├── application/src/{main,test}/java/com/miguel/pokemonapp/application/
│   ├── port/
│   │   ├── UserAccountRepository.java
│   │   ├── PasswordHasher.java
│   │   └── AccessTokenIssuer.java
│   ├── auth/
│   │   ├── RegisterUserUseCase.java
│   │   ├── AuthenticateUserUseCase.java  # dummy hash; >72-byte short-circuit
│   │   └── AuthSession.java
│   ├── DuplicateEmailException.java
│   └── InvalidCredentialsException.java
├── infrastructure/
│   ├── src/main/java/.../persistence/user/   # JPA + TransactionTemplate
│   ├── src/main/java/.../security/           # BCrypt hasher; JWT issuer (jose)
│   └── src/main/resources/db/migration/
│       └── V5__app_user.sql
├── web/src/{main,test}/java/com/miguel/pokemonapp/web/
│   ├── auth/                 # AuthController + DTOs (@ValidEmail, @MaxUtf8Bytes)
│   ├── config/               # JWT props + SecretKey/Encoder/Decoder;
│   │                         # SecurityFilterChain; shared public matchers
│   ├── error/                # InvalidCredentials / DuplicateEmail advice
│   └── support/              # shared test auth helper (test sources)
└── web/src/test/...          # security slices; AuthIntegrationTest (UUID emails);
                              # update Collection* write ITs to authenticate
```

**Structure Decision**: Extend existing Clean Architecture modules; no new
Maven modules. Package `com.miguel.pokemonapp`. Frontend untouched.

## TDD ordering (visible for tasks)

Work red → green per slice (separate commits at implement time):

1. **Domain** — UserAccount + Email (`PATTERN`, `isValid(raw)`) /
   PersonName constants / RawPassword (UTF-8 byte max); unit tests.
2. **Application** — ports; Register (session) + Authenticate (startup
   dummy hash via hasher; &gt;72-byte login short-circuit without hasher;
   single InvalidCredentials); DuplicateEmailException; Mockito tests.
3. **Infrastructure persistence** — V5 + entity/adapter; create; duplicate
   by constraint name; concurrent register; demo seed; ensure collection
   reset helpers leave `app_user` alone.
4. **Infrastructure crypto/JWT** — BCrypt hasher; issuer using injected
   `JwtEncoder` + Clock for live `iat`/`exp`; round-trip with web beans.
5. **Web JWT config + security + AuthController** — validated JWT props;
   one SecretKey/Encoder/Decoder factory; filter chain with shared
   method+path public matchers; Bearer ignore on public only; ERROR
   permit; entry point / access denied; register BV (`@ValidEmail`,
   `@MaxUtf8Bytes`); OpenAPI bearer scheme.
6. **Advice + ArchUnit** — InvalidCredentials → 401; DuplicateEmail →
   409; security exceptions never 500; ArchUnit ban
   `org.springframework.security..` on domain/application.
7. **E2E IT + migrate 004/005 write tests** — full matrix (UUID emails;
   expired via test encoder; POST collection with token → 201; 40×ñ →
   400; `Ash@…` → 201 normalised then `ASH@…` → 409; padded email →
   201; login &gt;72 bytes → 401); Compose/CI/README/`JWT_SECRET`.

## Complexity Tracking

> Not a gate violation — VI **PASSes** in this feature. This table
> documents supersession of prior temporary exceptions (FR-012): do
> **not** edit merged 004/005 plans.

| Topic | Record |
|-------|--------|
| Constitution **VI** public-write exception (004/005) | **Superseded by 006**: collection **writes** require Bearer JWT; reads stay public. |
| 004/005 follow-up “scope records per user” | **Narrowed (explicit decision)**: local collection remains a **single shared** store in 006; per-user ownership stays out of scope (future feature if ever required). |
| Merged 004/005 `plan.md` | **Untouched** — historical Complexity Tracking rows remain as written at merge time. |
