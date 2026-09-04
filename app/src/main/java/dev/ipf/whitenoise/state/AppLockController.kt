package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

enum class AppLockPhase { Unlocked, Evaluating, Locked, Authenticating }
enum class AppUnlockOutcome(val label: String) { Success("Unlock succeeds"), Cancelled("Unlock cancelled"), Failed("Credential not recognized"), LockedOut("Temporary lockout"), Unavailable("Authentication unavailable") }
data class AppUnlockRequest(val id: Long, val profileId: String, val outcome: AppUnlockOutcome)

/** App-owned lock lifecycle. The future credential adapter completes only its captured request. */
@Stable
class AppLockController(private val active: () -> Profile?, private val signedIn: (String) -> Boolean,
    private val gateChanged: (Boolean) -> Unit = {}) {
    var phase by mutableStateOf(AppLockPhase.Unlocked); private set
    var credentialAvailable by mutableStateOf<Boolean?>(null); private set
    var foreground by mutableStateOf(true); private set
    var request by mutableStateOf<AppUnlockRequest?>(null); private set
    var failure by mutableStateOf<AppUnlockOutcome?>(null); private set
    var autoPrompt by mutableStateOf(false); private set
    var scenario by mutableStateOf(AppUnlockOutcome.Success); private set
    private var scenarioOwner: String? = null
    private var owner: String? = null
    private var enabled = false
    private var generation = 0L
    private var backgroundAt: Long? = null
    private var lastNow = 0L
    val blocked get() = phase != AppLockPhase.Unlocked
    fun protects(profile: Profile?) = profile?.settings?.requireDeviceAuthentication == true && credentialAvailable != false &&
        (profile.id != owner || !enabled || blocked)
    val shieldsBackground get() = !foreground && enabled
    private fun profile() = active()?.takeIf { signedIn(it.id) }
    private fun time(value: Long): Long { lastNow = maxOf(lastNow,value.coerceAtLeast(0)); return lastNow }
    fun sync() {
        val p = profile()
        val changedOwner = p?.id != owner
        val becameEnabled = p?.settings?.requireDeviceAuthentication == true && (!enabled || changedOwner)
        if (changedOwner) {
            owner = p?.id; backgroundAt = null; request = null; failure = null; generation++
            scenario = AppUnlockOutcome.Success; scenarioOwner = null
        }
        if (scenarioOwner != null && (scenarioOwner != p?.id || p?.developerTools?.isEnabled != true)) { scenario = AppUnlockOutcome.Success; scenarioOwner = null }
        enabled = p?.settings?.requireDeviceAuthentication == true
        when {
            !enabled || credentialAvailable == false -> unlockState()
            credentialAvailable == null -> changePhase(AppLockPhase.Evaluating)
            becameEnabled || changedOwner || phase == AppLockPhase.Evaluating -> lock()
        }
    }
    fun credentials(available: Boolean) { val restored = credentialAvailable == false && available; credentialAvailable = available; sync(); if (restored && enabled) lock() }
    fun choose(value: AppUnlockOutcome) { profile()?.takeIf { it.developerTools.isEnabled }?.let { scenario = value; scenarioOwner = it.id } }
    fun background(nowMillis: Long) {
        sync(); val now = time(nowMillis)
        if (!foreground) return
        foreground = false
        if (enabled && credentialAvailable == true && phase == AppLockPhase.Unlocked) backgroundAt = now
        if (phase == AppLockPhase.Authenticating) cancel()
    }
    fun resume(nowMillis: Long) {
        val now = time(nowMillis); sync(); foreground = true
        if (enabled && credentialAvailable == true && phase == AppLockPhase.Unlocked &&
            backgroundAt?.let { now - it >= profile()!!.settings.autoLockDuration.delayMillis } == true) lock()
        backgroundAt = null
    }
    fun requestUnlock(): Long? {
        sync(); val p = profile() ?: return null
        if (!foreground || !enabled || credentialAvailable != true || phase != AppLockPhase.Locked) return null
        val r = AppUnlockRequest(++generation,p.id,scenario)
        request = r; failure = null; autoPrompt = false; changePhase(AppLockPhase.Authenticating)
        return r.id
    }
    fun complete(id: Long, outcome: AppUnlockOutcome? = null): Boolean {
        sync(); val r = request?.takeIf { it.id == id && it.profileId == profile()?.id && phase == AppLockPhase.Authenticating && foreground } ?: return false
        request = null
        when (val result = outcome ?: r.outcome) {
            AppUnlockOutcome.Success -> { backgroundAt = null; unlockState() }
            else -> { failure = result; autoPrompt = false; changePhase(AppLockPhase.Locked) }
        }
        // A scripted failure affects one prompt, so ordinary Retry can recover.
        scenario = AppUnlockOutcome.Success; scenarioOwner = null
        return true
    }
    fun cancel() {
        if (phase == AppLockPhase.Authenticating) { request = null; generation++; failure = AppUnlockOutcome.Cancelled; autoPrompt = false; changePhase(AppLockPhase.Locked) }
    }
    fun lockNow() { sync(); if (enabled && credentialAvailable == true) lock() }
    fun erase() { owner = null; enabled = false; backgroundAt = null; generation++; scenario = AppUnlockOutcome.Success; scenarioOwner = null; unlockState() }
    private fun lock() { request = null; generation++; failure = null; autoPrompt = true; changePhase(AppLockPhase.Locked) }
    private fun unlockState() { request = null; failure = null; autoPrompt = false; changePhase(AppLockPhase.Unlocked) }
    private fun changePhase(value: AppLockPhase) {
        val wasBlocked = blocked; phase = value
        if (wasBlocked != blocked) gateChanged(blocked)
    }
}
