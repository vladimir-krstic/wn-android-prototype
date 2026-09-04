package dev.ipf.whitenoise.model

import java.math.BigInteger

enum class AppUpdateDistribution(val developerLabel: String) {
    SelfManaged("Self-managed (Zapstore)"),
    StoreManaged("Store-managed"),
}

enum class AppUpdateCheckPhase { Unknown, Checking, Current, Available, Failed }

enum class AppUpdateCheckScenario(val developerLabel: String) {
    Unknown("Not checked"),
    Current("Up to date"),
    Available("One release available"),
    ImportantAvailable("Three releases behind"),
    Failure("Check failed"),
}

enum class AppSelfUpdateScenario(val developerLabel: String) {
    Success("Verified update"),
    ResolveFailure("Release resolution fails"),
    DownloadFailure("Download fails"),
    VerificationFailure("Verification fails"),
    PermissionRequired("Install permission required"),
    InstallFailure("Installer handoff fails"),
}

enum class AppSelfUpdatePhase {
    Idle,
    Resolving,
    Confirming,
    Downloading,
    Verifying,
    Ready,
    PermissionRequired,
    Failed,
}

enum class AppSelfUpdateFailure { Resolve, Download, Verification, Install }

data class AppUpdateCheckState(
    val phase: AppUpdateCheckPhase,
    val generation: Long = 0,
    val latestVersion: String? = null,
    val releasesBehind: Int? = null,
)

data class AppSelfUpdateState(
    val phase: AppSelfUpdatePhase = AppSelfUpdatePhase.Idle,
    val generation: Long = 0,
    val version: String? = null,
    val sizeBytes: Long? = null,
    val bytesRead: Long = 0,
    val failure: AppSelfUpdateFailure? = null,
)

data class AppUpdateState(
    val installedVersion: String,
    val distribution: AppUpdateDistribution,
    val check: AppUpdateCheckState,
    val dismissedVersion: String? = null,
    val checkScenario: AppUpdateCheckScenario = AppUpdateCheckScenario.Available,
    val selfUpdateScenario: AppSelfUpdateScenario = AppSelfUpdateScenario.Success,
    val selfUpdate: AppSelfUpdateState = AppSelfUpdateState(),
)

object AppUpdates {
    const val ImportantReleaseCount = 3
    const val FixtureDownloadBytes = 58_720_256L

    fun initial(installedVersion: String): AppUpdateState = AppUpdateState(
        installedVersion = installedVersion,
        distribution = AppUpdateDistribution.SelfManaged,
        check = checkFixture(AppUpdateCheckScenario.Available, installedVersion = installedVersion),
    )

    fun showsSettings(state: AppUpdateState): Boolean =
        state.distribution == AppUpdateDistribution.SelfManaged

    fun isAvailable(state: AppUpdateState): Boolean =
        state.check.phase == AppUpdateCheckPhase.Available &&
            state.check.latestVersion?.let { compareVersions(it, state.installedVersion) > 0 } == true

    fun isImportant(state: AppUpdateState): Boolean =
        (state.check.releasesBehind ?: 0) >= ImportantReleaseCount

    fun showsBanner(state: AppUpdateState): Boolean =
        showsSettings(state) && isAvailable(state) &&
            (isImportant(state) || state.dismissedVersion != state.check.latestVersion)

    fun canDismissBanner(state: AppUpdateState): Boolean = showsBanner(state) && !isImportant(state)

    fun selectDistribution(state: AppUpdateState, distribution: AppUpdateDistribution): AppUpdateState {
        if (distribution == state.distribution) return state
        return if (distribution == AppUpdateDistribution.StoreManaged) {
            state.copy(
                distribution = distribution,
                check = AppUpdateCheckState(AppUpdateCheckPhase.Unknown, state.check.generation + 1),
                dismissedVersion = null,
                selfUpdate = AppSelfUpdateState(generation = state.selfUpdate.generation + 1),
            )
        } else {
            state.copy(
                distribution = distribution,
                check = checkFixture(state.checkScenario, state.check.generation + 1, state.installedVersion),
                dismissedVersion = null,
                selfUpdate = AppSelfUpdateState(generation = state.selfUpdate.generation + 1),
            )
        }
    }

    fun previewCheck(state: AppUpdateState, scenario: AppUpdateCheckScenario): AppUpdateState =
        state.copy(
            checkScenario = scenario,
            check = if (showsSettings(state)) {
                checkFixture(scenario, state.check.generation + 1, state.installedVersion)
            } else state.check,
            dismissedVersion = null,
            selfUpdate = AppSelfUpdateState(generation = state.selfUpdate.generation + 1),
        )

    fun beginCheck(state: AppUpdateState): AppUpdateState {
        if (!showsSettings(state) || state.check.phase == AppUpdateCheckPhase.Checking) return state
        return state.copy(
            check = AppUpdateCheckState(AppUpdateCheckPhase.Checking, state.check.generation + 1),
            selfUpdate = AppSelfUpdateState(generation = state.selfUpdate.generation + 1),
        )
    }

    fun completeCheck(state: AppUpdateState, generation: Long): AppUpdateState {
        if (!showsSettings(state) || state.check.phase != AppUpdateCheckPhase.Checking ||
            state.check.generation != generation
        ) return state
        return state.copy(check = checkFixture(state.checkScenario, generation, state.installedVersion))
    }

    fun dismissBanner(state: AppUpdateState): AppUpdateState =
        if (canDismissBanner(state)) state.copy(dismissedVersion = state.check.latestVersion) else state

    fun selectSelfUpdateScenario(state: AppUpdateState, scenario: AppSelfUpdateScenario): AppUpdateState =
        state.copy(selfUpdateScenario = scenario)

