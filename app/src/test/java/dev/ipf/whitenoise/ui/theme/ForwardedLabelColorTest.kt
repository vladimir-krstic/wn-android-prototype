package dev.ipf.whitenoise.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.ipf.whitenoise.model.AppearanceColorPolicy
import dev.ipf.whitenoise.model.AppearanceColorTheme
import dev.ipf.whitenoise.model.AppearancePreference
import org.junit.Assert.*
import org.junit.Test

class ForwardedLabelColorTest {
    @Test fun grayAttributionIsQuieterThanBodyAndReadableInEveryTheme() {
        AppearancePreference.entries.forEach { appearance ->
            listOf(false, true).forEach { dark ->
                val scheme = whiteNoiseColorScheme(appearance, dark)
                val bubbles = defaultMessageBubbleColors(scheme, AppearanceColorTheme.resolve(appearance, dark))
                listOf(bubbles.mineContainer to bubbles.mineContent, bubbles.otherContainer to bubbles.otherContent,
                    scheme.surfaceContainerHigh to scheme.onSurface).forEach { (container, text) ->
                    val gray = forwardedLabelColor(container, text)
                    assertEquals(gray.red, gray.green, 0f)
                    assertEquals(gray.green, gray.blue, 0f)
                    assertTrue(ratio(gray, container) >= 4.5f)
                    assertTrue(ratio(gray, container) < ratio(text, container))
                }
            }
        }
    }
    @Test fun customBubblesKeepAccessibleGrayAcrossPresetsAndAllNeutralTones() {
        val backgrounds = AppearanceColorPolicy.presets + (0..255).map { 0xFF000000L or (it.toLong() * 0x010101L) }
        backgrounds.forEach { argb ->
            val container = colorFromOpaqueArgb(argb)
            val content = colorFromOpaqueArgb(AppearanceColorPolicy.readable(argb)!!.contentArgb)
            assertTrue(ratio(forwardedLabelColor(container, content), container) >= 4.5f)
        }
    }
    private fun ratio(a: Color, b: Color) =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
}
