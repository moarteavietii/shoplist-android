# theme-system Specification

## Purpose

Provide the app's theme catalog and application rules: a curated set of color themes ported from the web frontend, persisted device-locally, applied app-wide (including the system status bar), with contrast validated to WCAG AA.

## Requirements

### Requirement: The theme catalog contains exactly nine themes
The app SHALL offer exactly these themes, in this order: Default, Pastel, Mint, Lavender, Dark Slate, Ocean, Sunset, High Contrast, Christmas. Each theme SHALL define a full color palette (background, surface, content, content-muted, accent, danger, LED on/off) and a chrome color. No other themes SHALL be selectable.

#### Scenario: Catalog matches the curated set
- **WHEN** the theme picker lists available themes
- **THEN** exactly the nine curated themes appear in the defined order, each with its name and a color swatch

#### Scenario: Dropped web themes are absent
- **WHEN** the theme picker is displayed
- **THEN** Cream, Rose, Sakura, Forest, Nord, and Mocha do not appear

### Requirement: Theme palettes are ported from the web app

Each theme's palette SHALL match the corresponding `[data-theme]` block in the web app's `index.css` (Default from `:root`), mapped onto the app's design tokens. The Christmas theme SHALL be a light theme with a classic red/green festive palette (red accent, green secondary, warm white surfaces) designed to the same token structure and contrast rules as the ported themes. The shop watermark SHALL be rendered from the theme's content color at web-app scale (48sp or larger, bold weight, opacity 0.05 or lower) rather than from per-theme palette values.

#### Scenario: A ported theme matches its web palette

- **WHEN** a ported theme (e.g., Ocean) is active
- **THEN** the app's background, surface, text, accent, and danger colors equal the hex values defined in the web app's `[data-theme="ocean"]` block

#### Scenario: Christmas theme is light and festive

- **WHEN** the Christmas theme is active
- **THEN** the app renders a light background with red accent and green supporting colors

#### Scenario: Watermark uses theme content color at web scale

- **WHEN** any theme is active
- **THEN** the shop watermark renders in the theme's content color at 48sp or larger with opacity 0.05 or lower

### Requirement: The selected theme persists across restarts
The app SHALL persist the selected theme device-locally and restore it on launch. The default theme SHALL be Default. An unknown or missing stored value SHALL fall back to Default.

#### Scenario: Theme survives app restart
- **WHEN** the user selects Ocean, kills the app, and relaunches it
- **THEN** the app starts with the Ocean theme active

#### Scenario: Corrupted stored value falls back to Default
- **WHEN** the stored theme value is not a valid theme id
- **THEN** the app starts with the Default theme

### Requirement: The active theme applies app-wide
The active theme's palette SHALL drive every screen's design tokens, and the system status bar SHALL be tinted with the theme's chrome color while the app is foregrounded. Theme changes SHALL apply immediately without an app restart.

#### Scenario: Switching themes re-colors the app instantly
- **WHEN** the user switches from Dark Slate to Pastel
- **THEN** all visible screens re-render with the Pastel palette immediately, without restart or reload

#### Scenario: Status bar follows the theme
- **WHEN** a theme is active and the app is foregrounded
- **THEN** the status bar background is the theme's chrome color

### Requirement: Theme palettes meet WCAG AA contrast
Every theme SHALL satisfy WCAG AA contrast: at least 4.5:1 between content and surface colors, and at least 3:1 between accent and surface colors. This SHALL be enforced by unit tests covering all nine themes.

#### Scenario: Contrast tests pass for all themes
- **WHEN** the contrast unit tests run
- **THEN** every theme's content/surface pair is at least 4.5:1 and every accent/surface pair is at least 3:1

### Requirement: Text styling comes from the typography token scale
All text in the app SHALL be styled from the `ShoplistTypography` token scale: display (56sp), title (20sp), body (16sp), label (12sp), ledDot (12sp Monospace), micro (10sp Bold), chip (13sp SemiBold), stepperKey (15sp Bold), and stepperValue (17sp Bold). Components SHALL NOT introduce ad-hoc `fontSize` values outside this scale. Component-level refinements SHALL be limited to non-size attributes (weight, family, color) unless a new scale entry is added through a spec change.

#### Scenario: Token scale covers all rendered text
- **WHEN** any screen renders text
- **THEN** the text style derives from one of the nine scale tokens, with no component-level `fontSize` override

#### Scenario: Micro and chip tokens match the ported modal visuals
- **WHEN** the Add Item modal, View Drawer badges, or Quick Menu badge render small text
- **THEN** the sizes equal the previously ad-hoc values (10sp for badges/counts, 13sp for chip labels), so the visuals are unchanged from the web port

#### Scenario: Stepper text matches the ported modal visuals
- **WHEN** the quantity stepper renders its keys and value
- **THEN** the sizes equal the previously ad-hoc values (15sp keys, 17sp value), so the visuals are unchanged from the web port
