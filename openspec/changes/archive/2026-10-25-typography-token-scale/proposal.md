## Why

The modal/checkbox visual refresh introduced ad-hoc typography sizes (10/13/15/17sp) that bypass the `ShoplistTypography` token scale in AddItemModal, ViewDrawer, and QuickMenu — flagged as escalated finding TDT-3 in the branch's code review. The user decided to tokenize all four sizes, extending the approved type scale rather than leaving magic values in component code.

## What Changes

- Extend `ShoplistTypography` with four new tokens carrying the exact current ad-hoc values, so the change is pixel-identical:
  - `micro` — 10sp Bold, Default family (SwitcherBadge, drawer count, shop badge)
  - `chip` — 13sp SemiBold, Default family ("Temporary" chip label, ChecksCountPill)
  - `stepperKey` — 15sp Bold, Default family (stepper +/− keys)
  - `stepperValue` — 17sp Bold, Default family (stepper quantity)
- Replace all ad-hoc `fontSize` overrides and the raw `TextStyle` in AddItemModal.kt, ViewDrawer.kt, and QuickMenu.kt with the new tokens.
- No visual change: every token carries the exact size/weight/family currently in use.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `theme-system`: add a requirement that text styling comes from the `ShoplistTypography` token scale, which now includes the micro/chip/stepper tokens; ad-hoc `fontSize` overrides in components are no longer permitted.

## Impact

- `app/src/main/java/eu/pascariu/shoppinglist/ui/theme/ShoplistTypography.kt` — four new fields.
- `app/src/main/java/eu/pascariu/shoppinglist/ui/theme/ShoplistTheme.kt` — token values in `ShoplistTypographyDefault` (typography is shared across all 9 themes; no palette edits).
- `ui/list/AddItemModal.kt`, `ui/drawer/ViewDrawer.kt`, `ui/quickmenu/QuickMenu.kt` — call sites swap overrides for tokens.
- Unit tests: existing Compose UI tests assert on rendered text, not sizes, so no test changes expected; run the full suite to confirm.
