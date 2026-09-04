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
import dev.ipf.whitenoise.state.RelayPublicationController
import kotlinx.coroutines.delay

@Composable
fun ProfileRelaysScreen(
    profile: Profile,
    onBack: () -> Unit,
    onRelay: (String) -> Unit,
    onAdd: (String, Set<RelayRole>) -> Boolean,
    onConnected: (String) -> Boolean,
    onRestore: () -> Boolean,
    publication: RelayPublicationController? = null,
    publicationSurface: String = "relays",
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
    publication?.let { RelayPublicationHost(profile.id, publicationSurface, it) }
    SettingsScaffold(
        title = stringResource(R.string.relays),
        onBack = onBack,
    ) {
        SettingsList {
            val importedIssues = ProfileRelayFixtures.importedAddressesNeedingAttention(profile.settings.relays)
            if (importedIssues.isNotEmpty()) {
                item { SettingsCallout(
                    title = stringResource(R.string.relay_imported_issue_title),
                    text = stringResource(R.string.relay_imported_issue_detail),
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Section).testTag("relays.imported.issue"),
                ) }
            }
            val recovery = ProfileRelayFixtures.recoverySummary(profile.settings.relays)
            if (recovery != null) {
                item {
                    SettingsCallout(
                        title = stringResource(R.string.ui_profile_relays_need_attention),
                        text = recovery,
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    )
                }
            }
            if (publication != null) {
                item { SettingsSection(stringResource(R.string.relay_lists_section)) }
                item { SettingsGroup(Modifier.testTag("relay.publication.group")) {
                    RelayPublicationRows(profile, publication)
                } }
                item { SettingsExplainer(when (publication.projection(profile).phase) {
                    dev.ipf.whitenoise.model.RelayProjectionPhase.Published -> stringResource(R.string.relay_lists_published_help)
                    dev.ipf.whitenoise.model.RelayProjectionPhase.Missing -> stringResource(R.string.relay_lists_missing_help)
                    dev.ipf.whitenoise.model.RelayProjectionPhase.Unavailable -> stringResource(R.string.relay_lists_unavailable_help)
                }) }
            }
            item { SettingsSection(stringResource(R.string.ui_profile_relays)) }
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
                        title = stringResource(R.string.add_relay),
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
                    stringResource(R.string.ui_relays_let_your_profile_publish_information_receive_ch),
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
                ) { Text(stringResource(R.string.restore_default_relays)) }
            }
            item {
                SettingsExplainer(
                    if (profile.settings.relays == ProfileRelayFixtures.defaults) {
                        stringResource(R.string.relay_defaults_in_use)
                    } else {
                        stringResource(R.string.relay_restore_help)
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
            title = { Text(stringResource(R.string.ui_restore_default_relays)) },
            text = {
                Text(
                    stringResource(R.string.relay_restore_detail),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRestore()
                    restoreDialog = false
                }) { Text(stringResource(R.string.ui_restore_defaults), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { restoreDialog = false }) { Text(stringResource(R.string.cancel)) } },
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
    SettingsScaffold(title = stringResource(R.string.ui_relay), onBack = onBack) {
        SettingsList {
            item {
                SettingsGroup(
                    modifier = Modifier
                        .padding(top = WhiteNoiseSpacing.Section)
                        .testTag("relay.details.metadata"),
                ) {
                    RelayMetadataRow(stringResource(R.string.name), relay.name)
                    SettingsDivider()
                    RelayMetadataRow(
                        title = stringResource(R.string.url),
                        value = relay.url,
                    )
                    SettingsDivider()
                    RelayMetadataRow(
                        title = stringResource(R.string.message_status),
                        value = relayStatusLabel(relay.status),
                        status = relay.status,
                    )
                }
            }
            item { SettingsSection(stringResource(R.string.ui_use_for)) }
            item {
                SettingsGroup(modifier = Modifier.testTag("relay.details.roles")) {
                    RelayRole.entries.forEachIndexed { index, role ->
                        SettingsSwitch(
                            title = relayRoleLabel(role),
                            checked = role in relay.roles,
                            enabled = !relay.isReadOnly,
                            onCheckedChange = { onSetRole(role, it) },
                            subtitle = when (role) {
                                RelayRole.Profile -> stringResource(R.string.relay_role_profile_help)
                                RelayRole.Inbox -> stringResource(R.string.relay_role_inbox_help)
                                RelayRole.ChatMessages -> stringResource(R.string.relay_role_chat_messages_help)
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
                    stringResource(R.string.ui_this_relay_is_read_only_so_this_profile_cant_use_it_to),
                )
            }
            if (!relay.isReadOnly) {
                item {
                    SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                        SettingsAction(
                            title = stringResource(R.string.remove_relay),
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
            title = { Text(stringResource(R.string.remove_named_relay, relay.name)) },
            text = {
                Text(stringResource(R.string.ui_this_profile_will_stop_using_this_relay_existing_chats))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onRemove()) onBack()
                    removeDialog = false
                }) { Text(stringResource(R.string.remove_relay), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun RelayListRow(relay: ProfileRelay, onClick: () -> Unit) {
    val needsAttention = stringResource(R.string.relay_needs_attention)
    val readOnly = stringResource(R.string.read_only)
    val status = relayStatusLabel(relay.status)
    val capability = buildString {
        if (relay.isReadOnly) append(" · $readOnly")
        if (ProfileRelayFixtures.importedAddressNeedsAttention(relay)) append(" · $needsAttention")
    }
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
                    append(status)
                    if (relay.isReadOnly) append(", $readOnly")
                    if (ProfileRelayFixtures.importedAddressNeedsAttention(relay)) append(", $needsAttention")
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
            WhiteNoiseSheetHeader(title = stringResource(R.string.add_relay), onClose = onDismiss)
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
                    label = { Text(stringResource(R.string.relay_url)) },
                    placeholder = { Text("wss://relay.example.com") },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = duplicate || rejected,
                    errorMessage = stringResource(R.string.relay_unique_url_error),
                    supportingText = {
                        Text(
                            if (duplicate || rejected) {
                                stringResource(R.string.relay_unique_url_error)
                            } else {
                                stringResource(R.string.relay_url_help)
                            },
                        )
                    },
                )
                SettingsSection(stringResource(R.string.ui_use_for))
                SettingsGroup(modifier = Modifier.testTag("relay.add.roles")) {
                    RelayRole.entries.forEachIndexed { index, role ->
                        SettingsSwitch(
                            title = relayRoleLabel(role),
                            checked = role in roles,
                            onCheckedChange = { selected ->
                                roles = if (selected) roles + role else roles - role
                                rejectedValue = null
                            },
                            subtitle = when (role) {
                                RelayRole.Profile -> stringResource(R.string.relay_role_profile_help)
                                RelayRole.Inbox -> stringResource(R.string.relay_role_inbox_help)
                                RelayRole.ChatMessages -> stringResource(R.string.relay_role_chat_messages_help)
                            },
                        )
                        if (index != RelayRole.entries.lastIndex) SettingsDivider()
                    }
                }
                if (roles.isEmpty()) {
                    SettingsExplainer(stringResource(R.string.ui_choose_at_least_one_role))
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
                ) { Text(stringResource(R.string.add_relay)) }
            }
        }
    }
}

private enum class DonationMethod { Lightning, Bitcoin }

private val DonationIdentityGap = 1.dp
private val DonationCaptionPullUp = 4.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var copiedMethod by rememberSaveable { mutableIntStateOf(-1) }
    val method = DonationMethod.entries[selected]
    val methodLabel = donationMethodLabel(method)
    val donationAddress = stringResource(R.string.donation_address, methodLabel)
    val donationQrDescription = stringResource(R.string.donation_qr_code, methodLabel)
    val copyDonationAddress = stringResource(R.string.donation_copy_address, methodLabel)
    val donationAddressCopied = stringResource(R.string.donation_address_copied, methodLabel)
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
                            ) { Text(donationMethodLabel(candidate)) }
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
                            stringResource(R.string.ui_support_white_noise),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            stringResource(R.string.ui_white_noise_is_free_and_open_source_donations_help_us_),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IdentityQrCodeSurface(
                        value = value,
                        availableWidth = availableWidth,
                        contentDescription = donationQrDescription,
                        testTag = "donate.qr_surface",
                        modifier = Modifier.padding(top = 40.dp),
                    )
                    IdentifierCopyCapsule(
                        value = value,
                        copied = copiedMethod == selected,
                        onCopy = {
                            copyToClipboard(context, donationAddress, value)
                            copiedMethod = selected
                        },
                        copyContentDescription = copyDonationAddress,
                        copiedContentDescription = donationAddressCopied,
                        notCopiedStateDescription = stringResource(R.string.not_copied),
                        copiedStateDescription = stringResource(R.string.copied),
                        targetTestTag = "donate.copy_address",
                        visualTestTag = "donate.copy_address.visual",
                        modifier = Modifier.padding(top = DonationIdentityGap),
                    )
                    Text(
                        text = when (method) {
                            DonationMethod.Lightning -> stringResource(R.string.donation_lightning_address)
                            DonationMethod.Bitcoin -> stringResource(R.string.donation_bitcoin_silent_payment)
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

@Composable
private fun relayRoleLabel(role: RelayRole): String = stringResource(
    when (role) {
        RelayRole.Profile -> R.string.ui_profile
        RelayRole.Inbox -> R.string.relay_role_inbox
        RelayRole.ChatMessages -> R.string.relay_role_chat_messages
    },
)

@Composable
private fun relayStatusLabel(status: RelayConnectionStatus): String = stringResource(
    when (status) {
        RelayConnectionStatus.Connected -> R.string.relay_status_connected
        RelayConnectionStatus.Reconnecting -> R.string.relay_status_reconnecting
        RelayConnectionStatus.Disconnected -> R.string.relay_status_disconnected
    },
)

@Composable
private fun donationMethodLabel(method: DonationMethod): String = stringResource(
    when (method) {
        DonationMethod.Lightning -> R.string.donation_lightning
        DonationMethod.Bitcoin -> R.string.donation_bitcoin
    },
)
