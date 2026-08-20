# Official Android reference index

Last reviewed: 2026-08-20.

Use this file as a router. Open only the sources relevant to the selected
screen, confirm that the guidance is still current, and record material links
in that Android screen brief. Official Android/Google sources are live
authority; this index does not freeze library versions.

## UI foundations

- [Jetpack Compose overview](https://developer.android.com/develop/ui/compose)
- [Compose UI architecture and unidirectional data flow](https://developer.android.com/develop/ui/compose/architecture)
- [Android UI-layer architecture](https://developer.android.com/topic/architecture/ui-layer)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
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
- [Canonical adaptive layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts)
- [Edge-to-edge design](https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge)
- [Window insets in Compose](https://developer.android.com/develop/ui/compose/system/insets)
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
- [Bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
- [Menus](https://developer.android.com/develop/ui/compose/components/menu)
- [Search bar](https://developer.android.com/develop/ui/compose/components/search-bar)
- [Snackbar](https://developer.android.com/develop/ui/compose/components/snackbar)

## Lists, input, gestures, and state

- [Lists and grids](https://developer.android.com/develop/ui/compose/lists)
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input)
- [Focus behavior](https://developer.android.com/develop/ui/compose/touch-input/focus)
- [Understand Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)
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
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result)
- [CameraX overview](https://developer.android.com/media/camera/camerax)
- [CameraX image capture](https://developer.android.com/media/camera/camerax/take-photo)
- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner)
- [ML Kit barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android)
- [Android Sharesheet](https://developer.android.com/training/sharing/send)
- [Secure file sharing](https://developer.android.com/training/secure-file-sharing)

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
Gradle Plugin 9.3.1, Gradle 9.7.0, compile/target SDK 37, minimum SDK 23,
Kotlin 2.4.10 through AGP built-in Kotlin, Compose BOM 2026.08.00, Activity
Compose 1.13.0, Lifecycle 2.11.0, Navigation Compose 2.9.8, AndroidX Test
1.7.0, JUnit extension 1.3.0, and Espresso 3.7.0. Re-check this set before an
upgrade rather than treating it as a permanently current recommendation.

Batch 1 additionally verified Google Code Scanner 16.1.0 and AndroidX
ExifInterface 1.4.2. The scanner's transitive `INTERNET` and
`ACCESS_NETWORK_STATE` manifest entries are deliberately removed to preserve
the offline prototype boundary.

Batch 7 additionally verified ZXing core 3.5.4 for local, standards-compliant
QR generation. It is an encoder-only JVM dependency here and adds no Android
component or permission.

## Maintenance

- Re-check links and platform behavior before material decisions.
- Update the review date only after opening the relevant live sources.
- Do not copy large excerpts. Record the local decision and link to the source.
- Prefer official Android, AndroidX, Material, Kotlin, or Google capability
  documentation over blogs and remembered examples.
