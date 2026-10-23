# list-screen Delta

## MODIFIED Requirements

### Requirement: Active view watermark is centered behind the list
The list viewport SHALL display the active view name as a large, centered, low-opacity watermark fixed behind the list items so it does not scroll with the content. The watermark SHALL be centered within the list viewport (not the full screen). The active view SHALL be user-selectable via the view drawer and SHALL default to the Global shop.

#### Scenario: Viewing a named view
- **WHEN** the user has selected a view with a name (e.g., "Auchan" or "Global")
- **THEN** that name is rendered centered in the list viewport background at 5%–10% opacity and remains stationary while the list scrolls

#### Scenario: Watermark stays fixed during scroll
- **WHEN** the user scrolls the list
- **THEN** the watermark does not move with the list items

#### Scenario: Watermark defaults to Global
- **WHEN** the app launches and the user has not selected a view
- **THEN** the watermark shows "Global" and the list behaves as the Global view

### Requirement: Watermark updates with view changes
The watermark text SHALL update immediately when the active view changes.

#### Scenario: Switching views
- **WHEN** the user switches from one view to another
- **THEN** the centered watermark text changes to the new view name without any reload

### Requirement: The list filters items by the active view
The list SHALL display only items whose `visibleIn` set contains the active view's shop id, sorted by that view's fractional order keys. When the active view changes, the list SHALL re-filter and re-sort immediately. The add-item overlay SHALL create items visible in the active view by default.

#### Scenario: Switching views re-filters the list
- **WHEN** the user switches the active view from "Global" to "Carrefour"
- **THEN** the list shows only items whose `visibleIn` contains the Carrefour shop id, ordered by the Carrefour order keys

#### Scenario: Item visible in multiple views appears in each
- **WHEN** an item's `visibleIn` contains both the Global and Carrefour shop ids
- **THEN** the item appears in both the Global view and the Carrefour view

#### Scenario: Item not visible in the active view is hidden
- **WHEN** an item's `visibleIn` does not contain the active view's shop id
- **THEN** the item is not rendered in the list while that view is active
