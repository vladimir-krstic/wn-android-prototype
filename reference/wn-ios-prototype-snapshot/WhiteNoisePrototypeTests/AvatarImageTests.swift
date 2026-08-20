import ImageIO
import Testing
import UIKit
import UniformTypeIdentifiers
@testable import WhiteNoisePrototype

@Suite("Avatar images")
struct AvatarImageTests {
    @Test("Every deterministic web image URL round-trips to its catalog entry")
    func webImageURLsRoundTrip() {
        for choice in AvatarWebImageCatalog.choices {
            let url = AvatarWebImageCatalog.displayURL(for: choice)
            let resolvedChoice = AvatarWebImageCatalog.choice(matching: url)

            #expect(resolvedChoice?.id == choice.id)
        }
    }

    @Test("Every web image has a unique spoken description")
    func webImageAccessibilityLabelsAreUnique() {
        let labels = AvatarWebImageCatalog.choices.map(\.accessibilityLabel)

        #expect(Set(labels).count == labels.count)
    }

    @Test("Profile photos are encoded as JPEG at no more than 512 pixels")
    @MainActor
    func profilePhotoPreparation() throws {
        let renderer = UIGraphicsImageRenderer(
            size: CGSize(width: 1_200, height: 800)
        )
        let image = renderer.image { context in
            UIColor.systemBlue.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 1_200, height: 800))
        }
        let sourceData = try #require(image.pngData())
        let preparedData = try #require(
            ProfileAvatarImageProcessor.preparedData(from: sourceData)
        )
        let source = try #require(
            CGImageSourceCreateWithData(preparedData as CFData, nil)
        )
        let properties = try #require(
            CGImageSourceCopyPropertiesAtIndex(source, 0, nil)
                as? [CFString: Any]
        )
        let width = try #require(properties[kCGImagePropertyPixelWidth] as? Int)
        let height = try #require(properties[kCGImagePropertyPixelHeight] as? Int)

        #expect(max(width, height) <= 512)
        #expect(CGImageSourceGetType(source) == UTType.jpeg.identifier as CFString)
    }

    @Test("Conversation photos are encoded once at no more than 1024 pixels")
    @MainActor
    func conversationPhotoPreparation() throws {
        let renderer = UIGraphicsImageRenderer(
            size: CGSize(width: 1_800, height: 1_200)
        )
        let image = renderer.image { context in
            UIColor.systemGreen.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 1_800, height: 1_200))
        }
        let sourceData = try #require(image.pngData())
        let preparedData = try #require(
            ConversationImageProcessor.preparedData(from: sourceData)
        )
        let source = try #require(
            CGImageSourceCreateWithData(preparedData as CFData, nil)
        )
        let properties = try #require(
            CGImageSourceCopyPropertiesAtIndex(source, 0, nil)
                as? [CFString: Any]
        )
        let width = try #require(properties[kCGImagePropertyPixelWidth] as? Int)
        let height = try #require(properties[kCGImagePropertyPixelHeight] as? Int)

        #expect(max(width, height) <= 1_024)
        #expect(CGImageSourceGetType(source) == UTType.jpeg.identifier as CFString)
    }
}
