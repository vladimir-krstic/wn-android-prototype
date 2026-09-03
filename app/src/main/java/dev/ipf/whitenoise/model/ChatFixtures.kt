package dev.ipf.whitenoise.model

object ChatFixtures {
    const val SUPPORT_CHAT_ID = "white-noise-support"
    const val DEFAULT_CHAT_RELAY = "wss://relay.primal.net"

    val archivedIds = setOf(
        "catalog-direct-archived",
        "road-trip",
        "garden-club",
        "old-studio",
        "design-notes",
    )

    private val groupIds = setOf(
        "catalog-composer-mention",
        "catalog-group-messages",
        "catalog-group-colors",
        "catalog-group-events",
        "catalog-group-member",
        "catalog-group-sole-admin",
        "catalog-group-disappearing",
        "catalog-group-invitation",
        "catalog-group-left",
        "catalog-group-removed",
        "nostr-devs",
        "marmots",
        "weekend-walks",
        "product-circle",
        "book-club",
        "quiet-studio",
        "project-files",
        "neighborhood",
        "reading-list",
        "family-group",
        "photo-swap",
        "road-trip",
        "garden-club",
        "old-studio",
        "design-notes",
    )

    fun people(): List<Person> {
        val peopleFromChats = populatedChats(
            profileId = "fixture-profile",
            relayUrls = listOf(DEFAULT_CHAT_RELAY),
        ).asSequence()
            .filter { !it.isGroup && it.id != "catalog-direct-invitation" && it.id != SUPPORT_CHAT_ID }
            .map { chat ->
                Person(
                    id = chat.id,
                    name = chat.title,
                    about = aboutFor(chat.id),
                    nostrAddress = "${chat.id}@whitenoise.example",
                    isNostrAddressVerified = chat.id != "satoshi-nakamoto",
                    avatar = chat.avatar,
                    isFollowing = chat.id != "satoshi-nakamoto",
                    isBlocked = chat.id == "catalog-direct-blocked",
                )
            }
            .toMutableList()

        val extras = listOf(
            person("tim", "Tim"),
            person("jude", "Jude"),
            person("sam", "Sam"),
            person("owen", "Owen"),
            person("remy", "Remy"),
            person("iris", "Iris"),
            person("noah", "Noah"),
            person("avery-stone", "Avery Stone"),
            person("theo-grant", "Theo Grant", AvatarAsset.TheoGrant),
            person("identity-color-1", "Amina Cole", monogram = true),
            person("identity-color-7", "Bruno Diaz", monogram = true),
            person("identity-color-13", "Chloe Evans", monogram = true),
            person("identity-color-12", "Darius Ford", monogram = true),
            person("identity-color-0", "Eleni Gray", monogram = true),
            person("identity-color-2", "Farah Hall", monogram = true),
            person("identity-color-11", "Gideon Ito", monogram = true),
            person("identity-color-5", "Hana Jones", monogram = true),
            person("identity-color-22", "Imani King", monogram = true),
        )
        extras.forEach { candidate ->
            if (peopleFromChats.none { it.id == candidate.id }) peopleFromChats += candidate
        }
        return peopleFromChats
    }

