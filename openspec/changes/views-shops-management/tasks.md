## 1. Active view state in the list

- [x] 1.1 Add `activeViewId` to `ListViewModel` UI state (null = Global placeholder) with a `onViewSelected(shopId)` action; stop hardcoding the Global shop id in `buildUiState` (TDD: unit tests for filter/sort by selected view, Global default, fallback to Global when active shop is deleted)
- [x] 1.2 Derive `hasUncheckedItems: Map<Long, Boolean>` in `buildUiState` (TDD: unit tests for unchecked/fully-checked/empty shops, Global participation, empty-`visibleIn` counts toward Global)
- [x] 1.3 Update `ListUiState`/watermark to follow the active view; verify list filtering + watermark behavior per the list-screen delta spec

## 2. View drawer UI

- [x] 2.1 Build the drawer composable: left-anchored `AnimatedVisibility` panel over a tap-to-dismiss scrim, `BackHandler`, token-based styling, animated slide in/out
- [x] 2.2 Render shop rows: Global-first ordering from the repository stream, active-shop highlight, hollow-circle indicator from `hasUncheckedItems`, edit (pen) icon on non-Global rows with accessibility labels
- [x] 2.3 Wire selection vs. edit gestures: row tap → `onViewSelected` + close drawer; edit icon tap → open shop modal in edit mode, drawer stays open
- [x] 2.4 Add the full-width "Add shop" button below the list opening the shop modal in create mode
- [x] 2.5 Add the view-switcher icon to the widget stack (gray when closed, active when open) toggling the drawer

## 3. Drawer gestures

- [x] 3.1 Edge-swipe open: left-edge drag detection on the list container with threshold constants; verify no conflict with list scroll/taps
- [x] 3.2 Swipe-to-close: right-to-left drag on the drawer panel and upward drag from the bottom edge while open, with thresholds

## 4. Shop editor modal

- [x] 4.1 Add `ShopModalUiState` (mode, name, saving, error) + ViewModel actions (open create/edit, name change, save, discard) with validation computed properties (TDD: empty name, edit-unchanged, trim-on-save)
- [x] 4.2 Build the modal composable: centered dialog card with scrim, underlined name input as visual title, Save/Discard(/Delete) action row, disabled inputs while saving, back/overlay dismissal, scrollable when content overflows
- [x] 4.3 Wire create/rename through `ShopsRepository`; surface failure (e.g. duplicate 409) as modal error, keep modal open, re-enable input
- [x] 4.4 Add delete confirmation dialog naming the shop; confirm → delete via repository, both dialogs close; cancel → return to edit modal
- [x] 4.5 Verify immediate reflection of mutations in drawer/watermark/list (repository cache + SSE merges), including fallback to Global when the active shop is deleted

## 5. Verification

- [x] 5.1 Run `./gradlew test` and `./gradlew lint`; fix findings
- [ ] 5.2 Build `./gradlew assembleDebug` and smoke-test on device: drawer open/close (icon, edge swipe, swipes, backdrop), view switching with filter + watermark, add/rename/delete shop flows
