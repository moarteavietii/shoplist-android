## 1. API models

- [ ] 1.1 Add `@Serializable` DTOs in `data/model/`: `Item`, `Shop`, request bodies (`CreateItemRequest`, `UpdateItemRequest`, `CreateShopRequest`, `RenameShopRequest`), and error payload, with backend field names verbatim
- [ ] 1.2 Add `ShopId` value class and `VisibleIn`/`OrderMap` type aliases per design
- [ ] 1.3 Unit-test DTO deserialization against captured backend JSON shapes (item, shop, error body)

## 2. API services

- [ ] 2.1 Add Retrofit interfaces in `data/api/`: items endpoints (list, create, update, delete) and shops endpoints (list, create, rename, delete), plus health check if not already exposed
- [ ] 2.2 Register the API services in `NetworkModule` (main source set)

## 3. Device identity

- [ ] 3.1 Add device-id storage (UUID, generated once, persisted in encrypted prefs) with a hand-written fake for tests
- [ ] 3.2 Add `DeviceIdInterceptor` that attaches `X-Device-Id` to POST/PATCH/DELETE only; wire into the shared OkHttp client in `NetworkModule`
- [ ] 3.3 Unit-test the interceptor with MockWebServer: mutation carries the header, GET omits it, value is stable across requests

## 4. Fractional indexing

- [ ] 4.1 Port `generateKeyBetween` from the web repo into `domain/order/` as a pure Kotlin function (TDD: write tests first)
- [ ] 4.2 Unit-test: between-keys ordering, append after highest key, initial key `a0`, byte-wise comparator, edge cases (equal neighbors, empty view)

## 5. Repositories

- [ ] 5.1 Implement `ItemsRepository` in `data/repo/`: `StateFlow` cache, `suspend` refetch, create/update/delete updating the cache from server responses
- [ ] 5.2 Implement `ShopsRepository`: `StateFlow` cache, refetch, create/rename/delete, re-sorted Global-first then A→Z after every mutation
- [ ] 5.3 Bind both repositories in `NetworkModule`
- [ ] 5.4 Unit-test repositories with hand-written fakes + MockWebServer: first fetch populates cache, refetch replaces wholesale, 404 leaves cache unchanged, mutations update/re-sort the cache

## 6. Verification

- [ ] 6.1 Run `./gradlew test` and `./gradlew assembleDebug`; fix any failures
- [ ] 6.2 Run `./gradlew lint` and address new findings
