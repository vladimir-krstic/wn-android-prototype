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
| C024 · Reorder pinned chats | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add Move up/down and order state within pinned partition, preserving non-pinned order and accessible alternatives. |
| C025 · Multi-select chats and bulk actions | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add select/select-all-visible, read/unread, archive/unarchive, folder assignment and delete where offered; reconcile disappearing rows and partial failures. |
| C026 · Delete active chat locally with leave prerequisites | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Prototype deletes only ended chats. Add active-chat delete path, including leave/sole-admin prerequisites and outcome; never remove data before required leave succeeds. |
| C027 · Create, rename, describe and delete folders | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add Folders management and New/Edit Folder. Deleting a folder leaves chats intact; blank names cannot save. |
| C028 · Folder membership and rule matching | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Manual IDs union rule matches. People OR keyword; unread/groups/archive/muted constrain only rules; empty rule does not include every chat. Add included-chat/people pickers and preview counts. |
| C029 · Folder ordering, defaults and contextual assignment | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Support default Unread/Archived/Groups as editable folders, reorder with Move actions, restore missing defaults, assign from row/bulk/info and filter chats. Keep prototype Left discoverable. |
| C030 · Connectivity and catch-up recovery on Chats | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Distinguish no internet, connecting, catching up and failure/retry from missing relay-role configuration. Preserve loaded rows while recovery runs. |

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
