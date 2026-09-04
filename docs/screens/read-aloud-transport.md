# Read Aloud transport and source navigation

## Purpose and scope

B15 implements C061/C062: one foreground Read Aloud queue with sentence/message
navigation, pause/resume/stop, progress, bounded history loading and retry,
source return, authored-text highlighting and accessible sentence seeking.
B16 owns engine/voice/rate/mixing/auto-read preferences and background-control
fixtures. Backend/history networking, services and new permissions remain out
of scope. Existing sent/received text, captions, replies and selected passages
retain their eligibility; voice attachments and unavailable/deleted/expired
messages do not enter the authored-message queue.

## Parity contract and state

- A queue is owned by a profile and chat. Only a bounded window is prepared for
  speech. Crossing a window edge freezes the cursor, loads the adjacent window
  locally, and either advances, retains the cursor with Retry, or reaches an end.
  The developer-only failure fixture fails once; Retry resolves the same edge.
- Pause invalidates native callbacks and freezes current passage/progress. Resume
  restarts that sentence. Sentence/message seeking preserves paused state;
  explicit Read from here starts playback at the selected sentence.
- Message and sentence position accompany observational progress. Completion
  ends active highlighting/following and restores Read Aloud eligibility while
  retaining an explicit return-to-source bookmark until Stop or a new session. Android TTS
  provides optional range callbacks, so sentence/chunk boundaries remain the
  fallback. There is no fake audio scrubbing or timer-derived spoken progress.
- Exact source offsets survive Markdown rendering, repeated text, entities,
  Unicode and block boundaries. Read selected text preserves its bounded span;
  Read from here continues from its owning sentence. Choose sentence provides
  the same action without requiring a long press or drag.
- Automatic following stops on a manual scroll. Resume following returns to the
  current passage. Speech-driven positioning does not acknowledge unseen chat
  messages as read. Collapsed details containing the followed passage expand.
- Session and utterance generations reject callbacks from stopped, paused,
  replaced or failed playback. Source edits, deletion/expiry or missing source
  prevent stale text playback. Native errors retain a recoverable cursor.

## Entry, navigation, Back and exit

The existing message menu and full-message/file readers start speech. A shared
transport reserves space below route content; full-screen readers expose the
same controls in their own bottom slot. Back keeps its existing navigation or
selection-dismissal meaning. A text attachment remains scoped to its reader:
closing/replacing it stops its speech. One native engine is shared by these
entry points, preventing overlapping queues.

Return to spoken message validates the active profile, chat, message and source
snapshot immediately before dispatching a targeted conversation route. Ordinary
route changes preserve the queue. Manual profile change, sign-out, lock/background,
app-shell disposal or recreation clears speech and return ownership. This follows
F07's explicit local privacy boundary: it never silently switches back to an old
profile or resumes that profile's content. A stale return action is cancelled.

## Exact product copy

Read aloud; Pause reading; Resume reading; Stop reading; Previous sentence;
Next sentence; Previous message; Next message; Return to spoken message;
Read from here; Choose sentence; Resume following; Pause following;
Sentence %1$d of %2$d · Message %3$d of %4$d; Reading paused;
Loading earlier messages…; Loading later messages…;
Couldn’t load more messages.; Couldn’t read this text aloud.;
This message is no longer available.; Reading finished.; Retry; Close.
Sentence chooser rows use Read from sentence %1$d plus the sentence preview.
Fixture names appear only in Developer Tools.

## Android composition and accessibility

Use the established monochrome Material surfaces, native icon/text buttons,
progress indicator, wrapping action rows and adaptive content bounds. Keep
native minimum targets and shared 8/16 dp relationships. A full-screen sentence
chooser uses lazy native rows, standard app bar and system Back. Text highlighting
uses AnnotatedString span styles and source ranges; native BringIntoViewRequester
follows the active rectangle in a reader. Native selection remains intact. Reader actions share a bounded scrollable
bottom area, and the root consumes IME/navigation insets for its reserved transport.

Controls carry action labels and disabled semantics. Current sentence/status is
available as text/semantics independently of highlight color. Avoid live-region
announcements on each word. Narrow/large-type transport can scroll within a
bounded height; content/IME/system insets have one owner. Logical reading order,
RTL layout, reduced-motion-safe immediate positioning and source copy are retained.
No new gesture, drawing primitive, dependency, permission or device surface.

