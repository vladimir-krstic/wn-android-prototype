package dev.ipf.whitenoise.model

object ConversationFixtures {
    private const val OLDER = 0
    private const val RECENT = 1
    private const val YESTERDAY = 2
    private const val TODAY = 3

    fun timelineFor(chat: Chat, profileId: String): List<ChatTimelineEntry> = when (chat.id) {
        "catalog-direct-text" -> directText(profileId)
        "catalog-direct-dates" -> directDates(profileId)
        "catalog-direct-replies" -> directReplies(profileId)
        "catalog-direct-reactions" -> directReactions(profileId)
        "catalog-direct-new-draft" -> listOf(
            event("STATE-00", "You started the chat.", TODAY, "Today", 540),
            message("STATE-00-message", chat.id, TODAY, "Today", 545, "9:05 AM", "STATE-00: Existing history before the draft"),
        )
        "catalog-composer-reply" -> listOf(
            message(
                "CMP-REPLY-source",
                chat.id,
                TODAY,
                "Today",
                580,
                "9:40 AM",
                "CMP-REPLY: Would Thursday afternoon work?",
            ),
        )
        "catalog-media-single" -> mediaSingle(profileId)
        "catalog-media-gallery" -> mediaGalleries(profileId)
        "catalog-media-viewer" -> mediaViewer(profileId)
        "catalog-media-rich" -> richContent(profileId)
        "catalog-voice" -> voiceMessages(profileId)
        "catalog-group-messages" -> groupMessages(profileId)
        "catalog-group-colors" -> identityColors()
        "catalog-group-events" -> groupEvents(profileId)
        "catalog-group-member" -> listOf(
            event("EVT-02-member", "Maya Chen created the group.", RECENT, "Jul 18", 600),
            event("EVT-04-member", "Maya Chen added you and Elias Moreno.", RECENT, "Jul 18", 601),
            message("ROLE-02", "maya-chen", TODAY, "Today", 660, "11:00 AM", chat.preview),
        )
        "catalog-group-sole-admin" -> listOf(
            event("ROLE-03-created", "You created the group.", RECENT, "Jul 18", 600),
            event("ROLE-03-added", "You added Maya Chen and Elias Moreno.", RECENT, "Jul 18", 601),
            message("ROLE-03", profileId, TODAY, "Today", 660, "11:00 AM", chat.preview),
        )
        "catalog-direct-invitation" -> listOf(
            message("STATE-09", "avery-stone", TODAY, "Today", 570, "9:30 AM", "STATE-09: Are you free for a quick call tomorrow?"),
        )
        "catalog-group-invitation" -> listOf(
            event("STATE-10-created", "Maya Chen created the group.", YESTERDAY, "Yesterday", 600),
            message("STATE-10A", "maya-chen", YESTERDAY, "Yesterday", 610, "10:10 AM", "STATE-10A: We’re planning a short trail walk."),
            message("STATE-10B", "maya-chen", TODAY, "Today", 570, "9:30 AM", "STATE-10B: Bring water and a light jacket."),
        )
        "catalog-direct-left" -> listOf(
            message("STATE-02-message", chat.id, YESTERDAY, "Yesterday", 600, "10:00 AM", "STATE-02: Direct history remains readable after leaving."),
            event("STATE-02-left", "You left this chat.", TODAY, "Today", 540),
        )
        "catalog-group-left" -> listOf(
            event("STATE-03-created", "Maya Chen created the group.", RECENT, "Jul 18", 600),
            message("STATE-03-message", "maya-chen", YESTERDAY, "Yesterday", 620, "10:20 AM", "STATE-03: Group history remains readable after leaving."),
            event("STATE-03-left", "You left this group.", TODAY, "Today", 540),
        )
        "catalog-group-removed" -> listOf(
            event("STATE-04-created", "Maya Chen created the group.", RECENT, "Jul 18", 600),
            message("STATE-04-message", "maya-chen", YESTERDAY, "Yesterday", 620, "10:20 AM", "STATE-04: Group history remains readable after removal."),
            event("EVT-10", "Maya Chen removed you from the group.", TODAY, "Today", 540),
        )
        "catalog-direct-blocked" -> listOf(
            message("STATE-05", chat.id, TODAY, "Today", 600, "10:00 AM", "STATE-05: History remains available"),
        )
        "catalog-direct-missing-relays" -> listOf(
            message("STATE-06", chat.id, TODAY, "Today", 600, "10:00 AM", "STATE-06: Check Chat Relays"),
        )
        "catalog-direct-disappearing", "catalog-direct-disappearing-muted", "catalog-group-disappearing" -> listOf(
            event("${chat.id}-indicator", chat.preview, TODAY, "Today", 600),
        )
        "maya-chen" -> maya(profileId)
        "weekend-walks" -> weekend(profileId)
        "fiatjaf" -> fiatjaf(profileId)
        ChatFixtures.SUPPORT_CHAT_ID -> listOf(
            notice(
                "support-guidance",
                "How can we help? Ask a question, report a problem, or share a suggestion. We’ll reply here.",
            ),
        )
        else -> defaultTimeline(chat, profileId)
    }

