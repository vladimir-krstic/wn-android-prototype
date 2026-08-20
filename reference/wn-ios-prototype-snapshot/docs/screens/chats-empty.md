# Chats — Empty

## Purpose and navigation

Show the main Chats destination when the current profile has no chats. The profile avatar reserves the future Settings entry point. New Message reserves the future chat-creation flow; neither destination is implemented in this batch.

## Copy

- Default navigation title: none
- Selected Filter labels: **Unread**, **Archived**, **Left**
- Chats empty title: **No Chats**
- Chats description: **Start a new chat to send a message.**
- Unread empty title: **No Unread Chats**
- Unread description: **You’re all caught up.**
- Archived empty title: **No Archived Chats**
- Archived description: **Chats you archive will appear here.**
- Left empty title: **No Left Chats**
- Left description: **Chats you leave or are removed from will appear here.**
- Search prompt: **Search Chats**
- Search empty title: **No Results**
- Search empty description: **Check the spelling or try a different search.**
- Filter choices: **Chats**, **Unread**, **Archived**, **Left**
- Actions: **Profile**, **Relay Setup** when needed, **Search Chats**, **Filter
  Chats**, **New Message**

## Native components

- `NavigationStack` without a visible navigation title in any scope.
- `ContentUnavailableView` with the current scope’s SF Symbol: `bubble.left.and.bubble.right`, `message.badge`, `archivebox`, or `rectangle.portrait.and.arrow.right`.
- Native `.searchable` presentation triggered by the Search toolbar button.
- One `ToolbarItemGroup` containing Filter, Search, and New Message in that order.
- A native `Picker` inside the menu with SF Symbols for every scope and the system checkmark on the selected scope.
- In Chats, the native Filter `Menu` uses an icon-only `line.3.horizontal.decrease` label with no selected fill. In Unread, Archived, and Left, the same Menu label becomes a compact selected capsule inside the shared toolbar group: the filter SF Symbol appears first and the scope text appears second.
- The selected Menu artwork has a user-approved 34-point height and ten-point trailing internal inset. Its background extends five points into the Menu item's otherwise empty leading inset, leaving the same five-point visible gap found above and below the capsule inside the 44-point toolbar control. It uses the adaptive app accent as its background and `systemBackground` as its foreground, producing black with white content in Light Mode and white with black content in Dark Mode. The native Menu and toolbar continue to own the interaction target, glass container, menu presentation, remaining outer item padding, and motion.
- The Filter menu’s disclosure indicator is hidden so SwiftUI does not reserve unused trailing space beside the icon-only label.
- A default toolbar New Message button using `plus.bubble`, sharing the group’s system Liquid Glass background without a prominent tint.
- System-provided toolbar Liquid Glass, spacing, hit regions, menus, search motion, typography, and materials.
- One approved custom profile element: the 44-point circular profile avatar. Marmota uses the bundled user-supplied photo, with a one-letter monogram as the fallback for profiles without an image. A native `NavigationLink` owns its interaction and opens Settings. Its avatar-specific button style preserves the approved full-contrast artwork during the interactive navigation transition instead of applying a transient dimmed label treatment. `sharedBackgroundVisibility(.hidden)` keeps the avatar outside Liquid Glass while matching the native controls’ row size.
- When the active profile has an unavailable relay role, the existing New Message
  toolbar slot becomes a native `NavigationLink` with the outlined orange
  `exclamationmark.triangle` symbol. The system toolbar retains the same Liquid
  Glass group and pressing the warning pushes Relays; the native Back action
  returns to Chats. No extra toolbar item or warning banner changes geometry.

## Important behavior

- Search mounts the native searchable presenter on demand, then activates it on the following UI update so pressing the toolbar action focuses the field and opens the keyboard.
- Populated fixtures demonstrate the Signal-style compact recency sequence: **Now**, **1m–59m**, a clock time for older messages today, **Yesterday**, weekday, then date. Labels remain fixed and deterministic rather than using the current clock.
- In populated rows, the avatar is vertically centered against the complete text block; title and timestamp share the top row; unread and failure indicators align to the top of the preview row so they track its first line even when the preview wraps.
- Group senders and **You:** use the emphasized variant of Apple’s semantic `subheadline` while the message remains Regular. The entire preview retains the same secondary color and Dynamic Type behavior.
- Attachment previews use one baseline-aligned SF Symbol plus concise text, all inheriting the same semantic `subheadline`, secondary color, and Dynamic Type behavior. The deterministic active list covers Photo, multiple Photos, Video, Voice message, File, Contact, Link, and GIF. No icon receives a custom frame or point size.
- Chats, Unread, Archived, and Left update the empty-state copy.
- Each scope’s empty state uses the same SF Symbol as its filter-menu choice.
- Filter, Search, and New Message remain inside one shared Liquid Glass capsule in every scope, with no prominent black New Message treatment. The toolbar remains titleless and no separate filter title or chip is inserted into the list.
- Selecting **Chats** in the native Filter menu clears Unread, Archived, or Left without clearing an active search query.
- List geometry is identical in every scope. At rest there is no extra header surface; Apple’s hard top scroll-edge treatment appears only when rows move under the toolbar.
- Filter menu icons are `bubble.left.and.bubble.right` for Chats, `message.badge` for Unread, `archivebox` for Archived, and `rectangle.portrait.and.arrow.right` for Left.
- A nonempty search query shows the search-specific empty state.
- The profile avatar opens Settings through native stack navigation. Both the Back button and interactive swipe-back return to the same enabled avatar appearance. New Message exposes a callback whose destination is deferred.
- The relay warning action appears while a relay role is unassigned,
  reconnecting, or disconnected and disappears as soon as every role has at
  least one connected assigned relay.
