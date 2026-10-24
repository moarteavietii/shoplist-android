# check-undo Specification

## Purpose

Define check-off commit semantics and the device-local undo history: instant optimistic commit with rollback on failure, a bounded persisted history of own-device check actions, and validated undo that carries the `isUndo` flag so the backend can decrement the check counter.

## Requirements

### Requirement: Check and uncheck actions commit instantly
When the user checks or unchecks an item by tapping its checkbox, the app SHALL optimistically flip the row immediately and send `PATCH /api/v1/items/:itemId` with the new `isChecked` value without any artificial delay. If the PATCH fails, the app SHALL revert the optimistic flip and SHALL NOT record a history entry. The checkbox tap SHALL be the only row-level trigger for checking or unchecking; taps on the row body outside the checkbox SHALL NOT commit a check action.

#### Scenario: Check commits immediately
- **WHEN** the user taps the checkbox for an unchecked item
- **THEN** the item immediately appears checked and a `PATCH` with `{ "isChecked": true }` is sent right away
- **AND** no countdown timer or pending state exists

#### Scenario: Failed PATCH reverts the optimistic flip
- **WHEN** the `PATCH` request for a check or uncheck fails
- **THEN** the item's checkbox returns to its previous visual state
- **AND** no history entry is recorded for that action

#### Scenario: Multiple items can be checked concurrently
- **WHEN** the user checks item A and then checks item B
- **THEN** item A remains checked
- **AND** item B also appears checked

#### Scenario: Row body tap does not commit a check
- **WHEN** the user taps the row body outside the checkbox
- **THEN** no check or uncheck is committed and no `PATCH` is sent

### Requirement: Committed check actions are recorded in a device-local history
The app SHALL record every successfully committed check or uncheck performed by this device as an entry containing the item id, the previous checked value (`from`), the new checked value (`to`), and the commit time. Changes arriving from other users via realtime sync SHALL NOT be recorded. The history SHALL be bounded to the 50 most recent entries, with the oldest evicted when the bound is exceeded.

#### Scenario: Successful check is recorded
- **WHEN** the user checks an item and the `PATCH` succeeds
- **THEN** an entry `{ itemId, from: false, to: true, at }` is added to the top of the history

#### Scenario: History depth is capped at 50
- **WHEN** the history already holds 50 entries and the user commits another check
- **THEN** the oldest entry is evicted and the new entry is on top

#### Scenario: Realtime changes are not recorded
- **WHEN** another user's check of an item arrives via realtime sync
- **THEN** no history entry is created for that change

### Requirement: History entries expire after a TTL
The app SHALL stamp each history entry with the time it was committed and SHALL treat entries older than a time-to-live as no longer undoable. The TTL SHALL be defined in a code-level config module (no settings page exists yet) with a default of 30 minutes. The app SHALL prune expired entries on an interval defined in the same config (default: every minute), including at restore time, so the undo affordance disappears once every entry has aged out. Undo SHALL skip expired entries without sending a `PATCH`.

#### Scenario: Entry expires after the TTL
- **WHEN** a history entry has existed for longer than the configured TTL
- **THEN** it is no longer undoable and is pruned from the history and from persisted storage

#### Scenario: Undo skips an expired entry
- **WHEN** the user taps undo and the top entry has expired
- **THEN** the entry is removed without a `PATCH` and undo continues to the next entry

#### Scenario: Undo affordance hides when all entries have expired
- **WHEN** every history entry has aged past the TTL
- **THEN** the undo affordance is no longer rendered

### Requirement: Undo reverts the most recent check-off
The undo action SHALL revert the most recent valid history entry: it flips the item back to its previous checked state via a PATCH that carries `isUndo: true` identifying the request as an undo. The backend SHALL use this flag to decrement the item's check counter (floor 0) — a manual uncheck never carries the flag and never decrements.

