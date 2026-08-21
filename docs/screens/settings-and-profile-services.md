# Settings and profile services

Status: Implemented; static verification complete on 2026-08-15

## Purpose

Give each signed-in profile one reachable place to manage public identity,
local preferences, relay availability, support, and offline donation fixtures
without adding a backend or hidden global state.

## Scope and non-goals

This batch includes the Settings hub, Share & Connect, profile editing and
address verification, profile-key presentation/export, Notifications,
Appearance, Privacy & Security, Data Usage, profile Relays, support-chat
creation, and Donate. Developer controls, sign-out, removal, and device-wide
erase are covered by their separate Batch 8 brief. Preferences are in-memory
state; notification delivery, biometric authentication, real cryptography,
relay networking, and payments remain excluded.

## Parity contract

- Settings begins with the active profile and keeps Add/Switch Profile
  available without removing the chat-list switcher.
- Share & Connect renders a standards-compliant local QR code for the public
  key, copies public identity, uses Android Sharesheet, and uses permissionless
  Google Code Scanner before showing the deterministic Profile Found state.
- Profile edits name, About, verified `name@domain` address, and avatar through
  Photo Picker, Storage Access Framework, or the pinned web-image fixtures.
- Profile Keys visually hides/reveals and copies the deterministic private-key
  value, but never exposes it to accessibility semantics. Encrypted export is
  primary; raw export requires an explicit consequence confirmation. Both use
  Android's document creator and do not claim real cryptography.
- Notification, appearance, privacy, download, and quality choices belong to
  the active profile and update immediately. Appearance drives the app theme.
- Seven deterministic profile relay records expose connected, reconnecting,
  disconnected, read-only, custom, and unassigned states plus Profile, Inbox,
  and Chat Messages roles. Only connected Chat Messages relays are
  copied into newly created chats; existing chat relay lists remain unchanged.
- Support first explains the channel, then opens or creates exactly one
  profile-owned support conversation or routes to relay recovery.
  Donate provides scannable fixture codes and copy actions without opening a
  wallet or performing a payment.

## Entry, navigation, Back, and exit

The Chats app bar Settings action opens a typed Settings route. Every ordinary
row uses a typed Navigation Compose destination. System Back returns one level;
dialogs, menus, and the profile switcher dismiss before navigation. Successful
profile save returns to Settings. Support exits into the unique conversation.
System-owned scanner, picker, Sharesheet, document creator, notification
settings, and device-security settings return to the originating route.

## Exact product copy

Primary titles are **Settings**, **Share & Connect**, **Profile**, **Profile
Keys**, **Notifications**, **Appearance**, **Privacy & Security**, **Data
Usage**, **Relays**, **Chat with support**, and **Donate**. Relay roles are
**Profile**, **Inbox**, and **Chat Messages**. Unavailable states explain the
affected publishing, invitation, or new-chat function and route recovery.

## Android composition

Material 3 top app bars, lazy settings lists, `ListItem`, switches, radio rows,
tabs, buttons, menus, and `AlertDialog` provide native hierarchy and behavior.
Photo Picker, `OpenDocument`, `CreateDocument`, Android Sharesheet, Google Code
Scanner, notification/security settings intents, clipboard, and `FLAG_SECURE`
own device integrations. ZXing core 3.5.4 generates the QR `BitMatrix`; Compose
Canvas only renders that matrix using semantic theme colors.

## Behavior and state

`Profile.settings` is authoritative and isolated with the rest of each profile
graph. View-model mutations reject blank names, malformed addresses, duplicate
or non-`wss://` relays, changes to read-only relays, and invalid role targets.
Relay changes recompute only the profile's future-chat relay list. Restore
Defaults replaces the relay collection and future-chat availability, never an
existing chat's captured relay configuration. Transient menu/dialog/input and
private-key reveal state remain screen-owned and saveable where appropriate.

## System integrations

- permissionless Google Code Scanner for QR input;
- Android Photo Picker and Storage Access Framework for avatar input;
- platform-default Photo Picker and Files appearances without app color
  overrides;
- Android Sharesheet and clipboard for explicit public/donation output;
- Storage Access Framework document creation for deterministic key exports;
- app-notification and device-security Settings intents;
- secure-window protection for Recents previews and screenshots.

No camera, storage, microphone, notification, network, location, or biometric
permission is declared.

## Accessibility and adaptation

Visible control labels and component roles provide semantics. QR images have a
stable **Profile QR code** description; the private key uses cleared semantics
that announce only hidden/revealed state. Status always has text in addition
to color. The existing safe-drawing Scaffold and 680 dp adaptive content cap
apply throughout; scroll containers accommodate font/display scaling and IME.
Batch 9 added static RTL and 200% font-scale composition coverage. Final
TalkBack traversal and extreme-scale inspection remain part of the explicitly
user-directed device polish pass.

## Governing Android sources

- [Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation) — typed destination ownership and Back.
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker) and [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) — user-selected input/output without broad storage permission.
- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner) — system-delivered scanner without camera permission.
- [Android Sharesheet](https://developer.android.com/training/sharing/send) — public profile sharing.
- [Secure sensitive activities](https://developer.android.com/privacy-and-security/risks/screen-capture) — secure-window behavior.
- [ZXing 3.5.4 release](https://github.com/zxing/zxing/releases/tag/zxing-3.5.4) — local QR encoder version.

## iOS parity evidence

- `reference/wn-ios-prototype-snapshot/docs/screens/settings.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-support.md`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Settings/`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/App/PrototypeSettingsState.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototypeTests/RelayAvailabilityTests.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototypeTests/SupportChatTests.swift`

## Approved differences and custom exceptions

Android retains the fast avatar profile switcher and adds a Settings app-bar
action. The QR scanner is a Google system surface rather than the iOS custom
camera. Notification and device-authentication rows are preferences/system
handoffs only because the offline prototype does not deliver notifications or
lock real persisted data. QR output uses a small JVM encoder because Android
does not provide a platform QR-generation surface.

## Observable acceptance criteria

- Every ordinary Settings destination is reachable and returns with Back.
- Profile edits and preferences affect only the active profile.
- Theme changes are immediate; secure-window privacy follows its toggle.
- Generated public and donation codes decode to their exact fixture value.
- Profile relay role changes deterministically enable/disable new chat/support
  creation, while existing chats retain their own relay configuration.
- Restore Defaults recovers the seven accepted relay fixtures.
- Support never duplicates; donation never starts a payment.
