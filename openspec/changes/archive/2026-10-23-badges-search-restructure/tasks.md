## 1. Unchecked-count badges

- [x] 1.1 Derive `uncheckedCountByShop: Map<Long, Int>` in `buildUiState` (Global = all items; per-shop = `visibleIn` membership) (TDD: per-shop counts, Global counts all, multi-shop counted once, muted-zero rule)
- [x] 1.2 Replace the drawer's hollow-circle indicator with trailing count badges: accent pill for ≥1, muted non-accent "0" otherwise; label left, badge right, excluded from accessibility semantics (TDD: badge derivation per shop state)
- [x] 1.3 Add the View Switcher badge: Global unchecked count, hidden at 0, purely visual (no semantics, never blocks gestures, accessible name unchanged) (TDD: Global-count rule, hidden-at-zero)
- [x] 1.4 Verify live updates: check/undo/SSE event updates drawer rows and switcher badge at once (TDD: one event updates all surfaces)

## 2. Search restructure

- [x] 2.1 Add `searchExpanded` (persisted) to the list state; replace the persistent bottom field with the stack-toggled expandable input (TDD: expansion state persisted, query never persisted)
- [x] 2.2 Build the expandable input: bottom-anchored, focus on expand, keyboard insets preserved, animated expand/collapse, list reserves space above (TDD: derivation keeps items above input)
- [x] 2.3 Add the clear button (exactly one while query non-empty, ≥48dp target) and swipe-right dismiss with threshold; collapsing clears the query (TDD: clear restores full list, dismiss threshold)
- [x] 2.4 Replace the Search placeholder with the real control; LED dot hollow collapsed / filled expanded (TDD: dot derivation)
- [x] 2.5 Verify fuzzy semantics against the web spec (case-insensitive, out-of-order subsequence); align `FuzzyMatch.kt` tests if the "within the word" nuance differs (TDD: "mlk" matches "Milk")
- [x] 2.6 Wire the empty-results add affordance next to the input (hidden when results exist or query empty; pre-fills the add-item overlay) (TDD: affordance gating)

## 3. Hide-checked toggle

- [x] 3.1 Add `showChecked` (persisted, default showing) to the list state; implement the single-derivation rule with search override and manual-sort override (all items visible while manual sort is engaged; no reflow at drag start) (TDD: toggle × query × manual-sort matrix)
- [x] 3.2 Replace the Checked-Items Toggle placeholder with the real control: LED dot per state, pressed state exposed to accessibility (TDD: dot + pressed-state semantics)
- [x] 3.3 Add hide/show animations: shrink+fade out on check, grow+fade in on re-enable, undo restores an item that animated out (TDD: commit-instant property regardless of toggle)

## 4. Verification

- [x] 4.1 Run `./gradlew test` and `./gradlew lint`; fix findings
- [x] 4.2 Build `./gradlew assembleDebug` and smoke-test on device: badges on drawer rows + switcher (live updates), search expand/collapse/swipe-dismiss/clear, fuzzy query, hide-checked with animations, search override, persistence across restart
