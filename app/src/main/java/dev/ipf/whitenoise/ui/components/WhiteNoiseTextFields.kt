package dev.ipf.whitenoise.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/**
 * Keeps ordinary White Noise form fields outlined while adding only a subtle neutral surface.
 * Material continues to own focus, error, disabled, text, label, and outline state colors.
 */
@Composable
fun whiteNoiseOutlinedTextFieldColors(): TextFieldColors {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
    )
}
