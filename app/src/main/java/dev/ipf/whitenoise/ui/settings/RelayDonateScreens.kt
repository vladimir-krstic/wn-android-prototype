package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import androidx.compose.foundation.verticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileRelay
import dev.ipf.whitenoise.model.RelayConnectionStatus
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
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
    SettingsScaffold(
        title = "Relays",
        onBack = onBack,
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = { addDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add relay") }
            }
        },
    ) {
        SettingsList {
            item {
                val recovery = ProfileRelayFixtures.recoverySummary(profile.settings.relays)
                if (recovery != null) {
                    SettingsCallout(
                        title = "Profile relays need attention",
                        text = recovery,
                    )
                } else {
                    SettingsExplainer("Relays let your profile publish information, receive chat invitations, and deliver messages.")
                }
            }
            item { SettingsSection("Profile relays") }
            item {
                SettingsGroup {
                    profile.settings.relays.forEach { relay ->
                        SettingsLink(
                            title = relay.name,
                            subtitle = "${relay.url}${if (relay.isReadOnly) " · Read only" else ""} · ${relay.status.label}",
                            onClick = { onRelay(relay.id) },
                        )
                    }
                }
            }
            item { SettingsSection("Defaults") }
            item {
                SettingsGroup {
                    SettingsAction(
                        title = "Restore default relays",
                        subtitle = if (profile.settings.relays == ProfileRelayFixtures.defaults) {
                            "The seven default relays are already in use."
                        } else {
                            "Replace custom relays and role changes with the seven defaults."
                        },
                        enabled = profile.settings.relays != ProfileRelayFixtures.defaults,
                        onClick = { restoreDialog = true },
                    )
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
        SettingsList {
            item { SettingsSection("Relay") }
            item {
                SettingsGroup {
                    SettingsValue("Name", relay.name)
                    SettingsValue("URL", relay.url + if (relay.isReadOnly) " · Read only" else "")
                    SettingsValue("Status", relay.status.label)
                }
            }
            item { SettingsSection("Roles") }
            item {
                SettingsGroup {
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
                }
            }
            item {
                SettingsExplainer(
                    if (relay.isReadOnly) {
                        "This relay is read only, so this profile can’t use it to send data."
                    } else {
                        "Role changes are immediate. A disconnected relay cannot make new chats available."
                    },
                )
            }
            if (!relay.isReadOnly) {
                item { SettingsSection("Relay access") }
                item {
                    SettingsGroup {
                        SettingsAction(
                            title = "Remove Relay",
                            subtitle = "Stop using this relay for the active profile.",
                            onClick = { removeDialog = true },
                            destructive = true,
                        )
                    }
                }
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
    val value = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState(initialText = "wss://") }
    var roles by remember { mutableStateOf(RelayRole.entries.toSet()) }
    var submitted by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add relay") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                WhiteNoiseTextField(
                    state = value,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Secure relay URL") },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    isError = submitted,
                    errorMessage = "Enter a unique wss:// relay URL.",
                    supportingText = if (submitted) {
                        { Text("Enter a unique wss:// relay URL.") }
                    } else {
                        null
                    },
                )
                RelayRole.entries.forEach { role ->
                    SettingsSwitch(
                        title = role.label,
                        checked = role in roles,
                        onCheckedChange = { selected ->
                            roles = if (selected) roles + role else roles - role
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submitted = true; onAdd(value.text.toString(), roles) },
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
    SettingsScaffold(
        title = "Donate",
        onBack = onBack,
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = { copyToClipboard(context, "${method.label} donation address", value) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Copy ${method.label} address") }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .whiteNoiseVerticalScroll(rememberScrollState())
                .padding(
                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                    vertical = WhiteNoiseSpacing.Section,
                ),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
        ) {
            Text("Support White Noise", style = MaterialTheme.typography.headlineSmall)
            Text(
                "White Noise is free and open source. Donations help us improve it and keep it available to everyone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryTabRow(selectedTabIndex = selected) {
                DonationMethod.entries.forEachIndexed { index, candidate ->
                    Tab(
                        selected = index == selected,
                        onClick = { selected = index },
                        text = { Text(candidate.label) },
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(),
                color = Color.White,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                ProfileCode(
                    value = value,
                    modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                    contentDescription = "${method.label} donation QR code",
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    Text(
                        when (method) {
                            DonationMethod.Lightning -> "Lightning Address"
                            DonationMethod.Bitcoin -> "Bitcoin Silent Payment"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
