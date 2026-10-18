# Review of raw Task API sample

Source: [`../raw/`](../raw/) (unedited). Only issues **actually present** in that
output are listed. Items that the checklist asks about but the sample got right
are called out under “What was already correct”.

## Issue found → Why it matters → Fix

| Issue found | Why it matters | Fix |
|---|---|---|
| Optimistic lock is an in-memory `version` check on a loaded aggregate, then `save` (`UpdateTaskUseCase` + `Task.update`). No compare-and-set `UPDATE … WHERE version = :expected`. `TaskEntity` has `@Version`, but no adapter maps the domain bump onto a single atomic write. | Two concurrent writers can both load the same version, both pass the domain check, and last-write-wins (lost update)—the same class of bug this repo fixed with a CAS UPDATE. Manual `version++` in the domain also fights Hibernate `@Version` if both are applied. | Persist with one atomic update (`UPDATE … SET …, version = version + 1 WHERE id = :id AND owner_user_id = :owner AND version = :expected`); 0 rows → 409. Keep `@Version` as the DB column; do not double-bump in the domain after a successful CAS. |
| `@Validated` + `@Max` / `@Min` on list query params (`TaskController.list`) throw `ConstraintViolationException`, which `TaskExceptionHandler` does **not** handle (only `MethodArgumentNotValidException` for `@RequestBody`). | Oversized `size=` (or invalid `page`) can surface as an unhandled exception → often **500** instead of a clean **400** problem+json, defeating the “max page size” constraint. | Add `@ExceptionHandler(ConstraintViolationException.class)` → 400 `ProblemDetail` with a stable `type` and no class/stack leak; keep `@Max(MAX_PAGE_SIZE)` on the parameter. |
| No handler for `HttpMessageNotReadableException` (invalid `status` enum string, bad date, malformed JSON). | Invalid status is supposed to be **400**; Boot’s default problem body can be verbose and is easy to leave as a fingerprinting surface. Without an explicit mapping, behaviour is framework-default rather than contract-owned. | Handle `HttpMessageNotReadableException` → 400 with a stable validation/problem type and a short detail (e.g. “Invalid or unreadable request body”); never put exception class names in `detail`. |
| Sample includes no Spring Security filter-chain / `authorizeHttpRequests` for `/api/v1/tasks/**`. Controller uses `@AuthenticationPrincipal` only. | Without authenticated authorization on writes (and reads), endpoints can remain public: missing Bearer becomes NPE/`500` or anonymous access instead of **401**. Annotations alone do not enforce auth. | Resource-server config: `anyRequest` for `/api/v1/tasks/**` → `authenticated()`; unauthenticated → 401 problem+json. Keep ownership checks for 404-not-403. |
| Global `@ExceptionHandler(IllegalArgumentException.class)` maps **every** IAE to 400 with `ex.getMessage()`. | Convenient for domain validation, but also turns programmer bugs (unexpected IAEs) into client 400s and couples HTTP status to unchecked domain throws. | Prefer typed domain exceptions (or BV at the edge) for expected 400s; do not use a catch-all IAE→400. Leave unexpected IAEs for the catch-all 500 path (without leaking internals). |
| No JPA adapter implementation—only `TaskJpaRepository` + `TaskEntity` sketch. `TaskRepository.save(Task)` has no shown mapping. | Reviewers cannot verify that ownership filters and version columns are applied on the write path; the “port” alone is not enough to trust persistence behaviour. | Add a thin `TaskJpaAdapter` that maps entity ↔ domain and implements CAS update + `findByIdAndOwnerUserId`. |
| Tests cover ownership + stale version (good) but not web-layer failures: blank title → 400, `size>50` → 400, invalid status JSON → 400, unauthenticated write → 401. | Use-case tests alone miss the ConstraintViolation / message-not-readable gaps above. | Keep the ownership/lock tests; add controller slice or WebTestClient cases for those HTTP contracts. |
| Overdue tasks cannot be updated: `@FutureOrPresent` on `UpdateTaskRequest.dueDate` and domain `dueDate >= today` on every update (raw `Task.update` / reviewed-era `withUpdate` without an unchanged exception). | A task that became overdue cannot be marked `DONE` (or otherwise edited) without also changing the due date — a common real workflow breaks at both BV and domain layers. | Enforce future/present **only on create**, or on update **only when `dueDate` changes**; an unchanged past `dueDate` must be accepted. Drop `@FutureOrPresent` from the update DTO. |
| `LocalDate.now()` in the domain (raw javadoc claimed UTC; call uses the JVM default zone) and no injectable clock. | Zone mismatch vs documented UTC; due-date tests become flaky or impossible to pin to a fixed “today”. | Inject a `Clock` (UTC) into use cases; pass `LocalDate.now(clock)` as `today` into `Task`. Tests use `Clock.fixed(...)`. Domain never calls `LocalDate.now()`. |

## What was already correct

| Checklist item | Raw sample |
|---|---|
| Ownership / IDOR (other user’s task id) | Loads via `findByIdAndOwnerUserId`; foreign id → `TaskNotFoundException`. Test `foreignTaskLooksMissing` asserts not-found and no `save`. |
| 403 vs 404 leak | Documented and implemented as **404**, never 403. |
| Entity exposed as API DTO | Separate `TaskResponse` / request records; JPA `TaskEntity` stays in infrastructure. |
| Blank title / max lengths | `@NotBlank` / `@Size` on DTOs; domain re-validates title (and description) length. Past `due_date` on **update** is **not** correct — see issues table. Create-time `@FutureOrPresent` is appropriate. |
| Status as enum | `TaskStatus` enum on DTOs; invalid values are a deserialization concern (handler still needed—see table). |
| Pagination max size (intent) | `@Max(ListTasksUseCase.MAX_PAGE_SIZE)` and use-case guard (`MAX_PAGE_SIZE = 50`). |
| Problem bodies avoid class names | Handlers set `title`/`detail`/`type` without `ex.getClass()`. |
| Version required on update | `UpdateTaskRequest.version` is `@NotNull`; mismatch → conflict exception. |
| Tests not only happy path | Includes foreign-owner and stale-version cases (still thin on HTTP edge cases). |
