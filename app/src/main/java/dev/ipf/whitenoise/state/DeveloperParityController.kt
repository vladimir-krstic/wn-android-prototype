package dev.ipf.whitenoise.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ipf.whitenoise.model.*

/** A single route-scoped operation lease. Only fixed local inspection data is changed. */
class DeveloperParityController(
    private val active: () -> Profile?,
    private val signedIn: (String) -> Boolean,
    private val profileCount: () -> Int,
    private val update: (String, (Profile) -> Profile) -> Boolean,
    val performanceAvailable: Boolean = true,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    var work by mutableStateOf<DeveloperWork?>(null); private set
    var outcome by mutableStateOf(DeveloperOutcome.Success); private set
    private var sequence = 0L
    private var owner: Pair<String, String>? = null
    private fun profile(): Profile? = active()?.takeIf { signedIn(it.id) && it.developerTools.isEnabled }

    fun open(profileId: String, surface: String) {
        if (profile()?.id != profileId || owner == profileId to surface) return
        owner = profileId to surface
        work = null
        when {
            surface == "packages" -> begin(DeveloperOperation.RefreshPackages)
            surface == "diagnostics" -> begin(DeveloperOperation.RefreshHealth)
            surface.startsWith("push:") -> begin(DeveloperOperation.RefreshPush)
        }
    }
    fun close(profileId: String, surface: String) {
        if (owner == profileId to surface) cancel()
    }
    fun cancel() { owner = null; work = null; sequence++ }
    fun chooseOutcome(value: DeveloperOutcome) { if (profile() != null) outcome = value }
    fun inventoryExample(example: PackageInventoryExample) {
        val p = profile() ?: return
        work = null
        val material = p.developerTools.keyPackage.copy(relays = p.settings.relays.map { it.url }.distinct())
        val remote = material.copy(id = "relay-material", local = false)
        val records = when (example) {
            PackageInventoryExample.Published -> listOf(material)
            PackageInventoryExample.Retained -> listOf(material.copy(relays = emptyList()))
            PackageInventoryExample.Empty -> emptyList()
            PackageInventoryExample.RelayOnly -> listOf(remote)
            PackageInventoryExample.Mixed -> listOf(material, remote)
        }
        update(p.id) { it.copy(connectionInformationPublished = records.any { kp -> kp.relays.isNotEmpty() },
            developerTools = it.developerTools.copy(keyPackages = records, packageRevision = it.developerTools.packageRevision + 1)) }
    }
    fun streaming(enabled: Boolean) {
        val p = profile() ?: return
        update(p.id) { it.copy(developerTools = it.developerTools.copy(streamingDebug = enabled)) }
    }
    fun performance(enabled: Boolean) {
        val p = profile() ?: return
        if (enabled && !performanceAvailable) return
        if (enabled && remainingMillis() > 0) return
        val current = now()
        update(p.id) { it.copy(developerTools = it.developerTools.copy(performanceUntilMillis =
            if (enabled) current + DeveloperInspection.PerformanceDurationMillis else null)) }
    }
    fun remainingMillis(): Long {
        val p = profile() ?: return 0
        if (!performanceAvailable) return 0
        return ((p.developerTools.performanceUntilMillis ?: return 0) - now()).coerceAtLeast(0)
    }
    fun begin(operation: DeveloperOperation, targetId: String? = null): Boolean {
        val p = profile() ?: return false
        val route = owner?.takeIf { it.first == p.id }?.second ?: return false
        if (work?.phase in listOf(DeveloperPhase.Running, DeveloperPhase.Confirm)) return false
        val allowed = when (operation) {
            DeveloperOperation.RefreshPackages, DeveloperOperation.Republish, DeveloperOperation.PublishNew, DeveloperOperation.DeletePackage -> route == "packages"
            DeveloperOperation.RefreshHealth, DeveloperOperation.SendToSelf -> route == "diagnostics"
            DeveloperOperation.RefreshPush -> route.startsWith("push:") && p.developerTools.isConversationDebugEnabled && p.chats.any { it.id == route.removePrefix("push:") }
        }
        if (!allowed) return false
        val packages = DeveloperInspection.packages(p)
        val candidate = when (operation) {
            DeveloperOperation.PublishNew -> if (p.developerTools.packageRevision == 0) KeyPackage.PublishedFixture
                else KeyPackage("package-${p.developerTools.packageRevision + 1}", "Just now", "4 KB")
            DeveloperOperation.Republish -> packages.firstOrNull { it.id == p.developerTools.keyPackage.id && it.local } ?: return false
            DeveloperOperation.DeletePackage -> packages.firstOrNull { it.id == targetId && it.relays.isNotEmpty() } ?: return false
            else -> null
        }
        work = DeveloperWork(++sequence, p.id, route, operation, outcome,
            if (operation == DeveloperOperation.DeletePackage) DeveloperPhase.Confirm else DeveloperPhase.Running,
            candidate, targetId, p.developerTools.packageRevision, p.connectionInformationPublished, p.settings.relays.map { it.url }.distinct())
        return true
    }
    private fun valid(w: DeveloperWork): Profile? = profile()?.takeIf {
        it.id == w.profileId && owner == w.profileId to w.surface &&
            it.developerTools.packageRevision == w.revision && it.connectionInformationPublished == w.publishedBefore &&
            it.settings.relays.map { relay -> relay.url }.distinct() == w.relayUrls &&
            (!w.surface.startsWith("push:") || (it.developerTools.isConversationDebugEnabled && it.chats.any { c -> c.id == w.surface.removePrefix("push:") }))
    }
    fun confirm(id: Long) {
        val w = work?.takeIf { it.id == id && it.phase == DeveloperPhase.Confirm } ?: return
        if (valid(w) == null) { work = null; return }
        work = w.copy(phase = DeveloperPhase.Running)
    }
    fun dismiss(id: Long) { if (work?.id == id) work = null }
    fun retry(id: Long) {
        val w = work?.takeIf { it.id == id && it.phase in listOf(DeveloperPhase.Failed, DeveloperPhase.Partial, DeveloperPhase.Unavailable) } ?: return
        val p = valid(w) ?: run { work = null; return }
        work = w.copy(id = ++sequence, outcome = DeveloperOutcome.Success, phase = DeveloperPhase.Running,
            revision = p.developerTools.packageRevision, publishedBefore = p.connectionInformationPublished)
    }
    fun complete(id: Long) {
        val w = work?.takeIf { it.id == id && it.phase == DeveloperPhase.Running } ?: return
        val p = valid(w) ?: run { work = null; return }
        if (w.outcome == DeveloperOutcome.Failure || w.outcome == DeveloperOutcome.Unavailable) {
            work = w.copy(phase = if (w.outcome == DeveloperOutcome.Failure) DeveloperPhase.Failed else DeveloperPhase.Unavailable)
            return
        }
        val relays = p.settings.relays.map { it.url }.distinct()
        if (w.operation in listOf(DeveloperOperation.PublishNew, DeveloperOperation.Republish, DeveloperOperation.SendToSelf) && relays.isEmpty()) {
            work = w.copy(phase = DeveloperPhase.Unavailable); return
        }
        var partial = false
        val changed = when (w.operation) {
            DeveloperOperation.RefreshPackages -> p.copy(developerTools = p.developerTools.copy(keyPackages = DeveloperInspection.packages(p)))
            DeveloperOperation.PublishNew, DeveloperOperation.Republish -> {
                val material = w.candidate ?: return
                val successful = if (w.outcome == DeveloperOutcome.Partial && relays.size > 1) relays.take(1) else relays
                partial = successful.size < relays.size
                val old = DeveloperInspection.packages(p)
                val previous = old.firstOrNull { it.id == material.id }
                val published = material.copy(published = "Just now", relays = (previous?.relays.orEmpty() + successful).distinct())
                val records = old.filterNot { it.id == material.id }.map {
                    if (w.operation == DeveloperOperation.PublishNew && it.id == p.developerTools.keyPackage.id) it.copy(relays = emptyList()) else it
                }.filter { it.local || it.relays.isNotEmpty() } + published
                p.copy(connectionInformationPublished = true, developerTools = p.developerTools.copy(
                    keyPackage = material.copy(published = "Just now", relays = emptyList()),
                    keyPackages = records, packageRevision = p.developerTools.packageRevision + 1))
            }
            DeveloperOperation.DeletePackage -> {
                val target = DeveloperInspection.packages(p).firstOrNull { it.id == w.targetId } ?: return
                val remaining = if (w.outcome == DeveloperOutcome.Partial && target.relays.size > 1) target.relays.drop(1) else emptyList()
                partial = remaining.isNotEmpty()
                val records = DeveloperInspection.packages(p).map { if (it.id == target.id) it.copy(relays = remaining) else it }
                    .filter { it.local || it.relays.isNotEmpty() }
                p.copy(connectionInformationPublished = records.any { it.relays.isNotEmpty() }, developerTools = p.developerTools.copy(
                    keyPackages = records, packageRevision = p.developerTools.packageRevision + 1))
            }
            DeveloperOperation.RefreshHealth -> p.copy(developerTools = p.developerTools.copy(health = DeveloperInspection.health(p, profileCount())))
            DeveloperOperation.SendToSelf -> p.copy(developerTools = p.developerTools.copy(diagnosticEvents =
                (p.developerTools.diagnosticEvents + DiagnosticEvent("self-${w.id}", "Send to self succeeded; temporary chat removed")).takeLast(256)))
            DeveloperOperation.RefreshPush -> p
        }
        if (!update(p.id) { changed }) { work = null; return }
        work = w.copy(phase = if (partial) DeveloperPhase.Partial else DeveloperPhase.Complete,
            revision = changed.developerTools.packageRevision, publishedBefore = changed.connectionInformationPublished)
    }
}
