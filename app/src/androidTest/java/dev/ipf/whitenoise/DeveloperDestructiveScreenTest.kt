package dev.ipf.whitenoise

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperDestructiveScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun lockedDeveloperToolsShowsWarningAndMasterGate() {
        composeRule.setContent {
            WhiteNoiseTheme {
                DeveloperToolsScreen(ProfileFixtures.marmota, {}, { true }, { true }, {}, {})
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
            ),
        )
        composeRule.setContent {
            WhiteNoiseTheme {
                DeveloperToolsScreen(profile, {}, { true }, { true }, {}, {})
            }
        }
        composeRule.onNodeWithText("Debug Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Diagnostics").assertIsDisplayed()
        composeRule.onNodeWithText("Diagnostic Logging").assertIsDisplayed()
        composeRule.onNodeWithText("Off").assertIsDisplayed()
        composeRule.onNodeWithText("There are no logs.").assertIsDisplayed()
        composeRule.onNodeWithText("Export Diagnostic Logs").assertDoesNotExist()
        composeRule.onNodeWithText("Diagnostics & Improvements").assertDoesNotExist()
        composeRule.onNodeWithText(
            "Configure or clear diagnostic logs in Privacy & Security. " +
                "Existing sanitized files remain available here after logging is turned off.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun developerDiagnosticInventoryRetainsFilesWithoutOwningLoggingControls() {
        val diagnostics = ProfileFixtures.marmota.diagnostics
            .withLogging(
                enabled = true,
                profileId = ProfileFixtures.marmota.id,
                profileName = ProfileFixtures.marmota.name,
            )
            .copy(loggingEnabled = false)
        val profile = ProfileFixtures.marmota.copy(
            diagnostics = diagnostics,
            developerTools = ProfileFixtures.marmota.developerTools.copy(isEnabled = true),
        )

        composeRule.setContent {
            WhiteNoiseTheme {
                DeveloperToolsScreen(profile, {}, { true }, { true }, {}, {})
            }
        }

        composeRule.onNodeWithText("Diagnostic Logging").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(diagnostics.records.first().filename).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export Diagnostic Logs").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("There are no logs.").assertDoesNotExist()
        composeRule.onNodeWithText("Clear Diagnostic Logs").assertDoesNotExist()
        composeRule.onNodeWithText("Share Diagnostic Logs").assertDoesNotExist()
    }

    @Test
    fun diagnosticsKeepsOneConsoleAndVisibleLiveState() {
        val profile = ProfileFixtures.marmota.copy(
            developerTools = ProfileFixtures.marmota.developerTools.copy(isEnabled = true),
        )
        var tested = false
        var cleared = false
        composeRule.setContent {
            WhiteNoiseTheme {
                DiagnosticsScreen(
                    profile = profile,
                    diagnosticSummary = "Sanitized summary",
                    onBack = {},
                    onTest = { tested = true; true },
                    onClear = { cleared = true; true },
                )
            }
        }
        composeRule.onNodeWithText("Events").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Live event stream").assertIsDisplayed()
        composeRule.onNodeWithTag("diagnostics.live_indicator").assertIsDisplayed()
        composeRule.onNodeWithText("18:42:10  runtime started").assertIsDisplayed()
        composeRule.onNodeWithText("Test").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Diagnostic actions").performClick()
        composeRule.onNodeWithText("Copy Diagnostic Summary").assertIsDisplayed()
        composeRule.onNodeWithText("Test").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(tested) }
        composeRule.onNodeWithContentDescription("Diagnostic actions").performClick()
        composeRule.onNodeWithText("Clear Events").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(cleared) }
    }

    @Test
    fun diagnosticsHeaderSharesTheConsoleContentLine() {
        val profile = ProfileFixtures.marmota.copy(
            developerTools = ProfileFixtures.marmota.developerTools.copy(isEnabled = true),
        )
        composeRule.setContent {
            WhiteNoiseTheme {
                DiagnosticsScreen(profile, null, {}, { true }, { true })
            }
        }

        val title = composeRule.onNodeWithTag("diagnostics.events_title")
            .fetchSemanticsNode().boundsInRoot
        val live = composeRule.onNodeWithTag("diagnostics.live_indicator")
            .fetchSemanticsNode().boundsInRoot
        val firstEvent = composeRule.onNodeWithTag("diagnostics.event.0")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(firstEvent.left, title.left, 1f)
        assertEquals(firstEvent.right, live.right, 1f)
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
        composeRule.onNodeWithContentDescription("Diagnostic actions").performClick()
        composeRule.onNodeWithText("Clear Events").assertIsNotEnabled()

        var published = false
        composeRule.setContent {
            WhiteNoiseTheme {
                KeyPackagesScreen(profile, {}, { published = true; true })
            }
        }
        composeRule.onNodeWithTag("key_packages.publish").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(published) }
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
        composeRule.onNodeWithText(
            "This profile and all local data will be permanently removed. Previous chats won’t return.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Type “Marmota” to confirm permanent removal of this profile and its local data.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("sign_out.profile.divider").assertHeightIsEqualTo(2.dp)
        val signOutRoot = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val signOutSheet = composeRule.onNodeWithTag("sheet.surface").fetchSemanticsNode().boundsInRoot
        assertTrue(signOutSheet.top <= signOutRoot.height * 0.1f)

        composeRule.setContent {
            WhiteNoiseTheme { EraseAppDataSheet(listOf("marmota", "pebble"), {}, {}) }
        }
        composeRule.onNodeWithText("This can’t be undone").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmation phrase").assertIsDisplayed()
        composeRule.onNodeWithTag("erase.warning").assertIsDisplayed()
        composeRule.onNodeWithTag("erase.phrase").assertIsDisplayed()
        composeRule.onNodeWithTag("erase.confirmation").assertIsDisplayed()
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val eraseSheet = composeRule.onNodeWithTag("sheet.surface").fetchSemanticsNode().boundsInRoot
        assertTrue(eraseSheet.top <= root.height * 0.1f)
    }

    @Test
    fun signOutWithoutWipeKeepsLocalDataAndRemovesConfirmationGate() {
        composeRule.setContent {
            WhiteNoiseTheme { SignOutSheet(ProfileFixtures.marmota, {}, {}) }
        }

        composeRule.onNodeWithText("Wipe Data From This Device").performClick()
        composeRule.onNodeWithText(
            "This profile and its local data will stay on this device.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Enter Profile Name").assertDoesNotExist()
        composeRule.onNodeWithText("Profile name").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Sign Out").assertIsEnabled()
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