    fun populatedChats(
        profileId: String,
        relayUrls: List<String> = listOf(DEFAULT_CHAT_RELAY),
    ): List<Chat> = listOf(
        catalog("catalog-direct-text", "Direct - Text & Delivery", "DLV-03: Failed outgoing message", pinned = true, delivery = ChatDeliveryState.Failed),
        catalog("catalog-direct-dates", "Direct - Dates & Scrolling", "DATE-15: Long day keeps its date pinned"),
        catalog("catalog-direct-replies", "Direct - Replies & Deletion", "RPL-04: Missing reply target", unread = 3),
        catalog("catalog-direct-reactions", "Direct - Reactions & Actions", "ACT-05: Share available file URL", markedUnread = true),
        catalog("catalog-direct-new-draft", "Direct - New Chat & Draft", "STATE-01: Unsent draft", draft = true),
        catalog("catalog-composer-text", "Composer - Text", "Here’s the updated plan.", draft = true),
        catalog("catalog-composer-multiline", "Composer - Multiline", "I pulled together the notes:", draft = true),
        catalog("catalog-composer-link", "Composer - Link", "https://whitenoise.chat", draft = true),
        catalog("catalog-composer-link-preview", "Composer - Link Preview", "Worth a look: https://developer.apple.com", draft = true),
        catalog("catalog-composer-photo", "Composer - Photo", "Photo ready to send", draft = true),
        catalog("catalog-composer-photo-album", "Composer - Photo Album", "A few from today.", draft = true),
        catalog("catalog-composer-mixed-media", "Composer - Mixed Media", "Photos and a short clip from the walk.", draft = true),
        catalog("catalog-composer-file", "Composer - File", "Here’s the brief.", draft = true),
        catalog("catalog-composer-gif", "Composer - GIF", "GIF ready to send", draft = true),
        catalog("catalog-composer-contact", "Composer - Contact", "Maya can help with this.", draft = true),
        catalog("catalog-composer-reply", "Composer - Reply", "Yes—Thursday afternoon works for me.", draft = true),
        catalog("catalog-composer-mention", "Composer - Mention", "@Maya Chen can you take a look?", author = null, draft = true),
        catalog("catalog-media-single", "Media - Single Photos & Video", "MED-12: Unavailable video", muted = true),
        catalog("catalog-media-gallery", "Media - Gallery Layouts", "MED-GALLERY-08: Larger overflow"),
        catalog("catalog-media-viewer", "Media - Viewer & Actions", "MED-VIEW-07: Unavailable media excluded"),
        catalog("catalog-media-rich", "Media - Files & Rich Content", "RICH-05: Valid contact"),
        catalog("catalog-voice", "Voice Messages", "", attachment = AttachmentPreview.VoiceMessage),
        catalog("catalog-group-messages", "Group - Messages & Mentions", "GRP-RPL-02: Cross-author reply", author = "Maya"),
        catalog("catalog-group-colors", "Group - Identity Colors", "COLOR-09: Brown identity color", author = "Imani"),
        catalog("catalog-group-events", "Group - Events & Roles", "Elias Moreno turned off disappearing messages."),
        catalog("catalog-group-member", "Group - Member Permissions", "ROLE-02: Ordinary member permissions"),
        catalog("catalog-group-sole-admin", "Group - Sole Admin", "ROLE-03: Promote another admin before leaving"),
        catalog("catalog-direct-disappearing", "Direct - Disappearing", "IND-01: 1 Day disappearing messages", disappearing = DisappearingDuration.OneDay),
        catalog("catalog-direct-disappearing-muted", "Direct - Disappearing & Muted", "IND-02: 1 Week disappearing messages and muted", muted = true, disappearing = DisappearingDuration.OneWeek),
        catalog("catalog-group-disappearing", "Group - Disappearing", "IND-03: 4 Weeks disappearing messages", author = "Maya", disappearing = DisappearingDuration.FourWeeks),
        catalog("catalog-direct-invitation", "Direct - Invitation", "STATE-09: Are you free for a quick call tomorrow?", membership = ChatMembership.Invited, inviter = "Avery Stone", unread = 1),
        catalog("catalog-group-invitation", "Group - Invitation", "STATE-10B: Bring water and a light jacket.", membership = ChatMembership.Invited, inviter = "Maya Chen", unread = 2),
        catalog("catalog-direct-left", "Direct - Left", "You left the chat.", membership = ChatMembership.Left),
        catalog("catalog-group-left", "Group - Left", "You left the group.", membership = ChatMembership.Left),
        catalog("catalog-group-removed", "Group - Removed", "Maya Chen removed you from the group.", membership = ChatMembership.Removed),
        catalog("catalog-direct-blocked", "Direct - Blocked", "STATE-05: History remains available"),
        catalog("catalog-direct-missing-relays", "Direct - Missing Relays", "STATE-06: Check Chat Relays"),
        catalog("catalog-direct-archived", "Direct - Archived", "STATE-07: Unarchive from Chat Info", archived = true),
        row(SUPPORT_CHAT_ID, "Support - Timeline Notice", "Ask a question, report a problem, or share a suggestion.", "Thursday"),
        row("nostr-devs", "Nostr Devs", "Marmot draft merged. Time to test the new flow.", "Yesterday", author = "Tim", avatar = AvatarAsset.LegacyNostrDevs),
        row("radia-perlman", "Radia Perlman", "Let the network heal itself; loops (and censors) break.", "Sunday", avatar = AvatarAsset.LegacyRadiaPerlman),
        row("hal-finney", "Hal Finney", "Running bitcoin… still amazes me how far we’ve come.", "Now", avatar = AvatarAsset.LegacyHalFinney),
        row("judith-milhon", "Judith “St. Jude” Milhon", "Hacking means finding clever ways around dumb rules.", "2m", unread = 2, avatar = AvatarAsset.LegacyJudithMilhon),
        row("marmots", "Marmots", "Big plans—or no plans at all!", "9m", author = "Jude", unread = 128, avatar = AvatarAsset.LegacyMarmots),
        row("whitfield-diffie", "Whitfield Diffie", "Key exchange since ’76—still my favorite handshake.", "1h", author = "You", avatar = AvatarAsset.LegacyWhitfieldDiffie),
        row("richard-stallman", "Richard Stallman", "Free as in freedom, not as in beer. Keep your keys libre.", "8h", muted = true, avatar = AvatarAsset.LegacyRichardStallman),
        row("eric-hughes", "Eric Hughes", "Cypherpunks still write code. Ship the patch?", "Yesterday", unread = 12, avatar = AvatarAsset.LegacyEricHughes),
        row("david-chaum", "David Chaum", "Privacy is necessary for an open society in the electronic age.", "Saturday", avatar = AvatarAsset.LegacyDavidChaum),
        row("satoshi-nakamoto", "Satoshi Nakamoto", "Chancellor on Brink of Second Bailout for Banks.", "Friday", avatar = AvatarAsset.LegacySatoshiNakamoto),
        row("fiatjaf", "Fiatjaf", "Portable identity for the win.", "Thursday", attachment = AttachmentPreview.Photos(5), avatar = AvatarAsset.Fiatjaf),
        row("mina-park", "Mina Park", "Let’s pick this up after lunch", "Thursday", draft = true, avatar = AvatarAsset.MinaPark),
        row("theo-grant", "Theo Grant", "", "Wednesday", unread = 1, attachment = AttachmentPreview.VoiceMessage, avatar = AvatarAsset.TheoGrant),
        row("maya-chen", "Maya Chen", "Can you send the latest version when you have a moment?", "Monday", unread = 1, avatar = AvatarAsset.MayaChen),
        row("weekend-walks", "Weekend Walks", "Saturday morning works for me.", "Sunday", author = "Nora", unread = 12, avatar = AvatarAsset.Pebble),
        row("elias-moreno", "Elias Moreno", "I’ll be there at seven.", "7/19/26", author = "You", avatar = AvatarAsset.EliasMoreno),
        row("product-circle", "Product Circle", "I added the notes from today’s session.", "7/18/26", author = "Sam", unread = 128, muted = true),
        row("leo-martins", "Leo Martins", "Here’s the address.", "7/17/26", author = "You", avatar = AvatarAsset.LeoMartins),
        row("aisha-rahman", "Aisha Rahman", "", "7/16/26", attachment = AttachmentPreview.Photos(3), avatar = AvatarAsset.AishaRahman),
        row("lena-ortiz", "Lena Ortiz", "That sounds perfect to me.", "7/15/26", avatar = AvatarAsset.LenaOrtiz),
        row("nora-bennett", "Nora Bennett", "", "7/14/26", author = "You", attachment = AttachmentPreview.Photo, delivery = ChatDeliveryState.Failed, avatar = AvatarAsset.NoraBennett),
        row("book-club", "Book Club", "The next chapter is shorter than it looks.", "7/13/26", author = "Owen", membership = ChatMembership.Left),
        row("quiet-studio", "Quiet Studio Group", "I’ll lock up when I leave.", "7/12/26", author = "Remy", membership = ChatMembership.Removed),
        row("jonah-reed", "Jonah Reed", "I sent the details.", "7/11/26", author = "You", avatar = AvatarAsset.JonahReed),
        row("tessa-morgan", "Tessa Morgan", "Could we move it to Thursday?", "7/10/26", unread = 2, avatar = AvatarAsset.TessaMorgan),
        row("marcus-bell", "Marcus Bell", "Thanks, I’ll take a look tonight.", "7/9/26", avatar = AvatarAsset.MarcusBell),
        row("sofia-alvarez", "Sofia Alvarez", "", "7/8/26", author = "You", attachment = AttachmentPreview.Video, avatar = AvatarAsset.SofiaAlvarez),
        row("daniel-kim", "Daniel Kim", "The file opened without any problems.", "7/7/26", unread = 1, avatar = AvatarAsset.DanielKim),
        row("project-files", "Project Files", "", "7/6/26", author = "You", attachment = AttachmentPreview.File("Project Brief.pdf")),
        row("neighborhood", "Neighborhood", "", "7/5/26", author = "Maya", attachment = AttachmentPreview.Photo),
        row("jamie-cooper", "Jamie Cooper", "", "7/4/26", attachment = AttachmentPreview.Contact("Avery Stone")),
        row("reading-list", "Reading List", "", "7/3/26", author = "Owen", attachment = AttachmentPreview.Link),
        row("family-group", "Family Group", "", "7/2/26", author = "Nora", unread = 3, attachment = AttachmentPreview.Gif),
        row("photo-swap", "Photo Swap", "", "7/1/26", author = "You", muted = true, attachment = AttachmentPreview.Photos(3)),
        row("road-trip", "Road Trip", "The playlist is ready.", "7/12/26", author = "You", archived = true),
        row("garden-club", "Garden Club", "The seedlings made it through the heat.", "7/8/26", author = "Iris", unread = 4, archived = true, avatar = AvatarAsset.GardenClub),
        row("old-studio", "Old Studio", "Everything has been packed away.", "6/29/26", author = "Noah", muted = true, archived = true),
        row("design-notes", "Design Notes", "", "6/21/26", author = "You", attachment = AttachmentPreview.File("Project Notes.pdf"), archived = true),
    ).mapIndexed { index, chat ->
        val composerSeed = ComposerFixtures.seed(
            chatId = chat.id,
            fallbackText = chat.preview.takeIf { chat.isDraft }.orEmpty(),
        )
        val members = membersFor(chat, profileId)
        chat.copy(
            originalOrder = index,
            kind = if (chat.id == "catalog-direct-invitation") {
                ChatKind.Direct("avery-stone")
            } else {
                chat.kind
            },
            relayUrls = if (chat.id == "catalog-direct-missing-relays") emptyList() else relayUrls,
            defaultRelayUrls = relayUrls,
            members = members,
            description = groupDescriptionFor(chat.id).takeIf { chat.isGroup }.orEmpty(),
            timeline = ConversationFixtures.timelineFor(chat, profileId),
            draftText = composerSeed.text,
            draftAttachments = composerSeed.attachments,
            suppressedDraftLinkUrl = composerSeed.suppressedLinkUrl,
            draftReplyMessageId = composerSeed.replyMessageId,
        )
    }

