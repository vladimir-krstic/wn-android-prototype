# Android port handoff

2026-09-04: production parity B01–B02 are implemented and host-verified.
The active goal covers B01–B32 with one commit per completed batch
(`B01: <title>`). `docs/audits/production-android-parity/implementation-plan.md`
tracks the sequence. The selected access/recovery and keys/profile-exit briefs
record evidence. Continue with B03. Q05 is resolved by the approved checked wipe
default. Developer Tools now provides access/startup/sign-out outcomes and local
key availability. Latest clean gate: 216 unit tests, zero lint errors, both APKs;
14 pre-existing warnings. No runtime backend, new permission or device validation.

The native Android port is functionally complete inside the approved offline,
deterministic boundary. This handoff describes the shape that should remain
stable while visual polish proceeds one screen or bounded flow at a time.

## Architecture map

```text
MainActivity
└── WhiteNoiseApp
    ├── WhiteNoiseTheme (profile-owned System / Light / Dark)
    ├── AppViewModel
    │   └── AppUiState
    │       └── Profile[]
    │           ├── People[]
    │           ├── Chat[] + timeline/draft/relay state
    │           ├── ProfileSettings + profile relays
    │           └── DeveloperToolsState + sanitized artifacts
    └── WhiteNoiseNavHost
        ├── Onboarding and profile routes
        ├── Chats and creation routes
        ├── Shared conversation and information routes
        └── Settings, support, developer, and destructive routes
```

There is one application module, one launcher activity, one authoritative
in-memory state holder, and one typed navigation graph. UI surfaces receive
immutable model values plus mutation callbacks. Projections—chat rows,
conversation clusters, search results, shared content, availability, and
diagnostics—derive from the authoritative state rather than becoming a second
store.

## Android system boundaries

| Capability | Android-native boundary | App permission |
| --- | --- | --- |
| Back and destination state | Navigation Compose and system/predictive Back | None |
| Private-key QR scan | Near-full Material sheet with CameraX and bundled on-device ML Kit | Camera, requested just in time |
| Share & Connect QR scan | Shared near-full CameraX/ML Kit Material scanner sheet | Camera, requested just in time |
| QR generation | Local ZXing matrix rendered in Compose | None |
| Photos and videos | Android Photo Picker | None |
| Documents | Storage Access Framework | None |
| Camera attachment | External `TakePicture` contract and non-exported FileProvider | None |
| Share/copy | Android Sharesheet and clipboard | None |
| Read aloud | Android TextToSpeech with lifecycle shutdown | None |
| Notification access | Android 13+ runtime prompt and app-notification settings handoff | Notifications, requested only from the explicit Settings action |
| Security controls | Explicit Android security-settings intent | None |
| Recents privacy | Activity `FLAG_SECURE` derived from active profile | None |

The merged application declares camera permission for the selected Private Key
or Share & Connect QR scanner and `POST_NOTIFICATIONS` for the explicit Android
notification-access gate. It intentionally declares no network, storage,
microphone, or location permission. The notification prototype requests access
but does not create or deliver notifications. There is no backend, transport,
durable persistence, real authentication, cryptography, payment, telemetry
upload, microphone capture, or speech recognition.

## Verification boundary

The complete static gate is:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Unit tests execute on the host. Compose instrumentation tests are compiled and
packaged but are not run unless the user's current request explicitly asks for
device/emulator testing. Prior inspection requests and an attached phone are
not standing authorization. Do not use `adb`, install or launch the app,
interact with a device, or capture device screenshots outside that explicit
request. Static
coverage includes model consequences, fixture integrity, navigation values,
screen entry states, accessibility actions, adaptive width, 200% font scale,
RTL composition, and packaged resource identities.

The explicitly authorized 2026-08-31 hardening pass ran the complete packaged
suite on a physical Pixel 8a with Android 17: all 168 instrumentation tests
passed with zero failures, errors, or skips. The final host gate also passes
139 unit tests, both APK assemblies, and lint with zero errors plus six
deliberately retained dependency-version warnings. Direct device inspection
covered selected high-impact conversation/composer/search/info/contact/voice,
Chats/creation, Settings/privacy, dark, 200%-type, RTL, and 610/838 dp-wide
states. This is engineering evidence, not a replacement for user visual
acceptance or a complete hands-on TalkBack pass.

