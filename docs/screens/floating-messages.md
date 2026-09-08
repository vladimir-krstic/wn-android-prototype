# Floating messages

## Purpose and scope

Keep personal reference messages visible while moving between conversations and
the chat list. This is a user-approved Android extension, separate from shared
chat pins. It adds no notification bubble, system PiP, overlay permission, backend,
or persistent storage.

## Behavior and navigation

- A message's context menu and accessibility actions offer **Keep on screen**.
  Group admin permission is not required; normal pin permissions remain unchanged.
- One compact, rounded gray card holds an ordered stack. Adding a new reference
  selects it; adding an existing reference selects it without duplication or reordering.
- Swipe the content horizontally to page in both collapsed and expanded states,
  wrapping from last to first or first to last.
  Taps do not cycle messages.
  The left pagination marks reuse the chat-pin indicator, appear only for multiple
  messages, and sit beside the message, aligned to its top outside the pager and
  vertical scrolling area. Long press anywhere on
  the card background, header or preview toggles expansion of the same floating card. There is
  no expand button and no modal sheet.
- Expanded presentation shows the message content directly on the card surface,
  including formatted text and attachments, without a nested chat bubble. Long
  messages scroll vertically in a region bounded to 480 dp or the available height.
  Sender and source/time metadata appear only when expanded. Body text stays
  at the same bodyMedium size as the compact preview. Horizontal swipes change messages.
  Long press, the inward-arrow button and system Back collapse
  the card. The card remains draggable, and the surrounding app remains usable.
- The menu opens the source, removes the current reference, or clears the active
  account's stack. Removing the last reference dismisses the overlay. Opening the
  source uses the existing conversation target route and retains the stack.
- The full title row drags the card in either state; there is no separate handle.
  The menu precedes the inward-arrow collapse button at the trailing edge.
  Menu actions also move the card to the top or bottom without dragging.
- The card is shown only over conversations and the main chat list. It hides during
  account selection, app lock/background, full composer/attachment presentation,
  and message context menus. It does not follow the user outside this app.

## State and ownership

`FloatingMessageController` belongs to `AppViewModel`. Per-profile stacks hold
only profile/chat/message IDs and selected ID. Live timeline resolution reflects
edits and rejects deleted, expired, missing, or signed-out sources. Switching
accounts exposes only that account's stack; sign-out/wipe prunes references.
Clearing/removing changes only the personal stack, never the original message or
shared pins. In-memory stacks survive Activity recreation through the ViewModel;
process death clears them with the rest of this prototype's in-memory state.

## Android composition and accessibility

Use Material Surface, IconButton and expressive dropdown menus. Compose
HorizontalPager owns paging. `ReadOnlyMessageContent` reuses the existing message
text and attachment components without the directional bubble surface. The card
has no tap/press visual indication, per the user correction. Its menu and collapse
buttons retain their own standard control feedback. A custom navigation-level overlay and a dedicated drag gesture are needed
to satisfy the approved cross-screen positioning; a system video PiP window does
not fit this reference-message interaction.

The card fills the viewport with 24 dp margins on both sides (the standard chat
margin plus half), normal 48 dp icon touch targets, 16 dp content insets and 8 dp
related spacing. Its top position is based on the viewport, not message height;
paging does not recenter pagination vertically. Boundary clamping still applies. It has the semantic gray surface and 6 dp shadow,
plus the shared outline under AMOLED. It clamps to the available viewport inside
safe drawing/keyboard insets, below the toolbar and above composer/FAB clearance.
Short viewports retain the header with its long-press action and menu. Text uses theme type
and overflow; expanded content scrolls. Native pager supports RTL. Positioning
uses physical coordinates so dragging follows the pointer in either direction.
TalkBack gets position, pager/expand actions and non-gesture movement alternatives.

## Product copy

Strings use `floating_*` in `values/strings.xml`: “Keep on screen”, “Kept on screen”,
“Expand message”, “Next kept message”, “Remove this message”, “Clear all kept
messages”, and `From: [chat] · [time]`. Source navigation reuses “Go to Message”.

## Evidence and governing sources

- User authorization in this task is the feature scope; no upstream floating-stack
  capability is claimed. Shared message/pin foundations are mapped in
  `docs/port/source-map.md` and `docs/screens/conversation-history-and-reading.md`.
- https://developer.android.com/develop/ui/compose/layouts/pager — native paging.
- https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling
  — dedicated drag handling.
