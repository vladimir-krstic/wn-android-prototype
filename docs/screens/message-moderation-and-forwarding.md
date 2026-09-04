# Message moderation, batch deletion and forwarding — B10

Selected 2026-09-04 under the B01–B32 goal. C043–C045. Implemented and host-verified;
device/visual acceptance remains pending.

## Product contract

Delete for everyone is available for an own message in a writable conversation
and for another member's message when the current profile is an active group
admin. A direct chat never grants moderation. Deleted messages offer local
removal only. Re-check ownership, membership and role immediately before each
mutation. Preserve tombstones for everyone and clear their content, attachments,
reactions, edit history and reply payload; local removal removes only that row.

A mixed selection offers Delete for everyone when at least one item qualifies.
The confirmation names the number removed for everyone and the number removed
only from this device. Freeze each item's operation at confirmation; loss of
permission causes a failure, never an unannounced local fallback. Show completed
and failed counts, keep failed selections recoverable, and retry only failures.
Successful destructive work is never replayed. Sanitized reports contain counts
and categories without message text, keys or identifiers.

Forward preserves accepted source order and text/media while creating fresh
message identity and clearing source transport/reaction/reply/edit metadata.
Support the existing one-frame media forwarding and optional caption. The picker
retains the approved search/segmented rows/floating media composer and gains folder
selection, unavailable destination reasons and signed-in destination-profile choice.
Changing destination profile clears recipients; it does not switch the active
profile. Exclude the exact source profile/chat, never a same-ID chat in another
profile. Folder toggling selects/deselects eligible unique member chats.

Compare limits explicitly: production ForwardSelection and ForwardMessagePicker
apply no message/recipient count cap; MessageForwarding governs prepared payload
size and availability instead. The older prototype's 32-message/five-chat limits
are removed under the authorized production-parity goal. Preserve disabled and
blocked reasons for deleted/expired/unavailable sources and non-writable targets.
No silent truncation of messages or folder members.

Forward operations belong to app state after the picker closes. Expose preparation,
upload and send progress per destination, cancellation before publishing, partial
completion, retry of failed remaining work, and terminal expiry/session-change or
payload-too-large failures. Publishing cannot be cancelled once a destination is
sending or has sent messages. Retain completed destinations and sent-message counts
through retry so no accepted send repeats. Leaving the source screen does not lose
the operation; switching/signing out its source or destination profile invalidates
old completions. Foreground stepping is deterministic; no background service. Transient failures
retry automatically at 1, 2 and 4 seconds, up to three retries per operation;
preparation timeouts require explicit Retry failed. Expiry, source loss, payload
size and session failures are terminal. A delayed retry checks request/revision
identity; manual retry, cancellation, replacement or dismissal cannot replay it.

## Native UI, copy and Back

Use the shared Material confirmation dialog, grouped list/checkbox rows,
progress indicators and status surfaces. Use Delete for me, Delete for everyone,
Retry failed, Dismiss, Forward, Preparing, Uploading, Sending, Cancel and Details.
Report outcomes such as “Deleted 2 of 3 messages” and “Forwarded to 2 of 3 chats.”
A folder exposes native mixed/selected state and explains when none of its chats
can receive messages. Status details can close without stopping app-owned work.
Cancelling a media-forward picker returns to its viewer. Accepting forwarding
closes the media viewer so the app-owned progress is visible. Back dismisses a
picker/confirmation without starting work; progress dismissal is
separate from explicit Cancel. Keep selected content/drafts behind the task.

Use semantic error/disabled/progress states, named action targets, polite progress
announcements, scrollable details, shared adaptive bounds and 4/8 dp spacing.
Long labels, large type, RTL, keyboard and system insets remain supported. No real
message publishing, authentication, crypto, media upload, storage or new permission.
Developer-only controls expose deterministic partial/failure/expiry/session cases.

## Evidence and governing sources

Production master `319454889f1c2494dec4a69b5577d98017f44eee`: Controllers.kt
messageDeleteCapability; MessageDeleteDialog.kt; MessageBatchActions.kt and
MessageBatchDeleteOperations.kt; BatchDeleteFailureNotice.kt; ForwardSelection.kt;
ForwardMessagePicker.kt; MessageForwarding.kt; AppStateForwardTransport.kt and
ForwardOperationStatus.kt. Existing presentation authority is
[message interactions](message-interactions-and-search.md), WN-ANDROID-0107/0118,
[UI metrics](../ui-metrics.md) and [B09 reading](message-editing-and-reading.md).
Pinned iOS source mapping remains in [source-map](../port/source-map.md); production
adds these capabilities without importing its presentation or transport layers.

