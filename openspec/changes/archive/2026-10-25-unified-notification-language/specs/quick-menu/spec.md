## MODIFIED Requirements

### Requirement: The quick menu is anchored to the top-right of the list viewport
The list screen SHALL render a floating quick menu container fixed at the top-right of the list viewport, over the list content. The menu SHALL NOT block list scrolling or item taps outside its own controls.

#### Scenario: Menu floats over the list
- **WHEN** the shopping list renders
- **THEN** the quick menu container is visible at the top-right, over the list content

#### Scenario: Menu does not block the list
- **WHEN** the user scrolls the list or taps an item outside the menu's controls
- **THEN** the gesture reaches the list content normally

### Requirement: The Checked-Items Toggle reflects effective checked-item visibility
The quick menu's Checked-Items Toggle SHALL derive its LED state and pressed state from the *effective* checked-item visibility: the user's show-checked preference OR manual sorting engaged (checked items forced visible). While manual sorting is engaged, the toggle SHALL render in its active (LED filled, pressed) state regardless of the stored preference, and tapping it SHALL NOT toggle the preference; instead it SHALL surface the rejection message "Checked items stay visible while manual sorting is engaged" as a danger-severity notification in the unified notification pill. When manual sorting is disengaged, the toggle SHALL return to reflecting the stored preference and taps SHALL toggle it normally.

#### Scenario: LED reflects forced visibility during manual sort
- **WHEN** manual sorting is engaged while the stored show-checked preference is off
- **THEN** the Checked-Items Toggle's LED is filled and its pressed state is active, matching the checked items being visible in the list

#### Scenario: Tap during manual sort is rejected with a message
- **WHEN** the user taps the Checked-Items Toggle while manual sorting is engaged
- **THEN** the stored preference is unchanged and a danger-severity notification reading "Checked items stay visible while manual sorting is engaged" is displayed

#### Scenario: Toggle returns to preference after manual sort
- **WHEN** manual sorting is disengaged
- **THEN** the toggle's LED and pressed state again reflect the stored show-checked preference, and taps toggle it normally
