import Foundation
import Testing
@testable import WhiteNoisePrototype

@Suite("Authoritative chat model")
struct PrototypeChatModelTests {
    @Test("Group monograms use the first group-name letter")
    func groupMonograms() {
        #expect(prototypeGroupMonogram("") == "")
        #expect(prototypeGroupMonogram("Weekend") == "W")
        #expect(prototypeGroupMonogram(" Weekend   Walks ") == "W")
        #expect(prototypeGroupMonogram("river trail plans") == "R")
    }

    @Test("Direct chats are deduplicated and copy relays once")
    func directChatDeduplication() throws {
        var profile = PrototypeProfile.pebble
        profile.relayConfiguration = .fixtures
        let createdDirectID = profile.openOrCreateDirectChat(
            personID: "maya-chen",
            chatID: "maya-new"
        )
        let first = try #require(createdDirectID)
        let copiedRelays = try #require(profile.chats.first?.routing.relayURLs)
        profile.relayConfiguration = .missingChatMessages
        let second = profile.openOrCreateDirectChat(personID: "maya-chen", chatID: "duplicate")

        #expect(first == "maya-new")
        #expect(second == first)
        #expect(profile.chats.count == 1)
        #expect(profile.chats[0].routing.relayURLs == copiedRelays)
        #expect(profile.chats[0].timeline.count == 1)
        guard case let .event(startEvent)? = profile.chats[0].timeline.first else {
            Issue.record("A new direct chat must begin with its inception event.")
            return
        }
        #expect(startEvent.kind == .directChatStarted(actorID: profile.id))
    }

    @Test("A new direct chat requires profile Chat Messages relays")
    func directChatCreationRequiresRelays() {
        var profile = PrototypeProfile.pebble
        profile.relayConfiguration = .missingChatMessages

        #expect(profile.openOrCreateDirectChat(personID: "maya-chen") == nil)
        #expect(profile.chats.isEmpty)
    }

