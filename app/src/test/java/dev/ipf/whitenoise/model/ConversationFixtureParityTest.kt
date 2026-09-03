package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationFixtureParityTest {
    private val profileId = ProfileFixtures.MARMOTA_ID
    private val chats = ChatFixtures.populatedChats(profileId)

    @Test
    fun catalogTimelinesContainEveryPinnedIosScenarioInSourceOrder() {
        assertIds(
            "catalog-direct-text",
            "TXT-01", "TXT-02", "TXT-03", "TXT-04", "TXT-05", "CLUSTER-01", "CLUSTER-02",
            "TXT-06", "TXT-07", "TXT-08", "CLUSTER-03", "TXT-09", "TXT-10", "DLV-01", "DLV-02", "DLV-03",
        )
        assertIds(
            "catalog-direct-dates",
            *List(15) { "DATE-${(it + 1).toString().padStart(2, '0')}" }.toTypedArray(),
        )
        assertIds(
            "catalog-direct-replies",
            "RPL-01-source", "RPL-01", "RPL-02-source-caption", "RPL-02-source", "RPL-02",
            "DEL-02-caption", "DEL-02", "RPL-03", "DEL-01-caption", "DEL-01", "RPL-04",
        )
        assertIds(
            "catalog-direct-reactions",
            "RCT-01", "RCT-02", "RCT-03", "RCT-04", "RCT-05",
            *List(8) { "RCT-${it + 6}" }.toTypedArray(),
            "ACT-01", "ACT-02", "ACT-03-caption", "ACT-03", "ACT-04-caption", "ACT-04", "ACT-05-caption", "ACT-05",
        )
        assertIds(
            "catalog-media-single",
            "MED-01", "MED-02", "MED-03", "MED-SINGLE-04", "MED-SINGLE-05", "MED-SINGLE-06",
            "MED-11", "MED-SINGLE-08", "MED-13", "MED-12",
        )
        assertIds(
            "catalog-media-gallery",
            "MED-04", "MED-05", "MED-06", "MED-07", "MED-08", "MED-09", "MED-10", "MED-GALLERY-08",
        )
        assertIds(
            "catalog-media-viewer",
            "MED-VIEW-01", "MED-VIEW-02", "MED-VIEW-03", "MED-VIEW-04", "MED-VIEW-05", "MED-VIEW-06", "MED-VIEW-07",
        )
        assertIds(
            "catalog-media-rich",
            "FILE-01", "FILE-02", "FILE-03", "FILE-04", "FILE-05", "FILE-06",
            "LINK-01", "LINK-02", "LINK-03", "RICH-01", "RICH-05",
        )
        assertIds(
            "catalog-voice",
            "VOICE-01-caption", "VOICE-01", "VOICE-02-caption", "VOICE-02",
            "VOICE-03-caption", "VOICE-03", "VOICE-04-caption", "VOICE-04",
        )
        assertIds(
            "catalog-group-messages",
            "EVT-02", "EVT-06", "GRP-01", "GRP-02", "GRP-03", "GRP-04", "GRP-05",
            "MENTION-01", "MENTION-02", "MENTION-03", "MENTION-04",
            "GRP-RPL-01-source", "GRP-RPL-01", "GRP-RPL-02",
        )
        assertIds(
            "catalog-group-colors",
            "COLOR-EVT-01", *List(9) { "COLOR-${(it + 1).toString().padStart(2, '0')}" }.toTypedArray(),
        )
        assertIds(
            "catalog-group-events",
            "EVT-01", "EVT-03", "EVT-11", "EVT-04", "EVT-05", "EVT-07", "EVT-08", "EVT-09",
            "EVT-14", "EVT-12", "EVT-13", "EVT-12B", "EVT-15", "EVT-16", "EVT-17", "EVT-18", "EVT-19",
            "ROLE-01", "EVT-20", "EVT-21", "EVT-22", "EVT-23",
        )
    }

    @Test
    fun composerCatalogMatchesThePinnedDraftAndSourceMatrix() {
        val scenarioByChat = linkedMapOf(
            "catalog-composer-text" to "CMP-TEXT",
            "catalog-composer-multiline" to "CMP-MULTILINE",
            "catalog-composer-link" to "CMP-LINK",
            "catalog-composer-link-preview" to "CMP-LINK-PREVIEW",
            "catalog-composer-photo" to "CMP-PHOTO",
            "catalog-composer-photo-album" to "CMP-PHOTO-ALBUM",
            "catalog-composer-mixed-media" to "CMP-MIXED",
            "catalog-composer-file" to "CMP-FILE",
            "catalog-composer-gif" to "CMP-GIF",
            "catalog-composer-contact" to "CMP-CONTACT",
            "catalog-composer-reply" to "CMP-REPLY",
            "catalog-composer-mention" to "CMP-MENTION",
        )
        scenarioByChat.forEach { (chatId, scenarioId) -> assertIds(chatId, scenarioId) }

        assertEquals(
            "I pulled together the notes:\n• Confirm the time\n• Share the route\n• Bring a charger",
            chat("catalog-composer-multiline").draftText,
        )
        assertEquals("https://whitenoise.chat", chat("catalog-composer-link").suppressedDraftLinkUrl)
        assertEquals(4, chat("catalog-composer-photo-album").draftAttachments.size)
        assertEquals(
            listOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Video, MessageAttachmentKind.Photo),
            chat("catalog-composer-mixed-media").draftAttachments.map { it.kind },
        )
        assertEquals("CMP-REPLY", chat("catalog-composer-reply").draftReplyMessageId)
        assertIds("catalog-direct-new-draft", "STATE-01")
        assertEquals("STATE-01: Unsent draft", chat("catalog-direct-new-draft").draftText)
    }

    @Test
    fun repliesReactionsDeliveryAndDeletionPayloadsMatchThePinnedBranches() {
        assertEquals("RPL-01-source", message("catalog-direct-replies", "RPL-01").replyToMessageId)
        assertEquals("RPL-02-source", message("catalog-direct-replies", "RPL-02").replyToMessageId)
        assertEquals(MessageDeletionState.DeletedByOther, message("catalog-direct-replies", "DEL-02").deletionState)
        assertEquals(MessageDeletionState.DeletedByCurrentProfile, message("catalog-direct-replies", "DEL-01").deletionState)
        assertEquals("RPL-missing", message("catalog-direct-replies", "RPL-04").replyToMessageId)
        assertEquals(MessageDeliveryState.Sending, message("catalog-direct-text", "DLV-01").deliveryState)
        assertEquals(MessageDeliveryState.Failed, message("catalog-direct-text", "DLV-03").deliveryState)

        val mixed = message("catalog-direct-reactions", "RCT-05").reactions
        assertEquals(listOf("🤣", "🔥", "🦫"), mixed.map { it.emoji })
        assertEquals(listOf(profileId), mixed[0].personIds)
        assertEquals(listOf("catalog-direct-reactions", "maya-chen"), mixed[1].personIds)
        assertEquals(listOf(profileId, "catalog-direct-reactions", "nora-bennett"), mixed[2].personIds)
        assertEquals(7, message("catalog-direct-reactions", "RCT-13").reactions.size)
        assertEquals(profileId, message("catalog-direct-reactions", "ACT-05").authorId)
        assertEquals("Project Brief.pdf", message("catalog-direct-reactions", "ACT-05").attachments.single().label)
    }

    @Test
    fun mediaCatalogPreservesCountsDimensionsDurationAvailabilityAndViewerExclusions() {
        val single = chat("catalog-media-single")
        val panorama = single.message("MED-SINGLE-04").attachments.single()
        val tall = single.message("MED-SINGLE-05").attachments.single()
        val tiny = single.message("MED-SINGLE-06").attachments.single()
        assertEquals(3_000, panorama.pixelWidth)
        assertEquals(700, panorama.pixelHeight)
        assertEquals(700, tall.pixelWidth)
        assertEquals(3_000, tall.pixelHeight)
        assertEquals(96, tiny.pixelWidth)
        assertEquals(64, tiny.pixelHeight)
        assertEquals(8, single.message("MED-11").attachments.single().durationSeconds)
        assertEquals(18, single.message("MED-SINGLE-08").attachments.single().durationSeconds)
        assertFalse(single.message("MED-13").attachments.single().isAvailable)
        assertFalse(single.message("MED-12").attachments.single().isAvailable)

        assertEquals(
            listOf(2, 3, 4, 5, 6, 7, 5, 8),
            chat("catalog-media-gallery").messages().map { it.attachments.size },
        )
        assertEquals(
            listOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Video, MessageAttachmentKind.Photo, MessageAttachmentKind.Photo, MessageAttachmentKind.Photo),
            message("catalog-media-gallery", "MED-10").attachments.map { it.kind },
        )
        val viewerIds = ConversationMediaProjection.items(chat("catalog-media-viewer"), ProfileFixtures.marmota)
            .map { it.message.id }
        assertEquals(listOf("MED-VIEW-01", "MED-VIEW-02", "MED-VIEW-03", "MED-VIEW-04", "MED-VIEW-05"), viewerIds)
    }

    @Test
    fun filesLinksContactsAndVoicePayloadsAreComplete() {
        assertEquals(
            listOf("Project Brief.pdf", "Review Notes.docx", "Budget.xlsx", "Assets.zip", "Read Me.txt", "Unavailable.pdf"),
            chat("catalog-media-rich").messages().take(6).map { it.attachments.single().label },
        )
        assertFalse(message("catalog-media-rich", "FILE-06").attachments.single().isAvailable)
        assertEquals(
            listOf(15_449, 15_075, 15_814, 15_868, 15_449, 240_000),
            chat("catalog-media-rich").messages().take(6).map { it.attachments.single().fileSizeBytes },
        )
        assertEquals("Human Interface Guidelines", message("catalog-media-rich", "LINK-01").attachments.single().linkTitle)
        assertTrue(message("catalog-media-rich", "LINK-02").attachments.single().images.isEmpty())
        assertFalse(message("catalog-media-rich", "LINK-03").attachments.single().isAvailable)
        assertEquals("Contact: Avery Stone", message("catalog-media-rich", "RICH-05").attachments.single().label)
        assertEquals("avery-stone", message("catalog-media-rich", "RICH-05").attachments.single().contactPersonId)
        assertEquals(
            listOf(7, 18, 82, 12),
            listOf("VOICE-01", "VOICE-02", "VOICE-03", "VOICE-04").map {
                message("catalog-voice", it).attachments.single().durationSeconds
            },
        )
        assertEquals(VoiceMessageFixture.transcript, message("catalog-voice", "VOICE-04").text)
    }

    @Test
    fun lifecycleRoleAndIndicatorCatalogsUseExactStructuralScenarios() {
        assertIds("catalog-group-member", "EVT-02-member", "EVT-04-member", "ROLE-02")
        assertIds("catalog-group-sole-admin", "ROLE-03-created", "ROLE-03-added", "ROLE-03")
        assertIds("catalog-direct-disappearing", "IND-01")
        assertIds("catalog-direct-disappearing-muted", "IND-02")
        assertIds("catalog-group-disappearing", "IND-03")
        assertIds("catalog-direct-invitation", "STATE-09")
        assertIds("catalog-group-invitation", "STATE-10-created", "STATE-10A", "STATE-10B")
        assertIds("catalog-direct-left", "STATE-02-started", "STATE-02-message", "STATE-02")
        assertIds("catalog-group-left", "STATE-03-created", "STATE-03-added", "STATE-03-message", "STATE-03")
        assertIds("catalog-group-removed", "STATE-04-created", "STATE-04-added", "STATE-04-message", "EVT-10")
        assertIds("catalog-direct-blocked", "STATE-05-started", "STATE-05")
        assertIds("catalog-direct-missing-relays", "STATE-06-started", "STATE-06")
        assertIds("catalog-direct-archived", "STATE-07-started", "STATE-07")
        assertIds(ChatFixtures.SUPPORT_CHAT_ID, "STATE-08")

        assertEquals(GroupRole.Admin, chat("catalog-group-events").members.first { it.personId == profileId }.role)
        assertEquals(GroupRole.Member, chat("catalog-group-member").members.first { it.personId == profileId }.role)
        assertTrue(chat("catalog-group-sole-admin").isSoleAdmin(profileId))
        assertEquals(10, chat("catalog-group-colors").members.size)
    }

    @Test
    fun retainedAuthoredHistoriesAndOrdinarySeedsMatchIosStructure() {
        assertIds(
            "maya-chen",
            "maya-1", "maya-2", "maya-3", "maya-3b", "maya-3c", "maya-3d", "maya-3e", "maya-3f",
            "maya-4", "maya-5", "maya-6", "maya-7", "maya-8", "maya-9", "maya-9b", "maya-9c",
            "maya-10", "maya-10b", "maya-10c", "maya-10d", "maya-10e", "maya-11",
            "maya-12", "maya-12b", "maya-12c", "maya-12d",
            "maya-13", "maya-14", "maya-15", "maya-16", "maya-17",
        )
        assertIds(
            "weekend-walks",
            "week-event-created", "week-event-added", "week-msg-1", "week-msg-2", "week-event-added-one",
            "week-msg-3", "week-event-joined", "week-msg-4", "week-msg-5", "week-event-leo-joined",
            "week-event-name", "week-msg-6", "week-event-photo", "week-msg-7", "week-msg-8", "week-event-description",
            "week-event-admin", "week-msg-9", "week-msg-10", "week-msg-11", "week-msg-11b", "week-msg-12",
            "week-msg-13", "week-event-admin-remove", "week-event-description-remove", "week-msg-14", "week-msg-15",
            "week-msg-16", "week-msg-17", "week-msg-18", "week-msg-19", "week-msg-20", "week-msg-21",
            "week-msg-22", "week-msg-23", "week-msg-24", "week-msg-25", "week-event-left",
            "week-event-theo-added", "week-msg-26", "week-event-removed", "week-msg-27",
        )
        assertEquals(8, chat("fiatjaf").messages().size)
        assertEquals(5, message("fiatjaf", "fiatjaf-8").attachments.size)
        assertTrue(chat("mina-park").timeline.isEmpty())

        val specialized = setOf(
            "catalog-direct-text", "catalog-direct-dates", "catalog-direct-replies", "catalog-direct-reactions",
            "catalog-direct-new-draft", "catalog-media-single", "catalog-media-gallery", "catalog-media-viewer",
            "catalog-media-rich", "catalog-voice", "catalog-group-messages", "catalog-group-colors",
            "catalog-group-events", "catalog-group-member", "catalog-group-sole-admin", "catalog-direct-disappearing",
            "catalog-direct-disappearing-muted", "catalog-group-disappearing", "catalog-direct-invitation",
            "catalog-group-invitation", "catalog-direct-left", "catalog-group-left", "catalog-group-removed",
            "catalog-direct-blocked", "catalog-direct-missing-relays", "catalog-direct-archived",
            ChatFixtures.SUPPORT_CHAT_ID, "maya-chen", "weekend-walks", "fiatjaf", "mina-park",
        )
        chats.filterNot { it.id in specialized || it.id.startsWith("catalog-composer-") }.forEach { chat ->
            assertEquals("${chat.id}-seed", chat.timeline.first().id)
            if (chat.membership == ChatMembership.Left || chat.membership == ChatMembership.Removed) {
                assertEquals(2, chat.timeline.size)
            } else {
                assertEquals(1, chat.timeline.size)
            }
        }
    }

    @Test
    fun singleMediaFallbackFillsHeightAndCapsOnlyTheWidth() {
        assertEquals(SingleMediaSize(256, 256), SingleMediaLayout.size(message("catalog-media-single", "MED-SINGLE-04").attachments.single()))
        assertEquals(SingleMediaSize(60, 256), SingleMediaLayout.size(message("catalog-media-single", "MED-SINGLE-05").attachments.single()))
        assertEquals(SingleMediaSize(192, 192), SingleMediaLayout.size(message("catalog-media-single", "MED-SINGLE-06").attachments.single()))
    }

    private fun assertIds(chatId: String, vararg expected: String) {
        assertEquals(expected.toList(), chat(chatId).timeline.map { it.id })
    }

    private fun chat(id: String): Chat = chats.first { it.id == id }

    private fun message(chatId: String, messageId: String): ChatMessage = chat(chatId).message(messageId)

    private fun Chat.message(id: String): ChatMessage = messages().first { it.id == id }

    private fun Chat.messages(): List<ChatMessage> = timeline.filterIsInstance<ChatTimelineEntry.Message>()
        .map(ChatTimelineEntry.Message::message)
}
