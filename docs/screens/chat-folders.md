# Chat folders — B06

Selected 2026-09-04 under the all-batches goal. C027–C029; B05 provides manual
assignment. Implemented and host-verified; device/visual acceptance remains pending.

## Purpose and parity contract

Organize Chats with private, profile-owned folders. Folders supports New/Edit,
name, description, delete, Move Up/Down and Restore Default Folders. Blank names
cannot save. Delete removes only the folder. Seed Unread, Archived and Groups
once; all three can be renamed, re-ruled, reordered or deleted. Restore appends
only missing defaults and preserves edited defaults. Deleted defaults do not
reappear on navigation or profile changes. Keep Chats and Left discoverable.

Effective membership is manual IDs union automatic matches. People OR keyword
(title/group description, case-insensitive) provides the match; unread, groups,
archive side and include-muted constrain automatic matches only. With no people
or keyword, unread/groups/archive can stand alone. An empty rule, or include-muted
alone, includes nothing. Custom rules exclude muted chats by default; seeded
rules include muted chats to preserve existing Unread/Archived behavior.

## Entry, Back and composition

Folders opens from Settings and the end of the horizontal Chats pill row. Use the existing Settings detail
canvas, adaptive measure, native ListItem rows, shared tonal fields, switches,
menus and task controls. New/Edit is a typed profile/folder-owned destination.
Name and description precede Included Chats, automatic People/Keyword/constraint
controls and a live preview count. Included Chats and People use searchable
multiple-choice Material bottom sheets with checkbox row semantics, a fixed
search field and Done action, and lazy white segmented rows. Preview uses the
same sheet with read-only rows and lists the derived chats separately from
manual selection. Removing a manual ID may leave the chat
included by a rule; explain this relationship.

Save applies all folder values atomically. Back from a picker closes it; Back
from a dirty editor offers Discard Changes or Keep Editing. Nothing changes
before Save. Save failure retains the draft and supports retry. Missing folders
or wrong-profile routes cannot create or modify another folder. Draft fields and
picker choices survive recreation. Profile changes invalidate editing ownership.

Row/bulk assignment keeps B05's picker and result flow. Chat/Group Info adds Add
to Folder using the same picker and an owner-bound idempotent assignment callback.
Folder filters and preview counts recompute from current chat/read/mute/archive,
name and membership state. A deleted selected folder falls back to Chats.

## Copy and accessibility

Use Folders, New Folder, Edit Folder, Folder name, Description, Included Chats,
People, Keyword, Unread only, Groups only, Archived only, Include muted chats,
Preview and Restore Default Folders. Delete consequence: “Delete this folder?
Your chats will stay where they are.” Show counts with plurals and distinguish
manual membership from automatic rules. State changes have text, never color
alone; reorder is available through menus and TalkBack custom actions. Preserve
native target sizes, focus, RTL, large-font growth, scrolling, IME and system
insets. No new custom drawing or gestures.

## Sources and boundaries

Production is read-only whitenoise-android@319454889f1c2494dec4a69b5577d98017f44eee:
core/ChatFolderRules.kt; state/ChatFolderPreferences.kt; ui/settings/
ChatFoldersScreen.kt and ChatFolderEditScreen.kt; ui/chats/ChatFolderPickerSheet.kt.
See [audit F03](../audits/production-android-parity/flows/F03-chat-folders-and-organization.md),
[B05 brief](chat-organization-and-recovery.md) and the approved
[Chats composition](chats-and-chat-creation.md). Folders extend the accepted iOS
scope with explicitly authorized production Android capability.

