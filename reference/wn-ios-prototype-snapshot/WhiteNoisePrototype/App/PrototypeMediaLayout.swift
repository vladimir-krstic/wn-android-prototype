import CoreGraphics
import Foundation
import UIKit

struct PrototypeMediaLayout: Equatable {
    static let maximumSingleExtent: CGFloat = 256
    static let minimumSingleWidth: CGFloat = 192
    static let practicalSmallSourceExtent: CGFloat = 150
    static let minimumAspectRatio: CGFloat = 0.35
    static let maximumAspectRatio: CGFloat = 1 / minimumAspectRatio
    static let albumWidth: CGFloat = 256
    static let gutter: CGFloat = 2
    static let maximumVisibleItems = 5

    let size: CGSize
    let frames: [CGRect]
    let overflowCount: Int

    init(size: CGSize, frames: [CGRect], overflowCount: Int) {
        self.size = size
        self.frames = frames
        self.overflowCount = overflowCount
    }

    func isOverflowTile(at index: Int) -> Bool {
        overflowCount > 0 && index == Self.maximumVisibleItems - 1
    }

    static func singleSize(
        dimensions: PrototypeMediaDimensions?
    ) -> CGSize {
        guard let dimensions else {
            return CGSize(width: maximumSingleExtent, height: maximumSingleExtent)
        }

        let intrinsicRatio = CGFloat(dimensions.aspectRatio)
        let ratio = min(max(intrinsicRatio, minimumAspectRatio), maximumAspectRatio)
        var width = maximumSingleExtent * ratio
        var height = maximumSingleExtent

        width = max(width, minimumSingleWidth)
        if width > maximumSingleExtent {
            width = maximumSingleExtent
            height = maximumSingleExtent / ratio
        }

        let sourceShortExtent = CGFloat(
            min(dimensions.pixelWidth, dimensions.pixelHeight)
        )
        let destinationShortExtent = min(width, height)
        let minimumDisplayExtent = max(
            practicalSmallSourceExtent,
            minimumSingleWidth
        )
        if destinationShortExtent > minimumDisplayExtent,
           destinationShortExtent > sourceShortExtent {
            let scale = minimumDisplayExtent / destinationShortExtent
            width *= scale
            height *= scale
        }

        return CGSize(width: width, height: height)
    }

    init(count: Int) {
        let count = max(0, count)
        overflowCount = max(0, count - Self.maximumVisibleItems)

        switch count {
        case 0:
            size = .zero
            frames = []
        case 1:
            size = CGSize(width: Self.albumWidth, height: Self.albumWidth)
            frames = [CGRect(origin: .zero, size: size)]
        case 2:
            size = CGSize(width: 256, height: 127)
            frames = [
                CGRect(x: 0, y: 0, width: 127, height: 127),
                CGRect(x: 129, y: 0, width: 127, height: 127),
            ]
        case 3:
            size = CGSize(width: 256, height: 170)
            frames = [
                CGRect(x: 0, y: 0, width: 170, height: 170),
                CGRect(x: 172, y: 0, width: 84, height: 84),
                CGRect(x: 172, y: 86, width: 84, height: 84),
            ]
        case 4:
            size = CGSize(width: 256, height: 256)
            frames = [
                CGRect(x: 0, y: 0, width: 127, height: 127),
                CGRect(x: 129, y: 0, width: 127, height: 127),
                CGRect(x: 0, y: 129, width: 127, height: 127),
                CGRect(x: 129, y: 129, width: 127, height: 127),
            ]
        default:
            size = CGSize(width: 256, height: 213)
            frames = [
                CGRect(x: 0, y: 0, width: 127, height: 127),
                CGRect(x: 129, y: 0, width: 127, height: 127),
                CGRect(x: 0, y: 129, width: 84, height: 84),
                CGRect(x: 86, y: 129, width: 84, height: 84),
                CGRect(x: 172, y: 129, width: 84, height: 84),
            ]
        }
    }
}

struct PrototypeMediaItem: Identifiable, Equatable {
    let id: String
    let chatID: String
    let messageID: String
    let attachmentID: String
    let senderID: String
    let sentAt: Date
    let attachment: PrototypeAttachment

    var isAvailable: Bool { attachment.prototypeMediaIsAvailable }
}

enum PrototypeMediaIndex {
    static func allItems(in chat: PrototypeChat) -> [PrototypeMediaItem] {
        chat.messages.enumerated()
            .filter { !$0.element.isDeleted }
            .sorted { first, second in
                if first.element.sentAt == second.element.sentAt {
                    return first.offset < second.offset
                }
                return first.element.sentAt < second.element.sentAt
            }
            .flatMap { _, message -> [PrototypeMediaItem] in
                message.attachments.enumerated().compactMap { index, attachment in
                    guard attachment.prototypeIsPhotoOrVideo else { return nil }
                    return PrototypeMediaItem(
                        id: "\(message.id)-\(attachment.id)-\(index)",
                        chatID: chat.id,
                        messageID: message.id,
                        attachmentID: attachment.id,
                        senderID: message.authorID,
                        sentAt: message.sentAt,
                        attachment: attachment
                    )
                }
            }
    }

    static func availableItems(in chat: PrototypeChat) -> [PrototypeMediaItem] {
        allItems(in: chat).filter(\.isAvailable)
    }
}

extension PrototypeAttachment {
    var prototypeIsPhotoOrVideo: Bool {
        switch self {
        case .photo, .video: true
        default: false
        }
    }

    var prototypeMediaDimensions: PrototypeMediaDimensions? {
        switch self {
        case let .photo(_, source, _, storedDimensions):
            storedDimensions ?? source.prototypeDimensions
        case let .video(_, _, thumbnail, _, storedDimensions):
            storedDimensions ?? thumbnail.prototypeDimensions
        default:
            nil
        }
    }

    var prototypeMediaIsAvailable: Bool {
        switch self {
        case let .photo(_, source, _, _):
            source.prototypeImage != nil
        case let .video(_, url, _, _, _):
            url != nil
        default:
            false
        }
    }
}

extension PrototypeImageSource {
    var prototypeImage: UIImage? {
        switch self {
        case let .asset(name): UIImage(named: name)
        case let .data(data): UIImage(data: data)
        }
    }

    var prototypeDimensions: PrototypeMediaDimensions? {
        guard let image = prototypeImage else { return nil }
        let pixelWidth = Double(image.size.width * image.scale)
        let pixelHeight = Double(image.size.height * image.scale)
        return PrototypeMediaDimensions(pixelWidth: pixelWidth, pixelHeight: pixelHeight)
    }
}
