# Settings and profile services

2026-08-26 shared-sheet follow-up: the Settings-owned profile switcher uses
`WhiteNoiseModalBottomSheet` and `WhiteNoiseSheetHeader`: continuous
surfaceContainerLow, no extra handle/title spacer, 24 dp text insets, 8 dp
body gap and trailing native Close. Selected-profile tone is retained; the
profile list gives flexible space to the Add Profile action at large fonts.
Diagnostics and destructive-task sheets use the same shared presentation;
their profile ownership and dismissal/confirmation behavior are unchanged.

Status: Main Settings and consumer detail destinations visually refreshed and
statically verified on 2026-08-21; developer/destructive visual batch and
device acceptance pending

## Purpose

Give each signed-in profile one reachable place to manage public identity,
local preferences, relay availability, support, and offline donation fixtures
without adding a backend or hidden global state.

## Scope and non-goals

This batch includes the Settings hub, Share & Connect, profile editing and
address verification, profile-key presentation/export, Notifications,
Appearance, Privacy & Security, Data Usage, profile Relays, support-chat
creation, and Donate. Developer controls, sign-out, removal, and device-wide
erase remain in the separate visual-polish Batch 10. Preferences are in-memory
state; notification delivery, biometric authentication, real cryptography,
relay networking, and payments remain excluded.

## Parity contract

- Privacy & Security contains a typed **Diagnostics & Improvements** route
  with an Off/Analytics/Logs/On summary. Its two native switches, retained-log
  size, and confirmed clearing are profile-owned and independent of Developer
  Tools. See `diagnostics-and-improvements.md` for the scoped `4c25393` contract.

- Settings begins with the active profile and keeps Add/Switch Profile
  available in its profile header. Chats delegates profile management here.
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

The Chats app-bar avatar opens a typed Settings route. The Settings profile
header owns Switch Profile and Add Profile access. Every ordinary row uses a
typed Navigation Compose destination. System Back returns one level; dialogs,
menus, and the profile switcher dismiss before navigation. Successful profile
save returns to Settings. Support exits into the unique conversation.
System-owned scanner, picker, Sharesheet, document creator, notification
settings, and device-security settings return to the originating route.

## Exact product copy

Primary titles are **Settings**, **Share & Connect**, **Profile**, **Profile
Keys**, **Notifications**, **Appearance**, **Privacy & Security**, **Data
Usage**, **Relays**, **Chat with support**, and **Donate**. Relay roles are
**Profile**, **Inbox**, and **Chat Messages**. Unavailable states explain the
affected publishing, invitation, or new-chat function and route recovery.

## Android composition

Material 3 top app bars, lazy settings lists, `ListItem`, switches, selectable
radio rows, tabs, buttons, menus, and `AlertDialog` provide native hierarchy
and behavior. The Settings hub and consumer details group related rows with
restrained tonal containment and sentence-case section labels rather than
placing a divider after every row. Profile, Relays, Support, and Donate pin the
single primary task action; forms use fully rounded, label-above tonal fields
whose labels and supporting copy align to the 16 dp input content line;
unavailable dependencies retain their reason and Android recovery route. The
main overview uses compact one-line destination rows with 24 dp rounded
Material Symbols and disclosure chevrons. It removes explanatory summaries
that only restate the destination, retains the Profile relay dependency only
when it is actionable, and shows the active Appearance value in the trailing
slot. Section headings start on the group's 32 dp internal content grid,
aligning with overview icons and icon-free detail-row text rather than the
outer tonal-container edge. The established destination hierarchy and row
order do not change.
Photo Picker, `OpenDocument`, `CreateDocument`, Android Sharesheet, Google Code
Scanner, notification/security settings intents, clipboard, and `FLAG_SECURE`
own device integrations. ZXing core 3.5.4 generates the QR `BitMatrix`; Compose
Canvas renders black modules on a white technical surface so scan contrast is
stable in both app themes.

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

These settings add no permission. The app's separately approved onboarding QR
scanner declares camera permission; no storage, microphone, notification,
network, location, or biometric permission is added here.

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
- [Android settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings) — grouped hierarchy, subordinate screens, dependency explanation, and adaptive measure.
- [Switches](https://developer.android.com/develop/ui/compose/components/switch) and [radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button) — native boolean and mutually exclusive state semantics.
- [Tabs](https://developer.android.com/develop/ui/compose/components/tabs) — the two peer donation methods.
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input) — state-based, label-above profile, password, and relay fields.
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility) — merged row semantics, disabled state, QR purpose, and private-key exclusion.
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker) and [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) — user-selected input/output without broad storage permission.
- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner) — system-delivered scanner without camera permission.
- [Android Sharesheet](https://developer.android.com/training/sharing/send) — public profile sharing.
- [Secure sensitive activities](https://developer.android.com/privacy-and-security/risks/screen-capture) — secure-window behavior.
- [ZXing 3.5.4 release](https://github.com/zxing/zxing/releases/tag/zxing-3.5.4) — local QR encoder version.

## iOS parity evidence

- `wn-ios-prototype@0bd7cba:docs/screens/settings.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-support.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Settings/`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/App/PrototypeSettingsState.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototypeTests/RelayAvailabilityTests.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototypeTests/SupportChatTests.swift`

## Approved differences and custom exceptions

Android uses the Chats avatar as the Settings entry and keeps profile switching
inside the Settings profile header. The QR scanner is a Google system surface
rather than the iOS custom camera. Notification and device-authentication rows
are preferences/system handoffs only because the offline prototype does not
deliver notifications or lock real persisted data. QR output uses a small JVM
encoder because Android does not provide a platform QR-generation surface.
Its black-on-white treatment is the intentional technical exception to
theme-colored surfaces because code legibility and scanner interoperability
take priority over decorative theme matching.

## Observable acceptance criteria

2026-08-26 menu follow-up: Profile photo-source choices now use the shared
official Expressive menu. Existing labels, source contracts, removal and
draft/save behavior are retained; the system-owned Photos/Files surfaces are
not restyled. See `app-menus.md` for the exact API/pin and regression evidence.

- Every ordinary Settings destination is reachable and returns with Back.
- Profile edits and preferences affect only the active profile.
- Theme changes are immediate; secure-window privacy follows its toggle.
- Generated public and donation codes decode to their exact fixture value.
- Profile relay role changes deterministically enable/disable new chat/support
  creation, while existing chats retain their own relay configuration.
- Restore Defaults recovers the seven accepted relay fixtures.
- Support never duplicates; donation never starts a payment.
- The main Settings screen keeps its active-profile header, account switching,
  destination hierarchy, restrained tonal groups, sentence-case labels,
  compact icon-led destination rows, current Appearance value, conditional
  Profile dependency reason, content-grid-aligned section headings, and merged
  switch-row semantics at compact width and large-text RTL composition.
  Decorative row icons do not duplicate the visible label in accessibility
  output.
- Consumer detail screens share tonal groups, radio selection, label-above
  fields, explanatory disabled states, and one pinned primary task where
  appropriate. Static Compose coverage verifies profile/donation QR purpose,
  private-key privacy, notification dependencies, relay defaults, support
  recovery, and large-text RTL reachability.
