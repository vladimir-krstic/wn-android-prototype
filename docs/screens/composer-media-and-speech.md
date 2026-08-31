# Composer, attachments, media, and speech

Status: Current Pixel 8a device inspection passed on 2026-08-31 for compact,
IME-visible, attachment-menu, recording, review, transcription, format-menu,
200%-type dark, RTL, and forced wider-window states. Automated static and
connected gates pass; user visual acceptance remains separate.

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/conversation-composer-states.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-shared.md`
- `wn-ios-prototype@0bd7cba:docs/screens/speech-messages.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeComposerTextView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeComposerMediaViewer.swift`
- composer, attachment, media, and voice cases in `PrototypeChatModelTests.swift` and `ChatFlowsUITests.swift`

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
  scrolls internally.
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
  20-item limit, and files use `OpenMultipleDocuments`. No camera, microphone,
  storage, network, notification, or location permission is introduced.
  Contact selection remains deterministic in an expanded Material sheet. The
  sheet uses the app's neutral canvas, a 56 dp searchable field, and one
  white-equivalent segmented contact group with names, short public keys, and
  2 dp canvas-tone separators.
- The ordered shelf keeps exact-item review, include/exclude staging,
  cancellation, preparation failures, and atomic clear after send. Visual
  media is 112 dp high with aspect-derived 68–200 dp widths; utility cards are
  72 dp high with 104–160 dp widths. The shelf has 8 dp padding/gaps, clipped
  rounded top corners, a visual-media-only 1 dp inset separator, and a visible
  removal affordance inside a 48 dp target.
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

## Deterministic catalog and compatibility

- Preserve all twelve accepted Composer rows and exact draft text.
- Preserve the ordered photo, four-photo album, mixed image/video, file, GIF,
  contact, reply, link-preview, and suppressed-link artifacts.
- Keep the `Chat` draft schema and `MessageAttachmentKind` unchanged. The
  internal `VoiceDraftSubmission` carries format, transcript, and duration;
  the fixture helper still defaults to eight seconds when no duration is given.
- Draft list previews continue to derive from authoritative text/attachments.

## Acceptance evidence

- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
  `assembleDebugAndroidTest` pass on 2026-08-31. The current unit suite contains
  137 passing tests and lint reports no errors. Instrumentation sources include
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

Sources: [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures),
[gesture animation](https://developer.android.com/develop/ui/compose/animation/advanced),
[window insets](https://developer.android.com/develop/ui/compose/system/insets-ui),
[Material menus](https://developer.android.com/develop/ui/compose/components/menu),
[Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker),
[Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/semantics),
and [touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
