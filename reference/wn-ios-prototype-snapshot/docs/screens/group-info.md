# Group Info and management

## Purpose and hierarchy

**Group Info** presents the photo, name, description, member count, quick
notification and Search actions, shared-content categories, group management,
and Leave Group. Member rows push **User Profile**.

- Opening another member uses the complete authoritative **User Profile**
  presentation: avatar, name, bio when present, compact Verified Nostr Address,
  copyable public key, shared groups, contact state, and block state. It does
  not maintain a smaller duplicate Group Member profile design. The inline
  navigation title states **User Profile (Admin)** or **User Profile (Member)**
  for the selected person's current group role.
- Only in this group-origin profile context, the standard relationship controls
  use the **Profile Actions** section title. Profiles opened elsewhere keep the
  existing untitled presentation.
- When the active profile is an admin and the selected person is another active
  member, a separate final group-actions section follows the complete profile.
  Its title is **Group Actions**. It contains symbol-labeled **Make Admin** or
  **Remove Admin** and destructive **Remove from Group**. Remove from Group uses
  a red outlined minus-circle symbol distinct from Block. Ordinary members and
  inactive group memberships never see these controls.
- Make Admin, Remove Admin, and Remove from Group each use a standard centered
  native alert with **Cancel**, one explicit confirmation action, and concise
  consequence text. They do not use an anchored confirmation dialog or custom
  popover. Removing a member dismisses User Profile after the mutation.

- **Mute** or **Unmute**, **Disappearing Messages**, and **Search** appear as
  one centered quick-action group directly below the group identity. They use
  compact non-glass circular secondary controls on an adaptive white
  surface with concise captions beneath them: **Mute** or **Unmute**,
  **Disappearing**, and **Search**. They retain explicit accessibility names,
  and the disappearing-message menu announces its current value.
- One scrolling `List` replaces the rejected category tabs and pager.
  **Photos & Videos**, **Links**, and **Documents** appear as disclosure rows in
  one grouped container titled **Shared in Chat** and push focused
  shared-content destinations. Its heading is spaced from the quick actions by
  the same amount used between the group identity and quick actions.
- The Photos & Videos destination follows the shared Chat Info behavior: a
  three-column grid of square cells opens one selected item without paging the
  entire group history. The preview shows the sender and sent date without
  repeating message text, exposes system-owned Share, Forward, Save, and Go to
  Message toolbar actions, and uses the shared searchable multi-chat forwarding
  sheet.
- **Relays** and **Developer Tools** appear in the next grouped container titled
  **Advanced**.
  Members and permitted management actions follow. The final group contains
  **Archive** or **Unarchive** and destructive **Leave Group**, with no
  redundant heading. Leaving uses a native alert.
- Media, links, documents, relays, members, and settings derive from and mutate
  the same authoritative group chat as the conversation and chat row.

## Permissions and mutations

- Admins can edit group photo/name/description, add people, make/remove admins,
  and remove other members. Ordinary members receive the same readable info
  without admin commands.
- The active profile never removes itself through a member row. **Leave Group**
  owns self-removal.
- A group always retains an admin. The sole admin cannot leave and sees:
  **You’re the only admin in this group. Make another member an admin before you
  leave.**
- Edits and member/role changes append typed timeline events with human names
  and **You** for the active actor.
- Edit Group uses explicit **Cancel** and **Save** actions. Cancel discards the
  working copy. **Remove Photo** appears only for a replaceable custom photo;
  a default monogram or symbol offers **Add Photo** without a false removal
  action.
- Leaving uses native destructive confirmation, preserves readable history,
  moves the row to Left, and disables sending. Re-invitation is not modeled.

## Chat Relays

Chat Info and Group Info both expose **Relays** as a disclosure destination. It
says **These relays are used only to deliver messages in this chat.**, lists
the chat's independent `wss://` URLs, adds normalized nonduplicate values, and
removes values after consequence-aware confirmation. Add Relay expands to the
large detent when its URL field receives keyboard focus. Removing the final
relay is allowed after stating that sending will stop; history remains
available and adding a relay restores sending.

## Native components and accessibility

Use `Form`, `Section`, `NavigationLink`, `TextField`, `PhotosPicker`,
`LazyVGrid`, `ShareLink`, `fileExporter`, `Button`, native disabled states, and
alerts. Admin/Member is visible and spoken. Destructive actions are never
gesture-only.

## Governing sources

- [Forms](https://developer.apple.com/documentation/swiftui/form)
- [NavigationStack](https://developer.apple.com/documentation/swiftui/navigationstack)
- [List](https://developer.apple.com/documentation/swiftui/list)
- [NavigationLink](https://developer.apple.com/documentation/swiftui/navigationlink)
- [Menus](https://developer.apple.com/design/human-interface-guidelines/menus)
- [PhotosPicker](https://developer.apple.com/documentation/photosui/photospicker)
- [LazyVGrid](https://developer.apple.com/documentation/swiftui/lazyvgrid)
- [ShareLink](https://developer.apple.com/documentation/swiftui/sharelink)
- [fileExporter](https://developer.apple.com/documentation/swiftui/view/fileexporter(ispresented:document:contenttype:defaultfilename:oncompletion:))
- [Alerts](https://developer.apple.com/design/human-interface-guidelines/alerts)
- [alert(_:isPresented:actions:message:)](https://developer.apple.com/documentation/swiftui/view/alert(_:ispresented:actions:message:))

The current White Noise app supplied bounded behavioral comparison evidence for
search, group editing, adding/removing people, role changes, archiving, leaving,
and last-admin protection. This brief is the durable local authority.

## Acceptance criteria

- Weekend Walks exposes the complete admin path. Product Circle demonstrates
  the non-admin path. Existing left/removed groups remain readable only.
- Every mutation updates members, permissions, metadata, list projection, and
  the timeline atomically.
- Chat routing remains independent from profile routing after creation.
- User Profile presents Verified Nostr Address for the active profile and other
  members, with verified and unverified fixture states distinguishable.
- Another member's destination matches User Profile before any group-specific
  controls. Admins alone see the final role/removal section, and every command
  confirms in a centered alert rather than an anchored dialog.
- The title identifies the selected person's Admin or Member role. Profile and
  group action sections are labeled, and each group command has a distinct
  semantic symbol without depending on the symbol alone.
