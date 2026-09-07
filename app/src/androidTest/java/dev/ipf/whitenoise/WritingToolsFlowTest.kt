package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WritingToolsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var chat by mutableStateOf(Chat("chat", 0, ChatKind.Direct("friend"), "Friend", draftText = "i recieve teh note tommorow."))
    private var profile by mutableStateOf(Profile("me", "Me", "key"))
    private var editable by mutableStateOf(true)
    private var sends = 0
    @Composable private fun Screen(scenario: WritingScenario = WritingScenario.Ready) {
        WhiteNoiseTheme {
            CompositionLocalProvider(LocalWritingScenario provides { scenario }) {
                FullConversationComposer(profile, chat, { chat = chat.copy(draftText = it) }, {}, {}, {}, {},
                    onSendDraft = { sends++; true }, onSendVoice = { false }, writingToolsEnabled = editable)
            }
        }
    }
    private fun open(operation: WritingOperation = WritingOperation.Proofread) {
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithTag("writing.menu").performClick()
        rule.onNodeWithTag("writing.operation.${operation.name}").performClick()
    }
    private fun preview() = rule.waitUntil(4_000) { rule.onAllNodesWithTag("writing.suggestion").fetchSemanticsNodes().isNotEmpty() }

    @Test fun wholeDraftPreviewDiscardAndApplyNeverSend() {
        rule.setContent { Screen() }
        val original = chat.draftText
        open(); preview()
        rule.onNodeWithTag("writing.original").assertTextEquals(original)
        rule.onNodeWithTag("writing.suggestion").assertTextEquals("I receive the note tomorrow.")
        rule.runOnIdle { assertEquals(original, chat.draftText); assertEquals(0, sends) }
        rule.onNodeWithTag("writing.discard").performClick()
        rule.runOnIdle { assertEquals(original, chat.draftText) }
        open(); preview(); rule.onNodeWithTag("writing.apply").performClick()
        rule.runOnIdle { assertEquals("I receive the note tomorrow.", chat.draftText); assertEquals(0, sends) }
        rule.onNodeWithTag("writing.sheet").assertDoesNotExist()
    }
    @Test fun selectedPassageActionPreservesPrefixSuffixAndSelection() {
        chat = chat.copy(draftText = "Prefix teh suffix")
        rule.setContent { Screen() }
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performTextInputSelection(TextRange(7, 10))
        val action = editor.fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Writing tools" }
        rule.runOnIdle { assertTrue(action.action()) }
        rule.onNodeWithTag("writing.operation.Proofread").performClick(); preview()
        rule.onNodeWithTag("writing.original").assertTextEquals("teh")
        rule.onNodeWithTag("writing.apply").performClick()
        rule.runOnIdle { assertEquals("Prefix the suffix", chat.draftText); assertEquals(0, sends) }
        editor.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange, TextRange(7, 10)))
    }
    @Test fun nativeSelectionMenuOffersWritingTools() {
        chat = chat.copy(draftText = "teh")
        rule.setContent { Screen() }
        rule.onNodeWithTag("conversation.composer.editor").performTouchInput { longClick() }
        rule.onNodeWithText("Writing tools").performClick()
        rule.onNodeWithTag("writing.operation.Proofread").assertExists()
    }
    @Test fun emptyDraftDisablesWritingTools() {
        chat = chat.copy(draftText = "")
        rule.setContent { Screen() }
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithTag("writing.menu").assertIsNotEnabled()
    }
    @Test fun newerDraftAndEditModeInvalidatePreview() {
        rule.setContent { Screen() }
        open(); preview()
        rule.runOnIdle { chat = chat.copy(draftText = "My newer draft") }
        rule.onNodeWithTag("writing.status.Stale").assertExists()
        rule.onNodeWithTag("writing.apply").assertDoesNotExist()
        rule.onNodeWithTag("writing.discard").performClick()
        rule.runOnIdle { editable = false }
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithTag("writing.menu").assertIsNotEnabled()
    }
    @Test fun unavailableRetryAndExternalDisclosureKeepOriginalUntilApply() {
        var scenario by mutableStateOf(WritingScenario.Unavailable)
        rule.setContent { Screen(scenario) }
        val original = chat.draftText
        open()
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("writing.status.Unavailable").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("writing.retry").performClick(); preview()
        rule.onNodeWithTag("writing.discard").performClick()
        rule.runOnIdle { scenario = WritingScenario.NetworkConsent }
        open()
        rule.onNodeWithTag("writing.status.NetworkConsent").assertExists()
        rule.onNodeWithTag("writing.suggestion").assertDoesNotExist()
        rule.onNodeWithTag("writing.proceed").performClick(); preview()
        rule.runOnIdle { assertEquals(original, chat.draftText); assertEquals(0, sends) }
    }
    @Test fun backDuringDownloadCancelsAndRecreationKeepsDraft() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Screen(WritingScenario.DownloadRequired) }
        val original = chat.draftText
        open(); rule.onNodeWithTag("writing.proceed").performClick()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("writing.sheet").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertEquals(original, chat.draftText); assertEquals(0, sends) }
        rule.onNodeWithTag("writing.sheet").assertDoesNotExist()
    }
    @Test fun rtlLargeTextKeepsActionsAvailableAcrossThemes() {
        var appearance by mutableStateOf(AppearancePreference.Light)
        val controller = WritingToolsController().apply {
            observe(WritingDraft("me", "chat", "teh note", 8, 8)); open(false, WritingScenario.Ready)
            choose(WritingOperation.Proofread); complete(session!!.id)
        }
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme(appearance) { WritingToolsSheet(controller) {} }
            }
        }
        AppearancePreference.entries.forEach { value ->
            rule.runOnIdle { appearance = value }
            rule.onNodeWithTag("writing.apply").assertIsDisplayed().assertIsEnabled()
            rule.onNodeWithTag("writing.discard").assertIsDisplayed()
        }
    }
}
