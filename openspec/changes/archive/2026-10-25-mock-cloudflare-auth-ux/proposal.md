## Why

The production login page (`CredentialEntryScreen`) has never been exercised end-to-end: debug builds bypass auth entirely, and the rejection path is silent — when the server rejects credentials, the app bounces back to the login page with no explanation. Before production we need to verify the page works, handle authentication errors with clear on-page feedback, and let the user judge the visuals. Since the backend never emits 401/403 (Cloudflare gates in front of it), the only way to exercise the rejection path locally is to mock Cloudflare itself.

## What Changes

- Add a **mock Cloudflare interceptor** (debug builds only) that validates credentials against a hardcoded fake pair (`dev-client` / `dev-secret`): match → proceed to the backend; mismatch → synthesized 401 without a network call. It is bound as the innermost interceptor so the real `AuthResponseInterceptor` (clear + `CredentialsRejected` event) still runs — exercising the production rejection path for real.
- Gate the mock behind a `mockCloudflare` flag in gitignored `local.properties`, exposed as a `BuildConfig` field. Debug builds behave exactly as today unless the flag is set; release builds are untouched.
- Add **server-side verification before saving**: `submit()` probes the backend (existing health endpoint) with the entered credentials through the normal client. 401/403 → "Invalid credentials" on the page; network failure → "Can't reach the server" (distinct message); success → save and proceed.
- Improve **rejection UX**: when `CredentialsRejected` fires, the login page now shows an explicit "credentials rejected / session expired" message instead of silently resetting.
- Run a **device smoke test** with the mock enabled to verify the full flow (valid login, invalid login, session-expiry bounce-back) and let the user evaluate the page's look.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `cloudflare-auth`: add requirements for a debug-only mock Cloudflare gate (fake credential validation, build-config gated), server-side credential verification before saving, and explicit on-page error feedback for invalid credentials, unreachable server, and rejected/expired sessions.

## Impact

- `app/src/debug/` — new `MockCloudflareInterceptor` + Hilt module binding it (debug source set only).
- `app/src/main/` — `NetworkModule` (add innermost interceptor binding), `CredentialEntryViewModel` (probe-before-save, rejection message state), `CredentialEntryScreen` (error rendering), `build.gradle.kts` (BuildConfig flag from `local.properties`).
- `app/src/release/` — pass-through no-op binding for the new interceptor interface.
- Tests: new unit tests for the mock interceptor and ViewModel probe paths; existing `AuthResponseInterceptorTest` unaffected.
- No backend or API-contract changes; the dev backend ignores auth headers.
