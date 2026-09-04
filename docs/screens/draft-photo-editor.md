# Draft photo editor — B12

## Purpose and scope

Edit a selected draft photo without losing its original image, attachment identity,
other attachments, caption or reply. The user authorized all production parity
batches and a verified commit per batch; B12 implements C050 after B11.
GIF/video editing, networking, persistence, new permissions and device execution
remain outside this batch.

## Parity contract

- Enter from the selected photo in the existing draft preview; preserve review
  inclusion state. Edit the exact attachment/frame, including album frames.
- Offer Free, Original, Square, 4:3, 3:4, 16:9 and 9:16 crop presets, free crop
  adjustment and clockwise quarter turns. Crop and ink coordinates remain tied
  to the oriented original image through all transformations.
- Draw or erase with Small/Medium/Large/Extra large widths and red, yellow, green,
  blue and white annotation colors. Erasing clears annotation pixels only.
  These are literal image-editing values; app chrome remains monochrome.
- Keep undo/redo, reset to the recipe present when the editor opened, dirty
  discard confirmation, loading, saving, retry and unavailable/invalid/limit
  outcomes. Do not mutate the draft until a complete successful save.
- Preserve original image data and accepted edit recipes so subsequent editing
  or B11 quality changes never discard prior edits or accumulate recompression.
  Original with edits uses a visibly identified High-quality render; unchanged
  Original may preserve its sanitized encoded image. Retain the accepted High
  default and show the actual quality and encoded size; retain actual rendered
  pixel dimensions in attachment metadata. Per-frame quality overrides leave
  sibling album frames intact; a whole-draft quality choice reprocesses every
  frame from its original source and replays accepted edits.
- Match source limits: 50 undo/redo states, 256 strokes, 2,048 points per stroke,
  100,000 total points, 32,768 px source edge, 200 MP source pixels and 100:1
  aspect ratio. Existing 32 MiB input bound applies. Preview is bounded to 1,536 px;
  rendered edits are bounded to 4,096 px, 12 MP and 32 MiB. Reject or explain
  limits rather than silently claiming a complete edit.

## Navigation, composition and copy

Use an app-owned full-screen Material dialog matching the current media preview,
with native top-app-bar actions, semantic surfaces, shared margins and scrollable
tool controls. A custom canvas is required because Compose/Material has no
ready-made crop-and-annotation editor; ordinary controls remain native.

Labels include “Edit photo”, “Crop”, “Draw”, “Erase”, “Rotate clockwise”, “Undo”,
“Redo”, “Reset”, “Save”, “Saving…”, “Discard changes?” and “Keep editing”.
System Back follows the same dirty check as Close; a cancelled confirmation
returns to the editor. Saving is guarded against duplicate actions. Failures
retain the recipe for Retry. Closing/discarding changes no ordinary draft data.

## State, accessibility and adaptation

The editor session belongs to profile/chat/attachment/frame and has a request
generation and immutable history. Reject stale load/render completion, removed
attachments, changed sources and profile/session changes. Keep accepted recipes
and image bytes in memory across activity recreation; do not save them to disk.

Provide named selected tools/colors/widths, live action/limit announcements,
minimum touch targets and non-color selection cues. Crop has native numeric
adjustments as an alternative to handles, and drawing has a coordinate-based
line action for keyboard/accessibility input. Keep source coordinates separate
from RTL layout. Controls scroll at large font sizes and landscape widths;
canvas and app bar respect system/IME insets.

## Evidence and governing sources

Production is read-only at `319454889f1c2494dec4a69b5577d98017f44eee`:
`ui/conversation/media/editor/PhotoEditorScreen.kt`, `PhotoEditorState.kt`,
`ui/conversation/ConversationMediaDraft.kt`, and `media/editor/PhotoEditRecipe.kt`,
`PhotoEditTransform.kt`, `PhotoEditorRenderer.kt`, `PhotoEditorCommitter.kt`.
B11's local source/quality policy is in `composer-attachment-actions.md` and
`docs/port/source-map.md`; the established draft-preview presentation is in
`composer-media-and-speech.md`. No new iOS capability is inferred.

