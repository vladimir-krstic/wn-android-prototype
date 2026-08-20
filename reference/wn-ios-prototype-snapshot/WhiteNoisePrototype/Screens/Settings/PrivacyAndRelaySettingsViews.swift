import Foundation
import SwiftUI

struct PrivacySecurityPrototypeView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Binding var settings: PrototypeSettingsState
    @State private var isShowingEraseAllAppData = false

    let profiles: [PrototypeProfile]
    let onEraseAllAppData: () -> Void

    var body: some View {
        Form {
            Section {
                Toggle(
                    "Hide Screen in App Switcher",
                    isOn: $settings.hideScreenInAppSwitcher
                )
                .tint(appSecurityToggleTint)
            } header: {
                Text("App Security")
            } footer: {
                Text(
                    "Hides your conversations and profile details in the app switcher."
                )
            }

            Section {
                Toggle(
                    "Require Face ID",
                    isOn: $settings.screenLockEnabled
                )
                .tint(appSecurityToggleTint)
                .disabled(
                    !settings.deviceAuthenticationAvailability.canAuthenticate
                )

                if settings.screenLockEnabled
                    && settings.deviceAuthenticationAvailability.canAuthenticate
                {
                    Picker("Auto-Lock", selection: $settings.autoLock) {
                        ForEach(PrototypeAutoLock.allCases) { period in
                            Text(period.rawValue)
                                .tag(period)
                        }
                    }
                }
            } footer: {
                switch settings.deviceAuthenticationAvailability {
                case .faceID:
                    Text(
                        "Locks White Noise when you leave. Your iPhone passcode can be used if Face ID isn't available."
                    )
                case .passcode:
                    Text(
                        "Face ID isn't set up. Use your iPhone passcode to unlock White Noise."
                    )
                case .passcodeRequired:
                    Text("Set an iPhone passcode to require Face ID.")
                }
            }

            Section {
                Button("Erase App Data", role: .destructive) {
                    isShowingEraseAllAppData = true
                }
                .accessibilityIdentifier("privacy.erase-all-app-data")
            } header: {
                Text("Device Data")
            } footer: {
                Text(
                    "Signs out every profile and permanently removes all White Noise data from this iPhone."
                )
            }

        }
        .navigationTitle("Privacy & Security")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $isShowingEraseAllAppData) {
            EraseAllAppDataView(
                profileIDs: profiles.map(\.id),
                onErase: onEraseAllAppData
            )
        }
    }

    private var appSecurityToggleTint: Color {
        colorScheme == .dark
            ? Color(uiColor: .systemGray)
            : .black
    }
}

struct RelaysPrototypeView: View {
    @Binding var configuration: PrototypeRelayConfiguration
    @State private var isShowingAddRelay = false
    @State private var isShowingRestoreDefaultsConfirmation = false

    var body: some View {
        Form {
            if configuration.needsAttention {
                Section {
                    Label {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Profile relays need attention")

                            Text(configuration.recoverySummary)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                    }
                }
            }

            Section {
                if configuration.relays.isEmpty {
                    Text("This profile has no relays")
                        .foregroundStyle(.secondary)
                }

                ForEach(configuration.relays) { relay in
                    NavigationLink {
                        RelayDetailView(
                            configuration: $configuration,
                            relayID: relay.id
                        )
                    } label: {
                        relayRow(relay)
                    }
                }

                Button {
                    isShowingAddRelay = true
                } label: {
                    Label("Add Relay", systemImage: "plus.circle")
                }
            } footer: {
                Text(
                    "Relays let your profile publish information, receive chat invitations, and deliver messages."
                )
            }

            Section {
                Button("Restore Default Relays", role: .destructive) {
                    isShowingRestoreDefaultsConfirmation = true
                }
                .disabled(configuration.isDefaultConfiguration)
            } footer: {
                Text(
                    "Restores the default relays and role assignments for this profile."
                )
            }
        }
        .navigationTitle("Relays")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $isShowingAddRelay) {
            AddRelaySheet(
                existingRelays: configuration.relays
            ) { url, usages in
                addRelay(url, usages: usages)
            }
            .presentationDetents([.medium])
        }
        .alert(
            "Restore default relays?",
            isPresented: $isShowingRestoreDefaultsConfirmation
        ) {
            Button("Restore Defaults", role: .destructive) {
                configuration.restoreDefaults()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "This replaces this profile’s relay list and role assignments with the defaults. Custom relays will be removed."
            )
        }
    }

    private func relayRow(_ relay: PrototypeRelay) -> some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text(relay.displayName)

                Text(relayDisplayURL(relay))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }

            Spacer()

            RelayConnectionStatusView(
                state: relay.connectionState
            )
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(relay.displayName), \(relay.url)")
        .accessibilityValue(relayAccessibilityValue(relay))
    }

    private func relayAccessibilityValue(_ relay: PrototypeRelay) -> String {
        let status: String

        switch relay.connectionState {
        case .connected:
            status = "Connected"
        case .reconnecting:
            status = "Reconnecting"
        case .disconnected:
            status = "Disconnected"
        }

        var values = [status]

        if relay.capability == .readOnly {
            values.append("Read Only")
        }

        return values.joined(separator: ", ")
    }

    private func relayDisplayURL(_ relay: PrototypeRelay) -> String {
        if relay.capability == .readOnly {
            return "\(relay.url) (Read Only)"
        }

        return relay.url
    }

    private func addRelay(
        _ url: String,
        usages: Set<PrototypeRelayUsage>
    ) {
        let relayID = "custom-\(url.lowercased())"
        let displayName = URL(string: url)?.host ?? "Custom Relay"

        configuration.relays.append(
            PrototypeRelay(
                id: relayID,
                displayName: displayName,
                url: url,
                capability: .readWrite,
                connectionState: .reconnecting,
                usages: usages
            )
        )

        Task {
            try? await Task.sleep(for: .seconds(1.5))

            guard let relayIndex = configuration.relays.firstIndex(
                where: { $0.id == relayID }
            ) else {
                return
            }

            withAnimation {
                configuration.relays[relayIndex].connectionState = .connected
            }
        }
    }
}

