# Chats — Populated

## Purpose and navigation

Show a deterministic populated Chats destination after either onboarding path.
The profile avatar opens Settings, New Message starts direct/group creation,
and every row opens the shared direct/group conversation architecture. Rows
support native swipe actions.

## Copy

- Search prompt: **Search Chats**
- Filter choices: **Chats**, **Unread**, **Archived**, **Left**
- Selected Filter labels: **Unread**, **Archived**, **Left**
- Unread bulk action: **Read All**
- Leading swipe actions: **Read**, **Unread**, **Pin**, **Unpin**
- Trailing swipe actions: **Mute**, **Unmute**, **Archive**, **Unarchive**, **Leave**, **Delete**
- Mute dialog title: **Mute Notifications**
- Mute choices: **1 Hour**, **8 Hours**, **1 Day**, **1 Week**, **Always**
- Leave confirmation title: **Leave “Chat Name”?**
- Leave confirmation message: **You’ll stop receiving new messages. This chat will remain on this device as read-only history until you delete it.**
- Leave confirmation action: **Leave Chat**
- Delete confirmation title: **Delete “Chat Name” from this device?**
- Delete confirmation message: **This permanently removes the chat and its messages from this device. Signing in again won’t restore them.**
- Delete confirmation action: **Delete Chat**
- Ended-membership previews use the correct noun: direct chats say **You left
  this chat.**; groups say **You left this group.** or **You were removed from
  this group.**
- Empty and no-results copy remains defined by `chats-empty.md`.
- Fixture names and messages are fictional and stable.

## Native components

- A public UIKit list built with `UICollectionLayoutListConfiguration` owns scrolling, safe-area behavior, row placement, swipe geometry, system motion, dimming, and the system background. Its visible scroll indicator and separators are hidden.
- The collection view extends beneath the stationary top toolbar while UIKit’s automatic safe-area inset keeps resting content unobscured. At the exact resting top offset the scroll-edge effect is hidden. Once content moves beneath the controls, UIKit’s native hard top scroll-edge effect becomes visible and creates the more opaque, clearly defined toolbar boundary. Returning fully to the top hides it again. The system owns its material and boundary; the app reads the native adjusted content offset and adds no custom gradient, blur, separator, animation, or fixed top padding.
- The collection view extends its system background beneath the device’s bottom safe area, matching native full-screen lists. UIKit continues to adjust scroll content so the final row remains reachable above the Home indicator; no separate bottom strip is reserved.
- `UIHostingConfiguration` hosts the existing SwiftUI conversation-row composition inside each native list cell.
- Each conversation row is composed from native `Image`, `Text`, `Circle`, and SF Symbols because SwiftUI does not provide a stock Messages conversation row.
- The approved row avatar is a 56-point circle containing either a bundled photorealistic fictional portrait or a one-letter native monogram. SwiftUI has no stock avatar control or avatar size variants; 56 points is the user-approved custom list metric.
- A pinned chat overlays `pin.fill` on the avatar’s bottom-trailing edge. The compact badge uses caption-two typography, four-point padding, an opaque semantic system-background circle, a thin separator outline, and a slight outward offset. It uses no accent color, translucency, shadow, label, pinned section, title indicator, or trailing timestamp indicator.
- Titles use semantic headline typography. Previews use semantic subheadline typography and may occupy two lines.
- An enabled disappearing-message timer adds the secondary `timer` SF Symbol
  beside the title. It follows the existing mute symbol when both states apply
  and does not show its duration in the compact list row. The complete shared
  state and accessibility behavior is defined by
  [Disappearing-message indicators](disappearing-message-indicators.md).
