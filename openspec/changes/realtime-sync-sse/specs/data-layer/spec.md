## Purpose

Define the Android app's data layer: typed API models mirroring the backend payloads, repositories with an in-memory cache as the single source of app state, device identity on mutations, and client-side fractional ordering key generation.

## MODIFIED Requirements

### Requirement: Items repository fetches and caches list items
The app SHALL expose an items repository that fetches all items for a list via `GET /api/lists/:listId/items`, stores them in an in-memory cache, and serves subsequent reads from that cache until it is replaced or mutated. No local database is used. The repository SHALL expose the cached items as an observable stream that emits the full item list after every cache change, so consumers react to both local and remote (SSE-driven) mutations.

#### Scenario: First fetch populates the cache
- **WHEN** the items repository fetches items for a list for the first time
- **THEN** the returned items are stored in memory and the same data is served to later readers without a new network call

#### Scenario: Refetch replaces the cache wholesale
- **WHEN** the items repository performs a fresh fetch of the same list
- **THEN** the in-memory cache is replaced with the server response (the server is the source of truth)

#### Scenario: Fetching a nonexistent list surfaces an error
- **WHEN** the backend responds 404 to the items fetch
- **THEN** the repository surfaces an error to the caller and the cache is left unchanged

#### Scenario: Cache changes are observable
- **WHEN** the cached items change through any path (fetch, local mutation, or SSE merge)
- **THEN** the observable items stream emits the updated full item list

### Requirement: Items repository supports create, update, and delete
The items repository SHALL expose create (`POST /api/lists/:listId/items`), partial update (`PATCH /api/items/:itemId`), and delete (`DELETE /api/items/:itemId`) operations, and SHALL update its in-memory cache from the server's returned item state on create and update. The repository SHALL also expose cache merge methods for realtime events: an idempotent created-append, a diff-checked updated-replace, and a deleted-filter.

#### Scenario: Create item updates the cache
- **WHEN** an item is created through the repository
- **THEN** the server's returned created item is added to the in-memory cache

#### Scenario: Update item refreshes the cached entry
- **WHEN** an item is updated through the repository
- **THEN** the server's returned updated item replaces the cached entry for that id

#### Scenario: Delete item removes the cached entry
- **WHEN** an item is deleted through the repository and the server responds 204
- **THEN** the item is removed from the in-memory cache

#### Scenario: Created event appends idempotently
- **WHEN** a created event arrives for an item id that is already cached
- **THEN** the cache is unchanged (no duplicate entry)

#### Scenario: Created event appends a new item
- **WHEN** a created event arrives for an item id that is not cached
- **THEN** the item is appended to the cache

#### Scenario: Updated event replaces only when the item exists and differs
- **WHEN** an updated event arrives for an id that is cached and the payload differs from the cached entry
- **THEN** the cached entry is replaced with the event's item
- **WHEN** an updated event arrives for an id that is not cached, or whose payload is identical to the cached entry
- **THEN** the cache is unchanged

#### Scenario: Deleted event filters by id
- **WHEN** a deleted event arrives for a cached item id
- **THEN** the item is removed from the cache; an id that is not cached leaves the cache unchanged

### Requirement: Shops repository fetches and caches shops
The app SHALL expose a shops repository that fetches all shops via `GET /api/shops`, stores them in an in-memory cache, and keeps the cache ordered Global-first then A→Z by name after every mutation. The repository SHALL expose the cached shops as an observable stream that emits after every cache change, and SHALL expose idempotent created-append, replace, and deleted-filter merge methods for realtime shop events, re-sorting after every merge.

#### Scenario: Shops are fetched and ordered
- **WHEN** the shops repository fetches shops
- **THEN** the cached list is ordered with the Global shop first, followed by the remaining shops A→Z by name

#### Scenario: Shop create, rename, and delete update the cache
- **WHEN** a shop is created, renamed, or deleted through the shops repository
- **THEN** the in-memory cache is updated accordingly and re-sorted Global-first then A→Z

#### Scenario: Shop events merge idempotently and re-sort
- **WHEN** a created event arrives for an already-cached shop id, or an updated event arrives for a shop whose payload is unchanged
- **THEN** the cache is unchanged
- **WHEN** a created, updated, or deleted event changes the cached shops
- **THEN** the cache is updated and re-sorted Global-first then A→Z
