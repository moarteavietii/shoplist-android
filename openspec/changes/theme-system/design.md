## Context

The app has a single hardcoded dark palette (`ShoplistDarkColors` in `ui/theme/ShoplistColors.kt`) provided through `LocalShoplistColors` by `ShoplistTheme`. All screens already style via these tokens, so swapping palettes is a provider-level change. The web app defines full palettes as `[data-theme]` CSS blocks in `apps/frontend/src/index.css`; each block overrides a subset of ~20 color variables. Persistence on the web is `localStorage` under `shoppinglist:theme`.

## Goals / Non-Goals

**Goals:**
- 9 palettes as `ShoplistColors` values, hex-faithful to the web CSS
- Device-local persistence and instant app-wide application
- Settings drawer hosting the theme picker and the change-toasts toggle
- WCAG AA contrast enforced by unit tests

**Non-Goals:**
- No dynamic/Material You color, no system dark/light auto-switching (themes are explicit user choices, mirroring the web app)
- No server sync of preferences
- No animated cross-fade between themes (web's 200ms transition is a DOM-compositing trick; Compose recomposition is instant and good enough — revisit only if the switch feels jarring)
- No new themes beyond the catalog; the Christmas theme is designed once in this change

## Decisions

**1. Extend `ShoplistColors` rather than introduce a parallel palette type.**
Add `accent`, `accentDeep`, `contentFaint`, `border`, `chrome` (status bar) fields to the existing data class; keep `ledOn`/`ledOff` derived per theme from the web palettes (LED dots are an app-specific token with no web equivalent — map from accent hue, dimmed for `ledOff`). Alternative considered: a separate `ThemePalette` wrapper — rejected as indirection; every consumer already reads `ShoplistColors`.

**2. One Kotlin file of palette constants, hand-ported from the CSS.**
`ui/theme/ShoplistPalettes.kt` defines `val ShoplistThemePalettes: Map<ThemeId, ShoplistColors>` with the 9 entries. A port is a mechanical hex copy; a codegen step (parsing the CSS at build time) is overkill for 9 static themes and adds build complexity. A unit test asserts each palette's key hexes against the web values to catch porting typos.

**3. Theme id as a plain enum, persisted via DataStore Preferences.**
`ThemeId` enum (`DEFAULT, PASTEL, MINT, LAVENDER, DARK_SLATE, OCEAN, SUNSET, HIGH_CONTRAST, CHRISTMAS`) stored as its name string in DataStore. `ThemeRepository` exposes `StateFlow<ThemeId>`, defaults to `DEFAULT`, and falls back on unknown values. DataStore over SharedPreferences: coroutine-native, transactional, already the idiomatic Compose-era choice. New dependency: `androidx.dataStore:dataStore-preferences` (version catalog).

**4. `ShoplistTheme(themeId)` parameterized at the activity root.**
MainActivity collects `ThemeRepository.theme` and passes it to `ShoplistTheme`, which selects the palette and provides it via the existing CompositionLocals. No screen-level changes needed — they already read tokens. Status bar: `enableEdgeToEdge` + `WindowInsetsController`/`SystemBarStyle` set from the palette's `chrome` color in the same collector.

**5. Settings drawer reuses the view drawer's presentation components.**
The view drawer already implements left-anchored slide-in + scrim + back handling; extract/reuse that scaffolding for the settings drawer rather than introducing a third drawer implementation. The Settings gear button slots into the widget stack below Search (widget-stack spec owns the slot layout). Theme options render as rows (name + 3-color swatch drawn with `Canvas`/`Row` of boxes); active row marked with the accent color. The change-toasts toggle reuses the existing toast-enabled state (currently code-internal) by promoting it to a persisted preference in the same DataStore.

**6. Contrast gate as pure-Kotlin unit tests.**
A `ThemeContrastTest` computes WCAG relative-luminance contrast ratios over the palette constants (no Android deps, plain JUnit4): content vs surface ≥ 4.5:1, accent vs surface ≥ 3:1, danger vs surface ≥ 3:1, for all 9 themes. The web repo's `themeContrast.test.ts` is the reference for the algorithm.

## Risks / Trade-offs

- [Porting errors (wrong hex, swapped tokens)] → Unit test pins each palette's background/surface/text/accent/danger hexes to the web values; contrast tests catch illegible mistakes.
- [Muted/faint text tokens in some web themes sit near the 4.5:1 boundary] → The gate covers content and accent roles; if a ported theme fails, adjust that token in the Android palette and note the deviation in the palette file rather than shipping an illegible theme.
- [DataStore first-read latency on cold start] → Negligible (single small preference); the theme is read synchronously-ish via `runBlocking`-free `first()` in a `LaunchedEffect`-free pattern — the activity collects the StateFlow before calling `setContent` content that depends on it; worst case one frame on Default.
- [Extracting drawer scaffolding touches the view drawer] → Keep the refactor mechanical (move composables, no behavior change); widget-stack and view-drawer specs are unaffected.
