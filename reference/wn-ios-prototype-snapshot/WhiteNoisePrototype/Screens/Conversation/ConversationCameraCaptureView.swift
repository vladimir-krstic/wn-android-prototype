@preconcurrency import AVFoundation
import SwiftUI
import UIKit

struct ConversationCameraCapture: Identifiable {
    enum Content {
        case photo(Data)
        case video(URL)
    }

    let id = UUID()
    let content: Content
}

struct ConversationCameraCaptureView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @StateObject private var camera = ConversationCameraModel()
    @State private var shutterIsPressed = false
    @State private var didBeginVideo = false
    @State private var holdTask: Task<Void, Never>?

    let onCapture: (ConversationCameraCapture) -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            switch camera.state {
            case .preparing:
                ProgressView("Preparing Camera")
                    .tint(.white)
                    .foregroundStyle(.white)
            case .ready:
                cameraSurface
            case .denied:
                unavailableView(
                    title: "Camera Access Is Off",
                    systemImage: "camera.fill",
                    description: "Allow camera access in Settings to take photos and videos.",
                    offersSettings: true
                )
            case .restricted:
                unavailableView(
                    title: "Camera Is Restricted",
                    systemImage: "camera.fill",
                    description: "Camera access is restricted on this iPhone.",
                    offersSettings: false
                )
            case .unavailable:
                unavailableView(
                    title: "Camera Unavailable",
                    systemImage: "camera.fill",
                    description: "The camera isn’t available on this iPhone right now.",
                    offersSettings: false
                )
            }
        }
        .overlay(alignment: .topLeading) {
            closeButton.padding()
        }
        .task {
            await camera.prepare()
        }
        .onChange(of: camera.capture?.id) {
            guard let capture = camera.capture else { return }
            onCapture(capture)
            dismiss()
        }
        .onDisappear {
            holdTask?.cancel()
            camera.cancelAndStop()
        }
    }

    private var cameraSurface: some View {
        ZStack {
            ConversationCameraPreview(session: camera.session)
                .ignoresSafeArea()

            VStack {
                HStack {
                    Spacer()
                    Button(action: camera.switchCamera) {
                        Image(systemName: "arrow.triangle.2.circlepath.camera")
                    }
                    .buttonStyle(.glass)
                    .buttonBorderShape(.circle)
                    .controlSize(.large)
                    .accessibilityLabel("Switch Camera")
                }
                .padding()

                if camera.isRecordingVideo {
                    Label(
                        prototypeDurationString(camera.videoDuration),
                        systemImage: "record.circle.fill"
                    )
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(.red)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(.ultraThinMaterial, in: .capsule)
                    .accessibilityLabel(
                        "Recording video, \(prototypeDurationString(camera.videoDuration))"
                    )
                } else if !camera.recordsVideoSound {
                    Button(action: openSettings) {
                        Label("Videos record without sound", systemImage: "mic.slash")
                            .font(.caption)
                    }
                    .buttonStyle(.glass)
                    .accessibilityHint("Opens Settings to allow microphone access.")
                }

                Spacer()

                VStack(spacing: 12) {
                    shutterButton
                    Text("Tap for photo • Hold for video")
                        .font(.caption)
                        .foregroundStyle(.white)
                }
                .padding(.bottom, 28)
            }
        }
    }

    private var shutterButton: some View {
        ZStack {
            Circle()
                .stroke(.white, lineWidth: 4)
                .frame(width: 82, height: 82)
            Circle()
                .fill(camera.isRecordingVideo ? .red : .white)
                .frame(
                    width: camera.isRecordingVideo ? 48 : 68,
                    height: camera.isRecordingVideo ? 48 : 68
                )
                .animation(.snappy, value: camera.isRecordingVideo)
        }
        .contentShape(.circle)
        .scaleEffect(shutterIsPressed ? 0.94 : 1)
        .animation(.snappy, value: shutterIsPressed)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    guard !shutterIsPressed else { return }
                    shutterIsPressed = true
                    didBeginVideo = false
                    holdTask?.cancel()
                    holdTask = Task { @MainActor in
                        try? await Task.sleep(for: .milliseconds(350))
                        guard !Task.isCancelled, shutterIsPressed else { return }
                        didBeginVideo = true
                        camera.startVideoRecording()
                    }
                }
                .onEnded { _ in
                    shutterIsPressed = false
                    holdTask?.cancel()
                    holdTask = nil
                    if didBeginVideo {
                        camera.stopVideoRecording()
                    } else {
                        camera.capturePhoto()
                    }
                    didBeginVideo = false
                }
        )
        .accessibilityElement()
        .accessibilityAddTraits(.isButton)
        .accessibilityLabel(camera.isRecordingVideo ? "Stop Recording Video" : "Camera Shutter")
        .accessibilityHint("Double-tap for a photo. Press and hold to record video, then release to stop.")
        .accessibilityAction {
            camera.capturePhoto()
        }
        .accessibilityAction(named: "Start Recording Video") {
            camera.startVideoRecording()
        }
        .accessibilityAction(named: "Stop Recording Video") {
            camera.stopVideoRecording()
        }
    }

    private var closeButton: some View {
        Button {
            camera.cancelAndStop()
            dismiss()
        } label: {
            Image(systemName: "xmark")
        }
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
        .controlSize(.large)
        .accessibilityLabel("Close Camera")
    }

    private func unavailableView(
        title: String,
        systemImage: String,
        description: String,
        offersSettings: Bool
    ) -> some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            Text(description)
        } actions: {
            if offersSettings {
                Button("Open Settings", action: openSettings)
                    .buttonStyle(.glassProminent)
            }
        }
        .foregroundStyle(.white)
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        openURL(url)
    }
}

