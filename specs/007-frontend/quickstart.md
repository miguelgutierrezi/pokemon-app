# Quickstart validation: Frontend (007)

**Feature**: `007-frontend` | **Date**: 2026-10-17

Contracts: [ui-routes.md](./contracts/ui-routes.md),
[api-client.md](./contracts/api-client.md). Spec: [spec.md](./spec.md).
Figma file:
[PokeApp](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp).

Manual walkthrough against **docker compose** (UI :3000, API via nginx
`/api`). Also valid with Vite dev (:5173) once the `/api` proxy is added.

## Prerequisites

- Fresh stack with Flyway through **V5** and `JWT_SECRET` in local `.env`
  (see root README / `.env.example`)
- `docker compose up --build`
- Browser DevTools open (Console + Network); optional second tab for stale
  version
- Demo credential (labelled **demo**, not a secret):
  `demo@pokefolio.dev` / `Demo-Passw0rd!`
- Figma frame screenshots available (MCP `get_screenshot` or file export)
  for each row in ui-routes.md

## Visual check method (every screen)

For each screen below:

1. Desktop (~1280px): compare running page to the **desktop** Figma frame.
2. Mobile (~375px): compare to the **mobile** Figma frame (every screen
   has one — follow it; do not invent a desktop-only adaptation).
3. Record **accepted deviations** in the Results table (spec-wins copy
   from R11 conflicts 1–7, API images vs Figma placeholders, lucide vs
   custom icons, abilities without descriptions, etc.).
4. Console must stay at **zero** warnings/errors.

## 1. Browse catalog (HU-01) — Catalog [8-3955](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-3955) / [8-4385](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-4385)

1. Open http://localhost:3000/
2. Confirm grid (up to 9 cards): sprite (**from API**), name, category,
   weight (1 decimal), abilities (**name** only; **Hidden ability** label
   when applicable — no descriptions).
3. Go to page 2 — URL is `/?page=2`; browser Back returns to page 1.
4. Open a page beyond the last page (e.g. `/?page=99999`) — empty state +
   link to page 1 (no spinner hang).
5. **Visual check** desktop + mobile vs catalog frames.

Expected: Console **zero** warnings/errors. Nav label **Collection**
(not “My collection”).

## 2. Detail (HU-02) — Not saved [8-7212](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7212) / [8-7421](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7421); Saved [8-7626](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7626) / [8-7858](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7858)

1. Open a card (e.g. Bulbasaur) → `/pokemon/1`.
2. Confirm Network: `GET /api/v1/pokemon/1` **and**
   `GET /api/v1/collection/1` (public; **404** → not-saved UI; **200** →
   saved UI with **Already in the collection** / **Saved in the shared
   collection**).
3. Confirm image (**API `imageUrl`**), stats **including total**, flavor
   text, evolution UI; abilities = name + Hidden label only.
4. Open `/pokemon/999999` → not-found.
5. **Visual check** vs matching saved/not-saved frames (shared copy wins
   over Figma “my/your” wording).

## 3. Login demo (TR-API-AUTH) — Login [5-233](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-233) / [5-515](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-515)

1. From detail, click **Add to collection** while signed out → `/login`
   with `redirect=` back to the detail.
2. Confirm **demo credential** hint is visible (`demo@pokefolio.dev` /
   `Demo-Passw0rd!`) — **spec wins**; frames omit it.
3. Submit → land back on detail, signed in.
4. **Visual check** vs Login frames (+ demo hint deviation accepted).

Expected: Network `POST /api/v1/auth/login` **200**; password never in
response body.

## 4. Import (HU-03) — Confirm [8-18699](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18699) / [8-18738](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18738) → Progress [8-18777](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18777) / [8-18817](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18817) → Success [8-18857](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18857) / [8-18899](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18899) or 409 [8-18941](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18941) / [8-18983](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18983)

1. On a Pokémon not in the seed collection (e.g. `#25`), **Add to
   collection** (not “my”).
