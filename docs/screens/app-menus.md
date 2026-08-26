# App-owned Material menus

## Purpose and scope

Use Google's current standard-color vertical menus consistently across all six
app-owned popup entry points: Chats actions, Chats filters, Sign Up photo
sources, Set Up Group photo sources, Profile photo sources, and voice-review
format. This is a behavior-preserving menu migration, not a global Expressive
theme switch or a redesign of sheets, dialogs, fields, or system pickers.

## Product and state contract

Preserve each screen's accepted labels, action order, enabled/selected states,
callbacks, and source-picker boundaries. Chats retains live profile/chat
identity, long-press highlight, TalkBack actions, Undo, destructive confirmation,
and sole-admin protection. Dismiss before executing a command or selecting a
scope/voice format. Back, outside tap, navigation, and lost anchors keep their
existing dismissal behavior; no new persistence or telemetry.

## Android composition

Use `DropdownMenuPopup`, `DropdownMenuGroup`, and the new command/selected
`DropdownMenuItem` overloads from Material 3 `1.5.0-alpha25`. This explicit
prerelease exception is authorized by the user's request after the release
status explanation. Keep the existing Compose BOM and unrelated versions.

The latest alpha26 was checked first but its ripple AAR requires API 24;
the full manifest gate rejected it against this app's API 23 minimum. Pin
alpha25 instead: its Material/ripple manifests both declare API 23 and it
contains the same public Expressive menu family. Do not override the manifest
or drop Android 6 support for a visual migration. Explicit standard
`selectableItemColors` are used for command items too, avoiding alpha25's
baseline `itemColors` partial-override issue documented as fixed in alpha26.

One shared `WhiteNoiseDropdownMenu` composes a standalone native group with
`MenuDefaults.groupShapes`, position-aware item shapes, standard surface colors,
native typography/padding/elevation/motion, and official icon assets sized by
`MenuDefaults`. Selection uses the native radio-item semantics, shape/color
state and check, not a manually drawn selected background. Destructive commands
retain semantic error text/icon colors; disabled colors remain Material-owned.

The popup owns anchor positioning/focus/RTL/Back/outside dismissal. Its content
uses a standard Compose vertical scroll inside the rounded group so all
commands remain reachable in short windows, with the IME, or at 200% font size.
No fixed popup dimensions, custom positioning, gesture handler, or Signal code.
Chats alone uses the shared wrapper's optional `anchorSpacing`: 8 dp of
transparent vertical padding on `DropdownMenuPopup`, outside the group surface.
Native fitting includes that space for either above- or below-row placement;
the other five popup entry points retain zero additional spacing.

## Copy and parity evidence

No product copy changes. Existing contracts are in
`chats-and-chat-creation.md`, `onboarding-and-profiles.md`,
`settings-and-profile-services.md`, and `composer-media-and-speech.md`.
Pinned iOS paths are resolved by `docs/port/source-map.md`; existing Android
records are sufficient for this presentation-only migration. Signal's scoped
long-press behavior remains evidence, not the visual component authority.

## Verification criteria

- Every app-owned dropdown uses the shared Expressive implementation.
- Commands retain ordering, destructive safeguards and dismiss-before-dispatch.
- Scope and voice-format selection expose native selected semantics.
- Compact/expanded widths, RTL, large fonts, light/dark, keyboard-open layout,
  edge anchors, Back/outside dismissal, scrolling and disabled items have
  Compose regression coverage.
- Run unit tests, lint, and both APK assembly tasks. Device execution and
  visual acceptance remain separately requested.

## Governing sources

- [Material menu design](https://m3.material.io/components/menus/overview)
- [Material menu specifications](https://m3.material.io/components/menus/specs)
- [Material release and API status](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Menu defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/MenuDefaults)
- [Exact published source](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.5.0-alpha25/material3-android-1.5.0-alpha25-sources.jar)

## Related component audit — 2026-08-26

This is a code/API audit, not a new device visual review. A published design,
a public non-experimental API and a stable library release are three different
things. The previous broad description of the app as fully Expressive was
inaccurate. Findings outside menus are recorded, not implemented in this scope:

- **Task buttons:** `WhiteNoiseButtons.kt` has the correct copied 56 dp medium
  container and 24/8 dp padding, but calls the baseline button overload. Its
  labels inherit `labelLarge` (14 sp), not the new medium
  `ButtonDefaults.textStyleFor` result, `titleMedium` (16 sp); it also lacks
  the new pressed-shape behavior. Follow-up should adopt named defaults,
  size-matched text and native expressive shapes together, preserving busy
  semantics and loading contrast. Do not call the existing dimensions wrong.
- **Theme and motion:** `WhiteNoiseTheme.kt` uses `MaterialTheme` without a
  motion override. Published source defaults to `MotionScheme.standard()`;
  `MaterialExpressiveTheme` chooses `MotionScheme.expressive()`. The existing
  typography/shape scales are largely baseline too. This is incomplete
  adoption relative to the design direction, not broken navigation. Switching
  the whole motion scheme needs a separate bounded verification pass.
- **Native API modernization:** existing list items, sheet-state factories
  and header scroll adapters predate newer convenience APIs. The updated
  library emits deprecations for the old ListItem/sheet-state entry points;
  retaining them here preserves existing layout and dismissal behavior.
  The subsequent approved Chats-only refinement adopts the new clickable
  list item for native 12 dp avatar spacing and its rounded selected shape;
  other screen rows remain unchanged.
  A deprecation alone is not evidence of a visible defect.
- **Intentional, not accidental:** full-rounded 28 dp form fields are the
  accepted WN-ANDROID-0023/0024 treatment. Ordinary sheet surfaces/headers and
  the Mute dialog were explicitly approved in WN-ANDROID-0039. The native FAB,
  dialogs, valid baseline tabs, and system-owned picker boundaries do not
  become wrong because newer variants exist. No broad restyling is warranted
  from the menu finding alone.

No additional product-state, consent, fixture or network behavior was changed.

## Verification evidence — 2026-08-26

`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes with 117 unit tests, 126 compiled instrumentation tests and both APKs.
Lint reports zero errors and one understood `GradleDependency` warning:
alpha26 is newer than the intentional API-23-compatible alpha25 pin. No global
lint suppression or unsafe manifest override was added. Compiler deprecations
for existing non-menu list/sheet APIs remain visible for a later migration.

`WhiteNoiseMenuTest` adds command roles/targets, disabled and destructive
dismissal safety, dispatch ordering, light/dark standard surface, compact/
expanded panes, top/bottom anchors, short-menu scrolling, 200% RTL text,
Back/outside dismissal and an IME-open draft-preservation case.
`ChatsScreenTest` checks native selected scope semantics and photo-menu-to-web
handoff. `ConversationScreenTest` checks selected voice format and send-label
updates. Existing `ChatsPolishTest` preserves live eligibility, action ordering,
Undo, destructive/sole-admin protection, anchor/profile dismissal and TalkBack
coverage; `DiagnosticsPromptTest` retains consent-lifecycle coverage.

Source inventory confirms six shared-menu call sites and no baseline dropdown
call sites. Dependency insight confirms Compose UI remains 1.12.0; the merged
manifest remains minSdk 23 with no network permission. No emulator/device was
launched, installed, interacted with or captured. Instrumentation execution,
real TalkBack, gesture behavior and visual acceptance remain pending.
