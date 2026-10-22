# Migration Plan: shoplist-android

Reference document for migrating the shoppinglist web frontend to a native Android app. The backend (Elysia REST + SSE) is reused as-is; this app is a pure API client that replaces the React frontend.

- Web repo: `/Users/cosmin.pascariu/projects/shoppinglist` (backend + API contracts live there)
- Roadmap: `docs/ROADMAP.md` (what and why, ordered)
- This file: discovered contracts (source material), decisions log, and the traceability matrix correlating web specs to Android work

## 1. Goal & Context

Build a native Android (Kotlin + Compose) client for the existing shoppinglist backend:

- **Backend reused as-is:** Elysia REST API + SSE, PostgreSQL, deployed at `https://shoppinglist.pascariu.eu` behind a Cloudflare Tunnel with Cloudflare Access. The backend intentionally has no auth middleware — Cloudflare Access is the only gate.
- **No pagination, no API versioning:** every fetch returns the full item list; the server is the source of truth for ordering keys, expiry reconciliation, check-count arithmetic, and Global-shop protection.
- **Scale assumption:** shopping-list scale (hundreds of items max) — in-memory cache, no local database initially.

## 2. Decisions Log

| Decision | Choice | Notes |
|---|---|---|
| UI toolkit | Jetpack Compose with a **custom design language** | Port the web app's chromeless / borderless / LED-dot aesthetic. Material 3 only as an unthemed component toolkit where convenient. Alternative (full custom, no M3) kept in mind. |
| Auth | Cloudflare Access service tokens | `CF-Access-Client-Id` / `CF-Access-Client-Secret` headers on every request (including SSE connects). Encrypted storage (EncryptedSharedPreferences + Keystore), QR/manual entry, 401/403 → token re-entry screen. Ported from the `helloworldcosmin` testbed. |
| Auth dev bypass | Build-time switch | Local dev backend sits outside Cloudflare Access; debug builds use a no-op auth interceptor (Hilt provides per build type). Never scattered conditionals. |
| minSdk | 35 | Deliberate security choice. Do not lower. |
| Base URL | Prod: `https://shoppinglist.pascariu.eu` · Dev: `devBaseUrl` in gitignored `local.properties` (fallback `http://192.168.0.141:3005`, the workstation LAN IP) | Dev URL is machine-local so the LAN IP never lands in git; the fallback is the workstation LAN IP (believed DHCP-reserved; alternative: Bonjour `http://<macname>.local:3005`) since the user tests on a physical device. Debug builds also permit cleartext to `10.0.2.2` for emulator use. |
| Background power | No network while backgrounded | SSE + any polling stop when the app is not visible (`repeatOnLifecycle(STARTED)`); on foreground, reconnect + full refetch. Revisit only if notifications are added. |
| Pull-to-refresh | Dropped | SSE + refetch-on-reconnect (network drops and foregrounding) is the freshness mechanism. Revisit only if realtime proves unreliable. |
| Local database | None initially | In-memory repository cache; server is truth. Room arrives with offline support (roadmap item 8). |
| Themes | Rethink: small curated set | The web app's 14 themes are too many / too similar. Selection discussed when the themes item starts. WCAG AA contrast enforced by unit tests. |
| Undo history | Device-local, ported verbatim | 50 entries, 30-min TTL, stale-entry validation, `isUndo: true` PATCH semantics. |
| PWA features | Dropped | Service worker, install prompt, theme-color, reload toast — natively irrelevant. |

## 3. Discovered Contracts (from the web repo)

### 3.1 REST API surface

