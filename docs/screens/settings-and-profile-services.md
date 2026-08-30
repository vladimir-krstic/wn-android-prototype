# Settings and profile services

2026-08-29 Appearance follow-up: Appearance keeps the immediate profile-owned
System default/Light/Dark choice as one accessible Material radio group with
two-dp canvas-tone separators and no full-row selected fill. Concise theme
behavior sits immediately below that group. Language is no longer a second
dense inline picker: one disclosure row opens a typed **Language** destination
with System default, English, German, Spanish, French, Italian, Portuguese and
Serbian as immediately applied radio choices. The presentation follows Android
settings and radio semantics while preserving the pinned iOS product choices.
Because this prototype has no translated resources and preferences remain
profile-owned, selection stays deterministic and does not invoke Android's
app-wide locale API.

2026-08-29 Notifications permission follow-up: Android's real app-level
notification capability now gates this otherwise deterministic prototype.
Before the Android 13+ request, one contextual **Allow notifications** action
launches the system-owned runtime prompt. A denial or a system-level block
replaces it with **Notifications are off** and an app-specific Android Settings
handoff; returning to the app refreshes availability. Granted access removes
the permission group entirely. Android 12 and earlier derive availability from
the system notification toggle. The permanent Android section is removed,
while notification creation, delivery, channels, push and persistence remain
out of scope.

2026-08-29 Notifications presentation follow-up: Delivery keeps two whole-row Material
switches in one white-equivalent group with the accepted two-dp canvas-tone
separator and product-meaningful device/wake-up explanations. Preview is no
longer hidden behind the generic choice dialog: the Notifications detail page
shows all three native radio rows inline, separated by the same group rhythm,
followed by the deterministic example and concise helper. Selection applies
immediately; Android notification access owns Local notifications availability,
and Local notifications then owns Native push and Preview availability.

2026-08-27 Profile Keys field/dialog follow-up: key-value rows now render the
complete stored value into a weighted text slot and apply middle ellipsis only
at the measured trailing-action boundary. A hidden private key clips its mask
glyphs without drawing an ellipsis. Copy/visibility artwork sits on the
standard 16 dp visual edge while retaining native 48 dp action targets. The
encrypted export task uses the shared 28 dp rounded secure-field component
with attached labels, compact field rhythm, visible mismatch feedback and
standard Material alert actions.

2026-08-27 Profile Keys follow-up: the route now uses the Settings canvas and
three compact white-equivalent Material groups for Public key, Private key and
Export. Public/private values keep their copy, reveal and accessibility
boundaries without a repeated warning card, a separate Show row, oversized
task buttons, or implementation-oriented document-picker footer. The private
value remains absent from accessibility even while visibly revealed. Encrypted
export uses one focused Material password dialog with two fields and immediate
mismatch feedback; raw export uses the explicit **Keep Your Private Key Safe**
consequence dialog. Android's system document creator remains the final save
surface, and a failed write produces a concise recoverable error dialog.

2026-08-27 Profile follow-up: Profile now opens in a native read mode. Name,
Verified Nostr Address and About retain their Material field geometry but are
read-only, selectable and full-contrast until the app-bar **Edit** action is
selected. The official filled
verified seal sits inside the address field and replaces the repeated
verification helper line. Photo sources are editing-only. Edit mode exposes
one validated **Save** task above navigation/IME insets; its zero-elevation
surface matches the white-equivalent page instead of creating a gray footer.
Save returns to read mode on Profile. System Back cancels and restores an
active draft before a subsequent Back leaves the route.

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

2026-08-30 Relays follow-up: the profile relay list uses separated Material
name/URL rows with compact semantic connection indicators and an in-list Add
Relay action instead of a pinned page button. Relay detail uses compact
key/value metadata, a separated Use For switch group, and an isolated Remove
Relay action. Add Relay is a validity-aware Material task sheet; remove and
restore remain focused alerts with accepted product language. The deterministic
profile-owned relay state and networking boundary are unchanged. See
`relays.md` for the focused contract.

