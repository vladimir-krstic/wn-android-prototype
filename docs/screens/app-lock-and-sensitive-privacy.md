# App lock and sensitive privacy — B24

## Purpose and scope

C095–C098 complete app-owned return protection, keyboard learning requests and
sensitive audit-log control while preserving the existing Privacy & Security UI.
Use deterministic in-memory authentication and audit outcomes. No biometric
prompt, real logging engine, durable recording, telemetry, network, service or new
permission. The existing explicit Android document-creation contract serves user-requested
ZIP exports; the system owns destination selection. Device/emulator and visual execution remain outside this task.

## Production and local evidence

Production baseline `319454889f1c2494dec4a69b5577d98017f44eee`: AppLockScreen,
AppLockSettings and AppState's foreground/background/authentication methods;
IncognitoKeyboard; DevicePrivacyScreen; WindowSecureFlag; audit settings,
prepareAuditLogsForSharing and deleteAuditLogs. Fresh master is unchanged at
`911040c7e1c31652638c8cfd72812d1f3a694b9b` on 2026-09-04 (no main). Inspect
relevant drift before sign-off. The current diff changes none of these privacy,
lock, IME or audit contracts; AppState only adds an installer handoff guard that
requires foreground/unlocked state, retained as a later installer seam.

[Privacy & Security](privacy-and-security.md), WN-ANDROID-0067, the existing
sanitized [developer flows](developer-and-destructive-flows.md),
[UI metrics](../ui-metrics.md) and [source map](../port/source-map.md) govern local
presentation. Retain all current auto-lock choices; add production's 15-minute
choice without removing 30 minutes. Profile preferences remain profile-owned.

## App-lock contract

An enabled profile with an available device credential must not reveal app data
while the return decision is unknown, locked or authenticating. Time away starts
when an unlocked app backgrounds; time spent actively using the app does not
expire grace. Repeated background events while locked cannot restart grace.
Immediate/1/5/15/30-minute boundaries are exact and use injectable elapsed time.
A changed profile, prompt generation, sign-out, settings change or lifecycle
cancellation invalidates stale authentication results. Lost device credentials
retain the saved preference but expose its unavailable/recovery state.

Use an opaque app-owned lock surface with “White Noise is locked”, “Unlock”,
“Unlocking…”, cancellation, unsuccessful/locked-out/unavailable feedback and retry.
Do not imitate Android's biometric dialog. A local outcome completes only its
owned prompt. Back cancels an in-progress attempt; from locked state it can leave
the app but cannot expose the protected destination. Preserve the mounted navigation tree and UI state across the lock boundary.
Protected content is neither placed nor exposed in semantics, and its lifecycle
is capped at CREATED. Clear focus and suppress app input sessions while hidden;
existing lifecycle-aware media/foreground work receives pause/stop.
Incoming taps and notification actions reuse this gate. Resume pending owned work
after unlock; never activate another profile using a stale unlock result.

## Keyboard privacy

Add “Incognito keyboard” with copy requesting that the keyboard avoid learning
from typed input and explaining that the keyboard may ignore the request.
Install one stable Compose platform-text-input interceptor around all app-owned
input, including composer, search, settings, keys and modal fields. OR the Android
no-personalized-learning flag without changing existing IME actions. API 26+
supports the hint; show unavailable support on API 23–25. Do not remount navigation
when the preference changes or alter system-owned pickers/inputs.

## Audit-log contract

Q08 permits developer-only sensitive-log states while ordinary-settings placement
is unresolved. Keep Diagnostics & Improvements and sanitized exports intact.
Add a distinct Developer Tools “Audit logs” destination with explicit sensitive
recording consent, enabled/disabled/pending/failure/retry and retained file state.
Production recording is app-wide across sessions; represent that scope plainly.
Turning recording off retains files. Export requires separate confirmation naming
message content, identities and device details; cancellation prepares nothing.
Preparation, empty inventory, unavailable/share failure and retry retain truthful
outcomes and original request ownership. Never reuse sanitized-export reassurance.
Delete confirms destructive scope, covers partial failure/retry, clears prepared
copies and preserves the recording preference; enabled recording may continue in
a fresh file. Sensitive content/path details stay out of error messages and logs.
Lock, sign-out, profile/navigation changes and app erase invalidate pending exports.

## Screenshot and Recents privacy — resolved Q02

The user selected separate controls. **Hide Screen in Recents** asserts
`FLAG_SECURE` synchronously when the app pauses so Android cannot capture a task
preview, then releases it after resume when no stronger protection applies.
**Block screenshots in chats** applies the same platform protection while a
conversation or its chat-info, content, membership, relay, notification,
message-detail or developer-detail route is visible. App-lock evaluation,
background shielding and authentication always override both preferences.

The copy names screen recording because Android's secure-window contract blocks
recording and casting alongside screenshots. Defaults remain off. The controls
are profile-owned and independent; enabling Recents privacy does not block an
active screenshot, and enabling chat capture protection does not hide Settings
or the Chats list.
## Android composition, adaptation and sources

