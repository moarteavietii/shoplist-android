## Context

`ItemRow.kt` renders each list row with a decorative `LedCheckbox` (a 10dp `Canvas` circle using `ledOn`/`ledOff`) inside a row body that is fully `combinedClickable` with `role = Role.Checkbox` and `toggleableState` semantics — so any tap on the row checks the item. Long-press opens the item edit modal. The design language is chromeless/borderless with LED-dot accents; the widget stack already uses `tween(200)` for its chevron rotation. See proposal.md — Why for motivation.

## Goals / Non-Goals

**Goals:**

- A themed circle checkbox that reads as a checkbox while staying in the LED-dot visual identity
- Checkbox-only check/uncheck with a 48dp touch target
- Quiet, short animations consistent with the existing house style
- Accessibility semantics that survive the interaction restructure

**Non-Goals:**

- No new theme tokens or palette changes
- No haptics, scale pops, or ring pulses
- No changes to check-undo commit semantics, history, or the mutation queue
- No changes to drag-reorder behavior or the drag handle

## Decisions

**Custom-drawn circle checkbox over Material3 `Checkbox`.** The project treats Material 3 as an unthemed toolkit only, and the user prefers non-Material visuals. A custom `Canvas` circle (~30 lines) reuses `ledOn`/`ledOff` directly and matches the LED-dot identity. Alternative considered: Material3 `Checkbox` with custom colors — rejected because it drags Material ripple/shape conventions into a chromeless design.

**Reuse `ledOn`/`ledOff` tokens instead of new checkbox tokens.** These are already the checked/unchecked indicator pair in all 9 palettes and are used as the accent-ish color across drawers, widget stack, and modals. New tokens would ripple through 9 palettes and the theme-system spec for no visible gain.

**Circle with checkmark (LED evolution) over rounded square.** The circle is the natural continuation of the current LED dot — unchecked state stays visually close to today's LED-off dot, so the change reads as a refinement rather than a redesign. Alternative considered: rounded square with border — rejected as more visually foreign to the dot-based design language.

**`animateColorAsState` + animated path stroke, both `tween(200)`.** Matches the existing chevron timing in `WidgetStack`. The checkmark draws via an animated path-progress fraction: the ✓ path is measured once and drawn partially with `PathMeasure.getSegment`, so the same code animates the stroke in (check) and out (uncheck) with no direction-specific logic. Alternative considered: crossfade only — simpler, but the draw-in is the single "delight" moment of the control and costs ~10 lines.

**Tap target via `toggleable` on a 48dp box, semantics moved off the row.** The checkbox composable owns `Role.Checkbox`, `toggleableState`, and the click handler; the row body keeps only `combinedClickable` with `onLongClick` (no `onClick`). This satisfies the list-screen minimum-touch-target requirement and improves the accessibility story: screen readers see a real checkbox rather than a whole row pretending to be one. Alternative considered: keeping row-level `onClick` and filtering by tap position — rejected as fragile and semantically wrong.

**Row-body tap stays dead.** Per user decision, tapping the row does nothing; long-press remains the edit affordance. This avoids making tap and long-press redundant.

## Risks / Trade-offs

- [48dp target inflates row height] → The box is centered on the existing 20dp slot; verify row spacing stays compact per the list-screen spec and reduce visual padding if rows grow.
- [Checkmark path animation mis-measured on first frame] → Measure the path inside `remember` keyed on size; draw nothing while progress is 0 so the unchecked state never flashes a partial stroke.
- [Existing tests assert row-level tap-to-check] → Update interaction tests to target the checkbox semantics; ViewModel-level check tests are unaffected since the trigger change is UI-only.
