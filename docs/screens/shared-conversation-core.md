# Shared conversation core

2026-09-04 production B08 extends this accepted surface with paged history,
complete local search and target recovery, captured unread boundaries,
visible-only reading, unread-mention jumps and richer Message Details. See
[conversation-history-and-reading.md](conversation-history-and-reading.md) for
the current contract and 319-test host gate. The device evidence below describes
earlier builds; B08 UI tests were compiled only and visual acceptance is pending.

Status: 2026-09-01 pinned-fixture parity audit and conversation-surface
correction implemented. The 77-row chat inventory is intact, every specialized
catalog timeline now carries the complete pinned iOS scenario sequence rather
than a representative subset, and the retained Maya Chen, Weekend Walks, and
Fiatjaf histories are complete. The date-marker refinement separates ordinary
inline day text from the viewport-derived current-day pill and passes the host
unit/build/static gate; device and visual verification of that refinement remain
unclaimed until explicitly requested. Earlier physical Pixel 8a suite evidence
remains historical evidence for the preceding conversation batch. User visual
acceptance remains separate.

The 2026-09-03 height-based single-media correction passes unit tests, lint,
app assembly, and instrumentation-test APK compilation. Tests cover actual-image
dimension precedence, portrait/landscape sizing, invalid dimension fallback,
and tiny sources. Current-build device visual acceptance remains pending.

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
  ordinary in-list date headers, a separately derived current-day overlay, and
  programmatic initial/new-message positioning. The overlay is present only
  while the active day's own in-list header is outside the viewport; it yields
  to that ordinary header when the two meet. This follows the current official
  Android lazy-list state guidance:
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
- In-transcript day headers use the same quiet centered `labelMedium` system
  text treatment as ordinary timeline information, with heading semantics and
  no container. Only the current day whose beginning is no longer visible gets
  the separate top capsule. That capsule uses an 82% `surfaceDim` neutral with
  full-emphasis `onSurface` text. Material defines this role as consistently
  dimmer than the base surface in both appearances, giving it clearer
  separation from an incoming `surfaceContainerHigh` bubble underneath while
  retaining background context. Support guidance keeps its restrained tonal
  notice surface, so date, event, and notice roles remain distinct.
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
- Date headers remain as ordinary centered text at their exact transcript day
  boundaries. The active day gets a pinned replacement only after its inline
  header leaves the viewport; the replacement disappears as soon as that
  inline header becomes visible again, including when scrolling back to the
  boundary between bubbles.
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
  unavailable** when the target is missing or deleted. An available quote is a
  named target that jumps to and briefly highlights the source message.
- Rich messages use one content canvas instead of independently measuring each
  child. A 6 dp bubble inset surrounds reply quotes, single media, galleries,
  GIFs, files, contacts, links, and voice cards; each child uses a concentric
  10 dp outline inside the 16 dp bubble. Sections keep 6 dp gaps and gallery
  tiles keep 2 dp gaps under one group clip. Albums and rich cards use the
  fixed 256 dp canvas. A lone photo/video uses decoded image proportions,
  falling back to declared dimensions only when pixels cannot be read. Its
  media height is 256 dp, with width derived from the actual ratio and capped
  at the 256 dp canvas maximum. `ContentScale.FillHeight` preserves the full
  top-to-bottom image, centers any horizontal overflow, and clips only the
  sides. A narrower parent caps width without reducing the media height.
  Album tiles retain their count-derived crops.
  The tiny-source safeguard and unavailable states remain. This user-directed
  2026-09-03 correction supersedes the pinned iOS clamp/crop presentation;
  deterministic catalog metadata and labels remain intact. A lone image's
  caption wraps to that same width, preventing text from widening the
  bubble and leaving unused space beside the media. Mixed text restores the
  ordinary text inset within the shared canvas. Deleted messages do not retain
  hidden attachment/reply layout.
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
- B08 opening captures concrete unread IDs. Settled visible rows advance reading;
  off-screen content stays unread. Sending nonblank text appends one deterministic
  outgoing message, clears the draft, updates the Chats preview, and scrolls
  to the newest entry.
- Opening an available conversation settles its exact entry target or captured
  unread boundary; otherwise it waits for the measured composer and lazy layout
  and settles the newest entry completely above the floating controls. Edge-to-edge content may draw behind the composer only while
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

- Maya Chen carries all 31 pinned direct-history messages, including mixed
  files, links, contact, reply, reactions, unavailable content, and voice.
- Weekend Walks carries all 42 pinned entries: 28 messages and 14 group events
  spanning creation, membership, identity, admin, description, and removal.
- Fiatjaf retains the exact eight-message portable-identity story, reply,
  reaction, and five-photo attachment summary.
- The specialized developer timelines are source-ordered and exhaustive:
  Direct Text 16, Dates 15, Replies 11, Reactions/Actions 21, Single Media 10,
  Gallery 8, Viewer 7, Rich Content 11, Voice 8, Group Messages 14, Identity
  Colors 10, and Group Events/Roles 22 entries.
- All 12 composer fixtures preserve their exact source message, draft text,
  reply target, link-suppression state, and ordered attachment payloads.
- Media fixtures retain frame dimensions, duration, availability, gallery
  counts, and viewer exclusion rules. File fixtures retain type, byte size,
  availability, and contact identity. Inline Markdown, detected URLs, and
  mentions render as interactive text while copy/search/read-aloud use the
  visible plain text.
- Developer catalog chats also cover invitations, ended membership, blocked,
  missing-relay, archived, support, and disappearing-message indicators.
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
- The latest user-directed density refinement reduces emoji-only pills to an
  exact 31×23 dp visual, lets counted and `+N` pills grow horizontally, caps
  transcript/focused summaries at four real types plus one `+N`, and reduces
  Sent state to a 14 dp filled container with 10 dp check artwork. Its follow-up
  moves visible reaction pills 2 dp farther over the bubble for a 9 dp overlap.
  The timestamp is measured independently at a fixed 2 dp bubble gap even at
  200% type. On 2026-09-01, all 58 `ConversationScreenTest` cases passed on the
  physical Pixel 8a after rebuilding and reinstalling both APKs.
- `ConversationFixtureParityTest` guards every specialized source-order ID,
  composer state, media payload, rich payload, lifecycle branch, retained
  authored history, and ordinary seed policy. The complete unit suite contains
  158 passing tests.

## Current official Android sources

- [App bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Lazy lists and sticky headers](https://developer.android.com/develop/ui/compose/lists)
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Set up window insets](https://developer.android.com/develop/ui/compose/system/insets-ui)
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input)
