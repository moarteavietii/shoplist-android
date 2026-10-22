## Why

The list currently supports only manual fractional-index ordering with no way to change it. The web app's sort system — a sort-mode toggle (drag reordering on/off) plus a Manual / A→Z / Frequency order selector — is the next roadmap-5 batch, and drag-drop reordering depends on it being in place.

## What Changes

- Implement the widget stack's Sort Mode control (replacing its placeholder): tap toggles manual drag-and-drop reordering on/off; long-press opens a sort-order selector (Manual / A→Z / Frequency) anchored to the button
- Add frequency ordering: items ordered by all-time check-off count descending with case-insensitive alphabetical tie-break; counts change per the backend contract (+1 on check, unchanged on uncheck, −1 only via device-local undo)
- Add drag-and-drop reordering for the active view: drag initiates only from a dedicated row handle, optimistic reorder with fractional indexing, `PATCH` with merged `orderMap`
- Drag start reveals hidden checked items (interacts with the later hide-checked batch; until then all items are always visible, so this rule is established but inert)
- Suppress drag while overlays are open (add-item, shop modal, delete confirmation) and when sort mode is off or order is A→Z/Frequency
- Persist sort mode (default ON) and sort order (default Manual) across sessions
- Do NOT port the removed `frontend-manual-sort-theme` auto-theme behavior — sort-mode visibility is carried by the LED dot only

## Capabilities

### New Capabilities

- `sort-modes`: sort mode toggle (manual reordering on/off) and sort order selection (Manual / A→Z / Frequency) with LED dot rules, persistence, and drag gating
- `frequency-sort`: check-count semantics (increment/decrement rules) and frequency ordering with live re-ordering
- `drag-drop-reorder`: handle-initiated per-view drag-and-drop reordering with fractional indexing, optimistic updates, drag-reveal of hidden items, and suppression rules

### Modified Capabilities

- `widget-stack`: the Sort Mode placeholder becomes a real control with tap/long-press interactions and an anchored sort-order selector
- `list-screen`: item ordering becomes mode-dependent (Manual / A→Z / Frequency) instead of always manual; rows gain a drag handle when drag is enabled

## Impact

- `ui/list/ListViewModel.kt` + `ListUiState.kt` — sort mode/order state, ordering derivations, drag handlers
- `ui/list/ItemRow.kt` — drag handle rendering, drag state visuals
- `ui/widgetstack/WidgetStack.kt` — Sort Mode control becomes real (tap + long-press + anchored selector)
- `data/repo/ItemsRepository.kt` — reorder mutation (merged `orderMap` PATCH); `checkCount` already in the item model (verify)
- `data/prefs/ListPreferencesDataSource` — sort mode + order keys (from batch 1's foundation)
- New dependency likely: a Compose drag-and-drop approach (custom handle-based drag detection preferred over a library — KISS)
- No backend changes; fractional-index key format already ported with unit tests
