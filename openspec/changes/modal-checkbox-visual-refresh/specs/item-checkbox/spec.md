## MODIFIED Requirements

### Requirement: Item rows render a themed circle checkbox

Each shopping list item row SHALL render a bordered rounded-square checkbox (LED square) instead of a filled circle. When unchecked, the checkbox SHALL render as a hollow rounded-square: a visible border in the theme's border color with no fill. When checked, the checkbox SHALL render filled in the theme's LED-on color with the border visible and a checkmark drawn inside. Checkbox colors SHALL come from the design-token theme system, and no new theme tokens SHALL be introduced. The item name text SHALL render identically whether the item is checked or unchecked (same color, no dimming), so both states read equally while preparing the list.

#### Scenario: Unchecked checkbox appearance

- **WHEN** an item is unchecked
- **THEN** its row renders a hollow rounded-square with a visible border and no checkmark

#### Scenario: Checked checkbox appearance

- **WHEN** an item is checked
- **THEN** its row renders a rounded-square filled in the theme's LED-on color with a checkmark inside and the border visible

#### Scenario: Checked item name is not dimmed

- **WHEN** an item is checked
- **THEN** its name text renders with the same color and opacity as an unchecked item's name

#### Scenario: Checkbox follows the active theme

- **WHEN** the user switches to a different theme
- **THEN** the checkbox colors update to the new theme's LED-on and border colors

### Requirement: Checking animates with a color crossfade and checkmark draw-in

When the checkbox state changes, the fill SHALL crossfade between hollow (no fill) and the LED-on color and the checkmark SHALL draw in via a stroke animation, both over the app's standard short transition duration. The border color SHALL transition between the theme's border color (unchecked) and the LED-on color (checked). The animation SHALL run in both directions (check and uncheck).

#### Scenario: Check animates in

- **WHEN** the user checks an item via the checkbox
- **THEN** the fill crossfades to the LED-on color, the border transitions to the LED-on color, and the checkmark strokes in, completing together

#### Scenario: Uncheck animates out

- **WHEN** the user unchecks an item via the checkbox
- **THEN** the fill fades back to hollow, the border transitions back to the theme's border color, and the checkmark strokes out
