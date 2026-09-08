package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.MessageDocuments
import dev.ipf.whitenoise.ui.conversation.MessageDocumentContent
import dev.ipf.whitenoise.ui.conversation.MessageTextWithFooter
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class InlineTimestampLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun shortMessageAndTimeShareABaselineWithoutChangingAuthoredText() {
        lateinit var bodyLayout: TextLayoutResult
        lateinit var timeLayout: TextLayoutResult
        rule.setContent {
            WhiteNoiseTheme {
                Column(Modifier.widthIn(max = 300.dp).width(IntrinsicSize.Max)) {
                    MessageTextWithFooter(footer = {
                        Text("10:30 AM", Modifier.testTag("time"), style = MaterialTheme.typography.labelSmall,
                            onTextLayout = { timeLayout = it })
                    }, modifier = Modifier.testTag("bubble")) { onLayout ->
                        Text("Hello", Modifier.testTag("body"), style = MaterialTheme.typography.bodyLarge,
                            onTextLayout = { bodyLayout = it; onLayout(it) })
                    }
                }
            }
        }
        val body = rule.onNodeWithTag("body").assertTextEquals("Hello").fetchSemanticsNode().boundsInRoot
        val time = rule.onNodeWithTag("time").fetchSemanticsNode().boundsInRoot
        val bubble = rule.onNodeWithTag("bubble").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle {
            assertEquals(1, bodyLayout.lineCount)
            assertTrue(time.left > body.right)
            assertTrue(abs(body.top + bodyLayout.lastBaseline - time.top - timeLayout.lastBaseline) < 1.5f)
            assertTrue(abs(bubble.right - time.right) < 1.5f)
            assertTrue(time.bottom <= bubble.bottom)
        }
    }

    @Test fun formattedMultiParagraphMessageKeepsOneFooterInsideItsWidth() {
        rule.setContent {
            WhiteNoiseTheme {
                MessageDocumentContent(
                    MessageDocuments.parse("**A longer first paragraph**\n\nLast line"), emptyList(), {},
                    modifier = Modifier.width(260.dp).testTag("document"),
                    footer = { Text("10:30 AM", Modifier.testTag("time"), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        val document = rule.onNodeWithTag("document").fetchSemanticsNode().boundsInRoot
        val time = rule.onNodeWithTag("time").fetchSemanticsNode().boundsInRoot
        val lastLine = rule.onNodeWithText("Last line").fetchSemanticsNode().boundsInRoot
        assertTrue(abs(document.right - time.right) < 1.5f)
        assertTrue(time.top >= lastLine.top && time.top < lastLine.bottom)
        assertTrue(time.bottom <= document.bottom)
        rule.onAllNodesWithTag("time").assertCountEquals(1)
    }
}
