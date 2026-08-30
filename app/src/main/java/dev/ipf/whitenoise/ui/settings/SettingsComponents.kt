@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.LocalWhiteNoiseTextFieldContainerColor
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    prominentTitle: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    topBarContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    topBarScrolledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = contentWindowInsets,
        topBar = {
            WhiteNoiseTopBar(
                title = title,
                onBack = onBack,
                titleStyle = if (prominentTitle) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                containerColor = topBarContainerColor,
                scrolledContainerColor = topBarScrolledContainerColor,
                actions = topBarActions,
            )
        },
        bottomBar = bottomBar,
        containerColor = containerColor,
    ) { padding ->
        CompositionLocalProvider(
            LocalWhiteNoiseTextFieldContainerColor provides
                MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            AdaptiveContent(modifier = Modifier.fillMaxSize().padding(padding)) { content() }
        }
    }
}

@Composable
internal fun SettingsBottomAction(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = color,
        tonalElevation = tonalElevation,
    ) {
        AdaptiveContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(WhiteNoiseSpacing.PinnedActionInset),
                content = content,
            )
        }
    }
}

@Composable
internal fun SettingsSection(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WhiteNoiseSpacing.SettingsSectionInset,
                end = WhiteNoiseSpacing.SettingsSectionInset,
                top = WhiteNoiseSpacing.Section,
                bottom = WhiteNoiseSpacing.Related,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun SettingsGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor),
        content = content,
    )
}

@Composable
internal fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Composable
internal fun SettingsLink(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val headlineColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (enabled) 1f else 0.38f,
    )
    ListItem(
        headlineContent = { Text(title, color = headlineColor) },
        supportingContent = subtitle?.let { { Text(it, color = supportingColor) } },
        leadingContent = leading,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                value?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = supportingColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = supportingColor,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
            },
    )
}

@Composable
internal fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val headlineColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (enabled) 1f else 0.38f,
    )
    val supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (enabled) 1f else 0.38f,
    )
    ListItem(
        headlineContent = { Text(title, color = headlineColor) },
        supportingContent = subtitle?.let { { Text(it, color = supportingColor) } },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics { },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics {
                if (!enabled) disabled()
            },
    )
}

@Composable
internal fun SettingsChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    highlightSelected: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.38f
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = null,
                modifier = Modifier.clearAndSetSemantics { },
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected && highlightSelected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                Color.Transparent
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    )
}

@Composable
internal fun SettingsAction(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val headlineColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(title, color = headlineColor) },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.38f,
                    ),
                )
            }
        },
        leadingContent = leading,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
            },
    )
}

@Composable
internal fun SettingsValue(
    title: String,
    value: String,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun SettingsMetadata(
    title: String,
    value: String,
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun SettingsCallout(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    isError: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        color = container,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            verticalAlignment = Alignment.Top,
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                title?.let {
                    Text(
                        text = it,
                        color = contentColor,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun SettingsList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings.list"),
        contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
        content = content,
    )
}

@Composable
internal fun SettingsExplainer(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WhiteNoiseSpacing.SettingsSectionInset,
                top = WhiteNoiseSpacing.Related,
                end = WhiteNoiseSpacing.SettingsSectionInset,
            ),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SettingsVersionFooter(versionName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WhiteNoiseSpacing.CompactScreenMargin)
            .testTag("settings.version_footer"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Version $versionName",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
