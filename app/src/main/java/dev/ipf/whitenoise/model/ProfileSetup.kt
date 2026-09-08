package dev.ipf.whitenoise.model

/** One-shot developer inputs; never rendered by the onboarding UI. */
enum class ProfileSetupScenario(val developerLabel: String) {
    Ready("Ready profile"),
    Attention("Needs attention"),
    Recovery("Recovery required"),
    ProfileSaveFailure("Profile save fails once"),
    ProfileLookupFailure("Profile lookup fails once"),
    FollowsSkipped("People you follow skipped"),
    MissingRelays("Relay settings missing"),
    InconclusiveRelays("Relay lookup inconclusive"),
    UnknownDevice("Device discovery inconclusive"),
    PartialRelayFailure("One relay fails; another works"),
    MessagingFailure("Secure messaging fails once"),
}

enum class ProfileSetupStep(val required: Boolean) {
    Profile(false), Follows(false), Relays(true), Inbox(true), Device(true), Messaging(true),
}
enum class ProfileSetupStatus { Waiting, Checking, Done, Skipped, Attention }
enum class ProfileSetupIssue { OptionalProfile, ProfileLookup, MissingRelays, InconclusiveRelays, Device, Messaging, ProfileSave }
enum class ProfileSetupAction { Check, Skip, Retry, SaveProfile, UseDefaults, FindSettings, Acknowledge }
enum class SetupDeviceDiscovery { NoneFound, Possible, Unknown }

data class ProfileSetupCheck(
    val step: ProfileSetupStep,
    val status: ProfileSetupStatus = ProfileSetupStatus.Waiting,
    val issue: ProfileSetupIssue? = null,
)

data class SetupProfileDraft(val name: String, val about: String, val avatar: ProfileAvatar)

data class ProfileSetupWork(val revision: Long, val step: ProfileSetupStep, val action: ProfileSetupAction)

data class ProfileSetupSession(
    val id: Long,
    val candidate: Profile,
    val scenario: ProfileSetupScenario,
    val checks: List<ProfileSetupCheck>,
    val draft: SetupProfileDraft,
    val work: ProfileSetupWork? = null,
    val revision: Long = 0,
    val failedSaves: Int = 0,
    val discoveryUrl: String = "",
    val invalidDiscoveryUrl: Boolean = false,
) {
    val ready: Boolean get() = checks.filter { it.step.required }.all { it.status == ProfileSetupStatus.Done } &&
        RelayRole.entries.all { ProfileSetupPolicy.hasRoute(candidate, it) }
    val deviceDiscovery: SetupDeviceDiscovery get() = when (scenario) {
        ProfileSetupScenario.Recovery -> SetupDeviceDiscovery.Possible
        ProfileSetupScenario.UnknownDevice -> SetupDeviceDiscovery.Unknown
        else -> SetupDeviceDiscovery.NoneFound
    }
    fun check(step: ProfileSetupStep): ProfileSetupCheck = checks.first { it.step == step }
    fun actions(step: ProfileSetupStep): Set<ProfileSetupAction> {
        if (work != null || check(step).status != ProfileSetupStatus.Attention) return emptySet()
        return when (check(step).issue) {
            ProfileSetupIssue.OptionalProfile, ProfileSetupIssue.ProfileSave -> setOf(ProfileSetupAction.SaveProfile, ProfileSetupAction.Skip)
            ProfileSetupIssue.ProfileLookup -> setOf(ProfileSetupAction.Retry, ProfileSetupAction.Skip)
            ProfileSetupIssue.MissingRelays -> setOf(ProfileSetupAction.UseDefaults, ProfileSetupAction.FindSettings)
            ProfileSetupIssue.InconclusiveRelays -> setOf(ProfileSetupAction.Retry, ProfileSetupAction.FindSettings)
            ProfileSetupIssue.Device -> setOf(ProfileSetupAction.Acknowledge)
            ProfileSetupIssue.Messaging -> setOf(ProfileSetupAction.Retry)
            else -> emptySet()
        }
    }
}

object ProfileSetupPolicy {
    val defaultRelays: List<ProfileRelay> get() = ProfileRelayFixtures.defaults
    fun hasRoute(profile: Profile, role: RelayRole): Boolean =
        ProfileRelayFixtures.availability(profile.settings.relays.filter { discoveryUrl(it.url) != null }, role) == RelayRoleAvailability.Available

    fun discoveryUrl(value: String): String? {
        if (value.any { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }) return null
        val normalized = ProfileRelayFixtures.normalize(value) ?: return null
        if (normalized.any { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }) return null
        val uri = runCatching { java.net.URI(normalized) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase()
        if (uri.port !in -1..65535 || uri.port == 0) return null
        // No name resolution/network access. Reject local and literal IP destinations.
        if (!host.contains('.') || host.endsWith(".localhost") || host.endsWith(".local") ||
            host.startsWith("[") || host.all { it.isDigit() || it == '.' }) return null
        return normalized
    }
}
