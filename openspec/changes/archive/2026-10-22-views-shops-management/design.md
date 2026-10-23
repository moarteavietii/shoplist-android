## Context

The list screen MVP (`list-screen-mvp`, archived) renders items with a hardcoded Global watermark: `ListViewModel.buildUiState` already filters by the Global shop's id and sorts by `orderMap[viewId]`. `ShopsRepository` already exposes create/rename/delete plus idempotent realtime merges, and `Item.visibleIn` is populated. What's missing is entirely UI-layer: a drawer to switch the active view, a modal to manage shops, and user-selectable active-view state. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- Single source of truth for the active view, shared by the list, drawer, and shop modal
- Drawer and modal as pure Compose state (no navigation library), consistent with the existing overlay approach
- Immediate reflection of shop mutations via the existing repository cache + SSE merges

**Non-Goals:**

- Sort modes, hide-checked, LED dots/badges, expandable fuzzy search, drag-drop, view persistence (remaining roadmap 5 items — later changes)
- Room/offline, theme system, feature-parity modals (roadmap 6+)
- Backend changes of any kind

## Decisions

### Active view state lives in ListViewModel, keyed by shop id
`activeViewId: Long?` (null = Global placeholder before shops load) becomes part of the list UI state, selected via the drawer. `buildUiState` already takes `shops` and filters/sorts by a view id — it just stops hardcoding the Global shop's id. The drawer reads the same state, so drawer highlight, watermark, and filtering can never disagree.

*Alternative considered:* a separate `ViewsViewModel` or data-layer active-view store. Rejected — the list screen is the only consumer today, and a second observable would risk split-brain state. Revisit if the widget stack or other screens need the active view.

### Drawer as AnimatedVisibility composable, not ModalDrawer
The web aesthetic is chromeless; the design language already hand-rolls overlays (add-item overlay with scrim + back handling). The drawer follows the same pattern: a left-anchored `AnimatedVisibility` panel over a tap-to-dismiss scrim, with `BackHandler` for the back gesture. This avoids Material drawer chrome and keeps token-based styling.

*Alternative considered:* `ModalNavigationDrawer` (M3). Rejected — brings Material drawer geometry/defaults that fight the custom design language.

### Gestures: edge-swipe open, panel-swipe close, bottom-swipe close
- Open: `pointerInput` on the list container detecting a drag starting within ~24dp of the left edge with rightward velocity/distance beyond a threshold. Edge-only detection prevents conflict with list scrolling and future drag-drop reordering.
- Close: right-to-left drag on the drawer panel, and upward drag from the bottom edge while open.
- Thresholds (~48dp distance or ~600px/s velocity) live in one constants file so they're testable and tunable.

*Alternative considered:* `AnchoredDraggable` for full gesture-follow animation. Deferred — the spec requires animated open/close and reliable thresholds, not finger-tracking; `AnimatedVisibility` + discrete gesture detection is simpler (KISS). Revisit if the feel disappoints.

### Shop modal mirrors the add-item overlay pattern
A `ShopModalUiState` in the ViewModel (create/edit mode, name, saving flag, error) rendered as a centered dialog card with scrim, following `AddItemModal`'s structure and token styling. Deletion confirmation is a second lightweight dialog stacked on top, naming the shop. Validation (trimmed non-empty, edit-mode requires change) is computed properties on the UI state, mirroring `AddItemUiState.canSave`.

*Alternative considered:* putting shop state in a dedicated ViewModel. Rejected — mutations go through `ShopsRepository` which is already injected in `ListViewModel`; a second VM would need the same dependencies for no benefit.

### Drawer indicators derived in the ViewModel
`hasUncheckedItems: Map<Long, Boolean>` (shop id → has ≥1 unchecked visible item) is computed in `buildUiState` from items + shops, so the drawer gets it from the same state stream as everything else. Global participates like any shop; `visibleIn.isEmpty()` items count toward Global only (matching the existing filter rule that empty `visibleIn` = Global-only).

### Realtime consistency
Shop create/rename/delete go through `ShopsRepository` (cache updates + sort). SSE `shop:created/updated/deleted` events already merge idempotently. The drawer, watermark, and filter all derive from the same `shops` StateFlow, so no extra wiring is needed. If the active shop is deleted remotely, the app falls back to the Global view.

## Risks / Trade-offs

- [Edge-swipe may misfire on list interactions] → Edge-zone detection (start x < 24dp) plus threshold; instrumented test for the "starts elsewhere" scenario.
- [Active shop deleted remotely while viewing it] → Fall back to Global view; covered by a unit test.
- [Drawer + modal + confirmation stacking] → Only one dialog stack path exists (drawer → shop modal → confirm delete); state is a sealed set of nullable UI states, validated by unit tests.
- [Dislikes-Material constraint] → All new composables styled via `LocalShoplistColors/Shapes/Typography` tokens; M3 components used only unthemed where convenient (e.g. `TextField` as in `AddItemModal`).

## Migration Plan

Single `:app` change on a feature branch; no data migration. Rollback = revert the branch. Backend contract unchanged (`/api/shops` endpoints already consumed).

## Open Questions

None — the web specs (frontend-view-drawer, frontend-shops-management) fully define behavior, and the decisions log settles design-language and lifecycle questions.
