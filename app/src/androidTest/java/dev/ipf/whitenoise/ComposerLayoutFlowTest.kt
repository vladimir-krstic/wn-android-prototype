package dev.ipf.whitenoise

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposerLayoutFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    private lateinit var clearEditorFocus: () -> Unit

    private fun show(initialText: String = "", rtl: Boolean = false, fontScale: Float = 1f, blockKeyboard: Boolean = false) {
        val profile = ProfileFixtures.marmota
        val chat = mutableStateOf(profile.chats.first { it.id == "fiatjaf" }.copy(
            draftText = initialText, draftAttachments = emptyList(), draftReplyMessageId = null,
        ))
        rule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val focus = androidx.compose.ui.platform.LocalFocusManager.current
            clearEditorFocus = { focus.clearFocus() }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    if (rtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale),
            ) {
            dev.ipf.whitenoise.ui.settings.IncognitoKeyboardScope(enabled = false, blocked = blockKeyboard) {
            WhiteNoiseTheme {
                ConversationScreen(profile, chat.value, {}, { true }, {}, {}, {},
                    onDraftTextChanged = { chat.value = chat.value.copy(draftText = it) })
            }
            }
            }
        }
    }

    private fun surface() = rule.onNodeWithTag("conversation.composer.surface")

    private fun assertInternalActions() {
        val container = bounds()
        val editor = rule.onNodeWithTag("conversation.composer.editor").fetchSemanticsNode().boundsInRoot
        assertEquals(container.width, editor.width, 1f)
        listOf("conversation.attachment.add", "conversation.composer.emoji", "conversation.dictation.start").forEach { tag ->
            val button = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must remain inside the composer", button.left >= container.left - 1f &&
                button.right <= container.right + 1f && button.bottom <= container.bottom + 1f)
            val sharedLineBox = with(rule.density) { androidx.compose.ui.unit.Dp(4f).toPx() }
            assertTrue("Text line box may share only 4 dp with $tag", editor.bottom <= button.top + sharedLineBox + 1f)
        }
    }

    private fun assertReadingRow(rtl: Boolean = false) {
        val editor = rule.onNodeWithTag("conversation.composer.editor").assertIsNotFocused().fetchSemanticsNode().boundsInRoot
        val emoji = rule.onNodeWithTag("conversation.composer.emoji").fetchSemanticsNode().boundsInRoot
        val mic = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertEquals(emoji.center.y, editor.center.y, 1f)
        if (rtl) {
            assertTrue(editor.right <= emoji.left + 1f)
            assertTrue(editor.left >= mic.right - 1f)
        } else {
            assertTrue(editor.left >= emoji.right - 1f)
            assertTrue(editor.right <= mic.left + 1f)
        }
        rule.onNodeWithTag("conversation.attachment.add").assertIsDisplayed()
        rule.onNodeWithTag("conversation.composer.placeholder", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun emptyUnfocusedComposerUsesOneRowAndFocusOpensTwoWithoutChangingWidth() {
        show()
        val reading = bounds()
        val host = rule.onNodeWithTag("conversation.composer.host").fetchSemanticsNode().boundsInRoot
        val margin = with(rule.density) { androidx.compose.ui.unit.Dp(16f).toPx() }
        assertEquals(host.width - 2 * margin, reading.width, 1f)
        assertReadingRow()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick().assertIsFocused()
        val editing = bounds()
        assertEquals(reading.width, editing.width, 1f)
        assertTrue(editing.height > reading.height)
        assertInternalActions()
        rule.runOnIdle { clearEditorFocus() }
        assertReadingRow()
        assertEquals(reading.height, bounds().height, 1f)
        editor.performClick().performTextReplacement("Hi")
        rule.runOnIdle { clearEditorFocus() }
        assertEquals(editing.height, bounds().height, 1f)
        assertInternalActions()
        editor.performClick().performTextReplacement("One\nTwo\nThree")
        assertEquals(reading.width, bounds().width, 1f)
        assertTrue(bounds().height > editing.height)
        editor.performTextReplacement("")
        editor.assertIsFocused()
        assertEquals(editing.height, bounds().height, 1f)
        assertInternalActions()
        rule.runOnIdle { clearEditorFocus() }
        assertReadingRow()
        assertEquals(reading.height, bounds().height, 1f)
    }

    @Test fun emptyReadingRowAdaptsToLargeTextAndRtl() {
        show(rtl = true, fontScale = 2f)
        assertReadingRow(rtl = true)
        val readingWidth = bounds().width
        rule.onNodeWithTag("conversation.composer.editor").performClick()
        assertInternalActions()
        assertEquals(readingWidth, bounds().width, 1f)
    }

    @Test fun largeTextAndRtlKeepBothRowsAndActionsInsideTheContainer() {
        show("Hello", rtl = true, fontScale = 2f)
        assertInternalActions()
        val add = rule.onNodeWithTag("conversation.attachment.add").fetchSemanticsNode().boundsInRoot
        val microphone = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertTrue(add.center.x > microphone.center.x)
    }

    @Test fun multilineComposerShrinksWhileIconsRemainCentered() {
        show("One\nTwo\nThree")
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val textLayout = layouts.single()
        val lastLineBottom = editor.fetchSemanticsNode().boundsInRoot.top +
            with(rule.density) { androidx.compose.ui.unit.Dp(12f).toPx() } +
            textLayout.getLineBottom(textLayout.lineCount - 1)
        val toolbarTop = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot.top
        val reduction = with(rule.density) { androidx.compose.ui.unit.Dp(4f).toPx() }
        assertEquals("Toolbar shares only the lower 4 dp of the line box", lastLineBottom - reduction, toolbarTop, 1f)
        val microphone = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        val icon = rule.onNodeWithTag("conversation.dictation.icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("Artwork stays centered in its button", microphone.center.y, icon.center.y, 1f)
        assertEquals("The composer must shrink, not move empty space below the icons",
            lastLineBottom - bounds().top + microphone.height - reduction, bounds().height, 1f)
    }

    @Test fun addingAndDeletingLinesNeverClipsOrScrollsExistingTextDuringGrowth() {
        show("First line\nSecond", blockKeyboard = true)
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick().performTextInputSelection(TextRange("First line\nSecond".length))
        val reference = surface().captureToImage().toPixelMap()
        val left = with(rule.density) { androidx.compose.ui.unit.Dp(14f).roundToPx() }
        val top = with(rule.density) { androidx.compose.ui.unit.Dp(12f).roundToPx() }
        val right = with(rule.density) { androidx.compose.ui.unit.Dp(140f).roundToPx() }
        val bottom = with(rule.density) { androidx.compose.ui.unit.Dp(36f).roundToPx() }
        rule.mainClock.autoAdvance = false
        listOf("First line\nSecond\nThird", "First line\nSecond").forEach { draft ->
            editor.performTextReplacement(draft)
            repeat(14) {
                rule.mainClock.advanceTimeBy(16)
                val frame = surface().captureToImage().toPixelMap()
                var changed = 0
                for (y in top until bottom) for (x in left until right) {
                    if (frame[x, y] != reference[x, y]) changed++
                }
                assertEquals("The first line must stay rendered at the same position inside the surface", 0, changed)
            }
        }
        rule.mainClock.autoAdvance = true
    }

    @Test fun placeholderFadesAtFixedPositionsAndReversesWithoutSliding() {
        show(blockKeyboard = true)
        val reading = bounds()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        val placeholder = rule.onNodeWithTag("conversation.composer.placeholder", useUnmergedTree = true)
        fun inkPixels(): Int {
            val pixels = placeholder.captureToImage().toPixelMap()
            var ink = 0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                if (pixels[x, y].red < 0.5f) ink++
            }
            return ink
        }
        val ink = inkPixels()
        assertTrue(ink > 0)
        val readingX = placeholder.fetchSemanticsNode().boundsInRoot.left
        val editingX = reading.left + with(rule.density) { androidx.compose.ui.unit.Dp(14f).toPx() }
        fun assertFixedPosition() {
            val x = placeholder.fetchSemanticsNode().boundsInRoot.left
            assertTrue("Placeholder must stay at an endpoint, never slide between them",
                kotlin.math.abs(x - readingX) <= 1f || kotlin.math.abs(x - editingX) <= 1f)
        }
        rule.mainClock.autoAdvance = false
        editor.performClick()
        val openingInk = buildList {
            repeat(12) {
                rule.mainClock.advanceTimeBy(16)
                assertFixedPosition()
                add(inkPixels())
            }
        }
        assertTrue("Old placeholder fades before the new one appears", openingInk.min() < ink * 0.5f)
        assertTrue(openingInk.last() >= ink * 0.9f)
        assertEquals(editingX, placeholder.fetchSemanticsNode().boundsInRoot.left, 1f)
        rule.runOnUiThread { clearEditorFocus() }
        repeat(12) { rule.mainClock.advanceTimeBy(16); assertFixedPosition() }
        assertEquals(reading.height, bounds().height, 1f)
        assertEquals(readingX, placeholder.fetchSemanticsNode().boundsInRoot.left, 1f)
        editor.performClick()
        rule.mainClock.advanceTimeBy(48)
        rule.runOnUiThread { clearEditorFocus() }
        repeat(12) { rule.mainClock.advanceTimeBy(16); assertFixedPosition() }
        assertEquals(reading.height, bounds().height, 1f)
        rule.mainClock.autoAdvance = true
    }

    @Test fun firstCharacterHidesVoiceAndKeepsEmojiAndDictation() {
        show()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        rule.onNodeWithTag("conversation.voice.icon", useUnmergedTree = true).assertExists()
        editor.performClick().performTextInput(" ")
        rule.onNodeWithTag("conversation.voice.icon", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("conversation.composer.emoji").assertIsDisplayed()
        rule.onNodeWithTag("conversation.dictation.start").assertIsDisplayed()
        editor.performTextReplacement("")
        rule.onNodeWithTag("conversation.voice.icon", useUnmergedTree = true).assertExists()
    }

    @Test fun emojiReplacesSelectionAndBackKeepsDraft() {
        show("Hello world!")
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick().performTextInputSelection(TextRange(6, 11))
        rule.onNodeWithTag("conversation.composer.emoji").performClick()
        val emoji = dev.ipf.whitenoise.model.ReactionCatalog.search("").first().emoji.first()
        rule.onNodeWithTag("emoji.picker.item.recent.0").performClick()
        editor.assertTextContains("Hello $emoji!")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(6 + emoji.length)))
        rule.onNodeWithTag("conversation.composer.emoji").performClick()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("emoji.picker").assertDoesNotExist()
        editor.assertTextContains("Hello $emoji!")
    }
    private fun bounds() = surface().fetchSemanticsNode().boundsInRoot
    private fun action(label: String) {
        val action = surface().fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        rule.runOnIdle { assertTrue(action.action()) }
    }

    @Test fun additionalLinesGrowVerticallyWithControlsInsideTheSameWidth() {
        show()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick().performTextReplacement("One\nTwo\nThree")
        val narrow = bounds()
        editor.performTextInput("\nFour")
        val wide = bounds()
        assertTrue(wide.height > narrow.height)
        val add = rule.onNodeWithTag("conversation.attachment.add").fetchSemanticsNode().boundsInRoot
        val text = editor.fetchSemanticsNode().boundsInRoot
        val dictation = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertEquals(narrow.width, wide.width, 1f)
        assertEquals(add.left, wide.left, 1f)
        val emoji = rule.onNodeWithTag("conversation.composer.emoji").fetchSemanticsNode().boundsInRoot
        assertEquals(wide.width, text.width, 1f)
        assertEquals(dictation.bottom, emoji.bottom, 1f)
        assertEquals(add.width * 32f / 48f, emoji.center.x - add.center.x, 1f)
        val send = rule.onNodeWithTag("conversation.send.icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(add.width * 40f / 48f, send.center.x - dictation.center.x, 1f)
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
        assertEquals(narrow.width, wide.width, 1f)
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(wide.width, bounds().width, 1f)
        editor.assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2, 14)))
    }

    private fun assertWideFrame(expectedWidth: Float) {
        val surface = bounds()
        val editor = rule.onNodeWithTag("conversation.composer.editor").fetchSemanticsNode().boundsInRoot
        val add = rule.onNodeWithTag("conversation.attachment.add").fetchSemanticsNode().boundsInRoot
        assertEquals(expectedWidth, surface.width, 1f)
        val emoji = rule.onNodeWithTag("conversation.composer.emoji").fetchSemanticsNode().boundsInRoot
        assertEquals(surface.width, editor.width, 1f)
        val dictation = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertEquals(dictation.bottom, emoji.bottom, 1f)
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

    @Test fun manualExpansionAndCollapseOnlyChangeHeight() {
        show("Short draft")
        val narrow = bounds()
        rule.mainClock.autoAdvance = false
        action("Expand Message")
        val expansion = buildList {
            repeat(100) { rule.mainClock.advanceTimeBy(16); add(bounds()) }
        }
        val wide = expansion.last()
        assertEquals(narrow.width, wide.width, 1f)
        assertTrue(expansion.take(10).any {
            it.height > narrow.height + 2f
        })
        action("Collapse Message")
        val contraction = buildList {
            repeat(100) { rule.mainClock.advanceTimeBy(16); add(bounds()) }
        }
        assertTrue(contraction.take(10).any {
            it.height < wide.height - 2f
        })
        (expansion + contraction).forEach { assertEquals(narrow.width, it.width, 1f) }
        contraction.zipWithNext().forEach { (before, after) ->
            assertTrue("Collapse must not grow again at the intrinsic-size handoff", after.height <= before.height + 1f)
        }
        assertEquals(narrow.width, contraction.last().width, 1f)
        assertEquals(narrow.height, contraction.last().height, 2f)
        rule.mainClock.autoAdvance = true
    }

    @Test fun shortDraftDownwardDragFollowsFingerWithoutWaitingForWidth() {
        show("Short draft")
        val compact = bounds()
        action("Expand Message")
        val expanded = bounds()
        rule.mainClock.autoAdvance = false
        val start = Offset(expanded.center.x, expanded.top + 8f)
        val distance = (expanded.height - compact.height) * 0.25f
        rule.onRoot().performTouchInput {
            down(start)
            moveTo(start + Offset(0f, distance), delayMillis = 100)
        }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(expanded.height - distance, bounds().height, 3f)
        rule.onRoot().performTouchInput {
            moveTo(start + Offset(0f, distance * 2f), delayMillis = 100)
        }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(expanded.height - distance * 2f, bounds().height, 3f)
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.mainClock.autoAdvance = true
    }

    @Test fun editingWhileExpandedCollapsesToCurrentDraftHeightWithoutFinalJump() {
        show("One\nTwo\nThree\nFour")
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick()
        val compact = bounds()
        action("Expand Message")
        editor.performTextReplacement("One\nTwo\nThree\nFour\nFive\nSix")
        rule.mainClock.autoAdvance = false
        action("Collapse Message")
        val frames = buildList {
            repeat(100) { rule.mainClock.advanceTimeBy(16); add(bounds()) }
        }
        frames.zipWithNext().forEach { (before, after) ->
            assertTrue("Current draft height must be the initial collapse target", after.height <= before.height + 1f)
        }
        assertTrue(frames.last().height > compact.height)
        editor.assertIsFocused().assertTextContains("One\nTwo\nThree\nFour\nFive\nSix")
        rule.mainClock.autoAdvance = true
    }

    @Test fun reversingExpansionKeepsCurrentGeometryAndSelection() {
        show("A short draft")
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performClick().performTextInputSelection(TextRange(2, 7))
        val compact = bounds()
        rule.mainClock.autoAdvance = false
        action("Expand Message")
        rule.mainClock.advanceTimeBy(96)
        val interrupted = bounds()
        assertTrue(interrupted.height > compact.height)
        action("Collapse Message")
        assertEquals(interrupted.height, bounds().height, 1f)
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(compact.height, bounds().height, 2f)
        editor.assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2, 7)))
        rule.mainClock.autoAdvance = true
    }
}
