package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceColorPolicyTest {
    @Test
    fun defaultsRemainMonochromeAndUnspecified() {
        val defaults = ProfileSettings().colors
        AppearanceColorTheme.entries.forEach { theme ->
            assertEquals(ThemeColorOverrides(), defaults.forTheme(theme))
        }
    }

    @Test
    fun themeUpdatesStayIndependent() {
        val changed = AppearanceColorPreferences().updateTheme(AppearanceColorTheme.Dark) {
            it.copy(actionArgb = 0xFF1D4ED8L, mineBubbleArgb = 0xFF15803DL)
        }
        assertEquals(0xFF1D4ED8L, changed.dark.actionArgb)
        assertEquals(0xFF15803DL, changed.dark.mineBubbleArgb)
        assertEquals(ThemeColorOverrides(), changed.light)
        assertEquals(ThemeColorOverrides(), changed.amoled)
    }

    @Test
    fun perChatBubbleTakesPrecedenceOverGlobal() {
        assertEquals(0xFFB91C1CL, AppearanceColorPolicy.effectiveBubble(0xFFB91C1CL, 0xFF1D4ED8L))
        assertEquals(0xFF1D4ED8L, AppearanceColorPolicy.effectiveBubble(null, 0xFF1D4ED8L))
        assertNull(AppearanceColorPolicy.effectiveBubble(null, null))
    }

    @Test
    fun hexParsingRequiresSixOpaqueRgbDigits() {
        assertEquals(0xFFA1B2C3L, AppearanceColorPolicy.parseHex(" #A1b2C3 "))
        assertEquals("#A1B2C3", AppearanceColorPolicy.formatHex(0xFFA1B2C3L))
        assertNull(AppearanceColorPolicy.parseHex("#123"))
        assertNull(AppearanceColorPolicy.parseHex("#80112233"))
        assertNull(AppearanceColorPolicy.parseHex("blue"))
    }

    @Test
    fun everyPresetChoosesAaReadableText() {
        AppearanceColorPolicy.presets.forEach { background ->
            val resolved = AppearanceColorPolicy.readable(background)
            assertTrue(resolved != null)
            assertTrue(
                AppearanceColorPolicy.contrastRatio(resolved!!.contentArgb, background) >=
                    AppearanceColorPolicy.MINIMUM_TEXT_CONTRAST,
            )
        }
        assertEquals(10, AppearanceColorPolicy.presets.distinct().size)
    }

    @Test
    fun hsvRoundTripPreservesRepresentativeColors() {
        listOf(0xFFB91C1CL, 0xFF15803DL, 0xFF1D4ED8L, 0xFFBE185DL).forEach { color ->
            val roundTrip = AppearanceColorPolicy.fromHsv(AppearanceColorPolicy.toHsv(color))
            val channelError = listOf(16, 8, 0).maxOf { shift ->
                kotlin.math.abs(((color shr shift) and 0xFF) - ((roundTrip shr shift) and 0xFF))
            }
            assertTrue("${AppearanceColorPolicy.formatHex(color)} -> ${AppearanceColorPolicy.formatHex(roundTrip)}", channelError <= 1)
        }
    }
}
