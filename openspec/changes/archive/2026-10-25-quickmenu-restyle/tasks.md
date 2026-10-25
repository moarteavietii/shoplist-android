## 1. Rename pass (WidgetStack → QuickMenu)

- [x] 1.1 Rename package `ui.widgetstack` → `ui.quickmenu` and move `WidgetStack.kt` accordingly
- [x] 1.2 Rename identifiers: `WidgetStack` → `QuickMenu`, `WidgetStackControl` → `QuickMenuControl`, `isWidgetStackExpanded` → `isQuickMenuExpanded`, `onWidgetStackExpandedChanged` → `onQuickMenuExpandedChanged`, and update all usages in `ListScreen.kt`, `ListViewModel.kt`, `ListUiState.kt`
- [x] 1.3 Rename persisted prefs key `widgetStackExpanded` → `quickMenuExpanded` in `ListPreferencesDataSource.kt`
- [x] 1.4 Rename and update existing WidgetStack tests for the new identifiers; keep Robolectric `@Config(sdk = [36], application = android.app.Application::class)`
- [x] 1.5 Verify: `./gradlew assembleDebug test` green

## 2. Restyle pass (web parity)

- [x] 2.1 Container: near-opaque surface (`alpha = 0.95f`), rounded corners, soft shadow; remove opaque flat background
- [x] 2.2 Convert each control row into a 40dp bordered card button (1dp border, rounded, surface bg); dimmed inactive state (muted bg + faint tint) per design D2
- [x] 2.3 Move LED dots to the button's top-right corner with fake glow (filled) / ring (hollow) per design D3; remove inline next-to-icon placement
- [x] 2.4 Re-home the unchecked-count badge from the chevron to the View Switcher button, add 2dp surface ring per design D5
- [x] 2.5 Sort-order selector: per-order icons + check mark for selected, bordered container per design D6; verify icon glyphs exist in `material-icons-core`, fall back to `material-icons-extended` if needed (D7)
- [x] 2.6 Update visual test assertions (badge location, dimmed inactive, corner LED)

## 3. Verification

- [x] 3.1 `./gradlew assembleDebug test` green
- [x] 3.2 `./gradlew lint` clean
- [x] 3.3 Device smoke test: menu renders as card stack, badge on view switcher, corner LEDs, selector with icons; collapse/expand and swipe-to-collapse still work

## 4. Visual polish (user feedback)

- [x] 4.1 Remove tap ripple/graying from chevron, control buttons, and selector rows (indication = null)
- [x] 4.2 Center the control buttons in the menu instead of right-aligning them
- [x] 4.3 Soften button borders (alpha on border color)
- [x] 4.4 Move the widget down ~1% of screen height and stop sticking it to the right edge via app-wide horizontal padding (minimal at bottom)
