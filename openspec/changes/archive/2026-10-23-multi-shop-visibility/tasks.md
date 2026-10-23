## 1. Verification against spec

- [x] 1.1 Compare the edit-modal checklist implementation (`AddItemModal.kt`, `ListViewModel.kt`, `ListUiState.kt`) against each `item-visibility` requirement; record any gaps
  - Gaps found: (a) secondary button always reads "Discard" — spec requires "Close" when unchanged; (b) missing-Global fallback not implemented (also not implemented in the web app reference)
- [x] 1.2 Verify shop ordering (Global first, then A→Z) and the missing-Global fallback in `orderViewsGlobalFirst` / modal state
  - Ordering verified: `orderViewsGlobalFirst` sorts Global first then case-insensitive A→Z; covered by `ViewOrderingTest` and modal tests. Missing-Global fallback: NOT implemented (see gap b)

## 2. Gap fixes

- [x] 2.1 Fix any gaps found in task 1 (e.g. Close/Discard labeling, auto-focus behavior), keeping changes minimal
  - Fixed: `EditItemUiState.secondaryLabel` ("Close" when unchanged, "Discard" when changed) wired into `EditItemModal`; add modal keeps static "Discard". Missing-Global fallback: `orderViewsGlobalFirst` prepends a display-only Global with sentinel id `GLOBAL_FALLBACK_ID` (-2); both save paths filter the sentinel out of `visibleIn`. Auto-focus: verified not needed (Compose TextField doesn't auto-focus).
- [x] 2.2 Confirm save-diffing omits `visibleIn` when the selection is unchanged and includes it when changed
  - Confirmed: `onEditItemSave` sets `visibleIn` only when `selectedViewIds != originalVisibleIn`; `explicitNulls = false` in NetworkModule omits null fields from the wire format. Covered by tests `edit modal save with unchanged name does not patch` and `edit modal save with only visibility change patches visibleIn and closes`

## 3. Tests

- [x] 3.1 Add unit tests for `EditItemUiState` visibility rules: pre-selection from `visibleIn`, `canSave` on selection change, blank-name disable, unchanged-selection save omission
  - Added to `ItemModalUiStateTest`: secondary-label Close/Discard rules, pre-selection from `visibleIn`, selection-change enables save / unchanged keeps disabled (blank-name disable already covered). Added `missing global gets display-only fallback first` to `ViewOrderingTest`
- [x] 3.2 Add ViewModel tests: toggle-all rules (select all; deselect all but Global), discard reverts selection, save persists `visibleIn` via `updateDetails`
  - Added `edit modal toggle all selects everything and deselects all but Global` and `edit modal save strips fallback global sentinel from visibleIn`. Toggle-all rules for the add modal, discard-without-request, and save-persists-visibleIn were already covered by existing tests
- [x] 3.3 Add/extend realtime test: remote `item:updated` with changed `visibleIn` re-filters the active view
  - Added `remote item updated with changed visibleIn re-filters active view`: item visible in Auchan view, remote update removes Auchan from `visibleIn`, item disappears from the active view's filtered list

## 4. Verification

- [x] 4.1 Run `./gradlew test` and `./gradlew lint`; fix failures
  - Both pass. Initial run caught that the fallback inside `orderViewsGlobalFirst` corrupted the shops cache (the repository reuses it for sorting); fixed by splitting into a pure sort plus a separate `modalAvailableViews` helper used only by the item modals
- [x] 4.2 Run a device smoke test of the edit-modal visibility flow (toggle shops, save, verify list re-filters)
  - Passed end-to-end: modal pre-selection from `visibleIn` correct; secondary label "Close" when unchanged, flips to "Discard" on toggle; Save disabled while unchanged; save persisted `visibleIn [1,3,4]` (Auchan 2 added); item appeared in Auchan 2 view; remote PATCH removing Auchan 2 re-filtered the list live via SSE; Global view restored
