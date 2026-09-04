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
voice-query entry to `docs/screens/global-search.md`. B08 maps complete local
history search, older/newer pages, exact targets, captured/visible unread state
and Message Details metadata to `docs/screens/conversation-history-and-reading.md`.
Production ConversationHistorySearch, page/target/read controller methods and
MessageInfoSheet are the integration seams; prototype history stays in memory.
B09 maps authenticated edit aggregation and pending/retry/discard controller events,
MessageFullScreen/EditHistory, native passage selection and MarkdownRenderer into
`docs/screens/message-editing-and-reading.md`. Accepted text is authoritative;
local document/source annotations reconnect to production AST/selection data.
Encoded Nostr profile/event reference resolution is tracked with B30.

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

## Production B10 deletion and forwarding extension

`docs/screens/message-moderation-and-forwarding.md` maps production
messageDeleteCapability, MessageBatchDeleteOperations, MessageDeleteDialog,
ForwardSelection/Picker, ForwardSession and ForwardOperationCoordinator into
`MessageBatchModels.kt`, `AppViewModel.kt`, `MessageBatchUi.kt` and the shared
conversation/Shared Content picker. Production reconnects outcome and transport
callbacks; prototype behavior remains deterministic, in memory and foreground-owned.

## Production B11 media acquisition and attachment extension

`docs/screens/composer-attachment-actions.md` maps production MessageOutboundShare,
MessageAttachmentSave, RecentMedia, MediaQuality/MediaPipeline,
AttachmentTransferCoordinator and ContactShare/ContactPreview into
`AttachmentModels.kt`, `PhotoMetadata.kt`, `DraftPhotoProcessor.kt`,
`AttachmentAcquisitionUi.kt`, `MessageAttachmentExport.kt` and
`AnimatedAttachmentImage.kt`. AppViewModel owns deterministic revisions and draft
quality; production must reconnect authoritative cache/transfer/media grants.
System Photo Picker, document creation, contact phone-row selection and Sharesheet
remain platform-owned. No production database, permission or transport is copied.

## Production B12 draft photo editor

`docs/screens/draft-photo-editor.md` maps pinned production PhotoEditorScreen,
PhotoEditorState, PhotoEditRecipe, PhotoEditTransform, PhotoEditorRenderer and
PhotoEditorCommitter into `PhotoEditorModels.kt`, `PhotoEditorUi.kt`,
`PhotoEditorRenderer.kt`, `AppViewModel.kt` and the existing draft media preview.
The prototype retains originals and recipes in memory, and commits exactly one
frame after revision/source/owner checks. B11 quality reprocessing replays those
recipes; send/forward strips original sources and edit metadata. Production
reconnects its draft source/staging/commit stores rather than copying in-memory
fixtures into persistence. App chrome stays monochrome; five ink colors describe
photo content, independently of the B26 appearance decision.

## Production B13 attachment reading and shared content

`docs/screens/text-attachments-and-shared-content.md` maps pinned production
TextAttachmentPreviewPolicy/ReaderState/Reader/Fallback and MediaFileAccess/MediaIo
into `AttachmentReadingModels.kt`, `AttachmentSources.kt` and
`AttachmentReaderUi.kt`. MediaLibrary maps to `SharedContentProjection`,
`SharedContentLibrary.kt`, the refreshed media viewer and the existing Media3
boundary. AppViewModel owns developer examples/one-shot outcomes; source IDs own
reader/player lifetime. Production reconnects authoritative attachment cache,
voice sources and distribution/installer outcomes. Real installation and new
permissions remain outside Q06 prototype coverage.

## Production B14 location sharing

`docs/screens/location-sharing.md` maps pinned LocationPickerScreen/LocationShare,
LocationBubble/BubbleContentBlocks and Controllers.send into
`LocationSharingModels.kt`, `LocationSharingUi.kt`, the shared composer and
AppViewModel's owned location session. Location messages preserve the production
text wire form; explicit map opening reuses Android ACTION_VIEW. The prototype
uses manual point entry and developer-only one-shot current-location examples;
production must reconnect grants/provider cancellation, map selection and send
acceptance. No in-memory scenario state should migrate into a location service.

