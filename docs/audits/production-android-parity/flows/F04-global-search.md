# F04 — Global message and people search

## Purpose and current composition

Chats search expands into global results and typed filters. Opening a result preserves query/filter state and targets the exact message. Back clears a modal filter, then closes search.

Prototype surface: `ui/chats/ChatsScreen.kt; model/MessageInteractionModels.kt; navigation/AppRoute.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Search chats**, **Filters**, **Chat**, **Sender**, **Date**, **Content**, **Clear all**, **No matches**, and **Couldn’t load more** with **Retry**. A result labels its chat/sender and never exposes raw identifiers as the primary name.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C031 · Cross-chat message-body results | Implemented; host verified | Open search, filter or result; modal Back dismisses first and result Back restores search | Grouped Chats/Messages/People span active-profile history, including archived and ended chats. Ordinary chat matches retain normal opening; authored body/attachment/link matches have centered snippets and exact-message navigation. Deleted/system/private-note data is excluded and Back restores query/filters. |
| C032 · Filter global search by chats and senders | Implemented; host verified | Open search, filter or result; modal Back dismisses first and result Back restores search | Multi-select chat and sender filters use OR within and AND across categories. Searchable checkbox dialogs, removable chips, Clear All, saved state, profile reset and removed-ID reconciliation are connected. |
| C033 · Global date and content filters | Implemented; host verified | Open search, filter or result; modal Back dismisses first and result Back restores search | Today/7-day/30-day/custom inclusive civil-date bounds and all six content categories combine with query/chat/sender filters. A fixed UTC fixture calendar preserves visible timeline labels; native DateRangePicker and DST/boundary tests cover dates, with Android desugaring retaining API 23 support. |
| C034 · Identifier lookup and voice query entry | Implemented; host verified | Open search, filter or result; modal Back dismisses first and result Back restores search | Supported public keys, profile links and addresses reuse discovery with loading, invalid, unresolved, unavailable and retry states; unknown profiles have a readable name. Deterministic Voice Search success/cancel/unavailable/retry is guarded by owner/request/query and consumes no microphone or service. |
| C035 · In-conversation search and target navigation | Implemented; host verified | Page, search, reveal or inspect; Back/cancel retains content and prevents stale navigation | Complete local-history search retains immediate loaded matches and stable result identity. Failed scans and missing/deleted targets have visible Retry/Cancel without blanking the transcript. Exact search/reply/media targets load their window before reveal; superseding input or navigation cancels old work. Chat Info reuses the existing conversation and search close restores its anchor, offset and draft. |

## Production integration seam

Production evidence for each row is linked in the [matrix](../capability-matrix.md). During prototype work, add the smallest profile-owned immutable fixture/state transitions and callbacks needed to render every named result. Do not add Marmot, networking, signing, persistence, notification delivery, background services or cryptography. Name production events and ownership in the selected screen brief so the eventual migration reconnects to the cited controller/state methods rather than copying prototype fixtures into production storage.

## Copy, accessibility and adaptation

Use the approved product language and terminology. Production strings in the matrix are evidence of meaning, not automatic final copy. Keep raw keys, event IDs, MLS and engine errors off ordinary surfaces; developer surfaces may be exact. State must be conveyed by text/semantics as well as icon/color. Provide accessible equivalents for gestures, logical focus and Back order, and preserve action eligibility at large type and narrow height.

## Acceptance and host validation

- Every linked capability has a deterministic route/fixture and every mutation yields the specified success, cancellation, unavailable and failure outcomes relevant to it.
- Back, profile switching and restored state cannot commit work to the wrong profile/chat or repeat a completed mutation.
- Existing capabilities in this flow retain their current model and UI tests.
- Add unit tests for rules/ownership and Compose tests for durable navigation/actions/semantics. Run targeted host tests while iterating and the repository static gate after a meaningful batch. Compile instrumentation tests only; device execution and visual acceptance require a separate current request.

## Dependencies and decisions

Batches: B07, B08. Decisions: None. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B07 implementation evidence

C031–C034 are implemented and host-verified 2026-09-04. The
[selected brief](../../../screens/global-search.md#implementation-evidence)
records the global result/filter/lookup/voice contracts, fixed calendar and
production integration seams. The clean gate passed 296 unit tests, both APKs
and zero lint errors; eight new UI cases compile. C035 was deferred to B08
and is now completed below. Device execution and visual acceptance
remain separate.

## B08 implementation evidence

B08's selected capabilities are implemented and host-verified 2026-09-04. The
[selected brief](../../../screens/conversation-history-and-reading.md#implementation-evidence)
records history/search recovery, captured/visible unread state and delivery facts,
plus the eventual production page, target, read-anchor and metadata seams.
The clean gate passed 319 unit tests, both APKs and zero lint errors; twelve new
UI cases compile. No device execution or current-build visual acceptance is claimed.
