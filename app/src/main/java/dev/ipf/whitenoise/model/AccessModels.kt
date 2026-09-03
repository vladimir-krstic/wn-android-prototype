package dev.ipf.whitenoise.model

import dev.ipf.whitenoise.navigation.OnboardingOrigin

enum class ProfileSigningMode { LocalKey, Amber }

enum class AccessMethod { PrivateKey, CreateProfile, Amber, Retained }

enum class AccessPhase {
    SigningIn, CreatingProfile, AmberIdentity, AmberProof, Recovering, RecoveryConsent, Failed;

    val isBusy: Boolean
        get() = this !in setOf(RecoveryConsent, Failed)
}

enum class AccessFailure {
    Offline, SignIn, CreateProfile, SetupRetry, PublicationRetry, UnexpectedSetup,
    RecoveryPartial, RecoveryUnexpected, AmberUnavailable, AmberCancelled,
    AmberRejected, AmberMismatch,
}

/** One-shot, developer-only inputs. They describe outcomes, never contain credentials. */
enum class AccessScenario {
    Success, Offline, SignInFailure, SetupRetry, PublicationRetry, RecoveryConsent,
    RecoveryPartial, UnexpectedSetup, RecoveryUnexpected, AmberUnavailable,
    AmberIdentityCancelled, AmberIdentityRejected, AmberProofCancelled, AmberProofRejected,
    AmberMismatch,
}

data class AccessAttempt(
    val id: Long,
    val origin: OnboardingOrigin,
    val ownerProfileId: String?,
    val candidate: Profile,
    val method: AccessMethod,
    val phase: AccessPhase,
    val scenario: AccessScenario,
    val failure: AccessFailure? = null,
    val recoveryAcknowledged: Boolean = false,
) {
    val usesAmber: Boolean
        get() = candidate.signingMode == ProfileSigningMode.Amber

    /** null means successful completion; no automatic retries or inferred recovery consent. */
    fun advance(): AccessAttempt? {
        if (!phase.isBusy) return this
        if (phase == AccessPhase.Recovering) {
            return when (scenario) {
                AccessScenario.RecoveryPartial -> failed(AccessFailure.RecoveryPartial)
                AccessScenario.RecoveryUnexpected -> failed(AccessFailure.RecoveryUnexpected)
                else -> null
            }
        }
        if (scenario == AccessScenario.Offline) return failed(AccessFailure.Offline)
        if (usesAmber) {
            if (scenario == AccessScenario.AmberUnavailable) return failed(AccessFailure.AmberUnavailable)
            if (phase == AccessPhase.AmberIdentity) {
                return when (scenario) {
                    AccessScenario.AmberIdentityCancelled -> failed(AccessFailure.AmberCancelled)
                    AccessScenario.AmberIdentityRejected -> failed(AccessFailure.AmberRejected)
                    AccessScenario.AmberMismatch -> failed(AccessFailure.AmberMismatch)
                    else -> copy(phase = AccessPhase.AmberProof)
                }
            }
            return when (scenario) {
                AccessScenario.AmberProofCancelled -> failed(AccessFailure.AmberCancelled)
                AccessScenario.AmberProofRejected -> failed(AccessFailure.AmberRejected)
                AccessScenario.SignInFailure -> failed(AccessFailure.SignIn)
                else -> null
            }
        }
        if (method == AccessMethod.CreateProfile) {
            return if (scenario == AccessScenario.Success) null else failed(AccessFailure.CreateProfile)
        }
        return when (scenario) {
            AccessScenario.SignInFailure -> failed(AccessFailure.SignIn)
            AccessScenario.SetupRetry -> failed(AccessFailure.SetupRetry)
            AccessScenario.PublicationRetry -> failed(AccessFailure.PublicationRetry)
            AccessScenario.RecoveryConsent, AccessScenario.RecoveryPartial,
            AccessScenario.RecoveryUnexpected -> copy(phase = AccessPhase.RecoveryConsent)
            AccessScenario.UnexpectedSetup -> failed(AccessFailure.UnexpectedSetup)
            else -> null
        }
    }

    fun startingPhase(): AccessPhase = when {
        recoveryAcknowledged -> AccessPhase.Recovering
        usesAmber -> AccessPhase.AmberIdentity
        method == AccessMethod.CreateProfile -> AccessPhase.CreatingProfile
        else -> AccessPhase.SigningIn
    }

    private fun failed(reason: AccessFailure) = copy(phase = AccessPhase.Failed, failure = reason)
}

enum class StartupPhase { Loading, Failed, Ready }

data class StartupState(
    val generation: Long = 0,
    val phase: StartupPhase = StartupPhase.Loading,
    val fails: Boolean = false,
)