private func relayRemovalMessage(
    for impact: Set<PrototypeRelayUsage>
) -> String {
    guard !impact.isEmpty else {
        return "This profile will stop using this relay."
    }

    let roles = relayUsageList(impact)
    let requirement: String

    if impact.count == 1 {
        requirement = "This profile needs at least one \(roles) relay."
    } else {
        requirement = "This profile needs at least one relay for \(roles)."
    }

    return requirement
        + " If you remove this relay, \(relayCapabilityList(impact)) will be unavailable."
        + " You can choose another relay or restore defaults later."
}

private func relayFinalRoleMessage(
    for usage: PrototypeRelayUsage
) -> String {
    "This profile needs at least one \(usage.rawValue) relay for "
        + "\(usage.unavailableCapability). If you turn this off, "
        + "\(usage.unavailableCapability) will be unavailable. "
        + "You can choose another relay or restore defaults later."
}

private func relayUsageList(
    _ usages: Set<PrototypeRelayUsage>
) -> String {
    let names = PrototypeRelayUsage.allCases.compactMap { usage in
        usages.contains(usage) ? usage.rawValue : nil
    }

    return ListFormatter.localizedString(byJoining: names)
}

private func relayCapabilityList(
    _ usages: Set<PrototypeRelayUsage>
) -> String {
    let capabilities = PrototypeRelayUsage.allCases.compactMap { usage in
        usages.contains(usage) ? usage.unavailableCapability : nil
    }

    return ListFormatter.localizedString(byJoining: capabilities)
}

private struct RelayConnectionStatusView: View {
    let state: PrototypeRelayConnectionState

    var body: some View {
        Group {
            switch state {
            case .connected:
                Image(systemName: "checkmark.circle.fill")

            case .reconnecting:
                ProgressView()
                    .controlSize(.regular)
                    .tint(.secondary)

            case .disconnected:
                Image(systemName: "xmark.circle.fill")
            }
        }
        .foregroundStyle(statusStyle)
        .imageScale(.medium)
        .accessibilityHidden(true)
    }

    private var statusStyle: AnyShapeStyle {
        switch state {
        case .connected:
            return AnyShapeStyle(.green)
        case .reconnecting:
            return AnyShapeStyle(.secondary)
        case .disconnected:
            return AnyShapeStyle(.red)
        }
    }
}

private struct RelayDetailView: View {
    private enum RelayDetailAlert: Identifiable {
        case confirmDisableUsage(PrototypeRelayUsage)
        case confirmRemoval(
            relayID: String,
            relayName: String,
            impact: Set<PrototypeRelayUsage>
        )

        var id: String {
            switch self {
            case .confirmDisableUsage(let usage):
                "disable-\(usage.id)"
            case .confirmRemoval(let relayID, _, _):
                "remove-\(relayID)"
            }
        }

        var title: String {
            switch self {
            case .confirmDisableUsage(let usage):
                "Turn off \(usage.rawValue)?"
            case .confirmRemoval(_, let relayName, _):
                "Remove \(relayName)?"
            }
        }
    }