    private fun membersFor(chat: Chat, profileId: String): List<GroupMember> {
        if (!chat.isGroup) return emptyList()
        if (chat.membership == ChatMembership.Invited) {
            return listOf(
                GroupMember("maya-chen", GroupRole.Admin),
                GroupMember("elias-moreno", GroupRole.Member),
                GroupMember("nora-bennett", GroupRole.Member),
            )
        }
        if (chat.hasEndedMembership) {
            return listOf(
                GroupMember("maya-chen", GroupRole.Admin),
                GroupMember("elias-moreno", GroupRole.Member),
            )
        }
        return when (chat.id) {
            "catalog-composer-mention" -> listOf(
                GroupMember(profileId, GroupRole.Member),
                GroupMember("maya-chen", GroupRole.Admin),
                GroupMember("elias-moreno", GroupRole.Member),
            )
            "catalog-group-messages" -> listOf(
                GroupMember(profileId, GroupRole.Member),
                GroupMember("maya-chen", GroupRole.Admin),
                GroupMember("elias-moreno", GroupRole.Member),
                GroupMember("nora-bennett", GroupRole.Member),
                GroupMember("mina-park", GroupRole.Member),
            )
            "catalog-group-colors" -> listOf(GroupMember(profileId, GroupRole.Admin)) +
                listOf(1, 7, 13, 12, 0, 2, 11, 5, 22).map {
                    GroupMember("identity-color-$it", GroupRole.Member)
                }
            "catalog-group-events" -> listOf(
                GroupMember(profileId, GroupRole.Admin),
                GroupMember("maya-chen", GroupRole.Member),
                GroupMember("elias-moreno", GroupRole.Admin),
                GroupMember("mina-park", GroupRole.Member),
            )
            "catalog-group-member" -> listOf(
                GroupMember(profileId, GroupRole.Member),
                GroupMember("maya-chen", GroupRole.Admin),
                GroupMember("elias-moreno", GroupRole.Member),
            )
            "catalog-group-sole-admin" -> listOf(
                GroupMember(profileId, GroupRole.Admin),
                GroupMember("maya-chen", GroupRole.Member),
                GroupMember("elias-moreno", GroupRole.Member),
            )
            "weekend-walks" -> listOf(
                GroupMember(profileId, GroupRole.Admin),
                GroupMember("maya-chen", GroupRole.Member),
                GroupMember("mina-park", GroupRole.Member),
                GroupMember("elias-moreno", GroupRole.Member),
                GroupMember("nora-bennett", GroupRole.Member),
            )
            else -> {
                val currentRole = if (chat.id == "product-circle") GroupRole.Member else GroupRole.Admin
                val memberIds = buildList {
                    addAll(listOf("maya-chen", "mina-park", "elias-moreno", "nora-bennett", "leo-martins"))
                    when (chat.id) {
                        "nostr-devs" -> addAll(listOf("radia-perlman", "david-chaum"))
                        "marmots", "project-files" -> add("radia-perlman")
                    }
                }
                listOf(GroupMember(profileId, currentRole)) + memberIds.map { personId ->
                    GroupMember(
                        personId,
                        if (chat.id == "product-circle" && personId == "maya-chen") {
                            GroupRole.Admin
                        } else {
                            GroupRole.Member
                        },
                    )
                }
            }
        }
    }

