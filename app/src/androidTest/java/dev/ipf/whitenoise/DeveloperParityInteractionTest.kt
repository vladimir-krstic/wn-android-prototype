package dev.ipf.whitenoise

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.DeveloperParityController
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperParityInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var profile by mutableStateOf(ProfileFixtures.marmota.copy(developerTools = DeveloperToolsState(isEnabled = true, debugMode = true)))
    private var now = 1000L
    private val controller = DeveloperParityController({ profile }, { true }, { 1 }, { _, reduce -> profile = reduce(profile); true }, true, { now })
    private fun packages() {
        compose.setContent { WhiteNoiseTheme { KeyPackagesScreen(profile, controller, {}) } }
        compose.waitUntil { controller.work?.phase == DeveloperPhase.Complete }
    }
    @Test fun deletionRequiresConfirmationAndCancelPreservesPublication() {
        packages(); val before = profile
        compose.onNodeWithText("Delete Key Package").performScrollTo().performClick()
        compose.onNodeWithText("Delete Key Package?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(before, profile) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(before, profile) }
    }
    @Test fun confirmedDeleteMovesMaterialOutOfPublishedSection() {
        packages()
        compose.onNodeWithText("Delete Key Package").performScrollTo().performClick()
        compose.onAllNodesWithText("Delete Key Package").onLast().performClick()
        compose.waitUntil { !profile.connectionInformationPublished }
        compose.onNodeWithText("No key package is currently published. Publish one to receive new group invitations.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertTrue(DeveloperInspection.packages(profile).single().local) }
    }
    @Test fun failedPublicationOffersRecoveryWithoutReplacingMaterialEarly() {
        packages(); val before = profile.developerTools.keyPackage
        compose.runOnIdle { controller.chooseOutcome(DeveloperOutcome.Failure) }
        compose.onNodeWithTag("key_packages.publish").performScrollTo().performClick()
        compose.waitUntil { controller.work?.phase == DeveloperPhase.Failed }
        compose.runOnIdle { assertEquals(before,profile.developerTools.keyPackage) }
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.waitUntil { controller.work?.phase == DeveloperPhase.Complete }
        compose.runOnIdle { assertNotEquals(before,profile.developerTools.keyPackage) }
    }
    @Test fun disablingGateRemovesPackageActionsAndCancelsPendingConfirmation() {
        packages()
        compose.onNodeWithText("Delete Key Package").performScrollTo().performClick()
        compose.runOnIdle { profile = profile.copy(developerTools = profile.developerTools.withEnabled(false)) }
        compose.onNodeWithText("Developer Tools is off for this profile.").assertIsDisplayed()
        compose.onNodeWithTag("key_packages.publish").assertDoesNotExist()
        compose.waitUntil { controller.work == null }
    }
    @Test fun healthSheetKeepsTheConsoleAndSelfSendDoesNotAddChats() {
        val chats = profile.chats
        compose.setContent { WhiteNoiseTheme { DiagnosticsScreen(profile, null, {}, { true }, { true }, controller) } }
        compose.waitUntil { controller.work?.phase == DeveloperPhase.Complete }
        compose.onNodeWithTag("diagnostics.actions").performClick()
        compose.onNodeWithTag("diagnostics.action.health").performClick()
        compose.onNodeWithText("Connection attempts").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Send to Self").performScrollTo().performClick()
        compose.waitUntil { profile.developerTools.diagnosticEvents.last().text.contains("temporary chat removed") }
        compose.runOnIdle { assertEquals(chats,profile.chats) }
    }
    @Test fun pushFailureHidesTokenDetailsUntilRefreshRecovers() {
        controller.chooseOutcome(DeveloperOutcome.Failure)
        compose.setContent { WhiteNoiseTheme {
            val chat = profile.chats.first { it.id == "fiatjaf" }
            ConversationDebugScreen(profile, chat, ConversationDebugPolicy.snapshot(profile,chat.id), {}, {}, {}, parityController = controller)
        } }
        compose.waitUntil { controller.work?.phase == DeveloperPhase.Failed }
        compose.onNodeWithText("Total tokens").assertDoesNotExist()
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.waitUntil { controller.work?.phase == DeveloperPhase.Complete }
        compose.onNodeWithText("Total tokens").performScrollTo().assertIsDisplayed()
    }
}
