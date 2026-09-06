# Production Android parity audit

Status: **B01–B32 implemented and host-verified**

Production source is [`marmot-protocol/whitenoise-android`](https://github.com/marmot-protocol/whitenoise-android) `master` at [`319454889f1c2494dec4a69b5577d98017f44eee`](https://github.com/marmot-protocol/whitenoise-android/commit/319454889f1c2494dec4a69b5577d98017f44eee). The initial comparison target was this prototype's working tree based on `4c3f7366bcb738839f4969d762403adfc023b8a3`, reconciled on 2026-09-03 after concurrent local work completed; generated audit files are excluded from its hash manifest.

## Verdict

Production exposes a much larger Android product surface. The largest additions are chat folders and cross-chat search; message editing, richer deletion/forwarding and history recovery; photo editing, device contacts, location and text-attachment reading; speech transport, auto-read and dictation; group disbanding and custom retention; inbound Android sharing and notification actions; app lock; and per-network media controls. Profile banners/payment addresses, AI-agent setup, rich Nostr event cards, About/licenses and Zapstore updates add smaller complete flows.

The prototype remains the presentation authority. This audit does not recommend copying production's present styling. It maps each production outcome into the prototype's quiet monochrome Material system, shared 4/8 dp rhythm, semantic status roles, Android Back, system surfaces and accessibility rules. B26 completes the selected optional full-spectrum accents/bubbles while preserving monochrome defaults and neutral surfaces.

## Authoritative counts

The [capability matrix](capability-matrix.md) contains **122** user-outcome capabilities across **18** flows. The denominator is every reachable production surface found from the app phase, main shell, Settings destinations, conversation/group entry points, manifest external entries and distribution gates, plus prototype-only preservation checks.

| Status | Count |
| --- | ---: |
| covered | 121 |
| partial | 0 |
| missing | 0 |
| behavioral divergence | 0 |
| unverified | 0 |
| excluded with reason | 1 |


All 121 in-scope rows are covered; C116 remains excluded with its recorded
reason. The user resolved Q01 and Q09 by selecting full optional color controls
and complete Russian, Turkish, Simplified Chinese and Traditional Chinese
translations. Q03 is resolved by the separate policy-pruning and message-countdown paths. Covered C049, C058, C059, C066, C070, C084, C086, C090 and C121 retain Q06 for real library access, location/maps, installation, background services, exported receiving, shortcut publication, background notification services and APK installation outside prototype scope; C053 links to the now-recorded Q07 document-destination choice.

## Destination changes

The parity work adds Folders, Folder editor, global search filters/results, Read Aloud settings, Dictation settings, AI Agents, Help, About & licenses and distribution-gated update state. Existing Conversation, Composer, Chat/Group Info, Shared Content, Profile, Keys, Notifications, Appearance, Device Privacy, Data and Storage, Relays, Key Packages, Diagnostics and onboarding surfaces gain behavior and state.

Batch-specific changes, historical test totals, and evidence links are maintained
in [the implementation record](implementation-plan.md). For current presentation
and later approved changes, use [the screen briefs](../../screens/README.md).
Native voice search and live composer dictation now use device recognition under
the 2026-09-05 approval; the original B07/B17 deterministic acquisition limits
are historical. The retired device-contact/recent-media composer entries are
covered by the later attachment-menu decision.

## Deliverables

- [Source baselines and coverage](sources-and-coverage.md)
- [Authoritative capability matrix](capability-matrix.md)
- [Flow specifications](flows/)
- [Ordered implementation batches](implementation-plan.md)
- [Decisions and questions](decisions-and-questions.md)

## Limits and verification

At the initial audit on 2026-09-03, document validation passed: all 122 capability rows map to their flow specifications, all 105 actionable rows map to the 32 batches, and all nine decision scopes agree with the matrix. The pinned archive checksum, 311 production source links, 415 local links and line references, 360 prototype file hashes, and documentation whitespace checks passed.

The initial audit was static; subsequent host verification is tracked in each implementation brief. No device, emulator, `adb`, production network runtime, external signer, notification, background service, installer or visual inspection has been run for this implementation goal. Production's Marmot binding artifact and live relay/store behavior were not executed; user-visible states that cross those boundaries are specified as deterministic fixtures and their real integration seams are cited. Source presence and existing tests are evidence of an implemented code path, not proof that it works on every device. Visual acceptance remains a separate user-led gate.

B25 completes key-package and developer inspection states within the approved gate.
[Its evidence](../../screens/key-packages-and-developer-diagnostics.md#implementation-evidence) records 772 passing unit tests and six compiled UI cases. Q04 retains only ordinary Settings relocation/relay-policy decisions; it does not block the implemented developer states.

B26 completes AMOLED, optional action/global/per-chat colors, licensed font
families, font size, complete new locale catalogs and Enter behavior. The final
host gate passes 892 unit tests with zero failures/errors/skips, zero lint errors
and both debug APKs; ten B26 UI cases compile only. [The brief](../../screens/appearance-typography-and-input.md#implementation-evidence)
records Q01/Q09 resolution and exact evidence.
