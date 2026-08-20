# Speech messages

## Purpose

Let people record once and decide during review whether to send the recording,
an editable transcript, or one voice message with its transcript. Let a
recipient read text aloud or create a local transcript for a voice message
without adding permanent composer controls.

## Navigation and exact copy

- The resting composer and its press-and-hold waveform remain unchanged.
- Stopping a recording opens the existing voice review. Before transcription,
  the review centers one secondary **Transcribe** action beneath playback while
  **Send Voice Message** remains available.
- After transcription, a native Menu labeled **Message Format** appears at the
  top and offers **Voice**, **Text**, and **Both**. Its compact label shows the
  selected format with one downward chevron. **Both** is selected by default.
  One trailing Send action uses the selected format.
- **Voice** sends only the recording. **Text** sends the editable transcript as
  an ordinary text message. **Both** sends one message containing the voice
  attachment and ordinary message text; it never creates two timeline entries
  or a nested transcript card.
- A received text message adds **Read Aloud** to its existing message actions.
- A received voice message with no included or local transcript adds
  **Transcribe**. After a transcript exists, that action becomes **Show
  Transcript** or **Hide Transcript** according to the current local
  presentation. A visible transcript also offers **Copy Transcript**.
  A voice message sent with authored transcript text uses that same label,
  rather than the ambiguous generic **Copy**, and copies only its text.

## Native components and behavior

- The current `LongPressGesture`, recording waveform, timer, Stop, Cancel,
  playback, and Send controls remain authoritative.
- A native `Menu` owns the three mutually exclusive send formats. It appears
  only after transcription, shows the selected value followed by one
  `chevron.down`, marks the current choice in the menu, and uses the short noun
  labels **Voice**, **Text**, and **Both**. The user-approved compact menu is an
  intentional exception to Apple's inline guidance for fewer than five options
  because the visible segmented control made this temporary composer state too
  noisy. The resting selected value and chevron use the semantic secondary
  label color so they remain subordinate to editable message text.
- The review waveform fills its bars with an explicit fully opaque black in
  Light appearance and white in Dark appearance. It does not inherit a
  subordinate foreground style from the composer material.
- While the format Menu is visible, the composer beneath it remains inert
  through the menu's dismissal animation and the selected row's touch-up. A
  choice changes only the message format and never activates playback, text
  entry, Send, or Cancel beneath the menu.
- The format Menu's native button owns the full available 44-point-high row,
  not only the visible label, so tapping anywhere across that row opens it. It
  stays 44 points high when the composer expands rather than accepting the
  expanded surface's proposed height.
- The transcript uses a plain native multiline `TextField` directly inside the
  enlarged review composer. It has no label card, nested background, or second
  bubble. It is visible and editable for **Text** and **Both**, and hidden for
  **Voice**. The captured audio remains in memory while review is open so
  switching formats is reversible. In the compact composer it grows from its
  actual text, up to eight visible lines, without reserving blank rows below a
  short transcript. Its bottom text inset matches the ordinary message
  composer.
- Text and Both reviews use the conversation's existing vertical pull to
  expand and collapse the composer. Voice-only review stays compact. The same
  Expand Message and Collapse Message accessibility actions remain available.
  In the expanded state, the format row sits at the top edge, followed by the
  voice row when present and then editable text flowing beneath it. The pull
  gesture covers the complete composer, including the format row. A normal
  movement threshold preserves a stationary tap for the Menu while a deliberate
  vertical pull from the same rectangle expands or collapses the composer.
  Choosing Voice clears any expanded state. Switching from Voice back to Text
  or Both therefore starts at the default compact size and expands only after
  another deliberate pull.
- Temporarily blocking composer interaction while the format Menu is visible
  does not change the expanded layout. The composer remains present and stable
  behind the native Menu until dismissal completes.
- The format Menu suppresses UIKit's targeted source preview with an invisible
  finite one-point animation anchor installed before presentation. This keeps
  the native menu and its system motion while the complete composer remains
  visible and stationary behind it. Dismissal supplies no target, so choosing a
  row changes the review content without pivot-morphing the Menu into a view
  that SwiftUI may update. Missing or invalid geometry safely omits the custom
  opening preview.
- The empty resting composer disables its parent pull gesture while the
  hold-to-record control is visible. The record long press recognizes after
  0.4 seconds and tolerates ordinary finger drift within 32 points.
- Transcription is deterministic and in memory in this prototype. It uses the
  bundled voice sample's local transcript and never initializes microphone,
  Speech framework, networking, persistence, or background processing.
- Transcription never blocks voice sending. Cancelling review discards its
  recording, transcript, and selected format. Sending **Text** discards the
  recording after creating one text message. Sending **Both** creates one
  message whose recording is an attachment and whose transcript is ordinary
  message text.
- With **Both**, the transcript is ordinary message text attached to the same
  message as the recording, so it naturally survives forwarding. A
  recipient-created transcript belongs only to the current conversation view
  in memory; it is never forwarded or added to the chat's authored content.
- Any transcript shown with its voice attachment has a small secondary
  **Transcribed** provenance label immediately above the plain message text.
  This applies to both sender-provided and recipient-created transcripts. It
  adds no nested surface or second bubble, and keeps the extra content-spacing
  interval after the voice row so the waveform separation does not feel
  crowded.
