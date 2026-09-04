# Text attachments and Shared Content — B13

## Purpose and selected scope

Implement C055/C056/C059 under the authorized B01–B32 goal. Read local text and
Markdown files, browse separate image/video/voice content with useful metadata,
and explain package-open outcomes without installing an app. B11 provides local
file materialization and explicit save destinations; B12 is committed.

## Product contract

- Open text/Markdown from message attachments and Shared Content Documents.
  Show filename, MIME/size, loading, empty, unavailable/retry, invalid encoding,
  binary content and over-limit outcomes. Metadata proposes a text preview;
  strict byte decoding is authoritative. Accept UTF-8 and BOM-tagged UTF-16.
  Never allocate decoded text above 512 KiB of source bytes.
- Render bounded Markdown through the established document renderer, with a
  visible truncation notice and external-open fallback when formatting reaches
  its budget. Plain text uses the same visible preview budget to bound layout; full original text remains available to Copy. Native selection
  supports passage actions; full filenames are selectable/copyable.
- Read Aloud uses the existing local Android speech engine and complete bounded
  speech chunks, with stop/progress/error handling. Stop on dismissal, background,
  source removal/change and profile change. No recognition or network speech.
- Open other files through ACTION_VIEW and the existing FileProvider, preserving
  actual MIME and temporary read grants. Report unavailable bytes, no handler,
  user dismissal and access errors; never substitute unrelated sample documents.
  Back/cancel/source changes invalidate pending loads and external dispatch.
- Retain Chat Info's disclosure-row hierarchy. Photos & Videos gains All,
  Images and Videos filters; Voice is an additional disclosure destination.
  All library content is newest first, grouped by localized month, with stable
  source/frame keys and sender/time/type/size fallback metadata. Keep every
  album frame, unavailable rows, empty categories and Go to Message.
- Reuse the full chronological media viewer for exact-frame opening, forwarding,
  save/share and source navigation. Removed/deleted/expired sources disappear
  from active projections instead of remaining in a captured viewer snapshot.
- Voice/audio rows offer one foreground playback session, play/pause/seek,
  loading/error/retry and a distinct Go to Message action. Use actual local
  content or an explicitly authored bundled audio example; unknown audio never
  becomes an unrelated sample. Pause/release on background/route/profile change.
- Package metadata and archive shape distinguish Android app files from ordinary
  files, with valid/invalid, permission-required, installer-unavailable and Play
  distribution fallback states. Q06 authorizes local state coverage only: no
  installation intent, install permission, installer, or settings permission grant.
  Offer existing explicit file saving where useful; never claim installation.

## Android composition and copy

Use current Material app bars, shared dialogs/rows, filter chips, lazy lists and
adaptive grids. Text reading is a full-screen app-owned dialog with a selectable
body, overflow actions and accessible progress/errors. Keep neutral semantic
surfaces and shared 4/8 dp spacing; system file destinations/viewers keep their
own appearance. Controls wrap/scroll with enlarged text and RTL. Keep source
coordinates and stable identities independent of displayed ordering.

Ordinary labels include “Images”, “Videos”, “Voice”, “Open in another app”,
“Copy text”, “Read Aloud”, “View full filename”, “This file is empty”, “Retry”,
“Save file” and specific file/encoding/installation-unavailable explanations.
Developer-only examples/outcome selectors remain outside product copy.

## Sources and integration boundary

Pinned production master `319454889f1c2494dec4a69b5577d98017f44eee`:
TextAttachmentPreviewPolicy/ReaderState/Reader/Fallback, MediaFileAccess/MediaIo
and MediaLibrary. The local accepted hierarchy is in
[chat-and-group-information.md](chat-and-group-information.md); rendering,
selection and device-owned media behavior are in
[message-editing-and-reading.md](message-editing-and-reading.md) and
[composer-attachment-actions.md](composer-attachment-actions.md).

Production reconnects authoritative attachment references/cache/transport,
voice sources and distribution/installer capabilities. The prototype remains
in-memory, offline, permissionless for this batch and without services.

