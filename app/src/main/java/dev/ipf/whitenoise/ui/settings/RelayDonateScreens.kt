package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileRelay
import dev.ipf.whitenoise.model.ProfileSettingsPolicy
import dev.ipf.whitenoise.model.RelayConnectionStatus
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import kotlinx.coroutines.delay

@Composable
fun ProfileRelaysScreen(
    profile: Profile,
    onBack: () -> Unit,
    onRelay: (String) -> Unit,
    onAdd: (String, Set<RelayRole>) -> Boolean,
    onConnected: (String) -> Boolean,
    onRestore: () -> Boolean,
) {
    var addDialog by remember { mutableStateOf(false) }
    var restoreDialog by remember { mutableStateOf(false) }
    val connectingCustomRelayIds = profile.settings.relays.filter {
        it.id.startsWith("custom-") && it.status == RelayConnectionStatus.Reconnecting
    }.map(ProfileRelay::id)
    LaunchedEffect(connectingCustomRelayIds) {
        connectingCustomRelayIds.forEach { relayId ->
            delay(1_500)
            onConnected(relayId)
        }
    }
    SettingsScaffold(title = "Relays", onBack = onBack) {
        SettingsList {
            item {
                val recovery = ProfileRelayFixtures.recoverySummary(profile.settings.relays)
                if (recovery != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Profile relays need attention", color = MaterialTheme.colorScheme.tertiary)
                        Text(recovery, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    SettingsExplainer("Relays let your profile publish information, receive chat invitations, and deliver messages.")
                }
            }
            item { SettingsSection("Profile relays") }
            items(profile.settings.relays.size, key = { profile.settings.relays[it].id }) { index ->
                val relay = profile.settings.relays[index]
                SettingsLink(
                    title = relay.name,
                    subtitle = "${relay.url}${if (relay.isReadOnly) " (Read Only)" else ""} · ${relay.status.label}",
                    onClick = { onRelay(relay.id) },
                )
            }
            item {
                Button(onClick = { addDialog = true }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Add relay")
                }
                TextButton(
                    onClick = { restoreDialog = true },
                    enabled = profile.settings.relays != ProfileRelayFixtures.defaults,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Restore default relays")
                }
            }
        }
    }
    if (addDialog) {
        RelayInputDialog(onDismiss = { addDialog = false }) { value, roles ->
            if (onAdd(value, roles)) addDialog = false
        }
    }
    if (restoreDialog) {
        AlertDialog(
            onDismissRequest = { restoreDialog = false },
            title = { Text("Restore default relays?") },
            text = { Text("Custom profile relays and role changes will be replaced by the seven defaults.") },
            confirmButton = {
                TextButton(onClick = {
                    onRestore()
                    restoreDialog = false
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun ProfileRelayDetailsScreen(
    relay: ProfileRelay,
    onBack: () -> Unit,
    onSetRole: (RelayRole, Boolean) -> Boolean,
    onRemove: () -> Boolean,
) {
    var removeDialog by remember { mutableStateOf(false) }
    SettingsScaffold(title = relay.name, onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ListItem(headlineContent = { Text("Name") }, trailingContent = { Text(relay.name) })
            ListItem(
                headlineContent = { Text("URL") },
                supportingContent = { Text(relay.url + if (relay.isReadOnly) " (Read Only)" else "") },
            )
            ListItem(
                headlineContent = { Text("Status") },
                trailingContent = { Text(relay.status.label) },
            )
            HorizontalDivider()
            SettingsSection("Roles")
            RelayRole.entries.forEach { role ->
                SettingsSwitch(
                    title = role.label,
                    checked = role in relay.roles,
                    enabled = !relay.isReadOnly,
                    onCheckedChange = { onSetRole(role, it) },
                    subtitle = when (role) {
                        RelayRole.Profile -> "Publish and discover profile metadata."
                        RelayRole.Inbox -> "Receive invitations and incoming events."
                        RelayRole.ChatMessages -> "Use for newly created chats and support."
                    },
                )
            }
            SettingsExplainer(
                if (relay.isReadOnly) "This relay is read only, so this profile can’t use it to send data."
                else "Role changes are immediate. A disconnected relay cannot make new chats available.",
            )
            if (!relay.isReadOnly) {
                OutlinedButton(
                    onClick = { removeDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text("Remove Relay", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (removeDialog) {
        AlertDialog(
            onDismissRequest = { removeDialog = false },
            title = { Text("Remove ${relay.name}?") },
            text = { Text("This profile will stop using this relay. Existing chats will keep their own relay configuration.") },
            confirmButton = {
                TextButton(onClick = {
                    if (onRemove()) onBack()
                    removeDialog = false
                }) { Text("Remove Relay", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RelayInputDialog(onDismiss: () -> Unit, onAdd: (String, Set<RelayRole>) -> Unit) {
    var value by rememberSaveable { mutableStateOf("wss://") }
    var roles by remember { mutableStateOf(RelayRole.entries.toSet()) }
    var submitted by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add relay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value, { value = it }, label = { Text("Secure relay URL") }, singleLine = true)
                RelayRole.entries.forEach { role ->
                    SettingsSwitch(
                        title = role.label,
                        checked = role in roles,
                        onCheckedChange = { selected ->
                            roles = if (selected) roles + role else roles - role
                        },
                    )
                }
                if (submitted) Text("Enter a unique wss:// relay URL.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submitted = true; onAdd(value, roles) },
                enabled = roles.isNotEmpty(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private enum class DonationMethod(val label: String) {
    Lightning("Lightning"),
    Bitcoin("Bitcoin"),
}

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val method = DonationMethod.entries[selected]
    val value = when (method) {
        DonationMethod.Lightning -> "lnurl1dp68gurn8ghj7mrww4exctnrdakj7mrww4exctn0d3sk6urvv5hxxmmd9ashq6f0wcc"
        DonationMethod.Bitcoin -> "bc1q2z9k7x5m3v8c4n6p1s7h9d2f5j8a3e6u4w7r9t"
    }
    SettingsScaffold(title = "Donate", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Support White Noise", style = MaterialTheme.typography.headlineSmall)
            Text("White Noise is free and open source. Donations help us improve it and keep it available to everyone.")
            PrimaryTabRow(selectedTabIndex = selected) {
                DonationMethod.entries.forEachIndexed { index, candidate ->
                    Tab(
                        selected = index == selected,
                        onClick = { selected = index },
                        text = { Text(candidate.label) },
                    )
                }
            }
            ProfileCode(value, Modifier.fillMaxWidth().padding(24.dp))
            Text(
                when (method) {
                    DonationMethod.Lightning -> "Lightning Address"
                    DonationMethod.Bitcoin -> "Bitcoin Silent Payment"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(value, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { copyToClipboard(context, "${method.label} donation address", value) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Copy ${method.label} address") }
        }
    }
}
