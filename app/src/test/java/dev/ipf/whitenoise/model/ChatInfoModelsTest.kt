package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatInfoModelsTest {
    @Test
    fun sharedContentDerivesOnlyNondeletedCategoryItemsWithStableMessageIdentity() {
        val profile = ProfileFixtures.marmota
        val rich = profile.chats.first { it.id == "catalog-media-rich" }
        val gallery = profile.chats.first { it.id == "catalog-media-gallery" }

        assertEquals(listOf("RICH-02"), SharedContentProjection.items(rich, profile, SharedContentCategory.Links).map { it.messageId })
        assertEquals(listOf("RICH-01"), SharedContentProjection.items(rich, profile, SharedContentCategory.Documents).map { it.messageId })
        val media = SharedContentProjection.items(gallery, profile, SharedContentCategory.Media)
        assertEquals(27, media.size)
        assertEquals(media.size, media.map { it.id }.distinct().size)
        assertTrue(media.none { it.attachment.kind == MessageAttachmentKind.Gif })
    }

    @Test
    fun conversationMediaFlattensChronologicallyWithExactAlbumFrameIdentity() {
        val profile = ProfileFixtures.marmota
        val source = profile.chats.first { it.id == "catalog-media-gallery" }
        val album = MessageAttachment(
            id = "album",
            kind = MessageAttachmentKind.Photos,
            label = "Weekend album",
            images = listOf(
                ProfileAvatar.Asset(AvatarAsset.Marmot),
                ProfileAvatar.Asset(AvatarAsset.Badger),
            ),
        )
        val photo = MessageAttachment(
            id = "photo",
            kind = MessageAttachmentKind.Photo,
            label = "Last photo",
            images = listOf(ProfileAvatar.Asset(AvatarAsset.Fox)),
        )
        val unavailable = MessageAttachment("missing", MessageAttachmentKind.Photo, "Missing")
        val gif = MessageAttachment(
            id = "gif",
            kind = MessageAttachmentKind.Gif,
            label = "GIF",
            images = listOf(ProfileAvatar.Asset(AvatarAsset.Sloth)),
        )
        val chat = source.copy(
            timeline = listOf(
                ChatTimelineEntry.Message(
                    ChatMessage(
                        id = "first",
                        authorId = profile.id,
                        dayOrdinal = 1,
                        dayLabel = "Yesterday",
                        minuteOfDay = 10,
                        timeLabel = "9:10 AM",
                        attachments = listOf(album, gif),
                    ),
                ),
                ChatTimelineEntry.Message(
                    ChatMessage(
                        id = "deleted",
                        authorId = profile.id,
                        dayOrdinal = 1,
                        dayLabel = "Yesterday",
                        minuteOfDay = 11,
                        timeLabel = "9:11 AM",
                        attachments = listOf(photo),
                        deletionState = MessageDeletionState.DeletedByCurrentProfile,
                    ),
                ),
                ChatTimelineEntry.Message(
                    ChatMessage(
                        id = "second",
                        authorId = profile.people.first().id,
                        dayOrdinal = 2,
                        dayLabel = "Today",
                        minuteOfDay = 12,
                        timeLabel = "9:12 AM",
                        attachments = listOf(unavailable, photo),
                    ),
                ),
            ),
        )

        val media = ConversationMediaProjection.items(chat, profile)

        assertEquals(
            listOf(
                ConversationMediaKey("first", "album", 0),
                ConversationMediaKey("first", "album", 1),
                ConversationMediaKey("second", "photo", 0),
            ),
            media.map { it.key },
        )
        assertEquals("You", media.first().senderName)
        assertEquals("Today, 9:12 AM", media.last().sentLabel)
        assertEquals(media.map { it.key.stableId }, media.map { it.key.stableId }.distinct())
        assertNotEquals(
            ConversationMediaKey("a-b", "c", 0).stableId,
            ConversationMediaKey("a", "b-c", 0).stableId,
        )
        assertEquals(1, ConversationMediaProjection.selection(chat, profile, media[1].key)?.initialIndex)
    }

    @Test
    fun mediaFileMetadataUsesConcreteMimeAndSanitizedSuggestedName() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-single" }
        val media = ConversationMediaProjection.items(chat, profile)

        assertTrue(media.isNotEmpty())
        assertTrue(media.none { it.key.messageId == "MED-12" })
        assertTrue(media.any { it.mimeType == "image/jpeg" && it.suggestedFileName.endsWith(".jpg") })
        assertTrue(media.any { it.mimeType == "video/mp4" && it.suggestedFileName.endsWith(".mp4") })
        assertTrue(media.all { '/' !in it.suggestedFileName })
    }

    @Test
    fun relayPolicyNormalizesValidWssAndRejectsUnsafeOrDuplicateValues() {
        assertEquals("wss://relay.example.com/path", ChatRelayPolicy.normalize(" WSS://Relay.Example.com/path/ "))
        assertNull(ChatRelayPolicy.normalize("https://relay.example.com"))
        assertNull(ChatRelayPolicy.normalize("wss://user@relay.example.com"))
        assertEquals(
            listOf("wss://relay.one", "wss://relay.two"),
            ChatRelayPolicy.add(listOf("wss://relay.one"), "wss://RELAY.TWO/")!!,
        )
        assertNull(ChatRelayPolicy.add(listOf("wss://relay.one"), "wss://relay.one/"))
    }

    @Test
    fun groupsInCommonUsesOnlyActiveSharedMembership() {
        val profile = ProfileFixtures.marmota
        val groups = profile.groupsInCommon("maya-chen")

        assertTrue(groups.isNotEmpty())
        assertTrue(groups.all { it.membership == ChatMembership.Active && it.members.any { member -> member.personId == profile.id } })
        assertTrue(groups.none { it.id == "catalog-group-left" || it.id == "catalog-group-removed" })
    }
}
