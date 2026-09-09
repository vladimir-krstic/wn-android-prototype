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
        rule.onNodeWithTag("scenario.info").assertIsDisplayed()
        rule.onNodeWithTag("scenario.exit").performClick()
        rule.onNodeWithTag("scenario.variant.Recovery").assertExists()
        rule.runOnIdle { assertEquals(original, vm.uiState) }
    }

    @Test fun restartKeepsTheChosenVariantAndExitReturnsToTheExactLaunchingEntry() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        lateinit var nav: NavHostController
        var launchingEntryId: String? = null
        rule.setContent { nav = rememberNavController(); WhiteNoiseApp(nav, vm) }
        rule.runOnIdle { nav.navigate(AppRoute.ScenarioVariants("setup")) }
        rule.runOnIdle { launchingEntryId = nav.currentBackStackEntry!!.id }
        rule.onNodeWithTag("scenario.variant.Attention").performScrollTo().performClick()
        rule.onNodeWithTag("scenario.change").assertDoesNotExist()
        rule.onNodeWithTag("scenario.restart").performClick()
        rule.onNodeWithTag("scenario.info").performClick()
        rule.onNodeWithTag("scenario.info.title").assertTextEquals("Profile setup")
        rule.onNodeWithTag("scenario.info.description").assertTextContains("Edit your profile or skip it.", substring = true)
        androidx.test.espresso.Espresso.pressBack()
        rule.onNodeWithTag("scenario.info.description").assertDoesNotExist()
        rule.onNodeWithTag("scenario.exit").performClick()
        rule.onNodeWithTag("scenario.variant.Attention").assertIsDisplayed()
        rule.runOnIdle { assertEquals(launchingEntryId, nav.currentBackStackEntry!!.id) }
    }

    @Test fun scenarioExitAndRootBackResumeTheLivingLaunchingEntryAcrossRepeatedRuns() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseApp(nav, vm) }
        rule.runOnIdle { nav.navigate(AppRoute.Scenarios) }
        val original = rule.runOnIdle { vm.uiState }
        listOf("access" to "SignInFailure", "setup" to "Attention", "startup" to "Ready").forEach { (id, variant) ->
            rule.runOnIdle { nav.navigate(AppRoute.ScenarioVariants(id)) }
            val entry = rule.runOnIdle { nav.currentBackStackEntry!! }
            var destroyed = false
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) destroyed = true
            }
            rule.runOnIdle { entry.lifecycle.addObserver(observer) }
            rule.onNodeWithTag("scenario.variant.$variant").performScrollTo().performClick()
            rule.onNodeWithTag("scenario.exit").assertIsDisplayed()
            rule.onNodeWithTag("scenario.variant.$variant").assertDoesNotExist()
            rule.runOnIdle {
                assertFalse("Launching page was destroyed during $id", destroyed)
                assertSame(entry, nav.currentBackStackEntry)
                assertEquals(androidx.lifecycle.Lifecycle.State.CREATED, entry.lifecycle.currentState)
            }
            rule.onNodeWithTag("scenario.restart").performClick()
            if (id == "startup") androidx.test.espresso.Espresso.pressBack()
            else rule.onNodeWithTag("scenario.exit").performClick()
            rule.onNodeWithTag("scenario.variant.$variant").assertIsDisplayed()
            rule.runOnIdle {
                assertFalse("Launching page was destroyed on exit from $id", destroyed)
                assertSame(entry, nav.currentBackStackEntry)
                assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, entry.lifecycle.currentState)
                assertEquals(original, vm.uiState)
                entry.lifecycle.removeObserver(observer)
            }
            // Ordinary Back must still return to the catalog after leaving the run.
            androidx.test.espresso.Espresso.pressBack()
            rule.onNodeWithTag("scenarios.search").assertIsDisplayed()
        }
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
