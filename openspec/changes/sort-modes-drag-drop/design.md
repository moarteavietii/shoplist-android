## Context

The list sorts by `orderMap[activeView]` fractional keys only (`ListViewModel.sortManual`); `checkCount` is already in the item model and the fractional-indexing port (`domain/order/FractionalIndexing.kt`) has unit tests. Batch 1 lands the widget stack with a Sort Mode placeholder and the `ListPreferencesDataSource` this change persists into. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- Ordering as a pure derivation: (items, order, mode) → ordered list, unit-testable without Compose
- Drag as a self-contained row-level concern (handle + custom drag detection), no DnD library
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

### Drag: custom handle-based detection, no library
A `pointerInput` on the handle starts a drag after a small slop; the row's y-offset follows the drag; on drop, `FractionalIndexing.generateKeyBetween` computes the new key. Compose has no built-in reorder container that fits the chromeless design; a library (e.g. a Compose DnD port) is a heavy dependency for one list. The drag-accumulator pattern from `DrawerGestures.kt` is reused.

*Alternative:* a Compose DnD library. Rejected — none is established in the catalog, and handle-based custom drag is ~100 lines with full control over the "float above" and "no reflow" rules.

### Optimistic reorder in ItemsRepository
`reorder(itemId, orderMap)` updates the cache first, then PATCHes; on failure it restores the previous cache entry and the existing error-state path surfaces it. This mirrors the existing check-off optimistic pattern.

### Sort-order selector as an anchored Popup
`DropdownMenu` anchored to the Sort Mode control — unthemed M3 popup per the "unthemed component toolkit" rule, with token-styled content. Outside-tap and scroll-to-close are free with `DropdownMenu`; long-press via `combinedClickable`.

### All-items-visible while manual sort is engaged (replaces drag-reveal)
The web spec's "reveal hidden items at drag start" rule is replaced: while manual sort is engaged (sort mode ON and Manual order), all items in the active view are always visible regardless of the hide toggle. The derivation is `if (manualSortEngaged) allItems else visibleItems` — engaging manual sort reveals everything at once, so a drag start never reflows the list. This supersedes the drag-reveal rule in the later hide-checked batch.

## Risks / Trade-offs

- [Custom drag detection may fight LazyColumn scrolling] → Handle-only initiation isolates the gesture; displaced-row visibility and no-render-loop requirements are covered by stable item keys (already `item.id`) and unit-tested ordering derivations.
- [Frequency live reorder during SSE bursts] → Ordering is a pure derivation on the existing state flow; recomposition is idempotent.
- [Optimistic reorder + concurrent SSE update] → Repository merge rules are diff-checked already; the reorder PATCH response is authoritative and re-applied.
- [Keyboard drag descoped] → Noted as a non-goal; the web spec's keyboard drag may be revisited later.

## Migration Plan

Single `:app` change on a feature branch (branched from batch 1's branch — it consumes `ListPreferencesDataSource`). Rollback = revert. No data migration; fractional keys unchanged.

## Open Questions

None — the web specs (frontend-sort-mode-toggle, frontend-sort-by-frequency, frontend-drag-drop) fully define behavior, and `frontend-manual-sort-theme` is settled as removed upstream (not ported).
