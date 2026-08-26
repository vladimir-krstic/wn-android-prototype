# Diagnostics and first-login privacy choices

Status: User-approved flow implemented on 2026-08-26. Static gate recorded in
the parity ledger; device execution and visual acceptance remain pending.

## Purpose

Offer each profile optional analytics and diagnostic logging after successful
onboarding, with a permanent place to review or change those choices.

## Scope and non-goals

Initial Sign In, Sign Up, and Add Profile completion; one privacy sheet;
Privacy & Security → Diagnostics & Improvements; and moving consent out of
Developer Tools. All state and records are deterministic and in memory. No
analytics SDK, collection, upload, network, permission, durable storage, or
dependency upgrade is added. Production wording describes product intent,
not an implemented transport in this prototype.

## Parity contract

- Both independent switches start off. Changes apply immediately.
- Successful onboarding schedules only the activated profile if unseen.
- Present only when the Chats navigation entry reaches `RESUMED`, after its
  transition completes. Merely visiting or switching profiles does not prompt.
- Close, system Back, scrim, or downward dismissal marks that captured profile
  seen. Recomposition, stop/resume, and rotation do not count as dismissal.
- Stored/re-onboarded profiles retain preferences and prompt-seen state. A
  newly created or wiped profile starts fresh. The other profiles are unchanged.
- Enabling logging seeds two deterministic records once, totaling 32,000
  bytes. Disabling logging retains records. Confirmed clearing zeros their
  contents without changing either preference or recreating them on re-enable.
- Developer Tools never owns or gates consent. Turning its master off still
  disables debug mode but preserves consent, records, key packages, and events.

## Entry, navigation, Back, and exit

`AppRoute.SignedIn` hosts `DiagnosticsPromptHost` and observes the navigation
entry lifecycle. The sheet has a native handle and Close action, no Save or
Continue. All normal dismissal routes call `dismissDiagnosticsPrompt(id)`;
there is no disposal-based mutation. Privacy & Security exposes the typed
`AppRoute.DiagnosticsImprovements` destination with Off/Analytics/Logs/On
summary. Back from that destination preserves immediate changes. A focused
Material alert confirms clearing retained logs; Cancel changes nothing.

## Exact product copy

- **Help Improve White Noise**
- “Help us make messaging without a central point of control more reliable.
  Anonymous analytics and diagnostic logs are optional and can be changed in
  Settings.”
- **Share Anonymous Analytics** / **Share Diagnostic Logs**
- “Analytics never include messages, media, contacts, profile details, or
  keys. Diagnostic logs obscure identifiers and are securely sent to White
  Noise for troubleshooting.”
- **Diagnostics & Improvements**, **Stored Diagnostic Logs**, **On This
  Device**, **None**, **Clear Diagnostic Logs**.
- **Clear diagnostic logs?** / “This permanently removes all recorded
  diagnostic activity from this device. Your logging preference won’t change.”
- **Clear Logs** / **Cancel**.
- “Turning logging off keeps existing logs until you clear them.”

The detailed switch descriptions are preserved in `analytics_detail` and
`diagnostic_logging_detail` in Android string resources.

## Android composition

A content-height Material `ModalBottomSheet` with its default handle, shape,
width, safe/IME insets, and native dismissal. The shared surfaceContainerLow
sheet uses `WhiteNoiseSheetHeader` (titleLarge, 24 dp margins, trailing native
Close, no extra top spacer, 8 dp body gap). That compact header stays
above a scrollable body. Existing tonal `SettingsGroup` and merged
`SettingsSwitch` rows provide touch targets, state, focus, and ripple; Material
typography and the shared 16/24/32 dp composition rhythm own spacing. The
permanent destination uses the shared pinned header and actual scrolling list.

## Behavior and state

`Profile.diagnostics: DiagnosticsState` owns analyticsEnabled, loggingEnabled,
hasSeenPrompt, and retained records. `AppUiState.pendingDiagnosticsProfileId`
is transient scheduling state, retained by the ViewModel across rotation.
Mutations are ID-bound, so a stale callback cannot change another profile.
Developer Tools may inspect records, including when logging is off; its link
opens the consumer diagnostics destination rather than duplicating controls.

## System integrations

None. Android formats the retained byte count. The sheet and navigation use
standard Material/AndroidX lifecycle and Back handling.

## Accessibility and adaptation

Native switch roles expose Off/On, and the entire labeled row toggles once.
Copy scrolls at 200% font scale/short heights; there is no fixed iOS detent.
Directional insets, native 48 dp minimum controls, and monochrome semantic
light/dark roles apply. The Close/title row is separate from the scrolling
copy. Clearing has an explicit consequence, not a color-only signal.

## Governing Android sources

- [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Material switches](https://developer.android.com/develop/ui/compose/components/switch)
- [Material app bars](https://developer.android.com/develop/ui/compose/components/app-bars)
- [Material insets](https://developer.android.com/develop/ui/compose/system/material-insets)

## iOS parity evidence

Explicitly authorized scoped baseline: `wn-ios-prototype@4c25393`, not a
repository-wide baseline refresh:

- `docs/screens/diagnostics-and-improvements.md`
- `WhiteNoisePrototype/App/PrototypeDiagnosticsState.swift`
- `WhiteNoisePrototype/App/WhiteNoisePrototypeApp.swift`
- `WhiteNoisePrototype/Screens/Shared/DiagnosticsAndImprovementsViews.swift`
- `WhiteNoisePrototype/Screens/Chats/ChatsView.swift`

## Approved differences and custom exceptions

Use Android's content-height Material sheet and lifecycle-completed navigation
instead of SwiftUI detents/presentation timing. “On This Device” replaces the
iPhone-specific device label. No custom-drawn control or telemetry integration.

## Observable acceptance criteria

- `DiagnosticsConsentTest`: every onboarding origin, transition gate,
  profile isolation, dismissal/retention, developer independence, clearing,
  summaries, and wipe.
- `DiagnosticsPromptTest`: resumed-only presentation, native switch changes,
  saved-state recreation, Close/Back/scrim/swipe, a new profile prompt, and confirmed
  clearing with large dark RTL content.
- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
- Device execution, actual keyboard/rotation/predictive-Back gestures,
  TalkBack, and visual acceptance are separately user-requested verification.
