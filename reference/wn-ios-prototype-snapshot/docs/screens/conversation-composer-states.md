# Conversation composer states

## Purpose

Make every supported unsent-message state directly inspectable in deterministic
example chats. The composer remains the ordinary White Noise product surface;
scenario identifiers live only in the developer catalog timeline and brief.

## Navigation

- Composer examples appear as consecutive **Composer - …** rows in the
  developer chat catalog after **Direct - New Chat & Draft**.
- Opening any example uses the shared conversation destination and returns to
  Chats with the native Back action.
- Sending a draft appends the composed message, clears its text, attachments,
  reply, and link-preview suppression, and updates the chat row.

## Exact draft copy and states

- **Composer - Text:** **Here’s the updated plan.**
- **Composer - Multiline:** **I pulled together the notes:** followed by three
  short checklist lines.
- **Composer - Link:** **https://whitenoise.chat**, with its preview removed.
- **Composer - Link Preview:** **Worth a look:
  https://developer.apple.com/design/human-interface-guidelines**, with a rich
  preview.
- **Composer - Photo:** one captionless photo.
- **Composer - Photo Album:** **A few from today.** with four photos.
- **Composer - Mixed Media:** **Photos and a short clip from the walk.** with
  two photos and one video.
- **Composer - File:** **Here’s the brief.** with **Project Brief.pdf**.
- **Composer - GIF:** one captionless GIF.
- **Composer - Contact:** **Maya can help with this.** with Maya Chen’s contact.
- **Composer - Reply:** **Yes—Thursday afternoon works for me.** above the quote
  **CMP-REPLY: Would Thursday afternoon work?**
- **Composer - Mention:** **@Maya Chen can you take a look?** in a group chat.

The existing empty, focused, multiline growth, attachment-menu, recording,
voice-review, invitation, blocked, ended-membership, missing-relay, and message
selection states remain part of the shared composer flow.

## Native components and behavior

- `safeAreaBar` keeps the composer stationary and lets the transcript preserve
  the system scroll-edge transition.
- One continuous regular-glass composer surface owns every draft artifact:
  ordered attachments, rich link preview, quoted reply, editable text, and the
  trailing Send or waveform control. Supplemental content stacks above the
  field inside that surface; it never appears as a floating sibling card.
- A public TextKit 2 `UITextView`, bridged into SwiftUI, owns keyboard input,
  selection, focus, Return-key behavior, Dynamic Type, and growth up to ten
  visible lines. SwiftUI's ordinary `TextField` remains the default elsewhere,
  but it cannot style a selected range inside editable text.
- The text composer has exactly two resting heights: compact and expanded, with
  no persistent expansion handle. The complete composer surface accepts an
  upward pull to expand and a downward pull to collapse in both keyboard states,
  including when the draft is empty or one line. While the pull is held, the
  gesture remains mounted regardless of whether the draft is sendable, and the
  composer retains one stable view hierarchy from touch-down through settling.
  Entering flexible layout therefore cannot interrupt the first pull. A
  stationary press on the empty waveform remains the independent voice action.
  While held, the composer follows the finger continuously between the compact
  and expanded endpoints. Releasing selects an endpoint from the current
  position and projected movement, then SwiftUI's native `interactiveSpring`
  completes the remaining travel with the same interruptible, velocity-aware
  character used for system direct manipulation. Finger tracking and spring settling drive one
  continuous numeric expansion progress; changing the semantic resting endpoint
  never swaps the geometry's base height or clamps away the remaining animated
  distance. Drag translation is measured in the stable global coordinate space,
  not in the composer's changing local bounds, so resizing the surface cannot
  feed back into the finger measurement. The persistent composer does not become
  a modal `sheet`, because
  sheet presentation would add modal hierarchy, chrome, and background behavior
  that do not match an always-available message field.
