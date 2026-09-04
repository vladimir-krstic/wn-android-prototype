# Ordered implementation plan

The order follows state ownership and user dependencies. A later batch may be selected early only when all named dependencies already exist. The user authorized one goal for all 32 batches on 2026-09-04, executed as bounded batches with just-in-time briefs. Commit each completed, verified batch as `B01: <description>` using its batch number. No duration estimates are inferred.

## Implementation progress

- B01: implemented and host-verified 2026-09-04; 202 unit tests, zero lint errors, both APKs, six new compiled UI cases. [Selected brief and evidence](../../screens/access-and-recovery.md). Commit title: `B01: Add access recovery and retained-profile sign-in`.
- B02: implemented and host-verified 2026-09-04; 216 unit tests, zero lint errors, both APKs, five new compiled UI cases. [Selected brief and evidence](../../screens/keys-and-profile-exit.md). Q05 preserves the approved checked wipe default. Commit title: `B02: Add staged profile exit and temporary key exports`.
- B03: implemented and host-verified 2026-09-04; 231 unit tests, zero lint errors, both APKs and seven new compiled UI cases. [Selected brief and evidence](../../screens/people-discovery-and-private-details.md). Commit title: `B03: Add people discovery and private contact flows`.
- B04: implemented and host-verified 2026-09-04; 244 unit tests, zero lint errors, both APKs and seven new compiled UI cases. [Selected brief and evidence](../../screens/profile-media-and-lightning.md). Commit title: `B04: Add profile media and Lightning address editing`.
- B05: implemented and host-verified 2026-09-04; 262 unit tests, zero lint errors, both APKs and seven new compiled UI cases. [Selected brief and evidence](../../screens/chat-organization-and-recovery.md). Manual folder assignment provides part of B06. Commit title: `B05: Add chat organization and connection recovery`.
- B06: implemented and host-verified 2026-09-04; 277 unit tests, zero lint errors, both APKs and nine new compiled UI cases. [Selected brief and evidence](../../screens/chat-folders.md). Commit title: `B06: Add folder management and automatic rules`.
- B07–B32: pending; linked decisions apply only to their named slices.
- Device and user visual acceptance remain separate from host verification.

## Sequence

| Batch | Outcome | Capability IDs | Depends on | Decision state |
| --- | --- | --- | --- | --- |
| **B01** | Access failures, retained profiles and signer choice | C001, C003, C004, C005, C011 | None | Ready |
| **B02** | Keys, sign-out and staged wipe outcomes | C007, C008, C009, C010 | B01 | Resolved: approved wipe default retained |
| **B03** | People discovery and private contact data | C013, C014, C019, C021 | B01 | Ready |
| **B04** | Profile banner, avatar viewing and Lightning field | C016, C017, C018 | B03 | Ready |
| **B05** | Chat-list selection, order and connection recovery | C024, C025, C026, C030 | B01 | Ready |
| **B06** | Chat folders end to end | C027, C028, C029 | B05 | Ready |
| **B07** | Cross-chat search and filters | C031, C032, C033, C034 | B05 | Ready |
| **B08** | Conversation history, unread and details recovery | C035, C037, C047 | B05 | Ready |
| **B09** | Message editing, full reader and text selection | C038, C039, C040, C041 | B08 | Ready |
| **B10** | Moderation deletion and resilient forwarding | C043, C044, C045 | B09 | Ready |
| **B11** | Composer media acquisition and attachment actions | C046, C049, C051, C052, C054, C057 | B08 | Blocked slices: Q06 |
| **B12** | Draft photo editor | C050 | B11 | Ready |
| **B13** | Text attachment reader and expanded shared media | C055, C056, C059 | B11 | Blocked slices: Q06 |
| **B14** | Location sharing state flow | C058 | B11 | Blocked slices: Q06 |
| **B15** | Read Aloud transport and source navigation | C061, C062 | B08 | Ready |
| **B16** | Read Aloud preferences and auto-read | C063, C064, C065, C066 | B15 | Blocked slices: Q06 |
| **B17** | Dictation and production voice-note interaction | C067, C068, C069, C070 | B11 | Blocked slices: Q06 |
| **B18** | Group setup image and authoritative roster states | C072, C073, C074 | B03, B11 | Ready |
| **B19** | Administration transfer, disband and transcript export | C076, C077, C078, C079 | B18 | Ready |
| **B20** | Disappearing-message timers and expiry | C081, C082, C083 | B09, B18 | Blocked slices: Q03 |
| **B21** | Inbound sharing, shortcuts and profile links | C084, C085, C086, C087, C088 | B01, B11 | Blocked slices: Q06 |
| **B22** | Global and per-chat notification settings | C089, C090, C091, C092, C093 | B05 | Blocked slices: Q06 |
| **B23** | Notification routing and inline actions | C094 | B21, B22 | Blocked slices: Q06 |
| **B24** | App lock and sensitive privacy controls | C095, C096, C097, C098 | B01 | Blocked slices: Q02, Q06, Q08 |
| **B25** | Key packages and developer diagnostics | C080, C110, C111, C118, C119 | B01 | Blocked slices: Q04 |
| **B26** | Appearance, typography and input preferences | C100, C101, C102, C103, C104 | B01 | Blocked slices: Q01, Q09 |
| **B27** | Download matrix, queue and media quality | C105, C106, C107 | B11 | Ready |
| **B28** | Relay publication and managed-list differences | C108, C109 | B05 | Blocked slices: Q04 |
| **B29** | AI-agent setup and streamed operation rows | C112, C113 | B03, B09 | Ready |
| **B30** | Verified Nostr event cards and readers | C114, C115 | B09, B13 | Ready |
| **B31** | Help, About, licenses and external support | C117 | B01 | Ready |
| **B32** | Distribution-gated update experience | C120, C121 | B05 | Blocked slices: Q06 |

