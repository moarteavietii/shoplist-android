## Why

The health-check path (`GET /health` → `HealthRepository` → `HealthViewModel` → `HealthScreen`) is dead code in production: `MainActivity` shows only the credential entry screen or the list screen, and `HealthScreen` has no entry point anywhere. Worse, on the production deployment `/health` is served by the web frontend's SPA fallback and returns HTML, so the path would deterministically fail if it were ever wired back in. The credential probe already proves end-to-end connectivity more honestly (it targets the gate-protected `GET /api/v1/shops`), so the health-check capability is redundant.

## What Changes

- **BREAKING** Remove the health-check capability from the app:
  - Remove `HealthApi.getHealth()` and `HealthResponse` (the `probeShops()` endpoint stays — it is the credential probe).
  - Remove `HealthRepository`, `HealthViewModel`, `HealthScreen`, and their tests.
- Update the `app-scaffold` spec: the networking-stack requirement no longer mandates a health-check API, and the "Health status is visible in the UI" requirement is removed.
- Update the `api-contract-adoption` spec: the versioned-prefix rule no longer carves out an unversioned `/health` probe.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `app-scaffold`: Drop the health-check API mandate from the networking-stack requirement (remove its two scenarios) and remove the "Health status is visible in the UI" requirement entirely.
- `api-contract-adoption`: Remove the `/health` exception from the versioned-prefix requirement and its "Health probe stays unversioned" scenario.

## Impact

- Code: `app/src/main/java/eu/pascariu/shoppinglist/data/network/HealthApi.kt` (keep interface, drop `getHealth`/`HealthResponse`), `data/health/HealthRepository.kt`, `ui/health/HealthScreen.kt`, `ui/health/HealthViewModel.kt`, `NetworkModule.kt` (provider stays — `HealthApi` still backs the probe).
- Tests: `HealthRepositoryTest`, `HealthViewModelTest`, and the `FakeHealthApi` health-related members in `testutil/TestFixtures.kt`.
- Specs: `openspec/specs/app-scaffold/spec.md`, `openspec/specs/api-contract-adoption/spec.md`.
- No backend changes; no user-visible behavior change (the path was already unreachable).
