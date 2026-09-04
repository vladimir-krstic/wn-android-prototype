# White Noise Android UI metrics

These are the default metrics for custom White Noise Android compositions.
Use them unless the latest explicit user direction or a standard Android or
Material component requires a different value. Material components continue
to own their internal padding, typography, shape, state layers, and motion.

## Layout rhythm

Android's current guidance uses an 8 dp layout grid, with a 4 dp sub-grid for
small details. White Noise maps that guidance to these shared tokens:

| Relationship | Default | Code token |
| --- | ---: | --- |
| Compact-screen horizontal margin | 16 dp | `CompactScreenMargin` |
| Settings section heading inset | 32 dp | `SettingsSectionInset` |
| Closely related controls, actions, or helper copy | 8 dp | `Related` |
| Peer fields in one form group | 16 dp | `FormField` |
| Separate content groups or an inline primary action after fields | 24 dp | `Section` |
| Pinned bottom task-action inset | 16 dp | `PinnedActionInset` |

Use `WhiteNoiseSpacing` from `ui/theme/WhiteNoiseSpacing.kt`; do not duplicate
these values as screen-local magic numbers. Constrained/adaptive panes may add
outer space at larger widths, but the compact margin remains 16 dp inside the
active pane.

## Interaction state-layer containment

- A custom press, hover, focus, or ripple indication is bounded and clipped to
  the visible element by default. A larger transparent semantic/touch target
  may own input, but it must share its `MutableInteractionSource` with an
  indication drawn inside the visible element's real shape. Do not draw a
  rectangular state layer across invisible target padding, alignment space, or
  a full-width parent when the visible control is smaller.
- A whole-row indication is correct only when the row itself is the visible
  interactive element, such as a grouped Material list row or the explicitly
  accepted conversation selection row. Unbounded or intentionally overflowing
  indications require a component-specific decision; they are never the
  default.
- Material components continue to own their native indication when their
  component bounds and shape are the intended visible surface. For custom
  split target/visual compositions, suppress the outer indication and render
  one bounded Material ripple from the shared interaction source on the inner
  clipped surface.

## Forms and actions

- Use the shared Material-based tonal field for ordinary forms. It uses
  `surfaceContainerHigh`, no resting/disabled border, a 2 dp full-shape focus
  or error ring, and the shared extra-large shape so all four corners have the
  same 28 dp radius. A single-line 56 dp field is therefore a full capsule;
  multiline fields keep the fixed 28 dp radius rather than scaling it with
  their height. Disabled fields use `surfaceContainerLow` and Material disabled
  content colors.
- Settings detail pages and ordinary app-owned dialogs or sheets use
  `surfaceContainerLow` as their neutral canvas. Their grouped controls and
  shared text fields use `surfaceContainerLowest`, producing the same
  gray-canvas/white-component hierarchy as the Settings root without literal
  light-theme colors. System-owned surfaces and specialized camera/media
  presentations retain their platform or feature-specific treatment.
- The field content inset is 16 dp. Above-field labels and supporting/error
  text align to that same directional start line; Material's 24 dp leading
  icon artwork also begins on it inside the standard 48 dp slot. Keep the
  native 4 dp label-to-container and supporting-text gaps.
- Separate peer fields by 16 dp. Use 24 dp between the form and a distinct
  section or an inline primary task action. A pinned action belongs to the
  `Scaffold` bottom slot and uses 16 dp outer insets plus navigation-bar and IME
  handling.
- Single-line fields use Material's 56 dp minimum container. Full-width,
  form-adjacent, and primary task buttons use Material's matching 56 dp medium
  button container with its 24 dp horizontal and 8 dp vertical content
  padding. Closely related stacked buttons use an 8 dp gap.
- Compact contextual actions, search bars, menus, and message composers keep
  their native component metrics instead of inheriting form spacing.
- The conversation composer host is transparent: only the separate 48 dp Add
  control and 48 dp-minimum editor capsule draw containers. Over mixed
  timeline content, the idle Add control uses `primary`/`onPrimary` and the
  capsule uses a 1 dp `outlineVariant`; these semantic roles adapt in dark mode
  without restoring a full-width backing. Its attachment
  popup uses the user-approved 10 dp visible trigger gap; this is a scoped optical
  placement exception to the 4/8 dp layout rhythm. Bottom composer menus use
  an above-anchor provider because Material's stock 48 dp window-edge margin
  otherwise detaches them from their triggers. Their groups retain Material's
  default menu shadow elevation so they separate from content behind them.
  Idle waveform artwork is 24 dp inside a 48 dp target.
  Live/review waveforms use 2 dp bars, 2 dp gaps, and a 24 dp visual height.
  Stop is a red 20 dp square and Send is a 20 dp upward arrow inside separate
  48 dp targets. Review Play uses a 32 dp filled circle inside a 48 dp target.
  Review Transcribe and Message Format keep transparent 48 dp-minimum targets,
  but share interactions with separate clipped 32 dp-minimum visible pills.
  Those pills use 8 dp horizontal and 4 dp vertical content padding; 4 dp of
  transparent horizontal breathing room remains inside each target. Large
  text may grow the visible pill and target instead of clipping.
  Transcribe uses one 8 dp icon/label relationship with no additional spacer.
  Complete member mentions inside the editor use the adaptive medium-neutral
  `outlineVariant` surface with `onSurface` text and the shared
  search-highlight glyph renderer's 4 dp corner radius; do not use Compose's
  square span background. Composer reply and link-preview surfaces
  use an 8 dp inset and 16 dp radius inside the 24 dp composer. Timeline reply
  surfaces participate in the shared rich-message geometry below rather than
  owning a separate inset/radius rule. In draft
  media review, the 48 dp inclusion target anchors to the fitted image's
  bottom-end corner; its 22 dp visible check sits inside the image with 6 dp
  from both edges.
- Draft gallery thumbnails are 48 dp squares centered in adjacent 56 dp touch
  targets, giving an 8 dp visible image gap. The selected outline is 1 dp;
  unselected images have no outline. The 72 dp rail includes 8 dp top/bottom
  padding and retains the 16 dp outer horizontal inset.
