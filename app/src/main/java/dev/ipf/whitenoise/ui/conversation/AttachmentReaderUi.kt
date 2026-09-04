package dev.ipf.whitenoise.ui.conversation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class AttachmentAccessActions(val presented: Boolean = false, val open: ((String?, MessageAttachment) -> Unit)? = null)
internal val LocalAttachmentAccess = staticCompositionLocalOf { AttachmentAccessActions() }

/** The route/profile owns the request; only stable identifiers enter saved state. */
@Composable
internal fun AttachmentReaderScope(profile: Profile, chat: Chat, nextScenario: () -> AttachmentAccessScenario = { AttachmentAccessScenario.Success }, onPerson: (String) -> Unit = {}, content: @Composable () -> Unit) {
    var messageId by rememberSaveable(profile.id,chat.id) { mutableStateOf<String?>(null) }
    var attachmentId by rememberSaveable(profile.id,chat.id) { mutableStateOf<String?>(null) }
    var scenario by remember(profile.id,chat.id) { mutableStateOf(AttachmentAccessScenario.Success) }
    val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.message.id == messageId }?.message
    val current = message?.takeUnless { it.isDeleted || it.expiresAtMillis?.let { expiry -> expiry <= MessageForwarding.nowMillis } == true }
        ?.attachments?.firstOrNull { it.id == attachmentId }
    CompositionLocalProvider(LocalAttachmentAccess provides AttachmentAccessActions(messageId != null, { id, attachment ->
        val owner = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull {
            (id == null || it.message.id == id) && !it.message.isDeleted && it.message.attachments.any { item -> item.id == attachment.id }
        }?.message
        if (owner != null) { scenario = nextScenario(); messageId = owner.id; attachmentId = attachment.id }
    })) { key(profile.id, chat.id) { content() } }
    if (messageId != null && attachmentId != null) key(profile.id,chat.id,messageId,attachmentId) {
        val expected = remember { current }
        val valid = expected != null && current != null && expected.copy(transfer = null,isAvailable = true) == current.copy(transfer = null,isAvailable = true)
        AttachmentReaderDialog(profile, message, expected, current.takeIf { valid }, scenario,
            onDismiss = { messageId = null; attachmentId = null }, onPerson = onPerson)
    }
}

