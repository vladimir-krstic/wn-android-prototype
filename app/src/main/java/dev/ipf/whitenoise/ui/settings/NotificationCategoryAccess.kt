package dev.ipf.whitenoise.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dev.ipf.whitenoise.model.NotificationCategory

enum class NotificationSettingsOpen { Category, AppFallback, Unavailable }

/** Existing Android settings only. Creating channels or publishing conversations is a production seam. */
internal fun openNotificationCategory(context: Context, category: NotificationCategory, custom: Boolean = false): NotificationSettingsOpen {
    if (!custom && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val exists = runCatching { manager?.getNotificationChannel(category.channelId) != null }.getOrDefault(false)
        if (exists && runCatching { context.startActivity(notificationCategoryIntent(context,category.channelId)) }.isSuccess)
            return NotificationSettingsOpen.Category
    }
    return if (runCatching { context.startActivity(notificationSettingsIntent(context)) }.isSuccess)
        NotificationSettingsOpen.AppFallback else NotificationSettingsOpen.Unavailable
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
internal fun notificationCategoryIntent(context: Context, channelId: String) = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
    .putExtra(Settings.EXTRA_CHANNEL_ID,channelId)
