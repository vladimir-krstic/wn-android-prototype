# Android parity feature inventory

This is the durable ledger for the Android port. It describes the accepted
iOS product surface captured on 2026-08-15; it does not predesign Android
screens.

## Status model

- **Reference ready** — iOS docs/source/tests/assets are captured locally.
- **Briefed** — an Android brief exists for a user-selected bounded flow.
- **Implemented** — the bounded Android behavior and states exist.
- **Verified** — relevant automated checks pass and any requested device
  inspection has been performed.
- **Accepted** — the user has accepted the product and visual result.

Every item below is reference-backed and implemented unless its status names
an approved Android difference or explicitly defers device/visual acceptance.

## Application foundation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Native application shell | One activity, typed Compose navigation, monochrome Material 3 themes, edge-to-edge/adaptive root, Android Back ownership, offline deterministic boundary | Verified at the static gate on 2026-08-27 — shared typography/shape scales and component primitives, fully rounded 28 dp tonal form fields with 16 dp content-line alignment and 2 dp state rings, matched 56 dp task controls with primary-emphasis loading states, app-wide unhandled-background-tap focus clearing, 94 unit tests, 80 compiled UI tests, zero-issue lint, and both APKs; device interaction acceptance pending |

## Onboarding and profiles

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Welcome | White Noise mark, first-launch entry, Sign In, and Sign Up | Static gate passed 2026-08-26 — initial and Add Profile routes center a proportional 50%-width mark between system-safe top/app bar and Sign In, cap it at 260 dp, and shrink it in short windows; outgoing IME insets no longer move the mark or actions. Safe-area centering, expanded-width, short-window large-text/RTL, and keyboard-inset regressions compile; renewed device visual acceptance pending |
| Sign In | Private Key entry, validation/progress states, scanner entry, first launch and Add Profile routing | Static gate passed 2026-08-26 — secure Material input with trailing Paste/Clear icon, adjacent 56 dp tonal QR scanner action, bounded form/action measure, validation, primary-emphasis labeled progress, Sign-In-owned scanner sheet state, canonical completion, and focus/IME dismissal before app-bar Back; renewed device visual acceptance pending |
| Private-key QR scan | Camera-based scan, cancel/Back, permission and unavailable states, result returned to Sign In | Static gate passed 2026-08-26 with approved custom Android presentation — near-full Material modal sheet over Sign In, full-sheet CameraX 1.6.1 preview, bundled on-device ML Kit 17.3.0 analysis, just-in-time camera permission, compact centered Material handle/app-bar controls with inherited app-bar title typography, single white rounded-corner target, ephemeral result return, and swipe/Back/denial/wrong-content/unavailable recovery; renewed device visual acceptance pending |
| Sign Up | Name, About, optional avatar, deterministic creation, first profile and Add Profile variants | Static gate passed 2026-08-27 — bounded Material form composition with a scrollable 120 dp-avatar form and a separate full-width primary action pinned above navigation and IME safe areas; tonal pinned-app-bar scroll state, primary-emphasis labeled progress, both canonical identity paths, accessible photo feedback, and focus/IME dismissal before app-bar Back remain; renewed device visual acceptance pending |
| Avatar selection | Bundled choices, web-image catalog evidence, local image selection/capture path, processing limits | Static gate passed 2026-08-26 — Photo Picker, Files, 21 local web choices, deterministic URL mapping, remove/failure states, and EXIF-aware 512-pixel preparation. Fixed the shared decoder's rejection of every valid bounds-only result. Web Search/URL now uses native filled-tonal mode buttons, a 16 dp Done inset, modal-owned safe/IME insets, 24 dp result separation, scrollable content, and explicit search/clear actions. Image-import regressions and eight picker interaction/layout/local-restoration regressions compile; device execution and visual/TalkBack acceptance pending |
| Profile lifecycle | Add, switch, edit, re-onboard, sign out, remove, and isolated profile-owned state | Implemented — all identity, session, switching, editing, removal, and whole-app reset paths mutate isolated profile-owned state |
| First-login privacy choices | Optional per-profile analytics/logging after initial or Add Profile onboarding | Static gate passed 2026-08-26 — DiagnosticsPromptHost waits for RESUMED Chats; ID-bound immediate switches, dismissal-only seen state, restoration/retention/isolation tests; prompt intro, switch labels/controls and privacy copy share the exact 24 dp content line while each 56 dp switch target uses an 8 dp outer inset, rounded Material state layer and 16 dp inner padding; scoped iOS 4c25393 evidence; no real telemetry |
| Verified Nostr Address | Edit, deterministic verification state, verified seal, compact/read-only presentation | Implemented — validation and stored verification restoration; Profile defaults to full-contrast selectable read-only fields, puts the filled seal inside the address field, and permits changes only through explicit Edit/Save mode |

