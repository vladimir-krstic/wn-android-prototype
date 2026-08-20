import Foundation
import SwiftUI

enum PrototypeBuildMetadata {
    static let marmotKitName = "MarmotKit"
    static let marmotKitRevision = "790eb860"

    static var builtOn: String {
        "\(marmotKitName) (\(marmotKitRevision))"
    }
}

struct PrototypeAuditFile: Identifiable, Equatable {
    let id: String
    let filename: String
    var byteCount: Int
    let creationDate: Date
    let profileName: String

    static func fixtures(
        profileID: String,
        profileName: String
    ) -> [PrototypeAuditFile] {
        [
            PrototypeAuditFile(
                id: "audit-\(profileID)-01",
                filename: "audit-\(profileID)-20260806-01.jsonl",
                byteCount: 24_000,
                creationDate: Date(timeIntervalSince1970: 1_786_022_820),
                profileName: profileName
            ),
            PrototypeAuditFile(
                id: "audit-\(profileID)-02",
                filename: "audit-\(profileID)-20260805-01.jsonl",
                byteCount: 8_000,
                creationDate: Date(timeIntervalSince1970: 1_785_917_640),
                profileName: profileName
            ),
        ]
    }
}

struct PrototypeDeveloperToolsState: Equatable {
    var isEnabled = false
    var debugMode = false
    var anonymousTelemetry = false
    var auditLogging = false
    var auditFiles: [PrototypeAuditFile] = []
    var keyPackage = PrototypeKeyPackage.fixture

    static func fixtures(
        profileID: String = "marmota",
        profileName: String = "Marmota"
    ) -> PrototypeDeveloperToolsState {
        PrototypeDeveloperToolsState(
            auditFiles: PrototypeAuditFile.fixtures(
                profileID: profileID,
                profileName: profileName
            )
        )
    }

    var isConversationDebugEnabled: Bool {
        isEnabled && debugMode
    }

    var auditFileCount: Int {
        auditFiles.count
    }

    var auditLogTotalByteCount: Int {
        auditFiles.reduce(0) { partialResult, file in
            partialResult + file.byteCount
        }
    }

    var auditLogsContainData: Bool {
        auditFiles.contains { file in
            file.byteCount > 0
        }
    }

    mutating func setEnabled(_ isEnabled: Bool) {
        self.isEnabled = isEnabled

        guard !isEnabled else {
            return
        }

        debugMode = false
        anonymousTelemetry = false
        auditLogging = false
    }

    mutating func clearAuditLogContents() {
        for index in auditFiles.indices {
            auditFiles[index].byteCount = 0
        }
    }

    mutating func publishKeyPackage() {
        keyPackage = .publishedFixture
    }
}

struct PrototypeSettingsState {
    static let defaultAutoDownload: [
        PrototypeMediaType: PrototypeAutoDownloadLevel
    ] = [
        .photos: .wifi,
        .videos: .never,
        .audio: .wifi,
        .files: .never,
    ]

    var appearance = PrototypeAppearance.system
    var language = PrototypeLanguage.system
    var returnKeyBehavior = PrototypeReturnKeyBehavior.newLine
    var incomingMessageColor = PrototypeMessageColor.gray
    var outgoingMessageColor = PrototypeMessageColor.black

    var localNotificationsEnabled = true
    var nativePushEnabled = true
    var notificationPreview = PrototypeNotificationPreview.generic

    var mediaQuality = PrototypeMediaQuality.standard
    var autoDownload = PrototypeSettingsState.defaultAutoDownload

    var hideScreenInAppSwitcher = false
    var screenLockEnabled = false
    var autoLock = PrototypeAutoLock.immediately
    var deviceAuthenticationAvailability =
        PrototypeDeviceAuthenticationAvailability.faceID
}

enum PrototypeAppearance: String, CaseIterable, Identifiable {
    case system = "System"
    case light = "Light"
    case dark = "Dark"

    var id: Self { self }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

enum PrototypeLanguage: String, CaseIterable, Identifiable {
    case system
    case english
    case german
    case spanish
    case french
    case italian
    case portuguese
    case serbian

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .system: "System"
        case .english: "English"
        case .german: "German"
        case .spanish: "Spanish"
        case .french: "French"
        case .italian: "Italian"
        case .portuguese: "Portuguese"
        case .serbian: "Serbian"
        }
    }
}

enum PrototypeReturnKeyBehavior: String, CaseIterable, Identifiable {
    case newLine = "New Line"
    case send = "Send Message"

