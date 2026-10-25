## MODIFIED Requirements

### Requirement: The shop modal presents name and actions

The shop modal SHALL present an underline-style name field and an action row inside a centered panel that renders with a drop shadow. The name field SHALL show a "Shop name" placeholder inside the field (vertically centered, faint content color) when empty, like a standard input placeholder. Action buttons SHALL be bordered controls with a minimum height of 44dp: Delete = danger outline, Discard/Close = quiet bordered, Save = filled accent. Disabled buttons SHALL render at reduced opacity while keeping their boundary.

#### Scenario: Shop modal panel has a drop shadow

- **WHEN** the shop modal is open
- **THEN** the modal panel renders with a drop shadow visible against the dimmed background, uniformly on all sides and not clipped by any container

#### Scenario: Shop name placeholder renders inside the field

- **WHEN** the shop modal opens with an empty name
- **THEN** a "Shop name" placeholder renders inside the input field, vertically centered, in the faint content color, and disappears as soon as the user types

#### Scenario: Shop modal action buttons are bordered

- **WHEN** the shop modal's action row is rendered
- **THEN** the action buttons render as bordered controls with a minimum height of 44dp
