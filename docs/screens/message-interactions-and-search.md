# Message interactions and conversation search

Status: Implemented; static verification complete on 2026-08-15

## Source evidence

- `reference/wn-ios-prototype-snapshot/docs/screens/message-actions.md`
- `reference/wn-ios-prototype-snapshot/docs/screens/conversation-search.md`
- the message-action, forwarding, deletion, reaction, mention, and search cases
  in the pinned chat catalog and iOS tests

## Android-native adaptation

- Compose `combinedClickable` owns deliberate hold detection, ripple, keyboard,
  hover, and accessibility long-click behavior. Haptic feedback accompanies a
  successful hold. Every command is also exposed as a named custom
  accessibility action, so discovery never depends on a gesture.
- A Material modal bottom sheet presents the focused message's quick reactions
  and the ordered Reply, Forward, Copy, Select, Info, Delete commands. Retry is
  first for a failed outgoing message. Deleted messages expose no actions.
- A searchable Material emoji bottom sheet provides deterministic recent and
  standard categories. A profile-owned six-item quick-reaction configuration
  supports replacement, swap, Reset, cancel, and Done without persistence or
  network work.
- Selection uses a visible 48dp checkbox column and replaces the composer with
  a Material bottom action bar. Forwarding uses a searchable multi-select sheet
  capped at five destination chats and appends source-ordered in-memory copies.
- Message Details is a typed Navigation Compose destination backed by the real
  chat/message graph. Delete for Me removes local entries; Delete for Everyone
  is available only for nondeleted outgoing selections and leaves a clean
  tombstone.
- Conversation search stays in place. A top Material search field updates
  immediately; bottom previous/next/count controls replace the composer and
  select results newest first across message text, sender, and attachment/link
  labels. Search never mutates the draft.
- Group composer mention suggestions derive only from active members and insert
  the selected visible name into the authoritative draft.

## Current official Android guidance

- Android Developers recommends `combinedClickable` for long-click context
  menus and haptic feedback after recognition.
- Compose semantics and custom actions provide named assistive-technology
  alternatives to gesture-only operations.
- Material search fields, modal bottom sheets, lists, checkboxes, dialogs, and
  navigation remain the default platform composition for the bounded flows.

## Acceptance gates

- Unit tests cover action availability, reaction replacement/removal, quick
  configuration, search order/content, reply, forwarding bounds/order,
  deletion scopes, and per-profile isolation.
- Compose tests for long-click alternatives, selection, forwarding, details,
  emoji configuration, mention suggestions, and in-place search compile.
- The complete clean static gate and permission/export audit pass.
