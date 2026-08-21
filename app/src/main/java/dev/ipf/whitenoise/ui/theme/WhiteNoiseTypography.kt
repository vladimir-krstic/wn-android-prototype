package dev.ipf.whitenoise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

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
