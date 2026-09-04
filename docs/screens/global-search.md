# Global search — B07

Selected 2026-09-04 under the all-batches goal. C031–C034. Implemented and
host-verified; device/visual acceptance remains pending. B08 owns unloaded-history
recovery inside a chat.

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

Use the shared compact search field, adaptive list, Material filter/input chips,
checkbox/radio dialogs and native DateRangePicker. Filters are removable singly
and through Clear All. Back dismisses a picker/filter before search; selection
still takes priority. Save query and small filter values across recreation and
navigation; reset on profile change and reconcile removed chat/sender IDs.
Existing long-press chat actions and selection remain available for chat rows.

Voice Search is a deterministic entry dialog with success, cancellation and
unavailable/retry. Success supplies the phrase “trailhead”; cancel/failure never
overwrite existing text. Results are guarded by owner/request/query and cancelled
on navigation. No RecognizerIntent, microphone permission or recording is used.
The eventual production seam is the existing RecognizerIntent result contract.

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

Only in-memory state and existing loaded data. No backend, persistence, network,
permission or speech service. Android's core-library desugaring 2.1.5 supplies
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
  checkbox/radio filter dialogs and DateRangePicker. Empty and failed lookup
  results keep their useful actions visible. Profile switches reset state and
  deleted chats/senders are reconciled. Opening a result preserves search for Back.
- `AppViewModel.openGlobalSearchMessage` checks owner/chat/message before marking
  the chat read. `WhiteNoiseNavHost` carries the exact target ID to Conversation
  and reuses accepted discovered-person navigation. Developer Tools supplies
  one-shot voice outcomes; ordinary Voice Search has success/cancel/unavailable
  and retry with no microphone or speech-service access.
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
