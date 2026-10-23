## Purpose

Define how the user controls which shops an item appears in: the shop-visibility checklist in the item edit modal, its ordering and selection rules, save/discard semantics, and how visibility changes propagate to the list and to other devices via realtime sync.

## Requirements

### Requirement: The edit modal lists all available shops with Global first
The item edit modal SHALL list every shop returned by `GET /api/shops`, ordered with the Global shop first and all other shops in alphabetical order. If the Global shop is missing from the fetched shop list, the modal SHALL still show a Global option first.

#### Scenario: Global is first
- **WHEN** the modal opens and the shops are "Carrefour", "Auchan", and "Global"
- **THEN** "Global" is shown at the top of the list, followed by "Auchan" and then "Carrefour"

#### Scenario: No Global in fetched shops
- **WHEN** the modal opens and the Global shop is not in the fetched shop list
- **THEN** a "Global" option is still shown first in the modal

### Requirement: The checklist reflects the item's current visibility
When the edit modal opens for an existing item, the checklist SHALL reflect the item's current `visibleIn` set: a shop is checked when its id is in the set.

#### Scenario: Existing item visible in multiple shops
- **WHEN** the modal opens for an item with `visibleIn` set to `[globalShopId, carrefourId]`
- **THEN** "Global" and "Carrefour" are checked and all other shops are unchecked

### Requirement: Toggling a shop does not persist immediately
The user SHALL be able to check or uncheck any shop in the modal without immediately persisting changes; the backend is updated only on Save.

#### Scenario: Toggle a shop
- **WHEN** the user checks "Auchan"
- **THEN** "Auchan" appears checked and the backend is not yet updated

### Requirement: A Select all / Deselect all toggle manages the checklist
The modal SHALL provide a Select all / Deselect all toggle. Activating it when not every shop is selected SHALL select every shop; activating it when every shop is selected SHALL deselect every shop except Global. Global SHALL never be deselected by the toggle; it stays selected unless the user unchecks it manually.

#### Scenario: Toggle selects all shops
- **WHEN** not every shop is selected and the user activates the toggle
- **THEN** every shop becomes selected

#### Scenario: Toggle deselects all but Global
- **WHEN** every shop is selected and the user activates the toggle
- **THEN** every shop except Global becomes deselected and Global stays selected

### Requirement: Save is enabled only when changes exist
The modal's Save button SHALL be enabled only when the entered name, quantity, temporary flag, or the selected shops differ from the item's persisted values, and the name is not blank.

#### Scenario: No changes made
- **WHEN** the user opens the modal and does not change anything
- **THEN** the Save button is disabled

#### Scenario: Selection changed
- **WHEN** the user checks or unchecks any shop
- **THEN** the Save button becomes enabled

#### Scenario: Blank name disables Save
- **WHEN** the user clears the name input
- **THEN** the Save button is disabled even if shops have changed

### Requirement: Save persists visibility changes
Saving SHALL persist the selected shops together with any other changed fields by calling the item update API with `visibleIn` set to the selected shop ids; the request SHALL omit `visibleIn` when the selection is unchanged.

#### Scenario: Save updates visibility
- **WHEN** the user checks "Carrefour" and taps Save
- **THEN** the app calls `PATCH /api/items/:itemId` with `visibleIn` containing the Carrefour shop id

#### Scenario: Save omits unchanged visibility
- **WHEN** the user changes only the item name and taps Save
- **THEN** the update request contains the new name and no `visibleIn` field

#### Scenario: Save closes the modal
- **WHEN** the user taps Save and the update succeeds
- **THEN** the modal closes

#### Scenario: Save failure keeps the item unchanged
- **WHEN** the update fails after the user taps Save
- **THEN** the modal closes, an error is surfaced, and the item's persisted visibility is unchanged

### Requirement: Discard closes the modal without persisting
The modal SHALL offer a discard action (secondary button or back gesture) that closes the modal and reverts any pending selection changes without calling the backend.

#### Scenario: Discard after changes
- **WHEN** the user changes the selection and discards the modal
- **THEN** the modal closes and the item's visibility remains unchanged

### Requirement: Visibility changes propagate to the list and other devices
A saved visibility change SHALL immediately re-filter the active view's list, and SHALL reach other devices through the realtime `item:updated` event carrying the new `visibleIn` set.

#### Scenario: Item removed from active view
- **WHEN** the user saves the modal with the active view's shop unchecked
- **THEN** the item disappears from the active view's list immediately

#### Scenario: Remote visibility change applies locally
- **WHEN** another device saves the item with a changed `visibleIn` set
- **THEN** this device's list re-filters and the item appears or disappears accordingly

### Requirement: Quick-add from a shop view defaults visibility to that view
When the user creates an item from the empty-search flow while a shop view is active, the creation modal SHALL default the visibility checklist to every available view selected, and the saved item SHALL be visible in the selected views.

#### Scenario: Created item appears in selected views
- **WHEN** the user creates an item from the quick-add modal with "Carrefour" selected
- **THEN** the item is created with `visibleIn` containing the Carrefour shop id and appears in the Carrefour view
