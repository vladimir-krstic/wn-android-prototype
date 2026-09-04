# Group setup, images and roster recovery

## Purpose and scope

B18 implements C072–C074 after B03/B11: create a group with no other members,
choose its initial disappearing timer, distinguish creation from later timer/open
failure, add private/public and emoji image editing, and verify member operations
against an authoritative roster. Preserve current Group Info, Edit Group, Person
Profile and Add People presentation and behavior. B19 owns transfer/disband/export.
All creation, upload, roster and mutation outcomes remain deterministic in memory.
No backend, networking, encryption, storage, new permission or device execution.

## Evidence and current-source reconciliation

Pinned production `319454889f1c2494dec4a69b5577d98017f44eee`: NewGroupSetupScreen,
NewGroupCreate, GroupEmojiImagePicker/Renderer, GroupImageWorkflow, GroupEditScreen,
GroupRosterMutationUi, GroupRosterLoadStatus and Controllers inviteMembers,
removeMember/setAdmin, authoritativeAdministrationTarget and roster refresh.
The 2026-09-04 master comparison through `911040c7e1c31652638c8cfd72812d1f3a694b9b`
allows a warm member/admin seed to present Add People while loading. Failed or
inconsistent rosters do not. The actual invitation commit still requires Ready.
Adopt that distinction without inventing an authoritative role from cached rows.
The extracted ChatListGroupSeed also preserves unrecoverable/disbanding/disbanded
lifecycle on cold account-switch frames. That C078 boundary belongs to B19. The
Controllers diff changes no member-roster or member-commit method; its authoritative
timeline ordering changes remain for final reconciliation.
Local iOS evidence remains `chats-and-chat-creation.md`,
`chat-and-group-information.md` and `docs/port/source-map.md`'s NewChatView and
ChatInfoView paths at the pinned iOS baseline.

## Product contract and states

- Continue without selecting people; explain that people can be added later.
  Creation includes the active profile as admin and uses existing chat relays.
  Choose an initial timer through the shared disappearing-message picker.
- Capture the form, profile and request. Create once, apply the requested timer
  afterward, then open the same chat. A failed timer leaves a usable group with
  its existing timer and offers retry or opening it. A failed open retains the
  created ID and read-only submitted details. Retry must never create another
  group or replay an already applied timer. Leaving does not delete a created chat.
- Group image and public invite image are separate. Private selection never
  replaces the public invite preview. Show their distinct consequences and
  previews. Private images use existing Photo Picker/Files/local web catalog;
  public images use explicit image selection with visible public context.
- Emoji image creation accepts one or two catalog emoji, supports removal and
  replacement, reports the two-item limit, checks glyph support and generates
  image bytes off the UI thread. Unsupported/render-failure states retain the
  selection and permit recovery. Use the existing emoji picker and native dialog
  actions, not a new emoji catalog. Generated images use a fixed neutral canvas.
- Preserve current Edit Group Save behavior: staged metadata and private/public
  images commit as one owned local operation. Loading/upload/save failures keep
  the edited draft and previous committed preview; retry uses prepared images.
  Stale callbacks cannot overwrite another profile/group or newer source state.
- Roster states are Unknown, Loading, Ready, Failed and Inconsistent. Retain known
  rows as context but explain that membership is being checked. Only Ready permits
  role-sensitive commits. A seeded member/admin may open Add People while Loading;
  its confirmation waits for Ready. Failed/inconsistent offer Retry.
- Add, grant/revoke and remove use owned pending, convergence, failure and retry
  states. Pending invites are visible and excluded from duplicate selection;
  per-person status accompanies the group commit lock. Recheck actor membership,
  role, roster revision and targets before applying. Stale failures do not replay
  a previously accepted commit. Back leaves accepted work visible in Group Info.
  Sign-out prunes work; profile changes invalidate unaccepted callbacks.

## Android composition, navigation and accessibility

Reuse current Settings groups, native Material dialogs, labeled text fields,
48 dp actions and the shared 4/8 dp relationships. Form content scrolls under the
existing inset-aware bottom action. Keep the native Photo Picker/OpenDocument
appearance. Image generation uses platform Bitmap/Canvas/Paint; glyph checks are
API 23-compatible. No custom navigation or gestures are needed.

Back dismisses emoji/photo choice or confirmation first, then leaves the editor
or setup; accepted group changes remain. Failed operations show named recovery
near their initiating surface and retain the original profile/group identity.
Text conveys loading, selection, failure, public visibility and role status;
color is supplementary. Preserve large type, RTL, keyboard focus, compact/expanded
widths, light/dark roles and ordinary TalkBack/button semantics.

