# Shared sections and settings polish

## Purpose and scope

The user's 2026-09-05 request explicitly selects an app-wide presentation pass:
correct section padding, place explanatory copy below its controls, and use
Google's segmented list treatment. This includes Settings, profile and people
flows, chat preferences, and developer sections using the shared components.

## Parity contract and copy

Keep existing labels, summaries, values, warnings, destinations, availability,
selection, callbacks, and deterministic state. Existing screen briefs and string
resources remain the exact product-copy authority. No capabilities or system
integrations are added. Back, predictive Back, modal dismissal and restoration
retain their existing owners. The pinned source mapping in
`../port/source-map.md` supplies the Settings/Preferences, Profile, Chat Info,
and chat-creation product evidence; no upstream geometry is a requirement.

## Android composition

- `SettingsGroup` collects visible rows and assigns
  `ListItemDefaults.segmentedShapes(index, count)` and `SegmentedGap` (2 dp in
  the pinned library). Native rows render their own Material surfaces and
  interaction states. Custom content cells use a Material `Surface` with the
  same positional shape. No manual separator participates in the row count.
- Every independently interactive control is one row, including radio choices,
  expanded profile choices, publication actions and developer controls. Dynamic
  rows update the first/last positions in the same composition.
- The shared lazy list provides an 8 dp initial inset and related-item gap,
  24 dp bottom clearance, and 8 dp between multiple roots inside one lazy item.
  Headings contribute the remaining 16 dp above themselves to make a 24 dp
  section relationship; their following gap remains 8 dp. Non-lazy headings
  retain 24 dp above and 8 dp below.
- Group margins are 16 dp. Heading and helper text use the 32 dp directional
  row-text line. Helpers follow the relevant controls with an 8 dp gap, whether
  emitted inside the same lazy item, in a separate item, or in a regular column.
- Long values use Material supporting content below the label, leaving the
  trailing slot for the disclosure icon. Values and explanatory summaries can
  wrap; duplicate value/summary strings are displayed once. This fixes the
  supplied Read Aloud example without truncating the engine label.
- Section headings retain heading semantics. Links/actions, switches and radio
  choices use native interactive ListItem APIs and one accessible action each.
- Speech-option dialogs reuse `WhiteNoiseDialogChoiceRow`, now with optional
  supporting text, to avoid a second ListItem inset inside Material dialogs.
- Existing people-list groups use Material's normal inner corner shapes rather
  than the earlier square inner-corner override. Their lazy row composition and
  selection behavior remain intact.

## Accessibility and adaptation

Rows grow with font scale and longer labels; values never compete horizontally
with their setting names. Material owns targets, typography, disabled states,
focus, ripple and control semantics. Shared colors use semantic surface roles
in both themes. Directional insets and the existing adaptive content pane,
scrolling, system-bar, IME and safe-area behavior remain in place.

## Governing sources

