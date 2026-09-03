# Developer and destructive flows

2026-09-04: production B02 extends keys/sign-out/wipe behavior as recorded in
[keys-and-profile-exit.md](keys-and-profile-exit.md). Its lifecycle and staged
outcome contract supersedes earlier immediate-success descriptions here; the
approved presentation and checked wipe default remain.

Status: Implemented and visually refreshed through 2026-08-30; static
verification complete; device visual acceptance pending

## Purpose

Expose deterministic, sanitized technical inspection for development while
making session exit, profile removal, and whole-app erasure unmistakably
different tasks with consequences enforced by the state model.

## Scope and non-goals

This batch includes the per-profile Developer Tools gate, Debug Mode,
Diagnostics, retained diagnostic-record inspection, Key Packages,
conversation debug snapshots, Sign Out with optional active-profile wipe,
removal of another stored profile, and Erase App Data. No source event is
streamed, uploaded, persisted by the app, or read from Android logs. An
explicit export writes only the deterministic sanitized report to the person's
chosen document provider. No account, server data, cryptographic material,
operating-system app data, or real file is deleted.

## Parity contract

- Developer Tools starts off per profile. Disabling it turns off Debug Mode
  while preserving current key package and diagnostic events. The approved
  2026-08-26 `4c25393` delta moves consent and retained logs to
  `Profile.diagnostics`; Developer Tools never changes or gates those choices.
- Debug Mode is independent of telemetry/logging and adds a conversation-debug
  app-bar action only to Fiatjaf and White Noise Support.
- Diagnostics is one persistent console with Events, visible Live state,
  deterministic sanitized events, Test, Clear Events, and optional copied
  conversation summary. Empty state remains in the same console container.
- Developer Tools can inspect retained diagnostic records without filesystem
  paths even while logging is off. It shows the read-only logging state and
  only non-empty retained files, or **There are no logs.** when none contain
  data. When data exists, **Export Diagnostic Logs** writes one deterministic
  sanitized text report to a location chosen in Android's system Files picker.
  Privacy & Security alone owns the Diagnostics & Improvements route,
  preferences, retained-size summary and confirmed clearing. Turning logging
  off keeps records; clearing zeros sizes and keeps preferences.
- Key Packages displays exactly one package. Publishing replaces it with the
  deterministic just-published package.
- Conversation Debug derives lifecycle, member/admin counts, role, epoch,
  required event-kind count, derived group identifiers, relay count, and local
  notification/push fixture status. Copied summaries exclude public/private
  keys, message content, group identifiers, profile values, and unrelated chat
  data.
- Sign Out defaults to wiping. Retaining data removes only the active session;
  wiping requires the exact profile name and removes that profile graph.
- Remove Profile is available only for a different stored profile and requires
  that profile's exact name.
- Erase App Data requires a stable lowercase three-word challenge derived from
  sorted stored profile IDs. Surrounding whitespace is ignored; internal
  spacing and case must match. Success resets all app-owned state.

## Entry, navigation, Back, and exit

Settings links to Developer Tools, Manage Profiles, and one Sign Out sheet.
Privacy & Security links to one Erase App Data sheet. Developer child surfaces
use typed routes. Conversation Debug and its scoped Diagnostics route retain
only the chat ID. Back leaves state unchanged. Dismissing either destructive
sheet leaves state unchanged and is disabled during its short deterministic
progress state. With signed-in profiles remaining, Sign Out selects the first
remaining profile and presents the Settings profile switcher; otherwise it
returns to Welcome. Erase always returns to Welcome.

## Exact product copy

The warning is **For development and testing only** / **These tools can expose
technical information and change how the app behaves.** Technical labels are
**Developer Tools**, **Debug Mode**, **Diagnostics**, **Key Packages**,
**Diagnostic Logs**, **Diagnostic Logging**, **There are no logs.**,
**Export Diagnostic Logs**, **Events**, and **Live**. The export failure is
**Couldn’t Save Diagnostic Logs** / **Choose another location and try again.**
The Diagnostic Logs helper is **Configure or clear
diagnostic logs in Privacy & Security. Existing sanitized files remain
available here after logging is turned off.** Destructive tasks are **Sign
Out**, **Wipe Data From This Device**, **Remove Profile**, and **Erase App
Data**. The erase warning is
**This can’t be undone** and helper is **Enter the three words exactly to
continue.**

