# app-scaffold Specification

## Purpose

Defines the application shell for the native Android client: the Compose-based UI foundation carrying the custom design language, the dependency-injection and networking stack, and per-build-type server configuration.

## Requirements

### Requirement: Jetpack Compose is the UI toolkit
The app SHALL render its UI entirely with Jetpack Compose; the AppCompat/XML layout and theme scaffolding it replaces SHALL be removed.

#### Scenario: Compose activity renders
- **WHEN** the app is launched
- **THEN** the visible UI is produced by a Compose-hosting activity and no XML layout resources remain in use

### Requirement: Custom design language foundation exists
The app SHALL define a design-language foundation (color, typography, and shape tokens) that expresses the web app's chromeless / borderless / LED-dot aesthetic, and Compose components SHALL be themed by it rather than by a Material theme.

#### Scenario: Design tokens are the single source of UI styling
- **WHEN** any screen in the app is rendered
- **THEN** its colors, typography, and shapes come from the design-language tokens, not from a Material color scheme

### Requirement: Dependency injection is provided by Hilt
The app SHALL use Hilt for dependency injection, with an application class annotated for Hilt and all networking, auth, and repository dependencies provided through Hilt.

#### Scenario: Hilt application class is installed
- **WHEN** the app process starts
- **THEN** the manifest declares a Hilt-annotated application class and dependency graph construction succeeds

### Requirement: Networking stack uses Retrofit, OkHttp, and kotlinx.serialization
The app SHALL perform REST calls through Retrofit over OkHttp with kotlinx.serialization for JSON conversion.

#### Scenario: REST calls go through the typed client
- **WHEN** the app performs any REST call
- **THEN** the request is executed by Retrofit over OkHttp and the response is converted with kotlinx.serialization

### Requirement: Base URL is selected per build type
The app SHALL use `https://shoppinglist.pascariu.eu` as the base URL in release builds. In debug builds the base URL SHALL come from the `devBaseUrl` property in the gitignored `local.properties`, falling back to `http://192.168.0.141:3005` (the workstation's LAN address) when unset, and debug builds SHALL permit cleartext HTTP for the configured dev host and the workstation fallback only.

#### Scenario: Release build targets production
- **WHEN** a release build performs any REST call
- **THEN** the request targets `https://shoppinglist.pascariu.eu`

#### Scenario: Debug build targets the configured dev host
- **WHEN** a debug build performs any REST call and `local.properties` defines `devBaseUrl`
- **THEN** the request targets that `devBaseUrl` value

#### Scenario: Debug build falls back to the workstation
- **WHEN** a debug build performs any REST call and `local.properties` does not define `devBaseUrl`
- **THEN** the request targets `http://192.168.0.141:3005`

#### Scenario: Cleartext is restricted to the dev hosts
- **WHEN** a debug build attempts cleartext HTTP to any host other than the configured dev host or the workstation fallback
- **THEN** the platform blocks the request

### Requirement: No static title header
The app SHALL NOT render a persistent application title or header bar at the top of the viewport.

#### Scenario: No title is rendered
- **WHEN** the app renders
- **THEN** no static application title or header bar is visible at the top of the screen
