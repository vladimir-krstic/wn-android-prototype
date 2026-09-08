# Appearance, typography and input

Status: B26 implemented and host-verified. C100–C104 are covered. Device and
visual acceptance remain pending.

## Purpose and scope

Extend Appearance with production theme, color, typography, language and input
capabilities while preserving the prototype's monochrome defaults, neutral
Material surfaces and multiline composer. The implementation remains
deterministic and profile-owned; it adds no backend, persistence, network,
system-settings mutation or permission.

## Parity contract

- Add AMOLED as a fourth exclusive theme. App/canvas roles are black, layered
  surfaces remain neutral and distinguishable, and semantic error and disabled
  states remain Material-owned. System, Light and Dark retain their behavior.
- Add optional Action color and global message-bubble color controls for every
  theme. Each picker supports ten presets, hue/saturation/value sliders and a
  normalized six-digit hex value. Defaults remain monochrome and Reset restores
  the theme defaults. The action override changes primary action roles only;
  neutral surfaces and app chrome remain neutral.
- Add per-chat sent/received bubble overrides from Chat info. Per-chat values
  take precedence over the active theme's global values; clearing them restores
  the global values. Mutations are guarded by the owning profile and chat.
  Bubble text uses deterministic black-or-white content chosen for readable
  contrast with the selected background.
- Add System, Manrope, Outfit, Urbanist and Figtree app-font choices, with
  System unchanged as the default. Bundled static weights support API 23 and
  all 30 regular/emphasized Material roles share the chosen family. Explicit
  monospace remains explicit. [Font assets and licensing](../references/font-assets.md)
  records pinned provenance, notices and hashes.
- Add Small (0.85), Default (1), Large (1.15) and Extra large (1.3) font sizes.
  Scale explicit Material font sizes and sp line heights while preserving em or
  unspecified values and composing with Android's font/display scale.
- Add New line and Send message Enter behavior. New line remains the default.
  Send maps IME Send and hardware Enter/NumPad Enter to the existing guarded
  Send action. Shift+Enter, active IME composition, multiline paste/dictation,
  disabled states, key-up and held-key repeats cannot cause unintended sends.
- Preserve every existing language choice and add Russian, Turkish, Simplified
  Chinese and Traditional Chinese. Each new locale has a complete translated
  catalog matching all 1,762 translatable source resources, Android format
  tokens and locale plural rules. Selection applies immediately to the active
  profile's app context. System default keeps the device locale.

## Entry, navigation, Back and state

Appearance remains a Settings detail page. Theme uses the existing inline radio
group. App font, Font size, Language and Enter key behavior use the established
Material choice dialog. Action color and global Chat bubble colors open
dedicated Settings details; Chat info opens that chat's override screen. Bubble
picker changes preview immediately; one Save commits both message colors to the
owning profile/theme/chat. Back abandons unsaved bubble changes. Action-color
editing also previews locally and commits only with Save. Back
returns through the existing navigation stack. Profile switching restores the
selected profile's appearance, locale and input values without writing another
profile's state.

## Copy and composition

All ordinary interface copy is in `strings.xml` and the four complete locale
catalogs. The color screen pairs a live action or conversation preview with
presets, HSV sliders, a hex field, validation and Reset. It reuses
`SettingsScaffold`, Settings groups/rows, Material sliders and text fields.
Existing 16/24/8 dp composition, native touch targets and Material states remain
authoritative.

## Accessibility and system integration

Radio and slider semantics announce selection and value. Color is never the
only state signal; bubble foregrounds use the contrast policy, invalid hex input
has text/error semantics and Reset has a named action. Long translated labels
may wrap, layouts retain RTL support and font scaling changes text rather than
touch targets or geometry. Locale catalogs preserve numbered format tokens and
locale-appropriate plural quantities. Existing IME, focus, keyboard inset,
Back and expanded-composer behavior remain in control.

## Evidence and acceptance

Production baseline `319454889f1c2494dec4a69b5577d98017f44eee`:
`AppearanceScreen`, `ActionColorScreen`, `ChatBubbleColorsScreen`,
`FullSpectrumColorPicker`, `AppThemeMode`, `AppFont`, `AppFontScale`,
`AppLanguage`, `EnterKeyBehavior`, `ComposerPills` and theme typography. Its
warm AMOLED palette is translated to the approved monochrome identity while
its optional color capability remains available. The pinned source is routed
through the [source map](../port/source-map.md).

- [Keyboard actions](https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/commands)
- [Typography API](https://developer.android.com/reference/kotlin/androidx/compose/material3/Typography)
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)