Base path `/api` on the Elysia backend. No auth at the API level (Cloudflare Access at the edge). No pagination. Errors are plain HTTP status codes with JSON `{ error: string }` bodies (400 validation, 404 missing, 403 protected Global shop, 409 duplicate).

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Liveness probe → `{ status: "ok" }` |
| GET | `/api/lists/:listId/items` | All items in list (non-expired only; server deletes expired first and broadcasts `item:deleted`) |
| POST | `/api/lists/:listId/items` | Create item. Body: `{ name (1–200), visibleIn?: number[], orderMap?, quantity? (1–99), isTemporary? }` → 201 + Item. Server generates fractional order keys per shop |
| GET | `/api/lists/:listId/subscribe` | SSE stream for item events |
| PATCH | `/api/items/:itemId` | Partial update: `{ name?, isChecked?, isUndo?, orderMap?, visibleIn?, quantity?, isTemporary? }`. `isUndo: true` + `isChecked: false` decrements checkCount (floor 0); plain check-off increments. `visibleIn` change re-derives orderMap server-side |
| DELETE | `/api/items/:itemId` | Delete → 204 |
| GET | `/api/shops` | All shops, Global first, then A→Z |
| GET | `/api/shops/subscribe` | SSE stream for shop events |
| POST | `/api/shops` | Create shop `{ name (1–100, non-whitespace required) }`; 409 on duplicate |
| PATCH | `/api/shops/:shopId` | Rename; 403 if "Global" |
| DELETE | `/api/shops/:shopId` | Delete; 403 if "Global"; cascades item_shops |

**Device ID:** mutations carry an `X-Device-Id` header (UUID, ≤ 64 chars, `[A-Za-z0-9_-]`). The backend echoes it as `originDeviceId` in SSE events so clients can suppress toasts for their own changes. GETs and SSE connects do not send it.

### 3.2 SSE protocol

Two independent streams: items (`/api/lists/:listId/subscribe`) and shops (`/api/shops/subscribe`).

- Wire format: `event: <type>\ndata: <JSON>\n\n`; first frame is `event: connected`; heartbeat comment `: ping` every 25 s.
- **No replay / no Last-Event-ID:** events during a dropped connection are lost. On every reconnect, refetch the full list (items + shops).
- Event types (`packages/shared/src/api/events.ts`):
  - `item:created` / `item:updated` → `{ type, listId, item, originDeviceId }`
  - `item:deleted` → `{ type, listId, itemId, originDeviceId }`
  - `shop:created` / `shop:updated` → `{ type, shop, originDeviceId }`
  - `shop:deleted` → `{ type, shopId, originDeviceId }`
- Cache merge rules: `item:created` appends only if id not present (idempotent); `item:updated` replaces only if the item exists and actually differs; `item:deleted` filters by id; shops re-sort (Global first, then A→Z) after every event.
- Change toasts are derived client-side from SSE diffs (created / deleted / checked / unchecked / renamed / quantity / visibility), suppressed when `originDeviceId` matches the own device or toasts are disabled. One toast at a time, 3.5 s auto-dismiss.

### 3.3 Data model & invariants

- **lists:** `id`, `name`, `createdAt`. Single list (id 1) in practice.
- **shops:** `id`, `name` (unique), `createdAt`. The shop named **"Global"** cannot be renamed or deleted (403).
- **items:** `id`, `listId`, `name`, `isChecked`, `orderMap` (JSONB: shopId string → fractional key), `quantity` (1–99, default 1), `isTemporary`, `checkCount`, `expiresAt` (nullable), `createdAt`. `visibleIn: ShopId[]` is appended to every Item payload (from the `item_shops` join table).
- **Ordering:** sort within a view is a byte-wise (not locale-aware) string comparison of `orderMap[String(viewId)]`; items with no key sort last, tie-broken by `id`. The same item has independent order keys per shop.
- **Visibility:** Global view shows all items; a shop view shows only items whose `visibleIn` includes that shop. Empty/missing `visibleIn` normalizes to `[globalShopId]`.
- **checkCount:** check-off +1; manual uncheck no change; undo of a check-off −1 (via `isUndo: true`); undo that re-checks +1. Frequency sort = checkCount desc, name A→Z tie-break.
- **Temporary items + expiry:** checking a temporary item sets `expiresAt = now + 24h` (server); unchecking or making permanent clears it. Server deletes expired items on list fetch; client additionally hides expired rows locally on a 1-minute timer.
- **Validation limits to mirror in UI:** item name 1–200; shop name 1–100 (must contain non-whitespace); quantity 1–99; orderMap ≤ 100 keys, values ≤ 64 chars; device id ≤ 64 chars `[A-Za-z0-9_-]`.

