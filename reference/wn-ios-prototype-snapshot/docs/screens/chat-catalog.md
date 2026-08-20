# Chat scenario catalog

## Purpose

Provide one deterministic, developer-facing catalog for every supported chat
timeline renderer, state, recovery boundary, permission boundary, and native
message action. The catalog appears before retained story and marketing chats
and does not depend on those legacy fixtures for coverage.

Coverage is branch-based rather than a full Cartesian product. Every item
below has one stable fixture identifier and one intentional home.

Developer catalog chat titles use a regular spaced hyphen. Visible labeled
fixture messages and row previews use `Scenario ID: Description`; em dashes
and centered dots are not used for catalog separators.

## Catalog order

- [ ] **Direct - Text & Delivery** - text styles, clustering, and delivery
  states.
- [ ] **Direct - Dates & Scrolling** - inline day boundaries, sparse-day
  sequences, long-day sections, and pinned-header handoff.
- [ ] **Direct - Replies & Deletion** - reply resolution, navigation, and both
  deletion directions.
- [ ] **Direct - Reactions & Actions** - reaction treatments and every native
  context-menu capability combination.
- [ ] **Direct - New Chat & Draft** - direct inception with no sent messages
  and a persisted draft.
- [ ] **Composer - Text** through **Composer - Mention** - deterministic
  unsent text, multiline, raw-link, rich-link, media, file, GIF, contact,
  reply, and mention states.
- [ ] **Media - Single Photos & Video** - intrinsic photo/video ratios,
  captions, orientation, low-resolution safeguards, and unavailable media.
- [ ] **Media - Gallery Layouts** - deterministic album counts, mixed media,
  captions, and overflow.
- [ ] **Media - Viewer & Actions** - chat-wide paging, zoom, video playback,
  sharing/export, forwarding, and source-message navigation.
- [ ] **Media - Files & Rich Content** - files, deterministic link-preview
  renderers, GIF, and contact sharing.
- [ ] **Voice Messages** - incoming and outgoing playback presentations.
- [ ] **Group - Messages & Mentions** - group authorship, clusters, mentions,
  and cross-author replies.
- [ ] **Group - Identity Colors** - every stable public-key author color paired
  with its one-letter no-photo avatar treatment.
- [ ] **Group - Events & Roles** - complete group event history and admin
  management prerequisites.
- [ ] **Group - Member Permissions** - ordinary-member information and action
  boundaries.
- [ ] **Group - Sole Admin** - protected leave and promotion recovery.
- [ ] **Direct - Disappearing** - active 1 Day timer without mute.
- [ ] **Direct - Disappearing & Muted** - adjacent mute and active 1 Week
  timer indicators.
- [ ] **Group - Disappearing** - active 4 Weeks timer combined with the group
  member-count subtitle.
- [ ] **Direct - Left** - ended direct chat with direct-specific copy.
- [ ] **Group - Left** - voluntarily ended group membership.
- [ ] **Group - Removed** - group membership ended by another admin.
- [ ] **Direct - Blocked** - retained history and unblock recovery.
- [ ] **Direct - Missing Relays** - retained history and per-chat relay
  recovery.
- [ ] **Direct - Archived** - active chat in Archived scope.
- [ ] **Support - Timeline Notice** - the special guidance notice.

Only **Direct - Text & Delivery** starts pinned. Catalog rows remain in this
order; retained story chats are unpinned and follow the catalog.

## Scenario matrix

Checkboxes remain open until visual acceptance. **Implemented; review pending**
means the deterministic fixture and model branch exist and are guarded by unit
coverage, but have not yet been accepted in a simulator pass.

### Direct text, delivery, and clustering

