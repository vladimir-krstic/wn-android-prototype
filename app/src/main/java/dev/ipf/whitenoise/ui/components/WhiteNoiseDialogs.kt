package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

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

/**
 * Immediate single-choice row for an app-owned dialog.
 *
 * The dialog already owns the outer horizontal content inset, so this row deliberately avoids the
 * additional content padding of a settings [androidx.compose.material3.ListItem]. The radio keeps
 * its native touch target, and the whole row is the single selectable accessibility target.
 */
@Composable
fun WhiteNoiseDialogChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                // AlertDialog owns a 24 dp content inset. Let the rounded state layer use
                // the outer 16 dp while retaining an 8 dp gutter against the dialog edge.
                .requiredWidth(maxWidth + (WhiteNoiseSpacing.FormField * 2))
                .then(modifier)
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.large)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                // Restore the dialog-owned content line inside the wider state layer.
                .padding(horizontal = WhiteNoiseSpacing.FormField),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = null,
                modifier = Modifier
                    .testTag("dialog.choice.radio")
                    .clearAndSetSemantics { },
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = WhiteNoiseSpacing.FormField),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.38f,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
