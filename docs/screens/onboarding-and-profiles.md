# Onboarding and profile foundation

2026-08-26 shared-sheet follow-up: `AvatarWebImagePicker` uses
`WhiteNoiseModalBottomSheet` for the common surfaceContainer/inset owner.
Its native Close/Done task app bar already matches that color and uses zero
window insets; task height, mode buttons, privacy copy, grid spacing and image
selection are unchanged. Profile switching uses the shared wrapping
titleLarge header with trailing Close. The specialized camera sheet and all
system-owned pickers retain their own presentation.

## Purpose

Port and visually unify the first-launch and Add Profile paths: Welcome, Sign
In, the Android QR scanner contract, Sign Up, avatar selection, deterministic
profile creation, and profile switching. Completion reaches the signed-in
Chats root.

## Scope and non-goals

Included:

- initial and Add Profile variants of Welcome, Sign In, and Sign Up;
- safe prototype private-key validation and two-second cancelable progress;
- app-owned CameraX and bundled on-device ML Kit Private Key QR entry with
  just-in-time camera permission, wrong-content, cancellation, and unavailable
  recovery;
- Android Photo Picker, system document picker, deterministic bundled web
  image catalog, URL preview, removal, and bounded image preparation;
- canonical Marmota, Pebble, Open Circuit, and showcase profile identities;
- profile-owned state, active-profile selection, and a Material profile
  switcher sheet;
- verified-address model/validation primitives assigned only after profile
  creation.

Not included:

- the complete Chats list, Settings hub, editable Profile destination, account
  persistence, real authentication, key decoding, cryptography, networking,
  remote image loading or camera capture outside the selected Private Key QR
  scanner;
- final visual acceptance or emulator inspection.

## Parity contract

- Initial Sign In activates canonical Marmota. Initial Sign Up updates
  Marmota's editable name, About, and avatar while preserving its identity.
- Add Profile Sign In activates Open Circuit. Add Profile Sign Up creates
  canonical Pebble and commits its editable values.
- The first completed Add Profile flow also exposes the five deterministic
  showcase profiles and makes the newly added profile active.
- Marmota and Pebble receive their deterministic verified addresses only when
  their profile objects are created. Sign Up never displays an address field.
- Private-key validation trims input and accepts only lowercase values that
  begin with `nsec`; no length check, decoding, or authentication occurs.
- The fixed Paste fixture is fictional, has the accepted 63-character shape,
  and never enters logs, diagnostics, visible plain text, or accessibility
  labels.

## Android navigation and presentation

- Android uses full-screen Navigation Compose destinations for Welcome,
  Sign In, and Sign Up. This intentionally replaces iOS onboarding sheets:
  system Back, predictive Back, and IME resizing are more idiomatic and less
  fragile on Android. Private Key scanning is instead an immediately expanded
  near-full Material modal bottom sheet owned by Sign In. The underlying form
  remains visible under the scrim, and swipe, system Back, or Close dismisses
  the scanner without changing its in-memory draft.
- Initial Welcome is the root and has no app bar. Add Profile Welcome has a
  small top app bar with Back and the title **Add Profile**.
- Welcome centers the mark between the top safe area's bottom edge and the
  actual top edge of Sign In; Add Profile uses the bottom of its top app bar
  as the upper boundary. The mark uses 50% of the safe width, capped at 260 dp,
  with its original 598:460 aspect ratio and shrinks to fit short windows with
  16 dp vertical breathing room. This user-approved proportion replaces the
  former splash-matched size and full-window center. Android's system splash
  remains platform-sized, while the native bottom actions keep their existing
  dimensions, gap, and safe bottom placement.
- Welcome respects system bars and display cutouts, but not IME insets: it has
  no editor and must not move its actions or mark above the outgoing form's
  keyboard. Sign In/Sign Up's app-bar Back clears text focus and requests
  keyboard dismissal before popping; Navigation Compose retains its standard
  transition and Android still owns keyboard-first system Back behavior.
