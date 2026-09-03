package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileExitScenario
import dev.ipf.whitenoise.model.ProfileExitStep
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.SignOutOptions
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.settings.ProfileExitReportDialog
import dev.ipf.whitenoise.ui.settings.ProfileKeysScreen
import dev.ipf.whitenoise.ui.settings.SignOutSheet
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileExitFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun retainedSignOutExposesAndSubmitsTheRelayCleanupChoice() {
        var submitted: SignOutOptions? = null
        rule.setContent { WhiteNoiseTheme {
            SignOutSheet(ProfileFixtures.marmota, {}, { submitted = it })
        } }
        rule.onNodeWithText("Wipe Data From This Device").performClick()
        rule.onNodeWithText("Remove connection information").assertIsOn().performClick().assertIsOff()
        rule.onNodeWithContentDescription("Sign Out").performClick()
        rule.runOnIdle {
            assertEquals(false, submitted?.wipeData)
            assertEquals(false, submitted?.deleteConnectionInformation)
        }
    }

    @Test fun localFailureShowsIndependentResultsAndRetry() {
        val vm = failedExit(ProfileExitScenario.LocalCleanupFailure)
        rule.setContent { WhiteNoiseTheme {
            SignOutSheet(vm.uiState.activeProfile!!, vm::dismissProfileExit, {},
                attempt = vm.profileExitAttempt, onRetry = vm::retryProfileExit)
        } }
        rule.onNodeWithText("Local cleanup did not finish. This profile is still active. Earlier steps marked Done have already taken effect.").assertIsDisplayed()
        rule.onNodeWithTag("exit.step.LeaveGroups").assert(hasAnyDescendant(hasText("Done")))
        rule.onNodeWithTag("exit.step.LocalCleanup").assert(hasAnyDescendant(hasText("Incomplete")))
        rule.onNodeWithContentDescription("Try Again").performClick()
        rule.runOnIdle { assertEquals(ProfileExitStep.LocalCleanup, vm.profileExitAttempt!!.currentStep) }
    }

    @Test fun remoteFailureAfterWipeHasAnExplicitPostExitReport() {
        val vm = failedExit(ProfileExitScenario.RelayCleanupFailure)
        rule.setContent { WhiteNoiseTheme {
            vm.profileExitReport?.let { ProfileExitReportDialog(it, vm::dismissProfileExitReport) }
        } }
        rule.onNodeWithText("Local data was removed, but some cleanup did not finish. The results below show what remains incomplete.").assertIsDisplayed()
        rule.onNodeWithTag("exit.step.RelayCleanup").assert(hasAnyDescendant(hasText("Incomplete")))
        rule.onNodeWithText("Close").performClick()
        rule.runOnIdle {
            assertNull(vm.profileExitReport)
            assertTrue(vm.uiState.profiles.isEmpty())
        }
    }

    @Test fun unavailableLocalKeyShowsRecoveryInsteadOfSecretActions() {
        var profile by mutableStateOf(ProfileFixtures.marmota.copy(localKeyAvailable = false))
        rule.setContent { WhiteNoiseTheme {
            ProfileKeysScreen(profile, {}, onRetryKey = { profile = profile.copy(localKeyAvailable = true) })
        } }
        rule.onNodeWithText("Copy Private Key").assertDoesNotExist()
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithContentDescription("Show private key").assertIsDisplayed()
    }

    @Test fun secretVisibilityAndPasswordDraftDoNotEnterSavedState() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { WhiteNoiseTheme { ProfileKeysScreen(ProfileFixtures.marmota, {}) } }
        rule.onNodeWithContentDescription("Show private key").performClick()
        rule.onNodeWithContentDescription("Hide private key").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Show private key").assertIsDisplayed()
        rule.onNodeWithText("Export Encrypted Private Key").performScrollTo().performClick()
        rule.onNodeWithTag("profile_keys.export_password").performTextInput("long-test-password")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("profile_keys.export_password").assertDoesNotExist()
    }

    private fun failedExit(scenario: ProfileExitScenario) = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        setDeveloperToolsEnabled(true)
        selectProfileExitScenario(scenario)
        beginProfileExit(SignOutOptions(wipeData = true, confirmation = "Marmota"))
        repeat(3) { profileExitAttempt!!.let { advanceProfileExit(it.id, it.currentStep!!) } }
    }
}
