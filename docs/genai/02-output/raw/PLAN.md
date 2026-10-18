# Task Management REST API — Scaffold Plan

## Modules (Clean Architecture, inward deps only)

| Module | Responsibility |
|--------|----------------|
| `domain` | `Task` aggregate, `TaskStatus`, invariants; no Spring/JPA |
| `application` | Use cases (`CreateTask`, `UpdateTask`, `GetTask`, `ListTasks`, `DeleteTask`), ports (`TaskRepository`), ownership rules |
| `infrastructure` | JPA entity/adapter, Flyway, Spring Data, JWT resource-server wiring (auth already exists) |
| `web` | Controllers, request/response DTOs, OpenAPI, `ProblemDetail` advice |

## Endpoints (`/api/v1`)

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/tasks` | Create (owner = authenticated `userId`) |
| `GET` | `/api/v1/tasks` | List own tasks; paginated (`page`, `size`; max `size` = 50) |
| `GET` | `/api/v1/tasks/{id}` | Read own task |
| `PUT` | `/api/v1/tasks/{id}` | Update; requires `version` |
| `DELETE` | `/api/v1/tasks/{id}` | Delete own task |

## Ownership rule

Every read/update/delete loads by `(id, ownerUserId)`. If the task exists for another user or is missing → **404 Not Found** (never 403). List always filters by authenticated `userId`.

## Validation & due-date policy

- `title`: required, non-blank, max 200 chars
- `description`: optional, max 2000 chars
- `status`: enum `TODO | IN_PROGRESS | DONE` (invalid → 400)
- `due_date`: `LocalDate` (JSON date); optional on create; if present, must be **today or in the future** (UTC date at validation time)
- `version`: required on update; optimistic lock → **409** on mismatch

## Error shapes (RFC 7807)

`Content-Type: application/problem+json` via Spring `ProblemDetail`:

- `type` — stable URI (e.g. `https://api.example.com/problems/not-found`, `.../conflict`, `.../validation`)
- `title`, `status`, `detail` — human-readable; **no** exception class names or stack traces
- Validation failures: 400 with field errors in `properties.errors`
- Page size > max: 400 (`page-size-exceeded`)
- Version mismatch: 409 (`optimistic-lock`)

## TDD order

1. Failing ownership / optimistic-lock test
2. Domain + use case production code
3. Controller / DTO / repository port samples
