package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ProfileExitDestination
import dev.ipf.whitenoise.model.ProfileExitScenario
import dev.ipf.whitenoise.model.ProfileExitStep
import dev.ipf.whitenoise.model.ProfileExitStepResult
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.SignOutOptions
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class ProfileExitStateTest {
    @Test fun wipeRequiresCurrentProfileNameAndBlocksDuplicateRequests() {
        val vm = signedIn()
        assertFalse(vm.beginProfileExit(SignOutOptions(wipeData = true, confirmation = "wrong")))
        assertNull(vm.profileExitAttempt)
        assertTrue(vm.beginProfileExit(SignOutOptions(wipeData = true, confirmation = "Marmota")))
        val id = vm.profileExitAttempt!!.id
        assertFalse(vm.beginProfileExit(SignOutOptions(wipeData = false)))
        assertEquals(id, vm.profileExitAttempt!!.id)
        vm.dismissProfileExit()
        assertNotNull(vm.profileExitAttempt)
    }

    @Test fun retainingWithoutRelayCleanupRunsOnlyLocalSignOutAndPreservesData() {
        val vm = signedIn()
        vm.updateDraftText("catalog-direct-text", "Keep this draft")
        val original = vm.uiState.activeProfile!!
        vm.beginProfileExit(SignOutOptions(wipeData = false, deleteConnectionInformation = false))
        val attempt = vm.profileExitAttempt!!
        assertEquals(ProfileExitStep.LocalCleanup, attempt.currentStep)
        assertEquals(ProfileExitStepResult.NotRequested, attempt.results[ProfileExitStep.RelayCleanup])
        assertEquals(ProfileExitDestination.Welcome, advance(vm))
        assertEquals(original, vm.uiState.retainedProfiles.single())
        assertNull(vm.profileExitReport)
    }

    @Test fun relayFailureDoesNotMasqueradeAsLocalSignOutFailure() {
        val vm = signedIn(ProfileExitScenario.RelayCleanupFailure)
        vm.beginProfileExit(SignOutOptions(wipeData = false))
        assertNull(advance(vm))
        assertEquals(ProfileExitDestination.Welcome, advance(vm))
        val report = vm.profileExitReport!!
        assertTrue(report.localCleanupCompleted)
        assertEquals(ProfileExitStepResult.Incomplete, report.results[ProfileExitStep.RelayCleanup])
        assertTrue(vm.uiState.signedInProfileIds.isEmpty())
        assertEquals(1, vm.uiState.profiles.size)
        assertTrue(vm.uiState.profiles.single().connectionInformationPublished)
    }

    @Test fun completedRelayCleanupIsReflectedInRetainedStateAndReentryRepublishes() {
        val vm = signedIn()
        vm.beginProfileExit(SignOutOptions(wipeData = false))
        advance(vm)
        assertFalse(vm.uiState.activeProfile!!.connectionInformationPublished)
        advance(vm)
        val retained = vm.uiState.retainedProfiles.single()
        assertFalse(retained.connectionInformationPublished)
        vm.beginRetainedSignIn(OnboardingOrigin.Initial, retained.id)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        assertTrue(vm.uiState.activeProfile!!.connectionInformationPublished)
    }

    @Test fun localFailureRetainsProfileAndCompletedRemoteStepsThenRetriesOnlyLocalCleanup() {
        val vm = signedIn(ProfileExitScenario.LocalCleanupFailure)
        vm.beginProfileExit(SignOutOptions(wipeData = true, confirmation = "Marmota"))
        advance(vm)
        advance(vm)
        assertNull(advance(vm))
        val failed = vm.profileExitAttempt!!
        assertFalse(failed.isRunning)
        assertFalse(failed.localCleanupCompleted)
        assertEquals(ProfileFixtures.MARMOTA_ID, vm.uiState.activeProfileId)
        assertFalse(vm.uiState.activeProfile!!.connectionInformationPublished)
        assertTrue(vm.uiState.activeProfile!!.chats.filter { it.isGroup }.none { it.membership == ChatMembership.Active })
        vm.retryProfileExit(failed.id)
        assertEquals(ProfileExitStep.LocalCleanup, vm.profileExitAttempt!!.currentStep)
        assertNull(vm.advanceProfileExit(failed.id, ProfileExitStep.LocalCleanup))
        assertEquals(ProfileExitDestination.Welcome, advance(vm))
        assertTrue(vm.uiState.profiles.isEmpty())
    }

    @Test fun wipeWithRemoteFailureRemovesOnlyCapturedProfileAndReportsPartialResult() {
        val vm = signedIn()
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        vm.selectProfile(ProfileFixtures.MARMOTA_ID)
        vm.selectProfileExitScenario(ProfileExitScenario.GroupLeaveFailure)
        vm.beginProfileExit(SignOutOptions(wipeData = true, confirmation = "Marmota"))
        val request = vm.profileExitAttempt!!
        advance(vm)
        advance(vm)
        assertEquals(ProfileExitDestination.ProfileSwitcher, advance(vm))
        assertTrue(vm.uiState.profiles.none { it.id == ProfileFixtures.MARMOTA_ID })
        assertNotNull(vm.uiState.activeProfile)
        assertEquals(ProfileExitStepResult.Incomplete, vm.profileExitReport!!.results[ProfileExitStep.LeaveGroups])
        val remaining = vm.uiState.profiles
        assertNull(vm.advanceProfileExit(request.id, ProfileExitStep.LocalCleanup))
        assertEquals(remaining, vm.uiState.profiles)
    }

    @Test fun switchingProfileRejectsAStaleWipeCompletion() {
        val vm = signedIn()
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        vm.selectProfile(ProfileFixtures.MARMOTA_ID)
        vm.beginProfileExit(SignOutOptions(wipeData = true, confirmation = "Marmota"))
        val request = vm.profileExitAttempt!!
        vm.selectProfile(ProfileFixtures.openCircuit.id)
        assertNull(vm.advanceProfileExit(request.id, ProfileExitStep.LeaveGroups))
        assertTrue(vm.uiState.profiles.any { it.id == ProfileFixtures.MARMOTA_ID })
        assertEquals(ProfileFixtures.openCircuit.id, vm.uiState.activeProfileId)
    }

    @Test fun retryAfterAllStepsFailRepeatsOnlyIncompleteWorkInOrder() {
        val vm = signedIn(ProfileExitScenario.AllCleanupFailure)
        vm.beginProfileExit(SignOutOptions(wipeData = true, confirmation = "Marmota"))
        repeat(3) { advance(vm) }
        val failed = vm.profileExitAttempt!!
        assertTrue(failed.results.values.all { it == ProfileExitStepResult.Incomplete })
        assertTrue(vm.uiState.activeProfile!!.connectionInformationPublished)
        vm.retryProfileExit(failed.id)
        ProfileExitStep.entries.forEach { expected ->
            assertEquals(expected, vm.profileExitAttempt!!.currentStep)
            advance(vm)
        }
        assertTrue(vm.uiState.profiles.isEmpty())
    }

    @Test fun closingFailureDoesNotUndoRemoteWork() {
        val vm = signedIn(ProfileExitScenario.LocalCleanupFailure)
        vm.beginProfileExit(SignOutOptions(wipeData = false))
        advance(vm)
        advance(vm)
        vm.dismissProfileExit()
        assertNull(vm.profileExitAttempt)
        assertNotNull(vm.uiState.activeProfile)
        assertFalse(vm.uiState.activeProfile!!.connectionInformationPublished)
    }

    @Test fun unavailableKeyRetryIsOwnedByCurrentProfile() {
        val vm = signedIn()
        vm.setLocalKeyAvailable(false)
        assertFalse(vm.uiState.activeProfile!!.localKeyAvailable)
        vm.retryLocalKeyAccess("wrong-profile")
        assertFalse(vm.uiState.activeProfile!!.localKeyAvailable)
        vm.retryLocalKeyAccess(ProfileFixtures.MARMOTA_ID)
        assertTrue(vm.uiState.activeProfile!!.localKeyAvailable)
    }

    private fun signedIn(scenario: ProfileExitScenario = ProfileExitScenario.Success) = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        setDeveloperToolsEnabled(true)
        selectProfileExitScenario(scenario)
    }

    private fun advance(vm: AppViewModel): ProfileExitDestination? = vm.profileExitAttempt!!.let {
        vm.advanceProfileExit(it.id, checkNotNull(it.currentStep))
    }
}
