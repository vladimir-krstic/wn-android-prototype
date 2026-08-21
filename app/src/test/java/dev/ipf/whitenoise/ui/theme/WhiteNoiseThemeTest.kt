package dev.ipf.whitenoise.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.ui.components.WhiteNoiseButtonDefaults
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextFieldDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class WhiteNoiseThemeTest {
    @Test
    fun ordinaryLightAndDarkRolesAreNeutral() {
        assertNeutral(WhiteNoiseLightColors)
        assertNeutral(WhiteNoiseDarkColors)
    }

    @Test
    fun visualSystemUsesTheApprovedTypeWeightAndShapeScale() {
        assertEquals(FontWeight.Medium, WhiteNoiseTypography.headlineMedium.fontWeight)
        assertEquals(FontWeight.Medium, WhiteNoiseTypography.titleLarge.fontWeight)
        assertEquals(RoundedCornerShape(4.dp), WhiteNoiseShapes.extraSmall)
        assertEquals(RoundedCornerShape(8.dp), WhiteNoiseShapes.small)
        assertEquals(RoundedCornerShape(12.dp), WhiteNoiseShapes.medium)
        assertEquals(RoundedCornerShape(16.dp), WhiteNoiseShapes.large)
        assertEquals(RoundedCornerShape(28.dp), WhiteNoiseShapes.extraLarge)
    }

    @Test
    fun taskButtonsMatchTheMaterialSingleLineFieldHeight() {
        assertEquals(56.dp, WhiteNoiseButtonDefaults.TaskHeight)
    }

    @Test
    fun formFieldsUseTheApprovedContentLineAndStateRing() {
        assertEquals(16.dp, WhiteNoiseTextFieldDefaults.ContentInset)
        assertEquals(12.dp, WhiteNoiseTextFieldDefaults.AboveLabelAdditionalStartInset)
        assertEquals(2.dp, WhiteNoiseTextFieldDefaults.StateRingWidth)
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