@MainActor
private final class ConversationCameraModel: ObservableObject {
    enum State: Equatable {
        case preparing
        case ready
        case denied
        case restricted
        case unavailable
    }

    @Published private(set) var state = State.preparing
    @Published private(set) var capture: ConversationCameraCapture?
    @Published private(set) var isRecordingVideo = false
    @Published private(set) var videoDuration: TimeInterval = 0
    @Published private(set) var recordsVideoSound = true

    let session: AVCaptureSession

    private let service: ConversationCameraCaptureService
    private var recordingTimer: Task<Void, Never>?
    private var acceptsCapture = true

    init() {
        let service = ConversationCameraCaptureService()
        self.service = service
        session = service.session

        service.onPhoto = { [weak self] data in
            Task { @MainActor in
                guard let self, self.acceptsCapture else { return }
                self.capture = ConversationCameraCapture(content: .photo(data))
            }
        }
        service.onVideo = { [weak self] url in
            Task { @MainActor in
                guard let self, self.acceptsCapture else {
                    try? FileManager.default.removeItem(at: url)
                    return
                }
                self.capture = ConversationCameraCapture(content: .video(url))
            }
        }
        service.onFailure = { [weak self] in
            Task { @MainActor in
                self?.state = .unavailable
            }
        }
    }

    func prepare() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            break
        case .notDetermined:
            guard await AVCaptureDevice.requestAccess(for: .video) else {
                state = .denied
                return
            }
        case .denied:
            state = .denied
            return
        case .restricted:
            state = .restricted
            return
        @unknown default:
            state = .unavailable
            return
        }

        recordsVideoSound = await requestAudioAccess()

        do {
            try await withCheckedThrowingContinuation {
                (continuation: CheckedContinuation<Void, Error>) in
                service.configure(includeAudio: recordsVideoSound) { result in
                    continuation.resume(with: result)
                }
            }
            service.start()
            state = .ready
        } catch {
            state = .unavailable
        }
    }

    func capturePhoto() {
        guard state == .ready, !isRecordingVideo else { return }
        service.capturePhoto()
    }

    func startVideoRecording() {
        guard state == .ready, !isRecordingVideo else { return }
        isRecordingVideo = true
        videoDuration = 0
        service.startVideoRecording()
        recordingTimer?.cancel()
        recordingTimer = Task { @MainActor in
            let startedAt = Date()
            while !Task.isCancelled, isRecordingVideo {
                try? await Task.sleep(for: .milliseconds(100))
                guard !Task.isCancelled else { return }
                videoDuration = Date().timeIntervalSince(startedAt)
            }
        }
    }

    func stopVideoRecording() {
        guard isRecordingVideo else { return }
        isRecordingVideo = false
        recordingTimer?.cancel()
        recordingTimer = nil
        service.stopVideoRecording()
    }

    func switchCamera() {
        guard !isRecordingVideo else { return }
        service.switchCamera()
    }

    func cancelAndStop() {
        acceptsCapture = false
        isRecordingVideo = false
        recordingTimer?.cancel()
        recordingTimer = nil
        service.cancelRecording()
        service.stop()
    }

    private func requestAudioAccess() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .authorized:
            return true
        case .notDetermined:
            return await AVCaptureDevice.requestAccess(for: .audio)
        case .denied, .restricted:
            return false
        @unknown default:
            return false
        }
    }
}

private final class ConversationCameraCaptureService: NSObject, @unchecked Sendable {
    let session = AVCaptureSession()

    var onPhoto: (@Sendable (Data) -> Void)?
    var onVideo: (@Sendable (URL) -> Void)?
    var onFailure: (@Sendable () -> Void)?

    private let sessionQueue = DispatchQueue(label: "dev.ipf.whitenoise.conversation-camera")
    private let photoOutput = AVCapturePhotoOutput()
    private let movieOutput = AVCaptureMovieFileOutput()
    private var videoInput: AVCaptureDeviceInput?
    private var discardNextMovie = false

    func configure(
        includeAudio: Bool,
        completion: @escaping @Sendable (Result<Void, Error>) -> Void
    ) {
        sessionQueue.async { [self] in
            do {
                try configureSession(includeAudio: includeAudio)
                completion(.success(()))
            } catch {
                completion(.failure(error))
            }
        }
    }

