## 1. Room foundation

- [x] 1.1 Add Room (runtime + KSP compiler) to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.2 Create `ItemEntity`, `ShopEntity` with DTO↔entity mappers (orderMap/visibleIn as JSON text)
- [x] 1.3 Create `ShoplistDatabase` (version 1) with items, shops, and `pending_mutations` tables; DAOs exposing `Flow` queries
- [x] 1.4 Wire the database into Hilt via a `LocalModule` in the main source set
- [x] 1.5 Unit tests for entity mapping round-trips (DTO → entity → DTO)

## 2. Room-backed repositories

- [x] 2.1 Refactor `ItemsRepository` reads to Room-backed observable queries, keeping the public API (`items`, `cachedListIdFlow`) stable
- [x] 2.2 Refactor `ShopsRepository` reads to Room-backed queries, preserving Global-first then A→Z ordering on every write
- [x] 2.3 Route refetch and SSE merge methods through Room writes in transactions (single emission per logical change)
- [x] 2.4 Update existing repository unit tests to run against an in-memory Room database (or hand-written fake DAOs per project convention)
- [x] 2.5 Verify `./gradlew test` passes with the Room-backed repositories

## 3. Mutation queue

- [x] 3.1 Define `PendingMutation` entity + queue DAO (enqueue, peek oldest, delete, list-all)
- [x] 3.2 Implement mutation serialization: operation kind + payload JSON for item create/update/delete/check and shop create/rename/delete
- [x] 3.3 Add the offline branch to `ItemsRepository` mutations: unreachable → enqueue + optimistic Room write; reachable → existing behavior
- [x] 3.4 Add the offline branch to `ShopsRepository` mutations (create/rename/delete)
- [x] 3.5 Implement placeholder-id create-while-offline (negative local id, remap on replay)
- [x] 3.6 Coalesce repeated check mutations on the same item to the latest value
- [x] 3.7 Unit tests: enqueue order, persistence across restart (in-memory DB re-open), coalescing, optimistic visibility

## 4. Connectivity + queue drain

- [x] 4.1 Implement `ConnectivityMonitor` exposing `StateFlow<BackendConnectivity>` (network callback + health-check confirmation)
- [x] 4.2 Implement the queue drainer: pop oldest → send → apply server response / drop on 404/409/403 → stop on network error
- [x] 4.3 Gate draining on the existing foreground lifecycle scope (STARTED) so no drain happens while backgrounded
- [x] 4.4 Hook reconnect sync: on connectivity restored or foreground-with-connectivity → drain queue, then existing `refetchAll()`
- [x] 4.5 Unit tests: drain ordering, drop-on-permanent-failure, stop-on-network-error, drain-then-refetch sequencing (MockWebServer + Turbine)

## 5. UI + verification

- [x] 5.1 Expose backend reachability to the UI and add a minimal offline indicator consistent with the design language
- [x] 5.2 Verify optimistic check-off, undo, and drag-drop still behave with Room-backed reads (existing tests green)
- [x] 5.3 Run `./gradlew test` and `./gradlew lint`; fix findings
- [x] 5.4 Device smoke test: airplane-mode offline reads, queued check-off, reconnect drain + refetch
