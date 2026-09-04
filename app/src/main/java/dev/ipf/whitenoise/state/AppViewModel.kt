package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.LocationSession
import dev.ipf.whitenoise.model.LocationScenario
import dev.ipf.whitenoise.model.LocationEvent
import dev.ipf.whitenoise.model.LocationPhase
import dev.ipf.whitenoise.model.LocationFailure
import dev.ipf.whitenoise.model.PhotoEditorSession
import dev.ipf.whitenoise.model.PhotoEditorScenario
import dev.ipf.whitenoise.model.PhotoEditorEvent
import dev.ipf.whitenoise.model.PhotoEditorPhase
import dev.ipf.whitenoise.model.PhotoEditorFailure
import dev.ipf.whitenoise.model.PhotoEditHistory
import dev.ipf.whitenoise.model.PhotoEditRecipe
import dev.ipf.whitenoise.model.PhotoEditing
import dev.ipf.whitenoise.model.PhotoCropPreset
import dev.ipf.whitenoise.model.PhotoEditorTool
import dev.ipf.whitenoise.model.PhotoStroke
import dev.ipf.whitenoise.model.PhotoEditLimit

import dev.ipf.whitenoise.model.*

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
        if (uiState.profiles.none { it.id == profileId } || uiState.activeProfileId == profileId) return
        interruptMessageEdits()
        interruptMessageOperations()
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
        interruptMessageOperations()
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
        interruptMessageOperations()
        cancelCreatedChatOpen()
        cancelProfileSave()
        dismissChatBatch()
        cancelAccess()
        val signedIn = uiState.signedInProfileIds - activeId
        if (wipeData) messageForwards = messageForwards.filterValues { activeId !in setOf(it.sourceProfileId, it.destinationProfileId) }
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
        interruptMessageOperations(profileId)
        messageForwards = messageForwards.filterValues { profileId !in setOf(it.sourceProfileId, it.destinationProfileId) }
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
        nextMessageDeleteScenario = MessageDeleteScenario.Success
        nextMessageForwardScenario = MessageForwardScenario.Success
        photoEditorSession = null
        locationSession = null
        locationScenarios = emptyMap()
        nextPhotoEditorScenario = PhotoEditorScenario.Success
        nextAttachmentAccessScenario = dev.ipf.whitenoise.model.AttachmentAccessScenario.Success
        attachmentAccessScenarioOwner = null
        recentMediaAccess = dev.ipf.whitenoise.model.RecentMediaAccess.Full
        attachmentTransferScenario = dev.ipf.whitenoise.model.AttachmentTransferScenario.Success
        messageForwards = emptyMap()
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

    private var attachmentAccessScenarioOwner: String? = null
    var nextAttachmentAccessScenario by mutableStateOf(dev.ipf.whitenoise.model.AttachmentAccessScenario.Success)
        private set
    fun selectAttachmentAccessScenario(value: dev.ipf.whitenoise.model.AttachmentAccessScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) { attachmentAccessScenarioOwner = uiState.activeProfileId; nextAttachmentAccessScenario = value }
    }
    fun consumeAttachmentAccessScenario(profileId: String): dev.ipf.whitenoise.model.AttachmentAccessScenario {
        if (profileId != uiState.activeProfileId) return dev.ipf.whitenoise.model.AttachmentAccessScenario.LoadFailure
        if (attachmentAccessScenarioOwner != profileId) return dev.ipf.whitenoise.model.AttachmentAccessScenario.Success
        return nextAttachmentAccessScenario.also { nextAttachmentAccessScenario = dev.ipf.whitenoise.model.AttachmentAccessScenario.Success; attachmentAccessScenarioOwner = null }
    }
    fun addAttachmentReadingExamples(profileId: String, chatId: String): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId && it.developerTools.isEnabled } ?: return false
        val chat = chat(chatId)?.takeIf { it.composerAvailability(profile) == ComposerAvailability.Available } ?: return false
        val (day, minute) = nextTimelinePosition(chat)
        val sequence = createdChatSequence++
        val messages = dev.ipf.whitenoise.model.AttachmentReadingExamples.attachments().mapIndexed { index, attachment ->
            ChatTimelineEntry.Message(ChatMessage("$chatId-document-$sequence-$index", profileId, day, "Today", minute, "Now",
                attachments = listOf(attachment.copy(id = "${attachment.id}-$sequence"))))
        }
        mutateChat(chatId) { it.copy(timeline = it.timeline + messages, preview = "Shared files", previewAuthor = "You", timestamp = "Now") }
        return true
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

    private var photoEditorGeneration = 0L
    var photoEditorSession by mutableStateOf<PhotoEditorSession?>(null)
        private set
    var nextPhotoEditorScenario by mutableStateOf(PhotoEditorScenario.Success)
        private set

    fun selectPhotoEditorScenario(value: PhotoEditorScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextPhotoEditorScenario = value
    }

    fun openPhotoEditor(profileId: String, chatId: String, attachmentId: String, imageIndex: Int): Boolean {
        if (uiState.activeProfileId != profileId || profileId !in uiState.signedInProfileIds) return false
        val chat = chat(chatId) ?: return false
        if (composerAvailability(chatId) != ComposerAvailability.Available) return false
        val attachment = chat.draftAttachments.firstOrNull { it.id == attachmentId } ?: return false
        if (attachment.kind !in setOf(dev.ipf.whitenoise.model.MessageAttachmentKind.Photo, dev.ipf.whitenoise.model.MessageAttachmentKind.Photos) ||
            attachment.images.getOrNull(imageIndex) == null || photoEditorSession?.phase == PhotoEditorPhase.Saving) return false
        photoEditorSession = PhotoEditorSession(++photoEditorGeneration, profileId, chatId, attachmentId, imageIndex,
            attachment, PhotoEditHistory(initial = attachment.photoEdits[imageIndex] ?: PhotoEditRecipe()),
            attachment.photoFrameQualities[imageIndex] ?: attachment.photoQuality ?: chat.draftPhotoQuality,
            nextStrokeId = (attachment.photoEdits[imageIndex]?.strokes?.maxOfOrNull { it.id } ?: 0) + 1, scenario = nextPhotoEditorScenario)
        nextPhotoEditorScenario = PhotoEditorScenario.Success
        return true
    }

    fun photoEditorAction(sessionId: Long, event: PhotoEditorEvent): Boolean {
        val session = photoEditorSession?.takeIf { it.id == sessionId } ?: return false
        if (session.profileId != uiState.activeProfileId || session.profileId !in uiState.signedInProfileIds) return false
        if (event == PhotoEditorEvent.Close) {
            if (session.phase == PhotoEditorPhase.Saving) return false
            photoEditorSession = null
            return true
        }
        val attachment = chat(session.chatId)?.draftAttachments?.firstOrNull { it.id == session.attachmentId }
        if (attachment != session.expectedAttachment || composerAvailability(session.chatId) != ComposerAvailability.Available) {
            photoEditorSession = session.copy(phase = PhotoEditorPhase.Failed, revision = session.revision + 1, failure = PhotoEditorFailure.SourceChanged)
            return false
        }
        fun publish(next: PhotoEditorSession): Boolean {
            photoEditorSession = next.copy(revision = session.revision + 1)
            return true
        }
        if (event is PhotoEditorEvent.Loaded) {
            if (session.phase != PhotoEditorPhase.Loading || event.revision != session.revision) return false
            val failure = event.failure ?: if (!PhotoEditing.validSource(event.width, event.height)) PhotoEditorFailure.InvalidSource else null
            return publish(session.copy(sourceWidth = event.width, sourceHeight = event.height,
                phase = if (failure == null) PhotoEditorPhase.Ready else PhotoEditorPhase.Failed, failure = failure,
                scenario = if (session.scenario == PhotoEditorScenario.LoadFailure) PhotoEditorScenario.Success else session.scenario))
        }
        if (event is PhotoEditorEvent.Saved) {
            if (session.phase != PhotoEditorPhase.Saving || event.revision != session.revision) return false
            val frame = event.attachment
            val image = frame?.images?.singleOrNull() as? ProfileAvatar.DeviceImage
            if (event.failure != null || frame == null || image == null || image.bytes.isEmpty() || image.bytes.size > 32 * 1024 * 1024 ||
                (frame.pixelWidth ?: 0) <= 0 || (frame.pixelHeight ?: 0) <= 0) {
                return publish(session.copy(phase = PhotoEditorPhase.Failed, failure = event.failure ?: PhotoEditorFailure.SaveFailed, scenario = PhotoEditorScenario.Success))
            }
            // Only the selected frame may be committed; renderer output cannot replace draft identity or siblings.
            val images = attachment.images.mapIndexed { index, before -> if (index == session.imageIndex) image else before }
            val output = attachment.copy(images = images, sourceImages = attachment.sourceImages.ifEmpty { attachment.images },
                photoEdits = attachment.photoEdits + (session.imageIndex to session.history.current),
                photoFrameQualities = attachment.photoFrameQualities + (session.imageIndex to session.requestedQuality),
                metadataPolicy = frame.metadataPolicy, mimeType = frame.mimeType.takeIf { images.size == 1 },
                fileSizeBytes = if (images.all { it is ProfileAvatar.DeviceImage }) images.sumOf { (it as ProfileAvatar.DeviceImage).bytes.size } else null,
                pixelWidth = if (session.imageIndex == 0) frame.pixelWidth else attachment.pixelWidth,
                pixelHeight = if (session.imageIndex == 0) frame.pixelHeight else attachment.pixelHeight)
            mutateChat(session.chatId) { current -> current.copy(draftAttachments = current.draftAttachments.map { if (it.id == session.attachmentId) output else it }) }
            photoEditorSession = null
            return true
        }
        if (event == PhotoEditorEvent.Retry) {
            if (session.phase != PhotoEditorPhase.Failed || session.failure == PhotoEditorFailure.SourceChanged) return false
            return publish(session.copy(phase = if (session.sourceWidth > 0 && session.failure in setOf(PhotoEditorFailure.SaveFailed, PhotoEditorFailure.MemoryLimit)) PhotoEditorPhase.Saving else PhotoEditorPhase.Loading,
                failure = null, scenario = PhotoEditorScenario.Success))
        }
        if (!session.editable) return false
        val ready = session.copy(phase = PhotoEditorPhase.Ready, failure = null, limit = null)
        val next = when (event) {
            is PhotoEditorEvent.SelectTool -> ready.copy(tool = event.tool)
            is PhotoEditorEvent.SelectColor -> ready.copy(color = event.color)
            is PhotoEditorEvent.SelectWidth -> ready.copy(width = event.width)
            is PhotoEditorEvent.SelectQuality -> ready.copy(requestedQuality = event.quality)
            is PhotoEditorEvent.SelectPreset -> ready.copy(tool = PhotoEditorTool.Crop, preset = event.preset,
                history = session.history.commit(session.history.current.copy(crop = PhotoEditing.preset(event.preset, session.sourceWidth, session.sourceHeight, session.history.current.quarterTurns, session.history.current.crop))))
            is PhotoEditorEvent.Crop -> ready.copy(tool = PhotoEditorTool.Crop, preset = PhotoCropPreset.Free, history = session.history.commit(session.history.current.copy(crop = event.crop)))
            is PhotoEditorEvent.Stroke -> {
                if (session.tool == PhotoEditorTool.Crop || event.points.isEmpty()) return false
                val (history, limit) = session.history.add(PhotoStroke(session.nextStrokeId, event.points, session.width, session.color, session.tool == PhotoEditorTool.Erase))
                ready.copy(history = history, nextStrokeId = session.nextStrokeId + 1, limit = limit ?: PhotoEditLimit.StrokePoints.takeIf { event.limited })
            }
            PhotoEditorEvent.Rotate -> ready.copy(tool = PhotoEditorTool.Crop, preset = PhotoCropPreset.Free, history = session.history.commit(session.history.current.copy(quarterTurns = (session.history.current.quarterTurns + 1) % 4)))
            PhotoEditorEvent.Undo -> ready.copy(history = session.history.undo(), preset = PhotoCropPreset.Free)
            PhotoEditorEvent.Redo -> ready.copy(history = session.history.redo(), preset = PhotoCropPreset.Free)
            PhotoEditorEvent.Reset -> ready.copy(history = session.history.reset(), requestedQuality = session.initialQuality, preset = if (session.history.initial.crop == dev.ipf.whitenoise.model.PhotoCrop()) PhotoCropPreset.Original else PhotoCropPreset.Free)
            PhotoEditorEvent.Save -> ready.copy(phase = PhotoEditorPhase.Saving)
        }
        return publish(next)
    }

    var recentMediaAccess by mutableStateOf(dev.ipf.whitenoise.model.RecentMediaAccess.Full)
        private set
    var attachmentTransferScenario by mutableStateOf(dev.ipf.whitenoise.model.AttachmentTransferScenario.Success)
        private set

    fun selectRecentMediaAccess(value: dev.ipf.whitenoise.model.RecentMediaAccess) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) recentMediaAccess = value
    }

    fun selectAttachmentTransferScenario(value: dev.ipf.whitenoise.model.AttachmentTransferScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled != true) return
        attachmentTransferScenario = value
        updateActiveProfile { profile -> profile.copy(chats = profile.chats.map { chat ->
            chat.copy(timeline = chat.timeline.map { entry ->
                if (entry !is ChatTimelineEntry.Message || entry.message.isDeleted) entry else entry.copy(message = entry.message.copy(
                    attachments = entry.message.attachments.map { attachment ->
                        if (attachment.kind == dev.ipf.whitenoise.model.MessageAttachmentKind.Link || attachment.kind == dev.ipf.whitenoise.model.MessageAttachmentKind.Contact) attachment
                        else attachment.copy(transfer = dev.ipf.whitenoise.model.AttachmentTransfer(
                            phase = dev.ipf.whitenoise.model.AttachmentTransferPhase.CacheMiss,
                            scenario = value, attempt = 0,
                            revision = (attachment.transfer?.revision ?: 0) + 1,
                        ))
                    },
                ))
            })
        }) }
    }

    fun replaceDraftPhotos(profileId: String, chatId: String, expected: List<MessageAttachment>, quality: dev.ipf.whitenoise.model.PhotoQuality, photos: List<MessageAttachment>): Boolean {
        if (uiState.activeProfileId != profileId || profileId !in uiState.signedInProfileIds) return false
        var changed = false
        mutateChat(chatId) { chat ->
            if (chat.draftAttachments != expected || photos.map { it.id }.toSet().size != photos.size ||
                photos.any { replacement -> expected.none { it.id == replacement.id } }) chat
            else {
                changed = true
                chat.copy(draftPhotoQuality = quality, draftAttachments = chat.draftAttachments.map { before -> photos.firstOrNull { it.id == before.id } ?: before })
            }
        }
        return changed
    }

    fun attachmentTransferAction(profileId: String, chatId: String, messageId: String, attachmentId: String, action: String, expectedRevision: Long) {
        if (uiState.activeProfileId != profileId || profileId !in uiState.signedInProfileIds) return
        mutateChat(chatId) { chat -> chat.copy(timeline = chat.timeline.map { entry ->
            if (entry !is ChatTimelineEntry.Message || entry.message.id != messageId || entry.message.isDeleted) entry
            else entry.copy(message = entry.message.copy(attachments = entry.message.attachments.map { attachment ->
                if (attachment.id != attachmentId || (attachment.transfer?.revision ?: 0) != expectedRevision) attachment
                else {
                    val current = attachment.transfer
                    val next = when (action) {
                        "advance" -> current?.advance(expectedRevision)
                        "cancel" -> current?.cancel()
                        "retry" -> current?.retry()
                        "start" -> if (current == null) dev.ipf.whitenoise.model.AttachmentTransfer(scenario = attachmentTransferScenario,
                            direction = if (entry.message.authorId == profileId) dev.ipf.whitenoise.model.AttachmentTransferDirection.Upload else dev.ipf.whitenoise.model.AttachmentTransferDirection.Download) else current
                        else -> current
                    }
                    attachment.copy(transfer = if (entry.message.expiresAtMillis?.let { it <= dev.ipf.whitenoise.model.MessageForwarding.nowMillis } == true && next != null)
                        next.copy(phase = dev.ipf.whitenoise.model.AttachmentTransferPhase.Expired)
                        else if (!attachment.isAvailable && next?.phase == dev.ipf.whitenoise.model.AttachmentTransferPhase.Available)
                            next.copy(phase = dev.ipf.whitenoise.model.AttachmentTransferPhase.Unavailable)
                        else next)
                }
            }))
        }) }
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

    private var locationGeneration = 0L
    private var locationScenarios by mutableStateOf(emptyMap<String, LocationScenario>())
    val nextLocationScenario: LocationScenario get() = locationScenarios[uiState.activeProfileId] ?: LocationScenario.Unavailable
    var locationSession by mutableStateOf<LocationSession?>(null)
        private set

    fun selectLocationScenario(value: LocationScenario) {
        val profile = uiState.activeProfile?.takeIf { it.developerTools.isEnabled } ?: return
        locationScenarios = locationScenarios + (profile.id to value)
    }

    fun openLocation(profileId: String, chatId: String): Boolean {
        if (uiState.activeProfileId != profileId || profileId !in uiState.signedInProfileIds ||
            composerAvailability(chatId) != ComposerAvailability.Available) return false
        val chat = chat(chatId) ?: return false
        val reply = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.message.id == chat.draftReplyMessageId }?.message
        if (chat.draftReplyMessageId != null && (reply == null || reply.isDeleted || reply.expiresAtMillis?.let { it <= MessageForwarding.nowMillis } == true)) return false
        val scenario = if (uiState.activeProfile?.developerTools?.isEnabled == true) nextLocationScenario else LocationScenario.Unavailable
        locationSession = LocationSession(++locationGeneration, profileId, chatId, reply, scenario)
        locationScenarios = locationScenarios - profileId
        return true
    }

    fun locationAction(sessionId: Long, event: LocationEvent): Boolean {
        val session = locationSession?.takeIf { it.id == sessionId } ?: return false
        if (session.profileId != uiState.activeProfileId || session.profileId !in uiState.signedInProfileIds) return false
        if (event == LocationEvent.Close || event == LocationEvent.Back) {
            val next = session.reduce(event)
            locationSession = next.takeUnless { it.phase == LocationPhase.Closed }
            return true
        }
        val chat = chat(session.chatId)
        val reply = chat?.timeline?.filterIsInstance<ChatTimelineEntry.Message>()?.firstOrNull { it.message.id == session.expectedReply?.id }?.message
        val replyValid = session.expectedReply == null || (reply != null && !reply.isDeleted &&
            reply.expiresAtMillis?.let { it <= MessageForwarding.nowMillis } != true && reply.text == session.expectedReply.text && reply.attachments == session.expectedReply.attachments)
        if (chat == null || composerAvailability(session.chatId) != ComposerAvailability.Available ||
            chat.draftReplyMessageId != session.expectedReply?.id || !replyValid) {
            locationSession = session.copy(phase = LocationPhase.Review, failure = LocationFailure.SourceChanged, revision = session.revision + 1)
            return false
        }
        val next = session.reduce(event)
        if (next == session) return false
        if (next.phase == LocationPhase.Closed && event is LocationEvent.Sent) {
            val point = session.point ?: return false
            if (!sendContent(session.chatId, point.messageText, emptyList(), session.expectedReply?.id, preserveDraft = true)) {
                locationSession = session.copy(phase = LocationPhase.Review, failure = LocationFailure.SourceChanged, revision = session.revision + 1)
                return false
            }
            locationSession = null
        } else locationSession = next
        return true
    }

    fun sendDraft(chatId: String): Boolean {
        val chat = chat(chatId) ?: return false
        val trimmed = chat.draftText.trim()
        val linkPreview = LinkPreviewDetector.first(trimmed)
            ?.takeUnless { it.url == chat.suppressedDraftLinkUrl || (chat.draftAttachments.isEmpty() && dev.ipf.whitenoise.model.LocationSharing.parse(trimmed) != null) }
            ?.attachment("$chatId-link-${createdChatSequence + 1}")
        val attachments = chat.draftAttachments.map { it.copy(sourceImages = emptyList(), photoEdits = emptyMap(), photoFrameQualities = emptyMap(), transfer = if (it.kind in setOf(dev.ipf.whitenoise.model.MessageAttachmentKind.Photo, dev.ipf.whitenoise.model.MessageAttachmentKind.Photos,
            dev.ipf.whitenoise.model.MessageAttachmentKind.Video, dev.ipf.whitenoise.model.MessageAttachmentKind.File, dev.ipf.whitenoise.model.MessageAttachmentKind.Gif))
            dev.ipf.whitenoise.model.AttachmentTransfer(direction = dev.ipf.whitenoise.model.AttachmentTransferDirection.Upload, scenario = attachmentTransferScenario) else null) } + listOfNotNull(linkPreview)
        if (trimmed.isEmpty() && attachments.isEmpty()) return false
        val contactText = attachments.mapNotNull { it.deviceContact?.text }.joinToString("\n\n")
        return sendContent(chatId, listOf(trimmed, contactText).filter(String::isNotBlank).joinToString("\n\n"), attachments, chat.draftReplyMessageId)
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

    private var messageOperationGeneration = 0L
    var nextMessageDeleteScenario by mutableStateOf(MessageDeleteScenario.Success)
        private set
    var nextMessageForwardScenario by mutableStateOf(MessageForwardScenario.Success)
        private set
    var messageForwards by mutableStateOf<Map<String, MessageForwardOperation>>(emptyMap())
        private set

    fun selectMessageDeleteScenario(value: MessageDeleteScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextMessageDeleteScenario = value
    }
    fun selectMessageForwardScenario(value: MessageForwardScenario) {
        if (uiState.activeProfile?.developerTools?.isEnabled == true) nextMessageForwardScenario = value
    }
    fun beginMessageDeletion(profileId: String, chatId: String, ids: Set<String>, scope: MessageDeletionScope): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return false
        val chat = chat(chatId)?.takeUnless { it.messageDeletion?.isRunning == true } ?: return false
        val items = MessageDeletion.plan(profile, chat, ids, scope) ?: return false
        val operation = MessageDeleteOperation(++messageOperationGeneration, profileId, chatId, items, nextMessageDeleteScenario)
        nextMessageDeleteScenario = MessageDeleteScenario.Success
        mutateChat(chatId) { it.copy(messageDeletion = operation) }
        return true
    }
    fun advanceMessageDeletion(profileId: String, chatId: String, requestId: Long, revision: Int): Boolean {
        val profile = uiState.activeProfile?.takeIf { it.id == profileId } ?: return false
        val chat = chat(chatId) ?: return false
        val operation = chat.messageDeletion?.takeIf { it.id == requestId && it.revision == revision && it.isRunning } ?: return false
        val index = operation.items.indexOfFirst { it.phase == MessageDeletePhase.Pending }
        val item = operation.items[index]
        val message = message(chatId, item.messageId)
        val failure = when {
            message == null -> MessageDeleteFailure.Unavailable
            item.scope == MessageDeletionScope.ForEveryone && !MessageDeletion.canDeleteForEveryone(message, profile, chat) -> MessageDeleteFailure.PermissionDenied
            operation.attempt == 0 && (operation.scenario == MessageDeleteScenario.Failure ||
                (operation.scenario == MessageDeleteScenario.Partial && index % 2 == 1)) -> MessageDeleteFailure.Temporary
            else -> null
        }
        val outcome = item.copy(phase = if (failure == null) MessageDeletePhase.Succeeded else MessageDeletePhase.Failed, failure = failure)
        val next = operation.copy(items = operation.items.mapIndexed { i, value -> if (i == index) outcome else value }, revision = revision + 1)
        mutateChat(chatId) { current ->
            (if (failure == null) MessageDeletion.remove(current, item, profileId) else current).copy(messageDeletion = next)
        }
        return true
    }
    fun retryMessageDeletion(profileId: String, chatId: String, requestId: Long): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val op = chat(chatId)?.messageDeletion?.takeIf { it.id == requestId && it.canRetry } ?: return false
        mutateChat(chatId) { it.copy(messageDeletion = op.copy(attempt = op.attempt + 1, revision = op.revision + 1,
            items = op.items.map { item -> if (item.phase == MessageDeletePhase.Failed && item.failure != MessageDeleteFailure.SessionChanged)
                item.copy(phase = MessageDeletePhase.Pending, failure = null) else item })) }
        return true
    }
    fun dismissMessageDeletion(profileId: String, chatId: String, requestId: Long) {
        if (uiState.activeProfileId != profileId) return
        mutateChat(chatId) { chat -> if (chat.messageDeletion?.let { it.id == requestId && !it.isRunning } == true) chat.copy(messageDeletion = null) else chat }
    }
    /** Immediate local entry used by existing non-UI callers; the UI uses the staged batch. */
    fun deleteMessages(chatId: String, messageIds: Set<String>, scope: MessageDeletionScope): Boolean {
        val profile = uiState.activeProfile ?: return false
        val chat = chat(chatId) ?: return false
        val items = MessageDeletion.plan(profile, chat, messageIds, scope) ?: return false
        mutateChat(chatId) { original -> items.fold(original) { current, item -> MessageDeletion.remove(current, item, profile.id) } }
        return true
    }

    fun beginMessageForward(profileId: String, sourceChatId: String, messageIds: Set<String>, destinationProfileId: String,
        targetChatIds: List<String>, mediaKey: ConversationMediaKey? = null, accompanyingText: String = ""): Boolean {
        val sourceProfile = uiState.activeProfile?.takeIf { it.id == profileId && it.id in uiState.signedInProfileIds } ?: return false
        val destination = uiState.signedInProfiles.firstOrNull { it.id == destinationProfileId } ?: return false
        if (messageForwards[profileId]?.isRunning == true) return false
        val source = sourceProfile.chats.firstOrNull { it.id == sourceChatId } ?: return false
        val targets = targetChatIds.distinct()
        if (targets.isEmpty() || (destinationProfileId == profileId && sourceChatId in targets)) return false
        val payload = MessageForwarding.payload(sourceProfile, source, messageIds, mediaKey, accompanyingText) ?: return false
        val operation = MessageForwardOperation(++messageOperationGeneration, profileId, sourceChatId, destinationProfileId,
            messageIds.toSet(), payload, targets.map { id ->
                val chat = destination.chats.firstOrNull { it.id == id }
                val failure = MessageForwarding.targetFailure(destination, chat)
                MessageForwardTarget(id, chat?.title.orEmpty(), phase = if (failure == null) MessageForwardTargetPhase.Waiting else MessageForwardTargetPhase.Failed, failure = failure)
            }, nextMessageForwardScenario)
        nextMessageForwardScenario = MessageForwardScenario.Success
        messageForwards = messageForwards + (profileId to operation)
        return true
    }
    fun advanceMessageForward(profileId: String, requestId: Long, revision: Int): Boolean {
        val operation = messageForwards[profileId]?.takeIf { it.id == requestId && it.revision == revision && it.isRunning } ?: return false
        val sourceProfile = uiState.profiles.firstOrNull { it.id == operation.sourceProfileId }
        val destination = uiState.profiles.firstOrNull { it.id == operation.destinationProfileId }
        val source = sourceProfile?.chats?.firstOrNull { it.id == operation.sourceChatId }
        val sources = source?.timeline?.filterIsInstance<ChatTimelineEntry.Message>()?.map { it.message }?.filter { it.id in operation.sourceMessageIds }.orEmpty()
        val scenario = operation.scenario.takeIf { operation.attempt == 0 || (it == MessageForwardScenario.PartialSendUntilRetried && operation.manualRetries == 0) } ?: MessageForwardScenario.Success
        val globalFailure = when {
            uiState.activeProfileId != profileId || sourceProfile == null || destination == null ||
                operation.sourceProfileId !in uiState.signedInProfileIds || operation.destinationProfileId !in uiState.signedInProfileIds ||
                scenario == MessageForwardScenario.SessionChanged -> MessageForwardFailure.SessionChanged
            source == null || sources.size != operation.sourceMessageIds.size -> MessageForwardFailure.SourceUnavailable
            MessageForwarding.sourceFailure(sources) != null -> MessageForwarding.sourceFailure(sources)
            scenario == MessageForwardScenario.Expired -> MessageForwardFailure.Expired
            scenario == MessageForwardScenario.PayloadTooLarge -> MessageForwardFailure.PayloadTooLarge
            operation.phase == MessageForwardPhase.Preparing && scenario == MessageForwardScenario.PreparationFails -> MessageForwardFailure.Preparation
            operation.phase == MessageForwardPhase.Preparing && scenario == MessageForwardScenario.PreparationTimeout -> MessageForwardFailure.PreparationTimeout
            else -> null
        }
        fun store(next: MessageForwardOperation) { messageForwards = messageForwards + (profileId to next.copy(revision = revision + 1)) }
        if (globalFailure != null) {
            store(operation.copy(targets = operation.targets.map { if (it.phase == MessageForwardTargetPhase.Completed) it else
                it.copy(phase = MessageForwardTargetPhase.Failed, failure = globalFailure) }).settled())
            return true
        }
        if (operation.phase == MessageForwardPhase.Preparing) {
            val prepared = (operation.prepared + 1).coerceAtMost(operation.totalAttachments)
            store(operation.copy(prepared = prepared, phase = if (prepared == operation.totalAttachments) MessageForwardPhase.Running else MessageForwardPhase.Preparing).settled())
            return true
        }
        val targetIndex = operation.targets.indexOfFirst { it.phase in setOf(MessageForwardTargetPhase.Waiting, MessageForwardTargetPhase.Uploading, MessageForwardTargetPhase.Sending) }
        if (targetIndex < 0) { store(operation.settled()); return true }
        val target = operation.targets[targetIndex]
        val targetChat = destination!!.chats.firstOrNull { it.id == target.chatId }
        val failure = MessageForwarding.targetFailure(destination, targetChat)
        val next = if (failure != null) target.copy(phase = MessageForwardTargetPhase.Failed, failure = failure) else when (target.phase) {
            MessageForwardTargetPhase.Waiting -> target.copy(phase = if (operation.totalAttachments > 0) MessageForwardTargetPhase.Uploading else MessageForwardTargetPhase.Sending)
            MessageForwardTargetPhase.Uploading -> {
                if (scenario == MessageForwardScenario.PartialUpload && targetIndex == minOf(1, operation.targets.lastIndex))
                    target.copy(phase = MessageForwardTargetPhase.Failed, failure = MessageForwardFailure.Upload)
                else target.copy(uploaded = (target.uploaded + 1).coerceAtMost(operation.totalAttachments),
                    phase = if (target.uploaded + 1 >= operation.totalAttachments) MessageForwardTargetPhase.Sending else MessageForwardTargetPhase.Uploading)
            }
            MessageForwardTargetPhase.Sending -> {
                if (scenario in setOf(MessageForwardScenario.PartialSend, MessageForwardScenario.PartialSendUntilRetried) && targetIndex == minOf(1, operation.targets.lastIndex) && target.sent == minOf(1, operation.messages.lastIndex))
                    target.copy(phase = MessageForwardTargetPhase.Failed, failure = MessageForwardFailure.Send)
                else {
                    val (day, minute) = nextTimelinePosition(targetChat!!)
                    val copy = MessageForwarding.copyForDestination(operation.messages[target.sent], operation.id, targetChat, destination.id, target.sent, day, minute)
                    uiState = uiState.copy(profiles = uiState.profiles.map { profile -> if (profile.id != destination.id) profile else profile.copy(chats = profile.chats.map { chat ->
                        if (chat.id != target.chatId || chat.timeline.any { it.id == copy.id }) chat else {
                            val updated = chat.copy(timeline = chat.timeline + ChatTimelineEntry.Message(copy),
                                preview = copy.text.ifBlank { copy.attachments.firstOrNull()?.label.orEmpty() }, previewAuthor = "You",
                                attachmentPreview = attachmentPreview(copy.attachments), timestamp = "Now")
                            updated.copy(readState = updated.readState?.let { ConversationReading.reconcile(it, updated, profile.id) })
                        }
                    }) })
                    target.copy(sent = target.sent + 1, phase = if (target.sent + 1 == operation.messages.size) MessageForwardTargetPhase.Completed else MessageForwardTargetPhase.Sending)
                }
            }
            else -> target
        }
        store(operation.copy(targets = operation.targets.mapIndexed { i, value -> if (i == targetIndex) next else value }).settled())
        return true
    }
    fun retryMessageForward(profileId: String, requestId: Long, automatic: Boolean = false, expectedRevision: Int? = null): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val op = messageForwards[profileId]?.takeIf { it.id == requestId && it.canRetry } ?: return false
        if ((automatic && !op.canAutomaticallyRetry) || (expectedRevision != null && expectedRevision != op.revision)) return false
        messageForwards = messageForwards + (profileId to op.copy(attempt = op.attempt + 1, revision = op.revision + 1,
            automaticRetries = op.automaticRetries + if (automatic) 1 else 0,
            manualRetries = op.manualRetries + if (automatic) 0 else 1,
            phase = if (op.prepared < op.totalAttachments || op.targets.any { it.failure in setOf(MessageForwardFailure.Preparation, MessageForwardFailure.PreparationTimeout) })
                MessageForwardPhase.Preparing else MessageForwardPhase.Running,
            targets = op.targets.map { if (it.phase == MessageForwardTargetPhase.Failed && it.failure?.retryable == true)
                it.copy(phase = MessageForwardTargetPhase.Waiting, failure = null) else it }))
        return true
    }
    fun cancelMessageForward(profileId: String, requestId: Long): Boolean {
        if (uiState.activeProfileId != profileId) return false
        val op = messageForwards[profileId]?.takeIf { it.id == requestId && it.canCancel } ?: return false
        messageForwards = messageForwards + (profileId to op.copy(phase = MessageForwardPhase.Cancelled, revision = op.revision + 1,
            targets = op.targets.map { if (it.phase in setOf(MessageForwardTargetPhase.Completed, MessageForwardTargetPhase.Failed)) it else it.copy(phase = MessageForwardTargetPhase.Cancelled) }))
        return true
    }
    fun dismissMessageForward(profileId: String, requestId: Long) {
        if (uiState.activeProfileId == profileId && messageForwards[profileId]?.let { it.id == requestId && !it.isRunning } == true)
            messageForwards = messageForwards - profileId
    }
    fun interruptMessageOperations(profileId: String? = uiState.activeProfileId) {
        if (profileId == null) return
        if (photoEditorSession?.profileId == profileId) photoEditorSession = null
        if (locationSession?.profileId == profileId) locationSession = null
        messageForwards = messageForwards.mapValues { (_, op) ->
            if (!op.isRunning || profileId !in setOf(op.sourceProfileId, op.destinationProfileId)) op else op.copy(revision = op.revision + 1,
                targets = op.targets.map { if (it.phase == MessageForwardTargetPhase.Completed) it else it.copy(phase = MessageForwardTargetPhase.Failed, failure = MessageForwardFailure.SessionChanged) }).settled()
        }
        uiState = uiState.copy(profiles = uiState.profiles.map { profile -> if (profile.id != profileId) profile else profile.copy(chats = profile.chats.map { chat ->
            val op = chat.messageDeletion
            chat.copy(
                timeline = chat.timeline.map { entry ->
                    if (entry !is ChatTimelineEntry.Message || entry.message.attachments.none { it.transfer?.running == true }) entry
                    else entry.copy(message = entry.message.copy(attachments = entry.message.attachments.map { it.copy(transfer = it.transfer?.cancel()) }))
                },
                messageDeletion = if (op?.isRunning != true) op else op.copy(revision = op.revision + 1, items = op.items.map {
                    if (it.phase == MessageDeletePhase.Pending) it.copy(phase = MessageDeletePhase.Failed, failure = MessageDeleteFailure.SessionChanged) else it
                }),
            )
        }) })
    }
    fun forwardMessages(sourceChatId: String, messageIds: Set<String>, targetChatIds: List<String>): Boolean {
        val owner = uiState.activeProfileId ?: return false
        if (!beginMessageForward(owner, sourceChatId, messageIds, owner, targetChatIds)) return false
        while (messageForwards[owner]?.isRunning == true) {
            val op = messageForwards.getValue(owner); advanceMessageForward(owner, op.id, op.revision)
        }
        return messageForwards[owner]?.phase == MessageForwardPhase.Completed
    }
    fun forwardMediaFrame(sourceChatId: String, mediaKey: ConversationMediaKey, targetChatIds: List<String>, accompanyingText: String = ""): Boolean {
        val owner = uiState.activeProfileId ?: return false
        if (!beginMessageForward(owner, sourceChatId, setOf(mediaKey.messageId), owner, targetChatIds, mediaKey, accompanyingText)) return false
        while (messageForwards[owner]?.isRunning == true) {
            val op = messageForwards.getValue(owner); advanceMessageForward(owner, op.id, op.revision)
        }
        return messageForwards[owner]?.phase == MessageForwardPhase.Completed
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
        preserveDraft: Boolean = false,
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
                            isDraft = preserveDraft && (chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty()),
                            draftText = if (preserveDraft) chat.draftText else "",
                            draftAttachments = if (preserveDraft) chat.draftAttachments else emptyList(),
                            suppressedDraftLinkUrl = if (preserveDraft) chat.suppressedDraftLinkUrl else null,
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
        interruptMessageOperations()
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

    private fun attachmentPreview(attachments: List<MessageAttachment>) = messageAttachmentPreview(attachments)

}
