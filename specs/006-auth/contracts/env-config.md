# Contract: Environment configuration (006)

**Feature**: `006-auth`

Extends `specs/001-setup/contracts/env-config.md` through
`specs/005-hu04-local-update/contracts/env-config.md`.

## Keys added in 006

| Key / property | Source | Required | Notes |
|----------------|--------|----------|-------|
| `JWT_SECRET` | Env → `pokemonapp.security.jwt.secret` | **Yes** | HS256 signing secret; **no default**. Bound into validated `@ConfigurationProperties(prefix = "pokemonapp.security.jwt")` in **web/config**. Fail fast if missing or UTF-8 length **&lt; 32** bytes. |
| `pokemonapp.security.jwt.expiry` | `application.yml` / optional env binding | No | `Duration`, default **`1h`** (3600 s → `expiresIn`) |

**Bean construction**: one web/config class reads the validated properties and
builds the shared `SecretKey`, `JwtEncoder`, and `JwtDecoder` used by the
resource server and the infrastructure `AccessTokenIssuer`.

### Compose

```yaml
# backend service environment
JWT_SECRET: ${JWT_SECRET:?}
```

`${JWT_SECRET:?}` fails compose start if unset — never embed a default
secret in compose or `application.yml`.

### `.env.example`

```bash
# HS256 JWT signing secret (≥ 32 bytes). Generate locally, never commit real values:
#   openssl rand -base64 48
JWT_SECRET=
```

### CI

Docker smoke / compose jobs MUST generate a throwaway secret in the
workflow environment (e.g. `openssl rand -base64 48`) and pass it as
`JWT_SECRET`. Never commit a production or shared signing secret.

### Tests

Test `application-*.yml` / `DynamicPropertySource` supplies a
**non-production** ≥32-byte secret. Demo user password is a labelled
**demo credential** in README / migration seed — distinct from
`JWT_SECRET`.

## Unchanged keys

| Key / property | Source |
|----------------|--------|
| `POSTGRES_*` | 001 — DataSource + Flyway (V5 uses same DB) |
| PokeAPI client | 002/003 — import only |
| Jackson fail-on-unknown | app config (005) |
