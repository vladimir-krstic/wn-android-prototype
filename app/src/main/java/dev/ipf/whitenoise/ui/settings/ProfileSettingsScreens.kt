package dev.ipf.whitenoise.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ProfileKeyFixtures
import dev.ipf.whitenoise.model.ProfileSettingsPolicy
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ProfileSwitcherSheet
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseOutlinedButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseSecureTextField
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    uiState: AppUiState,
    onBack: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
    onShareConnect: () -> Unit,
    onEditProfile: () -> Unit,
    onProfileKeys: () -> Unit,
    onNotifications: () -> Unit,
    onAppearance: () -> Unit,
    onPrivacy: () -> Unit,
    onDataUsage: () -> Unit,
    onRelays: () -> Unit,
    onSupport: () -> Unit,
    onDonate: () -> Unit,
    onManageProfiles: () -> Unit,
    onDeveloperTools: () -> Unit,
    onSignOut: (Boolean) -> Unit,
    initiallyShowSwitcher: Boolean = false,
) {
    val profile = uiState.activeProfile ?: return
    val canEditProfile = ProfileSettingsPolicy.canPublishProfile(profile.settings)
    var switcherOpen by remember(initiallyShowSwitcher) { mutableStateOf(initiallyShowSwitcher) }
    var signOutOpen by remember { mutableStateOf(false) }
    SettingsScaffold(
        title = "Settings",
        onBack = onBack,
        prominentTitle = true,
    ) {
        SettingsList {
            item {
                SettingsProfileHeader(
                    profile = profile,
                    profileCount = uiState.signedInProfiles.size,
                    onShareConnect = onShareConnect,
                    onProfileManagement = {
                        if (uiState.signedInProfiles.size > 1) {
                            switcherOpen = true
                        } else {
                            onAddProfile()
                        }
                    },
                )
            }
            item { SettingsSection("Profile") }
            item {
                SettingsGroup {
                    SettingsHubLink(
                        title = "Profile",
                        icon = R.drawable.ic_settings_profile,
                        iconTag = "profile",
                        onClick = onEditProfile,
                        subtitle = if (canEditProfile) null else "Choose a connected Profile relay to edit",
                        enabled = canEditProfile,
                    )
                    SettingsHubLink(
                        title = "Profile Keys",
                        icon = R.drawable.ic_settings_key,
                        iconTag = "profile_keys",
                        onClick = onProfileKeys,
                    )
                    SettingsHubLink(
                        title = "Manage Profiles",
                        icon = R.drawable.ic_settings_manage_accounts,
                        iconTag = "manage_profiles",
                        onClick = onManageProfiles,
                    )
                }
            }
            item { SettingsSection("Preferences") }
            item {
                SettingsGroup {
                    SettingsHubLink(
                        title = "Notifications",
                        icon = R.drawable.ic_settings_notifications,
                        iconTag = "notifications",
                        onClick = onNotifications,
                    )
                    SettingsHubLink(
                        title = "Appearance",
                        icon = R.drawable.ic_settings_palette,
                        iconTag = "appearance",
                        onClick = onAppearance,
                        value = profile.settings.appearance.label,
                    )
                    SettingsHubLink(
                        title = "Privacy & Security",
                        icon = R.drawable.ic_settings_shield,
                        iconTag = "privacy_security",
                        onClick = onPrivacy,
                    )
                    SettingsHubLink(
                        title = "Data Usage",
                        icon = R.drawable.ic_settings_data_usage,
                        iconTag = "data_usage",
                        onClick = onDataUsage,
                    )
                    SettingsHubLink(
                        title = "Relays",
                        icon = R.drawable.ic_settings_hub,
                        iconTag = "relays",
                        onClick = onRelays,
                    )
                }
            }
            item { SettingsSection("Help") }
            item {
                SettingsGroup {
                    SettingsHubLink(
                        title = "Chat with support",
                        icon = R.drawable.ic_settings_chat,
                        iconTag = "support",
                        onClick = onSupport,
                    )
                    SettingsHubLink(
                        title = "Donate",
                        icon = R.drawable.ic_settings_favorite,
                        iconTag = "donate",
                        onClick = onDonate,
                    )
                    SettingsHubLink(
                        title = "Developer Tools",
                        icon = R.drawable.ic_settings_developer_mode,
                        iconTag = "developer_tools",
                        onClick = onDeveloperTools,
                    )
                }
            }
            item { SettingsSection("Session") }
            item {
                SettingsGroup {
                    SettingsHubLink(
                        title = "Sign Out",
                        icon = R.drawable.ic_settings_logout,
                        iconTag = "sign_out",
                        onClick = { signOutOpen = true },
                        destructive = true,
                    )
                }
            }
            item { SettingsExplainer("White Noise for Android · Version 0.1") }
        }
    }
    if (switcherOpen) {
        ProfileSwitcherSheet(
            profiles = uiState.signedInProfiles,
            activeProfileId = uiState.activeProfileId,
            onDismiss = { switcherOpen = false },
            onSelectProfile = {
                onSelectProfile(it)
                switcherOpen = false
            },
            onAddProfile = {
                switcherOpen = false
                onAddProfile()
            },
        )
    }
    if (signOutOpen) {
        SignOutSheet(
            profile = profile,
            onDismiss = { signOutOpen = false },
            onComplete = onSignOut,
        )
    }
}

