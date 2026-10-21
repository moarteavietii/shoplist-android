## Context

The repo is a single `:app` module built from the Android Studio "Empty Activity" template: AppCompat + XML theme, no feature code. The backend (Elysia REST + SSE) is reused as-is; contracts are recorded in `docs/MIGRATION_PLAN.md` §3. Auth decisions (service tokens, dev bypass, base URLs, minSdk 35) are settled in the decisions log — this design only turns them into an implementation approach. The `helloworldcosmin` testbed project is the reference for the Cloudflare Access auth pattern.

## Goals / Non-Goals

**Goals:**
- Compose-first app shell with the custom design language as a first-class token set (color / typography / shape), so later screens never reach for Material theming
- A Hilt graph that already anticipates the roadmap: networking module, auth module, per-build-type base URL
- Auth as a swappable interceptor: real service-token injection vs. no-op, chosen by build type
- Health check wired end-to-end (API → repository → UI) as the first vertical slice proving the whole stack

**Non-Goals:**
- SSE / realtime sync (roadmap 3), repositories and DTOs beyond the health check (roadmap 2)
- Actual list UI, design-language polish beyond the token foundation (roadmap 4+)
- Offline support, Room, DataStore persistence (roadmap 8; device id persistence arrives with the data layer)
- Backend changes of any kind

## Decisions

### D1: Compose with a design-language object graph, not a Material theme
Define `ShoplistTheme` built on our own token objects (`ShoplistColors`, `ShoplistTypography`, `ShoplistShapes`) exposed via CompositionLocals. Material 3 stays on the classpath as an unthemed toolkit (dialogs, ripple) but receives no Material color scheme. *Alternative considered:* full custom component set with no M3 — rejected for now (KISS); the decisions log keeps it as a future option.

### D2: Auth = OkHttp Interceptor behind an interface, selected by Hilt build type
`AuthInterceptor` interface with two implementations: `CloudflareAccessInterceptor` (reads credentials from encrypted storage, injects `CF-Access-Client-Id`/`CF-Access-Client-Secret`) and `NoOpAuthInterceptor`. A Hilt `@Provides` in a debug source set (or `@Named` binding flipped by `buildConfigField`) selects the no-op for debug, the real one for release. 401/403 handling lives in an `AuthInterceptor`-adjacent response interceptor that emits an auth-event the UI layer observes to navigate to re-entry. *Alternative considered:* checking build type inside one interceptor — rejected; the decisions log demands a build-time switch, not scattered conditionals.

### D3: Encrypted credentials via EncryptedSharedPreferences + Keystore
`security-crypto`'s `EncryptedSharedPreferences` with a Keystore master key, wrapped in a small `TokenStore` interface so tests can fake it. QR entry uses a camera scanner library (ML Kit barcode scanning via CameraX); manual entry is a plain form. QR payload format: a URL-encoded pair (`id=…&secret=…`) — matches the testbed convention.

### D4: Base URL per build type via `buildConfigField`
`PROD_BASE_URL` is fixed at `https://shoppinglist.pascariu.eu`; the debug base URL is read from the `devBaseUrl` property in the gitignored `local.properties` (machine-local config, so the LAN IP never lands in git), falling back to `http://10.0.2.2:3000` for the emulator. Cleartext exception: `networkSecurityConfig` in the debug source set allowing cleartext only for `192.168.0.141` and the `10.0.2.2` emulator fallback (and the Bonjour fallback hostname when verified); release source set keeps the default HTTPS-only config. *Alternative considered:* runtime base-URL switcher — rejected as scope creep.

### D5: Health check as the first vertical slice
`HealthApi` (Retrofit `@GET("health")` returning a kotlinx-serializable DTO) → `HealthRepository` → a minimal Compose screen showing status (LED-dot style: green/red dot + text). This exercises Compose + Hilt + Retrofit + serialization + error handling in one thin slice, and gives the dev-bypass and base-URL decisions something real to validate against.

### D6: Version catalog for all new dependencies
Compose BOM, activity-compose, Hilt (+ KSP), Retrofit, OkHttp, kotlinx-serialization (+ serialization plugin), security-crypto, CameraX + ML Kit, and their KSP/compiler plugins all go into `gradle/libs.versions.toml`; `build.gradle.kts` references aliases only. KSP is used over kapt for Hilt (faster, kapt is in maintenance mode).

## Risks / Trade-offs

- [Dev LAN IP is DHCP-assigned and may drift] → Verify the reservation during implementation; the Bonjour fallback (`http://<macname>.local:3000`) is a one-line `buildConfigField` change. Tracked in the decisions log as "verify".
- [`security-crypto` is effectively in maintenance] → Acceptable for now (Google's own guidance); the `TokenStore` interface isolates it so a swap to a hand-rolled Keystore scheme later touches one class.
- [ML Kit adds a sizable dependency for one QR feature] → Acceptable; it is on-device, no network. If size matters later, the entry screen can degrade to manual-only.
- [minSdk 35 means no coverage of older devices] → Deliberate (decisions log); not a risk to mitigate.
- [Cleartext dev exception could leak into release] → The exception lives only in the debug source set's `networkSecurityConfig`; lint (`./gradlew lint`) guards this.

## Migration Plan

Single-module change, no data migration. Rollback = revert the branch; the AppCompat scaffold it replaces is trivial to restore from git.

## Open Questions

- None blocking. The dev IP reservation and Bonjour hostname are flagged for verification during implementation (D4) and do not change the approach.
