# Composer media acquisition and attachment actions — B11

## Purpose and scope

Complete C046/C049/C051/C052/C054/C057 from the production Android audit while
preserving the accepted composer, media viewer and person-sharing presentation.
The user selected all batches and one verified commit per batch. This batch uses
in-memory media/state, permissionless system pickers and existing outbound file
sharing. Q06 continues to exclude real library grants and transport services.
Q07 retains explicit Android document destinations for Save.

## Parity contract

- Message actions share text and attachments and save selected attachments.
  Each item reports prepared/saved/unavailable/cancelled/failed independently;
  opening the Sharesheet means handed to Android, never delivered to a recipient.
- A recent-media sheet exposes none, selected-only, full and unavailable states
  with bundled content; Gallery always launches the standard Photo Picker.
- Draft photos offer Low, Standard, High and Original. Preserve original source
  in memory for reversible changes; discard that source from outgoing messages.
  Display the actual prepared byte count. JPEG/PNG Original strips identifying
  metadata without resizing where supported; other images use a disclosed safe
  re-encode. Never describe videos/files as metadata-stripped photos.
- Attachment transfer state belongs to profile/chat/message/attachment identity;
  queue, progress, cancellation, retry, cache loss, expiration and invalid-source
  states cannot be replaced by a stale completion or another profile's result.
- Decode bundled animated content with the platform; stop it in the background
  and expose static/error recovery where animation is unsupported or invalid.
- Keep White Noise Person separate from Device Contact. The latter picks one
  granted phone row, previews selectable fields, and queues a vCard with readable
  text. It never queries the entire address book or infers a White Noise profile.

## Composition, navigation and copy

Retain the accepted Add popup, ordered draft shelf and media viewer. Use native
Material sheets/dialogs, radio buttons, checkboxes, progress and list rows, with
shared margins/spacing. New labels include “Recent media”, “Device contact”,
“Photo quality”, “Original”, “Save attachments”, “Share”, “Retry” and “Cancel”.
Errors distinguish unavailable bytes, no compatible app, expired and invalid
attachments. Exact final copy lives in string resources.

Back dismisses the innermost choice/preview without sending or discarding the
ordinary draft. System picker cancellation changes no draft. Import completion
must respect owner and request generation. Export completion cannot update a
different message. Selection and progress have accessible labels, ordinary
touch targets and non-color state descriptions; layouts scroll at large text
and support RTL, landscape, system insets and keyboard dismissal.

## Sources and differences

Production evidence is pinned to `319454889f1c2494dec4a69b5577d98017f44eee`:
`MessageOutboundShare.kt`, `MessageAttachmentSave.kt`, `RecentMedia.kt`,
`AttachmentSheet.kt`, `PhotoQualitySelector.kt`, `MediaQuality.kt`,
`MediaPipeline.kt`, `AttachmentTransferCoordinator.kt`,
`AnimatedAttachmentImage.kt`, `UserShare.kt`, `ContactShare.kt` and
`ContactPreviewScreen.kt`, linked in the capability matrix. Existing iOS intent
is recorded in `composer-media-and-speech.md` and `docs/port/source-map.md`.

Current official sources consulted 2026-09-04:
- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker): selected-media access and unchanged system appearance.
- [Contact picker intents](https://developer.android.com/guide/components/intents-common#Contacts): permissionless selected data rows.
- [Document creation](https://developer.android.com/training/data-storage/shared/documents-files#create-file): explicit save destinations.
- [Contact details](https://developer.android.com/identity/providers/contacts-provider/retrieve-details): scoped contact data.
- [Android sharing](https://developer.android.com/develop/ui/compose/sharing/send): stream grants and multi-item handoff.
- [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder): platform image decoding.

## Acceptance and validation

Exercise mixed export results, picker cancellation, source/quality reversibility,
metadata stripping, contact-field omission and escaping, transfer ownership and
stale completion, and decoded animation fallback. Host unit tests and APK/lint
gates are required. System-surface execution, visual review, animation playback
and user acceptance remain pending explicit device-inspection authorization.

Status: implemented and host-verified 2026-09-04. Device execution and user visual acceptance remain pending.

## Implementation evidence

The clean gate `./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes **381 unit tests**, with zero failures/errors/skips, zero lint errors,
the same **14 pre-existing warnings** and both APKs. The 20 new host cases in
`AttachmentModelsTest` and `AttachmentStateTest` cover metadata/container bounds,
GIF sanitation, selected contact fields, transfer phases, session/revision and
message ownership, draft-quality isolation, and preserved source/destination IDs.
`AttachmentAcquisitionTest` adds **11 compiled UI/platform cases** for field
selection/cancel, recent access, quality choice, transfer controls, real encoded
sizes/EXIF removal, GIF decoding, mixed export and navigation during upload.
These cases were not executed on a device or emulator.

High retains the approved 4096px/JPEG95 default rather than reducing existing
imports to production's Standard default. Low and Standard use 1024/70 and
2048/85. Original JPEG/PNG uses an allowlist metadata pass; EXIF rotation,
Adobe color interpretation and unsupported containers fall back to safe decoding
and re-encoding at up to 4096px with a visible explanation. PNG alpha is retained.
Inputs are capped at 32 MiB, while each explicit outbound file export is capped
at 128 MiB. Prepared outputs retain actual dimensions/bytes and sources only in
the draft. Albums preserve all frame identities during quality changes.

`WhiteNoiseModalBottomSheet`, `WhiteNoiseSheetHeader` and `WhiteNoiseAlertDialog`
keep the established chrome; quality uses native radio semantics. The recent
strip's four access states are bundled, app-owned fixtures; Q06 still excludes
real library permission requests. The device contact contract intentionally
queries only the selected phone row, with name/phone and no address-book or
email-table read. The card model also supports an explicitly supplied email.

Export uses unique cache directories with human filenames, Android stream grants,
multi-item MIME negotiation and explicit per-item document destinations. A
cancelled destination stops the remaining queue; Retry preserves prior saves.
Profile/contact/text-only sharing uses human content and the existing public-key
identity. Sharesheet handoff does not claim delivery. Unknown filenames are not
substituted with unrelated sample files. Export and composer sheets suppress
underlying unread acknowledgment. Cached export files remain temporary files
needed by the recipient's URI grant, not a product database.

Transfers have one authoritative attachment record, revision-checked events and
an activity-foreground host outside navigation. Queued uploads continue when
opening Settings, pause in the system background and become cancelled/retryable
on a profile/session change. Deleted messages cannot regain attachments from old
completion. Download/transport outcomes remain deterministic; production must
reconnect the cited coordinator and cache authority.

The bundled GIF is a 160×96, 12-frame neutral dot animation generated as a
technical fixture. Its names now describe that content. Platform ImageDecoder
plays it on API28+; older supported devices receive a sampled static frame.
No Android decoder/playback, real picker, save destination or recipient app was
run in this task. Host build/logic evidence does not constitute visual acceptance.


Source anchors: [MessageAttachmentExport.kt:47](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/MessageAttachmentExport.kt:47), [AttachmentAcquisitionUi.kt:100](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/AttachmentAcquisitionUi.kt:100), [DraftPhotoProcessor.kt:18](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/DraftPhotoProcessor.kt:18), [AttachmentModels.kt:41](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/model/AttachmentModels.kt:41), [AnimatedAttachmentImage.kt:32](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/AnimatedAttachmentImage.kt:32), [AttachmentAcquisitionUi.kt:65](/Users/vladimirkrstic/Workspaces/wn-android-prototype/app/src/main/java/dev/ipf/whitenoise/ui/conversation/AttachmentAcquisitionUi.kt:65).