Reuse the current Settings scaffolds/groups/rows and native Material dialogs,
progress/error semantics, selectable rows and shared spacing. Lock content contains
no identity/chat data, scrolls in short/large-text layouts, handles safe/IME insets
and follows appearance. All actions retain accessible names, semantic disabled
states, keyboard focus, RTL and standard touch targets.

- [Biometric authentication](https://developer.android.com/identity/sign-in/biometric-auth): Android owns the eventual credential prompt; this batch implements only app-owned lifecycle/results.
- [EditorInfo](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING): API 26 hint, preserving existing flags and truthful limitations.
- [PlatformTextInputInterceptor](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/PlatformTextInputInterceptor): stable whole-subtree interception and session cancellation.

## Acceptance and verification

Host tests must exercise no-data visibility, background grace, retry/cancel,
credential loss, cold evaluation, owner/generation changes and incoming/action
gating; IME flag composition and broad input scope; audit consent, global recording,
retained files, export/partial delete recovery and stale lifecycle callbacks.
Compose cases compile only. Run compilation, unit tests, lint and both APK builds
before recording implementation evidence and the B24 commit. Device/visual and
real platform authentication/logging remain separate acceptance/integration seams.

## Implementation evidence

Implemented and host-verified on 2026-09-04. C095–C098 are covered and B24 is
ready for its batch commit.

`AppLockController` owns credential availability, evaluation, foreground/grace,
prompt generations and outcomes. `AppLockScope` starts deterministic prompts and
retains a single navigation tree through `ProtectedAppContent`, which excludes
protected placement/semantics and suspends its lifecycle. The early paused-window
flag cannot be cleared by intervening recomposition. Input interception suppresses
keyboard sessions while hidden, and AppViewModel rejects hidden visible-message
read acknowledgments. B21/B23 owned incoming/action work shares the gate; a
notification that selects a protected profile waits for that profile's unlock.

`IncognitoKeyboardScope` intercepts all descendant app text sessions at a stable
composition site. It preserves field IME actions/privacy bits and applies the
no-learning request only on API 26+. The pure API-level guard is covered below;
the inlined Android constant has a documented lint suppression because that guard
uses the caller-supplied SDK value. No third-party keyboard guarantee is made.

`WindowPrivacyPolicy` keeps the two profile controls truthful and independent.
Recents privacy is applied at pause before Android takes the task snapshot. Chat
capture protection follows the typed route family for conversations and their
detail surfaces. Lock/evaluation protection is always stronger. Unit coverage
proves the independent conditions and route scope; the Compose case proves that
toggling one setting does not mutate the other.

`AuditLogController` owns a separate app-wide recording flag and per-profile local
session samples. Enabling requires sensitive consent; disabling retains files.
Exports separately confirm sensitive content, package safe JSONL names in a stable
ZIP and use Android document creation. Request plus retry-attempt leases reject
stale destinations/writer callbacks. Failed/cancelled/stale writes clean up the
created destination; interrupted writing offers retry. Delete has partial recovery,
preserves recording, and starts fresh local files when needed. Profile wipe removes
only that profile's audit files; app erase clears all state. No real recording
engine, telemetry or durable audit store is introduced. Sanitized Diagnostic Logs
remain unchanged.

Host evidence: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`;
870 passing unit tests, no failures/errors/skips, zero lint errors (15 warnings and
two hints), and both APKs. `AppLockStateTest`, `AuditLogStateTest`,
`IncognitoKeyboardTest` and `WindowPrivacyPolicyTest` add 33 cases. Ten
`AppLockPrivacyInteractionTest` cases compile/package only, covering lock
cancel/retry/Back, retained protected input/lifecycle, keyboard limitation copy,
independent privacy controls and sensitive recording/export/partial-deletion
semantics. No device, emulator, IME, biometric prompt, document provider or visual
execution is claimed.

## Migration seams

Replace local prompt outcomes with owned BiometricPrompt/device-credential
callbacks, retaining lifecycle cancellation and exact request identity. Map local
monotonic time and first-frame evaluation to AppLockPreferences' persisted
last-background/unlock baseline without exposing data while it loads. Keep the
single mounted conversation and suspend protected interaction/media on lock.

The Compose IME interceptor already calls the public platform-input API; third-
party keyboard behavior and actual protected window/Recents behavior require
current device acceptance. The resolved C097 policy and pure route/rule coverage
remain the source contract until that inspection.

Replace local audit samples with engine recording settings and safely scoped file
enumeration/staging/deletion. Retain app-wide recording semantics, per-request
consent, partial deletion and retry-attempt ownership. Production may share its
staged files through Android Sharesheet; this prototype retains the approved
explicit document destination. Real filesystem logging, encrypted stores and
process-death export recovery remain outside prototype coverage.


## Integration with committed B25 and B27–B32

B25 and B27–B32 are committed and integrated into main. The complete combined
gate passes 870 unit tests with zero failures/errors/skips, zero lint errors (15
warnings and two hints) and both APKs. Ten B24 Compose cases compile/package only.
This validates shared navigation, developer entry, state-holder and window-policy
integration; it does not claim device/visual acceptance.
