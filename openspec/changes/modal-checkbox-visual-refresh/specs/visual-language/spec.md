## Purpose

Define the app's shared visual language: the rules that give every panel, control, and background element a consistent physical feel, so new UI elements can be designed by applying the same rules rather than ad-hoc choices.

## ADDED Requirements

### Requirement: Panels cast shadows

Elevated panels — modals, menus, floating containers — SHALL render with a drop shadow over the screen content, so they read as physically raised above the background. The shadow treatment SHALL be consistent across panels: the QuickMenu and the item/shop modals SHALL use the same elevation scale. Panel backgrounds SHALL use the theme's surface color.

#### Scenario: Modal has a drop shadow

- **WHEN** the add-item, edit-item, or shop modal is open
- **THEN** the modal panel renders with a drop shadow visible against the dimmed background

#### Scenario: Shadow treatment matches the QuickMenu

- **WHEN** the QuickMenu and a modal are compared
- **THEN** both use the same elevation scale for their drop shadows

### Requirement: Controls have edges

Interactive controls — buttons, checkbox containers, chips — SHALL render with a visible boundary (border or fill) that marks them as pressable, rather than as bare text. Disabled controls SHALL remain identifiable (reduced opacity) while keeping their boundary.

#### Scenario: Action button has a visible boundary

- **WHEN** a modal action button (Save, Delete, Discard/Close) is rendered
- **THEN** it renders with a visible border or fill and a minimum height of 44dp

#### Scenario: Disabled button keeps its boundary

- **WHEN** an action button is disabled
- **THEN** it renders at reduced opacity but its boundary remains visible

### Requirement: State is expressed as fill versus outline

Binary state controls (checkboxes) SHALL distinguish their two states by fill versus outline: the off state SHALL be hollow (outline only, no fill), and the on state SHALL be filled in the theme's LED-on color. The control's border SHALL be visible in both states. Off-state visuals SHALL NOT use a filled neutral that could read as "on".

#### Scenario: Unchecked checkbox is hollow

- **WHEN** a checkbox is unchecked
- **THEN** it renders as a bordered rounded-square with no fill inside the border

#### Scenario: Checked checkbox is filled

- **WHEN** a checkbox is checked
- **THEN** it renders with the theme's LED-on color filling the rounded-square and the border visible

### Requirement: Background chrome recedes

Decorative background elements — the shop watermark — SHALL be large but nearly invisible: sized prominently (48sp or larger) yet rendered at very low opacity (0.05 or lower) so they never compete with content. Watermark text SHALL use the theme's content color.

#### Scenario: Watermark is large and faint

- **WHEN** the list screen shows the shop watermark
- **THEN** the text renders at 48sp or larger with opacity 0.05 or lower

#### Scenario: Watermark does not intercept touches

- **WHEN** the user taps where the watermark is displayed
- **THEN** the touch passes through to the list content beneath
