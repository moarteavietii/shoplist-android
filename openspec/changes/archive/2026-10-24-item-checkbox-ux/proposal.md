## Why

Checking an item currently happens on a tap anywhere in the row, so users accidentally check items off while scrolling or interacting with the list. The LED dot is purely decorative with no touch target of its own, and the check-undo spec's scenarios already describe tapping "the checkbox" — the code is out of sync with the intended UX.

## What Changes

- Replace the decorative LED dot in item rows with a themed circle checkbox: `ledOff` circle when unchecked, `ledOn` fill with a drawn checkmark when checked.
- Add check animation: color crossfade plus checkmark stroke draw-in, both at `tween(200)` (matching the existing chevron timing in the widget stack).
- Move the check/uncheck tap handler from the row body to the checkbox only, wrapped in a 48dp touch target carrying `Role.Checkbox` and `toggleableState` semantics.
- Row body taps become inert; long-press continues to open the item edit modal.

## Capabilities

### New Capabilities

- `item-checkbox`: The item row checkbox — visual design (themed circle with checkmark), check animation, checkbox-only tap target with accessibility semantics, and inert row-body taps.

### Modified Capabilities

- `check-undo`: The check/uncheck trigger narrows from "tap anywhere on the row" to "tap the checkbox"; requirement scenarios are updated to name the checkbox as the sole trigger.

## Impact

- `app/src/main/java/eu/pascariu/shoppinglist/ui/list/ItemRow.kt` — row interaction restructure and new checkbox composable.
- No palette or theme-token changes: reuses existing `ledOn`/`ledOff` tokens, so `theme-system` and `ShoplistPalettes.kt` are untouched.
- Accessibility semantics move from the row to the checkbox; the `list-screen` accessibility requirement remains satisfied.
- Unit tests: `ListViewModelTest` and any row-level interaction tests may need updates for the narrowed tap target.
