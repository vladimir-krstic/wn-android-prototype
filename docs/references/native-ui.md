# Native Android UI evaluation

Use this process when translating iOS parity evidence or reviewing an Android
screen. Product parity determines what the person can accomplish; Android
determines how the interaction should work.

## Evaluation sequence

1. State the user goal and observable outcome without naming an iOS control.
2. List the accepted copy, data, states, mutations, and recovery behavior.
3. Identify which iOS details are product requirements and which are Apple
   rendering or navigation conventions.
4. Open the relevant current sources from `android.md`.
5. Choose the closest Material 3, Compose, AndroidX, or Android platform
   pattern.
6. Check Back, predictive Back, insets, IME, adaptation, semantics, focus,
   state restoration, motion, and permissions.
7. Use a custom composition or low-level gesture only if standard patterns
   cannot preserve the accepted product need.
8. Record the translation and any custom exception in the Android brief.

## Common intent translations

| iOS evidence | Android-native starting point | Preserve |
| --- | --- | --- |
| `NavigationStack` push | Navigation Compose destination with callbacks and system/predictive Back | Route hierarchy and completion destination |
| Navigation title and toolbar | Material 3 top app bar and actions | Title hierarchy and available commands |
| `List` / `Form` | Material list items, grouped content in `LazyColumn`, controls with complete row semantics | Grouping, labels, summaries, values, and mutations |
| Large or medium sheet | `ModalBottomSheet`, dialog, or full-screen destination based on Android task complexity | Modal task boundary, dismissal, and result |
| `confirmationDialog` / alert | Material `AlertDialog` or consequence-aware bottom sheet | Exact consequence, safe dismissal, destructive action |
| SwiftUI `Menu` | `DropdownMenu`, exposed dropdown, or an Android action sheet where context needs room | Command set, availability, and selection |
| Context menu with lifted preview | Android long-press contextual action sheet/menu or selection mode | Message focus and available actions, not the Apple preview animation |
| Swipe actions | Android swipe affordance only when discoverable and reversible; pair recoverable actions with Snackbar undo | Mutation, confirmation, and recovery |
| `ShareLink` / activity view | Android Sharesheet via `ACTION_SEND` and content URIs | Share payload and privacy boundary |
| Photos picker | Android Photo Picker Activity Result contract | Media types, selection limit, cancellation, and returned URIs |
| File importer | Storage Access Framework / `ACTION_OPEN_DOCUMENT` | MIME scope, cancellation, and access lifetime |
| Camera/QR scanner | Permissionless Google Code Scanner for system-owned scan; CameraX plus analyzer for approved custom UI | Supported code, success/cancel/error behavior |
| AV playback | AndroidX Media3 player owned by lifecycle-aware state | Play/pause/seek, exclusivity, release, and deterministic sample |
| `LocalAuthentication` concept | `BiometricPrompt` with an explicit authenticator policy | Return-access protection, capability state, and cancellation |
| SF Symbol | Closest Material Symbol with a localized accessible name; custom vector only for brand-specific meaning | Semantic action, not glyph geometry |
| iOS toast or transient overlay | Snackbar when transient feedback or undo fits | Message, action, duration, and non-color meaning |

## Component checks

- Does a Material component already supply correct roles, focus, keyboard
  interaction, touch target, ripple/state layer, disabled state, and motion?
- Does the layout remain useful at large font and display sizes without fixed
  iPhone measurements?
- Does the component handle edge-to-edge insets and IME animation, or must the
  screen apply them once at a clear owner?
- Is a trailing icon a real action, a value indicator, or decoration? Give it
  the correct semantics and touch target.
- Does the action belong in the top app bar, overflow menu, row, bottom sheet,
  dialog, contextual selection bar, or system surface?
- Can destructive or recoverable actions use system patterns rather than a
  bespoke red control?
- Does the screen support Android's system Back model instead of requiring an
  in-app close path copied from iOS?

## Accessibility and adaptation

- Prefer native Material semantics before adding manual properties.
- Merge or clear semantics only after inspecting the resulting tree.
- Keep traversal logical and avoid turning an entire complex row into one
  inaccessible click target when it owns independent controls.
- Give custom gestures equivalent accessible actions.
- Ensure status, selection, delivery, relay availability, and verification are
  expressed in text or state descriptions, not color or icon shape alone.
- Test at supported font/display scale extremes, RTL, compact/expanded widths,
  portrait/landscape, and gesture/button navigation as relevant.
- Use adaptive list-detail or supporting-pane layouts when extra width improves
  the task; do not simply stretch a phone column.

## Motion, feedback, and performance

- Prefer component and navigation motion. Preserve predictive Back previews.
- Avoid custom motion that competes with scrolling, Back gestures, the IME, or
  TalkBack exploration.
- Honor system animation scale and avoid motion-only meaning.
- Keep lazy-list item identity stable and move expensive image/media work out
  of composition.
- Use haptics only for meaningful Android feedback and never as the only state
  cue.

## Review output

For every material translation, record:

- the product behavior preserved;
- the Android component/system surface selected;
- the official source that governed it;
- any difference from iOS and why it is more native on Android;
- any custom exception, accessibility equivalent, and testable criterion.

