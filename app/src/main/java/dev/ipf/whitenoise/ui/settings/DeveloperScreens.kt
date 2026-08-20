package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ConversationDebugAccess
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ConversationDebugSnapshot
import dev.ipf.whitenoise.model.Profile

@Composable
fun DeveloperToolsScreen(
    profile: Profile,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Boolean,
    onDebugMode: (Boolean) -> Boolean,
    onDiagnostics: () -> Unit,
    onKeyPackages: () -> Unit,
    onTelemetry: (Boolean) -> Boolean,
    onAuditLogging: (Boolean) -> Boolean,
    onClearAuditLogs: () -> Boolean,
) {
    val tools = profile.developerTools
    var clearLogsDialog by remember { mutableStateOf(false) }
    SettingsScaffold(title = "Developer Tools", onBack = onBack) {
        SettingsList {
            item {
                ListItem(
                    headlineContent = { Text("For development and testing only") },
                    supportingContent = { Text("These tools can expose technical information and change how the app behaves.") },
                    leadingContent = {
                        Text("⚠", modifier = Modifier.clearAndSetSemantics { }, color = MaterialTheme.colorScheme.tertiary)
                    },
                )
            }
            item {
                SettingsSwitch(
                    title = "Developer Tools",
                    checked = tools.isEnabled,
                    onCheckedChange = { onEnabled(it) },
                    subtitle = "Enable technical tools for this profile.",
                )
            }
            if (tools.isEnabled) {
                item { SettingsSection("Debugging") }
                item {
                    SettingsSwitch(
                        title = "Debug Mode",
                        checked = tools.debugMode,
                        onCheckedChange = { onDebugMode(it) },
                        subtitle = "Adds technical details to the accepted conversations.",
                    )
                }
                item { SettingsLink("Diagnostics", "Persistent sanitized event console", onDiagnostics) }
                item { SettingsLink("Key Packages", "Exactly one current package", onKeyPackages) }
                item { SettingsSection("Telemetry") }
                item {
                    SettingsSwitch(
                        title = "Anonymous Telemetry",
                        checked = tools.anonymousTelemetry,
                        onCheckedChange = { onTelemetry(it) },
                        subtitle = "Shares anonymous reliability and performance data. It doesn’t include messages or profile keys.",
                    )
                }
                item { SettingsSection("Audit logging") }
                item {
                    SettingsSwitch(
                        title = "Audit Logging",
                        checked = tools.auditLogging,
                        onCheckedChange = { onAuditLogging(it) },
                        subtitle = "Stores sanitized technical activity locally for troubleshooting.",
                    )
                }
                if (tools.auditLogging) {
                    items(tools.auditFiles.size, key = { tools.auditFiles[it].id }) { index ->
                        val file = tools.auditFiles[index]
                        ListItem(
                            headlineContent = { Text(file.filename, fontFamily = FontFamily.Monospace) },
                            supportingContent = {
                                Text("${fileSize(file.byteCount)} · ${file.createdLabel} · ${file.profileName}")
                            },
                        )
                        HorizontalDivider()
                    }
                    item {
                        TextButton(
                            onClick = { clearLogsDialog = true },
                            enabled = tools.auditLogsContainData,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Clear Audit Logs", color = MaterialTheme.colorScheme.error) }
                        SettingsExplainer("Turning logging off hides the files but keeps them. Clearing removes their contents without deleting the files.")
                    }
                }
            }
            item { SettingsSection("About") }
            item {
                ListItem(headlineContent = { Text("Version") }, trailingContent = { Text("0.1 (1)") })
                HorizontalDivider()
                ListItem(headlineContent = { Text("Built on") }, trailingContent = { Text("MarmotKit (790eb860)") })
            }
        }
    }
    if (clearLogsDialog) {
        AlertDialog(
            onDismissRequest = { clearLogsDialog = false },
            title = { Text("Clear all audit logs?") },
            text = { Text("This removes all recorded activity from the audit log files. The files remain and Audit Logging stays on.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAuditLogs()
                    clearLogsDialog = false
                }) { Text("Clear logs", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { clearLogsDialog = false }) { Text("Cancel") } },
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
) {
    val context = LocalContext.current
    val events = profile.developerTools.diagnosticEvents
    SettingsScaffold(title = "Diagnostics", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Events", style = MaterialTheme.typography.titleMedium)
                Text("● Live", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onTest() }) { Text("Test") }
                OutlinedButton(onClick = { onClear() }, enabled = events.isNotEmpty()) { Text("Clear events") }
                diagnosticSummary?.let { summary ->
                    OutlinedButton(onClick = { copyToClipboard(context, "Diagnostic summary", summary) }) {
                        Text("Copy summary")
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (events.isEmpty()) {
                    Text("No events", style = MaterialTheme.typography.titleMedium)
                } else {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        events.forEachIndexed { index, event ->
                            Text(
                                event.text,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (index < events.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyPackagesScreen(
    profile: Profile,
    onBack: () -> Unit,
    onPublish: () -> Boolean,
) {
    val keyPackage = profile.developerTools.keyPackage
    SettingsScaffold(title = "Key Packages", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("Current key package")
            ListItem(
                headlineContent = { Text(keyPackage.id, fontFamily = FontFamily.Monospace) },
                supportingContent = { Text("Published ${keyPackage.published} · ${keyPackage.size}") },
            )
            HorizontalDivider()
            Button(
                onClick = { onPublish() },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Publish New Key Package") }
            SettingsExplainer("Publishes a new deterministic key package so this profile can receive group invitations.")
        }
    }
}

@Composable
fun ConversationDebugScreen(
    profile: Profile,
    chat: Chat?,
    snapshot: ConversationDebugSnapshot?,
    onBack: () -> Unit,
    onOpenDeveloperTools: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    SettingsScaffold(title = "Conversation Debug", onBack = onBack) {
        when (chat?.let { ConversationDebugPolicy.access(profile, it.id) } ?: ConversationDebugAccess.Unavailable) {
            ConversationDebugAccess.Unavailable -> DebugUnavailable("Chat unavailable", "This conversation is no longer available for inspection.")
            ConversationDebugAccess.Disabled -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Conversation Debugging Is Off", style = MaterialTheme.typography.headlineSmall)
                    Text("Turn on Developer Tools and Debug Mode for this profile to inspect this chat.")
                    Button(onClick = onOpenDeveloperTools, modifier = Modifier.padding(top = 16.dp)) { Text("Open Developer Tools") }
                }
            }
            ConversationDebugAccess.Enabled -> {
                val info = snapshot ?: return@SettingsScaffold
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    SettingsSection("Conversation")
                    DebugValue("State", info.lifecycle)
                    DebugValue("Epoch", info.epoch.toString())
                    info.memberCount?.let { DebugValue("MLS members", it.toString()) }
                    info.adminCount?.let { DebugValue("Admins", it.toString()) }
                    info.currentRole?.let { DebugValue("Your role", it) }
                    DebugValue("Event kinds", "${info.requiredEventKinds.size} required")
                    CopyableDebugValue("MLS group ID", info.mlsGroupId) { copyToClipboard(context, "MLS group ID", info.mlsGroupId) }
                    CopyableDebugValue("Nostr group ID", info.nostrGroupId) { copyToClipboard(context, "Nostr group ID", info.nostrGroupId) }
                    SettingsSection("Delivery & notifications")
                    DebugValue("Chat relays", info.relayCount.toString())
                    DebugValue("Notifications", if (info.push.notificationsEnabled) "On" else "Off")
                    DebugValue("Push", info.push.registrationStatus)
                    if (info.push.staleTokenCount > 0) DebugValue("Push tokens", "${info.push.staleTokenCount} stale")
                    if (info.push.missingRelayHintCount > 0) DebugValue("Relay hints", "${info.push.missingRelayHintCount} missing")
                    SettingsSection("Diagnostics")
                    SettingsLink("Diagnostics", "Copy a sanitized summary or inspect events", onDiagnostics)
                }
            }
        }
    }
}

@Composable
private fun DebugUnavailable(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(detail)
    }
}

@Composable
private fun DebugValue(label: String, value: String) {
    ListItem(headlineContent = { Text(label) }, trailingContent = { Text(value) })
    HorizontalDivider()
}

@Composable
private fun CopyableDebugValue(label: String, value: String, onCopy: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(shorten(value), fontFamily = FontFamily.Monospace) },
        modifier = Modifier.clickable(onClick = onCopy),
    )
    HorizontalDivider()
}

private fun shorten(value: String): String =
    if (value.length <= 22) value else "${value.take(12)}…${value.takeLast(6)}"

private fun fileSize(bytes: Int): String = when {
    bytes == 0 -> "0 B"
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}
