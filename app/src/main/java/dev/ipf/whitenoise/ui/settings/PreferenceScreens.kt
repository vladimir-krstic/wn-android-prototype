package dev.ipf.whitenoise.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.model.NotificationCategory
import dev.ipf.whitenoise.model.NotificationControls
import dev.ipf.whitenoise.model.NotificationEnvironment
import dev.ipf.whitenoise.model.PushAvailability
import dev.ipf.whitenoise.state.NotificationChange
import dev.ipf.whitenoise.state.NotificationDelivery
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.AutoLockDuration
import dev.ipf.whitenoise.model.LanguagePreference
import dev.ipf.whitenoise.model.NotificationPreviewMode
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.ui.components.WhiteNoiseDialogChoiceRow
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun NotificationsScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
    permissionStatusOverride: NotificationPermissionStatus? = null,
    onOpenCategory: ((NotificationCategory) -> NotificationSettingsOpen)? = null,
) {
    val permissionAccess = rememberNotificationPermissionAccess()
    val permissionStatus = permissionStatusOverride ?: permissionAccess.status
    val notificationsAllowed = permissionStatus == NotificationPermissionStatus.Allowed
    val settings = profile.settings
    val controller = LocalNotificationControls.current
    val environment = controller?.environment ?: NotificationEnvironment()
    var settingsResult by rememberSaveable(profile.id) { mutableStateOf<NotificationSettingsOpen?>(null) }
    val context = LocalContext.current
    val localNotificationsEnabled = notificationsAllowed && settings.localNotifications
    SettingsScaffold(title = "Notifications", onBack = onBack) {
        SettingsList {
            if (!notificationsAllowed) {
                item {
                    SettingsGroup(
                        modifier = Modifier.testTag("notifications.permission.group"),
                    ) {
                        when (permissionStatus) {
                            NotificationPermissionStatus.NotRequested -> SettingsAction(
                                title = "Allow notifications",
                                subtitle = "Get notified about new messages and use these options.",
                                onClick = permissionAccess.requestPermission,
                                leading = {
                                    NotificationPermissionIcon(R.drawable.ic_settings_notifications)
                                },
                            )
                            NotificationPermissionStatus.Blocked -> SettingsLink(
                                title = "Notifications are off",
                                subtitle = "Turn them on in Android Settings to use these options.",
                                onClick = permissionAccess.openSettings,
                                leading = {
                                    NotificationPermissionIcon(R.drawable.ic_notifications_off)
                                },
                            )
                            NotificationPermissionStatus.Allowed -> Unit
                        }
                    }
                }
            }
            item { SettingsSection("Delivery") }
            item {
                SettingsGroup {
                    SettingsSwitch(
                        title = "Local notifications",
                        checked = localNotificationsEnabled,
                        enabled = notificationsAllowed,
                        onCheckedChange = {
                            if (controller != null) controller.request(NotificationChange.Delivery(NotificationDelivery.Local,it),expectedProfileId = profile.id)
                            else onChange(settings.copy(localNotifications = it,
                                nativePushNotifications = settings.nativePushNotifications && it))
                        },
                        subtitle = "Create message notifications on this device. Without native push, delivery may wait until White Noise is active.",
                    )
                    SettingsDivider(Modifier.testTag("notifications.delivery.divider"))
                    SettingsSwitch(
                        title = "Native push",
                        checked = NotificationControls.pushEnabled(settings,notificationsAllowed,environment.push),
                        enabled = localNotificationsEnabled && environment.push == PushAvailability.Available,
                        onCheckedChange = {
                            if (controller != null) controller.request(NotificationChange.Delivery(NotificationDelivery.Push,it),expectedProfileId = profile.id)
                            else onChange(settings.copy(nativePushNotifications = it))
                        },
                        subtitle = when {
                            !notificationsAllowed -> "Allow notifications first."
                            !settings.localNotifications -> "Turn on local notifications first."
                            environment.push == PushAvailability.BuildNotConfigured -> stringResource(R.string.notification_push_build)
                            environment.push == PushAvailability.PlayServicesMissing -> stringResource(R.string.notification_push_services)
                            environment.push == PushAvailability.ProviderNotInitialized -> stringResource(R.string.notification_push_provider)
                            else -> "Use a generic wake-up signal to check for new messages in the background. Message details stay on this device."
                        },
                    )
                }
            }
            if (controller != null) {
                item { SettingsGroup {
                    SettingsSwitch(stringResource(R.string.notification_background),
                        NotificationControls.backgroundEnabled(controller.backgroundConnection,notificationsAllowed), {
                            controller.request(NotificationChange.Delivery(NotificationDelivery.Background,it),expectedProfileId = profile.id)
                        },enabled = notificationsAllowed,subtitle = stringResource(R.string.notification_background_detail))
                } }
                item { SettingsExplainer(stringResource(if (NotificationControls.backgroundEnabled(controller.backgroundConnection,notificationsAllowed))
                    R.string.notification_background_on else R.string.notification_background_off)) }
            }
            item { SettingsSection("Preview") }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("notifications.preview.group"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                    ) {
                        NotificationPreviewMode.entries.forEachIndexed { index, mode ->
                            if (index > 0) {
                                SettingsDivider()
                            }
                            SettingsChoice(
                                title = mode.label,
                                selected = settings.notificationPreviewMode == mode,
                                enabled = localNotificationsEnabled,
                                highlightSelected = false,
                                onClick = {
                                    onChange(settings.copy(notificationPreviewMode = mode))
                                },
                            )
                        }
                    }
                    SettingsDivider()
                    NotificationPreviewExample(
                        example = settings.notificationPreviewMode.example,
                        enabled = localNotificationsEnabled,
                    )
                }
            }
            item {
                SettingsExplainer(
                    when {
                        !notificationsAllowed -> "Allow notifications to change message previews."
                        !settings.localNotifications -> "Turn on local notifications to change message previews."
                        else -> "Choose how much message information appears in notifications."
                    },
                )
            }
            item { SettingsSection(stringResource(R.string.notification_categories)) }
            item { SettingsExplainer(stringResource(R.string.notification_categories_detail)) }
            item { SettingsGroup {
                NotificationCategory.global(environment.updatesAvailable).forEachIndexed { index, category ->
                    if (index > 0) SettingsDivider()
                    SettingsLink(stringResource(notificationCategoryResource(category)),
                        onClick = { settingsResult = onOpenCategory?.invoke(category) ?: openNotificationCategory(context,category) })
                }
            } }
        }
    }
    NotificationSettingsFeedback(settingsResult) { settingsResult = null }
}

