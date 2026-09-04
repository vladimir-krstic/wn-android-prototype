# Help, About and open source licenses

## Purpose

Give people one predictable place to report a problem, identify the installed
build, inspect bundled open source notices and read the White Noise privacy
policy.

## Scope and non-goals

B31 covers C117: a Help destination, a review-first bug-report handoff, an
About & licenses destination, version/build facts, generated dependency
notices, privacy-policy handoff and recoverable external-launch failures.

The prototype does not collect or upload diagnostics, prefill a GitHub report,
attach app data, authenticate with GitHub, fetch a privacy document itself or
add an in-app browser. Existing **Chat with support** remains a separate
conversation entry in Settings.

## Production evidence

The pinned production baseline is
[`319454889f1c2494dec4a69b5577d98017f44eee`](https://github.com/marmot-protocol/whitenoise-android/commit/319454889f1c2494dec4a69b5577d98017f44eee).

- [`HelpAboutScreens.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/ui/settings/HelpAboutScreens.kt)
  defines the Help, bug-report and About hierarchy.
- [`WhiteNoiseUrls.kt`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/app/src/main/java/dev/ipf/whitenoise/android/core/WhiteNoiseUrls.kt)
  owns the reviewed issue and privacy destinations.
- [`bug.yml`](https://github.com/marmot-protocol/whitenoise-android/blob/319454889f1c2494dec4a69b5577d98017f44eee/.github/ISSUE_TEMPLATE/bug.yml)
  is the public repository's structured issue form.

## Parity contract

- Settings exposes **Help** alongside the existing support conversation.
- Help offers separate **Report a bug** and **About & licenses** rows.
- Report a bug first explains the public GitHub destination and that White
  Noise attaches no messages, media, contacts, profile details, keys,
  diagnostic logs or audit logs.
- **Open GitHub** launches only the allowlisted HTTPS issue path in the system
  browser. No app-owned data is placed in the intent.
- About reads the actual application version name and build number from
  `BuildConfig` rather than fixture copy.
- **Open source licenses** opens Google's generated notice surface. Release
  builds generate the dependency names and license text from the resolved
  artifacts.
- **Privacy policy** launches only the allowlisted White Noise HTTPS privacy
  path in the system browser.
- Missing handlers and launch failures retain the current screen and offer
  **Retry** and **Cancel**.

## Entry, navigation, Back, and exit

Settings → **Help** opens the Help route. Help → **Report a bug** and Help →
**About & licenses** push typed Navigation Compose destinations. App-bar Back,
system Back and predictive Back return from either detail to Help, then to
Settings. External browser and license surfaces use Android activity handoff;
returning restores the owning destination.

## Exact product copy

Ordinary copy is resource-backed: **Help**, **Report a bug**, **Review what is
shared before opening GitHub**, **About & licenses**, **Version, open source
licenses and privacy policy**, **Report form**, **GitHub bug report**, **The
public White Noise Android issue form opens in your browser.**, **Before you
continue**, **Nothing is attached**, **White Noise does not attach messages,
media, contacts, profile details, keys, diagnostic logs or audit logs.**,
**Review anything you add to the form. GitHub issues are public.**, **Open
GitHub**, **App**, **Version**, **Build**, **Legal**, **Open source licenses**,
**Software notices included with White Noise**, **Privacy policy**, **How White
Noise handles your data**, **Retry** and **Cancel**.

Unavailable states use **Couldn’t open GitHub**, **Couldn’t open licenses** or
**Couldn’t open the privacy policy**. GitHub explains that no browser is
available; the About actions explain that the destination is unavailable and
offer retry or return.

## Android composition

The flow reuses the established Settings scaffold, grouped rows, semantic
sections, callouts and bottom action. Material rows keep the information
hierarchy compact while giving each destination its own 48 dp or larger action
target. The bug handoff uses a full destination so the privacy facts remain
visible before the external action.

The browser handoff uses `ACTION_VIEW` and a manifest `queries` declaration for
HTTPS capability checks. Open source notices use the first-party Google OSS
Licenses plugin and activity. The plugin is current at 0.13.0. SDK 17.3.0 is
the latest official line compatible with the established API 23 minimum;
17.4.0 and newer require API 24, so this batch does not raise the app minimum.

## Behavior and state

`HelpAboutPolicy` owns two immutable destinations and validates HTTPS scheme,
exact host, absent user info/port/fragment and the allowed path boundary before
an intent can be created. The bug-report exclusion set enumerates every
sensitive app-owned category and is covered by unit tests.

External and license activities report a Boolean launch result. Failure is
screen-local saveable state. Retry repeats only the same reviewed action;
Cancel clears the dialog. The flow has no profile mutation, background work,
network client, attachment builder or persistent state.

## System integrations

- Android browser through `ACTION_VIEW` for GitHub and the privacy policy.
- Google Play services OSS Licenses activity for generated dependency notices.
- No added runtime permission. The merged manifest continues to remove
  `INTERNET` and `ACCESS_NETWORK_STATE`; the selected browser owns network use.

## Accessibility and adaptation

- Every row and button has a visible name; decorative leading icons have no
  duplicate content description.
- Privacy facts and unavailable states use text rather than color alone.
- Settings lists scroll, bottom actions respect safe/IME insets and actions
  remain reachable at 200% font scale.
- Material controls retain TalkBack, Switch Access, keyboard, focus, ripple and
  minimum-target behavior.
- Logical source order and start/end alignment remain compatible with RTL and
  localization.

## Governing Android sources

- [Common intents](https://developer.android.com/guide/components/intents-common)
  governs browser handoff through `ACTION_VIEW`.
- [Navigation](https://developer.android.com/guide/navigation/)
  governs typed destinations and system/predictive Back ownership.
- [Google OSS Licenses](https://developers.google.com/android/guides/opensource)
  governs generated dependency notices and the license activity.
- [Google SDK release notes](https://developers.google.com/android/guides/releases)
  establish the API-level compatibility boundary for the chosen license SDK.

## iOS parity evidence

- Existing Android prototype Settings and support behavior remain the
  presentation baseline; this capability comes from the pinned production
  Android source above.
- `docs/screens/settings-and-profile-services.md`
- `app/src/main/java/dev/ipf/whitenoise/ui/settings/SupportScreen.kt`

## Approved differences and custom exceptions

The prototype keeps its API 23 baseline and therefore uses OSS Licenses SDK
17.3.0 with plugin 0.13.0. This is the latest officially documented SDK line
compatible with that baseline. External pages stay in the system browser, and
the bug report sends no diagnostic payload.

## Observable acceptance criteria

- Settings opens Help, and Back returns through the exact route stack.
- Report a bug names every excluded category before any external handoff.
- The GitHub action creates an `ACTION_VIEW` intent only for the reviewed
  `github.com/marmot-protocol/whitenoise-android/issues/new` path.
- About displays the packaged version and build values.
- A release build contains generated dependency metadata and license text and
  opens it through **Open source licenses**.
- Privacy policy uses only `https://www.whitenoise.chat/privacy`.
- Missing activity handlers show recoverable Retry/Cancel dialogs.
- Existing Settings destinations and **Chat with support** remain available.

## Implementation evidence

B31 implementation uses `HelpAbout.kt`, `HelpAboutScreens.kt`, three typed
routes, the existing Settings hierarchy and BuildConfig metadata. The Google
OSS Licenses plugin generates 239 release dependency metadata rows and their
license text. `HelpAboutPolicyTest` adds three destination/exclusion tests.
`HelpAboutInteractionTest` adds six compiled navigation, privacy, legal,
failure and large-type cases.

The complete host gate `./gradlew testDebugUnitTest lintDebug assembleDebug
assembleDebugAndroidTest` passes 829 unit tests with zero
failures/errors/skips, zero lint errors, 15 warnings and two hints, compiles all
six new Compose cases and produces both debug APKs. A separate
`./gradlew assembleRelease` succeeds and generates a release APK plus 239
non-placeholder dependency metadata rows and license text. Device execution,
browser and license-surface inspection, screenshots and visual acceptance were
not requested and are not claimed.
