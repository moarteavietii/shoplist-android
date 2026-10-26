## Purpose

Retires the health-check capability: the `GET /health` path is dead code in production and would fail against the SPA-fallback HTML served at `/health` on the production deployment. See the change proposal for motivation.

## MODIFIED Requirements

### Requirement: Networking stack uses Retrofit, OkHttp, and kotlinx.serialization
The app SHALL perform REST calls through Retrofit over OkHttp with kotlinx.serialization for JSON conversion.

#### Scenario: REST calls go through the typed client
- **WHEN** the app performs any REST call
- **THEN** the request is executed by Retrofit over OkHttp and the response is converted with kotlinx.serialization

## REMOVED Requirements

### Requirement: Health status is visible in the UI
**Reason:** The health-check path is dead code in production — the health screen has no entry point, and on the production deployment `/health` is served by the web frontend's SPA fallback and returns HTML, so the capability cannot work as specified. End-to-end connectivity is proven by the credential probe against `GET /api/v1/shops`.

**Migration:** None required; the path is unreachable in the current app. Connectivity diagnostics are covered by the credential probe and the OpenAPI contract-verification workflow.
