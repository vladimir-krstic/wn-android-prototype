# Key packages and developer diagnostics

Status: B25 implemented and host-verified. Device/visual acceptance pending.

## Purpose and scope

Complete C080, C110, C111, C118 and C119 within the established developer gate.
Q04 remains about ordinary Settings exposure; it does not block these states.
No backend, protocol execution, network, telemetry, persistent storage, or new
permission is introduced. B29 owns ordinary agent streaming operations.

## Parity contract

Key Packages separates Publishing, Published and Retained Local Material.
Republish preserves material identity; Publish New rotates it; Refresh reloads
inventory. Show local/relay provenance, publish time and seen-on relays.
Deletion requires confirmation, removes relay publication, and preserves local
material. Partial relay outcomes remain visible and retry only unfinished work.
Loading, empty, unavailable and failed results never masquerade as success.

Diagnostics retains the accepted Events console and its overflow commands.
A Health & Performance sheet contains relay connection counts, attempts and
successes, runtime counts, Refresh, Send to Self and opt-in 30-minute performance
state. Outcomes and summaries are sanitized; self-send never creates chat-list
artifacts. Performance uses monotonic elapsed time, expires after leaving the
screen, and is available only in debug builds. No runtime logs are collected.

Conversation Debug shows required kinds/components and detailed push registration
and member-token state. Raw identifiers stay within Developer Tools; copied
Diagnostic Summary excludes identity, group IDs, token values, relay URLs and
message content. Streaming Debug is a separate profile preference and exposes
read-only technical rows beside streaming messages without changing canonical
history, unread counts, selection, forwarding or export.

## Entry, navigation, Back and state

Retain Settings → Developer Tools and existing typed routes. The master gate
removes inspection and cancels pending work when disabled. Profile switching,
sign-out and route exit invalidate operation leases; late completions cannot
mutate another profile or a reopened screen. Dismissing a delete confirmation
is inert. Back cancels uncommitted work; completed local changes remain.

## Android composition, accessibility and adaptation

Reuse SettingsScaffold/List/Group, shared 16/24/8 spacing, Material list rows,
full-width 56 dp contextual tonal publication action, AlertDialog for deletion,
and a scrollable Material bottom sheet for health. Keep one persistent console.
Native buttons/switches own focus, roles, touch targets and disabled state;
progress/error is named in text and semantics. Long values wrap or ellipsize
visually while explicit copy retains the full value. Shared insets/adaptive
content bounds remain authoritative; no new fixed screen height.

## Copy and system integration

New strings live in strings.xml with the developer_ prefix. Principal labels:
“Republish”, “Publish New Key Package”, “Retained Local Material”, “Seen on”,
“Delete Key Package?”, “Health & Performance”, “Send to Self”, “Performance Logs”
and “Streaming Debug”. Deletion explains that retained local material remains.
Only existing clipboard integration; no new system-owned flow.

## Governing evidence

- [Material dialogs](https://developer.android.com/develop/ui/compose/components/dialog): consequence confirmation.
- [Compose effects](https://developer.android.com/develop/ui/compose/side-effects): route-scoped work and cancellation.
- [Existing developer brief](developer-and-destructive-flows.md) and WN-ANDROID-0073/0075/0076 preserve presentation.
- [Source map](../port/source-map.md): pinned iOS Settings brief,
  Screens/Settings/DeveloperSettingsViews.swift and
  Screens/Conversation/ChatDeveloperToolsView.swift, DeveloperToolsTests.swift.
- Production baseline 319454889f1c2494dec4a69b5577d98017f44eee:
  KeyPackagesScreen.kt, DiagnosticsScreen.kt, DeveloperScreen.kt,
  GroupInfoScreen.kt, GroupDetailsScreen.kt, StreamDebug.kt and PerformanceDiagnostics.kt.
  Master rechecked at 911040c7e1c31652638c8cfd72812d1f3a694b9b on 2026-09-04;
  the B25 contracts are unchanged. GroupDetailsScreen warm-roster changes were already reconciled in B18.

## Acceptance

Verify publication identity and provenance, partial retry/delete confirmation,
profile/route ownership, health failure/recovery, self-send cleanup, elapsed-time
expiry, stream-debug display isolation and sanitized-copy boundaries. Run unit
tests, lint, app assembly and instrumentation APK compilation. Device execution
and visual acceptance remain pending an explicit user request.

## Implementation evidence

DeveloperParityController owns exact profile/route/source-revision leases,
confirmation, recoverable outcomes and partial retry. DeveloperInspection derives
inventory/health; DeveloperModels carries detailed push data and independent
stream/performance preferences. DeveloperParityUi renders sections and the health
sheet; DeveloperScreens retains the persistent console. ConversationScreen adds
read-only stream status outside message controls, without modifying history.

The host gate `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes 772 unit tests with no failures/errors/skips, zero lint errors and both
APKs. DeveloperParityStateTest adds 24 behavior/ownership cases;
DeveloperParityInteractionTest adds six compiled interaction cases. Existing
key-package tests now exercise the staged implementation. No device tests ran.

All states remain in memory. Performance models a timed opt-in state; it has no
emitter or sink. Self-send records a sanitized result without creating a real
chat or network message. Streaming Debug covers the existing streaming-message
inspection; ordinary agent actions/terminal state delivery belong to B29.