- Only the compact composer's measured footprint participates in transcript
  layout. Ordinary compact growth while typing new lines continues to reserve
  more room and move the transcript as it does today. Direct manipulation and
  spring settling layer that same composer upward. Expansion captures whether
  the transcript's bottom marker was visible before the first movement and
  holds that decision through the complete presentation. When the transcript
  begins at the bottom, its viewport translates upward by the exact composer
  travel, preserving the compact gap from the newest bubble without issuing
  per-frame scroll requests. When expansion begins higher in history, the
  transcript remains fixed instead. An adaptive `systemBackground` backing in
  the same rounded shape fades in directly beneath the glass with expansion
  progress, preventing covered bubbles from reducing editor contrast while
  preserving the glass surface above it. The main regular-glass field
  owns one explicit rectangular interaction shape and one high-priority SwiftUI
  pull throughout its visible bounds, including over the bridged `UITextView`
  and nested controls. There is no second editor-pan expansion driver. A pull
  therefore has one source of translation and endpoint settling in both
  directions. The expansion pan direction-locks before recognition:
  horizontal-dominant movement fails it so the attachment shelf's native
  horizontal `ScrollView` remains the gesture owner, while vertical-dominant
  movement retains expand-and-collapse ownership. Taps still reach the editor
  and controls.
  Inline and pinned date pills remain structurally present but are visually and
  accessibly hidden from the first expansion movement until collapse settles.
  The covered transcript is also removed from the accessibility tree for that
  interval, matching its disabled touch interaction and preventing assistive
  navigation into content behind the focused composer.
- Expansion and keyboard focus are independent. With the software keyboard
  visible, the expanded surface fills the available region from 24 points
  below the navigation bar to the keyboard. Hiding the keyboard keeps the
  composer expanded and lets it extend to the bottom safe area; focusing the
  same text view restores the keyboard while keeping the top edge fixed.
  Collapsing never changes keyboard focus. The expanded `UITextView` scrolls
  vertically within the remaining surface without replacing the editor or
  losing its selection. The glass pull takes precedence over editor scrolling
  while composer expansion is available; outside taps and the accessibility
  action remain the keyboard-dismissal routes.
- Tapping the transcript outside the composer dismisses the keyboard without
  consuming the underlying message interaction. Composer controls, including
  the attachment menu, editor, and Send action, never count as an outside tap
  and therefore do not dismiss the keyboard implicitly. When expanded, the
  empty leading lane above the attachment control is also outside the editor:
  tapping it dismisses the keyboard and returns the composer to its compact
  state, while tapping the attachment control still opens its menu. Throughout
  direct manipulation and settling, the transcript is noninteractive and
  scrolling beneath the composer is disabled. A drag outside the glass does
  nothing instead of competing with the composer pull or covered messages.
- The attachment menu continues to open the public camera flow,
  `PhotosPicker`, file importer, and contact picker.
- Selected attachments appear in one horizontally scrolling, ordered,
  removable shelf. Photos, video, files, GIFs, and contacts retain recognizable
  SF Symbol or content previews; multiple photos stay separately removable.
  Visual-media previews keep one compact fixed height whether the keyboard is
  open or closed; each preview's decoded source aspect ratio determines its
  width, with stored dimensions used only when the source size is unavailable.
  The photo, video thumbnail, or GIF fills its own rounded preview with no gray
  wrapper, and the shelf scrolls horizontally when its content exceeds the
  composer. The scrolling shelf clips only its two top corners to the same
  rounded boundary as the composer; its bottom edge remains straight against
  the separator, and partially scrolled media never draws outside the glass
  surface. A slightly emphasized semantic system separator with comfortable
  side insets separates a visual-media shelf from the editable caption below
  it. Tapping a visual preview dismisses the keyboard and opens a
  native full-screen modal viewer on that exact item. The viewer uses a paged
  horizontal `ScrollView` with view-aligned targets, reserves the complete
  region above the thumbnail navigator for the selected page, fits the complete
  selected media inside that region, and keeps a horizontally scrolling
  thumbnail navigator below it. A rested page uses the full viewport width with
  no persistent leading or trailing inset; a 32-point gutter appears only
  between page targets while swiping. The viewer title is **Preview**. The
  navigator has generous leading and trailing margins, compact inter-thumbnail
  spacing, and centers a short collection. Current-page emphasis uses a subtle
  animated enlargement without an outline, plus one extra normal spacing unit
  on each horizontal side so the neighboring gaps are doubled. A check on the
  displayed media stages whether that item remains in the draft. This custom overlay matches
  the user-approved Messages reference with a compact 22-point black-accent
  circular surface, white border and checkmark, subtle contrast shadow, and a
  44-point hit target; its visible circle sits approximately 10 points from the
  media's bottom and trailing edges. A standard leading `xmark`
  cancels every staged change and closes the viewer; a native trailing
  black-accent Liquid Glass confirmation check applies the staged selection,
  removes excluded items from the composer, and closes the viewer. No composer
  attachment changes before confirmation. There is no Markup, Edit, or send
  action in the viewer. Because the viewer's fixed content does not scroll
  beneath its navigation controls, the navigation-bar background stays hidden
  and the pager explicitly suppresses its automatic top scroll-edge effect, so
  neither system contributes a redundant top surface or separator. The
  explicit close control on each composer preview remains the direct removal
  route outside the viewer.
