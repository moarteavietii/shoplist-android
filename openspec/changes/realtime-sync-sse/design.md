## Context

The data layer (roadmap item 2) already provides `ItemsRepository` / `ShopsRepository` with in-memory caches exposed as `StateFlow`, a persisted device id (`DeviceIdStore` + `DeviceIdInterceptor` on mutations only), and per-build-type auth (real `AuthInterceptor` in release, no-op in debug). The backend broadcasts mutations on two SSE streams with no replay. See proposal.md — Why.

Android concept worth noting: unlike the web's `EventSource`, OkHttp's SSE support (`okhttp-sse`) gives us raw frames over the same OkHttp client we already use for REST — one connection pool, one interceptor chain — but it does not auto-reconnect, so reconnection and lifecycle handling are ours to build.

## Goals / Non-Goals

**Goals:**

- Live cache updates: remote mutations appear in the repositories' observable cache without a full refetch.
- Reconnect → full refetch contract, covering fresh connect, network drop, and foregrounding.
- Hard power requirement: no SSE connections (or reconnect attempts) while the app is backgrounded.
- Own-echo groundwork: typed events carry `originDeviceId` for the later change-toast layer.

**Non-Goals:**

- No UI work (list screen consumes the cache in roadmap item 4).
- No change toasts yet (roadmap item 6) — only the event plumbing.
- No backend changes; no Room; no mutation queue (offline support is roadmap item 8).

## Decisions

### OkHttp `EventSource` over a raw call or a third-party library
`okhttp-sse` reuses the existing `OkHttpClient` (auth headers, base URL, logging) and parses the wire format into `EventSourceListener` callbacks. Alternatives: raw streaming `GET` with manual line parsing (duplicates what `okhttp-sse` already does), or a dedicated SSE library (new dependency for no gain). The `okhttp-sse` artifact is added to the version catalog.

### One `SseClient` per stream, sharing the OkHttp client
A small `RealtimeClient` owns both `EventSource` connections (items + shops) and exposes typed event flows. It reuses the Hilt-provided `OkHttpClient` so the `CF-Access-*` headers arrive via the existing interceptor chain — the debug/release split works unchanged. Alternative considered: a shared multiplexed connection — rejected, the backend exposes two independent endpoints and KISS applies.

### Parsing into typed events with kotlinx-serialization
Event DTOs (`ItemEvent`, `ShopEvent` with a `type` discriminator and nullable `originDeviceId`) deserialize the `data:` JSON frame. Heartbeat comment lines never reach the listener (OkHttp SSE handles comments), and the initial `connected` frame is consumed as the connect signal that triggers the full refetch.

### Reconnect = full refetch, driven by the connect signal
On every `onOpen` (first connect or reconnect), the app refetches items and shops wholesale — the existing `refetch()` methods already replace the cache. Backoff: exponential with jitter, capped (e.g. 1 s → 30 s), reset on successful connect. Alternatives: event replay via `Last-Event-ID` (server doesn't support it), polling (wasteful, and the decisions log settled on SSE).

### Lifecycle via `ProcessLifecycleOwner` + `repeatOnLifecycle(STARTED)`
A small lifecycle-aware component collects a `started` flow: on STARTED it connects both streams; on STOP it cancels them and any scheduled reconnect. This is the app-level equivalent of the web's mount/unmount, and it directly implements the "no network while backgrounded" hard requirement. Alternative: per-screen lifecycle — rejected, streams are app-scoped state, not screen-scoped.

### Merge rules live in the repositories, not the SSE layer
`ItemsRepository` gains `applyCreated(item)` (append only if id absent), `applyUpdated(item)` (replace only if id present and payload differs), `applyDeleted(itemId)` (filter); `ShopsRepository` gains the same trio with re-sort after every merge. The SSE layer stays a dumb event pipe; the repositories own cache invariants. This keeps merge logic unit-testable without any network.

### Device id: reuse the existing `DeviceIdStore`
The spec's device-id requirements are already satisfied by `DeviceIdStore` (persisted, guarded generation) and `DeviceIdInterceptor` (mutations only). No new code — the SSE events' `originDeviceId` simply compares against `DeviceIdStore.getOrCreate()` at the toast layer later.

## Risks / Trade-offs

- [OkHttp SSE has no auto-reconnect] → We own reconnection; the backoff loop is small, unit-tested, and reset on success. Worst case, a missed reconnect leaves stale data until the next foreground — acceptable at shopping-list scale.
- [Both streams refetch on every connect → duplicate refetches when both connect at once] → Refetch is idempotent and cheap (full-list GETs, no pagination); we deliberately do not deduplicate (KISS). Revisit only if it shows in battery/network logs.
- [`EventSource` callbacks arrive on OkHttp threads] → All cache writes go through `MutableStateFlow.update` (thread-safe) and events are re-emitted on a coroutine flow, so UI consumers stay on the main dispatcher.
- [Backgrounded device kills SSE mid-write] → Closing the streams on STOP cancels the call; OkHttp handles half-closed connections. Reconnect-on-foreground plus full refetch guarantees consistency.
- [Auth token expires during a long-lived stream] → A 401/403 on the SSE connect surfaces through the existing `AuthEventBus` → token re-entry screen, same as REST.

## Migration Plan

Single `:app` module, no data migration. Rollback: revert the commit — the SSE layer is additive; repositories keep working without it (the merge methods are simply never called).

## Open Questions

None.