## Android composition

Material settings lists, tonal groups, `ListItem`, merged `Switch` rows, typed
Navigation Compose routes, one scrollable Surface console, a top-app-bar
overflow menu, and `AlertDialog` own the developer hierarchy. A compact green
radiowave symbol uses a restrained repeating alpha pulse beside the persistent
Live label; their merged semantics communicate **Live event stream** even when
motion is disabled. Technical identifiers use
monospaced supporting text and explicit copy rows. The master gate reveals
Debugging, Key Packages and Diagnostic Logs only while enabled. The log group
starts with compact read-only **Diagnostic Logging — On/Off** metadata, then
lists only non-empty profile-owned sanitized files or one secondary empty row.
With data present, a final divided Material action launches Android's standard
`CreateDocument("text/plain")` contract with **White Noise Diagnostic
Logs.txt** as the suggested name. It has no switch, clear action, or
Diagnostics & Improvements destination.
Explanations sit on the shared settings helper line outside their related groups; dividers
separate peer rows; About uses compact key/value metadata. Key publication is
a full-width 56 dp filled-tonal Material button beside the current package it
replaces rather than a settings row or persistent page-completion action. Its
official package symbol and text share one button target, and its consequence
remains on the aligned helper line below. Diagnostics keeps its visible Live label
and one divided rounded console. The **Events** title and **Live** status share
the console rows' 16 dp inner content line rather than aligning to the rounded
surface's outer edges. A native trailing vertical-dots `IconButton`
anchors the shared Material menu containing optional **Copy Diagnostic
Summary**, **Test**, and availability-aware **Clear Events**; these contextual
commands no longer consume a separate row above the console.

Full-height `ModalBottomSheet` surfaces contain the multi-field Sign Out and
Erase confirmation tasks. The 2026-08-26 shared refinement uses continuous
surfaceContainer and `WhiteNoiseSheetHeader` with titleLarge, wrapping
titles, 24 dp margins, 8 dp body gap and a trailing native Close. No extra top
spacer follows the Material handle. They retain a scrollable task
body, label-above tonal fields with content-line alignment and explicit
focus/error rings, and one pinned semantic-error action;
dismissal stays disabled during progress. A focused `AlertDialog` owns the
smaller Remove Profile confirmation. Clear Diagnostic Logs now belongs to the
consumer privacy destination. The generated erase
phrase is selectable, and Manage Profiles groups only inactive identities
without adding an action to the active profile.

Sign Out opens directly at the expanded task height with no partial anchor.
Its active identity and wipe choice are peer rows in one white-equivalent
group, separated by the established 2 dp canvas-tone divider. The selected
wipe consequence and exact-name confirmation helper sit outside their
controls on the shared 32 dp settings helper line, 8 dp below the element they
explain. The confirmation keeps both the section instruction and persistent
Material field label. Turning wipe off clears the confirmation and focus;
Done clears focus. Close, system Back and sheet dismissal remain available
until progress starts, then every hidden transition is vetoed until the
named consequence completes. The pinned action is continuous with the gray
sheet canvas rather than adding a second tonal footer band.

## Behavior and state

All developer fields are immutable values under the active `Profile`.
View-model mutations reject debug/console/key-package changes while the gate
is off, and replace rather than append key packages. Consent/log clearing
uses a separate profile-owned state and does not require Developer Tools or
enabled logging. Conversation snapshots derive from current
chat membership/role/relay state on every read. Exit mutations atomically
update `profiles`, `signedInProfileIds`, and `activeProfileId`; no screen owns a
shadow copy of those consequences.

## System integrations

Diagnostic-log export uses the Storage Access Framework through AndroidX's
`CreateDocument("text/plain")` Activity Result contract. Android and the
selected document provider own destination selection; cancellation is a no-op,
and a write failure returns to one focused retry message. The exported payload
contains only the accepted deterministic sanitized event labels, creation
labels, and byte counts; it excludes profile names, profile IDs, source
filenames, messages, keys, and device identifiers. No broad storage permission
or persistent URI access is requested.

Clipboard copy is explicit and limited to sanitized diagnostic summaries or
user-requested derived technical IDs. The implementation does not inspect
Logcat, app files, network state, or OS-level app storage.

## Accessibility and adaptation