2026-08-30 Relays refinement: connection state is binary at a glance with a
filled green check for Connected and a filled red close for every
not-connected state; TalkBack retains exact Reconnecting/Disconnected wording.
Restore Default Relays is now a native full-width filled-tonal button. Shared
Settings helpers beneath groups and buttons align to the same 32 dp content
line as row titles and section labels app-wide, sit 8 dp below the content they
explain, and leave the larger 24 dp separation to the following section or
action. Relay status symbols use the compact 20 dp treatment.

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
  Tools. Auto-lock is visible only while Android device authentication is
  effective; an unsecured device offers a direct Android security-settings
  recovery action. Erase App Data keeps one consequence outside its entry and
  opens the exact-phrase Material sheet. See `privacy-and-security.md` and
  `diagnostics-and-improvements.md` for the accepted contracts.

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
- Profile reads name, About, and the verified `name@domain` address in
  full-contrast read-only tonal fields. App-bar Edit makes those fields
  editable and exposes editing-only avatar
  sources through Photo Picker, Storage Access Framework, or the pinned
  web-image fixtures; one Save action applies the draft and returns to read
  mode.
- Profile Keys presents public, private, and export actions as three compact
  grouped lists. It visually hides/reveals and copies the deterministic
  private-key value, but never exposes it to accessibility semantics.
  Key values fill the measured row and use middle ellipsis before their
  standard trailing action. Encrypted export uses a focused two-field native
  Material secure-input dialog; raw export
  requires an explicit consequence confirmation. Both continue through
  Android's document creator and do not claim real cryptography.
- Notification, appearance, language, privacy, download, and quality choices
  belong to the active profile and update immediately. Appearance drives the
  app theme. Its Theme choices stay inline; a single Language disclosure opens
  the dedicated radio-list destination and Back returns to Appearance.
  Notifications presents Local notifications and its dependent Native push
  switch as separated native rows. Its three dependent preview choices remain
  visible on the detail page as one accessible radio group with a deterministic
  example. Android's app-wide notification permission appears only as a
  contextual first-request or blocked-recovery group; it disables all
  profile-owned notification options until granted and refreshes after the
  system prompt or Settings return.
- Data Usage keeps the four per-media download policies and sent photo/video
  quality as compact immediate Material radio dialogs rather than copying iOS
  child-screen depth. Separated groups show every current value, place help
  with the setting it explains, and disable Reset download settings while all
  four download policies already match their defaults. See `data-usage.md`.
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
save returns to Profile read mode; Back then returns to Settings. Support exits
into the unique conversation.
The app-owned scanner and system-owned picker, Sharesheet, document creator,
notification settings, and device-security settings return to the originating
route.

## Exact product copy

Primary route titles are **Settings**, **Share & Connect**, **Profile**,
**Profile Keys**, **Notifications**, **Appearance**, **Language**,
**Privacy & Security**, **Data Usage**, **Relays**, **Chat with support**, and
**Donate**. Share &
Connect exposes the explicit task label **Scan QR Code**. Relay roles are
**Profile**, **Inbox**, and **Chat Messages**. Unavailable states explain the
affected publishing, invitation, or new-chat function and route recovery.
Profile-key export dialogs use **Encrypted Private Key** and **Keep Your
Private Key Safe**; a failed document write uses **Couldn't Save File**.

## Android composition

