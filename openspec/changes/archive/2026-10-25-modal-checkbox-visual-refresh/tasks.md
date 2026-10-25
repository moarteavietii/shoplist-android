## 1. Shared visual-language composables

- [x] 1.1 Create `ui/common/LedSquareCheckbox.kt`: bordered rounded-square (20dp visual, configurable 48dp touch target), hollow unchecked state (border token, no fill), LED-on filled checked state with checkmark, 200ms color crossfade + checkmark path draw-in, checkbox role and checked-state semantics, `testTag` support
- [x] 1.2 Create `ui/common/ModalButton.kt`: 44dp-min bordered button with `Primary` (filled accent), `Danger` (danger outline), `Quiet` (border outline) styles, disabled = 0.5 opacity with boundary kept, button role semantics
- [x] 1.3 Create `ui/common/ModalPanel.kt` (or equivalent modifier helper): `shadow(12.dp, shapes.large)` + `background(surface)` + optional hairline `HorizontalDivider` with `border.copy(alpha = 0.5f)` for section separators

## 2. Item row checkbox swap

- [x] 2.1 Replace `CircleCheckbox` in `ItemRow.kt` with `LedSquareCheckbox`, keeping the 48dp touch target, `ItemRowTestTags.checkbox` tag, toggleable-only-on-checkbox behavior, and long-press-on-row-body behavior
- [x] 2.2 Update `ItemRow` unit tests for the new visual states (hollow unchecked / filled checked) and unchanged semantics

## 3. Item modal rework (add + edit)

- [x] 3.1 Rework the name row in `AddItemModal.kt`: underline `BasicTextField` with focus highlight + quantity stepper in a pill inset well aligned right of the name field
- [x] 3.2 Replace visibility-row LEDs with `LedSquareCheckbox` and move `toggleable` to the checkbox touch target only (row text taps become inert)
- [x] 3.3 Replace the action-row bare texts with `ModalButton`s: Save = Primary, Delete = Danger (edit only), Discard/Close = Quiet; keep canSave/saving gating
- [x] 3.4 Apply `ModalPanel` treatment (shadow + separators) to the add/edit modal hosts in `ListScreen.kt`
- [x] 3.5 Update add/edit modal unit tests: stepper placement semantics, checkbox-only visibility toggling, bordered button presence/enablement

## 4. Shop modal rework

- [x] 4.1 Apply `ModalPanel` shadow to the shop modal panel and replace `ModalAction` texts with `ModalButton`s (Delete = Danger, Discard/Close = Quiet, Save = Primary)
- [x] 4.2 Update shop modal unit tests for the button changes

## 5. Watermark

- [x] 5.1 Change `ShoplistTypographyDefault.display` to 56sp Bold and the watermark alpha in `ListScreen.kt` from 0.07 to 0.04
- [x] 5.2 Verify watermark rendering with the longest shop name (no layout breakage; passes through touches)

## 6. Verification

- [x] 6.1 Run `./gradlew test` and fix any regressions
- [x] 6.2 Run `./gradlew lint` and resolve new findings
- [x] 6.3 Device smoke test across all nine themes: checkbox legibility (border visibility when unchecked), modal shadow, button contrast, watermark scale

## 7. Round-2 review fixes

- [x] 7.1 Restyle the Temporary toggle in the item modal as a bordered pill chip (web `chip`/`chipOn`): 32dp pill, 1dp border token, transparent bg, muted 13sp label; ON = accent border + accent tint bg + accent-deep text
- [x] 7.2 Tighten the shop visibility row spacing in the item modal (reduce vertical padding/arrangement gaps)
- [x] 7.3 Change the checked-item exit animation in the list to "fly right" (slide right + fade) instead of plain fade + shrink
- [x] 7.4 Raise the edit item modal placement so it does not stick to the bottom of the screen
- [x] 7.5 Make the quantity stepper sleeker to match the web app (smaller circular keys in a compact pill well, bold value)
- [x] 7.6 Restyle the view drawer: "Views" → "Shops", smaller unchecked-count badge (QuickMenu SwitcherBadge style), edit icon right-aligned, centered title with spacing below, title-worthy font size, obvious Add shop button
- [x] 7.7 Update unit tests for the round-2 changes

## 8. Round-3 review fixes

