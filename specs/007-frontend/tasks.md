---
description: "Task list for feature 007-frontend (TR-FRONTEND)"
---

# Tasks: Frontend (TR-FRONTEND)

**Input**: Design documents from `/specs/007-frontend/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/,
quickstart.md

**Tests**: Required — constitution **II** (TDD, visible red/green). Every
production change is preceded by a **RED** task (failing test for a stated
reason); the agent **shows the failing output**, then a **GREEN** task
implements. Tests and implementation MUST live in **separate files** so
commits can be split by path. Exceptions: pure tooling/config with no
behaviour under test (alias, proxy, token CSS map, shadcn generate) and
**User action** tasks.

**Organization**: Labels match scenarios — **`[HU01]`**, **`[HU02]`**,
**`[HU03]`**, **`[HU04]`**, **`[AUTH]`**, **`[FECRUD]`**, **`[XCUT]`** —
never `[USn]` (constitution X). Setup/foundation use no story label.

**Conventions**: sequential RED→GREEN pairs; ⏸ Review + Conventional Commit
message per task; **no RED pushes**; **the agent never commits** — each
task ends with stop for review/commit; the user makes all commits. We
review RED+GREEN pairs together after GREEN.

**Figma**: [PokeApp](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp)
— frame URLs in each screen task and in
[contracts/ui-routes.md](./contracts/ui-routes.md). Spec wins on conflicts
1–7 (research R11).

## Format: `[ID] [P?] [Label] Description`

- **[P]**: Can run in parallel (different files, no incomplete deps) — rare
- **[HUxx]/ / **[AUTH]** / **[FECRUD]** / **[XCUT]**: story / cross-cut
- **⏸ Review**: stop; user reviews and commits. Suggested message given.
  RED states are not pushed.

## Path Conventions

- Frontend: `frontend/src/...`, `frontend/vite.config.ts`,
  `frontend/tsconfig*.json`, `frontend/components.json`
- Root: `README.md`, `docker-compose.yml`
- Specs: `specs/007-frontend/`

## Global rules for every task

1. **Before starting**: run `git status`. If the previous task’s reviewed
   changes are still uncommitted, **STOP** and ask the user to commit first.
2. **Constitution IX**: add a dependency only inside the task that first
   uses it (versions re-verified at install; research R1 majors).
   **Forbidden**: react-hook-form, zod, formik, MSW, Query Devtools,
   Zustand/Redux/Jotai, `react-router-dom` package, axios, hex literals in
   TSX, Pokémon art exported from Figma, implementing the **403** frames,
   backend/OpenAPI/Flyway changes.
3. **RED**: named test(s) MUST fail for the stated reason; capture/show
   failing output; do **not** push; do **not** implement production code
   in the RED task (test file only).
4. **GREEN**: implement only the production file(s) for that task; before
   marking `[x]`:
   `cd frontend && npm run lint && npm run typecheck && npm test && npm run build`
   must pass (CI must never be red between pushes once the draft PR
   exists).
5. Presentational components under `features/*/components` MUST NOT import
   `useQuery` / `useMutation` / `api/*`. Pages wire hooks.
6. Shared-collection copy only (**Collection**, **Add to collection**,
   **Already in the collection**, **Saved in the shared collection**).
7. Once the draft PR exists (T004), every **push** keeps CI green (no RED
   pushes).

---

## Phase 1: Tooling

**Purpose**: Path alias, Vite `/api` proxy, zero-warnings test guard, draft
PR.

**Independent Test**: `npm run typecheck` resolves `@/`; proxy config
present; a deliberate `console.error` in a throwaway test fails the suite
when guard is on; draft PR open.

- [x] T001 Configure `@/` path alias in
  `frontend/tsconfig.app.json` (`compilerOptions.paths`: `@/*` → `./src/*`),
  `frontend/tsconfig.json` if needed for tooling, and
  `frontend/vite.config.ts` (`resolve.alias` via `path` / `fileURLToPath`).
  Confirm `npm run typecheck` green. No new runtime deps.
  ⏸ Review — `chore(frontend): add @ path alias`

- [x] T002 Add Vite `/api` dev proxy in `frontend/vite.config.ts`:
  `server.proxy['/api']` → `process.env.VITE_API_PROXY_TARGET ??
  'http://localhost:8080'`, `changeOrigin: true`, **no** path rewrite.
  Document `VITE_API_PROXY_TARGET` in README later (T041) — not
  `.env.example` now. Confirm `npm run lint && npm run typecheck && npm
  test && npm run build` green.
  ⏸ Review — `chore(frontend): vite /api dev proxy`

- [x] T003 Fail Vitest on any `console.warn` / `console.error` in
  `frontend/src/test/setup.ts` (spy + `afterEach` / global listeners that
  `expect` zero calls, or equivalent that fails the current test). Confirm
  an intentional console call in a temporary assertion fails, then remove
  that probe so the suite stays green. Keep
  `npm run lint && npm run typecheck && npm test && npm run build` green.
  ⏸ Review — `test(frontend): fail suite on console warn/error`

- [x] T004 **User action**: ensure branch `007-frontend` exists; commit any
  agreed 007 docs/tooling tip; push and open a **draft** PR to `main`
  (`gh pr create --draft --base main`). Confirm CI green on the tip. If
  red, fix before continuing. ⏸ Review

**Checkpoint**: Tooling ready; draft PR open; CI green.

---

## Phase 2: Design tokens + shadcn (foundation visuals)

**Purpose**: Map Figma catalog/detail tokens into `src/index.css`; init
shadcn with only components needed before screens.

**Figma**: catalog desktop
[8-3955](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-3955);
detail saved
[8-7626](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7626).
`get_variable_defs` is `{}` — extract from `get_design_context` (research
R11).

**Independent Test**: CSS variables / `@theme` present; `cn()` works;
`button` importable; build green.

- [x] T005 Define design tokens once in `frontend/src/index.css`: Tailwind
  v4 `@theme` + shadcn CSS variables from catalog `8:3955` + detail
  `8:7626` (colors `#182126`, `#c83738`, `#f5f7f8`, surfaces/borders/muted,
  Inter + IBM Plex Mono, radii 6/10/12). No hex in later TSX. Fonts via
  project-appropriate import (e.g. Google/fontsource) only if required for
  build. Confirm lint/typecheck/test/build green.
  ⏸ Review — `style(frontend): figma tokens in index.css`

- [x] T006 Init shadcn (Tailwind v4) into `frontend/` (`components.json`,
  `frontend/src/lib/utils.ts` `cn`) and add **only** components needed so
  far: `button` (pulls `class-variance-authority`, `clsx`,
  `tailwind-merge`, `@radix-ui/react-slot`, `lucide-react` as CLI
  requires — constitution IX). Do **not** add card/input/dialog yet.
  Confirm lint/typecheck/test/build green.
  ⏸ Review — `chore(frontend): shadcn init + button`

**Checkpoint**: Tokens + minimal shadcn ready.

---

## Phase 3: API client + ApiError (foundation)

**Goal**: Typed `fetch` client, problem+json → `ApiError`, field extraction
(`parameter 'x'` / `property 'x'`). Public GETs never send Bearer;
protected writes attach Bearer only when session valid.

**Independent Test**: Unit tests for parse + field + auth header policy
with `vi.mock` / mocked `fetch`; no MSW.

- [x] T007 RED: write
  `frontend/src/api/api-error.test.ts` and
  `frontend/src/api/client.test.ts` — cover problem+json → `ApiError`;
  field from `parameter 'password'` and `property 'version'`; no field;
  network/`status: 0`; auth header never on GET pokemon/collection or
  auth login/register; Bearer on POST/PUT/DELETE collection when token
  provided. Confirm fail (modules missing). Show failing output. Do not
  push. Test files only.
  ⏸ Review — `test(frontend): expect api error client parsing (red)`

- [x] T008 GREEN: implement `frontend/src/api/api-error.ts`,
  `frontend/src/api/types.ts` (DTO mirrors from data-model.md),
  `frontend/src/api/client.ts`, and thin endpoint modules
  `frontend/src/api/pokemon.ts`, `frontend/src/api/collection.ts`,
  `frontend/src/api/auth.ts` as needed by tests. T007 green;
  lint/typecheck/test/build green.
  ⏸ Review — `feat(frontend): api client ApiError problem+json (green)`

**Checkpoint**: API boundary ready and mocked in tests.

---

## Phase 4: Session + return path + AuthProvider (foundation)

**Goal**: `sanitizeReturnPath`; `pokefolio.session` store; React
`AuthProvider`.

**Independent Test**: Pure unit tests for sanitiser + session; provider
test with RTL.

- [x] T009 RED: write
  `frontend/src/lib/return-path.test.ts` — accept `/`, `/pokemon/1`,
  `/collection?page=2`; reject `//evil`, absolute URLs, empty → `/`.
  Confirm fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect return path sanitiser (red)`

- [x] T010 GREEN: implement `frontend/src/lib/return-path.ts`. T009 green;
  full frontend gate green.
  ⏸ Review — `feat(frontend): sanitizeReturnPath (green)`

- [x] T011 RED: write `frontend/src/lib/session.test.ts` — persist
  `{ accessToken, expiresAt, user }`; clear corrupt/expired on load;
  logout clears; `storage` event sync behaviour as research R4. Confirm
  fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect session store (red)`

- [x] T012 GREEN: implement `frontend/src/lib/session.ts`. T011 green;
  full frontend gate green.
  ⏸ Review — `feat(frontend): pokefolio.session store (green)`

- [x] T013 RED: write
  `frontend/src/features/auth/AuthProvider.test.tsx` — provides signed-in /
  signed-out state from session; login/logout updates consumers. Confirm
  fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect AuthProvider (red)`

- [x] T014 GREEN: implement
  `frontend/src/features/auth/AuthProvider.tsx` (and minimal hooks/context
  exports under `frontend/src/features/auth/`). T013 green; full gate
  green.
  ⏸ Review — `feat(frontend): AuthProvider (green)`

**Checkpoint**: Session layer ready for protected writes and login return.

---

## Phase 5: QueryClient + router + layout + not-found (foundation)

**Goal**: TanStack Query defaults (retry; 401 → session-expired signal);
React Router 8 data router; layout shell (nav, landmarks, h1 focus,
aria-live); not-found page.

**Figma not-found**: desktop
[8-10806](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10806)
/ mobile
[8-10853](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10853).
Nav labels: **Pokémon catalog**, **Collection** (not “My collection”).

**Independent Test**: Not-found route renders; layout landmarks present;
retry/401 wiring unit-tested; `npm test` + build green.

- [x] T015 RED: write
  `frontend/src/app/query-client.test.ts` — assert a pure config /
  defaults factory (no `@tanstack/react-query` package in this task):
  retry never on 4xx `ApiError`; at most once on `status === 0` or
  **502**; default `staleTime` **5 minutes**; pagination uses
  `placeholderData: keepPreviousData` (document the intended Query
  option shape); 401 handler clears session and raises session-expired
  UI signal (dialog host stubbed). Confirm fail. Show failing output.
  Test file only — **do not** add the dependency here (U4 / IX → T016).
  ⏸ Review — `test(frontend): expect query client retry 401 (red)`

- [x] T016 GREEN: add `@tanstack/react-query` (re-verify latest 5.x —
  first use); implement `frontend/src/app/query-client.ts` with those
  defaults (`staleTime` 5 min, `placeholderData: keepPreviousData` for
  list queries, retry policy, MutationCache 401 → session-expired
  signal) + helper under `frontend/src/features/auth/` or
  `frontend/src/app/` as needed. T015 green; full gate green.
  ⏸ Review — `feat(frontend): QueryClient retry and 401 signal (green)`

- [x] T017 RED: write
  `frontend/src/app/not-found.test.tsx` — unknown route shows not-found
  UI with link home; accessible heading. Confirm fail. Show failing
  output. Test file only.
  ⏸ Review — `test(frontend): expect not-found page (red)`

- [x] T018 GREEN: add `react-router` (re-verify latest 8.x; **not**
  `react-router-dom`); implement `frontend/src/app/not-found.tsx`,
  `frontend/src/app/router.tsx` (`createBrowserRouter` +
  `RouterProvider`), wire `frontend/src/main.tsx` /
  `frontend/src/app/providers.tsx` (QueryClientProvider + AuthProvider).
  Rebuild/replace skeleton `App.tsx` as needed so routes work. T017
  green; full gate green. Run `get_design_context` + `get_screenshot` for
  8-10806 / 8-10853 as reference before styling.
  ⏸ Review — `feat(frontend): router and not-found page (green)`

- [x] T019 RED: write `frontend/src/app/layout.test.tsx` — `header` /
  `main` / `nav`; brand; nav **Pokémon catalog** + **Collection**;
  `aria-live="polite"` region; focuses page `h1` on route change
  (simulate navigation); when signed in, account control exposes
  **Logout** that clears the session **client-side** (assert **no**
  network/logout API call) and **stays on the current page** when that
  page is publicly readable (e.g. catalog or collection). Confirm fail.
  Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect app layout shell (red)`

- [x] T020 GREEN: implement `frontend/src/app/layout.tsx` from Figma nav
  chrome (tokens + `button`; lucide icons). Shared-collection nav copy;
  signed-in account control + Logout per T019. T019 green; full gate
  green.
  ⏸ Review — `feat(frontend): app layout landmarks nav (green)`

**Checkpoint**: Shell ready; screens can mount under layout.

---

## Phase 6: HU-01 — Catalog list 🎯 MVP

**Goal**: Paginated catalog grid `/?page=N`, size 9; loading / empty
beyond-last / 502+retry / network connection-error; abilities name +
Hidden label only.

**Figma**: desktop
[8-3955](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-3955)
/ mobile
[8-4385](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-4385).
Network/connection-error visual reference:
[8-19215](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19215)
/
[8-19257](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19257).

**Independent Test**: Mock `api` → page states; URL `?page=2`; beyond-last
empty + link page 1; invalid page → replace to page 1 without API.

- [x] T021 [HU01] RED: write
  `frontend/src/pages/CatalogPage.test.tsx` — loading skeletons; success
  grid (sprite alt, name, category, weight 1 decimal, abilities name /
  Hidden); `?page=2`; while paginating, previous grid stays visible
  (`placeholderData: keepPreviousData`); beyond-last empty + link to
  page 1; invalid `page` (`0`, negative, non-numeric) →
  `navigate(..., { replace: true })` to page 1 **without** calling the
  API; **502** + retry (upstream copy); network `status === 0` →
  connection-error state + retry (distinct from 502 if copy differs).
  `vi.mock` api. Confirm fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect catalog page states (red)`

- [x] T022 [HU01] GREEN: add shadcn `card`, `badge`, `skeleton`,
  `pagination`, `alert` (IX — first use). Implement
  `frontend/src/features/catalog/` hooks (`staleTime` 5 min +
  `placeholderData: keepPreviousData`) + presentational components and
  `frontend/src/pages/CatalogPage.tsx`; register `/` route; images with
  explicit `width`/`height` and `loading="lazy"`. Follow desktop+mobile
  frames (reference only). T021 green; full gate green.
  ⏸ Review — `feat(frontend): catalog page HU-01 (green)`

**Checkpoint**: HU-01 independently demoable.

---

## Phase 7: HU-02 — Pokémon detail

**Goal**: Detail view; types badges; proactive
`GET /api/v1/collection/{id}` for saved/not-saved; abilities name +
Hidden only.

**Figma**: not saved
[8-7212](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7212)
/
[8-7421](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7421);
saved
[8-7626](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7626)
/
[8-7858](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7858).

**Independent Test**: Mock pokemon + collection GETs → not-saved vs saved
copy; catalog 404; 502+retry.

- [x] T023 [HU02] RED: write
  `frontend/src/pages/PokemonDetailPage.test.tsx` — success detail
  (stats+total, flavor, **types as badges**, evolution sequence vs list
  rule); abilities no descriptions; collection **404** → **Add to
  collection**; collection **200** → **Already in the collection** /
  **Saved in the shared collection**; catalog **404**; **502**+retry.
  Confirm fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect pokemon detail states (red)`

- [x] T024 [HU02] GREEN: implement
  `frontend/src/features/pokemon/` +
  `frontend/src/pages/PokemonDetailPage.tsx`; route `/pokemon/:id`;
  primary image with `width`/`height` and `loading="lazy"`. Figma
  reference desktop+mobile. T023 green; full gate green.
  ⏸ Review — `feat(frontend): pokemon detail HU-02 (green)`

**Checkpoint**: HU-02 independently demoable (auth next, then import).

---

## Phase 8: TR-API-AUTH — Login + Register

**Goal**: Login with demo hint + `reason=expired` banner; register;
return-path; signed-in visitors redirected away from auth forms.

**Figma**: login
[5-233](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-233)
/
[5-515](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-515);
register
[5-921](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-921)
/
[5-1019](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-1019).
Demo hint is **spec wins** (frames omit it).

**Independent Test**: Form validation + mocked auth API; redirect
sanitised; signed-in bypass.

- [x] T025 [AUTH] RED: write
  `frontend/src/pages/LoginPage.test.tsx` — demo credential hint;
  `reason=expired` shows **Your session expired** above the form;
  success → sanitised redirect; **Invalid email or password**; link to
  register; when already signed in → redirect to sanitised `redirect` or
  `/` (form not shown). Confirm fail. Show failing output. Test file
  only.
  ⏸ Review — `test(frontend): expect login page (red)`

- [x] T026 [AUTH] GREEN: add shadcn `input`, `label` (IX). Implement
  `frontend/src/features/auth/` login form +
  `frontend/src/pages/LoginPage.tsx`; route `/login` including
  **Your session expired** when `reason=expired`. T025 green; full gate
  green.
  ⏸ Review — `feat(frontend): login page (green)`

- [x] T027 [AUTH] RED: write
  `frontend/src/pages/RegisterPage.test.tsx` — valid register → session +
  redirect; **400** field inline; **409** account exists; client password
  8 chars / 72 UTF-8 bytes; when already signed in → redirect to
  sanitised `redirect` or `/`. Confirm fail. Show failing output. Test
  file only.
  ⏸ Review — `test(frontend): expect register page (red)`

- [x] T028 [AUTH] GREEN: implement register form +
  `frontend/src/pages/RegisterPage.tsx`; route `/register`. T027 green;
  full gate green.
  ⏸ Review — `feat(frontend): register page (green)`

**Checkpoint**: Auth screens ready before import.

---

## Phase 9: HU-03 — Import dialogs

**Goal**: Import flow confirmation → in-progress → success (link) or 409
dialog; guest → login with redirect.

**Figma**: confirm
[8-18699](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18699)
/
[8-18738](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18738);
progress
[8-18777](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18777)
/
[8-18817](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18817);
success
[8-18857](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18857)
/
[8-18899](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18899);
409
[8-18941](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18941)
/
[8-18983](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18983).

**Independent Test**: Mock mutation states through dialogs; signed-out
navigates to `/login?redirect=…`.

- [x] T029 [HU03] RED: write
  `frontend/src/features/pokemon/ImportFlow.test.tsx` (or
  `frontend/src/pages/PokemonDetailPage.import.test.tsx`) — signed-out →
  login redirect; signed-in confirm → progress → success link; **409**
  dialog **Already in the collection**. Confirm fail. Show failing
  output. Test file only.
  ⏸ Review — `test(frontend): expect import dialogs flow (red)`

- [x] T030 [HU03] GREEN: add shadcn `alert-dialog` (IX). Implement import
  dialogs under `frontend/src/features/pokemon/` wired from detail page;
  invalidate collection queries on success. T029 green; full gate green.
  ⏸ Review — `feat(frontend): import dialogs HU-03 (green)`

**Checkpoint**: Import path complete.

---

## Phase 10: HU-03 / HU-04 — Collection list

**Goal**: Public paginated collection; loading / empty / connection error /
beyond-last; write actions login-gated.

**Figma**: list
[8-9535](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9535)
/
[8-9713](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9713);
loading
[8-19025](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19025)
/
[8-19084](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19084);
empty
[8-19136](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19136)
/
[8-19176](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19176);
connection error
[8-19215](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19215)
/
[8-19257](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19257).

**Independent Test**: Mock list page states; `?page=N`; no Bearer on GET.

- [x] T031 [HU03] RED: write
  `frontend/src/pages/CollectionPage.test.tsx` — loading; success with
  proprietary fields; empty; network `status === 0` → connection-error +
  retry (Figma 8-19215/8-19257; distinct from **502** copy if design
  differs); beyond-last + link page 1; invalid `page` (`0`, negative,
  non-numeric) → `navigate(..., { replace: true })` to page 1 **without**
  API call; signed-out edit/delete → login redirect. Confirm fail. Show
  failing output. Test file only.
  ⏸ Review — `test(frontend): expect collection page states (red)`

- [x] T032 [HU03] GREEN: implement
  `frontend/src/features/collection/` list UI +
  `frontend/src/pages/CollectionPage.tsx`; route `/collection`; list
  query uses `staleTime` / `placeholderData: keepPreviousData` as
  applicable. Figma desktop+mobile. T031 green; full gate green.
  ⏸ Review — `feat(frontend): collection list page (green)`

**Checkpoint**: Collection browse ready.

---

## Phase 11: HU-04 — Edit local fields

**Goal**: Edit proprietary fields; no save confirmation modal; **400**
inline; **409** stale → **Modified elsewhere — reload**; record **404**.

**Figma**: edit
[8-10425](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10425)
/
[8-10536](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10536);
400
[8-19377](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19377)
/
[8-19459](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19459);
404
[8-19298](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19298)
/
[8-19338](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19338).

**Independent Test**: Mock GET/PUT; stale + validation paths.

- [ ] T033 [HU04] RED: write
  `frontend/src/pages/CollectionEditPage.test.tsx` — load form with
  `version`; save success no confirm modal; **400** field;
  **409** stale-version copy + reload; **404** record. Client limits:
  localizedName ≤ 100; location ≤ 200; ≤ 20 tags; each ≤ 32 trim. Confirm
  fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect collection edit page (red)`

- [ ] T034 [HU04] GREEN: implement edit form under
  `frontend/src/features/collection/` +
  `frontend/src/pages/CollectionEditPage.tsx`; route `/collection/:id`.
  T033 green; full gate green.
  ⏸ Review — `feat(frontend): collection edit HU-04 (green)`

**Checkpoint**: HU-04 edit complete.

---

## Phase 12: FE-CRUD — Delete

**Goal**: Delete confirmation dialog (title + description) → **204** →
`/collection`.

**Figma**: remove
[8-9889](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9889)
/
[8-9923](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9923).

**Independent Test**: Confirm deletes; cancel keeps record.

- [ ] T035 [FECRUD] RED: write
  `frontend/src/features/collection/DeleteLocalPokemonDialog.test.tsx`
  — dialog title+description; confirm → success navigates `/collection`;
  cancel → no delete. Confirm fail. Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect delete dialog (red)`

- [ ] T036 [FECRUD] GREEN: implement
  `frontend/src/features/collection/DeleteLocalPokemonDialog.tsx` wired
  from edit (and list if applicable). T035 green; full gate green.
  ⏸ Review — `feat(frontend): delete local pokemon dialog (green)`

**Checkpoint**: FE-CRUD delete complete.

---

## Phase 13: Cross-cutting — Session-expired dialog

**Goal**: On protected-write **401**, show Figma dialog; **Sign in again**
→ `/login?redirect=<sanitised>&reason=expired`; **Browse catalog** → `/`.

**Figma**:
[8-19540](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19540)
/
[8-19565](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19565).
**Do not implement** 403 frames
[8-19590](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19590)
/
[8-19632](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19632).

**Independent Test**: Trigger 401 on mocked mutation → dialog actions.

- [ ] T037 [XCUT] RED: write
  `frontend/src/features/auth/SessionExpiredDialog.test.tsx` — on 401
  signal: dialog visible; Sign in again uses sanitised redirect +
  `reason=expired`; Browse catalog → `/`; session cleared. Confirm fail.
  Show failing output. Test file only.
  ⏸ Review — `test(frontend): expect session expired dialog (red)`

- [ ] T038 [XCUT] GREEN: implement
  `frontend/src/features/auth/SessionExpiredDialog.tsx` mounted from
  layout/providers; wire T016 401 signal. T037 green; full gate green.
  ⏸ Review — `feat(frontend): session expired dialog (green)`

**Checkpoint**: Auth expiry UX complete.

---

## Phase 14: Polish & cross-cutting

**Purpose**: A11y, responsive vs mobile frames, README, quickstart + empty
console.

- [ ] T039 [P] A11y pass across screens: landmarks, h1 focus, labels,
  dialog title+description, `aria-live`, image alt — fix gaps in the
  owning feature files under `frontend/src/`. Full gate green.
  ⏸ Review — `fix(frontend): a11y pass primary screens`

- [ ] T040 [P] Responsive check vs mobile Figma frames (catalog 8-4385,
  detail 8-7421/8-7858, collection 8-9713, auth 5-515/5-1019, edit
  8-10536, dialogs as listed in ui-routes): adjust layout classes only;
  keep 1/2/3 column grid. Full gate green.
  ⏸ Review — `fix(frontend): align layouts to mobile figma frames`

- [ ] T041 [P] Expand README frontend section (dev proxy,
  `VITE_API_PROXY_TARGET`, scripts, demo credential, Figma SoT pointer,
  compose UI :3000) in `README.md`. Full gate green.
  ⏸ Review — `docs(readme): frontend 007 usage`

- [ ] T042 **User action / validation**: run
  [quickstart.md](./quickstart.md) against `docker compose` — visual
  compare each screen/state to Figma screenshots; empty-console check
  per screen/state; fill Results table. Fix any blockers in follow-up
  commits (still TDD if behaviour changes). ⏸ Review

**Checkpoint**: Feature ready for review merge.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Tooling)** → **Phase 2 (Tokens/shadcn)** → **Phase 3 (API)**
  → **Phase 4 (Session)** → **Phase 5 (Shell)** → screen phases in order
  → **Phase 14 (Polish)**
- Screen phases after shell: Catalog → Detail → **Auth** → **Import** →
  Collection → Edit → Delete → Session-expired dialog
- **No scenario work** before Phase 5 completes

### Scenario / label map

| Label | Spec scenario | Phases |
|---|---|---|
| HU01 | Catalog list | 6 |
| HU02 | Pokémon detail | 7 |
| AUTH | Login + Register | 8 |
| HU03 | Import + collection list | 9, 10 |
| HU04 | Edit | 11 |
| FECRUD | Delete | 12 |
| XCUT | Session-expired dialog | 13 |

### Within each RED→GREEN pair

1. RED test file only → show fail → ⏸ user commit (optional local)
2. GREEN production file(s) only → full npm gate → ⏸ user commit
3. Push only green tips

### Parallel Opportunities

- After shell: limited — prefer sequential for single implementer
- Polish T039–T041 can proceed in parallel after T038
- `[P]` only where files do not conflict

### Dependency first-use (IX)

| Dependency | First task |
|---|---|
| (none — alias/proxy/guard) | T001–T003 |
| shadcn CLI + button + cn deps | T006 |
| `@tanstack/react-query` | **T016 GREEN only** |
| `react-router` | T018 |
| shadcn card/badge/skeleton/pagination/alert | T022 |
| shadcn input/label | T026 |
| shadcn alert-dialog | T030 |

---

## Parallel Example (single-agent: still sequential RED→GREEN)

```bash
# Not parallel across RED/GREEN of the same unit — example of file split:
# RED only:  frontend/src/pages/CatalogPage.test.tsx
# GREEN only: frontend/src/pages/CatalogPage.tsx + features/catalog/*
```

---

## Implementation Strategy

### MVP First

1. Phases 1–5 (tooling → shell)
2. Phase 6 Catalog (HU-01) → demo browse
3. Stop / validate before Detail

### Incremental Delivery

1. Detail → Auth → Import → collection write path end-to-end
2. Collection list → Edit → Delete → Session dialog
3. Polish + quickstart

### Suggested MVP scope

Phases **1–6** (through Catalog HU-01) for first demo; full acceptance
needs through Phase 13 + T042.

---

## Notes

- Agent never commits; user commits every task after ⏸ Review
- Tests and implementation in separate files for path-split commits
- Every GREEN:
  `npm run lint && npm run typecheck && npm test && npm run build`
- Spec wins R11 conflicts 1–7; 403 frames documented only
- Mark tasks `[x]` only after the gate for that task type (RED fail /
  GREEN full gate)
