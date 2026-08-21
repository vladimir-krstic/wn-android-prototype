package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.settings.AppearanceScreen
import dev.ipf.whitenoise.ui.settings.DataUsageScreen
import dev.ipf.whitenoise.ui.settings.DonateScreen
import dev.ipf.whitenoise.ui.settings.NotificationsScreen
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
        composeRule.onNodeWithText("Add Profile").assertIsDisplayed()
        composeRule.onNodeWithText("Profile Keys").assertIsDisplayed()
        listOf(
            "profile" to null,
            "profile_keys" to "View, copy, and export your keys",
            "manage_profiles" to "Remove another stored profile",
            "notifications" to "Local and native push preferences",
            "appearance" to null,
            "privacy_security" to "Device protection and auto-lock",
            "data_usage" to "Downloads and sent-media quality",
            "relays" to null,
            "support" to "A unique local support conversation",
            "donate" to "Lightning or Bitcoin",
            "developer_tools" to "Development and testing only",
            "sign_out" to "End this profile’s session",
        ).forEach { (iconTag, removedDescription) ->
            composeRule.onNodeWithTag("settings.list").performScrollToNode(
                hasTestTag("settings.icon.$iconTag"),
            )
            composeRule.onNodeWithTag("settings.icon.$iconTag").assertIsDisplayed()
            removedDescription?.let {
                composeRule.onNodeWithText(it).assertIsNotDisplayed()
            }
            if (iconTag == "appearance") {
                composeRule.onNodeWithText("System default").assertIsDisplayed()
            }
        }
    }

    @Test
    fun settingsOwnsProfileSwitchingAndAddProfileEntry() {
        val profiles = listOf(ProfileFixtures.marmota, ProfileFixtures.pebble)
        var selectedProfileId: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                SettingsScreen(
                    uiState = AppUiState(
                        profiles = profiles,
                        activeProfileId = ProfileFixtures.MARMOTA_ID,
                        signedInProfileIds = profiles.mapTo(mutableSetOf()) { it.id },
                    ),
                    onBack = {},
                    onSelectProfile = { selectedProfileId = it },
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

        composeRule.onNodeWithText("Switch Profile").performClick()
        composeRule.onNodeWithText("Add Profile").assertIsDisplayed()
        composeRule.onNodeWithText("Pebble").performClick()
        composeRule.runOnIdle { check(selectedProfileId == ProfileFixtures.PEBBLE_ID) }
    }

    @Test
    fun shareConnectShowsNativeScannerAndAndroidShareActions() {
        composeRule.setContent {
            WhiteNoiseTheme { ShareConnectScreen(ProfileFixtures.marmota, onBack = {}) }
        }
        composeRule.onNodeWithText("Scan profile code").assertIsDisplayed()
        composeRule.onNodeWithText("Share with Android").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profile QR code").assertIsDisplayed()
    }

    @Test
    fun profileKeysKeepPrivateValueHiddenAndExposeDocumentExports() {
        composeRule.setContent {
            WhiteNoiseTheme { ProfileKeysScreen(ProfileFixtures.marmota, onBack = {}) }
        }
        composeRule.onNodeWithText("Show private key").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Private key hidden").assertIsDisplayed()
        composeRule.onNodeWithText("Export Private Key").assertIsDisplayed()
        composeRule.onNodeWithText("Export Encrypted Private Key").assertIsDisplayed()
    }

    @Test
    fun appearanceAndDataUsageUsePlatformChoiceRows() {
        var selectedAppearance: AppearancePreference? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                AppearanceScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = { selectedAppearance = it.appearance },
                )
            }
        }
        composeRule.onNodeWithText("System default").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.runOnIdle { check(selectedAppearance == AppearancePreference.Dark) }

        composeRule.setContent {
            WhiteNoiseTheme { DataUsageScreen(ProfileFixtures.marmota, {}, {}) }
        }
        composeRule.onNodeWithText("Automatic downloads").assertIsDisplayed()
        composeRule.onNodeWithText("Reset download settings").assertIsDisplayed()
    }

    @Test
    fun notificationDependenciesExplainWhyDependentControlsAreUnavailable() {
        val profile = ProfileFixtures.marmota.copy(
            settings = ProfileFixtures.marmota.settings.copy(
                localNotifications = false,
                nativePushNotifications = false,
            ),
        )
        composeRule.setContent {
            WhiteNoiseTheme { NotificationsScreen(profile, onBack = {}, onChange = {}) }
        }

        composeRule.onNodeWithText("Turn on local notifications first.").assertIsDisplayed()
        composeRule.onNodeWithText("Local notifications are off").assertIsDisplayed()
        composeRule.onNodeWithText("Open Android notification settings").assertIsDisplayed()
    }

    @Test
    fun relayListAndDetailsExposeAvailabilityRolesAndRecovery() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme { ProfileRelaysScreen(profile, {}, {}, { _, _ -> true }, { true }, { true }) }
        }
        composeRule.onNodeWithText("Primal").assertIsDisplayed()
        composeRule.onNodeWithText("Restore default relays").assertIsNotEnabled()

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
        composeRule.onNodeWithContentDescription("Lightning donation QR code").assertIsDisplayed()
    }

    @Test
    fun supportSurfaceExplainsPurposeBeforeOpeningTheUniqueChat() {
        composeRule.setContent {
            WhiteNoiseTheme { SupportScreen(ProfileFixtures.marmota, {}, {}, {}) }
        }
        composeRule.onNodeWithText("Questions, problems, and suggestions").assertIsDisplayed()
        composeRule.onNodeWithText("Start Chat").assertIsDisplayed()
    }

    @Test
    fun supportExplainsRelayRecoveryAndDisablesPrimaryActionWhenUnavailable() {
        val profile = ProfileFixtures.pebble.copy(chatRelayUrls = emptyList())
        composeRule.setContent {
            WhiteNoiseTheme { SupportScreen(profile, {}, {}, {}) }
        }

        composeRule.onNodeWithText("Profile relays need attention").assertIsDisplayed()
        composeRule.onNodeWithText("Open Relays").assertIsDisplayed()
        composeRule.onNodeWithText("Start Chat").assertIsNotEnabled()
    }
}
