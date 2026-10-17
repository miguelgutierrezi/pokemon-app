# Research: HU-03 Data Synchronization

**Feature**: `004-hu03-data-sync` | **Date**: 2026-10-17

Versions and Spring / Jackson / Boot starter APIs must be re-checked against
current docs at implement time (same rule as 002/003). Extend existing
modules; do not fork catalog or ProblemDetail stacks.

## R1. Spring Data JPA starter (Boot 4)

**Decision**: Add `org.springframework.boot:spring-boot-starter-data-jpa`
(BOM-managed, project parent **4.1.1**) to the **infrastructure** module.
Verified on Maven Central for Boot 4.1.1 (2026-10-17): artifact name is
unchanged from Boot 3 (`spring-boot-starter-data-jpa`); the starter pulls
`spring-boot-data-jpa` + JDBC. Keep existing
`spring-boot-starter-jdbc` / Flyway; JPA uses the same DataSource.

**Rationale**: Spec deferred JPA from 001 until a real persistence consumer
exists; 004 is that consumer (TR-DATA). Hibernate/JPA stays behind the
adapter.

**Alternatives considered**: Stay on plain JDBC/JdbcTemplate (rejected:
more mapping boilerplate for types/tags collections and concurrent unique
handling); introduce JPA in domain (rejected: constitution I).

## R2. Import orchestration — exists → fetchDetail → save + Clock

**Decision**: `ImportPokemonUseCase` order (mandatory):

1. `repository.existsByNationalNumber(n)` — if true →
   `DuplicateLocalPokemonException` (**409**) **without** calling the
   catalog.
2. `PokemonCatalogPort.fetchDetail(n)` only (003 phase a) — never
   `fetchEvolutionChain` / `fetchEntries`.
3. Map `CatalogDetailEntry` →
   `LocalPokemon.snapshotOf(entry, clock.instant())` (proprietary empty)
   and `repository.save(...)`.

Inject `java.time.Clock` (bean in `web/config`) so unit tests fix
`syncedAt`. Catalog 404 → `PokemonNotFoundException` (→ HTTP 404);
unavailable → `CatalogUnavailableException` (→ 502). The store unique
constraint remains the **backstop** for concurrent imports that both pass
`existsBy`.

**Rationale**: Avoids a useless upstream round-trip when the national
number is already local; snapshot fields still come only from
`CatalogPokemonDetail` / `CatalogSpeciesDetail`. Evolution tree is HU-02
only.

**Alternatives considered**: Catalog-first then save (rejected: wastes
PokeAPI on known duplicates); reuse full detail use case (rejected: extra
upstream + unused graph); store evolution snapshot (rejected: YAGNI).

## R3. Transaction boundary — no DB tx during HTTP; saveAndFlush

**Decision**: Use-case / adapter flow:

1. `existsBy` and catalog `fetchDetail` run **outside** any write
   `@Transactional` boundary that spans HTTP.
2. Adapter `save` is its **own** short `@Transactional` method and calls
   Spring Data `saveAndFlush` (not plain `save`), so a unique-constraint
   `DataIntegrityViolationException` surfaces **inside** the adapter.
3. The adapter catches that violation (PostgreSQL unique on
   `national_number`) and translates to `DuplicateLocalPokemonException`.

**Rationale**: Holding a connection/transaction open across RestClient
latency risks pool exhaustion and long locks (SC-001 path). With plain
`save()`, the flush/commit may happen after the adapter method returns,
so the violation becomes an untranslated 500. `saveAndFlush` forces the
constraint check while the adapter can still map it to 409. Concurrent
imports that both pass `existsBy` still hit the unique backstop (SC-002).

**Alternatives considered**: `@Transactional` around the whole use case
(rejected — holds tx during HTTP); plain `save()` (rejected — violation
may surface at commit outside the adapter); application catching Spring
data-access exceptions (rejected — leaks infrastructure types upward).

## R3b. JPA application.yml — open-in-view off, ddl-auto validate

**Decision**: In the app `application.yml` (web / shared Spring config used
at runtime):

| Property | Value | Why |
|----------|-------|-----|
| `spring.jpa.open-in-view` | `false` | Removes Boot’s startup WARN; avoids a request-long persistence session (lazy loads must be explicit in the adapter) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Flyway owns schema (V2/V3); Hibernate only validates entity mappings at startup |

**Rationale**: Aligns with Flyway-first migrations and Clean Architecture
(no accidental schema drift; no OSIV anti-pattern).

