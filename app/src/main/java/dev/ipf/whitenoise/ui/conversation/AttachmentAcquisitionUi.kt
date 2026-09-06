package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.ui.components.WhiteNoiseDialogChoiceRow
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import kotlinx.coroutines.delay

internal data class AttachmentEnvironment(
    val locationSession: LocationSession? = null,
    val openLocation: () -> Boolean = { false },
    val locationEvent: (Long, LocationEvent) -> String? = { _, _ -> null },
    val editorSession: PhotoEditorSession? = null,
    val openEditor: (String, Int) -> Boolean = { _, _ -> false },
    val editorEvent: (Long, PhotoEditorEvent) -> Boolean = { _, _ -> false },
    val transfer: (String, String, String, Long) -> Unit = { _, _, _, _ -> },
)
internal val LocalAttachmentEnvironment = staticCompositionLocalOf { AttachmentEnvironment() }

/** One foreground owner per chat, independent of the visible history window. */
@Composable
internal fun AttachmentTransferHost(chat: Chat) {
    val owner = LocalLifecycleOwner.current
    val action = rememberUpdatedState(LocalAttachmentEnvironment.current.transfer)
    chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().filterNot { it.message.isDeleted }.forEach { entry ->
        entry.message.attachments.filter { it.transfer?.running == true }.forEach { attachment ->
            val state = checkNotNull(attachment.transfer)
            LaunchedEffect(entry.message.id, attachment.id, state.revision) {
                owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    // A bounded queue may deny a slot without changing revision. Keep waiting;
                    // lifecycle cancellation and revision-key changes stop this exact request.
                    while (true) {
                        delay(350)
                        action.value(entry.message.id, attachment.id, "advance", state.revision)
                    }
                }
            }
        }
    }
}

@Composable
internal fun photoQualityLabel(quality: PhotoQuality): String = stringResource(when (quality) {
    PhotoQuality.Low -> R.string.photo_quality_low; PhotoQuality.Standard -> R.string.photo_quality_standard
    PhotoQuality.High -> R.string.photo_quality_high; PhotoQuality.Original -> R.string.photo_quality_original
})

@Composable
internal fun PhotoQualityDialog(current: PhotoQuality, onDismiss: () -> Unit, onSelect: (PhotoQuality) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.photo_quality)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
            Text(stringResource(R.string.photo_quality_explanation))
            PhotoQuality.entries.forEach { value ->
                WhiteNoiseDialogChoiceRow(
                    title = photoQualityLabel(value),
                    selected = value == current,
                    onClick = { onSelect(value) },
                    modifier = Modifier.testTag("photo.quality.${value.name}"),
                )
            }
        } }, confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun AttachmentTransferControls(messageId: String, attachment: MessageAttachment) {
    val state = attachment.transfer ?: return
    val environment = LocalAttachmentEnvironment.current
    Column(Modifier.fillMaxWidth().testTag("attachment.transfer.${attachment.id}").semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(when (state.phase) {
            AttachmentTransferPhase.Idle -> R.string.download_waiting
            AttachmentTransferPhase.Queued -> R.string.attachment_queued
            AttachmentTransferPhase.Active -> if (state.direction == AttachmentTransferDirection.Upload) R.string.attachment_uploading else R.string.attachment_downloading
            AttachmentTransferPhase.Available -> R.string.attachment_available
            AttachmentTransferPhase.Cancelled -> R.string.attachment_cancelled
            AttachmentTransferPhase.Failed -> R.string.attachment_transfer_failed
            AttachmentTransferPhase.CacheMiss -> R.string.attachment_cache_miss
            AttachmentTransferPhase.Expired -> R.string.attachment_expired
            AttachmentTransferPhase.Invalid -> R.string.attachment_invalid
            AttachmentTransferPhase.Unavailable -> R.string.attachment_export_unavailable
        }), style = MaterialTheme.typography.bodySmall)
        if (state.phase == AttachmentTransferPhase.Idle || (state.phase == AttachmentTransferPhase.Queued && state.origin == AttachmentTransferOrigin.Automatic)) {
            TextButton({ environment.transfer(messageId, attachment.id, "start", state.revision) }) { Text(stringResource(R.string.download_now)) }
        }
        if (state.running) {
            LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
            TextButton({ environment.transfer(messageId, attachment.id, "cancel", state.revision) }) { Text(stringResource(R.string.cancel)) }
        } else if (state.retryable) TextButton({ environment.transfer(messageId, attachment.id, "retry", state.revision) }) { Text(stringResource(R.string.attachment_retry)) }
    }
}
