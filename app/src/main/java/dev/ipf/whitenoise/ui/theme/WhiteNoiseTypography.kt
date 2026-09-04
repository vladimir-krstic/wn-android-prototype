package dev.ipf.whitenoise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AppFontFamily

private val BaselineTypography = Typography()

internal val WhiteNoiseTypography = Typography(
    displayLarge = BaselineTypography.displayLarge,
    displayMedium = BaselineTypography.displayMedium,
    displaySmall = BaselineTypography.displaySmall,
    headlineLarge = BaselineTypography.headlineLarge,
    headlineMedium = BaselineTypography.headlineMedium.copy(fontWeight = FontWeight.Medium),
    headlineSmall = BaselineTypography.headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = BaselineTypography.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = BaselineTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = BaselineTypography.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = BaselineTypography.bodyLarge,
    bodyMedium = BaselineTypography.bodyMedium,
    bodySmall = BaselineTypography.bodySmall,
    labelLarge = BaselineTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = BaselineTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = BaselineTypography.labelSmall,
)

/** Preserve platform density/font conversion; only scale explicit sp typography. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun Typography.scaledBy(factor: Float): Typography {
    require(factor.isFinite() && factor > 0)
    if (factor == 1f) return this
    fun TextUnit.scaled(): TextUnit = if (isSp) (value * factor).sp else this
    fun TextStyle.scaled() = copy(fontSize = fontSize.scaled(), lineHeight = lineHeight.scaled())
    return copy(
        displayLarge = displayLarge.scaled(),
        displayMedium = displayMedium.scaled(),
        displaySmall = displaySmall.scaled(),
        headlineLarge = headlineLarge.scaled(),
        headlineMedium = headlineMedium.scaled(),
        headlineSmall = headlineSmall.scaled(),
        titleLarge = titleLarge.scaled(),
        titleMedium = titleMedium.scaled(),
        titleSmall = titleSmall.scaled(),
        bodyLarge = bodyLarge.scaled(),
        bodyMedium = bodyMedium.scaled(),
        bodySmall = bodySmall.scaled(),
        labelLarge = labelLarge.scaled(),
        labelMedium = labelMedium.scaled(),
        labelSmall = labelSmall.scaled(),
        displayLargeEmphasized = displayLargeEmphasized.scaled(),
        displayMediumEmphasized = displayMediumEmphasized.scaled(),
        displaySmallEmphasized = displaySmallEmphasized.scaled(),
        headlineLargeEmphasized = headlineLargeEmphasized.scaled(),
        headlineMediumEmphasized = headlineMediumEmphasized.scaled(),
        headlineSmallEmphasized = headlineSmallEmphasized.scaled(),
        titleLargeEmphasized = titleLargeEmphasized.scaled(),
        titleMediumEmphasized = titleMediumEmphasized.scaled(),
        titleSmallEmphasized = titleSmallEmphasized.scaled(),
        bodyLargeEmphasized = bodyLargeEmphasized.scaled(),
        bodyMediumEmphasized = bodyMediumEmphasized.scaled(),
        bodySmallEmphasized = bodySmallEmphasized.scaled(),
        labelLargeEmphasized = labelLargeEmphasized.scaled(),
        labelMediumEmphasized = labelMediumEmphasized.scaled(),
        labelSmallEmphasized = labelSmallEmphasized.scaled(),
    )
}

private val ManropeFamily = FontFamily(
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)
private val OutfitFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)
private val UrbanistFamily = FontFamily(
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_medium, FontWeight.Medium),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold),
    Font(R.font.urbanist_bold, FontWeight.Bold),
)
private val FigtreeFamily = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun Typography.withAppFont(font: AppFontFamily): Typography {
    val family = when (font) {
        AppFontFamily.System -> return this
        AppFontFamily.Manrope -> ManropeFamily
        AppFontFamily.Outfit -> OutfitFamily
        AppFontFamily.Urbanist -> UrbanistFamily
        AppFontFamily.Figtree -> FigtreeFamily
    }
    return copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
        displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = family),
        displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = family),
        displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = family),
        headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = family),
        headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = family),
        headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = family),
        titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = family),
        titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = family),
        titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = family),
        bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = family),
        bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = family),
        bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = family),
        labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = family),
        labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = family),
        labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = family),
    )
}
