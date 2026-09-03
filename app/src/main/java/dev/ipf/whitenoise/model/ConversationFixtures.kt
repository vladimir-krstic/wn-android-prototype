package dev.ipf.whitenoise.model

/** Deterministic conversation graph ported from the pinned iOS fixture baseline. */
object ConversationFixtures {
    private const val OLDER = 0
    private const val RECENT = 1
    private const val YESTERDAY = 2
    private const val TODAY = 3

    fun timelineFor(chat: Chat, profileId: String): List<ChatTimelineEntry> = when {
        chat.id == "catalog-direct-text" -> directText(profileId)
        chat.id == "catalog-direct-dates" -> directDates(profileId)
        chat.id == "catalog-direct-replies" -> directReplies(profileId)
        chat.id == "catalog-direct-reactions" -> directReactions(profileId)
        chat.id == "catalog-direct-new-draft" -> listOf(
            event("STATE-01", "You started the chat.", TODAY, "Today", 540),
        )
        chat.id.startsWith("catalog-composer-") -> composerTimeline(chat)
        chat.id == "catalog-media-single" -> mediaSingle(profileId)
        chat.id == "catalog-media-gallery" -> mediaGalleries(profileId)
        chat.id == "catalog-media-viewer" -> mediaViewer(profileId)
        chat.id == "catalog-media-rich" -> richContent(profileId)
        chat.id == "catalog-voice" -> voiceMessages(profileId)
        chat.id == "catalog-group-messages" -> groupMessages(profileId)
        chat.id == "catalog-group-colors" -> identityColors(profileId)
        chat.id == "catalog-group-events" -> groupEvents(profileId)
        chat.id == "catalog-group-member" -> listOf(
            event("EVT-02-member", "Maya Chen created the group.", RECENT, "Jul 18", 600),
            event("EVT-04-member", "Maya Chen added you and Elias Moreno.", RECENT, "Jul 18", 601),
            message(
                "ROLE-02",
                "maya-chen",
                TODAY,
                "Today",
                660,
                "11:00 AM",
                "ROLE-02: Ordinary member: messaging, search, shared content, mute, archive, and leave remain available; admin controls are hidden.",
            ),
        )
        chat.id == "catalog-group-sole-admin" -> listOf(
            event("ROLE-03-created", "You created the group.", RECENT, "Jul 18", 600),
            event("ROLE-03-added", "You added Maya Chen and Elias Moreno.", RECENT, "Jul 18", 601),
            message(
                "ROLE-03",
                profileId,
                TODAY,
                "Today",
                660,
                "11:00 AM",
                "ROLE-03: Sole admin: promote another member before leaving the group.",
            ),
        )
        chat.id == "catalog-direct-disappearing" -> listOf(
            message("IND-01", chat.id, TODAY, "Today", 600, "10:00 AM", "IND-01: 1 Day disappearing messages"),
        )
        chat.id == "catalog-direct-disappearing-muted" -> listOf(
            message("IND-02", chat.id, TODAY, "Today", 600, "10:00 AM", "IND-02: 1 Week disappearing messages and muted"),
        )
        chat.id == "catalog-group-disappearing" -> listOf(
            message("IND-03", "maya-chen", TODAY, "Today", 600, "10:00 AM", "IND-03: 4 Weeks disappearing messages"),
        )
        chat.id == "catalog-direct-invitation" -> listOf(
            message("STATE-09", "avery-stone", TODAY, "Today", 570, "9:30 AM", "STATE-09: Are you free for a quick call tomorrow?"),
        )
        chat.id == "catalog-group-invitation" -> listOf(
            event("STATE-10-created", "Maya Chen created the group.", YESTERDAY, "Yesterday", 600),
            message("STATE-10A", "maya-chen", YESTERDAY, "Yesterday", 602, "10:02 AM", "STATE-10A: We’re meeting at the west trailhead at 9."),
            message("STATE-10B", "elias-moreno", YESTERDAY, "Yesterday", 604, "10:04 AM", "STATE-10B: Bring water and a light jacket."),
        )
        chat.id == "catalog-direct-left" -> listOf(
            event("STATE-02-started", "You started the chat.", YESTERDAY, "Yesterday", 600),
            message("STATE-02-message", chat.id, YESTERDAY, "Yesterday", 602, "10:02 AM", "STATE-02: Direct history remains readable after leaving."),
            event("STATE-02", "You left the chat.", YESTERDAY, "Yesterday", 604),
        )
        chat.id == "catalog-group-left" -> listOf(
            event("STATE-03-created", "Maya Chen created the group.", YESTERDAY, "Yesterday", 600),
            event("STATE-03-added", "Maya Chen added you and Elias Moreno.", YESTERDAY, "Yesterday", 601),
            message("STATE-03-message", "maya-chen", YESTERDAY, "Yesterday", 603, "10:03 AM", "STATE-03: Group history remains readable after leaving."),
            event("STATE-03", "You left the group.", YESTERDAY, "Yesterday", 605),
        )
        chat.id == "catalog-group-removed" -> listOf(
            event("STATE-04-created", "Maya Chen created the group.", YESTERDAY, "Yesterday", 600),
            event("STATE-04-added", "Maya Chen added you and Elias Moreno.", YESTERDAY, "Yesterday", 601),
            message("STATE-04-message", "maya-chen", YESTERDAY, "Yesterday", 603, "10:03 AM", "STATE-04: Group history remains readable after removal."),
            event("EVT-10", "Maya Chen removed you from the group.", YESTERDAY, "Yesterday", 605),
        )
        chat.id == "catalog-direct-blocked" -> recoveryTimeline(profileId, chat.id, "STATE-05", "STATE-05: History remains available while blocked")
        chat.id == "catalog-direct-missing-relays" -> recoveryTimeline(profileId, chat.id, "STATE-06", "STATE-06: History remains available without chat relays")
        chat.id == "catalog-direct-archived" -> recoveryTimeline(profileId, chat.id, "STATE-07", "STATE-07: Active archived chat")
        chat.id == "maya-chen" -> maya(profileId)
        chat.id == "weekend-walks" -> weekend(profileId)
        chat.id == "fiatjaf" -> fiatjaf(profileId)
        chat.id == ChatFixtures.SUPPORT_CHAT_ID -> listOf(
            notice("STATE-08", "How can we help? Ask a question, report a problem, or share a suggestion. We’ll reply here."),
        )
        else -> defaultTimeline(chat, profileId)
    }

