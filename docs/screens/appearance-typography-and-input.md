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
dedicated Settings details; Chat info opens that chat's override screen. Picker
changes preview immediately and commit to the owning profile/theme/chat. Back
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
