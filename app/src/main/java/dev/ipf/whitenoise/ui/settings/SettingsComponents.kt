@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.ui.components.WhiteNoiseListItemDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.ListItemShapes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
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
            .semantics { heading() }
            .padding(
                start = WhiteNoiseSpacing.SettingsSectionInset,
                end = WhiteNoiseSpacing.SettingsSectionInset,
                top = if (LocalSettingsList.current) WhiteNoiseSpacing.FormField else WhiteNoiseSpacing.Section,
                bottom = if (LocalSettingsList.current) 0.dp else WhiteNoiseSpacing.Related,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

/** Builds the visible rows before assigning Material's first/middle/last shapes. */
class SettingsGroupScope {
    internal val rows = mutableListOf<SettingsGroupEntry>()

    fun row(content: @Composable () -> Unit) {
        rows.add(SettingsGroupEntry(native = true, content))
    }

    fun item(content: @Composable () -> Unit) {
        rows.add(SettingsGroupEntry(native = false, content))
    }
}

internal data class SettingsGroupEntry(val native: Boolean, val content: @Composable () -> Unit)

private val LocalSettingsRowShapes = staticCompositionLocalOf<ListItemShapes?> { null }
private val LocalSettingsRowColor = staticCompositionLocalOf { Color.Transparent }
private val LocalSettingsList = staticCompositionLocalOf { false }

@Composable
internal fun SettingsGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    val group = SettingsGroupScope().apply { content() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        group.rows.forEachIndexed { index, row ->
            val shapes = WhiteNoiseListItemDefaults.segmentedShapes(index, group.rows.size)
            CompositionLocalProvider(
                LocalSettingsRowShapes provides shapes,
                LocalSettingsRowColor provides containerColor,
                // A row may contain its own explanatory content; it owns its spacing.
                LocalSettingsList provides false,
            ) {
                if (row.native) {
                    row.content()
                } else {
                    Surface(color = containerColor, shape = shapes.shape) {
                        Column(Modifier.fillMaxWidth()) { row.content() }
                    }
                }
            }
        }
    }
}

@Composable
private fun settingsRowShapes(): ListItemShapes = LocalSettingsRowShapes.current ?: WhiteNoiseListItemDefaults.shapes()

@Composable
internal fun SettingsDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(ListItemDefaults.SegmentedGap))
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
    val summary = listOfNotNull(value, subtitle).distinct().joinToString("\n").takeIf { it.isNotEmpty() }
    ListItem(
        onClick = onClick,
        enabled = enabled,
        content = { Text(title, color = headlineColor) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = leading,
        trailingContent = {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
        },
        shapes = settingsRowShapes(),
        colors = ListItemDefaults.colors(
            containerColor = LocalSettingsRowColor.current,
            disabledContainerColor = LocalSettingsRowColor.current,
        ),
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
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
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shapes = settingsRowShapes(),
        content = { Text(title, color = headlineColor) },
        supportingContent = subtitle?.let { { Text(it, color = supportingColor) } },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics { },
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = LocalSettingsRowColor.current,
            disabledContainerColor = LocalSettingsRowColor.current,
            selectedContainerColor = LocalSettingsRowColor.current,
        ),
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Switch },
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
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shapes = settingsRowShapes(),
        content = {
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
            containerColor = LocalSettingsRowColor.current,
            disabledContainerColor = LocalSettingsRowColor.current,
            selectedContainerColor = if (highlightSelected) MaterialTheme.colorScheme.surfaceContainerHigh else LocalSettingsRowColor.current,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun SettingsAction(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        onClick = onClick,
        enabled = enabled,
        shapes = settingsRowShapes(),
        content = { Text(title, color = headlineColor) },
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
        colors = ListItemDefaults.colors(containerColor = LocalSettingsRowColor.current, disabledContainerColor = LocalSettingsRowColor.current),
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
    )
}

@Composable
internal fun SettingsValue(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        shapes = settingsRowShapes(),
        content = { Text(title) },
        supportingContent = {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = ListItemDefaults.colors(containerColor = LocalSettingsRowColor.current, disabledContainerColor = LocalSettingsRowColor.current),
    )
}

@Composable
internal fun SettingsMetadata(title: String, value: String) {
    SettingsValue(title, value)
}

@Composable
internal fun SettingsCallout(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    isError: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    @androidx.annotation.DrawableRes icon: Int = if (isError) R.drawable.ic_error else R.drawable.ic_info,
) {
    dev.ipf.whitenoise.ui.components.WhiteNoiseCallout(
        text = text, title = title, isError = isError, leading = leading, icon = icon,
        modifier = modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
    )
}

@Composable
internal fun SettingsList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    CompositionLocalProvider(LocalSettingsList provides true) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("settings.list"),
            contentPadding = PaddingValues(top = WhiteNoiseSpacing.Related, bottom = WhiteNoiseSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            content = {
                val list = this
                val scope = object : LazyListScope by list {
                    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
                        list.item(key, contentType) {
                            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                                content()
                            }
                        }
                    }
                }
                scope.content()
            },
        )
    }
}

@Composable
internal fun SettingsExplainer(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WhiteNoiseSpacing.SettingsSectionInset,
                top = if (LocalSettingsList.current) 0.dp else WhiteNoiseSpacing.Related,
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