    private fun directText(profileId: String) = listOf(
        message("TXT-01", "catalog-direct-text", 0, "Jul 28, 2025", 540, "9:00 AM", "TXT-01: Incoming short text"),
        message("TXT-02", profileId, 1, "Mon, Jul 14", 540, "9:00 AM", "TXT-02: Outgoing short text"),
        message("TXT-03", "catalog-direct-text", 2, "Thursday", 600, "10:00 AM", "TXT-03: Cluster start"),
        message("TXT-04", "catalog-direct-text", 2, "Thursday", 601, "10:01 AM", "TXT-04: Cluster middle"),
        message("TXT-05", "catalog-direct-text", 2, "Thursday", 602, "10:02 AM", "TXT-05: Cluster end"),
        message("CLUSTER-01", profileId, 2, "Thursday", 603, "10:03 AM", "CLUSTER-01: Author change starts a new cluster"),
        message("CLUSTER-02", profileId, 2, "Thursday", 610, "10:10 AM", "CLUSTER-02: More than five minutes starts a new cluster"),
        message("TXT-06", "catalog-direct-text", 3, "Yesterday", 660, "11:00 AM", "TXT-06: Multiline text\nSecond line\nThird line"),
        message(
            "TXT-07",
            profileId,
            3,
            "Yesterday",
            662,
            "11:02 AM",
            "TXT-07: Long wrapping text demonstrates how a message bubble grows across several lines while preserving readable padding and alignment at both edges of the conversation.",
        ),
        message("TXT-08", "catalog-direct-text", 3, "Yesterday", 663, "11:03 AM", "TXT-08: 👋🏽🎉"),
        message("CLUSTER-03", "catalog-direct-text", 4, "Today", 480, "8:00 AM", "CLUSTER-03: A new day starts a new cluster"),
        message("TXT-09", profileId, 4, "Today", 481, "8:01 AM", "TXT-09: **Bold**, *emphasis*, and [White Noise](https://whitenoise.chat)"),
        message("TXT-10", "catalog-direct-text", 4, "Today", 482, "8:02 AM", "TXT-10: https://developer.apple.com/design/human-interface-guidelines"),
        message("DLV-01", profileId, 4, "Today", 483, "8:03 AM", "DLV-01: Sending outgoing message", delivery = MessageDeliveryState.Sending),
        message("DLV-02", profileId, 4, "Today", 484, "8:04 AM", "DLV-02: Sent outgoing message"),
        message("DLV-03", profileId, 4, "Today", 490, "8:10 AM", "DLV-03: Failed outgoing message", delivery = MessageDeliveryState.Failed),
    )

    private fun directDates(profileId: String) = listOf(
        message("DATE-01", "catalog-direct-dates", 0, "Dec 8, 2025", 540, "9:00 AM", "DATE-01: Old date includes its year"),
        message("DATE-02", profileId, 1, "Mon, Jul 14", 540, "9:00 AM", "DATE-02: Recent date uses weekday, month, and day"),
        message("DATE-03", "catalog-direct-dates", 2, "Wednesday", 600, "10:00 AM", "DATE-03: First sparse one-message day"),
        message("DATE-04", profileId, 3, "Thursday", 600, "10:00 AM", "DATE-04: Second sparse one-message day"),
        message("DATE-05", "catalog-direct-dates", 4, "Friday", 600, "10:00 AM", "DATE-05: Third sparse one-message day"),
        message("DATE-06", profileId, 5, "Saturday", 600, "10:00 AM", "DATE-06: Fourth sparse one-message day"),
        message("DATE-07", "catalog-direct-dates", 6, "Yesterday", 600, "10:00 AM", "DATE-07: Yesterday remains visible inline"),
        message("DATE-08", profileId, 7, "Today", 480, "8:00 AM", "DATE-08: Today begins a long section"),
        message("DATE-09", "catalog-direct-dates", 7, "Today", 481, "8:01 AM", "DATE-09: The inline Today header can scroll away"),
        message("DATE-10", profileId, 7, "Today", 482, "8:02 AM", "DATE-10: Its pinned pill remains above the transcript"),
        message("DATE-11", "catalog-direct-dates", 7, "Today", 483, "8:03 AM", "DATE-11: More messages make this day span the viewport"),
        message("DATE-12", profileId, 7, "Today", 484, "8:04 AM", "DATE-12: Scrolling preserves the current day context"),
        message("DATE-13", "catalog-direct-dates", 7, "Today", 485, "8:05 AM", "DATE-13: Long sections keep their day context"),
        message("DATE-14", profileId, 7, "Today", 486, "8:06 AM", "DATE-14: Date headers remain visible in the transcript"),
        message("DATE-15", "catalog-direct-dates", 7, "Today", 487, "8:07 AM", "DATE-15: Long day keeps its date pinned"),
    )

    private fun directReplies(profileId: String) = listOf(
        message("RPL-01-source", "catalog-direct-replies", YESTERDAY, "Yesterday", 600, "10:00 AM", "RPL-01 source: Incoming text"),
        message("RPL-01", profileId, YESTERDAY, "Yesterday", 601, "10:01 AM", "RPL-01: Outgoing reply to incoming text", reply = "RPL-01-source"),
        message("RPL-02-source-caption", "catalog-direct-replies", YESTERDAY, "Yesterday", 604, "10:04 AM", "RPL-02-source → next bubble: Outgoing attachment reply target"),
        message("RPL-02-source", profileId, YESTERDAY, "Yesterday", 605, "10:05 AM", attachments = listOf(photo("RPL-02-photo", "Fox beside a tree", AvatarAsset.Fox))),
        message("RPL-02", "catalog-direct-replies", YESTERDAY, "Yesterday", 606, "10:06 AM", "RPL-02: Incoming reply to outgoing attachment", reply = "RPL-02-source"),
        message("DEL-02-caption", profileId, TODAY, "Today", 600, "10:00 AM", "DEL-02 → next bubble: Incoming deletion and deleted reply target"),
        message("DEL-02", "catalog-direct-replies", TODAY, "Today", 601, "10:01 AM", deletion = MessageDeletionState.DeletedByOther),
        message("RPL-03", profileId, TODAY, "Today", 602, "10:02 AM", "RPL-03: Reply to deleted target", reply = "DEL-02"),
        message("DEL-01-caption", "catalog-direct-replies", TODAY, "Today", 605, "10:05 AM", "DEL-01 → next bubble: Outgoing deletion"),
        message("DEL-01", profileId, TODAY, "Today", 606, "10:06 AM", deletion = MessageDeletionState.DeletedByCurrentProfile),
        message("RPL-04", "catalog-direct-replies", TODAY, "Today", 610, "10:10 AM", "RPL-04: Missing reply target", reply = "RPL-missing"),
    )

