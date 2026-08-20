# Shared conversation

The deterministic unsent-message examples, rich-link composition, and
attachment-draft persistence are specified in
[Conversation composer states](conversation-composer-states.md).

## Purpose

Provide one native direct/group conversation surface for every chat row while
retaining the accepted Fiatjaf and White Noise Support stories.

## Timeline

- The principal toolbar item is the single entry point to Chat Info or Group
  Info. It uses a 44-point avatar beside the conversation title and, for a
  group, its member count. When disappearing messages are on, a direct chat
  shows the compact timer symbol and 1d, 1w, or 4w below its title; a group
  appends a centered dot and that same timer status after the member count.
  The complete status treatment is defined in
  [Disappearing-message indicators](disappearing-message-indicators.md). The
  system principal placement centers this combined identity control in the
  navigation bar. Search is not duplicated as a
  trailing conversation-toolbar action; it remains available inside the
  applicable info screen.
- Messages use stable identities in a `ScrollViewReader`, `ScrollView`, and
  `LazyVStack`. Incoming content is leading; outgoing content is trailing.
  Opening a conversation settles on the true newest timeline entry after the
  composer bottom bar is laid out; older history remains reachable by
  scrolling to the conversation's beginning.
- Two existing chats are the durable conversation reference histories. **Maya
  Chen** is the complete one-to-one catalog and **Weekend Walks** is the
  complete group catalog. Each begins with the conversation's earliest
  activity and proceeds in strict chronological order to the current day.
  Variants form a readable conversation rather than an adjacent checklist of
  unrelated examples.
- Consecutive messages group by author. Direct chats never show transcript
  avatars or author labels. Group chats show the incoming author's name above
  the first bubble in a cluster and the author's avatar beside the final bubble
  in that cluster. The author label's leading edge aligns with the same
  12-point inset as the message text. Each incoming group-author name receives one stable color
  derived from that person's complete public key. The nine-color palette uses
  the adaptive system red, orange, green, teal, blue, indigo, purple, pink, and
  brown hues; yellow, mint, and cyan are omitted from this small-text treatment.
  A hue is adjusted toward the adaptive system label color only as much as
  needed to preserve a 4.5:1 contrast ratio against the system background.
  A person without a photo uses the same public-key bucket for a filled
  monogram avatar, but does not reuse the adjusted author-name foreground as
  its fill. The avatar surface uses the corresponding adaptive system hue and,
  by explicit product direction, always renders one uppercase first initial in
  white in both light and dark appearances. The initial uses a slightly smaller
  semibold subheadline instead of the previous headline size. This white-only
  treatment is an approved custom exception; the visible name and profile
  action continue to carry identity without depending on the monogram color.
  **Group - Identity Colors** is a separate developer catalog chat with nine
  consecutive incoming examples, one for every supported identity bucket, so
  the name and monogram-avatar treatment can be reviewed together without
  changing state. **Group - Messages & Mentions** remains focused on authorship,
  clusters, mentions, and replies.
  The visible name and profile action continue to carry identity without
  relying on color. Locale-aware time remains visibly outside and below the
  terminal bubble. Every day boundary remains visible as an inline transcript
  header with 18 points of space above and below it. Once that header scrolls
  above the viewport, the same section becomes a noninteractive pinned regular
  Liquid Glass capsule; later date headers remain visible inline until each
  reaches the top and replaces the pinned day. The label uses medium footnote
  text with 12-point horizontal and 3-point vertical insets. It reads **Today**
  or **Yesterday** when applicable, abbreviated weekday/month/day for dates
  under six months old, and a locale-aware medium date including the year for
  older dates. Separate message clusters use 16 points of space; messages
  inside one cluster remain compact.
- Deleted messages remain as **You deleted this message.** or **This message
  was deleted.** A failed outgoing message replaces its timestamp with the red
  status **Not delivered, hold for options**. Its outlined warning icon and
  copy form a compact three-point-spaced
  unit whose leading edge matches the outgoing message text inset. Touching and
  holding either the bubble or that status opens the focused message-action
  presentation, whose first recovery action is **Retry Send**. Sent messages do
  not show delivery checkmarks.
- The selected message-action flow is defined in `message-actions.md`. A
  Signal-informed focused presentation owns quick and full-picker reactions,
  Reply, Forward, Copy, Select, Info, and Delete while standard controls own
  every command and subsequent screen.
