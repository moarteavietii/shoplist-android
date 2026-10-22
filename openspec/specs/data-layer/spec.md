# data-layer Specification

## Purpose

Define the Android app's data layer: typed API models mirroring the backend payloads, repositories with an in-memory cache as the single source of app state, device identity on mutations, and client-side fractional ordering key generation.

## Requirements

### Requirement: Typed API models mirror the backend payloads
The app SHALL define serializable data models for `Item`, `Shop`, `OrderMap`, `VisibleIn`, and `ShopId` that match the backend's JSON field names and types exactly, so every REST response and request body deserializes without custom adapters.

#### Scenario: Item payload deserializes
- **WHEN** the app receives an item JSON object from `GET /api/lists/:listId/items`
- **THEN** it deserializes into the item model with `id`, `listId`, `name`, `isChecked`, `orderMap`, `quantity`, `isTemporary`, `checkCount`, `expiresAt` (nullable), `createdAt`, and `visibleIn` populated

#### Scenario: Shop payload deserializes
- **WHEN** the app receives a shop JSON object from `GET /api/shops`
- **THEN** it deserializes into the shop model with `id`, `name`, and `createdAt` populated

#### Scenario: Error payload deserializes
- **WHEN** the backend responds with a non-2xx status and a JSON body `{ "error": string }`
- **THEN** the app can surface the error message from the body

### Requirement: Items repository fetches and caches list items
The app SHALL expose an items repository that fetches all items for a list via `GET /api/lists/:listId/items`, stores them in an in-memory cache, and serves subsequent reads from that cache until it is replaced or mutated. No local database is used.

#### Scenario: First fetch populates the cache
- **WHEN** the items repository fetches items for a list for the first time
- **THEN** the returned items are stored in memory and the same data is served to later readers without a new network call

#### Scenario: Refetch replaces the cache wholesale
- **WHEN** the items repository performs a fresh fetch of the same list
- **THEN** the in-memory cache is replaced with the server response (the server is the source of truth)

#### Scenario: Fetching a nonexistent list surfaces an error
- **WHEN** the backend responds 404 to the items fetch
- **THEN** the repository surfaces an error to the caller and the cache is left unchanged

### Requirement: Items repository supports create, update, and delete
The items repository SHALL expose create (`POST /api/lists/:listId/items`), partial update (`PATCH /api/items/:itemId`), and delete (`DELETE /api/items/:itemId`) operations, and SHALL update its in-memory cache from the server's returned item state on create and update.

#### Scenario: Create item updates the cache
- **WHEN** an item is created through the repository
- **THEN** the server's returned created item is added to the in-memory cache

#### Scenario: Update item refreshes the cached entry
- **WHEN** an item is updated through the repository
- **THEN** the server's returned updated item replaces the cached entry for that id

#### Scenario: Delete item removes the cached entry
- **WHEN** an item is deleted through the repository and the server responds 204
- **THEN** the item is removed from the in-memory cache

### Requirement: Shops repository fetches and caches shops
The app SHALL expose a shops repository that fetches all shops via `GET /api/shops`, stores them in an in-memory cache, and keeps the cache ordered Global-first then A→Z by name after every mutation.

#### Scenario: Shops are fetched and ordered
- **WHEN** the shops repository fetches shops
- **THEN** the cached list is ordered with the Global shop first, followed by the remaining shops A→Z by name

#### Scenario: Shop create, rename, and delete update the cache
- **WHEN** a shop is created, renamed, or deleted through the shops repository
- **THEN** the in-memory cache is updated accordingly and re-sorted Global-first then A→Z

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
