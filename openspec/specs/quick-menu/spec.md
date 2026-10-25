# quick-menu Specification

## Purpose

Provide a compact top-right floating menu of card-style action buttons that keeps the list viewport free of persistent controls, serving as the mounting point for all togglable list features. Visually mirrors the web app's burger menu.

## Requirements

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

### Requirement: The quick menu container is a near-opaque floating surface
The quick menu container SHALL render as a near-opaque surface (high-alpha surface color, no backdrop blur) with a soft drop shadow and rounded corners. The container SHALL remain legible over any list content beneath it.

#### Scenario: Container styling
- **WHEN** the quick menu renders
- **THEN** the container uses a high-alpha surface color with rounded corners and a soft shadow, without backdrop blur

#### Scenario: Legible over list content
- **WHEN** list rows scroll beneath the quick menu
- **THEN** the menu's controls remain fully legible

### Requirement: The quick menu defaults to collapsed and toggles via the chevron
The quick menu SHALL default to a collapsed state rendering only a downward chevron. Tapping the chevron SHALL expand the menu downward with the chevron remaining topmost and rotating to point upward. Tapping the rotated chevron SHALL collapse the menu. Both transitions SHALL be animated.

#### Scenario: Collapsed by default
- **WHEN** the app launches for the first time
- **THEN** only the chevron is visible in the menu

#### Scenario: Chevron expands the menu
- **WHEN** the user taps the chevron while collapsed
- **THEN** the menu expands downward, the chevron rotates to point upward, and the controls appear with animation

#### Scenario: Rotated chevron collapses the menu
- **WHEN** the user taps the chevron while expanded
- **THEN** the menu collapses to the chevron only, with animation

### Requirement: Each control renders as a bordered card button
Each quick menu control SHALL render as a square bordered card button (1px border, rounded corners, surface background) sized for comfortable touch. Inactive controls SHALL be dimmed: muted background and faint icon tint. Active controls SHALL use the full surface background and normal icon tint.

#### Scenario: Card button styling
- **WHEN** the menu is expanded
- **THEN** each control renders as a bordered rounded card button with surface background

#### Scenario: Inactive controls are dimmed
- **WHEN** a control's feature is off (drawer closed, search collapsed, checked items hidden, sort mode off with manual order)
- **THEN** that control renders with a muted background and faint icon tint

#### Scenario: Active controls are undimmed
- **WHEN** a control's feature is on
- **THEN** that control renders with the full surface background and normal icon tint

### Requirement: The expanded menu holds exactly five real controls in fixed order
The expanded quick menu SHALL render exactly five controls top-to-bottom: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search. The chevron SHALL sit above all five. The Search and Checked-Items Toggle controls SHALL be real; Settings remains the only disabled placeholder.

#### Scenario: Five controls in order
- **WHEN** the menu is expanded
- **THEN** the controls appear in order: View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search

#### Scenario: Search and Checked-Items Toggle are interactive
- **WHEN** the menu is expanded
- **THEN** tapping Search toggles the expandable input and tapping Checked-Items Toggle switches checked-item visibility

#### Scenario: Settings remains a placeholder
- **WHEN** the menu renders
- **THEN** Settings renders as a disabled placeholder and does not trigger actions

### Requirement: The View Switcher control toggles the view drawer and carries the unchecked badge
The View Switcher control SHALL toggle the view drawer open and closed. The control SHALL appear dimmed when the drawer is closed and active when the drawer is open. When the global unchecked count is greater than zero, the View Switcher button SHALL render a count badge at its top-right corner; the chevron SHALL NOT render a badge.

#### Scenario: View switcher opens the drawer
- **WHEN** the user taps the View Switcher control and the drawer is closed
- **THEN** the view drawer slides in from the left

#### Scenario: View switcher closes the drawer
- **WHEN** the user taps the View Switcher control and the drawer is open
- **THEN** the drawer closes

#### Scenario: View switcher reflects drawer state
- **WHEN** the drawer is closed
- **THEN** the View Switcher control is rendered dimmed; when the drawer is open, it is rendered active

#### Scenario: Badge sits on the view switcher
- **WHEN** the global unchecked count is greater than zero
- **THEN** the View Switcher button shows a count badge at its top-right corner and the chevron shows none

### Requirement: LED status dots mark togglable controls at the button corner
Togglable quick menu controls SHALL render a small LED status dot at the button's top-right corner: filled accent color with a soft glow when the toggled state is ON, hollow ring when OFF. The Search control SHALL always render its dot (hollow collapsed, filled expanded). The Checked-Items Toggle SHALL render its dot per its state. The Sort Mode control SHALL render its dot per the sort-mode LED state (filled when sort mode is on with manual order, hollow when off with manual order, none for non-manual orders). The View Switcher and Settings controls SHALL NOT render a dot.

#### Scenario: Search dot follows expansion
- **WHEN** the search is collapsed
- **THEN** the Search control shows a hollow corner dot; when expanded, a filled glowing dot

#### Scenario: Checked-Items Toggle dot follows state
- **WHEN** the toggle is showing checked items
- **THEN** its corner dot is filled with glow; when hiding, hollow

#### Scenario: Sort mode dot follows LED state
- **WHEN** sort mode is enabled with manual order
- **THEN** the Sort Mode control's corner dot is filled; when sort mode is off with manual order, hollow; for non-manual orders, no dot

#### Scenario: View Switcher and Settings have no dot
- **WHEN** the menu renders
- **THEN** the View Switcher and Settings controls show no LED dot

### Requirement: The sort-order selector offers icon options with a check mark
Long-pressing the Sort Mode control SHALL open a selector listing the sort orders, each with its own icon and label, and a check mark next to the selected order. Selecting an order SHALL apply it and close the selector.

#### Scenario: Selector lists orders with icons
- **WHEN** the user long-presses the Sort Mode control
- **THEN** a selector appears listing Manual, A→Z, and Frequency, each with its own icon, and a check mark next to the selected order

#### Scenario: Selecting an order applies it
- **WHEN** the user taps an order in the selector
- **THEN** the order is applied and the selector closes

### Requirement: The expanded menu collapses on upward swipe
The expanded quick menu SHALL collapse when the user performs an upward swipe across it beyond a small threshold.

#### Scenario: Upward swipe collapses the menu
- **WHEN** the menu is expanded and the user swipes upward across it beyond a small threshold
- **THEN** the menu collapses

#### Scenario: Small upward movements do not collapse
- **WHEN** the user moves their finger only slightly upward on the menu
- **THEN** the menu remains expanded

### Requirement: The quick menu expand state persists across sessions
The quick menu's expanded/collapsed state SHALL persist across app sessions and be restored on launch. The state SHALL use a dedicated storage key, isolated from other persisted list state.

#### Scenario: Expanded state survives restart
- **WHEN** the user expands the menu and restarts the app
- **THEN** the menu renders expanded

#### Scenario: Collapsed state survives restart
- **WHEN** the user collapses the menu and relaunches the app
- **THEN** the menu renders collapsed