@Composable
private fun NotificationPermissionIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotificationPreviewExample(
    example: String,
    enabled: Boolean,
) {
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (enabled) 1f else 0.38f,
    )
    ListItem(
        headlineContent = { Text(example, color = contentColor) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_settings_notifications),
                contentDescription = null,
                tint = contentColor,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notifications.preview.example"),
    )
}

@Composable
fun AppearanceScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
    onLanguage: () -> Unit,
) {
    val settings = profile.settings
    SettingsScaffold(title = "Appearance", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Theme") }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("appearance.theme.group"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                    ) {
                        AppearancePreference.entries.forEachIndexed { index, preference ->
                            if (index > 0) {
                                SettingsDivider()
                            }
                            SettingsChoice(
                                title = preference.label,
                                selected = settings.appearance == preference,
                                highlightSelected = false,
                                onClick = { onChange(settings.copy(appearance = preference)) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsExplainer(
                    "System default follows your device appearance. Light and Dark keep the selected appearance.",
                )
            }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("appearance.language.group"),
                ) {
                    SettingsLink(
                        title = "Language",
                        value = settings.language.label,
                        onClick = onLanguage,
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
) {
    val settings = profile.settings
    SettingsScaffold(title = "Language", onBack = onBack) {
        SettingsList {
            item {
                SettingsGroup(
                    modifier = Modifier
                        .padding(top = WhiteNoiseSpacing.Section)
                        .testTag("language.choices.group"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                    ) {
                        LanguagePreference.entries.forEachIndexed { index, preference ->
                            if (index > 0) {
                                SettingsDivider()
                            }
                            SettingsChoice(
                                title = preference.label,
                                selected = settings.language == preference,
                                highlightSelected = false,
                                onClick = { onChange(settings.copy(language = preference)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySecurityScreen(
    profile: Profile,
    allProfileIds: Collection<String>,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
    onEraseAppData: (String) -> Unit,
    onDiagnosticsImprovements: () -> Unit = {},
    deviceSecureOverride: Boolean? = null,
) {
    val context = LocalContext.current
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val secure = deviceSecureOverride ?: keyguard.isDeviceSecure
    var autoLockPicker by remember { mutableStateOf(false) }
    var eraseOpen by remember { mutableStateOf(false) }
    val settings = profile.settings
    val authenticationEnabled = secure && settings.requireDeviceAuthentication
    LaunchedEffect(authenticationEnabled) {
        if (!authenticationEnabled) autoLockPicker = false
    }
    SettingsScaffold(title = "Privacy & Security", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Device protection") }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("privacy.device_protection.group"),
                ) {
                    SettingsSwitch(
                        title = "Hide Screen in Recents",
                        checked = settings.hideScreenInRecents,
                        onCheckedChange = { onChange(settings.copy(hideScreenInRecents = it)) },
                        subtitle = "Hide conversations and profile details in Recents.",
                    )
                    SettingsDivider(Modifier.testTag("privacy.device_protection.divider.recents"))
                    SettingsSwitch(
                        title = "Require device authentication",
                        checked = authenticationEnabled,
                        enabled = secure,
                        onCheckedChange = { onChange(settings.copy(requireDeviceAuthentication = it)) },
                        subtitle = if (secure) {
                            "Use the device screen lock when returning to White Noise."
                        } else {
                            "Set a device screen lock first."
                        },
                    )
                    if (!secure) {
                        SettingsDivider(Modifier.testTag("privacy.device_protection.divider.security_settings"))
                        SettingsAction(
                            title = "Open Android security settings",
                            onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                        )
                    }
                    if (authenticationEnabled) {
                        SettingsDivider(Modifier.testTag("privacy.device_protection.divider.auto_lock"))
                        SettingsLink(
                            title = "Auto-lock",
                            value = settings.autoLockDuration.label,
                            onClick = { autoLockPicker = true },
                        )
                    }
                }
            }
            item { SettingsSection("Diagnostics") }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("privacy.diagnostics.group"),
                ) {
                    SettingsLink(
                        title = "Diagnostics & Improvements",
                        value = profile.diagnostics.summary,
                        onClick = onDiagnosticsImprovements,
                    )
                }
            }
            item {
                SettingsExplainer("Control optional analytics and diagnostic logs for this profile.")
            }
            item { SettingsSection("Device data") }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("privacy.erase.group"),
                ) {
                    SettingsAction(
                        title = "Erase App Data",
                        onClick = { eraseOpen = true },
                        destructive = true,
                    )
                }
            }
            item {
                SettingsExplainer(
                    "Signs out every profile and permanently removes all White Noise data from this device.",
                )
            }
        }
    }
    if (autoLockPicker) {
        ChoiceDialog(
            title = "Auto-lock",
            values = AutoLockDuration.entries,
            selected = settings.autoLockDuration,
            label = AutoLockDuration::label,
            onDismiss = { autoLockPicker = false },
            onSelect = {
                onChange(settings.copy(autoLockDuration = it))
                autoLockPicker = false
            },
        )
    }
    if (eraseOpen) {
        EraseAppDataSheet(
            profileIds = allProfileIds,
            onDismiss = { eraseOpen = false },
            onErase = onEraseAppData,
        )
    }
}

@Composable
internal fun <T> ChoiceDialog(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    supportingText: String? = null,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("choice_dialog.content")) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .testTag("choice_dialog.options"),
                ) {
                    values.forEachIndexed { index, value ->
                        WhiteNoiseDialogChoiceRow(
                            title = label(value),
                            selected = value == selected,
                            modifier = Modifier.testTag("choice_dialog.option.$index"),
                            onClick = { onSelect(value) },
                        )
                    }
                }
                supportingText?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.FormField),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