- Every preview uses the same regular secondary style, regardless of unread state.
- Timestamps use semantic caption typography in the title row. The deterministic fixtures cover today’s time, Yesterday, weekday, and calendar-date variants in chronological order.
- Row separators and disclosure chevrons are hidden to keep the dense status list visually quiet.
- Public `UIContextualAction` and `UISwipeActionsConfiguration` own the swipe actions. Their completion handler is deliberately deferred while Mute, Leave, or Delete is presented, which keeps the row revealed beneath the system dimming treatment.
- While a row is swiped, public `UICellConfigurationState.isSwiped` applies the adaptive `secondarySystemFill` background as a capsule derived from the current cell height. Contextual actions show system SF Symbols without visible text so UIKit centers each action in the row; their action and image accessibility labels retain the command names.
- Native `UIAlertController` action sheets own the Signal-derived mute duration picker and the destructive Leave and Delete confirmations. The sheet is anchored to the originating contextual action with no popover arrow; no custom modal, pointer, dimming layer, or transition is drawn by the app.
- A native bottom-bar `Button` presents **Read All** at the leading edge only while the Unread scope contains unread chats. Outside that state, the bottom toolbar modifier is not installed at all, so Chats, Archived, and the completed Unread empty state cannot reserve an invisible toolbar strip. The system toolbar owns the button’s Liquid Glass capsule, spacing, safe-area placement, hit target, and feedback.
- The existing native search, menu, and Picker remain unchanged. Filter, Search, and New Message share one system Liquid Glass toolbar group; New Message uses the default toolbar treatment rather than a separate prominent tint.
- The navigation toolbar remains titleless. The Filter Menu is icon-only in Chats and uses the compact adaptive symbol-plus-text selected label defined in `chats-empty.md` for Unread, Archived, and Left. No separate list header or dismissible chip is introduced.
- The leading profile avatar is a native `NavigationLink` to Settings, allowing SwiftUI’s navigation stack to own both the Back button and interactive swipe-back lifecycle. Marmota uses the bundled user-supplied photo, while profiles without an image fall back to their initial. Its avatar-specific button style returns the system link label without transient dimming so the artwork remains full contrast throughout the transition.
- When the active profile has an unavailable relay role, the New Message slot in the
  existing trailing toolbar group becomes a `NavigationLink` using the outlined
  orange `exclamationmark.triangle` SF Symbol. The system retains the same
  Liquid Glass group; pressing it pushes Relays and the native Back action
  returns to the same Chats scope and state. A role is unavailable while it is
  unassigned, reconnecting, or disconnected; one connected assigned relay is
  sufficient coverage. No extra item or recovery banner changes list geometry.

## Deterministic data and behavior

- The populated profile has exactly 77 conversations: 72 nonarchived and 5
  archived. Thirty-eight records form the ordered developer catalog, including
  one archived case. White Noise Support follows the catalog; retained story
  chats follow it.
- Chats contains nonarchived conversations, including retained read-only history after a person leaves or is removed. Archived conversations appear only in the separate Archived scope.
- Unread contains nonarchived conversations with an unread count or a manual unread reminder.
- Archived contains archived conversations, including one archived conversation that retains unread state without appearing in Unread.
- Left contains nonarchived retained history after the person leaves or is removed. Archiving a retained chat moves it from Left to Archived.
- Search matches title and preview within the selected scope, ignoring case and diacritics.
- Swipe mutations update the same in-memory collection used by Chats, Unread, Archived, Left, and Search, so every filtered projection stays consistent.
- **Read All** clears every nonarchived unread count and manual unread marker, including matches outside an active search query. The Unread scope then resolves to its existing **No Unread Chats** state.
- Fixture order, names, and initial status values are stable. Fixture dates
  resolve once from an injectable launch-time reference. Each row derives its
  preview, failure state, and timestamp from the latest visible timeline
  message or typed group event; a draft temporarily supersedes that projection.
  Deleting the latest message reveals the preceding valid activity instead of
  leaving stale duplicated row data.
- Opening a conversation through any route clears its unread count and manual
  unread reminder in the authoritative profile-owned chat. The Chats and Unread
  projections reflect that change immediately on return.
