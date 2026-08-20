# Composer, attachments, media, and speech

Status: Implemented; static verification complete on 2026-08-15

## Source evidence

- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-composer-states.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-shared.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/speech-messages.md`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Conversation/PrototypeComposerTextView.swift`
- `reference/wn-ios-prototype-snapshot/WhiteNoisePrototype/Screens/Conversation/PrototypeComposerMediaViewer.swift`
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
- The clean static gate succeeds with no lint issue, and the merged APK still
  declares no camera, storage, microphone, network, notification, or location
  permission.
