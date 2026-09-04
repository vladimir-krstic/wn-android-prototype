package dev.ipf.whitenoise.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ipf.whitenoise.model.*

/** Route- and profile-owned fixed relay-list outcomes; never performs relay or network work. */
class RelayPublicationController(
    private val profiles: () -> List<Profile>,
    private val activeId: () -> String?,
    private val signedIn: (String) -> Boolean,
) {
    var projections by mutableStateOf<Map<String, RelayPublicationProjection>>(emptyMap()); private set
    var work by mutableStateOf<RelayPublicationWork?>(null); private set
    var scenario by mutableStateOf(RelayPublicationScenario.Current); private set
    private var scenarioProfile: String? = null
    private var owner: Pair<String, String>? = null
    private var sequence = 0L

    fun projection(profile: Profile): RelayPublicationProjection = projections[profile.id] ?: RelayPublicationProjection()
    private fun active(): Profile? = profiles().firstOrNull { it.id == activeId() && signedIn(it.id) }

    fun open(profileId: String, surface: String) {
        if (active()?.id != profileId) return
        if (owner != profileId to surface) {
            owner = profileId to surface
            work = null
        }
        if (profileId !in projections) projections = projections + (profileId to RelayPublicationProjection())
    }
    fun close(profileId: String, surface: String) {
        if (owner == profileId to surface) {
            owner = null; work = null; sequence++
        }
    }
    fun reconcile() {
        projections = projections.filterKeys { id -> profiles().any { it.id == id } }
        val profile = active()
        if (owner?.first != profile?.id) { owner = null; work = null; sequence++ }
        if (scenarioProfile != null && (scenarioProfile != profile?.id || profile?.developerTools?.isEnabled != true)) {
            scenario = RelayPublicationScenario.Current; scenarioProfile = null
        }
    }
    fun erase() {
        projections = emptyMap(); owner = null; work = null; scenario = RelayPublicationScenario.Current
        scenarioProfile = null; sequence++
    }
    fun chooseScenario(value: RelayPublicationScenario) {
        val profile = active()?.takeIf { it.developerTools.isEnabled } ?: return
        scenarioProfile = profile.id; scenario = value
    }
    fun relaySettingsChanged(profileId: String, before: List<ProfileRelay>, after: List<ProfileRelay>) {
        val kinds = RelayListSignature.capture(before).changedKinds(RelayListSignature.capture(after))
        if (kinds.isEmpty()) return
        val previous = projections[profileId] ?: RelayPublicationProjection()
        projections = projections + (profileId to previous.changed(kinds))
        if (work?.profileId == profileId) { work = null; sequence++ }
    }
    fun begin(operation: RelayPublicationOperation): Boolean {
        val profile = active() ?: return false
        val surface = owner?.takeIf { it.first == profile.id }?.second ?: return false
        if (work?.phase == RelayPublicationWorkPhase.Running) return false
        val projection = projection(profile)
        if (operation == RelayPublicationOperation.PublishMissing && projection.missing.isEmpty()) return false
        work = RelayPublicationWork(++sequence, profile.id, surface, operation, scenario,
            RelayPublicationWorkPhase.Running, projection.revision, RelayListSignature.capture(profile.settings.relays))
        return true
    }
    private fun valid(value: RelayPublicationWork): Profile? = active()?.takeIf { profile ->
        owner == value.profileId to value.surface && profile.id == value.profileId &&
            projection(profile).revision == value.projectionRevision &&
            RelayListSignature.capture(profile.settings.relays) == value.signature
    }
    fun complete(id: Long) {
        val current = work?.takeIf { it.id == id && it.phase == RelayPublicationWorkPhase.Running } ?: return
        val profile = valid(current) ?: run { work = null; return }
        when (current.scenario) {
            RelayPublicationScenario.Failure -> work = current.copy(phase = RelayPublicationWorkPhase.Failed)
            RelayPublicationScenario.Unavailable -> {
                if (current.operation == RelayPublicationOperation.Refresh) {
                    val before = projection(profile)
                    projections = projections + (profile.id to RelayPublicationProjection(
                        phase = RelayProjectionPhase.Unavailable, revision = before.revision + 1))
                }
                work = current.copy(phase = RelayPublicationWorkPhase.Unavailable,
                    projectionRevision = projection(profile).revision)
            }
            else -> {
                val before = projection(profile)
                val missing = if (current.operation == RelayPublicationOperation.PublishMissing) emptySet() else when (current.scenario) {
                    RelayPublicationScenario.Current -> before.missing
                    RelayPublicationScenario.MissingPosting -> setOf(PublishedRelayList.Posting)
                    RelayPublicationScenario.MissingInbox -> setOf(PublishedRelayList.Inbox)
                    RelayPublicationScenario.MissingBoth -> PublishedRelayList.entries.toSet()
                    else -> emptySet()
                }
                projections = projections + (profile.id to RelayPublicationProjection(
                    phase = if (missing.isEmpty()) RelayProjectionPhase.Published else RelayProjectionPhase.Missing,
                    missing = missing, revision = before.revision + 1))
                work = current.copy(phase = RelayPublicationWorkPhase.Complete,
                    projectionRevision = projection(profile).revision)
            }
        }
    }
    fun retry(id: Long) {
        val current = work?.takeIf { it.id == id && it.phase in setOf(
            RelayPublicationWorkPhase.Failed, RelayPublicationWorkPhase.Unavailable) } ?: return
        val profile = active()?.takeIf { owner == current.profileId to current.surface } ?: run { work = null; return }
        val projection = projection(profile)
        work = current.copy(id = ++sequence, scenario = RelayPublicationScenario.Published,
            phase = RelayPublicationWorkPhase.Running, projectionRevision = projection.revision,
            signature = RelayListSignature.capture(profile.settings.relays))
    }
}
