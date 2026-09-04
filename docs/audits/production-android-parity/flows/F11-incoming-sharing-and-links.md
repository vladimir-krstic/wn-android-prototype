# F11 — Incoming sharing and external entry points

B21/C084–C088 are implemented and host-verified within the local scope. The
[selected brief](../../../screens/incoming-sharing-and-profile-links.md#implementation-evidence)
records 693 passing unit tests, nine compiled UI cases and exact migration seams.
Q06 still excludes real exported receiving/provider I/O and shortcut publication.
Device and visual acceptance remain pending.

## Purpose and current composition

Stage external requests at the app boundary and select profile/chat before editing the draft. Back cancels staging and returns to prior task. Consume requests exactly once and reject stale ownership.

Prototype surface: `model/IncomingSharing.kt; model/ProfileLinks.kt; state/IncomingController.kt; ui/share/IncomingShareUi.kt; navigation/WhiteNoiseNavHost.kt; ui/settings/ShareConnectScreen.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Share to**, **Search chats**, **Recent chats**, **No chats to share to**, **Share**, and account/target unavailable recovery. Shared content is staged for review in a draft; do not say **Sent** at the picker.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C084 · Receive Android shared text and attachments | Owned incoming payload | Prepare; Back cancels | Text/media/document descriptors, MIME precedence, deduplication, caps and distinct failures are implemented. Real receiving remains Q06. |
| C085 · Share destination/profile picker and multi-chat drafts | Prepared payload and selected profile | Select destinations; Share stages drafts | Existing draft content is merged without send. Open first and report other drafts/overflow. Opening retry never repeats staging. |
| C086 · Direct Share and conversation shortcuts | Direct or conversation target metadata | Validate owner; fall back or recover | Direct mismatch opens picker; conversation target retains exact owner and missing-target Go to chats recovery. Publication remains Q06. |
| C087 · Cross-client profile links and QR provenance | Public profile or recipient input | Share, scan or search; reject invalid input | Canonical marmot/QR provenance and supported public forms resolve actual identities. Secret/checksum/unknown-route rejection is implemented. |
| C088 · Lock/sign-in/deferred external route ownership | Owned queued/preparing/opening request | Defer, replace, cancel or activate | Fresh IDs, bare-launcher preservation, sign-in/lock deferral and route/profile/sign-out invalidation prevent stale or duplicate consumption. |

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

Batches: B21 complete within local scope. Decisions: Q06 retains real receiving/publication outside scope. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.
