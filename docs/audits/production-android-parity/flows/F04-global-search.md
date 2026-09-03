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
| C031 · Cross-chat message-body results | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add grouped chat/message/profile results with snippets and exact-message navigation; current Chats filters rows and conversation search is local. |
| C032 · Filter global search by chats and senders | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Multi-select chat and sender filters; OR within each category, AND across categories, removable chips and Clear all; reset/reconcile on profile switch. |
| C033 · Global date and content filters | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Date presets/custom bounds plus text, links, images/video, voice/audio, files and any attachment; test boundary inclusion using fixed clock. |
| C034 · Identifier lookup and voice query entry | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Resolve supported public IDs/addresses to profile result and offer deterministic voice-query success/cancel/unavailable. Do not request microphone in audit or fixture implementation. |
| C035 · In-conversation search and target navigation | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Keep current highlighting/navigation; add loading older-result, failed-history, unavailable target and retry without blanking existing timeline. |

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