## Batch details

### B01 — Access failures, retained profiles and signer choice

Capabilities: C001, C003, C004, C005, C011. Dependencies: None.

**Implementation:** Add rejected public/encrypted key, normalized scan/paste, unavailable network and action-specific setup failures. Prototype currently accepts every nsec prefix; keep deterministic keys, not real login. Add installed/unavailable, identity approval, proof approval, cancellation, rejection and signer-owned identity states. No real signer calls in this prototype batch. Distinguish safe resume, uncertain prior key-package publication, explicit recovery consent, declined, partial and unexpected-state outcomes. Add Continue as and retained-profile picker after non-wiping sign-out, restoring the same profile-owned history and drafts. Add deterministic startup loading, failure, retry and retained-account recovery states without initializing a runtime.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/WhiteNoiseApp.kt`, `app/src/main/java/dev/ipf/whitenoise/model/OnboardingValidation.kt`, `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/onboarding/SignInScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/onboarding/WelcomeScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C001 satisfies its matrix contract. C003 satisfies its matrix contract. C004 satisfies its matrix contract. C005 satisfies its matrix contract. C011 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B02 — Keys, sign-out and staged wipe outcomes

Capabilities: C007, C008, C009, C010. Dependencies: B01.

**Implementation:** Expose local versus external signing and disable unavailable secret export with a reason. Preserve existing reveal/copy/share safeguards. Keep local fake export but add explicit result expiry/background concealment and signer-unavailable state. No cryptography or real keys. Add delete-key-packages choice and distinguish local sign-out success with incomplete relay cleanup from sign-out failure. Preserve approved wipe default/presentation; Q05 resolved in B02. Represent leave, key-package cleanup and local wipe stages with independent outcomes; do not turn partial loss into generic success. Preserve prototype whole-app erase and inactive-profile removal.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/Profile.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/DestructiveScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/ProfileSettingsScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C007 satisfies its matrix contract. C008 satisfies its matrix contract. C009 satisfies its matrix contract. C010 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B03 — People discovery and private contact data

Capabilities: C013, C014, C019, C021. Dependencies: B01.

**Implementation:** Add local/following/network-result distinctions, resolving, partial results, retry, invalid npub and address not found; unresolved non-member can receive an invite link. Remember the created ID and retry opening it without creating a duplicate; drop completion after navigation/profile changes. Add per-profile nickname/notes editing; nickname cleaned/capped at 80 characters; clear restores published name. Propagate names to chats, search, shares and roster; never change published metadata. Add Start group with person, eligible multi-group add and eligible admin-promotion selection, pending/partial roster lookup and retry; use authoritative role fixtures.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatFixtures.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/chats/ChatCreationScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C013 satisfies its matrix contract. C014 satisfies its matrix contract. C019 satisfies its matrix contract. C021 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B04 — Profile banner, avatar viewing and Lightning field