- Both gallery viewports clip the pager and each page to their exact bounds,
  disable pager edge stretching, and translate only the pager content over a
  stationary opaque background for downward dismissal. The draft thumbnail
  rail also clips and disables stretching. Dismissal requires the lesser of
  120 dp or 20% of the media viewport height, or a downward flick of at least
  1250 dp/s after 32 dp travel. A strong upward reversal cancels. Material's
  fast spatial motion settles the pull. Horizontal paging, multi-touch zoom,
  and scrubbing retain priority; zoomed-photo vertical movement remains pan.
- App-owned single-choice dialogs place direct 56 dp-minimum selectable rows in
  the dialog content slot. The row starts at the dialog-owned content edge,
  lets `RadioButton` retain its native touch-target inset, and keeps 16 dp
  between that target and the label. Do not nest a settings `ListItem` inside
  the already-padded dialog slot; that creates a second horizontal inset.

## Conversation transcript

- Audio and video playback trackers omit Material's fixed terminal stop dot,
  per the user's 2026-09-03 direction. This includes voice bubbles, Read Aloud,
  and the gallery video slider. Preserve the actual seek thumb and standard
  track geometry, colors, semantics, and interaction.
- Each day boundary is ordinary centered `labelMedium` system text using
  `onSurfaceVariant`, 12 dp horizontal and 3 dp vertical label padding, and the
  shared 16 dp form-field relationship above and below. It has no pill, card,
  elevation, or other surface in the transcript. Once the active boundary has
  left the viewport, a separate top replacement uses the same label metrics in
  a capsule with `surfaceDim` at 82% alpha and `onSurface` text. Material's
  dim-surface role keeps the capsule darker in both light and dark appearance,
  giving it clearer separation from an incoming `surfaceContainerHigh` bubble
  while remaining translucent. The capsule disappears whenever the matching
  inline boundary is visible, so the two representations are never shown for
  the same active day at once.
- Tail-free message bubbles use a 16 dp radius, 12 dp horizontal and 8 dp
  vertical content inset, and a compact-screen maximum width of 340 dp. Same-
  author messages use a 2 dp vertical relationship; a new cluster uses 16 dp.
  Incoming group clusters reserve a 30 dp avatar and 6 dp avatar-to-message
  gap. Align the avatar's bottom to the bubble, excluding reactions and time;
  align the author label to the bubble's 12 dp content inset. Direct messages
  do not reserve hidden identity space.
- A bubble containing a reply quote or attachment changes from the text-only
  inset to one 6 dp shell inset on all four sides. Every quote, gallery, GIF,
  link preview, file, contact, and voice surface uses the resulting shared
  inner canvas and a 10 dp radius: the child radius is the 16 dp bubble radius
  minus the 6 dp inset, so the outlines remain concentric. Use 6 dp between
  stacked rich sections and 2 dp between gallery tiles; clip a gallery once at
  its outer 10 dp outline instead of rounding each tile. Rich cards use 6 dp
  internal content padding. A lone photo or video is the only canvas-width
  exception: use a 256 dp media height, derive width from the actual ratio,
  and cap the frame at 256 dp wide. Preserve the full image height and crop
  only centered horizontal overflow; constrained parents reduce width without
  changing height. The tiny-source safeguard can reduce both frame dimensions.
  Captions wrap to that same width. Decoded dimensions precede catalog metadata.
  Albums, GIFs, reply quotes, files, contacts,
  links, voice, and stacked combinations use a 256 dp inner canvas. Mixed text
  adds 6 dp horizontal and 2 dp bottom inside that canvas, restoring the same
  12 dp horizontal and 8 dp bottom visual text inset without moving the rich
  child away from the 6 dp shell edge. Link artwork fills the canvas above its
  text; file rows use a bare 24 dp semantic file glyph rather than another
  nested rounded icon surface.
- Terminal metadata occupies one nonwrapping line attached to the bubble. A
  reaction rail sits on the center-facing side and the time/state label on the
  opposite side. The visible reaction pill is 23 dp high and at least 31 dp
  wide, with a 1 dp outline, 7 dp horizontal inset, 2 dp emoji/count gap, and
  one 16 dp pinned Signal atlas sprite; show no visible count for one. Counted
  and `+N` pills grow from that compact
  minimum; neighboring visible pills are 3 dp apart. Keep their horizontal
  layout compact while retaining the 48 dp minimum vertical target and
  Compose-expanded minimum pointer target. The visible pill overlaps the
  bubble bottom by 9 dp. Inset both the center-facing rail and opposite
  timestamp 12 dp from the bubble edge. Timestamp direction does not change
  when reactions are absent: incoming time stays on the bubble's start/left
  edge and outgoing time on its end/right edge in LTR, mirrored with the
  message in RTL. With or without reactions, place the timestamp 2 dp below
  the bubble; only the visible reaction pill overlaps its bottom edge. Use the
  lower-emphasis `outline` neutral for timestamp text, the Sent status fill,
  and Sending progress in both the transcript and focused context-menu overlay.
  Focused metadata retains the ordinary chat colors over the light, translucent
  blurred backdrop. Sent and sending
  outgoing timestamps include their delivery state; the Sent state uses a
  14 dp filled status container with 10 dp check artwork, while incoming
  timestamps reserve no delivery icon. Failed outgoing delivery uses that same
  end/right placement, 12 dp edge inset, 2 dp top gap, 3 dp icon gap,
  `labelSmall` typography, and 14 dp status footprint; only the warning icon
  and **Not delivered, tap to retry** label switch to the semantic error color.
  Grow the bubble with metadata up to
  340 dp; within that width show at most four real reaction types followed by
  one `+N` overflow pill. Progressively reduce the real types further only
  when the available width requires it. Never wrap reactions into another row.
- Multi-message selection reserves one 48 dp leading column independent of
  incoming/outgoing direction. Center the native Checkbox against message
  content and retain a whole-row toggle target. Selection surfaces must not
  alter bubble width, alignment, or cluster geometry.
