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
| Closely related controls or actions | 8 dp | `Related` |
| Peer fields in one form group | 16 dp | `FormField` |
| Separate content groups or an inline primary action after fields | 24 dp | `Section` |
| Pinned bottom task-action inset | 16 dp | `PinnedActionInset` |

Use `WhiteNoiseSpacing` from `ui/theme/WhiteNoiseSpacing.kt`; do not duplicate
these values as screen-local magic numbers. Constrained/adaptive panes may add
outer space at larger widths, but the compact margin remains 16 dp inside the
active pane.

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

- All six popup entry points use `WhiteNoiseDropdownMenu`: native
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
- The popup owns position/focus/motion/RTL/Back/outside dismissal. A regular
  `verticalScroll` inside the group makes tall menus reachable while retaining
  the group's native clipping. No fixed menu dimensions or custom edge logic.
- WN-ANDROID-0040 pins Material 3 alpha25 for API 23 compatibility; see
  `docs/screens/app-menus.md` for the implementation and API-status audit.

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
