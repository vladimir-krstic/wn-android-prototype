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
- Status: Approved by implementation and static verification

Messages use Compose `combinedClickable` with haptic feedback for Android's
standard deliberate-hold interaction. Material modal sheets own contextual
commands, reactions, emoji configuration, and forwarding; every gesture-owned
command is duplicated as a named semantics custom action. This keeps TalkBack,
keyboard, mouse, and touch access aligned with current Android guidance without
copying the iOS/Signal custom overlay implementation.

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
- Status: Approved by explicit user direction and implementation

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
