# Notification routing and inline actions — B23

## Purpose and scope

C094 adds deterministic notification message/invitation/chat-list entry and inline
reply, reaction and mark-read outcomes. B21 owns incoming navigation; B22 owns
notification preferences. Preserve the established Compose UI and canonical chat
state. No live notification delivery, receiver, WorkManager, service, crypto,
durable completion store, networking or new permission is authorized by Q06.

## Evidence and source drift

Production baseline is `319454889f1c2494dec4a69b5577d98017f44eee` in the audit's
read-only archive. Relevant production paths under `app/src/main/java/dev/ipf/whitenoise/android`:
`notifications/NotificationTarget.kt`, `NotificationAction.kt`,
`NotificationActionReceiver.kt`, `NotificationReplyWorker.kt`,
`NotificationReactionWorker.kt`, `NotificationMarkReadWorker.kt` and
`ui/navigation/NotificationReadThroughCommitter.kt`.

Fresh master is `911040c7e1c31652638c8cfd72812d1f3a694b9b` (2026-09-04; no main).
The changed NotificationRouteTimelinePresentationTest moves subscription updates
to authoritative windows and preserves the mounted conversation after reconnect.
Use the existing canonical local history and stable conversation ownership;
notification entry must not substitute a second timeline or reset it on arrival.
Live subscription reconnect remains outside this local adapter. B08/final audit
owns broader window drift.

## Parity contract and state

Every tap carries profile, conversation, kind and optional exact message identity.
Role-change message routes may omit a message ID and open the chat without
advancing a read boundary. Fresh equal taps have different request IDs; stale completions cannot consume the
new tap. A bare launcher entry preserves pending work. Delay while startup or app
lock blocks access. Navigation activates only the owning signed-in profile and
opens the normal conversation, retaining the Chats Back destination. Membership
removal notifications open Chats. Missing profiles and conversations offer honest
recovery. Message history can resolve independently of the visible chat-list row.
An invitation without a materialized row gets three bounded authoritative probes;
terminal unavailable and inconclusive/retry outcomes differ. Left/removed groups
must not be reopened through invitation entry.

Capture the conversation's entry unread boundary before committing a notification
read-through. Commit once for the exact owner/chat/message, including route exit
or background fallback. Never mark later arrivals read merely because a notification
opened the chat. Missing/deleted targets cannot advance the cursor.

Inline reply trims and rejects empty text; reaction validates current owner choices
and the production 32-code-point limit. Message-only action targets require a card
identity and message ID. Signed-out and deleted/expired targets cannot be acted on. Blocked or ended
conversations cannot send replies/reactions; existing history may still be marked read. Inline actions preserve existing drafts, attachments and reply
selection and never activate another profile. Pending work can defer under app
lock; lock waiting has the production 24-hour bound on the fixed local clock. Operation failures have three attempts. Accepted
reply/reaction proof survives cleanup failure: Retry only finishes read/dismiss
cleanup, never repeats the mutation. Separate replies with equal text remain
separate user actions. Reactions set the chosen value without toggle-off on replay.
Mark-read uses the exact boundary. Dismissal is generation/owner aware, so a newer
card or sibling arrival survives stale cleanup. All proof and work stay in memory.

## Presentation, copy and Android composition

Reuse IncomingHost and the normal conversation. Add a small native Material action
status/recovery dialog only for app-owned pending/failure/result state. Use resource
copy: “Sending reply…”, “Adding reaction…”, “Marking as read…”, “Reply sent”,
“Reaction added”, “Marked as read”, “Retry”, “Done”, “Cancel”, and specific
unavailable/locked/retry-exhausted recovery. After an accepted mutation, cleanup
failure must explicitly say the reply/reaction succeeded. Back cancels unaccepted
work or dismisses accepted status without undoing or replaying the mutation.
Developer-only scenario controls supply incoming/action examples; no public mock
Android notification tray, card or inline-input screen is introduced.

## Accessibility, adaptation and system boundary

Use existing adaptive scaffolds and standard Material dialogs/buttons, scrollable
body, live status semantics and shared spacing. Inherit Android touch targets,
focus, keyboard, RTL, large-font and insets handling. Foreground lifecycle owns
local delayed completion; recreated hosts reuse state-holder request identities.
No system surface is restyled or executed for validation.

