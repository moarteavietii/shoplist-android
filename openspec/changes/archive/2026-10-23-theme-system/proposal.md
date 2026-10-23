## Why

Feature parity (roadmap 6) is complete; roadmap item 7 (Themes) is the next migration step. The web app ships 14 themes, several of which are near-duplicates (Rose ≈ Sakura, Cream ≈ Pastel, Nord ≈ Dark Slate, Mocha ≈ Sunset, Forest ≈ Mint/Ocean). The Android app currently hardcodes a single dark palette in `ShoplistColors.kt`, so there is no user-facing theme selection at all.

## What Changes

- Curate the web app's 14 themes down to a 9-theme set (user-confirmed): **Default, Pastel, Mint, Lavender, Dark Slate, Ocean, Sunset, High Contrast** + a new **Christmas** theme (light, classic red/green). Dropped as near-duplicates: Cream, Rose, Sakura, Forest, Nord, Mocha.
- Port each kept theme's full palette from the web `index.css` `[data-theme]` blocks into a `ShoplistColors` scheme (background, surface, content, contentMuted, accent, danger, LED on/off, chrome/status-bar color).
- Add a `ThemeRepository` that persists the selected theme device-locally (DataStore preferences) and exposes it as a `StateFlow`.
- Add a settings drawer (ported from web spec `frontend-settings-drawer`) with a theme picker showing each theme's name and 3-color swatch, persisted selection, and a toggle for change toasts (already exists in-app; surfaced here).
- Enforce WCAG AA contrast (≥ 4.5:1 for text, ≥ 3:1 for large text/UI) between every theme's text/surface/accent pairs as unit tests.
- Apply the selected theme app-wide: `ShoplistTheme` consumes the persisted theme, and the system status bar tint follows the theme's chrome color.

## Capabilities

### New Capabilities
- `theme-system`: Theme catalog (9 curated themes), per-theme `ShoplistColors` palettes, persistence, and app-wide application including status-bar chrome.
- `settings-drawer`: Settings drawer UI — entry point, theme picker with swatches, toast toggle, persistence of user preferences.

### Modified Capabilities
<!-- None: the list-screen spec already mandates design-token-driven colors and is theme-agnostic; swapping palettes behind the tokens changes no requirement. -->

## Impact

- **Code:** `ui/theme/` (ShoplistColors gains accent/chrome tokens + 9 palettes; ShoplistTheme becomes parameterized), new `data/theme/` (ThemeRepository + DataStore), `ui/settings/` (new settings drawer screen), MainActivity (status bar color wiring), list screen composables (token-only styling — should be minimal since the design language already reads tokens).
- **Dependencies:** adds `androidx.dataStore:dataStore-preferences` (version catalog only, per repo convention).
- **Web specs as source material:** `frontend-theme-system`, `frontend-settings-drawer` (adapt, don't copy — native persistence and status-bar behavior differ).
- **No backend changes.** Themes are device-local; nothing is synced to the server.
