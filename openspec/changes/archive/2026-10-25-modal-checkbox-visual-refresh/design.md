## Context

The Android app already has a token-based theme system (`ShoplistColors`/`ShoplistTypography`/`ShoplistShapes` via CompositionLocals) and one component that embodies the intended physical feel: the QuickMenu, which stacks `.shadow(...)` + `.background(surface)` + `.border(...)`. The web app (`apps/frontend` in the shoppinglist repo) is the design reference: its modal uses `--shadow-modal: 0 8px 24px rgba(0,0,0,0.2)` (0.55 in dark themes), an underline name input with a pill-shaped stepper well on the same row, hairline section separators, and a three-tier button system (primary filled / danger outline / quiet). Its watermark is `clamp(3rem, 15vw, 5rem)`, weight 700, opacity 0.04.

Current Android state: modals are flat `Column`s with `background(surface, shapes.large)` and no shadow; action buttons are bare `Text` + `clickable`; the checkbox is a filled 10dp circle (grey when unchecked — reads as "on"); visibility rows toggle on any row tap; the watermark is 34sp Light at alpha 0.07.

## Goals / Non-Goals

**Goals:**

- Extract the shared visual language into reusable pieces so modals, buttons, and checkboxes stop being one-off compositions.
- Port the web modal's structure faithfully: shadow, name+stepper row, hairline separators, three-tier buttons.
- Redesign the checkbox as the "LED square": bordered rounded-square, hollow when unchecked, LED-on fill when checked.
- Make the watermark recede like the web's.

**Non-Goals:**

- No new theme tokens or palette changes (per theme-system spec; colors come from existing tokens).
- No centering of the item modals — they stay bottom sheets (web is centered, but bottom-sheet is the established mobile pattern here; only the shop modal is centered today and stays centered).
- No real backdrop blur behind panels (same decision as QuickMenu: high-alpha surface approximation).
- No changes to the Temporary toggle's chip/LED-dot treatment (it's a mode, not a checkbox).
- No backend, data-model, or dependency changes.

## Decisions

### D1: Shared composables in `ui/common/` — `LedSquareCheckbox` and `ModalButton`

The checkbox visual is needed in three places (item rows, add/edit modal visibility rows) and the button treatment in three modals. Rather than copy-pasting the Canvas drawing, extract:

- `ui/common/LedSquareCheckbox.kt` — bordered rounded-square (20dp visual, 48dp touch target parameterizable), hollow→filled states, checkmark path animation ported from the current `CircleCheckbox`.
- `ui/common/ModalButton.kt` — bordered 44dp-min button with three styles (`Primary` filled accent, `Danger` danger outline, `Quiet` border-colored outline), disabled = 0.5 opacity.

Alternative considered: keep visuals inline per file. Rejected — the whole point of this change is a reusable language; a third copy of the checkbox drawing would guarantee drift.

### D2: Checkbox color story — border token when off, LED-on when on

Unchecked: border in `colors.border`, no fill. Checked: border and fill in `colors.ledOn`, checkmark in `colors.background`. This reuses the LED identity (state color = LED-on) while fixing the "grey circle reads as on" problem. The off-state border uses the existing `border` token rather than `ledOff` — `ledOff` is what the current filled circle uses and is part of why unchecked reads ambiguous; `border` is visually a "container edge" color.

Alternative considered: keep `ledOff` for the unchecked border (purer LED metaphor). Rejected for now — can revisit if `border` reads too faint in some themes; it's a one-line change in the shared composable.

### D3: Shadow via `Modifier.shadow(elevation, shape)` at the modal host

Apply `.shadow(elevation = 12.dp, shape = shapes.large)` before `.background(...)` at each modal's host modifier in `ListScreen.kt` (item modals) and inside `ShopModal.kt` (its own panel). 12dp matches the QuickMenu's dropdown panel elevation, keeping one elevation scale. No `Ambient`/tonal elevation tricks — a plain drop shadow matches the web's single box-shadow.

### D4: Name row = underline `BasicTextField` + stepper pill well

Replace the boxed M3 `TextField` for the item name with a `BasicTextField` over an underline (bottom hairline that highlights in `ledOn` on focus — port of the web's `.underline-input`/`.focus-bar`). The quantity stepper (− value +) moves into a pill-shaped well (`background(colors.chrome)` or inset tone, `RoundedCornerShape(50%)`) on the same row's right, port of `stepperWell`. The M3 `TextField` stays for the search field (different component, not in scope).

### D5: Hairline separators as simple `HorizontalDivider` with `colors.border` at low alpha

The web uses `1px solid var(--color-border-light)`. Android has no `border-light` token; use `colors.border.copy(alpha = 0.5f)` — visually equivalent, no new token.

### D6: Watermark — bump `display` style and drop alpha

Change `ShoplistTypographyDefault.display` to 56sp / Bold (within the web's 48–80sp clamp range; 56sp fits "Auchan" on a phone width) and the watermark alpha from 0.07 to 0.04. The `display` style is only used by the watermark today, so the change is contained.

### D7: Visibility rows — checkbox-only `toggleable` target

Move the `toggleable(value, role = Checkbox)` modifier from the row onto the checkbox composable's touch target only (mirroring `ItemRow`'s structure). The row text becomes plain text. Semantics: the checkbox carries role/state; the row is a label.

## Risks / Trade-offs

- [Bordered buttons make modals taller on small screens] → Buttons are 44dp vs current ~28dp text; the modal already scrolls (`verticalScroll`). Acceptable.
- [`border` token may read faint in some themes for the unchecked checkbox] → Single-line color change in `LedSquareCheckbox`; validate across all nine themes during device smoke test.
- [Underline `BasicTextField` loses M3 TextField niceties (placeholder handling, IME glue)] → Port is small (placeholder text + focus bar); the search field keeps M3 TextField so patterns remain available.
- [56sp Bold watermark may overflow for long shop names] → Web handles this with `white-space: nowrap` + viewport clamp; Android `Text` will ellipsize/clip at screen edge — acceptable for a decorative element, verify with the longest shop name in smoke test.
- [Shadow on AnimatedVisibility content can clip during slide animation] → `Modifier.shadow` draws within bounds; if clipping appears, add small horizontal padding at the host. Verify in smoke test.

## Migration Plan

Single APK change, no data migration. Rollback = revert the branch. Order: shared composables → checkbox swap in ItemRow → modal layout rework → watermark → tests.

## Open Questions

None.
