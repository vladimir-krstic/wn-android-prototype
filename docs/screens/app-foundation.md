# Native Android application foundation

## Purpose

Create the smallest buildable Android foundation needed for the staged White
Noise port. This batch establishes platform, navigation, theme, state, and test
conventions without implementing a product screen prematurely.

## Scope and non-goals

Included:

- Android app and Gradle configuration;
- one Compose activity and root navigation host;
- semantic monochrome light/dark themes;
- edge-to-edge and adaptive root behavior;
- accessible temporary start destination;
- deterministic root state ownership and baseline tests.

Not included:

- onboarding, Chats, Settings, or another product flow;
- production persistence, networking, authentication, cryptography, camera,
  media, QR, notifications, or backend integration;
- feature modules, dependency injection, repositories, or speculative shared
  abstractions;
- emulator inspection or visual acceptance.

## Parity contract

The app must be able to host the complete profile-owned deterministic product
state and route between onboarding and signed-in roots in later batches. Batch
0 does not copy an iOS screen; it preserves the source app's single coherent
application state and native navigation intent.

## Entry, navigation, Back, and exit

- `MainActivity` is the single launcher activity.
- A typed Navigation Compose graph owns destinations.
- Welcome replaced the temporary foundation destination in Batch 1 and remains
  the start destination.
- System Back and predictive Back remain owned by Android Navigation; the app
  does not intercept Back on the root placeholder.
- No deep links or exported product activities are added.

## Exact product copy

Retired Batch 0 developer-only surface:

- App name: **White Noise**
- Heading: **White Noise**
- Status: **Android foundation ready**

This status was removed when Batch 1 supplied the real Welcome screen.

## Android composition

- `ComponentActivity` with `setContent` and edge-to-edge enabled.
- Material 3 `MaterialTheme` with app-owned light/dark monochrome color
  schemes; dynamic color disabled.
- Navigation Compose `NavHost` with serializable typed routes where supported
  by the selected stable versions.
- `Scaffold` and a centered adaptive content container for the temporary
  destination.
- Android default sans-serif typography through the Material type scale.
- A native adaptive launcher icon uses a white background, a black foreground
  mark centered inside Android's 66 dp safe zone, and a mark-only monochrome
  layer for themed icons. The API 23–25 fallback preserves the same polarity.
- Android 12 and later use the exact White Noise Welcome mark, scaled inside
  the platform splash-icon safe area, on the semantic app background. The
  launcher differentiator mark is never reused as in-app or splash branding.

## Behavior and state

- Root state is deterministic and process-local.
- The foundation exposes no fake loading, account, or backend state.
- The placeholder survives activity recreation through the navigation stack;
  no business state requires persistence yet.

## System integrations

None.

## Accessibility and adaptation

- The heading has heading semantics; the status is ordinary readable text.
- No decorative image receives a content description.
- Content respects system bars and display cutouts.
- The centered content uses bounded width on expanded windows rather than
  stretching across the full display.
- Text supports font/display scaling and RTL without fixed heights.
- Light and dark schemes meet readable contrast with monochrome semantic roles.

## Governing Android sources

- `docs/references/android.md`: Compose, Material 3, Navigation Compose,
  edge-to-edge, adaptive apps, accessibility, testing, Compose BOM, and build
  tooling routes.
- `docs/references/native-ui.md`: standard-component and adaptation checks.
- [Android splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)

## iOS parity evidence

- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/WhiteNoisePrototypeApp.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype.xcodeproj/project.pbxproj`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototypeUITests/PerformanceUITests.swift`

## Approved differences and custom exceptions

None. Batch 0 is an Android-native foundation rather than a visible iOS port.

## Observable acceptance criteria

- The debug APK compiles from a clean Gradle invocation.
- Unit tests pass.
- The launcher activity renders the temporary accessible foundation state.
- The manifest exports only the launcher activity required by Android.
- The system splash packages the exact Welcome mark rather than the launcher
  differentiator mark and adapts its polarity in light and dark themes.
- Edge-to-edge is enabled and content applies the appropriate safe insets.
- No third-party runtime dependency, network permission, storage permission,
  or speculative feature architecture is present.
