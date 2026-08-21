package dev.ipf.whitenoise.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.AutoLockDuration
import dev.ipf.whitenoise.model.MediaDownloadPolicy
import dev.ipf.whitenoise.model.NotificationPreviewMode
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.model.SentMediaQuality

@Composable
fun NotificationsScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
) {
    val context = LocalContext.current
    var previewPicker by remember { mutableStateOf(false) }
    val settings = profile.settings
    SettingsScaffold(title = "Notifications", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Delivery") }
            item {
                SettingsGroup {
                    SettingsSwitch(
                        title = "Local notifications",
                        checked = settings.localNotifications,
                        onCheckedChange = {
                            onChange(
                                settings.copy(
                                    localNotifications = it,
                                    nativePushNotifications = settings.nativePushNotifications && it,
                                ),
                            )
                        },
                        subtitle = "Show notifications created on this device.",
                    )
                    SettingsSwitch(
                        title = "Native push",
                        checked = settings.nativePushNotifications,
                        enabled = settings.localNotifications,
                        onCheckedChange = { onChange(settings.copy(nativePushNotifications = it)) },
                        subtitle = if (settings.localNotifications) {
                            "Use native push delivery for new messages."
                        } else {
                            "Turn on local notifications first."
                        },
                    )
                }
            }
            item { SettingsSection("Preview") }
            item {
                SettingsGroup {
                    SettingsLink(
                        "Message preview",
                        if (settings.localNotifications) settings.notificationPreviewMode.label else "Local notifications are off",
                        onClick = { previewPicker = true },
                        enabled = settings.localNotifications,
                    )
                }
            }
            item { SettingsSection("Android") }
            item {
                SettingsGroup {
                    SettingsAction(
                        title = "Open Android notification settings",
                        subtitle = "Control app-level permission, channels, sound, and visibility.",
                        onClick = { context.startActivity(notificationSettingsIntent(context)) },
                    )
                }
            }
        }
    }
    if (previewPicker) {
        ChoiceDialog(
            title = "Message preview",
            values = NotificationPreviewMode.entries,
            selected = settings.notificationPreviewMode,
            label = NotificationPreviewMode::label,
            onDismiss = { previewPicker = false },
            onSelect = {
                onChange(settings.copy(notificationPreviewMode = it))
                previewPicker = false
            },
        )
    }
}

