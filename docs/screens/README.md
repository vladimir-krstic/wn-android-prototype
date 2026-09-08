# Android screen and flow briefs

These briefs own product behavior, current Android composition, copy, state,
and acceptance criteria. Later dated user-approved changes supersede earlier
presentation notes within their scope. Historical test counts apply only to
the recorded build; see [current engineering evidence](../codebase-hardening-audit.md).

For new selected work, follow [the brief instructions](AGENTS.md) and
[template](../templates/screen-brief.md). Use [the source map](../port/source-map.md)
for pinned evidence and [the parity ledger](../port/feature-inventory.md) for status.

## Foundation and shared UI

- [Native Android application foundation](app-foundation.md)
- [App-owned Material menus](app-menus.md)
- [Shared sections and settings polish](shared-settings-polish.md)

## Access and profiles

- [Onboarding and profile foundation](onboarding-and-profiles.md)
- [Optional quick account switching](quick-account-switching.md)
- [Access and recovery — production Android batch B01](access-and-recovery.md)
- [Keys and profile exit — production Android batch B02](keys-and-profile-exit.md)
- [People discovery and private details](people-discovery-and-private-details.md)
- [Profile media and Lightning address](profile-media-and-lightning.md)

## Chats, history, and messages

- [Chats and chat creation](chats-and-chat-creation.md)
- [Chat organization and recovery](chat-organization-and-recovery.md)
- [Chat folders](chat-folders.md)
- [Global search](global-search.md)
- [Shared conversation core](shared-conversation-core.md)
- [Conversation history, reading and message details](conversation-history-and-reading.md)
- [Message interactions and conversation search](message-interactions-and-search.md)
- [Message editing, full reader and text selection](message-editing-and-reading.md)
- [Message moderation, batch deletion and forwarding](message-moderation-and-forwarding.md)
- [Verified event cards and readers](verified-event-cards-and-readers.md)
- [AI agents and streaming operations](ai-agents-and-streaming-operations.md)

## Composer, media, and speech

- [Composer, attachments, media, and speech](composer-media-and-speech.md)
- [Composer media acquisition and attachment actions](composer-attachment-actions.md)
- [Draft photo editor](draft-photo-editor.md)
- [Text attachments and Shared Content](text-attachments-and-shared-content.md)
- [Location sharing](location-sharing.md)
- [Read Aloud transport and source navigation](read-aloud-transport.md)
- [Read Aloud preferences and auto-read](read-aloud-preferences.md)
- [Dictation and voice-note interaction](dictation-and-voice-recording.md)

## Groups and sharing

- [Chat and group information](chat-and-group-information.md)
- [Group setup, images and roster recovery](group-setup-images-and-roster.md)
- [Group administration, ended groups and transcript export](group-administration-and-transcript.md)
- [Disappearing-message timers and expiry](disappearing-timers-and-expiry.md)
- [Incoming sharing, shortcut targets and profile links](incoming-sharing-and-profile-links.md)

## Settings, privacy, and diagnostics

- [Settings and profile services](settings-and-profile-services.md)
- [Privacy & Security](privacy-and-security.md)
- [App lock and sensitive privacy](app-lock-and-sensitive-privacy.md)
- [Global and per-chat notification controls](notification-controls.md)
- [Notification routing and inline actions](notification-routing-and-actions.md)
- [Appearance, typography and input](appearance-typography-and-input.md)
- [Data Usage](data-usage.md)
- [Downloads and media quality](downloads-and-media-quality.md)
- [Relays](relays.md)
- [Relay publication and validation](relay-publication-and-validation.md)
- [Diagnostics and first-login privacy choices](diagnostics-and-improvements.md)
- [Key packages and developer diagnostics](key-packages-and-developer-diagnostics.md)
- [Developer and destructive flows](developer-and-destructive-flows.md)
- [Help, About and open source licenses](help-about-and-licenses.md)
- [Distribution-gated app updates](distribution-gated-app-updates.md)

- [Pinned messages](pinned-messages.md) — message pinning, browsing and exact-history navigation (#17).
- [Message translation](message-translation.md) — manual translation, original toggle and profile/chat automatic translation controls (#23).

- [Jump to date](jump-to-date.md) — calendar navigation from conversation search (#18).

- [Files and media from main search](global-files-and-media.md) — browse attachments across the current account (#19).

- [Timestamps inside message bubbles](message-inline-timestamps.md)
