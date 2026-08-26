# Settings and profile services

2026-08-27 Share & Connect follow-up: the route is now one stable Material page
instead of a mode switch. Its centered app-bar title is **Share & Connect**;
native Back and Share actions keep their standard targets. The page shows the
active profile as one clean adaptive identity composition: prominent avatar,
name, optional official filled verified seal, a visibly 32 dp copyable
public-key capsule inside a transparent 48 dp target, dominant black-on-white QR and
**Scan to connect.** caption. A single full-width primary **Scan QR Code** action
is pinned above the navigation safe area and opens the existing near-full
CameraX/ML Kit profile scanner sheet, retaining
its rounded target, torch, just-in-time camera permission, swipe/Back dismissal
and error recovery. Dismissing the scanner returns to the useful identity page;
a successful fixture scan shows the deterministic Profile Found state. No new
scanner, dependency, network path or copied iOS camera chrome was introduced.

2026-08-26 Settings-hub follow-up: the root uses a neutral
`surfaceContainerLow` canvas with white-equivalent `surfaceContainerLowest`
cards. Its first card contains the 56 dp active Share & Connect row and one
plain Material profile-management row separated by a subtle canvas-tone
divider. A sole profile gets an icon-led Add Profile row; alternates get a
stacked-avatar Switch Profile row, which expands the card to show deterministic
48 dp profile rows, unread badges and a final Add Profile row. The root then
exposes one seven-destination group, an unheaded Help destination group,
isolated Sign Out, and a centered **Version 0.1** footer without an Android or
product-name prefix. Manage Profiles remains a
compatible typed route but is no longer exposed here; Appearance state is
shown only inside Appearance.

The Settings-owned profile switcher uses
`WhiteNoiseModalBottomSheet` and `WhiteNoiseSheetHeader`: continuous
surfaceContainer, no extra handle/title spacer, 24 dp text insets, 8 dp
body gap and trailing native Close. Its 48 dp two-line rows keep active-first
stable order, a selected active container/check, inactive aggregate unread
badges capped at 99+, and a pinned Add Profile action.
Diagnostics and destructive-task sheets use the same shared presentation;
their profile ownership and dismissal/confirmation behavior are unchanged.

Status: Main Settings/profile switching statically verified on 2026-08-26;
consumer detail destinations remain at their 2026-08-21 static gate and device
acceptance is pending

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

- Settings begins with one compact active-profile card. The active row opens
  Share & Connect. Its Add Profile action is direct when no alternate exists;
  otherwise Switch Profile expands inline to show alternate unread state and
  Add Profile. Chats delegates profile management here, while typed external
  switcher entry retains the Material modal sheet.
- Share & Connect is one stable identity page. It renders a standards-compliant
  local QR code for the public key, copies public identity with temporary
  confirmation, uses Android Sharesheet, and exposes one explicit Scan QR Code
  action. The action reuses the approved app-owned CameraX/ML Kit scanner before
  showing the deterministic Profile Found state.
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
card owns Switch Profile and Add Profile access. Inline selection applies
immediately; Back first collapses expanded profiles. The retained modal
switcher selects immediately while Back, scrim, and swipe dismissal makes no
change. Every ordinary row uses a
typed Navigation Compose destination. System Back returns one level; dialogs,
menus, and the profile switcher dismiss before navigation. Successful profile
save returns to Settings. Support exits into the unique conversation.
The app-owned scanner and system-owned picker, Sharesheet, document creator,
notification settings, and device-security settings return to the originating
route.

## Exact product copy

Primary route titles are **Settings**, **Share & Connect**, **Profile**,
**Profile Keys**, **Notifications**, **Appearance**, **Privacy & Security**,
**Data Usage**, **Relays**, **Chat with support**, and **Donate**. Share &
Connect exposes the explicit task label **Scan QR Code**. Relay roles are
**Profile**, **Inbox**, and **Chat Messages**. Unavailable states explain the
affected publishing, invitation, or new-chat function and route recovery.

## Android composition

Material 3 top app bars, lazy settings lists, `ListItem`, switches, selectable
radio rows, tabs, buttons, menus, and `AlertDialog` provide native hierarchy
and behavior. Share & Connect uses a center-aligned title with native Back and
Share icon actions at their standard 48 dp targets. Its stable adaptive content
stays scrollable, preserves a bounded measure and uses semantic surface roles
while the QR alone remains literal black on white, with 12 dp of app-owned frame
padding, no additional encoder margin on this screen, the theme's less-rounded
16 dp `large` corners, a 16 dp gap from the public-key target, and a 1 dp gap to
its caption. An official filled rounded
`verified` symbol provides the optional address seal. The public-key capsule is
visibly 32 dp high at default text scale while its outer target remains 48 dp.
The target and capsule share interaction state, but the bounded ripple is
clipped to the visible pill rather than painting the larger hit region. Scan QR Code
uses the shared primary task button across the content width in a pinned,
safe-area-aware bottom slot. The Settings root uses one
expandable identity card, one unheaded
seven-destination group, an unheaded Help group, and an isolated
destructive Sign Out group. Consumer details retain relationship-specific
section labels. Profile, Relays, Support, and Donate pin the
single primary task action; forms use fully rounded, label-above tonal fields
whose labels and supporting copy align to the 16 dp input content line;
unavailable dependencies retain their reason and Android recovery route. The
main overview uses compact one-line destination rows with 24 dp rounded
Material Symbols and disclosure chevrons. It removes explanatory summaries
that only restate the destination, retains the Profile relay dependency only
when it is actionable, and keeps the active Appearance choice inside the
Appearance screen. The old Manage Profiles root row and redundant
Profile/Preferences headings are absent; its typed destination remains for
compatibility. Profile-switch projection and unread aggregation are pure,
unit-tested presentation state. Root groups use two-dp `surfaceContainerLow`
surface gaps between transparent native list rows, matching the page canvas
without introducing separate cards or button outlines inside a group. The
stacked preview's `+N` remainder is a true 32 dp circular slot, and inactive
profile unread badges use the same monochrome semantic colors as Chats.
Photo Picker, `OpenDocument`, `CreateDocument`, Android Sharesheet, CameraX,
bundled on-device ML Kit barcode analysis, notification/security settings
intents, clipboard, and `FLAG_SECURE` own device integrations. ZXing core 3.5.4
generates the QR `BitMatrix`; Compose
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

