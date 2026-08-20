package dev.ipf.whitenoise.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("Delivery")
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
                subtitle = "Use native push delivery for new messages.",
            )
            SettingsSection("Preview")
            SettingsLink(
                "Message preview",
                if (settings.localNotifications) settings.notificationPreviewMode.label else "Local notifications are off",
                onClick = { previewPicker = true },
                enabled = settings.localNotifications,
            )
            SettingsExplainer("Android notification controls remain available in system settings.")
            Button(
                onClick = {
                    context.startActivity(notificationSettingsIntent(context))
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Open Android notification settings") }
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("Theme")
            AppearancePreference.entries.forEach { preference ->
                ChoiceRow(
                    label = preference.label,
                    selected = settings.appearance == preference,
                    onClick = { onChange(settings.copy(appearance = preference)) },
                )
            }
            SettingsSection("Language")
            ChoiceRow(
                label = "System default (English)",
                selected = settings.language == "System default (English)",
                onClick = { onChange(settings.copy(language = "System default (English)")) },
            )
            ChoiceRow(
                label = "English",
                selected = settings.language == "English",
                onClick = { onChange(settings.copy(language = "English")) },
            )
            SettingsExplainer("English is currently available. Theme changes apply immediately to the active profile.")
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("Device protection")
            SettingsSwitch(
                title = "Hide Screen in Recents",
                checked = settings.hideScreenInRecents,
                onCheckedChange = { onChange(settings.copy(hideScreenInRecents = it)) },
                subtitle = "Uses Android secure-window protection for previews and screenshots.",
            )
            SettingsSwitch(
                title = "Require device authentication",
                checked = settings.requireDeviceAuthentication,
                enabled = secure,
                onCheckedChange = { onChange(settings.copy(requireDeviceAuthentication = it)) },
                subtitle = if (secure) "Require the device screen lock when returning to White Noise." else "Set a device screen lock first.",
            )
            if (!secure) {
                TextButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text("Open Android security settings") }
            }
            SettingsLink("Auto-lock", settings.autoLockDuration.label, onClick = { autoLockPicker = true })
            SettingsExplainer("Security preferences are stored separately for each profile.")
            SettingsSection("Device data")
            SettingsLink(
                title = "Erase App Data",
                subtitle = "Signs out every profile and permanently removes all White Noise data from this device.",
                onClick = { eraseOpen = true },
            )
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("Automatic downloads")
            SettingsLink("Photos", settings.photoDownloadPolicy.label, onClick = { picker = DataPicker.Photos })
            SettingsLink("Videos", settings.videoDownloadPolicy.label, onClick = { picker = DataPicker.Videos })
            SettingsLink("Audio", settings.audioDownloadPolicy.label, onClick = { picker = DataPicker.Audio })
            SettingsLink("Files", settings.fileDownloadPolicy.label, onClick = { picker = DataPicker.Files })
            TextButton(
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
                modifier = Modifier.padding(8.dp),
            ) { Text("Reset download settings") }
            SettingsSection("Sent media")
            SettingsLink("Photo and video quality", settings.sentMediaQuality.label, onClick = { qualityPicker = true })
            SettingsExplainer("Choose when White Noise may download each media type.")
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
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton }
            .clickable(onClick = onClick),
    )
    HorizontalDivider()
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
                    ListItem(
                        headlineContent = { Text(label(value)) },
                        leadingContent = { RadioButton(selected = value == selected, onClick = null) },
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
