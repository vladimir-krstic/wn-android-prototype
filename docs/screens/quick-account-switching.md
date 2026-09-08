# Optional quick account switching

## Purpose and scope

The user approved an optional one-tap account cycle beside the Chats avatar,
a direct Settings entry for a single signed-in account, and the existing Add
Profile action in the same segmented group as the Settings identity row,
visible only with exactly one signed-in account. This supersedes the earlier
always-open-switcher avatar decision. No authentication, persistence, backend,
or account-creation behavior changes.

## Behavior and navigation

- Appearance exposes Quick account switching, off by default. It belongs to
  AppUiState, not ProfileSettings, and stays enabled across account changes,
  adding accounts and signing out. Erase App Data resets it. Like the rest of
  the prototype's app state, it is in memory only.
- With one signed-in account the avatar opens Settings. With multiple accounts
  it opens the existing sheet, preserving its identity rows and footer actions.
- With the toggle enabled and at least two signed-in accounts, a separate icon
  beside the avatar cycles through the stable underlying profile-list order,
  wrapping at the end. The sheet's active-first presentation does not affect
  cycling. Retained signed-out profiles are excluded.
- Each tap computes its destination from current state and calls selectProfile,
  preserving existing account ownership/cleanup behavior. Missing active state,
  an off toggle or fewer than two signed-in accounts produce no switch.
- A short native toast names the account actually selected. Repeated taps
  replace the toast. Tooltip and accessibility label name the next account.
- After a second account is added, Settings hides Add Profile; the existing
  multi-account sheet still offers it. Signing out to one account restores it.
- Add Profile reuses SettingsAction, the person-add icon and the existing
  Welcome(AddProfile) navigation. Back returns to Settings without changing
  the active profile; successful onboarding uses its existing Chats destination.
- A forced post-sign-out sheet is omitted when only one account remains.

## Copy and native composition

Quick account switching
Show a button beside your avatar to switch accounts with one tap.
Switch to %1$s
Switched to %1$s

The existing Add Profile resource/flow remains shared. New copy is localized
across the four supported translated locales. The home shortcut uses a 20 dp
rounded Material Symbols swap_vert icon in a 30 dp neutral gray circle,
inside a 48 dp clickable target. The icon uses onSurfaceVariant and the circle uses
surfaceContainerHighest, with the existing outline treatment in AMOLED Outline.
The user rejected the filled circle symbol as too heavy in a supplied screenshot;
the smaller circle optically balances the 32 dp update emblem, while its separate
20 dp arrows match the update glyph scale. The visible circle moves 4 dp toward
the avatar using an RTL-aware offset,
without moving or overlapping either 48 dp touch target. The native clickable
modifier owns keyboard, focus, hover and button semantics. Its shared interaction
source drives a centered Material ripple clipped to the 30 dp circle. This small
custom composition meets the user requirement for a smaller state layer while
preserving the larger target; a default IconButton paints beyond the circle.
PlainTooltip remains unchanged. The avatar
retains its 40 dp image. Native Row placement handles RTL and the existing
TopAppBar owns insets and scrolling. Settings uses the shared segmented rows.
System Back, tooltip hover/long-press, TalkBack and keyboard activation remain
native. No added system permissions or network access.

## Sources

- Historical grouped profile/Add Profile layout: `527217d^`,
  `ProfileSettingsScreens.kt`, SettingsProfileHeader/ProfileManagementRow.
  Restored using the current shared segmented SettingsGroup pattern.

- https://developer.android.com/develop/ui/compose/components/icon-button
- https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions
- https://developer.android.com/develop/ui/compose/components/tooltip
- https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- Icon: https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/swap_vert/materialsymbolsrounded/swap_vert_24px.xml
- Local parity: onboarding-and-profiles.md; settings-and-profile-services.md;
  docs/port/source-map.md Settings/profile entries. This is an explicitly
  approved Android product change to the existing account entry behavior.

## Validation

Unit coverage checks default/single-account behavior, stable wraparound,
unchanged drafts, disabled actions, sign-out/retained profiles, addition/removal,
missing active profile and erase reset. Navigation coverage checks single-account
Settings/Add Profile/Back, Add Profile visibility after adding/signing out of
a second account, shared profile grouping, Appearance opt-in, cycling and the
preserved sheet.
Device inspection is not authorized in this request; visual acceptance pending.

Final host gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passed. All six new account-switching unit tests pass; updated navigation tests
compile. The toast uses configuration-aware resources, and the tooltip uses the
pinned Material API. No device testing was performed.
