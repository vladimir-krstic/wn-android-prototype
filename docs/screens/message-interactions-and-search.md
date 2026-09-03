# Message interactions and conversation search

Status: Search behavior and the message interaction surface are implemented.
Build, lint, and unit tests pass. The complete 62-case conversation suite,
focused quick-reaction geometry, and held mid-swipe LTR/RTL checks pass on the
physical Pixel 8a; current-build visual inspection confirms the evenly inset
reaction rail, fixed 28 dp focused Signal emoji artwork without clipping, neutral circular ellipsis,
bubble-attached reply indicator, soft focused-overlay backdrop, and searchable
sectioned emoji picker with a scroll-synchronized category rail. User visual
acceptance remains separate.

The 2026-09-03 backdrop, ordinary-timestamp, and Forward-arrow correction
passes lint, app assembly, and instrumentation-test APK compilation.
The current changes have not been inspected on a device.

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/message-actions.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-search.md`
- the message-action, forwarding, deletion, reaction, mention, and search cases
  in the pinned chat catalog and iOS tests
- explicitly requested current-iOS comparison at
  `wn-ios-prototype@4c25393f0eb6` for reply presentation, sent-media viewer,
  bubble metadata, reaction placement, focused actions, selection, reply
  swipe, and bottom settling; this does not globally repin the Android baseline
- Signal Android `879651dc47a7b18b67e7aea52a25197875024680` for its React With
  Any Emoji interaction structure and, by explicit user correction, its exact
  bundled 20-page WebP emoji atlas plus generated metadata. No Signal renderer,
  updater, database, or product-specific storage code is copied; provenance,
  hashes, and bundled legal files are in `signal-emoji-assets.md`

## Android-native adaptation

- The clipped 16 dp message bubble owns deliberate hold detection before its
  descendants receive pointer input. A stationary hold anywhere on text,
  inline links or mentions, reply quotes, media, files, contacts, link cards,
  voice playback, or other rich content opens the same source-preserving
  message actions and consumes the release so the descendant cannot also
  activate. Motion beyond touch slop cancels the hold and remains available to
  scrolling or Reply swipe. An ordinary release is never consumed by the
  bubble: the tapped descendant still opens its gallery, link, profile, file,
  reply source, or playback action. Haptic feedback accompanies a recognized
  hold, while named long-click and custom accessibility actions keep the
  interaction available without relying on touch. Bubble-attached reaction
  and timestamp metadata uses the same arbitration, so the earlier whole-
  message hold area remains intact without restoring a transparent full-row
  target.
- A source-preserving modal overlay presents a real rendering of the focused
  message, quick reactions above it, and the ordered Reply, Forward, Copy,
  Select, Info, Delete actions below it. Incoming/outgoing alignment and safe-
  edge shifting retain the message relationship. Retry is first for a failed
  outgoing message. Conditional Read Aloud/Stop Reading and voice transcript
  commands occupy the same policy-owned command order. Read Aloud follows Copy
  for sent and received authored text without voice attachments, including
  captions and replies; active reading replaces it with Stop Reading. Empty or
  deleted messages do not gain speech actions. Existing received-voice behavior
  remains. Tall real sources scale
  within a 320 dp preview budget. Its conversation backdrop uses a 24 dp
  Compose blur plus an 88% `surfaceContainerLowest` veil: translucent white in
  light appearance and adaptive near-black in dark appearance. The custom
  Dialog suppresses the platform's second dim layer. This follows the user's
  2026-09-03 refinement: the conversation shows through only as faint, softened
  shapes while the backdrop stays predominantly white. The veil remains on Android 11 and earlier
  as the no-blur fallback. Each quick reaction keeps
  a 48 dp semantic target but renders press feedback only through a centered
  40 dp circle; 4 dp rail insets on every edge and 4 dp inter-target spacing
  keep that circle evenly separated from the container. The emoji visual is a
  fixed 28 dp square drawn by the app-owned Kotlin renderer from Signal's pinned
  atlas artwork, including the exact Signal red heart. Image bounds replace
  font line metrics, preventing ascent/descent clipping. More Reactions replaces the text-like plus with Google's rounded
  24 dp horizontal ellipsis in a low-emphasis neutral 40 dp circle, while its
  semantic target remains 48 dp. The quick rail uses the same standard Material
  menu-group color and Level 2 shadow as the command surface; both retain their
  complete shadow inside explicit 8 dp top/bottom viewport gutters. Deleted
  messages expose no actions.
- The transcript and focused preview share one adaptive metadata layout. Its
  visible reaction pills use a 31×23 dp singleton minimum, expand horizontally
  for counts, keep 3 dp gaps, and overlap the bubble bottom by 9 dp. The bubble
  grows to 340 dp and shows at most four real types plus one `+N` pill, reducing
  the real set further only when width requires it. The timestamp keeps a 12 dp
  bubble-edge inset and always follows message direction: incoming start/left,
  outgoing end/right in LTR, mirrored with the message in RTL. Without
  reactions, it begins 2 dp below the bubble; that same gap remains when
  reactions are present, while only pills overlap. Timestamp text, Sent fill,
  and Sending progress use the same lower-emphasis `outline` neutral in both
  the normal transcript and focused context preview. Outgoing
  metadata includes Sent or Sending state; Sent uses a 14 dp filled
  status container with 10 dp check artwork.
  Failed delivery replaces time in that same slot with a 14 dp warning and
  **Not delivered, tap to retry**; geometry and typography stay identical and
  only the icon/label use the semantic error color.
  Message Details lists every type and its people.
- A searchable Material emoji bottom sheet presents the complete pinned White
  Noise catalog as one continuous lazy grid: Recently Used plus Smileys &
  People, Animals & Nature, Food & Drink, Activities, Travel & Places,
  Objects, Symbols, and Flags. Search matches category terms, common aliases,
  or a literal emoji; its named Clear action restores the full catalog. The
  pinned icon rail follows the visible section, scrolls to a selected section,
  expands the sheet for navigation, and leaves while an active query owns the
  surface. Configure Reactions is the leading gear action in that same rail.
  Every emoji keeps a circular 48 dp Material target around a fixed 32 dp
  Signal atlas sprite, section headings expose heading semantics, and the
  88%-height expanded sheet owns Back, swipe dismissal, focus and system
  insets. Keeping the pinned rail visible at rest takes precedence over
  Signal's window-level partial-sheet tab implementation. The deterministic
  catalog is offline and app-owned; exact pinned artwork/data is bundled with
  Signal's license and notice, while Signal storage, downloads, and renderer
  code are not used. A profile-owned six-item quick-
  reaction configuration supports replacement, swap, Reset, cancel, and Done
  without persistence or network work.
- Selection uses one stable leading 48 dp checkbox column and replaces the
  composer with a Material bottom action bar. The column does not move with
  bubble direction. Forwarding uses a searchable multi-select sheet capped at
  five destination chats and appends source-ordered in-memory copies.
- A leading-to-trailing reply swipe uses a 64 dp threshold, a 96 dp resisted
  maximum, threshold haptic, RTL mirroring, and vertical direction locking.
  It moves the complete bubble/reaction unit and is disabled whenever Reply is
  unavailable. A heavier 24 dp Google rounded Reply glyph sits in a 48 dp
  target anchored to the resting bubble rather than the row: it fades after
  5% progress, travels 10 dp, grows from 1.0 to 1.2, pulses to 1.8 at the
  threshold, and follows the same drag/overdrag/return state as the bubble.
  The named Reply semantics action remains equivalent.
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
  labels or filled button containers. Forward uses the user-requested broad,
  outlined right-turn arrow from Signal, shared with the message action menu
  and selection toolbar. Share retains the standard share icon. The icon-only import is documented in
  `../references/signal-interface-assets.md`. More owns Save and Go to Message. Go to
  Message closes the overlay and scrolls the typed conversation route to the
  source message.
- The viewer's top and bottom chrome use the same opaque `background` color
  as its canvas, including system-bar padding. The previous 94% alpha allowed
  tall selected media to show through the controls. Picked photos use the same
  viewer as bundled media and retain the higher-quality bytes prepared by the
  composer; image pixels are never reconstructed from a screen capture.
- Still photos support bounded 1×–4× pinch/pan and 1×/2× double-tap. Pager
  swiping is disabled while zoomed, page changes reset zoom, and Back resets
  zoom before dismissal. Named Zoom In, Zoom Out, and Reset actions provide
  non-gesture alternatives.
- At normal photo zoom, a single-finger downward swipe now dismisses the
  gallery, as explicitly requested on 2026-09-03. Short pulls spring back;
  horizontal paging, video scrubbing, and multi-touch zoom retain priority.
  The current content translates over an opaque gallery backdrop, with strict
  pager/page clipping and no pager edge-stretch effect. Zoomed photos keep
  vertical panning until zoom is reset. Close and system Back remain available.
- Videos now play inside the same viewer using AndroidX Media3 1.11.0 and
  its Material 3 `Player`/`PlayerDefaults` controls. The user explicitly
  replaced the external Open Video handoff with normal play/pause, seek
  backward/forward, a draggable position tracker, elapsed/total time, mute,
  and playback speed. The real video frame fits without cropping. Controls
  follow the same tap-to-toggle chrome and stay exposed for touch exploration.
  The bottom player controls clear the measured Share/Forward bar and system
  insets. In short windows, play/seek join the bottom row instead of
  overlapping the tracker; Material supplies focus, semantics and targets.
- Playback trackers have no fixed end dot, per the user's 2026-09-03 direction.
  The video slider retains Material's draggable thumb and Media3 progress/seek
  state. Media3's `ProgressSlider` has no track slot, so `VideoPlaybackSlider`
  connects the same state holder to a Material `Slider`, overriding only
  `SliderDefaults.Track(drawStopIndicator = null)`.
- Only the settled video page owns a foreground player. Entry and return
  start paused. Paging, dismissal, and leaving the foreground release the
  player; returning retains the in-memory position, mute and speed choices.
  Audio focus and headphone disconnection use Media3, and the screen stays
  awake only during playback. The player reports actual duration, buffering,
  and errors, and its Play control handles retry/replay. Device videos use
  their granted content URI; the catalog uses the existing bundled clip.
  No external player, background playback, network, or new permission is added.
  This overrides only the historical video handoff decision.
- Inactive video pages render only their poster, without allocating a video
  surface. Active playback keeps Media3's SurfaceView on API 24+; API 23 uses
  its TextureView option so dragging the player follows the UI correctly.
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
  the semantic error role. An 88% white-equivalent `surfaceContainerLowest`
  veil sits over the 24 dp-blurred conversation, retaining faint context
  behind the sharp focused content. Timestamps and non-error delivery states
  keep their ordinary chat colors. The quick strip
  gives every 48 dp target a concentric 40 dp circular state layer, with an
  equal 4 dp inset around the rail and between targets. The quick strip and
  command surface use equal 8 dp gaps; with source reactions, the lower gap
  begins at the visible pill edge, not the transparent touch-target edge.
  The command surface itself is the shared non-popup Material menu group used
  by Chats filtering: position-aware rounded state layers, group clipping,
  native 48 dp minimum targets, 20 dp icons, typography, padding, color and
  elevation are component-owned. The overlay retains placement and dismissal;
  no custom full-width pressed row remains. Empty-space dismissal belongs to
  the full-window backdrop, while only the visible reaction rail, actual
  message-plus-metadata bounds, and command group consume taps. A transparent
  full-width alignment column never blocks dismissal.
- Reaction summaries attach to the bubble on one nonwrapping metadata line
  opposite the terminal time with 12 dp edge insets. Each singleton uses a
  31×23 dp visible minimum; counted and `+N` pills expand horizontally, visible
  neighbors are 3 dp apart, and single reactions omit a redundant count. The
  rail overlaps the bubble bottom by 9 dp while retaining expanded minimum
  pointer targets. Each target suppresses its transparent 48 dp indication and
  shares interaction state with a ripple clipped to the visible pill. The
  bubble grows to its 340 dp maximum, the summary shows up
  to four real types plus one `+N`, and adaptive overflow may reduce the real
  set further when necessary.
  Normal tap selects/replaces but never removes an already selected reaction;
  explicit removal remains in focused reaction controls. At scaled type, the
  visible pill may grow while its 9 dp overlap and the focused visible-edge
  menu gap remain fixed.
- The emoji sheet uses a rounded Material search field and one continuous
  sectioned adaptive grid rather than making people switch a text chip before
  they can browse. Its bottom category rail uses official rounded Google
  symbols, a 36 dp selected circle inside each 48 dp target, a leading
  Configure gear, horizontal reachability for compact widths, and automatic
  selected-category tracking while the grid scrolls. The rail is absent while
  search results are active. Emoji targets remain at least 48 dp and their
  pinned Signal sprites remain fixed at 32 dp instead of scaling with body text.
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

- The video viewer uses [Media3 Compose UI](https://developer.android.com/media/media3/ui/compose)
  and the [ExoPlayer lifecycle contract](https://developer.android.com/media/media3/exoplayer/hello-world).
  The stable 1.11.0 release was verified on 2026-09-03. `PlayerDefaults` slots
  preserve the gallery's controls and adapt playback controls to short windows
  while native Material components own interaction and accessibility behavior.
- Android Developers' pointer-event model lets a parent observe the Initial
  pass before descendants and consume only after a recognized long press. The
  transcript uses that arbitration because its descendants have independent
  tap behavior; ordinary single-element long-click targets continue to use
  `combinedClickable`.
- Compose semantics and custom actions provide named assistive-technology
  alternatives to gesture-only operations.
- Material search fields, lists, checkboxes, dialogs, and navigation remain the
  default platform composition. Message actions use a custom modal composition
  because preserving the source-message relationship is the product need;
  emoji, configuration, forwarding, and confirmation flows keep their Material
  sheets or dialogs.
- Sources rechecked for this pass: [tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press),
  [handling interactions and shared interaction sources](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions),
  [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics),
  [Compose `Modifier.blur`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier),
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
  emoji configuration, mention suggestions, and in-place search compile. Rich-
  content regressions physically hold a reply quote, media tile, link card,
  contact card, and voice-playback control and require message actions without
  source, gallery, link, profile, or playback activation; their existing short-
  tap destination tests remain intact.
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
- The current complete 200-test instrumentation audit runs on the physical
  Pixel 8a: 198 tests pass. Two `ChatInfoScreenTest` shared-media viewer
  expectations fail again in isolation and do not exercise or depend on the
  emoji picker changes. The complete 62-case conversation suite is green,
  including the expanded forwarding surface, 48 dp media-viewer actions,
  selection, reply, search, gestures, accessibility, and scaled type.
- The latest user-directed density refinement reduces pills to 31×23 dp, caps
  the shared summary at four real types plus one `+N`, and reduces Sent check
  artwork to 10 dp. Unit, lint, build, and physical Pixel gates pass in the
  complete conversation suite.
- The focused quick-reaction regression proves 48 dp semantic targets contain
  concentric 40 dp circular state layers, with equal 4 dp rail insets on every
  side and 4 dp between adjacent targets. It also proves every accepted quick
  reaction uses 28 dp artwork and the focused viewport reserves 8 dp above and
  below its elevated surfaces. Targeted checks and idle/held frames pass on the
  physical Pixel 8a.
- The focused-overlay backdrop regression asserts full-window veil coverage.
  The established coverage test remains compiled; renewed device execution is
  intentionally deferred until the user explicitly requests it. Host
  compilation covers the 24 dp blur plus 88% adaptive surface veil path without
  reintroducing platform dimming.
- Reply motion has a pure resisted-overdrag regression plus physical LTR and
  RTL geometry tests that pause the return spring and prove the indicator stays
  between the resting and translated bubble edges while remaining vertically
  centered on the bubble. The complete 62-case conversation suite passes on
  the Pixel 8a, and a held mid-swipe frame confirms the weight-600 rounded
  glyph, reaction coupling, and bubble-relative placement in the current build.
- The full-picker regression verifies its expanded sheet, 48 dp emoji targets,
  section headings, named search/Clear behavior, search-time rail dismissal,
  and category jump navigation. It passes in the complete 62-case physical
  Pixel conversation suite. Current-build inspection covers the full catalog,
  IME-visible Beaver search, and the Animals & Nature jump/selected state.
- Signal-atlas parser tests cover sheet/index decoding, presentation-selector
  aliases, and multi-code-unit emoji. An on-device asset test proves every
  accepted catalog emoji resolves to bundled Signal artwork. Current-build
  Pixel inspection covers the quick strip, a transcript reaction pill, and the
  full picker; all use the Signal visuals without font-baseline clipping.
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
- `MediaViewerVideoTest` adds compiled regressions for in-viewer play/pause,
  accessible seeking, playback-control clearance above Share/Forward, Back
  dismissal, and releasing/recreating the player paused with its position and
  settings retained across page and foreground changes. The permission test
  also excludes Media3's unused wake-lock permission. These additions have not
  been executed on a device. The 2026-09-03 host gate passes with 174 unit tests,
  no lint errors, app assembly, and instrumentation-test APK compilation;
  existing unrelated lint warnings remain.
- The downward-dismissal follow-up adds unit coverage for travel/velocity
  thresholds, compact-height travel, upward reversal, and invalid viewport
  state. Compiled interaction coverage exercises short-pull return, horizontal
  paging, zoom protection, normal-photo dismissal, video dismissal, and composer
  cancellation without applying staged exclusions. Rendering-bleed reproduction
  and visual acceptance on a current device build remain pending.
