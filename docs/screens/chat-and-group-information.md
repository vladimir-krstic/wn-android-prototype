# Chat and group information

2026-09-04: B03 extends discovery, private contact details, created-chat opening
recovery and profile-to-group actions. See [people-discovery-and-private-details.md](people-discovery-and-private-details.md).
The existing identity/header/group composition remains authoritative; the older
single-group picker is replaced by checked multiple selection and confirmation.

Status: Info-screen redesign passed the host gate on 2026-09-03: 186 unit
tests, lint without errors, app assembly, and instrumentation-test APK
compilation. Current-build visual inspection and user acceptance remain
pending. The earlier Chat Info/Search device evidence predates this redesign.

## Source evidence

- pinned `chat-info.md`, `group-info.md`, `person-profile.md`,
  `conversation-search.md`, and `disappearing-message-indicators.md`
- `ChatInfoView.swift` and authoritative iOS chat-model tests
- user-approved current-iOS comparison at `wn-ios-prototype@4c25393f0eb6` for
  Shared Content/media-viewer capability, plus the explicitly requested
  2026-09-03 Chat/Group Info hierarchy comparison in
  `WhiteNoisePrototype/Screens/Conversation/ChatInfoView.swift`; these scoped
  comparisons do not repin the Android baseline

## 2026-09-03 info-screen refinement

- Use the supplied Google Messages reference for presentation: a neutral
  `surfaceContainerLow` canvas, back-only Material app bar, centered identity,
  equal-width tonal pill actions with captions, and white-equivalent
  `surfaceContainerLowest` segmented rows. Keep the accessible Chat Info or
  Group Info pane name and standard system Back behavior.
- Reuse Person Profile's 32%-of-pane avatar clamp (104–152 dp), semibold
  `headlineSmall` name, verified address, and full-value copyable public-key
  capsule. Group identity uses the same avatar/name scale, then description,
  member count, and any membership status.
- Direct order: identity; About, Mute/Unmute, Disappearing, Search; Shared in
  Chat (Photos & Videos, Links, Documents); Chat Actions (Relays, Developer
  Tools, Archive/Unarchive, active-only Leave Chat).
- Group order: identity; Mute/Unmute, Disappearing, Search; Shared in Chat;
  Advanced (Relays, Developer Tools); Members in model order; a separate
  active-admin-only Edit Group/Add People group; Archive/Unarchive and
  active-only Leave Group. Developer Tools opens the existing per-chat debug
  destination and preserves its profile-owned enablement gate.
- Rows use the current interactive Material ListItem API, positional rounded
  shapes, native measurement/state layers, 16 dp outer margins and 2 dp gaps.
  Headings follow the 32 dp content line; independent groups remain 24 dp
  apart. Member avatars stay 48 dp. Quick controls fill their equal-width
  slot at the established 56 dp height; captions wrap with font scaling.
- Preserve every existing state transition, confirmation, admin/sole-admin
  rule, route, clipboard behavior, and shared-content count. Expanded layouts
  retain AdaptiveContent; long names/descriptions wrap and rows grow with text.
  This pass does not add Google's reference-specific chat settings.
- Acceptance: host build/lint/unit tests and compiled UI coverage for copy,
  action navigation, section order, member/admin states, and enlarged text.
  Current-build device inspection and user visual acceptance remain separate.

## Android-native adaptation

- The conversation title/avatar opens one typed Chat Info route whose lazy
  Material list adapts to direct or group identity and current membership.
- Equal Material quick actions own About/member count context, mute duration,
  disappearing duration, and in-place Search return. Shared content uses three
  ordinary disclosure rows rather than tabs.
- Focused Photos & Videos uses an adaptive three-column frame grid and the same
  chat-wide Compose media pager as message bubbles. Links and Documents use
  lazy Material lists and local/system handoffs.
- Group admins receive explicit Edit Group, Add People, and role-aware member
  actions. Photo Picker, text fields, centered confirmation dialogs, and typed
  timeline events own every mutation. Ordinary members remain read-only.
- Edit Group uses Photo Picker with its default platform or OEM appearance.
- Chat Relays is an independent typed route. It normalizes/deduplicates `wss://`
  endpoints, confirms removal, permits an empty set, and restores the chat's
  captured defaults without changing profile or sibling chat relays.