internal enum class ExternalFileResult { Opened, Unavailable, NoHandler, Failed, PackageBlocked }
internal suspend fun openExternalAttachment(context: Context, attachment: MessageAttachment, canDispatch: () -> Boolean): ExternalFileResult {
    val file = exportAttachment(context,attachment,AttachmentExportKey(attachment.id)) ?: return ExternalFileResult.Unavailable
    if (!canDispatch()) { file.file.delete(); return ExternalFileResult.Unavailable }
    // This prototype never dispatches package installation, including MIME refined by a provider.
    if (PackageAttachments.candidate(attachment.copy(mimeType = file.mimeType))) { file.file.delete(); return ExternalFileResult.PackageBlocked }
    return try {
        val uri = FileProvider.getUriForFile(context,"${context.packageName}.files",file.file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,file.mimeType)
            .apply { clipData = ClipData.newUri(context.contentResolver,file.label,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
        ExternalFileResult.Opened
    } catch (_: ActivityNotFoundException) { ExternalFileResult.NoHandler }
    catch (_: SecurityException) { ExternalFileResult.Failed }
    catch (_: IllegalArgumentException) { ExternalFileResult.Failed }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachmentReaderDialog(profile: Profile, message: ChatMessage?, expected: MessageAttachment?, current: MessageAttachment?, scenario: AttachmentAccessScenario, onDismiss: () -> Unit, onPerson: (String) -> Unit = {}) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val liveCurrent = rememberUpdatedState(current)
    val readAloud = rememberReadAloudController()
    ReadAloudModal(readAloud)
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<TextAttachmentResult?>(null) }
    var presentation by remember { mutableStateOf<TextAttachmentPresentation?>(null) }
    var external by remember { mutableStateOf<ExternalFileResult?>(null) }
    var externalBusy by remember { mutableStateOf(false) }
    var packageOutcome by remember { mutableStateOf<PackageOpenOutcome?>(null) }
    var save by rememberSaveable { mutableStateOf(false) }
    var filename by rememberSaveable { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    val selection = rememberSelectionState()
    val candidate = remember(expected) { expected?.let(TextAttachments::candidate) }
    val isPackage = expected?.let(PackageAttachments::candidate) == true
    val requestId = "attachment:${profile.id}:${message?.id}:${expected?.id}"
    DisposableEffect(readAloud, requestId) { onDispose { readAloud.stopAttachment(requestId) } }
    val currentAvailable = current?.bytesAvailable == true
    fun copy(text: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("File",text)); copied = true
    }
    fun openExternal() {
        val attachment = current ?: return
        if (externalBusy) return
        scope.launch {
            externalBusy = true
            try {
                external = if (scenario == AttachmentAccessScenario.NoHandler && attempt == 0) ExternalFileResult.NoHandler else
                    openExternalAttachment(context,attachment) { liveCurrent.value == attachment && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
                if (candidate == null && external == ExternalFileResult.Opened) onDismiss()
            } finally { externalBusy = false }
        }
    }
    fun retry() { attempt++; readAloud.stop(); external = null; copied = false }
    LaunchedEffect(currentAvailable, current == null, attempt) {
        result = null; presentation = null; packageOutcome = null; selection.clear(); readAloud.stop()
        val attachment = current
        if (attachment == null) { save = false; result = TextAttachmentResult.Failed(TextAttachmentFailure.SourceChanged); return@LaunchedEffect }
        delay(150)
        if (!attachment.bytesAvailable || (scenario == AttachmentAccessScenario.LoadFailure && attempt == 0)) {
            result = TextAttachmentResult.Failed(TextAttachmentFailure.Unavailable); return@LaunchedEffect
        }
        if (isPackage) {
            val file = exportAttachment(context,attachment,AttachmentExportKey(attachment.id))
            if (file == null) result = TextAttachmentResult.Failed(TextAttachmentFailure.Unavailable)
            else {
                val valid = withContext(Dispatchers.IO) { AttachmentSources.validPackage(file.file) }
                file.file.delete()
                packageOutcome = PackageAttachments.outcome(attachment,valid,
                    installationEnabled = scenario in setOf(AttachmentAccessScenario.PackagePermission,AttachmentAccessScenario.PackageNoInstaller,AttachmentAccessScenario.PackageReady),
                    permission = scenario != AttachmentAccessScenario.PackagePermission, installer = scenario == AttachmentAccessScenario.PackageReady)
            }
        } else if (candidate != null) {
            val loaded = AttachmentSources.readText(context,attachment)
            if (loaded is TextAttachmentResult.Ready) presentation = withContext(Dispatchers.Default) { TextAttachments.presentation(loaded.text,candidate.format) }
            result = loaded
        } else openExternal()
    }
    val failure = (result as? TextAttachmentResult.Failed)?.reason
    val loading = result == null && presentation == null && packageOutcome == null && external == null
    val name = TextAttachments.safeName(expected?.label.orEmpty())
    fun back() { if (selection.selectedTexts.isNotEmpty()) selection.clear() else onDismiss() }
    if (candidate != null && !isPackage) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false)) {
            BackHandler(enabled = !filename && !(save && current != null && message != null), onBack = ::back)
            Scaffold(Modifier.fillMaxSize().testTag("attachment.reader"), contentWindowInsets = WindowInsets.safeDrawing,
                topBar = { TopAppBar(title = { Text(stringResource(R.string.attachment_reader_title)) }, navigationIcon = {
                    IconButton(::back) { Icon(painterResource(R.drawable.ic_arrow_back),stringResource(R.string.back)) }
                }, actions = { Box {
                    IconButton({ more = true }, Modifier.testTag("attachment.reader.more")) { Icon(painterResource(R.drawable.ic_more_vert),stringResource(R.string.more_options)) }
                    WhiteNoiseDropdownMenu(more,{ more = false },listOf(
                        WhiteNoiseMenuItem(stringResource(R.string.attachment_open_external),{ more = false; openExternal() },enabled = !externalBusy && currentAvailable,modifier = Modifier.testTag("attachment.reader.external")),
                        WhiteNoiseMenuItem(stringResource(R.string.attachment_save_file),{ more = false; save = true },enabled = currentAvailable),
                    ))
                } }) }, bottomBar = {
                    ReadAloudReaderBar {
                        ReadAloudTransport(readAloud)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                            val ready = result as? TextAttachmentResult.Ready
                            TextButton({ copy(selection.selectedTexts.joinToString("\n") { it.text }.ifEmpty { ready?.text.orEmpty() }) }, enabled = ready?.text?.isNotEmpty() == true,
                                modifier = Modifier.testTag("attachment.reader.copy")) { Text(stringResource(R.string.attachment_copy_text)) }
                            TextButton({
                                val selected = selection.selectedTexts.joinToString("\n") { it.text }
                                readAloud.toggle(requestId,selected.ifBlank { presentation?.speech.orEmpty() })
                            }, enabled = readAloud.ready && presentation?.speech?.isNotBlank() == true, modifier = Modifier.testTag("attachment.reader.speak")) {
                                Text(stringResource(if (readAloud.activeMessageId == requestId) R.string.stop_reading else R.string.read_aloud))
                            }
                        }
                    }
                }) { padding -> AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin)) {
                            Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("attachment.reader.filename"))
                            val bytes = (result as? TextAttachmentResult.Ready)?.byteCount ?: expected?.fileSizeBytes
                            Text(candidate.mime + (bytes?.let { " · " + android.text.format.Formatter.formatShortFileSize(context,it.toLong()) } ?: ""), style = MaterialTheme.typography.labelMedium)
                            TextButton({ filename = true }) { Text(stringResource(R.string.attachment_full_filename)) }
                            if (externalBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (copied) Text(stringResource(R.string.copied), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            if (external != null && external != ExternalFileResult.Opened) Text(stringResource(externalFailureLabel(external!!)),color = MaterialTheme.colorScheme.error)
                            if (readAloud.failed || (readAloud.initializationComplete && !readAloud.ready)) {
                                Text(stringResource(R.string.attachment_speech_unavailable),style = MaterialTheme.typography.bodySmall)
                                TextButton({ readAloud.initialize(context) }, modifier = Modifier.testTag("attachment.reader.speech.retry")) { Text(stringResource(R.string.attachment_retry)) }
                            }
                        }
                        HorizontalDivider()
                        if (loading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text(stringResource(R.string.attachment_reader_loading)) } }
                        else if (failure != null) Column(Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin).semantics { liveRegion = LiveRegionMode.Polite }) {
                            Text(stringResource(textFailureLabel(failure)),color = MaterialTheme.colorScheme.error)
                            if (failure == TextAttachmentFailure.Unavailable) TextButton(::retry, modifier = Modifier.testTag("attachment.reader.retry")) { Text(stringResource(R.string.attachment_retry)) }
                        } else presentation?.let { shown ->
                            SelectionContainer(state = selection, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin)) {
                                    if (shown.truncated) Text(stringResource(R.string.attachment_reader_truncated),style = MaterialTheme.typography.bodyMedium)
                                    if (shown.source.isEmpty()) Text(stringResource(R.string.attachment_reader_empty))
                                    else if (shown.document != null) MessageDocumentContent(shown.document,profile.people,{ onDismiss(); onPerson(it) },Modifier.fillMaxWidth(),annotateSource = true)
                                    else Text(shown.preview,modifier = Modifier.testTag("attachment.reader.body"))
                                }
                            }
                        }
                    }
                } }
        }
    } else {
        AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("attachment.file.open"), title = { Text(name) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                if (loading || externalBusy) { CircularProgressIndicator(); Text(stringResource(R.string.attachment_reader_loading)) }
                failure?.let { Text(stringResource(textFailureLabel(it)),color = MaterialTheme.colorScheme.error) }
                packageOutcome?.let { Text(stringResource(packageOutcomeLabel(it))) }
                external?.takeUnless { it == ExternalFileResult.Opened }?.let { Text(stringResource(externalFailureLabel(it))) }
                if (failure == TextAttachmentFailure.Unavailable || external in setOf(ExternalFileResult.Unavailable,ExternalFileResult.Failed,ExternalFileResult.NoHandler)) TextButton(::retry, modifier = Modifier.testTag("attachment.reader.retry")) { Text(stringResource(R.string.attachment_retry)) }
            } }, confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.close)) } },
            dismissButton = { if (currentAvailable) TextButton({ save = true }) { Text(stringResource(R.string.attachment_save_file)) } })
    }
    if (filename) AlertDialog(onDismissRequest = { filename = false },title = { Text(stringResource(R.string.attachment_filename)) },
        text = { SelectionContainer { Text(name,Modifier.verticalScroll(rememberScrollState()).testTag("attachment.reader.full.filename")) } },
        confirmButton = { TextButton({ copy(name); filename = false }) { Text(stringResource(R.string.copy)) } },
        dismissButton = { TextButton({ filename = false }) { Text(stringResource(R.string.close)) } })
    if (save && current != null && message != null) MessageAttachmentExportSheet(message.copy(attachments = listOf(current)),false,onDismiss = { save = false })
}
private fun textFailureLabel(reason: TextAttachmentFailure) = when (reason) {
    TextAttachmentFailure.Unavailable -> R.string.attachment_reader_unavailable
    TextAttachmentFailure.TooLarge -> R.string.attachment_reader_too_large
    TextAttachmentFailure.InvalidEncoding -> R.string.attachment_reader_encoding
    TextAttachmentFailure.Binary -> R.string.attachment_reader_binary
    TextAttachmentFailure.SourceChanged -> R.string.attachment_reader_changed
}
private fun externalFailureLabel(result: ExternalFileResult) = when (result) {
    ExternalFileResult.NoHandler -> R.string.attachment_no_viewer
    ExternalFileResult.PackageBlocked -> R.string.attachment_package_restricted
    ExternalFileResult.Unavailable -> R.string.attachment_reader_unavailable
    else -> R.string.attachment_open_failed
}
private fun packageOutcomeLabel(outcome: PackageOpenOutcome) = when (outcome) {
    PackageOpenOutcome.InvalidPackage -> R.string.attachment_package_invalid
    PackageOpenOutcome.RestrictedDistribution -> R.string.attachment_package_restricted
    PackageOpenOutcome.PermissionRequired -> R.string.attachment_package_permission
    PackageOpenOutcome.NoInstaller -> R.string.attachment_package_no_installer
    else -> R.string.attachment_package_ready
}
