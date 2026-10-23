## Why

Step 6 (feature parity) requires per-item multi-shop visibility, but the only spec'd Android surfaces are item creation (`quick-add`) and view filtering (`list-screen`). The edit modal's shop-visibility selector — the core of the web app's `frontend-view-visibility` capability — is implemented on this branch without a governing spec, so its contract (shop list ordering, selection state, save/discard semantics) is unverified and unarchived.

## What Changes

- Add a new `item-visibility` capability spec covering the shop-visibility selector in the item edit modal: available-shops list (Global first, then A→Z), pre-selection from the item's current `visibleIn`, toggle-without-persist, save enabled only on change, discard/close behavior, and persistence via `PATCH /api/items/:itemId` with `visibleIn`.
- Cover the quick-add path's visibility default (item created from a shop view is visible in that view) as part of the same capability, complementing `quick-add`.
- Verify the existing implementation against the spec and close residual gaps found during verification (e.g. secondary button Close/Discard labeling, no auto-focus on the name field) with tests.

## Capabilities

### New Capabilities
- `item-visibility`: Controlling which shops an item appears in — the visibility checklist in the item edit modal, its ordering and selection rules, save/discard semantics, and how visibility changes propagate to the list and to other devices via realtime sync.

### Modified Capabilities

## Impact

- `app/src/main/java/eu/pascariu/shoppinglist/ui/list/` — `AddItemModal.kt` (edit-modal checklist UI), `ListViewModel.kt` (selection state, save diffing), `ListUiState.kt` (`EditItemUiState`).
- `app/src/main/java/eu/pascariu/shoppinglist/data/` — `ShoplistModels.kt` (`visibleIn` on item payloads), `ItemsRepository.kt` (`updateDetails`).
- Tests: `ListViewModelTest.kt`, `ItemModalUiStateTest.kt` gain visibility-selection coverage.
- No backend changes; the REST + SSE contract is reused as-is.
