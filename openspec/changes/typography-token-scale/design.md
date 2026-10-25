## Context

The type scale lives in `ShoplistTypography` (5 tokens) with values in `ShoplistTheme.kt`; typography is shared across all 9 themes, so token additions touch no palettes. The ad-hoc sizes were introduced by the modal-checkbox-visual-refresh branch as a pixel-faithful port of the web modal visuals. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Every rendered text derives from a scale token; no component-level `fontSize` overrides.
- Pixel-identical outcome: tokens carry the exact current values.

**Non-Goals:**
- No changes to the 9 curated color palettes.
- No re-sizing or visual re-tuning of any text.
- No new abstraction (e.g., a size-only token system) — plain `TextStyle` fields on the existing data class.

## Decisions

- **Four new tokens, not two.** The user chose B+ (tokenize all four sizes), including the single-use stepper 15/17sp values. Rationale: a scale is a closed set; even single-use entries belong in it, and future stepper variants will reuse them. Alternative (two tokens for repeated values only) was rejected by the user.
- **Names reflect role, not size** (`micro`, `chip`, `stepperKey`, `stepperValue`), matching the existing convention (`ledDot` is a role name too). Sizes may change later without renaming call sites.
- **Weights/families baked into tokens.** Each token carries the full style currently in use (e.g., `micro` = 10sp Bold Default), so call sites use the token directly instead of `.copy(fontSize = …)`. Call sites may still adjust color via the `color` parameter, which is not part of the scale.
- **Delta spec modifies `theme-system`** because that capability owns the design-token contract; the typography scale is part of it even though the current spec text only covers palettes.

## Risks / Trade-offs

- [Scale grows to 9 entries, closer to Material's over-specified scale] → All four entries are in active use; none are speculative.
- [Robolectric/Compose tests asserting on text styles] → Run the full unit suite; existing tests assert on rendered text and semantics, not sizes.