Acceptance requires profile/theme/chat ownership, precedence and reset tests;
AMOLED/default/neutral-surface and typography invariance tests; locale key,
format-token and plural validation; Enter dispatch/composition/repeat tests;
compiled Compose interaction coverage; lint; and both debug APKs. Device and
visual acceptance require a separate current request.

## Resolved decisions

On 2026-09-04 the user selected all recommended choices. Q01 keeps monochrome
defaults and neutral app surfaces while enabling the full optional action and
global/per-chat bubble controls. Q09 keeps System and the device language as
defaults, uses licensed bundled static fonts, and provides complete Russian,
Turkish, Simplified Chinese and Traditional Chinese resource catalogs.

## Implementation evidence

`AppearanceInput` owns theme-aware action/bubble values, per-chat precedence,
normalization and readable foreground policy. `AppearanceColorScreens` provides
global and per-chat previews, presets, HSV and hex editing, validation and
reset. `WhiteNoiseTheme` applies only the chosen primary action roles;
`ConversationScreen` applies per-chat, then per-theme global, then monochrome
default bubble colors. `AppViewModel` rejects stale profile/chat mutations.

`WhiteNoiseTypography` maps the selected static family and size across every
Material style. `AppLocale` wraps the application content in the active
profile's localized configuration; app-bundle locale splitting is disabled so
runtime choices remain packaged. `verify_locale_resources.py` verifies 1,762
translatable resources across the four complete catalogs. The composer routes
the opt-in Enter command through its existing guarded Send callback.

The final host gate passes 892 unit tests with zero failures, errors or skips,
zero lint errors (15 retained dependency/version warnings and two hints), and
builds both debug APKs. Ten B26 Compose interaction cases compile in the
instrumentation APK; they were not executed. No device, emulator or visual
inspection was performed.


## Chat bubble reset in the header — 2026-09-05

The user's selected placement moves the reset action from the bottom of Chat
bubble colors into a trailing three-dot header menu using the shared native
menu component. Global editing offers Reset to default; per-chat editing offers
Reset to global colors. The menu item retains its previous availability rule,
clears only the relevant overrides and closes after selection. Back/outside
press dismisses the menu without resetting. The existing per-chat interaction
case now opens the menu, checks reset isolation/dismissal, then verifies disabled
reset when no overrides remain. Color pickers and previews are unchanged.

Host validation passes: 894 unit tests, zero lint errors, debug and
instrumentation-test APK assembly. The updated menu UI case compiles only;
no device/emulator inspection was performed.


## Standard Apply color button — 2026-09-05

The shared full-spectrum picker now uses `WhiteNoiseButton` for Apply color:
56 dp minimum task height and the shared 24 dp horizontal/8 dp vertical content
padding. This applies to Action color and both global/per-chat bubble pickers.
Full-width placement, valid-hex enablement and application behavior are unchanged.

The debug APK builds and whitespace checks pass. This uses the existing shared
button without new behavior or tests; no device inspection was performed.

## AMOLED Outline — 2026-09-06

The user approved a fifth appearance choice, **AMOLED Outline**, described as
“Pure black with crisp white outlines.” Existing AMOLED remains the layered,
filled monochrome variant. This is an explicitly authorized Android extension;
it does not change the pinned iOS or production parity baseline.

Observable behavior and composition:

- Selecting the radio row immediately applies the active profile's outline
  theme, independently of the system light/dark setting. Back, recreation,
  profile ownership, font and language behavior use the existing flow.
- Resting canvas and tonal container roles are pure black with transparent
  surface tint. Shared contained surfaces, message bubbles, composer, controls,
  grouped rows, menus and dialogs use a 1 dp white border following the existing
  component shape. Native sheets retain their handle, insets and motion; their
  content uses the shared outlined groups and controls.
- Sent and received bubbles both use black fill and white foreground. Existing
  alignment and shape identify direction. Replies, documents, link cards and
  voice transcripts use readable foregrounds instead of the old outgoing
  filled-action foreground. Images, avatars, QR codes and media overlays retain
  their content rendering.
- Selection gets a neutral white state layer; native input feedback, radio and
  checkbox marks, disabled alpha and semantic error colors remain distinct.
  Resting field borders are 1 dp, while focused/error fields retain the 2 dp
  state ring. No layout, type scale, gesture or touch-target changes are needed.
- This theme is fixed monochrome. Action/global bubble editing is disabled with
  explanatory copy; per-chat editing is likewise unavailable while selected.
  Existing saved overrides are retained and restored by switching themes.
  Directly opened color routes show the explanation and preserve Back. Both
  global and per-chat overrides are ignored when rendering outline bubbles.

