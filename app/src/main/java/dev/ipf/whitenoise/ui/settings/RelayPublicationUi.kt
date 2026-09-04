package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.RelayPublicationController
import kotlinx.coroutines.delay

@Composable
internal fun RelayPublicationHost(profileId: String, surface: String, controller: RelayPublicationController) {
    DisposableEffect(profileId, surface, controller) {
        controller.open(profileId, surface)
        onDispose { controller.close(profileId, surface) }
    }
    val running = controller.work?.takeIf { it.profileId == profileId && it.surface == surface &&
        it.phase == RelayPublicationWorkPhase.Running }
    LaunchedEffect(running?.id) {
        running?.let { delay(450); controller.complete(it.id) }
    }
}

@Composable
internal fun RelayPublicationRows(profile: Profile, controller: RelayPublicationController) {
    val projection = controller.projection(profile)
    val work = controller.work?.takeIf { it.profileId == profile.id }
    PublishedRelayList.entries.forEach { kind ->
        ListItem(
            headlineContent = { Text(if (kind == PublishedRelayList.Posting) stringResource(R.string.relay_list_posting) else stringResource(R.string.relay_list_receiving)) },
            trailingContent = {
                Text(when (projection.status(kind)) {
                    RelayProjectionPhase.Published -> stringResource(R.string.relay_list_published)
                    RelayProjectionPhase.Missing -> stringResource(R.string.relay_list_missing)
                    RelayProjectionPhase.Unavailable -> stringResource(R.string.relay_list_status_unavailable)
                })
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.testTag("relay.publication.${kind.name}"),
        )
        SettingsDivider()
    }
    when (work?.phase) {
        RelayPublicationWorkPhase.Running -> SettingsAction(
            title = if (work.operation == RelayPublicationOperation.Refresh) stringResource(R.string.relay_list_refreshing) else stringResource(R.string.relay_list_publishing),
            enabled = false,
            leading = { CircularProgressIndicator(Modifier.size(20.dp).clearAndSetSemantics { }, strokeWidth = 2.dp) },
            onClick = {},
        )
        RelayPublicationWorkPhase.Failed, RelayPublicationWorkPhase.Unavailable -> SettingsAction(
            title = stringResource(R.string.relay_list_retry),
            subtitle = if (work.operation == RelayPublicationOperation.Refresh)
                stringResource(R.string.relay_list_refresh_failed)
            else stringResource(R.string.relay_list_publish_failed),
            onClick = { controller.retry(work.id) },
            modifier = Modifier.testTag("relay.publication.retry"),
        )
        else -> {
            SettingsAction(stringResource(R.string.relay_list_refresh), { controller.begin(RelayPublicationOperation.Refresh) },
                modifier = Modifier.testTag("relay.publication.refresh"))
            if (projection.missing.isNotEmpty()) {
                SettingsDivider()
                SettingsAction(stringResource(R.string.relay_list_publish), { controller.begin(RelayPublicationOperation.PublishMissing) },
                    subtitle = stringResource(R.string.relay_list_publish_help),
                    modifier = Modifier.testTag("relay.publication.publish"))
            }
        }
    }
}

@Composable
internal fun RelayPublicationDeveloperControls(controller: RelayPublicationController, onImport: () -> Unit = {}) {
    var open by remember { mutableStateOf(false) }
    SettingsLink("Relay publication outcome", controller.scenario.developerLabel, { open = true })
    SettingsAction("Load imported relay issue", onImport,
        subtitle = "Adds one invalid imported address without removing its roles.")
    if (open) ChoiceDialog("Relay publication outcome", RelayPublicationScenario.entries,
        controller.scenario, RelayPublicationScenario::developerLabel,
        onDismiss = { open = false }, onSelect = { controller.chooseScenario(it); open = false })
}
