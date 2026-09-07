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


## 2026-09-06 — Forwarded attribution (#16)

The user selected WhatsApp's basic attribution treatment: an auto-mirrored
forward arrow and localized “Forwarded” above the content, inside the existing
bubble. Received content gains the marker when forwarded; an own original
message stays unmarked, including when sent through another signed-in profile.
An already forwarded message keeps the marker on subsequent forwards. The current
sender still owns the new message. No original sender, source chat, raw identifier,
forward counter or protocol metadata is exposed.

`ChatMessage.isForwarded` is deterministic local state. `MessageForwarding`
compares the source author against the source profile before assigning destination
ownership. Text, media-frame forwarding and mixed attachments retain it. The shared
bubble renders attribution before quotes, attachments and text, including search
and focused previews. Deleted messages omit it. Native passage selection stays
inside the body; Copy copies content without the attribution label. The bubble
announces “Forwarded” once; its decorative arrow and duplicate visual label are
excluded from normal semantics. During passage selection the label has its own
text semantics outside selectable content.

Material `labelMedium` italic text uses a readable neutral gray,
with a 16 dp existing forward vector and a shared 4 dp label gap. Rich-content
insets, rounded shapes, native selection and Back behavior remain unchanged.
Labels wrap at narrow widths and large font sizes; the vector mirrors in RTL.
All five resource locales have the label. Maya Chen's existing history ends with
an incoming forwarded text and outgoing forwarded photo, using ordinary product
copy. These two user-requested examples extend the pinned history.

Source: user direction on #16; pinned message actions remain mapped through
`docs/port/source-map.md`. Current official Android source checked:
[Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).
The final README host gate (`./gradlew testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest`) passed: 955 unit tests, zero failures/errors/skips,
lint and both APKs. Three new Compose cases compile for attribution/copy/selection/
search, media opening/deletion, and narrow RTL at 200% type across themes. Model
and state regressions cover ownership, forwarding again, mixed attachment kinds,
media frames and profile isolation. No device inspection was requested or performed;
visual acceptance remains pending.


## 2026-09-06 — Folder page, confirmation and quiet attribution

The user accepted the marker's layout and requested reduced contrast. Its arrow
and text now prefer #999999 on dark bubbles and #666666 on light bubbles, adjusting
to maintain at least 4.5:1 text contrast against the actual bubble. This includes
custom bubble colors, all themes and agent cards; native text-selection colors are
independent.

The initial forwarding picker shows chats and one “Forward to chats in a folder”
row. It uses the same shared segmented-list shape as the chat rows, with all
outer corners rounded because it is a standalone item. That row opens a separate
Folders page within the existing picker, with a
native top app bar, Back, Done and multi-select folder rows. Folder edits are
provisional until Done; toolbar/system Back discards those edits and restores the
chat picker, including its query, individual selections and media caption. Done
applies the deduplicated eligible chat selection and returns to the chat picker.
Folder tri-state reflects the selected chats, so overlapping folders do not send
duplicates. Destination-profile changes reset selection. The regular Forward
button is absent with no recipients and appears with “Forward to N Chats” after
selection. The media-caption field stays available, but its send button is also
absent until a recipient is selected; hidden buttons do not reserve overlay space.

Successful completion shows Android's standard `Toast.makeText(..., LENGTH_SHORT)`
with “Forwarded”, then removes the completed operation from the host. It has no
persistent success bar, Details or Dismiss, including when completion happens with
details open. Completion waits for a resumed foreground lifecycle and remembers
the notified request so recomposition does not repeat the toast. In-progress and
failed/partial operations retain progress and retry/cancellation controls.

The user's native-toast direction supersedes the generic Snackbar preference for
this confirmation. Official source checked:
[Android Toasts](https://developer.android.com/guide/topics/ui/notifiers/toasts).
Host validation: the full README gate passed 957 unit tests, lint and both APKs.
The final dismissal/UI-regression follow-up passed `lintDebug assembleDebug
assembleDebugAndroidTest`. Updated Compose coverage includes the separate folder
page, Done versus Back, overlapping folders without duplicates, zero-selection
button visibility, media send visibility, one-time success consumption and retained
failure recovery. These UI cases compiled only. No device execution or visual
verification was requested or performed.


2026-09-07 folder-entry shape correction: the standalone entry now uses shared
`segmentedShapes(0, 1)` to match the chat containers. Lint and app/test APK assembly
pass; this styling-only change adds no state or tests. No device run performed.


## 2026-09-07 — Forwarding starts with a native toast

The user requested toast feedback during forwarding as well as completion.
Preparing/uploading/sending no longer render a bottom progress bar or Details and
do not consume navigation insets or reduce the conversation height. The app-owned
operation continues independently of the toast and the current route. Show one
native short “Forwarding…” toast per operation/manual retry, then “Forwarded” on
completion. Revision changes, per-chat progress and automatic retries do not enqueue
more toasts. Completion cancels any still-visible start toast before showing success;
profile changes or host disposal cancel the active toast. Foreground lifecycle
requirements and one-time completion consumption remain.

Failed, partially failed and cancelled outcomes retain the existing recovery surface.
Retry hides that surface while the operation runs again; no in-progress details
sheet or cancel control is exposed. This supersedes the earlier persistent
in-progress presentation. All five resource locales include the start label.
`MessageBatchFlowTest` checks that preparation and sending retain full-height content
without progress controls while stepping continues; existing tests cover failure
recovery and single completion consumption. Lint and both APKs pass; the final
UI-test assertion follow-up also passed lint and test APK compilation. UI cases
compiled only; no device run.


## 2026-09-07 — Forwarding profile selector (#21)

The user requested an avatar and a clear pill selector in place of the plain
“Send as” text action. The shared forwarding picker now uses a native Material
FilledTonalButton with a capsule shape, the selected profile's 32 dp avatar,
“Send as [name]” and a decorative dropdown chevron. It sits at the shared 16 dp
screen inset, with 8 dp outer vertical and avatar/label/icon relationships.
The user requested tighter side padding: use 12 dp horizontal and 8 dp vertical
content padding. Material still owns typography, minimum touch target, focus and
ripple. Use secondaryContainer/onSecondaryContainer for the secondary gray fill;
AMOLED Outline retains its theme border. Names wrap when space or font scaling
requires it; decorative avatar and chevron avoid duplicate TalkBack labels.

The existing profile chooser and destination ownership remain authoritative:
show the selector only with multiple signed-in profiles, clear recipients when
changing destination, and leave the source profile/chat active. Back from the
profile chooser leaves the current selection intact. This shared control covers
ordinary and media forwarding. Existing MessageBatchFlowTest profile-selection
and media-forwarding cases provide interaction regression coverage; this change
adds presentation only. Current-build device/visual acceptance remains pending.

Official source checked 2026-09-07:
[Compose buttons](https://developer.android.com/develop/ui/compose/components/button).

Validation: README host gate passed (`testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest`): 991 unit tests, zero failures/errors/skips, lint and
both APKs. Script tests and locale-resource verification also passed. Existing
UI regressions compiled only; no device execution or visual verification.
