# Disappearing-message timers and expiry

## Purpose and scope

B20 covers C081–C083: production timer presets and bounded custom durations,
staged admin changes and pruning confirmation, row-owned expiry/read anchors,
remaining-time accessibility and consistent cleanup. Preserve the established
Group/Chat Info, new-group setup, conversation and shared-content presentation.
No backend, engine, durable store, WorkManager, background service, permission or
device execution is authorized. A fixed in-memory clock models foreground expiry.

## Evidence and Q03 resolution

Production baseline `319454889f1c2494dec4a69b5577d98017f44eee` contains:
`ui/group/DisappearingDuration.kt`, `DisappearingMessages.kt`,
`GroupDetailsScreen.kt:1145–1186`; `state/Controllers.kt` updateMessageRetention,
localExpiryRow, isDisappearingSendTimeExpiryDeferred and read-anchor handling;
`DisappearingMessageSweep.kt`; `MessageRetentionIndicator.kt` and its input and
accessibility helpers. `ConversationRetentionPolicyTransitionTest`,
`DisappearingReadAnchorTest`, `DisappearingMessageSweepTest` and duration/picker
regressions distinguish policy updates from per-message deadlines.

Q03 can be resolved from those separate paths under the approved production
parity direction: enabling or shortening a timer requires explicit destructive
confirmation and can prune older plaintext immediately. Off, unchanged and longer
timers do not perform that pruning. Ordinary filtering does not apply the current
group timer retroactively to every row: only a row with its own saved deadline or
send-time duration expires. Old rows without either remain. Group-system events
never expire. Existing pinned deadlines survive later timer changes, including
Off; turning Off affects newly sent messages and never resurrects removed content.
Received unread rows defer their countdown until an established read anchor;
outgoing rows do not wait for a received-message read watermark. Therefore the
existing broad “after they have been seen” help cannot accurately describe both
operations. Use separate precise copy, without requiring a new product policy.

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`; no main ref exists.
The seven-commit diff changes no duration/retention source or tests. Its timeline
ordering changes are a separate final-reconciliation seam; preserve stable local
row identity and the accepted B08 visible-read behavior here.

## Timer contract and exact copy

Presets: Off, 90 days, 4 weeks, 1 week, 1 day, 8 hours, 1 hour, 5 minutes,
30 seconds. Custom uses one positive whole number and one unit: seconds 1–59,
minutes 1–59, hours 1–23, days 1–6, weeks 1–4, months 1–12, years 1–10. A month
is 30 days and a year 365 days. Reject empty, zero, negative, nonnumeric,
out-of-range and overflowing input. Preserve a staged custom choice until Save;
Back/Cancel never applies it. New-group setup stages an initial timer with its
existing create-once/timer/open recovery.

Actions: “Disappearing messages”, “Custom time”, “Set timer”, “Save”, “Cancel”,
“Retry”. Read-only group help: “Only admins can change this”. Explanation:
“New messages you receive start their countdown when you read them. Messages you
send start theirs when sent.” Confirmation: “Set disappearing timer?” and
“Messages older than %1$s will be permanently removed for everyone.”
Turning Off or lengthening does not show this destructive confirmation. Existing
message countdowns do not change; explain that beside the timer help. Progress,
failed apply and accepted-but-refreshing outcomes must remain distinct.

## Ownership, state and cleanup

Bind each timer request to profile/chat, initial policy, roster revision and
expected members. Use the same B18/B19 group operation lock. Validate before
confirmation and final commit; a stale confirmation or callback cannot overwrite
a changed policy or act with lost administration. Accepted policy is retained if
history refresh fails; recovery must not repeat the mutation or deletion.

Capture message retention when a send is accepted. A received row can wait for
read, then gets one immutable first-read anchor; re-reading cannot extend it.
Use saturating arithmetic and reject invalid timestamps/durations. Existing
explicit expiry facts remain authoritative. Developer-only scenarios and clock
advance controls supply waiting/running/expired, failure and refresh-retry cases.
Foreground ticking changes the fixed local clock, with no hidden background work.

Remove expired message plaintext, attachments, revisions and reactions from the
canonical local timeline. Reconcile previews, unread/read state, selected message
IDs, search/target windows, reply drafts, focused editing, media/attachment readers,
forwarding, capture and Read Aloud/source return. A stale view cannot reveal a
removed message by switching profile or returning to a reader. Keep durable group
system events. Export already binds to the canonical transcript snapshot and must
invalidate if retention removes its source.

## Entry, Back, native presentation and accessibility

Chat/Group Info opens a staged native timer picker with presets and a bounded
custom form. Reuse the app's neutral Material dialog/sheet, tonal field, grouped
radio rows, shared typography/spacing and adaptive content bounds. The standard
Material controls own touch targets, focus, pressed/disabled/error state and
keyboard behavior. Preserve logical Back from custom form to timer choice and
Cancel to Info; only Set timer accepts the destructive consequence. Read-only
members can inspect the current setting and explanation.

Show a small native timer/progress indicator with a textual accessibility state
(waiting for read or remaining duration/expiry), never color or motion alone.
Support wrapping names/copy, large font/display scaling, RTL, TalkBack/Voice Access,
keyboard, compact/expanded widths, safe insets and IME. Avoid additional background
clock work for off-screen indicators. User/device visual acceptance is separate.

## Governing sources and existing parity

- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog): staged choice and explicit consequence confirmation.
- [State-based text fields](https://developer.android.com/develop/ui/compose/text/user-input): numeric custom input, persistent label and validation.
- [Progress indicators](https://developer.android.com/develop/ui/compose/components/progress): native determinate countdown presentation.
- [Source map](../port/source-map.md), [UI metrics](../ui-metrics.md), [native evaluation](../references/native-ui.md).
- The existing iOS timer intent and Info placement are recorded locally; the
  additional timers, pruning and read-anchor rules come from the approved Android
  production audit F10 rather than newer iOS scope.

## Observable acceptance criteria

- Every preset and custom bound is reachable; invalid input cannot commit.
- Confirm only enabling/shortening; Cancel preserves policy/history. Apply and
  refresh failures preserve the correct accepted state and retry stage.
- No unknown/non-admin/ended group can commit; no stale profile, roster, policy
  or generation callback can overwrite current state.
- Only row-owned retained messages expire. Read anchors are set once; sent rows
  do not defer; Off/longer timers do not rewrite old deadlines; events persist.
- Removed content disappears from all active readers, selections, drafts,
  forwarding/export and speech, while unrelated drafts/history remain intact.
- Meaningful host state/ownership tests and compiled UI tests cover the contract;
  the full static gate precedes the B20 commit.

## Implementation evidence

B20 is implemented within the deterministic prototype boundary. The production
rule maps to `model/DisappearingMessages.kt`, `state/RetentionController.kt` and
`ui/conversation/RetentionUi.kt`. `AppViewModel` captures send/forward policies,
sets first-read anchors, prunes and expires canonical rows, reconciles drafts and
pending forwarding/export, initializes read markers before expiry cleanup, and sweeps signed-in inactive profiles without changing
the active profile. Group/Chat Info and new-group setup use the same staged picker;
Conversation and Shared Content discard stale selections/readers. Existing
attachment/audio and Read Aloud ownership checks invalidate removed sources.

The complete host gate is `./gradlew testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest`: 655 unit tests pass, zero failures/errors/skips, zero
lint errors and both APKs. The 14 existing lint warnings remain. This batch adds
14 duration/expiry model cases, 22 owned state cases and two speech-expiry cases.
Nine new Compose cases compile (picker staging/custom validation/read-only help,
pruning consent independent of list visibility, accepted refresh retry,
waiting/countdown semantics and removal).
The two B18 setup cases now use staged Save. Instrumentation was compiled only;
no device, emulator or visual verification was performed.

Production migration must reconnect `updateMessageRetention` and the shared group
commit lock, authoritative per-row retention/expiry fields, optimistic ID transfer,
first-read anchors, timeline refetch/filtering and expiry scheduling. Keep the
accepted-policy-versus-refresh outcome separate. Replace the fixed clock and
local fixtures with those existing engine facts; do not apply the current group
timer to unpinned history. Durable group-system events remain outside expiry.
