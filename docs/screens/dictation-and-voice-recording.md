# Dictation and voice-note interaction

## Scope and evidence

B17 implements C067–C070 after B11. Preserve the established composer, recorded
voice review/transcription and Voice/Text/Both formats. Dictation is a separate
speech-to-text task. All capture, permission, provider and recognition outcomes
remain deterministic in memory. Q06 does not authorize microphone permission,
a recognizer/provider Activity, service, notification, networking or storage.

Production baseline remains `319454889f1c2494dec4a69b5577d98017f44eee`. On
2026-09-04 `master` resolved to `911040c7e1c31652638c8cfd72812d1f3a694b9b`, seven
commits ahead. The 94-file comparison changes no dictation/voice recording source
or tests. AppState's diff affects bootstrap, account snapshot, chat-list updates
and attachment installers; Controllers has timeline/roster changes and no capture
or dictation changes. B17 keeps the pinned contracts. Later batches/final
reconciliation must review the changed roster, Nostr, installer and startup seams.

Read production ConversationDictationController/Preferences/RecognitionService/
Compatibility, DictationSettingsScreen, ConversationDictationControls and
VoiceRecordingController/VoiceRecorder. Local iOS evidence remains
`docs/screens/composer-media-and-speech.md` and `docs/port/source-map.md`:
`wn-ios-prototype@0bd7cba` conversation-composer-states, conversation-shared and
speech-messages, plus their ConversationView/PrototypeComposerTextView sources.

## Product behavior

- Keep voice recording and dictation mutually exclusive by exact profile/chat/
  request ownership. Old release, callback, timeout or completion cannot control
  a replacement capture. Nothing sends on Back, gesture cancellation or teardown.
- Voice: hold starts capture, release finishes into the existing review. Drag
  toward logical start cancels; drag upward locks and release then keeps recording.
  Tap/keyboard/accessibility activation starts locked capture. Visible Stop,
  Cancel and Lock actions offer equivalent operation without dragging. Preserve
  the existing explicit Send step instead of production's release-to-send.
  Include too-short, microphone busy, denied/permanently denied and recording
  failure recovery. Cap local recording at production's five-minute limit.
  A too-short encoder result is a deterministic outcome, not a guessed platform
  minimum. Preserve draft text, attachments and reply throughout capture/review.
- Dictation enters through the composer Add menu, independently of voice-note
  transcription. Before its first use for a profile, disclose external speech
  processing and the default paste result; Cancel starts nothing.
- Capture the exact profile/chat, draft text and revision, selection, attachments,
  reply and composer/membership eligibility. Show Preparing, Listening,
  Transcribing, Review dictated text and typed failure states. Provider callbacks
  and timers require exact session/revision ownership. A selected speech service
  must match the discovered component exactly; never silently fall back.
- Default completion is manual Done; settings also offers 3, 5 or 10 seconds of
  silence. Capture these settings when a request starts. Speaking resets its
  silence boundary; provider segment completion alone cannot override manual
  finish. Show and retain accumulated text if a later segment fails/interruption
  occurs. Empty recognition reports No speech was recognized.
- Default result is Paste into draft at the captured selection, preserving
  grapheme boundaries and readable spaces. Send message is an explicit opt-in
  with visible consequences. Immediately before send, recheck origin, revision,
  draft payload and membership; never send to a different context. Per B17's
  explicit audit contract, any intervening draft/membership/session change puts
  the transcript in review, even if the text later returns to the same value.
- Review offers Copy, Insert at end and Discard. Insert is explicit and targets
  only the original available chat/current profile, using its latest draft;
  automatic send is not retried from review. A failed insert/send retains text.
  Leaving the origin or backgrounding releases capture and retains available
  text for review. Profile switch cannot reveal it in another profile. Sign-out,
  removal and wipe clear that profile's capture/review state.
- Model selected service missing, readiness/processing timeout, no speech,
  service busy, microphone busy, network, denial/permanent denial and generic
  failure. Retry takes a fresh owned snapshot; permanent denial and unavailable
  service offer standard Android Settings recovery. No app-owned permission
  panel pretends to be an Android system prompt.

## Android composition, navigation and accessibility