Material 3 top app bars, lazy settings lists, `ListItem`, switches, selectable
radio rows, tabs, buttons, menus, and `AlertDialog` provide native hierarchy
and behavior. Settings details and ordinary app-owned dialogs/sheets use a
`surfaceContainerLow` canvas with `surfaceContainerLowest` grouped elements
and fields. Ordinary app-owned text and secure inputs, including inputs in
dialogs, use the shared 28 dp rounded tonal field component; Material still
owns editing, selection, focus, IME behavior, error semantics, and dialog
layout. The encrypted-export fields fill and align to Material's dialog text
slot and expose pinned-iOS-equivalent Low/Fair/Strong strength feedback. Share
& Connect uses a center-aligned title with native Back and
Share icon actions at their standard 48 dp targets. Its stable adaptive content
stays scrollable, preserves a bounded measure and uses semantic surface roles
while the QR alone remains literal black on white, with 12 dp of app-owned frame
padding, no additional encoder margin on this screen, the theme's less-rounded
16 dp `large` corners, a 16 dp gap from the public-key target, and a 1 dp gap to
its caption. An official filled rounded
`verified` symbol provides the optional address seal. The public-key capsule is
visibly 240 dp wide and 32 dp high at default text scale while its outer target
remains 48 dp. It lays out the full public key in the available interior and
applies middle ellipsis there, so the wider surface reveals more of the key
with balanced 16 dp directional padding.
The target and capsule share interaction state, but the bounded ripple is
clipped to the visible pill rather than painting the larger hit region. Scan QR Code
uses the shared primary task button across the content width in a pinned,
safe-area-aware bottom slot. The Settings root uses one
expandable identity card, one unheaded
seven-destination group, an unheaded Help group, and an isolated
destructive Sign Out group. Consumer details retain relationship-specific
section labels. Profile keeps its single save/scan completion task pinned.
Relays keeps Add Relay in its endpoint group. Support uses one compact identity
group, an aligned explanation and an inline Start Chat action. Donate uses
Material 3 Expressive's connected single-choice button group in the centered
app-bar slot for the peer Lightning and Bitcoin modes. Its identity code is not a Donate-local
composition: it reuses Share & Connect's adaptive marginless QR matrix, 12 dp
white frame and 16 dp corners plus the exact 240 × 32 dp public-key capsule
inside a 48 dp target. The complete address remains one line with middle
ellipsis, a clipped state layer and copied feedback. The QR-to-address target
gap remains 1 dp; the smaller `bodyMedium` method caption is optically pulled
up to 5 dp below the visible capsule without reducing the 48 dp copy target. It
does not duplicate that contextual copy action in a bottom bar. Forms use fully
rounded, label-above tonal fields
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
Profile Keys uses the same neutral canvas and white-equivalent grouped
surfaces. Public and private values use one-line monospaced presentation;
official copy, visibility, lock and download controls keep native 48 dp touch
targets. Supporting copy aligns to the shared 32 dp group-content line with an
8 dp relationship gap below its group. Export choices are ordinary list rows,
not oversized page buttons. Password entry and raw-export confirmation use
focused Material alert dialogs rather than a nested sheet toolbar or a custom
stack of full-width controls.
Appearance uses one transparent-resting Material radio group for Theme, a
short explanation outside that white-equivalent group, and one unheaded
Language disclosure group. Language is a typed child route with a top-spaced
white-equivalent group containing all eight accepted choices, native radio
semantics and the shared two-dp row separators. The active radio, rather than a
square selected background or iOS checkmark, communicates selection.
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
- Android Sharesheet and clipboard for explicit public/donation output; private-key
  clipboard content is marked sensitive so supporting system UI can redact its preview;
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
- [Material dialogs](https://developer.android.com/develop/ui/compose/components/dialog) — focused password entry, consequence confirmation, and document-write recovery.
- [Android settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings) — grouped hierarchy, subordinate screens, dependency explanation, and adaptive measure.
- [Switches](https://developer.android.com/develop/ui/compose/components/switch) and [radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button) — native boolean and mutually exclusive state semantics.
- [Material 3 button groups](https://m3.material.io/components/button-groups/overview) — the connected single-choice donation methods.
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input) and [BasicTextField API](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/BasicTextField.composable) — state-based, label-above profile, password, relay fields, and selectable read-only Profile values.
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility) — merged row semantics, read-only Profile state, QR purpose, and private-key exclusion.
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
- Scoped Profile Keys evidence:
  `wn-ios-prototype@4c25393:WhiteNoisePrototype/Screens/Settings/ProfileKeysSettingsView.swift`

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
- Profile defaults to read mode with full-contrast, selectable read-only fields
  and one low-emphasis app-bar Text button for Edit. Verified state is conveyed
  once inside the address field. Edit mode
  shows photo sources and one Save task; saving stays on Profile, while Back
  during editing restores the stored draft before leaving.
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
- Donate presents Lightning and Bitcoin as one connected radio-semantic button
  group, keeps one method selected, retains the 1 dp QR-to-address-target gap,
  and uses a compact `bodyMedium` caption 5 dp below the visible address pill.
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
- Profile Keys has exactly three compact public/private/export groups, keeps
  helper text outside those groups, exposes no redundant warning card or
  giant export buttons, and never publishes the private value through
  accessibility. Encrypted export requires matching password fields in one
  focused dialog; raw export names its consequence before Android's document
  creator opens; document-write failure gives a recoverable error dialog.