- Every nondeleted message that can currently be replied to also accepts a
  semantic leading-to-trailing horizontal swipe. Vertical-dominant movement
  remains timeline scrolling, and right-to-left layout mirrors the reply
  direction. The message follows the finger while a reply indicator is
  revealed from beneath its leading edge at one-eighth of that movement. At a
  55-point threshold the indicator changes to its active treatment and gives
  one light impact; moving back below the threshold cancels that ready state,
  while movement beyond it gains one-sixth elastic resistance. Releasing in
  the ready state returns the message over 0.2 seconds, installs the existing
  reply quote, and focuses **Message**. Any other release only returns the
  message. Selection, invitations, unavailable composers, deleted messages,
  and the focused message-action presentation do not offer the gesture.
- Signal's current iOS message component and conversation pan recognizer are
  bounded comparison evidence for that threshold, direction lock, feedback,
  elastic reveal, and reset. White Noise recreates the behavior with an
  original `UIPanGestureRecognizer` bridged through
  `UIGestureRecognizerRepresentable`; it does not copy or port Signal source.
  Its delegate rejects vertical-dominant movement while the recognizer is still
  possible, before the reply pan can claim the touch, so the transcript's
  native vertical scroll recognizer remains the owner. The user-approved visual
  difference is the filled SF Symbol
  `arrowshape.turn.up.left.fill` rather than Signal's outlined reply asset. It
  retains the semantic secondary-gray foreground before and after activation,
  following the user-supplied Apple Messages reference; threshold scale and
  haptic feedback communicate readiness without turning the arrow black. Reply
  remains available through the named accessibility action and the press-and-
  hold action presentation, so the gesture is never the only route.
- Reaction chips use opaque adaptive system surfaces in both appearances;
  current-profile participation uses an opaque `systemGray4` surface rather
  than a translucent overlay. Each visible pill is shadowless and uses a
  one-point adaptive `separator` border to stay distinct from either bubble
  color. Signal's current iOS reaction metrics are the
  comparison basis: a 14-point bold emoji, a 12-point bold monospaced count,
  seven-point horizontal content insets, and two points between emoji and
  count. The approved White Noise exception is a 22-point visible pill.
  Adjacent pill surfaces use a three-point gap, while each pill
  retains a 40-point interaction height. This 40-point height
  is an explicit user-approved compact exception to the native 44-point target
  default. The reaction row's leading edge for outgoing messages, or trailing
  edge for incoming messages, aligns with the bubble's 12-point text inset.
  The timestamp remains on the opposite side at the same 12-point text edge and
  uses a 15-point external offset, aligning its lower edge with the visible
  22-point reaction pill. The summary still accounts for that timestamp
  width and shows every reaction type that fits without colliding with it; when
  the row is too narrow, its final visible item is an adaptive `+N` pill
  counting the omitted reaction types. This Signal-informed compact overflow
  rule is implemented locally rather than copied from external code. Reaction
  state changes suppress button dimming and interpolation so neither selected
  nor unselected surfaces flash transparent. Reactions add no internal bubble
  padding: the visible pill begins one point below the content's lower edge,
  overlaps only the lower bubble edge, and reserves its external hit area
  without changing a one-line bubble's height.
- Conversation search is defined in `conversation-search.md`: it keeps the
  transcript visible, navigates newest-first matches in place, and visibly
  emphasizes matching terms. Reply quotes scroll to the original without
  adding an outline or other temporary target indicator, or show a stable
  unavailable state.
- Inline emphasis uses native attributed text. Links and mentions retain the
  approved adaptive monochrome bubble palette while adding an underline;
  mentions also use semibold type. VoiceOver reads the rendered words rather
  than Markdown punctuation.

## Message presentation direction

- The message presentation is an original shared SwiftUI implementation. The
  project does not copy or port Signal source code and does not add Signal or
  another third-party runtime dependency.
- Signal provides bounded comparison evidence for message-content composition:
  reply quotes, reactions attached to their message, media galleries, link
  previews, mentions, file rows, audio-message controls, and full-height media
  browsing. Those behaviors are reimplemented against the prototype's own
  deterministic models. Signal's AGPL-licensed source is not copied or ported.
  The adopted media conclusions come from its current album and media-page
  implementations: deterministic count-based arrangements, a five-tile
  overflow limit, source-order paging, intrinsic single-media sizing, and one
  viewer shared by conversation and media-library entry points.
- Signal's current date-header component and conversation layout provide
  bounded comparison evidence for date sections. Signal keeps every generated
  date header in the transcript, pins the last header that moved above the
  viewport, leaves later headers inline, and pushes the pinned header away when
  the next one arrives. On iOS 26 and later its pinned variant uses a regular
  glass capsule with medium footnote type and 12-point horizontal and 3-point
  vertical margins. White Noise recreates that behavior with public SwiftUI
  visibility APIs: inline date rows remain in the lazy transcript while one
  conversation-level glass header follows the top visible timeline entry. Its
  initial value comes from the bottom day so opening a long bottom-anchored chat
  does not depend on an offscreen lazy header being instantiated. No Signal
  source is copied.
