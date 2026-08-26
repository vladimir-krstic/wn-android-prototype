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
| Native application shell | One activity, typed Compose navigation, monochrome Material 3 themes, edge-to-edge/adaptive root, Android Back ownership, offline deterministic boundary | Verified at the static gate on 2026-08-25 — shared typography/shape scales and component primitives, fully rounded 28 dp tonal form fields with 16 dp content-line alignment and 2 dp state rings, matched 56 dp task controls with primary-emphasis loading states, app-wide unhandled-background-tap focus clearing, 94 unit tests, 79 compiled UI tests, zero-issue lint, and both APKs; device interaction acceptance pending |

## Onboarding and profiles

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Welcome | White Noise mark, first-launch entry, Sign In, and Sign Up | Static gate passed 2026-08-26 — initial and Add Profile routes center a proportional 50%-width mark between system-safe top/app bar and Sign In, cap it at 260 dp, and shrink it in short windows; outgoing IME insets no longer move the mark or actions. Safe-area centering, expanded-width, short-window large-text/RTL, and keyboard-inset regressions compile; renewed device visual acceptance pending |
| Sign In | Private Key entry, validation/progress states, scanner entry, first launch and Add Profile routing | Static gate passed 2026-08-26 — secure Material input with trailing Paste/Clear icon, adjacent 56 dp tonal QR scanner action, bounded form/action measure, validation, primary-emphasis labeled progress, Sign-In-owned scanner sheet state, canonical completion, and focus/IME dismissal before app-bar Back; renewed device visual acceptance pending |
| Private-key QR scan | Camera-based scan, cancel/Back, permission and unavailable states, result returned to Sign In | Static gate passed 2026-08-26 with approved custom Android presentation — near-full Material modal sheet over Sign In, full-sheet CameraX 1.6.1 preview, bundled on-device ML Kit 17.3.0 analysis, just-in-time camera permission, compact centered Material handle/app-bar controls with inherited app-bar title typography, single white rounded-corner target, ephemeral result return, and swipe/Back/denial/wrong-content/unavailable recovery; renewed device visual acceptance pending |
| Sign Up | Name, About, optional avatar, deterministic creation, first profile and Add Profile variants | Static gate passed 2026-08-26 — bounded Material form composition with one IME-aware scroll surface, inline primary action, 120 dp avatar, tonal pinned-app-bar scroll state, primary-emphasis labeled progress, both canonical identity paths, accessible photo feedback, and focus/IME dismissal before app-bar Back; renewed device visual acceptance pending |
| Avatar selection | Bundled choices, web-image catalog evidence, local image selection/capture path, processing limits | Static gate passed 2026-08-26 — Photo Picker, Files, 21 local web choices, deterministic URL mapping, remove/failure states, and EXIF-aware 512-pixel preparation. Fixed the shared decoder's rejection of every valid bounds-only result. Web Search/URL now uses native filled-tonal mode buttons, a 16 dp Done inset, modal-owned safe/IME insets, 24 dp result separation, scrollable content, and explicit search/clear actions. Image-import regressions and eight picker interaction/layout/local-restoration regressions compile; device execution and visual/TalkBack acceptance pending |
| Profile lifecycle | Add, switch, edit, re-onboard, sign out, remove, and isolated profile-owned state | Implemented — all identity, session, switching, editing, removal, and whole-app reset paths mutate isolated profile-owned state |
| First-login privacy choices | Optional per-profile analytics/logging after initial or Add Profile onboarding | Static gate passed 2026-08-26 — DiagnosticsPromptHost waits for RESUMED Chats; ID-bound immediate switches, dismissal-only seen state, restoration/retention/isolation tests; scoped iOS 4c25393 evidence; no real telemetry |
| Verified Nostr Address | Edit, deterministic verification state, verified seal, compact/read-only presentation | Implemented — validation, stored verification restoration, editable Profile UI, and compact identity presentation are covered |

