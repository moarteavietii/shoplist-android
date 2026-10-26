## 1. Remove health-check code

- [x] 1.1 Remove `getHealth()` and `HealthResponse` from `HealthApi.kt` (keep the interface and `probeShops()`)
- [x] 1.2 Delete `data/health/HealthRepository.kt` and `ui/health/HealthViewModel.kt` and `ui/health/HealthScreen.kt`
- [x] 1.3 Trim `FakeHealthApi` in `testutil/TestFixtures.kt`: drop `healthBehavior` and the `getHealth()` override, keep the probe members

## 2. Remove health-check tests

- [x] 2.1 Delete `HealthRepositoryTest.kt` and `HealthViewModelTest.kt`
- [x] 2.2 Verify no remaining references to the removed symbols (`getHealth`, `HealthResponse`, `HealthRepository`, `HealthViewModel`, `HealthScreen`)

## 3. Verify

- [x] 3.1 `./gradlew test` passes
- [x] 3.2 `./gradlew assembleDebug` passes
