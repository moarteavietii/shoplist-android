## Purpose

Consume the backend's two Server-Sent Events streams (items and shops) so the app's cached state updates live: connection management with Cloudflare Access headers, wire-format parsing into typed events, reconnect-with-full-refetch, and lifecycle-aware start/stop to save power.

## ADDED Requirements

### Requirement: The app subscribes to the items SSE stream
The app SHALL open a Server-Sent Events connection to `GET /api/lists/:listId/subscribe` for the active list, sending the same authentication headers as REST requests. The connection SHALL be opened when the app enters the foreground (visible state) and closed when it leaves it. Until list switching exists, the app subscribes to a single hardcoded list id fixed at DI wiring time (`DEFAULT_LIST_ID` in `RealtimeClient`).

#### Scenario: Stream opens while app is visible
- **WHEN** the app becomes visible and the items stream is not connected
- **THEN** an SSE connection to the list subscribe endpoint is opened with the authentication headers attached

#### Scenario: Stream closes when app is backgrounded
- **WHEN** the app leaves the visible state
- **THEN** the items SSE connection is closed and no further item events are processed

#### Scenario: Lifecycle stop closes streams and cancels reconnects
- **WHEN** the lifecycle component receives STOP (app backgrounded)
- **THEN** both SSE streams are closed and any scheduled reconnect attempts are cancelled, so no SSE traffic runs while backgrounded

### Requirement: The app subscribes to the shops SSE stream
The app SHALL open a second, independent Server-Sent Events connection to `GET /api/shops/subscribe`, with the same authentication headers and lifecycle behavior as the items stream.

#### Scenario: Both streams run concurrently
- **WHEN** the app is visible
- **THEN** the items and shops streams are both connected and events from one do not affect the other

#### Scenario: Shops stream closes on background
- **WHEN** the app leaves the visible state
- **THEN** the shops SSE connection is closed

### Requirement: SSE wire format is parsed into typed events
The app SHALL parse the SSE wire format: `event: <type>` and `data: <JSON>` frame pairs, an initial `connected` event, and heartbeat comment lines (`: ping`) which SHALL be ignored. Each item event SHALL deserialize into a typed event carrying the item payload (or item id for deletions), the event type, and `originDeviceId` (nullable). Each shop event SHALL carry the shop payload (or shop id for deletions), the event type, and `originDeviceId` (nullable).

#### Scenario: Item event parses
- **WHEN** the stream delivers `event: item:updated` with a JSON data frame containing an item and `originDeviceId`
- **THEN** the app produces a typed item-updated event with the deserialized item and origin device id

#### Scenario: Heartbeat comments are ignored
- **WHEN** the stream delivers a `: ping` comment line
- **THEN** no event is emitted and the connection stays open

#### Scenario: Deleted events carry only the id
- **WHEN** the stream delivers `item:deleted` or `shop:deleted`
- **THEN** the typed event carries the deleted id and `originDeviceId`, with no item or shop payload

### Requirement: Reconnect triggers a full refetch
The server does not replay events missed during a dropped connection. The app SHALL refetch the full items list and shops list after every (re)connection to the streams, so missed mutations are recovered. Refetches triggered by reconnect SHALL NOT be sent with the device-id header (GETs never carry it). The refetch SHALL be driven only by fresh connection events: a foregrounding that does not produce a new connection SHALL NOT trigger a refetch.

#### Scenario: Fresh connect refetches everything
- **WHEN** the items or shops stream transitions to connected
- **THEN** the app refetches the full items list and the full shops list

#### Scenario: Reconnect after network drop resyncs
- **WHEN** the connection drops and the client reconnects
- **THEN** the full refetch runs again, replacing the cache with server state

#### Scenario: Foregrounding without a new connection does not refetch
- **WHEN** the app becomes visible but the streams are already connected (no fresh connect event)
- **THEN** no refetch is triggered by the foregrounding itself

### Requirement: Stream failures reconnect automatically
When an SSE connection fails or is closed by the server, the app SHALL attempt to reconnect with backoff rather than giving up, while the app remains in the visible state. Reconnection SHALL NOT run while the app is backgrounded.

#### Scenario: Server closes the connection
- **WHEN** the server closes the stream while the app is visible
- **THEN** the app schedules a reconnect attempt and resumes the stream

#### Scenario: No reconnect while backgrounded
- **WHEN** the connection fails while the app is backgrounded
- **THEN** no reconnect is attempted until the app becomes visible again

### Requirement: The device id is persisted and used for own-echo suppression
The app SHALL maintain a persistent device id: generated once, stored locally, and reused across sessions. Generation SHALL use a guarded fallback when the platform UUID generator is unavailable, and SHALL NOT throw. All mutating requests SHALL carry the device id via the `X-Device-Id` header; GET requests and SSE connects SHALL NOT. The device id SHALL be exposed to the app so the later change-toast layer can suppress toasts for this device's own changes. The encrypted storage backing the device id SHALL be warmed up eagerly at app start, off the main thread; Keystore and EncryptedSharedPreferences work SHALL NOT run on the main thread.

#### Scenario: Device id persists across restarts
- **WHEN** the app generates a device id and is restarted
- **THEN** the same device id is reused

#### Scenario: Device id storage is warmed up at app start
- **WHEN** the app process starts
- **THEN** the encrypted device-id storage is initialized eagerly on a background thread, so the first mutation never performs Keystore or EncryptedSharedPreferences work on the main thread or on a lazy OkHttp worker thread

#### Scenario: Mutations carry the device id
- **WHEN** the app sends a POST, PATCH, or DELETE request
- **THEN** the request includes the `X-Device-Id` header with the persisted device id

#### Scenario: GETs and SSE connects omit the device id
- **WHEN** the app sends a GET request or opens an SSE connection
- **THEN** no `X-Device-Id` header is sent
