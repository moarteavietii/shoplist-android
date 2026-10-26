## MODIFIED Requirements

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