2. Confirm dialog → in-progress → success dialog with a link to the
   record.
3. Repeat (or race) → **409** dialog **Already in the collection**.

Expected: `POST /api/v1/collection` with `Authorization: Bearer …` →
**201** then **409**.

## 5. Collection list + pagination — Collection [8-9535](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9535) / [8-9713](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9713); Loading [8-19025](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19025) / [8-19084](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19084); Empty [8-19136](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19136) / [8-19176](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19176); Connection error [8-19215](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19215) / [8-19257](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19257)

1. Open `/collection` — proprietary fields; public GETs **without**
   `Authorization`.
2. If multiple pages, use `?page=N` and Back button.
3. **Visual check** desktop + mobile vs collection frames (shared
   **Collection** label).

## 6. Edit + stale 409 (HU-04) — Edit [8-10425](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10425) / [8-10536](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10536); 400 [8-19377](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19377) / [8-19459](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19459); 404 [8-19298](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19298) / [8-19338](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19338)

1. Open `/collection/25` (or imported id); note `version`.
2. Confirm **no** save confirmation modal (only import has confirm).
3. **Two tabs**: both open the same edit page.
4. Tab A: save → **200**, version increments.
5. Tab B: save without reload → **Modified elsewhere — reload**; reload;
   no silent success.
6. **Visual check** vs Edit frames; validation/404 frames as needed.

## 7. Delete (FE-CRUD) — Remove dialog [8-9889](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9889) / [8-9923](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9923)

1. On edit page, Delete → dialog shows **title and description**.
2. Confirm → **204**; land on `/collection`; record gone.
3. Cancel path: open delete again and cancel → record remains.
4. **Visual check** vs remove-dialog frames.

## 8. Logout + public reads

1. Logout (client-only; no logout API call).
2. Catalog and collection GETs still work; writes send you to login.

## 9. Session expired path — Dialog [8-19540](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19540) / [8-19565](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19565)

1. Sign in; in DevTools Application → Local Storage, set
   `pokefolio.session` `expiresAt` to the past (or wait for real expiry).
2. Trigger a protected write → **session expired dialog** (not a silent
   redirect).
3. **Sign in again** → `/login?redirect=<sanitised current>&reason=expired`.
4. Separately: **Browse catalog** → `/`.

## 10. Mobile viewport + console

1. DevTools device toolbar ~375px — compare to **mobile** frames; catalog
   **1** column; no horizontal overflow of primary content.
2. Widen — **2** then **3** columns.
3. Re-check Console: **zero** warnings/errors on every screen visited.

## 11. Register smoke (optional) — Register [5-921](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-921) / [5-1019](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-1019)

1. `/register` with a unique email → immediate session (no second login).
2. Same email different casing → **An account with this email already
   exists**.
3. **Visual check** vs Register frames.

## 12. Not-found route — [8-10806](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10806) / [8-10853](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10853)

1. Open an unknown path → not-found page; visual check desktop + mobile.

## Not validated (by design)

- Access **403** frames [8-19590](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19590) /
  [8-19632](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19632):
  documented only; UI never calls denied routes.

## Results

_(Fill when validating.)_

| Check | Result | Accepted visual deviations vs Figma |
|-------|--------|-------------------------------------|
| Catalog pagination + beyond-last | | |
| Catalog visual (desktop / mobile) | | Shared “Collection”; abilities no descriptions |
| Detail + collection GET saved/not-saved | | Shared copy vs my/your |
| Detail visual | | |
| Demo login + return path | | Demo hint added |
| Login visual | | |
| Import confirm → progress → success / 409 | | Shared copy |
| Collection list + paging | | |
| Collection visual (loading / empty / error) | | |
| Edit + stale reload (no save modal) | | |
| Edit visual / 400 / 404 | | |
| Delete dialog a11y | | |
| Logout / public GET no Bearer | | |
| Session expired dialog → login / catalog | | |
| Register visual | | |
| Not-found route | | |
| Mobile frames + console clean | | |
| 403 | N/A — not implemented | |
