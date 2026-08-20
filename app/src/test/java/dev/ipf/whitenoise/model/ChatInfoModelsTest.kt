package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
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
        assertEquals(6, media.size)
        assertEquals(media.size, media.map { it.id }.distinct().size)
        assertTrue(media.none { it.attachment.kind == MessageAttachmentKind.Gif })
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