    private fun directReactions(profileId: String): List<ChatTimelineEntry> {
        val otherId = "catalog-direct-reactions"
        val emoji = listOf("❤️", "😀", "👍", "👎", "🤣", "🔥", "🦫")
        val result = mutableListOf<ChatTimelineEntry>(
            message("RCT-01", otherId, YESTERDAY, "Yesterday", 560, "9:20 AM", "RCT-01: Single reaction from another person", reactions = listOf(MessageReaction("❤️", listOf(otherId)))),
            message("RCT-02", profileId, YESTERDAY, "Yesterday", 561, "9:21 AM", "RCT-02: You reacted", reactions = listOf(MessageReaction("😀", listOf(profileId)))),
            message("RCT-03", otherId, YESTERDAY, "Yesterday", 562, "9:22 AM", "RCT-03: Repeated reaction from three others", reactions = listOf(MessageReaction("👍", listOf(otherId, "maya-chen", "nora-bennett")))),
            message("RCT-04", profileId, YESTERDAY, "Yesterday", 563, "9:23 AM", "RCT-04: Repeated reaction including you", reactions = listOf(MessageReaction("👎", listOf(profileId, otherId, "maya-chen")))),
            message(
                "RCT-05",
                otherId,
                YESTERDAY,
                "Yesterday",
                564,
                "9:24 AM",
                "RCT-05: Mixed reaction types and participation",
                reactions = listOf(
                    MessageReaction("🤣", listOf(profileId)),
                    MessageReaction("🔥", listOf(otherId, "maya-chen")),
                    MessageReaction("🦫", listOf(profileId, otherId, "nora-bennett")),
                ),
            ),
        )
        emoji.forEachIndexed { index, value ->
            result += message(
                id = "RCT-${index + 6}",
                authorId = if (index % 2 == 0) otherId else profileId,
                day = if (index < 4) YESTERDAY else TODAY,
                dayLabel = if (index < 4) "Yesterday" else "Today",
                minute = 570 + index,
                time = "${9 + (570 + index) / 60}:${((570 + index) % 60).toString().padStart(2, '0')} AM",
                text = "RCT-${index + 6}: Supported reaction $value",
                reactions = listOf(MessageReaction(value, listOf(otherId))),
            )
        }
        result += message(
            "RCT-13",
            profileId,
            TODAY,
            "Today",
            612,
            "10:12 AM",
            "RCT-13: Overflow summary",
            reactions = emoji.mapIndexed { index, value ->
                MessageReaction(value, if (index % 2 == 0) listOf(otherId) else listOf(profileId, otherId))
            },
        )
        result += listOf(
            message("ACT-01", otherId, TODAY, "Today", 620, "10:20 AM", "ACT-01: Incoming text: React, Reply, Copy, Share"),
            message("ACT-02", profileId, TODAY, "Today", 624, "10:24 AM", "ACT-02: Outgoing text: React, Reply, Copy, Share, Delete"),
            message("ACT-03-caption", profileId, TODAY, "Today", 626, "10:26 AM", "ACT-03 → next bubble: Incoming attachment-only: no Copy or Delete"),
            message("ACT-03", otherId, TODAY, "Today", 628, "10:28 AM", attachments = listOf(photo("ACT-photo-incoming", "Badger in grass", AvatarAsset.Badger))),
            message("ACT-04-caption", otherId, TODAY, "Today", 630, "10:30 AM", "ACT-04 → next bubble: Outgoing attachment-only: Delete, no Copy"),
            message("ACT-04", profileId, TODAY, "Today", 632, "10:32 AM", attachments = listOf(photo("ACT-photo-outgoing", "Badger in grass", AvatarAsset.Badger))),
            message("ACT-05-caption", otherId, TODAY, "Today", 634, "10:34 AM", "ACT-05 → next bubble: Available file shares its file URL"),
            message("ACT-05", profileId, TODAY, "Today", 636, "10:36 AM", attachments = listOf(file("ACT-05-file", "Project Brief.pdf"))),
        )
        return result
    }

    private fun composerTimeline(chat: Chat): List<ChatTimelineEntry> {
        val scenarioId = when (chat.id) {
            "catalog-composer-text" -> "CMP-TEXT"
            "catalog-composer-multiline" -> "CMP-MULTILINE"
            "catalog-composer-link" -> "CMP-LINK"
            "catalog-composer-link-preview" -> "CMP-LINK-PREVIEW"
            "catalog-composer-photo" -> "CMP-PHOTO"
            "catalog-composer-photo-album" -> "CMP-PHOTO-ALBUM"
            "catalog-composer-mixed-media" -> "CMP-MIXED"
            "catalog-composer-file" -> "CMP-FILE"
            "catalog-composer-gif" -> "CMP-GIF"
            "catalog-composer-contact" -> "CMP-CONTACT"
            "catalog-composer-reply" -> "CMP-REPLY"
            "catalog-composer-mention" -> "CMP-MENTION"
            else -> error("Unknown composer fixture ${chat.id}")
        }
        val text = if (chat.id == "catalog-composer-reply") {
            "CMP-REPLY: Would Thursday afternoon work?"
        } else {
            "$scenarioId: Composer state is ready below"
        }
        return listOf(
            message(
                scenarioId,
                if (chat.id == "catalog-composer-mention") "maya-chen" else chat.id,
                TODAY,
                "Today",
                600,
                "10:00 AM",
                text,
            ),
        )
    }

    private fun mediaSingle(profileId: String) = listOf(
        message("MED-01", "catalog-media-single", YESTERDAY, "Yesterday", 600, "10:00 AM", "MED-01: Incoming landscape", attachments = listOf(photo("MED-01-photo", "Fox in landscape", AvatarAsset.Fox, 1_200, 800))),
        message("MED-02", profileId, YESTERDAY, "Yesterday", 603, "10:03 AM", "MED-02: Outgoing square", attachments = listOf(photo("MED-02-photo", "Square portrait", AvatarAsset.GardenClub, 1_200, 1_200))),
        message("MED-03", "catalog-media-single", YESTERDAY, "Yesterday", 606, "10:06 AM", "MED-03: Captioned portrait", attachments = listOf(photo("MED-03-photo", "Portrait photograph", AvatarAsset.LegacySatoshiNakamoto, 800, 1_200))),
        message("MED-SINGLE-04", profileId, YESTERDAY, "Yesterday", 609, "10:09 AM", "MED-SINGLE-04: Panorama ratio clamped", attachments = listOf(photo("MED-SINGLE-04-photo", "Clamped panorama", AvatarAsset.Ostrich, 3_000, 700))),
        message("MED-SINGLE-05", "catalog-media-single", TODAY, "Today", 600, "10:00 AM", "MED-SINGLE-05: Tall ratio clamped", attachments = listOf(photo("MED-SINGLE-05-photo", "Clamped tall photograph", AvatarAsset.WebAionyHaust, 700, 3_000))),
        message("MED-SINGLE-06", profileId, TODAY, "Today", 603, "10:03 AM", "MED-SINGLE-06: Low-resolution safeguard", attachments = listOf(photo("MED-SINGLE-06-photo", "Low resolution photograph", AvatarAsset.LegacyMarmots, 96, 64))),
        message("MED-11", "catalog-media-single", TODAY, "Today", 606, "10:06 AM", "MED-11: Landscape video with duration", attachments = listOf(video("MED-11-video-landscape", "Landscape video", AvatarAsset.GardenClub, 8, 1_920, 1_080))),
        message("MED-SINGLE-08", profileId, TODAY, "Today", 609, "10:09 AM", "MED-SINGLE-08: Portrait video", attachments = listOf(video("MED-SINGLE-08-video-portrait", "Portrait video", AvatarAsset.LegacySatoshiNakamoto, 18, 1_080, 1_920))),
        message("MED-13", "catalog-media-single", TODAY, "Today", 612, "10:12 AM", "MED-13: Unavailable photo", attachments = listOf(unavailablePhoto("MED-13-photo", "Unavailable photo", 1_200, 800))),
        message("MED-12", profileId, TODAY, "Today", 615, "10:15 AM", "MED-12: Unavailable video", attachments = listOf(video("MED-12-video", "Unavailable video", AvatarAsset.WebChristopherCampbell, 42, 1_080, 1_920, available = false))),
    )

    private data class GallerySpec(
        val asset: AvatarAsset,
        val label: String,
        val width: Int,
        val height: Int,
    )

    private val gallerySpecs = listOf(
        GallerySpec(AvatarAsset.WebAionyHaust, "Portrait in soft daylight", 800, 1_200),
        GallerySpec(AvatarAsset.Fox, "Fox in landscape", 1_200, 800),
        GallerySpec(AvatarAsset.WebAyoOgunseinde, "Square portrait", 1_000, 1_000),
        GallerySpec(AvatarAsset.Ostrich, "Ostrich in landscape", 1_400, 800),
        GallerySpec(AvatarAsset.LegacySatoshiNakamoto, "Tall portrait", 700, 1_200),
        GallerySpec(AvatarAsset.Badger, "Badger in landscape", 1_200, 800),
        GallerySpec(AvatarAsset.WebPhilipMartin, "Portrait in bright light", 900, 1_100),
        GallerySpec(AvatarAsset.MayaChen, "Square portrait outdoors", 1_000, 1_000),
    )

    private fun galleryPhotos(prefix: String, count: Int): List<MessageAttachment> =
        gallerySpecs.take(count).mapIndexed { index, spec ->
            photo("$prefix-${index + 1}", spec.label, spec.asset, spec.width, spec.height)
        }

