# Access and recovery — production Android batch B01

Status: implemented and host-verified 2026-09-04; device and visual acceptance pending.

The user authorized B01–B32 as one implementation goal on 2026-09-04, with a
separate commit after each completed batch. B01 covers C001, C003, C004, C005
and C011. Existing onboarding presentation remains governed by
`onboarding-and-profiles.md`; this brief supersedes its prefix-only key
validation and Welcome-only-two-actions criteria where retained profiles exist.

## Product behavior

- Normalize surrounding whitespace and a `nostr:` prefix consistently for
  typed, pasted and scanned values. Recognize the 63-character lowercase
  nsec/npub shape and bounded encrypted-key shape without decoding or crypto.
  Public and encrypted keys have distinct helpful errors and cannot submit.
  Existing fixed Paste data remains fictional and masked.
- Private-key sign-in, profile creation, Amber sign-in and retained-profile
  re-entry use a request generation, originating profile and explicit phase.
  Duplicate actions and stale completions cannot activate a profile.
- Amber has identity-request and proof-approval progress, unavailable,
  cancelled, rejected and mismatched-identity outcomes. These are app-owned
  deterministic states; no external signer is launched or imitated.
- Setup retry and publication retry are explicit actions, never loops.
  Uncertain previous publication asks for consent before recovery. Declining
  leaves the profile untouched; partial or unexpected recovery must never
  claim that no changes occurred. Retry after consent does not ask again.
- Non-wiping sign-out keeps the same profile, history, drafts and preferences.
  Welcome offers **Continue as [name]** and a retained-profile picker. Re-entry
  uses the stored signing mode and cannot silently turn an Amber profile into
  a local-key profile. Wiped and currently signed-in profiles are ineligible.
- Startup has loading, failure, Retry and retained-profile recovery states.
  Failure preserves all in-memory profiles. No database or runtime is started.

## Composition, copy and Back

Keep the existing Welcome mark, task buttons, tonal credential field, QR sheet,
form bounds and system-owned surfaces. Add Amber as a secondary sign-in action.
Use shared Material dialogs for recovery consent, a shared sheet for retained
profiles, and ordinary inline text plus Retry/Cancel for failures. All added
copy lives in Android resources; errors name the attempted task. Progress uses
**Signing In…**, **Creating Profile…**, **Waiting for Amber…**,
**Confirming with Amber…**, **Recovering profile…**, and **Starting White Noise…**.

Recovery consent says: **An earlier setup may have published connection
information that cannot be removed. Recovering creates new connection
information and may leave the earlier copy behind. Cancel to leave this
profile unchanged.** Actions are **Recover** and **Cancel**. Partial recovery
says **Recovery did not finish. Some setup changes may already have been
applied. Retry to continue.**

Back cancels the current task or dismisses its modal before leaving the form.
Leaving access destinations invalidates pending callbacks. Rotation retains
the in-memory request but never persists raw credentials; process death starts
fresh under the established prototype boundary. Profile switching, sign-out,
removal and erase invalidate outstanding access ownership.

Developer Tools contains a clearly developer-only access-scenario chooser.
Its one-shot scenarios can be selected before a normal Add Profile or retained
entry; startup scenarios temporarily show the same app-owned startup surface.
Scenario labels do not appear in consumer flows.

## Acceptance and validation

- Every B01 outcome is reachable through the normal entry plus deterministic
  developer selection and has progress, cancellation and recovery behavior.
- Only successful activation changes signed-in membership; retained re-entry
  preserves data and never duplicates a profile.
- Tests cover shape validation/normalization, recovery consent and retry,
  duplicate/stale completion, profile ownership, wipe invalidation, signer
  ownership, retained data, startup failure and retry.
- Compose tests cover actionable failures, consent cancellation, retained
  selection, Amber phases and disabled duplicate actions. Host compilation of
  those tests is separate from device execution and user visual acceptance.
- Use the complete README static batch gate before the B01 commit.

## Evidence and Android guidance

Production is pinned to `319454889f1c2494dec4a69b5577d98017f44eee`.
`IdentityEntryInput`, `importIdentity`, `recoverIncompleteIdentitySetup`,
`loginWithAmber`, `reactivateRetainedAccount` and `AppPhase` are the future
production integration seams linked from the audit matrix. The current
prototype baseline `747b087` matches every recorded audit file hash.

Official guidance checked 2026-09-04:

- [State ownership](https://developer.android.com/develop/ui/compose/state-hoisting)
- [Material dialogs](https://developer.android.com/develop/ui/compose/components/dialog)
- [Android navigation](https://developer.android.com/guide/navigation)

Use the existing native navigation/Material patterns, 4/8 dp relationship
tokens, 520 dp form bound, wrapping text, semantic errors, live status and
48 dp minimum targets. Preserve keyboard/IME, RTL, large type and adaptive
behavior. No dependency, permission or production integration is added.

## Implementation evidence

`AccessModels.kt` and `AppViewModel` own typed phases, one-shot scenarios,
monotonic request IDs, origin checks, explicit recovery consent, signer mode
and retained-profile eligibility. `AccessUi.kt`, the onboarding screens and
`WhiteNoiseNavHost` render and route those states. Phase delays run only while
the entry is resumed; a returned request must still match its ID and phase.
Private input is cleared on departure and never enters the state holder.
`WhiteNoiseApp` owns startup loading/retry; Developer Tools supplies scenarios.

B01 also installs the minimum C007 safeguard required by its new Amber
profiles: Profile Keys shows the signer-owned explanation and exposes no local
secret or export. B02 still owns the fuller export/privacy lifecycle work.

The complete clean README gate passed: 202 unit tests, zero failures, zero lint
errors, both APKs and six new compiled `AccessFlowTest` cases. Fourteen
`AccessStateTest` cases and the expanded five-case `OnboardingValidationTest`
cover the new transitions and key/link classification. Fourteen lint warnings
remain in existing resources/code and dependency checks; none points to B01
code. No instrumentation, device or visual inspection was performed.
