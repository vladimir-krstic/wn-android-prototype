# Composer, attachments, media, and speech

Status: Current Pixel 8a device inspection passed on 2026-08-31 for the earlier
compact, IME-visible, attachment-menu, recording, review, transcription,
format-menu, 200%-type dark, RTL, and forced wider-window states. The subsequent
rich-content accessory/review refinement has a clean static gate; renewed
device inspection and user visual acceptance remain pending.

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/conversation-composer-states.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-shared.md`
- `wn-ios-prototype@0bd7cba:docs/screens/speech-messages.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeComposerTextView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeComposerMediaViewer.swift`
- composer, attachment, media, and voice cases in `PrototypeChatModelTests.swift` and `ChatFlowsUITests.swift`
- user-approved current-iOS comparison at `wn-ios-prototype@4c25393f0eb6` for
  rich composer accessories, reply geometry, draft-media review, and recipient
  speech-action placement; this scoped evidence does not repin the Android
  baseline

The iOS source establishes product states, ordering, format choices, and
two-endpoint behavior. Android owns presentation, accessibility, Back, IME,
pickers, popup placement, focus, motion, and safe-area handling.

## Android contract

### Host and editor

- The available composer is a transparent overlay above the conversation;
  there is no full-width backing or top divider. The separate Add control and
  editor capsule own their adaptive Material containers. Idle Add uses the
  primary/on-primary pair, consistent with the app's filled actions, while the
  editor capsule uses the lower-emphasis 1 dp `outlineVariant` role. This keeps
  the input boundary visible over mixed timeline media without adding a
  full-width scrim. Search, selection,
  invitation, blocked, ended, and recovery bars remain ordinary Scaffold
  bottom content.
- The timeline viewport paints to the physical bottom edge behind the floating
  composer and gesture area. Safe-bottom plus compact-composer clearance lives
  in LazyColumn content padding, keeping the last item reachable above the
  controls without clipping content into a white bottom cutout. The composer
  itself remains inset from system navigation and follows the IME.
- The compact row uses 16 dp screen margins, a separate circular 48 dp Add
  action, an 8 dp gap, and a rounded 48 dp-minimum editor capsule. Send and
  waveform controls retain 48 dp targets; the idle waveform artwork is 24 dp,
  matching Material's Add icon footprint.
- A Foundation text editor preserves the existing draft, selection,
  composition, mention styling, focus, and external updates without inheriting
  Material TextField's larger container. Compact budgets are ten text lines,
  six caption lines with attachments, and eight transcript lines; overflow
  scrolls internally. Complete member mentions use the adaptive medium-neutral
  `outlineVariant`/`onSurface` pair and the same shared 4 dp rounded glyph-run
  renderer as conversation search; Compose's square span background is not
  used.
- The composer has exactly two endpoints. Compact is content-driven. Expanded
  begins 24 dp below the chat header and ends above the IME or bottom safe
  area. A direction-locked vertical drag settles with an interruptible spring,
  a projected 48 dp threshold, and midpoint fallback. The attachment shelf
  retains horizontal scrolling.
- If the newest message was visible when expansion began, the timeline moves
  upward by the composer's exact travel. History otherwise remains stationary.
  Timeline scroll, hit testing, date overlays, and accessibility focus are
  unavailable during drag or settling.
- Expansion does not change editor focus. Back dismisses an app menu or IME
  first, then collapses an expanded composer, then leaves the conversation.
  Insets are consumed once by the overlay host so the header does not pan when
  the keyboard opens.
- TalkBack receives ordinary Expand Message, Collapse Message, Hide Keyboard,
  and Start recording alternatives. Visible controls retain 48 dp targets.

### Attachments and previews

- Add opens the shared anchored Material menu directly above its trigger with
  a 10 dp visible gap, in this exact order: Camera, Photos and videos, Files,
  Contact. A composer-scoped above-anchor provider bypasses the stock popup's
  48 dp window-edge fallback while retaining Material's popup, grouped items,
  focus, RTL, outside/Back dismissal, and lower-edge motion origin. The upward
  group retains Material's native menu shadow so it separates from timeline
  content without changing the visible gap above Add. GIF acquisition is removed; persisted GIF drafts, fixtures,
  received GIFs, rendering, and media review remain.
- Camera uses `TakePicture`, visual media uses Android Photo Picker with a
  20-item limit, and files use `OpenMultipleDocuments`. The merged app already
  declares `CAMERA` for the approved QR scanners, so Android also gates the
  external capture intent on that permission. Camera therefore requests access
  just in time after selection, launches capture immediately after approval,
  and provides Allow Camera or Open Settings recovery after denial. The
  attachment flow introduces no storage, microphone, network, notification, or
  location permission. Contact selection remains deterministic in an expanded
  Material sheet. The
  sheet uses the app's neutral canvas, a 56 dp searchable field, and one
  white-equivalent segmented contact group with names, short public keys, and
  2 dp canvas-tone separators.
- The ordered shelf keeps exact-item review, include/exclude staging,
  cancellation, preparation failures, and atomic clear after send. Visual
  media is 112 dp high with aspect-derived 68–200 dp widths; utility cards are
  72 dp high with 104–160 dp widths. The shelf has 8 dp padding/gaps, clipped
  rounded top corners, a visual-media-only 1 dp inset separator, and a visible
  removal affordance inside a 48 dp target. Photo, album, GIF, file, contact,
  link-preview, and reply cancellation all use one shared accessory: a
  transparent 48 × 48 dp semantic target, a 20 × 20 dp visible circle, a 12 dp
  close symbol, and equal 6 dp top/end insets. Its indication is clipped to the
  visible circle while the full target remains operable.
- Picked and camera photos use a message-media import policy: preserve aspect
  and EXIF orientation, keep up to 4096 px on the long edge, preserve PNG/alpha
  losslessly, and encode other photos as JPEG at quality 95. The existing
  512 px avatar policy remains unchanged. Encoded media stays in memory through
  review and send; thumbnail bitmaps are sampled to their displayed bounds and
  decoded off the main thread, while gallery pages use their larger viewport.
  Earlier 512 px imports have to be selected again to regain source detail.
- Utility cards keep their accepted 72 dp height without vertical content
  clipping. Contact cards show a 40 dp avatar or monogram and a visible
  single-line name. File cards keep the extension plus the final three stem
  characters visible while only the leading stem ellipsizes.
- Composer replies and timeline reply cards share one natural-height quote
  block: a 3 dp capsule begins 12 dp from the card edge, followed by a 10 dp
  text gap, author, and a two-line excerpt. Incoming/outgoing roles remain
  adaptive; composer cancellation overlays the top end without moving the
  quote geometry. The shared content does not force one container shape:
  composer quotes use the same 8 dp inset as attachment imagery and a 16 dp
  radius inside the 24 dp composer, while timeline quotes use an 8 dp inset and
  8 dp radius inside their 16 dp message bubble. Message body content remains
  aligned at 12 dp. Link previews use the composer quote's same 8 dp outer
  inset and 16 dp radius so their card is also concentric with the composer.
- Draft review fits media into the full available pager area without an added
  image margin, matching the sent-media viewer's full-width treatment while
  preserving aspect ratio. The review app bar, safe-drawing insets, thumbnail
  rail, include/exclude staging, Done, and cancellation behavior remain.
- Draft review shows no rail for one item. Multi-item review uses 56 dp targets
  containing 48 dp cropped images, with no extra gap between targets so the
  visible images are 8 dp apart. The rail is 72 dp high including 8 dp padding
  above and below. Only the selected image receives a 1 dp `onBackground`
  ring; unselected images have no frame. These smaller thumbnails, tighter gaps,
  and thinner outline follow the user's 2026-09-03 direction. Inclusion is a 22 dp circular
  check inside a 48 dp target, with explicit inclusion state and hint. The
  target follows the fitted preview image rather than the pager bounds, placing
  the visible check inside its bottom-end corner with 6 dp from both edges,
  a 50% increase requested on 2026-09-03. The 48 dp touch target is unchanged.
- A single-finger downward swipe in the media area closes Preview with the
  same cancellation behavior as Close/Back; staged exclusions are discarded.
  Only Done applies selection changes. Short pulls return with Material motion,
  and horizontal paging retains priority. Pager pages and the thumbnail rail
  clip to their bounds and disable edge stretching, so other media cannot draw
  outside the viewport during a pull. The backdrop remains opaque; thumbnail
  navigation is disabled while the pull settles. Both gallery variants share
  this explicit user-requested dismissal gesture.
- Deterministic link previews appear only for text-only drafts. Adding an
  attachment suppresses the card without losing draft text or the user's
  per-link suppression choice.

### Inline deterministic voice

- Voice is one internal state machine: Idle, Recording, Review. It remains a
  deterministic local simulation and never initializes microphone capture,
  speech recognition, networking, or background work.
- Empty state exposes a code-native waveform. Physical touch must remain
  inside a 32 dp tolerance for 400 ms; a normal tap does not record. TalkBack
  receives an ordinary Start recording action.
- Recording forces compact mode, hides the keyboard and Add action, and shows
  a 24 dp-high red waveform with dense 2 dp bars/gaps, a monospaced elapsed
  timer, and a red 20 dp Stop symbol inside a 48 dp inline `IconButton`.
  Elapsed state advances every 100 ms with no artificial 59-second cap. The
  waveform shifts one deterministic trailing sample every 200 ms instead of
  regenerating every bar, producing calmer, natural motion.
- Stop opens inline Review. Add becomes Cancel. Before transcription, Review
  shows a 32 dp filled Play/Pause circle in a 48 dp target, waveform, remaining
  duration, a centered light-gray chat-bubble **Transcribe** action, and a
  circular upward-arrow Send action. Transcription has a short deterministic
  progress state, inserts the pinned editable sample, and defaults to Both.
- The visible press treatment for **Transcribe** is a 32 dp-minimum clipped
  pill with 8 dp horizontal and 4 dp vertical content padding. Its transparent
  outer target remains at least 48 dp and adds 4 dp horizontal breathing room,
  so the state layer no longer fills the large accessibility target. The
  control can still grow vertically for large text.
- The chat-bubble/progress symbol and Transcribe label have one shared 8 dp
  relationship. No second spacer is inserted inside the pill.
- After transcription, the Message Format control sits above playback/text.
  It uses the same split target/visual-pill treatment as Transcribe: a 48 dp-
  minimum target and a 32 dp-minimum clipped state layer around its label and
  chevron.
  Its shared anchored menu has the same visibly clear 2 dp attachment above
  the control and offers
  Voice, Text, Both. Voice shows
  playback only and forces compact mode. Text shows the editor. Both shows
  playback and editor and supports the same two-endpoint pull expansion.
  Blank transcript disables Text/Both submission but never Voice.
- Submission creates exactly one timeline entry: voice attachment only, text
  only, or one voice attachment plus ordinary message text. The simulated
  duration is retained. Cancel, send, navigation, or loss of availability
  clears playback and transient review state.
- Expansion endpoint and Review are saveable per chat. Recreation during an
  active simulation converts elapsed recording to Review without sending or
  silently discarding it.

### Message speech presentation

- Playback remains the only permanent command inside a received voice bubble.
  Read Aloud, Stop Reading, Transcribe, Show/Hide Transcript, and Copy
  Transcript are conditional focused-message actions, matching the product
  command placement without expanding every bubble.
- As requested on 2026-09-03, Read Aloud applies to both sent and received
  messages with authored text and no voice attachment, including replies,
  links, and media/file captions. It follows Copy in the focused menu and is
  also a named accessibility action. Starting speech uses the existing visible
  plain-text projection; active speech replaces the command with Stop Reading
  and shows the existing compact progress row. Empty/deleted messages do not
  offer it. Existing received-voice transcript actions keep their eligibility.
  This extends the pinned iOS baseline's recipient-only rule under the user's
  current direction, without repinning other iOS behavior.
- Android TextToSpeech remains the platform implementation. The manifest
  declares the required `android.intent.action.TTS_SERVICE` query so Android
  11+ can discover installed engines. Start is available after successful
  engine/language initialization; Stop remains available for active speech.
  No permission or dependency is added.
- A deliberate Transcribe action creates only view-local transcript state.
  Show reveals the transcript beneath playback; Hide removes it again; Copy
  uses the revealed local text. Text messages show only a compact live
  read-aloud progress row while playback is active.
- Voice playback and Read Aloud progress tracks omit the fixed end dot, as
  requested on 2026-09-03, using Material's `drawStopIndicator` override.
  Their progress, duration, and accessibility behavior stay the same.

## Deterministic catalog and compatibility

- Preserve all twelve accepted Composer rows and exact draft text.
- Preserve the ordered photo, four-photo album, mixed image/video, file, GIF,
  contact, reply, link-preview, and suppressed-link artifacts.
- Keep the `Chat` draft schema and `MessageAttachmentKind` unchanged. The
  internal `VoiceDraftSubmission` carries format, transcript, and duration;
  the fixture helper still defaults to eight seconds when no duration is given.
- Draft list previews continue to derive from authoritative text/attachments.

## Acceptance evidence

- The 2026-09-03 import-quality correction passes 183 unit tests, lint, app
  assembly, and instrumentation-test APK compilation. `MediaImageSamplingTest`
  covers full-screen detail, thumbnail allocation, panorama crops, and invalid
  bounds. `ConversationImageImportTest` compiles full-resolution, exact PNG
  pixels, independent avatar limits, EXIF/large-photo bounds, and invalid-input
  cases; execution and current-build visual inspection remain pending.
- The 2026-09-03 draft-preview width correction removes the image's 16 dp
  margin in `DraftMediaViewer`, retaining aspect-fit rendering and all review
  interactions. `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
  `assembleDebugAndroidTest` pass. Current-build device inspection and user
  visual acceptance remain pending.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
  `assembleDebugAndroidTest` pass on 2026-08-31. The unit suite passes and lint
  reports no errors. Instrumentation sources include
  the transparent host; the 10 dp Add gap and format selector's 2 dp anchor/
  10 dp visible-pill gap; 20/24/32/48 dp
  Stop, waveform, Play and target geometry; format ordering; and the expanded
  searchable grouped contact sheet.