### 3.4 Behavior contracts to port

- **Optimistic check-off:** flip local state synchronously before the PATCH; roll back on failure; no invalidation — the SSE `item:updated` echo reconciles; cancel in-flight refetches so they cannot clobber the flip.
- **Delete / update item:** snapshot-previous → mutate → rollback-on-error → invalidate-on-settle.
- **Reorder:** compute the new fractional key between neighbors locally, PATCH the whole `orderMap`, invalidate on settle. Client computes keys only for local reorders and for keys of newly-added shops in the edit modal.
- **Drag display:** while dragging, show raw manual order (checked items temporarily revealed); clear the local override whenever the server-sorted list changes.
- **Undo history:** device-local, global across views; max 50 entries, 30-min TTL, persisted; entry = `{ itemId, from, to, at }`; no-op flips create no entries; undo validates before PATCHing (expired / item deleted / state mismatch → drop silently); waits (bounded 2 s) for in-flight check PATCHes; one undo per tap; on failure roll back and restore the entry.
- **Search:** subsequence fuzzy match, case-insensitive, in-order characters; empty result offers quick-add pre-filled with the query + multi-shop visibility checklist.
- **Persistence (localStorage → DataStore):** active view (fallback to Global if deleted), widget state (`isExpanded`, `isSearchExpanded`, `sortModeEnabled`, `showCheckedEnabled`, `sortOrder: manual|alphabetical|frequency`; search query never persisted), theme id, change-toasts-enabled (default true), device id, check history.
- **Gestures:** drag only from a per-row handle and only in Manual sort mode; long-press row → edit modal (drag disabled while open); long-press sort toggle → sort-order selector; collapsible widget stack with persisted state; bottom-anchored list layout.

## 4. Traceability Matrix

Web capability specs (`/Users/cosmin.pascariu/projects/shoppinglist/openspec/specs/<name>/spec.md`) mapped to their Android home. Status updates after each `/opsx-archive`.