    private fun directText(profileId: String) = listOf(
        message("TXT-01", "catalog-direct-text", OLDER, "Jul 28, 2025", 540, "9:00 AM", "TXT-01: Incoming short text"),
        message("TXT-02", profileId, RECENT, "Jul 16", 600, "10:00 AM", "TXT-02: Outgoing short text"),
        message("TXT-06", "catalog-direct-text", YESTERDAY, "Yesterday", 660, "11:00 AM", "TXT-06: Multiline text\nSecond line\nThird line"),
        message("DLV-02", profileId, TODAY, "Today", 600, "10:00 AM", "DLV-02: Sent outgoing message"),
        message("DLV-03", profileId, TODAY, "Today", 610, "10:10 AM", "DLV-03: Failed outgoing message", delivery = MessageDeliveryState.Failed),
    )

    private fun directDates(profileId: String) = listOf(
        message("DATE-01", "catalog-direct-dates", OLDER, "Dec 8, 2025", 540, "9:00 AM", "DATE-01: Older year includes the year"),
        message("DATE-02", profileId, RECENT, "Jul 14", 600, "10:00 AM", "DATE-02: Recent date"),
        message("DATE-03", "catalog-direct-dates", YESTERDAY, "Yesterday", 660, "11:00 AM", "DATE-03: Yesterday boundary"),
        message("DATE-15", profileId, TODAY, "Today", 720, "12:00 PM", "DATE-15: Long day keeps its date pinned"),
    )

    private fun directReplies(profileId: String) = listOf(
        message("RPL-01", "catalog-direct-replies", YESTERDAY, "Yesterday", 600, "10:00 AM", "RPL-01: Original incoming message"),
        message("RPL-02", profileId, YESTERDAY, "Yesterday", 604, "10:04 AM", "RPL-02: Reply to incoming", reply = "RPL-01"),
        message("RPL-03-source", profileId, TODAY, "Today", 600, "10:00 AM", "RPL-03: Deleted source", deletion = MessageDeletionState.DeletedByCurrentProfile),
        message("RPL-03", "catalog-direct-replies", TODAY, "Today", 604, "10:04 AM", "RPL-03: Reply keeps a deleted fallback", reply = "RPL-03-source"),
        message("RPL-04", "catalog-direct-replies", TODAY, "Today", 612, "10:12 AM", "RPL-04: Missing reply target", reply = "missing-message"),
    )

