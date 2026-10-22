# Agent Notes: shoplist-android

## Project State

This is an **OpenSpec-managed repo** for a native Android shopping list app. The `:app` module hosts a Jetpack Compose app with a custom design language (token-based theming via CompositionLocals), Hilt DI, Retrofit/OkHttp/kotlinx-serialization networking, and Cloudflare Access service-token auth (no-op in debug builds). Feature behavior lives in the capability specs under `openspec/specs/` — consult those instead of this file for feature-level contracts.

## Architecture & Stack

- **Platform:** Android (minSdk 35, target/compile SDK 37)
- **Build system:** Gradle with Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin via version catalog
- **Module layout:** Single `:app` module
- **UI:** Jetpack Compose with a custom design language (token-based theming via CompositionLocals; Material 3 only as an unthemed component toolkit)
- **Testing:** JUnit4 (unit), AndroidX Test + Espresso (instrumented)

## Safe Agent Commands

These Gradle tasks are the only shell commands agents should need for verification. Using them avoids permission prompts. Do not improvise equivalents; use the wrapper.

| Command | What it does |
|---------|--------------|
| `./gradlew assembleDebug` | Build the debug APK |
| `./gradlew test` | Run all unit tests |
| `./gradlew connectedDebugAndroidTest` | Run instrumented tests on a connected device/emulator |
| `./gradlew lint` | Run Android lint |

Notes:

- If an agent genuinely needs a command outside this list, surface it to the user instead of improvising — do not widen the allow list without confirmation.

## Workflow: OpenSpec

This repo uses the `spec-driven` OpenSpec schema. Use the provided commands rather than running the CLI directly:

- `/opsx-propose <change-name>` — create proposal, design, and tasks artifacts.
- `/opsx-apply <change-name>` — implement tasks from a change.
- `/opsx-archive <change-name>` — archive a completed change (optionally sync delta specs).
- `/opsx-sync <change-name>` — sync delta specs to main specs without archiving.
- `/opsx-explore <topic>` — thinking mode only; never writes code.

The matching skills live under `.coda/skills/openspec-*` if you need the full steps.

## Migration Conventions (until migration is complete)

The Android app replaces the shoppinglist web frontend; the backend REST + SSE API is reused as-is. `docs/MIGRATION_PLAN.md` is the authoritative reference for the migration — read it before any migration work. Until the migration is done:

- **Consult `docs/MIGRATION_PLAN.md` first.** It holds the discovered API/SSE/data-model contracts, the decisions log, and the traceability matrix. Do not re-derive contracts from the web repo when the plan already records them; go to the web repo only for details the plan lacks.
- **Follow the migration workflow in the plan** (§5): pick a roadmap item from `docs/ROADMAP.md` → branch → `/opsx-propose` (consulting the web specs listed in the traceability matrix as source material — adapt, don't copy) → `/opsx-apply` → `/opsx-archive` → update the matrix status in `docs/MIGRATION_PLAN.md`.
- **Never propose or implement backend changes in this repo.** Specs marked `n/a` in the matrix (database, deploy, dev-env) belong to the web repo.
- **Respect the decisions log.** The plan's decisions table (design language, auth + dev bypass, minSdk 35, no pull-to-refresh, backgrounded = no network, base URLs, no Room initially) is settled — do not relitigate or contradict it in proposals. If a decision must change, surface it to the user first and update the log.
- **After each `/opsx-archive`,** update the traceability matrix row(s) for the archived change and check whether `docs/ROADMAP.md` needs its item status adjusted.

## Repo Layout

- `docs/ROADMAP.md` — ordered, high-level plan for the app. Consult it before proposing changes; new feature work should map to a roadmap item.
- `docs/MIGRATION_PLAN.md` — migration reference: discovered contracts, decisions log, traceability matrix (web specs → Android work).
- `openspec/config.yaml` — OpenSpec config (currently minimal).
- `openspec/specs/` — main capability specs.
- `openspec/changes/` — active changes; completed changes move to `openspec/changes/archive/`.
- `gradle/libs.versions.toml` — version catalog; all dependency versions live here, never inline in build files.
- `.coda/commands/` — custom slash commands.
- `.coda/skills/` — full skill definitions.

## Guardrails for Agents

- **Teach while building.** The user is new to Android development and wants to learn the ins and outs of the platform, not just ship an app. Whenever you introduce or touch anything Android-specific (Gradle configuration, manifest, lifecycle, components, SDK tooling, etc.), briefly explain the concept and why it works the way it does — before or alongside the implementation. Do not assume prior Android knowledge.
- **Boyscout rule: leave the codebase cleaner than you found it.** When touching a file or area, fix adjacent issues you notice (stale comments, dead code, obvious bugs, missing types) — but keep it proportionate: small, related cleanups only, never unrelated refactors, and never mix them into a spec change without calling them out.
- **Do not write application code until a change exists.** Use `/opsx-propose` first so work is tracked in tasks.
- **Respect `actionContext` from `openspec status --json`.** Do not assume fixed paths; derive `changeRoot`, `planningHome`, and artifact paths from the CLI output.
- **Explore mode is read-only.** `/opsx-explore` must not produce code or mutate files beyond OpenSpec artifacts.
- **Archive syncs specs intelligently.** Delta specs use ADDED/MODIFIED/REMOVED/RENAMED sections; merge partial changes rather than replacing whole spec files.
- **KISS: keep it simple.** Prefer the smallest change that satisfies the spec; avoid premature abstraction.
- **Always start from a new branch for spec work.** The flow for every change is: create a new branch → create the spec/change (`/opsx-propose`) → implementation (`/opsx-apply`) → spec archival (`/opsx-archive`) → pull request. Never work on a spec or its implementation on `main`.
- **TDD: always write tests for the code you're about to write.** Run the tests after an implementation is done (all tasks are done).
- **Avoid code comments.** Write self-explanatory code instead. Only add a comment when it is truly necessary (non-obvious intent, workaround, or gotcha), and keep it to one short line.
- **After each `/opsx-archive` command,** check `AGENTS.md` and `README.md` against the just-archived change and its synced specs. Decide whether either file needs updating to reflect the new project state, capabilities, or workflow. If changes are warranted, present the proposed edits to the user and wait for confirmation before applying them.

## Project Particularities

- **Per-build-type bindings: only the auth interceptor differs.** Shared bindings (`TokenStore`, `AuthEventBus`, networking) live in the main-source-set `NetworkModule` — both build types need them. The `AuthInterceptor` binding is the only build-type switch: `app/src/debug/.../DebugAuthModule.kt` binds the no-op, `app/src/release/.../ReleaseAuthModule.kt` binds the real one. When adding new per-build-type behavior, follow this split; do not move shared bindings into a build-type source set (that broke the debug build once already).
- **androidx opt-in APIs need the androidx `@OptIn`, not Kotlin's.** For CameraX-style `@Experimental*` APIs, annotate with `@androidx.annotation.OptIn(Experimental…::class)` on the calling declaration. Android lint's `UnsafeOptInUsageError` check does not recognize Kotlin's own `@OptIn`, and the opt-in requirement propagates to every call site (a lambda calling an annotated function must itself be covered).
- **Testing convention: hand-written fakes, no mocking framework.** Unit tests use small hand-written fakes (`FakeTokenStore`, `FakeHealthApi`) and plain JUnit4; HTTP behavior is tested with MockWebServer, flows with Turbine. Do not introduce MockK/Mockito.
