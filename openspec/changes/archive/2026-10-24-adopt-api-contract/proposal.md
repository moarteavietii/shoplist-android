## Why

The backend now publishes a machine-readable API contract (OpenAPI 3.1 at `GET /openapi.json`, AsyncAPI at `apps/backend/asyncapi.yaml` in the web repo) and has flipped all routes from `/api` to `/api/v1` (**BREAKING**, already deployed). The Android app still hardcodes the unversioned `/api/...` paths in its Retrofit services and SSE client, so every REST call and both SSE subscriptions currently 404 against the live backend. Going forward, the contract documents are the source of truth for endpoint paths, request/response shapes, and SSE event payloads.

## What Changes

- Flip all Retrofit endpoint paths in `ItemsApi` and `ShopsApi` from `api/...` to `api/v1/...` (**BREAKING** against the old backend, required by the new one).
- Flip both SSE subscribe URLs in `RealtimeClient` to `/api/v1/lists/:listId/subscribe` and `/api/v1/shops/subscribe`.
- Update all unit tests that assert request paths (~25 assertions across 6 test files) to the versioned paths.
- Update `docs/MIGRATION_PLAN.md` §3 (Discovered Contracts) to the versioned paths and record the contract documents as the authoritative reference for REST + SSE work.
- Establish the convention: when touching anything endpoint- or SSE-related, consult the live OpenAPI document (`GET /openapi.json`) and the AsyncAPI document instead of re-deriving shapes; breaking backend changes arrive as a new `/api/vN` prefix with a deprecation window.

## Capabilities

### New Capabilities

- `api-contract-adoption`: The app targets the backend's versioned API surface (`/api/v1`) and treats the backend's machine-readable contract (OpenAPI for REST, AsyncAPI for SSE) as the authoritative reference for endpoint paths, payload shapes, and SSE event catalog.

### Modified Capabilities

- `realtime-sync`: The SSE subscribe endpoint paths change from `/api/lists/:listId/subscribe` and `/api/shops/subscribe` to the versioned `/api/v1/...` equivalents.
- `data-layer`: The REST endpoint paths referenced in the repository requirements change from `/api/...` to `/api/v1/...`.

## Impact

- **Code**: `app/src/main/java/eu/pascariu/shoppinglist/data/api/ItemsApi.kt`, `.../data/api/ShopsApi.kt` (Retrofit path annotations), `.../data/realtime/RealtimeClient.kt` (SSE URLs).
- **Tests**: `DeviceIdInterceptorTest`, `ShopsRepositoryMockWebServerTest`, `ItemsRepositoryMockWebServerTest`, `ListViewModelTest` (path assertions).
- **Docs**: `docs/MIGRATION_PLAN.md` (contracts section + source-of-truth pointer).
- **Backend**: none — the contract and the `/api/v1` flip are already live; this change only adopts them.
