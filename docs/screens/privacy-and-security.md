# Privacy & Security

Status: User-selected polish implemented on 2026-08-29. Static verification
is recorded in the parity ledger. The full route, destructive section,
light/dark appearance, and 200% type passed current Pixel 8a inspection on
2026-08-31; user visual acceptance remains separate.

## Purpose and scope

Give the active profile clear controls for Recents privacy, device
authentication and auto-lock, optional diagnostics, and device-wide erasure.
The iOS screens are product/copy evidence only. Android owns navigation,
switches, settings recovery, choice dialogs, bottom sheets, insets and Back.
The prototype keeps deterministic in-memory preferences; it does not add real
biometric prompting, telemetry transport, persistence or networking.

## Android composition and behavior

- The neutral Settings canvas contains white-equivalent grouped rows with the
  shared two-dp canvas-tone separators. **Hide Screen in Recents** describes
  the visible privacy outcome without using iOS App Switcher terminology.
- **Require device authentication** is available only when Android reports a
  secure device lock. Otherwise it stays visibly unavailable with one recovery
  action into Android security settings. **Auto-lock** appears only while
  authentication is effective, avoiding a timing control for a disabled gate.
- Auto-lock is one immediate Material radio choice in a compact alert dialog.
  The option list sits directly on the dialog surface as an accessible
  selectable group, without a nested card or separators. Selection is conveyed
  by the native radio, not a rectangular selected fill. Dialog-owned padding
  positions the row; the shared choice row does not add a settings-list inset.
- **Diagnostics & Improvements** keeps its Off/Analytics/Logs/On summary and a
  focused explanation. Its two independent switches form one separated group.
  Stored logs appear only after the profile has created records; clearing
  requires a Material alert and preserves the logging preference. This is the
  sole permanent consumer entry; Developer Tools only inspects the read-only
  logging state and non-empty sanitized file inventory.
- **Erase App Data** is one destructive row with its consequence outside the
  action. It opens an expanded Material modal bottom sheet with only Expanded
  and Hidden anchors, so it cannot settle at partial height; downward dismissal
  and Back remain available. The sheet uses an error callout, selectable
  generated phrase, shared white rounded confirmation field, exact-match gate,
  and a pinned error action on the same gray modal canvas with safe/IME insets.

## Accessibility and adaptation

Whole switch and radio rows expose one native role and state. Dividers do not
duplicate semantics. Material owns touch targets, state layers, focus, motion
and dialog dismissal. Lists and the destructive sheet scroll at large font or
short height; directional shared insets support RTL. System Back dismisses a
dialog or sheet before navigating away, and disabled actions retain an
explanatory reason.

## Governing Android sources

- [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button)
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
- [Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Secure sensitive activities](https://developer.android.com/security/fraud-prevention/activities)
- [Material inset handling](https://developer.android.com/develop/ui/compose/system/material-insets)

## Evidence and remaining boundary

Implementation lives in `PreferenceScreens.kt`,
`DiagnosticsImprovementsScreen.kt`, and `DestructiveScreens.kt`. Compose
regressions cover the authentication/Auto-lock dependency, separated choice
groups, conditional retained-log section, destructive sheet structure, large
text, dark theme and RTL compilation. Real device authentication and
telemetry remain outside this prototype; device execution, TalkBack and visual
acceptance require an explicitly requested hands-on pass.
