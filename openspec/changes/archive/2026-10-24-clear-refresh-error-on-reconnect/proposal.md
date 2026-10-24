## Why

Device smoke testing on the emulator found that after the backend becomes reachable again, the red "Couldn't refresh the list" error lingers on screen: `ListViewModel.refresh()` only runs once at composition, so nothing re-runs it when connectivity returns. The user must dismiss the error manually even though the reconnect itself succeeded (banner cleared, queue drained, data reconciled).

## What Changes

- When backend connectivity transitions from UNREACHABLE to REACHABLE, the list screen automatically re-runs the refresh that reconciles items and shops, so a stale "Couldn't refresh the list" error clears itself on reconnect.
- No change to the manual dismissal path (tapping the error still dismisses it) or to the offline banner behavior.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `offline-sync` (delta in the in-flight `offline-support` change): the "Offline state is observable to the UI" requirement gains a scenario — when reachability returns, the UI re-attempts the failed refresh so transient error states do not outlive the reconnect.