- Reply swipe follows semantic leading-to-trailing direction: 64 dp readiness,
  96 dp maximum visible travel, resisted overdrag, one threshold haptic, and a
  spring/short return. Bubble, metadata, and reactions move as one unit. Anchor
  a 48 dp indicator target to the resting bubble's semantic start and vertical
  center, not to the message row edge. Use the 24 dp Google Material Symbols
  Rounded Reply glyph at weight 600. After 5% progress, fade it in, travel it
  10 dp with the bubble, scale it from 1.0 to 1.2 by readiness, and pulse it to
  1.8 over 200 ms when readiness is first crossed. Its fade, travel, scale, and
  spring return derive from the same swipe state as the bubble. Keep the named
  Reply accessibility action.
- The focused-message overlay keeps a real message rendering between a quick-
  reaction rail and the ordered command surface. Align these surfaces with
  bubble direction, preserve 16 dp window margins, and shift the composition
  within status/navigation/IME safe bounds instead of detaching it into a
  bottom sheet. Scale exceptionally tall real-message previews proportionally
  within a 320 dp height budget rather than replacing them with a summary.
  Each quick reaction is a 48 dp semantic target with a centered 40 dp circular
  Material state layer and a 36 dp selected fill. Render the pinned Signal
  artwork through the app-owned atlas renderer in a fixed 28 dp square,
  independent of font scaling, and
  give the final More Reactions target a 24 dp horizontal-ellipsis glyph inside
  its own neutral 40 dp circle. The rail owns a 4 dp inset on all four sides and
  4 dp between targets, so its press feedback has equal breathing room instead
  of touching an edge or filling a rectangular slot. Use the exact standard
  Material menu-group container color, Level 0 tonal elevation, and Level 2
  shadow elevation shared by the command group. Keep an 8 dp shadow-safe
  gutter at the top and bottom of the focused scroll viewport so those shadows
  are not clipped; this gutter does not add to the visible 8 dp rail/message or
  message/menu gaps.
  Keep 8 dp above the message and 8 dp below its visible content; when reactions are
  present, measure the lower gap from the visible pill edge, excluding the
  remaining transparent interaction-target inset. Preserve that visible-edge
  relationship when scaled type increases the pill height. Its backdrop blurs
  the underlying conversation by 24 dp and applies an 88% `surfaceContainerLowest`
  veil, white in light appearance and adaptive near-black in dark appearance.
  Disable the Dialog window's additional platform dim so the layers do
  not compound into a harsh scrim. On Android 11 and earlier, where Compose
  blur is unavailable, retain the same translucent surface veil as the graceful
  fallback.
  The full-window dismiss target owns every visually empty point. Consume taps
  only inside the visible reaction rail, rendered message-plus-metadata bounds,
  and command group; do not attach a no-op click target to their full-width
  alignment column. One tap beside or between those surfaces dismisses the
  overlay immediately.
- The reaction emoji picker uses the shared ordinary Material sheet and one
  continuous sectioned lazy grid. Its expanded content is capped at 88% of the
  available height. The picker enables only Expanded and Hidden anchors so its
  pinned category rail cannot sit below a partially clipped viewport; Material
  retains the rounded cap, handle, motion, Back, swipe dismissal and inset
  ownership. Search uses the
  standard rounded 56 dp field with 16 dp horizontal insets and expands the
  sheet on focus. Section labels are `labelLarge`, semibold, heading-semantic,
  and at least 36 dp high. Emoji columns are adaptive with a 48 dp minimum;
  each cell owns one circular 48 dp Material target and a fixed 32 dp pinned
  Signal atlas sprite independent of font scale. The sprite renderer crops the
  one-pixel gutter from each 66 px source tile and scales imagery rather than a
  text baseline. A bottom 56 dp-minimum rail is
  pinned below a one-dp divider. Configure is a fixed leading 48 dp gear
  target. Categories remain horizontally scrollable; every category has a
  48 dp target, 22 dp official rounded symbol, and 36 dp selected circle. The
  selected category follows the grid's first visible section and a category
  tap expands then scrolls the sheet to its header. Hide the category rail for
  nonblank search results so the IME and results own the available height.
- The newest timeline item settles fully above the measured compact composer
  plus system bottom inset. Edge-to-edge drawing behind the floating composer
  is intentional; a settled message underneath an interactive composer target
  is not.

## Settings overview

- The Settings root uses `surfaceContainerLow` for its neutral canvas and
  `surfaceContainerLowest` for white-equivalent cards. The pinned app bar uses
  the canvas tone at rest and `surfaceContainer` under scroll. The hierarchy
  remains semantic in dark mode rather than substituting literal colors.
- One rounded profile card contains the 56 dp active-profile row and a native
  one-line profile-management `ListItem`, separated by a two-dp
  `surfaceContainerLow` surface gap. With no alternate it uses the official
  `person_add` symbol and reads Add Profile. With alternates it reads Switch
  Profile, previews one 32 dp avatar or at most three overlapping 32 dp avatars
  plus a true 32 dp circular `+N` remainder slot, and expands in place. It is
  not an outlined or contained button.
- Expanded inactive profiles use native two-line rows with 48 dp avatars,
  shortened keys and aggregate monochrome `primary`/`onPrimary` unread badges
  capped at `99+`. Add Profile is the final row. The same surface gap separates
  every row in the expanded group. Back collapses the card before navigating
  away.
- Main Settings destinations use Material's native one-line `ListItem`
  measurement rather than a custom fixed row height. In the current component
  this yields the standard 56 dp minimum while remaining free to grow for font
  and display scaling.
- Each destination uses one 24 dp leading Material Symbol and the standard
  trailing disclosure slot. The icon is a noninteractive signifier inside the
  row, not a separate touch target.
- Root destination and Help groups place a two-dp `surfaceContainerLow` gap
  between adjacent transparent `ListItem`s. The gap spans the card width and
  mirrors the page canvas, so a group reads as one white-equivalent surface
  with visible separation rather than a stack of individually outlined
  controls.
- The consolidated seven-destination root group and the Help destination group
  have no visible headings. Wherever a Settings section label remains on a
  detail screen, it starts 32 dp from the screen edge:
  the 16 dp compact margin plus the list item's 16 dp internal content inset.
  This aligns the heading with leading icons on the overview and with row text
  on icon-free detail lists; it does not align to the outer container edge.
