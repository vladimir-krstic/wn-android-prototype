import SwiftUI
import UIKit

struct ProfileCodeScannerView: View {
    @State private var foundProfile: PrototypeProfile?
    @State private var scanSessionID = UUID()

    let onDone: () -> Void

    var body: some View {
        Group {
            if let foundProfile {
                ContentUnavailableView {
                    Label(
                        "Profile Found",
                        systemImage:
                            "person.crop.circle.badge.checkmark"
                    )
                } description: {
                    ProfileSummary(
                        profile: foundProfile,
                        avatarSize: 64,
                        showsNostrAddress: true
                    )
                } actions: {
                    Button("Done", action: onDone)
                        .buttonStyle(.glassProminent)

                    Button("Scan Another") {
                        self.foundProfile = nil
                        scanSessionID = UUID()
                    }
                    .buttonStyle(.glass)
                }
            } else {
                scanner
                    .id(scanSessionID)
            }
        }
    }

    @ViewBuilder
    private var scanner: some View {
#if targetEnvironment(simulator)
        SimulatedProfileCodeScanner {
            foundProfile = .openQuill
        }
#else
        PhysicalQRCodeScannerView(
            deniedDescription:
                "Allow camera access in Settings to scan profile QR codes.",
            invalidDescription:
                "This QR code doesn’t contain a White Noise profile.",
            validate: { payload in
                payload.hasPrefix("npub")
            },
            onScan: { _ in
                foundProfile = .openQuill
            }
        )
#endif
    }
}

private struct SimulatedProfileCodeScanner: View {
    let onProfileFound: () -> Void

    var body: some View {
        Button(action: onProfileFound) {
            GeometryReader { proxy in
                ZStack {
                    Image("QRScannerBackdrop")
                        .resizable()
                        .scaledToFill()
                        .frame(
                            width: proxy.size.width,
                            height: proxy.size.height
                        )
                        .clipped()

                    profileQRCode
                        .frame(
                            width: proxy.size.width * 0.28,
                            height: proxy.size.width * 0.28
                        )

                    VStack {
                        Spacer()

                        Label(
                            "Scan a profile QR code",
                            systemImage: "qrcode.viewfinder"
                        )
                        .font(.callout.weight(.medium))
                        .padding()
                        .background(.regularMaterial, in: Capsule())
                        .padding()
                    }
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Scan Profile QR Code")
        .background(.black)
    }

    private var profileQRCode: some View {
        Group {
            if let image = QRCodeImageGenerator.image(
                for: PrototypeProfile.openQuill.publicKey,
                scale: 10
            ) {
                Image(uiImage: image)
                    .resizable()
                    .interpolation(.none)
                    .scaledToFit()
            } else {
                Image(systemName: "qrcode")
                    .resizable()
                    .scaledToFit()
            }
        }
        .background(.white)
    }
}

#Preview("Scan Profile") {
    NavigationStack {
        ProfileCodeScannerView {}
    }
    .tint(Color("AccentColor"))
}
