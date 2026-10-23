# sort-modes Delta

## Purpose

Let users toggle manual drag-and-drop reordering on/off and choose the list ordering: Manual, A→Z, or Frequency.

## ADDED Requirements

### Requirement: The sort mode control has two exclusive interactions
The widget stack's Sort Mode control SHALL support two exclusive interactions: tapping it toggles manual drag-and-drop reordering on/off, and long-pressing it opens a sort-order selector anchored to the button. A long-press SHALL NOT also toggle the mode.

#### Scenario: Tap toggles sort mode
- **WHEN** the user taps the Sort Mode control
- **THEN** manual drag-and-drop reordering toggles on or off

#### Scenario: Long-press opens the order selector
- **WHEN** the user long-presses the Sort Mode control
- **THEN** a sort-order selector opens anchored to the button and the mode is not toggled

### Requirement: The sort-order selector offers exactly three options
The selector SHALL offer exactly three options — Manual, A→Z, Frequency — with the active option visually indicated. Selecting an option SHALL apply it immediately and close the selector. The selector SHALL close on outside tap or list scroll without blocking the scroll.

#### Scenario: Three options are offered
- **WHEN** the selector opens
- **THEN** it shows exactly Manual, A→Z, and Frequency, with the active option visually indicated

#### Scenario: Selection applies immediately
- **WHEN** the user selects "A→Z"
- **THEN** the list re-orders alphabetically at once and the selector closes

#### Scenario: Outside tap or scroll closes the selector
- **WHEN** the selector is open and the user taps outside it or scrolls the list
- **THEN** the selector closes without changing the order

### Requirement: The LED dot marks Manual order only
The Sort Mode control SHALL render its LED dot filled while the order is Manual and sort mode is ON, and hollow while the order is Manual and sort mode is OFF. In A→Z or Frequency order the control SHALL NOT render a dot but SHALL keep active styling in A→Z.

#### Scenario: Dot filled in manual order
- **WHEN** sort mode is ON and the order is Manual
- **THEN** the Sort Mode control shows a filled LED dot

#### Scenario: No dot in A→Z or Frequency
- **WHEN** the order is A→Z or Frequency
- **THEN** the Sort Mode control shows no LED dot

### Requirement: Drag-and-drop is enabled only when sort mode is on and order is Manual
Row drag-and-drop SHALL be enabled only while sort mode is ON and the order is Manual. In A→Z or Frequency order, drag attempts SHALL NOT reorder and SHALL NOT re-sort the list to manual order mid-gesture.

#### Scenario: Drag works in manual mode
- **WHEN** sort mode is ON and the order is Manual
- **THEN** dragging a row's handle reorders the list

#### Scenario: Drag is inert in A→Z or Frequency
- **WHEN** the order is A→Z or Frequency
- **THEN** drag attempts do not reorder items and the list does not switch to manual order mid-gesture

### Requirement: No viewport highlight border while drag-and-drop is active
The app SHALL NOT render the web app's fullscreen highlight border while drag-and-drop is active (sort mode ON and Manual order). Sort-mode visibility SHALL be carried by the Sort Mode control's LED dot only.

#### Scenario: No border in manual sort
- **WHEN** sort mode is ON and the order is Manual
- **THEN** no highlight border is rendered around the viewport

#### Scenario: No border during a drag
- **WHEN** the user drags a row
- **THEN** no highlight border appears around the viewport

### Requirement: Sort mode and order persist across sessions
Sort mode SHALL persist across sessions (default ON) and the sort-order choice SHALL persist (default Manual). Toggling sort mode SHALL NOT change the app theme.

#### Scenario: Sort mode survives restart
- **WHEN** the user disables sort mode and restarts the app
- **THEN** sort mode is restored as off

#### Scenario: Order choice survives restart
- **WHEN** the user selects Frequency and relaunches the app
- **THEN** Frequency ordering is active

#### Scenario: Toggling never mutates theme
- **WHEN** the user toggles sort mode or changes the order
- **THEN** the app theme remains unchanged
