# Research: 007-frontend

**Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

Versions below were checked against npm / current docs on plan date.
**Re-verify at implement** before pinning majors; prefer latest compatible
patch within the decided major. No deprecated APIs; zero console warnings
is a hard requirement.

---

## R1 — Dependencies (constitution IX: first use)

### Decision

Add only what 007 first exercises:

| Package | Verified latest (plan date) | Role |
|---|---|---|
| `@tanstack/react-query` | **5.104.4** | Server state (constitution VII) |
| `react-router` | **8.4.0** | Data router (`createBrowserRouter` + `RouterProvider`) |
| shadcn/ui (CLI) | `shadcn@4.22.0` | Generate UI primitives into `src/components/ui` |
| `lucide-react` | **1.59.0** | Icons (pulled with shadcn) |
| `class-variance-authority` | **0.7.1** | shadcn `cn` / variants |
| `clsx` | **2.1.1** | class merging |
| `tailwind-merge` | **3.7.0** | class merging |
| `@radix-ui/react-slot` | **1.5.0** | Button asChild |
| `@radix-ui/react-label` | **2.1.17** | Label |
| `@radix-ui/react-alert-dialog` | **1.2.1** | Delete confirm |
| `tw-animate-css` (if CLI adds) | **1.4.0** | shadcn Tailwind v4 animations |

**shadcn components to add (only these)**: `button`, `input`, `label`,
`card`, `badge`, `skeleton`, `alert`, `alert-dialog`, `pagination` (plus
whatever single shared util `cn` the CLI writes under `src/lib/utils.ts`).

**Do not add**: react-hook-form / zod / formik, MSW, TanStack Query Devtools,
Zustand/Redux/Jotai, `react-router-dom` (removed as a separate package in
modern RR), axios.

**Existing skeleton (keep)**: Vite **8.3**, React **19.2**, TypeScript
**~6.0** strict, Tailwind **v4.3** + `@tailwindcss/vite`, Vitest **5** +
Testing Library, Node **≥24**, ESLint 10.

### Rationale

- Constitution VII mandates React + TanStack Query + presentational split;
  Technology Stack lists Tailwind, shadcn/ui, React Router, Vitest.
- IX: introduce packages only when first used — all above are first used in
  007.
- Controlled forms + TextEncoder password byte check avoid a form library.
- `vi.mock` of `src/api` avoids MSW.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| React Query Devtools | Extra dep; not needed for CI/demo; IX |
| MSW | Spec: mock at API-client boundary with `vi.mock` |
| Zustand / Redux | Session is small Context + pure store; server state is Query |
| axios | `fetch` is enough; fewer deps |
| react-hook-form + zod | Spec: controlled components; API already validates |
| Keep React Router v6 + future flags | Console warnings risk (hard requirement) |
| Pin exact React Router **v7** forever | Current GA is **8.4.0**; same data-router model, no `react-router-dom` package; implement uses **latest 8.x** verified at install (user intent: not v6) |

---

## R2 — Source structure

### Decision

```text
frontend/src/
├── app/           # providers, router, layout, not-found
├── api/           # fetch client, ApiError, endpoints + DTO types
├── features/
│   ├── catalog/
│   ├── pokemon/
│   ├── collection/
│   └── auth/      # query/mutation hooks + presentational components
├── pages/         # containers: hooks → components
├── components/ui/ # shadcn only
└── lib/           # cn(), sanitizeReturnPath, session store helpers
```

Presentational components under `features/*/components` **never** import
hooks that fetch (`useQuery` / `useMutation` / api modules). Pages own
wiring.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Fetch inside every card/component | Violates constitution VII |
| `src/services` bag without features | Harder HU/TR mapping and ownership |
| Colocate pages inside features only | Spec asks for `src/pages` containers |

---

## R3 — API client & problem+json field extraction

### Decision

- `fetch` + `JSON`; `Content-Type: application/json` on bodies.
- Attach `Authorization: Bearer <token>` **only** for protected writes:
  `POST /api/v1/collection`, `PUT|DELETE /api/v1/collection/{n}`.
- Public GETs and auth register/login **never** send the token.
- On `application/problem+json`, parse into `ApiError`:
  `{ status, type, title, detail, field? }`.
- **Field extraction** (contract dependency on 005/006 handler copy):
  - Match `parameter '([^']+)'` → e.g. BV:
    `Invalid value for parameter 'password'`
  - Match `property '([^']+)'` → e.g.
    `Unknown or read-only property 'name'`,
    `Invalid or missing value for property 'version'`
  - First match wins; unit-test both shapes and “no field”.
