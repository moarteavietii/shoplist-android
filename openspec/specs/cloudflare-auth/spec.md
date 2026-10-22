# cloudflare-auth Specification

## Purpose

Defines Cloudflare Access service-token authentication for the app: credential entry, encrypted storage, header injection on every outbound call, re-entry on rejection, and a build-time bypass for the dev backend that sits outside Cloudflare Access.

## Requirements

### Requirement: Service-token headers are attached to every request
The app SHALL attach `CF-Access-Client-Id` and `CF-Access-Client-Secret` headers to every outbound HTTP request, including SSE connects when they are introduced.

#### Scenario: Authenticated request carries both headers
- **WHEN** the app performs any REST call against the production backend
- **THEN** the request includes both `CF-Access-Client-Id` and `CF-Access-Client-Secret` headers with the stored credentials

### Requirement: Credentials are stored encrypted
The app SHALL store service-token credentials using Android encrypted storage (Keystore-backed), and SHALL NOT persist them in plaintext anywhere.

#### Scenario: Credentials survive restart without plaintext exposure
- **WHEN** the app is restarted after credentials were entered
- **THEN** the credentials are read from encrypted storage and no plaintext credential file exists in app-private storage

### Requirement: Credential entry screen supports manual and QR input
The app SHALL provide a credential entry screen that accepts the Cloudflare Access client id and secret via manual text input and via QR scan.

#### Scenario: Manual entry is accepted
- **WHEN** the user enters a client id and secret on the credential entry screen and submits
- **THEN** the credentials are stored encrypted and the app proceeds

#### Scenario: QR scan is accepted
- **WHEN** the user scans a QR code encoding the credentials
- **THEN** the credentials are extracted, stored encrypted, and the app proceeds

### Requirement: Rejected credentials trigger re-entry
The app SHALL respond to a 401 or 403 response from the backend by navigating to the credential entry screen, preserving the user's context where practical.

#### Scenario: 401 response leads to re-entry
- **WHEN** any REST call receives a 401 or 403 response
- **THEN** the app navigates to the credential entry screen instead of showing raw error content

### Requirement: Debug builds can bypass auth at build time
The app SHALL make auth skippable per build type: debug builds SHALL use a no-op auth interceptor provided through Hilt, and the bypass SHALL be a build-time switch rather than scattered runtime conditionals.

#### Scenario: Debug build sends no auth headers
- **WHEN** a debug build performs a REST call against the dev backend
- **THEN** no `CF-Access-*` headers are attached and the call succeeds

#### Scenario: Release build always authenticates
- **WHEN** a release build performs a REST call
- **THEN** the auth interceptor is the real credential-injecting implementation