    fun beginSelfUpdate(state: AppUpdateState): AppUpdateState {
        if (!showsSettings(state) || !isAvailable(state) ||
            state.selfUpdate.phase != AppSelfUpdatePhase.Idle
        ) return state
        return state.copy(
            selfUpdate = AppSelfUpdateState(
                phase = AppSelfUpdatePhase.Resolving,
                generation = state.selfUpdate.generation + 1,
                version = state.check.latestVersion,
            ),
        )
    }

    fun confirmDownload(state: AppUpdateState): AppUpdateState {
        val flow = state.selfUpdate
        if (flow.phase != AppSelfUpdatePhase.Confirming) return state
        return state.copy(
            selfUpdate = flow.copy(
                phase = AppSelfUpdatePhase.Downloading,
                bytesRead = 0,
                failure = null,
            ),
        )
    }

    fun advanceSelfUpdate(state: AppUpdateState, generation: Long): AppUpdateState {
        val flow = state.selfUpdate
        if (flow.generation != generation) return state
        val next = when (flow.phase) {
            AppSelfUpdatePhase.Resolving -> if (state.selfUpdateScenario == AppSelfUpdateScenario.ResolveFailure) {
                flow.failed(AppSelfUpdateFailure.Resolve)
            } else {
                flow.copy(phase = AppSelfUpdatePhase.Confirming, sizeBytes = FixtureDownloadBytes)
            }
            AppSelfUpdatePhase.Downloading -> if (flow.bytesRead == 0L) {
                flow.copy(bytesRead = (flow.sizeBytes ?: FixtureDownloadBytes) * 3 / 5)
            } else if (state.selfUpdateScenario == AppSelfUpdateScenario.DownloadFailure) {
                flow.failed(AppSelfUpdateFailure.Download)
            } else {
                flow.copy(
                    phase = AppSelfUpdatePhase.Verifying,
                    bytesRead = flow.sizeBytes ?: FixtureDownloadBytes,
                )
            }
            AppSelfUpdatePhase.Verifying -> when (state.selfUpdateScenario) {
                AppSelfUpdateScenario.VerificationFailure -> flow.failed(AppSelfUpdateFailure.Verification)
                AppSelfUpdateScenario.PermissionRequired -> flow.copy(phase = AppSelfUpdatePhase.PermissionRequired)
                else -> flow.copy(phase = AppSelfUpdatePhase.Ready)
            }
            else -> flow
        }
        return if (next == flow) state else state.copy(selfUpdate = next)
    }

    fun reviewInstallPermission(state: AppUpdateState): AppUpdateState =
        if (state.selfUpdate.phase == AppSelfUpdatePhase.PermissionRequired) {
            state.copy(selfUpdate = state.selfUpdate.copy(phase = AppSelfUpdatePhase.Ready))
        } else state

    fun requestInstall(state: AppUpdateState): AppUpdateState {
        val flow = state.selfUpdate
        if (flow.phase != AppSelfUpdatePhase.Ready) return state
        return if (state.selfUpdateScenario == AppSelfUpdateScenario.InstallFailure) {
            state.copy(selfUpdate = flow.failed(AppSelfUpdateFailure.Install))
        } else {
            state.copy(selfUpdate = AppSelfUpdateState(generation = flow.generation + 1))
        }
    }

    fun retry(state: AppUpdateState): AppUpdateState {
        val flow = state.selfUpdate
        if (flow.phase != AppSelfUpdatePhase.Failed) return state
        return state.copy(
            selfUpdate = AppSelfUpdateState(
                phase = AppSelfUpdatePhase.Resolving,
                generation = flow.generation + 1,
                version = state.check.latestVersion,
            ),
        )
    }

    fun cancel(state: AppUpdateState): AppUpdateState = state.copy(
        selfUpdate = AppSelfUpdateState(generation = state.selfUpdate.generation + 1),
    )

    private fun checkFixture(
        scenario: AppUpdateCheckScenario,
        generation: Long = 0,
        installedVersion: String,
    ): AppUpdateCheckState =
        when (scenario) {
            AppUpdateCheckScenario.Unknown -> AppUpdateCheckState(AppUpdateCheckPhase.Unknown, generation)
            AppUpdateCheckScenario.Current -> AppUpdateCheckState(AppUpdateCheckPhase.Current, generation, installedVersion, 0)
            AppUpdateCheckScenario.Available -> AppUpdateCheckState(AppUpdateCheckPhase.Available, generation, "0.2", 1)
            AppUpdateCheckScenario.ImportantAvailable -> AppUpdateCheckState(AppUpdateCheckPhase.Available, generation, "0.4", 3)
            AppUpdateCheckScenario.Failure -> AppUpdateCheckState(AppUpdateCheckPhase.Failed, generation)
        }

    private fun AppSelfUpdateState.failed(failure: AppSelfUpdateFailure): AppSelfUpdateState = copy(
        phase = AppSelfUpdatePhase.Failed,
        failure = failure,
    )

    private fun compareVersions(left: String, right: String): Int {
        val l = versionSegments(left)
        val r = versionSegments(right)
        for (index in 0 until maxOf(l.size, r.size)) {
            val result = l.getOrElse(index) { BigInteger.ZERO }.compareTo(r.getOrElse(index) { BigInteger.ZERO })
            if (result != 0) return result
        }
        return 0
    }

    private fun versionSegments(version: String): List<BigInteger> = version.trim().split('.').map { segment ->
        Regex("^\\d+").find(segment)?.value?.toBigIntegerOrNull() ?: BigInteger.ZERO
    }
}
