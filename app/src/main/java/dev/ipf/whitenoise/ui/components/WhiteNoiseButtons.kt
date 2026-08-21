package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object WhiteNoiseButtonDefaults {
    /** Exact Material medium-button metrics; matches a 56 dp single-line text field. */
    val TaskHeight = 56.dp
    val TaskContentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
}

@Composable
fun WhiteNoiseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = WhiteNoiseButtonDefaults.TaskHeight),
        enabled = enabled,
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
        content = content,
    )
}

@Composable
fun WhiteNoiseOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = WhiteNoiseButtonDefaults.TaskHeight),
        enabled = enabled,
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
        content = content,
    )
}

@Composable
fun WhiteNoiseFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = WhiteNoiseButtonDefaults.TaskHeight),
        enabled = enabled,
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
        content = content,
    )
}
