# Design QA

## Scope

- Target: Pixel 10 Pro XL AVD, API 36, portrait, light mode, English, font scale 1.0.
- Capture viewport: 1344×2992 at 480 dpi.
- Content references: the original four supplied iOS screenshots plus the supplied Share & Connect reference.
- Android visual truth:
  - `/Users/vladimirkrstic/Workspaces/wn-android/app/src/main/java/dev/ipf/whitenoise/android/ui/theme/Theme.kt`
  - `/Users/vladimirkrstic/Workspaces/wn-android/app/src/main/java/dev/ipf/whitenoise/android/ui/theme/Type.kt`
  - `/Users/vladimirkrstic/Workspaces/wn-android/app/src/main/java/dev/ipf/whitenoise/android/ui/theme/Dimens.kt`
  - `/Users/vladimirkrstic/Workspaces/wn-android/app/src/main/java/dev/ipf/whitenoise/android/ui/account/AccountSelectorSheet.kt`
  - `/Users/vladimirkrstic/Workspaces/wn-android/app/src/main/java/dev/ipf/whitenoise/android/ui/chats/ChatListTopBar.kt`
  - `/Users/vladimirkrstic/Workspaces/wn-android/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/composer/ComposerPills.kt`
  - `https://developer.android.com/develop/ui/compose/components/scaffold`
  - `https://developer.android.com/develop/ui/compose/components/app-bars`
  - `https://developer.android.com/develop/ui/compose/components/bottom-sheets`

The iOS sources are authoritative only for features, fixture order, copy, and imagery. Component selection, navigation, hierarchy, color roles, density, shapes, and controls must follow the Android Material 3 references.

## Evidence

| Scene | Final capture | iOS-content comparison | Material conversion | Grayscale conversion |
| --- | --- | --- | --- | --- |
| Relays | `screenshots/raw/01-relays.png` | `screenshots/qa/01-relays-comparison.png` | `screenshots/qa/material/01-relays-before-after.png` | `screenshots/qa/grayscale/01-relays-cyan-to-grayscale.png` |
| Profile switcher | `screenshots/raw/02-profile-switcher.png` | `screenshots/qa/02-profile-switcher-comparison.png` | `screenshots/qa/material/02-profile-switcher-before-after.png` | `screenshots/qa/grayscale/02-profile-switcher-cyan-to-grayscale.png` |
| Chats | `screenshots/raw/03-chats.png` | `screenshots/qa/03-chats-comparison.png` | `screenshots/qa/material/03-chats-before-after.png` | `screenshots/qa/grayscale/03-chats-cyan-to-grayscale.png` |
| Fiatjaf conversation | `screenshots/raw/04-conversation.png` | `screenshots/qa/04-conversation-comparison.png` | `screenshots/qa/material/04-conversation-before-after.png` | `screenshots/qa/grayscale/04-conversation-cyan-to-grayscale.png` |
| Share & connect | `screenshots/raw/05-share-connect.png` | `screenshots/qa/share-connect/03-ios-material-comparison.png` | `screenshots/qa/share-connect/01-material-share.png` | Native grayscale theme |

The iOS-content comparisons normalize both sides to 1496 px high. The Material and grayscale comparisons normalize the previous Android capture and revised Android capture to the same 672×1496 side. A literal iOS-to-Android overlay is inappropriate because the intended platform structures differ by design.

Focused crops were not required for the earlier four-scene review: the original-resolution 1344×2992 captures clearly expose relay states, selected-profile treatment, pinned and unread badges, reply styling, reaction chip, image crops, and composer controls. The later Relays action change includes a focused top-bar crop because that single icon is the entire scoped difference.

Latest scoped Relays evidence:

- Source visual state: `screenshots/qa/relays-overflow/01-before-pencil.png`, with the user-approved direction to replace the pencil with a Material overflow action.
- Implementation: `screenshots/raw/01-relays.png`.
- Full-view comparison: `screenshots/qa/relays-overflow/02-pencil-to-overflow.png`.
- Focused top-bar comparison: `screenshots/qa/relays-overflow/03-top-bar-comparison.png`.
- Both captures are 1344×2992 at 480 dpi in the same light-mode, portrait, fixed-System-UI state. No density normalization was required.

Latest scoped Profile Switcher evidence:

- Source visual state: `screenshots/qa/profile-switcher-no-dividers/01-before-dividers.png`, with the user-approved direction to remove separators between profiles.
- Implementation: `screenshots/raw/02-profile-switcher.png`.
- Full-view comparison: `screenshots/qa/profile-switcher-no-dividers/02-before-after.png`.
- Focused profile-list comparison: `screenshots/qa/profile-switcher-no-dividers/03-profile-list-comparison.png`.
- Both captures are 1344×2992 at 480 dpi in the same settled-sheet, light-mode, portrait, fixed-System-UI state. No density normalization was required.

