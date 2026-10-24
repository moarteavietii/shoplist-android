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
The view drawer SHALL list every shop returned by `GET /api/v1/shops`, ordered with the Global shop first and all other shops in alphabetical order. Each shop row SHALL render a trailing count badge: an accent pill with the count of unchecked items visible in that shop (the shop's id is in the item's `visibleIn` set), or a muted non-accent "0" with no pill when the shop's visible items are all checked or the shop has no items. The Global shop SHALL count all items. This replaces the previous hollow-circle indicator.

#### Scenario: Drawer shows all shops with counts
- **WHEN** the drawer opens and the backend returns shops named "Carrefour", "Auchan", and "Global"
- **THEN** the drawer shows "Global", "Auchan", and "Carrefour" in that order, each with its trailing unchecked count

#### Scenario: Global is included even if absent from fetched shops
- **WHEN** the drawer opens and the backend does not return a Global shop
- **THEN** "Global" is still shown as the first option and is treated as a placeholder until the real Global shop loads

#### Scenario: Shop with unchecked items shows its count
- **WHEN** the drawer renders and "Carrefour" has 3 unchecked items in its `visibleIn` set
- **THEN** the Carrefour row shows an accent pill badge reading "3"

#### Scenario: Fully checked shop shows muted zero
- **WHEN** every item visible in "Carrefour" has `isChecked` true
- **THEN** the "Carrefour" row shows a muted non-accent "0" with no pill

#### Scenario: Empty shop shows muted zero
- **WHEN** no item includes "Lidl" in its `visibleIn` set
- **THEN** the "Lidl" row shows a muted non-accent "0" with no pill

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
The view drawer SHALL render a full-width "Add shop" button below the shop list. Tapping the button SHALL open the shop editor modal in create mode with an empty name input.

#### Scenario: Add shop button opens create modal
- **WHEN** the user taps the "Add shop" button at the bottom of the open drawer
- **THEN** the shop editor modal opens in create mode with an empty name input

#### Scenario: Add shop button is accessible
- **WHEN** the drawer renders
- **THEN** the "Add shop" button has an accessible name such as "Add shop"

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
