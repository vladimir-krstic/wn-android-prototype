# AI agents and streaming operations

## Purpose

Let a signed-in person prepare a trusted AI agent connection with their public
identity, then understand agent text and operation progress inside an ordinary
conversation.

## Scope and non-goals

B29 covers C112 and C113: a Settings destination for Hermes, OpenClaw,
OpenCode and Codex setup prompts; public-key copy and manual contact guidance;
an external connector-documentation handoff; streaming message presentation;
and expandable queued, running, completed, failed, cancelled and unavailable
operation rows.

The prototype does not install a connector, create an agent account, resolve an
agent, send a setup request, start transport, access Marmot, sign or encrypt,
persist state, or fetch documentation itself. B30 owns referenced Nostr event
cards and readers.

## Parity contract

- AI Agents is an ordinary Settings destination for the active signed-in
  profile.
- Setup actions require a usable `npub`. A missing or non-public value disables
  every prompt action and explains recovery.
- Each connector has distinct explanatory copy and a setup prompt containing
  the complete active profile public key. No private key is included.
- Prompt text can be reviewed before copying. Copying changes no app or agent
  state.
- Manual setup tells the person to add the public key returned by the agent
  through New Message.
- Documentation opens through an Android browser intent and reports when no
  handler accepts it.
- A partially delivered agent response remains a normal message with visible
  Streaming status.
- Agent operations remain ordinary timeline content when Developer Tools is
  disabled. They never depend on Streaming Debug.
- Operation presentation names the operation, shows a textual phase and
  summary, uses indeterminate progress while queued and determinate progress
  when totals are known, and reveals request/result/status/duration details on
  demand.
- Developer-only conversation controls add stable local examples. Repeating the
  action replaces the owned rows rather than duplicating them.
- The example insertion requires Developer Tools, an exact active profile and
  chat, and active membership. A stale profile callback or ended conversation
  changes nothing.

## Entry, navigation, Back, and exit

Settings → AI Agents opens the typed `AppRoute.AiAgents` destination. App-bar
Back and system Back return to Settings through Navigation Compose. The selected agent is route-local saveable UI state, scoped to the active
profile and public key. Selecting a row opens a Material bottom sheet. Close,
system Back, or outside dismissal returns to the list without copying anything;
leaving the screen cannot perform or repeat setup.

Conversation Debug → Add agent conversation examples adds local rows to that
exact chat. Returning to the conversation presents those rows through the
ordinary timeline. Profile switching invalidates the old insertion callback.

## Exact product copy

All new interface copy is in `values/strings.xml`. The ordinary vocabulary is
**AI Agents**, **Chat with your AI agent**, **Choose an agent**, **Manual setup**,
**Set up [agent]**, **Copy prompt**, **Copy public key**, **Agent operation**,
**Queued**, **In progress**, **Completed**, **Failed**, **Cancelled** and
**Unavailable**. Setup copy states that the prompt contains a public key and
that the external agent must ask for approval before installation.

Technical example injection remains under Conversation Debug. Raw event kinds,
transport records and developer stream details never enter the ordinary
operation card.

## Android composition

The screen reuses the established Settings scaffold, adaptive content bounds,
white segmented groups, stable row shapes, 4/8 dp rhythm and app bar. A gray
info callout introduces externally hosted agents. Each agent is one native
clickable row with its name, short description, and chevron. Supporting guidance
sits below the group.

A selected agent opens the shared `WhiteNoiseModalBottomSheet` with a Close
header, concise gray instruction callout, white selectable prompt surface, and
next-step guidance. The body scrolls independently while one standard 56 dp
`WhiteNoiseButton` copies the exact prompt. Copy feedback is visible and announced
beside that action. The sheet uses the established 88% window-height cap and
shared safe-drawing insets; Material owns width, shape, dismissal, and gestures.

Manual setup provides a compact copy row with the abbreviated public key and a
copy icon, plus the documentation row. The complete key remains in the clipboard
payload and setup prompt. Missing public identity disables agent rows and key
copy, with a semantic error callout near the introduction. Documentation and
manual-copy outcomes use the shared callout. Operation details remain selectable
monospace text.

The timeline uses a bounded neutral Material surface. Text labels communicate
every phase; semantic error color reinforces Failed. Material linear progress
is indeterminate for queued work and determinate for known running totals. The
entire expandable card uses native combined-click semantics and preserves the
existing long-press message-action path.

## Behavior and state

`AgentSetupPolicy` owns connector availability and rejects blank or non-`npub`
identity values. `AgentOperation` owns immutable phase, bounded progress,
request/result/status details and optional duration. `AgentConversationExamples`
builds stable chat-owned rows for partial streaming and all operation outcomes.