Latest scoped Chats evidence:

- User reference: `screenshots/qa/chats-white-no-dividers/00-user-reference.png`.
- Source visual state: `screenshots/qa/chats-white-no-dividers/01-before.png`, with the approved direction to use white, remove row separators, and restore below-fold conversations from the iOS fixture.
- Implementation: `screenshots/raw/03-chats.png`.
- Full-view comparison: `screenshots/qa/chats-white-no-dividers/02-before-after.png`.
- Focused upper-list comparison: `screenshots/qa/chats-white-no-dividers/03-list-comparison.png`.
- Scroll evidence: `screenshots/qa/chats-white-no-dividers/04-below-fold.png`.
- Initial captures are 1344×2992 at 480 dpi in the same light-mode, portrait, fixed-System-UI state. The scroll evidence uses the same viewport but is interaction evidence rather than a final store capture. No density normalization was required.

Latest scoped Chats bottom-edge and avatar-alignment evidence:

- Source visual state: `screenshots/qa/chats-bottom-avatar-align/01-before.png`, with the approved direction to remove the terminal bottom cutoff and align the Marmota profile avatar with the conversation-avatar column.
- Implementation: `screenshots/raw/03-chats.png`.
- Full-view comparison: `screenshots/qa/chats-bottom-avatar-align/02-before-after.png`.
- Focused avatar-alignment comparison: `screenshots/qa/chats-bottom-avatar-align/03-avatar-alignment.png`.
- Focused bottom-edge comparison: `screenshots/qa/chats-bottom-avatar-align/04-bottom-edge.png`.
- Both captures are 1344×2992 at 480 dpi in the same light-mode, portrait, fixed-System-UI state. No density normalization was required.

Latest scoped Google Messages surface-pattern evidence:

- User reference: `screenshots/qa/chats-messages-surface/00-reference.png` (1080×2400), used only for the gray header canvas, rounded white content surface, and extended chat-action pattern.
- Source visual state: `screenshots/qa/chats-messages-surface/01-before.png`.
- Implementation: `screenshots/raw/03-chats.png` (1344×2992 at 480 dpi; native Compose, so CSS viewport size does not apply).
- Reference-to-implementation comparison: `screenshots/qa/chats-messages-surface/02-reference-implementation.png`.
- Before-to-after comparison: `screenshots/qa/chats-messages-surface/03-before-after.png`.
- Focused header and rounded-surface comparison: `screenshots/qa/chats-messages-surface/04-header-surface.png`.
- Focused extended-action comparison: `screenshots/qa/chats-messages-surface/05-new-chat-button.png`.
- Comparisons normalize each full capture to a 672×1496 side. The implementation remains in the same light-mode, portrait, fixed-System-UI state as the preceding capture.
- Intentional product differences in that iteration: Marmota remained on the left, White Noise kept its `Messages` title and filter/search actions, the chat fixtures remained unchanged, the action remained black, and Google/AI branding was omitted. The later title-removal pass supersedes the visible-title choice.

Latest scoped Chats title-removal evidence:

- Source visual state: `screenshots/qa/chats-no-title/01-before.png`, with the approved direction to remove the standalone visible `Messages` title.
- Implementation: `screenshots/qa/chats-no-title/02-after.png` and `screenshots/raw/03-chats.png`.
- Full-view comparison: `screenshots/qa/chats-no-title/03-before-after.png`.
- Focused header comparison: `screenshots/qa/chats-no-title/04-header-comparison.png`.
- Both captures are 1344×2992 at 480 dpi in the same light-mode, portrait, fixed-System-UI state. The comparison normalizes each side to 672×1496; the focused header uses equal source crops.
- The visible title is removed while `Messages` remains exposed as the screen pane title in Compose semantics.

Latest scoped Share & Connect evidence:

- User and iOS content reference: `screenshots/qa/share-connect/00-ios-content-reference.png`.
- Final deterministic Share capture: `screenshots/qa/share-connect/01-material-share.png` and `screenshots/raw/05-share-connect.png`.
- Functional Connect-tab state: `screenshots/qa/share-connect/02-material-connect.png`.
- Normalized iOS-content-to-Material comparison: `screenshots/qa/share-connect/03-ios-material-comparison.png`.
- The Android scene deliberately replaces the iOS toolbar palette control and circular controls with a Material top app bar, primary tabs, icon actions, tonal copy button, and elevated QR card.
- The former `chats-top-action` variation is retired from the enum, scene picker, activity routing, and capture script. The standard `chats` scene retains its approved extended New chat FAB.

Latest scoped Conversation Material-density evidence:

