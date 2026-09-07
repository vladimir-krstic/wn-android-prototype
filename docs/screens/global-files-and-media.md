# Files and media from main search

## Purpose and scope

Issue #19 and the user's 2026-09-07 approval add account-wide attachment browsing
inside the existing chat-list search. No separate settings destination is added.
Only the active profile's existing local fixtures are aggregated. No remote scan,
database, persisted media index, history API or background download is introduced.

## Entry, filters and navigation

Open main chat-list search and choose Photos & videos, Files, Audio or All
attachments. No text query is required. The normal result mode retains chat,
message and people search. Attachment modes set the existing typed content filter;
the Content picker can combine attachment types. Text/Links choices return to
normal message results. Ordinary HTTP links remain in message search, rather
than becoming attachment-library files.

Reuse existing chat/type, folder, sender and date filters, removable chips and
Clear All. Choices within a category are OR; categories intersect. Optional text
matches the existing authoritative message/attachment-label projection before
expanding the matching message into its eligible attachment items. MIME-typed
audio files now share the existing library classification with search.

Results are newest first with localized civil-date headings and stable chat,
message, attachment-index and gallery-frame ordering for ties. Every gallery
image is an item; identical filenames from different sources remain distinct.
Photos/videos use a responsive grid. Files and audio use full-width rows; All
attachments mixes image tiles with full-width audio/file rows in the same order.
Each item is a single clickable Material card with its chat title and time inside.
Tap any photo, video, file or audio result to open its exact source message. System
Back restores the same query, filters and scroll position. Playback and attachment
opening remain available in the conversation. There is no separate Go to Message
button or browser-owned player/viewer.

Files reuse the chat document content (icon, filename, type and size) directly
inside one result container, with no inner fill, outline or extra inset. A small
bodySmall line below reads “From: chat name · time” and wraps when necessary.
Audio reuses
the chat voice layout (play glyph, progress track and known duration), also without
an inner fill, outline or inset. Voice notes omit the repeated “Voice message”
heading; named audio files retain their filename. Every result, including photos and videos, uses the same compact, wrapping
bodySmall “From: chat name · time” metadata with onSurfaceVariant color. These are
noninteractive previews inside the result's single navigation target. Chat titles
are shown once; sender fallbacks do not duplicate them. Photo/video cards use a
square thumbnail with a tonal play badge for video and padded source metadata.
Image/video titles are not shown; their descriptions remain available to
accessibility services and search. Unavailable items retain their status label.

The query, filters and grid/list scroll use saved UI state. Explicitly closing
search clears filters. Account switching resets browser state. No content is
restored across process death beyond the existing in-memory prototype boundary.

## States and source ownership

GlobalAttachments expands GlobalSearchResults instead of creating a second
search engine. Namespaced keys include profile/chat/message/attachment/index/frame,
including when source IDs or filenames coincide across chats. Deleted/expired
messages disappear from the live projection. Unavailable items show a generic
attachment status with source context; tapping still opens the source message.
They expose no cached thumbnail, attachment label or raw ID. Live removal drops
the result, and source navigation revalidates the message before dispatch.

A short initial loading state is followed by results or the filtered empty state.
Developer tools adds Files and media scenarios: normal, partial results once,
or failure once. Retry retains filters and restores complete local results.
Query/profile/filter changes cancel stale loading work; saved completed results
do not reload or reset scroll solely because the user returns from a conversation.

Product copy: Photos & videos, Files, Audio, All attachments, Loading files and
media…, Some results could not be loaded., Couldn’t load files and media., No
files or media found, Attachment unavailable. Shared Retry and filter labels
apply. All five app locales are supplied.

## Android composition and accessibility

Native FilterChips/InputChips, existing filter pickers, Material clickable cards
and LazyVerticalGrid own focus, interaction states and traversal. An
adaptive 160 dp minimum tile width and full-span document/audio rows allow narrow,
expanded and landscape widths. Use shared 16 dp margins and 8 dp gaps. Metadata
sits inside 16 dp card padding, headings have heading semantics, loading/retry
status uses polite live regions, and controls retain native touch targets. Monochrome semantic roles and
surfaceContainerHigh/onSurface cards and the existing AMOLED outline helper
apply; file and audio previews have no inner surface treatment. Each card has a single native click
action; decorative previews do not expose nested playback/open actions. Keyboard
hides when choosing a content mode; search and Scaffold retain existing IME/system inset ownership.

## Evidence and approved differences

The baseline attachment/viewer mapping remains `docs/port/source-map.md` B13 and
`docs/screens/text-attachments-and-shared-content.md`. Cross-chat attachment
browsing is an explicitly approved extension; production issue
`marmot-protocol/whitenoise-android#2000` was still open at inspected production
`b6f99709972604ebaed00b15bc15e1eb8ce6fcaf`. The user chose main search as the
entry and approved Photos & videos / Files / Audio with the existing filters.
Production cursor/API work remains outside the local prototype.

Official sources checked 2026-09-07:
- https://developer.android.com/develop/ui/compose/components/card — single-content clickable cards and semantic container colors.
- https://developer.android.com/develop/ui/compose/lists — adaptive lazy grids, stable keys and full-row spans.
- https://developer.android.com/develop/ui/compose/state-saving — save stable UI state and scroll, not content caches.

## Observable acceptance and validation

- Blank-query browsing finds attachments across chats in the active profile.
- Mixed messages, galleries, duplicate filenames/IDs, type intersections, sender/date scope, deletion and expiry resolve deterministically.
- Tapping any result targets its exact source message; Back preserves query, filters and position.
- Empty, unavailable, loading, partial, failure and retry have explicit outcomes.
- Profile switches and live source invalidation cannot retain another source's content.
- Native focus, large text, RTL and theme behavior are covered by compiled UI regressions where applicable.

Host gate passed: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`. All 991 unit tests pass, including 10 attachment projection/filter cases and two developer-scenario ownership cases. Eight browser UI regressions cover direct card navigation and Back restoration, file/audio metadata with no nested actions, unavailable-source navigation, retry, live deletion, profile changes and narrow RTL/large-text theme layouts; they compile, and the existing content-filter empty-state regression is updated. Lint and both APK assemblies pass. Device execution and visual acceptance are not claimed.