- Settings helper/explainer text outside a group or button also starts 32 dp
  from the active pane edge. This is the same row-title/section-label content
  line: 16 dp outer group or button margin plus Material's 16 dp list content
  inset. It begins 8 dp below the group or action it explains and adds no
  bottom padding of its own; the following independent section or action owns
  the standard 24 dp separation. Do not align these helpers to the 16 dp outer
  container edge or give them symmetric vertical padding.
- Do not add supporting text when the headline already names the destination.
  Keep it only for an actionable unavailable reason. The Appearance choice is
  intentionally absent from the root and remains visible/selectable inside
  Appearance.
- Material continues to own list-item content padding, typography, state
  layer, minimum target size, and adaptation. Directional disclosure and exit
  symbols mirror in right-to-left layouts.
- The retained modal profile switcher uses native 48 dp two-line rows. The
  active row receives
  the selected container and 24 dp check; inactive unread badges use the same
  monochrome `primary`/`onPrimary` roles as Chats, expand with text, and cap at
  `99+`. Its full-width Add Profile action remains below the list rather than
  scrolling with it.

## Welcome mark and system splash

Welcome centers the mark in the space between the bottom of the top safe area
and the actual top edge of Sign In. Add Profile uses the bottom of its Material
top app bar as the upper boundary. A weighted logo region above the measured
button group owns this relationship; do not use a full-window center or a
fixed vertical offset.

Welcome has no text input. Use the union of system-bar and display-cutout
insets instead of `safeDrawing`, which also includes the IME. An outgoing
form's keyboard must not resize this logo region or lift Welcome's buttons
during a navigation transition.

The visible mark is 50% of the safe content width, capped at 260 dp (half the
520 dp form measure), with its original 598:460 aspect ratio. If vertical space
is tight, shrink it to fit with 16 dp of breathing room above and below. Actions
keep their native minimum 56 dp height, 8 dp gap, 16 dp horizontal margins and
bottom inset, and system-safe bottom clearance.

Android's system splash remains independently platform-sized: the visible
149.5 × 115 dp mark fits within the 192 dp safe circle of a centered 288 dp icon
canvas. Welcome no longer matches that size or position. This user-approved
layout supersedes the splash-matching requirement in WN-ANDROID-0019; do not
enlarge or reposition the system splash to imitate the Welcome composition.

## Share & Connect

- The route uses a center-aligned **Share & Connect** app-bar title. Back and
  Share keep native 48 dp icon targets; no segmented control or button group is
  present because the page has one stable identity state and one scanner task.
- The identity composition is proportional within the adaptive pane rather than
  tied to one device screenshot. The avatar uses 32% of the pane width clamped
  to 104–152 dp. The QR technical surface uses 81% clamped to 248–376 dp,
  retains its square aspect, omits ZXing's internal quiet-zone modules on this
  screen only, and has exactly 12 dp of app-owned white padding around the
  encoded matrix. The caption begins 1 dp below that technical surface. The
  public-key target-to-QR relationship gap is 16 dp. At compact width the
  content uses the shared 16 dp margin; the entire composition scrolls for
  large text and short windows.
- The QR technical surface uses the theme's 16 dp `large` corner shape rather
  than the 28 dp `extraLarge` container shape. The smaller radius keeps the
  white frame soft without visually rounding the QR matrix itself.
- Name, optional verified address and public key remain one identity cluster.
  The verified address uses the official 20 dp filled rounded `verified` seal.
  Name and address use their native line metrics without an added gap; the
  public-key target begins 4 dp below the address. The copy capsule is visibly
  240 dp wide and 32 dp high at default text scale, using 16 dp horizontal and
  8 dp vertical content padding around `bodySmall` text and a 16 dp copy/check
  icon. The full public key fills the remaining width and receives native
  middle ellipsis inside the capsule, rather than rendering a pre-shortened
  fixture string. It is centered inside a transparent 48 dp semantic target.
  The target owns input, while its shared interaction source renders the
  bounded state layer only in the visible capsule's clipped shape.
  The QR is the only literal white/black surface so it stays theme-independent
  and reliably scannable.
- The Profile route uses the shared 16 dp compact-screen margin. In read mode
  its tonal fields remain enabled-looking and selectable but use the native
  read-only state; the verified seal occupies the address field's native
  trailing-icon slot. Edit mode keeps the same geometry; its
  only persistent task action is the shared 56 dp Save button on a zero-tonal-
  elevation `surface` bottom slot, so no footer band appears against the page.
- A full-content-width standard primary **Scan QR Code** button is pinned to the
  bottom of the active pane with the shared 16 dp action inset plus the device
  navigation safe area. It keeps the shared 56 dp task height and official
  24 dp scanner icon. It opens the
  shared 94% near-full scanner sheet with its existing handle/header, rounded
  target, permission, torch, system Back and swipe-dismiss metrics. Dismissal
  returns to the stable identity page.

## Donate

- Lightning and Bitcoin use Material 3 Expressive's connected button-group
  pattern in the center-aligned app-bar title slot: two `ToggleButton`s with
  the library's connected leading/trailing shapes, native selection motion and
  radio semantics. Donate does not draw a legacy segmented control or add a
  redundant page title beside the selector.
- The donation QR reuses `IdentityQrCodeSurface` exactly. The technical surface
  is 81% of the active pane width clamped to 248–376 dp, the encoded matrix has
  no encoder-owned margin modules, and the app adds exactly 12 dp of literal
  white padding per edge inside the theme's 16 dp `large` corner shape.
- The donation address reuses `IdentifierCopyCapsule` exactly. Its visible pill
  is 240 × 32 dp at default text scale inside a transparent 48 dp target, with
  16 dp horizontal and 8 dp vertical content padding, a 16 dp copy/check icon,
  full-value middle ellipsis, a state layer clipped to the pill and two-second
  copied feedback. Donate must not introduce local QR or capsule geometry.
- QR-to-address-target spacing remains the Donate-specific compact 1 dp
  relationship. The method caption uses `bodyMedium` and is pulled 4 dp into
  the transparent bottom portion of the unchanged 48 dp copy target, leaving
  5 dp between the visible 32 dp capsule and the caption line box. This reduces
  the caption's perceived padding without shrinking or moving the copy target.
  It is a Donate-specific optical correction, not a general layout token.

## Web-image task sheet

