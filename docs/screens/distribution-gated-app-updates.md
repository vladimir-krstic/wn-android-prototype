# Distribution-gated app updates

## Purpose

Help people on a self-managed White Noise distribution understand when a newer
release is available and review a trusted update through every step before
Android installation. Let store-managed distributions keep update ownership
without an off-store prompt.

## Scope and non-goals

B32 covers C120 and C121 with app-wide deterministic update state: unknown,
checking, current, failed, available and important availability; per-version
banner dismissal; resolving, confirmation, bounded download progress, separate
verification, ready, install-permission, handoff failure, retry and cancel.

The prototype does not query Zapstore or relays, download or store an APK,
verify a real digest/signature/event, request install-package permission, open
unknown-source settings, invoke the package installer, persist update state,
schedule workers or post update notifications. Q06 retains those production
integrations. The state model names their eventual seams without claiming the
operations ran.

## Production evidence

The pinned production baseline is
[`319454889f1c2494dec4a69b5577d98017f44eee`](https://github.com/marmot-protocol/whitenoise-android/commit/319454889f1c2494dec4a69b5577d98017f44eee).

- [`AppUpdateModels.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/updates/AppUpdateModels.kt)
  defines availability, release counts and the three-release important bound.
- [`ChatsScreen.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/chats/ChatsScreen.kt)
  defines normal versus persistent update-banner behavior.
- [`SettingsScreen.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/SettingsScreen.kt)
  exposes installed/latest/check status only when self-update is enabled.
- [`AppSelfUpdateModels.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/updates/AppSelfUpdateModels.kt),
  [`AppSelfUpdateDialog.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/AppSelfUpdateDialog.kt)
  and
  [`ZapstoreAppSelfUpdateFlow.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/zapstore/java/dev/ipf/whitenoise/android/updates/ZapstoreAppSelfUpdateFlow.kt)
  separate resolution, download, verification, permission and installer handoff.
- [`PlayAppSelfUpdateFlow.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/play/java/dev/ipf/whitenoise/android/updates/PlayAppSelfUpdateFlow.kt)
  is the no-op store-managed boundary.

## Parity contract

- Update state belongs to the application, survives profile switching in the
  current process and never mutates a profile or chat.
- A self-managed fixture exposes **App updates** in Settings. Unknown, checking,
  failed and current states tap to check/retry. Available state starts the
  update review.
- Chats shows an update banner only for a newer release. One- and two-release
  warnings can be dismissed for the current latest version. A warning at three
  or more releases behind has no Dismiss action.
- Discovering a different newer version clears the prior per-version dismissal.
- A store-managed fixture has no Settings update section, Chats banner or
  self-update transition.
- Update review moves through Resolving → Confirming → Downloading → Verifying
  → Ready. Download completion never implies verification.
- Download progress is bounded and retains the expected total. Cancel from any
  step returns to Idle and rejects stale completion by generation.
- Resolve, download, verification and installer-handoff failures are distinct.
  Retry starts a new resolution generation.
- A permission-required outcome occurs only after verification and returns to
  Ready after the deterministic permission review.
- **Install** consumes only a Ready state. In B32 it models the handoff outcome;
  Q06 retains the real Android installer and permission flow.

## Entry, navigation, Back, and exit

Self-managed Settings includes **App updates** between preferences and Help.
Chats shows the warning as the first content card above connection state. Both
**Update now** and an available Settings row open the same app-owned modal flow.

Dialog Back, outside dismissal and Cancel stop the owned generation and return
to the same Chats or Settings destination. Navigation between those destinations
keeps the app-wide flow visible. Store-managed mode exposes no entry.

## Exact product copy

Ordinary resource-backed copy includes **App updates**, **Zapstore release**,
**Checking for updates…**, **Up to date**, **Update available**, **Important
update available**, **Version %1$s is available on Zapstore.**, **%1$d releases
behind**, **Update now**, **Dismiss update**, **Checking release**, **Download
update?**, **Download**, **Downloading update**, **Cancel download**, **Verifying
update**, **Ready to install**, **Install**, **Allow installs from this app**,
**Open settings**, **Update failed**, **Retry** and **Cancel**.

Failure copy distinguishes an unresolved signed release, a failed download with
partial cleanup, a failed verification that blocks installation and an
unavailable Android installer handoff.

## Android composition

The Chats warning is a neutral elevated Material card inside the existing lazy
content order. It uses visible status text, one primary **Update now** action
and an icon-only close action with the explicit **Dismiss update** name only
when the release can be dismissed.

Settings uses the established section/group/link hierarchy and package-owned
installed version. Modal Material alerts own confirmation, cancellation and
errors. Resolving and verifying use indeterminate progress; download uses a
determinate Material progress indicator. The UI uses the existing monochrome
semantic roles and shared 4/8 dp rhythm.

## Behavior and state

`AppUpdates` is the pure policy/reducer. It owns distribution eligibility,
numeric version comparison, the three-release important bound, per-version
dismissal, exact generation checks and legal state transitions.
`AppUpdateController`, owned once by `AppViewModel`, exposes Compose-observable
application state. `AppUpdateHost` advances only the currently mounted
generation. Stale completion, wrong-phase actions and store-managed actions are
no-ops.

Developer Tools selects distribution, check result and self-update outcome.
These controls are technical fixtures; ordinary surfaces expose only product
state and actions.

## System integrations

None in B32. The manifest adds no network, storage, install-package, service,
receiver, provider or notification capability. Production migration reconnects
release resolution, bounded download/storage, cryptographic verification,
unknown-source settings and package installation only after Q06 approval.

## Accessibility and adaptation

- Banner status, release count and actions are visible text; semantics also
  name normal versus important state.
- The decorative download icon has no duplicate content description.
- Every close, retry, cancel, settings, download and install action is a named
  Material control with native focus/ripple/keyboard behavior.
- Card content and dialogs wrap in logical order at 200% text; settings and
  Chats remain scrollable.
- State is never conveyed by color or progress alone. Progress dialogs expose
  a textual state description.
- Start/end alignment and resource-backed copy remain compatible with RTL and
  localization.

## Governing Android sources

- [Navigation](https://developer.android.com/guide/navigation/) governs dialog
  Back and destination ownership.
- [Progress indicators](https://developer.android.com/develop/ui/compose/components/progress)
  governs determinate download and indeterminate resolution/verification.
- [Compose accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
  governs named states and actions.
- [Package installation](https://developer.android.com/reference/android/content/Intent#ACTION_INSTALL_PACKAGE)
  identifies the future platform boundary; B32 does not invoke it.

## iOS parity evidence

- Existing Android prototype Chats and Settings remain the presentation
  authority; this distribution capability comes from the pinned production
  Android source above.
- `docs/screens/chats-and-chat-creation.md`
- `docs/screens/settings-and-profile-services.md`

## Approved differences and custom exceptions

The prototype models every app-owned state and handoff result without network,
files, cryptographic claims, install permission or installer execution. This is
the Q06 boundary already authorized for deterministic implementations.

## Observable acceptance criteria

- Self-managed Settings renders installed/current/check/available/failure facts.
- Store-managed mode renders no update row or banner and starts no flow.
- Normal banner dismissal applies only to the displayed latest version.
- An important three-release warning has no dismiss action.
- Download confirmation precedes progress; verification is a separately named
  state after complete progress.
- Verification failure blocks Ready and Install.
- Permission review appears only after verification.
- Retry creates a fresh generation; Cancel and stale completions cannot resume
  discarded work.
- No update action performs network, disk, permission, notification or installer
  work in this prototype.

## Implementation evidence

B32 implementation uses `AppUpdates.kt`, one app-wide `AppUpdateController`,
`AppUpdateUi.kt`, Chats/Settings integration and developer-only outcome controls.
`AppUpdatesTest` adds eight distribution, dismissal, ownership, phase, failure,
permission and installer-handoff regressions. `AppUpdateInteractionTest` adds
eight compiled banner, Settings, progress, verification, permission, install
and large-type cases.

The complete host gate passes 837 unit tests with zero failures/errors/skips,
compiles all eight new Compose cases, reports zero lint errors with 15 retained
warnings and two hints, and builds the debug app and instrumentation APKs.
Device execution, screenshots, external platform surfaces and visual acceptance
were not requested and are not claimed.
