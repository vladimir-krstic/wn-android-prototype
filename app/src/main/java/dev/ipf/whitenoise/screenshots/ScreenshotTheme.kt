package dev.ipf.whitenoise.screenshots

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val Highlight = Color(0xFF0B0B0C)
internal val OnHighlight = Color(0xFFFFFFFF)
internal val CanvasGray = Color(0xFFF4F4F7)
internal val SurfaceWhite = Color(0xFFFFFFFF)
internal val Ink = Color(0xFF1C1C1E)
internal val MutedInk = Color(0xFF6E6E73)
internal val Divider = Color(0xFFD1D1D6)
internal val IncomingBubble = Color(0xFFE5E5EA)
internal val ReplySurface = Color(0xFFDCDCE1)
internal val Success = Color(0xFF2DA44E)
internal val Failure = Color(0xFFD92D2D)

internal object Dimens {
    val spaceXxs = 2.dp
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val spaceXxl = 32.dp
}

private val Manrope =
    FontFamily(
        Font(R.font.manrope_medium, FontWeight.Medium),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_bold, FontWeight.Bold),
    )

private fun Typography.withManrope(): Typography =
    copy(
        displayLarge = displayLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
        displayMedium = displayMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
        displaySmall = displaySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
        headlineLarge = headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = headlineSmall.copy(fontFamily = Manrope),
        titleLarge = titleLarge.copy(fontFamily = Manrope),
        titleMedium = titleMedium.copy(fontFamily = Manrope),
        titleSmall = titleSmall.copy(fontFamily = Manrope),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        bodySmall = bodySmall.copy(fontFamily = Manrope),
        labelLarge = labelLarge.copy(fontFamily = Manrope),
        labelMedium = labelMedium.copy(fontFamily = Manrope),
        labelSmall = labelSmall.copy(fontFamily = Manrope),
    )

private val ScreenshotTypography = Typography().withManrope()

private val ScreenshotColors =
    lightColorScheme(
        primary = Highlight,
        onPrimary = OnHighlight,
        primaryContainer = Highlight,
        onPrimaryContainer = OnHighlight,
        secondary = Color(0xFF5C5C63),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2E2E6),
        onSecondaryContainer = Ink,
        tertiary = Color(0xFF4A4A50),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE8E8EB),
        onTertiaryContainer = Ink,
        error = Failure,
        onError = Color.White,
        errorContainer = Color(0xFFFFE3E3),
        onErrorContainer = Color(0xFF410003),
        background = CanvasGray,
        onBackground = Ink,
        surface = CanvasGray,
        onSurface = Ink,
        surfaceVariant = IncomingBubble,
        onSurfaceVariant = MutedInk,
        surfaceContainerLowest = SurfaceWhite,
        surfaceContainerLow = SurfaceWhite,
        surfaceContainer = Color(0xFFF0F0F3),
        surfaceContainerHigh = Color(0xFFEBEBEE),
        surfaceContainerHighest = ReplySurface,
        outline = Color(0xFF8E8E93),
        outlineVariant = Divider,
        inverseSurface = Ink,
        inverseOnSurface = Color(0xFFF2F2F7),
        scrim = Color.Black,
        surfaceTint = Color.Transparent,
    )

@Composable
internal fun ScreenshotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScreenshotColors,
        typography = ScreenshotTypography,
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
        content = content,
    )
}
