# Shared conversation core

Status: 2026-08-31 conversation-surface correction implemented. Build, lint,
unit tests, and instrumentation compilation pass for the latest 23 dp /
four-plus-overflow refinement. The preceding revision passed the full Pixel 8a
suite and physical inspection; renewed device execution is pending after the
wireless device disconnected. User visual acceptance remains separate.

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/conversation-shared.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-fiatjaf.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-support.md`
- `wn-ios-prototype@0bd7cba:docs/screens/chat-invitation.md`
- `wn-ios-prototype@0bd7cba:docs/screens/disappearing-message-indicators.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatModels.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatFixtures.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatOperations.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeMessageBubble.swift`
- explicitly requested current-iOS comparison at
  `wn-ios-prototype@4c25393f0eb6` for bubble metadata, reactions, focused
  actions, selection, reply swipe, and bottom settling; this does not globally
  repin the Android baseline
- Signal Android `879651dc47a7b18b67e7aea52a25197875024680` as native Android
  interaction evidence only; no code or Signal-specific product policy is
  imported

## Android-native adaptation

- One typed full-screen destination renders direct, group, support,
  invitation, left, removed, blocked, and missing-relay states.
- A small Material top app bar combines Back, a compact clickable identity,
  group-member or disappearing-message status, Search, and the profile-gated
  Conversation Debug action. Left alignment gives the title a stable reading
  edge and room for multiple actions; the identity opens the established Chat
  or Group Info destination.
- The timeline uses a keyed `LazyColumn`, derived chronological day sections,
  sticky date headers, and programmatic initial/new-message positioning. This
  follows the current official Android lazy-list guidance:
  <https://developer.android.com/develop/ui/compose/lists>.
- The existing multiline/attachment/speech composer remains behaviorally
  intact. Its boundary now shares one low tonal surface with the lifecycle
  panels, stays within the adaptive conversation measure, and uses standard
  Add and Microphone vectors in its resting state. Rich composer content is
  intentionally reserved for rollout Batch 6. Current text-input authority:
  <https://developer.android.com/develop/ui/compose/text/user-input>.
- Outgoing bubbles use the semantic primary/on-primary pair; incoming bubbles
  use `surfaceContainerHigh`. Shared large/medium shapes, compact same-author
  spacing, and a full 16 dp cluster break preserve hierarchy without copying
  Apple Messages geometry.
- Day headers use a pinned tonal capsule and heading semantics. Ordinary
  events stay quiet text while support guidance uses one restrained tonal
  notice surface, so date, event, and notice roles remain visibly distinct.
- Selection uses one stable leading 48 dp Checkbox column for incoming and
  outgoing messages without changing bubble geometry. Failed delivery keeps a
  visible 48 dp retry action with warning icon and error text.
- Incoming group identity belongs to the bubble rather than its metadata: the
  30 dp avatar ends at the bubble edge and the author label follows the
  bubble's 12 dp body inset.
- Android accessibility actions and visible buttons own retry and invitation
  decisions. Reply swipe and deliberate-hold actions keep named semantics
  alternatives.

## Timeline contract

- Every entry has a stable ID, logical day, deterministic chronological order,
  and a visible time where applicable.
- Messages carry author, text, reply target, reactions, delivery state,
  deletion state, and bounded attachment summaries.
- Date headers remain in transcript order and pin as the timeline scrolls.
- A cluster contains adjacent messages from the same author on the same day
  no more than five minutes apart. Events and notices break clusters.
- Direct chats omit transcript author identity. Group chats show an incoming
  author label at cluster start and avatar at cluster end, with the avatar
  bottom aligned to the bubble rather than the reaction/time line.
- Terminal messages show time. Failed outgoing messages replace that time with
  **Not delivered, tap to retry** using the same end/right alignment, 12 dp
  edge inset, 2 dp top gap, 3 dp icon gap, `labelSmall` typography, and 14 dp
  status footprint; only the warning and label use the error color. Tapping the
  message retries to Sent. Deleted messages remain as **You deleted this
  message.** or **This message was deleted.**
- Reply quotes show the resolved author/body or **Original message
  unavailable** when the target is missing or deleted.
- Reactions occupy one bubble-attached metadata line opposite time. The visible
  summary never wraps. Emoji-only pills share a 31×23 dp minimum, counted and
  `+N` pills grow horizontally, and visible pills keep 3 dp gaps. They overlap
  the bubble bottom by 9 dp while retaining expanded 48 dp minimum interaction
  targets; the bubble grows with metadata to 340 dp and shows at most four real
  types plus one `+N` pill, reducing the real set further only when width is
  exhausted. Rail and timestamp retain 12 dp edge insets. A timestamp without
  reactions keeps the same directional edge—start/left for incoming and
  end/right for outgoing in LTR—and begins 2 dp below the bubble. RTL mirrors
  that relationship with the message. The 2 dp timestamp gap also applies
  when reactions are present; only pills overlap the bubble. Timestamp text,
  the Sent fill, and Sending progress use the lighter/lower-emphasis `outline`
  neutral. Outgoing Sent/Sending state appears beside time with a 14 dp filled
  status container and 10 dp check artwork.
  Current-profile participation
  remains exposed in state semantics.
- Support guidance is a centered notice, never an incoming message.

## Lifecycle states

- Pending invitation history is readable while the composer is replaced by
  **Decline** and **Accept**. Accept preserves history; group acceptance adds
  the active profile as a member and appends **You joined the group.** Decline
  confirms with the exact direct/group consequence, removes the chat, and
  returns to Chats.
- Left and removed chats preserve readable history and replace the composer
  with the accepted membership status.
- A blocked direct chat preserves history and reports that messaging is
  unavailable. Missing Chat Relays preserves history and exposes **Check Chat
  Relays** recovery copy; the action now opens the established Chat Info route
  directly, where the chat-owned relay destination already lives.
- White Noise Support uses stable ID `white-noise-support`, one shared model,
  and cannot duplicate. A new support chat requires at least one available
  profile Chat Message relay.
- Opening clears unread state. Sending nonblank text appends one deterministic
  outgoing message, clears the draft, updates the Chats preview, and scrolls
  to the newest entry.
- Opening an available conversation waits for the measured compact composer
  and lazy layout, then settles the newest entry completely above the floating
  controls. Edge-to-edge content may draw behind the composer only while
  scrolling.
- A leading-to-trailing 64 dp reply swipe moves bubble and metadata together,
  resists beyond the threshold up to 96 dp, mirrors in RTL, yields vertical
  motion to scrolling, and is unavailable when Reply is unavailable.
- Deliberate hold presents the real source message in a focused modal overlay,
  with quick reactions above and ordered actions below, aligned to message
  direction and shifted within system-safe window bounds. Tall real sources
  scale proportionally within a 320 dp preview budget. Both focused accessory
  gaps are 8 dp; the lower gap starts at a visible reaction pill rather than
  its transparent target edge.
- Message Details presents every reaction type and the people represented by
  it; compact transcript/context overflow never hides information there.
- Recipient Read Aloud, Stop Reading, Transcribe, Show/Hide Transcript, and
  Copy Transcript commands are conditional focused-message actions. Only
  active progress or a deliberately revealed transcript is shown inline.

## Deterministic fixture coverage

- Maya Chen remains the complete direct reference history.
- Weekend Walks remains the complete group reference history.
- Fiatjaf retains the accepted eight-message portable-identity story, reply,
  reaction, and five-photo attachment summary.
- Developer catalog chats cover text/delivery, dates, replies/deletion,
  reactions, group authors/clusters, group events/roles, invitations, ended
  membership, blocked, missing-relay, and disappearing indicators.
- Every retained row opens a non-crashing shared destination with readable
  deterministic content.

## Acceptance gates

- Unit tests cover entry ordering, sections, five-minute clustering, reply
  resolution, invitation accept/decline, support uniqueness, send/retry,
  list-preview updates, profile isolation, and composer availability.
- Compose tests for direct, group, invitation, and ended states compile into
  the instrumentation APK. On 2026-08-31, a focused 12-test slice passed on
  the physical Pixel 8a, covering bottom settlement, bounded reactions,
  source-preserving actions, conditional recipient speech commands, stable
  selection geometry, forwarding/configuration, LTR/RTL reply swipe, and group
  identity alignment.
- Physical Pixel 8a inspection covered direct and group transcripts, compact
  reactions and overflow, focused text and voice actions, tall media preview,
  selection, failed-message actions, and newest-message/composer clearance.
  The clean build, lint, unit, and instrumentation-APK gates pass with no new
  permission.
- The 2026-08-31 reaction-overlap follow-up passed six focused Pixel 8a tests
  and physical inspection for 7 dp pill overlap, adaptive bubble growth/`+N`,
  12 dp timestamp inset, equal focused gaps, exhaustive Message Details, group
  avatar geometry, source-preserving actions, and the same overlap/gap geometry
  at 200% type.
- The subsequent metadata correction passed five focused Pixel 8a tests and
  physical inspection for consistent 31×25 dp singleton pills, horizontally
  growing count pills, 3 dp pill gaps, 2 dp below-bubble timestamps on text and
  media, outgoing delivery state, adaptive overflow, reaction-aware focused
  spacing, and 200% type.
- After the correction, all 191 instrumentation tests passed on the physical
  Pixel 8a, including conversation, media viewer, full-height forwarding,
  selection, reply, gesture, accessibility, and scaled-type coverage.
- The latest user-directed density refinement reduces pills to 31×23 dp, caps
  transcript/focused summaries at four real types plus one `+N`, and reduces
  Sent state to a 14 dp filled container with 10 dp check artwork. Its follow-up
  moves visible reaction pills 2 dp farther over the bubble for a 9 dp overlap.
  Unit, lint, build, and instrumentation compile gates pass; renewed Pixel
  execution remains pending after disconnection.

## Current official Android sources

- [App bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Lazy lists and sticky headers](https://developer.android.com/develop/ui/compose/lists)
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Set up window insets](https://developer.android.com/develop/ui/compose/system/insets-ui)
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input)