- Apple Messages provides the visual direction: semantic San Francisco system
  typography, generous breathing room, rounded message silhouettes, compact
  same-author clusters, larger separation between clusters, author identity at
  the cluster edges, and media that participates in the message silhouette.
- Message bubbles are tail-free. Incoming bubbles align precisely to the
  conversation's leading content margin and outgoing bubbles align precisely
  to its trailing content margin. That margin is shared with the composer and
  the screen's system-positioned controls instead of adding a second bubble
  inset.
- Every bubble keeps the same fully rounded continuous silhouette, including
  messages in a same-author cluster. Grouping is communicated by compact
  vertical spacing rather than flattened or joined corners. A bubble up to the
  one-line height is a true capsule whose radius is half its rendered height;
  once content grows beyond one line, the silhouette uses the fixed 18-point
  radius instead of continuing toward a circular side.
- Time remains visible below the terminal bubble rather than being hidden until
  a gesture. It sits beneath the bubble on its conversation-center side and is
  inset from that inner edge by the shared bubble corner radius. This is an
  explicit user-approved exception to the Apple Messages comparison and keeps
  delivery and failure state easy to scan.
- Incoming gray uses the adaptive UIKit `systemGray5` color. Apple does not
  publish the private Messages bubble color; `systemGray5` is the closest
  public semantic step that makes the previous `systemGray4` treatment a little
  lighter without approaching the page background as closely as
  `systemGray6`.
- Replies, reactions, gallery layouts, link previews, file rows, GIFs,
  contacts, and voice messages all use the same shared bubble shell, cluster
  spacing, timestamp placement, and group-identity rules in catalog and legacy
  chats.
- Unavailable photo data and video attachments without a playable destination
  retain their visible fallback tile but are static, noninteractive content.
  They expose no preview hint or button trait, do nothing when tapped, and are
  excluded from the chat-wide viewer page set. The viewer's unavailable state
  remains only as a defensive fallback for stale data that becomes unavailable
  after opening.
- File rows use a bare semantic file glyph rather than nesting the glyph in a
  second rounded container. File and contact accessories share one compact
  caption-sized symbol and a trailing slot with four points of breathing room.
  Reply bars begin at the rich card's 12-point inner corner stop rather than
  touching its shell inset. An incoming quote bar uses a lighter secondary
  gray than its outgoing equivalent. A quote preview takes its natural
  one-line height, grows to at most two lines, and then truncates without
  reserving an empty second line. On outgoing black bubbles, textual rich-card
  surfaces use a slightly stronger adaptive white overlay so their hierarchy
  remains legible without changing the bubble color.
- The shared bubble shell owns the content-to-silhouette spacing. Naturally
  sized text-only and deleted messages use a 12-point horizontal inset and an
  8-point vertical inset. Any message containing a reply quote or attachment
  uses a 6-point shell inset and one 256-point inner canvas so its quote, media,
  and stacked cards share exact leading and trailing edges. Captions and mixed message text
  add six horizontal points and two bottom points inside that canvas,
  preserving the same 12-by-8-point visual text inset without moving the rich
  components from their 6-point shell inset. Textual rich cards use a 6-point
  internal inset and a concentric 12-point corner radius; photo/video galleries
  and GIFs fill the canvas edge to edge.
  Rich components and mixed-content sections use 6 points of vertical spacing,
  while gallery tiles use a 2-point gap. These fixed values are
  an approved custom composition metric, not an inferred system-control size.
- Sticker and shared-location messages are not supported. They are absent from
  deterministic fixtures, catalog coverage, and the shared renderer.

## Composer and media

### Photo and video layout

- Every photo and video stores normalized display pixel dimensions. Video
  dimensions apply the asset track's preferred transform before normalization.
  Zero, negative, nonfinite, or unavailable dimensions are unknown and use a
  square fallback.
- A lone photo or video is the intentional exception to the fixed 256-point
  rich-content canvas. Its width-to-height ratio is clamped to `0.35...2.857`
  and laid out inside a maximum 256-by-256-point frame. Following the useful
  part of Signal's sizing behavior, portrait media has a 192-point minimum
  bubble-content width instead of collapsing into a narrow column; the media
  center-crops only where that minimum-width floor or the ratio clamp requires
  it. Low-resolution sources retain the 150-point practical display safeguard.
  A caption wraps below at the resulting media width.
- Albums retain a 256-point width, use 2-point gutters, preserve attachment
  order, and clip once with the 12-point inner-media radius. Each tile is
  hard-clipped to its calculated rectangle before overlays are added, so image
  aspect ratios can never stretch, overlap, or resize the album grid:
  - two: two 127-point squares;
  - three: one 170-point square beside two stacked 84-point squares;
  - four: a 2-by-2 grid of 127-point squares;
  - five: two 127-point squares above three 84-point squares;
  - six or more: the five-item layout, with tile five dimmed and labeled `+N`
    for the number of hidden attachments.
