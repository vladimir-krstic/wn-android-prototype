# Conversation fixture parity audit — 2026-09-01

## Outcome

The Android chat list already contained all 77 pinned rows in the accepted
order, but the initial conversation implementation did **not** contain all of
their iOS content. Most developer-catalog timelines and the two largest
retained stories had been represented by short samples. That gap is fixed.

The Android prototype now preserves the complete pinned scenario sequences,
payload branches, and deterministic story histories from
`wn-ios-prototype@0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`. Presentation and
system-owned actions remain Android-native. Automated parity checks guard the
source order and meaningful payload data so the catalog cannot silently
collapse back to nonempty samples.

User visual acceptance is separate from this engineering verification.

## Sources and method

The comparison used the pinned, read-only iOS source rather than a newer
working-tree snapshot:

- `WhiteNoisePrototype/App/PrototypeChatFixtures.swift`
- `WhiteNoisePrototype/Screens/Chats/ChatListFixtures.swift`
- `WhiteNoisePrototype/App/PrototypeMediaLayout.swift`
- `WhiteNoisePrototype/Screens/Conversation/PrototypeChatSharedViews.swift`
- `WhiteNoisePrototype/Screens/Conversation/PrototypeMessageBubble.swift`
- `docs/screens/chat-catalog.md`

Android evidence was traced through `ChatFixtures`, `ConversationFixtures`,
composer models, conversation projection, message rendering, Chat Info shared
content, navigation, unit tests, and the physical-device Compose suite.

The audit checked five distinct layers:

1. chat row count, order, title, membership, roster, role, and draft state;
2. every specialized timeline entry ID in source order;
3. content payloads: text, authorship, replies, deletion, delivery, reactions,
   media dimensions/duration/availability, files, links, contacts, and voice;
4. rendering and interaction: links, mentions, source replies, media viewer,
   rich cards, voice, context actions, selection, and search;
5. visual geometry at default and 200% type, including reactions, timestamps,
   delivery state, composer clearance, RTL, and expanded interaction targets.

## Inventory result

### Chats list

- 77 total Marmota chats remain present.
- 72 are non-archived and 5 are archived.
- The developer catalog remains ahead of retained stories in the pinned iOS
  order.
- Specialized chats use their exact scenario timelines; ordinary retained
  rows keep the deterministic `<chat-id>-seed` policy, with the additional
  terminal entry required by Left/Removed membership.
- Mina Park intentionally remains the empty direct-conversation case.

### Specialized timelines

| Catalog conversation | Complete entries | Restored/verified branches |
| --- | ---: | --- |
| Direct - Text & Delivery | 16 | short, outgoing, clusters, multiline, wrap, emoji, Markdown, raw URL, Sending, Sent, Failed |
| Direct - Dates & Scrolling | 15 | sparse days, long day, section/pinned-header handoff |
| Direct - Replies & Deletion | 11 | text reply, attachment reply, both deletion directions, deleted source, missing source |
| Direct - Reactions & Actions | 21 | singleton/current-profile/counts, seven supported emoji, overflow, text/media/file action combinations |
| Media - Single Photos & Video | 10 | landscape, square, portrait, panorama clamp, tall clamp, low-resolution safeguard, two video ratios, unavailable photo/video |
| Media - Gallery Layouts | 8 | 2/3/4/5/6/7/8 items, mixed photo/video, caption and `+N` overflow |
| Media - Viewer & Actions | 7 | cross-date paging, zoom/share/save/forward/go-to-message, video, deleted/unavailable exclusion |
| Media - Files & Rich Content | 11 | PDF, DOCX, XLSX, ZIP, TXT, unavailable file, three link states, GIF, contact |
| Voice Messages | 8 | four caption/payload pairs, both directions, short/long, transcript/Both |
| Group - Messages & Mentions | 14 | group events, author clusters, four mention states, two cross-author reply levels |
| Group - Identity Colors | 10 | creation plus all nine pinned public-key/color cases |
| Group - Events & Roles | 22 | creation/membership/identity/admin/description/removal/timer events and admin role state |

The lifecycle catalogs are also exact: direct/group invitations, direct/group
left, removed, blocked, missing relays, archived, support notice, three
disappearing-message states, ordinary-member permissions, and sole-admin
recovery.

### Composer matrix

