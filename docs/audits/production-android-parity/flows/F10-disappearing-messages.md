# F10 — Disappearing messages and expiration

## Purpose and current composition

Timer changes are admin-only and destructive consequences are named before commit. Fixed clocks/read anchors make expiry reproducible. Removed content disappears consistently from every projection.

Prototype surface: `model/ChatModels.kt; state/AppViewModel.kt; ui/conversation/ChatInfoScreens.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Disappearing messages**, **Off**, exact duration labels, **Custom time**, **Set timer**, and a consequence-specific confirmation. Avoid “after seen” until Q03 identifies the actual countdown anchor.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C081 · Expanded disappearing timers and custom duration | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add production presets and bounded units from seconds through years with validation and read-only member state. |
| C082 · Timer-change pruning and consequence confirmation | Decision required | Trigger its named entry/action; cancel with Back where available | Production explicitly prunes older plaintext on timer change; prototype sets a value/event. Confirm retroactive consequences and reconcile production after-seen help with actual update semantics before final copy. |
| C083 · Message expiration indicators and cleanup | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Use fixed clock/read anchors for expiry, remaining-time semantics and removal; reconcile selection/search/reply/media/TTS when a message expires. |

## Production integration seam

Production evidence for each row is linked in the [matrix](../capability-matrix.md). During prototype work, add the smallest profile-owned immutable fixture/state transitions and callbacks needed to render every named result. Do not add Marmot, networking, signing, persistence, notification delivery, background services or cryptography. Name production events and ownership in the selected screen brief so the eventual migration reconnects to the cited controller/state methods rather than copying prototype fixtures into production storage.

## Copy, accessibility and adaptation

Use the approved product language and terminology. Production strings in the matrix are evidence of meaning, not automatic final copy. Keep raw keys, event IDs, MLS and engine errors off ordinary surfaces; developer surfaces may be exact. State must be conveyed by text/semantics as well as icon/color. Provide accessible equivalents for gestures, logical focus and Back order, and preserve action eligibility at large type and narrow height.

## Acceptance and host validation

- Every linked capability has a deterministic route/fixture and every mutation yields the specified success, cancellation, unavailable and failure outcomes relevant to it.
- Back, profile switching and restored state cannot commit work to the wrong profile/chat or repeat a completed mutation.
- Existing capabilities in this flow retain their current model and UI tests.
- Add unit tests for rules/ownership and Compose tests for durable navigation/actions/semantics. Run targeted host tests while iterating and the repository static gate after a meaningful batch. Compile instrumentation tests only; device execution and visual acceptance require a separate current request.

## Dependencies and decisions

Batches: B20. Decisions: Q03. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