    private fun mediaGalleries(profileId: String): List<ChatTimelineEntry> {
        val otherId = "catalog-media-gallery"
        val mixedPhotos = galleryPhotos("MED-10-photo", 5)
        return listOf(
            message("MED-04", profileId, YESTERDAY, "Yesterday", 600, "10:00 AM", "MED-04: Gallery of 2", attachments = galleryPhotos("MED-04-photo", 2)),
            message("MED-05", otherId, YESTERDAY, "Yesterday", 603, "10:03 AM", "MED-05: Gallery of 3", attachments = galleryPhotos("MED-05-photo", 3)),
            message("MED-06", profileId, YESTERDAY, "Yesterday", 606, "10:06 AM", "MED-06: Gallery of 4", attachments = galleryPhotos("MED-06-photo", 4)),
            message("MED-07", otherId, YESTERDAY, "Yesterday", 609, "10:09 AM", "MED-07: Captioned gallery of 5", attachments = galleryPhotos("MED-07-photo", 5)),
            message("MED-08", profileId, TODAY, "Today", 600, "10:00 AM", "MED-08: Gallery of 6 with +1", attachments = galleryPhotos("MED-08-photo", 6)),
            message("MED-09", otherId, TODAY, "Today", 603, "10:03 AM", "MED-09: Gallery of 7 with +2", attachments = galleryPhotos("MED-09-photo", 7)),
            message(
                "MED-10",
                profileId,
                TODAY,
                "Today",
                606,
                "10:06 AM",
                "MED-10: Mixed photo and video album",
                attachments = listOf(
                    mixedPhotos[0],
                    video("MED-gallery-video", "Gallery video", AvatarAsset.GardenClub, 8, 1_920, 1_080),
                    mixedPhotos[2],
                    mixedPhotos[3],
                    mixedPhotos[4],
                ),
            ),
            message("MED-GALLERY-08", otherId, TODAY, "Today", 609, "10:09 AM", "MED-GALLERY-08: Larger overflow +3", attachments = galleryPhotos("MED-GALLERY-08-photo", 8)),
        )
    }

    private fun mediaViewer(profileId: String) = listOf(
        message("MED-VIEW-01", "catalog-media-viewer", OLDER, "Friday", 540, "9:00 AM", "MED-VIEW-01: Paging starts across dates", attachments = listOf(photo("MED-VIEW-01-photo", "Fox in grass", AvatarAsset.Fox, 1_200, 800))),
        message("MED-VIEW-02", profileId, OLDER, "Friday", 542, "9:02 AM", "MED-VIEW-02: Zoom and Share", attachments = listOf(photo("MED-VIEW-02-photo", "Ostrich portrait", AvatarAsset.Ostrich, 1_200, 800))),
        message("MED-VIEW-03", "catalog-media-viewer", YESTERDAY, "Yesterday", 840, "2:00 PM", "MED-VIEW-03: Save and Forward", attachments = listOf(photo("MED-VIEW-03-photo", "Badger in grass", AvatarAsset.Badger, 1_200, 800))),
        message("MED-VIEW-04", profileId, YESTERDAY, "Yesterday", 842, "2:02 PM", "MED-VIEW-04: Initial video autoplays", attachments = listOf(video("MED-VIEW-04-video", "Trail video", AvatarAsset.GardenClub, 8, 1_920, 1_080))),
        message("MED-VIEW-05", "catalog-media-viewer", TODAY, "Today", 600, "10:00 AM", "MED-VIEW-05: Go to Message", attachments = listOf(photo("MED-VIEW-05-photo", "Marmot in landscape", AvatarAsset.Marmot, 1_200, 800))),
        message("MED-VIEW-06", profileId, TODAY, "Today", 602, "10:02 AM", "MED-VIEW-06: Deleted media excluded", attachments = listOf(photo("MED-VIEW-06-photo", "Deleted portrait", AvatarAsset.MayaChen, 1_200, 800)), deletion = MessageDeletionState.DeletedByCurrentProfile),
        message("MED-VIEW-07", "catalog-media-viewer", TODAY, "Today", 604, "10:04 AM", "MED-VIEW-07: Unavailable media excluded", attachments = listOf(unavailablePhoto("MED-VIEW-07-photo", "Unavailable media", 1_200, 800))),
    )

    private fun richContent(profileId: String) = listOf(
        message("FILE-01", "catalog-media-rich", OLDER, "Friday", 600, "10:00 AM", "FILE-01: Available PDF", attachments = listOf(file("FILE-01-file", "Project Brief.pdf"))),
        message("FILE-02", profileId, OLDER, "Friday", 602, "10:02 AM", "FILE-02: Available DOCX", attachments = listOf(file("FILE-02-file", "Review Notes.docx"))),
        message("FILE-03", "catalog-media-rich", OLDER, "Friday", 604, "10:04 AM", "FILE-03: Available XLSX", attachments = listOf(file("FILE-03-file", "Budget.xlsx"))),
        message("FILE-04", profileId, OLDER, "Friday", 606, "10:06 AM", "FILE-04: Available ZIP", attachments = listOf(file("FILE-04-file", "Assets.zip"))),
        message("FILE-05", "catalog-media-rich", YESTERDAY, "Yesterday", 600, "10:00 AM", "FILE-05: Available TXT", attachments = listOf(file("FILE-05-file", "Read Me.txt"))),
        message("FILE-06", profileId, YESTERDAY, "Yesterday", 602, "10:02 AM", "FILE-06: Unavailable file", attachments = listOf(file("FILE-06-file", "Unavailable.pdf", available = false))),
        message(
            "LINK-01",
            "catalog-media-rich",
            YESTERDAY,
            "Yesterday",
            605,
            "10:05 AM",
            "LINK-01: Link preview with image",
            attachments = listOf(
                link(
                    "LINK-01-link",
                    "https://developer.apple.com/design/human-interface-guidelines",
                    "Human Interface Guidelines",
                    "developer.apple.com",
                    "Guidance for designing clear experiences on Apple platforms.",
                    AvatarAsset.OpenCircuit,
                ),
            ),
        ),
        message(
            "LINK-02",
            profileId,
            YESTERDAY,
            "Yesterday",
            607,
            "10:07 AM",
            "LINK-02: Link preview without image",
            attachments = listOf(link("LINK-02-link", "https://whitenoise.chat", "White Noise", "whitenoise.chat", "Private, resilient conversations.")),
        ),
        message(
            "LINK-03",
            "catalog-media-rich",
            TODAY,
            "Today",
            600,
            "10:00 AM",
            "LINK-03: Invalid destination",
            attachments = listOf(link("LINK-03-link", null, "Unavailable preview", "", "This destination cannot be opened.", available = false)),
        ),
        message("RICH-01", profileId, TODAY, "Today", 603, "10:03 AM", "RICH-01: GIF", attachments = listOf(gif("RICH-01-gif", "Marmot looking around", AvatarAsset.Marmot))),
        message("RICH-05", "catalog-media-rich", TODAY, "Today", 612, "10:12 AM", "RICH-05: Valid contact", attachments = listOf(contact("RICH-05-contact", "Avery Stone", AvatarAsset.WebChristopherCampbell))),
    )

