## Context

The app currently keeps items and shops in `MutableStateFlow` in-memory caches inside `ItemsRepository` / `ShopsRepository`; every mutation calls Retrofit directly. SSE events merge into those caches via `RealtimeSyncManager`, and `RealtimeLifecycle` stops all realtime work when the app is not `STARTED` (background power rule). There is no Room dependency yet. The undo history is already persisted via its own file-based store, and UI preferences live in DataStore — those stay as they are.

## Goals / Non-Goals

**Goals:**
- Items + shops survive process death and are readable offline
- Offline mutations queue up (persisted, ordered) and replay on reconnect
- Reconnect sync: drain queue → full refetch, server wins
- Keep optimistic-update + rollback semantics for the online path unchanged
- Respect the background power rule: no network while backgrounded

**Non-Goals:**
- No multi-device conflict resolution beyond "server wins" (user-confirmed)
- No background sync, WorkManager, or notifications
- No Room migration tooling beyond the initial schema (first DB version)
- No changes to the backend API, undo store, or theme/preferences storage

## Decisions

**1. Room as the local store, exposed as `Flow` from DAOs.**
Room entities for `ItemEntity` and `ShopEntity` map to/from the existing DTOs; the repositories keep their public API (`items: StateFlow<List<Item>>` etc.) but back it with Room's `Flow` queries instead of `MutableStateFlow`. Alternative considered: keep StateFlow and mirror into Room — rejected because two sources of truth invite drift; Room's observable queries give the same reactive behavior for free.

**2. Single Room database, two tables + a queue table.**
`items` (with `orderMap` serialized as JSON text, `visibleIn` as JSON array text), `shops`, and `pending_mutations` (auto-increment id = enqueue order, operation type, payload JSON, created-at). One database, version 1, no schema export initially. Alternative: DataStore/JSON file — rejected, no queryability and manual index management.

**3. Mutation queue as a Room table, drained by a foreground-only worker.**
`PendingMutation(id, kind, payload)` where `kind` covers item create/update/delete/check and shop create/rename/delete. The drainer is a coroutine owned by the same lifecycle scope that runs SSE (`RealtimeLifecycle`-driven), so it inherits the existing STARTED gating — no WorkManager, keeping the background rule trivially satisfied. Replay: pop oldest → send → on success delete row and apply server response locally; on 404/409/403 delete row and continue (drop); on network error stop draining (retry on next trigger).

**4. Connectivity detection via ConnectivityManager callback + health check.**
A small `ConnectivityMonitor` exposes `StateFlow<BackendConnectivity>` (Unreachable / Reachable). Network callback gives cheap edge detection; actual reachability of the backend is confirmed by the existing health check / first successful request. The UI may subscribe to show an offline indicator. Alternative: poll `/health` on a timer — rejected, wasteful.

**5. Reconnect sync = drain queue → refetch all.**
Hooked into the same place as the existing SSE reconnect refetch: when connectivity flips to reachable (or app foregrounds with connectivity), drain the queue first, then call the existing `refetchAll()` path. This reuses the "server is truth" refetch contract; the drain-then-refetch order prevents the refetch from clobbering not-yet-replayed optimistic state.

**6. Repositories gain an offline branch, not a parallel API.**
Each mutating repository method checks the connectivity state: reachable → existing behavior (network + cache update); unreachable → append to `pending_mutations` + apply optimistic write to Room. This keeps call sites (ViewModels) unchanged.

**7. Create-while-offline uses client-generated placeholder ids.**
Offline-created items get a negative temporary id locally; the queued create carries the full request body. On replay, the server returns the real item; the local placeholder row is replaced (negative id → server id) and any queued follow-up mutations referencing the placeholder are remapped. Alternative: block creates while offline — rejected, poor UX and the spec requires queued creates.

## Risks / Trade-offs

- [Replay vs SSE race: an SSE event for the same item arrives mid-drain] → Merge rules are already idempotent/diff-checked; server responses overwrite local rows, and the post-drain refetch is the final reconciler.
- [Placeholder id leaks into orderMap keys] → orderMap keys are shop ids, not item ids, so no collision; placeholder id only lives in the local row and queue payloads, remapped at replay.
- [Queue grows unbounded during long offline periods] → Mutations per kind collapse where safe (e.g. repeated checks on the same item coalesce to the latest value); cap is not needed for realistic shopping-list usage, revisit if it becomes a problem.
- [Room Flow emissions on every write could cause UI jank on bulk refetch] → Batch writes in one transaction so the observable emits once per logical change.
- [Cleartext dev traffic + Room adds no new security surface] → Room DB is app-private storage; no new sensitive data beyond what's already cached in memory.

## Migration Plan

1. Add Room dependencies to the version catalog and `app/build.gradle.kts` (ksp compiler).
2. Add database, entities, DAOs; wire into Hilt (`NetworkModule` or a new `LocalModule` in main source set — shared by both build types, per the per-build-type binding rule).
3. Refactor repositories to Room-backed reads; keep public API stable so ViewModels don't change.
4. Add queue + drain worker + connectivity monitor; hook drain into the existing lifecycle-gated scope.
5. Unit tests: fakes + Turbine for queue ordering, coalescing, replay failure paths; MockWebServer for drain-then-refetch sequencing.
6. Rollback: the change is additive; reverting the branch restores the in-memory behavior. No data migration concerns (first DB version).

## Open Questions

None — the conflict strategy was confirmed by the user (replay in order, server response wins), and the remaining choices are implementation-level.
