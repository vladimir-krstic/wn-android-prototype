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
- Status: Approved

The repository contains a read-only snapshot of the iOS docs, product source,
tests, UI tests, Xcode project metadata, and reusable resources. It was taken
from commit `58785a4724f33e23135c4dd3f98f231fca6a809d` plus the source
working-tree edits documented in `references/ios-prototype.md`.

Future implementation uses the local snapshot rather than depending on a
sibling checkout. Refreshing the snapshot is a separate explicit task because
it can change parity requirements.

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
- Status: Approved by explicit user direction and implementation

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
- Status: Approved by explicit user direction and implementation

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
