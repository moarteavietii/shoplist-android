## Purpose

Turn remote item changes into a glanceable, self-replacing toast at the top of the screen, so users of a shared list see what others did without watching the list.

## Requirements

### Requirement: Remote item changes produce change toasts
When an item change arrives via realtime sync from a device other than this one, the app SHALL display a toast describing the change. The following changes SHALL produce toasts with these labels:

- item checked → `checked: <name>`
- item unchecked → `unchecked: <name>`
- item created → `new item: <name>`
- item deleted → `deleted: <name>`
- item renamed → `<old name> → <new name>`
- item quantity changed → `quantity: <old> → <new> <name>`
- item no longer visible in a shop → `<name> removed from <shop name>`
- item newly visible in a shop → `<name> added to <shop name>`

Reorder changes SHALL NOT produce toasts. An update that changes several fields at once SHALL produce one toast per distinct change kind, queued and displayed one at a time in the order the change kinds were derived, each replacing the previous one. Duplicate or no-op change events (an update whose applied state matches the cached item) SHALL NOT produce toasts.

Toasts SHALL render in the unified notification style defined by the `notification-surface` capability: a top-center inverse pill with a severity LED, soft shadow, and single-line text. Change toasts SHALL use the standard (informational) severity LED.

#### Scenario: Remote check-off shows toast
- **WHEN** another device checks an item named "Bread"
- **THEN** this device shows a toast reading "checked: Bread" in the unified notification pill style

#### Scenario: Remote uncheck shows toast
- **WHEN** another device unchecks an item named "onion"
- **THEN** this device shows a toast reading "unchecked: onion"

#### Scenario: Remote creation shows toast
- **WHEN** another device creates an item named "oranges"
- **THEN** this device shows a toast reading "new item: oranges"

#### Scenario: Remote delete shows toast with pre-delete name
- **WHEN** another device deletes an item named "Bread"
- **THEN** this device shows a toast reading "deleted: Bread"

#### Scenario: Remote rename shows transition label
- **WHEN** another device renames an item from "Bread" to "Bread rolls"
- **THEN** this device shows a toast reading "Bread → Bread rolls"

#### Scenario: Remote quantity change shows toast
- **WHEN** another device changes an item's quantity from 2 to 3
- **THEN** this device shows a toast reading "quantity: 2 → 3 <name>"

#### Scenario: Remote visibility removal shows shop label
- **WHEN** another device updates an item so it is no longer visible in shop "Carrefour"
- **THEN** this device shows a toast reading "<name> removed from Carrefour"

#### Scenario: Remote visibility addition shows shop label
- **WHEN** another device updates an item so it becomes visible in shop "Auchan"
- **THEN** this device shows a toast reading "<name> added to Auchan"

#### Scenario: Reorder produces no toast
- **WHEN** another device reorders items
- **THEN** no toast is shown on this device

#### Scenario: Multi-field update produces sequential toasts
- **WHEN** another device checks an item and renames it in one update
- **THEN** this device shows a "checked: …" toast followed by a rename toast, one at a time

#### Scenario: No-op update produces no toast
- **WHEN** an `item:updated` event arrives whose applied state matches the cached item
- **THEN** no toast is shown

### Requirement: Own changes do not produce toasts
Changes echoed back to the device that originated them SHALL NOT produce toasts. The device identifies its own events via the `originDeviceId` carried on SSE events, matched against its persistent device id.

#### Scenario: Own check-off echo is silent
- **WHEN** this device checks an item and the corresponding SSE event echoes back with this device's `originDeviceId`
- **THEN** no toast is shown

#### Scenario: Another device's change shows a toast
- **WHEN** an SSE event arrives with a different `originDeviceId`
- **THEN** the corresponding toast is shown

### Requirement: Single self-replacing toast
At most one change toast SHALL be visible at a time. A new change SHALL replace the currently visible toast's content and restart its auto-dismiss timer. Replacing the content SHALL NOT replay the entrance animation — the toast SHALL update its text in place.

#### Scenario: New change replaces current toast
- **WHEN** a toast is visible and another change arrives
- **THEN** the toast content is replaced and the dismiss timer restarts

#### Scenario: Replacement does not replay the entrance animation
- **WHEN** a toast is visible and another change arrives
- **THEN** the toast text changes in place without sliding in again

#### Scenario: Only one toast visible
- **WHEN** several changes arrive in quick succession
- **THEN** only the most recent toast is visible at any moment

### Requirement: Toast auto-dismisses
A change toast SHALL dismiss itself automatically after approximately 3.5 seconds.

#### Scenario: Toast disappears without interaction
- **WHEN** a toast is shown and the user does nothing
- **THEN** the toast is gone after roughly 3.5 seconds

### Requirement: Toast is swipe-dismissible
Swiping the toast horizontally SHALL dismiss it immediately. The dismissal animation SHALL carry the toast out in the direction of the swipe. Non-swipe dismissals (auto-dismiss, programmatic) SHALL use the default fade-out.

#### Scenario: Horizontal swipe dismisses toast
- **WHEN** the user drags the toast horizontally beyond a small threshold and releases
- **THEN** the toast dismisses immediately

#### Scenario: Swipe-out follows the swipe direction
- **WHEN** the user swipes the toast to the right past the threshold
- **THEN** the toast animates out toward the right side
- **WHEN** the user swipes the toast to the left past the threshold
- **THEN** the toast animates out toward the left side

#### Scenario: Small touch does not dismiss
- **WHEN** the user touches the toast and releases without a horizontal drag beyond the threshold
- **THEN** the toast remains visible until auto-dismiss

### Requirement: Toast entrance animation
A change toast SHALL appear with a brief slide-in from the top of the screen. The entrance animation SHALL play only when the toast appears when none was visible — a replacement toast SHALL NOT replay it.

#### Scenario: Toast slides in on first appearance
- **WHEN** a change toast appears while no toast was visible
- **THEN** it slides in from the top into its resting position

#### Scenario: Entrance plays once, not on replacement
- **WHEN** a toast is visible and another change replaces its content
- **THEN** the entrance animation does not play again

### Requirement: Toast respects the active theme
The toast SHALL render using the active theme's tokens (surface, text, accent) and SHALL meet WCAG AA contrast for its text.

#### Scenario: Toast follows theme change
- **WHEN** the user switches themes
- **THEN** a subsequently shown toast uses the new theme's tokens

### Requirement: Change toasts honor the enabled preference
The app SHALL persist a change-toasts-enabled preference (default: enabled). When disabled, remote item changes SHALL NOT produce toasts. The preference SHALL take effect immediately without a restart, and it SHALL affect only toast display — it SHALL never tear down, reopen, or otherwise disturb the realtime SSE connection. The settings-drawer toggle UI is deferred to the themes/settings work; the preference is honored by the toast layer from this change onward.

#### Scenario: Disabled preference silences toasts
- **WHEN** the preference is disabled and another device checks an item
- **THEN** no toast is shown

#### Scenario: Preference takes effect immediately
- **WHEN** the preference changes while the list is mounted
- **THEN** subsequent remote changes produce toasts or not according to the new value, without a restart

#### Scenario: Toggling does not disturb the realtime connection
- **WHEN** the change-toasts preference changes
- **THEN** the SSE connection stays open and realtime list updates continue uninterrupted

#### Scenario: Preference state persists
- **WHEN** the preference is turned off and the app restarts
- **THEN** the preference is still off