- The compact focused composer retains the vertical budget of ten body-text
  lines. When a thumbnail shelf is present, its roughly four-line height is
  subtracted from that budget, leaving the caption up to six visible lines
  before it scrolls. In the expanded composer, the same fixed shelf remains at
  the top and the editable caption receives the remaining flexible height. The
  shelf itself has an explicit content height so its horizontal `ScrollView`
  cannot expand into unused transcript or editor space.
- The first eligible `https` URL in a text-only draft produces one deterministic
  compact rich-link preview. Apple’s `LPLinkView` remains the reference for
  metadata semantics, but its public API does not expose the user-approved
  horizontal arrangement. The accepted custom exception uses a native SwiftUI
  `HStack`: square artwork on the leading side, title and domain on the trailing
  side, adaptive system fill, and the shared close control. White Noise does not
  fetch metadata in this prototype. Closing the preview suppresses only that
  URL’s preview and leaves the typed URL intact.
- Sending an unsuppressed link includes the deterministic rich-preview
  attachment. Sending a suppressed link sends only its text.
- A reply quote remains directly above the input, inside the composer surface,
  and has a native close button. SwiftUI has no public quoted-reply component,
  so the accepted custom exception is an adaptive-gray rounded panel that
  mirrors the sent-bubble quote hierarchy: author above an excerpt of up to two
  lines, natural content height, and a leading semantic capsule that grows only
  with the quote. System text and the native button still own type and
  interaction.
- Every removable composer element uses a smaller 20-point flat `xmark`
  circle, no Liquid Glass, a comfortable visual inset, and a separate 44-point
  hit region. Utility surfaces use the quiet secondary-system treatment. Photo,
  video, and GIF previews use the user-approved Messages-inspired media
  exception: a stronger dark-neutral scrim, white glyph, and subtle contrasting
  edge so the control remains visible over arbitrary imagery without becoming
  glass. File and
  contact cards grow with their content from 104 to at most 160 points while
  keeping 12 points of internal padding at both horizontal edges. This
  preserves a clear trailing lane for the control target without moving or
  shrinking centered content. Contact names tail-truncate at the maximum
  width. Long filenames preserve their extension and the final three
  pre-extension characters while truncating the earlier stem. The close region
  alone removes content. The rest of a link, file, contact, quote, photo,
  video, or GIF never behaves as an implicit remove action.
- Complete group mentions use the same semantic gray rounded inline emphasis
  as mentions in sent message bubbles. The editable field applies UIKit's
  public `textHighlightStyle` through TextKit 2, with adaptive system fill and
  text colors rather than a fixed mention color.
- The editable text container keeps a small symmetric horizontal inset so a
  mention at either text edge retains both rounded highlight corners.
- The Send button appears for text, rich link, or any queued attachment. The
  waveform remains the empty-composer trailing action.

## Product comparisons adopted for this screen

The user requested a bounded Signal reference pass. Adopt only these durable
interaction conclusions:

- A generated link preview is optional and individually removable before send.
- A quoted reply is shown in the compose area and can be cancelled there.
- Media can be drafted with an optional caption and multiple selected items.
- Signal establishes the value of inspecting selected media at a useful size,
  but Apple Messages provides the accepted separation between a compact draft
  shelf and a dedicated full-screen inspection mode. White Noise adopts
  Messages' staged include/exclude selection and explicit completion, while
  omitting Markup and Edit.

White Noise does not copy either comparison app's visual metrics, colors,
editing tools, or private implementation. Apple components and the local
adaptive monochrome conversation treatment remain authoritative.

Comparison sources:

