# F13 — Device privacy and sensitive diagnostics

## Purpose and current composition

Privacy settings and lock screen use semantic error/recovery states. Never reveal chats behind lock or place sensitive exports in transient logs. Back cannot bypass authentication.

Prototype surface: `ui/settings/PreferenceScreens.kt; DiagnosticsImprovementsScreen.kt; WhiteNoiseApp.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **App lock**, **Lock when**, **Incognito keyboard**, **Hide Screen in Recents**, proposed **Block screenshots in chats**, **Audit logs**, **Export audit logs**, and **Delete audit logs**. Sensitive-export copy names message, identity and device data.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C095 · App lock return flow and retry | Decision required | Trigger its named entry/action; cancel with Back where available | Prototype configures device-authentication preference but lacks lock surface/session gating. Add deterministic locked/authenticating/cancel/failure/unlocked states, background timing and no-data flash. |
| C096 · Incognito keyboard preference | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add learning-request preference and coverage for relevant app-owned inputs; wording must say request, not guarantee about keyboard behavior. |
| C097 · Screenshot security versus Recents privacy | Decision required | Trigger its named entry/action; cancel with Back where available | Production combines screenshot blocking and Recents with secure windows; prototype explicitly promises Recents-only privacy. Preserve current setting and propose a separate screenshot option pending decision. |
| C098 · Sensitive audit-log recording/export/delete | Decision required | Trigger its named entry/action; cancel with Back where available | Existing diagnostic exports are sanitized. Production forensic files may contain message/identity/device data. Keep consent, sensitive export and sanitized diagnostics separate; do not reuse reassurance copy. |

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

Batches: B24. Decisions: Q02, Q06, Q08. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
