## 1. Build setup and dependencies

- [ ] 1.1 Add all new dependencies and plugins to `gradle/libs.versions.toml` (Compose BOM, activity-compose, Hilt + KSP, Retrofit, OkHttp, kotlinx-serialization + plugin, security-crypto, CameraX + ML Kit) and reference them from `app/build.gradle.kts` via aliases only
- [ ] 1.2 Add `buildConfigField` entries for base URLs (debug: `devBaseUrl` from gitignored `local.properties` with `http://10.0.2.2:3000` fallback, release: `https://shoppinglist.pascariu.eu`) and enable `buildConfig`
- [ ] 1.3 Add debug-source-set `network_security_config.xml` allowing cleartext only for the dev host; keep release HTTPS-only
- [ ] 1.4 Verify the build compiles with `./gradlew assembleDebug`

## 2. Compose shell and design language

- [ ] 2.1 Replace the AppCompat activity with a Compose-hosting activity; remove XML layouts/themes and AppCompat dependencies that Compose replaces; update `AndroidManifest.xml`
- [ ] 2.2 Add Hilt: annotated application class, KSP setup, manifest update
- [ ] 2.3 Create the design-language token foundation (`ShoplistColors`, `ShoplistTypography`, `ShoplistShapes`, `ShoplistTheme` with CompositionLocals) expressing the chromeless / borderless / LED-dot aesthetic
- [ ] 2.4 Verify the app launches with the Compose shell and no title/header bar

## 3. Networking stack

- [ ] 3.1 Add the Hilt networking module: OkHttp client, Retrofit instance wired to the per-build-type base URL from `BuildConfig`
- [ ] 3.2 Add `HealthApi` (Retrofit `GET health`) with a kotlinx-serializable response DTO and error handling for unreachable backend
- [ ] 3.3 Add `HealthRepository` exposing health status as a UI-ready state

## 4. Cloudflare Access auth

- [ ] 4.1 Define `AuthInterceptor` interface with `CloudflareAccessInterceptor` (header injection from token store) and `NoOpAuthInterceptor` implementations
- [ ] 4.2 Add `TokenStore` backed by EncryptedSharedPreferences + Keystore master key
- [ ] 4.3 Wire the Hilt binding so debug builds provide the no-op interceptor and release builds the real one
- [ ] 4.4 Add a response interceptor that emits an auth event on 401/403 for the UI to observe
- [ ] 4.5 Build the credential entry screen: manual id/secret form and QR scan (CameraX + ML Kit), storing via `TokenStore`
- [ ] 4.6 Navigate to the credential entry screen on auth events; proceed to the main screen on success

## 5. Health-check vertical slice

- [ ] 5.1 Build the main screen showing backend health status (LED-dot green/red + text) fed by `HealthRepository`
- [ ] 5.2 Verify end-to-end against the dev backend: debug build (no auth headers, LAN URL) shows healthy status; with backend stopped, shows the error state without crashing

## 6. Tests and verification

- [ ] 6.1 Unit tests for `CloudflareAccessInterceptor` header injection and the 401/403 response interceptor (fake `TokenStore`)
- [ ] 6.2 Unit tests for `HealthRepository` success/error mapping
- [ ] 6.3 Run `./gradlew test`, `./gradlew lint`, and `./gradlew assembleDebug`; fix any findings
