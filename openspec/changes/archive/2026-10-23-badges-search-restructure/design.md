## Context

The widget stack (batch 1) has Search and Checked-Items Toggle placeholders; the drawer shows hollow-circle indicators; the list has a persistent bottom search field wired to `fuzzyMatch`. The web specs for this batch (frontend-shop-unchecked-indicator, frontend-expandable-search, frontend-fuzzy-search, frontend-hide-checked-items) define the target behavior. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- Complete the widget stack's five controls (Settings stays a placeholder)
- One reactive derivation for all badge surfaces
- Search restructure with zero fuzzy-semantics change (reuse `FuzzyMatch.kt`)

**Non-Goals:**

- Settings behavior (real burger menu — future change, settled roadmap note)
- Drag behavior itself (batch 2); only the manual-sort override is wired here
- Theme system, feature-parity modals (roadmap 6+)

## Decisions

### Badge counts derived once in the ViewModel
`uncheckedCountByShop: Map<Long, Int>` joins the list UI state, derived in `buildUiState` from items + shops (Global = all items; per-shop = `visibleIn` membership). The drawer and the View Switcher badge both read it — one derivation, all surfaces live via the existing state stream. The View Switcher badge shows the Global count per the web spec (not the sum).

*Alternative:* deriving counts inside the drawer composable. Rejected — split derivation risks drawer/switcher disagreement and duplicates the filter rule.

### Hollow-circle indicator replaced, not extended
The first change's hollow-circle indicator is superseded by the count badge (the web spec evolved the same way). The drawer row drops the circle and renders the pill/muted-zero — one visual language, no dual indicators.

### Search restructure: state moves, matcher stays
`searchQuery` and the fuzzy filter derivation stay in `ListViewModel` unchanged; only the input's presentation moves (bottom persistent field → stack-toggled expandable input). New state: `searchExpanded` (persisted via `ListPreferencesDataSource`), query never persisted. Expand animation: `AnimatedVisibility` (expandVertically + fade) at the bottom of the list-area `Box`; focus via the existing `FocusRequester` pattern; swipe-right dismiss reuses the drag-accumulator pattern from `DrawerGestures.kt`.

*Alternative:* `SwipeToDismissBox`. Rejected — it's a Material-chrome component with anchored states; a simple threshold drag matches the chromeless aesthetic and the drawer's existing gesture code.

### Hide-checked as a list-derivation rule
`showChecked: Boolean` (persisted, default true) filters the derivation: `items.filter { showChecked || !it.isChecked || queryMatches(it) || manualSortEngaged }`. The search-override and manual-sort-override rules live in this single expression — one place, unit-testable, no UI branching. The manual-sort override (all items visible while sort mode is ON and Manual order) supersedes the web spec's "reveal at drag start" rule: engaging manual sort reveals everything at once, so a drag start never reflows the list.

### Clear button and touch targets
One clear button while the query is non-empty, 48dp minimum target (the app's existing convention; the web's 44px maps up).

## Risks / Trade-offs

- [Badge pill may overlap row label on long names] → Badge is trailing with fixed width; label ellipsizes; instrumented check on narrow screens.
- [Search restructure may regress keyboard insets] → The existing `WindowInsets.ime` handling in `ListScreen` is preserved; the input stays bottom-anchored with `imePadding()`.
- [Hide-checked + manual-sort override interaction] → Single-derivation rule (`|| manualSortEngaged`) keeps it correct; unit-tested matrix (toggle × query × manual sort).
- [Fuzzy semantics drift] → `FuzzyMatch.kt` is reused as-is; a task verifies the web spec's "out-of-order within the word" nuance against the current in-order subsequence implementation and aligns tests if needed.

## Migration Plan

Single `:app` change on a feature branch (branched from batch 2's branch). Rollback = revert. No data migration; two new DataStore keys.

## Open Questions

None — behavior is fully specified by the web specs (frontend-shop-unchecked-indicator, frontend-expandable-search, frontend-fuzzy-search, frontend-hide-checked-items).
