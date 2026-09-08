package dev.ipf.whitenoise

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compiled by the host gate. Running requires explicit device-test authorization. */
@RunWith(AndroidJUnit4::class)
class ProfileSetupFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    private fun open(vm: AppViewModel, origin: OnboardingOrigin = OnboardingOrigin.Initial): NavHostController {
        lateinit var nav: NavHostController
        rule.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        rule.runOnIdle {
            if (origin == OnboardingOrigin.AddProfile) nav.navigate(AppRoute.Welcome(origin))
        }
        // Exercise credential entry and its cleanup during the outgoing navigation animation.
        // Starting the ViewModel directly leaves this field empty and misses the regression.
        rule.onNodeWithText("Sign In").performClick()
        rule.onNodeWithContentDescription("Paste private key").performClick()
        rule.onNodeWithTag("onboarding.sign_in.action").performClick()
        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        rule.onNodeWithText("Profile Setup").assertIsDisplayed()
        rule.runOnIdle { assertEquals(AccessPhase.ProfileSetup, vm.accessAttempt?.phase) }
        return nav
    }

    private fun drain(vm: AppViewModel) = rule.runOnIdle {
        repeat(8) {
            val session = vm.profileSetup.session ?: return@runOnIdle
            val work = session.work ?: return@runOnIdle
            vm.profileSetup.complete(session.id, work)
        }
    }

    @Test fun deviceDecisionReturnsToStableChecklistAndOnlyOpenChatsActivates() {
        val vm = AppViewModel()
        open(vm)
        drain(vm)
        rule.onNodeWithTag("setup.open_chats").assertIsNotEnabled()
        rule.onNodeWithTag("setup.step.Device").performScrollTo().assertHasClickAction().performClick()
        rule.onNodeWithText("Continue").performClick()
        drain(vm)
        rule.onNodeWithTag("setup.open_chats").assertIsEnabled()
        rule.onNodeWithText("Checking your profile before you start chatting.").assertDoesNotExist()
        rule.runOnIdle { assertNull(vm.uiState.activeProfile) }
        rule.onNodeWithTag("setup.open_chats").performClick()
        rule.runOnIdle { assertEquals(ProfileFixtures.MARMOTA_ID, vm.uiState.activeProfileId) }
    }

    @Test fun detailBackLeavesChoiceUnconfirmedAndRootBackCancelsToWelcome() {
        val vm = AppViewModel(initialSetupScenario = ProfileSetupScenario.Attention)
        val nav = open(vm)
        drain(vm)
        rule.onNodeWithTag("setup.step.Profile").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag("setup.step.Profile").assertHasClickAction()
        rule.runOnIdle { assertEquals(ProfileSetupStatus.Attention, vm.profileSetup.session!!.check(ProfileSetupStep.Profile).status) }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Sign In").assertIsDisplayed()
        rule.onNodeWithText("Profile Setup").assertDoesNotExist()
        rule.runOnIdle {
            assertNull(vm.profileSetup.session)
            assertNull(vm.accessAttempt)
            assertEquals(AppRoute.Welcome::class.qualifiedName, nav.currentDestination!!.route!!.substringBefore('/').substringBefore('?'))
        }
    }

    @Test fun profileSaveFailureKeepsEditorAndDraftUntilRetrySucceeds() {
        val vm = AppViewModel(initialSetupScenario = ProfileSetupScenario.ProfileSaveFailure)
        open(vm)
        drain(vm)
        rule.onNodeWithTag("setup.step.Profile").performScrollTo().performClick()
        rule.onNodeWithText("Edit Profile").performClick()
        rule.onNode(hasSetTextAction().and(hasText("Marmota"))).performTextReplacement("My profile")
        rule.onNodeWithText("Save").assertIsDisplayed().performClick()
        drain(vm)
        rule.onNodeWithText("Save").assertIsDisplayed()
        rule.onNode(hasSetTextAction().and(hasText("My profile"))).assertExists()
        rule.runOnIdle { assertEquals(ProfileSetupIssue.ProfileSave, vm.profileSetup.session!!.check(ProfileSetupStep.Profile).issue) }
        rule.onNodeWithText("Save").performClick()
        drain(vm)
        rule.onNodeWithText("Profile Setup").assertIsDisplayed()
        rule.runOnIdle { assertEquals("My profile", vm.profileSetup.session!!.candidate.name) }
    }

    @Test fun inconclusiveRecoveryHasNoDefaultsActionAndRejectsInvalidDiscoveryInput() {
        val vm = AppViewModel(initialSetupScenario = ProfileSetupScenario.InconclusiveRelays)
        open(vm)
        drain(vm)
        rule.onNodeWithTag("setup.step.Relays").performScrollTo().performClick()
        rule.onNodeWithText("Use Default Relays").assertDoesNotExist()
        rule.onNodeWithText("Find My Settings").performClick()
        rule.onNodeWithText("Enter a valid public relay URL starting with wss://.").performScrollTo().assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextInput("wss://relay.example.com")
        rule.onNodeWithText("Find My Settings").performClick()
        drain(vm)
        rule.onNodeWithText("Profile Setup").assertIsDisplayed()
    }

    @Test fun addProfileCancellationPreservesTheExistingActiveProfile() {
        val vm = AppViewModel(initialSetupScenario = ProfileSetupScenario.Attention)
        vm.completeSignIn(OnboardingOrigin.Initial)
        val before = vm.uiState.activeProfile
        open(vm, OnboardingOrigin.AddProfile)
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Add Profile").assertIsDisplayed()
        rule.runOnIdle { assertEquals(before, vm.uiState.activeProfile); assertNull(vm.profileSetup.session) }
    }

    @Test fun catalogRunsImmediatelyWhenDeveloperToolsAreDisabledAndExitRestoresSession() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val original = vm.uiState
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseApp(nav, vm) }
        rule.runOnIdle { nav.navigate(AppRoute.DeveloperTools) }
        rule.onNodeWithTag("developer.scenarios").performScrollTo().performClick()
        rule.onNodeWithTag("scenario.setup").performScrollTo().performClick()
        rule.onNodeWithTag("scenario.variant.Recovery").performScrollTo().performClick()
        rule.onNodeWithTag("scenario.running").assertIsDisplayed()
        rule.onNodeWithTag("scenario.exit").performClick()
        rule.onNodeWithTag("scenario.variant.Recovery").assertExists()
        rule.runOnIdle { assertEquals(original, vm.uiState) }
    }

    @Test fun changeVariantReturnsToItsPageAndRestartKeepsTheChosenVariant() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseApp(nav, vm) }
        rule.runOnIdle { nav.navigate(AppRoute.ScenarioVariants("setup")) }
        rule.onNodeWithTag("scenario.variant.Attention").performScrollTo().performClick()
        rule.onNodeWithTag("scenario.restart").performClick()
        rule.onNodeWithTag("scenario.running").assertTextContains("Needs attention", substring = true)
        rule.onNodeWithTag("scenario.change").performClick()
        rule.onNodeWithTag("scenario.variant.MessagingFailure").performScrollTo().assertExists()
    }

    @Test fun restoredSetupRouteWithoutAttemptReturnsToInitialWelcome() {
        val vm = AppViewModel()
        lateinit var nav: NavHostController
        rule.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        rule.runOnIdle {
            nav.navigate(AppRoute.Welcome(OnboardingOrigin.AddProfile))
            nav.navigate(AppRoute.ProfileSetup(OnboardingOrigin.AddProfile))
        }
        rule.onNodeWithText("Sign In").assertIsDisplayed()
        rule.onNodeWithText("Add Profile").assertDoesNotExist()
        rule.runOnIdle { assertNull(vm.accessAttempt); assertNull(vm.uiState.activeProfile) }
    }
}
