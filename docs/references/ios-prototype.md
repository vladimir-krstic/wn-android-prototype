# iOS prototype snapshot provenance

## Capture

- Source checkout: sibling `wn-ios-prototype`
- Source branch: `master`
- Source commit: `58785a4724f33e23135c4dd3f98f231fca6a809d`
- Captured: 2026-08-15 in Europe/Belgrade
- Local destination: `reference/wn-ios-prototype-snapshot/`
- Snapshot size after excluding generated state: approximately 6.4 MB across
  210 files

The snapshot contains the iOS app source, Xcode project metadata, unit tests,
UI tests, docs, and product resources. Generated build output, `.git`, external
agent caches, Claude configuration, and per-user Xcode workspace state were
not copied. The source
`AGENTS.md` and `README.md` were named `AGENTS.ios.md` and `README.ios.md` so
the iOS contract cannot become a governing Android instruction by directory
scope.

## Working-tree state

The source checkout was not clean. The snapshot intentionally includes the
current working-tree contents of these modified files:

- `WhiteNoisePrototype/Screens/Chats/NewChatView.swift`
- `WhiteNoisePrototype/Screens/Conversation/ChatInfoView.swift`
- `WhiteNoisePrototype/Screens/Conversation/ConversationView.swift`
- `WhiteNoisePrototype/Screens/Shared/ProfileComponents.swift`
- `docs/screens/conversation-composer-states.md`
- `docs/screens/group-info.md`
- `docs/screens/person-profile.md`
- `docs/screens/verified-nostr-address.md`

This means the snapshot is identified by commit plus the explicit dirty-file
set above, not by commit alone.

## How to use the snapshot

1. Start with `docs/port/source-map.md`.
2. Read the copied screen brief for product intent and exact copy.
3. Read model and UI tests for invariants and flows.
4. Read Swift source only to resolve remaining behavior, state, or hierarchy.
5. Translate through `docs/references/native-ui.md` and current official
   Android guidance.
6. Copy a reusable asset into Android resources only when the selected flow
   needs it and after applying `resources.md`.

The snapshot must never be a build-time or runtime dependency.

## Refresh policy

Refresh only as a separate user-approved task. Before replacing anything:

1. record the new branch, commit, and dirty-file list;
2. diff product docs, source behavior, tests, and resources against this
   snapshot;
3. update `docs/port/feature-inventory.md`, `source-map.md`, and affected
   Android briefs;
4. preserve or deliberately supersede Android decisions already accepted by
   the user;
5. replace the snapshot atomically and verify the new inventory.