    private fun groupDescriptionFor(id: String): String = when (id) {
        "catalog-group-events" -> ""
        "weekend-walks" -> "Plans, routes, and photos from our weekend walks."
        "product-circle" -> "Notes and decisions from the product circle."
        "nostr-devs" -> "Building and testing open communication tools."
        else -> "A shared space for this group."
    }

    private fun catalog(
        id: String,
        title: String,
        preview: String,
        author: String? = null,
        attachment: AttachmentPreview? = null,
        membership: ChatMembership = ChatMembership.Active,
        inviter: String? = null,
        archived: Boolean = false,
        pinned: Boolean = false,
        unread: Int = 0,
        markedUnread: Boolean = false,
        muted: Boolean = false,
        disappearing: DisappearingDuration = DisappearingDuration.Off,
        draft: Boolean = false,
        delivery: ChatDeliveryState = ChatDeliveryState.None,
    ): Chat = row(
        id = id,
        title = title,
        preview = preview,
        timestamp = "Now",
        author = author,
        attachment = attachment,
        membership = membership,
        inviter = inviter,
        archived = archived,
        pinned = pinned,
        unread = unread,
        markedUnread = markedUnread,
        muted = muted,
        disappearing = disappearing,
        draft = draft,
        delivery = delivery,
        avatar = catalogAvatar(id),
    )

