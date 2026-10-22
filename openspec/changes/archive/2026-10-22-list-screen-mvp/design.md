## Context

The app has a working foundation: Hilt DI, Retrofit/OkHttp networking with Cloudflare Access auth (no-op in debug), `ItemsRepository`/`ShopsRepository` with in-memory caches exposed as observable streams, SSE realtime sync with lifecycle-aware connections, fractional indexing, and a token-based Compose theme (`ShoplistTheme`, `ShoplistColors`). The home content currently shows the health-check placeholder. The backend API is reused as-is; no backend changes are permitted.

Key constraints from the migration plan: server is source of truth (no Room), undo history is device-local and ported verbatim from the web app (50 entries / 30-min TTL / `isUndo: true` PATCH semantics), SSE merge rules already exist in the repository, and the design language is the ported chromeless/borderless/LED-dot aesthetic with Material 3 only as an unthemed toolkit.

## Goals / Non-Goals

**Goals:**
- A single-screen Compose list UI that reads items from `ItemsRepository`'s observable stream and renders them bottom-anchored in the custom design language
- Instant optimistic check-off with rollback, wired through the repository
- Device-local persisted undo history with web-identical semantics
- Quick-add flow from empty search with multi-view visibility selection
- Unit-testable state logic (sorting, filtering, undo validation) separated from Compose UI

**Non-Goals:**
- View drawer / shop management (roadmap 5) — MVP renders the Global view only; the watermark and view plumbing are built so a view switcher can be added later
- Sort modes, show/hide checked toggle UI, drag-and-drop reorder (roadmap 5) — only the fly-out animation hook for hide-checked is included
- Edit/delete item modals, quantities, temporary items, change toasts (roadmap 6)
- Offline support / mutation queue (roadmap 8)

## Decisions

- **ViewModel as state holder, repository as state source.** `ListViewModel` collects `ItemsRepository.items` and `ShopsRepository.shops`, applies MVP presentation logic (Global-view filter, manual sort, fuzzy search filter), and exposes a single `StateFlow<ListUiState>`. Check/undo/add calls go through the ViewModel into the repositories. Alternative: Compose state directly in the screen — rejected because sorting/filtering/undo validation need plain unit tests and the screen should stay declarative.

- **Optimistic check lives in `ItemsRepository`.** A new `setChecked(itemId, isChecked, isUndo)` method flips the cache entry immediately, PATCHes, and on success swaps in the server item; on failure it restores the pre-call cached entry and rethrows. Alternative: optimistic flip only in the ViewModel — rejected because SSE `item:updated` events can arrive mid-flight and the cache is the single state source; the rollback must be atomic with the cache, not with UI state.

- **Undo history as a separate `UndoHistoryStore` (DataStore-backed).** Serialized as JSON (kotlinx-serialization) into Preferences DataStore; exposes `StateFlow<List<UndoEntry>>` plus `record/prune/removeTop/restoreTop`. Config constants (`UNDO_MAX_ENTRIES = 50`, `UNDO_TTL = 30 min`, `UNDO_PRUNE_INTERVAL = 60 s`, `UNDO_INFLIGHT_WAIT = 2 s`) live in a code-level `UndoConfig` object, mirroring the web app's config module. Alternative: Room — rejected, no local DB until roadmap 8. Alternative: keeping history only in the ViewModel — rejected, the web spec requires persistence across restarts.

- **Undo orchestration in the ViewModel, not the store.** On undo tap: wait (bounded 2 s) for in-flight check mutations (tracked with an in-ViewModel counter of pending check calls), prune expired entries, then walk entries top-down: skip if item missing from cache or `entry.to != item.isChecked`; otherwise call `setChecked(itemId, entry.from, isUndo = true)`. On PATCH failure, restore the entry and roll back. One entry per tap. This mirrors the web implementation's logic 1:1 and is fully unit-testable with fakes.

- **Bottom anchoring via reverse layout + `Arrangement.Bottom`.** `LazyColumn` with `reverseLayout = true` gives bottom-anchored growth with normal scrolling semantics for free (index 0 = bottom). The undo bar and search field sit in a bottom `Column` below the list; the list's `contentPadding.bottom` tracks the expanded-search height so items never render behind it. Keyboard handling uses `imePadding()` on the root with `adjustResize`-compatible edge-to-edge insets. Alternative: measuring content height and padding manually — rejected as fragile with dynamic font scales.

- **Watermark as a non-interactive `Box` behind the list.** A `Box` layer with the view name at ~7% opacity, placed behind the `LazyColumn` in a `Box` container; Compose hit-testing already passes through non-clickable layers, so no special handling is needed. The watermark text derives from the active view state (constant "Global" for MVP).

- **Fuzzy search as a pure function.** `fun fuzzyMatch(query: String, name: String): Boolean` — case-insensitive in-order subsequence scan — in the domain package next to `FractionalIndexing`, with unit tests. The add affordance shows when the filtered list is empty and the query is non-blank.

- **Add-item modal as a Compose dialog in the design language.** State held in the ViewModel (`AddItemUiState`: title, available views Global-first-then-alphabetical, selected view ids, select-all toggle state). Save calls `ItemsRepository.create(...)` with `visibleIn` = selected ids; on success the ViewModel clears the query and signals the screen to scroll to the bottom and refocus search. Global is always present in the checklist (server normalizes empty `visibleIn` to `[globalShopId]`).

- **Fly-out animation via `LazyColumn` item animations.** `Modifier.animateItem()` with a custom fade+translate-x spec on removal when hide-checked filtering drops the row. For MVP, hide-checked defaults to off (the toggle UI arrives in roadmap 5), but the animation path is implemented and covered by the check-undo spec.

- **Home content swap in `MainActivity`.** After auth/health gate passes, show `ListScreen` instead of the health placeholder. Health screen remains reachable for diagnostics (kept as-is; its entry point is unchanged).

## Risks / Trade-offs

- [SSE `item:updated` arriving between optimistic flip and PATCH response can cause a flicker] → The repository's diff-checked merge only replaces when the payload differs; the PATCH response swap is last-write-wins. Worst case is a one-frame flicker; server remains truth.
- [Undo entry validation races with concurrent remote changes] → Mitigated by the bounded wait for in-flight own-device PATCHes plus the `to`-vs-current comparison; remote changes arriving after validation are handled by the server as the source of truth (PATCH simply succeeds or fails).
- [DataStore JSON serialization of history could corrupt on partial writes] → DataStore's atomic file write semantics plus a defensive parse (corrupt/absent file → empty history) keep the failure mode to "undo history lost", never a crash.
- [reverseLayout complicates "scroll to bottom after add"] → With `reverseLayout = true`, the bottom of the list is scroll position 0; scrolling to the newly added item is a simple `scrollToItem(0)` after the cache emits the created item.
- [Keyboard insets on the physical test device] → `imePadding()` with edge-to-edge is standard; verified on-device during apply (device gotcha: remember `adb reverse tcp:3005 tcp:3005` after reconnect).

## Migration Plan

Single-app change, no deployment steps beyond the normal flow: implement on branch `list-screen-mvp`, verify with `./gradlew test` and `assembleDebug`, exercise on the physical device via the dev tunnel, then `/opsx-archive` and update the traceability matrix rows for the five mapped web specs.

Rollback: revert the branch; no data migrations exist (undo history storage is additive and can be ignored by older builds).

## Open Questions

None — the web specs and the migration plan's decisions log settle the semantics; remaining choices (exact paddings, animation durations) are visual tuning done during implementation.