- Album tiles crop independently around center. Video play and duration
  overlays remain inside their tile; the overflow treatment replaces both on
  tile five. Selecting an available tile opens that exact attachment. Selecting
  the overflow tile starts at attachment five and hidden attachments follow by
  normal paging. An unavailable tile remains inert even if it occupies the
  overflow position.

### Unified media viewer

- Conversation and Chat Info present the same native large-detent sheet and
  the same chronological media index. The index preserves message order
  followed by attachment order, excludes deleted messages, and excludes
  unavailable media from paging while leaving those items visible in source
  bubbles and Shared Media.
- The viewer uses the same adaptive system-background surface as the composer
  media preview, appearing white in Light appearance. Horizontal paging keeps
  every settled page at the full viewport width and uses the composer
  preview's same 32-point gutter only between page targets while swiping.
  Photos and videos retain image pinch/double-tap zoom, native video playback,
  sender and localized time in top chrome, Share and Forward in the bottom
  toolbar, and Save plus Go to Message in More. Source-message captions are
  not repeated over media.
- The initially selected video autoplays. Paging pauses the prior video and
  resets image zoom; dismissal stops playback. Tapping media toggles chrome.
  SwiftUI owns the sheet's opening, whole-container drag-down interaction, and
  dismissal transition; the media page never receives a separate vertical
  dismissal offset. Interactive dismissal is unavailable while an image is
  zoomed. Ordinary dismissal preserves the conversation's exact scroll
  position; Go to Message dismisses and scrolls to the source.
- VoiceOver announces sender, media type, position, timestamp, duration, and
  availability, and provides explicit controls for gesture-only behaviors.
  Reduced Motion remains governed by the native sheet transition.

- The shared multiline field uses **Message**. Draft text and the pending reply
  remain with their chat through navigation; queued attachments remain ordered
  in the active composer until sent or removed.
- The resting field keeps one stable native interaction height and vertically
  centers its text. Typing never changes the field's width because the trailing
  44-point slot changes in place from waveform to Send. Send is inside the
  field, matching the familiar Messages composition.
- The field grows through ten visible lines, then the native vertical
  `TextField` scrolls. Its glass uses the resting corner radius at every height
  instead of becoming a tall capsule with semicircular sides.
- While the focused field gains visible lines, the timeline keeps its bottom
  anchor on every discrete line-height change so the newest content moves with
  the growing composer and keeps the same bottom spacing at each step. Manual
  timeline scrolling remains available immediately afterward.
- When the Message field gains focus and the software keyboard opens, the
  timeline returns to its true bottom even if the person had been reading older
  history. The keyboard therefore moves the newest content above the composer;
  manual timeline scrolling remains available after the focus transition.
- While the software keyboard is open, tapping anywhere in the conversation
  outside the text field dismisses it. A window-level UIKit tap recognizer
  ignores `UITextField` and `UITextView`, does not cancel the tapped control's
  action, and therefore preserves message, menu, attachment, playback, and
  navigation interactions while clearing composer focus.
- The attachment control and resting field share the same 44-point interaction
  and visible height. The Send circle is 32 points inside its 44-point trailing
  slot, leaving the user-approved six-point visual inset at the top, bottom, and
  trailing field edge. Voice review mirrors that geometry: its Play circle is
  32 points inside a 44-point leading slot, leaving six points at the top,
  leading, and bottom field edges.
- `PhotosPicker` selects multiple images/videos. `fileImporter` selects files.
  Images are prepared once at a maximum 1024-pixel dimension; selected videos
  and files use temporary app-owned copies only.
- **Camera** in the attachment menu opens an AVFoundation capture surface in a
  native SwiftUI sheet at the large detent. SwiftUI owns the sheet's corners,
  background, bottom attachment, transition, and interactive swipe-down
  dismissal, with the native drag indicator visible; no custom presentation
  mask or transparent sheet background is applied. The camera preview fills
  the sheet's content and safe-area background. The visible Close control
  provides an equivalent explicit exit.
  Tapping the shutter takes a photo; holding it records video until release.
  Captured media returns to the ordered attachment queue and remains in
  temporary app-owned storage. Camera or microphone denial keeps the
  conversation intact and provides system Settings recovery.
