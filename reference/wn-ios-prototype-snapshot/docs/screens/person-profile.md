# Person profile

## Purpose

Present another person's identity consistently with the active profile and make
the common relationship and conversation actions clear.

## Navigation and copy

- A person profile is pushed from New Chat, member lists, and other chat entry
  points. Its navigation title is **User Profile**, describing the destination
  without repeating the person's name from the identity header.
- The identity presentation reuses the same components as **Share & Connect**:
  a circular avatar occupying one third of the available width, the person's
  name, Verified Nostr Address, and compact copyable `npub` capsule.
- Copying the public key replaces the copy symbol with a checkmark briefly,
  produces native success feedback, and announces the result to VoiceOver.
- When a bio is present, it immediately follows the avatar and name in a native
  one-row `Section`. It uses the same page width, row sizing, insets, and system
  corner radius as the grouped action container below, with
  a user-approved 50% `quaternarySystemFill` background. The complete bio uses
  centered, italic, secondary gray `subheadline` text, matching the Verified
  Nostr Address size, with no **About** label, divider, icon, or border and
  retains the native row's standard internal insets. The identity header omits
  its usual trailing padding on this screen. The user-approved eight-point
  list-section spacing keeps it visually attached to the name and the identity
  values below. A person without a bio has no bio section.
- The person's compact secondary Verified Nostr Address follows the bio card
  and uses the same maximum 38-character middle abbreviation as one-on-one
  Chat Info, avoiding the list section's rounded clipping edge. It shows
  `checkmark.seal.fill` only when verified. The unchanged npub capsule follows
  eight points below as the final compact identity value. A 16-point trailing
  inset after the npub combines with section spacing to preserve 24 points
  before profile actions. When no bio exists, the address and npub remain in
  the identity header directly beneath the name.
- When at least one shared group exists, a compact **Groups in Common**
  navigation row appears beneath the identity header. It uses the approved
  overlapping-avatar stack: up to three 32-point group avatars and a `+N`
  circle for overflow. A person with no shared groups has no Groups in Common
  row; the same position instead shows **Add to Group** with the established
  `person.2.badge.plus` symbol and opens the existing add-to-group sheet. When
  present, Groups in Common shares the same grouped action container as the
  contact and block rows. Monogram and fallback avatars use an opaque adaptive
  gray surface so overlapping imagery never shows through them.
- The row pushes **Groups in Common**. That page shows every shared group using
  group avatar, headline name, and secondary member count. Group rows contain
  no message preview, timestamp, unread state, mute, delivery, membership, or
  other status treatment. Tapping a row explicitly opens that conversation.
- **Add to Another Group** remains the final row in the same grouped container
  as the shared groups on the Groups in Common page. It is secondary
  relationship management, not a prominent primary action. It uses the native
  system-image Button initializer with the established
  `person.2.badge.plus` relationship symbol, allowing SwiftUI's `Label` to own
  its text-matched scale, spacing, baseline, and list inset. It has no
  disclosure chevron because it starts the scoped add-to-group sheet rather
  than navigating deeper in the current hierarchy. The resulting selection
  sheet uses the same explicit title.
- Deterministic fixture coverage keeps Radia Perlman in three shared groups,
  David Chaum in one, and Maya Chen in enough groups to exercise the `+N`
  overflow state.
- The first four direct-chat people provide deterministic two-, two-, three-,
  and four-line bio examples. Two include emoji and two use text only, allowing
  quick inspection of multiline alignment and row growth.
- **Add Contact** and **Remove Contact** toggle immediately in place. This
  reversible relationship action does not present a confirmation dialog.
- **Add to Another Group** opens the existing grouped-list sheet. Each available group
  uses the chat-list row hierarchy: group avatar, headline name, and secondary
  member count. It deliberately omits last-message content, timestamps, unread
  state, delivery state, mute state, and other chat-list status indicators.
  Selecting a group presents one standard alert: **Add [person] to [group]?**,
  **They’ll be added as a member of this group.**, **Cancel**, and **Add**.
  Confirming adds the member, provides native success feedback and a VoiceOver
  announcement, dismisses the sheet, and returns to the screen that presented
  it. It does not open the conversation automatically.