## Chats hub and creation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Chats empty | Empty state, profile entry, New Message entry, relay recovery when relevant | Static gate passed 2026-08-26 — avatar-to-Settings, no default title, native icon-only New Message/Check Relays FAB in the active pane, hidden during search; edge-to-edge list with FAB content clearance; device acceptance pending |
| Chats populated | Direct/group/support/catalog rows, previews, dates, unread counts, delivery and attachment summaries | Static gate passed 2026-08-26 — ChatListRow/ChatListPresentation preserve all 77 fixtures/timestamps, semibold names and aligned avatars; native clickable rows use a 12 dp avatar gap while retaining the 16 dp artwork edge; mute/timer/membership icons follow the measured, ellipsized title independently of the timestamp; one exclusive invitation/failure/count/manual/none indicator with a filled invitation-plus badge, matching scalable footprints and retained unread state; ChatListRowTest/ChatsPolishTest compile; device acceptance pending |
| List organization | Pinned ordering, archived and left states, stable catalog/story order, search | Static gate passed 2026-08-26 — default Chats includes 72 non-archived rows including ended membership; Unread includes all unread non-archived rows; selected filled filter, shared preview/search projection, unchanged Archived/Left scopes; model tests pass |
| Row interactions | Open, pin/unpin, mark read/unread, archive/unarchive, leave/remove as allowed | Static gate passed 2026-08-26 — ChatContextMenuRow uses an 8 dp inset native rounded highlight and an 8 dp gap to its anchored Material menu, stable action order, live ID-bound eligibility, Back/outside/navigation/anchor dismissal, TalkBack actions, Undo, shared mute dialog, confirmations and sole-admin guard; no chat swipes or action sheet; UI regressions compiled, not run |
| Profile switching | Chats avatar routes to Settings; Settings owns account switching, unread badges, Add Profile, and isolated profile state | Implemented — selected tonal Material switcher in Settings, aggregate unread badges, Add Profile action, and independently owned people, chats, settings, relays, developer state, and drafts |
| New direct chat | People search/list, Person Profile step, Message action, deduplication, relay requirement | Implemented — search-first flat directory, shared no-results state, distinct tonal New Group action, existing-direct reuse, relay copying, and direct profile-relay recovery; visual-polish static gate passed |
| New group | Person selection, setup, name/photo/description, creator admin role, initial event, copied chat relays | Implemented — avatar `InputChip` selection, toggleable rows, bounded tonal setup fields, Photos/Files/Web states, member review, validation, members/admin, event, relay recovery, and route cleanup; visual-polish static gate passed |
| Person Profile | Identity, About, verified address, public key, shared groups, message/contact/block actions | Implemented — bounded expressive identity, tonal About and action groups, semantic verification/role treatment, pinned Message/recovery action, and one profile surface for creation and membership contexts; visual-polish static gate passed |

## Conversation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Shared conversation shell | Direct, group, support, invitation, ended, and developer-catalog conversations | Visual-polish static gate passed 2026-08-21 — small Material identity app bar, bounded keyed timeline/composer, tonal sticky day hierarchy, distinct notice/events, semantic selection, visible retry, and coordinated lifecycle panels; device acceptance pending |
| Timeline and clustering | Chronology, day boundaries, author and five-minute clusters, date markers, events, notices | Implemented — keyed lazy timeline, sticky date sections, deterministic ordering, cluster edges, group identity and centered information rows |
| Message states | Incoming/outgoing, sent/delivered/read/failed, retry, deleted/tombstoned, replies and missing fallbacks | Implemented — semantic bubbles, terminal time/state, retry, scoped deletion, reply resolution, action availability, and typed details |
| Text and links | Plain and multiline text, detected links, link preview, copy/search/read-aloud behavior | Visual-polish static gate passed 2026-08-21 — local first-HTTPS metadata, named suppression, tonal rich cards, copy/search, and received-text Android TextToSpeech with live progress |
| Images and video | One-to-seven media layout coverage, aspect clamping, viewer paging, capture and selection | Visual-polish static gate passed 2026-08-21 — count-derived grids, standard playback treatment, Photo Picker/external camera selection, ordered draft shelf, and safe-area exact-page HorizontalPager viewers |
| Files and rich content | Documents, contact, GIF showcase, deterministic labels/previews, unavailable media | Visual-polish static gate passed 2026-08-21 — system document handoff, semantic file/contact cards, bounded contact/GIF sheets, unavailable placeholders, and local-only fixture behavior |
| Reactions | Quick reactions, full emoji picker, counts, current-profile selection and replacement | Visual-polish static gate passed 2026-08-21 — profile-owned quick strip, selected extra, scroll-safe Material reactions, adaptive 48dp emoji grid, named configuration slots, swap/reset/apply, and semantic selected count chips |
| Replies and mentions | Reply context, deleted/missing fallback, group-member mention filtering | Implemented — action-owned reply draft/focus, cancellation and fallbacks; group draft suggestions derive from matching active members |
| Composer | Empty/one-line/multiline drafts, pull expansion, keyboard behavior, send clearing, link/media/file states | Visual-polish static gate passed 2026-08-21 — one tonal task region preserves authoritative drafts, bounded expansion, insets, link/reply/mention context, ordered attachments, and atomic clear/send |
| Attachments | Android-native photo/video picker, camera, document picker, preview/remove/viewer behavior | Visual-polish static gate passed 2026-08-21 without sensitive permissions — semantic Material sheets/shelf, visible named removal, preparation/error feedback, platform contracts, and exact-page shared viewer |
| Speech messages | Hold to record, review, deterministic waveform/sample, Transcribe, Voice/Text/Both formats, playback | Visual-polish static gate passed 2026-08-21 as the approved deterministic local simulation — code-native waveform, timer/review, editable transcript, format choice, and standard progress playback |
| Recipient speech actions | Read Aloud, Transcribe, Show/Hide Transcript, Copy Transcript, local-only provenance | Visual-polish static gate passed 2026-08-21 — view-local Transcribe, transcript reveal/copy and provenance, plus Android TextToSpeech with live progress and stop/shutdown lifecycle |
| Message actions | Long-press context, Reply, Forward, Copy, Select, Info, Delete, permissions and full action availability | Visual-polish static gate passed 2026-08-21 — Android combined-click hold/haptic, contextual message preview, semantic icon rows, named accessibility alternatives, conditional Copy/Retry, tonal typed details, and scoped confirmation |
| Selection and forwarding | Multi-select mode, target selection, ordered copies, media and keyboard behavior | Visual-polish static gate passed 2026-08-21 — explicit selected containment, live count, named compact actions, disabled-state explanation, native searchable checkboxes, 32-source/5-target limits, and source-ordered copies |
| Conversation search | Search text, sender, and attachment labels; current count and previous/next navigation | Visual-polish static gate passed 2026-08-21 — focused in-place search, named clear, newest-first projection, localized current-result semantics, text highlighting, explicit selected-result containment, compact older/newer controls, and draft-preserving close |
| Disappearing messages | Header/list indicators, duration setting event, deterministic status behavior | Implemented — list/header indicators, duration choice, and deterministic timeline events derive from chat state |
| Invitation and ended states | Read history before accept/decline, participation transition, leave/removed/ended composer behavior | Visual-polish static gate passed 2026-08-21 — exact outcomes and history remain intact; invitation, left, removed, blocked, and missing-relay states now share one accessible tonal lifecycle hierarchy with direct recovery |
| Relay recovery | Per-chat routing, final relay removal, send-disabled history-preserving state, add-to-recover | Implemented — independent editing, final-removal warning, history preservation, send block, Check Chat Relays recovery, and captured-default restore |

