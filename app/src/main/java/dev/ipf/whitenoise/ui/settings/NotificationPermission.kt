package dev.ipf.whitenoise.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class NotificationPermissionStatus {
    NotRequested,
    Allowed,
    Blocked,
}

internal fun projectNotificationPermissionStatus(
    runtimePermissionRequired: Boolean,
    permissionGranted: Boolean,
    notificationsEnabled: Boolean,
    userDecisionRecorded: Boolean,
): NotificationPermissionStatus = when {
    permissionGranted && notificationsEnabled -> NotificationPermissionStatus.Allowed
    !runtimePermissionRequired && notificationsEnabled -> NotificationPermissionStatus.Allowed
    runtimePermissionRequired && !userDecisionRecorded -> NotificationPermissionStatus.NotRequested
    else -> NotificationPermissionStatus.Blocked
}

internal data class NotificationPermissionAccess(
    val status: NotificationPermissionStatus,
    val requestPermission: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
internal fun rememberNotificationPermissionAccess(): NotificationPermissionAccess {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRequestAttempted by rememberSaveable { mutableStateOf(false) }
    var status by remember(context) {
        mutableStateOf(
            context.currentNotificationPermissionStatus(
                userDecisionRecorded = permissionRequestAttempted ||
                    context.shouldShowNotificationPermissionRationale(),
            ),
        )
    }
    val refresh = {
        status = context.currentNotificationPermissionStatus(
            userDecisionRecorded = permissionRequestAttempted ||
                context.shouldShowNotificationPermissionRationale(),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionRequestAttempted = !granted &&
            context.shouldShowNotificationPermissionRationale()
        refresh()
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return NotificationPermissionAccess(
        status = status,
        requestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                refresh()
            }
        },
        openSettings = {
            context.startActivity(notificationSettingsIntent(context))
        },
    )
}

private fun Context.currentNotificationPermissionStatus(
    userDecisionRecorded: Boolean,
): NotificationPermissionStatus {
    val runtimePermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permissionGranted = !runtimePermissionRequired ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
    return projectNotificationPermissionStatus(
        runtimePermissionRequired = runtimePermissionRequired,
        permissionGranted = permissionGranted,
        notificationsEnabled = notificationsEnabled,
        userDecisionRecorded = runtimePermissionRequired && userDecisionRecorded,
    )
}

private fun Context.shouldShowNotificationPermissionRationale(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        findActivity()?.let { activity ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } == true

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun notificationSettingsIntent(context: Context): Intent =
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
