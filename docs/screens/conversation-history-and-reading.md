# Conversation history, reading and message details — B08

Selected 2026-09-04 under the all-batches goal. C035, C037 and C047.
Implemented and host-verified; device/visual acceptance remains pending.

## Product contract

Keep the accepted transcript, bubbles, compact search field, result highlighting,
previous/next controls, composer drafts, reply and exact-message navigation.
Present a bounded loaded window of the existing in-memory history. Older/Newer
actions expose loading and retryable failures while retaining the current rows.
Stable message keys preserve the viewport when adjacent pages are added. A distant
target loads its surrounding window; missing/deleted targets show an unavailable
result. Back or a superseding query/target cancels that navigation intent.

Conversation search immediately shows matches in the loaded window, then scans
the complete local history after a short debounce. A failed scan retains loaded
matches and explicitly reports incomplete search with Retry. Successful scans
include unloaded matches. Pin selection by message ID as the result set changes;
loading its window must finish before scrolling/highlighting. Preserve the
established newest-first order and bounded previous/next behavior. Closing search
preserves the draft and restores its prior logical viewport.

Opening a chat captures its first unread message and count. Land at the separator
above that message, including when its history window must load first. The
separator is a noninteractive horizontal line with a centered localized count.
Keep its anchor/count stable as visible messages are acknowledged during the
visit, include newly received unread messages in that section, and remove it
when replying. Deletion advances a missing anchor to the next surviving section
message. Returning for a new visit captures the remaining unread messages.

Mark only received rows actually visible after viewport settlement; search,
overlays and backgrounded screens do not consume unread messages. Keep unread
IDs in profile/chat-owned memory so partial reading survives Back. Fixture
summary counts seed the most recent received rows; subsequent counts derive
from concrete IDs. No unread/mention jump or bulk-read button appears. The
existing small down arrow always targets the latest message after the accepted
one-viewport threshold, without temporarily highlighting the last bubble
(explicit user direction, 2026-09-05). Page loads and navigation requests do not imply reading.

Message Details retains its content, reactions and people. Add status, a truthful
Sent/Received/Created timestamp, sender-claimed time only when it meaningfully
differs from local receipt, authoritative expiry when supplied, and named copy
actions for Message ID and the sender's public key. Sending/failed outgoing
messages use Created; incoming Streaming uses Received and Streaming. These are
delivery facts, never read receipts. New optional timestamp data falls back to
B07's fixed calendar for existing display-only fixtures.

## Android composition and states

Use keyed LazyColumn rows, shared status content, Material progress indicators,
TextButtons for page/retry actions and named compact jump controls. Keep current
adaptive bounds, 4/8 dp rhythm, semantic colors, focus, RTL, large text and IME/
system inset ownership. Loading/failure and unread state are communicated in text
and polite semantics. Do not add a second transcript or a custom scrolling engine.
Small viewport/selection identifiers may be saved; asynchronous requests carry
owner/query/target generations and are cancelled on dismissal or navigation.

Developer-only controls select one-shot older/newer/search/target outcomes and
provide incoming-message/mention and timestamp/Streaming examples. Ordinary
surfaces contain production-ready messages, without fixture or protocol language.

## Evidence and integration

Read-only production whitenoise-android@319454889f1c2494dec4a69b5577d98017f44eee:
`ui/conversation/ConversationHistorySearch.kt`, `ConversationScreen.kt` search/
reply/unread navigation; `state/ConversationUnreadJumpState.kt`,
`ConversationInitialTimeline.kt`, and `Controllers.kt` page/target/read methods;
`ui/conversation/messages/MessageFullScreen.kt` MessageInfoSheet. Production's
local-store scan, paired pagination cursor, target loading, read anchor and exact
receipt/expiry metadata are the eventual integration seams. The prototype uses
the same outcomes over immutable in-memory content, without backend or storage.

