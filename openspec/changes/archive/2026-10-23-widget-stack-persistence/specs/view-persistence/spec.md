# view-persistence Delta

## Purpose

Persist the user's selected shopping context across sessions so the app reopens in the same view.

## ADDED Requirements

### Requirement: The active shop id persists across sessions
The app SHALL persist the active shop's numeric id whenever it changes (view drawer selection) and restore it on launch. The id SHALL use a single dedicated storage key, isolated from other persisted list state.

#### Scenario: Selected view survives restart
- **WHEN** the user selects the "Carrefour" view and restarts the app
- **THEN** the Carrefour view is active on launch, with its watermark, filtering, and ordering

#### Scenario: Every selection is written
- **WHEN** the user switches the active view
- **THEN** the new shop id is written to persistent storage immediately

### Requirement: Invalid or missing persisted ids fall back to Global
On launch, the app SHALL activate the Global view when nothing is persisted, or when the persisted shop id is not present in the fetched shop list. The persisted value SHALL be validated against the fetched shops before being applied.

#### Scenario: Nothing persisted defaults to Global
- **WHEN** the app launches for the first time
- **THEN** the Global view is active

#### Scenario: Persisted id not in fetched shops falls back to Global
- **WHEN** the persisted shop id references a shop that no longer exists in the fetched list
- **THEN** the Global view is active and the invalid id is not applied

#### Scenario: Persisted id is validated once shops load
- **WHEN** the app launches with a persisted id but the shop list has not loaded yet
- **THEN** the app does not render a view state derived from the unvalidated id
