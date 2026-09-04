# Location sharing — B14

## Purpose and scope

Implement C058 under the authorized B01–B32 goal: select a single fixed point,
review it, send a location message and open its coordinates explicitly in Maps.
B11/B13 attachment entry and message reading remain intact. Q06 keeps real GPS,
location permissions, tracking, map tiles, geocoding, networking and osmdroid
outside this prototype. No device/emulator execution is authorized.

## Parity contract

Production master `319454889f1c2494dec4a69b5577d98017f44eee` uses
`LocationPickerScreen`, `LocationShare`, `LocationBubble`, `BubbleContentBlocks`
and `Controllers.send`. One-shot location may be precise or approximate;
missing grants/providers/fixes must not produce a sent point. Explicit selection
is required: zero/zero is valid only when actually entered or selected.

The wire-compatible body is `Location: https://maps.google.com/maps?q=lat,lng`,
with dot-decimal coordinates to six decimal places. Read whole-body legacy
`https://maps.google.com/?q=...` and encoded-comma variants too. Out-of-range,
non-finite, partial, spoofed-host and prose-wrapped URLs remain ordinary text.
A location-only body becomes a coordinate card without fetching a map preview.
Copy/forward/search retain the full source body and ordinary prose remains visible.

## Entry, navigation and Back

Composer Add → Location opens a full-screen native form. Enter Latitude and
Longitude, or request current location. The default current-location result is
unavailable; Developer Tools supplies precise/approximate, denied, services-off,
request-failure and send-failure states without reading a device position.
Review location shows the exact selected point and available accuracy. Back
returns from review to editing; Back/Close during editing or work cancels the
owned session. There is no implicit send on selection, dismissal or restoration.

Send this location appends one standalone location message. It preserves draft
text, attachments and suppressed preview. A reply selected when opening the
picker is included and consumed only if still current and readable. Failed send
retains the point for Retry; Cancel leaves the entire ordinary draft unchanged.
Profile/session changes, closed requests, stale revisions, changed/deleted reply
sources and lost chat eligibility cannot send. Foreground lifecycle owns delayed
local outcomes; ordinary configuration recreation retains the in-memory session.

## Exact product copy

“Location”, “Latitude”, “Longitude”, “Use current location”, “Finding location…”,
“Review location”, “Send this location”, “Sending location…”, “Change location”,
“Open in Maps”, “Copy coordinates”, “Copy link”, “Retry”, “Cancel”.

Show specific messages for invalid coordinate bounds, approximate accuracy,
location access denied, services off, unavailable/failed current location,
changed reply/chat eligibility and failed sending. No fake system permission
prompt or unusable Open Settings action. Current-location failures leave manual
coordinate selection available. Map failures offer copyable coordinates/link
and Retry, without silently sending the point to a browser.

## Android composition and system boundary

Use the shared Material app bar, tonal form fields, primary task button and
neutral coordinate card. Manual decimal entry is the bounded, accessible
alternative to production's networked map selection; do not draw invented map
streets. Both fields keep persistent labels, locale decimal input and clear
range errors. The scrollable body and pinned action own IME/safe-drawing insets.
Review displays the point with a Material location symbol, not a fetched image.

An explicit Open in Maps tap reuses the existing permissionless external-view
boundary with a `geo:` ACTION_VIEW proposal. The receiving app owns its appearance;
missing handler/security failures remain in White Noise with Copy/Retry. This
adds no runtime location permission and does not query or execute maps during
agent validation. Location cards make no request on receipt or rendering.

## Accessibility and adaptation

Native labeled fields, buttons, selection and semantics remain in charge.
Coordinates are displayed left-to-right within RTL layouts; labels/actions adapt
normally. Full values remain selectable/copyable and are never conveyed by color
alone. Support large type, compact/expanded widths, landscape, keyboard/IME and
TalkBack names. Loading and errors carry visible labels/live-region feedback.

## Evidence and integration seam

This is a production Android extension absent from the pinned iOS inventory.
Keep [composer attachment decisions](composer-attachment-actions.md),
[attachment reading](text-attachments-and-shared-content.md), shared metrics and
native patterns as the presentation authority. Production reconnects current
location grants/provider/cancellation, map selection and controller acceptance;
prototype states must not become a second database or location service.

Current official sources opened 2026-09-04:
- [Android common map intents](https://developer.android.com/guide/components/intents-common#Maps).
- [Native Compose text input](https://developer.android.com/develop/ui/compose/text/user-input).
- [Material location symbol](https://github.com/google/material-design-icons/blob/master/symbols/android/location_on/materialsymbolsrounded/location_on_24px.xml).

## Acceptance and validation

Verify parsing/formatting and false-positive boundaries, valid zero, coordinate
editing/review, unavailable/denied/approximate states, stale callback rejection,
reply/draft preservation, send failure/retry and exactly-once append. Compile
interaction, Back, native map intent/error, copy and adaptive UI cases. Run the
full host gate and update C058/batch evidence before the B14 commit. Device and
visual acceptance remain separate.

## Implementation evidence

Implemented and host-verified 2026-09-04; device/visual acceptance pending.

`LocationSharingModels.kt` owns fixed-point parsing, locale-safe six-decimal
formatting and the editing/locating/review/sending state machine. Empty fields
never select zero implicitly. Whole-body legacy and encoded-comma forms become
cards; prose, invalid values, expired/deleted messages and attached media retain
ordinary rendering. Accessible ± buttons complement decimal keyboards without a minus key. Native
field length limits and value-equality guards prevent
UI synchronization from clearing current-location accuracy.

AppViewModel owns profile/chat/revision sessions. Developer outcomes are consumed
once by their profile and cannot become a current-location example after Developer
Tools is disabled. Close, profile change and lost chat/reply eligibility revoke
pending work. The send path appends exactly once, preserves ordinary text/media/
quality/suppressed-link drafts, and consumes only its unchanged selected reply.
Failed acceptance keeps the point and draft for Retry. The accepted message ID
returns through the composer into the existing history target/reveal path, even
when the user was reading older messages.

`LocationSharingUi.kt` provides the shared tonal native form, review, accuracy,
Back/cancel, foreground progress, specific failures and coordinate cards. No
map request occurs on receipt/render. Explicit Open in Maps constructs a `geo:`
ACTION_VIEW without a package or new permission; no-handler/access errors expose
Copy coordinates, Copy link and Retry. Pasted location-only drafts suppress the
generic link preview so there is one location presentation. Existing text Copy,
forwarding, search, edit presentation and Shared Content URL extraction keep the
wire-compatible body.

`./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
and the final follow-up gate after adding accessible sign controls pass:
**432 unit tests**, no failures/errors/skips, both APKs, zero lint errors,
14 pre-existing warnings and two hints. The 18 added model/state cases cover
coordinates/locale/wire boundaries, false positives, explicit zero, current
location outcomes/accuracy synchronization, cancellation/stale revisions,
review/send retry, profile/developer ownership, draft/reply preservation,
eligibility loss and pasted location preview behavior.

`LocationSharingTest` adds **13 compiled-only** UI/platform cases: composer entry,
review and explicit-zero send, validation errors, denied/manual and approximate
selection, request/send retry, system Back, exact map proposal, no-handler copy/
retry, narrow 200% RTL, signed input and revealing the sent card from older history. No GPS,
network map, new permission, dependency or service was added. No device
Maps execution, layout inspection or visual acceptance is claimed.
