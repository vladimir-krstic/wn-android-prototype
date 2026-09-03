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

## 2026-08-31 cross-app hardening evidence

The repository-wide hardening pass completed the clean host gate with 139 unit
tests, both APKs, zero lint errors, and six intentionally retained dependency
availability warnings. The complete physical Pixel 8a / Android 17 run passed
all 168 instrumentation tests with no failures, errors, or skips. Direct
inspection covered selected high-impact chat/composer/search/info/contact/voice,
Chats/creation, Settings/privacy, dark, 200%-type, Arabic RTL, and forced
610/838 dp-wide states. These results upgrade engineering verification only;
they do not mark any screen visually accepted by the user or claim a complete
manual TalkBack/system-surface pass. Exact findings and commits are recorded in
`docs/codebase-hardening-audit.md`.

## Application foundation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Native application shell | One activity, typed Compose navigation, monochrome Material 3 themes, edge-to-edge/adaptive root, Android Back ownership, offline deterministic boundary | Verified at the static gate on 2026-08-27 — shared typography/shape scales and component primitives, fully rounded 28 dp tonal form fields with 16 dp content-line alignment and 2 dp state rings, matched 56 dp task controls with primary-emphasis loading states, app-wide unhandled-background-tap focus clearing, 94 unit tests, 80 compiled UI tests, zero-issue lint, and both APKs; device interaction acceptance pending |

## Onboarding and profiles

2026-09-04 production B04: C016–C018 are implemented and host-verified.
Profile banner/URL selection and retry, full-size image viewing, optional
Lightning validation with atomic profile saves and draft-only name suggestions
extend the accepted Profile and Person Profile surfaces. See
[`profile-media-and-lightning.md`](../screens/profile-media-and-lightning.md).
The clean gate passed 244 unit tests, both APKs and zero lint errors; seven new
UI cases compile. Device/visual acceptance remains pending.

2026-09-04 production B03: C013, C014, C019 and C021 are implemented and
host-verified. Search result/retry states, private nickname/notes, canonical
created-chat opening and profile-to-group invitation/admin selection extend the
accepted people flows. See [`people-discovery-and-private-details.md`](../screens/people-discovery-and-private-details.md).
The clean gate passed 231 unit tests, both APKs and zero lint errors; seven new
UI cases compile. Device/visual acceptance remains pending.

2026-09-04 production B02: C007–C010 are implemented and host-verified.
Local key availability, temporary reveal/export, optional connection-information
cleanup, staged wipe, owner guards, retries and partial reports are covered in
[`keys-and-profile-exit.md`](../screens/keys-and-profile-exit.md). The approved
checked wipe default and exact-name confirmation remain. The clean gate passed
216 unit tests and both APKs with zero lint errors; five new UI cases compile.
The same 14 pre-existing lint warnings remain; device/visual acceptance pending.