Use existing Settings tonal groups, rows and native choice dialogs for a new
Dictation route. Reuse the composer's established Add popup, native editor and
shared status/dialog patterns. Preparing/listening/processing controls sit inline
above the native editor, which remains editable. Keep review text selectable,
show the origin chat and offer explicit insertion into its editable draft. Short/large-font content
scrolls; actions use normal 48 dp targets and shared 4/8 dp relationships. Preserve
IME/inset ownership, light/dark/RTL and compact/expanded composer behavior.

A hold-plus-two-axis lock/cancel gesture needs scoped pointer handling because
standard click/drag alone cannot express the full interaction. Keep its target
mounted through capture, honor cancellation/consumption, and retain native
click/keyboard/semantic alternatives. Translate cancel direction for RTL.
Gesture thresholds are interaction thresholds, not new layout spacing.

Back dismisses an inner menu/dialog first, cancels active capture without sending,
then follows existing IME/collapse/navigation behavior. Review dismissal retains
text until explicit Insert/Discard, so navigation cannot silently lose it.

## Exact copy

Dictation; Finish dictation; When I tap Done; After %1$d seconds of silence;
When finished; Paste into draft; Review or edit the text before sending;
Send message; Send when dictation finishes if this chat and draft are unchanged.;
If the draft, membership or session changes, keep the text for review.;
External speech recognition; Your speech service may send dictated audio to its
provider. Dictated text is pasted into your draft unless you enable Send message
in Dictation settings.; Start dictation; Cancel; Preparing…; Listening…;
Transcribing…; Done; Review dictated text; Insert at end; Copy; Discard;
The draft or conversation changed. Review this text before inserting it.;
Speech recognition isn’t available.; Microphone access is needed.;
Allow microphone access in Android Settings.; The microphone is already in use.;
No speech was recognized.; The speech service couldn’t connect.;
The speech service is busy.; Speech recognition timed out.;
Couldn’t transcribe speech.; Retry; Open Android Settings;
Recording…; Recording locked; Lock recording; Stop recording; Cancel recording;
Release to review; Slide to cancel or slide up to lock; Hold longer to record.;
Couldn’t record this voice message.

## Current official sources

Checked 2026-09-04:

- [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures): highest suitable input abstraction, pointer ownership, cancellation and semantics.
- [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer): selected recognition service, asynchronous callbacks, errors and microphone/network boundary. This batch models those outcomes only.
- [Text input](https://developer.android.com/develop/ui/compose/text/user-input): retain native text selection, IME and editing.

## Implementation evidence

Implemented and host-verified 2026-09-04 for C067–C070 within the deterministic
capture boundary. `ComposerDictation.kt` owns preferences, exact service identity,
grapheme-safe insertion, silence endpointing and revision-guarded recognition.
`ComposerCaptureController.kt` owns a single voice/dictation lease, origin draft
snapshots, review retention and fresh final delivery checks. `AppViewModel.kt`
reconciles real draft/member/session mutations and routes opted-in Send through
its existing attachment/reply submission. Sign-out/removal/wipe prune capture.

`DictationUi.kt`, `DictationSettingsScreen.kt` and the typed navigation route add
inline capture controls, first-use disclosure, selectable review, standard
Settings recovery and profile-owned preferences. `VoiceCapture.kt`,
`VoiceCaptureControl.kt`, `ConversationComposer.kt` and the failure dialog add
owned hold/release/RTL cancel/lock, native tap-to-lock, five-minute completion and
typed outcomes. Developer Tools selects failures; ordinary surfaces use resources.
The editor retains native selection through `TextFieldValue` and owned insertion.

Host validation: the clean full gate passed in `/tmp/wn-b17-full-gate-1.log`;
the final gate in `/tmp/wn-b17-completion-check.log` includes the localization fix
and actual AppViewModel integration checks. **550 unit tests pass**, with zero
failures/errors/skips, zero lint errors, 14 pre-existing warnings and two hints.
Both debug and instrumentation APKs assemble. Forty new unit tests cover pure
capture rules, controller ownership and actual draft/send/exit integration. Ten
new Compose interaction cases plus the updated existing voice regression compile
only: Settings choices, disclosure, editing during listening, review dismissal,
Back, permission recovery, membership refusal, tap/hold and too-short retry.

No device, microphone, recognizer, visual, gesture-runtime or background-service
execution is authorized or claimed. User visual acceptance remains pending.
Production migration must reconnect its real recognition/recorder callbacks and
authoritative revision/membership checks, not reuse the local elapsed ticks or
fixed transcript. Commit title: `B17: Add dictation and owned voice recording`.
