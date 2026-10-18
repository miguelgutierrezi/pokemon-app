# Implementation Plan: Frontend (TR-FRONTEND)

**Branch**: `007-frontend` | **Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-frontend/spec.md`

## Summary

Deliver the Pokéfolio UI on the existing backend (002–006): catalog browse
(HU-01), detail (HU-02), shared collection import/list (HU-03), edit
(HU-04), delete (TR-API-CRUD / FE-CRUD), and login/register session
(TR-API-AUTH). Architecture follows constitution **VII** / **FE-ARCH**:
React 19 + Vite 8 + TypeScript 6 strict + Tailwind v4 + TanStack Query +
React Router data API + shadcn/ui (only needed primitives). Presentational
components never fetch; pages wire hooks; typed `fetch` client parses
RFC 7807 into `ApiError`. Vite **gains** an `/api` dev proxy (nginx
unchanged). **No backend changes.**

**Visual source of truth**: Figma
[PokeApp](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp)
(constitution Technology Stack). Desktop **and** mobile frame URLs are in
[contracts/ui-routes.md](./contracts/ui-routes.md). The file has **no**
Figma variables (`get_variable_defs` → `{}`); extract colors, fonts,
radii, shadows from `get_design_context` of catalog + detail frames into
Tailwind v4 `@theme` + shadcn CSS variables in **one place**
(`frontend/src/index.css`) — no hardcoded hex in components. Per screen:
`get_design_context` + `get_screenshot` on desktop **and** mobile before
implement; generated code is **reference only** — rebuild with shadcn,
tokens, semantic HTML; **follow mobile frames** (no desktop-only guess).
Where Figma conflicts with the **spec**, the **spec wins** (R11 conflicts
1–7).

Traceability: TR-FRONTEND, FE-INTEGRATION, FE-RESPONSIVE, FE-CRUD,
FE-ARCH, HU-01..HU-04, TR-API-AUTH, TR-API-CRUD. See research for
versions and rejected alternatives.

## Technical Context

**Language/Version**: TypeScript **~6.0** strict; React **19.2**; Node
**≥24** (`frontend/.nvmrc` / engines)

**Primary Dependencies** (constitution **IX** — first use in 007; pin at
implement after re-check — see [research.md](./research.md)):

| Library | Verified (plan date) | Notes |
|---|---|---|
| Existing: Vite **8.3**, Tailwind **4.3**, Vitest **5**, Testing Library | already in skeleton | keep |
| `@tanstack/react-query` | **5.104.4** | `placeholderData: keepPreviousData` (v5 API) |
| `react-router` | **8.4.0** | `createBrowserRouter` + `RouterProvider` from `react-router` (not v6; not separate `react-router-dom`) |
| shadcn/ui CLI + selected components | CLI **4.22.0** | button, input, label, card, badge, skeleton, alert, alert-dialog, pagination + Radix/lucide/`cn` deps only |
| No form lib / MSW / Query Devtools / extra client state libs | — | IX + spec |

**Storage**: Browser `localStorage` key `pokefolio.session` (token,
`expiresAt`, user). XSS trade-off documented in research (MVP).

**Testing**: Vitest + Testing Library; `vi.mock` of `src/api`; cover
problem parsing, session, return-path sanitiser, page states, forms,
and protected-write **401** → session-expired **dialog** (Sign in again →
`/login?redirect=<sanitised>&reason=expired`; Browse catalog → `/`) per
FR-009. CI frontend job **unchanged** (lint, typecheck, test, build,
nginx smoke).

**Target Platform**: SPA in Docker (nginx :3000) and Vite dev (:5173);
same-origin `/api` to backend :8080.

**Project Type**: Frontend feature in existing monorepo (`frontend/`).

**Performance Goals**: Catalog `staleTime` 5 min; pagination without
flicker (`keepPreviousData`); images `loading="lazy"` + dimensions.

**Constraints**:
- Zero browser console warnings/errors on every screen/state (hard)
- No deprecated APIs; re-verify docs at implement
- Public GETs never send Bearer; protected writes only
- Return path: relative `/…`, not `//` (no open redirect)
- Shared-collection copy (not “my/your”)
- Collection + catalog: `?page=N` 1-based, size 9; beyond-last → empty +
  link to page 1
- Presentational components must not import fetching hooks
- No backend / OpenAPI / Flyway changes
- Figma = visual SoT; **spec wins** on copy/flows/states (R11 conflicts
  1–7): shared-collection wording; detail `GET /collection/{id}` for
  saved/not-saved; import confirm→progress→success/409; session-expired
  dialog; 403 not implemented; abilities name + Hidden only; demo hint
- Design tokens only via `src/index.css` `@theme` / shadcn CSS vars
  (extracted from catalog/detail `get_design_context` — no Figma
  variables) — **no hex literals in components**
- Pokémon images from API (`spriteUrl` / `imageUrl`) only — never Figma
  exports; icons from `lucide-react` matching the design
- Figma MCP output is reference — no absolute layout / fixed px widths
  from frames; follow mobile frames; catalog/collection **1 / 2 / 3**
  columns

