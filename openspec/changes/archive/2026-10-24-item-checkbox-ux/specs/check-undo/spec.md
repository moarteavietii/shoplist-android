## MODIFIED Requirements

### Requirement: Check and uncheck actions commit instantly
When the user checks or unchecks an item by tapping its checkbox, the app SHALL optimistically flip the row immediately and send `PATCH /api/v1/items/:itemId` with the new `isChecked` value without any artificial delay. If the PATCH fails, the app SHALL revert the optimistic flip and SHALL NOT record a history entry. The checkbox tap SHALL be the only row-level trigger for checking or unchecking; taps on the row body outside the checkbox SHALL NOT commit a check action.

#### Scenario: Check commits immediately
- **WHEN** the user taps the checkbox for an unchecked item
- **THEN** the item immediately appears checked and a `PATCH` with `{ "isChecked": true }` is sent right away
- **AND** no countdown timer or pending state exists

#### Scenario: Failed PATCH reverts the optimistic flip
- **WHEN** the `PATCH` request for a check or uncheck fails
- **THEN** the item's checkbox returns to its previous visual state
- **AND** no history entry is recorded for that action

#### Scenario: Multiple items can be checked concurrently
- **WHEN** the user checks item A and then checks item B
- **THEN** item A remains checked
- **AND** item B also appears checked

#### Scenario: Row body tap does not commit a check
- **WHEN** the user taps the row body outside the checkbox
- **THEN** no check or uncheck is committed and no `PATCH` is sent
