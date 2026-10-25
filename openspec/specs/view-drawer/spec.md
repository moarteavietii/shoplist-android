# view-drawer Specification

## Purpose

Let users switch the active shopping context through a left-aligned slide-out drawer that displays shops fetched from the backend.

## Requirements

### Requirement: The view switcher is triggered from the widget stack
The shopping list UI SHALL render a view-switcher icon in the widget stack that toggles the view drawer open and closed. The icon SHALL appear in an inactive/gray state when the drawer is closed and in an active state when the drawer is open.

#### Scenario: View switcher icon is visible
- **WHEN** the shopping list renders
- **THEN** the view-switcher icon appears in the widget stack

#### Scenario: Tapping the view switcher icon opens the drawer
- **WHEN** the user taps the view-switcher icon and the drawer is closed
- **THEN** a drawer slides in from the left side of the screen

#### Scenario: Tapping the view switcher icon closes the drawer
- **WHEN** the user taps the view-switcher icon and the drawer is open
- **THEN** the drawer closes

#### Scenario: View switcher icon is gray when drawer is closed
- **WHEN** the drawer is closed
- **THEN** the view-switcher icon in the widget stack is rendered in an inactive/gray color

#### Scenario: View switcher icon is active when drawer is open
- **WHEN** the drawer is open
- **THEN** the view-switcher icon in the widget stack is rendered in an active color

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

### Requirement: Selecting a shop changes the active shop and closes the drawer
The view drawer SHALL keep shop selection and management gestures distinct. Tapping the shop name or row body SHALL set it as the active shop and close the drawer. Tapping the row's edit icon SHALL open the shop edit modal without changing the active shop or closing the drawer.

#### Scenario: Selecting a shop updates the active shop
- **WHEN** the user taps "Carrefour" in the drawer
- **THEN** the active shop becomes the Carrefour shop's numeric ID and the list re-sorts using that shop's fractional index

#### Scenario: Selecting a shop closes the drawer
- **WHEN** the user taps any shop in the drawer
- **THEN** the drawer slides out and is no longer visible

#### Scenario: Tapping edit icon does not change active shop
- **WHEN** the user taps the edit icon on the "Carrefour" row
- **THEN** the active shop remains unchanged and the drawer stays open

### Requirement: The active shop is highlighted in the drawer
The drawer SHALL continue to visually indicate which shop is currently active. The edit icon SHALL remain clearly visible and tappable on non-active rows.

#### Scenario: Active shop is highlighted
- **WHEN** the drawer opens and the active shop is the Auchan shop
- **THEN** "Auchan" is rendered with a distinct active state

#### Scenario: Edit icon is visible on active and inactive rows
- **WHEN** the drawer renders with multiple shops
- **THEN** every non-Global row shows its edit icon regardless of active state

### Requirement: Tapping outside the drawer closes it
Tapping on the backdrop outside the drawer SHALL close the drawer without changing the active shop.

#### Scenario: Backdrop tap closes drawer
- **WHEN** the drawer is open and the user taps the shaded area outside the drawer
- **THEN** the drawer closes and the active shop remains unchanged

### Requirement: The drawer slides in and out smoothly
Opening and closing the drawer SHALL be animated so it slides horizontally from the left edge.

#### Scenario: Drawer opens with animation
- **WHEN** the user opens the view drawer
- **THEN** the drawer animates in from the left over a short duration

#### Scenario: Drawer closes with animation
- **WHEN** the user closes the view drawer
- **THEN** the drawer animates out to the left over a short duration

### Requirement: The view drawer opens with a left-to-right edge swipe
The view drawer SHALL open when the user performs a left-to-right swipe gesture starting from the left edge of the screen while the drawer is closed.

#### Scenario: Swiping right from the edge opens the drawer
- **WHEN** the user swipes from the left edge of the screen to the right beyond a small threshold while the drawer is closed
- **THEN** the drawer slides in from the left

