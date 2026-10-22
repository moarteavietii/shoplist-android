## 1. Dependencies & event models

- [x] 1.1 Add `okhttp-sse` to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.2 Define typed event DTOs (`ItemEvent`, `ShopEvent` with type discriminator and nullable `originDeviceId`) in `data/model` with kotlinx-serialization

## 2. SSE client

- [x] 2.1 Write failing unit tests for SSE wire-format handling and typed event emission (MockWebServer with `text/event-stream` responses; Turbine for flows)
- [x] 2.2 Implement `RealtimeClient`: opens both `EventSource` connections over the shared Hilt `OkHttpClient`, parses frames into typed event flows, ignores heartbeats, consumes `connected` as the connect signal
- [x] 2.3 Implement automatic reconnection with capped exponential backoff + jitter, reset on successful connect, suppressed while backgrounded; run tests green

## 3. Repository merge rules

- [x] 3.1 Write failing unit tests for `ItemsRepository` merge methods: idempotent created-append, diff-checked updated-replace (absent id or identical payload → no change), deleted-filter
- [x] 3.2 Implement `applyCreated` / `applyUpdated` / `applyDeleted` on `ItemsRepository`; run tests green
- [x] 3.3 Write failing unit tests for `ShopsRepository` merge methods: idempotent append/replace, deleted-filter, re-sort Global-first then A→Z after every merge
- [x] 3.4 Implement the merge trio on `ShopsRepository`; run tests green

## 4. Reconnect → full refetch wiring

- [x] 4.1 On every stream connect signal, trigger wholesale `refetch()` of items and shops (no `X-Device-Id` on GETs — already the case)
- [x] 4.2 Unit-test the connect→refetch path with hand-written fakes (FakeRealtimeClient / FakeItemsApi)

## 5. Lifecycle-aware connections

- [x] 5.1 Add a lifecycle-aware component using `ProcessLifecycleOwner` + `repeatOnLifecycle(STARTED)`: STARTED → connect both streams, STOP → close streams and cancel scheduled reconnects
- [x] 5.2 Wire Hilt bindings in the main source set (shared bindings only; no new per-build-type bindings needed) and start the component from `ShoplistApplication`

## 6. Verification

- [x] 6.1 Run `./gradlew test` and `./gradlew lint` and fix any issues
- [ ] 6.2 Manual device check: with the dev backend up (`adb reverse tcp:3005 tcp:3005`), create/rename/delete an item from the web client and confirm the app's cache updates; background the app and confirm streams close (no `: ping` traffic), foreground and confirm reconnect + refetch