    private fun voiceMessages(profileId: String) = listOf(
        message("VOICE-01-caption", profileId, YESTERDAY, "Yesterday", 600, "10:00 AM", "VOICE-01 → next bubble: Incoming short voice message"),
        message("VOICE-01", "catalog-voice", YESTERDAY, "Yesterday", 601, "10:01 AM", attachments = listOf(voice("VOICE-01-audio", 7))),
        message("VOICE-02-caption", "catalog-voice", YESTERDAY, "Yesterday", 603, "10:03 AM", "VOICE-02 → next bubble: Outgoing short voice message"),
        message("VOICE-02", profileId, YESTERDAY, "Yesterday", 604, "10:04 AM", attachments = listOf(voice("VOICE-02-audio", 18))),
        message("VOICE-03-caption", profileId, TODAY, "Today", 600, "10:00 AM", "VOICE-03 → next bubble: Voice duration over one minute"),
        message("VOICE-03", "catalog-voice", TODAY, "Today", 601, "10:01 AM", attachments = listOf(voice("VOICE-03-audio", 82))),
        message("VOICE-04-caption", "catalog-voice", TODAY, "Today", 603, "10:03 AM", "VOICE-04 → next bubble: Outgoing voice and text message"),
        message("VOICE-04", profileId, TODAY, "Today", 604, "10:04 AM", VoiceMessageFixture.transcript, attachments = listOf(voice("VOICE-04-audio", 12))),
    )

    private fun groupMessages(profileId: String) = listOf(
        event("EVT-02", "Maya Chen created the group.", OLDER, "Jul 22", 540),
        event("EVT-06", "You joined the group.", OLDER, "Jul 22", 541),
        message("GRP-01", "maya-chen", OLDER, "Jul 22", 543, "9:03 AM", "GRP-01: Incoming group cluster start"),
        message("GRP-02", "maya-chen", OLDER, "Jul 22", 544, "9:04 AM", "GRP-02: Same-author cluster end"),
        message("GRP-03", "elias-moreno", OLDER, "Jul 22", 545, "9:05 AM", "GRP-03: Author switch"),
        message("GRP-04", profileId, OLDER, "Jul 22", 546, "9:06 AM", "GRP-04: Outgoing interruption"),
        message("GRP-05", "elias-moreno", OLDER, "Jul 22", 552, "9:12 AM", "GRP-05: Five-minute cluster break"),
        message("MENTION-01", "maya-chen", YESTERDAY, "Yesterday", 600, "10:00 AM", "MENTION-01: @Marmota please review this."),
        message("MENTION-02", profileId, YESTERDAY, "Yesterday", 602, "10:02 AM", "MENTION-02: @Maya Chen has the latest version."),
        message("MENTION-03", "nora-bennett", YESTERDAY, "Yesterday", 604, "10:04 AM", "MENTION-03: @Maya Chen and @Elias Moreno can compare notes."),
        message("MENTION-04", "elias-moreno", YESTERDAY, "Yesterday", 606, "10:06 AM", "MENTION-04: @Unknown stays plain text."),
        message("GRP-RPL-01-source", "maya-chen", TODAY, "Today", 540, "9:00 AM", "GRP-RPL-01 source: Maya’s question"),
        message("GRP-RPL-01", "elias-moreno", TODAY, "Today", 541, "9:01 AM", "GRP-RPL-01: Elias replies to Maya", reply = "GRP-RPL-01-source"),
        message("GRP-RPL-02", "maya-chen", TODAY, "Today", 542, "9:02 AM", "GRP-RPL-02: Maya replies to Elias", reply = "GRP-RPL-01"),
    )

    private fun identityColors(profileId: String): List<ChatTimelineEntry> {
        val people = listOf(
            "identity-color-1" to "COLOR-01: Red identity color",
            "identity-color-7" to "COLOR-02: Orange identity color",
            "identity-color-13" to "COLOR-03: Green identity color",
            "identity-color-12" to "COLOR-04: Teal identity color",
            "identity-color-0" to "COLOR-05: Blue identity color",
            "identity-color-2" to "COLOR-06: Indigo identity color",
            "identity-color-11" to "COLOR-07: Purple identity color",
            "identity-color-5" to "COLOR-08: Pink identity color",
            "identity-color-22" to "COLOR-09: Brown identity color",
        )
        return listOf(event("COLOR-EVT-01", "You created the group.", TODAY, "Today", 540)) +
            people.mapIndexed { index, (personId, text) ->
                message(
                    "COLOR-${(index + 1).toString().padStart(2, '0')}",
                    personId,
                    TODAY,
                    "Today",
                    542 + index,
                    "9:${(index + 2).toString().padStart(2, '0')} AM",
                    text,
                )
            }
    }

    private fun groupEvents(profileId: String) = listOf(
        event("EVT-01", "You created the group.", RECENT, "Jul 18", 600),
        event("EVT-03", "You added Maya Chen.", RECENT, "Jul 18", 601),
        event("EVT-11", "You made Maya Chen an admin.", RECENT, "Jul 18", 602),
        event("EVT-04", "Maya Chen added Elias Moreno, Nora Bennett, Leo Martins and Mina Park.", RECENT, "Jul 18", 603),
        event("EVT-05", "Theo Grant joined the group.", RECENT, "Jul 18", 604),
        event("EVT-07", "Leo Martins left the group.", RECENT, "Jul 18", 605),
        event("EVT-08", "You removed Nora Bennett.", RECENT, "Jul 18", 606),
        event("EVT-09", "Maya Chen removed Theo Grant.", RECENT, "Jul 18", 607),
        event("EVT-14", "Maya Chen removed you as an admin.", YESTERDAY, "Yesterday", 600),
        event("EVT-12", "Maya Chen made you an admin.", YESTERDAY, "Yesterday", 601),
        event("EVT-13", "You removed Maya Chen as an admin.", YESTERDAY, "Yesterday", 602),
        event("EVT-12B", "You made Elias Moreno an admin.", YESTERDAY, "Yesterday", 603),
        event("EVT-15", "You changed the group name to Group - Events & Roles.", YESTERDAY, "Yesterday", 604),
        event("EVT-16", "Elias Moreno changed the group photo.", YESTERDAY, "Yesterday", 605),
        event("EVT-17", "You removed the group photo.", YESTERDAY, "Yesterday", 606),
        event("EVT-18", "Elias Moreno changed the group description.", YESTERDAY, "Yesterday", 607),
        event("EVT-19", "You removed the group description.", YESTERDAY, "Yesterday", 608),
        message("ROLE-01", profileId, YESTERDAY, "Yesterday", 609, "10:09 AM", "ROLE-01: Admin: edit group identity, add people, manage roles, remove members, and leave."),
        event("EVT-20", "You set disappearing messages to 1 Day.", TODAY, "Today", 600),
        event("EVT-21", "Elias Moreno set disappearing messages to 1 Week.", TODAY, "Today", 601),
        event("EVT-22", "You set disappearing messages to 4 Weeks.", TODAY, "Today", 602),
        event("EVT-23", "Elias Moreno turned off disappearing messages.", TODAY, "Today", 603),
    )

    private fun recoveryTimeline(
        profileId: String,
        otherId: String,
        scenarioId: String,
        label: String,
    ) = listOf(
        event("$scenarioId-started", "You started the chat.", TODAY, "Today", 600),
        message(scenarioId, otherId, TODAY, "Today", 602, "10:02 AM", label),
    )