- Sign In and Sign Up use fully rounded 28 dp Material-based tonal fields with
  persistent labels above the container. `surfaceContainerHigh` owns the
  resting boundary; focus and error use 2 dp full-shape semantic rings while
  disabled fields use `surfaceContainerLow`. Labels, input, and supporting
  text align to the same 16 dp directional content line. Both screens use a
  top app bar with system Back and vertically scrolling content. Sign In and
  Sign Up keep their full-width primary actions in the Scaffold bottom slot,
  above navigation-safe and IME insets. Sign Up's form remains independently
  scrollable behind that reserved action region; its pinned app bar changes
  tone when content scrolls beneath it.
- Sign In groups its three Private Key entry methods as one control cluster:
  manual entry in the field, Paste/Clear in the field's trailing icon slot,
  and a matching 56 dp tonal QR scanner icon button beside the field with an
  8 dp related-control gap. The 40 dp visible trailing control stays inside
  its 48 dp Material touch target and uses 4 dp horizontal optical padding,
  making its state-layer circle concentric with the 56 dp field cap while
  adding separation from the secure mask and cursor. The label and
  supporting/error text remain owned by the field.
- Authentication and profile forms remain centered and bounded to 520 dp at
  wider sizes instead of stretching across the available pane. Their pinned
  task actions use the same bound while remaining full width on compact
  phones.
- Full-width onboarding task actions use Material's medium 56 dp container
  height with 24 dp horizontal content padding. Compact-screen content and
  actions align to 16 dp horizontal margins; Welcome actions are separated by
  8 dp. Peer form fields use 16 dp, while the avatar/form and other distinct
  sections use 24 dp. Sign Up uses a 120 dp avatar and 16 dp
  scrolling-content top/bottom insets; Scaffold's measured bottom slot keeps
  the final field reachable above the action and keyboard.
- Completion reaches the current Chats root. Its avatar opens Settings, where
  the active-profile header owns the Material profile switcher and **Add
  Profile** entry. Selecting a profile is immediate; **Add Profile** dismisses
  the switcher and pushes the Add Profile Welcome flow.

## Exact product copy

Retain all accepted iOS copy for Welcome, Sign In, validation, Sign Up, photo
sources, web-image Search/URL modes, privacy disclosures, progress, and photo
failure. Android-only scanner failure detail is:

- **QR Scanning Unavailable**
- **QR scanning isn’t available on this device right now.**
- **Try Again**

User-approved Android loading copy is:

- **Signing In…**
- **Creating Profile…**

## Component and capability choices

- Material 3 medium task buttons, fully rounded tonal text fields, fully
  rounded tonal secure text field, top app bars, dialogs, tonal mode buttons, dropdown menu, lazy list/grid, progress
  indicator, Snackbar, and modal bottom sheet. Find Image on Web opens directly
  as a near-full sheet capped at 94% of the available height, retaining visible
  underlying context, rounded top corners, drag handle, and scrim. It uses a
  center-aligned top app bar, close icon button, contained Done action, and
  equal-width Search/URL mode buttons. These use the Photo Picker's public
  `FilledTonalButton` composition: the theme's 12 dp medium shape, primary
  selected fill, neutral unselected fill, standard button content/target
  metrics, an 8 dp gap, and 16 dp outer margins. They expose tab roles and
  selected state without adding a second click target. This replaces secondary
  underline tabs for this task only; Material tabs remain valid elsewhere.
  Done stays a compact native filled button, with a total 16 dp directional
  trailing inset rather than the app bar's icon-oriented 4 dp edge inset.
  The title inherits native app-bar typography and ellipsizes on one line.
  The full-window modal and scrim remain unconstrained; the cap belongs to the
  measured sheet content so its motion,
  system-bar treatment, and bottom coverage remain Material-owned. The sheet,
  handle area, app bar, and mode row use one `surfaceContainer` background
  so the task reads as one continuous modal surface. Material owns the sheet's
  safe drawing and keyboard insets; the nested app bar does not add another
  status-bar inset. The default handle, sheet shape, width cap, and gestures
  remain unchanged.
- Web Search uses one lazy grid with a full-span privacy/input header, so
  controls can scroll out of the way in a short window or above the keyboard.
  A 24 dp section gap separates the input and first image row. The three-column
  square grid retains its 2 dp media gutters and no extra enclosing card.
  URL input and preview share a vertical scroll surface with the same section
  separation. Privacy copy and fields use 16 dp peer spacing/margins, while
  the preview heading and image use an 8 dp related gap. Search has an explicit
  Search IME action and labeled Clear search icon; switching modes preserves
  query, URL, selection, and scroll position without keeping hidden input focus.
  Picker-local input mode and selected image ID use saveable state. The local
  restoration test does not establish full host-screen/process restoration.
