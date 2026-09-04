package dev.ipf.whitenoise.ui.settings

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.ipf.whitenoise.BuildConfig
import dev.ipf.whitenoise.state.AppUpdateController
import dev.ipf.whitenoise.ui.updates.AppUpdateSettingsGroup
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.ExportPasswordStrength
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ProfileKeyFixtures
import dev.ipf.whitenoise.model.ProfileKeyAccessPolicy
import dev.ipf.whitenoise.model.ProfileKeyExportKind
import dev.ipf.whitenoise.model.ProfileKeyExportRequest
import dev.ipf.whitenoise.model.ProfileSettingsPolicy
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ProfileSwitcherSheet
import dev.ipf.whitenoise.ui.components.AvatarPhotoButton
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
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    onSignOut: (dev.ipf.whitenoise.model.SignOutOptions) -> Unit,
    onFolders: () -> Unit = {},
    onReadAloud: () -> Unit = {},
    onDictation: () -> Unit = {},
    onAiAgents: () -> Unit = {},
    onHelp: () -> Unit = {},
    appUpdates: AppUpdateController? = null,
    initiallyShowSwitcher: Boolean = false,
    exitAttempt: dev.ipf.whitenoise.model.ProfileExitAttempt? = null,
    onAdvanceExit: (Long, dev.ipf.whitenoise.model.ProfileExitStep) -> Unit = { _, _ -> },
    onRetryExit: (Long) -> Unit = {},
    onDismissExit: () -> Unit = {},
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
    var signOutOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    BackHandler(
        enabled = profileCardExpanded && management != ProfileManagementPresentation.Add,
    ) {
        profileCardExpanded = false
    }
    SettingsScaffold(
        title = stringResource(R.string.ui_settings),
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
                        title = stringResource(R.string.ui_profile),
                        icon = R.drawable.ic_settings_account_circle,
                        iconTag = "profile",
                        onClick = onEditProfile,
                        subtitle = if (canEditProfile) null else stringResource(R.string.profile_relay_required_to_edit),
                        enabled = canEditProfile,
                    )
                    SettingsDivider(Modifier.testTag("settings.destinations.divider.0"))
                    SettingsHubLink(
                        title = stringResource(R.string.ui_profile_keys),
                        icon = R.drawable.ic_settings_key,
                        iconTag = "profile_keys",
                        onClick = onProfileKeys,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.ai_agents_title),
                        icon = R.drawable.ic_settings_person_add,
                        iconTag = "ai_agents",
                        onClick = onAiAgents,
                    )
                    SettingsDivider()
                    SettingsHubLink(title = stringResource(R.string.chat_folders), icon = R.drawable.ic_filter_list, iconTag = "folders", onClick = onFolders)
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.notification_controls_title),
                        icon = R.drawable.ic_settings_notifications,
                        iconTag = "notifications",
                        onClick = onNotifications,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.read_aloud), icon = R.drawable.ic_volume_up,
                        iconTag = "read_aloud", onClick = onReadAloud,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.dictation_title), icon = R.drawable.ic_mic,
                        iconTag = "dictation", onClick = onDictation,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.appearance_title),
                        icon = R.drawable.ic_settings_contrast,
                        iconTag = "appearance",
                        onClick = onAppearance,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.ui_privacy_security),
                        icon = R.drawable.ic_settings_front_hand,
                        iconTag = "privacy_security",
                        onClick = onPrivacy,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.data_usage_title),
                        icon = R.drawable.ic_settings_hard_drive,
                        iconTag = "data_usage",
                        onClick = onDataUsage,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.relays),
                        icon = R.drawable.ic_settings_cell_tower,
                        iconTag = "relays",
                        onClick = onRelays,
                    )
                }
            }
            appUpdates?.state?.let { update ->
                if (dev.ipf.whitenoise.model.AppUpdates.showsSettings(update)) {
                    item {
                        AppUpdateSettingsGroup(
                            state = update,
                            onAction = {
                                if (dev.ipf.whitenoise.model.AppUpdates.isAvailable(appUpdates.state)) {
                                    appUpdates.beginSelfUpdate()
                                } else {
                                    appUpdates.beginCheck()
                                }
                            },
                        )
                    }
                }
            }
            item {
                SettingsGroup(
                    modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    SettingsHubLink(
                        title = stringResource(R.string.help_title),
                        icon = R.drawable.ic_info,
                        iconTag = "help",
                        onClick = onHelp,
                    )
                    SettingsDivider(Modifier.testTag("settings.help.divider.help"))
                    SettingsHubLink(
                        title = stringResource(R.string.ui_chat_with_support),
                        icon = R.drawable.ic_settings_chat_bubble_outline,
                        iconTag = "support",
                        onClick = onSupport,
                    )
                    SettingsDivider(Modifier.testTag("settings.help.divider.0"))
                    SettingsHubLink(
                        title = stringResource(R.string.ui_donate),
                        icon = R.drawable.ic_settings_favorite_border,
                        iconTag = "donate",
                        onClick = onDonate,
                    )
                    SettingsDivider()
                    SettingsHubLink(
                        title = stringResource(R.string.developer_tools),
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
                        title = stringResource(R.string.ui_sign_out),
                        icon = R.drawable.ic_settings_logout,
                        iconTag = "sign_out",
                        onClick = { signOutOpen = true },
                        destructive = true,
                    )
                }
            }
            item { SettingsVersionFooter(BuildConfig.VERSION_NAME) }
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
            onDismiss = { onDismissExit(); signOutOpen = false },
            onComplete = onSignOut,
            attempt = exitAttempt,
            onAdvance = onAdvanceExit,
            onRetry = onRetryExit,
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
    onSaveDraft: ((dev.ipf.whitenoise.model.ProfileEditDraft) -> Boolean)? = null,
    saveAttempt: dev.ipf.whitenoise.model.ProfileSaveAttempt? = null,
    onAdvanceSave: (Long, dev.ipf.whitenoise.model.ProfileSavePhase) -> Boolean = { _, _ -> false },
    onCancelSave: () -> Unit = {},
    consumeImageFailure: () -> Boolean = { false },
    retainedImages: dev.ipf.whitenoise.model.ProfileImageDraft? = null,
    onRetainImages: (ProfileAvatar, ProfileAvatar?) -> Unit = { _, _ -> },
) {
    val startingDraft = saveAttempt?.draft ?: dev.ipf.whitenoise.model.ProfileEditDraft.from(profile)
    val name = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState(startingDraft.name) }
    val about = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState(startingDraft.about) }
    val address = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState(startingDraft.nostrAddress) }
    val lightning = rememberSaveable(profile.id, saver = TextFieldState.Saver) { TextFieldState(startingDraft.lightningAddress) }
    var avatar by remember(profile.id) { mutableStateOf(retainedImages?.avatar ?: startingDraft.avatar) }
    var banner by remember(profile.id) { mutableStateOf(if (retainedImages != null) retainedImages.banner else startingDraft.banner) }
    var isEditing by rememberSaveable(profile.id) { mutableStateOf(saveAttempt != null) }
    var suggestionIndex by rememberSaveable(profile.id) { mutableIntStateOf(0) }
    var imageBusy by remember(profile.id) { mutableStateOf(emptySet<String>()) }
    var error by remember(profile.id) { mutableStateOf(false) }
    var viewing by remember(profile.id) { mutableStateOf<ProfileAvatar?>(null) }
    val busy = saveAttempt?.isBusy == true
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun resetDraft() {
        name.edit { replace(0, length, profile.name) }
        about.edit { replace(0, length, profile.about) }
        address.edit { replace(0, length, profile.nostrAddress) }
        lightning.edit { replace(0, length, profile.lightningAddress) }
        avatar = profile.avatar
        banner = profile.banner
        error = false
    }
    fun beginEditing() { onCancelSave(); resetDraft(); isEditing = true }
    fun stopEditing() { onCancelSave(); resetDraft(); isEditing = false }
    fun handleBack() { if (isEditing) stopEditing() else onBack() }
    BackHandler(enabled = isEditing && viewing == null) { stopEditing() }
    LaunchedEffect(profile, isEditing) { if (!isEditing) resetDraft() }
    LaunchedEffect(saveAttempt?.id, saveAttempt?.phase, lifecycle) {
        val attempt = saveAttempt ?: return@LaunchedEffect
        if (attempt.isBusy) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(600)
            if (onAdvanceSave(attempt.id, attempt.phase)) { isEditing = false; error = false }
        }
    }
    val lightningValid = dev.ipf.whitenoise.model.LightningAddress.normalize(lightning.text.toString()) != null
    val saveFailure = saveAttempt?.failure
    val failureRes = when (saveFailure) {
        dev.ipf.whitenoise.model.ProfileSaveFailure.UnresolvedLightning -> R.string.profile_lightning_unresolved
        dev.ipf.whitenoise.model.ProfileSaveFailure.NoConnection -> R.string.profile_save_no_connection
        dev.ipf.whitenoise.model.ProfileSaveFailure.PublishFailed -> R.string.profile_publish_failed
        null -> null
    }
    SettingsScaffold(title = stringResource(R.string.ui_profile), onBack = ::handleBack, topBarActions = {
        if (!isEditing) TextButton(onClick = ::beginEditing) { Text(stringResource(R.string.message_edit)) }
    }, bottomBar = {
        if (isEditing) SettingsBottomAction(tonalElevation = 0.dp) {
            WhiteNoiseButton(onClick = {
                val draft = dev.ipf.whitenoise.model.ProfileEditDraft(name.text.toString(), about.text.toString(), avatar, banner, address.text.toString(), lightning.text.toString())
                if (onSaveDraft != null) error = !onSaveDraft(draft) else {
                    val normalized = draft.normalized()
                    val detailsSaved = normalized != null && (normalized.name == profile.name && normalized.about == profile.about && avatar == profile.avatar || onSave(normalized.name, normalized.about, avatar))
                    val addressSaved = normalized != null && (normalized.nostrAddress == profile.nostrAddress || onSaveAddress(normalized.nostrAddress))
                    if (detailsSaved && addressSaved) isEditing = false else error = true
                }
            }, enabled = name.text.isNotBlank() && ProfileSettingsPolicy.isValidNostrAddress(address.text.toString()) && lightningValid && imageBusy.isEmpty() && !busy,
                loading = busy, loadingLabel = if (saveAttempt?.phase == dev.ipf.whitenoise.model.ProfileSavePhase.CheckingLightning) stringResource(R.string.profile_lightning_checking) else stringResource(R.string.profile_saving),
                modifier = Modifier.fillMaxWidth().testTag("profile.save"),
            ) { Text(if (saveFailure != null) stringResource(R.string.people_retry) else stringResource(R.string.save)) }
        }
    }) {
        Column(Modifier.fillMaxSize().whiteNoiseVerticalScroll(rememberScrollState()).padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
            banner?.let { ProfileBanner(it, onOpen = { viewing = it }) }
            if (isEditing) ProfileImageActions(profile.id, banner, isBanner = true, enabled = !busy,
                onChange = { banner = it; onRetainImages(avatar, it) },
                onBusyChanged = { imageBusy = if (it) imageBusy + "banner" else imageBusy - "banner" }, consumeFailure = consumeImageFailure)
            Box(Modifier.size(120.dp).then(if (avatar != ProfileAvatar.Monogram) Modifier.clickable(onClickLabel = stringResource(R.string.profile_view_photo), role = Role.Button) { viewing = avatar } else Modifier).testTag("profile.avatar")) {
                ProfileAvatar(name.text.toString(), avatar, Modifier.fillMaxSize())
            }
            if (isEditing) ProfileImageActions(profile.id, avatar, isBanner = false, enabled = !busy,
                onChange = { avatar = it ?: ProfileAvatar.Monogram; onRetainImages(avatar, banner) },
                onBusyChanged = { imageBusy = if (it) imageBusy + "avatar" else imageBusy - "avatar" }, consumeFailure = consumeImageFailure)
            WhiteNoiseTextField(name, Modifier.fillMaxWidth().testTag("profile.name_field"), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                enabled = !busy, readOnly = !isEditing, label = { Text(stringResource(R.string.name)) }, lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
            if (isEditing) TextButton(onClick = {
                val suggestion = dev.ipf.whitenoise.model.ProfileNameSuggestions.next(name.text.toString(), suggestionIndex++)
                name.edit { replace(0, length, suggestion) }
            }, enabled = !busy, modifier = Modifier.testTag("profile.suggest_name")) { Text(stringResource(R.string.profile_suggest_name)) }
            WhiteNoiseTextField(address, Modifier.fillMaxWidth().testTag("profile.address_field"), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                enabled = !busy, readOnly = !isEditing, label = { Text(stringResource(R.string.ui_verified_nostr_address)) },
                trailingIcon = if (profile.isNostrAddressVerified && address.text.toString() == profile.nostrAddress) {
                    { Icon(painterResource(R.drawable.ic_verified_filled), stringResource(R.string.verified), tint = MaterialTheme.colorScheme.onSurface) }
                } else null, lineLimits = TextFieldLineLimits.SingleLine, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            WhiteNoiseTextField(lightning, Modifier.fillMaxWidth().testTag("profile.lightning_field"), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                enabled = !busy, readOnly = !isEditing, label = { Text(stringResource(R.string.profile_lightning_address)) },
                isError = !lightningValid, errorMessage = stringResource(R.string.profile_lightning_invalid),
                supportingText = { Text(stringResource(if (!lightningValid) R.string.profile_lightning_invalid else R.string.profile_lightning_hint)) },
                lineLimits = TextFieldLineLimits.SingleLine, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            WhiteNoiseTextField(about, Modifier.fillMaxWidth().testTag("profile.about_field"), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                enabled = !busy, readOnly = !isEditing, label = { Text(stringResource(R.string.about)) }, placeholder = { Text(stringResource(R.string.about_prompt)) },
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6))
            if (error || failureRes != null) Text(stringResource(failureRes ?: R.string.profile_publish_failed), color = MaterialTheme.colorScheme.error)
        }
    }
    viewing?.let { image -> ProfileImageViewer(profile.id, profile.name, image, onDismiss = { viewing = null }, onEdit = if (busy) null else { { viewing = null; if (!isEditing) beginEditing() } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileKeysScreen(profile: Profile, onBack: () -> Unit, onRetryKey: () -> Unit = {}) {
    val context = LocalContext.current
    val publicKeyLabel = stringResource(R.string.ui_public_key)
    val privateKeyLabel = stringResource(R.string.ui_private_key)
    val publicKeyCopied = stringResource(R.string.profile_public_key_copied)
    val copyPublicKey = stringResource(R.string.profile_copy_public_key)
    val privateKeyRevealed = stringResource(R.string.profile_private_key_revealed)
    val privateKeyHidden = stringResource(R.string.profile_private_key_hidden)
    val hidePrivateKey = stringResource(R.string.profile_hide_private_key)
    val showPrivateKey = stringResource(R.string.profile_show_private_key)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val hasLocalKey = profile.signingMode == dev.ipf.whitenoise.model.ProfileSigningMode.LocalKey
    val canReadKey = ProfileKeyAccessPolicy.canRead(profile)
    var showPrivate by remember(profile.id) { mutableStateOf(false) }
    var pendingExport by remember(profile.id) { mutableStateOf<ProfileKeyExportRequest?>(null) }
    var passwordDialog by remember(profile.id) { mutableStateOf(false) }
    var rawExportDialog by remember(profile.id) { mutableStateOf(false) }
    var saveErrorDialog by remember { mutableStateOf(false) }
    var expiredExportDialog by remember { mutableStateOf(false) }
    var copiedKey by remember { mutableStateOf<CopiedProfileKey?>(null) }
    val password = remember(profile.id) { TextFieldState() }
    val confirmation = remember(profile.id) { TextFieldState() }
    val passwordValue = password.text.toString()
    val confirmationValue = confirmation.text.toString()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val request = pendingExport
        pendingExport = null
        if (uri != null && ProfileKeyAccessPolicy.canComplete(request, profile, SystemClock.elapsedRealtime())) {
            val result = runCatching {
                checkNotNull(context.contentResolver.openOutputStream(uri))
                    .bufferedWriter()
                    .use { writer -> writer.write(checkNotNull(request).content) }
            }
            saveErrorDialog = result.isFailure
        } else if (uri != null) {
            expiredExportDialog = true
        }
    }

    fun clearExportPassword() {
        password.edit { replace(0, length, "") }
        confirmation.edit { replace(0, length, "") }
    }

    fun beginExport(kind: ProfileKeyExportKind) {
        if (!canReadKey || pendingExport != null) return
        if (kind == ProfileKeyExportKind.Encrypted && !ProfileSettingsPolicy.isValidExportPassword(passwordValue, confirmationValue)) return
        val content = when (kind) {
            ProfileKeyExportKind.Raw -> ProfileKeyFixtures.rawExport(profile)
            ProfileKeyExportKind.Encrypted -> ProfileKeyFixtures.encryptedExport(profile, passwordValue)
        }
        pendingExport = ProfileKeyExportRequest(profile.id, kind, SystemClock.elapsedRealtime(), content)
        rawExportDialog = false
        passwordDialog = false
        showPrivate = false
        clearExportPassword()
        val filename = if (kind == ProfileKeyExportKind.Raw) "${profile.id}-white-noise-key.txt" else "${profile.id}-white-noise-key.wnkey.txt"
        if (runCatching { export.launch(filename) }.isFailure) {
            pendingExport = null
            saveErrorDialog = true
        }
    }

    DisposableEffect(lifecycle, profile.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                showPrivate = false
                rawExportDialog = false
                passwordDialog = false
                clearExportPassword()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            showPrivate = false
            pendingExport = null
            clearExportPassword()
        }
    }
    LaunchedEffect(showPrivate) {
        if (showPrivate) { delay(ProfileKeyAccessPolicy.EXPIRY_MILLIS); showPrivate = false }
    }
    LaunchedEffect(pendingExport) {
        val request = pendingExport ?: return@LaunchedEffect
        delay(ProfileKeyAccessPolicy.EXPIRY_MILLIS)
        if (pendingExport === request) { pendingExport = null; expiredExportDialog = true }
    }

    LaunchedEffect(copiedKey) {
        if (copiedKey != null) {
            delay(2_000)
            copiedKey = null
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.ui_profile_keys),
        onBack = onBack,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBarScrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.ui_public_key)) }
            item {
                SettingsGroup(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    ProfileKeyValueRow(
                        value = profile.publicKey,
                        valueModifier = Modifier.testTag("profile_keys.public_key_value"),
                        trailingAction = {
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, publicKeyLabel, profile.publicKey)
                                    copiedKey = CopiedProfileKey.Public
                                },
                            ) {
                                val copied = copiedKey == CopiedProfileKey.Public
                                Icon(
                                    painter = painterResource(
                                        if (copied) R.drawable.ic_check else R.drawable.ic_content_copy,
                                    ),
                                    contentDescription = if (copied) publicKeyCopied else copyPublicKey,
                                )
                            }
                        },
                    )
                }
            }
            item {
                ProfileKeySupportingText(stringResource(R.string.profile_public_key_help))
            }
            if (!hasLocalKey) {
                item { SettingsSection(stringResource(R.string.access_signing)) }
                item { SettingsExplainer(stringResource(R.string.access_amber_owns_key)) }
            } else if (!canReadKey) {
                item { SettingsExplainer(stringResource(R.string.key_access_unavailable)) }
                item { SettingsAction(stringResource(R.string.try_again), onClick = onRetryKey) }
            } else {
                item { SettingsSection(stringResource(R.string.ui_private_key)) }
                item {
                    SettingsGroup(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        ProfileKeyValueRow(
                            value = if (showPrivate) {
                                ProfileKeyFixtures.PRIVATE_KEY
                            } else {
                                "••••••••••••••••••••••••••••••••"
                            },
                            overflow = if (showPrivate) {
                                TextOverflow.MiddleEllipsis
                            } else {
                                TextOverflow.Clip
                            },
                            valueModifier = Modifier
                                .testTag("profile_keys.private_key_value")
                                .clearAndSetSemantics {
                                    contentDescription = if (showPrivate) {
                                        privateKeyRevealed
                                    } else {
                                        privateKeyHidden
                                    }
                                },
                            trailingAction = {
                                IconButton(onClick = { showPrivate = !showPrivate }) {
                                    Icon(
                                        painter = painterResource(
                                            if (showPrivate) {
                                                R.drawable.ic_visibility_off
                                            } else {
                                                R.drawable.ic_visibility
                                            },
                                        ),
                                        contentDescription = if (showPrivate) {
                                            hidePrivateKey
                                        } else {
                                            showPrivateKey
                                        },
                                    )
                                }
                            },
                        )
                        SettingsDivider()
                        SettingsAction(
                            title = stringResource(R.string.ui_copy_private_key),
                            onClick = {
                                copyToClipboard(
                                    context = context,
                                    label = privateKeyLabel,
                                    text = ProfileKeyFixtures.PRIVATE_KEY,
                                    isSensitive = true,
                                )
                                copiedKey = CopiedProfileKey.Private
                            },
                            leading = {
                                val copied = copiedKey == CopiedProfileKey.Private
                                Icon(
                                    painter = painterResource(
                                        if (copied) R.drawable.ic_check else R.drawable.ic_content_copy,
                                    ),
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
                item {
                    ProfileKeySupportingText(
                        stringResource(R.string.profile_private_key_help),
                    )
                }
                item { SettingsSection(stringResource(R.string.ui_export)) }
                item {
                    SettingsGroup(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        SettingsAction(
                            title = stringResource(R.string.ui_export_encrypted_private_key),
                            enabled = pendingExport == null,
                            onClick = { passwordDialog = true },
                            leading = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock),
                                    contentDescription = null,
                                )
                            },
                        )
                        SettingsDivider()
                        SettingsAction(
                            title = stringResource(R.string.ui_export_private_key),
                            enabled = pendingExport == null,
                            onClick = { rawExportDialog = true },
                            modifier = Modifier.testTag("profile_keys.export_raw"),
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
        }
    }
    if (rawExportDialog && canReadKey) {
        AlertDialog(
            onDismissRequest = { rawExportDialog = false },
            title = { Text(stringResource(R.string.ui_keep_your_private_key_safe)) },
            text = {
                Text(
                    stringResource(R.string.ui_store_this_file_somewhere_secure_the_encrypted_export_),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        beginExport(ProfileKeyExportKind.Raw)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.ui_export_private_key),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = { TextButton(onClick = { rawExportDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (passwordDialog && canReadKey) {
        AlertDialog(
            onDismissRequest = {
                passwordDialog = false
                clearExportPassword()
            },
            title = { Text(stringResource(R.string.ui_encrypted_private_key)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WhiteNoiseSecureTextField(
                        state = password,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_keys.export_password"),
                        label = { Text(stringResource(R.string.ui_password)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    WhiteNoiseSecureTextField(
                        state = confirmation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_keys.export_confirmation"),
                        label = { Text(stringResource(R.string.ui_confirm_password)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        isError = confirmationValue.isNotEmpty() && passwordValue != confirmationValue,
                    )
                    val passwordsMismatch = confirmationValue.isNotEmpty() &&
                        passwordValue != confirmationValue
                    Text(
                        text = if (passwordsMismatch) {
                            stringResource(R.string.profile_passwords_mismatch)
                        } else {
                            stringResource(R.string.profile_export_password_help)
                        },
                        color = if (passwordsMismatch) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ProfileSettingsPolicy.exportPasswordStrength(passwordValue)?.let {
                        ExportPasswordStrengthIndicator(it)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ProfileSettingsPolicy.isValidExportPassword(passwordValue, confirmationValue),
                    onClick = {
                        beginExport(ProfileKeyExportKind.Encrypted)
                    },
                ) { Text(stringResource(R.string.ui_export)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        passwordDialog = false
                        clearExportPassword()
                    },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (saveErrorDialog) {
        AlertDialog(
            onDismissRequest = { saveErrorDialog = false },
            title = { Text(stringResource(R.string.ui_couldnt_save_file)) },
            text = { Text(stringResource(R.string.developer_choose_another_location_and_try_again)) },
            confirmButton = {
                TextButton(onClick = { saveErrorDialog = false }) { Text(stringResource(R.string.ui_ok)) }
            },
        )
    }
    if (expiredExportDialog) AlertDialog(
        onDismissRequest = { expiredExportDialog = false },
        title = { Text(stringResource(R.string.key_export_expired_title)) },
        text = { Text(stringResource(R.string.key_export_expired_body)) },
        confirmButton = { TextButton(onClick = { expiredExportDialog = false }) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun ExportPasswordStrengthIndicator(strength: ExportPasswordStrength) {
    val color = when (strength) {
        ExportPasswordStrength.Low -> MaterialTheme.colorScheme.error
        ExportPasswordStrength.Fair -> MaterialTheme.colorScheme.onSurfaceVariant
        ExportPasswordStrength.Strong -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_keys.password_strength"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.ui_strength), style = MaterialTheme.typography.labelLarge)
            Text(exportPasswordStrengthLabel(strength), color = color, style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { strength.completedSteps / 3f },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun exportPasswordStrengthLabel(strength: ExportPasswordStrength): String = stringResource(
    when (strength) {
        ExportPasswordStrength.Low -> R.string.profile_password_strength_low
        ExportPasswordStrength.Fair -> R.string.profile_password_strength_fair
        ExportPasswordStrength.Strong -> R.string.profile_password_strength_strong
    },
)

@Composable
private fun ProfileKeyValueRow(
    value: String,
    trailingAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.MiddleEllipsis,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            modifier = valueModifier.weight(1f),
            maxLines = 1,
            overflow = overflow,
            fontFamily = FontFamily.Monospace,
        )
        trailingAction()
    }
}

@Composable
private fun ProfileKeySupportingText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WhiteNoiseSpacing.SettingsSectionInset,
                end = WhiteNoiseSpacing.SettingsSectionInset,
                top = WhiteNoiseSpacing.Related,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

private enum class CopiedProfileKey { Public, Private }


@Composable
internal fun ProfileCode(
    value: String,
    modifier: Modifier = Modifier,
    contentDescription: String,
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

internal fun copyToClipboard(
    context: Context,
    label: String,
    text: String,
    isSensitive: Boolean = false,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ClipDescription.EXTRA_IS_SENSITIVE
                } else {
                    "android.content.extra.IS_SENSITIVE"
                },
                true,
            )
        }
    }
    clipboard.setPrimaryClip(clip)
}
