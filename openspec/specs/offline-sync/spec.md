# offline-sync Specification

## Purpose

Define what happens when connectivity returns: queued mutations replay first, then a full refetch reconciles local state with the server. This extends the existing reconnect contract (SSE reconnect → full refetch) to cover the offline case.

## Requirements

### Requirement: Reconnect sync drains the queue then refetches
When the app detects that the backend is reachable again (network restored or app foregrounded), it SHALL first drain the persisted mutation queue in order, then perform a full refetch of items and shops to reconcile with the server. The refetch SHALL replace the local database state for the fetched lists, so any divergence between the optimistic offline state and the server's truth is resolved in the server's favor.

#### Scenario: Full sync sequence on reconnect
- **WHEN** the backend becomes reachable with queued mutations
- **THEN** the queue drains first, then a full items + shops refetch replaces the local state

#### Scenario: Refetch reconciles divergent offline edits
- **WHEN** the server state differs from the optimistic offline state after the queue drains
- **THEN** the local database is updated to match the server response

### Requirement: Connectivity changes trigger sync
The app SHALL detect when the backend becomes reachable (network regained or app foregrounded) and SHALL trigger the reconnect sync automatically without user action. The app SHALL NOT perform any sync work while backgrounded, per the background power rule.

#### Scenario: Network restored triggers sync
- **WHEN** the device regains connectivity while the app is in the foreground
- **THEN** the reconnect sync (drain + refetch) starts automatically

#### Scenario: Foregrounding triggers sync
- **WHEN** the app is foregrounded after being backgrounded and the backend is reachable
- **THEN** the reconnect sync runs (this is the existing refetch-on-foreground contract, extended with queue draining)

### Requirement: Offline state is observable to the UI
The app SHALL expose the current backend reachability state as an observable stream so the UI can reflect offline status (e.g. an indicator) without polling. The exposure SHALL distinguish "backend unreachable" from "connected".

#### Scenario: UI can observe offline state
- **WHEN** the backend becomes unreachable or reachable again
- **THEN** the observable connectivity state emits the corresponding value

### Requirement: Transient refresh errors clear on reconnect
The list screen SHALL re-attempt a previously failed refresh when the backend becomes reachable again, so transient error states (e.g. "Couldn't refresh the list") do not outlive the reconnect. Manual dismissal SHALL remain available.

#### Scenario: Stale refresh error clears after reconnect
- **WHEN** a refresh failed while the backend was unreachable and the backend becomes reachable again
- **THEN** the refresh is re-attempted automatically and the error state clears on success

#### Scenario: Manual dismissal still works
- **WHEN** the error state is displayed and the user taps it
- **THEN** the error state is dismissed immediately
