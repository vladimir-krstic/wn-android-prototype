package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

enum class NotificationDelivery { Local, Push, Background }
enum class NotificationScenario(val developerLabel: String) {
    Success("Settings save succeeds"), SaveFailure("Settings save fails"), ServiceRejected("Background connection rejected")
}
enum class NotificationFailure { Save, Permission, PushUnavailable, ServiceRejected, ServiceStopped, Changed, ExpiredTime }
sealed interface NotificationChange {
    data class Delivery(val kind: NotificationDelivery, val enabled: Boolean) : NotificationChange
    data class Mute(val duration: MuteDuration?, val untilMillis: Long? = null) : NotificationChange
    data class Mode(val value: NotifyFor) : NotificationChange
    data class Vibration(val value: VibrationChoice) : NotificationChange
    data class Scope(val category: NotificationCategory, val custom: Boolean) : NotificationChange
}
data class NotificationSnapshot(
    val local: Boolean, val push: Boolean, val background: Boolean,
    val mute: MuteDuration?, val until: Long?, val mode: NotifyFor?, val vibration: VibrationChoice?, val categories: Set<NotificationCategory>?,
)
data class NotificationWork(
    val id: Long, val profileId: String, val chatId: String?, val change: NotificationChange,
    val before: NotificationSnapshot, val scenario: NotificationScenario, val failure: NotificationFailure? = null, val attempt: Int = 0,
) { val running get() = failure == null }
enum class VibrationPreviewPhase { Preparing, Playing, Complete, Unavailable }
data class VibrationPreview(val id: Long, val owner: GroupOwner, val pattern: VibrationChoice, val phase: VibrationPreviewPhase)

