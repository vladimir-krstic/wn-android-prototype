# Scenario catalog and temporary sessions

## Purpose and scope

Find every existing developer scenario in one place, understand what it tests,
and enter its real product flow immediately. This is the user-approved app-wide
catalog, superseding the next-attempt profile-setup chooser. It remains a
strictly in-memory prototype feature.

## Entry, navigation, Back and exit

Settings → Developer Tools → **Scenarios** is available with Developer Tools
on or off. Search matches feature groups, scenario names, variant names and
descriptions. Each scenario has a dedicated variant page. Tapping a variant
creates a fresh temporary AppViewModel, seeds its prerequisites and opens the
specified typed route. This catalog is the sole scenario-selection surface:
feature pages contain no outcome pickers or buttons that inject example data.
Message editing, forwarding, deletion, writing tools,
profile editing, translation, retention and group actions open their relevant
controls directly. No separate Apply button or trip through the app is needed.

The original model and navigation controller remain separate. **Restart**
recreates the temporary model and resets retry counters. **Change Variant**
returns to that scenario's variant page. **Exit Scenario** restores the original
app session. System Back uses the temporary navigation stack; Back at its root
exits the scenario. Native dialogs retain their ordinary Back-to-dismiss behavior;
the scenario controls remain below the app content and become actionable again
when a modal is dismissed.

## Exact developer copy

- Scenarios; Search all scenarios; Scenario; Test a flow.
- “Choose a scenario, then tap a variant to start immediately. Each run uses
  temporary accounts and chats. Exit Scenario restores your app session.”
- Restart; Change Variant; Exit Scenario.
- Unlock for deferred incoming requests; Advance Time 1 min for expiry examples.
- Per-scenario and per-variant descriptions live next to their launch recipes in
  `scenarios/ScenarioCatalog.kt`; setup descriptions in `SetupScenarioDescriptions.kt`.
  These developer-only descriptions intentionally use English, like the existing
  developer labels. Consumer product copy is unaffected.

## Behavior and coverage

`ScenarioCatalog` is the inventory and launch registry. It covers all declared
scenario enum families plus inventory, distribution, environment, incoming,
notification commands, speech commands and former switch/action examples:

- Accounts: onboarding entry points, startup, private-key/Amber/recovery,
  setup, profile save/image, local key availability and sign-out cleanup.
- People/groups: search, contact actions, opening a created chat, roster,
  membership updates, images, creation, administration and lifecycle states.
- Messages/chats: history, editing, deletion, forwarding, translation, writing,
  bulk actions, connection, voice search and attachment library.
- Attachments/voice: transfers, file opening, photo editor, location, download
  network/queue, dictation, recording, speech catalog/history/audio/background.
- Incoming/notifications: every incoming example and outcome, deferred requests,
  settings/push/vibration/availability, actions and background connection.
- Privacy/diagnostics/relays: retention/expiry, transcript, lock, audit,
  every inspection operation/outcome, package inventories, performance,
  streaming, conversation content examples, publication/import, update
  distribution/check/install.

Enum variants are generated from each actual enum, so adding an enum value
cannot silently omit it. `scripts/test_scenario_catalog.py` fails for an
unregistered scenario enum, protects the non-enum inventory, and rejects
scenario variant selectors or fixture-injection wiring in feature UI/navigation. Host tests launch
every variant and verify route prerequisites, restart isolation and destructive
state separation. Launch recipes use normal controllers and product UI.

Fresh temporary profiles enable fixture outcomes locally. This never changes the
original profile's Developer Tools setting. Scenario state survives Activity
configuration changes through a retained session ViewModel; process death ends
the in-memory run, consistent with the app's existing prototype boundary.

## Android composition, accessibility and adaptation

Existing SettingsScaffold/SettingsList/SettingsGroup/SettingsLink components,
a labeled Material text field and text buttons. A full page presents variants;
selection is an action, not a radio setting. Descriptions wrap naturally and use
standard semantic text. The control row wraps at larger text/display sizes.
Shared screen margins and edge-to-edge inset handling apply. No custom gestures,
iOS onboarding sheets, fixed orientation or runtime dependencies are introduced.

## System integrations

Existing Android integrations are retained. Catalog descriptions mark flows that
may depend on permissions, installed handlers or device capabilities. Scripted
speech/background examples remain explicitly local. Exiting restores app state;
it does not delete user-exported files or revert device settings. Speech, capture
and route-owned effects dispose with the temporary UI; its ViewModelStore is
cleared when the run ends. No connected testing is authorized by this task.

## Governing Android sources

- https://developer.android.com/guide/navigation — typed destinations and Back.
- https://developer.android.com/develop/ui/compose/state-saving — saved UI state
  and the distinction between configuration changes and process death.
- Local `docs/ui-metrics.md`, `docs/references/native-ui.md`, product language
  and terminology govern the existing native settings components.

## Parity evidence and approved differences

The app-wide catalog and isolated immediate launch behavior are explicit user
Android decisions. Existing product fixture/controllers and the parity records
in `docs/port/source-map.md` supply each tested flow. The pinned iOS baseline is
unchanged; no new upstream scope is inferred.

## Acceptance and validation

- Scenarios remains accessible with Developer Tools disabled.
- Search finds every family; every variant has an explanation and launch recipe.
- A variant enters its prepared flow; restart creates fresh data; exit restores
  original accounts, chats and developer settings.
- Cross-account notification examples have a second account with real fixture
  content; group operation examples have the required lifecycle/capability state.
- Host unit, locale/static, lint and APK build results are recorded in README.
- Navigation instrumentation is compiled, not executed. Hands-on, TalkBack,
  large-text and adaptive acceptance remain pending user inspection.

Host validation completed 2026-09-08: 74 scenarios, 372 variants, 1,089 passing
unit tests; lint 0 errors / 16 warnings / 2 hints; app and instrumentation APKs
built. Eight Python checks, all 1,805 translatable keys in four locales and
`git diff --check` pass. No device launch, instrumentation execution or visual
verification was performed.

## Exclusive scenario ownership — 2026-09-08

The user requires all scenarios to live exclusively in this catalog. Removed
Conversation Debug's history/agent/event/file injection group and its navigation
callbacks. Deleted the obsolete access/sign-out, speech, update, download, relay
and inspection chooser components; catalog metadata retains the access labels.
Real feature operations, notification previews and accepted diagnostic tools
(such as health refresh, package publication and performance logging) remain
normal app functionality; none offer fixture selection or inject scenario data.

Read Aloud's stale-command UI test now uses the catalog's example surface. The
new host boundary checks prohibit variant lists and fixture-injection callbacks
in feature screens/navigation, while permitting scenario runtime consumption.

Exclusive-ownership follow-up validation: 1,089 unit tests pass; all 74 scenarios
and 372 variants remain registered. Ten Python checks and 1,750 translated keys
across four locales pass. App and instrumentation APKs build; lint reports zero
errors and 16 warnings. No device or emulator testing was performed.
