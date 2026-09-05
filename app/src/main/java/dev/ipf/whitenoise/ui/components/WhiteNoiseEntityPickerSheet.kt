package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

internal data class WhiteNoisePickerItem(
    val id: String,
    val title: String,
    val avatar: dev.ipf.whitenoise.model.ProfileAvatar? = null,
    val enabled: Boolean = true,
)

/** Searchable app-owned directories. Selection belongs to the caller; null means read-only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WhiteNoiseEntityPickerSheet(
    title: String,
    items: List<WhiteNoisePickerItem>,
    onDismiss: () -> Unit,
    onSelect: ((String) -> Unit)? = null,
    selectedIds: Set<String> = emptySet(),
    multiple: Boolean = false,
    onDone: (() -> Unit)? = null,
    description: String? = null,
    searchTag: String = "entity.search",
    rowTagPrefix: String = "entity.choice",
) {
    var query by rememberSaveable(title) { mutableStateOf("") }
    val visible = items.filter { it.title.contains(query.trim(), ignoreCase = true) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    WhiteNoiseModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().heightIn(max = LocalWindowInfo.current.containerDpSize.height * 0.88f)) {
            WhiteNoiseSheetHeader(title, onClose = onDismiss)
            if (description != null) Text(
                description,
                Modifier.padding(horizontal = WhiteNoiseSpacing.Section, vertical = WhiteNoiseSpacing.Related),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WhiteNoiseCompactSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.folder_search),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related)
                    .testTag(searchTag),
            )
            WhiteNoiseLazyColumn(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().testTag("entity.list"),
                contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (visible.isEmpty()) item {
                    Text(stringResource(R.string.no_results), Modifier.padding(WhiteNoiseSpacing.Related))
                }
                itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                    val shapes = WhiteNoiseListItemDefaults.segmentedShapes(index, visible.size)
                    val colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    )
                    val leading: (@Composable () -> Unit)? = item.avatar?.let { avatar ->
                        { ProfileAvatar(item.title, avatar, Modifier.size(48.dp), contentDescription = null) }
                    }
                    val modifier = Modifier.fillMaxWidth().testTag("$rowTagPrefix.${item.id}")
                    if (onSelect == null) {
                        ListItem(modifier = modifier, shapes = shapes, colors = colors, leadingContent = leading) { Text(item.title) }
                    } else if (multiple) {
                        ListItem(
                            checked = item.id in selectedIds,
                            onCheckedChange = { onSelect(item.id) },
                            enabled = item.enabled,
                            modifier = modifier, shapes = shapes, colors = colors, leadingContent = leading,
                            trailingContent = {
                                Checkbox(item.id in selectedIds, onCheckedChange = null, enabled = item.enabled,
                                    modifier = Modifier.clearAndSetSemantics { })
                            },
                        ) { Text(item.title) }
                    } else {
                        ListItem(onClick = { onSelect(item.id) }, enabled = item.enabled,
                            modifier = modifier, shapes = shapes, colors = colors, leadingContent = leading,
                        ) { Text(item.title) }
                    }
                }
            }
            if (onDone != null) WhiteNoiseButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin),
            ) { Text(stringResource(R.string.done)) }
        }
    }
}
