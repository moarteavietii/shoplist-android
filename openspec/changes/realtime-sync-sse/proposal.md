## Why

The app currently shows stale data: changes made by other devices (or the web client) only appear after a manual restart or refetch. The backend already broadcasts every mutation over two Server-Sent Events streams, so consuming them is the cheapest way to make the shared list feel live — and it is the freshness mechanism the migration plan settled on (SSE + refetch-on-reconnect replaces pull-to-refresh).

## What Changes

- Add an SSE client layer on OkHttp `EventSource` for the two backend streams: items (`GET /api/lists/:listId/subscribe`) and shops (`GET /api/shops/subscribe`), authenticated with the same `CF-Access-*` headers as REST (no-op in debug builds, per the existing build-type split).
- Parse the SSE wire format (`event:`/`data:` frames, `connected` first frame, `: ping` heartbeat comments) into typed events (`item:created`, `item:updated`, `item:deleted`, `shop:created`, `shop:updated`, `shop:deleted`) carrying `originDeviceId`.
- Apply cache merge rules in the repositories: idempotent append for `item:created`, diff-checked replace for `item:updated`, filter by id for `item:deleted`, and re-sorted shops (Global first, then A→Z) after every shop event.
- Reconnect → full refetch contract: on every (re)connect, refetch the full items + shops lists, since the server has no replay/`Last-Event-ID`.
- **Lifecycle-aware connections (hard requirement):** stop both SSE streams and any in-flight work when the app is backgrounded; on foreground, reconnect and resync via full refetch (`repeatOnLifecycle(STARTED)`).
- Expose a device id (persisted, generated with a guarded fallback) used for own-echo suppression on mutations and forwarded with every event for the later change-toast layer.

## Capabilities

### New Capabilities

- `realtime-sync`: SSE client for the items and shops streams — connection management with auth headers, wire-format parsing, typed events, reconnect-with-full-refetch, and lifecycle-aware start/stop.

### Modified Capabilities

- `data-layer`: repositories gain SSE-driven cache mutation methods implementing the merge rules (idempotent created-append, diff-checked updated-replace, deleted-filter, shop re-sort) and expose the cache as an observable stream so the UI can react to remote changes.

## Impact

- New networking code: SSE client (OkHttp `EventSource`), event DTOs, device-id provider.
- `ItemsRepository` / `ShopsRepository` in the data layer: new merge methods and observable cache.
- App wiring: lifecycle observer that starts/stops the streams; Hilt bindings for the new components (shared bindings in the main source set, following the per-build-type split — only auth differs).
- Dependency: OkHttp's `okhttp-sse` artifact added to `gradle/libs.versions.toml`.
- No backend changes; no UI changes in this change (list screen consumes the observable cache in roadmap item 4).