- [x] 8.1 Add the clock icon to the Temporary chip (web `Clock size={14}`)
- [x] 8.2 Restore the checks counter in the edit modal (web `countPill`: inset-well pill, Repeat icon, muted 13sp semibold; add `EditItemUiState.checkCount`)
- [x] 8.3 Compact the edit modal: content spacing 12→8dp, visibility checkbox touch target 40→36dp
- [x] 8.4 Give the edit modal horizontal margins (16dp) so it does not touch the screen edges
- [x] 8.5 Center the +/- glyphs in the stepper keys (fixed-size Box with contentAlignment)
- [x] 8.6 Widen the drawer count badge (keep 10sp font, padding 8/3dp, corner 10dp)
- [x] 8.7 Pull the drawer edit icon toward the right edge (CenterEnd box, 18dp glyph, 4dp end padding)
- [x] 8.8 Add hairline separators after the drawer title and before the Add shop button
- [x] 8.9 Shrink drawer shop row spacing (vertical padding 10→6dp, drop horizontal padding)
- [x] 8.10 Add the 12dp dropshadow to the drawer container (DrawerShell, same as modalPanel)

## 9. Round-4 review fixes

- [x] 9.1 Center the quantity value text in the stepper (textAlign Center)
- [x] 9.2 Align the Temporary chip to the right in the add-item modal (spacer fills the left side when there is no checks counter)
- [x] 9.3 Give the add-item modal the same bottom (48dp) and horizontal (16dp) margins as the edit modal
- [x] 9.4 Align the drawer count badge to the right of the shop name text instead of the right of the container
- [x] 9.5 Uniform drawer row spacing (badge no longer stretches rows) and a 16dp gap between the title separator and the list
- [x] 9.6 Give the drawer a 16dp bottom margin so it does not touch the bottom of the screen

## 10. Round-5 review fixes

- [x] 10.1 Port the web ViewDrawer design to the shops drawer: bordered shop cards (1dp border, 8dp radius, 12dp padding) with name left and count inside right
- [x] 10.2 Active shop card accent-filled with inverted content and accent border (web `activeViewButton`)
- [x] 10.3 Count inside the card: accent-deep pill when unchecked > 0, faint "0" otherwise (web `countBadge`/`countZero`)
- [x] 10.4 Separate bordered 36dp square edit button right of the card (web `editButton`)
- [x] 10.5 Bare "+" add button, no border/background (web `addButton`), replacing the bordered ModalButton
- [x] 10.6 Left-aligned title, 8dp card spacing, hairline separators removed (web layout)

## 11. Round-6 review fixes

- [x] 11.1 Give the drawer "+" add button a 1dp border token border with 8dp radius, matching the other buttons
- [x] 11.2 Fix the Shop add/edit dialog bottom clipping: apply modalPanel (shadow + surface) before verticalScroll so the scroll container no longer clips the shadow
- [x] 11.3 Make "Shop name" a true in-field placeholder: overlay it inside the input box (centered vertically, contentFaint) instead of rendering it above the input
- [x] 11.4 Give checked and unchecked item names identical text styling (same color, no alpha dimming) so both read equally while preparing the list

## 12. Undo moves into the quick menu

- [x] 12.1 Add an UNDO control to QuickMenuControl with an undo icon and "Undo last action" content description
- [x] 12.2 Render the undo button only when canUndo is true; hide it when the history is empty (web `canUndo` conditional)
- [x] 12.3 Disable the undo button while an undo request is in flight (dimmed icon, taps ignored) (web `isUndoDisabled`)
- [x] 12.4 Track isUndoInFlight in the ViewModel around the undo flow and expose canUndo/isUndoInFlight in ListUiState
- [x] 12.5 Remove the full-width undo bar from ListScreen
- [x] 12.6 Add quick menu undo tests (absent/rendered/tap reports/disabled in flight) and update ListViewModelTest for the rename
- [x] 12.7 Batch the spec: add check-undo delta spec (undo affordance lives in the quick menu) to this change
- [x] 12.8 Animate the undo button in/out with expand/shrink vertically + fade so the quick menu container grows/shrinks at its bottom edge instead of the button popping

## 13. Add-item affordance restyle

- [x] 13.1 Replace the full-width "Add \"query\"" text line with a compact accent-filled ModalButton (Primary), matching the web ExpandableSearch add button
- [x] 13.2 Move the Add button into the search row (right-aligned, "Add" label only) so it squeezes the search field when it appears
- [x] 13.3 Remove the ripple/gray background from the search clear (×) button

## 14. QuickMenu polish

- [x] 14.1 Restyle the chevron button to match the other control buttons (chrome background, border, 8dp radius); it never renders a selected state
- [x] 14.2 Increase the control button corner radius from 4dp to 8dp
- [x] 14.3 Tighten the control spacing from 6dp to 4dp
- [x] 14.4 Shrink the container padding from 6dp to 4dp and bump its radius to 12dp so it stays concentric with the rounder buttons
- [x] 14.5 Remove the 48dp minimumInteractiveComponentSize touch-target expansion from the control buttons so the chevron-to-first-item gap matches the inter-item gap (4dp) and the container width is identical open and closed
- [x] 14.6 Add 4dp vertical spacing between the chevron button and the controls column so they no longer touch
