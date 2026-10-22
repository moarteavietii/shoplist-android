# list-screen Delta

## MODIFIED Requirements

### Requirement: Search is provided by the widget stack's expandable input
The list screen SHALL NOT render a persistent bottom search field; search SHALL be provided by the widget stack's Search control expanding to a full-width bottom-anchored input. The list SHALL reserve enough vertical space so all items remain fully visible above the expanded input and keyboard, and SHALL NOT scroll behind them.

#### Scenario: Bottom field is replaced by the expandable input
- **WHEN** the shopping list renders
- **THEN** no persistent search field is visible; expanding search from the stack shows the bottom-anchored input

#### Scenario: Items stay above the expanded input
- **WHEN** the search input is expanded
- **THEN** all items remain fully visible above it and the keyboard, with no content scrolling behind

### Requirement: The list derivation applies hide-checked with search override
The list derivation SHALL exclude checked items while the hide toggle is disabled, except while a search query is active: checked items matching the query SHALL stay visible, and hiding SHALL resume when the query is cleared. During a drag, all items SHALL be revealed.

#### Scenario: Hiding excludes checked items
- **WHEN** the toggle is disabled and no query is active
- **THEN** checked items are excluded from the rendered list, unchecked items keep their order

#### Scenario: Search override keeps checked matches visible
- **WHEN** the toggle is disabled and a query matches a checked item
- **THEN** the item stays visible until the query is cleared
