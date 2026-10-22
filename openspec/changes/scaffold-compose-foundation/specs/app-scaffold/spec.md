## Purpose

Defines the application shell for the native Android client: the Compose-based UI foundation carrying the custom design language, the dependency-injection and networking stack, per-build-type server configuration, and the health-check contract that proves end-to-end connectivity to the backend.

## ADDED Requirements

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
The app SHALL perform REST calls through Retrofit over OkHttp with kotlinx.serialization for JSON conversion, and SHALL expose a health-check API for `GET /health`.

#### Scenario: Health check succeeds against a healthy backend
- **WHEN** the app calls the health-check endpoint and the backend is reachable
- **THEN** the call returns the backend's `{ status: "ok" }` response

#### Scenario: Health check reports an unreachable backend
- **WHEN** the app calls the health-check endpoint and the backend is unreachable
- **THEN** the call fails with an error the UI can surface, without crashing the app

### Requirement: Base URL is selected per build type
The app SHALL use `https://shoppinglist.pascariu.eu` as the base URL in release builds and `http://192.168.0.141:3000` in debug builds, and debug builds SHALL permit cleartext HTTP for the dev host only.

#### Scenario: Release build targets production
- **WHEN** a release build performs any REST call
- **THEN** the request targets `https://shoppinglist.pascariu.eu`

#### Scenario: Debug build targets the LAN dev host
- **WHEN** a debug build performs any REST call
- **THEN** the request targets `http://192.168.0.141:3000`

#### Scenario: Cleartext is restricted to the dev host
- **WHEN** a debug build attempts cleartext HTTP to any host other than the dev host
- **THEN** the platform blocks the request

### Requirement: No static title header
The app SHALL NOT render a persistent application title or header bar at the top of the viewport.

#### Scenario: No title is rendered
- **WHEN** the app renders
- **THEN** no static application title or header bar is visible at the top of the screen

### Requirement: Health status is visible in the UI
The app SHALL display the backend health status returned by the health-check call.

#### Scenario: Health status is visible
- **WHEN** the app is launched and the backend is healthy
- **THEN** the UI displays the health status returned by the backend