## Chat and group information

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Direct Chat Info | Peer identity, notification/search actions, shared content, chat relays, relationship state | Visual-polish static gate passed 2026-08-21 — uncontained identity, tonal icon actions, grouped shared/actions hierarchy, selected duration state, archive/direct leave, and independent relays |
| Group Info | Photo/name/description/member count, notifications, search, shared content, management, Leave Group | Visual-polish static gate passed 2026-08-21 — identity/member hierarchy, admin-gated edit/add, shared categories, advanced routes, lifecycle status, archive and destructive leave |
| Member management | Roles, admin promotion/demotion, add/remove people, last-admin protection, role-aware profile | Visual-polish static gate passed 2026-08-21 — native checkbox selection, separate Profile/Group Actions, confirmed role/removal mutations, actor gates, and last-admin safeguards |
| Group mutation | Edit metadata/photo, remove photo, deterministic events, leave/ended transitions | Visual-polish static gate passed 2026-08-21 — system Photo Picker, progress/error feedback, content-aligned tonal fields, pinned Save, typed events and authoritative lifecycle updates |
| Shared media | Media/files/links categories, chronological index, unified viewer and unavailable-page rules | Visual-polish static gate passed 2026-08-21 — counted disclosure rows, complete empty state, stable open three-column grid, exact selected-page shared pager, and semantically styled rich lists |
| Chat Relays | Normalize/deduplicate endpoints, independent per-chat editing, final removal and recovery | Visual-polish static gate passed 2026-08-21 — strict `wss://` behavior, tonal endpoint group, named removal, empty recovery, final-relay consequence, and captured-default restore |
| Groups in common | Active-membership-derived list shared by current and viewed profile | Implemented — derived from active shared membership and used by the authoritative profile screen |

