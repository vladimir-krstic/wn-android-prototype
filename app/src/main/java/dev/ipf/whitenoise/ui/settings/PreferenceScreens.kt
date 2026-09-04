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
import dev.ipf.whitenoise.model.AppFontSize
import dev.ipf.whitenoise.model.AppFontFamily
import dev.ipf.whitenoise.model.EnterKeyBehavior
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
    SettingsScaffold(title = stringResource(R.string.notification_controls_title), onBack = onBack) {
        SettingsList {
            if (!notificationsAllowed) {
                item {
                    SettingsGroup(
                        modifier = Modifier.testTag("notifications.permission.group"),
                    ) {
                        when (permissionStatus) {
                            NotificationPermissionStatus.NotRequested -> SettingsAction(
                                title = stringResource(R.string.ui_allow_notifications),
                                subtitle = stringResource(R.string.ui_get_notified_about_new_messages_and_use_these_options),
                                onClick = permissionAccess.requestPermission,
                                leading = {
                                    NotificationPermissionIcon(R.drawable.ic_settings_notifications)
                                },
                            )
                            NotificationPermissionStatus.Blocked -> SettingsLink(
                                title = stringResource(R.string.ui_notifications_are_off),
                                subtitle = stringResource(R.string.ui_turn_them_on_in_android_settings_to_use_these_options),
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
            item { SettingsSection(stringResource(R.string.ui_delivery)) }
            item {
                SettingsGroup {
                    SettingsSwitch(
                        title = stringResource(R.string.ui_local_notifications),
                        checked = localNotificationsEnabled,
                        enabled = notificationsAllowed,
                        onCheckedChange = {
                            if (controller != null) controller.request(NotificationChange.Delivery(NotificationDelivery.Local,it),expectedProfileId = profile.id)
                            else onChange(settings.copy(localNotifications = it,
                                nativePushNotifications = settings.nativePushNotifications && it))
                        },
                        subtitle = stringResource(R.string.ui_create_message_notifications_on_this_device_without_na),
                    )
                    SettingsDivider(Modifier.testTag("notifications.delivery.divider"))
                    SettingsSwitch(
                        title = stringResource(R.string.ui_native_push),
                        checked = NotificationControls.pushEnabled(settings,notificationsAllowed,environment.push),
                        enabled = localNotificationsEnabled && environment.push == PushAvailability.Available,
                        onCheckedChange = {
                            if (controller != null) controller.request(NotificationChange.Delivery(NotificationDelivery.Push,it),expectedProfileId = profile.id)
                            else onChange(settings.copy(nativePushNotifications = it))
                        },
                        subtitle = when {
                            !notificationsAllowed -> stringResource(R.string.notification_allow_first)
                            !settings.localNotifications -> stringResource(R.string.notification_local_first)
                            environment.push == PushAvailability.BuildNotConfigured -> stringResource(R.string.notification_push_build)
                            environment.push == PushAvailability.PlayServicesMissing -> stringResource(R.string.notification_push_services)
                            environment.push == PushAvailability.ProviderNotInitialized -> stringResource(R.string.notification_push_provider)
                            else -> stringResource(R.string.notification_push_detail)
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
            item { SettingsSection(stringResource(R.string.preview)) }
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
                                title = notificationPreviewLabel(mode),
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
                        example = notificationPreviewExample(settings.notificationPreviewMode),
                        enabled = localNotificationsEnabled,
                    )
                }
            }
            item {
                SettingsExplainer(
                    when {
                        !notificationsAllowed -> stringResource(R.string.notification_preview_allow_first)
                        !settings.localNotifications -> stringResource(R.string.notification_preview_local_first)
                        else -> stringResource(R.string.notification_preview_help)
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
    onActionColor: () -> Unit = {},
    onBubbleColors: () -> Unit = {},
) {
    val settings = profile.settings
    var fontFamilyOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    var fontSizeOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    var enterOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    val familyLabels = AppFontFamily.entries.associateWith { stringResource(it.labelRes) }
    val fontLabels = AppFontSize.entries.associateWith { stringResource(it.labelRes) }
    val enterLabels = EnterKeyBehavior.entries.associateWith { stringResource(it.labelRes) }
    if (fontFamilyOpen) ChoiceDialog(stringResource(R.string.appearance_font_family), AppFontFamily.entries, settings.fontFamily,
        { familyLabels.getValue(it) }, onDismiss = { fontFamilyOpen = false },
        onSelect = { fontFamilyOpen = false; onChange(settings.copy(fontFamily = it)) })
    if (fontSizeOpen) ChoiceDialog(stringResource(R.string.appearance_font_size), AppFontSize.entries, settings.fontSize,
        { fontLabels.getValue(it) }, supportingText = stringResource(R.string.appearance_font_size_help), onDismiss = { fontSizeOpen = false },
        onSelect = { fontSizeOpen = false; onChange(settings.copy(fontSize = it)) })
    if (enterOpen) ChoiceDialog(stringResource(R.string.appearance_enter_behavior), EnterKeyBehavior.entries, settings.enterKeyBehavior,
        { enterLabels.getValue(it) }, supportingText = stringResource(R.string.appearance_enter_help), onDismiss = { enterOpen = false },
        onSelect = { enterOpen = false; onChange(settings.copy(enterKeyBehavior = it)) })
    SettingsScaffold(title = stringResource(R.string.appearance_title), onBack = onBack) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.appearance_theme)) }
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
                                title = stringResource(preference.labelResource()),
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
                    stringResource(R.string.appearance_theme_help),
                )
            }
            item {
                SettingsGroup {
                    SettingsLink(
                        title = stringResource(R.string.action_color),
                        onClick = onActionColor,
                    )
                    SettingsDivider()
                    SettingsLink(
                        title = stringResource(R.string.chat_bubble_colors),
                        onClick = onBubbleColors,
                    )
                }
            }
            item {
                SettingsGroup {
                    SettingsLink(stringResource(R.string.appearance_font_family), value = familyLabels.getValue(settings.fontFamily), onClick = { fontFamilyOpen = true })
                    SettingsDivider()
                    SettingsLink(stringResource(R.string.appearance_font_size), value = fontLabels.getValue(settings.fontSize), onClick = { fontSizeOpen = true })
                    SettingsDivider()
                    SettingsLink(stringResource(R.string.appearance_enter_behavior), value = enterLabels.getValue(settings.enterKeyBehavior), onClick = { enterOpen = true })
                }
            }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("appearance.language.group"),
                ) {
                    SettingsLink(
                        title = stringResource(R.string.language_title),
                        value = stringResource(settings.language.labelResource()),
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
    SettingsScaffold(title = stringResource(R.string.language_title), onBack = onBack) {
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
                                title = stringResource(preference.labelResource()),
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

private fun LanguagePreference.labelResource(): Int = when (this) {
    LanguagePreference.System -> R.string.language_system
    LanguagePreference.English -> R.string.language_english
    LanguagePreference.German -> R.string.language_german
    LanguagePreference.Spanish -> R.string.language_spanish
    LanguagePreference.French -> R.string.language_french
    LanguagePreference.Italian -> R.string.language_italian
    LanguagePreference.Portuguese -> R.string.language_portuguese
    LanguagePreference.Serbian -> R.string.language_serbian
    LanguagePreference.Russian -> R.string.language_russian
    LanguagePreference.Turkish -> R.string.language_turkish
    LanguagePreference.ChineseSimplified -> R.string.language_chinese_simplified
    LanguagePreference.ChineseTraditional -> R.string.language_chinese_traditional
}

private fun AppearancePreference.labelResource(): Int = when (this) {
    AppearancePreference.System -> R.string.language_system
    AppearancePreference.Light -> R.string.theme_light
    AppearancePreference.Dark -> R.string.theme_dark
    AppearancePreference.Amoled -> R.string.appearance_amoled
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
    val autoLockLabels = AutoLockDuration.entries.associateWith { autoLockLabel(it) }
    LaunchedEffect(authenticationEnabled) {
        if (!authenticationEnabled) autoLockPicker = false
    }
    SettingsScaffold(title = stringResource(R.string.ui_privacy_security), onBack = onBack) {
        SettingsList {
            item { SettingsSection(stringResource(R.string.ui_device_protection)) }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("privacy.device_protection.group"),
                ) {
                    SettingsSwitch(
                        title = stringResource(R.string.ui_hide_screen_in_recents),
                        checked = settings.hideScreenInRecents,
                        onCheckedChange = { onChange(settings.copy(hideScreenInRecents = it)) },
                        subtitle = stringResource(R.string.ui_hide_conversations_and_profile_details_in_the_recents_),
                    )
                    SettingsDivider(Modifier.testTag("privacy.device_protection.divider.recents"))
                    SettingsSwitch(
                        title = stringResource(R.string.block_screenshots_in_chats),
                        checked = settings.blockScreenshotsInChats,
                        onCheckedChange = { onChange(settings.copy(blockScreenshotsInChats = it)) },
                        subtitle = stringResource(R.string.block_screenshots_in_chats_detail),
                    )
                    SettingsDivider(Modifier.testTag("privacy.device_protection.divider.screenshots"))
                    SettingsSwitch(
                        title = stringResource(R.string.incognito_keyboard),
                        checked = settings.incognitoKeyboard,
                        enabled = android.os.Build.VERSION.SDK_INT >= 26,
                        onCheckedChange = { onChange(settings.copy(incognitoKeyboard = it)) },
                        subtitle = stringResource(if (android.os.Build.VERSION.SDK_INT >= 26) R.string.incognito_keyboard_detail else R.string.incognito_keyboard_unavailable),
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        title = stringResource(R.string.ui_require_device_authentication),
                        checked = authenticationEnabled,
                        enabled = secure,
                        onCheckedChange = { onChange(settings.copy(requireDeviceAuthentication = it)) },
                        subtitle = if (secure) {
                            stringResource(R.string.app_lock_enabled_detail)
                        } else {
                            stringResource(R.string.app_lock_setup_detail)
                        },
                    )
                    if (!secure) {
                        SettingsDivider(Modifier.testTag("privacy.device_protection.divider.security_settings"))
                        SettingsAction(
                            title = stringResource(R.string.ui_open_android_security_settings),
                            onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                        )
                    }
                    if (authenticationEnabled) {
                        SettingsDivider(Modifier.testTag("privacy.device_protection.divider.auto_lock"))
                        SettingsLink(
                            title = stringResource(R.string.ui_auto_lock),
                            value = autoLockLabel(settings.autoLockDuration),
                            onClick = { autoLockPicker = true },
                        )
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.developer_diagnostics)) }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("privacy.diagnostics.group"),
                ) {
                    SettingsLink(
                        title = stringResource(R.string.diagnostics_improvements),
                        value = profile.diagnostics.summary,
                        onClick = onDiagnosticsImprovements,
                    )
                }
            }
            item {
                SettingsExplainer(stringResource(R.string.ui_control_optional_analytics_and_diagnostic_logs_for_thi))
            }
            item { SettingsSection(stringResource(R.string.ui_device_data)) }
            item {
                SettingsGroup(
                    modifier = Modifier.testTag("privacy.erase.group"),
                ) {
                    SettingsAction(
                        title = stringResource(R.string.ui_erase_app_data),
                        onClick = { eraseOpen = true },
                        destructive = true,
                    )
                }
            }
            item {
                SettingsExplainer(
                    stringResource(R.string.ui_signs_out_every_profile_and_permanently_removes_all_wh),
                )
            }
        }
    }
    if (autoLockPicker) {
        ChoiceDialog(
            title = stringResource(R.string.ui_auto_lock),
            values = AutoLockDuration.entries,
            selected = settings.autoLockDuration,
            label = { autoLockLabels.getValue(it) },
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
private fun notificationPreviewLabel(mode: NotificationPreviewMode): String = stringResource(
    when (mode) {
        NotificationPreviewMode.SenderAndMessage -> R.string.notification_preview_sender_message
        NotificationPreviewMode.SenderOnly -> R.string.notification_preview_sender_only
        NotificationPreviewMode.Generic -> R.string.notification_preview_generic
    },
)

@Composable
private fun notificationPreviewExample(mode: NotificationPreviewMode): String = stringResource(
    when (mode) {
        NotificationPreviewMode.SenderAndMessage -> R.string.notification_preview_example_sender_message
        NotificationPreviewMode.SenderOnly -> R.string.notification_preview_example_sender_only
        NotificationPreviewMode.Generic -> R.string.notification_preview_example_generic
    },
)

@Composable
private fun autoLockLabel(duration: AutoLockDuration): String = stringResource(
    when (duration) {
        AutoLockDuration.Immediately -> R.string.auto_lock_immediately
        AutoLockDuration.OneMinute -> R.string.auto_lock_one_minute
        AutoLockDuration.FiveMinutes -> R.string.auto_lock_five_minutes
        AutoLockDuration.FifteenMinutes -> R.string.auto_lock_fifteen_minutes
        AutoLockDuration.ThirtyMinutes -> R.string.auto_lock_thirty_minutes
    },
)

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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