Capabilities: C016, C017, C018. Dependencies: B03.

**Implementation:** Add banner choice/URL/removal and loading/failure states; open avatar image from own and other profiles without entering edit. Add syntactic validation, checking, unresolved and no-connection outcomes and read-only display in person profile; use a deterministic non-payment fixture. Offer a name suggestion action that changes only the draft, with deterministic suggestions and explicit Save.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/Profile.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/ProfileSettingsScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C016 satisfies its matrix contract. C017 satisfies its matrix contract. C018 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B05 — Chat-list selection, order and connection recovery

Capabilities: C024, C025, C026, C030. Dependencies: B01.

**Implementation:** Add Move up/down and order state within pinned partition, preserving non-pinned order and accessible alternatives. Add select/select-all-visible, read/unread, archive/unarchive, folder assignment and delete where offered; reconcile disappearing rows and partial failures. Active/ended local deletion follows production's local-only action. An explicit optional leave-first path checks membership/sole-admin prerequisites and reports independent leave/local results; never remove data before a required leave succeeds. Distinguish no internet, connecting, catching up and failure/retry from missing relay-role configuration. Preserve loaded rows while recovery runs.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatListPresentation.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/chats/ChatsScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C024 satisfies its matrix contract. C025 satisfies its matrix contract. C026 satisfies its matrix contract. C030 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B06 — Chat folders end to end

Capabilities: C027, C028, C029. Dependencies: B05.

**Implementation:** Add Folders management and New/Edit Folder. Deleting a folder leaves chats intact; blank names cannot save. Manual IDs union rule matches. People OR keyword; unread/groups/archive/muted constrain only rules; empty rule does not include every chat. Add included-chat/people pickers and preview counts. Support default Unread/Archived/Groups as editable folders, reorder with Move actions, restore missing defaults, assign from row/bulk/info and filter chats. Keep prototype Left discoverable.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/chats/ChatsScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C027 satisfies its matrix contract. C028 satisfies its matrix contract. C029 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B07 — Cross-chat search and filters

Capabilities: C031, C032, C033, C034. Dependencies: B05.

**Implementation:** Add grouped chat/message/profile results with snippets and exact-message navigation; current Chats filters rows and conversation search is local. Multi-select chat and sender filters; OR within each category, AND across categories, removable chips and Clear all; reset/reconcile on profile switch. Date presets/custom bounds plus text, links, images/video, voice/audio, files and any attachment; test boundary inclusion using fixed clock. Resolve supported public IDs/addresses to profile result and offer deterministic voice-query success/cancel/unavailable. Do not request microphone in audit or fixture implementation.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/MessageInteractionModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/OnboardingValidation.kt`, `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/chats/ChatsScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C031 satisfies its matrix contract. C032 satisfies its matrix contract. C033 satisfies its matrix contract. C034 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B08 — Conversation history, unread and details recovery

Capabilities: C035, C037, C047. Dependencies: B05.

**Implementation:** Keep current highlighting/navigation; add loading older-result, failed-history, unavailable target and retry without blanking existing timeline. Add deterministic older/newer loading, failed pages, captured unread boundary and next unread mention. Arrival/scroll must not accidentally mark unseen content read. Add received/created timestamps, expiry and Streaming state where applicable; preserve existing ID/copy/details surface. Do not manufacture read receipts.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ConversationProjection.kt`, `app/src/main/java/dev/ipf/whitenoise/model/MessageInteractionModels.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/MessageInteractionsUi.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C035 satisfies its matrix contract. C037 satisfies its matrix contract. C047 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B09 — Message editing, full reader and text selection

Capabilities: C038, C039, C040, C041. Dependencies: B08.

**Implementation:** Add edit draft, nonblank save, cancel, pending/failed retry/discard, edited marker and timestamped original/revisions; project latest accepted text into replies/search/copy/speech. Prototype expands composer but lacks full message reader and per-chat Collapse long messages. Preserve draft/selection/reply during expand/collapse; reader shares actions and markdown. Add selection with Copy and speech from selected passage; preserve source offsets and accessibility. Whole-message Copy alone is insufficient. Prototype inline markup covers a subset. Add structured headings, lists, quotes, code and links as supported by production parse/render model; keep authored text for copy and speech.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/InlineMessageMarkup.kt`, `app/src/main/java/dev/ipf/whitenoise/model/MessageInteractionModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/InlineMessageText.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/MessageInteractionsUi.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C038 satisfies its matrix contract. C039 satisfies its matrix contract. C040 satisfies its matrix contract. C041 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B10 — Moderation deletion and resilient forwarding

