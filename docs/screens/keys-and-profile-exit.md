# Keys and profile exit — production Android batch B02

Status: implemented and host-verified 2026-09-04; device and visual acceptance pending.

The user selected all production-parity batches, with one verified commit per
batch. B02 covers C007–C010 and extends the existing Profile Keys and Sign Out
surfaces. Q05 is resolved by the existing explicit prototype direction:
**Wipe Data From This Device** stays selected by default and requires the exact
profile name. The extra relay-cleanup choice is selected by default when
retaining data. Wipe always includes attempted group departure and relay
cleanup. Both exit choices remain available.

## Behavior and composition

- Profile Keys distinguishes local key ownership, temporarily unavailable
  local key access and Amber ownership. Public-key copy remains available.
  Secret reveal/copy/export requires a locally available key. Retry restores
  the deterministic local capability; no real signer, crypto or auth is added.
- Reveal is temporary (30 seconds), cleared on background or profile change,
  and excluded from saved state. Password drafts are process-local, cleared on
  dialog dismissal/background and immediately after an export request starts.
- A file export is owned by its original profile and expires after 30 seconds.
  The system document picker retains its approved platform appearance. Its
  result is accepted only for the same profile and available local key within
  the time window. Rotation or departure may discard the request; a returned
  URI then shows **Export expired** and is never written. Cancellation is quiet.
- Non-wiping Sign Out optionally removes connection information from relays.
  Wipe runs group departure, relay cleanup and local cleanup as distinct steps.
  Each has Done/Incomplete/Not requested outcomes. Successful remote work must
  remain reflected if later local cleanup fails; retries skip completed work.
- Local cleanup failure keeps the profile active and shows a retryable report.
  Successful local cleanup signs out or wipes exactly the captured profile;
  remote cleanup failure still produces an explicit partial-result report
  after routing to Welcome or the remaining-profile switcher.
- Starting, completing or retrying requires the same request ID and owner.
  Duplicate callbacks cannot affect another profile. Confirmation and options
  are frozen at start. Back/dismiss remains disabled only during active steps;
  failure reports may be closed without claiming rollback.

Keep the accepted expanded Material sheet, grouped profile/choices, outside
consequences and exact-name field. Added outcome rows use visible text and
native progress. New copy uses resources, with ordinary **connection
information** in place of developer-only KeyPackage terminology. Developer
Tools supplies one-shot sign-out outcomes and local-key availability.

## Validation and production seams

Test stage ordering, optional cleanup, local failure, partial outcomes,
successful-step preservation on retry, stale ownership, key availability,
export ownership/expiry, and retained/wiped data consequences. Compile durable
Compose action/semantics tests; run the complete README host gate before B02's
commit. No device or emulator execution is authorized.

Production `signOutActiveAccount`, `signOutAndWipeActiveAccount` and
`exportActiveAccountNsec` at the pinned audit commit are the later integration
seams. Existing screen briefs govern approved presentation and destructive
copy. This implements local deterministic state and approved document output;
the production cryptographic/network/storage boundary is unchanged.

## Implementation evidence

- `model/ProfileExitModels.kt` and `state/AppViewModel.kt` own staged results,
  frozen confirmation/options, profile identity, consumed callbacks and retry.
  Completed group/relay work survives a later local failure. Retained re-entry
  preserves chats/settings and republishes connection information.
- `ProfileSettingsScreens.kt` binds temporary key access and export to the
  original profile, foreground privacy and elapsed-time expiry. The standard
  document contract is unchanged; passwords and reveal are not saved.
- `DestructiveScreens.kt`, `ProfileExitUi.kt` and navigation preserve the
  approved sign-out sheet and add option, progress and partial-result paths.
  Sheet visibility survives recreation while the ViewModel retains the task.
- `ProfileExitStateTest` adds 10 meaningful state regressions and
  `ProfileKeyAccessPolicyTest` adds four ownership/availability/expiry checks.
  `ProfileExitFlowTest` adds five compiled interaction/semantics/restoration
  cases. These UI tests have not been executed on a device.
- The clean README host gate passed: **216 unit tests**, zero failures/errors,
  zero lint errors, both APKs and instrumentation-test compilation. The same
  14 existing lint warnings remain. `git diff --check` passed. No device,
  emulator, picker interaction or renewed visual acceptance is claimed.

Official sources checked 2026-09-04: [Compose state ownership](https://developer.android.com/develop/ui/compose/state-hoisting),
[side effects and lifecycle](https://developer.android.com/develop/ui/compose/side-effects),
[Material dialogs](https://developer.android.com/develop/ui/compose/components/dialog),
and [system document creation](https://developer.android.com/training/data-storage/shared/documents-files).
The app owns process-local requests and task outcomes; Material owns modal
interaction and Android owns the document destination surface.


## Amber ownership callout — 2026-09-05

The user-approved shared callout replaces the plain Signing section for Amber.
It reads “Signed in with Amber” and “Amber holds this profile’s Private Key.
Manage backups in Amber.” The gray Material surface uses an info icon. Public
key copy remains available; no local secret, reveal, copy or export is rendered.
Temporary local-key unavailability uses the same callout with a warning icon
and keeps Retry. `AccessFlowTest` asserts the Amber callout and absence of local
secret/export controls. Signing custody now follows the chosen successful access
method on the same canonical account; see `onboarding-and-profiles.md`.