@Stable
class NotificationController(
    private val active: () -> Profile?, private val signedIn: (String) -> Boolean, private val now: () -> Long,
    private val settings: (String, (ProfileSettings) -> ProfileSettings) -> Boolean,
    private val chat: (GroupOwner, (Chat) -> Chat) -> Boolean,
) {
    var work by mutableStateOf<NotificationWork?>(null); private set
    var preview by mutableStateOf<VibrationPreview?>(null); private set
    var backgroundConnection by mutableStateOf(false); private set
    var environment by mutableStateOf(NotificationEnvironment()); private set
    var scenario by mutableStateOf(NotificationScenario.Success); private set
    var permissionAllowed by mutableStateOf(true); private set
    private var developerOwner: String? = null
    private var sequence = 0L
    private var route: String? = null
    val nowMillis: Long get() = now()
    private fun profile() = active()?.takeIf { signedIn(it.id) }
    private fun snapshot(profile: Profile, chatId: String?): NotificationSnapshot? {
        val c = chatId?.let { id -> profile.chats.firstOrNull { it.id == id } ?: return null }
        return NotificationSnapshot(profile.settings.localNotifications, profile.settings.nativePushNotifications, backgroundConnection,
            c?.muteDuration, c?.mutedUntilMillis, c?.notifyFor, c?.vibration, c?.customNotificationCategories)
    }
    fun choose(value: NotificationScenario) { if (profile()?.developerTools?.isEnabled == true) { developerOwner = profile()?.id; scenario = value } }
    fun chooseEnvironment(value: NotificationEnvironment) { if (profile()?.developerTools?.isEnabled == true) { developerOwner = profile()?.id; environment = value } }
    fun observePermission(allowed: Boolean) { permissionAllowed = allowed }
    fun observeRoute(value: String?) {
        if (route != null && route != value) { work = null; preview = null }
        route = value
    }
    fun reconcile() {
        val p = profile()
        if (developerOwner != null && (developerOwner != p?.id || p?.developerTools?.isEnabled != true)) {
            developerOwner = null; environment = NotificationEnvironment(); scenario = NotificationScenario.Success
        }
        work?.let { if (it.profileId != p?.id || snapshot(p,it.chatId) == null) work = null }
        preview?.let { if (it.owner.profileId != p?.id || p.chats.none { c -> c.id == it.owner.chatId }) preview = null }
    }
    fun request(change: NotificationChange, chatId: String? = null, expectedProfileId: String? = null): Long? {
        reconcile(); val p = profile() ?: return null
        if (expectedProfileId != null && expectedProfileId != p.id) return null
        if ((change is NotificationChange.Delivery) != (chatId == null)) return null
        val before = snapshot(p,chatId) ?: return null
        val id = ++sequence
        work = NotificationWork(id,p.id,chatId,change,before,scenario)
        return id
    }
    fun advance(id: Long, attempt: Int) {
        reconcile(); val w = work?.takeIf { it.id == id && it.attempt == attempt && it.running } ?: return
        val p = profile() ?: return
        if (snapshot(p,w.chatId) != w.before) { fail(w,NotificationFailure.Changed); return }
        val change = w.change
        if (change is NotificationChange.Delivery && change.enabled) {
            if (!permissionAllowed) { fail(w,NotificationFailure.Permission); return }
            if (change.kind == NotificationDelivery.Push && (!p.settings.localNotifications || environment.push != PushAvailability.Available)) {
                fail(w,NotificationFailure.PushUnavailable); return
            }
        }
        if (w.scenario == NotificationScenario.SaveFailure && w.attempt == 0) { fail(w,NotificationFailure.Save); return }
        val accepted = when (change) {
            is NotificationChange.Delivery -> when (change.kind) {
                NotificationDelivery.Background -> {
                    // This preference belongs to the app; enabling local delivery belongs to the initiating profile.
                    if (change.enabled && !p.settings.localNotifications && !settings(p.id) { it.copy(localNotifications = true) }) {
                        fail(w,NotificationFailure.Save); return
                    }
                    if (change.enabled && w.scenario == NotificationScenario.ServiceRejected && w.attempt == 0) {
                        backgroundConnection = false
                        fail(w.copy(before = snapshot(profile() ?: return,w.chatId) ?: return),NotificationFailure.ServiceRejected)
                        return
                    }
                    backgroundConnection = change.enabled
                    true
                }
                NotificationDelivery.Local -> settings(p.id) { current ->
                    current.copy(localNotifications = change.enabled,nativePushNotifications = current.nativePushNotifications && change.enabled)
                }.also { if (it && !change.enabled) backgroundConnection = false }
                NotificationDelivery.Push -> settings(p.id) { it.copy(nativePushNotifications = change.enabled) }
            }
            else -> {
                val owner = GroupOwner(p.id,w.chatId ?: return)
                val current = p.chats.firstOrNull { it.id == owner.chatId } ?: return
                val updated = when (change) {
                    is NotificationChange.Mute -> NotificationControls.mute(current,change.duration,now(),change.untilMillis)
                    is NotificationChange.Mode -> current.copy(notifyFor = change.value)
                    is NotificationChange.Vibration -> current.copy(vibration = change.value)
                    is NotificationChange.Scope -> NotificationControls.scope(current,change.category,change.custom)
                }
                if (updated == null) { fail(w,if (change is NotificationChange.Mute) NotificationFailure.ExpiredTime else NotificationFailure.Changed); return }
                chat(owner) { updated }
            }
        }
        if (accepted) work = null else fail(w,NotificationFailure.Save)
    }
    fun retry(id: Long): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && !it.running } ?: return false
        val p = profile() ?: return false
        val current = snapshot(p,w.chatId) ?: return false
        work = w.copy(before = current, failure = null, attempt = w.attempt + 1)
        return true
    }
    fun cancel(id: Long) { if (work?.id == id) work = null }
    fun stopBackground() {
        val p = profile()?.takeIf { it.developerTools.isEnabled && backgroundConnection } ?: return
        work = null
        backgroundConnection = false
        val current = profile() ?: return
        work = NotificationWork(++sequence,p.id,null,NotificationChange.Delivery(NotificationDelivery.Background,true),snapshot(current,null)!!,
            NotificationScenario.Success,NotificationFailure.ServiceStopped)
    }
    fun preview(chatId: String, pattern: VibrationChoice, expectedProfileId: String? = null): Long? {
        val p = profile()?.takeIf { it.chats.any { c -> c.id == chatId } } ?: return null
        if (expectedProfileId != null && expectedProfileId != p.id) return null
        val id = ++sequence; preview = VibrationPreview(id,GroupOwner(p.id,chatId),pattern,VibrationPreviewPhase.Preparing)
        return id
    }
    fun advancePreview(id: Long, phase: VibrationPreviewPhase) {
        reconcile(); val p = preview?.takeIf { it.id == id && it.phase == phase } ?: return
        preview = p.copy(phase = when (phase) {
            VibrationPreviewPhase.Preparing -> if (environment.previewAvailable) VibrationPreviewPhase.Playing else VibrationPreviewPhase.Unavailable
            VibrationPreviewPhase.Playing -> VibrationPreviewPhase.Complete
            else -> return
        })
    }
    fun eraseAppData() {
        work = null; preview = null; backgroundConnection = false
        environment = NotificationEnvironment(); scenario = NotificationScenario.Success; developerOwner = null; route = null
    }
    fun cancelPreview(owner: GroupOwner? = null) { if (owner == null || preview?.owner == owner) preview = null }
    private fun fail(w: NotificationWork, failure: NotificationFailure) { work = w.copy(failure = failure) }
}
