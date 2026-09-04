# Global and per-chat notification controls

## Purpose and selected scope

B22 covers C089–C093. Extend Notifications, add Sounds & notifications from Chat/
Group Info, and extend existing mute entry points with custom date/time. Preserve
the approved monochrome Settings hierarchy, contextual Android permission gate,
inline preview choices and immediate selection of existing mute presets.

Q06 retains actual notification delivery, push registration and foreground services
outside this batch. The prototype boundary also excludes new vibration permissions,
channel/shortcut publication and persistence.
Use profile/chat-owned deterministic states and developer-only outcome controls.
Existing app notification Settings and runtime permission surfaces stay native.
Do not imitate Android category settings or claim a settings handoff changed a
system preference. Missing category targets fall back truthfully to app Settings.

## Production evidence and drift

Pinned production `319454889f1c2494dec4a69b5577d98017f44eee`, under
`app/src/main/java/dev/ipf/whitenoise/android`: NotificationsScreen, AppState
setLocalNotificationsEnabled/setBackgroundConnectionEnabled/isNativePushAvailable,
BackgroundConnectionPolicy, NotificationChannelSpec, ConversationNotificationRouting,
ConversationNotificationSettingsScreen, ConversationNotificationCategoryRows,
ConversationVibrationPreferences, MuteDurationDialog, ChatMutePreferences and
MuteOverrideReconciliation. Production test counterparts cover these contracts.

Fresh master remains `911040c7e1c31652638c8cfd72812d1f3a694b9b` on 2026-09-04,
without main. Its seven-commit diff changes notification startup/routing tests,
not these settings, categories, vibration or mute contracts. Broader startup and
route-window drift remains for the later/final audit.

## Behavior and ownership

Local notification and push changes have pending, accepted and failed/retry states.
Push availability requires configured build, Google Play services and initialized
provider; each missing dependency is observable without adding those integrations.
Approved WN-ANDROID-0064/0065 keeps push and preview dependent on effective local
notifications. Permission loss changes effective controls without erasing stored
preferences. Disabling local notifications also stops background connection.

Keep connected in the background is an app-wide preference and survives profile
switches. Erase App Data resets it. Enabling it models enabling local notifications
for the initiating profile first,
permission denial, enabling, accepted connection, start rejection and exhausted/
stopped state. Rejection reverts the connection switch and offers Retry; accepted
local enabling remains accepted. No service actually starts.

Global categories: Direct messages, Group messages, Mentions, Reactions, Invitations,
Group membership, Agent activity and distribution-gated App updates. Category rows
open Android-owned settings; unavailable/scoped fallback is explicit. Per-chat
primary message settings belong to that chat. Mentions, Reactions, Invitations
and Agent activity can inherit global defaults or use custom settings for this
chat. Returning to inheritance preserves unused custom state. Group membership
and App updates remain global-only. Do not create/delete system channels here.

Notify for stores All messages or Mentions only independently of mute. Nothing
means muted and never overwrites the restore choice. Mute/unmute, duration changes
and expiry preserve that choice; changing the restore choice while muted need
not unmute. Exact future custom date/time and preset deadlines use the existing
fixed foreground clock, with native calendar/time input and proper zone conversion.
Always has no deadline. Reject past/elapsed custom times at final acceptance.
Expiry updates canonical mute state so list icons, folder rules and Info agree.

Vibration choices are System default, Short, Double and Long. Show selected versus
effective Android override, disabled or custom waveform state. A local preview
has pending/playing/unavailable outcomes and no fabricated claim of device output.
Reconnecting real device vibration is a migration seam under Q06.

Requests bind profile/chat, generation and current relevant preferences. Repeated
callbacks, stale routes, explicit profile changes, sign-out/wipe and lost chats
cannot commit to a new owner. Failures retain prior values; Retry revalidates the
current owner/selection. New requests supersede old requests. Stop callbacks and
system-return observations must remain bound to their originating owner.

## Presentation, copy and Back

Reuse SettingsScaffold, SettingsGroup/Link/Switch/Choice, shared 4/8 dp rhythm and
adaptive bounds. Retain Notifications' preview example inline. Add Sounds &
notifications as a typed detail route, with mute state/expiry, Notify for,
Vibration pattern and category scope/settings actions. Scope and settings actions
remain separately accessible controls. Use semantic disabled/error/live status,
48 dp targets, selectable groups and resource-based copy.

