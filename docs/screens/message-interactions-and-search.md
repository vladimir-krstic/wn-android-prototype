# Message interactions and conversation search

Status: Visual-polish static gate passed on 2026-08-21; device acceptance pending

## Source evidence

- `wn-ios-prototype@0bd7cba:docs/screens/message-actions.md`
- `wn-ios-prototype@0bd7cba:docs/screens/conversation-search.md`
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

## Visual-polish translation

- The long-press sheet now keeps a compact tonal preview of the focused
  message, a horizontally scrollable quick-reaction strip, a clear **More
  Reactions** tonal action, and icon-leading Material command rows. Retry stays
  first and Delete retains the semantic error role.
- The emoji sheet uses a rounded Material search field with named Clear and
  Configure actions, full category labels in a scrollable chip row, and an
  adaptive grid whose emoji targets remain at least 48dp instead of forcing an
  iPhone-specific eight-column geometry at every Android width.
- Reaction configuration uses six named replacement targets with Reset and
  Done hierarchy. Forwarding uses the same search-field treatment, native
  checkbox semantics, a visible five-chat limit, tonal selected rows, a useful
  no-results state, and one pinned 56dp completion action.
- Selection replaces text-heavy bars with a Close app-bar action, a live
  selected count, named Delete/Forward icon actions, and visible explanation
  when the accepted deleted-message or 32-message rule disables forwarding.
- Conversation search stays in the transcript, requests focus on entry, owns
  an in-field Clear action, preserves the draft, and uses compact previous/next
  icon controls around a live result count. The current result has explicit
  containment, noncurrent content is subdued, text occurrences use a
  high-contrast semantic highlight, and result-position semantics are
  localized.
- Message Details now follows the child-destination app-bar hierarchy and
  groups the shared message plus delivery/sender/recipient information on
  restrained tonal surfaces without exposing protocol data.

## Current official Android guidance

- Android Developers recommends `combinedClickable` for long-click context
  menus and haptic feedback after recognition.
- Compose semantics and custom actions provide named assistive-technology
  alternatives to gesture-only operations.
- Material search fields, modal bottom sheets, lists, checkboxes, dialogs, and
  navigation remain the default platform composition for the bounded flows.
- Sources rechecked for this pass: [tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press),
  [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics),
  [Material search](https://developer.android.com/develop/ui/compose/components/search-bar),
  and [Material bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets).

## Acceptance gates

2026-08-26 shared-sheet refinement: message actions, emoji selection, reaction
configuration and forwarding use `WhiteNoiseModalBottomSheet`; titled sheets
use `WhiteNoiseSheetHeader`. No repeated navigation-bar padding remains inside
these modals. The reaction configuration scrolls at large font sizes; message
actions reserve constrained space for their scrollable action list. Ordinary
rows stay transparent; preview, selection and completion groups retain their
intentional tones. Message actions still use a sheet; only Chats row commands
move to an anchored menu. Selection bars and media screens are unchanged.

- Unit tests cover action availability, reaction replacement/removal, quick
  configuration, search order/content, reply, forwarding bounds/order,
  deletion scopes, and per-profile isolation.
- Compose tests for long-click alternatives, selection, forwarding, details,
  emoji configuration, mention suggestions, and in-place search compile.
- Focused tests cover the message context/title, reaction configuration slots,
  selection controls, forwarding limit/search, focused conversation search,
  named Clear, and previous/next result actions.
- The complete clean static gate and permission/export audit pass.
