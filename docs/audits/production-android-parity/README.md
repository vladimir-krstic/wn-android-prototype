# Production Android parity audit

Status: **source audit complete; implementation has not started**

Production source is [`marmot-protocol/whitenoise-android`](https://github.com/marmot-protocol/whitenoise-android) `master` at [`319454889f1c2494dec4a69b5577d98017f44eee`](https://github.com/marmot-protocol/whitenoise-android/commit/319454889f1c2494dec4a69b5577d98017f44eee). The comparison target is this prototype's current dirty working tree based on `4c3f7366bcb738839f4969d762403adfc023b8a3`, reconciled on 2026-09-03 after concurrent local work completed; generated audit files are excluded from its hash manifest.

## Verdict

Production exposes a much larger Android product surface. The largest additions are chat folders and cross-chat search; message editing, richer deletion/forwarding and history recovery; photo editing, device contacts, location and text-attachment reading; speech transport, auto-read and dictation; group disbanding and custom retention; inbound Android sharing and notification actions; app lock; and per-network media controls. Profile banners/payment addresses, AI-agent setup, rich Nostr event cards, About/licenses and Zapstore updates add smaller complete flows.

The prototype remains the presentation authority. This audit does not recommend copying production's present styling. It maps each production outcome into the prototype's quiet monochrome Material system, shared 4/8 dp rhythm, semantic status roles, Android Back, system surfaces and accessibility rules. Production's full-spectrum accents/bubbles conflict with the approved monochrome identity and therefore remain recorded but decision-blocked.

## Authoritative counts

The [capability matrix](capability-matrix.md) contains **122** user-outcome capabilities across **18** flows. The denominator is every reachable production surface found from the app phase, main shell, Settings destinations, conversation/group entry points, manifest external entries and distribution gates, plus prototype-only preservation checks.

| Status | Count |
| --- | ---: |
| covered | 16 |
| partial | 41 |
| missing | 59 |
| behavioral divergence | 5 |
| unverified | 0 |
| excluded with reason | 1 |


Of the 122 rows, 105 require implementation work. **82** are ready within the established prototype boundary; **23** require a linked product/platform decision. A decision blocks only the named slice, so other batches can proceed. One covered capability also links to a non-blocking destination decision.

## Proposed destination changes

New top-level or detail destinations are needed for Folders, Folder editor, global search filters/results, Read Aloud settings, Dictation settings, AI Agents, Help, About & licenses and production-style update state. Existing Conversation, Composer, Chat/Group Info, Shared Content, Profile, Keys, Notifications, Appearance, Device Privacy, Data and Storage, Relays, Key Packages, Diagnostics and onboarding surfaces gain behavior and state.

The first recommended batch is **B01 — Access failures, retained profiles and signer choice**. It adds a reusable deterministic external-capability pattern and closes dangerous ambiguity around key types and incomplete setup before later external routes are introduced. B05–B07 (Chats organization and search) provide the largest visible feature gain after that foundation.

## Deliverables

- [Source baselines and coverage](sources-and-coverage.md)
- [Authoritative capability matrix](capability-matrix.md)
- [Flow specifications](flows/)
- [Ordered implementation batches](implementation-plan.md)
- [Decisions and questions](decisions-and-questions.md)
- [Reusable implementation-agent prompt](implementation-agent-prompt.md)

## Limits and verification

Document validation passed: all 122 capability rows map to their flow specifications, all 105 actionable rows map to the 32 batches, and all nine decision scopes agree with the matrix. The pinned archive checksum, 311 production source links, 415 local links and line references, 360 prototype file hashes, and documentation whitespace checks passed.

This was a static source audit. No production or prototype build, device, emulator, `adb`, network runtime, external signer, notification, background service, installer or visual inspection was run. Production's Marmot binding artifact and live relay/store behavior were not executed; user-visible states that cross those boundaries are specified as deterministic fixtures and their real integration seams are cited. Source presence and existing tests are evidence of an implemented code path, not proof that it works on every device. Visual acceptance remains a separate user-led gate.
