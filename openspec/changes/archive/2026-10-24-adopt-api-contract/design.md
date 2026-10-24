## Context

The backend's `api-contract-openapi-asyncapi` change is implemented and deployed: OpenAPI 3.1 is served live at `GET /openapi.json` (verified: `info.version: 1.0.0`, all 11 paths), the AsyncAPI document lives at `apps/backend/asyncapi.yaml` in the web repo, and all routes now answer under `/api/v1` (verified live; `/health` unversioned). The Android app hardcodes the unversioned paths in three places: `ItemsApi`/`ShopsApi` Retrofit annotations and the two SSE URLs in `RealtimeClient` — so the app is currently broken against the deployed backend. `docs/MIGRATION_PLAN.md` §3 still records the unversioned paths as the discovered contracts. See proposal.md — Why for motivation.

## Goals / Non-Goals

**Goals:**
- Restore the app against the live backend by adopting `/api/v1` everywhere (REST + SSE).
- Make the contract documents the recorded source of truth for future endpoint/SSE work.
- Keep the change mechanical: no behavior change beyond the path prefix.

**Non-Goals:**
- Generating a Kotlin client from the OpenAPI document (deferred alternative; ~10 endpoints don't justify codegen — revisit if drift recurs).
- Tightening the app's lenient `ItemEvent`/`ShopEvent` DTOs into sealed classes (they already mirror the contract; not required by it).
- Any backend change — the contract and version flip are already deployed.

## Decisions

### D1: Flip the path strings in place; no version abstraction
The Retrofit annotations and the two `RealtimeClient` URL constants change from `api/...` to `api/v1/...` directly. A `API_VERSION` constant or path-builder abstraction would add indirection for a value that changes at most once per backend major era, and the next flip (`/api/v2`) will be an explicit, reviewed change anyway (per the versioning rule).
*Alternative considered*: centralizing the prefix in one constant — rejected as premature abstraction (KISS).

### D2: Record the contract as source of truth in `docs/MIGRATION_PLAN.md`, not a new doc
`MIGRATION_PLAN.md` §3 is already the designated contracts reference that AGENTS.md funnels all endpoint/SSE work through. Updating its endpoint table to the versioned paths and adding a pointer to the live OpenAPI/AsyncAPI documents keeps one authoritative place. A separate contract doc would fork the reference.
*Alternative considered*: new `docs/API_CONTRACT.md` — rejected; duplicates MIGRATION_PLAN's role.

### D3: Update test path assertions in the same change
The MockWebServer/interceptor tests assert recorded request paths (`/api/shops`, `/api/items/10`, ...). They flip together with the code so the suite verifies the versioned paths, not the old ones. No new tests are needed: the behavior change is fully expressed by existing assertions.
*Alternative considered*: parameterizing tests over the version — rejected; over-engineering for a string flip.

### D4: Trailing slashes in the contract are not adopted
The live OpenAPI shows `PATCH|DELETE /api/v1/items/{itemId}/` with a trailing slash (Elysia route declared at the group root). Elysia normalizes trailing slashes (verified: both variants hit the route), and Retrofit paths without the slash are cleaner. The app keeps slash-less paths.
*Alternative considered*: matching the contract byte-for-byte — rejected; the normalization is verified behavior, and the contract tests on the backend enforce semantics, not slash bytes.

## Risks / Trade-offs

- [Hand-maintained paths can drift again] → The convention recorded in MIGRATION_PLAN (check `GET /openapi.json` / `asyncapi.yaml` before endpoint work) plus the backend's contract tests make future drift a review catch, not a runtime surprise. Kotlin client generation remains the standing fallback.
- [`isUndo` body field may be stale in MIGRATION_PLAN] → While updating §3, re-check the PATCH item request schema against the live OpenAPI document and correct the table in the same edit.
- [Device smoke test needed after flip] → The change is mechanical, but the app is currently 404-broken; a device smoke test after implementation confirms end-to-end recovery against the live backend.

## Migration Plan

1. Flip the three source files + test assertions (one commit).
2. Update `docs/MIGRATION_PLAN.md` §3 (versioned paths + contract source-of-truth pointer).
3. Verify: `./gradlew test` green, then a device smoke test against the live backend.
4. Rollback: revert the commit; the app returns to its previous (already broken) state, so there is no rollback risk.

## Open Questions

None.
