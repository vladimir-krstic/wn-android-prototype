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
| Native application shell | One activity, typed Compose navigation, monochrome Material 3 themes, edge-to-edge/adaptive root, Android Back ownership, offline deterministic boundary | Verified — clean build, unit test, compiled UI test, and zero-issue lint on 2026-08-15 |

## Onboarding and profiles

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Welcome | White Noise mark, first-launch entry, Sign In, and Sign Up | Implemented — initial and Add Profile Android routes; static gate passed |
| Sign In | Private Key entry, validation/progress states, scanner entry, first launch and Add Profile routing | Implemented — secure Material input, validation, cancelable progress, and canonical routing; static gate passed |
| Private-key QR scan | Camera-based scan, cancel/Back, permission and unavailable states, result returned to Sign In | Implemented with approved Android difference — permissionless Google Code Scanner owns camera UI; wrong content, cancel, and unavailable states exist; device execution pending polish |
| Sign Up | Name, About, optional avatar, deterministic creation, first profile and Add Profile variants | Implemented — both canonical identity paths and cancelable progress; static gate passed |
| Avatar selection | Bundled choices, web-image catalog evidence, local image selection/capture path, processing limits | Implemented — Photo Picker, Files, 21 local web choices, deterministic URL mapping, remove/failure states, and EXIF-aware 512-pixel preparation; device execution pending polish |
| Profile lifecycle | Add, switch, edit, re-onboard, sign out, remove, and isolated profile-owned state | Implemented — all identity, session, switching, editing, removal, and whole-app reset paths mutate isolated profile-owned state |
| Verified Nostr Address | Edit, deterministic verification state, verified seal, compact/read-only presentation | Implemented — validation, stored verification restoration, editable Profile UI, and compact identity presentation are covered |

## Chats hub and creation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Chats empty | Empty state, profile entry, New Message entry, relay recovery when relevant | Implemented — scope-specific empty/search copy, profile entry and New Message FAB; relay-empty creation reports recovery copy |
| Chats populated | Direct/group/support/catalog rows, previews, dates, unread counts, delivery and attachment summaries | Implemented — exact 77-row fixture, stable lazy identity, capped counts, drafts, delivery and attachment summaries; static gate passed |
| List organization | Pinned ordering, archived and left states, stable catalog/story order, search | Implemented — four Material scopes, case/diacritic-insensitive search, and tested stable partition ordering |
| Row interactions | Open, pin/unpin, mark read/unread, archive/unarchive, leave/remove as allowed | Implemented — authoritative mutations, mute durations, sole-admin protection, Android action sheets and confirmation dialogs |
| Profile switching | Active-profile avatar, unread badges, switcher, isolated per-profile chats and settings | Implemented — Material switcher, aggregate unread badges, and independently owned people, chats, settings, relays, developer state, and drafts |
| New direct chat | People search/list, Person Profile step, Message action, deduplication, relay requirement | Implemented — searchable directory, existing-direct reuse, relay copying and missing-relay recovery state |
| New group | Person selection, setup, name/photo/description, creator admin role, initial event, copied chat relays | Implemented — stable multi-select, Photos/Files/Web avatar sources, validation, members/admin, event, and route cleanup |
| Person Profile | Identity, About, verified address, public key, shared groups, message/contact/block actions | Implemented — one profile surface serves creation and membership contexts with role-aware group actions and derived shared groups |

