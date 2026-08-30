# Chats and chat creation

Status: Chats controls and shared Material-sheet follow-up implemented on
2026-08-26; New Message, Person Profile, New Group, and Set Up Group hierarchy
refined on 2026-08-30. Static verification is recorded in the parity ledger;
device/visual acceptance remains pending.

## Approved controls follow-up

- Title metadata refinement: mute, disappearing-message and ended-membership
  symbols belong directly after the chat name, not beside the timestamp.
  The text layout reserves the measured timestamp and icon widths first;
  the single-line name uses ellipsis only when that remaining space is full.
  Short names keep their symbols adjacent. Preserve full-name accessibility,
  RTL placement and font-scaled row height.
- Invitations use the same filled, font-scaled circular footprint and trailing
  position as single-digit unread badges, with the official Material Add
  symbol in `onPrimary`. Status precedence and underlying unread state do not
  change. The supplied iOS images specify these relationships, not Android
  pixel dimensions or new fixture content.
- Replace swipe reveals and the chat-actions sheet with a highlighted row and
  anchored Material `DropdownMenu`, retaining action eligibility, stable order,
  TalkBack actions, Undo, confirmations, and sole-admin protection. Only the
  active profile/chat identity is retained; actions resolve current state.
- Use one exclusive trailing status: invitation, visible failure, unread count,
  manual unread, or none. Manual unread, invitation and failure match the native
  numbered badge footprint, including font scaling; hidden unread state is retained.
- Restore an icon-only native Material FAB in the active content pane, hide it
  during search, and preserve Check Relays recovery. Scrolling content, not the
  viewport, reserves clearance for the FAB and navigation area.
- Both mute entry points use a shared immediate-selection Material dialog.
- All ordinary app sheets share `surfaceContainer`, transparent ordinary
  rows, and a `titleLarge` header with 24 dp text margins and 8 dp body gap.
  Material owns the handle slot; no extra top spacer or duplicate insets.
  Task app bars, intentional tonal groups, and specialized media stay intact.
