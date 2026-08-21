# Composer, attachments, media, and speech

Status: Visual-polish static gate passed on 2026-08-21; device acceptance pending

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/conversation-composer-states.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-shared.md`
- `wn-ios-prototype@0bd7cba:docs/screens/speech-messages.md`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeComposerTextView.swift`
- `wn-ios-prototype@0bd7cba:WhiteNoisePrototype/Screens/Conversation/PrototypeComposerMediaViewer.swift`
- composer, attachment, media, and voice cases in `PrototypeChatModelTests.swift` and `ChatFlowsUITests.swift`

## Android-native adaptation

- Keep one Scaffold bottom composer. A Material multiline text field grows to
  ten visible lines; an explicit expand/collapse action provides a bounded
  full-height editor without copying iOS pull geometry. IME insets, focus,
  Back, and Return are Android-owned.
- A Material modal bottom sheet owns **Camera**, **Photos and videos**,
  **File**, **Contact**, and deterministic **GIF** choices.
- Use Android Photo Picker for visual media and `ACTION_OPEN_DOCUMENT` for
  files. Use `ActivityResultContracts.TakePicture` with an app `FileProvider`
  URI for the external camera app; White Noise requests no camera or storage
  permission.
- Photo Picker, Files, and the external camera retain their default system or
  OEM appearance without app color overrides.
- Maintain one profile-owned ordered draft queue across navigation. Every item
  has a visible 48dp removal action. Sending clears text, attachments, link
  suppression, and voice-review state atomically.
- The first eligible HTTPS URL produces deterministic local metadata. Closing
  its preview suppresses only that URL; the prototype never fetches metadata.
- Media bubbles use count-based Compose grids. A full-screen Compose dialog
  with `HorizontalPager` owns chronological visual-media paging and is shared
  by the timeline and draft shelf. Video/file URIs hand off to an Android app
  through `ACTION_VIEW` when supported.
- Voice recording is an explicitly deterministic local simulation: press and
  hold or the named accessibility button starts a waveform/timer state; Stop
  opens review. It never initializes `RECORD_AUDIO`, SpeechRecognizer, a
  network service, or background processing.
- Voice review supports Voice, Text, and Both. Transcription uses the pinned
  local sentence, stays editable, and produces exactly one message. Recipient
  transcription is view-local; Android `TextToSpeech` provides Read Aloud for
  ordinary incoming text.

## Visual-polish translation

- The composer is one quiet `surfaceContainerHigh` task region rather than a
  stack of independent controls. Add, input, Send, and Record remain stable
  Material actions; the primary Send action appears only when the draft is
  sendable.
- Draft media, files, contacts, GIFs, links, replies, mentions, preparation,
  and failure feedback share the app shape, type, spacing, and tonal roles.
  Removal controls retain 48dp targets and standard Close icons.
- Attachment and deterministic picker sheets use Material list rows, semantic
  icons, transparent row containers, bounded height, and platform sheet
  behavior. Photo Picker, Files, and external camera presentation remain
  system-owned and unchanged.
- Draft and read-only media viewers are edge-to-edge modal tasks with safe-area
  `TopAppBar`s, `HorizontalPager`, fit-scaled media, chronological thumbnails,
  explicit selected state, and named Close/Done actions. Opening a draft item
  starts on that exact item.
- Voice record/review and received voice cards use code-native waveforms,
  standard Play/Pause icons, tonal controls, progress, transcript provenance,
  and editable Voice/Text/Both review. Read Aloud now exposes live progress
  while preserving Android `TextToSpeech` ownership and lifecycle shutdown.
- Official sources rechecked for this pass: [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input),
  [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets),
  [Compose pager](https://developer.android.com/develop/ui/compose/layouts/pager),
  [Material inset handling](https://developer.android.com/develop/ui/compose/system/material-insets),
  and [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker).

## Deterministic composer catalog

- Preserve the twelve accepted Composer rows and exact draft text.
- Seed the corresponding photo, four-photo album, mixed image/video, file,
  GIF, contact, reply, link-preview, and suppressed-link draft artifacts.
- Draft list previews derive from draft text/attachments, never from a second
  mutable row fixture.

## Acceptance gates

- Unit tests cover draft persistence/clearing, order/removal, link detection
  and suppression, one-to-seven media layout derivation, attachment preview
  derivation, voice format results, and per-profile isolation.
- Compose tests for text, photo album, mixed media, link preview, file/contact,
  recording/review, and received voice states compile into the instrumentation
  APK.
- Focused semantics cover the attachment-menu actions, draft attachment count,
  exact media-page targeting, link-preview removal, voice record/review,
  transcription, message-format choice, playback, and transcript actions.
- The clean static gate succeeds with no lint issue, and the merged APK still
  declares no camera, storage, microphone, network, notification, or location
  permission.