- User reference and annotated source state: `screenshots/qa/conversation-material-pass/00-user-reference.png`.
- Pixel-native source visual state: `screenshots/qa/conversation-material-pass/01-before.png`.
- Implementation: `screenshots/qa/conversation-material-pass/02-after.png` and `screenshots/raw/04-conversation.png`.
- Full-view comparison: `screenshots/qa/conversation-material-pass/03-before-after.png`.
- Focused app-bar and background comparison: `screenshots/qa/conversation-material-pass/04-header-background.png`.
- Focused reaction and bubble-density comparison: `screenshots/qa/conversation-material-pass/05-reaction-spacing.png`.
- Pixel-native captures are 1344×2992 at 480 dpi in the same light-mode, portrait, fixed-System-UI state. The full comparison normalizes each side to 672×1496; focused evidence uses equal source crops.

Latest scoped Conversation compact-density evidence:

- Approved source state: `screenshots/qa/conversation-density-pass/01-before.png`, together with the user-directed timestamp, reaction, and composer corrections.
- Official Google Messages composer reference: `screenshots/qa/conversation-density-pass/google-play/01.png`, downloaded from the Google Play listing for Google Messages.
- Implementation: `screenshots/qa/conversation-density-pass/02-after.png` and `screenshots/raw/04-conversation.png`.
- Full-view comparison: `screenshots/qa/conversation-density-pass/03-before-after.png`.
- Focused bubble, timestamp, and reaction comparison: `screenshots/qa/conversation-density-pass/04-bubbles-times-reaction.png`.
- Focused Google Messages composer comparison: `screenshots/qa/conversation-density-pass/05-google-composer-reference.png`.
- Pixel-native captures are 1344×2992 at 480 dpi in the same light-mode, portrait, fixed-System-UI state. The full comparison normalizes each side to 672×1496; focused crops normalize their paired regions to equal dimensions.

Latest scoped Conversation top-message evidence:

- User reference: `screenshots/qa/conversation-top-messages/00-user-reference.png`.
- Approved source state before the content addition: `screenshots/qa/conversation-top-messages/01-before.png`.
- Final implementation: `screenshots/qa/conversation-top-messages/02-after.png` and `screenshots/raw/04-conversation.png`.
- Full-view normalized comparison: `screenshots/qa/conversation-top-messages/03-reference-material-comparison.png`.
- Focused top-thread comparison: `screenshots/qa/conversation-top-messages/04-top-thread-comparison.png`.
- The three added messages preserve the exact iOS fixture order, direction, line break, and timestamps: Marmota at 18:31, Fiatjaf at 18:32, and Marmota at 18:33.
- The existing Android Material bubble geometry, edge-tracking timestamps, reply, reaction, media collage, and composer remain unchanged.
- Source and implementation are 1260×2736 and 1344×2992 respectively. The full comparison normalizes both to 1496 px high; the focused comparison uses equal 672 px widths.

Latest scoped Profile Switcher clean-footer evidence:

- User reference and problem state: `screenshots/qa/profile-switcher-clean-footer/00-user-reference.png`.
- Implementation: `screenshots/qa/profile-switcher-clean-footer/01-after.png` and `screenshots/raw/02-profile-switcher.png`.
- Focused footer comparison: `screenshots/qa/profile-switcher-clean-footer/02-footer-comparison.png`.
- The implementation capture is 1344×2992 at 480 dpi in the same light-mode, settled-sheet state. The focused comparison normalizes both footer regions to 1110×788 before placing them side by side.

## Comparison history

### Baseline finding

- [P1] The first implementation imported iOS visual grammar instead of only iOS content.
  - Evidence: centered titles, circular bordered back controls, floating grouped action capsules, oversized white rounded panels, black primary buttons, black outgoing bubbles, and an overlay action pinned across the profile list.
  - Impact: the screenshots read as an iOS recreation with Android system bars rather than an Android product.
  - Fix: rebuild the shared theme and all four scenes with the production Android Material 3 palette, shapes, spacing, typography, and component patterns.

### Material pass

- Relays now uses `Scaffold`, a left-aligned Material top app bar, trailing action slot, tonal card, `ListItem` rows, semantic status icons, and a supporting Material list item.
- Profile switcher now uses a standard Material modal bottom sheet, tonal selected row, monospaced key identifiers, normal list density, and a full-width `FilledTonalButton`.
- Chats now uses a Material top app bar with avatar navigation, filter/search icon actions, `ListItem` rows, Material pinned/unread badges, and a primary FAB for composing.
- Conversation now uses a Material top app bar, primary-container outgoing bubbles, surface-variant incoming bubbles, tonal reply surface, assist-chip reaction, day chip, and a production-style Material composer.

Post-fix evidence: all four `screenshots/qa/material/*-before-after.png` comparisons show a consistent and unmistakable Material 3 conversion with no lost content.

### Grayscale pass

