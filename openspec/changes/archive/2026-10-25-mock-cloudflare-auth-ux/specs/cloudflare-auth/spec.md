## ADDED Requirements

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
The credential entry flow SHALL verify entered credentials against the server before persisting them. Verification SHALL use the same request path production requests use, so the mock gate (when enabled) participates. On success the credentials are stored encrypted and the app proceeds; on rejection or network failure the credentials SHALL NOT be stored and an error SHALL be shown on the page.

#### Scenario: Valid credentials are saved and the app proceeds
- **WHEN** the user submits credentials and server verification succeeds
- **THEN** the credentials are stored encrypted and the app navigates forward

#### Scenario: Invalid credentials show an on-page error
- **WHEN** the user submits credentials and server verification is rejected (401/403)
- **THEN** the credentials are not stored and the page displays an invalid-credentials error

#### Scenario: Unreachable server shows a distinct error
- **WHEN** the user submits credentials and the server cannot be reached
- **THEN** the credentials are not stored and the page displays a connectivity error distinct from the invalid-credentials error

### Requirement: Rejected sessions show explicit feedback on re-entry
When the app navigates to the credential entry screen because a request was rejected (401/403), the screen SHALL display an explicit message telling the user their credentials were rejected or the session expired, rather than silently presenting empty fields.

#### Scenario: Bounce-back after rejection shows a message
- **WHEN** a stored-credential request receives a 401 or 403 and the app returns to the credential entry screen
- **THEN** the screen shows a session-expired/credentials-rejected message alongside the empty credential fields
