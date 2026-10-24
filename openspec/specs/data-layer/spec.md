# data-layer Specification

## Purpose

Define the Android app's data layer: typed API models mirroring the backend payloads, repositories backed by a persistent local database as the single source of app state, device identity on mutations, and client-side fractional ordering key generation.

## Requirements

### Requirement: Typed API models mirror the backend payloads
The app SHALL define serializable data models for `Item`, `Shop`, `OrderMap`, `VisibleIn`, and `ShopId` that match the backend's JSON field names and types exactly, so every REST response and request body deserializes without custom adapters.

#### Scenario: Item payload deserializes
- **WHEN** the app receives an item JSON object from `GET /api/v1/lists/:listId/items`
- **THEN** it deserializes into the item model with `id`, `listId`, `name`, `isChecked`, `orderMap`, `quantity`, `isTemporary`, `checkCount`, `expiresAt` (nullable), `createdAt`, and `visibleIn` populated

#### Scenario: Shop payload deserializes
- **WHEN** the app receives a shop JSON object from `GET /api/v1/shops`
- **THEN** it deserializes into the shop model with `id`, `name`, and `createdAt` populated

#### Scenario: Error payload deserializes
- **WHEN** the backend responds with a non-2xx status and a JSON body `{ "error": string }`
- **THEN** the app can surface the error message from the body

### Requirement: Items repository fetches and caches list items
The app SHALL expose an items repository that fetches all items for a list via `GET /api/v1/lists/:listId/items` and stores them in a persistent local database, which serves all subsequent reads. The repository SHALL expose the items as an observable stream backed by the local database that emits the full item list after every change, so consumers react to both local and remote (SSE-driven) mutations. When the backend is unreachable, reads SHALL be served from the local database without error.

#### Scenario: First fetch populates the local store
- **WHEN** the items repository fetches items for a list for the first time
- **THEN** the returned items are written to the local database and later readers are served from it without a new network call

#### Scenario: Refetch replaces local state wholesale
- **WHEN** the items repository performs a fresh fetch of the same list
- **THEN** the locally persisted items for that list are replaced with the server response (the server is the source of truth)

#### Scenario: Fetching a nonexistent list surfaces an error
- **WHEN** the backend responds 404 to the items fetch
- **THEN** the repository surfaces an error to the caller and the local state is left unchanged

#### Scenario: Local state changes are observable
- **WHEN** the persisted items change through any path (fetch, local mutation, queued replay, or SSE merge)
- **THEN** the observable items stream emits the updated full item list

#### Scenario: Reads succeed while offline
- **WHEN** the backend is unreachable and previously fetched items exist
- **THEN** the repository serves the persisted items without surfacing a network error

### Requirement: Items repository supports create, update, and delete
The items repository SHALL expose create (`POST /api/v1/lists/:listId/items`), partial update (`PATCH /api/v1/items/:itemId`), and delete (`DELETE /api/v1/items/:itemId`) operations. When the backend is reachable, the repository SHALL update the local database from the server's returned item state on create and update; when unreachable, the mutation SHALL be enqueued for later replay (see `mutation-queue`) and the optimistic result SHALL be applied to the local database immediately. On a non-2xx response while online, the repository SHALL surface an error to the caller and leave the local state unchanged. The repository SHALL also expose merge methods for realtime events: an idempotent created-append, a diff-checked updated-replace, and a deleted-filter. An update whose server-returned item is absent from the local store SHALL be inserted rather than dropped.

#### Scenario: Create item updates the local store
- **WHEN** an item is created through the repository while online
- **THEN** the server's returned created item is written to the local database

#### Scenario: Update item refreshes the local entry
- **WHEN** an item is updated through the repository while online
- **THEN** the server's returned updated item replaces the local entry for that id

#### Scenario: Update of an item absent from the local store inserts it
- **WHEN** an item is updated through the repository and the server-returned item's id is not stored (but its list is)
- **THEN** the server-returned item is inserted into the local database

#### Scenario: Delete item removes the local entry
- **WHEN** an item is deleted through the repository and the server responds 204
- **THEN** the item is removed from the local database

#### Scenario: Failed online mutation surfaces an error and leaves local state unchanged
- **WHEN** the backend responds with a non-2xx status to a create, update, or delete while online
- **THEN** the repository surfaces an error to the caller and the local state is left unchanged

#### Scenario: Offline mutation is enqueued and applied optimistically
- **WHEN** a create, update, or delete is attempted while the backend is unreachable
- **THEN** the mutation is appended to the persisted queue and the optimistic result is visible in the local database

#### Scenario: Created event appends idempotently
- **WHEN** a created event arrives for an item id that is already stored
- **THEN** the local state is unchanged (no duplicate entry)

#### Scenario: Created event appends a new item
- **WHEN** a created event arrives for an item id that is not stored
- **THEN** the item is appended to the local database

#### Scenario: Updated event replaces only when the item exists and differs
- **WHEN** an updated event arrives for an id that is stored and the payload differs from the stored entry
- **THEN** the stored entry is replaced with the event's item
- **WHEN** an updated event arrives for an id that is not stored, or whose payload is identical to the stored entry
- **THEN** the local state is unchanged

#### Scenario: Deleted event filters by id
- **WHEN** a deleted event arrives for a stored item id
- **THEN** the item is removed from the local database; an id that is not stored leaves the local state unchanged