- [P1] The cyan primary palette was correct for the production Android app but did not match the requested White Noise screenshot direction.
  - Evidence: cyan section labels, selected-profile treatment, pinned and unread badges, FAB, outgoing bubbles, reply accent, and composer surface were visually dominant in the Material pass.
  - Impact: content and component patterns were correct, but the screenshots no longer shared the black-and-white character of the polished iOS prototype.
  - Fix: retain the Material 3 hierarchy and components while remapping the screenshot theme to black primary actions, white and neutral-gray surfaces, gray secondary text, and transparent surface tint.
- Green success and red failure remain only where they communicate relay connection state; reconnecting progress is neutral gray.
- Avatars and media remain in color so the content imagery stays legible and faithful to the supplied fixtures.

Post-fix evidence: all four `screenshots/qa/grayscale/*-cyan-to-grayscale.png` comparisons show the palette correction without changes to Material structure, content, imagery, or layout.

### Relays pencil action

- [P2] The plain `Edit` text action was technically supported but visually resembled an iOS navigation-bar action more than a typical Material top-app-bar action.
  - Evidence: `screenshots/qa/relays-pencil/01-before-text-edit.png`.
  - Impact: the sole trailing action weakened the Android-native character of an otherwise Material screen.
  - Fix: replace the `TextButton` with a standard Material `IconButton` using `Icons.Default.Edit` and the semantic description `Edit relays`.
- Post-fix evidence: `screenshots/qa/relays-pencil/02-before-after.png` and the focused `screenshots/qa/relays-pencil/03-top-bar-comparison.png` show the pencil centered in the existing action slot with unchanged title, navigation, spacing, content, typography, and color treatment.

### Relays overflow action

- [P2] The pencil was Android-native but visually overemphasized direct editing and did not match the approved direction.
  - Evidence: `screenshots/qa/relays-overflow/01-before-pencil.png`.
  - Impact: the prominent edit metaphor made a secondary set of relay actions feel like the screen’s primary task.
  - Fix: replace the pencil with the standard Material vertical overflow `IconButton` and the semantic description `Relay actions`.
- Post-fix evidence: `screenshots/qa/relays-overflow/02-pencil-to-overflow.png` and the focused `screenshots/qa/relays-overflow/03-top-bar-comparison.png` show a quieter action affordance with unchanged alignment, touch-target slot, typography, spacing, color, and screen content.

### Profile Switcher dividers

- [P2] Inset separators between profile rows made the sheet feel visually segmented and busier than the approved direction.
  - Evidence: `screenshots/qa/profile-switcher-no-dividers/01-before-dividers.png`.
  - Impact: the lines competed with the avatars and selected-row surface instead of letting grouping come from alignment and spacing.
  - Fix: remove only the `InsetDivider` calls between `ProfileListItem` rows in the modal sheet.
- Post-fix evidence: `screenshots/qa/profile-switcher-no-dividers/02-before-after.png` and the focused `screenshots/qa/profile-switcher-no-dividers/03-profile-list-comparison.png` show a cleaner continuous list with unchanged row height, avatar crops, typography, selected state, partial Free Signal row, and Add profile action.

### Profile Switcher clean list-to-action boundary

- [P2] The fixed 328 dp profile viewport stopped through the Free Signal row immediately above the Add profile button.
  - Evidence: `screenshots/qa/profile-switcher-clean-footer/00-user-reference.png`.
  - Impact: the avatar and supporting identifier were visibly clipped, making the footer look like it overlaid broken content rather than following a deliberate scroll region.
  - Fix: expand the profile viewport to the exact five-row Material list height, keep the Add profile action outside the list, and enable normal list scrolling for future overflow.
- Post-fix evidence: `screenshots/qa/profile-switcher-clean-footer/02-footer-comparison.png` confirms Free Signal now renders as a complete row with its avatar, name, and identifier; the fixed tonal action begins only after a clean 24 dp separation.

### Chats white surface, continuous list, and below-fold content

- [P2] The gray canvas and inset row separators made the chat list feel segmented instead of reading as the approved clean message surface.
  - Evidence: `screenshots/qa/chats-white-no-dividers/01-before.png`.
  - Impact: the separators competed with avatar and typography hierarchy, while the gray background reduced the crisp black-and-white presentation.
  - Fix: use the white surface token for both the Scaffold and top app bar, and remove only the dividers between chat rows.
- [P2] The Android fixture stopped after David Chaum and omitted the tenth legacy marketing chat plus established older activity.
  - Evidence: the source fixture defines Satoshi Nakamoto as the tenth uninterrupted marketing entry, followed by Fiatjaf, Mina Park, Theo Grant, and Maya Chen.
  - Impact: the list looked artificially complete at the first viewport and did not preserve the intended below-fold depth.
  - Fix: add those five source-backed conversations with local avatars and enable normal LazyColumn scrolling.
