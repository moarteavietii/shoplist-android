## Why

The repo is an empty AppCompat/XML scaffold with no feature code, and the migration plan calls for replacing the web frontend with a native Compose client. The app needs its real foundation — UI toolkit, DI, networking, auth, and server configuration — before any feature work (data layer, SSE, list screen) can start.

## What Changes

- Replace the AppCompat/XML scaffold with **Jetpack Compose** as the UI toolkit, styled by a **custom design language** (chromeless / borderless / LED-dot aesthetic ported from the web app; Material 3 used only as an unthemed component toolkit where convenient)
- Add **Hilt** for dependency injection
- Add **Retrofit + OkHttp + kotlinx.serialization** for the REST layer, with a health-check call to `GET /health`
- Add **Cloudflare Access service-token auth**: `CF-Access-Client-Id` / `CF-Access-Client-Secret` headers on every request, encrypted storage (EncryptedSharedPreferences + Keystore), QR/manual token entry screen, 401/403 → token re-entry flow
- Add a **build-time auth dev bypass**: debug builds use a no-op auth interceptor (the local dev backend sits outside Cloudflare Access); the switch is a Hilt-provided interceptor per build type, never scattered conditionals
- Configure **base URLs**: production `https://shoppinglist.pascariu.eu`; dev builds point at `http://192.168.0.141:3000` over LAN with a cleartext-HTTP exception for the dev host only
- Remove AppCompat theme/XML layout dependencies that Compose replaces

## Capabilities

### New Capabilities
- `app-scaffold`: Compose application shell — custom design language foundation, Hilt wiring, Retrofit/OkHttp/kotlinx.serialization networking stack, base-URL configuration per build type, health-check call, and the no-title / bottom-anchored layout contract
- `cloudflare-auth`: Cloudflare Access service-token authentication — header injection, encrypted token storage, QR/manual entry screen, 401/403 re-entry flow, and the debug build-time bypass

### Modified Capabilities

(none — no main specs exist yet in `openspec/specs/`)

## Impact

- **Code:** `app/` module — `build.gradle.kts`, `AndroidManifest.xml`, all of `app/src/main` (replaces AppCompat activity/theme with Compose + Hilt application class)
- **Dependencies:** new — Compose BOM, Hilt, Retrofit, OkHttp (incl. EventSource-ready core), kotlinx-serialization, security-crypto, QR-scanning library; all versions via `gradle/libs.versions.toml`
- **Build config:** debug builds get a network-security-config cleartext exception for the dev host; release builds stay HTTPS-only
- **APIs:** consumes `GET /health` (existing backend; no backend changes)
- **Systems:** none beyond the app; backend reused as-is