- The attachment button uses a native `UIButton` control menu with a compact
  `UIMenu`; UIKit owns its rows, spacing, shape, material, ordering, and
  dismissal. Camera is the first item. Opening the menu while the keyboard is
  visible preserves composer focus and keeps the keyboard in place. The
  button and the presented menu are excluded from the conversation's global
  outside-tap keyboard dismissal, while composer mutations remain blocked
  beneath the menu. Choosing Camera, Photos and Videos, Files, or Contact then
  dismisses the keyboard before presenting that destination. Contact opens a
  native searchable list of followed White Noise profiles. Choosing a profile
  queues one shareable contact card and closes the picker; reopening the picker
  and choosing another profile replaces the queued contact card. A menu row can
  therefore never focus or blink the field beneath it.
- The attachment strip supports removal before sending. Messages may combine
  text, images, videos, files, and one contact card. A typed or pasted URL stays
  ordinary tappable message text, including when a file is queued; choosing a
  file does not synthesize a link-preview card. Media opens a paged viewer;
  videos use `VideoPlayer`, images use native `UIScrollView` pinch, pan, and
  double-tap zoom, files use Quick Look where supported, and sharing uses the
  system share sheet.
- Holding the empty-composer waveform for the native `LongPressGesture`
  threshold of 0.4 seconds, within its 32-point movement allowance, replaces
  the input with an animated red waveform, elapsed timer, and explicit Stop
  control. A tap alone does not start recording. Quiet or not-yet-populated
  waveform bars use a lighter red treatment while active bars use the full
  semantic red, and the waveform expands through the available field width.
  Stopping enters a review state instead of sending: Cancel discards the draft,
  Play or Pause previews it, and Send explicitly confirms it. The app never
  initializes audio recording or requests microphone permission for voice
  messages; microphone access is used only to include sound in captured video.
  VoiceOver exposes explicit start, stop, review playback, cancel, and send
  actions.
- Voice-message bubbles show a playable waveform, elapsed progress, and
  duration. The waveform is generated deterministically for bundled fixtures
  and for the in-memory recording state. Recording and bubble waveforms use
  two-point capsule bars. Playback updates are isolated to the voice control;
  pressing Play never changes the timeline's scroll position, and the rest of
  the message bubble is not a competing playback tap target.
- Only one audio or video item plays at once and playback stops on navigation.
- A voice bubble fills the shared 256-point rich-content canvas. Its fixed play
  target and duration flank a waveform that flexes through the remaining width.
  The centered play glyph's trailing visual space is balanced against the
  waveform-to-duration spacing while its control retains the full 44-point hit
  target, so the controls keep their intended geometry without introducing a
  narrower one-off attachment width.
- Contact is available from the composer attachment menu. GIF and deterministic
  link preview remain polished showcase renderers rather than composer options.
  Sticker and shared-location messages are intentionally unsupported.
- In groups, typing `@` offers matching members. A resolved mention has no
  underline; the entire `@Name` run uses semibold type on a subtle rounded
  four-point-radius surface and opens Group Member. The surface follows the
  run's exact typographic height and extends two points horizontally, keeping
  wrapped mentions visually separate without clipping their text. Unmatched
  `@` text remains ordinary message text. This adopts the compact highlighted-run behavior from
  the user-supplied Signal reference while using SwiftUI's native attributed
  links and `TextRenderer` rather than a separate overlay control.

## Availability

- A left/removed profile, blocked direct peer, or chat without Chat Relays keeps
  history visible but replaces the composer with the applicable recovery
  action.
- Leaving or being removed from a group creates a centered, nonactionable
  timeline event at the chronological point where membership ended. The exact
  copy is **You left the group.** or, when the removing member is known,
  **Maya Chen removed you from the group.**
- A left or removed group replaces the composer with a passive native `Label`
  in a system `safeAreaBar`: **You left this group.** or **You were removed
  from this group.** The status uses a semantic SF Symbol and secondary
  styling. It is not a disabled field, alert, toast, full-screen unavailable
  state, or button; no rejoin action is available in this flow.
- Membership statuses and the active composer use `safeAreaBar` with SwiftUI's
  soft bottom scroll-edge effect, allowing timeline content to transition
  beneath the stationary surface without a hard opaque boundary or a custom
  blur implementation. The safe-area bar follows keyboard-safe layout.
- The composer follows keyboard safe areas. It does not install translucent
  content behind the system keyboard.
- The active composer has no opaque `systemBackground` backing. The native soft
  bottom scroll-edge effect remains visible through the composer area, creating
  a progressive blur instead of a hard white cutoff.

## Custom exceptions