- Post-fix evidence: `screenshots/qa/chats-white-no-dividers/02-before-after.png` shows the white divider-free initial viewport with Satoshi Nakamoto completing the legacy block. `screenshots/qa/chats-white-no-dividers/04-below-fold.png` verifies Fiatjaf, Mina Park, Theo Grant, and Maya Chen are present and reachable below the fold.

### Chats bottom edge and avatar alignment

- [P2] The Marmota profile avatar in the top app bar was centered against the navigation slot rather than the conversation-avatar column.
  - Evidence: `screenshots/qa/chats-bottom-avatar-align/03-avatar-alignment.png`.
  - Impact: the two avatar systems appeared slightly staggered even though they establish the screen’s strongest vertical guide.
  - Fix: add the production 16 dp horizontal content inset to the top avatar action so its centerline matches the 56 dp conversation avatars.
- [P2] Scaffold navigation-bar insets and additional LazyColumn bottom padding produced a hard white terminal band at the bottom of the viewport.
  - Evidence: `screenshots/qa/chats-bottom-avatar-align/04-bottom-edge.png`.
  - Impact: the list appeared visibly cut off instead of continuing naturally below the fold.
  - Fix: remove the list-only 92 dp bottom padding, allow the Scaffold content to draw edge-to-edge with zero content insets, and apply `navigationBarsPadding()` only to the floating action button.
- Post-fix evidence: `screenshots/qa/chats-bottom-avatar-align/02-before-after.png` shows the aligned avatar column and the following Fiatjaf row continuing beneath the gesture-navigation area while the compose FAB remains safely positioned.

### Chats Google Messages surface pattern

- [P2] The previous all-white header and list merged into one flat plane instead of using the requested Google Messages-style Material surface hierarchy.
  - Evidence: `screenshots/qa/chats-messages-surface/01-before.png`.
  - Impact: the screen lacked the soft gray framing and clear rounded transition that make the reference feel polished and recognizably Material.
  - Fix: remap the Scaffold and top app bar to `CanvasGray`, then place the unchanged chat list in a full-height white `Surface` with 32 dp rounded top corners and 8 dp internal top spacing.
- [P2] The compact icon-only compose FAB did not reproduce the reference’s explicit chat action.
  - Evidence: `screenshots/qa/chats-messages-surface/03-before-after.png`.
  - Impact: the action was less self-explanatory and visually disconnected from the selected Material reference.
  - Fix: replace it with a black Material `ExtendedFloatingActionButton`, use the outlined chat-bubble icon, add the `New chat` label, and retain navigation-bar padding.
- Post-fix evidence: `screenshots/qa/chats-messages-surface/02-reference-implementation.png` confirms the intended two-surface hierarchy; focused comparisons `04-header-surface.png` and `05-new-chat-button.png` confirm the rounded transition, icon family, label, shape, and retained black action color.

### Chats visible title removal

- [P2] The standalone semibold `Messages` label competed with the Marmota avatar and attempted to occupy the role held by Google Messages’ custom branded wordmark.
  - Evidence: `screenshots/qa/chats-no-title/01-before.png`.
  - Impact: the gray header felt heavier and less intentional than the selected minimal utility-bar direction.
  - Fix: remove the visible `TopAppBar` title, preserve the existing avatar and actions at opposite edges, and retain `Messages` as the Scaffold pane title for accessibility semantics.
- Post-fix evidence: `screenshots/qa/chats-no-title/03-before-after.png` and the focused `screenshots/qa/chats-no-title/04-header-comparison.png` show a balanced empty center with unchanged header height, gray surface, avatar alignment, action placement, rounded list transition, and content.

### Share & Connect Material conversion

- [P2] Copying the iOS segmented toolbar, circular navigation controls, and edge-to-edge form geometry would reproduce Apple interaction grammar on Android.
  - Evidence: `screenshots/qa/share-connect/00-ios-content-reference.png`.
  - Impact: the new scene would conflict with the established Android Material component direction.
  - Fix: preserve Marmota, the public key, share/connect modes, QR code, and scan caption while using a Material top app bar, `PrimaryTabRow`, tonal copy action, and an elevated QR card.
- [P2] The removed chat top-action variation no longer served the approved screenshot set.
  - Fix: remove its route and renderer, keep the primary chat scene unchanged, and reuse capture slot 05 for Share & Connect.
- Post-fix evidence: `screenshots/qa/share-connect/03-ios-material-comparison.png` confirms matching content hierarchy with platform-native Android structure; `02-material-connect.png` confirms that the visible tab control changes the content state.

### Conversation white surface, compact density, and reaction placement

