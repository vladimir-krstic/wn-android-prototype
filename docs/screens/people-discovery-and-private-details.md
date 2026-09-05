# People discovery and private details — B03

Status: implemented and host-verified 2026-09-04; device/visual acceptance pending. The approved all-batch goal selects C013,
C014, C019 and C021; B01 and B02 are complete.

## Purpose and contract

Extend New Message and Person Profile while preserving the accepted hierarchy,
identity capsule, shared groups, relationship controls and conversation routing.
Search distinguishes people in chats, following and network results. A bounded
local directory stands in for resolving names, addresses and public keys; no
network request or cryptography is introduced. Invalid identifiers, no address
match, no profile, partial results, unavailable and retry are distinct. Local
matches remain usable while resolving. A non-member can receive an invitation
through the existing Android Sharesheet contract.

Private nickname and notes belong to the viewing profile's Person record.
Published name/about/address are unchanged. Clean nickname to one line and at
most 80 characters; clear restores the published name. Display names propagate
to chat titles, people/search, member and message authors, contact previews and
share text. Private notes are never included in shared content.

Creation records the canonical chat ID before attempting to open it. If opening
fails, **Chat created** explains the result and **Open chat** retries that ID.
Repeated actions cannot create another chat. Closing returns to Chats and discards only the pending open request; navigating
elsewhere also clears it. The chat remains available. Profile/route ownership
rejects late completion.

Person Profile offers **Nickname & notes**, **Start group**, and **Make admin
in groups**. Its group entry is **Groups in Common** when shared groups exist,
or **Add to a group** when there are none. Start group preselects this person in New Group.
Group selection includes only active groups where the viewer is an authoritative
admin, with the target absent for invites or a member for promotion. Loading,
partial/unavailable roster and retry never assume eligibility. Confirm selected
groups before applying. Partial results retain only failed selections for retry;
successful memberships/roles remain reflected. Commit-time checks repeat role,
membership and profile ownership validation.

## Composition, state and navigation

Reuse existing Settings groups, labeled rounded fields, native Material dialogs,
shared modal sheet and whole-row checkbox semantics. Search status uses ordinary
text with progress and retry, followed by source-labeled result groups. Empty
search retains the familiar people list. Notes are a multiline field in a
scrollable dialog. No new top-level navigation destination is needed.

Search and roster lookup are cancelable screen-owned effects keyed by query,
profile and retry attempt. Mutations and pending conversation identity are
ViewModel-owned. Developer Tools supplies deterministic search, creation and
roster/action outcomes. Sheet selection/drafts are bound to profile and person;
Back closes the top modal, then returns to origin. Standard Sharesheet retains
its platform appearance; no new permissions/services/storage.

## Evidence and acceptance

Production references are the pinned C013/C014/C019/C021 matrix links and the
RecipientResolution, ContactNicknamePreferences, ContactPrivateDetailsDialog,
profile-group picker and inviteProfileToGroups contracts. Existing Android
briefs `chats-and-chat-creation.md` and `chat-and-group-information.md` remain the
presentation authority over production styling.

Verify nickname cleaning, isolation and propagation; local/network discovery,
identifier states and retry; single canonical creation across projection retry;
role/roster eligibility and partial group application. Compile durable Compose
tests and run the clean README host gate. Device/visual acceptance stays pending.