## Chats hub and creation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Chats empty | Empty state, profile entry, New Message entry, relay recovery when relevant | Static gate passed 2026-08-26 — avatar-to-Settings, no default title, native icon-only New Message/Check Relays FAB in the active pane, hidden during search; edge-to-edge list with FAB content clearance; device acceptance pending |
| Chats populated | Direct/group/support/catalog rows, previews, dates, unread counts, delivery and attachment summaries | Device-inspected on Pixel 8a 2026-08-31 at default and 200% type, light/dark appearance, RTL, and forced wider widths — ChatListRow/ChatListPresentation preserve all 77 fixtures/timestamps, semibold names and aligned avatars; native clickable rows use a 12 dp avatar gap while retaining the 16 dp artwork edge; mute/timer/membership icons follow the measured, ellipsized title independently of the timestamp; one exclusive invitation/failure/count/manual/none indicator with a filled invitation-plus badge, matching scalable footprints and retained unread state; user visual acceptance remains separate |
| List organization | Pinned ordering, archived and left states, stable catalog/story order, search | Static gate passed 2026-08-30 — default Chats includes 72 non-archived rows including ended membership; Unread includes all unread non-archived rows; selected filled filter, shared preview/search projection, unchanged Archived/Left scopes. Active search uses a user-approved compact 48 dp-minimum top-app-bar field with centered, fully contained `bodyLarge` input, font-scale growth and native 48 dp Back/Clear targets; geometry regression compiles and device acceptance remains pending |
| Row interactions | Open, pin/unpin, mark read/unread, archive/unarchive, leave/remove as allowed | Refined 2026-08-30 — ChatContextMenuRow keeps its 8 dp inset native rounded highlight and anchored Material menu; shared Mute for uses title-aligned 56 dp whole-row radio choices whose rounded state layer expands to an 8 dp dialog-edge gutter without moving the content, with visible/semantic current-duration state, stable action order, live ID-bound eligibility, safe dismissal, TalkBack actions, Undo, confirmations and sole-admin guard; static gate passed and device acceptance pending |
| Profile switching | Chats avatar routes to Settings; Settings owns account switching, unread badges, Add Profile, and isolated profile state | Static gate passed 2026-08-26 — a sole profile gets a direct, divider-separated in-card Add Profile list row; alternates get a stacked-avatar Switch Profile row and expand inline into deterministic 48 dp rows with capped unread badges and a final Add Profile row; Back collapses first; the retained modal switcher preserves active-first ordering, check precedence and safe dismissal; exact sign-up/sign-in seven-profile sets and independently owned people, chats, settings, diagnostics, relays, developer state, and drafts remain tested; device acceptance pending |
| New direct chat | People search/list, Person Profile step, Message action, deduplication, relay requirement | Pixel 8a inspection passed 2026-08-31 at 200% type in dark appearance — gray Settings-detail canvas, standard 56 dp standalone search, edge-to-edge lazy viewport with safe bottom inside scroll content, one rounded white-equivalent lazy people group with 2 dp canvas-tone separators and the current Material 12 dp avatar/text relationship, ordinary icon-led New Group disclosure, shared no-results state, existing-direct reuse, relay copying, and profile-relay recovery; user visual acceptance remains separate |
| New group | Person selection, setup, name/photo/description, creator admin role, initial event, copied chat relays | Pixel 8a inspection passed 2026-08-31 at 200% type in dark appearance with the IME hidden and visible — compact white-equivalent search, stable removable 64 dp avatar strip, rounded white-equivalent selection and read-only setup-member groups with 2 dp canvas-tone separators and the current Material 12 dp avatar/text relationship; selected directory rows retain their first/middle/last geometry and add only the trailing check; setup reuses the 120 dp profile avatar/native tonal photo action with a proper empty group symbol, no redundant details title, rest/scrolled action-surface colors, Photos/Files/Web states, validation, members/admin, event, relay recovery, and route cleanup; user visual acceptance remains separate |
| Person Profile | Identity, About, verified address, public key, shared groups, message/contact/block actions | Static gate passed 2026-08-30 — Share & Connect avatar/headline/address/copy-capsule metrics, balanced 16 dp name relationships, higher-contrast tonal About treatment, iOS-parity About ordering, navigable Groups in Common, native add-to-group sheet/confirmation, Add/Remove Contact, Block/Unblock, grouped admin actions, and pinned Message/recovery; UI regressions compile, device acceptance pending |

