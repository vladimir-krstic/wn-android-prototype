package dev.ipf.whitenoise.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.AttachmentPreview
import dev.ipf.whitenoise.model.ChatDeliveryState
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
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.RelayConnectionStatus
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.model.ConversationDebugSnapshot
import dev.ipf.whitenoise.model.DiagnosticEvent
import dev.ipf.whitenoise.model.KeyPackage
import dev.ipf.whitenoise.model.ProfileExitDestination
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.model.LinkPreviewDetector
import dev.ipf.whitenoise.model.VoiceMessageFixture
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.model.visibleText
import dev.ipf.whitenoise.navigation.OnboardingOrigin

data class AppUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String? = null,
    val signedInProfileIds: Set<String> = emptySet(),
) {
    val activeProfile: Profile?
        get() = profiles.firstOrNull { it.id == activeProfileId }

    val signedInProfiles: List<Profile>
        get() = profiles.filter { it.id in signedInProfileIds }
}

class AppViewModel : ViewModel() {
    private var createdChatSequence = 0

    var uiState by mutableStateOf(AppUiState())
        private set

    fun completeSignIn(origin: OnboardingOrigin) {
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
        uiState = uiState.copy(activeProfileId = profileId)
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
                developerTools = profile.developerTools.copy(
                    auditFiles = profile.developerTools.auditFiles.map { it.copy(profileName = trimmedName) },
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

    fun setAnonymousTelemetry(enabled: Boolean): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled) tools else tools.copy(anonymousTelemetry = enabled)
    }

    fun setAuditLogging(enabled: Boolean): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled) tools else tools.copy(auditLogging = enabled)
    }

    fun clearAuditLogs(): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled || !tools.auditLogging || !tools.auditLogsContainData) {
            tools
        } else {
            tools.copy(auditFiles = tools.auditFiles.map { it.copy(byteCount = 0) })
        }
    }

    fun publishKeyPackage(): Boolean = updateDeveloperTools { tools ->
        if (!tools.isEnabled || tools.keyPackage == KeyPackage.PublishedFixture) tools
        else tools.copy(keyPackage = KeyPackage.PublishedFixture)
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
        val signedIn = uiState.signedInProfileIds - activeId
        val profiles = if (wipeData) uiState.profiles.filterNot { it.id == activeId } else uiState.profiles
        val nextActiveId = profiles.firstOrNull { it.id in signedIn }?.id
        uiState = AppUiState(
            profiles = profiles,
            activeProfileId = nextActiveId,
            signedInProfileIds = signedIn,
        )
        return if (signedIn.isEmpty()) ProfileExitDestination.Welcome else ProfileExitDestination.ProfileSwitcher
    }

    fun removeStoredProfile(profileId: String, confirmation: String): Boolean {
        val profile = uiState.profiles.firstOrNull { it.id == profileId } ?: return false
        if (profile.id == uiState.activeProfileId || !WipeConfirmationPhrase.matches(confirmation, profile.name)) {
            return false
        }
        uiState = uiState.copy(
            profiles = uiState.profiles.filterNot { it.id == profileId },
            signedInProfileIds = uiState.signedInProfileIds - profileId,
        )
        return true
    }

    fun eraseAppData(confirmation: String): Boolean {
        val expected = WipeConfirmationPhrase.make(uiState.profiles.map(Profile::id))
        if (!WipeConfirmationPhrase.matches(confirmation, expected)) return false
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
        mutateChat(chatId) { chat ->
            chat.copy(unreadCount = 0, isMarkedUnread = false)
        }
    }

    fun markChatUnread(chatId: String, unread: Boolean) {
        mutateChat(chatId) { chat ->
            chat.copy(
                unreadCount = if (unread) chat.unreadCount else 0,
                isMarkedUnread = unread,
            )
        }
    }

    fun markAllChatsRead() {
        updateActiveProfile { profile ->
            profile.copy(
                chats = profile.chats.map { chat ->
                    if (chat.isArchived) chat else chat.copy(unreadCount = 0, isMarkedUnread = false)
                },
            )
        }
    }

    fun toggleChatPin(chatId: String) {
        mutateChat(chatId) { chat ->
            if (chat.isArchived) chat else chat.copy(isPinned = !chat.isPinned)
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
        format: VoiceMessageFormat,
        transcript: String,
    ): Boolean {
        val (text, attachments) = VoiceMessageFixture.result(
            id = "$chatId-voice-${createdChatSequence + 1}",
            format = format,
            editedTranscript = transcript,
        )
        if ((format == VoiceMessageFormat.Text || format == VoiceMessageFormat.Both) && text.isBlank()) {
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
                            unreadCount = 0,
                            isMarkedUnread = false,
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

    fun openOrCreateSupportChat(requestedChatId: String = "white-noise-support"): String? {
        val profile = uiState.activeProfile ?: return null
        profile.chats.firstOrNull { it.id == "white-noise-support" }?.let { return it.id }
        if (profile.chatRelayUrls.isEmpty()) return null
        val support = Chat(
            id = requestedChatId,
            originalOrder = 0,
            kind = ChatKind.Direct("white-noise-support"),
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
            title = person.name,
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
            val name = uiState.activeProfile?.people?.firstOrNull { it.id == personId }?.name ?: "Member"
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
            val name = uiState.activeProfile?.people?.firstOrNull { it.id == personId }?.name ?: "Member"
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
        val profiles = uiState.profiles.toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            if (updatesStoredProfile) {
                profiles[index] = profiles[index].updateEditableValues(from = profile)
            }
        } else {
            profiles += profile
        }

        uiState = uiState.copy(
            profiles = profiles,
            activeProfileId = profile.id,
            signedInProfileIds = uiState.signedInProfileIds + profile.id,
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
            profile.copy(chats = profile.chats.map { if (it.id == chatId) transform(it) else it })
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
