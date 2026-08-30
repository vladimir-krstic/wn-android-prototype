package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileRelay
import dev.ipf.whitenoise.model.RelayConnectionStatus
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.WhiteNoiseFilledTonalButton
import dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold
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
    var addSheet by remember { mutableStateOf(false) }
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
    ) {
        SettingsList {
            val recovery = ProfileRelayFixtures.recoverySummary(profile.settings.relays)
            if (recovery != null) {
                item {
                    SettingsCallout(
                        title = "Profile relays need attention",
                        text = recovery,
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    )
                }
            }
            item { SettingsSection("Profile relays") }
            item {
                SettingsGroup(modifier = Modifier.testTag("relays.group")) {
                    profile.settings.relays.forEachIndexed { index, relay ->
                        RelayListRow(
                            relay = relay,
                            onClick = { onRelay(relay.id) },
                        )
                        SettingsDivider(
                            Modifier.testTag("relays.divider.$index"),
                        )
                    }
                    SettingsAction(
                        title = "Add Relay",
                        leading = {
                            Icon(
                                painterResource(R.drawable.ic_add),
                                contentDescription = null,
                            )
                        },
                        onClick = { addSheet = true },
                    )
                }
            }
            item {
                SettingsExplainer(
                    "Relays let your profile publish information, receive chat invitations, and deliver messages.",
                )
            }
            item {
                WhiteNoiseFilledTonalButton(
                    onClick = { restoreDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            top = WhiteNoiseSpacing.Section,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                        )
                        .testTag("relays.restore"),
                    enabled = profile.settings.relays != ProfileRelayFixtures.defaults,
                ) { Text("Restore Default Relays") }
            }
            item {
                SettingsExplainer(
                    if (profile.settings.relays == ProfileRelayFixtures.defaults) {
                        "The default relays and role assignments are already in use."
                    } else {
                        "Restores the default relays and role assignments for this profile."
                    },
                )
            }
        }
    }
    if (addSheet) {
        AddRelaySheet(
            existingRelays = profile.settings.relays,
            onDismiss = { addSheet = false },
        ) { value, roles ->
            onAdd(value, roles).also { added ->
                if (added) addSheet = false
            }
        }
    }
    if (restoreDialog) {
        AlertDialog(
            onDismissRequest = { restoreDialog = false },
            title = { Text("Restore default relays?") },
            text = {
                Text(
                    "This replaces this profile’s relay list and role assignments with the defaults. " +
                        "Custom relays will be removed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRestore()
                    restoreDialog = false
                }) { Text("Restore Defaults", color = MaterialTheme.colorScheme.error) }
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
    SettingsScaffold(title = "Relay", onBack = onBack) {
        SettingsList {
            item {
                SettingsGroup(
                    modifier = Modifier
                        .padding(top = WhiteNoiseSpacing.Section)
                        .testTag("relay.details.metadata"),
                ) {
                    RelayMetadataRow("Name", relay.name)
                    SettingsDivider()
                    RelayMetadataRow(
                        title = "URL",
                        value = relay.url,
                    )
                    SettingsDivider()
                    RelayMetadataRow(
                        title = "Status",
                        value = relay.status.label,
                        status = relay.status,
                    )
                }
            }
            item { SettingsSection("Use For") }
            item {
                SettingsGroup(modifier = Modifier.testTag("relay.details.roles")) {
                    RelayRole.entries.forEachIndexed { index, role ->
                        SettingsSwitch(
                            title = role.label,
                            checked = role in relay.roles,
                            enabled = !relay.isReadOnly,
                            onCheckedChange = { onSetRole(role, it) },
                            subtitle = when (role) {
                                RelayRole.Profile -> "Publish your profile and connection information."
                                RelayRole.Inbox -> "Receive invitations to new chats and groups."
                                RelayRole.ChatMessages -> {
                                    "Use for messages in chats you create. Existing chats keep their current relays."
                                }
                            },
                        )
                        if (index != RelayRole.entries.lastIndex) {
                            SettingsDivider(
                                Modifier.testTag("relay.details.role.divider.$index"),
                            )
                        }
                    }
                }
            }
            if (relay.isReadOnly) item {
                SettingsExplainer(
                    "This relay is read only, so this profile can’t use it to send data.",
                )
            }
            if (!relay.isReadOnly) {
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                        SettingsAction(
                            title = "Remove Relay",
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
            text = {
                Text("This profile will stop using this relay. Existing chats keep their current relays.")
            },
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
private fun RelayListRow(relay: ProfileRelay, onClick: () -> Unit) {
    val capability = if (relay.isReadOnly) " · Read only" else ""
    ListItem(
        supportingContent = {
            Text(
                text = relay.url + capability,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                RelayStatusIndicator(relay.status)
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                stateDescription = buildString {
                    append(relay.status.label)
                    if (relay.isReadOnly) append(", Read only")
                }
            }
            .testTag("relays.row.${relay.id}"),
    ) { Text(relay.name) }
}

@Composable
private fun RelayStatusIndicator(status: RelayConnectionStatus) {
    val connected = status == RelayConnectionStatus.Connected
    Surface(
        modifier = Modifier
            .size(RelayStatusIndicatorSize)
            .testTag(
                if (connected) "relay.status.connected.filled" else "relay.status.not_connected.filled",
            ),
        shape = CircleShape,
        color = if (connected) RelayConnectedContainer else RelayNotConnectedContainer,
        contentColor = Color.White,
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(if (connected) R.drawable.ic_check else R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(RelayStatusGlyphSize),
            )
        }
    }
}

private val RelayStatusIndicatorSize = 20.dp
private val RelayStatusGlyphSize = 14.dp
private val RelayConnectedContainer = Color(0xFF188038)
private val RelayNotConnectedContainer = Color(0xFFC5221F)

@Composable
private fun RelayMetadataRow(
    title: String,
    value: String,
    status: RelayConnectionStatus? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("relay.metadata.${title.lowercase()}")
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
    ) {
        Text(title, modifier = Modifier.widthIn(min = RelayMetadataLabelWidth))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(
                WhiteNoiseSpacing.Related,
                Alignment.End,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f, fill = false),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                textAlign = TextAlign.End,
            )
            status?.let { RelayStatusIndicator(it) }
        }
    }
}

private val RelayMetadataLabelWidth = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRelaySheet(
    existingRelays: List<ProfileRelay>,
    onDismiss: () -> Unit,
    onAdd: (String, Set<RelayRole>) -> Boolean,
) {
    val value = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    var roles by remember { mutableStateOf(RelayRole.entries.toSet()) }
    var rejectedValue by rememberSaveable { mutableStateOf<String?>(null) }
    val currentValue = value.text.toString()
    val normalized = ProfileRelayFixtures.normalize(currentValue)
    val duplicate = normalized != null && existingRelays.any {
        ProfileRelayFixtures.normalize(it.url) == normalized
    }
    val rejected = rejectedValue == currentValue
    val canAdd = normalized != null && !duplicate && roles.isNotEmpty()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            WhiteNoiseSheetHeader(title = "Add Relay", onClose = onDismiss)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                WhiteNoiseTextField(
                    state = value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                        .testTag("relay.add.url"),
                    label = { Text("Relay URL") },
                    placeholder = { Text("wss://relay.example.com") },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = duplicate || rejected,
                    errorMessage = "Enter a unique wss:// relay URL.",
                    supportingText = {
                        Text(
                            if (duplicate || rejected) {
                                "Enter a unique wss:// relay URL."
                            } else {
                                "Enter a relay URL beginning with wss://."
                            },
                        )
                    },
                )
                SettingsSection("Use For")
                SettingsGroup(modifier = Modifier.testTag("relay.add.roles")) {
                    RelayRole.entries.forEachIndexed { index, role ->
                        SettingsSwitch(
                            title = role.label,
                            checked = role in roles,
                            onCheckedChange = { selected ->
                                roles = if (selected) roles + role else roles - role
                                rejectedValue = null
                            },
                            subtitle = when (role) {
                                RelayRole.Profile -> "Publish your profile and connection information."
                                RelayRole.Inbox -> "Receive invitations to new chats and groups."
                                RelayRole.ChatMessages -> {
                                    "Use for messages in chats you create. Existing chats keep their current relays."
                                }
                            },
                        )
                        if (index != RelayRole.entries.lastIndex) SettingsDivider()
                    }
                }
                if (roles.isEmpty()) {
                    SettingsExplainer("Choose at least one role.")
                }
            }
            SettingsBottomAction(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                WhiteNoiseButton(
                    onClick = {
                        if (!onAdd(currentValue, roles)) rejectedValue = currentValue
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("relay.add.submit"),
                    enabled = canAdd,
                ) { Text("Add Relay") }
            }
        }
    }
}

private enum class DonationMethod(val label: String) {
    Lightning("Lightning"),
    Bitcoin("Bitcoin"),
}

private val DonationIdentityGap = 1.dp
private val DonationCaptionPullUp = 4.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var copiedMethod by rememberSaveable { mutableIntStateOf(-1) }
    val method = DonationMethod.entries[selected]
    val value = when (method) {
        DonationMethod.Lightning -> "lnurl1dp68gurn8ghj7mrww4exctnrdakj7mrww4exctn0d3sk6urvv5hxxmmd9ashq6f0wcc"
        DonationMethod.Bitcoin -> "bc1q2z9k7x5m3v8c4n6p1s7h9d2f5j8a3e6u4w7r9t"
    }
    LaunchedEffect(copiedMethod) {
        if (copiedMethod >= 0) {
            delay(2_000)
            copiedMethod = -1
        }
    }
    WhiteNoiseScaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .selectableGroup()
                            .testTag("donate.method_selector"),
                        horizontalArrangement = Arrangement.spacedBy(
                            ButtonGroupDefaults.ConnectedSpaceBetween,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DonationMethod.entries.forEachIndexed { index, candidate ->
                            ToggleButton(
                                checked = index == selected,
                                onCheckedChange = { selected = index },
                                shapes = if (index == 0) {
                                    ButtonGroupDefaults.connectedLeadingButtonShapes()
                                } else {
                                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                                },
                                modifier = Modifier
                                    .semantics { role = Role.RadioButton }
                                    .testTag("donate.method.$index"),
                            ) { Text(candidate.label) }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = LocalWhiteNoiseHeaderScroll.current,
            )
        },
    ) { innerPadding ->
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .whiteNoiseVerticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                val availableWidth = maxWidth
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            top = 48.dp,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                            bottom = WhiteNoiseSpacing.Section,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_favorite_border),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            "Support White Noise",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "White Noise is free and open source. Donations help us improve it and keep it available to everyone.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IdentityQrCodeSurface(
                        value = value,
                        availableWidth = availableWidth,
                        contentDescription = "${method.label} donation QR code",
                        testTag = "donate.qr_surface",
                        modifier = Modifier.padding(top = 40.dp),
                    )
                    IdentifierCopyCapsule(
                        value = value,
                        copied = copiedMethod == selected,
                        onCopy = {
                            copyToClipboard(context, "${method.label} donation address", value)
                            copiedMethod = selected
                        },
                        copyContentDescription = "Copy ${method.label} address",
                        copiedContentDescription = "${method.label} address copied",
                        notCopiedStateDescription = "Not copied",
                        copiedStateDescription = "Copied",
                        targetTestTag = "donate.copy_address",
                        visualTestTag = "donate.copy_address.visual",
                        modifier = Modifier.padding(top = DonationIdentityGap),
                    )
                    Text(
                        text = when (method) {
                            DonationMethod.Lightning -> "Lightning Address"
                            DonationMethod.Bitcoin -> "Bitcoin Silent Payment"
                        },
                        modifier = Modifier
                            .padding(top = DonationIdentityGap)
                            .offset(y = -DonationCaptionPullUp)
                            .testTag("donate.method_caption"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