### Requirement: Shops repository fetches and caches shops
The app SHALL expose a shops repository that fetches all shops via `GET /api/v1/shops`, stores them in the persistent local database, and keeps the stored shops ordered Global-first then A→Z by name after every change. The repository SHALL expose the shops as an observable stream that emits after every change, and SHALL expose idempotent created-append, replace, and deleted-filter merge methods for realtime shop events, re-sorting after every merge. Shop mutations attempted while the backend is unreachable SHALL be enqueued for later replay.

#### Scenario: Shops are fetched and ordered
- **WHEN** the shops repository fetches shops
- **THEN** the stored list is ordered with the Global shop first, followed by the remaining shops A→Z by name

#### Scenario: Shop create, rename, and delete update the local store
- **WHEN** a shop is created, renamed, or deleted through the shops repository while online
- **THEN** the local database is updated accordingly and re-sorted Global-first then A→Z

#### Scenario: Failed shop mutation surfaces an error and leaves local state unchanged
- **WHEN** the backend responds with a non-2xx status to a shop create, rename, or delete while online
- **THEN** the repository surfaces an error to the caller and the local state is left unchanged

#### Scenario: Offline shop mutation is enqueued
- **WHEN** a shop create, rename, or delete is attempted while the backend is unreachable
- **THEN** the mutation is appended to the persisted queue and the optimistic result is visible locally

#### Scenario: Shop events merge idempotently and re-sort
- **WHEN** a created event arrives for an already-stored shop id, or an updated event arrives for a shop whose payload is unchanged
- **THEN** the local state is unchanged
- **WHEN** a created, updated, or deleted event changes the stored shops
- **THEN** the local database is updated and re-sorted Global-first then A→Z

### Requirement: Mutations carry the device id header
The app SHALL attach an `X-Device-Id` header with a stable per-install value (UUID, at most 64 characters, limited to `[A-Za-z0-9_-]`) to every mutation request (POST, PATCH, DELETE). GET requests and SSE connections SHALL NOT include the header.

#### Scenario: Mutation includes the header
- **WHEN** the app sends a POST, PATCH, or DELETE request
- **THEN** the request carries an `X-Device-Id` header with the stable device id value

#### Scenario: Reads omit the header
- **WHEN** the app sends a GET request or opens an SSE connection
- **THEN** the request does not include an `X-Device-Id` header

#### Scenario: Device id is stable across requests
- **WHEN** multiple mutations are sent during one app install
- **THEN** they all carry the same `X-Device-Id` value

### Requirement: Client-side fractional indexing
The app SHALL generate fractional ordering keys locally so a moved or inserted item can be placed between any two neighbors (or appended after the last item) by sending the resulting key in `orderMap`, without server-side key generation for local reorders.

#### Scenario: Key between two neighbors sorts between them
- **GIVEN** two items with order keys `a` and `b` where `a` sorts before `b`
- **WHEN** the app generates a key between them
- **THEN** the generated key sorts strictly after `a` and strictly before `b` in byte-wise string comparison

#### Scenario: Append key sorts after the last item
- **GIVEN** a view whose highest existing order key is `a0z`
- **WHEN** the app generates an append key for that view
- **THEN** the generated key sorts strictly after `a0z`

#### Scenario: Initial key when the view is empty
- **GIVEN** a view with no items
- **WHEN** the app generates a key for the first item in that view
- **THEN** the generated key is the initial fractional key (`a0`)

#### Scenario: Byte-wise ordering matches the server
- **WHEN** keys generated by the app are compared
- **THEN** the comparison is byte-wise (not locale-aware), matching the server's ordering of `orderMap` values

### Requirement: Device-local persisted undo history store
The app SHALL expose a device-local undo history store that persists check-history entries across app restarts. Each entry SHALL contain the item id, the previous checked value (`from`), the new checked value (`to`), and the commit time (`at`). The store SHALL cap the history at the 50 most recent entries (oldest evicted) and SHALL support pruning entries older than a configured TTL. The store SHALL expose the current history as an observable stream. The store SHALL serialize all history mutations behind an internal lock, independent of callers.

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

#### Scenario: Concurrent mutations are serialized
- **WHEN** multiple history mutations are initiated concurrently
- **THEN** the store applies them serially and no entry is lost or resurrected

### Requirement: Items repository supports optimistic check mutation with rollback
The items repository SHALL expose a check mutation that optimistically flips the item's `isChecked` value in the local database immediately, sends `PATCH /api/v1/items/:itemId` with the new value (optionally carrying `isUndo: true`) when the backend is reachable, and on success replaces the stored entry with the server-returned item. When the backend is unreachable, the flipped state SHALL persist locally and the check SHALL be enqueued for replay. On failure while online, the repository SHALL roll back only the mutated field (`isChecked`) on the current stored entry — not a full item snapshot — so fields merged concurrently by realtime events are preserved, and SHALL surface the error to the caller.

#### Scenario: Optimistic flip is visible before the PATCH settles
- **WHEN** the user checks an item while online
- **THEN** the stored item's `isChecked` flips immediately, before the network response arrives

#### Scenario: Successful PATCH replaces the stored entry
- **WHEN** the check PATCH succeeds
- **THEN** the server-returned item replaces the stored entry for that id

#### Scenario: Failed PATCH rolls the checked state back field-scoped
- **WHEN** the check PATCH fails while online
- **THEN** only the stored item's `isChecked` value reverts to its pre-mutation state and the error is surfaced to the caller
- **AND** any other fields merged concurrently by realtime events are preserved

#### Scenario: Offline check persists the flip and queues the mutation
- **WHEN** the user checks an item while the backend is unreachable
- **THEN** the flipped checked state persists in the local database and the check mutation is queued for replay
