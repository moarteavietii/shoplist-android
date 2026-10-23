# list-screen Specification

## Purpose

Define how the shopping list screen renders and lays out items: chromeless rows in the custom design language, compact spacing, a fixed view-name watermark, hidden scrollbar, and bottom-anchored content that reserves space for the expanded search field and the virtual keyboard.

## Requirements

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

### Requirement: The list derivation applies hide-checked with manual-sort and search overrides
The list derivation SHALL exclude checked items while the hide toggle is disabled, except while a search query is active: checked items matching the query SHALL stay visible, and hiding SHALL resume when the query is cleared. While manual sort is engaged (sort mode ON and Manual order), all items SHALL always be visible regardless of the toggle, so a drag start never reflows the list.

#### Scenario: Hiding excludes checked items
- **WHEN** the toggle is disabled and no query is active
- **THEN** checked items are excluded from the rendered list, unchecked items keep their order

#### Scenario: Search override keeps checked matches visible
- **WHEN** the toggle is disabled and a query matches a checked item
- **THEN** the item stays visible until the query is cleared

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

### Requirement: Search is provided by the widget stack's expandable input
The list screen SHALL NOT render a persistent bottom search field; search SHALL be provided by the widget stack's Search control expanding to a full-width bottom-anchored input. The list SHALL reserve enough vertical space so all items remain fully visible above the expanded input and keyboard, and SHALL NOT scroll behind them.

#### Scenario: Bottom field is replaced by the expandable input
- **WHEN** the shopping list renders
- **THEN** no persistent search field is visible; expanding search from the stack shows the bottom-anchored input

#### Scenario: Items stay above the expanded input
- **WHEN** the search input is expanded
- **THEN** all items remain fully visible above it and the keyboard, with no content scrolling behind

### Requirement: Virtual keyboard does not hide active items
When the user focuses the search input and the virtual keyboard appears, the list content SHALL shift upward so the bottom-anchored items stay visible above the keyboard and search input. Inset handling SHALL NOT double-count overlapping system bar and keyboard insets.

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

### Requirement: The screen triggers the initial data load
The list screen SHALL trigger the initial data load on first composition, independent of the realtime connect signal, so a not-yet-loaded state is distinguishable from a genuinely empty list.

#### Scenario: First composition loads data
- **WHEN** the list screen is composed for the first time
- **THEN** the app initiates the items fetch without waiting for a realtime connect signal

### Requirement: Checkbox rows expose checked state to accessibility
Item checkbox rows SHALL expose their checked state (and row label) to accessibility services, so screen readers announce the current checked state.

#### Scenario: Screen reader announces checked state
- **WHEN** an accessibility service reads a list row
- **THEN** the row exposes its checked state and the screen reader announces whether the item is checked

### Requirement: Check-off action affordances meet minimum touch targets
Check-off related action controls (undo bar actions, select-all toggles, save/discard) SHALL provide touch targets of at least 48dp in both dimensions.

#### Scenario: Action controls are at least 48dp
- **WHEN** an action control (e.g. Save, Discard, Select all, undo bar action) is rendered
- **THEN** its touch target is at least 48dp in height and width

### Requirement: The add-item overlay has modal semantics
The add-item overlay SHALL render with a scrim over the background content, SHALL dismiss on outside-tap and on the system back gesture, and SHALL NOT leave the background interactive while open.

#### Scenario: Outside-tap dismisses the overlay
- **WHEN** the user taps outside the add-item overlay while it is open
- **THEN** the overlay dismisses without creating an item

#### Scenario: Back gesture dismisses the overlay
- **WHEN** the user activates the system back gesture while the add-item overlay is open
- **THEN** the overlay dismisses without creating an item

#### Scenario: Background is not interactive while the overlay is open
- **WHEN** the add-item overlay is open
- **THEN** taps on the background content hit the scrim and do not reach the list

### Requirement: The add-item overlay animates its exit
When the add-item overlay is dismissed, the app SHALL compose the overlay content during the exit animation (fade/slide-out) rather than removing it abruptly.

#### Scenario: Overlay fades out on dismiss
- **WHEN** the add-item overlay is dismissed
- **THEN** the overlay content remains composed and visible during the exit animation before being removed

### Requirement: The view switcher is triggered from the widget stack
The shopping list UI SHALL render the view-switcher trigger inside the top-right widget stack (View Switcher control) instead of a list-area icon. The trigger SHALL toggle the view drawer open and closed and SHALL appear in an inactive/gray state when the drawer is closed and in an active state when the drawer is open.

#### Scenario: View switcher icon is visible
- **WHEN** the shopping list renders
- **THEN** the view-switcher trigger appears inside the widget stack, not as a floating list-area icon

#### Scenario: Tapping the view switcher opens the drawer
- **WHEN** the user taps the view-switcher trigger and the drawer is closed
- **THEN** a drawer slides in from the left side of the screen

#### Scenario: Tapping the view switcher closes the drawer
- **WHEN** the user taps the view-switcher trigger and the drawer is open
- **THEN** the drawer closes

#### Scenario: View switcher is gray when drawer is closed
- **WHEN** the drawer is closed
- **THEN** the view-switcher trigger is rendered in an inactive/gray color

#### Scenario: View switcher is active when drawer is open
- **WHEN** the drawer is open
- **THEN** the view-switcher trigger is rendered in an active color

### Requirement: Failures surface in the list UI state
Network and save failures SHALL surface as an error state in the list UI state and be displayed on the list screen. The error state SHALL clear on retry or refresh.

#### Scenario: A failed mutation shows an error state
- **WHEN** a network or save mutation fails
- **THEN** the list screen displays an error state derived from the UI state

#### Scenario: Error state clears on retry or refresh
- **WHEN** the error state is displayed and a retry or refresh succeeds
- **THEN** the error state is cleared
