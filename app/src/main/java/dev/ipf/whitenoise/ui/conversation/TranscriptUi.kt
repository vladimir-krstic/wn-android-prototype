package dev.ipf.whitenoise.ui.conversation

import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.TranscriptController
import dev.ipf.whitenoise.ui.settings.SettingsGroup
import dev.ipf.whitenoise.ui.settings.SettingsAction
import androidx.compose.ui.res.painterResource
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.*

internal val LocalTranscript = staticCompositionLocalOf<TranscriptController?> { null }

/** Launcher lives above navigation so Back never reassigns a pending result to a different chat. */
@Composable
internal fun TranscriptHost(controller: TranscriptController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var launchedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val id = launchedId
        launchedId = null
        if (id != null && uri == null) controller.cancel(id)
        else if (uri != null) {
            val document = id?.let(controller::takeForWriting)
            val fail = controller.work?.scenario == TranscriptScenario.WriteFailure
            scope.launch {
                var success = false
                try {
                    success = withContext(Dispatchers.IO) {
                        if (document == null || fail) false else runCatching {
                            checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(document.toByteArray(Charsets.UTF_8)) }
                        }.isSuccess
                    }
                    if (id != null) success = controller.saved(id, success) && success
                    else success = false
                } finally {
                    if (!success) withContext(NonCancellable + Dispatchers.IO) {
                        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    }
                }
            }
        }
    }
    SideEffect { controller.reconcile() }
    DisposableEffect(controller) { onDispose { controller.interruptWriting() } }
    controller.work?.let { w ->
        LaunchedEffect(w.id, w.phase, w.readCount, launchedId) {
            when (w.phase) {
                TranscriptPhase.Reading -> { delay(250); controller.advance(w.id) }
                TranscriptPhase.Encoding -> {
                    val document = withContext(Dispatchers.Default) { runCatching { ConversationTranscript.encode(w.source, w.entries) }.getOrNull() }
                    controller.encoded(w.id, document)
                }
                TranscriptPhase.ChoosingDestination -> if (launchedId == null) {
                    launchedId = w.id
                    try { launcher.launch("white-noise-transcript.json") } catch (_: Exception) {
                        launchedId = null; controller.destinationFailed(w.id)
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
internal fun TranscriptPanel(profile: Profile, chat: Chat) {
    val controller = LocalTranscript.current ?: return
    val work = controller.work?.takeIf { it.source.profileId == profile.id && it.source.chatId == chat.id }
    SettingsGroup(Modifier.padding(vertical = WhiteNoiseSpacing.Related)) {
        row {
            SettingsAction(
                title = stringResource(R.string.transcript_export),
                onClick = { controller.begin(GroupOwner(profile.id, chat.id)) },
                enabled = controller.work?.busy != true,
                leading = { Icon(painterResource(R.drawable.ic_download), contentDescription = null) },
            )
        }
    }
    if (work == null) return
    val preparing = work.phase in setOf(TranscriptPhase.Reading, TranscriptPhase.Encoding)
    val cancellable = preparing || work.phase in setOf(TranscriptPhase.Ready, TranscriptPhase.ChoosingDestination)
    val dismiss = {
        // An accepted stream write finishes before dismissal; closing preparation never launches Files.
        if (work.phase != TranscriptPhase.Writing) {
            if (cancellable) controller.cancel(work.id)
            controller.dismiss(work.id)
        }
    }
    val statusResource = when (work.phase) {
        TranscriptPhase.Reading, TranscriptPhase.Encoding -> R.string.transcript_preparing
        TranscriptPhase.Ready -> R.string.transcript_ready
        TranscriptPhase.ChoosingDestination -> R.string.transcript_destination
        TranscriptPhase.Writing -> R.string.transcript_writing
        TranscriptPhase.Saved -> R.string.transcript_saved
        TranscriptPhase.Cancelled -> R.string.transcript_cancelled
        TranscriptPhase.Failed -> when (work.failure) {
            TranscriptFailure.SourceUnavailable -> R.string.transcript_unavailable
            TranscriptFailure.Destination -> R.string.transcript_destination_failed
            TranscriptFailure.Write -> R.string.transcript_write_failed
            else -> R.string.transcript_prepare_failed
        }
    }
    AlertDialog(
        modifier = Modifier.testTag("transcript.dialog"),
        onDismissRequest = dismiss,
        icon = { Icon(painterResource(R.drawable.ic_download), contentDescription = null) },
        title = { Text(stringResource(R.string.transcript_export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                // Measure every localized status at the actual width/font scale so state
                // changes do not resize the dialog. Only the current status is exposed.
                Box {
                    listOf(
                        R.string.transcript_preparing, R.string.transcript_ready,
                        R.string.transcript_destination, R.string.transcript_writing,
                        R.string.transcript_saved, R.string.transcript_cancelled,
                        R.string.transcript_unavailable, R.string.transcript_destination_failed,
                        R.string.transcript_write_failed, R.string.transcript_prepare_failed,
                    ).forEach { resource ->
                        Text(stringResource(resource), Modifier.alpha(0f).clearAndSetSemantics {})
                    }
                    Text(stringResource(statusResource), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Text(pluralStringResource(R.plurals.transcript_event_count, work.readCount, work.readCount))
                if (preparing || work.phase == TranscriptPhase.Writing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { if (work.phase in setOf(TranscriptPhase.Ready, TranscriptPhase.ChoosingDestination, TranscriptPhase.Saved)) 1f else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = work.phase in setOf(TranscriptPhase.Ready, TranscriptPhase.Failed),
                onClick = {
                    if (work.phase == TranscriptPhase.Failed) controller.retry(work.id)
                    else controller.save(work.id)
                },
            ) {
                Text(stringResource(if (work.phase == TranscriptPhase.Failed) R.string.lifecycle_retry else R.string.transcript_save))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss, enabled = work.phase != TranscriptPhase.Writing) {
                Text(stringResource(if (cancellable) R.string.cancel else R.string.lifecycle_dismiss))
            }
        },
    )
}
