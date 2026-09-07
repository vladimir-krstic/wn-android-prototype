package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusedMessagePreviewTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun longTextEllipsizesAtFiveLinesAndRespectsLargeFontSize() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                WhiteNoiseTheme {
                    Box(Modifier.width(260.dp)) {
                        FocusedMessageText((1..100).joinToString("\n") { "Line $it of the complete message" }, "long")
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("message.actions.excerpt.long")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(5, layout.lineCount)
        assertTrue(layout.isLineEllipsized(4))
        assertEquals(2f, layout.layoutInput.density.fontScale, 0f)
        assertEquals(16f, layout.layoutInput.style.fontSize.value, 0f)
    }

    @Test fun mediaScalesProportionallyAndFitsItsReportedBoundsInRtl() {
        rule.setContent {
            WhiteNoiseTheme {
                CompositionLocalProvider(LocalFocusedMessagePreview provides true, LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.width(192.dp).testTag("media.bounds")) {
                        FocusedMediaPreview { Box(Modifier.fillMaxWidth().height(128.dp).testTag("media.artwork")) }
                    }
                }
            }
        }
        rule.onNodeWithTag("media.bounds").assertWidthIsEqualTo(192.dp).assertHeightIsEqualTo(96.dp)
        val bounds = rule.onNodeWithTag("media.bounds").fetchSemanticsNode().boundsInRoot
        val media = rule.onNodeWithTag("media.artwork").fetchSemanticsNode().boundsInRoot
        assertEquals(bounds.left, media.left, 1f)
        assertEquals(bounds.right, media.right, 1f)
        assertEquals(bounds.bottom, media.bottom, 1f)
    }
}
