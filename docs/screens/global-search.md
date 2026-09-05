# Global search — B07

Selected 2026-09-04 under the all-batches goal. C031–C034. Implemented and
host-verified; device/visual acceptance remains pending. B08 now implements
unloaded-history recovery inside a chat; see [its brief](conversation-history-and-reading.md).

## Contract and presentation

Expand the existing Chats search field into grouped Chats, Messages and People
results across the active profile, including archived/ended history. Chat title,
preview and group-description matches open the chat normally and retain the
accepted case/diacritic folding. Authored message text/attachment labels and
shareable link destinations produce a centered snippet and open the exact message;
deleted messages and system events never match. People results reuse discovery
and supported public-key/profile-link/address resolution with loading, invalid,
unresolved, unavailable and retry outcomes. Unknown profiles have a readable
primary label, not a raw key. Opening a result preserves search and filters for
Back; explicitly closing search clears them.

Filters select multiple chats and senders, Today/Last 7 Days/Last 30 Days/custom
inclusive dates, and any combination of Text, Links, Images & Video, Voice &
Audio, Files & Documents and Any Attachment. OR within a category, AND across
categories. Chat-only filters can retain ordinary chat results; sender/date/
content filters operate on messages. People results show only when no message
filters are active, avoiding claims that a profile itself matches a date or
attachment type. Chat filters also hide People, since their scope is chat history.
Empty query plus active filters browses matching messages.

Use the shared compact search field and adaptive list. The header uses a native filter icon button (filled when active) and
`WhiteNoiseDropdownMenu`. The 2026-09-05 regular Chats pill row is hidden during
search and does not replace these advanced search categories.
Menu categories open searchable chat/sender bottom sheets, simple-value
checkbox/radio dialogs, and the native DateRangePicker. Active input chips remain
below the header; there is no empty filter row or intermediate category dialog. Filters are removable singly
and through Clear All. Back dismisses a picker/filter before search; selection
still takes priority. Save query and small filter values across recreation and
navigation; reset on profile change and reconcile removed chat/sender IDs.
Existing long-press chat actions and selection remain available for chat rows.

