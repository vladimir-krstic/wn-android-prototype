package dev.ipf.whitenoise

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ConversationProjection
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.conversation.stableComposerKeyboardConstraints
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.settings.IncognitoKeyboardScope
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Frame checks use controlled viewport resizing; device execution requires authorization. */
@OptIn(ExperimentalLayoutApi::class)
@RunWith(AndroidJUnit4::class)
class ComposerViewportFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var viewportHeight by mutableStateOf(600.dp)
    private val profile = ProfileFixtures.marmota
    private val chat = profile.chats.first { it.id == "catalog-direct-text" }
        .copy(draftText = "", draftAttachments = emptyList(), draftReplyMessageId = null)
    private val lastMessage = ConversationProjection.orderedEntries(chat)
        .filterIsInstance<ChatTimelineEntry.Message>().last().id

    private fun show() {
        rule.setContent {
            // Keep the keyboard deterministic while exercising the same changing constraints
            // that imePadding supplies to the production timeline and composer.
            IncognitoKeyboardScope(enabled = false, blocked = true) {
                WhiteNoiseTheme {
                    Box(Modifier.fillMaxWidth().height(viewportHeight)) {
                        ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                    }
                }
            }
        }
    }

    private fun bounds(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    @Test fun bottomMessageTracksComposerAndViewportOnEveryFrame() {
        show()
        rule.onNodeWithTag("conversation.timeline").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        val messageTag = "conversation.message.$lastMessage"
        val initial = bounds("conversation.composer.surface")
        val gap = initial.top - bounds(messageTag).bottom
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("conversation.composer.editor").performClick()
        var previousHeight = initial.height
        val heights = mutableListOf<Float>()
        repeat(30) { frame ->
            rule.runOnUiThread { viewportHeight = (600 - frame * 4).dp }
            rule.mainClock.advanceTimeBy(16)
            val composer = bounds("conversation.composer.surface")
            assertEquals(initial.width, composer.width, 1f)
            assertTrue(composer.height >= previousHeight - 1f)
            assertEquals("Messages must use the current frame’s composer height", gap, composer.top - bounds(messageTag).bottom, 2f)
            previousHeight = composer.height
            heights += composer.height
        }
        assertTrue(heights.distinct().size > 5)
        assertTrue(heights.last() > initial.height)
        rule.mainClock.autoAdvance = true
    }

    @Test fun readingOlderMessagesKeepsTheirPositionWhileComposerAndViewportChange() {
        show()
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.TXT-03"))
        val before = bounds("conversation.message.TXT-03")
        val width = bounds("conversation.composer.surface").width
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("conversation.composer.editor").performClick()
        repeat(30) { frame ->
            rule.runOnUiThread { viewportHeight = (600 - frame * 4).dp }
            rule.mainClock.advanceTimeBy(16)
            assertEquals(before.top, bounds("conversation.message.TXT-03").top, 1f)
            assertEquals(width, bounds("conversation.composer.surface").width, 1f)
        }
        rule.mainClock.autoAdvance = true
    }
    @Test fun composerTracksKeyboardInsetsAndCurveDuringOpeningAndClosing() {
        val ime = MutableWindowInsets()
        val source = MutableWindowInsets()
        val target = MutableWindowInsets()
        val insets = dev.ipf.whitenoise.ui.conversation.ComposerKeyboardInsets(ime, source, target)
        lateinit var clearFocus: () -> Unit
        rule.setContent {
            val focus = androidx.compose.ui.platform.LocalFocusManager.current
            clearFocus = { focus.clearFocus() }
            androidx.compose.runtime.CompositionLocalProvider(
                dev.ipf.whitenoise.ui.conversation.LocalComposerKeyboardInsets provides insets,
            ) {
                IncognitoKeyboardScope(enabled = false, blocked = true) {
                    WhiteNoiseTheme {
                        Box(Modifier.fillMaxWidth().height(600.dp).testTag("keyboard.testHost")) {
                            ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("conversation.timeline").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        val readingHeight = bounds("conversation.composer.surface").height
        val hostBottom = bounds("keyboard.testHost").bottom
        val keyboardHeight = with(rule.density) { 300.dp.roundToPx() }
        val growth = with(rule.density) { 32.dp.toPx() }
        val gap = with(rule.density) { 6.dp.toPx() }
        val navigationBottom = hostBottom - bounds("conversation.composer.surface").bottom - gap
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { target.insets = WindowInsets(bottom = keyboardHeight) }
        rule.onNodeWithTag("conversation.composer.editor").performClick()
        rule.mainClock.advanceTimeBy(16)
        listOf(0.1f, 0.22f, 0.49f, 0.76f, 0.93f, 1f).forEach { fraction ->
            val current = (keyboardHeight * fraction).toInt()
            rule.runOnUiThread { ime.insets = WindowInsets(bottom = current) }
            rule.mainClock.advanceTimeBy(16)
            val composer = bounds("conversation.composer.surface")
            assertEquals("Composer must sit on this frame's keyboard edge", hostBottom - maxOf(current.toFloat(), navigationBottom) - gap, composer.bottom, 2f)
            assertEquals("Expansion must follow the keyboard curve, not elapsed time", readingHeight + growth * fraction, composer.height, 2f)
        }
        rule.runOnUiThread {
            source.insets = WindowInsets(bottom = keyboardHeight)
            target.insets = WindowInsets(bottom = 0)
            clearFocus()
        }
        rule.mainClock.advanceTimeBy(16)
        listOf(0.92f, 0.73f, 0.46f, 0.21f, 0.1f).forEach { fraction ->
            val current = (keyboardHeight * fraction).toInt()
            rule.runOnUiThread { ime.insets = WindowInsets(bottom = current) }
            rule.mainClock.advanceTimeBy(16)
            val composer = bounds("conversation.composer.surface")
            assertEquals(hostBottom - maxOf(current.toFloat(), navigationBottom) - gap, composer.bottom, 2f)
            assertEquals(readingHeight + growth * fraction, composer.height, 2f)
        }
        rule.runOnUiThread { ime.insets = WindowInsets(bottom = 0); source.insets = WindowInsets(bottom = 0) }
        rule.mainClock.advanceTimeBy(16)
        assertEquals(readingHeight, bounds("conversation.composer.surface").height, 1f)
        rule.mainClock.autoAdvance = true
    }

    @Test fun keyboardFramesMoveTheComposerWithoutRecomposingItsConstraintContent() {
        val ime = MutableWindowInsets()
        val source = MutableWindowInsets()
        val target = MutableWindowInsets()
        val insets = dev.ipf.whitenoise.ui.conversation.ComposerKeyboardInsets(ime, source, target)
        val height = with(rule.density) { 300.dp.roundToPx() }
        target.insets = WindowInsets(bottom = height)
        var compositions = 0
        var measuredContentHeight = 0
        rule.setContent {
            Box(Modifier.fillMaxWidth().height(600.dp).testTag("stable.host")
                .windowInsetsPadding(ime)) {
                BoxWithConstraints(Modifier.fillMaxSize()
                    .stableComposerKeyboardConstraints(insets, WindowInsets(0))) {
                    androidx.compose.runtime.SideEffect { compositions++; measuredContentHeight = constraints.maxHeight }
                    Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).size(48.dp)
                        .testTag("stable.composer"))
                }
            }
        }
        val originalBottom = bounds("stable.composer").bottom
        assertTrue(measuredContentHeight > 0)
        var initialCompositions = 0
        rule.runOnIdle { initialCompositions = compositions }
        rule.mainClock.autoAdvance = false
        listOf(0.1f, 0.3f, 0.5f, 0.85f, 1f).forEach { fraction ->
            val bottom = (height * fraction).toInt()
            rule.runOnUiThread { ime.insets = WindowInsets(bottom = bottom) }
            rule.mainClock.advanceTimeBy(16)
            assertEquals(originalBottom - bottom, bounds("stable.composer").bottom, 1f)
            rule.runOnIdle { assertEquals("Keyboard frames must not subcompose the editor tree", initialCompositions, compositions) }
        }
        rule.runOnUiThread { source.insets = WindowInsets(bottom = height); target.insets = WindowInsets(0) }
        rule.mainClock.advanceTimeBy(16)
        rule.runOnIdle { initialCompositions = compositions }
        listOf(0.85f, 0.5f, 0.3f, 0.1f, 0f).forEach { fraction ->
            val bottom = (height * fraction).toInt()
            rule.runOnUiThread { ime.insets = WindowInsets(bottom = bottom) }
            rule.mainClock.advanceTimeBy(16)
            assertEquals(originalBottom - bottom, bounds("stable.composer").bottom, 1f)
            rule.runOnIdle { assertEquals(initialCompositions, compositions) }
        }
        rule.mainClock.autoAdvance = true
    }

}
