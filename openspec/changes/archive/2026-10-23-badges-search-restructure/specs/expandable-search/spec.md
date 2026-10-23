# expandable-search Delta

## Purpose

Provide list filtering through a widget-stack search control that expands into a full-width bottom-anchored input, reusing the app's fuzzy matcher.

## ADDED Requirements

### Requirement: The Search control expands to a bottom-anchored input
The widget stack's Search control SHALL toggle between a collapsed icon and an expanded full-width input fixed at the bottom of the list viewport. The control SHALL render gray/inactive when collapsed and active when expanded. Tapping it SHALL expand the input, which SHALL receive focus immediately (keyboard appears). The input SHALL stay fixed while scrolling and remain visible above the virtual keyboard.

#### Scenario: Tap expands search
- **WHEN** the user taps the collapsed Search control
- **THEN** a full-width input appears fixed at the bottom of the list viewport and receives focus

#### Scenario: Input stays fixed above the keyboard
- **WHEN** the user scrolls the list or the virtual keyboard appears
- **THEN** the input remains fixed at the bottom, fully visible above the keyboard

#### Scenario: LED dot marks expansion
- **WHEN** the Search control renders
- **THEN** its LED dot is hollow while collapsed and filled while expanded

### Requirement: The expanded input offers one clear button
While the query is non-empty, the input SHALL expose exactly one clear button with a touch target of at least 48dp in both dimensions. Tapping it SHALL clear the query and show the full list.

#### Scenario: Clear button appears with text
- **WHEN** the query is non-empty (focused or not)
- **THEN** exactly one clear button is visible with a ≥48dp touch target

#### Scenario: Clearing restores the full list
- **WHEN** the user taps the clear button
- **THEN** the query is emptied and the full list is shown

### Requirement: Swipe-right dismisses the expanded input
The expanded search bar SHALL dismiss when the user swipes it to the right beyond a small threshold; the list SHALL animate back to fill the space.

#### Scenario: Swipe-right dismisses search
- **WHEN** the user swipes the expanded input to the right beyond a small threshold
- **THEN** the bar dismisses and the list animates back up to fill the space

#### Scenario: Small movements do not dismiss
- **WHEN** the user moves the input only slightly rightward
- **THEN** the bar remains expanded

### Requirement: Collapsing clears the query and the expanded state persists
Collapsing the search SHALL clear the query so the list returns to unfiltered. The expanded/collapsed state SHALL persist across sessions; the query itself SHALL never be persisted to storage or the server.

#### Scenario: Collapsing resets filtering
- **WHEN** the user collapses the search while a query is active
- **THEN** the query is cleared and the full list is shown

#### Scenario: Expanded state survives restart
- **WHEN** the user expands search and restarts the app
- **THEN** search renders expanded

#### Scenario: Query never persists
- **WHEN** the user types a query and restarts the app
- **THEN** the query is not restored

### Requirement: Fuzzy filtering matches case-insensitively with out-of-order characters
The input SHALL filter the list as the user types using fuzzy matching: case-insensitive, with query characters matching the item name as a subsequence (out-of-order matching within the name per the web app's semantics — e.g. "mlk" matches "Milk"). Matching items SHALL keep their existing order; an empty query SHALL show all items.

#### Scenario: Out-of-order subsequence matches
- **WHEN** the user types "mlk"
- **THEN** "Milk" is shown as a match

#### Scenario: Matching items keep their order
- **WHEN** a query matches several items
- **THEN** the matches appear in their pre-query relative order

#### Scenario: Empty query shows all
- **WHEN** the query is emptied
- **THEN** the full list is shown

### Requirement: Empty results expose the add-item button
When a non-empty query matches no items, an add-item affordance SHALL appear next to the input; it SHALL be hidden when results exist or the query is empty. Tapping it SHALL open the add-item overlay with the query pre-filled.

#### Scenario: Add affordance appears on empty results
- **WHEN** the query matches no items
- **THEN** an add-item button appears next to the input

#### Scenario: Add affordance hidden otherwise
- **WHEN** results exist or the query is empty
- **THEN** the add-item button is hidden