@Composable
private fun SettingsHubLink(
    title: String,
    @DrawableRes icon: Int,
    iconTag: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val iconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    SettingsLink(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("settings.icon.$iconTag"),
                tint = iconColor,
            )
        },
        value = value,
        enabled = enabled,
        destructive = destructive,
    )
}

@Composable
private fun SettingsProfileHeader(
    profile: Profile,
    profileCount: Int,
    onShareConnect: () -> Unit,
    onProfileManagement: () -> Unit,
) {
    val shareDescription = stringResource(R.string.open_share_connect_for, profile.name)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                vertical = WhiteNoiseSpacing.Related,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShareConnect)
                    .padding(vertical = WhiteNoiseSpacing.Related)
                    .semantics(mergeDescendants = true) {
                        contentDescription = shareDescription
                        role = Role.Button
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                ProfileAvatar(
                    name = profile.name,
                    avatar = profile.avatar,
                    modifier = Modifier.size(72.dp),
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = profile.nostrAddress.ifBlank { profile.shortPublicKey },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Share & Connect",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WhiteNoiseOutlinedButton(
                onClick = onProfileManagement,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (profileCount > 1) "Switch Profile" else "Add Profile")
            }
        }
    }
}

@Composable
fun ShareConnectScreen(profile: Profile, onBack: () -> Unit) {
    val context = LocalContext.current
    var foundProfile by remember { mutableStateOf<Profile?>(null) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .allowManualInput()
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    fun startScan() {
        scannerError = null
        scanner.startScan()
            .addOnSuccessListener { foundProfile = demoFoundProfile() }
            .addOnFailureListener { scannerError = "Scanner unavailable. Choose the profile below instead." }
    }
    SettingsScaffold(title = "Share & Connect", onBack = onBack) {
        SettingsList {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                            vertical = WhiteNoiseSpacing.Section,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    Text("Share your profile", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Let someone scan this code, or share your profile through Android.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        modifier = Modifier
                            .padding(vertical = WhiteNoiseSpacing.Related)
                            .widthIn(max = 260.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.White,
                    ) {
                        ProfileCode(
                            value = profile.publicKey,
                            modifier = Modifier.padding(WhiteNoiseSpacing.FormField),
                        )
                    }
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        profile.nostrAddress.ifBlank { profile.shortPublicKey },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    WhiteNoiseButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Connect with ${profile.name} on White Noise:\n${profile.publicKey}")
                            }
                            context.startActivity(Intent.createChooser(send, "Share profile"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Share with Android") }
                    WhiteNoiseOutlinedButton(
                        onClick = { copyToClipboard(context, "Public key", profile.publicKey) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy public key") }
                }
            }
            item { SettingsSection("Connect with someone") }
            item {
                SettingsGroup {
                    SettingsAction(
                        title = "Scan profile code",
                        subtitle = "Use Android’s system-delivered code scanner.",
                        onClick = ::startScan,
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_qr_code),
                                contentDescription = null,
                            )
                        },
                    )
                    SettingsAction(
                        title = "Open Quill",
                        subtitle = "View a profile connection result.",
                        onClick = { foundProfile = demoFoundProfile() },
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_person),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            scannerError?.let { message ->
                item { SettingsCallout(text = message, title = "Couldn’t open scanner", isError = true) }
            }
            item {
                SettingsExplainer(
                    "The system scanner does not request camera permission from White Noise.",
                )
            }
        }
    }
    foundProfile?.let { found ->
        AlertDialog(
            onDismissRequest = { foundProfile = null },
            title = { Text("Profile found") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(found.name, found.avatar, Modifier.size(72.dp), contentDescription = null)
                    Spacer(Modifier.height(12.dp))
                    Text(found.name, style = MaterialTheme.typography.titleMedium)
                    Text(found.nostrAddress)
                    Text(found.shortPublicKey, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { foundProfile = null }) { Text("Done") } },
            dismissButton = {
                TextButton(
                    onClick = {
                        foundProfile = null
                        startScan()
                    },
                ) { Text("Scan Another") }
            },
        )
    }
}

@Composable
fun EditProfileScreen(
    profile: Profile,
    onBack: () -> Unit,
    onSave: (String, String, ProfileAvatar) -> Boolean,
    onSaveAddress: (String) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val name = rememberSaveable(profile.id, saver = TextFieldState.Saver) {
        TextFieldState(initialText = profile.name)
    }
    val about = rememberSaveable(profile.id, saver = TextFieldState.Saver) {
        TextFieldState(initialText = profile.about)
    }
    val address = rememberSaveable(profile.id, saver = TextFieldState.Saver) {
        TextFieldState(initialText = profile.nostrAddress)
    }
    val nameValue = name.text.toString()
    val aboutValue = about.text.toString()
    val addressValue = address.text.toString()
    var avatar by remember(profile.id) { mutableStateOf(profile.avatar) }
    var photoMenu by remember { mutableStateOf(false) }
    var webPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPreparingPhoto by remember { mutableStateOf(false) }
    fun prepare(uri: Uri) {
        scope.launch {
            isPreparingPhoto = true
            error = null
            val bytes = runCatching { AvatarImageProcessor.prepare(context.contentResolver, uri) }.getOrNull()
            isPreparingPhoto = false
            if (bytes == null) {
                error = "This image could not be prepared."
            } else {
                avatar = ProfileAvatar.DeviceImage(bytes)
            }
        }
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(::prepare) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(::prepare) }

    SettingsScaffold(
        title = "Profile",
        onBack = onBack,
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = {
                        val detailsSaved = onSave(nameValue, aboutValue, avatar)
                        val addressSaved = addressValue == profile.nostrAddress || onSaveAddress(addressValue)
                        if (detailsSaved || addressSaved) {
                            onBack()
                        } else {
                            error = "Enter a name and a valid address."
                        }
                    },
                    enabled = nameValue.isNotBlank() &&
                        ProfileSettingsPolicy.isValidNostrAddress(addressValue) &&
                        !isPreparingPhoto,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save") }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
        ) {
            ProfileAvatar(nameValue, avatar, Modifier.size(120.dp))
            Box {
                FilledTonalButton(
                    onClick = { photoMenu = true },
                    enabled = !isPreparingPhoto,
                ) { Text(if (avatar == ProfileAvatar.Monogram) "Add photo" else "Change photo") }
                WhiteNoiseDropdownMenu(
                    expanded = photoMenu,
                    onDismissRequest = { photoMenu = false },
                    items = buildList {
                        add(WhiteNoiseMenuItem("Choose photos", icon = R.drawable.ic_image, onClick = {
                            photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }))
                        add(WhiteNoiseMenuItem("Choose files", icon = R.drawable.ic_description, onClick = {
                            files.launch(arrayOf("image/*"))
                        }))
                        add(WhiteNoiseMenuItem("Find web image", icon = R.drawable.ic_search, onClick = {
                            webPicker = true
                        }))
                        if (avatar != ProfileAvatar.Monogram) {
                            add(WhiteNoiseMenuItem(
                                "Remove photo", icon = R.drawable.ic_delete, destructive = true,
                                onClick = { avatar = ProfileAvatar.Monogram },
                            ))
                        }
                    },
                )
            }
            if (isPreparingPhoto) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Preparing photo")
                }
            }
            WhiteNoiseTextField(
                state = name,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            WhiteNoiseTextField(
                state = address,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Verified Nostr Address") },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            Text(
                if (profile.isNostrAddressVerified && addressValue == profile.nostrAddress) {
                    "Verified address"
                } else {
                    "Save a valid name@domain address to verify it."
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            WhiteNoiseTextField(
                state = about,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("About") },
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (webPicker) {
        AvatarWebImagePicker(
            currentChoiceId = (avatar as? ProfileAvatar.WebImage)?.choiceId,
            onDismiss = { webPicker = false },
            onUseImage = {
                avatar = ProfileAvatar.WebImage(it.asset, it.id)
                webPicker = false
            },
        )
    }
}

@Composable
fun ProfileKeysScreen(profile: Profile, onBack: () -> Unit) {
    val context = LocalContext.current
    var showPrivate by rememberSaveable { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf("") }
    var passwordDialog by remember { mutableStateOf(false) }
    var rawExportDialog by remember { mutableStateOf(false) }
    val password = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    val confirmation = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    val passwordValue = password.text.toString()
    val confirmationValue = confirmation.text.toString()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(exportContent) } }
    }
    SettingsScaffold(title = "Profile Keys", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Public key") }
            item {
                SettingsGroup {
                    KeyValue(profile.publicKey, profile.publicKey)
                    SettingsAction(
                        title = "Copy public key",
                        subtitle = "Share this key so people can find and connect with you.",
                        onClick = { copyToClipboard(context, "Public key", profile.publicKey) },
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            item { SettingsSection("Private key") }
            item {
                SettingsCallout(
                    title = "Keep this private",
                    text = "Anyone with this key can use your profile, and White Noise can’t recover it.",
                )
            }
            item {
                SettingsGroup(
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Related),
                ) {
                    KeyValue(
                        value = if (showPrivate) ProfileKeyFixtures.PRIVATE_KEY else "••••••••••••••••••••••••••••••••",
                        accessibilityText = if (showPrivate) {
                            "Private key revealed. Use the copy action to retrieve it."
                        } else {
                            "Private key hidden"
                        },
                    )
                    SettingsAction(
                        title = if (showPrivate) "Hide private key" else "Show private key",
                        onClick = { showPrivate = !showPrivate },
                    )
                    SettingsAction(
                        title = "Copy private key",
                        onClick = { copyToClipboard(context, "Private key", ProfileKeyFixtures.PRIVATE_KEY) },
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            item { SettingsSection("Export") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    WhiteNoiseButton(
                        onClick = { passwordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export Encrypted Private Key") }
                    WhiteNoiseOutlinedButton(
                        onClick = { rawExportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export Private Key") }
                }
            }
            item { SettingsExplainer("Exports use Android’s document picker. Keep exported key files private.") }
        }
    }
    if (rawExportDialog) {
        AlertDialog(
            onDismissRequest = { rawExportDialog = false },
            title = { Text("Export unencrypted private key?") },
            text = { Text("This file gives complete access to your profile. Store it somewhere private and never share it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportContent = ProfileKeyFixtures.rawExport(profile)
                        rawExportDialog = false
                        export.launch("${profile.id}-white-noise-key.txt")
                    },
                ) { Text("Export Unencrypted") }
            },
            dismissButton = { TextButton(onClick = { rawExportDialog = false }) { Text("Cancel") } },
        )
    }
    if (passwordDialog) {
        AlertDialog(
            onDismissRequest = { passwordDialog = false },
            title = { Text("Protect export") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WhiteNoiseSecureTextField(
                        state = password,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    WhiteNoiseSecureTextField(
                        state = confirmation,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Text("Use at least 8 characters.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ProfileSettingsPolicy.isValidExportPassword(passwordValue, confirmationValue),
                    onClick = {
                        exportContent = ProfileKeyFixtures.encryptedExport(profile, passwordValue)
                        passwordDialog = false
                        export.launch("${profile.id}-white-noise-key.wnkey.txt")
                    },
                ) { Text("Export") }
            },
            dismissButton = { TextButton(onClick = { passwordDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun KeyValue(value: String, accessibilityText: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(WhiteNoiseSpacing.Related),
    ) {
        Text(
            text = value,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = accessibilityText }
                .padding(WhiteNoiseSpacing.FormField),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun ProfileCode(
    value: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Profile QR code",
) {
    val matrix = remember(value) { qrMatrix(value) }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawRect(Color.White)
        val cell = size.minDimension / matrix.width
        for (row in 0 until matrix.height) {
            for (column in 0 until matrix.width) {
                if (matrix[column, row]) {
                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(column * cell, row * cell), size = androidx.compose.ui.geometry.Size(cell, cell))
                }
            }
        }
    }
}

internal fun qrMatrix(value: String): BitMatrix = QRCodeWriter().encode(
    value,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(EncodeHintType.MARGIN to 2),
)

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun demoFoundProfile(): Profile {
    val choice = AvatarWebImageCatalog.choices.first()
    return Profile(
        id = "open-quill-found",
        name = "Open Quill",
        publicKey = "npub1q2v9n6t4r7c3x8m5k2w9p6s4y7h3d8f5j2a9e6u4z7n1m2d9",
        nostrAddress = "open-quill@whitenoise.example",
        avatar = ProfileAvatar.WebImage(choice.asset, choice.id),
    )
}
