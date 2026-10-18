# How GenAI was used to build this repository

**Requirement:** [GENAI] / [EVAL-GENAI] in `docs/requirements.md`.  
Facts below are taken from Spec Kit artifacts, `.cursor/agent-corrections.md`,
and `git` history. No invented metrics.

## Workflow

1. **Spec Kit** per feature under `specs/<nnn>-…/`:
   `specify` → `plan` → `tasks` → `analyze` → `implement` (plus clarify /
   checklist when needed).
2. **Cursor Agent** implements **one task (or RED/GREEN pair) at a time**
   against `tasks.md`, stopping for review; it does **not** commit.
3. **Claude Code** (and/or Miguel) reviews each stop; durable rules are appended
   to `.cursor/agent-corrections.md` with **Source: Miguel's review**, enforced
   by `.cursor/rules/agent-corrections.mdc`.
4. **Miguel** decides, amends scope when needed, and **makes every commit**.

The correction log is **feedback memory**: later agents must apply those rules
without re-learning the same failure from scratch. This file does **not** modify
that log.

## Concrete examples

### 1. Lost update closed with compare-and-set UPDATE

| | |
|---|---|
| **What the AI produced** | Optimistic update path that probed version, then re-loaded with a force-increment lock and applied changes — a window where a concurrent tags-only write could be overwritten. |
| **How it was caught** | Miguel’s review + concurrency-oriented persistence tests (`6fd39d2` RED expectations; force-concurrency scenario). |
| **Fix** | Single atomic JPQL CAS: `updateProprietaryIfVersion` (`SET …, version = version + 1 WHERE national_number = :n AND version = :expected`); 0 rows → stale. Commit **`834e028`**. Logged under “Optimistic update via compare-and-set UPDATE”. |

### 2. Caffeine + virtual threads: carrier pinning measured

| | |
|---|---|
| **What the AI produced** | Sync Caffeine `Cache` load doing blocking HTTP on Java 21 virtual threads inside the cache computation path. |
| **How it was caught** | Dedicated IT (`VirtualThreadCarrierPinningIT`): measured slowdown with few carriers (correction notes ~32×200ms loads ≈ 3264 ms with 2 carriers). |
| **Fix** | First moved toward `AsyncCache` (**`54084e2`**), then settled on sync `getIfPresent` / load / `put` **outside** the cache compute lock for deterministic expiry (**`36adb3c`**). Corrections: “Caffeine AsyncCache…” / “Caffeine getIfPresent/load/put”. |

### 3. `tailwind-merge` dropping custom font-size tokens

| | |
|---|---|
| **What the AI produced** | Default `twMerge` via `cn()`, so custom tokens like `text-display` were treated as conflicting with `text-foreground` and dropped. |
| **How it was caught** | UI review (PageHeading / badges looked wrong) + correction; contract test expectations in `utils.test.ts`. |
| **Fix** | `extendTailwindMerge` with custom tokens in the `font-size` group. Commit **`ff30d8c`** (implementation); rule logged in **`43ddc2b`**. |

### 4. Paginated list beyond last page → API 400 (not empty 200)

| | |
|---|---|
| **What the AI produced** | Frontend catalog treated “page past the end” like an empty **200** page. |
| **How it was caught** | Contract mismatch vs real API (`page N is beyond last page M` → **400**); domain rules already in **`2dd5861`** / list use cases; Miguel’s review on T022. |
| **Fix** | Frontend maps `ApiError` status 400 (when `page > 1`) to the beyond-last UI (empty + link to page 1). Correction “Paginated list beyond-last is API 400”; catalog GREEN **`ff30d8c`**, log **`43ddc2b`**. |

### 5. Unused `cn` / `radix-ui` dependencies removed

| | |
|---|---|
| **What the AI produced** | Extra npm dependencies pulled in during frontend scaffolding / shadcn setup that were not referenced. |
| **How it was caught** | Dependency review after tokens/shadcn work. |
| **Fix** | Drop unused packages from `package.json` / lockfile. Commit **`96da24c`**. |

### 6. Uncommitted tag text lost on Save

| | |
|---|---|
| **What the AI produced** | Edit form only added tags on Enter/Add; typing “Starter” and clicking Save omitted the draft from the PUT body. |
| **How it was caught** | Miguel’s review / manual edit flow; correction “Commit pending tag draft on edit Save”. |
| **Fix** | On submit, run the draft through the same commit/validation rules before building the body; invalid draft blocks save and focuses the tag field. Shipped in edit GREEN **`45d77aa`** (`edit-validation` / `EditForm` / `TagInput`). |

### 7. Password UTF-8 byte length (BCrypt 72) — avoid silent truncate / 500 paths

| | |
|---|---|
| **What the AI / naive BV produced** | Character-oriented `@Size` thinking; BCrypt’s **72-byte** limit is easy to miss. Oversized or invalid passwords then risk hasher surprises or domain `IllegalArgumentException` falling through to a catch-all **500**. |
| **How it was caught** | Auth spec/plan (`specs/006-auth`) + domain tests (**`53a5993`** RED) / GREEN VO (**`e5f3af1`**): `RawPassword.MAX_UTF8_BYTES = 72` with UTF-8 byte check; web `@MaxUtf8Bytes`. |
| **Fix** | Enforce max **72 UTF-8 bytes** in domain and HTTP validation; login path short-circuits &gt;72-byte passwords to 401 without hashing (per 006 plan). |

### 8. (Bonus) Problem details must not leak Jackson / type names

| | |
|---|---|
| **What the AI produced** | Error handlers that branched on exception **message text** or let malformed bodies surface framework wording. |
| **How it was caught** | Review + web tests expecting problem+json without leaks (**`5c97d90`** and related handler work). |
| **Fix** | Typed checks (`UnrecognizedPropertyException`, `MismatchedInputException` path) — never branch on library message strings. Correction “Never branch on library exception message text” / “Malformed-body no-leak”. |

## Takeaway for [EVAL-GENAI]

GenAI accelerated Spec Kit execution and boilerplate, but **acceptance came from
failing tests, ArchUnit, coverage gates, contract reads, Compose/`curl`/browser
checks, and human review** — with the correction log closing the loop so the
next task does not repeat the same defect.