    private fun maya(profileId: String): List<ChatTimelineEntry> {
        val photo = photo("maya-photo-one", "Portrait in soft daylight", AvatarAsset.WebAionyHaust)
        val video = video("maya-video", "Short clip", AvatarAsset.GardenClub, 8)
        return listOf(
            message("maya-1", "maya-chen", 0, "Jun 2", 540, "9:00 AM", "Hi—Maya here."),
            message("maya-2", profileId, 0, "Jun 2", 541, "9:01 AM", "Hi Maya. Good to meet you."),
            message("maya-3", "maya-chen", 0, "Jun 2", 543, "9:03 AM", "I pulled the first draft together.\nCould you look at the opening and the final paragraph?"),
            message("maya-3b", profileId, 0, "Jun 2", 544, "9:04 AM", "Yes. I’ll read both before lunch.", reply = "maya-3"),
            message("maya-3c", profileId, 0, "Jun 2", 545, "9:05 AM", "I’ll keep the opening intact and tighten the middle.\nThe ending can be direct enough to read quickly without losing the context that makes the recommendation useful."),
            message("maya-3d", "maya-chen", 0, "Jun 2", 547, "9:07 AM", "That sounds right. The full background matters, but the main idea should still be obvious to someone who only has a minute to read it."),
            message("maya-3e", "maya-chen", 0, "Jun 2", 548, "9:08 AM", "I marked the sentence I’d keep."),
            message("maya-3f", profileId, 0, "Jun 2", 549, "9:09 AM", "I agree. **The shorter version works better.** I’m using this as the reference: https://whitenoise.chat"),
            message("maya-4", profileId, 1, "Jul 16", 600, "10:00 AM", "This is the latest photo.", attachments = listOf(photo), reactions = listOf(MessageReaction("❤️", listOf("maya-chen")))),
            message("maya-5", "maya-chen", 1, "Jul 16", 602, "10:02 AM", "The crop looks great.", reply = "maya-4"),
            message("maya-6", "maya-chen", 1, "Jul 16", 604, "10:04 AM", attachments = listOf(photo.copy(id = "maya-photo-repeat"))),
            message(
                "maya-7",
                profileId,
                1,
                "Jul 16",
                606,
                "10:06 AM",
                "These two could sit together.",
                attachments = listOf(
                    photo("maya-two-a", "Portrait against a dark background", AvatarAsset.WebAyoOgunseinde),
                    photo("maya-two-b", "Green leaves in sunlight", AvatarAsset.GardenClub),
                ),
            ),
            message(
                "maya-8",
                "maya-chen",
                1,
                "Jul 16",
                608,
                "10:08 AM",
                "And these tell the full sequence.",
                attachments = listOf(
                    photo("maya-three-a", "Portrait outdoors", AvatarAsset.WebIanDooley),
                    photo("maya-three-b", "Portrait in warm light", AvatarAsset.WebSergioDePaula),
                    photo("maya-three-c", "Portrait near a window", AvatarAsset.WebVinceFleming),
                ),
            ),
            message("maya-9", "maya-chen", 1, "Jul 16", 610, "10:10 AM", "Here’s the short clip.", attachments = listOf(video)),
            message("maya-9b", profileId, 1, "Jul 16", 612, "10:12 AM", "That movement makes the sequence much clearer.", reply = "maya-9"),
            message("maya-9c", profileId, 1, "Jul 16", 614, "10:14 AM", "This pairing is probably the strongest.", attachments = listOf(photo.copy(id = "maya-pair-photo"), video.copy(id = "maya-pair-video"))),
            message("maya-10", "maya-chen", 2, "Yesterday", 600, "10:00 AM", "Here are the notes from our last pass.", attachments = listOf(file("maya-file", "Weekend Notes.pdf"))),
            message("maya-10b", profileId, 2, "Yesterday", 602, "10:02 AM", "I added the editable outline.", attachments = listOf(file("maya-docx", "Conversation Outline.docx"))),
            message("maya-10c", "maya-chen", 2, "Yesterday", 603, "10:03 AM", "Here’s the planning sheet.", attachments = listOf(file("maya-xlsx", "Launch Checklist.xlsx"))),
            message("maya-10d", profileId, 2, "Yesterday", 605, "10:05 AM", "These are the supporting files.", attachments = listOf(file("maya-zip", "Reference Images.zip"))),
            message("maya-10e", "maya-chen", 2, "Yesterday", 606, "10:06 AM", attachments = listOf(file("maya-txt", "Review Notes.txt"))),
            message("maya-11", profileId, 2, "Yesterday", 608, "10:08 AM", attachments = listOf(voice("maya-voice", VoiceMessageFixture.durationSeconds))),
            message("maya-12", "maya-chen", 2, "Yesterday", 609, "10:09 AM", "This is the reference I mentioned.", attachments = listOf(link("maya-link", "https://whitenoise.chat", "White Noise", "whitenoise.chat", "Private, resilient messaging for people and groups.", AvatarAsset.Marmota))),
            message("maya-12b", profileId, 2, "Yesterday", 611, "10:11 AM", "Apple’s design guidance is useful here.", attachments = listOf(link("maya-link-apple", "https://developer.apple.com/design/human-interface-guidelines", "Human Interface Guidelines", "developer.apple.com", "Guidance for designing clear, consistent experiences across Apple platforms.", AvatarAsset.OpenCircuit))),
            message("maya-12c", "maya-chen", 2, "Yesterday", 612, "10:12 AM", "I also saved the messaging reference.", attachments = listOf(link("maya-link-signal", "https://support.signal.org", "Signal Support", "support.signal.org", "Help and guidance for private messaging features.", AvatarAsset.FreeSignal))),
            message("maya-12d", profileId, 2, "Yesterday", 614, "10:14 AM", attachments = listOf(link("maya-link-github", "https://github.com", "GitHub", "github.com", "Code, issues, and project collaboration in one place.", AvatarAsset.OpenQuill))),
            message("maya-13", "maya-chen", 3, "Today", 540, "9:00 AM", deletion = MessageDeletionState.DeletedByOther),
            message("maya-14", profileId, 3, "Today", 542, "9:02 AM", deletion = MessageDeletionState.DeletedByCurrentProfile),
            message("maya-15", profileId, 3, "Today", 544, "9:04 AM", "Replying to a message that’s no longer available.", reply = "maya-13"),
            message("maya-16", profileId, 3, "Today", 546, "9:06 AM", "I’ll send the revised version now.", delivery = MessageDeliveryState.Failed),
            message(
                "maya-17",
                "maya-chen",
                3,
                "Today",
                548,
                "9:08 AM",
                "Can you send the latest version when you have a moment?",
                reactions = listOf(
                    MessageReaction("👍", listOf(profileId)),
                    MessageReaction("😀", listOf(profileId, "maya-chen")),
                ),
            ),
        )
    }

