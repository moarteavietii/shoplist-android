## Why

The app surfaces feedback through three disconnected, ad-hoc surfaces — a change toast that uses the same surface color as list rows (so it doesn't read as floating above the app), a plain danger text at the bottom for transient failures ("Couldn't refresh the list"), and a plain LED-colored text at the bottom for the persistent offline state ("Offline — changes will be queued"). None of them feel like notifications, and their styling is inconsistent with each other and with the app's LED-dot design language. Separately, the Checked-Items Toggle's LED does not reflect the forced visibility of checked items while manual sorting is engaged, diverging from the web app's behavior.

## What Changes

- Redesign the change toast as a high-contrast, notification-style pill consistent with the design system: inverse surface background, full-pill shape, soft shadow, single-line text, and a leading LED dot whose color reflects severity (default vs danger).
- Extend the toast system to carry transient failure messages (e.g. "Couldn't refresh the list", "Couldn't save the item") as danger-severity toasts, replacing the plain error text at the bottom of the list.
- Replace the plain offline text at the bottom with a persistent, quiet status chip (LED dot + label) that appears while offline and clears on reconnect.
- Align the Checked-Items Toggle with the web app during manual sort: the control's LED and pressed state reflect the *effective* visibility (checked items forced visible while manual sorting is engaged), and tapping it during manual sort shows a rejection message instead of toggling.

## Capabilities

### New Capabilities
- `notification-surface`: The unified visual language for transient notifications (change toasts and failure toasts) and the persistent offline status chip — styling, severity, dismissal, and placement.

### Modified Capabilities
- `change-toasts`: Toast presentation changes from a plain surface rounded-rect to a high-contrast inverse pill with a severity LED; the toast system now also carries transient failure messages (danger severity).
- `quick-menu`: The Checked-Items Toggle's LED/pressed state now derives from effective checked-item visibility (user preference OR manual sort engaged), and taps during manual sort produce a rejection message instead of toggling.
- `hide-checked-items`: The toggle's visual state requirement changes to reflect effective visibility during manual sort, with tap rejection while manual sorting is engaged.

## Impact

- **Code:** `ui/list/ChangeToast.kt`, `ui/list/ChangeToastOverlay.kt`, `ui/list/ListScreen.kt` (offline chip + error text removal), `ui/list/ListViewModel.kt` (failure toasts, rejection message), `ui/quickmenu/QuickMenu.kt` (effective LED state), `ui/theme/ShoplistColors.kt` + palettes (inverse tokens), `data/realtime/ChangeToastDerivation.kt` (severity field).
- **Tests:** `ChangeToastDerivationTest`, `ListViewModelTest`, `QuickMenu` LED derivation tests, new overlay styling tests.
- **No backend/API changes.** All behavior is client-side presentation and control-state logic.
- **Web parity:** Manual-sort toggle behavior mirrors `WidgetStack.tsx` (`effectiveShowChecked` + rejection message); toast styling mirrors `ChangeToast.tsx` (inverse pill, shadow, single line).
