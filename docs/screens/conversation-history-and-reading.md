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

Opening a chat captures its initial unread boundary instead of marking the entire
chat read. Mark only received rows actually visible after viewport settlement;
search, overlays and backgrounded screens do not consume unread messages.
Keep unread IDs in profile/chat-owned memory so partial reading survives Back.
Fixture summary counts seed the most recent received rows; subsequent counts
derive from those concrete IDs. Off-tail arrivals freeze the first unread target
for that stack, do not scroll the reader, and do not replace the target as more
messages arrive. The jump first visits that boundary, then offers the latest
messages. A separate next-unread-mention action resolves and highlights its target
before advancing read state through that intentionally visited mention. Missing
targets never advance read state. Ordinary page loads do not imply reading.

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
explicit mention advancement and no stale/failed-target read. Verify timestamp
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
