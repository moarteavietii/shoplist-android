## 1. Data layer

- [x] 1.1 Add `UndoConfig` code-level config object (max entries 50, TTL 30 min, prune interval 60 s, in-flight wait 2 s)
- [x] 1.2 Add `UndoEntry` model and `UndoHistoryStore` (DataStore-backed, JSON-serialized, observable `StateFlow`, record/evict/prune/removeTop/restoreTop) with unit tests using a temp DataStore file
- [x] 1.3 Add `setChecked(itemId, isChecked, isUndo)` to `ItemsRepository`: optimistic cache flip, PATCH with optional `isUndo: true`, server-item swap on success, cache rollback + rethrow on failure — with unit tests (MockWebServer)
- [x] 1.4 Wire `UndoHistoryStore` into Hilt (`NetworkModule`/main source set, shared binding)

## 2. Domain logic

- [x] 2.1 Add `fuzzyMatch(query, name)` pure function (case-insensitive in-order subsequence) with unit tests
- [x] 2.2 Add view-ordering helper (Global first, then alphabetical) with unit tests

## 3. ViewModel

- [x] 3.1 Create `ListUiState` (items for active view, watermark name, search query, filtered items, add-affordance visibility, undo-bar visibility, add-modal state)
- [x] 3.2 Implement `ListViewModel`: collect repositories, apply Global-view filter + manual sort + fuzzy filter, expose check/uncheck and add-item actions
- [x] 3.3 Implement undo orchestration: bounded wait for in-flight checks, prune, stale/expired/missing-item skip walk, one entry per tap, rollback + entry restore on failure — with unit tests (fakes + Turbine)
- [x] 3.4 Implement add-modal state: title, view checklist (Global first, all selected), select-all/deselect-all toggle (never deselects Global), save (create with `visibleIn`) and discard, post-save signal (clear query, scroll to bottom, refocus search) — with unit tests

## 4. UI

- [x] 4.1 Build `ListScreen` scaffold: edge-to-edge container, `imePadding()`, bottom-anchored `LazyColumn` (`reverseLayout = true`), hidden scrollbar
- [x] 4.2 Build item row composable: chromeless/borderless design-token styling, compact spacing, checkbox tap target
- [x] 4.3 Add view-name watermark layer behind the list (centered, ~7% opacity, non-interactive, updates with view state)
- [x] 4.4 Add bottom bar: search field (fuzzy filtering, IME Enter → add action, add button on empty result) and undo bar (visible while valid entries exist)
- [x] 4.5 Add checked-row fly-out animation (translate right + fade) when hide-checked drops a row
- [x] 4.6 Build add-item modal in the design language: pre-filled title, view checklist with select-all toggle, Save/Discard, validation (non-empty title)
- [x] 4.7 Wire post-save behavior: clear query, scroll to bottom (`scrollToItem(0)`), refocus search field

## 5. Integration

- [x] 5.1 Swap home content in `MainActivity` from health placeholder to `ListScreen` behind the existing auth/health gate
- [x] 5.2 Verify SSE interplay: remote `item:updated`/`item:deleted` events merge cleanly while optimistic check and undo are in flight (unit tests with simulated event + mutation interleaving)

## 6. Verification

- [x] 6.1 Run `./gradlew test` — all unit tests pass
- [x] 6.2 Run `./gradlew assembleDebug` and `./gradlew lint` — build and lint clean
- [x] 6.3 On-device smoke test via dev tunnel (`adb reverse tcp:3005 tcp:3005`): list renders, check-off + undo work, quick-add creates an item visible on the server
