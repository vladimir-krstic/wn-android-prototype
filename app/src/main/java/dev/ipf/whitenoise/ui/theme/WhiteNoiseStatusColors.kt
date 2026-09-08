package dev.ipf.whitenoise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class WhiteNoiseStatusColors(val success: Color, val warning: Color)

/** Semantic status accents; independent of the optional identity accent. */
@Composable
fun whiteNoiseStatusColors(): WhiteNoiseStatusColors = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
    WhiteNoiseStatusColors(success = Color(0xFF81C995), warning = Color(0xFFFFB86B))
} else {
    WhiteNoiseStatusColors(success = Color(0xFF146C2E), warning = Color(0xFF934B00))
}