Voice Search launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` through the
Activity Result API. The installed Android recognizer owns listening, language,
permissions, processing, and its UI; White Noise does not draw a substitute
listening dialog. The returned nonblank phrase replaces the current query and
immediately searches the existing local data. Cancellation preserves the query.
Missing/blocked recognizers, unsuccessful results, and empty transcripts offer
Try Again or Cancel without overwriting text.

The outstanding platform launch and its profile/query ownership are saved across
recreation. Only one native request may be outstanding; a late result after a
query edit, closed search, or profile change cannot overwrite another search.
Developer Tools retains explicit one-shot success/cancel/unavailable scenarios;
normal operation and the reset value are Device speech recognition. The fixed
“trailhead” phrase belongs only to the explicitly selected developer success
scenario. No app microphone permission or speech service is introduced.

## Time and integration

The accepted fixtures contain presentation labels and per-chat ordering numbers,
not absolute timestamps. For this batch, a fixed Monday **2026-08-03 UTC** is
Today; Yesterday/weekdays and explicit month/day/year labels map to calendar
values. A weekday/month/day label without a year uses the most recent compatible
year. This preserves all visible fixture labels and timeline ordering, including
explicit 2025 history. It is prototype calendar data, not a claim about real
message receipt. Runtime production will supply timestamps directly.

Date bounds use start-inclusive/end-exclusive civil days in the supplied zone;
Material picker milliseconds are decoded as UTC civil dates. Reversed or incomplete
ranges cannot apply. Tests cover inclusive boundaries, DST and the fixed clock.

## Sources and scope

Production is read-only whitenoise-android@319454889f1c2494dec4a69b5577d98017f44eee:
core/ChatListMessageSearch.kt, ChatListIdentifierSearch.kt; ui/chats/GlobalSearchState.kt,
GlobalSearchTypedFilterSheet.kt and ChatsScreen.kt; search/GlobalSearchDateFilter.kt
and GlobalSearchContentFilter.kt. See [F04](../audits/production-android-parity/flows/F04-global-search.md).

Approved local search/selection presentation is in
[message-interactions-and-search.md](message-interactions-and-search.md),
[chat-organization-and-recovery.md](chat-organization-and-recovery.md) and the
[Chats brief](chats-and-chat-creation.md). Reuse the accepted search highlight
renderer and native focus/Back/inset behavior; no new visual language.

Official sources checked 2026-09-04:
[Date pickers](https://developer.android.com/develop/ui/compose/components/datepickers),
[chips](https://developer.android.com/develop/ui/compose/components/chip),
[state saving](https://developer.android.com/develop/ui/compose/state-saving),
[Java API desugaring](https://developer.android.com/studio/write/java8-support).

Search remains in-memory over existing loaded data, with no backend or durable
storage. The 2026-09-05 user request authorizes real device-owned speech input:
the external recognizer may use its own services/settings, but White Noise adds
no network or microphone permission and does not record audio itself. Android's core-library desugaring 2.1.5 supplies
Java time on API 23–25; the official Google Maven release and Google changelog
were checked before pinning. See WN-ANDROID-0128. No device execution/visual acceptance.

## Acceptance

Verify grouped results and exact target identity; whitespace/markup/snippet and
surrogate boundaries; exclusion of deleted/system/private-note data; filter
algebra/content/date boundaries; profile isolation/reconciliation/restoration;
identifier normalization and failure retry; voice success/cancel/unavailable and
stale completion; preserved query/filters after returning from results. Compile
interaction tests and run the full host gate before the B07 commit.

## Implementation evidence

- `GlobalSearchModels.kt`: grouped chat/body projections, centered and surrogate-safe
  snippets, named-link destinations, typed OR/AND filters, fixed-calendar mapping,
  civil-day bounds, identifier validation and owned voice-result guards. Existing
  case/diacritic chat matching remains; deleted/system/private-note data is excluded.
- `ChatsScreen.kt` / `GlobalSearchUi.kt`: grouped rows with sender/chat context,
  exact-message callbacks, saved query/filter state, removable chips, native
  chat/sender filter sheets, simple-value dialogs and DateRangePicker. Empty and failed lookup
  results keep their useful actions visible. Profile switches reset state and
  deleted chats/senders are reconciled. Opening a result preserves search for Back.
- `AppViewModel.openGlobalSearchMessage` checks owner/chat/message before opening
  its reading context. B08 now marks only settled visible rows read, preserving
  unread content outside the revealed window. `WhiteNoiseNavHost` carries the exact target ID to Conversation
  and reuses accepted discovered-person navigation. Developer Tools supplies
  one-shot voice outcomes. Ordinary Voice Search now uses `VoiceSearchContract`
  and the installed recognizer, preserving query/owner guards and retry.
- `GlobalSearchTest` / `GlobalSearchStateTest`: 19 new passing tests cover matching,
  exclusions, filter algebra, all content kinds, inclusive and DST boundaries,
  fixture label mapping, snippet safety, identifier outcomes, deleted targets,
  profile isolation and stale voice completions.
- `GlobalSearchFlowTest`: eight compiled cases cover exact navigation/Back,
  checkbox/filter semantics, clear/remove, restored state, profile reset,
  deleted-chat reconciliation, no matches, native date ranges, lookup retry and
  readable unknown-profile names, and voice cancellation/failure/retry. The older
  Chats empty-search assertion now reflects the selected “No matches” copy.
- The final clean README gate passed **296 unit tests**, zero failures/errors/skips,
  zero lint errors and both APKs, with the same 14 pre-existing warnings. The
  initial lint gate identified missing API 23–25 Java time compatibility; enabling
  documented Android core-library desugaring resolved it (WN-ANDROID-0128).
  `git diff --check`, capability counts and changed-document link checks passed.
  UI cases were compiled only; no device, emulator or visual acceptance is claimed.

Commit title: `B07: Add cross-chat search and typed filters`.

## 2026-09-05 — native voice search repair

The user reported that Voice Search did not work and requested the Google/Android
flow. The previous timed dialog never recognized speech; it always returned a
fixture phrase. `VoiceSearchContract` now launches the installed recognizer
without styling overrides, parses the returned phrase, and distinguishes success,
cancellation, and unavailable results. `ChatsScreen` saves the pending request,
hides the keyboard before launch, blocks repeated launches, and applies only a
result still owned by the active search. A saved native request does not relaunch
on recreation. Actual listening UI is owned by the installed provider (Google
when that is the configured provider), not guaranteed to look identical on every
device.

`GlobalSearchTest` now validates actual returned phrases, trimming, blank/null
results, stale queries, and owner/search guards. `GlobalSearchStateTest` checks
native defaults, one-shot developer overrides, and reset on disabling Developer
Tools. `VoiceSearchFlowTest` adds six compiled Activity Result registry cases for
native handoff/real phrase, cancellation/empty output, missing provider/retry,
stale query/profile, closing search, and recreation without duplicate launch.
The registry tests never open a recognizer or microphone.

Official sources consulted 2026-09-05:
[RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent),
[Activity Results](https://developer.android.com/training/basics/intents/result).
The final host gate
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes 894 unit tests with zero failures/errors/skips, zero lint errors (16
existing warnings and two hints), and both debug APKs. Six new registry-based
Compose cases compile only. Actual recognition, device presentation, and
hands-on acceptance remain unverified; no device execution was requested.

## 2026-09-05 — shared filter icon and context menu

The user requested the regular Chats filter affordance in Search. Both modes now
use `ChatFilterIconButton`, preserving native touch target, active fill, icon,
and selected semantics. Search anchors the existing `WhiteNoiseDropdownMenu` to
that header button, with Chats, Senders, Date, Content, active-category checks,
and Clear All when applicable. Selecting a category closes the menu and opens
its existing picker directly. Picker dismissal returns to search; query and
filter rules are preserved. Closing Search or switching profiles dismisses its
menu; active chips remain individually removable.

`GlobalSearchFlowTest` uses the new header/menu path in existing filter cases and
adds a regression for active icon/category state and clearing without losing the
query. [Compose menus](https://developer.android.com/develop/ui/compose/components/menu)
was checked on 2026-09-05 for the native icon/anchor/dismissal pattern.
The host gate `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes: 894 unit tests with zero failures/errors/skips, zero lint errors (16
existing warnings and two hints), and both APKs assembled. Updated filter
interaction cases compile only; no device or visual inspection was requested.

## 2026-09-05 — suppress update callout during search

Per user direction, `ChatsScreen` omits the app-update banner whenever Search is
open, including an empty query and filter-only browsing. Closing Search restores
normal banner eligibility without dismissing the update or changing its state.

Debug APK assembly and whitespace checks pass. No device inspection was performed.