    private fun row(
        id: String,
        title: String,
        preview: String,
        timestamp: String,
        author: String? = null,
        attachment: AttachmentPreview? = null,
        membership: ChatMembership = ChatMembership.Active,
        inviter: String? = null,
        archived: Boolean = false,
        pinned: Boolean = false,
        unread: Int = 0,
        markedUnread: Boolean = false,
        muted: Boolean = false,
        disappearing: DisappearingDuration = DisappearingDuration.Off,
        draft: Boolean = false,
        delivery: ChatDeliveryState = ChatDeliveryState.None,
        avatar: AvatarAsset? = null,
    ): Chat = Chat(
        id = id,
        originalOrder = -1,
        kind = if (id in groupIds) ChatKind.Group else ChatKind.Direct(id),
        title = title,
        avatar = avatar?.let { ProfileAvatar.Asset(it) } ?: ProfileAvatar.Monogram,
        preview = preview,
        previewAuthor = author,
        attachmentPreview = attachment,
        timestamp = timestamp,
        membership = membership,
        invitationInviterName = inviter,
        isArchived = archived,
        isPinned = pinned,
        unreadCount = unread,
        isMarkedUnread = markedUnread,
        muteDuration = MuteDuration.Always.takeIf { muted },
        disappearingDuration = disappearing,
        isDraft = draft,
        deliveryState = delivery,
    )

