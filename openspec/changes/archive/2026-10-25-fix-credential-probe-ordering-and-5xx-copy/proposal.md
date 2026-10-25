## Why

The diff review of `mock-cloudflare-auth-ux` escalated two design decisions on the credential entry flow, now resolved by the user: (1) the probe currently verifies credentials before saving, but the spec delta and design doc still describe the opposite ordering, and the user has confirmed probe-then-save is the desired behavior; (2) non-401/403 HTTP errors (e.g. 5xx) are surfaced as "Can't reach the server", which misleads the user into thinking the network is down when the server itself is failing.

## What Changes

- Reconcile the `cloudflare-auth` spec with the implemented probe-then-save ordering: credentials SHALL be verified against the server before any persistence, and SHALL NOT be stored when verification fails for any reason (rejection, server error, or network failure).
- Add a distinct error kind for server errors: HTTP responses other than 401/403 (e.g. 5xx) SHALL surface a "server error" message on the credential entry page, distinct from both the invalid-credentials error and the unreachable-server error.
- Document that every request to the server is Cloudflare-Access-gated (including `/health`), so a successful probe of `GET /health` is acceptable credential verification.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `cloudflare-auth`: the credential-verification requirement is restated as probe-then-save (credentials SHALL NOT be persisted before verification succeeds, including on server errors); a new requirement defines distinct on-page error classification for server errors (5xx) versus unreachable-server errors.

## Impact

- `app/src/main/java/eu/pascariu/shoppinglist/ui/auth/CredentialEntryViewModel.kt` — new `ServerError` error kind and message; probe outcome mapping for non-401/403 HTTP errors changes from `Unreachable` to `ServerError`.
- `app/src/main/java/eu/pascariu/shoppinglist/ui/auth/CredentialEntryScreen.kt` — error copy handling for the new kind (if kinds are rendered distinctly).
- `app/src/test/java/eu/pascariu/shoppinglist/ui/auth/CredentialEntryViewModelTest.kt` — updated/added tests for the 5xx → server-error mapping.
- Spec sync: `openspec/specs/cloudflare-auth/spec.md` gains the clarified verification-ordering requirement and the error-classification requirement (via `/opsx-archive` sync).
