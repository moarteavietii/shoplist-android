## Purpose

Queue user-initiated mutations while the backend is unreachable and replay them in order when connectivity returns, so offline actions are not lost. The queue is persisted and survives process death.

## ADDED Requirements

### Requirement: Mutations made offline are queued
The app SHALL enqueue every user-initiated item or shop mutation (create, update, delete, check, reorder, visibility change) when it cannot reach the backend, instead of failing. The optimistic local change SHALL be applied to the local database immediately so the UI reflects the action. Queued entries SHALL be persisted and SHALL survive app restarts, preserving enqueue order.

#### Scenario: Check-off while offline is applied locally and queued
- **WHEN** the user checks an item while the backend is unreachable
- **THEN** the item's checked state flips in the local database immediately and a check mutation is appended to the persisted queue

#### Scenario: Create while offline is queued
- **WHEN** the user creates an item while offline
- **THEN** the item appears in the local list immediately and the create mutation is queued

#### Scenario: Queue survives process death
- **WHEN** mutations are queued and the app process is killed and restarted
- **THEN** the queued mutations are restored in their original order

### Requirement: Queued mutations replay in order on reconnect
When connectivity to the backend is restored, the app SHALL replay queued mutations in enqueue order, waiting for each to settle before sending the next. A replayed mutation SHALL apply its recorded operation against the current server state; when the server's response conflicts with the recorded intent, the server's response wins and the local state is reconciled to it. A replayed mutation that fails with a non-retryable error (e.g. 404 item deleted, 409 duplicate) SHALL be dropped without blocking the rest of the queue.

Queued create mutations (items and shops) SHALL carry a client-generated idempotency key (`clientId`, a UUID generated once at enqueue time and persisted in the mutation payload). Replays SHALL reuse the recorded key, and the backend SHALL dedupe on it: the first request creates the row, a replay with the same key returns the already-created row. This makes create replay exactly-once from the client's perspective, so a create the server already processed is never duplicated by a replay.

#### Scenario: Queue drains in order after reconnect
- **WHEN** connectivity is restored with multiple mutations queued
- **THEN** the mutations are sent one at a time in enqueue order until the queue is empty

#### Scenario: Server response wins on conflict
- **WHEN** a replayed mutation conflicts with the server's current state (e.g. the item was renamed or deleted by another device)
- **THEN** the server's response state is kept locally and the replay continues

#### Scenario: Permanently failed mutation is dropped
- **WHEN** a replayed mutation receives a non-retryable error (404, 409, 403)
- **THEN** that mutation is removed from the queue and the remaining mutations continue

#### Scenario: Replay pauses while offline
- **WHEN** connectivity drops while the queue is draining
- **THEN** draining stops, the remaining mutations stay queued, and the local database keeps the optimistic state

### Requirement: Queued mutations respect the background power rule
The app SHALL drain the mutation queue only while the app is in the foreground and connectivity is available. No queue-draining network activity SHALL occur while the app is backgrounded.

#### Scenario: No draining while backgrounded
- **WHEN** the app is backgrounded with a non-empty queue
- **THEN** no network requests are made to drain the queue

#### Scenario: Draining resumes on foreground
- **WHEN** the app returns to the foreground with queued mutations and connectivity
- **THEN** the queue drains before or alongside the reconnect refetch
