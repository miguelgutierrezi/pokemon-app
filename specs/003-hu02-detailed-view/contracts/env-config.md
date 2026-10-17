# Contract: Environment configuration (003)

**Feature**: `003-hu02-detailed-view`

Extends `specs/001-setup/contracts/env-config.md` and
`specs/002-hu01-pokemon-enumeration/contracts/env-config.md`.

## Keys added in 003

**None.** Detail reuses the existing PokeAPI client configuration:

| Key / property | Source |
|----------------|--------|
| `POKEAPI_BASE_URL` / `pokemonapp.pokeapi.base-url` | 002 |
| `pokemonapp.pokeapi.connect-timeout` / `read-timeout` | 002 |
| `pokemonapp.pokeapi.cache.ttl` / id & list max sizes | 002 — evolution-chain cache uses the same TTL (and id-tier max size unless tasks choose otherwise) |

No Compose or `.env.example` changes are required for HU-02.

## Keys from prior features (still required)

`POSTGRES_*` — unchanged (`${VAR:?...}` fail-fast in Compose).
`POKEAPI_BASE_URL` — listed in `.env.example` with default
`https://pokeapi.co/api/v2`.