**Alternatives considered**: Default `open-in-view=true` (rejected: WARN +
request-scoped session); `ddl-auto=update` / `none` (rejected: Flyway is
source of truth; `validate` catches mapping drift early).

## R4. `@DataJpaTest` slice in infrastructure

**Decision**: Place an empty `@SpringBootConfiguration` under
`infrastructure/src/test/java/...` so `@DataJpaTest` can bootstrap without
pulling the `web` application class. Tests use Testcontainers PostgreSQL
(same pattern as project Postgres IT). Prove: save/find/page; concurrent
insert of same `national_number` → one row + duplicate exception path.

**Rationale**: Spec assumption R6 / TR-UNIT — infrastructure covers its own
code; ArchUnit already forbids JPA leaking upward.

**Alternatives considered**: Only full `@SpringBootTest` for persistence
(rejected: slower and couples to web); H2 (rejected: dialect drift vs
Postgres unique/FK behaviour).

## R5. Optimistic locking `version` column — YAGNI until 005

**Decision**: **Do not** add a `version` / `@Version` column in V2.
HU-03 is create + read only; no concurrent updates of proprietary fields.
Feature **005** (HU-04 local update) introduces optimistic locking when
edit lands — add column via a later Flyway migration then.

**Rationale**: Constitution IX / YAGNI. Adding an unused version column now
is a speculative schema change with no reader/writer. A later `ALTER TABLE
... ADD COLUMN version` in 005 is cheap for this small table.

**Alternatives considered**: Include `version BIGINT NOT NULL DEFAULT 0` in
V2 “for free” (rejected: unused surface + entity mapping noise in 004);
application-level last-write-wins without version in 005 (weaker; 005 plan
should prefer `@Version`).

## R6. Schema V2 + seed V3

**Decision**:

| Migration | Purpose |
|-----------|---------|
| `V2__pokemon_record.sql` | `pokemon_record` (surrogate `id`, unique `national_number`, snapshot columns, proprietary columns, `created_at` / `updated_at`); `pokemon_record_type(record_id, slot, type)` FK CASCADE; `pokemon_record_tag(record_id, tag)` unique `(record_id, tag)` FK CASCADE |
| `V3__seed_local_collection.sql` | Static Bulbasaur / Ivysaur / Venusaur rows + sample proprietary fields; tags `first-partner`, `field-notes`, `forest` (lowercase; never `favorites`). No network. |

ON DELETE CASCADE on child tables prepares 005 delete without orphan rows.
006 will assign seed rows to the demo user (ownership column / join later —
not in 004).

**Rationale**: TR-DB + DEL-SEED; FKs with CASCADE match “delete record
removes types/tags” expected in 005.

**Alternatives considered**: Seed via application `@PostConstruct` (rejected:
needs catalog or hardcoded service code; migrations already run on compose
up); single JSON column for types/tags (rejected: weaker uniqueness /
harder queries).

## R7. Public write until 006 (constitution VI)

**Decision**: Document temporary exception in plan Complexity Tracking and
spec FR-010. Routes remain unauthenticated; store is global. 006 adds JWT
on writes and per-user scope; HU-03 seed rows assigned to the demo user.

**Rationale**: Feature ordering — auth is a separate story. Merging auth
into 004 would expand scope beyond HU-03.

**Alternatives considered**: Stub auth filter now (rejected: fake security);
delay collection writes until 006 (rejected: blocks TR-API-CRUD Create and
demo of local store).

## R8. HTTP surface + error mapping

**Decision**:

| Operation | Route | Success | Errors |
|-----------|-------|---------|--------|
| Import | `POST /api/v1/collection` body `{ nationalNumber }` | 201 + `Location: /api/v1/collection/{n}` | 400, 404 (catalog), 409, 502 |
| List | `GET /api/v1/collection?page&size` | 200 page | 400 |
| Get | `GET /api/v1/collection/{nationalNumber}` | 200 | 400, 404 (**local** wording) |

`CollectionController` + Bean Validation request record; DTOs mirror
[collection-api.yaml](./contracts/collection-api.yaml). Advice maps
`DuplicateLocalPokemonException` → 409 and
`LocalPokemonNotFoundException` → 404 with distinct detail. Reuse existing
catalog 404/502 handlers.

**Rationale**: Spec assumptions + FR-011; same ProblemDetail conventions as
002/003.

**Alternatives considered**: Nested under `/api/v1/pokemon/.../local`
(rejected: conflates catalog vs collection); PUT upsert (rejected: spec is
create-once + 409).
