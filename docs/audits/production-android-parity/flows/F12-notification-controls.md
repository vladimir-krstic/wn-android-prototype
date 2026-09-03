# F12 — Notifications and conversation alert rules

## Purpose and current composition

Global settings own enablement; chat details own overrides; Android owns channel/permission UI. Present requested, effective and unavailable states separately.

Prototype surface: `ui/settings/PreferenceScreens.kt; NotificationPermission.kt; ui/conversation/ChatInfoScreens.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Notifications**, **Local notifications**, **Keep connected**, **Push notifications**, **Defaults for all chats**, **Sounds & notifications**, **Notify for**, **Vibration pattern**, **Mute until**, and **Open Settings** only when that resolves the state.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C089 · Local/push switches and permission recovery | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Existing permission/push UI needs Play-services/build-configuration availability and asynchronous enable failure/revert states. |
| C090 · Keep connected in the background | Decision required | Trigger its named entry/action; cancel with Back where available | Add preference, permission dependency, rejected service/retry and stopped state; local model only unless Q06 approves actual service. |
| C091 · Android notification categories and global defaults | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add category rows for direct/group/mentions/reactions/invites/membership/agent activity and distribution-gated updates. Android owns category settings; do not imitate system screens. |
| C092 · Per-chat notify mode, custom category and vibration | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | New Sounds & notifications detail: all/mentions/nothing, inherit/custom category, effective Android override, selected vibration and preview state. |
| C093 · Custom mute-until and restoration of previous notify mode | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add custom date/time, expiry and restore previous all/mentions choice. Keep approved immediate-choice Material dialog for existing presets. |
| C094 · Notification tap, inline reply/reaction/mark-read | Decision required | Trigger its named entry/action; cancel with Back where available | Model message/invite route and account ownership, inline action pending/failure/retry, exactly-once result and read-through boundary; live notifications require Q06. |

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

Batches: B22, B23. Decisions: Q06. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
