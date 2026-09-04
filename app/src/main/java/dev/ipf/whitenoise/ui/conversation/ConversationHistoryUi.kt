package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

/** Screen-owned requests cancel when the conversation leaves composition; only stable IDs are saved. */
internal class ConversationHistoryUiState(window: Set<String>, observed: Set<String>, boundary: String?) {
    var windowIds by mutableStateOf(window)
    var observedIds by mutableStateOf(observed)
    var boundaryId by mutableStateOf(boundary)
    var jump by mutableStateOf(ConversationUnreadJump())
    var request by mutableStateOf<HistoryRequest?>(null)
    var readyTarget by mutableStateOf<HistoryRequest?>(null)
    var scan by mutableStateOf<List<ConversationSearchResult>?>(null)
    var scanning by mutableStateOf(false)
    var scanFailed by mutableStateOf(false)
    var scanRetry by mutableIntStateOf(0)
    private var generation = 0L

    fun page(operation: HistoryOperation, scenario: HistoryScenario) {
        request = HistoryRequest(++generation, operation, scenario)
        readyTarget = null
    }
    fun target(chat: Chat, id: String, scenario: HistoryScenario, markThrough: Boolean = false, offset: Int = 0, highlight: Boolean = true) {
        val next = HistoryRequest(++generation, HistoryOperation.Target, scenario, id, markThrough, scrollOffset = offset, highlight = highlight)
        readyTarget = null
        if (id in windowIds && ConversationHistory.target(chat, id) != null && scenario == HistoryScenario.Success) {
            request = null; readyTarget = next
        } else request = next
    }
    fun cancel() { generation++; request = null; readyTarget = null }
    fun retry() { request?.let { request = it.copy(id = ++generation, scenario = HistoryScenario.Success, phase = HistoryPhase.Loading) } }
    fun complete(chat: Chat, expected: HistoryRequest) {
        if (request?.id != expected.id || request?.phase != HistoryPhase.Loading) return
        if (expected.scenario != HistoryScenario.Success) {
            request = expected.copy(phase = if (expected.scenario == HistoryScenario.TargetUnavailable) HistoryPhase.Unavailable else HistoryPhase.Failed)
            return
        }
        if (expected.operation == HistoryOperation.Target) {
            val ids = expected.targetId?.let { ConversationHistory.target(chat, it) }
            if (ids == null) request = expected.copy(phase = HistoryPhase.Unavailable)
            else { windowIds = ids; request = null; readyTarget = expected }
        } else {
            windowIds = ConversationHistory.page(chat, windowIds, expected.operation)
            request = null
        }
    }
    fun reconcileArrivals(chat: Chat, profileId: String, followTail: Boolean) {
        val entries = ConversationProjection.orderedEntries(chat)
        val ids = entries.mapTo(hashSetOf()) { it.id }
        val added = entries.filter { it.id !in observedIds }
        val ownSend = added.any { it is ChatTimelineEntry.Message && it.message.authorId == profileId }
        windowIds = when {
            ownSend -> windowIds + entries.dropWhile { it.id !in windowIds }.map { it.id }
            followTail -> windowIds + added.map { it.id }
            else -> windowIds
        }.intersect(ids)
        if (windowIds.isEmpty() && entries.isNotEmpty()) windowIds = ConversationHistory.initial(chat)
        observedIds = ids
    }
}

private val HistoryUiSaver = listSaver<ConversationHistoryUiState, Any>(
    save = { listOf(ArrayList(it.windowIds), ArrayList(it.observedIds), it.boundaryId.orEmpty(), it.jump.pendingId.orEmpty(), it.jump.stackActive, it.jump.initialized) },
    restore = {
        @Suppress("UNCHECKED_CAST")
        ConversationHistoryUiState((it[0] as List<String>).toSet(), (it[1] as List<String>).toSet(), (it[2] as String).ifEmpty { null }).apply {
            jump = ConversationUnreadJump((it[3] as String).ifEmpty { null }, it[4] as Boolean, it[5] as Boolean)
        }
    },
)

@Composable
internal fun rememberConversationHistory(profile: Profile, chat: Chat): ConversationHistoryUiState {
    val state = rememberSaveable(profile.id, chat.id, saver = HistoryUiSaver) {
        val read = chat.readState ?: ConversationReading.initial(chat, profile.id)
        ConversationHistoryUiState(ConversationHistory.initial(chat), chat.timeline.mapTo(hashSetOf()) { it.id }, ConversationReading.firstUnread(read, chat))
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentChat by rememberUpdatedState(chat)
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && (state.request?.phase == HistoryPhase.Loading || state.readyTarget != null)) state.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); state.cancel() }
    }
    val request = state.request
    LaunchedEffect(profile.id, chat.id, request?.id, request?.phase, lifecycle) {
        if (request?.phase == HistoryPhase.Loading) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(450)
            state.complete(currentChat, request)
        }
    }
    return state
}

@Composable
internal fun ConversationHistoryScan(state: ConversationHistoryUiState, profile: Profile, chat: Chat, query: String, onScenario: (HistoryOperation) -> HistoryScenario) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(profile.id, profile.name, profile.people, chat.id, query, chat.timeline, state.scanRetry, lifecycle) {
        state.scan = null; state.scanFailed = false; state.scanning = query.isNotBlank()
        var completed = false
        if (query.isNotBlank()) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!completed) {
                state.scanning = true
                delay(350)
                val outcome = onScenario(HistoryOperation.Search)
                if (outcome == HistoryScenario.SearchFails) state.scanFailed = true
                else state.scan = ConversationSearch.results(chat, profile, query)
                state.scanning = false
                completed = true
            }
        }
    }
}

@Composable
internal fun HistoryPageControl(operation: HistoryOperation, request: HistoryRequest?, onLoad: () -> Unit, onRetry: () -> Unit) {
    val active = request?.takeIf { it.operation == operation }
    Column(Modifier.fillMaxWidth().testTag("history.${operation.name}").padding(vertical = WhiteNoiseSpacing.Related)
        .semantics { liveRegion = LiveRegionMode.Polite }) {
        when (active?.phase) {
            HistoryPhase.Loading -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.history_loading)) }
            HistoryPhase.Failed, HistoryPhase.Unavailable -> { Text(stringResource(R.string.history_page_failed)); TextButton(onClick = onRetry) { Text(stringResource(R.string.history_retry)) } }
            null -> TextButton(onClick = onLoad, enabled = request?.phase != HistoryPhase.Loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (operation == HistoryOperation.Older) R.string.history_older else R.string.history_newer))
            }
        }
    }
}

@Composable
internal fun HistoryTargetFeedback(request: HistoryRequest, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("history.target").padding(vertical = WhiteNoiseSpacing.Related)
        .semantics { liveRegion = LiveRegionMode.Polite }) {
        if (request.phase == HistoryPhase.Loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(when (request.phase) {
            HistoryPhase.Loading -> R.string.history_target_loading
            HistoryPhase.Failed -> R.string.history_target_failed
            HistoryPhase.Unavailable -> R.string.global_message_unavailable
        }))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            if (request.phase != HistoryPhase.Loading) TextButton(onClick = onRetry) { Text(stringResource(R.string.history_retry)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@Composable
internal fun HistorySearchFeedback(scanning: Boolean, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("history.searchStatus").padding(vertical = WhiteNoiseSpacing.Related).semantics { liveRegion = LiveRegionMode.Polite }) {
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(if (scanning) R.string.history_searching else R.string.history_search_failed))
        if (!scanning) TextButton(onClick = onRetry) { Text(stringResource(R.string.history_retry)) }
    }
}
