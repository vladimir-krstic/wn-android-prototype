package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.conversation.ChatInfoScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatFolderFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
    private fun showEditorField(tag: String) { rule.onNodeWithTag("folder.editorList").performScrollToNode(hasTestTag(tag)) }

    @Test fun foldersEntryOpensEditorAndProfileSwitchRejectsOldRoute() {
        val vm = model(); lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        rule.onNodeWithTag("chats.folders").performScrollToNode(hasTestTag("chats.manageFolders"))
        rule.onNodeWithTag("chats.manageFolders").performClick()
        rule.onNodeWithContentDescription("New Folder").performClick()
        rule.onNodeWithTag("folder.name").performTextInput("Owner draft")
        rule.runOnIdle { vm.completeSignIn(OnboardingOrigin.AddProfile); vm.dismissDiagnosticsPrompt(vm.uiState.activeProfileId!!) }
        rule.onNodeWithTag("folder.name").assertDoesNotExist()
        rule.runOnIdle { assertTrue(vm.uiState.profiles.none { profile -> profile.chatFolders.any { it.name == "Owner draft" } }) }
    }
    @Test fun deletingAndRestoringDefaultPreservesChatsAndReorderHasAccessibleBounds() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val before = vm.uiState.activeProfile!!.chats
        rule.setContent { WhiteNoiseTheme { ChatFoldersScreen(vm.uiState.activeProfile!!, {}, {}, {},
            { id, delta -> vm.moveChatFolder(owner, id, delta) }, { vm.deleteChatFolder(owner, it) }, { vm.restoreChatFolders(owner) }) } }
        val actions = rule.onNodeWithTag("folder.row.system:unread").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertFalse(actions.any { it.label == "Move Up" }); actions.first { it.label == "Move Down" }.action() }
        rule.onNodeWithContentDescription("Actions for Unread").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("Delete this folder? Your chats will stay where they are.").assertIsDisplayed()
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithTag("folder.row.system:unread").assertDoesNotExist()
        rule.onNodeWithText("Restore Default Folders").performClick()
        rule.onNodeWithTag("folder.row.system:unread").assertExists()
        rule.runOnIdle { assertEquals(before, vm.uiState.activeProfile!!.chats) }
    }
    @Test fun blankNameCannotSaveAndBackRequiresDiscardWithoutMutatingProfile() {
        val profile = ProfileFixtures.marmota; var saved: ChatFolderDraft? = null; var closed = false
        rule.setContent { WhiteNoiseTheme { ChatFolderEditScreen(profile, null, { closed = true }) { saved = it; true } } }
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithTag("folder.name").performTextInput("Unsaved")
        pressBack() // Dismiss IME, if it owns Back.
        if (rule.onAllNodesWithText("Discard changes?").fetchSemanticsNodes().isEmpty()) pressBack()
        rule.onNodeWithText("Keep Editing").performClick()
        rule.onNodeWithTag("folder.name").assertTextContains("Unsaved")
        pressBack()
        rule.onNodeWithText("Discard Changes").performClick()
        rule.runOnIdle { assertTrue(closed); assertNull(saved) }
    }
    @Test fun manualPickerHasCheckboxSemanticsAndPreviewIncludesManuallyAddedChat() {
        val profile = ProfileFixtures.marmota; val chat = profile.chats.first(); var saved: ChatFolderDraft? = null
        rule.setContent { WhiteNoiseTheme { ChatFolderEditScreen(profile, null, {}) { saved = it; true } } }
        rule.onNodeWithTag("folder.name").performTextInput("Manual")
        rule.onNodeWithTag("folder.editorList").performScrollToNode(hasText("Included Chats"))
        rule.onNodeWithText("Included Chats").performClick()
        rule.onNodeWithTag("folder.pickerSearch").performTextInput(chat.title)
        rule.onNodeWithTag("folder.choice.${chat.id}").performClick().assertIsOn()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithTag("folder.editorList").performScrollToNode(hasText("Preview"))
        rule.onAllNodesWithText("Preview").filter(hasClickAction()).onFirst().performClick()
        rule.onNodeWithTag("folder.choice.${chat.id}").assertExists()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(setOf(chat.id), saved!!.chatIds) }
    }
    @Test fun editorDraftAndRulesSurviveRecreationAndFailedSave() {
        val profile = ProfileFixtures.marmota; var attempts = 0; var saved: ChatFolderDraft? = null
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { ChatFolderEditScreen(profile, null, {}) { attempts++; if (attempts == 1) false else { saved = it; true } } } }
        rule.onNodeWithTag("folder.name").performTextInput("Persistent draft")
        showEditorField("folder.keyword"); rule.onNodeWithTag("folder.keyword").performTextInput("outdoor")
        rule.onNodeWithTag("folder.editorList").performScrollToNode(hasText("Groups only"))
        rule.onNodeWithText("Groups only").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Save").performClick()
        showEditorField("folder.name")
        rule.onNodeWithTag("folder.name").assertTextContains("Persistent draft")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(2, attempts); assertEquals("outdoor", saved!!.rule.keyword); assertTrue(saved.rule.groupsOnly) }
    }
    @Test fun peoplePickerSetsOrCriterionAndDirtyChangesWaitForSave() {
        val profile = ProfileFixtures.marmota; val person = profile.people.first(); var saved: ChatFolderDraft? = null
        rule.setContent { WhiteNoiseTheme { ChatFolderEditScreen(profile, null, {}) { saved = it; true } } }
        rule.onNodeWithTag("folder.name").performTextInput("People")
        rule.onNodeWithTag("folder.editorList").performScrollToNode(hasText("People"))
        rule.onAllNodesWithText("People").filter(hasClickAction()).onFirst().performClick()
        rule.onNodeWithTag("sheet.surface").assertExists()
        rule.onNodeWithTag("sheet.dragHandle").assertExists()
        rule.onNodeWithTag("folder.pickerSearch").performTextInput(person.displayName)
        rule.onNodeWithTag("folder.choice.${person.id}").performClick().assertIsOn()
        rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(setOf(person.id), saved!!.rule.personIds) }
    }
    @Test fun deletingSelectedFolderFallsBackToChats() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        rule.setContent { WhiteNoiseTheme { ChatsScreen(vm.uiState, {}, {}, vm::markChatUnread, vm::toggleChatPin, vm::setChatMute, vm::setChatArchived, vm::leaveChat, {}) } }
        rule.onNodeWithTag("chats.folders").performScrollToNode(hasTestTag("chats.folder.system:archived"))
        rule.onNodeWithTag("chats.folder.system:archived").performClick()
        rule.runOnIdle { vm.deleteChatFolder(owner, "system:archived") }
        rule.onNodeWithTag("chats.scope.chats").assertIsSelected()
        rule.onNodeWithText("Archived").assertDoesNotExist()
    }
    @Test fun distantFolderPillKeepsSelectionThroughRestoreAndReorderThenResetsForAnotherProfile() {
        val vm = model()
        val owner = vm.uiState.activeProfileId!!
        val chat = vm.uiState.activeProfile!!.chats.first()
        val folders = (1..12).map { vm.createChatFolder(owner, "Folder $it")!! }
        val selected = folders.last()
        vm.assignChatFolder(owner, chat.id, selected)
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { ChatsScreen(vm.uiState, {}, {}, vm::markChatUnread,
            vm::toggleChatPin, vm::setChatMute, vm::setChatArchived, vm::leaveChat, {}) } }
        val tag = "chats.folder.$selected"
        rule.onNodeWithContentDescription("Filter Chats").assertDoesNotExist()
        rule.onNodeWithTag("chats.folders").performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performClick().assertIsSelected()
        rule.onNodeWithTag("chat.row.${chat.id}").assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag(tag).assertIsDisplayed().assertIsSelected().performClick().assertIsSelected()
        rule.runOnIdle { vm.moveChatFolder(owner, selected, -1) }
        rule.onNodeWithTag(tag).assertIsDisplayed().assertIsSelected()
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("chats.folders").assertDoesNotExist()
        rule.onNodeWithContentDescription("Close search").performClick()
        rule.onNodeWithTag(tag).assertIsSelected()
        rule.runOnIdle { vm.completeSignIn(OnboardingOrigin.AddProfile) }
        rule.onNodeWithTag("chats.scope.chats").assertIsDisplayed().assertIsSelected()
    }

    @Test fun chatInfoAssignmentUsesSameFolderPicker() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val chat = vm.uiState.activeProfile!!.chats.first()
        val folder = vm.createChatFolder(owner, "From Info")!!
        rule.setContent { WhiteNoiseTheme { ChatInfoScreen(vm.uiState.activeProfile!!, chat,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { true }, {},
            onCreateFolder = { vm.createChatFolder(owner, it) }, onAddToFolder = { vm.assignChatFolder(owner, chat.id, it) }) } }
        rule.onNodeWithTag("chat_info.list").performScrollToNode(hasText("Add to Folder"))
        rule.onNodeWithText("Add to Folder").performClick()
        rule.onNodeWithText("From Info").performClick()
        rule.runOnIdle { assertEquals(setOf(chat.id), vm.uiState.activeProfile!!.chatFolders.single { it.id == folder }.chatIds) }
    }
    @Test fun settingsEntryCreatesFolderAndReturnsToManagement() {
        val vm = model(); lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Settings()) }
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Folders"))
        rule.onNodeWithText("Folders").performClick()
        rule.onNodeWithContentDescription("New Folder").performClick()
        rule.onNodeWithTag("folder.name").performTextInput("From Settings")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("From Settings").assertExists()
        rule.onNodeWithTag("folder.name").assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, vm.uiState.activeProfile!!.chatFolders.count { it.name == "From Settings" }) }
    }

}