    @Test("Group creation validates input, makes the profile admin, and copies relays")
    func groupCreation() throws {
        var profile = PrototypeProfile.pebble
        #expect(profile.createGroup(name: " ", description: "", avatar: .monogram("G"), selectedPersonIDs: ["maya-chen"]) == nil)
        #expect(profile.createGroup(name: "Walks", description: "Outside", avatar: .monogram("W"), selectedPersonIDs: []) == nil)

        let createdGroupID = profile.createGroup(
            id: "walks",
            name: " Walks ",
            description: " Outside ",
            avatar: .monogram("W"),
            selectedPersonIDs: ["maya-chen", "maya-chen", profile.id],
            now: Date(timeIntervalSince1970: 1_000)
        )
        let id = try #require(createdGroupID)
        let group = try #require(profile.chats.first { $0.id == id })
        #expect(group.groupName == "Walks")
        #expect(group.groupDescription == "Outside")
        #expect(group.members == [
            PrototypeGroupMember(personID: profile.id, role: .admin),
            PrototypeGroupMember(personID: "maya-chen", role: .member),
        ])
        #expect(group.routing.relayURLs == profile.relayConfiguration.availableChatMessageRelayURLs)
        #expect(group.timeline.count == 1)
        guard case let .event(createdEvent)? = group.timeline.first else {
            Issue.record("A new group must begin with its creation event.")
            return
        }
        #expect(createdEvent.kind == .groupCreated(actorID: profile.id))
    }

    @Test("A new group requires profile Chat Messages relays")
    func groupCreationRequiresRelays() {
        var profile = PrototypeProfile.pebble
        profile.relayConfiguration = .missingChatMessages

        #expect(
            profile.createGroup(
                name: "Walks",
                description: "",
                avatar: .monogram("W"),
                selectedPersonIDs: ["maya-chen"]
            ) == nil
        )
        #expect(profile.chats.isEmpty)
    }

    @Test("Support is not selectable for direct or group creation")
    func supportIsNotSelectable() {
        var profile = PrototypeProfile.marmota

        #expect(!profile.selectableChatPeople.contains { $0.id == ChatListFixtures.supportChatID })
        #expect(
            profile.createGroup(
                name: "Support Group",
                description: "",
                avatar: .monogram("S"),
                selectedPersonIDs: [ChatListFixtures.supportChatID]
            ) == nil
        )
    }

    @Test("Profile chat state is isolated")
    func profileIsolation() {
        var first = PrototypeProfile.marmota
        var second = PrototypeProfile.pebble
        let id = first.chats[0].id
        first.chats[0].draft = "Only first"
        _ = second.openOrCreateDirectChat(personID: "maya-chen", chatID: "second-maya")

        #expect(first.chats.first { $0.id == id }?.draft == "Only first")
        #expect(second.chats.first?.draft == "")
        #expect(second.chats.first?.id == "second-maya")
    }

    @Test("Every fixture chat has stable unique routing references")
    func fixtureRoutingIntegrity() {
        let profile = PrototypeProfile.marmota
        let personIDs = Set(profile.people.map(\.id))

        #expect(Set(profile.chats.map(\.id)).count == profile.chats.count)
        for chat in profile.chats {
            switch chat.kind {
            case let .direct(personID):
                #expect(personIDs.contains(personID))
            case .group:
                #expect(chat.members.allSatisfy {
                    $0.personID == profile.id || personIDs.contains($0.personID)
                })
            }
            #expect(chat.messages.allSatisfy {
                $0.authorID == profile.id || personIDs.contains($0.authorID)
            })
        }
    }

    @Test("Developer catalog order and list states stay stable")
    func developerCatalogOrderAndListStates() throws {
        let expectedTitles = [
            "Direct - Text & Delivery",
            "Direct - Dates & Scrolling",
            "Direct - Replies & Deletion",
            "Direct - Reactions & Actions",
            "Direct - New Chat & Draft",
            "Composer - Text",
            "Composer - Multiline",
            "Composer - Link",
            "Composer - Link Preview",
            "Composer - Photo",
            "Composer - Photo Album",
            "Composer - Mixed Media",
            "Composer - File",
            "Composer - GIF",
            "Composer - Contact",
            "Composer - Reply",
            "Composer - Mention",
            "Media - Single Photos & Video",
            "Media - Gallery Layouts",
            "Media - Viewer & Actions",
            "Media - Files & Rich Content",
            "Voice Messages",
            "Group - Messages & Mentions",
            "Group - Identity Colors",
            "Group - Events & Roles",
            "Group - Member Permissions",
            "Group - Sole Admin",
            "Direct - Disappearing",
            "Direct - Disappearing & Muted",
            "Group - Disappearing",
            "Direct - Invitation",
            "Group - Invitation",
            "Direct - Left",
            "Group - Left",
            "Group - Removed",
            "Direct - Blocked",
            "Direct - Missing Relays",
            "Direct - Archived",
            "Support - Timeline Notice",
        ]
        let rows = Array(ChatListFixtures.populated.prefix(ChatListFixtures.catalogChatIDs.count))
        let chats = Array(PrototypeProfile.marmota.chats.prefix(ChatListFixtures.catalogChatIDs.count))

        #expect(rows.map(\.id) == ChatListFixtures.catalogChatIDs)
        #expect(rows.map(\.title) == expectedTitles)
        #expect(rows.allSatisfy { !$0.title.contains("—") && !$0.preview.contains("·") })
        #expect(chats.map(\.id) == ChatListFixtures.catalogChatIDs)
        #expect(ChatListFixtures.populated.filter(\.isPinned).map(\.id) == ["catalog-direct-text"])
        #expect(rows.contains { $0.unreadCount > 0 })
        #expect(rows.contains { $0.isMarkedUnread })
        #expect(rows.contains { $0.isMuted })
        #expect(rows.contains { $0.isDraft })
        #expect(rows.contains { $0.deliveryState == .failed })
        #expect(rows.contains { $0.isArchived })
        #expect(rows.contains { $0.membershipState == .left })
        #expect(rows.contains { $0.membershipState == .removed })
        #expect(rows.filter { $0.membershipState == .invited }.count == 2)

        let firstLegacyRow = try #require(ChatListFixtures.populated.dropFirst(rows.count).first)
        #expect(!ChatListFixtures.catalogChatIDs.contains(firstLegacyRow.id))
    }

    @Test("Developer catalog covers disappearing-message row and header states")
    func developerCatalogDisappearingMessageIndicators() throws {
        let profile = PrototypeProfile.marmota
        let direct = try #require(
            profile.chats.first { $0.id == "catalog-direct-disappearing" }
        )
        let mutedDirect = try #require(
            profile.chats.first { $0.id == "catalog-direct-disappearing-muted" }
        )
        let group = try #require(
            profile.chats.first { $0.id == "catalog-group-disappearing" }
        )

        #expect(direct.disappearingMessageDuration == .oneDay)
        #expect(direct.disappearingMessageDuration.compactTitle == "1d")
        #expect(direct.listState.muteDuration == nil)

        #expect(mutedDirect.disappearingMessageDuration == .oneWeek)
        #expect(mutedDirect.disappearingMessageDuration.compactTitle == "1w")
        #expect(mutedDirect.listState.muteDuration != nil)

        #expect(group.isGroup)
        #expect(group.disappearingMessageDuration == .fourWeeks)
        #expect(group.disappearingMessageDuration.compactTitle == "4w")
        #expect(!group.members.isEmpty)

        let rows = [direct, mutedDirect, group].map {
            $0.row(
                people: profile.people,
                currentProfileID: profile.id,
                now: .now
            )
        }
        #expect(rows.allSatisfy { $0.hasDisappearingMessages })
        #expect(rows[0].disappearingMessageDuration == .oneDay)
        #expect(rows[1].isMuted)
        #expect(rows[2].isGroup)
    }

    @Test("Developer catalog fixtures cover every renderer without legacy chats")
    func developerCatalogFixtureCoverage() throws {
        let profile = PrototypeProfile.marmota
        let catalogIDs = Set(ChatListFixtures.catalogChatIDs)
        let chats = profile.chats.filter { catalogIDs.contains($0.id) }
        let entries = chats.flatMap(\.timeline)
        let messages = chats.flatMap(\.messages)
        let attachments = messages.flatMap(\.attachments)
        let events = entries.compactMap { entry -> PrototypeChatEventKind? in
            guard case let .event(event) = entry else { return nil }
            return event.kind
        }

        #expect(chats.count == ChatListFixtures.catalogChatIDs.count)
        #expect(Set(entries.map(\.id)).count == entries.count)
        #expect(chats.allSatisfy { $0.timeline.map(\.date) == $0.timeline.map(\.date).sorted() })
        #expect(messages.allSatisfy { !$0.text.contains("·") })
        let captionedScenarioIDs = Set(messages.compactMap { message -> String? in
            guard message.id.hasSuffix("-caption") else { return nil }
            return message.id.replacingOccurrences(of: "-caption", with: "")
        })
        for message in messages where !message.text.isEmpty {
            let scenarioID = message.id
                .replacingOccurrences(of: "-source-caption", with: "")
                .replacingOccurrences(of: "-caption", with: "")
                .replacingOccurrences(of: "-source", with: "")
                .replacingOccurrences(of: "-message", with: "")
            #expect(
                message.text.contains(scenarioID)
                    || captionedScenarioIDs.contains(scenarioID),
                "Message \(message.id) must contain scenario ID \(scenarioID)"
            )
        }

        let galleryCounts = Set(messages.map(\.attachments.count))
        #expect(galleryCounts.isSuperset(of: Set(1...7)))
        #expect(attachments.contains { if case .photo = $0 { true } else { false } })
        #expect(attachments.contains { if case .video = $0 { true } else { false } })
        #expect(attachments.contains { if case .file = $0 { true } else { false } })
        #expect(attachments.contains { if case .voice = $0 { true } else { false } })
        #expect(attachments.contains { if case .link = $0 { true } else { false } })
        #expect(attachments.contains { if case .gif = $0 { true } else { false } })
        #expect(attachments.contains { if case .contact = $0 { true } else { false } })
        #expect(attachments.contains { attachment in
            guard case let .photo(_, source, _, _) = attachment else { return false }
            if case .data = source { return true }
            return false
        })
        #expect(attachments.contains { if case let .video(_, url, _, _, _) = $0 { url == nil } else { false } })
        #expect(attachments.contains { if case let .file(_, _, _, url) = $0 { url == nil } else { false } })
        #expect(attachments.contains { if case let .link(_, _, domain, _, _) = $0 { domain.isEmpty } else { false } })

        let fileNames = attachments.compactMap { attachment -> String? in
            guard case let .file(_, name, _, url) = attachment, url != nil else { return nil }
            return name
        }
        #expect(Set(fileNames).isSuperset(of: [
            "Project Brief.pdf", "Review Notes.docx", "Budget.xlsx", "Assets.zip", "Read Me.txt",
        ]))
        #expect(messages.contains { message in
            guard message.id == "RICH-05", message.attachments.count == 1 else {
                return false
            }
            if case .contact = message.attachments[0] {
                return true
            }
            return false
        })
        #expect(messages.contains { $0.replyToMessageID == "RPL-missing" })
        #expect(messages.contains { $0.deletionState == .deletedByCurrentProfile })
        #expect(messages.contains { $0.deletionState == .deletedByOther })
        #expect(messages.contains { $0.deliveryState == .sending })
        #expect(messages.contains { $0.deliveryState == .sent })
        #expect(messages.contains { $0.deliveryState == .failed })
        #expect(Set(messages.flatMap(\.reactions).map(\.emoji)) == Set(PrototypeReaction.supportedEmoji))

        #expect(events.contains { if case .directChatStarted = $0 { true } else { false } })
        #expect(events.contains { if case .directChatLeft = $0 { true } else { false } })
        #expect(events.contains { if case .groupCreated = $0 { true } else { false } })
        #expect(events.contains { if case .membersAdded = $0 { true } else { false } })
        #expect(events.contains { if case .memberJoined = $0 { true } else { false } })
        #expect(events.contains { if case .memberLeft = $0 { true } else { false } })
        #expect(events.contains { if case .memberRemoved = $0 { true } else { false } })
        #expect(events.contains { if case .adminGranted = $0 { true } else { false } })
        #expect(events.contains { if case .adminRevoked = $0 { true } else { false } })
        #expect(events.contains { if case .groupNameChanged = $0 { true } else { false } })
        #expect(events.contains { if case .groupPhotoChanged = $0 { true } else { false } })
        #expect(events.contains { if case .groupPhotoRemoved = $0 { true } else { false } })
        #expect(events.contains { if case .groupDescriptionChanged = $0 { true } else { false } })
        #expect(events.contains { if case .groupDescriptionRemoved = $0 { true } else { false } })
        #expect(events.contains { if case .disappearingMessagesChanged = $0 { true } else { false } })

        let support = try #require(chats.first { $0.id == ChatListFixtures.supportChatID })
        #expect(support.messages.isEmpty)
        #expect(support.timeline.count == 1)
        #expect(support.timeline.first?.id == "STATE-08")
        #expect(
            support.row(people: profile.people, currentProfileID: profile.id).preview
                == support.emptyPreview
        )
    }

    @Test("Composer catalog fixtures expose every unsent content state")
    func composerCatalogFixtureCoverage() throws {
        let profile = PrototypeProfile.marmota
        let composerChats = profile.chats.filter {
            $0.id.hasPrefix("catalog-composer-")
        }

        #expect(composerChats.count == 12)
        #expect(composerChats.allSatisfy {
            !$0.draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || !$0.draftAttachments.isEmpty
        })

        let chats = Dictionary(
            uniqueKeysWithValues: composerChats.map { ($0.id, $0) }
        )
        #expect(chats["catalog-composer-text"]?.draft == "Here’s the updated plan.")
        #expect(chats["catalog-composer-multiline"]?.draft.split(separator: "\n").count == 4)
        #expect(chats["catalog-composer-link"]?.suppressedDraftLinkURL == "https://whitenoise.chat")
        #expect(chats["catalog-composer-link-preview"]?.suppressedDraftLinkURL == nil)
        #expect(chats["catalog-composer-photo"]?.draftAttachments.count == 1)
        #expect(chats["catalog-composer-photo-album"]?.draftAttachments.count == 4)
        #expect(chats["catalog-composer-mixed-media"]?.draftAttachments.count == 3)
        #expect(chats["catalog-composer-file"]?.draftAttachments.count == 1)
        #expect(chats["catalog-composer-gif"]?.draftAttachments.count == 1)
        #expect(chats["catalog-composer-contact"]?.draftAttachments.count == 1)
        #expect(chats["catalog-composer-reply"]?.replyToMessageID == "CMP-REPLY")
        #expect(chats["catalog-composer-mention"]?.isGroup == true)

        let album = try #require(
            chats["catalog-composer-photo-album"]?.draftAttachments
        )
        #expect(album.allSatisfy { if case .photo = $0 { true } else { false } })

        let mixed = try #require(
            chats["catalog-composer-mixed-media"]?.draftAttachments
        )
        #expect(mixed.filter { if case .photo = $0 { true } else { false } }.count == 2)
        #expect(mixed.filter { if case .video = $0 { true } else { false } }.count == 1)

        let photoDraftRow = try #require(
            chats["catalog-composer-photo"]?.row(
                people: profile.people,
                currentProfileID: profile.id
            )
        )
        #expect(photoDraftRow.isDraft)
        #expect(photoDraftRow.visiblePreview == "Photo")
    }

    @Test("Composer link detection accepts HTTPS and provides deterministic metadata")
    func composerLinkPreviewDetection() throws {
        let preview = try #require(
            PrototypeComposerLinkPreview.first(
                in: "Worth a look: https://developer.apple.com/design/human-interface-guidelines"
            )
        )

        #expect(preview.url.absoluteString == "https://developer.apple.com/design/human-interface-guidelines")
        #expect(preview.domain == "developer.apple.com")
        #expect(preview.title == "Apple Human Interface Guidelines")
        #expect(PrototypeComposerLinkPreview.first(in: "http://example.com") == nil)
        #expect(PrototypeComposerLinkPreview.first(in: "No link here") == nil)
    }

    @Test("Sending clears every persisted composer state")
    func sendingClearsPersistedComposerState() {
        var chat = emptyDirectChat()
        chat.draft = "Caption"
        chat.draftAttachments = [
            .photo(
                id: "photo",
                source: .asset("FiatjafMediaFox"),
                label: "Fox"
            ),
        ]
        chat.suppressedDraftLinkURL = "https://whitenoise.chat"
        chat.replyToMessageID = "source"

        chat.appendMessage(
            authorID: "marmota",
            text: chat.draft,
            attachments: chat.draftAttachments
        )

        #expect(chat.draft.isEmpty)
        #expect(chat.draftAttachments.isEmpty)
        #expect(chat.suppressedDraftLinkURL == nil)
        #expect(chat.replyToMessageID == nil)
        #expect(chat.messages.last?.text == "Caption")
        #expect(chat.messages.last?.attachments.count == 1)
        #expect(chat.messages.last?.replyToMessageID == "source")
    }

    @Test("Reaction catalog distinguishes count and participation states")
    func reactionCatalogVariants() throws {
        let profileID = "marmota"
        let chat = try #require(
            PrototypeChatFixtures.chats(
                profileID: profileID,
                relayURLs: ["wss://relay.example.com"],
                now: Date(timeIntervalSince1970: 2_000_000_000)
            ).first { $0.id == "catalog-direct-reactions" }
        )
        let messages = Dictionary(uniqueKeysWithValues: chat.messages.map { ($0.id, $0) })

        let singleOther = try #require(messages["RCT-01"]?.reactions.first)
        #expect(messages["RCT-01"]?.reactions.count == 1)
        #expect(singleOther.personIDs.count == 1)
        #expect(!singleOther.personIDs.contains(profileID))

        let singleCurrent = try #require(messages["RCT-02"]?.reactions.first)
        #expect(messages["RCT-02"]?.reactions.count == 1)
        #expect(singleCurrent.personIDs == [profileID])

        let repeatedOther = try #require(messages["RCT-03"]?.reactions.first)
        #expect(messages["RCT-03"]?.reactions.count == 1)
        #expect(repeatedOther.personIDs.count == 3)
        #expect(!repeatedOther.personIDs.contains(profileID))

        let repeatedCurrent = try #require(messages["RCT-04"]?.reactions.first)
        #expect(messages["RCT-04"]?.reactions.count == 1)
        #expect(repeatedCurrent.personIDs.count == 3)
        #expect(repeatedCurrent.personIDs.contains(profileID))

        let mixed = try #require(messages["RCT-05"]?.reactions)
        #expect(mixed.count == 3)
        #expect(mixed.contains { $0.personIDs.count == 1 })
        #expect(mixed.contains { $0.personIDs.count > 1 })
        #expect(mixed.contains { $0.personIDs.contains(profileID) })
        #expect(mixed.contains { !$0.personIDs.contains(profileID) })

        let overflow = try #require(messages["RCT-13"]?.reactions)
        #expect(overflow.count == PrototypeReaction.supportedEmoji.count)
        #expect(overflow.map(\.emoji) == PrototypeReaction.supportedEmoji)
        #expect(overflow.contains { $0.personIDs.contains(profileID) })
        #expect(overflow.contains { !$0.personIDs.contains(profileID) })
    }

    @Test("Catalog permissions and recovery states are explicit")
    func developerCatalogPermissionsAndRecovery() throws {
        let profile = PrototypeProfile.marmota
        let eventsGroup = try #require(profile.chats.first { $0.id == "catalog-group-events" })
        let memberGroup = try #require(profile.chats.first { $0.id == "catalog-group-member" })
        var soleAdminGroup = try #require(profile.chats.first { $0.id == "catalog-group-sole-admin" })
        let directLeft = try #require(profile.chats.first { $0.id == "catalog-direct-left" })
        let groupLeft = try #require(profile.chats.first { $0.id == "catalog-group-left" })
        let groupRemoved = try #require(profile.chats.first { $0.id == "catalog-group-removed" })
        let directInvitation = try #require(
            profile.chats.first { $0.id == "catalog-direct-invitation" }
        )
        let groupInvitation = try #require(
            profile.chats.first { $0.id == "catalog-group-invitation" }
        )
        let blocked = try #require(profile.chats.first { $0.id == "catalog-direct-blocked" })
        var missingRelays = try #require(profile.chats.first { $0.id == "catalog-direct-missing-relays" })
        let archived = try #require(profile.chats.first { $0.id == "catalog-direct-archived" })

        #expect(eventsGroup.canManageGroup(currentProfileID: profile.id))
        #expect(eventsGroup.groupDescription.isEmpty)
        #expect(eventsGroup.disappearingMessageDuration == .off)
        #expect(!memberGroup.canManageGroup(currentProfileID: profile.id))
        #expect(soleAdminGroup.members.filter { $0.role == .admin }.map(\.personID) == [profile.id])
        let protectedLeave = soleAdminGroup.leave(currentProfileID: profile.id)
        let promotedAdmin = soleAdminGroup.promoteMember(personID: "maya-chen", actorID: profile.id)
        let validLeave = soleAdminGroup.leave(currentProfileID: profile.id)
        #expect(!protectedLeave)
        #expect(promotedAdmin)
        #expect(validLeave)

        #expect(directLeft.listState.membershipState == .left)
        #expect(groupLeft.listState.membershipState == .left)
        #expect(groupRemoved.listState.membershipState == .removed)
        #expect(directInvitation.listState.membershipState == .invited)
        #expect(groupInvitation.listState.membershipState == .invited)
        #expect(!groupInvitation.members.contains { $0.personID == profile.id })
        #expect(
            directInvitation.composerAvailability(
                currentProfileID: profile.id,
                people: profile.people
            ) == .pendingInvitation
        )
        #expect(
            groupInvitation.composerAvailability(
                currentProfileID: profile.id,
                people: profile.people
            ) == .pendingInvitation
        )
        #expect(
            directLeft.row(people: profile.people, currentProfileID: profile.id).visiblePreview
                == "You left this chat."
        )
        #expect(
            groupLeft.row(people: profile.people, currentProfileID: profile.id).visiblePreview
                == "You left this group."
        )
        #expect(
            groupRemoved.row(people: profile.people, currentProfileID: profile.id).visiblePreview
                == "You were removed from this group."
        )
        #expect(!groupLeft.members.contains { $0.personID == profile.id })
        #expect(!groupRemoved.members.contains { $0.personID == profile.id })
        #expect(blocked.composerAvailability(currentProfileID: profile.id, people: profile.people) == .blocked)
        var unblockedPeople = profile.people
        let blockedPersonIndex = try #require(
            unblockedPeople.firstIndex { $0.id == "catalog-direct-blocked" }
        )
        unblockedPeople[blockedPersonIndex].isBlocked = false
        #expect(blocked.composerAvailability(currentProfileID: profile.id, people: unblockedPeople) == .available)
        #expect(missingRelays.composerAvailability(currentProfileID: profile.id, people: profile.people) == .missingRelays)
        missingRelays.routing.restoreDefaults()
        #expect(missingRelays.composerAvailability(currentProfileID: profile.id, people: profile.people) == .available)
        #expect(archived.listState.isArchived)
        #expect(archived.listState.membershipState == .active)
    }

    @Test("Groups in common require active membership for both profiles")
    func groupsInCommon() throws {
        var profile = PrototypeProfile.marmota

        #expect(
            profile.groupsShared(with: "radia-perlman").map(\.id)
                == ["nostr-devs", "marmots", "project-files"]
        )
        #expect(profile.groupsShared(with: "david-chaum").map(\.id) == ["nostr-devs"])
        #expect(profile.groupsShared(with: "maya-chen").count > 3)

        let sharedGroupID = try #require(
            profile.groupsShared(with: "david-chaum").first?.id
        )
        let index = try #require(
            profile.chats.firstIndex { $0.id == sharedGroupID }
        )
        profile.chats[index].listState.membershipState = .left

        #expect(profile.groupsShared(with: "david-chaum").isEmpty)
    }

    @Test("Row previews derive from messages, deletion, failure, and ended membership")
    func rowProjection() throws {
        var chat = emptyDirectChat()
        chat.appendMessage(authorID: "marmota", text: "First")
        chat.appendMessage(
            authorID: "marmota",
            text: "Failed",
            now: Date(timeIntervalSince1970: 2_000)
        )
        let lastID = try #require(chat.messages.last?.id)
        chat.mutateMessage(lastID) { $0.deliveryState = .failed }
        var row = chat.row(people: PrototypeChatFixtures.people(), currentProfileID: "marmota")
        #expect(row.preview == "Failed")
        #expect(row.deliveryState == .failed)

        chat.deleteMessage(lastID, currentProfileID: "marmota")
        row = chat.row(people: PrototypeChatFixtures.people(), currentProfileID: "marmota")
        #expect(row.preview == "First")

        chat.listState.membershipState = .left
        row = chat.row(people: PrototypeChatFixtures.people(), currentProfileID: "marmota")
        #expect(row.visiblePreview == "You left this chat.")
        #expect(row.deliveryState == .none)
    }

    @Test("Reply resolution preserves deleted and missing fallbacks")
    func replyResolution() {
        var chat = emptyDirectChat()
        let original = PrototypeMessage(id: "original", authorID: "maya-chen", sentAt: .now, text: "Hello")
        let reply = PrototypeMessage(id: "reply", authorID: "marmota", sentAt: .now, text: "Hi", replyToMessageID: "original")
        let missing = PrototypeMessage(id: "missing", authorID: "marmota", sentAt: .now, text: "Again", replyToMessageID: "gone")
        chat.timeline = [.message(original), .message(reply), .message(missing)]
        #expect(chat.messages.first { $0.id == reply.replyToMessageID }?.text == "Hello")
        #expect(chat.messages.first { $0.id == missing.replyToMessageID } == nil)
        chat.mutateMessage("original") { $0.deletionState = .deletedByOther }
        #expect(chat.messages.first { $0.id == "original" }?.isDeleted == true)
    }

    @Test("Reaction toggling is reversible")
    func reactionToggle() {
        var chat = emptyDirectChat()
        chat.timeline = [.message(PrototypeMessage(id: "m", authorID: "maya-chen", sentAt: .now, text: "Hi"))]
        chat.toggleReaction(emoji: "🦫", messageID: "m", currentProfileID: "marmota")
        #expect(chat.messages[0].reactions == [PrototypeReaction(emoji: "🦫", personIDs: ["marmota"])])
        chat.toggleReaction(emoji: "🦫", messageID: "m", currentProfileID: "marmota")
        #expect(chat.messages[0].reactions.isEmpty)
        chat.toggleReaction(emoji: "?", messageID: "m", currentProfileID: "marmota")
        #expect(chat.messages[0].reactions.isEmpty)
        chat.mutateMessage("m") { $0.deletionState = .deletedByOther }
        chat.toggleReaction(emoji: "❤", messageID: "m", currentProfileID: "marmota")
        #expect(chat.messages[0].reactions.isEmpty)
    }

    @Test("Selecting a reaction never removes the current profile's match")
    func reactionSelection() {
        var chat = emptyDirectChat()
        chat.timeline = [
            .message(
                PrototypeMessage(
                    id: "m",
                    authorID: "maya-chen",
                    sentAt: .now,
                    text: "Hi",
                    reactions: [
                        PrototypeReaction(
                            emoji: "🦫",
                            personIDs: ["maya-chen", "marmota"]
                        ),
                        PrototypeReaction(emoji: "🔥", personIDs: ["another-person"]),
                    ]
                )
            )
        ]

        chat.selectReaction(
            emoji: "🦫",
            messageID: "m",
            currentProfileID: "marmota"
        )
        #expect(
            chat.messages[0].reactions == [
                PrototypeReaction(
                    emoji: "🦫",
                    personIDs: ["maya-chen", "marmota"]
                ),
                PrototypeReaction(emoji: "🔥", personIDs: ["another-person"]),
            ]
        )

        chat.selectReaction(
            emoji: "🔥",
            messageID: "m",
            currentProfileID: "marmota"
        )
        #expect(
            chat.messages[0].reactions == [
                PrototypeReaction(emoji: "🦫", personIDs: ["maya-chen"]),
                PrototypeReaction(
                    emoji: "🔥",
                    personIDs: ["another-person", "marmota"]
                ),
            ]
        )
    }

    @Test("A full-picker reaction replaces the current profile's prior reaction")
    func reactionReplacement() {
        var chat = emptyDirectChat()
        chat.timeline = [
            .message(
                PrototypeMessage(
                    id: "m",
                    authorID: "maya-chen",
                    sentAt: .now,
                    text: "Hi",
                    reactions: [
                        PrototypeReaction(
                            emoji: "😂",
                            personIDs: ["marmota", "maya-chen"]
                        ),
                        PrototypeReaction(emoji: "🔥", personIDs: ["another-person"]),
                    ]
                )
            )
        ]

        chat.selectReaction(
            emoji: "🙂‍↕️",
            messageID: "m",
            currentProfileID: "marmota"
        )

        #expect(
            chat.messages[0].reactions == [
                PrototypeReaction(emoji: "😂", personIDs: ["maya-chen"]),
                PrototypeReaction(emoji: "🔥", personIDs: ["another-person"]),
                PrototypeReaction(emoji: "🙂‍↕️", personIDs: ["marmota"]),
            ]
        )
    }

    @Test("Delete for me removes messages but preserves chat events")
    func deleteForCurrentProfile() {
        var chat = emptyDirectChat()
        chat.timeline = [
            .event(
                PrototypeTimelineEvent(
                    id: "started",
                    date: Date(timeIntervalSince1970: 1),
                    kind: .directChatStarted(actorID: "marmota")
                )
            ),
            .message(
                PrototypeMessage(
                    id: "incoming",
                    authorID: "maya-chen",
                    sentAt: Date(timeIntervalSince1970: 2),
                    text: "Hello"
                )
            ),
            .message(
                PrototypeMessage(
                    id: "outgoing",
                    authorID: "marmota",
                    sentAt: Date(timeIntervalSince1970: 3),
                    text: "Hi"
                )
            ),
        ]
        chat.replyToMessageID = "incoming"

        chat.removeMessagesForCurrentProfile(["incoming"])

        #expect(chat.timeline.map(\.id) == ["started", "outgoing"])
        #expect(chat.replyToMessageID == nil)
    }

    @Test("Delete for everyone tombstones only current-profile messages")
    func deleteForEveryone() {
        var chat = emptyDirectChat()
        chat.timeline = [
            .message(
                PrototypeMessage(
                    id: "incoming",
                    authorID: "maya-chen",
                    sentAt: .now,
                    text: "Keep me"
                )
            ),
            .message(
                PrototypeMessage(
                    id: "outgoing",
                    authorID: "marmota",
                    sentAt: .now,
                    text: "Remove me",
                    replyToMessageID: "incoming",
                    reactions: [PrototypeReaction(emoji: "❤", personIDs: ["maya-chen"])]
                )
            ),
        ]

        chat.deleteMessagesForEveryone(
            ["incoming", "outgoing"],
            currentProfileID: "marmota"
        )

        #expect(chat.messages[0].text == "Keep me")
        #expect(!chat.messages[0].isDeleted)
        #expect(chat.messages[1].isDeleted)
        #expect(chat.messages[1].text.isEmpty)
        #expect(chat.messages[1].attachments.isEmpty)
        #expect(chat.messages[1].reactions.isEmpty)
        #expect(chat.messages[1].replyToMessageID == nil)
    }

    @Test("Forwarding copies visible messages in order without conversation metadata")
    func forwardMessages() {
        var destination = emptyDirectChat()
        destination.draft = "Unsent"
        destination.replyToMessageID = "old"
        let messages = [
            PrototypeMessage(
                id: "first",
                authorID: "maya-chen",
                sentAt: .now,
                text: "First",
                reactions: [PrototypeReaction(emoji: "❤", personIDs: ["marmota"])]
            ),
            PrototypeMessage(
                id: "deleted",
                authorID: "maya-chen",
                sentAt: .now,
                text: "Deleted",
                deletionState: .deletedByOther
            ),
            PrototypeMessage(
                id: "second",
                authorID: "marmota",
                sentAt: .now,
                text: "Second",
                replyToMessageID: "first"
            ),
        ]

        destination.appendForwardedMessages(
            messages,
            authorID: "marmota",
            now: Date(timeIntervalSince1970: 1_000)
        )

        #expect(destination.messages.map(\.text) == ["First", "Second"])
        #expect(destination.messages.allSatisfy { $0.authorID == "marmota" })
        #expect(destination.messages.allSatisfy { $0.replyToMessageID == nil })
        #expect(destination.messages.allSatisfy { $0.reactions.isEmpty })
        #expect(destination.draft.isEmpty)
        #expect(destination.replyToMessageID == nil)
    }

    @Test("Failed outgoing delivery retries to sent")
    func failedDeliveryRetry() throws {
        var chat = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "catalog-direct-text" }
        )
        #expect(chat.messages.first { $0.id == "DLV-03" }?.deliveryState == .failed)
        chat.retryMessage("DLV-03", currentProfileID: "marmota")
        #expect(chat.messages.first { $0.id == "DLV-03" }?.deliveryState == .sent)
    }

    @Test("Gallery layouts cover one through seven with exact Signal-informed frames")
    func galleryLayouts() {
        let one = PrototypeMediaLayout(count: 1)
        let two = PrototypeMediaLayout(count: 2)
        let three = PrototypeMediaLayout(count: 3)
        let four = PrototypeMediaLayout(count: 4)
        let five = PrototypeMediaLayout(count: 5)
        let six = PrototypeMediaLayout(count: 6)
        let seven = PrototypeMediaLayout(count: 7)

        #expect(one.size == CGSize(width: 256, height: 256))
        #expect(one.frames == [CGRect(x: 0, y: 0, width: 256, height: 256)])
        #expect(two.size == CGSize(width: 256, height: 127))
        #expect(two.frames == [
            CGRect(x: 0, y: 0, width: 127, height: 127),
            CGRect(x: 129, y: 0, width: 127, height: 127),
        ])
        #expect(three.size == CGSize(width: 256, height: 170))
        #expect(three.frames == [
            CGRect(x: 0, y: 0, width: 170, height: 170),
            CGRect(x: 172, y: 0, width: 84, height: 84),
            CGRect(x: 172, y: 86, width: 84, height: 84),
        ])
        #expect(four.size == CGSize(width: 256, height: 256))
        #expect(four.frames == [
            CGRect(x: 0, y: 0, width: 127, height: 127),
            CGRect(x: 129, y: 0, width: 127, height: 127),
            CGRect(x: 0, y: 129, width: 127, height: 127),
            CGRect(x: 129, y: 129, width: 127, height: 127),
        ])
        let fiveTileFrames = [
            CGRect(x: 0, y: 0, width: 127, height: 127),
            CGRect(x: 129, y: 0, width: 127, height: 127),
            CGRect(x: 0, y: 129, width: 84, height: 84),
            CGRect(x: 86, y: 129, width: 84, height: 84),
            CGRect(x: 172, y: 129, width: 84, height: 84),
        ]
        #expect(five.size == CGSize(width: 256, height: 213))
        #expect(five.frames == fiveTileFrames)
        #expect(six.frames == fiveTileFrames)
        #expect(seven.frames == fiveTileFrames)
        #expect(six.overflowCount == 1)
        #expect(seven.overflowCount == 2)
        #expect(!five.isOverflowTile(at: 4))
        #expect(six.isOverflowTile(at: 4))
        #expect(!six.isOverflowTile(at: 3))
    }

    @Test("Single media ratios clamp and invalid dimensions fall back to square")
    func singleMediaSizing() {
        #expect(PrototypeMediaLayout.singleSize(dimensions: nil) == CGSize(width: 256, height: 256))
        #expect(PrototypeMediaLayout.singleSize(
            dimensions: PrototypeMediaDimensions(pixelWidth: 1_600, pixelHeight: 800)
        ) == CGSize(width: 256, height: 128))
        #expect(PrototypeMediaLayout.singleSize(
            dimensions: PrototypeMediaDimensions(pixelWidth: 1_080, pixelHeight: 1_920)
        ) == CGSize(width: 192, height: 256))
        #expect(PrototypeMediaLayout.singleSize(
            dimensions: PrototypeMediaDimensions(pixelWidth: 3_000, pixelHeight: 500)
        ).height == 256 / PrototypeMediaLayout.maximumAspectRatio)
        #expect(PrototypeMediaLayout.singleSize(
            dimensions: PrototypeMediaDimensions(pixelWidth: 40, pixelHeight: 40)
        ) == CGSize(width: 192, height: 192))
        #expect(PrototypeMediaDimensions(pixelWidth: 0, pixelHeight: 100) == nil)
        #expect(PrototypeMediaDimensions(pixelWidth: .infinity, pixelHeight: 100) == nil)
    }

    @Test("Media index preserves chronology and excludes deleted and unavailable viewer pages")
    func mediaIndex() throws {
        let chat = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "catalog-media-viewer" }
        )
        let all = PrototypeMediaIndex.allItems(in: chat)
        let available = PrototypeMediaIndex.availableItems(in: chat)
        #expect(all.map(\.messageID) == [
            "MED-VIEW-01", "MED-VIEW-02", "MED-VIEW-03", "MED-VIEW-04",
            "MED-VIEW-05", "MED-VIEW-07",
        ])
        #expect(available.map(\.messageID) == [
            "MED-VIEW-01", "MED-VIEW-02", "MED-VIEW-03", "MED-VIEW-04",
            "MED-VIEW-05",
        ])
        #expect(available.map(\.sentAt) == available.map(\.sentAt).sorted())

        let conversationSelection = try #require(PrototypeMediaSelection(
            chat: chat,
            messageID: available[2].messageID,
            attachmentID: available[2].attachmentID
        ))
        let chatInfoSelection = try #require(PrototypeMediaSelection(
            chat: chat,
            selectedItemID: available[2].id
        ))
        #expect(conversationSelection.items == chatInfoSelection.items)
        #expect(conversationSelection.initialItemID == chatInfoSelection.initialItemID)
        #expect(conversationSelection.initialIndex == 2)
        let unavailableItemID = try #require(all.last?.id)
        #expect(PrototypeMediaSelection(chat: chat, selectedItemID: unavailableItemID) == nil)
    }

    @Test("Composer availability follows membership, blocks, and per-chat relays")
    func composerAvailability() throws {
        var people = PrototypeChatFixtures.people()
        var chat = emptyDirectChat()
        #expect(chat.composerAvailability(currentProfileID: "marmota", people: people) == .available)
        chat.routing.relayURLs = []
        #expect(chat.composerAvailability(currentProfileID: "marmota", people: people) == .missingRelays)
        chat.routing = PrototypeChatRouting()
        let personIndex = try #require(
            people.firstIndex { $0.id == "maya-chen" }
        )
        people[personIndex].isBlocked = true
        #expect(chat.composerAvailability(currentProfileID: "marmota", people: people) == .blocked)
        chat.listState.membershipState = .invited
        #expect(chat.composerAvailability(currentProfileID: "marmota", people: people) == .pendingInvitation)
        chat.listState.membershipState = .removed
        #expect(chat.composerAvailability(currentProfileID: "marmota", people: people) == .removed)
    }

    @Test("Accepting chat invitations preserves history and activates participation")
    func acceptingInvitations() throws {
        var profile = PrototypeProfile.marmota
        let directIndex = try #require(
            profile.chats.firstIndex { $0.id == "catalog-direct-invitation" }
        )
        let groupIndex = try #require(
            profile.chats.firstIndex { $0.id == "catalog-group-invitation" }
        )
        let directTimeline = profile.chats[directIndex].timeline
        let groupTimelineCount = profile.chats[groupIndex].timeline.count
        let acceptedAt = Date(timeIntervalSince1970: 2_000)

        #expect(profile.chats[directIndex].invitedByPersonID == "avery-stone")
        #expect(profile.chats[groupIndex].invitedByPersonID == "maya-chen")
        #expect(profile.people.first { $0.id == "avery-stone" }?.name == "Avery Stone")
        let directInvitationRow = profile.chats[directIndex].row(
            people: profile.people,
            currentProfileID: profile.id
        )
        let groupInvitationRow = profile.chats[groupIndex].row(
            people: profile.people,
            currentProfileID: profile.id
        )
        #expect(directInvitationRow.visiblePreview == "Invited to chat by Avery Stone")
        #expect(groupInvitationRow.visiblePreview == "Invited to chat by Maya Chen")
        #expect(directInvitationRow.visiblePreviewAuthor == nil)
        #expect(groupInvitationRow.visiblePreviewAuthor == nil)
        #expect(directInvitationRow.isInvitationPending)
        #expect(groupInvitationRow.isInvitationPending)

        let didAcceptDirectInvitation = profile.chats[directIndex].acceptInvitation(
            currentProfileID: profile.id,
            now: acceptedAt
        )
        #expect(didAcceptDirectInvitation)
        #expect(profile.chats[directIndex].listState.membershipState == .active)
        #expect(profile.chats[directIndex].timeline == directTimeline)
        #expect(profile.chats[directIndex].invitedByPersonID == nil)

        let didAcceptGroupInvitation = profile.chats[groupIndex].acceptInvitation(
            currentProfileID: profile.id,
            now: acceptedAt
        )
        #expect(didAcceptGroupInvitation)
        #expect(profile.chats[groupIndex].listState.membershipState == .active)
        #expect(profile.chats[groupIndex].invitedByPersonID == nil)
        #expect(
            profile.chats[groupIndex].members.contains {
                $0.personID == profile.id && $0.role == .member
            }
        )
        #expect(profile.chats[groupIndex].timeline.count == groupTimelineCount + 1)
        guard case let .event(event)? = profile.chats[groupIndex].timeline.last else {
            Issue.record("Group acceptance must append a membership event.")
            return
        }
        #expect(event.kind == .memberJoined(personID: profile.id))
    }

    @Test("Declining a chat invitation removes only that pending chat")
    func decliningInvitation() {
        var profile = PrototypeProfile.marmota
        let initialCount = profile.chats.count

        let didDeclineInvitation = profile.declineChatInvitation(
            "catalog-direct-invitation"
        )
        #expect(didDeclineInvitation)
        #expect(profile.chats.count == initialCount - 1)
        #expect(!profile.chats.contains { $0.id == "catalog-direct-invitation" })
        let didDeclineActiveChat = profile.declineChatInvitation(
            "catalog-direct-text"
        )
        #expect(!didDeclineActiveChat)
        #expect(profile.chats.count == initialCount - 1)
    }

    @Test("Voice recording moves to review before explicit completion")
    func voiceStateMachine() {
        var state = PrototypeVoiceRecordingState.idle
        let start = Date(timeIntervalSince1970: 1)
        state.begin(at: start)
        #expect(state == .recording(startedAt: start))
        let didMoveToReview = state.moveToReview(id: "voice-review", duration: 3)
        #expect(didMoveToReview)
        #expect(state == .review(id: "voice-review", duration: 3))
        state.reset()
        #expect(state == .idle)
        let didMoveFromIdle = state.moveToReview(id: "voice-review", duration: 3)
        #expect(didMoveFromIdle == false)
    }

    @Test("Search covers text, sender, and attachment labels")
    func messageSearch() throws {
        let chat = try #require(PrototypeProfile.marmota.chats.first { $0.id == "maya-chen" })
        #expect(chat.matchingMessages(query: "Maya", people: PrototypeChatFixtures.people(), currentProfileID: "marmota").isEmpty == false)
        #expect(chat.matchingMessages(query: "Weekend Notes", people: PrototypeChatFixtures.people(), currentProfileID: "marmota").count == 1)
        #expect(chat.matchingMessages(query: "revised", people: PrototypeChatFixtures.people(), currentProfileID: "marmota").count == 1)
    }

    @Test("Maya is a chronological and complete direct-message showcase")
    func mayaShowcaseCatalog() throws {
        let chat = try #require(PrototypeProfile.marmota.chats.first { $0.id == "maya-chen" })
        let incoming = chat.messages.filter { $0.authorID == "maya-chen" }
        let outgoing = chat.messages.filter { $0.authorID == "marmota" }

        #expect(Set(chat.timeline.map(\.id)).count == chat.timeline.count)
        #expect(chat.timeline.map(\.date) == chat.timeline.map(\.date).sorted())
        #expect(incoming.contains { !$0.text.isEmpty && $0.text.count < 30 })
        #expect(outgoing.contains { !$0.text.isEmpty && $0.text.count < 30 })
        #expect(incoming.contains { $0.text.contains("\n") })
        #expect(outgoing.contains { $0.text.contains("\n") })
        #expect(incoming.contains { $0.text.count > 120 })
        #expect(outgoing.contains { $0.text.count > 120 })
        #expect(chat.messages.first { $0.id == "maya-3b" }?.replyToMessageID == "maya-3")
        #expect(chat.messages.first { $0.id == "maya-5" }?.replyToMessageID == "maya-4")
        #expect(chat.messages.first { $0.id == "maya-9b" }?.replyToMessageID == "maya-9")
        #expect(chat.messages.contains { $0.deletionState == .deletedByOther })
        #expect(chat.messages.contains { $0.deletionState == .deletedByCurrentProfile })
        #expect(chat.messages.contains { $0.deliveryState == .failed })
        #expect(chat.messages.contains { $0.reactions.count == 1 })
        #expect(chat.messages.contains { $0.reactions.count > 1 })

        let attachmentCounts = Set(chat.messages.map(\.attachments.count))
        #expect(attachmentCounts.isSuperset(of: [1, 2, 3]))
        #expect(chat.messages.flatMap(\.attachments).contains { if case .video = $0 { true } else { false } })
        #expect(chat.messages.flatMap(\.attachments).contains { if case .file = $0 { true } else { false } })
        #expect(chat.messages.flatMap(\.attachments).contains { if case .voice = $0 { true } else { false } })
        #expect(chat.messages.flatMap(\.attachments).contains { if case .link = $0 { true } else { false } })
        #expect(chat.messages.flatMap(\.attachments).allSatisfy { attachment in
            if case let .file(_, _, _, url) = attachment { return url != nil }
            if case let .video(_, url, _, _, _) = attachment { return url != nil }
            return true
        })
    }

    @Test("Weekend Walks is a chronological group and system-event showcase")
    func weekendWalksShowcaseCatalog() throws {
        let calendar = Calendar.autoupdatingCurrent
        let now = try #require(calendar.date(from: DateComponents(year: 2026, month: 8, day: 8, hour: 12)))
        let group = try #require(
            PrototypeChatFixtures.chats(
                profileID: "marmota",
                relayURLs: ["wss://relay.example.com"],
                now: now
            ).first { $0.id == "weekend-walks" }
        )

        #expect(Set(group.timeline.map(\.id)).count == group.timeline.count)
        #expect(group.timeline.map(\.date) == group.timeline.map(\.date).sorted())
        guard case let .event(firstEvent)? = group.timeline.first else {
            Issue.record("Weekend Walks must begin with its creation event.")
            return
        }
        #expect(firstEvent.kind == .groupCreated(actorID: "marmota"))

        let eventCopy = Set(group.timeline.compactMap { entry -> String? in
            guard case let .event(event) = entry else { return nil }
            return PrototypeChatEventFormatter.text(
                for: event.kind,
                profileID: "marmota",
                people: PrototypeChatFixtures.people()
            )
        })
        let requiredEvents: Set<String> = [
            "You created the group.",
            "You added Maya Chen and Elias Moreno.",
            "You added Nora Bennett.",
            "Mina Park joined the group.",
            "Leo Martins left the group.",
            "You removed Theo Grant.",
            "You made Maya Chen an admin.",
            "You removed Maya Chen as an admin.",
            "You changed the group name to Weekend Walks.",
            "You changed the group photo.",
            "You changed the group description.",
            "You removed the group description.",
        ]
        #expect(eventCopy.isSuperset(of: requiredEvents))

        let galleryCounts = Set(group.messages.map(\.attachments.count))
        #expect(galleryCounts.isSuperset(of: [4, 5, 6, 7]))
        let attachments = group.messages.flatMap(\.attachments)
        #expect(attachments.contains { if case .video = $0 { true } else { false } })
        #expect(attachments.contains { if case .file = $0 { true } else { false } })
        #expect(attachments.contains { if case .voice = $0 { true } else { false } })
        #expect(attachments.contains { if case .gif = $0 { true } else { false } })
        #expect(attachments.contains { if case .contact = $0 { true } else { false } })
        #expect(attachments.allSatisfy { attachment in
            if case let .file(_, _, _, url) = attachment { return url != nil }
            if case let .video(_, url, _, _, _) = attachment { return url != nil }
            return true
        })
        #expect(group.messages.contains { $0.text.contains("@Marmota") })
        #expect(group.messages.contains { $0.replyToMessageID != nil && $0.authorID == "marmota" })
        #expect(group.messages.contains { $0.replyToMessageID != nil && $0.authorID != "marmota" })
        #expect(!group.members.contains { $0.personID == "leo-martins" })
        #expect(!group.members.contains { $0.personID == "theo-grant" })

        let direct = try #require(
            PrototypeChatFixtures.chats(
                profileID: "marmota",
                relayURLs: ["wss://relay.example.com"],
                now: now
            ).first { $0.id == "maya-chen" }
        )
        let showcaseGalleryCounts = Set((direct.messages + group.messages).map(\.attachments.count))
        #expect(showcaseGalleryCounts.isSuperset(of: Set(1...7)))
        let showcaseReactionSet = Set(
            (direct.messages + group.messages).flatMap(\.reactions).map(\.emoji)
        )
        #expect(showcaseReactionSet.isSuperset(of: ["❤", "😀", "👍", "👎", "🤣", "🔥", "🦫"]))

        let separators = Set(group.timeline.map { PrototypeDateFormatter.separator(for: $0.date, now: now) })
        #expect(separators.contains("Today"))
        #expect(separators.contains("Yesterday"))
        #expect(separators.contains(where: { $0.contains("2025") }))
        let weekdayDate = try #require(calendar.date(byAdding: .day, value: -4, to: now))
        let recentDateFormatter = DateFormatter()
        recentDateFormatter.setLocalizedDateFormatFromTemplate("EE, MMM d")
        #expect(separators.contains(recentDateFormatter.string(from: weekdayDate)))
    }

    @Test("Ended groups do not retain the active profile as a current member")
    func endedGroupMembershipMatchesListState() throws {
        let chats = PrototypeProfile.marmota.chats
        let left = try #require(chats.first { $0.id == "book-club" })
        let removed = try #require(chats.first { $0.id == "quiet-studio" })

        #expect(left.listState.membershipState == .left)
        #expect(removed.listState.membershipState == .removed)
        #expect(!left.members.contains { $0.personID == "marmota" })
        #expect(!removed.members.contains { $0.personID == "marmota" })
    }

    @Test("Ended groups retain the membership-ending event in their timeline")
    func endedGroupTimelinesExplainMembershipState() throws {
        let profile = PrototypeProfile.marmota
        let left = try #require(profile.chats.first { $0.id == "book-club" })
        let removed = try #require(profile.chats.first { $0.id == "quiet-studio" })
        let leftEvent = try #require(left.timeline.compactMap { entry -> PrototypeTimelineEvent? in
            guard case let .event(event) = entry else { return nil }
            return event
        }.last)
        let removedEvent = try #require(removed.timeline.compactMap { entry -> PrototypeTimelineEvent? in
            guard case let .event(event) = entry else { return nil }
            return event
        }.last)

        #expect(leftEvent.kind == .memberLeft(personID: profile.id))
        #expect(removedEvent.kind == .memberRemoved(actorID: "maya-chen", personID: profile.id))
        #expect(
            PrototypeChatEventFormatter.text(
                for: leftEvent.kind,
                profileID: profile.id,
                people: profile.people
            ) == "You left the group."
        )
        #expect(
            PrototypeChatEventFormatter.text(
                for: removedEvent.kind,
                profileID: profile.id,
                people: profile.people
            ) == "Maya Chen removed you from the group."
        )
    }

    @Test("Every chat event formats current and other participants correctly")
    func chatEventFormattingMatrix() {
        let profileID = "marmota"
        let people = PrototypeChatFixtures.people()
        let cases: [(PrototypeChatEventKind, String)] = [
            (.directChatStarted(actorID: profileID), "You started the chat."),
            (.directChatStarted(actorID: "maya-chen"), "Maya Chen started the chat."),
            (.directChatLeft, "You left the chat."),
            (.groupCreated(actorID: profileID), "You created the group."),
            (.groupCreated(actorID: "maya-chen"), "Maya Chen created the group."),
            (.membersAdded(actorID: profileID, personIDs: ["maya-chen"]), "You added Maya Chen."),
            (.membersAdded(actorID: "maya-chen", personIDs: [profileID, "elias-moreno"]), "Maya Chen added you and Elias Moreno."),
            (.memberJoined(personID: profileID), "You joined the group."),
            (.memberJoined(personID: "maya-chen"), "Maya Chen joined the group."),
            (.memberLeft(personID: profileID), "You left the group."),
            (.memberLeft(personID: "maya-chen"), "Maya Chen left the group."),
            (.memberRemoved(actorID: profileID, personID: "maya-chen"), "You removed Maya Chen."),
            (.memberRemoved(actorID: "maya-chen", personID: "elias-moreno"), "Maya Chen removed Elias Moreno."),
            (.memberRemoved(actorID: "maya-chen", personID: profileID), "Maya Chen removed you from the group."),
            (.adminGranted(actorID: profileID, personID: "maya-chen"), "You made Maya Chen an admin."),
            (.adminGranted(actorID: "maya-chen", personID: profileID), "Maya Chen made you an admin."),
            (.adminRevoked(actorID: profileID, personID: "maya-chen"), "You removed Maya Chen as an admin."),
            (.adminRevoked(actorID: "maya-chen", personID: profileID), "Maya Chen removed you as an admin."),
            (.groupNameChanged(actorID: profileID, name: "River Walks"), "You changed the group name to River Walks."),
            (.groupPhotoChanged(actorID: "maya-chen"), "Maya Chen changed the group photo."),
            (.groupPhotoRemoved(actorID: profileID), "You removed the group photo."),
            (.groupDescriptionChanged(actorID: "maya-chen"), "Maya Chen changed the group description."),
            (.groupDescriptionRemoved(actorID: profileID), "You removed the group description."),
            (.disappearingMessagesChanged(actorID: profileID, duration: .oneDay), "You set disappearing messages to 1 Day."),
            (.disappearingMessagesChanged(actorID: "maya-chen", duration: .oneWeek), "Maya Chen set disappearing messages to 1 Week."),
            (.disappearingMessagesChanged(actorID: profileID, duration: .fourWeeks), "You set disappearing messages to 4 Weeks."),
            (.disappearingMessagesChanged(actorID: "maya-chen", duration: .off), "Maya Chen turned off disappearing messages."),
        ]

        for (kind, expected) in cases {
            #expect(
                PrototypeChatEventFormatter.text(
                    for: kind,
                    profileID: profileID,
                    people: people
                ) == expected
            )
        }
    }

    @Test("Direct and group exit transitions append their distinct terminal events")
    func exitTransitionsUseChatAndGroupCopy() throws {
        let now = Date(timeIntervalSince1970: 2_000)
        var direct = emptyDirectChat()
        let leftDirect = direct.leave(currentProfileID: "marmota", now: now)
        #expect(leftDirect)
        guard case let .event(directEvent)? = direct.timeline.last else {
            Issue.record("Direct leave must append a timeline event.")
            return
        }
        #expect(directEvent.kind == .directChatLeft)
        #expect(
            PrototypeChatEventFormatter.text(
                for: directEvent.kind,
                profileID: "marmota",
                people: PrototypeChatFixtures.people()
            ) == "You left the chat."
        )

        var group = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "catalog-group-events" }
        )
        let leftGroup = group.leave(currentProfileID: "marmota", now: now)
        #expect(leftGroup)
        guard case let .event(groupEvent)? = group.timeline.last else {
            Issue.record("Group leave must append a timeline event.")
            return
        }
        #expect(groupEvent.kind == .memberLeft(personID: "marmota"))
        #expect(
            PrototypeChatEventFormatter.text(
                for: groupEvent.kind,
                profileID: "marmota",
                people: PrototypeChatFixtures.people()
            ) == "You left the group."
        )
    }

    @Test("Disappearing-message changes append one event only when the value changes")
    func disappearingMessageTimelineChanges() throws {
        var group = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "catalog-group-events" }
        )
        group.timeline = []
        group.disappearingMessageDuration = .off

        let changedToOneDay = group.setDisappearingMessages(
            .oneDay,
            actorID: "marmota",
            now: Date(timeIntervalSince1970: 1)
        )
        let repeatedOneDay = group.setDisappearingMessages(
            .oneDay,
            actorID: "marmota",
            now: Date(timeIntervalSince1970: 2)
        )
        let changedToOff = group.setDisappearingMessages(
            .off,
            actorID: "maya-chen",
            now: Date(timeIntervalSince1970: 3)
        )
        #expect(changedToOneDay)
        #expect(!repeatedOneDay)
        #expect(changedToOff)
        #expect(group.timeline.count == 2)
        guard case let .event(offEvent)? = group.timeline.last else {
            Issue.record("Turning disappearing messages off must append an event.")
            return
        }
        #expect(offEvent.kind == .disappearingMessagesChanged(actorID: "maya-chen", duration: .off))
        #expect(
            PrototypeChatEventFormatter.text(
                for: offEvent.kind,
                profileID: "marmota",
                people: PrototypeChatFixtures.people()
            ) == "Maya Chen turned off disappearing messages."
        )
    }

    @Test("Group photo removal is distinct and emits exactly one event")
    func groupPhotoRemovalEvent() throws {
        var group = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "catalog-group-events" }
        )
        group.timeline = []
        group.avatar = .imageData(Data([0x01]))

        let removedPhoto = group.updateGroupPhoto(
            .monogram("G"),
            actorID: "marmota",
            now: Date(timeIntervalSince1970: 1)
        )
        let repeatedRemoval = group.updateGroupPhoto(
            .monogram("G"),
            actorID: "marmota",
            now: Date(timeIntervalSince1970: 2)
        )
        #expect(removedPhoto)
        #expect(!repeatedRemoval)
        #expect(group.timeline.count == 1)
        guard case let .event(event)? = group.timeline.first else {
            Issue.record("Removing a group photo must append an event.")
            return
        }
        #expect(event.kind == .groupPhotoRemoved(actorID: "marmota"))
    }

    @Test("Mentions filter group members only")
    func mentions() throws {
        let group = try #require(PrototypeProfile.marmota.chats.first { $0.id == "weekend-walks" })
        let candidates = group.mentionCandidates(query: "ma", people: PrototypeChatFixtures.people(), currentProfileID: "marmota")
        #expect(candidates.map(\.id) == ["maya-chen"])
    }

    @Test("Last-admin protection and successful leave")
    func leaveSafety() {
        var group = PrototypeChat(
            id: "g",
            kind: .group,
            groupName: "Group",
            groupDescription: "",
            avatar: .monogram("G"),
            members: [PrototypeGroupMember(personID: "marmota", role: .admin)],
            routing: PrototypeChatRouting(),
            timeline: [],
            emptyPreview: "",
            draft: "",
            replyToMessageID: nil,
            listState: PrototypeChatListState()
        )
        #expect(group.leave(currentProfileID: "marmota") == false)
        group.members.append(PrototypeGroupMember(personID: "maya-chen", role: .admin))
        #expect(group.leave(currentProfileID: "marmota") == true)
        #expect(group.listState.membershipState == .left)
        #expect(!group.members.contains { $0.personID == "marmota" })
    }

    @Test("Group permissions protect ordinary members and apply admin mutations")
    func groupPermissionMatrix() throws {
        var group = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "weekend-walks" }
        )
        #expect(!group.canManageGroup(currentProfileID: "maya-chen"))
        let ordinaryAdd = group.addMembers(personIDs: ["avery-stone"], actorID: "maya-chen")
        let ordinaryPromote = group.promoteMember(personID: "maya-chen", actorID: "maya-chen")
        let ordinaryRemove = group.removeMember(personID: "marmota", actorID: "maya-chen")
        #expect(!ordinaryAdd)
        #expect(!ordinaryPromote)
        #expect(!ordinaryRemove)

        let adminAdd = group.addMembers(personIDs: ["avery-stone"], actorID: "marmota")
        let adminPromote = group.promoteMember(personID: "maya-chen", actorID: "marmota")
        let adminDemote = group.demoteMember(personID: "maya-chen", actorID: "marmota")
        let adminRemove = group.removeMember(personID: "avery-stone", actorID: "marmota")
        let selfRemove = group.removeMember(personID: "marmota", actorID: "marmota")
        #expect(adminAdd)
        #expect(adminPromote)
        #expect(adminDemote)
        #expect(adminRemove)
        #expect(!selfRemove)
    }

    @Test("Every attachment class has a deterministic row projection")
    func attachmentRowProjections() {
        let people = PrototypeChatFixtures.people()
        let photo = PrototypeAttachment.photo(id: "p", source: .asset("Photo"), label: "Photo")
        let video = PrototypeAttachment.video(id: "v", url: nil, thumbnail: .asset("Video"), duration: 1)
        let file = PrototypeAttachment.file(id: "f", name: "Notes.pdf", size: 10, url: nil)
        let voice = PrototypeAttachment.voice(id: "a", resourceName: "voice", duration: 1)
        let link = PrototypeAttachment.link(id: "l", title: "Title", domain: "example.com", summary: "Summary", image: nil)
        let gif = PrototypeAttachment.gif(id: "g", assetName: "GIF", label: "GIF")
        let contact = PrototypeAttachment.contact(id: "c", personID: "maya-chen")

        #expect(photo.listPreview(people: people) == .photo)
        #expect(video.listPreview(people: people) == .video)
        #expect(file.listPreview(people: people) == .file("Notes.pdf"))
        #expect(voice.listPreview(people: people) == .voiceMessage)
        #expect(link.listPreview(people: people) == .link)
        #expect(gif.listPreview(people: people) == .gif)
        #expect(contact.listPreview(people: people) == .contact("Maya Chen"))
    }

    @Test("Calendar-date fixtures have plausible message times and Support keeps its stable row date")
    func fixtureDateFidelity() throws {
        let calendar = Calendar.autoupdatingCurrent
        let now = try #require(
            calendar.date(from: DateComponents(year: 2026, month: 8, day: 8, hour: 18))
        )
        let chats = PrototypeChatFixtures.chats(
            profileID: "marmota",
            relayURLs: ["wss://relay.example.com"],
            now: now
        )
        let calendarDateIDs = Set(
            ChatListFixtures.populated
                .filter { $0.timestamp.contains("/") }
                .map(\.id)
        )
        let datedMessages = chats
            .filter { calendarDateIDs.contains($0.id) }
            .flatMap(\.messages)
        #expect(!datedMessages.isEmpty)
        #expect(datedMessages.allSatisfy { calendar.component(.hour, from: $0.sentAt) != 0 })

        let support = try #require(chats.first { $0.id == ChatListFixtures.supportChatID })
        #expect(
            support.row(people: PrototypeChatFixtures.people(), currentProfileID: "marmota", now: now)
                .timestamp == "Thursday"
        )
    }

    @Test("Pinned date labels cover relative, recent, old, and future states")
    func pinnedDateLabels() throws {
        let calendar = Calendar.autoupdatingCurrent
        let now = try #require(calendar.date(from: DateComponents(year: 2024, month: 3, day: 20, hour: 12)))
        let yesterday = try #require(calendar.date(byAdding: .day, value: -1, to: now))
        let recent = try #require(calendar.date(byAdding: .day, value: -3, to: now))
        let old = try #require(calendar.date(byAdding: .day, value: -400, to: now))
        let future = try #require(calendar.date(byAdding: .day, value: 1, to: now))
        let recentDateFormatter = DateFormatter()
        recentDateFormatter.setLocalizedDateFormatFromTemplate("EE, MMM d")

        #expect(PrototypeDateFormatter.separator(for: now, now: now) == "Today")
        #expect(PrototypeDateFormatter.separator(for: yesterday, now: now) == "Yesterday")
        #expect(
            PrototypeDateFormatter.separator(for: recent, now: now)
                == recentDateFormatter.string(from: recent)
        )
        #expect(PrototypeDateFormatter.separator(for: old, now: now).contains("2023"))
        #expect(
            PrototypeDateFormatter.separator(for: future, now: now)
                == PrototypeDateFormatter.separator(for: now, now: now)
        )
    }

    @Test("Date scrolling fixture covers sparse and long day sections")
    func dateScrollingFixtureCoverage() throws {
        let calendar = Calendar.autoupdatingCurrent
        let now = try #require(
            calendar.date(from: DateComponents(year: 2026, month: 8, day: 12, hour: 12))
        )
        let chat = try #require(
            PrototypeChatFixtures.chats(
                profileID: "marmota",
                relayURLs: ["wss://relay.example"],
                now: now
            ).first { $0.id == "catalog-direct-dates" }
        )
        let messagesByDay = Dictionary(grouping: chat.messages) {
            calendar.startOfDay(for: $0.sentAt)
        }

        #expect(chat.messages.map(\.id) == (1...15).map {
            String(format: "DATE-%02d", $0)
        })
        #expect(messagesByDay.count == 8)
        #expect(messagesByDay.values.filter { $0.count == 1 }.count == 7)
        #expect(messagesByDay.values.contains { $0.count == 8 })
    }

    @Test("Group author colors derive stable palette assignments from public keys")
    func groupAuthorColorAssignments() {
        let people = PrototypeChatFixtures.people()
        let assignments = people.map {
            PrototypeAuthorNameColor.paletteIndex(for: $0.publicKey)
        }

        #expect(PrototypeAuthorNameColor.paletteCount == 9)
        #expect(assignments.allSatisfy { (0..<PrototypeAuthorNameColor.paletteCount).contains($0) })
        #expect(assignments == people.map {
            PrototypeAuthorNameColor.paletteIndex(for: $0.publicKey)
        })
        #expect(Set(assignments).count == PrototypeAuthorNameColor.paletteCount)
    }

    @Test("Identity color group exposes every palette bucket with monogram avatars")
    func identityColorFixtureCoverage() throws {
        let people = PrototypeChatFixtures.people().filter {
            $0.id.hasPrefix("identity-color-")
        }
        let chat = try #require(
            PrototypeChatFixtures.chats(
                profileID: "marmota",
                relayURLs: ["wss://relay.example"]
            ).first { $0.id == "catalog-group-colors" }
        )
        let messagesChat = try #require(
            PrototypeChatFixtures.chats(
                profileID: "marmota",
                relayURLs: ["wss://relay.example"]
            ).first { $0.id == "catalog-group-messages" }
        )

        #expect(people.count == PrototypeAuthorNameColor.paletteCount)
        #expect(people.map { PrototypeAuthorNameColor.paletteIndex(for: $0.publicKey) }
            == Array(0..<PrototypeAuthorNameColor.paletteCount))
        #expect(people.allSatisfy {
            if case let .monogram(value) = $0.avatar { return value.count == 1 }
            return false
        })
        #expect(Set(people.map(\.id)).isSubset(of: Set(chat.members.map(\.personID))))
        #expect(Set(chat.messages.map(\.id)).isSuperset(of: Set((1...9).map {
            String(format: "COLOR-%02d", $0)
        })))
        #expect(messagesChat.messages.allSatisfy { !$0.id.hasPrefix("COLOR-") })
    }

    @Test("Chat-list timestamps derive from activity dates and visible content")
    func chatListTimestampProjection() throws {
        let calendar = Calendar.autoupdatingCurrent
        let now = try #require(calendar.date(from: DateComponents(year: 2024, month: 3, day: 20, hour: 12)))
        let earlier = try #require(calendar.date(byAdding: .day, value: -3, to: now))
        var chat = emptyDirectChat()
        chat.timeline = [
            .message(PrototypeMessage(id: "earlier", authorID: "maya-chen", sentAt: earlier, text: "Earlier")),
            .message(PrototypeMessage(id: "latest", authorID: "marmota", sentAt: now.addingTimeInterval(-120), text: "Latest")),
        ]

        var row = chat.row(
            people: PrototypeChatFixtures.people(),
            currentProfileID: "marmota",
            now: now
        )
        #expect(row.timestamp == "2m")

        chat.deleteMessage("latest", currentProfileID: "marmota")
        row = chat.row(
            people: PrototypeChatFixtures.people(),
            currentProfileID: "marmota",
            now: now
        )
        #expect(row.preview == "Earlier")
        #expect(row.timestamp == earlier.formatted(.dateTime.weekday(.wide)))
    }

    @Test("A group event becomes authoritative row activity")
    func groupEventRowProjection() throws {
        let now = Date(timeIntervalSince1970: 2_000_000_000)
        var group = try #require(
            PrototypeProfile.marmota.chats.first { $0.id == "weekend-walks" }
        )
        group.appendEvent(
            .groupNameChanged(actorID: "marmota", name: "River Walks"),
            now: now
        )

        let row = group.row(
            people: PrototypeChatFixtures.people(),
            currentProfileID: "marmota",
            now: now
        )
        #expect(row.preview == "You changed the group name to River Walks.")
        #expect(row.previewAuthor == nil)
        #expect(row.timestamp == "Now")
    }

    @Test("Chat relays normalize, reject duplicates, allow final removal, and stay independent")
    func chatRelays() {
        var first = PrototypeChatRouting(relayURLs: ["wss://Relay.Example.com/"])
        let second = first
        #expect(first.relayURLs == ["wss://relay.example.com"])
        #expect(first.add("wss://relay.example.com/") == false)
        #expect(first.add("https://relay.example.com") == false)
        #expect(first.add("wss://second.example.com/path/") == true)
        first.remove("wss://relay.example.com")
        first.remove("wss://second.example.com/path")
        #expect(first.relayURLs.isEmpty)
        #expect(!first.isDefaultConfiguration)
        first.restoreDefaults()
        #expect(first.relayURLs == ["wss://relay.example.com"])
        #expect(first.isDefaultConfiguration)
        #expect(second.relayURLs == ["wss://relay.example.com"])
    }

    @Test("Message clusters respect author, day, and five-minute gap")
    func messageGrouping() {
        let base = Date(timeIntervalSince1970: 10_000)
        let a = PrototypeMessage(id: "a", authorID: "maya", sentAt: base)
        let b = PrototypeMessage(id: "b", authorID: "maya", sentAt: base.addingTimeInterval(299))
        let c = PrototypeMessage(id: "c", authorID: "maya", sentAt: base.addingTimeInterval(301))
        let d = PrototypeMessage(id: "d", authorID: "other", sentAt: base.addingTimeInterval(30))
        #expect(PrototypeMessageGrouping.belongsToSameCluster(a, b))
        #expect(!PrototypeMessageGrouping.belongsToSameCluster(a, c))
        #expect(!PrototypeMessageGrouping.belongsToSameCluster(a, d))
    }

    private func emptyDirectChat() -> PrototypeChat {
        PrototypeChat(
            id: "direct",
            kind: .direct(personID: "maya-chen"),
            groupName: "Maya Chen",
            groupDescription: "",
            avatar: .monogram("M"),
            members: [],
            routing: PrototypeChatRouting(),
            timeline: [],
            emptyPreview: "No messages yet.",
            draft: "",
            replyToMessageID: nil,
            listState: PrototypeChatListState()
        )
    }
}
