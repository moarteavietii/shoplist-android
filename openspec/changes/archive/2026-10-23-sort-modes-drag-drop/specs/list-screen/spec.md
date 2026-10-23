# list-screen Delta

## MODIFIED Requirements

### Requirement: The list orders items by the selected sort mode
The list SHALL order items by the active sort order: Manual (ascending lexicographical `orderMap[activeView]`), A→Z (case-insensitive by name), or Frequency (check count descending, case-insensitive name tie-break). Changing the mode or order SHALL re-derive the list immediately without a reload, and switching back to Manual SHALL restore the exact stored manual order.

#### Scenario: Manual order uses fractional keys
- **WHEN** the order is Manual
- **THEN** items are sorted by ascending `orderMap[activeView]` fractional keys

#### Scenario: A→Z re-orders alphabetically
- **WHEN** the user selects A→Z
- **THEN** items are ordered case-insensitively by name

#### Scenario: Switching back to Manual restores the exact order
- **WHEN** the user switches from A→Z or Frequency back to Manual
- **THEN** the list shows the exact manual order stored before the switch

### Requirement: Rows render a drag handle when drag is enabled
When drag is enabled (sort mode ON and Manual order), each row SHALL render a dedicated drag handle; the handle SHALL be absent when drag is disabled. Row-body touch SHALL never initiate a drag.

#### Scenario: Handle renders in manual mode
- **WHEN** sort mode is ON and the order is Manual
- **THEN** each row renders a drag handle

#### Scenario: No handle outside manual mode
- **WHEN** sort mode is off or the order is A→Z/Frequency
- **THEN** rows render full-width without a handle zone
