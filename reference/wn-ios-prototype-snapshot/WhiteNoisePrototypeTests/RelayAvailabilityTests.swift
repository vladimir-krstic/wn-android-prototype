import Testing
@testable import WhiteNoisePrototype

@Suite("Relay availability")
struct RelayAvailabilityTests {
    @Test("Every role availability state is derived from assignments and connection")
    func roleAvailabilityStates() {
        let available = makeConfiguration(
            state: .connected,
            usages: [.profile]
        )
        let reconnecting = makeConfiguration(
            state: .reconnecting,
            usages: [.profile]
        )
        let disconnected = makeConfiguration(
            state: .disconnected,
            usages: [.profile]
        )
        let unassigned = PrototypeRelayConfiguration(relays: [])

        #expect(available.availability(for: .profile) == .available)
        #expect(
            reconnecting.availability(for: .profile) == .reconnecting
        )
        #expect(
            disconnected.availability(for: .profile) == .disconnected
        )
        #expect(unassigned.availability(for: .profile) == .unassigned)
    }

    @Test("A connected relay keeps its role available")
    func connectedRelayWins() {
        let configuration = PrototypeRelayConfiguration(
            relays: [
                relay(
                    id: "disconnected",
                    state: .disconnected,
                    usages: [.profile]
                ),
                relay(
                    id: "connected",
                    state: .connected,
                    usages: Set(PrototypeRelayUsage.allCases)
                ),
                relay(
                    id: "reconnecting",
                    state: .reconnecting,
                    usages: [.profile]
                ),
            ]
        )

        #expect(configuration.isAvailable(for: .profile))
        #expect(!configuration.needsAttention)
    }

    @Test("Reconnecting is immediately unavailable")
    func reconnectingIsUnavailable() {
        let configuration = PrototypeRelayConfiguration.reconnectingOnly

        #expect(configuration.needsAttention)
        #expect(
            configuration.reconnectingUsages
                == Set(PrototypeRelayUsage.allCases)
        )
        #expect(
            configuration.unavailableUsages
                == Set(PrototypeRelayUsage.allCases)
        )
    }

    @Test("Read-only relays never provide role availability")
    func readOnlyRelaysAreIgnored() {
        let configuration = PrototypeRelayConfiguration(
            relays: [
                relay(
                    id: "read-only",
                    state: .connected,
                    usages: [.profile],
                    capability: .readOnly
                ),
            ]
        )

        #expect(configuration.availability(for: .profile) == .unassigned)
        #expect(!configuration.hasConfiguredRelay(for: .profile))
    }

    @Test("Every unassigned role combination is deterministic")
    func unassignedRoleCombinations() {
        let scenarios: [(
            PrototypeRelayConfiguration,
            Set<PrototypeRelayUsage>
        )] = [
            (.fixtures, []),
            (.missingProfile, [.profile]),
            (.missingInbox, [.inbox]),
            (.missingChatMessages, [.chatMessages]),
            (.missingProfileAndInbox, [.profile, .inbox]),
            (
                .missingProfileAndChatMessages,
                [.profile, .chatMessages]
            ),
            (
                .missingInboxAndChatMessages,
                [.inbox, .chatMessages]
            ),
            (
                .missingAll,
                Set(PrototypeRelayUsage.allCases)
            ),
        ]

        for (configuration, expectedUnassignedUsages) in scenarios {
            #expect(
                configuration.unassignedUsages
                    == expectedUnassignedUsages
            )
            #expect(
                configuration.unavailableUsages
                    == expectedUnassignedUsages
            )
        }
    }

    @Test("Fully disconnected fixtures report every role")
    func fullyDisconnectedFixture() {
        let configuration = PrototypeRelayConfiguration.fullyDisconnected

        #expect(configuration.needsAttention)
        #expect(
            configuration.disconnectedUsages
                == Set(PrototypeRelayUsage.allCases)
        )
    }

    @Test("Removal impact depends on assignments, not connection")
    func removalImpact() {
        let relay = relay(
            id: "only-relay",
            state: .disconnected,
            usages: Set(PrototypeRelayUsage.allCases)
        )
        let configuration = PrototypeRelayConfiguration(relays: [relay])

        #expect(
            configuration.removalImpact(for: relay.id)
                == Set(PrototypeRelayUsage.allCases)
        )
        #expect(configuration.removalImpact(for: "missing") == [])
    }

    @Test("Restoring defaults restores connected role coverage")
    func restoringDefaults() {
        var configuration = PrototypeRelayConfiguration.missingAll
        configuration.relays.append(
            relay(
                id: "custom",
                state: .connected,
                usages: [.profile]
            )
        )

        configuration.restoreDefaults()

        #expect(configuration == .defaultConfiguration)
        #expect(configuration.isDefaultConfiguration)
        #expect(!configuration.relays.contains { $0.id == "custom" })
        #expect(configuration.unavailableUsages.isEmpty)
        #expect(!configuration.needsAttention)
    }

    @Test("Every role combination uses concise impact copy")
    func unavailableSummaries() {
        let scenarios: [(PrototypeRelayConfiguration, String)] = [
            (.fixtures, ""),
            (.missingProfile, "Publishing is unavailable."),
            (.missingInbox, "Invitations are unavailable."),
            (.missingChatMessages, "New chats are unavailable."),
            (
                .missingProfileAndInbox,
                "Publishing and invitations are unavailable."
            ),
            (
                .missingProfileAndChatMessages,
                "Publishing and new chats are unavailable."
            ),
            (
                .missingInboxAndChatMessages,
                "Invitations and new chats are unavailable."
            ),
            (
                .missingAll,
                "Publishing, invitations, and new chats are unavailable."
            ),
        ]

        for (configuration, expectedSummary) in scenarios {
            #expect(configuration.unavailableSummary == expectedSummary)
        }
    }

    @Test("Unassigned role combinations use grouped recovery copy")
    func unassignedRecoverySummaries() {
        let scenarios: [(PrototypeRelayConfiguration, String)] = [
            (
                .missingProfile,
                "Choose a relay for Profile. Publishing is unavailable."
            ),
            (
                .missingInbox,
                "Choose a relay for Inbox. Invitations are unavailable."
            ),
            (
                .missingChatMessages,
                "Choose a relay for Chat Messages. New chats are unavailable."
            ),
            (
                .missingProfileAndInbox,
                "Choose relays for Profile and Inbox. Publishing and invitations are unavailable."
            ),
            (
                .missingProfileAndChatMessages,
                "Choose relays for Profile and Chat Messages. Publishing and new chats are unavailable."
            ),
            (
                .missingInboxAndChatMessages,
                "Choose relays for Inbox and Chat Messages. Invitations and new chats are unavailable."
            ),
            (
                .missingAll,
                "Choose relays for Profile, Inbox, and Chat Messages. Publishing, invitations, and new chats are unavailable."
            ),
        ]

        for (configuration, expectedSummary) in scenarios {
            #expect(configuration.recoverySummary == expectedSummary)
        }
    }

    @Test("No assignable relays use one direct recovery sentence")
    func noAssignableRelaySummary() {
        let expected =
            "Add a relay to publish your profile, receive invitations, and start new chats."

        #expect(
            PrototypeRelayConfiguration(relays: []).recoverySummary
                == expected
        )
        #expect(
            PrototypeRelayConfiguration(
                relays: [
                    relay(
                        id: "read-only",
                        state: .connected,
                        usages: Set(PrototypeRelayUsage.allCases),
                        capability: .readOnly
                    ),
                ]
            ).recoverySummary == expected
        )
    }

    @Test("Reconnecting combinations use temporary grouped copy")
    func reconnectingRecoverySummaries() {
        let scenarios: [(Set<PrototypeRelayUsage>, String)] = [
            (
                [.profile],
                "Profile relays are reconnecting. Publishing is temporarily unavailable."
            ),
            (
                [.inbox],
                "Inbox relays are reconnecting. Invitations are temporarily unavailable."
            ),
            (
                [.chatMessages],
                "Chat Messages relays are reconnecting. New chats are temporarily unavailable."
            ),
            (
                [.profile, .inbox],
                "Relays for Profile and Inbox are reconnecting. Publishing and invitations are temporarily unavailable."
            ),
            (
                [.profile, .chatMessages],
                "Relays for Profile and Chat Messages are reconnecting. Publishing and new chats are temporarily unavailable."
            ),
            (
                [.inbox, .chatMessages],
                "Relays for Inbox and Chat Messages are reconnecting. Invitations and new chats are temporarily unavailable."
            ),
            (
                Set(PrototypeRelayUsage.allCases),
                "Your relays are reconnecting. Publishing, invitations, and new chats are temporarily unavailable."
            ),
        ]

        for (usages, expectedSummary) in scenarios {
            let configuration = availabilityConfiguration(
                state: .reconnecting,
                unavailableUsages: usages
            )
            #expect(configuration.recoverySummary == expectedSummary)
        }
    }

    @Test("Disconnected combinations use grouped connection copy")
    func disconnectedRecoverySummaries() {
        let scenarios: [(Set<PrototypeRelayUsage>, String)] = [
            (
                [.profile],
                "No Profile relay is connected. Publishing is unavailable."
            ),
            (
                [.inbox],
                "No Inbox relay is connected. Invitations are unavailable."
            ),
            (
                [.chatMessages],
                "No Chat Messages relay is connected. New chats are unavailable."
            ),
            (
                [.profile, .inbox],
                "No relay for Profile or Inbox is connected. Publishing and invitations are unavailable."
            ),
            (
                [.profile, .chatMessages],
                "No relay for Profile or Chat Messages is connected. Publishing and new chats are unavailable."
            ),
            (
                [.inbox, .chatMessages],
                "No relay for Inbox or Chat Messages is connected. Invitations and new chats are unavailable."
            ),
            (
                Set(PrototypeRelayUsage.allCases),
                "No assigned relay is connected. Publishing, invitations, and new chats are unavailable."
            ),
        ]

        for (usages, expectedSummary) in scenarios {
            let configuration = availabilityConfiguration(
                state: .disconnected,
                unavailableUsages: usages
            )
            #expect(configuration.recoverySummary == expectedSummary)
        }
    }

    @Test("Screenshot scenario groups unassigned and disconnected roles")
    func screenshotRecoverySummary() {
        let configuration = PrototypeRelayConfiguration(
            relays: [
                relay(
                    id: "inbox",
                    state: .disconnected,
                    usages: [.inbox]
                ),
            ]
        )

        #expect(
            configuration.recoverySummary
                == "Choose relays for Profile and Chat Messages. No Inbox relay is connected. Publishing, invitations, and new chats are unavailable."
        )
    }

    @Test("Mixed causes are ordered and never claim temporary recovery")
    func mixedRecoverySummary() {
        let configuration = PrototypeRelayConfiguration(
            relays: [
                relay(
                    id: "inbox",
                    state: .reconnecting,
                    usages: [.inbox]
                ),
                relay(
                    id: "messages",
                    state: .disconnected,
                    usages: [.chatMessages]
                ),
            ]
        )

        #expect(configuration.unassignedUsages == [.profile])
        #expect(configuration.reconnectingUsages == [.inbox])
        #expect(configuration.disconnectedUsages == [.chatMessages])
        #expect(
            configuration.recoverySummary
                == "Choose a relay for Profile. No Chat Messages relay is connected. Inbox relays are reconnecting. Publishing, invitations, and new chats are unavailable."
        )
    }

    @Test("Connection changes update availability immediately")
    func connectionChangesUpdateAvailability() {
        var configuration = makeConfiguration(
            state: .reconnecting,
            usages: Set(PrototypeRelayUsage.allCases)
        )

        #expect(configuration.needsAttention)

        configuration.relays[0].connectionState = .connected

        #expect(!configuration.needsAttention)
        #expect(configuration.unavailableUsages.isEmpty)

        configuration.relays[0].connectionState = .disconnected

        #expect(configuration.needsAttention)
        #expect(
            configuration.disconnectedUsages
                == Set(PrototypeRelayUsage.allCases)
        )
    }

    private func makeConfiguration(
        state: PrototypeRelayConnectionState,
        usages: Set<PrototypeRelayUsage>
    ) -> PrototypeRelayConfiguration {
        PrototypeRelayConfiguration(
            relays: [
                relay(id: "relay", state: state, usages: usages),
            ]
        )
    }

    private func availabilityConfiguration(
        state: PrototypeRelayConnectionState,
        unavailableUsages: Set<PrototypeRelayUsage>
    ) -> PrototypeRelayConfiguration {
        let availableUsages = Set(PrototypeRelayUsage.allCases)
            .subtracting(unavailableUsages)
        var relays: [PrototypeRelay] = []

        if !availableUsages.isEmpty {
            relays.append(
                relay(
                    id: "available",
                    state: .connected,
                    usages: availableUsages
                )
            )
        }

        if !unavailableUsages.isEmpty {
            relays.append(
                relay(
                    id: "unavailable",
                    state: state,
                    usages: unavailableUsages
                )
            )
        }

        return PrototypeRelayConfiguration(relays: relays)
    }

    private func relay(
        id: String,
        state: PrototypeRelayConnectionState,
        usages: Set<PrototypeRelayUsage>,
        capability: PrototypeRelayCapability = .readWrite
    ) -> PrototypeRelay {
        PrototypeRelay(
            id: id,
            displayName: "Relay",
            url: "wss://\(id).example.com",
            capability: capability,
            connectionState: state,
            usages: usages
        )
    }
}
