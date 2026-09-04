package dev.ipf.whitenoise.ui.conversation

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.coroutines.coroutineContext

internal data class ExportedAttachment(val key: AttachmentExportKey, val label: String, val mimeType: String, val file: File)
internal data class SaveAttachmentRequest(val name: String, val mimeType: String)
internal class CreateAttachmentDocument : ActivityResultContract<SaveAttachmentRequest, Uri?>() {
    override fun createIntent(context: Context, input: SaveAttachmentRequest) = Intent(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE).setType(input.mimeType).putExtra(Intent.EXTRA_TITLE, input.name)
    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

/** Materialize actual local bytes; unknown file labels never become unrelated sample PDFs. */
internal suspend fun exportAttachment(context: Context, attachment: MessageAttachment, key: AttachmentExportKey): ExportedAttachment? = withContext(Dispatchers.IO) {
    if (!attachment.bytesAvailable) return@withContext null
    var file: File? = null
    try {
        var mime = attachment.mimeType ?: "application/octet-stream"
        val input: InputStream = when {
            attachment.deviceContact != null -> {
                mime = "text/vcard"; attachment.deviceContact.vCard().byteInputStream()
            }
            attachment.kind == MessageAttachmentKind.Gif -> {
                mime = "image/gif"
                (attachment.images.firstOrNull() as? ProfileAvatar.DeviceImage)?.bytes?.inputStream()
                    ?: context.resources.openRawResource(R.raw.chat_animation)
            }
            attachment.kind == MessageAttachmentKind.Video -> {
                mime = "video/mp4"
                if (attachment.externalUri != null) {
                    val uri = attachment.externalUri.toUri()
                    if (uri.scheme != "content") return@withContext null
                    mime = context.contentResolver.getType(uri) ?: mime
                    context.contentResolver.openInputStream(uri) ?: return@withContext null
                } else context.resources.openRawResource(R.raw.chat_trail_clip)
            }
            attachment.kind in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos) -> {
                when (val image = attachment.images.getOrNull(key.imageIndex)) {
                    is ProfileAvatar.DeviceImage -> {
                        mime = attachment.mimeType ?: if (image.bytes.firstOrNull() == 137.toByte()) "image/png" else "image/jpeg"
                        image.bytes.inputStream()
                    }
                    is ProfileAvatar.Asset -> { mime = "image/png"; DraftPhotoProcessor.bundledSource(context, image.asset)?.inputStream() ?: return@withContext null }
                    is ProfileAvatar.WebImage -> { mime = "image/png"; DraftPhotoProcessor.bundledSource(context, image.asset)?.inputStream() ?: return@withContext null }
                    else -> return@withContext null
                }
            }
            attachment.externalUri != null -> {
                val uri = attachment.externalUri.toUri()
                if (uri.scheme != "content") return@withContext null
                mime = context.contentResolver.getType(uri) ?: mime
                context.contentResolver.openInputStream(uri) ?: return@withContext null
            }
            attachment.kind == MessageAttachmentKind.File -> {
                val resource = when (attachment.label.lowercase()) {
                    "project brief.pdf" -> R.raw.project_brief
                    "project notes.pdf" -> R.raw.project_notes
                    "trail plan.pdf" -> R.raw.trail_plan
                    "weekend notes.pdf" -> R.raw.weekend_notes
                    else -> return@withContext null
                }
                mime = "application/pdf"; context.resources.openRawResource(resource)
            }
            else -> return@withContext null
        }
        val actualInput = input.buffered()
        if (mime.startsWith("image/")) {
            actualInput.mark(16)
            val signature = ByteArray(12)
            val count = actualInput.read(signature)
            actualInput.reset()
            mime = when {
                count >= 8 && signature[0] == 137.toByte() -> "image/png"
                count >= 3 && signature[0] == 0xff.toByte() -> "image/jpeg"
                count >= 6 && String(signature, 0, 3, Charsets.US_ASCII) == "GIF" -> "image/gif"
                count >= 12 && String(signature, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
                else -> { actualInput.close(); return@withContext null }
            }
        }
        val extension = when (mime) { "text/vcard" -> "vcf"; "image/png" -> "png"; "image/jpeg" -> "jpg"; "image/gif" -> "gif"; "image/webp" -> "webp"; "video/mp4" -> "mp4"; "application/pdf" -> "pdf"; else -> attachment.label.substringAfterLast('.', "bin").take(8).filter(Char::isLetterOrDigit).ifEmpty { "bin" } }
        val stem = attachment.label.substringBeforeLast('.', attachment.label).replace(Regex("[^A-Za-z0-9 _-]"), "_").take(64).ifBlank { "attachment" }
        val name = if (attachment.images.size > 1) "$stem-${key.imageIndex + 1}.$extension" else "$stem.$extension"
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val requestDirectory = File(directory, "attachment-${java.util.UUID.randomUUID()}")
        check(requestDirectory.mkdir())
        file = File(requestDirectory, name)
        actualInput.use { source -> file.outputStream().use { target ->
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                coroutineContext.ensureActive()
                val read = source.read(buffer)
                if (read < 0) break
                total += read
                if (total > 128L * 1024 * 1024) error("Attachment exceeds local export limit")
                target.write(buffer, 0, read)
            }
            check(total > 0 || attachment.kind == MessageAttachmentKind.File)
        } }
        ExportedAttachment(key, name, mime, file)
    } catch (cancelled: CancellationException) { file?.parentFile?.deleteRecursively(); throw cancelled }
    catch (_: Exception) { file?.parentFile?.deleteRecursively(); null }
}

