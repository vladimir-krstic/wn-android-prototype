# White Noise Android Prototype

## Mission

Port the White Noise iOS prototype to a polished, idiomatic Android app one
user-agreed screen or tightly related flow at a time. Preserve product
capabilities, copy, deterministic behavior, and state coverage while
translating every presentation and interaction into native Android UI and UX.

The target is parity of product intent, not a pixel-for-pixel SwiftUI copy.

## Authority

Use this order when sources disagree:

1. The latest explicit user direction.
2. Approved local Android decisions in `docs/decisions.md`, shared UI metrics
   in `docs/ui-metrics.md`, product language in `docs/product-language.md`,
   terminology in `docs/terminology.md`, and the brief for the selected screen.
3. Accepted White Noise product behavior and copy captured in
   `reference/wn-ios-prototype-snapshot/` and summarized under `docs/port/`.
4. Current official Android design and developer documentation routed through
   `docs/references/android.md` and evaluated with
   `docs/references/native-ui.md`.
5. The current Android implementation.

When iOS and Android conventions differ, keep the product outcome and adopt
the Android convention. Record any user-approved exception in the selected
screen brief.

## Technical boundaries

- Use Kotlin, Jetpack Compose, Material 3, public Android platform APIs, and
  first-party AndroidX/Jetpack libraries.
- Prefer a single-activity Compose application and unidirectional data flow
  unless an implemented requirement demonstrates a better native boundary.
- Use Gradle Kotlin DSL and a version catalog. Pin versions only after checking
  current stable official releases; never guess version numbers from memory.
- Keep the product monochrome: black, white, and neutral grays expressed
  through semantic Material color roles. Do not enable dynamic color unless
  the user approves changing the White Noise identity.
- Do not imitate SwiftUI structure, iOS sheets, SF Symbols, Apple spacing,
  Apple navigation bars, or iPhone-only gestures. Map intent to Android
  components, Material Symbols, system Back, predictive Back, Android
  Sharesheet, Android pickers, and Android permission behavior.
- Launch Android Photo Picker and Files flows with their standard Activity
  Result contracts and platform-default appearance. Do not pass app color
  overrides or replace system-owned picker, camera, scanner, Sharesheet, or
  settings surfaces merely to force White Noise styling.
- Let standard Material and platform components own typography, touch-target
  size, padding, shape, state layers, focus, ripple, elevation, motion, and
  accessibility whenever a suitable component exists.
- Use the shared 4/8 dp layout rhythm and relationship-specific values in
  `docs/ui-metrics.md` for custom composition. Do not introduce screen-local
  spacing variants without a component requirement or explicit user direction.
- Draw edge to edge and handle system bars, display cutouts, gesture insets,
  the on-screen keyboard, and window-size changes correctly. Start
  phone-focused, but do not lock orientation or make the UI non-resizable.
- Keep layouts compatible with font scaling, display scaling, localization,
  right-to-left text, TalkBack, Switch Access, Voice Access, keyboard, mouse,
  and touch.
- Do not add third-party runtime libraries while a platform or first-party
  AndroidX/Google API meets the requirement. Any exception needs explicit user
  approval and a recorded decision.
- Preserve the current prototype boundary: deterministic in-memory product
  state and no backend, Nostr, Marmot, Rust, real authentication,
  cryptography, durable storage, or network access unless the user explicitly
  expands scope. A selected flow may use a real device-owned Android surface
  already present in iOS parity, such as camera capture, a picker, sharing, or
  media playback.

## Product and parity rules

- `docs/port/feature-inventory.md` is the parity ledger. Update its status only
  with implementation evidence; never mark a feature complete because a
  static screen resembles the source.
- The snapshot is read-only evidence. Never import Swift or Apple framework
  assumptions into production Android source.
- Product-surface copy must be production-ready. Never expose prototype,
  fixture, simulation, dummy-data, or implementation-boundary language outside
  developer-only surfaces.
- Retain the White Noise gray/black character without suppressing Android's
  semantic disabled, error, warning, success, focus, pressed, or selected
  states.
- Do not create speculative services, modules, repositories, data layers,
  screens, or abstractions. Introduce structure when the selected flow needs it
  or repeated implementation proves it useful.

## Screen workflow

1. Confirm the one screen or bounded flow the user selected.
2. Read its iOS brief and implementation paths through
   `docs/port/source-map.md`, plus any governing local decisions.
3. Create or update one concise Android brief in `docs/screens/` using
   `docs/templates/screen-brief.md`.
4. For a material UI or platform decision, open the relevant current official
   source from `docs/references/android.md` and apply
   `docs/references/native-ui.md`.
5. Define the parity behavior and observable states before translating the UI.
6. Start with the closest Material 3, Compose, AndroidX, or platform pattern.
   Add custom drawing or gesture handling only when the accepted product need
   cannot be expressed natively, and document the reason.
7. Implement the smallest complete flow with deterministic state.
8. Run lightweight static checks during iteration and the relevant build and
   tests at the end of a meaningful batch.
9. Launch an emulator or physical device, install the app, interact with it,
   or capture screenshots only when the user explicitly requests visual or
   hands-on inspection. Never claim visual verification without inspecting the
   current build.
10. Update the parity ledger with exact evidence and remaining gaps.

## Validation

- Once the Android project exists, keep the exact working build, lint, unit
  test, and instrumentation commands in `README.md`.
- Add unit tests for meaningful state transitions, derivations, and
  regressions. Add Compose UI tests for durable navigation, interaction, and
  accessibility behavior—not merely to inflate coverage.
- Exercise Android-specific behavior where relevant: system Back and
  predictive Back, state restoration after recreation, keyboard and insets,
  TalkBack semantics, large font and display sizes, light/dark themes, and at
  least compact and expanded window widths.
- Preserve unrelated user changes. Do not configure a remote, commit, push,
  or publish unless explicitly requested.

## Completion

A screen reaches implementation parity only when its behavior, states, copy,
accessibility, and Android-native interaction criteria pass. It is product- and
visually complete only after the user accepts it through hands-on inspection.