- Behavioral reference only: [Signal Android @441ba42](https://github.com/signalapp/Signal-Android/blob/441ba42c3f3175476a1f54eba8e72d8d6d304db7/app/src/main/java/org/thoughtcrime/securesms/conversationlist/ConversationListFragment.java#L1373-L1465).
  No Signal source, extra commands, selection model, or dependencies are copied.
  This supersedes the earlier swipe/toolbar-creation adaptation.

## Source evidence

The 2026-08-26 Chats-only delta is authorized against `wn-ios-prototype@4c25393`:
`Screens/Chats/ChatsView.swift`, `NativeChatList.swift`, `ChatListRow.swift`,
and `ChatListItem.swift` under `WhiteNoisePrototype/`. This does not refresh
the global baseline or import its changed timelines/timestamps.

- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Chats/ChatsView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Chats/NativeChatList.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Chats/ChatListItem.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Chats/ChatListFixtures.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Chats/NewChatView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatModels.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatFixtures.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatOperations.swift`
- The corresponding iOS briefs indexed by `docs/port/source-map.md`.

## Android-native adaptation

- Use a pinned Material 3 top app bar with the active-profile avatar, Filter,
  and Search. The default Chats title is empty; Unread, Archived, and Left
  remain visible titles. New Message is an icon-only native FAB; its slot
  becomes Check Relays when creation is unavailable and hides during search.
- The avatar opens Settings; profile switching and profile addition live in
  the Settings profile header rather than in a Chats-owned switcher.
- Enter search from the top-app-bar search action and expose a normal Android
  back/cancel affordance while search is active. The active top-app-bar field
  is a compact 48 dp contextual treatment with `bodyLarge` query text, not the
  full 56 dp standalone SearchBar or inherited app-bar title typography. Back
  and Clear keep native 48 dp touch targets. Material's decoration layout uses
  zero vertical content inset at default scale so placeholder, entered text,
  cursor and icons are centered and fully visible; the field may grow for
  larger font scales rather than clipping a line into the compact height.
- Expose Chats, Unread, Archived, and Left through a Material dropdown menu
  opened by the adjacent filter action. Non-default scopes use a native filled
  monochrome icon button with selected semantics and a named current scope.
- Do not show **Read All** as a persistent top-app-bar action. Preserve the
  authoritative model operation for any bounded contextual treatment selected
  later; visual polish does not silently remove product behavior.
- Use a `LazyColumn` for rows and a native `FloatingActionButton` in Scaffold's
  FAB slot, inset to the same centered content pane.
  The list sits directly on the ordinary screen surface: no enclosing rounded
  container and no individual card around each normal chat row.
- Rows use Material `ListItem`, semibold names, two-line previews, name-aligned
  timestamps and preview-aligned status. Larger fonts grow the row. A small
  avatar pin badge, inline mute/timer/membership symbols, primary-role unread
  badges, and semantic-error delivery symbol replace verbose status lines.
  The single preview-aligned status is invitation, visible failure, count,
  manual unread, or none; failure never clears the underlying unread state.
- Long press highlights the row with `surfaceContainerHigh` and opens an
  anchored Material `DropdownMenu`. Native focus, positioning, scrolling,
  dismissal and RTL are retained; the list never handles horizontal swipes.
  TalkBack exposes the same custom actions. No ellipsis or chat-actions sheet.
- The clickable Material list-item overload provides its native 12 dp
  avatar-to-text gap (previously 16 dp). Keep an 8 dp outer inset and 8 dp
  inner horizontal padding in every state, preserving the 16 dp avatar edge
  without shifting content on long press. Hold Material's selected shape
  while the menu is open. An 8 dp transparent vertical popup gutter separates
  the menu surface from the highlighted row on either side; native popup
  fitting, RTL placement, and constrained-window fallback remain unchanged.
- Scroll-crash repair: retain the native clickable `ListItem`, but measure
  title, metadata, timestamp, preview and status together in its content slot.
  The captured Pixel 8a crashes report `LayoutNode … not found in RectList`
  during the supporting-slot inherited-baseline query, including lazy
  premeasurement. A small single-pass text layout reads baselines only from
  the actual `Text` children; it never queries a `Row`/`Box` baseline or places
  children during measurement. Preserve the pinned component's 72/88 dp
  two-/three-line minimum heights, native vertical padding/alignment, all
  existing text relationships and larger-text growth. No dependency change,
  exception suppression, prefetch disabling, or replacement of native menus,
  gestures, shape, ripple or accessibility behavior.
- The toolbar's 40 dp avatar and list's 52 dp avatars share a visible leading
  edge at 16 dp inside the same maximum-680 dp pane. Native Back and
  conversation-title avatars retain their own component positions.
- Ordinary headers use `surface` at rest and `surfaceContainer` under scroll.
  The list viewport extends to the bottom edge; navigation clearance belongs
  to scrolling content padding so the last row stays reachable.
- Use Material alert dialogs for destructive and constrained operations.
- Use full-screen destinations for New Message, person profile, member selection, group setup, and the temporary conversation entry screen. Successful direct or group creation clears the creation flow before opening the conversation.
- Keep the compact layout phone-first. Expanded-width list/detail behavior is deferred until the conversation surface exists in Batch 3; content still respects the repository adaptive width boundary.
- New Message and New Group keep search visible because finding people is the
  primary task on both destinations. New Message uses Material's standard
  56 dp standalone filled search input because it is the primary control in
  the page body; New Group retains the rounded 48 dp-minimum compact field
  shared with contextual Chats search. Both use `bodyLarge`, a leading search
  symbol, on-demand clear action, Search IME action, font-scale growth and the
  shared no-results composition. Both routes retain the Settings
  `surfaceContainerLow` canvas. New Message and New Group each present their
  lazy people directory as one iOS-parity white-equivalent grouped section
  with rounded outer rows and the app's 2 dp canvas-tone separators. New
  Group's compact search uses that same white-equivalent container role while
  retaining its 48 dp minimum and full-rounded shape. Set Up Group presents
  its read-only members as the same inset white-equivalent segmented group,
  including 2 dp canvas-tone separators. All creation directories use Material's current interactive
  `ListItem`, whose 12 dp avatar-to-text relationship matches Chats and is
  tighter than the deprecated 16 dp overload. New Message's lazy viewport
  reaches the physical bottom edge while navigation clearance stays inside
  its scrolling content. New Group remains an ordinary white-equivalent
  icon-led disclosure rather than a screen-local feature card.
- New Group shows stable selected order in a horizontal strip of 64 dp avatars
  with visible filled remove badges and one whole-tile Remove action. The
  searchable person directory remains toggleable with selected semantics and
  a vector check. Selecting a person does not morph, round, resize, or recolor
  that row: its first, middle, last, or sole grouped shape remains unchanged
  and the check is the only added visual indicator. This replaces the earlier
  `InputChip` adaptation: the selected identities are the information, not
  filter criteria.
- Person Profile reuses the accepted Share & Connect identity proportions,
  verified seal, complete public-key value, native middle ellipsis, copy/check
  capsule and two-second semantic copy feedback. The avatar-to-name and
  name-to-About relationships are both 16 dp so the centered identity reads as
  one balanced stack. About is a centered italic `bodyLarge` message in
  `onSurfaceVariant` on a `surfaceContainerHigh` tonal group. When About is
  present, address and npub follow it as in the pinned iOS hierarchy instead
  of being duplicated in the header.
- Profile actions are real grouped Material button rows with official symbols:
  Groups in Common opens a navigable group list with overlapping previews;
  an eligible admin can add the person through a standard modal bottom sheet
  and confirmation; Add/Remove Contact mutates immediately; Block confirms and
  Unblock does not. Group-admin actions remain a separate labeled group. The
  role appears in the route title rather than a duplicate identity badge.
- Message/relay recovery, Continue, and Create Group remain full-width shared
  56 dp task buttons on a continuous gray bottom slot. That slot uses
  `surfaceContainerLow` at rest and the same `surfaceContainer` scrolled color
  as the pinned settings header once content moves beneath it. Set Up Group uses the
  same gray canvas, the shared 120 dp profile-avatar rendering, and the same
  native tonal Add/Change Photo action as Sign Up and Profile. The fields begin
  without a redundant Group Details heading; Members keeps its useful heading
  and one quiet white-equivalent grouped list rather than independent cards.
  The empty shared avatar uses the existing group symbol instead of a question
  mark. When Chat
  Message relays are unavailable, Message becomes **Check Chat Relays** and
  opens the established recovery destination.
- Set Up Group uses the shared fully rounded tonal fields with label, input,
  icon, and supporting copy on one 16 dp content line, plus the established
  profile photo action, visible preparation/error states, grouped read-only
  member rows with 2 dp separators, and a bounded 56 dp pinned Create action. The avatar
  becomes the name monogram or chosen photo as input changes. Photo Picker and
  Files remain completely system-owned.

## Deterministic data contract

- Marmota starts with exactly 77 chats: 72 non-archived and 5 archived.
- Exactly one chat is initially pinned.
- The first ten rows, in order, are the five direct catalog scenarios followed by Composer Text, Multiline, Link, Link Preview, and Photo.
- The five archived IDs are `catalog-direct-archived`, `road-trip`, `garden-club`, `old-studio`, and `design-notes`.
- Add-profile identities have the deterministic people directory and no chats.
- Available Chat Message relay URLs are copied into newly created direct and group chats. The prototype never performs network work.

## Scope and search behavior

- Chats contains all 72 non-archived chats, including left/removed membership.
- Unread contains every unread or manually marked-unread non-archived chat,
  including left/removed membership.
- Archived contains archived chats.
- Left contains non-archived chats whose membership is left or removed.
- Search applies inside the selected scope across title and the same
  `ChatListPresentation` used for display. Membership overrides stale drafts;
  drafts override the previous preview; sender and attachment text are kept.
  Matching is case- and diacritic-insensitive.
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
- Menu order: Read if unread, otherwise Unread only when non-archived;
  Pin/Unpin follows only when non-archived, including ended rows.
- Then Mute/Unmute for active
  non-archived rows; Archive/Unarchive; Leave for active groups or Delete for
  ended membership. Read archived rows start with Unarchive.
- Store only the menu's profile/chat IDs, resolve eligible actions from current
  state, and dismiss before dispatching. Order does not reverse when Material
  positions the menu above its anchor. No system-edge gesture exclusion.
- Only one menu is open. Back/outside tap closes it. Navigation, scope/profile
  changes, another menu, and removal/disposal of its anchor close it.
- Snackbar Undo restores only the affected read-state or archive/pin fields
  on the captured profile/chat. It does not replace newer drafts, media,
  membership, or other chat fields. A subsequent action replaces the Snackbar.
- Mute durations are 1 Hour, 8 Hours, 1 Day, 1 Week, and Always, in the shared
  `MuteDurationDialog` also used by Chat Info. The options form one accessible
  radio group; its first control begins on the dialog title/content line with
  no nested list inset. Each complete 56 dp row keeps its control on that line
  while the rounded interaction surface expands 16 dp outward on both sides,
  leaving an 8 dp gutter against the dialog edge. The current duration is
  visibly and semantically selected whenever one exists. Selection applies
  immediately; Cancel, Back and outside dismissal change nothing.
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

- `ChatListPresentationTest` covers previews/search, membership, exclusive
  status precedence, preserved unread state, eligible action order and Undo.
- `ChatsPolishTest` covers toolbar/filter semantics, icon-only/recovery FAB,
  search hiding, menu ordering/viewport edges/Back/outside dismissal, removed
  anchors/profile changes, long press/accessibility, Undo, destructive guards,
  status sizing, avatar alignment, compact/expanded panes, RTL, large fonts,
  no horizontal actions, and last-row clearance above the FAB.
- `ChatListRowTest` adds title-adjacent icon order/gaps, timestamp reservation
  and baseline alignment, one-line ellipsis with full-name semantics at
  320/680 dp, 100/200% font scales, and LTR/light plus RTL/dark layouts.
  Invitation color/plus checks cover both themes and font scales;
  `ChatsPolishTest` compares its geometry with count/manual/error indicators.
- `ChatListRowTest` also checks the native 12 dp avatar gap, 8 dp inset rounded
  highlight without content movement, retained Button semantics, and an 8 dp
  menu gap above/below the anchor. Coverage includes compact/expanded panes,
  LTR/light and 200% RTL/dark layouts; UI execution remains separately requested.
- `ChatListScrollRegressionTest` exercises repeated native flings and index
  jumps through real fixtures, retained menu/row interaction and unchanged
  chat state. A `SubcomposeLayoutState` harness explicitly reuses, premeasures,
  promotes and cancels row slots, including inline attachments and all status
  types, at compact/expanded widths and 100% LTR/light / 200% RTL/dark text.
  Single-/multiline minima and preview/status alignment have explicit checks.
  These regressions require separate device execution; compilation alone does
  not confirm the reported runtime crash is resolved.
- `MaterialSheetTest`, `ChatInfoScreenTest`, and `DiagnosticsPromptTest` add
  common surface/header, shared mute, font/RTL, and dismissal-lifecycle cases.
- `HeaderScrollTest` covers initial/programmatic scroll, return to top, and
  empty-list tonal reset. UI tests are compiled, not device-executed.

- Unit tests prove fixture counts/order, projections, diacritic search, authoritative Read All, stable pin behavior, direct deduplication, group validation/membership, relay copying, and profile isolation.
- Compose tests compile for avatar-to-Settings navigation, on-demand Chats
  search, scope-menu selection, absence of persistent Read All chrome,
  creation search-empty treatment, required member/name validation, photo
  sources, relay recovery, and large-text RTL reachability. Device execution
  and visual inspection remain explicitly deferred until requested.
- `clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` succeeds with no lint errors.

## Current official Android sources

2026-08-26 menu follow-up: Chats context actions, scope selection and Set Up
Group photo sources now use the shared official Expressive popup/group/item
family. Scope selection uses native radio-item semantics; command order,
current-state eligibility, dismissal, Undo and guards are unchanged. The
compatibility pin and compiled regression evidence are in `app-menus.md`.

- [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Material menus](https://developer.android.com/develop/ui/compose/components/menu)
- [Pinned native ListItem, Menu and token source](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.5.0-alpha25/material3-android-1.5.0-alpha25-sources.jar)
- [Material FAB](https://developer.android.com/develop/ui/compose/components/fab)
- [Material icons](https://developer.android.com/develop/ui/compose/graphics/images/material)
- [Material badges](https://developer.android.com/develop/ui/compose/components/badges)
- [Compose text layout and ellipsis](https://developer.android.com/develop/ui/compose/text/configure-layout)
- [AndroidX Row weight and baseline behavior](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Row.kt)
- [Compose single-pass custom layouts](https://developer.android.com/develop/ui/compose/layouts/custom)
- [Compose alignment lines](https://developer.android.com/develop/ui/compose/layouts/alignment-lines)
- [Material dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
- [Material insets](https://developer.android.com/develop/ui/compose/system/material-insets)

- [Search bar](https://developer.android.com/develop/ui/compose/components/search-bar)
- [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Material buttons](https://developer.android.com/develop/ui/compose/components/button)
- [Android touch-target guidance](https://support.google.com/accessibility/android/answer/7101858)
- [Lazy lists](https://developer.android.com/develop/ui/compose/lists)
- [Material chips](https://developer.android.com/develop/ui/compose/components/chip)
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Adaptive apps](https://developer.android.com/develop/adaptive-apps)