## Conversation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Shared conversation shell | Direct, group, support, invitation, ended, and developer-catalog conversations | Static gate passed 2026-08-30 — small Material identity app bar center-aligns its 40 dp avatar and compact title/metadata block with a 4 dp horizontal gap and user-approved negative 2 dp line arrangement for about 30% less visible leading; the identity retains Button/Chat Info behavior without the explicitly removed gray press indication, and the root app bar has no Search action; device acceptance pending |
| Timeline and clustering | Chronology, day boundaries, author and five-minute clusters, date markers, events, notices | Implemented — keyed lazy timeline, sticky date sections, deterministic ordering, cluster edges, group identity and centered information rows |
| Message states | Incoming/outgoing, sent/delivered/read/failed, retry, deleted/tombstoned, replies and missing fallbacks | Implemented — semantic bubbles, terminal time/state, retry, scoped deletion, reply resolution, action availability, and typed details |
| Text and links | Plain and multiline text, detected links, link preview, copy/search/read-aloud behavior | Visual-polish static gate passed 2026-08-21 — local first-HTTPS metadata, named suppression, tonal rich cards, copy/search, and received-text Android TextToSpeech with live progress |
| Images and video | One-to-seven media layout coverage, aspect clamping, viewer paging, capture and selection | Visual-polish static gate passed 2026-08-21 — count-derived grids, standard playback treatment, Photo Picker/external camera selection, ordered draft shelf, and safe-area exact-page HorizontalPager viewers |
| Files and rich content | Documents, contact, GIF showcase, deterministic labels/previews, unavailable media | Pixel 8a inspection passed 2026-08-31 for media timelines and the full-height searchable contact sheet with its white-equivalent segmented group and short public keys; system document handoff, semantic file/contact cards, retained bounded legacy GIF content, unavailable placeholders, and local-only fixture behavior remain statically covered; user visual acceptance remains separate |
| Reactions | Quick reactions, full emoji picker, counts, current-profile selection and replacement | Visual-polish static gate passed 2026-08-21 — profile-owned quick strip, selected extra, scroll-safe Material reactions, adaptive 48dp emoji grid, named configuration slots, swap/reset/apply, and semantic selected count chips |
| Replies and mentions | Reply context, deleted/missing fallback, group-member mention filtering | Implemented — action-owned reply draft/focus, cancellation and fallbacks; group draft suggestions derive from matching active members |
| Composer | Empty/one-line/multiline drafts, pull expansion, keyboard behavior, send clearing, link/media/file states | Current Pixel 8a inspection passed 2026-08-31 in light appearance with the IME hidden/visible and at 200% type in dark appearance, RTL, and forced wider windows — the full-width backing/divider is absent, the idle 48 dp Add action uses app-standard `primary` contrast, the 48 dp-minimum Foundation capsule has a low-emphasis 1 dp `outlineVariant`, and timeline content paints to the physical bottom edge while safe-bottom/composer clearance remains scrollable content padding; compact reservation, header-to-IME expansion, direction-locked spring drag, newest-visible timeline push, Back/focus/inset priority, ten/six/eight-line budgets, attached context and atomic send clearing remain; user visual acceptance remains separate |
| Attachments | Android-native photo/video picker, camera, document picker, preview/remove/viewer behavior | Current Pixel 8a inspection passed 2026-08-31 in light/dark, 200%-type, RTL, IME-visible and forced wider-window states without sensitive permissions — the composer-scoped Material popup places Camera/Photos and videos/Files/Contact with a clear visible gap above Add and native elevation, keeps editor focus and the IME, omits GIF acquisition, and retains the 20-item Photo Picker, aspect-derived ordered shelf, 48 dp removal, preparation/error feedback, platform contracts and exact-page review; existing GIF content remains compatible and user visual acceptance remains separate |
| Speech messages | Hold to record, review, deterministic waveform/sample, Transcribe, Voice/Text/Both formats, playback | Current Pixel 8a inspection passed 2026-08-31 for recording, pre/post-transcription review, Both format, and the anchored format menu — the approved deterministic local simulation uses 24 dp idle/live waveform geometry, one trailing visible sample per 200 ms, red icon-only Stop, upward-arrow Send, filled circular Play/Pause, light-gray bubble-led Transcribe, compact clipped 32 dp-minimum state layers inside unchanged 48 dp Transcribe/format targets, and an above-content format selector while preserving the 400 ms hold, uncapped timer, saveable inline review, deterministic transcription, editing, duration and exactly one result; user visual acceptance remains separate |
| Recipient speech actions | Read Aloud, Transcribe, Show/Hide Transcript, Copy Transcript, local-only provenance | Visual-polish static gate passed 2026-08-21 — view-local Transcribe, transcript reveal/copy and provenance, plus Android TextToSpeech with live progress and stop/shutdown lifecycle |
| Message actions | Long-press context, Reply, Forward, Copy, Select, Info, Delete, permissions and full action availability | Visual-polish static gate passed 2026-08-21 — Android combined-click hold/haptic, contextual message preview, semantic icon rows, named accessibility alternatives, conditional Copy/Retry, tonal typed details, and scoped confirmation |
| Selection and forwarding | Multi-select mode, target selection, ordered copies, media and keyboard behavior | Visual-polish static gate passed 2026-08-21 — explicit selected containment, live count, named compact actions, disabled-state explanation, native searchable checkboxes, 32-source/5-target limits, and source-ordered copies |
| Conversation search | Search text, sender, and attachment labels; current count and previous/next navigation | Pixel 8a inspection passed 2026-08-31 — exact shared 48 dp Chats field and Android Back behavior, focused in-place entry only from Chat/Group Info, IME-visible previous/next/count controls, newest-first projection, full-contrast result messages with only nonmatches subdued, platform-cyan/black 4 dp rounded exact-glyph highlights across visible text/author/attachment/link labels, current-only spoken position, and draft-preserving close; user visual acceptance remains separate |
| Disappearing messages | Header/list indicators, duration setting event, deterministic status behavior | Implemented — list/header indicators, duration choice, and deterministic timeline events derive from chat state |
| Invitation and ended states | Read history before accept/decline, participation transition, leave/removed/ended composer behavior | Visual-polish static gate passed 2026-08-21 — exact outcomes and history remain intact; invitation, left, removed, blocked, and missing-relay states now share one accessible tonal lifecycle hierarchy with direct recovery |
| Relay recovery | Per-chat routing, final relay removal, send-disabled history-preserving state, add-to-recover | Implemented — independent editing, final-removal warning, history preservation, send block, Check Chat Relays recovery, and captured-default restore |