Capabilities: C043, C044, C045. Dependencies: B09.

**Implementation:** Allow current admin to remove another author’s group message; retain author-only remote delete for DM/nonadmin and local removal of tombstones. Re-check membership and role on action. Add independent deletion outcomes, successful-removal counts and retry for failed items; do not replay successful destructive work. Add folder selection, per-target prepare/upload/send progress, cancellation, partial completion, blocked reasons and expiry/session-change recovery. Existing 32-message/5-chat prototype limits need explicit comparison, not silent removal.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/MessageInteractionModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C043 satisfies its matrix contract. C044 satisfies its matrix contract. C045 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B11 — Composer media acquisition and attachment actions

Capabilities: C046, C049, C051, C052, C054, C057. Dependencies: B08.

**Implementation:** Viewer already saves/shares images/video. Extend message/file actions and multi-attachment results with truthful partial outcomes, unavailable bytes and no-handler states. Add local preview strip with no/partial/full/unavailable grant states. Real library permissions require Q06; default fixture uses bundled media, while Gallery remains standard Photo Picker. Add Low/Standard/High/Original choices and truthful output-size/metadata treatment; Original still strips identifying EXIF. Do not claim stripping where local encoder does not do it. Extend loading/availability fixtures with queued/active/progress/cancel/retry, cache miss and expired/invalid transfer outcomes. Keep one authoritative attachment identity through reconciliation. Prototype has GIF kinds/fixtures; add deterministic animated decoded content and fallback/error behavior instead of relying on a static GIF-labelled tile. Keep person sharing; add distinct device-contact picker, field preview and outgoing vCard/text contract. Do not equate a phone contact with a White Noise identity.

**Likely prototype files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ComposerModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/MessageInteractionModels.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/MediaViewer.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C046 satisfies its matrix contract. C049 satisfies its matrix contract. C051 satisfies its matrix contract. C052 satisfies its matrix contract. C054 satisfies its matrix contract. C057 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B12 — Draft photo editor

Capabilities: C050. Dependencies: B11.

**Implementation:** New editor from a selected draft photo: presets, 90-degree rotate, pen colors/widths, erase strokes, undo/redo/reset, discard, save/progress/failure and limits; preserve original and attachment identity.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ComposerModels.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C050 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B13 — Text attachment reader and expanded shared media

Capabilities: C055, C056, C059. Dependencies: B11.

**Implementation:** Add full filename, size, loading/empty/error, bounded 512 KiB preview, unsupported encoding/truncation fallback, copy/read-aloud and Open in another app. Existing Media/Files/Links grouping lacks separate images/video/voice views and voice library playback, grouping and fallback metadata. Extend existing Shared Content destinations. Represent valid/invalid package, unsupported installer, permission-required and Play-build fallback. No real installation or new installer permission in prototype.

**Likely prototype files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/ipf/whitenoise/model/ChatInfoModels.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C055 satisfies its matrix contract. C056 satisfies its matrix contract. C059 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B14 — Location sharing state flow

Capabilities: C058. Dependencies: B11.

**Implementation:** Add fixed-coordinate selection/review, current-location availability/denial, message card and Maps handoff proposal. Real location/map networking or osmdroid dependency needs Q06.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C058 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B15 — Read Aloud transport and source navigation

Capabilities: C061, C062. Dependencies: B08.

**Implementation:** Add pause/resume/stop, previous/next sentence/message, progress, lazy history edges and return to owning message across routes/profile changes. Map authored/markdown source offsets to current spoken passage; add accessible seek/read-from-here and resume-follow without mandatory gestures.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/InlineMessageText.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C061 satisfies its matrix contract. C062 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B16 — Read Aloud preferences and auto-read

Capabilities: C063, C064, C065, C066. Dependencies: B15.

