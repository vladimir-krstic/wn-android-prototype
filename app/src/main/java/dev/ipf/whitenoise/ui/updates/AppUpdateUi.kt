package dev.ipf.whitenoise.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AppSelfUpdateFailure
import dev.ipf.whitenoise.model.AppSelfUpdatePhase
import dev.ipf.whitenoise.model.AppUpdateCheckPhase
import dev.ipf.whitenoise.model.AppUpdateState
import dev.ipf.whitenoise.model.AppUpdates
import dev.ipf.whitenoise.state.AppUpdateController
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.settings.SettingsGroup
import dev.ipf.whitenoise.ui.settings.SettingsLink
import dev.ipf.whitenoise.ui.settings.SettingsSection
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

@Composable
fun AppUpdateBanner(
    state: AppUpdateState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!AppUpdates.showsBanner(state)) return
    val latest = state.check.latestVersion ?: return
    val important = AppUpdates.isImportant(state)
    val accessibilityState = stringResource(
        if (important) R.string.app_update_important_title else R.string.app_update_available_title,
    )
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related)
            .testTag("appUpdate.banner")
            .semantics {
                stateDescription = accessibilityState
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.FormField),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                Text(
                    stringResource(if (important) R.string.app_update_important_title else R.string.app_update_available_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.app_update_available_detail, latest),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.check.releasesBehind?.takeIf { it > 0 }?.let { count ->
                    Text(
                        pluralStringResource(R.plurals.app_update_releases_behind, count, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onUpdate, modifier = Modifier.testTag("appUpdate.banner.update")) {
                    Text(stringResource(R.string.app_update_now))
                }
            }
            if (AppUpdates.canDismissBanner(state)) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("appUpdate.banner.dismiss")) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.app_update_dismiss))
                }
            }
        }
    }
}

