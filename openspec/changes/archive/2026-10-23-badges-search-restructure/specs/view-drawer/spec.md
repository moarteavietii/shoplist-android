# view-drawer Delta

## MODIFIED Requirements

### Requirement: The drawer lists all available shops with unchecked-count badges
The view drawer SHALL list every shop returned by `GET /api/shops`, ordered with the Global shop first and all other shops in alphabetical order. Each shop row SHALL render a trailing count badge: an accent pill with the count of unchecked items visible in that shop (the shop's id is in the item's `visibleIn` set), or a muted non-accent "0" with no pill when the shop's visible items are all checked or the shop has no items. The Global shop SHALL count all items. This replaces the previous hollow-circle indicator.

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