- The toolbar contains one unified trailing glass group so the three chat-level actions read as a quiet, related control cluster.

## Accessibility

- Every icon-only toolbar action has a concise accessibility label.
- The Filter menu exposes the selected scope as its accessibility value.
- `ContentUnavailableView`, `Menu`, `Picker`, toolbar buttons, and search retain their native semantics and Dynamic Type behavior.
- The Filter’s custom artwork remains inside the native Menu’s system-owned 44-point interaction target.

## Apple developer references

- [ContentUnavailableView](https://developer.apple.com/documentation/swiftui/contentunavailableview)
- [Refining Liquid Glass in toolbars](https://developer.apple.com/documentation/swiftui/landmarks-refining-the-system-provided-glass-effect-in-toolbars)
- [ToolbarSpacer](https://developer.apple.com/documentation/swiftui/toolbarspacer)
- [sharedBackgroundVisibility](https://developer.apple.com/documentation/swiftui/toolbarcontent/sharedbackgroundvisibility(_:))
- [Search modifiers](https://developer.apple.com/documentation/swiftui/view-search)
- [Adding a search interface](https://developer.apple.com/documentation/swiftui/adding-a-search-interface-to-your-app)
- [Menus and commands](https://developer.apple.com/documentation/swiftui/menus-and-commands)
- [SF Symbols](https://developer.apple.com/sf-symbols/)
- [Typography](https://developer.apple.com/design/human-interface-guidelines/typography)

Optional Apple product comparisons below preserve historical visual context.
They are not platform authority and are not required to implement this screen.

- [Send and reply to messages on iPhone](https://support.apple.com/en-ca/guide/iphone/-iph82fb73ba3/ios)
- [About iMessage](https://support.apple.com/guide/iphone/about-imessage-iph4e9799206/ios)
- [Use iMessage apps in Messages on iPhone](https://support.apple.com/guide/iphone/use-imessage-apps-iphf9c9c01d3/ios)

## Comparative reference

These shipped-app source links are optional comparison evidence. The local
brief remains complete and authoritative.

- [Signal iOS `DateUtil`](https://github.com/signalapp/Signal-iOS/blob/main/SignalServiceKit/Util/DateUtil.swift) — comparative evidence for compact chat-list recency labels and their transition points.
- [Signal iOS `ChatListCell`](https://github.com/signalapp/Signal-iOS/blob/main/Signal/src/ViewControllers/HomeView/Chat%20List/ChatListCell.swift) — comparative evidence that the conversation-list timestamp refreshes dynamically while recent.

## Acceptance

- The empty state is centered and entirely system laid out.
- Filter, Search, and New Message appear in one system Liquid Glass group with visually consistent leading, inter-item, and trailing insets.
- Chats uses the plain Filter glyph with no black selected background. Unread, Archived, and Left show their name and Filter glyph on one compact adaptive selected capsule inside the same shared toolbar group.
- New Message uses the same default toolbar treatment as Filter and Search and remains the trailing action inside their shared pill.
- The profile avatar appears independently at the leading edge, without Liquid Glass, and matches the controls’ 44-point row size.
- An unavailable relay role replaces New Message with one compact outlined
  orange recovery button in the same system toolbar group; it opens Relays and
  Back restores the same Chats destination.
- Tapping the profile avatar pushes Settings, and completing an interactive swipe-back restores the avatar without a stale pressed or disabled appearance.
- The toolbar remains title-free in every scope and the list never receives a separate filter header or dismissible chip.
- Selecting Chats in the Filter menu returns to the unfiltered scope.
- Search presents and focuses the native field, opens the keyboard, and filters the current scope; Filter shows icons and a checked native menu choice.
- No custom toolbar capsule, glass material, empty-state layout, menu, search field, or motion is implemented.
- The selected Filter label’s 34-point height, ten-point trailing internal inset, five-point leading background extension, semantic capsule fill, and symbol-plus-text composition are the approved custom exception required to communicate the selected scope with even visible insets without separating the control from its native toolbar group.
