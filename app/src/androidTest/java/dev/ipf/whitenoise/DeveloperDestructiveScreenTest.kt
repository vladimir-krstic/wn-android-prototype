package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.ui.settings.ConversationDebugScreen
import dev.ipf.whitenoise.ui.settings.DeveloperToolsScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsScreen
import dev.ipf.whitenoise.ui.settings.EraseAppDataSheet
import dev.ipf.whitenoise.ui.settings.KeyPackagesScreen
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
        composeRule.onNodeWithContentDescription("Live event stream").assertIsDisplayed()
        composeRule.onNodeWithText("18:42:10  runtime started").assertIsDisplayed()
    }

    @Test
    fun diagnosticsEmptyStateAndKeyPackageTaskRemainExplicit() {
        val profile = ProfileFixtures.marmota.copy(
            developerTools = ProfileFixtures.marmota.developerTools.copy(
                isEnabled = true,
                diagnosticEvents = emptyList(),
            ),
        )
        composeRule.setContent {
            WhiteNoiseTheme { DiagnosticsScreen(profile, null, {}, { true }, { true }) }
        }
        composeRule.onNodeWithText("No Events").assertIsDisplayed()
        composeRule.onNodeWithText("Clear events").assertIsNotEnabled()

        composeRule.setContent {
            WhiteNoiseTheme { KeyPackagesScreen(profile, {}, { true }) }
        }
        composeRule.onNodeWithText("Publish New Key Package").assertIsDisplayed()
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
    fun signOutWipeRequiresTheExactProfileName() {
        composeRule.setContent {
            WhiteNoiseTheme { SignOutSheet(ProfileFixtures.marmota, {}, {}) }
        }

        composeRule.onNodeWithContentDescription("Sign Out and Wipe Data").assertIsNotEnabled()
        composeRule.onNodeWithText("Profile name").performTextInput("Marmota")
        composeRule.onNodeWithContentDescription("Sign Out and Wipe Data").assertIsEnabled()
    }

    @Test
    fun eraseAppDataRequiresTheGeneratedPhrase() {
        val profileIds = listOf("marmota", "pebble")
        val phrase = WipeConfirmationPhrase.make(profileIds)
        composeRule.setContent {
            WhiteNoiseTheme { EraseAppDataSheet(profileIds, {}, {}) }
        }

        composeRule.onNodeWithContentDescription("Erase App Data").assertIsNotEnabled()
        composeRule.onNodeWithText("Confirmation phrase").performTextInput(phrase)
        composeRule.onNodeWithContentDescription("Erase App Data").assertIsEnabled()
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
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.onNodeWithText("Remove Profile").assertIsNotEnabled()
        composeRule.onNodeWithText("Profile name").performTextInput("Pebble")
        composeRule.onNodeWithText("Remove Profile").assertIsEnabled()
    }
}
