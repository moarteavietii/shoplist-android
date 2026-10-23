## Why

Roadmap item 6 (feature parity) is the next migration step, and the edit/delete item modal is its first slice. Today an item can only be created and checked off — there is no way to rename it or remove it from the app, so the app cannot replace the web frontend for everyday use.

## What Changes

- Long-pressing an item row opens an **edit item modal** (bottom sheet, same presentation as the add-item modal) pre-filled with the item's current name and per-shop visibility
- The edit modal reuses the add-item modal's layout: name field, "Visible in" LED-dot checklist with select-all/deselect-all, and a Save/Discard action row
- A **Delete** button joins the Save/Discard action row (danger coloring, no divider, no separate row)
- Tapping Delete closes the edit modal and opens a **delete confirmation modal** (centered dialog, Confirm/Cancel) following the existing shop-modal confirmation pattern
- Confirm calls `DELETE /api/items/:itemId` with optimistic removal + rollback on failure; Cancel closes without any request
- Remote deletions already sync via SSE (`item:deleted` cache filter) — no new realtime work
- Drag-and-drop is disabled while the edit modal is open; drag only starts from the row handle, so long-press and drag do not conflict

## Capabilities

### New Capabilities

- `item-edit-delete`: Editing an item's name via a long-press edit modal, and deleting an item through an edit-modal Delete action with a confirmation step, including optimistic update semantics and error handling.

### Modified Capabilities

## Impact

- `app/src/main/java/eu/pascariu/shoppinglist/ui/list/` — new `EditItemModal` composable (reusing `AddItemModal` structure), `EditItemUiState`/delete-confirm state in `ListUiState.kt`, new ViewModel intents in `ListViewModel.kt`, modal hosting + BackHandler in `ListScreen.kt`, long-press handler in `ItemRow.kt`
- `ItemsRepository.delete(itemId)` already exists (DELETE endpoint + cache update) — no data-layer changes
- No backend changes; no new dependencies
