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
| Agents/rich events | [AiAgentsScreen.kt:64](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/AiAgentsScreen.kt#L64); [NostrEventCards.kt:48](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/conversation/nostr/NostrEventCards.kt#L48) | B29 setup/streaming and B30 in-app identities, typed cards, readers and local video are host-verified; no relay/network work | F17 |
| Help/diagnostics/updates | [HelpAboutScreens.kt:49](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/HelpAboutScreens.kt#L49); [AppSelfUpdateDialog.kt:22](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/AppSelfUpdateDialog.kt#L22) | B25 diagnostics, B31 Help/About and B32 distribution/dismissal/review state are host-verified; real self-update remains outside Q06 | F18 |
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

## Implementation drift check — B18, 2026-09-04

A fresh `ls-remote` still resolves master to
`911040c7e1c31652638c8cfd72812d1f3a694b9b` with no main ref. B18 reconciles
GroupDetailsScreen and GroupRosterLoadStatus: known membership/admin permits
opening the picker while Loading, and confirmation still requires Ready.
The Controllers diff changes no member-roster/commit method. The extracted
ChatListGroupSeed now preserves unrecoverable alongside disbanding/disbanded on
cold/profile-switch frames; B19 must reconcile that C078 behavior. Its admin
seed remains conservative. The larger authoritative timeline-window ordering,
Nostr, installation, Amber/contact/startup changes remain for the relevant later
batch or final audit, not implicitly completed by B18.

## Implementation drift check — B19, 2026-09-04

The current master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`, with no main
ref. B19 explicitly reconciles
[ChatListGroupSeed.kt](https://github.com/marmot-protocol/whitenoise-android/blob/911040c7e1c31652638c8cfd72812d1f3a694b9b/app/src/main/java/dev/ipf/whitenoise/android/state/ChatListGroupSeed.kt):
unrecoverable, disbanding/disbanded and request state survive the first frame
before the full record arrives; absent admin evidence remains conservative.
Local Chat stores lifecycle independently and gates composer and administration
before membership presentation, including profile round trips. B19 keeps the
pinned transfer/disband/transcript contracts and the B18 warm roster behavior.
The full engine export schema is a documented migration seam, not fabricated
local state. This does not complete unrelated timeline-window ordering,
Nostr-event, installer, Amber/contact or startup drift.


## Implementation drift check — B20, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`, without main.
The seven-commit diff changes no duration, retention picker, expiry indicator or
retention/read-anchor test. Controllers changes around retention mentions concern
timeline structures and seed extraction, not timer rules. The immutable baseline's
GroupDetailsScreen confirmation, updateMessageRetention accepted-before-refresh
ordering, DisappearingMessageSweep and read-anchor tests resolve Q03 as distinct
policy-pruning and row-deadline operations. B20 implements those owned local states;
its [brief](../../screens/disappearing-timers-and-expiry.md#implementation-evidence)
records 655 passing unit tests and nine compiled UI cases. This does not finish
broader authoritative timeline-window, Nostr, installer, Amber/contact/startup drift.

## Implementation drift check — B21, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`, without main.
The seven-commit diff changes no SharePayload/ShareStaging, ProfileLink, QR or
shortcut file. AppState's patch leaves stageInboundShare unchanged; it changes
bootstrap/account snapshots, timeline metadata and installer handoffs. MainShell
only adds the installer handoff import/effect. B21 therefore uses the pinned
staging/merge, target ownership and accepted-open contracts. The
[brief](../../screens/incoming-sharing-and-profile-links.md#implementation-evidence)
records 693 passing unit tests and nine compiled UI cases, with explicit Q06
external integration seams. Broader timeline-window, Nostr, installer and
Amber/contact/startup drift still awaits the relevant batch or final audit.

## Implementation drift check — B22, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`, without main.
The seven-commit diff changes notification startup/route test coverage, not
NotificationsScreen, category routing, vibration, mute preferences or their settings
contracts. AppState's settings setters are unchanged. The pinned getter and
updateBackgroundConnectionPreference explicitly establish app-wide ownership;
local enabling remains account-owned and accepted before a rejected service start.
NotifyForDialog stores All/Mentions separately from the authoritative mute state.
B22 preserves these distinctions, fixed-clock expiry and scoped settings recovery.
The [brief](../../screens/notification-controls.md#implementation-evidence) records
714 passing unit tests and ten compiled UI/platform cases. Broader timeline-window,
notification startup, Nostr, installer and Amber/contact drift remains for its
relevant batch or final reconciliation.

## Implementation drift check — B23, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`; no main.
NotificationRouteTimelinePresentationTest now consumes authoritative subscription
windows and explicitly preserves the mounted conversation through reconnect.
The local route continues using canonical history and stable message/request IDs;
no separate notification timeline is introduced. NotificationTarget preserves
message routes without a message ID for role-change events. Reply/Reaction/
MarkRead workers distinguish accepted mutation from cleanup, exact read targets,
24-hour lock deferral, three operation failures and generation-aware dismissal.
The [B23 brief](../../screens/notification-routing-and-actions.md#implementation-evidence)
records these local mappings and 748 passing unit tests/seven compiled UI cases.
Real subscriptions, encrypted/durable worker recovery and system notification
presentation remain explicit production seams. Broader startup/window, Nostr,
installer and Amber/contact drift remains for later/final reconciliation.

## Implementation drift check — B24, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`, without main.
The seven-commit diff changes no AppLockSettings/AppLockScreen, DevicePrivacyScreen,
IncognitoKeyboard, WindowSecureFlag or audit-log helper contract. AppState adds
an installer handoff guard requiring foreground/unlocked state; it remains a later
installer reconciliation seam. Existing return rules use time away, a protected
unknown-baseline evaluation and exact authentication-request ownership.

The [working brief](../../screens/app-lock-and-sensitive-privacy.md#implementation-evidence)
records C095–C098 host evidence: 870 passing unit tests, zero lint errors,
both APK builds and ten compiled UI cases. Q02 is resolved with independent
paused-Recents and active-chat capture controls. No device, biometric prompt,
IME/provider execution, real audit recording or process-death recovery is claimed.

## Implementation drift check — B25, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`; no main.
The seven-commit comparison changes none of KeyPackagesScreen, DiagnosticsScreen,
DeveloperScreen, GroupInfoScreen, StreamDebug or PerformanceDiagnostics. AppState
changes none of its B25 package/developer methods. GroupDetailsScreen changes
warm roster presentation and timing, already reconciled in B18, not push details.
The pinned publication/delete/provenance, health, stream gating, push inspection
and timed performance contracts therefore remain the B25 authority.
[The brief](../../screens/key-packages-and-developer-diagnostics.md#implementation-evidence)
records host evidence. Broader startup/windows, Nostr, installer and Amber/contact
source drift still awaits its relevant batch/final reconciliation.

## Implementation drift check — B26, 2026-09-04

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`; no main.
The seven-commit comparison changes no AppearanceScreen, FontSizeScreen, AppFont,
AppFontScale, AppLanguage, EnterKeyBehavior, ComposerPills, Theme or Type contract.
AppState's appearance/font/input setters are absent from the diff. Pinned source
therefore governs the optional AMOLED mode, app font/scale, language choices and
Shift+Enter/IME behavior. Production's default Manrope and warm AMOLED colors
are translated to the already-approved System typeface and monochrome identity.
[The completed brief](../../screens/appearance-typography-and-input.md#implementation-evidence)
records 892 tests after B32 integration, ten compiled UI cases, four complete
1,762-resource locale catalogs and the Q01/Q09 decisions. Pinned font assets,
color/ownership policy and locale key/token/plural coverage are host-verified.
Device rendering, system keyboard behavior and visual acceptance are not
inferred from compilation.

## Implementation drift check — B27, 2026-09-04

`git ls-remote` still resolves master to `911040c7e1c31652638c8cfd72812d1f3a694b9b`
and no main branch. Comparing the pinned audit to that head leaves
MediaAutoDownloadMatrix, MediaQuality, AutoDownloadScreen, AttachmentDownloadGate
and AttachmentDownloadPolicy unchanged. AppState's patch does not change these
settings or stop/restart methods. IntentStore gains installer handoff ownership;
Worker gains strict protocol ID validation and durable work observation. Those
production persistence/installer seams do not change C105–C107's prototype
contract. [Implementation and verification](../../screens/downloads-and-media-quality.md#implementation-evidence).

## Implementation drift check — B28, 2026-09-04

Production `master` remains `911040c7e1c31652638c8cfd72812d1f3a694b9b` with
no main branch. RelaysScreen, account relay-list mutation/validation, Marmot
relay-list projection and RelayUrlsTest are unchanged from the pinned audit.
AppState drift does not touch their contract. [B28 evidence](../../screens/relay-publication-and-validation.md#implementation-evidence).
