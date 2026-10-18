# Contract: Security route policy (006)

**Feature**: `006-auth` | **Date**: 2026-10-17

Implements FR-005 / FR-007. Filter chain lives in `web/config`
(`SecurityFilterChain` `@Bean`). Session **STATELESS**; CSRF off;
`httpBasic` / `formLogin` off; OAuth2 resource-server JWT.

## Matchers are METHOD + PATH

The same path can be public for one method and protected for another.
Example: `GET /api/v1/collection/**` is public; `POST /api/v1/collection`
is protected. Path-only public matchers are **incorrect**.

### Shared public `RequestMatcher`s

Define the public matcher list **once** (constant / `@Bean` / helper in
`web/config`) and reuse that **same** set in:

1. `authorizeHttpRequests` → `requestMatchers(...).permitAll()`
2. Custom `BearerTokenResolver` → return `null` when any shared matcher
   matches (ignore `Authorization`)

This prevents drift between “permitted” and “token ignored”.

### Public (permitAll + ignore Bearer)

| Method + path | Notes |
|---------------|--------|
| `GET /api/v1/pokemon/**` | Catalog reads |
| `GET /api/v1/collection/**` | Collection reads only |
| `POST /api/v1/auth/register` | Registration |
| `POST /api/v1/auth/login` | Login |
| `/actuator/health` | Health (method as exposed) |
| OpenAPI / Swagger UI paths | As exposed by springdoc |

Stale/expired/tampered Bearer on public **GETs** MUST still yield **200**
(subject to existing business rules) — never **401**.

### Protected (authenticated — Bearer **not** ignored)

| Method + path | Notes |
|---------------|--------|
| `POST /api/v1/collection` | Import — valid token required |
| `PUT /api/v1/collection/{nationalNumber}` | Update |
| `DELETE /api/v1/collection/{nationalNumber}` | Delete |

Missing / invalid / expired / tampered → **401**
`urn:problem-type:unauthorized` + `WWW-Authenticate: Bearer`.

**Required IT**: `POST /api/v1/collection` with a **valid** Bearer token
→ **201** (or existing 004 business status such as **409** duplicate) —
proves the token is **not** ignored on the protected method of a path
that is public for GET.

Collection remains a **single shared** store (no per-user filtering).

### Deny by default

`anyRequest().denyAll()`.

Examples:

| Request | Token | Expected |
|---------|-------|----------|
| `GET /api/v1/does-not-exist` | none | **401** unauthorized |
| `GET /api/v1/does-not-exist` | valid Bearer | **403** `urn:problem-type:forbidden` |

## ERROR dispatcher

Also permit `DispatcherType.ERROR` (e.g.
`dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()`).

**Why**: After a controller produces a business error (404, 400, 409, 415,
502, …), the container forwards to `/error`. Under deny-by-default, that
dispatch would otherwise be treated as an unauthenticated/forbidden
request and **rewrite** the real status to **401**/**403**. Permitting
ERROR keeps RFC 7807 business statuses intact; entry point / access-denied
handlers still own genuine authn/authz failures.

## Error writers

| Handler | Status | `type` | Extra |
|---------|--------|--------|-------|
| `AuthenticationEntryPoint` | 401 | `urn:problem-type:unauthorized` | `WWW-Authenticate: Bearer`; `application/problem+json` |
| `AccessDeniedHandler` | 403 | `urn:problem-type:forbidden` | `application/problem+json` |

Both reuse the same ProblemDetail field shape as `GlobalExceptionHandler`
(`type`, `title`, `status`, `detail`, `instance`). No stack traces or
framework package names. Security failures MUST NOT fall through to the
catch-all **500**.

## OpenAPI

- Global or component `bearerAuth` security scheme (`http` / `bearer` /
  `JWT`) — see [auth-api.yaml](./auth-api.yaml).
- `@SecurityRequirement` on protected collection write operations
  (004/005 controllers updated at implement).
- Document **401** / **403** on those operations; extend
  `OpenApiDocsIntegrationTest`.
