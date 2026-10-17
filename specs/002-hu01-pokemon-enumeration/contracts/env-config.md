# Contract: Environment configuration (002 additions)

**Feature**: `002-hu01-pokemon-enumeration`

Extends the rules in `specs/001-setup/contracts/env-config.md`. New keys are
added only because this feature first uses them (constitution IX).

## Rules (unchanged)

1. Every key below MUST appear in root `.env.example` with a non-secret
   placeholder.
2. Real secrets MUST NOT be committed; `.env` MUST remain git-ignored.
3. Compose uses `${VAR:?...}` for required secrets (`POSTGRES_*`). For the
   non-secret catalog URL, Compose uses an explicit default (below).
4. Later features add only the keys they use.

## Keys added in 002

| Key | Example / default | Required | Consumed by |
|-----|-------------------|----------|-------------|
| `POKEAPI_BASE_URL` | `https://pokeapi.co/api/v2` | listed in `.env.example`; has defaults | backend RestClient / `PokeApiCatalogAdapter` |

**Defaults (no either/or)**:
- `application.yml`: default `POKEAPI_BASE_URL` /
  `pokemonapp.pokeapi.base-url` = `https://pokeapi.co/api/v2`
- `.env.example`: include `POKEAPI_BASE_URL=https://pokeapi.co/api/v2`
- `docker-compose.yml` backend env:
  `POKEAPI_BASE_URL: ${POKEAPI_BASE_URL:-https://pokeapi.co/api/v2}`

**Timeouts / cache**: bind via Spring properties (e.g.
`pokemonapp.pokeapi.connect-timeout`, `read-timeout`,
`pokemonapp.pokeapi.cache.ttl` default **24h**, `*.max-size`) in
`application.yml`; override with env if needed. Not required in
`.env.example` unless operators must tune them for demos.

## Keys from 001 (still required)

`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` — unchanged
(`${VAR:?...}` fail-fast in Compose).
