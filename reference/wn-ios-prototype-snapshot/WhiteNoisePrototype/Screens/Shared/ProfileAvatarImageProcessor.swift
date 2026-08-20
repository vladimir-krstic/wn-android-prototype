import Foundation
import ImageIO
import UniformTypeIdentifiers

enum ProfileAvatarImageProcessor {
    private static let maximumPixelDimension = 512

    static func preparedData(from sourceData: Data) -> Data? {
        ImageDataProcessor.preparedJPEGData(
            from: sourceData,
            maximumPixelDimension: maximumPixelDimension
        )
    }

    static func preparedDataAsync(from sourceData: Data) async -> Data? {
        await ImageDataProcessor.preparedJPEGDataAsync(
            from: sourceData,
            maximumPixelDimension: maximumPixelDimension
        )
    }

    static func preparedData(contentsOf url: URL) async throws -> Data? {
        try await ImageDataProcessor.preparedJPEGData(
            contentsOf: url,
            maximumPixelDimension: maximumPixelDimension
        )
    }
}

enum ConversationImageProcessor {
    private static let maximumPixelDimension = 1024

    static func preparedData(from sourceData: Data) -> Data? {
        ImageDataProcessor.preparedJPEGData(
            from: sourceData,
            maximumPixelDimension: maximumPixelDimension
        )
    }

    static func preparedDataAsync(from sourceData: Data) async -> Data? {
        await ImageDataProcessor.preparedJPEGDataAsync(
            from: sourceData,
            maximumPixelDimension: maximumPixelDimension
        )
    }
}

private enum ImageDataProcessor {
    static func preparedJPEGDataAsync(
        from sourceData: Data,
        maximumPixelDimension: Int
    ) async -> Data? {
        await Task.detached(priority: .userInitiated) {
            preparedJPEGData(
                from: sourceData,
                maximumPixelDimension: maximumPixelDimension
            )
        }.value
    }

    static func preparedJPEGData(
        contentsOf url: URL,
        maximumPixelDimension: Int
    ) async throws -> Data? {
        try await Task.detached(priority: .userInitiated) {
            let sourceData = try Data(contentsOf: url)
            return preparedJPEGData(
                from: sourceData,
                maximumPixelDimension: maximumPixelDimension
            )
        }.value
    }

    static func preparedJPEGData(
        from sourceData: Data,
        maximumPixelDimension: Int
    ) -> Data? {
        guard maximumPixelDimension > 0,
              let source = CGImageSourceCreateWithData(
                  sourceData as CFData,
                  [kCGImageSourceShouldCache: false] as CFDictionary
              ),
              let thumbnail = CGImageSourceCreateThumbnailAtIndex(
                  source,
                  0,
                  [
                      kCGImageSourceCreateThumbnailFromImageAlways: true,
                      kCGImageSourceCreateThumbnailWithTransform: true,
                      kCGImageSourceThumbnailMaxPixelSize:
                          maximumPixelDimension,
                      kCGImageSourceShouldCacheImmediately: true,
                  ] as CFDictionary
              )
        else {
            return nil
        }

        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            output,
            UTType.jpeg.identifier as CFString,
            1,
            nil
        ) else {
            return nil
        }

        CGImageDestinationAddImage(
            destination,
            thumbnail,
            [kCGImageDestinationLossyCompressionQuality: 0.88] as CFDictionary
        )

        guard CGImageDestinationFinalize(destination) else {
            return nil
        }

        return output as Data
    }
}
