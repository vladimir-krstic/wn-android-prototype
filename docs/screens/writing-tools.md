# Composer writing tools

## Purpose and scope

Issue #22, approved 2026-09-07: let the user proofread, rewrite or summarize a whole
draft or selected passage, review the suggestion and explicitly apply or discard
it. This is a deterministic local prototype. No AI runtime, model artifact,
provider configuration, credentials, network request, telemetry or auto-send is
added. The original draft remains authoritative until Apply.

## Entry, navigation and ownership

Writing tools appears last in the composer + menu, disabled for blank drafts. It uses
the whole draft even if a range is selected. Native text selection appends Writing
tools for a nonblank selection; a matching accessibility action provides access
without pointer selection-menu gestures. That path changes only the selected
passage. Both open the same Material bottom sheet: choose Proofread, Rewrite or
Summarize, then review Original and Suggestion with Apply and Discard.

The request binds profile, chat, original draft, selection, reply/attachment
context, edit eligibility and a monotonically changing composer revision. New
text, selection changes, edit mode, capture, or context changes invalidate the
suggestion. An edit followed by undo cannot revive an older result. Apply checks
the binding again, changes only that draft, preserves surrounding text and selects
the replaced passage (whole-draft use places the cursor at the result end).
Duplicate Apply and late completion after cancellation have no effect.

Discard, native Back and outside dismissal leave the draft untouched. Navigation,
backgrounding and recreation discard the transient suggestion; the existing
composer saves draft text/selection and expanded state. No generated text is saved
across recreation. No changes are made to edit-message dialogs or sent messages.

## Deterministic behavior and states

A fixed set of spelling/phrase substitutions and a first-sentence extract exercise
the interactions without a model. Example: `i recieve teh note tommorow.` becomes
`I receive the note tomorrow.` Proofread preserves complete code, mentions, URLs
and Markdown spans. Selections cutting through protected tokens produce no change;
partial UTF-16 surrogate selections are rejected. Summarize leaves rich text and
multiline structures intact. Other text can return No changes suggested, with
Apply disabled. These narrow examples do not claim general language capability.

An 8,000-character operation-input limit applies to the selected passage or whole
draft. Developer tools → Writing tools scenarios provides Local ready, local model
unavailable once, model download required, timeout once, refusal once, too-long
input once and external-provider disclosure. Download and execution progress use
bounded local delays. Explicit Download starts the download scenario; Cancel
interrupts it. Retry stays on the local ready path. The optional external-provider
scenario requires Continue before executing its local example; it sends nothing.
There is no silent external fallback when the local-ready path is unavailable.

Observable phases: choose, loading, preview/no change, unavailable, download
required/downloading, external disclosure, timeout, refused, too long and stale.
Cancellation closes the sheet. An identity-checked one-shot developer scenario
cannot be consumed by another profile or remain active after developer tools are
disabled. App erasure clears the pending scenario.

## Copy and Android composition

Use localized Writing tools, Proofread, Rewrite, Summarize, Original, Suggestion,
Apply and Discard. Status text explains unavailable, download, timeout, refusal,
long input and changed drafts; all five application locales are supplied in the
`writing_*` resources. The disclosure says an external provider would receive the
chosen text and requires explicit Continue. Technical scenario labels stay in
Developer tools.

Use the existing native expressive + menu, Compose appendTextContextMenuComponents
and the shared Material modal-sheet wrapper. All phases fit their content up to
the existing 88% window-height cap, with a scrolling body and actions below it.
The chooser shows the actual operation input, truncated after five lines with an
ellipsis, followed by start-aligned native list rows with check, edit and list
icons. It does not label the scope as whole draft or selected text. The full
input remains bound to the operation regardless of visual truncation.
Original and Suggestion share one surfaceContainerHigh container, separated by a
HorizontalDivider. Original text uses onSurfaceVariant; the suggestion uses
onSurface. Both use bodyLarge text, 16 dp insets and 8/16 dp related spacing.
The shared gray callout announces no changes or the operation-specific changes:
proofreading fixes, rephrasing, or the local first-sentence summary. Action
buttons wrap with FlowRow for large text, narrow widths and RTL.
Titles use heading semantics and status updates use polite live regions. Native
focus, touch targets, Back, safe-drawing/IME insets and monochrome themes remain.

## Evidence and approved extension

Existing composer intent comes from `docs/port/source-map.md` Composer and
attachments, pinned iOS `0bd7cba` conversation-composer-states/conversation-shared
briefs and PrototypeComposerTextView.swift. Android implementation evidence is
`docs/screens/composer-media-and-speech.md`. Writing tools itself is an approved
extension; production Android issue #2232 remained open at inspected master
`b6f99709972604ebaed00b15bc15e1eb8ce6fcaf`.

Official Android guidance checked 2026-09-07:
- https://developer.android.com/develop/ui/compose/text/user-input — retain TextFieldValue text/selection ownership and synchronous draft updates.
- https://developer.android.com/develop/ui/compose/components/bottom-sheets — native modal sheet, dismissal and sheet-state ownership.
- Existing MessageReadingUi uses the same first-party appended selection-menu API; no custom replacement toolbar is introduced.
- https://developer.android.com/develop/ui/compose/text/configure-layout — five-line input preview with native ellipsis (checked 2026-09-07).
- https://developer.android.com/develop/ui/compose/components/divider — native horizontal divider within the shared comparison container (checked 2026-09-07).

## Acceptance and validation

- Whole-draft and selected-passage operations preserve original text until Apply.
- Discard, Back, cancellation, stale results and profile/chat changes never send or overwrite another draft.
- Reply/attachment context, rich spans, Unicode and selection ranges remain coherent.
- Recovery and explicit download/disclosure states remain bounded local examples.
- Preview actions remain available with large text, RTL and all themes.

Host gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
UI tests compile only unless the user explicitly requests device execution.
Current-build visual acceptance remains pending.

2026-09-07 validation: the full host gate passed with 1,013 unit tests (22
writing-tools regressions), zero failures, lint and both APK assemblies. Eight
writing-tools UI tests compiled; none ran on a device. Locale verification
passed for all four translated locales; all six script tests passed.

The 2026-09-07 presentation follow-up passed the same full host gate and locale
verification (1,801 resources across four translations). No device inspection
was performed for this revision.

User accepted the flow on 2026-09-07 and requested #22 closure after placing
Writing tools last in the menu. The native text-selection entry already appends
after the standard actions. Agent validation remains host-side only.
