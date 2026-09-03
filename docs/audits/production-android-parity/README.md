# Production Android parity audit

Status: **B01–B04 implemented and host-verified; B05–B32 pending**

Production source is [`marmot-protocol/whitenoise-android`](https://github.com/marmot-protocol/whitenoise-android) `master` at [`319454889f1c2494dec4a69b5577d98017f44eee`](https://github.com/marmot-protocol/whitenoise-android/commit/319454889f1c2494dec4a69b5577d98017f44eee). The comparison target is this prototype's current dirty working tree based on `4c3f7366bcb738839f4969d762403adfc023b8a3`, reconciled on 2026-09-03 after concurrent local work completed; generated audit files are excluded from its hash manifest.

## Verdict

Production exposes a much larger Android product surface. The largest additions are chat folders and cross-chat search; message editing, richer deletion/forwarding and history recovery; photo editing, device contacts, location and text-attachment reading; speech transport, auto-read and dictation; group disbanding and custom retention; inbound Android sharing and notification actions; app lock; and per-network media controls. Profile banners/payment addresses, AI-agent setup, rich Nostr event cards, About/licenses and Zapstore updates add smaller complete flows.

The prototype remains the presentation authority. This audit does not recommend copying production's present styling. It maps each production outcome into the prototype's quiet monochrome Material system, shared 4/8 dp rhythm, semantic status roles, Android Back, system surfaces and accessibility rules. Production's full-spectrum accents/bubbles conflict with the approved monochrome identity and therefore remain recorded but decision-blocked.

## Authoritative counts

The [capability matrix](capability-matrix.md) contains **122** user-outcome capabilities across **18** flows. The denominator is every reachable production surface found from the app phase, main shell, Settings destinations, conversation/group entry points, manifest external entries and distribution gates, plus prototype-only preservation checks.

| Status | Count |
| --- | ---: |
| covered | 32 |
| partial | 34 |
| missing | 50 |
| behavioral divergence | 5 |
| unverified | 0 |
| excluded with reason | 1 |


After B04, 89 of the 122 rows still require implementation work. **69** are ready within the established prototype boundary; **20** require a linked product/platform decision. A decision blocks only the named slice, so other batches can proceed. One covered capability also links to a non-blocking destination decision.

## Proposed destination changes

New top-level or detail destinations are needed for Folders, Folder editor, global search filters/results, Read Aloud settings, Dictation settings, AI Agents, Help, About & licenses and production-style update state. Existing Conversation, Composer, Chat/Group Info, Shared Content, Profile, Keys, Notifications, Appearance, Device Privacy, Data and Storage, Relays, Key Packages, Diagnostics and onboarding surfaces gain behavior and state.

**B01 — Access failures, retained profiles and signer choice** is implemented and host-verified. It covers typed access results, explicit recovery consent, retained-profile re-entry and startup retry. See [its implementation evidence](../../screens/access-and-recovery.md#implementation-evidence). B02 adds temporary key exports and staged profile exit; [its evidence](../../screens/keys-and-profile-exit.md#implementation-evidence) records 216 passing unit tests. Q05 is resolved by the approved wipe default. B03 adds people discovery, private contact details and profile-to-group actions; [its evidence](../../screens/people-discovery-and-private-details.md#implementation-evidence) records 231 passing unit tests. B04 adds profile banners, image viewing, Lightning validation and draft-only name suggestions; [its evidence](../../screens/profile-media-and-lightning.md#implementation-evidence) records 244 passing unit tests. The next batch is B05. B05–B07 (Chats organization and search) provide the largest visible feature gain after that foundation.

## Deliverables

- [Source baselines and coverage](sources-and-coverage.md)
- [Authoritative capability matrix](capability-matrix.md)
- [Flow specifications](flows/)
- [Ordered implementation batches](implementation-plan.md)
- [Decisions and questions](decisions-and-questions.md)
- [Reusable implementation-agent prompt](implementation-agent-prompt.md)

## Limits and verification

At the initial audit on 2026-09-03, document validation passed: all 122 capability rows map to their flow specifications, all 105 actionable rows map to the 32 batches, and all nine decision scopes agree with the matrix. The pinned archive checksum, 311 production source links, 415 local links and line references, 360 prototype file hashes, and documentation whitespace checks passed.

The initial audit was static; subsequent host verification is tracked in each implementation brief. No device, emulator, `adb`, production network runtime, external signer, notification, background service, installer or visual inspection has been run for this implementation goal. Production's Marmot binding artifact and live relay/store behavior were not executed; user-visible states that cross those boundaries are specified as deterministic fixtures and their real integration seams are cited. Source presence and existing tests are evidence of an implemented code path, not proof that it works on every device. Visual acceptance remains a separate user-led gate.
