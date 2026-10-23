# widget-stack Delta

## MODIFIED Requirements

### Requirement: The expanded stack holds exactly five controls in fixed order
The expanded widget stack SHALL render exactly five controls top-to-bottom: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search. The chevron SHALL sit above all five. The Sort Mode control SHALL be real (tap toggles sort mode, long-press opens the sort-order selector); Settings and Checked-Items Toggle remain disabled placeholders in this change.

#### Scenario: Five controls in order
- **WHEN** the stack is expanded
- **THEN** the controls appear in order: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search

#### Scenario: Sort Mode is interactive
- **WHEN** the stack is expanded
- **THEN** tapping Sort Mode toggles manual reordering and long-pressing it opens the sort-order selector

#### Scenario: Remaining placeholders are visibly inert
- **WHEN** the stack renders with not-yet-implemented controls
- **THEN** those controls render as disabled placeholders and do not trigger actions
