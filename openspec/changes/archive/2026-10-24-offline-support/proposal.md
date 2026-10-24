## Why

The app currently requires connectivity for every action: the repository cache is in-memory only and every mutation goes straight to the network. If the device is offline (or the backend is unreachable), the app is a read-only snapshot that loses state on process death. Offline support is the last open roadmap item (item 8) and makes the app resilient: data survives restarts and user actions queue up instead of failing.

## What Changes

- Add a Room database as the persistent local store for items and shops, replacing the in-memory cache as the source the UI reads from
- Add a persisted mutation queue: create / update / delete mutations made while offline are recorded and replayed in order when connectivity returns
- Add a sync strategy on reconnect: queued mutations replay first, then a full refetch reconciles state; the server response wins on conflict (user-confirmed decision)
- Keep the existing optimistic-update + rollback behavior for online mutations; the queue is the offline path, not a replacement
- Keep the "no network while backgrounded" power requirement intact: the queue drains only while the app is foregrounded and connectivity is available
- SSE merge rules continue to reconcile the local store in real time when connected

## Capabilities

### New Capabilities
- `offline-persistence`: Room-backed local persistence of items and shops; the UI reads from the local database, which survives process death
- `mutation-queue`: persisted, ordered queue of mutations created while offline; replay in FIFO order on reconnect
- `offline-sync`: reconciliation strategy when connectivity returns — queue drain, full refetch, conflict resolution (server wins), and connectivity/lifecycle gating

### Modified Capabilities
- `data-layer`: repositories switch from in-memory cache to Room-backed storage; mutation paths route through the offline queue when the network is unavailable

## Impact

- **Code:** `data` layer (repositories, DTOs → entities mapping), new `local` package (Room database, DAOs, entities), new sync/queue components, Hilt `NetworkModule`/DI wiring
- **Dependencies:** new Room dependency (runtime + compiler) in `gradle/libs.versions.toml`
- **Behavior:** app usable offline for reads and queued writes; no API changes (backend reused as-is); no UI changes beyond potential offline indicators if specs require them
- **Risk:** mutation replay ordering and SSE-vs-replay reconciliation are the subtle parts; covered by unit tests (fakes + MockWebServer + Turbine per project convention)