Current official guidance opened 2026-09-04:
- [Lazy lists and grids](https://developer.android.com/develop/ui/compose/lists).
- [Text interaction and selection](https://developer.android.com/develop/ui/compose/text/user-interactions).
- [Android speech voice network capability](https://developer.android.com/reference/android/speech/tts/Voice#isNetworkConnectionRequired%28%29).
- [Secure file sharing](https://developer.android.com/training/secure-file-sharing/share-file).

## Acceptance and evidence

Test candidate/decoder/budget rules, package classification, ordered/category/month
projections and stale/source ownership. Compile reader, filename, retry, filter,
voice and package UI/platform cases. Run the complete host gate before committing
B13. No device/emulator execution or visual acceptance is authorized.

## Implementation evidence

Implemented and host-verified 2026-09-04; device and visual acceptance pending.

- `AttachmentReadingModels.kt` and `AttachmentSources.kt` use metadata candidate
  rules followed by strict UTF-8/BOM UTF-16 decoding. Reads stop after 512 KiB
  plus one sentinel byte; oversized bytes, invalid encoding and binary content
  have distinct outcomes. Empty files remain valid. The rendered preview is
  capped at 64 Ki characters or 2,048 lines, with visible truncation and full
  original Copy. Markdown reuses the accepted source-aware document renderer.
- `AttachmentReaderScope` owns stable profile/chat/message/attachment IDs and
  invalidates changed, deleted and expired sources. The reader has full filename
  copy, native text selection, loading/retry, native speech progress/stop/error,
  explicit file saving and external-open errors. Metadata and body scroll
  together; Copy/Read Aloud remain below, while Open/Save use the app-bar menu.
  Source loss also dismisses an open save sheet and restores Back handling.
- `ReadAloudController` queues lossless engine-sized chunks with generation-safe
  callbacks. Reader and message speech do not overlap; background/dismissal and
  profile/source changes stop the owned reader. Android engine/voice availability
  remains a device check, with visible unavailable/retry behavior. Only installed
  voices declaring no network requirement are selected; unavailable language or
  local voice data never falls back to network speech.
- `SharedContentProjection` groups newest-first content by localized month,
  separates Voice/audio from Documents, includes body URLs without previews and
  every album frame, and retains unavailable rows with Go to Message. Media
  filters are All/Images/Videos. The shared and conversation viewer refresh from
  live source projections; save/share validate the source, and GIF export keeps
  animation bytes. Shared audio uses one foreground Media3 session with revision,
  source-change and lifecycle guards, play/pause/seek, loading/failure/retry and
  a separate message jump. Unknown audio never becomes the bundled example.
- Package outcomes combine MIME/filename classification and binary-manifest ZIP
  shape checks. Ordinary builds show installation-unavailable saving; developer
  states exercise permission-required, no installer and ready-for-review copy.
  These are app-owned results, not signature verification or an installed app.
  Every package dispatch is blocked, including provider-refined package MIME.
  Q06 still reserves real installation/permissions for production integration.
- Developer Tools adds explicit text/encoding/size/audio/package examples and
  one-shot, profile-owned file-open scenarios. Ordinary drafts are preserved.
  A four-second bundled audio cue is authored test content, not a voice recording.

`./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
and a follow-up gate after the installed-local-voice guard pass: **414 unit tests**, no failures/errors/skips, both APKs, zero lint errors
and the same 14 pre-existing warnings plus two hints. `AttachmentReadingModelsTest`
adds 13 decoder/projection/source/speech/package rules; `AttachmentReadingStateTest`
adds two developer ownership/draft tests. Existing media ordering/GIF expectations
are updated to the full source projection.

`AttachmentReadingTest` adds **13 compiled-only** UI/platform cases covering
reader/full-name copy, retry/empty, source/profile revocation, truncation/full
copy, native stream bounds, package shape/dispatch and permission fallback,
media filtering/unavailable source navigation, audio failure/stale load/seek/
source replacement, byte-preserving GIF export and a narrow 200% RTL reader.
Existing gallery cases now scroll to their exact frame and assert all 40 items.
These cases have not run on a device; no playback, speech, system surface,
accessibility, layout or visual acceptance is claimed from compilation.