2026-09-04 production B01: C001, C003, C004, C005 and C011 are implemented
within the deterministic boundary. `AccessModels`, `AppViewModel`,
`OnboardingValidation`, `AccessUi` and onboarding navigation now cover
key/link shape and type errors, Amber identity/proof outcomes, consent-gated
setup recovery and partial results, retained-profile re-entry and startup
retry. Profile-owned chats/drafts/settings survive retained re-entry; stale
callbacks cannot activate a profile. Amber profiles cannot expose a local
secret/export. The clean README gate passed 202 unit tests, both APKs and zero
lint errors; six new UI cases compile. Existing lint warnings remain, and no
new device or visual acceptance is claimed. See
[`access-and-recovery.md`](../screens/access-and-recovery.md).

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
| Timeline and clustering | Chronology, day boundaries, author and five-minute clusters, date markers, events, notices | Refined 2026-09-01 — the keyed lazy timeline now keeps every day boundary as ordinary centered system text and derives a separate semi-transparent current-day capsule only while that boundary is outside the viewport; reaching the inline boundary removes the capsule instead of converting the transcript text into a pill. The capsule uses `surfaceDim` at 82% alpha, Material's consistently dimmer semantic surface, so it remains distinct when overlapping an incoming `surfaceContainerHigh` bubble. Host unit, Kotlin, lint, build, and instrumentation-compilation evidence covers the transition; device/visual verification of this refinement remains unclaimed until explicitly requested. The earlier Pixel 8a suite still covers deterministic ordering, cluster edges, group identity, and composer-aware end position, while exhaustive fixture regression guards the complete specialized source-order entries plus all 31 Maya Chen and 42 Weekend Walks entries. User visual acceptance remains separate |
| Message states | Incoming/outgoing, sent/delivered/read/failed, retry, deleted/tombstoned, replies and missing fallbacks | Verified 2026-09-01 — tail-free bubbles retain the established insets and cluster rhythm. Sent, Sending, and Not Delivered use the same directional 12 dp timestamp edge, 2 dp bubble gap, compact status footprint, and appropriate neutral/error colors; scoped deletion, retry, action eligibility, resolved/deleted/missing replies, typed details, and source navigation pass the Pixel 8a suite. User visual acceptance remains separate |
| Text and links | Plain and multiline text, detected links, link preview, copy/search/read-aloud behavior | Read Aloud eligibility corrected 2026-09-03 — sent and received authored text without voice attachments, including replies and captions, now share the focused action and active Stop Reading/progress behavior. 186 unit tests, lint, app assembly, and instrumentation-test APK compilation pass; device speech verification remains pending. Rich-canvas static gate passed 2026-09-01 — all pinned text scenarios remain present. Markdown emphasis and named/raw links render without source punctuation, links are interactive, and copy/search/read-aloud consume visible plain text. Link artwork now fills the shared 256 dp canvas above a 6 dp-inset text stack inside the concentric rich surface. Host unit/build/instrumentation compilation passes; current-build device and visual verification remain unclaimed until explicitly requested. |
| Images and video | One-to-seven media layout coverage, proportional single-image sizing, viewer paging, capture and selection | Gallery cleanup static gate passed 2026-09-03 — both galleries now clip the pager and each page, disable edge stretching, and share downward dismissal with short-pull return and priority for horizontal paging, zoom, and scrubbing. Inactive videos no longer create rendering surfaces. Host unit/build/lint and gesture-test compilation pass; device reproduction of the reported rendering bleed remains pending. Sent videos now use Media3 1.11.0 inside the gallery with native Material play/pause, seek buttons, position slider, elapsed/total time, mute, speed, buffering, and error/replay controls. Only the settled foreground page owns playback; page exit, dismissal, and backgrounding release the player, with position and settings restored paused on return. Bottom controls clear Share/Forward and adapt to short windows. Compiled `MediaViewerVideoTest` covers controls, seeking, Back, and lifecycle/page ownership; device playback and visual acceptance remain pending. single photos/video thumbnails now fill a 256 dp media height, derive width from decoded dimensions, and crop only centered horizontal overflow beyond the 256 dp canvas or available parent width. Full vertical content remains visible, with the tiny-source safeguard retained; metadata remains a fallback. Album crops and GIF frames remain. Forward uses the pinned Signal broad outlined right-turn arrow in the media viewer, message action menu, and selection toolbar; Share retains its standard icon. Native targets, mirroring, and actions remain unchanged. Unit tests, lint, app assembly, and instrumentation-test APK compilation pass; current-build device inspection is pending. Earlier rich-canvas evidence — all pinned single, gallery, mixed, viewer, deleted, and unavailable scenarios preserve dimensions, duration, frame identity, gallery overflow, and chat-wide viewer exclusions. A lone photo/video now owns the bubble canvas width so mixed text wraps below it without lateral dead space; albums share the fixed canvas and clip once around the complete 2 dp-gapped grid. Host width derivation and compiled geometry regressions cover portrait, landscape, attachment-only, and mixed cases; platform picker/camera ownership remains unchanged. Device and visual verification remain unclaimed until requested. |
| Files and rich content | Documents, contact, GIF showcase, deterministic labels/previews, unavailable media | Rich-canvas static gate passed 2026-09-01 — all six file types/states, three link-preview branches, GIF, valid contact, voice card, and reply surface now share a 6 dp four-side bubble inset, 10 dp concentric child radius, 6 dp section rhythm, and 256 dp canvas where applicable. Rich cards use 6 dp internal padding; files use a bare semantic glyph rather than a nested circle. Host unit/build/instrumentation compilation passes, while opening, contact routing, availability gates, and bubble-owned holds remain covered. Current-build device and visual verification remain unclaimed until requested. |
| Reactions | Quick reactions, full emoji picker, counts, current-profile selection and replacement | Verified 2026-09-01 by physical Pixel 8a execution — emoji-only pills are exactly 31×23 dp, counted and `+N` pills grow horizontally, visible neighbors keep 3 dp gaps, and the rail overlaps the bubble by 9 dp while retaining 48 dp targets. Summary capacity is four real types plus `+N`; width may reduce it further. Directional timestamp insets and the measured 2 dp bubble gap pass at default and 200% type. The focused quick strip keeps 48 dp targets around centered 40 dp circular state layers, equal 4 dp outer insets, 4 dp neighboring gaps, fixed 28 dp Signal atlas artwork, and a neutral circular 24 dp horizontal-ellipsis More control. Its standard Material menu-group color and Level 2 shadow now match the command surface, with explicit 8 dp viewport gutters preventing shadow clipping. More Reactions opens the complete pinned iOS catalog as a searchable, sectioned Compose grid with deterministic recents, 48 dp circular emoji targets, named Clear, and a pinned scroll-synchronized icon rail that includes Configure Reactions. The exact 20-page artwork atlas and generated metadata from Signal Android `879651dc47a7b18b67e7aea52a25197875024680` are bundled offline with license/notice, while parsing and rendering remain app-owned Kotlin with no Signal runtime or network behavior. Asset-resolution coverage proves every accepted catalog emoji maps to artwork. Current-build idle and held quick-strip frames confirm the smaller Signal visuals, all-reaction circular state layer, matched surfaces, and complete shadows; pill/full-picker artwork remains centered and free of font-baseline clipping. Full-catalog, IME search, and category-jump frames were inspected; focused spacing, exhaustive Message Details, selection semantics, configuration, and picker behavior pass the relevant conversation coverage. User visual acceptance remains separate |
| Replies and mentions | Reply context, deleted/missing fallback, group-member mention filtering | Verified 2026-09-01 — all direct and cross-author group reply scenarios are restored. Available quotes jump to and briefly highlight their source; deleted/missing targets retain the fallback. Composer/message quote geometry, cancellation, swipe reply, group mention filtering, inline mention styling, and person routing pass unit/device coverage; user visual acceptance remains separate |
| Composer | Empty/one-line/multiline drafts, pull expansion, keyboard behavior, send clearing, link/media/file states | Mention-highlight correction implemented 2026-08-31 — complete member mentions now use the adaptive medium-neutral `outlineVariant`/`onSurface` pair with the same shared 4 dp rounded glyph-run renderer as search, replacing the overly dark primary inversion. The previously inspected transparent host, 48 dp Add action, low-emphasis capsule outline, edge-to-edge timeline, compact/expanded/IME behavior, text budgets, attached context and atomic send clearing remain; renewed mention-state inspection and user visual acceptance are pending |
| Attachments | Android-native photo/video picker, camera, document picker, preview/remove/viewer behavior | Import-quality correction passed the host gate 2026-09-03 — camera/picked photos retain up to 4096 px, lossless PNG/alpha or JPEG 95, and EXIF orientation; avatar imports remain 512 px. `DeviceMediaImage` decodes viewport-sized bitmaps off the main thread. 183 unit tests, lint, both APK builds, and image-import instrumentation compilation pass; device execution remains pending. Draft-gallery static gate passed 2026-09-03 — downward dismissal now cancels staged exclusions, with short-pull return, strict page/thumbnail clipping, and no edge stretching. The cancellation/paging regression compiles; current-build device verification remains pending. `DraftMediaViewer` removes its 16 dp image margin to match the sent viewer's full-width, aspect-fit treatment; unit tests, lint, app assembly, and instrumentation-test APK compilation pass. Review actions and staging remain unchanged. Photo/album/GIF/file/contact/link/reply removal shares one 48 dp target with a 20 dp circle, 12 dp X, equal 6 dp corner inset and inner-only state layer; 72 dp utility cards retain contact names and suffix-preserving filenames; draft review now uses 48 dp thumbnails in adjacent 56 dp targets (8 dp visible gaps), a 1 dp selected-only ring, and a 72 dp rail; the updated geometry assertions compile and the 183-test host gate passes. It hides a single-item rail, and anchors its 48/22 dp inclusion control to the fitted image with the visible check 6 dp inside both bottom-end edges, a user-requested 50% increase. The existing inclusion-geometry regression is updated and compiled. The earlier inspected picker/menu behavior and WN-ANDROID-0104 permission-safe camera repair remain; current-build device inspection and user visual acceptance are pending |
| Speech messages | Hold to record, review, deterministic waveform/sample, Transcribe, Voice/Text/Both formats, playback | Playback-track cleanup passed the host gate 2026-09-03 — voice progress omits the fixed end dot in `TimelineMessageContent.kt`; current-build device inspection remains pending. Verified 2026-09-01 — the complete eight-entry pinned voice timeline is restored, including 7 s, 18 s, 1:22, and Both/transcript cases. The deterministic local recording/review flow, waveform, hold threshold, playback, remaining-duration formatting, transcription, editing, format selection, and single-result submission pass the physical Pixel 8a suite; user visual acceptance remains separate |
| Recipient speech actions | Read Aloud, Transcribe, Show/Hide Transcript, Copy Transcript, local-only provenance | Read Aloud expanded 2026-09-03 to sent and received text without voice; installed-engine discovery is repaired with the required TTS service query. Model regressions cover direction, captions/replies, empty/deleted/voice exclusions, readiness, and active Stop Reading. A sent-text start/progress/stop UI case compiles; device execution remains pending. Playback-track cleanup passed the host gate 2026-09-03 — Read Aloud omits the fixed end dot in `TimelineMessageContent.kt`; current-build device inspection remains pending. Pixel 8a focused-action inspection passed 2026-08-31 — received text/voice commands now live conditionally in the source-preserving action overlay; ordinary bubbles retain only playback, active read-aloud progress, or a deliberately revealed view-local transcript. Transcribe/reveal/hide/copy provenance and Android TextToSpeech stop/shutdown behavior remain; model and focused UI tests pass |
| Message actions | Long-press context, Reply, Forward, Copy, Select, Info, Delete, permissions and full action availability | Verified 2026-09-01 on the physical Pixel 8a — swipe Reply uses a weight-600 Google rounded glyph whose 48 dp indicator is anchored to the actual incoming/outgoing bubble, not the row edge; its fade, 10 dp travel, 1.0→1.2 growth, threshold pulse, resisted overdrag, and spring return share the bubble's gesture state in LTR/RTL. Held-return geometry regressions and a current-build mid-swipe visual inspection pass, as does the complete 62-case conversation suite. Focused actions retain the real bubble between a quick-reaction rail with circular, evenly inset Material press feedback and the policy-ordered command surface, with safe-bound shifting, tall-preview scaling, a 24 dp conversation blur, and no compounded Dialog dimming. The 2026-09-03 contrast correction uses an 88% white-equivalent `surfaceContainerLowest` veil over the blur so the background remains white with faint, softened content showing through and returns focused timestamps, Sent fill, and Sending progress to the ordinary chat `outline` role; failed delivery remains semantic error. Unit tests, lint, app assembly, and instrumentation-test APK compilation pass; current-build device inspection is pending. The quick rail and command group share the exact standard menu color and Level 2 shadow, while 8 dp top/bottom viewport gutters let both shadows finish without clipping. The command surface reuses the same non-popup `DropdownMenuGroup`/position-aware `DropdownMenuItem` renderer as Chats filtering, replacing square custom press bands with native rounded state layers, group clipping, 48 dp targets, 20 dp icons, standard metrics/colors/elevation and semantic-error Delete. The bubble now arbitrates touch at the Initial pointer pass: holding any nested reply quote, media/link/file/contact card, mention, or voice control opens message actions and suppresses that child release, while an ordinary tap remains exclusively owned by the rich child. Host and instrumentation-test compilation cover nested quote, media, link, contact, and voice-control holds; current-build device execution is intentionally deferred until requested. Conditional Retry/Copy/speech actions, named accessibility alternatives, typed details and scoped confirmation remain; user visual acceptance remains separate |
| Selection and forwarding | Multi-select mode, target selection, ordered copies, media and keyboard behavior | Pixel 8a conversation-surface inspection passed 2026-08-31 — selection uses one stable 48 dp leading Checkbox column for both message directions, whole-row toggle semantics, a restrained row state layer, live count and named Delete/Forward actions without changing bubble geometry. Forwarding retains explicit limits/order, the shared 48 dp search, segmented destination group, count-bearing media action, measured final-row clearance, high non-full-screen state, exact-frame validation and single-Photo normalization; focused selection/forwarding tests pass |
| Conversation search | Search text, sender, and attachment labels; current count and previous/next navigation | Pixel 8a inspection passed 2026-08-31 — exact shared 48 dp Chats field and Android Back behavior, focused in-place entry only from Chat/Group Info, IME-visible previous/next/count controls, newest-first projection, full-contrast result messages with only nonmatches subdued, platform-cyan/black 4 dp rounded exact-glyph highlights across visible text/author/attachment/link labels, current-only spoken position, and draft-preserving close; user visual acceptance remains separate |
| Disappearing messages | Header/list indicators, duration setting event, deterministic status behavior | Implemented — list/header indicators, duration choice, and deterministic timeline events derive from chat state |
| Invitation and ended states | Read history before accept/decline, participation transition, leave/removed/ended composer behavior | Visual-polish static gate passed 2026-08-21 — exact outcomes and history remain intact; invitation, left, removed, blocked, and missing-relay states now share one accessible tonal lifecycle hierarchy with direct recovery |
| Relay recovery | Per-chat routing, final relay removal, send-disabled history-preserving state, add-to-recover | Implemented — independent editing, final-removal warning, history preservation, send block, Check Chat Relays recovery, and captured-default restore |

