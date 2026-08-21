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
| Native application shell | One activity, typed Compose navigation, monochrome Material 3 themes, edge-to-edge/adaptive root, Android Back ownership, offline deterministic boundary | Verified at the static gate on 2026-08-21 — shared typography/shape scales and component primitives, fully rounded 28 dp tonal form fields with 16 dp content-line alignment and 2 dp state rings, matched 56 dp task controls, 94 unit tests, 74 compiled UI tests, zero-issue lint, and both APKs; device visual acceptance pending |

## Onboarding and profiles

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Welcome | White Noise mark, first-launch entry, Sign In, and Sign Up | Implemented — initial and Add Profile Android routes preserve the centered mark and approved action hierarchy; visual-polish static gate passed |
| Sign In | Private Key entry, validation/progress states, scanner entry, first launch and Add Profile routing | Implemented — secure Material input, bounded form/action measure, tonal QR alternative, validation, cancelable compact progress, and canonical routing; visual-polish static gate passed |
| Private-key QR scan | Camera-based scan, cancel/Back, permission and unavailable states, result returned to Sign In | Implemented with approved Android difference — permissionless Google Code Scanner owns camera UI; wrong content, cancel, and unavailable states exist; device execution pending |
| Sign Up | Name, About, optional avatar, deterministic creation, first profile and Add Profile variants | Implemented — bounded Material form composition, both canonical identity paths, accessible photo feedback, and cancelable compact progress; visual-polish static gate passed |
| Avatar selection | Bundled choices, web-image catalog evidence, local image selection/capture path, processing limits | Implemented — Photo Picker, Files, 21 local web choices, shared search-empty and vector selected states, deterministic URL mapping, remove/failure states, and EXIF-aware 512-pixel preparation; device execution pending |
| Profile lifecycle | Add, switch, edit, re-onboard, sign out, remove, and isolated profile-owned state | Implemented — all identity, session, switching, editing, removal, and whole-app reset paths mutate isolated profile-owned state |
| Verified Nostr Address | Edit, deterministic verification state, verified seal, compact/read-only presentation | Implemented — validation, stored verification restoration, editable Profile UI, and compact identity presentation are covered |

## Chats hub and creation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Chats empty | Empty state, profile entry, New Message entry, relay recovery when relevant | Implemented — shared quiet empty/search composition, avatar-to-Settings entry, primary New Message FAB, and relay recovery action |
| Chats populated | Direct/group/support/catalog rows, previews, dates, unread counts, delivery and attachment summaries | Implemented — exact 77-row fixture, stable flat lazy list, capped counts, drafts, delivery and attachment summaries; pilot static gate passed |
| List organization | Pinned ordering, archived and left states, stable catalog/story order, search | Implemented — four app-bar menu scopes with visible/semantic selection, on-demand focused search, case/diacritic-insensitive matching, and tested stable partition ordering |
| Row interactions | Open, pin/unpin, mark read/unread, archive/unarchive, leave/remove as allowed | Implemented — authoritative mutations, mute durations, sole-admin protection, Android action sheets and confirmation dialogs |
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
| Data Usage | Media quality and auto-download preferences by media type and connectivity level | Visual-polish static gate passed 2026-08-21 — grouped media summaries, radio dialogs, reset explanation, and sent-quality hierarchy |
| Profile Relays | Endpoint list/details, Profile/Inbox/Chat Messages roles, availability, recovery, restore defaults | Visual-polish static gate passed 2026-08-21 — recovery callout, grouped endpoint/status details, role switches, disabled defaults state, pinned Add, and semantic removal |
| Support | Create/open one profile-owned White Noise Support conversation without duplicates | Visual-polish static gate passed 2026-08-21 — open purpose hierarchy, explicit relay recovery, and one availability-aware pinned Start Chat action |
| Donate | Deterministic donation surface and accepted copy | Visual-polish static gate passed 2026-08-21 — peer method tabs, purpose-specific stable QR semantics, tonal address surface, and one copy-only primary action |
| Developer Tools gate | Per-profile master gate with Debug Mode, Anonymous Telemetry, and Audit Logging | Visual-polish static gate passed 2026-08-21 — explicit warning callout, independent tonal technical groups, merged switches, and preserved child-reset/artifact rules |
| Diagnostics | Persistent live event console, Test/Clear commands, empty state, sanitized copy | Visual-polish static gate passed 2026-08-21 — one adaptive tonal console, semantic Live state, responsive actions, durable empty state, and scoped summary copy |
| Audit logs | Inline file inventory while enabled; clear contents while preserving file records | Visual-polish static gate passed 2026-08-21 — grouped path-free monospaced records, explanatory disabled clear state, and consequence-focused confirmation |
| Key Packages | One current deterministic package and Publish New Key Package replacement | Visual-polish static gate passed 2026-08-21 — one contained current package and one pinned replacement task |
| Conversation debug | Profile-gated developer action and deterministic direct/group snapshots | Visual-polish static gate passed 2026-08-21 — grouped authoritative values, explicit copy rows, Diagnostics disclosure, and complete disabled/unavailable states |
| Sign Out | Active-profile flow, wipe default, typed profile-name gate, routing to switcher or Welcome | Visual-polish static gate passed 2026-08-21 — native full-height sheet, active identity, merged wipe choice, label-above exact-name gate, pinned error action, and named progress |
| Remove Profile | Remove another stored profile with exact-name confirmation and active-profile protection | Visual-polish static gate passed 2026-08-21 — grouped inactive identities, durable no-other-profiles state, focused identity confirmation, and no active-profile action |
| Erase App Data | Device-wide irreversible flow, stable three-word phrase gate, removes all local state | Visual-polish static gate passed 2026-08-21 — text-plus-symbol consequence, selectable phrase, label-above exact gate, pinned error action, and dismissal-safe progress |

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
