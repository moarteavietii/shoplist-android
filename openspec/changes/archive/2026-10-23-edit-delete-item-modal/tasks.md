## 1. Edit modal state and ViewModel

- [x] 1.1 Add `EditItemUiState` (itemId, originalName, name, availableViews, selectedViewIds, derived `canSave`/`isUnchanged`) and `editItemModal` + delete-confirm state to `ListUiState` (TDD: `canSave` derivation — blank name, unchanged name, changed name)
- [x] 2.1 Add ViewModel intents: `onItemLongPressed(item)` (builds pre-filled modal state), `onEditItemTitleChanged`, `onEditItemViewToggled`, `onEditItemToggleAllTapped`, `onEditItemDiscard` (TDD: modal opens pre-filled; discard closes with no request)
- [x] 2.2 Implement optimistic rename: snapshot → local update → PATCH name → rollback + `errorMessage` on failure (TDD: success updates cache; failure restores name and shows error)
- [x] 2.3 Force `dragEnabled` off while `editItemModal != null` (TDD: drag disabled while modal open, restored after close)

## 3. Edit modal UI

- [x] 3.1 Extract the add-item modal's shared layout (name field, "Visible in" LED-dot checklist with select-all/deselect-all, action row) so add and edit share one composable; action row parameterized per mode
- [x] 3.2 Add the Delete action to the edit mode's action row: same geometry as Save/Discard, danger coloring, no divider, no separate row
- [x] 3.3 Host the edit modal in `ListScreen` (bottom sheet, AnimatedVisibility like the add modal) with BackHandler for discard; add the long-press handler to `ItemRow` (drag stays on the handle)

## 4. Delete confirmation and deletion flow

- [x] 4.1 Add the delete confirmation dialog reusing the shop-modal centered-dialog pattern (scrim, scrollable centered column, Confirm/Cancel, BackHandler); Delete tap closes the edit modal and opens the confirmation
- [x] 4.2 Wire Confirm to optimistic delete: remove locally → `ItemsRepository.delete` → restore + `errorMessage` on failure; Cancel closes with no request (TDD: success removes item; failure restores and shows error; cancel makes no request)

## 5. Verification

- [x] 5.1 Run `./gradlew test` and `./gradlew lint`; fix any issues
- [x] 5.2 Device smoke test: long-press → edit → rename → save; long-press → delete → confirm; delete failure path (backend stopped); remote deletion via second client
