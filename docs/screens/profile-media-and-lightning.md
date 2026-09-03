# Profile media and Lightning address — B04

Status: implemented and host-verified 2026-09-04; device/visual acceptance pending.
C016–C018 were selected by the all-batch goal.
B03 commit `a774791` is the clean starting point. The production audit baseline
remains pinned; previous Android decisions and profile hierarchy govern styling.

## Behavior and composition

Own Profile stays read-only until Edit. Add an optional wide banner above the
existing avatar and fields; omit it when absent outside edit. Banner/Avatar
controls reuse the approved menus and system Photo Picker/Files contracts, plus
the bounded web-image catalog and URL mode. Loading disables Save, failure
preserves the previous image, Retry repeats the selection, and Remove changes
only the draft. Back cancels uncommitted image work and restores saved data.

Tapping an available own/other-profile avatar or banner opens a full-screen
Material dialog with fit-to-window image, native transformable zoom/pan, visible
zoom/reset controls, Back reset/close and Save using the established system
document destination. Own media also exposes Edit profile. Monograms have no
image-viewer action. Person Profile and direct Chat Info share the image-entry
behavior. Existing conversation media behavior stays intact.

Name offers **Suggest name** while editing; deterministic suggestions update
only the draft and require Save. They never publish from the suggestion action.
Lightning address is optional and separate from Verified Nostr Address. Validate
local/domain syntax (including path/port rejection and lowercased domain), show
**Checking address…**, and distinguish **Address could not be verified** from
**No connection**. The save request freezes all profile fields and owner, checks
a changed nonblank Lightning address, then publishes atomically. A failed check
or publish changes nothing; edit/retry or Back cancels the request. Inactive
owners and duplicate callbacks cannot receive completed changes. Clearing the
address is allowed. Read-only other-profile display has no payment action.

Use the existing monochrome form fields, semantic errors, app bar and pinned
Save action. Current shared metrics govern spacing, scrolling, touch targets and
large text; no production styling or new branding color is imported. Banner
aspect ratio is 2:1 within the established content pane; crop only its preview,
while full-screen viewing retains the full image. The image viewer uses native
Compose transformable because fit-only Image does not provide pan/zoom.

## Scope and validation

All profile/publication, Lightning and web-image results remain deterministic
in memory. No network lookup, payment, upload, crypto, persistence or permission
is added. Local image selection and document output reuse existing authorized
platform boundaries. Developer Tools supplies save/check/image failure cases.

Production evidence: C016–C018 matrix links to ProfileEditScreen, AvatarViewer,
Lud16Resolver and ProfilePseudonymGenerator. Accepted Android references:
`settings-and-profile-services.md`, `chats-and-chat-creation.md`,
`chat-and-group-information.md`, `docs/ui-metrics.md` and product terminology.

Acceptance covers draft-only suggestions/images, cancel/reset, optional and
invalid Lightning input, checking/unresolved/offline/publish failures, atomic
save and owner/retry guards, available image entry/zoom/save and other-profile
read-only content. Run relevant unit tests and the complete README host gate;
compile UI tests only. Device/visual acceptance remains pending.

Official sources checked 2026-09-04:
[Compose pan/zoom](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch),
[Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker),
and [document creation](https://developer.android.com/training/data-storage/shared/documents-files).

## Implementation evidence

- `ProfileEditorModels.kt` defines Lightning normalization, immutable profile
  drafts/save phases, deterministic name suggestions and fitted-image pan
  bounds. `Profile` and `Person` gain optional banner/Lightning metadata.
- `AppViewModel.beginProfileSave`/`advanceProfileSave` freeze the owner and
  entire draft, verify a changed nonblank Lightning address and apply all fields
  together. Failure/retry, duplicate/stale callbacks and cancel are guarded.
  Image drafts survive recreation in the ViewModel and clear on cancel/owner
  change; an explicitly removed banner remains removed in the draft.
- `EditProfileScreen` preserves read/edit modes and the pinned Save action,
  adds draft-only suggestions and media controls, and drives request phases
  only while resumed. Other-profile banner/Lightning and avatar entry are
  shared through `PersonIdentityHeader`, including direct Chat Info.
- `ProfileMediaUi.kt` reuses native image/document contracts and the bounded
  web-image URL catalog. Picker results and image preparation remain owned by
  the initiating editor. Failed selection retains the old image; retry consumes
  a one-shot failure. Full-screen images expose fit/zoom/pan/reset/save with
  visible accessible controls. Viewing while editing does not reset the draft.
- `ProfileEditorModelsTest` adds five validation/suggestion/geometry cases;
  `ProfileEditorStateTest` adds eight atomic-save/failure/owner/image-draft
  cases. `ProfileEditorFlowTest` adds seven compiled interaction/restoration
  cases. No new UI tests have been executed on a device.
- The clean README host gate passed **244 unit tests**, zero failures/errors,
  zero lint errors and both APKs. The same 14 pre-existing lint warnings remain.
  `git diff --check` passed. No device, picker/viewer interaction or renewed
  visual acceptance is claimed.

Commit title: `B04: Add profile media and Lightning address editing`.