#### Scenario: Undo PATCH carries the undo flag
- **WHEN** the undo path reverts a check-off
- **THEN** the revert PATCH includes `isUndo: true` so the backend can distinguish it from a manual uncheck

#### Scenario: Manual uncheck does not carry the undo flag
- **WHEN** the user unchecks an item directly (not via undo)
- **THEN** the uncheck PATCH does not include the undo flag

### Requirement: Undo skips entries whose recorded value no longer matches
Before applying an undo, the app SHALL compare the entry's recorded `to` value with the item's current checked state. If they do not match (e.g. the item was changed by another user via realtime sync), the app SHALL skip that entry without sending a `PATCH` and continue with the next entry.

#### Scenario: Stale entry is skipped
- **WHEN** the top history entry's `to` value differs from the item's current checked state
- **THEN** no `PATCH` is sent for that entry
- **AND** the entry is removed from the history and the next entry becomes the undo target

#### Scenario: Undo of an item deleted on the server is dropped silently
- **WHEN** the top history entry references an item that no longer exists in the repository cache
- **THEN** the entry is removed without a `PATCH` and undo continues to the next entry

### Requirement: Check history survives an app restart
The app SHALL persist the check history to device-local storage and restore it on launch, so undo remains available after an app restart.

#### Scenario: History is restored after restart
- **WHEN** the user commits check actions, restarts the app, and opens the widget area
- **THEN** the undo affordance is visible and tapping it reverts the most recent committed action from before the restart

### Requirement: Undo works across views
The check history SHALL be global across views. Undoing an action SHALL revert the item's checked state regardless of which view's list is currently displayed.

#### Scenario: Undo reverts an item from another view
- **WHEN** the user checks an item in shop A, switches to shop B, and taps undo
- **THEN** the item from shop A is reverted to its previous checked state
- **AND** shop B's list is unaffected except through the shared item state

### Requirement: One undo per tap
The undo action SHALL revert exactly one history entry per activation; it SHALL NOT cascade through multiple entries in a single tap.

#### Scenario: Single entry reverted per tap
- **WHEN** the user taps undo once
- **THEN** exactly one valid entry is reverted (or skipped) and the undo action completes

### Requirement: Undo waits for in-flight check mutations
The undo action SHALL wait (bounded to 2 seconds) for any in-flight check PATCHes to settle before validating and applying the revert, so the recorded state comparison is not made against a mid-flight value. The wait SHALL proceed as soon as in-flight checks settle rather than sleeping the full bounded window.

#### Scenario: Undo after a rapid check-off
- **WHEN** the user checks an item and taps undo while that check PATCH is still in flight
- **THEN** undo waits (up to the bounded window) for the PATCH to settle before validating the entry

### Requirement: Undo failure rolls back and restores the entry
If the undo PATCH fails, the app SHALL roll back the item's visual state and restore the history entry so the user can retry.

#### Scenario: Failed undo PATCH restores the entry
- **WHEN** the undo PATCH fails
- **THEN** the item's checkbox returns to its pre-undo visual state
- **AND** the entry is restored to the top of the history

### Requirement: Checked item animates out on check-off
When an item is checked while hide-checked mode is enabled, the app SHALL animate the row out of the list (translating toward the right and fading) instead of removing it abruptly.

#### Scenario: Checked item flies out to the right
- **WHEN** the user checks an item while "Hide checked items" is enabled
- **THEN** the row animates toward the right while fading before disappearing from the list

### Requirement: Undo history store serializes mutations
The undo history store SHALL serialize all history mutations (record, evict, prune, removal) behind an internal lock, so rapid concurrent actions (rapid checks, undo during in-flight checks) cannot interleave read-modify-write cycles and lose or resurrect entries. The guarantee SHALL hold independent of callers.

#### Scenario: Rapid concurrent mutations do not lose entries
- **WHEN** multiple history mutations are initiated concurrently (e.g. rapid check-offs, undo during in-flight checks)
- **THEN** every committed mutation is applied and no entry is lost or resurrected
