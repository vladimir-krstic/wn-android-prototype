# Chats and chat creation

Status: Implemented; static verification complete on 2026-08-15

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

- Use a Material 3 top app bar titled **Chats**. The iOS titleless toolbar is not a parity requirement.
- Enter search from a top-app-bar action and expose a normal Android back/cancel affordance while search is active.
- Expose Chats, Unread, Archived, and Left through a Material menu. The active scope is always announced in the UI and to accessibility services.
- Use a `LazyColumn` for rows and a floating action button for **New Message**.
- Use a row overflow action and a Material modal bottom sheet for the complete action set. This replaces UIKit multi-action swipe geometry while preserving every operation with discoverable, keyboard-accessible targets.
- Use Material alert dialogs for destructive and constrained operations.
- Use full-screen destinations for New Message, person profile, member selection, group setup, and the temporary conversation entry screen. Successful direct or group creation clears the creation flow before opening the conversation.
- Keep the compact layout phone-first. Expanded-width list/detail behavior is deferred until the conversation surface exists in Batch 3; content still respects the repository adaptive width boundary.

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
- Compose navigation tests compile for the principal entry points. Device execution and visual inspection remain explicitly deferred until requested.
- `clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` succeeds with no lint errors.
