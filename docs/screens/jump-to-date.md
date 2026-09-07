# Jump to date

## Purpose and scope

Issue #18 and the user's 2026-09-07 approval add calendar navigation inside chat
search, for direct chats and groups. This extends the existing in-memory history
window; it adds no storage, network seek, protocol integration or new destination.

## Parity and navigation

Open chat search, then the calendar beside the search field. The Material date
picker opens on the first visible message/event's date, or today if there is no
available anchor. Cancel and system Back close only the picker, preserving search,
viewport and draft. Jump targets the first available message or system event on
or after the chosen civil day. Empty days remain selectable within the range; if
nothing follows, use the newest available entry. Deleted messages and informational
notices cannot be targets. Successful navigation exits search and keeps the target
in view, with the draft intact; it does not restore the pre-search position.

The B08 history mapping in `docs/port/source-map.md` and
`docs/screens/conversation-history-and-reading.md` govern paging, target recovery
and reading. Date navigation is an explicitly approved extension, not baseline
iOS parity. Production Android issue `marmot-protocol/whitenoise-android#1887`
describes the intended capability; it was still open and absent from production
`b6f99709972604ebaed00b15bc15e1eb8ce6fcaf` when checked. The user selected calendar
access inside search rather than an additional overflow item.

## Android composition, copy and state

Native Material IconButton, DatePickerDialog and DatePicker retain standard
focus, dismissal, touch targets, month/year navigation, locale and typed-date
input. Copy is “Jump to date”, “Jump”, “Cancel”, “No messages to jump to.”, with
all five app locales supplied. Existing history loading/failure/unavailable/retry
copy remains shared with other exact-entry navigation.

The earliest eligible local entry and fixed prototype today bound selection.
Empty history disables Jump. `ConversationDates` derives a small index from the
authoritative timeline; no message bodies or second history window are cached.
Real receipt/creation timestamps use the device zone captured for the visit;
fixtures use the existing `GlobalSearchClock` civil-date mapping and fixed
2026-08-03 today. The native picker's UTC-midnight value is interpreted as a
civil date, then resolved at the beginning of that day in the captured zone,
including daylight-saving transitions. Equal timestamps use stable ID order.

Picker visibility, its native selection/display mode, anchor and captured zone
restore with the conversation. Date seeks reuse the existing bounded 18-entry
target window, cancellation generations and loading/retry states. A failed or
removed target keeps the old window. Search results completing in the background
cannot supersede a date seek. Background/disposal cancels pending history work.

## Accessibility and adaptation

The calendar is labelled “Jump to date”; native dialog focus and Back apply.
The input method hides on entry. The calendar body scrolls when constrained,
while confirmation buttons remain in the native dialog footer. Native locale,
RTL and year/month controls remain intact. Semantic monochrome roles support all
themes. After a jump, the date heading briefly uses primaryContainer with its
matching content role and announces the localized full date through a polite
live region. Existing message highlight also applies where the target is a
message. Interrupted highlights clear without lingering state. No new animation
or system permissions are introduced.

## Official Android sources

- https://developer.android.com/develop/ui/compose/components/datepickers —
  native date dialog, UTC date representation and input-mode toggle; checked 2026-09-07.
- https://github.com/google/material-design-icons/blob/master/symbols/android/calendar_month/materialsymbolsrounded/calendar_month_24px.xml — official calendar icon.

## Observable acceptance and validation

- Search exposes a calendar with bounded selection; empty and deleted-only history cannot jump.
- Exact days, gaps, system events, duplicate timestamps, zone offsets and daylight-saving boundaries resolve deterministically.
- Cancel preserves search position; successful Jump exits search, reveals the target and retains the draft.
- Failed/unavailable, retried, cancelled and superseded requests preserve valid history state.
- Picker restoration, search cancellation and system-event navigation have Compose regressions.

Host gate passed: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`. All 979 unit tests pass, including 11 date/index/request regressions. Lint and both APK assemblies pass; four date Compose regressions compile. Device execution and visual acceptance are not claimed.