Official Android sources checked 2026-09-04:
[checkbox row semantics](https://developer.android.com/develop/ui/compose/components/checkbox)
and [state ownership](https://developer.android.com/develop/ui/compose/state-hoisting).
Material owns touch targets/state layers; shared layout metrics govern custom
composition, with scrollable IME-safe dialogs and wrapping status text.

## Implementation evidence

- `model/PeopleModels.kt` defines nickname cleaning, name/address/public-key
  lookup, source labels, local/partial/unavailable/no-profile results and
  authoritative group eligibility. The directory is bounded and deterministic.
- `Person.displayName` preserves published metadata. `savePrivateContact` owns
  profile isolation and updates direct titles, contact attachments/drafts and
  latest-author previews. Conversation authors, search, mentions, roster,
  shared content and contact presentations resolve the private display name;
  existing published-name mentions remain navigable. Notes never enter sharing.
- `AppViewModel` deduplicates discovered people by public key, retains canonical
  created-chat identity, rejects stale route/profile completions and rechecks
  group permissions before every mutation. Partial actions retain successful
  state and return the failed group IDs for retry.
- `PeopleFlowUi`, `ChatCreationScreens` and typed navigation add status/retry,
  invitation handoff, private editor, preselected New Group and multiple-group
  selection/confirmation. The default New Message hierarchy and shared empty
  treatment remain. Developer Tools exposes the relevant deterministic outcomes.
- `PeopleDiscoveryTest` adds seven rule/lookup cases; `PeopleFlowStateTest` adds
  eight isolation/propagation/creation/role/partial-result cases. `PeopleFlowTest`
  adds seven compiled UI cases and the previous single-group test now follows
  multiple selection plus confirmation.
- The clean README gate passed **231 unit tests**, no failures/errors, zero
  lint errors, app APK and instrumentation-test APK. The same 14 pre-existing
  lint warnings remain. No device, Sharesheet execution or visual acceptance
  is claimed. `git diff --check` passed.

B03 started from clean B02 commit `7514ff7`. The audit's pinned production source
remains unchanged; B01/B02 edits are the only intervening implementation drift.
Commit title: `B03: Add people discovery and private contact flows`.


New Message also offers Connect with QR and Invite a friend before the people
list, without requiring an empty search. These reuse the existing Share &
Connect scanner/display destination and the public-profile invitation Sharesheet.
See `chats-and-chat-creation.md` for the 2026-09-05 entry-point follow-up.

## 2026-09-05 — User Profile action icons

Every action row has a decorative leading icon using the shared ProfileActionIcon
size/tint and grouped-row alignment: edit for Nickname & notes, group creation,
groups for Add to groups, admin shield for promotion, person add/remove for the
contact toggle and blocked/check for Block/Unblock. Contextual group actions use
admin/member and person removal symbols. Destructive actions retain semantic
error color. Labels remain the accessible action names; no duplicate icon labels.

The additional 24 dp rounded Material Symbols (person_remove,
admin_panel_settings, block) are Google's Apache-2.0 Android vectors from the
[official icon repository](https://github.com/google/material-design-icons/tree/master/symbols/android).
No behavior, navigation or dependencies change. Host compilation/lint is recorded
in the parity ledger; device and visual acceptance remain pending.

## 2026-09-05 — group-picker spacing and identity

The shared Add to groups / Make admin in groups sheet keeps search outside the
scrolling group list. A 24 dp field-to-list relationship (16 dp field bottom plus
8 dp list inset) separates the white search field from the first white row.
Search, list and pinned action share the 16 dp horizontal margin; list bottom
inset and pinned-action padding keep the last row clear of the button. Loading
feedback has an 8 dp progress-to-text gap. Material still owns sheet insets,
handle/header, row shapes, typography, touch targets and checkbox semantics.

Each group row now shows its existing 48 dp circular photo or initials avatar
on the leading side and the checkbox on the trailing side, matching other entity
pickers. Both invitation and promotion use this shared implementation. Group
identity, eligibility, search, whole-row selection, retry, confirmation and action
callbacks are unchanged. Decorative avatars do not repeat the group name to
accessibility services. Existing Groups in common and shared chat/entity pickers
already show group avatars and remain consistent.

Validation uses the host gate recorded in the latest parity ledger entry;
existing group-action interaction tests compile only. Device/visual acceptance
remains pending.

## 2026-09-05 — nickname dialog field contrast and app-wide audit

Nickname & notes now uses WhiteNoiseAlertDialog, the established ordinary-dialog
wrapper. Its neutral surfaceContainerLow canvas provides surfaceContainerLowest
to nested WhiteNoiseTextField controls, so empty nickname/notes inputs retain the
same visible white containers used in other dialogs, with semantic dark-theme
equivalents. Native labels, supporting text, focus/error rings, scroll behavior,
Cancel/Save, and private-detail validation remain unchanged.

The app-wide source audit also migrated the remaining default Material alerts
in DeveloperParityUi (package deletion), MessageDocumentUi (link review/failure)
and NostrEventUi (unavailable profile/event details). All ordinary AlertDialog
calls now resolve to the shared wrapper. Full-screen editors, media viewers,
and system-owned surfaces retain their separate presentation; inspected editing
surfaces already distinguish fields from their canvas. No dependencies or new
UI patterns were introduced. Validation is recorded in the parity ledger;
existing interaction tests compile only and device/visual acceptance is pending.

## 2026-09-05 — shared groups and invite entry

The profile shows exactly one group entry: Groups in Common with the existing
avatar stack when shared groups exist, otherwise Add to a group with its icon.
The shared-groups page pins the standard Add to Another Group primary button
(with group-add icon) below the list, outside its scrolling content. It opens
the existing eligible-group picker and confirmation flow. If shared groups
become empty while the page is open, the button reads Add to a group. Membership
changes recompute the entry from current profile chats; the sheet state is keyed
to profile and person. Existing retry, partial-success and permission guards
remain. `ChatsScreenTest` covers both profile branches and the bottom action.
