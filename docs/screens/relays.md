# Relays

## Purpose

Let the active profile inspect relay availability, assign Profile, Inbox and
Chat Messages roles, add a secure relay, remove an eligible relay, and restore
the pinned defaults without implying real network behavior in the prototype.

## Scope and non-goals

This batch covers the profile relay list, relay detail, Add Relay task,
role switches, availability presentation, recovery callout, remove
confirmation, and default restoration. Relay networking, durable storage,
Nostr transport, background reconnection and endpoint discovery remain out of
scope; the accepted deterministic in-memory fixtures and delayed custom-relay
status transition remain unchanged.

## Parity contract

- The active profile owns one ordered relay list with the pinned names, secure
  URLs, connection states, read-only capability and role assignments.
- Each relay opens a detail destination. Editable relays expose Profile, Inbox
  and Chat Messages switches; read-only relays expose those roles disabled and
  cannot be removed.
- Add Relay accepts one unique normalized `wss://` URL and at least one role,
  then adds the deterministic reconnecting custom relay.
- Remove Relay removes the selected eligible relay only after confirmation.
  Existing chats retain their own relay configuration.
- Restore Default Relays confirms before replacing custom relays and role
  changes with the seven pinned defaults.
- Unavailable profile capabilities retain the existing actionable recovery
  callout; the screen does not claim live relay connectivity.

## Entry, navigation, Back, and exit

Settings opens the typed Relays destination. A relay row opens the typed Relay
detail destination. App-bar Back and system/predictive Back return one level.
Add Relay opens a dismissible Material modal bottom sheet; Close, scrim,
downward dismissal and Back discard its draft. Remove and restore use Material
alerts; Cancel, scrim and Back leave state unchanged.

## Exact product copy

- Titles: **Relays**, **Relay**, **Add Relay**.
- Group labels: **Profile relays**, **Use For**.
- Roles: **Profile**, **Inbox**, **Chat Messages**.
- Statuses: **Connected**, **Reconnecting**, **Disconnected**; capability:
  **Read only**.
- Add field: **Relay URL**; placeholder `wss://relay.example.com`; help:
  **Enter a relay URL beginning with wss://.** Invalid/duplicate error:
  **Enter a unique wss:// relay URL.**
- Role help: **Publish your profile and connection information.**,
  **Receive invitations to new chats and groups.**, and
  **Use for messages in chats you create. Existing chats keep their current relays.**
- Actions: **Add Relay**, **Remove Relay**, **Restore Default Relays**,
  **Restore Defaults**, **Cancel**.
- Restore consequence: **This replaces this profile’s relay list and role
  assignments with the defaults. Custom relays will be removed.**
- Remove consequence: **This profile will stop using this relay. Existing
  chats keep their current relays.**

## Android composition

The list is a standard Settings group of Material list rows on the shared gray
canvas. Each row keeps name and URL as the primary and secondary scan path,
uses a compact 20 dp filled trailing status symbol with a 14 dp glyph, and
includes the exact status in TalkBack state. Connected is a green circle/check;
both Reconnecting and Disconnected are unavailable red circle/close states
rather than animated loading promises. Read only remains secondary text
because it is a capability, not a transient connection state. **Add Relay** is
the final icon-led action inside the group rather than a permanent page button
because adding is a peer management command, not the sole completion action.

Relay detail uses compact horizontal key/value rows with separators. Its label
column has a 64 dp minimum rather than claiming a percentage of the row; the
weighted value consumes the remaining width. Relay URLs use the normal UI
typeface, stay on one line and middle-ellipsize visually while retaining their
complete accessibility value. Whole-row Material switches follow. Add Relay
uses a Material modal bottom sheet because URL entry plus three explained roles
is a multi-control task that is too dense for an alert. The sheet uses one
shared tonal field, one separated white-equivalent role group and one
safe-area/IME-aware filled completion action. **Restore Default Relays** is a
full-width filled-tonal Material button because it is a significant explicit
command, not another setting row. Remove and restore remain focused Material
alert dialogs.

