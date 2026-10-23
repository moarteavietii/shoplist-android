# widget-stack Delta

## Purpose

Provide a compact top-right floating cluster of action icons that keeps the list viewport free of persistent controls, serving as the mounting point for all togglable list features.

## ADDED Requirements

### Requirement: The widget stack is anchored to the top-right of the list viewport
The list screen SHALL render a floating widget stack container fixed at the top-right of the list viewport, over the list content. The stack SHALL NOT block list scrolling or item taps outside its own controls.

#### Scenario: Stack floats over the list
- **WHEN** the shopping list renders
- **THEN** the widget stack container is visible at the top-right, over the list content

#### Scenario: Stack does not block the list
- **WHEN** the user scrolls the list or taps an item outside the stack's controls
- **THEN** the gesture reaches the list content normally

### Requirement: The widget stack defaults to collapsed and toggles via the chevron
The widget stack SHALL default to a collapsed state rendering only a downward chevron. Tapping the chevron SHALL expand the stack downward with the chevron remaining topmost and rotating to point upward. Tapping the rotated chevron SHALL collapse the stack. Both transitions SHALL be animated.

#### Scenario: Collapsed by default
- **WHEN** the app launches for the first time
- **THEN** only the chevron is visible in the stack

#### Scenario: Chevron expands the stack
- **WHEN** the user taps the chevron while collapsed
- **THEN** the stack expands downward, the chevron rotates to point upward, and the controls appear with animation

#### Scenario: Rotated chevron collapses the stack
- **WHEN** the user taps the chevron while expanded
- **THEN** the stack collapses to the chevron only, with animation

### Requirement: The expanded stack holds exactly five controls in fixed order
The expanded widget stack SHALL render exactly five controls top-to-bottom: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search. The chevron SHALL sit above all five. Controls not yet implemented SHALL render as disabled placeholders in their reserved positions.

#### Scenario: Five controls in order
- **WHEN** the stack is expanded
- **THEN** the controls appear in order: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search

#### Scenario: Placeholder controls are visibly inert
- **WHEN** the stack renders with not-yet-implemented controls
- **THEN** those controls render as disabled placeholders and do not trigger actions

### Requirement: The View Switcher control toggles the view drawer
The View Switcher control SHALL toggle the view drawer open and closed, replacing the previous list-area trigger icon. The control SHALL appear in an inactive/gray state when the drawer is closed and in an active state when the drawer is open.

#### Scenario: View switcher opens the drawer
- **WHEN** the user taps the View Switcher control and the drawer is closed
- **THEN** the view drawer slides in from the left

#### Scenario: View switcher closes the drawer
- **WHEN** the user taps the View Switcher control and the drawer is open
- **THEN** the drawer closes

#### Scenario: View switcher reflects drawer state
- **WHEN** the drawer is closed
- **THEN** the View Switcher control is rendered in an inactive/gray color; when the drawer is open, it is rendered in an active color

### Requirement: LED status dots mark togglable controls
Togglable widget stack controls SHALL render a small LED status dot: filled accent color when the toggled state is ON, hollow when OFF. The View Switcher and Settings controls SHALL NOT render a dot. Placeholder controls SHALL NOT render a dot until implemented.

#### Scenario: View switcher has no dot
- **WHEN** the stack renders
- **THEN** the View Switcher control shows no LED dot

### Requirement: The expanded stack collapses on upward swipe
The expanded widget stack SHALL collapse when the user performs an upward swipe across it beyond a small threshold.

#### Scenario: Upward swipe collapses the stack
- **WHEN** the stack is expanded and the user swipes upward across it beyond a small threshold
- **THEN** the stack collapses

#### Scenario: Small upward movements do not collapse
- **WHEN** the user moves their finger only slightly upward on the stack
- **THEN** the stack remains expanded

### Requirement: The widget stack expand state persists across sessions
The widget stack's expanded/collapsed state SHALL persist across app sessions and be restored on launch. The state SHALL use a dedicated storage key, isolated from other persisted list state.

#### Scenario: Expanded state survives restart
- **WHEN** the user expands the stack and restarts the app
- **THEN** the stack renders expanded

#### Scenario: Collapsed state survives restart
- **WHEN** the user collapses the stack and relaunches the app
- **THEN** the stack renders collapsed
