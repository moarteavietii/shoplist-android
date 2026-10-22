## Purpose

Define how the shopping list screen renders and lays out items: chromeless rows in the custom design language, compact spacing, a fixed view-name watermark, hidden scrollbar, and bottom-anchored content that reserves space for the expanded search field and the virtual keyboard.

## ADDED Requirements

### Requirement: List items render without borders
The shopping list item rows SHALL render without visible borders, card outlines, or shadows. Row and background colors SHALL come from the design-token theme system.

#### Scenario: Viewing the list
- **WHEN** the shopping list is displayed
- **THEN** each item row appears as a continuous row without a border, outline, or shadow separator, colored via design tokens

### Requirement: List spacing is compact
The shopping list SHALL use reduced vertical spacing between item rows.

#### Scenario: Measuring row spacing
- **WHEN** the list contains multiple items
- **THEN** the vertical gap or padding between consecutive rows is compact, consistent with the web app's compact list design

### Requirement: Active view watermark is centered behind the list
The list viewport SHALL display the active view name as a large, centered, low-opacity watermark fixed behind the list items so it does not scroll with the content.

#### Scenario: Viewing a named view
- **WHEN** the user has selected a view with a name (e.g., "Auchan" or "Global")
- **THEN** that name is rendered centered in the list viewport background at 5%–10% opacity and remains stationary while the list scrolls

#### Scenario: Watermark stays fixed during scroll
- **WHEN** the user scrolls the list
- **THEN** the watermark does not move with the list items

### Requirement: Watermark updates with view changes
The watermark text SHALL update immediately when the active view changes.

#### Scenario: Switching views
- **WHEN** the user switches from one view to another
- **THEN** the centered watermark text changes to the new view name without any reload

### Requirement: Watermark layer does not block list interactions
The watermark layer SHALL be non-interactive and SHALL NOT block taps, drags, scrolls, or long-presses on list items.

#### Scenario: Watermark remains non-interactive
- **WHEN** the user taps, drags, scrolls, or long-presses anywhere on the list while the watermark layer is visible
- **THEN** the gesture passes through to the list content and the watermark layer never receives it

### Requirement: The list scrollbar is hidden
The list viewport SHALL NOT display a scrollbar.

#### Scenario: Scrolling a long list
- **WHEN** the user scrolls a list that overflows the viewport
- **THEN** no scrollbar indicator is visible

### Requirement: The shopping list content is anchored to the bottom of the viewport
The list content area SHALL be laid out so that list items are visually anchored near the bottom of the viewport and grow upward as items are added, while still supporting normal scrolling for long lists with the first item fully reachable.

#### Scenario: Few items sit at the bottom
- **WHEN** the list contains only a few items
- **THEN** the items are rendered at the bottom of the list viewport rather than at the top

#### Scenario: Many items scroll upward
- **WHEN** the list contains more items than fit in the viewport
- **THEN** the list scrolls normally, with the newest/lowest items anchored at the bottom and older items extending upward

#### Scenario: The topmost item is reachable on long lists
- **WHEN** the list contains more items than fit in the viewport
- **THEN** the user can scroll to the very top of the list and the first item is fully visible

### Requirement: Search expansion keeps items above the input
When the search input is expanded at the bottom of the viewport, the list SHALL reserve enough vertical space so all items remain fully visible above the search input and SHALL NOT scroll behind it.

#### Scenario: Few items sit above the expanded search input
- **WHEN** the list contains only a few items and the search input is expanded
- **THEN** the items are rendered fully above the search input with no overlap

#### Scenario: Scrolling the full list stops before the search input
- **WHEN** the list contains more items than fit in the viewport and the user scrolls to the bottom
- **THEN** the last item is fully visible above the expanded search input and does not disappear behind it

### Requirement: Virtual keyboard does not hide active items
When the user focuses the search input and the virtual keyboard appears, the list content SHALL shift upward so the bottom-anchored items stay visible above the keyboard and search input.

#### Scenario: Focusing search with items visible
- **WHEN** the user focuses the expanded search input and the virtual keyboard appears
- **THEN** the bottom of the list remains visible above the search input and keyboard

### Requirement: The app container fills the currently visible viewport
The app container SHALL be sized to the currently visible viewport height (accounting for system bars and keyboard insets) so the bottom-anchored list content and the expanded search bar remain within the visible fold. The layout SHALL NOT depend on page-level scrolling; only the internal list scroll container scrolls.

#### Scenario: Single item is visible
- **WHEN** the list contains a single item
- **THEN** the item is rendered within the visible viewport without requiring any scrolling

#### Scenario: No page-level scrolling is possible
- **WHEN** the app is loaded on any device
- **THEN** the app container itself does not scroll; only the internal list scroll container scrolls