    var id: Self { self }
}

enum PrototypeMessageColor: String, CaseIterable, Identifiable {
    case black = "Black"
    case gray = "Gray"
    case blue = "Blue"
    case green = "Green"
    case purple = "Purple"
    case orange = "Orange"

    var id: Self { self }

    var color: Color {
        switch self {
        case .black: Color("AccentColor")
        case .gray: Color(uiColor: .systemGray5)
        case .blue: .blue
        case .green: .green
        case .purple: .purple
        case .orange: .orange
        }
    }

    var foregroundColor: Color {
        switch self {
        case .black:
            Color(uiColor: .systemBackground)
        case .gray:
            Color(uiColor: .label)
        case .blue, .green, .purple, .orange:
            .white
        }
    }
}

enum PrototypeNotificationPreview: String, CaseIterable, Identifiable {
    case senderAndMessage = "Sender and Message"
    case senderOnly = "Sender Only"
    case generic = "Generic"

    var id: Self { self }

    var example: String {
        switch self {
        case .senderAndMessage:
            "Maya Chen · Can you send the latest version?"
        case .senderOnly:
            "Maya Chen · New message"
        case .generic:
            "White Noise · New message"
        }
    }
}

enum PrototypeMediaQuality: String, CaseIterable, Identifiable {
    case standard = "Standard"
    case high = "High"

    var id: Self { self }
}

enum PrototypeMediaType: String, CaseIterable, Identifiable {
    case photos = "Photos"
    case videos = "Videos"
    case audio = "Audio"
    case files = "Files"

    var id: Self { self }
}

enum PrototypeAutoDownloadLevel: String, CaseIterable, Identifiable {
    case never = "Never"
    case wifi = "Wi-Fi"
    case wifiAndCellular = "Wi-Fi and Cellular"

    var id: Self { self }
}

enum PrototypeAutoLock: String, CaseIterable, Identifiable {
    case immediately = "Immediately"
    case oneMinute = "After 1 Minute"
    case fiveMinutes = "After 5 Minutes"
    case fifteenMinutes = "After 15 Minutes"

    var id: Self { self }
}

enum PrototypeDeviceAuthenticationAvailability {
    case faceID
    case passcode
    case passcodeRequired

    var canAuthenticate: Bool {
        self != .passcodeRequired
    }
}

enum PrototypeRelayConnectionState: Hashable {
    case connected
    case reconnecting
    case disconnected
}

enum PrototypeRelayCapability: Equatable {
    case readWrite
    case readOnly
}

enum PrototypeRelayUsage: String, CaseIterable, Identifiable {
    case profile = "Profile"
    case inbox = "Inbox"
    case chatMessages = "Chat Messages"

    var id: Self { self }

    var symbol: String {
        switch self {
        case .profile:
            "person.crop.circle"
        case .inbox:
            "tray"
        case .chatMessages:
            "message"
        }
    }

    var explanation: String {
        switch self {
        case .profile:
            "Publishes your profile and connection information."
        case .inbox:
            "Receives invitations to new chats and groups."
        case .chatMessages:
            "Used for messages in chats you create. Existing chats keep their current relays."
        }
    }

    var unavailableCapability: String {
        switch self {
        case .profile:
            "profile publishing"
        case .inbox:
            "chat invitations"
        case .chatMessages:
            "new chats"
        }
    }

    var recoveryCapability: String {
        switch self {
        case .profile:
            "publishing"
        case .inbox:
            "invitations"
        case .chatMessages:
            "new chats"
        }
    }

    var unavailableMessage: String {
        switch self {
        case .profile:
            "Profile changes can’t be published."
        case .inbox:
            "New chat and group invitations can’t arrive."
        case .chatMessages:
            "New chats can’t be created."
        }
    }
}

enum PrototypeRelayRoleAvailability: Equatable {
    case available
    case reconnecting
    case disconnected
    case unassigned

    var isAvailable: Bool {
        self == .available
    }
}

struct PrototypeRelayConfiguration: Equatable {
    var relays: [PrototypeRelay]

    static let defaultConfiguration = PrototypeRelayConfiguration(
        relays: PrototypeRelay.fixtures
    )

    static let fixtures = defaultConfiguration