## Visual-polish implementation authority

`docs/visual-polish.md` records the approved quiet Material 3 Expressive
direction, visual-system contract, component migration matrix, cross-app state
contract, foundation-first rollout, and acceptance gates. That plan supersedes
the earlier screen order while preserving the one-screen-or-bounded-flow
implementation workflow.

The first implementation batch established the shared theme and primitives,
then applied them to the paired Chats and Settings roots. The user accepted
that cleaner direction on 2026-08-21. A second bounded batch applies the same
system to Welcome, Sign In, Sign Up, Add Profile, and avatar-source states,
with bounded forms, tonal alternate input, task-appropriate pinned or inline
actions, compact progress, and shared empty/selected treatment. Sign Up's
Pixel 8a polish pass moves completion into the IME-aware form scroll surface.
Static coverage validates these presentations without changing their state or
navigation contracts. Full flow device acceptance remains pending.

A third bounded batch extends that system through New Message, Person Profile,
New Group selection, and Set Up Group. Persistent people search remains visible
where it is the primary task; native chips and toggleable rows own selection;
tonal label-above forms, grouped member review, photo feedback, and pinned
actions own setup; and the existing Relays destination now provides direct
recovery when new messaging is unavailable.

A fourth bounded batch recomposes the shared conversation shell without
changing its state model: a small Material identity app bar replaces the
crowded centered treatment; the transcript and composer share an adaptive
measure; sticky dates, ordinary events, and support notices have distinct
tonal hierarchy; message clusters use the shared shape/spacing system; and
invitation, ended, blocked, failed-send, and missing-relay states expose clear
semantic recovery.

A fifth bounded batch recomposes composer and rich-content presentation while
retaining the authoritative draft and timeline contracts. The composer is one
tonal task region with stable Material add/input/send/record actions; draft
attachments, links, replies, mentions, loading, and errors share the system;
attachment sheets and rich cards use semantic icons; and draft/read-only media
viewers use safe-area app bars and chronological pagers. Voice review and
received speech use code-native waveforms, standard playback controls,
transcript provenance, and accessible progress. Photo Picker, Files, external
camera, document handoff, and TextToSpeech remain Android-owned runtime
boundaries. Video handoff was subsequently replaced with foreground Media3
playback inside the gallery on 2026-09-03 (WN-ANDROID-0123). At that point in
the rollout, message actions/search and Chat Info
remained later polish batches.

A sixth bounded batch recomposes message interactions and in-conversation
search without changing `MessageActionPolicy`, `ReactionCatalog`,
`ConversationSearch`, or any mutation callback. Long-press opens a Material
sheet with focused message context, quick reactions, and semantic command
icons; emoji/configuration and forwarding sheets use adaptive targets, native
selection, visible limits, and clear task hierarchy; selection/search bars use
compact named actions and live state; and search adds focus, Clear, localized
position, explicit current-result containment, and inline text highlighting.
Message Details uses the same child-destination and tonal grouping system.
Clipboard and IME behavior remain Android-owned runtime boundaries.

A seventh bounded batch recomposes Chat and Group Info without changing its
typed routes or model gates. Identity headers stay open; quick actions use
quiet tonal icon controls; shared content, members, administration, and chat
actions use deliberate grouped surfaces; selection uses native radio/checkbox
semantics; Edit Group uses the shared form and pinned-action system; and Chat
Relays exposes named removal, empty recovery, and captured-default restore.
Member profiles separate relationship actions from role/removal actions while
sole-admin, ended-membership, history, and relay consequences remain model
owned. Photo Picker execution and visual/device acceptance remain pending.

An eighth bounded batch brings the accepted system to consumer Settings
details without changing the established destination graph or profile-owned
state. Share & Connect, Profile, Keys, Notifications, Appearance, Privacy &
Security, Data Usage, profile Relays, Support, and Donate now share tonal
groups, native radio/switch semantics, label-above fields, concise value and
action rows, explanatory disabled/recovery states, and pinned primary tasks
where one task dominates. Profile and donation QR codes use a stable
black-on-white technical surface; Android-owned scanner, Photo Picker, Files,
Sharesheet, document creator, notification settings, and device-security
settings remain unchanged.

