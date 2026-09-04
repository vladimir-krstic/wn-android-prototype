package dev.ipf.whitenoise

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
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
class MessageEditingReadingFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val chatId = "fiatjaf"
    private fun model(body: String = "The original plan") = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!)
        sendText(chatId, body); updateDraftText(chatId, "Keep this draft")
    }
    private fun AppViewModel.message() = chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
    private fun open(vm: AppViewModel): NavHostController {
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation(chatId, targetMessageId = vm.message().id)) }
        return nav
    }
    private fun action(vm: AppViewModel, label: String) {
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.${vm.message().id}"))
        val action = rule.onNodeWithTag("conversation.message.${vm.message().id}").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == label }
        rule.runOnIdle { assertTrue(action.action()) }
    }
    private fun clipboard(): String = (rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()

    @Test fun editSaveUpdatesBubbleAndHistoryIncludesOriginalAndAcceptedRevision() {
        val vm = model(); open(vm); action(vm, "Edit")
        rule.onNodeWithTag("message.edit.input").performTextReplacement("The revised plan")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(4_000) { vm.message().text == "The revised plan" && vm.message().editAttempt == null }
        action(vm, "Edit history")
        rule.onNodeWithText("Original").assertExists(); rule.onNodeWithText("The original plan").assertExists()
        rule.onNodeWithText("Revision 1").assertExists(); rule.onNode(hasText("The revised plan") and hasAnyAncestor(hasTestTag("message.history"))).assertExists()
        rule.runOnIdle { assertEquals("Keep this draft", vm.chat(chatId)!!.draftText) }
    }
    @Test fun failedEditRetainsAcceptedTextAndRetryDoesNotDuplicateHistory() {
        val vm = model().apply { setDeveloperToolsEnabled(true); selectMessageEditScenario(MessageEditScenario.SaveFails) }
        open(vm); action(vm, "Edit")
        rule.onNodeWithTag("message.edit.input").performTextReplacement("A retryable change")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(4_000) { vm.message().editAttempt?.phase == MessageEditPhase.Failed }
        rule.onNodeWithText("Couldn’t save this edit.").assertExists()
        rule.runOnIdle { assertEquals("The original plan", vm.message().text) }
        action(vm, "Retry edit")
        rule.waitUntil(4_000) { vm.message().editAttempt == null }
        rule.runOnIdle { assertEquals("A retryable change", vm.message().text); assertEquals(1, vm.message().editHistory!!.revisions.size) }
    }
    @Test fun failedEditDiscardAndEditorCancelLeaveAcceptedBodyAndComposerUntouched() {
        val vm = model().apply { setDeveloperToolsEnabled(true); selectMessageEditScenario(MessageEditScenario.SaveFails) }
        open(vm); action(vm, "Edit")
        rule.onNodeWithTag("message.edit.input").performTextReplacement("Discard this edit")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(4_000) { vm.message().editAttempt?.phase == MessageEditPhase.Failed }
        action(vm, "Discard edit")
        action(vm, "Edit"); rule.onNodeWithTag("message.edit.input").performTextReplacement("Unsaved editor text")
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.onNodeWithTag("message.edit").assertDoesNotExist()
        rule.runOnIdle { assertEquals("The original plan", vm.message().text); assertNull(vm.message().editAttempt); assertEquals("Keep this draft", vm.chat(chatId)!!.draftText) }
    }
    @Test fun editorDraftAndSelectionSurviveSavedStateRestoration() {
        val message = ChatMessage("edit", "owner", 1, "Today", 600, "10:00", "Original")
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { MessageEditDialog("owner", message, {}, { _, _ -> false }) } }
        rule.onNodeWithTag("message.edit.input").performTextReplacement("Retained editor draft")
        rule.onNodeWithTag("message.edit.input").performTextInputSelection(TextRange(3, 9))
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("message.edit.input").assertTextContains("Retained editor draft")
        rule.onNodeWithTag("message.edit.input").assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(3, 9)))
    }
    @Test fun fullReaderOpensFromLongPreviewAndShowsTheEntireDocument() {
        val vm = model().apply { setDeveloperToolsEnabled(true); addMessageReadingExample(uiState.activeProfileId!!, chatId) }
        open(vm)
        rule.onNodeWithTag("message.readMore.${vm.message().id}").performScrollTo().performClick()
        rule.onNodeWithTag("message.reader").assertExists()
        rule.onNode(hasText("Walking journal") and hasAnyAncestor(hasTestTag("message.reader"))).assertExists()
        rule.onNode(hasText("Day 60:", substring = true) and hasAnyAncestor(hasTestTag("message.reader"))).performScrollTo().assertIsDisplayed()
        rule.onNode(hasContentDescription("Back") and hasAnyAncestor(hasTestTag("message.reader"))).performClick()
        rule.onNodeWithText("Keep this draft").assertExists()
    }
    @Test fun collapseSettingUsesNativeSwitchAndAffectsOnlyThisChat() {
        val vm = model(); val nav = open(vm)
        rule.runOnIdle { nav.navigate(AppRoute.ChatInfo(chatId)) }
        rule.onNodeWithText("Collapse long messages").performScrollTo().performClick()
        rule.runOnIdle { assertFalse(vm.chat(chatId)!!.collapseLongMessages); assertTrue(vm.chat("maya-chen")!!.collapseLongMessages) }
        rule.onNodeWithText("Collapse long messages").assertIsOff()
    }
    @Test fun nativePassageSelectionCopiesExactTextAndBackClearsSelectionBeforeClosingReader() {
        val vm = model("First repeat and second repeat"); open(vm); action(vm, "Select text")
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("message.selection.copy").fetchSemanticsNodes().isNotEmpty() }
        val text = rule.onNode(hasText("First repeat and second repeat") and hasAnyAncestor(hasTestTag("message.reader")))
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        text.performTouchInput { longClick(layouts.single().getBoundingBox(26).center) }
        rule.onNodeWithTag("message.selection.copy").performClick()
        rule.runOnIdle { assertEquals("repeat", clipboard()) }
        rule.onNode(hasContentDescription("Back") and hasAnyAncestor(hasTestTag("message.reader"))).performClick()
        rule.onNodeWithTag("message.reader").assertExists()
        rule.onNodeWithTag("message.selection.copy").assertDoesNotExist()
        rule.onNode(hasContentDescription("Back") and hasAnyAncestor(hasTestTag("message.reader"))).performClick()
        rule.onNodeWithTag("message.reader").assertDoesNotExist()
    }
    @Test fun namedMarkdownLinkShowsDestinationAndCopiesWithoutOpeningExternalHandler() {
        val opened = mutableListOf<String>()
        rule.setContent { CompositionLocalProvider(LocalUriHandler provides object : UriHandler { override fun openUri(uri: String) { opened += uri } }) {
            WhiteNoiseTheme { MessageDocumentContent(MessageDocuments.parse("[Trail](https://example.org/actual)"), emptyList(), {}) }
        } }
        rule.onNode(hasText("Trail") and hasClickAction()).performClick()
        rule.onNodeWithText("Open link?").assertExists()
        rule.onNodeWithText("https://example.org/actual").assertExists()
        rule.onNodeWithText("Copy link").performClick()
        rule.runOnIdle { assertEquals("https://example.org/actual", clipboard()); assertTrue(opened.isEmpty()) }
    }
    @Test fun acceptedRevisionAndDeletedSourceInvalidateReaderSelectionAndContent() {
        val vm = model(); open(vm); action(vm, "Select text")
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("message.selection.copy").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle {
            vm.beginMessageEdit(vm.uiState.activeProfileId!!, chatId, vm.message().id, "Updated while reading")
            vm.advanceMessageEdit(vm.uiState.activeProfileId!!, chatId, vm.message().id, vm.message().editAttempt!!.id)
        }
        rule.onNodeWithTag("message.selection.copy").assertDoesNotExist()
        rule.onNode(hasText("Updated while reading") and hasAnyAncestor(hasTestTag("message.reader"))).assertExists()
        rule.runOnIdle { vm.deleteMessages(chatId, setOf(vm.message().id), MessageDeletionScope.ForEveryone) }
        rule.onNodeWithTag("message.reader").assertDoesNotExist()
    }
    @Test fun fullComposerExpansionPreservesSelectionDraftAndReply() {
        val vm = model().apply { setDraftReply(chatId, message().id); updateDraftText(chatId, "An ordinary composer draft") }
        open(vm)
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performTextInputSelection(TextRange(3, 11))
        val expand = rule.onNodeWithTag("conversation.composer.surface").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Expand Message" }
        rule.runOnIdle { assertTrue(expand.action()) }
        editor.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(3, 11)))
        val collapse = rule.onNodeWithTag("conversation.composer.surface").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Collapse Message" }
        rule.runOnIdle { assertTrue(collapse.action()) }
        editor.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(3, 11)))
        rule.runOnIdle { assertEquals("An ordinary composer draft", vm.chat(chatId)!!.draftText); assertNotNull(vm.chat(chatId)!!.draftReplyMessageId) }
    }
}
