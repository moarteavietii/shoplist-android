## Why

Roadmap item 5 (views/shops + widget stack) is the next migration step after the list screen MVP. The app currently shows all items with a hardcoded "Global" watermark — users cannot switch shopping contexts or manage shops. This change ports the web app's view drawer and shop management modal, the two foundational pieces of that roadmap item.

## What Changes

- Add a left-aligned slide-out view drawer listing all shops (Global first, then A→Z), with hollow-circle indicators for shops holding unchecked items, active-shop highlighting, and an "Add shop" button
- Add drawer gestures: open via view-switcher icon or left-edge swipe, close via backdrop tap, right-to-left swipe, or upward swipe from the bottom
- Add a shop editor modal (create/edit modes) with name validation, trimmed saves, explicit dismissal paths, and confirmed deletion
- Wire shop mutations (create/rename/delete) through the existing `ShopsRepository` so the drawer list updates immediately, including via realtime SSE events
- Introduce an active-shop concept in the list screen: selecting a shop filters items to those visible in it and updates the watermark
- **BREAKING**: the list no longer shows all items regardless of context — items are filtered by the active shop's `visibleIn` set (previously the watermark was fixed to "Global" and no filtering existed)

## Capabilities

### New Capabilities

- `view-drawer`: slide-out drawer for switching the active shop — shop list ordering, unchecked-item indicators, selection vs. edit gesture separation, open/close animations and swipe gestures, add/edit entry points
- `shops-management`: shop editor modal — create/edit modes, name validation and trimming, dismissal paths, deletion with confirmation, immediate reflection of mutations across surfaces

### Modified Capabilities

- `list-screen`: the active view becomes user-selectable (via the drawer) instead of fixed; selecting a shop filters the list to items whose `visibleIn` contains the active shop id, and the watermark follows the active shop

## Impact

- `ui/list/` — `ListScreen`, `ListViewModel`, `ListUiState` gain active-shop state and filtering; new composables for the drawer and shop modal (likely `ui/drawer/`, `ui/shops/`)
- `data/repo/ShopsRepository` — already supports create/rename/delete + realtime merges; consumed as-is
- `data/model/Item` — `visibleIn` already present; used for filtering and drawer indicators
- No backend changes; no new dependencies expected (Compose animation + gesture APIs already available)
- Unit tests: filtering logic, drawer indicator derivation, modal validation; fakes per testing convention
