## Why

The transition when hiding checked items currently plays in two disconnected beats: checked rows first slide out horizontally as large fading blocks (still occupying their slots), and only after they are gone do the unchecked items reflow upward. The eye perceives two separate motions where one fluid motion is expected, and the full-width horizontal slide reads as heavy "panel dismissal" rather than items being checked off a list.

## What Changes

- Replace the per-row exit animation with a **collapse-while-fade** exit: vertical height collapse + fade + a small (~24dp) horizontal drift, so the reflow of unchecked items starts immediately and the transition reads as one motion.
- Add a **budget-capped stagger** across checked rows: each checked row's exit is delayed by its index, with the per-item delay compressed as the checked count grows (`stagger = min(40ms, 250ms / checkedCount)`), so small lists get a full top-to-bottom "peel" while large lists stay under ~470ms total.
- Mirror the stagger on **enter** (re-showing checked items) so the reverse transition is equally choreographed.
- Tune **asymmetric curves/durations**: exits use a faster, accelerating curve (~200ms); enters use a slightly slower, decelerating curve — things leave quicker than they arrive.
- Keep the existing `animateItem` fade specs nulled (bottom-edge flicker workaround) — the change is confined to the row-level `AnimatedVisibility` and the delay values passed into it.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `hide-checked-items`: the "Hiding animates and check-off commits instantly" requirement's animation contract is refined — exit becomes collapse-while-fade with a budget-capped stagger, enter mirrors the stagger, and curves become asymmetric (exit faster than enter).

## Impact

- `app/src/main/java/eu/pascariu/shoppinglist/ui/list/ItemRow.kt` — exit/enter animation specs, stagger delay parameter.
- `app/src/main/java/eu/pascariu/shoppinglist/ui/list/ListScreen.kt` — pass per-row stagger delay (derived from checked-item index and count) into `ItemRow`.
- No data-layer, API, or persistence changes. No new dependencies.
