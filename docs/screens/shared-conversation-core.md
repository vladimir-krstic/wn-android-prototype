# Shared conversation core

Status: Implemented; static verification complete on 2026-08-15

## Source evidence

- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-shared.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-fiatjaf.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-support.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/chat-invitation.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/disappearing-message-indicators.md`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatModels.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatFixtures.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatOperations.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Conversation/PrototypeMessageBubble.swift`

## Android-native adaptation

- One typed full-screen destination renders direct, group, support,
  invitation, left, removed, blocked, and missing-relay states.
- A Material center-aligned top app bar combines Back, avatar, title, and a
  concise group-member or disappearing-message subtitle. Info navigation is
  reserved for Batch 6.
- The timeline uses a keyed `LazyColumn`, derived chronological day sections,
  sticky date headers, and programmatic initial/new-message positioning. This
  follows the current official Android lazy-list guidance:
  <https://developer.android.com/develop/ui/compose/lists>.
- A standard Material text field and Send button provide the bounded text
  entry needed to prove active/invitation/ended transitions. Batch 4 replaces
  this bounded composer with the complete multiline/attachment/speech system.
  Current text-input authority:
  <https://developer.android.com/develop/ui/compose/text/user-input>.
- Outgoing bubbles use the semantic primary/on-primary pair; incoming bubbles
  use surface variant. Fully rounded, tail-free shapes and compact same-author
  spacing preserve product identity without copying Apple Messages geometry.
- Android accessibility actions and visible buttons own retry and invitation
  decisions. Gesture-only message actions remain Batch 5.

## Timeline contract

- Every entry has a stable ID, logical day, deterministic chronological order,
  and a visible time where applicable.
- Messages carry author, text, reply target, reactions, delivery state,
  deletion state, and bounded attachment summaries.
- Date headers remain in transcript order and pin as the timeline scrolls.
- A cluster contains adjacent messages from the same author on the same day
  no more than five minutes apart. Events and notices break clusters.
- Direct chats omit transcript author identity. Group chats show an incoming
  author label at cluster start and avatar at cluster end.
- Terminal messages show time. Failed outgoing messages show **Not delivered,
  tap to retry** and retry to Sent. Deleted messages remain as **You deleted
  this message.** or **This message was deleted.**
- Reply quotes show the resolved author/body or **Original message
  unavailable** when the target is missing or deleted.
- Reactions show emoji and counts, with current-profile participation exposed
  in state semantics. Full reaction interaction remains Batch 5.
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
  Relays** recovery copy; actual relay editing is Batch 6.
- White Noise Support uses stable ID `white-noise-support`, one shared model,
  and cannot duplicate. A new support chat requires at least one available
  profile Chat Message relay.
- Opening clears unread state. Sending nonblank text appends one deterministic
  outgoing message, clears the draft, updates the Chats preview, and scrolls
  to the newest entry.

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
  the instrumentation APK. Device execution and visual inspection remain
  deferred until requested.
- The clean static gate passes with zero lint issues and no new permission.