Official Android sources checked 2026-09-04:
[dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
[progress](https://developer.android.com/develop/ui/compose/components/progress),
[checkboxes](https://developer.android.com/develop/ui/compose/components/checkbox).

## Observable acceptance

Prove own/member/admin/DM/ended/deleted eligibility, mixed-operation confirmation,
independent failures and failed-only retry; re-check role and target existence.
Prove folder selection without truncation, destination-profile isolation, source
ordering, accepted edit projection, per-stage progress, partial send recovery,
cancellation boundary, expired/session-changed source rejection, stale request
rejection and app-owned operation continuity. Preserve media-frame semantics,
ordinary composer state and unread reconciliation. Run meaningful state tests,
compile durable UI regressions and run the complete README host gate before the
B10 commit. No device run or visual verification is authorized.

## Implementation evidence

- `MessageBatchModels.kt` owns the deletion capability/plan and immutable per-item
  outcomes, and the forwarding source/target eligibility, folder toggling,
  preparation/progress/retry state and fresh destination copies. Everyone/local
  operation kinds are frozen at confirmation. The old count caps are removed,
  with explicit source/target eligibility replacing arbitrary truncation.
- `AppViewModel.kt` rechecks current membership, role, source existence, deadline,
  signed-in owner/destination and request revision at each step. Only failed
  deletion items retry. Forwarded destinations retain accepted message counts;
  retry starts at the next unsent message, uses stable operation/message identity
  and never replays a completed destination. One-frame forwarding keeps the
  selected image and optional trimmed caption. Profile wipe removes operation
  snapshots that contain its message data; selecting the already active profile
  does not interrupt work.
- `MessageBatchUi.kt` owns foreground stepping outside individual routes, a
  native status bar/details sheet and per-chat deletion recovery. Details closes
  independently of explicit Cancel; publishing cannot be cancelled once sending
  starts. Active operations survive navigation and pause in the background.
  Three delayed transient retries use production's 1/2/4-second schedule;
  timeouts and terminal source/session failures follow their separate policies.
- Conversation and Shared Content both use the expanded shared forwarding picker.
  It retains the accepted segmented selection rows, search and media-caption
  capsule, adding mixed-state folder selection, unavailable-chat reasons and
  destination-profile choice. Changing that choice clears recipients without
  switching the active profile. Delete confirmation names mixed scopes; a
  tombstone's hold/accessibility action opens only local removal. Reading behind
  the forwarding details sheet is excluded from visible-read acknowledgement.
- Developer Tools exposes deletion and forwarding outcomes. Failure reports
  contain operation counts/categories only, without source content or identity.
  Product labels are resources; quantities use plural forms where needed.
- Eighteen `MessageBatchStateTest` regressions cover moderation/DM/member/ended/
  tombstone rules, frozen mixed deletion, partial failures, failed-only retry,
  stale requests, source loss/expiry, 33-message/six-chat forwarding, folders,
  media preparation/upload, partial send prefixes, cancellation, destination
  profile isolation, three automatic retries, and wipe/session cleanup. Existing
  policy/order tests now assert the explicitly expanded count behavior.
- Nine new `MessageBatchFlowTest` cases compile for admin/tombstone removal,
  mixed confirmation, failed selection/retry, six-chat folder choice, unavailable
  reasons, destination-profile switching, progress across navigation and explicit
  cancellation versus closing details, plus Shared Content forwarding one media
  frame to another signed-in profile through the same staged operation. Existing
  forwarding guidance tests compile
  with the new folder wording.
- The clean README gate and final incremental integration check passed **361 unit tests**, zero failures/errors/skips,
  zero lint errors and both APKs, with the same 14 pre-existing warnings. All new
  UI cases were compiled only; no device, spoken/network transport or visual
  acceptance is claimed. Changed document links and matrix counts were checked.

Production integration should replace deterministic advance/scenario callbacks
with the existing delete outcomes, ForwardSession/transport and app-owned
coordinator. Preserve stable identity, partial counts, eligibility rechecks and
cancellation boundaries; do not copy fixture transport into production.

Commit title: `B10: Add message moderation and forwarding recovery`.
