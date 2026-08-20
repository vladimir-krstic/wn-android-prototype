# White Noise iOS Prototype

A fast, native iPhone workspace for building and refining the White Noise
product one screen or bounded flow at a time.

## Current state

The prototype currently includes:

- Welcome, Sign In, private-key QR scanning, and Sign Up.
- Populated, empty, unread, archived, and left-chat list states, including pin
  reordering, search, profile switching, and direct- or group-chat creation.
- Direct, group, support, invitation, and developer-catalog conversations with
  text, media, reactions, replies, attachments, message actions, search,
  disappearing-message indicators, and deterministic voice-message behavior.
- Chat and group information, member and person profiles, shared-media paging,
  relay selection, and verified-address presentation.
- Settings, profile editing and switching, Profile Keys, appearance,
  notifications, privacy, storage, relays, support, donations, Developer Tools,
  sign out, and profile removal.
- Share & Connect with local QR generation and native VisionKit scanning.

All product state is deterministic and process-local. There is no backend,
networking, persistence, authentication, or cryptography.

## Self-contained project knowledge

All White Noise product rules, terminology, decisions, screen requirements,
assets, and implementation context required for normal work live in this
repository. Work must not depend on another local checkout or project-specific
skill.

Official Apple documentation is an intentional live authority. Agents use the
local index to open current Apple sources before material component or
interaction decisions.

- [AGENTS.md](AGENTS.md) — authority, boundaries, workflow, build, and
  inspection rules.
- [Project decisions](docs/decisions.md) — durable project decisions.
- [Product language](docs/product-language.md) — product voice and
  interface-writing rules.
- [Terminology](docs/terminology.md) — canonical White Noise product terms.
- [Apple reference index](docs/references/apple.md) — organized official Apple
  source router.
- [Native UI evaluation](docs/references/native-ui.md) — local
  native-component and review method.
- [Screen briefs](docs/screens/) — decision-complete briefs for implemented
  screens.

User-supplied Figma links, GitHub issues, shipped-app examples, and asset source
links are optional provenance or comparison evidence. They are not required to
understand or implement a screen.

## Requirements

- Xcode 27 beta at `/Applications/Xcode-beta.app`
- iOS 27 simulator
- SwiftUI and public Apple APIs only
- iPhone Air for requested simulator inspection

## Build

```sh
DEVELOPER_DIR=/Applications/Xcode-beta.app/Contents/Developer \
xcodebuild \
  -project WhiteNoisePrototype.xcodeproj \
  -scheme WhiteNoisePrototype \
  -destination 'generic/platform=iOS Simulator' \
  build
```

Open `WhiteNoisePrototype.xcodeproj`, select the shared
`WhiteNoisePrototype` scheme and the iPhone Air running iOS 27, then press Run
only when hands-on inspection is requested.

The app follows the system appearance by default and also provides a
process-local Appearance setting.

## Device Hub and screenshots

- Reuse the booted iPhone Air in Device Hub. Do not boot a second simulator
  unless another device is explicitly requested.
- Check `xcrun simctl list devices | rg Booted` before launching.
- For consistent screenshot time:

```sh
xcrun simctl status_bar booted override --time 18:15
```

- Clear the override after screenshot work:

```sh
xcrun simctl status_bar booted clear
```

## Screen workflow

1. Agree on one screen or tightly related flow.
2. Create or update its concise brief under `docs/screens/`.
3. Build it with native Apple components and only the state it actually needs.
4. Use static checks during small iterations and build once at the end of a
   meaningful batch.
5. Open Device Hub only when the user requests inspection or approves it after
   Codex explains why it is needed.
6. Iterate until the user accepts the screen, then move to the next batch.

## Tests

The unit suite covers fixture integrity and performance, chat operations,
profiles, relays, Developer Tools, support-chat behavior, sign-out flows, and
avatar handling. The UI suite exercises representative authentication,
profile-exit, chat, composer, message-action, relay, pin-reordering, launch,
and navigation-performance paths.

```sh
DEVELOPER_DIR=/Applications/Xcode-beta.app/Contents/Developer \
xcodebuild \
  -project WhiteNoisePrototype.xcodeproj \
  -scheme WhiteNoisePrototype \
  -destination 'platform=iOS Simulator,name=iPhone Air,OS=27.0' \
  -parallel-testing-enabled NO \
  test
```

Tests are added only for meaningful nonvisual behavior, confirmed regressions,
or durable performance protection. The one-time full-project hardening record
is in [docs/project-hardening.md](docs/project-hardening.md). Claude and Fable
are never invoked automatically.
