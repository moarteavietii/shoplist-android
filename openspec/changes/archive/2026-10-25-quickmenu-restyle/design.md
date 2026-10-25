## Context

The Android burger menu lives in `ui/widgetstack/WidgetStack.kt` (331 lines): a `Column` of bare icon rows inside an opaque surface box, with an inline `LedDot` next to icons, the count badge on the chevron, and a `Popup`-based sort-order selector. The web reference is `apps/frontend/src/components/WidgetStack.tsx` (564 lines): a frosted translucent stack of bordered 40×40 card buttons, corner LED dots with glow, dimmed inactive state, badge on the view-switcher button, and a fixed-position selector with per-order icons and check marks. The web component is the app's design reference (see `docs/ROADMAP.md` history — the custom design language ports the web aesthetic).

## Goals / Non-Goals

**Goals:**
- Pixel-close visual parity with the web burger menu, using the existing token system (`LocalShoplistColors/Shapes/Typography`).
- Rename `WidgetStack` → `QuickMenu` everywhere (package, composable, enum, VM state, persisted key, tests).
- Keep GPU cost near zero: no real backdrop blur.

**Non-Goals:**
- No behavior changes: control order, tap/long-press semantics, swipe-to-collapse, persistence, drawer/search/sort wiring stay as-is.
- No undo button (undo is not implemented on Android yet — `check-undo` spec covers the list-level undo only).
- No tooltips (Compose tooltip parity with `WidgetTooltip` is not worth the custom popup work now).
- No changes to the ViewDrawer or SettingsDrawer (separate concern, same "flat vs card" gap, but out of scope here).

## Decisions

**D1: Rename mechanics — single mechanical pass.**
`ui.widgetstack` → `ui.quickmenu`; `WidgetStack` → `QuickMenu`; `WidgetStackControl` → `QuickMenuControl`; `SwitcherBadge` stays (private). VM/state identifiers: `isWidgetStackExpanded` → `isQuickMenuExpanded`, `onWidgetStackExpandedChanged` → `onQuickMenuExpandedChanged`, `widgetStackExpanded` (prefs key) → `quickMenuExpanded`. The persisted key changes value, so existing installs lose the persisted expand state once — acceptable for a personal app; the state is trivial to re-toggle. Alternative considered: keep the old storage key to preserve state — rejected, the key rename is part of making the codebase coherent, and the cost is one re-toggle.

**D2: Card buttons via existing tokens, no new theme primitives.**
Each control row becomes a 40dp `Box` with `border(1.dp, colors.borderSoft, shapes.small)` + `background(colors.surface)`; inactive state swaps to `colors.surfaceMuted` bg + `colors.contentFaint` tint. This mirrors the web's `button`/`buttonInactive` styles. If `borderSoft`/`surfaceMuted`/`contentFaint` tokens don't exist under those names, use the closest existing tokens rather than adding new ones (KISS). Alternative: introduce a `QuickMenuButton` abstraction — rejected for now, five call sites don't justify it; a private composable inside the file is enough.

**D3: Corner LED with glow — layered draws, no blur.**
The web LED is an 8px absolute dot with `boxShadow: 0 0 3px accent`. Compose equivalent: 8dp dot drawn at the button's top-right via `Box` alignment, filled state = accent circle + a slightly larger low-alpha accent circle behind it (fake glow), hollow = 1.5dp ring (`border` on a transparent circle). No `Modifier.blur` / `RenderEffect` — that's the performance trap this change explicitly avoids. The existing `LedDot` composable is repurposed/renamed to a corner variant; the inline next-to-icon placement disappears.

**D4: Near-opaque container instead of backdrop blur.**
Web uses `rgba(surface, 0.95)` + `blur(4px)`. Android: `colors.surface.copy(alpha = 0.95f)` + `shadow(4.dp, shapes.medium)` on the container `Column`. Real blur (`RenderEffect`) is GPU-costly and fiddly behind Popups; at 95% alpha the visual difference over a list background is negligible. Alternative: real blur gated on device capability — rejected, complexity not justified.

**D5: Badge moves to the View Switcher button.**
`SwitcherBadge` re-homes from the chevron `Box` to the View Switcher control's `Box`, aligned `TopEnd`, keeping the existing pill styling (accent-deep bg, inverted text) which already matches the web's `countBadge`. The web adds a 2px surface ring (`boxShadow: 0 0 0 2px surface`); replicate with a 2dp surface border on the badge.

**D6: Sort selector options get icons + check.**
Each `SortOrder` maps to an icon (Manual → `ArrowUpDown`-equivalent `Icons.Filled.Sort`, A→Z → `Icons.Filled.SortByAlpha`, Frequency → `Icons.Filled.LocalFireDepartment`), with a `Check` icon for the selected row replacing the `LedDot`. Selector container gains a 1dp border to match the web's bordered menu.

**D7: Icons stay Material-filled set.**
The web uses lucide icons (`Store`, `Settings`, `ArrowUpDown`, `Check`, `Search`, `Undo2`). Android keeps `material-icons-core` (already a dependency, no new dependency for lucide-equivalents): View Switcher → `Icons.Filled.Store` (exists in core? verify at implementation; fallback `Icons.Filled.Home` or `ViewWeek`), Settings → `Icons.Filled.Settings`, Sort → `Icons.Filled.Sort`, Checked toggle → `Icons.Filled.Check`, Search → `Icons.Filled.Search`. Verify each exists in `material-icons-core` during implementation; `material-icons-extended` is the fallback if a glyph is missing, but prefer core to keep APK size flat.

**D8: Tests follow the rename.**
Existing `WidgetStack` tests (grep for them at implementation) are renamed and updated for new identifiers and the new visual assertions (badge on view switcher, dimmed inactive state, corner LED). Robolectric config per the known gotcha: `@Config(sdk = [36], application = android.app.Application::class)`.

## Risks / Trade-offs

- [Persisted expand state lost on upgrade] → Accepted (D1); one re-toggle.
- [Icon glyph gaps in `material-icons-core`] → Verify at implementation; `material-icons-extended` fallback if needed (D7).
- [Fake glow looks flat on some themes] → The glow is a low-alpha halo; if it reads poorly on dark themes, tune halo alpha per-theme later — visual-only, no spec impact.
- [95% alpha container over bright list content] → At 0.95 alpha, contrast loss is minimal; if legibility suffers in practice, bump to 0.97 — visual-only tweak.
- [Rename touches many files] → Mechanical, compiler-verified; do rename first, restyle second, so each step builds green.

## Migration Plan

1. Rename pass (package + identifiers + prefs key + tests) — build + tests green.
2. Restyle pass (container, card buttons, corner LED, badge re-home, selector icons) — build + tests green.
3. Device smoke test on the physical device.

Rollback: revert the branch; no data migration beyond the lost expand-state key.
