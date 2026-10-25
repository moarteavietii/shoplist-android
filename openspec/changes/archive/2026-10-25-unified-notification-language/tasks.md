## Tasks

### Theme tokens

- [x] Add `surfaceInverse` and `contentInverse` to `ShoplistColors` and derive them in all palettes (mechanical content↔background inversion); add a unit test asserting both tokens are populated for every theme.

### Toast redesign (notification-surface)

- [x] Add `severity` (`Info`/`Danger`) to `ChangeToast` and thread it through `ChangeToastDerivation` (change toasts → `Info`); update `ChangeToastDerivationTest`.
- [x] Restyle `ChangeToastOverlay` as the unified pill: inverse surface background, full-pill shape, soft shadow, single-line text with ellipsis, leading severity LED (standard LED color for `Info`, `danger` for `Danger`); keep horizontal swipe dismissal and add tap-to-dismiss.
- [x] Add overlay styling tests (pill renders with inverse surface + LED; tap dismisses; swipe dismisses).

### Failure toasts (notification-surface)

- [x] Route transient failures in `ListViewModel` ("Couldn't refresh the list", "Couldn't save the item", "Couldn't move the item", "Couldn't delete the item") through the toast system as `Danger` toasts; clear on operation success as today; remove the `errorMessage` bottom text and its AnimatedVisibility block from `ListScreen`.
- [x] Update `ListViewModelTest` for failure-toast behavior (failure posts Danger toast; success clears).

### Offline chip (notification-surface)

- [x] Add a `StatusChip` composable (LED dot + label, chrome/surface pill styling) and render it in the list screen header column while `isOffline` is true with label "Offline — changes will be queued"; remove the plain offline text block.
- [x] Add a test for chip visibility driven by the offline state (visible while offline, gone on reconnect).

### Quick menu effective visibility (quick-menu, hide-checked-items)

- [x] Extend `deriveControlLedState` and the `isActive` computation for `CHECKED_ITEMS_TOGGLE` to use `isShowingCheckedItems || isManualSortEngaged`; pass `isManualSortEngaged` (`sortModeEnabled && sortOrder == MANUAL`) into `QuickMenu` from `ListScreen`.
- [x] In `ListViewModel.onQuickMenuControlTapped(CHECKED_ITEMS_TOGGLE)`, when manual sort is engaged, post the rejection message "Checked items stay visible while manual sorting is engaged" as a `Danger` toast and leave the preference unchanged.
- [x] Update LED-derivation and `ListViewModelTest` tests: LED filled during manual sort with preference off; tap during manual sort posts rejection toast and doesn't toggle; toggle returns to preference after manual sort disengages.

### Verification

- [x] Run `./gradlew test` and `./gradlew lint`; fix any failures.
- [x] Smoke-test on the device: change toast styling, airplane-mode offline chip, failure toast on refresh with backend down, manual-sort toggle rejection.
