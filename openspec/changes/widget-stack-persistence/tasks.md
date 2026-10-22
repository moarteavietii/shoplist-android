## 1. Preferences foundation

- [ ] 1.1 Create `data/prefs/ListPreferencesDataSource` (DataStore Preferences) with keys for widget-stack-expanded and active-shop-id; suspend API + StateFlow on the IO dispatcher (TDD: fake-based tests for write/read round-trip and key isolation)
- [ ] 1.2 Wire the data source in DI (main source set, shared by both build types per the NetworkModule convention)

## 2. Active view persistence

- [ ] 2.1 Restore flow in `ListViewModel`: read persisted id at init, hold pending until shops emit, validate membership, apply or fall back to Global (TDD: valid id, invalid id, nothing persisted, shops-not-loaded gate)
- [ ] 2.2 Write the active shop id on every drawer selection (TDD: selection writes immediately)

## 3. Widget stack UI

- [ ] 3.1 Build `ui/widgetstack/WidgetStack.kt`: top-end floating container, chevron-only collapsed state, animated expand/collapse (chevron rotation + expand/fade), upward-swipe collapse with threshold
- [ ] 3.2 Add the five-control order with `WidgetStackControl` enum: View Switcher (real, wired to `onDrawerToggled`, gray/active by drawer state), Settings/Sort Mode/Checked-Items Toggle/Search as disabled placeholders
- [ ] 3.3 Add reusable `LedDot(filled: Boolean)` composable with token styling (unit-test the filled/hollow derivation; no dot on View Switcher/Settings/placeholders)
- [ ] 3.4 Remove the temporary burger icon from the list area; verify drawer open/close, edge-swipe open, and swipe-close still work via the stack trigger
- [ ] 3.5 Persist expand/collapse state through the ViewModel (TDD: expand state survives fake-store restart simulation)

## 4. Verification

- [ ] 4.1 Run `./gradlew test` and `./gradlew lint`; fix findings
- [ ] 4.2 Build `./gradlew assembleDebug` and smoke-test on device: stack expand/collapse (tap, swipe-up, restart persistence), drawer toggle from stack, view selection restored after restart, Global fallback with a stale persisted id
