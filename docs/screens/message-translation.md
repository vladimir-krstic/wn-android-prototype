# Message translation

## Agreed scope

Issue #23, approved 2026-09-07. Long press → Translate → target language;
render the result in the same bubble with compact gray source/target metadata.
Reopen Translate to choose Show original / Show translation. Remember the profile's last manual
language. Profile settings → Translation owns automatic translation (initially
off) and default target; chat info → Auto-translate owns Inherit/On/Off and an
optional target override. All preferences and results are in memory only.

This is an approved extension beyond pinned iOS message interactions/settings in
`docs/port/source-map.md`. Production Android #2413/#2414 remain planned at
`b6f99709972604ebaed00b15bc15e1eb8ce6fcaf`. No AI, models, downloads, network,
persistence, telemetry or protocol mutation is added. Fixed phrase translations
and developer scenarios demonstrate the flow; unknown inputs retain originals.

## State and rendering contract

One conversation-owned coordinator binds message content/revision, profile, chat,
target and request generation. It deduplicates requests, bounds automatic work to
loaded incoming messages, prioritizes visible messages, and cancels work on
navigation, backgrounding, source edits/deletion or settings changes. Results are
never stored in the authoritative timeline. Search and quoted messages use
originals; copy, text selection and reading use the displayed rendition.
Translation status and recovery live only in the Translate sheet, with no
error/retry controls or automatic failure banner in the chat. Manual failures
offer retry, language selection and cancellation in the sheet. Missing packs require explicit Download
before a local progress example. Offline-unavailable, unsupported, uncertain
source, timeout and generic failure are developer-selectable cases.

## Android composition and validation

Use shared Material modal sheets, native action rows with trailing chevrons,
settings switches and gray callouts. Language rows execute immediately without
radios. The manual sheet starts with a gray original-text preview capped at five
lines and a localized Detected language label. Hide the detected source language
from targets and reject same-language manual/automatic requests before execution.
In-bubble metadata is static: bodySmall gray text and a centered, mirrored 16 dp
arrow, with 4 dp top and horizontal gaps. It disappears when the original is shown. Keep 16 dp sheet margins, 8/16 dp related spacing,
scrolling content, wrapping actions, system Back and existing IME/system insets.
No visual verification is claimed without an authorized current-build inspection.

Official sources checked 2026-09-07:
- https://developer.android.com/develop/ui/compose/components/bottom-sheets
- https://developer.android.com/develop/ui/compose/components/switch

Validate ownership, inheritance, original preservation, deduplication, cancellation,
stale callbacks, protected markup, failure/retry, and UI navigation with host unit
tests and compiled Compose tests. Run the README host gate; no device execution.

## Implementation evidence

`MessageTranslation.kt` contains the preference resolver and fixed phrase table
(English, Spanish, Serbian, German, French and Arabic). Unknown prose returns an
unavailable/uncertain state rather than a partial or invented translation.
Protected mentions, link destinations and code are retained verbatim; surrounding
Markdown and line breaks remain. Maya Chen includes three translation examples,
including two incoming Spanish messages and an outgoing English message.

`TranslationController.kt` owns at most 40 loaded-message requests/results,
prioritizes visible messages, processes one request at a time and ignores old
generations. Automatic work excludes own, deleted, expired, blank, agent, event,
location, voice-transcript, editing and over-limit messages. Manual cancellation suppresses an
automatic retry until settings/source/window changes. Backgrounding drops all
renditions; resuming schedules only enabled automatic work.

`TranslationUi.kt` supplies the language actions, source-to-target labels and
sheet-owned original toggle and retry/download/cancel controls. After a manual
language action, the sheet shows progress and closes on success; Back/Close
cancels pending work. Failed and missing-pack states stay in the sheet.
The Close action is a native X IconButton before the sheet title, with no bottom
Close button, in both message translation and the settings language picker.
Automatic failures retain originals silently; reopen Translate for recovery. Restoring an
existing rendition reuses the result without another request.
`TranslationSettings.kt` uses the profile settings route and chat info sheet.
The profile screen uses the shared SettingsList; SettingsGroup owns its single
16 dp horizontal margin, matching the other settings screens without an extra
outer inset.
AppViewModel accepts preference changes only for the active owning profile.
Developer tools → Translation supplies one-shot, profile-bound outcomes.

The conversation renderer uses originals during search and in reply quotes.
Copy, inline selection and the reader use the displayed rendition. Read-aloud
speaks the rendition with an original-source ownership field; edits and deletion
invalidate it, and changing the displayed rendition stops mismatched playback.
Profile preferences never contain source/translated text. No provider is invoked.

The Translate icon is the standard Material Symbols outlined 24 px vector from
https://github.com/google/material-design-icons/blob/master/symbols/android/translate/materialsymbolsoutlined/translate_24px.xml.

## Validation result

2026-09-07: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed. All 1,038 unit tests passed, including 22 translation/preference/speech
regressions. Six translation UI tests compiled; they were not run on a device.
All 1,823 resources verify across four translated locales. User hands-on
acceptance remains pending; issue #23 remains open.

2026-09-07 user refinement: remove all bubble translation actions and failure
notices, use chevron language actions with source preview/detection, move
Show original into Translate, exclude same-language targets and make metadata
compact and gray. The entrance caption is also a fixed translation example.
Arrow source: https://github.com/google/material-design-icons/blob/master/symbols/android/arrow_right_alt/materialsymbolsoutlined/arrow_right_alt_24px.xml.

Refinement validation: the full README host gate passed, with 1,046 unit tests
and no failures. Translation navigation tests were updated for sheet-owned
original access, source preview and source-language exclusion; all six compile.
All 1,825 resources verify across four translated locales. Device inspection and
user acceptance of this revision remain pending.
