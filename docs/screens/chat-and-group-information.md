# Chat and group information

Status: Implemented; static verification complete on 2026-08-15

## Source evidence

- pinned `chat-info.md`, `group-info.md`, `person-profile.md`,
  `conversation-search.md`, and `disappearing-message-indicators.md`
- `ChatInfoView.swift` and authoritative iOS chat-model tests

## Android-native adaptation

- The conversation title/avatar opens one typed Chat Info route whose lazy
  Material list adapts to direct or group identity and current membership.
- Equal Material quick actions own About/member count context, mute duration,
  disappearing duration, and in-place Search return. Shared content uses three
  ordinary disclosure rows rather than tabs.
- Focused Photos & Videos uses an adaptive three-column grid and the same
  Compose media pager as message bubbles. Links and Documents use lazy Material
  lists and local/system handoffs.
- Group admins receive explicit Edit Group, Add People, and role-aware member
  actions. Photo Picker, text fields, centered confirmation dialogs, and typed
  timeline events own every mutation. Ordinary members remain read-only.
- Edit Group uses Photo Picker with its default platform or OEM appearance.
- Chat Relays is an independent typed route. It normalizes/deduplicates `wss://`
  endpoints, confirms removal, permits an empty set, and restores the chat's
  captured defaults without changing profile or sibling chat relays.
- Archive, mute, disappearing, leave, role, member, metadata, relationship,
  and relay changes mutate only the active profile's authoritative chat graph.

## Acceptance gates

- Unit tests cover shared-content projection, relay normalization/isolation,
  group edits/members/roles/events, last-admin rules, groups in common, timer
  changes, and recovery after final-relay removal.
- Compose route tests for direct/group info, shared categories, admin/member
  states, group editing, add people, and chat relays compile.
- Clean build, lint, tests, APKs, and permission/export audits pass.
