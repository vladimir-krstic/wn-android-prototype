package dev.ipf.whitenoise.ui.components

import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * Ordinary app-owned Material dialog.
 *
 * The dialog is the neutral canvas; nested controls use the white-equivalent surface. Platform
 * pickers, permissions, Sharesheet surfaces, and specialized camera presentation do not use this
 * wrapper and remain system-owned.
 */
@Composable
fun WhiteNoiseAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(
        LocalWhiteNoiseTextFieldContainerColor provides scheme.surfaceContainerLowest,
    ) {
        MaterialAlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            containerColor = scheme.surfaceContainerLow,
            iconContentColor = scheme.onSurfaceVariant,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurfaceVariant,
        )
    }
}