- https://developer.android.com/design/ui/mobile/guides/home-screen/picture-in-picture
  — system PiP is not the presentation used for this flow.

## Acceptance and validation

Host unit tests cover order, deduplication, removal selection, account isolation,
live edits, deletion/expiry/missing sources, sign-out cleanup and unchanged shared
pins. `FloatingMessageFlowTest` covers the menu entry, cross-screen presence,
source navigation, swipe-only cycling and inert taps, in-card expansion/collapse, fixed pagination,
single-message pagination omission, width and removal/clear.
Instrumentation is compiled only. Visual, gesture and accessibility acceptance on
a device remains pending the user's hands-on review; no device inspection is claimed.

Host verification (2026-09-08): `./gradlew --no-daemon testDebugUnitTest lintDebug
assembleDebug assembleDebugAndroidTest` passed on the final source, with 1,067 unit
tests and zero failures/errors. Both debug APKs assembled. The final run used a
fresh Gradle process after an earlier lint scanner stalled. Log:
`/tmp/wn-floating-clean-gate.log`. No connected tests or device inspection ran.

## Follow-up acceptance — 2026-09-08

The user superseded the initial sheet presentation with in-card expansion, direct
message content, full-card long-press feedback, a fixed top pagination header shown
only for multiple messages, and 24 dp outer margins. The expanded card wraps short
content and bounds long scrolling content. Validation below will record this batch;
the preceding gate above covers the initial implementation.

Gesture source: https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press
— shared `combinedClickable` interactions and Material indication.

Follow-up host verification: `./gradlew --no-daemon testDebugUnitTest lintDebug
assembleDebug assembleDebugAndroidTest` passed in 2m42s, with 1,067 unit tests,
zero failures/errors, lint passing and both debug APKs assembled. The updated UI
checks compiled only. Log: `/tmp/wn-floating-inline-gate.log`. Device execution
and visual acceptance remain pending.

## Pagination and gesture correction — 2026-09-08

Pagination is a stationary side rail aligned to the top of the message, not part
of the toolbar. It remains outside scrolling/paging content and is hidden for one
message. Taps on header or content cycle in both states; long press toggles expand
and collapse. Content taps are intercepted on the initial pointer pass so image
and link children cannot swallow cycling, while drags still reach scrolling and
the pager. The shared whole-card press overlay is removed. Standard keyboard and
accessibility actions remain on `combinedClickable`. Existing X/Back still collapse.

Correction host gate: `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest` passed in 2m35s. Updated interaction checks compiled;
no device tests ran. Log: `/tmp/wn-floating-gestures-gate.log`.

## Header and paging correction — 2026-09-08

Latest user direction supersedes tap cycling: only horizontal content swipes
change the selected message. Short content taps remain inert and long press
still toggles expansion. The whole header owns drag movement, with no separate
handle; menu and collapse buttons retain their normal click actions. Collapsed
previews omit the sender, while expanded content shows it. The menu sits before
the trailing Material close_fullscreen inward-arrow collapse control. There is
no card press effect. Existing side pagination stays aligned to the content top.

Interaction checks now cover inert taps in both states, swipe selection, sender
visibility, header drag movement, action order and explicit collapse.

Header correction host gate: `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` passed. All 1,067 unit tests passed; lint and both APK builds passed. Updated interaction tests compiled only. Log: `/tmp/wn-floating-header-gate.log`. No device inspection or connected tests ran.

## Compact typography and wraparound — 2026-09-08

The latest correction keeps body text at bodyMedium in both presentations and
omits all sender/source/time metadata from the collapsed preview. Expanded
content retains formatting with a floating-card-only body style override.
Native HorizontalPager uses one copy at each boundary and recenters onto the
same message once scrolling settles. Swipes therefore wrap in either direction,
while pagination and controller selection still reflect actual messages. A
single message has one non-scrollable page. Membership changes rebuild this
mapping using the selected live reference, preserving deletion/account behavior.

`FloatingMessagePages` unit tests cover both boundary directions, identical
content after recentering, and the single-message case. Instrumentation contracts
cover wrapping in both presentations, metadata visibility and equal body font
sizes. Governing source: https://developer.android.com/develop/ui/compose/layouts/pager
(native paging and settled-page observation).

Wraparound host gate: `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` passed. All 1069 unit tests passed, lint passed, and both APKs assembled. Updated instrumentation tests compiled only; no device inspection ran. Log: `/tmp/wn-floating-wrap-gate.log`.
