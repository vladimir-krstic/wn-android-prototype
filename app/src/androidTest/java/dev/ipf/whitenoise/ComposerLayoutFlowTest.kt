package dev.ipf.whitenoise

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposerLayoutFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    private fun show(initialText: String = "") {
        val profile = ProfileFixtures.marmota
        val chat = mutableStateOf(profile.chats.first { it.id == "fiatjaf" }.copy(
            draftText = initialText, draftAttachments = emptyList(), draftReplyMessageId = null,
        ))
        rule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(profile, chat.value, {}, { true }, {}, {}, {},
                    onDraftTextChanged = { chat.value = chat.value.copy(draftText = it) })
            }
        }
    }

    private fun surface() = rule.onNodeWithTag("conversation.composer.surface")
    private fun bounds() = surface().fetchSemanticsNode().boundsInRoot
    private fun action(label: String) {
        val action = surface().fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        rule.runOnIdle { assertTrue(action.action()) }
    }

    @Test fun fourthLineIntegratesAddAndKeepsTextAboveControlsThenContracts() {
        show()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick().performTextReplacement("One\nTwo\nThree")
        val narrow = bounds()
        editor.performTextInput("\nFour")
        val wide = bounds()
        val add = rule.onNodeWithTag("conversation.attachment.add").fetchSemanticsNode().boundsInRoot
        val text = editor.fetchSemanticsNode().boundsInRoot
        val dictation = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertTrue(wide.width > narrow.width)
        assertEquals(add.left, wide.left, 1f)
        assertEquals(wide.width, text.width, 1f)
        assertTrue(text.bottom <= dictation.top + 1f)
        editor.assertIsFocused().assertTextContains("One\nTwo\nThree\nFour")
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithTag("conversation.attachment.menu").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        editor.performTextReplacement("One\nTwo\nThree")
        assertEquals(narrow.width, bounds().width, 1f)
    }

    @Test fun wrappedTextStaysWideAfterReflowAndPreservesSelection() {
        show()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        val narrow = bounds()
        val text = "A naturally wrapped message without line breaks. ".repeat(8)
        editor.performClick().performTextReplacement(text)
        editor.performTextInputSelection(TextRange(2, 14))
        val wide = bounds()
        assertTrue(wide.width > narrow.width)
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(wide.width, bounds().width, 1f)
        editor.assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2, 14)))
    }

    private fun assertWideFrame(expectedWidth: Float) {
        val surface = bounds()
        val editor = rule.onNodeWithTag("conversation.composer.editor").fetchSemanticsNode().boundsInRoot
        val add = rule.onNodeWithTag("conversation.attachment.add").fetchSemanticsNode().boundsInRoot
        assertEquals(expectedWidth, surface.width, 1f)
        assertEquals(surface.width, editor.width, 1f)
        assertEquals(surface.left, add.left, 1f)
    }

    @Test fun fourLineCollapseActionOnlyChangesHeightOnEveryFrame() {
        show("One\nTwo\nThree\nFour")
        val compact = bounds()
        action("Expand Message")
        val expanded = bounds()
        assertTrue(expanded.height > compact.height)
        assertEquals(compact.width, expanded.width, 1f)
        rule.mainClock.autoAdvance = false
        action("Collapse Message")
        repeat(100) {
            rule.mainClock.advanceTimeBy(16)
            assertWideFrame(compact.width)
        }
        assertEquals(compact.height, bounds().height, 2f)
        rule.mainClock.autoAdvance = true
    }

    @Test fun wrappedDraftPullTracksHeightWithoutNarrowingDuringDragOrSettling() {
        show("A naturally wrapped message without line breaks. ".repeat(8))
        val compact = bounds()
        action("Expand Message")
        val expanded = bounds()
        assertTrue(expanded.height > compact.height)
        rule.mainClock.autoAdvance = false
        // Inject at fixed root coordinates so layout movement does not move the drag origin.
        val start = Offset(expanded.center.x, expanded.top + 8f)
        rule.onRoot().performTouchInput {
            down(start)
            moveTo(start + Offset(0f, (expanded.height - compact.height) * 0.25f), delayMillis = 100)
        }
        repeat(20) {
            rule.mainClock.advanceTimeBy(16)
            assertWideFrame(compact.width)
        }
        assertTrue(bounds().height < expanded.height - 2f)
        rule.onRoot().performTouchInput {
            moveTo(start + Offset(0f, (expanded.height - compact.height) * 0.8f), delayMillis = 100)
            up()
        }
        repeat(100) {
            rule.mainClock.advanceTimeBy(16)
            assertWideFrame(compact.width)
        }
        assertEquals(compact.height, bounds().height, 2f)
        rule.mainClock.autoAdvance = true
    }

    @Test fun manualExpansionGrowsHeightBeforeWidthAndReversesOnCollapse() {
        show()
        val narrow = bounds()
        rule.mainClock.autoAdvance = false
        action("Expand Message")
        val expansion = buildList {
            repeat(100) { rule.mainClock.advanceTimeBy(16); add(bounds()) }
        }
        val wide = expansion.last()
        assertTrue(wide.width > narrow.width)
        assertTrue(expansion.any { it.height > narrow.height + 2f && abs(it.width - narrow.width) <= 1f })
        expansion.filter { it.width > narrow.width + 1f }.forEach { assertEquals(wide.height, it.height, 2f) }
        action("Collapse Message")
        val contraction = buildList {
            repeat(100) { rule.mainClock.advanceTimeBy(16); add(bounds()) }
        }
        contraction.filter { it.height < wide.height - 2f }.forEach { assertEquals(narrow.width, it.width, 1f) }
        assertEquals(narrow.height, contraction.last().height, 2f)
        rule.mainClock.autoAdvance = true
    }
}
