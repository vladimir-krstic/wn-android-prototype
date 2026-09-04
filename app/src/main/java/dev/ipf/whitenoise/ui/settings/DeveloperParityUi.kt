package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.DeveloperParityController
import dev.ipf.whitenoise.ui.components.WhiteNoiseFilledTonalButton
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

@Composable
internal fun DeveloperOperationHost(profile: Profile, surface: String, controller: DeveloperParityController) {
    LifecycleResumeEffect(profile.id, profile.developerTools.isEnabled, surface, controller) {
        if (profile.developerTools.isEnabled) controller.open(profile.id, surface)
        onPauseOrDispose { controller.close(profile.id, surface) }
    }
    val w = controller.work
    LaunchedEffect(w?.id, w?.phase) {
        if (w?.phase == DeveloperPhase.Running) { delay(400); controller.complete(w.id) }
    }
}

@Composable
internal fun DeveloperResult(controller: DeveloperParityController) {
    val w = controller.work ?: return
    if (w.phase == DeveloperPhase.Confirm) return
    Column(Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin).semantics { liveRegion = LiveRegionMode.Polite }) {
        if (w.phase == DeveloperPhase.Running) LinearProgressIndicator(Modifier.fillMaxWidth())
        val action = stringResource(when (w.operation) {
            DeveloperOperation.RefreshPackages -> R.string.developer_refresh_packages
            DeveloperOperation.Republish -> R.string.developer_republish
            DeveloperOperation.PublishNew -> R.string.developer_publish_new
            DeveloperOperation.DeletePackage -> R.string.developer_delete_package
            DeveloperOperation.RefreshHealth -> R.string.developer_refresh_health
            DeveloperOperation.SendToSelf -> R.string.developer_self_send
            DeveloperOperation.RefreshPush -> R.string.developer_refresh_push
        })
        Text(stringResource(R.string.developer_work_status, action, stringResource(when (w.phase) {
            DeveloperPhase.Running -> R.string.developer_work_running
            DeveloperPhase.Complete -> R.string.developer_work_complete
            DeveloperPhase.Partial -> R.string.developer_work_partial
            DeveloperPhase.Failed -> R.string.developer_work_failed
            DeveloperPhase.Unavailable -> R.string.developer_work_unavailable
            DeveloperPhase.Confirm -> R.string.developer_work_running
        })), Modifier.testTag("developer.result"), color = if (w.phase == DeveloperPhase.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        if (w.phase in listOf(DeveloperPhase.Partial, DeveloperPhase.Failed, DeveloperPhase.Unavailable)) {
            TextButton(onClick = { controller.retry(w.id) }) { Text(stringResource(R.string.developer_retry)) }
        }
    }
}

@Composable
fun KeyPackagesScreen(profile: Profile, controller: DeveloperParityController, onBack: () -> Unit) {
    DeveloperOperationHost(profile, "packages", controller)
    val packages = DeveloperInspection.packages(profile)
    val w = controller.work
    val busy = w?.phase in listOf(DeveloperPhase.Running, DeveloperPhase.Confirm)
    val enabled = profile.developerTools.isEnabled && !busy
    SettingsScaffold(title = "Key Packages", onBack = onBack, topBarActions = {
        TextButton(onClick = { controller.begin(DeveloperOperation.RefreshPackages) }, enabled = enabled) { Text(stringResource(R.string.developer_refresh)) }
    }) {
        SettingsList {
            if (!profile.developerTools.isEnabled) item { SettingsExplainer(stringResource(R.string.developer_disabled)) }
            else {
                item { SettingsSection(stringResource(R.string.developer_publishing)) }
                item {
                    SettingsGroup {
                        TextButton(onClick = { controller.begin(DeveloperOperation.Republish) }, enabled = enabled && packages.any { it.local && it.id == profile.developerTools.keyPackage.id }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.developer_republish))
                        }
                    }
                    SettingsExplainer(stringResource(R.string.developer_republish_help))
                    WhiteNoiseFilledTonalButton(onClick = { controller.begin(DeveloperOperation.PublishNew) }, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related).testTag("key_packages.publish")) {
                        Text(stringResource(R.string.developer_publish_new))
                    }
                    SettingsExplainer(stringResource(R.string.developer_rotate_help))
                }
                item { DeveloperResult(controller) }
                item { SettingsSection(stringResource(R.string.developer_published)) }
                if (packages.none { it.relays.isNotEmpty() }) item { SettingsExplainer(stringResource(R.string.developer_not_published)) }
                packages.filter { it.relays.isNotEmpty() }.forEach { kp ->
                    item(key = "published-${kp.id}") {
                        SettingsGroup {
                            SettingsValue("Package ID", kp.id)
                            SettingsValue("Published", kp.published)
                            SettingsValue("Source", if (kp.local) "Local and relay" else "Relay")
                            SettingsValue(stringResource(R.string.developer_seen_on), kp.relays.joinToString("\n"))
                            TextButton(onClick = { controller.begin(DeveloperOperation.DeletePackage, kp.id) }, enabled = enabled) {
                                Text(stringResource(R.string.developer_delete_package), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item { SettingsSection(stringResource(R.string.developer_retained)) }
                item { SettingsExplainer(stringResource(R.string.developer_retained_help)) }
                val retained = packages.filter { it.local && it.relays.isEmpty() }
                if (retained.isEmpty()) item { SettingsExplainer(stringResource(R.string.developer_no_retained)) }
                retained.forEach { kp -> item(key = "local-${kp.id}") {
                    SettingsGroup { SettingsValue("Package ID", kp.id); SettingsValue("Source", "Local"); SettingsValue("Size", kp.size) }
                } }
            }
        }
    }
    if (w?.phase == DeveloperPhase.Confirm && w.operation == DeveloperOperation.DeletePackage) AlertDialog(
        onDismissRequest = { controller.dismiss(w.id) },
        title = { Text(stringResource(R.string.developer_delete_title)) },
        text = { Text(stringResource(R.string.developer_delete_help, w.targetId.orEmpty())) },
        confirmButton = { TextButton(onClick = { controller.confirm(w.id) }) { Text(stringResource(R.string.developer_delete_package)) } },
        dismissButton = { TextButton(onClick = { controller.dismiss(w.id) }) { Text(stringResource(R.string.developer_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticHealthSheet(profile: Profile, controller: DeveloperParityController, onDismiss: () -> Unit) {
    val busy = controller.work?.phase == DeveloperPhase.Running
    var remaining by remember { mutableLongStateOf(controller.remainingMillis()) }
    LaunchedEffect(profile.id, profile.developerTools.performanceUntilMillis) {
        do { remaining = controller.remainingMillis(); if (remaining > 0) delay(1_000) } while (remaining > 0)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = WhiteNoiseSpacing.Section)) {
            SettingsSection(stringResource(R.string.developer_health))
            SettingsGroup {
                TextButton(onClick = { controller.begin(DeveloperOperation.RefreshHealth) }, enabled = !busy) { Text(stringResource(R.string.developer_refresh)) }
                SettingsValue("Active profile", "Present")
                val health = profile.developerTools.health
                if (health == null) SettingsValue("Relay health", "Unavailable") else {
                    SettingsValue("Relays", health.total.toString())
                    SettingsValue("Connected", health.connected.toString())
                    SettingsValue("Connecting", health.connecting.toString())
                    SettingsValue("Disconnected", health.disconnected.toString())
                    SettingsValue("Connection attempts", health.attempts.toString())
                    SettingsValue("Connection successes", health.successes.toString())
                    SettingsValue("Signed-in profiles", health.profiles.toString())
                    SettingsValue("Bootstrap relays", health.bootstrapRelays.toString())
                }
                TextButton(onClick = { controller.begin(DeveloperOperation.SendToSelf) }, enabled = !busy) { Text(stringResource(R.string.developer_self_send)) }
            }
            if (profile.developerTools.health != null && controller.work?.let { it.operation == DeveloperOperation.RefreshHealth && it.phase != DeveloperPhase.Complete } == true) {
                SettingsExplainer("Showing the last successful health snapshot.")
            }
            DeveloperResult(controller)
            if (controller.performanceAvailable) {
                SettingsSection(stringResource(R.string.developer_performance))
                SettingsGroup {
                    SettingsSwitch(stringResource(R.string.developer_performance), checked = remaining > 0,
                        onCheckedChange = { controller.performance(it); remaining = controller.remainingMillis() })
                    SettingsValue("Status", if (remaining > 0) pluralStringResource(R.plurals.developer_performance_remaining, ((remaining + 59_999) / 60_000).toInt(), ((remaining + 59_999) / 60_000).toInt()) else stringResource(R.string.developer_inactive))
                }
                SettingsExplainer(stringResource(R.string.developer_performance_help))
            }
        }
    }
}

@Composable
internal fun DeveloperParityControls(profile: Profile, controller: DeveloperParityController) {
    var choose by remember { mutableStateOf(false) }
    var inventory by remember { mutableStateOf(false) }
    SettingsSection("Inspection controls")
    SettingsGroup {
        SettingsSwitch(stringResource(R.string.developer_streaming), checked = profile.developerTools.streamingDebug, onCheckedChange = controller::streaming)
        SettingsLink("Inspection operation outcome", controller.outcome.name, { choose = true })
        SettingsLink("Key-package inventory example", onClick = { inventory = true })
    }
    SettingsExplainer(stringResource(R.string.developer_streaming_help))
    if (inventory) ScenarioChoiceDialog("Key-package inventory example", PackageInventoryExample.entries, PackageInventoryExample.Published, { it.name }, controller::inventoryExample, { inventory = false })
    if (choose) ScenarioChoiceDialog("Inspection operation outcome", DeveloperOutcome.entries, controller.outcome, { it.name }, controller::chooseOutcome, { choose = false })
}