## Conversation

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Shared conversation shell | Direct, group, support, invitation, ended, and developer-catalog conversations | Implemented — one typed native destination and lifecycle bottom-state architecture; static gate passed |
| Timeline and clustering | Chronology, day boundaries, author and five-minute clusters, date markers, events, notices | Implemented — keyed lazy timeline, sticky date sections, deterministic ordering, cluster edges, group identity and centered information rows |
| Message states | Incoming/outgoing, sent/delivered/read/failed, retry, deleted/tombstoned, replies and missing fallbacks | Implemented — semantic bubbles, terminal time/state, retry, scoped deletion, reply resolution, action availability, and typed details |
| Text and links | Plain and multiline text, detected links, link preview, copy/search/read-aloud behavior | Implemented — local first-HTTPS metadata, suppression, atomic send, rich cards, copy/search, and received-text Android TextToSpeech |
| Images and video | One-to-seven media layout coverage, aspect clamping, viewer paging, capture and selection | Implemented — count-derived grids, overflow, Photo Picker/external camera selection, ordered draft shelf, and one read-only HorizontalPager viewer |
| Files and rich content | Documents, contact, GIF showcase, deterministic labels/previews, unavailable media | Implemented — Open Document, bundled PDF handoff, contact/GIF sheets, rich cards, unavailable placeholders, and local-only fixture behavior |
| Reactions | Quick reactions, full emoji picker, counts, current-profile selection and replacement | Implemented — profile-owned six-item strip, selected extra, normal/context removal rules, searchable picker, configuration swap/reset/apply, and count chips |
| Replies and mentions | Reply context, deleted/missing fallback, group-member mention filtering | Implemented — action-owned reply draft/focus, cancellation and fallbacks; group draft suggestions derive from matching active members |
| Composer | Empty/one-line/multiline drafts, pull expansion, keyboard behavior, send clearing, link/media/file states | Implemented — authoritative per-profile drafts, bounded expansion, insets, local link previews, ordered attachments, and atomic clear/send |
| Attachments | Android-native photo/video picker, camera, document picker, preview/remove/viewer behavior | Implemented without sensitive permissions — platform contracts, non-exported FileProvider, processing state, ordered removal, and shared viewer |
| Speech messages | Hold to record, review, deterministic waveform/sample, Transcribe, Voice/Text/Both formats, playback | Implemented as the approved deterministic local simulation with timer, review, editable transcript, format choice, and progress playback |
| Recipient speech actions | Read Aloud, Transcribe, Show/Hide Transcript, Copy Transcript, local-only provenance | Implemented — transcript reveal/copy, Transcribed provenance, and Android TextToSpeech with stop/shutdown lifecycle |
| Message actions | Long-press context, Reply, Forward, Copy, Select, Info, Delete, permissions and full action availability | Implemented — Android combined-click hold/haptic, named accessibility alternatives, conditional Copy/Retry, typed details, and scoped confirmation |
| Selection and forwarding | Multi-select mode, target selection, ordered copies, media and keyboard behavior | Implemented — visible toggles, zero-state controls, 32-source/5-target limits, searchable eligible targets, and source-ordered attachment-preserving copies |
| Conversation search | Search text, sender, and attachment labels; current count and previous/next navigation | Implemented — in-place top search, newest-first projection, older/newer controls, selected-result contrast, and draft-preserving close/clear behavior |
| Disappearing messages | Header/list indicators, duration setting event, deterministic status behavior | Implemented — list/header indicators, duration choice, and deterministic timeline events derive from chat state |
| Invitation and ended states | Read history before accept/decline, participation transition, leave/removed/ended composer behavior | Implemented — exact confirmation outcomes, group join event, history preservation and lifecycle bottom states; static gate passed |
| Relay recovery | Per-chat routing, final relay removal, send-disabled history-preserving state, add-to-recover | Implemented — independent editing, final-removal warning, history preservation, send block, Check Chat Relays recovery, and captured-default restore |

## Chat and group information

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Direct Chat Info | Peer identity, notification/search actions, shared content, chat relays, relationship state | Implemented — typed Material list, About/profile route, mute/disappearing/search, shared categories, archive/direct leave, and relays |
| Group Info | Photo/name/description/member count, notifications, search, shared content, management, Leave Group | Implemented — identity/member list, admin-aware editing/actions, shared content, advanced routes, archive and leave states |
| Member management | Roles, admin promotion/demotion, add/remove people, last-admin protection, role-aware profile | Implemented — complete profile reuse, Admin/Member title, confirmed group actions, add people, actor checks, and last-admin safeguards |
| Group mutation | Edit metadata/photo, remove photo, deterministic events, leave/ended transitions | Implemented — Photo Picker/monogram removal, name/description save, typed events and authoritative membership/list lifecycle updates |
| Shared media | Media/files/links categories, chronological index, unified viewer and unavailable-page rules | Implemented — nondeleted category projection, stable message-scoped identity, three-column media grid, shared pager, and rich rows |
| Chat Relays | Normalize/deduplicate endpoints, independent per-chat editing, final removal and recovery | Implemented — strict `wss://` normalization, isolated add/remove, consequence warning, send block/recovery, and captured-default restore |
| Groups in common | Active-membership-derived list shared by current and viewed profile | Implemented — derived from active shared membership and used by the authoritative profile screen |

