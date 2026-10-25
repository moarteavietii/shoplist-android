## MODIFIED Requirements

### Requirement: The drawer lists all available shops with unchecked-count badges

The view drawer SHALL present a "Shops" title, left-aligned, in a title-worthy style. Each shop SHALL render as a bordered card (1dp border token, 8dp corner radius, 12dp padding) with the shop name on the left and the unchecked count inside the card on the right: a small accent-deep pill when there are unchecked items, a faint "0" when the shop has items but none unchecked. The active shop's card SHALL be accent-filled with inverted content and an accent border. Non-Global shops SHALL render a separate bordered 36dp square edit button to the right of the card. Shop cards SHALL be spaced 8dp apart. The drawer SHALL end with a bare "+" add button (no border, no background). The drawer container SHALL render with a drop shadow consistent with the app's modal panels and a bottom margin so it does not touch the bottom of the screen. Tapping a shop card SHALL set it as the active shop and close the drawer; tapping the edit button SHALL open the shop edit modal without changing the active shop.

#### Scenario: Drawer title is left-aligned and prominent

- **WHEN** the drawer opens
- **THEN** the "Shops" title renders left-aligned in a title-sized style with spacing between it and the shop list

#### Scenario: Shop renders as a bordered card

- **WHEN** the drawer lists shops
- **THEN** each shop renders as a bordered card with the name on the left and the unchecked count inside the card on the right

#### Scenario: Active shop is accent-filled

- **WHEN** a shop is the active view
- **THEN** its card renders accent-filled with inverted content and an accent border

#### Scenario: Unchecked-count badge is compact

- **WHEN** a shop card renders with unchecked items
- **THEN** the count renders as a small accent-deep pill inside the card; a shop with items but none unchecked renders a faint "0"

#### Scenario: Drawer container has a drop shadow

- **WHEN** the drawer is open
- **THEN** the drawer container renders with a drop shadow consistent with the app's modal panels

#### Scenario: Edit button is a bordered square

- **WHEN** a non-Global shop row renders
- **THEN** a separate bordered 36dp square edit button renders to the right of the shop card

### Requirement: The drawer has an "Add shop" button at the bottom

The view drawer SHALL render a full-width bordered "+" add button below the shop list (1dp border token, 8dp corner radius, matching the other buttons). Tapping the button SHALL open the shop editor modal in create mode with an empty name input.

#### Scenario: Add shop button opens create modal

- **WHEN** the user taps the "+" button at the bottom of the open drawer
- **THEN** the shop editor modal opens in create mode with an empty name input

#### Scenario: Add shop button is bordered

- **WHEN** the drawer renders
- **THEN** the "+" button renders with a 1dp border token border and 8dp corner radius, centered
