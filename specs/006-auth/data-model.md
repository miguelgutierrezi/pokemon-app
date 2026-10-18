# Data Model: Authentication API (TR-API-AUTH)

**Feature**: `006-auth` | **Date**: 2026-10-17

HTTP: [contracts/auth-api.yaml](./contracts/auth-api.yaml). Route policy:
[contracts/route-policy.md](./contracts/route-policy.md). Env:
[contracts/env-config.md](./contracts/env-config.md).

Local Pokémon collection ownership model is **unchanged** (single shared
store). This feature adds the **user account** aggregate and session token
shapes only.

## Domain

### Email

| Constant / rule | Value / behaviour |
|-----------------|-------------------|
| `Email.MAX_LENGTH` | `254` (after trim) |
| `Email.PATTERN` | See concrete regex below — applied **only** to the normalised form |
| `Email.isValid(String raw)` | Domain static predicate: normalise (`trim` + lowercase), then max length + `PATTERN`; **single source of truth** for VO construction and register edge |
| Normalisation | `trim` + lowercase **before** matching `PATTERN` / storage |
| Blank | Rejected (`isValid` → false) |

**Concrete `Email.PATTERN`** (Java string; applied only to the normalised
form — lowercase, no leading/trailing space):

```text
^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$
```

Notes:
- Requires a local part, `@`, and a domain with at least one dot (TLD).
- Labels are DNS-like (max 63 chars, no leading/trailing hyphen).
- `PATTERN` is lowercase-only by design — **never** apply
  `@Pattern(regexp = Email.PATTERN)` to the raw JSON value
  (`"Ash@Pokefolio.dev"` / `" ash@pokefolio.dev "` would wrongly **400**
  and break case-insensitive duplicate → **409**).
- Register edge uses custom **`@ValidEmail`** whose validator delegates to
  `Email.isValid(raw)` (same normalisation + rules as the VO). By
  construction, edge acceptance ⇒ domain acceptance (no BV-ok /
  domain-IAE path → no catch-all **500**).

Used on register **and** login (login normalises before lookup; login
edge does **not** apply `@ValidEmail` — only `@NotBlank`).

### PersonName (display name)

| Constant / rule | Value / behaviour |
|-----------------|-------------------|
| `PersonName.MIN_LENGTH` | `1` (after trim) |
| `PersonName.MAX_LENGTH` | `100` (after trim) |
| Normalisation | `trim`; blank after trim rejected |
| Scope | **Register only** (login has no name field) |

Register BV uses the same min/max constants (not hard-coded literals that
can drift).

### RawPassword

| Constant / rule | Value / behaviour |
|-----------------|-------------------|
| `RawPassword.MIN_LENGTH` | `8` **characters** |
| `RawPassword.MAX_UTF8_BYTES` | `72` **UTF-8 bytes** (BCrypt limit) |
| Scope | **Register only** — login MUST NOT apply this policy |

Register BV: `@Size(min = RawPassword.MIN_LENGTH)` (or equivalent) **plus**
custom **`@MaxUtf8Bytes(72)`** — `@Size(max = 72)` is **not** sufficient
(character count ≠ byte count). Example: password `"ñ"×40` is 40 chars /
**80** UTF-8 bytes → register **400** naming `password`.

Stored form on the account is an opaque `passwordHash` string; domain never
depends on BCrypt / Spring Security types.

### UserAccount

| Field | Type | Notes |
|-------|------|-------|
| `id` | long (surrogate) | Assigned on persist; used as JWT `sub` |
| `name` | PersonName | Display name |
| `email` | Email | Unique case-insensitively; stored normalised |
| `passwordHash` | String | Opaque one-way hash; never in API responses |
| `createdAt` | Instant | Set on create |

**Factories / behaviour** (illustrative):

| Method | Behaviour |
|--------|-----------|
| `register(name, email, rawPassword)` / factory | Validates VOs; does **not** hash (hasher is a port) |
| Accessors | Expose id/name/email/hash/createdAt; no password plaintext |

No roles beyond “authenticated user”.

## Edge ↔ domain parity (register)

| Field | Domain | Register BV |
|-------|--------|-------------|
| name | `PersonName.MIN_LENGTH` / `MAX_LENGTH` after trim | Same constants |
| email | `Email.isValid(raw)` → normalise then `MAX_LENGTH` + `PATTERN` | `@ValidEmail` → delegates to `Email.isValid(raw)` (**not** raw `@Pattern`) |
| password | min 8 chars; max 72 UTF-8 bytes | min length + `@MaxUtf8Bytes(72)` |

Goal: domain `IllegalArgumentException` for these rules never reaches the
catch-all **500**.

**Email normalisation IT cases** (register):

