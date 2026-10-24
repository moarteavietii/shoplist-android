## Purpose

Persist the app's items and shops locally so the list survives process death and remains readable with no connectivity. The local database replaces the in-memory cache as the store the UI reads from; the server remains the source of truth whenever connectivity exists.

## ADDED Requirements

### Requirement: Items are persisted in a local database
The app SHALL store all fetched and merged items in a local database on the device, keyed by item id, preserving every item field (name, checked state, per-shop order keys, quantity, temporary flag, check count, expiry, visibility, creation time). Reads of the item list SHALL be served from the local database without a network call once data has been fetched at least once, and SHALL survive app restarts.

#### Scenario: Items survive process death
- **WHEN** items have been fetched and the app process is killed and restarted
- **THEN** the previously fetched items are available immediately from the local database without a network call

#### Scenario: Server refetch replaces local item state
- **WHEN** a full items fetch completes
- **THEN** the locally persisted items for that list are replaced with the server response (the server is the source of truth)

#### Scenario: Realtime events update the persisted store
- **WHEN** an SSE item event is merged (created, updated, or deleted)
- **THEN** the change is applied to the local database using the existing merge rules (idempotent created-append, diff-checked updated-replace, deleted-filter)

### Requirement: Shops are persisted in a local database
The app SHALL store all fetched shops in the local database, keeping the Global-first then A→Z ordering after every change, and SHALL merge SSE shop events with the same idempotent rules as the in-memory cache did previously. Shop data SHALL survive app restarts.

#### Scenario: Shops survive process death
- **WHEN** shops have been fetched and the app restarts
- **THEN** the previously fetched shops are available immediately, ordered Global-first then A→Z

#### Scenario: Shop events merge into the persisted store
- **WHEN** a shop SSE event arrives
- **THEN** the persisted shops are updated accordingly and re-sorted Global-first then A→Z

### Requirement: Reads work with no connectivity
The app SHALL serve all list and shop reads from the local database when the backend is unreachable, without surfacing network errors for read paths. The UI SHALL continue to render, sort, filter, and search the locally persisted data identically to the connected case.

#### Scenario: App opens with no connectivity
- **WHEN** the app starts with no network connectivity and previously fetched data exists
- **THEN** the list screen renders the persisted items and shops normally

#### Scenario: First launch with no connectivity
- **WHEN** the app starts with no network connectivity and no data has ever been fetched
- **THEN** the app shows its empty/error state as today and does not crash
