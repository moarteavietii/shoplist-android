## 1. Config and expiry helpers

- [x] 1.1 Add `FeatureConfig` (sibling of `UndoConfig`) with `temporaryItemTtl = 1.days` and `expiryPruneInterval = 1.minutes`; add an `Item.isExpiredAt(now)` helper with defensive ISO-8601 parsing (unparseable/absent → not expired) (TDD: boundary `expiresAt <= now` counts as expired; malformed string → not expired)

## 2. Repository expiry reconciliation

- [x] 2.1 Extend `ItemsRepository.setChecked` optimistic copy: temporary + checking → stamp `expiresAt = now + TTL`; unchecking → clear `expiresAt`; rollback restores the previous `expiresAt` (TDD: stamp on check, clear on uncheck, rollback restores)
- [x] 2.2 Extend `ItemsRepository.updateDetails` for `isTemporary` changes: temporary off → clear cached expiry; temporary on while checked → stamp; fold `quantity` into the optimistic copy and rollback (TDD: temporary off clears, temporary on + checked stamps, quantity-only change persists)

## 3. Modal state and ViewModel

- [x] 3.1 Add `quantity`/`isTemporary` (+ originals) to `AddItemUiState` and `EditItemUiState`; fold into `isUnchanged`/`canSave` so a quantity-only or temporary-only change enables Save (TDD: derivations)
- [x] 3.2 Add ViewModel intents and request wiring for quantity + temporary (TDD: intents update state; create sends fields; edit PATCHes changed fields only)

## 4. Expiry pruning in UI derivation

- [x] 4.1 Add a 1-minute tick flow to `ListViewModel` and filter expired items in `buildUiState` (covers at-load and mid-session hiding; no cache mutation, no network) (TDD: expired temporary item excluded; unexpired kept; permanent item with stale expiry kept)

## 5. UI: modal controls and list rendering

- [x] 5.1 Add the quantity stepper (decrement/current/increment, 1–99, disabled at bounds) and the Temporary toggle to the shared add/edit modal composable; add the Temporary chip to the edit modal's meta row (right-aligned, Select all/Deselect all on the left)
- [x] 5.2 Render the `x<quantity>` suffix after the item name in `ItemRow` in the muted text token when `quantity > 1`

## 6. Verification

- [x] 6.1 Run `./gradlew test` and `./gradlew lint`; fix any issues
- [x] 6.2 Device smoke test: add temporary item → check it → verify it disappears after expiry (short TTL via second client or manual `expiresAt`); edit quantity → suffix updates; uncheck clears expiry (row stays); remote expiry deletion via second client