- Calendar-date fixtures retain deterministic, plausible times of day instead
  of resolving to midnight. White Noise Support retains its stable Thursday
  fixture date, and Contact previews resolve the referenced person's name.
- Representative rows cover ordinary, unread count, muted, disappearing-only,
  muted-and-disappearing, group-disappearing, draft, failed, direct-chat,
  group-chat, and ten distinct attachment-preview treatments.
- **Direct - Text & Delivery** is the only initially pinned chat. The remaining
  catalog rows retain their documented order beneath it, followed by the
  unpinned retained story chats, including Maya Chen and Weekend Walks.
- Mina Park remains the deterministic draft example farther down the list and shows **Draft: Let’s pick this up after lunch** in the regular secondary preview style.
- Book Club starts in the voluntary-left state and shows **You left this
  group.** Quiet Studio Group remains the removed fixture and shows **You were
  removed from this group.** Both appear in Chats and Left as retained
  read-only history.
- Empty fixtures remain available for the future empty profile and for previews.
- Fiatjaf uses the approved Figma avatar, appears after the legacy marketing
  conversations, and opens its accepted story through
  the shared conversation defined in `conversation-fiatjaf.md` and
  `conversation-shared.md`.
- White Noise Support follows the developer catalog, uses the native
  `questionmark.bubble` identity, and opens the single profile-owned
  conversation defined in `conversation-support.md`.

## Initial developer viewport

The normal populated Marmota profile opens with the developer catalog. It is
not a separate test-only profile or scenario.

The first visible block is fixed:

1. **Direct - Text & Delivery** — pinned and failed — DLV-03: Failed outgoing message
2. **Direct - Dates & Scrolling** — DATE-15: Long day keeps its date pinned
3. **Direct - Replies & Deletion** — unread 3 — RPL-04: Missing reply target
4. **Direct - Reactions & Actions** — manually unread — ACT-05: Share available file URL
5. **Direct - New Chat & Draft** — draft — STATE-01: Unsent draft
6. **Composer - Text** — draft — Here’s the updated plan.
7. **Composer - Multiline** — draft — I pulled together the notes:
8. **Composer - Link** — draft — https://whitenoise.chat
9. **Composer - Link Preview** — draft — Worth a look: https://developer.apple.com
10. **Composer - Photo** — draft — Photo ready to send

The remaining developer cases continue in catalog order. White Noise Support,
the deterministic legacy marketing sequence, Fiatjaf, and the retained story
conversations remain reachable by scrolling and search.

## Swipe actions

- Leading swipe toggles Read and Unread and offers Pin or Unpin. Read clears both numeric unread counts and a manually marked unread state. Unread creates the approved empty 20-point monochrome badge with no number.
- Pin and Unpin apply immediately to nonarchived chats. Pinned chats sort above unpinned chats within Chats, Unread, Left, and active search results while preserving the deterministic relative order inside each group.
- Pin and Unpin first complete UIKit’s native contextual action, then observe the public `UICellConfigurationState.isSwiped` state on the display refresh cycle. Only after UIKit reports that no visible row remains swiped does the in-memory order change. The final sorted snapshot and pin-badge state are then swapped in atomically with UIKit’s reload-data snapshot API and animations disabled. The swipe closure provides the system-owned action feedback; the list doesn’t animate a row across unrelated conversations. No guessed delay, fade, custom duration, spring, transition, or manual row offset is applied.
- An active row in Chats or Unread exposes Mute/Unmute and Archive. Active
  group rows also expose Leave; direct chats never expose Leave.
- An inactive retained row in Chats or Left exposes Archive and Delete; it no longer exposes Mute, Unmute, or Leave.
- An active group row in Archived exposes Unarchive and Leave. Active direct
  rows expose only Unarchive. An inactive row exposes Unarchive and Delete;
  archived rows never expose Mute or Unmute.