    @Binding var configuration: PrototypeRelayConfiguration
    @Environment(\.dismiss) private var dismiss
    @State private var relayAlert: RelayDetailAlert?
    @State private var pendingRemovalID: String?

    let relayID: String

    var body: some View {
        Group {
            if let relayIndex {
                Form {
                    Section {
                        LabeledContent(
                            "Name",
                            value: relay.displayName
                        )

                        LabeledContent("URL") {
                            Text(relay.url)
                                .font(.subheadline.monospaced())
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.trailing)
                        }

                        LabeledContent("Status") {
                            HStack {
                                Text(statusTitle)
                                    .foregroundStyle(.secondary)

                                RelayConnectionStatusView(
                                    state: relay.connectionState
                                )
                            }
                        }
                    }

                    Section {
                        ForEach(PrototypeRelayUsage.allCases) { usage in
                            Toggle(
                                isOn: usageBinding(
                                    usage,
                                    relayIndex: relayIndex
                                )
                            ) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(usage.rawValue)

                                    Text(usage.explanation)
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .disabled(
                                relay.capability == .readOnly
                            )
                            .accessibilityHint(
                                usageAccessibilityHint(usage)
                            )
                        }
                    } header: {
                        Text("Use For")
                    } footer: {
                        if relay.capability == .readOnly {
                            Text(
                                "This relay is read only, so this profile can’t use it to send data."
                            )
                        }
                    }

                    Section {
                        Button("Remove Relay", role: .destructive) {
                            relayAlert = .confirmRemoval(
                                relayID: relay.id,
                                relayName: relay.displayName,
                                impact: configuration.removalImpact(
                                    for: relay.id
                                )
                            )
                        }
                    }
                }
            } else {
                ContentUnavailableView(
                    "Relay Not Available",
                    systemImage: "antenna.radiowaves.left.and.right"
                )
            }
        }
        .navigationTitle("Relay")
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            relayAlert?.title ?? "",
            item: $relayAlert
        ) { alert in
            switch alert {
            case .confirmDisableUsage(let usage):
                Button("Turn Off", role: .destructive) {
                    disableUsage(usage)
                }

                Button("Cancel", role: .cancel) {}

            case .confirmRemoval(
                let relayID,
                _,
                _
            ):
                Button("Remove Relay", role: .destructive) {
                    pendingRemovalID = relayID
                    relayAlert = nil

                    Task { @MainActor in
                        await Task.yield()
                        dismiss()
                    }
                }

                Button("Cancel", role: .cancel) {}
            }
        } message: { alert in
            Text(relayAlertMessage(for: alert))
        }
        .onDisappear {
            guard let pendingRemovalID else {
                return
            }

            configuration.relays.removeAll {
                $0.id == pendingRemovalID
            }
            self.pendingRemovalID = nil
        }
    }

    private var relayIndex: Int? {
        configuration.relays.firstIndex { $0.id == relayID }
    }

    private var relay: PrototypeRelay {
        guard let relayIndex else {
            return PrototypeRelay(
                id: "missing",
                displayName: "Relay",
                url: "",
                capability: .readWrite,
                connectionState: .disconnected,
                usages: []
            )
        }

        return configuration.relays[relayIndex]
    }

    private var statusTitle: String {
        switch relay.connectionState {
        case .connected:
            "Connected"
        case .reconnecting:
            "Reconnecting"
        case .disconnected:
            "Disconnected"
        }
    }

    private func relayAlertMessage(
        for alert: RelayDetailAlert
    ) -> String {
        switch alert {
        case .confirmDisableUsage(let usage):
            relayFinalRoleMessage(for: usage)
        case .confirmRemoval(_, _, let impact):
            relayRemovalMessage(for: impact)
        }
    }

    private func usageBinding(
        _ usage: PrototypeRelayUsage,
        relayIndex: Int
    ) -> Binding<Bool> {
        Binding {
            configuration.relays[relayIndex].usages.contains(usage)
        } set: { isSelected in
            if isSelected {
                setUsage(usage, isEnabled: true)
            } else if isOnlySelection(usage) {
                relayAlert = .confirmDisableUsage(usage)
            } else {
                setUsage(usage, isEnabled: false)
            }
        }
    }

    private func disableUsage(_ usage: PrototypeRelayUsage) {
        setUsage(usage, isEnabled: false)
    }

    private func setUsage(
        _ usage: PrototypeRelayUsage,
        isEnabled: Bool
    ) {
        guard let relayIndex else {
            return
        }

        var updatedConfiguration = configuration

        if isEnabled {
            updatedConfiguration.relays[relayIndex].usages.insert(usage)
        } else {
            updatedConfiguration.relays[relayIndex].usages.remove(usage)
        }

        configuration = updatedConfiguration
    }

    private func isOnlySelection(
        _ usage: PrototypeRelayUsage
    ) -> Bool {
        relay.usages.contains(usage)
            && configuration.relays.count {
                $0.capability == .readWrite
                    && $0.usages.contains(usage)
            } == 1
    }

    private func usageAccessibilityHint(
        _ usage: PrototypeRelayUsage
    ) -> String {
        if relay.capability == .readOnly {
            return "This relay is read only."
        }

        if isOnlySelection(usage) {
            return "Turning this off makes \(usage.unavailableCapability) unavailable."
        }

        return usage.explanation
    }
}