| Input email | Expected |
|-------------|----------|
| `Ash@Pokefolio.dev` | **201**; stored/returned email `ash@pokefolio.dev` |
| `ASH@pokefolio.dev` (after prior success) | **409** `urn:problem-type:conflict` |
| ` ash2@pokefolio.dev ` | **201**; stored/returned email `ash2@pokefolio.dev` |

## Application

### Ports

| Port | Responsibility |
|------|----------------|
| `UserAccountRepository` | `save` / `findByEmail` (normalised); create maps unique-violation → `DuplicateEmailException` |
| `PasswordHasher` | `hash(raw)` → hash; `matches(raw, hash)` → boolean |
| `AccessTokenIssuer` | `issue(userId)` → token string + `expiresIn` seconds (uses injected `Clock` for live tokens) |

**Dummy hash** (unknown-email timing): computed **once at startup** by
calling `PasswordHasher.hash(...)` with a fixed non-secret placeholder
string (same BCrypt cost as real hashes). Stored for the lifetime of the
process — **not** a hardcoded BCrypt literal in source.

### Commands / results

| Type | Fields |
|------|--------|
| Register command | name, email, password (raw) |
| Login command | email, password (raw) — non-blank only at web; use case normalises email |
| `AuthSession` | `accessToken`, `tokenType` (`"Bearer"`), `expiresIn` (seconds), `user { id, name, email }` |

### Use-case flows

**Register**

```text
validate domain VOs (name, email, RawPassword policy)
hash password
save UserAccount
  unique email violation (uq_app_user_email) → DuplicateEmailException
issue access token (Clock)
return AuthSession
```

**Authenticate (login)**

```text
normalise email (trim + lowercase)
if password UTF-8 byte length > 72
  → throw InvalidCredentialsException   # no hasher call (BCrypt would reject)
findByEmail
  absent → matches(raw, startupDummyHash); throw InvalidCredentialsException
  present → !matches(raw, hash) → InvalidCredentialsException
issue access token
return AuthSession
```

No password-policy validation on login (short passwords → **401**, not
**400**). Single `InvalidCredentialsException` for all credential
failures. Cover &gt;72-byte login with unit test (hasher never called) + IT
(**401** generic).

### Exceptions

| Type | HTTP | `type` | Detail |
|------|------|--------|--------|
| `DuplicateEmailException` | 409 | `urn:problem-type:conflict` | Email already registered (wording distinct from Pokémon duplicate if needed; same URN family) |
| `InvalidCredentialsException` | 401 | `urn:problem-type:unauthorized` | Exactly `Invalid email or password` |
| Security entry point (no/invalid token on protected) | 401 | `urn:problem-type:unauthorized` | + `WWW-Authenticate: Bearer` |
| Access denied (authenticated, denyAll path) | 403 | `urn:problem-type:forbidden` | ProblemDetail shape as advice |

## Persistence (Flyway V5)

Table **`app_user`** (`user` is reserved in PostgreSQL):

```sql
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

-- Seed exactly one demo user (hash generated at implement time)
INSERT INTO app_user (name, email, password_hash, created_at)
VALUES (
    'Demo Trainer',
    'demo@pokefolio.dev',
    '<bcrypt-hash-at-implement>',
    NOW()
);
```

| Column | Maps to |
|--------|---------|
| `id` | `UserAccount.id` |
| `name` | display name (trimmed) |
| `email` | normalised lowercase |
| `password_hash` | opaque BCrypt hash |
| `created_at` | `createdAt` |

**Adapter rules** (002–005 patterns):

- `TransactionTemplate` only (no `@Transactional` on adapter methods).
- Unique violation on constraint name **`uq_app_user_email`** →
  `DuplicateEmailException` (including concurrent register races).
- Demo plaintext password (suggested for docs until hash is fixed at
  implement): `Demo-Passw0rd!` — labelled **demo credential** in README.
- Collection seed **reset** used by ITs must **not** truncate/reseed
  `app_user`.

## Access token (JWT)

| Claim | Value |
|-------|-------|
| `iss` | Fixed issuer string (e.g. `pokefolio`) |
| `sub` | User id as string |
| `iat` | Issued-at from injected `Clock` (live issuer) |
| `exp` | `iat` + configured `Duration` (default `1h`) |

Algorithm: **HS256** with secret from `JWT_SECRET` (≥ 32 UTF-8 bytes).
`SecretKey` / `JwtEncoder` / `JwtDecoder` built once in web/config from
validated `pokemonapp.security.jwt` properties.

**Expired-token tests**: mint with a test `JwtEncoder` and set `exp` at
least **2 minutes** in the past (decoder default clock skew is 60 s). Do
**not** alter the global `Clock` bean.

No refresh tokens; clients discard the token when done.

## Collection (unchanged ownership)

`LocalPokemon` / `pokemon_record` unchanged. Writes require authentication
at the **HTTP** layer only; no `owner_id` / user FK on collection rows in
006.
