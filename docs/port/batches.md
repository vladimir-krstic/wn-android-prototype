# Android port batches

This is the execution order for the persistent full-port goal. Each batch must
leave the application buildable, keep prior behavior intact, and update the
parity ledger with concrete evidence.

Visual acceptance is intentionally separate. During these batches, implement
complete Android-native behavior and static quality; later, inspect and polish
one screen or flow at a time with the user.

## Working cadence

For each batch:

1. Read the relevant pinned iOS briefs, implementation, tests, and resources
   through `source-map.md`.
2. Create or update the bounded Android brief in `docs/screens/`.
3. Verify material decisions against current official Android sources.
4. Implement the smallest complete state, navigation, UI, and capability
   boundary for the batch.
5. Add tests for durable logic, navigation, accessibility, and regression
   coverage appropriate to the batch.
6. Run build, lint, and relevant tests.
7. Update `feature-inventory.md`, this file, and the root README with factual
   evidence and remaining gaps.
8. Continue to the next batch without waiting for a routine prompt.

Do not use `adb`, run connected instrumentation, launch an emulator, install or
launch the app, interact with a device, capture device screenshots, or claim
visual verification during this implementation sequence. Those actions occur
only when the user's current request explicitly asks for device/emulator
testing or visual/hands-on inspection; an earlier request is not standing
authorization for a later polish iteration.

## Batch 0 — Native project foundation

Status: **Complete**

Deliver:

- a minimal current Android application using Kotlin, Compose, Material 3,
  Gradle Kotlin DSL, and a version catalog;
- one activity, typed Navigation Compose shell, edge-to-edge behavior,
  predictive Back compatibility, and adaptive root layout foundations;
- monochrome light/dark semantic color schemes and typography based on
  Android defaults;
- app-owned string and icon conventions, preview-safe fixture entry, and
  deterministic app state ownership;
- unit, Compose UI, lint, and build baselines with exact commands in README;
- no speculative feature modules, persistence, network, or dependency
  injection.

Exit gate: debug build and unit tests pass; the shell has an accessible
placeholder destination that later batches replace.

Evidence: one app module and launcher activity, typed Navigation Compose
destination, semantic monochrome themes, edge-to-edge safe insets, unit test,
compiled Compose instrumentation test, clean debug APK, and zero-issue lint.
Verified with the root README's complete static batch gate on 2026-08-15.

## Batch 1 — Onboarding and profile foundation

Status: **Complete**

Deliver:

- Welcome, Sign In, private-key QR entry contract, Sign Up, avatar choice, and
  profile creation;
- first-profile and Add Profile routing;
- profile editing primitives, verified-address state, active-profile state,
  and profile switching foundation;
- deterministic validation, progress, cancellation, Back, and capability
  states;
- safe handling that keeps private keys out of logs and accessibility output.

Exit gate: both onboarding paths reach the signed-in shell; multiple profiles
can be created and switched in memory; relevant logic/UI tests pass.

Evidence: initial and Add Profile routes, Material secure-key entry, fixed
prototype Paste fixture, permissionless Google Code Scanner with recovery,
Photo Picker and document picker, 512-pixel EXIF-aware image preparation,
21 bundled web-image choices with Search/URL modes, deterministic canonical
profile state, and Material profile switching are implemented. Fifteen unit
tests pass, three Compose navigation tests compile into the instrumentation
APK, lint reports no issues, the debug APK exposes only the launcher activity,
and the merged manifest contains no network, camera, storage, or other
sensitive permission. Device/UI execution remains for the user-directed
polish pass under the repository rule against unrequested emulator use.

## Batch 2 — Chats hub and chat creation

Status: **Complete**

Deliver:

- empty and populated Chats, row projections, unread/delivery/attachment/date
  summaries, pinning, archive/left states, search, and list actions;
- profile-switcher entry and active-profile isolation;
- People, Person Profile entry, direct-chat deduplication, group selection and
  setup, creator-admin state, and copied default chat relays;
- compact and expanded list behavior with stable lazy-list identity.

Exit gate: deterministic profiles own independent chat lists; direct and group
creation update authoritative state and list projections; tests pass.

Evidence: Marmota owns the accepted 77-row fixture (72 non-archived and five
archived), while added identities start empty against the same People
directory. Material tabs, search, an Android floating action button, lazy
rows, profile switching with aggregate unread badges, action sheets, mute and
destructive dialogs, direct-chat deduplication, and photo-enabled group setup
all derive from immutable profile-owned state. Twenty-seven unit tests pass,
five Compose tests compile into the instrumentation APK, lint reports no
issues, and both APK variants build. A production-shaped conversation entry
destination deliberately hands off to Batch 3. Device and visual execution
remain reserved for the later user-directed polish pass.

