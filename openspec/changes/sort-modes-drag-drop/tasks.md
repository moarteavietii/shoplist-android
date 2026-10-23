## 1. Ordering derivations

- [x] 1.1 Add `sortModeEnabled` (default ON) and `sortOrder` (default Manual) to the list UI state, persisted via `ListPreferencesDataSource` (TDD: persistence round-trip, defaults on first launch)
- [x] 2.1 Implement ordering as a pure derivation: Manual (existing fractional sort), A→Z (case-insensitive), Frequency (count desc, case-insensitive name tie-break); switching back to Manual restores the exact stored order (TDD: all three orders, tie-breaks, exact-restore property)
- [x] 1.3 Wire the derivations into `buildUiState` and verify live re-order on check/undo/SSE (TDD: count change re-derives ordering without reload)

## 2. Sort Mode control

- [x] 2.1 Replace the Sort Mode placeholder: `combinedClickable` tap toggles sort mode, long-press opens the anchored sort-order selector (TDD: toggle does not fire on long-press; mode/order persisted)
- [x] 2.2 Build the selector: exactly Manual/A→Z/Frequency, active option indicated, immediate apply + close, closes on outside tap/scroll (TDD: selection applies and closes)
- [x] 2.3 Apply LED dot rules: filled in Manual+ON, hollow in Manual+OFF, none in A→Z/Frequency (active styling kept in A→Z) (TDD: dot derivation per state)

## 3. Drag-and-drop reorder

- [x] 3.1 Add `reorder` to `ItemsRepository`: optimistic cache update with merged `orderMap`, PATCH, rollback + error surface on failure (TDD: MockWebServer success and failure paths)
- [x] 3.2 Add handle rendering to `ItemRow` (only when drag enabled) and handle-initiated drag detection with slop; dragged row renders above all content, displaced rows stay fully visible (TDD: handle visibility gating)
- [x] 3.3 Compute drop target + fractional key on drop; optimistic position held until PATCH completes (TDD: between-neighbors, start-boundary, end-boundary key generation)
- [x] 3.4 Implement suppression: drag disabled while add-item/shop/delete overlays are open, sort mode off, or order A→Z/Frequency; resumes on close (TDD: gating matrix)
- [x] 3.5 Implement the always-visible rule: while manual sort is engaged, all items in the active view are visible regardless of the hide toggle; no list change at drag start (TDD: engage reveals all with no reflow, drag start is inert, disengage restores hiding)

## 4. Verification

- [x] 4.1 Run `./gradlew test` and `./gradlew lint`; fix findings
- [x] 4.2 Build `./gradlew assembleDebug` and smoke-test on device: tap/long-press Sort Mode, all three orders, persistence across restart, drag reorder with optimistic hold and rollback on airplane-mode drop, no drag in A→Z/Frequency or with overlays open (verified on device across the layout/order fixes; airplane-mode rollback still to confirm at archive time)
