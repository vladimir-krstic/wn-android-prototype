# F08 — Dictation and voice recording

## Purpose and current composition

Voice note and dictation are distinct modes competing for microphone ownership. Back/cancel never sends. Guard automatic send by the original profile/chat/draft revision.

Prototype surface: `ui/conversation/ConversationComposer.kt; model/ComposerModels.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Record voice message** for audio and **Dictation** for speech-to-text. Dictation states are **Preparing**, **Listening**, **Transcribing**, and **Review dictated text**; recovery actions are **Retry**, **Open Settings**, **Insert at end**, **Copy**, and **Discard**.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C067 · Voice-note recording, cancel, lock and review | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Existing deterministic record/review is retained. Add production hold/release/lock/cancel ownership, too-short/mic-in-use/permission/failure states where absent, with an accessible tap alternative. |
| C068 · Dictation into draft | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Separate dictation from a voice-note transcript. Add service-check/listening/processing/review/error/cancel states and exact destination draft ownership. |
| C069 · Dictation completion and guarded automatic send | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | New Dictation settings: manual or silence completion and paste versus explicit opt-in send. If draft, membership or session changes, preserve transcript for review; never send to a new context. |
| C070 · Selected speech service and failure recovery | Decision required | Trigger its named entry/action; cancel with Back where available | Model selected service missing/busy/network/timed out and denial/permanent denial, then Copy/Insert at end/Discard review. Real recognition/microphone service requires Q06. |

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

Batches: B17. Decisions: Q06. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B17 implementation evidence

C067–C070 are covered within the deterministic capture boundary on 2026-09-04.
The [selected brief](../../../screens/dictation-and-voice-recording.md#implementation-evidence)
records inline editable-draft dictation, profile-owned settings, revision-guarded
Paste/Send, retained review and exact selected-service recovery. Voice includes
owned hold/release-to-review/RTL cancel/lock, native tap-to-lock and typed errors.
550 unit tests pass; lint has zero errors; both APKs assemble; ten new UI cases
compile only. Q06 real microphone/recognizer/service and device/visual acceptance
remain separate. The table above is the original audit contract.