## Batch 3 — Shared conversation core

Status: **Complete**

Deliver:

- one shared direct/group/support/invitation/ended conversation architecture;
- chronological timeline, date sections, message clusters, author identity,
  delivery/failure/deletion states, replies, events, and notices;
- invitation accept/decline, ended membership, support-chat uniqueness, retry,
  and scrolling-to-newest behavior;
- deterministic developer catalog coverage for core renderers.

Exit gate: every retained chat row opens the correct conversation state and
core timeline derivations are covered by tests.

Evidence: one typed destination now renders every retained direct, group,
support, invitation, left, removed, blocked, missing-relay, and catalog chat.
Keyed lazy timelines provide deterministic day sections with sticky headers,
five-minute clustering, direct/group identity differences, replies and
fallbacks, reactions, deletion, delivery/failure and retry, events, notices,
initial newest positioning, and semantic monochrome bubbles. Invitation
accept/decline, support uniqueness, lifecycle composer availability, and
bounded text send/preview updates mutate the same profile-owned state used by
Chats. Thirty-nine unit tests pass, nine Compose tests compile into the
instrumentation APK, lint reports no issues, and both APK variants build.
Complete composer/media/message actions remain in their assigned Batches 4–5;
device and visual execution remain reserved for user-directed polish.

## Batch 4 — Composer, attachments, media, and speech

Status: **Complete**

Deliver:

- empty, single-line, multiline, compact, and expanded composer behavior;
- text/link send, link preview, draft clearing, keyboard/inset behavior;
- Android Photo Picker, document picker, camera capability boundary, attachment
  preview/removal, image/video/file messages, media grids, and viewer paging;
- deterministic hold-to-record/review flow, Voice/Text/Both choice,
  transcription state, playback, and recipient read-aloud/transcript actions;
- no real microphone or speech recognition unless separately approved.

Exit gate: all composer catalog states and attachment/media/speech state
transitions work and relevant tests pass.

Evidence: one profile-owned ordered draft queue now drives all twelve Composer
catalog rows plus the New Draft state. The bounded Material composer provides
explicit expansion, deterministic local link metadata/suppression, reply
context, staged attachment removal, Photo Picker, Open Document, external
camera capture through a non-exported FileProvider, contact/GIF choice, and
atomic sending. Count-derived media grids and one HorizontalPager viewer serve
draft and timeline media; bundled PDF/video fixtures are shared through a
temporary FileProvider URI. Voice review produces exactly one Voice, Text, or
Both message, while timeline voice playback/transcript controls and Android
TextToSpeech remain local. Forty-seven unit tests pass, thirteen Compose tests
compile, lint reports no issues, both APK variants build, and the merged
manifest still has no camera, storage, microphone, network, notification, or
location permission. Device execution remains reserved for the polish pass.

## Batch 5 — Message interaction flows

Status: **Complete**

Deliver:

- quick reactions and complete emoji choice;
- Reply, Forward, Copy, Select, Info, Delete, retry, and action availability;
- selection mode, ordered forwarding, message details, deletion scopes and
  confirmations;
- mention filtering and conversation search by text, sender, and attachment
  label;
- accessible alternatives for every long-press or gesture-owned action.

Exit gate: the full message-action matrix and search/selection flows pass
logic and Compose tests.

Evidence: every nondeleted message now exposes Android `combinedClickable`
hold behavior with haptics plus named accessibility actions. A Material action
sheet owns profile-specific quick reactions, the searchable emoji catalog and
configure/reset/apply flow, Retry, Reply, Forward, Copy, Select, Info, and
Delete in the accepted order. Selection supports zero-to-32 items with visible
state, five-target source-ordered forwarding, scoped deletion, and a typed
Message Details destination. In-place newest-first search covers text, sender,
attachment and link labels while preserving the draft; group mention
suggestions derive from active members. The action catalog now includes
RCT-01–13 and ACT-01–05. Fifty-five unit tests pass, sixteen Compose tests
compile, lint reports no issues, and both APKs build from clean state.

## Batch 6 — Information, membership, and per-chat routing

Status: **Complete**

Deliver:

- Direct Chat Info, Group Info, User Profile, role-aware member profiles, and
  groups in common;
- shared media/files/links categories and viewer integration;
- group metadata/photo edits, add/remove people, promote/demote, last-admin
  protection, leave/ended transitions, and authoritative timeline events;
- notification/disappearing-message controls and indicators;
- independent per-chat relay editing, final-removal send blocking, and
  recovery.

Exit gate: direct/group information and administrative mutations are complete,
isolated, and tested.

