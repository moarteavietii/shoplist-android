## Context

The list sorts by `orderMap[activeView]` fractional keys only (`ListViewModel.sortManual`); `checkCount` is already in the item model and the fractional-indexing port (`domain/order/FractionalIndexing.kt`) has unit tests. Batch 1 lands the widget stack with a Sort Mode placeholder and the `ListPreferencesDataSource` this change persists into. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- Ordering as a pure derivation: (items, order, mode) → ordered list, unit-testable without Compose
- Drag via the `sh.calvin.reorderable` library (handle-based, live rearrangement), not hand-rolled gesture math
- Optimistic reorder reusing the repository's existing cache-merge patterns

**Non-Goals:**

- Hide-checked behavior (batch 3; the always-visible-while-manual-sort rule is established but inert)
- Keyboard drag accessibility (descoped — pointer drag only; revisit later)
- The removed auto-theme behavior (never ported)
- Edit-modal "Checked N times" line (belongs to the edit/delete modals batch, roadmap 6)

## Decisions

### Ordering as a pure derivation in the ViewModel
`buildUiState` gains a `sortOrder`/`sortModeEnabled` input and derives ordering: Manual → existing `sortManual`, A→Z → `compareBy(String.CASE_INSENSITIVE_ORDER)`, Frequency → `compareByDescending(checkCount).thenBy(name)`. Frequency never touches the stored manual order — it's a view derivation, so switching back is exact by construction. Live re-ordering falls out of the existing reactive state (items flow + SSE merges).

*Alternative:* pre-sorting in the repository. Rejected — the repository holds server truth (manual order); mode-dependent ordering is presentation.

### Frequency semantics ride the existing check/undo paths
`checkCount` comes from the backend; the app's only count mutation is the undo path (`isUndo: true` PATCH, already implemented in `check-undo`). No new count logic is needed — the spec pins the contract so tests can pin the ported behavior.

### Drag: `sh.calvin.reorderable` (revised during implementation)
The initial design used custom `pointerInput` drag detection on the handle. During device testing this produced a visible jump when rows swapped (two independent animation clocks: the dragged row's offset vs. displaced rows' `animateItem()` placement), and hand-rolling the coordination was disproportionate. The implementation now uses `sh.calvin.reorderable` 3.1.0: `rememberReorderableLazyListState` drives live reordering of a working-copy `dragOrder` list, `ReorderableItem` manages the dragged row's offset (fixing the jump), and `Modifier.draggableHandle()` keeps initiation handle-only per the spec. Drop still computes a fractional key via `dropKeyForIndex`/`generateKeyBetween` and persists through the same optimistic repository path.

*Alternative:* custom handle-based drag (~100 lines). Rejected after device testing — the library is the de-facto Compose standard for reorderable lists and coordinates the drag/placement animations correctly out of the box.

### List layout: top-to-bottom key order, bottom-anchored content
The `LazyColumn` renders items in ascending order-key order (index 0 at the top), matching the web app. `reverseLayout` was removed after device testing showed it mirrored the web order; short lists stay bottom-anchored via `verticalArrangement = Arrangement.Bottom`. The post-save scroll targets the last index (newest item, highest key).

### Optimistic reorder in ItemsRepository
`reorder(itemId, orderMap)` updates the cache first, then PATCHes; on failure it restores the previous cache entry and the existing error-state path surfaces it. This mirrors the existing check-off optimistic pattern.

### Sort-order selector as a token-styled Popup
Raw `Popup` anchored to the Sort Mode control (not M3 `DropdownMenu` — its colors are hard-coded by `MenuDefaults` and ignore the token system, which produced a white-on-white menu). The popup uses `colors.surface` + `shapes.medium`, LED-dot active-state markers, and opens to the left of the control via a negative x-offset from the row's `TopEnd` alignment. Outside-tap dismissal via `focusable = true`; long-press via `combinedClickable`.

### All-items-visible while manual sort is engaged (replaces drag-reveal)
The web spec's "reveal hidden items at drag start" rule is replaced: while manual sort is engaged (sort mode ON and Manual order), all items in the active view are always visible regardless of the hide toggle. The derivation is `if (manualSortEngaged) allItems else visibleItems` — engaging manual sort reveals everything at once, so a drag start never reflows the list. This supersedes the drag-reveal rule in the later hide-checked batch.

## Risks / Trade-offs

- [Drag gesture conflicts with LazyColumn scrolling] → Handle-only initiation via `draggableHandle` isolates the gesture; displaced-row visibility and no-render-loop requirements are covered by stable item keys (already `item.id`) and unit-tested ordering derivations.
- [Frequency live reorder during SSE bursts] → Ordering is a pure derivation on the existing state flow; recomposition is idempotent.
- [Optimistic reorder + concurrent SSE update] → Repository merge rules are diff-checked already; the reorder PATCH response is authoritative and re-applied.
- [Keyboard drag descoped] → Noted as a non-goal; the web spec's keyboard drag may be revisited later.

## Migration Plan

Single `:app` change on a feature branch (branched from batch 1's branch — it consumes `ListPreferencesDataSource`). Rollback = revert. No data migration; fractional keys unchanged.

## Open Questions

None — the web specs (frontend-sort-mode-toggle, frontend-sort-by-frequency, frontend-drag-drop) fully define behavior, and `frontend-manual-sort-theme` is settled as removed upstream (not ported).