- Close and Done use native compact app-bar controls, not 56 dp form actions.
  Done's outer trailing clearance is 16 dp, including the app bar's native
  4 dp action-edge inset. Its internal content padding remains Material-owned.
- Search/URL uses the Photo Picker's filled-tonal navigation-button pattern:
  theme medium shape (12 dp), equal widths, 16 dp screen margins, an 8 dp gap,
  and native compact button/48 dp minimum target metrics.
- Privacy and input use 16 dp peer spacing. Input-to-results and
  supporting-text-to-preview-heading use 24 dp section spacing; preview heading
  to image uses 8 dp. The dense, square three-column grid keeps 2 dp media
  gutters, a component-specific exception to the general 4/8 dp layout rhythm.
- The modal owns safe-area/IME insets once. Do not add status-bar padding to
  its nested app bar or another keyboard padding layer to its content.
  Scroll the privacy/input/results body instead of trapping fixed controls
  above a zero-height grid when space is constrained.

## Chats and shared scrolling headers

- Chats and New Group share one compact contextual search component: its
  rounded field is 48 dp high at default scale and uses `bodyLarge`, rather
  than inheriting an app-bar title style. This is a user-approved contextual
  exception to Material's 56 dp standalone SearchBar token. New Message uses
  the standard 56 dp filled field because search is a primary standalone page
  control there. Back and Clear remain native 48 dp icon-button
  targets. The field uses Material's text-field decoration with zero vertical
  content inset at default scale; placeholder, input, cursor and icons share
  the vertical center and remain inside the container. The 48 dp value is a
  minimum, so accessibility font scaling can grow the field instead of
  clipping text. Focus, IME, single-line input, clear, and Back are unchanged.
- Conversation search reuses this exact compact component: 48 dp minimum,
  `bodyLarge`, the same tonal container, Search/Clear artwork, zero default-
  scale vertical inset and native growth. Its result controls apply the union
  of IME and navigation-bar clearance so the 48 dp arrow targets and localized
  count stay above the keyboard.
- The conversation identity title uses a 40 dp avatar and a 4 dp gap to the
  title/metadata column. A user-approved negative 2 dp arrangement between the
  native `titleMedium` and `labelSmall` line boxes optically reduces their
  visible leading by about 30%; it does not change either text style. The Row
  center-aligns the resulting block and avatar inside a transparent 48 dp-
  minimum touch target and allows the text block to grow at larger font scales.
  There is no visible container padding. Its absent press/ripple indication is
  the explicit WN-ANDROID-0094 exception; Button semantics and the Chat Info
  destination remain. Search is not a root conversation app-bar action.
- Search-match backgrounds use Android platform cyan, black foreground and a
  4 dp radius around exact per-line glyph bounds with no horizontal inset.
  Every matching message remains at full contrast; nonmatches use 38% alpha.
  This selected-flow color is not added to the app's general Material scheme.
- The sent-media viewer's bottom Share and Forward actions are transparent
  native 48 dp `IconButton` targets with 24 dp symbols, aligned to opposite
  edges above navigation-bar clearance. Do not expand them into labeled or
  filled equal-width buttons.
- Media-viewer top and bottom chrome share the opaque gallery `background`
  color. Their background extends through system-bar padding so full-height
  media cannot ghost through titles or controls.
- Chats toolbar and row avatars have 40 dp and 52 dp visible diameters. Their
  leading artwork edges share the 16 dp content margin inside the same capped
  680 dp pane. The toolbar uses an 8 dp relationship inset in addition to its
  native 4 dp edge and 4 dp touch-target/40 dp avatar difference. Native Back
  targets and conversation-title avatars do not inherit this avatar inset.
- Chat rows use the native clickable `ListItem` overload, with its 12 dp
  avatar-to-content gap (25% tighter than the former 16 dp baseline). An 8 dp
  outer anchor inset plus 8 dp inner horizontal padding preserves the 16 dp
  artwork/trailing-content margin in every state. Names are semibold; text
  line height, vertical padding, ripple, and accessibility growth retain
  Material's metrics. Preview has up to two lines. Timestamp aligns to the
  name baseline; read/failure/invitation indicators align with preview content.
- The single-line name and its mute/timer/ended-membership symbols form one
  leading group, with 4 dp gaps between them. Reserve an 8 dp minimum gap to
  the independently measured timestamp at the trailing edge. Measure the
  timestamp and symbols first; the name uses its natural width up to the
  remaining constraint and native ellipsis, so short names keep adjacent
  icons and long names never displace them. No character-count or screen-width cutoff;
  the full name stays in semantics and the layout mirrors in RTL.
- A scoped scrolling-crash workaround keeps all five text/status slots in
  `ChatRowTextLayout`, inside the native `ListItem` content slot, rather than
  querying inherited baselines on a supporting `Row`/`Box`. Read baselines
  only from direct `Text` children and place children only in the placement
  pass. Preserve alpha25's 72/88 dp two-/three-line minima (minus the native
  outer padding inside this text region), `ListItemDefaults.verticalAlignment`
  and unconstrained growth for larger text. The avatar gap, outer geometry,
  native interaction/shape and menu remain unchanged. This is a compatibility
  exception, not a new app-wide list component or arbitrary height system.
- Custom pin badges are 20 dp with 14 dp artwork. Noninteractive inline state
  icons are 16 dp; gaps use the 4/8 dp grid. The exclusive trailing status uses
  the native content-badge minimum of 16 dp. Its shared circular footprint is
  the greater of that minimum, the measured `labelSmall` line height, and a
  single digit plus Material's 4 dp text padding on each side. Manual unread is
  a solid primary circle with that footprint, never the native 6 dp empty
  badge. Invitations use this same primary circle with the official Add
  symbol in `onPrimary`, centered as an overlay so icon/text padding cannot
  widen the circle. Multi-digit/99+ badges may expand horizontally. The unchanged error
  XML has an 800/960-unit circle; 1.2× artwork scale fits its visible circle to
  the same slot and uses `error`. Failure suppresses the unread display only.
