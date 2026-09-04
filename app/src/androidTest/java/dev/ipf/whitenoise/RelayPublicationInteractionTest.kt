package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.Text
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.RelayPublicationController
import dev.ipf.whitenoise.ui.settings.ProfileRelaysScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RelayPublicationInteractionTest {
    @get:Rule val compose = createComposeRule()
    private var profile by mutableStateOf(ProfileFixtures.marmota.copy(developerTools = DeveloperToolsState(isEnabled = true)))
    private val controller = RelayPublicationController({ listOf(profile) }, { profile.id }, { true })
    private fun show(onBack: () -> Unit = {}, largeType: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, if (largeType) 2f else density.fontScale)) {
                WhiteNoiseTheme { ProfileRelaysScreen(profile, onBack, {}, { _, _ -> true }, { true }, { true },
                    publication = controller, publicationSurface = "route") }
            }
        }
    }
    @Test fun publishedListStateIsSeparateFromDisconnectedRelayRows() {
        profile = profile.copy(settings = profile.settings.copy(relays = profile.settings.relays.map { it.copy(status = RelayConnectionStatus.Disconnected) }))
        show()
        compose.onNodeWithTag("relay.publication.group").assertIsDisplayed()
        compose.onAllNodesWithText("Published").assertCountEquals(2)
        compose.onAllNodes(hasStateDescription("Disconnected")).onFirst().assertIsDisplayed()
    }
    @Test fun refreshExposesPendingAndBothMissingResults() {
        controller.chooseScenario(RelayPublicationScenario.MissingBoth); show()
        compose.onNodeWithTag("relay.publication.refresh").performClick()
        compose.onNodeWithText("Refreshing…").assertIsDisplayed()
        compose.waitUntil { controller.work?.phase == RelayPublicationWorkPhase.Complete }
        compose.onAllNodesWithText("Missing").assertCountEquals(2)
        compose.onNodeWithTag("relay.publication.publish").performScrollTo().assertIsDisplayed()
    }
    @Test fun failureKeepsStatusAndRetryPublishesFreshResult() {
        controller.chooseScenario(RelayPublicationScenario.Failure); show()
        compose.onNodeWithTag("relay.publication.refresh").performClick()
        compose.waitUntil { controller.work?.phase == RelayPublicationWorkPhase.Failed }
        compose.onNodeWithText("Couldn’t refresh relay lists. Your last known status is shown.").assertIsDisplayed()
        compose.onNodeWithTag("relay.publication.retry").performClick()
        compose.waitUntil { controller.work?.phase == RelayPublicationWorkPhase.Complete }
        compose.onAllNodesWithText("Published").assertCountEquals(2)
    }
    @Test fun unavailableResultHasTextAndRecoveryAction() {
        controller.chooseScenario(RelayPublicationScenario.Unavailable); show()
        compose.onNodeWithTag("relay.publication.refresh").performClick()
        compose.waitUntil { controller.work?.phase == RelayPublicationWorkPhase.Unavailable }
        compose.onAllNodesWithText("Status unavailable").assertCountEquals(2)
        compose.onNodeWithTag("relay.publication.retry").assertIsDisplayed()
    }
    @Test fun leavingRouteCancelsPendingRefreshBeforeCompletion() {
        var visible by mutableStateOf(true)
        compose.setContent { WhiteNoiseTheme { if (visible) ProfileRelaysScreen(profile, { visible = false }, {}, { _, _ -> true }, { true }, { true },
            publication = controller, publicationSurface = "route") else Text("Settings") } }
        compose.onNodeWithTag("relay.publication.refresh").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertNull(controller.work) }
        compose.waitForIdle(); compose.onNodeWithText("Settings").assertIsDisplayed()
    }
    @Test fun largeTypeCanReachPublicationAndEndpointControls() {
        controller.open(profile.id, "setup"); controller.chooseScenario(RelayPublicationScenario.MissingInbox)
        controller.begin(RelayPublicationOperation.Refresh); controller.complete(controller.work!!.id); controller.close(profile.id, "setup")
        show(largeType = true)
        compose.onNodeWithTag("relay.publication.publish").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add Relay").performScrollTo().assertIsDisplayed()
    }
    @Test fun invalidImportedAddressIsVisibleAndKeepsItsRoleAssignment() {
        profile = profile.copy(settings = profile.settings.copy(relays = profile.settings.relays + ProfileRelay(
            "imported-invalid", "Imported relay", "https://legacy.example", RelayConnectionStatus.Disconnected,
            roles = RelayRole.entries.toSet())))
        show()
        compose.onNodeWithTag("relays.imported.issue").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Imported relay").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(RelayRole.entries.toSet(), profile.settings.relays.last().roles) }
    }
}
