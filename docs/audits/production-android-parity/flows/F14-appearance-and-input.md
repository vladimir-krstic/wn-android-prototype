# F14 — Appearance, text preferences and keyboard behavior

## Purpose and current composition

Appearance changes retain semantic Material roles and compose with system font/display scaling. Defaults stay the approved monochrome prototype direction while optional capabilities are decided.

Prototype surface: `ui/settings/PreferenceScreens.kt; model/ProfileSettings.kt; ui/theme/`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Appearance**, **Theme mode**, **AMOLED**, **Font size**, **App font**, **Language**, **Enter key behavior**, **Send message**, and **New line**. Color customization labels remain decision-dependent; default surfaces stay monochrome.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C099 · System/light/dark appearance | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C100 · AMOLED appearance option | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add optional pure-black theme using semantic roles, with contrast/state distinction and no default palette replacement. |
| C101 · Font family and in-app font size | Decision required | Trigger its named entry/action; cancel with Back where available | Add System/Manrope/Outfit/Urbanist/Figtree choices and 0.85/1/1.15/1.3 scale composed with system scale; font resources/provenance and approved default require Q09. |
| C102 · Accent color and global/per-chat bubble color overrides | Decision required | Trigger its named entry/action; cancel with Back where available | Production has full-spectrum/hex customization by theme/profile and bubble side. This conflicts with monochrome identity; keep full capability recorded, block colored implementation pending Q01. |
| C103 · Enter sends versus newline | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add input preference and IME/hardware Enter behavior while preserving multiline/dictation and accessible Send. |
| C104 · Additional language choices and real localization | Decision required | Trigger its named entry/action; cancel with Back where available | Prototype offers a different language set and in-memory selection; add Russian/Turkish/Simplified/Traditional Chinese and determine translation scope Q09. Preserve existing Serbian and other prototype choices. |

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

Batches: B26. Decisions: Q01, Q09. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