## Chat and group information

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Direct Chat Info | Peer identity, notification/search actions, shared content, chat relays, relationship state | Pixel 8a inspection passed 2026-08-31 — uncontained identity, equal tonal icon actions including Search return into the focused conversation flow, grouped shared/actions hierarchy, selected duration state, archive/direct leave, and independent relays; user visual acceptance remains separate |
| Group Info | Photo/name/description/member count, notifications, search, shared content, management, Leave Group | Static gate passed 2026-08-30 — identity/member hierarchy, equal quick actions including Search, admin-gated edit/add, shared categories, advanced routes, lifecycle status, archive and destructive leave; device acceptance pending |
| Member management | Roles, admin promotion/demotion, add/remove people, last-admin protection, role-aware profile | Visual-polish static gate passed 2026-08-21 — native checkbox selection, separate Profile/Group Actions, confirmed role/removal mutations, actor gates, and last-admin safeguards |
| Group mutation | Edit metadata/photo, remove photo, deterministic events, leave/ended transitions | Visual-polish static gate passed 2026-08-21 — system Photo Picker, progress/error feedback, content-aligned tonal fields, pinned Save, typed events and authoritative lifecycle updates |
| Shared media | Media/files/links categories, chronological index, unified viewer and unavailable-page rules | Visual-polish static gate passed 2026-08-21 — counted disclosure rows, complete empty state, stable open three-column grid, exact selected-page shared pager, and semantically styled rich lists |
| Chat Relays | Normalize/deduplicate endpoints, independent per-chat editing, final removal and recovery | Visual-polish static gate passed 2026-08-21 — strict `wss://` behavior, tonal endpoint group, named removal, empty recovery, final-relay consequence, and captured-default restore |
| Groups in common | Active-membership-derived list shared by current and viewed profile | Implemented — derived from active shared membership and used by the authoritative profile screen |

