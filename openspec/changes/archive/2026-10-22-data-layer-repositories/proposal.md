## Why

The scaffold migration (roadmap 1) delivered the app shell, auth, and networking plumbing, but the app has no data layer yet: no DTOs, no repositories, no device identity, and no fractional-indexing port. Every later roadmap item (realtime sync, list screen, views) builds on these, so the data layer is the next migration item.

## What Changes

- Add kotlinx.serialization DTOs mirroring the web repo's `packages/shared` API payload types: `Item`, `Shop`, `OrderMap`, `VisibleIn`, `ShopId`, plus request bodies and error payloads
- Add `ItemsRepository` and `ShopsRepository` backed by Retrofit endpoints, with an in-memory cache (no Room — per decisions log)
- Add an `X-Device-Id` interceptor applied to mutation requests only (POST/PATCH/DELETE); GETs and SSE connects never send it
- Port the web app's fractional indexing (`generateKeyBetween`) to Kotlin, with unit tests
- Expose repositories via Hilt (`NetworkModule` main source set — shared by both build types)

## Capabilities

### New Capabilities
- `data-layer`: DTOs, repositories with in-memory cache, device-id header on mutations, and fractional indexing port

### Modified Capabilities

## Impact

- New code under `app/src/main/.../data/` (DTOs, API interfaces, repositories) and `app/src/test/...` (unit tests)
- `NetworkModule` gains Retrofit API service and repository bindings; a new `DeviceIdInterceptor` joins the shared OkHttp stack
- New dependency: none (kotlinx-serialization, Retrofit, OkHttp already present)
- Web specs used as source material (adapted, not copied): `rest-api` (client side), `shared-types-package`, `shops-data-model`, `frontend-data-fetching`
