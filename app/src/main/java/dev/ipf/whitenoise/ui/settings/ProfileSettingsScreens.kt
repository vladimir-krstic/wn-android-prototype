package dev.ipf.whitenoise.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ProfileKeyFixtures
import dev.ipf.whitenoise.model.ProfileSettingsPolicy
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ProfileSwitcherSheet
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
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
    var switcherOpen by remember(initiallyShowSwitcher) { mutableStateOf(initiallyShowSwitcher) }
    var signOutOpen by remember { mutableStateOf(false) }
    SettingsScaffold(title = "Settings", onBack = onBack) {
        SettingsList {
            item {
                ListItem(
                    headlineContent = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(profile.nostrAddress.ifBlank { profile.shortPublicKey }) },
                    leadingContent = {
                        ProfileAvatar(profile.name, profile.avatar, Modifier.size(56.dp), contentDescription = null)
                    },
                    trailingContent = { Text("Edit") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onShareConnect, modifier = Modifier.weight(1f)) {
                        Text("Share & Connect")
                    }
                    OutlinedButton(onClick = { switcherOpen = true }, modifier = Modifier.weight(1f)) {
                        Text("Switch profile")
                    }
                }
                HorizontalDivider()
            }
            item { SettingsSection("Profile") }
            item {
                SettingsLink(
                    "Profile",
                    if (ProfileSettingsPolicy.canPublishProfile(profile.settings)) {
                        "Name, address, about, and photo"
                    } else {
                        "Choose a connected Profile relay to edit"
                    },
                    onEditProfile,
                    enabled = ProfileSettingsPolicy.canPublishProfile(profile.settings),
                )
            }
            item { SettingsLink("Profile Keys", "View, copy, and export your keys", onProfileKeys) }
            item { SettingsLink("Manage Profiles", "Remove another stored profile", onManageProfiles) }
            item { SettingsSection("Preferences") }
            item { SettingsLink("Notifications", "Local and native push preferences", onNotifications) }
            item { SettingsLink("Appearance", profile.settings.appearance.label, onAppearance) }
            item { SettingsLink("Privacy & Security", "Device protection and auto-lock", onPrivacy) }
            item { SettingsLink("Data Usage", "Downloads and sent-media quality", onDataUsage) }
            item { SettingsLink("Relays", "${profile.settings.relays.size} profile relays", onRelays) }
            item { SettingsSection("Help") }
            item { SettingsLink("Chat with support", "A unique local support conversation", onSupport) }
            item { SettingsLink("Donate", "Lightning or Bitcoin", onDonate) }
            item { SettingsLink("Developer Tools", "Development and testing only", onDeveloperTools) }
            item { SettingsSection("Session") }
            item { SettingsLink("Sign Out", "End this profile’s session", onClick = { signOutOpen = true }) }
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
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Share your profile", style = MaterialTheme.typography.headlineSmall)
            ProfileCode(profile.publicKey, Modifier.fillMaxWidth(0.65f))
            Text(profile.name, style = MaterialTheme.typography.titleLarge)
            Text(profile.nostrAddress.ifBlank { profile.shortPublicKey }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { copyToClipboard(context, "Public key", profile.publicKey) }, modifier = Modifier.fillMaxWidth()) {
                Text("Copy public key")
            }
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Connect with ${profile.name} on White Noise:\n${profile.publicKey}")
                    }
                    context.startActivity(Intent.createChooser(send, "Share profile"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Share with Android") }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Connect with someone", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = ::startScan,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Scan profile code") }
            OutlinedButton(onClick = { foundProfile = demoFoundProfile() }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Quill")
            }
            scannerError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "Scanning uses the Android system-delivered code scanner and does not request camera permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var name by rememberSaveable(profile.id) { mutableStateOf(profile.name) }
    var about by rememberSaveable(profile.id) { mutableStateOf(profile.about) }
    var address by rememberSaveable(profile.id) { mutableStateOf(profile.nostrAddress) }
    var avatar by remember(profile.id) { mutableStateOf(profile.avatar) }
    var photoMenu by remember { mutableStateOf(false) }
    var webPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun prepare(uri: Uri) {
        scope.launch {
            val bytes = runCatching { AvatarImageProcessor.prepare(context.contentResolver, uri) }.getOrNull()
            if (bytes == null) error = "This image could not be prepared." else avatar = ProfileAvatar.DeviceImage(bytes)
        }
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(::prepare) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(::prepare) }

    SettingsScaffold(title = "Profile", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileAvatar(name, avatar, Modifier.size(112.dp))
            Box {
                TextButton(onClick = { photoMenu = true }) { Text("Change photo") }
                DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                    DropdownMenuItem(text = { Text("Choose photos") }, onClick = {
                        photoMenu = false
                        photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    DropdownMenuItem(text = { Text("Choose files") }, onClick = {
                        photoMenu = false
                        files.launch(arrayOf("image/*"))
                    })
                    DropdownMenuItem(text = { Text("Find web image") }, onClick = {
                        photoMenu = false
                        webPicker = true
                    })
                    DropdownMenuItem(text = { Text("Remove photo", color = MaterialTheme.colorScheme.error) }, onClick = {
                        photoMenu = false
                        avatar = ProfileAvatar.Monogram
                    })
                }
            }
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Verified Nostr Address") }, singleLine = true)
            Text(
                if (profile.isNostrAddressVerified && address == profile.nostrAddress) "Verified address" else "Save a valid name@domain address to verify it.",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(about, { about = it }, Modifier.fillMaxWidth(), label = { Text("About") }, minLines = 3, maxLines = 6)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val detailsSaved = onSave(name, about, avatar)
                    val addressSaved = address == profile.nostrAddress || onSaveAddress(address)
                    if (detailsSaved || addressSaved) onBack() else error = "Enter a name and a valid address."
                },
                enabled = name.isNotBlank() && ProfileSettingsPolicy.isValidNostrAddress(address),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
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
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(exportContent) } }
    }
    SettingsScaffold(title = "Profile Keys", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Public key", style = MaterialTheme.typography.titleMedium)
            Text(
                "Share this key so people can find and connect with you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KeyValue(profile.publicKey, profile.publicKey)
            OutlinedButton(onClick = { copyToClipboard(context, "Public key", profile.publicKey) }, modifier = Modifier.fillMaxWidth()) {
                Text("Copy public key")
            }
            HorizontalDivider()
            Text("Private key", style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep this key private. Anyone with it can use your profile, and White Noise can’t recover it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KeyValue(
                value = if (showPrivate) ProfileKeyFixtures.PRIVATE_KEY else "••••••••••••••••••••••••••••••••",
                accessibilityText = if (showPrivate) "Private key revealed. Use the copy action to retrieve it." else "Private key hidden",
            )
            OutlinedButton(onClick = { showPrivate = !showPrivate }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showPrivate) "Hide private key" else "Show private key")
            }
            OutlinedButton(
                onClick = { copyToClipboard(context, "Private key", ProfileKeyFixtures.PRIVATE_KEY) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Copy private key") }
            Button(
                onClick = { passwordDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export Encrypted Private Key") }
            OutlinedButton(onClick = { rawExportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Export Private Key")
            }
            Text(
                "Exports use Android’s document picker. Keep exported key files private.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation())
                    Text("Use at least 8 characters.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ProfileSettingsPolicy.isValidExportPassword(password, confirmation),
                    onClick = {
                        exportContent = ProfileKeyFixtures.encryptedExport(profile, password)
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
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityText }
            .padding(12.dp),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun ProfileCode(value: String, modifier: Modifier = Modifier) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surface
    val matrix = remember(value) { qrMatrix(value) }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "Profile QR code" },
    ) {
        drawRect(background)
        val cell = size.minDimension / matrix.width
        for (row in 0 until matrix.height) {
            for (column in 0 until matrix.width) {
                if (matrix[column, row]) {
                    drawRect(foreground, topLeft = androidx.compose.ui.geometry.Offset(column * cell, row * cell), size = androidx.compose.ui.geometry.Size(cell, cell))
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
