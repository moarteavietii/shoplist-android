## 1. Toast derivation

- [x] 1.1 Add `deriveChangeToasts(event, previousItem)` pure function producing labels for checked / unchecked / created / deleted / renamed / quantity / visibility changes, with no derivation for reorders and no-op updates
- [x] 1.2 Add unit tests for derivation: each label kind, multi-field update ordering, no-op suppression, delete using the pre-delete name

## 2. Preference

- [x] 2.1 Add persisted `changeToastsEnabled` (default true) to `ListPreferencesDataSource` with tests
- [x] 2.2 Honor the preference in the toast layer (disabled → no toasts) without touching the SSE connection

## 3. Sync integration

- [x] 3.1 Hook derivation into `RealtimeSyncManager` item-event merging: capture previous item, derive labels, emit via callback/flow; suppress when `originDeviceId` matches the device id
- [x] 3.2 Extend `RealtimeSyncManagerTest`: own-device echo silent, remote event emits labels, no-op emits nothing

## 4. Toast UI

- [x] 4.1 Add toast state to `ListViewModel` (single `StateFlow<ChangeToast?>` + queue, 3.5 s auto-dismiss, in-place replacement)
- [x] 4.2 Build the toast composable over `ListScreen`: design-language tokens, slide-in entrance on first appearance, fade-out on auto/programmatic dismiss, horizontal swipe dismissal with directional exit
- [x] 4.3 Add ViewModel tests: replacement restarts the timer, queue drains one at a time, disabled preference suppresses display

## 5. Verification

- [x] 5.1 Run `./gradlew test` and `./gradlew lint`; fix failures
- [x] 5.2 Run a device smoke test: two-device or simulated remote event produces a toast; own changes stay silent; swipe and auto-dismiss work
