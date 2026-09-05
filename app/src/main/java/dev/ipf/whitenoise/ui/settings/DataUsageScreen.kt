package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import dev.ipf.whitenoise.ui.components.whiteNoiseDialogSelection
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun DataUsageScreen(profile: Profile, onBack: () -> Unit, onChange: (ProfileSettings) -> Unit,
    onPauseAutomatic: (Boolean) -> Unit = {}) {
    val settings = profile.settings
    var picker by rememberSaveable(profile.id) { mutableStateOf<DownloadMediaType?>(null) }
    var qualityPicker by rememberSaveable(profile.id) { mutableStateOf(false) }
    var stopConfirmation by rememberSaveable(profile.id) { mutableStateOf(false) }
    val counts = profile.downloadQueueCounts()
    val qualityLabels = SentMediaQuality.entries.associateWith { stringResource(when (it) {
        SentMediaQuality.Low -> R.string.photo_quality_low
        SentMediaQuality.Standard -> R.string.photo_quality_standard
        SentMediaQuality.High -> R.string.photo_quality_high
        SentMediaQuality.Original -> R.string.photo_quality_original
    }) }
    SettingsScaffold(title = stringResource(R.string.data_usage_title), onBack = onBack) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.download_automatic)) }
            item { SettingsGroup(Modifier.testTag("data_usage.downloads.group")) {
                DownloadMediaType.entries.forEach {type ->
                    val allowed = DownloadNetwork.entries.filter { settings.downloadMatrix.allows(type, it) }
                    val summary = allowed.map { stringResource(it.labelRes) }.joinToString().ifEmpty { stringResource(R.string.download_never) }
                    row {
                        SettingsLink(stringResource(type.labelRes), summary, { picker = type })
                    }
                }
                row {
                    SettingsAction(stringResource(R.string.download_reset),
                        onClick = { onChange(settings.copy(downloadMatrix = MediaDownloadMatrix())) },
                        subtitle = stringResource(R.string.download_reset_help),
                        enabled = settings.downloadMatrix != MediaDownloadMatrix())
                }
            } }
            item { SettingsExplainer(stringResource(R.string.download_rules_help)) }
            item { SettingsSection(stringResource(R.string.download_queue)) }
            item { SettingsGroup(Modifier.testTag("data_usage.queue")) {
                row {
                    SettingsAction(stringResource(if (settings.automaticDownloadsPaused) R.string.download_restart else R.string.download_stop),
                        subtitle = stringResource(if (settings.automaticDownloadsPaused) R.string.download_paused else R.string.download_enabled),
                        onClick = { if (settings.automaticDownloadsPaused) onPauseAutomatic(false) else stopConfirmation = true })
                }
            } }
            item { SettingsExplainer(stringResource(R.string.download_queue_counts, counts.automatic, counts.manual, counts.active, counts.failed)) }
            item { SettingsSection(stringResource(R.string.download_sent_media)) }
            item { SettingsGroup(Modifier.testTag("data_usage.sent_media.group")) {
                row {
                    SettingsLink(stringResource(R.string.download_quality), qualityLabels.getValue(settings.sentMediaQuality), { qualityPicker = true })
                }
            } }
            item { SettingsExplainer(stringResource(R.string.download_quality_help)) }
        }
    }
    picker?.let { type -> AlertDialog(onDismissRequest = { picker = null },
        title = { Text(stringResource(type.labelRes)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()).testTag("download.network.options")) {
            DownloadNetwork.entries.forEach { network ->
                val enabled = settings.downloadMatrix.allows(type, network)
                Row(Modifier.fillMaxWidth().whiteNoiseDialogSelection(enabled).toggleable(enabled, role = Role.Switch,
                    onValueChange = { onChange(settings.copy(downloadMatrix = settings.downloadMatrix.change(type, network, it))) })
                    .testTag("download.network.${network.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    Text(stringResource(network.labelRes), Modifier.weight(1f))
                    Switch(enabled, onCheckedChange = null)
                }
            }
            Text(stringResource(R.string.download_rules_help))
        } }, confirmButton = { TextButton({ picker = null }) { Text(stringResource(R.string.download_done)) } }) }
    if (qualityPicker) ChoiceDialog(title = stringResource(R.string.download_quality), values = SentMediaQuality.entries,
        selected = settings.sentMediaQuality, label = { qualityLabels.getValue(it) },
        onDismiss = { qualityPicker = false },
        onSelect = { onChange(settings.copy(sentMediaQuality = it)); qualityPicker = false })
    if (stopConfirmation) AlertDialog(onDismissRequest = { stopConfirmation = false },
        title = { Text(stringResource(R.string.download_stop)) },
        text = { Text(stringResource(R.string.download_stop_confirmation)) },
        confirmButton = { TextButton({ onPauseAutomatic(true); stopConfirmation = false }, Modifier.testTag("download.stop.confirm")) {
            Text(stringResource(R.string.download_stop)) } },
        dismissButton = { TextButton({ stopConfirmation = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun SettingsGroupScope.DownloadExampleControls(example: DownloadNetworkExample, held: Boolean,
    onNetwork: (DownloadNetworkExample) -> Unit, onSeed: () -> Unit, onHold: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    row {
        SettingsLink(stringResource(R.string.ui_download_network_example), example.developerLabel, { open = true })
    }
    row {
        SettingsAction(stringResource(R.string.ui_load_automatic_download_queue), onSeed,
            subtitle = stringResource(R.string.ui_uses_existing_local_attachments_and_holds_transfer_pro))
    }
    row {
        SettingsSwitch(stringResource(R.string.ui_hold_transfer_example), held, onHold)
    }
    if (open) ChoiceDialog(stringResource(R.string.ui_download_network_example), DownloadNetworkExample.entries, example,
        { it.developerLabel }, onDismiss = { open = false }, onSelect = { onNetwork(it); open = false })
}