    static let missingProfile = fixtures.missing([.profile])
    static let missingInbox = fixtures.missing([.inbox])
    static let missingChatMessages = fixtures.missing([.chatMessages])
    static let missingProfileAndInbox = fixtures.missing([
        .profile,
        .inbox,
    ])
    static let missingProfileAndChatMessages = fixtures.missing([
        .profile,
        .chatMessages,
    ])
    static let missingInboxAndChatMessages = fixtures.missing([
        .inbox,
        .chatMessages,
    ])
    static let missingAll = fixtures.missing(
        Set(PrototypeRelayUsage.allCases)
    )
    static let reconnectingOnly = fixtures.settingConnectionState(
        .reconnecting
    )
    static let fullyDisconnected = fixtures.settingConnectionState(
        .disconnected
    )

    var unavailableUsages: Set<PrototypeRelayUsage> {
        Set(
            PrototypeRelayUsage.allCases.filter { usage in
                !availability(for: usage).isAvailable
            }
        )
    }

    var unassignedUsages: Set<PrototypeRelayUsage> {
        usages(with: .unassigned)
    }

    var reconnectingUsages: Set<PrototypeRelayUsage> {
        usages(with: .reconnecting)
    }

    var disconnectedUsages: Set<PrototypeRelayUsage> {
        usages(with: .disconnected)
    }

    var needsAttention: Bool {
        !unavailableUsages.isEmpty
    }

    var isDefaultConfiguration: Bool {
        self == .defaultConfiguration
    }

    var unavailableSummary: String {
        Self.unavailableSummary(for: unavailableUsages)
    }

    var recoverySummary: String {
        guard needsAttention else {
            return ""
        }

        guard relays.contains(where: {
            $0.capability == .readWrite
        }) else {
            return "Add a relay to publish your profile, receive invitations, and start new chats."
        }

        let recoveryDetails = [
            Self.unassignedSummary(for: unassignedUsages),
            Self.disconnectedSummary(for: disconnectedUsages),
            Self.reconnectingSummary(for: reconnectingUsages),
        ].compactMap { $0 }
        let isTemporarilyUnavailable =
            unavailableUsages == reconnectingUsages

        return (
            recoveryDetails
                + [
                    Self.unavailableSummary(
                        for: unavailableUsages,
                        temporarily: isTemporarilyUnavailable
                    ),
                ]
        )
            .joined(separator: " ")
    }

    func availability(
        for usage: PrototypeRelayUsage
    ) -> PrototypeRelayRoleAvailability {
        let assignedRelays = relays.filter { relay in
            relay.capability == .readWrite
                && relay.usages.contains(usage)
        }

        guard !assignedRelays.isEmpty else {
            return .unassigned
        }

        if assignedRelays.contains(where: {
            $0.connectionState == .connected
        }) {
            return .available
        }

        if assignedRelays.contains(where: {
            $0.connectionState == .reconnecting
        }) {
            return .reconnecting
        }

        return .disconnected
    }

    func isAvailable(for usage: PrototypeRelayUsage) -> Bool {
        availability(for: usage).isAvailable
    }

    func hasConfiguredRelay(for usage: PrototypeRelayUsage) -> Bool {
        relays.contains { relay in
            relay.capability == .readWrite
                && relay.usages.contains(usage)
        }
    }

    func removalImpact(for relayID: String) -> Set<PrototypeRelayUsage> {
        guard let relay = relays.first(where: { $0.id == relayID }) else {
            return []
        }

        return Set(
            relay.usages.filter { usage in
                relays.count { candidate in
                    candidate.capability == .readWrite
                        && candidate.usages.contains(usage)
                } == 1
            }
        )
    }

    mutating func restoreDefaults() {
        self = .defaultConfiguration
    }

    static func unavailableSummary(
        for usages: Set<PrototypeRelayUsage>,
        temporarily: Bool = false
    ) -> String {
        let capabilities = PrototypeRelayUsage.allCases.compactMap { usage in
            usages.contains(usage) ? usage.recoveryCapability : nil
        }
        let formatted = ListFormatter.localizedString(
            byJoining: capabilities
        )

        guard !formatted.isEmpty else {
            return ""
        }

        let subject = formatted.prefix(1).uppercased() + formatted.dropFirst()
        let verb = usages == [.profile] ? "is" : "are"
        let qualifier = temporarily ? "temporarily " : ""
        return "\(subject) \(verb) \(qualifier)unavailable."
    }

    private static func unassignedSummary(
        for usages: Set<PrototypeRelayUsage>
    ) -> String? {
        let names = orderedRoleNames(for: usages)

        switch names.count {
        case 0:
            return nil
        case 1:
            return "Choose a relay for \(names[0])."
        default:
            return "Choose relays for \(formattedList(names))."
        }
    }

