## MODIFIED Requirements

### Requirement: Credentials are verified against the server before saving
The credential entry flow SHALL verify entered credentials against the server before persisting them. Verification SHALL use the same request path production requests use, so the mock gate (when enabled) participates. Every request to the server is Cloudflare-Access-gated, so a successful probe of the health endpoint constitutes valid credential verification. Credentials SHALL NOT be stored before verification succeeds: on rejection, server error, or network failure the credentials SHALL NOT be stored and an error SHALL be shown on the page.

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

## ADDED Requirements

### Requirement: Credential entry errors are classified by cause
The credential entry flow SHALL classify verification failures into distinct error kinds — invalid credentials (401/403), server error (any other HTTP error), and unreachable server (network failure) — and SHALL display a distinct message for each kind on the credential entry page.

#### Scenario: Server error is not reported as a connectivity problem
- **WHEN** verification fails with an HTTP 5xx response
- **THEN** the page displays a server-error message and does not display the connectivity error message

#### Scenario: Each error kind has distinct copy
- **WHEN** verification fails with a 401/403, an HTTP 5xx, or a network failure in turn
- **THEN** the page displays three different messages, one per failure kind
