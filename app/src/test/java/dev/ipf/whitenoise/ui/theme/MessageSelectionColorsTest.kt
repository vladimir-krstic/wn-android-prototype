package dev.ipf.whitenoise.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.ipf.whitenoise.model.AppearanceColorPolicy
import dev.ipf.whitenoise.model.AppearanceColorTheme
import dev.ipf.whitenoise.model.AppearancePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSelectionColorsTest {
    @Test fun blackBubbleOnLightCanvasKeepsBothHandlesAndSelectedTextVisible() {
        val colors = messageSelectionColors(Color.Black, Color.White, WhiteNoiseLightColors.surface)
        assertTrue(ratio(colors.handleColor, Color.Black) >= 3f)
        assertTrue(ratio(colors.handleColor, WhiteNoiseLightColors.surface) >= 3f)
        assertTrue(ratio(colors.backgroundColor, Color.Black) >= 3f)
        assertTrue(ratio(Color.White, colors.backgroundColor) >= 4.5f)
    }

    @Test fun incomingOutgoingAndAgentTextMeetContrastTargetsInEveryTheme() {
        AppearancePreference.entries.forEach { appearance ->
            listOf(false, true).forEach { systemDark ->
                val scheme = whiteNoiseColorScheme(appearance, systemDark)
                val bubbles = defaultMessageBubbleColors(scheme, AppearanceColorTheme.resolve(appearance, systemDark))
                listOf(
                    bubbles.mineContainer to bubbles.mineContent,
                    bubbles.otherContainer to bubbles.otherContent,
                    scheme.surfaceContainerHigh to scheme.onSurface,
                ).forEach { (container, text) ->
                    val context = "$appearance / systemDark=$systemDark / $container"
                    val colors = messageSelectionColors(container, text, scheme.surface)
                    assertTrue("$context: handle inside", ratio(colors.handleColor, container) >= 3f)
                    assertTrue("$context: handle outside", ratio(colors.handleColor, scheme.surface) >= 3f)
                    assertTrue("$context: highlight", ratio(colors.backgroundColor, container) >= 3f)
                    assertTrue("$context: selected text", ratio(text, colors.backgroundColor) >= 4.5f)
                }
            }
        }
    }

    @Test fun pureBlackOutlineThemeUsesLightHandlesAndAVisibleNeutralHighlight() {
        val colors = messageSelectionColors(Color.Black, Color.White, Color.Black)
        assertEquals(Color.White, colors.handleColor)
        assertEquals(colors.backgroundColor.red, colors.backgroundColor.green, 0f)
        assertEquals(colors.backgroundColor.green, colors.backgroundColor.blue, 0f)
        assertTrue(colors.backgroundColor.luminance() > 0f)
    }

    @Test fun customBubblesPreserveReadableSelectedTextAcrossPresetsAndAllNeutralTones() {
        val backgrounds = AppearanceColorPolicy.presets + (0..255).map { 0xFF000000L or (it.toLong() * 0x010101L) }
        backgrounds.forEach { argb ->
            val readable = AppearanceColorPolicy.readable(argb)!!
            val text = colorFromOpaqueArgb(readable.contentArgb)
            val container = colorFromOpaqueArgb(argb)
            listOf(WhiteNoiseLightColors.surface, WhiteNoiseDarkColors.surface, Color.Black).forEach { surrounding ->
                val colors = messageSelectionColors(container, text, surrounding)
                assertTrue("$argb: selected text", ratio(text, colors.backgroundColor) >= 4.5f)
                assertTrue("$argb: selection differs", ratio(container, colors.backgroundColor) > 1.1f)
            }
        }
    }

    private fun ratio(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
}