    private static func disconnectedSummary(
        for usages: Set<PrototypeRelayUsage>
    ) -> String? {
        let names = orderedRoleNames(for: usages)

        switch names.count {
        case 0:
            return nil
        case 1:
            return "No \(names[0]) relay is connected."
        case PrototypeRelayUsage.allCases.count:
            return "No assigned relay is connected."
        default:
            return "No relay for \(names.joined(separator: " or ")) is connected."
        }
    }

    private static func reconnectingSummary(
        for usages: Set<PrototypeRelayUsage>
    ) -> String? {
        let names = orderedRoleNames(for: usages)

        switch names.count {
        case 0:
            return nil
        case 1:
            return "\(names[0]) relays are reconnecting."
        case PrototypeRelayUsage.allCases.count:
            return "Your relays are reconnecting."
        default:
            return "Relays for \(formattedList(names)) are reconnecting."
        }
    }

    private static func orderedRoleNames(
        for usages: Set<PrototypeRelayUsage>
    ) -> [String] {
        PrototypeRelayUsage.allCases.compactMap { usage in
            usages.contains(usage) ? usage.rawValue : nil
        }
    }

    private static func formattedList(_ values: [String]) -> String {
        ListFormatter.localizedString(byJoining: values)
    }

    private func missing(
        _ usages: Set<PrototypeRelayUsage>
    ) -> PrototypeRelayConfiguration {
        var configuration = self

        for relayIndex in configuration.relays.indices {
            configuration.relays[relayIndex].usages.subtract(usages)
        }

        return configuration
    }

    private func usages(
        with availability: PrototypeRelayRoleAvailability
    ) -> Set<PrototypeRelayUsage> {
        Set(
            PrototypeRelayUsage.allCases.filter {
                self.availability(for: $0) == availability
            }
        )
    }

    private func settingConnectionState(
        _ state: PrototypeRelayConnectionState
    ) -> PrototypeRelayConfiguration {
        var configuration = self

        for relayIndex in configuration.relays.indices
        where configuration.relays[relayIndex].capability == .readWrite {
            configuration.relays[relayIndex].connectionState = state
        }

        return configuration
    }
}

struct PrototypeRelay: Identifiable, Equatable {
    let id: String
    var displayName: String
    var url: String
    var capability: PrototypeRelayCapability
    var connectionState: PrototypeRelayConnectionState
    var usages: Set<PrototypeRelayUsage>

    static let fixtures = [
        PrototypeRelay(
            id: "primal",
            displayName: "Primal",
            url: "wss://relay.primal.net",
            capability: .readWrite,
            connectionState: .connected,
            usages: [.profile, .inbox, .chatMessages]
        ),
        PrototypeRelay(
            id: "damus",
            displayName: "Damus",
            url: "wss://relay.damus.io",
            capability: .readWrite,
            connectionState: .connected,
            usages: [.profile, .chatMessages]
        ),
        PrototypeRelay(
            id: "nos-lol",
            displayName: "nos.lol",
            url: "wss://nos.lol",
            capability: .readWrite,
            connectionState: .connected,
            usages: [.profile, .inbox, .chatMessages]
        ),
        PrototypeRelay(
            id: "nostr-band",
            displayName: "Nostr.Band",
            url: "wss://relay.nostr.band",
            capability: .readWrite,
            connectionState: .connected,
            usages: [.profile]
        ),
        PrototypeRelay(
            id: "vertex",
            displayName: "Vertex",
            url: "wss://relay.vertexlab.io",
            capability: .readOnly,
            connectionState: .connected,
            usages: []
        ),
        PrototypeRelay(
            id: "white-noise-profile",
            displayName: "White Noise Profile",
            url: "wss://relay.whitenoise.chat",
            capability: .readWrite,
            connectionState: .reconnecting,
            usages: [.profile, .chatMessages]
        ),
        PrototypeRelay(
            id: "white-noise-inbox",
            displayName: "White Noise Inbox",
            url: "wss://inbox.whitenoise.chat",
            capability: .readWrite,
            connectionState: .disconnected,
            usages: [.inbox]
        ),
    ]
}

struct PrototypeKeyPackage: Identifiable, Equatable {
    let id: String
    let published: String
    let size: String

    static let fixture = PrototypeKeyPackage(
        id: "a17c2e93d8f4b1",
        published: "Today at 18:05",
        size: "4 KB"
    )

    static let publishedFixture = PrototypeKeyPackage(
        id: "d48e1a7c9b320f",
        published: "Just now",
        size: "4 KB"
    )
}
