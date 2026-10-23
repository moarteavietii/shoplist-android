## 1. Theme catalog and contrast gate (TDD)

- [x] 1.1 Add `ThemeId` enum and extend `ShoplistColors` with `accent`, `accentDeep`, `contentFaint`, `border`, `chrome` fields (update existing usages of the extended fields)
- [x] 1.2 Write `ThemeContrastTest` (plain JUnit4, WCAG relative-luminance algorithm ported from web `themeContrast.test.ts`) asserting content/surface ≥ 4.5:1, accent/surface ≥ 3:1, danger/surface ≥ 3:1 for all 9 themes; run it against a placeholder to see it fail
- [x] 1.3 Create `ui/theme/ShoplistPalettes.kt` with the 9 palettes hand-ported from web `index.css` `[data-theme]` blocks (Default, Pastel, Mint, Lavender, Dark Slate, Ocean, Sunset, High Contrast) plus the new light red/green Christmas palette; derive `ledOn`/`ledOff` per theme
- [x] 1.4 Write a palette-pinning unit test asserting each theme's background/surface/content/accent/danger hexes equal the web CSS values; make both test suites pass (`./gradlew test`)

## 2. Persistence

- [x] 2.1 Add `androidx.dataStore:dataStore-preferences` to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 2.2 Write `FakeThemeStore`-style unit tests for `ThemeRepository`: default is `DEFAULT`, unknown stored value falls back to `DEFAULT`, selection persists
- [x] 2.3 Implement `ThemeRepository` (DataStore Preferences, `StateFlow<ThemeId>`, `setTheme`) plus the change-toasts-enabled preference in the same DataStore; wire Hilt binding; make tests pass

## 3. App-wide application

- [x] 3.1 Parameterize `ShoplistTheme(themeId)` to select the palette from `ShoplistThemePalettes` and provide it via the existing CompositionLocals
- [x] 3.2 MainActivity: collect `ThemeRepository.theme`, pass it to `ShoplistTheme`, and set the status-bar `SystemBarStyle` from the palette's `chrome` color; verify no flash of wrong palette on relaunch
- [x] 3.3 Promote the change-toasts enabled flag to read from `ThemeRepository` so the existing toast flow consumes the persisted preference

## 4. Settings drawer

- [x] 4.1 Extract/reuse the view drawer's left-anchored panel + scrim + back-handling scaffolding for a reusable drawer shell (mechanical refactor, no behavior change; `./gradlew test` green)
- [x] 4.2 Add the Settings gear button to the widget stack below Search, opening the settings drawer
- [x] 4.3 Implement the settings drawer UI: Theme section (9 rows with name + 3-color swatch, active row marked) and the change-toasts toggle; selecting a theme applies immediately and the drawer stays open
- [x] 4.4 Add unit tests for the settings view model (theme selection delegates to repository; toggle persists)

## 5. Verification

- [x] 5.1 Run `./gradlew test` and `./gradlew lint`; fix findings
- [x] 5.2 Device smoke test: launch, open settings, switch through several themes (incl. Christmas), verify instant recolor + status bar tint, kill and relaunch to verify persistence
