# White Noise Android Store Screenshot Prototype

A standalone, deterministic Jetpack Compose harness for five White Noise store
screenshots. It contains no backend, networking, persistence, authentication,
or Marmot bindings.

The screenshots use Android Material 3 structure with the product's
black-and-white visual direction:
`Scaffold`, top app bars, tonal cards, `ListItem` rows, badges, a modal bottom
sheet, a chat FAB, monochrome message colors, and neutral tonal surfaces.
The iOS prototype supplies content hierarchy, fixtures, and imagery only.

## Scenes

- `whitenoise-screenshots://scene/relays`
- `whitenoise-screenshots://scene/profile-switcher`
- `whitenoise-screenshots://scene/chats`
- `whitenoise-screenshots://scene/conversation`
- `whitenoise-screenshots://scene/share-connect`

Launching the app normally opens a development-only scene picker. Deep links
open the selected store scene directly.

## Build

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Pixel 10 Pro XL capture

Create the API 36 ARM64 emulator once:

```bash
./scripts/setup-pixel-10-pro-xl-avd.sh
```

Launch `WhiteNoise_Pixel_10_Pro_XL_API_36`, wait for it to finish booting, then:

```bash
./scripts/capture-store-screens.sh
```

The capture script installs the debug app, locks the device to light portrait
mode and font scale 1.0, enables a deterministic System UI demo state, and
writes 1344×2992 PNGs to `screenshots/raw/`.

To keep the running emulator clock fixed at 18:15 during manual review:

```bash
./scripts/lock-emulator-clock.sh
```

The clock lock uses Android System UI demo mode and should be reapplied after
rebooting the emulator. The capture script reapplies it automatically when it
finishes.

## Visual sources and assets

- Deterministic copy, fixture ordering, and local imagery:
  `/Users/vladimirkrstic/Workspaces/wn-ios-prototype`
- Material 3 component patterns, Manrope fonts,
  spacing/radius tokens, and launcher assets:
  `/Users/vladimirkrstic/Workspaces/wn-android`

All portraits and conversation media are copied locally from the iOS
prototype’s asset catalog. Their public-marketing provenance and rights
assumptions are documented in that prototype’s screen briefs. The source
projects are not modified by this harness.
