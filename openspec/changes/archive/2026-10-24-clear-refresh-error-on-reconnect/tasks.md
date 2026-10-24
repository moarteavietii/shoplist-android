## 1. Implementation

- [x] 1.1 In `ListViewModel`, collect the connectivity flow in `init` and call `refresh()` on each UNREACHABLE → REACHABLE transition (skip the initial emission)
- [x] 1.2 Add a unit test: refresh failure while offline sets the error, transition to REACHABLE re-runs refresh, error clears on success (fake connectivity + fake repository)
- [x] 1.3 Add a unit test: transition to REACHABLE with no prior failure does not duplicate the initial `LaunchedEffect` refresh (no double refetch assertion)

## 2. Verification

- [x] 2.1 `./gradlew test` passes with the new tests
- [x] 2.2 Emulator smoke check: trigger the offline error, restore the tunnel, confirm the error clears without manual dismissal
