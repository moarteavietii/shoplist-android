## 1. Checkbox composable

- [x] 1.1 Replace `LedCheckbox` in `ItemRow.kt` with a circle checkbox composable: `ledOff` circle unchecked, `ledOn` fill with a drawn checkmark when checked, colors from `LocalShoplistColors`
- [x] 1.2 Add the check animation: `animateColorAsState` crossfade plus checkmark stroke draw-in via animated path progress (`PathMeasure.getSegment`), both `tween(200)`, running in both directions
- [x] 1.3 Wrap the checkbox in a 48dp touch target box with `toggleable`, carrying `Role.Checkbox` and `toggleableState` semantics, visual circle centered

## 2. Row interaction restructure

- [x] 2.1 Remove `onClick` and the checkbox role/toggleable-state semantics from the row body's `combinedClickable`; keep `onLongClick` for the edit modal
- [x] 2.2 Verify row spacing stays compact with the 48dp target (adjust visual padding if rows grow) and the drag handle layout is unaffected

## 3. Tests and verification

- [x] 3.1 Update row interaction tests for the narrowed tap target (checkbox toggles, row-body tap inert, long-press opens edit)
- [x] 3.2 Run `./gradlew test` and `./gradlew lint` and fix any failures
