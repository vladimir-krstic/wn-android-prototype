package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class GlobalAttachmentsTest {
    private val all = GlobalSearchFilters(content = setOf(GlobalSearchContent.AnyAttachment))
    private fun attachment(id: String = "file", kind: MessageAttachmentKind = MessageAttachmentKind.File, name: String = "Plan.pdf") =
        MessageAttachment(id, kind, name)
    private fun message(id: String = "message", vararg attachments: MessageAttachment) = ChatMessage(id, "friend", 3, "Today", 600, "10:00", "A plan", attachments.toList())
    private fun chat(id: String, vararg messages: ChatMessage) = Chat(id, 0, ChatKind.Direct("friend"), "Chat $id", timeline = messages.map(ChatTimelineEntry::Message))
    private fun profile(vararg chats: Chat) = Profile("me", "Me", "key", chats = chats.toList(), people = listOf(Person("friend", "Friend")))

    @Test fun galleriesExpandWithoutMergingEqualNamesOrSourceIdsAcrossChats() {
        val photo = attachment("gallery", MessageAttachmentKind.Photos, "Photos").copy(images = listOf(ProfileAvatar.Monogram, ProfileAvatar.Monogram))
        val message = message("same", photo, attachment(), attachment("other", name = "Plan.pdf"))
        val owner = profile(chat("a", message), chat("b", message))
        val rows = GlobalAttachments.results(owner, "", all)
        assertEquals(8, rows.size)
        assertEquals(8, rows.map { it.id }.distinct().size)
        assertEquals(4, rows.mapNotNull { it.media }.map { it.key }.distinct().size)
        assertEquals(listOf(0, 1), rows.filter { it.chatId == "a" && it.attachment.id == "gallery" }.map { it.imageIndex })
    }

    @Test fun typeFiltersExpandOnlyEligibleAttachmentsInsideMixedMessages() {
        val owner = profile(chat("a", message("m", attachment(), attachment("audio", name = "Voice.bin").copy(mimeType = "audio/ogg"),
            attachment("photo", MessageAttachmentKind.Photo).copy(images = listOf(ProfileAvatar.Monogram)),
            attachment("link", MessageAttachmentKind.Link).copy(externalUri = "https://example.com"))))
        val rows = GlobalAttachments.results(owner, "", all)
        assertEquals(3, rows.size)
        assertEquals(setOf("file", "audio"), GlobalAttachments.results(owner, "", all.copy(content = setOf(GlobalSearchContent.Files, GlobalSearchContent.VoiceAudio))).map { it.attachment.id }.toSet())
        assertEquals("audio", GlobalAttachments.results(owner, "", all.copy(content = setOf(GlobalSearchContent.VoiceAudio))).single().attachment.id)
        assertEquals(1, GlobalSearch.results(owner, "", all.copy(content = setOf(GlobalSearchContent.Links))).messages.size)
        assertFalse(GlobalAttachments.browsing(all.copy(content = setOf(GlobalSearchContent.Links))))
    }

    @Test fun uriTypedAudioUsesTheSameClassificationAsSearchAndTheChatLibrary() {
        val audio = attachment("audio", name = "Recording").copy(externalUri = "content://local/recording.opus")
        val owner = profile(chat("a", message("m", audio)))
        assertTrue(SharedContentProjection.isAudio(audio))
        assertEquals("audio", GlobalAttachments.results(owner, "", all.copy(content = setOf(GlobalSearchContent.VoiceAudio))).single().attachment.id)
        assertTrue(GlobalAttachments.results(owner, "", all.copy(content = setOf(GlobalSearchContent.Files))).isEmpty())
    }

    @Test fun querySenderChatTypeAndDatesReuseSearchIntersection() {
        val today = message("today", attachment()).copy(authorId = "me")
        val yesterday = message("yesterday", attachment()).copy(dayLabel = "Yesterday", dayOrdinal = 2)
        val owner = profile(chat("a", today, yesterday), chat("b", today.copy(id = "other", text = "Different", attachments = listOf(attachment(name = "Other.pdf")))))
        val filters = all.copy(senderIds = setOf("me"), chatIds = setOf("a", "b"), chatTypes = setOf(GlobalSearchChatType.Direct), date = GlobalSearchDate.Today)
        assertEquals("today", GlobalAttachments.results(owner, "Plan.pdf", filters).single().message.id)
        assertTrue(GlobalAttachments.results(owner, "", filters.copy(chatTypes = setOf(GlobalSearchChatType.Groups))).isEmpty())
        assertTrue(GlobalAttachments.results(owner, "", all.copy(date = GlobalSearchDate.Custom, fromDay = 10, toDay = 1)).isEmpty())
    }

    @Test fun blankQueryIncludesUnnamedAttachmentsAndDoesNotInventOrdinaryLinks() {
        val owner = profile(chat("a", message("unnamed", attachment(name = "")).copy(text = ""), message("url").copy(text = "https://example.com")))
        assertEquals("unnamed", GlobalAttachments.results(owner, "", all).single().message.id)
    }

    @Test fun folderUnionIntersectsChatSelectionAndEmptyFoldersDoNotWidenResults() {
        val owner = profile(chat("a", message("a", attachment())), chat("b", message("b", attachment())), chat("c", message("c", attachment())))
            .copy(chatFolders = listOf(ChatFolder("one", "One", chatIds = setOf("a", "b")), ChatFolder("two", "Two", chatIds = setOf("b", "c")), ChatFolder("empty", "Empty")))
        val filters = all.copy(folderIds = setOf("one", "two"), chatIds = setOf("b"))
        assertEquals(listOf("b"), GlobalAttachments.results(owner, "", filters).map { it.chatId })
        assertTrue(GlobalAttachments.results(owner, "", all.copy(folderIds = setOf("empty"))).isEmpty())
    }

    @Test fun deletedAndExpiredSourcesAreRemovedWhileUnavailableItemsCannotOpen() {
        val source = message("m", attachment())
        val owner = profile(chat("a", source.copy(id = "deleted", deletionState = MessageDeletionState.DeletedByOther),
            source.copy(id = "expired", expiresAtMillis = MessageForwarding.nowMillis),
            source.copy(id = "unavailable", attachments = listOf(attachment().copy(isAvailable = false)))))
        val rows = GlobalAttachments.results(owner, "", all)
        assertEquals("unavailable", rows.single().message.id)
        assertFalse(rows.single().available)
        assertNull(rows.single().media)
    }

    @Test fun liveDeletionInvalidatesPreviouslyMatchedMessagesAndProfilesNeverMerge() {
        val owner = profile(chat("a", message("m", attachment())))
        val matches = GlobalSearch.results(owner, "", all)
        assertTrue(GlobalAttachments.items(owner.copy(chats = emptyList()), matches, all).isEmpty())
        val other = owner.copy(id = "other", chats = listOf(chat("b", message("m", attachment()))))
        assertTrue(GlobalAttachments.results(owner, "", all).map { it.id }.intersect(GlobalAttachments.results(other, "", all).map { it.id }.toSet()).isEmpty())
        assertTrue(GlobalAttachments.results(other, "", all).all { it.profileId == "other" && it.chatId == "b" })
    }

    @Test fun stableNewestOrderSurvivesReorderedChatsAndEqualTimestampMessages() {
        val owner = profile(chat("b", message("z", attachment()), message("a", attachment())),
            chat("a", message("old", attachment()).copy(dayLabel = "Yesterday", dayOrdinal = 2)))
        val original = GlobalAttachments.results(owner, "", all)
        val reversed = owner.copy(chats = owner.chats.reversed().map { it.copy(timeline = it.timeline.reversed()) })
        assertEquals(original.map { it.id }, GlobalAttachments.results(reversed, "", all).map { it.id })
        assertEquals(listOf("a", "z", "old"), original.map { it.message.id })
    }

    @Test fun mediaKeysStayStableButLivePayloadChangesAndByteLossInvalidateViewerItems() {
        val photo = attachment("photo", MessageAttachmentKind.Photo).copy(images = listOf(ProfileAvatar.Monogram))
        val owner = profile(chat("a", message("m", photo)))
        val original = GlobalAttachments.results(owner, "", all).single()
        val changed = profile(chat("a", message("m", photo.copy(label = "New caption"))))
        assertEquals(original.id, GlobalAttachments.results(changed, "", all).single().id)
        assertNotEquals(original.media, GlobalAttachments.results(changed, "", all).single().media)
        assertNull(GlobalAttachments.results(profile(chat("a", message("m", photo.copy(isAvailable = false)))), "", all).single().media)
    }
}
