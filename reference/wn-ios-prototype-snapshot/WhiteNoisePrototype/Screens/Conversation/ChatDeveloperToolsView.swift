import SwiftUI
import UIKit

struct ChatDeveloperToolsView: View {
    @Binding var profile: PrototypeProfile
    let settings: PrototypeSettingsState
    let chatID: String

    @State private var copiedValue = ""

    var body: some View {
        Group {
            switch access {
            case .unavailable:
                unavailableContent
            case .disabled:
                disabledContent
            case let .enabled(info):
                inspectorForm(info)
            }
        }
        .navigationTitle("Chat Developer Tools")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.success, trigger: copiedValue)
    }

    private var access: PrototypeConversationDebugAccess {
        .resolve(
            profile: profile,
            chatID: chatID,
            nativePushEnabled: settings.nativePushEnabled
        )
    }

    private var unavailableContent: some View {
        ContentUnavailableView(
            "Chat Unavailable",
            systemImage: "bubble.left",
            description: Text(
                "This conversation is no longer available for inspection."
            )
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
    }

    private var disabledContent: some View {
        ContentUnavailableView {
            Label(
                "Conversation Debugging Is Off",
                systemImage: "wrench.and.screwdriver"
            )
        } description: {
            Text(
                "Turn on Developer Tools and Debug Mode for this "
                    + "profile to inspect this chat."
            )
        } actions: {
            NavigationLink {
                DeveloperToolsPrototypeView(
                    developerTools: $profile.developerTools,
                    profile: profile
                )
            } label: {
                Text("Open Developer Tools")
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
    }

    private func inspectorForm(_ info: PrototypeConversationDebugInfo) -> some View {
        Form {
            Section("Conversation") {
                LabeledContent("State", value: info.lifecycle)
                LabeledContent("Epoch", value: info.epoch.formatted())

                if let memberCount = info.memberCount {
                    LabeledContent("MLS Members", value: memberCount.formatted())
                }
                if let adminCount = info.adminCount {
                    LabeledContent("Admins", value: adminCount.formatted())
                }
                if let currentRole = info.currentRole {
                    LabeledContent("Your Role", value: currentRole)
                }

                NavigationLink {
                    RequiredEventKindsView(kinds: info.requiredEventKinds)
                } label: {
                    LabeledContent(
                        "Event Kinds",
                        value: "\(info.requiredEventKinds.count) required"
                    )
                }

                copyableValue(
                    "MLS Group ID",
                    value: info.mlsGroupID
                )
                copyableValue(
                    "Nostr Group ID",
                    value: info.nostrGroupID
                )
            }

            Section("Delivery & Notifications") {
                statusRow(
                    "Chat Relays",
                    value: info.relayCount.formatted(),
                    isWarning: info.relayCount == 0
                )
                statusRow(
                    "Notifications",
                    value: info.push.notificationsEnabled ? "On" : "Off",
                    isWarning: !info.push.notificationsEnabled
                )
                statusRow(
                    "Push",
                    value: info.push.registrationStatus,
                    isWarning: info.push.registrationStatus != "Registered"
                )

                if info.push.staleTokenCount > 0 {
                    statusRow(
                        "Push Tokens",
                        value: "\(info.push.staleTokenCount) stale",
                        isWarning: true
                    )
                }
                if info.push.missingRelayHintCount > 0 {
                    statusRow(
                        "Relay Hints",
                        value: "\(info.push.missingRelayHintCount) missing",
                        isWarning: true
                    )
                }
            }

            Section("Diagnostics") {
                NavigationLink {
                    DiagnosticsPrototypeView(
                        diagnosticSummary: info.diagnosticSummary
                    )
                } label: {
                    Label("Diagnostics", systemImage: "stethoscope")
                }
            }
        }
    }

    private func statusRow(
        _ label: String,
        value: String,
        isWarning: Bool
    ) -> some View {
        LabeledContent(label) {
            Text(value)
                .foregroundStyle(isWarning ? Color.orange : Color.primary)
        }
    }

    private func copyableValue(
        _ label: String,
        value: String
    ) -> some View {
        LabeledContent(label) {
            Button {
                UIPasteboard.general.string = value
                copiedValue = value
            } label: {
                HStack(spacing: 6) {
                    Text(shortened(value))
                        .font(.caption.monospaced())
                    Image(
                        systemName: copiedValue == value
                            ? "checkmark"
                            : "doc.on.doc"
                    )
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Copy \(label)")
            .accessibilityValue(value)
        }
    }

    private func shortened(_ value: String) -> String {
        guard value.count > 22 else { return value }
        return "\(value.prefix(12))…\(value.suffix(6))"
    }
}

private struct RequiredEventKindsView: View {
    let kinds: [PrototypeRequiredEventKind]

    var body: some View {
        Form {
            Section {
                ForEach(kinds) { kind in
                    Text(kind.rawValue.formatted(.number.grouping(.never)))
                        .font(.body.monospaced())
                        .accessibilityLabel("Event kind \(kind.rawValue)")
                }
            } footer: {
                Text(
                    "These Nostr event kinds are required for this conversation."
                )
            }
        }
        .navigationTitle("Event Kinds")
        .navigationBarTitleDisplayMode(.inline)
    }
}
