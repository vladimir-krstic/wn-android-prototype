package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.settings.ChatDownloadScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ChatDownloadInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var profile by mutableStateOf(Profile("me", "Me", "key", chats = listOf(Chat("chat", 0, ChatKind.Group, "Trip"))))
    private fun edit(transform: (Chat) -> Chat) { profile = profile.copy(chats = listOf(transform(profile.chats.single()))) }

    @Composable private fun Screen() {
        ChatDownloadScreen(profile, profile.chats.single(), {},
            onInheritance = { type, inherit -> edit { it.copy(downloadOverrides = it.downloadOverrides.inherit(type, inherit, profile.settings.downloadMatrix)) } },
            onNetwork = { type, network, enabled -> edit { it.copy(downloadOverrides = it.downloadOverrides.change(type, network, enabled, profile.settings.downloadMatrix)) } },
            onReset = { edit { it.copy(downloadOverrides = ChatDownloadOverrides()) } },
        )
    }

    @Test fun inheritedRulesAreVisibleAndCustomNeverCanResetWithoutChangingProfile() {
        val defaults = profile.settings
        rule.setContent { WhiteNoiseTheme { Screen() } }
        rule.onNodeWithTag("chat.downloads.reset").assertIsNotEnabled()
        rule.onNodeWithTag("chat.downloads.Photos").performClick()
        rule.onNodeWithTag("chat.downloads.inherit").assertIsOn()
        rule.onNodeWithTag("download.network.Wifi").assertIsOn().assertIsNotEnabled()
        rule.onNodeWithTag("chat.downloads.inherit").performClick()
        rule.onNodeWithTag("download.network.Wifi").assertIsEnabled().performClick()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithTag("chat.downloads.Photos").assertTextContains("Custom: Never")
        rule.onNodeWithTag("chat.downloads.Audio").assertTextContains("Profile default: Wi-Fi")
        rule.onNodeWithTag("chat.downloads.reset").performClick().assertIsNotEnabled()
        rule.runOnIdle {
            assertEquals(defaults, profile.settings)
            assertTrue(profile.chats.single().downloadOverrides.media.isEmpty())
        }
    }

    @Test fun pickerRestoresForItsOwnerAndInheritedRulesUpdateLive() {
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { Screen() } }
        rule.onNodeWithTag("chat.downloads.Videos").performClick()
        rule.onNodeWithTag("download.network.Wifi").assertIsOff()
        rule.runOnIdle { profile = profile.copy(settings = profile.settings.copy(downloadMatrix =
            profile.settings.downloadMatrix.change(DownloadMediaType.Videos, DownloadNetwork.Wifi, true))) }
        rule.onNodeWithTag("download.network.Wifi").assertIsOn().assertIsNotEnabled()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("chat.downloads.inherit").assertIsOn().performClick()
        rule.runOnIdle { profile = profile.copy(settings = profile.settings.copy(downloadMatrix = MediaDownloadMatrix(emptySet()))) }
        rule.onNodeWithTag("download.network.Wifi").assertIsOn().assertIsEnabled()
        rule.runOnIdle { profile = profile.copy(id = "other", chats = listOf(profile.chats.single().copy(downloadOverrides = ChatDownloadOverrides()))) }
        rule.onNodeWithTag("download.network.options").assertDoesNotExist()
        rule.onNodeWithTag("chat.downloads.Videos").assertTextContains("Profile default: Never")
    }

    @Test fun darkLargeRtlMutedChatKeepsPauseAndNetworkChoicesAccessible() {
        profile = profile.copy(settings = profile.settings.copy(automaticDownloadsPaused = true),
            chats = listOf(profile.chats.single().copy(muteDuration = MuteDuration.Always)))
        rule.setContent { WhiteNoiseTheme(appearance = AppearancePreference.Dark) {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) { Screen() }
        } }
        rule.onNodeWithText("Automatic downloads are paused").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("This chat is muted. Download settings still apply.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("chat.downloads.Files").performScrollTo().performClick()
        rule.onNodeWithTag("chat.downloads.inherit").performScrollTo().performClick()
        rule.onNodeWithTag("download.network.Metered").performScrollTo().performClick().assertIsOn()
        rule.onNodeWithText("Done").performClick()
    }

    @Test fun chatAndGroupInfoOpenOwnedDownloadsAndBackReturnsToInfo() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
        val owner = vm.uiState.activeProfile!!
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        listOf(owner.chats.first { !it.isGroup }, owner.chats.first { it.isGroup }).forEach { chat ->
            rule.runOnIdle { nav.navigate(AppRoute.ChatInfo(chat.id)) }
            rule.onNodeWithTag("chat.downloads").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(AppRoute.ChatDownloads(owner.id, chat.id), nav.currentBackStackEntry!!.toRoute<AppRoute.ChatDownloads>())
                nav.popBackStack()
            }
            rule.onNodeWithTag("chat.downloads").assertExists()
            rule.runOnIdle { assertEquals(chat.id, nav.currentBackStackEntry!!.toRoute<AppRoute.ChatInfo>().chatId) }
        }
        rule.runOnIdle { nav.navigate(AppRoute.ChatDownloads("wrong-profile", owner.chats.first().id)) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(owner.chats.first { it.isGroup }.id, nav.currentBackStackEntry!!.toRoute<AppRoute.ChatInfo>().chatId) }
    }
}
