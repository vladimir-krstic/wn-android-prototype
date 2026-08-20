import SwiftUI
import UIKit

enum SimulatedQRScanOutcome: CaseIterable {
    case wrongContent
    case validKey

    var next: Self {
        switch self {
        case .wrongContent:
            .validKey
        case .validKey:
            .wrongContent
        }
    }
}

enum SimulatedCameraPermissionState {
    case requesting
    case allowed
    case denied
}

struct QRScannerView: View {
    @Environment(\.dismiss) private var dismiss

    @Binding private var simulatedPermission:
        SimulatedCameraPermissionState
    let simulatedOutcome: SimulatedQRScanOutcome
    let onSimulatedAttempt: () -> Void
    let onScan: (String) -> Void

    init(
        simulatedPermission:
            Binding<SimulatedCameraPermissionState> = .constant(.requesting),
        simulatedOutcome: SimulatedQRScanOutcome = .wrongContent,
        onSimulatedAttempt: @escaping () -> Void = {},
        onScan: @escaping (String) -> Void
    ) {
        _simulatedPermission = simulatedPermission
        self.simulatedOutcome = simulatedOutcome
        self.onSimulatedAttempt = onSimulatedAttempt
        self.onScan = onScan
    }

    var body: some View {
        Group {
#if targetEnvironment(simulator)
            SimulatedQRScannerView(
                permissionState: $simulatedPermission,
                outcome: simulatedOutcome,
                onAttempt: onSimulatedAttempt,
                onScan: completeScan
            )
#else
            PhysicalQRCodeScannerView(
                deniedDescription:
                    "Allow camera access in Settings to scan "
                    + "a private key QR code.",
                invalidDescription:
                    "This QR code doesn’t contain a private key.",
                validate: { payload in
                    payload.hasPrefix("nsec")
                },
                onScan: completeScan
            )
#endif
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func completeScan(_ payload: String) {
        onScan(payload)
        dismiss()
    }
}

private struct SimulatedQRScannerView: View {
    private enum Metrics {
        static let cardCenterYRatio: CGFloat = 0.435
        static let qrWidthRatio: CGFloat = 0.28
    }

    @Binding var permissionState: SimulatedCameraPermissionState
    let outcome: SimulatedQRScanOutcome
    let onAttempt: () -> Void
    let onScan: (String) -> Void

    @State private var isShowingWrongContentAlert: Bool

    init(
        permissionState: Binding<SimulatedCameraPermissionState>,
        outcome: SimulatedQRScanOutcome,
        initiallyShowsWrongContentAlert: Bool = false,
        onAttempt: @escaping () -> Void,
        onScan: @escaping (String) -> Void
    ) {
        _permissionState = permissionState
        self.outcome = outcome
        self.onAttempt = onAttempt
        self.onScan = onScan
        _isShowingWrongContentAlert = State(
            initialValue: initiallyShowsWrongContentAlert
        )
    }

    var body: some View {
        Group {
            switch permissionState {
            case .requesting:
                Color.black
                    .ignoresSafeArea()
                    .toolbarColorScheme(.dark, for: .navigationBar)
                    .toolbarBackground(.hidden, for: .navigationBar)
            case .allowed:
                scannerCanvas
            case .denied:
                ContentUnavailableView {
                    Label(
                        "Camera Access Is Off",
                        systemImage: "camera.fill"
                    )
                } description: {
                    Text(
                        "Allow camera access to scan a private key QR code."
                    )
                } actions: {
                    Button("Try Again") {
                        permissionState = .requesting
                    }
                    .buttonStyle(.glassProminent)
                }
            }
        }
        .alert(
            "Allow Camera Access?",
            isPresented: permissionAlertIsPresented
        ) {
            Button("Don’t Allow", role: .cancel) {
                permissionState = .denied
            }

            Button("Allow Camera") {
                permissionState = .allowed
            }
        } message: {
            Text("Use the camera to scan a private key QR code.")
        }
        .alert(
            "Can’t Use This QR Code",
            isPresented: $isShowingWrongContentAlert
        ) {
            Button("Try Again") {}
        } message: {
            Text("This QR code doesn’t contain a private key.")
        }
    }

    private var permissionAlertIsPresented: Binding<Bool> {
        Binding {
            permissionState == .requesting
        } set: { _ in }
    }

    private var scannerCanvas: some View {
        Button(action: performAttempt) {
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

                    qrCode
                        .frame(
                            width: proxy.size.width
                                * Metrics.qrWidthRatio,
                            height: proxy.size.width
                                * Metrics.qrWidthRatio
                        )
                        .position(
                            x: proxy.size.width / 2,
                            y: proxy.size.height
                                * Metrics.cardCenterYRatio
                        )
                }
            }
        }
        .buttonStyle(.plain)
        .allowsHitTesting(permissionState == .allowed)
        .accessibilityLabel("Scan QR Code")
        .ignoresSafeArea()
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbarBackground(.hidden, for: .navigationBar)
        .background(.black)
    }

    private var qrCode: some View {
        Group {
            if let image = QRCodeImageGenerator.image(
                for: LoginPrototypeData.privateKey,
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

    private func performAttempt() {
        guard permissionState == .allowed else {
            return
        }

        onAttempt()

        switch outcome {
        case .wrongContent:
            isShowingWrongContentAlert = true
        case .validKey:
            onScan(LoginPrototypeData.privateKey)
        }
    }
}

#Preview("QR Scanner — Ready") {
    NavigationStack {
        SimulatedQRScannerView(
            permissionState: .constant(.allowed),
            outcome: .wrongContent,
            onAttempt: {},
            onScan: { _ in }
        )
    }
    .tint(Color("AccentColor"))
}

#Preview("QR Scanner — Permission Denied") {
    NavigationStack {
        SimulatedQRScannerView(
            permissionState: .constant(.denied),
            outcome: .wrongContent,
            onAttempt: {},
            onScan: { _ in }
        )
    }
    .tint(Color("AccentColor"))
}

#Preview("QR Scanner — Permission Request") {
    NavigationStack {
        SimulatedQRScannerView(
            permissionState: .constant(.requesting),
            outcome: .wrongContent,
            onAttempt: {},
            onScan: { _ in }
        )
    }
    .tint(Color("AccentColor"))
}

#Preview("QR Scanner — Wrong Content") {
    NavigationStack {
        SimulatedQRScannerView(
            permissionState: .constant(.allowed),
            outcome: .wrongContent,
            initiallyShowsWrongContentAlert: true,
            onAttempt: {},
            onScan: { _ in }
        )
    }
    .tint(Color("AccentColor"))
}
