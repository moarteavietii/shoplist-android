# app-identity Specification

## Purpose

Defines the app's user-facing identity on the device: the launcher icon (adaptive layers and legacy fallbacks), the launcher label, and the signing configuration that makes release builds installable and updatable.

## Requirements

### Requirement: Launcher icon uses the web app's artwork
The app SHALL use the web project's shopping-cart icon (blue cart with checkmark badge on white) as its launcher icon, in both the launcher's default rendering and the round-icon variant.

#### Scenario: Launcher shows the new icon
- **WHEN** the app is installed and viewed in the device launcher
- **THEN** the icon shown is the shopping-cart artwork, not the stock template icon

#### Scenario: Round icon variant is available
- **WHEN** a launcher requests a round icon
- **THEN** the system resolves a round variant of the same artwork

### Requirement: Adaptive icon artwork survives launcher masking
The adaptive icon's foreground layer SHALL place all meaningful artwork within the adaptive-icon safe zone (the central 66dp circle of the 108dp canvas, shrunk a further ~20% as verified on-device), with the white background provided by the background layer, so that no visually important part of the artwork is clipped by any launcher mask (circle, squircle, or rounded square).

#### Scenario: Circle mask does not clip the cart
- **WHEN** the launcher applies a circular mask to the icon
- **THEN** the full cart and checkmark remain visible with no clipped edges

#### Scenario: Background layer is opaque white
- **WHEN** the adaptive icon is rendered
- **THEN** the background layer presents a uniform opaque white surface behind the foreground artwork

### Requirement: Legacy icon fallbacks match the new artwork
The density-specific raster launcher icons (`mipmap-*`) SHALL be regenerated from the new artwork so that launchers or system surfaces that do not use the adaptive icon render the same shopping-cart design.

#### Scenario: Non-adaptive consumer renders the same design
- **WHEN** a system surface resolves the icon from a density-specific raster resource
- **THEN** it displays the shopping-cart artwork consistent with the adaptive icon

### Requirement: Launcher label is the product name
The launcher label SHALL be `shopy`, applied via the `app_name` string resource, and SHALL render correctly in the launcher and in system settings. (The name is plain ASCII by user decision — a diacritic variant was rejected for searchability.)

#### Scenario: Label under the icon
- **WHEN** the app is viewed in the device launcher
- **THEN** the label beneath the icon reads `shopy`

#### Scenario: Label in system settings
- **WHEN** the app's entry is opened in the system Settings app list
- **THEN** the app name reads `shopy`

### Requirement: Release builds are signed with a user-controlled key
The release build type SHALL be signed with a keystore controlled by the user, with keystore path and passwords supplied from gitignored configuration (never committed), so that release artifacts install on devices and can be updated in place by builds signed with the same key.

#### Scenario: Release build is signed
- **WHEN** a release build is assembled
- **THEN** the produced artifact is signed and installs successfully on a device

#### Scenario: Secrets stay out of the repository
- **WHEN** the repository contents are inspected
- **THEN** no keystore file, keystore path, or keystore password is present in version-controlled files

#### Scenario: Missing signing configuration fails clearly
- **WHEN** a release build is requested on a machine where the keystore properties are not configured
- **THEN** the build fails with an error that identifies the missing signing configuration rather than producing an unsigned artifact silently
