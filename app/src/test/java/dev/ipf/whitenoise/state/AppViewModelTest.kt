package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ProfileRelayFixtures
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.RelayRole
import dev.ipf.whitenoise.model.RelayConnectionStatus
import dev.ipf.whitenoise.model.PublishedRelayList
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.model.ChatFixtures
import dev.ipf.whitenoise.model.ChatProjection
import dev.ipf.whitenoise.model.ChatScope
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.ConversationMediaProjection
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.VoiceDraftSubmission
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppViewModelTest {
    @Test
    fun initialSignInCreatesOneActiveMarmotaProfile() {
        val viewModel = AppViewModel()

        viewModel.completeSignIn(OnboardingOrigin.Initial)

        assertEquals(ProfileFixtures.MARMOTA_ID, viewModel.uiState.activeProfileId)
        assertEquals(listOf(ProfileFixtures.MARMOTA_ID), viewModel.uiState.profiles.map { it.id })
        assertEquals(setOf(ProfileFixtures.MARMOTA_ID), viewModel.uiState.signedInProfileIds)
    }

    @Test
    fun addedSignUpActivatesPebbleAndExposesShowcaseProfiles() {
        val viewModel = AppViewModel()
        viewModel.completeSignIn(OnboardingOrigin.Initial)

        viewModel.completeSignUp(OnboardingOrigin.AddProfile, "Pebble", "", null)

        assertEquals(ProfileFixtures.PEBBLE_ID, viewModel.uiState.activeProfileId)
        assertEquals(7, viewModel.uiState.signedInProfiles.size)
        assertTrue(
            ProfileFixtures.showcaseProfiles.all { profile ->
                profile.id in viewModel.uiState.signedInProfileIds
            },
        )
        assertEquals(
            setOf(
                ProfileFixtures.MARMOTA_ID,
                ProfileFixtures.PEBBLE_ID,
                "open-quill",
                "cipher-wheel",
                "free-signal",
                "public-voice",
                "liberty-relay",
            ),
            viewModel.uiState.signedInProfiles.mapTo(mutableSetOf()) { it.id },
        )
    }

    @Test
    fun addedSignInUsesOpenCircuitInsteadOfPebbleInTheSevenProfileSet() {
        val viewModel = AppViewModel()
        viewModel.completeSignIn(OnboardingOrigin.Initial)

        viewModel.completeSignIn(OnboardingOrigin.AddProfile)

        assertEquals("open-circuit", viewModel.uiState.activeProfileId)
        assertEquals(
            setOf(
                ProfileFixtures.MARMOTA_ID,
                "open-circuit",
                "open-quill",
                "cipher-wheel",
                "free-signal",
                "public-voice",
                "liberty-relay",
            ),
            viewModel.uiState.signedInProfiles.mapTo(mutableSetOf()) { it.id },
        )
    }

    @Test
    fun profileSwitchingRequiresAStoredSignedInProfile() {
        val viewModel = AppViewModel()
        viewModel.completeSignIn(OnboardingOrigin.Initial)
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)

        viewModel.selectProfile(ProfileFixtures.MARMOTA_ID)
        assertEquals(ProfileFixtures.MARMOTA_ID, viewModel.uiState.activeProfileId)

        viewModel.selectProfile("missing")
        assertEquals(ProfileFixtures.MARMOTA_ID, viewModel.uiState.activeProfileId)
    }

    @Test
    fun openingCapturesUnreadAndReadAllMutatesAuthoritativeRowsOutsideFilters() {
        val viewModel = signedInMarmota()

        viewModel.openChat("catalog-direct-replies")
        assertTrue(viewModel.chat("catalog-direct-replies")!!.isUnread)
        assertTrue(viewModel.chat("catalog-direct-replies")!!.readState != null)

        viewModel.markAllChatsRead()
        assertTrue(viewModel.uiState.activeProfile!!.chats.filter { !it.isArchived }.none { it.isUnread })
        assertEquals(4, viewModel.chat("garden-club")!!.unreadCount)
    }

    @Test
    fun pinAndUnpinUseStableFixtureOrdering() {
        val viewModel = signedInMarmota()
        val originalIds = ChatProjection.rows(
            viewModel.uiState.activeProfile!!.chats,
            ChatScope.Chats,
        ).map { it.id }

        viewModel.toggleChatPin("catalog-direct-replies")
        assertEquals(
            listOf("catalog-direct-text", "catalog-direct-replies"),
            ChatProjection.rows(viewModel.uiState.activeProfile!!.chats, ChatScope.Chats).take(2).map { it.id },
        )

        viewModel.toggleChatPin("catalog-direct-replies")
        assertEquals(
            originalIds,
            ChatProjection.rows(viewModel.uiState.activeProfile!!.chats, ChatScope.Chats).map { it.id },
        )
    }

    @Test
    fun directCreationDeduplicatesAndCopiesActiveProfileRelays() {
        val viewModel = signedInEmptyProfile()

        val firstId = viewModel.openOrCreateDirectChat("maya-chen", "created-maya")
        val secondId = viewModel.openOrCreateDirectChat("maya-chen", "another-id")

        assertEquals("created-maya", firstId)
        assertEquals(firstId, secondId)
        assertEquals(1, viewModel.uiState.activeProfile!!.chats.size)
        assertEquals(
            viewModel.uiState.activeProfile!!.chatRelayUrls,
            viewModel.chat(firstId!!)!!.relayUrls,
        )
    }

    @Test
    fun groupCreationValidatesMembersAndMakesCurrentProfileAdmin() {
        val viewModel = signedInEmptyProfile()
        val profile = viewModel.uiState.activeProfile!!

        assertNull(viewModel.createGroup(" ", "", ProfileAvatar.Monogram, listOf("maya-chen")))
        assertNotNull(viewModel.createGroup("Solo", "", ProfileAvatar.Monogram, emptyList()))
        val chatId = viewModel.createGroup(
            name = "  Weekend Crew  ",
            description = "  A quiet plan.  ",
            avatar = ProfileAvatar.Monogram,
            selectedPersonIds = listOf("maya-chen", "maya-chen", profile.id, "missing"),
            requestedChatId = "created-group",
        )
        val chat = viewModel.chat(chatId!!)!!

        assertEquals("Weekend Crew", chat.title)
        assertEquals("A quiet plan.", chat.description)
        assertEquals(listOf(profile.id, "maya-chen"), chat.members.map { it.personId })
        assertEquals(GroupRole.Admin, chat.members.first().role)
        assertEquals(profile.chatRelayUrls, chat.relayUrls)
        assertEquals("You created the group.", chat.preview)
    }

    @Test
    fun soleAdminCannotLeaveButOrdinaryActiveGroupCan() {
        val viewModel = signedInMarmota()

        assertFalse(viewModel.leaveChat("catalog-group-sole-admin"))
        assertTrue(viewModel.leaveChat("catalog-group-messages"))
        assertTrue(viewModel.chat("catalog-group-messages")!!.hasEndedMembership)
    }

    @Test
    fun chatMutationsStayIsolatedToTheActiveProfile() {
        val viewModel = signedInMarmota()
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        viewModel.openOrCreateDirectChat("maya-chen", "open-circuit-maya")
        assertEquals(1, viewModel.uiState.activeProfile!!.chats.size)

        viewModel.selectProfile(ProfileFixtures.MARMOTA_ID)
        assertEquals(77, viewModel.uiState.activeProfile!!.chats.size)
        assertEquals(5, viewModel.uiState.activeProfile!!.chats.count { it.id in ChatFixtures.archivedIds })
    }

    @Test
    fun acceptingInvitationsPreservesHistoryAndAddsGroupMembershipEvent() {
        val viewModel = signedInMarmota()
        val directHistory = viewModel.chat("catalog-direct-invitation")!!.timeline
        val groupBefore = viewModel.chat("catalog-group-invitation")!!
        assertFalse(groupBefore.members.any { it.personId == ProfileFixtures.MARMOTA_ID })

        assertTrue(viewModel.acceptInvitation("catalog-direct-invitation"))
        assertEquals(directHistory, viewModel.chat("catalog-direct-invitation")!!.timeline)
        assertEquals(ChatMembership.Active, viewModel.chat("catalog-direct-invitation")!!.membership)

        assertTrue(viewModel.acceptInvitation("catalog-group-invitation"))
        val groupAfter = viewModel.chat("catalog-group-invitation")!!
        assertTrue(groupAfter.members.any { it.personId == ProfileFixtures.MARMOTA_ID })
        assertEquals("You joined the group.", (groupAfter.timeline.last() as ChatTimelineEntry.Event).text)
    }

    @Test
    fun decliningInvitationRemovesOnlyThatPendingChat() {
        val viewModel = signedInMarmota()
        val count = viewModel.uiState.activeProfile!!.chats.size

        assertTrue(viewModel.declineInvitation("catalog-direct-invitation"))
        assertNull(viewModel.chat("catalog-direct-invitation"))
        assertEquals(count - 1, viewModel.uiState.activeProfile!!.chats.size)
        assertFalse(viewModel.declineInvitation("fiatjaf"))
    }

    @Test
    fun sendAppendsOutgoingMessageAndUpdatesAuthoritativeRowPreview() {
        val viewModel = signedInMarmota()
        val before = viewModel.chat("fiatjaf")!!.timeline.size

        assertTrue(viewModel.sendText("fiatjaf", "  A deterministic reply.  "))
        val chat = viewModel.chat("fiatjaf")!!
        val sent = (chat.timeline.last() as ChatTimelineEntry.Message).message
        assertEquals(before + 1, chat.timeline.size)
        assertEquals("A deterministic reply.", sent.text)
        assertEquals(ProfileFixtures.MARMOTA_ID, sent.authorId)
        assertEquals("A deterministic reply.", chat.preview)
        assertEquals("You", chat.previewAuthor)
        assertFalse(viewModel.sendText("fiatjaf", "   "))
        assertFalse(viewModel.sendText("catalog-direct-missing-relays", "Blocked"))
    }

    @Test
    fun retryChangesOnlyFailedOutgoingMessage() {
        val viewModel = signedInMarmota()
        val failed = viewModel.chat("catalog-direct-text")!!.timeline
            .filterIsInstance<ChatTimelineEntry.Message>()
            .first { it.message.id == "DLV-03" }
        assertEquals(MessageDeliveryState.Failed, failed.message.deliveryState)

        assertTrue(viewModel.retryMessage("catalog-direct-text", "DLV-03"))
        val retried = viewModel.chat("catalog-direct-text")!!.timeline
            .filterIsInstance<ChatTimelineEntry.Message>()
            .first { it.message.id == "DLV-03" }
        assertEquals(MessageDeliveryState.Sent, retried.message.deliveryState)
        assertFalse(viewModel.retryMessage("catalog-direct-text", "TXT-01"))
    }

    @Test
    fun supportCreationIsStableUniqueAndCopiesRelays() {
        val viewModel = signedInEmptyProfile()

        val first = viewModel.openOrCreateSupportChat()
        val second = viewModel.openOrCreateSupportChat()

        assertEquals(ChatFixtures.SUPPORT_CHAT_ID, first)
        assertEquals(first, second)
        assertEquals(1, viewModel.uiState.activeProfile!!.chats.count { it.id == ChatFixtures.SUPPORT_CHAT_ID })
        assertEquals(viewModel.uiState.activeProfile!!.chatRelayUrls, viewModel.chat(first!!)!!.relayUrls)
        assertEquals(ComposerAvailability.Available, viewModel.composerAvailability(first))
    }

    @Test
    fun draftTextAndAttachmentOrderPersistAcrossNavigationAndProfileSwitching() {
        val viewModel = signedInMarmota()
        val first = MessageAttachment("first", MessageAttachmentKind.File, "First.pdf")
        val second = MessageAttachment("second", MessageAttachmentKind.Photo, "Second")
        viewModel.updateDraftText("fiatjaf", "Persistent draft")
        viewModel.addDraftAttachments("fiatjaf", listOf(first, second, first))
        viewModel.openChat("maya-chen")
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        viewModel.selectProfile(ProfileFixtures.MARMOTA_ID)

        val draft = viewModel.chat("fiatjaf")!!
        assertEquals("Persistent draft", draft.draftText)
        assertEquals(listOf("first", "second"), draft.draftAttachments.map { it.id })

        viewModel.removeDraftAttachment("fiatjaf", "first")
        assertEquals(listOf("second"), viewModel.chat("fiatjaf")!!.draftAttachments.map { it.id })
    }

    @Test
    fun sendDraftClearsEveryDraftFieldAndAddsLocalLinkMetadataOnce() {
        val viewModel = signedInMarmota()
        viewModel.updateDraftText("fiatjaf", "Read https://developer.android.com")
        viewModel.addDraftAttachments(
            "fiatjaf",
            listOf(MessageAttachment("file", MessageAttachmentKind.File, "Notes.pdf")),
        )

        assertTrue(viewModel.sendDraft("fiatjaf"))
        val chat = viewModel.chat("fiatjaf")!!
        val sent = (chat.timeline.last() as ChatTimelineEntry.Message).message
        assertEquals(listOf(MessageAttachmentKind.File, MessageAttachmentKind.Link), sent.attachments.map { it.kind })
        assertEquals("Android Developers", sent.attachments.last().linkTitle)
        assertEquals("", chat.draftText)
        assertTrue(chat.draftAttachments.isEmpty())
        assertNull(chat.suppressedDraftLinkUrl)
        assertNull(chat.draftReplyMessageId)
        assertFalse(chat.hasDraft)
    }

    @Test
    fun suppressingDraftLinkKeepsTextButDoesNotCreateLinkAttachment() {
        val viewModel = signedInMarmota()
        val url = "https://whitenoise.chat"
        viewModel.updateDraftText("fiatjaf", url)
        viewModel.suppressDraftLink("fiatjaf", url)

        assertTrue(viewModel.sendDraft("fiatjaf"))
        val sent = (viewModel.chat("fiatjaf")!!.timeline.last() as ChatTimelineEntry.Message).message
        assertEquals(url, sent.text)
        assertTrue(sent.attachments.isEmpty())
    }

    @Test
    fun voiceTextAndBothEachAppendExactlyOneMessage() {
        VoiceMessageFormat.entries.forEach { format ->
            val viewModel = signedInMarmota()
            val before = viewModel.chat("fiatjaf")!!.timeline.size

            assertTrue(
                viewModel.sendVoice(
                    "fiatjaf",
                    VoiceDraftSubmission(format, "Edited transcript", durationSeconds = 17),
                ),
            )
            val chat = viewModel.chat("fiatjaf")!!
            val sent = (chat.timeline.last() as ChatTimelineEntry.Message).message
            assertEquals(before + 1, chat.timeline.size)
            assertEquals(if (format == VoiceMessageFormat.Voice) "" else "Edited transcript", sent.text)
            assertEquals(if (format == VoiceMessageFormat.Text) 0 else 1, sent.attachments.size)
            sent.attachments.singleOrNull()?.let { assertEquals(17, it.durationSeconds) }
        }
    }

    @Test
    fun blankVoiceTranscriptIsRejectedForTextAndBothButNotVoice() {
        VoiceMessageFormat.entries.forEach { format ->
            val viewModel = signedInMarmota()
            val accepted = viewModel.sendVoice(
                "fiatjaf",
                VoiceDraftSubmission(format, "   ", durationSeconds = 3),
            )
            assertEquals(format == VoiceMessageFormat.Voice, accepted)
        }
    }

    @Test
    fun reactionReplacementAndContextRemovalKeepOneReactionPerProfile() {
        val viewModel = signedInMarmota()
        val chatId = "catalog-direct-reactions"
        val messageId = "RCT-03"

        assertTrue(viewModel.setMessageReaction(chatId, messageId, "👍", false))
        assertFalse(viewModel.setMessageReaction(chatId, messageId, "👍", false))
        assertTrue(viewModel.setMessageReaction(chatId, messageId, "🚀", false))
        var message = viewModel.message(chatId, messageId)!!
        assertEquals(listOf("🚀"), message.reactions.filter { ProfileFixtures.MARMOTA_ID in it.personIds }.map { it.emoji })

        assertTrue(viewModel.setMessageReaction(chatId, messageId, "🚀", true))
        message = viewModel.message(chatId, messageId)!!
        assertTrue(message.reactions.none { ProfileFixtures.MARMOTA_ID in it.personIds })
    }

    @Test
    fun replyAndQuickReactionPreferencesAreProfileOwned() {
        val viewModel = signedInMarmota()
        assertTrue(viewModel.setDraftReply("fiatjaf", "fiatjaf-7"))
        assertEquals("fiatjaf-7", viewModel.chat("fiatjaf")!!.draftReplyMessageId)
        val custom = listOf("😀", "😃", "😄", "😁", "😆", "😂")
        assertTrue(viewModel.setQuickReactions(custom))
        assertEquals(custom, viewModel.uiState.activeProfile!!.quickReactions)

        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        assertEquals(dev.ipf.whitenoise.model.ReactionCatalog.defaults, viewModel.uiState.activeProfile!!.quickReactions)
        viewModel.selectProfile(ProfileFixtures.MARMOTA_ID)
        assertEquals(custom, viewModel.uiState.activeProfile!!.quickReactions)
    }

    @Test
    fun deletionScopesRemoveLocallyOrLeaveCleanOutgoingTombstone() {
        val local = signedInMarmota()
        assertTrue(local.setDraftReply("catalog-direct-text", "TXT-01"))
        assertTrue(local.deleteMessages("catalog-direct-text", setOf("TXT-01"), MessageDeletionScope.ForMe))
        assertNull(local.message("catalog-direct-text", "TXT-01"))
        assertNull(local.chat("catalog-direct-text")!!.draftReplyMessageId)

        val everyone = signedInMarmota()
        assertFalse(everyone.deleteMessages("catalog-direct-text", setOf("TXT-01"), MessageDeletionScope.ForEveryone))
        assertTrue(everyone.deleteMessages("catalog-direct-text", setOf("TXT-02"), MessageDeletionScope.ForEveryone))
        val tombstone = everyone.message("catalog-direct-text", "TXT-02")!!
        assertEquals(dev.ipf.whitenoise.model.MessageDeletionState.DeletedByCurrentProfile, tombstone.deletionState)
        assertTrue(tombstone.attachments.isEmpty())
        assertTrue(tombstone.reactions.isEmpty())
        assertNull(tombstone.replyToMessageId)
    }

    @Test
    fun forwardingPreservesSourceOrderAndPayloadAcrossMoreThanFiveTargets() {
        val viewModel = signedInMarmota()
        val targetIds = listOf("maya-chen", "fiatjaf")
        val sourceIds = setOf("TXT-01", "TXT-02")
        val before = targetIds.associateWith { viewModel.chat(it)!!.timeline.size }

        assertTrue(viewModel.forwardMessages("catalog-direct-text", sourceIds, targetIds))
        targetIds.forEach { targetId ->
            val copies = viewModel.chat(targetId)!!.timeline.drop(before.getValue(targetId))
                .filterIsInstance<ChatTimelineEntry.Message>()
                .map(ChatTimelineEntry.Message::message)
            assertEquals(listOf("TXT-01: Incoming short text", "TXT-02: Outgoing short text"), copies.map { it.text })
            assertTrue(copies.all { it.authorId == ProfileFixtures.MARMOTA_ID && it.replyToMessageId == null && it.reactions.isEmpty() })
        }
        assertTrue(viewModel.forwardMessages("catalog-direct-text", sourceIds, listOf("maya-chen", "fiatjaf", "weekend-walks", "theo-grant", "aisha-rahman", "nora-bennett")))
        assertFalse(viewModel.forwardMessages("catalog-direct-text", sourceIds, listOf("catalog-direct-text")))
    }

    @Test
    fun forwardingOneAlbumFrameNormalizesItToOnePhotoAndTrimsOptionalText() {
        val viewModel = signedInMarmota()
        val profile = viewModel.uiState.activeProfile!!
        val source = viewModel.chat("catalog-media-gallery")!!
        val selected = ConversationMediaProjection.items(source, profile).first {
            it.key.messageId == "MED-04" && it.key.attachmentId == "MED-04-photo-2"
        }
        val targetId = "maya-chen"
        val before = viewModel.chat(targetId)!!.timeline.size

        assertTrue(
            viewModel.forwardMediaFrame(
                sourceChatId = source.id,
                mediaKey = selected.key,
                targetChatIds = listOf(targetId),
                accompanyingText = "  Take a look  ",
            ),
        )

        val forwarded = (viewModel.chat(targetId)!!.timeline.drop(before).single() as ChatTimelineEntry.Message).message
        assertEquals("Take a look", forwarded.text)
        assertEquals(1, forwarded.attachments.size)
        assertEquals(MessageAttachmentKind.Photo, forwarded.attachments.single().kind)
        assertEquals(selected.attachment.label, forwarded.attachments.single().label)
        assertEquals(listOfNotNull(selected.image), forwarded.attachments.single().images)
        assertFalse(viewModel.forwardMediaFrame(source.id, selected.key, listOf(source.id)))
        assertFalse(viewModel.forwardMediaFrame(source.id, selected.key, emptyList()))
        assertFalse(
            viewModel.forwardMediaFrame(
                source.id,
                selected.key.copy(attachmentId = "missing"),
                listOf(targetId),
            ),
        )
    }

    @Test
    fun disappearingSettingUpdatesHeaderStateAndAppendsOneTypedEvent() {
        val viewModel = signedInMarmota()
        val before = viewModel.chat("fiatjaf")!!.timeline.size

        assertTrue(viewModel.setChatDisappearing("fiatjaf", dev.ipf.whitenoise.model.DisappearingDuration.OneWeek))
        val chat = viewModel.chat("fiatjaf")!!
        assertEquals(dev.ipf.whitenoise.model.DisappearingDuration.OneWeek, chat.disappearingDuration)
        assertEquals(before + 1, chat.timeline.size)
        assertEquals("You set disappearing messages to 1 week.", (chat.timeline.last() as ChatTimelineEntry.Event).text)
        assertFalse(viewModel.setChatDisappearing("fiatjaf", dev.ipf.whitenoise.model.DisappearingDuration.OneWeek))
    }

    @Test
    fun chatRelayEditingIsNormalizedIndependentAndRecoversAfterFinalRemoval() {
        val viewModel = signedInMarmota()
        val sibling = viewModel.chat("maya-chen")!!.relayUrls
        val relay = viewModel.chat("fiatjaf")!!.relayUrls.single()

        assertTrue(viewModel.removeChatRelay("fiatjaf", relay))
        assertTrue(viewModel.chat("fiatjaf")!!.relayUrls.isEmpty())
        assertEquals(ComposerAvailability.MissingRelays, viewModel.composerAvailability("fiatjaf"))
        assertEquals(sibling, viewModel.chat("maya-chen")!!.relayUrls)
        assertTrue(viewModel.addChatRelay("fiatjaf", " WSS://Relay.Example.com/path/ "))
        assertEquals(listOf("wss://relay.example.com/path"), viewModel.chat("fiatjaf")!!.relayUrls)
        assertEquals(ComposerAvailability.Available, viewModel.composerAvailability("fiatjaf"))
        assertTrue(viewModel.restoreChatRelays("fiatjaf"))
        assertEquals(listOf(ChatFixtures.DEFAULT_CHAT_RELAY), viewModel.chat("fiatjaf")!!.relayUrls)
    }

    @Test
    fun groupAdminMutationsUpdateMetadataMembersRolesAndTimelineAtomically() {
        val viewModel = signedInMarmota()
        val chatId = "weekend-walks"
        val before = viewModel.chat(chatId)!!.timeline.size

        assertTrue(viewModel.editGroup(chatId, " Weekend Ramblers ", "A new route.", ProfileAvatar.Monogram))
        assertTrue(viewModel.addGroupMembers(chatId, listOf("theo-grant", "theo-grant")))
        assertTrue(viewModel.setGroupMemberAdmin(chatId, "maya-chen", true))
        assertTrue(viewModel.setGroupMemberAdmin(chatId, "maya-chen", false))
        assertTrue(viewModel.removeGroupMember(chatId, "elias-moreno"))
        val chat = viewModel.chat(chatId)!!

        assertEquals("Weekend Ramblers", chat.title)
        assertEquals("A new route.", chat.description)
        assertTrue(chat.members.any { it.personId == "theo-grant" })
        assertTrue(chat.members.none { it.personId == "elias-moreno" })
        assertEquals(GroupRole.Member, chat.members.first { it.personId == "maya-chen" }.role)
        assertEquals(before + 5, chat.timeline.size)
    }

    @Test
    fun ordinaryMemberCannotAdministerAndDirectChatCanLeaveReadOnly() {
        val viewModel = signedInMarmota()
        assertFalse(viewModel.editGroup("product-circle", "Renamed", "", ProfileAvatar.Monogram))
        assertFalse(viewModel.addGroupMembers("product-circle", listOf("mina-park")))
        assertFalse(viewModel.setGroupMemberAdmin("product-circle", "maya-chen", true))
        assertFalse(viewModel.removeGroupMember("product-circle", "maya-chen"))

        assertTrue(viewModel.leaveChat("fiatjaf"))
        val left = viewModel.chat("fiatjaf")!!
        assertEquals(ChatMembership.Left, left.membership)
        assertEquals("You left this chat.", (left.timeline.last() as ChatTimelineEntry.Event).text)
    }

    @Test
    fun profileDetailsAddressAndSettingsMutateOnlyTheActiveProfile() {
        val viewModel = signedInMarmota()
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        val marmotaBefore = viewModel.uiState.profiles.first { it.id == ProfileFixtures.MARMOTA_ID }

        assertTrue(viewModel.updateActiveProfileDetails(" Open Circuit Updated ", " Local profile ", ProfileAvatar.Monogram))
        assertTrue(viewModel.updateNostrAddress("updated@whitenoise.example"))
        val settings = viewModel.uiState.activeProfile!!.settings.copy(
            appearance = dev.ipf.whitenoise.model.AppearancePreference.Dark,
        )
        assertTrue(viewModel.updateProfileSettings(settings))

        val updated = viewModel.uiState.activeProfile!!
        assertEquals("Open Circuit Updated", updated.name)
        assertEquals("Local profile", updated.about)
        assertEquals("updated@whitenoise.example", updated.nostrAddress)
        assertTrue(updated.isNostrAddressVerified)
        assertEquals(dev.ipf.whitenoise.model.AppearancePreference.Dark, updated.settings.appearance)
        assertEquals(marmotaBefore, viewModel.uiState.profiles.first { it.id == ProfileFixtures.MARMOTA_ID })
    }

    @Test
    fun profileRelayRolesControlOnlyFutureChatAvailability() {
        val viewModel = signedInEmptyProfile()
        val relayIds = viewModel.uiState.activeProfile!!.settings.relays
            .filter { dev.ipf.whitenoise.model.RelayRole.ChatMessages in it.roles }
            .map { it.id }
        relayIds.forEach {
            assertTrue(viewModel.setProfileRelayRole(it, dev.ipf.whitenoise.model.RelayRole.ChatMessages, false))
        }
        assertTrue(viewModel.uiState.activeProfile!!.chatRelayUrls.isEmpty())
        assertNull(viewModel.openOrCreateSupportChat())

        val relayId = dev.ipf.whitenoise.model.ProfileRelayFixtures.DEFAULT_CHAT_RELAY_ID
        assertTrue(viewModel.setProfileRelayRole(relayId, dev.ipf.whitenoise.model.RelayRole.ChatMessages, true))
        assertEquals(listOf(ChatFixtures.DEFAULT_CHAT_RELAY), viewModel.uiState.activeProfile!!.chatRelayUrls)
        assertEquals(ChatFixtures.SUPPORT_CHAT_ID, viewModel.openOrCreateSupportChat())
    }

    @Test
    fun profileRelayAddRemoveReadonlyAndRestoreRulesAreEnforced() {
        val viewModel = signedInEmptyProfile()
        assertTrue(viewModel.addProfileRelay("wss://relay.example.com/path/"))
        val custom = viewModel.uiState.activeProfile!!.settings.relays.last()
        assertEquals("wss://relay.example.com/path", custom.url)
        assertEquals(dev.ipf.whitenoise.model.RelayConnectionStatus.Reconnecting, custom.status)
        assertTrue(
            viewModel.setProfileRelayConnectionStatus(
                custom.id,
                dev.ipf.whitenoise.model.RelayConnectionStatus.Connected,
            ),
        )
        assertFalse(viewModel.addProfileRelay("wss://relay.example.com/path"))
        assertTrue(viewModel.removeProfileRelay(custom.id))
        assertFalse(viewModel.removeProfileRelay("vertex"))

        assertTrue(viewModel.setProfileRelayRole("damus", dev.ipf.whitenoise.model.RelayRole.Inbox, true))
        assertTrue(viewModel.restoreProfileRelays())
        assertEquals(dev.ipf.whitenoise.model.ProfileRelayFixtures.defaults, viewModel.uiState.activeProfile!!.settings.relays)
        assertEquals(
            listOf("wss://relay.primal.net", "wss://relay.damus.io", "wss://nos.lol"),
            viewModel.uiState.activeProfile!!.chatRelayUrls,
        )
    }

    @Test
    fun settingsValidationRejectsEmptyNamesAndMalformedAddresses() {
        val viewModel = signedInMarmota()
        val before = viewModel.uiState.activeProfile!!
        assertFalse(viewModel.updateActiveProfileDetails(" ", "Changed", ProfileAvatar.Monogram))
        assertFalse(viewModel.updateNostrAddress("not-an-address"))
        assertEquals(before, viewModel.uiState.activeProfile)
    }

    @Test
    fun developerPreferencesAreProfileScopedAndMasterDisablePreservesArtifacts() {
        val viewModel = signedInMarmota()
        val profileId = viewModel.uiState.activeProfileId!!
        assertTrue(viewModel.setDeveloperToolsEnabled(true))
        assertTrue(viewModel.setDebugMode(true))
        assertTrue(viewModel.setAnalyticsEnabled(profileId, true))
        assertTrue(viewModel.setDiagnosticLoggingEnabled(profileId, true))
        val diagnostics = viewModel.uiState.activeProfile!!.diagnostics
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(viewModel.uiState.activeProfile!!.developerTools.isEnabled)

        viewModel.selectProfile(ProfileFixtures.MARMOTA_ID)
        assertTrue(viewModel.setDeveloperToolsEnabled(false))
        val disabled = viewModel.uiState.activeProfile!!.developerTools
        assertFalse(disabled.debugMode)
        assertEquals(diagnostics, viewModel.uiState.activeProfile!!.diagnostics)
    }

    @Test
    fun diagnosticConsoleAndKeyPackageMutationsRespectDeveloperGate() {
        val viewModel = signedInMarmota()
        assertFalse(viewModel.developerParity.begin(dev.ipf.whitenoise.model.DeveloperOperation.PublishNew))
        assertFalse(viewModel.runDiagnosticTest())
        assertTrue(viewModel.setDeveloperToolsEnabled(true))
        val controller = viewModel.developerParity
        controller.open(viewModel.uiState.activeProfileId!!, "packages")
        controller.complete(controller.work!!.id)
        assertTrue(controller.begin(dev.ipf.whitenoise.model.DeveloperOperation.PublishNew))
        controller.complete(controller.work!!.id)
        assertEquals(dev.ipf.whitenoise.model.KeyPackage.PublishedFixture, viewModel.uiState.activeProfile!!.developerTools.keyPackage)
        assertTrue(viewModel.clearDiagnosticEvents())
        assertTrue(viewModel.runDiagnosticTest())
        assertEquals(listOf("18:42:15  diagnostic test passed"), viewModel.uiState.activeProfile!!.developerTools.diagnosticEvents.map { it.text })
    }

    @Test
    fun signOutWithoutWipeRetainsStoredProfileAndRoutesToWelcomeWhenLastSession() {
        val viewModel = signedInMarmota()
        viewModel.updateDraftText("fiatjaf", "Retained draft")
        assertEquals(
            dev.ipf.whitenoise.model.ProfileExitDestination.Welcome,
            viewModel.signOutActiveProfile(wipeData = false),
        )
        assertTrue(viewModel.uiState.signedInProfileIds.isEmpty())
        assertNull(viewModel.uiState.activeProfileId)
        assertEquals("Retained draft", viewModel.uiState.profiles.single().chats.first { it.id == "fiatjaf" }.draftText)

        viewModel.completeSignIn(OnboardingOrigin.Initial)
        assertEquals("Retained draft", viewModel.chat("fiatjaf")!!.draftText)
    }

    @Test
    fun signOutWithWipeRemovesOnlyActiveProfileAndSelectsRemainingSession() {
        val viewModel = signedInMarmota()
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        val activeId = viewModel.uiState.activeProfileId!!
        assertEquals(
            dev.ipf.whitenoise.model.ProfileExitDestination.ProfileSwitcher,
            viewModel.signOutActiveProfile(wipeData = true),
        )
        assertTrue(viewModel.uiState.profiles.none { it.id == activeId })
        assertFalse(activeId in viewModel.uiState.signedInProfileIds)
        assertEquals(ProfileFixtures.MARMOTA_ID, viewModel.uiState.activeProfileId)
    }

    @Test
    fun removeProfileNeedsExactNameAndCannotRemoveActiveProfile() {
        val viewModel = signedInMarmota()
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(viewModel.removeStoredProfile(ProfileFixtures.MARMOTA_ID, "wrong"))
        assertTrue(viewModel.removeStoredProfile(ProfileFixtures.MARMOTA_ID, " Marmota "))
        assertTrue(viewModel.uiState.profiles.none { it.id == ProfileFixtures.MARMOTA_ID })
        assertFalse(viewModel.removeStoredProfile(ProfileFixtures.PEBBLE_ID, "Pebble"))
    }

    @Test
    fun eraseAppDataRequiresStablePhraseAndFreshOnboardingRebuildsCanonicalState() {
        val viewModel = signedInMarmota()
        viewModel.completeSignIn(OnboardingOrigin.AddProfile)
        val phrase = dev.ipf.whitenoise.model.WipeConfirmationPhrase.make(viewModel.uiState.profiles.map { it.id })
        assertFalse(viewModel.eraseAppData(phrase.uppercase()))
        assertTrue(viewModel.eraseAppData(" $phrase "))
        assertTrue(viewModel.uiState.profiles.isEmpty())
        assertTrue(viewModel.uiState.signedInProfileIds.isEmpty())
        assertNull(viewModel.uiState.activeProfileId)

        viewModel.completeSignIn(OnboardingOrigin.Initial)
        assertEquals(ProfileFixtures.marmota, viewModel.uiState.activeProfile)
    }

    @Test
    fun disablingLocalNotificationsAlsoDisablesNativePush() {
        val viewModel = signedInMarmota()
        val settings = viewModel.uiState.activeProfile!!.settings

        assertTrue(
            viewModel.updateProfileSettings(
                settings.copy(localNotifications = false, nativePushNotifications = true),
            ),
        )

        assertFalse(viewModel.uiState.activeProfile!!.settings.localNotifications)
        assertFalse(viewModel.uiState.activeProfile!!.settings.nativePushNotifications)
    }

    private fun signedInMarmota() = AppViewModel().also {
        it.completeSignIn(OnboardingOrigin.Initial)
    }

    private fun signedInEmptyProfile() = AppViewModel().also {
        it.completeSignIn(OnboardingOrigin.AddProfile)
    }
    @Test
    fun relayPublicationChangesTrackOnlyProfileAndInboxRoles() {
        val viewModel = AppViewModel(); viewModel.completeSignIn(OnboardingOrigin.Initial)
        val profile = viewModel.uiState.activeProfile!!; val relay = profile.settings.relays.first { RelayRole.Profile in it.roles }
        assertTrue(viewModel.setProfileRelayRole(profile.id, relay.id, RelayRole.Profile, false))
        assertEquals(setOf(PublishedRelayList.Posting), viewModel.relayPublication.projection(viewModel.uiState.activeProfile!!).missing)
        val before = viewModel.relayPublication.projection(viewModel.uiState.activeProfile!!)
        assertTrue(viewModel.setProfileRelayConnectionStatus(profile.id, relay.id, RelayConnectionStatus.Disconnected))
        assertEquals(before, viewModel.relayPublication.projection(viewModel.uiState.activeProfile!!))
    }

    @Test
    fun staleRelayCallbacksCannotMutateNewActiveProfile() {
        val viewModel = AppViewModel(); viewModel.completeSignIn(OnboardingOrigin.Initial)
        val first = viewModel.uiState.activeProfile!!; val target = first.settings.relays.first()
        viewModel.completeSignIn(OnboardingOrigin.AddProfile); val second = viewModel.uiState.activeProfile!!
        assertFalse(viewModel.setProfileRelayRole(first.id, target.id, RelayRole.Profile, false))
        assertFalse(viewModel.removeProfileRelay(first.id, target.id))
        assertFalse(viewModel.setProfileRelayConnectionStatus(first.id, target.id, RelayConnectionStatus.Disconnected))
        assertEquals(second, viewModel.uiState.activeProfile)
    }

    @Test
    fun relayAddAndRestoreMarkExactlyTheirPublishedListChanges() {
        val viewModel = AppViewModel(); viewModel.completeSignIn(OnboardingOrigin.Initial)
        val id = viewModel.uiState.activeProfileId!!
        assertTrue(viewModel.addProfileRelay(id, "wss://new.example", setOf(RelayRole.Inbox, RelayRole.ChatMessages)))
        assertEquals(setOf(PublishedRelayList.Inbox), viewModel.relayPublication.projection(viewModel.uiState.activeProfile!!).missing)
        assertTrue(viewModel.restoreProfileRelays(id))
        assertEquals(setOf(PublishedRelayList.Inbox), viewModel.relayPublication.projection(viewModel.uiState.activeProfile!!).missing)
    }

    @Test
    fun erasingAppDataDropsRelayPublicationState() {
        val viewModel = AppViewModel(); viewModel.completeSignIn(OnboardingOrigin.Initial)
        val profile = viewModel.uiState.activeProfile!!; val relay = profile.settings.relays.first()
        viewModel.setProfileRelayRole(profile.id, relay.id, RelayRole.Profile, false)
        assertTrue(viewModel.relayPublication.projections.isNotEmpty())
        assertTrue(viewModel.eraseAppData(WipeConfirmationPhrase.make(viewModel.uiState.profiles.map(Profile::id))))
        assertTrue(viewModel.relayPublication.projections.isEmpty())
    }
    @Test
    fun importedRelayExampleIsDeveloperGatedAndPreservesEveryRoleUntilExplicitRemoval() {
        val viewModel = AppViewModel(); viewModel.completeSignIn(OnboardingOrigin.Initial)
        assertFalse(viewModel.loadRelayImportExample())
        viewModel.setDeveloperToolsEnabled(true); assertTrue(viewModel.loadRelayImportExample())
        val imported = viewModel.uiState.activeProfile!!.settings.relays.single { it.id == "imported-invalid" }
        assertEquals(RelayRole.entries.toSet(), imported.roles)
        assertTrue(ProfileRelayFixtures.importedAddressNeedsAttention(imported))
        assertTrue(viewModel.removeProfileRelay(viewModel.uiState.activeProfileId!!, imported.id))
    }
}