- `AVSpeechSynthesizer` reads received ordinary text. Starting it stops other
  local audio. While speech is active, a native determinate linear
  `ProgressView` with a speaker symbol appears beneath the message text, and
  holding the same message offers **Stop Reading**.
- The message action presentation sizes to its complete relevant action list.
  It never contains an internal `ScrollView`; the preview scales and the group
  repositions so every action, including Delete, remains visible at once.
- Opening another message action, starting media, recording, or leaving the
  conversation stops the active reading or playback.

## Important states

- Recording; voice-only review; transcribing; transcription available.
- Voice, Text, and Both selected, including edits to the transcript.
- Text and Both compact, expanding, expanded, and collapsing.
- Cancel from every review format and one explicit Send result per format.
- Received text idle, reading aloud, and stopped.
- Received voice with no transcript, recipient-created local transcript,
  ordinary sender-provided text, local transcript hidden, and local transcript
  visible.
- Empty edited transcript disables Text and Both sending without affecting the
  Voice option.

## Accessibility

- The format Menu is labeled **Message Format** and announces the selected
  value. The Send control announces **Send Voice Message**, **Send Text
  Message**, or **Send Voice and Text Message**.
- **Transcribe**, **Read Aloud**, **Stop Reading**, **Show Transcript**, and
  **Hide Transcript** are exposed as named VoiceOver actions whenever their
  visible context-menu counterparts apply.
- A visible transcript is included in the voice message's accessibility value
  with **Transcribed** provenance, matching the quiet visual label.
  The speech progress announces **Reading Aloud** and its percentage, so it
  never relies on animation or color alone.
- Native controls retain their system interaction geometry, focus behavior,
  Dynamic Type adaptation, and localization behavior.

## Relevant Apple sources

- [Menu](https://developer.apple.com/documentation/swiftui/menu)
- [TextField](https://developer.apple.com/documentation/swiftui/textfield)
- [Context menus](https://developer.apple.com/design/human-interface-guidelines/context-menus)
- [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- [ProgressView](https://developer.apple.com/documentation/swiftui/progressview)
- [Linear progress style](https://developer.apple.com/documentation/swiftui/progressviewstyle/linear)
- [SpeechAnalyzer](https://developer.apple.com/documentation/speech/speechanalyzer)
- [SpeechTranscriber](https://developer.apple.com/documentation/speech/speechtranscriber)
- [AVSpeechSynthesizer](https://developer.apple.com/documentation/avfaudio/avspeechsynthesizer)

## Observable acceptance criteria

- The empty composer has no new visible button, label, or mode.
- Stopping a voice recording preserves the existing review and exposes one
  centered secondary **Transcribe** action without delaying voice sending.
- Transcription enlarges the composer, defaults a native Menu to **Both**, shows
  the selected format with one downward chevron, shows editable ordinary
  message text, and preserves exactly one Send action.
- Every format row remains selectable when the Menu overlaps the composer. The
  same tap never activates any composer control beneath the selected row.
- The full 44-point rectangular format row opens the Menu from one tap without
  precision targeting. A deliberate pull from that same row still expands or
  collapses the composer.
- The voice-review waveform is primary black in Light appearance rather than
  the dimmed unplayed-waveform gray.
- Pulling a Text or Both review expands and collapses the composer; Voice-only
  review remains compact.
- Choosing Voice from an expanded Text or Both review collapses it. Returning
  to Text or Both remains compact until the user pulls to expand again.
- Compact transcript text keeps the same bottom inset as the ordinary message
  composer and does not touch the review surface's lower edge.
- Expanded review content begins at the top edge in format, voice, and text
  order; it is never vertically centered in the available space.
- Opening the format Menu from an expanded review leaves the expanded composer
  visible and unchanged behind the Menu. Compact review does the same: it never
  disappears into or morphs out of the Menu's presentation animation.
- Opening or closing the format Menu never produces invalid preview geometry,
  raises a UIKit preview-target exception, or stalls the app.
- Holding the resting waveform begins recording without the composer pull
  gesture cancelling small finger movement.
- Sending Voice creates one voice-only message. Sending Text creates one
  ordinary text message. Sending Both creates one playable message with its
  transcript rendered as ordinary message text beneath the voice attachment,
  introduced by a small secondary **Transcribed** label.
- An incoming text message offers Read Aloud but no transcription command. An
  incoming voice-only message offers transcription but no Read Aloud command.
- The message action presentation shows every relevant action without an
  internal scroll gesture or clipped final action.
- Read Aloud adds visible determinate progress beneath the message text and
  removes it when reading stops or finishes.
- A recipient-created transcript can be shown, hidden, and copied locally
  without mutating or forwarding the source message. When shown, it uses the
  same secondary **Transcribed** label as a sender-provided voice transcript.
- A voice message sent with its transcript offers **Copy Transcript**, and the
  action copies its text without implying that the audio itself is copied.
- Existing recording, voice playback, message actions, forwarding, reply,
  selection, deletion, and ordinary composer behavior remain functional.
- Completion still requires user acceptance after hands-on inspection.
