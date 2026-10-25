## Purpose

Covers the auth-flow fixes discovered while testing the signed release build on a device: the credential screen never appeared on fresh install, and a rejected probe crashed the app.

## MODIFIED Requirements

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