Apple does not provide a public complete message-bubble, reaction, gallery,
waveform, Messages-style composer, or press-and-hold photo/video shutter.
Those bounded compositions are original SwiftUI views that use semantic system
colors/type, 44-point
interaction targets, interruptible state-driven motion, Reduce Motion, and
complete VoiceOver actions. Standard navigation, text entry, capture sessions,
pickers, menus, sharing, playback, permissions, alerts, and keyboard layout stay
system-owned. The capsule one-line treatment, 18-point expanded bubble radius,
12-point horizontal and 8-point vertical text insets, 6-point rich shell and
card insets, 256-point rich-content canvas, 12-point rich-component
radius, and 2-point gallery gap are explicit user-approved visual metrics for
message composition.
The 32-point Send and voice-review Play circles inside 44-point hit targets and
the two-point waveform bars are explicit user-approved visual metrics for this
custom composer. Presenting the camera in a native large sheet instead of
Apple's default full-screen camera treatment is an explicit user-approved
presentation choice; the standard sheet still owns all geometry and dismissal
behavior.
Apple also does not provide a message-level swipe-to-reply control for a custom
conversation timeline. The user-approved Signal-informed, direction-gated
`UIPanGestureRecognizer` is a bounded custom exception exposed to SwiftUI only
through Apple's public `UIGestureRecognizerRepresentable`. Direct manipulation
remains visible during the drag; Reduce Motion removes the threshold scale
animation, and the existing Reply command and accessibility action provide
equivalent explicit activation.
The unified media viewer's 32-point interpage gutter is an explicit
user-approved custom spacing value shared with the composer media preview; it
does not inset a settled page.

## Voice asset provenance

The shared voice sample is an original local asset created on 2026-08-08 with
the macOS Samantha text-to-speech voice reading the original phrase **Meet me
by the old bridge.** It was transcoded locally to a short 16 kHz mono MP3 and
embedded in the app. No recording or copyrighted audio was downloaded, and no
speech synthesizer or microphone API is used at runtime.

## Showcase document and video provenance

The four PDF fixtures contain original local prototype notes generated on
2026-08-08 and bundled solely to exercise Quick Look. `ChatTrailClip.mp4` is an
original eight-second local motion treatment generated from the already
documented bundled pebble artwork. The app performs no runtime download, and
every showcased file and video has usable local data rather than a decorative
dead affordance.

## Image asset provenance

Conversation member portraits, abstract identities, and catalog media use
statically bundled, size-optimized Unsplash images. Each adopted source is
recorded beside the bundled asset in `docs/references/chat-image-assets.md` with
its photographer and Unsplash page URL. The app performs no runtime image
download, remote lookup, or attribution request.

## Governing sources

