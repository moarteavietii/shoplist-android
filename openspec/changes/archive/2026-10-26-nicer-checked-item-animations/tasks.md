## 1. Row-level animation rework

- [x] 1.1 In `ItemRow.kt`, replace the exit spec with `fadeOut + shrinkVertically + slideOutHorizontally(24dp)` using a ~200ms accelerating tween; replace the enter spec with `fadeIn + expandVertically` using a ~260ms decelerating tween
- [x] 1.2 Add a `staggerDelayMillis: Long = 0` parameter to `ItemRow` and apply it as the start delay on both enter and exit specs
- [x] 1.3 In `ListScreen.kt`, compute each row's stagger delay from its index among checked items and the checked count (`min(40, 250 / checkedCount) × checkedIndex`), and pass it to `ItemRow`; single-item check-off gets zero delay

## 2. Verification

- [x] 2.1 Run `./gradlew test` and confirm no regressions
- [x] 2.2 Device smoke test: hide/re-show with a small checked count (peel visible, one motion), a large count (compressed stagger, brief total), and single check-off while hiding (no stagger)
