# Shoplist Android

A native Android shopping list app. Works offline: items, shops, and pending changes are persisted locally (Room) and synced to the backend on reconnect.

## Tech Stack

- **Platform:** Android (minSdk 35, target/compile SDK 37)
- **Build system:** Gradle with Kotlin DSL, Android Gradle Plugin via version catalog (`gradle/libs.versions.toml`)
- **UI:** Jetpack Compose with a custom design language (token-based theming; Material 3 only as an unthemed component toolkit)
- **Persistence:** Room (items, shops, pending-mutation queue); server is source of truth on refetch
- **Testing:** JUnit4 (unit), Robolectric (Room DAO tests), AndroidX Test + Espresso (instrumented)

## Project Structure

```text
app/                        # Android application module
  src/main/                 # App code, resources, manifest
  src/test/                 # Unit tests
  src/androidTest/          # Instrumented tests
gradle/
  libs.versions.toml        # Version catalog (all dependency versions)
openspec/
  specs/                    # Capability specs
  changes/                  # Active changes (archived ones under changes/archive/)
```

## Building and Testing

```bash
# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires a connected device or emulator)
./gradlew connectedDebugAndroidTest

# Run Android lint
./gradlew lint
```

## Running on Your Device

Prerequisites (one-time): the dev backend must be reachable on workstation port 3005, and `devBaseUrl=http://127.0.0.1:3005` set in gitignored `local.properties`.

```bash
# 0. Check the device is connected
adb devices

# 1. Re-establish the USB tunnel to the dev backend
#    (REQUIRED after every device reconnect — the rule dies with the cable)
adb reverse tcp:3005 tcp:3005

# 2. Build + install the debug app on the phone
./gradlew installDebug

# 3. Launch the app
adb shell monkey -p eu.pascariu.shoppinglist -c android.intent.category.LAUNCHER 1
```

Quick one-liner (steps 1–3 combined):

```bash
adb reverse tcp:3005 tcp:3005 && ./gradlew installDebug && adb shell monkey -p eu.pascariu.shoppinglist -c android.intent.category.LAUNCHER 1
```

Useful extras:

```bash
# Force-stop the app (clean start on next launch)
adb shell am force-stop eu.pascariu.shoppinglist

# Capture a screenshot to the project's tmp folder
adb exec-out screencap -p > .coda/tmp/screen.png

# Watch logcat for crashes (leave running while testing)
adb logcat -s AndroidRuntime:E
```

Troubleshooting:

- **"backend unreachable" (red LED)** → the `adb reverse` tunnel is gone; re-run step 1.
- **`adb: no devices/emulators found`** → reconnect the USB cable, enable file transfer mode, then re-run step 1.
- **Install conflict** → uninstall the old build first: `adb uninstall eu.pascariu.shoppinglist`

## Development Workflow

This project is managed with [OpenSpec](https://github.com/Fission-AI/OpenSpec) using the `spec-driven` schema. Feature work follows the flow: create a branch → propose a change (`/opsx-propose`) → implement (`/opsx-apply`) → archive with spec sync (`/opsx-archive`). See `AGENTS.md` for the full conventions.

The migration from the shoppinglist web frontend is **complete** — the app is at feature parity. `docs/ROADMAP.md` and `docs/MIGRATION_PLAN.md` are kept as historical references (contracts, decisions log, traceability matrix).
