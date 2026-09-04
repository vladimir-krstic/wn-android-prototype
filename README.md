# White Noise Android Prototype

This repository is the staged Android-native port of the White Noise iOS
prototype. The previous Android workspace was removed and the new application
was bootstrapped from a clean Kotlin/Compose foundation.

## Direction

- Preserve the iOS prototype's product features, copy, deterministic behavior,
  fixtures, and important states.
- Re-express them with Kotlin, Jetpack Compose, Material 3, AndroidX, public
  Android APIs, and Android navigation and system surfaces.
- Keep the White Noise visual identity monochrome—black, white, and neutral
  gray—without carrying over Apple-specific control geometry or interaction.
- Implement the port in the ordered batches in `docs/port/batches.md`, then
  inspect and polish one screen or small flow at a time with the user.

## Current Android baseline

Production Android parity batches B01–B18 are implemented and host-verified.
[B18 group setup and recovery](docs/screens/group-setup-images-and-roster.md#implementation-evidence)
adds solo groups, initial timer/open recovery, private/public and emoji images,
and authoritative roster/member progress with guarded retries.
The latest host gate passes 575 unit tests, zero lint errors and both APKs.
Ten new UI/bitmap cases compile only; device and visual acceptance remain pending.
Creation, uploads and roster operations remain deterministic in memory.
[The implementation plan](docs/audits/production-android-parity/implementation-plan.md)
tracks the remaining B19–B32 batches and per-batch commits.

- Application ID: `dev.ipf.whitenoise.android.prototype`
- Namespace: `dev.ipf.whitenoise`
- Minimum SDK: 23
- Java time compatibility: Android core-library desugaring 2.1.5
- Compile and target SDK: 37
- Android Gradle Plugin: 9.3.2
- Gradle: 9.7.1
- Kotlin: 2.4.10 using AGP built-in Kotlin support
- Compose BOM: 2026.08.00
- Material 3: 1.5.0-alpha25, a user-approved exception for official Expressive
  menus; this version retains API 23 support (alpha26's ripple requires API 24)
- CameraX: 1.6.1 for the app-owned Private Key and Share &amp; Connect scanners
- Bundled ML Kit Barcode Scanning: 17.3.0 for offline QR analysis
- AndroidX ExifInterface: 1.4.2
- AndroidX Media3: 1.11.0 for local video playback and standard Material 3
  player controls inside the sent-media viewer
- ZXing core: 3.5.4 for local QR encoding
- One `app` module and one launcher activity

The runtime is intentionally offline and deterministic. It declares camera
permission for the user-selected Private Key and Share &amp; Connect QR scanners.
Because Android also gates an external photo-capture intent when that manifest
permission is present, the scanners and the chat Camera action request access
only after the person selects them. On Android 13 and newer, the Settings
prototype also requests `POST_NOTIFICATIONS` from its explicit notification
access action; the app does not create or deliver notifications. It declares
no network, storage, microphone, or location permission.

Implementation status: Batches 0–9 are complete at the static verification
gate. The app includes the complete accepted onboarding, profile, Chats,
creation, conversation, composer, media, speech simulation, interaction,
search, information, membership, relay, Settings, support, developer, and
destructive-flow surface. Batch 9 adds relay consequence hardening, private-key
export safety, notification dependencies, adaptive/RTL/large-font coverage,
resource integrity, and native adaptive/monochrome launcher assets.

All app-owned visual-polish rollout batches passed the clean gate on
2026-08-21 with 94 unit tests, 74 compiled Compose instrumentation tests, zero
lint issues, and both APKs. Ordinary form fields now share the approved 28 dp
tonal treatment, 16 dp label/input/supporting alignment, and 2 dp focus/error
rings. Device execution and visual acceptance remain pending.

The 2026-08-26 Chats/privacy work uses scoped iOS `4c25393`: minimal Chats,
shared scrolled headers, and profile-owned first-login privacy choices plus
Diagnostics & Improvements settings. Its approved controls follow-up replaces
swipes with native anchored long-press menus (Signal `441ba42` behavior only),
restores an icon-only Material FAB, unifies status indicators, and standardizes
ordinary sheet surfaces/headers and the shared Mute dialog. The static gate
passes with 117 unit tests, 121 compiled instrumentation tests, zero lint
issues, and both APKs. Consent and diagnostic records remain in memory;
no telemetry, persistence, dependency upgrade or device execution was added.

The subsequent app-wide menu migration replaces all six baseline dropdowns
with Google's Expressive popup/group/item APIs through `WhiteNoiseDropdownMenu`.
The explicit Material pin above preserves the minimum SDK and existing Compose
BOM. The full static gate passes with 117 unit tests, 126 compiled
instrumentation tests and both APKs. Lint has zero errors and one expected
newer-version warning for that compatibility pin. No device execution was
performed. `docs/screens/app-menus.md` records implementation evidence and the
related audit: task-button typography/shape behavior and global motion still
use baseline choices, so the app is not yet a complete Expressive migration.

The 2026-08-31 repository-wide hardening pass removed lint-proven dead
resources, repaired lifecycle and restoration boundaries, tightened clipboard
privacy, canonicalized support and relay identity, corrected search and media
preparation edge cases, and made the connected-test harness trustworthy. The
current gate passes with 139 host unit tests, 168 instrumentation tests on a
Pixel 8a running Android 17, both APKs, zero lint errors, and six intentionally
deferred dependency-version warnings. Direct inspection also covered the
highest-impact chat/composer, creation, Settings, dark, 200%-type, RTL, and
610/838 dp-wide states. See `docs/codebase-hardening-audit.md` for exact
evidence, bounded decisions, and remaining user-acceptance work.

## Repository map

- `AGENTS.md` — project authority, platform boundaries, workflow, and
  completion rules.
- `.agents/skills/white-noise-android-prototype/` — repository-local skill for
  Android implementation and review work.
- `docs/decisions.md` — durable Android project decisions.
- `docs/handoff.md` — final architecture, system boundaries, known device
  backlog, and the ordered visual-polish sequence.
- `docs/visual-polish.md` — approved quiet Material 3 Expressive direction,
  component migration, rollout batches, and visual acceptance gates.
- `docs/product-language.md` and `docs/terminology.md` — product voice and
  canonical terms.
- `docs/port/` — feature inventory, parity process, and source-to-destination
  map, including the autonomous batch sequence.
- `docs/references/` — Android platform router, native-UI evaluation method,
  pinned iOS provenance, and resource guidance.
- `docs/screens/` — Android screen briefs created only when a screen is
  selected. The [production parity plan](docs/audits/production-android-parity/implementation-plan.md)
  tracks completed batches and their verification; [B09](docs/screens/message-editing-and-reading.md)
  covers message editing, full reading and native passage selection.
  [B10](docs/screens/message-moderation-and-forwarding.md) adds moderation, batch
  deletion recovery and forwarding progress.
  [B11](docs/screens/composer-attachment-actions.md) adds reversible photo quality,
  contact/media acquisition, transfer recovery and attachment save/share results.

## iOS parity reference

The accepted iOS parity baseline is the private
[`wn-ios-prototype`](https://github.com/vladimir-krstic/wn-ios-prototype)
repository at commit `0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`.
`docs/references/ios-prototype.md` records its provenance and access rules;
`docs/port/source-map.md` maps Android flows to relevant upstream paths.

The iOS repository is read-only parity evidence, not an Android dependency.
Production source must not reference it at runtime or build time, and newer
iOS commits do not expand Android scope without explicit approval.

## Build and verification

Run from the repository root:

```bash
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
```

The complete static batch gate, including a clean rebuild, is:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The commands above are the default agent validation boundary. Run connected
tests only when the user's current request explicitly asks for device or
emulator testing; prior inspection requests and an attached phone do not carry
forward as permission:

```bash
./gradlew connectedDebugAndroidTest
```

The connected-test command is documentation, not standing authorization to use
`adb`, install or launch the app, interact with a tethered phone/emulator, or
capture device screenshots. If a device-only check is needed, explain why and
wait for the user to request it.

The debug APK is generated at
`app/build/outputs/apk/debug/app-debug.apk`. The instrumentation-test APK is
generated at
`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Continuing with visual polish

Use the foundation-first plan in `docs/visual-polish.md` and the device
checklist in `docs/handoff.md`. The shared foundation and bounded Chats and
Settings root batch are implemented at the static gate. When the user
explicitly requests a device inspection, use the agreed Android reference
device before treating the result as a final visual reference. Otherwise keep
polish iterations within the host-side validation boundary.