    private fun directReactions(profileId: String) = listOf(
        message("RCT-01", "catalog-direct-reactions", YESTERDAY, "Yesterday", 560, "9:20 AM", "RCT-01: One reaction from another person", reactions = listOf(MessageReaction("❤", listOf("maya-chen")))),
        message("RCT-02", profileId, YESTERDAY, "Yesterday", 564, "9:24 AM", "RCT-02: Current-profile reaction", reactions = listOf(MessageReaction("❤", listOf(profileId)))),
        message("RCT-03", "catalog-direct-reactions", YESTERDAY, "Yesterday", 568, "9:28 AM", "RCT-03: Repeated reaction from three others", reactions = listOf(MessageReaction("👍", listOf("maya-chen", "nora-bennett", "catalog-direct-reactions")))),
        message("RCT-04", profileId, YESTERDAY, "Yesterday", 572, "9:32 AM", "RCT-04: Repeated reaction including you", reactions = listOf(MessageReaction("👎", listOf(profileId, "maya-chen", "nora-bennett")))),
        message("RCT-05", "catalog-direct-reactions", YESTERDAY, "Yesterday", 576, "9:36 AM", "RCT-05: Multiple reaction types", reactions = listOf(MessageReaction("😀", listOf("maya-chen")), MessageReaction("🔥", listOf(profileId, "nora-bennett")), MessageReaction("🦫", listOf("elias-moreno")))),
        message("RCT-06", profileId, YESTERDAY, "Yesterday", 580, "9:40 AM", "RCT-06: Heart", reactions = listOf(MessageReaction("❤", listOf("maya-chen")))),
        message("RCT-07", "catalog-direct-reactions", YESTERDAY, "Yesterday", 584, "9:44 AM", "RCT-07: Smile", reactions = listOf(MessageReaction("😀", listOf(profileId)))),
        message("RCT-08", profileId, YESTERDAY, "Yesterday", 588, "9:48 AM", "RCT-08: Thumbs up", reactions = listOf(MessageReaction("👍", listOf("maya-chen")))),
        message("RCT-09", "catalog-direct-reactions", YESTERDAY, "Yesterday", 592, "9:52 AM", "RCT-09: Thumbs down", reactions = listOf(MessageReaction("👎", listOf(profileId)))),
        message("RCT-10", profileId, TODAY, "Today", 600, "10:00 AM", "RCT-10: Laugh", reactions = listOf(MessageReaction("🤣", listOf("maya-chen")))),
        message("RCT-11", "catalog-direct-reactions", TODAY, "Today", 604, "10:04 AM", "RCT-11: Fire", reactions = listOf(MessageReaction("🔥", listOf(profileId)))),
        message("RCT-12", profileId, TODAY, "Today", 608, "10:08 AM", "RCT-12: Beaver", reactions = listOf(MessageReaction("🦫", listOf("maya-chen")))),
        message("RCT-13", profileId, TODAY, "Today", 612, "10:12 AM", "Narrow", reactions = listOf("❤", "😀", "👍", "👎", "🤣", "🔥", "🦫").map { MessageReaction(it, listOf("maya-chen")) }),
        message("ACT-01", "catalog-direct-reactions", TODAY, "Today", 620, "10:20 AM", "ACT-01: Incoming text actions"),
        message("ACT-02", profileId, TODAY, "Today", 624, "10:24 AM", "ACT-02: Outgoing text actions"),
        message("ACT-03", "catalog-direct-reactions", TODAY, "Today", 628, "10:28 AM", attachments = listOf(photo("ACT-03-photo", "Incoming attachment", AvatarAsset.Badger))),
        message("ACT-04", profileId, TODAY, "Today", 632, "10:32 AM", attachments = listOf(photo("ACT-04-photo", "Outgoing attachment", AvatarAsset.Fox))),
        message("ACT-05", "catalog-direct-reactions", TODAY, "Today", 636, "10:36 AM", "ACT-05: Share available file URL", attachments = listOf(file("ACT-05-file", "Trail Plan.pdf"))),
    )

