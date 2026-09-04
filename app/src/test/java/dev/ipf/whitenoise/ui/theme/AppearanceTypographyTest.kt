package dev.ipf.whitenoise.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class AppearanceTypographyTest {
    @Test fun systemTypefaceIsUnchangedAndEachOptionalFamilyIsDistinct() {
        assertSame(WhiteNoiseTypography,WhiteNoiseTypography.withAppFont(AppFontFamily.System))
        val families = AppFontFamily.entries.filter { it != AppFontFamily.System }.map { choice ->
            val type = WhiteNoiseTypography.withAppFont(choice)
            assertEquals(type.bodyLarge.fontFamily,type.labelSmallEmphasized.fontFamily)
            assertEquals(WhiteNoiseTypography.bodyLarge.fontSize,type.bodyLarge.fontSize)
            assertEquals(WhiteNoiseTypography.titleLarge.fontWeight,type.titleLarge.fontWeight)
            type.bodyLarge.fontFamily
        }
        assertEquals(4,families.toSet().size)
        assertTrue(families.all { it != null })
    }
    @Test fun familyAndSizeComposeWithoutChangingWeightOrLineHeightRatio() {
        val baseline = WhiteNoiseTypography
        val type = baseline.withAppFont(AppFontFamily.Figtree).scaledBy(1.15f)
        assertEquals(baseline.scaledBy(1.15f).withAppFont(AppFontFamily.Figtree),type)
        assertEquals(baseline.headlineMedium.fontWeight,type.headlineMedium.fontWeight)
    }
    @Test fun newChoicesPreserveExistingDefaultsAndPalettes() {
        val settings = ProfileSettings()
        assertEquals(AppearancePreference.System, settings.appearance)
        assertEquals(AppFontSize.Default, settings.fontSize)
        assertEquals(AppFontFamily.System,settings.fontFamily)
        assertEquals(EnterKeyBehavior.NewLine, settings.enterKeyBehavior)
        assertSame(WhiteNoiseLightColors, whiteNoiseColorScheme(AppearancePreference.System,false))
        assertSame(WhiteNoiseDarkColors, whiteNoiseColorScheme(AppearancePreference.System,true))
        assertSame(WhiteNoiseLightColors, whiteNoiseColorScheme(AppearancePreference.Light,true))
        assertSame(WhiteNoiseDarkColors, whiteNoiseColorScheme(AppearancePreference.Dark,false))
    }
    @Test fun amoledUsesBlackCanvasAndKeepsDistinctSemanticSurfaces() {
        val colors = whiteNoiseColorScheme(AppearancePreference.Amoled,false)
        assertSame(colors, whiteNoiseColorScheme(AppearancePreference.Amoled,true))
        assertEquals(Color.Black, colors.background); assertEquals(Color.Black, colors.surface)
        assertEquals(Color.Black, colors.surfaceContainerLow); assertEquals(Color.Transparent,colors.surfaceTint)
        assertNotEquals(colors.surface, colors.surfaceContainer)
        assertNotEquals(colors.surfaceContainer, colors.surfaceContainerHigh)
        assertEquals(WhiteNoiseDarkColors.error, colors.error)
        assertEquals(WhiteNoiseDarkColors.onError, colors.onError)
    }
    @Test fun amoledTextAndActionsRetainReadableContrast() {
        val c = WhiteNoiseAmoledColors
        for ((text, fill) in listOf(c.onSurface to c.surface, c.onSurfaceVariant to c.surfaceContainerHighest,
            c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer, c.onError to c.error)) {
            val bright = maxOf(text.luminance(), fill.luminance()); val dark = minOf(text.luminance(), fill.luminance())
            assertTrue((bright + 0.05f) / (dark + 0.05f) >= 4.5f)
        }
    }
    @Test fun actionColorChangesOnlyActionRolesAndKeepsNeutralSurfaces() {
        val changed = withActionColor(WhiteNoiseLightColors, 0xFF1D4ED8L)
        assertNotEquals(WhiteNoiseLightColors.primary, changed.primary)
        assertEquals(WhiteNoiseLightColors.background, changed.background)
        assertEquals(WhiteNoiseLightColors.surface, changed.surface)
        assertEquals(WhiteNoiseLightColors.surfaceContainer, changed.surfaceContainer)
        assertEquals(WhiteNoiseLightColors.surfaceTint, changed.surfaceTint)
    }
    @Test fun defaultScaleIsAnIdentityAndLargerTextScalesLineHeight() {
        assertSame(WhiteNoiseTypography, WhiteNoiseTypography.scaledBy(1f))
        val scaled = WhiteNoiseTypography.scaledBy(1.3f)
        assertEquals(WhiteNoiseTypography.bodyLarge.fontSize.value * 1.3f, scaled.bodyLarge.fontSize.value,0.001f)
        assertEquals(WhiteNoiseTypography.bodyLarge.lineHeight.value * 1.3f, scaled.bodyLarge.lineHeight.value,0.001f)
        assertEquals(WhiteNoiseTypography.titleMedium.fontWeight, scaled.titleMedium.fontWeight)
        assertEquals(WhiteNoiseTypography.labelSmallEmphasized.fontSize.value * 1.3f, scaled.labelSmallEmphasized.fontSize.value,0.001f)
    }
    @Test fun relativeAndUnspecifiedTextUnitsAreNotScaledTwice() {
        val source = WhiteNoiseTypography.copy(bodyLarge = TextStyle(fontSize = 16.sp,lineHeight = 1.4.em),
            bodySmall = TextStyle(fontSize = TextUnit.Unspecified,lineHeight = TextUnit.Unspecified))
        val scaled = source.scaledBy(0.85f)
        assertEquals(1.4.em, scaled.bodyLarge.lineHeight)
        assertEquals(TextUnit.Unspecified,scaled.bodySmall.fontSize)
        assertEquals(TextUnit.Unspecified,scaled.bodySmall.lineHeight)
    }
    @Test fun appScaleLeavesPlatformFontConversionToCompose() {
        val source = WhiteNoiseTypography
        AppFontSize.entries.forEach { size ->
            val scaled = source.scaledBy(size.factor)
            assertEquals(source.bodyLarge.fontSize.value * size.factor,scaled.bodyLarge.fontSize.value,0.001f)
            assertEquals(source.bodyLarge.letterSpacing,scaled.bodyLarge.letterSpacing)
            assertEquals(source.bodyLarge.fontFamily,scaled.bodyLarge.fontFamily)
        }
    }
}
