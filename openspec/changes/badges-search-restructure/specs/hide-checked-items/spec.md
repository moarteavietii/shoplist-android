# hide-checked-items Delta

## Purpose

Let users hide checked items from the list via a widget-stack toggle, with smooth animations and a search-override rule.

## ADDED Requirements

### Requirement: The Checked-Items Toggle controls checked-item visibility
The widget stack's Checked-Items Toggle SHALL control whether checked items appear in the list. It SHALL be enabled (showing) by default, render an LED dot with dimmed/active styling, and expose its pressed state to accessibility services. The state SHALL persist across sessions.

#### Scenario: Toggle hides checked items
- **WHEN** the user disables the toggle
- **THEN** checked items are excluded from the view but kept in data, and unchecked items keep their order

#### Scenario: Toggle state survives restart
- **WHEN** the user disables the toggle and restarts the app
- **THEN** checked items remain hidden

#### Scenario: Pressed state is accessible
- **WHEN** an accessibility service reads the toggle
- **THEN** it exposes its pressed (showing/hiding) state

### Requirement: Hiding animates and check-off commits instantly
When checked items are hidden, newly checked items SHALL animate out (height to zero + fade); re-enabling the toggle SHALL animate them back in (grow + fade) in their original order. Check/uncheck SHALL commit instantly regardless of the toggle, and the undo bar SHALL remain available to restore an item that animated out.

#### Scenario: Checking animates the item out
- **WHEN** the user checks an item while hiding is active
- **THEN** the item animates out (shrink + fade) and the check commits instantly

#### Scenario: Undo restores a hidden item
- **WHEN** the user undoes a check-off for an item that animated out
- **THEN** the item is restored and animates back in

### Requirement: Search overrides the hide toggle
While a search query is active, checked items matching the query SHALL stay visible regardless of the toggle; hiding SHALL resume when the query is cleared. Unchecking a found item SHALL keep it visible until the query is cleared.

#### Scenario: Checked matches stay visible during search
- **WHEN** a query is active and a checked item matches it
- **THEN** the item stays visible even while the toggle is disabled

#### Scenario: Hiding resumes when the query clears
- **WHEN** the query is cleared
- **THEN** checked items are hidden again (toggle still disabled)

### Requirement: Drag reveals hidden items
While a drag is in progress, all items in the active view — including hidden checked ones — SHALL be revealed in sorted position so drop targets reflect the full order; hidden state SHALL restore on drag end. (Depends on the drag-and-drop batch.)

#### Scenario: Drag reveals hidden checked items
- **WHEN** a drag starts while checked items are hidden
- **THEN** all items appear instantly in sorted position, and hidden state is restored on drag end