- [TextField](https://developer.apple.com/documentation/swiftui/textfield)
- [Focus](https://developer.apple.com/documentation/swiftui/focus)
- [LongPressGesture](https://developer.apple.com/documentation/swiftui/longpressgesture)
- [onLongPressGesture](https://developer.apple.com/documentation/swiftui/view/onlongpressgesture(minimumduration:maximumdistance:perform:onpressingchanged:))
- [DragGesture](https://developer.apple.com/documentation/swiftui/draggesture)
- [simultaneousGesture(_:including:)](https://developer.apple.com/documentation/swiftui/view/simultaneousgesture(_:including:))
- [UIGestureRecognizerRepresentable](https://developer.apple.com/documentation/swiftui/uigesturerecognizerrepresentable)
- [UIPanGestureRecognizer](https://developer.apple.com/documentation/uikit/uipangesturerecognizer)
- [gestureRecognizerShouldBegin(_:)](https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate/gesturerecognizershouldbegin(_:))
- [lineLimit](https://developer.apple.com/documentation/swiftui/view/linelimit(_:reservesspace:))
- [ScrollView](https://developer.apple.com/documentation/swiftui/scrollview)
- [defaultScrollAnchor](https://developer.apple.com/documentation/swiftui/view/defaultscrollanchor(_:for:))
- [onGeometryChange](https://developer.apple.com/documentation/swiftui/view/ongeometrychange(for:of:action:))
- [LazyVStack](https://developer.apple.com/documentation/swiftui/lazyvstack)
- [Grouping data with lazy stack views](https://developer.apple.com/documentation/swiftui/grouping-data-with-lazy-stack-views)
- [scrollTargetLayout](https://developer.apple.com/documentation/swiftui/view/scrolltargetlayout(isenabled:))
- [onScrollTargetVisibilityChange](https://developer.apple.com/documentation/swiftui/view/onscrolltargetvisibilitychange(idtype:threshold:_:))
- [glassEffect](https://developer.apple.com/documentation/swiftui/view/glasseffect(_:in:))
- [Context menus](https://developer.apple.com/documentation/swiftui/view/contextmenu)
- [Menu](https://developer.apple.com/documentation/swiftui/menu)
- [menuActionDismissBehavior](https://developer.apple.com/documentation/swiftui/view/menuactiondismissbehavior(_:))
- [UIButton menu](https://developer.apple.com/documentation/uikit/uibutton/menu)
- [UIContextMenuInteraction](https://developer.apple.com/documentation/uikit/uicontextmenuinteraction)
- [UIContextMenuInteractionDelegate](https://developer.apple.com/documentation/uikit/uicontextmenuinteractiondelegate)
- [ControlSize](https://developer.apple.com/documentation/swiftui/controlsize)
- [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets)
- [sheet](https://developer.apple.com/documentation/swiftui/view/sheet(ispresented:ondismiss:content:))
- [sheet(item:onDismiss:content:)](https://developer.apple.com/documentation/swiftui/view/sheet(item:ondismiss:content:))
- [presentationDetents](https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:))
- [presentationDragIndicator](https://developer.apple.com/documentation/swiftui/view/presentationdragindicator(_:))
- [interactiveDismissDisabled](https://developer.apple.com/documentation/swiftui/view/interactivedismissdisabled(_:))
- [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker)
- [fileImporter](https://developer.apple.com/documentation/swiftui/view/fileimporter)
- [AVCam: Building a camera app](https://developer.apple.com/documentation/avfoundation/avcam-building-a-camera-app)
- [AVCapturePhotoOutput](https://developer.apple.com/documentation/avfoundation/avcapturephotooutput)
- [AVCaptureMovieFileOutput](https://developer.apple.com/documentation/avfoundation/avcapturemoviefileoutput)
- [Camera authorization](https://developer.apple.com/documentation/avfoundation/requesting-authorization-to-capture-and-save-media)
- [VideoPlayer](https://developer.apple.com/documentation/avkit/videoplayer)
- [AVAudioPlayer](https://developer.apple.com/documentation/avfaudio/avaudioplayer)
- [Quick Look](https://developer.apple.com/documentation/swiftui/view/quicklookpreview)
- [ShareLink](https://developer.apple.com/documentation/swiftui/sharelink)
- [Label](https://developer.apple.com/documentation/swiftui/label)
- [safeAreaBar](https://developer.apple.com/documentation/swiftui/view/safeareabar(edge:alignment:spacing:content:))
- [ScrollEdgeEffectStyle](https://developer.apple.com/documentation/swiftui/scrolledgeffectstyle)
- [Typography](https://developer.apple.com/design/human-interface-guidelines/typography)
- [Color](https://developer.apple.com/design/human-interface-guidelines/color)
- [TextRenderer](https://developer.apple.com/documentation/swiftui/textrenderer)
- [Text.Layout.Run](https://developer.apple.com/documentation/swiftui/text/layout/run)
- [Standard colors](https://developer.apple.com/documentation/uikit/standard-colors)
- [systemGray5](https://developer.apple.com/documentation/uikit/uicolor/systemgray5)
- [Group conversations in Messages](https://support.apple.com/guide/iphone/group-conversations-iphb10c80fc5/ios)
- [Signal iOS source and license](https://github.com/signalapp/Signal-iOS)
- [Signal date-header component](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/Components/CVComponentDateHeader.swift)
- [Signal sticky conversation layout](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/ConversationViewLayout.swift)
- [Signal date formatting](https://github.com/signalapp/Signal-iOS/blob/main/SignalServiceKit/Util/DateUtil.swift)
- [Signal message swipe action](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/Components/CVComponentMessage.swift)
- [Signal conversation gesture recognition](https://github.com/signalapp/Signal-iOS/blob/main/Signal/ConversationView/ConversationViewController%2BGestureRecognizers.swift)
- [Signal: Troubleshooting sending messages](https://support.signal.org/hc/en-us/articles/360009303072-Troubleshooting-sending-messages)
- [Signal Desktop English localization](https://github.com/signalapp/Signal-Desktop/blob/main/_locales/en/messages.json)

The shipped app supplied bounded comparison evidence for image/video messages,
reply, reactions, deletion, sharing, search, mentions, and media galleries.
This brief records the adopted behaviors locally.

Signal's current public support material describes sending as a distinct
message status, and its open-source localization keeps **Retry Send** in the
message options menu for an outgoing message that failed. White Noise follows
that interaction split while using the user-approved visible copy above.

## Acceptance criteria

- Every chat row opens the same conversation architecture.
- The principal header removes its timer presentation while disappearing
  messages are off, shows the compact timer and duration for an enabled direct
  chat, and appends that status after the group member count when enabled.
- Every catalog and legacy chat uses the same message shell, grouping,
  timestamp, reply, reaction, attachment, and group-identity rules.
- Direct transcripts never show member avatars. Group transcripts show an
  incoming author name only above the first message in a cluster and that
  author's avatar only beside the final message in the cluster.
- Same-author bubbles form a compact cluster while keeping fully rounded
  corners and one precise author-side edge. Separate clusters have visibly more
  breathing room. The terminal bubble's timestamp sits outside the bubble on
  its conversation-center side, inset by the shared corner radius; delivery or
  failure state also remains outside the bubble.
- Incoming gray is the adaptive `systemGray5`. Every day boundary remains an
  inline header. The last header above the viewport becomes one pinned
  regular-glass capsule, later headers remain inline, and the next header
  replaces it at the top. Headers keep equal 18-point spacing to adjacent
  transcript content and use the accepted relative/recent/old date formats.
  Separate clusters use 16 points of space.
- No sticker or shared-location fixture, catalog claim, attachment case, or
  renderer remains.
- Maya Chen covers the complete direct-message catalog; Weekend Walks covers
  the complete group/system-event catalog and gallery sizes one through seven.
- Maya includes short, multiline, and long text in both directions; replies to
  text, photo, and video; every agreed media and reaction state; both deleted
  states; and a failed outgoing message, all in chronological narrative order.
- Weekend Walks starts with **You created the group.** and interleaves every
  agreed membership, role, name, photo, and description event with the
  messages that give it context. Its dates visibly span an older full date, a
  recent weekday, Yesterday, and Today.
- Sending, drafts, replies, reactions, deletion, retry, search, attachments,
  voice playback, and row previews remain coherent through navigation.
- Media opened from Conversation or Shared Media appears in the same native
  large sheet with the composer media preview's adaptive system background.
  Each settled media page reaches both horizontal viewport edges, while
  swiping reveals the same 32-point gutter before the next page. Pulling down
  moves and dismisses the complete sheet as one system-owned surface; the
  image, toolbar, and bottom controls never separate during dismissal, and a
  zoomed image cannot dismiss interactively.
- The resting and one-line typing states have identical composer width and
  height; single-line text is vertically centered; Send replaces waveform
  inside the field; multiline entry preserves the resting corner radius, grows
  to ten visible lines, then scrolls.
- Each new visible composer line keeps the timeline bottom above the field;
  the spacing to the latest content stays constant at every line-height step,
  and the person can still scroll the timeline normally afterward.
- Opening the keyboard always brings the true timeline bottom above the
  composer, including when the person had scrolled to older history first.
- Tapping anywhere outside the focused composer dismisses the keyboard without
  consuming the tapped message, control, menu, media, or navigation action.
- A failed outgoing message shows no timestamp. A primary-label outlined
  warning icon precedes **Not delivered, hold for options**, communicating the
  state without depending on color and retaining system contrast in either
  appearance. The status is left-aligned to the outgoing message text inset,
  and touching and holding the bubble or status exposes **Retry Send** first in
  the focused message-action presentation.
- Swiping any currently replyable nondeleted message from its semantic leading
  side tracks the message without blocking vertical scrolling. The filled
  secondary-gray reply arrow reveals beneath the message, visibly and
  haptically becomes ready at 55 points, and retains only one sixth of movement
  beyond that point. Releasing there installs the same pending reply and
  focused composer as the Reply command.
  Releasing below the threshold changes no reply state. Right-to-left layout
  mirrors the gesture, Reduce Motion removes threshold scaling, and explicit
  Reply actions remain available without the gesture.
- The attachment control matches the resting field height. Send has a
  six-point visual inset at the top, bottom, and trailing edge. Voice-review
  Play mirrors it at the top, leading, and bottom edge.
- The active composer is installed with the native bottom `safeAreaBar`, and
  timeline content transitions beneath it through SwiftUI's soft progressive
  scroll-edge blur with no opaque white cutoff.
- Camera is present in the attachment menu as a genuine native sheet at the
  large detent. The system owns its corners and bottom attachment; it dismisses
  by swiping down or using Close, without a custom mask or exposed bottom strip.
  Tap captures a photo, hold captures video, and either result returns to the
  removable attachment queue.
- Camera, Photos and Videos, Files, and Contact each open from one menu
  selection. Contact presents followed White Noise profiles and queues one
  removable contact card.
- Camera is the first attachment-menu item, and selecting any item cannot also
  focus or activate the composer beneath the menu.
- Recording and voice-message bubbles both present an accessible waveform with
  two-point bars. Recording starts only after the native half-second long press;
  Stop enters review, where the draft can be played, canceled, or explicitly
  sent.
- Left and removed groups retain readable history, show the corresponding
  chronological membership event, and replace the composer with the matching
  passive current-state label.
- The chat-row preview and timestamp always derive from the latest visible
  message or typed event, including after deletion and group-management
  mutations.
- Fiatjaf and Support preserve their accepted visible content and special
  presentation where required.