## Settings and support

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Settings hub | Active profile group, native settings organization, profile switching | Pixel 8a inspection passed 2026-08-31 — one white-equivalent profile card on a neutral Material canvas; 56 dp active identity opens Share & Connect; a plain gap-separated list row is Add Profile for one identity and stacked-avatar expandable Switch Profile for alternates, with a true circular `+N`, monochrome unread badges, and final Add Profile without management commands; seven-destination and Help groups are unheaded white-equivalent cards with visible two-dp canvas-tone gaps; isolated Sign Out and centered `Version 0.1` footer remain; the pinned header matches the canvas at rest and uses the next tonal role under scroll; no root Manage Profiles or Appearance summary; user visual acceptance remains separate |
| Share & Connect | Local QR generation, public profile values, copy, Android Sharesheet, scanner/Profile Found state | Rebuilt 2026-08-27 — one stable Share & Connect identity page with centered native app bar; official filled verification seal; visibly 240 × 32 dp public-key capsule at default text scale inside a transparent 48 dp target, with 16 dp horizontal content padding, full-key middle ellipsis, temporary copy confirmation and a bounded capsule-sized state layer; dominant bounded black-on-white QR removes its Share-only encoder margin, uses a literal 12 dp white frame with the theme's 16 dp large corners, begins 16 dp below the public-key target, and keeps a one-dp caption gap; Android Sharesheet action; explicit full-width primary Scan QR Code task is pinned above the navigation safe area and reuses the near-full CameraX/ML Kit scanner with profile-specific permission/error/result handling and deterministic Profile Found; dismissal returns to the useful identity page; static gate passed 2026-08-27 and device acceptance remains separate |
| Profile | Edit name, About, avatar, Verified Nostr Address and deterministic verification | Reworked 2026-08-27 — Profile defaults to full-contrast selectable Material read-only fields with a low-emphasis app-bar Edit text action; the official filled verification badge lives inside the address field without duplicate helper copy; avatar sources and the single validated filled Save task appear only in edit mode; Save returns to read mode on the same all-surface page, and Back cancels an active draft before leaving; compiled UI regression covers mode, field, badge and save behavior; device acceptance pending |
| Profile Keys | Private/Public Key presentation and safe copy/share boundaries | Static gate passed 2026-08-27 — complete visible key values measure before middle ellipsis while the hidden private-key mask clips without an added ellipsis; compact white-equivalent groups sit on the neutral Settings canvas; the encrypted-export Material alert uses aligned white shared rounded fields on a gray canvas plus pinned-iOS-equivalent Low/Fair/Strong strength feedback; raw-export consequence and recoverable Android document-write failure remain; device acceptance pending |
| Notifications | Master and detail preferences, preview behavior, system-setting handoff where applicable | Reworked 2026-08-29 — real app-wide Android notification access conditionally shows first-request or blocked-recovery UI, invokes the native Android 13+ permission prompt, refreshes on resume and removes the old permanent Android section; profile-owned Local notifications and dependent Native push remain separated whole-row switches; three dependent preview radio rows remain inline with immediate selection, deterministic example and accessible semantics; no notification creation, delivery, channels, push service or product persistence; static gate passed 2026-08-29 |
| Appearance | System/light/dark choice and accepted language selection with monochrome semantic schemes | Reworked 2026-08-29 — immediate profile-owned System default/Light/Dark theme uses a separated transparent-resting Material radio group and focused helper; one Language disclosure opens a typed child route with eight accepted native radio choices and Back behavior; deterministic language state does not claim translated resources or invoke Android's app-wide locale API; static gate passed 2026-08-29 |
| Privacy & Security | Device authentication, Recents snapshot privacy, Erase App Data entry and availability | Pixel 8a inspection passed 2026-08-31 in light/dark appearance at default and 200% type, including the scrolled destructive section — separated Material device-protection group, Android Recents wording, real device-lock recovery, and Auto-lock revealed only while authentication is effective; its single-choice radio list sits directly on the Material dialog surface without a nested card or separators; Diagnostics and device-wide erase use concise rows with consequences outside the actions; user visual acceptance remains separate |
| Diagnostics & Improvements | Typed consumer privacy destination, independent choices, summary, stored size, confirmed clear | Corrected 2026-08-30 — Privacy & Security is the sole permanent consumer entry; its concise summarized route, independent native switches, conditional Stored Diagnostic Logs, platform-formatted size and confirmed clear preserve preferences; Developer Tools no longer links to or duplicates these controls; large dark RTL regression compiles and static gate passed |
| Data Usage | Media quality and auto-download preferences by media type and connectivity level | Reworked 2026-08-29 — separated automatic-download and sent-media groups, section-local product help, direct-surface immediate Material radio dialogs, sent-quality data/compression consequence, and default-aware four-policy reset; full static gate passed and device acceptance remains pending |
| Profile Relays | Endpoint list/details, Profile/Inbox/Chat Messages roles, availability, recovery, restore defaults | Refined 2026-08-30 — separated name/URL rows use compact 20 dp filled green connected and filled red not-connected check/close indicators with exact status semantics; Add Relay remains in-list; Restore Default Relays is a native filled-tonal button; detail URLs use normal one-line body text, flexible remaining width and middle ellipsis with complete semantics; compact Relay metadata, accepted Use For copy, validity-aware Material task sheet, read-only gating, focused confirmations, and shared 32 dp / 8 dp helper alignment remain statically covered; device acceptance pending |
| Support | Create/open one profile-owned White Noise Support conversation without duplicates | Refined 2026-08-30 — compact support identity group, title-aligned purpose helper, contextual inline Start Chat, and unchanged relay recovery/availability behavior; static gate passed and device acceptance pending |
| Donate | Deterministic donation surface and accepted copy | Refined 2026-08-30 — compact centered Material 3 Expressive connected single-choice button group for Lightning/Bitcoin plus the exact shared Share & Connect QR and npub capsule components; adaptive QR size/frame/margin and 240 × 32 dp capsule inside an unchanged 48 dp target cannot drift independently; the 1 dp QR-to-target relationship is retained while the smaller `bodyMedium` method caption sits 5 dp below the visible pill; no wallet or duplicate pinned action; static gate passed and device acceptance pending |
| Developer Tools gate | Per-profile technical master gate and Debug Mode, independent of consent | Corrected 2026-08-30 — progressive master gate, separated debugging/key/log groups, title-aligned helpers, divided peer rows and compact About metadata; Diagnostic Logs is read-only On/Off plus only non-empty files or a durable empty row, with no consumer destination or clear action; non-empty data adds a native system-document Export Diagnostic Logs action with deterministic sanitized payload and focused failure recovery; master-off still resets debug only and preserves records and consent; static gate passed and device acceptance pending |
| Diagnostics | Persistent live event console, Test/Clear commands, empty state, sanitized copy | Refined 2026-08-30 — native top-app-bar vertical-dots action and shared Material menu contain optional summary copy, Test, and disabled-when-empty Clear Events; compact green radiowave alpha pulse reinforces the persistent Live label and Live event stream semantics above one 16 dp rounded divided console; Events and Live share the console rows' 16 dp inner content line; static gate passed and device acceptance pending |
| Diagnostic logs | Profile-owned retained records; clearing preserves records and preferences | Corrected 2026-08-30 — DiagnosticsState owns deterministic records and a sanitized export projection; Privacy & Security exclusively owns preferences, retained size and confirmed clearing; Developer Tools independently shows read-only logging state, only non-empty files, **There are no logs.** after clearing, and a data-conditional Storage Access Framework text export that excludes profile/file identifiers; no collection or transport |
| Key Packages | One current deterministic package and Publish New Key Package replacement | Refined 2026-08-30 — contained current package plus a real full-width 56 dp filled-tonal Publish New Key Package button with package symbol, complete button semantics and title-aligned consequence helper; exact one-package replacement behavior remains tested; static gate passed and device acceptance pending |
| Conversation debug | Profile-gated developer action and deterministic direct/group snapshots | Visual-polish static gate passed 2026-08-21 — grouped authoritative values, explicit copy rows, Diagnostics disclosure, and complete disabled/unavailable states |
| Sign Out | Active-profile flow, wipe default, typed profile-name gate, routing to switcher or Welcome | Refined 2026-08-30 — expanded-only native sheet, current Material identity row and 2 dp-separated wipe choice, external 32 dp consequence/confirmation helpers, persistent exact-name label and gate, continuous pinned error action, and dismissal-safe named progress; static gate passed and device acceptance pending |
| Remove Profile | Remove another stored profile with exact-name confirmation and active-profile protection | Visual-polish static gate passed 2026-08-21 — grouped inactive identities, durable no-other-profiles state, focused identity confirmation, and no active-profile action |
| Erase App Data | Device-wide irreversible flow, stable three-word phrase gate, removes all local state | Reworked 2026-08-29 — Android Material sheet starts Expanded and exposes no partial anchor while retaining Hidden dismissal; semantic error callout, selectable phrase, shared white rounded exact-match field, pinned error action continuous with the gray modal canvas, safe/IME insets and dismissal-safe progress; static gate passed |

