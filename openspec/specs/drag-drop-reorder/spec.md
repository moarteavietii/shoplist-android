# drag-drop-reorder Specification

## Purpose

Reorder items within the active view via handle-initiated drag-and-drop, persisting fractional-index order keys to the backend.

## Requirements

### Requirement: Drag initiates only from a dedicated row handle
Reordering SHALL initiate only from a dedicated drag handle on the row; row-body touch SHALL never start a drag, and a long-press on the row body SHALL NOT drag. The handle SHALL render only when drag is enabled (sort mode ON and Manual order); when disabled, rows SHALL use the full width without a handle zone.

#### Scenario: Handle drag reorders
- **WHEN** drag is enabled and the user drags a row's handle
- **THEN** the row reorders with the drag

#### Scenario: Row body never drags
- **WHEN** the user touches and drags the row body
- **THEN** the row does not drag (the gesture scrolls the list)

#### Scenario: Handle only renders when drag is enabled
- **WHEN** sort mode is off or the order is A→Z/Frequency
- **THEN** rows render without a handle zone

### Requirement: A drop persists a fractional-index reorder optimistically
On drop, the app SHALL compute a new fractional index between the item's neighbors (or a boundary index at the start/end), update the cache optimistically so the item stays in the dropped position until the server responds, and send `PATCH /api/v1/items/:itemId` with the merged `orderMap`. A failed reorder SHALL roll back the optimistic update and surface an error.

#### Scenario: Drop between neighbors
- **WHEN** the user drops an item between two others
- **THEN** a fractional key between the neighbors' keys is generated and sent to the backend

#### Scenario: Optimistic position holds until the response
- **WHEN** the item is dropped
- **THEN** it stays in the dropped position until the server responds, with no snap-back

#### Scenario: Failed reorder rolls back
- **WHEN** the backend rejects the reorder
- **THEN** the item returns to its previous position and an error is surfaced

### Requirement: The dragged item renders above all other content
During a drag, the dragged item SHALL render above all other content for the whole drag, and displaced rows SHALL remain fully visible without collapsing or fading.

#### Scenario: Dragged row floats above
- **WHEN** the user drags a row across the list
- **THEN** the row renders above all other content for the whole drag

#### Scenario: Displaced rows stay visible
- **WHEN** rows are displaced during a drag
- **THEN** they remain fully visible with no collapse or fade

### Requirement: All items are always visible while manual sort is engaged
While manual sort is engaged (sort mode ON and Manual order), all items in the active view SHALL always be visible — including checked items — regardless of the hide-checked toggle, so the list never reflows when a drag starts. Hidden checked items SHALL reappear the moment manual sort is engaged and hide again when it is disengaged. (Until the hide-checked batch lands, all items are always visible in every mode, so this rule is inert.)

#### Scenario: Engaging manual sort reveals all items
- **WHEN** the user enables manual sort while some checked items are hidden
- **THEN** all items in the active view appear instantly in sorted position, with no reflow of the unchecked items

#### Scenario: No reveal at drag start
- **WHEN** the user starts dragging a row while checked items are hidden
- **THEN** the list does not change — no items appear or disappear at drag start

#### Scenario: Disengaging manual sort restores hiding
- **WHEN** the user switches to A→Z or Frequency order, or disables sort mode
- **THEN** hidden checked items are hidden again per the hide toggle

### Requirement: Drag is suppressed while overlays are open
Drag SHALL be suppressed while the add-item overlay, shop editor modal, or delete confirmation is open, and while sort mode is off or the order is A→Z/Frequency. Drag SHALL resume when the overlay closes.

#### Scenario: Overlays suppress drag
- **WHEN** any list overlay is open
- **THEN** drag attempts do not start a reorder

#### Scenario: Drag resumes when overlays close
- **WHEN** an overlay closes
- **THEN** drag works again (given sort mode ON and Manual order)
