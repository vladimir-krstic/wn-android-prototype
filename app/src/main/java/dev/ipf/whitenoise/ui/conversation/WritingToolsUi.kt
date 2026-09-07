package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseCallout
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalWritingScenario = staticCompositionLocalOf<() -> WritingScenario> { { WritingScenario.Ready } }
internal object WritingSelectionMenuKey

/** Requests belong to this composer instance and disappear on navigation/recreation. */
@Stable
internal class WritingToolsController {
    var session by mutableStateOf<WritingSession?>(null)
        private set
    private var current: WritingDraft? = null
    private var revision = 0L
    private var sequence = 0L
    fun observe(draft: WritingDraft) {
        if (current != draft) {
            current = draft
            revision++
            session = session?.copy(phase = WritingPhase.Stale, suggestion = null)
        }
    }
    fun open(selected: Boolean, scenario: WritingScenario) {
        session = current?.let { WritingTools.begin(++sequence, it, revision, selected, scenario) }
    }
    fun choose(operation: WritingOperation) { session = session?.takeIf { it.phase == WritingPhase.Choose }?.let { WritingTools.choose(it, operation) } ?: session }
    fun complete(id: Long) {
        val value = session?.takeIf { it.id == id } ?: return
        session = current?.let { WritingTools.finish(value, it, revision) }
    }
    fun proceed() {
        session = session?.let { value -> when (value.phase) {
            WritingPhase.DownloadRequired -> value.copy(phase = WritingPhase.Downloading)
            WritingPhase.NetworkConsent -> value.copy(phase = WritingPhase.Loading, scenario = WritingScenario.Ready)
            WritingPhase.Unavailable, WritingPhase.Timeout, WritingPhase.Refused -> value.copy(phase = WritingPhase.Loading, scenario = WritingScenario.Ready)
            else -> value
        } }
    }
    fun apply(): WritingReplacement? {
        val value = session ?: return null
        val replacement = current?.let { WritingTools.apply(value, it, revision) }
        if (replacement != null) session = null
        return replacement
    }
    fun close() { session = null }
}

internal val WritingOperation.label: Int get() = when (this) {
    WritingOperation.Proofread -> R.string.writing_proofread
    WritingOperation.Rewrite -> R.string.writing_rewrite
    WritingOperation.Summarize -> R.string.writing_summarize
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WritingToolsSheet(controller: WritingToolsController, onApply: () -> Unit) {
    val session = controller.session ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.close() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(session.id, session.phase) {
        if (session.phase == WritingPhase.Loading || session.phase == WritingPhase.Downloading) {
            delay(if (session.phase == WritingPhase.Downloading) 900 else 500)
            controller.complete(session.id)
        }
    }
    val maximumHeight = LocalWindowInfo.current.containerDpSize.height * 0.88f
    WhiteNoiseModalBottomSheet(onDismissRequest = controller::close, sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))) {
        Column(Modifier.fillMaxWidth().heightIn(max = maximumHeight)
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
            .testTag("writing.sheet")) {
            Text(stringResource(session.operation?.label ?: R.string.writing_tools), modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(WhiteNoiseSpacing.Related))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                if (session.phase == WritingPhase.Choose) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = dev.ipf.whitenoise.ui.theme.amoledOutlineBorder()) {
                        Text(session.original, Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin)
                            .testTag("writing.input"), style = MaterialTheme.typography.bodyLarge,
                            maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                    Column {
                        WritingOperation.entries.forEach { operation ->
                            ListItem(
                                headlineContent = { Text(stringResource(operation.label)) },
                                leadingContent = {
                                    Icon(painterResource(when (operation) {
                                        WritingOperation.Proofread -> R.drawable.ic_check
                                        WritingOperation.Rewrite -> R.drawable.ic_edit
                                        WritingOperation.Summarize -> R.drawable.ic_format_list_bulleted
                                    }), contentDescription = null, modifier = Modifier.size(24.dp))
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth().testTag("writing.operation.${operation.name}")
                                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { controller.choose(operation) },
                            )
                        }
                    }
                } else if (session.phase == WritingPhase.Preview) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = dev.ipf.whitenoise.ui.theme.amoledOutlineBorder()) {
                        Column {
                            WritingPreview(R.string.writing_original, session.original, "writing.original", muted = true)
                            HorizontalDivider(Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin))
                            WritingPreview(R.string.writing_suggestion, session.suggestion.orEmpty(), "writing.suggestion")
                        }
                    }
                    WhiteNoiseCallout(
                        text = stringResource(if (session.suggestion == session.original) R.string.writing_no_changes else when (session.operation) {
                            WritingOperation.Proofread -> R.string.writing_proofread_changes
                            WritingOperation.Rewrite -> R.string.writing_rewrite_changes
                            else -> R.string.writing_summarize_changes
                        }),
                        modifier = Modifier.testTag("writing.result_note").semantics { liveRegion = LiveRegionMode.Polite },
                    )
                } else {
                    val message = when (session.phase) {
                        WritingPhase.Loading -> R.string.writing_working
                        WritingPhase.Downloading -> R.string.writing_downloading
                        WritingPhase.DownloadRequired -> R.string.writing_download_required
                        WritingPhase.NetworkConsent -> R.string.writing_external_disclosure
                        WritingPhase.Unavailable -> R.string.writing_unavailable
                        WritingPhase.Timeout -> R.string.writing_timeout
                        WritingPhase.Refused -> R.string.writing_refused
                        WritingPhase.TooLong -> R.string.writing_too_long
                        else -> R.string.writing_stale
                    }
                    if (session.phase in setOf(WritingPhase.Loading, WritingPhase.Downloading)) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(message), Modifier.testTag("writing.status.${session.phase.name}").semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            FlowRow(Modifier.fillMaxWidth().padding(vertical = WhiteNoiseSpacing.Related), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = controller::close, modifier = Modifier.testTag("writing.discard")) {
                    Text(stringResource(if (session.phase == WritingPhase.Preview) R.string.writing_discard else R.string.cancel))
                }
                when (session.phase) {
                    WritingPhase.Preview -> Button(onClick = onApply, enabled = session.suggestion != session.original,
                        modifier = Modifier.testTag("writing.apply")) { Text(stringResource(R.string.writing_apply)) }
                    WritingPhase.DownloadRequired -> Button(onClick = controller::proceed, modifier = Modifier.testTag("writing.proceed")) { Text(stringResource(R.string.writing_download)) }
                    WritingPhase.NetworkConsent -> Button(onClick = controller::proceed, modifier = Modifier.testTag("writing.proceed")) { Text(stringResource(R.string.writing_continue)) }
                    WritingPhase.Unavailable, WritingPhase.Timeout, WritingPhase.Refused -> Button(onClick = controller::proceed,
                        modifier = Modifier.testTag("writing.retry")) { Text(stringResource(R.string.history_retry)) }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun WritingPreview(title: Int, text: String, tag: String, muted: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, Modifier.fillMaxWidth().testTag(tag), style = MaterialTheme.typography.bodyLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}
