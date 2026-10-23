# widget-stack Delta

## MODIFIED Requirements

### Requirement: The expanded stack holds exactly five real controls in fixed order
The expanded widget stack SHALL render exactly five controls top-to-bottom: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search. The chevron SHALL sit above all five. The Search and Checked-Items Toggle controls SHALL be real; Settings remains the only disabled placeholder.

#### Scenario: Five controls in order
- **WHEN** the stack is expanded
- **THEN** the controls appear in order: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search

#### Scenario: Search and Checked-Items Toggle are interactive
- **WHEN** the stack is expanded
- **THEN** tapping Search toggles the expandable input and tapping Checked-Items Toggle switches checked-item visibility

#### Scenario: Settings remains a placeholder
- **WHEN** the stack renders
- **THEN** Settings renders as a disabled placeholder and does not trigger actions

### Requirement: LED status dots mark togglable controls
Togglable widget stack controls SHALL render a small LED status dot: filled accent color when the toggled state is ON, hollow when OFF. The Search control SHALL always render its dot (hollow collapsed, filled expanded). The Checked-Items Toggle SHALL render its dot per its state. The View Switcher and Settings controls SHALL NOT render a dot.

#### Scenario: Search dot follows expansion
- **WHEN** the search is collapsed
- **THEN** the Search control shows a hollow dot; when expanded, a filled dot

#### Scenario: Checked-Items Toggle dot follows state
- **WHEN** the toggle is showing checked items
- **THEN** its dot is filled; when hiding, hollow

#### Scenario: View Switcher and Settings have no dot
- **WHEN** the stack renders
- **THEN** the View Switcher and Settings controls show no LED dot
