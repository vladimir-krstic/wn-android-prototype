package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
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
    fun settingsHubConsolidatesProfilePreferencesAndSupportHierarchy() {
        val profile = ProfileFixtures.marmota
        var openedShareConnect = false
        var openedAddProfile = false
        composeRule.setContent {
            WhiteNoiseTheme {
                SettingsScreen(
                    uiState = AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                    onBack = {},
                    onSelectProfile = {},
                    onAddProfile = { openedAddProfile = true },
                    onShareConnect = { openedShareConnect = true },
                    onEditProfile = {},
                    onProfileKeys = {},
                    onNotifications = {},
                    onAppearance = {},
                    onPrivacy = {},
                    onDataUsage = {},
                    onRelays = {},
                    onSupport = {},
                    onDonate = {},
                    onDeveloperTools = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.active_profile").performClick()
        composeRule.runOnIdle { check(openedShareConnect) }
        composeRule.onNodeWithTag("settings.profile.divider").assertIsDisplayed()
        composeRule.onNodeWithText("Add Profile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.profile_management").performClick()
        composeRule.runOnIdle { check(openedAddProfile) }
        composeRule.onNodeWithText("Profile Keys").assertIsDisplayed()
        listOf(
            "profile" to null,
            "profile_keys" to "View, copy, and export your keys",
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
        }
        composeRule.onNodeWithText("Preferences").assertDoesNotExist()
        composeRule.onNodeWithText("Help").assertDoesNotExist()
        composeRule.onNodeWithText("Manage Profiles").assertDoesNotExist()
        composeRule.onNodeWithText("System default").assertDoesNotExist()
        composeRule.onNodeWithTag("settings.list").performScrollToNode(
            hasTestTag("settings.destinations.divider.0"),
        )
        composeRule.onNodeWithTag("settings.destinations.divider.0").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.list").performScrollToNode(
            hasTestTag("settings.version_footer"),
        )
        composeRule.onNodeWithText("Version 0.1").assertIsDisplayed()
        composeRule.onNodeWithText("White Noise for Android", substring = true).assertDoesNotExist()
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
                    onDeveloperTools = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings.profile_management").performClick()
        composeRule.onNodeWithTag("settings.profile.add_profile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.profile.alternate.${ProfileFixtures.PEBBLE_ID}").performClick()
        composeRule.runOnIdle { check(selectedProfileId == ProfileFixtures.PEBBLE_ID) }
    }

    @Test
    fun dismissingProfileSwitcherLeavesSelectionUntouched() {
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
                    onDeveloperTools = {},
                    onSignOut = {},
                    initiallyShowSwitcher = true,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Current profile").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.onNodeWithTag("profile_switcher.add_profile").assertDoesNotExist()
        composeRule.runOnIdle { check(selectedProfileId == null) }
    }

    @Test
    fun backCollapsesInlineProfilesBeforeLeavingSettings() {
        val profiles = listOf(ProfileFixtures.marmota, ProfileFixtures.pebble)
        var navigatedBack = false
        composeRule.setContent {
            WhiteNoiseTheme {
                SettingsScreen(
                    uiState = AppUiState(
                        profiles = profiles,
                        activeProfileId = ProfileFixtures.MARMOTA_ID,
                        signedInProfileIds = profiles.mapTo(mutableSetOf()) { it.id },
                    ),
                    onBack = { navigatedBack = true },
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
                    onDeveloperTools = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings.profile_management").performClick()
        composeRule.onNodeWithTag("settings.profile.alternate.${ProfileFixtures.PEBBLE_ID}")
            .assertIsDisplayed()
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("settings.profile.alternate.${ProfileFixtures.PEBBLE_ID}")
            .assertDoesNotExist()
        composeRule.runOnIdle { check(!navigatedBack) }
    }

    @Test
    fun settingsShowsStackedSwitcherPreviewAndCapsInactiveUnread() {
        val highUnread = ProfileFixtures.pebble.copy(
            chats = listOf(
                ProfileFixtures.marmota.chats.first().copy(unreadCount = 120),
            ),
        )
        val profiles = listOf(
            ProfileFixtures.marmota,
            highUnread,
            *ProfileFixtures.showcaseProfiles.toTypedArray(),
        )
        composeRule.setContent {
            WhiteNoiseTheme {
                SettingsScreen(
                    uiState = AppUiState(
                        profiles = profiles,
                        activeProfileId = ProfileFixtures.MARMOTA_ID,
                        signedInProfileIds = profiles.mapTo(mutableSetOf()) { it.id },
                    ),
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
                    onDeveloperTools = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithText("Switch Profile").assertIsDisplayed()
        composeRule.onNodeWithTag(
            "settings.profile.preview_remaining",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(32.dp)
            .assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithText("Switch Profile").performClick()
        composeRule.onNodeWithText("99+").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.profile.add_profile").assertIsDisplayed()
    }

    @Test
    fun shareConnectUsesOneStableIdentityPageWithExplicitScannerAction() {
        composeRule.setContent {
            WhiteNoiseTheme { ShareConnectScreen(ProfileFixtures.marmota, onBack = {}) }
        }
        composeRule.onNodeWithText("Share & Connect").assertIsDisplayed()
        composeRule.onNodeWithTag("share_connect.mode").assertDoesNotExist()
        composeRule.onNodeWithTag("share_connect.share").assertIsDisplayed()
        composeRule.onNodeWithText("Marmota").assertIsDisplayed()
        composeRule.onNodeWithText("Scan to connect.").assertIsDisplayed()
        composeRule.onNodeWithText("Scan QR Code").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Verified").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profile QR code").assertIsDisplayed()
        composeRule.onNodeWithTag("share_connect.copy_public_key.visual")
            .assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithTag("share_connect.copy_public_key")
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("share_connect.copy_public_key").performClick()
        composeRule.onNodeWithContentDescription("Copied").assertIsDisplayed()
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
