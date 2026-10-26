# hide-checked-items Specification

## Purpose

Let users hide checked items from the list via a widget-stack toggle, with smooth animations and a search-override rule.

## Requirements

### Requirement: The Checked-Items Toggle controls checked-item visibility
The list screen's Checked-Items Toggle SHALL control whether checked items appear in the list. It SHALL be enabled (showing) by default, render an LED dot with dimmed/active styling, and expose its pressed state to accessibility services. The state SHALL persist across sessions.

While manual sorting is engaged, checked items SHALL remain visible regardless of the toggle, and the toggle SHALL reflect this effective visibility in its LED and pressed state (see the `quick-menu` capability). Tapping the toggle while manual sorting is engaged SHALL surface a rejection message instead of changing the stored preference.

#### Scenario: Toggle hides checked items
- **WHEN** the user disables the toggle
- **THEN** checked items are excluded from the view but kept in data, and unchecked items keep their order

#### Scenario: Toggle state survives restart
- **WHEN** the user disables the toggle and restarts the app
- **THEN** checked items remain hidden

#### Scenario: Pressed state is accessible
- **WHEN** an accessibility service reads the toggle
- **THEN** it exposes its pressed (showing/hiding) state

#### Scenario: Effective visibility during manual sort
- **WHEN** manual sorting is engaged while the toggle is disabled
- **THEN** checked items are visible in the list and the toggle's LED and pressed state reflect them as shown

### Requirement: Hiding animates and check-off commits instantly
When checked items are hidden, the transition SHALL read as a single fluid motion: each checked row's exit SHALL combine vertical height collapse with fade and a small horizontal drift, so unchecked items begin reflowing while the checked rows are still dissolving. Exits across multiple checked rows SHALL be staggered top-to-bottom, with the per-row delay compressed as the checked count grows so the total transition stays bounded (small lists get a pronounced peel; large lists compress the stagger rather than extending the duration). Re-enabling the toggle SHALL animate checked rows back in (grow + fade) in their original order, with a mirrored stagger. Exit animations SHALL be faster than enter animations (asymmetric timing). Check/uncheck SHALL commit instantly regardless of the toggle, and the undo bar SHALL remain available to restore an item that animated out. A single item animating out on check-off SHALL use the same collapse-while-fade exit without stagger.

#### Scenario: Hiding plays as one motion
- **WHEN** the user disables the toggle with several checked items visible
- **THEN** each checked row collapses vertically while fading, and unchecked items move up during — not after — the exits

#### Scenario: Stagger compresses for large counts
- **WHEN** many checked items are hidden at once
- **THEN** the per-row delay shrinks as the count grows, keeping the total transition brief instead of scaling linearly with item count

#### Scenario: Re-showing mirrors the choreography
- **WHEN** the user re-enables the toggle
- **THEN** checked rows animate back in (grow + fade) in their original order, with a staggered entrance

#### Scenario: Exit is faster than enter
- **WHEN** comparing the hide and re-show transitions
- **THEN** the exit completes in less time than the entrance

#### Scenario: Checking animates the item out
- **WHEN** the user checks an item while hiding is active
- **THEN** the item animates out (collapse + fade) and the check commits instantly

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

### Requirement: Manual sort engagement overrides the hide toggle
While manual sort is engaged (sort mode ON and Manual order), all items in the active view SHALL always be visible — including checked items — regardless of this toggle, so the list never reflows when a drag starts. Hidden checked items SHALL reappear the moment manual sort is engaged and hide again when it is disengaged (order switched to A→Z/Frequency or sort mode disabled). This supersedes the web spec's "reveal at drag start" rule.

#### Scenario: Engaging manual sort reveals all items
- **WHEN** the user enables manual sort while checked items are hidden
- **THEN** all items appear instantly in sorted position, with no reflow of the unchecked items

#### Scenario: No list change at drag start
- **WHEN** the user starts dragging a row while checked items are hidden
- **THEN** the list does not change — no items appear or disappear at drag start

#### Scenario: Disengaging manual sort restores hiding
- **WHEN** the user switches to A→Z or Frequency order, or disables sort mode
- **THEN** hidden checked items are hidden again per this toggle