- Long press uses `WhiteNoiseDropdownMenu`, the shared native Expressive menu
  described below. The highlighted row uses `surfaceContainerHigh` and the
  native list selected shape (large/16 dp corners); no padding changes on
  selection. Its popup anchor follows the inset row. Chats adds 8 dp of
  transparent vertical padding outside the menu group, separating its surface
  both below and above the row while preserving native fitting/fallback and
  start alignment in RTL. Other dropdowns keep their existing spacing. No
  horizontal chat gesture or custom swipe-action metrics remain.
- New Message uses the native 56 dp FAB, native large rounded-square shape,
  elevation, a 24 dp rounded pencil and primary/onPrimary. Scaffold owns the
  16 dp end/bottom inset, safe area and Snackbar clearance; a pane-only inset
  aligns it within the same capped 680 dp list on expanded windows.
- `WhiteNoiseScaffold` owns each destination's pinned Material scroll behavior.
  `WhiteNoiseTopBar` accepts it; actual lazy-list/grid/scroll state also updates
  the content offset after restoration/programmatic changes. Rest is `surface`;
  under-scroll is `surfaceContainer`. Empty/disposed scrollers reset the state.
  Camera, media, and contextual-selection headers keep specialized styling.
- Chats does not inset the list viewport above the bottom system area. Safe
  bottom clearance plus 88 dp (56 dp FAB + two 16 dp gaps) belongs to scrolling
  content padding, reduced to 16 dp when search hides the FAB.

## App-owned Material menus

- All app-owned popup entry points use `WhiteNoiseDropdownMenu`: native
  `DropdownMenuPopup`, one `DropdownMenuGroup`, and the new command/selected
  item overloads, not the baseline menu overloads.
- Use `MenuDefaults.groupShapes` and index-aware `itemShape` with no local
  corner radius, width, row-height, typography or padding override. Official
  icon XML uses `MenuDefaults.LeadingIconSize` (20 dp in the pinned release),
  not the unrelated 24 dp toolbar metric. Material retains minimum targets
  and font-scaling behavior.
- Standard group color is `surfaceContainerLow`. Selected choices use native
  standard selection colors, shapes, radio semantics and a leading check.
  Destructive commands retain `error` text/icon roles; disabled colors remain
  native. Do not apply a global high-surface override to new menus.
- When a modal already owns placement and dismissal, use the shared
  `WhiteNoiseMenuGroup` rather than rebuilding menu rows. The focused-message
  command surface uses this embedded variant, so its index-aware rounded state
  layers, clipping, 48 dp minimum targets, 20 dp icons, typography, padding,
  colors and elevation exactly match Chats filtering. Do not add a local
  `Surface`, row `clickable`, corner radius, icon size or pressed fill there.
- The popup owns focus/motion/RTL/Back/outside dismissal. The composer-only
  bottom-edge exception uses the popup's public position-provider contract to
  attach the group 2 dp above its trigger and preserve a lower-edge transform
  origin; other menus retain Material's adaptive position provider. A regular
  `verticalScroll` inside the group makes tall menus reachable while retaining
  the group's native clipping. No fixed menu dimensions are introduced.
- WN-ANDROID-0040 pins Material 3 alpha25 for API 23 compatibility; see
  `docs/screens/app-menus.md` for the implementation and API-status audit.

## Diagnostics

- The **Events** / **Live** header uses the console's 16 dp inner content line:
  both edges are inset one `FormField` from the console surface, matching the
  event text and divider endpoints instead of the surface's outer bounds.
- The centered Diagnostics app bar keeps a native 48 dp trailing `IconButton`
  with the official 24 dp vertical-dots symbol. It anchors
  `WhiteNoiseDropdownMenu`; optional **Copy Diagnostic Summary**, **Test**, and
  **Clear Events** retain the shared menu's native 20 dp leading icons, item
  targets, shapes, colors, focus, placement, dismissal and scrolling. Clear is
  disabled while the console is empty. No duplicate command row appears in
  content.
- The Events header pairs an 18 dp official cell-tower/radiowave symbol with
  **Live** using a 6 dp gap. The symbol uses semantic success green and a
  900 ms 0.42-to-1 alpha pulse with reverse repeat. Its visible Live text and
  merged **Live event stream** description are persistent, so motion and color
  only reinforce state; Compose animation duration follows the system scale.
- The console remains the one flexible-width, flexible-height 16 dp-corner
  technical surface below the header. Removing the exposed action row gives
  that console the reclaimed height at every window size and font scale.

## Key Packages

- **Publish New Key Package** uses the shared full-width 56 dp
  `WhiteNoiseFilledTonalButton`, inset 16 dp from the active pane and separated
  24 dp from the current-package explanation. Its official 24 dp package
  symbol and label are one native button target with Material-owned shape,
  padding, state layer, focus and semantics. It is neither a white-equivalent
  settings row nor a pinned page-completion action.
- The consequence begins 8 dp below the button on the shared 32 dp helper
  content line. Publishing still replaces exactly one deterministic package;
  presentation does not introduce progress, networking or persistence.

## Ordinary Material sheets

- `WhiteNoiseModalBottomSheet` uses one continuous `surfaceContainer`:
  #EFEFEF in light mode, #1E1E1E in dark. This one-step-stronger neutral makes
  the modal surface visibly gray over its scrim while staying in the same
  semantic Material hierarchy as the Settings canvas. Ordinary `ListItem` containers are
  transparent; intentional selected/tonal groups, fields and errors remain.
- Material owns width, top corners, handle, motion, drag and dismissal. Its
  native 32×4 dp handle and 22 dp vertical padding already form a 48 dp slot.
  Do not add another top spacer. The modal owns safeDrawing/IME insets once.
- `WhiteNoiseSheetHeader`: `titleLarge`, directional 24 dp text insets and
  an 8 dp gap to the body, no top padding. Titles wrap. Existing Close actions
  use trailing native IconButtons (48 dp targets), not a leading imitation of
  an iOS sheet control. Task sheets with Close/Done retain a native app bar,
  matching the sheet color, with zero repeated window insets.
