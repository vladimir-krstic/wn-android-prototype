package dev.ipf.whitenoise

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatOrganizationFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private val first = "catalog-direct-text"
    @Composable private fun Chats(vm: AppViewModel) {
        val owner = vm.uiState.activeProfileId!!
        ChatsScreen(vm.uiState, {}, {}, vm::markChatUnread, vm::toggleChatPin, vm::setChatMute, vm::setChatArchived,
            vm::leaveChat, { vm.deleteEndedChat(it) },
            onMovePin = { id, delta -> vm.movePinnedChat(owner, id, delta) },
            onCreateFolder = { vm.createChatFolder(owner, it) },
            onBeginBatch = { ids, action, folder, leave -> vm.beginChatBatch(owner, ids, action, folder, leave) },
            batchAttempt = vm.chatBatchAttempt, onAdvanceBatch = vm::advanceChatBatch,
            onRetryBatch = { vm.retryChatBatch() }, onDismissBatch = vm::dismissChatBatch,
            onRetryConnection = { vm.retryChatConnection(owner) }, onAdvanceConnection = vm::advanceChatConnection)
    }
    private fun action(id: String, label: String) {
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("chat.row.$id"))
        val actions = rule.onNodeWithTag("chat.row.$id").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertTrue(actions.first { it.label == label }.action()) }
    }
    @Test fun selectionHasCheckboxSemanticsAndBackRestoresSearchFirst() {
        val vm = model()
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("chats.searchField").performTextInput(vm.chat(first)!!.title)
        action(first, "Select")
        rule.onNodeWithTag("chat.row.$first").assertIsOn()
        rule.onNodeWithTag("chats.newMessage").assertDoesNotExist()
        pressBack()
        rule.onNodeWithTag("chats.searchField").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("chats.newMessage").assertIsDisplayed()
    }
    @Test fun selectionSurvivesRestorationAndResetsOnProfileChange() {
        val vm = model(); val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { Chats(vm) } }
        action(first, "Select")
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("chat.row.$first").assertIsOn()
        rule.runOnIdle { vm.completeSignIn(OnboardingOrigin.AddProfile) }
        rule.onNodeWithContentDescription("Close selection").assertDoesNotExist()
    }
    @Test fun selectAllUsesCurrentFilterAndArchiveRemovesOnlyThoseRows() {
        val vm = model(); val unread = ChatProjection.rows(vm.uiState.activeProfile!!.chats, ChatScope.Unread).map { it.id }
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithContentDescription("Filter Chats").performClick()
        rule.onNodeWithText("Unread").performClick()
        action(unread.first(), "Select")
        rule.onNodeWithText("Select All").performClick()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Archive").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertTrue(unread.all { vm.chat(it)!!.isArchived }) }
        rule.onNodeWithContentDescription("Close selection").assertDoesNotExist()
    }
    @Test fun localDeleteRequiresConfirmationAndDefaultDoesNotLeave() {
        val vm = model(); val id = vm.createGroup("Sole admin", "", ProfileAvatar.Monogram, listOf(vm.uiState.activeProfile!!.people.first().id))!!
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        action(id, "Delete Chat")
        rule.onNodeWithText("Also leave active chats").assertIsOff()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNotNull(vm.chat(id)) }
        action(id, "Delete Chat")
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertNull(vm.chat(id)) }
    }
    @Test fun failedLeavePreservesRowAndOffersRetry() {
        val vm = model(); vm.selectChatBatchScenario(ChatBatchScenario.LeaveFailure)
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        action(first, "Delete Chat")
        rule.onNodeWithText("Also leave active chats").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("Could not leave this chat. Your history is still here.").assertIsDisplayed()
        rule.runOnIdle { assertNotNull(vm.chat(first)) }
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertNull(vm.chat(first)) }
    }
    @Test fun folderAssignmentCreatesUsableFilterAndRejectsBlankName() {
        val vm = model()
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        action(first, "Add to Folder")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithTag("chat.folderName").performTextInput("Friends")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithContentDescription("Filter Chats").performClick()
        rule.onNodeWithText("Friends").performClick()
        rule.onNodeWithTag("chat.row.$first").assertIsDisplayed()
        rule.runOnIdle { assertEquals(setOf(first), vm.uiState.activeProfile!!.chatFolders.single().chatIds) }
    }
    @Test fun offlineRetryPreservesRowsThroughFailureAndRecovery() {
        val vm = model(); vm.selectChatConnectionScenario(ChatConnectionScenario.RetryFailure)
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$first").assertExists()
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithText("Could not reconnect. Your loaded chats are still available.").assertIsDisplayed()
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithTag("chats.connection").assertDoesNotExist()
        rule.onNodeWithTag("chat.row.$first").assertExists()
    }
}
