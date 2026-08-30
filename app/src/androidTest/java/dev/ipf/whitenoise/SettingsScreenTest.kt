package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.LanguagePreference
import dev.ipf.whitenoise.model.MediaDownloadPolicy
import dev.ipf.whitenoise.model.NotificationPreviewMode
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.SentMediaQuality
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
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

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
            Triple("profile", "Profile", null),
            Triple("profile_keys", "Profile Keys", "View, copy, and export your keys"),
            Triple("notifications", "Notifications", "Local and native push preferences"),
            Triple("appearance", "Appearance", null),
            Triple("privacy_security", "Privacy & Security", "Device protection and auto-lock"),
            Triple("data_usage", "Data Usage", "Downloads and sent-media quality"),
            Triple("relays", "Relays", null),
            Triple("support", "Support", "A unique local support conversation"),
            Triple("donate", "Donate", "Lightning or Bitcoin"),
            Triple("developer_tools", "Developer Tools", "Development and testing only"),
            Triple("sign_out", "Sign Out", "End this profile’s session"),
        ).forEach { (iconTag, title, removedDescription) ->
            composeRule.onNodeWithTag("settings.list").performScrollToNode(
                hasText(title),
            )
            composeRule.onNodeWithTag("settings.icon.$iconTag", useUnmergedTree = true)
                .assertIsDisplayed()
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
        composeRule.onNodeWithTag("share_connect.copy_public_key.visual", useUnmergedTree = true)
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
        composeRule.onNodeWithText("Change photo").performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithTag("profile_keys.export_password")
            .performScrollTo()
        composeRule.onAllNodesWithText("Export")[1].assertIsNotEnabled()
        composeRule.onNodeWithTag("profile_keys.export_password").performTextInput("safe-password")
        composeRule.onNodeWithTag("profile_keys.password_strength").assertIsDisplayed()
        composeRule.onNodeWithText("Fair").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_keys.export_confirmation").performTextInput("safe-password")
        composeRule.onAllNodesWithText("Export")[1].assertIsEnabled()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Export Private Key").performClick()
        composeRule.onNodeWithText("Keep Your Private Key Safe").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Store this file somewhere secure. The encrypted export or a trusted password manager is safer.",
        ).assertIsDisplayed()
    }

    @Test
    fun appearanceUsesImmediateThemeChoicesAndLanguageNavigation() {
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
    }

    @Test
    fun dataUsageUsesSeparatedGroupsImmediateDialogsAndDefaultAwareReset() {
        var changedSettings: ProfileSettings? = null
        var profile by mutableStateOf(ProfileFixtures.marmota)
        composeRule.setContent {
            WhiteNoiseTheme {
                DataUsageScreen(
                    profile = profile,
                    onBack = {},
                    onChange = { changedSettings = it },
                )
            }
        }

        composeRule.onNodeWithText("Automatic downloads").assertIsDisplayed()
        composeRule.onNodeWithTag("data_usage.downloads.group").assertIsDisplayed()
        composeRule.onNodeWithTag("data_usage.downloads.divider.photos").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Media that isn't downloaded automatically shows a download button.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Reset download settings").assertIsNotEnabled()

        composeRule.onNodeWithText("Photos").performClick()
        composeRule.onNodeWithTag("choice_dialog.options").assertIsDisplayed()
        composeRule.onNodeWithTag("choice_dialog.option.1").assertIsSelected()
        composeRule.onNodeWithTag("choice_dialog.option.2").performClick()
        composeRule.runOnIdle {
            check(changedSettings?.photoDownloadPolicy == MediaDownloadPolicy.WifiAndCellular)
        }

        composeRule.onNodeWithTag("settings.list").performScrollToNode(
            hasText("Photo and video quality"),
        )
        composeRule.onNodeWithText("Photo and video quality").performClick()
        composeRule.onNodeWithTag("choice_dialog.option.0").assertIsSelected()
        composeRule.onNodeWithText(
            "High sends uncompressed photos and videos for better quality, but uses more data. " +
                "Standard compresses media to use less data.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("choice_dialog.option.1").performClick()
        composeRule.runOnIdle {
            check(changedSettings?.sentMediaQuality == SentMediaQuality.High)
        }

        val customizedProfile = ProfileFixtures.marmota.copy(
            settings = ProfileFixtures.marmota.settings.copy(
                fileDownloadPolicy = MediaDownloadPolicy.WifiAndCellular,
            ),
        )
        composeRule.runOnIdle { profile = customizedProfile }
        composeRule.onNodeWithTag("settings.list").performScrollToNode(
            hasText("Reset download settings"),
        )
        composeRule.onNodeWithText("Reset download settings").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            check(changedSettings?.fileDownloadPolicy == ProfileSettings().fileDownloadPolicy)
        }
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
        var permissionStatus by mutableStateOf(NotificationPermissionStatus.NotRequested)
        composeRule.setContent {
            WhiteNoiseTheme {
                NotificationsScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onChange = {},
                    permissionStatusOverride = permissionStatus,
                )
            }
        }

        composeRule.onNodeWithText("Allow notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Notifications are off").assertDoesNotExist()
        composeRule.onNodeWithText("Local notifications").assertIsNotEnabled()
        composeRule.onNodeWithText("Allow notifications first.").assertIsDisplayed()
        composeRule.onNodeWithText("Android").assertDoesNotExist()

        composeRule.runOnIdle { permissionStatus = NotificationPermissionStatus.Blocked }

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
        composeRule.onNodeWithTag("choice_dialog.option.0").assertIsSelected()
        composeRule.onNodeWithText("After 5 minutes").performClick()
        composeRule.runOnIdle {
            check(profile.settings.autoLockDuration.label == "After 5 minutes")
        }

        composeRule.onNodeWithText("Diagnostics & Improvements").assertIsDisplayed()
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
        var showDetails by mutableStateOf(false)
        composeRule.setContent {
            WhiteNoiseTheme {
                if (showDetails) {
                    ProfileRelayDetailsScreen(
                        profile.settings.relays.first(),
                        {},
                        { _, _ -> true },
                        { true },
                    )
                } else {
                    ProfileRelaysScreen(profile, {}, {}, { _, _ -> true }, { true }, { true })
                }
            }
        }
        composeRule.onNodeWithText("Primal").assertIsDisplayed()
        composeRule.onNodeWithTag("relays.row.primal").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Connected"),
        )
        composeRule.onNodeWithText("Add Relay").assertIsDisplayed()
        check(
            composeRule.onAllNodesWithTag(
                "relay.status.connected.filled",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty(),
        )
        check(
            composeRule.onAllNodesWithTag(
                "relay.status.not_connected.filled",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty(),
        )
        val density = composeRule.activity.resources.displayMetrics.density
        val statusWidth = composeRule.onAllNodesWithTag(
            "relay.status.connected.filled",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().first().boundsInRoot.width
        check(kotlin.math.abs(statusWidth - 20f * density) < 1f)
        composeRule.onNodeWithTag("relays.restore").assertIsNotEnabled()
        composeRule.onNodeWithTag("relays.divider.0").assertIsDisplayed()
        val sectionLeft = composeRule.onNodeWithText(
            "Profile relays",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.left
        val helperLeft = composeRule.onNodeWithText(
            "Relays let your profile publish information, receive chat invitations, and deliver messages.",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.left
        check(kotlin.math.abs(sectionLeft - helperLeft) < 1f)
        val groupBottom = composeRule.onNodeWithTag(
            "relays.group",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.bottom
        val helperTop = composeRule.onNodeWithText(
            "Relays let your profile publish information, receive chat invitations, and deliver messages.",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.top
        check(kotlin.math.abs((helperTop - groupBottom) - 8f * density) < 1f)

        composeRule.runOnIdle { showDetails = true }
        composeRule.onNodeWithText("Relay").assertIsDisplayed()
        composeRule.onNodeWithText("Use For").assertIsDisplayed()
        composeRule.onNodeWithText("Chat Messages").assertIsDisplayed()
        composeRule.onNodeWithText("Status").assertIsDisplayed()
        composeRule.onNodeWithText("Use for messages in chats you create.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("relay.details.role.divider.0").assertIsDisplayed()
        composeRule.onNodeWithText("Remove Relay").performClick()
        composeRule.onNodeWithText("Remove Primal?").assertIsDisplayed()
    }

    @Test
    fun relayDetailKeepsLongUrlOnOneLineWithCompleteSemantics() {
        val relay = ProfileFixtures.marmota.settings.relays.first {
            it.name == "White Noise Profile"
        }
        composeRule.setContent {
            WhiteNoiseTheme {
                ProfileRelayDetailsScreen(relay, {}, { _, _ -> true }, { true })
            }
        }

        composeRule.onNodeWithText(relay.url, useUnmergedTree = true).assertIsDisplayed()
        val density = composeRule.activity.resources.displayMetrics.density
        val urlRowHeight = composeRule.onNodeWithTag(
            "relay.metadata.url",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.height
        check(kotlin.math.abs(urlRowHeight - 56f * density) < 1f)
    }

    @Test
    fun addRelayUsesAValidatedTaskSheetAndRestoreKeepsItsFocusedConfirmation() {
        var profile by mutableStateOf(ProfileFixtures.marmota)
        var addedUrl: String? = null
        var addedRoles: Set<RelayRole>? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                ProfileRelaysScreen(
                    profile = profile,
                    onBack = {},
                    onRelay = {},
                    onAdd = { url, roles ->
                        addedUrl = url
                        addedRoles = roles
                        true
                    },
                    onConnected = { true },
                    onRestore = { true },
                )
            }
        }

        composeRule.onNodeWithText("Add Relay").performClick()
        composeRule.onNodeWithTag("sheet.surface").assertIsDisplayed()
        composeRule.onNodeWithText("Relay URL").assertIsDisplayed()
        composeRule.onNodeWithText("Use For").assertIsDisplayed()
        composeRule.onNodeWithText("Receive invitations to new chats and groups.").assertIsDisplayed()
        composeRule.onNodeWithTag("relay.add.submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("relay.add.url").performTextInput("wss://relay.example.com")
        composeRule.onNodeWithTag("relay.add.submit").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            check(addedUrl == "wss://relay.example.com")
            check(addedRoles == RelayRole.entries.toSet())
        }

        val customized = ProfileFixtures.marmota.copy(
            settings = ProfileFixtures.marmota.settings.copy(
                relays = ProfileRelayFixtures.defaults.dropLast(1),
            ),
        )
        composeRule.runOnIdle { profile = customized }
        composeRule.onNodeWithTag("settings.list").performScrollToNode(
            hasText("Restore Default Relays"),
        )
        composeRule.onNodeWithText("Restore Default Relays").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Restore default relays?").assertIsDisplayed()
        composeRule.onNodeWithText("Custom relays will be removed.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Restore Defaults").assertIsDisplayed()
    }

    @Test
    fun donationSurfaceIsExplicitlyOfflineAndCopyOnly() {
        var showDonation by mutableStateOf(false)
        composeRule.setContent {
            WhiteNoiseTheme {
                if (showDonation) {
                    DonateScreen(onBack = {})
                } else {
                    ShareConnectScreen(ProfileFixtures.marmota, onBack = {})
                }
            }
        }
        val establishedQrBounds = composeRule.onNodeWithTag(
            "share_connect.qr_surface",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        composeRule.runOnIdle { showDonation = true }
        composeRule.onNodeWithTag("donate.method_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("donate.method.0").assertIsOn()
        composeRule.onNodeWithTag("donate.method.1").assertIsOff()
        composeRule.onNodeWithText("free and open source", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("donate.copy_address.visual", useUnmergedTree = true)
            .assertWidthIsEqualTo(240.dp)
            .assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithTag("donate.copy_address").assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("donate.method_caption").assertHeightIsEqualTo(20.dp)
        composeRule.onNodeWithContentDescription("Copy Lightning address").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Lightning address copied").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Lightning donation QR code").assertIsDisplayed()
        val donationQrBounds = composeRule.onNodeWithTag(
            "donate.qr_surface",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val donationAddressBounds = composeRule.onNodeWithTag("donate.copy_address")
            .fetchSemanticsNode().boundsInRoot
        val donationAddressVisualBounds = composeRule.onNodeWithTag(
            "donate.copy_address.visual",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val donationCaptionBounds = composeRule.onNodeWithTag("donate.method_caption")
            .fetchSemanticsNode().boundsInRoot
        val targetGap = with(composeRule.density) { 1.dp.toPx() }
        val captionOpticalGap = with(composeRule.density) { 5.dp.toPx() }
        val qrToAddressTargetGap = donationAddressBounds.top - donationQrBounds.bottom
        val addressVisualToCaptionGap = donationCaptionBounds.top - donationAddressVisualBounds.bottom
        check(kotlin.math.abs(establishedQrBounds.width - donationQrBounds.width) < 1f)
        check(kotlin.math.abs(establishedQrBounds.height - donationQrBounds.height) < 1f)
        check(kotlin.math.abs(qrToAddressTargetGap - targetGap) < 1f)
        check(kotlin.math.abs(addressVisualToCaptionGap - captionOpticalGap) < 1f)

        composeRule.onNodeWithTag("donate.method.1").performClick().assertIsOn()
        composeRule.onNodeWithTag("donate.method.0").assertIsOff()
        composeRule.onNodeWithContentDescription("Bitcoin donation QR code").assertIsDisplayed()
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
