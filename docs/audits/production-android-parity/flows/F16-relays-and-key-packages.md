# F16 — Relay management and key packages

## Purpose and current composition

Relays and key packages remain local projections of production concepts. Keep transport connection, publication readiness and assigned role distinct. Technical values stay developer-facing unless approved.

Prototype surface: `ui/settings/RelayDonateScreens.kt; DeveloperScreens.kt; model/ProfileSettings.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Relays**, **Where I post**, **Where I receive**, existing prototype role names, **Key Packages**, **Refresh**, **Republish**, **Publish New**, and **Delete key package**. Raw event IDs and package internals remain technical copy.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C108 · Managed-only posting/inbox relay editing | Decision required | Trigger its named entry/action; cancel with Back where available | Production only supports managed secure relays and two editable lists; mutation can clean unsupported imports. Prototype permits general URLs and three role assignments. Keep existing scope pending Q04; add validation/recovery states without silently dropping roles. |
| C109 · Relay publication readiness and refresh | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add unavailable/missing/published list projection and refresh/pending/error independently of connected/disconnected socket state. |
| C110 · Key-package entry and published/retained sections | Decision required | Trigger its named entry/action; cancel with Back where available | Production exposes Key Packages at Settings root and splits publishing/published/retained material. Prototype is developer-gated and simpler; exposure is Q04, state expansion is ready. |
| C111 · Republish, rotate/publish new, refresh and delete | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add separate actions, result/error and delete confirmation; show local/relay provenance, seen-on relays, publish time and retained-not-published explanation. |

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

Batches: B25, B28. Decisions: Q04. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B25 implementation evidence — 2026-09-04

C110/C111 is implemented and host-verified within the approved Developer Tools
gate. [The B25 brief](../../../screens/key-packages-and-developer-diagnostics.md#implementation-evidence)
records 772 passing unit tests, zero lint errors and six compiled UI cases.
Q04 remains about ordinary Settings relocation; new states preserve the existing
placement. No device or visual acceptance is claimed.