- Network / non-JSON / non-problem HTTP → `ApiError` with `status: 0` (or
  raw status if HTTP but unparseable body) and generic English detail
  suitable for `aria-live`.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Always send Bearer | Spec: public reads ignore/never depend on token |
| Rely on undocumented `errors[]` extension | Backend uses detail strings today |
| Throw raw `Response` | Pages need typed mapping for inline fields |

---

## R4 — Session storage

### Decision

- Pure session module + `AuthProvider` (React context).
- Persist under `localStorage` key `pokefolio.session`:
  `{ accessToken, expiresAt (epoch ms), user: { id, name, email } }`
  where `expiresAt = Date.now() + expiresIn * 1000` from login/register.
- On load: clear if missing/corrupt/expired.
- Before each protected write: if expired, clear and treat as signed out
  (same UX as 401).
- `storage` event syncs other tabs.
- **XSS trade-off (MVP)**: token in `localStorage` is readable by any XSS
  on the origin. Memory-only avoids persistence across refresh; httpOnly
  cookie would need backend changes (out of scope). Accept localStorage
  for interview MVP; mitigate with CSP/same-origin and no `dangerouslySetInnerHTML`.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Memory-only session | Loses session on refresh; worse demo UX |
| httpOnly cookie | Requires backend/CORS cookie work — out of scope |
| sessionStorage | No multi-tab sync story; still XSS-visible |

---

## R5 — 401 → session-expired dialog

### Decision

- On `ApiError` with `status === 401` from a protected write: clear
  session; show the Figma **session expired** dialog (desktop
  [8-19540](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19540)
  / mobile
  [8-19565](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19565)).
- Dialog actions:
  - **Sign in again** →
    `/login?redirect=<encodeURIComponent(sanitised current path+search)>&reason=expired`
  - **Browse catalog** → `/`
- Login may still show expired context when `reason=expired`.
- `sanitizeReturnPath(raw): string` — pure, unit-tested: must start with
  `/`, must not start with `//`, otherwise `/`.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Navigate straight to `/login` with no dialog | Figma defines a dialog; product decision keeps it |
| Global `fetch` interceptor only | Mutations are the protected-write path |
| Open redirects via absolute `redirect=` | Spec forbids; sanitiser required |

---

## R6 — Query design

### Decision

- Query-key factory (e.g. `queryKeys.catalog.list(page)`,
  `queryKeys.pokemon.detail(id)`, `queryKeys.collection.list(page)`,
  `queryKeys.collection.item(id)`).
- Catalog `staleTime`: **5 minutes** (backend Caffeine TTL 24 h — avoid
  refetch chatter).
- Pagination: `placeholderData: keepPreviousData` from
  `@tanstack/react-query` v5 (not removed boolean `keepPreviousData: true`).
- `retry`: `(count, err) =>` never on 4xx `ApiError`; at most **once** on
  network/`status === 0` or **502**.
- Detail page also queries `queryKeys.collection.item(id)` via public
  `GET /api/v1/collection/{id}`: **200** → saved UI; **404** → not-saved
  UI (proactive; **409** still for import races).
- After import/update/delete: invalidate collection list + item keys.
- Import UX follows Figma: confirmation dialog → in-progress → success
  (link to the record) or **409** dialog.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| `keepPreviousData: true` (v4 API) | Removed in v5 — use `placeholderData: keepPreviousData` |
| `staleTime: 0` on catalog | Unnecessary refetch vs 24 h server cache |
| Retry all errors | Amplifies 400/401/404 noise |

---

## R7 — Forms & validation

### Decision

- Controlled inputs; client mirrors API:
  - Register: name trim 1–100; email shape; password length ≥ 8 chars and
    `new TextEncoder().encode(pw).length ≤ 72`.
  - Edit: localizedName ≤ 100; location ≤ 200; ≤ 20 tags; each non-blank
    ≤ 32 after trim; empty localizedName/location → JSON `null`.
- Map server 400 → field via `ApiError.field`; focus first invalid field.
- No save confirmation on edit (only edit SAVE skips confirmation);
  import uses confirmation → progress → success/409 dialogs; delete uses
  `AlertDialog` (title + description).

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| zod + RHF | Extra deps; IX; controlled forms sufficient |

---

## R8 — Vite dev proxy

### Decision

In `vite.config.ts` `server.proxy`:

```ts
'/api': {
  target: env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
  changeOrigin: true,
  // no rewrite
}
```

`frontend/nginx.conf` `location /api/` stays
`proxy_pass http://backend:8080;` (path preserved).

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Rewrite `/api` → `` | Breaks `/api/v1/...` constitution IV |
| Absolute API URLs in client | Breaks Docker same-origin nginx |