| Done | Scenario ID | Catalog chat | Fixture/message ID | Expected result | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- | --- |
| [ ] | `TXT-01` | Direct - Text & Delivery | `TXT-01` | Incoming short text | React, Reply, Forward, Copy, Select, Info, Delete | Implemented; review pending |
| [ ] | `TXT-02` | Direct - Text & Delivery | `TXT-02` | Outgoing short text | React, Reply, Forward, Copy, Select, Info, Delete | Implemented; review pending |
| [ ] | `TXT-03`–`TXT-05` | Direct - Text & Delivery | same IDs | Same-author cluster start, middle, end | Standard message actions | Implemented; review pending |
| [ ] | `TXT-06` | Direct - Text & Delivery | `TXT-06` | Multiline text | Standard message actions | Implemented; review pending |
| [ ] | `TXT-07` | Direct - Text & Delivery | `TXT-07` | Long wrapping text | Standard message actions | Implemented; review pending |
| [ ] | `TXT-08` | Direct - Text & Delivery | `TXT-08` | Emoji-only text | Standard message actions | Implemented; review pending |
| [ ] | `TXT-09` | Direct - Text & Delivery | `TXT-09` | Markdown emphasis and inline link | Open link; standard actions | Implemented; review pending |
| [ ] | `TXT-10` | Direct - Text & Delivery | `TXT-10` | Raw URL | Open URL; standard actions | Implemented; review pending |
| [ ] | `DLV-01` | Direct - Text & Delivery | `DLV-01` | Sending outgoing state | Standard outgoing actions | Implemented; review pending |
| [ ] | `DLV-02` | Direct - Text & Delivery | `DLV-02` | Sent outgoing state | Standard outgoing actions | Implemented; review pending |
| [ ] | `DLV-03` | Direct - Text & Delivery | `DLV-03` | Primary-label outlined warning icon with **Not delivered, hold for options** status, left-aligned like the outgoing timestamp at the bubble's inner corner inset; no timestamp | Touch and hold → Retry Send; standard outgoing actions | Implemented; review pending |
| [ ] | `CLUSTER-01` | Direct - Text & Delivery | `CLUSTER-01` | Author change starts a cluster | None | Implemented; review pending |
| [ ] | `CLUSTER-02` | Direct - Text & Delivery | `CLUSTER-02` | More than five minutes starts a cluster | None | Implemented; review pending |
| [ ] | `CLUSTER-03` | Direct - Text & Delivery | `CLUSTER-03` | New day starts a cluster | None | Implemented; review pending |

### Dates and scrolling

| Done | Scenario ID | Catalog chat | Fixture/message ID | Expected result | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- | --- |
| [ ] | `DATE-01` | Direct - Dates & Scrolling | `DATE-01` | A date at least six calendar months old uses a locale-aware medium date including the year | None | Implemented; review pending |
| [ ] | `DATE-02` | Direct - Dates & Scrolling | `DATE-02` | A recent date older than Yesterday uses locale-aware abbreviated weekday, month, and day | None | Implemented; review pending |
| [ ] | `DATE-03`–`DATE-07` | Direct - Dates & Scrolling | same IDs | Consecutive one-message days allow multiple inline date headers to remain visible together | Scroll | Implemented; review pending |
| [ ] | `DATE-08`–`DATE-15` | Direct - Dates & Scrolling | same IDs | A long Today section spans more than one viewport and keeps Today pinned after its inline header scrolls away | Scroll | Implemented; review pending |
| [ ] | `DATE-PIN-01` | Direct - Dates & Scrolling | `DATE-03`–`DATE-07` | The most recent header above the viewport is the only pinned regular-glass pill while later date headers remain inline | Scroll | Implemented; review pending |
| [ ] | `DATE-PIN-02` | Direct - Dates & Scrolling | `DATE-07`–`DATE-08` | The approaching next header pushes out and replaces the pinned header at the top | Scroll | Implemented; review pending |

### Replies, deletion, reactions, and message actions

