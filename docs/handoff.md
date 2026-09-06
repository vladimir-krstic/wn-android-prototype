# Android architecture and handoff

The original iOS port and production Android batches B01–B32 are implemented.
Current product contracts live in [screen briefs](screens/README.md), governed by
[decisions](decisions.md) and [shared metrics](ui-metrics.md). The
[parity ledger](port/feature-inventory.md) records implementation evidence and
remaining acceptance gaps. Completed batch history lives in the
[production implementation record](audits/production-android-parity/implementation-plan.md).

## Architecture and ownership

One application module and launcher activity host a typed Navigation Compose
graph. `MainActivity` enters `WhiteNoiseApp`, which composes the profile-owned
theme, app-wide lifecycle hosts, privacy handling, and `WhiteNoiseNavHost`.

`AppViewModel` owns `AppUiState`: profiles, signed-in membership, the active
profile, and first-login diagnostics state. Each profile owns people, chats,
timelines/drafts, settings, relays, and developer artifacts. UI receives model
values and callbacks. Chat rows, message documents, search results, availability,
and shared content derive from that state rather than becoming another store.

Operation controllers in `state/` own staged group work, lifecycle changes,
retention, capture, incoming sharing, notification actions/settings, transcript
export, audit logs, relay publication, developer diagnostics, app lock, and app
updates. Their callbacks re-check the initiating owner and operation revision.
Accepted stages survive retry where the product contract requires it; stale
results cannot mutate another profile or chat. Controllers remain in memory.

Activity-wide hosts preserve work that should survive navigation, such as
foreground attachment transfer progress. Screen-owned media, image preparation,
recognition, and readers must release or cancel work at their lifecycle boundary.
Process recreation resets product state; navigation must return safely to
onboarding when a restored destination has no corresponding active profile.
Temporary content files support Android URI handoff, not durable product storage.

## Android system boundaries

| Capability | Integration | App permission |
| --- | --- | --- |
| QR scanning | Shared CameraX and bundled ML Kit scanner | Camera, requested from the scan action |
| QR generation | Local ZXing matrix | None |
| Photos/videos | Standard Android Photo Picker | None |
| Files/save destinations | Storage Access Framework | None |
| Camera attachment | `TakePicture` and non-exported FileProvider | Camera: also required for this intent because the app declares it |
| Share/copy | Android Sharesheet, URI grants, clipboard | None |
| Video | Foreground Media3 player for local content | None |
| Read Aloud | Local Android TextToSpeech with lifecycle shutdown | None |
| Global voice search | Provider-owned recognition activity | Provider owns its permission flow |
| Composer dictation | Android SpeechRecognizer, partial text in the editor | Microphone, requested when starting dictation |
| Notification access | Explicit Android 13+ permission/settings action | Notifications |
| Security/settings | Approved Android settings intents | None |
| Window privacy | Central policy for Recents and chat capture | None |

The manifest declares `CAMERA`, `RECORD_AUDIO`, and `POST_NOTIFICATIONS`.
It removes inherited network and wake-lock permissions and declares no storage
or location permission. Speech service processing belongs to the installed
provider; the app itself has no network transport. Live dictation and native voice
search supersede their original deterministic-only acquisition paths; explicit
Developer Tools outcomes remain available for tests.

Real notification delivery, background services, voice-note encoding, GPS/maps
acquisition, incoming exported Android receivers, shortcut publication, installer
work, backend authentication, cryptography, payment, and persistent product state
remain outside the approved prototype boundary. See the relevant screen brief
before changing any integration; historical batch restrictions are superseded
only by the recorded user-approved capability.

## Verification and next work

Use the exact setup and host/device commands in [README](../README.md#build-and-verification).
The [engineering audit](codebase-hardening-audit.md) records dated results.
Compiling instrumentation tests does not execute their interactions or establish
accessibility/visual acceptance.

The authorized 2026-08-31 Pixel 8a / Android 17 inspection is historical evidence
for that build: 168 instrumentation tests passed, with selected direct inspection.
It does not verify subsequent parity work or UI changes. Details remain in the
[historical audit](codebase-hardening-audit.md).

Future device testing requires an explicit request in the current task. Remaining
checks include:

- Full navigation, system/predictive Back, recreation, and profile isolation.
- Live dictation permission/partial/error/pause/resume and voice-search return.
- Photo Picker, Files, camera/scanner, Sharesheet, clipboard, document export,
  TextToSpeech, Android settings, and video play/seek/audio-focus/release.
- TalkBack, keyboard/D-pad, Switch Access, Voice Access, focus and custom actions.
- Large text/display scale, RTL, light/dark, compact/expanded widths, landscape,
  IME, cutouts, gesture/three-button navigation, and reduced motion.
- User-led product and visual acceptance, one screen or bounded flow at a time.

Apply the [visual contract](visual-polish.md) and current screen brief during
future polish. Do not treat old screenshots, rollout plans, or test totals as a
current visual specification or authorization for device use.