## Settings and support

| Capability | Accepted parity scope | Android status |
| --- | --- | --- |
| Settings hub | Active profile header, native settings organization, destination summaries | Implemented — active summary, Share/Edit/Switch, native grouping, typed destinations, support/donate entries |
| Share & Connect | Local QR generation, public profile values, copy, Android Sharesheet, scanner/Profile Found state | Implemented — standards-compliant local QR, permissionless system scanner, fixture found state, copy and Sharesheet |
| Profile | Edit name, About, avatar, Verified Nostr Address and deterministic verification | Implemented — validated profile mutation plus Photo Picker, Files, pinned Web fixtures, and verified address |
| Profile Keys | Private/Public Key presentation and safe copy/share boundaries | Implemented — private semantics are cleared, raw export requires consequence confirmation, and encrypted export is the primary document action |
| Notifications | Master and detail preferences, preview behavior, system-setting handoff where applicable | Implemented — local/native-push and preview preferences plus Android notification-settings handoff; no delivery/permission |
| Appearance | System/light/dark choice with monochrome semantic schemes | Implemented — per-profile immediate System/Light/Dark theme plus authored English choices |
| Privacy & Security | Device authentication, Recents snapshot privacy, Erase App Data entry and availability | Implemented — device-security status/handoff, auth/auto-lock preferences, `FLAG_SECURE`, and three-word whole-app erase flow |
| Data Usage | Media quality and auto-download preferences by media type and connectivity level | Implemented — four media policies, reset, and Standard/High sent quality |
| Profile Relays | Endpoint list/details, Profile/Inbox/Chat Messages roles, availability, recovery, restore defaults | Implemented — seven fixtures, custom add/remove, read-only state, three roles, availability consequence and restore |
| Support | Create/open one profile-owned White Noise Support conversation without duplicates | Implemented — a dedicated Support destination explains the channel, opens the unique profile-owned chat, and routes relay recovery when creation is unavailable |
| Donate | Deterministic donation surface and accepted copy | Implemented — Lightning/Bitcoin scannable fixture codes and clipboard copy without wallet/payment |
| Developer Tools gate | Per-profile master gate with Debug Mode, Anonymous Telemetry, and Audit Logging | Implemented — independent profile state, locked technical sections, child reset and artifact preservation on disable |
| Diagnostics | Persistent live event console, Test/Clear commands, empty state, sanitized copy | Implemented — one adaptive console, visible Live state, persistent profile events, Test/Clear/No Events and scoped summary copy |
| Audit logs | Inline file inventory while enabled; clear contents while preserving file records | Implemented — two sanitized path-free records, visibility follows logging, confirmed zero-byte clear preserves metadata/state |
| Key Packages | One current deterministic package and Publish New Key Package replacement | Implemented — exactly one package with deterministic replacement and just-published state |
| Conversation debug | Profile-gated developer action and deterministic direct/group snapshots | Implemented — accepted Fiatjaf/Support action gate, authoritative lifecycle/role/relay/push facts and content-free summary |
| Sign Out | Active-profile flow, wipe default, typed profile-name gate, routing to switcher or Welcome | Implemented — retain-or-wipe consequences, exact-name gate, deterministic progress, remaining-profile selection or Welcome |
| Remove Profile | Remove another stored profile with exact-name confirmation and active-profile protection | Implemented — Manage Profiles excludes active identity and atomically removes stored/session state after confirmation |
| Erase App Data | Device-wide irreversible flow, stable three-word phrase gate, removes all local state | Implemented — strict deterministic phrase, complete in-memory reset and Welcome routing |

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
