### Requirement: The app calls the versioned API surface
The app SHALL send all REST requests and open all SSE connections against the versioned prefix `/api/v1`. No request SHALL target the unversioned `/api/...` prefix.

#### Scenario: REST requests carry the versioned prefix
- **WHEN** the app sends any REST request (items or shops endpoints)
- **THEN** the request path starts with `/api/v1/`

#### Scenario: SSE connections carry the versioned prefix
- **WHEN** the app opens either SSE subscription
- **THEN** the connection URL path starts with `/api/v1/`

### Requirement: The backend contract is the source of truth for API work
When implementing or changing anything that touches server endpoints or SSE (paths, request/response bodies, event names, event payloads), the app's implementation SHALL be derived from the backend's machine-readable contract documents — the live OpenAPI document (`GET /openapi.json` on the backend) for REST and the AsyncAPI document in the web repo for SSE — rather than re-derived from observation or memory.

#### Scenario: Endpoint work starts from the contract
- **WHEN** a change touches any REST endpoint path, request body, or response shape
- **THEN** the implementation matches the OpenAPI document's declared path, method, and schema for that endpoint

#### Scenario: SSE work starts from the contract
- **WHEN** a change touches SSE event names or event payload fields
- **THEN** the implementation matches the AsyncAPI document's channel and message schemas

### Requirement: Breaking backend changes arrive as a new major prefix
When the backend introduces a breaking change, it SHALL appear as a new versioned prefix (e.g. `/api/v2`) with a deprecation window for the previous version, per the backend's versioning rule. The app SHALL adopt a new major prefix only through an explicit change, and SHALL NOT mix major versions within one release.

#### Scenario: Additive backend change needs no app release
- **WHEN** the backend adds a non-breaking endpoint or field within `/api/v1`
- **THEN** the app continues to work against `/api/v1` without a forced update

#### Scenario: Breaking backend change is adopted explicitly
- **WHEN** the backend announces a `/api/v2` with a deprecation window for v1
- **THEN** the app adopts v2 through its own change before the v1 window closes