## Production B15 Read Aloud transport and source navigation

`docs/screens/read-aloud-transport.md` maps pinned TtsPlaybackQueue/TtsChunker,
TtsHistorySession, TtsTransportBar, TtsDestinationNavigation, TtsSpeakFromHere and
TtsVisibleSentenceMapping into `SpeechPlayback.kt`, `ReadAloudController.kt`,
`ReadAloudUi.kt`, the NavHost and existing message/document/file readers. One
foreground engine consumes generation-owned local queue events. SourceText
retains authored offsets through Markdown, sentence splitting and native chunks;
range timing refines the current passage. Production reconnects its authoritative
history projection and playback controller. Profile/background boundaries cancel
the local queue and return ownership; B16 owns preferences/auto-read and Q06's
background integration boundary.

## Production B16 Read Aloud preferences and auto-read

`docs/screens/read-aloud-preferences.md` maps pinned TextToSpeechScreen,
TtsEngineResolver/Selection/Catalog, TtsVoice/Rate/MediaMix/AutoRead preferences,
TtsAudioFocusOwner and ConversationTtsEffects/AutoReadCursor into
`SpeechPreferences.kt`, `SpeechPlatform.kt`, `ReadAloudController.kt`,
`ReadAloudSettingsScreen.kt` and the existing conversation/settings routes.
The local cursor uses prototype chronology and captured unread IDs; production
reconnects authoritative history, settings and audio ownership. The
TtsPlaybackForegroundService/AppState lock-boundary contract is represented only
by `SpeechBackgroundExample` and `SpeechDeveloperDialog`. These example controls
must not be migrated as a real notification, service, clock or lock authority.

## Production B17 dictation and voice recording

`docs/screens/dictation-and-voice-recording.md` maps pinned
ConversationDictationController/Preferences/RecognitionService/Compatibility,
DictationSettingsScreen, VoiceRecordingController and VoiceRecorder into
`ComposerDictation.kt`, `ComposerCaptureController.kt`, `VoiceCapture.kt`, the
composer and Dictation settings. Profile/chat/request, captured preferences,
draft revision/selection/payload and membership remain the migration seams.
Production supplies the real recognizer/recorder and authoritative callbacks;
local fixed transcripts, elapsed ticks and failure choices are not services.
The prototype retains release-to-review plus explicit Send and uses a stricter
any-draft-change-to-review guard specified by B17. The seven-commit master drift
through `911040c7e1c31652638c8cfd72812d1f3a694b9b` changes no capture sources.

## Production B18 group setup, images and roster

`docs/screens/group-setup-images-and-roster.md` maps NewGroupSetupScreen/NewGroupCreate,
GroupEditScreen, GroupImageWorkflow, GroupEmojiImagePicker/Renderer and
GroupRosterMutationUi/LoadStatus into `GroupWork.kt`, `GroupWorkController.kt`,
`GroupWorkUi.kt`, `GroupEditorScreen.kt` and `GroupEmojiImage.kt`, wired through
existing creation/info/member routes. Production reconnects its authoritative
member roster/revision, group commit lock and actual image/retention callbacks.
Private group image and public invite preview are distinct local fields; preserve
the production legacy-image migration semantics when reconnecting real uploads.
Current master permits warm seeded picker presentation but still gates commit on
Ready. B19 covers the ChatListGroupSeed cold-frame terminal/unrecoverable checks.
The prototype retains staged Edit Group Save and existing initial timer presets.

## Production B19 administration, lifecycle and transcript

