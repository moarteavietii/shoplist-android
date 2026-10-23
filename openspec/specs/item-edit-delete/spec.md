# item-edit-delete Specification

## Purpose

Provide the in-app ability to edit an item's name and delete an item: a long-press edit modal reusing the add-item modal's layout, and a delete action inside that modal gated by a confirmation step, with optimistic update semantics and error handling consistent with the rest of the app.

## Requirements

### Requirement: Long-pressing an item row opens the edit item modal

Long-pressing an item row SHALL open an edit item modal for that item, pre-filled with the item's current name and per-shop visibility. The modal SHALL use the same bottom-sheet presentation and layout as the add-item modal: a name field, a "Visible in" checklist of all shops with select-all/deselect-all, and an action row. While the edit modal is open, drag-and-drop reordering SHALL be disabled.

#### Scenario: Long-press opens pre-filled modal

- **WHEN** the user long-presses an item row
- **THEN** the edit item modal opens, showing the item's current name in the name field and its current visibility selections in the checklist

#### Scenario: Drag is disabled while the modal is open

- **WHEN** the edit item modal is open
- **THEN** drag-and-drop reordering is disabled until the modal closes

### Requirement: The edit modal saves a renamed item

The edit modal SHALL contain a Save action that PATCHes the item's name when the name is non-blank and changed, updates the local item on success, and closes the modal. Save SHALL be disabled while the name is blank or unchanged.

#### Scenario: Save persists a renamed item

- **WHEN** the user changes the name to a non-blank value and taps Save
- **THEN** the app calls the item update endpoint with the new name, the local item reflects the new name, and the modal closes

#### Scenario: Save is disabled for blank or unchanged names

- **WHEN** the name field is blank, or the name matches the item's current name
- **THEN** the Save action is disabled

#### Scenario: Save failure rolls back

- **WHEN** the update request fails
- **THEN** the local item keeps its previous name, the modal closes, and an error message is shown

### Requirement: The edit modal discards changes

The edit modal SHALL contain a Discard action that closes the modal without applying any change and without any backend request. Tapping the scrim outside the modal and pressing back SHALL behave like Discard.

#### Scenario: Discard leaves the item unchanged

- **WHEN** the user taps Discard, taps the scrim, or presses back while the edit modal is open
- **THEN** the modal closes, the item is unchanged, and no update request is made

### Requirement: The edit modal contains a Delete action

The edit modal SHALL contain a Delete action rendered in the same action row as Save and Discard, with danger coloring. There SHALL be no horizontal divider and no separate full-width Delete row.

#### Scenario: Modal renders the Delete action

- **WHEN** the edit item modal is open
- **THEN** it shows a Delete action in the same action row as Save and Discard, with no divider separating them

#### Scenario: Tapping Delete opens the confirmation modal

- **WHEN** the user taps Delete in the edit item modal
- **THEN** the edit item modal closes and the delete confirmation modal opens for the same item

### Requirement: The delete confirmation modal shows Confirm and Cancel

The delete confirmation modal SHALL display a primary Confirm action and a secondary Cancel action, presented as a centered dialog following the app's existing confirmation-modal pattern.

#### Scenario: Modal renders confirm and cancel

- **WHEN** the delete confirmation modal is open
- **THEN** it shows a Confirm action and a Cancel action

### Requirement: Cancel closes the confirmation without deleting

Tapping Cancel SHALL close the confirmation modal and leave the item unchanged.

#### Scenario: Cancel after opening the modal

- **WHEN** the user opens the delete confirmation modal and taps Cancel
- **THEN** the modal closes and no deletion request is made

### Requirement: Confirm deletes the item optimistically

Tapping Confirm SHALL remove the item from the local list immediately, call `DELETE /api/items/:itemId`, and close the confirmation modal. On failure, the item SHALL be restored and an error message shown.

#### Scenario: Confirm deletes an existing item

- **WHEN** the user taps Confirm for an existing item
- **THEN** the item is removed from the displayed list immediately, the app calls `DELETE /api/items/:itemId`, and the confirmation modal closes

#### Scenario: Deletion failure restores the item

- **WHEN** the deletion request fails
- **THEN** the item is restored to the local list and an error message is shown

### Requirement: Deletion syncs via realtime updates

Deletions made by other clients SHALL be reflected locally when the corresponding SSE event arrives, without a full refetch-driven reload of the UI.

#### Scenario: Remote deletion removes the item locally

- **WHEN** another client deletes an item and the current client receives the corresponding SSE event
- **THEN** the deleted item is removed from the local item list