private struct AddRelaySheet: View {
    let existingRelays: [PrototypeRelay]
    let onAdd: (String, Set<PrototypeRelayUsage>) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var relayURL = ""
    @State private var selectedUsages = Set(
        PrototypeRelayUsage.allCases
    )

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(
                        "wss://relay.example.com",
                        text: $relayURL
                    )
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                } header: {
                    Text("Relay URL")
                } footer: {
                    Text("Enter a relay URL beginning with wss://.")
                }

                Section("Use For") {
                    ForEach(PrototypeRelayUsage.allCases) { usage in
                        Toggle(
                            isOn: usageBinding(usage)
                        ) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(usage.rawValue)

                                Text(usage.explanation)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Add Relay")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(normalizedRelayURL, selectedUsages)
                        dismiss()
                    }
                    .buttonStyle(.glassProminent)
                    .disabled(!canAddRelay)
                }
            }
        }
    }

    private var canAddRelay: Bool {
        guard
            !normalizedRelayURL.contains(where: { $0.isWhitespace }),
            let components = URLComponents(string: normalizedRelayURL),
            components.scheme?.lowercased() == "wss",
            components.host?.isEmpty == false
        else {
            return false
        }

        return !selectedUsages.isEmpty
            && !existingRelays.contains {
                normalizedKey(for: $0.url)
                    == normalizedKey(for: normalizedRelayURL)
            }
    }

    private var normalizedRelayURL: String {
        var relay = relayURL.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        while relay.hasSuffix("/") {
            relay.removeLast()
        }

        return relay
    }

    private func normalizedKey(for relay: String) -> String {
        var key = relay.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()

        while key.hasSuffix("/") {
            key.removeLast()
        }

        return key
    }

    private func usageBinding(
        _ usage: PrototypeRelayUsage
    ) -> Binding<Bool> {
        Binding {
            selectedUsages.contains(usage)
        } set: { isSelected in
            if isSelected {
                selectedUsages.insert(usage)
            } else {
                selectedUsages.remove(usage)
            }
        }
    }
}

#Preview("Privacy & Security") {
    @Previewable @State var settings = PrototypeSettingsState()

    NavigationStack {
        PrivacySecurityPrototypeView(
            settings: $settings,
            profiles: PrototypeProfile.multipleProfileFixtures,
            onEraseAllAppData: {}
        )
    }
}

#Preview("Privacy & Security — Passcode Required") {
    @Previewable @State var settings = PrototypeSettingsState(
        deviceAuthenticationAvailability: .passcodeRequired
    )

    NavigationStack {
        PrivacySecurityPrototypeView(
            settings: $settings,
            profiles: PrototypeProfile.multipleProfileFixtures,
            onEraseAllAppData: {}
        )
    }
}

#Preview("Privacy & Security — Passcode Fallback") {
    @Previewable @State var settings = PrototypeSettingsState(
        deviceAuthenticationAvailability: .passcode
    )

    NavigationStack {
        PrivacySecurityPrototypeView(
            settings: $settings,
            profiles: PrototypeProfile.multipleProfileFixtures,
            onEraseAllAppData: {}
        )
    }
}

#Preview("Relays") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.fixtures

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Profile") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingProfile

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Inbox") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingInbox

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Chat Messages") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingChatMessages

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Profile and Inbox") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingProfileAndInbox

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Profile and Chat Messages") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingProfileAndChatMessages

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Inbox and Chat Messages") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingInboxAndChatMessages

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — All Roles") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.missingAll

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Reconnecting") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.reconnectingOnly

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}

#Preview("Relays Recovery — Disconnected") {
    @Previewable @State var configuration =
        PrototypeRelayConfiguration.fullyDisconnected

    NavigationStack {
        RelaysPrototypeView(configuration: $configuration)
    }
}