**Implementation:** New Read Aloud settings: discovering/none/usable, per-engine voice, offline-only eligibility, saved voice fallback, unknown engine trust consent and settings recovery. System/preset/custom rate plus explicit speech-over-media setting and quiet/medium/loud; deterministic audio-focus/other-media states without background services. Global default plus per-chat inherit/on/off and arrival cursor. Suppress duplicates, history replay and reading after lock/sign-out. Specify local lifecycle state and notification-control fixture; real foreground playback/service is an explicit Q06 expansion.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/WhiteNoiseApp.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/PreferenceScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C063 satisfies its matrix contract. C064 satisfies its matrix contract. C065 satisfies its matrix contract. C066 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B17 — Dictation and production voice-note interaction

Capabilities: C067, C068, C069, C070. Dependencies: B11.

**Implementation:** Existing deterministic record/review is retained. Add production hold/release/lock/cancel ownership, too-short/mic-in-use/permission/failure states where absent, with an accessible tap alternative. Separate dictation from a voice-note transcript. Add service-check/listening/processing/review/error/cancel states and exact destination draft ownership. New Dictation settings: manual or silence completion and paste versus explicit opt-in send. If draft, membership or session changes, preserve transcript for review; never send to a new context. Model selected service missing/busy/network/timed out and denial/permanent denial, then Copy/Insert at end/Discard review. Real recognition/microphone service requires Q06.

**Likely prototype files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/ipf/whitenoise/model/ComposerModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C067 satisfies its matrix contract. C068 satisfies its matrix contract. C069 satisfies its matrix contract. C070 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B18 — Group setup image and authoritative roster states

Capabilities: C072, C073, C074. Dependencies: B03, B11.

**Implementation:** Add explicit no-other-member setup and initial timer; distinguish created group from failed timer application and failed opening. One/two emoji image, unsupported/limit states, private group photo versus public invite avatar, image loading/upload retry. Preserve public preview continuity. Add unknown/loading/failed roster, pending invites, per-person mutation lock, retry and stale completion handling before enabling role-sensitive commands.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/chats/ChatCreationScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C072 satisfies its matrix contract. C073 satisfies its matrix contract. C074 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B19 — Administration transfer, disband and transcript export

Capabilities: C076, C077, C078, C079. Dependencies: B18.

**Implementation:** Add explicit transfer-and-step-down and transfer-then-leave completion, partial failure and sole-member deletion case. A blocked leave dialog alone does not cover this flow. Add capability-enable, engine-declared blockers, confirmation, pending convergence, failure acknowledgment/retry and permanently ended state. Distinguish local delete and ordinary leave. Add verified-role unknown, unrecoverable and disbanding/disbanded states; block send/reaction/edit until permitted; preserve readable history and recovery explanation. Add export progress, unavailable source, cancellation/error and completed local document handoff; preserve ordering, authored identity and edits per export contract.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C076 satisfies its matrix contract. C077 satisfies its matrix contract. C078 satisfies its matrix contract. C079 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B20 — Disappearing-message timers and expiry

Capabilities: C081, C082, C083. Dependencies: B09, B18.

**Implementation:** Add production presets and bounded units from seconds through years with validation and read-only member state. Production explicitly prunes older plaintext on timer change; prototype sets a value/event. Confirm retroactive consequences and reconcile production after-seen help with actual update semantics before final copy. Use fixed clock/read anchors for expiry, remaining-time semantics and removal; reconcile selection/search/reply/media/TTS when a message expires.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C081 satisfies its matrix contract. C082 satisfies its matrix contract. C083 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B21 — Inbound sharing, shortcuts and profile links

Capabilities: C084, C085, C086, C087, C088. Dependencies: B01, B11.

**Implementation:** Add deterministic request/staging model and supported text/image/video/audio/document payload outcomes; real exported share target is Q06. Pick one profile and eligible chats, stage drafts rather than send, open first chat and report others; cancel restores prior route, no duplicate consumption. Model target metadata, unavailable/deleted/signed-out owner, fallback picker and replacement request. Actual shortcut publication is Q06. Prototype QR works internally. Add canonical marmot profile links plus accepted legacy/address forms and invalid/secret rejection; do not add arbitrary URL handlers. Queue latest owned request through lock or activation, resolve target under original profile, reject stale completion after explicit navigation; deterministic event harness, no public mock launcher.

