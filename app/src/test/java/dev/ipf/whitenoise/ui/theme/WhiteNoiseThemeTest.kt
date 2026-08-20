package dev.ipf.whitenoise.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class WhiteNoiseThemeTest {
    @Test
    fun ordinaryLightAndDarkRolesAreNeutral() {
        assertNeutral(WhiteNoiseLightColors)
        assertNeutral(WhiteNoiseDarkColors)
    }

    private fun assertNeutral(colors: ColorScheme) {
        listOf(
            colors.primary,
            colors.onPrimary,
            colors.primaryContainer,
            colors.onPrimaryContainer,
            colors.inversePrimary,
            colors.secondary,
            colors.onSecondary,
            colors.secondaryContainer,
            colors.onSecondaryContainer,
            colors.tertiary,
            colors.onTertiary,
            colors.tertiaryContainer,
            colors.onTertiaryContainer,
            colors.background,
            colors.onBackground,
            colors.surface,
            colors.onSurface,
            colors.surfaceVariant,
            colors.onSurfaceVariant,
            colors.surfaceTint,
            colors.inverseSurface,
            colors.inverseOnSurface,
            colors.outline,
            colors.outlineVariant,
            colors.scrim,
            colors.surfaceBright,
            colors.surfaceDim,
            colors.surfaceContainerLowest,
            colors.surfaceContainerLow,
            colors.surfaceContainer,
            colors.surfaceContainerHigh,
            colors.surfaceContainerHighest,
            colors.primaryFixed,
            colors.primaryFixedDim,
            colors.onPrimaryFixed,
            colors.onPrimaryFixedVariant,
            colors.secondaryFixed,
            colors.secondaryFixedDim,
            colors.onSecondaryFixed,
            colors.onSecondaryFixedVariant,
            colors.tertiaryFixed,
            colors.tertiaryFixedDim,
            colors.onTertiaryFixed,
            colors.onTertiaryFixedVariant,
        ).forEach(::assertNeutral)
    }

    private fun assertNeutral(color: Color) {
        assertEquals(color.red, color.green, 0.0001f)
        assertEquals(color.green, color.blue, 0.0001f)
    }
}
