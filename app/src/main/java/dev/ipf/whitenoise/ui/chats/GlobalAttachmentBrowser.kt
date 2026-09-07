package dev.ipf.whitenoise.ui.chats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun GlobalAttachmentModes(filters: GlobalSearchFilters, onChange: (GlobalSearchFilters) -> Unit) {
    val choices = listOf(null to R.string.shared_filter_all, GlobalSearchContent.ImagesVideo to R.string.library_photos_videos,
        GlobalSearchContent.Files to R.string.library_files, GlobalSearchContent.VoiceAudio to R.string.library_audio,
        GlobalSearchContent.AnyAttachment to R.string.library_all)
    LazyRow(Modifier.fillMaxWidth().testTag("global.library.modes"), contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        choices.forEach { (type, label) -> item(key = type?.name ?: "Messages") {
            FilterChip(selected = if (type == null) !GlobalAttachments.browsing(filters) else filters.content == setOf(type),
                onClick = { onChange(filters.copy(content = type?.let { setOf(it) }.orEmpty())) },
                label = { Text(stringResource(label)) }, modifier = Modifier.testTag("global.library.mode.${type?.name ?: "Messages"}"))
        } }
    }
}

@Composable
internal fun GlobalAttachmentBrowser(
    profile: Profile, query: String, filters: GlobalSearchFilters, results: GlobalSearchResults,
    bottomPadding: androidx.compose.ui.unit.Dp, onGoToMessage: (String, String) -> Unit,
    nextScenario: () -> GlobalLibraryScenario = { GlobalLibraryScenario.Ready },
) {
    val rows = remember(profile, results, filters) { GlobalAttachments.items(profile, results, filters) }
    var phase by rememberSaveable(profile.id, query, filters) { mutableStateOf("Loading") }
    var retry by rememberSaveable(profile.id, query, filters) { mutableIntStateOf(0) }
    val grid = rememberLazyGridState()
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    LaunchedEffect(profile.id, query, filters, retry) {
        if (phase == "Loading") {
            val outcome = if (retry > 0) GlobalLibraryScenario.Ready else nextScenario()
            delay(250)
            phase = outcome.name
        }
    }
    val visible = if (phase == GlobalLibraryScenario.Partial.name) rows.take((rows.size + 1) / 2) else rows
    val visualMode = filters.content.any { it == GlobalSearchContent.ImagesVideo || it == GlobalSearchContent.AnyAttachment }
    Column(Modifier.fillMaxSize().testTag("global.library")) {
        if (phase == "Loading") LinearProgressIndicator(Modifier.fillMaxWidth().testTag("global.library.loading"))
        if (phase in setOf(GlobalLibraryScenario.Partial.name, GlobalLibraryScenario.Failed.name)) {
            Row(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (phase == GlobalLibraryScenario.Partial.name) R.string.library_partial else R.string.library_failed), Modifier.weight(1f))
                TextButton(onClick = { phase = "Loading"; retry++ }, Modifier.testTag("global.library.retry")) { Text(stringResource(R.string.history_retry)) }
            }
        }
        when {
            phase == "Loading" -> Text(stringResource(R.string.library_loading), Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin).semantics { liveRegion = LiveRegionMode.Polite })
            phase == GlobalLibraryScenario.Failed.name -> Unit
            visible.isEmpty() && phase == GlobalLibraryScenario.Ready.name -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WhiteNoiseEmptyState(stringResource(R.string.library_empty), stringResource(R.string.global_no_matches_detail))
            }
            visible.isEmpty() -> Unit
            else -> LazyVerticalGrid(
                columns = if (visualMode) GridCells.Adaptive(160.dp) else GridCells.Fixed(1), state = grid,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("global.library.results"),
                contentPadding = PaddingValues(start = WhiteNoiseSpacing.CompactScreenMargin, end = WhiteNoiseSpacing.CompactScreenMargin,
                    bottom = bottomPadding + WhiteNoiseSpacing.Section),
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                visible.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(GlobalSearchClock.zone).toLocalDate() }.forEach { (day, entries) ->
                    item(key = "day-$day", span = { GridItemSpan(maxLineSpan) }) {
                        Text(day.format(formatter), Modifier.padding(vertical = WhiteNoiseSpacing.Related).semantics { heading() }, style = MaterialTheme.typography.titleSmall)
                    }
                    items(entries, key = { it.id }, span = { GridItemSpan(if (it.type == GlobalSearchContent.ImagesVideo) 1 else maxLineSpan) }) { item ->
                        GlobalAttachmentCard(item) { onGoToMessage(item.chatId, item.message.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalAttachmentCard(item: GlobalAttachmentItem, onClick: () -> Unit) {
    val visual = item.type == GlobalSearchContent.ImagesVideo
    val label = if (item.available) item.attachment.label.ifBlank { stringResource(item.type.labelResource) }
        else stringResource(R.string.library_unavailable)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("global.library.item.${item.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface),
        border = dev.ipf.whitenoise.ui.theme.amoledOutlineBorder(),
    ) {
        if (visual) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)
                .semantics { if (item.available) contentDescription = label }, contentAlignment = Alignment.Center) {
                if (item.available && item.attachment.images.getOrNull(item.imageIndex) != null) {
                    ComposerImage(item.attachment.images[item.imageIndex], Modifier.fillMaxSize())
                } else {
                    Icon(painterResource(if (item.attachment.kind == MessageAttachmentKind.Video) R.drawable.ic_play_arrow else R.drawable.ic_image),
                        null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.available && item.attachment.kind == MessageAttachmentKind.Video) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
                        Icon(painterResource(R.drawable.ic_play_arrow), null, Modifier.padding(WhiteNoiseSpacing.Related).size(24.dp))
                    }
                }
            }
        }
        Column(Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            if (!visual && item.available) {
                TimelineLibraryAttachmentPreview(item.attachment, audio = item.type == GlobalSearchContent.VoiceAudio)
            } else if (!item.available) {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(stringResource(R.string.library_source, item.chatTitle, item.message.timeLabel),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