- **Block** sits in the same secondary-action group as the contact action and,
  when applicable, Groups in Common. Its person-removal symbol and label are
  both destructive red. A standard alert confirms blocking. After blocking,
  the same row becomes the neutral **Unblock** action and unblocks immediately.
- **Message** follows the secondary actions as the final full-width prominent
  primary action. It uses the same `plus.bubble` symbol as other chat-starting
  actions and has 24 points of separation from the action group. When required
  profile Chat Messages relays are unavailable, the existing relay-recovery
  destination replaces it with the same spacing.
- When Chat Info's About quick action opens User Profile, the same identity,
  bio, shared-groups, contact, and block content remains, but Message is
  omitted because that direct chat is already open.

## Native components

`NavigationStack`, `List`, `Section`, `Button`, `NavigationLink`, the prominent
glass button style, system typography, semantic colors, SF Symbols, a native
alert, clipboard feedback, and accessibility announcements own the presentation
and behavior.

The profile identity header is a single shared SwiftUI component used by both
the active profile and person profiles. The app does not maintain visually
similar duplicate implementations.

## States and accessibility

- The contact label and symbol always describe the next action.
- The primary action remains a minimum system-sized, full-width target and uses
  adaptive foreground/background colors in light and dark appearances.
- Avatar, name, public-key copy action, bio, and actions remain separate,
  understandable VoiceOver elements.
- The bio reads as its complete italic text without a visible About label,
  remains centered at the Verified Nostr Address's `subheadline` size, and grows
  with Dynamic Type without truncating the text.
- Verified Nostr Address announces the address and either Verified or Not
  verified; the visual seal is never the only accessible state.
- Blocking never removes history. Relay recovery remains available when a new
  direct chat cannot yet be created.

## Governing sources

- [Form](https://developer.apple.com/documentation/swiftui/form)
- [Layout](https://developer.apple.com/design/human-interface-guidelines/layout)
- [Lists and tables](https://developer.apple.com/design/human-interface-guidelines/lists-and-tables)
- [NavigationLink](https://developer.apple.com/documentation/swiftui/navigationlink)
- [ContentUnavailableView](https://developer.apple.com/documentation/swiftui/contentunavailableview)
- [quaternarySystemFill](https://developer.apple.com/documentation/uikit/uicolor/quaternarysystemfill)
- [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets)
- [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)
- [GlassProminentButtonStyle](https://developer.apple.com/documentation/swiftui/glassprominentbuttonstyle)
- [Alerts](https://developer.apple.com/documentation/swiftui/view/alert(_:ispresented:actions:message:))
- [Sensory feedback](https://developer.apple.com/documentation/swiftui/view/sensoryfeedback(_:trigger:))

## Acceptance criteria

- The active profile and person profile use identical avatar sizing, name
  typography, npub capsule, truncation, copy transition, feedback, and spacing.
- A person's bio appears directly after the avatar and name in a one-row
  light-gray native section with the same width, row geometry, and corner radius
  as the white action section below. It uses centered italic secondary text at
  the Verified Nostr Address's `subheadline` size with no About label, retains
  standard internal row padding, has eight-point external spacing above and
  below, and is absent when no bio exists.
- Verified Nostr Address and the public-key capsule appear beneath the bio, with
  the address matching one-on-one Chat Info's compact abbreviation, the
  trailing seal only for verified values, and 24 points before profile actions.
  Without a bio they remain inside the identity header below the name. The npub
  capsule's styling, truncation, position, and behavior remain unchanged.
- The profile represents one or more shared groups with one compact
  avatar-stack row; the row is absent when there are no shared groups. The
  complete list and secondary Add to Another Group action live on its
  destination page. With no shared groups, Add to Group appears directly in the
  profile action container and opens the same scoped selection flow.
- Message is the final, visually dominant primary action and uses the shared
  new-chat symbol when User Profile is opened before starting a chat. It is
  separated from profile actions by 24 points and is absent when the screen is
  reached from an existing chat's About action.
- Add/Remove Contact changes state without a modal.
- Contact and Block share one secondary-action container; Block remains visibly
  destructive, confirms in an alert, and changes to Unblock.
