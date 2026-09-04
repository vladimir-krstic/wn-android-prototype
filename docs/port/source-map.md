# iOS-to-Android source map

## Production Android extension

The 2026-09-04 all-batch implementation authorization adds production Android
capability evidence at `marmot-protocol/whitenoise-android@319454889f1c2494dec4a69b5577d98017f44eee`.
The audit matrix is the permanent source map for those capabilities. B01 maps
identity input, import/recovery, Amber, retained reactivation and bootstrap to
`docs/screens/access-and-recovery.md`. B02 maps key availability/export and
staged sign-out/wipe to `docs/screens/keys-and-profile-exit.md`; the established Android presentation
remains authoritative. Production is read-only evidence and does not replace
the accepted iOS scope or prototype-only features below. B03 maps discovery,
private contact details, created-chat recovery and profile-to-group actions to
`docs/screens/people-discovery-and-private-details.md`. B04 maps profile banner,
avatar viewing, Lightning validation and generated name drafts to
`docs/screens/profile-media-and-lightning.md`. B05 maps pinned ordering, selection,
local/leave-first deletion and connectivity recovery to
`docs/screens/chat-organization-and-recovery.md`; folder assignment is the
manual foundation for B06. B06 maps folder preferences/rules, management/editor,
defaults/order and contextual assignment to `docs/screens/chat-folders.md`.
B07 maps cross-chat body results, typed search filters, identifier lookup and
voice-query entry to `docs/screens/global-search.md`; existing Conversation
accepts the exact loaded-message ID, while B08 owns unloaded-target recovery.

