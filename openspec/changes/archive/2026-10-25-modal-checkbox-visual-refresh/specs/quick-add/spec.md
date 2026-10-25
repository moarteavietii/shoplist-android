## MODIFIED Requirements

### Requirement: Add affordance appears on empty search result

When the current search query returns no matching items, the app SHALL show an add affordance in the search row: a compact accent-filled button (Primary button style, 44dp minimum height, 8dp corner radius) labeled "Add", right-aligned in the same row as the search field. When the button appears, the search field SHALL shrink to make room for it. Tapping the button SHALL open the item-creation modal with the query pre-filled.

#### Scenario: Empty result shows the add button

- **WHEN** the user types a query that matches no items
- **THEN** a compact accent-filled "Add" button renders right-aligned in the search row and the search field shrinks to make room

#### Scenario: Non-empty result hides the add button

- **WHEN** the search query matches at least one item
- **THEN** no add button is rendered and the search field expands back to full width

### Requirement: The add-item modal presents name, quantity, and visibility

The add-item modal SHALL present: a name field with the quantity stepper in an inset pill well aligned to the right of the name field on the same row; a Temporary pill chip; a "Visible in" checklist of all shops with select-all/deselect-all; and an action row. The name field SHALL be an underline-style input (no box). The quantity stepper SHALL be compact: small circular keys inside a pill well with a bold, centered value and centered +/- glyphs, matching the web app's proportions. The Temporary control SHALL be a bordered pill chip with a small clock icon (border token, transparent background, muted label) that when on renders an accent border, accent-tint background, and accent-deep label; it SHALL be aligned to the right edge of the modal in both add and edit variants. The edit-item variant SHALL additionally present a checks counter pill (inset-well background, repeat icon, muted label) in the same meta row as the Temporary chip. The modal panel SHALL render with a drop shadow, hairline separators between sections, and bordered action buttons, and SHALL have bottom and horizontal margins so it does not touch the screen edges. Tapping a visibility row's checkbox SHALL toggle that shop; tapping the row's text SHALL NOT toggle it. Visibility rows SHALL be tightly spaced.

#### Scenario: Quantity stepper sits right of the name

- **WHEN** the add-item modal is open
- **THEN** the quantity stepper renders in a compact inset pill well on the same row as the name field, aligned to the right

#### Scenario: Temporary renders as a pill chip

- **WHEN** the item modal is open
- **THEN** the Temporary control renders as a bordered pill chip, not a LED dot with text

#### Scenario: Temporary chip on state

- **WHEN** the Temporary chip is on
- **THEN** the chip renders an accent border, accent-tint background, and accent-deep label

#### Scenario: Edit modal shows the checks counter

- **WHEN** the edit-item modal is open
- **THEN** a checks counter pill (repeat icon + count) renders in the meta row opposite the Temporary chip

#### Scenario: Visibility rows toggle only from the checkbox

- **WHEN** the user taps the text of a visibility row in the add-item modal
- **THEN** that shop's selection does not change

#### Scenario: Action buttons are bordered

- **WHEN** the add-item modal's action row is rendered
- **THEN** the Discard and Save buttons render as bordered controls with a minimum height of 44dp
