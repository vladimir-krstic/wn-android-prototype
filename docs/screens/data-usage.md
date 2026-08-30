# Data Usage

Status: User-selected Android polish implemented on 2026-08-29. Static
verification is recorded in the parity ledger; device visual acceptance
remains pending.

## Purpose and scope

Let the active profile choose when each media type downloads automatically and
the quality used for sent photos and videos. This prototype keeps the choices
in memory. It does not add transfer scheduling, connectivity observation,
compression, persistence, or networking.

## Parity contract

- Photos, Videos, Audio, and Files each choose Never, Wi-Fi, or Wi-Fi and
  cellular and update immediately.
- The defaults remain Photos: Wi-Fi, Videos: Never, Audio: Wi-Fi, and Files:
  Never. Reset download settings restores all four together and is unavailable
  while those defaults are already active.
- Photo and video quality chooses Standard or High and updates immediately.
  The choice explains that High preserves uncompressed quality while using
  more data and Standard compresses media to use less data.

## Entry, navigation, Back, and exit

Settings opens the typed Data Usage destination. Each disclosure opens a
compact Material single-choice dialog because these sets contain only two or
three options. Selection applies and dismisses immediately. Cancel, scrim tap,
and system Back dismiss without changing the value; Back on the page returns
to Settings.

## Exact product copy

The page uses **Data Usage**, **Automatic downloads**, **Photos**, **Videos**,
**Audio**, **Files**, **Reset download settings**, **Sent media**, and **Photo
and video quality**. Download help reads **Media that isn't downloaded
automatically shows a download button.** Sent-media help reads **Choose the
quality for photos and videos you send.** The quality dialog explains **High
sends uncompressed photos and videos for better quality, but uses more data.
Standard compresses media to use less data.**

## Android composition and behavior

The neutral Settings canvas contains two white-equivalent Material groups.
Automatic-download disclosures and the reset action use the shared two-dp
canvas-tone separators. The sent-media group contains one disclosure. Related
help follows its own group instead of describing a previous section from the
bottom of the page.

The small discrete option sets use Material `AlertDialog` and whole-row radio
selection instead of copying iOS child-screen navigation. Radio rows sit
directly on the dialog surface without nested cards, separators, or selected
rectangles. The dialog owns the outer content inset, so its direct choice rows
do not inherit the second inset used by full-page settings `ListItem`s. The
selectable group and row own native radio semantics, state layers, touch
targets, focus, and motion.

## Accessibility and adaptation

Each disclosure and reset action is one named button. Dialog options form one
accessible selectable group; the row owns the radio role and state. The reset
action exposes the disabled state when defaults are active. Material content
can grow for localization and font scaling, the page remains scrollable, and
directional insets and disclosure symbols support RTL.

## Governing Android sources

- [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button)
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)

## iOS parity evidence

- `docs/port/source-map.md` Preferences entry
- Pinned `WhiteNoisePrototype/Screens/Settings/PreferenceSettingsViews.swift`
- User-provided Data Usage, per-media, and Sent Media Quality screenshots

## Approved differences and custom exceptions

Android uses immediate radio dialogs for these two- and three-choice settings;
the iOS reference uses navigation child screens. This is a standard Android
multiple-choice settings pattern and avoids adding navigation depth for a
single compact decision. No custom interaction is required.

## Observable acceptance criteria

- Every current value is visible on the main page and selecting a radio option
  updates the matching profile setting immediately.
- Reset is disabled at the four defaults, becomes available after any download
  policy differs, and restores all four defaults.
- Help text remains attached to the correct group, and the quality tradeoff is
  visible while choosing quality.
- Dialog dismissal without selection preserves the current value.
- Every Auto-lock, per-media and sent-quality modal aligns the radio touch
  target to the dialog content edge without double-indenting the option row.