## Governing Android sources

Checked 2026-09-04:

- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech): stop discards queued utterances; speak is asynchronous and bounded by the engine input limit. Resume therefore requeues a sentence.
- [UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener): range timing is optional; start/done/error and generation ownership govern progress and recovery.
- [Responsive/adaptive layout](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes): current window dimensions and bounded scrollable controls retain reading space.
- [BringIntoViewRequester](https://developer.android.com/reference/kotlin/androidx/compose/foundation/relocation/BringIntoViewRequester): native relocation of the current text rectangle; user input remains unconsumed.
- [Compose text styles](https://developer.android.com/develop/ui/compose/text/style-text): source-mapped AnnotatedString spans preserve native selection and formatting.

## Evidence and integration seam

Production baseline remains master `319454889f1c2494dec4a69b5577d98017f44eee`.
Read `audio/tts/TtsPlaybackQueue.kt`, `TtsChunker.kt`, history-window/session
contracts, `ui/conversation/TtsTransportBar.kt`,
`ui/conversation/messages/TtsSpeakFromHere.kt`, `TtsVisibleSentenceMapping.kt`,
and `ui/navigation/TtsDestinationNavigation.kt`. The production controller and
history loader replace local queue events at migration; no production backend
is added to this prototype. The [source map](../port/source-map.md) retains pinned
iOS speech and B09/B13 document/reader evidence. Android-only scope follows
[F07](../audits/production-android-parity/flows/F07-read-aloud.md) and B15.

## Observable acceptance criteria

- Pause, resume, stop, sentence/message navigation, completion, failed edge Retry,
  stale callback rejection and profile/source cancellation have host rule tests.
- Source return, selected-passage seeking, repeated Markdown mapping, follow
  interruption/resume and accessible transport controls have meaningful coverage.
- Full host gate passes; compile UI tests only. Device speech, TalkBack, visual,
  predictive Back and hands-on acceptance remain pending explicit authorization.

## Implementation evidence

- `SpeechPlayback.kt` owns the immutable catalog and eight-message prepared
  window, locale-aware sentence/source projection, engine-sized chunks, optional
  word offsets, frozen pause progress, navigation, history retry and completion.
  Common title abbreviations stay with names; authored block boundaries remain.
- `ReadAloudController.kt` replaces route-owned engines with one foreground
  session, source/profile validation, revision-checked callbacks and return
  dispatch. Optional word timing refines the chunk fallback. Same-file replay
  after completion works without an extra Stop action.
- `ReadAloudUi.kt` reserves adaptive transport space under app routes and inside
  full readers; `WhiteNoiseNavHost.kt` resolves the current owned message at
  source-return dispatch. `MessageDocumentUi.kt` paints authored source spans
  and uses native relocation; details expand only for the followed passage.
- `ConversationScreen.kt` follows bounded history targets, suspends following
  for native user scroll input and suppresses unread acknowledgement during
  speech-driven positioning. `MessageReadingUi.kt` provides selection-based
  Read from here and the accessible lazy sentence chooser. File-reader scope
  still stops playback when its current source or reader ends.
- Developer Tools exposes one-shot earlier/later history failures, gated by the
  active developer profile. No engine trust/rate/auto-read preferences, service,
  manifest permission, dependency or production-network capability was added.

`SpeechPlaybackTest` and `SpeechOwnershipTest` add **33 host cases** for queue
boundaries, paused navigation, frozen progress, optional range/chunk mapping,
long Unicode, repeated Markdown, entities/blocks/titles, bounded selections,
completion/replay, stale callbacks, retry and profile/chat/source ownership.

`ReadAloudTransportTest` adds **15 UI/platform cases**, compiled only: transport
pause/seek/stop, history/native-engine retry, source-mapped highlighting, sentence
choice without gestures, manual follow interruption/resume, source edit/profile
cancellation, chat/file exclusivity, source return from Settings, unread gating,
large-text RTL reachability and same-file replay after completion.

The clean host gate and final focused follow-ups pass **465 unit tests** with no
failures/errors/skips, lint with **0 errors, 14 pre-existing warnings and 2 hints**,
app APK assembly and instrumentation-test APK compilation. No device/emulator,
audible speech, platform-surface, visual or hands-on acceptance is claimed.
