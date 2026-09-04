# Chat organization and recovery — B05

2026-09-04: B06 completes the folder foundation described here. See
[chat-folders.md](chat-folders.md) for editable defaults, rules and management.

Selected by the user's all-batches goal, 2026-09-04. Capabilities C024, C025,
C026 and C030. Implemented and host-verified; device/visual acceptance not claimed.

## Contract

Preserve the accepted Chats row, header, search, filters, FAB and anchored menus.
Add Select to the row menu and accessibility actions. Selection shows native
checkboxes and a contextual app bar with count, close, select-all-visible and
bulk read/unread, archive/unarchive, folder assignment and Delete. Mixed archive
selection offers Archive; all archived offers Unarchive. Back exits selection
before search. Selection is profile-owned, saved across recreation, reconciled
to visible rows and cleared after successful operations; failed targets remain.

Move Up/Down changes only the pinned partition with bounded menu and TalkBack
actions. Original unpinned ordering survives moves, unpinning and archiving.

Delete removes local history by default, matching the production row and bulk
call to deleteGroupLocalFromChatList. An unchecked Also leave option offers the
separate production leave-first path. A sole admin with remaining members must
promote another admin first; Open Group leads to the existing management screen.
A group with only oneself can be removed without a leave operation. Failed leave
preserves history. A successful leave followed by failed deletion preserves the
Left state and history; retry skips leave. Requests freeze owner and target IDs,
report per-target results, reject stale completion, and never reapply successes.

Folder assignment has a native picker with an inline New Folder name and Save;
blank names cannot save. Profile-owned manual folders become Chats filters.
This is C025's usable foundation; B06 owns full editing, defaults and rule logic.

Connectivity distinguishes Offline, Connecting, Catching up and Failed with
Retry. Cached rows/drafts stay available. Only the current profile/generation
may advance recovery. Missing Chat Messages relay configuration has its own
Check Relays action and is never described as no internet. Developer Tools owns
one-shot operation failures and deterministic connection scenarios.

## Native presentation and scope

Use shared Material menus, checkbox semantics, contextual TopAppBar, scrollable
AlertDialogs and existing adaptive pane/spacing. Selection rows retain the
measured text-layout crash fix and their whole-row click target. Status and
progress have text and live-region semantics; actions remain accessible without
long press or dragging. Dialog Back cancels confirmation; active short stages
finish before a dismissible report. No new permissions, network, durable storage,
backend or dependency. No device execution authorized.

Official sources checked 2026-09-04:
[Checkbox](https://developer.android.com/develop/ui/compose/components/checkbox),
[app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
[side effects](https://developer.android.com/develop/ui/compose/side-effects).

Production evidence is read-only whitenoise-android@319454889f1c2494dec4a69b5577d98017f44eee:
ui/chats/ChatListSelectionActions.kt, ChatActionSheet.kt, ChatsScreen.kt,
ChatFolderPickerSheet.kt, ConnectivityBannerState.kt and state/Controllers.kt.
Baseline iOS source and approved layout are in [Chats brief](chats-and-chat-creation.md)
and [source map](../port/source-map.md). This batch extends the earlier exclusion
of extra menu commands with explicit user authorization; it does not copy Signal.

## Validation

Verify pin bounds/order; visible selection and mixed archive; local-only and
leave-first deletion, sole-admin and sole-member handling; partial failures,
retry and profile ownership; folder validation/isolation; connection readiness
and stale generations. Compile interaction tests for contextual actions, Back,
confirmations and recovery without claiming they ran on a device.

## Implementation evidence

- `ChatOrganizationModels.kt` and `Profile.chatFolders/chatConnection` own
  pinned-order projections, visible selection, manual folder membership,
  per-target action results and connection phases. `AppViewModel` exposes
  guarded requests and staged deletion; route/profile changes invalidate work.
- `ChatsScreen`, `ChatContextMenu`, `ChatListRow` and `ChatOrganizationUi`
  provide selection/Back/restoration, bounded Move actions and TalkBack
  equivalents, folder assignment/filtering, local/leave-first confirmations,
  progress, partial results/retry and connection/relay recovery.
- Local-only deletion matches `Controllers.deleteGroupLocalFromChatList:5279`;
  explicit leave-first maps to `deleteGroupFromChatList:5305`. The audit's old
  ended-only limitation is resolved. Sole-member deletion skips leave only with
  an authoritative self member; an absent self roster cannot authorize it.
- `ChatOrganizationTest` and `ChatOrganizationStateTest`: 18 new host tests
  cover ordering, eligibility, partial retries, membership prerequisites,
  mutation timing, folder ownership, cancellation and stale recovery callbacks.
- `ChatOrganizationFlowTest`: seven compiled UI cases cover checkbox semantics,
  Back/search ordering, recreation/profile reset, select-all filtering, deletion
  confirmation/retry, folder naming/filtering and recovery. They were not run.
- The clean README gate passed **262 unit tests**, zero failures/errors/skips,
  zero lint errors and both APKs. The same 14 pre-existing lint warnings remain.
  `git diff --check` passed. No device execution or visual acceptance is claimed.

C027–C029 are partially advanced by the required folder-assignment foundation.
B06 still owns management/editor, description, rules and included-person/chat
pickers, defaults/restore, folder ordering and the Chat Info entry point.

Commit title: `B05: Add chat organization and connection recovery`.
