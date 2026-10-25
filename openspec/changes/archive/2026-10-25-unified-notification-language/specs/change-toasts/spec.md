## MODIFIED Requirements

### Requirement: Remote item changes produce change toasts
When an item change arrives via realtime sync from a device other than this one, the app SHALL display a toast describing the change. The following changes SHALL produce toasts with these labels:

- item checked → `checked: <name>`
- item unchecked → `unchecked: <name>`
- item created → `new item: <name>`
- item deleted → `deleted: <name>`
- item renamed → `<old name> → <new name>`
- item quantity changed → `quantity: <old> → <new> <name>`
- item no longer visible in a shop → `<name> removed from <shop name>`
- item newly visible in a shop → `<name> added to <shop name>`

Reorder changes SHALL NOT produce toasts. An update that changes several fields at once SHALL produce one toast per distinct change kind, queued and displayed one at a time in the order the change kinds were derived, each replacing the previous one. Duplicate or no-op change events (an update whose applied state matches the cached item) SHALL NOT produce toasts.

Toasts SHALL render in the unified notification style defined by the `notification-surface` capability: a top-center inverse pill with a severity LED, soft shadow, and single-line text. Change toasts SHALL use the standard (informational) severity LED.

#### Scenario: Remote check-off shows toast
- **WHEN** another device checks an item named "Bread"
- **THEN** this device shows a toast reading "checked: Bread" in the unified notification pill style

#### Scenario: Remote uncheck shows toast
- **WHEN** another device unchecks an item named "onion"
- **THEN** this device shows a toast reading "unchecked: onion"

#### Scenario: Remote creation shows toast
- **WHEN** another device creates an item named "oranges"
- **THEN** this device shows a toast reading "new item: oranges"

#### Scenario: Remote delete shows toast with pre-delete name
- **WHEN** another device deletes an item named "Bread"
- **THEN** this device shows a toast reading "deleted: Bread"

#### Scenario: Remote rename shows transition label
- **WHEN** another device renames an item from "Bread" to "Bread rolls"
- **THEN** this device shows a toast reading "Bread → Bread rolls"
