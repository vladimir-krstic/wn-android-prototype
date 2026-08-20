import AVFoundation
import Foundation
import UIKit

struct ConversationPreparedVideo {
    let url: URL
    let thumbnailData: Data?
    let duration: TimeInterval
    let dimensions: PrototypeMediaDimensions?
}

enum ConversationVideoProcessor {
    static func prepare(data: Data) async -> ConversationPreparedVideo? {
        let url = FileManager.default.temporaryDirectory
            .appending(path: "video-\(UUID().uuidString).mov")

        do {
            try await Task.detached(priority: .userInitiated) {
                try data.write(to: url, options: .atomic)
            }.value
            try Task.checkCancellation()

            return await prepareOwnedVideo(at: url)
        } catch {
            try? FileManager.default.removeItem(at: url)
            return nil
        }
    }

    static func prepare(fileAt sourceURL: URL) async -> ConversationPreparedVideo? {
        let destination = FileManager.default.temporaryDirectory
            .appending(path: "video-\(UUID().uuidString).mov")

        do {
            try await Task.detached(priority: .userInitiated) {
                try FileManager.default.copyItem(at: sourceURL, to: destination)
            }.value
            try Task.checkCancellation()

            return await prepareOwnedVideo(at: destination)
        } catch {
            try? FileManager.default.removeItem(at: destination)
            return nil
        }
    }

    private static func prepareOwnedVideo(at url: URL) async -> ConversationPreparedVideo? {
        do {
            let asset = AVURLAsset(url: url)
            let loadedDuration = try await asset.load(.duration)
            let duration = loadedDuration.seconds.isFinite
                ? max(0, loadedDuration.seconds)
                : 0
            let dimensions = try await normalizedDimensions(for: asset)

            let generator = AVAssetImageGenerator(asset: asset)
            generator.appliesPreferredTrackTransform = true
            generator.maximumSize = CGSize(width: 1_024, height: 1_024)
            let requestedTime = CMTime(
                seconds: min(max(duration * 0.1, 0), 0.5),
                preferredTimescale: 600
            )
            let generated = try await generator.image(at: requestedTime)
            let jpeg = UIImage(cgImage: generated.image).jpegData(compressionQuality: 0.82)
            let thumbnail: Data? = if let jpeg {
                await ConversationImageProcessor.preparedDataAsync(from: jpeg)
            } else {
                nil
            }

            return ConversationPreparedVideo(
                url: url,
                thumbnailData: thumbnail,
                duration: duration,
                dimensions: dimensions
            )
        } catch {
            try? FileManager.default.removeItem(at: url)
            return nil
        }
    }

    private static func normalizedDimensions(
        for asset: AVURLAsset
    ) async throws -> PrototypeMediaDimensions? {
        guard let track = try await asset.loadTracks(withMediaType: .video).first else {
            return nil
        }
        let naturalSize = try await track.load(.naturalSize)
        let preferredTransform = try await track.load(.preferredTransform)
        let transformed = naturalSize.applying(preferredTransform)
        return PrototypeMediaDimensions(
            pixelWidth: Double(abs(transformed.width)),
            pixelHeight: Double(abs(transformed.height))
        )
    }
}
