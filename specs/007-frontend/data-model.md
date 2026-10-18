# Data Model (client): 007-frontend

**Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Client-side types mirroring backend DTOs (002–006). No persistence schema
changes. Field names use the API’s JSON camelCase.

---

## Session

Persisted under `localStorage` key **`pokefolio.session`**.

| Field | Type | Notes |
|---|---|---|
| `accessToken` | `string` | Opaque JWT; never logged |
| `expiresAt` | `number` | Epoch ms; `Date.now() + expiresIn * 1000` at issue |
| `user.id` | `number` | Surrogate id |
| `user.name` | `string` | Display name |
| `user.email` | `string` | Normalised lowercase from API |

**Transitions**:

```text
absent/corrupt/expired → signed out (cleared)
login|register 200/201 → signed in (written)
logout | 401 on protected write | pre-write expiry check → signed out
storage event (other tab) → re-read or clear
```

---

## Auth API shapes

### RegisterRequest / LoginRequest

| Field | Register | Login | Client validation |
|---|---|---|---|
| `name` | required | — | trim, length 1–100 |
| `email` | required | required | non-blank; basic shape; server is source of truth |
| `password` | required | required | register: ≥ 8 chars and ≤ 72 UTF-8 bytes (`TextEncoder`); login: non-blank only |

### AuthResponse

| Field | Type |
|---|---|
| `accessToken` | `string` |
| `tokenType` | `"Bearer"` |
| `expiresIn` | `number` (seconds) |
| `user` | `{ id, name, email }` |

---

## Catalog (HU-01)

### PokemonSummary (list item)

| Field | Type | UI |
|---|---|---|
| `nationalNumber` | `number` | key / link to detail |
| `name` | `string` | card title; image alt |
| `spriteUrl` | `string` | card image |
| `category` | `string` | card |
| `weightKg` | `number` | display one decimal |
| `abilities` | `{ name: string; hidden: boolean }[]` | skills list |

### PokemonPage

| Field | Type |
|---|---|
| `content` | `PokemonSummary[]` |
| `page` | `number` (1-based) |
| `size` | `number` |
| `totalElements` | `number` |
| `totalPages` | `number` |

Request: `GET /api/v1/pokemon?page=&size=` with UI `size=9`.

---

## Detail (HU-02)

### PokemonDetail

| Field | Type | UI |
|---|---|---|
| `nationalNumber` | `number` | |
| `name` | `string` | |
| `category` | `string` | |
| `types` | `string[]` | |
| `heightM` / `weightKg` | `number` | |
| `abilities` | `{ name; hidden }[]` | |
| `generation` | `string` | |
| `imageUrl` | `string` | primary image |
| `stats` | `{ hp, attack, defense, specialAttack, specialDefense, speed, total }` | |
| `flavorText` | `{ text, version }` | |
| `evolution` | `EvolutionNode` | lineage |

### EvolutionNode

| Field | Type | UI |
|---|---|---|
| `nationalNumber` | `number` | |
| `name` | `string` | |
| `imageUrl` | `string` | thumbnail |
| `isCurrent` | `boolean` | highlight |
| `children` | `EvolutionEdge[]` | |

### EvolutionEdge

| Field | Type |
|---|---|
| `trigger` | `string` |
| `minLevel` | `number \| null` |
| `itemSlug` | `string \| null` |
| `node` | `EvolutionNode` |

**Presentation rule**: if every node has at most one child edge → render as
a **sequence**; if any node has multiple children → render branching as a
**list**.

---

## Collection (HU-03 / HU-04 / FE-CRUD)

### LocalPokemon

| Field | Type | UI |
|---|---|---|
| `nationalNumber` | `number` | id / route param |
| `name` | `string` | catalog snapshot (read-only) |
| `types` | `string[]` | read-only |
| `heightM` / `weightKg` | `number` | read-only |
| `category` / `generation` / `imageUrl` | `string` | read-only |
| `syncedAt` | `string` (ISO-8601) | read-only |
| `localizedName` | `string \| null` | editable |
| `location` | `string \| null` | editable |
| `tags` | `string[]` | editable chips |
| `version` | `number` | optimistic lock; send on PUT |

### LocalPokemonPage

Same envelope as catalog page (`content`, `page`, `size`, `totalElements`,
`totalPages`). UI `size=9`.

### ImportPokemonRequest

`{ nationalNumber: number }` — `POST /api/v1/collection`.

### UpdateLocalPokemonRequest

| Field | Client rule |
|---|---|
| `localizedName` | string ≤ 100 or `null` if empty |
| `location` | string ≤ 200 or `null` if empty |
| `tags` | ≤ 20 items; each trimmed non-blank ≤ 32; never `null` (use `[]`) |
| `version` | integer from last GET |

---

## ApiError

| Field | Type | Notes |
|---|---|---|
| `status` | `number` | HTTP status; `0` for network / unparseable |
| `type` | `string` | Problem `type` URN when present; else `""` |
| `title` | `string` | Problem title or generic |
| `detail` | `string` | User-facing message |
| `field` | `string \| undefined` | From `parameter 'x'` / `property 'x'` in detail |

---

## UI form state (ephemeral)

Not persisted.

- **LoginForm**: `email`, `password`, `error` (form-level), `submitting`.
- **RegisterForm**: `name`, `email`, `password`, `fieldErrors`, `submitting`.
- **EditForm**: `localizedName`, `location`, `tags[]`, `version`,
  `fieldErrors`, `stale` flag, `submitting`.