## 2026-08-30 conversation header and search refinement

The conversation identity app bar uses a centered 40 dp avatar and compact
title/metadata block inside a 48 dp touch target. A negative 2 dp line
arrangement reduces the visible title-to-metadata leading by about 30%. The
identity remains an accessible Chat Info button while omitting the user-
rejected visible gray press indication. Conversation search reuses the exact
compact Chats search field, is available only from Chat/Group Info, keeps every
matching message at full contrast, and highlights visible matching glyphs with
Android platform cyan, black text, and 4 dp rounded geometry. Previous/next and
current/total controls remain visible above the IME, and Android Back closes
search without losing the draft.

Gate: `./gradlew clean testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest` passes. All 132 unit tests pass; instrumentation
sources compile; app and test APKs assemble; lint reports zero errors and 11
non-blocking warnings for pinned dependency updates and existing unused
strings. Focused UI coverage verifies header alignment, the shared 48 dp search
field, Chat Info search entry, Back behavior, result controls and the cyan
highlight. No emulator/device was launched, installed, interacted with or
captured; instrumentation execution, TalkBack and visual acceptance remain a
separately requested step.

## 2026-08-30 message composer overhaul

The available composer is now a transparent overlay with no full-width backing
or divider, a separate 48 dp Add action, compact Foundation editor capsule,
and exactly two direct-drag endpoints. It reserves only its compact height and
pushes the timeline by the exact travel only when the newest message was
visible. Android owns IME,
Back, insets, focus, spring settling and accessible alternatives. The anchored
attachment menu is exactly Camera, Photos and videos, Files, Contact; Photo
Picker supports 20 items and GIF acquisition is removed without invalidating
GIF content. Ordered aspect-derived previews remain inline.

Voice is an inline deterministic Idle/Recording/Review state machine with a
400 ms hold, uncapped timer, playback, transcription, anchored Voice/Text/Both
format menu, editable transcript, duration preservation and exactly one
outgoing result. Review and expansion restore safely; an interrupted active
recording restores as Review.

Gate: `./gradlew testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest` passes after the optical follow-up with 137 unit
tests, no lint errors, and both APKs. Before that follow-up, focused
`ConversationScreenTest` passed 30/30 tests on an Android 17 Pixel 10 Pro XL
emulator, and hands-on captures covered compact/expanded and IME states,
attachments/menu, live recording, pre/post-transcription review, all three
formats, dark appearance, 150% text and a wider resizable window. The updated
instrumentation APK compiles but has not been installed or run; the prior
captures do not evidence the latest optical adjustments. The repository-wide
instrumentation command also continues into unrelated legacy test classes
whose activity harness or historical assertions predate this flow. User visual
acceptance remains pending.