Official sources checked 2026-09-04:
[Switch](https://developer.android.com/develop/ui/compose/components/switch),
[state saving](https://developer.android.com/develop/ui/compose/state-saving),
[Checkbox](https://developer.android.com/develop/ui/compose/components/checkbox).

Only deterministic in-memory state. No persistence, backend, network, permissions,
services or dependencies. Host validation only; no device/visual acceptance claimed.

## Observable acceptance

Test rule OR/AND behavior and manual exceptions; empty rules; direct counterparts
and group members; case-insensitive title/description matching; live membership;
default editing/deletion/restore and order bounds; profile isolation, atomic save,
blank/missing rejection, assignment idempotence and filter fallback. Compile UI
coverage for entry points, editor discard/restoration, picker semantics, live
preview, deletion consequences, reorder and contextual assignment.

## Implementation evidence

- `ChatFolderModels.kt`: pure manual-union-rule evaluation, category constraints,
  counterpart/roster matching, invariant keyword folding, live projections,
  editable defaults, bounded movement and idempotent restoration.
- `Profile.chatFolders` seeds the three defaults only on construction.
  `AppViewModel.saveChatFolder/deleteChatFolder/moveChatFolder/restoreChatFolders`
  guards owner and folder identity; saves are atomic, deletion preserves chats,
  deleted defaults stay deleted, and assignment is idempotent.
- `ChatFolderScreens.kt`: Folders management, count/description rows, native
  overflow and TalkBack Move actions, consequence-aware deletion, restoration,
  draft-only New/Edit with explicit Save/discard/retry, searchable checkbox
  pickers and a derived Preview. Typed routes carry the owning profile ID.
- `ChatsScreen` replaces hardcoded Unread/Archived menu entries with their
  editable folders, adds Groups and Folders, keeps Chats/Left and falls back to
  Chats when the selected folder disappears. Row/bulk quick creation remains
  available; full editing is in Folders. Chat/Group Info uses the shared picker.
- `ChatFolderRulesTest` and `ChatFolderStateTest`: 15 new unit tests cover rule
  semantics, manual exceptions, live mutations, defaults/order, atomicity and
  owner/target guards. B05's folder tests now account for the seeded defaults.
- `ChatFolderFlowTest`: nine compiled cases cover both navigation entry points,
  profile-bound routes, discard, restoration and failed-save draft retention,
  picker semantics, preview, default deletion/restore, accessible movement,
  selected-folder fallback and Chat Info assignment. They were not executed.
- The clean README gate passed **277 unit tests**, zero failures/errors/skips,
  zero lint errors and both APKs, with the same 14 pre-existing warnings.
  `git diff --check` and new documentation-link checks passed. No device,
  emulator or visual acceptance is claimed.

Commit title: `B06: Add folder management and automatic rules`.


## Folder icon polish — 2026-09-05

The Settings Folders entry uses the native folder symbol in place of Filter.
Every folder row on the management page uses the same 24 dp leading folder
symbol, including built-in and custom folders. Semantic onSurfaceVariant tint
matches adjacent Settings icons. The icon is decorative so TalkBack announces
the folder name/count once. Row actions and labels are unchanged. The official
asset and hash are recorded in `../references/material-symbols.md`.

Host validation: debug APK assembly and whitespace checks pass. No behavior
tests were added for the decorative icon change; device inspection is pending.

## 2026-09-05 horizontal filter and folder pills

Latest user direction replaces the regular Chats header filter icon/menu with
Signal-style horizontally scrollable pills beneath the app bar. Chats appears
first, followed by saved folders in management order, Left, and the Folders
management action with a folder icon. A selected pill has a neutral gray fill;
unselected pills are transparent, without outlines or checkmarks. All pills
retain the same capsule shape when selected. The app bar has no duplicate scope
title. Search and selection hide this row; advanced search retains its anchored
filter menu.

Native Material FilterChip owns input, selected semantics, typography, ripple
and touch targets inside a LazyRow. Shared 16 dp side margins and 8 dp gaps
keep the row aligned with the chat pane. This follows the current official
[Compose chip guidance](https://developer.android.com/develop/ui/compose/components/chip)
(reviewed 2026-09-05), with the user-requested capsule/tonal treatment.

Filtering still uses ChatProjection/ChatFolders; tapping the active pill keeps
it selected. Selection survives recreation and folder reorder, with an offscreen
selected pill scrolled into view. Deleting it falls back to Chats; switching
profiles resets selection. Selecting a pill closes any open row menu. Existing
folder creation, assignment and management destinations remain connected.

Validation covers the existing folder/filter interaction tests migrated to pills
and a new distant-folder scroll, restore, reorder, search-return and profile-reset
regression. Current-build device inspection and visual acceptance remain pending.

Host gate: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes with 895 unit tests and zero lint errors. Both APKs assemble; Compose
interaction tests compile only. `git diff --check` passes.
