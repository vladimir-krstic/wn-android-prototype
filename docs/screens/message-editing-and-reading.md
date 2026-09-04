# Message editing, full reader and text selection — B09

Selected 2026-09-04 under the all-batches goal. C038–C041.
Implemented and host-verified; device/visual acceptance remains pending.

## Product contract

Own, nondeleted authored messages in a writable chat offer **Edit**. Editing
starts with the current accepted text in a dedicated editor, preserves the
ordinary composer draft/reply/attachments, and offers **Save** and **Cancel**.
Blank or unchanged text cannot submit. Back cancels the edit draft. Pending
edits show the submitted body and **Saving edit…**; failed edits restore the
accepted body and offer **Retry edit** and **Discard edit**. A newer attempt,
discard, deleted target or changed profile cannot be overwritten by stale work.
Accepted edits append one timestamped revision while retaining the exact original
body. **Edited** opens **Edit history**, newest first, including **Original**.
Replies, both searches, copy, forwarding and speech derive from accepted text.
Keep the established plain-text **Copy** and add **Copy Markdown** when formatting
is present, preserving the complete authored source including link destinations.
Attachments and the original send time remain unchanged.

**Open message** opens a full reader for authored text, including long messages,
captions and structured Markdown. **Collapse long messages** is a profile/chat-
owned preference in Chat/Group Info, initially on. Long collapsed bubbles have
an explicit **Read more** affordance; the reader shows the complete document.
Its menu reuses eligible reply, reaction, copy, edit, history, selection, speech,
forward, details and delete actions. Back first exits text selection, then the
reader. Missing/deleted sources dismiss or show an unavailable state; profile
changes cannot leave another profile's text displayed. The existing full-height
composer expansion retains text selection, draft, reply and attachments through
expand/collapse and Back.

**Select text** enters native passage selection. Native handles, keyboard and
selection semantics own the range; **Copy** copies the selected passage and
**Read Aloud** starts the existing speech controller at that source range.
Repeated words, reversed selection, emoji and Markdown presentation must not
lose the source position. Selecting a passage is distinct from selecting whole
messages for bulk actions. Selection resets when its source revision changes.

Render the production document shapes: paragraphs, six heading levels, ordered/
unordered/task lists, quotes, thematic breaks, fenced/indented code, tables,
disclosures, and raw math source. Inline presentation includes nested emphasis,
strong, strike, code, named/bare links and image alt text. Preserve hard/soft
breaks, escaped delimiters, unknown/incomplete syntax and original authored text.
Images never trigger remote loading. Unsupported link schemes stay inert;
named links reveal their actual destination before external opening. Existing
person mentions remain connected. Encoded Nostr profile/event references retain
their authored text; their resolution, profile presentation and rich cards are
implemented and host-verified in B30 alongside referenced article/video readers.
Document and inline projections retain source offsets for selection/speech and
search; rendering limits must retain a route to the complete authored content.

## Android composition and state

Retain the accepted monochrome transcript, bubble metrics, focused action menu,
compact composer and search. Use Material app bars, native text input/selection,
the shared adaptive pane and 4/8 dp rhythm. The task editor and reader have one
owner for system insets/IME and remain usable at large type and expanded widths.
Markdown uses semantic Material typography and native text/link annotations;
headings, task state, disclosure state, editing progress and failures have named
semantics. Code and tables scroll horizontally within their content region.
No third-party runtime parser, HTML/WebView, network, permission or new service.

Developer-only controls provide one-shot edit failure/unavailable outcomes and
authored examples for long documents, revisions and selection. Ordinary surfaces
contain production-ready copy. Real publishing, authenticated edit aggregation,
parser AST delivery and durable draft/history storage are future integration seams.

## Evidence

Production master pinned at `319454889f1c2494dec4a69b5577d98017f44eee`:
`core/MessageEdits.kt`; `state/Controllers.kt` edit/pending/retry handling;
`ui/conversation/messages/MessageBubble.kt`, `EditHistory.kt`,
`MessageFullScreen.kt`, `MessageTextSelection.kt`, `ReaderTextSelection.kt`,
`MessageTextSelectionToolbar.kt`; `ui/conversation/composer/ComposerExpansion.kt`;
`ui/MarkdownRenderer.kt`, `MarkdownParsing.kt`, and `core/MarkdownDocuments.kt`.
The production [F05 matrix](../audits/production-android-parity/flows/F05-message-lifecycle.md)
owns capability scope. Existing [message interactions](message-interactions-and-search.md),
[composer](composer-media-and-speech.md), [shared conversation](shared-conversation-core.md)
and [B08 reading](conversation-history-and-reading.md) retain local presentation authority.