Evidence: tapping the conversation identity now opens a typed direct/group Chat
Info list with quick mute/disappearing/search actions, authoritative identity,
shared-content counts, members, archive/leave, and advanced routing. Focused
media, link, and document destinations derive from nondeleted timelines and
reuse the media/file renderers. Weekend Walks exposes admin-only group edit,
Photo Picker, add-person, role, and removal paths with typed events; Product
Circle remains read-only and last-admin protection is retained. Member routes
reuse the complete User Profile surface and add confirmed group actions only
in context. Chat Relays normalizes/deduplicates independent `wss://` endpoints,
permits final removal, blocks sending while empty, and restores captured
defaults without touching siblings. Sixty-three unit tests pass, twenty-one
Compose tests compile, lint reports no issues, both APKs build, and the manifest
still declares no sensitive/runtime permission.

## Batch 7 — Settings and profile services

Status: **Complete**

Deliver:

- Settings hub and active-profile summary;
- Share & Connect, QR presentation contract, copy, Sharesheet, scanner/Profile
  Found state;
- Profile, Profile Keys, Notifications, Appearance, Privacy & Security, Data
  Usage, profile Relays, Support, and Donate;
- per-profile preference isolation, relay role assignment/availability,
  recovery copy, removal consequences, and Restore Defaults;
- Android-native system-setting and permission handoffs only where the
  prototype capability requires them.

Exit gate: every ordinary Settings destination and relay state is reachable,
functional in memory, and tested.

Evidence: the Chats app bar reaches a typed, profile-owned Settings tree with
Share & Connect, standards-compliant local QR generation, permissionless code
scanning, Android Sharesheet/clipboard, complete profile/avatar/address edits,
private-safe key presentation and document exports, live theme changes,
notification/privacy/data preferences, secure-window and system-settings
handoffs, seven profile-relay fixtures with three assignable roles and recovery,
unique support creation, and offline Lightning/Bitcoin donation fixtures.
Seventy-two unit tests pass, 27 Compose tests compile into the instrumentation
APK, lint reports zero issues, both APKs assemble, and the merged manifest has
no sensitive permission. The complete clean static gate passed on 2026-08-15.

## Batch 8 — Developer and destructive flows

Status: **Complete**

Deliver:

- per-profile Developer Tools gate, Debug Mode, Anonymous Telemetry, Audit
  Logging, inline audit files, Diagnostics, Key Packages, and conversation
  debug snapshots;
- sanitized diagnostics and content clearing without sensitive values;
- Sign Out with wipe-default behavior and typed profile-name confirmation;
- Remove Profile and Erase App Data with stable three-word phrase confirmation
  and correct routing;
- deterministic state cleanup and multi-profile outcomes.

Exit gate: developer and destructive flows match accepted consequences and
all lifecycle tests pass.

Evidence: every profile owns an initially locked Developer Tools graph with
independent Debug Mode, telemetry, audit logging/files, diagnostic events, and
one replaceable key package. Accepted conversations expose authoritative,
sanitized debug snapshots only while both gates are enabled. Sign Out retains
or wipes the active profile as selected, Manage Profiles protects the active
identity, and Erase App Data enforces the stable three-word challenge before a
complete root reset. Eighty-seven unit tests pass, 33 Compose tests compile,
lint reports zero issues, both APKs assemble, and the merged manifest still has
no sensitive permission. The complete clean static gate passed on 2026-08-15.

## Batch 9 — Parity hardening and handoff

Status: **Complete**

Deliver:

- close every remaining item in `feature-inventory.md` that belongs to the
  implementation goal;
- accessibility semantics/traversal, keyboard/focus, large font/display scale,
  RTL, compact/expanded layouts, edge-to-edge, IME, and Back audits;
- resource optimization and provenance audit;
- deterministic fixture integrity and performance checks;
- complete build, lint, unit, and instrumentation-test compilation;
- README commands, final architecture map, known visual-polish backlog, and a
  screen-by-screen inspection order for the user.

Exit gate: the app is functionally parity-complete, builds cleanly, and is
ready for user-led visual polish and acceptance.

Evidence: the parity ledger has no implementation-stage gaps. Relay recovery
now distinguishes unassigned, reconnecting, disconnected, and available roles;
future profile publication, direct/group creation, and Support respect those
consequences. Batch 9 also adds a dedicated Support destination, safe raw-key
export confirmation, notification dependency enforcement, decorative-content
semantics, adaptive/RTL/200%-font Compose coverage, 45-avatar/five-raw-resource
integrity checks, and adaptive foreground/background/monochrome launcher
layers. The final clean gate executed 82 tasks in 35 seconds: 89 unit tests
passed, 39 Compose instrumentation tests compiled into the test APK, lint
reported no issues, and both APKs assembled. The merged app has no sensitive
runtime permission; its only permission is AndroidX's generated
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Device execution and visual
acceptance remain explicitly deferred to the user-led polish pass.
