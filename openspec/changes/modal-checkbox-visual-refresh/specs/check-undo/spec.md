## MODIFIED Requirements

### Requirement: Undo affordance lives in the quick menu

The undo affordance SHALL be a quick menu control button (undo icon, same button style as the other quick menu controls) instead of a full-width undo bar in the list. The button SHALL render only while the history holds at least one entry and SHALL disappear when the history is empty. Showing and hiding the button SHALL animate (vertical expand/shrink with fade) so the quick menu container grows and shrinks at its bottom edge instead of the button popping in or out. While an undo request is in flight, the button SHALL stay mounted but render disabled (dimmed icon, taps ignored). Tapping the button SHALL trigger the same validated single-entry undo flow specified elsewhere.

#### Scenario: Undo button appears when history is non-empty

- **WHEN** the check history holds at least one entry
- **THEN** the quick menu renders an undo control button

#### Scenario: Undo button disappears when history is empty

- **WHEN** the check history is empty (nothing to undo, or the last entry was undone)
- **THEN** the quick menu renders no undo control button

#### Scenario: Undo button is disabled while undo is in flight

- **WHEN** an undo request is in flight
- **THEN** the undo button stays visible but renders disabled and taps are ignored

#### Scenario: No undo bar in the list

- **WHEN** the list screen renders with a non-empty check history
- **THEN** no full-width undo bar is rendered in the list