    private fun mediaSingle(profileId: String) = listOf(
        message("MED-01", "catalog-media-single", YESTERDAY, "Yesterday", 600, "10:00 AM", attachments = listOf(photo("MED-01-photo", "Portrait photo", AvatarAsset.MayaChen))),
        message("MED-02", profileId, YESTERDAY, "Yesterday", 605, "10:05 AM", "MED-02: Outgoing landscape", attachments = listOf(photo("MED-02-photo", "Landscape photo", AvatarAsset.GardenClub))),
        message("MED-11", "catalog-media-single", TODAY, "Today", 600, "10:00 AM", attachments = listOf(MessageAttachment("MED-11-video", MessageAttachmentKind.Video, "Trail video, 0:12", images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub))))),
        message("MED-12", profileId, TODAY, "Today", 610, "10:10 AM", "MED-12: Unavailable video", attachments = listOf(MessageAttachment("MED-12-video", MessageAttachmentKind.Video, "Unavailable video"))),
    )

    private fun mediaGalleries(profileId: String): List<ChatTimelineEntry> = (2..7).mapIndexed { index, count ->
        val images = listOf(
            AvatarAsset.Marmot,
            AvatarAsset.Badger,
            AvatarAsset.Fox,
            AvatarAsset.Sloth,
            AvatarAsset.Ostrich,
            AvatarAsset.GardenClub,
            AvatarAsset.MayaChen,
        ).take(count).map(ProfileAvatar::Asset)
        message(
            id = "MED-GALLERY-${count.toString().padStart(2, '0')}",
            authorId = if (index % 2 == 0) "catalog-media-gallery" else profileId,
            day = TODAY,
            dayLabel = "Today",
            minute = 560 + index * 8,
            time = "10:${(index * 8).toString().padStart(2, '0')} AM",
            text = if (count == 7) "MED-GALLERY-08: Larger overflow" else "$count photos",
            attachments = listOf(MessageAttachment("gallery-$count", MessageAttachmentKind.Photos, "$count Photos", images)),
        )
    }

    private fun mediaViewer(profileId: String) = listOf(
        message(
            "MED-VIEW-01",
            "catalog-media-viewer",
            TODAY,
            "Today",
            600,
            "10:00 AM",
            "Tap the gallery to page through it.",
            attachments = listOf(
                MessageAttachment(
                    "viewer-gallery",
                    MessageAttachmentKind.Photos,
                    "4 Photos",
                    listOf(AvatarAsset.Marmot, AvatarAsset.Badger, AvatarAsset.Fox, AvatarAsset.Sloth).map(ProfileAvatar::Asset),
                ),
            ),
        ),
        message("MED-VIEW-07", profileId, TODAY, "Today", 610, "10:10 AM", "MED-VIEW-07: Unavailable media excluded", attachments = listOf(MessageAttachment("viewer-unavailable", MessageAttachmentKind.Photo, "Unavailable media"))),
    )

    private fun richContent(profileId: String) = listOf(
        message("RICH-01", "maya-chen", YESTERDAY, "Yesterday", 600, "10:00 AM", attachments = listOf(MessageAttachment("rich-file", MessageAttachmentKind.File, "Project Brief.pdf"))),
        message("RICH-02", profileId, YESTERDAY, "Yesterday", 610, "10:10 AM", "Useful reference", attachments = listOf(LinkPreviewDetector.first("https://developer.android.com")!!.attachment("rich-link"))),
        message("RICH-03", "catalog-media-rich", TODAY, "Today", 600, "10:00 AM", attachments = listOf(MessageAttachment("rich-gif", MessageAttachmentKind.Gif, "Marmot looking around", images = listOf(ProfileAvatar.Asset(AvatarAsset.Marmot))))),
        message("RICH-05", profileId, TODAY, "Today", 610, "10:10 AM", "RICH-05: Valid contact", attachments = listOf(MessageAttachment("rich-contact", MessageAttachmentKind.Contact, "Contact: Maya Chen", images = listOf(ProfileAvatar.Asset(AvatarAsset.MayaChen))))),
    )

    private fun voiceMessages(profileId: String) = listOf(
        message("VOICE-01", "catalog-voice", YESTERDAY, "Yesterday", 600, "10:00 AM", attachments = VoiceMessageFixture.result("VOICE-01-audio", VoiceMessageFormat.Voice).second),
        message("VOICE-02", profileId, TODAY, "Today", 600, "10:00 AM", VoiceMessageFixture.transcript, attachments = VoiceMessageFixture.result("VOICE-02-audio", VoiceMessageFormat.Both).second),
        message("VOICE-03", "catalog-voice", TODAY, "Today", 610, "10:10 AM", VoiceMessageFixture.transcript, attachments = VoiceMessageFixture.result("VOICE-03-audio", VoiceMessageFormat.Both).second),
    )

    private fun groupMessages(profileId: String) = listOf(
        event("EVT-02", "Maya Chen created the group.", OLDER, "Jan 12", 540),
        message("GRP-01", "maya-chen", OLDER, "Jan 12", 600, "10:00 AM", "GRP-01: Incoming group cluster start"),
        message("GRP-02", "maya-chen", OLDER, "Jan 12", 604, "10:04 AM", "GRP-02: Same-author cluster end"),
        message("GRP-03", profileId, RECENT, "Jul 18", 600, "10:00 AM", "GRP-03: Outgoing group message"),
        message("MENTION-01", "maya-chen", YESTERDAY, "Yesterday", 600, "10:00 AM", "MENTION-01: @Marmota please review this."),
        message("GRP-RPL-01", "elias-moreno", TODAY, "Today", 600, "10:00 AM", "GRP-RPL-01: Elias asks a question"),
        message("GRP-RPL-02", "maya-chen", TODAY, "Today", 604, "10:04 AM", "GRP-RPL-02: Maya replies to Elias", reply = "GRP-RPL-01"),
    )

    private fun identityColors(): List<ChatTimelineEntry> {
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
        return people.mapIndexed { index, (personId, text) ->
            message(
                "COLOR-${index + 1}",
                personId,
                TODAY,
                "Today",
                600 + index,
                "10:${index.toString().padStart(2, '0')} AM",
                text,
            )
        }
    }

    private fun groupEvents(profileId: String) = listOf(
        event("EVT-02", "You created the group.", RECENT, "Jul 18", 600),
        event("EVT-03", "You added Maya Chen.", RECENT, "Jul 18", 601),
        event("EVT-11", "You made Maya Chen an admin.", RECENT, "Jul 18", 602),
        event("EVT-04", "Maya Chen added Elias Moreno, Nora Bennett, Leo Martins, and Mina Park.", YESTERDAY, "Yesterday", 600),
        message("EVT-message", profileId, YESTERDAY, "Yesterday", 610, "10:10 AM", "The group is ready."),
        event("EVT-12", "Maya Chen made you an admin.", TODAY, "Today", 600),
        event("EVT-13", "You removed Maya Chen as an admin.", TODAY, "Today", 601),
        event("EVT-15", "Elias Moreno turned off disappearing messages.", TODAY, "Today", 602),
    )

    private fun maya(profileId: String) = listOf(
        message("maya-1", "maya-chen", OLDER, "Jun 2", 540, "9:00 AM", "Hi—Maya here."),
        message("maya-2", profileId, OLDER, "Jun 2", 545, "9:05 AM", "Good to hear from you."),
        message("maya-3", "maya-chen", OLDER, "Jun 2", 550, "9:10 AM", "I pulled the first draft together.\nCould you look at the opening and the final paragraph?"),
        message("maya-4", profileId, RECENT, "Jul 16", 600, "10:00 AM", "This is the latest photo.", attachments = listOf(photo("maya-photo", "Latest photo", AvatarAsset.MayaChen)), reactions = listOf(MessageReaction("❤", listOf("maya-chen")))),
        message("maya-5", "maya-chen", RECENT, "Jul 16", 604, "10:04 AM", "The crop looks great.", reply = "maya-4"),
        message("maya-10", "maya-chen", YESTERDAY, "Yesterday", 600, "10:00 AM", "Here are the notes from our last pass.", attachments = listOf(file("maya-file", "Weekend Notes.pdf"))),
        message("maya-13", "maya-chen", TODAY, "Today", 540, "9:00 AM", deletion = MessageDeletionState.DeletedByOther),
        message("maya-17", "maya-chen", TODAY, "Today", 600, "10:00 AM", "Can you send the latest version when you have a moment?", reactions = listOf(MessageReaction("😀", listOf(profileId, "maya-chen")))),
    )

    private fun weekend(profileId: String) = listOf(
        event("week-event-created", "You created the group.", OLDER, "May 9", 540),
        event("week-event-added", "You added Maya Chen and Elias Moreno.", OLDER, "May 9", 541),
        message("week-msg-1", "maya-chen", OLDER, "May 9", 600, "10:00 AM", "Thanks for setting this up."),
        message("week-msg-2", "elias-moreno", OLDER, "May 9", 610, "10:10 AM", "The riverside path is open again."),
        message("week-msg-9", "maya-chen", RECENT, "Jul 17", 600, "10:00 AM", "I’ll organize the route options and meeting points."),
        message("week-msg-12", profileId, YESTERDAY, "Yesterday", 600, "10:00 AM", "The riverside path works for me.", reactions = listOf(MessageReaction("❤", listOf("maya-chen", "elias-moreno")))),
        message("week-msg-14", "maya-chen", YESTERDAY, "Yesterday", 604, "10:04 AM", "@Marmota, does the riverside path work?"),
        message("week-msg-20", "nora-bennett", TODAY, "Today", 600, "10:00 AM", "Saturday morning works for me."),
        message("week-msg-21", "maya-chen", TODAY, "Today", 610, "10:10 AM", "Not the steep shortcut—the entrance with the sunlit trees.", attachments = listOf(photo("week-photo", "Sunlit trail", AvatarAsset.Marmot))),
    )

    private fun fiatjaf(profileId: String) = listOf(
        message("fiatjaf-1", profileId, TODAY, "Today", 540, "9:00 AM", "I’m moving from Feather to White Noise."),
        message("fiatjaf-2", "fiatjaf", TODAY, "Today", 542, "9:02 AM", "Let me know how it goes."),
        message("fiatjaf-3", profileId, TODAY, "Today", 544, "9:04 AM", "Signing in now.\nI’ll send a test next."),
        message("fiatjaf-4", profileId, TODAY, "Today", 548, "9:08 AM", "Switched from Feather to White Noise. Same key, same contacts."),
        message("fiatjaf-5", "fiatjaf", TODAY, "Today", 552, "9:12 AM", "Yep, I still see you on Primal. No extra setup on my side.", reply = "fiatjaf-4"),
        message("fiatjaf-6", profileId, TODAY, "Today", 556, "9:16 AM", "Exactly. Moved apps, kept everything. Didn’t have to re-add anyone.", reactions = listOf(MessageReaction("🔥", listOf("fiatjaf")))),
        message("fiatjaf-7", "fiatjaf", TODAY, "Today", 560, "9:20 AM", "Perfect!"),
        message(
            "fiatjaf-8",
            "fiatjaf",
            TODAY,
            "Today",
            564,
            "9:24 AM",
            "Portable identity for the win.",
            attachments = listOf(
                MessageAttachment(
                    id = "fiatjaf-gallery",
                    kind = MessageAttachmentKind.Photos,
                    label = "5 Photos",
                    images = listOf(
                        AvatarAsset.Marmot,
                        AvatarAsset.Badger,
                        AvatarAsset.Fox,
                        AvatarAsset.Sloth,
                        AvatarAsset.Ostrich,
                    ).map { ProfileAvatar.Asset(it) },
                ),
            ),
        ),
    )

    private fun defaultTimeline(chat: Chat, profileId: String): List<ChatTimelineEntry> {
        val authorId = when {
            chat.previewAuthor == "You" -> profileId
            chat.isGroup -> when (chat.previewAuthor) {
                "Nora" -> "nora-bennett"
                "Maya" -> "maya-chen"
                "Owen" -> "owen"
                "Iris" -> "iris"
                "Noah" -> "noah"
                "Sam" -> "sam"
                else -> "maya-chen"
            }
            else -> (chat.kind as? ChatKind.Direct)?.personId ?: chat.id
        }
        val body = chat.preview.ifBlank { chat.attachmentPreview?.label ?: "Conversation history" }
        val attachment = chat.attachmentPreview?.let { preview ->
            val kind = when (preview) {
                AttachmentPreview.Photo -> MessageAttachmentKind.Photo
                is AttachmentPreview.Photos -> MessageAttachmentKind.Photos
                AttachmentPreview.Video -> MessageAttachmentKind.Video
                AttachmentPreview.VoiceMessage -> MessageAttachmentKind.Voice
                is AttachmentPreview.File -> MessageAttachmentKind.File
                is AttachmentPreview.Contact -> MessageAttachmentKind.Contact
                AttachmentPreview.Link -> MessageAttachmentKind.Link
                AttachmentPreview.Gif -> MessageAttachmentKind.Gif
            }
            MessageAttachment("${chat.id}-attachment", kind, preview.label)
        }
        return listOf(
            message(
                id = "${chat.id}-message",
                authorId = authorId,
                day = TODAY,
                dayLabel = "Today",
                minute = 600,
                time = "10:00 AM",
                text = if (attachment == null) body else chat.preview,
                attachments = listOfNotNull(attachment),
                delivery = if (chat.deliveryState == ChatDeliveryState.Failed) MessageDeliveryState.Failed else MessageDeliveryState.Sent,
            ),
        )
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

    private fun photo(id: String, label: String, asset: AvatarAsset) = MessageAttachment(
        id,
        MessageAttachmentKind.Photo,
        label,
        listOf(ProfileAvatar.Asset(asset)),
    )

    private fun file(id: String, label: String) = MessageAttachment(id, MessageAttachmentKind.File, label)
}
