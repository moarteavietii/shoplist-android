## Context

The data model already carries everything: `Item` has `quantity`, `isTemporary`, and `expiresAt` (ISO-8601 string), and `CreateItemRequest`/`UpdateItemRequest` accept `quantity`/`isTemporary`. The server owns the expiry lifecycle (stamps on check-off, clears on uncheck/permanent, deletes expired rows on read and broadcasts `item:deleted`). The client's job is: expose the two fields in the add/edit modal, render the quantity suffix, mirror the server's expiry reconciliation optimistically, and hide expired rows locally so the UI never shows a dead item between expiry and the server's deletion event.

Relevant existing code: `ItemsRepository.setChecked` does the optimistic check-off (cache copy → PATCH → replace with server item, rollback on failure); `updateDetails` does the optimistic name/visibility save for the edit modal; `ListViewModel.buildUiState` derives `filteredItems` from the repository cache; `UndoConfig` in `data/undo` is the established pattern for code-level config constants.

## Goals / Non-Goals

**Goals:**
- Quantity stepper (1–99) and Temporary toggle in the shared add/edit modal
- `x<n>` suffix on list rows when quantity > 1
- Optimistic cache that always mirrors the server's expiry reconciliation (stamp on entering temporary+checked, clear on leaving it)
- Local hiding of expired rows on a 1-minute tick and at load, without waiting for the server

**Non-Goals:**
- Any expiry enforcement beyond hiding rows — the server deletes expired items; the client never deletes on its own
- Persisting expiry state locally (no Room; the in-memory cache refetches on reconnect)
- A visible countdown or expiry indicator in the row UI

## Decisions

**1. Expiry reconciliation lives in `ItemsRepository`, not the ViewModel.**
`setChecked` extends its optimistic copy: when the item is temporary and the target state is checked, stamp `expiresAt = now + 24h`; when the target is unchecked, clear it. `updateDetails` (edit modal) does the same reconciliation for `isTemporary` changes: temporary off → clear; temporary on while checked → stamp. Rationale: the spec requires the prune to never act on a stale cached expiry, and the cache is the single source the prune reads. Doing it at the repository layer means every consumer (list, badges, undo validation) sees consistent state, and rollback restores the previous `expiresAt` together with `isChecked`. Alternative considered: stamping/clearing in ViewModel intents — rejected because the cache could briefly hold a stale expiry between the intent and the repository call.

**2. Expired rows are hidden by UI-state derivation, not by mutating the cache.**
`buildUiState` filters out items where `isTemporary && expiresAt != null && parsed <= now`. A 1-minute tick (flow in the ViewModel, `viewModelScope`) re-runs the derivation; the initial derivation at load covers the at-restore case. Rationale: the cache should reflect server state; removing rows locally would make a refetch resurrect them (flicker) until the server's deletion event arrives. Derivation is idempotent and self-healing. Alternative considered: pruning the cache on a timer — rejected for the resurrection flicker and because it duplicates the server's deletion responsibility.

**3. `expiresAt` parsing is defensive.** A small helper (`Instant.parse` in a try/catch) treats unparseable/absent values as not expired. The server sends ISO-8601; a malformed value must never crash the list.

**4. Config constants follow the `UndoConfig` pattern.** New `data/config/FeatureConfig.kt` (or a sibling of `UndoConfig`) with `temporaryItemTtl: Duration = 1.days` and `expiryPruneInterval: Duration = 1.minutes`. The TTL is only used for the optimistic stamp; the server's value always wins once the response arrives.

**5. Modal state extends the existing UI-state classes.** `AddItemUiState` gains `quantity: Int = 1` and `isTemporary: Boolean = false`; `EditItemUiState` gains the same plus originals, folded into `isUnchanged`/`canSave` so a quantity-only or temporary-only change enables Save. The stepper and Temporary chip are added to the shared modal composable so add and edit stay one layout, per the existing shared-modal structure.

**6. Quantity suffix is a styled span in `ItemRow`.** Name text followed by `x<n>` in the theme's muted/secondary text token when `quantity > 1`. No new composable; matches the web's lighter-suffix treatment.

## Risks / Trade-offs

- [Clock skew between device and server] → The optimistic stamp is provisional; the server response replaces it within one round-trip. The prune uses device time only for hiding, and the server's deletion event is the authoritative removal.
- [1-minute prune delay shows an expired row up to 60 s] → Accepted, matches the web contract; the server's `item:deleted` broadcast usually removes it sooner.
- [Undo re-check stamps a fresh optimistic expiry that differs from the server's] → Same provisional-stamp argument; the PATCH response reconciles.
- [Tick runs while backgrounded] → The tick only recomposes local state, performs no network; the backgrounded no-network rule is unaffected.

## Migration Plan

Single release; no data migration. Rollback is a plain revert — the new fields are already in the API contract and simply ignored if the UI regresses.

## Open Questions

None.
