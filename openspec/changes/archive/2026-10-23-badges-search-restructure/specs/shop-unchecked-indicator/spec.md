# shop-unchecked-indicator Delta

## Purpose

Provide quiet at-a-glance awareness of unchecked items via count badges in the view drawer and on the View Switcher control.

## ADDED Requirements

### Requirement: Drawer shop rows show trailing unchecked-count badges
Each view drawer shop row SHALL render a trailing accent pill badge with the count of unchecked items visible in that shop (an item is visible in a shop when the shop's id is in the item's `visibleIn` set). Fully-checked or empty shops SHALL show a muted, non-accent "0" with no pill. The Global shop SHALL count all items. The label SHALL sit left and the badge right.

#### Scenario: Shop with unchecked items shows its count
- **WHEN** the drawer renders and "Carrefour" has 3 unchecked items in its `visibleIn` set
- **THEN** the Carrefour row shows an accent pill badge reading "3"

#### Scenario: Fully checked shop shows muted zero
- **WHEN** every item visible in "Carrefour" is checked
- **THEN** the Carrefour row shows a muted non-accent "0" with no pill

#### Scenario: Global counts all items
- **WHEN** the Global row renders
- **THEN** its badge counts all unchecked items (Global is visible in every item's context)

### Requirement: Counts update live
Per-shop counts SHALL update immediately on check/uncheck and via realtime sync from other devices, without any refresh.

#### Scenario: Check updates the badge
- **WHEN** the user checks the last unchecked item visible in "Carrefour"
- **THEN** the Carrefour badge immediately shows the muted "0"

#### Scenario: Remote event updates the badge
- **WHEN** another device creates an unchecked item visible in "Auchan"
- **THEN** the Auchan badge updates as the event arrives

### Requirement: The View Switcher badge shows the Global unchecked count
The widget stack's View Switcher control SHALL render an accent pill badge with the count of unchecked items in the Global view (multi-shop items counted once — not the sum of per-shop counts). The badge SHALL be hidden at 0 or when the list is empty, SHALL sit fully inside the top-right corner with a surface ring, and SHALL be purely visual: small, excluded from accessibility semantics, never blocking taps or drags, and never changing the control's accessible name.

#### Scenario: Badge shows Global unchecked count
- **WHEN** the Global view has 5 unchecked items
- **THEN** the View Switcher control shows an accent pill badge reading "5"

#### Scenario: Badge hidden at zero
- **WHEN** no unchecked items exist
- **THEN** the View Switcher badge is hidden

#### Scenario: Multi-shop items counted once
- **WHEN** an unchecked item is visible in both Global and Carrefour
- **THEN** the Global badge counts it once

#### Scenario: Badge never blocks gestures
- **WHEN** the user taps or drags over the badge area
- **THEN** the gesture reaches the View Switcher control and the badge is not announced by accessibility services

### Requirement: Badges update live everywhere
All badges (drawer rows and View Switcher) SHALL derive from the same reactive state, so a check, undo, or remote event updates every surface at once without a reload.

#### Scenario: One event updates all surfaces
- **WHEN** the user checks an item visible in multiple shops
- **THEN** the drawer row badges and the View Switcher badge all update immediately
