# Profile setup after Sign In

## Purpose and scope

Approved 2026-09-08: Private Key Sign In from initial Welcome or Add Profile
checks readiness before activation. Sign Up, Amber and retained-profile entry
keep their established behavior. Everything remains deterministic and in memory.

## Parity and presentation

Use PR #939 at merge `094f61d5d7f135a4057996b519d91b455637ba30` for its six
checks, device acknowledgment and recovery distinctions; issue #940 supersedes
its resumable setup and presentation. Android uses full-screen Navigation
Compose destinations for the checklist, decisions and reused profile editor.
No Later, unfinished-setup picker, persisted checkpoint or automatic activation.

Keep Profile, Follows, Relays, Message inbox, Using one device and Secure
messaging rows stable. One check runs at a time. Done uses a green semantic
success icon; attention uses orange; Waiting and Skipped use distinct neutral
symbols and text. Entire attention rows are accessible actions. Detail pages
contain explanations and bottom actions; nothing expands between list rows.
Reuse onboarding fields, app bars, 16 dp margins, 520 dp content bounds and
56 dp primary buttons. Save remains above the IME. Status colors have light
and dark variants while the rest of the surface keeps the existing theme.

## State, Back and cancellation

An attempt and each operation have an identity. Late, duplicate and foreign
callbacks cannot change state or activate a profile. Required relay routes,
device acknowledgment and secure-messaging preparation gate Open Chats.
Optional profile work and follows may be skipped. One failed endpoint does
not fail a role served by another usable endpoint.

Back from detail returns to the same checklist position without applying an
unconfirmed choice. Back from setup cancels to the originating Welcome; the
previous active profile is preserved for Add Profile. Rotation retains the
active attempt and drafts. Process death starts fresh. Cancellation preserves
already-applied profile/relay values in process, but discards the checklist and
unconfirmed draft. Open Chats alone activates the ready candidate.

## Recovery and copy

Profile lookup failure says “Couldn’t load your profile”; retry and Not Now
are available. Optional editing reuses Sign Up in editing mode and retains a
failed-save draft. Missing follows are skipped without replacing a list.
Relay recovery distinguishes missing settings from inconclusive discovery;
defaults require an explicit choice and cannot follow an inconclusive result.
Use the existing Android default relay set and show exact addresses once below
“Relays let your profile publish information, receive chat invitations, and
deliver messages.” The action is “Use Default Relays”. Searching another relay
does not itself publish settings. Device discovery distinguishes none found,
possible installation and unknown; Continue / Continue Anyway acknowledges
the consequence and starts secure-messaging preparation. Retry does not repeat
the acknowledgment. Ready removes checking copy and retains final results.

## Developer scenarios

Developer Tools → Scenarios → Profile setup immediately launches a temporary
Private Key Sign In at the checks page. Variants include:
Ready profile, Needs attention, Recovery required, plus focused profile-save,
profile-lookup, follows, missing-relay, inconclusive-relay, unknown-device,
partial-endpoint and messaging-failure cases. Sign Up/Amber do not consume it.
Defaults return to Ready profile after consumption. No profile-count sequence.

## Accessibility and validation

Use Material semantics, text and symbols in addition to color, logical focus,
48 dp targets, wrapping content, RTL, large fonts, safe drawing and IME insets.
Host tests cover required/optional gating, fresh attempts, every scenario,
draft retention, relay approval, applied-change preservation, ownership and
stale work. Compose tests cover navigation, actionable rows and recovery.
Run the README host gate; device execution and visual acceptance are separate.

## Sources

- https://github.com/marmot-protocol/whitenoise-ios/pull/939
- https://github.com/marmot-protocol/whitenoise-ios/issues/940
- https://developer.android.com/guide/navigation
- https://developer.android.com/develop/ui/compose/system/insets-ui
- https://developer.android.com/develop/ui/compose/accessibility/semantics

## Implementation evidence

