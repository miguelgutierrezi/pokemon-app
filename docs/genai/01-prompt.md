# Prompt — RESTful Task Management API

**Requirement:** [GENAI] in `docs/requirements.md`.  
**Tool used for the raw run:** Cursor Agent (isolated scratch folder, no Pokéfolio repo context).  
**Date of prompt text:** 2026-10-18.

Copy everything below the line into a **new** chat / scratch workspace with no project history.

---

You are scaffolding a small **Task Management REST API**. Do **not** invent product
features beyond what is listed. First reply with a **short plan** (modules,
endpoints, ownership rule, error shapes). Wait for no further confirmation — then
emit the representative code files listed at the end.

## Context (match this stack)

- Java **21**, Spring Boot **4.1.x**
- **Clean Architecture** Maven modules: `domain` → `application` → `infrastructure` → `web`
  (dependencies only inward; domain has no Spring/JPA)
- Ports & adapters: use cases in `application`, persistence/security adapters in
  `infrastructure`, HTTP in `web`
- PostgreSQL + **Flyway** migrations; Spring Data **JPA**
- Spring Security as a **JWT resource server** (Bearer access tokens; a basic
  `User` model already exists — do not rebuild auth)
- OpenAPI via springdoc

## Product

CRUD for **tasks** belonging to a user:

| Field | Notes |
|---|---|
| `title` | required, non-blank |
| `description` | optional string |
| `status` | enum (e.g. `TODO`, `IN_PROGRESS`, `DONE`) |
| `due_date` | `java.time.LocalDate` |
| `version` | optimistic locking field, exposed on responses and required on update |

Assume the authenticated principal exposes a stable `userId` (Long).

## Hard constraints

1. **TDD** — write failing tests first; then production code.
2. **Bean Validation** on request DTOs **and** domain invariants (blank title,
   max lengths, valid status, due-date policy you state explicitly).
3. Errors as **RFC 7807** `ProblemDetail` / `application/problem+json` — never
   leak exception class names or stack traces in the body.
4. All endpoints under **`/api/v1`**.
5. List endpoints use **pagination** with a **hard max page size** (reject larger).
6. `status` is an **enum** (invalid value → **400**, not 500).
7. `due_date` is **`LocalDate`** (JSON date, not datetime).
8. **Ownership:** a user may only list/read/update/delete **their own** tasks.
   Looking up another user’s task id returns **404 Not Found**, never **403**
   (do not confirm existence).
9. **Optimistic locking:** updates include `version`; mismatch → conflict
   (409) with a stable problem type.
10. **No secrets** in source (no default JWT secrets, no hardcoded DB passwords).
11. Document the API with **OpenAPI** annotations / springdoc.

## Deliverables (representative sample only)

After the short plan, produce these files (enough to review security and
validation — not a full multi-module repo dump):

1. Domain `Task` (or equivalent aggregate) with invariants
2. Create/Update **request DTO**(s) with Bean Validation
3. `TaskController` (or web adapter)
4. Application **use case / service** that enforces ownership
5. Repository **port + JPA adapter** sketch (or Spring Data interface)
6. **One** focused test (prefer ownership or optimistic-lock; not only happy path)

Keep packages coherent with the module layout above.

## Why these constraints (failure modes)

- **TDD first** — without a failing test, ownership and stale-version bugs ship
  silently and only show up in production races.
- **BV + domain invariants** — edge-only checks miss trimmed blanks; domain-only
  checks become unhandled 500s if the HTTP layer does not map them.
- **RFC 7807, no leak** — default Spring error bodies expose class names and
  help attackers fingerprint the stack.
- **`/api/v1` + max page size** — unbounded `size=` is an easy DoS / memory spike;
  a versioned prefix keeps breaking changes from colliding with old clients.
- **Enum status + LocalDate** — free-form strings become 500s on deserialize;
  datetimes invite timezone bugs for a calendar “due day”.
- **404 not 403 for foreign ids** — 403 confirms the resource exists (IDOR /
  enumeration). 404 is the safe not-found for both missing and not-owned.
- **Optimistic `version`** — last-write-wins without compare-and-set loses
  concurrent edits (classic lost update).
- **No secrets in code** — committed defaults are copied into forks and CI logs;
  fail-fast on missing env is cheaper than a leaked signing key.