- [Signal Link Previews](https://support.signal.org/hc/en-us/articles/360022474332-Link-Previews)
- [Signal Reply to a specific message](https://support.signal.org/hc/en-us/articles/6851465208986-Reply-to-a-specific-message)
- [Signal Broadcast Media](https://support.signal.org/hc/en-us/articles/360044640011-Broadcast-Media)
- User-supplied Apple Messages composer screenshots and screen recording,
  reviewed 2026-08-14.

## Important states

- Empty and nonempty text, single-line and multiline.
- Raw `https` URL with a preview and the same URL with preview suppressed.
- Captionless and captioned attachment drafts.
- Compact media shelf with the keyboard both open and closed, plus the modal
  full-screen media viewer.
- Compact and expanded text composers with the keyboard both open and closed.
- Expansion committed by an upward composer drag, cancelled by an insufficient
  drag, and collapsed by a downward composer drag, for empty, one-line, and
  multiline drafts.
- Viewer selection unchanged, one or more items staged for exclusion, cancelled
  staged changes, and confirmed staged changes including exclusion of every
  visual item.
- Single photo, multiple photos, mixed photo/video, file, GIF, and contact.
- Reply quote plus text and group mention text.
- Compact reply layout at every available transcript height; its leading accent
  can never claim unbounded vertical space.
- Attachment removal, link-preview removal, reply cancellation, and send.
- Existing recording, voice review, invitation, selection, recovery, and ended
  membership replacements.

## Accessibility

- Every remove control has a content-specific label and a 44-point interaction
  frame.
- The attachment shelf announces its item count; each item retains its concise
  content label.
- The rich link preview is one read-only element whose label includes its title
  and domain; **Remove Link Preview** is a separate button.
- Reply cancellation is labeled **Cancel Reply**.
- Every visible close control keeps its 44-point hit region even though its
  translucent circular surface is visually smaller. VoiceOver names the exact
  item removed.
- The media viewer's cancellation action is labeled **Cancel Media Changes**
  and its confirmation action is labeled **Apply Media Selection**. The
  displayed item's selection control announces whether that item is included
  and explains that changes apply only after confirmation. Each page and
  thumbnail announces its position in the draft, current-page state, and
  included state.
- Mention emphasis remains supplemental: the complete `@Name` text is still
  exposed by the native editable-text accessibility element.
- The composer exposes custom VoiceOver actions named **Expand Message** or
  **Collapse Message** according to its current endpoint. When expanded and
  focused, it also exposes **Hide Keyboard**, so the drag is not the only route
  to either state even though no handle is visible.
- Link and attachment meaning never relies on color, and standard controls own
  focus, keyboard, and Dynamic Type behavior.

## Relevant Apple sources

- [safeAreaBar](https://developer.apple.com/documentation/swiftui/view/safeareabar(edge:alignment:spacing:content:))
- [overlay(alignment:content:)](https://developer.apple.com/documentation/swiftui/view/overlay(alignment:content:))
- [contentShape(_:eoFill:)](https://developer.apple.com/documentation/swiftui/view/contentshape(_:eofill:))
- [DragGesture](https://developer.apple.com/documentation/swiftui/draggesture)
- [UIGestureRecognizerRepresentable](https://developer.apple.com/documentation/swiftui/uigesturerecognizerrepresentable)
- [UIGestureRecognizerDelegate](https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate)
- [UIPanGestureRecognizer](https://developer.apple.com/documentation/uikit/uipangesturerecognizer)
- [gestureRecognizerShouldBegin(_:)](https://developer.apple.com/documentation/uikit/uigesturerecognizerdelegate/gesturerecognizershouldbegin(_:))
- [interactiveSpring](https://developer.apple.com/documentation/swiftui/animation/interactivespring)
- [withAnimation(_:completionCriteria:_:completion:)](https://developer.apple.com/documentation/swiftui/withanimation(_:completioncriteria:_:completion:))
- [simultaneousGesture(_:including:)](https://developer.apple.com/documentation/swiftui/view/simultaneousgesture(_:including:))
- [scrollDisabled(_:)](https://developer.apple.com/documentation/swiftui/view/scrolldisabled(_:))
- [offset(x:y:)](https://developer.apple.com/documentation/swiftui/view/offset(x:y:))
- [accessibilityHidden(_:)](https://developer.apple.com/documentation/swiftui/view/accessibilityhidden(_:))
- [scrollDismissesKeyboard(_:)](https://developer.apple.com/documentation/swiftui/view/scrolldismisseskeyboard(_:))
- [UITextView](https://developer.apple.com/documentation/uikit/uitextview)
- [Adjusting layout with the keyboard layout guide](https://developer.apple.com/documentation/uikit/adjusting-your-layout-with-keyboard-layout-guide)
- [NSAttributedString.TextHighlightStyle](https://developer.apple.com/documentation/foundation/nsattributedstring/texthighlightstyle)
- [textHighlightAttributes](https://developer.apple.com/documentation/uikit/uitextview/texthighlightattributes)
- [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker)
- [ScrollView](https://developer.apple.com/documentation/swiftui/scrollview)
- [Modality](https://developer.apple.com/design/human-interface-guidelines/modality)
- [fullScreenCover(item:onDismiss:content:)](https://developer.apple.com/documentation/swiftui/view/fullscreencover%28item%3Aondismiss%3Acontent%3A%29)
- [containerRelativeFrame(_:alignment:)](https://developer.apple.com/documentation/swiftui/view/containerrelativeframe(_:alignment:))
- [ViewAlignedScrollTargetBehavior](https://developer.apple.com/documentation/swiftui/viewalignedscrolltargetbehavior)
- [scrollPosition(id:anchor:)](https://developer.apple.com/documentation/SwiftUI/View/scrollPosition%28id%3Aanchor%3A%29)
- [ToolbarItemPlacement.confirmationAction](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/confirmationaction)
- [ToolbarItemPlacement.cancellationAction](https://developer.apple.com/documentation/swiftui/toolbaritemplacement/cancellationaction)
- [aspectRatio](https://developer.apple.com/documentation/swiftui/view/aspectratio(_:contentmode:)-6j7xz)
- [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)
- [fileImporter](https://developer.apple.com/documentation/swiftui/view/fileimporter(ispresented:allowedcontenttypes:allowmultipleselection:oncompletion:))
- [LPLinkView](https://developer.apple.com/documentation/linkpresentation/lplinkview)
- [Applying Liquid Glass to custom views](https://developer.apple.com/documentation/SwiftUI/Applying-Liquid-Glass-to-custom-views)
- [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- [Motion](https://developer.apple.com/design/human-interface-guidelines/motion)

## Observable acceptance criteria

- Twelve consecutive composer example chats open with the exact drafts above.
- The two link chats prove both rich-preview and intentionally suppressed states.
- The rich-link preview keeps one square artwork preview on the leading side and
  its title and domain on the trailing side instead of stacking artwork above
  text.
- The attachment examples show the right number and type of removable items
  before sending.
- Photo, photo-album, mixed-media, and GIF drafts use the same compact,
  horizontally scrolling shelf with the keyboard open or closed.
  Horizontal-dominant drags move that shelf without changing composer height;
  vertical-dominant pulls beginning on the shelf retain the normal composer
  expansion and collapse behavior. Visual previews share one height, derive
  their widths from their decoded source
  aspect ratios with stored dimensions as fallback, fill their rounded bounds,
  and retain their individual remove
  controls. Partially scrolled previews remain inside the composer's rounded
  top corners without rounding or cutting away the shelf's bottom edge. A
  slightly stronger, comfortably inset divider visibly separates the
  shelf from caption text. Tapping any visual preview opens a full-screen viewer
  titled **Preview** on that exact item. The selected
  media fills the available page region above the bottom thumbnails without
  disappearing or being displaced. A settled item reaches both horizontal
  viewport edges without internal side padding caused by stale metadata;
  swiping reveals a 32-point gutter before the next full-width page. Tapping a
  bottom thumbnail selects its page, current-page emphasis animates without an
  outline, and the selected thumbnail receives double the normal horizontal
  gap on both sides while other thumbnails use tighter inter-item spacing.
  The thumbnail row retains comfortable leading and trailing margins and
  centers when all items fit. Toggling the displayed item's check stages an
  include/exclude change without immediately changing the composer. The leading
  X closes and discards those changes. The trailing confirmation check applies
  them, removes only excluded visual items, and leaves text plus other
  attachments unchanged. The per-item control uses the approved black-accent
  selection treatment and stays optically aligned near the image's
  bottom-trailing edge while retaining its 44-point target. The viewer fits
  complete media and contains no Markup, Edit, or send action.
  Its title and actions remain native navigation-bar items without a visible
  bar background or top scroll-edge divider over the otherwise continuous
  screen.
- Compact focused media drafts never exceed the ordinary ten-line composer
  budget: the thumbnail shelf consumes about four lines and the caption can use
  up to six. Expanding keeps the shelf fixed and gives the caption the remaining
  surface height.
- No expansion handle is visible. The complete composer surface recognizes the
  same directional pull for empty, one-line, and multiline drafts at both
  endpoints and with the keyboard open or closed, tracks the finger continuously
  while held, and clamps at both endpoints. Releasing chooses the resting
  endpoint using projected movement and completes the remaining travel with the
  native interactive spring. Equivalent custom accessibility actions remain
  available. Releasing at any intermediate height continues from that exact
  presentation height with no endpoint-state jump, flash, or one-frame clamp.
  Holding the finger stationary while the composer changes height cannot move
  the reported drag position or make the surface oscillate.
- Compact line growth continues to increase the transcript's reserved bottom
  region. Beginning an expansion pull freezes that compact reservation; the
  composer grows upward as an overlay. If the bottom marker was visible at the
  start, the complete transcript viewport translates upward by precisely the
  same amount throughout the pull and spring. If expansion began higher in
  history, the transcript remains stationary and the composer's matching
  adaptive system-background backing fades from clear to opaque beneath its
  glass. That starting decision cannot change during the presentation. Every
  visible point inside the main glass field owns the same
  high-priority pull and blocks covered message bubbles; downward movement
  collapses rather than scrolling either editor or transcript. Transcript
  scrolling, message hit testing, and transcript accessibility remain disabled
  throughout every noncompact presentation. Pulling or settling cannot trigger the compact line-growth
  `scrollTo` path. Inline and pinned date pills are hidden throughout every
  noncompact presentation and return only after compact settling completes.
- Tapping or scrolling the transcript dismisses the keyboard while preserving
  the underlying message interaction. Interacting with any composer control does
  not trigger outside-tap dismissal or change the expansion target mid-animation.
  Opening the attachment menu while the editor is focused keeps the keyboard and
  composer geometry stationary, so the native menu remains anchored to the
  attachment button; only choosing a system destination may dismiss the keyboard.
  In the expanded state, tapping outside the glass, including the otherwise
  empty leading lane above the attachment button, dismisses the keyboard and
  collapses the composer. A drag beginning outside the glass cannot scroll or
  activate the transcript, while the attachment button retains its normal menu
  action.
- Expanded with the keyboard visible leaves 24 points below the navigation bar
  and ends above the keyboard. Dismissing the keyboard keeps the top edge fixed
  and extends the same composer to the bottom safe area; focusing the text view
  reverses that change without losing the insertion point or text scroll
  position. Collapsing does not change keyboard focus.
- GIF previews show a complete **GIF** stamp in the composer shelf.
- All close controls use the approved smaller flat appearance and retain the
  same corner inset, press feedback, 44-point hit region, and removal-only
  behavior. Visual media uses the high-contrast dark-neutral overlay variant;
  utility surfaces use the quieter secondary-system variant. Adaptive file and
  contact cards reserve enough
  trailing width that their close target never overlaps the icon, avatar,
  filename, or contact name, and their content retains solid left and right
  padding at every supported width.
- Every draft artifact is visibly contained by the single composer surface;
  previews, attachment shelves, and reply quotes never float above a separate
  text capsule.
- The reply quote follows the sent-bubble quote hierarchy, allows a two-line
  excerpt without compression, remains content-height, and keeps its cancel
  action aligned with the quote instead of expanding the bottom safe-area bar.
- **@Maya Chen** in the mention example uses the same adaptive gray rounded
  emphasis as the sent-message mention treatment while remaining editable.
- Sending preserves text, attachments, generated link preview, or reply target
  as appropriate and clears all unsent state.
- Removing one attachment leaves the others ordered and sendable.
- Removing a link preview leaves the URL and Send button intact.
- Existing attachment acquisition, voice, recovery, invitation, and selection
  behavior remains available.
- Completion still requires user acceptance after hands-on inspection.