| Web spec | Android home | Status |
|---|---|---|
| `frontend-scaffold` | Roadmap 1 — scaffold migration | **done** — `scaffold-compose-foundation` (archived 2026-10-22); Android specs: `app-scaffold`, `cloudflare-auth` |
| `rest-api` (client side) | Roadmap 2 — data layer | **done** — `data-layer-repositories` (archived 2026-10-22); Android spec: `data-layer` |
| `shared-types-package` | Roadmap 2 — DTOs | **done** — `data-layer-repositories` (archived 2026-10-22); Android spec: `data-layer` |
| `sse-subscriptions` | Roadmap 3 — realtime sync | **done** — `realtime-sync-sse` (archived 2026-10-22); Android spec: `realtime-sync` |
| `frontend-realtime-sync` | Roadmap 3 — realtime sync | **done** — `realtime-sync-sse` (archived 2026-10-22); Android specs: `realtime-sync`, `data-layer` (merge methods) |
| `frontend-data-fetching` | Roadmap 2 + 3 — repositories + SSE merge | **done** — repositories in `data-layer-repositories`; SSE merge in `realtime-sync-sse` (both archived 2026-10-22); Android specs: `data-layer`, `realtime-sync` |
| `frontend-list-styling` | Roadmap 4 — list screen MVP (design language) | **done** — `list-screen-mvp` (archived 2026-10-22); Android specs: `list-screen` |
| `frontend-bottom-anchored-list` | Roadmap 4 — list screen MVP | **done** — `list-screen-mvp` (archived 2026-10-22); Android spec: `list-screen` |
| `frontend-modal-design-language` | Roadmap 4+ — modals | partially — add-item overlay in `list-screen-mvp` (archived 2026-10-22); edit/delete modals remain (roadmap 6) |
| `frontend-check-history-undo` | Roadmap 4 — list screen MVP | **done** — `list-screen-mvp` (archived 2026-10-22); Android specs: `check-undo`, `data-layer` (history store) |
| `frontend-undo-snackbar` | Roadmap 4 — list screen MVP | **done** — `list-screen-mvp` (archived 2026-10-22); Android spec: `check-undo` |
| `frontend-add-item-from-search` | Roadmap 4 — list screen MVP | **done** — `list-screen-mvp` (archived 2026-10-22); Android spec: `quick-add` |
| `frontend-view-drawer` | Roadmap 5 — views + widget stack | not started |
| `frontend-shops-management` | Roadmap 5 — views + widget stack | not started |
| `shops-data-model` | Roadmap 2 — data layer | **done** — `data-layer-repositories` (archived 2026-10-22); Android spec: `data-layer` |
| `frontend-widget-stack` | Roadmap 5 — views + widget stack | not started |
| `frontend-sort-mode-toggle` | Roadmap 5 — views + widget stack | not started |
| `frontend-sort-by-frequency` | Roadmap 5 — views + widget stack | not started |
| `frontend-manual-sort-theme` | Roadmap 5 — views + widget stack | not started |
| `frontend-hide-checked-items` | Roadmap 5 — views + widget stack | not started |
| `frontend-shop-unchecked-indicator` | Roadmap 5 — views + widget stack | not started |
| `frontend-expandable-search` | Roadmap 5 — views + widget stack | not started |
| `frontend-fuzzy-search` | Roadmap 5 — views + widget stack | not started |
| `frontend-drag-drop` | Roadmap 5 — views + widget stack | not started |
| `frontend-view-visibility` | Roadmap 6 — feature parity | not started |
| `frontend-item-deletion` | Roadmap 6 — feature parity | not started |
| `frontend-item-quantity` | Roadmap 6 — feature parity | not started |
| `frontend-temporary-items` | Roadmap 6 — feature parity | not started |
| `item-expiry` | Roadmap 6 — feature parity | not started |
| `frontend-change-toasts` | Roadmap 6 — feature parity | not started |
| `frontend-view-persistence` | Roadmap 5 — views + widget stack | not started |
| `frontend-theme-system` | Roadmap 7 — themes (curated set) | not started |
| `frontend-settings-drawer` | Roadmap 7 — themes | not started |
| `frontend-pull-to-refresh` | **dropped** — SSE + refetch-on-reconnect | dropped |
| `frontend-pwa-install` | **dropped** — PWA-specific, natively irrelevant | dropped |
| `database-schema`, `backend-scaffold`, `db-migration-scripts`, `dev-environment-setup`, `local-docker-dev`, `compose-watch-dev-loop`, `monorepo-workspaces`, `prod-deploy-script` | **N/A** — backend reused as-is; never propose backend changes in this repo | n/a |

## 5. Migration Workflow

For each roadmap item:

1. Pick the next item from `docs/ROADMAP.md` (respect order and dependencies).
2. Create a branch; run `/opsx-propose` for the change. **Consult the web specs listed in the traceability matrix** for that item as source material — adapt, don't copy: `localStorage` → DataStore, PWA bits dropped, web gestures re-thought for native touch, theming per the custom design language.
3. Implement with `/opsx-apply`; verify with the safe commands in AGENTS.md.
4. Archive with `/opsx-archive` (syncs delta specs into `openspec/specs/`).
5. Update the traceability matrix status in this file, and check whether `AGENTS.md` / `README.md` / `docs/ROADMAP.md` need updating (per the AGENTS.md post-archive rule).
