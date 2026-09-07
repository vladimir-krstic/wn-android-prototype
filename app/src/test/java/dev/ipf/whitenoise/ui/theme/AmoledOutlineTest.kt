package dev.ipf.whitenoise.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.AppearanceColorPolicy
import dev.ipf.whitenoise.model.AppearanceColorPreferences
import dev.ipf.whitenoise.model.AppearanceColorTheme
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.ThemeColorOverrides
import org.junit.Assert.*
import org.junit.Test

class AmoledOutlineTest {
    private val theme = AppearanceColorTheme.AmoledOutline

    @Test fun explicitOutlineIsIndependentOfSystemTheme() {
        listOf(false, true).forEach { systemDark ->
            assertEquals(theme, AppearanceColorTheme.resolve(AppearancePreference.AmoledOutline, systemDark))
            assertSame(WhiteNoiseAmoledOutlineColors, whiteNoiseColorScheme(AppearancePreference.AmoledOutline, systemDark))
        }
        assertSame(WhiteNoiseAmoledColors, whiteNoiseColorScheme(AppearancePreference.Amoled, false))
        assertNotEquals(WhiteNoiseAmoledColors.surfaceContainerHigh, WhiteNoiseAmoledOutlineColors.surfaceContainerHigh)
    }

    @Test fun restingSurfacesAreBlackWithWhiteBoundariesAndNoTint() {
        val c = WhiteNoiseAmoledOutlineColors
        listOf(c.background, c.surface, c.surfaceBright, c.surfaceDim, c.surfaceVariant,
            c.surfaceContainerLowest, c.surfaceContainerLow, c.surfaceContainer,
            c.surfaceContainerHigh, c.surfaceContainerHighest,
            c.primaryContainer, c.secondaryContainer, c.tertiaryContainer).forEach {
            assertEquals(Color.Black, it)
        }
        assertEquals(Color.Transparent, c.surfaceTint)
        assertEquals(Color.White, c.outline)
        assertEquals(Color.White, c.outlineVariant)
        assertEquals(Color.White, c.onSurface)
        assertEquals(WhiteNoiseDarkColors.error, c.error)
        assertEquals(WhiteNoiseDarkColors.errorContainer, c.errorContainer)
    }

    @Test fun outlinedBubblesUseReadableWhiteTextOnBothSides() {
        val c = WhiteNoiseAmoledOutlineColors
        val bubbles = defaultMessageBubbleColors(c, theme)
        assertEquals(Color.Black, bubbles.mineContainer)
        assertEquals(Color.Black, bubbles.otherContainer)
        assertEquals(Color.White, bubbles.mineContent)
        assertEquals(Color.White, bubbles.otherContent)
        listOf(c.onSurface to c.surface, c.onSurfaceVariant to c.surface,
            c.onPrimary to c.primary, c.onSecondaryContainer to c.secondaryContainer,
            c.onErrorContainer to c.errorContainer).forEach { (text, fill) ->
            assertTrue((maxOf(text.luminance(), fill.luminance()) + 0.05f) /
                (minOf(text.luminance(), fill.luminance()) + 0.05f) >= 4.5f)
        }
    }

    @Test fun whiteOneDpBorderIsScopedToOutlineAndReflectsDisabledState() {
        AppearanceColorTheme.entries.filter { it != theme }.forEach { assertNull(outlineBorder(it)) }
        val enabled = requireNotNull(outlineBorder(theme))
        val disabled = requireNotNull(outlineBorder(theme, enabled = false))
        assertEquals(1.dp, enabled.width)
        assertEquals(enabled.width, disabled.width)
        assertEquals(Color.White, (enabled.brush as SolidColor).value)
        assertEquals(Color.White.copy(alpha = 0.38f), (disabled.brush as SolidColor).value)
    }

    @Test fun outlineCannotApplyOrOverwriteSavedColorOverrides() {
        val saved = AppearanceColorPreferences(
            amoled = ThemeColorOverrides(actionArgb = 0xFFFFFF00, mineBubbleArgb = 0xFFFF0000),
        )
        assertFalse(theme.supportsCustomColors)
        assertEquals(ThemeColorOverrides(), saved.forTheme(theme))
        assertSame(saved, saved.updateTheme(theme) { error("Outline must not edit saved colors") })
        assertSame(WhiteNoiseAmoledOutlineColors, withActionColor(WhiteNoiseAmoledOutlineColors, saved.forTheme(theme).actionArgb))
        assertNull(AppearanceColorPolicy.effectiveBubble(0xFFFF0000, 0xFFFFFF00, theme))
        assertEquals(0xFFFF0000L, AppearanceColorPolicy.effectiveBubble(0xFFFF0000, 0xFFFFFF00, AppearanceColorTheme.Amoled))
        assertEquals(0xFFFFFF00L, saved.amoled.actionArgb)
    }
}
