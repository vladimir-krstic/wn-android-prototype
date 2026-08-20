# Port parity checklist

Use this checklist for each selected screen or bounded flow. It prevents both
pixel-copying iOS and shipping an Android surface that only looks complete.

## Before implementation

- [ ] The user selected this screen or bounded flow.
- [ ] An Android brief exists under `docs/screens/`.
- [ ] The relevant copied iOS brief, source, tests, and assets were inspected.
- [ ] Product outcomes, exact copy, mutations, and important states are listed.
- [ ] iOS-only presentation assumptions are identified separately.
- [ ] Current official Android guidance was opened for material platform
      decisions.
- [ ] The closest standard Compose, Material 3, AndroidX, or platform patterns
      are named.
- [ ] Any real device capability, permission, or new dependency is in scope.

## Implementation parity

- [ ] Every accepted entry point reaches the flow.
- [ ] System Back and predictive Back lead to the expected destination.
- [ ] Dismissal, cancellation, success, and destructive completion route
      correctly.
- [ ] Deterministic state transitions match the product contract.
- [ ] Empty, loading/progress, content, error, unavailable, and recovery states
      needed by the flow exist.
- [ ] Copy uses `docs/product-language.md` and `docs/terminology.md`.
- [ ] Sensitive data is absent from logs, screenshots, examples, semantics,
      and transient messages.
- [ ] Process recreation/restoration behavior is intentional and tested where
      losing state would break the task.
- [ ] The implementation adds no speculative future architecture.

## Android-native quality

- [ ] Standard components own interaction, focus, ripple/state layers, touch
      targets, and semantics wherever possible.
- [ ] No iOS toolbar, sheet, spacing, symbol, or gesture was copied literally.
- [ ] Content draws edge to edge and handles status/navigation bars, cutouts,
      gesture areas, and the on-screen keyboard.
- [ ] The flow works at compact and expanded widths without simple stretching.
- [ ] The flow survives font and display scaling, localization expansion, and
      RTL.
- [ ] TalkBack order, labels, roles, state descriptions, headings, and custom
      actions are useful and do not duplicate visible labels.
- [ ] Keyboard, mouse, touch, Switch Access, and Voice Access remain usable as
      relevant.
- [ ] Light and dark monochrome themes retain readable contrast and distinct
      semantic states.
- [ ] Motion respects system duration/reduced-motion expectations and never
      carries essential meaning alone.
- [ ] Android system surfaces are used for sharing, picking, permissions,
      authentication, and settings when applicable.

## Verification and ledger

- [ ] Relevant unit tests pass.
- [ ] Relevant Compose/instrumentation tests pass.
- [ ] Build and lint pass using commands recorded in `README.md`.
- [ ] Requested emulator or device inspection used the current build.
- [ ] Visual verification is not claimed without inspected evidence.
- [ ] `feature-inventory.md` records the exact status and evidence.
- [ ] Remaining gaps and approved Android differences are explicit.
- [ ] The user accepted the result before the item is marked **Accepted**.