**Likely prototype files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/ipf/whitenoise/MainActivity.kt`, `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/navigation/WhiteNoiseNavHost.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/onboarding/PrivateKeyQrScannerScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/ShareConnectScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C084 satisfies its matrix contract. C085 satisfies its matrix contract. C086 satisfies its matrix contract. C087 satisfies its matrix contract. C088 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B22 — Global and per-chat notification settings

Capabilities: C089, C090, C091, C092, C093. Dependencies: B05.

**Implementation:** Existing permission/push UI needs Play-services/build-configuration availability and asynchronous enable failure/revert states. Add preference, permission dependency, rejected service/retry and stopped state; local model only unless Q06 approves actual service. Add category rows for direct/group/mentions/reactions/invites/membership/agent activity and distribution-gated updates. Android owns category settings; do not imitate system screens. New Sounds & notifications detail: all/mentions/nothing, inherit/custom category, effective Android override, selected vibration and preview state. Add custom date/time, expiry and restore previous all/mentions choice. Keep approved immediate-choice Material dialog for existing presets.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/components/MuteDurationDialog.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/NotificationPermission.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/PreferenceScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C089 satisfies its matrix contract. C090 satisfies its matrix contract. C091 satisfies its matrix contract. C092 satisfies its matrix contract. C093 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B23 — Notification routing and inline actions

Capabilities: C094. Dependencies: B21, B22.

**Implementation:** Model message/invite route and account ownership, inline action pending/failure/retry, exactly-once result and read-through boundary; live notifications require Q06.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/MainActivity.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C094 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B24 — App lock and sensitive privacy controls

Capabilities: C095, C096, C097, C098. Dependencies: B01.

**Implementation:** Prototype configures device-authentication preference but lacks lock surface/session gating. Add deterministic locked/authenticating/cancel/failure/unlocked states, background timing and no-data flash. Add learning-request preference and coverage for relevant app-owned inputs; wording must say request, not guarantee about keyboard behavior. Production combines screenshot blocking and Recents with secure windows; prototype explicitly promises Recents-only privacy. Preserve current setting and propose a separate screenshot option pending decision. Existing diagnostic exports are sanitized. Production forensic files may contain message/identity/device data. Keep consent, sensitive export and sanitized diagnostics separate; do not reuse reassurance copy.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/WhiteNoiseApp.kt`, `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/DeveloperScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/DiagnosticsImprovementsScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/PreferenceScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C095 satisfies its matrix contract. C096 satisfies its matrix contract. C097 satisfies its matrix contract. C098 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B25 — Key packages and developer diagnostics

Capabilities: C080, C110, C111, C118, C119. Dependencies: B01.

**Implementation:** Extend developer inspection for MLS/Nostr identifiers, required components and detailed push state; keep raw protocol fields developer-only unless Q04 changes exposure. Production exposes Key Packages at Settings root and splits publishing/published/retained material. Prototype is developer-gated and simpler; exposure is Q04, state expansion is ready. Add separate actions, result/error and delete confirmation; show local/relay provenance, seen-on relays, publish time and retained-not-published explanation. Production version-tap unlock and inline stream debugging differ from prototype enabled toggle/debug snapshots. Preserve approved developer entry while adding explicit streaming-event controls. Add loading/error refresh, connection attempts/successes, self-send outcome and 30-minute performance-logging state; retain sanitized copy boundary and no telemetry/network execution.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/DeveloperModels.kt`, `app/src/main/java/dev/ipf/whitenoise/state/AppViewModel.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ChatInfoScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/DeveloperScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C080 satisfies its matrix contract. C110 satisfies its matrix contract. C111 satisfies its matrix contract. C118 satisfies its matrix contract. C119 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B26 — Appearance, typography and input preferences

Capabilities: C100, C101, C102, C103, C104. Dependencies: B01.

**Implementation:** Add optional pure-black theme using semantic roles, with contrast/state distinction and no default palette replacement. Add System/Manrope/Outfit/Urbanist/Figtree choices and 0.85/1/1.15/1.3 scale composed with system scale; font resources/provenance and approved default require Q09. Production has full-spectrum/hex customization by theme/profile and bubble side. This conflicts with monochrome identity; keep full capability recorded, block colored implementation pending Q01. Add input preference and IME/hardware Enter behavior while preserving multiline/dictation and accessible Send. Prototype offers a different language set and in-memory selection; add Russian/Turkish/Simplified/Traditional Chinese and determine translation scope Q09. Preserve existing Serbian and other prototype choices.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/ConversationComposer.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/PreferenceScreens.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/theme/WhiteNoiseTheme.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C100 satisfies its matrix contract. C101 satisfies its matrix contract. C102 satisfies its matrix contract. C103 satisfies its matrix contract. C104 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B27 — Download matrix, queue and media quality

