package dev.ipf.whitenoise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AccessPhase
import dev.ipf.whitenoise.model.AccessScenario
import dev.ipf.whitenoise.model.LoginPrototypeData
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileSigningMode
import dev.ipf.whitenoise.model.StartupPhase
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.onboarding.AccessFeedback
import dev.ipf.whitenoise.ui.onboarding.SignInScreen
import dev.ipf.whitenoise.ui.onboarding.StartupScreen
import dev.ipf.whitenoise.ui.onboarding.WelcomeScreen
import dev.ipf.whitenoise.ui.settings.ProfileKeysScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun publicKeyShowsSpecificErrorAndCannotSubmit() {
        var submissions = 0
        rule.setContent { WhiteNoiseTheme {
            SignInScreen(
                onBack = {}, onScan = {}, privateKey = remember { TextFieldState("npub1" + "q".repeat(58)) },
                scannedPrivateKey = null, scannerUnavailable = false,
                onScannedPrivateKeyConsumed = {}, onScannerUnavailableConsumed = {},
                onSignIn = { submissions++ }, onAmberSignIn = {}, attempt = null,
                onRetry = {}, onRecover = {}, onCancel = {},
            )
        } }
        rule.onNodeWithText("This is a Public Key. Enter your Private Key to sign in.").assertIsDisplayed()
        rule.onNodeWithTag("onboarding.sign_in.action").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(0, submissions) }
    }

    @Test fun decliningRecoveryConsentKeepsProfileUnchanged() {
        val vm = AppViewModel(AccessScenario.RecoveryConsent)
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        rule.setContent { WhiteNoiseTheme {
            AccessFeedback(vm.accessAttempt, vm::retryAccess, vm::confirmAccessRecovery, vm::cancelAccess)
        } }
        rule.onNodeWithText("Recover profile setup?").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle {
            assertNull(vm.accessAttempt)
            assertTrue(vm.uiState.profiles.isEmpty())
        }
    }

    @Test fun partialRecoveryRetryReturnsToRecoveryWithoutAnotherConsentPrompt() {
        val vm = AppViewModel(AccessScenario.RecoveryPartial)
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        vm.confirmAccessRecovery(vm.accessAttempt!!.id)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        rule.setContent { WhiteNoiseTheme {
            AccessFeedback(vm.accessAttempt, vm::retryAccess, vm::confirmAccessRecovery, vm::cancelAccess)
        } }
        rule.onNodeWithTag("access.failure").assertTextContains("Some setup changes may already have been applied.", substring = true)
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithText("Recover profile setup?").assertDoesNotExist()
        rule.runOnIdle { assertEquals(AccessPhase.Recovering, vm.accessAttempt!!.phase) }
        rule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test fun retainedPickerStartsTheSelectedStoredProfile() {
        val vm = AppViewModel()
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        val ids = vm.uiState.signedInProfileIds.toList()
        ids.forEach { vm.selectProfile(it); vm.signOutActiveProfile(wipeData = false) }
        rule.setContent { WhiteNoiseTheme {
            WelcomeScreen(
                OnboardingOrigin.Initial, {}, {}, {}, retainedProfiles = vm.uiState.retainedProfiles,
                attempt = vm.accessAttempt,
                onContinueProfile = { vm.beginRetainedSignIn(OnboardingOrigin.Initial, it) },
            )
        } }
        rule.onNodeWithText("Choose profile").performScrollTo().performClick()
        rule.onNodeWithText("Marmota").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(ProfileFixtures.MARMOTA_ID, vm.accessAttempt!!.candidate.id) }
        rule.onNodeWithText("Signing In…").assertIsNotEnabled()
    }

    @Test fun startupFailureHasRetryAndRetainedProfileRecoveryActions() {
        val vm = AppViewModel(startupFails = true)
        vm.advanceStartup(vm.startupState.generation)
        rule.setContent { WhiteNoiseTheme {
            StartupScreen(vm.startupState.phase, hasProfiles = false, vm::retryStartup, vm::recoverStartupProfiles)
        } }
        rule.onNodeWithText("Couldn’t start White Noise").assertIsDisplayed()
        rule.onNodeWithText("Choose profile").assertDoesNotExist()
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithText("Starting White Noise…").assertIsDisplayed()
        rule.runOnIdle { assertEquals(StartupPhase.Loading, vm.startupState.phase) }
    }

    @Test fun amberProfileNeverOffersALocalSecretOrExport() {
        rule.setContent { WhiteNoiseTheme {
            ProfileKeysScreen(ProfileFixtures.openCircuit.copy(signingMode = ProfileSigningMode.Amber), {})
        } }
        rule.onNodeWithTag("profile_keys.amber_info").assertIsDisplayed()
        rule.onNodeWithText("Signed in with Amber").assertIsDisplayed()
        rule.onNodeWithText("Amber holds this profile’s Private Key. Manage backups in Amber.").assertIsDisplayed()
        rule.onNodeWithText("Copy Private Key").assertDoesNotExist()
        rule.onNodeWithText("Export Private Key").assertDoesNotExist()
        rule.onNodeWithTag("profile_keys.private_key_value").assertDoesNotExist()
    }
}
