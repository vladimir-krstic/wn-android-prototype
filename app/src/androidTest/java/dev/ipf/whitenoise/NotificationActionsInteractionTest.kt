package dev.ipf.whitenoise

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.share.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationActionsInteractionTest {
    @get:Rule val rule=createAndroidComposeRule<EmptyTestActivity>()
    private val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun target(): NotificationTarget {
        val chat=vm.createGroup("Trail", "", ProfileAvatar.Monogram,emptyList())!!
        vm.addConversationArrival(vm.uiState.activeProfileId!!,chat)
        return NotificationTarget(vm.uiState.activeProfileId!!,chat,vm.chat(chat)!!.timeline.last().id)
    }
    private fun start(t: NotificationTarget,scenario: NotificationActionScenario=NotificationActionScenario.Success): Long {
        vm.notificationActions.choose(scenario)
        return vm.notificationActions.submit(NotificationActionInput("action",NotificationCard("card",1,t),NotificationActionKind.Reply,"Hello"))!!
    }
    private fun step() { vm.notificationActions.work!!.let { vm.notificationActions.advance(it.id,it.phase,it.attempt) } }
    private fun show() { rule.setContent { WhiteNoiseTheme { NotificationActionStatus(vm.notificationActions) } } }
    @Test fun pendingActionCanBeCancelledWithoutSending() {
        val t=target(); start(t); show(); rule.onNodeWithText("Sending reply…").assertExists(); rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNull(vm.notificationActions.work); assertFalse(vm.chat(t.chatId)!!.timeline.any { it.id.startsWith("notification-reply-") }) }
    }
    @Test fun operationFailureOffersRetryAndThenAcceptedStatus() {
        val t=target(); start(t,NotificationActionScenario.FailsOnce); step(); show()
        rule.onNodeWithText("The action could not be completed. Try again.").assertExists()
        rule.onNodeWithTag("notification.action.retry").performClick(); rule.runOnIdle { step(); step() }
        rule.onNodeWithText("Reply sent").assertExists(); rule.onNodeWithTag("notification.action.done").performClick()
        rule.runOnIdle { assertNull(vm.notificationActions.work) }
    }
    @Test fun cleanupFailureSaysReplySentAndRetryDoesNotDuplicate() {
        val t=target(); start(t,NotificationActionScenario.CleanupFails); step(); step(); show()
        rule.onNodeWithText("Reply sent").assertExists(); rule.onNodeWithText("The action succeeded, but its notification could not be cleared. Retry to finish without sending again.").assertExists()
        rule.onNodeWithTag("notification.action.retry").performClick(); rule.runOnIdle { step(); assertEquals(1,vm.chat(t.chatId)!!.timeline.count { it.id.startsWith("notification-reply-") }) }
    }
    @Test fun exhaustedRetryHasNoRetryAffordance() {
        val t=target(); val id=start(t,NotificationActionScenario.AlwaysFails)
        repeat(2) { step(); vm.notificationActions.retry(id) }; step(); show()
        rule.onNodeWithTag("notification.action.retry").assertDoesNotExist()
        rule.onNodeWithText("The action could not be completed after three attempts. Open the conversation to continue.").assertExists()
    }
    @Test fun systemBackDismissesUnacceptedAction() {
        val t=target(); start(t); show(); rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertNull(vm.notificationActions.work) }
    }
    @Test fun notificationHostDoesNotShowAnotherProfilesAction() {
        val t=target(); start(t,NotificationActionScenario.FailsOnce); step()
        rule.setContent { WhiteNoiseTheme { NotificationActionsHost(vm.notificationActions,"another-profile",false) { Text("Profile settings") } } }
        rule.onNodeWithText("Profile settings").assertExists(); rule.onNodeWithText("Notification action").assertDoesNotExist()
    }
    @Test fun unconfirmedInvitationOffersChatsRecovery() {
        val t=target().copy(chatId="pending-row",messageId=null,kind=NotificationTargetKind.Invite)
        vm.incoming.receive(IncomingEntry.Notification(t)); repeat(3) { vm.incoming.work!!.let { w->vm.incoming.advance(w.id,w.phase,w.attempt) } }
        rule.setContent { WhiteNoiseTheme { IncomingShareScreen(vm.incoming) } }
        rule.onNodeWithText("This invitation could not be confirmed. Try again or go to Chats.").assertExists()
        rule.onNodeWithText("Go to Chats").performClick(); rule.runOnIdle { assertTrue(vm.incoming.opening(vm.incoming.work!!.id)!!.chatList) }
    }
}
