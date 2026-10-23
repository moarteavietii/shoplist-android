## Purpose

Provide a left-anchored settings drawer, opened from the widget stack, that hosts app-level preferences — the theme picker and the change-toasts toggle — in a single extensible home that does not crowd the list viewport.

## ADDED Requirements

### Requirement: The widget stack renders a Settings button
The widget stack SHALL render a Settings button (gear icon) below the Search button, styled consistently with the other widget-stack controls. Tapping it SHALL open the settings drawer.

#### Scenario: Settings button is present below search
- **WHEN** the widget stack is expanded
- **THEN** a gear-icon Settings button appears beneath the Search button

#### Scenario: Tapping the settings button opens the drawer
- **WHEN** the user taps the Settings button
- **THEN** the settings drawer opens

### Requirement: The settings drawer opens and closes like the view drawer
The settings drawer SHALL be a left-anchored panel over a dimming scrim, visually and behaviorally consistent with the view drawer. It SHALL close when the user taps the scrim or presses the system back gesture.

#### Scenario: Drawer opens with animation
- **WHEN** the user opens the settings drawer
- **THEN** a left-anchored panel slides in over a dimming scrim, consistent with the view drawer's presentation

#### Scenario: Tapping the scrim closes the drawer
- **WHEN** the settings drawer is open and the user taps the scrim outside it
- **THEN** the drawer closes

#### Scenario: Back closes the drawer
- **WHEN** the settings drawer is open and the user performs the system back gesture
- **THEN** the drawer closes instead of leaving the app

### Requirement: The drawer hosts a Theme section
The settings drawer SHALL contain a "Theme" section listing all nine catalog themes. Each option SHALL show the theme's name and a small color swatch preview, and the currently active theme SHALL be visually indicated. Exactly one option SHALL be indicated as active at any time.

#### Scenario: All selectable themes are listed
- **WHEN** the settings drawer is open
- **THEN** the Theme section lists all nine themes, each with a name and swatch preview

#### Scenario: Active theme is indicated
- **WHEN** the settings drawer is open with a non-default theme active
- **THEN** that theme's option is visually marked as active

### Requirement: Selecting a theme applies it immediately
Selecting a theme SHALL apply it to the entire UI immediately, without a restart and without requiring the drawer to close.

#### Scenario: Theme applies on selection
- **WHEN** the user selects a theme in the drawer
- **THEN** the whole UI switches to that theme's palette immediately and the drawer stays open

#### Scenario: Selecting the default theme removes the override
- **WHEN** the user selects the Default theme while a non-default theme is active
- **THEN** the app returns to the default palette immediately

### Requirement: Change-toast toggle
The settings drawer SHALL include a "change toasts" toggle that enables or disables change toasts for remote list changes. The default state SHALL be enabled, the state SHALL persist across restarts, and toggling SHALL take effect immediately across all mounted components without a reload.

#### Scenario: Toggle present in settings drawer
- **WHEN** the user opens the settings drawer
- **THEN** a "change toasts" toggle is visible

#### Scenario: Default is enabled
- **WHEN** the user has never touched the toggle
- **THEN** change toasts are enabled

#### Scenario: Toggle takes effect immediately
- **WHEN** the user toggles change toasts while other components that consume the setting are mounted
- **THEN** the new state applies to those components immediately, without a reload

#### Scenario: State persists across restart
- **WHEN** the user toggles change toasts off and relaunches the app
- **THEN** the toggle is still off and change toasts remain disabled