- [Android settings](https://developer.android.com/design/ui/mobile/guides/patterns/settings):
  related groups, clear labels, supporting text, selection and adaptation.
- [Android grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units):
  shared 4/8 dp composition rhythm.
- [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3).
- The already-pinned official Material 3 `1.5.0-alpha25` source archive,
  `ListItem.kt` and `ListItemDefaults.kt`: native interaction overloads,
  positional shapes, supporting content and `SegmentedGap`. No version change.

## Observable acceptance criteria and evidence

- Adjacent controls have native segmented corners and a 2 dp gap; separate
  groups no longer touch. Conditional rows do not leave an empty cell.
- Group headings precede controls; section helpers follow them at the shared
  inset. Empty states and actionable warnings remain near their context.
- The Read Aloud Engine title remains readable with the complete engine value
  below it, including at 200% type in RTL. Voice and rate have the same behavior.
- Tapping a switch changes it once, disabled actions remain disabled, and all
  existing destinations, dialogs and values remain available.
- `SettingsLayoutTest` covers dynamic-row separation and switch semantics,
  disabled actions, long engine values at 200% RTL in dark appearance, and
  identical helper spacing for combined/separate lazy items. These are compiled
  instrumentation cases; execution requires a current device-testing request.
- Host validation passes: 892 unit tests, zero lint errors (15 warnings and
  two hints), debug APK assembly and instrumentation-test APK compilation.
  The three new layout cases compile only. Current-build device inspection
  and user visual acceptance remain pending.

## Approved differences

The latest user request supersedes earlier straight inner-edge card treatment
and above-control explanatory placement. Native Material owns row geometry;
the app only composes its existing custom content cells and section hierarchy.


## Modal selector follow-up — 2026-09-05

The user explicitly requested rounded selection surfaces throughout app-owned
modal selectors, using Dictation's Finish dictation and When finished dialogs
as examples. `WhiteNoiseDialogChoiceRow` remains the common radio-row composition
already used by Mute, general preferences and speech choices. Its selected
`surfaceContainerHigh` fill now shares the existing `MaterialTheme.shapes.large`
clip with native pressed, focus, hover and ripple feedback. Unselected rows
remain transparent, and disabled choices retain native disabled control/text
states. This follows the user's selected-fill reference; the radio remains the
non-color selected indicator and the whole row remains one accessible action.

Photo quality, disappearing-message presets, access/sign-out scenarios and the
generic developer scenario chooser now reuse that row. Radio groups expose
`selectableGroup` semantics. Folder multi-choice and download-network switch
rows keep their controls and use the same shared selection-boundary modifier.
The remaining separate image-selection surface is the custom avatar image grid,
whose image-shaped selection is outside the modal list-selector pattern.

All labels and supporting text remain. Immediate choices still apply and close
according to their caller; retention and scenario dialogs still stage a choice
until Save/confirmation, and Cancel/Back retain their existing behavior. Android
pickers, permissions, date/time components and other system-owned surfaces are
unchanged.

Official sources checked for this follow-up:
[Compose radio buttons](https://developer.android.com/develop/ui/compose/components/radio-button)
for whole-row actions and radio-group semantics, and
[Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
for the native modal container. The app's previously accepted rounded row owns
the surface geometry.

`ModalSelectorTest` adds compiled regressions for selected and pressed corner
containment in light/dark appearance, full-row single activation, disabled
choices and supporting copy. The host gate passes: 892 unit tests, zero lint
errors (15 warnings and two hints), and both debug APKs assembled. The three
new UI cases compile only; device execution and visual acceptance are pending.

## Chat, people and profile selector sheets — 2026-09-05

The user selected all app-owned chat/user directory modals for migration to the
existing contact/forwarding bottom-sheet pattern. Included Chats, People and
folder Preview now share `WhiteNoiseEntityPickerSheet` with global chat/sender
filters, group administration transfer, forwarding profile choice and incoming
share profile choice. Existing contact, forwarding, contact-group and profile
switcher sheets use white segmented rows with native inner corners. Incoming
share's full-screen review task retains its task boundary and adopts matching
recipient row containers.

The shared sheet uses `WhiteNoiseModalBottomSheet`, expanded/hidden anchors, a
Close header, pinned compact search, keyed lazy rows and an optional pinned Done
button. Short lists wrap content; long lists stop at 88% of the app window's
height and scroll. Semantic surface roles supply white rows in light appearance
and their dark equivalents. Material supplies row geometry, ripple, targets,
checkbox semantics, window/IME insets, drag dismissal and predictive Back.

Existing strings remain authoritative. Folder toggles only alter the editor's
draft until Save; Preview is read-only. Global filter toggles apply immediately,
Close/Back returns to the filter menu, and Done closes the filter flow. Admin
selection still precedes the separate consequence confirmation. Profile choice
only changes the destination profile; it never starts forwarding/sharing. The
parent forwarding sheet remains mounted beneath its profile chooser, retaining
its query and pending message. Device contact/photo/file pickers remain owned
by Android. Inline mention suggestions, value dialogs, editing forms and
consequence confirmations keep their existing task boundaries.

The local product evidence remains the folder/global-search, group lifecycle,
message-forwarding and incoming-share records routed through
`../port/source-map.md`; this changes presentation only. Current official
[Compose bottom-sheet guidance](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
was checked on 2026-09-05 for state/dismissal ownership. The existing pinned
Material sources govern segmented list metrics.

Acceptance: long directories scroll without moving search/Done out of reach;
filtering recomputes first/last row corners; multiple selections survive search
and recreation; preview exposes no selection action; disabled members cannot be
selected; Back/Close dismiss the top picker without saving a folder or starting
a group/share operation. `EntityPickerSheetTest` covers restoration, one-callback
selection, read-only/empty preview and disabled selection. `ChatFolderFlowTest`
now asserts the native sheet boundary in its draft/save flow. Instrumentation
execution and visual acceptance remain pending; only host checks are authorized.

Host evidence: `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes with 892 unit tests, zero lint errors (15 warnings and two hints), and
both APKs built. The three new instrumentation cases compile; no device or
emulator was used. A test-only resource-label correction was subsequently
checked with `assembleDebugAndroidTest`.

## Stable row shape follow-up — 2026-09-05

The user accepts the white grouped treatment and explicitly requests disabling
shape morphing app-wide. `WhiteNoiseListItemDefaults` retains Material's resting
row shape in every interaction state, including selected/checked, pressed,
focused, hovered and dragged. Group positions still determine rounded outer
corners and smaller inner corners; selection cannot turn a row into a pill.
Shared settings, entity pickers, contact/group/profile lists, chat-info actions,
forwarding and standalone interactive chat/search rows use these defaults.

Labels, selection/toggle callbacks, native control feedback, state-layer colors,
ripples, availability, semantics and task behavior remain unchanged. This is a
user-approved presentation override of Material's default shape transitions,
using its public `ListItemShapes` API rather than replacing the controls.
[Official shape API](https://developer.android.com/reference/kotlin/androidx/compose/material3/ListItemShapes)
and the pinned Material source were checked for all six state shapes. Existing
screen evidence and accessibility/adaptation criteria above remain applicable.

Acceptance: tapping, toggling, selecting and pressing a row preserve its resting
boundary and group spacing; selected states remain identifiable through native
controls and state colors. Device visual verification remains pending.

Host checks pass for this follow-up: 892 unit tests, lint with zero errors
(15 warnings and two hints), debug APK assembly and instrumentation-test APK
compilation. The existing chat-row corner assertion samples the resting corner
boundary instead of assuming the former larger selected corner.


## Shared informational callouts — 2026-09-05

The user's Developer Tools reference defines the shared `WhiteNoiseCallout`:
a Material Surface with the existing large corners, `surfaceContainer`,
`onSurfaceVariant`, 16 dp internal padding, a 24 dp icon and a 16 dp icon/text
gap. Optional title uses titleSmall and body uses bodyMedium. `SettingsCallout`
is its margin-owning adapter. Information defaults to an info icon; warnings use
the warning icon, updates use download, and existing destructive error callouts
retain their semantic error roles. Icons are decorative companions to text,
not duplicate TalkBack labels. Titles and bodies wrap with no fixed height;
callers retain live regions, native actions, progress and dismissal semantics.

Consumers include Amber signing ownership and local-key availability, developer
notices, relay attention notices, help/support information, public bug-report
reminders, speech discovery/engine/settings notices, connection/relay banners
and the update banner. Update release counts, Update now, important-release
non-dismissibility, connection progress, retry and relay navigation are
preserved. Ordinary field and section helpers retain their below-control
placement. This is the shared existing notice composition, not a new interaction
or runtime capability.

Current [Compose container guidance](https://developer.android.com/develop/ui/compose/components/card)
was checked on 2026-09-05. The native Surface composition comes from the existing
accepted Developer Tools callout. Acceptance requires consistent gray notice
containers and meaning-appropriate icons across these consumers in both themes,
with actions still reachable at large type. Device/visual acceptance is pending.

Host validation for the combined 2026-09-05 follow-up passes: 894 unit tests,
lint with zero errors, debug APK and instrumentation-test APK assembly. UI
cases compile only; no device/emulator use or visual verification is claimed.