All iOS paths below are relative to the root of the private
[`wn-ios-prototype`](https://github.com/vladimir-krstic/wn-ios-prototype)
repository at the accepted, read-only parity baseline
`0bd7cbae56c92f07c7639be78b9bb62f8e5297cb`. Screen briefs use the compact
notation `wn-ios-prototype@0bd7cba:<path>` for the same source.

When the sibling checkout is available, inspect one path without changing its
working tree using:

```bash
git -C ../wn-ios-prototype show \
  0bd7cbae56c92f07c7639be78b9bb62f8e5297cb:<path>
```

See `docs/references/ios-prototype.md` for provenance and drift rules.

## Scoped 2026-08-26 authorization

The approved Chats polish and first-login privacy flow use read-only iOS commit
`4c25393f0eb694fc5838d3a451e17db9a6abbcbe` as scoped evidence. The global
`0bd7cba` baseline, Android conversation fixtures, and deterministic timestamps
remain unchanged. Relevant delta paths:

- `docs/screens/diagnostics-and-improvements.md`
- `WhiteNoisePrototype/App/PrototypeDiagnosticsState.swift`
- `WhiteNoisePrototype/App/WhiteNoisePrototypeApp.swift`
- `WhiteNoisePrototype/Screens/Shared/DiagnosticsAndImprovementsViews.swift`
- `WhiteNoisePrototype/Screens/Chats/ChatsView.swift`, `NativeChatList.swift`,
  `ChatListRow.swift`, and `ChatListItem.swift`
- `WhiteNoisePrototype/Screens/Settings/SettingsView.swift` and
  `WhiteNoisePrototypeTests/ProfileLifecycleTests.swift` for the explicitly
  approved Settings-hub/profile-switching follow-up

Android contracts are in `docs/screens/chats-and-chat-creation.md`,
`docs/screens/diagnostics-and-improvements.md`, and
`docs/screens/settings-and-profile-services.md`. No other upstream drift is
implicitly accepted.

## Scoped 2026-09-03 info-screen authorization

The user's Google-style Chat/Group Info redesign explicitly requests the iOS
content hierarchy. Read-only comparison uses
`4c25393f0eb694fc5838d3a451e17db9a6abbcbe:WhiteNoisePrototype/Screens/Conversation/ChatInfoView.swift`.
Direct identity/quick actions precede Shared in Chat and Chat Actions. Groups
place Advanced before Members, followed by separate admin management and
lifecycle actions. Developer Tools is retained through Android's existing
gated Conversation Debug route. The global baseline remains unchanged.

Use this map to select evidence, not to derive Android package names or port
Swift types one-for-one.

## Product-wide evidence

| Need | Pinned iOS paths |
| --- | --- |
| Product scope | `README.md`, `docs/decisions.md` |
| Product voice and terms | `docs/product-language.md`, `docs/terminology.md` |
| App/root routing | `WhiteNoisePrototype/App/WhiteNoisePrototypeApp.swift` |
| Profile model | `WhiteNoisePrototype/App/PrototypeProfile.swift` |
| Chat model and derivations | `WhiteNoisePrototype/App/PrototypeChatModels.swift`, `PrototypeChatOperations.swift`, `PrototypeMediaLayout.swift` |
| Deterministic fixture graph | `WhiteNoisePrototype/App/PrototypeChatFixtures.swift`, `Screens/Chats/ChatListFixtures.swift` |
| Settings and relay state | `WhiteNoisePrototype/App/PrototypeSettingsState.swift` |
| Playback and speech fixture | `WhiteNoisePrototype/App/PrototypePlaybackCoordinator.swift`, `PrototypeVoiceSample.swift` |
| Debug derivations | `WhiteNoisePrototype/App/PrototypeChatDebug.swift` |
| Product test contract | `WhiteNoisePrototypeTests/` |
| End-to-end behavior | `WhiteNoisePrototypeUITests/` |

## Flow evidence

| Android flow | Briefs | Primary iOS implementation | Important tests/resources |
| --- | --- | --- | --- |
| Welcome | `docs/screens/welcome.md` | `WhiteNoisePrototype/Screens/Welcome/WelcomeView.swift` | White Noise mark under `WhiteNoisePrototype/Resources/Assets.xcassets/WhiteNoiseMark.imageset/` |
| Sign In and QR | `docs/screens/login.md`, `qr-scanner.md` | `Screens/Login/LoginView.swift`, `QRScannerView.swift`; `Screens/Shared/PhysicalQRCodeScannerView.swift` | `Resources/Assets.xcassets/QRScannerBackdrop.imageset/` |
| Sign Up and avatar | `docs/screens/sign-up.md`, `verified-nostr-address.md` | `Screens/SignUp/SignUpView.swift`, `AvatarWebImagePicker.swift`; `Screens/Shared/ProfileComponents.swift`, `ProfileAvatarImageProcessor.swift` | `WhiteNoisePrototypeTests/AvatarImageTests.swift`, `ProfileLifecycleTests.swift`; Avatar and ProfileAvatar image sets |
| Chats empty/populated | `docs/screens/chats-empty.md`, `chats-populated.md` | `Screens/Chats/ChatsView.swift`, `NativeChatList.swift`, `ChatListRow.swift`, `ChatListItem.swift`, `ChatListFixtures.swift` | `WhiteNoisePrototypeUITests/PinReorderMotionUITests.swift`; chat model tests for row projection, order, dates, and clustering |
| Chat creation and profiles | `docs/screens/chat-creation.md`, `person-profile.md`, `verified-nostr-address.md` | `Screens/Chats/NewChatView.swift`; `Screens/Shared/ProfileComponents.swift` | `WhiteNoisePrototypeTests/PrototypeChatModelTests.swift`, `ProfileLifecycleTests.swift`; member/profile avatars |
| Shared conversation | `docs/screens/conversation-shared.md`, `conversation-fiatjaf.md`, `conversation-support.md`, `chat-invitation.md`, `disappearing-message-indicators.md` | `Screens/Conversation/ConversationView.swift`, `PrototypeMessageBubble.swift`, `MessageBubbleComponents.swift`, `PrototypeChatSharedViews.swift` | `WhiteNoisePrototypeTests/PrototypeChatModelTests.swift`, `SupportChatTests.swift`; `WhiteNoisePrototypeUITests/ChatFlowsUITests.swift` |
| Composer and attachments | `docs/screens/conversation-composer-states.md`, `conversation-shared.md` | `ConversationView.swift`, `PrototypeComposerTextView.swift`, `ConversationAttachmentMenuButton.swift`, `PrototypeComposerLinkPreview.swift`, `PrototypeComposerMediaViewer.swift`, `PrototypeComposerRemoveButton.swift`, `ConversationCameraCaptureView.swift` | Composer and attachment cases in `PrototypeChatModelTests.swift` and `ChatFlowsUITests.swift`; media, PDFs, and video under Resources |
| Speech messages | `docs/screens/speech-messages.md`, `conversation-composer-states.md` | `ConversationView.swift`, `PrototypeAudioWaveform.swift`, `PrototypeMessageBubble.swift`; app playback/voice files | Voice tests in `PrototypeChatModelTests.swift` and `ChatFlowsUITests.swift`; `WhiteNoisePrototype/Resources/ChatTrailClip.mp4` is video, while the generated voice sample lives in source |
| Message actions | `docs/screens/message-actions.md` | `Screens/Conversation/MessageActionFlowViews.swift` | Reaction, reply, delete, forward, retry, and selection tests in `PrototypeChatModelTests.swift` and `ChatFlowsUITests.swift` |
| Conversation search | `docs/screens/conversation-search.md` | `Screens/Conversation/ConversationView.swift` | Search model/UI tests |
| Chat and Group Info | `docs/screens/chat-info.md`, `group-info.md`, `person-profile.md` | `Screens/Conversation/ChatInfoView.swift`; `Screens/Chats/NewChatView.swift` for shared profile/group surfaces | Membership, role, relay, metadata, media-index, and leave tests |
| Settings hub | `docs/screens/settings.md` | `Screens/Settings/SettingsView.swift` | `PrototypeSettingsState.swift`, `ProfileLifecycleTests.swift` |
| Share & Connect | Settings brief section plus verified-address brief | `Screens/Settings/ShareAndConnectView.swift`, `Screens/Shared/ShareableCodeViews.swift`, `QRCodeImageGenerator.swift`, `Screens/Settings/ProfileCodeScannerView.swift` | Android `ShareConnectScreen.kt`, shared `PrivateKeyQrScannerScreen.kt`, `SettingsScreenTest`; iOS Profile/QR assets and lifecycle tests |
| Profile and keys | Settings brief sections | `Screens/Settings/ProfileSettingsView.swift`, `ProfileKeysSettingsView.swift` | `ProfileLifecycleTests.swift`, `AvatarImageTests.swift` |
| Preferences | Settings brief: Notifications, Appearance, Data Usage | `Screens/Settings/PreferenceSettingsViews.swift` | `PrototypeSettingsState.swift` |
| Privacy and relays | Settings brief: Privacy & Security, Relays | `Screens/Settings/PrivacyAndRelaySettingsViews.swift`, `Components/RelayWarningLink.swift` | `WhiteNoisePrototypeTests/RelayAvailabilityTests.swift` |
| Support and Donate | Settings brief sections; `conversation-support.md` | `Screens/Settings/SupportAndDonateSettingsViews.swift` | `WhiteNoisePrototypeTests/SupportChatTests.swift` |
| Developer Tools | Settings brief section | `Screens/Settings/DeveloperSettingsViews.swift`, `Screens/Conversation/ChatDeveloperToolsView.swift` | `WhiteNoisePrototypeTests/DeveloperToolsTests.swift` |
| Sign Out and erase | `docs/screens/sign-out.md` | `Screens/Settings/SignOutSettingsView.swift` | `WhiteNoisePrototypeTests/SignOutFlowTests.swift`, `WhiteNoisePrototypeUITests/ProfileExitFlowUITests.swift` |

## Android translation rule

For each flow:

1. Read the brief for product intent and accepted copy.
2. Read the model/operation tests for behavioral invariants.
3. Read only enough SwiftUI/UIKit to resolve missing behavior or visual
   hierarchy.
4. Inspect only the resources used by that flow.
5. Write the Android brief before implementation.
6. Choose Android components from current official guidance; never make Swift
   type names or Apple metrics the Android architecture.