| Done | Scenario ID | Catalog chat | Fixture/message ID | Expected result | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- | --- |
| [ ] | `RPL-01` | Direct - Replies & Deletion | `RPL-01` → `RPL-01-source` | Outgoing reply to incoming text | Tap quote to highlight source | Implemented; review pending |
| [ ] | `RPL-02` | Direct - Replies & Deletion | `RPL-02` → `RPL-02-source` | Incoming reply to outgoing attachment | Tap quote to highlight source | Implemented; review pending |
| [ ] | `RPL-03` | Direct - Replies & Deletion | `RPL-03` → `DEL-02` | Deleted target says **Message deleted** | Tap quote to highlight deleted source | Implemented; review pending |
| [ ] | `RPL-04` | Direct - Replies & Deletion | `RPL-04` → `RPL-missing` | Missing target says **Message unavailable** | No target navigation | Implemented; review pending |
| [ ] | `DEL-01` | Direct - Replies & Deletion | `DEL-01-caption`, `DEL-01` | **You deleted this message.** | No action presentation | Implemented; review pending |
| [ ] | `DEL-02` | Direct - Replies & Deletion | `DEL-02-caption`, `DEL-02` | **This message was deleted.** | No action presentation | Implemented; review pending |
| [ ] | `RCT-01` | Direct - Reactions & Actions | `RCT-01` | One reaction from another person; no count | Select chip | Implemented; review pending |
| [ ] | `RCT-02` | Direct - Reactions & Actions | `RCT-02` | One current-profile reaction with the opaque selected treatment | Selected chip tap is a no-op | Implemented; review pending |
| [ ] | `RCT-03` | Direct - Reactions & Actions | `RCT-03` | Three people use the same reaction without current-profile participation | Select chip | Implemented; review pending |
| [ ] | `RCT-04` | Direct - Reactions & Actions | `RCT-04` | Three people use the same reaction including current-profile participation | Selected chip tap is a no-op | Implemented; review pending |
| [ ] | `RCT-05` | Direct - Reactions & Actions | `RCT-05` | Multiple reaction types mix single/count and current/other participation | Select another chip to replace | Implemented; review pending |
| [ ] | `RCT-06`–`RCT-12` | Direct - Reactions & Actions | same IDs | ❤ 😀 👍 👎 🤣 🔥 🦫 all represented as renderer coverage beyond quick defaults | Display and select chip | Implemented; review pending |
| [ ] | `RCT-13` | Direct - Reactions & Actions | `RCT-13` | A narrow outgoing bubble proves three-point reaction gaps, opposite-edge timestamp placement, and adaptive `+N` overflow | Select visible chips | Implemented; review pending |
| [ ] | `ACT-01` | Direct - Reactions & Actions | `ACT-01` | Incoming text includes Delete for Me | Long press | Implemented; review pending |
| [ ] | `ACT-02` | Direct - Reactions & Actions | `ACT-02` | Outgoing text includes both deletion scopes | Long press; confirm Delete | Implemented; review pending |
| [ ] | `ACT-03` | Direct - Reactions & Actions | `ACT-03-caption`, `ACT-03` | Incoming attachment-only includes Delete, not Copy | Long press | Implemented; review pending |
| [ ] | `ACT-04` | Direct - Reactions & Actions | `ACT-04-caption`, `ACT-04` | Outgoing attachment-only includes Delete, not Copy | Long press | Implemented; review pending |
| [ ] | `ACT-05` | Direct - Reactions & Actions | `ACT-05-caption`, `ACT-05` | Available file can be forwarded with its attachment | Long press; Forward | Implemented; review pending |

### Media, files, rich content, and voice