Subsequent user-approved polish gives Add a clear 10 dp popup gap and keeps the
format popup 2 dp above its target (10 dp from its inset visible pill), with Material's native menu shadow; matches idle
waveform artwork to the 24 dp Add icon footprint, and replaces the flashing
sparse live waveform with a dense 24 dp deterministic trailing window. Stop is
now a red icon-only action; Send uses an upward arrow; Review uses a filled
Play/Pause circle and light-gray bubble-led Transcribe; contact sharing opens
an expanded searchable sheet with a white-equivalent grouped directory. Static
evidence is recorded in the same implementation and tests; the prior device
captures predate these optical adjustments.

## 2026-08-26 Chats, privacy and shared-sheet static verification

The approved batch uses scoped iOS `4c25393`; all unrelated baseline behavior
and conversation fixtures are unchanged. Shared `WhiteNoiseScaffold`,
`WhiteNoiseTopBar`, lazy-list/grid and form scroll tracking connect ordinary
headers to actual restored/programmatic positions. Chats scrolls to the
bottom viewport; safe clearance is in content padding, not a fixed stripe.
Camera/media/selection styling is preserved. The approved follow-up uses
Signal `441ba42` only as a highlighted-row/anchored-menu interaction reference.
`WhiteNoiseModalBottomSheet`/`WhiteNoiseSheetHeader` are applied to profile,
diagnostics, composer/contact/GIF/voice review, message actions/reaction/
forwarding, timer, destructive-task and web-image sheets. Ordinary rows are
transparent, intentional groups/fields keep their roles, and task app bars
have no repeated insets. `MuteDurationDialog` is shared by Chats and Chat Info.
Control assets use unmodified Google XML; exact hashes and source links live
in `docs/references/material-symbols.md`.

Gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
117 unit tests pass; 121 instrumentation tests compile; lint reports no
issues; app and test APKs assemble. New coverage lives in
`ChatListPresentationTest`, `DiagnosticsConsentTest`, `ChatsPolishTest`,
`DiagnosticsPromptTest`, `MaterialSheetTest`, `ChatInfoScreenTest`, and
`HeaderScrollTest`. UI cases include
compact/expanded, RTL, 200% text, light/dark, keyboard-open onboarding,
scroll restoration, menu edge placement/order/dismissal, no horizontal chat
actions, status sizing, native FAB placement/bottom reach, shared mute,
sheet background/header geometry, rounded inset prompt-switch state layers,
Undo, destructive safeguards, Back and
privacy prompt timing/dismissal lifecycle.
No emulator/device was launched, installed, interacted with or captured;
instrumentation execution, actual system gestures, TalkBack and visual
acceptance remain a separately requested step.

## 2026-08-26 app-wide Expressive menu migration

All six app-owned popup entry points use `WhiteNoiseDropdownMenu` and Google's
actual `DropdownMenuPopup`/`DropdownMenuGroup`/new item overloads: Chats actions,
Chats filters, Sign Up photo sources, Set Up Group photo sources, Profile photo
sources, and voice-review format. Native shapes, standard colors, typography,
targets, positioning/focus/RTL/motion and selected semantics replace the
baseline menus. Scrolling remains inside the native group. Action labels,
ordering, eligibility, Undo, confirmations, photo contracts, voice results and
consent behavior are unchanged.

WN-ANDROID-0040 records the user-approved Material 1.5.0-alpha25 exception.
Alpha26's ripple requires API 24; alpha25 retains API 23 and the new menu
family. The existing BOM, core Compose 1.12.0 and unrelated dependencies stay
unchanged. The merged app still has no network permission.

Gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes: 123 unit tests, 136 compiled instrumentation tests, both APKs and zero
lint errors. The remaining availability warnings are for intentionally pinned
Navigation, Material 3 compatibility, and CameraX versions.
`WhiteNoiseMenuTest` plus extended `ChatsScreenTest`/`ConversationScreenTest`
cover native roles/selection, enabled/dismiss-before-dispatch behavior, themes,
edge anchors, small/large panes, 200% RTL text, constrained scrolling and IME
transitions. Existing Chats and privacy regressions remain compiled. No device
execution or visual acceptance is claimed. `docs/screens/app-menus.md` also
records the broader audit: baseline button typography/shape behavior and
standard global motion remain follow-up, not completed Expressive parity.

The Settings follow-up adds pure Add/single/multiple management projection,
stable active-first switcher projection, unread aggregation, exact seven-profile
fixture-set assertions, root callback/organization tests, single/stacked
previews, direct Add Profile, inline selection/Back collapse, selected-check/
99+ coverage and modal dismissal safety. Static compilation
also retains compact/expanded, large-text RTL, theme, scroll and inset
regressions. Device execution and visual acceptance are not claimed.

## 2026-08-26 chat title metadata and invitation-badge refinement

`ChatListRow` groups mute/timer/ended-membership symbols beside the title and
reserves their measured widths plus the timestamp before native ellipsis.
Short titles keep their symbols adjacent; full names remain accessible.
Invitations use the same primary circular badge as unread counts with the
official Add symbol in onPrimary. No fixture, timestamp, action, menu,
navigation, consent or dependency change.

