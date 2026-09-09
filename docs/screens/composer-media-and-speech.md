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

- The available composer is one full-width rounded Material surface inside
  16 dp screen margins. Empty and unfocused, it uses one reading row with
  Add, emoji, Message, dictation and voice inside the surface. Focusing it or
  adding draft content places the text above the internal action row.
  The outer host stays transparent over the timeline. The shared surface uses
  `surfaceContainerHigh` with a 1 dp `outlineVariant` border and 24 dp radius.
  Add is unfilled inside it. Search, selection, invitation, blocked, ended and
  recovery bars remain ordinary Scaffold bottom content.
- The timeline viewport paints to the physical bottom edge behind the floating
  composer and gesture area. Safe-bottom plus compact-composer clearance lives
  in LazyColumn content padding, keeping the last item reachable above the
  controls without clipping content into a white bottom cutout. The composer
  itself remains inset from system navigation and follows the IME.
- The reading row starts at 48 dp minimum, with Message immediately after
  emoji. In editing mode, text uses 14 dp horizontal and 12 dp vertical content
  insets above a 48 dp action row. The editing composer starts at approximately
  96 dp tall before outer padding and grows
  with font size and content. Existing 32 dp Add/emoji and 40 dp trailing action
  centers remain. The surface and editor retain the same width across typing,
  clearing, dictation, recording, review, expansion and collapse. No width
  threshold, horizontal inset animation or toolbar integration animation remains.
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
  area. A direction-locked vertical drag settles with an interruptible linear tween,
  a projected 48 dp threshold, and midpoint fallback. The attachment shelf
  retains horizontal scrolling.
- If the timeline was fully at the bottom when expansion began, it moves
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

## B09 editing and reading extension

The [message editing and reading brief](message-editing-and-reading.md) adds
accepted/pending edit separation, original/revision history, full reading,
per-chat collapse, native passage selection and structured source-aware text.
The ordinary composer draft, selection, reply and attachments remain independent
of the edit task. The clean host gate passes 343 unit tests and both APKs; ten
new UI cases compile. Device/visual acceptance remains pending.

## Production B11 extension — 2026-09-04

[Composer attachment actions](composer-attachment-actions.md) extends this accepted
composition with reversible per-draft photo quality, actual byte counts, bundled
recent-media access states, device contacts, transfer recovery and decoded GIFs.
The previously accepted 4096px/JPEG95 import is now the High default. White Noise
person keeps the existing searchable sheet; Device contact uses the system phone
row and a selected-field preview. Existing camera, Photo Picker, Files, draft
shelf, voice and media-viewer behavior remain governed here. The B11 clean gate
passes 381 unit tests, zero lint errors and both APKs; new UI/platform cases are
compiled only. No historical device evidence verifies the current build.

## 2026-09-05 — draft video gallery playback parity

Explicit user direction gives composer-gallery video pages the same Media3
`MediaViewerVideo` component as sent-message gallery pages. The shared component
accepts attachment, poster and stable playback key directly; drafts do not create
fake sent-message records. Video contributes one pager frame. Photo previews and
editing remain unchanged.

Use the existing Play/Pause, seeking, skip, mute, playback speed, buffering and
compact-height controls. Start paused. Only the settled foreground page owns a
player; scrolling/dismissing, leaving the foreground or closing releases it.
Position, mute and speed reuse the existing saveable playback state. Media remains
limited to selected content URIs and the existing bundled clip, with no new
permission, dependency, network source or external-app handoff.

Composer Include/Exclude and Done/Cancel remain available. Reserve 48 dp beneath
video controls for the existing inclusion target; the thumbnail strip stays
outside the player. TalkBack touch exploration keeps controls accessible.
`MediaViewerVideoTest` adds draft playback/pause/seek, speed controls, no-overlap
with inclusion and applying exclusion through Done. Existing foreground-release
and sent-gallery regressions exercise the same player. Tests are compiled only;
current device playback/visual acceptance remains pending.

