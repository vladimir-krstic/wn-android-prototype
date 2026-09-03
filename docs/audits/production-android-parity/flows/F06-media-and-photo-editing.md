# F06 — Media acquisition, editing and reading

## Purpose and current composition

Extend composer attachments, rich message bodies, viewer and Shared Content. External pickers remain system-owned. Draft identity survives edit/preview/cancel, and bytes/URI failures never become a sent success.

Prototype surface: `ui/conversation/ConversationComposer.kt; MediaViewer.kt; model/ComposerModels.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Gallery**, **Take photo**, **Document**, **Location**, **User**, **Contact**, **Edit photo**, **Crop**, **Rotate**, **Draw**, **Erase**, **Undo/Redo**, **Save**, **Share**, **Open**, and format-specific unavailable/error copy. Never call a device contact a White Noise person.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C048 · Photo Picker, Files and external camera acquisition | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C049 · Recent device-media strip and partial permission access | Decision required | Trigger its named entry/action; cancel with Back where available | Add local preview strip with no/partial/full/unavailable grant states. Real library permissions require Q06; default fixture uses bundled media, while Gallery remains standard Photo Picker. |
| C050 · Photo crop, rotate, draw and erase editor | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | New editor from a selected draft photo: presets, 90-degree rotate, pen colors/widths, erase strokes, undo/redo/reset, discard, save/progress/failure and limits; preserve original and attachment identity. |
| C051 · Per-draft quality selection and metadata policy | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add Low/Standard/High/Original choices and truthful output-size/metadata treatment; Original still strips identifying EXIF. Do not claim stripping where local encoder does not do it. |
| C052 · Upload/download, cache and failed-transfer states | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Extend loading/availability fixtures with queued/active/progress/cancel/retry, cache miss and expired/invalid transfer outcomes. Keep one authoritative attachment identity through reconciliation. |
| C053 · Image/video viewer, exact page, save and share | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C054 · Animated image playback | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Prototype has GIF kinds/fixtures; add deterministic animated decoded content and fallback/error behavior instead of relying on a static GIF-labelled tile. |
| C055 · In-app text and Markdown attachment reader | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add full filename, size, loading/empty/error, bounded 512 KiB preview, unsupported encoding/truncation fallback, copy/read-aloud and Open in another app. |
| C056 · Shared-media library categories | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Existing Media/Files/Links grouping lacks separate images/video/voice views and voice library playback, grouping and fallback metadata. Extend existing Shared Content destinations. |
| C057 · Share White Noise person versus device contact | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Keep person sharing; add distinct device-contact picker, field preview and outgoing vCard/text contract. Do not equate a phone contact with a White Noise identity. |
| C058 · Location selection and location messages | Decision required | Trigger its named entry/action; cancel with Back where available | Add fixed-coordinate selection/review, current-location availability/denial, message card and Maps handoff proposal. Real location/map networking or osmdroid dependency needs Q06. |
| C059 · APK attachment open/install behavior by distribution | Decision required | Trigger its named entry/action; cancel with Back where available | Represent valid/invalid package, unsupported installer, permission-required and Play-build fallback. No real installation or new installer permission in prototype. |

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

Batches: B11, B12, B13, B14. Decisions: Q06, Q07. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
