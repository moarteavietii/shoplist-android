## Context

The list screen currently has three disconnected feedback surfaces:

1. **Change toast** (`ChangeToastOverlay.kt`) — a `surface`-colored rounded rect at top-center. It uses the same surface color as list rows, so it doesn't read as floating above the app. No shadow, no severity, 2-line text.
2. **Error text** (`ListScreen.kt`) — plain `danger`-colored label at the bottom for transient failures ("Couldn't refresh the list", "Couldn't save the item", …), set via `errorMessage` in `ListViewModel` local state.
3. **Offline text** (`ListScreen.kt`) — plain `ledOn`-colored label at the bottom, driven by `isOffline` from `RealtimeSyncManager`.

The web app's equivalents: `ChangeToast.tsx` renders an inverse-surface full pill (radius 999) with `--shadow-toast`, single-line 14px/500 text, swipe-to-dismiss. The web has no offline banner (browsers signal connectivity natively), so the offline chip is an Android-only design decision. The web's `WidgetStack.tsx` derives `effectiveShowChecked = showChecked || isManualSortEngaged` for the checked-items button and rejects taps during manual sort with the message "Checked items stay visible while manual sorting is engaged".

The Android app already forces checked items visible during manual sort in the *rendering* path (`ListViewModel` view-state derivation: `hideChecked = !showChecked && !queryActive && !(sortModeEnabled && sortOrder == MANUAL)`), but the *control* (`QuickMenu` LED + tap handling) ignores manual sort.

Design-language constraints: custom token system via CompositionLocals (`LocalShoplistColors/Shapes/Typography`); Material 3 is only an unthemed toolkit. `ShoplistColors` has no inverse tokens today. The LED dot is the app's established status marker (quick menu, sort modes, settings).

## Goals / Non-Goals

**Goals:**
- One notification language: inverse pill for transient events (change toasts + failure toasts), quiet chip for persistent status (offline).
- Web parity for the manual-sort toggle behavior (effective visibility + rejection message).
- Severity expressed through the LED dot, keeping the pill itself monochrome.

**Non-Goals:**
- Real system notifications (NotificationManager) — the app is foreground-only for sync by design.
- Changing toast *content* or derivation rules (labels, queueing, dedup) — only presentation and the addition of failure toasts.
- A queued-mutation count badge on the offline chip (can be added later if wanted).
- Backporting anything to the web app.

## Decisions

### D1: Add inverse tokens to `ShoplistColors` rather than hardcoding
Add `surfaceInverse: Color` and `contentInverse: Color` to `ShoplistColors` and derive them per palette (e.g. from `content`/`background` — inverse surface ≈ the palette's content color, inverse content ≈ the palette's background/surface color). All 9 curated themes get them automatically.

*Alternative considered:* hardcode dark-gray/white in the overlay. Rejected — it would break themed rendering (e.g. High Contrast, Dark Slate) and violate the token system.

### D2: Extend `ChangeToast` with a severity field instead of a parallel error channel
`ChangeToast(id, text, severity)` where `severity ∈ {Info, Danger}`. Failure messages route through the same toast queue/derivation machinery as change toasts, just with `Danger`. The existing `errorMessage` local state is replaced by posting a failure toast; "clears on success" maps to the existing pattern of clearing `errorMessage` on success (the toast is self-replacing/short-lived, and a new failure re-posts it).

*Alternative considered:* a separate sticky error banner component. Rejected — two competing top-of-screen surfaces; the unified pill keeps one notification lane. Trade-off: failure toasts auto-expire like change toasts, so a persistent failure is re-announced on each retry rather than latched on screen. This matches the "quiet" philosophy; the offline chip covers the persistent case.

### D3: Offline chip is a new small composable in the list screen column
A `StatusChip` composable (LED dot + label, chrome/surface background, pill shape) placed where the offline text is today (bottom of the header column, above the list). It stays quiet: no animation beyond fade/expand, no danger color. Driven by the existing `isOffline` state flow — no new plumbing.

*Alternative considered:* moving it into the toast system as a sticky toast (web-style). Rejected — conflates persistent status with transient events and complicates the toast queue.

### D4: Effective visibility for the toggle, mirroring the web exactly
- `QuickMenu`: `deriveControlLedState(CHECKED_ITEMS_TOGGLE)` and the `isActive` computation take `isManualSortEngaged` and use `isShowingCheckedItems || isManualSortEngaged` (web's `effectiveShowChecked`).
- `ListViewModel.onQuickMenuControlTapped(CHECKED_ITEMS_TOGGLE)`: when manual sort is engaged, post the rejection message as a Danger toast and return; otherwise toggle as today.
- The rejection message text matches the web verbatim: "Checked items stay visible while manual sorting is engaged".

*Alternative considered:* disabling the button during manual sort. Rejected — the web deliberately keeps it enabled-but-inert so the user gets an explanation, not a dead control.

### D5: Keep swipe-to-dismiss and add tap-to-dismiss on the pill
The existing horizontal-drag dismissal in `ChangeToastOverlay` is kept (threshold unchanged); tap-to-dismiss is added for failure toasts (and harmless for change toasts). The pill becomes single-line with ellipsis, matching the web's `whiteSpace: nowrap` behavior.

## Risks / Trade-offs

- [Failure toasts expire, so a persistent failure isn't latched on screen] → The offline chip covers the persistent connectivity case; operation failures re-announce on each retry, which is acceptable for a glanceable system.
- [Inverse tokens touch all 9 palettes] → Derive them mechanically from existing palette colors (content ↔ background inversion) so no palette needs hand-tuning; a unit test asserts the tokens are populated for every theme.
- [Tap-to-dismiss on change toasts could cause accidental dismissal] → Change toasts are self-replacing and low-stakes; accidental dismissal loses nothing (next event re-toasts).
- [Single-line ellipsis may truncate long names] → Matches web behavior; the full name remains visible in the list itself.

## Migration Plan

Single client-side change, no data migration. Rollout order within the change: theme tokens → toast redesign → failure-toast routing (remove error text) → offline chip (remove offline text) → quick-menu effective state + rejection. Each step keeps the app shippable; rollback is reverting the branch.

## Open Questions

None — the toast style (inverse pill, web-faithful), offline chip (quiet, no count), and rejection delivery (danger toast) were settled during exploration.