- Forwarding uses the shared 48 dp-minimum compact search and one 16 dp-inset
  destination group. Its rows use positional segmented shapes and the
  established 2 dp sheet-canvas separator. Selection preserves each row's
  shape/fill and adds one trailing 24 dp check while the whole row retains
  Checkbox semantics. The fixed title/search region uses
  `surfaceContainerLow` at rest and the one-step-darker `surfaceContainer`
  after the destination list scrolls; this color is applied to Material's
  rounded sheet cap and drag-handle area as well as the title/search content,
  while the destination canvas remains `surfaceContainerLow`. Media
  forwarding places its optional one-to-four-line message in a chat-matched
  48 dp-minimum, 24 dp-radius capsule with a trailing 48 dp action target and
  32 dp filled arrow. The capsule overlays the scrolling list without a footer
  surface. End padding includes its measured height, its relationship spacing,
  and the system bottom inset with a 24 dp minimum, keeping the final
  destination fully clear of the capsule and gesture area. The destination
  viewport itself excludes only horizontal/top safe drawing insets, so its
  canvas and scrolling rows reach the physical bottom edge; the floating
  capsule independently applies navigation-bar padding. Forwarding may expand
  to a high 88%-of-available-height modal state while retaining Material's
  rounded sheet cap rather than becoming a full-screen destination.
- Chats and Chat Info share a native immediate-choice Mute alert dialog with
  scrollable duration rows and Cancel. Ordinary short choices no longer need
  a large separate mute sheet. Specialized camera/media and system UI are
  excluded from these surface/header rules.
- The first-login diagnostics prompt keeps the header's directional 24 dp
  content line for its intro, switch labels/trailing controls and privacy copy.
  Each 56 dp switch target has an 8 dp transparent outer inset, clips its
  native Material state layer to `MaterialTheme.shapes.large`, and uses 16 dp
  internal horizontal padding. The combined 8 + 16 dp geometry preserves the
  shared 24 dp grid while preventing pressed feedback from becoming an
  edge-to-edge rectangular band.

### Immediate-choice dialogs

- `MuteDurationDialog` uses Material `AlertDialog` and one selectable radio
  group in its text slot. It does not nest Material `ListItem`, so the first
  native radio target begins on the dialog title/text content line instead of
  adding a second 16 dp list inset.
- Each duration is one 56 dp-minimum whole-row radio target. The native radio
  and label have the standard 16 dp relationship; the row clips hover, focus,
  pressed and ripple feedback to `MaterialTheme.shapes.large` before applying
  selection. That clipped surface expands 16 dp beyond the dialog content line
  on each side, leaving an 8 dp gutter against the default alert-dialog edge,
  then restores 16 dp internal padding so the radio and label do not move. The
  selected duration remains visible and exposed through radio semantics when
  the dialog has a current value.

### Destructive task sheets

- Sign Out starts expanded and exposes only Expanded and Hidden anchors. It
  keeps Material-owned sheet motion/dismissal while idle and rejects every
  transition to Hidden during named progress.
- The active identity and wipe choice are peer rows in one 16 dp-inset
  `surfaceContainerLowest` group with the established 2 dp
  `surfaceContainerLow` divider. The profile row uses Material's current
  12 dp leading-content relationship.
- The dynamic wipe consequence and exact-name confirmation helper use the
  shared settings helper geometry: 32 dp from the pane edge, 8 dp below their
  related group or field, with no helper-owned bottom padding. The destructive
  field keeps a persistent Material label above its input.
- The pinned 56 dp destructive action uses `surfaceContainerLow` at zero tonal
  elevation so it reads as one continuous gray task canvas, not a raised or
  differently colored footer.

## New Message and person profiles

- New Message, New Group, Set Up Group, Person Profile and Groups in Common
  use the Settings-detail color hierarchy: `surfaceContainerLow` canvas and
  `surfaceContainerLowest` groups. New Message's persistent people search is a
  standard 56 dp filled field; New Group reuses the compact 48 dp-minimum
  contextual field used by active Chats search.
- People directories stay lazy and use the current interactive Material
  `ListItem` overload. Its native 12 dp leading-content relationship replaces
  the deprecated overload's 16 dp avatar-to-text gap, matching Chats even with
  creation's smaller 48 dp avatar. New Message and New Group rows each form
  one 16 dp-inset, white-equivalent segmented group with rounded first/last
  rows, native 16 dp internal content padding, and a 2 dp canvas-tone gap
  between adjacent rows. New Group's compact search keeps its 48 dp minimum
  and 16 dp outer margin but uses the same `surfaceContainerLowest` fill as
  the people group. Selected New Group rows retain the exact same positional
  segmented shape and fill as their resting state; only the trailing vector
  check and selected semantics change. Set Up Group uses the same 16 dp-inset
  segmented group and 2 dp separators for its read-only member review. Every
  variant retains native measurement, text slots, state layer and font-scale
  growth.
- New Message does not reserve the bottom safe area outside its lazy viewport.
  The viewport reaches the screen edge; the safe bottom plus 24 dp section
  clearance belongs to scroll content so the final person remains reachable.
- Selected New Group members use 64 dp avatars inside 80 dp-wide removable
  identity tiles. A 24 dp filled close badge is visual feedback; the entire
  tile is the Button-semantic remove target, so the badge never becomes an
  undersized independent control.
- Person Profile shares the Share & Connect 32%-of-pane avatar clamp
  (104–152 dp), 20 dp filled verified seal, 240 dp visible npub capsule inside
  its 48 dp target, complete source value and native middle ellipsis. The name
  retains `headlineSmall`/semibold and sits 16 dp below the avatar; an available
  About group begins another 16 dp below the name. About uses
  `surfaceContainerHigh` with centered italic `bodyLarge`/
  `onSurfaceVariant`, while action groups remain `surfaceContainerLowest`.
  About and action groups retain 16 dp outer margins; independent groups are
  24 dp apart and labeled section text follows the 32 dp content line.
- Continue, Create Group, Message and relay recovery use the shared 56 dp
  primary task button in a zero-elevation bottom slot. Its resting
  `surfaceContainerLow` changes to `surfaceContainer` whenever the shared
  tracked scroll state overlaps content, matching the settings top app bar.
  Set Up Group reuses the 120 dp `ProfileAvatar` and native default-height
  tonal Add/Change Photo button already used by Sign Up and Profile. The empty
  avatar uses the existing 40 dp group symbol until a name or photo is present.
  Group fields need no duplicate section title; member rows form one quiet
  white-equivalent group under the Members heading. Material
  bottom sheets own Add-to-Group width, shape, handle, motion, insets, focus,
  Back and dismissal.