The same static gate passes: 117 unit tests, 128 compiled instrumentation
tests, both APKs, zero lint errors and the existing Material compatibility-pin
warning. `ChatListRowTest` covers adjacent icons, title truncation, reserved
timestamp/baselines, 320/680 dp, 100/200% text, LTR/light and RTL/dark, plus
invitation circle/plus colors in both themes and font scales. The extended
`ChatsPolishTest` checks equal count/manual/invitation/error geometry.
These UI regressions are compiled only; no device execution or visual
acceptance is claimed.

## 2026-08-26 Chats long-press spacing refinement

`ChatContextMenuRow` now anchors the native popup to an 8 dp inset row.
`ChatListRow` uses the pinned Material clickable overload: native 12 dp
avatar/content spacing, native vertical padding and feedback, and the native
selected shape while its context menu is open. Its 8 dp inner horizontal
padding preserves the shared 16 dp avatar/trailing-content edge. Selection
does not move the row's contents. `WhiteNoiseDropdownMenu.anchorSpacing`
adds an 8 dp transparent vertical popup gutter only for Chats; native
above/below fitting, edge fallback, start alignment, RTL and dismissal remain
in charge. Other popup entry points keep their existing spacing.

Gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes: 117 unit tests, 130 compiled instrumentation tests, both APKs, zero
lint errors and the existing Material compatibility-pin warning.
`ChatListRowTest` adds rounded-highlight pixels/insets, stable avatar/text
geometry, 12 dp spacing, Button semantics, and above/below 8 dp menu-gap
assertions across compact/expanded and large-text RTL/dark variants.
Existing action, status, title, badge and privacy regressions remain compiled.
No fixture, action, consent, dependency, icon, or system-surface change.
No emulator/device execution or visual acceptance is claimed.

## 2026-08-26 Chats scroll-crash repair

Read-only Pixel 8a crash logs confirm repeated main-thread
`LayoutNode … not found in RectList` exceptions. Stacks reach Material
`InteractiveListItemMeasurePolicy`'s supporting-slot baseline query during
both visible lazy measurement and prefetch. This is runtime failure evidence
from the previously installed build, not verification of a repaired APK.

`ChatListRow.ChatRowTextLayout` now measures the title, metadata, timestamp,
preview and status together in the native clickable row's content slot. It
reads only direct Text baselines and places children only during placement,
avoiding inherited-container baseline queries. Native Material still owns
the row's 12 dp avatar gap, padding, shape, feedback and menu behavior. The
text region retains the pinned 72/88 dp two-/three-line minima, font-scaled
growth, timestamp baseline, adjacent metadata and preview-aligned status.
The 8 dp highlight insets and menu gap, fixtures, actions and dependencies
are unchanged.

Gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes with 117 unit tests, 133 compiled instrumentation tests, both APKs,
zero lint errors and the existing Material compatibility-pin warning.
`ChatListScrollRegressionTest` adds native fling/jump scrolling, unchanged
chat state and usable menus afterward; explicit subcomposition reuse,
premeasure/promotion/cancellation with text/attachment/status variants;
and row-minimum-height/status-alignment checks. Coverage includes compact/
expanded widths and 100% LTR/light / 200% RTL/dark text. Existing geometry,
status, accessibility, action and privacy tests remain compiled.

No APK was installed and no UI tests were device-executed in this repair turn.
The runtime fix still needs confirmation by scrolling the newly built APK.

## Deterministic model and quality coverage

These are product functions, not optional polish:

- Profile-owned People, chats, settings, relays, developer preferences, audit
  artifacts, reactions, drafts, and unread state remain isolated.
- Chat-list rows derive from authoritative chat state; they are not separately
  mutated fixtures.
- Direct chats deduplicate by person. Support chats never duplicate.
- New chat creation copies available profile Chat Messages relays once;
  per-chat edits never rewrite profile defaults or sibling chats.
- Relay role availability derives from assignment, capability, and connection
  state, including unassigned, reconnecting, disconnected, and available.
- All catalog scenario IDs and deterministic renderer coverage remain stable
  until an Android brief deliberately translates or consolidates them.
- Sensitive values never enter screenshots, diagnostics, logs, examples, or
  accessibility output.
- Android must cover Back, state restoration, system insets/keyboard, TalkBack,
  large font/display size, light/dark themes, RTL, and adaptive widths for the
  selected flow.

## Intentional prototype exclusions

Unless later user direction expands scope, parity does not mean adding:

- a backend or networking;
- real Nostr relay access, Marmot, MLS, Rust, cryptography, or key management;
- real authentication or account recovery;
- durable database or preference storage;
- remote avatar fetching or arbitrary web content;
- microphone speech recognition when deterministic local fixtures provide the
  accepted prototype behavior;
- payments, push registration, or telemetry upload.
