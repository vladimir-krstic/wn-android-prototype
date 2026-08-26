package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.settings.DiagnosticsImprovementsScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsPromptHost
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticsPromptTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun scrimAndSwipeDismissalsRecordChoicesWithoutEnablingConsent() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        rule.setContent { WhiteNoiseTheme {
            DiagnosticsPromptHost(vm.uiState, true, { id, enabled -> vm.setAnalyticsEnabled(id, enabled) },
                { id, enabled -> vm.setDiagnosticLoggingEnabled(id, enabled) }, vm::dismissDiagnosticsPrompt)
        } }
        rule.onNodeWithTag("sheet.surface").performTouchInput { click(Offset(centerX, -24f)) }
        rule.waitUntil { vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt }
        rule.runOnIdle {
            assertFalse(vm.uiState.activeProfile!!.diagnostics.analyticsEnabled)
            assertFalse(vm.uiState.activeProfile!!.diagnostics.loggingEnabled)
            vm.completeSignIn(OnboardingOrigin.AddProfile)
        }
        rule.onNodeWithTag("sheet.surface").performTouchInput {
            swipe(Offset(centerX, 20f), Offset(centerX, height.toFloat() - 4f), 300)
        }
        rule.waitUntil { vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt }
        rule.runOnIdle {
            assertFalse(vm.uiState.activeProfile!!.diagnostics.analyticsEnabled)
            assertFalse(vm.uiState.activeProfile!!.diagnostics.loggingEnabled)
        }
    }

    @Test fun signUpWithFocusedEditorShowsPrivacyOnlyAfterChatsNavigation() {
        val vm = AppViewModel()
        lateinit var controller: NavHostController
        rule.setContent {
            val nav = rememberNavController()
            SideEffect { controller = nav }
            WhiteNoiseApp(navController = nav, appViewModel = vm)
        }
        rule.onNodeWithText("Sign Up").performClick()
        rule.onNode(hasSetTextAction().and(hasText("Marmota"))).performClick().assertIsFocused()
        rule.onNode(hasClickAction().and(hasText("Sign Up"))).performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 10_000) { vm.uiState.activeProfile != null }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Help Improve White Noise").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Help Improve White Noise").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(Lifecycle.State.RESUMED, controller.currentBackStackEntry!!.lifecycle.currentState)
            assertFalse(vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt)
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
    }

    @Test fun promptWaitsForResumedChatsAndRestorationDoesNotRecordDismissal() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val resumed = mutableStateOf(false)
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            WhiteNoiseTheme {
                DiagnosticsPromptHost(vm.uiState, resumed.value,
                    { id, enabled -> vm.setAnalyticsEnabled(id, enabled) },
                    { id, enabled -> vm.setDiagnosticLoggingEnabled(id, enabled) },
                    vm::dismissDiagnosticsPrompt)
            }
        }
        rule.onNodeWithText("Help Improve White Noise").assertDoesNotExist()
        rule.runOnIdle { resumed.value = true }
        rule.onNodeWithText("Help Improve White Noise").assertIsDisplayed()
        rule.onNodeWithText("Share Anonymous Analytics").assertIsOff().performClick().assertIsOn()
        rule.onNodeWithText("Share Diagnostic Logs").assertIsOff()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Share Anonymous Analytics").assertIsOn()
        rule.runOnIdle { assertFalse(vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt) }
        rule.onNodeWithContentDescription("Close").performClick()
        rule.waitUntil { vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt }
        rule.onNodeWithTag("diagnostics.prompt").assertDoesNotExist()
    }

    @Test fun backDismissalRecordsAnOffChoiceForOnlyThePresentedProfile() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        rule.setContent {
            WhiteNoiseTheme {
                DiagnosticsPromptHost(vm.uiState, true, { id, enabled -> vm.setAnalyticsEnabled(id, enabled) },
                    { id, enabled -> vm.setDiagnosticLoggingEnabled(id, enabled) }, vm::dismissDiagnosticsPrompt)
            }
        }
        rule.onNodeWithText("Help Improve White Noise").assertIsDisplayed()
        // Deliver Back to the dialog window, not the Activity underneath it.
        rule.onNodeWithTag("diagnostics.prompt").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Back) }
        rule.waitUntil { vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt }
        rule.runOnIdle {
            assertFalse(vm.uiState.activeProfile!!.diagnostics.analyticsEnabled)
            assertFalse(vm.uiState.activeProfile!!.diagnostics.loggingEnabled)
            vm.completeSignIn(OnboardingOrigin.AddProfile)
        }
        rule.onNodeWithText("Help Improve White Noise").assertIsDisplayed()
        rule.runOnIdle { assertFalse(vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt) }
    }

    @Test fun diagnosticsSettingsRemainReachableInLargeTextDarkRtlAndConfirmClearing() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val id = vm.uiState.activeProfileId!!
        vm.setDiagnosticLoggingEnabled(id, true)
        vm.setDiagnosticLoggingEnabled(id, false)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme(AppearancePreference.Dark) {
                    Box(Modifier.requiredSize(400.dp, 700.dp)) {
                        DiagnosticsImprovementsScreen(vm.uiState.activeProfile!!, {},
                            { vm.setAnalyticsEnabled(id, it) }, { vm.setDiagnosticLoggingEnabled(id, it) },
                            { vm.clearDiagnosticRecords(id) })
                    }
                }
            }
        }
        rule.onNodeWithText("Share Anonymous Analytics").performScrollTo().assertIsOff()
        rule.onNodeWithText("Share Diagnostic Logs").performScrollTo().assertIsOff()
        rule.onNodeWithText("Clear Diagnostic Logs").performScrollTo().performClick()
        rule.onNodeWithText("Clear diagnostic logs?").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(32_000L, vm.uiState.activeProfile!!.diagnostics.storedBytes) }
        rule.onNodeWithText("Clear Diagnostic Logs").performClick()
        rule.onNodeWithText("Clear Logs").performClick()
        rule.runOnIdle { assertEquals(0L, vm.uiState.activeProfile!!.diagnostics.storedBytes); assertFalse(vm.uiState.activeProfile!!.diagnostics.loggingEnabled) }
    }
}
