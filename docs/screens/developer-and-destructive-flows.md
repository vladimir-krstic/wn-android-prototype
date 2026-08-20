# Developer and destructive flows

Status: Implemented; static verification complete on 2026-08-15

## Purpose

Expose deterministic, sanitized technical inspection for development while
making session exit, profile removal, and whole-app erasure unmistakably
different tasks with consequences enforced by the state model.

## Scope and non-goals

This batch includes the per-profile Developer Tools gate, Debug Mode,
Diagnostics, Anonymous Telemetry, Audit Logging/files, Key Packages,
conversation debug snapshots, Sign Out with optional active-profile wipe,
removal of another stored profile, and Erase App Data. No event is streamed,
uploaded, persisted, or read from Android logs. No account, server data,
cryptographic material, operating-system app data, or real file is deleted.

## Parity contract

- Developer Tools starts off per profile. Disabling it also turns off Debug
  Mode, telemetry, and audit logging, while preserving audit-file metadata,
  current key package, and diagnostic events.
- Debug Mode is independent of telemetry/logging and adds a conversation-debug
  app-bar action only to Fiatjaf and White Noise Support.
- Diagnostics is one persistent console with Events, visible Live state,
  deterministic sanitized events, Test, Clear Events, and optional copied
  conversation summary. Empty state remains in the same console container.
- Audit Logging shows two sanitized inline records without filesystem paths.
  Turning it off hides but preserves them. Confirmed Clear Audit Logs changes
  every size to zero while preserving records and enabled state.
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
**Anonymous Telemetry**, **Audit Logging**, **Clear Audit Logs**, **Events**,
and **Live**. Destructive tasks are **Sign Out**, **Wipe Data From This
Device**, **Remove Profile**, and **Erase App Data**. The erase warning is
**This can’t be undone** and helper is **Enter the three words exactly to
continue.**

## Android composition

Material settings lists, `ListItem`, `Switch`, typed Navigation Compose routes,
scrollable Surface console, ordinary/outlined buttons, and `AlertDialog` own
the developer hierarchy. Full-height `ModalBottomSheet` surfaces contain the
multi-field Sign Out and Erase confirmation tasks. A focused `AlertDialog`
owns the smaller Remove Profile and Clear Audit Logs confirmations. Destructive
buttons use the semantic error color and remain in the task body rather than
an app-bar action.

## Behavior and state

All developer fields and artifacts are immutable values under the active
`Profile`. View-model mutations reject child changes while the gate is off,
reject audit clearing unless logging is visible and nonempty, and replace
rather than append key packages. Conversation snapshots derive from current
chat membership/role/relay state on every read. Exit mutations atomically
update `profiles`, `signedInProfileIds`, and `activeProfileId`; no screen owns a
shadow copy of those consequences.

## System integrations

None. Clipboard copy is explicit and limited to sanitized diagnostic summaries
or user-requested derived technical IDs. The implementation does not inspect
Logcat, app files, device identifiers, network state, or OS-level app storage.

## Accessibility and adaptation

Warnings combine text and symbol, not color alone. Switches and disabled
destructive buttons expose native roles/state. Confirmation fields have visible
labels and ordinary IME behavior. Progress labels state the active consequence.
Developer values use text labels in addition to semantic status colors. Sheets
consume safe-drawing insets; settings lists and consoles scroll at larger font
and display scales. Final device-level TalkBack, keyboard, RTL, and predictive
Back inspection remains Batch 9 under the no-unrequested-emulator rule.

## Governing Android sources

- [Create a bottom sheet](https://developer.android.com/develop/ui/compose/quick-guides/content/create-bottom-sheet) — modal task ownership and removal from composition.
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog) — focused confirmation and input tasks.
- [Material 3 insets](https://developer.android.com/develop/ui/compose/system/material-insets) — safe sheet and Scaffold layout.
- [Android security checklist](https://developer.android.com/privacy-and-security/security-tips) — do not place personal or sensitive values in logs; keep local technical data app-scoped.

## iOS parity evidence

- `reference/wn-ios-prototype-snapshot/docs/screens/settings.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/sign-out.md`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeSettingsState.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeChatDebug.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Settings/DeveloperSettingsViews.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Settings/SignOutSettingsView.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototypeTests/DeveloperToolsTests.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototypeTests/SignOutFlowTests.swift`

## Approved differences and custom exceptions

Android uses Material modal bottom sheets for the two large destructive tasks
instead of iOS large-detent Form sheets. Remove Profile is an explicit Manage
Profiles destination because Android's existing profile switcher remains a
focused selection surface. Developer events use a static Live text indicator;
motion is deferred to visual polish and is not necessary to communicate state.

## Observable acceptance criteria

- Profile switching immediately changes the developer gate, child preferences,
  artifacts, and debug-action availability.
- Disabling tools preserves files/package/events but stops all child features.
- Clearing diagnostics leaves a visible No Events console; Test adds one
  deterministic event.
- Clearing audit content leaves two zero-byte rows and logging enabled.
- Exactly one key package exists before and after publish.
- Debug summaries contain no message, public/private key, profile, or derived
  group-identifier value.
- Every destructive confirmation rejects incorrect input and cancellation is a
  no-op; each successful consequence and root route is model-tested.