Capabilities: C105, C106, C107. Dependencies: B11.

**Implementation:** Replace limited policy model with four media types across Wi-Fi/mobile/roaming/metered; most restrictive active condition wins, unknown network does not auto-download. Preserve approved grouping. Clear queued automatic work only; active and explicitly tapped transfers continue. Model per-profile queue/pause and recovery with no networking. Add missing quality modes, resulting photo/voice policy and metadata explanation. Do not promise video re-encoding: production sends selected videos/audio as-is.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/PreferenceScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C105 satisfies its matrix contract. C106 satisfies its matrix contract. C107 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B28 — Relay publication and managed-list differences

Capabilities: C108, C109. Dependencies: B05.

**Implementation:** Production only supports managed secure relays and two editable lists; mutation can clean unsupported imports. Prototype permits general URLs and three role assignments. Keep existing scope pending Q04; add validation/recovery states without silently dropping roles. Add unavailable/missing/published list projection and refresh/pending/error independently of connected/disconnected socket state.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ProfileSettings.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/RelayDonateScreens.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C108 satisfies its matrix contract. C109 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B29 — AI-agent setup and streamed operation rows

Capabilities: C112, C113. Dependencies: B03, B09.

**Implementation:** New AI Agents screen with Hermes/OpenClaw/OpenCode/Codex prompt preview/copy and docs/manual contact setup. Public key only; no installation or account connection is performed. Add streaming text, operation summary/details/status/progress and failure/cancelled outcomes; distinguish ordinary operation presentation from developer stream events.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/SettingsComponents.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C112 satisfies its matrix contract. C113 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B30 — Verified Nostr event cards and readers

Capabilities: C114, C115. Dependencies: B09, B13.

**Implementation:** Typed fixtures for supported note/article/image/video/document/event kinds, loading/not-found/invalid/unavailable/retry; preserve authored reference and Copy. No relay/network resolution. Reuse rich reader and native local-video viewer for event content, loaded metadata and retry states; remote video remains a local fixture.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/model/ChatModels.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/InlineMessageText.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/MediaViewer.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/conversation/TimelineMessageContent.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C114 satisfies its matrix contract. C115 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B31 — Help, About, licenses and external support

Capabilities: C117. Dependencies: B01.

**Implementation:** Add Help and About destinations, version/build rows, safe external links and license listing. Bug report previews must exclude messages, keys and logs unless separately selected.

**Likely prototype files:** `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/SupportScreen.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C117 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.

### B32 — Distribution-gated update experience

Capabilities: C120, C121. Dependencies: B05.

**Implementation:** Add version/checking/failure/current/available states in Chats and Settings and source-defined dismissibility. Store-managed fixture must have no in-app update entry. Model resolve/confirm/download/verify/ready/install-permission/error/retry/cancel without fetching APKs or invoking installer. Keep verification distinct from download completion.

**Likely prototype files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/ipf/whitenoise/navigation/AppRoute.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/chats/ChatsScreen.kt`, `app/src/main/java/dev/ipf/whitenoise/ui/settings/SettingsComponents.kt`.

**Fixture/state work:** Add profile/chat-owned immutable states for each named eligibility, progress, cancellation, success, partial, unavailable and failure result. Use a fixed clock/IDs where time or asynchronous ownership matters. Expose ordinary user copy only through resources; keep fixture controls developer-only.

**Acceptance and validation:** C120 satisfies its matrix contract. C121 satisfies its matrix contract. Preserve all existing flow behavior. Add rule/ownership unit tests and durable Compose interaction/semantics tests; run targeted host tests and the complete static gate at the end. Device/system-surface and visual acceptance remain separate.

**Non-goals:** No backend, network transport, Marmot, real signing/encryption, persistence, installer, notification delivery, background service or device automation unless a later explicit request expands the selected batch. Do not restyle system-owned surfaces or redesign unrelated screens.
