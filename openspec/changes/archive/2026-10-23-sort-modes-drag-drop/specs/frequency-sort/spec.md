# frequency-sort Delta

## Purpose

Order items by all-time check-off count so frequently checked items surface first, with counts that follow the backend contract.

## ADDED Requirements

### Requirement: Check counts follow the backend increment rules
Each item's check count SHALL increment by 1 on an unchecked→checked transition, SHALL remain unchanged on a manual uncheck (including post-trip resets) and on no-op flips, SHALL decrement by 1 only via the device-local undo path (floor 0), and SHALL start at 0. Editing an item or changing shop visibility SHALL NOT reset the count.

#### Scenario: Checking increments the count
- **WHEN** the user checks off an item
- **THEN** its check count increases by 1

#### Scenario: Unchecking does not decrement
- **WHEN** the user manually unchecks an item
- **THEN** the check count is unchanged

#### Scenario: Undo decrements
- **WHEN** the user undoes a check-off via the device-local undo path
- **THEN** the check count decrements by one, floored at 0

#### Scenario: Editing does not reset the count
- **WHEN** the user renames an item or changes its shop visibility
- **THEN** the check count is unchanged

### Requirement: Frequency ordering is count descending with alphabetical tie-break
Frequency ordering SHALL sort items by check count descending, breaking ties case-insensitively by name. It SHALL apply across all shops including Global and SHALL NOT mutate the stored manual order — switching back to Manual restores the exact previous order.

#### Scenario: Higher count first
- **WHEN** "Milk" has 5 checks and "Bread" has 2
- **THEN** "Milk" renders above "Bread" in Frequency order

#### Scenario: Tie-break is case-insensitive alphabetical
- **WHEN** two items have equal counts, named "apple" and "Banana"
- **THEN** "apple" is ordered before "Banana"

#### Scenario: Switching back restores manual order
- **WHEN** the user switches from Manual to Frequency and back to Manual
- **THEN** the list restores the exact manual order it had before

### Requirement: The list re-orders live as counts change
The list SHALL re-order immediately when a check count changes — from a local check, a device-local undo, or a realtime sync from another device — without any reload.

#### Scenario: Local check re-orders live
- **WHEN** the user checks an item while Frequency order is active
- **THEN** the item moves to its new frequency position immediately

#### Scenario: Remote sync re-orders live
- **WHEN** another device checks an item and the event arrives via realtime sync
- **THEN** the list re-orders without any reload
