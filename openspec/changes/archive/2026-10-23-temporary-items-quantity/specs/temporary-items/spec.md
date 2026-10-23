## Purpose

Provide the UI for marking items as temporary — a toggle in the item creation flow and the edit item modal — plus client-side hiding of rows whose expiry has passed, so users never see a dead item mid-session.

## ADDED Requirements

### Requirement: Item creation flow offers a temporary toggle
The item creation flow (from search, with per-shop selection) SHALL offer a "Temporary" toggle. When enabled, the created item SHALL be marked temporary. The toggle SHALL default to off.

#### Scenario: Toggle defaults to off
- **WHEN** the item creation flow is opened
- **THEN** the Temporary toggle is rendered unchecked

#### Scenario: Creating a temporary item
- **WHEN** the user enables the Temporary toggle and creates an item
- **THEN** the item is created with `isTemporary: true` and no expiry set

### Requirement: Edit item modal offers a temporary toggle
The edit item modal SHALL contain a "Temporary" toggle rendered as a compact icon+label chip in the modal's meta row (right-aligned, alongside the check-count pill, with the Select all / Deselect all control on the row's left side). Toggling it SHALL persist the new value via the existing item update request.

#### Scenario: Toggling temporary on an existing item
- **WHEN** the user flips the Temporary chip in the edit modal and saves
- **THEN** the item's `isTemporary` value is updated

#### Scenario: Toggling temporary off cancels a pending expiry
- **WHEN** the user saves the edit modal with the Temporary toggle turned off for an item that has a pending expiry
- **THEN** the item's expiry is cleared and the item no longer disappears automatically

### Requirement: The app hides items whose expiry has passed
The app SHALL hide a temporary item from the list once the current time is past the item's `expiresAt` value, without waiting for a server round-trip. The app SHALL prune such items on an interval defined in the code-level config module (default: every minute), including at restore time.

#### Scenario: Item hidden after expiry passes
- **WHEN** a temporary checked item's `expiresAt` time passes while the list is open
- **THEN** the row disappears from the list without a reload

#### Scenario: Expiry check runs at load
- **WHEN** the user opens the list and an item's `expiresAt` is already in the past
- **THEN** the item is not rendered

### Requirement: Expiry-driven removal syncs across clients
When the app learns (via realtime sync or refetch) that an item was deleted by expiry, it SHALL remove it from the local list using the existing deletion sync path.

#### Scenario: Remote expiry removal disappears locally
- **WHEN** another client's fetch triggers the backend deletion of an expired item
- **THEN** this client removes the item via the existing realtime deletion sync without a reload

### Requirement: Optimistic cache mirrors expiry reconciliation
When the app applies an optimistic update for an item that stops being temporary + checked (unchecked, made permanent, or temporary toggled off), it SHALL also clear the cached `expiresAt` in the same optimistic update, mirroring the server's expiry reconciliation. When an item transitions into the temporary + checked state (check-off of a temporary item, or making a checked item temporary), the app SHALL stamp the cached `expiresAt` to now + 24 h in the same optimistic update. The client-side prune SHALL therefore never act on a stale cached expiry for an item the server would keep.

#### Scenario: Unchecking clears cached expiry optimistically
- **WHEN** the user unchecks a checked temporary item and the optimistic update is applied
- **THEN** the cached `expiresAt` is cleared immediately, so the item cannot flicker or vanish locally before the server response arrives

#### Scenario: Checking a temporary item stamps a cached expiry optimistically
- **WHEN** the user checks off an unchecked temporary item and the optimistic update is applied
- **THEN** the cached `expiresAt` is stamped to now + 24 h immediately, matching the server's behavior

#### Scenario: Local prune never diverges from server contract
- **WHEN** an item's optimistic state is temporary + unchecked (or permanent) with a stale `expiresAt` still cached
- **THEN** the prune does not hide the item, because the optimistic update already cleared the cached expiry
