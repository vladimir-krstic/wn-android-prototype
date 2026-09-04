# Android decisions

This file records only durable decisions that govern later Android work.

## WN-ANDROID-0001 — Clean Android restart

- Date: 2026-08-15
- Status: Approved by explicit user direction

The previous Android project, build output, IDE state, screenshots, scripts,
and documentation were removed from the working tree while `.git` and its
history were preserved. The repository restarts as a documentation and
reference foundation. A new Android project is created only when the first
screen or bounded flow is selected.

## WN-ANDROID-0002 — Product parity with Android-native presentation

- Date: 2026-08-15
- Status: Approved by explicit user direction

- The iOS prototype defines the product features, flows, copy, deterministic
  data, and important states to port.
- Android does not clone the iOS rendering. Kotlin, Jetpack Compose, Material
  3, AndroidX, Material Symbols, system navigation, Android system surfaces,
  and Android accessibility behavior define presentation and interaction.
- The White Noise identity remains black, white, and neutral gray in both
  light and dark themes. Semantic Android status and interaction colors remain
  available where meaning or accessibility requires them.
- User direction outranks both parity evidence and the platform default. An
  exception is recorded in the selected Android screen brief.

## WN-ANDROID-0003 — Self-contained pinned iOS evidence

- Date: 2026-08-15
- Status: Superseded on 2026-08-21 by WN-ANDROID-0026

The repository contains a read-only snapshot of the iOS docs, product source,
tests, UI tests, Xcode project metadata, and reusable resources. It was taken
from commit `58785a4724f33e23135c4dd3f98f231fca6a809d` plus the source
working-tree edits documented in `references/ios-prototype.md`.

Future implementation uses the local snapshot rather than depending on a
sibling checkout. Refreshing the snapshot is a separate explicit task because
it can change parity requirements.

This was the original port-bootstrap rule. After the scoped port reached its
static gate and the captured working tree became exactly reproducible from an
upstream commit, WN-ANDROID-0026 replaced the local-copy requirement.

## WN-ANDROID-0004 — Current official Android guidance is live authority

- Date: 2026-08-15
- Status: Approved

Official Android and Google developer documentation is an intentional live
authority for Android platform behavior. Before a material component,
navigation, permission, accessibility, adaptive-layout, or system-integration
decision, route through `references/android.md` and apply
`references/native-ui.md`.

Do not pin build tools, SDK levels, Kotlin, Compose, or library versions from
memory. Verify the latest stable compatible releases when the Android project
is bootstrapped or upgraded.

## WN-ANDROID-0005 — Prototype implementation boundary

- Date: 2026-08-15
- Status: Approved as initial parity boundary

Product state is deterministic and in memory. The port does not introduce a
backend, network transport, real Nostr or Marmot integration, cryptography,
real sign-in, or durable local storage without later user direction.

Device-owned flows already represented by iOS parity—camera capture, QR
scanning, media/file selection, sharing, playback, text-to-speech, and similar
system surfaces—may use the closest official Android API when their selected
screen is implemented. Sensitive or permission-heavy behavior must be
confirmed in that screen brief.

## WN-ANDROID-0006 — Just-in-time architecture

- Date: 2026-08-15
- Status: Approved

- Work on one selected screen or bounded flow at a time.
- Create the smallest app, package, state, fixture, and test structure needed
  by that work.
- Prefer a single app module, single activity, Compose, Material 3, and
  unidirectional data flow at bootstrap.
- Do not pre-create feature modules, service layers, repositories, databases,
  dependency injection, navigation destinations, or models for future parity
  items.
- Consolidate shared code only after actual repetition or a platform boundary
  proves the abstraction.

## WN-ANDROID-0007 — Verified native build baseline

- Date: 2026-08-15
- Status: Approved by successful clean build

The port starts with one `app` module, one exported launcher activity, and no
other exported component. The application ID is
`dev.ipf.whitenoise.android.prototype`; production source uses the
`dev.ipf.whitenoise` namespace.

The current verified baseline uses Android Gradle Plugin 9.3.1, Gradle 9.7.1,
compile/target SDK 37, minimum SDK 23, Java 17 bytecode, AGP built-in Kotlin
2.4.10, Compose BOM 2026.08.00, Material 3, and typed Navigation Compose.
Versions remain catalog-owned and may move only as a compatible, current,
stable set after a clean gate passes. Gradle moved from 9.7.0 to the officially
recommended 9.7.1 patch on 2026-08-21 and passed the same clean gate.

The app owns explicit light and dark monochrome Material color schemes and
does not use dynamic color. Android Navigation owns system and predictive
Back. The manifest contains no network or sensitive permission, disables
backup for deterministic prototype state, and declares only the launcher
activity.

## WN-ANDROID-0008 — Android-native onboarding capabilities

- Date: 2026-08-15
- Status: Approved by implementation and static verification

Onboarding uses full-screen Navigation Compose destinations instead of
recreating iOS medium/large sheets. This lets system and predictive Back, IME
resizing, top app bars, and task cancellation follow Android conventions.
Profile switching remains a bounded Material modal bottom sheet.

Private-key QR entry uses Google Code Scanner 16.1.0 because the task needs a
system-owned QR result rather than a custom camera experience. The app requests
no camera permission. Network and network-state permissions contributed by
the scanner's transitive manifest are explicitly removed; Play services owns
any scanner-module provisioning and the White Noise process remains offline.
Wrong content, cancellation, and unavailability are local UI states.

Avatar selection uses Android Photo Picker and `ACTION_OPEN_DOCUMENT`, with no
photo-library or storage permission. The accepted web-search and URL flows map
to 21 bundled images and never fetch remote content. AndroidX ExifInterface
1.4.2 corrects device-image orientation before the image is bounded to 512
pixels and held only in process memory.

## WN-ANDROID-0009 — Android-native Chats organization and creation

- Date: 2026-08-15
- Status: Presentation partially superseded by WN-ANDROID-0022

Chats uses a titled Material top app bar, primary scrollable scope tabs,
inline search, a New Message extended floating action button, lazy list rows,
and one discoverable overflow action sheet per row. These controls replace the
iOS titleless toolbar and multi-action swipe geometry while preserving all
accepted operations. Destructive and constrained actions use Material alert
dialogs; mute duration uses a modal bottom sheet.

All list surfaces project from the active profile's immutable chat records.
Pinning changes the projected partition without destroying fixture-relative
order, Read All mutates every non-archived record independent of the visible
filter, and direct/group creation copies current profile Chat Message relays
without network activity. Successful creation removes intermediary creation
destinations before opening the new conversation. Expanded list/detail
composition is intentionally completed with the real conversation surface in
Batch 3 rather than duplicating temporary UI.

## WN-ANDROID-0010 — One lifecycle-aware Compose conversation

- Date: 2026-08-15
- Status: Approved by implementation and static verification

Every direct, group, support, invitation, ended, recovery, and developer
catalog row opens one typed Compose destination. The screen uses a keyed
`LazyColumn`, derived sticky day headers, programmatic newest positioning, a
Material top app bar, semantic message surfaces, and Scaffold-owned bottom
content. A five-minute same-author derivation controls clustering; the stored
timeline remains a flat, chronological, immutable list.

Timeline dates and times are deterministic logical fixture values rather than
wall-clock or locale mocks. Support notices may intentionally precede the
first date section; sending the first message introduces Today. A bounded
Material text composer proves lifecycle transitions and authoritative preview
updates, while the complete attachment/media/speech composer remains Batch 4.
Failed send retry is visible and accessible without requiring a gesture. Full
message actions and reactions remain Batch 5.

Group author labels use a stable nine-color public-key bucket with a white
monogram fallback; visible names and semantics keep identity independent of
color. The rest of the conversation continues to use the app's adaptive
monochrome Material scheme.

## WN-ANDROID-0011 — Permissionless composer capabilities and local speech

- Date: 2026-08-15
- Status: Approved by implementation and static verification

The complete composer stores text, reply context, ordered attachments, and
link suppression only in its profile-owned chat record. Android Photo Picker,
Open Document, and an external `TakePicture` contract provide media/file/camera
capabilities without storage or camera permission. A non-exported FileProvider
owns camera cache URIs and copies of bundled PDF/video fixtures used for
explicit Android `ACTION_VIEW` handoff.

Link metadata is a deterministic local lookup and never fetches the URL.
Voice capture and transcription remain an explicit deterministic simulation,
so no microphone or speech-recognition capability is initialized. Recipient
Read Aloud uses Android TextToSpeech and shuts the engine down with the
composable lifecycle. Count-derived media grids and one shared Compose pager
replace iOS-specific composer and viewer geometry while preserving the accepted
one-to-seven media and Voice/Text/Both behavior.

## WN-ANDROID-0012 — Android message actions and in-place search

- Date: 2026-08-15
- Status: Context-sheet presentation superseded by WN-ANDROID-0107; named
  actions, state behavior, and in-place search remain current

Messages use Compose `combinedClickable` with haptic feedback for Android's
standard deliberate-hold interaction. Emoji configuration and forwarding use
Material modal sheets; every gesture-owned command is duplicated as a named
semantics custom action. WN-ANDROID-0107 replaces only the detached message-
action sheet with a source-preserving focused overlay.

Selection, reactions, replies, forwarding, deletion, and quick-reaction
preferences mutate only active-profile state. Message Details is a typed route.
Conversation search is a local projection rendered in the existing Scaffold:
it replaces the composer with previous/next/count controls, orders matches
newest first, and searches message text, sender names, attachment labels, and
link metadata without filtering or rewriting the timeline.

## WN-ANDROID-0013 — One adaptive Chat Info route family

- Date: 2026-08-15
- Status: Approved by implementation and static verification

Direct and group information share one typed Material list architecture whose
sections adapt from authoritative chat kind, membership, and role. Focused
Shared Content, Edit Group, Add People, member profile, and Chat Relays routes
stay separately addressable through Navigation Compose. Group administration
is model-gated as well as visually gated; every successful metadata, member,
role, timer, or leave mutation appends a deterministic timeline event.

Each chat captures its default relay set at creation. Relay normalization,
deduplication, final removal, send blocking, recovery, and restore therefore
remain independent of the profile defaults and sibling chats. Android Photo
Picker owns group-photo replacement without storage permission; native lists,
fields, bottom sheets, and alerts own the rest of the information hierarchy.

## WN-ANDROID-0014 — Profile-owned settings with system-surface boundaries

- Date: 2026-08-15
- Status: Approved by implementation and static verification

All ordinary settings are fields of the authoritative active `Profile`, so
switching profiles also switches appearance, notification preferences,
privacy choices, download policies, and profile-relay roles. Existing chats
retain relay lists captured at creation; only future direct/group/support
creation reads currently operational Chat messages roles.

Android owns interactions that already have a secure system surface: Photo
Picker, document providers, Sharesheet, clipboard, notification/security
settings, and permissionless Google Code Scanner. Android provides no QR
encoder surface, so ZXing core creates standards-compliant matrices rendered
by Compose Canvas. Private fixture keys are visually revealable by explicit
action but cleared from accessibility semantics, diagnostics, and logs. No
preference screen expands the prototype into real networking, notification
delivery, biometric authentication, cryptography, or payments.

## WN-ANDROID-0015 — Developer artifacts are profile state; exit consequences are model state

- Date: 2026-08-15
- Status: Approved by implementation and static verification

Developer Tools, diagnostic events, sanitized audit-file records, and the
single key package live inside each authoritative profile. The master gate
controls availability but does not own artifact lifetime: turning it off stops
its child features and preserves files, package, and events. Conversation debug
snapshots are read-time projections of the active chat. Copied summaries omit
message/profile/key content and derived group identifiers; no implementation
reads or writes Android logs.

Session exit and local erasure are separate atomic view-model operations.
Sign Out removes the active session and optionally its profile graph; Manage
Profiles can remove only another stored profile; Erase App Data clears every
profile and root-state field. Material bottom sheets own the two multi-field
destructive tasks, while focused Material alerts own profile removal and audit
clearing. Navigation reacts only to the returned typed exit destination, so a
screen cannot visually claim an outcome the state model did not perform.

## WN-ANDROID-0016 — Static parity handoff precedes device polish

- Date: 2026-08-15
- Status: Approved by implementation and static verification

The initial Android port is complete at the repository's static gate. Product
flows share one in-memory `AppViewModel` state graph and typed Navigation
Compose destinations; system capabilities remain explicit Activity Result,
settings, Sharesheet, clipboard, TextToSpeech, or Google Code Scanner
boundaries. Relay availability gates profile publication and future chat or
support creation in the model as well as the UI.

Batch 9 adds adaptive/RTL/large-font Compose coverage, resource-integrity
coverage, an adaptive monochrome launcher icon, decorative-image semantics,
safe private-key export confirmation, notification dependency enforcement,
and a dedicated support/recovery destination. Device execution, screenshot
comparison, and visual acceptance remain intentionally unclaimed until the
user selects each screen for the separate polish pass.

## WN-ANDROID-0017 — Product branding and Material roles remain strictly monochrome

- Date: 2026-08-20
- Status: Member-row treatment superseded by WN-ANDROID-0088; identity,
  fields, photo flow, and bottom action remain current

The launcher differentiator mark exists only to distinguish the installed
prototype icon. Android 12's system splash uses the exact White Noise mark from
Welcome, fitted inside the platform splash safe area, with light/dark polarity
owned by semantic resources.

Every ordinary Material 3 color role is explicitly grayscale in light and dark
themes so newly added component roles cannot fall back to baseline purple.
Semantic error colors remain available only where the UI communicates an
actual error or destructive consequence. Full-width task actions use the
current 56 dp medium Material button container with 24 dp horizontal content
padding; compact contextual actions retain their native compact size. On Sign
Up, the photo action is an 8 dp-attached compact filled-tonal pill rather than
an uncontained text action.

## WN-ANDROID-0018 — Ordinary form fields are outlined, labeled above, and lightly surfaced

- Date: 2026-08-20
- Status: Presentation superseded by WN-ANDROID-0023 and WN-ANDROID-0024

Ordinary form input uses Material 3 outlined text fields, including the secure
variant where required. Labels remain persistently above the container through
Material's native `TextFieldLabelPosition.Above` API rather than interrupting
the outline. A monochrome `surfaceContainerLow` fill gives the editable region
a slight surface distinction while the standard Material outline remains its
primary boundary and Material continues to own focus, error, disabled, and
accessibility state styling.

This rule applies to ordinary forms such as onboarding Name, About, and Private
Key. The bounded web-image task also applies it to Search Images and Image URL
because these are explicit form inputs rather than an app-level search mode.
App-level search bars and message composers remain specialized Android
component patterns and do not inherit the form-field presentation.

## WN-ANDROID-0019 — Shared Android grid and seamless launch-mark handoff

- Date: 2026-08-20
- Status: Shared grid remains approved; splash-matched Welcome mark
  superseded on 2026-08-26 by WN-ANDROID-0033

Custom White Noise layout uses Android's 8 dp grid with the 4 dp sub-grid only
for small component details. Compact panes use 16 dp horizontal margins;
closely related controls use 8 dp; peer form fields use 16 dp; distinct
sections use 24 dp; and pinned bottom task actions use 16 dp outer insets plus
system navigation and IME insets. Standard Material components retain control
of their internal metrics. `docs/ui-metrics.md` and `WhiteNoiseSpacing` are the
durable documentation and code authority unless explicit user direction or a
native component requirement overrides them.

The initial Welcome mark uses the same centered visible geometry as Android's
system splash: 149.5 × 115 dp within the platform's centered 288 dp icon
canvas. Welcome actions are independently pinned to safe drawing insets so
they never displace the mark during the splash-to-content handoff.

## WN-ANDROID-0020 — System picker surfaces use their platform defaults

- Date: 2026-08-20
- Status: Approved by explicit user direction and implementation

Android Photo Picker and Storage Access Framework launches use their standard
Activity Result requests without app accent or color extras. Android and the
device manufacturer own the complete picker appearance, including any dynamic
system color. External camera, Google Code Scanner, Android Sharesheet, and
system Settings follow the same system-owned rule. White Noise does not replace
these surfaces with custom UI merely to force monochrome styling.

## WN-ANDROID-0021 — Web-image selection is a near-full Android task sheet

- Date: 2026-08-20
- Status: Sheet boundary approved; field treatment superseded by WN-ANDROID-0024 and mode controls/insets refined by WN-ANDROID-0034

Find Image on Web opens a Material modal bottom sheet immediately at its
maximum state, capped at 94% of the available height. The visible underlying
screen, rounded top corners, drag handle, and scrim preserve an obvious modal
sheet boundary without forcing the task into Material's shorter partial anchor.
The modal dialog and scrim retain their full-window defaults; only the sheet's
intrinsic content height is bounded. Never apply a fractional height modifier
to `ModalBottomSheet` itself, because that changes the draggable surface's
measurement space and can leave uncovered content, incorrect system-bar color,
and broken entrance geometry.
A center-aligned top app bar owns the task title, a standard close icon button,
and a contained primary Done action; the sheet no longer uses detached edge
text actions. Search and URL are equal-width secondary tabs inside the task
hierarchy. Both inputs use the approved outlined, label-above, lightly surfaced
field treatment and the shared Android spacing tokens. The sheet, drag-handle
area, top app bar, and tab row share `surfaceContainerLow`; component defaults
must not create a brighter app-bar band inside this single modal surface.

## WN-ANDROID-0022 — Quiet Material 3 Expressive visual-polish system

- Date: 2026-08-21
- Status: Approved by explicit user direction after Chats and Settings pilots

The complete app enters a behavior-preserving visual-polish phase. The target
is a quiet Material 3 Expressive system: strong hierarchy, selective scale,
semantic monochrome color, deliberate tonal surfaces, a shared Material type
and shape scale, standard icons, and native motion. Expressiveness clarifies
the primary task and selected state; it does not add decorative containers,
Google branding, Pixel screenshot imitation, or a new information
architecture. Figma is not required for implementation.

Chats uses a clean top app bar with active-profile avatar, visible active-scope
title, filter action, and collapsed search action. Chats, Unread, Archived,
and Left move from exposed tabs to a Material dropdown menu; a non-default
scope uses both the visible title and tonal selected filter treatment. The
conversation list has no enclosing rounded container, and ordinary rows do
not become individual cards. Read All is not a persistent app-bar action, but
the tested model operation is not deleted as part of visual work.

The Chats avatar opens Settings. The Settings profile header owns Switch
Profile and Add Profile access, while the existing settings destination
hierarchy remains intact. Settings groups use restrained tonal containment
instead of a divider after every row. All action glyphs migrate to Material
Symbols or existing product vectors with standard targets and semantics.

Implementation proceeds through the bounded batches, state contract, and
acceptance gates in `docs/visual-polish.md`. Product behavior, typed routes,
system surfaces, accessibility, deterministic state, and the offline boundary
remain authoritative throughout.

## WN-ANDROID-0023 — Rounded Expressive fields and medium task controls

- Date: 2026-08-21
- Status: Shape and task-control sizing remain approved; field surface and state treatment superseded by WN-ANDROID-0024

Ordinary app-owned text, secure, and multiline fields retain their established
labels, helper/error text, tonal fill, and outlined state treatment, but all
four corners now use the shared 28 dp extra-large shape. A 56 dp single-line
field therefore reads as a full capsule; taller multiline fields retain the
same fixed 28 dp radius at every corner. App-level search and the message
composer use the same full-rounded field language. Android- and OEM-owned
pickers and settings remain untouched.

A single-line Material text field and a Material Expressive medium button are
both 56 dp high. White Noise therefore uses the official medium button height,
padding, and pill shape for full-width, form-adjacent, pinned, and other clear
task actions. Text buttons, dialog actions, icon buttons, toolbar actions,
photo/source controls, reaction controls, media controls, and other contextual
actions retain their native compact metrics. Multiline fields may grow with
content; their paired task button does not stretch to match that expanded
height.

The latest user-approved White Noise direction intentionally uses a larger
radius than Material's current medium `roundedShape` recommendation. The
pinned stable Compose Material 3 version also predates the convenience
`TextFieldDefaults.roundedShape` and
`OutlinedTextFieldDefaults.roundedShape` properties and the named medium-button
metrics now visible in the live API/reference source. White Noise therefore
uses its existing `MaterialTheme.shapes.extraLarge` 28 dp role for every
app-owned text input and the exact 56/24/8 dp medium-button metrics without
changing dependencies solely for convenience aliases.

## WN-ANDROID-0024 — Tonal form fields align labels to their content line

- Date: 2026-08-21
- Status: Approved by explicit user direction and implementation

Ordinary app-owned text, secure, and multiline form fields use the existing
fixed 28 dp extra-large shape on a stronger `surfaceContainerHigh` tonal
container. The resting and disabled borders are transparent. Focus uses a
2 dp full-shape `primary` ring, and error uses a 2 dp full-shape semantic
`error` ring plus supporting copy and accessibility error semantics. Disabled
fields move to `surfaceContainerLow` while Material retains disabled content
colors. App-level search bars and message composers remain specialized
Material patterns and do not gain a persistent outline.

The input text or placeholder begins 16 dp from a field edge. Above-field
labels and supporting/error text align to that same 16 dp directional content
line, including right-to-left layouts; a 24 dp leading field icon occupies
Material's standard 48 dp slot while its visible edge also begins on the
16 dp line. This is a shared component rule, not a screen-local optical offset.
Material's state-based text editing, label/supporting typography, selection,
cursor, icon slots, and animated container state remain authoritative.

## WN-ANDROID-0025 — Settings overview uses compact icon-led destinations

- Date: 2026-08-21
- Status: Approved by explicit user direction and implementation

The main Settings overview uses Material `ListItem` destinations with one
concise headline, one 24 dp rounded Material Symbol, and a trailing disclosure
chevron. Explanatory subtitles that merely restate a destination are removed.
Secondary text remains available only when it communicates an actionable
unavailable reason; a short current value may instead sit beside the trailing
chevron when it helps scanning. Appearance shows its active preference this
way. The profile-relay dependency remains visible when Profile is unavailable.

The symbols are decorative within the complete clickable row and therefore
have null accessibility descriptions; the visible label, optional value or
reason, button role, enabled state, and row action remain authoritative.
Destructive destinations retain semantic error color for both label and icon.
Section headings above tonal groups use a 32 dp directional inset: the 16 dp
compact screen margin plus Material's 16 dp list-item content inset. This
aligns headings with overview icons and icon-free detail-row text rather than
with the outer container boundary. Restrained tonal groups, row order, typed
navigation, and profile-owned state do not change.

## WN-ANDROID-0026 — Completed port uses a pinned upstream iOS reference

- Date: 2026-08-21
- Status: Approved by explicit user direction

The scoped Android port has reached its complete static implementation gate,
so the copied iOS source tree is no longer retained in this repository. The
former snapshot is exactly reproducible from the private `wn-ios-prototype`
repository at commit `0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`:
its original base was commit `58785a4724f33e23135c4dd3f98f231fca6a809d`,
and its eight captured working-tree edits are exactly the eight files committed
by `0bd7cba`.

Android briefs, decisions, tests, and the parity ledger remain the working
authority. When they are insufficient, `docs/port/source-map.md` identifies
the minimum upstream iOS evidence to inspect at the pinned commit. A newer iOS
commit does not silently change Android scope; comparing or syncing later iOS
work requires explicit user direction. The iOS repository remains neither a
runtime nor a build-time Android dependency.

