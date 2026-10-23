## Context

The visibility feature is already implemented on this branch: `Item.visibleIn` (data model), the add/edit modal checklists (`AddItemModal.kt`), selection state and save-diffing (`ListViewModel.kt`, `EditItemUiState`), view filtering (`ListViewModel` line ~643), and Global-first ordering (`orderViewsGlobalFirst`). What is missing is the governing capability spec and verification that the implementation matches it. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Formalize the `item-visibility` contract as a spec so it can be archived and synced into `openspec/specs/`.
- Verify each spec requirement against the current implementation; fix residual gaps with the smallest possible change.
- Add unit-test coverage for selection state, save diffing, and the toggle-all rules.

**Non-Goals:**
- No backend changes; the REST + SSE contract is reused as-is.
- No redesign of the modal UI beyond what the spec requires.
- No per-shop order-key management in the modal (reordering keys are handled by `drag-drop-reorder`).

## Decisions

- **Spec-first, verify-then-fix.** The implementation exists; the change's job is to make its contract explicit and close gaps. Alternative — rewrite the UI from the spec — was rejected as scope creep for working code.
- **One capability, not deltas to `quick-add`/`list-screen`.** The edit-modal selector is a distinct behavior surface; `quick-add` keeps creation-flow requirements and `list-screen` keeps filtering requirements. The new spec references those behaviors without duplicating them.
- **Save-diffing stays in the ViewModel.** `EditItemUiState.canSave` already computes "changed vs persisted" including the selection; the spec requires exactly this, so no state-shape change is needed.
- **Toggle-all rules mirror quick-add.** Select-all selects everything; deselect-all keeps Global. This matches the existing `onEditItemToggleAllTapped` behavior and the web app's semantics, so no divergence is introduced.

## Risks / Trade-offs

- [Implementation drift from the web app's modal (e.g. Close/Discard labeling)] → Verification tasks compare against the web spec; gaps are fixed or the spec is adjusted before archive.
- [SSE `item:updated` echo races the PATCH response] → Already mitigated by the duplicate-key fix on this branch; the spec's remote-propagation scenario is covered by existing realtime tests.

## Migration Plan

Not applicable — single-module app change, no data migration. Rollback is reverting the branch.

## Open Questions

None.
