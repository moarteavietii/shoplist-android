## MODIFIED Requirements

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