Use “Keep connected in the background”, “Notify for”, “All messages”,
“Mentions only”, “Nothing”, “Custom time”, “Muted until …”, “Use global default”,
“Custom for this chat”, “Off in Android Settings”, “Retry” and “Open Settings”.
Native Back dismisses the current date/time/choice dialog before the detail route;
cancellation does not alter the saved preference. Custom time requires a final
future-time check. Existing mute presets continue applying with one selection.
Keep controls useful at large text/display scale, compact/expanded width, RTL,
short landscape, IME, keyboard and assistive access.

## Native evaluation and sources

- [Notification channels](https://developer.android.com/develop/ui/compose/notifications/channels): Android owns post-creation sound, vibration and importance; read current settings and hand off to system settings.
- [Date pickers](https://developer.android.com/develop/ui/compose/components/datepickers) and [time pickers](https://developer.android.com/develop/ui/compose/components/time-pickers): native Material selection/input and cancellation.
- [Source map](../port/source-map.md), [shared metrics](../ui-metrics.md), [native evaluation](../references/native-ui.md), WN-ANDROID-0064/0065 and existing notification/mute briefs govern local presentation.

## Acceptance and validation

Every capability's success, pending, cancellation, unavailable and failed/retry
outcomes are observable. Test dependency/rollback, owner isolation, category scope
and Android override projection, mute restore/expiry and custom-time boundaries.
Compile durable Compose interaction/semantics tests and run the full host unit,
lint, debug APK and instrumentation-test APK gate before the B22 commit.
No device/emulator execution or visual acceptance is authorized.

## Implementation evidence

Implemented in `NotificationControls.kt`, `NotificationController.kt`, the
navigation host, Notifications and the new Sounds & notifications route from
Chat/Group Info. Delivery requests validate permission, provider availability,
origin profile, current values and generation. Failures retain previous settings;
background start rejection retains an accepted local-enable stage while reverting
the app-wide connection preference. That preference survives profile switches and
sign-out, and Erase App Data resets it. Runtime stop has an explicit retry.

Category identities and primary/custom/global policies mirror production. Scope
changes and staged vibration selection have owned save/retry/cancel states. The
selected vibration is kept separate from effective Android override and unavailable
preview states. `NotificationCategoryAccess.kt` checks existing system categories,
uses the public channel-settings action and reports app-wide fallback/failure.
No channel creation, shortcut publication or system-setting imitation is added.
App update availability and environment outcomes are developer-only; reconnect
real build distribution, channel observations and provider readiness in production.

The shared Mute for dialog retains immediate preset selection and adds a native
DatePicker/TimeInput sequence with UTC calendar-day to chosen-zone conversion,
final future-time validation and cancellation. Chat notification mode remains
All messages or Mentions only while mute determines effective Nothing. The existing
foreground clock expires canonical mute deadlines across signed-in profiles;
icons, folder eligibility and Info therefore read the same state. Custom input
is connected at Chats, Chat/Group Info and Sounds & notifications entry points.

Host gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
714 unit tests pass with zero failures/errors/skips. B22 adds 21 cases: seven pure
policy/time tests and 14 state/ownership cases, including app-wide background
preference, partial failure, stale callbacks, changed settings and expiry. Ten new
Compose/platform cases compile/package only. Existing mute-dialog call shape and
preview tests are retained, with scroll-aware assertions where content expands.
Lint has zero errors, 14 existing warnings and two hints; both APKs assemble.
No device execution or visual acceptance is claimed.

Migration reconnects AppState's real settings/MDK commands, app-wide
BackgroundConnectionPreferences and runtime service feedback, provider readiness,
NotificationChannelSpec/ConversationNotificationRouting and prepared child targets,
Android effective vibration/preview, authoritative mute results and engine expiry.
Production NotifyForDialog has only All/Mentions; Nothing is the separate mute
state. Q06 retains actual delivery, push, services, publication, vibration permission
and persistence outside local coverage. Commit:
`B22: Add global and per-chat notification settings`.
