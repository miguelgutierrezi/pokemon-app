# How GenAI suggestions were validated and corrected

**Requirement:** [GENAI] / [EVAL-GENAI] in `docs/requirements.md`.  
This describes the **validation discipline used on Pokéfolio**, not claims about
the Task API scratch sample (that sample was not compiled or executed).

## 1. Tests first (RED → GREEN)

- Spec Kit tasks pair **RED** (failing test file only) with **GREEN** (production
  code). The suite must fail for the right reason before implementation lands.
- Post-review behaviour changes also get a failing test first when they alter
  contracts (e.g. beyond-last list → 400, pending tag committed on Save).

## 2. Run the suite and the coverage gate

- Backend: `./mvnw -B verify` (JaCoCo **≥ 80%** line coverage per module).
- Frontend: `npm run lint && npm run typecheck && npm test && npm run build`.
- Never treat an agent “all green” claim as proof — **re-run the gate locally
  (and watch CI)** after each GREEN task.

## 3. ArchUnit layering

- Package / dependency rules fail the build if `domain` depends on Spring/JPA
  or if modules depend “outward”.
- Generated “god classes” that pull infrastructure into domain are rejected by
  these tests, not by eyeballing alone.

## 4. Read generated code against the spec and contracts

- Compare controllers, problem types, and status codes to
  `specs/<feature>/spec.md`, `contracts/`, and OpenAPI expectations.
- Spec wins on documented conflicts (e.g. shared-collection copy vs Figma
  “my collection” wording).
- Correction log (`.cursor/agent-corrections.md`) records durable rules so the
  same mistake is not reintroduced.

## 5. Manual API checks (`curl`)

- Quickstart scripts exercise real HTTP against Compose or local Boot:
  pagination, 401/409, problem+json shapes, seed data.
- Frontend quickstart adds browser checks (Figma frames, console / SC-006).

## 6. Library versions and deprecations

- Compile with deprecation warnings enabled; prefer current Spring 7 / Jackson 3
  APIs (recorded as an agent correction).
- Pin GitHub Actions only after checking the action’s current major release —
  never from memory.

## 7. Dependencies the generator added

- Drop unused packages the scaffold or agent introduced (example: unused
  `cn` / `radix-ui` on the frontend — commit `96da24c`).
- Prefer the smallest set that the gate and ArchUnit still accept.

## 8. What we do **not** trust

| Claim | Required evidence |
|---|---|
| “Tests pass” | Fresh `verify` / `npm test` output |
| “Secure by default” | Ownership tests + Security filter-chain review |
| “Matches OpenAPI” | Contract test or manual `curl` + problem type check |
| “No lost updates” | Concurrent / CAS test, not only a unit stub |
| “Accessible dialog” | RTL + zero `console.warn`/`error` guard |
