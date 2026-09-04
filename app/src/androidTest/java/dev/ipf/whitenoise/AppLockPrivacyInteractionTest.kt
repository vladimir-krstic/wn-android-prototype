package dev.ipf.whitenoise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockPrivacyInteractionTest {
    @get:Rule val rule=createAndroidComposeRule<EmptyTestActivity>()
    private val profile=ProfileFixtures.showcaseProfiles.first().copy(settings=ProfileSettings(requireDeviceAuthentication=true),developerTools=DeveloperToolsState(isEnabled=true))
    private fun lock()=AppLockController({profile},{true}).apply { credentials(true) }
    @Test fun lockHasNoProfileDataAndCancelCannotBypassIt() {
        val controller=lock(); controller.requestUnlock()
        rule.setContent { WhiteNoiseTheme { AppLockScreen(controller) } }
        rule.onNodeWithText("White Noise is locked").assertExists(); rule.onNodeWithText(profile.name).assertDoesNotExist()
        rule.onNodeWithText("Cancel").performClick(); rule.onNodeWithTag("app.unlock").assertExists()
        rule.runOnIdle { assertTrue(controller.blocked) }
    }
    @Test fun failedUnlockOffersRetryOnTheSameLockSurface() {
        val controller=lock(); controller.complete(controller.requestUnlock()!!,AppUnlockOutcome.Failed)
        rule.setContent { WhiteNoiseTheme { AppLockScreen(controller) } }
        rule.onNodeWithText("Your identity could not be confirmed. Try again.").assertExists()
        rule.onNodeWithTag("app.unlock").performClick(); rule.onNodeWithText("Unlocking…").assertExists()
        rule.runOnIdle { assertTrue(controller.blocked); assertEquals(AppLockPhase.Authenticating,controller.phase) }
    }
    @Test fun protectedContentKeepsStateButLosesPlacementSemanticsAndResumedLifecycle() {
        var hidden by mutableStateOf(false)
        var lifecycle: Lifecycle?=null
        var mounts=0
        rule.setContent { WhiteNoiseTheme {
            ProtectedAppContent(hidden,{Text("Locked")}) {
                lifecycle=LocalLifecycleOwner.current.lifecycle
                val input=remember { mounts++; TextFieldState("Private draft") }
                WhiteNoiseTextField(state=input,modifier=Modifier.testTag("private.input"))
            }
        } }
        rule.onNodeWithTag("private.input").performTextInput(" retained")
        rule.runOnIdle { hidden=true }
        rule.onNodeWithTag("private.input").assertDoesNotExist(); rule.onNodeWithText("Locked").assertExists()
        rule.runOnIdle { assertEquals(Lifecycle.State.CREATED,lifecycle!!.currentState); hidden=false }
        rule.onNodeWithText("Private draft retained").assertExists(); rule.runOnIdle { assertEquals(1,mounts) }
    }
    @Test fun backDuringAuthenticationCancelsOnlyTheAttempt() {
        val controller=lock(); controller.requestUnlock(); var left=false
        rule.setContent { WhiteNoiseTheme { AppLockScreen(controller,{left=true}) } }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertTrue(controller.blocked); assertNull(controller.request); assertFalse(left) }
    }
    @Test fun keyboardPreferenceShowsRequestLimitation() {
        var p by mutableStateOf(profile)
        rule.setContent { WhiteNoiseTheme { PrivacySecurityScreen(p,listOf(p.id),{}, {p=p.copy(settings=it)}, {},deviceSecureOverride=true) } }
        rule.onNodeWithText("Incognito keyboard").performScrollTo().assertExists()
        rule.onNodeWithText("Ask your keyboard not to learn from what you type in White Noise. Your keyboard may ignore this request.").assertExists()
    }
    @Test fun recentsAndScreenshotControlsRemainIndependent() {
        var p by mutableStateOf(profile.copy(settings = ProfileSettings()))
        rule.setContent { WhiteNoiseTheme { PrivacySecurityScreen(p,listOf(p.id),{}, {p=p.copy(settings=it)}, {},deviceSecureOverride=true) } }
        rule.onNodeWithText("Hide Screen in Recents").performClick()
        rule.onNodeWithText("Block screenshots in chats").performScrollTo().assertIsOff().performClick().assertIsOn()
        rule.runOnIdle {
            assertTrue(p.settings.hideScreenInRecents)
            assertTrue(p.settings.blockScreenshotsInChats)
        }
    }
    @Test fun auditRecordingRequiresExplicitSensitiveConsent() {
        val controller=AuditLogController({listOf(profile)},{profile.id},{true},{false})
        rule.setContent { WhiteNoiseTheme { AuditLogsScreen(controller,{}) } }
        rule.onNodeWithText("Record audit logs").performClick()
        rule.onNodeWithText("Record sensitive audit logs?").assertExists()
        rule.runOnIdle { assertFalse(controller.state.enabled) }
        rule.onNodeWithTag("audit.confirm").performClick()
        rule.runOnIdle { controller.work!!.let { controller.advance(it.id,it.attempt) }; assertTrue(controller.state.enabled) }
    }
    @Test fun sensitiveExportConsentDoesNotUseSanitizedReassurance() {
        val controller=AuditLogController({listOf(profile)},{profile.id},{true},{false})
        controller.begin(AuditLogAction.Export)
        rule.setContent { WhiteNoiseTheme { AuditLogsScreen(controller,{}) } }
        rule.onNodeWithText("This export may contain message content, identities and device details from profiles on this device. Save it somewhere private and share it only with someone you trust.").assertExists()
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertNull(controller.work) }
    }
    @Test fun partialAuditDeletePresentsRetryAndPreservesRecording() {
        val other=profile.copy(id="second"); val c=AuditLogController({listOf(profile,other)},{profile.id},{true},{false})
        c.begin(AuditLogAction.Enable); c.confirm(c.work!!.id); c.advance(c.work!!.id,0)
        c.choose(AuditLogScenario.PartialDelete); c.begin(AuditLogAction.Delete); c.confirm(c.work!!.id); c.advance(c.work!!.id,0)
        rule.setContent { WhiteNoiseTheme { AuditLogsScreen(c,{}) } }
        rule.onNodeWithText("Some audit logs were deleted. Retry to delete the remaining files.").performScrollTo().assertExists()
        rule.onNodeWithTag("audit.retry").performClick(); rule.runOnIdle { c.work!!.let { c.advance(it.id,it.attempt) }; assertTrue(c.state.enabled) }
    }
    @Test fun backFromLockedScreenLeavesAppWithoutUnlocking() {
        val controller=lock(); var left=false
        rule.setContent { WhiteNoiseTheme { AppLockScreen(controller,{left=true}) } }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertTrue(left); assertTrue(controller.blocked); assertNull(controller.request) }
    }

}
