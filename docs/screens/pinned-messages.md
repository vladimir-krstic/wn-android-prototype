# Pinned messages

## Purpose and scope

Issue #17 and the user's 2026-09-07 approval add message pinning, a compact
conversation banner, exact-message navigation, and the full pin list. Product
state stays in memory; no protocol events, persistence, synchronization, timers,
or invented pin limit. Pins remain until explicitly unpinned. Pin order is the
order of pinning; unpinning then repinning appends the message.

## Parity and navigation

The accepted message-action baseline is mapped in `docs/port/source-map.md` to
`wn-ios-prototype@0bd7cba:docs/screens/message-actions.md` and
`Screens/Conversation/MessageActionFlowViews.swift`. Message pinning is an
explicit Android extension under #17, not a claim about that iOS baseline.
Signal/WhatsApp supply the familiar long-press Pin and compact top-banner pattern.
The latest user screenshots supersede the separate arrows, toolbar pin shortcut,
and full-screen pin destination.

Pin/Unpin appears for eligible sent, nondeleted messages. All groups require
an active admin for both pinning and unpinning, including the source author.
Direct chats allow either participant. Disabled capability and ended membership
block mutations. Members can still browse pins and jump to their source.
A full-width rectangular gray strip sits directly below the chat header. It
shows sender and one-line message text, vertical pagination at the leading edge,
and a trailing pin icon. Tapping the preview cycles to the next pin and wraps;
it does not move the transcript. Position remains available to accessibility.

The pin icon opens a native anchored dropdown in this order: Unpin, Go to
message, View all messages. Unpin respects fixture permission; Go to message is
disabled for deleted/missing sources. View all messages opens an in-chat Material
bottom sheet with actual transcript bubbles, status, and permitted Unpin controls.
Selecting an available row dismisses the sheet and targets/highlights the source
through the existing history controller. No new navigation destination or top
app-bar shortcut remains. Closing the sheet preserves drafts and viewport.

The strip disappears when no pins remain. Removing the last pin while browsing
shows the sheet's empty state until dismissed. Search and selection hide the
strip; manual scroll and composer expansion do not change its selected pin.
Current pin and sheet visibility restore with the conversation. Product mutations
remain in memory, so process-death persistence is outside the prototype.

## Android composition and accessibility

Material Surface, IconButton, the shared native dropdown, original message bubbles,
and ModalBottomSheet use semantic monochrome roles and AMOLED outlines. The strip
background spans the full window; content remains adaptive on expanded widths.
Shared 8 dp insets/gaps, labelLarge author, bodyMedium preview, and native 48 dp
pin target apply. Text is unscaled and grows the strip height with font size.
The user explicitly removed the cycling preview's pressed/ripple fill. Its
clickable semantics remain, with an outline for keyboard focus. The pin icon
and dropdown retain native interaction feedback.

The pin sheet reuses `MessageRow` through `PinnedMessageBubble` with original
colors, full text, attachments, replies, attribution, reactions and timestamps.
No focused-preview scaling or shortened list projection is applied. A read-only
interaction overlay opens the exact source; nested links/media/reactions do not
activate separately inside the pin sheet. Day context and an admin-only Unpin
button precede each bubble; sections use the shared 24 dp gap. Missing/deleted
pins retain clear status containers without cached message content.

The noninteractive pagination rail is 2 dp wide by 32 dp tall with 2 dp gaps and
rounded segment ends. A moving window of up to five segments keeps large pin
collections legible without imposing a product pin limit. The active segment
uses onSurface; inactive segments use outlineVariant. A localized state
description announces N of M. RTL mirrors the leading rail/trailing menu.
The menu and sheet own Back/dismissal and native focus behavior. Behind-sheet
messages are excluded from visible-read tracking. No new system integration.

Copy: Pin, Unpin, Go to Message, View all messages, All pinned messages,
No pinned messages, Message deleted, Message unavailable. Deleted/unavailable
sources never reveal cached message content.

## Observable states and acceptance

- Multiple messages pin/unpin in stable order without affecting other chats or profiles.
- Existing source edits appear in previews; deletion and removal retain an explicit unavailable pin.
- Empty list, denied mutation, member/admin fixtures, full source, and missing source are deterministic.
- Maya Chen starts with two pins. Group - Member has an admin-only available pin and an unavailable pin.
- Exact target jumps use existing history loading/failure/retry and highlight behavior.
- All-pins Back, targeted return, font scaling, RTL, theme roles, and accessibility controls are covered by host checks / compiled UI regressions.

## Official sources

- https://developer.android.com/develop/ui/compose/components/menu — native anchored action menu.
- https://github.com/google/material-design-icons/blob/master/symbols/android/format_list_bulleted/materialsymbolsrounded/format_list_bulleted_24px.xml — native rounded list icon.
- https://developer.android.com/develop/ui/compose/components/bottom-sheets — modal versus separate destination; all-pins uses a modal sheet.
- https://support.signal.org/hc/en-us/articles/10270961459226-Signal-Pinned-Messages — familiar pin and banner pattern.
- https://faq.whatsapp.com/294619079641794/ — familiar message pinning pattern.

## Validation

Full-bubble/admin-only host gate passed: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`. All 968 unit tests pass, including ordinary-group member rejection and demotion blocking Unpin. Lint and both APK assemblies pass. Eight pin UI regressions compile, including real photo/text bubble content and metadata. No device execution or visual verification is claimed.
