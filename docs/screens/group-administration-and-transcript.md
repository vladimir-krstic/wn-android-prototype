# Group administration, ended groups and transcript export

## Purpose and scope

B19 implements C076–C079: transfer administration and step down or leave,
sole-member deletion, capability-controlled disbanding, frozen/ended conversation
states, and a complete local transcript. B18 owns roster and image work; B20 owns
retention. The authorized boundary remains deterministic in-memory state, with
only a user-requested document written through Android's existing Files contract.

## Parity contract and state

Production evidence is pinned at `319454889f1c2494dec4a69b5577d98017f44eee`:
`state/Controllers.kt` transferAdmin, stepDownAsAdmin, leaveGroup and disband
methods; `ui/group/GroupDetailsScreen.kt`, `GroupDisbandControls.kt`;
`ui/conversation/composer/ComposerGate.kt`; and
`core/ConversationTranscriptExport.kt`. Consult the production audit F09 and
source map for the baseline. iOS supplies the established Group Info, member,
leave and readable-history presentation; the additional capabilities come from
the authorized production Android audit, not new iOS scope.

Every command binds profile, chat, generation, roster revision and expected
members. Administration uses the same lock as B18. Transfer grants the selected
member first, then steps down, then optionally leaves. A failure after grant
keeps both admins; a failure after step-down keeps ordinary membership. Retry
revalidates and resumes the remaining stage. A direct step-down requires another
admin. The only member can explicitly delete the group. Local delete, leave and
disband confirmations describe their distinct consequences.

Disband controls use typed authoritative capability/blocker state. Enabling and
disbanding are separate commands; acceptance moves immediately to Disbanding,
then converges to Disbanded or an acknowledged, retryable failure. Ended and
unrecoverable state outrank invitation or seeded membership. Cold unknown,
failed and inconsistent rosters block outbound actions. A positive warm member
seed can preserve messaging while administration awaits verification. History,
search and export remain readable. Current master `911040c7e1c31652638c8cfd72812d1f3a694b9b`
adds `ChatListGroupSeed.kt`; preserve terminal and unrecoverable state across
profile switches/cold presentation rather than briefly showing a composer.

Export snapshots all available local history, independent of the loaded UI
window/search. Sort chronologically with stable ID tie-breaking and deduplicate
IDs. Preserve authored identity, accepted text and revision history, reactions,
reply references, deletion status and available attachment metadata. Never export
pending optimistic edits, drafts, private keys or fabricate Marmot wire fields.
Use a separately identified local transcript schema. Generation and profile
checks guard preparation, cancellation, picker return and writing. Source
unavailable, preparation failure, missing destination, cancellation and completed
write have distinct observable results; an empty available history is valid.

## Entry, Back and Android composition

Group Info hosts administration and lifecycle rows, inline progress/recovery,
and a scrollable Material member selector. Material AlertDialog owns transfer,
step-down, leave, deletion and disband confirmation. Back dismisses unsubmitted
choices without mutation; accepted work belongs to the profile/chat and survives
navigation. Successful leave returns to readable history; sole deletion returns
to Chats. Export is available in Chat Info for groups and direct chats, including
ended history. Preparation can be cancelled; the standard unstyled Files UI
owns destination selection and cancellation. No storage permission is requested.

Use semantic Material surface/error roles, shared spacing, adaptive width and
safe drawing insets. Native buttons and rows retain keyboard, TalkBack and Voice
Access semantics. Progress/status is textual with polite live-region semantics;
long names, blockers and explanations wrap. No fixed-height modal content or
custom gesture is needed. Visual/device acceptance remains pending.

## Exact product copy

Actions: “Transfer administration”, “Step down as admin”, “Transfer and leave”,
“Enable disbanding”, “Disband group”, “Export transcript”, “Save transcript”,
“Retry”, “Dismiss”, “Cancel”, “Delete group”.

Transfer confirmation identifies the new admin and that you become a member;
transfer-and-leave additionally says you leave. Partial failure explicitly says
which accepted step remains: “Administration was granted, but you’re still an
admin.” or “You stepped down, but couldn’t leave the group.”

Disband confirmation: “This permanently ends the group for everyone. Nobody can
send new messages afterwards. This cannot be undone.” Sole-member deletion:
“You’re the only member. Leaving deletes this group and its history from this
device.” Frozen: “This group is temporarily frozen while White Noise verifies
and repairs its state.” Ended: “This group has ended. You can still read its
history.” Export success is only “Transcript saved” after a completed write.
Resources are the authoritative exact text for remaining state labels.

## Governing Android sources

- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog): standard consequence confirmation and safe dismissal.
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files): permissionless user-selected document creation and provider-owned UI.
- [Native evaluation](../references/native-ui.md), [shared metrics](../ui-metrics.md), [product language](../product-language.md).

