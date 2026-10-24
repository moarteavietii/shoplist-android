## 1. Path flip (REST + SSE)

- [x] 1.1 Flip all Retrofit paths in `ItemsApi.kt` from `api/...` to `api/v1/...`
- [x] 1.2 Flip all Retrofit paths in `ShopsApi.kt` from `api/...` to `api/v1/...`
- [x] 1.3 Flip both SSE subscribe URLs in `RealtimeClient.kt` to `/api/v1/lists/$listId/subscribe` and `/api/v1/shops/subscribe`

## 2. Test assertions

- [x] 2.1 Update path assertions in `DeviceIdInterceptorTest` to `/api/v1/...`
- [x] 2.2 Update path assertions in `ShopsRepositoryMockWebServerTest` to `/api/v1/...`
- [x] 2.3 Update path assertions in `ItemsRepositoryMockWebServerTest` to `/api/v1/...`
- [x] 2.4 Update path assertions in `ListViewModelTest` to `/api/v1/...`

## 3. Docs

- [x] 3.1 Update `docs/MIGRATION_PLAN.md` §3 endpoint table to the versioned paths; re-check the PATCH item request schema (incl. `isUndo`) against the live OpenAPI document and correct if stale
- [x] 3.2 Add the contract source-of-truth pointer to `docs/MIGRATION_PLAN.md` §3: live OpenAPI (`GET /openapi.json`) for REST, `apps/backend/asyncapi.yaml` in the web repo for SSE, plus the versioning rule (breaking → new major prefix + deprecation window)

## 4. Verification

- [x] 4.1 Run `./gradlew test` — all unit tests green
- [x] 4.2 Run `./gradlew lint` — no new lint issues
- [x] 4.3 Device smoke test against the live backend: app loads items and shops, SSE events flow (no 404s)
