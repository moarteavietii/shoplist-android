# Release signing — notes for the user

## Where things live

- **Keystore file:** `shoplist-release.jks` — **must live outside the repository**, e.g. `~/keystores/shoplist-release.jks`. The build refuses to sign with a keystore located inside the repo (a repo copy could be committed by a forced `git add` or a `.gitignore` regression). Point `shoplist.release.keystore.path` at the outside-repo location; the file also can't be lost by a careless `rm -rf` of the project.
- **Passwords:** expected in gitignored `local.properties` (repo root) or `~/.gradle/gradle.properties` (machine-independent). Required keys:

  ```properties
  shoplist.release.keystore.path=/absolute/path/to/shoplist-release.jks
  shoplist.release.keystore.password=YOUR_KEYSTORE_PASSWORD
  shoplist.release.keystore.alias=YOUR_KEY_ALIAS
  ```

  `~` in the path is expanded to your home directory. The key password defaults to the store password; if your keystore uses a distinct key password, set `shoplist.release.keystore.keypassword=YOUR_KEY_PASSWORD` alongside the other keys.

## Behavior

- `./gradlew assembleRelease` fails fast with a clear message naming the missing keys when any of the three properties is absent — it never silently produces an unsigned artifact.
- Debug builds ignore all of this.

## Backup guidance (do this once, today)

1. Store the **keystore file** and the **three property values** (path, password, alias) in your password manager as a secure note (or an encrypted backup).
2. Losing the keystore means release builds can no longer update the installed app in place — the only recovery is uninstall/reinstall (Room cache re-syncs from the server; Cloudflare credentials are re-entered). A backup removes even that cost.
