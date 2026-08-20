# Chat invitation

## Purpose and navigation

An incoming one-to-one or group invitation opens in the shared conversation
screen. The person can read the messages already sent to the chat before
deciding whether to participate. The native navigation stack continues to own
Back behavior.

## Copy and actions

- Status: **Invited to chat by <name>**.
- Bottom actions: **Decline** and **Accept**.
- Decline confirmation title: **Decline Invitation?**
- Direct-chat consequence: **This invitation and its messages will be removed
  from Chats.**
- Group consequence: **This group invitation and its messages will be removed
  from Chats.**
- Confirmation actions: **Cancel** and **Decline**.

## Native components and visual rules

- The complete received timeline remains readable before acceptance.
- One native secondary `Label` pairs `envelope.badge` with **Invited to chat by
  <name>**, using the same size, color, centering, and spacing as the existing
  left/removed conversation status.
- A bottom `safeAreaBar` replaces the composer while the invitation is
  pending. It lets SwiftUI extend the conversation's soft scroll-edge effect
  beneath the stationary actions, preserving the accepted progressive blur
  without an opaque backing or custom blur.
- **Decline** is a native destructive `Button` using `.glass`.
- **Accept** is the primary native `Button` using `.glassProminent`.
- The two controls share one horizontal row, native large control sizing,
  flexible button sizing, system margins, and system spacing.
- The buttons use text labels because both decisions need to remain explicit.
- In Chats, the pending invitation replaces message-preview content with
  **Invited to chat by <name>**. A simple `plus` symbol appears inside the same
  adaptive black 20-point circle used by the unread badge. It replaces unread
  count/dot presentation while the invitation is pending. The simple glyph is
  an approved optical refinement after the detailed envelope appeared cramped
  at this compact size.
- Conversation scrolling uses bottom anchors only for the initial position and
  for aligning a transcript shorter than its viewport. It does not run a
  second deferred initial `scrollTo` or re-anchor automatically for incidental
  container-size changes, preventing the first interaction with the invitation
  surface from moving the transcript.

## Important states and behavior

- Pending direct invitation: received messages are visible and the composer is
  absent.
- Pending group invitation: received messages and existing participant
  identity remain visible, but the invited profile is not yet a member.
- Accepting a direct invitation preserves history, changes the chat to active,
  and restores the standard composer.
- Accepting a group invitation preserves history, adds the current profile as
  a member, appends **You joined the group.**, and restores the composer.
- Declining asks for confirmation because it removes the invitation and its
  visible local history. Confirming removes the chat and returns to Chats;
  Cancel keeps the invitation unchanged.
- A pending invitation is not eligible for leaving, muting, forwarding into,
  group administration, or sending.

## Accessibility

- Both actions retain their visible text as their VoiceOver and Voice Control
  names.
- The invitation label is one readable status.
- Decline exposes the destructive role. Accept is presented after Decline in
  reading order and is the visually prominent action.
- Native buttons own Dynamic Type, minimum interaction geometry, pressed
  feedback, contrast adaptation, Reduce Transparency, and Reduce Motion
  behavior.

## Governing Apple sources

- [Adopting Liquid Glass](https://developer.apple.com/documentation/TechnologyOverviews/adopting-liquid-glass)
- [GlassButtonStyle](https://developer.apple.com/documentation/swiftui/glassbuttonstyle)
- [safeAreaBar](https://developer.apple.com/documentation/swiftui/view/safeareabar(edge:alignment:spacing:content:))
- [Label](https://developer.apple.com/documentation/swiftui/label)
- [defaultScrollAnchor](https://developer.apple.com/documentation/swiftui/view/defaultscrollanchor(_:for:))
- [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)
- [Alerts](https://developer.apple.com/design/human-interface-guidelines/alerts)

## Acceptance criteria

- The developer catalog contains one pending direct invitation and one pending
  group invitation with readable received messages.
- Both screens replace the composer with equal-width Liquid Glass **Decline**
  and **Accept** buttons over the existing progressive bottom blur.
- Both screens visibly identify the state and inviter in one line, and
  VoiceOver reads that status as one element.
- Both Chats rows replace received-message preview content with that same
  invitation line and show the circular invitation symbol instead of an unread
  count or dot.
- The first tap or drag after opening an invitation does not shift the
  transcript; short and long histories retain normal scrolling.
- Accepting either invitation keeps its history and immediately restores the
  active conversation composer; group acceptance also records the membership
  event.
- Declining either invitation presents the exact confirmation copy, removes
  the chat only after confirmation, and returns to Chats.
- The prototype remains deterministic and in memory, with no networking,
  persistence, authentication, or third-party dependency.
