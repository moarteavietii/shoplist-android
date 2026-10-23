## Why

Roadmap 6 (feature parity) is in progress: the edit/delete item modals are done, but the list still ignores two item fields the backend already returns — `quantity` and `isTemporary`/`expiresAt`. Users cannot buy "3 milks", and checked temporary items never disappear, so the Android app lags behind the web frontend it replaces.

## What Changes

- Quantity selector (stepper, 1–99) in the shared add/edit item modal; saving persists quantity via create/PATCH
- List rows render an `x<quantity>` suffix in a lighter color when quantity > 1
- "Temporary" toggle in the add-item modal (default off) and as a chip in the edit modal's meta row
- Client-side expiry pruning: temporary checked items whose `expiresAt` has passed are hidden locally on a 1-minute timer and at load, without waiting for the server
- Optimistic cache mirrors the server's expiry reconciliation: unchecking / making permanent clears the cached `expiresAt` in the same optimistic update; check-off of a temporary item stamps `expiresAt = now + 24h` locally
- Expiry-driven removals sync through the existing realtime deletion path (no new sync logic)

## Capabilities

### New Capabilities
- `item-quantity`: quantity selector in the item editor modal and `x<n>` suffix rendering in the list
- `temporary-items`: temporary toggle in add/edit flows and client-side expiry pruning

### Modified Capabilities

None — the quantity and temporary behaviors are additive: the new capabilities own the modal controls and the list-rendering/pruning rules end-to-end, and no existing spec requirement changes meaning.

## Impact

- `ShoplistModels.kt` — no changes needed (`quantity`, `isTemporary`, `expiresAt` already exist on `Item`, `CreateItemRequest`, `UpdateItemRequest`)
- `ListViewModel` / `ListUiState` — modal state gains quantity + temporary fields; check-off and uncheck optimistic updates gain expiry stamping/clearing; a periodic prune job is added
- `ListScreen` / item row / add + edit modal composables — stepper UI, Temporary chip, `x<n>` suffix
- No backend changes; no new dependencies
