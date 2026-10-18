# Raw GenAI output — provenance

| Field | Value |
|---|---|
| **Tool** | Cursor Agent (`generalPurpose` subagent) in an isolated scratch directory (`/tmp/genai-task-api-scratch`) with instructions not to read the Pokéfolio repository |
| **Model** | Inherited from the parent Cursor session (Composer family; slug not separately selected) |
| **Date (UTC)** | 2026-10-18T05:55:53Z |
| **Prompt** | [`docs/genai/01-prompt.md`](../../01-prompt.md) (body below the horizontal rule) |
| **Editing** | Files in this directory are **unedited** copies of the generator output |

## Files (representative sample)

| File | Role |
|---|---|
| `Task.java` | Domain aggregate |
| `CreateTaskRequest.java` / `UpdateTaskRequest.java` | Request DTOs |
| `TaskController.java` | HTTP adapter |
| `UpdateTaskUseCase.java` | Application use case (ownership + version check) |
| `TaskRepository.java` | Persistence port |
| `UpdateTaskUseCaseTest.java` | Focused tests (ownership + stale version) |
| `PLAN.md` | Short plan emitted before the code |
| `META.md` | Generator-written metadata |

Supporting types the sample imports (also unedited) live under `_support/` for review
context only (`TaskStatus`, `TaskResponse`, `TaskExceptionHandler`, JPA sketch, other use cases).
