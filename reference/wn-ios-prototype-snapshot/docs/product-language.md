# White Noise product language

Use this reference for English product-surface copy. The user’s latest
direction, the canonical terms in `docs/terminology.md`, and the current screen
brief outrank examples here.

## Voice

White Noise sounds calm, direct, human, and lightly warm.

- Lead with the task or outcome using familiar words and short sentences.
- Name the action instead of the implementation or protocol.
- Use contractions when they sound natural.
- Keep routine copy neutral. Do not turn controls, errors, or empty states into
  privacy marketing.
- Avoid hype, fear, blame, jokes, exclamation marks, and false reassurance in
  serious states.
- Use sentence case and remove words that do not add meaning.
- Write all product copy as final production copy. Keep prototype, simulation,
  fixture, dummy-data, and implementation-boundary language in documentation or
  developer-only surfaces.

English is the authored language. Native layouts and custom compositions must
still tolerate localization expansion and right-to-left presentation without
inventing translated strings.

## Titles and actions

- Name the task or situation, not the underlying subsystem.
- Give alerts a specific, useful title; do not use **Error**, **Warning**, or an
  internal code as the title.
- Do not repeat a navigation title in the first line of body copy.
- Label buttons with the result: **Sign In**, **Retry**, **Open Settings**,
  **Remove Profile**.
- Use **Sign Out** for the session-ending action. In its confirmation sheet,
  **Wipe Data From This Device** is selected by default; when it is selected,
  state that the profile and local data are permanently removed and previous
  chats won't return. Keep the final **Sign Out** role destructive whether
  wiping is selected or not; present it as a full-width red in-sheet action
  with a regular-weight label.
- Use **Erase App Data** for the uncommon device-wide destructive action.
  State that it signs out every profile and permanently removes all local White
  Noise data. Use **Erase** for the final full-width red in-sheet action. Do
  not offer or refer to a device-wide sign-out that retains local data.
- Sign Out and Erase App Data are large confirmation sheets. Use the leading
  Close icon for dismissal; don't add a trailing completion action or a text
  **Cancel** button to these sheets.
- Use **Cancel** for cancellation and the exact destructive action in a
  confirmation.
- Avoid **OK**, **Submit**, **Proceed**, **Yes**, and **No** when a clearer action
  exists.
- Do not explain an already-clear button in adjacent text.

## Fields and help

- Label information people recognize, not the format the app parses.
- Keep persistent labels visible; placeholders are examples or brief hints, not
  replacements for labels.
- Add concise format guidance only when it prevents a likely error.
- Never place private keys or other sensitive values in examples, screenshots,
  logs, finished errors, or accessibility labels and values.

## Empty, progress, and success states

- State what is absent in the current context and offer the most useful next
  action when one exists.
- Use the action in progress: **Signing In…**, **Creating Profile…**,
  **Signing out…**, **Signing out and wiping data…**.
- Keep progress labels stable and near the initiating action.
- Confirm the result without *successfully*: **Profile Created**, **Copied**.
- Do not add success copy when the resulting screen already makes the outcome
  obvious.

## Errors and recovery

Use this order:

1. State what could not be completed.
2. Explain the useful reason or consequence when known.
3. Provide the next action.

- Prefer **Couldn’t…** for action failures rather than **Failed to…**.
- Never expose raw engine text, localized error descriptions, protocol codes,
  payloads, or internal identifiers as finished product copy.
- Use a generic fallback only when the app cannot distinguish the cause; still
  name the attempted action and offer recovery.

### Relay ownership and recovery

- Treat relays and role assignments as properties of the active profile. Use
  **this profile** in confirmations and consequences, **your profile** in
  explanatory copy, and White Noise only when the app itself is the actor.
- Use **Profile relays need attention** when a required relay role is
  unassigned, reconnecting, or disconnected. Do not describe the complete app
  as offline or broken.
- Name only what is unavailable: **Profile publishing**, **chat invitations**,
  or **new chats**.
- Group recovery by cause instead of emitting one full sentence per role.
  Use **Choose a relay for…** for unassigned roles, **…relays are
  reconnecting** while recovery is in progress, and **No … relay is
  connected** when assigned relays are disconnected. Follow the grouped cause
  sentences with one concise combined impact using **publishing**,
  **invitations**, and **new chats**. Use **temporarily unavailable** only when
  every affected role is reconnecting. Keep the complete explanation on
  **Relays**;
  Chats may use a compact warning in the New Message toolbar slot, while an
  open conversation uses the complete empty **Check your profile relays**
  composer as the same recovery action. Both link to Relays without repeating
  the full explanation.
- Before a final role is turned off, state that this profile needs at least one
  relay for that role, then name the exact consequence. Before a relay is
  removed, name any capability that removal makes unavailable. Use **Turn
  Off** or **Remove Relay** rather than a generic confirmation.
- Use **Restore Default Relays** and **Restore Defaults** for the confirmed
  full reset. State that the default relays and role assignments will replace
  the current profile configuration and that custom relays will be removed.

## Destructive actions

- Name exactly what is removed.
- Explain the unique consequence once, including what remains and whether the
  action is recoverable.
- Distinguish device-local removal from effects elsewhere.
- Use a native destructive role and a safe **Cancel** action.
- Do not soften irreversible loss or repeat the same warning in the title,
  message, and button.

## Permissions

- Explain the feature benefit immediately before the system prompt when the
  surrounding context does not already make it clear.
- Let iOS present its permission alert.
- After denial, state what is unavailable and offer **Open Settings** when that
  resolves it.

## App privacy and authentication

- Use **Require Face ID** for return authentication on the supported iPhone
  experience. The device passcode remains the system fallback; don’t introduce
  a separate White Noise PIN or call the preference **Face ID Lock**.
- If device authentication is unavailable because no iPhone passcode exists,
  name that requirement directly without blaming the person.
- Use **Hide Screen in App Switcher** for snapshot privacy. Don’t call it
  screenshot blocking or imply that it hides active screen recording.

## Accessibility copy

- Describe the action or current value, not the symbol’s appearance.
- Avoid words the native control trait already announces.
- Keep visible, spoken, and Voice Control names consistent.
- Never announce private keys or other sensitive values.
- Do not use color, sound, animation, or haptics as the only expression of a
  state.

## Product and developer surfaces

Ordinary onboarding, Chats, Profile, Settings, permission, and recovery UI uses
the human terms in `docs/terminology.md`. Developer Tools and Diagnostics may
use exact protocol and implementation terms when precision is their purpose.

When a technical value must appear in ordinary UI, introduce the human label
first and show the technical form secondarily only where recognition helps
complete the task.

## Apple writing authority

- [Writing for interfaces](https://developer.apple.com/videos/play/wwdc2022/10037/)
- [Alerts](https://developer.apple.com/design/human-interface-guidelines/alerts)
- [Feedback](https://developer.apple.com/design/human-interface-guidelines/feedback)

These live Apple sources govern interface structure and writing behavior. This
local reference owns White Noise voice and terminology.
