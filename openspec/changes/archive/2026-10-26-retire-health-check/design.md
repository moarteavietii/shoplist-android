## Context

The health-check path (`HealthApi.getHealth()` → `HealthRepository` → `HealthViewModel` → `HealthScreen`) is unreferenced from production code: `MainActivity` renders only `CredentialEntryScreen` or `ListScreen`. The `HealthApi` interface itself is still needed — `CredentialEntryViewModel` uses `probeShops()` for the credential probe. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Remove the dead health-check code and its tests.
- Update the two specs that mandate the capability (`app-scaffold`, `api-contract-adoption`).

**Non-Goals:**
- Touching the credential probe (`probeShops`) — it is the live replacement for connectivity verification.
- Any backend change; `/health` continues to exist on the server.
- Adding a replacement diagnostics screen.

## Decisions

- **Keep the `HealthApi` interface, remove only `getHealth()` and `HealthResponse`.** The interface is the probe's home; renaming it (e.g. to `ProbeApi`) would churn imports and the Hilt provider for no behavioral gain. Alternative considered: rename to reflect its probe-only role — rejected as cosmetic churn; can be done later if the interface grows.
- **Keep the `NetworkModule` provider as-is.** It still provides `HealthApi` for the probe; no DI changes are needed.
- **Trim `FakeHealthApi` rather than delete it.** The test fake keeps `probeBehavior`/`probedClientId`/`probedClientSecret` (used by `CredentialEntryViewModelTest`) and drops the `healthBehavior` member and `getHealth()` override.
- **Spec deltas over wholesale rewrites.** `app-scaffold` keeps its networking requirement minus the health-check mandate; the "Health status is visible in the UI" requirement is REMOVED with reason/migration. `api-contract-adoption` drops the `/health` parenthetical exception and its scenario.

## Risks / Trade-offs

- [Losing a ready-made diagnostics screen] → The screen never had an entry point, so nothing is lost in practice; connectivity diagnostics remain available via the credential probe and the OpenAPI contract-verification workflow in AGENTS.md.
- [Future need for a health indicator] → Would go through a new `/opsx-propose` change; nothing in this change precludes re-adding one against a gate-safe endpoint.
