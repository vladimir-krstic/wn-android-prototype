# F18 — Help, diagnostics and distribution updates

## Purpose and current composition

Help/About external links use Android intents; diagnostics stay sanitized; updates are distribution-gated fixtures. Store-managed builds expose no off-store update path.

Prototype surface: `ui/settings/SupportScreen.kt; DeveloperScreens.kt; RelayDonateScreens.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Help**, **Report a bug**, **About & licenses**, **Open source licenses**, **Privacy policy**, **Diagnostics**, **Send to self**, **Performance logs**, **Update available**, **Download**, **Install**, **Open Settings**, **Retry**, and **Cancel download**.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C116 · Support contact and donation presentation | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C117 · Help, bug report, About, licenses and privacy policy | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add Help and About destinations, version/build rows, safe external links and license listing. Bug report previews must exclude messages, keys and logs unless separately selected. |
| C118 · Developer unlock and diagnostics/stream controls | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Production version-tap unlock and inline stream debugging differ from prototype enabled toggle/debug snapshots. Preserve approved developer entry while adding explicit streaming-event controls. |
| C119 · Diagnostics health, send-to-self and timed performance logs | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add loading/error refresh, connection attempts/successes, self-send outcome and 30-minute performance-logging state; retain sanitized copy boundary and no telemetry/network execution. |
| C120 · Update availability and version warning | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add version/checking/failure/current/available states in Chats and Settings and source-defined dismissibility. Store-managed fixture must have no in-app update entry. |
| C121 · Verified APK update download/install state flow | Decision required | Trigger its named entry/action; cancel with Back where available | Model resolve/confirm/download/verify/ready/install-permission/error/retry/cancel without fetching APKs or invoking installer. Keep verification distinct from download completion. |
| C122 · Protocol/database/push/crypto production infrastructure | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Explicit prototype boundary excludes real services, signing, encryption and persistence. User-visible states are mapped above; production migration must reuse Marmot/SQLite authority, not copy fixture state as a second database. |

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

Batches: B25, B31, B32. Decisions: Q06. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B25 implementation evidence — 2026-09-04

C118/C119 is implemented and host-verified within the approved Developer Tools
gate. [The B25 brief](../../../screens/key-packages-and-developer-diagnostics.md#implementation-evidence)
records 772 passing unit tests, zero lint errors and six compiled UI cases.
Q04 remains about ordinary Settings relocation; new states preserve the existing
placement. No device or visual acceptance is claimed.