- [P2] The conversation inherited the theme’s gray canvas, making the app bar and thread resemble the chat-list framing even though a detail conversation should read as one continuous surface.
  - Evidence: `screenshots/qa/conversation-material-pass/01-before.png`.
  - Impact: the gray band weakened continuity between the participant header, messages, and composer.
  - Fix: use `SurfaceWhite` for the Scaffold, top app bar, and composer container while retaining neutral gray only for incoming messages, quoted content, the day marker, and the composer field.
- [P2] Message interiors, quoted-reply nesting, media framing, and the composer used excessive padding for the requested compact conversation density.
  - Evidence: `screenshots/qa/conversation-material-pass/03-before-after.png`.
  - Impact: bubbles felt inflated, fewer messages fit naturally in the viewport, and the content hierarchy was softened.
  - Fix: reduce text-bubble padding to 12 dp horizontal and 8 dp vertical, reduce bubble radii from 20 to 18 dp, compact quote and media insets, tighten list rhythm, and reduce the composer field from 52 to 48 dp.
- [P2] The fire reaction used an `AssistChip`, which is semantically intended for task assistance and produced an oversized control detached from its message.
  - Evidence: `screenshots/qa/conversation-material-pass/05-reaction-spacing.png`.
  - Impact: the reaction read like a separate button rather than metadata attached to the outgoing message.
  - Fix: replace the chip with a compact white pill `Surface`, add a neutral outline and 1 dp elevation, and overlap it 10 dp across the outgoing bubble’s lower-left edge while preserving the external timestamp.
- Post-fix evidence: `screenshots/qa/conversation-material-pass/04-header-background.png` confirms a continuous white thread surface; `05-reaction-spacing.png` confirms compact bubble padding and the attached reaction pill; the full `03-before-after.png` confirms all original copy, reply context, timestamps, collage imagery, and composer actions remain intact.

### Conversation compact scale, edge-tracking timestamps, reaction, and composer

- [P2] The previous message scale still felt enlarged because body text, nested quote padding, bubble radii, media width, and top-bar identity were all reduced only slightly.
  - Evidence: `screenshots/qa/conversation-density-pass/03-before-after.png`.
  - Impact: the conversation continued to read as a magnified preview instead of a dense Android messaging thread.
  - Fix: move message and caption copy to the Material `bodyMedium` role, reduce text-bubble insets to 10 dp horizontal and 6 dp vertical, reduce bubbles to 16 dp radii, compact the quote card and identity header, and narrow reply/media groups to 80% of the available content width.
- [P2] Timestamps were aligned to the screen edge rather than the message geometry.
  - Evidence: `screenshots/qa/conversation-density-pass/04-bubbles-times-reaction.png`.
  - Impact: timestamps appeared detached from short and medium-width messages.
  - Fix: wrap each message and its timestamp in one intrinsic-width group; align outgoing timestamps to the bubble’s lower-left edge and incoming timestamps to its lower-right edge.
- [P2] The reaction pill remained visually heavy because its elevation produced a conspicuous shadow and the emoji sat too close to the outline.
  - Evidence: `screenshots/qa/conversation-density-pass/04-bubbles-times-reaction.png`.
  - Impact: the reaction still read as a floating control rather than lightweight message metadata.
  - Fix: remove elevation entirely, soften the outline, increase the pill’s horizontal breathing room, and keep the pill overlapping the outgoing bubble’s lower-left edge.
- [P2] The composer put emoji, attachment, and microphone actions inside one full-width 48 dp field, leaving the placeholder squeezed despite the field’s large footprint.
  - Evidence: `screenshots/qa/conversation-density-pass/05-google-composer-reference.png`.
  - Impact: the control looked inflated and internally cramped at the same time.
  - Fix: follow the official Google Messages structure: place an outlined add action outside a 44 dp rounded field, keep the placeholder comfortably inset, and retain only emoji and microphone actions inside the field with 40 dp icon-button slots.
- Post-fix evidence: `03-before-after.png` confirms the reduced overall scale; `04-bubbles-times-reaction.png` confirms bubble-edge timestamp tracking and the shadowless overlapping reaction; `05-google-composer-reference.png` confirms the composer now follows the same outer-add/inner-field hierarchy and proportions as Google Messages.

### Conversation fixture-completeness pass

- [P2] The Android conversation began at the 18:36 migration confirmation and omitted the first three messages from the polished iOS fixture.
  - Evidence: `screenshots/qa/conversation-top-messages/01-before.png` compared with `00-user-reference.png`.
  - Impact: the thread lost the setup and conversational progression that explains the later reply.
  - Fix: insert the exact 18:31 outgoing, 18:32 incoming, and two-line 18:33 outgoing messages immediately after the Today marker.
- Post-fix evidence: `screenshots/qa/conversation-top-messages/03-reference-material-comparison.png` confirms matching order, text, direction, and timestamps while preserving the denser Material layout; `04-top-thread-comparison.png` confirms the added top sequence without changes to the approved lower thread styling.

