package dev.ipf.whitenoise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.ipf.whitenoise.model.AppearancePreference

internal val WhiteNoiseLightColors = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF171717),
    inversePrimary = Color(0xFFF5F5F5),
    secondary = Color(0xFF4D4D4D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color(0xFF262626),
    tertiary = Color(0xFF666666),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5E5E5),
    onTertiaryContainer = Color(0xFF262626),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF171717),
    surface = Color(0xFFF7F7F7),
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF4D4D4D),
    surfaceTint = Color(0xFF171717),
    inverseSurface = Color(0xFF262626),
    inverseOnSurface = Color(0xFFF5F5F5),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE0DE),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF737373),
    outlineVariant = Color(0xFFC7C7C7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE9E9E9),
    surfaceContainerHighest = Color(0xFFE3E3E3),
    surfaceDim = Color(0xFFDADADA),
    primaryFixed = Color(0xFFE5E5E5),
    primaryFixedDim = Color(0xFFC7C7C7),
    onPrimaryFixed = Color(0xFF171717),
    onPrimaryFixedVariant = Color(0xFF4D4D4D),
    secondaryFixed = Color(0xFFE5E5E5),
    secondaryFixedDim = Color(0xFFC7C7C7),
    onSecondaryFixed = Color(0xFF171717),
    onSecondaryFixedVariant = Color(0xFF4D4D4D),
    tertiaryFixed = Color(0xFFE5E5E5),
    tertiaryFixedDim = Color(0xFFC7C7C7),
    onTertiaryFixed = Color(0xFF171717),
    onTertiaryFixedVariant = Color(0xFF4D4D4D),
)

internal val WhiteNoiseDarkColors = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF171717),
    primaryContainer = Color(0xFF404040),
    onPrimaryContainer = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFF171717),
    secondary = Color(0xFFD4D4D4),
    onSecondary = Color(0xFF262626),
    secondaryContainer = Color(0xFF404040),
    onSecondaryContainer = Color(0xFFF5F5F5),
    tertiary = Color(0xFFB8B8B8),
    onTertiary = Color(0xFF171717),
    tertiaryContainer = Color(0xFF404040),
    onTertiaryContainer = Color(0xFFF5F5F5),
    background = Color(0xFF101010),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFD4D4D4),
    surfaceTint = Color(0xFFF5F5F5),
    inverseSurface = Color(0xFFE5E5E5),
    inverseOnSurface = Color(0xFF262626),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF999999),
    outlineVariant = Color(0xFF4D4D4D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383838),
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
    surfaceDim = Color(0xFF101010),
    primaryFixed = Color(0xFFE5E5E5),
    primaryFixedDim = Color(0xFFC7C7C7),
    onPrimaryFixed = Color(0xFF171717),
    onPrimaryFixedVariant = Color(0xFF4D4D4D),
    secondaryFixed = Color(0xFFE5E5E5),
    secondaryFixedDim = Color(0xFFC7C7C7),
    onSecondaryFixed = Color(0xFF171717),
    onSecondaryFixedVariant = Color(0xFF4D4D4D),
    tertiaryFixed = Color(0xFFE5E5E5),
    tertiaryFixedDim = Color(0xFFC7C7C7),
    onTertiaryFixed = Color(0xFF171717),
    onTertiaryFixedVariant = Color(0xFF4D4D4D),
)

@Composable
fun WhiteNoiseTheme(
    appearance: AppearancePreference = AppearancePreference.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearance) {
        AppearancePreference.System -> isSystemInDarkTheme()
        AppearancePreference.Light -> false
        AppearancePreference.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) WhiteNoiseDarkColors else WhiteNoiseLightColors,
        content = content,
    )
}