#### Scenario: Small rightward movements do not open the drawer
- **WHEN** the user moves their finger only slightly rightward from the left edge
- **THEN** the drawer remains closed

#### Scenario: Rightward swipe on the open drawer does not re-open it
- **WHEN** the drawer is already open and the user swipes rightward
- **THEN** the drawer remains open and does not flicker or restart its open animation

#### Scenario: Left-to-right swipe does not conflict with list item interactions
- **WHEN** the user begins a left-to-right swipe over a list item
- **THEN** the gesture either opens the drawer if it starts at the screen edge or is handled by the list/drag layer if it starts elsewhere

### Requirement: The view drawer is closable with a right-to-left swipe
The open view drawer SHALL close when the user performs a right-to-left swipe gesture across the drawer panel or its handle.

#### Scenario: Swiping left closes the drawer
- **WHEN** the user swipes the open drawer from right to left beyond a small threshold
- **THEN** the drawer closes

#### Scenario: Small leftward movements do not close the drawer
- **WHEN** the user moves their finger only slightly leftward on the drawer
- **THEN** the drawer remains open

### Requirement: The drawer closes with an upward swipe from the bottom
The view drawer SHALL close when the user performs an upward swipe gesture starting from the bottom of the screen.

#### Scenario: Swiping up from the bottom closes the drawer
- **WHEN** the user swipes upward from the bottom edge of the screen beyond a small threshold while the drawer is open
- **THEN** the drawer closes

#### Scenario: Small upward movements from the bottom do not close the drawer
- **WHEN** the user moves their finger only slightly upward from the bottom edge
- **THEN** the drawer remains in its current state

### Requirement: The drawer has an "Add shop" button at the bottom

The view drawer SHALL render a full-width bordered "+" add button below the shop list (1dp border token, 8dp corner radius, matching the other buttons). Tapping the button SHALL open the shop editor modal in create mode with an empty name input.

#### Scenario: Add shop button opens create modal

- **WHEN** the user taps the "+" button at the bottom of the open drawer
- **THEN** the shop editor modal opens in create mode with an empty name input

#### Scenario: Add shop button is bordered

- **WHEN** the drawer renders
- **THEN** the "+" button renders with a 1dp border token border and 8dp corner radius, centered

### Requirement: Each non-Global shop row has an edit action in the drawer
Each non-Global shop row in the view drawer SHALL render an edit (pen) icon on the right side. Tapping the icon SHALL open the shop editor modal in edit mode for that shop. The Global shop SHALL not display an edit icon.

#### Scenario: Edit icon opens edit modal
- **WHEN** the user taps the edit icon on the "Carrefour" row in the drawer
- **THEN** the shop editor modal opens in edit mode with "Carrefour" in the name input

#### Scenario: Global shop has no edit icon in the drawer
- **WHEN** the drawer renders
- **THEN** the "Global" row does not have an edit button

#### Scenario: Edit icon is accessible
- **WHEN** a drawer shop row renders with an edit icon
- **THEN** the edit button has an accessible name such as "Edit Carrefour"

### Requirement: Drawer shop mutations update the drawer list immediately
After a successful create, rename, or delete from a drawer-triggered shop editor modal, the drawer shop list SHALL reflect the change immediately without requiring a manual refresh.

#### Scenario: New shop appears in drawer after creation
- **WHEN** the user saves a new shop named "Lidl" from the drawer's add button
- **THEN** "Lidl" appears in the drawer list in the correct order (Global first, then alphabetical)

#### Scenario: Renamed shop shows new name in drawer
- **WHEN** the user renames "Carrefour" to "Carrefour Express" from the drawer's edit icon
- **THEN** the drawer row shows "Carrefour Express" in alphabetical position

#### Scenario: Deleted shop is removed from drawer
- **WHEN** the user deletes "Carrefour" from the edit modal opened via the drawer
- **THEN** "Carrefour" is removed from the drawer list and both modals close