private fun notificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationSettingsIntentApi26(context)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun notificationSettingsIntentApi26(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

@Composable
fun AppearanceScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
) {
    val settings = profile.settings
    SettingsScaffold(title = "Appearance", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Theme") }
            item {
                SettingsGroup {
                    AppearancePreference.entries.forEach { preference ->
                        SettingsChoice(
                            title = preference.label,
                            selected = settings.appearance == preference,
                            onClick = { onChange(settings.copy(appearance = preference)) },
                        )
                    }
                }
            }
            item { SettingsSection("Language") }
            item {
                SettingsGroup {
                    SettingsChoice(
                        title = "System default (English)",
                        selected = settings.language == "System default (English)",
                        onClick = { onChange(settings.copy(language = "System default (English)")) },
                    )
                    SettingsChoice(
                        title = "English",
                        selected = settings.language == "English",
                        onClick = { onChange(settings.copy(language = "English")) },
                    )
                }
            }
            item { SettingsExplainer("English is currently available. Theme changes apply immediately to the active profile.") }
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
) {
    val context = LocalContext.current
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val secure = keyguard.isDeviceSecure
    var autoLockPicker by remember { mutableStateOf(false) }
    var eraseOpen by remember { mutableStateOf(false) }
    val settings = profile.settings
    SettingsScaffold(title = "Privacy & Security", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Device protection") }
            item {
                SettingsGroup {
                    SettingsSwitch(
                        title = "Hide Screen in Recents",
                        checked = settings.hideScreenInRecents,
                        onCheckedChange = { onChange(settings.copy(hideScreenInRecents = it)) },
                        subtitle = "Protect previews and screenshots with Android’s secure window.",
                    )
                    SettingsSwitch(
                        title = "Require device authentication",
                        checked = settings.requireDeviceAuthentication,
                        enabled = secure,
                        onCheckedChange = { onChange(settings.copy(requireDeviceAuthentication = it)) },
                        subtitle = if (secure) {
                            "Use the device screen lock when returning to White Noise."
                        } else {
                            "Set a device screen lock first."
                        },
                    )
                    if (!secure) {
                        SettingsAction(
                            title = "Open Android security settings",
                            onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                        )
                    }
                    SettingsLink("Auto-lock", settings.autoLockDuration.label, onClick = { autoLockPicker = true })
                }
            }
            item { SettingsExplainer("Security preferences are stored separately for each profile.") }
            item { SettingsSection("Device data") }
            item {
                SettingsGroup {
                    SettingsAction(
                        title = "Erase App Data",
                        subtitle = "Signs out every profile and permanently removes all White Noise data from this device.",
                        onClick = { eraseOpen = true },
                        destructive = true,
                    )
                }
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
fun DataUsageScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
) {
    val settings = profile.settings
    var picker by remember { mutableStateOf<DataPicker?>(null) }
    var qualityPicker by remember { mutableStateOf(false) }
    SettingsScaffold(title = "Data Usage", onBack = onBack) {
        SettingsList {
            item { SettingsSection("Automatic downloads") }
            item {
                SettingsGroup {
                    SettingsLink("Photos", settings.photoDownloadPolicy.label, onClick = { picker = DataPicker.Photos })
                    SettingsLink("Videos", settings.videoDownloadPolicy.label, onClick = { picker = DataPicker.Videos })
                    SettingsLink("Audio", settings.audioDownloadPolicy.label, onClick = { picker = DataPicker.Audio })
                    SettingsLink("Files", settings.fileDownloadPolicy.label, onClick = { picker = DataPicker.Files })
                    SettingsAction(
                        title = "Reset download settings",
                        subtitle = "Restore the default policy for every media type.",
                        onClick = {
                            onChange(
                                settings.copy(
                                    photoDownloadPolicy = ProfileSettings().photoDownloadPolicy,
                                    videoDownloadPolicy = ProfileSettings().videoDownloadPolicy,
                                    audioDownloadPolicy = ProfileSettings().audioDownloadPolicy,
                                    fileDownloadPolicy = ProfileSettings().fileDownloadPolicy,
                                ),
                            )
                        },
                    )
                }
            }
            item { SettingsSection("Sent media") }
            item {
                SettingsGroup {
                    SettingsLink("Photo and video quality", settings.sentMediaQuality.label, onClick = { qualityPicker = true })
                }
            }
            item { SettingsExplainer("Choose when White Noise may download each media type.") }
        }
    }
    picker?.let { target ->
        val current = when (target) {
            DataPicker.Photos -> settings.photoDownloadPolicy
            DataPicker.Videos -> settings.videoDownloadPolicy
            DataPicker.Audio -> settings.audioDownloadPolicy
            DataPicker.Files -> settings.fileDownloadPolicy
        }
        ChoiceDialog(
            title = target.label,
            values = MediaDownloadPolicy.entries,
            selected = current,
            label = MediaDownloadPolicy::label,
            onDismiss = { picker = null },
            onSelect = { selected ->
                onChange(
                    when (target) {
                        DataPicker.Photos -> settings.copy(photoDownloadPolicy = selected)
                        DataPicker.Videos -> settings.copy(videoDownloadPolicy = selected)
                        DataPicker.Audio -> settings.copy(audioDownloadPolicy = selected)
                        DataPicker.Files -> settings.copy(fileDownloadPolicy = selected)
                    },
                )
                picker = null
            },
        )
    }
    if (qualityPicker) {
        ChoiceDialog(
            title = "Sent media quality",
            values = SentMediaQuality.entries,
            selected = settings.sentMediaQuality,
            label = SentMediaQuality::label,
            onDismiss = { qualityPicker = false },
            onSelect = {
                onChange(settings.copy(sentMediaQuality = it))
                qualityPicker = false
            },
        )
    }
}

private enum class DataPicker(val label: String) {
    Photos("Photos"),
    Videos("Videos"),
    Audio("Audio"),
    Files("Files"),
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                values.forEach { value ->
                    SettingsChoice(
                        title = label(value),
                        selected = value == selected,
                        onClick = { onSelect(value) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
