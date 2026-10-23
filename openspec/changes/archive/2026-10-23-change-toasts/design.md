## Context

The realtime layer already provides the inputs: `ItemEvent.originDeviceId` on SSE events, a persisted device id (`DeviceIdStore`), diff-checked `item:updated` merging in `RealtimeSyncManager`, and a DataStore-backed preferences source (`ListPreferencesDataSource`) with existing boolean flows. There is no toast surface. See proposal.md for motivation and the toggle-UI deferral decision.

## Goals / Non-Goals

**Goals:**
- Derive toast labels from item-event diffs, with own-device and no-op suppression.
- Present one self-replacing toast over the list screen, matching the custom design language.
- Persist `changeToastsEnabled` (default true) and honor it in the toast layer.

**Non-Goals:**
- No settings-drawer UI (roadmap 7); only the persisted preference.
- No Escape-key dismissal (mobile-first; dropped from the web contract).
- No shop-event toasts (shop created/renamed/deleted) — the web contract scopes toasts to item changes.
- No backend changes.

## Decisions

- **Derivation as a pure function over event + previous item.** A pure `deriveChangeToasts(event, previousItem)` (event payload + cached item before merge → list of labels) keeps the logic unit-testable without Compose or coroutines, and mirrors the web app's client-side diff approach. Alternative — diffing inside the composable — was rejected: it couples presentation to sync timing.
- **Hook into `RealtimeSyncManager` after cache merge, not into the repository.** The sync manager already sees every item event with its `originDeviceId` and owns the merge order; the repository stays a pure cache. The manager emits toast labels through a callback/flow the ViewModel collects.
- **Delete toasts use the pre-delete name.** `item:deleted` events carry only `itemId`, so the derivation must resolve the name from the cache before `applyDeleted` removes it. This ordering is a contract, not an implementation detail.
- **Single toast as ViewModel state.** One `StateFlow<ChangeToast?>` plus a queued list; replacement updates content in place (stable composable identity, `AnimatedContent`-free) so the entrance animation plays only on appear. Auto-dismiss via a 3.5 s coroutine cancelled on replacement.
- **Swipe via `draggable` horizontal drag with threshold.** Pointer capture is Compose's default within a drag detector; exit animation follows the drag direction, fade-out for other dismissals.
- **Preference in `ListPreferencesDataSource`.** A `changeToastsEnabled: StateFlow<Boolean>` (default true) persisted in DataStore, consistent with the existing widget/sort prefs. The toast layer collects it; no UI writes it yet.

## Risks / Trade-offs

- [Toast derivation races the cache merge (needs the pre-merge item)] → Derivation runs inside the sync manager before/around merge, with the previous item captured first; covered by unit tests on ordering.
- [Queued toasts feel noisy on busy lists] → One-at-a-time with in-place replacement matches the web contract; the queue drains by replacement, so bursts collapse into a single visible toast.
- [Events without `originDeviceId` (older backend?) would be treated as remote] → The backend always stamps `originDeviceId`; if absent, treat as remote (fail-open toward showing toasts) and revisit only if it proves wrong.

## Migration Plan

Not applicable — single-module app change, no data migration. Rollback is reverting the branch.

## Open Questions

None.