Implemented in `model/ProfileSetup.kt`, `state/ProfileSetupController.kt`,
`ProfileSetupScreens.kt`, `WhiteNoiseStatusColors.kt`, access state/navigation,
Sign Up’s editing mode, and the Developer Tools chooser. Thirteen host tests in
`ProfileSetupTest` cover readiness, all eleven scenarios, retry, optional skip,
profile ownership, fresh checks, retained applied values, wipe cleanup, safe
relay input and explicit default approval. Legacy access tests explicitly
complete the new ready-setup gate. Seven `ProfileSetupFlowTest` cases compile
production navigation, system Back, draft retry, relay input, Add Profile
cancellation, scenario confirmation and a restored route without an attempt.

The 2026-09-08 README host gate passes: 1,082 unit tests, zero failures/errors/
skips; lint has zero errors and 16 warnings; app and instrumentation APKs
assemble. Six Python verifier tests and all 1,877 translatable resources across
four locale catalogs pass. `git diff --check` is clean. No instrumentation,
device launch, screenshot or visual acceptance was performed.

Use Scenarios → Onboarding entry points to inspect initial, added-account and
retained-account access. Profile setup variants start directly at their checks.
All scenario explanations and controls stay developer-only.

## System integrations and exact copy

No new permission, dependency or system surface is introduced. Editing reuses
the established avatar acquisition flows without modifying Android pickers.
English and translated product strings are the `setup_*` resources in the five
`strings.xml` catalogs. The screen title is **Profile Setup**; statuses are
**Waiting**, **Checking…**, **Done**, **Skipped**, **Needs your attention**.
Actions are **Edit Profile**, **Not Now**, **Use Default Relays**, **Find My
Settings**, **Try Again**, **Continue**, **Continue Anyway**, **Save**, and
**Open Chats**. Only actual profile saving uses **Saving profile…**.

## Observable acceptance criteria

- No candidate is activated before required checks and explicit Open Chats.
- An attention row opens a full-screen task; Back returns to its unchanged
  checklist position, and root Back cancels with no unfinished-setup entry.
- Optional skip, failed save/retry, relay discovery/default consent and device
  acknowledgment are repeatable through the documented developer scenarios.
- A cancelled or foreign callback cannot apply work or activate a profile;
  cancellation preserves already-applied values and unrelated profile data.
- Light/dark contrast, TalkBack status/action labels, large text, RTL, IME Save
  visibility and predictive Back await authorized hands-on inspection.

### Sign In transition regression — 2026-09-08

The host clears Private Key text while the outgoing Sign In destination is
still composed during navigation. Its old key-change callback cancelled the
new setup attempt, sending the person back to Welcome. Sign In now keeps
controls in their loading state through the setup handoff and dispatches key
edits separately from Cancel. `AppViewModel.signInKeyEdited` may clear only
the matching failed sign-in; busy, newer and setup attempts are preserved.

Two host regressions cover both onboarding origins and stale/failed edit
callbacks. All seven compiled `ProfileSetupFlowTest` cases now enter using the
actual Welcome → Paste private key → Sign In controls, including credential
clearing during the outgoing transition. This replaces direct ViewModel entry
that bypassed the failing path. Validation is host-only; device execution and
visual acceptance remain pending.

Focused verification passed: 34 tests in `ProfileSetupTest` and `AccessStateTest`,
zero failures/errors/skips; `lintDebug` has zero errors; `assembleDebug` and
`assembleDebugAndroidTest` pass. `git diff --check` is clean. Instrumentation
was compiled, not executed.

### App-wide scenario catalog — 2026-09-08

The earlier next-attempt chooser is superseded by
[the app-wide catalog](scenario-catalog.md). Developer Tools → Scenarios →
Profile setup lists all eleven described variants. Tapping one starts its checks
immediately in a temporary session, even when Developer Tools is disabled in the
original account. Restart resets the run; Exit Scenario returns to the launching selection page
and restores the original app data. The strip's info icon opens its title and
instructions in a tooltip. Earlier instructions to select
an outcome and then roam to Add Profile are obsolete.

Latest catalog validation: 74 scenarios / 372 variants; 1,089 host unit tests pass,
lint has 0 errors and 16 warnings, both APKs assemble. Eight Python checks and
1,805 translated resource keys across four locales pass. No device execution.