- the shared app-owned CameraX/ML Kit scanner for profile QR input, with camera
  permission requested only when the explicit Scan QR Code action opens it;
- Android Photo Picker and Storage Access Framework for avatar input;
- platform-default Photo Picker and Files appearances without app color
  overrides;
- Android Sharesheet and clipboard for explicit public/donation output;
- Storage Access Framework document creation for deterministic key exports;
- app-notification and device-security Settings intents;
- secure-window protection for Recents previews and screenshots.

This follow-up adds no permission or dependency. It reuses the camera permission
and scanner runtime already approved for onboarding; no storage, microphone,
notification, network, location, or biometric permission is added here.

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
- [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars) and [Material insets](https://developer.android.com/develop/ui/compose/system/material-insets) — centered task control, navigation/action placement, scroll tone and single inset ownership.
- [Material buttons](https://developer.android.com/develop/ui/compose/components/button) — explicit primary scanner task with native state, target and motion.
- [Material Symbols](https://developer.android.com/develop/ui/compose/graphics/images/material) — official filled verification and scanner artwork.
- [Android settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings) — grouped hierarchy, subordinate screens, dependency explanation, and adaptive measure.
- [Switches](https://developer.android.com/develop/ui/compose/components/switch) and [radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button) — native boolean and mutually exclusive state semantics.
- [Tabs](https://developer.android.com/develop/ui/compose/components/tabs) — the two peer donation methods.
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input) — state-based, label-above profile, password, and relay fields.
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility) — merged row semantics, disabled state, QR purpose, and private-key exclusion.
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker) and [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) — user-selected input/output without broad storage permission.
- [CameraX preview](https://developer.android.com/media/camera/camerax/preview) and [ML Kit barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android) — shared app-owned live preview and on-device profile-code analysis.
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
- Scoped current Settings evidence:
  `wn-ios-prototype@4c25393:WhiteNoisePrototype/Screens/Settings/SettingsView.swift`
  and `WhiteNoisePrototypeTests/ProfileLifecycleTests.swift`

## Approved differences and custom exceptions

Android uses the Chats avatar as the Settings entry. Settings expands alternate
profiles inline; a Material modal switcher remains for the typed external
switcher entry. Share & Connect keeps one stable identity page and opens the
shared app-owned scanner through a single full-width primary action as a near-full
Material task sheet instead of copying iOS translucent camera chrome. A button
group is intentionally absent because the Android route has one scanner task,
not multiple persistent peer modes.
Notification and device-authentication rows
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
- Share & Connect opens as one stable identity page, exposes Android Sharesheet,
  gives public-key copy confirmation from a compact visible capsule with a full
  target and a capsule-bounded state layer, uses the official filled
  verification seal, and keeps QR purpose in
  semantics. Scan QR Code opens the shared scanner; dismissal returns to the
  same useful page, invalid/unavailable scans explain recovery, and a valid
  profile code reaches Profile Found.
- Profile relay role changes deterministically enable/disable new chat/support
  creation, while existing chats retain their own relay configuration.
- Restore Defaults recovers the seven accepted relay fixtures.
- Support never duplicates; donation never starts a payment.
- The main Settings screen keeps a compact 56 dp active-profile row and an
  in-card, divider-separated Add Profile or expandable Switch Profile list row
  with adaptive profile previews, a neutral canvas with white-equivalent
  containers and subtle canvas-tone row separators, seven
  consolidated icon-led destinations,
  conditional Profile dependency reason, unheaded Help destinations, isolated
  Sign Out, centered version-only footer, and merged row
  semantics at compact width and large-text RTL composition. It does not show
  old Profile/Preferences headings, Manage Profiles, or Appearance's current
  value on the root. Inline alternates retain stored order, 48 dp rows and
  capped unread badges; the modal switcher keeps active-first/stable order,
  48 dp rows,
  active check precedence, capped inactive unread badges, immediate selection,
  dismissal safety, and its pinned Add Profile action.
  Decorative row icons do not duplicate the visible label in accessibility
  output.
- Consumer detail screens share tonal groups, radio selection, label-above
  fields, explanatory disabled states, and one pinned primary task where
  appropriate. Static Compose coverage verifies profile/donation QR purpose,
  private-key privacy, notification dependencies, relay defaults, support
  recovery, and large-text RTL reachability.
