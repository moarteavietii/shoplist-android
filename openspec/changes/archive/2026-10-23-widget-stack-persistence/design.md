## Context

The list screen currently has a single floating control: the temporary burger icon (drawer trigger) at the list-area top-start, plus a bottom search field. Every remaining roadmap-5 feature mounts on the web app's widget stack, so the stack comes first. The app has no persistent local storage yet; DataStore Preferences is already in the version catalog and `build.gradle.kts` (added with the scaffold) but unused. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- Widget stack as a self-contained composable other batches extend without touching `ListScreen` layout again
- First `DataStore` usage establishing the app's preferences pattern (injectable store, testable fakes)
- Zero behavior change for the drawer itself — only its trigger moves

**Non-Goals:**

- Implementing Settings, Sort Mode, or Checked-Items Toggle behavior (placeholder slots only)
- LED dots on togglable controls, count badges, search restructure (later batches)
- Room or any structured storage

## Decisions

### Stack state in ListViewModel, persisted via a new PreferencesStore
`widgetStackExpanded: Boolean` joins the list UI state; a new `data/prefs/ListPreferencesDataSource` (DataStore Preferences) persists it. The data source is constructor-injected like the repositories, so unit tests use a hand-written fake — no Android fixture needed for the ViewModel tests.

*Alternative:* rememberSaveable in the composable. Rejected — it doesn't survive process death reliably for this purpose and scatters persistence into UI code; the DataStore source is also exactly what batches 2–3 need for sort mode, show-checked, and search-expanded keys.

### View persistence rides the same data source
`activeShopId: Long?` gets its own key in the same `ListPreferencesDataSource`. Restore flow: ViewModel reads the id at init, holds it as "pending" until `ShopsRepository.shops` emits, validates membership, then applies it (or falls back to Global). This satisfies the "never render an unvalidated view state" scenario without a loading flag in the UI.

*Alternative:* a separate `ViewPreferencesDataSource`. Rejected — one data source with distinct keys matches the web spec's "single dedicated key per concern" convention and keeps DI simple.

### Widget stack composable owns its own layout; ListViewModel owns its state
A new `ui/widgetstack/WidgetStack.kt` renders chevron + slots; `ListScreen` places it top-end in the existing list-area `Box`. The View Switcher slot calls the same `viewModel.onDrawerToggled()` the burger icon used. Placeholders are a sealed `WidgetStackControl` enum so later batches add real controls without layout changes.

*Alternative:* `Popup`/`DropdownMenu`-based stack. Rejected — those fight the chromeless token styling and add popup semantics we don't want; a plain `Column` in a surface-less token-styled container matches the design language.

### Chevron + expand animation
`AnimatedVisibility` (expandVertically/shrinkVertically + fade) for the control column, `animateFloatAsState` for chevron rotation. Upward-swipe collapse reuses the drag-accumulator pattern from `DrawerGestures.kt` with a small threshold.

### LED dot as a reusable composable now
`LedDot(filled: Boolean)` lands in this change (used by nothing yet except tests/preview) so batches 2–3 don't duplicate it. One-line token styling; no premature API.

## Risks / Trade-offs

- [First DataStore usage may block main thread if misused] → DataStore reads/writes on the injected IO dispatcher (existing `CoroutinesModule`); expose suspend functions + a StateFlow, never blocking calls.
- [Placeholder controls may confuse users] → Placeholders render visibly disabled (muted, non-clickable); they exist to lock the five-control order the web spec mandates.
- [Restoring active view before shops load] → Validation gate in the ViewModel (restore only after shops emit); unit-tested with a fake repository.

## Migration Plan

Single `:app` change on a feature branch; no data migration (first DataStore creation is automatic). Rollback = revert branch. The moved drawer trigger is the only user-visible change.

## Open Questions

None — control order, dot rules, and persistence keys are fully specified by the web specs (frontend-widget-stack, frontend-view-persistence) and the settled roadmap note about the burger icon.