internal fun shareAttachments(context: Context, text: String, items: List<ExportedAttachment>): AttachmentExportOutcome = try {
    val uris = ArrayList(items.map { FileProvider.getUriForFile(context, "${context.packageName}.files", it.file) })
    val send = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
        type = if (items.isEmpty()) "text/plain" else AttachmentExports.mimeType(items.map { it.mimeType })
        if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
        else if (uris.isNotEmpty()) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        if (uris.isNotEmpty()) {
            clipData = ClipData.newUri(context.contentResolver, items.first().label, uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    if (context.packageManager.queryIntentActivities(send, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) AttachmentExportOutcome.NoHandler
    else {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.attachment_share)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        AttachmentExportOutcome.HandedOff
    }
} catch (_: Exception) { AttachmentExportOutcome.NoHandler }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageAttachmentExportSheet(message: ChatMessage, sharing: Boolean, people: List<Person> = emptyList(), onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keys = remember(message.id) { AttachmentExports.keys(message) }
    var prepared by remember(message.id) { mutableStateOf(emptyList<ExportedAttachment>()) }
    var outcomes by rememberSaveable(message.id, stateSaver = listSaver<Map<AttachmentExportKey, AttachmentExportOutcome>, String>(
        save = { it.flatMap { (key, value) -> listOf(key.attachmentId, key.imageIndex.toString(), value.name) } },
        restore = { it.chunked(3).associate { row -> AttachmentExportKey(row[0], row[1].toInt()) to AttachmentExportOutcome.valueOf(row[2]) } },
    )) { mutableStateOf(emptyMap<AttachmentExportKey, AttachmentExportOutcome>()) }
    var busy by remember { mutableStateOf(false) }
    var shareOutcome by rememberSaveable(message.id) { mutableStateOf<AttachmentExportOutcome?>(null) }
    var confirmPartial by remember { mutableStateOf(false) }
    val keySaver = listSaver<List<AttachmentExportKey>, String>(
        save = { it.flatMap { key -> listOf(key.attachmentId, key.imageIndex.toString()) } },
        restore = { it.chunked(2).map { row -> AttachmentExportKey(row[0], row[1].toInt()) } },
    )
    var saveQueue by rememberSaveable(message.id, stateSaver = keySaver) { mutableStateOf(emptyList<AttachmentExportKey>()) }
    var pendingSaveKeys by rememberSaveable(message.id, stateSaver = keySaver) { mutableStateOf(emptyList<AttachmentExportKey>()) }
    val pendingSave = pendingSaveKeys.singleOrNull()
    val latestMessage = rememberUpdatedState(message)

    suspend fun prepare() {
        busy = true
        try {
            keys.filter { key -> prepared.none { it.key == key } }.forEach { key ->
                val attachment = latestMessage.value.attachments.firstOrNull { it.id == key.attachmentId }
                val item = attachment?.let { exportAttachment(context, it, key) }
                if (item == null) outcomes = outcomes + (key to AttachmentExportOutcome.Unavailable)
                else { prepared = prepared + item; if (outcomes[key] != AttachmentExportOutcome.Saved) outcomes = outcomes + (key to AttachmentExportOutcome.Ready) }
            }
        } finally { busy = false }
    }
    LaunchedEffect(message.id) { prepare() }
    val save = rememberLauncherForActivityResult(CreateAttachmentDocument()) { uri ->
        val key = pendingSave
        pendingSaveKeys = emptyList()
        if (key != null) {
            if (uri == null) {
                outcomes = outcomes + (key to AttachmentExportOutcome.Cancelled)
                saveQueue = emptyList()
            } else scope.launch {
                busy = true
                val item = prepared.firstOrNull { it.key == key } ?: latestMessage.value.attachments.firstOrNull { it.id == key.attachmentId }?.let { exportAttachment(context, it, key) }
                val result = withContext(Dispatchers.IO) {
                    runCatching { checkNotNull(item); context.contentResolver.openOutputStream(uri)?.use { target -> item.file.inputStream().use { it.copyTo(target) } } ?: error("No destination") }.isSuccess
                }
                outcomes = outcomes + (key to if (result) AttachmentExportOutcome.Saved else AttachmentExportOutcome.Failed)
                busy = false
            }
        }
    }
    LaunchedEffect(saveQueue, pendingSave, busy) {
        if (!busy && pendingSave == null && saveQueue.isNotEmpty()) {
            val key = saveQueue.first()
            saveQueue = saveQueue.drop(1)
            val item = prepared.firstOrNull { it.key == key }
            if (item != null) {
                pendingSaveKeys = listOf(key)
                if (runCatching { save.launch(SaveAttachmentRequest(item.label, item.mimeType)) }.isFailure) {
                    pendingSaveKeys = emptyList(); outcomes = outcomes + (key to AttachmentExportOutcome.NoHandler); saveQueue = emptyList()
                }
            }
        }
    }
    val sharedText = (listOf(MessageDocuments.plainText(message.text)) + message.attachments.mapNotNull { attachment ->
        attachment.contactPersonId?.let { id -> people.firstOrNull { it.id == id } }?.let { "${it.displayName}\n${it.publicKey}" }
    }).filter(String::isNotBlank).joinToString("\n\n")
    fun share() { shareOutcome = shareAttachments(context, sharedText, prepared) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        WhiteNoiseSheetHeader(stringResource(if (sharing) R.string.attachment_share else R.string.save_attachments), onClose = onDismiss)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sharedText.isNotBlank()) Text(sharedText, maxLines = 4)
            keys.forEach { key ->
                val attachment = message.attachments.firstOrNull { it.id == key.attachmentId }
                ListItem(headlineContent = { Text(prepared.firstOrNull { it.key == key }?.label ?: attachment?.label.orEmpty()) },
                    supportingContent = { Text(exportOutcomeLabel(outcomes[key])) }, modifier = Modifier.testTag("attachment.export.${key.attachmentId}.${key.imageIndex}"))
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            shareOutcome?.let { Text(exportOutcomeLabel(it), Modifier.testTag("attachment.export.result")) }
            if (outcomes.values.any { it == AttachmentExportOutcome.Unavailable }) TextButton({ scope.launch { prepare() } }, enabled = !busy) { Text(stringResource(R.string.attachment_retry)) }
            if (sharing) FilledTonalButton({ if (prepared.size < keys.size) confirmPartial = true else share() },
                enabled = !busy && (prepared.isNotEmpty() || sharedText.isNotBlank()), modifier = Modifier.fillMaxWidth().testTag("attachment.export.share")) { Text(stringResource(R.string.attachment_share)) }
            else FilledTonalButton({ saveQueue = prepared.map { it.key }.filter { outcomes[it] != AttachmentExportOutcome.Saved } },
                enabled = !busy && pendingSave == null && saveQueue.isEmpty() && prepared.any { outcomes[it.key] != AttachmentExportOutcome.Saved },
                modifier = Modifier.fillMaxWidth().testTag("attachment.export.save")) { Text(stringResource(R.string.save_attachments)) }
        }
    }
    if (confirmPartial) AlertDialog(onDismissRequest = { confirmPartial = false }, title = { Text(stringResource(R.string.attachment_share)) },
        text = { Text(stringResource(R.string.attachment_share_partial)) },
        confirmButton = { TextButton({ confirmPartial = false; share() }) { Text(stringResource(R.string.attachment_share_available)) } },
        dismissButton = { TextButton({ confirmPartial = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun exportOutcomeLabel(outcome: AttachmentExportOutcome?): String = stringResource(when (outcome) {
    AttachmentExportOutcome.Ready -> R.string.attachment_export_ready
    AttachmentExportOutcome.Saved -> R.string.attachment_export_saved
    AttachmentExportOutcome.HandedOff -> R.string.attachment_export_handed_off
    AttachmentExportOutcome.Unavailable -> R.string.attachment_export_unavailable
    AttachmentExportOutcome.Cancelled -> R.string.attachment_export_cancelled
    AttachmentExportOutcome.Failed -> R.string.attachment_export_failed
    AttachmentExportOutcome.NoHandler -> R.string.attachment_export_no_handler
    null -> R.string.attachment_export_pending
})
