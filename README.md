# Shoplist Android

A native Android shopping list app.

## Tech Stack

- **Platform:** Android (minSdk 35, target/compile SDK 37)
- **Build system:** Gradle with Kotlin DSL, Android Gradle Plugin via version catalog (`gradle/libs.versions.toml`)
- **UI:** Jetpack Compose with a custom design language (token-based theming; Material 3 only as an unthemed component toolkit)
- **Testing:** JUnit4 (unit), AndroidX Test + Espresso (instrumented)

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

## Development Workflow

This project is managed with [OpenSpec](https://github.com/Fission-AI/OpenSpec) using the `spec-driven` schema. Feature work follows the flow: create a branch → propose a change (`/opsx-propose`) → implement (`/opsx-apply`) → archive with spec sync (`/opsx-archive`). See `AGENTS.md` for the full conventions.
