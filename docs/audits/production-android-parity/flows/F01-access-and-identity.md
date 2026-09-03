# F01 — Access, identity and recovery

## Purpose and current composition

Enter at Welcome or Settings profile management. Back cancels only the current import/signer task; successful activation clears onboarding. Model key validation, signer presence, network, incomplete setup and retained identities independently.

Prototype surface: `ui/onboarding/; ui/settings/DestructiveScreens.kt; state/AppViewModel.kt`. Reuse `WhiteNoiseScaffold`, tonal Settings groups/rows, shared sheets/dialogs, `WhiteNoiseTextField`, `WhiteNoiseButton`, adaptive content bounds, message action/reaction components, MediaViewer and established empty/loading/error content as applicable. Keep 48 dp minimum targets, label-above forms, semantic error colors, bounded state layers, RTL, 200% text, IME/inset ownership and compact/expanded behavior.

## Required content and proposed copy

Primary actions: **Sign In**, **Sign Up**, **Continue as [name]**, **Sign in with Amber**, **Recover**, **Retry**, **Cancel**. Name the current task in progress. For uncertain setup, state what may have been published and that cancelling changes nothing.

These labels are the audit recommendation and follow current prototype terminology. Validate exact surrounding help/error copy in the selected screen brief against each matrix source link; preserve production security and destructive consequences without exposing implementation terms.

## Capability and state contract

| Capability | Initial state | Event / Back behavior | Observable result |
| --- | --- | --- | --- |
| C001 · Welcome, new profile, and private-key sign-in | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Add rejected public/encrypted key, normalized scan/paste, unavailable network and action-specific setup failures. Prototype currently accepts every nsec prefix; keep deterministic keys, not real login. |
| C002 · Profile creation form | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C003 · Sign in with Amber / external signer | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Add installed/unavailable, identity approval, proof approval, cancellation, rejection and signer-owned identity states. No real signer calls in this prototype batch. |
| C004 · Incomplete identity setup retry and recovery consent | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Distinguish safe resume, uncertain prior key-package publication, explicit recovery consent, declined, partial and unexpected-state outcomes. |
| C005 · Retained-profile reactivation from Welcome | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Add Continue as and retained-profile picker after non-wiping sign-out, restoring the same profile-owned history and drafts. |
| C006 · Switch profiles and isolated unread state | Existing equivalent | Trigger its named entry/action; cancel with Back where available | Preserve behavior |
| C007 · Signer ownership and private-key export availability | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Local/Amber ownership and temporarily unavailable local-key access now have explicit reasons and retry. Secret reveal/copy/export requires an available local key. |
| C008 · Raw and encrypted private-key backup | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Raw/encrypted fixture output uses a process-local profile-owned request, 30-second expiry and stale-result rejection. Reveal/passwords clear on background and are excluded from saved state. No real cryptography. |
| C009 · Sign out with relay cleanup outcomes | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Non-wipe sign-out offers connection-information cleanup. Separate remote/local results distinguish partial success from local failure, preserve completed cleanup and retry only unfinished work. Approved wipe default retained; Q05 resolved. |
| C010 · Account wipe progress and partial outcome report | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Group departure, relay cleanup and local wipe have ordered independent outcomes. Exact-name confirmation and owner guards remain; local failure stays active and remote partial results survive routing after exit. |
| C011 · Bootstrap failure and retry | Implemented; host verified | Trigger its named entry/action; cancel with Back where available | Add deterministic startup loading, failure, retry and retained-account recovery states without initializing a runtime. |

## Production integration seam

Production evidence for each row is linked in the [matrix](../capability-matrix.md). During prototype work, add the smallest profile-owned immutable fixture/state transitions and callbacks needed to render every named result. Do not add Marmot, networking, signing, persistence, notification delivery, background services or cryptography. Name production events and ownership in the selected screen brief so the eventual migration reconnects to the cited controller/state methods rather than copying prototype fixtures into production storage.

## Copy, accessibility and adaptation

Use the approved product language and terminology. Production strings in the matrix are evidence of meaning, not automatic final copy. Keep raw keys, event IDs, MLS and engine errors off ordinary surfaces; developer surfaces may be exact. State must be conveyed by text/semantics as well as icon/color. Provide accessible equivalents for gestures, logical focus and Back order, and preserve action eligibility at large type and narrow height.

## Acceptance and host validation

- Every linked capability has a deterministic route/fixture and every mutation yields the specified success, cancellation, unavailable and failure outcomes relevant to it.
- Back, profile switching and restored state cannot commit work to the wrong profile/chat or repeat a completed mutation.
- Existing capabilities in this flow retain their current model and UI tests.
- Add unit tests for rules/ownership and Compose tests for durable navigation/actions/semantics. Run targeted host tests while iterating and the repository static gate after a meaningful batch. Compile instrumentation tests only; device execution and visual acceptance require a separate current request.

## Dependencies and decisions

Batches: B01, B02. Q05 resolved: preserve the approved checked wipe default. Facts are the matrix's cited production behavior and current prototype evidence. UI placement and proposed copy remain recommendations until the selected screen brief records them.

## B01 implementation evidence

C001, C003, C004, C005 and C011 are implemented and host-verified on 2026-09-04. See [the selected brief](../../../screens/access-and-recovery.md#implementation-evidence) for files, 202 passing unit tests, both APKs and six new compiled UI cases. B02 extends the signer-owned export guard with the complete lifecycle and outcome paths below. Device and visual acceptance remain pending.

## B02 implementation evidence

C007–C010 are implemented and host-verified 2026-09-04. See [the selected brief](../../../screens/keys-and-profile-exit.md#implementation-evidence) for key ownership/expiry, optional relay cleanup, staged wipe, retry and partial outcomes. The clean gate passed 216 unit tests, zero lint errors and both APKs; five new UI cases compile. No device or visual acceptance is claimed.
