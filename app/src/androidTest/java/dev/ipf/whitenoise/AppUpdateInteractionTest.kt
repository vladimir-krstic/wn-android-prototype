package dev.ipf.whitenoise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import dev.ipf.whitenoise.model.AppSelfUpdatePhase
import dev.ipf.whitenoise.model.AppSelfUpdateScenario
import dev.ipf.whitenoise.model.AppUpdateCheckScenario
import dev.ipf.whitenoise.model.AppUpdateDistribution
import dev.ipf.whitenoise.model.AppUpdates
import dev.ipf.whitenoise.state.AppUpdateController
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import dev.ipf.whitenoise.ui.updates.AppUpdateBanner
import dev.ipf.whitenoise.ui.updates.AppUpdateHost
import dev.ipf.whitenoise.ui.updates.AppUpdateSettingsGroup
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppUpdateInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun normalChatsBannerUpdatesOrDismissesForTheVersion() {
        val controller = AppUpdateController("0.1")
        compose.setContent {
            WhiteNoiseTheme {
                AppUpdateBanner(controller.state, controller::beginSelfUpdate, controller::dismissBanner)
            }
        }
        compose.onNodeWithText("Update available").assertIsDisplayed()
        compose.onNodeWithText("Update now").performClick()
        compose.runOnIdle { assertEquals(AppSelfUpdatePhase.Resolving, controller.state.selfUpdate.phase) }
        compose.runOnIdle { controller.cancel() }
        compose.onNodeWithContentDescription("Dismiss update").performClick()
        compose.onNodeWithTag("appUpdate.banner").assertDoesNotExist()
    }

    @Test
    fun importantBannerHasNoDismissAction() {
        val controller = AppUpdateController("0.1").apply {
            previewCheck(AppUpdateCheckScenario.ImportantAvailable)
        }
        compose.setContent {
            WhiteNoiseTheme {
                AppUpdateBanner(controller.state, {}, {})
            }
        }
        compose.onNodeWithText("Important update available").assertIsDisplayed()
        compose.onNodeWithText("3 releases behind").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dismiss update").assertDoesNotExist()
    }

    @Test
    fun storeManagedBuildHasNoSettingsRowOrChatsBanner() {
        val controller = AppUpdateController("0.1").apply {
            selectDistribution(AppUpdateDistribution.StoreManaged)
        }
        compose.setContent {
            WhiteNoiseTheme {
                AppUpdateSettingsGroup(controller.state, {})
                AppUpdateBanner(controller.state, {}, {})
            }
        }
        compose.onNodeWithText("App updates").assertDoesNotExist()
        compose.onNodeWithTag("appUpdate.banner").assertDoesNotExist()
    }

    @Test
    fun settingsRowShowsCurrentFailureAndAvailableCopy() {
        val controller = AppUpdateController("0.1")
        compose.setContent { WhiteNoiseTheme { AppUpdateSettingsGroup(controller.state, {}) } }
        compose.onNodeWithText("Installed: 0.1 · Latest: 0.2 · 1 release behind").assertIsDisplayed()
        compose.runOnIdle { controller.previewCheck(AppUpdateCheckScenario.Current) }
        compose.onNodeWithText("Installed: 0.1 · Up to date").assertIsDisplayed()
        compose.runOnIdle { controller.previewCheck(AppUpdateCheckScenario.Failure) }
        compose.onNodeWithText("Couldn’t check for updates. Tap to retry.").assertIsDisplayed()
    }

    @Test
    fun downloadAndVerificationHaveDistinctProgressStates() {
        compose.mainClock.autoAdvance = false
        val controller = AppUpdateController("0.1")
        controller.beginSelfUpdate()
        controller.advanceSelfUpdate(controller.state.selfUpdate.generation)
        controller.confirmDownload()
        compose.setContent { WhiteNoiseTheme { AppUpdateHost(controller) } }
        compose.onNodeWithText("Downloading update").assertIsDisplayed()
        compose.runOnIdle {
            val generation = controller.state.selfUpdate.generation
            controller.advanceSelfUpdate(generation)
            controller.advanceSelfUpdate(generation)
        }
        compose.onNodeWithText("Verifying update").assertIsDisplayed()
    }

    @Test
    fun verifiedUpdateOffersInstallAndCancel() {
        compose.mainClock.autoAdvance = false
        val controller = readyController(AppSelfUpdateScenario.Success)
        compose.setContent { WhiteNoiseTheme { AppUpdateHost(controller) } }
        compose.onNodeWithText("Ready to install").assertIsDisplayed()
        compose.onNodeWithText("Install").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(AppSelfUpdatePhase.Idle, controller.state.selfUpdate.phase) }
    }

    @Test
    fun verifiedPermissionStateOffersAndroidSettingsReview() {
        compose.mainClock.autoAdvance = false
        val controller = readyController(AppSelfUpdateScenario.PermissionRequired)
        compose.setContent { WhiteNoiseTheme { AppUpdateHost(controller) } }
        compose.onNodeWithText("Allow installs from this app").assertIsDisplayed()
        compose.onNodeWithText("Open settings").performClick()
        compose.runOnIdle { assertEquals(AppSelfUpdatePhase.Ready, controller.state.selfUpdate.phase) }
    }

    @Test
    fun largeTypeKeepsImportantUpdateActionReachable() {
        val controller = AppUpdateController("0.1").apply {
            previewCheck(AppUpdateCheckScenario.ImportantAvailable)
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                WhiteNoiseTheme { AppUpdateBanner(controller.state, {}, {}) }
            }
        }
        compose.onNodeWithText("Update now").assertIsDisplayed().assertHasClickAction()
    }

    private fun readyController(scenario: AppSelfUpdateScenario): AppUpdateController =
        AppUpdateController("0.1").apply {
            selectSelfUpdateScenario(scenario)
            beginSelfUpdate()
            val generation = state.selfUpdate.generation
            advanceSelfUpdate(generation)
            confirmDownload()
            advanceSelfUpdate(generation)
            advanceSelfUpdate(generation)
            advanceSelfUpdate(generation)
        }
}