See [F04](../audits/production-android-parity/flows/F04-global-search.md),
[F05](../audits/production-android-parity/flows/F05-message-lifecycle.md),
[existing interactions](message-interactions-and-search.md),
[shared conversation](shared-conversation-core.md) and [B07](global-search.md).
Current official guidance checked 2026-09-04:
[lazy lists and stable keys](https://developer.android.com/develop/ui/compose/lists),
[semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).
No dependency, permission, service, network, device or emulator execution is added.

## Acceptance

Verify bounded older/newer pages and exact targets, deduplication and deletion,
failed-page retention/retry, exhaustive search with loaded fallback and stable
result identity, restored viewport/draft, cancellation and profile ownership.
Prove captured unread boundaries, off-tail arrival stability, visible-only reads,
stable divider retention, reply dismissal and no stale/failed-target read. Verify timestamp
precedence, skew threshold, Created/Received/Streaming and expiry/copy semantics.
Run the host gate and compile durable UI cases before the B08 commit; current-build
device behavior and visual acceptance remain separate.

## Implementation evidence

- `ConversationHistoryModels.kt` and `ConversationProjection.kt` share one ordered
  history with stable IDs. The initial 18-entry window pages in both directions;
  exact targets load a surrounding window. Deleted/event targets are unavailable,
  full history remains authoritative, and replies outside the window keep excerpts.
- `ConversationHistoryUi.kt` owns saved windows and cancellable page/target/search
  requests. A failed request keeps current content; Retry replaces only that
  request. Full search merges back into stable selected-message identity, while a
  failed scan explicitly retains loaded matches. Live target completion checks
  current history. Leaving the foreground cancels target navigation; returning
  does not leave the initial viewport unsettled or replay an old jump.
- `ConversationScreen.kt` preserves the accepted bubbles, composer and search
  controls. Retry feedback stays above the transcript. Chat Info search reuses
  its existing back-stack entry and restores the prior anchor/offset on close.
  Incoming messages follow only an already settled tail; off-tail arrivals retain
  the reader's position and frozen unread target. Mention advancement happens
  after the requested message has actually been revealed.
- `AppViewModel.kt` captures unread IDs on entry and validates profile/chat/row
  ownership before acknowledging visible messages. Search, overlays, background,
  scrolling and unsettled navigation suppress those acknowledgements. Viewport
  intersection excludes the overlaid composer and clipped rows. Sending does not
  consume unseen incoming messages; deletion, read/unread actions, undo, profile
  switching and explicit leave/invitation transitions reconcile the same state.
- `MessageFacts.kt` / `MessageFactsUi.kt` add Created/Sent/Received/Streaming,
  preferred local receipt, sender-claimed time beyond the five-second tolerance,
  authoritative expiry and exact ID/sender-key copy. Localized timestamps include
  their time zone. Existing message content, reactions and recipients remain.
  Developer Tools supplies one-shot failures and incoming mention/Streaming
  examples with explicit receipt/expiry facts; no transport or service is added.
- `ConversationHistoryTest`, `MessageFactsTest` and
  `ConversationReadingStateTest` add **23 passing unit tests** covering ordering,
  pages, failure/cancellation, live deleted targets, search identity, partial
  visible reading, covered/zero-height viewports, arrivals, mentions, undo,
  profile ownership and timestamp precedence. Existing opening/global-search
  tests now require explicit visible acknowledgements rather than entry-as-read.
- `ConversationHistoryFlowTest` adds **12 compiled UI cases** for older/newer
  failure/retry, exhaustive search recovery, unavailable targets/draft retention,
  saved windows, foreground cancellation, paging after unavailable entry,
  visible-only reading, mention failure,
  superseding query, existing-entry search/Back, media return to an unloaded source,
  and exact details copying. The catalog presentation helper loads older pages
  through real controls; Message
  Details assertions account for the added facts and scroll to reactions.
- The clean README host gate passed **319 unit tests**, zero failures/errors/skips,
  zero lint errors and both APKs, with the same 14 pre-existing warnings. The first
  lint gate caught a non-observable locale fallback in Message Details; using
  the current configuration with a fixed fallback resolved it.
  `git diff --check`, matrix counts and changed-document links were checked before
  committing. UI cases were compiled only; current-build device behavior and
  user visual acceptance remain pending.

Commit title: `B08: Add conversation history and unread recovery`.


## 2026-09-05 latest-message jump polish

Latest user direction supersedes the earlier two-viewport threshold. Show the
arrow after one full usable transcript viewport above the tail, excluding the
composer's covered area. Keep it at the trailing edge above the composer. Use a
32 dp circular SmallFloatingActionButton with a 20 dp down arrow, native shadow,
semantic surfaceContainerHigh/onSurface contrast and the native 48 dp minimum
interaction target. Material owns elevation and pressed/focus feedback.
The latest-message accessible label, recovery and search/selection suppression
remain. The subsequent unread-divider change removes unread/mention targeting.

The prior ScrollIndicatorState estimates changed their average row height as
galleries entered and left the viewport, making visibility oscillate. Replace
that estimate with ConversationTailJump: cache measured heights by stable row
key and sum the actual trailing geometry, spacing and bottom inset. Measurements
survive older-page insertion and clear when width/density/font scale changes.
Transient empty layouts retain the previous visibility instead of blinking.
When a distant jump restores unmeasured trailing rows, use a fixed viewport
fallback per unknown row; unloaded newer history retains a recovery action.
This fallback is limited to unmeasured history, not ordinary scrolling from the
tail. The exact distance is available once those rows have been measured.

Sources reviewed 2026-09-05:
- [Native small FAB and elevation](https://developer.android.com/develop/ui/compose/components/fab#small)
- [ScrollIndicatorState estimates](https://developer.android.com/reference/kotlin/androidx/compose/foundation/ScrollIndicatorState)

ConversationTailJumpTest covers the one-viewport boundary, alternating gallery
heights, reverse scrolling, page insertion, transient layouts, resizing, distant
history and overflow. The existing Compose jump-and-return flow compiles only.
Host gate passed: 905 unit tests, zero lint errors (18 existing warnings),
and both APKs assembled. `git diff --check` passes. Device visual acceptance
remains pending.

## Unread separator follow-up — 2026-09-05

Explicit user direction replaces unread navigation actions with Signal-style
reading separation. Current first-party references reviewed:
- [Signal divider anchor/count and reply dismissal](https://github.com/signalapp/Signal-Android/blob/main/app/src/main/java/org/thoughtcrime/securesms/conversation/v2/ConversationItemDecorations.kt)
- [Signal initial unread landing](https://github.com/signalapp/Signal-Android/blob/main/app/src/main/java/org/thoughtcrime/securesms/conversation/v2/ConversationFragment.kt)
- [Signal separator layout](https://github.com/signalapp/Signal-Android/blob/main/app/src/main/res/layout/conversation_item_last_seen.xml)

Native Compose uses a 1 dp semantic outline divider, labelMedium/onSurface count,
24 dp top clearance, 8 dp line/label spacing and 16 dp bottom clearance on the
existing transcript margins. It has merged reading semantics and no click action.
`ConversationUnreadDivider` owns captured IDs separately from mutable read state;
`ConversationHistoryUiState` saves the section across recreation. Initial landing
uses the separator's keyed row rather than clipping it by aligning its message.
The old unread-stack model, mention button, read-through navigation callback and
unused labels are removed. The profile-owned visible-only read API is unchanged.

Six host regressions cover capture, reading/re-entry, arrivals, reply, deletion
and exclusion of own messages/events. Compose coverage checks initial separator
visibility, absent unread buttons, latest-only jump, and reading/recreation/reply
retention. Host gate passed: 909 unit tests, zero lint errors (18 existing
warnings), and both debug APKs assembled. The final additional Compose case
compiles; UI execution and visual acceptance remain pending.

## 2026-09-05 — closer tail arrow with shorter shadow

Explicit screenshot feedback moves the arrow 16 dp toward the composer by
subtracting Scaffold’s existing FAB bottom spacing from the extra measured
composer clearance (clamped at zero). The 32 dp visual, 20 dp chevron, native
48 dp touch target and system/IME inset handling remain. Native FAB elevation
is half the previous defaults: 3 dp resting/pressed/focused, 4 dp hovered.
The one-viewport threshold and stable visibility model are unchanged.

Host gate `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed: 910 unit tests, zero failures/errors/skips, zero lint errors and both APKs.
Device visual acceptance remains pending.

## 2026-09-05 — tail arrow matches translucent date pills

Latest direction supersedes the smaller, elevated arrow. Restore a 40 dp circle
and 24 dp chevron, with the exact pinned date-pill surfaceDim/alpha and onSurface
foreground. Set native FAB elevation to zero for resting, pressed, focused and
hovered states. The closer composer placement, 48 dp touch target, one-viewport
threshold and stable visibility logic remain intact.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 909 unit tests, zero lint errors and both APKs. Device visual
acceptance remains pending.
