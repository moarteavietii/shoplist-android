## Context

The manifest already wires `@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`, and a monochrome layer correctly; every layer currently points at stock Compose template assets. The web project (`shoppinglist/apps/frontend/public/`) provides `pwa-512.png` (512×512 RGBA, blue cart + checkmark on white, ~10–15% padding). Its "maskable" variants are byte-identical to the regular ones, so no true maskable source exists — the safe-zone treatment must be done on the Android side. The user has already generated a release keystore outside the repo.

## Goals / Non-Goals

**Goals:**

- Faithful import of the web icon into the adaptive-icon pipeline with safe-zone compliance (option A, user-confirmed).
- Product label `şopy` everywhere the launcher label surfaces.
- Reproducible, gitignored release signing wired into Gradle.

**Non-Goals:**

- Fixing the web repo's maskable icons (pre-existing PWA nit, separate repo).
- A dedicated monochrome/themed-icon glyph (duotone art renders as-is when wallpaper-tinted; revisit if it looks bad on device).
- Play Store distribution concerns (Play App Signing, AAB-specific config) — app is sideloaded.

## Decisions

### D1: Safe-zone scaling done as image processing, not vector redraw
Process `pwa-512.png` once at implementation time: composite the artwork at ~66% scale, centered, onto an opaque white 512×512 canvas. The result becomes the adaptive foreground source; the background layer is flat white.

- *Why not redraw as vector:* the source is raster; a faithful vector trace is effort with no payoff for a two-tone icon, and the raster pipeline (webp at each density) is exactly what the template already uses.
- *Why not use the PNG as-is (option B):* the cart's runner bar and handle sit near the mask edge; circle/squircle launchers would clip corners.

### D2: Foreground/background as generated webp + XML, replacing template assets
Replace `drawable/ic_launcher_foreground.xml` / `ic_launcher_background.xml` with the processed raster (as density webp in `mipmap-*`, referenced by the adaptive icon XML), and regenerate the legacy `ic_launcher*.webp` fallbacks from the same source. `mipmap-anydpi/ic_launcher*.xml` keeps its current structure (background + foreground + monochrome referencing the new foreground).

- *Alternative considered:* Image Asset Studio in Android Studio — rejected for this flow because it's an interactive IDE tool; the same output is achievable deterministically with a small script (`sips`/ImageMagick or a Python script) and keeps the change reviewable.

### D3: Signing config reads from gitignored properties, fails loudly
`app/build.gradle.kts` reads `shoplist.release.keystore.path`, `.password`, `.alias` from `local.properties` (falling back to `~/.gradle/gradle.properties` for machine-independent setups). If the properties are absent, the release signing config is created with a `keystoreProperties` check that throws a clear Gradle error naming the missing entries — release builds fail fast instead of silently producing unsigned artifacts. Debug builds are unaffected.

- *Why `local.properties`:* already gitignored and already used for `devBaseUrl`; one place for machine-local secrets.
- *Alternative considered:* environment variables — works in CI but is awkward for a personal machine; properties files are the established pattern in this repo.

### D4: Password storage guidance (user-facing, not code)
The keystore passwords live in gitignored `local.properties`; the keystore file itself stays outside the repo. Backup guidance: store the keystore file + passwords in the user's password manager (or encrypted backup). Losing both is recoverable only via uninstall/reinstall (data loss: Room cache re-syncs; Cloudflare credentials re-entered) — acceptable for personal distribution, but the backup removes even that cost.

## Risks / Trade-offs

- [Duotone art as monochrome layer may look poor when wallpaper-tinted] → Verify on device during smoke test; if unacceptable, a follow-up adds a dedicated single-color silhouette glyph (small, isolated change).
- [Raster foreground at 66% scale may soften edges at small densities] → Generate webp from the 512px source with high-quality downsampling; visually verify at launcher size on device.
- [Keystore loss] → Password-manager backup of keystore + passwords (D4); worst case is uninstall/reinstall, not permanent lockout.
- [`şopy` diacritic rendering] → Full Unicode labels are standard on Android; verify on device screenshot anyway since it's the most visible string in the product.

## Migration Plan

1. Implement on this branch; verify with `assembleDebug`, `assembleRelease` (signed), and a device smoke test (icon, label, themed icon).
2. Rollback: revert the branch; no data migrations, no server interaction — the change is fully client-side and stateless.

## Open Questions

None.
