package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.ui.theme.amoledOutlineBorder
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ConversationDebugAccess
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ConversationDebugSnapshot
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun DeveloperToolsScreen(
    profile: Profile,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Boolean,
    onDebugMode: (Boolean) -> Boolean,
    onDiagnostics: () -> Unit,
    onKeyPackages: () -> Unit,
    onAuditLogs: () -> Unit = {},
    onScenarios: () -> Unit = {},
) {
    val context = LocalContext.current
    val tools = profile.developerTools
    var exportContent by rememberSaveable(profile.id) { mutableStateOf("") }
    var saveErrorDialog by rememberSaveable(profile.id) { mutableStateOf(false) }
    val exportLogs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                checkNotNull(context.contentResolver.openOutputStream(uri))
                    .bufferedWriter()
                    .use { writer -> writer.write(exportContent) }
            }
            saveErrorDialog = result.isFailure
        }
        exportContent = ""
    }
    SettingsScaffold(title = stringResource(R.string.developer_tools), onBack = onBack) {
        SettingsList {
            item {
                SettingsCallout(
                    title = stringResource(R.string.developer_for_development_and_testing_only),
                    text = "These tools can expose technical information and change how the app behaves.",
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            item {
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                    row {
                        SettingsSwitch(
                            title = stringResource(R.string.developer_tools),
                            checked = tools.isEnabled,
                            onCheckedChange = { onEnabled(it) },
                        )
                    }
                }
                SettingsExplainer(stringResource(R.string.developer_enable_technical_tools_for_this_profile))
            }
            item {
                SettingsGroup { row {
                    SettingsLink("Scenarios", "Browse every scenario and launch a temporary test session", onScenarios,
                        modifier = Modifier.testTag("developer.scenarios"))
                } }
            }
            if (tools.isEnabled) {
                item { SettingsSection(stringResource(R.string.developer_debugging)) }
                item {
                    SettingsGroup {
                        row {
                            SettingsSwitch(
                                title = stringResource(R.string.developer_debug_mode),
                                checked = tools.debugMode,
                                onCheckedChange = { onDebugMode(it) },
                            )
                        }
                        row {
                            SettingsLink(stringResource(R.string.developer_diagnostics), stringResource(R.string.developer_diagnostics_help), onDiagnostics)
                        }
                        row {
                            SettingsLink(stringResource(R.string.audit_logs_title),stringResource(R.string.audit_logs_sensitive),onAuditLogs)
                        }
                    }
                    SettingsExplainer(
                        stringResource(R.string.developer_debug_mode_adds_technical_details_to_supported_convers),
                    )
                }
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                        row {
                            SettingsLink(stringResource(R.string.developer_key_packages), stringResource(R.string.developer_key_packages_help), onKeyPackages)
                        }
                    }
                }
                item { SettingsSection(stringResource(R.string.developer_diagnostic_logs)) }
                item {
                    val nonemptyRecords = profile.diagnostics.records.filter { it.byteCount > 0 }
                    SettingsGroup {
                        row {
                            SettingsMetadata(
                                title = stringResource(R.string.developer_diagnostic_logging),
                                value = if (profile.diagnostics.loggingEnabled) "On" else "Off",
                            )
                        }
                        if (nonemptyRecords.isEmpty()) {
                            item {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                ) {
                                    Text(
                                        text = "There are no logs.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        nonemptyRecords.forEach { file ->
                            item {
                                ListItem(
                                    supportingContent = { Text("${fileSize(file.byteCount)} · ${file.createdLabel} · ${file.profileName}") },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                ) {
                                    Text(file.filename, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        if (nonemptyRecords.isNotEmpty()) {
                            row {
                                SettingsAction(
                                    title = stringResource(R.string.developer_export_diagnostic_logs),
                                    onClick = {
                                        exportContent = profile.diagnostics.diagnosticLogExportText
                                        exportLogs.launch("White Noise Diagnostic Logs.txt")
                                    },
                                    leading = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_download),
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    SettingsExplainer(
                        stringResource(R.string.developer_configure_or_clear_diagnostic_logs_in_privacy_security) +
                            "Existing sanitized files remain available here after logging is turned off.",
                    )
                }
            }
            item { SettingsSection(stringResource(R.string.about)) }
            item {
                SettingsGroup {
                    row {
                        SettingsMetadata("Version", "0.1 (1)")
                    }
                    row {
                        SettingsMetadata("Built on", "MarmotKit (790eb860)")
                    }
                }
            }
        }
    }
    if (saveErrorDialog) {
        AlertDialog(
            onDismissRequest = { saveErrorDialog = false },
            title = { Text(stringResource(R.string.developer_couldnt_save_diagnostic_logs)) },
            text = { Text(stringResource(R.string.developer_choose_another_location_and_try_again)) },
            confirmButton = {
                TextButton(onClick = { saveErrorDialog = false }) {
                    Text(stringResource(R.string.batch_dismiss))
                }
            },
        )
    }
}

@Composable
fun DiagnosticsScreen(
    profile: Profile,
    diagnosticSummary: String?,
    onBack: () -> Unit,
    onTest: () -> Boolean,
    onClear: () -> Boolean,
    parityController: dev.ipf.whitenoise.state.DeveloperParityController? = null,
) {
    if (parityController != null) DeveloperOperationHost(profile, "diagnostics", parityController)
    var showHealth by rememberSaveable(profile.id) { mutableStateOf(false) }
    if (showHealth && parityController != null && profile.developerTools.isEnabled) DiagnosticHealthSheet(profile, parityController) {
        showHealth = false
        parityController.work?.let { if (it.phase == dev.ipf.whitenoise.model.DeveloperPhase.Running) parityController.dismiss(it.id) }
    }
    if (!profile.developerTools.isEnabled) {
        SettingsScaffold(title = stringResource(R.string.developer_diagnostics), onBack = onBack) { SettingsCallout(stringResource(R.string.developer_disabled)) }
        return
    }
    val context = LocalContext.current
    val events = profile.developerTools.diagnosticEvents
    var actionsExpanded by remember { mutableStateOf(false) }
    SettingsScaffold(
        title = stringResource(R.string.developer_diagnostics),
        onBack = onBack,
        topBarActions = {
            Box {
                IconButton(
                    onClick = { actionsExpanded = true },
                    modifier = Modifier.testTag("diagnostics.actions"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.developer_diagnostic_actions),
                    )
                }
                WhiteNoiseDropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                    modifier = Modifier.testTag("diagnostics.actions.menu"),
                    items = buildList {
                        if (parityController != null && profile.developerTools.isEnabled) add(WhiteNoiseMenuItem(
                            label = stringResource(R.string.developer_health), icon = R.drawable.ic_bug_report,
                            onClick = { showHealth = true }, modifier = Modifier.testTag("diagnostics.action.health"),
                        ))
                        diagnosticSummary?.let { summary ->
                            add(
                                WhiteNoiseMenuItem(
                                    label = "Copy Diagnostic Summary",
                                    icon = R.drawable.ic_content_copy,
                                    onClick = {
                                        copyToClipboard(context, "Diagnostic summary", summary)
                                    },
                                    modifier = Modifier.testTag("diagnostics.action.copy_summary"),
                                ),
                            )
                        }
                        add(
                            WhiteNoiseMenuItem(
                                label = "Test",
                                icon = R.drawable.ic_check,
                                onClick = { onTest() },
                                modifier = Modifier.testTag("diagnostics.action.test"),
                            ),
                        )
                        add(
                            WhiteNoiseMenuItem(
                                label = "Clear Events",
                                icon = R.drawable.ic_delete,
                                onClick = { onClear() },
                                enabled = events.isNotEmpty(),
                                modifier = Modifier.testTag("diagnostics.action.clear"),
                            ),
                        )
                    },
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(WhiteNoiseSpacing.CompactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.FormField),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.developer_events),
                    modifier = Modifier.testTag("diagnostics.events_title"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
                DiagnosticLiveIndicator()
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = amoledOutlineBorder(),
                shape = MaterialTheme.shapes.large,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (events.isEmpty()) {
                        WhiteNoiseEmptyState(
                            title = stringResource(R.string.developer_no_events),
                            detail = "Run a diagnostic test to add a sanitized event.",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = WhiteNoiseSpacing.FormField,
                                vertical = WhiteNoiseSpacing.Related,
                            ),
                        ) {
                            itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                                Column {
                                    Text(
                                        event.text,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("diagnostics.event.$index")
                                            .padding(vertical = WhiteNoiseSpacing.FormField),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (index != events.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticLiveIndicator() {
    val liveEventStreamDescription = stringResource(R.string.developer_live_event_stream)
    val infiniteTransition = rememberInfiniteTransition(label = "diagnostics_live")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "diagnostics_live_alpha",
    )
    Row(
        modifier = Modifier
            .testTag("diagnostics.live_indicator")
            .clearAndSetSemantics { contentDescription = liveEventStreamDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings_cell_tower),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { alpha = pulseAlpha },
            tint = DiagnosticLiveGreen,
        )
        Text(
            text = "Live",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private val DiagnosticLiveGreen = Color(0xFF188038)

@Composable
fun ConversationDebugScreen(
    profile: Profile,
    chat: Chat?,
    snapshot: ConversationDebugSnapshot?,
    onBack: () -> Unit,
    onOpenDeveloperTools: () -> Unit,
    onDiagnostics: () -> Unit,
    parityController: dev.ipf.whitenoise.state.DeveloperParityController? = null,
) {
    val context = LocalContext.current
    if (chat != null && parityController != null) DeveloperOperationHost(profile, "push:${chat.id}", parityController)
    SettingsScaffold(title = stringResource(R.string.conversation_debug), onBack = onBack) {
        when (chat?.let { ConversationDebugPolicy.access(profile, it.id) } ?: ConversationDebugAccess.Unavailable) {
            ConversationDebugAccess.Unavailable -> DebugUnavailable("Chat unavailable", "This conversation is no longer available for inspection.")
            ConversationDebugAccess.Disabled -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WhiteNoiseEmptyState(
                        title = stringResource(R.string.developer_conversation_debugging_is_off),
                        detail = "Turn on Developer Tools and Debug Mode for this profile to inspect this chat.",
                    )
                    WhiteNoiseButton(
                        onClick = onOpenDeveloperTools,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.developer_open_developer_tools)) }
                }
            }
            ConversationDebugAccess.Enabled -> {
                val info = snapshot
                if (info == null) {
                    DebugUnavailable(
                        "Debug data unavailable",
                        "Technical details for this conversation could not be prepared.",
                    )
                } else {
                    SettingsList {
                    item { SettingsSection(stringResource(R.string.developer_conversation)) }
                    item {
                        SettingsGroup {
                            row {
                                DebugValue("State", info.lifecycle)
                            }
                            row {
                                DebugValue("Epoch", info.epoch.toString())
                            }
                            info.memberCount?.let {
                                row {
                                    DebugValue("MLS members", it.toString())
                                }
                            }
                            info.adminCount?.let {
                                row {
                                    DebugValue("Admins", it.toString())
                                }
                            }
                            info.currentRole?.let {
                                row {
                                    DebugValue("Your role", it)
                                }
                            }
                            row {
                                DebugValue("Event kinds", info.requiredEventKinds.joinToString())
                            }
                            row {
                                DebugValue("Required components", dev.ipf.whitenoise.model.DeveloperInspection.requiredComponents.joinToString("\n"))
                            }
                            item {
                                CopyableDebugValue("MLS group ID", info.mlsGroupId) {
                                    copyToClipboard(context, "MLS group ID", info.mlsGroupId)
                                }
                            }
                            item {
                                CopyableDebugValue("Nostr group ID", info.nostrGroupId) {
                                    copyToClipboard(context, "Nostr group ID", info.nostrGroupId)
                                }
                            }
                        }
                    }
                    if (parityController != null) item {
                        SettingsGroup {
                            item {
                                TextButton(onClick = { parityController.begin(dev.ipf.whitenoise.model.DeveloperOperation.RefreshPush) },
                                                           enabled = parityController.work?.phase != dev.ipf.whitenoise.model.DeveloperPhase.Running) { Text(stringResource(R.string.developer_refresh)) }
                            }
                        }
                        DeveloperResult(parityController)
                    }
                    if (parityController == null || parityController.work?.phase == dev.ipf.whitenoise.model.DeveloperPhase.Complete) {
                    item { SettingsSection(stringResource(R.string.developer_delivery_notifications)) }
                    item {
                        SettingsGroup {
                            row {
                                DebugValue("Chat relays", info.relayCount.toString())
                            }
                            row {
                                DebugValue("Notifications", if (info.push.notificationsEnabled) "On" else "Off")
                            }
                            row {
                                DebugValue("Push", info.push.registrationStatus)
                            }
                            row {
                                DebugValue("Total tokens", info.push.totalTokenCount.toString())
                            }
                            row {
                                DebugValue("Active tokens", info.push.activeTokenCount.toString())
                            }
                            row {
                                DebugValue("Local notifications", info.push.localNotifications.toString())
                            }
                            row {
                                DebugValue("Shareable", info.push.shareable.toString())
                            }
                            row {
                                DebugValue("Local leaf", info.push.localLeaf?.toString() ?: "Unavailable")
                            }
                            row {
                                DebugValue("Local token cached", info.push.localTokenCached.toString())
                            }
                            row {
                                DebugValue("Token list updated", info.push.updatedAt)
                            }
                            if (info.push.staleTokenCount > 0) {
                                row {
                                    DebugValue("Push tokens", "${info.push.staleTokenCount} stale")
                                }
                            }
                            if (info.push.missingRelayHintCount > 0) {
                                row {
                                    DebugValue("Relay hints", "${info.push.missingRelayHintCount} missing")
                                }
                            }
                        }
                    }
                    info.push.members.forEach { token -> item(key = "push-${token.memberId}") {
                        SettingsSection(stringResource(R.string.developer_member_token, token.leaf))
                        SettingsGroup {
                            item {
                                CopyableDebugValue("Member", token.memberId) { copyToClipboard(context, "Member", token.memberId) }
                            }
                            row {
                                DebugValue("Platform", token.platform)
                            }
                            row {
                                DebugValue("Fingerprint", token.fingerprint)
                            }
                            item {
                                CopyableDebugValue("Push server public key", token.serverKey) { copyToClipboard(context, "Push server public key", token.serverKey) }
                            }
                            row {
                                DebugValue("Relay hint", token.relayHint.toString())
                            }
                            row {
                                DebugValue("Active leaf", token.activeLeaf.toString())
                            }
                            row {
                                DebugValue("Matches active leaf", token.matchesLeaf.toString())
                            }
                            row {
                                DebugValue("Local member", token.localMember.toString())
                            }
                            row {
                                DebugValue("Updated", token.updatedAt)
                            }
                        }
                    } }
                    }
                    item { SettingsSection(stringResource(R.string.developer_diagnostics)) }
                    item {
                        SettingsGroup {
                            row {
                                SettingsLink(stringResource(R.string.developer_diagnostics), stringResource(R.string.developer_diagnostics_conversation_help), onDiagnostics)
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugUnavailable(title: String, detail: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhiteNoiseEmptyState(title = title, detail = detail)
    }
}

@Composable
private fun DebugValue(label: String, value: String) {
    SettingsValue(label, value)
}

@Composable
private fun CopyableDebugValue(label: String, value: String, onCopy: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(shorten(value), fontFamily = FontFamily.Monospace) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_content_copy),
                contentDescription = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .semantics { role = Role.Button },
    )
}

private fun shorten(value: String): String =
    if (value.length <= 22) value else "${value.take(12)}…${value.takeLast(6)}"

private fun fileSize(bytes: Int): String = when {
    bytes == 0 -> "0 B"
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}
