package dev.ipf.whitenoise.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.AppearanceColorTheme

@Composable
fun isAmoledOutline(): Boolean = LocalAppearanceColorTheme.current == AppearanceColorTheme.Amoled

internal fun outlineBorder(theme: AppearanceColorTheme, enabled: Boolean = true): BorderStroke? =
    if (theme == AppearanceColorTheme.Amoled) {
        BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 1f else 0.38f))
    } else null

/** Use Surface.border when available so the border follows the component's own shape and motion. */
@Composable
fun amoledOutlineBorder(enabled: Boolean = true): BorderStroke? =
    outlineBorder(LocalAppearanceColorTheme.current, enabled)

/** Message direction changes the bubble boundary, without dimming its content. */
@Composable
fun amoledMessageBorder(outgoing: Boolean): BorderStroke? =
    if (isAmoledOutline()) {
        BorderStroke(1.dp, if (outgoing) Color.White else WhiteNoiseDarkColors.outline)
    } else null

@Composable
fun Modifier.amoledOutline(shape: Shape, enabled: Boolean = true): Modifier =
    if (isAmoledOutline() && shape is ConnectedRowShape) {
        connectedRowBorder(shape, Color.White.copy(alpha = if (enabled) 1f else 0.38f))
    } else amoledOutlineBorder(enabled)?.let { border(it, shape) } ?: this

/** Selected containers need a state layer even when resting tonal surfaces are all black. */
@Composable
fun outlineSelectionColor(default: Color): Color =
    if (isAmoledOutline()) Color.White.copy(alpha = 0.16f) else default

@Composable
fun outlineButtonColors(): ButtonColors = if (isAmoledOutline()) {
    ButtonDefaults.outlinedButtonColors(containerColor = Color.Black, contentColor = Color.White)
} else ButtonDefaults.buttonColors()
