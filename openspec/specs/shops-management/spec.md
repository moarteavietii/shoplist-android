# shops-management Specification

## Purpose

Let users create, rename, and delete shops (their shopping contexts) through a centered modal editor opened from the view drawer, so managing shops never leaves the list context.

## Requirements

### Requirement: The shop editor modal opens in create and edit modes
The application SHALL provide a single shop editor modal rendered as a dialog with modal semantics and an accessible label. The modal SHALL NOT render a separate heading; the underlined name input acts as the modal's visual title. In create mode the name input SHALL start empty; in edit mode it SHALL be pre-filled with the shop's current name. While a save is in progress, the name input SHALL be disabled.

#### Scenario: Create mode starts empty
- **WHEN** the shop editor modal opens in create mode
- **THEN** the name input is empty and no separate heading is rendered

#### Scenario: Edit mode is pre-filled
- **WHEN** the shop editor modal opens in edit mode for the shop "Carrefour"
- **THEN** the name input contains "Carrefour"

#### Scenario: Input is locked while saving
- **WHEN** a shop save is in progress
- **THEN** the name input is disabled and cannot be edited

### Requirement: The shop name is validated before saving
The Save button SHALL be disabled while the trimmed name is empty. In edit mode, Save SHALL additionally be disabled until the trimmed name differs from the shop's current name. The saved name SHALL be the trimmed value.

#### Scenario: Empty name cannot be saved
- **WHEN** the name input contains only whitespace
- **THEN** the Save button is disabled

#### Scenario: Edit mode requires a change
- **WHEN** the modal is in edit mode and the name is unchanged from the shop's current name
- **THEN** the Save button is disabled

#### Scenario: Surrounding whitespace is trimmed on save
- **WHEN** the user enters "  Lidl  " and saves
- **THEN** the shop is saved with the name "Lidl"

### Requirement: The modal offers explicit dismissal paths
The modal SHALL close when the user presses the system back button, taps the overlay outside the modal, or taps the Discard button. In create mode the secondary button SHALL be labeled "Discard". In edit mode it SHALL be labeled "Discard" when the name has unsaved changes and "Close" when it has not. The secondary button SHALL be disabled while a save is in progress.

#### Scenario: Back button closes the modal
- **WHEN** the shop editor modal is open and the user presses the system back button
- **THEN** the modal closes without saving

#### Scenario: Overlay tap closes the modal
- **WHEN** the user taps the dimmed overlay outside the modal card
- **THEN** the modal closes without saving

#### Scenario: Discard label reflects unsaved changes
- **WHEN** the modal is in edit mode
- **THEN** the secondary button reads "Discard" while the name has unsaved changes and "Close" once the name matches the saved value

### Requirement: Creating a shop persists a new shop
Saving the modal in create mode SHALL create a new shop with the entered name. On success the modal SHALL close and the new shop SHALL appear in the shop list (including the view drawer) without a manual refresh.

#### Scenario: New shop is created and the modal closes
- **WHEN** the user enters "Lidl" in create mode and taps Save
- **THEN** a shop named "Lidl" is created, the modal closes, and "Lidl" appears in the view drawer list

#### Scenario: Failed creation keeps the modal open
- **WHEN** the backend rejects a shop creation (e.g. duplicate name)
- **THEN** the modal stays open, the name input is re-enabled, and an error is surfaced

### Requirement: Renaming a shop updates its name
Saving the modal in edit mode SHALL rename the shop to the entered name. On success the modal SHALL close and every surface showing the shop's name (view drawer, list watermark, item rows) SHALL reflect the new name without a manual refresh.

#### Scenario: Renamed shop updates everywhere
- **WHEN** the user renames "Carrefour" to "Carrefour Express" and saves
- **THEN** the modal closes and the view drawer shows "Carrefour Express" in its alphabetical position

### Requirement: Deleting a shop requires confirmation
The edit-mode modal SHALL render a "Delete" button in the same action row as Discard and Save, using the shared compact button geometry with danger coloring. Tapping it SHALL NOT delete immediately: a confirmation dialog SHALL open naming the shop. Only confirming the deletion SHALL delete the shop; cancelling SHALL return to the edit modal with the shop untouched. On successful deletion both dialogs SHALL close, the shop SHALL disappear from all surfaces without a manual refresh, and the Global shop SHALL never offer deletion (it has no edit entry point).

#### Scenario: Delete requires confirmation
- **WHEN** the user taps "Delete" in the edit modal for "Carrefour"
- **THEN** a confirmation dialog opens naming "Carrefour" and nothing is deleted yet

#### Scenario: Confirming deletes the shop
- **WHEN** the user confirms the deletion of "Carrefour"
- **THEN** the shop is deleted, both dialogs close, and "Carrefour" is removed from the view drawer list

#### Scenario: Cancelling keeps the shop
- **WHEN** the user cancels the deletion confirmation
- **THEN** the shop remains and the user returns to the edit modal

#### Scenario: Global shop cannot be deleted
- **WHEN** the user inspects the Global shop in the view drawer
- **THEN** it has no edit entry point and therefore no path to deletion

### Requirement: Shop editor modal remains fully reachable on small screens
When the shop editor modal's content is taller than the viewport, the modal SHALL become scrollable so all of its content — including the action buttons — remains reachable, instead of clipping content beyond the viewport edges. When the content fits, the modal SHALL remain vertically and horizontally centered.

#### Scenario: Shop modal content taller than the viewport is scrollable
- **WHEN** the shop editor modal is open on a screen too short to fit its full content
- **THEN** the modal content can be scrolled to reveal every field and action button, with no content permanently clipped

#### Scenario: Fitting shop modal stays centered
- **WHEN** the shop editor modal is open on a screen tall enough to fit its full content
- **THEN** the modal is centered within the viewport