Shared implementation follows the existing [Media3 Compose player guidance](https://developer.android.com/media/media3/ui/compose).

Host validation: `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed: 909 unit tests, zero failures/errors/skips, zero lint errors (18 warnings
and two hints), and both APKs assembled. No device playback inspection performed.

## 2026-09-05 — dictation and full-width multiline composer

Explicit user direction adds a 24 dp microphone immediately before the existing
record waveform (or Send for a sendable draft), with a native 48 dp target. It
starts the existing owner-bound dictation flow with the current selection; review,
disclosure, capture exclusion and cancellation stay with the shared controller.
The attachment-menu entry was removed by the subsequent user direction below.

The fourth visual line, whether wrapped or entered, automatically widens the
text composer across the available screen margin. Measure against its compact
editor width with current typography and density even while wide, so reflow
cannot oscillate between layouts. One through three lines retain the separate
Add circle. At four or more, first grow a bottom action row, then widen the
capsule to include Add at the left. Add becomes an unfilled 24 dp icon, matching
microphone/waveform artwork, with a 48 dp touch target. Text uses the full inner
width above the controls, with the existing 14 dp horizontal and 12 dp vertical
editor insets. Keep the existing outer 16 dp margins, 24 dp capsule corners and
semantic surface/outline colors. Existing attachment and caption limits remain.

Manual expansion still reaches 24 dp below the header: height settles first,
then the surface widens. Collapse contracts width before height. A composer that
still has at least four compact-width lines returns to its content-height wide
layout after manual collapse. Downward dragging contracts width before following
height; extending an already-wide multiline composer keeps its width. Springs
are interruptible and non-bouncy; native animation scale applies. Recording and
voice review keep their established presentation and capture behavior.

Use a single Foundation editor in a small Compose Layout rather than switching
between separate Row/Column editor trees: this preserves focus, selection,
composition and mention painting. Measure the action row separately, reserve it
below text in the wide state and use relative placement for RTL. The existing
menu owner remains mounted while its Add control becomes part of the capsule.
Back, Expand/Collapse accessibility actions, IME/insets and timeline interaction
restrictions retain their existing contract.

`ComposerLayoutFlowTest` covers the explicit fourth line, soft wrapping without
reflow flicker, selection, attachment-menu access and both animation sequences.
`DictationCaptureTest` exercises the direct microphone and its position before
recording. The existing expansion-policy unit case covers the three/four-line
boundary. UI cases are compiled only; device motion/visual acceptance is pending.

Official guidance: [Compose animation](https://developer.android.com/develop/ui/compose/animation/composables-modifiers)
and [text fields](https://developer.android.com/develop/ui/compose/text/user-input).

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 909 unit tests, zero failures/errors/skips, zero lint errors
(18 warnings/three hints), and both APKs assembled. New interaction and motion
cases compile only; no device or emulator inspection was performed.

## 2026-09-05 — single dictation entry

Latest user direction removes Dictation and its pending-review variant from the
Add attachment menu. The composer microphone remains the entry for starting
dictation or reopening retained review. The menu now contains Camera, Photos
and videos, Files, Location and Contact. Updated existing dictation interaction
coverage to use the microphone, assert the absent menu entry and reopen review
through the microphone. No capture, permission or navigation behavior changes.

Host validation: `./gradlew assembleDebug assembleDebugAndroidTest` passed; the
updated interaction tests compile. No device execution was performed.

## 2026-09-05 — restore the single-row live recording UI

Latest explicit correction removes visible locked/release/drag instructions,
Cancel and Lock actions from active recording. Show only one live waveform with
Stop immediately after it, in the existing 48 dp composer row. Waveform height
remains 24 dp and Stop uses its existing 20 dp red square/48 dp accessible target.
The X remains in stopped recording review. Existing tap/hold capture, system Back
cancellation, lifecycle cleanup and review behavior remain intact.

The existing recording interaction regression now asserts absent lock/Cancel
copy, a 48 dp composer, waveform/Stop alignment and subsequent review with X.
No new UI or permission flow is introduced. Device inspection remains pending.

Host validation: `./gradlew assembleDebug assembleDebugAndroidTest` passed.
The updated recording/review interaction regression compiles; no device run.

## 2026-09-05 — live, editable composer dictation

Latest explicit user direction supersedes the earlier sample-driven dictation,
app disclosure, separate transcript/review, Done/Cancel and auto-send settings.
The composer microphone starts Android SpeechRecognizer directly (after Android's
microphone permission prompt when necessary). This is the user-authorized scope
expansion to actual dictation; the app adds RECORD_AUDIO and the recognition-service
package query, with no app INTERNET permission or backend.

Partial recognition updates the existing composer editor immediately. Each
utterance replaces its own provisional text at the captured selection instead
of appending repeated partials. Final results establish the next utterance's
insertion point. Partial-result timing depends on the installed speech service.
A pulsing semantic-error red 24 dp microphone indicates capture, followed by a
48 dp Pause action. Hide the voice-note waveform, duplicate Message placeholder
and separate transcript controls throughout dictation. The leading X uses the
same presentation as stopped voice review and exits dictation without discarding
the editable draft.

Pause releases capture and leaves text editable. Editing or moving the selection
also pauses before another recognition callback can overwrite the change. Tap
the microphone again to capture a fresh selection in the edited draft. Paused
mode offers the microphone and explicit Send when appropriate. No dictation
completion or legacy preference sends automatically. Settings now explains this
flow and links to Android voice settings instead of offering obsolete finish/
auto-send choices. Translations are updated in all five resource sets.

System Back and background/navigation pause capture; returning never restarts it
automatically. Ownership and eligibility guard every callback, and permission
results also require the current session. Membership loss releases the microphone;
sign-out/removal clears owned capture state. Provider/no-speech/permission errors
pause inline and retain draft text for retry; permanent permission denial offers
Android app settings. Preparation times out after 15 seconds. Recognition is
cancelled and destroyed when the session ends, pauses or leaves the origin.

The single native editor retains keyboard, selection, mention styling, RTL,
font scaling and compact/fourth-line/manual-expansion behavior. Existing voice
capture stays mutually exclusive through the shared owned lease. The retained
legacy deterministic reducer tests document the earlier contract, not the
current composer dictation UI.

Official sources: [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer),
[RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener)
and [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).

Evidence: eight new ComposerCaptureControllerTest cases cover revised partials,
selection replacement, final continuation without auto-send, stale callbacks,
edit/background/navigation/membership/sign-out, failure retry and voice exclusion.
DictationCaptureTest now covers inline entry, absent old controls, pause/edit/
selection/resume, retained draft on X/background, inline recovery and revised settings,
with the platform recognizer disabled in the UI test host. AppResourceIntegrityTest
expects the newly authorized audio permission. Device recognition, live partial
timing and visual acceptance remain unverified.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 917 unit tests, no failures/errors/skips, zero lint errors
(18 warnings and three hints), and both APKs assembled. The final Back-priority
change additionally passed `./gradlew lintDebug assembleDebug assembleDebugAndroidTest`.
Interaction tests compile only. No device recognition, installation or visual
inspection was performed.

## 2026-09-06 — dictation silence and control order

Latest user direction: do not show a transcription failure as soon as speech
stops. Allow five seconds without newly transcribed text. If this Start/Resume
has produced any partial or final transcript, pause quietly and keep it editable;
otherwise show the existing “No speech was recognized.” error. Pre-existing draft
text does not count as recognition. Fresh Start/Resume begins a new window after
recognizer readiness; permission and initial preparation do not consume it.

The controller uses a monotonic clock and carries the inactivity deadline and
recognized-text flag across provider utterances. Revised nonblank text resets
the window; repeated or blank partials do not. Empty final results, no-match,
speech-timeout and generic provider failures retry within the same window, with
a 250 ms retry delay to avoid a tight service restart loop. Partial text becomes
the next insertion anchor on retry. Explicit permission, network, microphone and
service failures retain their specific recovery. Terminal provider callbacks are
accepted once; stale callbacks and timers cannot affect a replacement request.

Place the standard 48 dp Pause action before the 48 dp animated-microphone slot
in logical layout order, preserving native RTL mirroring, labels and tap targets.
The editor, X, explicit Send and lifecycle/ownership behavior remain as above.

Official source review: [RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener)
distinguishes end-of-speech, final results and errors. [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS)
warns that provider silence extras may be ignored, so the five-second contract
is enforced by the app instead of relying on an intent extra.

Acceptance: no immediate error for a pause after text; no-speech feedback only
at the five-second boundary for an empty attempt; retained partials/finals;
fresh resume and stale-callback protection; Pause precedes the animated mic.
Seven new controller regressions exercise timing and recovery with a fake clock.
The existing Compose entry regression now asserts the control order.
Device recognition and visual acceptance remain pending.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 925 unit tests and zero failures/errors/skips. Both APKs assembled
and lint passed. Six repository script tests and locale-resource verification
also passed. Instrumentation coverage was compiled only; no device installation,
recognizer execution or visual inspection was performed.

## 2026-09-06 — continuous dictation and cursor indicator

Latest correction supersedes automatic pausing in the silence revision above.
Silence and ordinary provider endpoints keep dictation active until explicit
Pause/X, editing or selection movement, lifecycle/ownership exit, or an actual
capability failure. After five seconds with no recognized text, retain the inline
no-speech feedback while continuing to listen; the next nonblank result clears
it. Successful dictation never acquires a silence error. Preserve the current
Pause-before-microphone order.

Show three softly staggered animated dots immediately after the current editor
insertion point throughout active dictation, including between utterances. They
are display-only and must never enter draft, clipboard or sent message text.
Use the native text layout and offset mapping so wrapping, scrolling, font scale,
RTL and insertion into existing text stay aligned. Preserve mention styling and
native editing; expose a stable localized Transcribing state rather than
announcing animation frames. A cursor-owned indicator requires this small custom
text decoration because a separate progress component cannot follow the caret.
Pause and X remove it. Use Compose animation so system motion scaling applies.

Official sources: [VisualTransformation](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/input/VisualTransformation),
[OffsetMapping](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/input/OffsetMapping),
and [Compose value animations](https://developer.android.com/develop/ui/compose/animation/value-based).
Acceptance covers long pauses followed by more speech, retained empty-attempt
feedback with continued capture, dots at empty/middle/end insertion positions,
unmodified draft text, and removal on Pause/edit/exit. Device checks remain pending.

Implementation: `ComposerCaptureController` retains capture across silence and
clears empty-attempt feedback on speech. `ComposerTextTransformation` adds the
three display-only dots and maps native text offsets/mention backgrounds;
`ConversationComposer` supplies staggered Compose alpha animations and the
localized Transcribing state. Standard transformed-text semantics and selection
handling remain with the native editor.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 929 unit tests, zero failures/errors/skips, zero lint errors (nine
warnings), and both APKs assembled. The final cursor-space assertion correction
also passed `./gradlew assembleDebugAndroidTest`. Instrumentation coverage was
compiled only. No device recognition, installation or visual inspection occurred;
continuous live-provider behavior and visual acceptance remain pending.

## 2026-09-06 — multiline collapse keeps full width

Latest explicit direction supersedes the width-first collapse for drafts with
four or more compact-width visual lines. Once automatically wide, pulling down
or using Collapse/Back changes only the composer height; the full-width editor,
integrated Add and bottom action row stay in place throughout drag and settling.
Use the existing compact-width line measurement for entered and wrapped lines,
so the expanded editor's reflow cannot change eligibility. One-to-three-line
manual expansion retains its staged width-then-height collapse.

Apply the same eligibility to drag preparation, direct height tracking, and the
shared settle path. Preserve cancellation, reversal, native spring motion,
keyboard, selection, insets, RTL and the existing text editor. No spacing or
component changes are needed. Acceptance requires constant surface/editor width
on every sampled collapse frame for explicit four-line and soft-wrapped drafts,
with the original content-height destination and short-draft sequence preserved.
Official source checked: [Compose value animations](https://developer.android.com/develop/ui/compose/animation/value-based).
Device motion and visual acceptance remain pending.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 929 unit tests, zero failures/errors/skips, and both APKs assembled.
Lint passed with zero errors and nine warnings. Two new frame-sampled Compose
regressions compiled; the existing short-draft animation test remains in place.
No device/emulator execution or visual inspection was performed.

## 2026-09-06 — dictation has no X or no-speech feedback

Latest user direction removes the redundant leading X from active and paused
dictation and completely disables no-speech feedback, superseding the five-second
rules above. Keep Pause/Resume, editable text and explicit Send. No replacement
leading control is shown in dictation; the capsule uses the vacated leading
space. Back pauses active capture; Back from paused dictation returns to the
ordinary composer while retaining the draft, so attachment actions remain
reachable without a visible exit button. Actual voice-note review is distinct
and retains its existing discard action.

Remove the empty-capture timer and its clock/state entirely. Empty results and
provider no-match/speech-timeout callbacks silently continue listening with the
existing retry delay; no duration of silence creates an error or ends capture.
Other capability failures retain recovery. Suppress no-speech copy at the
shared dictation UI boundary as well. Cursor dots, microphone, ownership,
multiline width, editing and lifecycle behavior otherwise remain as accepted.
Official callback contract: [RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener).
Acceptance includes repeated empty/provider-timeout outcomes, no error text,
no X/leading slot before and after Pause, retained text, Resume and Back exit.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed with 927 unit tests, zero failures/errors/skips, zero lint errors (nine
warnings), and both APKs assembled. Six endpoint/retention cases replace eight
superseded timer cases. Updated Compose regressions compile only. No device
recognition, installation or visual inspection was performed.


## 2026-09-07 — Continuous composer expansion and collapse

The user supplied a recording showing short-draft growth and repeated collapsing
with a perceptible stop between stages, and requested smooth continuous motion.
This supersedes the earlier height-then-width / width-then-height choreography.
Manual expansion now derives capsule width, action-row integration and height
from the same progress. Automatic four-line widening uses one additional baseline
progress for both width and toolbar; it stays wide during manual collapse.
Retain the non-bouncy Compose spring (medium-low stiffness), with a 0.001 progress
visibility threshold to avoid a several-pixel endpoint snap. Reversal starts at
the current progress/velocity. Native motion scaling still applies.

Dragging follows root-coordinate finger displacement immediately in either
direction, without waiting for width contraction or launching a coroutine per
pointer move. This also prevents the moving composer's local coordinates from
feeding back into displacement and velocity. On release, hand the current drag
position and velocity to the spring. Timeline travel observes the same progress.

The compact destination adjusts for text-height changes made while expanded,
retaining measured non-editor chrome instead of landing at the stale pre-edit
height and jumping when intrinsic layout returns. Keep one editor, focus,
selection, draft, current insets, 24 dp expanded top gap, existing Back ordering,
voice behavior, and four-line full-width eligibility.

`ComposerLayoutFlowTest` now checks concurrent dimensions in both directions,
downward short-draft tracking at two root-coordinate positions, monotonic collapse
after editing while expanded, and interrupted expansion preserving selection.
Existing multiline per-frame width and drag tests remain. These tests require
explicitly authorized device execution; compilation is host-side evidence only.
The supplied recording was inspected; no current-build device inspection is claimed.

Official source checked:
[Compose value animations](https://developer.android.com/develop/ui/compose/animation/value-based).

Validation: README host gate passed (`testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest`): 1013 unit tests, 0 failures, 0 errors and
0 skipped; lint and both APKs passed. Script tests and locale verification
also passed. Motion regressions compiled only; current-build device verification
remains pending.

## 2026-09-07 — Composer emoji entry

The user requested a leading emoji action inside the text capsule, before both
placeholder and typed text. Reuse the shared searchable EmojiPickerSheet and
Material smiley icon in a native 48 dp IconButton. Selection inserts at the
current cursor or replaces the selected range, preserves the surrounding draft,
and returns to typing; Back dismisses without editing or sending. The action
stays before text in multiline and expanded layouts. Opening the picker pauses
active dictation. Recording retains its dedicated waveform/Stop presentation.

The voice-record action is visible only with an empty text draft (including no
whitespace); sendable drafts show Send instead. Dictation remains available.
Compact wrapping measurement includes the new leading action width. Existing
layout assertions now include its width. Added navigation coverage for emoji
insertion/selection, Back preservation and first-character voice visibility.
Device inspection is not part of this request.

Validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed. The new interaction tests compile; they were not run on a device.

User alignment refinement: emoji stays bottom-aligned with dictation in every
composer height. In the wide/expanded composer it joins the bottom action row
after Add, leaving the text above full width. Its position follows the existing
shared toolbar animation and uses logical start placement for RTL. Layout
regressions assert bottom alignment and separation from Add.

Alignment refinement validation: the full host gate passed (unit tests, lint,
APK assembly and instrumentation-test compilation). No device run performed.

Spacing trial requested by the user: mic-to-waveform/Send centers are now 36 dp;
Add-to-emoji centers are 28 dp in the integrated bottom row. Icons retain their
existing sizes and 48 dp vertical targets. The native controls use narrower
horizontal layout bounds, with platform touch expansion, so their actual layout
bounds do not overlap. Add keeps its original center and the trailing action
keeps its original end inset. The existing toolbar progress animates the emoji
width/position and Add inset together. Recording and active-dictation controls
retain their existing layout.

Native reference: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
(documents automatic touch expansion and recommends 48 dp layout bounds). The
user explicitly requested the narrower horizontal spacing as a trial; this is a
scoped exception, with visual/touch acceptance pending.

Spacing trial validation: full host gate passed, including updated layout-test
compilation. No device inspection was requested or performed.

## 2026-09-07 — Paused dictation restores composer actions

A paused inline session was incorrectly treated as active dictation for Add,
voice visibility and emoji placement. Only active capture now hides Add and
voice and reserves the full-width capture row. Pausing or editing returns the
normal composer layout while keeping Resume dictation available. Clearing text
restores the Message placeholder and voice action; wide/expanded editing retains
Add with emoji beside it. Writing tools are available while dictation is paused.

The user revised mic-to-wave/Send spacing to 40 dp centers and pause-to-animated
mic spacing to 28 dp. Add-to-emoji remains 28 dp in the integrated row. Glyph
sizes and vertical alignment stay unchanged. Regression coverage exercises
editing a live dictation into a wide draft, clearing it, and both spacing states.

Recovery validation: full host gate passed (unit tests, lint, debug APK and
instrumentation-test APK compilation). The new navigation regression compiled;
it was not run on a device.

Final user spacing adjustment: integrated Add-to-emoji centers are 32 dp. Add's
horizontal inset and emoji's width/position animate together, retaining the
original Add center. Mic-to-wave/Send stays 40 dp; active pause-to-mic stays 28 dp.

32 dp adjustment validation: full host gate passed; updated layout tests compile.
No device inspection performed.


## Always full-width composer — 2026-09-09

User-provided ChatGPT/Claude mobile references supersede the earlier compact
capsule and side-expansion decisions. “Two lines” follows those references:
one text row plus one internal action row at rest. Additional text grows upward
to the existing line budget and then scrolls. Manual expansion, Back/collapse,
selection, dictation, attachments, voice review and timeline clearance retain
their accepted behavior; expansion changes height only. Add and voice-review
Cancel are now children of the appropriate internal toolbar, including when
preparation/error rows appear below it.

The existing custom Layout remains solely to allocate expanded editor height
and retain approved 32/40 dp action spacing. Foundation BasicTextField continues
to own editing, selection and scrolling; the editor is never replaced during
layout changes. Governing current Android sources:
[Text input](https://developer.android.com/develop/ui/compose/text/user-input) and
[Compose layout](https://developer.android.com/develop/ui/compose/layouts/basics).

ComposerLayoutFlowTest covers the empty two-row layout, full available width,
text growth/clearing, action containment, large text/RTL, selection and fixed
width throughout drag/expansion/collapse. DictationCaptureTest checks the same
width and internal controls during recording speech and pausing. Device
execution and visual acceptance of this revision remain pending.

2026-09-09 host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest` passes: 1,089 tests, zero lint errors (16 warnings,
2 hints), app and instrumentation APKs built. The final dictation geometry test
update also passes `lintDebug assembleDebugAndroidTest`. Ten Python checks and
all 1,750 translated keys in four locales pass. No device/emulator inspection
or instrumentation execution was performed.


### Empty, unfocused reading row — 2026-09-09

User refinement: keep the surface full width, but use a single row when the
editor is empty and unfocused. Add remains internal, followed by emoji and the
Message placeholder; dictation/voice stay at the end. Native editor focus opens
the two-row layout. Clearing text while focused keeps two rows; losing focus
then restores the reading row. Any text (including whitespace), attachment,
reply, link preview, voice/dictation session, preparation or attachment error
keeps the editing presentation. Closing only the keyboard does not force focus
loss. Manual vertical expansion and Back remain available.

One BasicTextField is retained through focus changes. The existing custom Layout
places empty text inline or above the controls without changing container width.
The inline placeholder uses one line with ellipsis if needed; controls center
vertically with large text and mirror in RTL. The compact-height calculation
uses the reading endpoint for correct timeline clearance and manual collapse.

ComposerLayoutFlowTest covers reading alignment, focus/blur, preserving unfocused
drafts, clearing while focused, repeated transitions, large text and RTL.
Current Android source: [Focus observation and ordering](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior).
Device execution and visual acceptance remain pending.

Reading-row validation on 2026-09-09: the full host gate passes (1,089 unit
tests, lint zero errors / 16 warnings / 2 hints, app and instrumentation APKs).
Ten Python checks, all 1,750 translated keys in four locales and diff whitespace
checks pass. Focus/blur and layout regressions are compiled, not device-run.


### Linear composer motion and timeline anchoring — 2026-09-09

User correction replaces instant reading/editing reflow with a 220 ms linear,
interruptible transition. Text allocation, placeholder inset and toolbar
separation share one progress value. Text-row growth uses a matching linear
animation, and manual drag settling uses the same tween; surface width stays
fixed. A single editor retains focus and selection.

TimelineResizeAnchor captures the previous viewport during measurement. When
height or bottom clearance changes, a timeline fully at its end requests the
last item's end in that same layout pass. Older history requests its original
first item and pixel offset. A partly visible last message is not the bottom;
active user scrolls and explicit history navigation are not overridden. This
replaces the repeated, delayed scrollToItem calls on every composer resize.
Manual expansion also tests canScrollForward rather than last-item visibility.

IME and remaining navigation-safe clearance are read during measurement, so
keyboard-driven viewport resizing and list padding use the same inset frame.
No second animation is applied to the platform keyboard.

Six host TimelineResizeAnchorTest cases cover bottom/history, a partly visible
last item, oversized last messages, repeated frames, stable layout, empty lists
and active scrolling. ComposerViewportFlowTest adds frame geometry checks with
controlled viewport resizing, for bottom following and stationary history; it
also checks progressive, monotonic composer motion. Device execution remains
pending and the controlled viewport is not claimed as real IME verification.

Governing Android sources: [Animation specifications](https://developer.android.com/develop/ui/compose/animation/customize),
[Inset measurement and animation](https://developer.android.com/develop/ui/compose/system/insets-ui),
and LazyListState.requestScrollToItem in the pinned AndroidX Foundation source.

Motion/viewport validation on 2026-09-09: the final full Gradle gate passes with
1,095 host tests, lint zero errors / 16 warnings / 2 hints, and app plus
instrumentation APKs built. Ten Python checks, 1,750 translated keys in four
locales and diff checks pass. Frame tests compile; real IME/device execution
and visual acceptance remain pending.


### Tighter text-to-toolbar spacing — 2026-09-09

User screenshot correction halves the editing text's bottom inset from 12 dp
to 6 dp. Top padding stays 12 dp; icon targets and their row remain intact.
The measured compact editor height subtracts the same 6 dp, including during
linear reading/editing motion, so the removed padding does not survive as blank
allocated space. Empty reading mode retains its centered 48 dp row. The normal
one-line editing composer is now approximately 90 dp before outer padding.

Spacing validation: `testDebugUnitTest --tests '*Composer*' --tests
'*TimelineResizeAnchorTest' lintDebug assembleDebug assembleDebugAndroidTest`
passes (67 targeted tests, lint and both APKs). Diff checks pass. Current
device/visual inspection remains pending.


### Combined gap correction — 2026-09-09

The user reports that the 6 dp change is not visually sufficient. The original
gap combined 12 dp below text with the 12 dp above 24 dp icons in their 48 dp
targets. Remove the text-side spacer completely, leaving the icon-side spacing:
24 dp of explicit combined padding becomes 12 dp. Glyph line metrics remain
font-owned. The measured editor height subtracts the full 12 dp, giving an
approximately 84 dp one-line editing surface. Reading mode and action targets
are unchanged. This supersedes the preceding 6 dp bottom-inset trial.

ComposerLayoutFlowTest now checks that the last text line's measured bottom
meets the toolbar boundary without extra allocated space. Compilation is host
validation; current-build visual and device execution remain pending.

Combined-gap correction validation: 67 targeted composer/viewport host tests,
lint and app/instrumentation APK builds pass. The new measured-line geometry
regression compiles; device execution and visual acceptance remain pending.

### 36 px gap correction — 2026-09-09

The user rejected raising icons inside unchanged buttons: the composer must
become shorter. Remove those offsets and reserve 4 dp less for the editing
toolbar, sharing its empty top edge with the lower text line box. Text retains
its full measurement and buttons keep centered artwork. The compact surface
is 4 dp shorter, with the same reduction in the manual-collapse endpoint.
Reading and voice layouts remain unchanged; focus progress interpolates the
reserved height. The geometry regression checks surface height and centered
artwork. Current-build device execution and visual acceptance remain pending.

Height-correction host validation: all 67 targeted unit tests, lint and both
APK builds pass. The revised geometry regression compiles; device execution
and visual acceptance remain pending.

### Stable composer transition — 2026-09-09

User-supplied screen-20260909-082959-1788935363435.mp4 shows repeated keyboard
open/close transitions and first-line clipping when inserting newlines (around
12 seconds), followed by repeated growth/deletion. Keep the accepted compact
spacing and full-width shape. Focus and automatic line growth use 160 ms linear
motion, superseding 220 ms for those two transitions; manual settling retains
its existing timing.

StableComposerTextViewport measures the native BasicTextField at the current
natural text height, with constant 12 dp top/bottom internal decoration. Its
outer viewport reveals the animated text-row height, so the extra internal
bottom inset does not add visible spacing. Adding a line no longer gives the
native editor an undersized animated viewport that scrolls existing text away
from the caret. The placeholder stays one persistent, single-line Text node.
Native scrolling still handles drafts beyond the compact line limit.

ConversationScreen measures the composer overlay before the lazy timeline,
keeping its foreground draw order with zIndex. The timeline therefore reads the
current measured composer height for its padding, retaining bottom anchoring
and stationary history through IME changes. Keep IME inset reads in layout and
let Android own keyboard animation, following [Compose phases](https://developer.android.com/develop/ui/compose/phases)
and [inset timing](https://developer.android.com/develop/ui/compose/system/insets-ui).

ComposerLayoutFlowTest adds rendered first-line checks during line insertion and
deletion plus placeholder visibility during interrupted focus transitions.
ComposerViewportFlowTest retains per-frame bottom/history anchoring coverage.
Current-build device execution and visual acceptance remain pending; video
inspection was of the user-provided previous build.

Stable-motion host validation: all 1,095 unit tests, lint, app assembly and
instrumentation-test APK assembly pass. Rendered-frame regression tests are
compiled only; no device or emulator was used for this change.

### Placeholder endpoint fade — 2026-09-09

User requests that the empty Message placeholder disappear at its reading
position and appear at the editing position instead of sliding across icons.
Render it separately from the moving input viewport: the first half of the
existing 160 ms transition fades the reading label out; the second half fades
the editing label in. Switch positions only at zero opacity and reverse the
same behavior on collapse or interrupted focus. Reading placement follows the
bottom row and respects the natural height at larger font scales. Preserve
entered text, native caret behavior, composer height and timeline anchoring.
The input retains the Message accessibility label; the visual placeholder does
not add a duplicate announcement. ComposerLayoutFlowTest replaces the old
always-visible placeholder assertion with endpoint/fade/reversal coverage.

Placeholder-fade host validation: 67 targeted unit tests, lint and both APK
builds pass. Updated UI regression coverage compiles; device execution and
visual acceptance remain pending.

### Keyboard-synchronized composer — 2026-09-09

User reports keyboard drawing and composer movement out of sync. Conversation
now owns bottom insets once: the union of navigation bars and IME applies to
the composer and timeline together. Scaffold receives system bars/display
cutouts, not the IME; the nested composer no longer reapplies navigation bars.
The search bottom bar retains its reserved space. The jump-to-latest FAB
consumes Scaffold's navigation clearance before applying keyboard clearance.

ComposerKeyboardMotion follows Android's current/source/target keyboard insets
for focus expansion or collapse. Insets and progress are sampled in layout and
drawing, including text padding and placeholder fade. There is no separate
lift animation. The fallback gives a software keyboard up to 160 ms to start;
if no animation arrives, focus uses the existing 160 ms linear transition.
Hardware or blocked keyboard input bypasses that wait. A late IME takes over
from the currently presented height without snapping backward. Retain the
endpoint when Android clears animation metadata, and retain the editing state
when an already-open keyboard changes height. Draft text and focus still decide
whether collapse is appropriate. Typed-line growth and manual expansion keep
their previously accepted behavior.

Use the same inset values for physical positioning and transition progress,
following [Android inset timing](https://developer.android.com/develop/ui/compose/system/insets-ui).
ComposerKeyboardMotionTest covers keyboard curves, fallback, delayed startup,
completion, reversal and keyboard height changes. ComposerViewportFlowTest now
also feeds changing keyboard insets, checking the exact bottom edge and height
on opening/closing frames, rather than only resizing the host. These UI tests
remain compiled-only until device testing is requested.

Keyboard-sync host validation: all 1,103 unit tests, lint and both APK builds
pass. The inset-driven UI regression compiles; device execution and visual
acceptance remain pending. No device or emulator was used.

### Reduce keyboard-frame layout work — 2026-09-09

User reports remaining jumpiness after progress synchronization. Source review
found that changing IME constraints subcomposed the full composer body each
frame, while intermediate measured heights updated composition state and
restarted initial/end-settlement effects. This is a concrete source of extra
frame work; device profiling has not yet confirmed total frame-time impact.

For ordinary keyboard transitions, stableComposerKeyboardConstraints measures
composer content at the keyboard endpoint height and places that content at
the current keyboard edge. BoxWithConstraints therefore sees a stable height
through intermediate frames. Manual expansion retains actual available bounds.
Keep the existing inset owner, focus progress, placeholder fade and dimensions.

The compact height used as the manual-expansion endpoint is cached only after
focus/line motion settles. The timeline still receives the current height in
measurement. Initial/end-settlement effects depend on whether the composer has
been measured, not every pixel change. Report floating-surface clearance from
the measurement callback instead of a composition-level SideEffect.

ComposerViewportFlowTest adds an opening/closing inset regression that checks
physical placement while counting constraint-content compositions. Follow
[Compose performance guidance](https://developer.android.com/develop/ui/compose/performance/bestpractices)
for deferred state reads and reducing composition work. Current-build frame
profiling, UI-test execution and visual acceptance remain pending.

Keyboard frame-work host validation: all 1,103 unit tests, lint and both APK
builds pass. The new composition-count UI regression compiles; it was not run
on a device. Current-build runtime performance and visual acceptance remain
unverified.

### Preserve the input session through keyboard dismissal — 2026-09-09

The supplied 09:33 recording shows a keyboard key-layout change near the end of
closing and a composer reversal on rapid retaps. Source review found immediate
app-shell focus clearing on background taps and editor tap consumption occurring
after the parent Final-pass observer. These are concrete lifecycle/order issues;
the recording alone does not establish their share of the total frame stutter.

BackgroundKeyboardDismissalHost now requests keyboard hiding while retaining
native input focus until current, source and target IME insets all reach zero.
A bounded one-second fallback releases focus if the IME does not complete its
hide request. During dismissal, the empty composer follows the existing closing
inset curve and hides its caret without ending the input session early. Consumed
control/editor taps cancel the pending dismissal; generation checks prevent an
old completion from clearing newly acquired focus. Losing composer focus or
leaving the conversation invalidates a pending request. Editor taps are consumed
in Main pass before the app-shell Final observer, after inner text selection.

Use the public [SoftwareKeyboardController.hide API](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/SoftwareKeyboardController)
to separate keyboard visibility from focus. Keep the accepted geometry,
placeholder fade and timeline anchoring. BackgroundKeyboardDismissalTest covers
completion and superseded requests. ComposerKeyboardDismissalFlowTest exercises
actual pointer dispatch through the app-shell host, focus retention during the
last inset frame and retap cancellation with a controlled keyboard controller.
Current-build device execution and visual acceptance remain pending.

Input-session host validation: all 1,106 unit tests, lint, app assembly and
instrumentation-test APK assembly pass. The two app-shell pointer/focus UI
regressions compile; no device or emulator was used. Closing smoothness and
rapid-retap motion on the current build remain unverified.
