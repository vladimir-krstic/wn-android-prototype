# F15 — Data usage and automatic downloads

## Purpose and current composition

Data controls are profile-owned deterministic settings plus queued-transfer fixtures. Network classes may overlap, so the most restrictive matching rule decides. Manual downloads remain available.

Prototype surface: `ui/settings/PreferenceScreens.kt; model/ProfileSettings.kt; model/ComposerModels.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Data and storage**, **Auto-download media**, network labels, media-type labels, **Download queue**, **Stop automatic downloads**, **Restart automatic downloads**, and quality labels. Explain that manual/active downloads continue when the automatic queue stops.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C105 · Per-type/per-network auto-download matrix | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Replace limited policy model with four media types across Wi-Fi/mobile/roaming/metered; most restrictive active condition wins, unknown network does not auto-download. Preserve approved grouping. |
| C106 · Stop/restart queued automatic downloads | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Clear queued automatic work only; active and explicitly tapped transfers continue. Model per-profile queue/pause and recovery with no networking. |
| C107 · Global Low/Standard/High/Original media quality | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add missing quality modes, resulting photo/voice policy and metadata explanation. Do not promise video re-encoding: production sends selected videos/audio as-is. |

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

Batches: B27. Decisions: None. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B27 implementation evidence — 2026-09-04

C105–C107 are implemented and host-verified. The accepted title remains **Data
Usage**, with Photos/Videos/Audio/Files groups and independent network switches.
The earlier Data and storage/Auto-download media labels above were audit
recommendations, superseded by the [selected brief](../../../screens/downloads-and-media-quality.md).
The canonical attachment queue supports admission, stop/restart, promotion,
per-file cancellation, failure/retry and exact profile/revision ownership.
Settings edits cannot overwrite queue pause state. Low/Standard/High/Original
feed future photo and voice policy while preserving prepared media. Videos/audio
files are sent as-is. The full host gate passes 791 unit tests, zero lint errors
and both APKs; six added UI cases compile only. Visual acceptance remains pending.