Current official guidance consulted 2026-09-04:
- [Compose graphics](https://developer.android.com/develop/ui/compose/graphics/draw/overview): canvas transforms and bounded image rendering.
- [Pointer input](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling): crop and drawing gesture ownership.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): accessible control state and alternative actions.

## Acceptance and validation

Verify crop ratios and inverse coordinate mapping through every quarter turn,
history/limits, erased-layer composition, source/quality reversibility, exact
frame ownership, cancellation and stale save rejection. Add meaningful host
tests and compile durable UI/rendering cases. Run the full host gate before the
B12 commit. Device interaction, rendering/playback and visual acceptance remain
pending explicit authorization.

## Implementation evidence

Implemented and host-verified on 2026-09-04; device and user visual acceptance
remain pending. No device, emulator, installation or screenshot was used.

- [PhotoEditorModels.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/model/PhotoEditorModels.kt:40)
  owns reversible history, source-space geometry, limits and typed session events.
- [AppViewModel.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt:1447)
  owns profile/chat/attachment/frame sessions and revision-checked load/save.
  It commits only the rendered frame, preserving identity, siblings, caption,
  reply and retained sources. Removal, quality changes and profile/session
  changes invalidate stale work. Save failure retains the recipe and supports
  Retry or editing at a lower quality.
- [PhotoEditorUi.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/PhotoEditorUi.kt:50)
  adds the Material editor, crop/draw/erase canvas, native coordinate sliders,
  quality, live feedback and dirty dismissal. Editing is included in the existing
  modal exclusion from conversation read acknowledgement. Controls scroll and
  move beside the canvas in wide/short windows; physical image coordinates are
  independent of layout direction.
- [PhotoEditorRenderer.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/PhotoEditorRenderer.kt:94)
  decodes a bounded source region, applies EXIF orientation, crop/rotation and a
  separate ink layer, then returns measured PNG/JPEG bytes. Preview and export
  share annotation drawing; erasing never clears source pixels. Animated,
  corrupt, unavailable, over-limit and allocation failures remain explicit.
- [ConversationComposer.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt:2099)
  reviews every album frame, targets the selected frame for editing and retains
  review exclusion across saved edits/recreation. Inclusion remains an
  attachment-level action. B11 quality changes replay recipes, and the composer
  displays mixed quality when frames differ. Sent/forwarded payloads retain
  rendered images and discard original sources, recipes and frame overrides.

Preserve B11's explicitly selected Low/Standard/High quality instead of silently
upgrading Low on entry, as production currently does. Original with actual edits
uses High and explains that conversion. Five functional ink colors do not change
monochrome app chrome or resolve the separate B26/Q01 appearance question.

Validation: the full clean README gate and subsequent canvas-helper check passed **399 unit tests**, zero failures,
errors or skips, **zero lint errors**, the same 14 pre-existing warnings and two
hints, and both application/test APKs. Eighteen new host tests cover transform
inverses, preset ratios, crop bounds, history/limits, quality reversibility,
selected-frame ownership, discard/retry and stale/profile rejection. Nine new
UI/platform cases compile for selected pixels/dimensions, erasing, quality
replay, all eight EXIF orientations, invalid/animated input, dirty dismissal,
retry, accessible coordinate actions and album review. They were not run.

Evidence: [PhotoEditorModelsTest.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/test/java/dev/ipf/whitenoise/model/PhotoEditorModelsTest.kt),
[PhotoEditorStateTest.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/test/java/dev/ipf/whitenoise/state/PhotoEditorStateTest.kt),
[PhotoEditorTest.kt](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/androidTest/java/dev/ipf/whitenoise/PhotoEditorTest.kt).
Transient validation logs are `/tmp/wn-b12-final-gate.log` and
`/tmp/wn-b12-canvas-check.log`; the latter clears two new KTX helper warnings
found by the clean lint pass.
