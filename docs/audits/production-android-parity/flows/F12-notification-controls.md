# F12 — Notifications and conversation alert rules

B22/C089–C093 are implemented and host-verified within local scope. The
[selected brief](../../../screens/notification-controls.md#implementation-evidence)
records 714 passing unit tests, ten compiled UI/platform cases and production
integration seams. Q06 excludes real delivery/services/publication/vibration.
B23/C094 is implemented and host-verified: [notification routing/action evidence](../../../screens/notification-routing-and-actions.md#implementation-evidence) records 748 passing unit tests and seven compiled UI cases. Device and visual acceptance remain pending.

## Purpose and current composition

Global settings own enablement; chat details own overrides; Android owns channel/permission UI. Present requested, effective and unavailable states separately.

Prototype surface: `ui/settings/PreferenceScreens.kt; NotificationPermission.kt; ui/conversation/ChatInfoScreens.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Notifications**, **Local notifications**, **Keep connected in the background**, **Native push**, **Notification categories**, **Sounds & notifications**, **Notify for**, **Vibration pattern**, **Mute until**, and **Open Settings** only when that resolves the state.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C089 · Local/push switches and permission recovery | Saved profile preferences and effective permission/provider state | Choose delivery; pending/cancel/retry | Availability gates and asynchronous rollback preserve stored choices and origin-profile ownership. |
| C090 · Keep connected in the background | App-wide background preference | Enable local delivery first, then connection; retry rejection or stop | Accepted local enabling is retained after service rejection. Switching profiles keeps the global preference; app erase clears it. Real service remains Q06. |
| C091 · Android notification categories and global defaults | Global categories and distribution availability | Open Android category settings | Exact existing category targets or explicit app-settings fallback/unavailable outcomes; no imitation system screen or channel publication. |
| C092 · Per-chat notify mode, custom category and vibration | Saved Notify for, mute, scope and vibration | Select modes/scope; stage vibration; save, preview or cancel | All/Mentions survives effective Nothing while muted. Primary/custom/global scope and Android override projections are independent and owner-checked. |
| C093 · Custom mute-until and restoration of previous notify mode | Existing preset/custom mute or unmuted state | One-tap presets or native date/time; Back/cancel preserves | Future-time acceptance and shared foreground expiry update canonical mute state and restore the saved choice. |
| C094 · Notification tap, inline reply/reaction/mark-read | Owned local route/action and card generation | Tap, act, defer/unlock, cancel, Retry or Done | Message/invite/role/list entry, bounded invitation probes, exact read-through, accepted action proof and cleanup-only retry preserve drafts, profile ownership and newer arrivals. Live delivery/workers remain Q06. |

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

Batches: B22 complete locally; B23 pending. Decisions: Q06 retains real platform delivery/services/publication/vibration outside scope. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