## WN-ANDROID-0027 — Sign Up uses one IME-aware task surface

- Date: 2026-08-25
- Status: Approved by explicit user direction after Pixel 8a inspection

Sign Up no longer reserves a pinned bottom action above the software keyboard.
Its primary action follows Name and About inside the same bounded vertical
scroll surface, separated from the form by the shared 24 dp section spacing.
That surface owns IME padding, while Scaffold and Material continue to own
safe-drawing inset consumption and keyboard animation. This supersedes the
Sign Up-specific pinned-action portions of WN-ANDROID-0008 and
WN-ANDROID-0019; other task screens retain their approved presentation until
they receive their own device polish pass.

The small top app bar remains pinned and now receives Material scroll behavior
so its existing scrolled tonal container communicates content overlap instead
of producing an unmarked hard crop. The optional avatar is 120 dp, restored
after explicit user review, and Sign Up's
scrolling content uses a 16 dp top/bottom inset; the accepted 8/16/24 dp
relationship rhythm, 56 dp primary button, full-width compact form measure,
Back behavior, copy, state, and accessibility semantics remain unchanged.

## WN-ANDROID-0028 — Unhandled app-background taps dismiss text input

- Date: 2026-08-25
- Status: Approved by explicit user direction and implementation

The application shell clears Compose focus after a complete pointer tap that
descendant content leaves unconsumed. Clearing the focused text editor lets
Compose and the Android input system dismiss the software keyboard without
manually forcing IME visibility. This behavior applies across app-owned
destinations and has no visual, semantic, or state-layer representation.

The shell observes the final pointer-event pass and never consumes events.
Material controls, clickable rows, text fields, scrolling, dragging, sheets,
dialogs, and system-owned surfaces therefore retain their existing gesture and
focus behavior. A consumed press, a drag, or a canceled gesture does not clear
focus. This low-level pointer observer is the smallest cross-app implementation
because Compose has no standard app-shell background-tap focus modifier.

## WN-ANDROID-0029 — Processing primary buttons retain primary emphasis

- Date: 2026-08-25
- Status: Approved by explicit user direction and implementation

An app-owned primary task button distinguishes loading from unavailability.
While its action is processing, it blocks duplicate activation but retains the
theme's `primary` container and `onPrimary` content. A compact indeterminate
indicator appears beside a stable, action-specific progress label, and the
button exposes **In progress** as its accessibility state. Motion is therefore
not the only status cue and the 56 dp task-button geometry does not change.

Material's ordinary disabled colors remain authoritative when an action is
genuinely unavailable, such as Sign In before a valid Private Key exists. The
shared `WhiteNoiseButton` owns this distinction so current Sign In and Sign Up
loading states, and future primary task loading states, do not reproduce the
disabled-gray plus low-contrast inline-indicator treatment.

## WN-ANDROID-0030 — Sign In groups equivalent key-entry actions

- Date: 2026-08-25
- Status: Approved by explicit user direction after Android/iOS comparison

Manual entry, paste, and QR scanning are three ways to supply the same Private
Key, so Sign In presents them as one compact Material control cluster. Paste
uses the secure text field's native trailing-icon slot and changes to Clear
after entry. QR scanning uses a separate 56 × 56 dp filled-tonal icon button
immediately beside the field with the shared 8 dp related-control gap. Both
actions use current rounded Material Symbols and action-specific accessibility
labels; the field continues to own its persistent label, helper/error text,
secure editing, focus, and validation semantics. The 40 dp visible trailing
control retains Material's 48 dp touch target and uses 4 dp horizontal optical
padding so its state-layer circle is concentric inside the 56 dp field cap and
does not crowd the secure mask or cursor.

This supersedes the content-width labeled QR button presentation in the
onboarding brief without changing scanner behavior. Google Code Scanner still
owns the permissionless camera surface and its success, cancellation, wrong
content, and unavailable states. The Android full-screen destination, system
Back, bounded form measure, pinned primary action, and loading treatment remain
unchanged.

## WN-ANDROID-0031 — Sign In owns a monochrome camera scanner

- Date: 2026-08-25
- Status: Presentation and route ownership superseded on 2026-08-25 by
  WN-ANDROID-0032; CameraX, ML Kit, permission, and result behavior remain
  approved

The Private Key scanner replaces Google Code Scanner with an app-owned CameraX
preview and bundled on-device ML Kit QR analysis. This is an explicit
Sign-In-only exception to WN-ANDROID-0008 and WN-ANDROID-0020: the user chose a
custom White Noise camera experience over the permissionless Google-owned
surface after inspecting its multicolor target, bright system-bar bands, and
Google attribution. Share & Connect continues to use its existing system-owned
scanner until that separate flow is selected.

The scanner is a typed Navigation Compose destination that draws edge to edge
on black, with the live preview filling one safe-drawing-aware 28 dp rounded
card. App-owned close and flashlight controls, a centered task label, and four
black chamfered target corners with a thin white contrast under-stroke replace
the Google surface. System and predictive Back return to the in-memory Sign In
draft. Android's camera privacy indicator remains system-owned and is never
hidden or imitated.

White Noise now declares `CAMERA` and requests it only after **Scan QR Code**
is selected. Denial remains inside the scanner task with **Allow Camera** or,
after permanent denial, **Open Settings** recovery; Back always returns to the
unchanged Sign In draft. Camera or analyzer setup failure returns to Sign In's
existing unavailable dialog. Valid and wrong-content results return through
an ephemeral in-memory navigation bridge, keeping the raw value out of route
arguments and saved-state storage while preserving accepted field replacement,
error, retry, and sensitive-value handling. CameraX 1.6.1
and bundled ML Kit Barcode Scanning 17.3.0 are first-party, stable, offline
runtime dependencies; the implementation adds no network capability.

## WN-ANDROID-0032 — Private Key scanning is a near-full Material task sheet

- Date: 2026-08-25
- Status: Approved and refined by explicit user direction after Pixel 8a
  inspection

The custom full-window scanner, inset camera card, floating title and
instruction capsules, and double-stroked target did not meet the approved
quiet Android-native direction. Sign In now owns scanning as an immediately
expanded Material modal bottom sheet capped at 94% of the available height.
The underlying Sign In destination remains visible under Material's scrim;
the sheet owns its rounded top boundary, standard drag handle, swipe dismissal,
and system Back behavior.

The CameraX preview fills the complete sheet content and is clipped only by
the sheet. Material's drag handle is explicitly centered, while a transparent
center-aligned top app bar overlaps its lower area instead of being stacked
below the handle's full slot; Close, the visible **Scan QR Code** title, and
flashlight therefore occupy the compact top-sheet position. A restrained top
scrim preserves contrast. The target is one 3 dp white rounded-corner bracket
set with a 24 dp curve and no inner stroke, Google brand colors, detached
camera card, outer gutter, title capsule, or instruction capsule.

Google Code Scanner cannot be hosted or restyled inside an app-owned Material
sheet: Google Play services owns that complete surface and returns only the
scan result. The accepted CameraX 1.6.1 and bundled on-device ML Kit 17.3.0
implementation therefore remains the correct custom-content boundary. Camera
permission, denial/settings recovery, torch, analyzer failure, valid/wrong
content handling, and ephemeral Private Key return remain unchanged. The
former typed scanner navigation route is removed; close, swipe, and Back
dismiss the sheet without changing the Sign In draft, while a valid scan fills
the existing field after dismissal.

## WN-ANDROID-0033 — Welcome logo scales within the space above its actions

- Date: 2026-08-26
- Status: Approved by explicit user direction after Android/iOS comparison

Welcome uses a larger proportional White Noise mark, centered between the
bottom of the top safe area and the top edge of Sign In, rather than at the
full-window center. Add Profile retains its native top app bar and centers the
mark below it. A weighted Compose region above the measured native action
group preserves this relationship as insets, window size, and text size change.
Welcome's safe bounds include system bars and display cutouts, not the IME
from an outgoing form. App-bar Back from Sign In/Sign Up clears focus and
requests keyboard dismissal before the native navigation pop; no delayed or
replacement navigation animation is introduced.

The mark uses 50% of the safe content width, capped at 260 dp (half the existing
520 dp form measure), and preserves the original 598:460 aspect ratio. It
shrinks when necessary to leave 16 dp of vertical breathing room in short
windows. Native button sizing, the 8 dp action gap, monochrome colors, and
16 dp horizontal and bottom margins remain unchanged.

This explicitly supersedes WN-ANDROID-0019's identical splash/Welcome geometry.
Android still owns the centered, safe-area-constrained system splash; its
149.5 × 115 dp visible mark is not enlarged or moved to match Welcome. The
iOS screenshot is evidence for the accepted proportion and spatial balance,
not authority for Android buttons, safe-area values, navigation, or motion.

## WN-ANDROID-0034 — Web-image modes follow the native Photo Picker button pattern

- Date: 2026-08-26
- Status: Implemented from user-requested native-picker review; device acceptance pending

Find Image on Web keeps its near-full Material modal boundary, centered native
top app bar, compact filled Done action, privacy copy, and deterministic
Search/URL behavior. Search and URL now use equal-width `FilledTonalButton`s
with the theme's medium shape and semantic selected colors, matching the
public component composition in the current AOSP Photo Picker. This is not a
new tab API or a claim that Material's underline tabs are obsolete. The
buttons retain native padding, minimum targets, focus, and ripple, and add
selected-state/tab semantics to the same single click target. No dependency
upgrade or custom-drawn segmented control is needed.

Use 16 dp outer mode-row margins and an 8 dp gap. Done retains its compact
native content padding but receives a total 16 dp directional trailing inset;
the app bar's default 4 dp edge clearance is intended for icon actions. The
single-line title inherits the app bar's typography and ellipsizes if needed.

The modal owns safe drawing, cutouts, and keyboard insets. Its nested app bar
does not repeat status padding, and its content does not repeat IME padding.
Only the content height is capped; the modal/scrim, handle, shape, and width
remain Material-owned. Search uses a full-span privacy/input header in its
lazy grid, and URL input/preview shares one scroll surface, keeping controls
reachable at large text or short window heights. There is a 24 dp section gap
before results/preview, while the square image grid retains its 2 dp media
gutters without an enclosing decorative card.

The supplied screenshots are review evidence, not verification of the new
build. Native Photo Picker/Files appearance, network exclusion, and all
existing catalog results and selection/validation behavior remain unchanged.

