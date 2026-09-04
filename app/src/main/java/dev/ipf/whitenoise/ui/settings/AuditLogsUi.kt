package dev.ipf.whitenoise.ui.settings

import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.AuditLogController
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.*

internal val LocalAuditLogs = staticCompositionLocalOf<AuditLogController?> { null }

/** Keep the result lease above the lock boundary so stale document results can be cleaned up. */
@Composable
internal fun AuditLogHost(controller: AuditLogController, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var launched by rememberSaveable { mutableStateOf<Long?>(null) }
    var launchedAttempt by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val id = launched; val attempt = launchedAttempt; launched = null
        if (uri == null) { if (id != null) controller.cancel(id) }
        else {
            val bytes = id?.let { controller.takeForWriting(it,attempt) }
            val fail = controller.work?.let { it.scenario == AuditLogScenario.WriteFails && it.attempt == 0 } == true
            scope.launch {
                var accepted = false
                try {
                    val saved = withContext(Dispatchers.IO) {
                        bytes != null && !fail && runCatching {
                            checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use { it.write(bytes) }
                        }.isSuccess
                    }
                    accepted = id != null && controller.written(id,attempt,saved)
                } finally {
                    bytes?.fill(0)
                    if (!accepted) withContext(NonCancellable + Dispatchers.IO) { runCatching { DocumentsContract.deleteDocument(context.contentResolver,uri) } }
                }
            }
        }
    }
    SideEffect { controller.reconcile() }
    DisposableEffect(controller) { onDispose { controller.interruptWriting() } }
    val work = controller.work
    LaunchedEffect(work?.id,work?.phase,work?.attempt,launched,lifecycle) {
        if (work != null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            when (work.phase) {
                AuditLogPhase.Applying -> { delay(350); controller.advance(work.id,work.attempt) }
                AuditLogPhase.ChoosingDestination -> if (launched == null) {
                    launched = work.id; launchedAttempt = work.attempt
                    try { launcher.launch("white-noise-audit-logs.zip") }
                    catch (_: Exception) { launched = null; controller.destinationFailed(work.id,work.attempt) }
                }
                else -> Unit
            }
        }
    }
    CompositionLocalProvider(LocalAuditLogs provides controller) { content() }
}

@Composable
internal fun AuditLogsScreen(controller: AuditLogController, onBack: () -> Unit) {
    val state = controller.state
    val work = controller.work
    val context = LocalContext.current
    val busy = work?.busy == true
    SettingsScaffold(title = stringResource(R.string.audit_logs_title),onBack = onBack) {
        SettingsList {
            item { SettingsExplainer(stringResource(R.string.audit_logs_detail)) }
            item { SettingsGroup {
                SettingsSwitch(stringResource(R.string.audit_logs_record),state.enabled,{ controller.begin(if (it) AuditLogAction.Enable else AuditLogAction.Disable) },enabled = !busy,
                    subtitle = stringResource(R.string.audit_logs_record_detail))
            } }
            item { SettingsSection(stringResource(R.string.audit_logs_files)) }
            if (state.files.isEmpty()) item { SettingsExplainer(stringResource(R.string.audit_logs_empty)) }
            else item { SettingsGroup { state.files.forEachIndexed { index,file ->
                if (index > 0) SettingsDivider()
                SettingsExplainer("${file.name} · ${Formatter.formatShortFileSize(context,file.bytes.toLong())}")
            } } }
            item { SettingsGroup {
                SettingsAction(stringResource(R.string.audit_logs_export),onClick = { controller.begin(AuditLogAction.Export) },enabled = !busy)
                SettingsDivider()
                SettingsAction(stringResource(R.string.audit_logs_delete),onClick = { controller.begin(AuditLogAction.Delete) },enabled = !busy,destructive = true)
            } }
            if (work != null && work.phase != AuditLogPhase.Consent) item {
                Column(Modifier.testTag("audit.status"),verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(auditStatus(work,state.enabled)),modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = if (work.phase == AuditLogPhase.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    if (work.phase == AuditLogPhase.Failed && work.failure !in setOf(AuditLogFailure.Empty,AuditLogFailure.SourceChanged))
                        TextButton(onClick = { controller.retry(work.id) },modifier = Modifier.testTag("audit.retry")) { Text(stringResource(R.string.notification_action_retry)) }
                    TextButton(onClick = { controller.cancel(work.id) }) { Text(stringResource(if (busy) R.string.cancel else R.string.done)) }
                }
            }
        }
    }
    if (work?.phase == AuditLogPhase.Consent) WhiteNoiseAlertDialog(onDismissRequest = { controller.cancel(work.id) },
        title = { Text(stringResource(when(work.action) { AuditLogAction.Enable -> R.string.audit_logs_record_consent; AuditLogAction.Delete -> R.string.audit_logs_delete; else -> R.string.audit_logs_export })) },
        text = { Text(stringResource(when(work.action) { AuditLogAction.Enable -> R.string.audit_logs_record_warning; AuditLogAction.Delete -> R.string.audit_logs_delete_warning; else -> R.string.audit_logs_export_warning }),
            modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { controller.confirm(work.id) },modifier = Modifier.testTag("audit.confirm")) { Text(stringResource(when(work.action) { AuditLogAction.Enable -> R.string.audit_logs_start; AuditLogAction.Delete -> R.string.audit_logs_delete; else -> R.string.audit_logs_export })) } },
        dismissButton = { TextButton(onClick = { controller.cancel(work.id) }) { Text(stringResource(R.string.cancel)) } })
}

private fun auditStatus(work: AuditLogWork, enabled: Boolean): Int = when (work.phase) {
    AuditLogPhase.Consent -> R.string.audit_logs_export_warning
    AuditLogPhase.Applying -> R.string.audit_logs_working
    AuditLogPhase.ChoosingDestination -> R.string.audit_logs_destination
    AuditLogPhase.Writing -> R.string.audit_logs_writing
    AuditLogPhase.Complete -> when(work.action) {
        AuditLogAction.Enable -> R.string.audit_logs_on
        AuditLogAction.Disable -> R.string.audit_logs_off
        AuditLogAction.Export -> R.string.audit_logs_saved
        AuditLogAction.Delete -> if (enabled) R.string.audit_logs_deleted_recording else R.string.audit_logs_deleted
    }
    AuditLogPhase.Failed -> when(work.failure) {
        AuditLogFailure.Empty -> R.string.audit_logs_empty
        AuditLogFailure.SourceChanged -> R.string.audit_logs_changed
        AuditLogFailure.PartialDelete -> if (work.removed.isEmpty()) R.string.audit_logs_delete_failed else R.string.audit_logs_partial_delete
        AuditLogFailure.Delete -> R.string.audit_logs_delete_failed
        AuditLogFailure.Update -> R.string.audit_logs_update_failed
        AuditLogFailure.Preparation -> R.string.audit_logs_prepare_failed
        AuditLogFailure.Destination -> R.string.audit_logs_destination_failed
        else -> R.string.audit_logs_write_failed
    }
}
