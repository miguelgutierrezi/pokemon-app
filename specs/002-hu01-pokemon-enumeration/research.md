# Research: HU-01 Pokemon Enumeration

**Feature**: `002-hu01-pokemon-enumeration` | **Date**: 2026-10-16
(revised after Miguel’s review)

Versions below were checked against Maven Central / springdoc.org /
Spring Framework docs on 2026-10-16. Do not substitute from memory at
implement time without re-checking.

## R1. HTTP client — Spring RestClient

**Decision**: Use Spring Framework `RestClient` (via
`spring-boot-starter-webmvc`) for PokeAPI calls. Configure connect/read
timeouts on the underlying `ClientHttpRequestFactory` (JDK
`HttpClient`/`JdkClientHttpRequestFactory` or equivalent Boot 4.1 factory
APIs). Base URL from `POKEAPI_BASE_URL` (default
`https://pokeapi.co/api/v2` in `application.yml`).

**Rationale**: First-party Spring client for Boot 4; no extra HTTP stack;
easy to construct in unit tests without a Spring context.

**Alternatives considered**: WebClient (reactive weight unnecessary);
`RestTemplate` (maintenance mode); raw `HttpClient` (more boilerplate).

## R2. Parallel fan-out — virtual threads

**Decision**: For each page, after `speciesPage(offset, limit)` returns ids +
total, fetch `/pokemon/{id}` and `/pokemon-species/{id}` pairs concurrently
using `Executors.newVirtualThreadPerTaskExecutor()` (Java 21), with a clear
timeout/cancel policy so a single slow call cannot hang the request past
configured read timeouts. If any required call fails, fail the whole page
via `CatalogUnavailableException` (502) — never return a partial page as
complete. Do not cache failures.

**Rationale**: Max size 20 → up to 40 GETs; virtual threads keep code
synchronous-style and match Java 21.

**Assumption**: For National Dex ids **1–1025**, species id equals the
default Pokémon id used by `/pokemon/{id}`.

**Alternatives considered**: Fixed platform thread pool (harder to size);
sequential fetches (fails SC-004).

## R3. Caching — programmatic Caffeine (not @Cacheable)

**Decision**: Own Caffeine `Cache` instances inside the infrastructure
adapter (or a caching decorator of `PokemonCatalogPort`), **not** Spring
`@Cacheable` / `spring-boot-starter-cache`. Three caches for reuse in 003:

| Cache | Key | Value |
|---|---|---|
| `pokeapi-species-list` | `offset:limit` | species page (ids + count) |
| `pokeapi-pokemon` | national id | pokemon resource |
| `pokeapi-species` | national id | species resource |

Defaults (properties, overridable): TTL **24 hours**, max size **2048**
entries per id-cache (species-list may use a smaller max, e.g. **256**).
Depend on `com.github.ben-manes.caffeine:caffeine` from Boot **4.1.1 BOM
(caffeine.version 3.2.4)**.

**Cache policy**: only successful responses are stored; transport/timeout/5xx
and incomplete page assembly **must never** be cached.

**Rationale**: Infrastructure tests (WireMock, no Spring context) can assert
two identical requests → one upstream call. Spring Cache would force a
context and obscure that proof.

**Alternatives considered**: `@Cacheable` + starter-cache (rejected: Spring
context required); Redis (ops weight).

## R4. springdoc OpenAPI 3.x for Boot 4 — spike first

**Decision**: `org.springdoc:springdoc-openapi-starter-webmvc-ui` **3.1.1**
(latest stable on Central; Boot 4 line per springdoc.org).

**Implementation gate**: The **first web task** is a spike — add the
dependency, boot the app, assert `GET /v3/api-docs` returns **200**. If it
fails, **stop and decide** before writing controllers or advice.

**Rationale**: Constitution IV; deferred from 001; compatibility risk on Boot
4 is handled by an explicit spike instead of speculative notes.

**Alternatives considered**: springdoc 2.x (Boot 3 only); hand-written YAML
only (no live UI for interview).

## R5. WireMock — standalone for web IT

**Decision**:
- Infrastructure tests: `org.wiremock:wiremock` **3.13.2** *or*
  `wiremock-standalone` **3.13.2** (either is fine without a Spring context).
- Web IT (`@SpringBootTest`): use **`org.wiremock:wiremock-standalone`
  3.13.2** to avoid Jetty / servlet clashes with Spring Boot’s embedded
  server.

Avoid WireMock 4.x beta for 002.

**Rationale**: R6 coverage in infrastructure; standalone artifact is the
safer classpath for a full Boot IT.

**Alternatives considered**: MockWebServer; Mockito-only RestClient stubs
(weaker contract fidelity).

## R6. Validation split (two-layer)

**Decision**:
- **Web**: `@RequestParam` with `@Min(1)` on `page`; `@Min(1)` `@Max(20)` on
  `size`; default size **9** when omitted. Failures → 400 ProblemDetail via
  advice.
- **Domain / use case**: After `speciesPage` returns `total`,
  `PageRequest` / domain check rejects page beyond last →
  `PageOutOfRangeException` → 400. Weight/name/genus mapping pure in domain.

**Rationale**: Constitution V; Bean Validation cannot know “last page”
without the catalog `count` from the same `speciesPage` call.

## R7. Error mapping — ProblemDetail

**Decision**: `CatalogUnavailableException` lives in **`application`**.
Global `@RestControllerAdvice` in **web** maps:
- validation / constraint / domain out-of-range page → **400**
  `application/problem+json`
- `CatalogUnavailableException` (and equivalent catalog failures) → **502**
  `application/problem+json`
Never expose stack traces in the body. Never cache failures.

**Rationale**: Constitution IV + spec FR-005/FR-006; keeps HTTP mapping out
of domain/infrastructure.

## R8. PokeAPI resources & mapping

**Decision**:
- One list call: `GET /api/v2/pokemon-species?offset=&limit=` → port operation
  `speciesPage(offset, limit)` returns **ids + total `count`** (PokeAPI
  returns both in one response). Page-beyond-last uses that count.
- Per id in parallel: `GET /api/v2/pokemon/{id}` and
  `GET /api/v2/pokemon-species/{id}` (mapping per spec).
- Default base URL: `https://pokeapi.co/api/v2` in `application.yml`;
  `.env.example` lists `POKEAPI_BASE_URL`; Compose sets
  `POKEAPI_BASE_URL: ${POKEAPI_BASE_URL:-https://pokeapi.co/api/v2}`.

**Rationale**: Spec FR-009–FR-012; single list call avoids a separate count
round-trip; avoids `/pokemon` list forms (10001+).

## R9. JaCoCo report-aggregate

**Decision**: Add a `report-aggregate` execution on the `web` module that
aggregates execution data from dependent modules for a single HTML report.
**Do not** bind `check` to the aggregate; per-module `check` remains the gate
(001 research R6).

**Rationale**: Visibility for interviewers without weakening the gate.
