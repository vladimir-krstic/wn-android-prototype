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

- Application ID: `dev.ipf.whitenoise.android.prototype`
- Namespace: `dev.ipf.whitenoise`
- Minimum SDK: 23
- Compile and target SDK: 37
- Android Gradle Plugin: 9.3.1
- Gradle: 9.7.0
- Kotlin: 2.4.10 using AGP built-in Kotlin support
- Compose BOM: 2026.08.00
- Google Code Scanner: 16.1.0, with transitive network permissions removed
- AndroidX ExifInterface: 1.4.2
- ZXing core: 3.5.4 for local QR encoding
- One `app` module and one launcher activity

The runtime is intentionally offline and deterministic. It declares no
network, storage, camera, microphone, notification, or location permission.

Implementation status: Batches 0–9 are complete at the static verification
gate. The app includes the complete accepted onboarding, profile, Chats,
creation, conversation, composer, media, speech simulation, interaction,
search, information, membership, relay, Settings, support, developer, and
destructive-flow surface. Batch 9 adds relay consequence hardening, private-key
export safety, notification dependencies, adaptive/RTL/large-font coverage,
resource integrity, and native adaptive/monochrome launcher assets.

The final clean gate passed on 2026-08-15 with 89 unit tests, 39 compiled
Compose instrumentation tests, zero lint issues, and both APKs. Device and
visual acceptance are reserved for the later screen-by-screen polish pass.

## Repository map

- `AGENTS.md` — project authority, platform boundaries, workflow, and
  completion rules.
- `.agents/skills/white-noise-android-prototype/` — repository-local skill for
  Android implementation and review work.
- `docs/decisions.md` — durable Android project decisions.
- `docs/handoff.md` — final architecture, system boundaries, known device
  backlog, and the ordered visual-polish sequence.
- `docs/product-language.md` and `docs/terminology.md` — product voice and
  canonical terms.
- `docs/port/` — feature inventory, parity process, and source-to-destination
  map, including the autonomous batch sequence.
- `docs/references/` — Android platform router, native-UI evaluation method,
  iOS snapshot provenance, and resource guidance.
- `docs/screens/` — Android screen briefs created only when a screen is
  selected.
- `reference/wn-ios-prototype-snapshot/` — read-only local snapshot of the
  iOS product docs, source, tests, and reusable resources.

## Source snapshot

The reference snapshot was captured from the sibling iOS working tree on
2026-08-15 at commit `58785a4724f33e23135c4dd3f98f231fca6a809d`.
It intentionally includes the current uncommitted edits listed in
`docs/references/ios-prototype.md` because those files represented the newest
available product behavior at capture time.

The snapshot is evidence, not an Android dependency. Production source must
not reference it at runtime or build time.

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

Run device tests only when an emulator or physical-device inspection has been
explicitly requested:

```bash
./gradlew connectedDebugAndroidTest
```

The debug APK is generated at
`app/build/outputs/apk/debug/app-debug.apk`. The instrumentation-test APK is
generated at
`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Continuing with visual polish

Use the ordered screen list and device checklist in `docs/handoff.md`. Select
one screen or bounded flow, compare it on the agreed Android reference device,
polish its hierarchy and interaction without regressing model parity, rerun the
static gate, and record visual evidence before marking it accepted.
