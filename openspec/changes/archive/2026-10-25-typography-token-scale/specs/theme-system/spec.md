## MODIFIED Requirements

### Requirement: Text styling comes from the typography token scale
All text in the app SHALL be styled from the `ShoplistTypography` token scale: display (56sp), title (20sp), body (16sp), label (12sp), ledDot (12sp Monospace), micro (10sp Bold), chip (13sp SemiBold), stepperKey (15sp Bold), and stepperValue (17sp Bold). Components SHALL NOT introduce ad-hoc `fontSize` values outside this scale. Component-level refinements SHALL be limited to non-size attributes (weight, family, color) unless a new scale entry is added through a spec change.

#### Scenario: Token scale covers all rendered text
- **WHEN** any screen renders text
- **THEN** the text style derives from one of the nine scale tokens, with no component-level `fontSize` override

#### Scenario: Micro and chip tokens match the ported modal visuals
- **WHEN** the Add Item modal, View Drawer badges, or Quick Menu badge render small text
- **THEN** the sizes equal the previously ad-hoc values (10sp for badges/counts, 13sp for chip labels), so the visuals are unchanged from the web port

#### Scenario: Stepper text matches the ported modal visuals
- **WHEN** the quantity stepper renders its keys and value
- **THEN** the sizes equal the previously ad-hoc values (15sp keys, 17sp value), so the visuals are unchanged from the web port
