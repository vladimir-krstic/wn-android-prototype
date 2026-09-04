# Downloads and media quality — B27

## Purpose and scope

Complete C105–C107 in the existing Data Usage flow: profile-owned automatic
download rules, stop/restart of waiting automatic work, and global photo/voice
quality. Keep deterministic in-memory state and the existing foreground transfer
driver. No networking, background workers, permissions or real voice encoder.

## Parity contract

- Photos, Videos, Audio and Files each offer Wi-Fi, Mobile data, Roaming and
  Metered network switches. Every matching condition must allow a download;
  an unknown/offline network admits nothing automatically. Accepted queued work
  survives later policy changes. Defaults remain photos/audio on unmetered Wi-Fi,
  videos/files off; reset restores only the matrix.
- Stop confirmation pauses admission for this profile and clears only queued
  automatic requests. Active, manual and upload requests continue. An explicit
  tap promotes queued automatic work to manual without duplicating identity.
  Per-file cancellation suppresses automatic readmission until manual retry or
  Restart. Restart clears suppression only in the owning profile.
- Queue status derives from canonical message attachments. Deleted/expired
  messages and stale callbacks cannot materialize bytes. Transfer failures,
  cache misses, retry and unavailable bytes retain B11 recovery behavior.
- Low, Standard, High and Original apply to future photos and new voice drafts.
  High preserves the accepted 4096px/JPEG95 photo default. Existing prepared media
  retains its captured quality; the per-draft selector remains an explicit
  override. Voice captures the 32/64/96/96 kbps production policy at recording
  start; deterministic sample bytes are not misrepresented as newly encoded.
  Photos use B11 metadata stripping, including Original safe fallback. Imported
  videos and audio files are sent as-is.

## Entry, Back and composition

Settings → Data Usage keeps its title, media-first grouping, shared Settings
canvas, metrics, adaptive content bound and section help. Each media row opens
a scrollable Material dialog with four independent native switches; changes
apply immediately and Done/Back dismiss. Quality uses the existing shared radio
dialog. Stop uses a consequence-specific Material confirmation; Cancel/Back
leaves work unchanged. Profile identity keys modal state and callbacks.

## Product copy

“Automatic downloads”, “Photos”, “Videos”, “Audio”, “Files”, “Wi-Fi”, “Mobile
data”, “Roaming”, “Metered network”, “Never”, “Reset download settings”.
“All matching network rules must allow a download. You can always download
media yourself.” “Download queue”, “Automatic downloads are paused”,
“Stop automatic downloads”, “Restart automatic downloads”. Stop consequence:
“Clear waiting automatic downloads for this profile? Active downloads and
downloads you started yourself will continue.” “Media quality”;
“Applies to new photos and voice messages. Videos and audio files are sent
as-is.” Photo privacy help covers photos only, including Original.

## Accessibility, adaptation and integrations

Native switch/radio/button semantics, whole-row targets, localized labels and
textual queue state; no state relies on color. Dialog content scrolls at large
type and short/expanded windows. Shared scaffold owns insets and Back. No new
system integration; existing picker and image-processing contracts remain.
Network/queue examples are developer-only, never device connectivity claims.

## Governing evidence

- [Compose switches](https://developer.android.com/develop/ui/compose/components/switch)
  and [dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
  checked 2026-09-04: standard controls and modal dismissal.
- [Source map](../port/source-map.md): accepted iOS Settings brief,
  `Screens/Settings/PreferenceSettingsViews.swift`, `PrototypeSettingsState.swift`.
- [Production matrix](../audits/production-android-parity/capability-matrix.md),
  C105–C107, pinned `319454889f1c2494dec4a69b5577d98017f44eee`:
  `state/MediaAutoDownloadMatrix.kt`, `state/MediaQuality.kt`,
  `state/AttachmentDownloadGate.kt`, `ui/conversation/media/AttachmentDownloadPolicy.kt`,
  `ui/settings/AutoDownloadScreen.kt`, `state/AppState.kt`.
- [B11 media brief](composer-attachment-actions.md) and decisions 0068/0069/0131
  govern existing grouping, dialog alignment, photo quality and metadata floor.

## Observable acceptance and validation

All 16 cells, overlapping/unknown conditions, reset isolation, stop/manual/active
distinction, promotion, retry suppression, profile isolation and stale revisions
have host tests. Quality reaches new media without retroactively changing draft
bytes. Durable Settings interactions and semantics compile as Compose tests.
Run the README host gate; device execution and visual acceptance remain pending.

## Implementation evidence

Implemented and host-verified on 2026-09-04. `MediaDownloads.kt` owns the 16-cell
matrix, overlapping-condition admission, canonical queue counts and effective
photo policy. `AttachmentTransfer` records automatic/manual origin, suppression,
promotion and revision-safe stop/retry. AppViewModel owns atomic profile mutations
and a two-slot deterministic foreground download gate. The existing transfer host
continues independent of the visible chat. Profile departure retains the existing
B11 cancellation boundary; this is not background transfer execution.

`DataUsageScreen.kt` preserves Data Usage and media-first groups with native
switch/radio/confirmation dialogs. Developer Tools can load existing local
attachments into a held queue and choose network/outcome examples. Production
connectivity, WorkManager, intent persistence and AAC encoding remain seams.

Global quality feeds the B11 photo processor. A captured voice policy survives
recording/review/restoration and reaches the sent attachment. Existing prepared
photos retain bytes/quality, and a successful send clears the draft override.
The formerly inert Standard label now reflects the already accepted High photo
default (4096px/JPEG95), while Low/Standard/Original remain explicit choices.
No claim is made that fixed voice sample bytes were re-encoded.

`MediaDownloadPolicyTest` and `MediaDownloadStateTest` add 19 passing rule,
queue/revision/profile, recovery and quality cases. `MediaDownloadInteractionTest`
adds six compiled UI cases; SettingsScreenTest updates the existing layout/reset
case for 16-cell policy and four quality modes. The full host gate
`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
passes 791 unit tests with no failures/errors/skips, zero lint errors and both
APKs. Existing lint warnings/hints remain. No device tests or visual inspection.

Current production `master` is `911040c7e1c31652638c8cfd72812d1f3a694b9b`.
Its matrix, quality, gate and policy contracts are unchanged from the pinned audit.
IntentStore additions concern installer ownership; Worker changes include strict
protocol ID lengths and durable-work observation. Those belong at production's
installer/worker seam and do not add services or durable storage here.