Implementation: `AppearancePreference`, `AppearanceColorTheme`,
`WhiteNoiseAmoledOutlineColors`, `AmoledOutline`, shared components and the
conversation's bubble/accessory rendering. Prefer `Surface.border` and native
menu border parameters; modifiers cover components without a border parameter.
This extends Material theming without replacing native interaction components:
[Compose custom theme guidance](https://developer.android.com/develop/ui/compose/designsystems/custom).

User-requested reference inspection: production Android commit
`3b30efeb4f963ef1b9aa83dff6a88274ed34d9cf`, `ui/theme/AmoledSurface.kt`,
`Theme.kt`, and `ui/conversation/messages/BubblePresentation.kt`. The outline
mechanism informs this variant; the production warm palette and blue-channel
filter are not used.

Acceptance: host tests must verify system-independent selection, black resting
roles, white border and disabled alpha, foreground contrast, both bubble
backgrounds and override suppression without loss of saved settings. The theme
switch/customization interaction case must compile with the instrumentation
APK. Visual, large-font, RTL, TalkBack and native-sheet motion acceptance remain
pending a user-requested device inspection.

Host validation: `./scripts/verify_locale_resources.py` passes all 1,727
translatable resources across four complete locales.
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes 932 unit tests with zero failures, errors or skips, zero lint errors
and nine retained warnings, and assembles both debug APKs. The new theme
interaction case compiles; it has not been run on a device. No device/emulator
inspection was performed.


## 2026-09-07 — #28 live bubble preview with one Save

User requested live adjustment followed by a single Save when satisfied.
Global and per-chat bubble screens now keep an unsaved draft: presets, HSV
sliders and valid hex input update both the color indicator and the appropriate
message bubble immediately. A native sticky header keeps both example bubbles
visible while scrolling between sent and received controls. The shared pinned
bottom action saves both colors together and returns to the previous screen.
There are no per-picker Apply buttons on this screen.

Reset previews the default/global colors and also requires Save. Invalid hex
retains the last valid preview and disables Save until corrected or reset.
Save is disabled when nothing changed. Back uses existing navigation and writes
nothing. Saveable draft, HSV, raw hex and validation state survive recreation;
profile, chat, theme or source-color changes start a fresh editor. HSV controls
retain their hue through zero saturation/brightness instead of round-tripping
through the preview RGB color. Other theme overrides and profile preferences
are preserved. AMOLED Outline keeps its established fixed-color notice.

The screen reuses SettingsScaffold, SettingsBottomAction, WhiteNoiseButton,
Material sliders/fields and LazyColumn stickyHeader. No new layout metrics,
runtime dependencies or device integrations are introduced. The separate Action
color screen retains Apply. Slider value descriptions use localized labels.
Official sources checked: [Compose sliders](https://developer.android.com/develop/ui/compose/components/slider),
[Compose state](https://developer.android.com/develop/ui/compose/state), and
[sticky headers](https://developer.android.com/develop/ui/compose/lists#sticky-headers).

BubbleColorEditingTest adds Light/Dark rendered-preview checks for all three
sliders, both message colors, neutral/black hue preservation, explicit Save,
invalid hex, recreation, Back without saving and profile isolation.
AppearanceInputInteractionTest covers per-chat preview/save/reset while keeping
global colors unchanged. UI tests are host-compiled only; current-build device
and visual acceptance remain pending.

Visual entry: Settings → Appearance → Chat bubble colors, or a chat's title →
Chat bubble colors. Adjust sliders, swatches or hex; inspect the pinned preview,
then Save. Leave with Back to abandon a trial, or reset from the header menu and
Save to restore defaults.

Host validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` passed with 1,046 unit tests, zero failures/errors/skips, lint success and both APKs assembled. All six script tests, locale verification and whitespace checks pass. New UI regressions are compiled only; no device installation or inspection was performed.

## 2026-09-07 — Action color follows explicit Save

User supplied the Action color screenshot and requested Reset inside the white
picker block as an outlined task button, supporting text immediately below the
block, and Save matching Chat bubble colors. ActionColorEditor now previews the
accent locally with the same semantic primary/foreground policy as the saved
app theme. Sliders, swatches and valid hex update the Action example immediately;
a sticky preview keeps it visible while editing. The shared pinned Save button
commits the draft to the owning theme and returns. Back writes nothing.

Reset to default is a full-width WhiteNoiseOutlinedButton in the picker Column,
using the shared task height, native outlined treatment and 16 dp field spacing.
It resets the local preview and fields, and requires Save to commit. The standard
SettingsExplainer immediately follows the containing SettingsGroup. Save is
disabled for unchanged drafts and invalid hex; reset also clears invalid input.
Drafts are saveable and keyed by profile, theme and source accent. The default
preview comes from the uncustomized theme so reset cannot retain the old accent.
The shared FullSpectrumColorPicker now emits every valid color edit, with no
Apply button or intermediate commit path in either color screen.

ActionColorEditingTest adds compiled Light/Dark preview/Save, invalid hex,
default reset, Reset containment and Back-without-save coverage. The existing
AppearanceInputInteractionTest now checks Save before both color and reset
commits. Current-build device inspection remains pending.

Action-color host validation: the full unit/lint/app APK/UI-test APK gate passed, with 1,046 unit tests and no failures/errors/skips. Script tests, locale verification and whitespace checks pass. UI regression tests were compiled, not executed on a device.

## One AMOLED theme and connected groups — 2026-09-08

Latest user direction supersedes the separate AMOLED variants above. Appearance
now offers System default, Light, Dark and **AMOLED**. AMOLED retains the former
outlined palette, pure black surfaces, white outlines, fixed monochrome colors
and native interaction states. The old filled variant is removed. Existing
non-AMOLED color preferences, profile ownership and navigation are unchanged.

AMOLED grouped rows touch with no gap and no rounded inner corners. Each group
keeps its native outside corners and one physical-pixel outline. Each row owns
its bottom boundary; the next row omits its top stroke, so adjacent rows share
one separator. This is intentionally a physical pixel rather than one dp.
Independent groups retain their existing section margins.

`WhiteNoiseListItemDefaults` supplies theme-aware gaps and positional shapes.
`ConnectedRowShape` identifies those boundaries for the shared `amoledOutline`
modifier, including independently composed lazy rows. Material components still
own row semantics, touch targets, disabled/selected feedback, focus and clicks.
A small border path is needed because a complete border on every native row
would double the shared boundary; no custom gesture or state handling is added.
The same rule covers mixed Settings groups, profile switching, people/group
creation, group members/actions, entity/contact pickers, forwarding and incoming
share destinations. Other themes keep the existing Material segmented gaps.

State coverage: single/first/middle/last rows; conditional rows; enabled and
disabled controls; selected rows; LTR/RTL and changing density. The attached
reference is used for grouping geometry only; its yellow colors are not adopted.
No new platform integration, route or Back behavior is introduced.

Sources: [Compose dividers](https://developer.android.com/develop/ui/compose/components/divider)
and [Dp.Hairline](https://developer.android.com/reference/kotlin/androidx/compose/ui/unit/Dp).
Local parity remains the Appearance entry in `docs/port/source-map.md`; this
explicit Android presentation update does not expand the pinned iOS baseline.

Validation: `AmoledOutlineTest` now verifies the four-choice theme set and the
remaining palette/override policy. `AppearanceInputInteractionTest` covers
AMOLED selection and restoring color editing in Dark. `AmoledGroupedRowsTest`
checks touching mixed rows, a single-pixel separator at 1× and 3× density, RTL
edge continuity and retained row actions. Device tests are compiled only;
visual acceptance remains with the user.

Host verification (2026-09-08): `./gradlew testDebugUnitTest lintDebug
assembleDebug assembleDebugAndroidTest` passes; 1,058 unit tests, zero failures
or errors. App and instrumentation APKs assemble successfully. No device or
emulator was used.

### Message outline direction — 2026-09-08

User-approved follow-up: sent AMOLED bubbles retain pure white outlines;
received bubbles use the existing neutral gray outline color (`#999999`).
Both keep the existing 1 dp stroke, black fill and readable white content.
`amoledMessageBorder` applies at the shared bubble surface, including focused,
pinned/search presentations and agent-operation cards. Other themes and
non-message control borders retain their existing treatment.

Host follow-up verification: unit tests (1,058, zero failures/errors), lint,
app APK and instrumentation APK assembly all pass. No device inspection was
performed for the outline-color update.

### Chat-list rows and grouped perimeter weight — 2026-09-08

User screenshot follow-up: ordinary `ChatListRow` entries have no AMOLED outline.
Their existing selection, context actions and native pressed feedback remain.
Grouped controls retain touching rows and a single 1px internal divider; the
outside perimeter now uses the same pixel-rounded 1 dp weight as other outlined
controls. This supersedes the earlier 1px outside-border metric, avoiding the
very thin appearance of high-density rounded corners. First/last positional
shapes preserve continuous corners; lazy interior rows extend both side strokes
to their full height without adding another horizontal border.

Host verification for this follow-up: `testDebugUnitTest`, `lintDebug`,
`assembleDebug` and `assembleDebugAndroidTest` pass; 1,058 unit tests with no
failures or errors. No device or emulator inspection was performed.
