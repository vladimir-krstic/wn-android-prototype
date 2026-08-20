import SwiftUI

struct RelayWarningLink: View {
    @Binding var configuration: PrototypeRelayConfiguration

    var body: some View {
        NavigationLink {
            RelaysPrototypeView(configuration: $configuration)
        } label: {
            Image(systemName: "exclamationmark.triangle")
                .foregroundStyle(.orange)
        }
        .accessibilityLabel("Profile relays need attention")
        .accessibilityValue(configuration.recoverySummary)
        .accessibilityHint("Opens Relays.")
    }
}
