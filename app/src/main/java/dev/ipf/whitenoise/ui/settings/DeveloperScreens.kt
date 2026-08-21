package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

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
                SettingsCallout(
                    title = "For development and testing only",
                    text = "These tools can expose technical information and change how the app behaves.",
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Related),
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
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.FormField)) {
                    SettingsSwitch(
                        title = "Developer Tools",
                        checked = tools.isEnabled,
                        onCheckedChange = { onEnabled(it) },
                        subtitle = "Enable technical tools for this profile.",
                    )
                }
            }
            if (tools.isEnabled) {
                item { SettingsSection("Debugging") }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            title = "Debug Mode",
                            checked = tools.debugMode,
                            onCheckedChange = { onDebugMode(it) },
                            subtitle = "Adds technical details to the accepted conversations.",
                        )
                        SettingsLink("Diagnostics", "Persistent sanitized event console", onDiagnostics)
                        SettingsLink("Key Packages", "Exactly one current package", onKeyPackages)
                    }
                }
                item { SettingsSection("Telemetry") }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            title = "Anonymous Telemetry",
                            checked = tools.anonymousTelemetry,
                            onCheckedChange = { onTelemetry(it) },
                            subtitle = "Shares anonymous reliability and performance data. It doesn’t include messages or profile keys.",
                        )
                    }
                }
                item { SettingsSection("Audit logging") }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            title = "Audit Logging",
                            checked = tools.auditLogging,
                            onCheckedChange = { onAuditLogging(it) },
                            subtitle = "Stores sanitized technical activity locally for troubleshooting.",
                        )
                    }
                }
                if (tools.auditLogging) {
                    item {
                        SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Related)) {
                            tools.auditFiles.forEach { file ->
                                ListItem(
                                    headlineContent = {
                                        Text(file.filename, fontFamily = FontFamily.Monospace)
                                    },
                                    supportingContent = {
                                        Text("${fileSize(file.byteCount)} · ${file.createdLabel} · ${file.profileName}")
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            }
                            SettingsAction(
                                title = "Clear Audit Logs",
                                subtitle = if (tools.auditLogsContainData) {
                                    "Remove recorded activity while keeping both files."
                                } else {
                                    "The audit log files are already empty."
                                },
                                onClick = { clearLogsDialog = true },
                                enabled = tools.auditLogsContainData,
                                destructive = true,
                            )
                        }
                        SettingsExplainer("Turning logging off hides the files but keeps them. Clearing removes their contents without deleting the files.")
                    }
                }
            }
            item { SettingsSection("About") }
            item {
                SettingsGroup {
                    SettingsValue("Version", "0.1 (1)")
                    SettingsValue("Built on", "MarmotKit (790eb860)")
                }
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
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(WhiteNoiseSpacing.CompactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Events", style = MaterialTheme.typography.headlineSmall)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.semantics { contentDescription = "Live event stream" },
                ) {
                    Text(
                        text = "Live",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                OutlinedButton(onClick = { onTest() }) { Text("Test") }
                OutlinedButton(onClick = { onClear() }, enabled = events.isNotEmpty()) { Text("Clear events") }
                diagnosticSummary?.let { summary ->
                    OutlinedButton(onClick = { copyToClipboard(context, "Diagnostic summary", summary) }) {
                        Text("Copy summary")
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (events.isEmpty()) {
                        WhiteNoiseEmptyState(
                            title = "No Events",
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
                            items(events, key = { it.id }) { event ->
                                Text(
                                    event.text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = WhiteNoiseSpacing.FormField),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
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
    SettingsScaffold(
        title = "Key Packages",
        onBack = onBack,
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = { onPublish() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Publish New Key Package") }
            }
        },
    ) {
        SettingsList {
            item { SettingsSection("Current key package") }
            item {
                SettingsGroup {
                    ListItem(
                        headlineContent = {
                            Text(keyPackage.id, fontFamily = FontFamily.Monospace)
                        },
                        supportingContent = {
                            Text("Published ${keyPackage.published} · ${keyPackage.size}")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            item {
                SettingsExplainer(
                    "Publishes a new deterministic key package so this profile can receive group invitations.",
                )
            }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WhiteNoiseEmptyState(
                        title = "Conversation Debugging Is Off",
                        detail = "Turn on Developer Tools and Debug Mode for this profile to inspect this chat.",
                    )
                    WhiteNoiseButton(
                        onClick = onOpenDeveloperTools,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open Developer Tools") }
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
                    item { SettingsSection("Conversation") }
                    item {
                        SettingsGroup {
                            DebugValue("State", info.lifecycle)
                            DebugValue("Epoch", info.epoch.toString())
                            info.memberCount?.let { DebugValue("MLS members", it.toString()) }
                            info.adminCount?.let { DebugValue("Admins", it.toString()) }
                            info.currentRole?.let { DebugValue("Your role", it) }
                            DebugValue("Event kinds", "${info.requiredEventKinds.size} required")
                            CopyableDebugValue("MLS group ID", info.mlsGroupId) {
                                copyToClipboard(context, "MLS group ID", info.mlsGroupId)
                            }
                            CopyableDebugValue("Nostr group ID", info.nostrGroupId) {
                                copyToClipboard(context, "Nostr group ID", info.nostrGroupId)
                            }
                        }
                    }
                    item { SettingsSection("Delivery & notifications") }
                    item {
                        SettingsGroup {
                            DebugValue("Chat relays", info.relayCount.toString())
                            DebugValue("Notifications", if (info.push.notificationsEnabled) "On" else "Off")
                            DebugValue("Push", info.push.registrationStatus)
                            if (info.push.staleTokenCount > 0) {
                                DebugValue("Push tokens", "${info.push.staleTokenCount} stale")
                            }
                            if (info.push.missingRelayHintCount > 0) {
                                DebugValue("Relay hints", "${info.push.missingRelayHintCount} missing")
                            }
                        }
                    }
                    item { SettingsSection("Diagnostics") }
                    item {
                        SettingsGroup {
                            SettingsLink("Diagnostics", "Copy a sanitized summary or inspect events", onDiagnostics)
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
