# Message interactions and conversation search

Status: Search behavior and the 2026-08-31 message-surface correction are
implemented. Build, lint, unit tests, and instrumentation compilation pass for
the latest 23 dp / four-plus-overflow refinement. The preceding revision passed
the full Pixel 8a suite and physical inspection; renewed device execution is
pending after disconnection. User visual acceptance remains separate.

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/message-actions.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-search.md`
- the message-action, forwarding, deletion, reaction, mention, and search cases
  in the pinned chat catalog and iOS tests
- explicitly requested current-iOS comparison at
  `wn-ios-prototype@4c25393f0eb6` for reply presentation, sent-media viewer,
  bubble metadata, reaction placement, focused actions, selection, reply
  swipe, and bottom settling; this does not globally repin the Android baseline
- Signal Android `879651dc47a7b18b67e7aea52a25197875024680` as native Android
  interaction evidence only

## Android-native adaptation

- Compose `combinedClickable` owns deliberate hold detection, ripple, keyboard,
  hover, and accessibility long-click behavior. Haptic feedback accompanies a
  successful hold. Every command is also exposed as a named custom
  accessibility action, so discovery never depends on a gesture.
- A source-preserving modal overlay presents a real rendering of the focused
  message, quick reactions above it, and the ordered Reply, Forward, Copy,
  Select, Info, Delete actions below it. Incoming/outgoing alignment and safe-
  edge shifting retain the message relationship. Retry is first for a failed
  outgoing message. Conditional Read Aloud/Stop Reading and voice transcript
  commands occupy the same policy-owned command order. Tall real sources scale
  within a 320 dp preview budget. Deleted messages expose no actions.
- The transcript and focused preview share one adaptive metadata layout. Its
  visible reaction pills use a 31×23 dp singleton minimum, expand horizontally
  for counts, keep 3 dp gaps, and overlap the bubble bottom by 9 dp. The bubble
  grows to 340 dp and shows at most four real types plus one `+N` pill, reducing
  the real set further only when width requires it. The timestamp keeps a 12 dp
  bubble-edge inset and always follows message direction: incoming start/left,
  outgoing end/right in LTR, mirrored with the message in RTL. Without
  reactions, it begins 2 dp below the bubble; that same gap remains when
  reactions are present, while only pills overlap. Timestamp text, Sent fill,
  and Sending progress use the lower-emphasis `outline` neutral. Outgoing
  metadata includes Sent or Sending state; Sent uses a 14 dp filled
  status container with 10 dp check artwork.
  Failed delivery replaces time in that same slot with a 14 dp warning and
  **Not delivered, tap to retry**; geometry and typography stay identical and
  only the icon/label use the semantic error color.
  Message Details lists every type and its people.
- A searchable Material emoji bottom sheet provides deterministic recent and
  standard categories. A profile-owned six-item quick-reaction configuration
  supports replacement, swap, Reset, cancel, and Done without persistence or
  network work.
- Selection uses one stable leading 48 dp checkbox column and replaces the
  composer with a Material bottom action bar. The column does not move with
  bubble direction. Forwarding uses a searchable multi-select sheet capped at
  five destination chats and appends source-ordered in-memory copies.
- A leading-to-trailing reply swipe uses a 64 dp threshold, a 96 dp resisted
  maximum, threshold haptic, RTL mirroring, and vertical direction locking.
  It moves the complete bubble/reaction unit and is disabled whenever Reply is
  unavailable; the named Reply semantics action remains equivalent.
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
- Deliberate hold now keeps the real source message visually focused between a
  bounded quick-reaction strip and an icon-leading command surface. The
  composition aligns to bubble direction and shifts within system-safe bounds;
  scrim tap, Back and Escape dismiss it. Retry stays first and Delete retains
  the semantic error role. The quick strip and command surface use equal 8 dp
  gaps; with source reactions, the lower gap begins at the visible pill edge,
  not the transparent touch-target edge.
- Reaction summaries attach to the bubble on one nonwrapping metadata line
  opposite the terminal time with 12 dp edge insets. Each singleton uses a
  31×23 dp visible minimum; counted and `+N` pills expand horizontally, visible
  neighbors are 3 dp apart, and single reactions omit a redundant count. The
  rail overlaps the bubble bottom by 9 dp while retaining expanded minimum
  pointer targets. The bubble grows to its 340 dp maximum, the summary shows up
  to four real types plus one `+N`, and adaptive overflow may reduce the real
  set further when necessary.
  Normal tap selects/replaces but never removes an already selected reaction;
  explicit removal remains in focused reaction controls. At scaled type, the
  visible pill may grow while its 9 dp overlap and the focused visible-edge
  menu gap remain fixed.
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
- Material search fields, lists, checkboxes, dialogs, and navigation remain the
  default platform composition. Message actions use a custom modal composition
  because preserving the source-message relationship is the product need;
  emoji, configuration, forwarding, and confirmation flows keep their Material
  sheets or dialogs.
- Sources rechecked for this pass: [tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press),
  [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics),
  [Material search](https://developer.android.com/develop/ui/compose/components/search-bar),
  [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars),
  [window insets](https://developer.android.com/develop/ui/compose/system/insets-ui),
  [drag, swipe, and fling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling),
  [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
  [multitouch transforms](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch),
  [Android Sharesheet](https://developer.android.com/training/sharing/send), and
  [`CreateDocument`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument).

## Acceptance gates

WN-ANDROID-0107 supersedes only the 2026-08-26 message-action sheet. Emoji
selection, reaction configuration and forwarding continue to use
`WhiteNoiseModalBottomSheet`; confirmations continue to use Material dialogs.

- Unit tests cover action availability, reaction replacement/removal, quick
  configuration, search order/content, reply, forwarding bounds/order,
  deletion scopes, and per-profile isolation.
- Compose tests for long-click alternatives, selection, forwarding, details,
  emoji configuration, mention suggestions, and in-place search compile.
- A focused 12-test interaction slice passes on the physical Pixel 8a. It
  covers real-source hold presentation, bottom settlement, bounded single-line
  reaction summaries, stable selection columns, LTR/RTL reply swipes,
  recipient text/voice speech-action placement, group avatar geometry,
  selection actions, forwarding, and reaction configuration.
- A five-test reaction-geometry follow-up passes on the physical Pixel 8a for
  visible overlap, adaptive bubble growth and overflow, timestamp inset,
  equal reaction-aware focused gaps, exhaustive reaction details, group-avatar
  alignment, and source-preserving actions.
- A further five-test metadata correction passes on the physical Pixel 8a for
  compact singleton/count geometry, 3 dp visible spacing, below-bubble text and
  media timestamps, outgoing delivery state, overflow, focused spacing, and
  200% type.
- The complete 191-test instrumentation suite then passes on the physical
  Pixel 8a, including the expanded forwarding surface, 48 dp media-viewer
  actions, selection, reply, search, gestures, accessibility, and scaled type.
- The latest user-directed density refinement reduces pills to 31×23 dp, caps
  the shared summary at four real types plus one `+N`, and reduces Sent check
  artwork to 10 dp. Unit, lint, build, and instrumentation compile gates pass;
  renewed Pixel execution remains pending after disconnection.
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
- Renewed Pixel 8a visual inspection covers direct/group alignment, reaction
  rails, bottom clearance, focused text/voice/tall-media actions, selection,
  and failed-message recovery. User visual acceptance remains separate.
- The rich-media follow-up adds unit coverage for chronological flattening,
  stable exact-frame identity, metadata, unavailable/deleted filtering, MIME
  and filename derivation, and exact-frame forwarding/validation. Compiled
  Compose coverage asserts exact album-tile entry, chat-wide page count,
  sender/position chrome, no sent-media thumbnail rail, 48 dp icon-only Share
  and Forward actions, and targeted message return. The updated overlay has
  not yet received a device visual pass.
