## Why

The final roadmap-5 batch: quiet at-a-glance awareness of unchecked work (count badges) and the search restructure that moves the bottom search field into the widget stack as the web app's expandable search. This completes the widget stack's five controls and the drawer's at-a-glance indicators.

## What Changes

- Add trailing count badges to view drawer shop rows: accent pill with the count of unchecked items visible in that shop; muted non-accent "0" for fully-checked/empty shops; Global counts all items
- Add a count badge to the widget stack's View Switcher control: the Global unchecked count (not the sum of per-shop counts), hidden at 0, purely visual (no accessibility semantics, never blocks gestures)
- Implement the widget stack's Search control (replacing its placeholder): tap expands to a full-width bottom-anchored input; LED dot hollow when collapsed, filled when expanded
- Move the existing bottom search field into the stack's expandable pattern: focus on expand, one clear button (≥48dp target), swipe-right dismiss, collapsing clears the query, expanded state persisted but the query never persisted
- Wire the existing fuzzy matcher (`FuzzyMatch.kt`) into the restructured input: case-insensitive out-of-order subsequence matching, search overrides the later hide-checked toggle (rule established, inert until then), empty results expose the add-item button
- Implement the widget stack's Checked-Items Toggle control (replacing its placeholder): show/hide checked items with animated height+fade transitions, persisted, search overrides it
- Settings remains the only placeholder after this change (real burger menu is a future change per the settled roadmap note)

## Capabilities

### New Capabilities

- `shop-unchecked-indicator`: per-shop unchecked counts in drawer rows and the View Switcher badge, with live updates and quiet (non-semantic) presentation
- `expandable-search`: widget-stack search control expanding to a bottom-anchored input with focus, clear button, swipe dismiss, and persistence of expanded state (not the query)
- `hide-checked-items`: show/hide checked items toggle with animations, persistence, and the search-override rule

### Modified Capabilities

- `widget-stack`: the Search and Checked-Items Toggle placeholders become real controls; the Search slot gains its LED dot rule
- `list-screen`: the bottom search field is replaced by the stack's expandable input; the list derivation gains hide-checked filtering with search override
- `view-drawer`: shop rows gain trailing unchecked-count badges (replacing the hollow-circle indicator from the first change)

## Impact

- `ui/drawer/ViewDrawer.kt` — hollow-circle indicator replaced by count badges
- `ui/widgetstack/WidgetStack.kt` — Search + Checked-Items Toggle become real; badge overlay on View Switcher
- `ui/list/ListScreen.kt` + `ListViewModel.kt` + `ListUiState.kt` — search restructure (bottom field removed, expandable input added), hide-checked derivation
- `data/prefs/ListPreferencesDataSource` — show-checked + search-expanded keys
- `domain/search/FuzzyMatch.kt` — reused as-is (verify semantics match the web spec: case-insensitive, out-of-order subsequence)
- No backend changes; no new dependencies
