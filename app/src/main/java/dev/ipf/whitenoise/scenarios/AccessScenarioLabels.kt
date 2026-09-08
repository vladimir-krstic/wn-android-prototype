package dev.ipf.whitenoise.scenarios

import dev.ipf.whitenoise.model.AccessScenario
import dev.ipf.whitenoise.model.ProfileExitScenario

internal val AccessScenario.developerLabel: String
    get() = when (this) {
        AccessScenario.Success -> "Success"
        AccessScenario.Offline -> "No connection"
        AccessScenario.SignInFailure -> "Sign-in / creation failure"
        AccessScenario.SetupRetry -> "Setup needs retry"
        AccessScenario.PublicationRetry -> "Connection publication needs retry"
        AccessScenario.RecoveryConsent -> "Recovery consent, then success"
        AccessScenario.RecoveryPartial -> "Recovery consent, then partial result"
        AccessScenario.UnexpectedSetup -> "Unexpected setup state"
        AccessScenario.RecoveryUnexpected -> "Unexpected state during recovery"
        AccessScenario.AmberUnavailable -> "Amber unavailable"
        AccessScenario.AmberIdentityCancelled -> "Amber identity request cancelled"
        AccessScenario.AmberIdentityRejected -> "Amber identity request rejected"
        AccessScenario.AmberProofCancelled -> "Amber proof cancelled"
        AccessScenario.AmberProofRejected -> "Amber proof rejected"
        AccessScenario.AmberMismatch -> "Amber identity mismatch"
        AccessScenario.AmberTimeout -> "Amber timeout"
        AccessScenario.AmberInvalidResponse -> "Amber invalid response"
    }

internal val ProfileExitScenario.developerLabel: String
    get() = when (this) {
        ProfileExitScenario.Success -> "All steps complete"
        ProfileExitScenario.GroupLeaveFailure -> "Group departure incomplete"
        ProfileExitScenario.RelayCleanupFailure -> "Relay cleanup incomplete"
        ProfileExitScenario.LocalCleanupFailure -> "Local cleanup incomplete"
        ProfileExitScenario.AllCleanupFailure -> "All cleanup incomplete"
    }