Source: [AOSP Photo Picker navigation buttons](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/main/photopicker/src/com/android/photopicker/features/navigationbar/NavigationBar.kt),
[Material tabs](https://developer.android.com/develop/ui/compose/components/tabs),
[Material insets](https://developer.android.com/develop/ui/compose/system/material-insets),
and [Compose lazy grids](https://developer.android.com/develop/ui/compose/lists).

## WN-ANDROID-0035 — Minimal Chats and multi-action native-button reveals

The swipe and toolbar-creation portions are superseded by WN-ANDROID-0038.
The row organization, filters, alignment and Undo contract remain in force.

- Date: 2026-08-26
- Status: User-approved plan implemented; static gate passed; device acceptance pending

The default Chats scope has no visible title and includes all non-archived
rows, including left/removed conversations. Unread includes all non-archived
unread rows. Archived and Left keep their existing respective scopes. The
active non-default scope uses a filled monochrome Material filter button with
selected-state semantics. Filter, Search, and New Message live in the toolbar;
Check Relays replaces creation in that slot when necessary. There is no FAB.
This supersedes older Chats-title/FAB/ellipsis and ended-membership-exclusion
decisions for this screen only.

Plain Material list rows use semibold names, two-line previews, baseline
timestamps, avatar pin badges, inline state symbols, primary-role unread
badges, and semantic-error failure icons. `ChatListPresentation` serves both
display and search. Avatar artwork aligns at the pane's 16 dp leading margin;
toolbar/list diameters remain 40/52 dp. Existing fixture text/time/order remain
authoritative; no timeline overhaul is included.

The user explicitly selected multi-action reveals. Standard Material
swipe-to-dismiss handles directional update/dismiss, not the required several
independently selectable actions per side. A small AndroidX
`anchoredDraggable` container therefore reveals Material tonal icon buttons
with native targets, tooltip labels, focus, and ripple. Leading order is
Read/Unread then Pin/Unpin; trailing order is Mute/Unmute, Archive/Unarchive,
then eligible Leave/Delete. Full leading swipe may perform read-state change;
trailing swipe never performs a destructive action. Action availability is
shared with long-press sheets and TalkBack custom actions. One row is open;
Back, another reveal, navigation, scope/profile changes and reordering close
it. Read/archive changes have conditional profile-bound field-level Snackbar
Undo. Existing mute durations, confirmations and sole-admin protection remain.

Scoped iOS evidence: `4c25393` ChatsView, NativeChatList, ChatListRow and
ChatListItem. This is not a global upstream baseline refresh.
Sources: [Material swipe guidance](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss),
[app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
and [semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

## WN-ANDROID-0036 — Shared pinned headers follow actual content scroll

- Date: 2026-08-26
- Status: Implemented; static gate passed; device acceptance pending

`WhiteNoiseScaffold` scopes a native pinned scroll behavior to each ordinary
destination. `WhiteNoiseTopBar` accepts that behavior; shared Settings lists,
ordinary forms, creation/info screens, conversation timelines, details, and
shared-media grids connect their real state. Actual list/grid/scroll offsets
supplement nested-scroll events so restored/programmatic positions, empty
content, and return-to-top get the correct tonal state. Material owns the
surface-to-surfaceContainer transition. Camera/media and selection bars retain
their specialized styling. The shared pane's maximum width is constrained
before fillMaxWidth so header and list have the same effective measure.

Chats gives the LazyColumn the full bottom viewport; navigation clearance is
scrolling content padding rather than an outer fixed white strip. Native Back
buttons and conversation-title avatars do not move onto the profile-avatar
grid.
Sources: [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars)
and [Material inset handling](https://developer.android.com/develop/ui/compose/system/material-insets).

## WN-ANDROID-0037 — Profile-owned first-login privacy choices

- Date: 2026-08-26
- Status: User-approved scoped iOS delta implemented; static gate passed; device acceptance pending

Use iOS `4c25393` DiagnosticsState, DiagnosticsAndImprovementsViews and root
presentation as scoped product/copy evidence. Successful initial or Add
Profile onboarding schedules an unseen profile's **Help Improve White Noise**
sheet; its Chats navigation entry must reach RESUMED before presentation.
Use a content-height Material sheet, native handle/Close, scrollable copy and
two native switch rows, both initially off. Apply immediately, with no Save or
Continue. Only an actual Close/Back/scrim/swipe dismissal records prompt-seen
for the captured profile; rotation/disposal does not.

`Profile.diagnostics` owns independent analytics/logging choices, seen state,
and deterministic retained records. Privacy & Security links to the typed
Diagnostics & Improvements destination with summary, switches, retained size,
and confirmed clearing. Developer Tools no longer owns or gates consent.
Master-off disables debug only; logging-off retains records. Clear zeros
record contents without changing preferences. Stored profiles preserve these
values; wiping removes them with the profile graph.

Exact accepted production wording is preserved; the prototype performs no
telemetry collection, upload, networking or persistence and adds no SDK or
dependency upgrade. See `docs/screens/diagnostics-and-improvements.md`.
Sources: [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
and [native switches](https://developer.android.com/develop/ui/compose/components/switch).

## WN-ANDROID-0038 — Chats uses anchored long-press menus and a native FAB

Accepted: 2026-08-26. Supersedes WN-ANDROID-0035's swipe and toolbar-creation
decisions at the user's explicit request, not a change to product capability.

The scoped reference is Signal Android `441ba42c3f3175476a1f54eba8e72d8d6d304db7`,
`ConversationListFragment.java` lines 1373–1465: highlight a held conversation
and anchor its actions to that row. No Signal implementation, extra commands,
bulk selection or folders are imported. Material `DropdownMenu` owns native
placement above/below the row, scrolling, focus, RTL and dismissal. Order stays
Read/Unread, Pin/Unpin, Mute/Unmute, Archive/Unarchive, eligible Leave/Delete,
even above the anchor. Store only profile/chat IDs, resolve live eligibility,
dismiss before dispatch, and clear on Back/outside/navigation/scope/profile
change or anchor removal. `combinedClickable` supplies native hold feedback;
TalkBack custom actions remain. Remove ChatSwipeRow and ChatActionsSheet.

Restore a standard icon-only Material FAB with Google's rounded 24 dp Edit
symbol, primary/onPrimary and native size/shape/elevation. Scaffold owns safe
placement and Snackbar clearance; expanded windows add only the centered
content-pane inset. Hide it during search and preserve Check Relays recovery.
The list remains edge-to-edge with scrollable 88 dp + safe-bottom clearance.

`ChatListPresentation.status` is exclusive: invitation, visible failure,
unread count, manual unread or none. Draft/membership overrides still suppress
stale failures. Manual unread and failure use the numbered badge footprint,
growing with `labelSmall` at font scale. Only wider counts expand horizontally.
Failure hides unread visually but preserves model state, filters and actions.
Existing Undo and destructive/sole-admin safeguards are unchanged.

The subsequent user-requested row refinement places mute, timer and ended-
membership symbols immediately after the single-line title. The text layout
reserves their widths and the separate timestamp before ellipsizing the
name; short names do not stretch away from their symbols. Full-name semantics,
baseline alignment and RTL remain native. Invitations now share the same
font-scaled primary circle as unread counts, containing Google's unchanged
Add symbol in onPrimary. This is a layout/status-artwork change only, not a
fixture, timestamp, preview or action-policy change.

The next row-spacing refinement uses the pinned library's clickable `ListItem`
overload, not custom drawing: its native avatar/content gap is 12 dp instead
of 16 dp. An 8 dp anchor inset plus 8 dp inner horizontal padding keeps the
visible avatar at the shared 16 dp content edge. Hold the native selected
shape while highlighted, preserving ordinary Button semantics rather than
adopting the single-selection overload's RadioButton role. The menu gains
8 dp transparent top/bottom popup padding outside its native group; this
separates above- and below-anchor placement without overriding Material's
position provider. Other popup entry points and action/state behavior remain
unchanged. No dependency or theme change.

The subsequent Pixel scroll-crash repair keeps this native interactive row
but moves headline/preview/status into one measured content region. Captured
`LayoutNode … not found in RectList` stacks reach alpha25's supporting-slot
baseline query during both lazy measurement and prefetch. `ChatRowTextLayout`
avoids inherited `Row`/`Box` baseline queries, reads only direct `Text`
baselines and places children only during placement. This narrowly scoped
layout exception preserves the pinned native 72/88 dp minimum heights,
padding, alignment and larger-text growth; all interaction, shape, menus and
dependency pins remain native/unchanged. Do not disable lazy prefetch, swallow
framework exceptions or redesign unrelated list controls as a workaround.

Evidence: `ChatContextMenu.kt`, `ChatListRow.kt`, `ChatsScreen.kt`,
`ChatListPresentation.kt`, `ChatListPresentationTest`, `ChatsPolishTest`,
`ChatListRowTest`, `ChatListScrollRegressionTest`.
Sources: [Signal reference](https://github.com/signalapp/Signal-Android/blob/441ba42c3f3175476a1f54eba8e72d8d6d304db7/app/src/main/java/org/thoughtcrime/securesms/conversationlist/ConversationListFragment.java#L1373-L1465),
[Material menus](https://developer.android.com/develop/ui/compose/components/menu),
[FAB](https://developer.android.com/develop/ui/compose/components/fab),
[badges](https://developer.android.com/develop/ui/compose/components/badges),
[Compose custom layouts](https://developer.android.com/develop/ui/compose/layouts/custom),
[exact ListItem/Menu implementation and tokens](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.5.0-alpha25/material3-android-1.5.0-alpha25-sources.jar).

## WN-ANDROID-0039 — Ordinary sheets share one Material surface and header

Accepted: 2026-08-26, explicitly app-wide for ordinary app-owned sheets.

`WhiteNoiseModalBottomSheet` centralizes `surfaceContainerLow` and one
safeDrawing/IME owner; native Material owns the handle, shape, width, motion
and dismissal. Ordinary list rows are transparent. Preserve fields, deliberate
tonal groups, selected states and error roles instead of flattening them.
`WhiteNoiseSheetHeader` starts immediately after Material's 48 dp handle slot:
titleLarge, 24 dp directional text margins, 8 dp body gap, wrapping title and
an optional trailing native Close button. No extra top spacer. Task sheets
with Close/Done keep a matching native app bar with zero repeated insets.

Applied to profile switching, diagnostics, composer attachments/contact/GIF/
voice review, message actions/reactions/configuration/forwarding, disappearing
timer, Sign Out/Erase, and Web Image. Camera, full-screen media, contextual
selection and platform/OEM-owned surfaces keep their specialized presentation.

The two mute sheets become one `MuteDurationDialog`: native AlertDialog,
scrollable immediate-choice rows (1 Hour, 8 Hours, 1 Day, 1 Week, Always) and
Cancel. Back/outside dismissal changes nothing. Neither this change nor the
header refactor alters diagnostics consent or its dismissal lifecycle.

Control icons are Google's unmodified rounded Material Symbol Android XML
downloads, recorded in `docs/references/material-symbols.md`. No icon library,
custom path drawing, dependency upgrade, collection, persistence or transport.
Evidence: shared components and their call sites; `MaterialSheetTest`,
`ChatInfoScreenTest`, `DiagnosticsPromptTest`, and retained flow tests.
Sources: [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
[dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
[official icon guidance](https://developer.android.com/develop/ui/compose/graphics/images/material).

## WN-ANDROID-0040 — App-wide official Expressive menus, scoped prerelease exception

Accepted: 2026-08-26. The user explicitly requested replacing all app-owned
dropdowns after the distinction between published Material design and library
release status was explained. This supersedes WN-ANDROID-0038's baseline menu
component, not its long-press behavior, safeguards or action policy.

Pin Material 3 **1.5.0-alpha25**, leaving Compose BOM 2026.08.00 and all
unrelated versions unchanged. Current latest alpha26 was inspected and tried;
its ripple AAR requires minSdk 24, causing the manifest gate to reject it.
Alpha25's Material and ripple AARs both support the existing minSdk 23 and
contain the public Expressive menu family. Do not silently raise minSdk or
use an unsafe manifest override. Compose UI/Foundation continue resolving to
the BOM's stable 1.12.0. This is a scoped prerelease exception, not blanket
permission for other dependency upgrades. Menu APIs graduated in May 2026;
the artifact still being prerelease does not make Google's design experimental.

`WhiteNoiseDropdownMenu` composes native `DropdownMenuPopup`,
`DropdownMenuGroup` and shape/selection-aware `DropdownMenuItem` overloads.
Use standalone `MenuDefaults.groupShapes`, position-aware item shapes,
standard surface-based colors and native internal metrics. Do not copy
Signal's custom popup dimensions or merely round the baseline menu. A standard
Compose vertical scroll sits inside the rounded group because this new popup
does not add scrolling itself. Material owns popup positioning, clipping,
motion, focus, RTL, touch targets and Back/outside dismissal. Scope/format
choices use native radio semantics and the selected leading check. Explicit
`selectableItemColors` are used for commands too, avoiding alpha25's known
baseline `itemColors` partial-override issue. Destructive text/icons retain
semantic error colors; disabled and selected defaults are preserved.

All six entry points use this implementation: Chats context actions, Chats
filters, Sign Up photo sources, Set Up Group photo sources, Profile photo
sources, and voice-review format. Dismiss before dispatch. Preserve live chat
eligibility, action order, Undo, confirmations, sole-admin guards, source
pickers, in-memory state and privacy behavior. No product copy changes.

The accompanying code audit found that task buttons still combine medium
container metrics with baseline label typography/no expressive pressed-shape
behavior, and the global theme still uses standard motion. Those are partial
Expressive adoption, not invalid Android components; a full-Expressive claim
was too broad. They are documented, not silently redesigned. The accepted
rounded form fields are deliberate local styling. Ordinary sheets, dialogs,
FAB and system picker boundaries remain valid native patterns. See
`docs/screens/app-menus.md` for audit evidence and verification.

Sources: [Material menus](https://m3.material.io/components/menus/overview),
[specifications](https://m3.material.io/components/menus/specs),
[release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3),
[exact alpha25 source](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.5.0-alpha25/material3-android-1.5.0-alpha25-sources.jar).

## WN-ANDROID-0041 — Settings consolidates profile management at the root

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

The Settings root begins with one compact `surfaceContainerLow` profile group.
Its active row uses a 56 dp avatar, name, shortened public key, official
`qr_code_2` symbol and disclosure, and opens the existing Share & Connect
destination. A pure profile-management projection drives the second row:
Add Profile with `person_add`, one named alternate, or Switch Profile with at
most three overlapping 32 dp alternate avatars and a remainder badge. Alternate
rows open the switcher and never silently change the active profile.

Profile, Profile Keys, Notifications, Appearance, Privacy & Security, Data
Usage and Relays form one unheaded destination group. Help remains separate;
Sign Out remains isolated and semantic error-colored; the Android version
footer remains. Remove Manage Profiles from the root API and presentation but
retain its typed route and implementation for compatibility. Appearance no
longer exposes a redundant root summary; System default remains selectable in
the Appearance screen. This supersedes WN-ANDROID-0022's former Settings
hierarchy and WN-ANDROID-0025's Appearance trailing-value rule only for this
root surface.

The shared Material modal profile switcher keeps active-first order followed
by deterministic stored order. Native two-line rows use 48 dp avatars; the
active row owns selected tone and a check, while inactive rows may show
aggregate non-archived active-membership unread counts capped at `99+`. The
active check suppresses its unread badge without mutating unread state. A
full-width Add Profile task is pinned below the scrollable list. Selection
applies immediately and closes; Back, scrim or swipe dismissal changes
nothing. Profile-owned chats, settings, diagnostics, relays, drafts and unread
state remain untouched.

Official Google-hosted rounded Material Symbol XML supplies the Settings
artwork; no icon or runtime dependency is added. Scoped iOS commit `4c25393`
is authorized only for the current Settings organization and profile lifecycle
evidence. Android `ListItem`, selected containers, modal sheet behavior,
insets, targets, type growth, RTL and dismissal remain native.

Evidence: `ProfileManagementPresentation.kt`, `ProfileSettingsScreens.kt`,
`ProfileSwitcherSheet.kt`, `ProfileManagementPresentationTest`,
`SettingsScreenTest`, and the exact Add Profile fixture-set assertions in
`AppViewModelTest`.
Sources: [Android settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
[badges](https://developer.android.com/develop/ui/compose/components/badges),
[official icon guidance](https://developer.android.com/develop/ui/compose/graphics/images/material).

## WN-ANDROID-0042 — Profile switching is a separate root control

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

The Settings root no longer renders Add Profile or an alternate identity as a
second row joined to the active-profile card. The active identity remains a
compact tonal Share & Connect row. A separate one-line tonal **Switch Profile**
control follows with an 8 dp gap, a trailing 32 dp profile preview and tonal
expand affordance. It always opens the existing Material profile sheet; Add
Profile remains the pinned action inside that sheet, including when only the
active profile exists. This follows the hierarchy of Google's collapsed
account switcher without reproducing a system account surface or inline
expansion.

The former internal divider is removed. The Help destinations remain a
separate tonal group but no longer carry a visible Help heading, matching the
root's unheaded group organization. This supersedes only WN-ANDROID-0041's
joined adaptive second-row presentation and labeled Help-group wording. Pure
Add/single/multiple projection remains authoritative for trailing previews and
switcher tests.

## WN-ANDROID-0043 — Settings uses an inline Material profile card

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

The Settings root uses `surfaceContainerLow` as its neutral page canvas and
`surfaceContainerLowest` for its white-equivalent cards. Its pinned app bar
matches the canvas at rest and changes to `surfaceContainer` when content
passes underneath. This follows Material's semantic surface hierarchy in both
themes instead of hard-coding white and gray.

The active 56 dp Share & Connect identity and its profile-management control
now share one rounded card without an internal divider. With no alternate
profile, the outlined control reads **Add Profile** and starts the existing Add
Profile flow directly. With alternates, it reads **Switch Profile** and expands
the same card. The expanded region shows inactive profiles in deterministic
stored order with 48 dp avatars, shortened keys and aggregate unread badges,
followed by Add Profile. Selecting an alternate applies immediately and
collapses the card; Back collapses it before leaving Settings. No manage-device,
account-info, or removal commands are added.

The existing Material modal switcher remains available to the typed Settings
entry used by other app surfaces. Its active-first ordering, selected check,
unread behavior, Add Profile action and safe dismissal remain unchanged. This
supersedes WN-ANDROID-0042's separate Switch Profile group and root-to-sheet
behavior while preserving WN-ANDROID-0041's profile projection and ownership
rules.

Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3),
[Material cards](https://developer.android.com/develop/ui/compose/components/card).

## WN-ANDROID-0044 — Settings groups use separated native rows

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

The active Share & Connect identity and profile-management control remain in
one white-equivalent Settings card, but the management control is now a native
transparent `ListItem`, not an outlined button. A sole profile shows the
official `person_add` symbol and **Add Profile**. Alternate profiles show their
existing 32 dp stacked preview, **Switch Profile**, and the disclosure used to
expand deterministic inline rows. A one-dp `surfaceContainerLow` divider
separates the active identity, management row, expanded profiles, and final Add
Profile row. Existing callbacks, unread projection, stored order, Back-first
collapse, modal switcher compatibility, and profile ownership are unchanged.

The same canvas-tone divider separates adjacent rows in the consolidated
destination and Help cards. Cards remain `surfaceContainerLowest` over the
`surfaceContainerLow` Settings canvas, so the separator reads as a subtle
surface gap in light and dark themes without hard-coded colors. Material
`ListItem` continues to own typography, targets, state layers, growth, and RTL.
This supersedes only WN-ANDROID-0043's outlined/no-divider presentation.

Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3),
[Material lists](https://developer.android.com/develop/ui/compose/lists).

## WN-ANDROID-0045 — Settings row gaps and profile badges use explicit geometry

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

Settings keeps native Material `ListItem` measurement and content padding, but
uses a two-dp `surfaceContainerLow` inter-row gap in white-equivalent Settings
cards. Material's default divider is intentionally thinner; the wider neutral
gap is the accepted match for the supplied Google Messages reference and does
not change touch targets or text/icon alignment. This supersedes only the
one-dp separator metric in WN-ANDROID-0044.

The stacked-profile remainder is rendered as a real 32 dp circular surface at
the same overlap step as its avatar peers, rather than stretching a `Badge`
into an avatar-shaped placeholder. Inactive profile unread counts use the
app's monochrome `primary`/`onPrimary` badge roles in both the inline expansion
and retained modal switcher; selected checks still take precedence.

Ordinary app-owned sheets continue to use the already-approved shared
`surfaceContainerLow` sheet wrapper from WN-ANDROID-0039. Material owns sheet
shape, handle, motion and dismissal; specialized camera/media and system-owned
surfaces remain outside that rule.

Sources: [Material horizontal divider](https://developer.android.com/reference/kotlin/androidx/compose/material3/HorizontalDivider.composable),
[Material list item](https://developer.android.com/reference/kotlin/androidx/compose/material3/ListItem.composable),
[Material modal bottom sheet](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalBottomSheet.composable).

## WN-ANDROID-0046 — Ordinary sheets use a visibly neutral modal surface

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

All ordinary app-owned modal sheets continue through the single
`WhiteNoiseModalBottomSheet` boundary, but its container advances from
`surfaceContainerLow` to `surfaceContainer` (#EFEFEF light, #1E1E1E dark).
This keeps the sheet in the monochrome Material surface hierarchy while making
the gray modal plane legible over its scrim. The audit found every ordinary
profile, diagnostics, composer, message-action, timer, destructive-task and
web-image sheet already uses this wrapper. The direct CameraX scanner sheet is
the intentional specialized camera exception; system-owned pickers, scanner,
Sharesheet and settings surfaces remain untouched. This supersedes only the
surface role in WN-ANDROID-0039 and WN-ANDROID-0045, not their handle, shape,
header, inset, motion or dismissal rules.

The first-login diagnostics sheet no longer nests Settings-card and ListItem
horizontal insets. Its intro, two full-width 56 dp switch rows and privacy copy
share the header's 24 dp directional content line; the native switches remain
trailing and the complete row retains Switch semantics and ripple. The
Settings root footer is centered and reads only **Version 0.1**.

Evidence: `WhiteNoiseSheets.kt`, `DiagnosticsImprovementsScreen.kt`,
`SettingsComponents.kt`, `ProfileSettingsScreens.kt`, `MaterialSheetTest`,
`DiagnosticsPromptTest`, and `SettingsScreenTest`.
Sources: [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
[Material switches](https://developer.android.com/develop/ui/compose/components/switch).

## WN-ANDROID-0047 — Diagnostics prompt switches use inset rounded state layers

- Date: 2026-08-26
- Status: Approved by explicit user direction and implementation

The first-login diagnostics sheet retains Material's pressed/focused state
feedback and whole-row Switch semantics, but the interactive row no longer
paints an edge-to-edge rectangular band. Each switch row now has an 8 dp
transparent outer inset, clips the native state layer to
`MaterialTheme.shapes.large`, keeps a 56 dp minimum target, and uses 16 dp
internal horizontal padding. The resulting 8 + 16 dp geometry preserves the
24 dp alignment shared by the sheet title, explanatory copy, switch labels and
trailing controls while giving the press state visible side clearance and
rounded corners. The native `Switch` remains the visual control and exposes no
duplicate semantics inside the merged toggle target.

This supersedes only WN-ANDROID-0046's full-width rectangular prompt-row
geometry; the approved modal surface, copy, lifecycle, consent ownership and
immediate preference behavior are unchanged.

Sources: [Handle user interactions](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions),
[Material switches](https://developer.android.com/develop/ui/compose/components/switch).

## WN-ANDROID-0048 — Share & Connect uses Material peer modes and the shared scanner

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Share & Connect preserves the iOS product organization without copying its
presentation. A center-aligned Material app bar contains a native
`SingleChoiceSegmentedButtonRow` for the mutually exclusive **Share** and
**Connect** modes, with native Back and a Share-only app-bar action. Share uses
an adaptive, scrollable identity composition: avatar at 32% of pane width
clamped to 104–152 dp, name and optional verified address, a 48 dp copyable
public-key capsule with temporary copied state, and a black-on-white QR surface
at 81% clamped to 248–376 dp. Android Sharesheet remains the only outbound
target chooser.

Connect reuses the approved near-full CameraX 1.6.1 and bundled on-device ML
Kit 17.3.0 scanner sheet from WN-ANDROID-0032 with profile-specific title,
permission copy and validation. It retains the existing rounded target, torch,
just-in-time camera permission, swipe/Back dismissal and unavailable recovery.
The former Google Code Scanner dependency and Share & Connect integration are
removed because no selected flow uses them. A valid fixture profile code
reaches deterministic Profile Found; invalid input never mutates profile state.

This supersedes the Share & Connect Google Code Scanner exception in
WN-ANDROID-0031 and the old Settings brief, while leaving the specialized
camera-sheet geometry in WN-ANDROID-0032 unchanged.

Evidence: `ShareConnectScreen.kt`, `PrivateKeyQrScannerScreen.kt`,
`SettingsScreenTest`, `settings-and-profile-services.md`, and
`feature-inventory.md`.
Sources: [Material segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button),
[Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
[CameraX preview](https://developer.android.com/media/camera/camerax/preview),
[ML Kit barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android),
and [Android Sharesheet](https://developer.android.com/training/sharing/send).

## WN-ANDROID-0049 — Share & Connect is one stable identity page

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Share & Connect no longer presents Share and Connect as persistent modes. The
route uses a center-aligned **Share & Connect** app-bar title with native Back
and Android Sharesheet actions, followed by one adaptive identity composition.
The verified address uses Google's official filled rounded `verified` symbol at
20 dp. The public-key capsule is visibly 40 dp high with 12 dp horizontal
padding and an 18 dp copy/check symbol while a clipped 48 dp outer target owns
touch and semantics.

One standard filled-tonal **Scan QR Code** action follows the QR caption and
opens the scanner approved in WN-ANDROID-0032. Back, swipe or scrim dismissal
returns to the stable identity page instead of exposing an empty scanner mode.
Invalid and unavailable results explain recovery inline; valid fixture input
still reaches deterministic Profile Found. Android Sharesheet, scanner state,
permission scope and profile state are otherwise unchanged.

This supersedes WN-ANDROID-0048's segmented peer-mode presentation only. A
Material button group is intentionally not used because there is one scanner
task rather than a set of sibling actions. It would also require Material
1.5.0-alpha26, while WN-ANDROID-0040 pins alpha25 to preserve API 23 without a
dependency or minimum-SDK change.

Evidence: `ShareConnectScreen.kt`, `ic_verified_filled.xml`,
`SettingsScreenTest`, `settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Material buttons](https://developer.android.com/develop/ui/compose/components/button),
[Material button groups](https://m3.material.io/components/button-groups/overview),
[Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
[Material Symbols](https://developer.android.com/develop/ui/compose/graphics/images/material),
[CameraX preview](https://developer.android.com/media/camera/camerax/preview),
and [Android Sharesheet](https://developer.android.com/training/sharing/send).

## WN-ANDROID-0050 — Share & Connect uses compact identity details and a primary scanner task

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The public-key capsule is visibly 36 dp high with 10 dp horizontal padding and
a 16 dp copy/check symbol while its clipped 48 dp outer target continues to own
touch and semantics. The address follows the name without an added layout gap,
and the public-key target starts 4 dp below the address. These tighter values
change visual density without reducing accessibility targets.

The QR matrix retains its prior rendered size. App-owned white padding around
it decreases from 8 dp to 4 dp per edge, so the surrounding white surface is
8 dp smaller in each dimension. **Scan QR Code** now uses the shared 56 dp
primary Material `Button` across the content width, reflecting its role as the
single principal task on the screen.

This supersedes only WN-ANDROID-0049's public-key, QR-frame, identity-gap, and
filled-tonal scanner-action geometry. The stable page structure, Android
Sharesheet, scanner behavior, permission scope, and deterministic state are
unchanged.

Evidence: `ShareConnectScreen.kt`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Source: [Material buttons](https://developer.android.com/develop/ui/compose/components/button).

## WN-ANDROID-0051 — Share QR framing is optically minimal

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Share & Connect keeps the rendered QR matrix at its existing size while
reducing app-owned white frame padding from 4 dp to 2 dp per edge. The outer
technical surface therefore shrinks by another 4 dp in each dimension. Its
**Scan to connect.** caption begins 1 dp below the white surface.

These are explicit optical exceptions to the shared 4/8 dp rhythm for the
QR's noninteractive technical frame and its caption relationship. QR quiet
modules encoded by ZXing remain present, and button geometry, scanner behavior,
semantics, state, and adaptive bounds are unchanged.

Evidence: `ShareConnectScreen.kt`, `settings-and-profile-services.md`,
`ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0052 — Share QR owns its full visible frame and pins the scanner task

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The Share & Connect QR now disables ZXing's two-module encoder margin for this
screen only and retains the app's literal 2 dp white `Surface` padding. This
corrects WN-ANDROID-0051, where only the Compose frame changed while the
encoded matrix still contained a visibly larger white quiet zone. Other QR
presentations retain the default two-module encoder margin.

The public-key target-to-QR gap decreases from 32 dp to 16 dp. The full-width
primary **Scan QR Code** action moves from the scrolling identity composition
to the scaffold bottom slot, with the shared 16 dp action inset and navigation
safe-area padding. Scanner behavior, permissions, results, QR content, and the
Profile Found state are unchanged.

Evidence: `ShareConnectScreen.kt`, `ProfileSettingsScreens.kt`,
`ProfileSettingsTest`, `settings-and-profile-services.md`, `ui-metrics.md`,
and `feature-inventory.md`.
Source: [Material inset handling](https://developer.android.com/develop/ui/compose/system/material-insets).

## WN-ANDROID-0053 — Share QR frame balances separation and matrix prominence

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Device inspection showed that WN-ANDROID-0052's literal 2 dp frame was too
tight once the hidden encoder margin was correctly removed. Share & Connect
therefore keeps its marginless encoded matrix but increases the app-owned white
frame to 6 dp per edge. The rendered QR matrix keeps the same dimensions; only
the outer technical surface grows by 8 dp in each dimension.

The technical surface changes from the theme's 28 dp `extraLarge` shape to its
16 dp `large` shape. This keeps the frame recognizably rounded without making
the square QR appear heavily clipped. Caption spacing, the 16 dp public-key
relationship gap, pinned scanner action, and scanner behavior remain unchanged.

Evidence: `ShareConnectScreen.kt`, `settings-and-profile-services.md`,
`ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0054 — Compact public-key feedback and pinned authentication actions

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The Share & Connect public-key capsule now derives its default 24 dp visual
height from `bodySmall`/16 dp icon content plus 4 dp vertical padding. A
transparent 48 dp outer target still owns semantics and input. Both layers use
one `MutableInteractionSource`, but the Material ripple is drawn only inside
the visible capsule after its circular clip. This removes the prior mismatch
where the pressed state expanded to the full touch target. The capsule remains
content-sized at larger font scales rather than forcing a fixed 24 dp height.

Sign In retains its existing pinned action and Sign Up now uses the same
bounded Scaffold bottom slot. The slot applies the shared 16 dp outer inset,
navigation-bar padding, and IME padding; Sign Up's scrolling body consumes the
Scaffold content padding without adding a second IME inset. This supersedes
WN-ANDROID-0027's inline Sign Up action following renewed device direction.

Evidence: `ShareConnectScreen.kt`, `SignInScreen.kt`, `SignUpScreen.kt`,
`SettingsScreenTest`, `AdaptiveLayoutTest`, `DiagnosticsPromptTest`,
`onboarding-and-profiles.md`, `settings-and-profile-services.md`,
`ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0055 — Share identity framing restores balanced breathing room

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The Share & Connect QR keeps the same marginless rendered matrix and 16 dp
corner shape, while its app-owned white frame doubles from 6 dp to 12 dp per
edge. Only the technical surface grows; QR content, scan reliability,
semantics, caption gap and pinned scanner action remain unchanged.

The public-key capsule grows from its 24 dp default visual height to 32 dp by
increasing vertical content padding from 4 dp to 8 dp. Horizontal content
padding increases from 10 dp to 25 dp per side, adding exactly 30 dp to the
capsule's default visual width. Its transparent 48 dp semantic target, shared
interaction source, pill-clipped state layer, text/icon scale and temporary
copy confirmation remain unchanged.

This supersedes only WN-ANDROID-0053's QR frame metric and WN-ANDROID-0054's
public-key capsule padding. Evidence: `ShareConnectScreen.kt`,
`SettingsScreenTest`, `settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0056 — Share public key uses the widened capsule interior

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The Share & Connect public-key capsule now has a 240 dp visual width, 40 dp
wider than the reviewed compact presentation, while retaining its 32 dp
default visual height, 25 dp directional content padding, 16 dp copy/check
icon and transparent 48 dp semantic target. The text slot fills the remaining
interior width and receives the profile's full public key; Compose applies
middle ellipsis inside that slot rather than displaying the model's already
shortened fixture value. The added width therefore reveals more useful key
content instead of becoming empty padding.

The pill-clipped shared state layer, copy confirmation, QR geometry, pinned
scanner task and adaptive bounds are unchanged. This supersedes only
WN-ANDROID-0055's intrinsic-width public-key presentation. Evidence:
`ShareConnectScreen.kt`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0057 — Profile separates reading from editing

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The Share & Connect public-key capsule retains its reviewed 240 × 32 dp visual
size, full-key middle ellipsis, 16 dp icon and transparent 48 dp target, but its
directional content padding is reduced from 25 dp to the shared 16 dp content
inset. The state layer remains clipped to the visible pill.

Profile now opens in a read mode with its name, verified address and About
fields disabled. The app-bar Edit action enters editing; avatar-source controls
appear only there, and a single validated bottom Save applies the draft and
returns to Profile read mode. System Back first cancels editing and restores
the stored values. The filled official verified symbol occupies the address
field's trailing-icon slot, replacing the redundant `Verified address` helper.
The route and pinned action both use `surface` with zero tonal elevation, so
the Save area does not paint a gray footer across the otherwise white page.

This is the Material translation of the read/edit contract in pinned iOS
`ProfileSettingsView.swift` at `0bd7cba`, while Android's top app bar owns the
Edit action and system Back owns cancellation. It follows current official
Android app-bar, text-input and button guidance. This supersedes only
WN-ANDROID-0056's 25 dp public-key inset and the earlier always-editable Profile
presentation. Evidence: `WhiteNoiseTopBar.kt`, `SettingsComponents.kt`,
`ProfileSettingsScreens.kt`, `ShareConnectScreen.kt`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0058 — Profile read mode preserves normal field contrast

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Profile's resting Name, Verified Nostr Address and About fields now use the
native read-only state rather than the disabled state. They remain visually
enabled, focusable, selectable and copyable, but user input cannot modify them
until Edit is selected. This follows Compose's documented distinction:
`enabled = false` removes editing, focus and selection, while
`readOnly = true` blocks modification but retains focus and copying.

The app-bar **Edit** action remains a standard Material `TextButton`: it is a
secondary, low-emphasis transition into editing rather than the screen's
commit action. The editing-only **Save** task remains the filled high-emphasis
button. This supersedes only WN-ANDROID-0057's disabled read-field treatment;
the read/edit lifecycle, verification badge, avatar controls, Save behavior
and Back cancellation are unchanged. Evidence: `ProfileSettingsScreens.kt`,
`SettingsScreenTest`, `settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0059 — Profile-key export uses compact groups and focused dialogs

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Profile Keys now uses the Settings neutral canvas with three compact
white-equivalent groups for public key, private key, and export actions.
Supporting explanations sit outside their related groups. The prior redundant
private-key warning card, separate show row, oversized export buttons, and
implementation-boundary footer are removed. Official Material copy,
visibility, lock, and download symbols retain native touch targets; the
private value remains excluded from accessibility semantics whether visually
hidden or shown.

Encrypted export is a focused Material alert dialog with Password and Confirm
Password fields, native Cancel/Export actions, IME traversal, mismatch state,
and the accepted short helper. Raw export uses a separate consequence dialog
with the accepted iOS product wording and a semantic destructive action.
Either successful confirmation opens Android's system document creator;
write failure returns a recoverable alert rather than silently dismissing.
Sensitive dialog state is cleared on every exit. This is the Android Material
translation of pinned iOS `ProfileKeysSettingsView.swift` at `4c25393`; it
does not reproduce iOS sheet chrome or add real encryption/persistence.

Evidence: `ProfileSettingsScreens.kt`, `SettingsScreenTest`, official Material
Symbol XML, `settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0060 — Key values measure before truncation and encrypted export uses native secure fields

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Profile Keys passes the complete public/private value to a weighted monospaced
slot and applies middle ellipsis only after the native 48 dp trailing action
has been measured. This replaces fixture-level pre-shortening, makes compact
screens use all available width, and preserves recognizable key prefixes and
suffixes. The custom row keeps a 64 dp minimum height and places official 24 dp
copy/visibility artwork on a 16 dp visual end edge without shrinking its touch
target.

Encrypted export remains a focused Material alert, but its two password inputs
now use Material `SecureTextField` controls with attached labels instead of the
screen-level above-label field wrapper. The dialog retains native width,
shape, actions, focus, IME behavior, dismissal, password clearing and mismatch
feedback. No custom dialog dimensions or copied iOS modal chrome are added.

Evidence: `ProfileSettingsScreens.kt`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0061 — Dialog inputs retain the shared rounded field language

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The encrypted-export dialog now uses the same `WhiteNoiseSecureTextField`
component as ordinary secure input elsewhere in the app. Its 28 dp rounded
tonal container, 16 dp content line, transparent resting border, and 2 dp
focus/error ring remain the app-wide rules from WN-ANDROID-0023 and
WN-ANDROID-0024 even when the field appears inside a Material `AlertDialog`.
Material continues to own secure state-based editing, label/supporting
typography, cursor and selection, focus, IME behavior, state animation, dialog
geometry, actions, and dismissal. This supersedes only WN-ANDROID-0060's use
of the default `SecureTextField` container; the measured key row and dialog
task boundary remain unchanged.

When the private key is hidden, its repeated mask glyphs use clipped overflow
rather than text ellipsis. No literal ellipsis is shown after the mask, no
secret-length claim is made, and the accessibility surface remains the
state-only **Private key hidden** description. Revealed and public keys retain
measured middle ellipsis so their recognizable prefix and suffix stay visible.

Evidence: `WhiteNoiseTextFields.kt`, `ProfileSettingsScreens.kt`,
`SettingsScreenTest`, `settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0062 — Nested dialog fields keep visible tonal separation

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

The encrypted-export dialog keeps the shared 28 dp rounded secure-field
component, but its enabled field containers use the stronger semantic
`surfaceContainerHighest` role. A Material alert already uses a high tonal
surface; repeating that same role on the inputs made their boundaries flatten
into the dialog and read as white. The nested role restores a visibly gray
input surface without hardcoded colors, custom geometry, or changes to focus,
error, IME, selection, and accessibility behavior. Ordinary page fields remain
`surfaceContainerHigh`.

Evidence: `WhiteNoiseTextFields.kt`, `ProfileSettingsScreens.kt`,
`settings-and-profile-services.md`, and `ui-metrics.md`.

## WN-ANDROID-0063 — Settings and app-owned task surfaces share a nested tonal hierarchy

- Date: 2026-08-27
- Status: Approved by explicit user direction and implementation

Settings detail scaffolds and ordinary app-owned alert/dialog/sheet surfaces
use the semantic `surfaceContainerLow` role as their neutral canvas. Settings
groups and shared text or secure fields nested in those surfaces use
`surfaceContainerLowest`, producing the same gray-canvas/white-component
hierarchy as the Settings root in both light and dark themes. A scoped
composition local applies the field role without changing onboarding fields,
system-owned pickers and settings, or specialized camera/media surfaces.
Material continues to own dialog width, shape, actions, state layers, focus,
IME behavior, motion, and dismissal.

The encrypted private-key export dialog fills Material's text-content slot so
both rounded fields align with the dialog copy. It also projects the pinned iOS
password-strength rules as Low, Fair, or Strong with a three-step Material
progress indicator. Android retains its existing eight-character export
minimum and matching-password requirement. This decision supersedes
WN-ANDROID-0062's inverted high-tonal dialog field treatment and the earlier
white Profile-detail canvas treatment.

Evidence: `WhiteNoiseDialogs.kt`, `WhiteNoiseSheets.kt`,
`WhiteNoiseTextFields.kt`, `SettingsComponents.kt`,
`ProfileSettingsScreens.kt`, `ProfileSettingsTest`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0064 — Notification previews stay visible on their detail screen

- Date: 2026-08-29
- Status: Approved by explicit user direction and implementation

Notifications retains its native Android detail destination instead of copying
iOS navigation chrome. Local notifications and its dependent Native push switch
remain whole-row Material switch targets in one white-equivalent group, now
separated by the accepted two-dp Settings canvas gap. Their supporting copy
preserves the product outcomes: local creation, possible foreground delay,
generic background wake-up and on-device message details.

The existing Notifications destination is already the focused child screen for
this preference, so its three preview choices move out of the generic
`ChoiceDialog` and into one inline `selectableGroup`. Native radio rows apply
immediately, use two-dp separators and rely on radio state rather than the old
square edge-to-edge selected fill. A final noninteractive row previews the
selected deterministic notification. Local notifications continues to disable
Native push and Preview together, with a visible reason.

Android notification permission presentation is superseded by
WN-ANDROID-0065. No notification service, persistence, networking or delivery
behavior is added here. This applies the WN-ANDROID-0045
row-gap and WN-ANDROID-0063 nested-surface decisions to Notifications without
changing other choice dialogs.

Evidence: `ProfileSettings.kt`, `PreferenceScreens.kt`,
`SettingsComponents.kt`, `ProfileSettingsTest`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button),
and [Compose switches](https://developer.android.com/develop/ui/compose/components/switch).

## WN-ANDROID-0065 — Notification access is contextual system-owned gating

- Date: 2026-08-29
- Status: Approved by explicit user direction and implementation

The prototype declares Android's app-level `POST_NOTIFICATIONS` permission but
does not create or deliver a notification. On Android 13 and newer, the
Notifications detail page projects the real platform state as **Not requested**,
**Allowed**, or **Blocked**. Not requested exposes one contextual
**Allow notifications** action that launches the system-owned runtime prompt.
Blocked exposes **Notifications are off** and the app-specific Android
notification Settings handoff. Allowed removes this platform group entirely.
Android 12 and earlier use system notification availability and can only need
the blocked recovery state.

The screen refreshes permission availability after the runtime result and on
every resumed return from Android Settings. App-wide access gates the
profile-owned Local notifications switch; that switch continues to gate Native
push and Preview. Stored profile preferences are not cleared when system access
is lost, so they become effective again when access returns. The old permanent
Android section is removed. A saveable in-screen denial marker complements
Android's permission-rationale signal during the prototype session; no new
product persistence is introduced.

Evidence: `AndroidManifest.xml`, `NotificationPermission.kt`,
`PreferenceScreens.kt`, `NotificationPermissionTest`, `SettingsScreenTest`,
`AppResourceIntegrityTest`, `settings-and-profile-services.md`,
`ui-metrics.md`, and `feature-inventory.md`.
Sources: [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission),
[Request runtime permissions](https://developer.android.com/training/permissions/requesting),
and [app notification Settings](https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_SETTINGS).

## WN-ANDROID-0066 — Appearance separates theme choice from language navigation

- Date: 2026-08-29
- Status: Approved by explicit user direction and implementation

Appearance preserves the pinned iOS product choices without copying its
checkmark rows or navigation chrome. System default, Light and Dark remain one
immediate profile-owned Material radio group. The rows use the accepted two-dp
canvas-tone separation and transparent selected container so the radio itself
communicates selection. Focused helper copy explains the persistent theme
behavior outside the group.

Language is one disclosure row on Appearance and a typed child destination,
not a second dense inline group or generic dialog. The child screen exposes
System default, English, German, Spanish, French, Italian, Portuguese and
Serbian in pinned order as an accessible Material `selectableGroup`, with
immediate profile-owned selection and normal system Back behavior.

Android 13's official per-app language preference is app-wide, synchronized
with system Settings and intended for actual localized resources. This
prototype has no translated resource sets and explicitly keeps ordinary
preferences isolated by profile, so the deterministic language choice does
not call `LocaleManager`/`AppCompatDelegate`, configure a locale list, or claim
that product copy has been translated. A real app-wide locale handoff remains
future scope for the localized product.

Evidence: `ProfileSettings.kt`, `PreferenceScreens.kt`, `AppRoute.kt`,
`WhiteNoiseNavHost.kt`, `ProfileSettingsTest`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages),
[Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
and [Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button).

## WN-ANDROID-0067 — Privacy controls reveal only actionable dependent state

- Date: 2026-08-29
- Status: Approved by explicit user direction and implementation

Privacy & Security keeps the iOS product outcomes while using Android-owned
presentation. Device-protection controls remain one white-equivalent Material
group on the Settings canvas with two-dp separators. **Hide Screen in Recents**
uses Android terminology and describes the visible privacy outcome. The secure
device state gates **Require device authentication**; an unavailable device
retains one Android security-settings recovery action. **Auto-lock** is hidden
until authentication is effective, then opens an immediate native radio dialog
whose selectable radio list sits directly on the dialog surface without a
nested card, separators, or rectangular selected fill.

Diagnostics keeps one summarized route and focused explanation. Its permanent
destination removes the redundant Diagnostics heading, separates the two
native switch rows, and shows Stored Diagnostic Logs only after records exist.
Clearing records retains its Material alert and does not alter consent.

Erase App Data is one destructive row with its consequence outside the action.
The existing Material modal bottom sheet remains Android-native and gains a
semantic error callout plus a pinned action area continuous with the gray modal
canvas; its exact generated-phrase gate, shared white rounded field, progress,
safe/IME handling and wipe behavior are unchanged. Its state starts Expanded
and enables only Hidden and Expanded anchors, preventing the destructive form
from settling at an ambiguous partial height while retaining swipe dismissal
and Back.

Evidence: `PreferenceScreens.kt`, `DiagnosticsImprovementsScreen.kt`,
`DestructiveScreens.kt`, `SettingsScreenTest`, `DiagnosticsPromptTest`,
`DeveloperDestructiveScreenTest`, `privacy-and-security.md`, and
`feature-inventory.md`.
Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button),
[Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
[Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
and [secure sensitive activities](https://developer.android.com/security/fraud-prevention/activities).

## WN-ANDROID-0068 — Data Usage keeps compact choices modal and section help local

- Date: 2026-08-29
- Status: Approved by explicit user direction and implementation

Data Usage preserves all pinned iOS values, defaults, reset behavior and
quality consequences while retaining the more compact Android presentation.
Each per-media policy has three choices and sent photo/video quality has two,
so each disclosure opens an immediate Material radio dialog instead of adding
an iOS-style navigation destination for one small decision. The accessible
radio list sits directly on the dialog surface without a nested card,
separators, or a rectangular selected fill. Cancel, scrim and system Back make
no change.

The main page uses the shared gray Settings canvas, white-equivalent groups and
two-dp canvas-tone separators. Automatic-download help immediately follows the
download group; sent-media help follows the sent-media group. Quality's longer
data/compression consequence appears in its dialog. Reset download settings is
disabled while Photos, Videos, Audio and Files already match their defaults,
and restores all four together after any policy changes.

Evidence: `PreferenceScreens.kt`, `SettingsComponents.kt`,
`SettingsScreenTest`, `data-usage.md`, `settings-and-profile-services.md`, and
`feature-inventory.md`.
Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button),
and [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog).

## WN-ANDROID-0069 — Radio dialogs use dialog-owned alignment app-wide

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

App-owned single-choice dialogs share one direct Material radio-row
composition. `AlertDialog` owns the outer content inset; each option begins at
that content edge, preserves the radio's native touch target, keeps the native
radio-to-label relationship, and makes the complete 56 dp-minimum row the one
selectable accessibility target. Settings `ListItem` remains correct for
full-page Theme, Language and Notification choices, but is not nested inside a
dialog because its additional content inset visibly double-indents the radio
list.

The shared row governs Auto-lock, Photos, Videos, Audio, Files and sent
photo/video quality. Immediate selection, Cancel, scrim dismissal, system Back
and existing deterministic preference behavior are unchanged.

Evidence: `WhiteNoiseDialogs.kt`, `PreferenceScreens.kt`,
`SettingsScreenTest`, `ui-metrics.md`, `privacy-and-security.md`, and
`data-usage.md`.
Sources: [Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button)
and [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog).

## WN-ANDROID-0070 — Relays use a status list and a task sheet, not a pinned add command

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Profile Relays preserves the accepted endpoint, role, availability, recovery,
removal and restore behavior while adopting the Android Settings hierarchy.
Relay rows keep name and URL as the scan path, move transient connection state
to a compact trailing availability indicator, and expose the complete
state through TalkBack. Read only remains secondary capability text. Add Relay
is the final icon-led peer action inside the relay group; the old persistent
bottom button is removed because relay creation is not a page-completion task.

Relay detail uses compact horizontal metadata rows and a separated **Use For**
switch group. URL entry plus three explained roles is a multi-control task, so
Add Relay moves from an overloaded alert to the shared Material modal bottom
sheet with one validity-aware filled action. Remove and restore remain focused
Material alerts and use the pinned product terms **Remove Relay**, **Restore
Default Relays**, and **Restore Defaults**. The prototype state model and lack
of live relay networking are unchanged.

Evidence: `RelayDonateScreens.kt`, `SettingsScreenTest`, `relays.md`,
`settings-and-profile-services.md`, and `feature-inventory.md`.
Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Compose switches](https://developer.android.com/develop/ui/compose/components/switch),
[Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
and [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog).

## WN-ANDROID-0071 — Relay status is binary at a glance and Settings helpers share the title line

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Profile Relays keeps the exact Connected, Reconnecting and Disconnected model
and accessibility wording, but makes the list projection intentionally binary:
Connected uses a filled green circle/check, while both not-connected states use
a filled red circle/close. Reconnecting no longer animates as an in-progress
promise in this deterministic prototype. Shape as well as color communicates
the state, and TalkBack retains the more precise model label.

**Restore Default Relays** is a full-width filled-tonal Material button with a
native disabled state, not a white-equivalent settings row. It remains a
confirmed significant command and stays disabled while defaults are active.

App-wide Settings helper copy now uses the shared 32 dp directional content
line already used by row titles and section labels. The change lives in
`SettingsExplainer`, so helper text below groups, containers and standalone
buttons no longer falls back to the 16 dp outer edge. It begins 8 dp below the
content it explains and contributes no bottom padding; the next independent
section or action owns the 24 dp separation. Embedded Material supporting text
inside list rows, switches and fields remains component-owned and already
aligns with its corresponding title.

Relay status symbols use a compact 20 dp filled container with a 14 dp glyph.
The reduction preserves the binary green-check/red-close scan path without
giving the noninteractive status the same visual weight as the standard 24 dp
disclosure icon beside it.

Evidence: `RelayDonateScreens.kt`, `SettingsComponents.kt`,
`SettingsScreenTest`, `relays.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
and [Compose buttons](https://developer.android.com/develop/ui/compose/components/button).

## WN-ANDROID-0072 — Relay detail URLs use the available single-line value space

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Relay detail metadata keeps compact key/value rows but no longer reserves 32%
of every row for short labels. A 64 dp minimum label column preserves Name,
URL and Status alignment while the value takes all remaining width. Relay URLs
use normal Material body typography rather than monospaced text, remain one
line, and middle-ellipsize only after the available width is measured. The
complete URL remains present in accessibility semantics.

Evidence: `RelayDonateScreens.kt`, `SettingsScreenTest`, `relays.md`, and
`feature-inventory.md`.
Source: [Material 3 ListItem](https://developer.android.com/reference/kotlin/androidx/compose/material3/ListItem.composable).

## WN-ANDROID-0073 — Support, Donate and developer actions stay with their content

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Support, Donate and Developer Tools preserve the accepted product behavior but
no longer reserve persistent bottom space for actions that are understandable
only beside their content. Support uses one compact support-identity group,
the shared title-aligned helper line and an inline availability-aware **Start
Chat** action. Its relay recovery remains visible before the disabled action.

Donate keeps Android's peer Material primary tabs immediately below the app
bar instead of copying the iOS segmented-control chrome. A centered concise
purpose cluster precedes one bounded literal black-on-white QR. The selected
address is the copy target: it stays one line, middle-ellipsizes only visually,
retains the complete value in semantics and changes its icon/description after
copy. The redundant pinned copy button is removed; no wallet or payment
capability is introduced.

Developer Tools reveals technical groups only while its per-profile master
gate is enabled. Debug Mode, Diagnostics, Key Packages, diagnostic consent and
retained records remain behaviorally independent as already specified, but
peer rows now have visible group separation and explanations use the shared
helper content line. About uses compact horizontal metadata. Diagnostics keeps
one semantic Live console with responsive commands and divided event rows.
**Publish New Key Package** is an inline settings action beside the package it
replaces, not a page-completion footer.

Evidence: `SupportScreen.kt`, `RelayDonateScreens.kt`, `DeveloperScreens.kt`,
`SettingsComponents.kt`, `SettingsScreenTest`,
`DeveloperDestructiveScreenTest`, `settings-and-profile-services.md`,
`developer-and-destructive-flows.md`, and `feature-inventory.md`.
Sources: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings),
[Compose tabs](https://developer.android.com/develop/ui/compose/components/tabs),
[Compose cards](https://developer.android.com/develop/ui/compose/components/card),
and [Compose buttons](https://developer.android.com/develop/ui/compose/components/button).

WN-ANDROID-0074 supersedes WN-ANDROID-0073's Donate selector and locally
composed identity-code geometry. Its Support and Developer Tools decisions
remain current.

## WN-ANDROID-0074 — Donate reuses the established selector, QR and npub capsule

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Donate uses the compact Material `SingleChoiceSegmentedButtonRow` previously
accepted for the peer Share/Connect modes, now centered in the app-bar title
slot for Lightning and Bitcoin. This preserves the iOS information hierarchy
without copying Apple's segmented-control drawing or interaction.

Share & Connect and Donate use the same code components rather than parallel
measurements. Their QR surface takes 81% of the active pane width clamped to
248–376 dp, generates a marginless matrix, and adds a 12 dp literal-white frame
inside 16 dp corners. Their identifier capsule is visibly 240 × 32 dp at
default text scale inside a 48 dp target, with 16 dp directional padding, a
16 dp copy/check icon, full-value middle ellipsis, a clipped state layer and
two-second copied feedback. A regression assertion compares the two QR bounds
directly, so the surfaces cannot silently diverge.

Donate retains its purpose copy, method caption and deterministic copy-only
behavior. It introduces neither wallet/payment behavior nor a duplicate pinned
copy action.

Evidence: `IdentityCodeComponents.kt`, `ShareConnectScreen.kt`,
`RelayDonateScreens.kt`, `SettingsScreenTest`, `ui-metrics.md`,
`settings-and-profile-services.md`, and `feature-inventory.md`.
Sources: [Compose segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button)
and [Material content structure](https://m3.material.io/foundations/layout/understanding-layout/overview).

## WN-ANDROID-0075 — Diagnostics commands move to native overflow and Live gains semantic motion

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Diagnostics preserves its one persistent console, deterministic events, empty
state, optional sanitized summary copy, Test mutation and availability-aware
clearing. Copy Diagnostic Summary when present, Test, and Clear Events move
from an exposed button row to a trailing 48 dp vertical-dots `IconButton` and
the existing shared Material dropdown menu. Clear remains disabled while the
console is empty. This is the direct Android translation of the accepted iOS
toolbar menu and lets the console reclaim the command row's height.

The header retains visible **Live** text and merged **Live event stream**
semantics. An official 18 dp cell-tower/radiowave Material symbol adds the
user-approved semantic green state and a restrained repeating 0.42-to-1 alpha
pulse. The animation follows Compose's duration scale and is supplemental;
text and semantics communicate the complete state with color or motion absent.
This supersedes the static-indicator exception in the Developer and
destructive flows brief and WN-ANDROID-0073's exposed responsive commands.

Evidence: `DeveloperScreens.kt`, `ic_more_vert.xml`,
`DeveloperDestructiveScreenTest`, `developer-and-destructive-flows.md`,
`ui-metrics.md`, and `feature-inventory.md`.
Sources: [Compose menus](https://developer.android.com/develop/ui/compose/components/menu)
and [Compose value-based animation](https://developer.android.com/develop/ui/compose/animation/value-based).

## WN-ANDROID-0076 — Key-package publication is a real contextual button

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

**Publish New Key Package** remains beside the current package because it
replaces that artifact rather than completing the page, but it no longer
imitates a Settings destination row. It uses the shared full-width 56 dp
`WhiteNoiseFilledTonalButton` with the official package symbol, Material-owned
shape, padding, state layer, focus and Button semantics. The consequence stays
8 dp below on the shared title-aligned helper line.

The mutation remains deterministic and replaces exactly one profile-owned key
package. This presentation change adds no progress claim, networking,
cryptography or persistence. It supersedes WN-ANDROID-0073's generic inline
settings-action wording for Key Packages without moving the action to a pinned
footer.

Evidence: `DeveloperScreens.kt`, `DeveloperDestructiveScreenTest`,
`developer-and-destructive-flows.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0080 — Donate adopts the Expressive connected button group

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Lightning and Bitcoin replace the legacy `SingleChoiceSegmentedButtonRow`
with Material 3 Expressive's connected single-choice button-group pattern.
Two native `ToggleButton`s use `ButtonGroupDefaults` leading/trailing shapes,
selection morphing, native state layers and explicit radio semantics in the
same centered app-bar slot. The pinned Material 3 artifact already contains
these APIs, so this decision adds no dependency or custom-drawn control.

The code-to-address-target relationship uses the explicitly approved smaller
1 dp gap. WN-ANDROID-0084 supersedes this decision's initial caption size and
lower optical relationship. QR dimensions, address content, copy feedback and
offline behavior remain unchanged.

This supersedes WN-ANDROID-0074's selector component and Donate's unequal
16 dp / 1 dp address relationships. The initial implementation incorrectly
equalized them at the larger value; the user-directed correction retains the
smaller value for both. Its shared QR and identifier components remain current.

Evidence: `RelayDonateScreens.kt`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Source: [Material 3 button groups](https://m3.material.io/components/button-groups/overview).
Source: [Compose buttons](https://developer.android.com/develop/ui/compose/components/button).

## WN-ANDROID-0077 — Privacy owns diagnostics choices; Developer Tools only inspects logs

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Privacy & Security remains the sole permanent consumer entry to **Diagnostics
& Improvements**. It owns the two independent profile choices, aggregate
retained size, destructive confirmation, and clearing. Developer Tools no
longer presents a disclosure to that consumer destination and never duplicates
either preference or the clear action.

When the master developer gate is enabled, **Diagnostic Logs** begins with
read-only **Diagnostic Logging — On/Off** metadata. It then lists only
non-empty sanitized profile-owned records; absent or zero-byte records collapse
to one secondary **There are no logs.** row. Its helper points people to
Privacy & Security for configuration and clearing while explaining that
existing sanitized files remain visible after logging is turned off.

This matches the explicitly authorized `wn-ios-prototype@4c25393` ownership
contract while retaining Android's grouped Material list hierarchy. It also
follows Android's recommendation to divide settings by meaningful groups and
subscreens, while keeping technical inventory contextual to Developer Tools.

Evidence: `DeveloperScreens.kt`, `WhiteNoiseNavHost.kt`,
`DeveloperDestructiveScreenTest`, `diagnostics-and-improvements.md`,
`privacy-and-security.md`, `developer-and-destructive-flows.md`, and
`feature-inventory.md`.
Source: [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings).

## WN-ANDROID-0078 — Diagnostic logs export through the system document picker

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

When Developer Tools has at least one non-empty sanitized record, its
Diagnostic Logs group ends with **Export Diagnostic Logs**. The action uses
AndroidX's `CreateDocument("text/plain")` contract with **White Noise
Diagnostic Logs.txt** as the suggested name. Android's Storage Access Framework
owns local/cloud destination selection, cancellation changes nothing, and a
provider write failure presents **Couldn’t Save Diagnostic Logs** with a
specific retry instruction.

The profile-owned diagnostics state derives one deterministic report matching
the accepted iOS content boundary: heading, sanitized purpose line, ordinal log
entries, creation labels, byte counts, and three fixed technical event labels.
It excludes profile names and IDs, source filenames, message content, keys, and
device identifiers. The export action is absent after clearing or before any
record contains data, needs no broad storage permission, and never changes the
logging preference or retained inventory.

Evidence: `DiagnosticsState.kt`, `DeveloperScreens.kt`,
`DiagnosticsConsentTest`, `DeveloperDestructiveScreenTest`,
`developer-and-destructive-flows.md`, `diagnostics-and-improvements.md`, and
`feature-inventory.md`.
Sources: [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
and [Activity Result APIs](https://developer.android.com/training/basics/intents/result).

## WN-ANDROID-0079 — Diagnostics header follows the console content line

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

The **Events** title and persistent **Live** status are each inset 16 dp from
the console surface edges, matching the event text and divider endpoints below.
This preserves one vertical content line on both sides without changing the
console shape, app-bar alignment, Live motion, or event density.

A Compose geometry regression compares the title and Live bounds directly to
the first event row, preventing either header edge from drifting back to the
rounded surface's outer bounds.

Evidence: `DeveloperScreens.kt`, `DeveloperDestructiveScreenTest`,
`developer-and-destructive-flows.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0081 — Chat discovery uses one compact contextual search field

- Date: 2026-08-30
- Status: New Message portion superseded by WN-ANDROID-0085; Chats and New Group remain current

Material's standalone SearchBar is the reference when search is the primary
surface and its pinned container token is 56 dp. Chats instead enters search
inside an already-established top app bar. In that bounded context, the active
query field is 48 dp high and explicitly uses `bodyLarge`, preventing it from
inheriting the app-bar title typography. Its full-rounded shape, tonal
container, single-line input, autofocus, IME, clear action, and Back behavior
remain unchanged.

The compact field uses Material's `DecorationBox` instead of forcing the
standard 56 dp `TextField` and its 16 dp vertical content padding into a fixed
48 dp height. Its default-scale vertical content inset is zero, which centers
and fully contains the placeholder, entered text, cursor and icons. The field
uses 48 dp as a minimum so larger accessibility text can grow rather than
clip. Material continues to own editing, selection, decoration colors, shape,
focus and icon placement.

The field and its native Back/Clear icon buttons meet Android's 48 by 48 dp
minimum interactive target. New Group reuses this exact component. New
Message's later user-directed return to a standard 56 dp standalone field is
recorded in WN-ANDROID-0085. This remains a bounded chat-discovery density
exception, not a new app-wide search token and not a reduction of any touch
target.

Evidence: `WhiteNoiseCompactSearchField.kt`, `ChatsScreen.kt`,
`ChatCreationScreens.kt`, `ChatsPolishTest`, `ChatsScreenTest`,
`chats-and-chat-creation.md`, `ui-metrics.md`, and `feature-inventory.md`.
Sources: [Compose Search bar](https://developer.android.com/develop/ui/compose/components/search-bar)
and [Android touch-target guidance](https://support.google.com/accessibility/android/answer/7101858).

## WN-ANDROID-0082 — New Message adopts the shared grouped task hierarchy

- Date: 2026-08-30
- Status: New Group search/directory surfaces superseded by WN-ANDROID-0087;
  other decisions remain current

New Message, New Group, Set Up Group, Person Profile, and the new Groups in
Common destination use the established Settings-detail hierarchy:
`surfaceContainerLow` canvas, `surfaceContainerLowest` control groups, 32 dp
title/helper content lines, and continuous gray bottom slots around shared
56 dp task buttons. WN-ANDROID-0085 supersedes this decision's compact-search
choice for New Message only; New Group retains WN-ANDROID-0081's field.

New Group replaces selected `InputChip`s with the pinned iOS outcome: a stable
horizontal strip of named 64 dp avatars. Each has a visible 24 dp filled remove
badge but exposes one whole 80 dp identity tile as the Button-semantic action.
The searchable directory remains lazy and toggleable, while its transparent
Material rows sit directly on the gray canvas rather than inside a white card.
The shared bottom action surface follows the header's scroll-state color:
`surfaceContainerLow` at rest and `surfaceContainer` after overlap. Set Up
Group's earlier details-title, white member group, and task-height photo-button
treatment is superseded by WN-ANDROID-0083.

Person Profile no longer maintains a simplified duplicate identity surface. It
reuses the Share & Connect avatar proportions, filled verified seal and
complete-value copy capsule with native middle ellipsis and two-second semantic
feedback. The accepted iOS About/identity ordering is retained. Groups in
Common is navigable; eligible Add to Group work uses the shared Material modal
bottom sheet followed by a confirmation and the existing deterministic member
mutation. Add/Remove Contact, Block/Unblock, Message/recovery and contextual
admin actions remain authoritative and now use grouped Material action rows.

This changes presentation and exposes already-modelled group membership; it
adds no backend, relay, network, persistence, or cryptography scope.

Evidence: `ChatCreationScreens.kt`, `AppRoute.kt`, `WhiteNoiseNavHost.kt`,
`ChatsScreenTest`, `chats-and-chat-creation.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Compose Search bar](https://developer.android.com/develop/ui/compose/components/search-bar),
[Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
and [Material buttons](https://developer.android.com/develop/ui/compose/components/button).

## WN-ANDROID-0083 — Set Up Group reuses the lightweight identity treatment

- Date: 2026-08-30
- Status: Member-row treatment superseded by WN-ANDROID-0088; identity,
  fields, photo flow, and bottom action remain current

Set Up Group uses the same 120 dp `ProfileAvatar` and shared native
`AvatarPhotoButton` used by Sign Up and Profile for Add/Change Photo. The photo
source menu, preparation and error states, removal, and system-owned Photos and
Files contracts remain unchanged.

The fields begin immediately after the identity control; **Group Details** is
removed because the destination already states the task. **Members** remains a
useful label. Its later return to the established grouped-row treatment is
recorded in WN-ANDROID-0088. The pinned Create Group slot uses
the shared creation treatment: `surfaceContainerLow` at rest and
`surfaceContainer` after content scrolls beneath the header.

Evidence: `ChatCreationScreens.kt`, `ChatsScreenTest`,
`chats-and-chat-creation.md`, `ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0084 — Donate caption is smaller and optically closer

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

The Lightning/Bitcoin method caption moves from `bodyLarge` to `bodyMedium`.
Its line box is pulled 4 dp into the transparent lower portion of the existing
48 dp address-copy target, leaving a measured 5 dp gap below the visible 32 dp
pill. The QR-to-address-target gap remains 1 dp, and the shared pill size,
middle ellipsis, ripple, copy feedback, semantics and full 48 dp target do not
change.

This is a Donate-only optical correction that supersedes WN-ANDROID-0080's
larger caption and equal target-bound gap. It does not change the shared npub
capsule component or create a smaller interactive target.

Evidence: `RelayDonateScreens.kt`, `SettingsScreenTest`,
`settings-and-profile-services.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0085 — New Message restores standalone search and edge-to-edge scrolling

- Date: 2026-08-30
- Status: Row-surface portion superseded by WN-ANDROID-0086 and
  WN-ANDROID-0087; search and viewport remain current

New Message restores Material's standard 56 dp filled text-field treatment
because persistent search is the primary standalone control in this page body.
Chats and New Group keep WN-ANDROID-0081's 48 dp contextual field. Both sizes
remain minimums that preserve native icon targets and can grow with accessible
text.

The New Message lazy viewport reaches the physical bottom edge instead of
ending above the navigation area. Safe-bottom and section clearance move into
the list's content padding, keeping the final person reachable without drawing
the scroll viewport as a clipped inset panel.

Shared creation person rows remove their redundant screen-local 16 dp outer
margin. Set Up Group and related free directories use native Material
`ListItem` content padding as their sole 16 dp leading artwork line. New
Message and New Group later return to grouped list surfaces with the
established group margin outside that native inset, as recorded in
WN-ANDROID-0086 and WN-ANDROID-0087. This changes spacing and viewport
ownership only; search behavior, selection, navigation, deterministic people
data and relay requirements are unchanged.

Evidence: `SettingsComponents.kt`, `ChatCreationScreens.kt`,
`ChatsScreenTest`, `chats-and-chat-creation.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Source: [Compose Search bar](https://developer.android.com/develop/ui/compose/components/search-bar).

## WN-ANDROID-0086 — New Message people return to a grouped Material list

- Date: 2026-08-30
- Status: New Group exception and separator omission superseded by
  WN-ANDROID-0087; New Message grouping remains current

New Message returns its people directory to the accepted iOS product outcome:
one white-equivalent grouped section on the gray task canvas. Rows remain
individually lazy and keyed; Material segmented shapes round only the first
and last boundaries, preserving one visual group without building the entire
directory in an eager nested column. WN-ANDROID-0087 adds the omitted group
separators and applies the same grouped directory outcome to New Group. Set Up
Group's later read-only grouped treatment is recorded in WN-ANDROID-0088.

All creation person rows migrate from Material's deprecated legacy `ListItem`
overload to the current interactive overloads. Material now owns the 12 dp
leading-content gap already accepted on Chats, replacing the older 16 dp
avatar-to-name gap while retaining 48 dp avatars, complete-row click/toggle
semantics, native state layers, selected state, font scaling and ellipsis.
New Message's group keeps the established 16 dp outer margin and native 16 dp
internal content inset; its edge-to-edge viewport and safe-bottom scrolling
padding from WN-ANDROID-0085 are unchanged.

Evidence: pinned iOS `NewChatView.swift` at `0bd7cba`, Material 3 alpha25
`ListItem` source, `ChatCreationScreens.kt`, `ChatsScreenTest`,
`chats-and-chat-creation.md`, `ui-metrics.md`, and `feature-inventory.md`.
Source: [Compose lists](https://developer.android.com/develop/ui/compose/lists).

## WN-ANDROID-0087 — Creation directories use separated grouped surfaces

- Date: 2026-08-30
- Status: Set Up Group exception superseded by WN-ANDROID-0088; New Message
  and New Group remain current

New Message and New Group each present their people directory as one
white-equivalent group on the neutral gray canvas. Every adjacent pair is
separated by the established 2 dp `surfaceContainerLow` gap used by app-owned
Settings groups. The gap spans the group's 16 dp outer margin, while each
current interactive Material `ListItem` keeps its native 16 dp content inset,
12 dp avatar-to-text relationship, complete-row state layer, selection
semantics and adaptive measurement. Rows remain individually lazy and keyed;
they do not become separate cards.

New Group's existing 48 dp-minimum compact search keeps its editing, focus,
clear, IME and accessibility behavior but changes its container role to
`surfaceContainerLowest`, matching the grouped people surface. Its 16 dp outer
margin and full-rounded shape remain unchanged. New Message retains its 56 dp
standalone search treatment. Set Up Group's later read-only grouped treatment
is recorded in WN-ANDROID-0088.

Evidence: pinned iOS `NewChatView.swift` at `0bd7cba`,
`WhiteNoiseCompactSearchField.kt`, `ChatCreationScreens.kt`,
`ChatsScreenTest`, `chats-and-chat-creation.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Compose lazy lists](https://developer.android.com/develop/ui/compose/lists)
and [Compose search](https://developer.android.com/develop/ui/compose/components/search-bar).

## WN-ANDROID-0088 — Set Up Group completes the grouped member hierarchy

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Set Up Group now presents its read-only Members list as the same single
white-equivalent segmented group used by the preceding New Message and New
Group directories. The group keeps a 16 dp outer margin, native 16 dp row
content inset, Material's 12 dp avatar-to-text relationship, and the app's
2 dp canvas-tone gaps between adjacent rows. Members remain noninteractive;
the added surface does not imply selection or editing on this review step.

The shared 120 dp `ProfileAvatar` accepts an optional empty-monogram icon. Set
Up Group supplies the existing Material group symbol while both name and photo
are empty, replacing the ambiguous question-mark fallback without creating a
second avatar component. Entering a name still produces its monogram, and a
chosen photo still replaces the monogram through the unchanged system-owned
photo flow. Fields, validation, relay copy, scrolling, and the pinned Create
Group action remain unchanged.

Evidence: `ProfileAvatar.kt`, `ChatCreationScreens.kt`, `ChatsScreenTest`,
`chats-and-chat-creation.md`, `ui-metrics.md`, and `feature-inventory.md`.
Source: [Compose lists](https://developer.android.com/develop/ui/compose/lists).

## WN-ANDROID-0089 — Sign Out adopts the completed settings hierarchy

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Sign Out preserves the accepted product behavior: wipe is the default, exact
profile-name confirmation gates a wipe, retaining data removes only the
session, named progress blocks dismissal, and completion routes to the profile
switcher or Welcome from authoritative remaining state. Android keeps its
native modal bottom sheet and trailing Close action rather than copying the
iOS leading circular control.

The sheet now starts Expanded and exposes no partial anchor. Active identity
and wipe choice are peer rows in one white-equivalent group separated by the
app's 2 dp canvas-tone divider. The selected wipe consequence moves to the
shared external 32 dp helper line. Exact-name confirmation retains both the
section instruction and persistent Material field label; its consequence
helper uses the same external line. Turning wipe off clears confirmation and
focus, and the Done IME action clears focus.

The sheet allows native drag, Back and Close dismissal while idle, then vetoes
every transition to Hidden and disables Close during progress. Its pinned
error action uses the sheet canvas role at zero tonal elevation, maintaining
one continuous task surface without changing disabled, ready, progress or
error semantics.

Evidence: `DestructiveScreens.kt`, `DeveloperDestructiveScreenTest`,
`developer-and-destructive-flows.md`, `ui-metrics.md`, and
`feature-inventory.md`.
Sources: [Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
[Compose switches](https://developer.android.com/develop/ui/compose/components/switch),
and [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input).

## WN-ANDROID-0090 — Mute duration is an aligned rounded radio group

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

`MuteDurationDialog` keeps WN-ANDROID-0039's native `AlertDialog`, five
immediate choices and safe Cancel/Back/outside dismissal. Its presentation no
longer nests `ListItem` inside the dialog text slot. It reuses the app's
whole-row `WhiteNoiseDialogChoiceRow`: the native radio begins on the dialog
title/text content line, the label keeps the standard 16 dp control
relationship, and the complete row remains a 56 dp-minimum radio target.

The choice column is one selectable group. A supplied current duration is
visible through the native selected radio and exposed through radio semantics.
Each row clips its Material hover, focus, press and ripple state to the shared
large rounded shape before selection, preventing the prior squared-off state
layer. Selection still applies immediately; no duration, state mutation, or
dismissal behavior changes.

Evidence: `MuteDurationDialog.kt`, `WhiteNoiseDialogs.kt`, `ChatsScreen.kt`,
`ChatInfoScreens.kt`, `MaterialSheetTest`, `chats-and-chat-creation.md`,
`chat-and-group-information.md`, `ui-metrics.md`, and `feature-inventory.md`.
Sources: [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
and [Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button).

## WN-ANDROID-0091 — Dialog choice state layers keep an outer gutter

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

The shared immediate-choice row keeps its radio and label on the existing
dialog-owned content line, but its clipped selectable surface now expands 16 dp
past that line in both directions. Against Material's default 24 dp alert-dialog
content inset, this leaves an 8 dp transparent gutter between the rounded
pressed, focused, hovered, or selected state layer and the dialog edge.

The wider surface restores 16 dp of internal horizontal padding before laying
out the unchanged native radio target and label relationship. Content therefore
does not move when the feedback region grows. Mute and the Settings choice
dialogs continue to share this component, accessibility semantics, 56 dp
minimum target, immediate selection, and safe dismissal behavior.

Evidence: `WhiteNoiseDialogs.kt`, `MaterialSheetTest`,
`chats-and-chat-creation.md`, `chat-and-group-information.md`,
`ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0092 — New Group selection preserves grouped row geometry

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

New Group continues to use Material's toggleable `ListItem` so each person
keeps selected semantics, full-row interaction, focus, hover, press and ripple
feedback. Its selected container color already matches the resting container.
The selected shape now also resolves to the row's current positional segmented
shape, preserving the same sole, first, middle, or last corners after a person
is chosen.

The existing trailing vector check remains the only added visual selection
indicator. This keeps adjacent rows reading as one stable group while the
selected-avatar strip continues to provide ordered removal controls.

Evidence: `ChatCreationScreens.kt`, `ChatsScreenTest`,
`chats-and-chat-creation.md`, `ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0093 — Person Profile balances identity spacing and tones About

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Person Profile retains the established Share & Connect identity scale rather
than introducing another profile-only size system: the avatar remains 32% of
the active pane clamped to 104–152 dp, the name remains
`headlineSmall`/semibold, the verified address remains `bodyLarge` with a
20 dp filled seal, and the public-key capsule remains the shared 240 dp visual
inside its 48 dp target.

The centered identity stack now applies the same 16 dp relationship above and
below the name. When About exists, its rounded group begins 16 dp after the
name and uses `surfaceContainerHigh` instead of the white-equivalent action
group color. Centered italic `bodyLarge` text remains
`onSurfaceVariant`, providing the quieter gray-on-gray hierarchy visible in
the pinned iOS evidence while preserving Android semantic light/dark colors.
Address and public key continue to follow About with the existing 8 dp related
spacing; action behavior and product state do not change.

Evidence: `ChatCreationScreens.kt`, `ChatsScreenTest`,
`chats-and-chat-creation.md`, `ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0094 — Conversation identity and search use one compact header system

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

The conversation identity target keeps its complete Button semantics and Chat
Info navigation, but removes its visible press/ripple indication at the user's
explicit request. This is a conversation-header-only feedback exception: Back,
debug and every other Material action keep native state layers. The identity
uses a 40 dp avatar beside a title/metadata column, with a 4 dp horizontal
relationship gap. A negative 2 dp arrangement between the native
`titleMedium` and `labelSmall` line boxes is the user-approved optical
exception that reduces their visible leading by about 30% without changing
either type style. Both remain centered on the same vertical axis and free to
grow for accessible text. A transparent 48 dp-minimum row preserves the
Android touch target without adding visible container padding. This replaces
the former 36 dp avatar and 8 dp horizontal gap.

Conversation search reuses `WhiteNoiseCompactSearchField`, including the
same 48 dp minimum, `bodyLarge` input, rounded tonal container, Search symbol,
Clear action, focus, keyboard action and growth behavior already used by
Chats. Its leading app-bar action uses the same Android Back treatment as
Chats and closes search in place; system Back does the same. Search is
available only from the existing equal Chat Info/Group Info quick action,
which returns to the conversation with search focused. The conversation root
app bar has no Search action. Draft state remains untouched.

Previous/next/count controls retain their compact Material composition but now
apply IME as well as navigation-bar insets, so they remain immediately above
the software keyboard. Matching messages all remain at full contrast; only
nonmatching messages are subdued to 38%. This explicitly supersedes the prior
current-result-only contrast rule and selected-bubble outline. The current
position still comes from the newest-first result index and is announced only
on the current message, while the visible count remains a polite live region.

Every visible query occurrence in message text, visible group-author names,
attachment labels, link titles and link domains uses Android's platform
`Color.CYAN` with black foreground. Compose's standard span background is
square, so one scoped text-layout renderer groups exact glyph bounds per line
and draws a flush 4 dp rounded background. Case- and diacritic-insensitive
range mapping matches the existing search projection without changing the
stored text. Cyan is the user-approved semantic search exception to the
ordinary monochrome palette; contrast, result position and navigation keep
the state understandable without color.

Evidence: `ConversationScreen.kt`, `SearchHighlightedText.kt`,
`TimelineMessageContent.kt`, `MessageInteractionsUi.kt`,
`SearchHighlightedTextTest`, `ConversationScreenTest`,
`ChatInfoScreenTest`, `message-interactions-and-search.md`,
`chat-and-group-information.md`, `ui-metrics.md`, and `feature-inventory.md`.
Sources: [Compose search](https://developer.android.com/develop/ui/compose/components/search-bar),
[app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
[window insets](https://developer.android.com/develop/ui/compose/system/insets-ui),
and [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

## WN-ANDROID-0095 — Composer uses an overlay, anchored acquisition, and inline voice

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

The available conversation composer moves from Scaffold bottom-bar ownership
to an in-window overlay with an opaque adaptive surface and 1 dp top divider.
Only its measured compact height is reserved below the timeline. The separate
48 dp Add control and 48 dp-minimum Foundation editor capsule provide two
directly draggable endpoints: content-driven compact and expanded from 24 dp
below the chat header to the IME or safe bottom. Expansion preserves focus;
only a newest-visible timeline is translated by the exact composer travel.
Android retains Back, focus, insets, direction locking, interruptible spring
settling, minimum targets, and accessibility alternatives.

The Add control uses the shared anchored Material menu for Camera, Photos and
videos, Files, and Contact. GIF is no longer acquired here but remains valid
persisted and received content. The ordered inline shelf owns previews and
removal while platform camera, Photo Picker (up to 20), and document contracts
retain their system presentation.

Deterministic speech is an inline Idle/Recording/Review state machine. A
400 ms hold starts recording; Stop, Cancel, playback, transcription, the
anchored Voice/Text/Both format choice, editing, and submission remain in the
composer. Exactly one outgoing message preserves the simulated duration.
Recreation converts an active recording to Review and restores Review or the
expansion endpoint safely.

This supersedes WN-ANDROID-0039 only for the composer attachment chooser and
voice-review sheet. WN-ANDROID-0039 still governs the deterministic Contact
picker and every other ordinary app-owned sheet.

Evidence: `ComposerModels.kt`, `AppViewModel.kt`,
`ConversationComposer.kt`, `ConversationScreen.kt`,
`ComposerModelsTest`, `AppViewModelTest`, `ConversationScreenTest`, and
`composer-media-and-speech.md`.
Sources: [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures),
[gesture animation](https://developer.android.com/develop/ui/compose/animation/advanced),
[window insets](https://developer.android.com/develop/ui/compose/system/insets-ui),
[Material menus](https://developer.android.com/develop/ui/compose/components/menu),
[Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker),
and [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/semantics).

## WN-ANDROID-0096 — Composer chrome and recording waveform are optically lighter

- Date: 2026-08-30
- Status: Stop action and popup placement superseded by WN-ANDROID-0097;
  other decisions remain current

The conversation composer overlay no longer paints a full-width adaptive
surface or top divider. Only the separate Add control and editor capsule own
containers, so the timeline canvas remains continuous behind their margins.
This supersedes WN-ANDROID-0095 only for the host backing and divider.

The Add popup remains the shared Material menu but uses a scoped 2 dp optical
gap directly above its 48 dp trigger. Idle waveform artwork is reduced to
24 dp to match the visible Material Add icon while both retain 48 dp targets.
Recording uses a visible tonal Stop Recording button and a 24 dp waveform of
2 dp bars separated by 2 dp. The precise elapsed state still ticks every
100 ms; presentation advances a deterministic trailing sample every 200 ms,
instead of regenerating the whole waveform, to make motion calmer and more
natural. Review uses the same compact waveform geometry.

Evidence: `ComposerModels.kt`, `ConversationComposer.kt`,
`ComposerModelsTest`, `ConversationScreenTest`, `ui-metrics.md`,
`composer-media-and-speech.md`, and `feature-inventory.md`.

## WN-ANDROID-0097 — Composer controls attach to their source and contact sharing expands

- Date: 2026-08-30
- Status: Add popup gap superseded by WN-ANDROID-0099 and popup shadow by
  WN-ANDROID-0100; other decisions remain current

Recording replaces the textual tonal Stop action with the official red 20 dp
Stop symbol inside a 48 dp `IconButton`. Every composer submission uses the
official upward arrow inside the existing 32 dp filled circle and 48 dp target.
Voice Review uses a 32 dp filled Play/Pause circle inside a 48 dp action. Its
pre-transcription command pairs an official 20 dp chat-bubble symbol with
light-gray **Transcribe** text. Once a transcript exists, the compact
Voice/Text/Both selector moves above playback and text, matching the accepted
information order without changing format behavior or accessible labels.

Material's stock popup provider enforces a 48 dp window-edge margin. At the
bottom composer edge that margin rejects the correct above-trigger coordinate
and visually detaches the popup. Add and Message Format therefore keep
`DropdownMenuPopup`, `DropdownMenuGroup`, Material items, focus, Back/outside
dismissal, RTL alignment, and popup motion, but use a scoped public
`DropdownMenuPopupPositionProvider` that attaches the group exactly 2 dp above
its source and calculates the transform origin from that source. Material
offers a configurable shadow elevation, not a direction-specific shadow; these
upward groups use zero elevation so shadow blur cannot visually consume the
requested 2 dp clear gap. Every other app menu retains Material's adaptive
provider and default shadow.

Contact sharing remains an app-owned Material modal sheet but now opens
expanded with no partial anchor. It uses the ordinary neutral sheet canvas, a
standard searchable field, and one white-equivalent segmented contact group
with 2 dp canvas-tone separators, names, short public keys, and 48 dp avatars.
Material continues to own the sheet's width, top shape, handle, insets, drag,
Back, and dismissal. Selection still queues exactly one deterministic contact
attachment.

This supersedes WN-ANDROID-0096's tonal textual Stop action and stock-provider
popup placement. Its transparent host, 24 dp idle/live waveform geometry and
200 ms visible waveform advance remain current.

Evidence: `WhiteNoiseMenus.kt`, `ConversationComposer.kt`, official
`ic_arrow_upward.xml` and `ic_stop.xml`, `ConversationScreenTest`,
`composer-media-and-speech.md`, `ui-metrics.md`, and `feature-inventory.md`.
Sources: [Compose menu API](https://developer.android.com/reference/kotlin/androidx/compose/material3/DropdownMenu.composable),
[Menu anchor positioning](https://developer.android.com/reference/kotlin/androidx/compose/material3/MenuAnchorPosition),
and [Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets-partial).

## WN-ANDROID-0098 — Voice-review text actions separate targets from state layers

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

Voice Review keeps accessible 48 dp-minimum touch targets for Transcribe and
the Voice/Text/Both Message Format selector, but no longer lets their visible
press treatment fill those targets. Each outer target owns click, focus,
semantics and a remembered `MutableInteractionSource` without drawing an
indication. A separate inner pill clips the shared Material ripple to a 32 dp
minimum around the icon/label or label/chevron, with 8 dp horizontal and 4 dp
vertical content padding plus 4 dp transparent horizontal breathing room
inside the target. The visible pill and target remain minimums rather than
fixed heights, so larger text can grow instead of clipping.

This applies the existing WN-ANDROID-0054 target/visual-layer pattern to the
two voice-review text actions. Voice state, anchored menu placement, semantics,
keyboard behavior and submission do not change.

Evidence: `ConversationComposer.kt`, `ConversationScreenTest`,
`composer-media-and-speech.md`, `ui-metrics.md`, and `feature-inventory.md`.
Sources: [Handling interactions](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions),
[InteractionSource](https://developer.android.com/reference/kotlin/androidx/compose/foundation/interaction/InteractionSource),
and [Compose accessibility testing](https://developer.android.com/codelabs/basic-android-kotlin-compose-test-accessibility).

## WN-ANDROID-0099 — Composer Add menu matches the selector's visible separation

- Date: 2026-08-30
- Status: Shadow treatment superseded by WN-ANDROID-0100; other decisions remain current

The Message Format popup remains 2 dp above its transparent 48 dp target. Its
32 dp visible pill is centered inside that target, so the reviewed visible
pill-to-menu separation is 10 dp. The Add control fills its entire 48 dp target;
its popup therefore uses a direct 10 dp anchor gap to match that accepted
visible separation instead of retaining WN-ANDROID-0097's barely perceptible
2 dp gap. Both upward popups retain zero shadow elevation, anchored motion,
RTL alignment, focus, Back and outside dismissal.

Within the compact Transcribe pill, the official chat-bubble or progress
symbol and label now use only the shared 8 dp related-control spacing. The
removed duplicate 8 dp spacer had accidentally produced a 16 dp gap. Target,
state-layer, type, color, transcription and accessibility behavior remain
unchanged.

Evidence: `ConversationComposer.kt`, `ConversationScreenTest`,
`composer-media-and-speech.md`, `ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0100 — Upward composer menus retain native elevation

- Date: 2026-08-30
- Status: Approved by explicit user direction and implementation

The Add and Message Format popups restore Material's native
`MenuDefaults.ShadowElevation` instead of forcing zero elevation. In the
pinned Material 3 implementation this resolves through the menu token to
Level 2 (3 dp), separating each popup from timeline content while preserving
the accepted 10 dp visible Add gap, the format selector's 2 dp target gap,
above-anchor placement, lower-edge transform origin, focus, RTL, motion, Back
and outside dismissal. No direction-specific or custom shadow is introduced.

This supersedes only the zero-shadow portions of WN-ANDROID-0097 and
WN-ANDROID-0099.

Evidence: `ConversationComposer.kt`, pinned Material 3 alpha25 sources,
`composer-media-and-speech.md`, `ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0101 — Floating composer controls own contrast over an edge-to-edge timeline

- Date: 2026-08-31
- Status: Control color roles superseded by WN-ANDROID-0102; edge-to-edge
  viewport and transparent-host decisions remain current

The transparent composer host remains free of a full-width surface or divider.
Instead, its individual controls provide separation over mixed timeline media:
the available idle Add action uses the Material primary/on-primary pair, while
the editor capsule keeps its neutral container and gains a 1 dp adaptive
on-surface outline. Voice Review Cancel remains neutral so the primary treatment
continues to identify attachment acquisition rather than every leading action.

The available-composer timeline viewport now paints to the physical bottom
edge. Scaffold's top and horizontal safe content padding still position the
conversation, but its bottom safe inset moves into lazy-list content padding
together with the measured compact composer height. This allows media to scroll
behind the floating controls and gesture-navigation area while the newest item
can still settle fully above them. The composer overlay separately consumes the
navigation-bar inset and continues to follow the IME, avoiding the former white
bottom cutout and inset duplication.

This refines WN-ANDROID-0096's transparent host without restoring its
superseded backing or divider.

Evidence: `ConversationScreen.kt`, `ConversationComposer.kt`,
`ConversationScreenTest`, `composer-media-and-speech.md`, and
`feature-inventory.md`. Sources: [Material 3 design system](https://developer.android.com/develop/ui/compose/designsystems/material3),
[Material Surface](https://developer.android.com/reference/kotlin/androidx/compose/material3/Surface.composable),
[edge-to-edge codelab](https://developer.android.com/codelabs/edge-to-edge), and
[Compose insets](https://developer.android.com/develop/ui/compose/system/insets-ui).

## WN-ANDROID-0102 — Composer contrast uses Material's medium-emphasis roles

- Date: 2026-08-31
- Status: Superseded by WN-ANDROID-0103

The transparent host and edge-to-edge timeline remain unchanged, but the two
floating controls step down from WN-ANDROID-0101's strongest contrast. The
editor capsule border changes from `onSurface` to Material's primary outline
role, which resolves to the theme's medium gray and remains adaptive. The idle
Add action changes from `primary`/`onPrimary` to
`secondary`/`onSecondary`. In White Noise's monochrome scheme, `secondary` is
the intended medium-gray filled accent; `secondaryContainer` would reproduce
nearly the same light tone as the earlier low-contrast button. Review Cancel
remains neutral and disabled Add retains semantic disabled colors.

This supersedes only WN-ANDROID-0101's Add and capsule color-role choices.

Evidence: `WhiteNoiseTheme.kt`, `ConversationComposer.kt`,
`composer-media-and-speech.md`, `ui-metrics.md`, and `feature-inventory.md`.
Sources: [Material 3 color usage](https://developer.android.com/develop/ui/compose/designsystems/material3),
[Compose ColorScheme](https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme),
and [filled tonal buttons](https://developer.android.com/reference/kotlin/androidx/compose/material3/FilledTonalButton.composable).

## WN-ANDROID-0103 — Composer restores primary Add and softens only the outline

- Date: 2026-08-31
- Status: Approved by explicit user direction and implementation

The idle Add action returns to `primary`/`onPrimary`, matching the app's other
filled actions and resolving to black with a white icon in the light theme.
The editor capsule keeps its neutral container but steps its 1 dp boundary down
from `outline` to `outlineVariant`, Material's lower-emphasis outline role. The
result preserves separation over mixed timeline content without making the
entire composer visually heavy. Review Cancel remains neutral and disabled Add
retains semantic disabled colors.

This supersedes WN-ANDROID-0102's Add and capsule color-role choices only. The
transparent host and edge-to-edge timeline remain unchanged.

Evidence: `WhiteNoiseTheme.kt`, `ConversationComposer.kt`,
`composer-media-and-speech.md`, `ui-metrics.md`, and `feature-inventory.md`.
Source: [Compose ColorScheme](https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme).

## WN-ANDROID-0104 — Chat camera capture honors the merged camera permission

- Date: 2026-08-31
- Status: Implemented in response to reported regression and platform requirement

The external composer camera remains Android's `TakePicture` contract with a
non-exported FileProvider destination. The merged app now also declares
`CAMERA` for its approved app-owned QR scanners. Android throws a
`SecurityException` when an app that declares this permission launches
`ACTION_IMAGE_CAPTURE` before the permission is granted, so the former
permissionless composer launch had regressed into the generic attachment
error.

Selecting **Camera** now requests camera access just in time when needed and
launches the pending capture immediately after approval. A denial uses the
accepted **Camera Access Needed** recovery with **Allow Camera**, or **Open
Settings** after Android no longer offers the permission prompt. Returning
from Settings resumes the same pending capture only when access was granted;
Cancel or dismissal clears it. Photo Picker, Files, Contact, ordered draft
state, FileProvider ownership and image preparation are unchanged, and no
storage permission is added.

This supersedes WN-ANDROID-0011 and the earlier composer brief only where they
described the external camera action as permissionless in the merged app.

Evidence: `ConversationComposer.kt`, `strings.xml`,
`composer-media-and-speech.md`, `feature-inventory.md`, and `README.md`.
Sources: [MediaStore `ACTION_IMAGE_CAPTURE`](https://developer.android.com/reference/android/provider/MediaStore#ACTION_IMAGE_CAPTURE),
[minimize permission requests](https://developer.android.com/privacy-and-security/minimize-permission-requests),
and [`ActivityResultContracts.TakePicture`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture).

## WN-ANDROID-0105 — Rich composer accessories split minimum targets from visual chrome

- Date: 2026-08-31
- Status: Implemented from explicit user direction; device visual acceptance pending

Photo, album, GIF, file, contact, link-preview, and reply removal now use one
shared accessory contract. The semantic/clickable target remains a transparent
48 × 48 dp square, while its visible control is a 20 × 20 dp circle containing
a 12 dp close symbol. Equal 6 dp top and end insets align that circle
concentrically with each card corner. The outer target has no indication; it
shares an interaction source with a state layer clipped to the visible circle,
so the accessibility target does not create an oversized halo.

Utility file/contact cards retain their accepted 72 dp height. Contact identity
uses a 40 dp avatar or monogram plus a visible one-line name. Filename layout
preserves the extension and final three stem characters while ellipsizing only
the leading stem. One natural-height quote component now owns both composer and
timeline replies: a 3 dp adaptive capsule begins 12 dp from the card edge, the
text follows after 10 dp, and author plus excerpt remain font-scale safe. Its
content geometry is shared while its container follows the parent: composer
quotes use an 8 dp inset and 16 dp radius inside the 24 dp composer, and message
quotes use an 8 dp inset and 8 dp radius inside the 16 dp bubble. Both pairs
remain concentric; message body content keeps its established 12 dp alignment.
Composer link previews use the same 8 dp inset and 16 dp radius as composer
quotes and attachment imagery rather than the former 4 dp vertical inset and
12 dp radius.

Complete composer mentions retain the shared search renderer's 4 dp rounded
glyph-run shape, but use the adaptive medium-neutral `outlineVariant` surface
with `onSurface` text. This explicit user correction replaces the visually
heavy `primary`/`onPrimary` inversion while preserving legibility in both
appearances.

After the user's 2026-09-03 size correction, draft review uses adjacent 56 dp
thumbnail targets containing unframed 48 dp crops, leaving an 8 dp visible gap.
Only the selected item receives a 1 dp `onBackground` ring, a single item has no
rail, and inclusion becomes a 22 dp check inside its own 48 dp target. That
target anchors to the fitted media rectangle rather than the pager page; its
visible circle sits inside the image's bottom-end corner with a 6 dp
edge inset, increased by 50% at the user's request on 2026-09-03. Draft media
has no added image margin, matching the sent viewer. The remaining values
translate the user-approved current iOS comparison at
`wn-ios-prototype@4c25393f0eb6` without repinning the Android baseline and keep
Android minimum-target and semantic behavior authoritative.

Evidence: `ComposerModels.kt`, `ConversationComposer.kt`,
`ConversationQuoteBlock.kt`, `ConversationScreen.kt`,
`ComposerModelsTest`, `ConversationScreenTest`,
`composer-media-and-speech.md`, `message-interactions-and-search.md`, and
`feature-inventory.md`. Source: [Compose minimum touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

## WN-ANDROID-0106 — Sent media has one exact-frame conversation projection and viewer

- Date: 2026-08-31
- Status: Implemented from explicit user direction; device visual acceptance pending

Conversation bubbles and Shared Content now consume the same chronological
photo/video projection. It flattens message order, attachment order, then image
order; identifies each frame by message, attachment, and image index; carries
sender, time, source-message, selected-image, and attachment metadata; and
excludes deleted or unavailable frames. Every visible album tile therefore
opens its exact frame in a complete chat-wide pager. Sent media deliberately
has no draft-style thumbnail rail.

Overlay chrome shows sender, sent time, current position, Close and More above,
with edge-aligned Share and Forward icon actions below. The bottom actions are
simple 24 dp symbols in transparent 48 dp `IconButton` targets rather than
wide filled buttons, following the user-provided Signal reference while
retaining native Android state layers and accessibility names. Media taps
toggle chrome without changing page geometry; touch exploration keeps named
actions available. Photos support
bounded 1×–4× pinch/pan and 1×/2× double-tap, disable pager movement while
zoomed, reset on page changes, and consume Back to reset before dismissal.
Named Zoom In, Zoom Out, and Reset actions retain non-gesture access. Existing
video handoff behavior remains.

Share stages only the exact JPEG/MP4 frame behind the existing FileProvider and
launches Android Sharesheet with temporary read access. Save uses exact-MIME
`CreateDocument` contracts and reports preparation/copy failures in place.
Forward reuses the searchable five-chat picker, accepts optional trimmed text,
and normalizes one selected album image into one Photo attachment. Go to
Message closes the viewer and targets the source message; Shared Content
replaces its information stack with that typed conversation route.

The user-directed forwarding polish applies to media and ordinary message
forwarding through their shared sheet. Search reuses the established compact
48 dp contextual field over the sheet's white-equivalent
`surfaceContainerLowest`; destinations form one 16 dp-inset segmented group
with 2 dp canvas-tone separators. Native multi-selection semantics stay on
each whole row, while selection is shown by one trailing check without
changing the row's positional shape or fill. The fixed title/search region
uses the sheet canvas at rest and `surfaceContainer` once the destinations
scroll, preserving the same one-step darker pinned-header cue used elsewhere.
That state colors Material's full rounded sheet cap and drag-handle area as
well as the title/search content; a separate `surfaceContainerLow` destination
canvas prevents the darker tone from leaking into the scrolling list.

For media forwarding, the optional one-to-four-line message and completion
action now share one chat-matched 48 dp-minimum, 24 dp-radius capsule. The
48 dp trailing action target contains the same 32 dp filled upward-arrow
treatment as the conversation composer and owns the selected destination count
as its accessibility name. This capsule overlays the destination list without
an opaque footer surface. Bottom content padding combines the measured overlay
height, relationship spacing, and system bottom safe area with a 24 dp minimum,
so every row scrolls fully clear of both the capsule and the gesture area.
The destination viewport does not apply that inset as a layout cutoff: it
draws through the bottom system area to the physical edge, while the floating
capsule applies navigation-bar padding independently. The modal content can
expand to 88% of the available height, preserving Material's rounded cap and
sheet gestures instead of becoming a full-screen route. Ordinary message
forwarding retains its direct completion action because it has no
accompanying-message editor.

This applies the user-approved current iOS capability at
`wn-ios-prototype@4c25393f0eb6` as scoped evidence without changing the pinned
baseline, adding storage, networking, permissions, or a third-party runtime.

Evidence: `ChatInfoModels.kt`, `AppViewModel.kt`, `AppRoute.kt`,
`WhiteNoiseNavHost.kt`, `TimelineMessageContent.kt`, `MediaViewer.kt`,
`ConversationScreen.kt`, `ChatInfoScreens.kt`, `MessageInteractionsUi.kt`,
their unit/Compose tests, `message-interactions-and-search.md`,
`chat-and-group-information.md`, and `feature-inventory.md`. Sources:
[Compose multitouch transforms](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch),
[Android Sharesheet](https://developer.android.com/training/sharing/send), and
[`CreateDocument`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument).

## WN-ANDROID-0107 — Conversation messages form one bottom-settled, source-preserving interaction surface

- Date: 2026-08-31
- Status: Implemented; build, lint, and unit tests pass. The complete 62-case
  conversation suite, including focused quick-reaction geometry and held-return
  LTR/RTL reply-indicator geometry, passes on the physical Pixel 8a. Current-
  build visual inspection confirms the evenly inset quick strip, fixed 28 dp
  pinned Signal emoji artwork, circular horizontal-ellipsis control, and that the
  weight-600 reply glyph remains attached to the bubble while reactions travel
  with it. User visual acceptance remains separate.

The available-composer timeline may draw behind its floating controls, but its
newest entry must settle fully above the measured compact composer and system
safe area. Initial positioning therefore waits for the composer measurement
and final lazy-list layout before resolving the exact chronological end. It is
not re-run for unrelated recompositions, while a newly sent message explicitly
settles to the same end. This corrects the race that could leave the last
message and time underneath the composer while preserving WN-ANDROID-0101's
edge-to-edge viewport.

Messages retain a full 16 dp tail-free bubble, 12 dp horizontal and 8 dp
vertical content insets, 2 dp same-cluster separation, and 16 dp cluster
separation. Direct chats omit identity; incoming group clusters use the
existing 30 dp avatar and 6 dp relationship gap. The group avatar terminates at
the bubble edge, excluding reactions and time, while the author label begins at
the bubble's 12 dp body inset. Selection reserves one stable 48 dp leading
control column for every direction and centers its Checkbox against message
content. A restrained whole-row state layer may reinforce the native checked
state, but message direction no longer moves the control or creates a bubble-
sized selection island.

Reaction metadata becomes one bubble-attached, nonwrapping line. Its visible
pill is 23 dp high and at least 31 dp wide, with a one-dp outline, 7 dp
horizontal content inset, 2 dp emoji/count gap, and no visible count for a
single reaction. Counted and `+N` pills grow naturally from that minimum and
neighboring visible pills retain a 3 dp gap. Their horizontal layout remains
compact while a 48 dp vertical target plus Compose's expanded minimum pointer
target preserves operability. The visible pill overlaps the bubble's bottom
edge by 9 dp. The bubble grows with its reaction/time metadata up to the
established 340 dp compact maximum. Within that width, the summary shows at
most four real reaction types plus one `+N` overflow pill and progressively
shortens the real set further only when required. The reaction rail occupies
the center-facing side and terminal time the opposite side, both inset 12 dp from the
bubble edge. The timestamp remains on the incoming start/left edge or outgoing
end/right edge in LTR whether or not reactions exist, and mirrors with the
message in RTL. Without reactions, time begins 2 dp below the bubble rather
than overlapping its content. The same 2 dp top gap applies when reactions are
present; only reaction pills cross the bubble edge. Timestamp text, the Sent
status fill, and Sending progress use the lower-emphasis `outline` neutral.
Outgoing terminal metadata includes the Sent check or Sending progress; the Sent state uses a 14 dp filled status container with
10 dp check artwork, and incoming metadata reserves no delivery icon. Failed
outgoing delivery reuses the exact timestamp geometry—end/right alignment,
12 dp edge inset, 2 dp top gap, 3 dp icon gap, `labelSmall`, and a 14 dp status
footprint—while the warning icon and **Not delivered, tap to retry** label use
the semantic error color. It never becomes a
detached wrapping shelf. A normal pill tap selects or replaces the active
profile's reaction but does not remove
an already selected reaction; removal remains explicit in the focused quick
strip or full picker. Message Details lists every reaction type and its people,
independent of the compact summary.

A deliberate hold keeps Compose recognition and haptic feedback, then lifts a
real rendering of the source message into a modal focused overlay. The quick
reaction strip sits above it and the ordered action surface below it, aligned
to incoming/outgoing direction and shifted within window and system-safe
bounds when the source is near an edge. An unusually tall source scales as one
unit to a 320 dp preview budget so the real bubble and complete command surface
remain available without substituting a generic summary. Scrim tap, system
Back, Escape, and Close dismiss. The surface retains Reply, Forward, Copy,
Select, Info and Delete policy, with Retry first when applicable. Conditional
recipient speech commands remain in this ordered surface: Read Aloud/Stop
Reading for received text, Transcribe for received voice without a local
transcript, then Show/Hide and Copy Transcript once available. Ordinary
bubbles show only active read-aloud progress or a deliberately revealed
transcript, never permanent speech command buttons. This source-preserving
composition supersedes WN-ANDROID-0012's message-action bottom sheet; the emoji
picker, configuration, forwarding, and confirmations remain Material sheets or
dialogs.

The focused preview uses the same adaptive reaction summary as the transcript.
Each quick reaction retains one 48 dp semantic target, while a shared
`MutableInteractionSource` sends hover, focus, and press feedback to a centered
40 dp circular Material state layer rather than painting the target's
rectangular slot. A 36 dp selected fill remains concentric inside it. The rail
keeps 4 dp on every outer edge and 4 dp between targets, giving the circular
state equal breathing room without shrinking accessibility or pointer reach.
Each emoji uses a fixed 28 dp visual box with the app-owned atlas renderer
specified by WN-ANDROID-0111 rather than scaling an ordinary typography token.
Signal's pinned artwork is cropped and scaled as imagery, so glyph font metrics
cannot clip the visual. The
final More Reactions target replaces the plus with Google's unmodified 24 dp
Material Symbols Rounded horizontal ellipsis inside a low-emphasis neutral
40 dp circle. It retains the same 48 dp semantics, concentric state layer, and
4 dp relationship to the rail as every reaction target.
The quick-reaction surface and command surface each keep the shared 8 dp visual
gap from the focused message. When the source has reactions, the lower gap is
measured from the visible pill edge rather than from the remaining transparent
portion of its 48 dp minimum interaction target. Scaled type may grow the
visible pill, but the 9 dp overlap and visible-edge menu gap remain unchanged.

A horizontal leading-to-trailing swipe invokes Reply at a 64 dp threshold,
caps the visual displacement at 96 dp with resisted overdrag, provides one
readiness haptic, and returns the bubble plus reactions as one unit. Layout
direction mirrors the gesture in RTL. Direction locking leaves vertically
dominant motion to timeline scrolling. Swipe reply is disabled during
selection, for deleted or unavailable messages, and whenever Reply is not an
available action. The existing named Reply accessibility action remains the
non-gesture equivalent. The indicator uses Google's 24 dp Material Symbols
Rounded Reply asset at weight 600 inside a 48 dp target anchored to the resting
bubble's semantic-start edge and vertical center. From the same gesture state,
it becomes visible after 5% progress, travels 10 dp in the swipe direction,
scales from 1.0 to 1.2, pulses to 1.8 over 200 ms at readiness, and follows the
bubble continuously through resisted overdrag and spring return.

This decision preserves product behavior from the pinned iOS baseline and the
explicitly requested scoped current-iOS comparison at
`wn-ios-prototype@4c25393f0eb6`. Signal Android at
`879651dc47a7b18b67e7aea52a25197875024680` supplies interaction evidence: its
bubble-attached bounded reaction summary, source-preserving reaction overlay,
semantic reply swipe, threshold feedback, resisted motion, and selection-bar
viewport handling inform the Android translation without copying its renderer
code or Signal product policy. WN-ANDROID-0111 separately records the user's
explicitly requested exact emoji-artwork import.

Evidence target: `ConversationScreen.kt`, `MessageInteractionsUi.kt`,
conversation state models, unit and Compose tests,
`shared-conversation-core.md`, `message-interactions-and-search.md`,
`ui-metrics.md`, and `feature-inventory.md`. Sources:
[lazy lists](https://developer.android.com/develop/ui/compose/lists),
[tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press),
[handling interactions](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions),
[drag, swipe, and fling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling),
and [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

## WN-ANDROID-0108 — Focused message actions use a soft material backdrop

- Date: 2026-09-01
- Status: The 24 dp blur and single-layer Dialog ownership remain implemented;
  the 80% neutral veil is superseded by WN-ANDROID-0114.

The focused-message modal replaces its flat 42% black scrim with a material-
like neutral backdrop. While the focused message, quick reactions, and command
surface remain sharp in the modal window, the underlying conversation Scaffold
receives a 24 dp Compose blur and the modal draws an 80% translucent
`surfaceContainerHigh` veil across the complete window. The result preserves
only indistinct conversation structure behind the focused content instead of
leaving readable dark bubbles under a harsh dim layer.

Because Compose `Dialog` also enables Android window dimming when it owns an
edge-to-edge floating window, this custom full-screen modal sets that separate
dim amount to zero. The app-owned veil remains the single contrast layer and
continues to own scrim taps; system Back, Escape, focus, safe bounds, and the
source-preserving action composition are unchanged. `Modifier.blur` is
supported on Android 12 and later. On older supported releases it is ignored by
Compose, so the translucent semantic-neutral veil remains the deliberate
graceful fallback rather than adding another rendering dependency or custom
bitmap pipeline.

This is a user-approved visual translation of current iOS
`regularMaterial` plus its low-opacity contrast overlay. It preserves the
material outcome without importing an Apple component or literal light-theme
color. Evidence: `ConversationScreen.kt`, `ConversationScreenTest.kt`,
`ui-metrics.md`, `message-interactions-and-search.md`, and
`feature-inventory.md`. Source: [Compose `Modifier.blur`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier).

## WN-ANDROID-0109 — Focused message commands reuse the app's native Material menu group

- Date: 2026-09-01
- Status: Implemented; shared-menu and complete conversation tests pass on the
  physical Pixel 8a. Idle and held-item visual inspection pass. User visual
  acceptance remains separate.

The focused-message dialog keeps ownership of source-message placement,
directional alignment, safe bounds, blur/veil, Back and outside dismissal. Its
command surface must not create a nested popup. It now uses
`WhiteNoiseMenuGroup`, the non-popup half of the same shared Material component
used by Chats filtering and all app-owned dropdowns. Both variants render
through one `WhiteNoiseMenuItems` implementation backed by
`DropdownMenuGroup`, position-aware `MenuDefaults.itemShape`, and the new
`DropdownMenuItem` overloads.

This removes the focused surface's custom `Surface` and full-width
`Row.clickable` rows. Material now owns the per-position item shape, clipped
pressed state layer, standard `surfaceContainerLow` color,
typography, horizontal padding, 48 dp minimum target, 20 dp leading icon,
elevation, enabled semantics, and destructive error colors. The established
action policy/order and the handler's dismiss-before-dispatch behavior are
unchanged, as are the quick reactions, real message preview and 8 dp visible-
edge gaps.

The embedded-group regression verifies native Button semantics and targets
without popup dismissal ownership. Focused-message coverage verifies the same
for Reply and Delete, while the complete 62-case conversation class preserves
reaction-aware spacing, large type, selection, details, forwarding and every
conditional action. Current-build Pixel 8a inspection covers the idle surface
and a held Reply frame; the first item's state layer has rounded inner corners
and correct outer clipping instead of the former square band.

Evidence: `WhiteNoiseMenus.kt`, `ConversationScreen.kt`,
`WhiteNoiseMenuTest.kt`, `ConversationScreenTest.kt`, `app-menus.md`,
`message-interactions-and-search.md`, `ui-metrics.md`, and
`feature-inventory.md`. Sources: [Compose menus](https://developer.android.com/develop/ui/compose/components/menu),
[Material menu specifications](https://m3.material.io/components/menus/specs),
and [Menu defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/MenuDefaults).

## WN-ANDROID-0110 — Reaction picking uses a sectioned, navigable emoji surface

- Date: 2026-09-01
- Status: Implemented; the static gate and complete 62-case conversation suite
  pass. Physical Pixel 8a inspection covers the full catalog, IME-visible
  search, and scroll-synchronized category navigation. User visual acceptance
  remains separate.

The former picker exposed only one 16-item category at a time behind a row of
text chips. Its search could match only a literal glyph, so it did not preserve
the accepted iOS catalog or provide a credible full emoji task. The replacement
keeps the shared Material modal sheet but presents the complete pinned White
Noise catalog in one continuous Compose lazy grid: deterministic Recently Used,
Smileys & People, Animals & Nature, Food & Drink, Activities, Travel & Places,
Objects, Symbols, and Flags.

The sheet uses Material's Expanded and Hidden anchors with content bounded to
88% height. This keeps the pinned bottom rail inside the visible sheet at rest;
Signal can pin a separate View tab bar to its dialog window during a partial
state, while Compose's sheet content would otherwise be clipped below that
viewport. A pinned bottom rail tracks the first visible section while scrolling
and scrolls to a tapped category header. Configure Reactions is its fixed leading gear
action. Categories remain horizontally reachable on compact widths; each uses
an official 22 dp Material Symbols Rounded glyph, a 48 dp target, and a 36 dp
selected circle. The rail leaves while a nonblank search query owns the surface.
Search matches category vocabulary, common reaction aliases, and literal emoji,
with named Clear and no-results behavior. Section labels are headings. Every
emoji uses a circular 48 dp Material target around one fixed 32 dp pinned Signal
sprite, independent of font scale.

Signal Android at `879651dc47a7b18b67e7aea52a25197875024680` confirms the
Android interaction structure: one sectioned grid, expansion on search/category
navigation, a scroll-synchronized bottom category recycler, search-time rail
dismissal, and recent-emoji grouping. WN-ANDROID-0111 separately authorizes the
exact pinned sprite pages and generated atlas metadata. No Signal renderer,
downloader, database search index, or persistence implementation is copied.
The category contents and ordering come from the accepted read-only White Noise
iOS baseline; parsing, rendering, search, state and Compose UI are app-owned
Kotlin, and no runtime dependency or network request is added.

Evidence: `EmojiCatalog.kt`, `MessageInteractionModels.kt`,
`MessageInteractionsUi.kt`, `EmojiCatalogTest.kt`, `ConversationScreenTest.kt`,
`message-interactions-and-search.md`, `ui-metrics.md`, and
`feature-inventory.md`. Sources: [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
[Compose lazy grids](https://developer.android.com/develop/ui/compose/lists),
[Material search](https://developer.android.com/develop/ui/compose/components/search-bar),
[window insets](https://developer.android.com/develop/ui/compose/system/insets),
and [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

## WN-ANDROID-0111 — Reaction visuals use the pinned Signal Android emoji atlas

- Date: 2026-09-01
- Status: Implemented; atlas-resolution unit/instrumentation tests pass and
  current-build physical Pixel 8a inspection confirms the focused strip,
  transcript pill, and complete picker render the Signal artwork without
  clipping. The complete 62-case conversation suite and static gate pass.

The user's explicit correction requires the same emoji artwork used by Signal
Android, not merely the same reaction choices or native-device approximations.
This supersedes WN-ANDROID-0107 and WN-ANDROID-0110 only where they previously
selected the device emoji font for reaction presentation.

The app bundles the 20 WebP sprite pages and generated `emoji_data.json` from
Signal Android commit `879651dc47a7b18b67e7aea52a25197875024680`, unchanged,
under `app/src/main/assets/signal_emoji/`. Signal's license and notice are
bundled beside those assets, and exact provenance and hashes are recorded in
`docs/references/signal-emoji-assets.md`.

Production code does not copy Signal's Java/Kotlin renderer. The app-owned
Compose implementation parses only the manifest's stable `emoji` object,
decodes its UTF-16 hexadecimal keys, caches atlas pages, and draws the selected
tile. It follows the observable atlas geometry used by Signal: 66 px source
tiles, 16 columns, a one-pixel crop on every edge, bitmap-filtered scaling into
a square destination. Fixed 28 dp focused quick-reaction boxes, 32 dp picker
boxes, and 16 dp pill boxes therefore contain image content rather than text
baselines, removing the font-ascent/descent clipping that produced the rejected result. Missing atlas
entries retain a defensive native-text fallback, while instrumentation proves
that every accepted White Noise catalog entry resolves to bundled artwork.

The import adds no Signal runtime, updater, database, network fetch, or
persistence behavior. White Noise continues to own catalog semantics, recents,
search, reaction state, accessibility descriptions, targets, pressed states,
layout, and rendering code.

Evidence: `SignalEmoji.kt`, `SignalEmojiTest.kt`, `SignalEmojiAssetTest.kt`,
the reaction-surface call sites and Compose tests,
`signal-emoji-assets.md`, `message-interactions-and-search.md`,
`ui-metrics.md`, and `feature-inventory.md`.

## WN-ANDROID-0112 — Focused reaction and command surfaces share one menu elevation system

- Date: 2026-09-01
- Status: Implemented; focused geometry tests pass on the physical Pixel 8a.
  Current-build idle and held-reaction inspection pass. User visual acceptance
  remains separate.

The focused quick-reaction rail now uses the exact standard Material menu-group
container color, Level 0 tonal elevation, and Level 2 shadow elevation already
owned by the command group below it. This makes the two controls read as one
interaction family without nesting the reaction choices inside the command
menu or changing their separate shapes.

Reaction artwork is reduced from 32 dp to 28 dp inside the existing 36 dp
concentric selected fill and 40 dp circular press layer. The semantic target
remains 48 dp, the rail inset and inter-target spacing remain 4 dp, and the
same circle-clipped state layer continues to wrap every quick reaction rather
than only the visually distinctive rocket. The More Reactions ellipsis retains
its own neutral 40 dp circle and the same 48 dp target.

The focused composition's scroll viewport now includes explicit 8 dp top and
bottom shadow-safe gutters. These gutters are outside the visible rail-to-
message and message-to-command relationships, which remain exactly 8 dp, but
inside the viewport's clipping boundary. Material's soft shadow can therefore
finish around both elevated surfaces instead of being cut flat at the top or
bottom edge when the composition is content-sized or scrolled to an extreme.

Evidence: `ConversationScreen.kt`, `ConversationScreenTest.kt`,
`message-interactions-and-search.md`, `ui-metrics.md`, and
`feature-inventory.md`. Sources: pinned Signal Android reaction-overlay layout
at `879651dc47a7b18b67e7aea52a25197875024680` and Material
`MenuDefaults`/`DropdownMenuGroup` source for the pinned Compose version.

## WN-ANDROID-0113 — Device testing requires an explicit current user request

- Date: 2026-09-01
- Status: Approved by explicit user direction

Routine agent validation is host-side: compilation, relevant unit tests, lint,
debug APK assembly, and instrumentation-test APK compilation as appropriate to
the change. The presence of a tethered phone does not change that default.

The agent must not use `adb`, execute connected instrumentation, start an
emulator, install or launch the app on a device, interact with a phone/emulator,
or capture device screenshots unless the user's current request explicitly
asks for device/emulator testing or visual/hands-on inspection. Authorization
from an earlier request does not carry into later implementation or polish
requests, and a visually oriented change is not by itself authorization.

If evidence can only be obtained on a device, the agent reports what requires
that check and waits for the user to request it. Device execution and visual
verification may be claimed only after such an authorized current-build pass.
Compiling and packaging Compose instrumentation tests remains within the
host-side boundary and does not authorize running them.

## WN-ANDROID-0114 — Focused actions use a restrained translucent-black backdrop

- Date: 2026-09-01
- Status: Superseded on 2026-09-03 by WN-ANDROID-0121.

The 80% `surfaceContainerHigh` wash left insufficient separation between an
incoming light message bubble and the blurred conversation behind it. The
focused-message overlay now keeps the accepted 24 dp background blur but uses
a 24% translucent black veil across the complete window. This is substantially
lighter than the rejected former 42% black scrim while providing a clearer
neutral contrast boundary around light focused content.

The app-owned veil remains the only contrast layer. Dialog window dimming stays
disabled, so the 24% value is not compounded by a second platform scrim. The
focused message, reaction rail, and command menu remain outside the veil and
therefore sharp. Scrim taps, Back, safe bounds, accessibility ownership, and
the Android 11-and-earlier no-blur fallback remain unchanged.

Evidence: `ConversationScreen.kt`, `ConversationScreenTest.kt`,
`message-interactions-and-search.md`, `ui-metrics.md`, and
`feature-inventory.md`.

## WN-ANDROID-0115 — Focused message timestamps use full-emphasis contrast

- Date: 2026-09-01
- Status: Superseded on 2026-09-03 by WN-ANDROID-0121.

The transcript keeps the accepted lower-emphasis `outline` neutral for time,
Sent fill, and Sending progress. When the real message is lifted into the
focused context-menu overlay, those same non-error metadata elements switch to
`onSurface`, which is black in the light theme. This restores contrast against
the translucent-black blurred backdrop without changing timestamp position,
padding, typography, delivery-icon geometry, or reaction layout. The semantic
error color continues to own failed delivery in both contexts.

Evidence: `ConversationScreen.kt`, `message-interactions-and-search.md`, and
`ui-metrics.md`.

## WN-ANDROID-0116 — Focused actions dismiss from every visual empty-space tap

- Date: 2026-09-01
- Status: Implemented; host compilation and instrumentation-test compilation
  pass. Device and visual verification remain unclaimed until explicitly
  requested.

The focused action composition previously placed a no-op click target on its
full-width alignment column. That invisible target included blank horizontal
space beside narrow incoming and outgoing messages, so taps there were
consumed instead of reaching the full-window dismiss action.

The alignment and scroll column no longer consumes taps. Only the visible
reaction surface, the rendered message-plus-metadata bounds, and the command
group block backdrop dismissal. Every visually empty point—including space
beside or between those surfaces—now reaches the single full-window dismiss
target on the first completed tap. Surface actions, scrim semantics, system
Back, Escape, placement, and scrolling remain unchanged.

Evidence: `ConversationScreen.kt`, `ConversationScreenTest.kt`,
`message-interactions-and-search.md`, and `ui-metrics.md`.

## WN-ANDROID-0117 — Custom interaction feedback stays inside visible elements

- Date: 2026-09-01
- Status: Implemented for the audited custom targets; host Kotlin and
  instrumentation-test compilation pass. Device and visual verification remain
  unclaimed until explicitly requested.

Custom White Noise targets may remain larger than their artwork for touch,
keyboard, and accessibility reach, but their press/hover/focus/ripple feedback
does not inherit that transparent geometry. The target suppresses its own
indication, shares one `MutableInteractionSource`, and draws the Material state
layer on the actual clipped visual shape. Unbounded or intentionally
overflowing feedback requires an explicit component decision.

The audit corrects the transcript message long-press layer from the full row to
the 16 dp bubble, reaction feedback from the 48 dp target to the 23 dp pill,
reply-quote feedback to its rounded quote surface, draft-media feedback from
the 72 dp target to the 64 dp thumbnail, selected-person removal feedback from
the 80 dp column to the avatar, and attachment-card feedback to its rounded
card. Existing compact composer actions, remove/inclusion controls, identity
copy capsules, focused reactions, menu groups, dialog rows, and diagnostics
switches already use clipped visual state layers. Material list rows remain
whole-row because that visible row is the intended element, and conversation
selection retains its separately approved whole-row state.

Evidence: `ConversationScreen.kt`, `ConversationComposer.kt`,
`TimelineMessageContent.kt`, `ChatCreationScreens.kt`,
`ConversationScreenTest.kt`, `ChatsScreenTest.kt`, `ui-metrics.md`, and
`message-interactions-and-search.md`.

## WN-ANDROID-0118 — Bubble holds outrank rich-child taps without stealing taps

- Date: 2026-09-01
- Status: Implemented; host Kotlin and instrumentation-test compilation pass.
  Device and visual verification remain unclaimed until explicitly requested.

Every message bubble is one long-press target even when its descendants own
ordinary tap actions. The bubble observes touch in Compose's Initial pointer
pass and waits for the platform long-press timeout without consuming a normal
press or release. Movement beyond platform touch slop cancels recognition so
timeline scrolling and Reply swipe retain priority. Once a stationary hold is
recognized, the bubble performs the long-press haptic, opens the shared message
action overlay, and consumes subsequent pointer changes through release. This
prevents a nested link, mention, reply quote, media tile, file, contact, link
preview, or voice control from firing after the context action appears.
Bubble-attached reaction and timestamp metadata share the same detector;
reaction taps remain reaction-owned while timestamp holds continue to reach
message actions. The transparent width outside the actual message composition
does not regain hit handling.

An ordinary tap remains descendant-owned and therefore keeps the exact
destination behavior: gallery, external link, person profile, file opener,
reply-source navigation, or playback. Selection mode retains its accepted
whole-row toggle, deleted messages expose no hold, and the focused preview does
not recursively reopen itself. Named long-click semantics and policy-owned
custom accessibility actions remain available for keyboard and assistive
technology.

Evidence: `ConversationScreen.kt`, `ConversationScreenTest.kt`,
`message-interactions-and-search.md`, and `feature-inventory.md`. Source:
[Compose gesture propagation](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures).

## WN-ANDROID-0119 — Only the offscreen current day owns a pinned capsule

- Date: 2026-09-01
- Status: Implemented; host unit/build/static checks pass. Device and visual
  verification remain unclaimed until explicitly requested.

Every chronological day boundary remains at its source position as ordinary
centered system text with heading semantics. Inline boundaries do not draw a
pill, card, shadow, or tonal background. The timeline derives the active day
from the top visible item and shows a separate top replacement only when that
day's own inline header has left the viewport. When the matching inline header
becomes visible—including when it reaches the top replacement while scrolling
back—the replacement is removed and the boundary remains in place as text.

The replacement uses the established capsule shape, 12 dp horizontal and 3 dp
vertical label padding, full-emphasis `onSurface` text, and
`surfaceDim` at 82% alpha. Material defines that semantic role as consistently
dimmer than the base surface in both light and dark appearance. It therefore
preserves a clearer boundary when the capsule overlaps an incoming
`surfaceContainerHigh` bubble while keeping background context visible. It is a
passive overlay with no tap target; both representations retain heading
semantics, but the visibility rule avoids duplicating the active heading. This
preserves the pinned iOS product behavior without making every Android list
section a sticky surfaced row.

Evidence: pinned iOS
`WhiteNoisePrototype/Screens/Conversation/ConversationView.swift` at
`0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`, `ConversationScreen.kt`,
`ConversationDayHeaderTest.kt`, `ConversationScreenTest.kt`,
`shared-conversation-core.md`, `ui-metrics.md`, and `feature-inventory.md`.
Android sources: [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3),
[Compose lazy lists](https://developer.android.com/develop/ui/compose/lists), and
[Compose heading semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics#headings).

## WN-ANDROID-0120 — Rich message children share one concentric canvas

- Date: 2026-09-01
- Status: Implemented; host unit/build/lint and instrumentation-test
  compilation pass. Device and visual verification remain unclaimed until
  explicitly requested.

Text-only and deleted bubbles retain the accepted 12 dp horizontal and 8 dp
vertical inset. A nondeleted message containing a reply or attachment instead
uses one 6 dp shell inset on all four sides. Its reply quote, media group, GIF,
link preview, file, contact, and voice card use a 10 dp outline—the 16 dp bubble
radius less the 6 dp inset—so nested silhouettes are concentric. Stacked rich
sections keep 6 dp separation; gallery tiles keep 2 dp separation and are
clipped once as a group. Rich cards own 6 dp internal content padding.

Albums and nonmedia rich content use one 256 dp inner canvas. A lone photo or
video preserves the accepted aspect-derived width, and any caption wraps to
that exact width rather than widening the bubble independently. Mixed text
adds 6 dp horizontal and 2 dp bottom inside the rich canvas, recovering the
ordinary 12 dp horizontal and 8 dp bottom visual text inset. Link artwork fills
the canvas above its text, and file rows use a bare semantic glyph rather than
another nested rounded icon container. Deleted messages render only their
tombstone instead of retaining attachment or reply geometry.

This translates the pinned iOS product composition rather than its SwiftUI
structure. Compose `width` constraints establish the single sibling canvas;
Material `Surface` continues to own semantic colors and the visible child
container while explicit clipping contains custom state layers.

Evidence: pinned iOS `conversation-shared.md`,
`PrototypeMessageBubble.swift`, and `MessageBubbleComponents.swift` at
`0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`;
`ConversationMessageMetrics.kt`, `ConversationScreen.kt`,
`TimelineMessageContent.kt`, `TimelineMessageContentTest.kt`,
`ConversationScreenTest.kt`, `shared-conversation-core.md`, `ui-metrics.md`,
and `feature-inventory.md`. Android sources:
[Compose constraints and modifier order](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers)
and [Material Card containers](https://developer.android.com/develop/ui/compose/components/card).


## WN-ANDROID-0121 — Focused actions restore the light material backdrop and ordinary timestamps

- Date: 2026-09-03
- Status: Implemented from explicit user direction; host gate passes, device acceptance pending.

The focused conversation retains its 24 dp blur and uses an 88%
`surfaceContainerLowest` veil: translucent white in light appearance and
adaptive near-black in dark appearance. The user's same-day refinement lowers
the white opacity slightly so underlying content reads as faint blurred shapes.
Dialog dimming stays disabled. Time,
Sent fill, and Sending progress use the same `outline` role as the ordinary
chat screen; the focused-only `onSurface` override is removed. Failed delivery
retains its semantic error color. Dismissal, reaction/menu placement, metadata
geometry, and the earlier-Android veil fallback remain unchanged.

Evidence: `ConversationScreen.kt`, `message-interactions-and-search.md`, and
`ui-metrics.md`. Unit tests, lint, app assembly, and instrumentation-test APK
compilation pass; no current-build device inspection was performed.

## WN-ANDROID-0122 — Single media fills height and crops only horizontal overflow

- Date: 2026-09-03
- Status: Implemented from explicit user direction; host gate passes, device acceptance pending.

A single photo or video thumbnail uses decoded image dimensions before stored
catalog dimensions. The media height is 256 dp, and width follows the source
ratio up to the 256 dp canvas maximum. Compose `ContentScale.FillHeight`
preserves the complete vertical image extent while a clipped frame removes
only centered horizontal overflow. A narrower parent caps width without
shrinking height. The tiny-source safeguard can reduce both dimensions.
Captions share that frame width. Albums retain their existing tile crops and
GIFs retain their fixed frame.

The pinned iOS baseline uses the same former clamp/crop policy, including
synthetic panorama dimensions. This is an explicit Android presentation
correction following the user's explicit full-height, side-crop rule;
the deterministic fixture records and content remain intact.

Evidence: `SingleMediaLayout`, `ConversationMessageMetrics.kt`,
`TimelineMessageContent.kt`, `TimelineMessageContentTest`, and
`ConversationFixtureParityTest`. Unit tests, lint, app assembly, and
instrumentation-test APK compilation pass; device visual acceptance is pending.
Source: [Compose image scaling](https://developer.android.com/develop/ui/compose/graphics/images/customize).

## WN-ANDROID-0123 — Embedded video playback with Google's Material player controls

- Date: 2026-09-03
- Status: Implemented from explicit user direction; host gate passes, device acceptance pending.

The sent-media viewer replaces its external Open Video action with Media3
ExoPlayer and `media3-ui-compose-material3` 1.11.0, the current stable release
verified in Google's release notes. Google's `Player`, `PlayerDefaults`, and
control components own video fitting, play/pause/replay, backward/forward
seeking, the progress slider, elapsed/total time, mute, playback speed, and
error/retry state. The surrounding gallery still owns paging and its existing
Share, Forward, Save, and Go to Message actions. The slider and speed control
receive explicit accessible names. In compact-height windows, play/seek move
into the bottom control row so they cannot overlap the tracker. Measured
toolbar clearance and safe horizontal insets protect the controls.

The user's subsequent playback-track direction removes the fixed end dot
across voice, Read Aloud, and video. Voice/text progress uses Material's empty
stop-indicator callback. Video connects Media3's progress state to a Material
Slider with the stop indicator disabled, preserving the seek thumb and native
interaction because Media3's slider wrapper does not expose its track slot.

Only a settled foreground video owns a player. Entry is paused; paging away,
closing, or leaving the foreground releases playback. Position, volume, and
speed survive a return within the viewer, with playback paused. Media3 owns
audio focus and headphone-disconnection handling; `keepScreenOn` applies only
while playing. Sources stay limited to granted device content URIs and the
existing bundled clip. The inherited Media3 wake-lock permission is removed,
as no background playback or wake lock is used. No network permission, service,
or durable media state is introduced. Existing minimum API 23 and Material
version pins remain valid.

This explicitly supersedes the earlier sent-viewer video handoff exception.
Signal's media-preview control source was inspected as behavioral reference;
its bottom seek/time controls and omission of previous/next track buttons
support this composition. No Signal playback implementation is imported.

Evidence: `MediaViewerVideo.kt`, `MediaViewer.kt`, the version catalog,
`MediaViewerVideoTest`, and the permission regression in
`AppResourceIntegrityTest`. `testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest` passes with 174 unit tests, no lint errors, and both
APKs; existing unrelated lint warnings remain. The merged manifest adds no
media permission. Current-build playback and visual acceptance
require a user-requested device run; instrumentation compilation alone does
not establish them.

Sources: [Media3 Compose UI](https://developer.android.com/media/media3/ui/compose),
[stable releases](https://developer.android.com/jetpack/androidx/releases/media3),
[ExoPlayer lifecycle](https://developer.android.com/media/media3/exoplayer/hello-world),
and [Signal media-preview controls](https://github.com/signalapp/Signal-Android/blob/879651dc47a7b18b67e7aea52a25197875024680/app/src/main/java/org/thoughtcrime/securesms/mediapreview/MediaPreviewPlayerControlView.kt).

## WN-ANDROID-0124 — Gallery clipping and downward dismissal

- Date: 2026-09-03
- Status: Implemented from explicit user direction; host gate passes, device reproduction pending.

Both sent and composer galleries now use a shared, downward-only dismissal
gesture. The first deliberate direction decides ownership: horizontal paging
and already-consumed child controls win, and multi-touch cancels dismissal.
Zoomed photos keep vertical panning. A short pull springs back; a pull past
the documented travel/velocity threshold closes the viewer using Material's
fast spatial motion. In the composer this is cancellation, so only Done applies
staged exclusions. Close and Android Back keep their existing behavior.
This is the user's explicit gallery-gesture exception to the general policy
against importing platform-specific gestures. The pinned iOS composer provides
the separate cancel/confirm semantics; the new gesture is directly authorized
by the user rather than inferred from newer upstream source.

The Android source investigation found default horizontal-pager clipping leaves
cross-axis drawing space for effects; its stretch renderer uses an expanded
rendering layer. Gallery pagers, pages, and the draft thumbnail rail now clip
strictly, with edge stretching disabled. A stationary opaque backdrop remains
behind the translated content, preventing drawing outside the media viewport
from exposing adjacent content. Inactive video pages also stop creating
Media3 rendering surfaces: they render only a poster. Active video uses the
recommended SurfaceView on API 24+ and TextureView on API 23 for synchronized
drag animation. The exact reported rendering artifact still requires a
user-authorized current-build device reproduction; source inspection and host
tests do not establish visual acceptance.

Evidence: `GalleryDismissGesture.kt`, both viewer integrations,
`MediaViewerVideo.kt`, `GalleryDismissPolicyTest`, and compiled gesture cases
in `ConversationScreenTest` / `MediaViewerVideoTest`. The host gate passes with
179 unit tests, no lint errors, app assembly, and instrumentation-test APK
compilation; existing unrelated lint warnings remain. No new dependency,
permission, or product-state mutation is introduced.

Sources: [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures),
[drag/swipe handling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling),
[pager](https://developer.android.com/develop/ui/compose/layouts/pager), and
[Media3 surfaces](https://developer.android.com/media/media3/ui/surface).
The resolved Foundation 1.12.0 `ClipScrollableContainer` and Android overscroll
source jars provide the clipping/expanded-layer evidence.

## WN-ANDROID-0125 — Message photo detail and opaque gallery chrome

- Date: 2026-09-03
- Status: Implemented from user direction; host gate passes, device acceptance pending.

Picked/camera message photos no longer use the avatar's 512 px import policy.
`ConversationImageProcessor` configures the existing orientation-aware decoder
for a 4096 px long edge, lossless PNG/alpha, and JPEG quality 95 otherwise.
Avatar callers retain 512 px/JPEG 88. Encoded image bytes remain in the existing
in-memory attachment model through sending; no new storage or permission is
introduced. `DeviceMediaImage` samples decoded bitmaps to their displayed
bounds off the main thread, so thumbnails do not allocate full photo bitmaps.
Already-reduced imports need reselection; missing source pixels cannot be
recovered from their stored bytes.

Both sent-viewer chrome surfaces use the opaque gallery background instead of
94% alpha. Tall images can no longer show through the title and action bars.
The selected image's own content is preserved, including embedded UI when the
person selects a screenshot; no screen capture is used in the import path.

Evidence: 183 unit tests, lint without errors, and both APK builds pass.
`MediaImageSamplingTest` exercises display-size sampling; compiled
`ConversationImageImportTest` covers exact screenshot pixels, independent
avatar sizing, orientation/large-image bounds, and import failure. No device
execution or current-build visual acceptance is claimed.

Sources: [Android bitmap decoding](https://developer.android.com/topic/performance/graphics/load-bitmap)
and [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker).

## WN-ANDROID-0126 — Read Aloud for authored text in both directions

- Date: 2026-09-03
- Status: Implemented from user direction; host gate passes, device speech verification pending.

Messages with authored text and no voice attachment offer Read Aloud regardless
of sender, including replies and media/file captions. The action follows Copy
and changes to Stop Reading while the existing speech/progress controller is
active. Empty/deleted messages remain ineligible; received voice/transcript
behavior is preserved. This explicitly extends the pinned iOS recipient-only
eligibility under the user's current request.

The manifest now includes Android's TTS service query, which was missing and
could prevent installed-engine discovery on Android 11+. Start still requires
successful engine/language initialization; an active Stop Reading command
remains available independently. No permission or dependency is added.

Evidence: 186 unit tests, lint without errors, app assembly, and
instrumentation-test APK compilation pass. Model tests cover direction,
captions/replies, exclusions, and readiness/stop transitions. The sent-text
start/progress/stop UI case compiles. The packaged manifest contains the query
and continues to omit Internet/microphone permissions. Device execution and
audible speech verification remain pending.

Source: [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech).

## WN-ANDROID-0127 — Google-style information screens with iOS content order

- Date: 2026-09-03
- Status: Implemented from user reference; host gate passes, device acceptance pending.

Chat Info and Group Info use the supplied Google Messages presentation:
neutral gray canvas, back-only Material app bar, centered identity, wide
tonal pill actions, and white-equivalent native segmented list rows. The
accessible pane title remains Chat Info/Group Info. Avatar sizing follows
the established Person Profile/Share & Connect 32% pane clamp (104–152 dp),
not the reference screenshot's avatar. Direct identity reuses the verified
address and full public-key copy capsule; group description precedes count.

The explicitly authorized read-only iOS comparison at `4c25393f0eb6` supplies
product order. Direct chats place Shared in Chat before Chat Actions with
Relays, Developer Tools, Archive/Unarchive and active-only Leave Chat. Groups
place Shared in Chat, Advanced, Members, active-admin Edit Group/Add People,
then Archive/Unarchive and active-only Leave Group. Developer Tools opens
the existing Conversation Debug destination with its original enablement
gate. Existing search handoff, membership consequences and role rules remain.

The current Material ListItem API owns row sizing, positional shapes and
interaction states; groups retain 16 dp margins and 2 dp gaps, headings follow
the 32 dp content line, and member avatars stay 48 dp. Quick actions retain
56 dp height while filling equal-width slots. No new dependency is needed.

Evidence: 186 unit tests and lint without errors; app and instrumentation-test
APKs assemble. Compiled UI regressions cover full-key clipboard data, debug
navigation callbacks, group section order, self/member semantics, and ended
groups with dark 200% RTL text. No device execution or visual acceptance is
claimed for this build.

Sources: [Android icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button),
[lists and grids](https://developer.android.com/develop/ui/compose/lists), and
the pinned Material 3 source archive's `ListItemDefaults.segmentedShapes`.

## WN-ANDROID-0128 — Java time compatibility for civil-date filtering

2026-09-04 implementation decision under the authorized B07 global-search batch.
Keep minimum SDK 23 and use Android core-library desugaring for Java time, so
civil-day boundaries, DST and Material picker date conversion use the same
calendar model on all supported versions. Enable `isCoreLibraryDesugaringEnabled`
and pin Google's `com.android.tools:desugar_jdk_libs:2.1.5` in the version catalog.
Google Maven metadata and the official changelog both identified 2.1.5 as the
current stable release when checked. This is Android toolchain compatibility;
no backend, permission or product service is added. Calendar filters use the
documented fixed fixture clock until production reconnects real timestamps.

Sources: [Android Java API desugaring](https://developer.android.com/studio/write/java8-support),
[Google changelog](https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md),
[Google Maven metadata](https://dl.google.com/dl/android/maven2/com/android/tools/desugar_jdk_libs/maven-metadata.xml).
The final clean host gate passed 296 unit tests, both APKs and lint without
errors after the compatibility fix. Device execution remains unrequested.

## WN-ANDROID-0129 — Native passage selection retains authored source spans

2026-09-04 implementation decision under the authorized B09 batch. Use Compose
Foundation's `SelectionState` for full-reader selection and native handles,
keyboard, toolbar and Back behavior. Each rendered UTF-16 unit carries an
annotation pointing to its unchanged authored span. Selected fragments retain
annotations, so repeated words and Markdown/entity presentation resolve without
searching for an ambiguous text occurrence. A source revision clears selection;
speech stores the selected span and rejects stale utterance callbacks.

The local parser renders the named production document shapes with Material
text/link primitives and literal fallback; production integration should supply
its existing AST rather than adopt a second runtime parser. Copy retains the
established readable text; Copy Markdown exposes the complete authored source.
Encoded Nostr identity/event reference resolution remains tracked in B30. No
third-party runtime parser, WebView, network, service or permission is added.

Sources checked 2026-09-04: [Compose selection and links](https://developer.android.com/develop/ui/compose/text/user-interactions),
[SelectionState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/selection/SelectionState),
and the resolved Foundation 1.12.0 source. Nine edit-state, eleven document and
four source-selection unit tests pass; ten new UI cases compile. Device and
visual acceptance remain pending.

## WN-ANDROID-0130 — Source-aligned message batches retain explicit operation ownership

2026-09-04 implementation decision under authorized B10 production parity.
Production ForwardSelection/ForwardMessagePicker at pinned master apply no
message/recipient count cap. Replace the older prototype 32-message/five-chat
caps with explicit source/target eligibility; never silently truncate folder
selection. This supersedes the earlier count limits without changing the
approved picker rows, search, sheet geometry or media-caption capsule.

Freeze each deletion item's everyone/local operation after an explicit count
breakdown. Revalidate role/membership at execution and retry only failures;
permission loss must not silently convert an everyone delete into local hiding.
A tombstone can open local-removal confirmation through hold or accessibility,
superseding the earlier no-hold rule for this one action.

Forwarding belongs to app state outside the source route. Native status/details
surfaces expose per-target progress, partial counts, explicit cancellation and
retry. Preserve sent prefixes and completed targets; cancel only before publishing.
Match production's three transient retries at 1/2/4 seconds, leaving preparation
timeouts for manual retry and source/expiry/session failures terminal. Native
lifecycle pausing and profile/session/request guards prevent stale completions.
Destination-profile selection is explicit and never switches the active profile;
wiping profile data clears retained operation payloads.

Sources: pinned production MessageBatchDeleteOperations.kt, ForwardSelection.kt,
MessageForwarding.kt and AppState.kt; current [Material dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
[progress indicators](https://developer.android.com/develop/ui/compose/components/progress)
and [checkbox semantics](https://developer.android.com/develop/ui/compose/components/checkbox).
361 unit tests and both APKs pass; nine new UI cases compile. Runtime transport,
background services and current-build device/visual acceptance remain outside
this host-verified implementation evidence.

## WN-ANDROID-0131 — Draft media keeps reversible quality and explicit external handoff

- Date: 2026-09-04
- Status: Implemented from the authorized production parity B11; host verified,
  device and visual acceptance pending.

Retain the accepted 4096px/JPEG95 policy as High. Add Low, Standard and Original
per draft, preserve source images in memory through quality changes, and remove
those sources from sent/forwarded payloads. Original strips supported JPEG/PNG
metadata losslessly; orientation/color/unsupported formats use a disclosed safe
re-encode. Output sizes are measured bytes, and videos/files are not described as
metadata-stripped photos. GIFs preserve animation blocks while dropping comments
and unrelated application metadata. A neutral animated fixture replaces the
previous static GIF-labelled presentation, with sampled static/error fallback.

Recent media uses deterministic none/selected/full/unavailable states; Gallery
remains the standard Photo Picker. Q06 does not authorize real library grants.
White Noise person and Device contact are distinct actions. The latter launches
the permissionless phone-row picker, reads only that granted row, previews
available fields and sends selected-field vCard/text. It never queries the email
table or derives White Noise identity from a phone number.

Q07 retains explicit Android document destinations. Message attachment actions
report per-item save/cancel/failure and confirm partial sharing. Unique temporary
files carry actual names/MIME types and Android URI grants; opening sharing does
not claim delivery. Transfer state stays profile/chat/message/attachment-owned,
revision-guarded and driven only in the app foreground. It survives ordinary app
navigation and invalidates old events on profile/session changes. Real transport,
cache and media permission integrations remain production seams.

Evidence: `docs/screens/composer-attachment-actions.md`, `AttachmentModelsTest`,
`AttachmentStateTest` and compiled `AttachmentAcquisitionTest`; 381 passing host
unit tests, zero lint errors and both APKs. Current official sources are linked
in the selected brief. No device or visual verification is claimed.

## WN-ANDROID-0132 — Draft photo edits stay reversible and frame-owned

- Date: 2026-09-04
- Status: Implemented under authorized B12 production parity; host verified,
  device and visual acceptance pending.

Retain each original image, normalized crop/rotation/ink recipe and per-frame
quality in the in-memory draft. A save replaces only the selected frame after
profile/chat/attachment/revision/source validation; caption, reply, siblings and
review exclusion remain intact. Sending or forwarding drops originals and edit
metadata so only flattened rendered content leaves the draft.

Preserve the user's explicit B11 Low/Standard/High choice. Original with edits
uses a disclosed High-quality render, while an unchanged Original may retain
sanitized encoded content. A whole-draft quality change replays every recipe
from its original and replaces individual frame overrides. The composer reports
mixed quality when applicable. Reset returns to the editor's opening recipe and
quality; closing dirty work requires discard confirmation.

Use a bounded native canvas for crop/rotation/ink because Material has no complete
photo editor component. Native controls, scrollable tool rows, coordinate-slider
alternatives and live feedback own the surrounding interaction/accessibility.
The five production ink colors are literal photo content; they do not change
monochrome semantic chrome or resolve B26/Q01 customization. Erasing applies to
a separate annotation layer, preserving the source photo.

Evidence and current official guidance: [draft photo editor brief](screens/draft-photo-editor.md#implementation-evidence).
The clean gate passes 399 unit tests, both APKs and zero lint errors; nine new
UI/rendering cases compile. No device/visual verification is claimed.

## WN-ANDROID-0133 — Attachment reading and shared content preserve source identity

- Date: 2026-09-04
- Status: Implemented under authorized B13; host verified, device/visual acceptance pending.

Read text only after strict bounded decoding; distinguish unavailable, encoding,
binary and size failures. Render budgets preserve full original Copy and expose
truncation/external fallback. Native selection and engine-sized speech chunks
reuse the established document interaction. The reader's metadata scrolls with
its body; native app-bar overflow contains external Open/Save.

Keep Chat Info disclosure rows and add Voice. Photos & Videos contains All,
Images and Videos filters. Newest-first month grouping does not change exact
message/album-frame identity or chronological viewer order. Unknown audio remains
unavailable; a single foreground player owns actual local bytes and invalidates
old callbacks/source replacements. Both viewers use fresh source projections.

Q06 covers app-owned package validity/distribution/permission/installer outcomes
only. Archive-shape validation is not signature verification or installation.
Save file/Close is the local fallback; even provider-refined package MIME cannot
dispatch installation. Existing system surfaces retain platform appearance.

Evidence and official guidance: [attachment reading brief](screens/text-attachments-and-shared-content.md#implementation-evidence).
The clean gate passes 414 unit tests, both APKs and zero lint errors. Thirteen
new UI/platform cases compile; no device playback/speech/visual result is claimed.

## WN-ANDROID-0134 — Location shares are fixed points with explicit external viewing

- Date: 2026-09-04
- Status: Implemented under authorized B14; host verified, device/visual acceptance pending.

Select a point with native coordinate fields and review it before sending. Empty
input is not zero/zero; finite bounds and dot-decimal wire formatting are explicit.
Current-location fixtures require an enabled developer profile and a one-shot
selection. Q06 keeps GPS, real location grants, map tiles/geocoding and tracking
off. Manual fields are the accessible, bounded replacement for a networked picker.

Preserve the production location-only text wire format and strict whole-body
legacy parsing. Render coordinates without a map request. Reuse the permissionless
ACTION_VIEW boundary only after Open in Maps; missing/access failures offer
Copy/Retry. Prose around links and ordinary media must stay visible.

A location send preserves the ordinary draft, includes and consumes only its
unchanged valid reply, rejects stale/profile/eligibility changes, and reveals the
accepted source message through history targeting. Failure retains the point and
draft. Coordinate synchronization must not silently discard reported accuracy.

Evidence and current official sources: [location sharing brief](screens/location-sharing.md#implementation-evidence).
The clean gate passes 432 unit tests, both APKs and zero lint errors. Thirteen new
UI/platform cases compile; no native Maps execution or visual result is claimed.

## WN-ANDROID-0135 — One foreground speech queue preserves source ownership

- Date: 2026-09-04
- Status: Implemented under authorized B15; host verified, device/visual acceptance pending.

Read Aloud now belongs to the app shell and survives ordinary navigation. The
existing message and file readers share its native engine; file playback still
ends when its reader closes. A session owns immutable profile/chat/source IDs,
a bounded prepared history window and generation-checked native callbacks.
Pause freezes progress; resume restarts the sentence because Android TTS stop
clears queued utterances. Word timing is optional, with engine-sized chunks as
the fallback. Native sentence choice and selection-based Read from here use
exact authored offsets rather than matching repeated visible strings.

Keep ordinary bubbles free of permanent speech commands. Focused readers and
the shared transport own controls, while native source-aware spans mark the
current passage. Manual scroll suspends following and Resume following restores
it. Speech-driven scrolling never acknowledges unseen messages. Return validates
the current source immediately before targeted navigation. Following F07's
explicit local contract, manual profile changes, sign-out and background/lock
clear speech and return ownership; do not silently switch back to an old profile.
No service, new permission or external audio/network capability is introduced.

The [selected brief](screens/read-aloud-transport.md#implementation-evidence)
records official Android sources and exact evidence. The host gate passes 465
unit tests, zero lint errors and both APKs; 15 new UI/platform cases compile only.
