## Context

The scaffold change (roadmap 1) already provides Hilt, Retrofit + OkHttp + kotlinx-serialization, the Cloudflare Access auth interceptor (per build type), and base-URL wiring. This change adds the data layer on top: models, API services, repositories, device identity, and fractional indexing. Source material: web repo specs `rest-api` (client side), `shared-types-package`, `shops-data-model`, `frontend-data-fetching` — adapted, not copied (TanStack Query → in-memory repositories, localStorage → later DataStore work).

## Goals / Non-Goals

**Goals:**
- DTOs that deserialize the backend's payloads with zero custom adapters
- Repositories as the single owner of app state (in-memory cache, StateFlow-based reads)
- `X-Device-Id` on mutations only, stable per install
- Kotlin fractional-indexing port with exhaustive unit tests (TDD)

**Non-Goals:**
- SSE / realtime sync (roadmap 3) — repositories only expose manual refetch + mutation for now
- Room or any local persistence (decisions log; arrives with offline support)
- UI code, optimistic-update semantics, undo history (roadmap 4+)
- Toast/`originDeviceId` suppression logic (roadmap 6)

## Decisions

- **Package layout: `data/` with `model/`, `api/`, `repo/` sub-packages.** Keeps networking contracts separate from cache logic; mirrors the existing package structure conventions.
- **DTOs use `@Serializable` data classes with backend field names verbatim** (`isChecked`, `orderMap`, `visibleIn`, …). The backend already emits camelCase JSON, so no `@SerialName` noise. `expiresAt` is nullable `String?` (ISO timestamp) — parsing to `Instant` is a UI-layer concern; avoids a datetime-format dependency in the model.
- **`OrderMap` = `Map<String, String>`** (shopId-as-string → fractional key), matching the JSONB shape; `VisibleIn` = `List<Long>` and `ShopId` is a `@JvmInline` `value class ShopId(val value: Long)` to mirror the web repo's type aliases with real type safety.
- **Repositories hold `MutableStateFlow` caches and expose `StateFlow` + `suspend` refetch/mutation methods.** Alternative considered: exposing cold Flow per call — rejected because roadmap 3's SSE merge rules need a single live cache to merge events into, and the list screen needs shared state across recompositions. Cache updates are synchronous on the returned item (server truth), no invalidation/refetch dance.
- **`DeviceIdInterceptor` inspects `request.method` and only adds the header for POST/PATCH/DELETE.** Alternative considered: separate OkHttp clients for reads vs writes — rejected as duplication; one client + method check is simpler and cannot drift. The id is a UUID generated once and stored via the existing `TokenStore`-style encrypted prefs pattern (DataStore migration comes later with the persistence work).
- **Device id binding lives in the main source set `NetworkModule`.** It is shared behavior, not a build-type switch — only the auth interceptor differs per build type (project particularity).
- **Fractional indexing ported from the web repo's `generateKeyBetween` as a pure function** in `domain/order/` (no framework deps), with the byte-wise comparator (`a0`-style keys, `z9` append) unit-tested against the server's documented ordering rules. Ported verbatim rather than redesigned — the server's key format is fixed, and the web implementation is battle-tested.

## Risks / Trade-offs

- [In-memory cache lost on process death] → Next fetch repopulates; server is truth. Acceptable at shopping-list scale.
- [Method-based header check could miss a future mutation verb] → Only POST/PATCH/DELETE exist in the API surface; the interceptor is one small, testable class.
- [Inline value class `ShopId` in public DTOs can leak into serialization edge cases] → Keep `visibleIn` as `List<Long>` on the wire models; value class used in domain-facing signatures only.
- [Fractional key exhaustion (very long keys after many mid-list inserts)] → Same behavior as the web app; keys regenerate on server-side visibility changes, and scale is hundreds of items.

## Migration Plan

Single `:app` module change, additive only — no migration or rollback concerns. Verification via `./gradlew test` and `./gradlew assembleDebug`.
