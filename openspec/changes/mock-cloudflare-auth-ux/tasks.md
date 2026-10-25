## 1. Build configuration

- [x] 1.1 Read `mockCloudflare` from gitignored `local.properties` in `app/build.gradle.kts` and expose it as `BuildConfig.MOCK_CLOUDFLARE` (Boolean, default false)
- [x] 1.2 Define the `MockCloudflareGate` interface in the main source set (pass-through contract) and bind a pass-through no-op in `app/src/release/`

## 2. Mock Cloudflare gate (debug)

- [x] 2.1 Implement `MockCloudflareGate` in `app/src/debug/`: when `BuildConfig.MOCK_CLOUDFLARE` is on, inject stored credentials as CF Access headers and validate against the fake pair (`dev-client` / `dev-secret`); match → `chain.proceed`, mismatch → synthesized 401 with no network call; when the flag is off, pass through untouched
- [x] 2.2 Bind the debug implementation in a debug Hilt module and register the new interceptor as the innermost interceptor in `NetworkModule` (after `AuthResponseInterceptor` in the chain)
- [x] 2.3 Write unit tests for the gate: fake pair passes through, wrong pair yields synthesized 401, flag off yields untouched pass-through

## 3. Probe-before-save in the credential entry flow

- [x] 3.1 Add a health-probe dependency to `CredentialEntryViewModel` and make `submit()` verify credentials via `GET /health` on the IO dispatcher before saving; map 2xx → save+navigate, 401/403 → "Invalid credentials", `IOException` → "Can't reach the server" (nothing stored on failure)
- [x] 3.2 Render the probe error states in `CredentialEntryScreen` (danger-colored message, distinct connectivity wording)
- [x] 3.3 Write ViewModel tests (hand-written fakes, Turbine): valid probe saves and emits `saved`, rejected probe shows invalid-credentials error and stores nothing, unreachable probe shows connectivity error

## 4. Rejected-session feedback

- [x] 4.1 Add a rejection-banner state to `CredentialEntryViewModel` driven by `CredentialsRejected` (distinct from probe errors) and render it above the fields in `CredentialEntryScreen`
- [x] 4.2 Write a ViewModel test: `CredentialsRejected` event produces the rejection banner state

## 5. Verification

- [x] 5.1 Run `./gradlew test` and `./gradlew lint`; fix any regressions
- [x] 5.2 Device smoke test with `mockCloudflare=true` + `adb reverse`: valid login → list screen; wrong credentials → on-page invalid-credentials error; "revoked" session → bounce-back with rejection banner; flag-off pass → behavior unchanged; user evaluates page visuals
