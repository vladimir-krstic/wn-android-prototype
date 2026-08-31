# Message interactions and conversation search

Status: Refined static gate passed on 2026-08-30. Search entry, IME-visible
controls, count/navigation, cyan highlights, result contrast, and close/Back
behavior passed current Pixel 8a inspection on 2026-08-31. The later shared
reply/media-viewer refinement has a clean static gate; renewed device inspection
and user visual acceptance remain pending.

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/message-actions.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-search.md`
- the message-action, forwarding, deletion, reaction, mention, and search cases
  in the pinned chat catalog and iOS tests
- user-approved current-iOS comparison at `wn-ios-prototype@4c25393f0eb6` for
  reply presentation and sent-media viewer behavior only; this does not repin
  the Android baseline

## Android-native adaptation

- Compose `combinedClickable` owns deliberate hold detection, ripple, keyboard,
  hover, and accessibility long-click behavior. Haptic feedback accompanies a
  successful hold. Every command is also exposed as a named custom
  accessibility action, so discovery never depends on a gesture.
- A Material modal bottom sheet presents the focused message's quick reactions
  and the ordered Reply, Forward, Copy, Select, Info, Delete commands. Retry is
  first for a failed outgoing message. Deleted messages expose no actions.
- A searchable Material emoji bottom sheet provides deterministic recent and
  standard categories. A profile-owned six-item quick-reaction configuration
  supports replacement, swap, Reset, cancel, and Done without persistence or
  network work.
- Selection uses a visible 48dp checkbox column and replaces the composer with
  a Material bottom action bar. Forwarding uses a searchable multi-select sheet
  capped at five destination chats and appends source-ordered in-memory copies.
- Message Details is a typed Navigation Compose destination backed by the real
  chat/message graph. Delete for Me removes local entries; Delete for Everyone
  is available only for nondeleted outgoing selections and leaves a clean
  tombstone.
- Conversation search stays in place. A top Material search field updates
  immediately; bottom previous/next/count controls replace the composer and
  select results newest first across message text, sender, and attachment/link
  labels. Deleted messages never contribute content or sender metadata to
  results. Search never mutates the draft.
- Group composer mention suggestions derive only from active members and insert
  the selected visible name into the authoritative draft.
- Sent photo/video frames use one chronological conversation projection with
  stable message/attachment/image identity. Each album tile opens its exact
  frame in the complete chat-wide pager; deleted and unavailable frames are
  omitted and the pager has no thumbnail rail.
- The media overlay reports sender, sent time, and current position. Media taps
  toggle unchanged-size chrome containing Close, More, Share, and Forward;
  touch exploration keeps the named actions visible. Share and Forward are
  edge-aligned 24 dp symbols in transparent native 48 dp icon targets, without
  labels or filled button containers. More owns Save and Go to Message. Go to
  Message closes the overlay and scrolls the typed conversation route to the
  source message.
- Still photos support bounded 1×–4× pinch/pan and 1×/2× double-tap. Pager
  swiping is disabled while zoomed, page changes reset zoom, and Back resets
  zoom before dismissal. Named Zoom In, Zoom Out, and Reset actions provide
  non-gesture alternatives. Video retains its existing handoff behavior.
- Share sends only the current JPEG/MP4 frame through Android Sharesheet with a
  temporary readable content URI. Save uses type-specific `CreateDocument`
  contracts and reports preparation/copy failures without closing the viewer.
  Forward reuses the searchable five-chat picker, accepts an optional one-to-
  four-line message, and normalizes one selected album image to one Photo. The
  shared picker uses the established compact 48 dp search and one 16 dp-inset
  segmented destination group over the gray sheet canvas. Whole rows retain
  native Checkbox semantics; a trailing check is the only selected visual.
  Media forwarding uses a 48 dp-minimum chat-style message capsule with its
  forward arrow inside the trailing 48 dp target. It overlays the scrolling
  destinations without an opaque footer, while measured, system-inset-aware
  list padding keeps the final row fully above the capsule. The complete
  rounded sheet cap—including Material's drag-handle area—and pinned
  title/search content shift one neutral step darker once destinations scroll
  beneath them; the destination canvas retains its lighter tone and draws to
  the physical bottom edge. The capsule owns navigation-bar clearance
  separately. The picker supports Material partial-to-high-sheet expansion,
  with its content capped at 88% of the available height instead of switching
  to a full-screen route.

## Visual-polish translation

- Reply content keeps the shared 12/3/10 dp accent and text geometry, but its
  container follows the message bubble: an 8 dp quote inset and 8 dp quote
  radius sit concentrically inside the bubble's 16 dp radius. The remaining
  message content keeps its established 12 dp horizontal alignment.
- The long-press sheet now keeps a compact tonal preview of the focused
  message, a horizontally scrollable quick-reaction strip, a clear **More
  Reactions** tonal action, and icon-leading Material command rows. Retry stays
  first and Delete retains the semantic error role.
- The emoji sheet uses a rounded Material search field with named Clear and
  Configure actions, full category labels in a scrollable chip row, and an
  adaptive grid whose emoji targets remain at least 48dp instead of forcing an
  iPhone-specific eight-column geometry at every Android width.
- Reaction configuration uses six named replacement targets with Reset and
  Done hierarchy. Forwarding uses the compact white-equivalent search, native
  checkbox semantics, a visible five-chat limit, a segmented white-equivalent
  destination group with 2 dp canvas separators, a useful no-results state,
  and a scroll-reactive fixed header. Selected rows keep the group's
  shape/fill and expose one trailing check. Media forwarding puts its named
  count-bearing action inside the optional-message composer; ordinary message
  forwarding retains its direct completion action.
- Selection replaces text-heavy bars with a Close app-bar action, a live
  selected count, named Delete/Forward icon actions, and visible explanation
  when the accepted deleted-message or 32-message rule disables forwarding.
- Conversation search stays in the transcript, requests focus on entry, owns
  the exact shared 48 dp Chats search field, preserves the draft, and uses
  compact previous/next icon controls around a live result count. Those
  controls own IME and navigation-bar clearance. Every matching message stays
  at full contrast while only nonmatches are subdued. Visible occurrences in
  message text, group-author names, attachment labels, link titles and link
  domains use Android platform cyan with black text and exact 4 dp rounded
  glyph-run backgrounds; the current position remains localized and spoken.
- The conversation identity target keeps its Chat Info Button semantics but,
  by explicit user direction, has no press/ripple indication. A 40 dp avatar
  and the compact title/metadata block share one center axis with a 4 dp
  horizontal gap. A 2 dp optical overlap between the native title and metadata
  line boxes reduces their visible leading by about 30%. Search is entered
  only through Chat/Group Info; Android/system Back closes it in place through
  the same pattern used by Chats.
- Message Details now follows the child-destination app-bar hierarchy and
  groups the shared message plus delivery/sender/recipient information on
  restrained tonal surfaces without exposing protocol data.

## Current official Android guidance

- Android Developers recommends `combinedClickable` for long-click context
  menus and haptic feedback after recognition.
- Compose semantics and custom actions provide named assistive-technology
  alternatives to gesture-only operations.
- Material search fields, modal bottom sheets, lists, checkboxes, dialogs, and
  navigation remain the default platform composition for the bounded flows.
- Sources rechecked for this pass: [tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press),
  [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics),
  [Material search](https://developer.android.com/develop/ui/compose/components/search-bar),
  [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
  [window insets](https://developer.android.com/develop/ui/compose/system/insets-ui),
  [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
  [multitouch transforms](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch),
  [Android Sharesheet](https://developer.android.com/training/sharing/send), and
  [`CreateDocument`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument).

## Acceptance gates

2026-08-26 shared-sheet refinement: message actions, emoji selection, reaction
configuration and forwarding use `WhiteNoiseModalBottomSheet`; titled sheets
use `WhiteNoiseSheetHeader`. No repeated navigation-bar padding remains inside
these modals. The reaction configuration scrolls at large font sizes; message
actions reserve constrained space for their scrollable action list. Ordinary
rows stay transparent; preview, selection and completion groups retain their
intentional tones. Message actions still use a sheet; only Chats row commands
move to an anchored menu. Selection bars and media screens are unchanged.

- Unit tests cover action availability, reaction replacement/removal, quick
  configuration, search order/content, reply, forwarding bounds/order,
  deletion scopes, and per-profile isolation.
- Compose tests for long-click alternatives, selection, forwarding, details,
  emoji configuration, mention suggestions, and in-place search compile.
- Focused tests cover the message context/title, reaction configuration slots,
  selection controls, forwarding limit/search, focused conversation search,
  the shared 48 dp field, named Clear, previous/next/count controls, Back
  dismissal, matching-message contrast, cyan range generation, and centered
  header identity geometry with compact title/metadata leading. Reply coverage
  asserts the shared accent inset plus the context-specific 8 dp concentric
  quote inset. The root conversation app bar explicitly exposes no Search
  action.
- Media-forward coverage additionally asserts the exact 48 dp compact search,
  48 dp chat-style message capsule and trailing action target, destination-list
  overlap beneath the transparent composer overlay, final-row clearance,
  physical-edge destination viewport, high non-full-screen sheet bounds,
  matching darker scrolled cap/handle/header, whole-row Checkbox semantics,
  the 2 dp segmented-list rhythm, one trailing selected check, and count-bearing
  action semantics.
- The complete clean static gate and permission/export audit pass.
- The rich-media follow-up adds unit coverage for chronological flattening,
  stable exact-frame identity, metadata, unavailable/deleted filtering, MIME
  and filename derivation, and exact-frame forwarding/validation. Compiled
  Compose coverage asserts exact album-tile entry, chat-wide page count,
  sender/position chrome, no sent-media thumbnail rail, 48 dp icon-only Share
  and Forward actions, and targeted message return. The updated overlay has
  not yet received a device visual pass.
