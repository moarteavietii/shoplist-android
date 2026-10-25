## Context

The app's only build-type switch is the `AuthInterceptor` Hilt binding: debug binds `NoOpAuthInterceptor`, release binds `CloudflareAccessInterceptor` (header injection from `TokenStore`). A shared `AuthResponseInterceptor` sits on every client and turns 401/403 into `tokenStore.clear()` + `AuthEventBus.emit(CredentialsRejected)`; `MainActivity` swaps `ListScreen` for `CredentialEntryScreen` on that event. The backend never emits 401/403 — Cloudflare gates in front of it and ignores nothing: the backend itself doesn't care about the headers. So the rejection path can only be exercised locally by mocking Cloudflare. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- Exercise the production rejection path (401 → clear → event → re-entry) on-device without touching the backend.
- Give the login page real error handling: invalid credentials, unreachable server, and rejected-session feedback.
- Keep the mock strictly opt-in and impossible to leak into release.

**Non-Goals:**

- No backend or Cloudflare configuration changes; no new API endpoints.
- No redesign of the login page visuals beyond error rendering — the user judges aesthetics during the final smoke test.
- No persistence of the mock state at runtime; it is a build-time flag only.

## Decisions

### 1. Mock Cloudflare as an innermost interceptor, not a copy of `CloudflareAccessInterceptor`

OkHttp interceptors are an onion: an interceptor that short-circuits (returns a response without `chain.proceed()`) prevents all outer interceptors from seeing the exchange. If the mock copied `CloudflareAccessInterceptor`'s position (outermost), a synthesized 401 would bypass `AuthResponseInterceptor` entirely — no `tokenStore.clear()`, no `CredentialsRejected` event, no re-entry. The mock therefore gets its own interface and is bound as the **innermost** interceptor, so its synthesized 401 bubbles back out through the real response interceptor and the production rejection path runs for real.

Alternative considered: making the mock implement `AuthInterceptor` in the debug source set. Rejected — it would replace `NoOpAuthInterceptor` wholesale, conflating "no auth" with "mocked auth", and the outermost/innermost distinction would be lost.

### 2. Fake credential pair hardcoded in the debug source set

`dev-client` / `dev-secret`, defined only in `app/src/debug/`. The mock compares the request's `CF-Access-Client-Id`/`-Secret` headers (injected by the real `CloudflareAccessInterceptor`... which is release-only — so in debug the mock itself injects the stored credentials into the probe request before validating, keeping the flow identical to production where headers are injected then validated by Cloudflare).

Alternative considered: reading the pair from `local.properties` too. Rejected — two config values to keep in sync for no benefit; the pair is a test fixture, not configuration.

### 3. Enable flag: `mockCloudflare=true` in gitignored `local.properties` → `BuildConfig` field

`app/build.gradle.kts` reads `local.properties` (already the pattern for `devBaseUrl`) and exposes `BuildConfig.MOCK_CLOUDFLARE` (Boolean, default false). The debug Hilt module consults it to decide whether the mock interceptor validates or passes through. Release builds never read the flag — their binding is a hard-coded pass-through.

Alternative considered: flipping the `DebugAuthModule` binding per session. Rejected — requires code edits each time and risks committing an activated mock.

### 4. Probe-before-save via the existing health endpoint

`CredentialEntryViewModel.submit()` first performs `GET /health` through the normal OkHttp client (same interceptor chain, so the mock participates). Outcomes map to UI state:

| Outcome | UI |
|---|---|
| 2xx | save encrypted, navigate forward |
| 401/403 | "Invalid credentials" (danger color), nothing stored |
| `IOException` | "Can't reach the server" (distinct message), nothing stored |

Note: `AuthResponseInterceptor` clears the store on 401/403 — harmless during the probe since nothing was saved yet, and it emits `CredentialsRejected`, which the ViewModel must not confuse with the probe result; the probe's own outcome drives the error message.

Alternative considered: verifying only on bounce-back (status quo). Rejected — the user learns credentials are bad only after navigating away and back.

### 5. Rejection feedback via a distinct event payload

`MainActivity` already collects `CredentialsRejected`. The re-entry screen needs to know *why* it's showing. The ViewModel gains a `rejectionMessage` state set when `CredentialsRejected` arrives while the screen is visible (or on init after bounce-back), rendered as a banner above the fields. The probe-failure error and the rejection banner are separate states so "you typed something wrong" and "your session expired" render differently.

## Risks / Trade-offs

- [Synthesized 401s could mask real backend behavior] → The mock only activates with the flag on, in debug only; the smoke test checklist includes a flag-off pass to confirm normal behavior.
- [`AuthResponseInterceptor` fires during the probe, emitting a spurious `CredentialsRejected`] → The ViewModel treats the probe outcome as authoritative for the error message; the event only drives the banner, which the probe path overrides.
- [Fake credentials committed accidentally] → The pair lives in the debug source set as an obvious test fixture; it grants access to nothing (backend ignores headers; Cloudflare is not involved in dev).
- [EncryptedSharedPreferences latency on first probe] → Pre-existing behavior; the probe runs on the IO dispatcher like the existing save path.

## Migration Plan

No data migration. Rollback = remove the flag from `local.properties` (mock inert) or revert the change; release behavior is untouched throughout.
