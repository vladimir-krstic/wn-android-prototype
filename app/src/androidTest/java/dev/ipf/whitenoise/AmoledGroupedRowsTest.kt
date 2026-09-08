package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.ui.settings.SettingsGroup
import dev.ipf.whitenoise.ui.settings.SettingsLink
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmoledGroupedRowsTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun mixedRowsShareOnePixelBoundaryAtNormalDensity() = verifyJoinedRows(1f, LayoutDirection.Ltr)
    @Test fun mixedRowsShareOnePixelBoundaryAtHighDensityInRtl() = verifyJoinedRows(3f, LayoutDirection.Rtl)

    private fun verifyJoinedRows(density: Float, direction: LayoutDirection) {
        var clicks = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 1f),
                LocalLayoutDirection provides direction,
            ) {
                WhiteNoiseTheme(appearance = AppearancePreference.Amoled) {
                    SettingsGroup(Modifier.testTag("group")) {
                        row { SettingsLink("First", onClick = { clicks++ }, modifier = Modifier.testTag("first")) }
                        item { Box(Modifier.fillMaxWidth().height(64.dp).testTag("middle")) { Text("Middle") } }
                        row { SettingsLink("Last", onClick = {}, modifier = Modifier.testTag("last")) }
                    }
                }
            }
        }
        val first = rule.onNodeWithTag("first").fetchSemanticsNode().boundsInRoot
        val middle = rule.onNodeWithTag("middle").fetchSemanticsNode().boundsInRoot
        val last = rule.onNodeWithTag("last").fetchSemanticsNode().boundsInRoot
        assertEquals(first.bottom, middle.top, 0f)
        assertEquals(middle.bottom, last.top, 0f)
        val group = rule.onNodeWithTag("group")
        val bounds = group.fetchSemanticsNode().boundsInRoot
        val pixels = group.captureToImage().toPixelMap()
        val x = pixels.width / 2
        listOf(middle.top, last.top).forEach { boundary ->
            val y = (boundary - bounds.top).toInt()
            assertEquals(Color.Black, pixels[x, y - 2])
            assertEquals(Color.White, pixels[x, y - 1])
            assertEquals(Color.Black, pixels[x, y])
            assertEquals(Color.Black, pixels[x, y + 1])
            // No rounded inner corners or gaps along either outside edge.
            val left = (middle.left - bounds.left).toInt()
            val right = (middle.right - bounds.left).toInt() - 1
            assertEquals(Color.White, pixels[left, y])
            assertEquals(Color.White, pixels[right, y])
        }
        rule.onNodeWithTag("first").performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }
}
