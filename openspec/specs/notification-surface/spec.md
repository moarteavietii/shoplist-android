# notification-surface Specification

## Purpose

Define the unified visual language for transient notifications (change toasts and failure toasts) and the persistent offline status chip, so all app feedback reads as a coherent, design-system-consistent notification layer instead of ad-hoc text.

## Requirements

### Requirement: Transient notifications render as high-contrast inverse pills
Transient notifications (change toasts and failure toasts) SHALL render as a floating pill at the top-center of the list viewport with: an inverse surface background (high contrast against the list), a full-pill shape, a soft drop shadow, and single-line text in the inverse content color with ellipsis overflow. The pill SHALL remain legible over any list content beneath it.

#### Scenario: Change toast styling
- **WHEN** a change toast is displayed
- **THEN** it renders as a top-center pill with inverse surface background, full-pill shape, soft shadow, and single-line inverse-content text

#### Scenario: Pill is legible over list content
- **WHEN** list rows scroll beneath a visible notification pill
- **THEN** the pill remains fully legible and visually distinct from the rows

### Requirement: Notifications carry a severity LED
Each transient notification SHALL display a leading LED dot whose color reflects severity: the standard LED color for informational change toasts, and the danger color for failure notifications. Failure notifications SHALL be visually distinguishable from informational ones by this LED alone.

#### Scenario: Informational toast LED
- **WHEN** a remote change toast ("checked: Bread") is displayed
- **THEN** its LED dot uses the standard LED-on color

#### Scenario: Failure toast LED
- **WHEN** a failure notification ("Couldn't refresh the list") is displayed
- **THEN** its LED dot uses the danger color

### Requirement: Transient failures surface as failure toasts
Transient operation failures (e.g. refresh, save, move, delete failures) SHALL be surfaced as danger-severity notifications in the same top-center pill system as change toasts, replacing the previous plain error text at the bottom of the list. A failure notification SHALL be dismissible by tap or horizontal swipe, and SHALL clear automatically when the failed operation subsequently succeeds.

#### Scenario: Refresh failure shows failure toast
- **WHEN** a list refresh fails while the backend is unreachable
- **THEN** a danger-severity pill reading "Couldn't refresh the list" appears at the top-center, and no plain error text appears at the bottom

#### Scenario: Failure toast dismissal
- **WHEN** the user taps or horizontally swipes a visible failure pill
- **THEN** the pill is dismissed

#### Scenario: Failure clears on success
- **WHEN** the failed operation (e.g. refresh) succeeds afterwards
- **THEN** the failure pill is cleared automatically

### Requirement: Offline state renders as a persistent status chip
While the app is offline, the list screen SHALL display a persistent, quiet status chip (LED dot + label, e.g. "Offline — changes will be queued") instead of the previous plain text at the bottom. The chip SHALL use the design system's chrome/surface styling, SHALL NOT demand attention like an error, and SHALL disappear when connectivity is restored.

#### Scenario: Offline chip appears
- **WHEN** the app loses connectivity to the backend
- **THEN** a status chip with an LED dot and the label "Offline — changes will be queued" is displayed

#### Scenario: Offline chip clears on reconnect
- **WHEN** connectivity is restored
- **THEN** the chip disappears

#### Scenario: Chip does not block the list
- **WHEN** the offline chip is visible
- **THEN** list scrolling and item taps outside the chip are unaffected
