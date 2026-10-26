## Context

`ItemRow` wraps its content in `AnimatedVisibility` with `enter = fadeIn() + expandVertically()` and `exit = slideOutHorizontally(it / 2) + fadeOut()`. Because the exit keeps the row's slot occupied until the animation completes, the `LazyColumn` reflow (via `Modifier.animateItem`) only starts after the exits finish — producing the two-beat feel described in the proposal. The list-level `animateItem` fade specs are deliberately nulled to avoid the `Arrangement.Bottom` bottom-edge flicker gotcha; this design must not disturb that.

## Goals / Non-Goals

**Goals:**
- One-motion transition: reflow overlaps the exit.
- Budget-capped stagger for both exit and enter.
- Asymmetric timing (exit faster than enter).
- All changes confined to row-level animation specs + a delay parameter.

**Non-Goals:**
- No list-level removal via `visibleItems` filtering (that's where the flicker gotcha lives).
- No changes to drag-reorder, search-override, or manual-sort-override behavior.
- No new animation libraries.

## Decisions

**1. Exit = `fadeOut + shrinkVertically + slideOutHorizontally(24dp)`**
The vertical shrink is what collapses the row's slot during the exit, letting placement animations run concurrently. The small horizontal drift keeps a hint of "dismissed" direction without the heavy full-width sweep. Alternative considered: list-level removal with `animateItem` fade/placement specs — rejected because it re-opens the bottom-edge flicker risk we already engineered around.

**2. Budget-capped stagger: `delay = min(40ms, 250ms / checkedCount)`**
Fixed per-item delays scale linearly with count (40 checked items → 1.8s, unacceptable). A fixed total budget divided across rows keeps the peel visible for small lists while compressing to near-simultaneity for large ones. Total worst case ≈ exitDuration + 250ms ≈ 450ms regardless of count.

**3. Per-row delay computed in `ListScreen`, passed as a parameter to `ItemRow`**
`ItemRow` stays a dumb renderer: it receives `staggerDelayMillis` and applies it to its `AnimatedVisibility` specs. `ListScreen` derives the delay from the row's position among checked items (index among checked / checked count). Alternative: compute inside `ItemRow` from a passed index — rejected, it would couple the row to list-ordering knowledge.

**4. Asymmetric curves: exit ~200ms accelerating, enter ~260ms decelerating**
Symmetric timing reads as mechanical; things leaving faster than they arrive matches physical intuition. Compose's `tween` with `FastOutLinearInEasing` (exit) and `LinearOutSlowInEasing` (enter) map directly to this. Springs were considered but tween with material curves is more predictable for a fixed-choreography transition.

**5. Enter stagger mirrors exit (top-to-bottom, same budget)**
Re-showing should feel as intentional as hiding. Same formula, same cap.

## Risks / Trade-offs

- [Stagger delay applies to enter too — a row entering with a delay may briefly overlap a neighbor's placement animation] → Keep enter stagger ≤ exit stagger and verify visually on device; the budget cap bounds worst case.
- [`shrinkVertically` on exit + `animateItem` placement could double-animate the height change] → The `AnimatedVisibility` shrink collapses the row's measured height; `animateItem` placement animates the *offsets* of surviving rows. They compose rather than conflict, but this is the main thing to watch in the device smoke test.
- [Bottom-edge flicker regression] → Fade specs on `animateItem` stay nulled; no list-level changes are made.
