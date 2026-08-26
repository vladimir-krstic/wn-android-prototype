# Official Android reference index

Last reviewed: 2026-08-26.

Use this file as a router. Open only the sources relevant to the selected
screen, confirm that the guidance is still current, and record material links
in that Android screen brief. Official Android/Google sources are live
authority; this index does not freeze library versions.

## UI foundations

- [Jetpack Compose overview](https://developer.android.com/develop/ui/compose)
- [Compose UI architecture and unidirectional data flow](https://developer.android.com/develop/ui/compose/architecture)
- [Android UI-layer architecture](https://developer.android.com/topic/architecture/ui-layer)
- [Compose design systems](https://developer.android.com/develop/ui/compose/designsystems)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material 3 Expressive research](https://design.google/library/expressive-material-design-google-research)
- [Material components in Compose](https://developer.android.com/develop/ui/compose/components)
- [Interactive component guides](https://developer.android.com/develop/ui/compose/quick-guides/collections/display-interactive-components)
- [Material Design 3](https://m3.material.io/)
- [Material color system](https://m3.material.io/styles/color/overview)
- [Material typography](https://m3.material.io/styles/typography/overview)
- [Material motion](https://m3.material.io/styles/motion/overview)
- [Material Symbols](https://developers.google.com/fonts/docs/material_symbols)

## Layout, adaptation, and system UI

- [Grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)
- [Content composition and structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Adaptive apps](https://developer.android.com/develop/adaptive-apps)
- [Adaptive do's and don'ts](https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts)
- [Window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)
- [Canonical adaptive layouts](https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts)
- [Edge-to-edge design](https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge)
- [Window insets in Compose](https://developer.android.com/develop/ui/compose/system/insets)
- [Set up window insets](https://developer.android.com/develop/ui/compose/system/insets-ui)
- [Material 3 inset handling](https://developer.android.com/develop/ui/compose/system/material-insets)
- [Android system bars](https://developer.android.com/design/ui/mobile/guides/foundations/system-bars)
- [Keyboard/IME animation with insets](https://developer.android.com/develop/ui/compose/system/ime-keyboard)
- [Splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Support different languages and cultures](https://developer.android.com/training/basics/supporting-devices/languages)
- [Support right-to-left layouts](https://developer.android.com/training/basics/supporting-devices/languages#SupportRTL)

## Navigation and presentation

- [Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation)
- [Navigation design](https://developer.android.com/design/ui/mobile/guides/layout-and-content/navigation)
- [Predictive Back design](https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back)
- [Add predictive Back support](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
- [App bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button)
- [Floating action buttons](https://developer.android.com/develop/ui/compose/components/fab)
- [Badges](https://developer.android.com/develop/ui/compose/components/badges)
- [Material icon XML guidance](https://developer.android.com/develop/ui/compose/graphics/images/material)
- [Bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
- [Menus](https://developer.android.com/develop/ui/compose/components/menu)
- [Current Material menu design](https://m3.material.io/components/menus/overview)
- [Current Material menu specifications](https://m3.material.io/components/menus/specs)
- [Material 3 releases and API graduation](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Menu defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/MenuDefaults)
- [Search bar](https://developer.android.com/develop/ui/compose/components/search-bar)
- [Snackbar](https://developer.android.com/develop/ui/compose/components/snackbar)

For the user-approved app-wide menu migration, use the actual new popup/group/
item family, not the baseline `DropdownMenu` illustrated in some Compose
guides. WN-ANDROID-0040 pins `material3:1.5.0-alpha25`: its Material/ripple AARs
retain API 23 support, while alpha26's ripple requires API 24. The design and
public API status are distinct from artifact release status. The exact
published source and compatibility evidence are linked in
`docs/screens/app-menus.md`; do not infer APIs from AndroidX main instead.

## Lists, input, gestures, and state

- [Authentication and onboarding](https://developer.android.com/design/ui/mobile/guides/patterns/onboarding)
- [Android settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Lists and grids](https://developer.android.com/develop/ui/compose/lists)
- [Switches](https://developer.android.com/develop/ui/compose/components/switch)
- [Radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button)
- [Tabs](https://developer.android.com/develop/ui/compose/components/tabs)
- [Pager layouts](https://developer.android.com/develop/ui/compose/layouts/pager)
- [Chips](https://developer.android.com/develop/ui/compose/components/chip)
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input)
- [Material TextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldDefaults)
- [Material OutlinedTextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextFieldDefaults)
- [Material text-field label positions](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldLabelPosition)
- [Material buttons](https://developer.android.com/develop/ui/compose/components/button)
- [Material Button defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonDefaults)
- [Progress indicators](https://developer.android.com/develop/ui/compose/components/progress)
- [Focus behavior](https://developer.android.com/develop/ui/compose/touch-input/focus)
- [Understand Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)
- [Tap and press gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press)
- [Handling interactions and state layers](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions)
- [Save UI state](https://developer.android.com/develop/ui/compose/state-saving)
- [State holders and UI state](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- [Lifecycle-aware state collection](https://developer.android.com/develop/ui/compose/state#lifecycles)

Prefer component-provided gestures and semantics before low-level pointer
input. Custom chat gestures must coexist with scrolling, selection, the IME,
system edge gestures, TalkBack, and predictive Back.

## Accessibility and quality

- [Android accessibility design](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility)
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing)
- [Touch-target size](https://support.google.com/accessibility/android/answer/7101858)
- [Compose testing](https://developer.android.com/develop/ui/compose/testing)
- [Test navigation](https://developer.android.com/guide/navigation/testing/compose)
- [Core app quality](https://developer.android.com/docs/quality-guidelines/core-app-quality)

Use visible text, role, state, and custom actions together. Do not add a content
description that merely repeats a visible label, and never put private keys in
the semantics tree.

## Photos, camera, files, and sharing

- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Photo Picker navigation-button implementation (AOSP)](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/main/photopicker/src/com/android/photopicker/features/navigationbar/NavigationBar.kt)
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result)
- [CameraX overview](https://developer.android.com/media/camera/camerax)
- [CameraX image capture](https://developer.android.com/media/camera/camerax/take-photo)
- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner)
- [ML Kit barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android)
- [Android Sharesheet](https://developer.android.com/training/sharing/send)
- [Secure file sharing](https://developer.android.com/training/secure-file-sharing)

Use standard Photo Picker and Storage Access Framework contracts without app
color overrides. Android and the device manufacturer own these surfaces'
colors and layout, along with camera, scanner, Sharesheet, and settings UI.

Use the permissionless Google Code Scanner when the selected QR flow needs only
a system-owned result and Google Play services is an accepted availability
requirement. Use CameraX plus barcode analysis only when an approved custom
camera experience or offline packaging requires it. QR generation has no
standard Android platform surface; choose and record an encoder only when
Share & Connect is selected.

## Media, audio, and speech

- [Android Camera and Media center](https://developer.android.com/media)
- [Media3 playback](https://developer.android.com/media/media3/exoplayer/hello-world)
- [Media3 Player interface](https://developer.android.com/media/media3/session/player)
- [Audio capture](https://developer.android.com/media/platform/mediarecorder)
- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)

The current prototype uses deterministic voice recording/transcription state.
Do not add microphone or speech-recognition permission merely because Android
provides the API. Use a real capability only after the selected brief confirms
that the product boundary changed.

## Permissions, privacy, identity, and notifications

- [App permissions best practices](https://developer.android.com/training/permissions/usage-notes)
- [Request runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Privacy best practices](https://developer.android.com/privacy-and-security/about)
- [BiometricPrompt](https://developer.android.com/identity/sign-in/biometric-auth)
- [Secure sensitive activities](https://developer.android.com/privacy-and-security/risks/screen-capture)
- [Notifications](https://developer.android.com/develop/ui/views/notifications)
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [App settings intents](https://developer.android.com/reference/android/provider/Settings)

Prototype preference screens do not automatically justify requesting a real
permission or registering a system service.

## Build, dependencies, and testing

- [Android Studio releases](https://developer.android.com/studio/releases)
- [Android Gradle Plugin release notes](https://developer.android.com/build/releases/gradle-plugin)
- [Kotlin support](https://developer.android.com/build/kotlin-support)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)
- [AndroidX releases](https://developer.android.com/jetpack/androidx/versions)
- [Build your first Android app](https://developer.android.com/codelabs/basic-android-kotlin-compose-first-app)
- [Test in Android Studio](https://developer.android.com/studio/test)

At bootstrap, select mutually compatible stable versions, record exact working
commands in the root README, and avoid alpha/beta dependencies unless a
selected requirement cannot be met with stable APIs and the user approves the
risk.

### Verified project baseline

The foundation was built and linted successfully on 2026-08-15 with Android
Gradle Plugin 9.3.1, and the current clean gate uses Gradle 9.7.1 with
compile/target SDK 37, minimum SDK 23,
Kotlin 2.4.10 through AGP built-in Kotlin, Compose BOM 2026.08.00, Activity
Compose 1.13.0, Lifecycle 2.11.0, Navigation Compose 2.9.8, AndroidX Test
1.7.0, JUnit extension 1.3.0, and Espresso 3.7.0. Re-check this set before an
upgrade rather than treating it as a permanently current recommendation.

Gradle 9.7.1 is the official 9.7 patch release recommended on 2026-08-21 and
passed the complete project gate without changing the Android plugin baseline.

Batch 1 originally verified Google Code Scanner 16.1.0 and AndroidX
ExifInterface 1.4.2. WN-ANDROID-0048 removed the scanner dependency after the
last system-scanner flow moved to the already bundled CameraX/ML Kit scanner;
the manifest continues to reject transitive `INTERNET` and
`ACCESS_NETWORK_STATE` entries to preserve the offline prototype boundary.

The Sign In device-polish pass additionally verified stable CameraX 1.6.1 and
bundled ML Kit Barcode Scanning 17.3.0 for the user-approved app-owned scanner,
now shared with Share & Connect profile scanning.
The bundled detector is immediately available and operates on device; only
the just-in-time `CAMERA` permission is added.

Batch 7 additionally verified ZXing core 3.5.4 for local, standards-compliant
QR generation. It is an encoder-only JVM dependency here and adds no Android
component or permission.

## Maintenance

- Re-check links and platform behavior before material decisions.
- Update the review date only after opening the relevant live sources.
- Do not copy large excerpts. Record the local decision and link to the source.
- Prefer official Android, AndroidX, Material, Kotlin, or Google capability
  documentation over blogs and remembered examples.
