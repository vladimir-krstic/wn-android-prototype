package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.AccessFailure
import dev.ipf.whitenoise.model.AccessPhase
import dev.ipf.whitenoise.model.AccessScenario
import dev.ipf.whitenoise.model.LoginPrototypeData
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileSigningMode
import dev.ipf.whitenoise.model.StartupPhase
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class AccessStateTest {
    @Test
    fun wrongKeyTypesAndWrongOriginDoNotStartAnAttempt() {
        val vm = AppViewModel()
        assertFalse(vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, "npub1" + "q".repeat(58)))
        assertFalse(vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, "ncryptsec1" + "q".repeat(60)))
        assertFalse(vm.beginPrivateKeySignIn(OnboardingOrigin.AddProfile, LoginPrototypeData.privateKey))
        assertNull(vm.accessAttempt)
        assertTrue(vm.uiState.profiles.isEmpty())
    }

    @Test
    fun cancelAndDuplicateCallbacksCannotActivateAProfile() {
        val vm = AppViewModel()
        start(vm)
        val first = vm.accessAttempt!!
        assertFalse(vm.beginAmberSignIn(OnboardingOrigin.Initial))
        vm.cancelAccess()
        start(vm)
        assertFalse(vm.advanceAccess(first.id, first.phase))
        assertTrue(vm.uiState.profiles.isEmpty())
        assertTrue(advance(vm))
        assertEquals(1, vm.uiState.profiles.size)
        assertFalse(vm.advanceAccess(first.id, first.phase))
    }

    @Test
    fun recoveryCannotRunBeforeConsentAndDecliningChangesNothing() {
        val vm = AppViewModel(AccessScenario.RecoveryConsent)
        start(vm)
        val request = vm.accessAttempt!!
        vm.confirmAccessRecovery(request.id)
        assertFalse(vm.accessAttempt!!.recoveryAcknowledged)
        advance(vm)
        assertEquals(AccessPhase.RecoveryConsent, vm.accessAttempt!!.phase)
        assertFalse(advance(vm))
        vm.cancelAccess()
        vm.confirmAccessRecovery(request.id)
        assertNull(vm.accessAttempt)
        assertTrue(vm.uiState.profiles.isEmpty())
    }

    @Test
    fun partialRecoveryKeepsAcknowledgementAndRetriesWithoutRepeatingConsent() {
        val vm = AppViewModel(AccessScenario.RecoveryPartial)
        start(vm)
        advance(vm)
        vm.confirmAccessRecovery(vm.accessAttempt!!.id)
        assertEquals(AccessPhase.Recovering, vm.accessAttempt!!.phase)
        advance(vm)
        val failed = vm.accessAttempt!!
        assertEquals(AccessFailure.RecoveryPartial, failed.failure)
        assertTrue(vm.uiState.signedInProfileIds.isEmpty())
        vm.retryAccess(failed.id)
        assertEquals(AccessPhase.Recovering, vm.accessAttempt!!.phase)
        assertTrue(vm.accessAttempt!!.recoveryAcknowledged)
        assertFalse(vm.advanceAccess(failed.id, failed.phase))
        assertTrue(advance(vm))
        assertEquals(ProfileFixtures.MARMOTA_ID, vm.uiState.activeProfileId)
    }

    @Test
    fun ordinarySetupOutcomesRequireExplicitRetry() {
        val cases = mapOf(
            AccessScenario.Offline to AccessFailure.Offline,
            AccessScenario.SignInFailure to AccessFailure.SignIn,
            AccessScenario.SetupRetry to AccessFailure.SetupRetry,
            AccessScenario.PublicationRetry to AccessFailure.PublicationRetry,
            AccessScenario.UnexpectedSetup to AccessFailure.UnexpectedSetup,
        )
        cases.forEach { (scenario, failure) ->
            val vm = AppViewModel(scenario)
            start(vm)
            assertFalse(advance(vm))
            assertEquals(failure, vm.accessAttempt!!.failure)
            assertFalse(advance(vm))
            assertTrue(vm.uiState.profiles.isEmpty())
            vm.retryAccess(vm.accessAttempt!!.id)
            assertTrue(advance(vm))
        }
    }

    @Test
    fun unexpectedRecoveryDoesNotBecomeAnUntouchedOrOrdinarySetupFailure() {
        val vm = AppViewModel(AccessScenario.RecoveryUnexpected)
        start(vm)
        advance(vm)
        vm.confirmAccessRecovery(vm.accessAttempt!!.id)
        advance(vm)
        assertEquals(AccessFailure.RecoveryUnexpected, vm.accessAttempt!!.failure)
        assertTrue(vm.accessAttempt!!.recoveryAcknowledged)
    }

    @Test
    fun amberRequiresBothStagesAndPersistsExternalOwnership() {
        val vm = AppViewModel()
        assertTrue(vm.beginAmberSignIn(OnboardingOrigin.Initial))
        assertEquals(AccessPhase.AmberIdentity, vm.accessAttempt!!.phase)
        assertFalse(advance(vm))
        assertEquals(AccessPhase.AmberProof, vm.accessAttempt!!.phase)
        assertTrue(vm.uiState.profiles.isEmpty())
        assertTrue(advance(vm))
        val profile = vm.uiState.activeProfile!!
        assertEquals(ProfileSigningMode.Amber, profile.signingMode)
        vm.signOutActiveProfile(wipeData = false)
        assertTrue(vm.beginRetainedSignIn(OnboardingOrigin.Initial, profile.id))
        assertEquals(AccessPhase.AmberIdentity, vm.accessAttempt!!.phase)
        advance(vm)
        advance(vm)
        assertEquals(profile, vm.uiState.activeProfile)
    }

    @Test
    fun amberUsesTheRegularAccountForEachSignInOrigin() {
        OnboardingOrigin.entries.forEach { origin ->
            val regular = AppViewModel()
            val amber = AppViewModel()
            if (origin == OnboardingOrigin.AddProfile) {
                regular.completeSignIn(OnboardingOrigin.Initial)
                amber.completeSignIn(OnboardingOrigin.Initial)
            }
            assertTrue(regular.beginPrivateKeySignIn(origin, LoginPrototypeData.privateKey))
            assertTrue(advance(regular))
            assertTrue(amber.beginAmberSignIn(origin))
            assertFalse(advance(amber))
            assertTrue(advance(amber))
            assertEquals(regular.uiState.activeProfile!!.copy(signingMode = ProfileSigningMode.Amber), amber.uiState.activeProfile)
            assertEquals(regular.uiState.profiles.map { it.id }, amber.uiState.profiles.map { it.id })
        }
    }

    @Test
    fun switchingSignInMethodPreservesExistingAccountDataWithoutDuplicatingIt() {
        val vm = AppViewModel()
        assertTrue(vm.beginProfileCreation(OnboardingOrigin.Initial, "My name", "My bio", null))
        assertTrue(advance(vm))
        vm.updateDraftText("catalog-direct-text", "Keep this draft")
        vm.markChatUnread("catalog-direct-text", true)
        val expected = vm.uiState.activeProfile!!
        vm.signOutActiveProfile(wipeData = false)
        assertTrue(vm.beginAmberSignIn(OnboardingOrigin.Initial))
        assertFalse(advance(vm))
        assertTrue(advance(vm))
        assertEquals(expected.copy(signingMode = ProfileSigningMode.Amber), vm.uiState.activeProfile)
        assertEquals(1, vm.uiState.profiles.size)
        vm.signOutActiveProfile(wipeData = false)
        assertTrue(vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey))
        assertTrue(advance(vm))
        assertEquals(expected, vm.uiState.activeProfile)
        assertEquals(1, vm.uiState.profiles.size)
    }

    @Test
    fun amberCancellationRejectionUnavailabilityAndMismatchNeverCreateAProfile() {
        val cases = mapOf(
            AccessScenario.AmberUnavailable to AccessFailure.AmberUnavailable,
            AccessScenario.AmberIdentityCancelled to AccessFailure.AmberCancelled,
            AccessScenario.AmberIdentityRejected to AccessFailure.AmberRejected,
            AccessScenario.AmberProofCancelled to AccessFailure.AmberCancelled,
            AccessScenario.AmberProofRejected to AccessFailure.AmberRejected,
            AccessScenario.AmberMismatch to AccessFailure.AmberMismatch,
        )
        cases.forEach { (scenario, failure) ->
            val vm = AppViewModel(scenario)
            vm.beginAmberSignIn(OnboardingOrigin.Initial)
            advance(vm)
            if (vm.accessAttempt!!.phase.isBusy) advance(vm)
            assertEquals(failure, vm.accessAttempt!!.failure)
            assertTrue(vm.uiState.profiles.isEmpty())
        }
    }

    @Test
    fun retainedReentryPreservesTheExactProfileAndDoesNotDuplicateIt() {
        val vm = AppViewModel()
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.updateDraftText("catalog-direct-text", "Saved draft")
        vm.markChatUnread("catalog-direct-text", true)
        val expected = vm.uiState.activeProfile!!
        vm.signOutActiveProfile(wipeData = false)
        assertEquals(expected, vm.uiState.retainedProfiles.single())
        assertTrue(vm.beginRetainedSignIn(OnboardingOrigin.Initial, expected.id))
        assertTrue(advance(vm))
        assertEquals(expected, vm.uiState.activeProfile)
        assertEquals(1, vm.uiState.profiles.size)
        assertFalse(vm.beginRetainedSignIn(OnboardingOrigin.AddProfile, expected.id))
    }

    @Test
    fun wipeAndRemovalInvalidateRetainedRequests() {
        val vm = AppViewModel()
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.signOutActiveProfile(wipeData = true)
        assertFalse(vm.beginRetainedSignIn(OnboardingOrigin.Initial, ProfileFixtures.MARMOTA_ID))
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.signOutActiveProfile(wipeData = false)
        vm.beginRetainedSignIn(OnboardingOrigin.Initial, ProfileFixtures.MARMOTA_ID)
        val attempt = vm.accessAttempt!!
        assertTrue(vm.removeStoredProfile(ProfileFixtures.MARMOTA_ID, "Marmota"))
        assertFalse(vm.advanceAccess(attempt.id, attempt.phase))
        assertTrue(vm.uiState.profiles.isEmpty())
    }

    @Test
    fun changingProfilesOrErasingInvalidatesPendingAccess() {
        val vm = AppViewModel()
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.beginAmberSignIn(OnboardingOrigin.AddProfile)
        val attempt = vm.accessAttempt!!
        vm.selectProfile(ProfileFixtures.MARMOTA_ID)
        assertFalse(vm.advanceAccess(attempt.id, attempt.phase))
        vm.beginAmberSignIn(OnboardingOrigin.AddProfile)
        val second = vm.accessAttempt!!
        val phrase = WipeConfirmationPhrase.make(vm.uiState.profiles.map { it.id })
        assertTrue(vm.eraseAppData(phrase))
        assertFalse(vm.advanceAccess(second.id, second.phase))
        assertTrue(vm.uiState.profiles.isEmpty())
    }

    @Test
    fun creationFailureRetainsSubmittedDraftUntilRetryOrCancel() {
        val vm = AppViewModel(AccessScenario.SignInFailure)
        vm.beginProfileCreation(OnboardingOrigin.Initial, "Saved name", "Saved about", null)
        advance(vm)
        assertEquals(AccessFailure.CreateProfile, vm.accessAttempt!!.failure)
        assertTrue(vm.uiState.profiles.isEmpty())
        vm.retryAccess(vm.accessAttempt!!.id)
        assertTrue(advance(vm))
        assertEquals("Saved name", vm.uiState.activeProfile!!.name)
        assertEquals("Saved about", vm.uiState.activeProfile!!.about)
    }

    @Test
    fun ordinaryLaunchIsReadyWithoutWaitingForAStartupCallback() {
        val vm = AppViewModel()
        assertEquals(StartupPhase.Ready, vm.startupState.phase)
        val initialState = vm.uiState
        vm.advanceStartup(vm.startupState.generation)
        assertEquals(StartupPhase.Ready, vm.startupState.phase)
        assertEquals(initialState, vm.uiState)
    }

    @Test
    fun startupRetryRejectsOldGenerationAndPreservesProfiles() {
        val vm = AppViewModel(startupFails = true)
        vm.advanceStartup(0)
        assertEquals(StartupPhase.Failed, vm.startupState.phase)
        vm.retryStartup()
        vm.advanceStartup(0)
        assertEquals(StartupPhase.Loading, vm.startupState.phase)
        vm.advanceStartup(vm.startupState.generation)
        assertEquals(StartupPhase.Ready, vm.startupState.phase)
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.setDeveloperToolsEnabled(true)
        val profiles = vm.uiState.profiles
        vm.previewStartupFailure()
        vm.advanceStartup(vm.startupState.generation)
        vm.recoverStartupProfiles()
        assertEquals(profiles, vm.uiState.profiles)
        assertEquals(profiles, vm.uiState.retainedProfiles)
        assertNull(vm.uiState.activeProfile)
    }

    @Test
    fun scenarioControlsRequireDeveloperToolsAndAreOneShot() {
        val vm = AppViewModel()
        vm.selectAccessScenario(AccessScenario.Offline)
        assertEquals(AccessScenario.Success, vm.nextAccessScenario)
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.setDeveloperToolsEnabled(true)
        vm.selectAccessScenario(AccessScenario.Offline)
        vm.beginAmberSignIn(OnboardingOrigin.AddProfile)
        assertEquals(AccessScenario.Offline, vm.accessAttempt!!.scenario)
        assertEquals(AccessScenario.Success, vm.nextAccessScenario)
    }

    private fun start(vm: AppViewModel) = assertTrue(vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey))

    private fun advance(vm: AppViewModel): Boolean = vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
}
