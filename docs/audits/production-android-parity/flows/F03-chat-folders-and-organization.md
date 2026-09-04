# F03 — Chat organization and folders

## Purpose and current composition

Enter at Chats and Settings → Folders. Back exits selection before search, search before folder scope, and editor changes with discard protection. Folder membership is per profile and locally derived.

Prototype surface: `ui/chats/ChatsScreen.kt; model/ChatModels.kt; model/ChatListPresentation.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Chats**, **Folders**, **New folder**, **Edit folder**, **Add to folder**, **Included chats**, **People**, **Restore default folders**, **Select all**, **Move up/down**, and exact archive/delete verbs. Folder deletion copy must say chats remain.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C022 · Chat list previews, unread, archived and ended states | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C023 · Pin, mute, read/unread, archive per chat | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C024 · Reorder pinned chats | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Move Up/Down and accessible actions reorder only pinned chats with bounded availability. Unpinned original order survives moving, unpinning and archiving. |
| C025 · Multi-select chats and bulk actions | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Select and select-all-visible provide read/unread, archive/unarchive, folder assignment and confirmed deletion. Selection reconciles to visible rows, survives recreation, resets by profile and retains only failed targets; retries skip successes. |
| C026 · Delete active chat locally with leave prerequisites | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Active/ended local deletion is available without leaving. Optional Also leave enforces sole-admin and authoritative membership prerequisites before history removal, with explicit leave/local failure results and stage-aware retry. |
| C027 · Create, rename, describe and delete folders | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Folders management and draft-only New/Edit provide name, description, manual membership, atomic Save, discard protection and failed-save retention. Blank names and missing/stale targets cannot save; deleting any folder preserves chats. |
| C028 · Folder membership and rule matching | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Manual chat IDs union automatic matches. People OR title/group-description keyword matches, narrowed by unread/groups/archive/mute; empty rules match nothing. Searchable Included Chats/People pickers and live counts/Preview expose the result, including manual exceptions. |
| C029 · Folder ordering, defaults and contextual assignment | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Editable Unread/Archived/Groups defaults support Move Up/Down with TalkBack equivalents and restore-only-missing behavior. Row/bulk/Chat Info assignment and derived folder filters are connected. Chats/Left remain available; deleted selected folders fall back to Chats. |
| C030 · Connectivity and catch-up recovery on Chats | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Offline, Connecting, Catching up and Failed/retry preserve loaded rows/drafts. Profile/generation guards reject stale readiness. Relay-role configuration and availability have separate Check Relays recovery. |

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

Batches: B05, B06. Decisions: None. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B05 implementation evidence

C024–C026 and C030 are implemented and host-verified 2026-09-04. The
[selected brief](../../../screens/chat-organization-and-recovery.md#implementation-evidence)
records selection, bulk actions and failed-target retry; pinned ordering;
local-only versus optional leave-first deletion; and owned recovery phases.
The clean gate passed 262 unit tests, both APKs and zero lint errors. Seven UI
cases compile but were not executed. B05 introduced manual creation, assignment and filtering. B06 subsequently
completed C027–C029 as recorded below.

## B06 implementation evidence

C027–C029 are implemented and host-verified 2026-09-04. The
[selected brief](../../../screens/chat-folders.md#implementation-evidence)
records atomic New/Edit with discard protection, rule/preview semantics,
editable defaults, restore/order, derived filters and all assignment entry
points. The clean gate passed 277 unit tests, both APKs and zero lint errors.
Nine new UI cases compile; device execution and visual acceptance remain separate.