- Sign In uses the secure field's Material trailing-icon slot for a standard
  Paste icon when empty and Clear icon after entry. A separate 56 × 56 dp
  `FilledTonalIconButton` with the standard QR scanner symbol sits immediately
  beside the field. The icon-only actions reduce form height while their
  proximity communicates that manual entry, paste, and scanning produce the
  same value; the tonal scanner remains subordinate to the pinned primary
  Sign In action.
- Ordinary onboarding form labels use Material's `TextFieldLabelPosition.Above`
  and align to the same 16 dp content line as input text or leading icon
  artwork. The stronger neutral tonal container is the resting boundary;
  full-shape rings appear only for focus and error.
  The web-image Search Images and Image URL inputs use the same treatment
  because they are explicit form inputs inside a bounded task. App-level search
  fields and message composers keep their specialized native patterns.
- Sign Up keeps its photo action attached to the avatar group with an 8 dp gap
  and uses a compact filled-tonal pill. The action remains visually contained
  without inflating it to the 56 dp task-button size.
- Sign Up's primary action uses the same persistent, bounded bottom slot as
  Sign In. Navigation-bar and IME padding are applied once to that slot; the
  form consumes Scaffold's measured content padding rather than adding a
  second IME inset. This keeps the action anchored and the focused field
  scrollable above it.
- While Sign In or Sign Up is processing, its primary task button blocks
  duplicate activation but retains the semantic `primary`/`onPrimary` color
  pair. A compact indeterminate indicator and stable visible progress label
  replace the resting label without changing the button's 56 dp container.
  Material's gray disabled treatment remains reserved for an action that is
  genuinely unavailable.
- `ActivityResultContracts.PickVisualMedia` for one image and
  `ActivityResultContracts.OpenDocument` for a file-owned image.
- Photo Picker and Files retain their platform or OEM appearance without app
  color overrides.
- CameraX 1.6.1 owns the back-camera preview, lifecycle, focus, zoom, and torch.
  Sign In presents it inside a Material modal bottom sheet immediately at its
  maximum state and caps the sheet content at 94% of the available height. The
  camera fills the complete sheet and is clipped only by its rounded boundary.
  A centered standard drag handle and transparent center-aligned top app bar
  share the compact top region over the preview, with Close, title, and
  flashlight; a restrained top scrim preserves contrast without detached
  control capsules. The title inherits the app bar's Material typography
  without a screen-local size override and uses single-line ellipsis when
  space is constrained; compact header positioning remains unchanged.
  Bundled ML Kit Barcode Scanning 17.3.0 analyzes QR codes fully on device
  without model download. One 3 dp white target uses 24 dp rounded corners
  instead of the former chamfers, Google colors, or double stroke.
- `CAMERA` is requested only after **Scan QR Code** is selected. Denial keeps
  the person in the scanner with **Allow Camera** recovery; permanent denial
  offers **Open Settings**. Android's camera privacy indicator remains
  system-owned. The later Share & Connect polish reuses this same approved
  app-owned scanner with profile-specific title, permission copy and result
  validation; it does not duplicate the camera implementation.
- Bundled deterministic web results are never fetched. Search and URL preserve
  the accepted production-facing privacy consequence while the prototype maps
  valid input to a local image.
- Chosen images are decoded off the main thread, scaled to at most 512 pixels,
  and compressed before entering process-local state. Cancellation or failure
  preserves the previous valid avatar.
  The initial size-only decode checks the stream and populated dimensions,
  not its deliberately null bitmap result; valid images continue to the
  sampled decode, EXIF orientation, resize, and compression stages.

## Accessibility, adaptation, and privacy

- The White Noise mark is announced once as **White Noise**.
- The secure field owns password semantics; the raw key is not added to custom
  semantics, labels, test output, or logs.
- All icon-only or image-only actions have explicit labels. The secure field
  action exposes **Paste private key** or **Clear private key** as its state
  changes; the adjacent scanner exposes **Scan QR Code**. Avatar catalog items
  expose their subject, selected state, and a visual check, so color is not the
  only cue.
