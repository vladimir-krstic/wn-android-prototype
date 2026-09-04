package dev.ipf.whitenoise.ui.conversation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import kotlinx.coroutines.delay

internal data class AttachmentEnvironment(
    val recentAccess: RecentMediaAccess = RecentMediaAccess.Full,
    val editorSession: PhotoEditorSession? = null,
    val openEditor: (String, Int) -> Boolean = { _, _ -> false },
    val editorEvent: (Long, PhotoEditorEvent) -> Boolean = { _, _ -> false },
    val replacePhotos: (List<MessageAttachment>, PhotoQuality, List<MessageAttachment>) -> Boolean = { _, _, _ -> false },
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
                    delay(350)
                    action.value(entry.message.id, attachment.id, "advance", state.revision)
                }
            }
        }
    }
}

internal class PickDeviceContactPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.data?.takeIf { resultCode == Activity.RESULT_OK && it.scheme == "content" }
}

/** Only the returned phone row is covered by the picker's grant. No email-table query. */
internal fun readDeviceContact(context: Context, uri: Uri): SharedDeviceContact? = runCatching {
    context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) null else SharedDeviceContact(
            name = cursor.getString(0)?.trim()?.takeIf(String::isNotEmpty),
            phone = cursor.getString(1)?.trim()?.takeIf(String::isNotEmpty),
        ).takeIf { it.fields.isNotEmpty() }
    }
}.getOrNull()

@Composable
internal fun DeviceContactPreview(contact: SharedDeviceContact, onDismiss: () -> Unit, onAdd: (SharedDeviceContact) -> Unit) {
    var name by rememberSaveable(contact) { mutableStateOf(contact.name != null) }
    var phone by rememberSaveable(contact) { mutableStateOf(contact.phone != null) }
    var email by rememberSaveable(contact) { mutableStateOf(contact.email != null) }
    val selected = contact.selected(name, phone, email)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.device_contact)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.contact_choose_fields))
            listOf(Triple(contact.name, name, { v: Boolean -> name = v }), Triple(contact.phone, phone, { v: Boolean -> phone = v }), Triple(contact.email, email, { v: Boolean -> email = v }))
                .forEach { (value, checked, change) -> if (value != null) Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = change).padding(vertical = 4.dp)) {
                    Checkbox(checked, null); Text(value, Modifier.weight(1f).padding(12.dp))
                } }
        } }, confirmButton = { TextButton({ onAdd(selected) }, enabled = selected.fields.isNotEmpty(), modifier = Modifier.testTag("contact.preview.add")) { Text(stringResource(R.string.attachment_add)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentMediaSheet(access: RecentMediaAccess, onDismiss: () -> Unit, onGallery: () -> Unit, onAdd: (ProfileAvatar) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        WhiteNoiseSheetHeader(stringResource(R.string.recent_media), onClose = onDismiss)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(when (access) {
                RecentMediaAccess.None -> R.string.recent_media_none
                RecentMediaAccess.SelectedOnly -> R.string.recent_media_selected
                RecentMediaAccess.Full -> R.string.recent_media_full
                RecentMediaAccess.Unavailable -> R.string.recent_media_unavailable
            }))
            val assets = when (access) {
                RecentMediaAccess.Full -> listOf(AvatarAsset.Marmot, AvatarAsset.Fox, AvatarAsset.Badger, AvatarAsset.Sloth)
                RecentMediaAccess.SelectedOnly -> listOf(AvatarAsset.Marmot)
                else -> emptyList()
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(assets) { asset ->
                val image = ProfileAvatar.Asset(asset)
                val description = stringResource(when (asset) {
                    AvatarAsset.Marmot -> R.string.recent_add_marmot
                    AvatarAsset.Fox -> R.string.recent_add_fox
                    AvatarAsset.Badger -> R.string.recent_add_badger
                    else -> R.string.recent_add_sloth
                })
                FilledTonalButton({ onAdd(image) }, modifier = Modifier.testTag("recent.media.${asset.name}").semantics { contentDescription = description }, contentPadding = PaddingValues(8.dp)) {
                    ComposerImage(image, Modifier.size(80.dp))
                    Text(stringResource(R.string.attachment_add), Modifier.padding(start = 8.dp))
                }
            } }
            FilledTonalButton(onGallery, modifier = Modifier.fillMaxWidth().testTag("recent.media.gallery")) { Text(stringResource(R.string.photos_and_videos)) }
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
                Row(Modifier.fillMaxWidth().testTag("photo.quality.${value.name}")
                    .selectable(value == current, role = Role.RadioButton, onClick = { onSelect(value) }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(value == current, null); Text(photoQualityLabel(value), Modifier.weight(1f).padding(start = 8.dp))
                }
            }
        } }, confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun AttachmentTransferControls(messageId: String, attachment: MessageAttachment) {
    val state = attachment.transfer ?: return
    val environment = LocalAttachmentEnvironment.current
    Column(Modifier.fillMaxWidth().testTag("attachment.transfer.${attachment.id}").semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(when (state.phase) {
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
        if (state.running) {
            LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
            TextButton({ environment.transfer(messageId, attachment.id, "cancel", state.revision) }) { Text(stringResource(R.string.cancel)) }
        } else if (state.retryable) TextButton({ environment.transfer(messageId, attachment.id, "retry", state.revision) }) { Text(stringResource(R.string.attachment_retry)) }
    }
}