---

## R9 — Accessibility & zero-warnings

### Decision

- Landmarks: `header` / `main` / `nav`; one `h1` per page; focus `h1` on
  route change (`useEffect` + `tabIndex={-1}`).
- `aria-live="polite"` region for async errors/success.
- Labels associated with inputs; image `alt={name}`, explicit
  `width`/`height`, `loading="lazy"`.
- AlertDialog: `AlertDialogTitle` + `AlertDialogDescription` always.
- Visible focus rings (Tailwind / shadcn defaults).
- Grid: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`, aligned with mobile
  frames in Figma (every screen has a mobile frame — follow them).
- Icons: `lucide-react` matching Figma; Pokémon images from API only.
- Abilities: API returns only `name` + `hidden` → show name and a
  **Hidden ability** label; **no** ability descriptions.
- StrictMode on; unique list keys; no controlled↔uncontrolled flips;
  no RR future-flag warnings (RR 8 data router).

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Toast-only errors without live region | Weaker a11y for async failures |
| Unstyled confirm `window.confirm` | No title/description; a11y/console risk |

---

## R10 — Tests & CI

### Decision

- Vitest + RTL; `vi.mock` the `api` module (or per-endpoint modules).
- Cover: problem parsing + field regex; session store; return-path
  sanitiser; page states (loading/empty/not-found/502/success/conflict/
  stale); form validation; 401 mutation → login redirect.
- Existing CI frontend job unchanged (lint, typecheck, test, build, nginx
  smoke) and must stay green.

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| MSW | Spec boundary is api client |
| Change CI job | Spec: unchanged |

---

## R11 — Figma as visual source of truth

### Decision

Constitution Technology Stack: **UI designed in Figma**. For 007:

1. **File / frames** — File:
   [PokeApp](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp)
   (`fileKey=XfY2LFLQuycRZge36S1zey`). Pattern:
   `https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=<id with "-">`.
   Full desktop/mobile map:
   [contracts/ui-routes.md](./contracts/ui-routes.md).

   | Screen | Desktop | Mobile |
   |---|---|---|
   | Login | [5-233](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-233) | [5-515](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-515) |
   | Register | [5-921](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-921) | [5-1019](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-1019) |
   | Catalog | [8-3955](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-3955) | [8-4385](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-4385) |
   | Detail not saved | [8-7212](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7212) | [8-7421](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7421) |
   | Detail saved | [8-7626](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7626) | [8-7858](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7858) |
   | Collection | [8-9535](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9535) | [8-9713](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9713) |
   | Remove dialog | [8-9889](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9889) | [8-9923](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9923) |
   | Edit | [8-10425](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10425) | [8-10536](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10536) |
   | Not-found route | [8-10806](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10806) | [8-10853](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10853) |
   | Import confirmation | [8-18699](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18699) | [8-18738](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18738) |
   | Import in progress | [8-18777](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18777) | [8-18817](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18817) |
   | Import success | [8-18857](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18857) | [8-18899](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18899) |
   | Duplicate 409 | [8-18941](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18941) | [8-18983](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18983) |
   | Collection loading | [8-19025](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19025) | [8-19084](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19084) |
   | Empty collection | [8-19136](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19136) | [8-19176](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19176) |
   | Collection connection error | [8-19215](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19215) | [8-19257](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19257) |
   | Record not found 404 | [8-19298](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19298) | [8-19338](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19338) |
   | Edit validation 400 | [8-19377](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19377) | [8-19459](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19459) |
   | Session expired 401 | [8-19540](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19540) | [8-19565](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19565) |
   | Access 403 (**do not implement**) | [8-19590](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19590) | [8-19632](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19632) |

2. **Design tokens** — `get_variable_defs` on catalog frame `8:3955`
   returns **`{}`** (file has **no** Figma variables). Extract colors,
   font families/sizes/weights, radii, and shadows from
   `get_design_context` of **catalog** (`8:3955`) and **detail**
   (`8:7212` / `8:7626`) and define them **once** in
   `frontend/src/index.css` (`@theme` + shadcn CSS vars). Components use
   semantic utilities / CSS vars — **no hardcoded hex** in TSX.

   | Token group | Values (catalog `8:3955`, detail `8:7626`; alerts `8:19215` / `8:18857`) → `frontend/src/index.css` |
   |---|---|
   | Surfaces / ink | `#182126` nav/ink (`--nav`, `--foreground`); `#f5f7f8` page (`--background`); `#ffffff` card; `#fafbfc` surface/muted (`--surface`, `--muted`) |
   | Accent | `#c83738` Pokéball red (`--primary`, `--destructive`, `--ring`) |
   | Neutrals / borders | `#626d75` muted text; `#aab6bc` nav inactive (`--nav-muted`); `#e8ebed` / `#dde2e5` borders (`--border`, `--input`) |
   | Success / danger tints | Success text `#416a43`, muted `#edf4ed`; danger muted `#f9eeee`, border `#e9c5c5` (connection-error emblem / evo current); overlay `rgb(24 33 38 / 0.5)` |
   | Type chips (optional) | Grass `#edf4ed`/`#416a43`; Poison `#f2edf5`/`#785288` (detail frames; not wired as shadcn core vars) |
   | Typography | **Inter** 400/500/600 (`--font-sans`); **IBM Plex Mono** 400 (`--font-mono`) for uppercase labels e.g. “NATIONAL POKÉDEX”. Loaded via Google Fonts `<link display=swap>` in `index.html`. Sizes: 9–16, 18, 20, 21, 24–25, 32, 36, 56 (hero) |
   | Radii | `6px` chips (`--radius-sm`); `10px` buttons (`--radius` / `--radius-md`); `12px` panels (`--radius-lg`); stat bar `3px` (local utility later) |
   | Shadows | Dialog: `0 12px 36px rgb(24 33 38 / 0.14)` (`--shadow-dialog`); catalog/detail surfaces otherwise border-only |

