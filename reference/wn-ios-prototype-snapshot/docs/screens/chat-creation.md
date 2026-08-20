# Chat creation

## Purpose

Start a direct chat through a person's profile or create a named group without
leaving deterministic in-memory prototype state.

## Navigation and copy

- **New Chat** is pushed from Chats. It uses native search with **Name or
  npub**, shows **New Group** first, and then the profile's People directory.
- **New Group** uses a standard `Label` with the uncontained outline
  `person.2` SF Symbol. Its narrower geometry keeps the action optically aligned
  with the people rows without compensating padding. `Label`, the symbol's
  native scale, and `List` own its alignment, spacing, row insets, disclosure
  indicator, and pressed state; the app adds no circle, fill variant, icon
  frame, or decorative background.
- Choosing a person pushes their profile. Its identity header is shared with
  **Share & Connect**, **Message** is the prominent primary action, contact
  membership toggles immediately, and profiles with shared groups expose
  **Groups in Common**, which owns the secondary **Add to Another Group**
  action. Profiles without shared groups omit that row. See
  [Person profile](person-profile.md). Message opens the existing direct chat
  or creates one; duplicate direct chats are never created.
- **New Group** pushes a searchable multi-selection list. **Continue** requires
  at least one other person.
- Selected people appear as larger avatar items in a full-width horizontal
  `ScrollView`. Its section removes the grouped list's horizontal margins so
  the scroll viewport reaches both screen edges; only the scroll content keeps
  the normal resting margin, aligned with the people card below. The strip
  starts at its leading edge, preserves selection order, scrolls fully off
  either screen edge, and has no grouped-card background. Each remove badge is
  centered on the avatar's upper-trailing circular edge and remains inside its
  item bounds. The selected section removes its bottom section margin so its
  gap before the people card matches the header-to-avatar rhythm above. Avatar
  item width and stack spacing produce an 18-point visible gap between adjacent
  64-point images.
- **Set Up Group** owns the optional photo, required **Group Name**, optional
  **Description**, member review, and **Create Group**.
- **Set Up Group** retains the grouped `Form`'s native system background; it
  does not hide the scroll content background or replace it with plain white.
- Group photo editing is identical to Sign Up and Profile editing: **Choose
  from Photos**, **Choose from Files**, **Find Image on Web**, and destructive
  **Remove Photo** when an image exists. The same avatar presentation,
  preparation progress, failure copy, and system pickers are used.
- Before a photo is chosen and while **Group Name** is empty, the avatar shows
  the outline two-person symbol on a neutral system-fill circle. Entering a
  name replaces the symbol with its first letter on the standard accent avatar.
- **Create** is the trailing primary toolbar action. It remains disabled until
  the trimmed group name is nonempty and photo preparation has finished.
- A new group makes the active profile an admin, copies that profile's available
  Chat Messages relays, appends **You created the group.**, and opens the chat.
- Successfully starting a direct chat or creating a group completes the
  creation task. Its conversation replaces the creation routes in the Chats
  navigation path, so Back or the interactive back gesture returns directly to
  the Chats list and cannot reopen Person, New Group, or Set Up Group.

## Native components

`NavigationStack`, `NavigationLink`, `.searchable`, `List`, `Form`, `TextField`,
horizontal `ScrollView`, `PhotosPicker`, `fileImporter`,
`ContentUnavailableView`, `primaryAction`, and native disabled states own
presentation and interaction.

SwiftUI provides interactive keyboard dismissal while scrolling but no
background-only tap-dismiss modifier for a `Form`. Group setup therefore
installs one bounded UIKit tap recognizer while visible. It ignores touches
inside text inputs, sets `cancelsTouchesInView` to false so buttons and links
still receive the tap, and changes only SwiftUI focus state.

## States and accessibility

- Search covers names and public keys; no results use the native search-empty
  presentation.
- Back or Cancel before creation makes no mutation.
- Group Details uses focus state and interactive scroll dismissal. A tap outside
  either text field also ends editing without preventing the tapped control's
  own action.
- The active profile never appears as a selectable person.
- White Noise Support is a service destination, not a person. It never appears
  in New Chat, New Group, or Add People results and cannot be added to a group.
- Selection state is visible, spoken, and not color-only. Avatar-only actions
  have explicit labels.
- Without an available profile Chat Messages relay, Chats keeps the accepted
  relay-recovery toolbar action and does not enter creation.

## Governing sources

- [NavigationStack](https://developer.apple.com/documentation/swiftui/navigationstack)
- [Search](https://developer.apple.com/documentation/swiftui/view-search)
- [Lists](https://developer.apple.com/design/human-interface-guidelines/lists-and-tables)
- [Label](https://developer.apple.com/documentation/swiftui/label)
- [SF Symbols](https://developer.apple.com/design/human-interface-guidelines/sf-symbols)
- [Toolbars](https://developer.apple.com/design/human-interface-guidelines/toolbars)
- [Confirmation action](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/confirmationaction)
- [ScrollView](https://developer.apple.com/documentation/swiftui/scrollview)
- [listSectionMargins](https://developer.apple.com/documentation/swiftui/view/listsectionmargins(_:_:))
- [contentMargins](https://developer.apple.com/documentation/swiftui/view/contentmargins(_:for:))
- [defaultScrollAnchor](https://developer.apple.com/documentation/swiftui/view/defaultscrollanchor(_:for:))
- [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker)
- [fileImporter](https://developer.apple.com/documentation/swiftui/view/fileimporter(ispresented:allowedcontenttypes:allowmultipleselection:oncompletion:))
- [Focus](https://developer.apple.com/documentation/swiftui/focus)
- [Interactive keyboard dismissal](https://developer.apple.com/documentation/swiftui/scrolldismisseskeyboardmode/interactively)
- [UIGestureRecognizerDelegate](https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate)
- [cancelsTouchesInView](https://developer.apple.com/documentation/uikit/uigesturerecognizer/cancelstouchesinview)
- [ContentUnavailableView](https://developer.apple.com/documentation/swiftui/contentunavailableview)

The current White Noise app supplied bounded behavioral comparison evidence for
the person-profile step and two-stage group creation. This brief is the durable
local authority; implementation does not depend on that external source.

## Acceptance criteria

- Direct creation deduplicates and opens the correct chat.
- After either direct or group creation succeeds, the conversation is the only
  destination above Chats; navigating back returns directly to the chat list.
- Group selection, setup, relay copying, membership, initial admin role, system
  event, list insertion, and navigation occur as one successful mutation.
- Selected avatars are unobstructed, horizontally scrollable, removable, and
  announced as selected/removal actions without relying on color. The first
  item rests on the people card's leading alignment while scrolling content
  enters and exits at the physical screen edges without an inset clip. Remove
  badges sit on the avatar perimeter, and the vertical spacing above and below
  the strip reads as one balanced rhythm.
- Group photo sources and states match Sign Up/Profile, the fallback monogram
  tracks the first letter of the group name, the grouped system background is
  visible, tapping outside dismisses the keyboard, and Create occupies the
  trailing primary toolbar position.
- All creation state is profile-scoped and survives navigation until process
  exit.
