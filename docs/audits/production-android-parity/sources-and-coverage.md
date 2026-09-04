# Sources and coverage

## Reproducible baselines

| Source | Baseline | Working state | Drift check |
| --- | --- | --- | --- |
| Production Android | `https://github.com/marmot-protocol/whitenoise-android`, default `master`, `319454889f1c2494dec4a69b5577d98017f44eee` | Immutable GitHub archive; no production edits | Archive SHA-256 `76edef86a09485df0967af7d77aaea94a124fd3f1070cfb042175e148104e16e` plus commit |
| Prototype | `/Users/vladimirkrstic/Workspaces/wn-android-prototype`, `main`, `4c3f7366bcb738839f4969d762403adfc023b8a3` | Dirty working tree reconciled after concurrent local changes; generated audit files excluded | [Baseline manifest](baseline-manifest.json) records 360 tracked/untracked target-file hashes and reconciled porcelain status |

The production default ref was resolved with `git ls-remote --symref`; the commit archive was downloaded from GitHub without adding a remote. The [public repository page](https://github.com/marmot-protocol/whitenoise-android) identifies the Kotlin/Compose/Marmot architecture and `master` branch.

## Inspection method and denominator

The audit enumerated and mechanically indexed every production main-source Kotlin file (**615**), every production unit-test Kotlin file (**860 files**), every instrumentation-test Kotlin file (**27 files**) and **429** committed PNG baselines. A 10,697-line declaration/action/string index was generated from all production main Kotlin. High-level reachability was then traced from `AppPhase`, `WhiteNoiseApp`, `MainShell`, `SettingsDetail`, conversation/group screens, `AndroidManifest.xml`, environment/distribution variants and their state/controller handlers. The prototype comparison indexed all **76** main Kotlin, **29** unit-test and **25** instrumentation-test files, then checked the typed routes, screen briefs and parity ledger.

The matrix denominator is capability-level rather than file-level: 122 reachable user outcomes, including covered rows and explicit exclusions. Test *files* and screenshot assets are inventory evidence; this audit did not execute them or claim each contains a runnable test.

## Product-area coverage

| Area | Production entry evidence | Reachability/gates | Result |
| --- | --- | --- | --- |
| Startup/onboarding/accounts | [OnboardingScreen.kt:109](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/onboarding/OnboardingScreen.kt#L109) (`OnboardingScreen`); [AppPhase.kt:3](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/state/AppPhase.kt#L3) | Signed out, retained account, Amber installed, network/recovery | F01 |
| Chats, folders and global search | [ChatsScreen.kt:133](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/chats/ChatsScreen.kt#L133); [ChatFoldersScreen.kt:69](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/ChatFoldersScreen.kt#L69); [GlobalSearchState.kt:20](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/chats/GlobalSearchState.kt#L20) | Ready phase, active account, folder/profile scope | F03–F04 |
| Conversation/messages/media | [ConversationScreen.kt:205](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/ConversationScreen.kt#L205); [MessageActions.kt:119](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/messages/MessageActions.kt#L119); [MediaViewer.kt:96](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/media/MediaViewer.kt#L96) | Membership, authorship, role, attachment state | F05–F08 |
| Groups/retention | [GroupDetailsScreen.kt:159](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/group/GroupDetailsScreen.kt#L159); [GroupDisbandControls.kt:37](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/group/GroupDisbandControls.kt#L37) | DM/group, roster verified, admin, lifecycle components | F09–F10 |
| External entry | [AndroidManifest.xml:1](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/AndroidManifest.xml#L1); [MainShell.kt:1311](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/navigation/MainShell.kt#L1311) (`stageShareToChats`) | Share/deep link/shortcut/notification, lock and profile owner | F11–F12 |
| Settings/privacy/data | [SettingsScreen.kt:132](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/SettingsScreen.kt#L132) (`settingsHomeState`); [DevicePrivacyScreen.kt:50](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/DevicePrivacyScreen.kt#L50) | active account, credential, service/build availability | F12–F16 |
| Agents/rich events | [AiAgentsScreen.kt:64](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/AiAgentsScreen.kt#L64); [NostrEventCards.kt:48](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/nostr/NostrEventCards.kt#L48) | Settings plus message content types/stream states | F17 |
| Help/diagnostics/updates | [HelpAboutScreens.kt:49](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/HelpAboutScreens.kt#L49); [AppSelfUpdateDialog.kt:22](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/AppSelfUpdateDialog.kt#L22) | developer unlock; Zapstore only for self-update | F18 |
| Variants | [build.gradle.kts:42](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/build.gradle.kts#L42) | dev/preview/production/staging × Zapstore/Play; unsupported build-type combinations disabled | Cited per row |

Production uses minimum SDK 30 and target SDK 36; the prototype uses minimum SDK 23 and target SDK 37. Any future platform implementation must keep API 23 fallbacks or explicitly change the prototype baseline. The audit does not assume a production API is directly available to the prototype.

## Existing test evidence sampled

Production contains focused tests and Roborazzi baselines for identity input/profile links, folders/rules, global filters, message edits, photo editing, disappearing messages, app lock, notification scope/actions, incoming sharing, TTS/dictation and many adaptive/loading/failure visuals. Matrix source links remain primary because tests were not run. The prototype's existing unit/Compose suites cover its current routes, state transitions and accessibility contracts; the implementation plan requires extending those tests rather than copying production test architecture.

## Coverage limits

The production source archive was fully accessible. Runtime-generated Marmot bindings, SQLite content, Firebase/push servers, relays, installed TTS/speech services, external signer, location/map services, app stores and device/OEM behavior were not executed. Their Android call sites and user-visible handling are included, while protocol correctness remains unverified by this audit. The archive contained no live build credentials and none were requested. Production issues/Project 7 were not used to expand scope because the user asked for current `master` behavior; unfinished issue proposals are not shipped capability evidence.

No emulator/device or rendered-screen inspection was performed. Therefore hierarchy recommendations inherit the prototype's accepted documentation and code, and visual acceptance remains pending.

## Prototype-only reverse map

| Capability to preserve | Reason |
| --- | --- |
| First-login privacy choices | Profile-owned analytics/logging prompt and dismissed state remain; production has settings but no equivalent first-login consent flow. |
| Notification preview privacy | Sender/message, sender-only and generic preview choices remain; production source exposes category settings instead. |
| Full deterministic story catalog | All direct/group/support/invite/ended/recovery fixtures remain for design and regression state coverage. |
| Profile removal and whole-app erase | Inactive-profile removal and exact-confirmation Erase App Data remain alongside production-style active-account wipe. |
| Left chat scope | The prototype Left scope remains visible even when production organization moves other scopes into folders. |
| Avatar web catalog | Bundled search/URL choices remain deterministic; production remote image search is not introduced. |
| Voice/Text/Both voice formats | Prototype recording/transcription formats remain alongside distinct production-style dictation. |
| Configurable quick reactions | Prototype quick-reaction replacement remains even if production derives recents differently. |
| Three relay roles and broad secure URLs | Profile, Inbox and Chat Messages roles remain until Q04 resolves the production managed-only two-list model. |
| Notification previews, media save and broad language set | Existing accepted settings, Serbian language, CreateDocument save flow and dirty-tree video/gallery work remain unless an explicit decision changes them. |
| Blocking and follow state | Prototype blocking, follow/unfollow and related recovery stay even where production source emphasizes follow/private notes. |
| Diagnostics & Improvements prompt and sanitized exports | Existing profile-owned consent and sanitized diagnostic copy remain distinct from production forensic audit logs. |


## Official Android constraints consulted

- [Receiving shared content](https://developer.android.com/develop/ui/compose/sharing/receive): declare only supported MIME types, validate untrusted payloads, and let the person confirm/edit before use.
- [Notification channels](https://developer.android.com/develop/ui/compose/notifications/channels): Android owns channel behavior after creation; app UI must show effective versus requested state honestly.
- [Navigation](https://developer.android.com/guide/navigation): external entry and Back behavior need explicit destination ownership and restored state.
- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech): engine lifecycle and availability are device-owned boundaries.
- [Permission best practices](https://developer.android.com/training/permissions/usage-notes): request sensitive access only after the relevant user action and preserve capability when denial allows it.

## Implementation drift check — B17, 2026-09-04

Production `master` resolved to
[`911040c7e1c31652638c8cfd72812d1f3a694b9b`](https://github.com/marmot-protocol/whitenoise-android/commit/911040c7e1c31652638c8cfd72812d1f3a694b9b);
there is no `main` ref. The [seven-commit comparison](https://github.com/marmot-protocol/whitenoise-android/compare/319454889f1c2494dec4a69b5577d98017f44eee...911040c7e1c31652638c8cfd72812d1f3a694b9b)
contains 94 changed files and no dictation/voice-recording source or test change.
AppState changes concern startup retry, account snapshots, chat-list publication
and attachment installers; Controllers changes concern timeline/roster behavior.
B17 retains its pinned capture contracts. Roster/admin availability, Nostr event
reading, installer handoffs, Amber grants, contact-follow presentation and startup
must be checked in the relevant later batches/final reconciliation. This scoped
check does not replace the immutable baseline or claim the other drift is done.
