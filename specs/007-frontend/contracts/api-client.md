# API client contract: 007-frontend

**Date**: 2026-10-17 | **Spec**: [../spec.md](../spec.md) |
**Data model**: [../data-model.md](../data-model.md)

Same-origin calls to `/api/v1/...` (Vite proxy in dev; nginx in Docker).
**No path rewrite.**

---

## Auth header policy

| Call | `Authorization` |
|---|---|
| `GET /api/v1/pokemon/**` | **Never** |
| `GET /api/v1/collection/**` | **Never** |
| `POST /api/v1/auth/register` | **Never** |
| `POST /api/v1/auth/login` | **Never** |
| `POST /api/v1/collection` | Bearer if session valid |
| `PUT /api/v1/collection/{n}` | Bearer if session valid |
| `DELETE /api/v1/collection/{n}` | Bearer if session valid |

If a protected write is attempted without a valid (non-expired) session,
the UI redirects to login **before** calling the API when the action is
user-initiated. When the server returns **401** on a protected write,
clear the session and show the session-expired **dialog** (FR-009) —
**not** a silent/direct redirect: **Sign in again** →
`/login?redirect=<sanitised current path+search>&reason=expired`;
**Browse catalog** → `/`.

---

## ApiError

```ts
type ApiError = {
  status: number;      // HTTP status, or 0 for network / unparseable
  type: string;        // Problem type URN or ""
  title: string;
  detail: string;      // user-facing
  field?: string;      // optional form field name
};
```

---

## Error mapping table

| Condition | `status` | `type` (typical) | `detail` / UI | `field` |
|---|---|---|---|---|
| Problem+json body | HTTP status | from body | from body | see extraction |
| BV / type mismatch | 400 | `urn:problem-type:bad-request` | `Invalid value for parameter 'x'` | `x` |
| Unknown/read-only JSON property | 400 | `urn:problem-type:bad-request` | `Unknown or read-only property 'x'` | `x` |
| Invalid/missing JSON property | 400 | `urn:problem-type:bad-request` | `Invalid or missing value for property 'x'` | `x` |
| Page out of range / other 400 | 400 | `urn:problem-type:bad-request` | server detail | if quoted |
| Login bad credentials | 401 | `urn:problem-type:unauthorized` | `Invalid email or password` | — |
| Missing/invalid Bearer on write | 401 | `urn:problem-type:unauthorized` | `Missing or invalid access token` (+ WWW-Authenticate) | — → clear session + **session-expired dialog** (Sign in again → `/login?redirect=<sanitised>&reason=expired`; Browse catalog → `/`) |
| Forbidden | 403 | `urn:problem-type:forbidden` | server detail | — |
| Catalog/local not found | 404 | `urn:problem-type:not-found` | server detail | — |
| Duplicate import | 409 | `urn:problem-type:conflict` | already in local collection | — → “Already in the collection” |
| Duplicate email | 409 | `urn:problem-type:conflict` | `An account with this email already exists` | — |
| Stale version | 409 | `urn:problem-type:stale-version` | modified elsewhere… | — → “Modified elsewhere — reload” |
| Upstream catalog | 502 | `urn:problem-type:bad-gateway` | temporarily unavailable | — → retry |
| Network / CORS / aborted | 0 | `""` | generic “Network error. Try again.” | — |
| Non-JSON / non-problem HTTP error | HTTP or 0 | `""` | generic or truncated text | — |

---

## Field extraction (contract on 005/006 detail formats)

Pure function over `detail` string (unit-tested):

1. Match `/parameter '([^']+)'/` → capture group 1.
2. Else match `/property '([^']+)'/` → capture group 1.
3. Else `field` undefined.

**Dependency**: `GlobalExceptionHandler` uses these quoted forms for BV and
Jackson property errors. If the backend changes wording, update parser +
tests in the same change (still no backend work in 007 unless a bugfix
is agreed separately).

---

## Return-path sanitiser

```ts
function sanitizeReturnPath(raw: string | null | undefined): string
```

| Input | Output |
|---|---|
| `undefined` / `null` / `""` | `/` |
| `/collection?page=2` | `/collection?page=2` |
| `/pokemon/25` | `/pokemon/25` |
| `//evil.com` | `/` |
| `https://evil.com` | `/` |
| `pokemon/1` (no leading `/`) | `/` |

Used for login/register success and for building `redirect=` query values
(encode when placing in the URL; sanitise when reading).
