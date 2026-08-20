import CoreImage.CIFilterBuiltins
import UIKit

enum QRCodeImageGenerator {
    private static let context = CIContext()

    static func image(
        for payload: String,
        scale: CGFloat = 12,
        removesQuietZone: Bool = false
    ) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "M"

        guard let outputImage = filter.outputImage else {
            return nil
        }

        let sourceImage: CIImage
        if removesQuietZone {
            let symbolExtent = outputImage.extent.insetBy(dx: 1, dy: 1)
            sourceImage = outputImage
                .cropped(to: symbolExtent)
                .transformed(
                    by: CGAffineTransform(
                        translationX: -symbolExtent.minX,
                        y: -symbolExtent.minY
                    )
                )
        } else {
            sourceImage = outputImage
        }

        let scaledImage = sourceImage.transformed(
            by: CGAffineTransform(scaleX: scale, y: scale)
        )
        guard let image = context.createCGImage(
            scaledImage,
            from: scaledImage.extent
        ) else {
            return nil
        }

        return UIImage(cgImage: image)
    }
}
