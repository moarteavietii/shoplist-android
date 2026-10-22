## Why

The app foundation (scaffold, data layer, realtime SSE sync) is complete, but there is no shopping list screen yet — the app cannot actually be used to track items. Roadmap item 4 (List screen MVP) is the first user-facing feature and the core of the app: displaying items, checking them off, undoing check-offs, and quick-adding new items from search.

## What Changes

- New list screen (Compose) replacing the placeholder home content, rendering items for the active view (Global for MVP) using the custom design language (chromeless, borderless rows, compact spacing, centered low-opacity view-name watermark, hidden scrollbar)
- Bottom-anchored list layout: few items sit at the bottom of the viewport and grow upward; long lists scroll normally with the first item fully reachable; expanded search reserves space at the bottom and the list never scrolls behind it; keyboard insets keep active items visible
- Check-off with instant optimistic commit: tap flips the row immediately, `PATCH /api/items/:itemId` fires right away, failed PATCH reverts the flip; no countdown or pending state
- Device-local check history + undo: every successful own-device check/uncheck is recorded (`{ itemId, from, to, at }`), capped at 50 entries with 30-minute TTL, persisted across app restarts, global across views; undo reverts the most recent valid entry via a PATCH carrying `isUndo: true`; stale/expired entries are skipped silently; undo affordance is a snackbar-style bar at the bottom
- Quick add from empty search: search field with subsequence fuzzy matching; when a query matches nothing, an add affordance opens an item-creation modal pre-filled with the query, with multi-view visibility checklist (Global first, all selected by default, Select all/Deselect all toggle that never deselects Global), Save/Discard actions, focus returning to search after save
- Checked-row fly-out animation when hide-checked mode is enabled (row translates right and fades)

## Capabilities

### New Capabilities
- `list-screen`: List screen rendering and layout — chromeless rows, compact spacing, view-name watermark, hidden scrollbar, bottom anchoring, search-expansion space reservation, keyboard visibility handling
- `check-undo`: Check-off commit semantics and device-local undo history — optimistic flip + PATCH, 50-entry/30-min-TTL persisted history, undo validation (stale/expired skip), `isUndo` flag semantics, fly-out animation
- `quick-add`: Item creation from empty search — fuzzy search, add modal with multi-view visibility selection, save/discard, focus management

### Modified Capabilities
- `data-layer`: ItemsRepository gains check/uncheck mutation with optimistic update + rollback support, and item creation; undo history store (device-local, persisted) added to the data layer

## Impact

- New UI: `ui/list/` (screen, rows, search bar, add-item modal, undo bar) + `ui/list/ListViewModel.kt`
- Data layer: `ItemsRepository` (check mutation, create), new `UndoHistoryStore` (DataStore-backed persistence), wiring in Hilt modules
- Navigation: MainActivity home content switches from health placeholder to the list screen (auth-gated as today)
- API usage: existing `PATCH /api/items/:itemId` (with `isUndo`), `POST /api/lists/:listId/items` — no backend changes
- Dependencies: none new (Compose, DataStore already available or already in use)