## Settings and support

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Settings hub | Active profile header, native settings organization, destination summaries | Visual-polish static gate passed 2026-08-21 — prominent active identity, Share & Connect, adaptive Add/Switch Profile action, restrained tonal groups with 32 dp content-grid headings, compact icon-led destinations without redundant summaries, trailing Appearance state, conditional dependency explanation, typed routes, and semantic disabled/destructive treatment |
| Share & Connect | Local QR generation, public profile values, copy, Android Sharesheet, scanner/Profile Found state | Visual-polish static gate passed 2026-08-21 — open identity hierarchy, stable black-on-white QR, clear share/copy actions, grouped scanner recovery, and unchanged system surfaces |
| Profile | Edit name, About, avatar, Verified Nostr Address and deterministic verification | Visual-polish static gate passed 2026-08-21 — prominent avatar, source/progress/error states, content-aligned tonal fields, and pinned validated Save |
| Profile Keys | Private/Public Key presentation and safe copy/share boundaries | Visual-polish static gate passed 2026-08-21 — tonal key groups, private semantics cleared, encrypted export primary, and confirmed raw document export |
| Notifications | Master and detail preferences, preview behavior, system-setting handoff where applicable | Visual-polish static gate passed 2026-08-21 — grouped switches, explanatory disabled dependency, preview choice, and named Android-settings handoff; no delivery/permission |
| Appearance | System/light/dark choice with monochrome semantic schemes | Visual-polish static gate passed 2026-08-21 — native grouped radio semantics and immediate profile-owned System/Light/Dark theme |
| Privacy & Security | Device authentication, Recents snapshot privacy, Erase App Data entry and availability | Visual-polish static gate passed 2026-08-21 — grouped device protection, dependency recovery, semantic destructive entry, and unchanged `FLAG_SECURE` behavior |
| Diagnostics & Improvements | Typed consumer privacy destination, independent choices, summary, stored size, confirmed clear | Static gate passed 2026-08-26 — DiagnosticsImprovementsScreen and Privacy & Security link; Off/Analytics/Logs/On summary, native switches, platform-formatted size, confirmed clearing with logging unchanged; large dark RTL and prompt regressions compile |
| Data Usage | Media quality and auto-download preferences by media type and connectivity level | Visual-polish static gate passed 2026-08-21 — grouped media summaries, radio dialogs, reset explanation, and sent-quality hierarchy |
| Profile Relays | Endpoint list/details, Profile/Inbox/Chat Messages roles, availability, recovery, restore defaults | Visual-polish static gate passed 2026-08-21 — recovery callout, grouped endpoint/status details, role switches, disabled defaults state, pinned Add, and semantic removal |
| Support | Create/open one profile-owned White Noise Support conversation without duplicates | Visual-polish static gate passed 2026-08-21 — open purpose hierarchy, explicit relay recovery, and one availability-aware pinned Start Chat action |
| Donate | Deterministic donation surface and accepted copy | Visual-polish static gate passed 2026-08-21 — peer method tabs, purpose-specific stable QR semantics, tonal address surface, and one copy-only primary action |
| Developer Tools gate | Per-profile technical master gate and Debug Mode, independent of consent | Static gate passed 2026-08-26 — DeveloperToolsState no longer owns analytics/logging; master-off resets debug only and preserves diagnostic records and consent; existing console/key-package guards remain tested |
| Diagnostics | Persistent live event console, Test/Clear commands, empty state, sanitized copy | Visual-polish static gate passed 2026-08-21 — one adaptive tonal console, semantic Live state, responsive actions, durable empty state, and scoped summary copy |
| Diagnostic logs | Profile-owned retained records; clearing preserves records and preferences | Static gate passed 2026-08-26 — DiagnosticsState owns deterministic records; logging off retains them; consumer settings confirms clearing; Developer Tools inspects them independently of logging; no collection/transport/persistence |
| Key Packages | One current deterministic package and Publish New Key Package replacement | Visual-polish static gate passed 2026-08-21 — one contained current package and one pinned replacement task |
| Conversation debug | Profile-gated developer action and deterministic direct/group snapshots | Visual-polish static gate passed 2026-08-21 — grouped authoritative values, explicit copy rows, Diagnostics disclosure, and complete disabled/unavailable states |
| Sign Out | Active-profile flow, wipe default, typed profile-name gate, routing to switcher or Welcome | Visual-polish static gate passed 2026-08-21 — native full-height sheet, active identity, merged wipe choice, label-above exact-name gate, pinned error action, and named progress |
| Remove Profile | Remove another stored profile with exact-name confirmation and active-profile protection | Visual-polish static gate passed 2026-08-21 — grouped inactive identities, durable no-other-profiles state, focused identity confirmation, and no active-profile action |
| Erase App Data | Device-wide irreversible flow, stable three-word phrase gate, removes all local state | Visual-polish static gate passed 2026-08-21 — text-plus-symbol consequence, selectable phrase, label-above exact gate, pinned error action, and dismissal-safe progress |

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
sheet background/header geometry, Undo, destructive safeguards, Back and
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
passes: 117 unit tests, 126 compiled instrumentation tests, both APKs, zero lint
errors and one expected newer-version warning for the compatibility pin.
`WhiteNoiseMenuTest` plus extended `ChatsScreenTest`/`ConversationScreenTest`
cover native roles/selection, enabled/dismiss-before-dispatch behavior, themes,
edge anchors, small/large panes, 200% RTL text, constrained scrolling and IME
transitions. Existing Chats and privacy regressions remain compiled. No device
execution or visual acceptance is claimed. `docs/screens/app-menus.md` also
records the broader audit: baseline button typography/shape behavior and
standard global motion remain follow-up, not completed Expressive parity.

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
