package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.ChatOrganization
import dev.ipf.whitenoise.model.MessageEditing
import dev.ipf.whitenoise.model.MessageEditAttempt
import dev.ipf.whitenoise.model.MessageEditScenario
import dev.ipf.whitenoise.model.MessageEditPhase
import dev.ipf.whitenoise.model.MessageEditFailure
import dev.ipf.whitenoise.model.ChatFolder
import dev.ipf.whitenoise.model.ChatFolderDraft
import dev.ipf.whitenoise.model.ChatFolders
import dev.ipf.whitenoise.model.GlobalVoiceScenario
import dev.ipf.whitenoise.model.ChatBatchAttempt
import dev.ipf.whitenoise.model.ChatBatchScenario
import dev.ipf.whitenoise.model.ChatBatchPhase
import dev.ipf.whitenoise.model.ChatBatchFailure
import dev.ipf.whitenoise.model.ChatBatchResult
import dev.ipf.whitenoise.model.ChatBulkAction
import dev.ipf.whitenoise.model.ChatConnectionState
import dev.ipf.whitenoise.model.ChatConnectionScenario
import dev.ipf.whitenoise.model.ChatConnectionPhase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.AttachmentPreview
import dev.ipf.whitenoise.model.ChatDeliveryState
import dev.ipf.whitenoise.model.ChatFixtures
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ChatRelayPolicy
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.GroupMember
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.PeopleSearchScenario
import dev.ipf.whitenoise.model.GroupContactScenario
import dev.ipf.whitenoise.model.GroupContactAction
import dev.ipf.whitenoise.model.GroupContactPolicy
import dev.ipf.whitenoise.model.GroupContactResult
import dev.ipf.whitenoise.model.PrivateContactDetails
import dev.ipf.whitenoise.model.CreatedChatOpen
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileEditDraft
import dev.ipf.whitenoise.model.ProfileSaveAttempt
import dev.ipf.whitenoise.model.ProfileSavePhase
import dev.ipf.whitenoise.model.ProfileSaveFailure
import dev.ipf.whitenoise.model.ProfileSaveScenario
import dev.ipf.whitenoise.model.ProfileSettingsPolicy
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.RelayConnectionStatus
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ConversationDebugSnapshot
import dev.ipf.whitenoise.model.ConversationMediaKey
import dev.ipf.whitenoise.model.ConversationMediaProjection
import dev.ipf.whitenoise.model.DiagnosticEvent
import dev.ipf.whitenoise.model.KeyPackage
import dev.ipf.whitenoise.model.ProfileExitDestination
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.model.LinkPreviewDetector
import dev.ipf.whitenoise.model.VoiceMessageFixture
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.model.VoiceDraftSubmission
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.model.visibleText
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.model.AccessAttempt
import dev.ipf.whitenoise.model.AccessFailure
import dev.ipf.whitenoise.model.AccessMethod
import dev.ipf.whitenoise.model.AccessPhase
import dev.ipf.whitenoise.model.AccessScenario
import dev.ipf.whitenoise.model.PrivateKeyState
import dev.ipf.whitenoise.model.PrivateKeyValidator
import dev.ipf.whitenoise.model.ProfileSigningMode
import dev.ipf.whitenoise.model.StartupPhase
import dev.ipf.whitenoise.model.StartupState
import dev.ipf.whitenoise.model.ProfileExitAttempt
import dev.ipf.whitenoise.model.ProfileExitScenario
import dev.ipf.whitenoise.model.ProfileExitStep
import dev.ipf.whitenoise.model.ProfileExitStepResult
import dev.ipf.whitenoise.model.SignOutOptions

data class AppUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String? = null,
    val signedInProfileIds: Set<String> = emptySet(),
    val pendingDiagnosticsProfileId: String? = null,
    val lastRetainedProfileId: String? = null,
) {
    val activeProfile: Profile?
        get() = profiles.firstOrNull { it.id == activeProfileId }

    val signedInProfiles: List<Profile>
        get() = profiles.filter { it.id in signedInProfileIds }

    val retainedProfiles: List<Profile>
        get() = profiles.filterNot { it.id in signedInProfileIds }
            .sortedBy { if (it.id == lastRetainedProfileId) 0 else 1 }

    fun diagnosticsPromptProfile(chatsResumed: Boolean): Profile? = activeProfile?.takeIf {
        chatsResumed && it.id == pendingDiagnosticsProfileId && !it.diagnostics.hasSeenPrompt
    }
}

