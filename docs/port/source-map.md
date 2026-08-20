# iOS-to-Android source map

All paths below are relative to the repository root and point into the pinned,
read-only snapshot at `reference/wn-ios-prototype-snapshot/`.

Use this map to select evidence, not to derive Android package names or port
Swift types one-for-one.

## Product-wide evidence

| Need | Snapshot paths |
| --- | --- |
| Product scope | `README.ios.md`, `docs/decisions.md` |
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
| Share & Connect | Settings brief section plus verified-address brief | `Screens/Settings/ShareAndConnectView.swift`, `Screens/Shared/ShareableCodeViews.swift`, `QRCodeImageGenerator.swift`, `Screens/Settings/ProfileCodeScannerView.swift` | Profile/QR assets and lifecycle tests |
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