| Done | Scenario ID | Catalog chat | Fixture/message ID | Expected result | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- | --- |
| [ ] | `MED-01`–`MED-03` | Media - Single Photos & Video | same IDs | Incoming/outgoing photo-only and captioned single media use natural constrained sizing | Open exact item in chat-wide viewer | Implemented; review pending |
| [ ] | `MED-SINGLE-04`–`MED-SINGLE-06` | Media - Single Photos & Video | same IDs | Panorama-clamped, tall-clamped, and low-resolution sizing | Open exact item | Implemented; review pending |
| [ ] | `MED-11`, `MED-SINGLE-08` | Media - Single Photos & Video | same IDs | Available landscape and portrait video with play and duration overlays | Open exact item; native playback | Implemented; review pending |
| [ ] | `MED-12`–`MED-13` | Media - Single Photos & Video | same IDs | Unavailable video/photo remain visible and fully noninteractive | None | Implemented; review pending |
| [ ] | `MED-04`–`MED-09` | Media - Gallery Layouts | same IDs | Counts 2–7 use Signal-informed five-tile layouts and 2-point gutters | Open selected attachment | Implemented; review pending |
| [ ] | `MED-GALLERY-08` | Media - Gallery Layouts | same ID | Larger overflow shows the hidden count on tile five | Open at attachment five | Implemented; review pending |
| [ ] | `MED-10` | Media - Gallery Layouts | `MED-10` | Mixed photo/video album; overflow replaces competing video overlays | Open exact available item | Implemented; review pending |
| [ ] | `MED-VIEW-01`–`MED-VIEW-06` | Media - Viewer & Actions | same IDs | Multiple senders/dates prove chat-wide paging, zoom, autoplay, Share, Forward, Save, and Go to Message | Viewer actions | Implemented; review pending |
| [ ] | `MED-VIEW-07` | Media - Viewer & Actions | same ID | Unavailable source remains in transcript/grid but is excluded from paging | None | Implemented; review pending |
| [ ] | `FILE-01`–`FILE-05` | Media - Files & Rich Content | same IDs | Available PDF, DOCX, XLSX, ZIP, TXT rows | Open preview | Implemented; review pending |
| [ ] | `FILE-06` | Media - Files & Rich Content | `FILE-06` | Unavailable file | No preview; unavailable value | Implemented; review pending |
| [ ] | `LINK-01` | Media - Files & Rich Content | `LINK-01` | Link preview with image | Open link | Implemented; review pending |
| [ ] | `LINK-02` | Media - Files & Rich Content | `LINK-02` | Link preview without image | Open link | Implemented; review pending |
| [ ] | `LINK-03` | Media - Files & Rich Content | `LINK-03` | Invalid destination | No open action; unavailable value | Implemented; review pending |
| [ ] | `RICH-01` | Media - Files & Rich Content | `RICH-01` | GIF | Standard message actions | Implemented; review pending |
| [ ] | `RICH-05` | Media - Files & Rich Content | `RICH-05` | Contact selected from **+ > Contact** renders one valid profile card | Choose Contact; open profile | Implemented; review pending |
| [ ] | `VOICE-01` | Voice Messages | `VOICE-01-caption`, `VOICE-01` | Incoming short voice bubble | Play/pause, progress | Implemented; review pending |
| [ ] | `VOICE-02` | Voice Messages | `VOICE-02-caption`, `VOICE-02` | Outgoing short voice bubble | Play/pause, progress | Implemented; review pending |
| [ ] | `VOICE-03` | Voice Messages | `VOICE-03-caption`, `VOICE-03` | Duration over one minute | Play/pause, elapsed/remaining | Implemented; review pending |
| [ ] | `VOICE-04` | Voice Messages | existing composer recording state machine | Record → review → listen → discard/send | Native composer controls | Implemented; review pending |

### Composer states

| Done | Scenario ID | Catalog chat | Initial composer | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- |
| [ ] | `CMP-TEXT` | Composer - Text | Single-line text | Edit or send | Implemented; review pending |
| [ ] | `CMP-MULTILINE` | Composer - Multiline | Four-line text | Edit, scroll after ten lines, or send | Implemented; review pending |
| [ ] | `CMP-LINK` | Composer - Link | Raw `https` URL with preview already removed | Edit or send text-only link | Implemented; review pending |
| [ ] | `CMP-LINK-PREVIEW` | Composer - Link Preview | Multiline text and native rich preview | Remove preview or send rich link | Implemented; review pending |
| [ ] | `CMP-PHOTO` | Composer - Photo | One captionless photo | Remove, add caption, or send | Implemented; review pending |
| [ ] | `CMP-PHOTO-ALBUM` | Composer - Photo Album | Four photos and caption | Remove any item, edit caption, or send | Implemented; review pending |
| [ ] | `CMP-MIXED` | Composer - Mixed Media | Two photos, one video, and caption | Remove any item, edit caption, or send | Implemented; review pending |
| [ ] | `CMP-FILE` | Composer - File | PDF and caption | Remove, edit caption, or send | Implemented; review pending |
| [ ] | `CMP-GIF` | Composer - GIF | One captionless GIF | Remove, add caption, or send | Implemented; review pending |
| [ ] | `CMP-CONTACT` | Composer - Contact | Contact and caption | Remove, edit caption, or send | Implemented; review pending |
| [ ] | `CMP-REPLY` | Composer - Reply | Quote and text | Cancel reply, edit, or send | Implemented; review pending |
| [ ] | `CMP-MENTION` | Composer - Mention | Group mention text | Edit or send | Implemented; review pending |

All composer details and external comparison evidence are governed by
[Conversation composer states](conversation-composer-states.md).

### Group messages, events, and permissions