`AppViewModel.addAgentConversationExamples` performs the developer gate and
exact active-profile/chat check before a synchronous immutable chat update.
There is no asynchronous task to survive Back, repeat after recreation or
write into a newly selected profile.

## System integrations

The setup screen uses the existing Android clipboard helper for user-initiated
public-key and prompt copy. Connector documentation uses `ACTION_VIEW` with an
HTTPS URI. It adds no permission or in-app networking and exposes a visible
failure result when the handoff cannot start.

## Accessibility and adaptation

- Visible text and `stateDescription` communicate operation phase without
  relying on color or motion.
- The progress count is written as “current of total steps”; queued work has a
  textual Queued label beside its indeterminate indicator.
- Agent rows retain native button semantics and touch targets. The sheet body
  scrolls under large type, keeping the standard copy button reachable.
- Prompt and detail text is selectable, while the copied identity is public.
- The existing scaffold owns safe drawing insets and adaptive width. Lists
  remain scrollable at 200% text, and start/end layout supports RTL.
- App-bar/system Back use the standard route stack. No custom edge gesture is
  introduced.

## Governing Android sources

- [Bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
  — a focused prompt-review task uses Material dismissal and sheet state
  (consulted 2026-09-05).

- [Navigation](https://developer.android.com/guide/navigation) — a typed
  destination and the existing NavHost own entry and Back.
- [Progress indicators](https://developer.android.com/develop/ui/compose/components/progress)
  — determinate progress is used only with a known total; queued work is
  indeterminate.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
  — visible status is supplemented with phase state semantics.
- [Common intents](https://developer.android.com/guide/components/intents-common#Browser)
  — connector documentation leaves through the system browser handoff.
- [Secure clipboard handling](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling)
  — only the explicitly shown public key and prompt are copied; no secret is
  introduced.

## Production parity evidence

- `AiAgentsScreen.kt` at pinned production commit `31945488`: connector prompt
  preview/copy, manual `npub` setup and documentation entry.
- `AgentConnector.kt` at the same commit: Hermes, OpenClaw, OpenCode and Codex
  connector set.
- `AgentOperationPresentation.kt` and `AgentOperationRow.kt` at the same commit:
  ordinary typed operation summary/status/details/duration presentation.
- `StreamDebug.kt` at the same commit: developer-only raw streaming records,
  kept separate from ordinary operations here.
- [F17 audit contract](../audits/production-android-parity/flows/F17-agents-and-rich-events.md)
  and matrix rows C112/C113.

## Approved differences and custom exceptions

The prototype keeps its accepted monochrome Settings and conversation system
instead of copying production layout or accents. Local conversation examples
stand in for production operation records, and the browser handoff is the only
external action. No custom gesture, permission or third-party dependency is
added.

## Observable acceptance criteria

- Settings exposes AI Agents and Back returns to Settings.
- All four agent rows open a setup sheet and copy a distinct prompt containing
  the exact active profile public key; an unavailable key disables these actions.
- Closing a setup sheet without copying performs no action.
- Manual setup can copy the public key and reports documentation handoff
  failure.
- Streaming text has visible progress metadata.
- Queued/running operation rows show progress; completed, failed, cancelled and
  unavailable rows have explicit textual outcomes; details expand and collapse.
- Operation rows remain after Developer Tools is disabled, while injection and
  raw stream records stay developer-only.
- Stale profile, unavailable chat, ended membership and repeated insertion are
  safe.

## Implementation evidence

B29 implementation uses `AgentOperations.kt`, `AiAgentsScreen.kt`,
`AgentOperationUi.kt`, the typed navigation route, exact-profile AppViewModel
mutation and developer-only example action. `AgentOperationStateTest` adds eight
rules/ownership regressions; `AgentFeaturesInteractionTest` adds seven compiled
navigation, copy, unavailable, progress, terminal-state and large-type cases.

The complete host gate
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes 817 unit tests with zero failures/errors/skips, zero lint errors (14
existing warnings and two hints), and both APKs. All seven new Compose cases
compile only. Device execution, screenshots and visual acceptance were not
requested and are not claimed.

## 2026-09-05 — agent setup design polish

The user requested clearer hierarchy and recognizable actions, matching the
shared Google/Material presentation established in the preceding polish pass.
This replaces repeated inline preview/copy buttons with agent choices and a
focused setup sheet. All four original prompt payloads, public-key ownership,
clipboard behavior, manual setup, and browser handoff remain intact. Introduction
and instructions are shortened in all five supported resource sets.

`AgentFeaturesInteractionTest` now covers opening and copying every agent prompt,
closing without copying, unavailable keys, manual copy/documentation failure,
and large-type reachability of the sheet action. The complete host gate
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes: 894 unit tests, zero failures/errors/skips, zero lint errors, and both
debug APKs assembled. The eight Compose cases compile only; no device execution
or visual acceptance is claimed.
