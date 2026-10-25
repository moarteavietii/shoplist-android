## Why

The app's modals, buttons, checkboxes, and background watermark don't yet follow a coherent visual language: modals are flat (no shadow), action buttons are bare text with no edges, the unchecked checkbox is a filled grey circle that reads as "on", and the shop watermark is smaller and more present than the web app's. The web app already embodies the intended design language — physical panels, edged controls, fill-vs-outline state, receding chrome — and the Android app should follow the same rules so every new element can be judged against them.

## What Changes

- **Modals get physical presence**: the add-item, edit-item, and shop modals render with a drop shadow (matching the QuickMenu's elevation treatment) and hairline separators between sections, ported from the web modal's `--shadow-modal` treatment.
- **Action buttons get edges**: modal action buttons (Save, Delete, Discard/Close) render as bordered, minimum-44dp-tall controls instead of bare text. Save = filled accent, Delete = danger outline, Discard/Close = quiet bordered. Disabled state = 50% opacity.
- **Checkbox redesign**: the item-row checkbox becomes a bordered rounded-square ("LED square") — unchecked = hollow with visible border, checked = LED-on fill with checkmark. Border visible in both states. The same visual is reused for the visibility checklist rows in the item modals.
- **Visibility rows become checkbox-only touch targets**: in the item modals, tapping a visibility row's text no longer toggles it; only touching the checkbox toggles, matching the item list's behavior.
- **Quantity stepper moves next to the title**: in the item modals, the quantity stepper renders in an inset pill well on the right of the name field (web layout), replacing the current separate row.
- **Watermark matches web sizing**: the shop watermark text renders at web scale (48–80sp range, bold, opacity 0.04) instead of the current 34sp Light at 0.07.
- **Design rules documented**: the four extracted rules (panels cast shadows, controls have edges, state = fill vs. outline, chrome recedes) are captured in design.md as the reference for future elements.

## Capabilities

### New Capabilities

- `visual-language`: the app's shared visual rules for panels, controls, state, and chrome — the four rules plus the concrete treatments (shadow, borders, LED-square checkbox, watermark) that new UI elements SHALL follow.

### Modified Capabilities

- `item-checkbox`: checkbox visual changes from circle to bordered rounded-square (LED square); border visible in both states; unchecked is hollow. Tap-target, animation, and accessibility requirements carry over unchanged.
- `item-edit-delete`: edit modal layout changes — quantity stepper right of the name field, hairline separators, bordered action buttons, checkbox-only visibility rows.
- `quick-add`: add-item modal gets the same layout changes (stepper placement, separators, bordered buttons, checkbox-only visibility rows) as the edit modal.
- `shops-management`: shop modal gets the drop shadow and bordered action buttons.
- `theme-system`: no palette changes; the watermark treatment (size/weight/opacity) is specified as part of the visual language rather than per-theme palette values.

## Impact

- **Code**: `ui/list/AddItemModal.kt`, `ui/list/ShopModal.kt`, `ui/list/ItemRow.kt` (checkbox), `ui/list/ListScreen.kt` (watermark + modal host modifiers), `ui/theme/ShoplistTypography.kt` (display style), possibly a new shared `ui/theme/` or `ui/common/` composable for the LED-square checkbox and bordered button.
- **Specs**: one new capability (`visual-language`) plus deltas to `item-checkbox`, `item-edit-delete`, `quick-add`, `shops-management`, `theme-system`.
- **No backend/API changes.** No new dependencies. No data-model changes. Existing unit tests for the touched components will need updating (checkbox test tags and semantics stay stable).
