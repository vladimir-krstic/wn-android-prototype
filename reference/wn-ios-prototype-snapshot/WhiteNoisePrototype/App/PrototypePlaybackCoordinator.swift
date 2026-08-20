import AVFoundation
import Foundation

private struct PrototypeAudioPlayerHandle: @unchecked Sendable {
    let player: AVAudioPlayer
}

private let prototypeAudioOperationQueue = DispatchQueue(
    label: "dev.ipf.whitenoise.prototype-audio",
    qos: .userInitiated
)

/// Coordinates deterministic playback without microphone access.
/// Every voice bubble plays the same locally bundled recording.
@MainActor
final class PrototypePlaybackCoordinator: NSObject, ObservableObject,
    AVAudioPlayerDelegate, AVSpeechSynthesizerDelegate
{
    static let shared = PrototypePlaybackCoordinator()

    @Published private(set) var activeVoiceID: String?
    @Published private(set) var elapsed: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var isPaused = false
    @Published private(set) var activeSpokenMessageID: String?
    @Published private(set) var spokenProgress: Double = 0
    @Published private var registeredWaveforms: [String: [Double]] = [:]

    private var player: AVAudioPlayer?
    private let speechSynthesizer = AVSpeechSynthesizer()
    private var activeSpeechUtteranceID: ObjectIdentifier?
    private var progressTask: Task<Void, Never>?
    private var voiceActivationID: UUID?

    override private init() {
        super.init()
        speechSynthesizer.delegate = self
    }

    func toggleVoice(id: String, duration requestedDuration: TimeInterval) {
        if activeVoiceID == id {
            if isPaused {
                guard let player else {
                    stopAll(deactivateAudioSession: false)
                    return
                }
                resume(player)
                isPaused = false
                startProgress()
            } else {
                if let player {
                    let handle = PrototypeAudioPlayerHandle(player: player)
                    prototypeAudioOperationQueue.async {
                        handle.player.pause()
                    }
                }
                isPaused = true
                progressTask?.cancel()
            }
            return
        }

        stopAll(deactivateAudioSession: false)
        guard let player = try? AVAudioPlayer(data: PrototypeVoiceSample.data) else { return }
        let activationID = UUID()
        voiceActivationID = activationID
        self.player = player
        player.delegate = self
        let playbackDuration = max(0.1, requestedDuration)
        if playbackDuration > player.duration {
            player.numberOfLoops = -1
        }
        activeVoiceID = id
        duration = playbackDuration
        elapsed = 0
        isPaused = false

        let audioSession = AVAudioSession.sharedInstance()
        audioSession.activate(options: []) { [weak self] activated, _ in
            Task { @MainActor in
                guard let self,
                      self.voiceActivationID == activationID,
                      self.player === player
                else {
                    if activated {
                        audioSession.deactivate(options: []) { _, _ in }
                    }
                    return
                }

                guard activated else {
                    self.stopAll(deactivateAudioSession: false)
                    return
                }
                self.start(player, activationID: activationID)
            }
        }
    }

    func registerWaveform(_ samples: [Double], for id: String) {
        registeredWaveforms[id] = samples
    }

    func waveform(for id: String) -> [Double] {
        registeredWaveforms[id] ?? PrototypeWaveformSamples.samples(seed: id)
    }

    func readAloud(messageID: String, text: String) {
        let spokenText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !spokenText.isEmpty else { return }

        stopAll()
        let utterance = AVSpeechUtterance(string: spokenText)
        activeSpeechUtteranceID = ObjectIdentifier(utterance)
        activeSpokenMessageID = messageID
        spokenProgress = 0
        speechSynthesizer.speak(utterance)
    }

    func stopReading() {
        guard activeSpeechUtteranceID != nil
                || activeSpokenMessageID != nil
                || speechSynthesizer.isSpeaking
                || speechSynthesizer.isPaused
        else { return }

        activeSpeechUtteranceID = nil
        speechSynthesizer.stopSpeaking(at: .immediate)
        activeSpokenMessageID = nil
        spokenProgress = 0
    }

    func stopAll() {
        stopAll(deactivateAudioSession: true)
    }

    private func stopAll(deactivateAudioSession: Bool) {
        let hadVoicePlayback = progressTask != nil
            || player != nil
            || activeVoiceID != nil
            || elapsed != 0
            || duration != 0
            || isPaused

        progressTask?.cancel()
        progressTask = nil
        voiceActivationID = nil
        let playerToStop = player
        player = nil
        if hadVoicePlayback {
            activeVoiceID = nil
            elapsed = 0
            duration = 0
            isPaused = false
        }
        if let playerToStop {
            let handle = PrototypeAudioPlayerHandle(player: playerToStop)
            prototypeAudioOperationQueue.async {
                handle.player.stop()
                if hadVoicePlayback, deactivateAudioSession {
                    AVAudioSession.sharedInstance().deactivate(options: []) { _, _ in }
                }
            }
        } else if hadVoicePlayback, deactivateAudioSession {
            AVAudioSession.sharedInstance().deactivate(options: []) { _, _ in }
        }
        stopReading()
    }

    private func start(_ player: AVAudioPlayer, activationID: UUID) {
        let handle = PrototypeAudioPlayerHandle(player: player)
        prototypeAudioOperationQueue.async { [weak self] in
            let started = handle.player.prepareToPlay()
                && handle.player.play()
            Task { @MainActor [weak self] in
                guard let self,
                      self.voiceActivationID == activationID,
                      self.player === handle.player
                else {
                    if started {
                        prototypeAudioOperationQueue.async {
                            handle.player.stop()
                        }
                    }
                    return
                }

                guard started else {
                    self.stopAll(deactivateAudioSession: false)
                    return
                }
                self.startProgress()
            }
        }
    }

    private func resume(_ player: AVAudioPlayer) {
        let handle = PrototypeAudioPlayerHandle(player: player)
        prototypeAudioOperationQueue.async { [weak self] in
            guard handle.player.play() else {
                Task { @MainActor in
                    guard let self, self.player === handle.player else { return }
                    self.stopAll()
                }
                return
            }
        }
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor [weak self] in self?.stopAll() }
    }

    nonisolated func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        willSpeakRangeOfSpeechString characterRange: NSRange,
        utterance: AVSpeechUtterance
    ) {
        let utteranceID = ObjectIdentifier(utterance)
        let characterCount = utterance.speechString.utf16.count
        let spokenCharacterCount = min(NSMaxRange(characterRange), characterCount)
        Task { @MainActor [weak self] in
            guard self?.activeSpeechUtteranceID == utteranceID else { return }
            self?.spokenProgress = characterCount > 0
                ? min(max(Double(spokenCharacterCount) / Double(characterCount), 0), 1)
                : 0
        }
    }

    nonisolated func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didFinish utterance: AVSpeechUtterance
    ) {
        let utteranceID = ObjectIdentifier(utterance)
        Task { @MainActor [weak self] in
            guard self?.activeSpeechUtteranceID == utteranceID else { return }
            self?.activeSpeechUtteranceID = nil
            self?.activeSpokenMessageID = nil
            self?.spokenProgress = 0
        }
    }

    nonisolated func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didCancel utterance: AVSpeechUtterance
    ) {
        let utteranceID = ObjectIdentifier(utterance)
        Task { @MainActor [weak self] in
            guard self?.activeSpeechUtteranceID == utteranceID else { return }
            self?.activeSpeechUtteranceID = nil
            self?.activeSpokenMessageID = nil
            self?.spokenProgress = 0
        }
    }

    private func startProgress() {
        progressTask?.cancel()
        progressTask = Task { @MainActor in
            while !Task.isCancelled, activeVoiceID != nil, !isPaused {
                try? await Task.sleep(for: .milliseconds(100))
                guard !Task.isCancelled else { return }
                elapsed = min(duration, elapsed + 0.1)
                if elapsed >= duration {
                    stopAll()
                    return
                }
            }
        }
    }
}