- Before the latest optical follow-up, the focused `ConversationScreenTest`
  ran 30/30 scenarios on the Android 17 Pixel 10 Pro XL emulator. That gate
  covered expansion, IME and Back priority, the exact four-item menu/no GIF,
  horizontal shelf gestures, all twelve fixtures, hold-to-record, inline
  record/review, transcription, playback, all formats,
  cancellation/submission, accessibility actions, 200% type, RTL, light/dark,
  and compact/wider constraints. The updated instrumentation APK compiles, but
  was not installed or run for this follow-up.
- The earlier hands-on inspection covered gesture navigation; keyboard shown/hidden;
  compact/expanded text; multi-photo shelf; attachment menu; live recording;
  pre/post-transcription review; Voice/Text/Both; dark appearance; large text;
  and a wider resizable window. Those captures predate the current arrow/Stop,
  filled Play, Transcribe, attached format/Add popup, shadow, and contact-sheet
  adjustments in WN-ANDROID-0097.
- A repository-wide instrumentation run also reaches unrelated legacy test
  classes whose `MainActivity` harness already owns `setContent`, or whose old
  assertions no longer match current screens. Those pre-existing failures are
  outside this flow; the isolated composer host prevents them from obscuring
  the 30-scenario composer result.

- Camera capture now accounts for the merged manifest's QR-scanner `CAMERA`
  declaration: the composer requests access from the explicit Camera action,
  retains the pending capture across the permission/settings round trip, and
  only creates and launches the FileProvider-backed `TakePicture` request once
  access is granted. Static verification for this regression is recorded in
  WN-ANDROID-0104; renewed hands-on capture remains pending.
- The rich-content follow-up adds unit coverage for suffix-preserving filenames
  and compiled Compose coverage for shared 48/20/12/6 dp removal geometry,
  unclipped utility labels at 200% type/RTL, shared quote-bar geometry, clean
  selected-only draft thumbnails, fitted-image bottom-end inclusion geometry,
  inclusion semantics, and the hidden single-item rail. `testDebugUnitTest`,
  `lintDebug`, `assembleDebug`, and
  `assembleDebugAndroidTest` are the required static gate; these updated states
  have not yet been visually inspected.
- The mention-highlight follow-up adds exact-name/range unit coverage and a
  compiled pixel regression for the adaptive medium-gray background,
  `onSurface` foreground, and shared rounded-corner treatment. Device
  inspection of this state remains pending.

Sources: [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures),
[image scaling](https://developer.android.com/develop/ui/compose/graphics/images/customize),
[layout constraints and modifier order](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers),
[gesture animation](https://developer.android.com/develop/ui/compose/animation/advanced),
[window insets](https://developer.android.com/develop/ui/compose/system/insets-ui),
[Material menus](https://developer.android.com/develop/ui/compose/components/menu),
[Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker),
[Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/semantics),
and [touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
