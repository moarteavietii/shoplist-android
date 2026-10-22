## ADDED Requirements

### Requirement: Device-local persisted undo history store
The app SHALL expose a device-local undo history store that persists check-history entries across app restarts. Each entry SHALL contain the item id, the previous checked value (`from`), the new checked value (`to`), and the commit time (`at`). The store SHALL cap the history at the 50 most recent entries (oldest evicted) and SHALL support pruning entries older than a configured TTL. The store SHALL expose the current history as an observable stream.

#### Scenario: Entry is persisted and restored
- **WHEN** a check action is recorded and the app restarts
- **THEN** the entry is restored with its original `from`, `to`, and `at` values

#### Scenario: History is capped at 50 entries
- **WHEN** the store already holds 50 entries and a new entry is recorded
- **THEN** the oldest entry is evicted and the new entry is on top

#### Scenario: Expired entries are pruned
- **WHEN** pruning runs and entries older than the configured TTL exist
- **THEN** those entries are removed from the store and from persisted storage

#### Scenario: History is observable
- **WHEN** the history changes through record, evict, prune, or removal
- **THEN** the observable history stream emits the updated entry list

### Requirement: Items repository supports optimistic check mutation with rollback
The items repository SHALL expose a check mutation that optimistically flips the item's `isChecked` value in the in-memory cache immediately, sends `PATCH /api/items/:itemId` with the new value (optionally carrying `isUndo: true`), and on success replaces the cached entry with the server-returned item. On failure, the repository SHALL roll the cached entry back to its pre-mutation state and surface the error to the caller.

#### Scenario: Optimistic flip is visible before the PATCH settles
- **WHEN** the user checks an item
- **THEN** the cached item's `isChecked` flips immediately, before the network response arrives

#### Scenario: Successful PATCH replaces the cached entry
- **WHEN** the check PATCH succeeds
- **THEN** the server-returned item replaces the cached entry for that id

#### Scenario: Failed PATCH rolls the cache back
- **WHEN** the check PATCH fails
- **THEN** the cached item returns to its pre-mutation `isChecked` value and the error is surfaced to the caller
