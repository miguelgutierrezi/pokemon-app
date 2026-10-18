# UI routes contract: 007-frontend

**Date**: 2026-10-17 | **Spec**: [../spec.md](../spec.md)

Route → page container → API calls → primary UI states. Query param
`redirect` is always sanitised (see [api-client.md](./api-client.md) /
session helpers).

**Figma file** (visual SoT):
[PokeApp](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp)

Frame URL pattern:
`https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=<id>`
(use hyphenated ids in the query string, e.g. `8-3955`).

Per screen: run Figma MCP `get_design_context` + `get_screenshot` on the
desktop **and** mobile frames before implement; treat output as
**reference only**. Rebuild with shadcn + tokens from `src/index.css`.

| Route | Page | Figma desktop | Figma mobile | Auth to view | API calls | Primary states / related frames |
|---|---|---|---|---|---|---|
| `/login` | LoginPage | [5-233](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-233) | [5-515](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-515) | Public | `POST /api/v1/auth/login` | Form; **401** generic; demo credential hint (**spec wins** — not in frame); success → sanitised redirect. Session expired dialog: [8-19540](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19540) / [8-19565](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19565) |
| `/register` | RegisterPage | [5-921](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-921) | [5-1019](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=5-1019) | Public | `POST /api/v1/auth/register` | Form; **400** field; **409** account exists; success → session + sanitised redirect |
| `/` `?page=N` | CatalogPage | [8-3955](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-3955) | [8-4385](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-4385) | Public | `GET /api/v1/pokemon?page=N&size=9` | Loading / success / beyond-last empty + link page 1 / **400** / **502** + retry |
| `/pokemon/:id` | PokemonDetailPage | Not saved [8-7212](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7212); Saved [8-7626](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7626) | Not saved [8-7421](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7421); Saved [8-7858](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-7858) | Public | `GET /api/v1/pokemon/{id}` + `GET /api/v1/collection/{id}` (public; **404** → not-saved UI); import `POST /api/v1/collection` (protected) | Detail **404** catalog; **502** + retry. Import dialogs: confirm [8-18699](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18699) / [8-18738](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18738); in progress [8-18777](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18777) / [8-18817](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18817); success [8-18857](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18857) / [8-18899](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18899); duplicate **409** [8-18941](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18941) / [8-18983](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-18983). Abilities: name + “Hidden ability” only (no descriptions). Copy: **Add to collection** / **Already in the collection** / **Saved in the shared collection** |
| `/collection` `?page=N` | CollectionPage | [8-9535](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9535) | [8-9713](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9713) | Public read | `GET /api/v1/collection?page=N&size=9` | Loading [8-19025](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19025) / [8-19084](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19084); empty [8-19136](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19136) / [8-19176](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19176); connection error [8-19215](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19215) / [8-19257](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19257); beyond-last empty + link page 1; nav label **Collection** (not “My collection”) |
| `/collection/:id` | CollectionEditPage | [8-10425](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10425) | [8-10536](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10536) | Public read; writes need session | `GET /api/v1/collection/{id}`; `PUT` (protected); `DELETE` (protected) | **404** record [8-19298](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19298) / [8-19338](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19338); save **400** [8-19377](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19377) / [8-19459](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19459); **409** stale “Modified elsewhere — reload”; **no** save confirmation modal. Delete dialog [8-9889](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9889) / [8-9923](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-9923) → **204** → `/collection` |
| `*` | NotFoundPage | [8-10806](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10806) | [8-10853](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-10853) | Public | none | Unknown route |

## Not implemented (document only)

| Frame | Desktop | Mobile | Reason |
|---|---|---|---|
| Access **403** | [8-19590](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19590) | [8-19632](https://www.figma.com/design/XfY2LFLQuycRZge36S1zey/PokeApp?node-id=8-19632) | UI never calls denied routes; do **not** implement |

## Navigation notes

- Catalog/collection page changes update the URL (`?page=N`, **1-based**)
  and support the browser back button.
- Login-required actions navigate to
  `/login?redirect=<encodeURIComponent(path+search)>`.
- On protected-write **401**: show session-expired **dialog** (Figma); 
  **Sign in again** → `/login?redirect=<sanitised current>&reason=expired`;
  **Browse catalog** → `/`.
- After login/register, navigate to `sanitizeReturnPath(redirect)`.
- Logout is client-only; prefer stay on current publicly readable page.

## Layout shell

- Brand / nav: **Pokémon catalog**, **Collection** (shared wording — not
  “My collection”), Sign in / Create account or signed-in account control
- `main` landmark + page `h1` (focused on route change)
- `aria-live="polite"` region for async messages

## Visual / responsive notes

- Mobile frames exist for every screen — **follow them** (not a
  desktop-only adaptation guess).
- Rebuild with shadcn + tokens — no absolute positioning, no pixel-fixed
  widths copied from frames.
- Spec wins on shared-collection copy and other conflicts (research R11).