class AppViewModel(
    initialAccessScenario: AccessScenario = AccessScenario.Success,
    startupFails: Boolean = false,
) : ViewModel() {
    private var createdChatSequence = 0
    private var accessGeneration = 0L

    var accessAttempt by mutableStateOf<AccessAttempt?>(null)
        private set
    var nextAccessScenario by mutableStateOf(initialAccessScenario)
        private set
    var startupState by mutableStateOf(StartupState(fails = startupFails))
        private set
    private var profileExitGeneration = 0L
    var profileExitAttempt by mutableStateOf<ProfileExitAttempt?>(null)
        private set
    var profileExitReport by mutableStateOf<ProfileExitAttempt?>(null)
        private set
    var nextProfileExitScenario by mutableStateOf(ProfileExitScenario.Success)
        private set

    var uiState by mutableStateOf(AppUiState())
        private set

    fun completeSignIn(origin: OnboardingOrigin) {
        cancelAccess()
        startupState = startupState.copy(phase = StartupPhase.Ready)
        val profile = when (origin) {
            OnboardingOrigin.Initial -> ProfileFixtures.marmota
            OnboardingOrigin.AddProfile -> ProfileFixtures.openCircuit
        }
        activate(profile, updatesStoredProfile = false)
        if (origin == OnboardingOrigin.AddProfile) addShowcaseProfiles()
    }

    fun completeSignUp(
        origin: OnboardingOrigin,
        name: String,
        about: String,
        avatar: ProfileAvatar?,
    ) {
        cancelAccess()
        startupState = startupState.copy(phase = StartupPhase.Ready)
        val profile = when (origin) {
            OnboardingOrigin.Initial -> ProfileFixtures.initialSignUp(name, about, avatar)
            OnboardingOrigin.AddProfile -> ProfileFixtures.addedSignUp(name, about, avatar)
        }
        activate(profile, updatesStoredProfile = true)
        if (origin == OnboardingOrigin.AddProfile) addShowcaseProfiles()
    }

    fun selectProfile(profileId: String) {
        if (profileId !in uiState.signedInProfileIds) return
        if (uiState.profiles.none { it.id == profileId }) return
        interruptMessageEdits()
        cancelAccess()
        profileExitAttempt = null
        cancelCreatedChatOpen()
        cancelProfileSave()
        dismissChatBatch()
        uiState = uiState.copy(activeProfileId = profileId, pendingDiagnosticsProfileId = null)
        updateActiveProfile { it.copy(chatConnection = it.chatConnection.copy(generation = it.chatConnection.generation + 1)) }
    }

    fun beginPrivateKeySignIn(origin: OnboardingOrigin, key: String): Boolean {
        if (PrivateKeyValidator.state(key) != PrivateKeyState.Valid) return false
        val candidate = if (origin == OnboardingOrigin.Initial) ProfileFixtures.marmota else ProfileFixtures.openCircuit
        return beginAccess(origin, candidate, AccessMethod.PrivateKey)
    }

    fun beginAmberSignIn(origin: OnboardingOrigin): Boolean = beginAccess(
        origin,
        ProfileFixtures.openCircuit.copy(
            id = "amber-open-circuit",
            publicKey = "npub1" + "z".repeat(58),
            signingMode = ProfileSigningMode.Amber,
        ),
        AccessMethod.Amber,
    )

    fun beginProfileCreation(origin: OnboardingOrigin, name: String, about: String, avatar: ProfileAvatar?): Boolean {
        val candidate = when (origin) {
            OnboardingOrigin.Initial -> ProfileFixtures.initialSignUp(name, about, avatar)
            OnboardingOrigin.AddProfile -> ProfileFixtures.addedSignUp(name, about, avatar)
        }
        return beginAccess(origin, candidate, AccessMethod.CreateProfile)
    }

    fun beginRetainedSignIn(origin: OnboardingOrigin, profileId: String): Boolean {
        val candidate = uiState.retainedProfiles.firstOrNull { it.id == profileId } ?: return false
        return beginAccess(origin, candidate, AccessMethod.Retained)
    }

    private fun beginAccess(origin: OnboardingOrigin, candidate: Profile, method: AccessMethod): Boolean {
        if (accessAttempt?.phase?.isBusy == true || accessAttempt?.phase == AccessPhase.RecoveryConsent) return false
        if ((origin == OnboardingOrigin.Initial) != (uiState.activeProfileId == null)) return false
        val scenario = nextAccessScenario
        nextAccessScenario = AccessScenario.Success
        val attempt = AccessAttempt(
            id = ++accessGeneration,
            origin = origin,
            ownerProfileId = uiState.activeProfileId,
            candidate = candidate,
            method = method,
            phase = AccessPhase.SigningIn,
            scenario = scenario,
        )
        accessAttempt = attempt.copy(phase = attempt.startingPhase())
        return true
    }

    fun advanceAccess(requestId: Long, phase: AccessPhase): Boolean {
        val attempt = accessAttempt ?: return false
        if (attempt.id != requestId || attempt.phase != phase || !phase.isBusy) return false
        if (!ownsAccess(attempt)) {
            cancelAccess()
            return false
        }
        val next = attempt.advance()
        if (next != null) {
            accessAttempt = next
            return false
        }
        val stored = uiState.profiles.firstOrNull { it.id == attempt.candidate.id }
        if (stored != null && stored.signingMode != attempt.candidate.signingMode) {
            accessAttempt = attempt.copy(phase = AccessPhase.Failed, failure = AccessFailure.AmberMismatch)
            return false
        }
        activate(attempt.candidate, updatesStoredProfile = attempt.method == AccessMethod.CreateProfile)
        if (attempt.origin == OnboardingOrigin.AddProfile && attempt.method != AccessMethod.Retained) addShowcaseProfiles()
        accessAttempt = null
        return true
    }

    fun confirmAccessRecovery(requestId: Long) {
        val attempt = accessAttempt ?: return
        if (attempt.id != requestId || attempt.phase != AccessPhase.RecoveryConsent || !ownsAccess(attempt)) return
        accessAttempt = attempt.copy(phase = AccessPhase.Recovering, recoveryAcknowledged = true)
    }

    fun retryAccess(requestId: Long) {
        val attempt = accessAttempt ?: return
        if (attempt.id != requestId || attempt.phase != AccessPhase.Failed || !ownsAccess(attempt)) return
        accessAttempt = attempt.copy(
            id = ++accessGeneration,
            phase = attempt.startingPhase(),
            scenario = AccessScenario.Success,
            failure = null,
        )
    }

    fun cancelAccess() {
        accessGeneration++
        accessAttempt = null
    }

    private fun ownsAccess(attempt: AccessAttempt): Boolean =
        uiState.activeProfileId == attempt.ownerProfileId &&
            (attempt.method != AccessMethod.Retained || uiState.retainedProfiles.any { it.id == attempt.candidate.id })

    fun selectAccessScenario(scenario: AccessScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled != true) return
        nextAccessScenario = scenario
    }

    fun previewStartupFailure() {
        if (uiState.activeProfile?.developerTools?.isEnabled != true) return
        cancelAccess()
        startupState = StartupState(generation = startupState.generation + 1, fails = true)
    }

    fun advanceStartup(generation: Long) {
        if (startupState.generation != generation || startupState.phase != StartupPhase.Loading) return
        startupState = startupState.copy(phase = if (startupState.fails) StartupPhase.Failed else StartupPhase.Ready)
    }

    fun retryStartup() {
        if (startupState.phase != StartupPhase.Failed) return
        startupState = StartupState(generation = startupState.generation + 1)
    }

    fun recoverStartupProfiles() {
        if (startupState.phase != StartupPhase.Failed || uiState.profiles.isEmpty()) return
        interruptMessageEdits()
        cancelAccess()
        uiState = uiState.copy(activeProfileId = null, signedInProfileIds = emptySet(), pendingDiagnosticsProfileId = null)
        startupState = startupState.copy(phase = StartupPhase.Ready)
    }

    var profileImageDraft by mutableStateOf<dev.ipf.whitenoise.model.ProfileImageDraft?>(null)
        private set
    fun retainProfileImages(profileId: String, avatar: ProfileAvatar, banner: ProfileAvatar?) {
        if (uiState.activeProfileId == profileId) profileImageDraft = dev.ipf.whitenoise.model.ProfileImageDraft(profileId, avatar, banner)
    }
    var profileSaveAttempt by mutableStateOf<ProfileSaveAttempt?>(null)
        private set
    var nextProfileSaveScenario by mutableStateOf(ProfileSaveScenario.Success)
        private set
    var nextProfileImageFails by mutableStateOf(false)
        private set
    private var profileSaveGeneration = 0L

    fun selectProfileSaveScenario(scenario: ProfileSaveScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextProfileSaveScenario = scenario
    }
    fun selectProfileImageFailure(fails: Boolean) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextProfileImageFails = fails
    }
    fun consumeProfileImageFailure(profileId: String): Boolean {
        if (uiState.activeProfileId != profileId) return true
        return nextProfileImageFails.also { nextProfileImageFails = false }
    }
    fun beginProfileSave(profileId: String, draft: ProfileEditDraft): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return false
        if (profileSaveAttempt?.isBusy == true || !ProfileSettingsPolicy.canPublishProfile(profile.settings)) return false
        val normalized = draft.normalized() ?: return false
        val checksLightning = normalized.lightningAddress.isNotBlank() && normalized.lightningAddress != profile.lightningAddress
        profileSaveAttempt = ProfileSaveAttempt(++profileSaveGeneration, profileId, normalized,
            if (checksLightning) ProfileSavePhase.CheckingLightning else ProfileSavePhase.Publishing, nextProfileSaveScenario)
        nextProfileSaveScenario = ProfileSaveScenario.Success
        return true
    }
    fun advanceProfileSave(requestId: Long, phase: ProfileSavePhase): Boolean {
        val request = profileSaveAttempt ?: return false
        val profile = uiState.activeProfile ?: return false
        if (request.id != requestId || request.phase != phase || !request.isBusy || request.profileId != profile.id) return false
        val failure = when {
            request.scenario == ProfileSaveScenario.NoConnection || !ProfileSettingsPolicy.canPublishProfile(profile.settings) -> ProfileSaveFailure.NoConnection
            phase == ProfileSavePhase.CheckingLightning && request.scenario == ProfileSaveScenario.UnresolvedLightning -> ProfileSaveFailure.UnresolvedLightning
            phase == ProfileSavePhase.Publishing && request.scenario == ProfileSaveScenario.PublishFailure -> ProfileSaveFailure.PublishFailed
            else -> null
        }
        if (failure != null) { profileSaveAttempt = request.copy(phase = ProfileSavePhase.Failed, failure = failure); return false }
        if (phase == ProfileSavePhase.CheckingLightning) { profileSaveAttempt = request.copy(phase = ProfileSavePhase.Publishing); return false }
        val draft = request.draft
        updateActiveProfile { current -> current.copy(
            name = draft.name, about = draft.about, avatar = draft.avatar, banner = draft.banner,
            nostrAddress = draft.nostrAddress,
            isNostrAddressVerified = if (draft.nostrAddress == current.nostrAddress) current.isNostrAddressVerified else draft.nostrAddress.isNotBlank(),
            lightningAddress = draft.lightningAddress,
            diagnostics = current.diagnostics.copy(records = current.diagnostics.records.map { it.copy(profileName = draft.name) }),
        ) }
        profileSaveAttempt = null
        profileImageDraft = null
        return true
    }
    fun cancelProfileSave(profileId: String? = null) {
        if (profileId == null || profileSaveAttempt?.profileId == profileId) profileSaveAttempt = null
        if (profileId == null || profileImageDraft?.profileId == profileId) profileImageDraft = null
    }

    fun updateActiveProfileDetails(
        name: String,
        about: String,
        avatar: ProfileAvatar,
    ): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false
        var changed = false
        updateActiveProfile { profile ->
            val updated = profile.copy(
                name = trimmedName,
                about = about.trim(),
                avatar = avatar,
                diagnostics = profile.diagnostics.copy(
                    records = profile.diagnostics.records.map { it.copy(profileName = trimmedName) },
                ),
            )
            changed = updated != profile
            updated
        }
        return changed
    }

    fun updateNostrAddress(address: String): Boolean {
        val normalized = address.trim()
        if (!dev.ipf.whitenoise.model.ProfileSettingsPolicy.isValidNostrAddress(normalized)) return false
        var changed = false
        updateActiveProfile { profile ->
            val updated = profile.copy(
                nostrAddress = normalized,
                isNostrAddressVerified = true,
            )
            changed = updated != profile
            updated
        }
        return changed
    }

    fun updateProfileSettings(settings: ProfileSettings): Boolean {
        val normalized = if (settings.localNotifications) {
            settings
        } else {
            settings.copy(nativePushNotifications = false)
        }
        var changed = false
        updateActiveProfile { profile ->
            if (profile.settings == normalized) {
                profile
            } else {
                changed = true
                profile.copy(settings = normalized)
            }
        }
        return changed
    }

    fun addProfileRelay(
        value: String,
        roles: Set<RelayRole> = RelayRole.entries.toSet(),
    ): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val relays = ProfileRelayFixtures.add(profile.settings.relays, value, roles)
                ?: return@updateActiveProfile profile
            changed = true
            val settings = profile.settings.copy(relays = relays)
            profile.copy(
                settings = settings,
                chatRelayUrls = ProfileRelayFixtures.chatMessageUrls(relays),
            )
        }
        return changed
    }

    fun setProfileRelayConnectionStatus(relayId: String, status: RelayConnectionStatus): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val relays = profile.settings.relays.map { relay ->
                if (relay.id != relayId || relay.status == status) relay else {
                    changed = true
                    relay.copy(status = status)
                }
            }
            if (!changed) profile else profile.copy(
                settings = profile.settings.copy(relays = relays),
                chatRelayUrls = ProfileRelayFixtures.chatMessageUrls(relays),
            )
        }
        return changed
    }

    fun removeProfileRelay(relayId: String): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val target = profile.settings.relays.firstOrNull { it.id == relayId }
            if (target == null || target.isReadOnly) return@updateActiveProfile profile
            val relays = profile.settings.relays.filterNot { it.id == relayId }
            changed = true
            profile.copy(
                settings = profile.settings.copy(relays = relays),
                chatRelayUrls = ProfileRelayFixtures.chatMessageUrls(relays),
            )
        }
        return changed
    }

    fun setProfileRelayRole(relayId: String, role: RelayRole, enabled: Boolean): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val relays = profile.settings.relays.map { relay ->
                if (relay.id != relayId || relay.isReadOnly || (role in relay.roles) == enabled) {
                    relay
                } else {
                    changed = true
                    relay.copy(roles = if (enabled) relay.roles + role else relay.roles - role)
                }
            }
            if (!changed) profile else profile.copy(
                settings = profile.settings.copy(relays = relays),
                chatRelayUrls = ProfileRelayFixtures.chatMessageUrls(relays),
            )
        }
        return changed
    }

    fun restoreProfileRelays(): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            if (profile.settings.relays == ProfileRelayFixtures.defaults) return@updateActiveProfile profile
            changed = true
            profile.copy(
                settings = profile.settings.copy(relays = ProfileRelayFixtures.defaults),
                chatRelayUrls = ProfileRelayFixtures.chatMessageUrls(ProfileRelayFixtures.defaults),
            )
        }
        return changed
    }

    fun setDeveloperToolsEnabled(enabled: Boolean): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val tools = profile.developerTools.withEnabled(enabled)
            changed = tools != profile.developerTools
            profile.copy(developerTools = tools)
        }
        return changed
    }

    fun setDebugMode(enabled: Boolean): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled) tools else tools.copy(debugMode = enabled)
    }

    fun setAnalyticsEnabled(profileId: String, enabled: Boolean): Boolean = updateDiagnostics(profileId) {
        it.diagnostics.copy(analyticsEnabled = enabled)
    }

    fun setDiagnosticLoggingEnabled(profileId: String, enabled: Boolean): Boolean = updateDiagnostics(profileId) {
        it.diagnostics.withLogging(enabled, it.id, it.name)
    }

    fun clearDiagnosticRecords(profileId: String): Boolean = updateDiagnostics(profileId) {
        it.diagnostics.clearRecords()
    }

    fun dismissDiagnosticsPrompt(profileId: String) {
        if (uiState.pendingDiagnosticsProfileId != profileId) return
        updateDiagnostics(profileId) { it.diagnostics.copy(hasSeenPrompt = true) }
        uiState = uiState.copy(pendingDiagnosticsProfileId = null)
    }

    private fun updateDiagnostics(profileId: String, transform: (Profile) -> dev.ipf.whitenoise.model.DiagnosticsState): Boolean {
        if (profileId !in uiState.signedInProfileIds) return false
        var changed = false
        uiState = uiState.copy(profiles = uiState.profiles.map { profile ->
            if (profile.id != profileId) profile else {
                val diagnostics = transform(profile)
                changed = diagnostics != profile.diagnostics
                profile.copy(diagnostics = diagnostics)
            }
        })
        return changed
    }

    fun publishKeyPackage(): Boolean {
        val profile = uiState.activeProfile ?: return false
        if (!profile.developerTools.isEnabled ||
            (profile.connectionInformationPublished && profile.developerTools.keyPackage == KeyPackage.PublishedFixture)) return false
        updateActiveProfile { it.copy(
            connectionInformationPublished = true,
            developerTools = it.developerTools.copy(keyPackage = KeyPackage.PublishedFixture),
        ) }
        return true
    }

    fun clearDiagnosticEvents(): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled || tools.diagnosticEvents.isEmpty()) tools else tools.copy(diagnosticEvents = emptyList())
    }

    fun runDiagnosticTest(): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled) {
            tools
        } else {
            val event = DiagnosticEvent(
                id = "diagnostic-test-${tools.diagnosticEvents.size}",
                text = "18:42:15  diagnostic test passed",
            )
            tools.copy(diagnosticEvents = tools.diagnosticEvents + event)
        }
    }

    fun conversationDebugSnapshot(chatId: String): ConversationDebugSnapshot? =
        uiState.activeProfile?.let { ConversationDebugPolicy.snapshot(it, chatId) }

    fun signOutActiveProfile(wipeData: Boolean): ProfileExitDestination? {
        val activeId = uiState.activeProfileId ?: return null
        interruptMessageEdits()
        cancelCreatedChatOpen()
        cancelProfileSave()
        dismissChatBatch()
        cancelAccess()
        val signedIn = uiState.signedInProfileIds - activeId
        val profiles = if (wipeData) uiState.profiles.filterNot { it.id == activeId } else uiState.profiles
        val nextActiveId = profiles.firstOrNull { it.id in signedIn }?.id
        uiState = AppUiState(
            profiles = profiles,
            activeProfileId = nextActiveId,
            signedInProfileIds = signedIn,
            lastRetainedProfileId = activeId.takeUnless { wipeData },
        )
        return if (signedIn.isEmpty()) ProfileExitDestination.Welcome else ProfileExitDestination.ProfileSwitcher
    }

    fun beginProfileExit(options: SignOutOptions): Boolean {
        val profile = uiState.activeProfile ?: return false
        if (profileExitAttempt != null) return false
        if (options.wipeData && !WipeConfirmationPhrase.matches(options.confirmation, profile.name)) return false
        cancelAccess()
        profileExitReport = null
        profileExitAttempt = ProfileExitAttempt(
            id = ++profileExitGeneration, profileId = profile.id, profileName = profile.name,
            options = options.copy(deleteConnectionInformation = options.wipeData || options.deleteConnectionInformation),
            scenario = nextProfileExitScenario,
        )
        nextProfileExitScenario = ProfileExitScenario.Success
        return true
    }

    fun advanceProfileExit(requestId: Long, step: ProfileExitStep): ProfileExitDestination? {
        val attempt = profileExitAttempt ?: return null
        if (attempt.id != requestId || attempt.currentStep != step) return null
        if (attempt.profileId != uiState.activeProfileId) {
            profileExitAttempt = null
            return null
        }
        val next = attempt.advance()
        if (step == ProfileExitStep.RelayCleanup && next.results[step] == ProfileExitStepResult.Done) {
            updateActiveProfile { it.copy(connectionInformationPublished = false) }
        }
        if (step == ProfileExitStep.LeaveGroups && next.results[step] == ProfileExitStepResult.Done) {
            updateActiveProfile { profile -> profile.copy(chats = profile.chats.map { chat ->
                if (!chat.isGroup || chat.membership != ChatMembership.Active) chat else chat.copy(
                    membership = ChatMembership.Left, isPinned = false, unreadCount = 0, isMarkedUnread = false,
                    readState = chat.readState?.copy(unreadIds = emptySet()),
                    members = chat.members.filterNot { it.personId == profile.id },
                    timeline = chat.timeline + ChatTimelineEntry.Event(
                        id = "${chat.id}-wipe-exit-${attempt.id}", text = "You left the group.",
                        dayOrdinal = nextTimelinePosition(chat).first, dayLabel = "Today",
                        minuteOfDay = nextTimelinePosition(chat).second,
                    ),
                )
            }) }
        }
        profileExitAttempt = next
        if (next.isRunning || !next.localCleanupCompleted) return null
        val destination = signOutActiveProfile(next.options.wipeData)
        profileExitReport = next.takeIf { it.hasIncompleteWork }
        profileExitAttempt = null
        cancelCreatedChatOpen()
        cancelProfileSave()
        dismissChatBatch()
        return destination
    }

    fun retryProfileExit(requestId: Long) {
        val attempt = profileExitAttempt ?: return
        if (attempt.id != requestId || attempt.isRunning || attempt.localCleanupCompleted || attempt.profileId != uiState.activeProfileId) return
        profileExitAttempt = attempt.retry(++profileExitGeneration)
    }

    fun dismissProfileExit() {
        if (profileExitAttempt?.isRunning != true) profileExitAttempt = null
    }

    fun dismissProfileExitReport() { profileExitReport = null }

    fun selectProfileExitScenario(scenario: ProfileExitScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextProfileExitScenario = scenario
    }

    fun setLocalKeyAvailable(available: Boolean) {
        if (uiState.activeProfile?.developerTools?.isEnabled != true) return
        updateActiveProfile { it.copy(localKeyAvailable = available) }
    }

    fun retryLocalKeyAccess(profileId: String) {
        if (uiState.activeProfileId == profileId) updateActiveProfile { it.copy(localKeyAvailable = true) }
    }

    fun removeStoredProfile(profileId: String, confirmation: String): Boolean {
        val profile = uiState.profiles.firstOrNull { it.id == profileId } ?: return false
        if (profile.id == uiState.activeProfileId || !WipeConfirmationPhrase.matches(confirmation, profile.name)) {
            return false
        }
        if (accessAttempt?.candidate?.id == profileId) cancelAccess()
        uiState = uiState.copy(
            profiles = uiState.profiles.filterNot { it.id == profileId },
            signedInProfileIds = uiState.signedInProfileIds - profileId,
        )
        return true
    }

    fun eraseAppData(confirmation: String): Boolean {
        val expected = WipeConfirmationPhrase.make(uiState.profiles.map(Profile::id))
        if (!WipeConfirmationPhrase.matches(confirmation, expected)) return false
        cancelAccess()
        nextAccessScenario = AccessScenario.Success
        profileExitAttempt = null
        profileExitReport = null
        nextProfileExitScenario = ProfileExitScenario.Success
        peopleSearchScenario = PeopleSearchScenario.Success
        groupContactScenario = GroupContactScenario.Success
        nextCreatedChatUnavailable = false
        nextProfileSaveScenario = ProfileSaveScenario.Success
        nextProfileImageFails = false
        nextChatBatchScenario = ChatBatchScenario.Success
        nextGlobalVoiceScenario = GlobalVoiceScenario.Success
        nextHistoryScenario = dev.ipf.whitenoise.model.HistoryScenario.Success
        nextMessageEditScenario = MessageEditScenario.Success
        cancelCreatedChatOpen()
        cancelProfileSave()
        dismissChatBatch()
        uiState = AppUiState()
        createdChatSequence = 0
        return true
    }

    private fun updateDeveloperTools(
        transform: (dev.ipf.whitenoise.model.DeveloperToolsState) -> dev.ipf.whitenoise.model.DeveloperToolsState,
    ): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val tools = transform(profile.developerTools)
            changed = tools != profile.developerTools
            profile.copy(developerTools = tools)
        }
        return changed
    }

    fun openChat(chatId: String) {
        val owner = uiState.activeProfileId ?: return
        mutateChat(chatId) { chat ->
            chat.copy(readState = chat.readState ?: dev.ipf.whitenoise.model.ConversationReading.initial(chat, owner))
        }
    }

    fun markConversationVisible(profileId: String, chatId: String, messageIds: Set<String>): Boolean {
        if (uiState.activeProfileId != profileId || messageIds.isEmpty()) return false
        val chat = chat(chatId) ?: return false
        val visible = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().filter { it.id in messageIds && !it.message.isDeleted }.mapTo(hashSetOf()) { it.id }
        if (visible.isEmpty()) return false
        val read = dev.ipf.whitenoise.model.ConversationReading.reconcile(chat.readState ?: dev.ipf.whitenoise.model.ConversationReading.initial(chat, profileId), chat, profileId)
        val changed = dev.ipf.whitenoise.model.ConversationReading.seen(read, visible)
        mutateChat(chatId) { it.copy(readState = changed, unreadCount = changed.unreadIds.size, isMarkedUnread = false) }
        return true
    }

    fun markConversationThrough(profileId: String, chatId: String, messageId: String): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val chat = chat(chatId) ?: return false
        val ordered = dev.ipf.whitenoise.model.ConversationProjection.orderedEntries(chat)
        val index = ordered.indexOfFirst { it is ChatTimelineEntry.Message && !it.message.isDeleted && it.id == messageId }
        if (index < 0) return false
        return markConversationVisible(profileId, chatId, ordered.take(index + 1).mapTo(hashSetOf()) { it.id })
    }

    fun markChatUnread(chatId: String, unread: Boolean) {
        mutateChat(chatId) { chat ->
            chat.copy(
                unreadCount = 0,
                isMarkedUnread = unread,
                readState = chat.readState?.copy(unreadIds = emptySet()),
            )
        }
    }

    fun undoChatListAction(undo: dev.ipf.whitenoise.model.ChatListUndo) {
        if (uiState.activeProfileId != undo.profileId) return
        mutateChat(undo.chatId, undo::restore)
    }

    fun markAllChatsRead() {
        updateActiveProfile { profile ->
            profile.copy(
                chats = profile.chats.map { chat ->
                    if (chat.isArchived) chat else chat.copy(unreadCount = 0, isMarkedUnread = false, readState = chat.readState?.copy(unreadIds = emptySet()))
                },
            )
        }
    }

    fun toggleChatPin(chatId: String) {
        updateActiveProfile { owner ->
            val nextOrder = (ChatOrganization.pinned(owner.chats).maxOfOrNull { it.pinnedOrder ?: it.originalOrder } ?: -1) + 1
            owner.copy(chats = owner.chats.map { chat ->
                if (chat.id != chatId || chat.isArchived) chat else chat.copy(isPinned = !chat.isPinned,
                    pinnedOrder = if (chat.isPinned) null else nextOrder)
            })
        }
    }

    fun setChatMute(chatId: String, duration: MuteDuration?) {
        mutateChat(chatId) { chat -> chat.copy(muteDuration = duration) }
    }

    fun setChatDisappearing(chatId: String, duration: DisappearingDuration): Boolean {
        var changed = false
        mutateChat(chatId) { chat ->
            if (chat.disappearingDuration == duration || chat.membership != ChatMembership.Active) {
                chat
            } else {
                changed = true
                val (day, minute) = nextTimelinePosition(chat)
                chat.copy(
                    disappearingDuration = duration,
                    timeline = chat.timeline + ChatTimelineEntry.Event(
                        id = "$chatId-disappearing-${createdChatSequence++}",
                        text = if (duration == DisappearingDuration.Off) {
                            "You turned off disappearing messages."
                        } else {
                            "You set disappearing messages to ${duration.label}."
                        },
                        dayOrdinal = day,
                        dayLabel = "Today",
                        minuteOfDay = minute,
                    ),
                )
            }
        }
        return changed
    }

    fun setChatArchived(chatId: String, archived: Boolean) {
        mutateChat(chatId) { chat ->
            chat.copy(isArchived = archived, isPinned = chat.isPinned && !archived)
        }
    }

    fun leaveChat(chatId: String): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val chats = profile.chats.map { chat ->
                if (
                    chat.id != chatId ||
                    chat.membership != ChatMembership.Active ||
                    (chat.isGroup && chat.isSoleAdmin(profile.id))
                ) {
                    chat
                } else {
                    changed = true
                    chat.copy(
                        membership = ChatMembership.Left,
                        isPinned = false,
                        readState = chat.readState?.copy(unreadIds = emptySet()),
                        unreadCount = 0,
                        isMarkedUnread = false,
                        members = if (chat.isGroup) chat.members.filterNot { it.personId == profile.id } else chat.members,
                        timeline = chat.timeline + ChatTimelineEntry.Event(
                            id = "$chatId-left-${createdChatSequence++}",
                            text = if (chat.isGroup) "You left the group." else "You left this chat.",
                            dayOrdinal = nextTimelinePosition(chat).first,
                            dayLabel = "Today",
                            minuteOfDay = nextTimelinePosition(chat).second,
                        ),
                    )
                }
            }
            profile.copy(chats = chats)
        }
        return changed
    }

    fun deleteEndedChat(chatId: String): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            val chats = profile.chats.filterNot { chat ->
                (chat.id == chatId && chat.hasEndedMembership).also { if (it) changed = true }
            }
            profile.copy(chats = chats)
        }
        return changed
    }

    private var chatBatchGeneration = 0L
    var chatBatchAttempt by mutableStateOf<ChatBatchAttempt?>(null)
        private set
    var nextChatBatchScenario by mutableStateOf(ChatBatchScenario.Success)
        private set

    fun selectChatBatchScenario(scenario: ChatBatchScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextChatBatchScenario = scenario
    }

    fun movePinnedChat(profileId: String, chatId: String, delta: Int) {
        if (uiState.activeProfileId != profileId) return
        updateActiveProfile { it.copy(chats = ChatOrganization.move(it.chats, chatId, delta)) }
    }

    fun createChatFolder(profileId: String, name: String): String? {
        if (uiState.activeProfileId != profileId || name.isBlank()) return null
        val id = "$profileId-folder-${createdChatSequence++}"
        updateActiveProfile { it.copy(chatFolders = it.chatFolders + ChatFolder(id, name.trim())) }
        return id
    }

    var nextGlobalVoiceScenario by mutableStateOf(GlobalVoiceScenario.Success)
        private set

    var nextHistoryScenario by mutableStateOf(dev.ipf.whitenoise.model.HistoryScenario.Success)
        private set

    fun selectHistoryScenario(value: dev.ipf.whitenoise.model.HistoryScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextHistoryScenario = value
    }

    fun consumeHistoryScenario(profileId: String, operation: dev.ipf.whitenoise.model.HistoryOperation): dev.ipf.whitenoise.model.HistoryScenario {
        if (uiState.activeProfileId != profileId) return dev.ipf.whitenoise.model.HistoryScenario.TargetUnavailable
        if (!nextHistoryScenario.appliesTo(operation)) return dev.ipf.whitenoise.model.HistoryScenario.Success
        return nextHistoryScenario.also { nextHistoryScenario = dev.ipf.whitenoise.model.HistoryScenario.Success }
    }

    fun addConversationArrival(profileId: String, chatId: String, streaming: Boolean = false): Boolean {
        val profile = uiState.activeProfile ?: return false
        if (profile.id != profileId || !profile.developerTools.isEnabled) return false
        val chat = chat(chatId) ?: return false
        if (chat.membership != ChatMembership.Active) return false
        val sender = (chat.kind as? dev.ipf.whitenoise.model.ChatKind.Direct)?.personId
            ?: chat.members.firstOrNull { it.personId != profileId }?.personId ?: profile.people.firstOrNull()?.id ?: return false
        val (day, minute) = nextTimelinePosition(chat)
        val message = ChatMessage("$chatId-arrival-${createdChatSequence++}", sender, day, "Today", minute, "Now",
            if (streaming) "I’m putting the details together…" else "Could you check the plan, @${profile.name}?",
            deliveryState = if (streaming) MessageDeliveryState.Streaming else MessageDeliveryState.Sent)
        val created = dev.ipf.whitenoise.model.GlobalSearchClock.timestamp(message)
        val withMessage = chat.copy(timeline = chat.timeline + ChatTimelineEntry.Message(message.copy(
            createdAtMillis = created, receivedAtMillis = created + 20_000, expiresAtMillis = created + 86_400_000)),
            preview = message.text, previewAuthor = profile.people.firstOrNull { it.id == sender }?.displayName, timestamp = "Now")
        val read = dev.ipf.whitenoise.model.ConversationReading.reconcile(chat.readState ?: dev.ipf.whitenoise.model.ConversationReading.initial(chat, profileId), withMessage, profileId)
        mutateChat(chatId) { withMessage.copy(readState = read, unreadCount = read.unreadIds.size) }
        return true
    }

    fun selectGlobalVoiceScenario(scenario: GlobalVoiceScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextGlobalVoiceScenario = scenario
    }

    fun consumeGlobalVoiceScenario(profileId: String): GlobalVoiceScenario {
        if (uiState.activeProfileId != profileId) return GlobalVoiceScenario.Unavailable
        return nextGlobalVoiceScenario.also { nextGlobalVoiceScenario = GlobalVoiceScenario.Success }
    }

    fun openGlobalSearchMessage(profileId: String, chatId: String, messageId: String): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val message = chat(chatId)?.timeline?.filterIsInstance<ChatTimelineEntry.Message>()?.firstOrNull { it.message.id == messageId }?.message
        if (message == null || message.isDeleted) return false
        openChat(chatId)
        return true
    }

    fun saveChatFolder(profileId: String, folderId: String?, draft: ChatFolderDraft): String? {
        val profile = uiState.activeProfile ?: return null
        val value = draft.normalized()
        if (profile.id != profileId || value.name.isEmpty()) return null
        val existing = profile.chatFolders.firstOrNull { it.id == folderId }
        if (folderId != null && existing == null) return null
        val id = folderId ?: "$profileId-folder-${createdChatSequence++}"
        val folder = ChatFolder(id, value.name, value.chatIds.filterTo(mutableSetOf()) { candidate -> profile.chats.any { it.id == candidate } },
            value.description, value.rule, existing?.systemKind)
        updateActiveProfile { it.copy(chatFolders = if (existing == null) it.chatFolders + folder else it.chatFolders.map { current -> if (current.id == id) folder else current }) }
        return id
    }

    fun deleteChatFolder(profileId: String, folderId: String): Boolean {
        val profile = uiState.activeProfile ?: return false
        if (profile.id != profileId || profile.chatFolders.none { it.id == folderId }) return false
        updateActiveProfile { it.copy(chatFolders = it.chatFolders.filterNot { folder -> folder.id == folderId }) }
        return true
    }

    fun moveChatFolder(profileId: String, folderId: String, delta: Int) {
        if (uiState.activeProfileId != profileId) return
        updateActiveProfile { it.copy(chatFolders = ChatFolders.move(it.chatFolders, folderId, delta)) }
    }

    fun restoreChatFolders(profileId: String) {
        if (uiState.activeProfileId != profileId) return
        updateActiveProfile { it.copy(chatFolders = ChatFolders.restore(it.chatFolders)) }
    }

    fun assignChatFolder(profileId: String, chatId: String, folderId: String): Boolean {
        val profile = uiState.activeProfile ?: return false
        if (profile.id != profileId || profile.chats.none { it.id == chatId } || profile.chatFolders.none { it.id == folderId }) return false
        updateActiveProfile { it.copy(chatFolders = it.chatFolders.map { folder -> if (folder.id == folderId) folder.copy(chatIds = folder.chatIds + chatId) else folder }) }
        return true
    }

    fun beginChatBatch(profileId: String, ids: List<String>, action: ChatBulkAction, folderId: String? = null, leaveFirst: Boolean = false): Boolean {
        val profile = uiState.activeProfile ?: return false
        if (profile.id != profileId || ids.isEmpty() || chatBatchAttempt?.isBusy == true) return false
        if (action == ChatBulkAction.Folder && profile.chatFolders.none { it.id == folderId }) return false
        chatBatchAttempt = ChatBatchAttempt(++chatBatchGeneration, profileId, ids.distinct(), action,
            folderId, leaveFirst, scenario = nextChatBatchScenario)
        nextChatBatchScenario = ChatBatchScenario.Success
        return true
    }

    fun retryChatBatch(): Boolean {
        val attempt = chatBatchAttempt ?: return false
        if (attempt.isBusy || attempt.failedIds.isEmpty()) return false
        return beginChatBatch(attempt.profileId, attempt.failedIds, attempt.action, attempt.folderId, attempt.leaveFirst)
    }

    fun dismissChatBatch() { chatBatchAttempt = null }

    /** Each callback is bound to the exact owner, request, target and stage. */
    fun advanceChatBatch(id: Long, index: Int, phase: ChatBatchPhase): Boolean {
        val request = chatBatchAttempt ?: return false
        val profile = uiState.activeProfile ?: return false
        if (request.id != id || request.index != index || request.phase != phase || !request.isBusy || profile.id != request.profileId) return false
        val target = request.targets[index]
        val chat = profile.chats.firstOrNull { it.id == target }
        fun finish(failure: ChatBatchFailure? = null, consumed: Boolean = false) {
            val next = index + 1
            chatBatchAttempt = request.copy(index = next, phase = if (next == request.targets.size) ChatBatchPhase.Finished else ChatBatchPhase.Applying,
                results = request.results + ChatBatchResult(target, chat?.title.orEmpty(), failure, request.leaveFirst && chat?.membership == ChatMembership.Left), scenarioConsumed = request.scenarioConsumed || consumed)
        }
        if (chat == null) { finish(ChatBatchFailure.Unavailable); return true }
        if (!request.scenarioConsumed && request.scenario == ChatBatchScenario.PartialApply && index == 1) {
            finish(ChatBatchFailure.Unavailable, consumed = true); return true
        }
        if (request.action == ChatBulkAction.Delete) {
            if (request.leaveFirst && chat.membership == ChatMembership.Active && chat.isGroup && chat.members.none { it.personId == profile.id }) {
                finish(ChatBatchFailure.Unavailable); return true
            }
            when (phase) {
                ChatBatchPhase.Applying -> {
                    if (request.leaveFirst && ChatOrganization.requiresAdmin(chat, profile.id)) finish(ChatBatchFailure.NeedsAdmin)
                    else chatBatchAttempt = request.copy(phase = if (request.leaveFirst && ChatOrganization.requiresLeave(chat, profile.id)) ChatBatchPhase.Leaving else ChatBatchPhase.Deleting)
                }
                ChatBatchPhase.Leaving -> {
                    if (ChatOrganization.requiresAdmin(chat, profile.id)) finish(ChatBatchFailure.NeedsAdmin)
                    else if (!request.scenarioConsumed && request.scenario == ChatBatchScenario.LeaveFailure) finish(ChatBatchFailure.LeaveFailed, consumed = true)
                    else if (ChatOrganization.requiresLeave(chat, profile.id) && !leaveChat(target)) finish(ChatBatchFailure.LeaveFailed)
                    else chatBatchAttempt = request.copy(phase = ChatBatchPhase.Deleting)
                }
                ChatBatchPhase.Deleting -> {
                    // Recheck membership: a changed target must never bypass a required leave.
                    if (request.leaveFirst && ChatOrganization.requiresLeave(chat, profile.id)) {
                        chatBatchAttempt = request.copy(phase = ChatBatchPhase.Applying)
                    } else if (!request.scenarioConsumed && request.scenario == ChatBatchScenario.DeleteFailure) finish(ChatBatchFailure.DeleteFailed, consumed = true)
                    else {
                        updateActiveProfile { owner -> owner.copy(chats = owner.chats.filterNot { it.id == target },
                            chatFolders = owner.chatFolders.map { it.copy(chatIds = it.chatIds - target) }) }
                        finish()
                    }
                }
                ChatBatchPhase.Finished -> Unit
            }
        } else {
            when (request.action) {
                ChatBulkAction.Read -> markChatUnread(target, false)
                ChatBulkAction.Unread -> markChatUnread(target, true)
                ChatBulkAction.Archive -> setChatArchived(target, true)
                ChatBulkAction.Unarchive -> setChatArchived(target, false)
                ChatBulkAction.Folder -> {
                    if (profile.chatFolders.none { it.id == request.folderId }) { finish(ChatBatchFailure.Unavailable); return true }
                    updateActiveProfile { owner -> owner.copy(chatFolders = owner.chatFolders.map {
                        if (it.id == request.folderId) it.copy(chatIds = it.chatIds + target) else it
                    }) }
                }
                ChatBulkAction.Delete -> Unit
            }
            finish()
        }
        return true
    }

    fun selectChatConnectionScenario(scenario: ChatConnectionScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled != true) return
        updateActiveProfile { owner -> owner.copy(chatConnection = ChatConnectionState(
            phase = when (scenario) {
                ChatConnectionScenario.Online -> ChatConnectionPhase.Online
                ChatConnectionScenario.Offline, ChatConnectionScenario.RetryFailure -> ChatConnectionPhase.Offline
                ChatConnectionScenario.Connecting -> ChatConnectionPhase.Connecting
                ChatConnectionScenario.CatchingUp -> ChatConnectionPhase.CatchingUp
            }, generation = owner.chatConnection.generation + 1, retryFails = scenario == ChatConnectionScenario.RetryFailure,
        )) }
    }

    fun retryChatConnection(profileId: String) {
        if (uiState.activeProfileId != profileId) return
        updateActiveProfile { owner -> owner.copy(chatConnection = owner.chatConnection.copy(
            phase = ChatConnectionPhase.Connecting, generation = owner.chatConnection.generation + 1,
        )) }
    }

    fun advanceChatConnection(profileId: String, generation: Long, phase: ChatConnectionPhase): Boolean {
        val owner = uiState.activeProfile ?: return false
        if (owner.id != profileId || owner.chatConnection.generation != generation || owner.chatConnection.phase != phase) return false
        if (phase != ChatConnectionPhase.Connecting && phase != ChatConnectionPhase.CatchingUp) return false
        updateActiveProfile { it.copy(chatConnection = it.chatConnection.copy(
            phase = if (phase == ChatConnectionPhase.CatchingUp) ChatConnectionPhase.Online
                else if (it.chatConnection.retryFails) ChatConnectionPhase.Failed else ChatConnectionPhase.CatchingUp,
            retryFails = false,
        )) }
        return true
    }

    var peopleSearchScenario by mutableStateOf(PeopleSearchScenario.Success)
        private set
    var groupContactScenario by mutableStateOf(GroupContactScenario.Success)
        private set
    var nextCreatedChatUnavailable by mutableStateOf(false)
        private set
    var createdChatOpen by mutableStateOf<CreatedChatOpen?>(null)
        private set
    var createdChatProjectionUnavailable by mutableStateOf(false)
        private set
    private var createdChatOpenSequence = 0L

    fun selectPeopleSearchScenario(value: PeopleSearchScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) peopleSearchScenario = value
    }
    fun selectGroupContactScenario(value: GroupContactScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) groupContactScenario = value
    }
    fun setCreatedChatUnavailable(value: Boolean) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextCreatedChatUnavailable = value
    }

    fun acceptDiscoveredPerson(profileId: String, person: Person): String? {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return null
        if (person.publicKey == profile.publicKey || person.id == profileId) return null
        profile.people.firstOrNull { it.publicKey == person.publicKey }?.let { return it.id }
        updateActiveProfile { it.copy(people = it.people + person.copy(nickname = "", privateNotes = "")) }
        return person.id
    }

    fun savePrivateContact(profileId: String, personId: String, nickname: String, notes: String): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return false
        val person = profile.people.firstOrNull { it.id == personId } ?: return false
        val updated = person.copy(nickname = PrivateContactDetails.nickname(nickname), privateNotes = PrivateContactDetails.notes(notes))
        fun contactLabel(attachment: MessageAttachment): MessageAttachment = if (attachment.contactPersonId == personId) {
            attachment.copy(label = "Contact: ${updated.displayName}")
        } else attachment
        updateActiveProfile { current -> current.copy(
            people = current.people.map { if (it.id == personId) updated else it },
            chats = current.chats.map { chat ->
                val timeline = chat.timeline.map { entry -> if (entry is ChatTimelineEntry.Message) {
                    ChatTimelineEntry.Message(entry.message.copy(attachments = entry.message.attachments.map(::contactLabel)))
                } else entry }
                chat.copy(
                    title = if ((chat.kind as? ChatKind.Direct)?.personId == personId) updated.displayName else chat.title,
                    previewAuthor = if (chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().lastOrNull()?.message?.authorId == personId) updated.displayName else chat.previewAuthor,
                    attachmentPreview = if (chat.attachmentPreview is AttachmentPreview.Contact && chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().lastOrNull()?.message?.attachments?.firstOrNull()?.contactPersonId == personId) AttachmentPreview.Contact(updated.displayName) else chat.attachmentPreview,
                    timeline = timeline,
                    draftAttachments = chat.draftAttachments.map(::contactLabel),
                )
            },
        ) }
        return true
    }

    fun applyContactToGroups(profileId: String, personId: String, groupIds: List<String>, action: GroupContactAction): GroupContactResult {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId }
            ?: return GroupContactResult(emptyList(), groupIds.distinct())
        val eligible = GroupContactPolicy.eligible(profile, personId, action).map(Chat::id).toSet()
        val selected = groupIds.distinct()
        val scenario = groupContactScenario
        val unresolved = GroupContactPolicy.unresolved(profile, scenario)
        val completed = mutableListOf<String>()
        val failed = mutableListOf<String>()
        selected.forEachIndexed { index, chatId ->
            val fail = chatId !in eligible || chatId in unresolved ||
                (scenario == GroupContactScenario.PartialApply && index == selected.lastIndex)
            val changed = !fail && when (action) {
                GroupContactAction.Invite -> addGroupMembers(chatId, listOf(personId))
                GroupContactAction.Promote -> setGroupMemberAdmin(chatId, personId, true)
            }
            if (changed) completed += chatId else failed += chatId
        }
        if (scenario == GroupContactScenario.PartialApply) groupContactScenario = GroupContactScenario.Success
        return GroupContactResult(completed, failed)
    }

    fun retryContactRoster(profileId: String) {
        if (uiState.activeProfileId == profileId) groupContactScenario = GroupContactScenario.Success
    }

    fun startDirectConversation(personId: String, origin: String): CreatedChatOpen? {
        createdChatOpen?.let { return it.takeIf { request -> request.profileId == uiState.activeProfileId && request.origin == origin } }
        val chatId = openOrCreateDirectChat(personId) ?: return null
        return prepareCreatedChatOpen(chatId, origin)
    }

    fun startGroupConversation(name: String, description: String, avatar: ProfileAvatar, personIds: List<String>, origin: String): CreatedChatOpen? {
        createdChatOpen?.let { return it.takeIf { request -> request.profileId == uiState.activeProfileId && request.origin == origin } }
        val chatId = createGroup(name, description, avatar, personIds) ?: return null
        return prepareCreatedChatOpen(chatId, origin)
    }

    private fun prepareCreatedChatOpen(chatId: String, origin: String): CreatedChatOpen? {
        val profileId = uiState.activeProfileId ?: return null
        createdChatProjectionUnavailable = nextCreatedChatUnavailable
        nextCreatedChatUnavailable = false
        return CreatedChatOpen(++createdChatOpenSequence, profileId, origin, chatId).also { createdChatOpen = it }
    }

    fun completeCreatedChatOpen(requestId: Long, origin: String): String? {
        val request = createdChatOpen ?: return null
        if (request.id != requestId || request.origin != origin || request.profileId != uiState.activeProfileId || chat(request.chatId) == null) return null
        cancelCreatedChatOpen()
        return request.chatId
    }

    fun cancelCreatedChatOpen() { createdChatOpen = null; createdChatProjectionUnavailable = false }
    fun reconcileCreatedChatOrigin(origin: String?) {
        if (createdChatOpen?.let { it.profileId != uiState.activeProfileId || it.origin != origin } == true) cancelCreatedChatOpen()
    }

    fun toggleFollowing(personId: String) {
        mutatePerson(personId) { it.copy(isFollowing = !it.isFollowing) }
    }

    fun toggleBlocked(personId: String) {
        mutatePerson(personId) { it.copy(isBlocked = !it.isBlocked) }
    }

    fun acceptInvitation(chatId: String): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            profile.copy(
                chats = profile.chats.map { chat ->
                    if (chat.id != chatId || chat.membership != ChatMembership.Invited) {
                        chat
                    } else {
                        changed = true
                        val joinedMembers = if (chat.isGroup && chat.members.none { it.personId == profile.id }) {
                            chat.members + GroupMember(profile.id, GroupRole.Member)
                        } else {
                            chat.members
                        }
                        val joinedEvent = if (chat.isGroup) {
                            val (day, minute) = nextTimelinePosition(chat)
                            listOf(
                                ChatTimelineEntry.Event(
                                    id = "$chatId-event-joined",
                                    text = "You joined the group.",
                                    dayOrdinal = day,
                                    dayLabel = "Today",
                                    minuteOfDay = minute,
                                ),
                            )
                        } else {
                            emptyList()
                        }
                        chat.copy(
                            membership = ChatMembership.Active,
                            invitationInviterName = null,
                            readState = chat.readState?.copy(unreadIds = emptySet()),
                            unreadCount = 0,
                            isMarkedUnread = false,
                            members = joinedMembers,
                            timeline = chat.timeline + joinedEvent,
                        )
                    }
                },
            )
        }
        return changed
    }

    fun declineInvitation(chatId: String): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            profile.copy(
                chats = profile.chats.filterNot { chat ->
                    (chat.id == chatId && chat.membership == ChatMembership.Invited).also {
                        if (it) changed = true
                    }
                },
            )
        }
        return changed
    }

    fun retryMessage(chatId: String, messageId: String): Boolean {
        var changed = false
        mutateChat(chatId) { chat ->
            val timeline = chat.timeline.map { entry ->
                val messageEntry = entry as? ChatTimelineEntry.Message
                if (
                    messageEntry?.message?.id == messageId &&
                    messageEntry.message.deliveryState == MessageDeliveryState.Failed &&
                    messageEntry.message.authorId == uiState.activeProfileId
                ) {
                    changed = true
                    messageEntry.copy(message = messageEntry.message.copy(deliveryState = MessageDeliveryState.Sent))
                } else {
                    entry
                }
            }
            if (changed) chat.copy(timeline = timeline, deliveryState = ChatDeliveryState.None) else chat
        }
        return changed
    }

    private var messageEditGeneration = 0L
    var nextMessageEditScenario by mutableStateOf(MessageEditScenario.Success)
        private set

    fun selectMessageEditScenario(scenario: MessageEditScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextMessageEditScenario = scenario
    }

    fun setCollapseLongMessages(profileId: String, chatId: String, collapse: Boolean) {
        if (uiState.activeProfileId == profileId) mutateChat(chatId) { it.copy(collapseLongMessages = collapse) }
    }

    fun addMessageReadingExample(profileId: String, chatId: String): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId && it.developerTools.isEnabled } ?: return false
        val chat = chat(chatId)?.takeIf { it.composerAvailability(profile) == ComposerAvailability.Available } ?: return false
        val (day, minute) = nextTimelinePosition(chat)
        val message = ChatMessage("$chatId-reading-${createdChatSequence++}", profileId, day, "Today", minute, "Now", dev.ipf.whitenoise.model.MessageReadingExamples.document)
        val original = "# Notes from the trail\n\nMeet at the west gate."
        val time = dev.ipf.whitenoise.model.GlobalSearchClock.timestamp(message)
        val edited = message.copy(editHistory = dev.ipf.whitenoise.model.MessageEditHistory(original, time - 60_000,
            listOf(dev.ipf.whitenoise.model.MessageRevision(++messageEditGeneration, message.text, time))))
        mutateChat(chatId) { it.copy(timeline = it.timeline + ChatTimelineEntry.Message(edited), preview = "Notes from the trail", previewAuthor = "You", timestamp = "Now") }
        return true
    }

    fun beginMessageEdit(profileId: String, chatId: String, messageId: String, text: String, expectedRevision: Int? = null): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return false
        val chat = chat(chatId)?.takeIf { it.composerAvailability(profile) == ComposerAvailability.Available } ?: return false
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.id == messageId }?.message ?: return false
        val revision = message.editHistory?.revisions?.size ?: 0
        if (!MessageEditing.eligible(message, profileId) || !MessageEditing.canSave(message, text) ||
            (expectedRevision != null && expectedRevision != revision)) return false
        val attempt = MessageEditAttempt(++messageEditGeneration, profileId, text.trim(), message.text, revision, nextMessageEditScenario)
        nextMessageEditScenario = MessageEditScenario.Success
        mutateMessage(chatId, messageId) { it.copy(editAttempt = attempt) }
        return true
    }

    fun advanceMessageEdit(profileId: String, chatId: String, messageId: String, requestId: Long): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return false
        val chat = chat(chatId) ?: return false
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.id == messageId }?.message ?: return false
        val attempt = message.editAttempt ?: return false
        if (attempt.id != requestId || attempt.profileId != profileId || attempt.phase != MessageEditPhase.Pending ||
            message.text != attempt.baseText || (message.editHistory?.revisions?.size ?: 0) != attempt.baseRevision) return false
        val failure = when {
            !MessageEditing.eligible(message, profileId) || chat.composerAvailability(profile) != ComposerAvailability.Available ||
                attempt.scenario == MessageEditScenario.Unavailable -> MessageEditFailure.Unavailable
            attempt.scenario == MessageEditScenario.SaveFails -> MessageEditFailure.SaveFailed
            else -> null
        }
        if (failure != null) {
            mutateMessage(chatId, messageId) { it.copy(editAttempt = attempt.copy(phase = MessageEditPhase.Failed, failure = failure)) }
            return false
        }
        mutateMessage(chatId, messageId) { MessageEditing.accept(it, attempt) }
        val current = chat(chatId) ?: return true
        val latest = dev.ipf.whitenoise.model.ConversationProjection.orderedEntries(current).filterIsInstance<ChatTimelineEntry.Message>().lastOrNull()?.message
        if (latest?.id == messageId) mutateChat(chatId) { it.copy(preview = latest.text) }
        return true
    }

    fun retryMessageEdit(profileId: String, chatId: String, messageId: String): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val message = chat(chatId)?.timeline?.filterIsInstance<ChatTimelineEntry.Message>()?.firstOrNull { it.id == messageId }?.message ?: return false
        val failed = message.editAttempt?.takeIf { it.phase == MessageEditPhase.Failed && it.profileId == profileId } ?: return false
        return beginMessageEdit(profileId, chatId, messageId, failed.text, failed.baseRevision)
    }

    fun discardMessageEdit(profileId: String, chatId: String, messageId: String): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val message = chat(chatId)?.timeline?.filterIsInstance<ChatTimelineEntry.Message>()?.firstOrNull { it.id == messageId }?.message ?: return false
        if (message.editAttempt?.profileId != profileId) return false
        mutateMessage(chatId, messageId) { it.copy(editAttempt = null) }
        return true
    }

    /** Leaving an owner/session preserves retryable edits but invalidates all pending completions. */
    fun interruptMessageEdits(profileId: String? = uiState.activeProfileId, chatId: String? = null) {
        if (profileId == null) return
        uiState = uiState.copy(profiles = uiState.profiles.map { profile ->
            if (profile.id != profileId) profile else profile.copy(chats = profile.chats.map { chat ->
                if (chatId != null && chat.id != chatId) chat else chat.copy(timeline = chat.timeline.map { entry ->
                    if (entry !is ChatTimelineEntry.Message || entry.message.editAttempt?.phase != MessageEditPhase.Pending) entry
                    else entry.copy(message = entry.message.copy(editAttempt = entry.message.editAttempt.copy(
                        phase = MessageEditPhase.Failed, failure = MessageEditFailure.Interrupted,
                    )))
                })
            })
        })
    }

    private fun mutateMessage(chatId: String, messageId: String, transform: (ChatMessage) -> ChatMessage) {
        mutateChat(chatId) { chat -> chat.copy(timeline = chat.timeline.map { entry ->
            if (entry is ChatTimelineEntry.Message && entry.id == messageId) entry.copy(message = transform(entry.message)) else entry
        }) }
    }

    fun sendText(chatId: String, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        return sendContent(chatId, trimmed, emptyList(), null)
    }

    fun updateDraftText(chatId: String, text: String) {
        mutateChat(chatId) { chat ->
            chat.copy(
                draftText = text,
                isDraft = text.isNotBlank() || chat.draftAttachments.isNotEmpty() || chat.draftReplyMessageId != null,
            )
        }
    }

    fun addDraftAttachments(chatId: String, attachments: List<MessageAttachment>) {
        if (attachments.isEmpty()) return
        mutateChat(chatId) { chat ->
            val additions = attachments.filter { candidate ->
                chat.draftAttachments.none { it.id == candidate.id }
            }.distinctBy(MessageAttachment::id)
            chat.copy(
                draftAttachments = chat.draftAttachments + additions,
                isDraft = true,
            )
        }
    }

    fun removeDraftAttachment(chatId: String, attachmentId: String) {
        mutateChat(chatId) { chat ->
            val attachments = chat.draftAttachments.filterNot { it.id == attachmentId }
            chat.copy(
                draftAttachments = attachments,
                isDraft = chat.draftText.isNotBlank() || attachments.isNotEmpty() || chat.draftReplyMessageId != null,
            )
        }
    }

    fun suppressDraftLink(chatId: String, url: String?) {
        mutateChat(chatId) { it.copy(suppressedDraftLinkUrl = url) }
    }

    fun cancelDraftReply(chatId: String) {
        mutateChat(chatId) { chat ->
            chat.copy(
                draftReplyMessageId = null,
                isDraft = chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty(),
            )
        }
    }

    fun sendDraft(chatId: String): Boolean {
        val chat = chat(chatId) ?: return false
        val trimmed = chat.draftText.trim()
        val linkPreview = LinkPreviewDetector.first(trimmed)
            ?.takeUnless { it.url == chat.suppressedDraftLinkUrl }
            ?.attachment("$chatId-link-${createdChatSequence + 1}")
        val attachments = chat.draftAttachments + listOfNotNull(linkPreview)
        if (trimmed.isEmpty() && attachments.isEmpty()) return false
        return sendContent(chatId, trimmed, attachments, chat.draftReplyMessageId)
    }

    fun sendVoice(
        chatId: String,
        submission: VoiceDraftSubmission,
    ): Boolean {
        val (text, attachments) = VoiceMessageFixture.result(
            id = "$chatId-voice-${createdChatSequence + 1}",
            format = submission.format,
            editedTranscript = submission.transcript,
            durationSeconds = submission.durationSeconds,
        )
        if (
            (submission.format == VoiceMessageFormat.Text || submission.format == VoiceMessageFormat.Both) &&
            text.isBlank()
        ) {
            return false
        }
        return sendContent(chatId, text, attachments, null)
    }

    fun setDraftReply(chatId: String, messageId: String): Boolean {
        var changed = false
        mutateChat(chatId) { chat ->
            val exists = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
                .any { it.message.id == messageId && !it.message.isDeleted }
            if (!exists || chat.composerAvailability(uiState.activeProfile ?: return@mutateChat chat) != ComposerAvailability.Available) {
                chat
            } else {
                changed = true
                chat.copy(draftReplyMessageId = messageId, isDraft = true)
            }
        }
        return changed
    }

    fun setMessageReaction(
        chatId: String,
        messageId: String,
        emoji: String,
        removeIfSelected: Boolean,
    ): Boolean {
        val profileId = uiState.activeProfileId ?: return false
        var changed = false
        mutateChat(chatId) { chat ->
            val timeline = chat.timeline.map { entry ->
                val messageEntry = entry as? ChatTimelineEntry.Message ?: return@map entry
                val message = messageEntry.message
                if (message.id != messageId || message.isDeleted) return@map entry
                val selected = message.reactions.firstOrNull { profileId in it.personIds }?.emoji
                if (selected == emoji && !removeIfSelected) return@map entry
                var reactions = message.reactions.mapNotNull { reaction ->
                    val people = reaction.personIds.filterNot { it == profileId }
                    if (people.isEmpty()) null else reaction.copy(personIds = people)
                }
                if (!(selected == emoji && removeIfSelected)) {
                    val existingIndex = reactions.indexOfFirst { it.emoji == emoji }
                    reactions = if (existingIndex >= 0) {
                        reactions.mapIndexed { index, reaction ->
                            if (index == existingIndex) reaction.copy(personIds = reaction.personIds + profileId) else reaction
                        }
                    } else {
                        reactions + dev.ipf.whitenoise.model.MessageReaction(emoji, listOf(profileId))
                    }
                }
                changed = true
                messageEntry.copy(message = message.copy(reactions = reactions))
            }
            if (changed) chat.copy(timeline = timeline) else chat
        }
        return changed
    }

    fun setQuickReactions(reactions: List<String>): Boolean {
        if (reactions.size != 6 || reactions.distinct().size != 6) return false
        updateActiveProfile { it.copy(quickReactions = reactions) }
        return true
    }

    fun deleteMessages(
        chatId: String,
        messageIds: Set<String>,
        scope: MessageDeletionScope,
    ): Boolean {
        if (messageIds.isEmpty()) return false
        val profileId = uiState.activeProfileId ?: return false
        var changed = false
        mutateChat(chatId) { chat ->
            val selected = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
                .map(ChatTimelineEntry.Message::message)
                .filter { it.id in messageIds }
            if (selected.size != messageIds.size) return@mutateChat chat
            if (scope == MessageDeletionScope.ForEveryone && !MessageActionPolicy.canDeleteForEveryone(selected, profileId)) {
                return@mutateChat chat
            }
            changed = true
            val timeline = when (scope) {
                MessageDeletionScope.ForMe -> chat.timeline.filterNot { entry ->
                    entry is ChatTimelineEntry.Message && entry.message.id in messageIds
                }
                MessageDeletionScope.ForEveryone -> chat.timeline.map { entry ->
                    if (entry is ChatTimelineEntry.Message && entry.message.id in messageIds) {
                        entry.copy(
                            message = entry.message.copy(
                                text = "",
                                attachments = emptyList(),
                                replyToMessageId = null,
                                reactions = emptyList(),
                                deletionState = dev.ipf.whitenoise.model.MessageDeletionState.DeletedByCurrentProfile,
                                editHistory = null,
                                editAttempt = null,
                            ),
                        )
                    } else {
                        entry
                    }
                }
            }
            val latest = timeline.filterIsInstance<ChatTimelineEntry.Message>().lastOrNull()?.message
            chat.copy(
                timeline = timeline,
                preview = latest?.visibleText(profileId).orEmpty(),
                previewAuthor = latest?.let { if (it.authorId == profileId) "You" else null },
                attachmentPreview = latest?.let { attachmentPreview(it.attachments) },
                draftReplyMessageId = chat.draftReplyMessageId?.takeUnless(messageIds::contains),
                isDraft = chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty() ||
                    (chat.draftReplyMessageId != null && chat.draftReplyMessageId !in messageIds),
            )
        }
        return changed
    }

    fun forwardMessages(
        sourceChatId: String,
        messageIds: Set<String>,
        targetChatIds: List<String>,
    ): Boolean {
        val profile = uiState.activeProfile ?: return false
        val source = profile.chats.firstOrNull { it.id == sourceChatId } ?: return false
        val targets = targetChatIds.distinct()
        if (targets.isEmpty() || targets.size > 5 || sourceChatId in targets) return false
        val selected = source.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .map(ChatTimelineEntry.Message::message)
            .filter { it.id in messageIds }
        if (selected.size != messageIds.size || !MessageActionPolicy.canForward(selected)) return false
        val eligibleTargets = profile.chats.filter { target ->
            target.id in targets && target.composerAvailability(profile) == ComposerAvailability.Available
        }
        if (eligibleTargets.size != targets.size) return false

        updateActiveProfile { active ->
            active.copy(chats = active.chats.map { target ->
                if (target.id !in targets) return@map target
                var minute = nextTimelinePosition(target).second
                val copies = selected.map { sourceMessage ->
                    createdChatSequence += 1
                    ChatTimelineEntry.Message(
                        sourceMessage.copy(
                            id = "${target.id}-forward-$createdChatSequence",
                            authorId = active.id,
                            dayOrdinal = 3,
                            dayLabel = "Today",
                            minuteOfDay = minute++,
                            timeLabel = "Now",
                            replyToMessageId = null,
                            reactions = emptyList(),
                            deliveryState = MessageDeliveryState.Sent,
                            deletionState = dev.ipf.whitenoise.model.MessageDeletionState.None,
                            editHistory = null,
                            editAttempt = null,
                            createdAtMillis = null,
                            receivedAtMillis = null,
                            expiresAtMillis = null,
                        ),
                    )
                }
                val latest = copies.last().message
                target.copy(
                    timeline = target.timeline + copies,
                    preview = latest.text.ifBlank { latest.attachments.firstOrNull()?.label.orEmpty() },
                    previewAuthor = "You",
                    attachmentPreview = attachmentPreview(latest.attachments),
                    timestamp = "Now",
                )
            })
        }
        return true
    }

    fun forwardMediaFrame(
        sourceChatId: String,
        mediaKey: ConversationMediaKey,
        targetChatIds: List<String>,
        accompanyingText: String = "",
    ): Boolean {
        val profile = uiState.activeProfile ?: return false
        val source = profile.chats.firstOrNull { it.id == sourceChatId } ?: return false
        val media = ConversationMediaProjection.items(source, profile).firstOrNull {
            it.key == mediaKey
        } ?: return false
        val targets = targetChatIds.distinct()
        if (targets.isEmpty() || targets.size > 5 || sourceChatId in targets) return false
        val eligibleTargets = profile.chats.filter { target ->
            target.id in targets && target.composerAvailability(profile) == ComposerAvailability.Available
        }
        if (eligibleTargets.size != targets.size) return false
        val text = accompanyingText.trim()

        updateActiveProfile { active ->
            active.copy(chats = active.chats.map { target ->
                if (target.id !in targets) return@map target
                val (day, minute) = nextTimelinePosition(target)
                createdChatSequence += 1
                val sequence = createdChatSequence
                val attachment = media.attachment.copy(
                    id = "${target.id}-media-$sequence",
                    kind = if (media.attachment.kind == MessageAttachmentKind.Video) {
                        MessageAttachmentKind.Video
                    } else {
                        MessageAttachmentKind.Photo
                    },
                    label = if (media.attachment.kind == MessageAttachmentKind.Photos) {
                        "Photo"
                    } else {
                        media.attachment.label
                    },
                    images = listOfNotNull(media.image),
                )
                val forwarded = ChatMessage(
                    id = "${target.id}-forward-media-$sequence",
                    authorId = active.id,
                    dayOrdinal = day,
                    dayLabel = "Today",
                    minuteOfDay = minute,
                    timeLabel = "Now",
                    text = text,
                    attachments = listOf(attachment),
                    deliveryState = MessageDeliveryState.Sent,
                )
                target.copy(
                    timeline = target.timeline + ChatTimelineEntry.Message(forwarded),
                    preview = text.ifBlank { attachment.label },
                    previewAuthor = "You",
                    attachmentPreview = attachmentPreview(listOf(attachment)),
                    timestamp = "Now",
                )
            })
        }
        return true
    }

    fun message(chatId: String, messageId: String): ChatMessage? = chat(chatId)?.timeline
        ?.filterIsInstance<ChatTimelineEntry.Message>()
        ?.firstOrNull { it.message.id == messageId }
        ?.message

    private fun sendContent(
        chatId: String,
        text: String,
        attachments: List<MessageAttachment>,
        replyMessageId: String?,
    ): Boolean {
        var changed = false
        updateActiveProfile { profile ->
            profile.copy(
                chats = profile.chats.map { chat ->
                    if (chat.id != chatId || chat.composerAvailability(profile) != ComposerAvailability.Available) {
                        chat
                    } else {
                        val (day, minute) = nextTimelinePosition(chat)
                        createdChatSequence += 1
                        val preview = text.ifBlank { attachments.firstOrNull()?.label.orEmpty() }
                        changed = true
                        chat.copy(
                            preview = preview,
                            previewAuthor = "You",
                            attachmentPreview = attachmentPreview(attachments),
                            timestamp = "Now",
                            unreadCount = chat.unreadCount,
                            isMarkedUnread = chat.isMarkedUnread,
                            isDraft = false,
                            draftText = "",
                            draftAttachments = emptyList(),
                            suppressedDraftLinkUrl = null,
                            draftReplyMessageId = null,
                            deliveryState = ChatDeliveryState.None,
                            timeline = chat.timeline + ChatTimelineEntry.Message(
                                ChatMessage(
                                    id = "$chatId-message-$createdChatSequence",
                                    authorId = profile.id,
                                    dayOrdinal = day,
                                    dayLabel = "Today",
                                    minuteOfDay = minute,
                                    timeLabel = "Now",
                                    text = text,
                                    attachments = attachments,
                                    replyToMessageId = replyMessageId,
                                ),
                            ),
                        )
                    }
                },
            )
        }
        return changed
    }

    fun openOrCreateSupportChat(): String? {
        val profile = uiState.activeProfile ?: return null
        profile.chats.firstOrNull { it.id == ChatFixtures.SUPPORT_CHAT_ID }?.let { return it.id }
        if (profile.chatRelayUrls.isEmpty()) return null
        val support = Chat(
            id = ChatFixtures.SUPPORT_CHAT_ID,
            originalOrder = 0,
            kind = ChatKind.Direct(ChatFixtures.SUPPORT_CHAT_ID),
            title = "White Noise Support",
            preview = "Ask a question, report a problem, or share a suggestion.",
            relayUrls = profile.chatRelayUrls,
            defaultRelayUrls = profile.chatRelayUrls,
            timeline = listOf(
                ChatTimelineEntry.Notice(
                    id = "support-guidance",
                    text = "How can we help? Ask a question, report a problem, or share a suggestion. We’ll reply here.",
                    dayLabel = "",
                ),
            ),
        )
        updateActiveProfile { current -> current.copy(chats = insertAfterPinned(current.chats, support)) }
        return support.id
    }

    fun composerAvailability(chatId: String): ComposerAvailability? {
        val profile = uiState.activeProfile ?: return null
        return profile.chats.firstOrNull { it.id == chatId }?.composerAvailability(profile)
    }

    fun openOrCreateDirectChat(
        personId: String,
        requestedChatId: String? = null,
    ): String? {
        val profile = uiState.activeProfile ?: return null
        val person = profile.people.firstOrNull { it.id == personId } ?: return null
        val existing = profile.chats.firstOrNull { chat ->
            (chat.kind as? ChatKind.Direct)?.personId == personId
        }
        if (existing != null) {
            openChat(existing.id)
            return existing.id
        }
        if (profile.chatRelayUrls.isEmpty()) return null

        val id = requestedChatId ?: nextChatId("direct-$personId")
        if (profile.chats.any { it.id == id }) return null
        val chat = Chat(
            id = id,
            originalOrder = 0,
            kind = ChatKind.Direct(personId),
            title = person.displayName,
            avatar = person.avatar,
            preview = "You started the chat.",
            relayUrls = profile.chatRelayUrls,
            defaultRelayUrls = profile.chatRelayUrls,
            timeline = listOf(
                ChatTimelineEntry.Event("$id-event-direct-started", "You started the chat."),
            ),
        )
        updateActiveProfile { current -> current.copy(chats = insertAfterPinned(current.chats, chat)) }
        return id
    }

    fun createGroup(
        name: String,
        description: String,
        avatar: ProfileAvatar,
        selectedPersonIds: List<String>,
        requestedChatId: String? = null,
    ): String? {
        val profile = uiState.activeProfile ?: return null
        val trimmedName = name.trim()
        val uniqueIds = selectedPersonIds.fold(mutableListOf<String>()) { result, personId ->
            if (
                personId != profile.id &&
                personId != "white-noise-support" &&
                profile.people.any { it.id == personId } &&
                personId !in result
            ) {
                result += personId
            }
            result
        }
        if (trimmedName.isEmpty() || uniqueIds.isEmpty() || profile.chatRelayUrls.isEmpty()) return null

        val id = requestedChatId ?: nextChatId("group")
        if (profile.chats.any { it.id == id }) return null
        val chat = Chat(
            id = id,
            originalOrder = 0,
            kind = ChatKind.Group,
            title = trimmedName,
            description = description.trim(),
            avatar = avatar,
            preview = "You created the group.",
            members = listOf(GroupMember(profile.id, GroupRole.Admin)) +
                uniqueIds.map { GroupMember(it, GroupRole.Member) },
            relayUrls = profile.chatRelayUrls,
            defaultRelayUrls = profile.chatRelayUrls,
            timeline = listOf(
                ChatTimelineEntry.Event("$id-event-group-created", "You created the group."),
            ),
        )
        updateActiveProfile { current -> current.copy(chats = insertAfterPinned(current.chats, chat)) }
        return id
    }

    fun chat(chatId: String): Chat? = uiState.activeProfile?.chats?.firstOrNull { it.id == chatId }

    fun editGroup(
        chatId: String,
        name: String,
        description: String,
        avatar: ProfileAvatar,
    ): Boolean {
        val profileId = uiState.activeProfileId ?: return false
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false
        var changed = false
        mutateChat(chatId) { chat ->
            val isAdmin = chat.members.firstOrNull { it.personId == profileId }?.role == GroupRole.Admin
            if (!chat.isGroup || !isAdmin || chat.membership != ChatMembership.Active) return@mutateChat chat
            if (chat.title == trimmedName && chat.description == description.trim() && chat.avatar == avatar) return@mutateChat chat
            changed = true
            val (day, minute) = nextTimelinePosition(chat)
            chat.copy(
                title = trimmedName,
                description = description.trim(),
                avatar = avatar,
                timeline = chat.timeline + ChatTimelineEntry.Event(
                    id = "$chatId-edited-${createdChatSequence++}",
                    text = "You updated the group info.",
                    dayOrdinal = day,
                    dayLabel = "Today",
                    minuteOfDay = minute,
                ),
            )
        }
        return changed
    }

    fun addGroupMembers(chatId: String, personIds: List<String>): Boolean {
        val profile = uiState.activeProfile ?: return false
        var changed = false
        mutateChat(chatId) { chat ->
            val isAdmin = chat.members.firstOrNull { it.personId == profile.id }?.role == GroupRole.Admin
            if (!chat.isGroup || !isAdmin || chat.membership != ChatMembership.Active) return@mutateChat chat
            val existing = chat.members.map { it.personId }.toSet()
            val additions = personIds.distinct().mapNotNull { id ->
                profile.people.firstOrNull { it.id == id && id !in existing && id != profile.id }
            }
            if (additions.isEmpty()) return@mutateChat chat
            changed = true
            val (day, minute) = nextTimelinePosition(chat)
            chat.copy(
                members = chat.members + additions.map { GroupMember(it.id, GroupRole.Member) },
                timeline = chat.timeline + ChatTimelineEntry.Event(
                    id = "$chatId-added-${createdChatSequence++}",
                    text = "You added ${additions.joinToString { it.name }}.",
                    dayOrdinal = day,
                    dayLabel = "Today",
                    minuteOfDay = minute,
                ),
            )
        }
        return changed
    }

    fun setGroupMemberAdmin(chatId: String, personId: String, isAdmin: Boolean): Boolean {
        val profileId = uiState.activeProfileId ?: return false
        if (personId == profileId) return false
        var changed = false
        mutateChat(chatId) { chat ->
            val actorIsAdmin = chat.members.firstOrNull { it.personId == profileId }?.role == GroupRole.Admin
            val member = chat.members.firstOrNull { it.personId == personId }
            val desired = if (isAdmin) GroupRole.Admin else GroupRole.Member
            if (!actorIsAdmin || member == null || member.role == desired || chat.membership != ChatMembership.Active) return@mutateChat chat
            if (!isAdmin && chat.members.count { it.role == GroupRole.Admin } <= 1) return@mutateChat chat
            changed = true
            val name = uiState.activeProfile?.people?.firstOrNull { it.id == personId }?.displayName ?: "Member"
            val (day, minute) = nextTimelinePosition(chat)
            chat.copy(
                members = chat.members.map { if (it.personId == personId) it.copy(role = desired) else it },
                timeline = chat.timeline + ChatTimelineEntry.Event(
                    id = "$chatId-role-${createdChatSequence++}",
                    text = if (isAdmin) "You made $name an admin." else "You removed $name as an admin.",
                    dayOrdinal = day,
                    dayLabel = "Today",
                    minuteOfDay = minute,
                ),
            )
        }
        return changed
    }

    fun removeGroupMember(chatId: String, personId: String): Boolean {
        val profileId = uiState.activeProfileId ?: return false
        if (personId == profileId) return false
        var changed = false
        mutateChat(chatId) { chat ->
            val actorIsAdmin = chat.members.firstOrNull { it.personId == profileId }?.role == GroupRole.Admin
            val member = chat.members.firstOrNull { it.personId == personId }
            if (!actorIsAdmin || member == null || chat.membership != ChatMembership.Active) return@mutateChat chat
            if (member.role == GroupRole.Admin && chat.members.count { it.role == GroupRole.Admin } <= 1) return@mutateChat chat
            changed = true
            val name = uiState.activeProfile?.people?.firstOrNull { it.id == personId }?.displayName ?: "Member"
            val (day, minute) = nextTimelinePosition(chat)
            chat.copy(
                members = chat.members.filterNot { it.personId == personId },
                timeline = chat.timeline + ChatTimelineEntry.Event(
                    id = "$chatId-removed-${createdChatSequence++}",
                    text = "You removed $name from the group.",
                    dayOrdinal = day,
                    dayLabel = "Today",
                    minuteOfDay = minute,
                ),
            )
        }
        return changed
    }

    fun addChatRelay(chatId: String, value: String): Boolean {
        var changed = false
        mutateChat(chatId) { chat ->
            val relays = ChatRelayPolicy.add(chat.relayUrls, value) ?: return@mutateChat chat
            changed = true
            chat.copy(relayUrls = relays)
        }
        return changed
    }

    fun removeChatRelay(chatId: String, relayUrl: String): Boolean {
        var changed = false
        mutateChat(chatId) { chat ->
            val normalized = ChatRelayPolicy.normalize(relayUrl) ?: return@mutateChat chat
            val relays = chat.relayUrls.filterNot { ChatRelayPolicy.normalize(it) == normalized }
            if (relays.size == chat.relayUrls.size) return@mutateChat chat
            changed = true
            chat.copy(relayUrls = relays)
        }
        return changed
    }

    fun restoreChatRelays(chatId: String): Boolean {
        var changed = false
        mutateChat(chatId) { chat ->
            if (chat.relayUrls == chat.defaultRelayUrls) return@mutateChat chat
            changed = true
            chat.copy(relayUrls = chat.defaultRelayUrls)
        }
        return changed
    }

    fun person(personId: String): Person? =
        uiState.activeProfile?.people?.firstOrNull { it.id == personId }

    private fun activate(profile: Profile, updatesStoredProfile: Boolean) {
        interruptMessageEdits()
        cancelCreatedChatOpen()
        cancelProfileSave()
        dismissChatBatch()
        val profiles = uiState.profiles.toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            if (updatesStoredProfile) {
                profiles[index] = profiles[index].updateEditableValues(from = profile)
            }
        } else {
            profiles += profile
        }

        val activatedIndex = profiles.indexOfFirst { it.id == profile.id }
        profiles[activatedIndex] = profiles[activatedIndex].copy(connectionInformationPublished = true,
            chatConnection = profiles[activatedIndex].chatConnection.let { connection ->
                if (connection.phase == ChatConnectionPhase.Connecting || connection.phase == ChatConnectionPhase.CatchingUp)
                    connection.copy(generation = connection.generation + 1) else connection
            })

        uiState = uiState.copy(
            profiles = profiles,
            activeProfileId = profile.id,
            signedInProfileIds = uiState.signedInProfileIds + profile.id,
            pendingDiagnosticsProfileId = profile.id.takeUnless {
                profiles.first { candidate -> candidate.id == profile.id }.diagnostics.hasSeenPrompt
            },
        )
    }

    private fun addShowcaseProfiles() {
        val profiles = uiState.profiles.toMutableList()
        ProfileFixtures.showcaseProfiles.forEach { profile ->
            if (profiles.none { it.id == profile.id }) profiles += profile
        }
        uiState = uiState.copy(
            profiles = profiles,
            signedInProfileIds = uiState.signedInProfileIds +
                ProfileFixtures.showcaseProfiles.map(Profile::id),
        )
    }

    private fun mutateChat(chatId: String, transform: (Chat) -> Chat) {
        updateActiveProfile { profile ->
            profile.copy(chats = profile.chats.map { before ->
                if (before.id != chatId) before else {
                    val after = transform(before)
                    if (after.readState != null && after.timeline != before.timeline) {
                        val read = dev.ipf.whitenoise.model.ConversationReading.reconcile(after.readState, after, profile.id)
                        after.copy(readState = read, unreadCount = read.unreadIds.size)
                    } else after
                }
            })
        }
    }

    private fun mutatePerson(personId: String, transform: (Person) -> Person) {
        updateActiveProfile { profile ->
            profile.copy(people = profile.people.map { if (it.id == personId) transform(it) else it })
        }
    }

    private fun updateActiveProfile(transform: (Profile) -> Profile) {
        val activeId = uiState.activeProfileId ?: return
        uiState = uiState.copy(
            profiles = uiState.profiles.map { profile ->
                if (profile.id == activeId) transform(profile) else profile
            },
        )
    }

    private fun insertAfterPinned(chats: List<Chat>, chat: Chat): List<Chat> {
        val mutable = chats.toMutableList()
        val insertion = mutable.indexOfFirst { !it.isPinned }.let { if (it == -1) mutable.size else it }
        mutable.add(insertion, chat)
        return mutable.mapIndexed { index, item -> item.copy(originalOrder = index) }
    }

    private fun nextChatId(prefix: String): String {
        createdChatSequence += 1
        return "$prefix-$createdChatSequence"
    }

    private fun nextTimelinePosition(chat: Chat): Pair<Int, Int> {
        val day = maxOf(3, chat.timeline.maxOfOrNull(ChatTimelineEntry::dayOrdinal) ?: 3)
        val latestMinute = chat.timeline.filter { it.dayOrdinal == day }.maxOfOrNull(ChatTimelineEntry::minuteOfDay)
        return day to ((latestMinute ?: 719) + 1)
    }

    private fun attachmentPreview(attachments: List<MessageAttachment>): AttachmentPreview? {
        if (attachments.isEmpty()) return null
        val visualCount = attachments.count {
            it.kind == MessageAttachmentKind.Photo || it.kind == MessageAttachmentKind.Photos
        }
        if (visualCount > 1 && visualCount == attachments.size) {
            return AttachmentPreview.Photos(visualCount)
        }
        val first = attachments.first()
        return when (first.kind) {
            MessageAttachmentKind.Photo -> AttachmentPreview.Photo
            MessageAttachmentKind.Photos -> AttachmentPreview.Photos(first.images.size.coerceAtLeast(1))
            MessageAttachmentKind.Video -> AttachmentPreview.Video
            MessageAttachmentKind.Voice -> AttachmentPreview.VoiceMessage
            MessageAttachmentKind.File -> AttachmentPreview.File(first.label)
            MessageAttachmentKind.Contact -> AttachmentPreview.Contact(first.label.removePrefix("Contact: "))
            MessageAttachmentKind.Link -> AttachmentPreview.Link
            MessageAttachmentKind.Gif -> AttachmentPreview.Gif
        }
    }
}
