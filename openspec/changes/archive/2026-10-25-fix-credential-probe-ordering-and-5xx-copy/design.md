## Context

The credential entry flow already implements probe-then-save: `CredentialEntryViewModel.verifyAndSave` probes `GET /health` with explicit CF headers and only writes to `TokenStore` on `ProbeOutcome.Verified` (see the diff-review commits `8daad73`, `7b0b64a`). What remains is (1) spec-level reconciliation — the delta spec in the `mock-cloudflare-auth-ux` change and the design doc still describe the ordering ambiguously — and (2) error classification: `probeCredentials` maps every non-401/403 `HttpException` to `Unreachable`, so a 5xx shows "Can't reach the server".

The user confirmed: every request to the server is Cloudflare-Access-gated (including `/health`), probe-then-save is the desired ordering, and 5xx must not read as a connectivity problem.

## Goals / Non-Goals

**Goals:**
- Restate the verification requirement as unambiguous probe-then-save in the delta spec.
- Add a `ServerError` error kind with its own message for non-401/403 HTTP failures.
- Keep the mock gate and existing rejection handling untouched.

**Non-Goals:**
- Changing the probe endpoint (a versioned endpoint would contradict the active design; `/health` gating is confirmed).
- Retry/backoff logic for 5xx during verification.
- Changes to the stored-credential rejection path (`AuthEventBus` handling).

## Decisions

- **Map non-401/403 `HttpException` to a new `ProbeOutcome.ServerError`** rather than reusing `Unreachable`: the two causes need distinct copy, and the spec now requires distinct messages per kind. Alternative — a boolean "reachable" flag — was rejected as less explicit and harder to test.
- **Add `CredentialErrorKind.ServerError` with message "Server error. Please try again later."** kept in the ViewModel's companion alongside the existing messages, matching the current pattern.
- **Spec reconciliation via MODIFIED requirement in this change's delta** rather than editing the `mock-cloudflare-auth-ux` change retroactively: that change is already implemented and partially merged; a follow-up delta keeps the history clean and syncs on archive.

## Risks / Trade-offs

- [Transient credentials in memory only during probe] → Already the case; no persistence before verification, covered by a new test asserting the store is empty after a failed probe.
- [5xx during probe leaves the user stuck without credentials] → Acceptable: the server is genuinely unhealthy; the message tells the user to retry later. No auto-retry.

## Migration Plan

Single APK change; no data migration. Rollback is reverting the commit.

## Open Questions

(none)
