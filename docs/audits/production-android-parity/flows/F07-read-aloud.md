# F07 — Read aloud and speech settings

## Purpose and current composition

Read Aloud starts from a message or reader and owns one queue. Transport can leave the source while preserving a return target. Stop on profile/sign-out/lock boundaries. Settings are profile-owned fixtures.

Prototype surface: `ui/conversation/TimelineMessageContent.kt; ConversationScreen.kt; ui/settings/PreferenceScreens.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Read aloud**, **Pause/Resume reading**, **Stop reading**, **Previous/next sentence**, **Previous/next message**, **Return to spoken message**, **Speech rate**, **Engine**, **Voice**, and **Read messages aloud**. State when an engine/voice may use a network.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C060 · Read an authored message aloud and stop | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C061 · Speech transport queue and return to source | B15 implemented; host verified | Existing message/reader actions; Back keeps navigation; Stop ends queue | Shared foreground sentence/message transport, paused navigation, bounded history/retry and source-validated return. Profile/sign-out/background changes cancel playback and stale return. |
| C062 · Speech passage highlighting and seeking | B15 implemented; host verified | Native selection, Read from here or Choose sentence; manual scroll suspends follow | Exact authored/Markdown offsets, optional word timing with chunk fallback, pause-frozen progress, accessible sentence choice and Resume following. Speech positioning does not acknowledge unseen messages. |
| C063 · Engine and offline voice selection with availability | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | New Read Aloud settings: discovering/none/usable, per-engine voice, offline-only eligibility, saved voice fallback, unknown engine trust consent and settings recovery. |
| C064 · Speech rate and media mixing | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | System/preset/custom rate plus explicit speech-over-media setting and quiet/medium/loud; deterministic audio-focus/other-media states without background services. |
| C065 · Global and per-chat automatic reading | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Global default plus per-chat inherit/on/off and arrival cursor. Suppress duplicates, history replay and reading after lock/sign-out. |
| C066 · Background speech and controls at lock boundaries | Decision required | Trigger its named entry/action; cancel with Back where available | Specify local lifecycle state and notification-control fixture; real foreground playback/service is an explicit Q06 expansion. |

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

Batches: B15, B16. Decisions: Q06. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B15 implementation evidence

The [selected brief](../../../screens/read-aloud-transport.md#implementation-evidence)
records the queue/controller/transport/document/navigation seams, 33 new host
rules and 15 compiled UI/platform cases. The host gate passes 465 unit tests,
zero lint errors and both APKs. One foreground engine now serves conversation
and file readers. File speech remains reader-scoped; ordinary navigation can
retain a conversation queue. The local profile-change cancellation contract
supersedes production's optional account-switch resolver. Engine preferences,
auto-read and background-control fixtures remain B16. No device/visual result
or background-service capability is claimed.
