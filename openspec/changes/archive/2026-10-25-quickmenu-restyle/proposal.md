## Why

The Android burger menu (`WidgetStack`) renders as a flat column of bare icons and reads as visually broken next to the web app's polished burger menu (`apps/frontend/src/components/WidgetStack.tsx`): bordered card buttons, dimmed inactive state, corner LED dots with glow, and a frosted translucent container. The name "WidgetStack" is also misleading in Android terms (app widgets are home-screen components; this is a menu). The web component is the design reference for the whole app, so the Android menu should match it.

## What Changes

- **BREAKING (internal API only)**: Rename `WidgetStack` → `QuickMenu`, package `ui.widgetstack` → `ui.quickmenu`, `WidgetStackControl` → `QuickMenuControl`, and all related identifiers (composable, enum, view-model state, persisted key, tests).
- Port the web burger menu's visual language to the Android menu:
  - Each control becomes a bordered card button (1px border, rounded, surface background) instead of a bare icon row.
  - Inactive controls are dimmed (muted background + faint icon tint); active controls use full surface + normal text color.
  - LED status dots move to the button's top-right corner, with a glow when filled and a ring when hollow.
  - The container becomes a near-opaque surface (high alpha, no real backdrop blur — performance) with a soft shadow.
  - The unchecked-count badge moves from the chevron to the View Switcher button.
  - Sort-order selector options get per-order icons and a check mark for the selected order.
- No behavior changes: control order, tap/long-press semantics, swipe-to-collapse, persistence, and drawer/search/sort wiring stay exactly as they are.

## Capabilities

### New Capabilities

- `quick-menu`: The renamed and restyled burger menu — floating top-right control stack with card buttons, corner LED dots, dimmed inactive state, and near-opaque container. Supersedes the `widget-stack` capability.

### Modified Capabilities

- `widget-stack`: **REMOVED** — the capability is renamed to `quick-menu`; all requirements carry over unchanged except the visual presentation requirements, which are updated to the card-button design.

## Impact

- **Code**: `app/src/main/java/eu/pascariu/shoppinglist/ui/widgetstack/` (package rename + restyle), `ui/list/ListScreen.kt`, `ui/list/ListViewModel.kt`, `ui/list/ListUiState.kt`, `data/prefs/ListPreferencesDataSource.kt` (persisted expand-state key), `data/prefs/SortOrder.kt` (no change, just imports).
- **Tests**: `WidgetStack` unit tests renamed and updated; Robolectric Compose tests updated for the new package/identifiers.
- **No backend/API impact**: purely client-side UI.
- **Performance**: no real backdrop blur is used; the frosted look is approximated with a high-alpha surface color to keep GPU cost near zero.