## Governing sources and approved differences

- [Android notifications](https://developer.android.com/develop/ui/compose/notifications/create-notification): content taps, independent actions and system-owned RemoteInput; production adapters remain excluded.
- [Compose effects](https://developer.android.com/develop/ui/compose/side-effects): keyed effects, cancellation and disposal for request/lifecycle ownership.
- [Source map](../port/source-map.md), [native UI evaluation](../references/native-ui.md), [UI metrics](../ui-metrics.md), [B21](incoming-sharing-and-profile-links.md) and [B22](notification-controls.md) govern existing presentation.

Production Android extends the iOS feature set by explicit user direction. The
approved in-memory boundary replaces real workers and system delivery with local
state transitions; production UI copy remains normal. No new visual exception.

## Observable acceptance and validation

- Owned message/invite/list entry, deferred readiness/lock, missing owner/target,
  bounded invitation probe, exact message positioning and stale same-target tap.
- Exactly-once reply/reaction acceptance, independent equal replies, preserved
  composer, current allowed reactions, lock/sign-out/expiry and three-attempt retry.
- Cleanup-only retry, exact read-through and newer-card preservation.
- Durable status/retry/Back/navigation semantics compiled as Compose tests.
- Host unit tests, lint, app assembly and instrumentation APK compilation only.
  Device execution and visual/user acceptance remain outstanding.

## Implementation evidence

Implemented 2026-09-04 within local scope. `NotificationTarget` and
`NotificationActions` normalize target/action fields and share canonical read and
reaction projections. `IncomingController` handles owned notification entry, bounded
invitation probes, recovery and same-target replacement. `AppRoute.Conversation`
carries target/request identity. `NotificationReadBoundary` runs after the existing
history boundary is captured, with idempotent owner-checked stop/disposal fallback.
Role-change routes without message IDs open normally without a read commit.

`NotificationActionController` separates pending mutation, accepted proof and
finishing cleanup. It keeps immutable request identity after status dismissal,
validates the target owner's current reaction choices, defers under lock for up to
24 fixed-clock hours, counts only operation failures toward three attempts and
preserves newer card generations. Dismissing accepted status keeps cleanup alive.
AppViewModel applies replies/reactions to the originating profile without switching
profiles or altering its composer. Read-through updates that profile's canonical
unread counts and existing retention read deadlines. Mark-read may affect retained
history in an ended conversation; sending still requires an eligible composer.

Developer Tools supplies notification routes, action kinds and outcomes through
existing scenario controls. `NotificationActionsUi` uses standard Material status,
retry, cancellation and completion controls, hidden from other profiles or while
locked. The consumer UI gains no imitation Android notification surface.

Validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`;
748 unit tests pass with no failures/errors/skips; lint has zero errors, the same
14 existing warnings and two hints. Both APKs build. Seven new Compose cases in
`NotificationActionsInteractionTest` compile/package only. Rule/state coverage in
`NotificationActionsTest`, `NotificationActionStateTest` and
`NotificationRouteStateTest` adds 34 unit cases covering ownership, replay,
cleanup, exact boundaries, lock/retry accounting, malformed/changed inputs,
invitation probes and role-change entry. No device/emulator/visual execution.

## Production migration seam

Replace local target injection with NotificationTarget's validated intent routing;
retain request IDs and preload ownership. Reconnect canonical history to the
single mounted conversation/window controller and authoritative invitation probes.
Replace the action controller's mutation/read callbacks with AppState's
sendNotificationReply, sendNotificationReaction and markNotificationMessageRead.
Production workers retain encrypted input, durable completion/retry stores,
receiver completion and process-death recovery; none is claimed locally.

Map card descriptors to real tag/ID/latest-message generation and presenter
cleanup. The local monotonic card generation conservatively preserves everything
newer than action receipt. Production separately captures reaction tap, mark-read
attempt and reply-cleanup baselines and handles RemoteInput lifetime extension.
Existing app lock readiness is a local B21 gate; B24 supplies the app-owned lock
flow. Posting, real inline input, workers/services and their permissions stay Q06
integration work. Visual and user acceptance remain pending.