    func start() {
        sessionQueue.async { [self] in
            guard !session.isRunning else { return }
            session.startRunning()
        }
    }

    func stop() {
        sessionQueue.async { [self] in
            guard session.isRunning else { return }
            session.stopRunning()
        }
    }

    func capturePhoto() {
        sessionQueue.async { [self] in
            applyPortraitRotation(to: photoOutput.connection(with: .video))
            photoOutput.capturePhoto(with: AVCapturePhotoSettings(), delegate: self)
        }
    }

    func startVideoRecording() {
        sessionQueue.async { [self] in
            guard !movieOutput.isRecording else { return }
            discardNextMovie = false
            let url = FileManager.default.temporaryDirectory
                .appending(path: "camera-\(UUID().uuidString).mov")
            applyPortraitRotation(to: movieOutput.connection(with: .video))
            movieOutput.startRecording(to: url, recordingDelegate: self)
        }
    }

    func stopVideoRecording() {
        sessionQueue.async { [self] in
            guard movieOutput.isRecording else { return }
            movieOutput.stopRecording()
        }
    }

    func cancelRecording() {
        sessionQueue.async { [self] in
            discardNextMovie = true
            if movieOutput.isRecording {
                movieOutput.stopRecording()
            }
        }
    }

    func switchCamera() {
        sessionQueue.async { [self] in
            guard let currentInput = videoInput else { return }
            let position: AVCaptureDevice.Position = currentInput.device.position == .back
                ? .front
                : .back
            guard let device = camera(position: position),
                  let replacement = try? AVCaptureDeviceInput(device: device)
            else { return }

            session.beginConfiguration()
            session.removeInput(currentInput)
            if session.canAddInput(replacement) {
                session.addInput(replacement)
                videoInput = replacement
            } else {
                session.addInput(currentInput)
            }
            session.commitConfiguration()
        }
    }

    private func configureSession(includeAudio: Bool) throws {
        guard let videoDevice = camera(position: .back) else {
            throw ConversationCameraError.noCamera
        }

        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .high

        let input = try AVCaptureDeviceInput(device: videoDevice)
        guard session.canAddInput(input) else {
            throw ConversationCameraError.cannotAddInput
        }
        session.addInput(input)
        videoInput = input

        if includeAudio,
           let microphone = AVCaptureDevice.default(for: .audio),
           let audioInput = try? AVCaptureDeviceInput(device: microphone),
           session.canAddInput(audioInput) {
            session.addInput(audioInput)
        }

        guard session.canAddOutput(photoOutput), session.canAddOutput(movieOutput) else {
            throw ConversationCameraError.cannotAddOutput
        }
        session.addOutput(photoOutput)
        session.addOutput(movieOutput)
    }

    private func camera(position: AVCaptureDevice.Position) -> AVCaptureDevice? {
        AVCaptureDevice.default(
            .builtInWideAngleCamera,
            for: .video,
            position: position
        )
    }

    private func applyPortraitRotation(to connection: AVCaptureConnection?) {
        guard let connection, connection.isVideoRotationAngleSupported(90) else {
            return
        }
        connection.videoRotationAngle = 90
    }
}

extension ConversationCameraCaptureService:
    AVCapturePhotoCaptureDelegate,
    AVCaptureFileOutputRecordingDelegate {
    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        guard error == nil, let data = photo.fileDataRepresentation() else {
            onFailure?()
            return
        }
        onPhoto?(data)
    }

    func fileOutput(
        _ output: AVCaptureFileOutput,
        didFinishRecordingTo outputFileURL: URL,
        from connections: [AVCaptureConnection],
        error: Error?
    ) {
        let recordingSucceeded = error == nil
            || (error as NSError?)?.userInfo[AVErrorRecordingSuccessfullyFinishedKey]
                as? Bool == true

        if discardNextMovie {
            discardNextMovie = false
            try? FileManager.default.removeItem(at: outputFileURL)
        } else if recordingSucceeded {
            onVideo?(outputFileURL)
        } else {
            try? FileManager.default.removeItem(at: outputFileURL)
            onFailure?()
        }
    }
}

private final class ConversationCameraPreviewView: UIView {
    override class var layerClass: AnyClass {
        AVCaptureVideoPreviewLayer.self
    }

    var previewLayer: AVCaptureVideoPreviewLayer {
        layer as! AVCaptureVideoPreviewLayer
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let connection = previewLayer.connection
        if connection?.isVideoRotationAngleSupported(90) == true {
            connection?.videoRotationAngle = 90
        }
    }
}

private struct ConversationCameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> ConversationCameraPreviewView {
        let view = ConversationCameraPreviewView()
        view.backgroundColor = .black
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(
        _ view: ConversationCameraPreviewView,
        context: Context
    ) {
        view.previewLayer.session = session
    }
}

private enum ConversationCameraError: Error {
    case noCamera
    case cannotAddInput
    case cannotAddOutput
}
