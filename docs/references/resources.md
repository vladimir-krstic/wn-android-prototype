# Product resource reference

For the current app-control glyph provenance and exact Google XML hashes, see
[Material Symbol control assets](material-symbols.md). Product images below
retain their separate pinned iOS provenance.

For the explicitly requested pinned Signal reaction artwork, exact upstream
commit, bundled legal files, and SHA-256 manifest, see
[Signal Android emoji asset provenance](signal-emoji-assets.md).

The reusable source material lives in the private `wn-ios-prototype`
repository at the accepted commit recorded in `ios-prototype.md`, under:

`WhiteNoisePrototype/Resources/`

It is read-only upstream evidence. Android production resources are
introduced only when a selected flow uses them.

## Captured resource groups

| Group | Source | Intended parity use |
| --- | --- | --- |
| White Noise mark | `Assets.xcassets/WhiteNoiseMark.imageset/white-noise-mark-black.svg`, `white-noise-mark-white.svg` | Welcome, support identity, brand surfaces |
| App icon source | `AppIcon.icon/Assets/Vector.svg` and `icon.json` | Evidence for a later Android adaptive/monochrome launcher icon |
| Primary people avatars | `Avatar*.imageset/` | Chats, direct conversations, people, groups |
| Web-choice avatars | `AvatarWeb*.imageset/` | Deterministic Sign Up avatar catalog; no runtime web fetch |
| Legacy chat avatars | `LegacyAvatar*.imageset/` | Retained catalog/story fixtures |
| Profile choices | `ProfileAvatar*.imageset/` | Profile creation, switching, and settings |
| Fiatjaf gallery | `FiatjafMedia*.imageset/` | One-to-five media story and shared-media coverage |
| Scanner backdrop | `QRScannerBackdrop.imageset/qr-scanner-backdrop.jpg` | iOS visual evidence only; do not fake an Android camera preview with it |
| Video | `ChatTrailClip.mp4` | Deterministic conversation/video viewer fixture |
| Documents | `ProjectBrief.pdf`, `ProjectNotes.pdf`, `TrailPlan.pdf`, `WeekendNotes.pdf` | Attachment rows, forwarding, and file-opening fixtures |

The asset-catalog `Contents.json` files remain in the pinned upstream commit as
provenance. They are not Android metadata.

## Import rules

- Copy only the files needed by the selected flow into the Android app. Never
  reference the iOS repository from Gradle or production code.
- Preserve the pinned upstream source unchanged. Derive optimized Android
  copies in `app/src/main/res/` with clear lowercase resource names.
- Keep photographs in an appropriate raster format and validate decoded size,
  memory use, crop behavior, color space, and accessibility description.
- Keep the White Noise mark as vector only if Android's vector pipeline renders
  it faithfully. Otherwise derive density-appropriate raster assets and record
  the decision.
- Build a true Android adaptive launcher icon with foreground/background and a
  monochrome layer; do not rename the Apple icon package and ship it.
- Never use the scanner backdrop as if it were a live camera. Android should
  show the official scanner or real camera surface for a real scan and a
  clearly developer-only deterministic state for tests.
- PDFs and MP4 files may use `res/raw` only when bundling them is part of the
  accepted deterministic fixture. Open external documents through content
  URIs and Android system contracts.
- Keep human-facing attribution or provenance required by an accepted brief.
  Do not invent authors, URLs, licenses, or spoken descriptions.
- Do not add a general-purpose image-loading dependency for bundled resources.
  Reconsider only when a selected requirement introduces real remote content.

## Android naming examples

These are naming guidance, not an instruction to import everything:

- `white_noise_mark_black`
- `white_noise_mark_white`
- `avatar_maya_chen`
- `profile_avatar_marmota`
- `fiatjaf_media_marmot`
- `chat_trail_clip`
- `weekend_notes`

Maintain a mapping in the selected brief whenever an Android resource name no
longer matches the upstream filename.

## Imported for onboarding and profiles

Batch 1 introduced the White Noise mark as
`app/src/main/res/drawable/ic_white_noise_mark.xml`, translated from the black
SVG path and tinted semantically by Compose. It also imported exactly the 21
accepted deterministic image choices into `drawable-nodpi`:

- five Fiatjaf animal images as `avatar_badger`, `avatar_fox`,
  `avatar_marmot`, `avatar_ostrich`, and `avatar_sloth`;
- seven web-choice portraits as `avatar_web_*`;
- `avatar_garden_club`;
- Marmota, Pebble, Open Circuit, Open Quill, Cipher Wheel, Free Signal,
  Public Voice, and Liberty Relay as `profile_avatar_*`.

The Kotlin `AvatarAsset` mapping is the authoritative Android resource lookup.

Batch 4 additionally imports the pinned `ChatTrailClip.mp4` and four PDF
fixtures as lowercase `res/raw` resources: `chat_trail_clip`, `project_brief`,
`project_notes`, `trail_plan`, and `weekend_notes`. They are opened only after
copying to an app cache file exposed by the non-exported FileProvider; the
upstream iOS repository is never read at runtime.
The original images and provenance remain unchanged at the pinned upstream
commit; the app performs no remote image loading. Device-selected images are
EXIF-corrected, scaled so their largest dimension is at most 512 pixels, and
JPEG-compressed before entering process-local state.

## Imported for Chats and chat creation

Batch 2 added 24 pinned portraits to `drawable-nodpi`: the thirteen named
story contacts from Maya Chen through Daniel Kim, Fiatjaf, and ten legacy
Nostr/cypherpunk chat avatars. Existing onboarding portraits are reused by
the developer catalog and deterministic People directory. No group or person
screen loads an image from a URL; Photos and Files selections reuse the Batch
1 preparation boundary, and Web choices remain bundled fixtures.

## Final resource audit

Batch 9 verified 45 distinct avatar mappings, five packaged raw handoff files,
and approximately 4.1 MB of authored resources. Avatar sources are at most 512
pixels on their longest edge, so no additional lossy recompression was applied.
Instrumentation resource-integrity checks cover the enum-to-drawable mapping,
launcher resource, PDFs, and MP4.

The launcher now uses a native Android adaptive icon on API 26 and later with
separate background, foreground, and monochrome layers. API 23–25 uses the
vector legacy fallback. The icon geometry derives from the pinned White Noise
source mark; no Apple icon package is included at runtime.
