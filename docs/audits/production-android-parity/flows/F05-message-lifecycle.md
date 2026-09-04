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
| C038 · Edit own message, retry/discard and revision history | Implemented; host verified | Edit/read/select; Back cancels the editor or clears selection before closing | Own writable messages offer nonblank changed edits, cancel, pending body, failed retry/discard and timestamped original/revision history. Accepted text drives replies/search/copy/forward/speech. Stale attempts, revisions, deleted targets and profile/foreground changes cannot apply old work; the ordinary composer draft/reply/attachments remain intact. |
| C039 · Long-message reader and full-screen composer | Implemented; host verified | Edit/read/select; Back cancels the editor or clears selection before closing | Open message and long-bubble Read more expose the complete structured body with shared actions. Chat/Group Info owns a profile/chat collapse switch; the preview limit scales with body typography. Back clears selection before closing; missing/deleted sources close. Existing composer expansion retains draft, selection and reply. |
| C040 · Select a text passage and act on it | Implemented; host verified | Edit/read/select; Back cancels the editor or clears selection before closing | Native SelectionState owns passage handles and selected fragments. Per-character source annotations preserve repeated-word/entity positions across formatted leaves. Copy and Read Aloud act on the selected passage; source edits clear selection. Unique utterance identities reject stale same-message speech callbacks. |
| C041 · Full markdown and document rendering | Implemented; host verified | Edit/read/select; Back cancels the editor or clears selection before closing | Structured paragraphs, six headings, lists/tasks, quotes, rules, fenced/indented code, raw math, tables and disclosures share source-aware inline formatting, safe native links and image alt text. Unknown syntax remains visible; no remote image fetch. Plain Copy and Copy Markdown preserve readable and authored content. Encoded Nostr reference resolution/cards remain tracked in B30. |
| C042 · Reactions, picker and configurable quick reactions | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C043 · Delete for everyone including group moderation | Implemented; host verified | Confirm mixed deletion or choose forward destinations; close details independently of cancellation | Active group admins can remove other authors’ messages; direct/nonadmin remote deletion remains author-only. Each mutation rechecks membership, role and publication availability. Deleted rows expose local cleanup only. Everyone deletion clears content, attachments, reactions, reply/edit payload while retaining the tombstone. |
| C044 · Batch deletion and partial-failure recovery | Implemented; host verified | Confirm mixed deletion or choose forward destinations; close details independently of cancellation | Mixed confirmation names everyone versus device-only counts and freezes each item’s operation. Independent outcomes preserve successes; only failed items retry and keep recoverable selection. Lost role/permission never silently changes scope. Sanitized reports omit content/identity, and profile/session changes reject stale work. |
| C045 · Forward text/media to multiple chats | Implemented; host verified | Confirm mixed deletion or choose forward destinations; close details independently of cancellation | Conversation and Shared Content forward complete source-ordered accepted text/media through folder and signed-in destination-profile selection, with unavailable reasons and no legacy 32/5 count caps. App-owned preparation/upload/send state survives navigation, preserves accepted destination/message counts through retry, and permits cancellation only before publishing. Transient failures retry at 1/2/4 seconds, with timeout/manual and terminal expiry/session/source recovery. Wiping a profile clears retained payload. |
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

## B09 implementation evidence

C038–C041's editing, reader, selection and structured-document contracts are
implemented and host-verified. The [selected brief](../../../screens/message-editing-and-reading.md#implementation-evidence)
records accepted versus pending text, original/revision history, owner/request
cancellation, native selection and shared document/source projections. The clean
gate passed 343 unit tests, both APKs and zero lint errors; ten new UI cases
compile. Encoded Nostr reference resolution/profile presentation and rich cards
are explicitly tracked in B30. No device or current-build visual acceptance is claimed.

## B10 implementation evidence

C043–C045 are implemented and host-verified. The [selected brief](../../../screens/message-moderation-and-forwarding.md#implementation-evidence)
records active-admin moderation, frozen everyone/local batch outcomes, failed-only
retry and app-owned forwarding with folders/destination profiles, progress,
cancellation, automatic retry and stale/expired session protection. The source
comparison replaces the legacy 32-message/five-chat caps with explicit eligibility.
The clean gate passed 361 unit tests, both APKs and zero lint errors; nine new
UI cases compile. No current-build device or visual acceptance is claimed.
