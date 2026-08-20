import Foundation

private final class PrototypeChatFixtureBundleToken {}

enum PrototypeChatFixtures {
    private static let unsplashMemberAssets = [
        "AvatarMayaChen", "AvatarEliasMoreno", "AvatarMinaPark",
        "AvatarLeoMartins", "AvatarNoraBennett", "AvatarTheoGrant",
        "AvatarAishaRahman", "AvatarLenaOrtiz", "AvatarJonahReed",
        "AvatarTessaMorgan", "AvatarMarcusBell", "AvatarSofiaAlvarez",
        "AvatarDanielKim", "AvatarWebAionyHaust", "AvatarWebAyoOgunseinde",
        "AvatarWebChristopherCampbell", "AvatarWebIanDooley",
        "AvatarWebPhilipMartin", "AvatarWebSergioDePaula",
        "AvatarWebVinceFleming",
    ]

    static let groupIDs: Set<String> = [
        "catalog-group-messages", "catalog-group-colors", "catalog-group-events",
        "catalog-group-member", "catalog-group-sole-admin",
        "catalog-group-disappearing",
        "catalog-group-invitation",
        "catalog-composer-mention",
        "catalog-group-left", "catalog-group-removed",
        "nostr-devs", "marmots", "weekend-walks", "product-circle",
        "book-club", "quiet-studio", "project-files", "neighborhood",
        "reading-list", "family-group", "photo-swap", "road-trip",
        "garden-club", "old-studio", "design-notes",
    ]

    private static let identityColorPeople: [PrototypePerson] = [
        PrototypePerson(id: "identity-color-1", name: "Amina Cole", avatar: .monogram("A")),
        PrototypePerson(id: "identity-color-7", name: "Bruno Diaz", avatar: .monogram("B")),
        PrototypePerson(id: "identity-color-13", name: "Chloe Evans", avatar: .monogram("C")),
        PrototypePerson(id: "identity-color-12", name: "Darius Ford", avatar: .monogram("D")),
        PrototypePerson(id: "identity-color-0", name: "Eleni Gray", avatar: .monogram("E")),
        PrototypePerson(id: "identity-color-2", name: "Farah Hall", avatar: .monogram("F")),
        PrototypePerson(id: "identity-color-11", name: "Gideon Ito", avatar: .monogram("G")),
        PrototypePerson(id: "identity-color-5", name: "Hana Jones", avatar: .monogram("H")),
        PrototypePerson(id: "identity-color-22", name: "Imani King", avatar: .monogram("I")),
    ]

    static func people() -> [PrototypePerson] {
        var result: [PrototypePerson] = []

        for row in ChatListFixtures.populated
        where !groupIDs.contains(row.id) && row.id != "catalog-direct-invitation" {
            result.append(
                PrototypePerson(
                    id: row.id,
                    name: row.title,
                    about: about(for: row.id),
                    nostrAddress: "\(row.id)@whitenoise.example",
                    isNostrAddressVerified: row.id != "satoshi-nakamoto",
                    avatar: unsplashAvatar(for: row.id, preferred: row.avatar),
                    isFollowing: row.id != "satoshi-nakamoto",
                    isBlocked: row.id == "catalog-direct-blocked"
                )
            )
        }

        let extras: [PrototypePerson] = [
            PrototypePerson(id: "tim", name: "Tim", avatar: unsplashAvatar(for: "tim")),
            PrototypePerson(id: "jude", name: "Jude", avatar: unsplashAvatar(for: "jude")),
            PrototypePerson(id: "sam", name: "Sam", avatar: unsplashAvatar(for: "sam")),
            PrototypePerson(id: "owen", name: "Owen", avatar: unsplashAvatar(for: "owen")),
            PrototypePerson(id: "remy", name: "Remy", avatar: unsplashAvatar(for: "remy")),
            PrototypePerson(id: "iris", name: "Iris", avatar: unsplashAvatar(for: "iris")),
            PrototypePerson(id: "noah", name: "Noah", avatar: unsplashAvatar(for: "noah")),
            PrototypePerson(id: "avery-stone", name: "Avery Stone", avatar: unsplashAvatar(for: "avery-stone")),
            PrototypePerson(id: "theo-grant", name: "Theo Grant", avatar: unsplashAvatar(for: "theo-grant")),
        ] + identityColorPeople

        for person in extras where !result.contains(where: { $0.id == person.id }) {
            result.append(person)
        }
        return result
    }

    private static func unsplashAvatar(for id: String) -> ChatListItem.Avatar {
        let scalarSum = id.unicodeScalars.reduce(0) { $0 + Int($1.value) }
        return .asset(unsplashMemberAssets[scalarSum % unsplashMemberAssets.count])
    }

    private static func unsplashAvatar(
        for id: String,
        preferred: ChatListItem.Avatar
    ) -> ChatListItem.Avatar {
        if case let .asset(assetName) = preferred,
           unsplashMemberAssets.contains(assetName) {
            return preferred
        }
        return unsplashAvatar(for: id)
    }

    static func chats(
        profileID: String,
        relayURLs: [String],
        now: Date = .now
    ) -> [PrototypeChat] {
        ChatListFixtures.populated.map { row in
            var chat = baseChat(
                from: row,
                profileID: profileID,
                relayURLs: relayURLs,
                now: now
            )
            switch row.id {
            case "catalog-direct-text":
                chat.timeline = directTextTimeline(profileID: profileID, now: now)
            case "catalog-direct-dates":
                chat.timeline = directDatesTimeline(profileID: profileID, now: now)
            case "catalog-direct-replies":
                chat.timeline = directRepliesTimeline(profileID: profileID, now: now)
            case "catalog-direct-reactions":
                chat.timeline = directReactionsTimeline(profileID: profileID, now: now)
            case "catalog-direct-new-draft":
                chat.timeline = directNewChatTimeline(profileID: profileID, now: now)
            case let id where id.hasPrefix("catalog-composer-"):
                configureComposerFixture(
                    &chat,
                    id: id,
                    profileID: profileID,
                    now: now
                )
            case "catalog-media-single":
                chat.timeline = singleMediaTimeline(profileID: profileID, now: now)
            case "catalog-media-gallery":
                chat.timeline = mediaGalleryTimeline(profileID: profileID, now: now)
            case "catalog-media-viewer":
                chat.timeline = mediaViewerTimeline(profileID: profileID, now: now)
            case "catalog-media-rich":
                chat.timeline = richContentTimeline(profileID: profileID, now: now)
            case "catalog-voice":
                chat.timeline = voiceTimeline(profileID: profileID, now: now)
            case "catalog-group-messages":
                chat.members = groupMessageMembers(profileID: profileID)
                chat.timeline = groupMessagesTimeline(profileID: profileID, now: now)
            case "catalog-group-colors":
                chat.members = identityColorMembers(profileID: profileID)
                chat.timeline = identityColorTimeline(profileID: profileID, now: now)
            case "catalog-group-events":
                chat.members = groupEventMembers(profileID: profileID)
                chat.timeline = groupEventsTimeline(profileID: profileID, now: now)
                chat.groupDescription = ""
                chat.disappearingMessageDuration = .off
            case "catalog-group-member":
                chat.members = groupMemberMembers(profileID: profileID)
                chat.timeline = groupMemberTimeline(profileID: profileID, now: now)
            case "catalog-group-sole-admin":
                chat.members = soleAdminMembers(profileID: profileID)
                chat.timeline = soleAdminTimeline(profileID: profileID, now: now)
            case "catalog-direct-disappearing":
                chat.timeline = indicatorTimeline(
                    scenarioID: "IND-01",
                    authorID: row.id,
                    label: "IND-01: 1 Day disappearing messages",
                    now: now
                )
            case "catalog-direct-disappearing-muted":
                chat.timeline = indicatorTimeline(
                    scenarioID: "IND-02",
                    authorID: row.id,
                    label: "IND-02: 1 Week disappearing messages and muted",
                    now: now
                )
            case "catalog-group-disappearing":
                chat.timeline = indicatorTimeline(
                    scenarioID: "IND-03",
                    authorID: "maya-chen",
                    label: "IND-03: 4 Weeks disappearing messages",
                    now: now
                )
            case "catalog-direct-invitation":
                chat.kind = .direct(personID: "avery-stone")
                chat.invitedByPersonID = "avery-stone"
                chat.timeline = directInvitationTimeline(now: now)
            case "catalog-group-invitation":
                chat.invitedByPersonID = "maya-chen"
                chat.members = groupInvitationMembers()
                chat.timeline = groupInvitationTimeline(now: now)
            case "catalog-direct-left":
                chat.timeline = directLeftTimeline(profileID: profileID, now: now)
            case "catalog-group-left":
                chat.members = endedGroupMembers()
                chat.timeline = groupLeftTimeline(profileID: profileID, now: now)
            case "catalog-group-removed":
                chat.members = endedGroupMembers()
                chat.timeline = groupRemovedTimeline(profileID: profileID, now: now)
            case "catalog-direct-blocked":
                chat.timeline = recoveryTimeline(
                    profileID: profileID,
                    otherID: row.id,
                    scenarioID: "STATE-05",
                    label: "STATE-05: History remains available while blocked",
                    now: now
                )
            case "catalog-direct-missing-relays":
                chat.routing = PrototypeChatRouting(
                    relayURLs: [],
                    defaultRelayURLs: relayURLs
                )
                chat.timeline = recoveryTimeline(
                    profileID: profileID,
                    otherID: row.id,
                    scenarioID: "STATE-06",
                    label: "STATE-06: History remains available without chat relays",
                    now: now
                )
            case "catalog-direct-archived":
                chat.timeline = recoveryTimeline(
                    profileID: profileID,
                    otherID: row.id,
                    scenarioID: "STATE-07",
                    label: "STATE-07: Active archived chat",
                    now: now
                )
            case "maya-chen":
                chat.timeline = mayaTimeline(profileID: profileID, now: now)
            case "weekend-walks":
                chat.timeline = weekendTimeline(profileID: profileID, now: now)
            case ChatListFixtures.fiatjafChatID:
                chat.timeline = fiatjafTimeline(profileID: profileID, now: now)
            case ChatListFixtures.supportChatID:
                chat.timeline = [supportNotice(now: seedDate(for: row.timestamp, now: now))]
                chat.emptyPreview = "Ask a question, report a problem, or share a suggestion."
            default:
                break
            }
            if chat.draft.isEmpty, let latestDate = chat.timeline.last?.date {
                chat.listState.activityDate = latestDate
            }
            return chat
        }
    }

    static func supportNotice(now: Date) -> PrototypeTimelineEntry {
        .notice(
            PrototypeTimelineNotice(
                id: "STATE-08",
                date: now,
                text: "How can we help? Ask a question, report a problem, or share a suggestion. We’ll reply here."
            )
        )
    }