3. **Per screen** — Before implementing a page: `get_design_context` +
   `get_screenshot` for **desktop and mobile** frames. Generated code is
   **REFERENCE ONLY**: rebuild with shadcn, tokens, semantic HTML.
   **Forbidden**: absolute positioning and pixel-fixed widths copied from
   the frame.

4. **Responsive** — **Mobile frames exist for every screen.** Drop the
   desktop-only assumption; **follow the mobile frames**. Catalog /
   collection grid still targets **1 / 2 / 3** columns as width grows,
   consistent with those frames. Full-width primary actions; no
   horizontal overflow; edit form stacks; nav follows mobile frame (no
   invented hamburger unless Figma shows one).

5. **Assets** — Pokémon images **only** from API (`spriteUrl` /
   `imageUrl`). **Never** export Pokémon art from Figma. Icons:
   `lucide-react` equivalents of the design’s icons.

6. **Conflict policy** — If Figma disagrees with [spec.md](./spec.md) on
   copy, flows, or states, **the SPEC wins**. Conflicts 1–7 below are
   locked for implement.

### Spec-vs-Figma conflicts (1–7)

| # | Topic | Spec (wins) | Figma shows / notes |
|---|---|---|---|
| **1** | Shared-collection copy | Nav **Collection**; CTA **Add to collection**; duplicate **Already in the collection**; saved **Saved in the shared collection** | “My collection”, “Save to my collection”, “Already in your collection”, “Saved to your account…”, “Private to your account…” |
| **2** | Detail saved / not-saved | Proactive `GET /api/v1/collection/{id}` (public); **404** → not-saved UI; **200** → saved UI; **409** remains for import races | Frames exist for both variants; wiring is GET-based, not import-only |
| **3** | Import flow | Confirmation dialog → in-progress → success (link to record) **or** 409 dialog. Edit **SAVE** has **no** confirmation modal | Import dialog frames `8-18699`…`8-18983`; edit has no save-confirm frame to implement |
| **4** | Session expired (401) | Show Figma dialog; **Sign in again** → `/login?redirect=<sanitised current>&reason=expired`; **Browse catalog** → `/` | Frames `8-19540` / `8-19565` |
| **5** | Access 403 | **Document only — do not implement** (UI never calls denied routes) | Frames `8-19590` / `8-19632` |
| **6** | Abilities | Show **name** + **Hidden ability** label when `hidden`; **no** descriptions | Detail frames include ability descriptions and “STANDARD” / “HIDDEN ABILITY” chrome |
| **7** | Login demo hint | Show `demo@pokefolio.dev` / `Demo-Passw0rd!` labelled **demo credential** | Login frames omit the hint — **add it** |

### Alternatives rejected

| Alternative | Why rejected |
|---|---|
| Treat Figma-generated code as production | Violates FE-ARCH / a11y / responsive rules |
| Export sprites from Figma | Spec/API already provide image URLs |
| Hardcode hex in TSX | Breaks single-token-source (`index.css`) |
| Rely on Figma variables | File has none (`get_variable_defs` → `{}`) |
| Desktop-only adaptation guess | Mobile frames exist for every screen |
