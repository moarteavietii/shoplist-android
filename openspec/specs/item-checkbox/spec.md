# item-checkbox Specification

## Purpose

Define the item row checkbox: its themed circle visual with checkmark, the check animation, the checkbox-only tap target with accessibility semantics, and inert row-body taps.

## Requirements

### Requirement: Item rows render a themed circle checkbox
Each shopping list item row SHALL render a circle checkbox instead of the decorative LED dot. When unchecked, the checkbox SHALL render as a circle in the theme's LED-off color. When checked, the checkbox SHALL render filled in the theme's LED-on color with a checkmark drawn inside. Checkbox colors SHALL come from the design-token theme system, and no new theme tokens SHALL be introduced.

#### Scenario: Unchecked checkbox appearance
- **WHEN** an item is unchecked
- **THEN** its row renders a circle in the theme's LED-off color with no checkmark

#### Scenario: Checked checkbox appearance
- **WHEN** an item is checked
- **THEN** its row renders a circle filled in the theme's LED-on color with a checkmark inside

#### Scenario: Checkbox follows the active theme
- **WHEN** the user switches to a different theme
- **THEN** the checkbox colors update to the new theme's LED-on and LED-off colors

### Requirement: Checking animates with a color crossfade and checkmark draw-in
When the checkbox state changes, the fill color SHALL crossfade between the LED-off and LED-on colors and the checkmark SHALL draw in via a stroke animation, both over the app's standard short transition duration. The animation SHALL run in both directions (check and uncheck).

#### Scenario: Check animates in
- **WHEN** the user checks an item via the checkbox
- **THEN** the fill color crossfades to the LED-on color and the checkmark strokes in, completing together

#### Scenario: Uncheck animates out
- **WHEN** the user unchecks an item via the checkbox
- **THEN** the fill color crossfades back to the LED-off color and the checkmark strokes out

### Requirement: Only the checkbox toggles the checked state
Tapping the checkbox SHALL be the only way to check or uncheck an item from the row. The checkbox SHALL provide a touch target of at least 48dp in both dimensions, centered on the visual circle. Tapping the row body outside the checkbox SHALL NOT change the checked state.

#### Scenario: Checkbox tap toggles the item
- **WHEN** the user taps the checkbox of an item
- **THEN** the item's checked state flips and the check or uncheck commits per the check-undo behavior

#### Scenario: Row body tap does not toggle
- **WHEN** the user taps the item name or any other part of the row outside the checkbox
- **THEN** the item's checked state does not change

#### Scenario: Checkbox meets minimum touch target
- **WHEN** the checkbox is rendered
- **THEN** its touch target is at least 48dp in both dimensions with the visual circle centered

### Requirement: Checkbox exposes checked state to accessibility
The checkbox SHALL carry the checkbox role and expose its checked state to accessibility services, so screen readers announce the current state and the control as a checkbox.

#### Scenario: Screen reader announces the checkbox
- **WHEN** an accessibility service reads an item row
- **THEN** the checkbox exposes its checked state and is announced with the checkbox role

### Requirement: Long-press on the row opens item edit
Long-pressing the row body SHALL open the item edit modal. Row-body taps SHALL remain inert while long-press continues to work.

#### Scenario: Long-press opens edit
- **WHEN** the user long-presses the item row body
- **THEN** the item edit modal opens

#### Scenario: Row tap stays inert while long-press works
- **WHEN** the user taps the row body without holding
- **THEN** nothing happens, and a subsequent long-press still opens the edit modal