## Observable acceptance criteria

- No transfer/retry leaves zero admins or repeats an accepted grant; stale role,
  source, profile and generation callbacks cannot mutate the wrong group.
- Group commits serialize; accepted disband immediately blocks outbound actions;
  failure acknowledgment and convergence retain readable history.
- Terminal/unrecoverable state survives profile switching. Unknown membership
  never borrows unverified admin authority. Recovery restores messaging only
  when the current state permits it.
- Transcript contains all available history in stable order, true authored
  identity and accepted revisions, with no draft or pending edit leakage.
- Export cancellation and stale picker callbacks never write a different
  profile’s data or claim success. Host unit tests and compiled UI tests cover
  these contracts; device/system picker execution requires a new user request.

## Implementation evidence

Implemented and host-verified 2026-09-04. C076–C079 are covered within the
local prototype boundary; user/device/visual acceptance remains pending.

- `GroupLifecycle.kt` and `GroupLifecycleController.kt` implement grant-first
  transfer, self-demotion and leave stages; partial and interrupted work retains
  accepted roles. Ready roster/revision/target checks and the shared B18 lock
  guard each commit. Direct step-down, sole deletion and folder cleanup are tested.
- `GroupLifecycleUi.kt` adds established neutral Material dialogs, a member
  selector, confirmations, progress and retry in Group Info. The chat-list
  sole-admin warning links to administration management instead of a dead end.
- Capability flags and typed blockers control enable/disband. Accepted disband
  blocks outbound immediately; convergence can complete in the original inactive
  profile. Failure acknowledgment preserves the group until a new valid request.
- `ConversationProjection.kt`, message forwarding/reaction/edit guards and
  `ChatListPresentation.kt` preserve frozen, unknown, disbanding and ended state.
  Cold unknown blocks messaging; a positive warm member seed does not grant
  administration. Ended history can be searched, exported or locally deleted.
- `ConversationTranscript.kt`, `TranscriptController.kt` and `TranscriptUi.kt`
  snapshot all available history, page 200 entries at a time, deduplicate IDs and
  sort timestamp/ID. The JSON document includes authored IDs/public keys,
  accepted originals/revisions, replies, reactions, deletion status, system
  entries and available attachment metadata. Deleted bodies/revisions, draft
  content and pending edits are excluded. Encoding runs off the UI thread.
- The Files launcher lives above navigation and uses
  `CreateDocument("application/json")`; picker/result/write IDs stay bound to the
  original source. Successful close of the output stream precedes Saved. Stale,
  failed or interrupted writes trigger best-effort deletion of the newly created
  document. Providers can refuse cleanup; no stronger atomic-write guarantee is
  claimed. No system picker was executed during this batch.

`GroupLifecycleStateTest` and `TranscriptStateTest` add 42 host cases, including
three-page export, profile round trips, stale requests, partial grants, pending
edit exclusion, deleted-content redaction, capability blockers and folder cleanup.
`GroupLifecycleInteractionTest` adds nine compiled-only Compose cases for
confirmation/cancel, partial retry, blockers, frozen/ended state and export status.
The full host gate passes **617 unit tests**, **0 lint errors**, **14 existing
warnings**, **2 hints**, debug APK assembly and instrumentation-test APK assembly.
No device/emulator execution, installation, new permission or external runtime
service was added.

### Production migration and source drift

The new production `ChatListGroupSeed.kt` at master
`911040c7e1c31652638c8cfd72812d1f3a694b9b` retains unrecoverable, disbanding,
disbanded and request state before a full group projection exists. Local Chat
owns those lifecycle facts independently of the current screen and roster;
terminal precedence/profile-switch tests cover the equivalent behavior. The
immutable audit baseline remains unchanged; unrelated timeline and other drift
still requires its own reconciliation.

Reconnect authoritative management capabilities/blockers, roster revision,
commit serialization, accepted grant/demotion results and disband convergence to
the cited production controllers. Local repair/failure scenarios are not an
engine, network repair service or durable operation store. Production transfer
runs its accepted demotion stage non-cancellably; local profile changes instead
retain an explicit interrupted partial state and require a guarded stage retry.

Export uses schema `white-noise-local-transcript`, version 1, with the existing
fixed prototype clock. It does not fabricate source/wire IDs, signed event kinds,
Marmot tags or encrypted media records unavailable in the prototype. On production
migration retain `ConversationTranscriptExport`’s full engine-event schema and
paging contract. Android’s existing Q07 explicit document-destination choice
supplies the local handoff; the production cache-and-Sharesheet path remains a
separate platform integration decision for that repository.
