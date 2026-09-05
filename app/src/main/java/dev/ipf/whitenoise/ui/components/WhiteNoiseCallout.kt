package dev.ipf.whitenoise.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

/** Informational notices share a quiet surface; actions and live announcements belong to callers. */
@Composable
fun WhiteNoiseCallout(
    modifier: Modifier = Modifier,
    text: String? = null,
    title: String? = null,
    @DrawableRes icon: Int = R.drawable.ic_info,
    isError: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            verticalAlignment = Alignment.Top,
        ) {
            if (leading != null) leading() else Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (content != null) Column(
                    modifier = if (title != null || text != null) Modifier.padding(top = WhiteNoiseSpacing.Related) else Modifier,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) { content() }
            }
            trailing?.invoke()
        }
    }
}