- Muting opens **Mute Notifications** with Signal’s current duration set: 1 hour, 8 hours, 1 day, 1 week, and Always. Unmute applies immediately.
- Archive and Unarchive apply immediately and move the row between the native filtered projections. Existing mute state is preserved while archived and returns if the chat is restored.
- Leaving clears unread and mute state, replaces the preview with the matching
  **You left this chat.** or **You left this group.** copy, and shows
  `rectangle.portrait.and.arrow.right` beside the chat name.
- Attempting to leave from the chat list while the active profile is the sole
  admin leaves state untouched and presents **Can’t Leave Group** with **You’re
  the only admin in this group. Make another member an admin before you
  leave.** This matches Group Info's role protection.
- The fixed **Book Club** fixture starts in the voluntary-left state, appears
  in Left, and shows **You left this group.**
- The fixed **Quiet Studio Group** fixture starts in the removed state, appears
  in Left, and shows **You were removed from this group.** with the same
  ended-membership symbol.
- After membership ends, Delete replaces Leave and remains paired with Archive or Unarchive. Delete opens a destructive confirmation dialog, then removes the chat from every projection.
- Archived rows with unread content can be marked Read. Read archived rows cannot be manually marked Unread.
- Read/Unread remains the first leading action, so a full leading swipe performs only that reversible toggle. Pin/Unpin requires tapping its revealed action. Full swipe remains disabled for the multi-action trailing edge.
- Leave and Delete swipe buttons use the system red contextual-action tint without completing the swipe mutation before confirmation. Their confirmation actions use UIKit’s native destructive style.
- Mute, Leave, and Delete dialogs remain anchored to the action that opened them. The revealed swipe state stays visible under the system dimming treatment while the dialog is present; dismissing or cancelling does not require a custom reset animation.
- The revealed row retains the approved adaptive gray capsule, and icon-only contextual actions remain vertically centered against that same dynamic row height.
- The Unread bottom bar offers **Read All** without confirmation. It is fully hidden in Chats and Archived and disappears without retaining space after no unread chats remain.

## Status presentation

- Unread counts use an adaptive monochrome circle for one digit and a capsule for two digits or the capped **99+** value.
- A manually marked unread chat uses the same 20-point monochrome circle without a number.
- Muted uses `bell.slash.fill` immediately after the chat name.
- A left or removed chat uses `rectangle.portrait.and.arrow.right` in the same title-row status position.
- A pinned chat uses a compact `pin.fill` badge on the avatar edge. The row exposes **Pinned** as an accessibility value while the decorative badge itself is hidden from accessibility.
- Drafts use a visible **Draft:** preview prefix in the same secondary preview style.
- Failed sending uses the system-red outline `exclamationmark.circle`.
- Unread uses a 20-point status region while Failed retains an 18-point outlined SF Symbol. A single-digit unread count is independently centered over an exact 20-point circle without label padding. Multi-digit counts use a 20-point-high capsule with six-point symmetric horizontal padding and an independently centered label.
- Unread labels use tabular digits and a system-centered text frame. Counts greater than 99 display as **99+**.
- No Sent, mention, or sending state appears in the Chats list.

## Attachment previews

- Every supported attachment preview is present in the active Chats scope:
  Photo, multiple Photos, Video, Voice message, File, Contact, Link, and GIF.
- Each uses a baseline-aligned SF Symbol plus concise text in the same regular secondary `subheadline` style as an ordinary message preview.
- Photos, videos, files, and voice messages also exercise the shared composer.
  Contact and GIF remain deterministic showcase-only renderers. Sticker and
  shared-location messages are unsupported.

## Avatar provenance

Every person row and transcript member resolves to the documented bundled
Unsplash image pool. Group rows may use bundled group artwork or a monogram,
and White Noise Support uses its SF Symbol avatar. The images represent fixture
identities only; they are not White Noise users. The app performs no
runtime fetching.
The source links in this section preserve asset provenance only. The local
asset names, uses, transformations, and acceptance criteria are complete; an
agent does not need to open these sources to implement or evaluate the screen.

