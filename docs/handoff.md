# Android port handoff

The native Android port is functionally complete inside the approved offline,
deterministic boundary. This handoff describes the shape that should remain
stable while visual polish proceeds one screen or bounded flow at a time.

## Architecture map

```text
MainActivity
└── WhiteNoiseApp
    ├── WhiteNoiseTheme (profile-owned System / Light / Dark)
    ├── AppViewModel
    │   └── AppUiState
    │       └── Profile[]
    │           ├── People[]
    │           ├── Chat[] + timeline/draft/relay state
    │           ├── ProfileSettings + profile relays
    │           └── DeveloperToolsState + sanitized artifacts
    └── WhiteNoiseNavHost
        ├── Onboarding and profile routes
        ├── Chats and creation routes
        ├── Shared conversation and information routes
        └── Settings, support, developer, and destructive routes
```

There is one application module, one launcher activity, one authoritative
in-memory state holder, and one typed navigation graph. UI surfaces receive
immutable model values plus mutation callbacks. Projections—chat rows,
conversation clusters, search results, shared content, availability, and
diagnostics—derive from the authoritative state rather than becoming a second
store.

## Android system boundaries

| Capability | Android-native boundary | App permission |
| --- | --- | --- |
| Back and destination state | Navigation Compose and system/predictive Back | None |
| QR scan | Google Code Scanner system UI | None |
| QR generation | Local ZXing matrix rendered in Compose | None |
| Photos and videos | Android Photo Picker | None |
| Documents | Storage Access Framework | None |
| Camera attachment | External `TakePicture` contract and non-exported FileProvider | None |
| Share/copy | Android Sharesheet and clipboard | None |
| Read aloud | Android TextToSpeech with lifecycle shutdown | None |
| Notification/security controls | Explicit Android settings intents | None |
| Recents privacy | Activity `FLAG_SECURE` derived from active profile | None |

The merged application intentionally declares no network, camera, storage,
microphone, notification, or location permission. There is no backend,
transport, durable persistence, real authentication, cryptography, payment,
telemetry upload, microphone capture, or speech recognition.

## Verification boundary

The complete static gate is:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Unit tests execute on the host. Compose instrumentation tests are compiled and
packaged but are not run without an explicit emulator/device request. Static
coverage includes model consequences, fixture integrity, navigation values,
screen entry states, accessibility actions, adaptive width, 200% font scale,
RTL composition, and packaged resource identities.

## Screen-by-screen polish order

1. Welcome, Sign In, scanner handoff, Sign Up, and avatar selection.
2. Chats, scopes/search, row actions, empty/recovery states, and profile switcher.
3. New Message, Person Profile, group selection, and group setup.
4. Direct, group, support, invitation, ended, and relay-recovery conversations.
5. Composer, attachment shelf, media viewer, file/contact/GIF, and voice review.
6. Reactions, message actions, selection, forwarding, details, and search.
7. Direct/Group Info, shared content, member management, edit group, and Chat Relays.
8. Settings hub, Share & Connect, Profile, and Profile Keys.
9. Notifications, Appearance, Privacy & Security, Data Usage, and Profile Relays.
10. Support and Donate.
11. Developer Tools, Diagnostics, Key Packages, and Conversation Debug.
12. Sign Out, Manage Profiles, Remove Profile, and Erase App Data.

## Known polish and device backlog

- Run the packaged instrumentation suite on the chosen reference device/API.
- Inspect compact/expanded, landscape, gesture/three-button navigation, IME,
  cutout, 200% font, display scale, dark theme, and RTL states on device.
- Walk every flow with TalkBack and keyboard/D-pad focus; confirm switch and
  custom-action announcement quality.
- Exercise Photo Picker, Files, external camera, Sharesheet, clipboard,
  document/video viewer, TextToSpeech, settings intents, and Code Scanner on
  real system surfaces.
- Capture Android screenshots and compare hierarchy, density, typography,
  rhythm, shape, state color, motion, and empty/error/recovery treatment one
  selected flow at a time.
- Treat any visual changes as presentation refinements unless the user
  explicitly changes the accepted product behavior or offline boundary.
