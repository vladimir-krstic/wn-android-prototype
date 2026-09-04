# F09 — Group setup, roles and lifecycle

## Purpose and current composition

Use current Group Info/Edit/Profile member flows. State comes from authoritative deterministic roster and management fixtures. Back never cancels already accepted group commits; show convergence instead.

Prototype surface: `ui/conversation/ChatInfoScreens.kt; model/ChatModels.kt; state/AppViewModel.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Use **Group info**, **Edit group info**, **Add members**, **Grant/Revoke admin**, **Transfer admin**, **Step down as admin**, **Leave group**, **Disband group**, **Export conversation transcript**, and named pending/failure consequences. “Disband” always says it ends the group for everyone.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C071 · Group creation, edit and member selection | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C072 · Empty group setup and timer during creation | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add explicit no-other-member setup and initial timer; distinguish created group from failed timer application and failed opening. |
| C073 · Private group image and emoji-generated avatar | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | One/two emoji image, unsupported/limit states, private group photo versus public invite avatar, image loading/upload retry. Preserve public preview continuity. |
| C074 · Role and roster mutation pending/failure | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add unknown/loading/failed roster, pending invites, per-person mutation lock, retry and stale completion handling before enabling role-sensitive commands. |
| C075 · Promote, revoke and remove member | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C076 · Transfer administration and step down | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add explicit transfer-and-step-down and transfer-then-leave completion, partial failure and sole-member deletion case. A blocked leave dialog alone does not cover this flow. |
| C077 · Disband group for everyone | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add capability-enable, engine-declared blockers, confirmation, pending convergence, failure acknowledgment/retry and permanently ended state. Distinguish local delete and ordinary leave. |
| C078 · Frozen/unrecoverable, verifying and disbanded composer | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add verified-role unknown, unrecoverable and disbanding/disbanded states; block send/reaction/edit until permitted; preserve readable history and recovery explanation. |
| C079 · Conversation transcript export | Deterministic gap fixture | Trigger its named entry/action; cancel with Back where available | Add export progress, unavailable source, cancellation/error and completed local document handoff; preserve ordering, authored identity and edits per export contract. |
| C080 · Technical group identifiers and relay inspection | Decision required | Trigger its named entry/action; cancel with Back where available | Extend developer inspection for MLS/Nostr identifiers, required components and detailed push state; keep raw protocol fields developer-only unless Q04 changes exposure. |

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

Batches: B18, B19, B25. Decisions: Q04. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B18 implementation evidence

C072–C074 are covered on 2026-09-04: explicit solo setup and initial timer;
create-once/timer/open recovery; separate private/public and emoji image editing;
roster loading/unknown/failure, seeded presentation and authoritative commits;
member pending/convergence, primitive locks and exact profile/revision retry.
The [selected brief](../../../screens/group-setup-images-and-roster.md#implementation-evidence)
records 575 passing unit tests, zero lint errors, both APKs and ten new compiled
UI/bitmap cases. Production master warm-roster presentation is reconciled.
B19 lifecycle/transfer/export and B25 technical details remain pending; the table
above preserves the original complete flow contract. Device/visual acceptance is
separate from this host verification.
