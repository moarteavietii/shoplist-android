## MODIFIED Requirements

### Requirement: The app subscribes to the items SSE stream
The app SHALL open a Server-Sent Events connection to `GET /api/v1/lists/:listId/subscribe` for the active list, sending the same authentication headers as REST requests. The connection SHALL be opened when the app enters the foreground (visible state) and closed when it leaves it. Until list switching exists, the app subscribes to a single hardcoded list id fixed at DI wiring time (`DEFAULT_LIST_ID` in `RealtimeClient`).

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
The app SHALL open a second, independent Server-Sent Events connection to `GET /api/v1/shops/subscribe`, with the same authentication headers and lifecycle behavior as the items stream.

#### Scenario: Both streams run concurrently
- **WHEN** the app is visible
- **THEN** the items and shops streams are both connected and events from one do not affect the other

#### Scenario: Shops stream closes on background
- **WHEN** the app leaves the visible state
- **THEN** the shops SSE connection is closed
