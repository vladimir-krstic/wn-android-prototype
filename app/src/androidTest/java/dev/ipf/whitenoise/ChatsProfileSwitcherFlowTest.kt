package dev.ipf.whitenoise

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import androidx.navigation.compose.rememberNavController
import dev.ipf.whitenoise.navigation.AppRoute
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.navigation.WhiteNoiseNavHost
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatsProfileSwitcherFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun singleAccountAvatarOpensSettingsAndReusesAddProfileFlow() {
        val vm = AppViewModel().apply {
            completeSignIn(OnboardingOrigin.Initial)
            dismissDiagnosticsPrompt(uiState.activeProfileId!!)
            setQuickAccountSwitching(true)
        }
        val originalId = vm.uiState.activeProfileId
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        compose.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        compose.onNodeWithTag("chats.quickSwitch").assertDoesNotExist()
        compose.onNodeWithTag("chats.switchProfile").performClick()
        compose.onNodeWithTag("profile_switcher.settings").assertDoesNotExist()
        compose.runOnIdle { assertTrue(nav.currentBackStackEntry!!.destination.hasRoute<AppRoute.Settings>()) }
        compose.onNodeWithTag("settings.add_profile").performClick()
        compose.runOnIdle {
            assertEquals(OnboardingOrigin.AddProfile, nav.currentBackStackEntry!!.toRoute<AppRoute.Welcome>().origin)
            nav.popBackStack()
            assertEquals(originalId, vm.uiState.activeProfileId)
            assertTrue(nav.currentBackStackEntry!!.destination.hasRoute<AppRoute.Settings>())
        }
    }

    @Test fun appearanceOptInCyclesAccountsWithoutOpeningSheetAndStaysEnabled() {
        val vm = AppViewModel().apply {
            completeSignIn(OnboardingOrigin.Initial)
            completeSignIn(OnboardingOrigin.AddProfile)
            dismissDiagnosticsPrompt(uiState.activeProfileId!!)
        }
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        compose.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        compose.onNodeWithTag("chats.quickSwitch").assertDoesNotExist()
        compose.runOnIdle { nav.navigate(AppRoute.Appearance) }
        compose.onNodeWithText("Quick account switching").performClick()
        compose.runOnIdle { assertTrue(vm.uiState.quickAccountSwitching); nav.popBackStack() }
        repeat(vm.uiState.signedInProfiles.size + 1) {
            val next = vm.uiState.nextQuickSwitchProfile!!
            compose.onNodeWithContentDescription("Switch to ${next.name}").performClick()
            compose.runOnIdle { assertEquals(next.id, vm.uiState.activeProfileId) }
            compose.onNodeWithTag("profile_switcher.settings").assertDoesNotExist()
        }
        compose.onNodeWithTag("chats.switchProfile").performClick()
        compose.onNodeWithTag("profile_switcher.settings").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.runOnIdle { nav.navigate(AppRoute.Appearance) }
        compose.onNodeWithText("Quick account switching").performClick()
        compose.runOnIdle { nav.popBackStack() }
        compose.onNodeWithTag("chats.quickSwitch").assertDoesNotExist()
    }

    @Test
    fun selectingProfileClosesSwitcherAndStaysOnChats() {
        val vm = AppViewModel().apply {
            completeSignIn(OnboardingOrigin.Initial)
            completeSignIn(OnboardingOrigin.AddProfile)
            dismissDiagnosticsPrompt(uiState.activeProfileId!!)
        }
        val originalId = vm.uiState.activeProfileId!!
        val alternateId = vm.uiState.signedInProfiles.first { it.id != originalId }.id
        val originalChats = vm.uiState.activeProfile!!.chats
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        compose.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        compose.onNodeWithTag("chats.switchProfile").performClick()
        compose.onNodeWithTag("profile_switcher.profile.$originalId").assertIsSelected()
        compose.onNodeWithTag("profile_switcher.profile.$alternateId").performClick()
        compose.onNodeWithTag("profile_switcher.settings").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(alternateId, vm.uiState.activeProfileId)
            assertTrue(nav.currentBackStackEntry!!.destination.hasRoute<AppRoute.SignedIn>())
        }
        compose.onNodeWithTag("chats.switchProfile").performClick()
        compose.onNodeWithTag("profile_switcher.profile.$alternateId").assertIsSelected()
        compose.onNodeWithTag("profile_switcher.profile.$originalId").performClick()
        compose.runOnIdle { assertEquals(originalChats, vm.uiState.activeProfile!!.chats) }
    }

    @Test
    fun settingsAndAddProfileNavigateFromTheMainSwitcher() {
        val vm = AppViewModel().apply {
            completeSignIn(OnboardingOrigin.Initial)
            completeSignIn(OnboardingOrigin.AddProfile)
            dismissDiagnosticsPrompt(uiState.activeProfileId!!)
        }
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        compose.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        compose.onNodeWithTag("chats.switchProfile").performClick()
        compose.onNodeWithTag("profile_switcher.settings").performClick()
        compose.runOnIdle {
            assertTrue(nav.currentBackStackEntry!!.destination.hasRoute<AppRoute.Settings>())
            nav.popBackStack()
        }
        compose.onNodeWithTag("profile_switcher.settings").assertDoesNotExist()
        compose.onNodeWithTag("chats.switchProfile").performClick()
        compose.onNodeWithTag("profile_switcher.add_profile").performClick()
        compose.runOnIdle {
            assertEquals(OnboardingOrigin.AddProfile, nav.currentBackStackEntry!!.toRoute<AppRoute.Welcome>().origin)
        }
    }
    @Test
    fun signingOutWithOtherProfilesReturnsToTheMainSwitcher() {
        val vm = AppViewModel().apply {
            completeSignIn(OnboardingOrigin.Initial)
            completeSignIn(OnboardingOrigin.AddProfile)
            dismissDiagnosticsPrompt(uiState.activeProfileId!!)
        }
        val departingId = vm.uiState.activeProfileId
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        compose.runOnIdle { nav.navigate(AppRoute.Settings()) }
        compose.onNodeWithTag("settings.list").performScrollToNode(hasText("Sign Out"))
        compose.onNodeWithText("Sign Out").performClick()
        compose.onNodeWithContentDescription("Sign Out").performClick()
        compose.waitUntil(10_000) { vm.uiState.activeProfileId != departingId }
        compose.onNodeWithTag("profile_switcher.settings").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(nav.currentBackStackEntry!!.destination.hasRoute<AppRoute.SignedIn>())
        }
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithTag("chats.switchProfile").assertIsDisplayed()
        compose.onNodeWithTag("profile_switcher.settings").assertDoesNotExist()
    }

}
