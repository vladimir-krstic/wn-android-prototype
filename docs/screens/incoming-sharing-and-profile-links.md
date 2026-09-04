# Incoming sharing, shortcut targets and profile links

## Purpose and scope

B21 covers C084–C088: owned incoming content, profile/chat selection, multi-chat
draft staging, Direct Share and conversation-shortcut recovery, profile-link and
QR parsing, and deferred/replaced navigation. Preserve the established monochrome
Material interface, existing draft content and native system Back.

Q06 remains explicit: implement app-owned deterministic requests and callbacks,
with Developer Tools entry scenarios. Do not export a share Activity, register
new URL handlers, publish shortcuts, read another app's content provider, add
permissions, deliver notifications or implement authentication/network/storage.
Existing outbound Sharesheet and authorized QR scanner remain native surfaces.
No device/emulator execution is authorized.

## Production evidence and drift

The immutable production baseline is `319454889f1c2494dec4a69b5577d98017f44eee`.
Relevant files under `app/src/main/java/dev/ipf/whitenoise/android`:
`share/SharePayload.kt`, `ShareRequest.kt`, `ShareInboundStager.kt`,
`ShareStaging.kt`, `ShareRouting.kt`, `ShareShortcutPublisher.kt`;
`ui/share/ShareChatPickerFullScreen.kt`; MainShell stage/open/replace routing;
AppState stageInboundShare/validatedInboundShareTarget; DraftStore.mergeText;
`notifications/ConversationShortcuts.kt`, `ConversationShortcutOwnership.kt` and
NotificationTarget's inbound routing; `core/ProfileLink.kt`, NostrProfileReference
and `ui/qr/QrScanResult.kt`. Focused parser/staging/routing tests are evidence,
not executed production tests.

Fresh master on 2026-09-04 remains `911040c7e1c31652638c8cfd72812d1f3a694b9b`;
there is no main branch. The seven-commit diff changes no named share/profile-link/
QR/shortcut source. The AppState diff leaves stageInboundShare unchanged; its
changes concern bootstrap, account-switch snapshots, timeline metadata and installer
handoffs. MainShell only adds the installer handoff effect/import. Preserve the
separate final timeline/installer/Nostr and startup/contact/Amber drift audit.

## Content and draft contract

Accept text and supported image/video/audio/document descriptors. Trim outer share
text, deduplicate identical stream identities, use provider MIME when available
and intent MIME only as fallback. Images/video use the media shelf; audio/other
files use documents. Never log payloads or use raw input as an external launch.
Unknown/malformed/empty content and denied/unavailable/too-large input produce
specific recoverable outcomes. Preparation is request-bound and cancellable.

Select one signed-in profile and eligible chats, including archived chats. Profile
selection inside the picker does not switch the active profile or carry another
profile's selected chat IDs. Preserve and merge existing draft text, attachments,
reply and draft ownership. Apply production's separate ten-media/ten-document
incoming shelf caps against already queued items; report dropped items explicitly.
These caps apply to incoming staging, not unrelated accepted composer capacity.
Nothing is sent. After acceptance open the first selected chat under its chosen
profile and report drafts staged in other chats. Failures or lost targets keep
recovery visible; accepted staging is never repeated on opening retry.

## Ownership and external routing

Each arrival has its own monotonic request ID, even an equal payload/target retap.
A newer request replaces old pending/preparing/navigation work. Bare launcher
re-entry preserves pending work. Cancel removes only the pending request and
returns to the prior route without modifying drafts. A completion checks request,
profile/session and navigation ownership before mutating or opening anything.

Direct Share resolves only a shortcut owned by the receiving active profile and
an eligible current chat; unavailable, unknown and mismatched metadata falls back
to the picker. Conversation shortcuts retain exact profile/chat ownership and
recover from missing or signed-out owners. Account switching must not delete other
profiles' conversations/shortcut records; real publication/cleanup is a migration
seam. Defer presentation through lock or sign-in/activation, retain the original
target owner, and cancel stale routing after explicit user navigation.

## Profile links and QR

Emit `marmot://profile/<npub>` for sharing and append `?from=qr` for generated QR.
Accept known marmot/whitenoise flavor schemes, opaque legacy forms, nostr/bare npub,
known whitenoise.chat/marmot.app profile URLs, supported nprofile references and
recipient hex/address inputs in their appropriate entry paths. Validate public
reference shape/checksum; reject secret/encrypted keys, malformed or mixed-case
encodings, unknown hosts and unsupported routes. Parsing never fetches a URL.
QR provenance is metadata, not authority. Existing fixed identities must have
valid public encodings so outgoing profile links round-trip through the scanner;
no real key generation or signing is introduced. Resolve the scanned identity,
not a hard-coded unrelated profile. Unknown valid profiles have an honest minimal
profile result; address lookup uses the existing deterministic people directory.

