# Timestamps inside message bubbles

## Scope and behavior

2026-09-08 user direction supersedes the earlier outside-bubble timestamp decision.
Keep existing timestamp visibility (cluster endings and messages with retention),
text, delivery states and retry behavior. Move the time, delivery and retention
indicators inside the right edge of both incoming and outgoing bubbles.

Short text shares its last baseline with the footer. Wrapped text uses available
space after the last line, with an 8 dp gap. If the last line cannot accommodate
the measured footer, place it on a separate line inside the bubble with a 4 dp
gap. Do not shrink the text or insert spacer characters into its authored content.
Final paragraphs/headings retain formatting, links, mentions and native selection.
Bidi text uses its measured occupied edge and moves the footer below when needed.

Attachment captions share the text rule. Attachment-only messages, structured
blocks, location/agent cards, translation/edit controls and read-aloud progress
use a trailing footer inside their container. Long-message Read more remains
above the footer. Search results and pinned/focused previews reuse the renderer.
Reactions remain outside, overlapping the bubble; their target reserve no longer
includes timestamp height. Existing grouping and composer behavior are unchanged.

## Native composition and accessibility

Material Text supplies real TextLayoutResult line geometry and the status row's
baseline supplies vertical alignment. A small Compose Layout combines them because
Row/FlowRow cannot flow a footer into a paragraph's last line. Intrinsic width
supports forwarded labels, multiple blocks and the reaction minimum bubble width.

Existing 12 dp horizontal / 8 dp text padding remains. Rich cards keep their
existing insets. Time and delivery use a neutral gray chosen against the actual
bubble color at at least 4.5:1 contrast, including custom colors and dark/AMOLED.
Retention uses the same gray; failures retain their semantic error presentation.
Footer content is excluded from text selection. Native links, accessibility
labels, retry actions, keyboard behavior, Back and system surfaces are retained.
No permissions, networking, authentication or storage changes.

## Evidence

- Local baseline: shared-conversation-core.md and message-editing-and-reading.md;
  docs/port/source-map.md conversation entries. Explicit user direction changes
  the timestamp placement without expanding the pinned iOS scope.
- Signal interaction reference: https://github.com/signalapp/Signal-Android/blob/main/app/src/main/java/org/thoughtcrime/securesms/conversation/ConversationItem.java
  Signal checks the last text line against footer width and spacing, collapsing
  its footer only when it fits. Used as behavior evidence, not imported code.
- Compose measurement: https://developer.android.com/develop/ui/compose/layouts/custom
- Text layout: https://developer.android.com/reference/kotlin/androidx/compose/ui/text/TextLayoutResult

## Validation

InlineFooterGeometryTest covers short, wrapped, overflowing, RTL, large-type /
long-footer and externally constrained widths. InlineTimestampLayoutTest covers
real Compose text baselines and formatted document footer placement. Existing
conversation tests now expect inside-right timestamps, including reactions and
200% type. Host gate passed: testDebugUnitTest, lintDebug, assembleDebug and
assembleDebugAndroidTest. Instrumentation tests are compiled only;
current-build device testing and visual acceptance are not claimed.
