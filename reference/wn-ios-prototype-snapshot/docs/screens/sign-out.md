# Sign Out and App Data Erasure

## Purpose

Separate two different tasks clearly:

- **Sign Out** ends the active profile session and can optionally remove that
  profile's local data.
- **Erase App Data** signs out every profile and permanently removes all
  White Noise data from this iPhone.

All behavior is deterministic and process-local. The prototype performs no
real authentication, persistence, account deletion, key destruction, or
network operation.

## Entry and navigation

- Settings contains one neutral **Sign Out** row for the active profile.
- Privacy & Security contains **Erase App Data** under **Device Data**.
- Each action opens a large-only native Form sheet. A leading system `xmark`
  toolbar button closes the sheet without changing session or local state.
- A single-profile exit presents the profile switcher when another signed-in
  profile remains and Welcome when none remain.
- Successful whole-app erasure always returns to Welcome.
- Device-wide sign-out without erasure is not offered.

## Sign Out sheet

- Title: **Sign Out**.
- The active profile and **Wipe Data From This Device** Toggle share one Form
  section.
- Wiping is on by default.
- Toggle-off footer: **This profile and its local data will stay on this
  device.**
- Toggle-on footer: **This profile and all local data will be permanently
  removed. Previous chats won’t return.**
- With wiping off, **Sign Out** ends only the active session and retains its
  chats, media, drafts, keys, and settings locally.
- With wiping on, exact profile-name confirmation is required. The final
  action is destructive and remains disabled until the name matches.
- **Sign Out** is a full-width red destructive button at the bottom of the
  Form. Its regular-weight label and placement remain stable in both states,
  so changing the wipe choice does not change the action's visual identity.
- A native leading X is the sheet's only toolbar action.
- The profile-name field uses direct local text state, ordinary keyboard input,
  and native focus. Typing the matching name enables Sign Out immediately.
- The sheet is the complete confirmation task and never stacks another alert.
- The progress state reads **Signing out…** when local data is retained and
  **Signing out and wiping data…** when wiping is selected.

## Erase App Data

- Privacy & Security section title: **Device Data**.
- Direct destructive action: **Erase App Data**.
- Footer: **Signs out every profile and permanently removes all White Noise
  data from this iPhone.**
- The action opens a Form sheet titled **Erase App Data**.
- A native warning callout uses the semantic orange
  `exclamationmark.triangle.fill`, the primary title **This can’t be undone**,
  and the secondary consequence **Every profile and all local chats, media,
  drafts, keys, and settings will be removed from this iPhone.**
- A deterministic three-word lowercase phrase is displayed above a
  **Confirmation phrase** field.
- Helper: **Enter the three words exactly to continue.** Leading and trailing
  whitespace from text entry is ignored; case and spacing between the words
  must still match.
- A native leading X remains in the toolbar. A full-width red destructive
  **Erase** button appears at the bottom of the Form and remains disabled until
  the phrase matches exactly.
- The phrase comes from a small repository-owned subset of the BIP-39 English
  list and is deterministically selected from stored profile IDs. It is only a
  confirmation challenge, never a key or recovery phrase.
- Completion clears every stored and signed-in profile, profile-owned content,
  app settings, and transient root state before returning to Welcome.

## Native components and accessibility

- `Button`, `sheet`, `NavigationStack`, `Form`, `Section`, `Toggle`,
  `TextField`, native toolbars, semantic destructive roles, and `ProgressView`.
- The whole-app erase action uses the destructive role and system red; it is
  never styled as the primary action.
- Sign Out uses the destructive role with or without wiping. The Toggle,
  footer, confirmation field, and enabled state communicate whether local
  data will also be removed.
- Sign Out and Erase use native full-width `borderedProminent` buttons inside
  their Forms. Both use the destructive role, semantic red tint, regular body
  text, and the native disabled treatment. They never appear in the trailing
  toolbar. Their Form rows remove the usual inner row inset so the visible
  buttons span the complete grouped content width.
- The warning callout uses semantic orange and text hierarchy, so color is not
  its only warning signal.
- Native controls own focus, keyboard behavior, Dynamic Type, enabled state,
  roles, and VoiceOver output.
- The profile row identifies the profile affected by Sign Out.
- Confirmation internals never expose private keys or other credentials.

## Apple authority

- [Managing accounts](https://developer.apple.com/design/human-interface-guidelines/managing-accounts)
- [Settings](https://developer.apple.com/design/human-interface-guidelines/settings)
- [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets)
- [Modality](https://developer.apple.com/design/human-interface-guidelines/modality)
- [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)
- [Entering data](https://developer.apple.com/design/human-interface-guidelines/entering-data)
- [Feedback](https://developer.apple.com/design/human-interface-guidelines/feedback)
- [Form](https://developer.apple.com/documentation/swiftui/form)
- [Toggle](https://developer.apple.com/documentation/swiftui/toggle)
- [TextField](https://developer.apple.com/documentation/swiftui/textfield)

Optional word-list provenance: [BIP-39 English word
list](https://github.com/bitcoin/bips/blob/master/bip-0039/english.txt).

## Acceptance criteria

- Settings shows only one **Sign Out** row.
- Privacy & Security shows **Erase App Data** under **Device Data**.
- The leading X preserves all session and local state.
- Both confirmation tasks always open at the large sheet detent.
- The profile-name field accepts typing normally and enables Sign Out as soon
  as the name matches.
- Erase remains disabled until the three-word phrase is entered with the exact
  case and word spacing; surrounding whitespace is ignored.
- Successful erasure removes every profile and all local app state, then opens
  Welcome.
- Fresh onboarding after either last-profile wipe or app-data erasure
  reconstructs the canonical Marmota profile. A later Add Profile Sign Up
  creates Pebble as a separate identity rather than reusing Marmota's ID or
  avatar.
- Current-profile Sign Out and optional wipe behavior remain unchanged.