`docs/screens/group-administration-and-transcript.md` maps Controllers transfer,
self-demotion/leave, enable/disband/failure acknowledgment, GroupDisbandControls,
ComposerGate and ConversationTranscriptExport to `GroupLifecycle.kt`,
`GroupLifecycleController.kt`, `GroupLifecycleUi.kt`, `ConversationProjection.kt`,
`ConversationTranscript.kt`, `TranscriptController.kt` and `TranscriptUi.kt`.
The B18 group lock and authoritative roster remain shared. Production reconnects
accepted-stage ownership, management capability/blocker state and convergence.
The latest ChatListGroupSeed terminal/unrecoverable preservation is covered.
Transcript handoff follows local Q07 Files destination; its separately identified
local JSON schema does not fabricate production wire-event fields. Restore the
full production export schema and engine timeline reader during migration.


## Production B20 disappearing timers and expiry

`docs/screens/disappearing-timers-and-expiry.md` maps DisappearingDuration,
DisappearingMessages, GroupDetailsScreen timer confirmation, Controllers
updateMessageRetention/localExpiryRow/read deferral and DisappearingMessageSweep
to `DisappearingMessages.kt`, `RetentionController.kt`, `RetentionUi.kt` and
AppViewModel's send/forward/read/prune callbacks. Conversation/Shared Content and
existing attachment/speech ownership consume the canonical timeline after removal.
Reconnect authoritative saved expiry/duration, first-read anchors, optimistic-ID
transfer and engine sweep/refetch during migration. Preserve accepted mutation
when refresh fails and never infer expiry for unpinned history from current policy.
Q03 is resolved in WN-ANDROID-0140. No relevant retention drift exists through
master `911040c7e1c31652638c8cfd72812d1f3a694b9b`; broader ordering remains open.

## Production B21 incoming sharing and profile links

`docs/screens/incoming-sharing-and-profile-links.md` maps SharePayload,
ShareInboundStager, ShareStaging, DraftStore.mergeText, MainShell.stageShareToChats
and AppState.stageInboundShare to `IncomingSharing.kt`, `IncomingController.kt`,
`IncomingShareUi.kt`, AppViewModel and the navigation host. ShareRequest/ShareRouting,
ConversationShortcutOwnership and NotificationTarget provide request replacement,
original owner and deferred route contracts. Reconnect actual Intent/ClipData,
provider access, lock readiness, shortcut publication and accepted-stage callbacks
when migrating; Q06 excludes these external integrations here.
ProfileLink, NostrProfileReference and QrScanResult map to public-only
`ProfileLinks.kt`, ShareConnectScreen and shared recipient normalization. Canonical
links and QR provenance resolve actual identities; public NIP-19 examples verify
checksum/TLV handling. Current master changes none of these contracts.

## Production B22 notification controls

`docs/screens/notification-controls.md` maps NotificationsScreen and AppState's
local/push/background setters/provider gate to `NotificationController.kt`,
`NotificationControls.kt` and `PreferenceScreens.kt`. BackgroundConnectionPreferences
is app-wide; initiating-profile local enable and accepted service rejection remain
separate. Erase App Data resets local app-wide state. Real runtime supervision,
provider registration and permission results reconnect at this boundary.

NotificationChannelSpec, ConversationNotificationRouting, ConversationNotifyDialogs,
ConversationNotificationSettingsScreen and ConversationVibrationPreferences map to
`NotificationControlsUi.kt` and `NotificationCategoryAccess.kt`. Existing public
Android settings targets are used; missing category/child targets report fallback.
Real publication, override observation and vibration remain migration seams.
ChatMutePreferences/MuteOverrideReconciliation and AppState mute commands map to
independent NotifyFor and mute/deadline fields. `MuteDurationDialog.kt` uses native
calendar/time input; RetentionController's shared clock expires canonical mute state.
All/Mentions remain independent of effective Nothing while muted. Current master
changes none of these settings contracts.

## Production B23 notification routing and actions

