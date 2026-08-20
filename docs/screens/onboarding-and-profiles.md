# Onboarding and profile foundation

## Purpose

Port the first-launch and Add Profile paths: Welcome, Sign In, the Android QR
scanner contract, Sign Up, avatar selection, deterministic profile creation,
and profile switching. Completion reaches the signed-in Chats root that Batch
2 will replace with the full chat hub.

## Scope and non-goals

Included:

- initial and Add Profile variants of Welcome, Sign In, and Sign Up;
- safe prototype private-key validation and two-second cancelable progress;
- permissionless Google Code Scanner QR entry with wrong-content,
  cancellation, and unavailable recovery;
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
  remote image loading, custom camera UI, or a camera permission;
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
  system Back, predictive Back, IME resizing, and the scanner's Google-owned
  surface are more idiomatic and less fragile on Android.
- Initial Welcome is the root and has no app bar. Add Profile Welcome has a
  small top app bar with Back and the title **Add Profile**.
- The initial Welcome mark is centered in the full launch window at the exact
  149.5 × 115 dp visible size used inside Android's centered splash-icon canvas.
  Bottom actions respect safe drawing insets independently and never reposition
  the mark as the splash is dismissed.
- Sign In and Sign Up use Material outlined fields with persistent labels above
  the container and a subtle `surfaceContainerLow` fill. Material continues to
  own focus, error, disabled, outline, and accessibility states. Both screens
  use a top app bar with system Back, vertically scrolling content, and a
  full-width bottom action above safe drawing and IME insets.
- Full-width onboarding task actions use Material's medium 56 dp container
  height with 24 dp horizontal content padding. Compact-screen content and
  pinned actions align to 16 dp horizontal margins; Welcome actions are
  separated by 8 dp. Peer form fields use 16 dp, while the avatar/form and
  other distinct sections use 24 dp.
- The signed-in placeholder is production-shaped: **Chats**, an active-profile
  avatar action, and a genuine empty state. It contains no prototype or
  implementation language and is replaced in Batch 2.
- The profile switcher is a Material modal bottom sheet. Selecting a profile
  is immediate; **Add Profile** dismisses the sheet and pushes the Add Profile
  Welcome flow.

## Exact product copy

Retain all accepted iOS copy for Welcome, Sign In, validation, Sign Up, photo
sources, web-image Search/URL modes, privacy disclosures, progress, and photo
failure. Android-only scanner failure detail is:

- **QR Scanning Unavailable**
- **QR scanning isn’t available on this device right now.**
- **Try Again**

The temporary signed-in empty state uses:

- **Chats**
- **No conversations yet**
- **Start a new conversation when you’re ready.**
- **Switch Profile**
- **Add Profile**

## Component and capability choices

- Material 3 medium task buttons, outlined text fields, outlined secure text
  field, top app bars, dialogs, tabs, dropdown menu, lazy list/grid, progress
  indicator, Snackbar, and modal bottom sheet.
- Ordinary onboarding form labels use Material's `TextFieldLabelPosition.Above`
  rather than cutting into the outline. The slight neutral container surface is
  supplementary; the standard Material outline remains the field boundary.
  Specialized search fields and message composers keep their native component
  patterns rather than inheriting this form treatment.
- Sign Up keeps its photo action attached to the avatar group with an 8 dp gap
  and uses a compact filled-tonal pill. The action remains visually contained
  without inflating it to the 56 dp task-button size.
- `ActivityResultContracts.PickVisualMedia` for one image and
  `ActivityResultContracts.OpenDocument` for a file-owned image.
- `play-services-code-scanner` 16.1.0 for a Google-owned, permissionless,
  QR-only scanner with auto zoom. No custom camera viewport or camera
  permission is present.
- Bundled deterministic web results are never fetched. Search and URL preserve
  the accepted production-facing privacy consequence while the prototype maps
  valid input to a local image.
- Chosen images are decoded off the main thread, scaled to at most 512 pixels,
  and compressed before entering process-local state. Cancellation or failure
  preserves the previous valid avatar.

## Accessibility, adaptation, and privacy

- The White Noise mark is announced once as **White Noise**.
- The secure field owns password semantics; the raw key is not added to custom
  semantics, labels, test output, or logs.
- All icon-only or image-only actions have explicit labels. Avatar catalog
  items expose their subject, selected state, and a visual check, so color is
  not the only cue.
- Fields retain persistent labels, IME actions, error semantics, and disabled
  states. Loading buttons expose **Signing In** or **Signing Up** and
  **In progress**.
- Every ordinary Material color role, including the newer surface-container
  and fixed roles, is explicitly grayscale in both themes. Semantic error
  roles remain red and appear only for actual failures or destructive meaning.
- Scroll containers, safe drawing/IME insets, bounded widths, font scaling,
  RTL, keyboard, mouse, touch, and minimum Material targets remain supported.

## Governing sources

- `docs/references/android.md`: Navigation Compose, Material 3, text input,
  bottom sheets, Photo Picker, Activity Result APIs, Google Code Scanner,
  insets, accessibility, and testing.
- [Configure Compose text fields](https://developer.android.com/develop/ui/compose/text/user-input)
- [TextFieldLabelPosition](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldLabelPosition)
- [OutlinedTextFieldDefaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextFieldDefaults)
- [Android accessibility foundations](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Android grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)
- [Android content composition](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Android splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner)
- [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Material 3 Button defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonDefaults)

## iOS parity evidence

- `reference/wn-ios-prototype-snapshot/docs/screens/welcome.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/login.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/qr-scanner.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/sign-up.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/verified-nostr-address.md`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/WhiteNoisePrototypeApp.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeProfile.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototypeTests/ProfileLifecycleTests.swift`
- onboarding SwiftUI source and bundled avatar resources routed through
  `docs/port/source-map.md`.

## Observable acceptance criteria

- Welcome contains only the mark, **Sign In**, and **Sign Up**; both actions
  are at least 56 dp high.
- On initial launch, the splash and Welcome marks are both centered at
  149.5 × 115 dp, so dismissal produces no logo resize or position cut.
- Back returns from Sign In/Sign Up to the correct initial or Add Profile
  Welcome without committing state.
- Empty/invalid/valid private-key states, Paste/Clear, loading, scanner result,
  wrong QR, cancellation, and unavailable recovery behave deterministically.
- Both initial onboarding paths reach Chats with exactly one active canonical
  profile.
- Photo Picker, Files, web Search, web URL, selection, removal, loading, and
  failure states preserve the last valid draft and commit with Name/About.
- **Add Photo** and **Change Photo** are compact filled pills 8 dp below the
  avatar, and ordinary fields and containers have no pink or lavender cast.
- Name, About, and Private Key are outlined fields with persistent labels above
  the outline and a slight neutral surface. Focus, error, and disabled states
  remain distinguishable without relying on the fill alone.
- Compact onboarding margins are 16 dp, peer form-field gaps are 16 dp,
  related-action gaps are 8 dp, distinct-section gaps are 24 dp, and Material
  continues to own internal component spacing.
- Add Profile Sign In and Sign Up create distinct canonical identities, expose
  the showcase profiles, reach Chats, and support immediate switching.
- Profile, validator, catalog, navigation, and state-transition tests pass;
  the complete clean static batch gate passes with zero lint issues.
