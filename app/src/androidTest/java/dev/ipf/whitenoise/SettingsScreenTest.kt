package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.LanguagePreference
import dev.ipf.whitenoise.model.NotificationPreviewMode
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.settings.AppearanceScreen
import dev.ipf.whitenoise.ui.settings.DataUsageScreen
import dev.ipf.whitenoise.ui.settings.DonateScreen
import dev.ipf.whitenoise.ui.settings.EditProfileScreen
import dev.ipf.whitenoise.ui.settings.LanguageScreen
import dev.ipf.whitenoise.ui.settings.NotificationsScreen
import dev.ipf.whitenoise.ui.settings.NotificationPermissionStatus
import dev.ipf.whitenoise.ui.settings.ProfileKeysScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelayDetailsScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelaysScreen
import dev.ipf.whitenoise.ui.settings.PrivacySecurityScreen
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
            .assertWidthIsEqualTo(240.dp)
            .assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithText(ProfileFixtures.marmota.publicKey).assertIsDisplayed()
        composeRule.onNodeWithTag("share_connect.copy_public_key")
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("share_connect.copy_public_key").performClick()
        composeRule.onNodeWithContentDescription("Copied").assertIsDisplayed()
    }

    @Test
    fun profileUsesExplicitReadAndEditModesWithInlineVerification() {
        var savedName: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                EditProfileScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onSave = { name, _, _ ->
                        savedName = name
                        true
                    },
                    onSaveAddress = { true },
                )
            }
        }

        composeRule.onNodeWithText("Edit").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertDoesNotExist()
        composeRule.onNodeWithText("Change photo").assertDoesNotExist()
        composeRule.onNodeWithText("Verified address").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Verified").assertIsDisplayed()
        composeRule.onNodeWithTag("profile.name_field").assertIsEnabled()
        composeRule.onNodeWithTag("profile.address_field").assertIsEnabled()
        composeRule.onNodeWithTag("profile.about_field").assertIsEnabled()

        composeRule.onNodeWithText("Edit").performClick()
        composeRule.onNodeWithTag("profile.name_field").assertIsEnabled()
        composeRule.onNodeWithTag("profile.address_field").assertIsEnabled()
        composeRule.onNodeWithTag("profile.about_field").assertIsEnabled()
        composeRule.onNodeWithText("Change photo").assertIsDisplayed()
        composeRule.onNodeWithTag("profile.name_field").performTextReplacement("Marmota Updated")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle { check(savedName == "Marmota Updated") }
        composeRule.onNodeWithText("Edit").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun profileKeysKeepPrivateValueHiddenAndExposeDocumentExports() {
        composeRule.setContent {
            WhiteNoiseTheme { ProfileKeysScreen(ProfileFixtures.marmota, onBack = {}) }
        }
        composeRule.onNodeWithContentDescription("Show private key").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Private key hidden").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Copy public key").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_keys.public_key_value")
            .assertTextEquals(ProfileFixtures.marmota.publicKey)
        composeRule.onNodeWithText("Keep this key private. Anyone with it can use your profile, and White Noise can’t recover it.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Export Private Key").assertIsDisplayed()
        composeRule.onNodeWithText("Export Encrypted Private Key").assertIsDisplayed()
        composeRule.onNodeWithText("Exports use Android’s document picker. Keep exported key files private.")
            .assertDoesNotExist()

        composeRule.onNodeWithText("Export Encrypted Private Key").performClick()
        composeRule.onNodeWithText("Encrypted Private Key").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_keys.export_password").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_keys.export_confirmation").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_keys.password_strength").assertDoesNotExist()
        composeRule.onNodeWithText("Use a long, unique password. You’ll need it to open the encrypted file.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Export").assertIsNotEnabled()
        composeRule.onNodeWithTag("profile_keys.export_password").performTextInput("safe-password")
        composeRule.onNodeWithTag("profile_keys.password_strength").assertIsDisplayed()
        composeRule.onNodeWithText("Fair").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_keys.export_confirmation").performTextInput("safe-password")
        composeRule.onNodeWithText("Export").assertIsEnabled()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Export Private Key").performClick()
        composeRule.onNodeWithText("Keep Your Private Key Safe").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Store this file somewhere secure. The encrypted export or a trusted password manager is safer.",
        ).assertIsDisplayed()
    }

    @Test
    fun appearanceAndDataUsageUsePlatformChoiceRows() {
        var selectedAppearance: AppearancePreference? = null
        var openedLanguage = false
        composeRule.setContent {
            WhiteNoiseTheme {
                AppearanceScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = { selectedAppearance = it.appearance },
                    onLanguage = { openedLanguage = true },
                )
            }
        }
        composeRule.onNodeWithTag("appearance.theme.group").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.runOnIdle { check(selectedAppearance == AppearancePreference.Dark) }
        composeRule.onNodeWithText("Language").performClick()
        composeRule.runOnIdle { check(openedLanguage) }

        composeRule.setContent {
            WhiteNoiseTheme { DataUsageScreen(ProfileFixtures.marmota, {}, {}) }
        }
        composeRule.onNodeWithText("Automatic downloads").assertIsDisplayed()
        composeRule.onNodeWithText("Reset download settings").assertIsDisplayed()
    }

    @Test
    fun languageChoicesUseASeparateImmediateRadioDestination() {
        var selectedLanguage: LanguagePreference? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                LanguageScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = { selectedLanguage = it.language },
                )
            }
        }

        composeRule.onNodeWithTag("language.choices.group").assertIsDisplayed()
        composeRule.onNodeWithText("System default").assertIsSelected()
        composeRule.onNodeWithText("Spanish").performClick()
        composeRule.runOnIdle { check(selectedLanguage == LanguagePreference.Spanish) }
        composeRule.onNodeWithTag("settings.list").performScrollToNode(hasText("Serbian"))
        composeRule.onNodeWithText("Serbian").assertExists().assertIsDisplayed()
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
            WhiteNoiseTheme {
                NotificationsScreen(
                    profile = profile,
                    onBack = {},
                    onChange = {},
                    permissionStatusOverride = NotificationPermissionStatus.Allowed,
                )
            }
        }

        composeRule.onNodeWithText("Turn on local notifications first.").assertIsDisplayed()
        composeRule.onNodeWithText("Turn on local notifications to change message previews.").assertIsDisplayed()
        composeRule.onNodeWithText("Sender and message").assertIsNotEnabled()
        composeRule.onNodeWithText("Notifications are off").assertDoesNotExist()
    }

    @Test
    fun notificationPreviewChoicesAreInlineAndApplyImmediately() {
        var updatedSettings: ProfileSettings? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                NotificationsScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = { updatedSettings = it },
                    permissionStatusOverride = NotificationPermissionStatus.Allowed,
                )
            }
        }

        composeRule.onNodeWithTag("notifications.delivery.divider").assertIsDisplayed()
        composeRule.onNodeWithText("Sender and message").assertIsDisplayed()
        composeRule.onNodeWithText("Sender only").assertIsDisplayed()
        composeRule.onNodeWithText("New message only").assertIsSelected()
        composeRule.onNodeWithText("White Noise · New message").assertIsDisplayed()
        composeRule.onNodeWithText("Message preview").assertDoesNotExist()

        composeRule.onNodeWithText("Sender only").performClick()
        composeRule.runOnIdle {
            check(updatedSettings?.notificationPreviewMode == NotificationPreviewMode.SenderOnly)
        }
    }

    @Test
    fun notificationPermissionGateUsesFirstRequestAndBlockedRecoveryStates() {
        composeRule.setContent {
            WhiteNoiseTheme {
                NotificationsScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = {},
                    permissionStatusOverride = NotificationPermissionStatus.NotRequested,
                )
            }
        }

        composeRule.onNodeWithText("Allow notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Notifications are off").assertDoesNotExist()
        composeRule.onNodeWithText("Local notifications").assertIsNotEnabled()
        composeRule.onNodeWithText("Allow notifications first.").assertIsDisplayed()
        composeRule.onNodeWithText("Android").assertDoesNotExist()

        composeRule.setContent {
            WhiteNoiseTheme {
                NotificationsScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = {},
                    permissionStatusOverride = NotificationPermissionStatus.Blocked,
                )
            }
        }

        composeRule.onNodeWithText("Notifications are off").assertIsDisplayed()
        composeRule.onNodeWithText("Turn them on in Android Settings to use these options.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Allow notifications").assertDoesNotExist()
        composeRule.onNodeWithText("Local notifications").assertIsNotEnabled()
    }

    @Test
    fun privacySecurityRevealsAutoLockOnlyAfterAuthenticationAndKeepsConsequencesOutsideActions() {
        var profile by mutableStateOf(
            ProfileFixtures.marmota.copy(
                settings = ProfileFixtures.marmota.settings.copy(
                    requireDeviceAuthentication = false,
                ),
            ),
        )
        composeRule.setContent {
            WhiteNoiseTheme {
                PrivacySecurityScreen(
                    profile = profile,
                    allProfileIds = listOf(profile.id),
                    onBack = {},
                    onChange = { profile = profile.copy(settings = it) },
                    onEraseAppData = {},
                    onDiagnosticsImprovements = {},
                    deviceSecureOverride = true,
                )
            }
        }

        composeRule.onNodeWithTag("privacy.device_protection.group").assertIsDisplayed()
        composeRule.onNodeWithTag("privacy.device_protection.divider.recents").assertIsDisplayed()
        composeRule.onNodeWithText("Auto-lock").assertDoesNotExist()
        composeRule.onNodeWithText("Require device authentication").performClick()
        composeRule.onNodeWithText("Auto-lock").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("choice_dialog.options").assertIsDisplayed()
        composeRule.onNodeWithText("Immediately").assertIsSelected()
        composeRule.onNodeWithText("After 5 minutes").performClick()
        composeRule.runOnIdle {
            check(profile.settings.autoLockDuration.label == "After 5 minutes")
        }

        composeRule.onNodeWithText("Control optional analytics and diagnostic logs for this profile.")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Signs out every profile and permanently removes all White Noise data from this device.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Security preferences are stored separately for each profile.")
            .assertDoesNotExist()
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
