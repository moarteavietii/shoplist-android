## 1. Icon import

- [x] 1.1 Process `pwa-512.png` from the web repo into a safe-zone-compliant foreground source: artwork scaled so its bounding-box diagonal fits the 66dp safe-zone circle (×0.8 on-device-verified shrink), centered on a 512×512 canvas with transparent padding (white comes from the background layer)
- [x] 1.2 Replace the adaptive-icon layers: new foreground/background assets replacing `drawable/ic_launcher_foreground.xml` and `ic_launcher_background.xml`, keeping the `mipmap-anydpi` adaptive XML structure (background + foreground + monochrome) intact
- [x] 1.3 Regenerate legacy raster fallbacks `mipmap-*/ic_launcher.webp` and `mipmap-*/ic_launcher_round.webp` from the new artwork
- [x] 1.4 Verify with `assembleDebug` and a launcher screenshot on device: no mask clipping, correct artwork in default and round variants

## 2. App label

- [x] 2.1 Change `app_name` in `res/values/strings.xml` from `shoplist-android` to `shopy` (user decision: plain ASCII; the earlier diacritic variant was rejected for searchability, and 2026-10-26 confirmed plain `shopy` as final)
- [x] 2.2 Verify on device: label renders under the launcher icon and in system Settings

## 3. Release signing

- [x] 3.1 Add keystore property reading to `app/build.gradle.kts` (`shoplist.release.keystore.path` / `.password` / `.alias` from `local.properties` with `~/.gradle/gradle.properties` fallback), failing with a clear error when missing
- [x] 3.2 Wire the signing config to the `release` build type; debug builds unaffected
- [x] 3.3 Document the keystore/password storage and backup guidance (password manager; keystore outside the repo) in the change notes for the user (`signing-notes.md`)
- [ ] 3.4 Verify with `assembleRelease`: build succeeds with properties present, fails with a clear message when absent, and the artifact installs on the device

## 4. Final verification

- [x] 4.1 Run `./gradlew test` and `./gradlew lint` — no regressions
- [x] 4.2 Device smoke test: fresh install shows new icon + `shopy` label; release build installs and launches