@Composable
fun AppUpdateSettingsGroup(
    state: AppUpdateState,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!AppUpdates.showsSettings(state)) return
    val subtitle = when (state.check.phase) {
        AppUpdateCheckPhase.Unknown -> stringResource(R.string.app_update_settings_unknown, state.installedVersion)
        AppUpdateCheckPhase.Checking -> stringResource(R.string.app_update_settings_checking)
        AppUpdateCheckPhase.Current -> stringResource(R.string.app_update_settings_current, state.installedVersion)
        AppUpdateCheckPhase.Available -> {
            val latest = state.check.latestVersion.orEmpty()
            state.check.releasesBehind?.let {
                pluralStringResource(
                    R.plurals.app_update_settings_available_count,
                    it,
                    state.installedVersion,
                    latest,
                    it,
                )
            } ?: stringResource(R.string.app_update_settings_available, state.installedVersion, latest)
        }
        AppUpdateCheckPhase.Failed -> stringResource(R.string.app_update_settings_failed)
    }
    Column(modifier) {
        SettingsSection(stringResource(R.string.app_updates_title))
        SettingsGroup {
            SettingsLink(
                title = stringResource(R.string.app_update_settings_title),
                subtitle = subtitle,
                onClick = onAction,
                enabled = state.check.phase != AppUpdateCheckPhase.Checking,
                leading = {
                    Icon(
                        painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
fun AppUpdateHost(controller: AppUpdateController) {
    val state = controller.state
    val check = state.check
    val flow = state.selfUpdate
    LaunchedEffect(check.phase, check.generation) {
        if (check.phase == AppUpdateCheckPhase.Checking) {
            delay(450)
            controller.completeCheck(check.generation)
        }
    }
    LaunchedEffect(flow.phase, flow.generation, flow.bytesRead) {
        if (flow.phase in setOf(
                AppSelfUpdatePhase.Resolving,
                AppSelfUpdatePhase.Downloading,
                AppSelfUpdatePhase.Verifying,
            )
        ) {
            delay(550)
            controller.advanceSelfUpdate(flow.generation)
        }
    }
    when (flow.phase) {
        AppSelfUpdatePhase.Idle -> Unit
        AppSelfUpdatePhase.Resolving -> UpdateProgressDialog(
            title = stringResource(R.string.app_update_resolving_title),
            body = stringResource(R.string.app_update_resolving_detail),
            state = stringResource(R.string.app_update_state_resolving),
            onCancel = controller::cancel,
        )
        AppSelfUpdatePhase.Confirming -> WhiteNoiseAlertDialog(
            onDismissRequest = controller::cancel,
            title = { Text(stringResource(R.string.app_update_download_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.app_update_download_detail,
                        flow.version.orEmpty(),
                        formatBytes(flow.sizeBytes),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = controller::confirmDownload) { Text(stringResource(R.string.app_update_download)) }
            },
            dismissButton = {
                TextButton(onClick = controller::cancel) { Text(stringResource(R.string.cancel)) }
            },
        )
        AppSelfUpdatePhase.Downloading -> UpdateProgressDialog(
            title = stringResource(R.string.app_update_downloading_title),
            body = stringResource(
                R.string.app_update_downloading_detail,
                formatBytes(flow.bytesRead),
                formatBytes(flow.sizeBytes),
            ),
            state = stringResource(R.string.app_update_state_downloading),
            progress = flow.sizeBytes?.takeIf { it > 0 }?.let { flow.bytesRead.toFloat() / it.toFloat() },
            onCancel = controller::cancel,
            cancelLabel = stringResource(R.string.app_update_cancel_download),
        )
        AppSelfUpdatePhase.Verifying -> UpdateProgressDialog(
            title = stringResource(R.string.app_update_verifying_title),
            body = stringResource(R.string.app_update_verifying_detail),
            state = stringResource(R.string.app_update_state_verifying),
            onCancel = controller::cancel,
        )
        AppSelfUpdatePhase.Ready -> WhiteNoiseAlertDialog(
            onDismissRequest = controller::cancel,
            title = { Text(stringResource(R.string.app_update_ready_title)) },
            text = { Text(stringResource(R.string.app_update_ready_detail, flow.version.orEmpty())) },
            confirmButton = {
                TextButton(onClick = controller::requestInstall) { Text(stringResource(R.string.app_update_install)) }
            },
            dismissButton = {
                TextButton(onClick = controller::cancel) { Text(stringResource(R.string.cancel)) }
            },
        )
        AppSelfUpdatePhase.PermissionRequired -> WhiteNoiseAlertDialog(
            onDismissRequest = controller::cancel,
            title = { Text(stringResource(R.string.app_update_permission_title)) },
            text = { Text(stringResource(R.string.app_update_permission_detail)) },
            confirmButton = {
                TextButton(onClick = controller::reviewInstallPermission) { Text(stringResource(R.string.app_update_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = controller::cancel) { Text(stringResource(R.string.cancel)) }
            },
        )
        AppSelfUpdatePhase.Failed -> WhiteNoiseAlertDialog(
            onDismissRequest = controller::cancel,
            title = { Text(stringResource(R.string.app_update_failed_title)) },
            text = { Text(selfUpdateFailureText(flow.failure)) },
            confirmButton = {
                TextButton(onClick = controller::retry) { Text(stringResource(R.string.app_update_retry)) }
            },
            dismissButton = {
                TextButton(onClick = controller::cancel) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun UpdateProgressDialog(
    title: String,
    body: String,
    state: String,
    onCancel: () -> Unit,
    progress: Float? = null,
    cancelLabel: String = stringResource(R.string.cancel),
) {
    WhiteNoiseAlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("appUpdate.progress").semantics { stateDescription = state },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                Text(body)
                if (progress == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(cancelLabel) } },
    )
}

@Composable
private fun selfUpdateFailureText(failure: AppSelfUpdateFailure?): String = stringResource(
    when (failure) {
        AppSelfUpdateFailure.Resolve -> R.string.app_update_resolve_failed
        AppSelfUpdateFailure.Download -> R.string.app_update_download_failed
        AppSelfUpdateFailure.Verification -> R.string.app_update_verification_failed
        AppSelfUpdateFailure.Install -> R.string.app_update_install_failed
        null -> R.string.app_update_resolve_failed
    },
)

@Composable
private fun formatBytes(bytes: Long?): String = when {
    bytes == null || bytes <= 0 -> stringResource(R.string.app_update_size_unknown)
    bytes >= 1_048_576 -> stringResource(R.string.app_update_size_mb, bytes / 1_048_576)
    bytes >= 1_024 -> stringResource(R.string.app_update_size_kb, bytes / 1_024)
    else -> stringResource(R.string.app_update_size_bytes, bytes)
}
