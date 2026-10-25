# Release signing — notes for the user

## Where things live

- **Keystore file:** `shoplist-release.jks` (currently in the repo root; it is gitignored via `*.jks`, so it will never be committed). Recommended: move it somewhere outside the repo, e.g. `~/keystores/shoplist-release.jks`, and point `shoplist.release.keystore.path` at that location — the repo copy then can't be lost by a careless `rm -rf` of the project.
- **Passwords:** expected in gitignored `local.properties` (repo root) or `~/.gradle/gradle.properties` (machine-independent). Required keys:

  ```properties
  shoplist.release.keystore.path=/absolute/path/to/shoplist-release.jks
  shoplist.release.keystore.password=YOUR_KEYSTORE_PASSWORD
  shoplist.release.keystore.alias=YOUR_KEY_ALIAS
  ```

  `~` in the path is expanded to your home directory. The key password is assumed to be the same as the store password; if you set a distinct key password, tell the agent and a `shoplist.release.keystore.keypassword` key can be added.

## Behavior

- `./gradlew assembleRelease` fails fast with a clear message naming the missing keys when any of the three properties is absent — it never silently produces an unsigned artifact.
- Debug builds ignore all of this.

## Backup guidance (do this once, today)

1. Store the **keystore file** and the **three property values** (path, password, alias) in your password manager as a secure note (or an encrypted backup).
2. Losing the keystore means release builds can no longer update the installed app in place — the only recovery is uninstall/reinstall (Room cache re-syncs from the server; Cloudflare credentials are re-entered). A backup removes even that cost.