Warnings combine text and symbol, not color alone. Switches and disabled
destructive buttons expose native roles/state. Confirmation fields have visible
labels and ordinary IME behavior. Progress labels state the active consequence.
Developer values use text labels in addition to semantic status colors. The
green Live animation is supplemental: the visible Live label and merged
**Live event stream** description expose the same state without motion, and
Compose follows the system animation-duration scale. Sheets
consume safe-drawing insets; settings lists and consoles scroll at larger font
and display scales. Static Compose coverage checks disabled/ready confirmation
actions, exact typed gates, Live and empty diagnostics, and large-text RTL
reachability. Final device-level TalkBack, keyboard, IME, and predictive Back
inspection remains the explicit device-acceptance pass.

## Governing Android sources

- [Create a bottom sheet](https://developer.android.com/develop/ui/compose/quick-guides/content/create-bottom-sheet) — modal task ownership and removal from composition.
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog) — focused confirmation and input tasks.
- [Android settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings) — grouped hierarchy, switch rows, supporting explanations, and child destinations.
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input) — state-based label-above confirmation fields and IME behavior.
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility) — disabled/ready state, live progress, merged rows, and named copy actions.
- [Material 3 insets](https://developer.android.com/develop/ui/compose/system/material-insets) — safe sheet and Scaffold layout.
- [Android security checklist](https://developer.android.com/privacy-and-security/security-tips) — do not place personal or sensitive values in logs; keep local technical data app-scoped.
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) — system-owned destination selection through `ACTION_CREATE_DOCUMENT`.
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result) — lifecycle-safe result registration and delivery.

## iOS parity evidence

- `wn-ios-prototype@0bd7cba:docs/screens/settings.md`
- `wn-ios-prototype@0bd7cba:docs/screens/sign-out.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeSettingsState.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeChatDebug.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Settings/DeveloperSettingsViews.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Settings/SignOutSettingsView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototypeTests/DeveloperToolsTests.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototypeTests/SignOutFlowTests.swift`
- `wn-ios-prototype@4c25393:WhiteNoisePrototype/App/PrototypeDiagnosticsState.swift`
- `wn-ios-prototype@4c25393:WhiteNoisePrototype/Screens/Settings/DeveloperSettingsViews.swift`

## Approved differences and custom exceptions

Android uses Material modal bottom sheets for the two large destructive tasks
instead of iOS large-detent Form sheets. Remove Profile is an explicit Manage
Profiles destination because Android's existing profile switcher remains a
focused selection surface. The iOS Diagnostics toolbar menu maps directly to
Android's top-app-bar `IconButton` and Material dropdown menu. Its variable
radiowave effect maps to a low-amplitude alpha pulse on the official Material
cell-tower/radiowave symbol rather than custom-drawn Apple symbol layers.
Sign Out retains Android's trailing Close action and native sheet dismissal
instead of copying the iOS leading circular close control.

## Observable acceptance criteria

- Profile switching immediately changes the developer gate, child preferences,
  artifacts, and debug-action availability.
- Disabling tools preserves consent/records/package/events and stops debug
  features; it does not disable analytics or logging choices.
- Clearing diagnostics leaves a visible No Events console; Test adds one
  deterministic event.
- Clearing diagnostic content leaves zero-byte records and logging unchanged.
- Developer Tools never exposes the consumer Diagnostics & Improvements
  destination or logging switches; it shows On/Off plus only non-empty files,
  and zero-byte or absent files collapse to **There are no logs.**.
- Export Diagnostic Logs appears only with non-empty files, launches the
  system document picker, and writes one sanitized deterministic text report;
  cancellation is a no-op and a provider failure names the attempted export.
- Exactly one key package exists before and after publish.
- Publish New Key Package is one enabled filled-tonal button and invokes the
  deterministic replacement mutation from its complete target.
- Debug summaries contain no message, public/private key, profile, or derived
  group-identifier value.
- Every destructive confirmation rejects incorrect input and cancellation is a
  no-op; each successful consequence and root route is model-tested.
- Developer sections retain technical density inside deliberate groups;
  Diagnostics retains one Live/empty console, a native contextual overflow
  menu, disabled Clear Events when empty, a non-motion Live equivalent, and a
  header aligned to the console rows' 16 dp inner content line.
- Sign Out and Erase keep their consequence, confirmation, disabled/ready
  action, progress, and close behavior reachable at large text and RTL.
