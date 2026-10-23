## Context

The list screen has an add-item modal (bottom sheet: name field + "Visible in" LED-dot checklist + Save/Discard row) and a shop modal (centered dialog with an edit mode and a delete-confirmation sub-state). `ItemsRepository.delete(itemId)` already exists and the SSE `item:deleted` cache filter already handles remote deletions. The edit modal is the first slice of roadmap 6; quantities, temporary items, and change toasts follow in later slices and will extend the same modal.

## Goals / Non-Goals

**Goals:**

- One edit modal that reuses the add-item modal's layout and presentation, so later roadmap-6 fields (quantity, temporary toggle) slot into the same sheet
- Delete with confirmation, mirroring the shop modal's confirm/cancel pattern
- Optimistic rename and delete with rollback, consistent with the check-off semantics already in the ViewModel

**Non-Goals:**

- No quantity, temporary-item, or expiry fields (later roadmap-6 slices)
- No change toasts (later slice)
- No new realtime work — SSE deletion sync already works
- No data-layer changes

## Decisions

### Reuse the add-item modal layout via a shared composable, not a copy
`AddItemModal` already renders exactly what the edit modal needs (name field, visibility checklist, action row). Extract its body into a shared `ItemModalContent` (or parameterize `AddItemModal`) that both add and edit flows render, with the action row parameterized so edit mode can inject the Delete action. Alternative considered: a separate `EditItemModal` composable duplicating the layout — rejected, it would drift from the add modal as roadmap-6 fields are added.

### Edit state lives in `ListUiState` as `editItemModal: EditItemUiState?`
Mirrors `addItemModal`/`shopModal`: a nullable modal state the ViewModel owns, with `itemId`, `originalName`, `name`, `availableViews`, `selectedViewIds`, and derived `canSave` (non-blank + changed). The delete confirmation follows the `ShopModalUiState.deleteConfirmOpen` precedent — a `deleteConfirmItem: Item?` (or equivalent) in the UI state — rather than a separate screen. Alternatives considered: reusing `AddItemUiState` with a nullable `editItemId` — rejected, the two flows have different save semantics (POST vs PATCH) and conflating them complicates `canSave`.

### Long-press on the row opens the modal; drag stays on the handle
`ItemRow` gains a long-press handler (the row already uses a clickable/tap gesture; long-press is added there). Drag already starts only from the per-row handle and only in Manual sort mode, so there is no gesture conflict; while the edit modal is open, `dragEnabled` is forced off in the ViewModel (the modal state gates it) to satisfy the spec.

### Optimistic semantics: snapshot-previous → mutate → rollback-on-error
Rename: apply the new name to the local cache immediately, PATCH; on failure restore the snapshot and surface `errorMessage` (existing UI state field). Delete: remove from cache immediately, DELETE; on failure re-insert at its previous position and surface the error. The SSE echo of own mutations reconciles idempotently (existing diff-checked `item:updated` merge). This matches the migration plan's "Delete / update item: snapshot-previous → mutate → rollback-on-error → invalidate-on-settle" contract.

### Delete confirmation as a centered dialog, not a bottom sheet
The web app shows a centered confirmation dialog; the shop modal already implements exactly that presentation (scrim, centered column, scrollable, Confirm/Cancel). Reuse that pattern for visual consistency. Alternative: a second bottom sheet — rejected, it stacks awkwardly over the edit sheet and diverges from the established confirmation pattern.

## Risks / Trade-offs

- [Rename PATCH and SSE echo race] → The cache merge is diff-checked and idempotent; the echo either matches the optimistic state (no-op) or arrives first (merge is a no-op after the optimistic update). Same pattern as check-off, already proven.
- [Delete while an undo entry references the item] → Undo already validates before PATCHing (expired/deleted/state mismatch → drop silently), so a deleted item's undo entry is dropped silently. No new handling needed.
- [Delete button next to Save invites accidental taps] → Confirmation modal is the mitigation; Delete uses danger coloring and the confirm step is mandatory.

## Open Questions

None.