    private static func baseChat(
        from row: ChatListItem,
        profileID: String,
        relayURLs: [String],
        now: Date
    ) -> PrototypeChat {
        let isGroup = groupIDs.contains(row.id)
        let authorID = seedAuthorID(for: row, profileID: profileID)
        let attachments = seedAttachments(row.attachmentPreview, chatID: row.id)
        let message = PrototypeMessage(
            id: "\(row.id)-seed",
            authorID: authorID,
            sentAt: seedDate(for: row.timestamp, now: now),
            text: row.isDraft ? "" : row.preview,
            attachments: attachments,
            deliveryState: row.deliveryState == .failed ? .failed : .sent
        )
        var members: [PrototypeGroupMember] = []
        if isGroup {
            let currentRole: PrototypeGroupRole = row.id == "product-circle" ? .member : .admin
            if row.membershipState == .active {
                members = [PrototypeGroupMember(personID: profileID, role: currentRole)]
            }
            var currentMemberIDs = row.id == "weekend-walks"
                ? ["maya-chen", "mina-park", "elias-moreno", "nora-bennett"]
                : ["maya-chen", "mina-park", "elias-moreno", "nora-bennett", "leo-martins"]

            switch row.id {
            case "nostr-devs":
                currentMemberIDs += ["radia-perlman", "david-chaum"]
            case "marmots", "project-files":
                currentMemberIDs.append("radia-perlman")
            default:
                break
            }

            for personID in currentMemberIDs {
                let role: PrototypeGroupRole = row.id == "product-circle" && personID == "maya-chen"
                    ? .admin
                    : .member
                members.append(PrototypeGroupMember(personID: personID, role: role))
            }
            if authorID != profileID && !members.contains(where: { $0.personID == authorID }) {
                members.append(PrototypeGroupMember(personID: authorID, role: .member))
            }
        }

        let timeline: [PrototypeTimelineEntry]
        if row.isDraft || row.id == ChatListFixtures.supportChatID {
            timeline = []
        } else if isGroup {
            let membershipEventDate = message.sentAt.addingTimeInterval(60)
            switch row.membershipState {
            case .invited:
                timeline = [.message(message)]
            case .active:
                timeline = [.message(message)]
            case .left:
                timeline = [
                    .message(message),
                    .event(
                        PrototypeTimelineEvent(
                            id: "\(row.id)-membership-left",
                            date: membershipEventDate,
                            kind: .memberLeft(personID: profileID)
                        )
                    ),
                ]
            case .removed:
                timeline = [
                    .message(message),
                    .event(
                        PrototypeTimelineEvent(
                            id: "\(row.id)-membership-removed",
                            date: membershipEventDate,
                            kind: .memberRemoved(actorID: "maya-chen", personID: profileID)
                        )
                    ),
                ]
            }
        } else {
            timeline = [.message(message)]
        }

        return PrototypeChat(
            id: row.id,
            kind: isGroup ? .group : .direct(personID: row.id),
            groupName: row.title,
            groupDescription: isGroup ? groupDescription(for: row.id) : "",
            avatar: row.avatar,
            members: members,
            routing: PrototypeChatRouting(relayURLs: relayURLs),
            timeline: timeline,
            emptyPreview: row.id == ChatListFixtures.supportChatID ? row.preview : "",
            draft: row.isDraft ? row.preview : "",
            replyToMessageID: nil,
            listState: PrototypeChatListState(
                membershipState: row.membershipState,
                isArchived: row.isArchived,
                isPinned: row.isPinned,
                unreadCount: row.unreadCount,
                isMarkedUnread: row.isMarkedUnread,
                muteDuration: row.muteDuration,
                activityDate: seedDate(for: row.timestamp, now: now)
            ),
            disappearingMessageDuration: row.disappearingMessageDuration
        )
    }

    private static func catalogDate(
        daysAgo: Int,
        hour: Int = 12,
        now: Date
    ) -> Date {
        let calendar = Calendar.autoupdatingCurrent
        let shifted = calendar.date(byAdding: .day, value: -daysAgo, to: now) ?? now
        return calendar.date(bySettingHour: hour, minute: 0, second: 0, of: shifted) ?? shifted
    }

    private static func catalogEvent(
        _ id: String,
        date: Date,
        kind: PrototypeChatEventKind
    ) -> PrototypeTimelineEntry {
        .event(PrototypeTimelineEvent(id: id, date: date, kind: kind))
    }

    private static func groupMessageMembers(profileID: String) -> [PrototypeGroupMember] {
        [
            PrototypeGroupMember(personID: profileID, role: .member),
            PrototypeGroupMember(personID: "maya-chen", role: .admin),
            PrototypeGroupMember(personID: "elias-moreno", role: .member),
            PrototypeGroupMember(personID: "nora-bennett", role: .member),
            PrototypeGroupMember(personID: "mina-park", role: .member),
        ]
    }

    private static func identityColorMembers(profileID: String) -> [PrototypeGroupMember] {
        [PrototypeGroupMember(personID: profileID, role: .admin)] + identityColorPeople.map {
            PrototypeGroupMember(personID: $0.id, role: .member)
        }
    }

    private static func groupEventMembers(profileID: String) -> [PrototypeGroupMember] {
        [
            PrototypeGroupMember(personID: profileID, role: .admin),
            PrototypeGroupMember(personID: "maya-chen", role: .member),
            PrototypeGroupMember(personID: "elias-moreno", role: .admin),
            PrototypeGroupMember(personID: "mina-park", role: .member),
        ]
    }

    private static func groupMemberMembers(profileID: String) -> [PrototypeGroupMember] {
        [
            PrototypeGroupMember(personID: profileID, role: .member),
            PrototypeGroupMember(personID: "maya-chen", role: .admin),
            PrototypeGroupMember(personID: "elias-moreno", role: .member),
        ]
    }

    private static func soleAdminMembers(profileID: String) -> [PrototypeGroupMember] {
        [
            PrototypeGroupMember(personID: profileID, role: .admin),
            PrototypeGroupMember(personID: "maya-chen", role: .member),
            PrototypeGroupMember(personID: "elias-moreno", role: .member),
        ]
    }

    private static func endedGroupMembers() -> [PrototypeGroupMember] {
        [
            PrototypeGroupMember(personID: "maya-chen", role: .admin),
            PrototypeGroupMember(personID: "elias-moreno", role: .member),
        ]
    }

    private static func directTextTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-direct-text"
        let calendar = Calendar.autoupdatingCurrent
        let priorYear = calendar.date(byAdding: .year, value: -1, to: now) ?? now.addingTimeInterval(-365 * 86_400)
        let sameYear = catalogDate(daysAgo: 30, hour: 9, now: now)
        let weekday = catalogDate(daysAgo: 4, hour: 10, now: now)
        let yesterday = catalogDate(daysAgo: 1, hour: 11, now: now)
        let today = catalogDate(daysAgo: 0, hour: 8, now: now)

