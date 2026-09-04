package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalMessageOperationCover = staticCompositionLocalOf { false }

/** Foreground operation stepping outlives any particular picker or conversation route. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageOperationsHost(profile: Profile?, forward: MessageForwardOperation?,
    onAdvanceForward: (Long, Int) -> Unit, onAdvanceDelete: (String, Long, Int) -> Unit,
    onRetry: (Long) -> Unit, onCancel: (Long) -> Unit, onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier, onAutomaticRetry: (Long, Int) -> Unit = { _, _ -> }, content: @Composable (Modifier) -> Unit) {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val deletions=profile?.chats.orEmpty().mapNotNull { it.messageDeletion?.takeIf { op->op.isRunning } }
    val steps=deletions.map { Triple(it.chatId,it.id,it.revision) }
    LaunchedEffect(profile?.id,forward?.id,forward?.revision,steps,lifecycle) {
        if (forward?.isRunning==true || steps.isNotEmpty()) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(180)
            if(forward?.isRunning==true) onAdvanceForward(forward.id,forward.revision)
            steps.forEach { (chat,id,revision)->onAdvanceDelete(chat,id,revision) }
        }
    }
    LaunchedEffect(profile?.id, forward?.id, forward?.revision, lifecycle) {
        if (forward?.canAutomaticallyRetry == true) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(forward.automaticRetryDelayMillis)
            onAutomaticRetry(forward.id, forward.revision)
        }
    }
    var details by rememberSaveable(profile?.id,forward?.id) { mutableStateOf(false) }
    CompositionLocalProvider(LocalMessageOperationCover provides details) {
        Column(modifier.fillMaxSize()) {
            content(Modifier.weight(1f).then(if(forward!=null) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) else Modifier))
            if(forward!=null) Surface(color=MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().testTag("message.forward.status")) {
                    if(forward.isRunning) LinearProgressIndicator(progress={forward.progress},modifier=Modifier.fillMaxWidth())
                    FlowRow(Modifier.fillMaxWidth().padding(horizontal=WhiteNoiseSpacing.CompactScreenMargin),
                        horizontalArrangement=Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        Text(forwardSummary(forward),Modifier.weight(1f).padding(vertical=WhiteNoiseSpacing.Related)
                            .semantics { liveRegion=LiveRegionMode.Polite },style=MaterialTheme.typography.bodyMedium)
                        TextButton(onClick={details=true}) { Text(stringResource(R.string.batch_details)) }
                        if(!forward.isRunning) TextButton(onClick={onDismiss(forward.id)}) { Text(stringResource(R.string.batch_dismiss)) }
                    }
                }
            }
        }
        if(details&&forward!=null) ModalBottomSheet(onDismissRequest={details=false}) {
            WhiteNoiseSheetHeader(stringResource(R.string.batch_forward_title))
            Column(Modifier.fillMaxWidth().heightIn(max=560.dp)
                .verticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin).testTag("message.forward.details"),
                verticalArrangement=Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                Text(forwardSummary(forward),style=MaterialTheme.typography.titleMedium)
                if(forward.canAutomaticallyRetry) Text(stringResource(R.string.batch_automatic_retry))
                if(forward.phase==MessageForwardPhase.Preparing) Text(stringResource(R.string.batch_preparing_media,forward.prepared,forward.totalAttachments))
                forward.targets.forEach { target ->
                    Surface(color=MaterialTheme.colorScheme.surfaceContainerLow,shape=MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin).testTag("message.forward.target.${target.chatId}"),
                            verticalArrangement=Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                            Text(target.title.ifBlank { stringResource(R.string.batch_chat_unavailable) },style=MaterialTheme.typography.titleSmall)
                            Text(target.failure?.let { forwardFailureText(it) } ?: forwardTargetText(target),
                                color=if(target.failure!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.batch_target_progress,target.uploaded,forward.totalAttachments,target.sent,forward.messages.size),style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    if(forward.canRetry) TextButton(onClick={onRetry(forward.id)}) { Text(stringResource(R.string.batch_retry_failed)) }
                    if(forward.canCancel) TextButton(onClick={onCancel(forward.id)}) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick={details=false}) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}

@Composable
internal fun MessageDeletionNotice(operation: MessageDeleteOperation,onRetry: ()->Unit,onDismiss: ()->Unit) {
    val context=LocalContext.current
    val failed=operation.failed.isNotEmpty()
    Surface(color=if(failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor=if(failed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.Related).testTag("message.delete.status")
            .semantics { liveRegion=LiveRegionMode.Polite }) {
            if(operation.isRunning) LinearProgressIndicator(progress={operation.items.count { it.phase!=MessageDeletePhase.Pending }.toFloat()/operation.items.size},modifier=Modifier.fillMaxWidth())
            Text(pluralStringResource(R.plurals.batch_delete_progress,operation.items.size,operation.succeeded,operation.items.size),style=MaterialTheme.typography.titleSmall)
            if(failed) {
                val remote=operation.failed.count { it.scope==MessageDeletionScope.ForEveryone }
                val local=operation.failed.size-remote
                Text(when {
                    remote>0&&local>0->stringResource(R.string.batch_delete_failures,remote,local)
                    remote>0->stringResource(R.string.batch_delete_failed_remote,remote)
                    else->stringResource(R.string.batch_delete_failed_local,local)
                },style=MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                val colors=ButtonDefaults.textButtonColors(contentColor=LocalContentColor.current)
                if(operation.canRetry) TextButton(onClick=onRetry,colors=colors) { Text(stringResource(R.string.batch_retry_failed)) }
                if(!operation.isRunning) TextButton(onClick=onDismiss,colors=colors) { Text(stringResource(R.string.batch_dismiss)) }
                if(failed) TextButton(onClick={ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Message deletion report",operation.report())) },colors=colors) { Text(stringResource(R.string.batch_copy_report)) }
            }
        }
    }
}

@Composable
internal fun ForwardFolderChoice(folder: ChatFolder,members: List<String>,selected: Set<String>,onToggle: ()->Unit) {
    val state=when { members.isNotEmpty()&&members.all { it in selected }->ToggleableState.On;members.any { it in selected }->ToggleableState.Indeterminate;else->ToggleableState.Off }
    Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLowest) {
        ListItem(modifier=Modifier.triStateToggleable(state=state,enabled=members.isNotEmpty(),role=Role.Checkbox,onClick=onToggle)
            .testTag("conversation.forward.folder.${folder.id}"),
            colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surfaceContainerLowest),
            supportingContent={Text(if(members.isEmpty()) stringResource(R.string.batch_folder_empty) else pluralStringResource(R.plurals.batch_folder_count,members.size,members.size))},
            trailingContent={TriStateCheckbox(state=state,onClick=null,enabled=members.isNotEmpty())}) { Text(folder.name) }
    }
}
@Composable
internal fun forwardSummary(op: MessageForwardOperation): String = when(op.phase) {
    MessageForwardPhase.Preparing->stringResource(R.string.batch_preparing)
    MessageForwardPhase.Running->pluralStringResource(R.plurals.batch_forward_running,op.targets.size,op.succeeded,op.targets.size)
    MessageForwardPhase.Completed,MessageForwardPhase.PartialFailure->pluralStringResource(R.plurals.batch_forward_complete,op.targets.size,op.succeeded,op.targets.size)
    MessageForwardPhase.Failed->stringResource(R.string.batch_forward_failed)
    MessageForwardPhase.Cancelled->stringResource(R.string.batch_forward_cancelled)
}
@Composable
private fun forwardTargetText(target: MessageForwardTarget): String = stringResource(when(target.phase) {
    MessageForwardTargetPhase.Waiting->R.string.batch_waiting
    MessageForwardTargetPhase.Uploading->R.string.batch_uploading
    MessageForwardTargetPhase.Sending->R.string.batch_sending
    MessageForwardTargetPhase.Completed->R.string.batch_sent
    MessageForwardTargetPhase.Failed->R.string.batch_forward_failed
    MessageForwardTargetPhase.Cancelled->R.string.batch_forward_cancelled
})
@Composable
internal fun forwardFailureText(failure: MessageForwardFailure): String = stringResource(when(failure) {
    MessageForwardFailure.Preparation->R.string.batch_prepare_failed
    MessageForwardFailure.PreparationTimeout->R.string.batch_prepare_timeout
    MessageForwardFailure.Upload->R.string.batch_upload_failed
    MessageForwardFailure.Send->R.string.batch_send_failed
    MessageForwardFailure.PayloadTooLarge->R.string.batch_payload_large
    MessageForwardFailure.Expired->R.string.batch_source_expired
    MessageForwardFailure.SessionChanged->R.string.batch_session_changed
    MessageForwardFailure.SourceUnavailable->R.string.batch_source_unavailable
    MessageForwardFailure.Invitation->R.string.batch_invitation
    MessageForwardFailure.Left->R.string.batch_left
    MessageForwardFailure.Removed->R.string.batch_removed
    MessageForwardFailure.Blocked->R.string.batch_blocked
    MessageForwardFailure.MissingRelays->R.string.batch_missing_relays
    MessageForwardFailure.TargetUnavailable->R.string.batch_chat_unavailable
})
