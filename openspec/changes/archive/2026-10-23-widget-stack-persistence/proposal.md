## Why

Roadmap 5's remaining features (sort modes, hide-checked, search restructure, badges) all hang off the web app's widget stack — a compact top-right action cluster. Porting features before the stack exists would scatter controls across the list screen. This change builds the stack scaffold and the active-view persistence, the two foundations the later batches plug into.

## What Changes

- Add a top-right floating widget stack: collapsed by default (chevron only), tap to expand downward, tap the rotated chevron to collapse; expand/collapse animated
- Move the existing temporary burger icon (drawer trigger) into the stack as the View Switcher control — per the settled roadmap note, it stays a simple trigger; the real burger menu is a future change
- Reserve the five-control order (View Switcher, Settings, Sort Mode, Checked-Items Toggle, Search) — Settings, Sort Mode, and Checked-Items Toggle render as placeholders in this change; later batches implement them
- Persist the widget stack's expanded/collapsed state across sessions
- Persist the active shop id across sessions: restore on load, validate against the fetched shop list, fall back to Global when nothing persisted or the id is invalid
- LED status dot convention established on the stack (filled = ON, hollow = OFF) — applied here to the View Switcher per the web spec's no-dot rule (dot reserved for togglable controls in later batches)

## Capabilities

### New Capabilities

- `widget-stack`: top-right floating action cluster — collapsed/expanded states with chevron animation, five-control order, LED dot convention, upward-swipe collapse, persisted expand state
- `view-persistence`: active shop id persisted across sessions with Global fallback and validation against fetched shops

### Modified Capabilities

- `list-screen`: the drawer trigger moves from the list-area top-start icon into the widget stack's View Switcher control (same toggle behavior, same accessibility)

## Impact

- `ui/list/ListScreen.kt` — new widget stack composable anchored top-end; existing burger icon removed
- `ui/list/ListViewModel.kt` + `ListUiState.kt` — expand/collapse state; active-view restore-on-load
- New `data/prefs/` (or adjacent) preferences store — first persistent local storage in the app (no Room; simple key-value per the decisions log)
- No backend changes; no new dependencies expected (DataStore is already in the catalog — verify)