        return [
            .message(PrototypeMessage(id: "TXT-01", authorID: otherID, sentAt: priorYear, text: "TXT-01: Incoming short text")),
            .message(PrototypeMessage(id: "TXT-02", authorID: profileID, sentAt: sameYear, text: "TXT-02: Outgoing short text")),
            .message(PrototypeMessage(id: "TXT-03", authorID: otherID, sentAt: weekday, text: "TXT-03: Cluster start")),
            .message(PrototypeMessage(id: "TXT-04", authorID: otherID, sentAt: weekday.addingTimeInterval(60), text: "TXT-04: Cluster middle")),
            .message(PrototypeMessage(id: "TXT-05", authorID: otherID, sentAt: weekday.addingTimeInterval(120), text: "TXT-05: Cluster end")),
            .message(PrototypeMessage(id: "CLUSTER-01", authorID: profileID, sentAt: weekday.addingTimeInterval(180), text: "CLUSTER-01: Author change starts a new cluster")),
            .message(PrototypeMessage(id: "CLUSTER-02", authorID: profileID, sentAt: weekday.addingTimeInterval(600), text: "CLUSTER-02: More than five minutes starts a new cluster")),
            .message(PrototypeMessage(id: "TXT-06", authorID: otherID, sentAt: yesterday, text: "TXT-06: Multiline text\nSecond line\nThird line")),
            .message(PrototypeMessage(id: "TXT-07", authorID: profileID, sentAt: yesterday.addingTimeInterval(90), text: "TXT-07: Long wrapping text demonstrates how a message bubble grows across several lines while preserving readable padding and alignment at both edges of the conversation.")),
            .message(PrototypeMessage(id: "TXT-08", authorID: otherID, sentAt: yesterday.addingTimeInterval(180), text: "TXT-08: 👋🏽🎉")),
            .message(PrototypeMessage(id: "CLUSTER-03", authorID: otherID, sentAt: today, text: "CLUSTER-03: A new day starts a new cluster")),
            .message(PrototypeMessage(id: "TXT-09", authorID: profileID, sentAt: today.addingTimeInterval(60), text: "TXT-09: **Bold**, *emphasis*, and [White Noise](https://whitenoise.chat)")),
            .message(PrototypeMessage(id: "TXT-10", authorID: otherID, sentAt: today.addingTimeInterval(120), text: "TXT-10: https://developer.apple.com/design/human-interface-guidelines")),
            .message(PrototypeMessage(id: "DLV-01", authorID: profileID, sentAt: today.addingTimeInterval(180), text: "DLV-01: Sending outgoing message", deliveryState: .sending)),
            .message(PrototypeMessage(id: "DLV-02", authorID: profileID, sentAt: today.addingTimeInterval(240), text: "DLV-02: Sent outgoing message")),
            .message(PrototypeMessage(id: "DLV-03", authorID: profileID, sentAt: today.addingTimeInterval(600), text: "DLV-03: Failed outgoing message", deliveryState: .failed)),
        ]
    }

    private static func directDatesTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-direct-dates"
        let old = catalogDate(daysAgo: 220, hour: 9, now: now)
        let recent = catalogDate(daysAgo: 21, hour: 9, now: now)
        let fiveDaysAgo = catalogDate(daysAgo: 5, hour: 10, now: now)
        let fourDaysAgo = catalogDate(daysAgo: 4, hour: 10, now: now)
        let threeDaysAgo = catalogDate(daysAgo: 3, hour: 10, now: now)
        let twoDaysAgo = catalogDate(daysAgo: 2, hour: 10, now: now)
        let yesterday = catalogDate(daysAgo: 1, hour: 10, now: now)
        let today = catalogDate(daysAgo: 0, hour: 8, now: now)

        return [
            .message(PrototypeMessage(id: "DATE-01", authorID: otherID, sentAt: old, text: "DATE-01: Old date includes its year")),
            .message(PrototypeMessage(id: "DATE-02", authorID: profileID, sentAt: recent, text: "DATE-02: Recent date uses weekday, month, and day")),
            .message(PrototypeMessage(id: "DATE-03", authorID: otherID, sentAt: fiveDaysAgo, text: "DATE-03: First sparse one-message day")),
            .message(PrototypeMessage(id: "DATE-04", authorID: profileID, sentAt: fourDaysAgo, text: "DATE-04: Second sparse one-message day")),
            .message(PrototypeMessage(id: "DATE-05", authorID: otherID, sentAt: threeDaysAgo, text: "DATE-05: Third sparse one-message day")),
            .message(PrototypeMessage(id: "DATE-06", authorID: profileID, sentAt: twoDaysAgo, text: "DATE-06: Fourth sparse one-message day")),
            .message(PrototypeMessage(id: "DATE-07", authorID: otherID, sentAt: yesterday, text: "DATE-07: Yesterday remains visible inline")),
            .message(PrototypeMessage(id: "DATE-08", authorID: profileID, sentAt: today, text: "DATE-08: Today begins a long section")),
            .message(PrototypeMessage(id: "DATE-09", authorID: otherID, sentAt: today.addingTimeInterval(60), text: "DATE-09: The inline Today header can scroll away")),
            .message(PrototypeMessage(id: "DATE-10", authorID: profileID, sentAt: today.addingTimeInterval(120), text: "DATE-10: Its pinned pill remains above the transcript")),
            .message(PrototypeMessage(id: "DATE-11", authorID: otherID, sentAt: today.addingTimeInterval(180), text: "DATE-11: More messages make this day span the viewport")),
            .message(PrototypeMessage(id: "DATE-12", authorID: profileID, sentAt: today.addingTimeInterval(240), text: "DATE-12: Scrolling preserves the current day context")),
            .message(PrototypeMessage(id: "DATE-13", authorID: otherID, sentAt: today.addingTimeInterval(300), text: "DATE-13: Long sections keep their day context")),
            .message(PrototypeMessage(id: "DATE-14", authorID: profileID, sentAt: today.addingTimeInterval(360), text: "DATE-14: Date headers remain visible in the transcript")),
            .message(PrototypeMessage(id: "DATE-15", authorID: otherID, sentAt: today.addingTimeInterval(420), text: "DATE-15: Long day keeps its date pinned")),
        ]
    }

    private static func directRepliesTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-direct-replies"
        let start = now.addingTimeInterval(-7_200)
        let photo = PrototypeAttachment.photo(
            id: "RPL-02-photo",
            source: .asset("FiatjafMediaFox"),
            label: "Fox beside a tree"
        )

        return [
            .message(PrototypeMessage(id: "RPL-01-source", authorID: otherID, sentAt: start, text: "RPL-01 source: Incoming text")),
            .message(PrototypeMessage(id: "RPL-01", authorID: profileID, sentAt: start.addingTimeInterval(60), text: "RPL-01: Outgoing reply to incoming text", replyToMessageID: "RPL-01-source")),
            .message(PrototypeMessage(id: "RPL-02-source-caption", authorID: otherID, sentAt: start.addingTimeInterval(240), text: "RPL-02-source → next bubble: Outgoing attachment reply target")),
            .message(PrototypeMessage(id: "RPL-02-source", authorID: profileID, sentAt: start.addingTimeInterval(300), attachments: [photo])),
            .message(PrototypeMessage(id: "RPL-02", authorID: otherID, sentAt: start.addingTimeInterval(360), text: "RPL-02: Incoming reply to outgoing attachment", replyToMessageID: "RPL-02-source")),
            .message(PrototypeMessage(id: "DEL-02-caption", authorID: profileID, sentAt: start.addingTimeInterval(600), text: "DEL-02 → next bubble: Incoming deletion and deleted reply target")),
            .message(PrototypeMessage(id: "DEL-02", authorID: otherID, sentAt: start.addingTimeInterval(660), deletionState: .deletedByOther)),
            .message(PrototypeMessage(id: "RPL-03", authorID: profileID, sentAt: start.addingTimeInterval(720), text: "RPL-03: Reply to deleted target", replyToMessageID: "DEL-02")),
            .message(PrototypeMessage(id: "DEL-01-caption", authorID: otherID, sentAt: start.addingTimeInterval(900), text: "DEL-01 → next bubble: Outgoing deletion")),
            .message(PrototypeMessage(id: "DEL-01", authorID: profileID, sentAt: start.addingTimeInterval(960), deletionState: .deletedByCurrentProfile)),
            .message(PrototypeMessage(id: "RPL-04", authorID: otherID, sentAt: start.addingTimeInterval(1_200), text: "RPL-04: Missing reply target", replyToMessageID: "RPL-missing")),
        ]
    }

    private static func directReactionsTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-direct-reactions"
        let start = now.addingTimeInterval(-9_000)
        let emoji = PrototypeReaction.supportedEmoji
        let actionPhoto = PrototypeAttachment.photo(
            id: "ACT-photo",
            source: .asset("FiatjafMediaBadger"),
            label: "Badger in grass"
        )

        var result: [PrototypeTimelineEntry] = [
            .message(PrototypeMessage(id: "RCT-01", authorID: otherID, sentAt: start, text: "RCT-01: Single reaction from another person", reactions: [.init(emoji: "❤", personIDs: [otherID])])),
            .message(PrototypeMessage(id: "RCT-02", authorID: profileID, sentAt: start.addingTimeInterval(60), text: "RCT-02: You reacted", reactions: [.init(emoji: "😀", personIDs: [profileID])])),
            .message(PrototypeMessage(id: "RCT-03", authorID: otherID, sentAt: start.addingTimeInterval(120), text: "RCT-03: Repeated reaction from three others", reactions: [.init(emoji: "👍", personIDs: [otherID, "maya-chen", "nora-bennett"])])),
            .message(PrototypeMessage(id: "RCT-04", authorID: profileID, sentAt: start.addingTimeInterval(180), text: "RCT-04: Repeated reaction including you", reactions: [.init(emoji: "👎", personIDs: [profileID, otherID, "maya-chen"])])),
            .message(PrototypeMessage(id: "RCT-05", authorID: otherID, sentAt: start.addingTimeInterval(240), text: "RCT-05: Mixed reaction types and participation", reactions: [.init(emoji: "🤣", personIDs: [profileID]), .init(emoji: "🔥", personIDs: [otherID, "maya-chen"]), .init(emoji: "🦫", personIDs: [profileID, otherID, "nora-bennett"])])),
        ]

        for (index, value) in emoji.enumerated() {
            result.append(
                .message(
                    PrototypeMessage(
                        id: "RCT-\(index + 6)",
                        authorID: index.isMultiple(of: 2) ? otherID : profileID,
                        sentAt: start.addingTimeInterval(TimeInterval(360 + index * 60)),
                        text: "RCT-\(index + 6): Supported reaction \(value)",
                        reactions: [.init(emoji: value, personIDs: [otherID])]
                    )
                )
            )
        }

        result.append(
            .message(
                PrototypeMessage(
                    id: "RCT-13",
                    authorID: profileID,
                    sentAt: start.addingTimeInterval(780),
                    text: "RCT-13: Overflow summary",
                    reactions: emoji.enumerated().map { index, value in
                        PrototypeReaction(
                            emoji: value,
                            personIDs: index.isMultiple(of: 2)
                                ? [otherID]
                                : [profileID, otherID]
                        )
                    }
                )
            )
        )

        result += [
            .message(PrototypeMessage(id: "ACT-01", authorID: otherID, sentAt: start.addingTimeInterval(900), text: "ACT-01: Incoming text: React, Reply, Copy, Share")),
            .message(PrototypeMessage(id: "ACT-02", authorID: profileID, sentAt: start.addingTimeInterval(960), text: "ACT-02: Outgoing text: React, Reply, Copy, Share, Delete")),
            .message(PrototypeMessage(id: "ACT-03-caption", authorID: profileID, sentAt: start.addingTimeInterval(1_080), text: "ACT-03 → next bubble: Incoming attachment-only: no Copy or Delete")),
            .message(PrototypeMessage(id: "ACT-03", authorID: otherID, sentAt: start.addingTimeInterval(1_140), attachments: [actionPhoto])),
            .message(PrototypeMessage(id: "ACT-04-caption", authorID: otherID, sentAt: start.addingTimeInterval(1_260), text: "ACT-04 → next bubble: Outgoing attachment-only: Delete, no Copy")),
            .message(PrototypeMessage(id: "ACT-04", authorID: profileID, sentAt: start.addingTimeInterval(1_320), attachments: [actionPhoto])),
            .message(PrototypeMessage(id: "ACT-05-caption", authorID: otherID, sentAt: start.addingTimeInterval(1_440), text: "ACT-05 → next bubble: Available file shares its file URL")),
            .message(PrototypeMessage(id: "ACT-05", authorID: profileID, sentAt: start.addingTimeInterval(1_500), attachments: [bundledFile(id: "ACT-05-file", name: "Project Brief.pdf", resourceName: "ProjectBrief")])),
        ]
        return result
    }

    private static func directNewChatTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        [
            catalogEvent(
                "STATE-01",
                date: now.addingTimeInterval(-300),
                kind: .directChatStarted(actorID: profileID)
            ),
        ]
    }

    private static func configureComposerFixture(
        _ chat: inout PrototypeChat,
        id: String,
        profileID: String,
        now: Date
    ) {
        let scenarioID = composerScenarioID(for: id)
        let otherID: String
        if id == "catalog-composer-mention" {
            otherID = "maya-chen"
            chat.members = [
                PrototypeGroupMember(personID: profileID, role: .member),
                PrototypeGroupMember(personID: "maya-chen", role: .admin),
                PrototypeGroupMember(personID: "elias-moreno", role: .member),
            ]
        } else {
            otherID = id
        }

        chat.timeline = [
            .message(
                PrototypeMessage(
                    id: scenarioID,
                    authorID: otherID,
                    sentAt: now.addingTimeInterval(-300),
                    text: id == "catalog-composer-reply"
                        ? "CMP-REPLY: Would Thursday afternoon work?"
                        : "\(scenarioID): Composer state is ready below"
                )
            ),
        ]
        chat.draftAttachments = []
        chat.suppressedDraftLinkURL = nil

        switch id {
        case "catalog-composer-text":
            chat.draft = "Here’s the updated plan."
        case "catalog-composer-multiline":
            chat.draft = "I pulled together the notes:\n• Confirm the time\n• Share the route\n• Bring a charger"
        case "catalog-composer-link":
            chat.draft = "https://whitenoise.chat"
            chat.suppressedDraftLinkURL = "https://whitenoise.chat"
        case "catalog-composer-link-preview":
            chat.draft = "Worth a look:\nhttps://developer.apple.com/design/human-interface-guidelines"
        case "catalog-composer-photo":
            chat.draft = ""
            chat.draftAttachments = [
                composerPhoto(
                    id: "CMP-PHOTO-attachment",
                    asset: "FiatjafMediaFox",
                    label: "Fox in grass"
                ),
            ]
        case "catalog-composer-photo-album":
            chat.draft = "A few from today."
            chat.draftAttachments = composerAlbum(count: 4)
        case "catalog-composer-mixed-media":
            chat.draft = "Photos and a short clip from the walk."
            chat.draftAttachments = [
                composerPhoto(
                    id: "CMP-MIXED-photo-1",
                    asset: "FiatjafMediaMarmot",
                    label: "Marmot on a rock"
                ),
                .video(
                    id: "CMP-MIXED-video",
                    url: showcaseVideoURL,
                    thumbnail: .asset("AvatarGardenClub"),
                    duration: 8,
                    dimensions: PrototypeMediaDimensions(
                        pixelWidth: 1_920,
                        pixelHeight: 1_080
                    )
                ),
                composerPhoto(
                    id: "CMP-MIXED-photo-2",
                    asset: "FiatjafMediaOstrich",
                    label: "Ostrich in a field"
                ),
            ]
        case "catalog-composer-file":
            chat.draft = "Here’s the brief."
            chat.draftAttachments = [
                bundledFile(
                    id: "CMP-FILE-attachment",
                    name: "Project Brief.pdf",
                    resourceName: "ProjectBrief"
                ),
            ]
        case "catalog-composer-gif":
            chat.draft = ""
            chat.draftAttachments = [
                .gif(
                    id: "CMP-GIF-attachment",
                    assetName: "FiatjafMediaMarmot",
                    label: "Marmot looking around"
                ),
            ]
        case "catalog-composer-contact":
            chat.draft = "Maya can help with this."
            chat.draftAttachments = [
                .contact(id: "CMP-CONTACT-attachment", personID: "maya-chen"),
            ]
        case "catalog-composer-reply":
            chat.draft = "Yes—Thursday afternoon works for me."
            chat.replyToMessageID = scenarioID
        case "catalog-composer-mention":
            chat.draft = "@Maya Chen can you take a look?"
        default:
            break
        }
    }

    private static func composerScenarioID(for chatID: String) -> String {
        switch chatID {
        case "catalog-composer-text": "CMP-TEXT"
        case "catalog-composer-multiline": "CMP-MULTILINE"
        case "catalog-composer-link": "CMP-LINK"
        case "catalog-composer-link-preview": "CMP-LINK-PREVIEW"
        case "catalog-composer-photo": "CMP-PHOTO"
        case "catalog-composer-photo-album": "CMP-PHOTO-ALBUM"
        case "catalog-composer-mixed-media": "CMP-MIXED"
        case "catalog-composer-file": "CMP-FILE"
        case "catalog-composer-gif": "CMP-GIF"
        case "catalog-composer-contact": "CMP-CONTACT"
        case "catalog-composer-reply": "CMP-REPLY"
        case "catalog-composer-mention": "CMP-MENTION"
        default: "CMP"
        }
    }

    private static func composerPhoto(
        id: String,
        asset: String,
        label: String
    ) -> PrototypeAttachment {
        .photo(
            id: id,
            source: .asset(asset),
            label: label,
            dimensions: PrototypeMediaDimensions(
                pixelWidth: 1_200,
                pixelHeight: 800
            )
        )
    }

    private static func composerAlbum(count: Int) -> [PrototypeAttachment] {
        let specs = [
            ("FiatjafMediaMarmot", "Marmot on a rock"),
            ("FiatjafMediaBadger", "Badger in grass"),
            ("FiatjafMediaFox", "Fox in grass"),
            ("FiatjafMediaSloth", "Sloth in a tree"),
        ]
        return specs.prefix(count).enumerated().map { index, spec in
            composerPhoto(
                id: "CMP-PHOTO-ALBUM-\(index + 1)",
                asset: spec.0,
                label: spec.1
            )
        }
    }

    private static func singleMediaTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-media-single"
        let start = now.addingTimeInterval(-14_000)
        func photo(
            _ id: String,
            _ asset: String,
            _ label: String,
            _ width: Double,
            _ height: Double
        ) -> PrototypeAttachment {
            .photo(
                id: id,
                source: .asset(asset),
                label: label,
                dimensions: PrototypeMediaDimensions(pixelWidth: width, pixelHeight: height)
            )
        }

        let landscape = photo("MED-01-photo", "FiatjafMediaFox", "Fox in landscape", 1_200, 800)
        let square = photo("MED-02-photo", "AvatarGardenClub", "Square portrait", 1_200, 1_200)
        let portrait = photo("MED-03-photo", "LegacyAvatarSatoshiNakamoto", "Portrait photograph", 800, 1_200)
        let panorama = photo("MED-SINGLE-04-photo", "FiatjafMediaOstrich", "Clamped panorama", 3_000, 700)
        let tall = photo("MED-SINGLE-05-photo", "QRScannerBackdrop", "Clamped tall photograph", 700, 3_000)
        let lowResolution = photo("MED-SINGLE-06-photo", "LegacyAvatarMarmots", "Low resolution photograph", 96, 64)
        let landscapeVideo = PrototypeAttachment.video(
            id: "MED-11-video-landscape",
            url: showcaseVideoURL,
            thumbnail: .asset("AvatarGardenClub"),
            duration: 8,
            dimensions: PrototypeMediaDimensions(pixelWidth: 1_920, pixelHeight: 1_080)
        )
        let portraitVideo = PrototypeAttachment.video(
            id: "MED-SINGLE-08-video-portrait",
            url: showcaseVideoURL,
            thumbnail: .asset("LegacyAvatarSatoshiNakamoto"),
            duration: 18,
            dimensions: PrototypeMediaDimensions(pixelWidth: 1_080, pixelHeight: 1_920)
        )

        return [
            .message(PrototypeMessage(id: "MED-01", authorID: otherID, sentAt: start, text: "MED-01: Incoming landscape", attachments: [landscape])),
            .message(PrototypeMessage(id: "MED-02", authorID: profileID, sentAt: start.addingTimeInterval(180), text: "MED-02: Outgoing square", attachments: [square])),
            .message(PrototypeMessage(id: "MED-03", authorID: otherID, sentAt: start.addingTimeInterval(360), text: "MED-03: Captioned portrait", attachments: [portrait])),
            .message(PrototypeMessage(id: "MED-SINGLE-04", authorID: profileID, sentAt: start.addingTimeInterval(540), text: "MED-SINGLE-04: Panorama ratio clamped", attachments: [panorama])),
            .message(PrototypeMessage(id: "MED-SINGLE-05", authorID: otherID, sentAt: start.addingTimeInterval(720), text: "MED-SINGLE-05: Tall ratio clamped", attachments: [tall])),
            .message(PrototypeMessage(id: "MED-SINGLE-06", authorID: profileID, sentAt: start.addingTimeInterval(900), text: "MED-SINGLE-06: Low-resolution safeguard", attachments: [lowResolution])),
            .message(PrototypeMessage(id: "MED-11", authorID: otherID, sentAt: start.addingTimeInterval(1_080), text: "MED-11: Landscape video with duration", attachments: [landscapeVideo])),
            .message(PrototypeMessage(id: "MED-SINGLE-08", authorID: profileID, sentAt: start.addingTimeInterval(1_260), text: "MED-SINGLE-08: Portrait video", attachments: [portraitVideo])),
            .message(PrototypeMessage(id: "MED-13", authorID: otherID, sentAt: start.addingTimeInterval(1_440), text: "MED-13: Unavailable photo", attachments: [.photo(id: "MED-13-photo", source: .data(Data([0x00, 0x01, 0x02])), label: "Unavailable photo", dimensions: PrototypeMediaDimensions(pixelWidth: 1_200, pixelHeight: 800))])),
            .message(PrototypeMessage(id: "MED-12", authorID: profileID, sentAt: start.addingTimeInterval(1_620), text: "MED-12: Unavailable video", attachments: [.video(id: "MED-12-video", url: nil, thumbnail: .asset("AvatarWebChristopherCampbell"), duration: 42, dimensions: PrototypeMediaDimensions(pixelWidth: 1_080, pixelHeight: 1_920))])),
        ]
    }

    private static func mediaGalleryTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-media-gallery"
        let start = now.addingTimeInterval(-11_000)
        let specs: [(String, String, Double, Double)] = [
            ("AvatarWebAionyHaust", "Portrait in soft daylight", 800, 1_200),
            ("FiatjafMediaFox", "Fox in landscape", 1_200, 800),
            ("AvatarWebAyoOgunseinde", "Square portrait", 1_000, 1_000),
            ("FiatjafMediaOstrich", "Ostrich in landscape", 1_400, 800),
            ("LegacyAvatarSatoshiNakamoto", "Tall portrait", 700, 1_200),
            ("FiatjafMediaBadger", "Badger in landscape", 1_200, 800),
            ("AvatarWebPhilipMartin", "Portrait in bright light", 900, 1_100),
            ("AvatarMayaChen", "Square portrait outdoors", 1_000, 1_000),
        ]
        let gallery = specs.enumerated().map { index, spec in
            PrototypeAttachment.photo(
                id: "MED-gallery-\(index + 1)",
                source: .asset(spec.0),
                label: spec.1,
                dimensions: PrototypeMediaDimensions(pixelWidth: spec.2, pixelHeight: spec.3)
            )
        }
        let video = PrototypeAttachment.video(
            id: "MED-gallery-video",
            url: showcaseVideoURL,
            thumbnail: .asset("AvatarGardenClub"),
            duration: 8,
            dimensions: PrototypeMediaDimensions(pixelWidth: 1_920, pixelHeight: 1_080)
        )

        return [
            .message(PrototypeMessage(id: "MED-04", authorID: profileID, sentAt: start, text: "MED-04: Gallery of 2", attachments: Array(gallery.prefix(2)))),
            .message(PrototypeMessage(id: "MED-05", authorID: otherID, sentAt: start.addingTimeInterval(180), text: "MED-05: Gallery of 3", attachments: Array(gallery.prefix(3)))),
            .message(PrototypeMessage(id: "MED-06", authorID: profileID, sentAt: start.addingTimeInterval(360), text: "MED-06: Gallery of 4", attachments: Array(gallery.prefix(4)))),
            .message(PrototypeMessage(id: "MED-07", authorID: otherID, sentAt: start.addingTimeInterval(540), text: "MED-07: Captioned gallery of 5", attachments: Array(gallery.prefix(5)))),
            .message(PrototypeMessage(id: "MED-08", authorID: profileID, sentAt: start.addingTimeInterval(720), text: "MED-08: Gallery of 6 with +1", attachments: Array(gallery.prefix(6)))),
            .message(PrototypeMessage(id: "MED-09", authorID: otherID, sentAt: start.addingTimeInterval(900), text: "MED-09: Gallery of 7 with +2", attachments: Array(gallery.prefix(7)))),
            .message(PrototypeMessage(id: "MED-10", authorID: profileID, sentAt: start.addingTimeInterval(1_080), text: "MED-10: Mixed photo and video album", attachments: [gallery[0], video, gallery[2], gallery[3], gallery[4]])),
            .message(PrototypeMessage(id: "MED-GALLERY-08", authorID: otherID, sentAt: start.addingTimeInterval(1_260), text: "MED-GALLERY-08: Larger overflow +3", attachments: gallery)),
        ]
    }

    private static func mediaViewerTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-media-viewer"
        func viewerPhoto(_ id: String, _ asset: String, _ label: String) -> PrototypeAttachment {
            .photo(
                id: id,
                source: .asset(asset),
                label: label,
                dimensions: PrototypeMediaDimensions(pixelWidth: 1_200, pixelHeight: 800)
            )
        }
        let video = PrototypeAttachment.video(
            id: "MED-VIEW-04-video",
            url: showcaseVideoURL,
            thumbnail: .asset("AvatarGardenClub"),
            duration: 8,
            dimensions: PrototypeMediaDimensions(pixelWidth: 1_920, pixelHeight: 1_080)
        )
        let dayOne = catalogDate(daysAgo: 3, hour: 9, now: now)
        let dayTwo = catalogDate(daysAgo: 1, hour: 14, now: now)
        let today = catalogDate(daysAgo: 0, hour: 10, now: now)
        return [
            .message(PrototypeMessage(id: "MED-VIEW-01", authorID: otherID, sentAt: dayOne, text: "MED-VIEW-01: Paging starts across dates", attachments: [viewerPhoto("MED-VIEW-01-photo", "FiatjafMediaFox", "Fox in grass")])),
            .message(PrototypeMessage(id: "MED-VIEW-02", authorID: profileID, sentAt: dayOne.addingTimeInterval(120), text: "MED-VIEW-02: Zoom and Share", attachments: [viewerPhoto("MED-VIEW-02-photo", "FiatjafMediaOstrich", "Ostrich portrait")])),
            .message(PrototypeMessage(id: "MED-VIEW-03", authorID: otherID, sentAt: dayTwo, text: "MED-VIEW-03: Save and Forward", attachments: [viewerPhoto("MED-VIEW-03-photo", "FiatjafMediaBadger", "Badger in grass")])),
            .message(PrototypeMessage(id: "MED-VIEW-04", authorID: profileID, sentAt: dayTwo.addingTimeInterval(120), text: "MED-VIEW-04: Initial video autoplays", attachments: [video])),
            .message(PrototypeMessage(id: "MED-VIEW-05", authorID: otherID, sentAt: today, text: "MED-VIEW-05: Go to Message", attachments: [viewerPhoto("MED-VIEW-05-photo", "FiatjafMediaMarmot", "Marmot in landscape")])),
            .message(PrototypeMessage(id: "MED-VIEW-06", authorID: profileID, sentAt: today.addingTimeInterval(120), text: "MED-VIEW-06: Deleted media excluded", attachments: [viewerPhoto("MED-VIEW-06-photo", "AvatarMayaChen", "Deleted portrait")], deletionState: .deletedByCurrentProfile)),
            .message(PrototypeMessage(id: "MED-VIEW-07", authorID: otherID, sentAt: today.addingTimeInterval(240), text: "MED-VIEW-07: Unavailable media excluded", attachments: [.photo(id: "MED-VIEW-07-photo", source: .data(Data([0x00])), label: "Unavailable media", dimensions: PrototypeMediaDimensions(pixelWidth: 1_200, pixelHeight: 800))])),
        ]
    }

    private static func richContentTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-media-rich"
        let start = now.addingTimeInterval(-15_000)
        let availableFiles = [
            bundledFile(id: "FILE-01-file", name: "Project Brief.pdf", resourceName: "ProjectBrief"),
            bundledFile(id: "FILE-02-file", name: "Review Notes.docx", resourceName: "ProjectNotes"),
            bundledFile(id: "FILE-03-file", name: "Budget.xlsx", resourceName: "WeekendNotes"),
            bundledFile(id: "FILE-04-file", name: "Assets.zip", resourceName: "TrailPlan"),
            bundledFile(id: "FILE-05-file", name: "Read Me.txt", resourceName: "ProjectBrief"),
        ]

        return [
            .message(PrototypeMessage(id: "FILE-01", authorID: otherID, sentAt: start, text: "FILE-01: Available PDF", attachments: [availableFiles[0]])),
            .message(PrototypeMessage(id: "FILE-02", authorID: profileID, sentAt: start.addingTimeInterval(120), text: "FILE-02: Available DOCX", attachments: [availableFiles[1]])),
            .message(PrototypeMessage(id: "FILE-03", authorID: otherID, sentAt: start.addingTimeInterval(240), text: "FILE-03: Available XLSX", attachments: [availableFiles[2]])),
            .message(PrototypeMessage(id: "FILE-04", authorID: profileID, sentAt: start.addingTimeInterval(360), text: "FILE-04: Available ZIP", attachments: [availableFiles[3]])),
            .message(PrototypeMessage(id: "FILE-05", authorID: otherID, sentAt: start.addingTimeInterval(480), text: "FILE-05: Available TXT", attachments: [availableFiles[4]])),
            .message(PrototypeMessage(id: "FILE-06", authorID: profileID, sentAt: start.addingTimeInterval(600), text: "FILE-06: Unavailable file", attachments: [.file(id: "FILE-06-file", name: "Unavailable.pdf", size: 240_000, url: nil)])),
            .message(PrototypeMessage(id: "LINK-01", authorID: otherID, sentAt: start.addingTimeInterval(780), text: "LINK-01: Link preview with image", attachments: [.link(id: "LINK-01-link", title: "Human Interface Guidelines", domain: "developer.apple.com", summary: "Guidance for designing clear experiences on Apple platforms.", image: .asset("ProfileAvatarOpenCircuit"))])),
            .message(PrototypeMessage(id: "LINK-02", authorID: profileID, sentAt: start.addingTimeInterval(900), text: "LINK-02: Link preview without image", attachments: [.link(id: "LINK-02-link", title: "White Noise", domain: "whitenoise.chat", summary: "Private, resilient conversations.", image: nil)])),
            .message(PrototypeMessage(id: "LINK-03", authorID: otherID, sentAt: start.addingTimeInterval(1_020), text: "LINK-03: Invalid destination", attachments: [.link(id: "LINK-03-link", title: "Unavailable preview", domain: "", summary: "This destination cannot be opened.", image: nil)])),
            .message(PrototypeMessage(id: "RICH-01", authorID: profileID, sentAt: start.addingTimeInterval(1_200), text: "RICH-01: GIF", attachments: [.gif(id: "RICH-01-gif", assetName: "FiatjafMediaMarmot", label: "Marmot looking around")])),
            .message(PrototypeMessage(id: "RICH-05", authorID: otherID, sentAt: start.addingTimeInterval(1_740), text: "RICH-05: Valid contact", attachments: [.contact(id: "RICH-05-contact", personID: "avery-stone")]))
        ]
    }

    private static func voiceTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let otherID = "catalog-voice"
        let start = now.addingTimeInterval(-3_600)
        return [
            .message(PrototypeMessage(id: "VOICE-01-caption", authorID: profileID, sentAt: start, text: "VOICE-01 → next bubble: Incoming short voice message")),
            .message(PrototypeMessage(id: "VOICE-01", authorID: otherID, sentAt: start.addingTimeInterval(60), attachments: [.voice(id: "VOICE-01-audio", resourceName: PrototypeVoiceSample.resourceName, duration: 7)])),
            .message(PrototypeMessage(id: "VOICE-02-caption", authorID: otherID, sentAt: start.addingTimeInterval(180), text: "VOICE-02 → next bubble: Outgoing short voice message")),
            .message(PrototypeMessage(id: "VOICE-02", authorID: profileID, sentAt: start.addingTimeInterval(240), attachments: [.voice(id: "VOICE-02-audio", resourceName: PrototypeVoiceSample.resourceName, duration: 18)])),
            .message(PrototypeMessage(id: "VOICE-03-caption", authorID: profileID, sentAt: start.addingTimeInterval(360), text: "VOICE-03 → next bubble: Voice duration over one minute")),
            .message(PrototypeMessage(id: "VOICE-03", authorID: otherID, sentAt: start.addingTimeInterval(420), attachments: [.voice(id: "VOICE-03-audio", resourceName: PrototypeVoiceSample.resourceName, duration: 82)])),
            .message(PrototypeMessage(id: "VOICE-04-caption", authorID: otherID, sentAt: start.addingTimeInterval(540), text: "VOICE-04 → next bubble: Outgoing voice and text message")),
            .message(PrototypeMessage(id: "VOICE-04", authorID: profileID, sentAt: start.addingTimeInterval(600), text: PrototypeVoiceSample.transcript, attachments: [.voice(id: "VOICE-04-audio", resourceName: PrototypeVoiceSample.resourceName, duration: 12)])),
        ]
    }

    private static func groupMessagesTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let old = catalogDate(daysAgo: 9, hour: 9, now: now)
        let yesterday = catalogDate(daysAgo: 1, hour: 10, now: now)
        let today = catalogDate(daysAgo: 0, hour: 9, now: now)
        return [
            catalogEvent("EVT-02", date: old, kind: .groupCreated(actorID: "maya-chen")),
            catalogEvent("EVT-06", date: old.addingTimeInterval(60), kind: .memberJoined(personID: profileID)),
            .message(PrototypeMessage(id: "GRP-01", authorID: "maya-chen", sentAt: old.addingTimeInterval(180), text: "GRP-01: Incoming group cluster start")),
            .message(PrototypeMessage(id: "GRP-02", authorID: "maya-chen", sentAt: old.addingTimeInterval(240), text: "GRP-02: Same-author cluster end")),
            .message(PrototypeMessage(id: "GRP-03", authorID: "elias-moreno", sentAt: old.addingTimeInterval(300), text: "GRP-03: Author switch")),
            .message(PrototypeMessage(id: "GRP-04", authorID: profileID, sentAt: old.addingTimeInterval(360), text: "GRP-04: Outgoing interruption")),
            .message(PrototypeMessage(id: "GRP-05", authorID: "elias-moreno", sentAt: old.addingTimeInterval(720), text: "GRP-05: Five-minute cluster break")),
            .message(PrototypeMessage(id: "MENTION-01", authorID: "maya-chen", sentAt: yesterday, text: "MENTION-01: @Marmota please review this.")),
            .message(PrototypeMessage(id: "MENTION-02", authorID: profileID, sentAt: yesterday.addingTimeInterval(120), text: "MENTION-02: @Maya Chen has the latest version.")),
            .message(PrototypeMessage(id: "MENTION-03", authorID: "nora-bennett", sentAt: yesterday.addingTimeInterval(240), text: "MENTION-03: @Maya Chen and @Elias Moreno can compare notes.")),
            .message(PrototypeMessage(id: "MENTION-04", authorID: "elias-moreno", sentAt: yesterday.addingTimeInterval(360), text: "MENTION-04: @Unknown stays plain text.")),
            .message(PrototypeMessage(id: "GRP-RPL-01-source", authorID: "maya-chen", sentAt: today, text: "GRP-RPL-01 source: Maya’s question")),
            .message(PrototypeMessage(id: "GRP-RPL-01", authorID: "elias-moreno", sentAt: today.addingTimeInterval(60), text: "GRP-RPL-01: Elias replies to Maya", replyToMessageID: "GRP-RPL-01-source")),
            .message(PrototypeMessage(id: "GRP-RPL-02", authorID: "maya-chen", sentAt: today.addingTimeInterval(120), text: "GRP-RPL-02: Maya replies to Elias", replyToMessageID: "GRP-RPL-01")),
        ]
    }

    private static func identityColorTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = catalogDate(daysAgo: 0, hour: 9, now: now)
        return [
            catalogEvent("COLOR-EVT-01", date: start, kind: .groupCreated(actorID: profileID)),
            .message(PrototypeMessage(id: "COLOR-01", authorID: "identity-color-1", sentAt: start.addingTimeInterval(120), text: "COLOR-01: Red identity color")),
            .message(PrototypeMessage(id: "COLOR-02", authorID: "identity-color-7", sentAt: start.addingTimeInterval(180), text: "COLOR-02: Orange identity color")),
            .message(PrototypeMessage(id: "COLOR-03", authorID: "identity-color-13", sentAt: start.addingTimeInterval(240), text: "COLOR-03: Green identity color")),
            .message(PrototypeMessage(id: "COLOR-04", authorID: "identity-color-12", sentAt: start.addingTimeInterval(300), text: "COLOR-04: Teal identity color")),
            .message(PrototypeMessage(id: "COLOR-05", authorID: "identity-color-0", sentAt: start.addingTimeInterval(360), text: "COLOR-05: Blue identity color")),
            .message(PrototypeMessage(id: "COLOR-06", authorID: "identity-color-2", sentAt: start.addingTimeInterval(420), text: "COLOR-06: Indigo identity color")),
            .message(PrototypeMessage(id: "COLOR-07", authorID: "identity-color-11", sentAt: start.addingTimeInterval(480), text: "COLOR-07: Purple identity color")),
            .message(PrototypeMessage(id: "COLOR-08", authorID: "identity-color-5", sentAt: start.addingTimeInterval(540), text: "COLOR-08: Pink identity color")),
            .message(PrototypeMessage(id: "COLOR-09", authorID: "identity-color-22", sentAt: start.addingTimeInterval(600), text: "COLOR-09: Brown identity color")),
        ]
    }

    private static func groupEventsTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-40_000)
        let minute: TimeInterval = 60
        return [
            catalogEvent("EVT-01", date: start, kind: .groupCreated(actorID: profileID)),
            catalogEvent("EVT-03", date: start.addingTimeInterval(minute), kind: .membersAdded(actorID: profileID, personIDs: ["maya-chen"])),
            catalogEvent("EVT-11", date: start.addingTimeInterval(2 * minute), kind: .adminGranted(actorID: profileID, personID: "maya-chen")),
            catalogEvent("EVT-04", date: start.addingTimeInterval(3 * minute), kind: .membersAdded(actorID: "maya-chen", personIDs: ["elias-moreno", "nora-bennett", "leo-martins", "mina-park"])),
            catalogEvent("EVT-05", date: start.addingTimeInterval(4 * minute), kind: .memberJoined(personID: "theo-grant")),
            catalogEvent("EVT-07", date: start.addingTimeInterval(5 * minute), kind: .memberLeft(personID: "leo-martins")),
            catalogEvent("EVT-08", date: start.addingTimeInterval(6 * minute), kind: .memberRemoved(actorID: profileID, personID: "nora-bennett")),
            catalogEvent("EVT-09", date: start.addingTimeInterval(7 * minute), kind: .memberRemoved(actorID: "maya-chen", personID: "theo-grant")),
            catalogEvent("EVT-14", date: start.addingTimeInterval(8 * minute), kind: .adminRevoked(actorID: "maya-chen", personID: profileID)),
            catalogEvent("EVT-12", date: start.addingTimeInterval(9 * minute), kind: .adminGranted(actorID: "maya-chen", personID: profileID)),
            catalogEvent("EVT-13", date: start.addingTimeInterval(10 * minute), kind: .adminRevoked(actorID: profileID, personID: "maya-chen")),
            catalogEvent("EVT-12B", date: start.addingTimeInterval(11 * minute), kind: .adminGranted(actorID: profileID, personID: "elias-moreno")),
            catalogEvent("EVT-15", date: start.addingTimeInterval(12 * minute), kind: .groupNameChanged(actorID: profileID, name: "Group - Events & Roles")),
            catalogEvent("EVT-16", date: start.addingTimeInterval(13 * minute), kind: .groupPhotoChanged(actorID: "elias-moreno")),
            catalogEvent("EVT-17", date: start.addingTimeInterval(14 * minute), kind: .groupPhotoRemoved(actorID: profileID)),
            catalogEvent("EVT-18", date: start.addingTimeInterval(15 * minute), kind: .groupDescriptionChanged(actorID: "elias-moreno")),
            catalogEvent("EVT-19", date: start.addingTimeInterval(16 * minute), kind: .groupDescriptionRemoved(actorID: profileID)),
            .message(PrototypeMessage(id: "ROLE-01", authorID: profileID, sentAt: start.addingTimeInterval(990), text: "ROLE-01: Admin: edit group identity, add people, manage roles, remove members, and leave.")),
            catalogEvent("EVT-20", date: start.addingTimeInterval(17 * minute), kind: .disappearingMessagesChanged(actorID: profileID, duration: .oneDay)),
            catalogEvent("EVT-21", date: start.addingTimeInterval(18 * minute), kind: .disappearingMessagesChanged(actorID: "elias-moreno", duration: .oneWeek)),
            catalogEvent("EVT-22", date: start.addingTimeInterval(19 * minute), kind: .disappearingMessagesChanged(actorID: profileID, duration: .fourWeeks)),
            catalogEvent("EVT-23", date: start.addingTimeInterval(20 * minute), kind: .disappearingMessagesChanged(actorID: "elias-moreno", duration: .off)),
        ]
    }

    private static func groupMemberTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-4_000)
        return [
            catalogEvent("EVT-02-member", date: start, kind: .groupCreated(actorID: "maya-chen")),
            catalogEvent("EVT-04-member", date: start.addingTimeInterval(60), kind: .membersAdded(actorID: "maya-chen", personIDs: [profileID, "elias-moreno"])),
            .message(PrototypeMessage(id: "ROLE-02", authorID: "maya-chen", sentAt: start.addingTimeInterval(180), text: "ROLE-02: Ordinary member: messaging, search, shared content, mute, archive, and leave remain available; admin controls are hidden.")),
        ]
    }

    private static func soleAdminTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-3_000)
        return [
            catalogEvent("ROLE-03-created", date: start, kind: .groupCreated(actorID: profileID)),
            catalogEvent("ROLE-03-added", date: start.addingTimeInterval(60), kind: .membersAdded(actorID: profileID, personIDs: ["maya-chen", "elias-moreno"])),
            .message(PrototypeMessage(id: "ROLE-03", authorID: profileID, sentAt: start.addingTimeInterval(180), text: "ROLE-03: Sole admin: promote another member before leaving the group.")),
        ]
    }

    private static func directLeftTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-3_600)
        return [
            catalogEvent("STATE-02-started", date: start, kind: .directChatStarted(actorID: profileID)),
            .message(PrototypeMessage(id: "STATE-02-message", authorID: "catalog-direct-left", sentAt: start.addingTimeInterval(120), text: "STATE-02: Direct history remains readable after leaving.")),
            catalogEvent("STATE-02", date: start.addingTimeInterval(240), kind: .directChatLeft),
        ]
    }

    private static func directInvitationTimeline(now: Date) -> [PrototypeTimelineEntry] {
        [
            .message(
                PrototypeMessage(
                    id: "STATE-09",
                    authorID: "avery-stone",
                    sentAt: now.addingTimeInterval(-900),
                    text: "STATE-09: Are you free for a quick call tomorrow?"
                )
            ),
        ]
    }

    private static func groupInvitationMembers() -> [PrototypeGroupMember] {
        [
            PrototypeGroupMember(personID: "maya-chen", role: .admin),
            PrototypeGroupMember(personID: "elias-moreno", role: .member),
            PrototypeGroupMember(personID: "nora-bennett", role: .member),
        ]
    }

    private static func groupInvitationTimeline(now: Date) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-1_800)
        return [
            catalogEvent(
                "STATE-10-created",
                date: start,
                kind: .groupCreated(actorID: "maya-chen")
            ),
            .message(
                PrototypeMessage(
                    id: "STATE-10A",
                    authorID: "maya-chen",
                    sentAt: start.addingTimeInterval(120),
                    text: "STATE-10A: We’re meeting at the west trailhead at 9."
                )
            ),
            .message(
                PrototypeMessage(
                    id: "STATE-10B",
                    authorID: "elias-moreno",
                    sentAt: start.addingTimeInterval(240),
                    text: "STATE-10B: Bring water and a light jacket."
                )
            ),
        ]
    }

    private static func groupLeftTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-3_600)
        return [
            catalogEvent("STATE-03-created", date: start, kind: .groupCreated(actorID: "maya-chen")),
            catalogEvent("STATE-03-added", date: start.addingTimeInterval(60), kind: .membersAdded(actorID: "maya-chen", personIDs: [profileID, "elias-moreno"])),
            .message(PrototypeMessage(id: "STATE-03-message", authorID: "maya-chen", sentAt: start.addingTimeInterval(180), text: "STATE-03: Group history remains readable after leaving.")),
            catalogEvent("STATE-03", date: start.addingTimeInterval(300), kind: .memberLeft(personID: profileID)),
        ]
    }

    private static func groupRemovedTimeline(
        profileID: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-3_600)
        return [
            catalogEvent("STATE-04-created", date: start, kind: .groupCreated(actorID: "maya-chen")),
            catalogEvent("STATE-04-added", date: start.addingTimeInterval(60), kind: .membersAdded(actorID: "maya-chen", personIDs: [profileID, "elias-moreno"])),
            .message(PrototypeMessage(id: "STATE-04-message", authorID: "maya-chen", sentAt: start.addingTimeInterval(180), text: "STATE-04: Group history remains readable after removal.")),
            catalogEvent("EVT-10", date: start.addingTimeInterval(300), kind: .memberRemoved(actorID: "maya-chen", personID: profileID)),
        ]
    }

    private static func recoveryTimeline(
        profileID: String,
        otherID: String,
        scenarioID: String,
        label: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-1_800)
        return [
            catalogEvent("\(scenarioID)-started", date: start, kind: .directChatStarted(actorID: profileID)),
            .message(PrototypeMessage(id: scenarioID, authorID: otherID, sentAt: start.addingTimeInterval(120), text: label)),
        ]
    }

    private static func indicatorTimeline(
        scenarioID: String,
        authorID: String,
        label: String,
        now: Date
    ) -> [PrototypeTimelineEntry] {
        [
            .message(
                PrototypeMessage(
                    id: scenarioID,
                    authorID: authorID,
                    sentAt: now.addingTimeInterval(-1_800),
                    text: label
                )
            ),
        ]
    }

    private static func mayaTimeline(profileID: String, now: Date) -> [PrototypeTimelineEntry] {
        let inception = now.addingTimeInterval(-45 * 86_400)
        let mediaDay = now.addingTimeInterval(-12 * 86_400)
        let yesterday = now.addingTimeInterval(-86_400)
        let today = now.addingTimeInterval(-3_600)
        let photo = PrototypeAttachment.photo(
            id: "maya-photo-one",
            source: .asset("AvatarWebAionyHaust"),
            label: "Portrait in soft daylight"
        )
        let video = PrototypeAttachment.video(
            id: "maya-video",
            url: showcaseVideoURL,
            thumbnail: .asset("AvatarGardenClub"),
            duration: 8
        )

        return [
            .message(PrototypeMessage(id: "maya-1", authorID: "maya-chen", sentAt: inception, text: "Hi—Maya here.")),
            .message(PrototypeMessage(id: "maya-2", authorID: profileID, sentAt: inception.addingTimeInterval(60), text: "Hi Maya. Good to meet you.")),
            .message(PrototypeMessage(id: "maya-3", authorID: "maya-chen", sentAt: inception.addingTimeInterval(150), text: "I pulled the first draft together.\nCould you look at the opening and the final paragraph?")),
            .message(PrototypeMessage(id: "maya-3b", authorID: profileID, sentAt: inception.addingTimeInterval(240), text: "Yes. I’ll read both before lunch.", replyToMessageID: "maya-3")),
            .message(PrototypeMessage(id: "maya-3c", authorID: profileID, sentAt: inception.addingTimeInterval(300), text: "I’ll keep the opening intact and tighten the middle.\nThe ending can be direct enough to read quickly without losing the context that makes the recommendation useful.")),
            .message(PrototypeMessage(id: "maya-3d", authorID: "maya-chen", sentAt: inception.addingTimeInterval(390), text: "That sounds right. The full background matters, but the main idea should still be obvious to someone who only has a minute to read it.")),
            .message(PrototypeMessage(id: "maya-3e", authorID: "maya-chen", sentAt: inception.addingTimeInterval(450), text: "I marked the sentence I’d keep.")),
            .message(PrototypeMessage(id: "maya-3f", authorID: profileID, sentAt: inception.addingTimeInterval(540), text: "I agree. **The shorter version works better.** I’m using this as the reference: https://whitenoise.chat")),

            .message(PrototypeMessage(id: "maya-4", authorID: profileID, sentAt: mediaDay, text: "This is the latest photo.", attachments: [photo], reactions: [PrototypeReaction(emoji: "❤", personIDs: ["maya-chen"])])),
            .message(PrototypeMessage(id: "maya-5", authorID: "maya-chen", sentAt: mediaDay.addingTimeInterval(120), text: "The crop looks great.", replyToMessageID: "maya-4")),
            .message(PrototypeMessage(id: "maya-6", authorID: "maya-chen", sentAt: mediaDay.addingTimeInterval(210), attachments: [photo])),
            .message(PrototypeMessage(id: "maya-7", authorID: profileID, sentAt: mediaDay.addingTimeInterval(330), text: "These two could sit together.", attachments: [
                .photo(id: "maya-two-a", source: .asset("AvatarWebAyoOgunseinde"), label: "Portrait against a dark background"),
                .photo(id: "maya-two-b", source: .asset("AvatarGardenClub"), label: "Green leaves in sunlight"),
            ])),
            .message(PrototypeMessage(id: "maya-8", authorID: "maya-chen", sentAt: mediaDay.addingTimeInterval(450), text: "And these tell the full sequence.", attachments: [
                .photo(id: "maya-three-a", source: .asset("AvatarWebIanDooley"), label: "Portrait outdoors"),
                .photo(id: "maya-three-b", source: .asset("AvatarWebSergioDePaula"), label: "Portrait in warm light"),
                .photo(id: "maya-three-c", source: .asset("AvatarWebVinceFleming"), label: "Portrait near a window"),
            ])),
            .message(PrototypeMessage(id: "maya-9", authorID: "maya-chen", sentAt: mediaDay.addingTimeInterval(570), text: "Here’s the short clip.", attachments: [video])),
            .message(PrototypeMessage(id: "maya-9b", authorID: profileID, sentAt: mediaDay.addingTimeInterval(660), text: "That movement makes the sequence much clearer.", replyToMessageID: "maya-9")),
            .message(PrototypeMessage(id: "maya-9c", authorID: profileID, sentAt: mediaDay.addingTimeInterval(750), text: "This pairing is probably the strongest.", attachments: [photo, video])),

            .message(PrototypeMessage(id: "maya-10", authorID: "maya-chen", sentAt: yesterday, text: "Here are the notes from our last pass.", attachments: [bundledFile(id: "maya-file", name: "Weekend Notes.pdf", resourceName: "WeekendNotes")])),
            .message(PrototypeMessage(id: "maya-10b", authorID: profileID, sentAt: yesterday.addingTimeInterval(90), text: "I added the editable outline.", attachments: [bundledFile(id: "maya-docx", name: "Conversation Outline.docx", resourceName: "ProjectBrief")])),
            .message(PrototypeMessage(id: "maya-10c", authorID: "maya-chen", sentAt: yesterday.addingTimeInterval(180), text: "Here’s the planning sheet.", attachments: [bundledFile(id: "maya-xlsx", name: "Launch Checklist.xlsx", resourceName: "ProjectNotes")])),
            .message(PrototypeMessage(id: "maya-10d", authorID: profileID, sentAt: yesterday.addingTimeInterval(270), text: "These are the supporting files.", attachments: [bundledFile(id: "maya-zip", name: "Reference Images.zip", resourceName: "TrailPlan")])),
            .message(PrototypeMessage(id: "maya-10e", authorID: "maya-chen", sentAt: yesterday.addingTimeInterval(360), attachments: [bundledFile(id: "maya-txt", name: "Review Notes.txt", resourceName: "WeekendNotes")])),
            .message(PrototypeMessage(id: "maya-11", authorID: profileID, sentAt: yesterday.addingTimeInterval(450), attachments: [.voice(id: "maya-voice", resourceName: PrototypeVoiceSample.resourceName, duration: PrototypeVoiceSample.duration)])),
            .message(PrototypeMessage(id: "maya-12", authorID: "maya-chen", sentAt: yesterday.addingTimeInterval(540), text: "This is the reference I mentioned.", attachments: [.link(id: "maya-link", title: "White Noise", domain: "whitenoise.chat", summary: "Private, resilient messaging for people and groups.", image: .asset("WhiteNoiseMark"))])),
            .message(PrototypeMessage(id: "maya-12b", authorID: profileID, sentAt: yesterday.addingTimeInterval(630), text: "Apple’s design guidance is useful here.", attachments: [.link(id: "maya-link-apple", title: "Human Interface Guidelines", domain: "developer.apple.com", summary: "Guidance for designing clear, consistent experiences across Apple platforms.", image: .asset("ProfileAvatarOpenCircuit"))])),
            .message(PrototypeMessage(id: "maya-12c", authorID: "maya-chen", sentAt: yesterday.addingTimeInterval(720), text: "I also saved the messaging reference.", attachments: [.link(id: "maya-link-signal", title: "Signal Support", domain: "support.signal.org", summary: "Help and guidance for private messaging features.", image: .asset("ProfileAvatarFreeSignal"))])),
            .message(PrototypeMessage(id: "maya-12d", authorID: profileID, sentAt: yesterday.addingTimeInterval(810), attachments: [.link(id: "maya-link-github", title: "GitHub", domain: "github.com", summary: "Code, issues, and project collaboration in one place.", image: .asset("ProfileAvatarOpenQuill"))])),

            .message(PrototypeMessage(id: "maya-13", authorID: "maya-chen", sentAt: today, deletionState: .deletedByOther)),
            .message(PrototypeMessage(id: "maya-14", authorID: profileID, sentAt: today.addingTimeInterval(120), deletionState: .deletedByCurrentProfile)),
            .message(PrototypeMessage(id: "maya-15", authorID: profileID, sentAt: today.addingTimeInterval(240), text: "Replying to a message that’s no longer available.", replyToMessageID: "maya-13")),
            .message(PrototypeMessage(id: "maya-16", authorID: profileID, sentAt: today.addingTimeInterval(360), text: "I’ll send the revised version now.", deliveryState: .failed)),
            .message(PrototypeMessage(id: "maya-17", authorID: "maya-chen", sentAt: today.addingTimeInterval(480), text: "Can you send the latest version when you have a moment?", reactions: [
                PrototypeReaction(emoji: "👍", personIDs: [profileID]),
                PrototypeReaction(emoji: "😀", personIDs: [profileID, "maya-chen"]),
            ])),
        ]
    }

    private static func weekendTimeline(profileID: String, now: Date) -> [PrototypeTimelineEntry] {
        let old = now.addingTimeInterval(-400 * 86_400)
        let earlyHistory = now.addingTimeInterval(-120 * 86_400)
        let weekday = now.addingTimeInterval(-4 * 86_400)
        let yesterday = now.addingTimeInterval(-86_400)
        let today = now.addingTimeInterval(-7_200)
        func image(_ id: String, _ asset: String, _ label: String) -> PrototypeAttachment {
            .photo(id: id, source: .asset(asset), label: label)
        }

        let gallery = [
            image("week-1", "AvatarGardenClub", "Green leaves in sunlight"),
            image("week-2", "AvatarWebAionyHaust", "Portrait in soft daylight"),
            image("week-3", "AvatarWebAyoOgunseinde", "Portrait against a dark background"),
            image("week-4", "AvatarWebIanDooley", "Portrait outdoors"),
            image("week-5", "AvatarWebSergioDePaula", "Portrait in warm light"),
            image("week-6", "AvatarWebVinceFleming", "Portrait near a window"),
            image("week-7", "AvatarWebPhilipMartin", "Portrait with a bright background"),
        ]

        return [
            .event(PrototypeTimelineEvent(id: "week-event-created", date: old, kind: .groupCreated(actorID: profileID))),
            .event(PrototypeTimelineEvent(id: "week-event-added", date: old.addingTimeInterval(60), kind: .membersAdded(actorID: profileID, personIDs: ["maya-chen", "elias-moreno"]))),
            .message(PrototypeMessage(id: "week-msg-1", authorID: "maya-chen", sentAt: old.addingTimeInterval(180), text: "Thanks for setting this up.")),
            .message(PrototypeMessage(id: "week-msg-2", authorID: "elias-moreno", sentAt: old.addingTimeInterval(240), text: "I have a few easy routes we can try.")),
            .event(PrototypeTimelineEvent(id: "week-event-added-one", date: old.addingTimeInterval(360), kind: .membersAdded(actorID: profileID, personIDs: ["nora-bennett"]))),
            .message(PrototypeMessage(id: "week-msg-3", authorID: "nora-bennett", sentAt: old.addingTimeInterval(480), text: "Welcome everyone. Let’s choose a route that works for the whole group.")),
            .event(PrototypeTimelineEvent(id: "week-event-joined", date: old.addingTimeInterval(600), kind: .memberJoined(personID: "mina-park"))),
            .message(PrototypeMessage(id: "week-msg-4", authorID: "mina-park", sentAt: old.addingTimeInterval(720), text: "Glad I found the group.")),
            .message(PrototypeMessage(id: "week-msg-5", authorID: "mina-park", sentAt: old.addingTimeInterval(780), text: "Sunday mornings usually work for me.")),
            .event(PrototypeTimelineEvent(id: "week-event-leo-joined", date: old.addingTimeInterval(900), kind: .memberJoined(personID: "leo-martins"))),

            .event(PrototypeTimelineEvent(id: "week-event-name", date: earlyHistory, kind: .groupNameChanged(actorID: profileID, name: "Weekend Walks"))),
            .message(PrototypeMessage(id: "week-msg-6", authorID: "nora-bennett", sentAt: earlyHistory.addingTimeInterval(120), text: "Weekend Walks fits us better.")),
            .event(PrototypeTimelineEvent(id: "week-event-photo", date: earlyHistory.addingTimeInterval(240), kind: .groupPhotoChanged(actorID: profileID))),
            .message(PrototypeMessage(id: "week-msg-7", authorID: "maya-chen", sentAt: earlyHistory.addingTimeInterval(360), text: "That photo is from our first riverside route.")),
            .message(PrototypeMessage(id: "week-msg-8", authorID: "maya-chen", sentAt: earlyHistory.addingTimeInterval(420), text: "I still like that path best.")),
            .event(PrototypeTimelineEvent(id: "week-event-description", date: earlyHistory.addingTimeInterval(540), kind: .groupDescriptionChanged(actorID: profileID))),

            .event(PrototypeTimelineEvent(id: "week-event-admin", date: weekday, kind: .adminGranted(actorID: profileID, personID: "maya-chen"))),
            .message(PrototypeMessage(id: "week-msg-9", authorID: "maya-chen", sentAt: weekday.addingTimeInterval(120), text: "I’ll organize the route options and meeting points.")),
            .message(PrototypeMessage(id: "week-msg-10", authorID: "elias-moreno", sentAt: weekday.addingTimeInterval(240), text: "Here are four from the west trail.", attachments: Array(gallery.prefix(4)), reactions: [PrototypeReaction(emoji: "👍", personIDs: [profileID])])),
            .message(PrototypeMessage(id: "week-msg-11", authorID: "mina-park", sentAt: weekday.addingTimeInterval(360), text: "And five from the lake loop.", attachments: Array(gallery.prefix(5)))),
            .message(PrototypeMessage(id: "week-msg-11b", authorID: "maya-chen", sentAt: weekday.addingTimeInterval(420), text: "These six cover the trail from start to finish.", attachments: Array(gallery.prefix(6)))),
            .message(PrototypeMessage(id: "week-msg-12", authorID: "nora-bennett", sentAt: weekday.addingTimeInterval(480), text: "A few views from last time.", attachments: gallery, reactions: [
                PrototypeReaction(emoji: "❤", personIDs: [profileID, "maya-chen", "elias-moreno"]),
                PrototypeReaction(emoji: "🔥", personIDs: ["mina-park"]),
            ])),
            .message(PrototypeMessage(id: "week-msg-13", authorID: "maya-chen", sentAt: weekday.addingTimeInterval(600), text: "I’m done with the route changes, so you can take admin back.")),
            .event(PrototypeTimelineEvent(id: "week-event-admin-remove", date: weekday.addingTimeInterval(720), kind: .adminRevoked(actorID: profileID, personID: "maya-chen"))),

            .event(PrototypeTimelineEvent(id: "week-event-description-remove", date: yesterday, kind: .groupDescriptionRemoved(actorID: profileID))),
            .message(PrototypeMessage(id: "week-msg-14", authorID: "maya-chen", sentAt: yesterday, text: "@Marmota, does the riverside path work?")),
            .message(PrototypeMessage(id: "week-msg-15", authorID: profileID, sentAt: yesterday.addingTimeInterval(120), text: "Yes, and the forecast looks clear.", replyToMessageID: "week-msg-14")),
            .message(PrototypeMessage(id: "week-msg-16", authorID: "nora-bennett", sentAt: yesterday.addingTimeInterval(240), text: "Then let’s keep the changing details here instead of in the description.", replyToMessageID: "week-msg-15")),
            .message(PrototypeMessage(id: "week-msg-17", authorID: "maya-chen", sentAt: yesterday.addingTimeInterval(360), text: "This clip shows the narrow section.", attachments: [
                .video(id: "week-video", url: showcaseVideoURL, thumbnail: .asset("ProfileAvatarPebble"), duration: 8),
            ])),
            .message(PrototypeMessage(id: "week-msg-18", authorID: profileID, sentAt: yesterday.addingTimeInterval(480), text: "And here’s the bridge beside it.", attachments: [
                gallery[0],
                .video(id: "week-mixed-video", url: showcaseVideoURL, thumbnail: .asset("ProfileAvatarPebble"), duration: 8),
            ], replyToMessageID: "week-msg-17")),

            .message(PrototypeMessage(id: "week-msg-19", authorID: "leo-martins", sentAt: today, attachments: [.gif(id: "week-gif", assetName: "FiatjafMediaMarmot", label: "Marmot looking around")], reactions: [PrototypeReaction(emoji: "🤣", personIDs: [profileID, "maya-chen"])])),
            .message(PrototypeMessage(id: "week-msg-20", authorID: profileID, sentAt: today.addingTimeInterval(120), attachments: [gallery[5]], reactions: [PrototypeReaction(emoji: "🦫", personIDs: ["elias-moreno"])])),
            .message(PrototypeMessage(id: "week-msg-21", authorID: "maya-chen", sentAt: today.addingTimeInterval(240), text: "Not the steep shortcut—the entrance with the sunlit trees.", attachments: [gallery[0]], reactions: [PrototypeReaction(emoji: "👎", personIDs: [profileID, "nora-bennett"])])),
            .message(PrototypeMessage(id: "week-msg-22", authorID: "elias-moreno", sentAt: today.addingTimeInterval(360), attachments: [.contact(id: "week-contact", personID: "avery-stone")])),
            .message(PrototypeMessage(id: "week-msg-23", authorID: "nora-bennett", sentAt: today.addingTimeInterval(480), attachments: [bundledFile(id: "week-file", name: "Trail Plan.pdf", resourceName: "TrailPlan")])),
            .message(PrototypeMessage(id: "week-msg-24", authorID: profileID, sentAt: today.addingTimeInterval(600), attachments: [.voice(id: "week-voice", resourceName: PrototypeVoiceSample.resourceName, duration: PrototypeVoiceSample.duration)])),
            .message(PrototypeMessage(id: "week-msg-25", authorID: "leo-martins", sentAt: today.addingTimeInterval(720), text: "I’m stepping out, but I hope the walk goes well.")),
            .event(PrototypeTimelineEvent(id: "week-event-left", date: today.addingTimeInterval(840), kind: .memberLeft(personID: "leo-martins"))),
            .event(PrototypeTimelineEvent(id: "week-event-theo-added", date: today.addingTimeInterval(900), kind: .membersAdded(actorID: profileID, personIDs: ["theo-grant"]))),
            .message(PrototypeMessage(id: "week-msg-26", authorID: "theo-grant", sentAt: today.addingTimeInterval(960), text: "I won’t be joining this one.")),
            .event(PrototypeTimelineEvent(id: "week-event-removed", date: today.addingTimeInterval(1_080), kind: .memberRemoved(actorID: profileID, personID: "theo-grant"))),
            .message(PrototypeMessage(id: "week-msg-27", authorID: "nora-bennett", sentAt: today.addingTimeInterval(1_200), text: "Saturday morning works for me.")),
        ]
    }

    private static func fiatjafTimeline(profileID: String, now: Date) -> [PrototypeTimelineEntry] {
        let start = now.addingTimeInterval(-2_400)
        return [
            .message(PrototypeMessage(id: "fiatjaf-1", authorID: profileID, sentAt: start, text: "I’m moving from Feather to White Noise.")),
            .message(PrototypeMessage(id: "fiatjaf-2", authorID: "fiatjaf", sentAt: start.addingTimeInterval(60), text: "Let me know how it goes.")),
            .message(PrototypeMessage(id: "fiatjaf-3", authorID: profileID, sentAt: start.addingTimeInterval(120), text: "Signing in now.\nI’ll send a test next.")),
            .message(PrototypeMessage(id: "fiatjaf-4", authorID: profileID, sentAt: start.addingTimeInterval(300), text: "Switched from Feather to White Noise. Same key, same contacts.")),
            .message(PrototypeMessage(id: "fiatjaf-5", authorID: "fiatjaf", sentAt: start.addingTimeInterval(420), text: "Yep, I still see you on Primal. No extra setup on my side.", replyToMessageID: "fiatjaf-4")),
            .message(PrototypeMessage(id: "fiatjaf-6", authorID: profileID, sentAt: start.addingTimeInterval(540), text: "Exactly. Moved apps, kept everything. Didn’t have to re-add anyone.", reactions: [PrototypeReaction(emoji: "🔥", personIDs: ["fiatjaf"])])),
            .message(PrototypeMessage(id: "fiatjaf-7", authorID: "fiatjaf", sentAt: start.addingTimeInterval(660), text: "Perfect!")),
            .message(PrototypeMessage(id: "fiatjaf-8", authorID: "fiatjaf", sentAt: start.addingTimeInterval(780), text: "Portable identity for the win.", attachments: [
                .photo(id: "fiatjaf-photo-1", source: .asset("FiatjafMediaMarmot"), label: "Marmot"),
                .photo(id: "fiatjaf-photo-2", source: .asset("FiatjafMediaBadger"), label: "Badger"),
                .photo(id: "fiatjaf-photo-3", source: .asset("FiatjafMediaFox"), label: "Fox"),
                .photo(id: "fiatjaf-photo-4", source: .asset("FiatjafMediaSloth"), label: "Sloth"),
                .photo(id: "fiatjaf-photo-5", source: .asset("FiatjafMediaOstrich"), label: "Ostrich"),
            ])),
        ]
    }

    private static func seedAuthorID(for row: ChatListItem, profileID: String) -> String {
        guard row.previewAuthor != "You" else { return profileID }
        guard groupIDs.contains(row.id), let author = row.previewAuthor else { return row.id }
        let known = [
            "Tim": "tim", "Jude": "jude", "Sam": "sam", "Owen": "owen",
            "Remy": "remy", "Maya": "maya-chen", "Nora": "nora-bennett",
            "Iris": "iris", "Noah": "noah",
        ]
        return known[author] ?? author.lowercased().replacingOccurrences(of: " ", with: "-")
    }

    private static func seedAttachments(
        _ preview: ChatListItem.AttachmentPreview?,
        chatID: String
    ) -> [PrototypeAttachment] {
        guard let preview else { return [] }
        switch preview {
        case .photo:
            return [.photo(id: "\(chatID)-photo", source: .asset("AvatarWebAionyHaust"), label: "Portrait in soft daylight")]
        case let .photos(count):
            let assets = [
                "AvatarWebAionyHaust", "AvatarGardenClub",
                "AvatarWebAyoOgunseinde", "AvatarWebIanDooley",
                "AvatarWebSergioDePaula", "AvatarWebVinceFleming",
                "AvatarWebPhilipMartin",
            ]
            return (0..<count).map { index in
                .photo(id: "\(chatID)-photo-\(index)", source: .asset(assets[index % assets.count]), label: "Photo \(index + 1)")
            }
        case .video:
            return [.video(id: "\(chatID)-video", url: showcaseVideoURL, thumbnail: .asset("AvatarGardenClub"), duration: 8)]
        case .voiceMessage:
            return [.voice(id: "\(chatID)-voice", resourceName: PrototypeVoiceSample.resourceName, duration: PrototypeVoiceSample.duration)]
        case let .file(name):
            let resourceName = switch name {
            case "Project Brief.pdf": "ProjectBrief"
            case "Project Notes.pdf": "ProjectNotes"
            case "Trail Plan.pdf": "TrailPlan"
            default: "WeekendNotes"
            }
            return [bundledFile(id: "\(chatID)-file", name: name, resourceName: resourceName)]
        case let .contact(name):
            return [.contact(id: "\(chatID)-contact", personID: name == "Avery Stone" ? "avery-stone" : "maya-chen")]
        case .link:
            return [.link(id: "\(chatID)-link", title: "Reading for later", domain: "whitenoise.chat", summary: "A useful link shared with the chat.", image: nil)]
        case .gif:
            return [.gif(id: "\(chatID)-gif", assetName: "FiatjafMediaMarmot", label: "Marmot")]
        }
    }

    private static func seedDate(for label: String, now: Date) -> Date {
        if label == "Now" { return now }
        if label.hasSuffix("m"), let minutes = Int(label.dropLast()) {
            return now.addingTimeInterval(TimeInterval(-minutes * 60))
        }
        if label.hasSuffix("h"), let hours = Int(label.dropLast()) {
            return now.addingTimeInterval(TimeInterval(-hours * 3_600))
        }
        if label == "Yesterday" { return now.addingTimeInterval(-86_400) }

        let calendar = Calendar.autoupdatingCurrent
        let weekdaySymbols = [
            "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday",
        ]
        if let weekdayIndex = weekdaySymbols.firstIndex(of: label) {
            let targetWeekday = weekdayIndex + 1
            let currentWeekday = calendar.component(.weekday, from: now)
            let daysBack = (currentWeekday - targetWeekday + 7) % 7
            return calendar.date(byAdding: .day, value: -(daysBack == 0 ? 7 : daysBack), to: now)
                ?? now.addingTimeInterval(-7 * 86_400)
        }

        let dateParts = label.split(separator: "/").compactMap { Int($0) }
        if dateParts.count == 3,
           let parsed = calendar.date(
               from: DateComponents(
                   year: 2_000 + dateParts[2],
                   month: dateParts[0],
                   day: dateParts[1]
               )
           ) {
            let day = calendar.component(.day, from: parsed)
            let hour = 9 + day % 10
            let minute = (day * 7 % 4) * 15
            return calendar.date(bySettingHour: hour, minute: minute, second: 0, of: parsed)
                ?? parsed
        }
        return now.addingTimeInterval(-7 * 86_400)
    }

    private static let showcaseVideoURL = resourceURL(
        name: "ChatTrailClip",
        fileExtension: "mp4"
    )

    private static let bundledFileResources: [String: (url: URL?, size: Int)] = {
        let names = ["ProjectBrief", "ProjectNotes", "TrailPlan", "WeekendNotes"]
        return Dictionary(uniqueKeysWithValues: names.map { name in
            let url = resourceURL(name: name, fileExtension: "pdf")
            let size = url.flatMap { url in
                try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize
            } ?? 0
            return (name, (url, size))
        })
    }()

    private static func bundledFile(
        id: String,
        name: String,
        resourceName: String
    ) -> PrototypeAttachment {
        let resource = bundledFileResources[resourceName]
        return .file(
            id: id,
            name: name,
            size: resource?.size ?? 0,
            url: resource?.url
        )
    }

    private static func resourceURL(name: String, fileExtension: String) -> URL? {
        fixtureBundle.url(forResource: name, withExtension: fileExtension)
            ?? Bundle.main.url(forResource: name, withExtension: fileExtension)
    }

    private static let fixtureBundle = Bundle(
        for: PrototypeChatFixtureBundleToken.self
    )

    private static func about(for id: String) -> String {
        switch id {
        case "catalog-direct-text":
            "Turning complexity into clarity.\nAlways happy to compare notes."
        case "catalog-direct-dates":
            "Planning one good day at a time.\nUsually outside before sunset. 🌤️"
        case "catalog-direct-replies":
            "Making space for thoughtful conversations.\nCollecting useful references.\nLearning something new every day."
        case "catalog-direct-reactions":
            "Here for good questions and honest answers.\nUsually carrying a camera. 📷\nSend the interesting ideas my way.\nTea and long walks help. 🍵"
        case "maya-chen": "Designer, careful listener, and collector of useful references."
        case "fiatjaf": "Building open tools for portable identity and resilient communication."
        case ChatListFixtures.supportChatID: "Help with White Noise."
        default: "Quietly sharing ideas and keeping in touch."
        }
    }

    private static func groupDescription(for id: String) -> String {
        switch id {
        case "weekend-walks": "Plans, routes, and photos from our weekend walks."
        case "product-circle": "Notes and decisions from the product circle."
        case "nostr-devs": "Building and testing open communication tools."
        default: "A shared space for this group."
        }
    }
}

extension PrototypeRelayConfiguration {
    var availableChatMessageRelayURLs: [String] {
        relays.compactMap { relay in
            guard relay.capability == .readWrite,
                  relay.connectionState == .connected,
                  relay.usages.contains(.chatMessages)
            else { return nil }
            return relay.url
        }
    }
}
