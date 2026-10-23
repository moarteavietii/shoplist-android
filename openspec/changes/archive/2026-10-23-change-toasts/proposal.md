## Why

Step 6 (feature parity) requires change toasts: users of a shared list should see what others did without watching the list. The realtime layer already carries everything needed — SSE events expose `originDeviceId`, the device id is persisted, and the sync manager diffs updates — but no toast surface exists, and the web app's `frontend-change-toasts` contract is not yet ported.

## What Changes

- Add a change-toast layer that derives human-readable toasts from realtime item events (checked / unchecked / created / deleted / renamed / quantity / visibility), suppressed for own-device echoes and no-op updates.
- Render a single self-replacing toast at the top of the list screen: slide-in entrance, 3.5 s auto-dismiss, horizontal swipe dismissal, in-place content replacement without replaying the entrance.
- Persist a change-toasts-enabled preference (default: enabled) in DataStore. The settings-drawer toggle UI is deferred to roadmap 7; the pref is honored by the toast layer now.
- Escape-key dismissal is dropped (mobile-first; no hardware keyboard path in scope). Swipe and auto-dismiss cover dismissal.

## Capabilities

### New Capabilities
- `change-toasts`: Glanceable toasts for remote item changes — event-to-label derivation, own-device suppression, single self-replacing presentation with auto-dismiss and swipe dismissal, and the persisted enabled preference.

### Modified Capabilities

## Impact

- New: a toast derivation component (event diff → labels) and a composable toast surface over `ListScreen`.
- `app/src/main/java/eu/pascariu/shoppinglist/data/realtime/RealtimeSyncManager.kt` — hook toast derivation into item-event merging.
- `app/src/main/java/eu/pascariu/shoppinglist/data/prefs/ListPreferencesDataSource.kt` — new persisted `changeToastsEnabled` flow (default true).
- `app/src/main/java/eu/pascariu/shoppinglist/ui/list/` — `ListScreen.kt` (toast placement), `ListViewModel.kt` (toast state).
- Tests: new derivation tests; `RealtimeSyncManagerTest` gains toast-hook coverage.
- No backend changes; the REST + SSE contract is reused as-is.
