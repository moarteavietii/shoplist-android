# quick-add Specification

## Purpose

Define item creation from the empty-search flow: subsequence fuzzy matching over the active view's items, an add affordance when a query matches nothing, and an item-creation modal with multi-view visibility selection, save/discard actions, and focus management.

## Requirements

### Requirement: Search filters the list with subsequence fuzzy matching
The search field SHALL filter the visible list using a subsequence fuzzy match: the query matches an item name when the query's characters appear in order (not necessarily contiguously), case-insensitively. Matching SHALL be Unicode-normalized (NFC) before comparison, so decomposed accents and special characters (e.g. `ß`) match their composed equivalents.

#### Scenario: Query matches in-order characters
- **WHEN** the list contains an item named "Milk" and the user types "mlk" in the search field
- **THEN** the "Milk" row remains visible

#### Scenario: Non-matching items are filtered out
- **WHEN** the user types a query that does not fuzzy-match an item
- **THEN** that item is not shown in the filtered list

#### Scenario: Matching is Unicode-normalized
- **WHEN** an item name contains a decomposed accent (e.g. "e" + combining acute) or a special character like `ß`
- **THEN** the query matches the name using its NFC-normalized form, case-insensitively

### Requirement: Add affordance appears on empty search result
When the current search query returns no matching items, the app SHALL show an add affordance next to the search field.

#### Scenario: Empty result shows the add button
- **WHEN** the user types a query that matches no items
- **THEN** an add button is rendered next to the search field

#### Scenario: Matching result hides the add button
- **WHEN** the search query matches at least one item
- **THEN** no add button is rendered

### Requirement: Item-creation modal opens from the add affordance
Activating the add affordance SHALL open an item-creation modal with the current search text pre-filled as the item title.

#### Scenario: Add button opens modal
- **WHEN** the user taps the add button after a query returned no results
- **THEN** a modal opens with the current search text pre-filled as the item title

### Requirement: Enter key triggers the add action from the search field
When the search field is focused and contains text, pressing Enter (including the IME action) SHALL trigger the same action as tapping the add button. With empty search text, Enter SHALL do nothing.

#### Scenario: Enter opens the item-creation modal
- **WHEN** the user types text in the search field and presses Enter
- **THEN** the item-creation modal opens with the current search text pre-filled as the item title

#### Scenario: Enter with empty search text does nothing
- **WHEN** the search field is focused but contains no text and the user presses Enter
- **THEN** no modal opens and no item is created

### Requirement: Modal allows multi-view selection
The item-creation modal SHALL let the user choose which views the item is shown on, defaulting to all available views selected. The modal SHALL provide a Select all / Deselect all toggle control for the checklist. Activating the toggle when not every view is selected SHALL select every view; activating it when every view is selected SHALL deselect every view except the default view (`Global`). The default view (`Global`) SHALL never be deselected by the toggle: it stays selected unless the user unchecks it manually.

#### Scenario: View selector defaults to all views
- **WHEN** the modal opens
- **THEN** every available view is selected in the checklist

#### Scenario: User can select multiple views
- **WHEN** the user checks additional views in the checklist
- **THEN** the selected views are reflected in the modal state without immediately creating the item

#### Scenario: User can uncheck any selected view
- **WHEN** the user unchecks a selected view in the checklist
- **THEN** that view is no longer selected and the other selections remain unchanged

#### Scenario: Deselect all keeps the default view selected
- **WHEN** every view is selected and the user activates the Deselect all toggle
- **THEN** every view except `Global` is deselected and `Global` remains selected

#### Scenario: Select all selects every view
- **WHEN** not every view is selected and the user activates the Select all toggle
- **THEN** every view, including any previously deselected ones, becomes selected

#### Scenario: Toggle only changes modal state
- **WHEN** the user activates the Select all / Deselect all toggle
- **THEN** the item is not created and the modal stays open

### Requirement: Modal lists all available views in a stable order
The item-creation modal SHALL list every view available to the current list, ordered with `Global` first and all other views in alphabetical order. The ordering SHALL come from the single shared shop-ordering helper used by both the modal and the shops repository.

#### Scenario: Views are ordered Global-first then alphabetically
- **WHEN** the modal opens and the current list has views named "Carrefour", "Auchan", and "Global"
- **THEN** the checklist shows "Global" first, followed by "Auchan" and then "Carrefour"

### Requirement: Modal supports save and discard
The item-creation modal SHALL provide Save and Discard actions. After a successful save, the app SHALL scroll the list so the newly added item is visible (the item is placed at the bottom of the list). Repeated Save activations while a create request is in flight SHALL be ignored, so no duplicate items are created.

#### Scenario: Save creates the item
- **WHEN** the user taps Save
- **THEN** the app creates the item with the entered title and selected views, closes the modal, and clears the search query

#### Scenario: List scrolls to the newly added item after save
- **WHEN** the user taps Save and the item is created
- **THEN** the list scrolls so the newly added item, rendered at the bottom of the list, is visible

#### Scenario: Discard closes without changes
- **WHEN** the user taps Discard
- **THEN** the modal closes without creating an item and the search query remains unchanged

#### Scenario: Save requires a non-empty title
- **WHEN** the user clears the title field and taps Save
- **THEN** the modal stays open and the item is not created

#### Scenario: Double-tap Save does not create duplicates
- **WHEN** the user activates Save while a create request from a previous activation is still in flight
- **THEN** the repeated activation is ignored and only one item is created

### Requirement: Modal persists selected views on Save
When the user saves the modal, the app SHALL create the item with a `visibleIn` array containing the identifiers of all selected views.

#### Scenario: Save with multiple views selected
- **WHEN** the user enters a title, selects "Global" and "Carrefour", and taps Save
- **THEN** the app calls `POST /api/lists/:listId/items` with the title and `visibleIn` set to the selected view ids

#### Scenario: Save with only Global selected
- **WHEN** the user enters a title and leaves only "Global" selected
- **THEN** the app calls `POST /api/lists/:listId/items` with `visibleIn` set to `[globalShopId]`

### Requirement: Focus returns to search field after item creation
After an item is created from the search flow, the app SHALL return focus to the search field so the next search or item creation can start immediately.

#### Scenario: Focus returns after saving the modal
- **WHEN** the user saves the item-creation modal and the item is created
- **THEN** the search field is focused and ready for text input

#### Scenario: Focus returns even when search query is cleared
- **WHEN** the user saves the item-creation modal and the search query is cleared as part of the save
- **THEN** the search field is still focused after the query is cleared
