package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import dev.ipf.whitenoise.state.ComposerCaptureController
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalComposerCapture = staticCompositionLocalOf<ComposerCaptureController?> { null }

@Composable
internal fun ComposerCaptureHost(controller: ComposerCaptureController, content: @Composable () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    SideEffect { controller.reconcile() }
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.background() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); controller.background() }
    }
    CompositionLocalProvider(LocalComposerCapture provides controller, content = content)
}

@Composable
internal fun DictationOriginHost(profile: Profile, chat: Chat) {
    val controller = LocalComposerCapture.current ?: return
    val owner = remember(profile.id, chat.id) { ComposerCaptureOwner(profile.id, chat.id) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val speech = LocalReadAloudController.current
    DisposableEffect(controller, owner) { controller.open(owner); onDispose { controller.close(owner) } }
    val attempt = controller.attempts[owner] ?: return
    var open by rememberSaveable(owner.profileId, owner.chatId, attempt.id) { mutableStateOf(true) }
    LaunchedEffect(controller.presentationRevision) { if (controller.presentationOwner == owner) open = true }
    LaunchedEffect(attempt.id, attempt.capturing) { if (attempt.capturing) speech?.pause() }
    LaunchedEffect(attempt.id, attempt.revision, lifecycle) {
        if (attempt.capturing) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val wait = when {
                attempt.phase == DictationPhase.Preparing && attempt.scenario == DictationScenario.ReadinessTimeout -> 1_500L
                attempt.phase == DictationPhase.Processing && attempt.scenario == DictationScenario.ProcessingTimeout -> 20_000L
                attempt.phase == DictationPhase.Listening -> 100L
                else -> 350L
            }
            delay(wait); controller.advance(owner, attempt.id, attempt.revision)
        }
    }
    androidx.activity.compose.BackHandler(enabled = attempt.capturing) { controller.cancel(owner, attempt.id) }
    if (attempt.capturing) return
    if (!open || attempt.terminal) return
    val context = LocalContext.current
    var copied by remember(attempt.id) { mutableStateOf(false) }
    var settingsFailed by remember(attempt.id) { mutableStateOf(false) }
    val dismiss = {
        if (attempt.phase == DictationPhase.Review) open = false else controller.cancel(owner, attempt.id)
    }
    val title = stringResource(when (attempt.phase) {
        DictationPhase.Disclosure -> R.string.dictation_disclosure_title
        DictationPhase.Review -> R.string.dictation_review
        else -> R.string.dictation_title
    })
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Text(chat.title, style = MaterialTheme.typography.titleSmall)
            if (attempt.phase == DictationPhase.Disclosure) Text(stringResource(R.string.dictation_disclosure_detail))
            else {
                if (attempt.capturing) {
                    Text(stringResource(when (attempt.phase) {
                        DictationPhase.Preparing -> R.string.dictation_preparing
                        DictationPhase.Listening -> R.string.dictation_listening
                        else -> R.string.dictation_processing
                    }), Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("dictation.status"))
                    if (attempt.phase != DictationPhase.Listening) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                attempt.failure?.let { Text(stringResource(dictationFailureString(it)), color = MaterialTheme.colorScheme.error) }
                if (attempt.phase == DictationPhase.Review && attempt.reviewReason != DictationReviewReason.RecognitionFailure)
                    Text(stringResource(R.string.dictation_review_detail))
                if (attempt.retainedText.isNotBlank()) SelectionContainer {
                    Text(attempt.retainedText, Modifier.testTag("dictation.transcript"))
                }
                if (attempt.phase == DictationPhase.Review) {
                    TextButton(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText(title, attempt.retainedText)); copied = true
                    }, enabled = attempt.retainedText.isNotBlank()) { Text(stringResource(if (copied) R.string.dictation_copied else R.string.copy)) }
                }
                if (attempt.failure in setOf(DictationFailure.PermissionPermanentlyDenied, DictationFailure.PermissionDenied, DictationFailure.ServiceMissing)) {
                    TextButton(onClick = { settingsFailed = runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }.isFailure }) {
                        Text(stringResource(R.string.speech_android_settings))
                    }
                }
                if (settingsFailed) Text(stringResource(R.string.speech_settings_failed))
            }
        }
    }, confirmButton = {
        when (attempt.phase) {
            DictationPhase.Disclosure -> TextButton(onClick = { controller.acceptDisclosure(owner, attempt.id) }) { Text(stringResource(R.string.dictation_start)) }
            DictationPhase.Listening -> TextButton(onClick = { controller.finish(owner, attempt.id) }) { Text(stringResource(R.string.dictation_done)) }
            DictationPhase.Review -> TextButton(onClick = { controller.insertAtEnd(owner, attempt.id) },
                enabled = controller.available(owner) && attempt.retainedText.isNotBlank()) { Text(stringResource(R.string.dictation_insert)) }
            DictationPhase.Failed -> TextButton(onClick = { controller.retry(owner, attempt.id) }, enabled = controller.available(owner)) { Text(stringResource(R.string.dictation_retry)) }
            else -> Unit
        }
    }, dismissButton = {
        TextButton(onClick = { controller.cancel(owner, attempt.id) }) {
            Text(stringResource(if (attempt.phase == DictationPhase.Review) R.string.dictation_discard else R.string.cancel))
        }
    })
}

@Composable
internal fun DictationActiveControls(owner: ComposerCaptureOwner) {
    val controller = LocalComposerCapture.current ?: return
    val attempt = controller.attempts[owner]?.takeIf { it.capturing } ?: return
    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related)
        .testTag("dictation.controls"), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        Text(stringResource(when (attempt.phase) {
            DictationPhase.Preparing -> R.string.dictation_preparing
            DictationPhase.Listening -> R.string.dictation_listening
            else -> R.string.dictation_processing
        }), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.labelLarge)
        if (attempt.retainedText.isNotBlank()) Text(attempt.retainedText, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.testTag("dictation.partial"))
        if (attempt.phase != DictationPhase.Listening) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            TextButton(onClick = { controller.cancel(owner, attempt.id) }) { Text(stringResource(R.string.cancel)) }
            if (attempt.phase == DictationPhase.Listening) TextButton(onClick = { controller.finish(owner, attempt.id) }) { Text(stringResource(R.string.dictation_done)) }
        }
    }
}

internal fun dictationFailureString(failure: DictationFailure): Int = when (failure) {
    DictationFailure.ServiceMissing -> R.string.dictation_service_missing
    DictationFailure.PermissionDenied -> R.string.dictation_permission_denied
    DictationFailure.PermissionPermanentlyDenied -> R.string.dictation_permission_permanent
    DictationFailure.MicrophoneBusy -> R.string.dictation_microphone_busy
    DictationFailure.NoSpeech -> R.string.dictation_no_speech
    DictationFailure.Network -> R.string.dictation_network
    DictationFailure.ServiceBusy -> R.string.dictation_service_busy
    DictationFailure.TimedOut -> R.string.dictation_timeout
    DictationFailure.Unknown -> R.string.dictation_failed
}
