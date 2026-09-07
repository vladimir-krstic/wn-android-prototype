package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog

@Composable
fun ChatDownloadScreen(
    profile: Profile, chat: Chat, onBack: () -> Unit,
    onInheritance: (DownloadMediaType, Boolean) -> Unit,
    onNetwork: (DownloadMediaType, DownloadNetwork, Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var picker by rememberSaveable(profile.id, chat.id) { mutableStateOf<DownloadMediaType?>(null) }
    val overrides = chat.downloadOverrides
    val defaults = profile.settings.downloadMatrix
    SettingsScaffold(stringResource(R.string.download_automatic), onBack) {
        SettingsList {
            item { SettingsSection(chat.title) }
            item { SettingsGroup(Modifier.testTag("chat.downloads.group")) {
                DownloadMediaType.entries.forEach { type ->
                    val networks = overrides.networks(type, defaults)
                    val resolved = DownloadNetwork.entries.filter { it in networks }
                        .map { stringResource(it.labelRes) }.joinToString().ifEmpty { stringResource(R.string.download_never) }
                    val summary = stringResource(if (overrides.inherits(type)) R.string.download_chat_inherited else R.string.download_chat_custom, resolved)
                    row { SettingsLink(stringResource(type.labelRes), summary, { picker = type },
                        modifier = Modifier.testTag("chat.downloads.${type.name}")) }
                }
                row { SettingsAction(stringResource(R.string.download_reset), onReset,
                    subtitle = stringResource(R.string.download_chat_reset_help), enabled = overrides.media.isNotEmpty(),
                    modifier = Modifier.testTag("chat.downloads.reset")) }
            } }
            item { SettingsExplainer(stringResource(R.string.download_chat_help)) }
            if (profile.settings.automaticDownloadsPaused) item { SettingsCallout(stringResource(R.string.download_paused)) }
            if (chat.muteDuration != null) item { SettingsExplainer(stringResource(R.string.download_chat_muted)) }
        }
    }
    picker?.let { type ->
        val inherited = overrides.inherits(type)
        WhiteNoiseAlertDialog(
            onDismissRequest = { picker = null },
            title = { Text(stringResource(type.labelRes)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()).testTag("download.network.options")) {
                DownloadSwitch(stringResource(R.string.download_use_profile), inherited, "chat.downloads.inherit") { onInheritance(type, it) }
                DownloadNetworkOptions(overrides.networks(type, defaults), enabled = !inherited) { network, enabled ->
                    onNetwork(type, network, enabled)
                }
                Text(stringResource(R.string.download_rules_help))
            } },
            confirmButton = { TextButton(onClick = { picker = null }) { Text(stringResource(R.string.download_done)) } },
        )
    }
}