### Google Messages-density conversation pass

- [P2] The prior fixture-completeness capture compressed the entire thread into one non-scrollable viewport.
  - Evidence: `screenshots/qa/conversation-google-density/01-before.png`.
  - Impact: message text and media read as a scaled-down screenshot rather than a normal Android conversation, and there was no meaningful below-the-fold timeline.
  - Fix: restore Material `bodyLarge` message copy at 16 sp/24 sp, 12 dp horizontal and 8 dp vertical bubble insets, 18 dp main corners with 4 dp directional corners, a 340 dp maximum bubble width, 16 dp timeline margins, 4 dp block rhythm, and native `LazyColumn` scrolling anchored at the Today marker.
- [P2] The prior header and composer did not include the selected publicly visible Google Messages shell.
  - Evidence: `screenshots/qa/conversation-google-density/00-google-play-reference.png` and `06-google-composer-comparison.png`.
  - Impact: the conversation lacked expected search and overflow actions, while the composer omitted the separate media action used by the public reference.
  - Fix: add Search and overflow actions to the participant app bar; use separate 48 dp Add and Add Photo actions beside a 48 dp rounded field containing the Message placeholder, emoji, and microphone actions.
- [P2] The Today marker and fire reaction were visually heavier than message metadata.
  - Evidence: `screenshots/qa/conversation-google-density/05-before-after.png`.
  - Impact: the day marker read like a filter chip and the reaction competed with its parent message.
  - Fix: use centered muted label text for Today and a 28 dp outlined reaction pill with no elevation, attached by an 8 dp overlap.
- Post-fix evidence: `screenshots/qa/conversation-google-density/02-after.png` confirms the natural-density top viewport; `03-scrolled.png` confirms the lower reply, reaction, short message, and media remain reachable while the app bar and composer stay pinned; `04-user-material-comparison.png` confirms the supplied content hierarchy translated into Material structure; `06-google-composer-comparison.png` confirms the public Google Messages composer proportions.

### Real-device Google Messages full-screen shell pass

- Source visual truth: `screenshots/qa/conversation-google-fullscreen/00-real-device-reference.png`, captured by the user from the shipping Google Messages full-screen conversation on a Google phone.
- Implementation: `screenshots/qa/conversation-google-fullscreen/02-after.png`.
- Viewport and normalization: source 1080×2400; implementation 1344×2992 at the screenshot AVD’s 480 dpi. Both were normalized to 672 px width for the focused 1344×420 header comparison in `03-header-comparison.png`.
- State qualification: the source is an empty SMS/MMS thread with messaging disabled because no SIM is installed; the implementation is a populated White Noise thread. QA therefore compares only the full-screen app-bar shell and rounded conversation-surface boundary, not message content or composer state.
- [P2] The prior conversation used one continuous white surface, included a Search action, and did not reproduce the rounded white thread sheet visible on the shipping Google phone.
  - Evidence: `screenshots/qa/conversation-google-fullscreen/01-before.png` and the source reference.
  - Impact: the screen followed the Android 17 compact Bubble-window promo more closely than the current full-screen Google Messages hierarchy.
  - Fix: use the neutral canvas token behind the status/app-bar region, remove Search, retain Back/avatar/name/overflow, enlarge the participant avatar to 40 dp, and place the timeline on a white surface with 32 dp top corners.
- Post-fix evidence: `screenshots/qa/conversation-google-fullscreen/03-header-comparison.png` confirms the gray shell, action set, participant identity, white rounded thread surface, and boundary position; `04-before-after.png` confirms message content and the existing composer remain unchanged.

### Timestamp inset and reaction-height refinement

- Source visual truth: `screenshots/qa/conversation-timestamp-reaction-refinement/00-timestamp-reference.png` and `00-reaction-reference.png`, cropped from the user-reviewed Android capture.
- Implementation: `screenshots/qa/conversation-timestamp-reaction-refinement/02-after.png`, 1344×2992 at 480 dpi.
- [P2] External timestamps aligned with the bubble’s curved outer bounds rather than its text column.
  - Evidence: `screenshots/qa/conversation-timestamp-reaction-refinement/01-before.png`.
  - Impact: short bubbles left timestamps sitting directly beneath the large directional corner, where they appeared visually detached from the copy.
  - Fix: inset outgoing timestamps 12 dp from the bubble’s left edge and incoming timestamps 12 dp from its right edge, matching the message text padding.
- [P2] The 28 dp reaction pill still appeared tall relative to the compact timestamp and message density.
  - Evidence: `screenshots/qa/conversation-timestamp-reaction-refinement/00-reaction-reference.png`.
  - Impact: the reaction carried more visual weight than lightweight message metadata.
  - Fix: reduce the reaction to 24 dp high, use the `bodySmall` emoji role, and preserve the 8 dp attachment overlap by reducing both reserved space and vertical offset to 16 dp.
