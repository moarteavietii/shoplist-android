## 1. Token scale extension

- [x] 1.1 Add `micro`, `chip`, `stepperKey`, and `stepperValue` fields to `ShoplistTypography` with values matching the current ad-hoc styles (10sp Bold, 13sp SemiBold, 15sp Bold, 17sp Bold — all Default family)
- [x] 1.2 Populate the new tokens in `ShoplistTypographyDefault` in `ShoplistTheme.kt`

## 2. Call-site migration

- [x] 2.1 SwitcherBadge in QuickMenu.kt: replace `typography.ledDot.copy(fontFamily = Default, fontWeight = Bold, fontSize = 10.sp, lineHeight = 10.sp)` with `typography.micro`
- [x] 2.2 Drawer count and shop badge in ViewDrawer.kt: replace `label/ledDot.copy(..., fontSize = 10.sp)` overrides with `typography.micro`
- [x] 2.3 "Temporary" chip and ChecksCountPill in AddItemModal.kt: replace `label.copy(..., fontSize = 13.sp)` overrides with `typography.chip`
- [x] 2.4 Stepper keys in AddItemModal.kt: replace the raw `TextStyle(fontFamily = Default, fontWeight = Bold, fontSize = 15.sp)` with `typography.stepperKey`
- [x] 2.5 Stepper quantity in AddItemModal.kt: replace `body.copy(fontWeight = Bold, fontSize = 17.sp)` with `typography.stepperValue`

## 3. Verification

- [x] 3.1 Repo-wide search confirms no component-level `fontSize = N.sp` overrides remain outside `ShoplistTheme.kt`
- [x] 3.2 Run `./gradlew test` and confirm the full unit suite passes
- [x] 3.3 Build `./gradlew assembleDebug` successfully
