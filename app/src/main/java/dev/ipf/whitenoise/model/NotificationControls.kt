package dev.ipf.whitenoise.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class NotifyFor { AllMessages, MentionsOnly }
enum class VibrationChoice { SystemDefault, Short, Double, Long }
enum class NotificationCategory(val channelId: String, val overridable: Boolean = false) {
    DirectMessages("messages_dm"), GroupMessages("messages_group"),
    Mentions("mentions", true), Reactions("reactions_v2", true), Invitations("invites_v2", true),
    GroupMembership("group_membership_v1"), AgentActivity("agent_activity_v1", true), AppUpdates("app_updates_v1");

    companion object {
        fun global(updatesAvailable: Boolean) = entries.filter { it != AppUpdates || updatesAvailable }
        fun forChat(chat: Chat) = listOf(if (chat.isGroup) GroupMessages else DirectMessages) + entries.filter { it.overridable }
    }
}

enum class PushAvailability { Available, BuildNotConfigured, PlayServicesMissing, ProviderNotInitialized }
enum class AndroidVibrationOverride { None, Off, Custom, Short, Double, Long }
data class NotificationEnvironment(
    val push: PushAvailability = PushAvailability.Available,
    val updatesAvailable: Boolean = false,
    val vibrationOverride: AndroidVibrationOverride = AndroidVibrationOverride.None,
    val previewAvailable: Boolean = false,
)
data class EffectiveVibration(val selected: VibrationChoice, val enabled: Boolean, val pattern: VibrationChoice?, val overridden: Boolean)

/** App-owned choices; system settings remain authoritative when the platform adapter reconnects. */
object NotificationControls {
    fun localEnabled(settings: ProfileSettings, permission: Boolean) = permission && settings.localNotifications
    fun pushEnabled(settings: ProfileSettings, permission: Boolean, availability: PushAvailability) =
        localEnabled(settings, permission) && settings.nativePushNotifications && availability == PushAvailability.Available
    fun backgroundEnabled(requested: Boolean, permission: Boolean) = requested && permission
    fun effectiveVibration(selected: VibrationChoice, override: AndroidVibrationOverride) = when (override) {
        AndroidVibrationOverride.None -> EffectiveVibration(selected, true, selected, false)
        AndroidVibrationOverride.Off -> EffectiveVibration(selected, false, null, true)
        AndroidVibrationOverride.Custom -> EffectiveVibration(selected, true, null, true)
        AndroidVibrationOverride.Short -> EffectiveVibration(selected, true, VibrationChoice.Short, true)
        AndroidVibrationOverride.Double -> EffectiveVibration(selected, true, VibrationChoice.Double, true)
        AndroidVibrationOverride.Long -> EffectiveVibration(selected, true, VibrationChoice.Long, true)
    }
    fun usesCustom(chat: Chat, category: NotificationCategory) = category == NotificationCategory.forChat(chat).first() || category in chat.customNotificationCategories
    fun scope(chat: Chat, category: NotificationCategory, custom: Boolean): Chat? {
        if (!category.overridable || category !in NotificationCategory.forChat(chat)) return null
        return chat.copy(customNotificationCategories = if (custom) chat.customNotificationCategories + category else chat.customNotificationCategories - category)
    }
    fun durationMillis(duration: MuteDuration): Long? = when (duration) {
        MuteDuration.OneHour -> 3_600_000L
        MuteDuration.EightHours -> 28_800_000L
        MuteDuration.OneDay -> 86_400_000L
        MuteDuration.OneWeek -> 604_800_000L
        MuteDuration.Always, MuteDuration.Custom -> null
    }
    fun mute(chat: Chat, duration: MuteDuration?, now: Long, customUntil: Long? = null): Chat? {
        if (duration == MuteDuration.Custom && (customUntil == null || customUntil <= now)) return null
        val until = when (duration) {
            null, MuteDuration.Always -> null
            MuteDuration.Custom -> customUntil
            else -> durationMillis(duration)?.let { if (now > Long.MAX_VALUE - it) return null else now + it }
        }
        return chat.copy(muteDuration = duration, mutedUntilMillis = until)
    }
    fun expire(chat: Chat, now: Long) = if (chat.muteDuration != null && chat.mutedUntilMillis?.let { it <= now } == true)
        chat.copy(muteDuration = null, mutedUntilMillis = null) else chat
    fun customUntil(date: LocalDate, time: LocalTime, zone: ZoneId, now: Long): Long? =
        runCatching { date.atTime(time).atZone(zone).toInstant().toEpochMilli() }.getOrNull()?.takeIf { it > now }
}