- Post-fix evidence: `screenshots/qa/conversation-timestamp-reaction-refinement/03-before-after-detail.png` confirms timestamp alignment now follows each bubble’s text column; `04-reaction-comparison.png` confirms the shorter, shadowless attached reaction.

### Reaction 20 dp refinement

- [P3] The 24 dp reaction remained slightly taller than the final requested metadata scale.
  - Fix: reduce its height to 20 dp and its reserved vertical offset to 12 dp, preserving the approved 8 dp overlap.
- Evidence: `screenshots/qa/conversation-reaction-20dp/03-before-after-reaction.png` confirms the slimmer pill remains legible, attached, and free of clipping or shadow.

### Reaction 22 dp final balance

- [P3] The 20 dp reaction appeared slightly compressed around the emoji.
  - Fix: settle at 22 dp high with a 14 dp offset, retaining the same 8 dp attachment overlap.
- Evidence: `screenshots/qa/conversation-reaction-22dp/03-before-after-reaction.png` confirms improved vertical breathing room without returning to the earlier tall appearance.

### Google Messages compact composer pass

- Source visual truth: `screenshots/qa/conversation-google-composer-final/00-android17-reference.png`, the user-selected Google Messages composer shown in Google’s Android 17 Bubble-window presentation.
- Implementation: `screenshots/qa/conversation-google-composer-final/02-after.png`, 1344×2992 at 480 dpi.
- State qualification: the source is a compact floating conversation while the implementation is a full-screen Pixel conversation. QA compares the composer’s component hierarchy, density, shapes, icon order, and visual emphasis rather than equal overall width.
- [P2] The prior composer placed Add and gallery outside the message field and kept the microphone inside it.
  - Evidence: `screenshots/qa/conversation-google-composer-final/01-before.png`.
  - Impact: the row was visually fragmented and did not match the selected newer Google Messages hierarchy.
  - Fix: use one 48 dp tonal pill containing Add, Message, emoji, and gallery; place an independent 48 dp filled-tonal voice button after an 8 dp gap; retain the white, elevation-free surrounding surface and gesture clearance.
- Post-fix evidence: `screenshots/qa/conversation-google-composer-final/03-reference-composer-comparison.png` confirms the selected icon order, pill proportions, internal density, gallery symbol, and separate waveform action; `04-before-after-composer.png` confirms the cleaner grouping without affecting the timeline or media.

## Required fidelity surfaces

- Fonts and typography: Manrope remains the brand family, now applied to the Material 3 baseline type scale. App-bar titles, list headlines, supporting copy, timestamps, badges, and message text use appropriate Material roles without clipping.
- Spacing and layout rhythm: layouts use `Scaffold`, built-in app-bar and sheet insets, Material list density, a 4 dp spacing grid, and the Android 8/12/16/24 dp shape scale. Conversation timestamps track their message edges, and persistent controls clear the gesture-navigation area.
- Colors and tokens: core UI is black, white, and neutral gray while preserving Material 3 role relationships. Green relay success and Material error red are limited to semantic status communication.
- Image quality and asset fidelity: all supplied avatars and five conversation images remain local, sharp, correctly masked, and deterministically center-cropped.
- Copy and content: all seven relays, all five profile rows, fourteen chats, unread counts, pinned/muted states, timestamps, message text, reply, reaction, and five-image collage remain intact.
- Icons and components: visible controls use Material icons and Material 3 composables rather than handcrafted approximations. The conversation composer uses the official Google Messages outer-add/inner-field hierarchy while preserving the prototype’s grayscale theme.
- Accessibility and tap targets: app-bar actions, icon buttons, sheet rows, tonal button, FAB, and composer actions use standard Material touch targets and semantics.

No actionable P0, P1, P2, or P3 visual findings remain.

## Determinism and verification

Two consecutive complete capture runs produced identical SHA-256 hashes:

```text
497026ff5fef85e194b98e779eb7988ff5fbf800a848b7cfdd95577fc233ce47  01-relays.png
fc655e49b50f4cb478c44d9f06add8435cd14b60d88012831d601504d6be986f  02-profile-switcher.png
a36aef74e063efb94e118584c031490bdc4a113a9d92614b03e8b9257b25cb8f  03-chats.png
31aa4ba9b799c8954e2a0cf67e0fb3cd3ca760aeef1ee2f3ce8dac9e66810a83  04-conversation.png
e527978f766cbb8ce4bf07f3ec5275af592f5b9da64000c79bb0ba6391818ce3  05-share-connect.png
```

`assembleDebug`, unit tests, Android lint, APK installation, and all five deep-link smoke launches passed.

final result: passed
