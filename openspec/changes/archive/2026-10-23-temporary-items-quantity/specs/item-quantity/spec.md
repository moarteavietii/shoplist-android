## Purpose

Let users attach an amount to any item (e.g., 3 milks) and see it in the list without cluttering the default appearance: a compact stepper in the shared add/edit item modal and an `x<n>` suffix on list rows only when the quantity exceeds one.

## ADDED Requirements

### Requirement: The add/edit item modal exposes a quantity selector
The item editor modal (used for both adding and editing) SHALL render a quantity selector as a compact horizontal stepper — a decrement button, the current quantity, and an increment button — constrained to the range 1–99. The quantity SHALL NOT be directly editable as a number input.

#### Scenario: Modal opens with default quantity
- **WHEN** the user opens the add-item modal
- **THEN** the quantity selector shows 1

#### Scenario: Incrementing quantity
- **WHEN** the user taps the increment button while quantity is below 99
- **THEN** the displayed quantity increases by 1

#### Scenario: Decrement button is disabled at the minimum
- **WHEN** the quantity is 1
- **THEN** the decrement button is disabled and tapping it does not change the quantity

#### Scenario: Increment button is disabled at the maximum
- **WHEN** the quantity is 99
- **THEN** the increment button is disabled

#### Scenario: Quantity is not directly editable
- **WHEN** the user interacts with the quantity selector
- **THEN** the quantity can only be changed via the decrement and increment buttons (no editable number input)

### Requirement: Saving an item persists its quantity
When the user saves an item from the modal, the app SHALL send the selected quantity with the create or update request, and the saved item SHALL reflect that quantity.

#### Scenario: Creating an item with quantity greater than one
- **WHEN** the user sets the quantity to 3 and saves a new item
- **THEN** the created item has quantity 3

#### Scenario: Editing an item's quantity
- **WHEN** the user opens the edit modal for an item with quantity 1, sets it to 4, and saves
- **THEN** the item's quantity becomes 4

#### Scenario: Save button enables on quantity change
- **WHEN** the user, in edit mode, changes only the quantity
- **THEN** the save button becomes enabled

### Requirement: New items default to quantity one
Items created without an explicit quantity SHALL have quantity 1.

#### Scenario: Add-item modal default
- **WHEN** the user opens the add-item modal without changing the quantity
- **THEN** the created item has quantity 1

### Requirement: The list shows the quantity only when greater than one
The list SHALL render the item name alone when quantity is 1. When quantity is greater than 1, the app SHALL render an `x<quantity>` suffix after the name in a lighter text color than the name.

#### Scenario: Default quantity renders no suffix
- **WHEN** an item with quantity 1 is displayed in the list
- **THEN** only the item name is shown

#### Scenario: Quantity greater than one renders a suffix
- **WHEN** an item with quantity 3 is displayed
- **THEN** the row shows the item name followed by `x3` in a lighter text color

#### Scenario: Quantity updates are reflected in the list
- **WHEN** an item's quantity is changed via the edit modal
- **THEN** the row's suffix updates (appears, disappears, or changes value) without a reload