## Behavior and state

The Add Relay action is disabled until the normalized URL is valid and unique
and at least one role is selected. Distinct URLs retain distinct internal row
identities even when their readable URL slugs collide, so role changes and
removal remain scoped to one relay. Submission delegates to the existing state
mutation; a rejected race remains in the sheet with the validation error.
Role changes remain immediate. Connected versus not connected is always
represented by both color and check/close shape while exact Reconnecting or
Disconnected wording remains available to accessibility. Restore is disabled
while the relay list exactly matches the defaults. Transient sheet/dialog
visibility and the add draft survive ordinary Compose recomposition; product
mutations remain in the existing profile state holder.

## System integrations

None. The URL field requests the URI keyboard. The prototype does not contact
the endpoint or open a platform networking surface.

## Accessibility and adaptation

Every relay row is one button target and announces name, URL, read-only state
when relevant, and the exact connection status. Status is not color-only:
check and close symbols distinguish connected and not connected in every
theme. Whole role rows are switches with native semantics; the trailing
switch contributes no duplicate node. URL text may middle-ellipsize visually
but its merged row semantics retain the complete value. Sheet content scrolls,
honors IME and safe-area insets, and keeps the action reachable. Layouts retain
the shared bounded adaptive width, directional padding, font scaling,
localization, RTL, keyboard focus, minimum touch targets and system Back.

## Governing Android sources

- [Android Settings patterns](https://developer.android.com/design/ui/mobile/guides/patterns/settings) — list hierarchy, grouped settings, supporting status and concise copy.
- [Compose switches](https://developer.android.com/develop/ui/compose/components/switch) — whole-row binary roles and native switch behavior.
- [Compose buttons](https://developer.android.com/develop/ui/compose/components/button) — filled-tonal Restore Default Relays action and disabled state.
- [Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) — Add Relay task container, dismissal and Material ownership.
- [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog) — focused remove and restore confirmations.

## iOS parity evidence

- `docs/port/source-map.md`
- `/Users/vladimirkrstic/Workspaces/wn-ios-prototype/docs/screens/settings.md`
- `/Users/vladimirkrstic/Workspaces/wn-ios-prototype/WhiteNoisePrototype/Screens/Settings/PrivacyAndRelaySettingsViews.swift`
- `/Users/vladimirkrstic/Workspaces/wn-ios-prototype/WhiteNoisePrototypeTests/RelayAvailabilityTests.swift`

## Approved differences and custom exceptions

The iOS reference uses check/error ornaments, an iOS navigation composition,
an iOS form sheet, and glass-style confirmation controls. Android uses
Material list rows, its app bar and Back model, a Material modal bottom sheet,
standard alerts and semantic Material colors. The app-owned two-dp group gaps
and white-equivalent settings surfaces are the already approved White Noise
Settings treatment.

## Observable acceptance criteria

- Relays display as separated name/URL rows with a filled green connected or
  filled red not-connected compact 20 dp indicator and an exact announced
  connection status; Add Relay is the final group action and no pinned page
  action remains.
- Restore Default Relays is a full-width Material button, disabled while the
  seven defaults and role assignments are already active.
- Relay detail is titled Relay, uses compact metadata rows, and exposes the
  exact three Use For switches with accepted help text. Its URL is normal
  body text on one line, uses the available width, and middle-ellipsizes only
  when necessary.
- Add Relay opens a Material task sheet, rejects invalid, duplicate or
  role-empty drafts, and completes only a valid deterministic add.
- Remove Relay and Restore Default Relays require their focused confirmations;
  Cancel and Back do not mutate state.
- Read-only relays cannot change roles or expose Remove Relay.
- The flow remains usable with TalkBack, large text, dark theme, RTL, keyboard,
  compact and expanded widths, IME and system/predictive Back.