    private fun weekend(profileId: String): List<ChatTimelineEntry> {
        val gallery = listOf(
            photo("week-1", "Green leaves in sunlight", AvatarAsset.GardenClub),
            photo("week-2", "Portrait in soft daylight", AvatarAsset.WebAionyHaust),
            photo("week-3", "Portrait against a dark background", AvatarAsset.WebAyoOgunseinde),
            photo("week-4", "Portrait outdoors", AvatarAsset.WebIanDooley),
            photo("week-5", "Portrait in warm light", AvatarAsset.WebSergioDePaula),
            photo("week-6", "Portrait near a window", AvatarAsset.WebVinceFleming),
            photo("week-7", "Portrait with a bright background", AvatarAsset.WebPhilipMartin),
        )
        return listOf(
            event("week-event-created", "You created the group.", 0, "Jun 2025", 540),
            event("week-event-added", "You added Maya Chen and Elias Moreno.", 0, "Jun 2025", 541),
            message("week-msg-1", "maya-chen", 0, "Jun 2025", 543, "9:03 AM", "Thanks for setting this up."),
            message("week-msg-2", "elias-moreno", 0, "Jun 2025", 544, "9:04 AM", "I have a few easy routes we can try."),
            event("week-event-added-one", "You added Nora Bennett.", 0, "Jun 2025", 546),
            message("week-msg-3", "nora-bennett", 0, "Jun 2025", 548, "9:08 AM", "Welcome everyone. Let’s choose a route that works for the whole group."),
            event("week-event-joined", "Mina Park joined the group.", 0, "Jun 2025", 550),
            message("week-msg-4", "mina-park", 0, "Jun 2025", 552, "9:12 AM", "Glad I found the group."),
            message("week-msg-5", "mina-park", 0, "Jun 2025", 553, "9:13 AM", "Sunday mornings usually work for me."),
            event("week-event-leo-joined", "Leo Martins joined the group.", 0, "Jun 2025", 555),
            event("week-event-name", "You changed the group name to Weekend Walks.", 1, "May 3", 540),
            message("week-msg-6", "nora-bennett", 1, "May 3", 542, "9:02 AM", "Weekend Walks fits us better."),
            event("week-event-photo", "You changed the group photo.", 1, "May 3", 544),
            message("week-msg-7", "maya-chen", 1, "May 3", 546, "9:06 AM", "That photo is from our first riverside route."),
            message("week-msg-8", "maya-chen", 1, "May 3", 547, "9:07 AM", "I still like that path best."),
            event("week-event-description", "You changed the group description.", 1, "May 3", 549),
            event("week-event-admin", "You made Maya Chen an admin.", 2, "Thursday", 540),
            message("week-msg-9", "maya-chen", 2, "Thursday", 542, "9:02 AM", "I’ll organize the route options and meeting points."),
            message("week-msg-10", "elias-moreno", 2, "Thursday", 544, "9:04 AM", "Here are four from the west trail.", attachments = gallery.take(4), reactions = listOf(MessageReaction("👍", listOf(profileId)))),
            message("week-msg-11", "mina-park", 2, "Thursday", 546, "9:06 AM", "And five from the lake loop.", attachments = gallery.take(5)),
            message("week-msg-11b", "maya-chen", 2, "Thursday", 547, "9:07 AM", "These six cover the trail from start to finish.", attachments = gallery.take(6)),
            message(
                "week-msg-12",
                "nora-bennett",
                2,
                "Thursday",
                548,
                "9:08 AM",
                "A few views from last time.",
                attachments = gallery,
                reactions = listOf(
                    MessageReaction("❤️", listOf(profileId, "maya-chen", "elias-moreno")),
                    MessageReaction("🔥", listOf("mina-park")),
                ),
            ),
            message("week-msg-13", "maya-chen", 2, "Thursday", 550, "9:10 AM", "I’m done with the route changes, so you can take admin back."),
            event("week-event-admin-remove", "You removed Maya Chen as an admin.", 2, "Thursday", 552),
            event("week-event-description-remove", "You removed the group description.", 3, "Yesterday", 540),
            message("week-msg-14", "maya-chen", 3, "Yesterday", 540, "9:00 AM", "@Marmota, does the riverside path work?"),
            message("week-msg-15", profileId, 3, "Yesterday", 542, "9:02 AM", "Yes, and the forecast looks clear.", reply = "week-msg-14"),
            message("week-msg-16", "nora-bennett", 3, "Yesterday", 544, "9:04 AM", "Then let’s keep the changing details here instead of in the description.", reply = "week-msg-15"),
            message("week-msg-17", "maya-chen", 3, "Yesterday", 546, "9:06 AM", "This clip shows the narrow section.", attachments = listOf(video("week-video", "Narrow trail clip", AvatarAsset.Pebble, 8))),
            message("week-msg-18", profileId, 3, "Yesterday", 548, "9:08 AM", "And here’s the bridge beside it.", attachments = listOf(gallery[0], video("week-mixed-video", "Bridge video", AvatarAsset.Pebble, 8)), reply = "week-msg-17"),
            message("week-msg-19", "leo-martins", 4, "Today", 480, "8:00 AM", attachments = listOf(gif("week-gif", "Marmot looking around", AvatarAsset.Marmot)), reactions = listOf(MessageReaction("🤣", listOf(profileId, "maya-chen")))),
            message("week-msg-20", profileId, 4, "Today", 482, "8:02 AM", attachments = listOf(gallery[5]), reactions = listOf(MessageReaction("🦫", listOf("elias-moreno")))),
            message("week-msg-21", "maya-chen", 4, "Today", 484, "8:04 AM", "Not the steep shortcut—the entrance with the sunlit trees.", attachments = listOf(gallery[0]), reactions = listOf(MessageReaction("👎", listOf(profileId, "nora-bennett")))),
            message("week-msg-22", "elias-moreno", 4, "Today", 486, "8:06 AM", attachments = listOf(contact("week-contact", "Avery Stone", AvatarAsset.WebChristopherCampbell))),
            message("week-msg-23", "nora-bennett", 4, "Today", 488, "8:08 AM", attachments = listOf(file("week-file", "Trail Plan.pdf"))),
            message("week-msg-24", profileId, 4, "Today", 490, "8:10 AM", attachments = listOf(voice("week-voice", VoiceMessageFixture.durationSeconds))),
            message("week-msg-25", "leo-martins", 4, "Today", 492, "8:12 AM", "I’m stepping out, but I hope the walk goes well."),
            event("week-event-left", "Leo Martins left the group.", 4, "Today", 494),
            event("week-event-theo-added", "You added Theo Grant.", 4, "Today", 495),
            message("week-msg-26", "theo-grant", 4, "Today", 496, "8:16 AM", "I won’t be joining this one."),
            event("week-event-removed", "You removed Theo Grant.", 4, "Today", 498),
            message("week-msg-27", "nora-bennett", 4, "Today", 500, "8:20 AM", "Saturday morning works for me."),
        )
    }

    private fun fiatjaf(profileId: String) = listOf(
        message("fiatjaf-1", profileId, TODAY, "Today", 540, "9:00 AM", "I’m moving from Feather to White Noise."),
        message("fiatjaf-2", "fiatjaf", TODAY, "Today", 541, "9:01 AM", "Let me know how it goes."),
        message("fiatjaf-3", profileId, TODAY, "Today", 542, "9:02 AM", "Signing in now.\nI’ll send a test next."),
        message("fiatjaf-4", profileId, TODAY, "Today", 545, "9:05 AM", "Switched from Feather to White Noise. Same key, same contacts."),
        message("fiatjaf-5", "fiatjaf", TODAY, "Today", 547, "9:07 AM", "Yep, I still see you on Primal. No extra setup on my side.", reply = "fiatjaf-4"),
        message("fiatjaf-6", profileId, TODAY, "Today", 549, "9:09 AM", "Exactly. Moved apps, kept everything. Didn’t have to re-add anyone.", reactions = listOf(MessageReaction("🔥", listOf("fiatjaf")))),
        message("fiatjaf-7", "fiatjaf", TODAY, "Today", 551, "9:11 AM", "Perfect!"),
        message(
            "fiatjaf-8",
            "fiatjaf",
            TODAY,
            "Today",
            553,
            "9:13 AM",
            "Portable identity for the win.",
            attachments = listOf(
                photo("fiatjaf-photo-1", "Marmot", AvatarAsset.Marmot),
                photo("fiatjaf-photo-2", "Badger", AvatarAsset.Badger),
                photo("fiatjaf-photo-3", "Fox", AvatarAsset.Fox),
                photo("fiatjaf-photo-4", "Sloth", AvatarAsset.Sloth),
                photo("fiatjaf-photo-5", "Ostrich", AvatarAsset.Ostrich),
            ),
        ),
    )

