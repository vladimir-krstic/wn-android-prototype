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
- Gradle: 9.7.1
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

All app-owned visual-polish rollout batches passed the clean gate on
2026-08-21 with 94 unit tests, 74 compiled Compose instrumentation tests, zero
lint issues, and both APKs. Ordinary form fields now share the approved 28 dp
tonal treatment, 16 dp label/input/supporting alignment, and 2 dp focus/error
rings. Device execution and visual acceptance remain pending.

## Repository map

- `AGENTS.md` — project authority, platform boundaries, workflow, and
  completion rules.
- `.agents/skills/white-noise-android-prototype/` — repository-local skill for
  Android implementation and review work.
- `docs/decisions.md` — durable Android project decisions.
- `docs/handoff.md` — final architecture, system boundaries, known device
  backlog, and the ordered visual-polish sequence.
- `docs/visual-polish.md` — approved quiet Material 3 Expressive system,
  component migration, rollout batches, and visual acceptance gates.
- `docs/product-language.md` and `docs/terminology.md` — product voice and
  canonical terms.
- `docs/port/` — feature inventory, parity process, and source-to-destination
  map, including the autonomous batch sequence.
- `docs/references/` — Android platform router, native-UI evaluation method,
  pinned iOS provenance, and resource guidance.
- `docs/screens/` — Android screen briefs created only when a screen is
  selected.

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

Use the foundation-first plan in `docs/visual-polish.md` and the device
checklist in `docs/handoff.md`. The shared foundation and bounded Chats and
Settings root batch are implemented at the static gate. Inspect them on the
agreed Android reference device before treating them as the final visual
reference, then polish one later screen or tightly related flow at a time
without regressing model parity.