All 12 composer conversations now preserve the pinned source message and
state: text, multiline text, raw link, rich link preview, one photo, four-photo
album, ordered mixed media, file, GIF, contact, reply, and mention. This
includes exact draft copy, suppressed-link URL, reply target, ordered
attachments, dimensions, duration, and contact identity.

### Retained stories

- **Maya Chen:** all 31 direct-history messages, including Markdown/links,
  photo and galleries, five file formats, GIF/contact, voice/transcript,
  reactions, replies, and unavailable content.
- **Weekend Walks:** all 42 entries—28 messages and 14 group events—including
  creation, member changes, title/photo/description/admin changes, media,
  file, contact, reactions, replies, departure, addition, and removal.
- **Fiatjaf:** all 8 messages, including the portable-identity story, reply,
  reaction, and final five-photo gallery.

## Product defects found and corrected

### Missing content rather than missing rows

The old `nonempty timeline` coverage gave a false sense of completeness. It
proved that a row opened, but not that its iOS scenario list survived. The
fixture implementation was expanded and `ConversationFixtureParityTest` now
asserts every specialized ID in order, the retained histories, all composer
states, and the ordinary seed policy.

### Inline text

Markdown was displayed as raw punctuation. Bold, emphasis, named links, raw
URLs, and inline mentions now render as visible inline content. Links and
mentions are actionable. Copy, search, reply excerpts, accessibility
descriptions, and read-aloud operate on the visible plain text rather than the
source markup.

### Media and rich payloads

Single-media layout now uses the pinned pixel dimensions and clamp policy.
Gallery grids preserve the pinned 2–8-item layouts, mixed video indicator, and
overflow. Viewer projection excludes deleted and unavailable frames while
retaining exact source-frame selection.

File cards now keep the full filename, type, deterministic byte size,
availability, and opening behavior. The valid contact carries its person ID
and opens that profile. Unavailable video retains its poster but is visibly
gated. Shared Content consumes the same availability rules.

### Replies, reactions, delivery, and voice

Available reply quotes now jump to and briefly highlight the source. Deleted
and missing targets retain the unavailable fallback. Reaction summaries show
at most four real types plus one `+N`; Message Details remains exhaustive.

Emoji-only reaction pills are 31×23 dp, counted/overflow pills grow, gaps are
3 dp, and pills overlap the bubble by 9 dp while retaining 48 dp targets.
Timestamps keep a measured 2 dp bubble gap whether reactions exist or not,
including at 200% type. Incoming metadata follows the start/left bubble edge
and outgoing metadata follows the end/right edge in LTR, mirrored in RTL.
Sent, Sending, and Not Delivered share that geometry; only failed delivery is
red.

The 82-second voice case now displays a valid `1:22` duration and playback
uses remaining time. All four pinned voice payload branches are present.

## Intentional Android adaptations

These are not parity omissions:

- Material 3 surfaces, Android Back/predictive Back ownership, native
  accessibility actions, Android Sharesheet, Photo Picker, camera, and file
  contracts replace iOS presentation mechanics while retaining outcomes.
- Fixture dates use deterministic day ordinals/labels rather than live iOS
  `Date` values, so screenshots and tests remain stable.
- The prototype ships a bounded local resource set. DOCX/XLSX/ZIP/TXT fixture
  labels and sizes are exact, but their deterministic open action is backed by
  bundled local PDF resources; no backend or network fetch was introduced.
- Media assets use the accepted Android bundled equivalents while preserving
  scenario identity, labels, dimensions, duration, count, and availability.
- Latest explicit user direction governs failed-delivery wording/interaction
  and reaction/timestamp geometry where it supersedes older iOS catalog copy.

## Verification

- `testDebugUnitTest`: 158 tests passed, including the new exhaustive fixture,
  Markdown, duration, media-size, projection, and rich-payload regressions.
- `ConversationScreenTest`: all 58 cases passed on physical Pixel 8a
  `53221JEKB07374` after rebuilding and reinstalling both APKs.
- Device coverage includes source-preserving context actions, reply swipe and
  source jump, selection/forwarding, exhaustive reactions, timestamp geometry,
  large type, RTL, media viewer, rich contact/file cards, voice, search,
  composer fixtures, and newest-message clearance.
- `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, and `git diff
  --check` are final handoff gates for this batch.

## Remaining boundary

No pinned fixture scenario is knowingly absent after this audit. The remaining
open gate is user hands-on/visual acceptance and any later product change to
the pinned iOS baseline; neither should be represented as missing engineering
coverage in the current Android prototype.
