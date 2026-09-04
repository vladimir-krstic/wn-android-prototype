# F05 — Message lifecycle, reading and actions

## Purpose and current composition

Extend the shared conversation and focused-message action surfaces. All mutations re-check authorship, membership and role. Keep pending, accepted, partial and failed outcomes distinct.

Prototype surface: `ui/conversation/ConversationScreen.kt; MessageInteractionsUi.kt; model/MessageInteractionModels.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Edit**, **Edit history**, **Original**, **Select text**, **Forward**, **Delete for me**, **Delete for everyone**, **Message info**, **Retry**, **Discard**, and named partial results such as “Forwarded to 2 of 3 chats.”

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C036 · Text send, reply and failed-send retry | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C037 · Paged history, read boundary and unread mention jump | Implemented; host verified | Page, search, reveal or inspect; Back/cancel retains content and prevents stale navigation | Older/newer pages retain stable rows through loading/failure/retry. Captured unread IDs survive partial reading and profile/chat navigation; only settled visible rows are acknowledged. Covered, searched, backgrounded and unloaded rows remain unread. Off-tail arrivals freeze a boundary without scrolling; next unread mention loads and reveals its exact target before explicit read-through. Failed or stale targets never advance reading. |
| C038 · Edit own message, retry/discard and revision history | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add edit draft, nonblank save, cancel, pending/failed retry/discard, edited marker and timestamped original/revisions; project latest accepted text into replies/search/copy/speech. |
| C039 · Long-message reader and full-screen composer | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Prototype expands composer but lacks full message reader and per-chat Collapse long messages. Preserve draft/selection/reply during expand/collapse; reader shares actions and markdown. |
| C040 · Select a text passage and act on it | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add selection with Copy and speech from selected passage; preserve source offsets and accessibility. Whole-message Copy alone is insufficient. |
| C041 · Full markdown and document rendering | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Prototype inline markup covers a subset. Add structured headings, lists, quotes, code and links as supported by production parse/render model; keep authored text for copy and speech. |
| C042 · Reactions, picker and configurable quick reactions | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C043 · Delete for everyone including group moderation | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Allow current admin to remove another author’s group message; retain author-only remote delete for DM/nonadmin and local removal of tombstones. Re-check membership and role on action. |
| C044 · Batch deletion and partial-failure recovery | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add independent deletion outcomes, successful-removal counts and retry for failed items; do not replay successful destructive work. |
| C045 · Forward text/media to multiple chats | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add folder selection, per-target prepare/upload/send progress, cancellation, partial completion, blocked reasons and expiry/session-change recovery. Existing 32-message/5-chat prototype limits need explicit comparison, not silent removal. |
| C046 · Outbound share and save from message actions | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Viewer already saves/shares images/video. Extend message/file actions and multi-attachment results with truthful partial outcomes, unavailable bytes and no-handler states. |
| C047 · Message details and delivery state | Implemented; host verified | Page, search, reveal or inspect; Back/cancel retains content and prevents stale navigation | Message Details preserves content, reactions and people, adding Created/Sent/Received timestamps, Streaming, preferred local receipt, sender-claimed time beyond five seconds and authoritative expiry. Localized times name their zone; Message ID and known incoming sender public key copy exact values. Delivery facts never imply read receipts. |

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

Batches: B08, B09, B10, B11. Decisions: None. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B08 implementation evidence

B08's selected capabilities are implemented and host-verified 2026-09-04. The
[selected brief](../../../screens/conversation-history-and-reading.md#implementation-evidence)
records history/search recovery, captured/visible unread state and delivery facts,
plus the eventual production page, target, read-anchor and metadata seams.
The clean gate passed 319 unit tests, both APKs and zero lint errors; twelve new
UI cases compile. No device execution or current-build visual acceptance is claimed.