    private fun catalogAvatar(id: String): AvatarAsset {
        val assets = listOf(
            AvatarAsset.WebAionyHaust,
            AvatarAsset.WebAyoOgunseinde,
            AvatarAsset.WebChristopherCampbell,
            AvatarAsset.WebIanDooley,
            AvatarAsset.WebPhilipMartin,
            AvatarAsset.WebSergioDePaula,
            AvatarAsset.WebVinceFleming,
            AvatarAsset.MayaChen,
        )
        return assets[id.sumOf(Char::code) % assets.size]
    }

    private fun person(
        id: String,
        name: String,
        avatar: AvatarAsset? = null,
        monogram: Boolean = false,
    ): Person = Person(
        id = id,
        name = name,
        avatar = when {
            monogram -> ProfileAvatar.Monogram
            avatar != null -> ProfileAvatar.Asset(avatar)
            else -> deterministicPeopleAvatar(id)
        },
    )

    private fun deterministicPeopleAvatar(id: String): ProfileAvatar {
        val assets = listOf(
            AvatarAsset.MayaChen,
            AvatarAsset.EliasMoreno,
            AvatarAsset.MinaPark,
            AvatarAsset.LeoMartins,
            AvatarAsset.NoraBennett,
            AvatarAsset.TheoGrant,
            AvatarAsset.AishaRahman,
            AvatarAsset.LenaOrtiz,
            AvatarAsset.JonahReed,
            AvatarAsset.TessaMorgan,
            AvatarAsset.MarcusBell,
            AvatarAsset.SofiaAlvarez,
            AvatarAsset.DanielKim,
            AvatarAsset.WebAionyHaust,
            AvatarAsset.WebAyoOgunseinde,
            AvatarAsset.WebChristopherCampbell,
            AvatarAsset.WebIanDooley,
            AvatarAsset.WebPhilipMartin,
            AvatarAsset.WebSergioDePaula,
            AvatarAsset.WebVinceFleming,
        )
        return ProfileAvatar.Asset(assets[id.sumOf(Char::code) % assets.size])
    }

    private fun aboutFor(id: String): String = when (id) {
        "catalog-direct-text" -> "Turning complexity into clarity.\nAlways happy to compare notes."
        "catalog-direct-dates" -> "Planning one good day at a time.\nUsually outside before sunset. 🌤️"
        "catalog-direct-replies" -> "Making space for thoughtful conversations.\nCollecting useful references.\nLearning something new every day."
        "catalog-direct-reactions" -> "Here for good questions and honest answers.\nUsually carrying a camera. 📷\nSend the interesting ideas my way.\nTea and long walks help. 🍵"
        "maya-chen" -> "Designer, careful listener, and collector of useful references."
        "fiatjaf" -> "Building open tools for portable identity and resilient communication."
        SUPPORT_CHAT_ID -> "Help with White Noise."
        else -> "Quietly sharing ideas and keeping in touch."
    }
}
