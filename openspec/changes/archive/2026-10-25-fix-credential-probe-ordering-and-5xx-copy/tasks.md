## 1. Tests first (TDD)

- [x] 1.1 Add a test asserting a non-401/403 `HttpException` (e.g. 500) during probe surfaces the server-error message and `CredentialErrorKind.ServerError`, not the unreachable message
- [x] 1.2 Add a test asserting `TokenStore` remains empty after a failed probe (rejection, 5xx, and network failure cases)

## 2. Implementation

- [x] 2.1 Add `ServerError` to `CredentialErrorKind` and `SERVER_ERROR_MESSAGE` to the ViewModel companion
- [x] 2.2 Add `ProbeOutcome.ServerError` and map non-401/403 `HttpException` to it; update `verifyAndSave` to surface the server-error kind/message
- [x] 2.3 Check `CredentialEntryScreen.kt` for any error-kind-specific rendering that needs the new kind

## 3. Verification

- [x] 3.1 Run `./gradlew test` and confirm all unit tests pass
- [x] 3.2 Run `./gradlew lint` and confirm no new lint issues
