# Read Aloud preferences and auto-read

## Purpose and scope

B16 implements C063–C066 after B15: engine/voice availability and selection,
engine trust consent, rate and media mixing, global/per-chat auto-read and local
background/notification-control states. Reuse the app-owned foreground speech
queue. Preferences stay profile-owned and in memory. No background service,
notification delivery, engine installation, new permission, backend or network
is introduced. Q06 remains the boundary for real background playback/controls.

## Product parity and behavior

- Read Aloud settings distinguishes checking engines, none usable, usable and
  discovery/selection failure. Refresh and Android Settings provide recovery.
  Engine changes are transactional: failed, stale or wrong-profile completion
  retains the prior selection. Engine labels are display data, never authority.
- Voices use exact engine/name/locale identities. Show unavailable same-language
  voices with a reason: not installed, network required, duplicate identity or
  missing identity. These options cannot be selected. Automatic offline voice
  uses deterministic quality/name/locale ordering; a missing saved choice stays
  saved while the effective fallback is disclosed. A failed setVoice operation
  cannot be shown as the effective choice.
- Unknown engine use requires explicit engine-scoped, profile-owned consent
  before handing it message text. Dismissal starts nothing. Platform fallback
  binding cannot silently inherit a requested package's on-device trust.
  White Noise still selects only voices marked installed and offline; unknown
  engine trust is a separate disclosure, not proof that an engine cannot network.
- Speech rate offers System, 0.5, 0.75, 1, 1.25, 1.5, 2, 2.5 and 3 times normal,
  plus Custom from 0.1 to 10. Exact presets stay exact; other valid values round
  to one decimal using half-up. Reject malformed, nonfinite and out-of-range
  input without replacing the saved value. Apply rate at an utterance boundary.
- Speak over playing media is an explicit opt-in. It requires active other
  media and leaves that media untouched. Quiet/Medium/Loud select speech-engine
  volume 35/60/85 percent. Without this opt-in, focus loss pauses rather than
  destroys the queue. Recovery preserves the cursor and needs explicit Resume.
  Deterministic media/focus outcomes must be reachable through Developer Tools.
- Read messages aloud by default is off initially. Each Chat/Group Info offers
  Use default (On/Off), On or Off. Capture the unread boundary when opening the
  conversation, read its bounded backlog once, then append eligible arrivals
  only to that chat/profile's owned auto-read session. Manual speech replaces
  auto ownership only after a successful start. Turning off effective auto-read
  stops auto-owned speech without stopping manual playback.
- Cursor identity/order separates new arrivals from historical pages, edited
  messages, repeated snapshots and duplicate IDs. Foreground return may read
  arrivals after the captured cursor if no newer active/manual session owns
  playback. Lock, sign-out or a different profile invalidates that resumption.
  Opening another chat must never append its messages to the previous queue.
- Actual backgrounding continues to stop native speech as established in B15.
  A developer-only playback-controls example models foreground/background,
  scheduled lock expiry, notification start failure and Pause/Resume/Stop/source
  commands. Commands carry session/profile identity and cannot affect a newer
  session. The example never posts a notification or starts a service.

## Entry, navigation, Back and Android composition

Add Read Aloud to Settings using its existing tonal grouped rows and detail
scaffold. Engine, voice, rate and volume choices use shared Material choices and
scrollable sheets/dialogs. Custom rate uses the shared labeled tonal field,
locale-aware decimal keyboard, inline validation, Apply and Cancel. Engine trust
uses the shared Material AlertDialog with a specific explanation and safe Cancel.
Per-chat auto-read lives in the existing Chat/Group Info preferences group.

Back dismisses the current choice/consent first, then returns to Settings or
Chat Info. Cancelling a picker leaves preferences unchanged. Profile switch and
route disposal cancel pending selection/consent ownership. Reading may continue
while ordinary settings routes are open, following B15's foreground policy.
Settings and speech failures use resources and actionable recovery, not raw
engine exceptions or private identifiers. System Settings retains platform UI.

## Exact product copy

Read Aloud; Engine; Voice; Speech rate; System (follow device setting);
Automatic offline voice; Available offline; Not installed;
Requires a network connection; Duplicate voice identity; Voice unavailable;
No offline voice available; Saved voice unavailable; using %1$s;
Checking speech engines…; No usable speech engine is available.;
Couldn’t check speech engines.; Couldn’t change the speech engine.;
Refresh; Open Android Settings; Custom; Apply; Cancel;
Enter a rate from 0.1× to 10.0×.; Speak over playing media;
Only starts while other audio is playing. White Noise does not control that audio.;
Speech-over-media volume; Quiet; Medium; Loud;
Start music or a podcast before using speech-over-media.;
Read messages aloud by default; Read the unread backlog when a chat opens.;
Read messages aloud; Use default (On); Use default (Off); On; Off.

External speech engine: The selected speech engine is a separate app. It may
send message text to its developer’s servers. White Noise selects voices marked
available offline, but cannot verify how this engine handles your text.
Use engine / Read aloud is the specific confirmation for settings / pending use.
Cancel is always available. No real installation is offered or claimed.

## Accessibility and adaptation

Reuse native settings rows, switches, selectable semantics and 48 dp targets.
Voice rows announce language and eligibility; disabled choices include the reason.
Checked state reflects saved preference; effective fallback is separately named.
Rows and helper copy wrap at 200% type. Pickers scroll on short windows and keep
IME/system insets at their native owner. Maintain monochrome semantic roles,
RTL, logical focus, keyboard/Voice Access, meaningful labels and non-color errors.
The custom rate field is a native decimal input, not a slider-only interaction.

## Governing Android sources

Checked 2026-09-04:

- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech): asynchronous initialization, installed engines, setVoice/setSpeechRate and utterance parameters.
- [Voice](https://developer.android.com/reference/android/speech/tts/Voice): exact voice identity, language, quality and network requirement.
- [Audio focus](https://developer.android.com/media/optimize/audio-focus): foreground eligibility, focus loss and retained pause; no background service expansion.
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input): native labeled input and keyboard handling; shared White Noise field remains authoritative.

## Production and iOS evidence

Pinned production master remains `319454889f1c2494dec4a69b5577d98017f44eee`.
`ui/settings/TextToSpeechScreen.kt`, `audio/tts/TtsEngineResolver.kt`,
`TtsEngineSelection.kt`, `TtsTrustWarning.kt`, `TtsAudioFocusOwner.kt`,
`state/TtsVoicePreferences.kt`, `TtsRatePreferences.kt`, `TtsMediaMixPreferences.kt`,
`TtsAutoReadPolicy.kt`, `AppStateTtsVoiceMedia.kt`,
`ui/conversation/ConversationTtsEffects.kt`, `ConversationAutoReadCursor.kt` and
`state/AppState.kt` define the behavior. Production reconnects its preferences,
engine handles, authoritative history cursor, lifecycle and foreground service.
The local [source map](../port/source-map.md) and
[B15 brief](read-aloud-transport.md) retain the accepted iOS speech boundary.
[F07](../audits/production-android-parity/flows/F07-read-aloud.md) selects these
Android-only outcomes; they do not authorize copying production presentation.

## Observable acceptance criteria

- Host tests cover rate normalization, voice identity/fallback/eligibility,
  failed/stale engine selection and consent/profile isolation.
- Auto-read tests cover entry backlog, overrides, arrivals, historical/duplicate
  suppression, manual-session preservation, stop/lock/profile boundaries and
  foreground-resume ownership. Local notification commands reject stale owners.
- UI cases cover settings choices, disabled reasons, custom-rate validation,
  consent Cancel/confirm, source return and chat overrides. Compile only.
- Full host gate passes before B16 is marked implemented and committed. Actual
  device speech, system surfaces, notification/background behavior and visual
  acceptance remain unverified pending explicit device authorization.

## Implementation evidence

B16 implements C063–C066 within the authorized local scope. The Settings hub
opens `ReadAloudSettingsScreen`; Chat/Group Info includes `ChatAutoReadSetting`.
Both use existing settings groups, native selectable rows, shared dialogs and
the labeled tonal field. Product strings live in `strings.xml`.

- `model/SpeechPreferences.kt` owns profile-scoped engine/voice consent, offline
  eligibility, exact voice identity, deterministic fallback, rates, media policy
  and global/per-chat overrides. `AppViewModel.updateSpeechPreferences` applies
  reducers only to the matching current profile, preserving unrelated settings.
- `ui/conversation/SpeechPlatform.kt` discovers native engines without authored
  text, resolves installed offline voices, times out initialization and releases
  cancelled/stale candidates. Successful adoption stops the old engine; failure
  retains it. Refresh retains an unchanged usable handle. Runtime binding stays
  unverified/unknown because Android's public enumeration is not binding proof.
  Rate and volume apply at utterance boundaries; native focus loss pauses the
  queue and requires explicit Resume. Mixing does not request audio focus.
- `ReadAloudController` gates message and file starts with engine/profile-scoped
  consent and media/focus eligibility. Cancellation or stale source/profile
  starts nothing. A failed manual replacement preserves the old paused queue
  and automatic ownership. Engine/voice selection adopts its configured result
  before replacing saved preferences. Profile changes clear pending operations.
- `model/SpeechAutoRead.kt` captures unread boundaries and arrival cursors.
  Conversation captures unread IDs before visibility acknowledgements and opens
  its speech entry once the engine is ready. Backlog scans at most 100 rows and
  takes 50 eligible messages; arrivals are deduplicated and append at the logical
  tail without changing the active callback or expanding the prepared window.
  Manual starts, explicit Stop, different profiles, disabled effective auto-read
  and background return obey queue ownership. Background stops actual speech;
  returning can read only new arrivals when no newer/manual queue intervened.
- `SpeechCatalogExamples`, the controller's developer outcomes and
  `SpeechDeveloperDialog` expose checking/empty/failure/offline/missing-voice/
  selection-failure catalogs, media/focus conditions and a separate local
  background-controls example. The latter models notification start failure,
  foreground/background, immediate/delayed lock, profile exit and session-owned
  Pause/Resume/Stop/source commands. Old commands cannot control a new example.
  It does not create a notification, service or platform lock implementation.

Validation: **510 unit tests pass**, with no failures/errors/skips. B16 adds
45 host tests: 16 preference rules, 10 cursor/lifecycle rules, 11 queue/coordinator
cases and 8 consent/selection/media cases. Eight new Compose interaction cases
compile only: chat override/inheritance, custom-rate validation, unavailable
voices, consent cancellation/adoption, playback consent, conditional volume,
refresh recovery and stale notification commands. Existing B15 cases also compile.

The clean gate passes in `/tmp/wn-b16-full-gate-1.log` (83 tasks). Final
verification after the selection-cancellation/audio-fixture review passes in
`/tmp/wn-b16-completion-check.log`. Lint reports zero errors, 14 pre-existing
warnings and two hints. Both debug and instrumentation APKs assemble; no
instrumentation tests run. Commit title: `B16: Add Read Aloud preferences and auto-read`.

Q06 remains open for real background playback, notification delivery, services,
permissions and device execution. Native engine output, external Settings,
audio focus on hardware, visual acceptance and accessibility on a device have
not been executed or claimed. No backend, persistence, installer, network or
new permission is introduced.