**Scale/Scope**: Six screens + shared layout/states; client types mirroring
existing DTOs; no new mocking stack.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I–VI, X (backend) | N/A for UI-only feature | N/A — no backend changes; consume 002–006 |
| **VII. Frontend Standards** | React/TS/Vite; TanStack Query for server state; presentational components do not fetch; responsive + a11y; **zero console warnings**; UI designed in Figma | **PASS** — structure, Query, a11y, zero-warnings (R9); Figma SoT + tokens (R11) |
| **VIII. Quality Gates** | CI green; one branch/PR `007-frontend` | **PASS** — existing frontend job unchanged; branch matches `specs/007-frontend/` |
| **IX. Simplicity** | Deps only on first use; YAGNI | **PASS** — R1 dependency list; no form lib/MSW/devtools |
| **X. Requirements Traceability** | Spec cites HU/TR/FE IDs; no new/renamed HUs | **PASS** — [spec.md](./spec.md) traceability table |

**Post-design re-check**: PASS — contracts and data-model stay within
existing API; Vite proxy addition is frontend-only (FR-013).

## Project Structure

### Documentation (this feature)

```text
specs/007-frontend/
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1 — client types + session
├── quickstart.md        # Phase 1 — manual compose walkthrough
├── contracts/
│   ├── ui-routes.md     # route → page → API → states
│   └── api-client.md    # auth header policy + error mapping
├── checklists/
│   └── requirements.md
└── tasks.md             # NOT created by /speckit-plan (use /speckit-tasks)
```

### Source Code (repository root)

```text
frontend/
├── components.json          # shadcn (Tailwind v4: empty tailwind.config)
├── vite.config.ts           # + @ alias; + server.proxy /api → backend
├── tsconfig.json            # + paths @/*
├── tsconfig.app.json        # + paths @/*
├── nginx.conf               # UNCHANGED (/api/ → backend:8080, no rewrite)
├── Dockerfile               # UNCHANGED
└── src/
    ├── main.tsx             # StrictMode; mount
    ├── index.css            # ONLY place for design tokens: Tailwind v4
    │                        # @theme + shadcn CSS vars (from catalog/
    │                        # detail get_design_context; no Figma vars);
    │                        # no hex in components
    ├── app/
    │   ├── providers.tsx    # QueryClientProvider, AuthProvider
    │   ├── router.tsx       # createBrowserRouter
    │   ├── layout.tsx       # nav, landmarks, aria-live, h1 focus
    │   └── not-found.tsx
    ├── api/
    │   ├── client.ts        # fetch + ApiError + field extract
    │   ├── types.ts         # DTO mirrors
    │   ├── auth.ts
    │   ├── pokemon.ts
    │   └── collection.ts
    ├── features/
    │   ├── catalog/         # hooks + presentational cards/grid
    │   ├── pokemon/         # detail + evolution + add action UI
    │   ├── collection/      # list + edit form + delete dialog UI
    │   └── auth/            # login/register forms + session hooks UI
    ├── pages/               # containers only
    │   ├── CatalogPage.tsx
    │   ├── PokemonDetailPage.tsx
    │   ├── CollectionPage.tsx
    │   ├── CollectionEditPage.tsx
    │   ├── LoginPage.tsx
    │   └── RegisterPage.tsx
    ├── components/ui/       # shadcn primitives only
    └── lib/
        ├── utils.ts         # cn()
        ├── session.ts       # pure localStorage session store
        └── return-path.ts   # sanitizeReturnPath
```

**Structure Decision**: Feature-sliced `features/*` for hooks + dumb UI;
`pages/*` as containers; `api/*` as the sole HTTP boundary (mocked in
tests). Matches spec FR-014 and research R2. Backend tree untouched.

## Complexity Tracking

> No constitution violations requiring justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

**Documented trade-off (not a violation)**: session token in `localStorage`
(XSS-readable) vs memory-only or httpOnly cookie — see research R4; MVP
choice; no backend cookie work in 007.

## Phase 0 & 1 outputs

| Artifact | Path |
|---|---|
| Research | [research.md](./research.md) |
| Client data model | [data-model.md](./data-model.md) |
| UI routes contract | [contracts/ui-routes.md](./contracts/ui-routes.md) |
| API client contract | [contracts/api-client.md](./contracts/api-client.md) |
| Quickstart | [quickstart.md](./quickstart.md) |

## Implementation notes (for later `/speckit-tasks`)

Suggested ordering (not tasks.md):

1. Tooling: `@/` alias, Vite `/api` proxy, shadcn init + listed components.
2. Figma tokens: from catalog/detail `get_design_context` → map into
   `src/index.css` (`@theme` + shadcn vars). Frame URLs already in
   `contracts/ui-routes.md`.
3. `api` client + `ApiError` + field parser tests.
4. Session store + AuthProvider + return-path tests.
5. QueryClient defaults (retry; 401 → session-expired dialog) + router
   shell/layout.
6. Per screen: desktop + mobile `get_design_context` + `get_screenshot`
   → rebuild with shadcn/tokens (Catalog → Detail with collection GET →
   Auth → Import dialogs → Collection → Edit/Delete).
7. A11y + zero-warnings; quickstart visual check vs Figma frames; CI green.

**TDD**: RED tests at api/session/pages boundaries before UI fill-in;
presentational components tested with props only.
