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
The app SHALL provide a credential entry screen that accepts the Cloudflare Access client id and secret via manual text input and via QR scan. The QR payload SHALL be a JSON object `{"clientId":"...","clientSecret":"..."}` (unknown keys tolerated), matching the format used by the helloworldcosmin app so QR codes are interchangeable. The credential probe SHALL verify credentials against a gate-protected JSON API route (`GET /api/v1/shops`), not `/health` — on the production deployment `/health` is served by the web frontend's SPA fallback and returns HTML even when the gate accepts the token, so it cannot distinguish accepted from rejected credentials.

#### Scenario: Manual entry is accepted
- **WHEN** the user enters a client id and secret on the credential entry screen and submits
- **THEN** the credentials are stored encrypted and the app proceeds

#### Scenario: QR scan is accepted
- **WHEN** the user scans a QR code encoding the credentials as the JSON payload
- **THEN** the credentials are extracted, stored encrypted, and the app proceeds

#### Scenario: QR payload with unknown keys is accepted
- **WHEN** the scanned JSON payload contains additional keys beyond `clientId` and `clientSecret`
- **THEN** the two known fields are extracted and treated as the credentials

#### Scenario: Unrecognized QR payload is rejected
- **WHEN** the scanned payload is not valid JSON with non-blank `clientId` and `clientSecret`
- **THEN** the app shows an unrecognized-format error and stores nothing

### Requirement: Rejected credentials trigger re-entry
The app SHALL respond to credential rejection by navigating to the credential entry screen, preserving the user's context where practical. Rejection is signaled by a 401 or 403 response, or by a response that the HTTP client followed to the Cloudflare Access login page (HTML instead of JSON).

#### Scenario: 401 response leads to re-entry
- **WHEN** any REST call receives a 401 or 403 response
- **THEN** the app navigates to the credential entry screen instead of showing raw error content

#### Scenario: Gate login-page redirect leads to re-entry
- **WHEN** a REST call is redirected to the Cloudflare Access login page and returns HTML instead of JSON
- **THEN** the app treats this as credential rejection: stored credentials are cleared and the credential entry screen is shown

#### Scenario: Fresh install shows credential entry proactively
- **WHEN** the app starts with no stored credentials in a build that requires them
- **THEN** the credential entry screen is shown before any data refresh is attempted

#### Scenario: Rejected credential probe does not crash
- **WHEN** the credential probe receives HTML (or any non-JSON body) instead of the expected JSON
- **THEN** the probe outcome is treated as rejected credentials, an error message is shown, and nothing is stored

#### Scenario: Probe parse-failure classification is deliberately coarse
- **WHEN** the probe response cannot be parsed as the expected JSON schema (gate HTML, an error envelope, or a drifted response shape)
- **THEN** all such parse failures are classified as rejected credentials, even though a 200 response with a drifted shape would misreport valid credentials as invalid
- **AND** this coarse mapping is an accepted trade-off: distinguishing gate HTML from a drifted JSON shape would require refactoring the probe to inspect raw response bodies, and response-shape drift is guarded by the OpenAPI contract-verification workflow instead

### Requirement: Debug builds can bypass auth at build time
The app SHALL make auth skippable per build type: debug builds SHALL use a no-op auth interceptor provided through Hilt, and the bypass SHALL be a build-time switch rather than scattered runtime conditionals.

#### Scenario: Debug build sends no auth headers
- **WHEN** a debug build performs a REST call against the dev backend
- **THEN** no `CF-Access-*` headers are attached and the call succeeds

#### Scenario: Release build always authenticates
- **WHEN** a release build performs a REST call
- **THEN** the auth interceptor is the real credential-injecting implementation

### Requirement: Debug builds can mock the Cloudflare gate
Debug builds SHALL provide a mock Cloudflare gate that validates stored credentials against a known fake credential pair before any request reaches the backend. When the credentials match the fake pair, the request SHALL proceed to the backend; otherwise the gate SHALL synthesize a 401 response without performing a network call. The mock gate SHALL be active only when explicitly enabled via a local, gitignored build configuration flag, and SHALL be inert in all other builds.

#### Scenario: Fake credentials allow requests through
- **WHEN** the mock gate is enabled and the stored credentials match the fake credential pair
- **THEN** requests proceed to the backend and succeed

#### Scenario: Non-fake credentials are rejected with a synthesized 401
- **WHEN** the mock gate is enabled and the stored credentials do not match the fake credential pair
- **THEN** a 401 response is produced without any network call, and the app's standard rejection handling (credential clearing and re-entry) runs as it would in production

#### Scenario: Mock gate is disabled by default
- **WHEN** the enable flag is not set in a debug build
- **THEN** the app behaves exactly as before this change (no credential validation, no synthesized responses)

#### Scenario: Release builds never mock
- **WHEN** a release build performs any request
- **THEN** the mock gate is a pass-through and no credential validation or response synthesis occurs

### Requirement: Credentials are verified against the server before saving
The credential entry flow SHALL verify entered credentials against the server before persisting them. Verification SHALL use the same request path production requests use, so the mock gate (when enabled) participates. The probe SHALL target a gate-protected JSON API route (`GET /api/v1/shops`) rather than `/health`: on the production deployment `/health` is served by the web frontend's SPA fallback and returns HTML even when the gate accepts the token, so it cannot distinguish accepted from rejected credentials. Credentials SHALL NOT be stored before verification succeeds: on rejection, server error, or network failure the credentials SHALL NOT be stored and an error SHALL be shown on the page.

#### Scenario: Valid credentials are saved and the app proceeds
- **WHEN** the user submits credentials and server verification succeeds
- **THEN** the credentials are stored encrypted and the app navigates forward

#### Scenario: Invalid credentials show an on-page error
- **WHEN** the user submits credentials and server verification is rejected (401/403)
- **THEN** the credentials are not stored and the page displays an invalid-credentials error

#### Scenario: Unreachable server shows a distinct error
- **WHEN** the user submits credentials and the server cannot be reached
- **THEN** the credentials are not stored and the page displays a connectivity error distinct from the invalid-credentials error

#### Scenario: Server error shows a distinct error
- **WHEN** the user submits credentials and server verification fails with an HTTP error other than 401/403 (e.g. 5xx)
- **THEN** the credentials are not stored and the page displays a server-error message distinct from both the invalid-credentials and connectivity errors

#### Scenario: Credentials are never persisted before verification
- **WHEN** the user submits credentials and verification fails for any reason (rejection, server error, or network failure)
- **THEN** no credential material from that submission exists in storage after the failure

### Requirement: Credential entry errors are classified by cause
The credential entry flow SHALL classify verification failures into distinct error kinds — invalid credentials (401/403), server error (any other HTTP error), and unreachable server (network failure) — and SHALL display a distinct message for each kind on the credential entry page.

#### Scenario: Server error is not reported as a connectivity problem
- **WHEN** verification fails with an HTTP 5xx response
- **THEN** the page displays a server-error message and does not display the connectivity error message

#### Scenario: Each error kind has distinct copy
- **WHEN** verification fails with a 401/403, an HTTP 5xx, or a network failure in turn
- **THEN** the page displays three different messages, one per failure kind

### Requirement: Rejected sessions show explicit feedback on re-entry
When the app navigates to the credential entry screen because a request was rejected (401/403), the screen SHALL display an explicit message telling the user their credentials were rejected or the session expired, rather than silently presenting empty fields.

#### Scenario: Bounce-back after rejection shows a message
- **WHEN** a stored-credential request receives a 401 or 403 and the app returns to the credential entry screen
- **THEN** the screen shows a session-expired/credentials-rejected message alongside the empty credential fields
