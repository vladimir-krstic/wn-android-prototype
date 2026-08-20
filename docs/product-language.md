# White Noise product language

Use this reference for English product-surface copy. The latest user direction,
`terminology.md`, and the selected screen brief outrank examples here. Exact
copy already accepted in iOS remains the parity baseline unless Android needs a
platform-specific capability name.

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
- Write product copy as final production copy. Keep prototype, simulation,
  fixture, dummy-data, and implementation-boundary language in documentation
  or developer-only surfaces.

English is the authored language. Layouts must still tolerate localization
expansion and right-to-left presentation without inventing translations.

## Titles and actions

- Name the task or situation, not the underlying subsystem.
- Give dialogs a specific useful title; do not use **Error**, **Warning**, or an
  internal code as the title.
- Label buttons with the result: **Sign In**, **Retry**, **Open Settings**,
  **Remove Profile**.
- Use **Cancel** for cancellation and the exact destructive verb in a
  confirmation.
- Avoid **OK**, **Submit**, **Proceed**, **Yes**, and **No** when a clearer
  action exists.
- Do not explain an already-clear button in adjacent text.
- Preserve accepted destructive labels such as **Sign Out**, **Wipe Data From
  This Device**, **Erase App Data**, and **Erase**. Translate their iOS sheet
  layout to the correct Android dialog or bottom-sheet pattern without
  rewriting the consequence.

## Fields and help

- Label information people recognize, not the format the app parses.
- Keep labels persistent; placeholders are examples or short hints, not label
  replacements.
- Add format guidance only when it prevents a likely error.
- Never place private keys or other sensitive values in examples,
  screenshots, logs, finished errors, content descriptions, or accessibility
  state descriptions.
- Use Android input types, keyboard actions, autofill semantics, error support
  text, and focus behavior appropriate to the field.

## Empty, progress, success, and transient feedback

- State what is absent in the current context and offer the most useful next
  action when one exists.
- Use the action in progress: **Signing In…**, **Creating Profile…**,
  **Signing out…**.
- Keep progress labels stable and near the initiating action.
- Confirm the result without *successfully*: **Profile Created**, **Copied**.
- Do not add success copy when the resulting screen already makes the outcome
  obvious.
- Use a Snackbar for brief recoverable feedback or an optional undo when that
  is the Android convention. Do not reproduce an iOS toast literally.

## Errors and recovery

Use this order:

1. State what could not be completed.
2. Explain the useful reason or consequence when known.
3. Provide the next action.

- Prefer **Couldn’t…** for action failures rather than **Failed to…**.
- Never expose raw engine text, protocol codes, payloads, stack traces, or
  internal identifiers as finished product copy.
- Use a generic fallback only when the app cannot distinguish the cause; still
  name the attempted action and offer recovery.
- When Android permanently denies a permission or the system owns the setting,
  name what is unavailable and offer **Open Settings** only when it resolves
  the problem.

## Relay ownership and recovery

- Treat relays and role assignments as properties of the active profile.
- Use **this profile** in confirmations and consequences and **your profile**
  in explanatory copy.
- Use **Profile relays need attention** when a required role is unassigned,
  reconnecting, or disconnected. Do not describe the whole app as offline.
- Name only the unavailable capability: **Profile publishing**, **chat
  invitations**, or **new chats**.
- Group recovery by cause. Keep complete explanations on **Relays** and use
  concise recovery links in Chats or an open conversation.
- Use **Turn Off**, **Remove Relay**, **Restore Default Relays**, and **Restore
  Defaults** rather than generic confirmation labels.

## Destructive actions

- Name exactly what is removed.
- Explain the unique consequence once, including what remains, what is only on
  this device, and whether recovery is possible.
- Use Material destructive/error semantics and a safe cancellation path.
- Do not soften irreversible loss or repeat the same warning in the title,
  body, and button.
- Do not rely on red alone; the label and consequence must identify the action.

## Permissions and system surfaces

- Ask only when the selected action needs the capability.
- Explain the feature benefit immediately before the Android system prompt
  when context does not already make it clear.
- Let Android own the permission dialog, picker, Sharesheet, biometric prompt,
  and system settings surfaces.
- Do not mimic a system prompt inside the app or imply a permission is required
  when a permissionless official API is available.

## App privacy and device authentication

- Use **Require device authentication** as the platform-neutral setting label
  until the selected screen brief confirms the exact Android capability and
  authenticator combination. Do not copy **Require Face ID** to Android.
- Let `BiometricPrompt` name and render the supported device authentication
  method. Do not promise face unlock on a device that provides fingerprint or
  device credential instead.
- Use **Hide Screen in Recents** for Android's recent-tasks snapshot privacy.
  Do not call it screenshot blocking or imply it prevents active screen
  recording.

## Accessibility copy

- Describe the action or current value, not the icon's appearance.
- Avoid words that TalkBack already announces from the component role.
- Keep visible labels, content descriptions, and Voice Access names
  consistent.
- Give decorative icons no content description.
- Never announce private keys or other sensitive values.
- Do not use color, sound, animation, or haptics as the only expression of a
  state.

## Product and developer surfaces

Ordinary onboarding, Chats, Profile, Settings, permission, and recovery UI uses
the human terms in `terminology.md`. Developer Tools and Diagnostics may use
exact protocol and implementation terms when precision is their purpose.

When a technical value must appear in ordinary UI, introduce the human label
first and show the technical form secondarily only where recognition helps
complete the task.

## Writing authority

- [Material communication guidance](https://m3.material.io/foundations/content-design/overview)
- [Android accessibility design](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Android permission UX](https://developer.android.com/training/permissions/usage-notes)

These live sources govern Android interaction and capability language. This
local file owns White Noise voice and terminology.

