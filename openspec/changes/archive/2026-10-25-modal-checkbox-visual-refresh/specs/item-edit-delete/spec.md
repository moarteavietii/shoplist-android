## MODIFIED Requirements

### Requirement: Long-pressing an item row opens the edit item modal

Long-pressing an item row SHALL open an edit item modal for that item, pre-filled with the item's current name and per-shop visibility. The modal SHALL use the same bottom-sheet presentation and layout as the add-item modal: a name field with the quantity stepper in an inset pill well aligned to the right of the name field, a "Visible in" checklist of all shops with select-all/deselect-all, and an action row. The modal panel SHALL render with a drop shadow, hairline separators between sections, and bordered action buttons (Delete = danger outline, Discard/Close = quiet bordered, Save = filled accent). Tapping a visibility row's checkbox SHALL toggle that shop; tapping the row's text SHALL NOT toggle it. While the edit modal is open, drag-and-drop reordering SHALL be disabled.

#### Scenario: Long-press opens pre-filled modal

- **WHEN** the user long-presses an item row
- **THEN** the edit item modal opens, showing the item's current name in the name field and its current visibility selections in the checklist

#### Scenario: Quantity stepper sits right of the name

- **WHEN** the edit modal is open
- **THEN** the quantity stepper renders in an inset pill well on the same row as the name field, aligned to the right

#### Scenario: Visibility rows toggle only from the checkbox

- **WHEN** the user taps the text of a visibility row in the edit modal
- **THEN** that shop's selection does not change

#### Scenario: Action buttons are bordered

- **WHEN** the edit modal's action row is rendered
- **THEN** Delete renders as a danger-outlined button, Discard/Close as a quiet bordered button, and Save as a filled accent button, each with a minimum height of 44dp

#### Scenario: Drag is disabled while the modal is open

- **WHEN** the edit item modal is open
- **THEN** drag-and-drop reordering is disabled until the modal closes