A ninth bounded batch completes the app-owned rollout through Developer Tools,
Diagnostics, Audit Logs, Key Packages, Conversation Debug, Sign Out, Manage
Profiles, Remove Profile, and Erase App Data. Technical state stays precise
inside deliberate tonal groups and one adaptive console; warnings use text and
Material symbols; unavailable and empty states remain actionable. Large
destructive tasks stay native full-height sheets with close disabled during
named progress, label-above exact confirmation, and one pinned error-role
action. The active-profile and stable three-word gates, artifact preservation,
sanitized-copy boundary, and exit routing remain model-owned and unchanged.
All app-owned visual batches are now statically implemented; device visual
acceptance remains pending.

A cross-app form-system pass replaces the resting outline on every ordinary
app-owned text, secure, and multiline field with a stronger tonal container.
All 19 form fields keep the approved 28 dp corners, align label, input or
leading icon artwork, and supporting/error copy to one 16 dp directional
content line, and expose 2 dp full-shape focus/error rings. Search bars and
message composers remain their specialized Material patterns. The clean gate
passes with 94 unit tests, 74 compiled Compose instrumentation tests, zero lint
issues, and both APKs; device visual acceptance remains pending.

A focused Settings-overview refinement follows the user's hands-on review.
Every destination now uses a consistent rounded Material Symbol, concise
one-line label, and disclosure chevron inside the existing tonal groups.
Descriptions that merely repeated the destination are removed; Appearance
keeps its active value in the trailing slot, and Profile keeps a supporting
reason only when its relay dependency disables editing. Navigation, account
switching, destination order, state ownership, and destructive behavior remain
unchanged. Shared Settings group headings use a 32 dp directional inset so
they align with leading icons on the overview and row text on icon-free detail
lists instead of sitting on the outer tonal-container edge.

## Known polish and device backlog

Latest approved batch (2026-08-26): Chats uses a title-free default scope,
icon-only native New Message/recovery FAB, aligned 40/52 dp avatars, exclusive
status indicators and all non-archived membership states. Native long-press
DropdownMenus replace swipes and the chat-actions sheet, using Signal
`441ba42` as interaction evidence only. Read/archive changes have field-level
Undo; TalkBack exposes the same eligible commands. Ordinary headers use pinned
Material behavior tied to actual scroll; Chats' bottom inset is content
padding. New profile-owned DiagnosticsState supplies the first-login
content-height sheet and Privacy & Security → Diagnostics & Improvements.
Prompt timing waits for RESUMED Chats; actual dismissal, not rotation,
records seen state. Developer Tools does not own or disable consent. Ordinary
sheets now share surfaceContainerLow, transparent ordinary rows, a wrapping
titleLarge/24 dp header and 8 dp body gap with no extra handle spacer. Both
mute entry points use the shared immediate-choice Material dialog.

Scoped iOS evidence is `4c25393`, without changing the global baseline or
conversation fixture timestamps/content. Static gate: 117 unit tests, 121
compiled UI tests, no lint issues, both APKs. No device execution or screenshots
were performed for this batch. Briefs, metrics, source map and decisions
0035–0039 record the implementation; 0038 retires the old swipe exception.

- Repeat the complete packaged suite after future product changes; the
  2026-08-31 Pixel 8a / Android 17 baseline is 168 of 168 passing.
- Complete user-led visual acceptance and remaining hands-on landscape,
  three-button navigation, display-scale, cutout, and motion checks; automated
  and selected direct inspection already cover compact/expanded windows, IME,
  gesture navigation, 200% type, dark theme, RTL, and wider windows.
- Walk every flow with TalkBack and keyboard/D-pad focus; confirm switch and
  custom-action announcement quality.
- Exercise Photo Picker, Files, external camera, Sharesheet, clipboard,
  document viewer, TextToSpeech, settings intents, and Code Scanner on real
  system surfaces. Verify the embedded video player's play/pause, scrubbing,
  replay, local URI failures, audio focus, page/lifecycle release, and compact
  landscape controls when the user requests a device run.
- Capture Android screenshots and compare hierarchy, density, typography,
  rhythm, shape, state color, motion, and empty/error/recovery treatment one
  selected flow at a time.
- Treat any visual changes as presentation refinements unless the user
  explicitly changes the accepted product behavior or offline boundary.