| Done | Scenario ID | Catalog chat | Fixture/message ID | Expected result | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- | --- |
| [ ] | `GRP-01`–`GRP-05` | Group - Messages & Mentions | same IDs | Avatars/names, clusters, author switch, outgoing interruption, time/day breaks | Standard message actions | Implemented; review pending |
| [ ] | `MENTION-01` | Group - Messages & Mentions | `MENTION-01` | Mention current profile | Open profile | Implemented; review pending |
| [ ] | `MENTION-02` | Group - Messages & Mentions | `MENTION-02` | Mention another member | Open profile | Implemented; review pending |
| [ ] | `MENTION-03` | Group - Messages & Mentions | `MENTION-03` | Multiple valid mentions | Open either profile | Implemented; review pending |
| [ ] | `MENTION-04` | Group - Messages & Mentions | `MENTION-04` | Unmatched `@` remains plain | Standard text actions | Implemented; review pending |
| [ ] | `GRP-RPL-01`–`GRP-RPL-02` | Group - Messages & Mentions | same IDs + source | Cross-author replies | Tap quote to highlight source | Implemented; review pending |
| [ ] | `COLOR-01`–`COLOR-09` | Group - Identity Colors | same IDs | All nine stable public-key identity colors; each example pairs the author-name color with its adaptive one-letter, white-monogram avatar surface | Open each profile | Implemented; review pending |
| [ ] | `EVT-01` | Group - Events & Roles | `EVT-01` | You created the group | None | Implemented; review pending |
| [ ] | `EVT-02` | Group - Messages & Mentions | `EVT-02` | Maya created the group | None | Implemented; review pending |
| [ ] | `EVT-03` | Group - Events & Roles | `EVT-03` | You added one person | None | Implemented; review pending |
| [ ] | `EVT-04` | Group - Member Permissions | `EVT-04-member` | Maya added you and another person | None | Implemented; review pending |
| [ ] | `EVT-05` | Group - Events & Roles | `EVT-05` | Another member joined | None | Implemented; review pending |
| [ ] | `EVT-06` | Group - Messages & Mentions | `EVT-06` | You joined the group | None | Implemented; review pending |
| [ ] | `EVT-07` | Group - Events & Roles | `EVT-07` | Another member left | None | Implemented; review pending |
| [ ] | `EVT-08` | Group - Events & Roles | `EVT-08` | You removed a member | None | Implemented; review pending |
| [ ] | `EVT-09` | Group - Events & Roles | `EVT-09` | Another admin removed another member | None | Implemented; review pending |
| [ ] | `EVT-10` | Group - Removed | `EVT-10` | Maya removed You | Delete ended group | Implemented; review pending |
| [ ] | `EVT-11` | Group - Events & Roles | `EVT-11` | You made another member admin | None | Implemented; review pending |
| [ ] | `EVT-12` | Group - Events & Roles | `EVT-12` | Another admin made you admin | None | Implemented; review pending |
| [ ] | `EVT-13` | Group - Events & Roles | `EVT-13` | You removed another admin | None | Implemented; review pending |
| [ ] | `EVT-14` | Group - Events & Roles | `EVT-14` | Another admin removed you as admin | None | Implemented; review pending |
| [ ] | `EVT-15` | Group - Events & Roles | `EVT-15` | Group name changed | None | Implemented; review pending |
| [ ] | `EVT-16` | Group - Events & Roles | `EVT-16` | Group photo changed | None | Implemented; review pending |
| [ ] | `EVT-17` | Group - Events & Roles | `EVT-17` | Group photo removed | None | Implemented; review pending |
| [ ] | `EVT-18` | Group - Events & Roles | `EVT-18` | Description changed | None | Implemented; review pending |
| [ ] | `EVT-19` | Group - Events & Roles | `EVT-19` | Description removed | None | Implemented; review pending |
| [ ] | `EVT-20`–`EVT-23` | Group - Events & Roles | same IDs | Disappearing messages: 1 Day, 1 Week, 4 Weeks, Off | Change setting | Implemented; review pending |
| [ ] | `ROLE-01` | Group - Events & Roles | `ROLE-01` + admin roster state | All management controls visible | Native group-info actions | Implemented; review pending |
| [ ] | `ROLE-02` | Group - Member Permissions | `ROLE-02` + member roster state | Admin controls hidden; member controls remain | Native group-info actions | Implemented; review pending |
| [ ] | `ROLE-03` | Group - Sole Admin | `ROLE-03` + sole-admin roster state | Leave blocked until promotion | Alert, Promote, Leave | Implemented; review pending |

### Invitations, ended, recovery, and list states

