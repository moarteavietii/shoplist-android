# list-screen Delta

## MODIFIED Requirements

### Requirement: The view switcher is triggered from the widget stack
The shopping list UI SHALL render the view-switcher trigger inside the top-right widget stack (View Switcher control) instead of a list-area icon. The trigger SHALL toggle the view drawer open and closed and SHALL appear in an inactive/gray state when the drawer is closed and in an active state when the drawer is open.

#### Scenario: View switcher icon is visible
- **WHEN** the shopping list renders
- **THEN** the view-switcher trigger appears inside the widget stack, not as a floating list-area icon

#### Scenario: Tapping the view switcher opens the drawer
- **WHEN** the user taps the view-switcher trigger and the drawer is closed
- **THEN** a drawer slides in from the left side of the screen

#### Scenario: Tapping the view switcher closes the drawer
- **WHEN** the user taps the view-switcher trigger and the drawer is open
- **THEN** the drawer closes

#### Scenario: View switcher is gray when drawer is closed
- **WHEN** the drawer is closed
- **THEN** the view-switcher trigger is rendered in an inactive/gray color

#### Scenario: View switcher is active when drawer is open
- **WHEN** the drawer is open
- **THEN** the view-switcher trigger is rendered in an active color
