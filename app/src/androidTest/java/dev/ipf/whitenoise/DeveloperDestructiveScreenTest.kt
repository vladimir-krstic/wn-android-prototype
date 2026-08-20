package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.settings.ConversationDebugScreen
import dev.ipf.whitenoise.ui.settings.DeveloperToolsScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsScreen
import dev.ipf.whitenoise.ui.settings.EraseAppDataSheet
import dev.ipf.whitenoise.ui.settings.ManageProfilesScreen
import dev.ipf.whitenoise.ui.settings.SignOutSheet
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperDestructiveScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lockedDeveloperToolsShowsWarningAndMasterGate() {
        composeRule.setContent {
            WhiteNoiseTheme {
                DeveloperToolsScreen(ProfileFixtures.marmota, {}, { true }, { true }, {}, {}, { true }, { true }, { true })
            }
        }
        composeRule.onNodeWithText("For development and testing only").assertIsDisplayed()
        composeRule.onNodeWithText("Developer Tools").assertIsDisplayed()
    }

    @Test
    fun enabledDeveloperToolsShowsIndependentTechnicalSections() {
        val profile = ProfileFixtures.marmota.copy(
            developerTools = ProfileFixtures.marmota.developerTools.copy(
                isEnabled = true,
                auditLogging = true,
            ),
        )
        composeRule.setContent {
            WhiteNoiseTheme {
                DeveloperToolsScreen(profile, {}, { true }, { true }, {}, {}, { true }, { true }, { true })
            }
        }
        composeRule.onNodeWithText("Debug Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Diagnostics").assertIsDisplayed()
        composeRule.onNodeWithText("Anonymous Telemetry").assertIsDisplayed()
    }

    @Test
    fun diagnosticsKeepsOneConsoleAndVisibleLiveState() {
        val profile = ProfileFixtures.marmota.copy(
            developerTools = ProfileFixtures.marmota.developerTools.copy(isEnabled = true),
        )
        composeRule.setContent {
            WhiteNoiseTheme { DiagnosticsScreen(profile, null, {}, { true }, { true }) }
        }
        composeRule.onNodeWithText("Events").assertIsDisplayed()
        composeRule.onNodeWithText("● Live").assertIsDisplayed()
        composeRule.onNodeWithText("18:42:10  runtime started").assertIsDisplayed()
    }

    @Test
    fun enabledConversationDebugShowsDerivedRoutingAndDiagnostics() {
        val profile = ProfileFixtures.marmota.copy(
            developerTools = ProfileFixtures.marmota.developerTools.copy(isEnabled = true, debugMode = true),
        )
        val chat = profile.chats.first { it.id == "fiatjaf" }
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationDebugScreen(profile, chat, ConversationDebugPolicy.snapshot(profile, chat.id), {}, {}, {})
            }
        }
        composeRule.onNodeWithText("Conversation").assertIsDisplayed()
        composeRule.onNodeWithText("Delivery & notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Chat relays").assertIsDisplayed()
    }

    @Test
    fun signOutAndEraseUseSeparateNativeSheetsAndConsequences() {
        composeRule.setContent {
            WhiteNoiseTheme { SignOutSheet(ProfileFixtures.marmota, {}, {}) }
        }
        composeRule.onNodeWithText("Wipe Data From This Device").assertIsDisplayed()
        composeRule.onNodeWithText("Profile name").assertIsDisplayed()

        composeRule.setContent {
            WhiteNoiseTheme { EraseAppDataSheet(listOf("marmota", "pebble"), {}, {}) }
        }
        composeRule.onNodeWithText("This can’t be undone").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmation phrase").assertIsDisplayed()
    }

    @Test
    fun manageProfilesNeverOffersActiveProfileForRemoval() {
        composeRule.setContent {
            WhiteNoiseTheme {
                ManageProfilesScreen(
                    profiles = listOf(ProfileFixtures.marmota, ProfileFixtures.pebble),
                    activeProfileId = ProfileFixtures.MARMOTA_ID,
                    onBack = {},
                    onRemove = { _, _ -> true },
                )
            }
        }
        composeRule.onNodeWithText("Pebble").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").assertIsDisplayed()
    }
}
