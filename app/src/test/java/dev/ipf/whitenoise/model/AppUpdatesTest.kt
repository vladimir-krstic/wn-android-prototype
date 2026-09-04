package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatesTest {
    @Test
    fun storeManagedDistributionHasNoInAppUpdateSurfaceOrFlow() {
        val state = AppUpdates.selectDistribution(
            AppUpdates.initial("0.1"),
            AppUpdateDistribution.StoreManaged,
        )

        assertFalse(AppUpdates.showsSettings(state))
        assertFalse(AppUpdates.showsBanner(state))
        assertEquals(state, AppUpdates.beginCheck(state))
        assertEquals(state, AppUpdates.beginSelfUpdate(state))
    }

    @Test
    fun normalUpdateDismissesOnlyForItsCurrentVersion() {
        var state = AppUpdates.initial("0.1")
        assertTrue(AppUpdates.showsBanner(state))
        assertTrue(AppUpdates.canDismissBanner(state))

        state = AppUpdates.dismissBanner(state)
        assertFalse(AppUpdates.showsBanner(state))

        state = AppUpdates.previewCheck(state, AppUpdateCheckScenario.ImportantAvailable)
        assertTrue(AppUpdates.showsBanner(state))
    }

    @Test
    fun importantUpdateCannotBeDismissed() {
        val state = AppUpdates.previewCheck(
            AppUpdates.initial("0.1"),
            AppUpdateCheckScenario.ImportantAvailable,
        )
        assertTrue(AppUpdates.isImportant(state))
        assertFalse(AppUpdates.canDismissBanner(state))
        assertEquals(state, AppUpdates.dismissBanner(state))
    }

    @Test
    fun checkCompletionRejectsStaleGenerationAndProjectsEveryResult() {
        var state = AppUpdates.previewCheck(AppUpdates.initial("7.2"), AppUpdateCheckScenario.Current)
        state = AppUpdates.beginCheck(state)
        val generation = state.check.generation
        assertEquals(AppUpdateCheckPhase.Checking, state.check.phase)
        assertEquals(state, AppUpdates.completeCheck(state, generation - 1))

        state = AppUpdates.completeCheck(state, generation)
        assertEquals(AppUpdateCheckPhase.Current, state.check.phase)
        assertEquals("7.2", state.check.latestVersion)

        state = AppUpdates.previewCheck(state, AppUpdateCheckScenario.Failure)
        assertEquals(AppUpdateCheckPhase.Failed, state.check.phase)
        state = AppUpdates.previewCheck(state, AppUpdateCheckScenario.Unknown)
        assertEquals(AppUpdateCheckPhase.Unknown, state.check.phase)
    }

    @Test
    fun successfulFlowSeparatesDownloadVerificationReadinessAndInstallRequest() {
        var state = AppUpdates.beginSelfUpdate(AppUpdates.initial("0.1"))
        val generation = state.selfUpdate.generation
        assertEquals(AppSelfUpdatePhase.Resolving, state.selfUpdate.phase)
        assertEquals(state, AppUpdates.advanceSelfUpdate(state, generation - 1))

        state = AppUpdates.advanceSelfUpdate(state, generation)
        assertEquals(AppSelfUpdatePhase.Confirming, state.selfUpdate.phase)
        state = AppUpdates.confirmDownload(state)
        assertEquals(AppSelfUpdatePhase.Downloading, state.selfUpdate.phase)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        assertTrue(state.selfUpdate.bytesRead in 1 until state.selfUpdate.sizeBytes!!)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        assertEquals(AppSelfUpdatePhase.Verifying, state.selfUpdate.phase)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        assertEquals(AppSelfUpdatePhase.Ready, state.selfUpdate.phase)

        state = AppUpdates.requestInstall(state)
        assertEquals(AppSelfUpdatePhase.Idle, state.selfUpdate.phase)
    }

    @Test
    fun eachFailureStopsAtItsOwnedBoundaryAndRetryRestartsResolution() {
        val scenarios = mapOf(
            AppSelfUpdateScenario.ResolveFailure to AppSelfUpdateFailure.Resolve,
            AppSelfUpdateScenario.DownloadFailure to AppSelfUpdateFailure.Download,
            AppSelfUpdateScenario.VerificationFailure to AppSelfUpdateFailure.Verification,
        )
        scenarios.forEach { (scenario, expectedFailure) ->
            var state = AppUpdates.selectSelfUpdateScenario(AppUpdates.initial("0.1"), scenario)
            state = AppUpdates.beginSelfUpdate(state)
            val generation = state.selfUpdate.generation
            state = AppUpdates.advanceSelfUpdate(state, generation)
            if (scenario != AppSelfUpdateScenario.ResolveFailure) {
                state = AppUpdates.confirmDownload(state)
                state = AppUpdates.advanceSelfUpdate(state, generation)
                state = AppUpdates.advanceSelfUpdate(state, generation)
                if (scenario == AppSelfUpdateScenario.VerificationFailure) {
                    state = AppUpdates.advanceSelfUpdate(state, generation)
                }
            }
            assertEquals(AppSelfUpdatePhase.Failed, state.selfUpdate.phase)
            assertEquals(expectedFailure, state.selfUpdate.failure)
            val failedGeneration = state.selfUpdate.generation
            state = AppUpdates.retry(state)
            assertEquals(AppSelfUpdatePhase.Resolving, state.selfUpdate.phase)
            assertNotEquals(failedGeneration, state.selfUpdate.generation)
            assertEquals(AppSelfUpdatePhase.Idle, AppUpdates.cancel(state).selfUpdate.phase)
        }
    }

    @Test
    fun installPermissionComesAfterVerificationAndReturnsToReadyReview() {
        var state = AppUpdates.selectSelfUpdateScenario(
            AppUpdates.initial("0.1"),
            AppSelfUpdateScenario.PermissionRequired,
        )
        state = AppUpdates.beginSelfUpdate(state)
        val generation = state.selfUpdate.generation
        state = AppUpdates.advanceSelfUpdate(state, generation)
        state = AppUpdates.confirmDownload(state)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        assertEquals(AppSelfUpdatePhase.Verifying, state.selfUpdate.phase)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        assertEquals(AppSelfUpdatePhase.PermissionRequired, state.selfUpdate.phase)
        state = AppUpdates.reviewInstallPermission(state)
        assertEquals(AppSelfUpdatePhase.Ready, state.selfUpdate.phase)
    }

    @Test
    fun installerHandoffFailureIsRetryableWithoutClaimingInstallation() {
        var state = AppUpdates.selectSelfUpdateScenario(
            AppUpdates.initial("0.1"),
            AppSelfUpdateScenario.InstallFailure,
        )
        state = readyState(state)
        state = AppUpdates.requestInstall(state)
        assertEquals(AppSelfUpdatePhase.Failed, state.selfUpdate.phase)
        assertEquals(AppSelfUpdateFailure.Install, state.selfUpdate.failure)
        assertEquals(AppSelfUpdatePhase.Resolving, AppUpdates.retry(state).selfUpdate.phase)
    }

    private fun readyState(start: AppUpdateState): AppUpdateState {
        var state = AppUpdates.beginSelfUpdate(start)
        val generation = state.selfUpdate.generation
        state = AppUpdates.advanceSelfUpdate(state, generation)
        state = AppUpdates.confirmDownload(state)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        state = AppUpdates.advanceSelfUpdate(state, generation)
        return AppUpdates.advanceSelfUpdate(state, generation)
    }
}
