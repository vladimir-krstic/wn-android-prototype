package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageBatchFlowTest {
    @get:Rule val rule=createAndroidComposeRule<EmptyTestActivity>()
    private val chatId="fiatjaf"
    private fun model()=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial);dismissDiagnosticsPrompt(uiState.activeProfileId!!);setDeveloperToolsEnabled(true) }
    private fun AppViewModel.owner()=uiState.activeProfileId!!
    private fun AppViewModel.send(): String { sendText(chatId,"A message to manage");return chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().id }
    private fun open(vm: AppViewModel, chat: String, target: String): NavHostController {
        lateinit var nav: NavHostController
        rule.setContent { nav=rememberNavController();WhiteNoiseTheme { WhiteNoiseNavHost(nav,vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation(chat,targetMessageId=target)) }
        return nav
    }
    private fun action(id: String,label: String) {
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.$id"))
        val action=rule.onNodeWithTag("conversation.message.$id").fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label==label }
        rule.runOnIdle { assertTrue(action.action()) }
    }
    @Test fun adminCanRemoveAnotherAuthorsMessageThenRemoveItsTombstoneLocally() {
        val vm=model();val p=vm.uiState.activeProfile!!
        val chat=p.chats.first { it.isGroup&&it.composerAvailability(p)==ComposerAvailability.Available&&it.members.any { m->m.personId==p.id&&m.role==GroupRole.Admin } }
        val id=chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.message.authorId!=p.id }.id
        open(vm,chat.id,id);action(id,"Delete");rule.onNodeWithText("Delete for everyone").performClick()
        rule.waitUntil(4_000){vm.message(chat.id,id)?.isDeleted==true}
        rule.onNodeWithTag("conversation.message.$id").performSemanticsAction(SemanticsActions.OnLongClick)
        rule.onNodeWithText("Delete for everyone").assertDoesNotExist()
        rule.onNodeWithText("Delete for me").performClick()
        rule.waitUntil(4_000){vm.message(chat.id,id)==null}
    }
    @Test fun mixedConfirmationExplainsRemoteAndLocalCountsBeforeAnyMutation() {
        val vm=model();val own=vm.send();val other=vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.message.authorId!=vm.owner() }.id
        var requested: MessageDeletionScope?=null
        rule.setContent {WhiteNoiseTheme {DeleteMessagesDialog(listOf(vm.message(chatId,own)!!,vm.message(chatId,other)!!),vm.owner(),{}, {requested=it},remoteIds=setOf(own))}}
        rule.onNodeWithText("For everyone: 1. Only from this device: 1. Others will still see the messages removed only from this device.").assertExists()
        rule.onNodeWithText("Cancel").performClick();rule.runOnIdle {assertNull(requested);assertFalse(vm.message(chatId,own)!!.isDeleted)}
    }
    @Test fun failedBatchRetainsFailedSelectionAndRetryFinishesOnlyThoseItems() {
        val vm=model();val first=vm.send();val second=vm.send();vm.selectMessageDeleteScenario(MessageDeleteScenario.Partial)
        open(vm,chatId,second);action(second,"Select")
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.$first"))
        rule.onNodeWithTag("conversation.message.$first").performClick()
        rule.onNodeWithContentDescription("Delete Selected Messages").performClick();rule.onNodeWithText("Delete for everyone").performClick()
        rule.waitUntil(4_000){vm.chat(chatId)!!.messageDeletion?.let{!it.isRunning&&it.failed.size==1}==true}
        rule.onNodeWithText("Deleted 1 of 2 messages").assertExists();rule.onNodeWithText("Retry failed").performClick()
        rule.waitUntil(4_000){vm.chat(chatId)!!.messageDeletion?.succeeded==2}
        rule.onNodeWithText("Deleted 2 of 2 messages").assertExists()
        rule.onNodeWithContentDescription("Close Selection").assertDoesNotExist()
    }
    @Test fun foldersUseSeparatePageAndDoneAppliesWhileBackDiscards() {
        val vm=model();val p=vm.uiState.activeProfile!!
        val targets=p.chats.filter{it.id!=chatId&&it.composerAvailability(p)==ComposerAvailability.Available}.take(6).map{it.id}.toSet()
        val folderId=vm.saveChatFolder(p.id,null,ChatFolderDraft("Recipients",chatIds=targets))!!
        rule.setContent {WhiteNoiseTheme {ForwardMessagesSheet(vm.uiState.activeProfile!!,chatId,{},onForward={_,_->})}}
        rule.onNodeWithTag("conversation.forward.submit").assertDoesNotExist()
        rule.onNodeWithTag("conversation.forward.folder.$folderId").assertDoesNotExist()
        rule.onNodeWithTag("conversation.forward.chooseFolders").performClick()
        rule.onNodeWithTag("conversation.forward.folderList").performScrollToNode(hasTestTag("conversation.forward.folder.$folderId"))
        rule.onNodeWithTag("conversation.forward.folder.$folderId").performClick().assertIsOn()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag("conversation.forward.submit").assertDoesNotExist()
        rule.onNodeWithTag("conversation.forward.chooseFolders").performClick()
        rule.onNodeWithTag("conversation.forward.folderList").performScrollToNode(hasTestTag("conversation.forward.folder.$folderId"))
        rule.onNodeWithTag("conversation.forward.folder.$folderId").performClick()
        rule.onNodeWithTag("conversation.forward.folders.done").performClick()
        rule.onNodeWithText("Forward to 6 Chats").assertExists()
        rule.onNodeWithTag("conversation.forward.chooseFolders").performClick()
        rule.onNodeWithTag("conversation.forward.folderList").performScrollToNode(hasTestTag("conversation.forward.folder.$folderId"))
        rule.onNodeWithTag("conversation.forward.folder.$folderId").performClick().assertIsOff()
        rule.onNodeWithTag("conversation.forward.folders.done").performClick()
        rule.onNodeWithTag("conversation.forward.submit").assertDoesNotExist()
    }
    @Test fun forwardActionAppearsOnlyAfterSelectingAChatAndHidesAfterDeselecting() {
        val vm=model()
        rule.setContent {WhiteNoiseTheme {ForwardMessagesSheet(vm.uiState.activeProfile!!,chatId,{},onForward={_,_->})}}
        rule.onNodeWithTag("conversation.forward.submit").assertDoesNotExist()
        rule.onNodeWithTag("conversation.forward.search").performTextInput("Maya")
        rule.onNodeWithTag("conversation.forward.destination.maya-chen").performClick()
        rule.onNodeWithTag("conversation.forward.submit").assertIsEnabled()
        rule.onNodeWithText("Forward to 1 Chat").assertExists()
        rule.onNodeWithTag("conversation.forward.destination.maya-chen").performClick()
        rule.onNodeWithTag("conversation.forward.submit").assertDoesNotExist()
    }
    @Test fun unavailableDestinationExplainsWhyItCannotBeSelected() {
        val vm=model();vm.toggleBlocked("maya-chen")
        rule.setContent {WhiteNoiseTheme {ForwardMessagesSheet(vm.uiState.activeProfile!!,chatId,{},onForward={_,_->})}}
        rule.onNodeWithTag("conversation.forward.search").performTextInput("Maya")
        rule.onNodeWithTag("conversation.forward.destinations").performScrollToNode(hasTestTag("conversation.forward.destination.maya-chen"))
        rule.onNodeWithTag("conversation.forward.destination.maya-chen").assertIsNotEnabled()
        rule.onNodeWithText("Unblock this person to send messages.").assertExists()
    }
    @Test fun multipleFoldersSelectUniqueChatsAndSystemBackReturnsToThePicker() {
        val vm=model(); val p=vm.uiState.activeProfile!!
        val targets=p.chats.filter { it.id!=chatId && it.composerAvailability(p)==ComposerAvailability.Available }.take(3).map { it.id }
        val first=vm.saveChatFolder(p.id,null,ChatFolderDraft("First recipients",chatIds=targets.take(2).toSet()))!!
        val second=vm.saveChatFolder(p.id,null,ChatFolderDraft("More recipients",chatIds=targets.drop(1).toSet()))!!
        rule.setContent { WhiteNoiseTheme { ForwardMessagesSheet(vm.uiState.activeProfile!!,chatId,{},onForward={_,_->}) } }
        rule.onNodeWithTag("conversation.forward.chooseFolders").performClick()
        for (id in listOf(first,second)) {
            rule.onNodeWithTag("conversation.forward.folderList").performScrollToNode(hasTestTag("conversation.forward.folder.$id"))
            rule.onNodeWithTag("conversation.forward.folder.$id").performClick()
        }
        rule.onNodeWithTag("conversation.forward.folders.done").performClick()
        rule.onNodeWithText("Forward to 3 Chats").assertExists()
        rule.onNodeWithTag("conversation.forward.chooseFolders").performClick()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("conversation.forward.folders").assertDoesNotExist()
        rule.onNodeWithText("Forward to 3 Chats").assertExists()
    }
    @Test fun completedForwardClearsOnceAndNeverShowsPersistentSuccessControls() {
        val vm=model(); val id=vm.send()
        assertTrue(vm.forwardMessages(chatId,setOf(id),listOf("maya-chen")))
        val op=vm.messageForwards.getValue(vm.owner())
        var dismissed=0
        var appearance by mutableStateOf(AppearancePreference.Light)
        rule.setContent {
            WhiteNoiseTheme(appearance=appearance) {
                MessageOperationsHost(vm.uiState.activeProfile,op,{_,_->},{_,_,_->},{},{},{ dismissed++ }) {}
            }
        }
        rule.waitUntil { dismissed==1 }
        rule.onNodeWithTag("message.forward.status").assertDoesNotExist()
        rule.onNodeWithText("Details").assertDoesNotExist()
        rule.onNodeWithText("Dismiss").assertDoesNotExist()
        rule.runOnIdle { appearance=AppearancePreference.Dark }
        rule.runOnIdle { assertEquals(1,dismissed) }
    }
    @Test fun changingDestinationProfileClearsRecipientsWithoutSwitchingActiveProfile() {
        val vm=model();val owner=vm.owner();vm.completeSignIn(OnboardingOrigin.AddProfile);val other=vm.owner();vm.openOrCreateDirectChat("maya-chen",requestedChatId="maya-chen");vm.selectProfile(owner)
        rule.setContent {WhiteNoiseTheme {ForwardMessagesSheet(vm.uiState.activeProfile!!,chatId,{},onForward={_,_->},destinationProfiles=vm.uiState.signedInProfiles)}}
        rule.onNodeWithTag("conversation.forward.search").performTextInput("Maya")
        rule.onNodeWithTag("conversation.forward.destinations").performScrollToNode(hasTestTag("conversation.forward.destination.maya-chen"))
        rule.onNodeWithTag("conversation.forward.destination.maya-chen").performClick().assertIsOn()
        rule.onNodeWithTag("conversation.forward.profile").performClick()
        rule.onNodeWithText(vm.uiState.profiles.first{it.id==other}.name).performClick()
        rule.onNodeWithTag("conversation.forward.destination.maya-chen").assertIsOff()
        rule.runOnIdle {assertEquals(owner,vm.owner())}
    }
    @Test fun appOwnedForwardProgressSurvivesNavigationAndExposesRetry() {
        val vm=model();val id=vm.send();vm.selectMessageForwardScenario(MessageForwardScenario.PartialSendUntilRetried)
        val nav=open(vm,chatId,id)
        rule.runOnIdle {assertTrue(vm.beginMessageForward(vm.owner(),chatId,setOf(id),vm.owner(),listOf("maya-chen","theo-grant")));nav.navigate(AppRoute.Settings())}
        rule.waitUntil(5_000){vm.messageForwards[vm.owner()]?.phase==MessageForwardPhase.PartialFailure}
        rule.onNodeWithTag("message.forward.status").assertExists();rule.onNodeWithText("Details").performClick()
        rule.onNodeWithTag("message.forward.target.theo-grant").performScrollTo().assertExists()
        rule.onNodeWithText("Couldn’t send the remaining messages.").assertExists();rule.onNodeWithText("Retry failed").performScrollTo().performClick()
        rule.waitUntil(5_000){vm.messageForwards[vm.owner()]==null}
        rule.onNodeWithTag("message.forward.details").assertDoesNotExist()
        rule.onNodeWithTag("message.forward.status").assertDoesNotExist()
    }
    @Test fun preparingAndSendingKeepContentFullHeightWithoutProgressControls() {
        val vm=model(); val id=vm.send()
        vm.beginMessageForward(vm.owner(),chatId,setOf(id),vm.owner(),listOf("maya-chen"))
        var op by mutableStateOf(vm.messageForwards.getValue(vm.owner()))
        var advanced=0
        var cancelled=false
        var dismissed=false
        rule.setContent {
            WhiteNoiseTheme {
                MessageOperationsHost(vm.uiState.activeProfile,op,{_,_-> advanced++},{_,_,_->},{},{cancelled=true},{dismissed=true},
                    modifier=androidx.compose.ui.Modifier.testTag("operation.host")) { modifier ->
                    androidx.compose.foundation.layout.Box(modifier.testTag("operation.content"))
                }
            }
        }
        rule.waitUntil { advanced>0 }
        rule.onNodeWithTag("message.forward.status").assertDoesNotExist()
        rule.onNodeWithText("Details").assertDoesNotExist()
        val host=rule.onNodeWithTag("operation.host").fetchSemanticsNode().boundsInRoot
        val content=rule.onNodeWithTag("operation.content").fetchSemanticsNode().boundsInRoot
        assertEquals(host.top,content.top,1f)
        assertEquals(host.bottom,content.bottom,1f)
        rule.runOnIdle { op=op.copy(phase=MessageForwardPhase.Running,revision=op.revision+1) }
        rule.onNodeWithTag("message.forward.status").assertDoesNotExist()
        rule.runOnIdle { assertFalse(cancelled); assertFalse(dismissed) }
    }
    @Test fun sharedContentForwardsOneFrameToAnotherProfileThroughTheAppOwnedOperation() {
        val vm=model();val owner=vm.owner()
        vm.completeSignIn(OnboardingOrigin.AddProfile);val destination=vm.owner()
        vm.openOrCreateDirectChat("maya-chen",requestedChatId="maya-chen");vm.selectProfile(owner)
        val source="catalog-media-gallery"
        val frame=ConversationMediaProjection.items(vm.chat(source)!!,vm.uiState.activeProfile!!).first()
        lateinit var nav: NavHostController
        rule.setContent {nav=rememberNavController();WhiteNoiseTheme {WhiteNoiseNavHost(nav,vm)}}
        rule.runOnIdle {nav.navigate(AppRoute.SharedContent(source,SharedContentCategory.Media.name))}
        rule.onNodeWithTag("conversation.shared.media.${frame.key.stableId}").performClick()
        rule.onNodeWithContentDescription("Forward").performClick()
        rule.onNodeWithTag("conversation.forward.profile").performClick()
        rule.onNodeWithText(vm.uiState.profiles.first {it.id==destination}.name).performClick()
        rule.onNodeWithTag("conversation.forward.search").performTextInput("Maya")
        rule.onNodeWithTag("conversation.forward.destinations").performScrollToNode(hasTestTag("conversation.forward.destination.maya-chen"))
        rule.onNodeWithTag("conversation.forward.destination.maya-chen").performClick()
        rule.onNodeWithContentDescription("Forward to 1 Chat").performClick()
        rule.waitUntil(5_000){vm.messageForwards[owner]==null}
        rule.onNodeWithTag("message.forward.status").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(owner,vm.owner())
            val copy=vm.uiState.profiles.first {it.id==destination}.chats.first {it.id=="maya-chen"}.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
            assertEquals(1,copy.attachments.size);assertEquals(frame.image,copy.attachments.single().images.singleOrNull())
        }
    }

}
