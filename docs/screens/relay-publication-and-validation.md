# Relay publication and validation — B28

## Purpose and scope

Add production relay-list readiness, refresh, publication and recovery to the
accepted Relays flow while preserving its general secure URLs and Profile,
Inbox and Chat Messages roles. This completes the unblocked C108/C109 state
work with deterministic in-memory operations. No relay traffic, Nostr events,
DNS, persistence, signing or background work is added.

Q04 continues to govern a future reduction to only White Noise managed hosts,
two role lists, or moving technical package data into consumer Settings. B28
does not silently clean imported relays, remove Chat Messages, or relocate Key
Packages.

## Parity contract

- Publication readiness is separate from Connected/Reconnecting/Disconnected.
  **Where I post** and **Where I receive** each show Published or Missing.
  A third result reports that list status is unavailable.
- **Refresh status** has pending, all-published, either-list-missing,
  both-missing, unavailable and failure results. The last accepted projection
  remains visible after a recoverable failure. **Retry** uses a new request.
- A Profile-role URL mutation marks Where I post missing; an Inbox-role mutation
  marks Where I receive missing. A mutation affecting both marks both. A
  Chat-Messages-only change and socket-state-only change do not change
  publication readiness. **Publish missing lists** has pending, success,
  unavailable and failure/retry states.
- Operations capture the exact profile, route instance, relay publication
  revision and role/URL signature. Back, profile changes, deletion, sign-out,
  relay edits and stale completion cancel or reject that request. Accepted
  projections remain profile-owned across profile switching in this session.
- Existing URL normalization and unique `wss://` validation remain. The UI
  does not claim production managed-host support or remove the final role.

## Entry, navigation and Android composition

Settings → Relays remains the single consumer destination and keeps the shared
Settings scaffold, relay rows, Add Relay sheet, role details and restore/remove
dialogs. A **Relay lists** group precedes endpoint rows. Material list rows show
both named statuses; native buttons own Refresh/Publish/Retry. A 20 dp
indeterminate circular indicator accompanies an in-progress action. A concise
semantic error callout keeps recovery local. System/predictive Back cancels the
route lease and returns to Settings; no completed callback may reopen or mutate
the route.

## Product copy

**Relay lists**, **Where I post**, **Where I receive**, **Published**,
**Missing**, **Status unavailable**, **Refresh status**, **Refreshing…**,
**Publish missing lists**, **Publishing…**, **Retry**. Published help:
**Your relay lists are published.** Missing help: **Publish the missing lists so
other people can find where to reach this profile.** Failure:
**Couldn’t refresh relay lists. Your last known status is shown.** Publication
failure: **Couldn’t publish relay lists. Your relay settings were kept.**

## Accessibility, adaptation and system integrations

Status is always text, never color-only. The in-progress indicator shares the
visible action label and contributes no duplicate focus target. Native rows and
buttons retain 48 dp targets, logical traversal and disabled semantics. The
group wraps at 200% text, RTL/localization and compact/expanded widths inside
the existing adaptive Settings bound. No system integration or permission.

## Governing evidence

- [Compose progress indicators](https://developer.android.com/develop/ui/compose/components/progress),
  checked 2026-09-04: use indeterminate progress for work with no measurable
  completion fraction.
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
  continues to govern existing relay confirmations; no new modal is required.
- [Existing relay brief](relays.md) and decisions 0070–0072 govern layout,
  role controls, socket status, URL validation and copy hierarchy.
- [Production matrix](../audits/production-android-parity/capability-matrix.md),
  C108/C109, pinned `319454889f1c2494dec4a69b5577d98017f44eee`:
  `ui/settings/RelaysScreen.kt`, `state/AppState.kt`, `state/MarmotClient.kt`,
  and `RelayUrlsTest.kt`.

## Observable acceptance and validation

Unit tests cover independent list/socket state, every refresh result, role
mapping, exact request ownership, changed-input cancellation, stale completion,
profile switching and retry. Compose cases cover published/missing/unavailable,
pending/error/retry, Back cancellation, semantics and large-type reachability.
Run the complete host gate and compile the instrumentation APK; no device or
visual verification is claimed.

## Implementation evidence

Implemented and host-verified on 2026-09-04. `RelayPublication.kt` defines the
independent list projection, signature and work contract.
`RelayPublicationController` retains accepted per-profile projections, maps
role/URL mutations to the exact missing list, and rejects callbacks after Back,
profile changes, settings changes or stale revisions. `RelayPublicationUi`
adds the native status/action group and route-owned progress driver.

AppViewModel now exposes exact-profile relay mutations; delayed custom-relay
callbacks cannot target a newly active profile. General `wss://` endpoints and
all three accepted roles remain. A developer action adds one invalid imported
address with every role intact; the ordinary screen names the issue and keeps
explicit remove/replacement recovery. Socket status changes do not change list
publication state.

809 unit tests pass with no failures/errors/skips. Eighteen added unit cases
cover projection rules, every result, retries, profile/route/revision ownership,
role mapping, import preservation and AppViewModel guards. Seven new Compose
cases compile for published/missing/unavailable, pending/error/retry, Back,
invalid imports and 200% type. The full host gate
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes with zero lint errors and both APKs; 14 existing warnings and two hints
remain. No device/emulator execution or visual acceptance.

Current production `master` remains
`911040c7e1c31652638c8cfd72812d1f3a694b9b`. Its RelaysScreen, relay-list
mutation/validation methods and tests are unchanged from the pinned audit;
AppState drift does not touch this contract.