| Asset | Fixture identity | Photographer | Unsplash source |
| --- | --- | --- | --- |
| AvatarMayaChen | Maya Chen | Christina @ wocintechchat.com | [SJvDxw0azqw](https://unsplash.com/photos/SJvDxw0azqw) |
| AvatarEliasMoreno | Elias Moreno | Albert Dera | [ILip77SbmOE](https://unsplash.com/photos/ILip77SbmOE) |
| AvatarMinaPark | Mina Park | Jake Nackos | [IF9TK5Uy-KI](https://unsplash.com/photos/IF9TK5Uy-KI) |
| AvatarLeoMartins | Leo Martins | Jurica Koletić | [7YVZYZeITc8](https://unsplash.com/photos/7YVZYZeITc8) |
| AvatarNoraBennett | Nora Bennett | Good Faces | [xmSWVeGEnJw](https://unsplash.com/photos/xmSWVeGEnJw) |
| AvatarTheoGrant | Theo Grant | Joseph Gonzalez | [iFgRcqHznqg](https://unsplash.com/photos/iFgRcqHznqg) |
| AvatarAishaRahman | Aisha Rahman | Clay Elliot | [mpDV4xaFP8c](https://unsplash.com/photos/mpDV4xaFP8c) |
| AvatarLenaOrtiz | Lena Ortiz | Michael Dam | [mEZ3PoFGs_k](https://unsplash.com/photos/mEZ3PoFGs_k) |
| AvatarJonahReed | Jonah Reed | Ryan Hoffman | [Ft4p5E9HjTQ](https://unsplash.com/photos/Ft4p5E9HjTQ) |
| AvatarTessaMorgan | Tessa Morgan | Christina @ wocintechchat.com | [Zpzf7TLj_gA](https://unsplash.com/photos/Zpzf7TLj_gA) |
| AvatarMarcusBell | Marcus Bell | Alex Suprun | [ZHvM3XIOHoE](https://unsplash.com/photos/ZHvM3XIOHoE) |
| AvatarSofiaAlvarez | Sofia Alvarez | Štefan Štefančík | [QXevDflbl8A](https://unsplash.com/photos/QXevDflbl8A) |
| AvatarDanielKim | Daniel Kim | Ludovic Migneault | [EZ4TYgXPNWk](https://unsplash.com/photos/EZ4TYgXPNWk) |
| AvatarGardenClub | Garden Club member | alex starnes | [WYE2UhXsU1Y](https://unsplash.com/photos/WYE2UhXsU1Y) |

Within Chats, Fiatjaf and other legacy identities that previously used
non-Unsplash portraits resolve through the same deterministic Unsplash member
pool. The approved Fiatjaf artwork remains documented for its original bounded
story reference but is not used as a conversation-member portrait.

- Retrieval date: 2026-07-24
- Transformation: Unsplash CDN face crop to a 512-by-512 JPEG, then clipped into a circle at runtime
- Intended use: internal fictional prototype conversation avatars only
- License reference: [Unsplash License](https://unsplash.com/license)
- The direct-chat fixture mix is seven women and six men.

### Legacy marketing avatars

The ten legacy avatars below come from the approved [White Noise marketing Chat List node](https://www.figma.com/design/jzWaS92LwoBjqTtOLP6ij7/White-Noise---Web---Marketing?node-id=1404-5548). Nine are bundled source images and only clipped into the existing circular avatar at runtime. Marmots is the exact composed Avatar sublayer exported from [the approved gray treatment](https://www.figma.com/design/jzWaS92LwoBjqTtOLP6ij7/White-Noise---Web---Marketing?node-id=1404-5553); its gray image treatment, crop, circular mask, and border are baked into the PNG rather than recreated in SwiftUI.

| Asset | Fixture identity |
| --- | --- |
| LegacyAvatarHalFinney | Hal Finney |
| LegacyAvatarJudithMilhon | Judith “St. Jude” Milhon |
| LegacyAvatarMarmots | Marmots |
| LegacyAvatarRichardStallman | Richard Stallman |
| LegacyAvatarWhitfieldDiffie | Whitfield Diffie |
| LegacyAvatarEricHughes | Eric Hughes |
| LegacyAvatarNostrDevs | Nostr Devs |
| LegacyAvatarRadiaPerlman | Radia Perlman |
| LegacyAvatarDavidChaum | David Chaum |
| LegacyAvatarSatoshiNakamoto | Satoshi Nakamoto |

- Source nodes: `1404:5548`; Marmots composed export `1404:5553`
- Retrieval date: 2026-07-27
- Transformation: source imagery is downscaled to a maximum 512-pixel dimension for the prototype bundle without content edits; Marmots remains the exact Figma Avatar sublayer, downscaled only
- Intended use: public App Store Chats hero and related White Noise marketing
- Rights assumption: the user confirmed that the historical cast and supplied Figma artwork are cleared for public marketing use

## Accessibility

- Avatar initials and decorative artwork do not produce duplicate VoiceOver output.
- Each row combines its visible identity, preview, timestamp, and statuses into one readable element.
- Status meaning is not conveyed by color alone.
- Preview text can grow to two lines without a fixed row height.

## Apple developer references

- [Lists and tables](https://developer.apple.com/design/human-interface-guidelines/lists-and-tables)
- [UICollectionLayoutListConfiguration](https://developer.apple.com/documentation/uikit/uicollectionlayoutlistconfiguration)
- [Updating collection views using diffable data sources](https://developer.apple.com/documentation/uikit/updating-collection-views-using-diffable-data-sources)
- [UIScrollView topEdgeEffect](https://developer.apple.com/documentation/uikit/uiscrollview/topedgeeffect)
- [UIScrollEdgeEffect.Style.hard](https://developer.apple.com/documentation/uikit/uiscrolledgeeffect/style-swift.class/hard)
- [UIScrollEdgeEffect isHidden](https://developer.apple.com/documentation/uikit/uiscrolledgeeffect/ishidden)
- [UIScrollViewDelegate](https://developer.apple.com/documentation/uikit/uiscrollviewdelegate)
- [UIScrollView contentInsetAdjustmentBehavior](https://developer.apple.com/documentation/uikit/uiscrollview/contentinsetadjustmentbehavior-swift.property)
- [SwiftUI ignoresSafeArea](https://developer.apple.com/documentation/swiftui/view/ignoressafearea(_:edges:))
- [UIHostingConfiguration](https://developer.apple.com/documentation/swiftui/uihostingconfiguration)
- [UIContextualAction](https://developer.apple.com/documentation/uikit/uicontextualaction)
- [UISwipeActionsConfiguration](https://developer.apple.com/documentation/uikit/uiswipeactionsconfiguration)
- [UICellConfigurationState](https://developer.apple.com/documentation/uikit/uicellconfigurationstate)
- [UIAlertController](https://developer.apple.com/documentation/uikit/uialertcontroller)
- [Toolbars](https://developer.apple.com/documentation/swiftui/toolbars)
- [Action sheets](https://developer.apple.com/design/human-interface-guidelines/action-sheets)
- [badge(_:)](https://developer.apple.com/documentation/swiftui/view/badge(_:))
- [BadgeProminence](https://developer.apple.com/documentation/swiftui/badgeprominence)
- [SF Symbols](https://developer.apple.com/sf-symbols/)
- [Accessibility fundamentals](https://developer.apple.com/documentation/swiftui/accessibility-fundamentals)

Optional Apple product comparisons below preserve historical visual context.
They are not platform authority and are not required to implement this screen.

- [About iMessage](https://support.apple.com/guide/iphone/about-imessage-iph4e9799206/ios)
- [Use iMessage apps in Messages on iPhone](https://support.apple.com/guide/iphone/use-imessage-apps-iphf9c9c01d3/ios)

## Acceptance

- The populated account displays 72 nonarchived and 5 archived conversations.
- Unread, Archived, and Left show correct deterministic subsets and identify their scope inside the Filter Menu label; Chats uses the plain icon-only Filter label.
- Unread shows a native bottom-leading **Read All** action while unread chats remain; activating it clears every active unread state and reveals **No Unread Chats** without an empty bottom-bar strip.
- Search filters the selected scope and restores the existing native no-results state when empty.
- Read/Unread, Mute/Unmute, Archive/Unarchive, Leave, and Delete all mutate the visible deterministic state and remain correct in Chats, Unread, Archived, Left, and Search. Pin/Unpin remains available only for nonarchived chats.
- Pinning moves a nonarchived chat above unpinned chats without changing its selected scope; unpinning restores the fixture’s deterministic relative position.
- Pinning and unpinning close the system swipe presentation, then update directly to the final sorted state with no travelling row, duplicate row, temporary overlap, stacked text, or secondary transition.
- Pinned state is shown only by the avatar-edge badge; it adds nothing beside the timestamp, title, preview status, or toolbar.
- Mute presents all five durations; choosing one displays the mute symbol, and Unmute removes it.
- An enabled disappearing-message timer displays one timer symbol beside the
  title. The mute and timer symbols remain adjacent when both states apply, and
  turning the timer off removes only the timer symbol.
- Leave and Delete require explicit confirmation and Cancel leaves the chat unchanged.
- Opening Mute, Leave, or Delete keeps the originating swipe actions revealed and dimmed behind the native dialog.
- Leading and trailing swipes retain the rounded adaptive gray row container and vertically centered system action symbols.
- Book Club shows the voluntary-left preview and Quiet Studio Group shows the removed preview. Both use the ended-membership symbol and expose Archive/Unarchive and Delete instead of Mute or Leave.
- Rows use 56-point avatars, one-letter monograms, no separators, no chevrons, and support two-line previews while scrolling.
- At rest the top toolbar has no hard container boundary. Scrolling rows beneath it reveals Apple’s native hard glass container, and returning fully to the top removes it again.
- Returning from Settings by either the Back button or the interactive swipe gesture restores the profile avatar at full contrast without retaining a pressed or disabled state.
- An unavailable relay role replaces New Message with the compact warning
  action; New Message returns as soon as every role has connected coverage.
- The list fills the viewport through the bottom device safe area without leaving a separate white strip beneath its final visible row.
- Every message preview uses the same regular secondary text style.
- Numeric unread, mute, draft, and failed states are visibly distinct.
- Single-digit Unread and Failed appear optically equal in the shared status region.
- The visible fixtures include one-digit, two-digit, and capped **99+** unread values.
- The complete fixture set includes one-digit, two-digit, manual, and capped
  **99+** unread treatments without changing the catalog-first order.
- Mute appears beside the chat name; the timestamp shares that top row.
- Fixtures demonstrate time, Yesterday, weekday, and date timestamp variants.
  The unpinned sequence retains its deterministic relative order; **Direct -
  Text & Delivery** remains the sole initial pin.
- The first viewport begins with the ordered developer catalog. Retained story
  and legacy marketing conversations remain searchable and reachable below it.
- Every original conversation remains searchable and reachable after adding the ten legacy marketing conversations.
- The active list contains all ten documented attachment-preview treatments.
- Both onboarding paths open the populated account.
- Empty profile previews remain directly inspectable.
- No bulk-editing or refresh control is introduced. Profile switching remains
  available through Settings, and every catalog and retained row opens its
  profile-owned conversation state.