Current official guidance checked 2026-09-04:
[text input](https://developer.android.com/develop/ui/compose/text/user-input),
[selection and links](https://developer.android.com/develop/ui/compose/text/user-interactions),
[SelectionState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/selection/SelectionState).

## Acceptance

Verify edit eligibility, nonblank/changed save, cancellation, optimistic/pending,
failure/retry/discard, original/revision ordering, stale completion and profile/
chat ownership. Prove accepted edits propagate to replies, search, copy, forwarding
and speech. Verify complete reading, per-chat collapse, source disappearance,
shared actions, range/source mapping and native selection Back behavior. Cover
the named Markdown forms, literal fallback, safe links and no remote image loads.
Preserve composer expansion state and existing focused interactions. Run meaningful
unit tests and the complete README host gate; compile durable UI cases. Device,
speech output and visual acceptance remain pending until explicitly requested.

## Implementation evidence

- `MessageEditingModels.kt` and `AppViewModel.kt` preserve accepted text separately
  from the pending edit, guard profile/chat/author/revision/request identity, and
  append timestamped original/revision history only after acceptance. Retry,
  discard, deletion, lost write access and profile/foreground changes invalidate
  stale work. Forwarded copies carry accepted content without the source's edit
  history or transport timestamps. Replies, search, copy and speech read the same
  accepted body; a revised/deleted speech source stops its current utterance.
- `MessageReadingUi.kt` supplies a native full-screen task editor, revision history
  and reader. The editor retains its own `TextFieldValue` and leaves the ordinary
  composer state intact. The reader reuses eligible message actions; selection
  consumes Back before dismissal. `ChatInfoScreen` owns the per-chat collapse
  switch. Collapsed bubbles measure their actual content against the production
  52-line, font-scaled limit and expose Read more when needed.
- `MessageDocument.kt` parses the named block/inline forms with exact authored
  source retained and literal fallback for unrecognized syntax. It is a local
  presentation parser, not a replacement for the production Marmot AST. Structured
  text uses Material typography, native links, scrollable code/tables, heading/task/
  disclosure semantics, and no remote image fetching. Existing plain Copy remains;
  Copy Markdown retains the complete authored source and link destinations.
- `MessageDocumentUi.kt` annotates each displayed UTF-16 unit with its source span.
  Native Compose `SelectionState` supplies selected fragments; slicing retains
  those annotations, including repeated words and decoded entities. The reader
  offers selected Copy and Read Aloud, with an appended native speech context-menu
  action. `ReadAloudController` assigns each utterance a new identity so callbacks
  from an earlier passage cannot change a later passage from the same message.
- `MessageEditingStateTest` has nine state/ownership regressions;
  `MessageDocumentTest` has eleven document/link/source-range regressions;
  `MessageSelectionTest` has four native-annotation bridge regressions.
  Ten new `MessageEditingReadingFlowTest` UI cases compile: save/history,
  failure/retry/discard/cancel, editor restoration, complete reading, per-chat
  collapse, native passage long-press/copy/Back, named-link destination disclosure,
  source mutation/deletion, and composer expansion preserving draft/selection/reply.
- The clean README host gate passed **343 unit tests**, zero failures/errors/skips,
  zero lint errors and both APKs, with the same 14 pre-existing warnings. The first
  gate caught an incorrect test-harness custom-action invocation; the tests now
  call the actual semantics action and use native long-press for passage selection.
  UI cases were compiled only. Device behavior, spoken audio and visual acceptance
  remain unverified until the user requests inspection.

Commit title: `B09: Add message editing and full reading`.

### B15 speech extension

The [Read Aloud transport brief](read-aloud-transport.md#implementation-evidence)
now governs the shared engine, pause/resume, sentence/message controls and
source-aware seeking/following. Original copy/selection and file-reader source,
dismissal and background boundaries remain. B15's 465-test host gate passes;
15 new UI/platform cases compile without device execution.
