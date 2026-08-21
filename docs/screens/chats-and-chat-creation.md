# Chats and chat creation

Status: Chats direction accepted; chat-creation visual refresh implemented and
statically verified on 2026-08-21; device visual acceptance pending

## Source evidence

- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Chats/ChatsView.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Chats/NativeChatList.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Chats/ChatListItem.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Chats/ChatListFixtures.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Chats/NewChatView.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatModels.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatFixtures.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatOperations.swift`
- The corresponding iOS briefs indexed by `docs/port/source-map.md`.

## Android-native adaptation

- Use a clean Material 3 top app bar with the active-profile avatar, the active
  scope as title, a filter action, and a collapsed search action.
- The avatar opens Settings; profile switching and profile addition live in
  the Settings profile header rather than in a Chats-owned switcher.
- Enter search from the top-app-bar search action and expose a normal Android
  back/cancel affordance while search is active.
- Expose Chats, Unread, Archived, and Left through a Material dropdown menu
  opened by the adjacent filter action. The active scope remains visible in
  the title and accessibility state; a non-default scope also gives the filter
  action a restrained tonal selected treatment.
- Do not show **Read All** as a persistent top-app-bar action. Preserve the
  authoritative model operation for any bounded contextual treatment selected
  later; visual polish does not silently remove product behavior.
- Use a `LazyColumn` for rows and a floating action button for **New Message**.
  The list sits directly on the ordinary screen surface: no enclosing rounded
  container and no individual card around each normal chat row.
- Use a row overflow action and a Material modal bottom sheet for the complete action set. This replaces UIKit multi-action swipe geometry while preserving every operation with discoverable, keyboard-accessible targets.
- Use Material alert dialogs for destructive and constrained operations.
- Use full-screen destinations for New Message, person profile, member selection, group setup, and the temporary conversation entry screen. Successful direct or group creation clears the creation flow before opening the conversation.
- Keep the compact layout phone-first. Expanded-width list/detail behavior is deferred until the conversation surface exists in Batch 3; content still respects the repository adaptive width boundary.
- New Message and New Group keep search visible because finding people is the
  primary task on both destinations. The rounded Material search field uses a
  leading search symbol, an on-demand clear action, and the shared no-results
  composition; New Group remains the first distinct tonal action on New
  Message.
- New Group uses selected `InputChip` items with avatars and explicit removal,
  plus toggleable person rows with a vector check. Stable order and selection
  semantics replace the previous Unicode check and remove glyphs.
- Person Profile uses a bounded identity hierarchy, compact role treatment,
  tonal About and secondary-action groups, semantic verified state, and one
  pinned primary Message action. When Chat Message relays are unavailable,
  that action becomes **Check Chat Relays** and opens the established recovery
  destination.
- Set Up Group uses the shared fully rounded tonal fields with label, input,
  icon, and supporting copy on one 16 dp content line, plus a compact tonal
  photo action, visible preparation/error states,
  a grouped read-only member review, and a bounded 56 dp pinned Create action.
  Its empty unnamed avatar uses a semantic group symbol rather than a fake
  profile initial; the preview becomes the name monogram or chosen photo as
  input changes. Photo Picker and Files remain completely system-owned.

## Deterministic data contract

- Marmota starts with exactly 77 chats: 72 non-archived and 5 archived.
- Exactly one chat is initially pinned.
- The first ten rows, in order, are the five direct catalog scenarios followed by Composer Text, Multiline, Link, Link Preview, and Photo.
- The five archived IDs are `catalog-direct-archived`, `road-trip`, `garden-club`, `old-studio`, and `design-notes`.
- Add-profile identities have the deterministic people directory and no chats.
- Available Chat Message relay URLs are copied into newly created direct and group chats. The prototype never performs network work.

## Scope and search behavior

- Chats contains non-archived chats whose membership has not ended.
- Unread contains unread or manually marked-unread non-archived chats whose membership has not ended.
- Archived contains archived chats.
- Left contains non-archived chats whose membership is left or removed.
- Search applies inside the selected scope across title and visible preview. Matching is case- and diacritic-insensitive.
- Search empty state: **No Results** / **Check the spelling or try a different search.**
- Scope empty states:
  - Chats: **No Chats** / **Start a new chat to send a message.**
  - Unread: **No Unread Chats** / **You’re all caught up.**
  - Archived: **No Archived Chats** / **Chats you archive will appear here.**
  - Left: **No Left Chats** / **Chats you leave or are removed from will appear here.**

## Authoritative actions

- Opening a chat clears its unread count and manual unread marker.
- **Read All** clears unread state for every non-archived chat, including rows outside the current search result.
- Pinning moves a chat above unpinned rows while retaining stable fixture order within each partition. Unpinning restores its fixture-relative position.
- Active, non-archived rows expose Read/Unread, Pin/Unpin, Mute/Unmute, Archive, and Leave for groups.
- Archived rows expose Read/Unread, Mute/Unmute, and Unarchive.
- Ended rows expose Read/Unread, Mute/Unmute, Archive, and Delete.
- Mute durations are 1 Hour, 8 Hours, 1 Day, 1 Week, and Always.
- A sole administrator cannot leave a group until another administrator exists.
- Ended membership copy is normalized to **You left this chat.**, **You left this group.**, or **You were removed from this group.**

## Creation flows

- New Message searches people by name or npub, places **New Group** first, and excludes the active profile and White Noise Support.
- Selecting a person opens their profile. **Message** opens the existing direct chat for that person or creates exactly one new direct chat.
- New Group supports searchable multi-selection, stable selection order, removable selections, and enables **Continue** after at least one selection.
- Set Up Group accepts an optional photo through the same Photos, Files, Web, and removal model used by onboarding, requires a trimmed group name, accepts an optional description, and reviews selected members.
- Photo Picker and Files use their default system-owned appearance.
- Creating a group makes the active profile an administrator, selected people members, records **You created the group.**, copies available relay URLs, and opens the new conversation.
- Back and cancel do not mutate profile or chat state.

## Acceptance gates

- Unit tests prove fixture counts/order, projections, diacritic search, authoritative Read All, stable pin behavior, direct deduplication, group validation/membership, relay copying, and profile isolation.
- Compose tests compile for avatar-to-Settings navigation, on-demand Chats
  search, scope-menu selection, absence of persistent Read All chrome,
  creation search-empty treatment, required member/name validation, photo
  sources, relay recovery, and large-text RTL reachability. Device execution
  and visual inspection remain explicitly deferred until requested.
- `clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` succeeds with no lint errors.

## Current official Android sources

- [Search bar](https://developer.android.com/develop/ui/compose/components/search-bar)
- [Lazy lists](https://developer.android.com/develop/ui/compose/lists)
- [Material chips](https://developer.android.com/develop/ui/compose/components/chip)
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Adaptive apps](https://developer.android.com/develop/adaptive-apps)