- Fields retain persistent labels, IME actions, error semantics, and disabled
  states. Loading buttons expose **Signing In…** or **Creating Profile…** and
  **In progress**; progress is communicated by text and state semantics as
  well as motion.
- Button and photo-preparation indicators use compact geometry within their
  existing layout. Photo-preparation failures are announced as a polite live
  update and remain visible in the semantic error role.
- Every ordinary Material color role, including the newer surface-container
  and fixed roles, is explicitly grayscale in both themes. Semantic error
  roles remain red and appear only for actual failures or destructive meaning.
- Scroll containers, safe drawing/IME insets, bounded widths, font scaling,
  RTL, keyboard, mouse, touch, and minimum Material targets remain supported.
- The scanner title remains visible text, icon-only close/torch actions have
  state-specific labels, permission recovery is not color-only, and the raw
  scanned Private Key is never exposed to semantics, logs, or copy.

## Governing sources

- `docs/references/android.md`: Navigation Compose, Material 3, text input,
  bottom sheets, Photo Picker, Activity Result APIs, CameraX, ML Kit barcode
  scanning, permissions, insets, accessibility, and testing.
- [Configure Compose text fields](https://developer.android.com/develop/ui/compose/text/user-input)
- [Icon buttons in Compose](https://developer.android.com/develop/ui/compose/components/icon-button)
- [Material Symbols](https://developers.google.com/fonts/docs/material_symbols)
- [Authentication and onboarding](https://developer.android.com/design/ui/mobile/guides/patterns/onboarding)
- [Progress indicators](https://developer.android.com/develop/ui/compose/components/progress)
- [TextFieldLabelPosition](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldLabelPosition)
- [OutlinedTextFieldDefaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextFieldDefaults)
- [Android accessibility foundations](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Android grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)
- [Android content composition](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Material 3 inset handling](https://developer.android.com/develop/ui/compose/system/material-insets)
- [Window inset types](https://developer.android.com/develop/ui/compose/system/insets)
- [Software keyboard control](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/SoftwareKeyboardController)
- [BitmapFactory decode options](https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inJustDecodeBounds)
- [Android splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [CameraX](https://developer.android.com/media/camera/camerax)
- [CameraX ML Kit Analyzer](https://developer.android.com/media/camera/camerax/mlkitanalyzer)
- [ML Kit barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android)
- [Request runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Material 3 Button defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonDefaults)
- [Photo Picker navigation buttons (AOSP)](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/main/photopicker/src/com/android/photopicker/features/navigationbar/NavigationBar.kt)
- [Material tabs](https://developer.android.com/develop/ui/compose/components/tabs)
- [Lazy grids, full-span items, and content spacing](https://developer.android.com/develop/ui/compose/lists)

## iOS parity evidence

- `wn-ios-prototype@0bd7cba:docs/screens/welcome.md`
- `wn-ios-prototype@0bd7cba:docs/screens/login.md`
- `wn-ios-prototype@0bd7cba:docs/screens/qr-scanner.md`
- `wn-ios-prototype@0bd7cba:docs/screens/sign-up.md`
- `wn-ios-prototype@0bd7cba:docs/screens/verified-nostr-address.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/WhiteNoisePrototypeApp.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeProfile.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototypeTests/ProfileLifecycleTests.swift`
- onboarding SwiftUI source and bundled avatar resources routed through
  `docs/port/source-map.md`.

## Observable acceptance criteria

- Successful initial Sign In, Sign Up, and Add Profile schedule the profile's
  optional **Help Improve White Noise** sheet. It appears only after the Chats
  entry reaches `RESUMED`, never over the outgoing keyboard/form transition.
  Both switches start off for new profiles; actual dismissal records the
  prompt as seen, while rotation does not. See
  `diagnostics-and-improvements.md` for exact copy, ownership, and coverage.

- Welcome contains only the mark, **Sign In**, and **Sign Up**; both actions
  are at least 56 dp high.
- On Welcome, the mark's center is halfway between the top safe area's bottom
  edge (or Add Profile app bar) and Sign In's top edge. Its width is half the
  safe content width on phones, capped at 260 dp on wider layouts. Short
  windows shrink the logo without distorting it, overlapping actions, or
  shrinking their touch targets; Android's system splash remains unchanged.
- Back returns from Sign In/Sign Up to the correct initial or Add Profile
  Welcome without committing state.
- Returning while a form editor is focused dismisses its keyboard without
  lifting Welcome's action group or moving its logo during the transition.
  Welcome's bounds remain the same under changing IME insets; system bars and
  cutouts continue to be respected.
- Sign In and Sign Up form content and task actions do not exceed 520 dp on
  expanded layouts, while staying full width within compact 16 dp margins.
- Sign Up's 120 dp avatar, Name, and About share one scroll surface beneath a
  separate pinned primary action. With the IME visible, no field is
  permanently occluded; the action remains above the keyboard and the top app
  bar exposes its Material scrolled tonal state.
- QR entry is a semantically labeled 56 × 56 dp tonal icon button adjacent to
  the Private Key field; it remains available whenever Sign In is idle.
- Selecting QR entry opens an immediately expanded near-full Material sheet
  over the still-visible Sign In screen. Its live preview fills the sheet with
  no inner card or outer gutter; a Material drag handle and top app bar own the
  controls, and one white rounded-corner target has no inner or branded-color
  stroke.
- Camera permission is requested only from the selected scanner task. Denial,
  permanent denial, missing camera, Back/close/swipe dismissal, valid QR, and
  wrong-content QR all have a visible deterministic exit or recovery path.
- Paste/Clear occupies the field's Material trailing-icon slot, uses a standard
  vector symbol, exposes an action-specific accessibility label, and preserves
  deterministic empty/populated transitions. Its 40 dp visible state layer is
  inset evenly from the 56 dp field cap while retaining a 48 dp touch target
  and clear separation from the secure mask and cursor.
- Empty/invalid/valid private-key states, Paste/Clear, loading, scanner result,
  wrong QR, cancellation, and unavailable recovery behave deterministically.
- Sign In and Sign Up loading buttons retain their primary container and
  contrasting content while rejecting duplicate activation; unavailable
  non-loading buttons continue to use Material disabled colors.
- Both initial onboarding paths reach Chats with exactly one active canonical
  profile.
- Photo Picker, Files, web Search, web URL, selection, removal, loading, and
  failure states preserve the last valid draft and commit with Name/About.
- Find Image on Web keeps a 16 dp trailing inset for Done, native 48 dp minimum
  control targets, equal-width selected-state mode buttons with an 8 dp gap,
  and 24 dp between input/supporting text and results/preview. Close and Done
  stay reachable while Search/URL content scrolls at large font sizes and short
  heights. The handle stays below the status/cutout safe area with the IME open;
  no nested app-bar status padding is added. Selection, URL validation,
  clear/search keyboard actions, and restoration have regression coverage.
- Valid PNG and JPEG content URIs pass the bounds-only decode, prepare a
  correctly oriented image with a longest edge of at most 512 pixels, and
  replace the avatar. Invalid image data still follows the existing failure
  path without clearing the previous avatar.
- **Add Photo** and **Change Photo** are compact filled pills 8 dp below the
  avatar, and ordinary fields and containers have no pink or lavender cast.
- Name, About, and Private Key are fully rounded tonal fields with persistent
  labels aligned to their input content. Focus, error, and disabled states
  remain distinguishable without relying on the fill alone.
- Compact onboarding margins are 16 dp, peer form-field gaps are 16 dp,
  related-action gaps are 8 dp, distinct-section gaps are 24 dp, and Material
  continues to own internal component spacing.
- Add Profile Sign In and Sign Up create distinct canonical identities, expose
  the showcase profiles, reach Chats, and support immediate switching.
- Profile, validator, catalog, navigation, and state-transition tests pass;
  the complete clean static batch gate passes with zero lint issues.

2026-08-26 menu follow-up: Sign Up/Add Profile photo-source dropdowns use
`WhiteNoiseDropdownMenu` and official icons with native Expressive metrics.
Dismiss before opening Photos, Files or Web Image; preserve removal,
preparation cancellation and the existing draft/error lifecycle. No picker
appearance or onboarding behavior changes. See `app-menus.md` for the current
pin and verification (including its understood dependency-update warning).