| Done | Scenario ID | Catalog chat | Fixture/message ID | Expected result | Supported action | Implementation status |
| --- | --- | --- | --- | --- | --- | --- |
| [ ] | `STATE-01` | Direct - New Chat & Draft | `STATE-01`, draft row | Inception event, no messages, prefilled draft; clearing restores new-chat state | Edit/clear draft | Implemented; review pending |
| [ ] | `STATE-02` | Direct - Left | `STATE-02-message`, `STATE-02` | History ends **You left the chat.**; chat-specific read-only copy | Delete ended chat | Implemented; review pending |
| [ ] | `STATE-03` | Group - Left | `STATE-03-message`, `STATE-03` | History ends **You left the group.**; profile absent from roster | Delete ended group | Implemented; review pending |
| [ ] | `STATE-04` | Group - Removed | `STATE-04-message`, `EVT-10` | History ends **Maya Chen removed you from the group.** | Delete ended group | Implemented; review pending |
| [ ] | `STATE-05` | Direct - Blocked | `STATE-05` | History retained; composer says **Unblock to Send Messages** | Unblock | Implemented; review pending |
| [ ] | `STATE-06` | Direct - Missing Relays | `STATE-06` | Empty chat relays; history retained | Check Chat Relays; restore/add relay | Implemented; review pending |
| [ ] | `STATE-07` | Direct - Archived | `STATE-07` | Active chat in Archived scope | Unarchive | Implemented; review pending |
| [ ] | `STATE-08` | Support - Timeline Notice | `STATE-08` notice | Guidance notice renders distinctly and is excluded from row activity | Standard support composer | Implemented; review pending |
| [ ] | `STATE-09` | Direct - Invitation | `STATE-09` | Received direct message is readable before acceptance; Liquid Glass invitation actions replace the composer | Decline or Accept | Implemented; review pending |
| [ ] | `STATE-10` | Group - Invitation | `STATE-10A`, `STATE-10B` | Received group messages and participant identity are readable before membership; Liquid Glass invitation actions replace the composer | Decline or Accept | Implemented; review pending |
| [ ] | `IND-01` | Direct - Disappearing | row and principal header | Timer icon in Chats; timer icon plus **1d** below the direct-chat title | Change timer in Chat Info | Implemented; review pending |
| [ ] | `IND-02` | Direct - Disappearing & Muted | row and principal header | Mute then timer icons in Chats; timer icon plus **1w** below the direct-chat title | Unmute or change timer | Implemented; review pending |
| [ ] | `IND-03` | Group - Disappearing | row and principal header | Timer icon in Chats; **members · [timer] 4w** below the group title | Change timer in Group Info | Implemented; review pending |
| [ ] | `LIST-01` | Direct - Text & Delivery | row `catalog-direct-text` | Pinned | Unpin | Implemented; review pending |
| [ ] | `LIST-02` | Direct - Replies & Deletion | row `catalog-direct-replies` | Unread count | Read | Implemented; review pending |
| [ ] | `LIST-03` | Direct - Reactions & Actions | row `catalog-direct-reactions` | Marked unread | Read | Implemented; review pending |
| [ ] | `LIST-04` | Media - Single Photos & Video | row `catalog-media-single` | Muted | Unmute | Implementation pending |
| [ ] | `LIST-05` | Direct - New Chat & Draft | row `catalog-direct-new-draft` | Draft preview | Open/edit draft | Implemented; review pending |
| [ ] | `LIST-06` | Direct - Text & Delivery | row `catalog-direct-text` | Failed delivery indicator | Retry | Implemented; review pending |
| [ ] | `LIST-07` | Direct - Archived | row `catalog-direct-archived` | Archived | Unarchive | Implemented; review pending |
| [ ] | `LIST-08` | Direct/Group - Left/Removed | four ended rows | Left scope and ended-membership copy | Delete | Implemented; review pending |

Textless targets use a preceding caption message containing the scenario ID;
the target remains structurally authentic. Malformed references without an
intentional product fallback are outside catalog scope.

## Native behavior

- Message actions follow [Message actions](message-actions.md): a bounded
  Signal-informed focused presentation contains native buttons for reactions,
  Reply, Forward, Copy when text exists, Select, Info, and Delete.
- Deleted messages have no action presentation. Failed outgoing messages replace the
  timestamp with **Not delivered, hold for options** and expose **Retry Send**
  first in the focused presentation.
