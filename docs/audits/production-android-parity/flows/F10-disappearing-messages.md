# F10 — Disappearing messages and expiration

B20/C081–C083 are implemented and host-verified. The
[selected brief](../../../screens/disappearing-timers-and-expiry.md#implementation-evidence)
records exact behavior, source seams, 655 passing unit tests and nine compiled UI
cases. Device and visual acceptance remain pending.

## Purpose and current composition

Timer changes are admin-only and destructive consequences are named before commit. Fixed clocks/read anchors make expiry reproducible. Removed content disappears consistently from every projection.

Prototype surface: `model/ChatModels.kt; state/AppViewModel.kt; ui/conversation/ChatInfoScreens.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Disappearing messages**, **Off**, exact duration labels, **Custom time**, **Set timer**, and a consequence-specific confirmation. Q03 is resolved: received messages start on first read; outgoing messages start at send. Explain confirmed timer-change pruning separately.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C081 · Expanded disappearing timers and custom duration | Current preset/custom value | Stage choices; Save accepts, Back/Cancel discards | Nine presets, bounded seconds–years validation and read-only member help are implemented. |
| C082 · Timer-change pruning and consequence confirmation | Current policy and authoritative role | Enable/shorten confirms pruning; Cancel preserves history | Owned apply and accepted refresh failure/retry are distinct. Off/longer does not prune. Q03 resolved. |
| C083 · Message expiration indicators and cleanup | Waiting/read/send countdown | First read anchors once; foreground fixed clock expires due rows | Remaining-time semantics and canonical cleanup reconcile projections, reply/selection, media/readers, forwarding/export and speech. |

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

Batches: B20 complete. Decisions: Q03 resolved (WN-ANDROID-0140). Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
