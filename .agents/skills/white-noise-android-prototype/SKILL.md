---
name: white-noise-android-prototype
description: Build, port, iterate, or review one agreed White Noise native Android prototype screen or bounded flow using Kotlin, Jetpack Compose, Material 3, AndroidX, pinned upstream iOS parity evidence, and current official Android guidance. Use in wn-android-prototype for Android UI implementation, navigation, product copy, accessibility, motion, adaptive layout, system integrations, tests, emulator inspection, or feature-parity review.
---

# White Noise Android Prototype

1. Read `AGENTS.md`, `docs/decisions.md`, and the selected Android screen
   brief. For any layout or visual work, also read `docs/ui-metrics.md`.
2. Read `docs/product-language.md` and `docs/terminology.md` whenever work
   touches copy, labels, errors, permissions, destructive actions, privacy, or
   accessibility wording.
3. Resolve parity evidence through `docs/port/source-map.md`. Consult the
   pinned upstream iOS baseline only when local Android records are
   insufficient, and read only the relevant brief, source, tests, and
   resources; do not port Apple implementation structure. Never treat newer
   upstream changes as Android scope without explicit user approval.
4. For material platform decisions or UI review, read
   `docs/references/native-ui.md`, route through
   `docs/references/android.md`, and open the relevant current official source.
5. Limit work to the user-agreed screen or tightly related flow. Do not prepare
   future screens or speculative architecture.
6. Preserve the product outcome, accepted copy, deterministic behavior, and
   state coverage. Re-express presentation with Kotlin, Compose, Material 3,
   AndroidX, Material Symbols, system Back, Android pickers, Android sharing,
   and Android permission behavior. Use standard Photo Picker and Files
   contracts without app color overrides; leave these system-owned surfaces
   under platform or OEM control.
7. Keep the White Noise palette monochrome through semantic Material roles.
   Preserve Android feedback and state semantics; do not flatten error,
   warning, disabled, focused, pressed, or selected states into gray.
8. Start with the closest standard component and interaction. Let it own
   metrics, state layers, touch targets, accessibility, and motion. Document
   why any custom control or gesture is necessary.
9. Create or update one concise brief in `docs/screens/` only after the user
   selects the screen. Include parity behavior, Android composition, Back
   behavior, system integrations, important states, adaptive behavior,
   accessibility, official sources, pinned iOS evidence paths, and acceptance
   criteria.
10. Add fixed in-memory data and capability wrappers only when the selected
    flow needs them. Keep backend, networking, cryptography, authentication,
    and persistence out of scope unless the user expands it.
11. Use lightweight host-side validation during iteration. Build and run
    relevant host tests at the end of a meaningful batch. Do not use `adb`, run
    connected instrumentation, open an emulator, install or launch the app on
    a device, interact with a device, or capture device screenshots unless the
    user's current request explicitly asks for device/emulator testing or
    visual/hands-on inspection. Earlier requests and an already connected phone
    are not standing authorization. If a device-only check is important,
    explain the need and wait for the user to request it. Compiling the
    instrumentation-test APK remains host-side and does not authorize executing
    it.
12. Update `docs/port/feature-inventory.md` only with concrete Android
    implementation evidence. Never claim visual verification without
    inspecting the current build.

Stop and ask only when an unresolved decision would materially change the
product experience or authorize a new external capability.
