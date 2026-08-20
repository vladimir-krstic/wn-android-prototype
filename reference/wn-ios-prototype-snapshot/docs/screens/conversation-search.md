# Conversation search

## Purpose

Find text and searchable attachment labels without leaving the conversation.
Signal's user-supplied iPhone screenshots are bounded comparison evidence for
keeping the transcript visible, navigating matches in place, and showing the
current result count. No Signal source is copied or required.

## Navigation and copy

- **Search** in Chat Info or Group Info returns to the conversation and opens a
  focused top search field with the prompt **Messages**.
- The native in-field Clear control clears only the query. The search
  presentation's Close action ends search and leaves the transcript at its
  current position.
- A nonempty query reports **0 matches** or **N of M matches** above the
  keyboard.

## Native components and approved composition

- A native UIKit `UISearchBar` hosted in SwiftUI's top `safeAreaBar` owns the
  search field, focus, Clear control, and keyboard. A separate native Close
  button ends search. This user-approved composition fixes the field at the
  top because iOS 27 can adapt SwiftUI `searchable` placement requests into a
  bottom toolbar even when `navigationBarDrawer` is requested. The Close
  action uses the system `.circle` button border shape and `.large` control
  size. The row reuses the app's approved field-adjacent control spacing: a
  4-point stack gap combines with `UISearchBar`'s native trailing inset, while
  SwiftUI supplies the outer safe-area padding.
- `safeAreaBar(edge:)` owns the keyboard-adjacent result controls. Two native
  Buttons with `chevron.up` and `chevron.down` step through results inside one
  compact Liquid Glass capsule. A separate glass count capsule appears only
  after entry begins and remains centered on the screen independently of the
  leading navigation capsule.
- The joined previous/next capsule, count capsule, transcript dimming, and
  cyan inline term background are approved Signal-informed custom composition
  because the native search field doesn't provide in-document match navigation
  or emphasis. Search terms reuse the existing mention `TextRenderer`'s
  continuous rounded corners instead of square attributed-string backgrounds,
  but remain flush with the matched glyph run and don't inherit the mention
  treatment's horizontal inset.

## Behavior and states

- Search updates as each character is entered. It continues to use the
  conversation's existing case-insensitive matching of message text, sender
  names, file names, and attachment labels.
- Results are numbered newest first. A new query selects and centers the newest
  match. Up moves to an older match; Down moves to a newer match. Controls are
  disabled when movement in their direction isn't possible.
- While a match is selected, its message remains at full contrast; the rest of
  the transcript is subdued. Every visible occurrence of the query in message
  text, group sender names, file names, and link text receives a rounded cyan
  background with black foreground. Its horizontal bounds match the selected
  characters exactly so adjacent letters remain unobscured. Clearing or
  closing search restores the transcript immediately.
- The normal composer, invitation, selection, and recovery bars are replaced
  by search navigation only while search is open. Search doesn't mutate the
  draft or chat.

## Accessibility and localization

- The field is named **Search Messages**. Previous and next controls expose
  those actions by name rather than relying on their arrows.
- The current message announces its result position, and the visible count
  uses locale-ready plural copy and monospaced digits.
- Disabled state, full-versus-subdued contrast, spoken result position, and
  enabled navigation accompany color highlighting. Dynamic Type may expand
  the count capsule without clipping.

## Apple sources

- [Search fields](https://developer.apple.com/design/human-interface-guidelines/search-fields)
- [Search modifiers](https://developer.apple.com/documentation/swiftui/view-search)
- [Adding a search interface](https://developer.apple.com/documentation/swiftui/adding-a-search-interface-to-your-app)
- [`UISearchBar`](https://developer.apple.com/documentation/uikit/uisearchbar)
- [`UISearchBarDelegate`](https://developer.apple.com/documentation/uikit/uisearchbardelegate)
- [`ButtonBorderShape`](https://developer.apple.com/documentation/swiftui/buttonbordershape)
- [`GlassButtonStyle`](https://developer.apple.com/documentation/swiftui/glassbuttonstyle)
- [`TextRenderer`](https://developer.apple.com/documentation/swiftui/textrenderer)
- [`safeAreaBar(edge:alignment:spacing:content:)`](https://developer.apple.com/documentation/swiftui/view/safeareabar(edge:alignment:spacing:content:))
- [Focus](https://developer.apple.com/documentation/swiftui/focus)

Apple's current guidance favors immediate results, a scope-specific prompt,
and top placement when bottom content is a primary function. Here, the message
composer is that bottom function.

## Acceptance

- Search never opens a modal result list.
- Opening Search focuses **Messages** and opens the keyboard over the existing
  conversation, with the field at the top of the navigation area.
- Typing immediately centers the newest match, shows the correct newest-first
  count in a screen-centered capsule, highlights visible occurrences with
  flush rounded backgrounds, and subdues noncurrent content.
- Up and Down visit every match in order, preserve the keyboard, and update the
  count and spoken position.
- Clear restores the unfiltered transcript while keeping search open; Close
  restores the normal conversation bar and keeps the current scroll position.
- Empty and no-result queries never enable match navigation or crash.
