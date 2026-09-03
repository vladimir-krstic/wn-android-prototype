package dev.ipf.whitenoise.model

data class SignOutOptions(
    val wipeData: Boolean,
    val deleteConnectionInformation: Boolean = true,
    val confirmation: String = "",
)

enum class ProfileExitStep { LeaveGroups, RelayCleanup, LocalCleanup }
enum class ProfileExitStepResult { Pending, Done, Incomplete, NotRequested }
enum class ProfileExitScenario { Success, GroupLeaveFailure, RelayCleanupFailure, LocalCleanupFailure, AllCleanupFailure }

data class ProfileExitAttempt(
    val id: Long,
    val profileId: String,
    val profileName: String,
    val options: SignOutOptions,
    val scenario: ProfileExitScenario,
    val results: Map<ProfileExitStep, ProfileExitStepResult> = ProfileExitStep.entries.associateWith {
        when {
            it == ProfileExitStep.LeaveGroups && !options.wipeData -> ProfileExitStepResult.NotRequested
            it == ProfileExitStep.RelayCleanup && !options.wipeData && !options.deleteConnectionInformation -> ProfileExitStepResult.NotRequested
            else -> ProfileExitStepResult.Pending
        }
    },
) {
    val currentStep: ProfileExitStep?
        get() = ProfileExitStep.entries.firstOrNull { results[it] == ProfileExitStepResult.Pending }
    val isRunning: Boolean get() = currentStep != null
    val localCleanupCompleted: Boolean get() = results[ProfileExitStep.LocalCleanup] == ProfileExitStepResult.Done
    val hasIncompleteWork: Boolean get() = results.values.any { it == ProfileExitStepResult.Incomplete }

    fun advance(): ProfileExitAttempt {
        val step = currentStep ?: return this
        val failed = scenario == ProfileExitScenario.AllCleanupFailure || when (step) {
            ProfileExitStep.LeaveGroups -> scenario == ProfileExitScenario.GroupLeaveFailure
            ProfileExitStep.RelayCleanup -> scenario == ProfileExitScenario.RelayCleanupFailure
            ProfileExitStep.LocalCleanup -> scenario == ProfileExitScenario.LocalCleanupFailure
        }
        return copy(results = results + (step to if (failed) ProfileExitStepResult.Incomplete else ProfileExitStepResult.Done))
    }

    fun retry(newId: Long): ProfileExitAttempt = copy(
        id = newId,
        scenario = ProfileExitScenario.Success,
        results = results.mapValues { (_, result) ->
            if (result == ProfileExitStepResult.Incomplete) ProfileExitStepResult.Pending else result
        },
    )
}

enum class ProfileKeyExportKind { Raw, Encrypted }

/** A process-local request; never put its payload in saved state, logs or diagnostics. */
class ProfileKeyExportRequest(
    val profileId: String,
    val kind: ProfileKeyExportKind,
    val createdAtMillis: Long,
    val content: String,
)

object ProfileKeyAccessPolicy {
    const val EXPIRY_MILLIS = 30_000L

    fun canRead(profile: Profile): Boolean = profile.signingMode == ProfileSigningMode.LocalKey && profile.localKeyAvailable

    fun canComplete(request: ProfileKeyExportRequest?, profile: Profile, nowMillis: Long): Boolean =
        request != null && request.profileId == profile.id && canRead(profile) &&
            nowMillis >= request.createdAtMillis && nowMillis - request.createdAtMillis < EXPIRY_MILLIS
}