## Exact copy

Create without other members; You can add people after creating this group.;
Creating group…; Applying timer…; Group created; The group was created, but the
disappearing timer wasn’t applied.; Retry timer; Open group; Retry opening;
Private group photo; Visible to group members.; Public invite image; Visible to
people viewing an invitation.; Create from emoji; Choose one or two emoji.;
Choose at most two emoji.; This emoji can’t be displayed on this device.;
Couldn’t create this image.; Use image; Remove emoji; Loading group photo…;
Saving group…; Couldn’t save the group image.; Retry; Discard;
Checking members…; Membership hasn’t been verified.; Couldn’t load members.;
The member list changed. Check it before trying again.; Invitation pending;
Updating member…; Updating member list…; Couldn’t update members.;
The group changed. Review your changes before saving again.
Existing accepted labels remain: Group Info, Edit Group, Add People, Make Admin,
Remove as Admin, Remove From Group, Save, Cancel and Disappearing Messages.

## Governing Android sources

Checked 2026-09-04:

- [Dialogs](https://developer.android.com/develop/ui/compose/components/dialog): native choice, confirmation and recoverable error boundaries.
- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker): existing permissionless Activity Result integration.
- [Paint](https://developer.android.com/reference/android/graphics/Paint): glyph support, measuring and platform image rendering.

## Implementation evidence

Implemented and host-verified 2026-09-04 for C072–C074. `GroupWork.kt` defines
roster authority, captured group forms, image destinations, operation phases and
emoji selection. `GroupWorkController.kt` owns create/timer/open stages, roster
refresh, member commits/convergence and atomic local edit recovery. Exact
profile/chat/request, roster revision and original metadata/image snapshots are
rechecked before applying. The AppViewModel primitives also enforce roster and
commit-lock gates; group-contact eligibility and moderation use the same authority.
Profile activation invalidates unaccepted work before a possible round trip.

`ChatCreationScreens.kt` permits explicit solo setup, captures an initial timer
and freezes submitted details, retaining the created ID through failures. Its
selected image comes from the owned submitted draft after recreation.
`GroupEditorScreen.kt` stages private/public previews separately, preserves
prepared failed drafts and commits through the existing Save boundary. Loading
failure cannot replace a newly selected image; image preparation blocks Save.
`GroupEmojiImage.kt` reuses the existing emoji picker, checks platform glyphs and
renders a measured one/two-emoji 512 px opaque neutral PNG off the UI thread.
Ready images are bound to the exact selected emoji, and selection is saveable.
Public invite images remain independent; invited chat/list headers use the public
preview while members see the private image. Existing image fixtures retain their
public preview; new private group images default to no public invite image.

`GroupWorkUi.kt` provides foreground completion effects, status/retry panels,
pending target identity and member-row state. Chat Info, Person Profile and Add
People use these states; warm Loading can open the picker but cannot confirm.
Unknown/failed/inconsistent rosters retain context and offer recovery. Accepted
member commits remain during convergence/navigation; stale unaccepted results
are rejected. Existing B03 multi-group actions retain their own progress/result
flow and now share the primitive authority/lock guards. Developer Tools exposes
creation, roster, member and image outcomes. No production service is created.

Validation: `/tmp/wn-b18-regression-check.log` and the full host gate
`/tmp/wn-b18-final.log` pass **575 unit tests**, zero failures/errors/skips,
zero lint errors, 14 pre-existing warnings and two hints. Debug and instrumentation
APKs assemble. Twenty-five new unit tests cover creation/timer/open isolation,
revision/role/target guards, locks/convergence/retry, profile exit, private/public
image boundaries and emoji selection; the prior empty-group rejection regression
now accepts the new solo-group contract. Ten new Compose/bitmap cases compile
only, covering solo/timer setup, pending/failure recovery, warm roster gating,
editor save/retry, shared emoji picker/Back and opaque image/glyph outcomes.

No device/emulator, runtime bitmap/gesture, screenshot, upload or visual result
is claimed. User visual acceptance remains separate. Production migration must
reconnect authoritative roster/commit events and image upload/private encryption;
local ticks, fixture bytes and outcome selectors are not production data layers.
The initial timer reuses current presets; B20 owns custom retention.
Commit title: `B18: Add group setup, image and roster recovery`.