- Chat list actions remain the native UIKit swipe actions: Read/Unread,
  Pin/Unpin, Archive/Unarchive, Mute/Unmute, Leave, and Delete where permitted.
- Mute values remain **1 Hour**, **8 Hours**, **1 Day**, **1 Week**, and
  **Always**.
- Active disappearing-message timers use the compact presentation defined by
  [Disappearing-message indicators](disappearing-message-indicators.md): Chats
  shows a timer symbol beside mute when both apply, direct headers show
  **[timer] 1d/1w/4w**, and group headers append
  **· [timer] 1d/1w/4w** after the member count.
- Group information remains the action owner for editing group identity, adding
  people, changing roles, removing members, and leaving.
- Composer, attachment-menu, camera-sheet, and audio-recording behavior remain
  governed by [Shared conversation](conversation-shared.md) and
  [Conversation composer states](conversation-composer-states.md).

### Interaction acceptance matrix

| Done | Action ID | Catalog prerequisite | Expected native behavior | Implementation status |
| --- | --- | --- | --- | --- |
| [ ] | `MSG-ACT-01` | `RCT-06`–`RCT-12` | Quick reactions offer ❤ 🤘 🔥 😂 🦫 🚀 plus the selected nonfavorite when needed, then More Reactions; the selected quick emoji removes current-profile participation | Implemented; review pending |
| [ ] | `MSG-ACT-02` | Any nondeleted message | Reply opens the composer quote; sending preserves the target ID | Implemented; review pending |
| [ ] | `MSG-ACT-03` | `ACT-01`, `ACT-02` | Copy appears only when text exists | Implemented; review pending |
| [ ] | `MSG-ACT-04` | `ACT-01`–`ACT-05` | Forward opens eligible chat selection and preserves source order and attachments | Implemented; review pending |
| [ ] | `MSG-ACT-05` | `ACT-01`–`ACT-04` | Delete for Me handles any nondeleted message; Delete for Everyone is limited to eligible outgoing selection | Implemented; review pending |
| [ ] | `MSG-ACT-06` | `DLV-03` | Touch and hold exposes Retry Send; retry changes failed delivery to sent | Implemented; review pending |
| [ ] | `MSG-ACT-07` | `RPL-01`–`RPL-03` | Quote tap scrolls to the target without adding a temporary highlight | Implemented; review pending |
| [ ] | `MSG-ACT-08` | `MENTION-01`–`MENTION-03` | Mention tap opens the matching profile | Implemented; review pending |
| [ ] | `MSG-ACT-09` | `MED-01`–`MED-VIEW-07` | Media opens at the exact tapped attachment and pages chronologically across all available chat photos/videos | Implemented; review pending |
| [ ] | `MSG-ACT-10` | `MED-11` | Available video opens native playback; unavailable video does not | Implemented; review pending |
| [ ] | `MSG-ACT-11` | `FILE-01`–`FILE-06` | Available file opens native preview; unavailable file does not | Implemented; review pending |
| [ ] | `MSG-ACT-12` | `LINK-01`–`LINK-03` | Valid destination opens; invalid destination exposes no open action | Implemented; review pending |
| [ ] | `MSG-ACT-13` | `RICH-05` | Contact opens the referenced profile | Implemented; review pending |
| [ ] | `MSG-ACT-14` | Any nondeleted message | Select enters multi-message selection with count, selected-message Delete, Forward, and Close controls | Implemented; review pending |
| [ ] | `MSG-ACT-15` | Any nondeleted message | Info pushes Message Details with direction, time, sender, delivery state, and recipients | Implemented; review pending |
| [ ] | `MSG-ACT-16` | `VOICE-01`–`VOICE-03` | Play/pause, progress, waveform, and elapsed/remaining time stay synchronized | Implemented; review pending |
| [ ] | `CHAT-ACT-01` | `LIST-02`, `LIST-03` | Read/Unread updates count and marked-unread state | Implemented; review pending |
| [ ] | `CHAT-ACT-02` | `LIST-01` | Pin/Unpin updates ordering; legacy fixtures remain below catalog | Implemented; review pending |
| [ ] | `CHAT-ACT-03` | `LIST-07` | Archive/Unarchive moves between All and Archived scopes | Implemented; review pending |
| [ ] | `CHAT-ACT-04` | `LIST-04` | Mute menu offers 1 Hour, 8 Hours, 1 Day, 1 Week, Always; Unmute clears it | Implemented; review pending |
| [ ] | `CHAT-ACT-05` | Active group | Leave appends a terminal event and moves the row to Left | Implemented; review pending |
| [ ] | `CHAT-ACT-06` | `STATE-02`–`STATE-04` | Ended chat can be deleted from Left; active chat is not offered this action | Implemented; review pending |
| [ ] | `CHAT-ACT-07` | Any populated catalog chat | Search finds text, author, and attachment labels, opens on the newest result, and navigates matches in place | Implemented; review pending |
| [ ] | `CHAT-ACT-08` | `IND-01`–`IND-03` | Changing or turning off disappearing messages updates or removes the Chats-row and conversation-header timer presentation from the shared chat state | Implemented; review pending |
| [ ] | `DIRECT-ACT-01` | Any active direct catalog chat | Chat Info exposes contact, disappearing messages, chat relays, archive, and block controls | Implemented; review pending |
| [ ] | `DIRECT-ACT-02` | `STATE-05` | Unblock restores the composer without losing timeline history | Implemented; review pending |
| [ ] | `DIRECT-ACT-03` | `STATE-06` | Add or Restore Defaults re-enables the composer | Implemented; review pending |
| [ ] | `INVITE-ACT-01` | `STATE-09`, `STATE-10` | Accept preserves history, activates the chat, and restores the composer; group acceptance also appends **You joined the group.** | Implemented; review pending |
| [ ] | `INVITE-ACT-02` | `STATE-09`, `STATE-10` | Decline confirms removal, Cancel preserves the invitation, and confirmation removes the chat and returns to Chats | Implemented; review pending |
| [ ] | `GROUP-ACT-01` | `ROLE-01` | Admin can edit name, photo, and description; removal events are distinct | Implemented; review pending |
| [ ] | `GROUP-ACT-02` | `ROLE-01` | Admin can add people, grant/revoke admin, and remove members | Implemented; review pending |
| [ ] | `GROUP-ACT-03` | `ROLE-02` | Member keeps messages/search/shared content/mute/archive/leave; admin-only controls are absent | Implemented; review pending |
| [ ] | `GROUP-ACT-04` | `ROLE-03` | Sole-admin leave shows the existing alert; promotion makes leave valid | Implemented; review pending |
| [ ] | `COMPOSER-ACT-01` | `CMP-TEXT`–`CMP-MENTION` | Text growth, removable rich link preview, persisted attachment shelf, reply quote, and mention states use the accepted conversation flow | Implemented in shared flow; review pending |
| [ ] | `COMPOSER-ACT-02` | Voice Messages | Hold threshold, live recording, review playback, discard, and send use the accepted audio flow | Implemented in shared flow; review pending |

