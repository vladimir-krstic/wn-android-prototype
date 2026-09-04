# F14 — Appearance, text preferences and keyboard behavior

## Purpose and current composition

Appearance composes app-owned options with Android's font/display scale, IME
and locale behavior. Defaults remain the approved monochrome prototype while
optional production capabilities are available through native Android UI.

Prototype surface: `model/AppearanceInput.kt`, `model/ProfileSettings.kt`,
`ui/settings/PreferenceScreens.kt`, `ui/settings/AppearanceColorScreens.kt`,
`ui/settings/AppLocale.kt`, `ui/theme/`, `ui/conversation/ConversationComposer.kt`
and `ui/conversation/ConversationScreen.kt`. Reuse the established Settings
scaffolds/groups/rows, Material dialogs, sliders and fields, 48 dp targets,
adaptive bounds, RTL, 200% text and IME/inset ownership.

## Required content and copy

Use **Appearance**, **Theme mode**, **AMOLED**, **Action color**, **Chat bubble
colors**, **App font**, **Font size**, **Language**, **Enter key behavior**,
**Send message** and **New line**. Color controls include presets, HSV, hex,
preview, validation and Reset. All ordinary labels and consequences are
resource-backed in the default and complete Russian, Turkish, Simplified
Chinese and Traditional Chinese catalogs.

## Capability and state contract

| Capability | Implemented state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C099 · System/light/dark appearance | Existing equivalent | Select inline; Back preserves committed choice | Preserved behavior |
| C100 · AMOLED appearance option | Covered | Select inline | Optional black canvas with neutral layered surfaces and semantic state distinction; no default replacement. |
| C101 · Font family and in-app font size | Covered | Select from Material dialogs; dismiss/Back is inert | System plus four licensed bundled families and 0.85/1/1.15/1.3 scale composed with system scale. |
| C102 · Accent and global/per-chat bubble colors | Covered | Open detail, edit preset/HSV/hex, Reset, or Back | Per-theme/profile action and two global bubble colors plus per-chat overrides; defaults and neutral surfaces stay monochrome; readable content is deterministic. |
| C103 · Enter sends versus newline | Covered | Select preference; use IME or hardware keyboard | Opt-in guarded Send; default/newline, Shift+Enter, composition, paste/dictation and repeat behavior remain native and safe. |
| C104 · Additional languages and real localization | Covered | Select profile language; dismiss/Back is inert | Four new complete locale catalogs apply immediately; System and all existing language choices remain. |

## Production integration seam

Production evidence for each row is linked in the [matrix](../capability-matrix.md).
The prototype stores immutable profile/theme/chat fixtures and renders every
named result without Marmot, networking, signing, persistence, background
services or cryptography. Future production migration reconnects these
preferences to the cited state methods rather than copying fixture storage.

## Copy, accessibility and adaptation

State is conveyed by text and semantics as well as color. Picker values and
errors are named; bubble foregrounds use the contrast policy; radio/slider
semantics remain native. Long translated labels wrap, focus order remains
logical and Back returns through the established hierarchy. Actions retain
eligibility at large type and narrow height.

## Acceptance and host validation

- Every C100–C104 mutation is guarded by the owning profile/theme/chat and
  restores or resets predictably.
- Default colors, System typography, default scale, device locale and New line
  behavior remain unchanged until selected.
- Locale verification checks all 1,762 translatable keys, format tokens and
  locale plural forms.
- Unit tests cover rules and ownership; ten Compose cases cover routes,
  selection, Russian resource resolution and Enter behavior and compile only.
- The complete host gate passes 892 tests, lint with zero errors and both debug
  APK builds. Device and visual acceptance remain separate.

## Dependencies and decisions

Batch B26 depends on B01. Q01 and Q09 were resolved on 2026-09-04 by the user's
selection of all recommendations: preserve monochrome defaults and neutral
surfaces while adding full optional color controls, licensed optional fonts and
complete Russian, Turkish, Simplified Chinese and Traditional Chinese catalogs.

## B26 implementation evidence — 2026-09-04

C100–C104 are implemented and host-verified. System defaults remain unchanged;
AMOLED, optional colors, family/size, locale and Enter choices are profile-owned,
and chat overrides are profile/chat-owned. Font licenses, static API-23 weights
and hashes are documented. Locale verification covers 1,762 translatable
resources. The final gate passes 892 unit tests with zero failures/errors/skips,
zero lint errors (15 retained warnings/two hints) and both debug APKs. Ten B26
UI cases compile only. No device, emulator or visual acceptance occurred.
[Selected brief](../../../screens/appearance-typography-and-input.md#implementation-evidence).
