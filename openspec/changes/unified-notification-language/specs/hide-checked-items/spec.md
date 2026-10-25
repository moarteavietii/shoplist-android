## MODIFIED Requirements

### Requirement: The Checked-Items Toggle controls checked-item visibility
The list screen's Checked-Items Toggle SHALL control whether checked items appear in the list. It SHALL be enabled (showing) by default, render an LED dot with dimmed/active styling, and expose its pressed state to accessibility services. The state SHALL persist across sessions.

While manual sorting is engaged, checked items SHALL remain visible regardless of the toggle, and the toggle SHALL reflect this effective visibility in its LED and pressed state (see the `quick-menu` capability). Tapping the toggle while manual sorting is engaged SHALL surface a rejection message instead of changing the stored preference.

#### Scenario: Toggle hides checked items
- **WHEN** the user disables the toggle
- **THEN** checked items are excluded from the view but kept in data, and unchecked items keep their order

#### Scenario: Toggle state survives restart
- **WHEN** the user disables the toggle and restarts the app
- **THEN** checked items remain hidden

#### Scenario: Pressed state is accessible
- **WHEN** an accessibility service reads the toggle
- **THEN** it exposes its pressed (showing/hiding) state

#### Scenario: Effective visibility during manual sort
- **WHEN** manual sorting is engaged while the toggle is disabled
- **THEN** checked items are visible in the list and the toggle's LED and pressed state reflect them as shown
