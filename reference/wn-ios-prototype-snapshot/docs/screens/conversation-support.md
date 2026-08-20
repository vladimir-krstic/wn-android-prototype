# White Noise Support Conversation

## Purpose and navigation

Provide one reusable conversation where a person can ask a White Noise
question, report a problem, or share a suggestion. Settings can create or open
it, and its Chats row always opens that same destination. A profile can never
contain more than one support conversation.

## Exact copy

- Identity: **White Noise Support**
- Settings summary: **Questions, problems, and suggestions**
- Settings explanation: **Ask how something works, report a problem, or share
  a suggestion.**
- New action: **Start Chat**
- Existing conversation action: **Start Chat**
- System guidance: **How can we help? Ask a question, report a problem, or
  share a suggestion. We’ll reply here.**
- Composer placeholder: **Message**

## Native components

- A native pushed navigation destination preserves the originating Settings or
  Chats Back behavior.
- The toolbar uses the same compact identity composition as the Fiatjaf
  conversation, with the outline SF Symbol `questionmark.bubble` in an adaptive
  semantic-fill circle.
- The support introduction uses the centered secondary timeline-event
  treatment used by day and membership events. It is not an incoming message
  bubble and has no sender timestamp.
- Support uses the shared profile-owned chat model and shared conversation;
  there is no separate Support message collection or bespoke composer.
- The timeline reuses the accepted incoming and outgoing message bubbles,
  external timestamps, native scrolling, soft bottom scroll-edge treatment,
  and Liquid Glass composer structure from `conversation-fiatjaf.md`.
- The composer provides text sending, Camera, PhotosPicker, file importing, and
  the shared deterministic waveform recording state. Starting it dismisses
  text focus; Stop sends the voice message and restores text entry.
- Governing Apple sources:
  [NavigationLink](https://developer.apple.com/documentation/swiftui/navigationlink),
  [Button](https://developer.apple.com/documentation/swiftui/button),
  [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker),
  [fileImporter](https://developer.apple.com/documentation/swiftui/view/fileimporter(ispresented:allowedcontenttypes:allowsmultipleselection:oncompletion:)),
  and [SF Symbols](https://developer.apple.com/sf-symbols/).

## Deterministic behavior

- Stable conversation ID: `white-noise-support`.
- Marmota's populated fixture places White Noise Support immediately after
  Fiatjaf.
- Starting support for a profile without the row inserts it once. Repeated
  activation finds the stable ID and opens the existing conversation.
- A new conversation requires an available Chat Messages relay. Once created,
  it remains usable like other existing conversations.
- An empty support conversation shows only the support guidance. **Today** is
  added beneath it only after the first actual message is sent.
- Sent text, selected images, and chosen-file names remain in the active
  profile's in-memory support conversation for the process lifetime. The Chats
  preview updates to the latest sent content. No support backend, ticket
  system, or network operation exists.
- Selected photos are downsampled off the main actor before storage so timeline
  rendering never repeatedly decodes the original full-resolution source.

## Accessibility

- The support avatar is decorative wherever adjacent text names the identity.
- The support guidance is one accessibility element identified as support
  information with its complete content.
- Start Chat exposes whether it creates or returns to the conversation. When a
  new chat is unavailable, its hint routes the person toward profile relays.
- Composer controls retain explicit Add Attachment, Record Voice Message, Stop
  Recording, and Send labels.

## Acceptance criteria

- White Noise Support appears directly below Fiatjaf in the populated Chats
  list with the question-mark speech-bubble avatar.
- Settings clearly communicates the three supported reasons to chat.
- Start Chat inserts exactly one row and pushes the support conversation.
- Start Chat and the Chats row always push the same conversation.
- The centered system guidance explains what a person can ask, report, or
  suggest.
- Text, Photos, Files, Contacts, and voice-control interactions have coherent
  outcomes.
- No product surface claims to create a ticket or contact a live backend.
