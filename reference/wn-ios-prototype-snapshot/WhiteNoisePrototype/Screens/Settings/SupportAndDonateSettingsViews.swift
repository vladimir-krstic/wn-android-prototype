import SwiftUI
import UIKit

struct SupportPrototypeView: View {
    @State private var isShowingSupportConversation = false
    @Binding private var profile: PrototypeProfile
    @Binding private var settings: PrototypeSettingsState

    init(
        profile: Binding<PrototypeProfile> = .constant(.marmota),
        settings: Binding<PrototypeSettingsState> = .constant(PrototypeSettingsState())
    ) {
        _profile = profile
        _settings = settings
    }

    var body: some View {
        Form {
            Section {
                HStack(spacing: 12) {
                    WhiteNoiseSupportAvatar(size: 56)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("White Noise Support")
                            .font(.headline)

                        Text("Questions, problems, and suggestions")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityElement(children: .combine)
            } footer: {
                Text(
                    "Ask how something works, report a problem, or share a suggestion."
                )
            }

            Section {
                Button(action: openSupportChat) {
                    Label("Start Chat", systemImage: "plus.bubble")
                        .symbolRenderingMode(.monochrome)
                        .foregroundStyle(
                            Color(uiColor: .systemBackground)
                        )
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
                .disabled(!canOpenSupportChat)
                .accessibilityHint(startChatAccessibilityHint)
            }
        }
        .navigationTitle("Chat with support")
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(
            isPresented: $isShowingSupportConversation
        ) {
            ConversationView(
                profile: $profile,
                settings: $settings,
                chatID: ChatListFixtures.supportChatID
            )
        }
    }

    private var supportChatExists: Bool {
        profile.chats.contains(where: { chat in
            chat.id == ChatListFixtures.supportChatID
        })
    }

    private var canOpenSupportChat: Bool {
        supportChatExists
            || profile.relayConfiguration.isAvailable(for: .chatMessages)
    }

    private func openSupportChat() {
        isShowingSupportConversation = profile.openOrCreateSupportChat() != nil
    }

    private var startChatAccessibilityHint: String {
        if supportChatExists {
            return "Opens your existing conversation with White Noise Support."
        }

        if profile.relayConfiguration.isAvailable(for: .chatMessages) {
            return "Starts a conversation with White Noise Support."
        }

        return "Check your profile relays to start a support chat."
    }
}

struct DonatePrototypeView: View {
    @State private var selectedMethod = DonationMethod.lightning
    @State private var copiedMethod: DonationMethod?
    @State private var copyFeedbackTrigger = 0
    @State private var copyResetTask: Task<Void, Never>?

    var body: some View {
        Form {
            Section {
                donationIntroduction
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())

            donationSection(selectedMethod)
        }
        .navigationTitle("Donate")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Picker("Donation method", selection: $selectedMethod) {
                    ForEach(DonationMethod.allCases) { method in
                        Text(method.selectorTitle)
                            .tag(method)
                    }
                }
                .labelsHidden()
                .pickerStyle(.palette)
                .controlSize(.extraLarge)
                .frame(width: 180)
            }
        }
        .sensoryFeedback(.success, trigger: copyFeedbackTrigger)
        .onChange(of: selectedMethod) { _, _ in
            resetCopyFeedback()
        }
        .onDisappear {
            resetCopyFeedback()
        }
    }

    private var donationIntroduction: some View {
        VStack(spacing: 8) {
            Image(systemName: "heart")
                .font(.largeTitle)
                .foregroundStyle(.primary)

            Text("Support White Noise")
                .font(.headline)

            Text(
                "White Noise is free and open source. Donations help us improve it and keep it available to everyone."
            )
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
    }

    private func donationSection(
        _ method: DonationMethod
    ) -> some View {
        Section {
            VStack(spacing: 0) {
                if let image = QRCodeImageGenerator.image(
                    for: method.address,
                    removesQuietZone: true
                ) {
                    ShareableQRCodeView(
                        image: image,
                        accessibilityLabel: "\(method.title) QR code"
                    )
                }

                Button(action: { copy(method) }) {
                    CompactCopyValueLabel(
                        value: method.address,
                        isCopied: copiedMethod == method,
                        fillsAvailableWidth: true
                    )
                }
                .buttonStyle(.plain)
                .containerRelativeFrame(.horizontal) { length, _ in
                    (length * 0.81) - 16
                }
                .padding(.top, 18)
                .accessibilityLabel(
                    copiedMethod == method
                        ? "\(method.title) copied"
                        : "Copy \(method.title)"
                )

                Text(method.addressTitle)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 6)
            }
            .frame(maxWidth: .infinity)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets())
    }

    private func copy(_ method: DonationMethod) {
        UIPasteboard.general.string = method.address
        copiedMethod = method
        copyFeedbackTrigger += 1
        UIAccessibility.post(
            notification: .announcement,
            argument: "\(method.title) copied"
        )
        scheduleCopyReset()
    }

    private func scheduleCopyReset() {
        copyResetTask?.cancel()
        copyResetTask = Task { @MainActor in
            try? await Task.sleep(for: .seconds(2))

            guard !Task.isCancelled else {
                return
            }

            copiedMethod = nil
            copyResetTask = nil
        }
    }

    private func resetCopyFeedback() {
        copyResetTask?.cancel()
        copyResetTask = nil
        copiedMethod = nil
    }
}

private enum DonationMethod: CaseIterable, Equatable, Identifiable {
    case lightning
    case bitcoin

    var id: Self { self }

    var selectorTitle: String {
        switch self {
        case .lightning: "Lightning"
        case .bitcoin: "Bitcoin"
        }
    }

    var title: String {
        switch self {
        case .lightning: "Lightning Address"
        case .bitcoin: "Bitcoin Silent Payment Address"
        }
    }

    var addressTitle: String {
        switch self {
        case .lightning: "Lightning Address"
        case .bitcoin: "Bitcoin Silent Payment"
        }
    }

    var address: String {
        switch self {
        case .lightning:
            "whitenoise@donate.ipf.dev"
        case .bitcoin:
            "sp1qqvp56mxcj9pz9xudvlch5g4ah5hrc8rj6neu25p34rc9gxhp38cwqqlmld28u57w2srgckr34dkyg3q02phu8tm05cyj483q026xedp0s5f5j40p"
        }
    }

}

#Preview("Chat with Support") {
    NavigationStack {
        SupportPrototypeView()
    }
}

#Preview("Donate") {
    NavigationStack {
        DonatePrototypeView()
    }
}