    private fun defaultTimeline(chat: Chat, profileId: String): List<ChatTimelineEntry> {
        if (chat.isDraft) return emptyList()
        val authorId = when {
            chat.previewAuthor == "You" -> profileId
            chat.isGroup -> when (chat.previewAuthor) {
                "Tim" -> "tim"
                "Jude" -> "jude"
                "Sam" -> "sam"
                "Owen" -> "owen"
                "Remy" -> "remy"
                "Maya" -> "maya-chen"
                "Nora" -> "nora-bennett"
                "Iris" -> "iris"
                "Noah" -> "noah"
                else -> "maya-chen"
            }
            else -> (chat.kind as? ChatKind.Direct)?.personId ?: chat.id
        }
        val seed = message(
            id = "${chat.id}-seed",
            authorId = authorId,
            day = TODAY,
            dayLabel = defaultDayLabel(chat.timestamp),
            minute = 600,
            time = "10:00 AM",
            text = chat.preview,
            attachments = seedAttachments(chat),
            delivery = if (chat.deliveryState == ChatDeliveryState.Failed) {
                MessageDeliveryState.Failed
            } else {
                MessageDeliveryState.Sent
            },
        )
        return when {
            !chat.isGroup || chat.membership == ChatMembership.Active || chat.membership == ChatMembership.Invited -> listOf(seed)
            chat.membership == ChatMembership.Left -> listOf(
                seed,
                event("${chat.id}-membership-left", "You left the group.", TODAY, defaultDayLabel(chat.timestamp), 601),
            )
            else -> listOf(
                seed,
                event("${chat.id}-membership-removed", "Maya Chen removed you from the group.", TODAY, defaultDayLabel(chat.timestamp), 601),
            )
        }
    }

    private fun seedAttachments(chat: Chat): List<MessageAttachment> = when (val preview = chat.attachmentPreview) {
        null -> emptyList()
        AttachmentPreview.Photo -> listOf(photo("${chat.id}-photo", "Portrait in soft daylight", AvatarAsset.WebAionyHaust))
        is AttachmentPreview.Photos -> {
            val assets = listOf(
                AvatarAsset.WebAionyHaust,
                AvatarAsset.GardenClub,
                AvatarAsset.WebAyoOgunseinde,
                AvatarAsset.WebIanDooley,
                AvatarAsset.WebSergioDePaula,
                AvatarAsset.WebVinceFleming,
                AvatarAsset.WebPhilipMartin,
            )
            List(preview.count) { index ->
                photo("${chat.id}-photo-$index", "Photo ${index + 1}", assets[index % assets.size])
            }
        }
        AttachmentPreview.Video -> listOf(video("${chat.id}-video", "Video", AvatarAsset.GardenClub, 8))
        AttachmentPreview.VoiceMessage -> listOf(voice("${chat.id}-voice", VoiceMessageFixture.durationSeconds))
        is AttachmentPreview.File -> listOf(file("${chat.id}-file", preview.name))
        is AttachmentPreview.Contact -> listOf(contact("${chat.id}-contact", preview.name, AvatarAsset.WebChristopherCampbell))
        AttachmentPreview.Link -> listOf(link("${chat.id}-link", "https://whitenoise.chat", "Reading for later", "whitenoise.chat", "A useful link shared with the chat."))
        AttachmentPreview.Gif -> listOf(gif("${chat.id}-gif", "Marmot", AvatarAsset.Marmot))
    }

    private fun defaultDayLabel(timestamp: String): String = when {
        timestamp == "Yesterday" -> "Yesterday"
        timestamp in setOf("Now", "2m", "9m", "1h", "8h") -> "Today"
        else -> timestamp
    }

    private fun message(
        id: String,
        authorId: String,
        day: Int,
        dayLabel: String,
        minute: Int,
        time: String,
        text: String = "",
        attachments: List<MessageAttachment> = emptyList(),
        reply: String? = null,
        reactions: List<MessageReaction> = emptyList(),
        delivery: MessageDeliveryState = MessageDeliveryState.Sent,
        deletion: MessageDeletionState = MessageDeletionState.None,
    ) = ChatTimelineEntry.Message(
        ChatMessage(
            id = id,
            authorId = authorId,
            dayOrdinal = day,
            dayLabel = dayLabel,
            minuteOfDay = minute,
            timeLabel = time,
            text = text,
            attachments = attachments,
            replyToMessageId = reply,
            reactions = reactions,
            deliveryState = delivery,
            deletionState = deletion,
        ),
    )

    private fun event(id: String, text: String, day: Int, dayLabel: String, minute: Int) =
        ChatTimelineEntry.Event(id, text, day, dayLabel, minute)

    private fun notice(id: String, text: String) = ChatTimelineEntry.Notice(id, text, dayLabel = "")

    private fun photo(
        id: String,
        label: String,
        asset: AvatarAsset,
        width: Int? = null,
        height: Int? = null,
    ) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Photo,
        label = label,
        images = listOf(ProfileAvatar.Asset(asset)),
        pixelWidth = width,
        pixelHeight = height,
    )

    private fun unavailablePhoto(id: String, label: String, width: Int, height: Int) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Photo,
        label = label,
        pixelWidth = width,
        pixelHeight = height,
        isAvailable = false,
    )

    private fun video(
        id: String,
        label: String,
        asset: AvatarAsset,
        duration: Int,
        width: Int? = null,
        height: Int? = null,
        available: Boolean = true,
    ) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Video,
        label = label,
        images = listOf(ProfileAvatar.Asset(asset)),
        durationSeconds = duration,
        pixelWidth = width,
        pixelHeight = height,
        isAvailable = available,
    )

    private fun file(
        id: String,
        label: String,
        available: Boolean = true,
        sizeBytes: Int = bundledFileSize(label),
    ) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.File,
        label = label,
        fileSizeBytes = if (available) sizeBytes else 240_000,
        isAvailable = available,
    )

    private fun bundledFileSize(label: String): Int = when (label) {
        "Project Brief.pdf", "Conversation Outline.docx", "Read Me.txt" -> 15_449
        "Review Notes.docx", "Launch Checklist.xlsx" -> 15_075
        "Budget.xlsx", "Weekend Notes.pdf", "Review Notes.txt" -> 15_814
        "Assets.zip", "Reference Images.zip", "Trail Plan.pdf" -> 15_868
        else -> 15_449
    }

    private fun link(
        id: String,
        uri: String?,
        title: String,
        domain: String,
        summary: String,
        image: AvatarAsset? = null,
        available: Boolean = true,
    ) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Link,
        label = "Link",
        images = image?.let { listOf(ProfileAvatar.Asset(it)) }.orEmpty(),
        externalUri = uri,
        linkTitle = title,
        linkDomain = domain,
        linkSummary = summary,
        isAvailable = available,
    )

    private fun gif(id: String, label: String, asset: AvatarAsset) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Gif,
        label = label,
        images = listOf(ProfileAvatar.Asset(asset)),
    )

    private fun contact(id: String, name: String, asset: AvatarAsset) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Contact,
        label = "Contact: $name",
        images = listOf(ProfileAvatar.Asset(asset)),
        contactPersonId = name.lowercase().replace(' ', '-'),
    )

    private fun voice(id: String, duration: Int) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Voice,
        label = "Voice message",
        durationSeconds = duration,
        voiceFormat = VoiceMessageFormat.Voice,
    )
}