- Archive, mute, disappearing, leave, role, member, metadata, relationship,
  and relay changes mutate only the active profile's authoritative chat graph.

## Visual-polish translation

- Direct and group identity remain uncontained and centered, with selective
  scale for the avatar and title. Compact Verified Nostr Address, public-key,
  description, member-count, and ended-membership treatment use semantic
  supporting roles instead of an enclosing hero card.
- About, Mute/Unmute, Disappearing, and Search use equal-width, 56dp-tall filled-tonal icon
  controls with visible captions and one concise accessibility name. Group
  Info retains the same system without reserving an empty About position.
  Search is retained as a working root quick action for both direct and group
  information; it replaces the Chat Info route with the same conversation and
  opens the shared focused in-place search without mutating the draft.
- Shared content, members, administration, and chat actions use
  `surfaceContainerLowest` segmented Material list items, standard
  symbols, and disclosure chevrons only for routes. Archive and destructive
  leave actions no longer pretend to navigate.
- Disappearing-message selection uses a radio-button group; Add People uses
  whole-row checkbox semantics and tonal selected state. Member profiles
  separate relationship actions from role/removal actions, and the existing
  model still gates all administration and sole-admin consequences.
- Shared-content destinations use one open three-column media grid or ordinary
  rich-content lists. The media grid and conversation consume the same
  chronological message → attachment → image projection, so every album tile
  opens its exact frame in the complete chat-wide pager. The viewer carries
  sender/time/source metadata and shares the same Share, Forward, Save, zoom,
  and Go to Message behavior. Go to Message replaces the information stack
  with the typed conversation route targeted to the source message. Empty
  content retains a complete title/detail state.
- Edit Group uses the shared fully rounded tonal fields with label-above
  content alignment and focus/error rings, a 120dp identity preview,
  Photo Picker-owned selection, explicit photo
  preparation/error feedback, and one pinned 56dp Save action.
- Chat Relays groups the independent endpoints, uses named remove icon actions,
  explains empty history-preserving recovery, distinguishes Restore Defaults,
  and keeps final-relay removal behind the accepted consequence dialog.

## Current official Android guidance

- Material top app bars and Scaffold own destination navigation and content
  structure; lists and lazy grids retain stable collection identity.
- Chats and Chat Info share `MuteDurationDialog`, a native AlertDialog with
  aligned, rounded whole-row radio choices, visible current-duration state,
  immediate duration selection and safe Cancel/Back/outside dismissal. Its
  state layer expands toward the dialog edges with an 8 dp outer gutter while
  16 dp internal padding keeps the radio content on the title line. Timer
  choices use the shared sheet surface/header with scrollable radio rows.
  AlertDialog also owns focused
  destructive consequences, and native radio/checkbox controls expose current
  selection without glyph-only state.
- Android Photo Picker remains permissionless and system-owned. AdaptiveContent
  constrains expanded layouts while compact screens retain the shared 16dp
  content margin and 8/24dp relationship rhythm.
- Sources rechecked for this pass: [app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
  [lists and grids](https://developer.android.com/develop/ui/compose/lists),
  [icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button),
  [bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
  [dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
  [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility),
  [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker),
  and [canonical adaptive layouts](https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts).

## Acceptance gates

- Unit tests cover shared-content projection, relay normalization/isolation,
  group edits/members/roles/events, last-admin rules, groups in common, timer
  changes, and recovery after final-relay removal.
- Compose route tests for direct/group info, shared categories, admin/member
  states, selected duration treatment, role-aware member profiles, group
  editing, add people, the working Search quick action, and normal/empty chat
  relays compile.
- The 2026-09-03 compiled regressions cover full public-key copying, the
  Developer Tools callback, complete group section order, self/member
  navigation semantics, and ended-group action gating with dark 200% RTL text.
- Clean build, lint, tests, APKs, and permission/export audits pass.
- The unified media projection has unit coverage for frame order, stable keys,
  exact initial selection, album order, metadata, and filtering. Compiled
  Shared Content coverage opens an exact frame and exercises Go to Message;
  the updated grid/viewer states have not yet received a device visual pass.
