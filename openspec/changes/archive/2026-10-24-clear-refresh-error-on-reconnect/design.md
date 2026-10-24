## Context

`ListViewModel.refresh()` (ListViewModel.kt:172) guards on `connectivity.value == UNREACHABLE` and runs once from `LaunchedEffect(Unit)` in ListScreen.kt:109. When it fails, it sets `errorMessage = "Couldn't refresh the list"`; the error is dismissable by tap (`onErrorDismissed`). The ViewModel already observes the `ConnectivityMonitor` stream (`isOffline` at line 69 maps it to a boolean).

## Goals

- A failed refresh retries itself when the backend becomes reachable again, clearing the error on success.
- No sync work while backgrounded (existing power rule).

## Non-Goals

- Changing the offline banner, mutation queue, or drain sequencing.
- Retry/backoff for other error types (mutation failures keep their existing behavior).

## Existing Solutions

The ViewModel already collects the connectivity flow. The offline-support change wires reconnect sync (drain + refetch) through `RealtimeSyncManager`; this change only adds the UI-level retry of the *list refresh* on the same transition.

## Decisions

- **Retry in the ViewModel, not the UI.** Collect the connectivity flow in `ListViewModel.init` and, on each UNREACHABLE → REACHABLE transition, call `refresh()`. The existing guard at the top of `refresh()` makes it a no-op while offline, so no extra state is needed. This keeps the retry logic testable with the existing fake-connectivity test setup.
- **No debounce.** Connectivity callbacks can flap, but `refresh()` is idempotent (a refetch) and cheap relative to the drain; flap only causes an extra refetch.
- **Background safety.** The retry fires from the connectivity flow, which the app already pauses while backgrounded (background power rule), so no additional lifecycle gating is required.

## Risks / Trade-offs

- A flapping connection could refetch a few extra times; accepted (refetch is idempotent and cheap).
