# Shared conversation core

Status: Visual-polish refresh implemented; static verification complete on
2026-08-21; device acceptance pending

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
- Selection uses a standard checkbox role and state rather than Unicode
  selected/unselected glyphs. Failed delivery keeps a visible 48 dp retry
  action with warning icon and error text.
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
  Relays** recovery copy; the action now opens the established Chat Info route
  directly, where the chat-owned relay destination already lives.
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
  the instrumentation APK. Focused coverage also verifies the clickable
  identity, direct relay recovery, named failed-send retry, support notice,
  and existing 200% font-scale RTL composition. Device execution and visual
  inspection remain deferred until requested.
- The clean static gate passes with zero lint issues and no new permission.

## Current official Android sources

- [App bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Lazy lists and sticky headers](https://developer.android.com/develop/ui/compose/lists)
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Set up window insets](https://developer.android.com/develop/ui/compose/system/insets-ui)
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input)