`docs/screens/notification-routing-and-actions.md` maps NotificationTarget,
NotificationAction and NotificationReadThroughCommitter to `IncomingController`,
`NotificationActions`, typed conversation request/target arguments and
`NotificationReadBoundary`. History uses the existing single canonical local
conversation; optional message IDs and bounded invitation probes preserve source
routing distinctions. Request leases guard read-through after unread capture.

NotificationActionReceiver and Reply/Reaction/MarkRead workers map to
`NotificationActionController` and AppViewModel's originating-profile mutation/read
callbacks. Local generation-aware cleanup, accepted proof and lock/operation retry
accounting reconnect to production's presenter, durable stores and AppState
notification methods. `NotificationActionsUi` owns app status; Developer Tools
owns examples. Real exported intents, RemoteInput, delivery, workers, services and
process-death persistence remain outside Q06 local coverage. Current route test
drift preserves one mounted controller while moving to authoritative windows.

## Production Android B25 — key packages and developer diagnostics

[Selected brief](../screens/key-packages-and-developer-diagnostics.md) maps C080,
C110/C111 and C118/C119 to model/DeveloperParity.kt, DeveloperModels.kt,
state/DeveloperParityController.kt and ui/settings/DeveloperParityUi.kt.
DeveloperScreens retains the console and enriched push details; ConversationScreen
renders developer-only stream status outside message controls. Native source
seams are KeyPackagesScreen/AppState publication methods, DiagnosticsScreen,
PerformanceDiagnostics, DeveloperScreen, GroupDetailsScreen push inspection and
StreamDebug. The fixed local state is not a production engine or telemetry sink.

## Production Android B27 — download matrix, queue and quality

[Selected brief](../screens/downloads-and-media-quality.md) maps C105–C107 to
model/MediaDownloads.kt, AttachmentModels.kt, ProfileSettings.kt,
state/AppViewModel.kt and ui/settings/DataUsageScreen.kt. Production
MediaAutoDownloadMatrix maps to immutable local rules; AttachmentDownloadGate,
AttachmentDownloadPolicy and AppState stop/restart map to canonical attachment
origin/admission/suppression and exact profile/revision guards. The existing
AttachmentTransferHost owns foreground progress; Developer Tools owns network
and held-queue examples. Reconnect production connectivity and durable workers
at migration. MediaQuality maps to effective draft photo policy and captured
voice policy in ComposerModels/ConversationComposer; real encoding is outside
this prototype. B11 remains photo processing and metadata authority.

## Production Android B28 — relay publication and validation

[Selected brief](../screens/relay-publication-and-validation.md) maps C108/C109
to model/RelayPublication.kt, state/RelayPublicationController.kt,
ui/settings/RelayPublicationUi.kt and exact-profile relay mutations in
AppViewModel. Production AccountRelayListsFfi/MissingRelayListKindFfi maps to
Where I post/Where I receive projections; RelaysScreen reload/mutation maps to
owned Refresh/Publish work. Production managed-host validation becomes a named
compatibility issue without narrowing accepted general `wss://` support or
silently cleaning three-role imports. Reconnect Marmot I/O, DNS/SSRF checks,
event publication and durable state only in production.

## Production Android B29 — AI agents and streaming operations

[Selected brief](../screens/ai-agents-and-streaming-operations.md) maps C112/C113
to model/AgentOperations.kt, ui/settings/AiAgentsScreen.kt,
ui/conversation/AgentOperationUi.kt, ChatMessage, AppViewModel and the typed
navigation destination. Production AiAgentsScreen/AgentConnector maps to the
four public-key prompt definitions, manual contact guidance and user-initiated
browser handoff. Production AgentOperationPresentation/AgentOperationRow maps to
the immutable ordinary operation projection; StreamDebug maps only to the
existing developer-only record surface.

Reconnect real account records, stream updates and Marmot operation payloads at
the AppViewModel/model boundary during production migration. Preserve exact
profile/chat ownership and do not make ordinary operation visibility depend on
Developer Tools. The prototype examples are local records and do not define a
wire schema, install connectors, create accounts, start networking or persist.
