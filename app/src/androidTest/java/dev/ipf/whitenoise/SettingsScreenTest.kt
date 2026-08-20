package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.settings.AppearanceScreen
import dev.ipf.whitenoise.ui.settings.DataUsageScreen
import dev.ipf.whitenoise.ui.settings.DonateScreen
import dev.ipf.whitenoise.ui.settings.ProfileKeysScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelayDetailsScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelaysScreen
import dev.ipf.whitenoise.ui.settings.SettingsScreen
import dev.ipf.whitenoise.ui.settings.ShareConnectScreen
import dev.ipf.whitenoise.ui.settings.SupportScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsHubShowsProfilePreferencesAndSupportHierarchy() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme {
                SettingsScreen(
                    uiState = AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                    onBack = {},
                    onSelectProfile = {},
                    onAddProfile = {},
                    onShareConnect = {},
                    onEditProfile = {},
                    onProfileKeys = {},
                    onNotifications = {},
                    onAppearance = {},
                    onPrivacy = {},
                    onDataUsage = {},
                    onRelays = {},
                    onSupport = {},
                    onDonate = {},
                    onManageProfiles = {},
                    onDeveloperTools = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Share & Connect").assertIsDisplayed()
        composeRule.onNodeWithText("Profile Keys").assertIsDisplayed()
    }

    @Test
    fun shareConnectShowsNativeScannerAndAndroidShareActions() {
        composeRule.setContent {
            WhiteNoiseTheme { ShareConnectScreen(ProfileFixtures.marmota, onBack = {}) }
        }
        composeRule.onNodeWithText("Scan profile code").assertIsDisplayed()
        composeRule.onNodeWithText("Share with Android").assertIsDisplayed()
    }

    @Test
    fun profileKeysKeepPrivateValueHiddenAndExposeDocumentExports() {
        composeRule.setContent {
            WhiteNoiseTheme { ProfileKeysScreen(ProfileFixtures.marmota, onBack = {}) }
        }
        composeRule.onNodeWithText("Show private key").assertIsDisplayed()
        composeRule.onNodeWithText("Export Private Key").assertIsDisplayed()
        composeRule.onNodeWithText("Export Encrypted Private Key").assertIsDisplayed()
    }

    @Test
    fun appearanceAndDataUsageUsePlatformChoiceRows() {
        composeRule.setContent {
            WhiteNoiseTheme { AppearanceScreen(ProfileFixtures.marmota, {}, {}) }
        }
        composeRule.onNodeWithText("System default").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").assertIsDisplayed()

        composeRule.setContent {
            WhiteNoiseTheme { DataUsageScreen(ProfileFixtures.marmota, {}, {}) }
        }
        composeRule.onNodeWithText("Automatic downloads").assertIsDisplayed()
        composeRule.onNodeWithText("Reset download settings").assertIsDisplayed()
    }

    @Test
    fun relayListAndDetailsExposeAvailabilityRolesAndRecovery() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme { ProfileRelaysScreen(profile, {}, {}, { _, _ -> true }, { true }, { true }) }
        }
        composeRule.onNodeWithText("Primal").assertIsDisplayed()
        composeRule.onNodeWithText("Restore default relays").assertIsDisplayed()

        composeRule.setContent {
            WhiteNoiseTheme {
                ProfileRelayDetailsScreen(profile.settings.relays.first(), {}, { _, _ -> true }, { true })
            }
        }
        composeRule.onNodeWithText("Chat Messages").assertIsDisplayed()
        composeRule.onNodeWithText("Status").assertIsDisplayed()
    }

    @Test
    fun donationSurfaceIsExplicitlyOfflineAndCopyOnly() {
        composeRule.setContent { WhiteNoiseTheme { DonateScreen(onBack = {}) } }
        composeRule.onNodeWithText("free and open source", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Copy Lightning address").assertIsDisplayed()
    }

    @Test
    fun supportSurfaceExplainsPurposeBeforeOpeningTheUniqueChat() {
        composeRule.setContent {
            WhiteNoiseTheme { SupportScreen(ProfileFixtures.marmota, {}, {}, {}) }
        }
        composeRule.onNodeWithText("Questions, problems, and suggestions").assertIsDisplayed()
        composeRule.onNodeWithText("Start Chat").assertIsDisplayed()
    }
}
