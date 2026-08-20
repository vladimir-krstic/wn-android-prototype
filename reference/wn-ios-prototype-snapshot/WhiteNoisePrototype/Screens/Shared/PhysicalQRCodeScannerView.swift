import AVFoundation
import SwiftUI
import UIKit
import VisionKit

struct PhysicalQRCodeScannerView: View {
    private enum ScannerState {
        case checking
        case scanning
        case denied
        case restricted
        case unsupported
        case unavailable
    }

    @Environment(\.openURL) private var openURL
    @State private var state = ScannerState.checking
    @State private var isShowingInvalidCodeAlert = false

    let deniedDescription: String
    let invalidDescription: String
    let validate: (String) -> Bool
    let onScan: (String) -> Void

    var body: some View {
        Group {
            switch state {
            case .checking:
                ProgressView("Preparing Camera")
            case .scanning:
                NativeDataScannerView(
                    validate: validate,
                    onPayload: onScan,
                    onInvalidPayload: {
                        isShowingInvalidCodeAlert = true
                    },
                    onUnavailable: {
                        state = .unavailable
                    }
                )
                .ignoresSafeArea()
                .toolbarColorScheme(.dark, for: .navigationBar)
                .toolbarBackground(.hidden, for: .navigationBar)
            case .denied:
                ContentUnavailableView {
                    Label(
                        "Camera Access Is Off",
                        systemImage: "camera.fill"
                    )
                } description: {
                    Text(deniedDescription)
                } actions: {
                    Button("Open Settings", action: openSettings)
                        .buttonStyle(.glassProminent)
                }
            case .restricted:
                unavailableView(
                    description:
                        "Camera access is restricted on this iPhone."
                )
            case .unsupported:
                unavailableView(
                    description:
                        "QR scanning isn’t available on this iPhone."
                )
            case .unavailable:
                unavailableView(
                    description:
                        "QR scanning isn’t available right now. "
                        + "Try again later."
                )
            }
        }
        .task {
            await prepareCamera()
        }
        .alert(
            "Can’t Use This QR Code",
            isPresented: $isShowingInvalidCodeAlert
        ) {
            Button("Try Again") {}
        } message: {
            Text(invalidDescription)
        }
    }

    private func unavailableView(description: String) -> some View {
        ContentUnavailableView {
            Label(
                "QR Scanning Unavailable",
                systemImage: "qrcode.viewfinder"
            )
        } description: {
            Text(description)
        }
    }

    private func prepareCamera() async {
        guard DataScannerViewController.isSupported else {
            state = .unsupported
            return
        }

        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            state = DataScannerViewController.isAvailable
                ? .scanning
                : .unavailable
        case .notDetermined:
            let isAuthorized = await AVCaptureDevice.requestAccess(
                for: .video
            )
            state = isAuthorized
                && DataScannerViewController.isAvailable
                ? .scanning
                : .denied
        case .denied:
            state = .denied
        case .restricted:
            state = .restricted
        @unknown default:
            state = .unavailable
        }
    }

    private func openSettings() {
        guard let url = URL(
            string: UIApplication.openSettingsURLString
        ) else {
            return
        }

        openURL(url)
    }
}

private struct NativeDataScannerView: UIViewControllerRepresentable {
    let validate: (String) -> Bool
    let onPayload: (String) -> Void
    let onInvalidPayload: () -> Void
    let onUnavailable: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            validate: validate,
            onPayload: onPayload,
            onInvalidPayload: onInvalidPayload,
            onUnavailable: onUnavailable
        )
    }

    func makeUIViewController(
        context: Context
    ) -> DataScannerViewController {
        let scanner = DataScannerViewController(
            recognizedDataTypes: [
                .barcode(symbologies: [.qr])
            ],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: true,
            isPinchToZoomEnabled: true,
            isGuidanceEnabled: true,
            isHighlightingEnabled: true
        )

        scanner.delegate = context.coordinator
        startScanning(scanner, coordinator: context.coordinator)
        return scanner
    }

    func updateUIViewController(
        _ scanner: DataScannerViewController,
        context: Context
    ) {
        guard !scanner.isScanning else {
            return
        }

        startScanning(scanner, coordinator: context.coordinator)
    }

    static func dismantleUIViewController(
        _ scanner: DataScannerViewController,
        coordinator: Coordinator
    ) {
        scanner.stopScanning()
    }

    private func startScanning(
        _ scanner: DataScannerViewController,
        coordinator: Coordinator
    ) {
        do {
            try scanner.startScanning()
        } catch {
            coordinator.reportUnavailable()
        }
    }

    @MainActor
    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        private let validate: (String) -> Bool
        private let onPayload: (String) -> Void
        private let onInvalidPayload: () -> Void
        private let onUnavailable: () -> Void
        private var didScan = false

        init(
            validate: @escaping (String) -> Bool,
            onPayload: @escaping (String) -> Void,
            onInvalidPayload: @escaping () -> Void,
            onUnavailable: @escaping () -> Void
        ) {
            self.validate = validate
            self.onPayload = onPayload
            self.onInvalidPayload = onInvalidPayload
            self.onUnavailable = onUnavailable
        }

        func dataScanner(
            _ dataScanner: DataScannerViewController,
            didAdd addedItems: [RecognizedItem],
            allItems: [RecognizedItem]
        ) {
            guard !didScan else {
                return
            }

            for item in addedItems {
                guard case .barcode(let barcode) = item,
                      let payload = barcode.payloadStringValue else {
                    continue
                }

                let normalizedPayload = payload.trimmingCharacters(
                    in: .whitespacesAndNewlines
                )

                guard validate(normalizedPayload) else {
                    onInvalidPayload()
                    return
                }

                didScan = true
                dataScanner.stopScanning()
                onPayload(normalizedPayload)
                return
            }
        }

        func dataScanner(
            _ dataScanner: DataScannerViewController,
            becameUnavailableWithError error:
                DataScannerViewController.ScanningUnavailable
        ) {
            reportUnavailable()
        }

        func reportUnavailable() {
            Task { @MainActor in
                onUnavailable()
            }
        }
    }
}