Latest host-only message-action refinement (2026-09-01): the focused overlay
no longer lets its transparent full-width alignment column intercept backdrop
taps. A compiled Compose regression targets blank space beside the lifted
message and requires one tap to close; only the visible reaction rail,
message-plus-metadata bounds, and command group consume taps. Current-build
device execution remains intentionally deferred until requested.

Latest host-only interaction-containment audit (2026-09-01): message holds,
reaction pills, reply quotes, draft thumbnails, selected-person removal, and
attachment cards now keep state layers inside their visible clipped shapes
while preserving the larger semantic targets. Grouped Material rows and the
explicitly accepted selection-row state remain whole-row. Geometry regressions
for the message bubble, thumbnail, and selected-person target/visual split are
compiled; current-build device execution remains deferred until requested.

## Chat and group information

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Direct Chat Info | Peer identity, notification/search actions, shared content, chat relays, relationship state | Host gate passed 2026-09-03 — Google-reference gray canvas, back-only Material bar, existing profile avatar scale, verified address/full-key copy capsule, wide tonal quick actions, white segmented Shared in Chat then Chat Actions in iOS order; Relays/Developer Tools/Archive/Leave remain functional. Copy/debug callback regressions compile; current-build device acceptance pending |
| Group Info | Photo/name/description/member count, notifications, search, shared content, management, Leave Group | Host gate passed 2026-09-03 — shared profile avatar scale and tonal quick actions, description before count, white segmented Shared in Chat → Advanced → lazy Members → active-admin management → Archive/Leave hierarchy; existing gates and confirmations preserved. Section order, member navigation, and dark 200% RTL ended-group regressions compile; current-build device acceptance pending |
| Member management | Roles, admin promotion/demotion, add/remove people, last-admin protection, role-aware profile | Visual-polish static gate passed 2026-08-21 — native checkbox selection, separate Profile/Group Actions, confirmed role/removal mutations, actor gates, and last-admin safeguards |
| Group mutation | Edit metadata/photo, remove photo, deterministic events, leave/ended transitions | Visual-polish static gate passed 2026-08-21 — system Photo Picker, progress/error feedback, content-aligned tonal fields, pinned Save, typed events and authoritative lifecycle updates |
| Shared media | Media/files/links categories, chronological index, unified viewer and unavailable-page rules | Viewer-background correction passed the host gate 2026-09-03 — top/bottom chrome now matches the opaque gallery canvas, preventing full-height images showing through controls. Picked photos share the existing viewer and use the higher-quality in-memory import. Current-build visual inspection remains pending. Playback-track cleanup passed the host gate 2026-09-03 — `VideoPlaybackSlider` removes the fixed end dot while keeping Media3 progress/seek state and Material thumb/semantics; 179 unit tests, lint, app assembly, and instrumentation-test APK compilation pass. Current-build device inspection remains pending. Shared video viewer updated 2026-09-03 — both entry points now use the embedded Media3 player and its standard playback/seek controls (WN-ANDROID-0123); host build/test compilation passes, device playback remains pending. Earlier forward-sheet evidence from 2026-08-31 — Shared Content and the conversation use one chronological frame-level projection with stable message/attachment/image keys, exact album-tile selection, deleted/unavailable filtering, complete viewer actions, the shared polished media-forward sheet, and typed Go to Message; counted disclosure rows, empty state and rich lists remain; device inspection pending |
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

The 2026-08-31 hardening gate supersedes the earlier compile-only evidence:
the current APK participates in the 139-unit and 168-of-168 physical Pixel 8a
test baseline. Direct inspection of the current build covers compact/expanded
and IME composer states, attachment and format menus, recording, pre/post-
transcription review, all three formats, dark appearance, 200% type, Arabic
RTL, and 610/838 dp-wide windows. User visual acceptance remains pending.

Subsequent user-approved polish gives Add a clear 10 dp popup gap and keeps the
format popup 2 dp above its target (10 dp from its inset visible pill), with
Material's native menu shadow; matches idle waveform artwork to the 24 dp Add
icon footprint, and replaces the flashing
sparse live waveform with a dense 24 dp deterministic trailing window. Stop is
now a red icon-only action; Send uses an upward arrow; Review uses a filled
Play/Pause circle and light-gray bubble-led Transcribe; contact sharing opens
an expanded searchable sheet with a white-equivalent grouped directory. Static
and current Pixel 8a evidence are recorded in the same implementation, tests,
and hardening audit; user visual acceptance remains separate.

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
