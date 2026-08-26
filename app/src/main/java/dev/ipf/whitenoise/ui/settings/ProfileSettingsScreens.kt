package dev.ipf.whitenoise.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
    onDeveloperTools: () -> Unit,
    onSignOut: (Boolean) -> Unit,
    initiallyShowSwitcher: Boolean = false,
) {
    val profile = uiState.activeProfile ?: return
    val canEditProfile = ProfileSettingsPolicy.canPublishProfile(profile.settings)
    val management = profileManagementPresentation(
        profiles = uiState.signedInProfiles,
        activeProfileId = uiState.activeProfileId,
    )
    val alternateProfiles = profileSwitcherPresentation(
        profiles = uiState.signedInProfiles,
        activeProfileId = uiState.activeProfileId,
    ).filterNot { it.isActive }
    var profileCardExpanded by rememberSaveable(profile.id) { mutableStateOf(false) }
    var switcherOpen by remember(initiallyShowSwitcher) { mutableStateOf(initiallyShowSwitcher) }
    var signOutOpen by remember { mutableStateOf(false) }
    BackHandler(
        enabled = profileCardExpanded && management != ProfileManagementPresentation.Add,
    ) {
        profileCardExpanded = false
    }
    SettingsScaffold(
        title = "Settings",
        onBack = onBack,
        prominentTitle = true,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBarScrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        SettingsList {
            item {
                SettingsProfileHeader(
                    profile = profile,
                    management = management,
                    alternateProfiles = alternateProfiles,
                    expanded = profileCardExpanded && management != ProfileManagementPresentation.Add,
                    onShareConnect = onShareConnect,
                    onProfileManagement = {
                        if (management == ProfileManagementPresentation.Add) {
                            onAddProfile()
                        } else {
                            profileCardExpanded = !profileCardExpanded
                        }
                    },
                    onSelectProfile = {
                        profileCardExpanded = false
                        onSelectProfile(it)
                    },
                    onAddProfile = {
                        profileCardExpanded = false
                        onAddProfile()
                    },
                )
            }
            item {
                SettingsGroup(
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.FormField),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    SettingsHubLink(
                        title = "Profile",
                        icon = R.drawable.ic_settings_account_circle,
                        iconTag = "profile",
                        onClick = onEditProfile,
                        subtitle = if (canEditProfile) null else "Choose a connected Profile relay to edit",
                        enabled = canEditProfile,
                    )
                    SettingsDivider(Modifier.testTag("settings.destinations.divider.0"))
                    SettingsHubLink(
                        title = "Profile Keys",
                        icon = R.drawable.ic_settings_key,
                        iconTag = "profile_keys",
                        onClick = onProfileKeys,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = "Notifications",
                        icon = R.drawable.ic_settings_notifications,
                        iconTag = "notifications",
                        onClick = onNotifications,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = "Appearance",
                        icon = R.drawable.ic_settings_contrast,
                        iconTag = "appearance",
                        onClick = onAppearance,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = "Privacy & Security",
                        icon = R.drawable.ic_settings_front_hand,
                        iconTag = "privacy_security",
                        onClick = onPrivacy,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = "Data Usage",
                        icon = R.drawable.ic_settings_hard_drive,
                        iconTag = "data_usage",
                        onClick = onDataUsage,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = "Relays",
                        icon = R.drawable.ic_settings_cell_tower,
                        iconTag = "relays",
                        onClick = onRelays,
                    )
                }
            }
            item {
                SettingsGroup(
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    SettingsHubLink(
                        title = "Chat with support",
                        icon = R.drawable.ic_settings_chat_bubble_outline,
                        iconTag = "support",
                        onClick = onSupport,
                    )
                    SettingsDivider(Modifier.testTag("settings.help.divider.0"))
                    SettingsHubLink(
                        title = "Donate",
                        icon = R.drawable.ic_settings_favorite_border,
                        iconTag = "donate",
                        onClick = onDonate,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = "Developer Tools",
                        icon = R.drawable.ic_settings_handyman,
                        iconTag = "developer_tools",
                        onClick = onDeveloperTools,
                    )
                }
            }
            item {
                SettingsGroup(
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    SettingsHubLink(
                        title = "Sign Out",
                        icon = R.drawable.ic_settings_logout,
                        iconTag = "sign_out",
                        onClick = { signOutOpen = true },
                        destructive = true,
                    )
                }
            }
            item { SettingsVersionFooter("0.1") }
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
    management: ProfileManagementPresentation,
    alternateProfiles: List<ProfileSwitcherPresentation>,
    expanded: Boolean,
    onShareConnect: () -> Unit,
    onProfileManagement: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
) {
    val shareDescription = stringResource(R.string.open_share_connect_for, profile.name)
    Column(
        modifier = Modifier
            .padding(vertical = WhiteNoiseSpacing.Related)
            .testTag("settings.profile_group"),
    ) {
        SettingsGroup(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
            ListItem(
                headlineContent = {
                    Text(
                        text = profile.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text(
                        text = profile.shortPublicKey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    ProfileAvatar(
                        name = profile.name,
                        avatar = profile.avatar,
                        modifier = Modifier.size(56.dp),
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_qr_code_2),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShareConnect)
                    .testTag("settings.active_profile")
                    .semantics(mergeDescendants = true) {
                        contentDescription = shareDescription
                        role = Role.Button
                    },
            )
            SettingsDivider(Modifier.testTag("settings.profile.divider"))
            ProfileManagementRow(
                presentation = management,
                expanded = expanded,
                onClick = onProfileManagement,
            )
            AnimatedVisibility(
                visible = expanded && management != ProfileManagementPresentation.Add,
            ) {
                Column {
                    SettingsDivider()
                    alternateProfiles.forEachIndexed { index, alternate ->
                        InlineAlternateProfileRow(
                            presentation = alternate,
                            onClick = { onSelectProfile(alternate.profile.id) },
                        )
                        if (index < alternateProfiles.lastIndex) {
                            SettingsDivider()
                        }
                    }
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.add_profile)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_person_add),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAddProfile)
                            .testTag("settings.profile.add_profile")
                            .semantics { role = Role.Button },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileManagementRow(
    presentation: ProfileManagementPresentation,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val isAdd = presentation == ProfileManagementPresentation.Add
    ListItem(
        headlineContent = {
            Text(
                text = if (isAdd) {
                    stringResource(R.string.add_profile)
                } else {
                    stringResource(R.string.switch_profile)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            if (isAdd) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_person_add),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                when (presentation) {
                    ProfileManagementPresentation.Add -> Unit
                    is ProfileManagementPresentation.SingleAlternate -> ProfileAvatar(
                        name = presentation.profile.name,
                        avatar = presentation.profile.avatar,
                        modifier = Modifier.size(32.dp),
                        contentDescription = null,
                    )
                    is ProfileManagementPresentation.MultipleAlternates -> ProfilePreviewStack(
                        profiles = presentation.previewProfiles,
                        remainingCount = presentation.remainingCount,
                    )
                }
            }
        },
        trailingContent = if (isAdd) {
            null
        } else {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("settings.profile_management")
            .semantics { role = Role.Button },
    )
}

@Composable
private fun InlineAlternateProfileRow(
    presentation: ProfileSwitcherPresentation,
    onClick: () -> Unit,
) {
    val profile = presentation.profile
    val unreadDescription = if (presentation.unreadCount > 99) {
        stringResource(R.string.unread_count_capped)
    } else {
        pluralStringResource(
            R.plurals.unread_count,
            presentation.unreadCount,
            presentation.unreadCount,
        )
    }
    ListItem(
        headlineContent = {
            Text(
                text = profile.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = profile.shortPublicKey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            ProfileAvatar(
                name = profile.name,
                avatar = profile.avatar,
                modifier = Modifier.size(48.dp),
                contentDescription = null,
            )
        },
        trailingContent = {
            if (presentation.unreadCount > 0) {
                Badge(
                    modifier = Modifier.semantics {
                        contentDescription = unreadDescription
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(if (presentation.unreadCount > 99) "99+" else presentation.unreadCount.toString())
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("settings.profile.alternate.${profile.id}")
            .semantics { role = Role.Button },
    )
}

@Composable
private fun ProfilePreviewStack(
    profiles: List<Profile>,
    remainingCount: Int,
) {
    val avatarSpacing = 22.dp
    val visibleSlotCount = profiles.size + if (remainingCount > 0) 1 else 0
    Box(
        modifier = Modifier
            .width(32.dp + avatarSpacing * (visibleSlotCount - 1).coerceAtLeast(0))
            .height(32.dp),
    ) {
        profiles.forEachIndexed { index, alternate ->
            Surface(
                modifier = Modifier.offset(x = avatarSpacing * index),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                ProfileAvatar(
                    name = alternate.name,
                    avatar = alternate.avatar,
                    modifier = Modifier.size(32.dp),
                    contentDescription = null,
                )
            }
        }
        if (remainingCount > 0) {
            Surface(
                modifier = Modifier
                    .offset(x = avatarSpacing * profiles.size)
                    .size(32.dp)
                    .testTag("settings.profile.preview_remaining"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+$remainingCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
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
    marginModules: Int = 2,
) {
    val matrix = remember(value, marginModules) { qrMatrix(value, marginModules) }
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

internal fun qrMatrix(value: String, marginModules: Int = 2): BitMatrix = QRCodeWriter().encode(
    value,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(EncodeHintType.MARGIN to marginModules),
)

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