## Notifications

- The detail page retains the Settings `surfaceContainerLow` canvas and
  `surfaceContainerLowest` groups. Local notifications and its dependent
  Native push switch remain whole-row Material switch targets in one group,
  separated by the accepted two-dp canvas-tone gap.
- Preview is an inline accessible radio group on the existing Notifications
  subscreen, not another dialog. Sender and message, Sender only and New
  message only use native `SettingsChoice` measurement, transparent resting
  and selected containers, radio state, and two-dp inter-row gaps. The radio
  control alone communicates selection rather than a full-width gray block.
- A final noninteractive native list row shows the deterministic preview for
  the selected choice. Concise helper copy sits outside the group. When Local
  notifications is off, Native push, every preview choice and the example use
  Material disabled semantics/colors while explaining the dependency.
- Notification access is an app-wide platform gate above the profile-owned
  groups. Before Android 13+ has recorded a choice, one grouped action requests
  the native `POST_NOTIFICATIONS` prompt. When access is blocked, the same slot
  becomes a grouped **Notifications are off** disclosure into app-specific
  Android notification settings. Granted access removes the gate; there is no
  permanent Android section.
- Permission loss visually turns Local notifications off and disables its
  row. Native push, every Preview choice and its example follow the resulting
  dependency chain. Returning from the system prompt or Settings refreshes the
  gate without adding notification delivery, channels, push, or persistence.

## Appearance and language

- Appearance retains the standard Settings detail canvas and begins with the
  shared section-heading rhythm for Theme. Its three native radio rows use
  transparent resting and selected containers plus the shared two-dp
  canvas-tone separators; radio state alone communicates selection.
- The theme explanation uses the shared Settings explainer inset immediately
  below the group. Language follows as one white-equivalent disclosure group,
  showing the active preference as a trailing value instead of repeating all
  language controls on Appearance.
- Language is a separate typed route. Its white-equivalent choice group begins
  one shared section interval below the app bar, then uses native radio-row
  measurement and two-dp separators for all eight accepted choices. No
  screen-local iOS checkmark alignment, card metric, or navigation control is
  introduced.

## Profile Keys

- The route uses the Settings hierarchy: `surfaceContainerLow` canvas,
  `surfaceContainerLowest` grouped surfaces, 16 dp compact/24 dp expanded
  screen margins, and 24 dp between key sections.
- Public/private values use monospaced one-line text in a weighted 64 dp key
  row. The complete stored value is composed with middle ellipsis at the
  measured action boundary rather than shortened before layout. A hidden
  private key clips its repeated mask glyphs at that boundary and never adds
  an ellipsis that could be mistaken for part of the masked value. Official
  24 dp copy and visibility artwork keeps 48 dp controls; 16 dp start and
  visual end edges come from 16 dp start plus a 4 dp row end inset around the
  centered action artwork. Private-value accessibility remains state-only.
- Supporting copy starts on the shared 32 dp group-content line and sits 8 dp
  below its related group. Export rows share a tonal group with the standard
  subtle divider inset; there are no screen-width export buttons.
- Material `AlertDialog` owns dialog width, shape, spacing, actions, Back,
  outside dismissal, focus, IME movement, and large-text adaptation. Ordinary
  dialog inputs do not opt out of the app-wide form language: the encrypted-
  export form uses `WhiteNoiseSecureTextField` with the shared 28 dp rounded
  white-equivalent container, attached labels, and an 8 dp field rhythm on the
  neutral dialog canvas. Both fields fill and align to Material's dialog text
  slot. Low/Fair/Strong feedback uses the pinned iOS thresholds and a
  three-step Material progress indicator. Material still owns secure editing,
  focus, IME behavior, state animation, and error semantics; no local dialog
  dimensions or iOS sheet metrics are introduced.

## Chat and Group Info

- Root info screens share the Settings `surfaceContainerLow` canvas and a
  back-only Material app bar. Chat Info/Group Info remains the accessible pane
  name. Expanded panes retain the existing AdaptiveContent bound.
- Identity reuses Person Profile's 32%-of-pane avatar clamp (104–152 dp),
  `headlineSmall` semibold name and 16 dp avatar/name relationship. Direct
  identity shares its verified-address and full-key copy components. Group
  description precedes member count, with 8 dp related spacing.
- Tonal quick actions fill equal-width slots at the established 56 dp height,
  with 8 dp between controls and before wrapping `labelLarge` captions. The
  row keeps 16 dp outer margins and native button states/touch semantics.
- SettingsSection supplies 32 dp heading alignment and 24/8 dp section
  spacing. Native interactive ListItem rows use `surfaceContainerLowest`,
  positional segmented shapes, 2 dp canvas gaps and 16 dp group margins.
  Member rows remain lazy and use the established 48 dp avatar. Management
  and final lifecycle groups have 24 dp separation. These metrics do not
  change Edit Group or Chat Relays.

## Governing Android sources

- [Grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)
- [Content composition and structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Material 3 inset handling](https://developer.android.com/develop/ui/compose/system/material-insets)
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
- [Material TextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldDefaults)
- [Material OutlinedTextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextFieldDefaults)
- [Material text-field label positions](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldLabelPosition)
- [Material Button defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonDefaults)
- [Material menu specifications](https://m3.material.io/components/menus/specs)
- [Photo Picker navigation buttons (AOSP)](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/main/photopicker/src/com/android/photopicker/features/navigationbar/NavigationBar.kt)

## Read Aloud transport

The shared transport uses the existing surfaceContainerHigh, adaptive content
bound, native 48 dp minimum icon controls, wrapping text actions and 16 dp outer
inset. Native components own state and measurement. A scrollable transport has a
45%-of-window height budget (at least two touch targets); a full reader's combined
transport/selection actions have a 55% budget, retaining scrollable reading space
in short windows and at large type. Use current window dimensions, not physical
screen metrics. The app shell reserves transport space and owns its navigation
bar/IME padding; descendants consume those insets once. There is no overlay on
top of the composer and no new permanent speech command inside message bubbles.
