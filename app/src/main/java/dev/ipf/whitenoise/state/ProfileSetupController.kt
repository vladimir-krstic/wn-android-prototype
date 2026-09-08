package dev.ipf.whitenoise.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ipf.whitenoise.model.*

/** Attempt-scoped work. Only explicit actions apply changes; completion never activates. */
class ProfileSetupController(
    private val ownsAttempt: (Long) -> Boolean,
    private val applyProfile: (Profile) -> Unit,
) {
    var session by mutableStateOf<ProfileSetupSession?>(null)
        private set

    fun start(id: Long, profile: Profile, scenario: ProfileSetupScenario) {
        session = ProfileSetupSession(id, profile, scenario,
            ProfileSetupStep.entries.map { ProfileSetupCheck(it) },
            SetupProfileDraft(profile.name, profile.about, profile.avatar))
        advance()
    }

    fun cancel() { session = null }

    fun updateDraft(id: Long, draft: SetupProfileDraft) {
        val current = owned(id) ?: return
        if (current.work == null) session = current.copy(draft = draft)
    }

    fun updateDiscoveryUrl(id: Long, value: String) {
        val current = owned(id) ?: return
        if (current.work == null) session = current.copy(discoveryUrl = value, invalidDiscoveryUrl = false)
    }

    fun act(id: Long, step: ProfileSetupStep, action: ProfileSetupAction): Boolean {
        val current = owned(id) ?: return false
        if (action !in current.actions(step)) return false
        if (action == ProfileSetupAction.UseDefaults && ProfileSetupPolicy.defaultRelays.any { ProfileSetupPolicy.discoveryUrl(it.url) == null }) return false
        if (action == ProfileSetupAction.FindSettings && ProfileSetupPolicy.discoveryUrl(current.discoveryUrl) == null) {
            session = current.copy(invalidDiscoveryUrl = true)
            return false
        }
        if (action == ProfileSetupAction.Skip) {
            finishStep(step, ProfileSetupStatus.Skipped)
            advance()
        } else schedule(step, action)
        return true
    }

    fun complete(id: Long, work: ProfileSetupWork): Boolean {
        val current = owned(id) ?: return false
        if (current.work != work) return false
        session = current.copy(work = null)
        when (work.action) {
            ProfileSetupAction.Check -> check(work.step)
            ProfileSetupAction.SaveProfile -> {
                if (current.scenario in setOf(ProfileSetupScenario.Recovery, ProfileSetupScenario.ProfileSaveFailure) && current.failedSaves == 0) {
                    session = session!!.copy(failedSaves = 1)
                    attention(work.step, ProfileSetupIssue.ProfileSave)
                } else {
                    val profile = current.candidate.copy(name = current.draft.name.trim(), about = current.draft.about, avatar = current.draft.avatar)
                    apply(profile)
                    finishStep(work.step)
                }
            }
            ProfileSetupAction.UseDefaults -> {
                // Exact displayed set; no silent fallback following inconclusive discovery.
                apply(current.candidate.copy(settings = current.candidate.settings.copy(relays = ProfileSetupPolicy.defaultRelays),
                    chatRelayUrls = ProfileRelayFixtures.chatMessageUrls(ProfileSetupPolicy.defaultRelays)))
                finishStep(work.step)
            }
            ProfileSetupAction.FindSettings -> {
                // Discovery is read-only. A successful fixture lookup recovers current settings.
                resolveRelays(work.step)
            }
            ProfileSetupAction.Retry -> when (work.step) {
                ProfileSetupStep.Profile -> attention(work.step, ProfileSetupIssue.OptionalProfile)
                ProfileSetupStep.Relays, ProfileSetupStep.Inbox -> resolveRelays(work.step)
                else -> finishStep(work.step)
            }
            ProfileSetupAction.Acknowledge -> finishStep(work.step)
            ProfileSetupAction.Skip -> return false
        }
        advance()
        return true
    }

    private fun resolveRelays(step: ProfileSetupStep) {
        val candidate = session?.candidate ?: return
        val roles = if (step == ProfileSetupStep.Inbox) listOf(RelayRole.Inbox) else listOf(RelayRole.Profile, RelayRole.ChatMessages)
        if (roles.all { ProfileSetupPolicy.hasRoute(candidate, it) }) finishStep(step)
        else attention(step, ProfileSetupIssue.MissingRelays)
    }

    private fun owned(id: Long): ProfileSetupSession? = session?.takeIf { it.id == id && ownsAttempt(id) }

    private fun apply(profile: Profile) {
        session = session!!.copy(candidate = profile)
        applyProfile(profile)
    }

    private fun schedule(step: ProfileSetupStep, action: ProfileSetupAction) {
        val current = session ?: return
        val revision = current.revision + 1
        session = current.copy(revision = revision, work = ProfileSetupWork(revision, step, action),
            checks = current.checks.map { if (it.step == step) it.copy(status = ProfileSetupStatus.Checking) else it })
    }

    private fun finishStep(step: ProfileSetupStep, status: ProfileSetupStatus = ProfileSetupStatus.Done) {
        val current = session ?: return
        session = current.copy(checks = current.checks.map { if (it.step == step) it.copy(status = status, issue = null) else it })
    }

    private fun attention(step: ProfileSetupStep, issue: ProfileSetupIssue) {
        val current = session ?: return
        session = current.copy(checks = current.checks.map { if (it.step == step) it.copy(status = ProfileSetupStatus.Attention, issue = issue) else it })
    }

    private fun advance() {
        val current = session ?: return
        if (current.work != null || current.checks.any { it.status == ProfileSetupStatus.Attention }) return
        current.checks.firstOrNull { it.status == ProfileSetupStatus.Waiting }?.let { schedule(it.step, ProfileSetupAction.Check) }
    }

    private fun check(step: ProfileSetupStep) {
        val current = session ?: return
        val scenario = current.scenario
        if (scenario == ProfileSetupScenario.PartialRelayFailure && step in setOf(ProfileSetupStep.Relays, ProfileSetupStep.Inbox)) {
            val relays = current.candidate.settings.relays.mapIndexed { index, relay ->
                if (index == 0) relay.copy(status = RelayConnectionStatus.Disconnected) else relay
            }
            val profile = current.candidate.copy(settings = current.candidate.settings.copy(relays = relays))
            val roles = if (step == ProfileSetupStep.Inbox) listOf(RelayRole.Inbox) else listOf(RelayRole.Profile, RelayRole.ChatMessages)
            if (roles.all { ProfileSetupPolicy.hasRoute(profile, it) }) finishStep(step)
            else attention(step, ProfileSetupIssue.MissingRelays)
            return
        }
        when (step) {
            ProfileSetupStep.Profile -> when (scenario) {
                ProfileSetupScenario.Recovery, ProfileSetupScenario.ProfileLookupFailure -> attention(step, ProfileSetupIssue.ProfileLookup)
                ProfileSetupScenario.Attention, ProfileSetupScenario.ProfileSaveFailure -> attention(step, ProfileSetupIssue.OptionalProfile)
                else -> finishStep(step)
            }
            ProfileSetupStep.Follows -> finishStep(step, if (scenario in setOf(ProfileSetupScenario.Attention, ProfileSetupScenario.Recovery, ProfileSetupScenario.FollowsSkipped)) ProfileSetupStatus.Skipped else ProfileSetupStatus.Done)
            ProfileSetupStep.Relays -> when {
                scenario in setOf(ProfileSetupScenario.Recovery, ProfileSetupScenario.InconclusiveRelays) -> attention(step, ProfileSetupIssue.InconclusiveRelays)
                scenario in setOf(ProfileSetupScenario.Attention, ProfileSetupScenario.MissingRelays) ||
                    !ProfileSetupPolicy.hasRoute(current.candidate, RelayRole.Profile) || !ProfileSetupPolicy.hasRoute(current.candidate, RelayRole.ChatMessages) -> attention(step, ProfileSetupIssue.MissingRelays)
                else -> finishStep(step)
            }
            ProfileSetupStep.Inbox -> if (scenario == ProfileSetupScenario.Recovery || !ProfileSetupPolicy.hasRoute(current.candidate, RelayRole.Inbox)) attention(step, ProfileSetupIssue.MissingRelays) else finishStep(step)
            ProfileSetupStep.Device -> attention(step, ProfileSetupIssue.Device)
            ProfileSetupStep.Messaging -> if (scenario in setOf(ProfileSetupScenario.Recovery, ProfileSetupScenario.MessagingFailure)) attention(step, ProfileSetupIssue.Messaging) else finishStep(step)
        }
    }
}