## Presentation, copy and Back

Reuse a full-screen Material task surface for Share to: profile selector, payload
summary, Search chats, recent/eligible destination rows, selection count and Share.
Explain once: “Review this content in each chat before sending.” Use “Preparing
shared content…”, “No chats to share to”, “Choose another profile”, “Retry” and
specific unavailable-content/target recovery. Never show Sent after staging.
Results distinguish created drafts from opening failure and show dropped-file
counts. Native Back first dismisses profile/search substate, then cancels the
pending task; returning to the underlying route does not reconsume it.

Use established adaptive content bounds, safe/IME insets, 48 dp touch targets,
resource/plural copy, native selection and semantic disabled/error/status roles.
Support large text/display scaling, RTL, keyboard, TalkBack and logical focus.
Do not restyle Android Sharesheet, scanner or other system-owned surfaces.

## Governing sources and accepted parity

- [Receiving shared content](https://developer.android.com/develop/ui/compose/sharing/receive): validate payloads and stage editable content before use.
- [Creating shortcuts](https://developer.android.com/develop/ui/compose/system/shortcuts/creating-shortcuts): owned targets and external publication seam.
- [NIP-19 public encodings](https://github.com/nostr-protocol/nips/blob/master/19.md): public display/checksum/TLV parsing and independent public test examples; no protocol client or cryptography.
- [Navigation deep links](https://developer.android.com/guide/navigation/design/deep-link): explicit target parsing and navigation ownership.
- Local [source map](../port/source-map.md), [UI metrics](../ui-metrics.md),
  [native evaluation](../references/native-ui.md) and existing Share & Connect/
  New Message/forwarding surfaces govern presentation. Incoming features are
  approved production Android scope, not an expansion of the pinned iOS baseline.

## Acceptance and validation

- Every supported content kind stages without sending and preserves prior drafts.
- Caps, deduplication, invalid metadata, denied content and retry are observable.
- Profile/target selection is explicit and current; no stale callback, duplicate
  consumption or navigation round trip writes to another profile.
- Direct and generic share routes, missing shortcuts, sign-in/lock deferral,
  replacement, cancellation and accepted-stage opening recovery are covered.
- Canonical/legacy/public reference forms round-trip; invalid/secret inputs never
  open a profile or arbitrary external app. QR opens its actual parsed identity.
- Host model/state tests, compiled UI tests and the complete build/lint/APK gate
  precede the single B21 commit. Device/visual acceptance remains separate.

## Implementation evidence

Implemented in `IncomingSharing.kt`, `IncomingExamples.kt`, `IncomingController.kt`,
`IncomingShareUi.kt`, AppViewModel and the navigation host. Developer Tools owns
incoming request/outcome/deferral scenarios. The full-screen Material picker merges
text, applies independent ten-item shelves, selects a profile and multiple eligible
chats, reports overflow and other drafts, and separates staging from opening retry.
Navigation/profile changes, sign-out/wipe, equal replacement and stale callbacks
cannot consume old work. Missing conversation shortcuts have explicit Go to chats
recovery. Developer deferral resets when its owning profile/tools disappear.

`ProfileLinks.kt` validates public Bech32/checksum/TLV data, supported routes and
recipient forms. Share & Connect shares canonical links and generates QR provenance;
scanning resolves the parsed identity. People discovery and global recipient search
share the same normalization, while ordinary web queries retain message search.
Bundled identities now use stable valid public encodings, with no corresponding
secret or cryptographic operation.

Host gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
693 unit tests pass with zero failures/errors/skips. B21 adds 38 cases: nine public
reference tests (including independent published NIP-19 vectors), six staging tests
and 23 ownership/state tests. Nine new Compose interaction cases compile/package
only: selection, profile isolation, search, Back, fallback and failure/recovery.
Lint has zero errors, 14 existing warnings and two hints. Debug and test APKs
assemble. No device execution or visual acceptance is claimed.

Migration reconnects Android Intent extraction/ClipData and URI grants, off-main
ContentResolver preparation, signing-account selection, ShareStaging/DraftStore,
MainShell's accepted-stage/open callbacks, real app-lock readiness and exact
conversation shortcut ownership/publication. Q06 still excludes exported receiving
and shortcut publication here; no manifest, permission, backend, storage or service
expansion is included. Public-only encoding is display validation, not identity
creation or authentication. Commit: `B21: Add incoming sharing, shortcuts and profile links`.
