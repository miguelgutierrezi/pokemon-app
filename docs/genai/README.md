# GenAI deliverable ([GENAI] / [EVAL-GENAI])

Index for the Ballast Lane GenAI requirement (`docs/requirements.md`).
**Docs only** — nothing here is on the application classpath or in any build.

## Parts

| # | Document | Contents |
|---|---|---|
| 1 | [`01-prompt.md`](01-prompt.md) | Prompt used to generate a Task Management REST API (stack, constraints, why each constraint exists). |
| 2 | [`02-output/raw/`](02-output/raw/) | Unedited representative output from running that prompt in an isolated Cursor agent scratch (`RAW.md` = tool / model / date). |
| 3 | [`02-output/reviewed/`](02-output/reviewed/) | Same sample after critical review + [`REVIEW.md`](02-output/reviewed/REVIEW.md) (issue → why → fix; only real defects). |
| 4 | [`03-validation.md`](03-validation.md) | How Pokéfolio validates and corrects GenAI suggestions (TDD, gates, ArchUnit, contracts, curl, deps). |
| 5 | [`04-this-project.md`](04-this-project.md) | How GenAI was used on **this** repo (Spec Kit, Cursor, review, Miguel commits) with concrete git/correction examples. |

## Five-line summary

1. We prompt with **context first**, then hard constraints tied to real failure modes (ownership 404, CAS versioning, ProblemDetail, max page size).  
2. We **ran** that prompt in an isolated Cursor agent and kept the raw files unedited.  
3. Review found real gaps (in-memory optimistic lock, unhandled `ConstraintViolationException`, missing security wiring)—not a rubber stamp.  
4. On Pokéfolio, GenAI only ships after RED→GREEN, Maven/npm gates, ArchUnit, and human review.  
5. Durable mistakes live in `.cursor/agent-corrections.md` so agents do not repeat them.
