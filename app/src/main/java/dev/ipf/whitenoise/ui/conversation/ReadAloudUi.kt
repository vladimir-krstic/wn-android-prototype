package dev.ipf.whitenoise.ui.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

@Composable
internal fun ReadAloudHost(profile: Profile?, onSource: (SpeechReturnTarget) -> Unit,
    modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    val controller = LocalReadAloudController.current ?: rememberOwnedReadAloudController()
    val liveProfile = rememberUpdatedState(profile)
    val liveSource = rememberUpdatedState(onSource)
    SideEffect { controller.profile = { liveProfile.value }; controller.onSource = { liveSource.value(it) }; controller.reconcile() }
    val session = controller.session?.takeIf { it.owner.profileId == profile?.id }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(session?.id, session?.revision, lifecycle) {
        if (session?.phase == SpeechPhase.Loading) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(350); controller.settleEdge(session.id, session.revision)
        }
    }
    CompositionLocalProvider(LocalReadAloudController provides controller) {
        val visible = session != null && controller.modalCount == 0
        Column(modifier.fillMaxSize()) {
            content(Modifier.weight(1f).then(if (visible) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).union(WindowInsets.ime)) else Modifier))
            if (visible) Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                ReadAloudTransport(controller, Modifier.navigationBarsPadding().imePadding())
            }
        }
    }
}

@Composable
internal fun ReadAloudModal(controller: ReadAloudController) {
    DisposableEffect(controller) { controller.modalCount++; onDispose { controller.modalCount-- } }
}

@Composable
internal fun ReadAloudTransport(controller: ReadAloudController, modifier: Modifier = Modifier,
    onReturn: () -> Unit = { controller.returnToSource() }, onResumeFollowing: () -> Unit = onReturn) {
    val session = controller.session ?: return
    val position = stringResource(R.string.speech_position, session.sentenceIndex + 1, session.current.sentences.size,
        session.messageIndex + 1, session.catalog.size)
    val phase = session.phase
    val status = when (phase) {
        SpeechPhase.Paused -> R.string.speech_paused
        SpeechPhase.Loading -> if (session.edge!!.targetIndex < session.windowStart) R.string.speech_loading_earlier else R.string.speech_loading_later
        SpeechPhase.EdgeError -> R.string.speech_history_error
        SpeechPhase.EngineError -> R.string.speech_engine_error
        SpeechPhase.Unavailable -> R.string.speech_source_unavailable
        SpeechPhase.Completed -> R.string.speech_finished
        SpeechPhase.Speaking -> null
    }
    val maxHeight = (with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.45f).coerceAtLeast(96.dp)
    AdaptiveContent(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin).testTag("speech.transport")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (session.returnTarget != null) TextButton(onClick = onReturn,
                        modifier = Modifier.testTag("speech.return")) {
                        Column {
                            Text(stringResource(R.string.speech_return), style = MaterialTheme.typography.labelLarge)
                            Text(session.sentence.text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (phase != SpeechPhase.Unavailable) Text(session.sentence.text, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    Text(status?.let { stringResource(it) } ?: position,
                        Modifier.testTag("speech.status"), style = MaterialTheme.typography.labelMedium,
                        color = if (phase in setOf(SpeechPhase.EdgeError, SpeechPhase.EngineError, SpeechPhase.Unavailable)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = controller::stop, modifier = Modifier.testTag("speech.stop")) {
                    Icon(painterResource(R.drawable.ic_stop), stringResource(R.string.stop_reading))
                }
            }
            if (phase != SpeechPhase.Unavailable) {
                if (status != null) Text(position, style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(progress = { session.progress }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = position }, drawStopIndicator = {})
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { controller.move(SpeechMove.PreviousSentence) }, enabled = session.navigable,
                    modifier = Modifier.testTag("speech.previousSentence")) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.speech_previous_sentence))
                }
                FilledTonalIconButton(onClick = { if (phase == SpeechPhase.Speaking) controller.pause() else controller.resume() },
                    enabled = phase in setOf(SpeechPhase.Speaking, SpeechPhase.Paused), modifier = Modifier.testTag("speech.pauseResume")) {
                    Icon(painterResource(if (phase == SpeechPhase.Speaking) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        stringResource(if (phase == SpeechPhase.Speaking) R.string.speech_pause else R.string.speech_resume))
                }
                IconButton(onClick = { controller.move(SpeechMove.NextSentence) }, enabled = session.navigable,
                    modifier = Modifier.testTag("speech.nextSentence")) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.speech_next_sentence), Modifier.rotate(180f))
                }
                if (phase in setOf(SpeechPhase.EdgeError, SpeechPhase.EngineError)) TextButton(onClick = controller::retry,
                    modifier = Modifier.testTag("speech.retry")) { Text(stringResource(R.string.attachment_retry)) }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { controller.move(SpeechMove.PreviousMessage) }, enabled = session.navigable,
                    modifier = Modifier.testTag("speech.previousMessage")) { Text(stringResource(R.string.speech_previous_message)) }
                TextButton(onClick = { controller.move(SpeechMove.NextMessage) }, enabled = session.navigable,
                    modifier = Modifier.testTag("speech.nextMessage")) { Text(stringResource(R.string.speech_next_message)) }
                if (session.returnTarget != null && phase != SpeechPhase.Completed) TextButton(onClick = {
                    controller.follow(!session.following)
                    if (!session.following) onResumeFollowing()
                },
                    modifier = Modifier.testTag("speech.follow")) {
                    Text(stringResource(if (session.following) R.string.speech_pause_follow else R.string.speech_resume_follow))
                }
            }
        }
    }
}

/** All sentence destinations are ordinary lazy native buttons, independent of selection gestures. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeechSentenceChooser(message: ChatMessage, onRead: (Int) -> Unit, onDismiss: () -> Unit) {
    val locale = androidx.core.os.ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.ROOT
    val sentences = remember(message.text, locale) { SpeechDocuments.sentences(SpeechDocuments.project(message.text), locale) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onDismiss)
        Scaffold(Modifier.fillMaxSize().testTag("speech.sentences"), contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { TopAppBar(title = { Text(stringResource(R.string.speech_choose_sentence)) }, navigationIcon = {
                IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
            }) }) { padding -> AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(sentences) { index, sentence ->
                    val label = stringResource(R.string.speech_read_sentence, index + 1)
                    TextButton(onClick = { SpeechDocuments.range(sentence)?.let { onRead(it.first) } },
                        modifier = Modifier.fillMaxWidth().testTag("speech.sentence.$index").semantics { stateDescription = label }) {
                        Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.Related)) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                            Text(sentence.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        } }
    }
}

internal val LocalSpeechOwner = staticCompositionLocalOf<SpeechOwner?> { null }

@Composable
internal fun Modifier.observeSpeechScroll(controller: ReadAloudController, enabled: Boolean = true): Modifier {
    val liveEnabled = rememberUpdatedState(enabled)
    val observer = remember(controller) { object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (liveEnabled.value && source == NestedScrollSource.UserInput && available != Offset.Zero) controller.follow(false)
            return Offset.Zero
        }
    } }
    return nestedScroll(observer)
}

@Composable
internal fun ReadAloudReaderBar(content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.55f
    Column(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(max = maxHeight)
        .verticalScroll(rememberScrollState()).padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin), content = content)
}
