import SwiftUI
import UIKit

struct ShareAndConnectView: View {
    private enum Mode: String, CaseIterable, Identifiable {
        case share = "Share"
        case scan = "Connect"

        var id: Self { self }
    }

    @State private var mode = Mode.share

    let profile: PrototypeProfile

    private let qrImage: UIImage?

    init(profile: PrototypeProfile) {
        self.profile = profile
        qrImage = QRCodeImageGenerator.image(
            for: profile.publicKey,
            removesQuietZone: true
        )
    }

    var body: some View {
        ZStack {
            if mode == .share {
                shareContent
                    .transition(.opacity)
            } else {
                ProfileCodeScannerView {
                    mode = .share
                }
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity
                )
                .transition(.opacity)
                .ignoresSafeArea()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .animation(.default, value: mode)
        .navigationTitle("Share & Connect")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(
            mode == .scan ? .hidden : .automatic,
            for: .navigationBar
        )
        .toolbar {
            ToolbarItem(placement: .principal) {
                Picker("Mode", selection: $mode) {
                    ForEach(Mode.allCases) { mode in
                        Text(mode.rawValue)
                            .tag(mode)
                    }
                }
                .labelsHidden()
                .pickerStyle(.palette)
                .controlSize(.extraLarge)
                .frame(width: 180)
            }

            if mode == .share {
                ToolbarItem(placement: .topBarTrailing) {
                    ShareLink(item: shareText) {
                        Label(
                            "Share Profile",
                            systemImage: "square.and.arrow.up"
                        )
                        .labelStyle(.iconOnly)
                    }
                }
            }
        }
    }

    private var shareContent: some View {
        Form {
            Section {
                profileHeader
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())

            Section {
                qrCodeContent
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())
        }
    }

    private var profileHeader: some View {
        ProfileIdentityHeader(
            name: profile.name,
            publicKey: profile.publicKey,
            nostrAddress: profile.nostrAddress,
            isNostrAddressVerified: profile.isNostrAddressVerified
        ) { size in
            ProfileAvatarView(
                profile: profile,
                size: size
            )
        }
    }

    @ViewBuilder
    private var qrCodeContent: some View {
        VStack(spacing: 6) {
            if let qrImage {
                ShareableQRCodeView(
                    image: qrImage,
                    accessibilityLabel: "\(profile.name)’s profile QR code"
                )

                Text("Scan to connect.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            } else {
                ContentUnavailableView(
                    "QR Code Unavailable",
                    systemImage: "qrcode"
                )
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, 32)
    }

    private var shareText: String {
        "\(profile.name) on White Noise\n\(profile.publicKey)"
    }

}
