## Purpose

Removes the unversioned `/health` exception from the versioned-prefix rule now that the app no longer calls `/health`. See the change proposal for motivation.

## MODIFIED Requirements

### Requirement: The app calls the versioned API surface
The app SHALL send all REST requests and open all SSE connections against the versioned prefix `/api/v1`. No request SHALL target the unversioned `/api/...` prefix.

#### Scenario: REST requests carry the versioned prefix
- **WHEN** the app sends any REST request (items or shops endpoints)
- **THEN** the request path starts with `/api/v1/`

#### Scenario: SSE connections carry the versioned prefix
- **WHEN** the app opens either SSE subscription
- **THEN** the connection URL path starts with `/api/v1/`
