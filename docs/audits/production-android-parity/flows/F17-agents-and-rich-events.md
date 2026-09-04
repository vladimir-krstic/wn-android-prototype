# F17 — AI-agent setup, streaming and rich event content

## Purpose and current composition

Agent setup copies public-key prompts only; rich event cards use verified local fixtures and preserve raw reference for copying. Loading/retry never initializes networking.

Prototype surface: `ui/settings/SettingsComponents.kt; model/ChatModels.kt; ui/conversation/TimelineMessageContent.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **AI agents**, **Connectors**, **Manual setup**, connector product names, **Show setup prompt**, **Copy prompt**, streaming operation status, **Open event**, **Copy event reference**, **Article**, and clear invalid/not-found/retry states.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C112 · AI-agent connector setup guide | Active-profile public key or unavailable-key recovery | Open Settings → AI Agents; reveal/copy a connector prompt; open documentation; Back returns to Settings | Hermes/OpenClaw/OpenCode/Codex prompts contain only the exact public key, manual setup and browser failure are explicit, and no installation or connection runs. [B29 evidence](../../../screens/ai-agents-and-streaming-operations.md#implementation-evidence). |
| C113 · Streaming agent messages and operation rows | Ordinary partial text plus typed operation phases | Expand an operation for details; message actions remain available; developer injection replaces its owned rows | Streaming text and queued/running/completed/failed/cancelled/unavailable rows use text, progress and details independent of Streaming Debug. [B29 evidence](../../../screens/ai-agents-and-streaming-operations.md#implementation-evidence). |
| C114 · Verified Nostr event cards and recovery | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Typed fixtures for supported note/article/image/video/document/event kinds, loading/not-found/invalid/unavailable/retry; preserve authored reference and Copy. No relay/network resolution. |
| C115 · Article reader and referenced-video viewer | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Reuse rich reader and native local-video viewer for event content, loaded metadata and retry states; remote video remains a local fixture. |

## Production integration seam

Production evidence for each row is linked in the [matrix](../capability-matrix.md). During prototype work, add the smallest profile-owned immutable fixture/state transitions and callbacks needed to render every named result. Do not add Marmot, networking, signing, persistence, notification delivery, background services or cryptography. Name production events and ownership in the selected screen brief so the eventual migration reconnects to the cited controller/state methods rather than copying prototype fixtures into production storage.

## Copy, accessibility and adaptation

Use the approved product language and terminology. Production strings in the matrix are evidence of meaning, not automatic final copy. Keep raw keys, event IDs, MLS and engine errors off ordinary surfaces; developer surfaces may be exact. State must be conveyed by text/semantics as well as icon/color. Provide accessible equivalents for gestures, logical focus and Back order, and preserve action eligibility at large type and narrow height.

## Acceptance and host validation

- Every linked capability has a deterministic route/fixture and every mutation yields the specified success, cancellation, unavailable and failure outcomes relevant to it.
- Back, profile switching and restored state cannot commit work to the wrong profile/chat or repeat a completed mutation.
- Existing capabilities in this flow retain their current model and UI tests.
- Add unit tests for rules/ownership and Compose tests for durable navigation/actions/semantics. Run targeted host tests while iterating and the repository static gate after a meaningful batch. Compile instrumentation tests only; device execution and visual acceptance require a separate current request.

## Dependencies and decisions

Batches: B29, B30. Decisions: None. B29 is implemented and host-verified under [WN-ANDROID-0149](../../../decisions.md#wn-android-0149--agent-operations-are-ordinary-content-raw-streams-stay-developer-only); B30 remains. Facts are the matrix's cited production behavior and current prototype evidence.
