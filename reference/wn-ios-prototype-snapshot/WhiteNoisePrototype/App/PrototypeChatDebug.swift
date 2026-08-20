import Foundation

enum PrototypeConversationDebugAccess: Equatable {
    case unavailable
    case disabled
    case enabled(PrototypeConversationDebugInfo)

    static func resolve(
        profile: PrototypeProfile,
        chatID: String,
        nativePushEnabled: Bool
    ) -> PrototypeConversationDebugAccess {
        guard profile.chats.contains(where: { $0.id == chatID }) else {
            return .unavailable
        }
        guard profile.developerTools.isConversationDebugEnabled else {
            return .disabled
        }
        guard let info = PrototypeConversationDebugInfo.snapshot(
            profile: profile,
            chatID: chatID,
            nativePushEnabled: nativePushEnabled
        ) else {
            return .unavailable
        }
        return .enabled(info)
    }
}

struct PrototypeConversationDebugInfo: Equatable {
    let chatID: String
    let title: String
    let lifecycle: String
    let memberCount: Int?
    let adminCount: Int?
    let epoch: Int
    let currentRole: String?
    let requiredEventKinds: [PrototypeRequiredEventKind]
    let mlsGroupID: String
    let nostrGroupID: String
    let relayCount: Int
    let push: PrototypeConversationPushDebugInfo

    static func snapshot(
        profile: PrototypeProfile,
        chatID: String,
        nativePushEnabled: Bool
    ) -> PrototypeConversationDebugInfo? {
        guard let chat = profile.chats.first(where: { $0.id == chatID }) else {
            return nil
        }

        let fixture = PrototypeConversationDebugFixtureFacts.facts(for: chat.id)
        let participantIDs = participantIDs(for: chat, profileID: profile.id)
        let lifecycle = lifecycleLabel(chat.listState.membershipState)
        let canRegister = chat.listState.membershipState == .active
            && !chat.routing.relayURLs.isEmpty
            && nativePushEnabled
        let staleTokenCount = fixture.staleTokenOffsets.filter {
            participantIDs.indices.contains($0)
        }.count

        return PrototypeConversationDebugInfo(
            chatID: chat.id,
            title: chat.title(people: profile.people),
            lifecycle: lifecycle,
            memberCount: chat.isGroup ? chat.members.count : nil,
            adminCount: chat.isGroup
                ? chat.members.filter { $0.role == .admin }.count
                : nil,
            epoch: fixture.epoch,
            currentRole: chat.isGroup
                ? currentRoleLabel(chat: chat, profileID: profile.id)
                : nil,
            requiredEventKinds: fixture.requiredEventKinds,
            mlsGroupID: fixture.mlsGroupID,
            nostrGroupID: fixture.nostrGroupID,
            relayCount: chat.routing.relayURLs.count,
            push: PrototypeConversationPushDebugInfo(
                notificationsEnabled: nativePushEnabled,
                registrationStatus: canRegister ? "Registered" : "Not Registered",
                staleTokenCount: staleTokenCount,
                missingRelayHintCount: chat.routing.relayURLs.isEmpty
                    ? participantIDs.count
                    : 0
            )
        )
    }

    var diagnosticSummary: String {
        var lines = [
            "Chat Diagnostic Summary",
            "Chat ID: \(chatID)",
            "Title: \(title)",
            "State: \(lifecycle)",
            "Epoch: \(epoch)",
            "Chat Relays: \(relayCount)",
            "Notifications: \(push.notificationsEnabled ? "On" : "Off")",
            "Push: \(push.registrationStatus)",
        ]
        if let memberCount {
            lines.append("MLS Members: \(memberCount)")
        }
        if let adminCount {
            lines.append("Admins: \(adminCount)")
        }
        if let currentRole {
            lines.append("Your Role: \(currentRole)")
        }
        if push.staleTokenCount > 0 {
            lines.append("Stale Push Tokens: \(push.staleTokenCount)")
        }
        if push.missingRelayHintCount > 0 {
            lines.append("Missing Relay Hints: \(push.missingRelayHintCount)")
        }
        lines.append(
            "Required Event Kinds: "
                + requiredEventKinds.map { String($0.rawValue) }
                    .joined(separator: ", ")
        )
        return lines.joined(separator: "\n")
    }

    private static func participantIDs(
        for chat: PrototypeChat,
        profileID: String
    ) -> [String] {
        let source: [String]
        switch chat.kind {
        case let .direct(personID):
            source = [profileID, personID]
        case .group:
            source = [profileID] + chat.members.map(\.personID)
        }
        return source.reduce(into: []) { result, participantID in
            guard !result.contains(participantID) else { return }
            result.append(participantID)
        }
    }

    private static func lifecycleLabel(
        _ state: ChatListItem.MembershipState
    ) -> String {
        switch state {
        case .invited: "Invitation Pending"
        case .active: "Active"
        case .left: "Left"
        case .removed: "Removed"
        }
    }

    private static func currentRoleLabel(
        chat: PrototypeChat,
        profileID: String
    ) -> String {
        if chat.listState.membershipState == .invited {
            return "Invited"
        }
        guard chat.listState.membershipState == .active else {
            return "Former Member"
        }
        switch chat.members.first(where: { $0.personID == profileID })?.role {
        case .admin: return "Admin"
        case .member: return "Member"
        case nil: return "Not a Member"
        }
    }
}

struct PrototypeRequiredEventKind: Identifiable, Equatable {
    let rawValue: Int

    var id: Int { rawValue }
}

struct PrototypeConversationPushDebugInfo: Equatable {
    let notificationsEnabled: Bool
    let registrationStatus: String
    let staleTokenCount: Int
    let missingRelayHintCount: Int
}

private struct PrototypeConversationDebugFixtureFacts {
    let epoch: Int
    let mlsGroupID: String
    let nostrGroupID: String
    let staleTokenOffsets: Set<Int>
    let requiredEventKinds: [PrototypeRequiredEventKind]

    static func facts(for chatID: String) -> PrototypeConversationDebugFixtureFacts {
        let seed = stableNumber(chatID)
        return PrototypeConversationDebugFixtureFacts(
            epoch: max(1, seed % 24),
            mlsGroupID: "mls-\(chatID)-\(seed)",
            nostrGroupID: "nostr-\(chatID)-\(seed + 41)",
            staleTokenOffsets: chatID == "weekend-walks" ? [4] : [],
            requiredEventKinds: [
                32769, 32771, 32772, 32774, 32777, 32779, 32780,
            ].map(PrototypeRequiredEventKind.init(rawValue:))
        )
    }

    private static func stableNumber(_ value: String) -> Int {
        value.utf8.enumerated().reduce(17) { result, item in
            (result &* 31 &+ Int(item.element) &+ item.offset) % 9_973
        }
    }
}