## Accessibility

- Scenario labels are developer fixture content; existing product controls keep
  production-ready labels and traits.
- Unavailable attachment states expose an unavailable accessibility value.
- Mention, reply, media, contact, retry, reaction, and voice controls retain
  explicit action labels and do not rely on color.
- Catalog ordering and chat names provide navigation context without requiring
  a visual legend.

## Governing sources

- [Shared conversation](conversation-shared.md)
- [Populated Chats](chats-populated.md)
- [Disappearing-message indicators](disappearing-message-indicators.md)
- [Group info](group-info.md)
- [Chat creation](chat-creation.md)
- [Context menus](https://developer.apple.com/design/human-interface-guidelines/context-menus)
- [Menus and actions](https://developer.apple.com/design/human-interface-guidelines/menus-and-actions)
- [ShareLink](https://developer.apple.com/documentation/swiftui/sharelink)

## Acceptance criteria

- Every checklist ID maps to exactly one deterministic catalog fixture.
- Every supported event, attachment type, gallery layout, delivery state,
  deletion state, reply fallback, reaction treatment, membership state, and
  recovery state appears without relying on a retained story chat.
- Direct and group inception and exit copy use the correct noun.
- Changing disappearing messages emits one event only when the value changes.
- The catalog exposes disappearing-only, disappearing-and-muted, and
  group-member-count-plus-disappearing states with compact 1d, 1w, and 4w
  header values.
- Removing a group photo is distinct from changing it.
- Catalog chats precede all retained story chats and only the first catalog chat
  begins pinned.
- Native menus and swipe actions remain interactive rather than permanently
  displayed.
- Completion still requires user acceptance after hands-on inspection.
