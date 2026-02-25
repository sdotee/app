import Foundation
import WebP

#if os(macOS)
import AppKit
#else
import UIKit
#endif

enum PasteImageFormat: String, CaseIterable, Identifiable {
    case webp = "WebP"
    case png = "PNG"

    var id: String { rawValue }
    var fileExtension: String { rawValue.lowercased() }
}

enum ImageConverter {
    /// Convert image data to WebP lossless format using Swift-WebP library.
    /// Falls back to nil if conversion fails (caller should fallback to PNG).
    static func toWebPLossless(data: Data) -> Data? {
        guard let cgImage = createCGImage(from: data) else {
            return nil
        }
        return encodeToWebP(cgImage: cgImage)
    }

    /// Encode a CGImage to WebP lossless format.
    private static func encodeToWebP(cgImage: CGImage) -> Data? {
        let width = cgImage.width
        let height = cgImage.height
        let bytesPerPixel = 4
        let stride = width * bytesPerPixel

        // Render CGImage to RGBA pixel buffer
        guard let colorSpace = cgImage.colorSpace ?? CGColorSpace(name: CGColorSpace.sRGB),
              let context = CGContext(
                  data: nil,
                  width: width,
                  height: height,
                  bitsPerComponent: 8,
                  bytesPerRow: stride,
                  space: colorSpace,
                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
              ) else {
            return nil
        }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        guard let pixelData = context.data else {
            return nil
        }

        let buffer = UnsafeBufferPointer(
            start: pixelData.assumingMemoryBound(to: UInt8.self),
            count: height * stride
        )

        do {
            var config = try WebPEncoderConfig.losslessPreset(level: 6)
            config.alphaQuality = 100

            let encoder = WebPEncoder()
            let webpData = try encoder.encode(
                buffer,
                format: .rgba,
                config: config,
                originWidth: width,
                originHeight: height,
                stride: stride
            )
            return webpData
        } catch {
            return nil
        }
    }

    /// Create a CGImage from any image data format.
    private static func createCGImage(from data: Data) -> CGImage? {
        #if os(macOS)
        guard let nsImage = NSImage(data: data) else { return nil }
        var rect = CGRect(x: 0, y: 0, width: nsImage.size.width, height: nsImage.size.height)
        return nsImage.cgImage(forProposedRect: &rect, context: nil, hints: nil)
        #else
        guard let uiImage = UIImage(data: data) else { return nil }
        return uiImage.cgImage
        #endif
    }

    /// Read image data from the system clipboard.
    static func clipboardImageData() -> Data? {
        #if os(macOS)
        let pasteboard = NSPasteboard.general

        // 1. Try NSImage from pasteboard (handles screenshots, copied images)
        if let images = pasteboard.readObjects(forClasses: [NSImage.self]) as? [NSImage],
           let image = images.first,
           let tiffData = image.tiffRepresentation {
            return tiffData
        }

        // 2. Try file URLs (e.g., Finder copied image files)
        if let urls = pasteboard.readObjects(forClasses: [NSURL.self]) as? [URL],
           let url = urls.first {
            let ext = url.pathExtension.lowercased()
            let imageExts = ["png", "jpg", "jpeg", "gif", "webp", "bmp", "tiff", "tif", "heic", "heif"]
            if imageExts.contains(ext), let data = try? Data(contentsOf: url) {
                return data
            }
        }

        // 3. Try raw pasteboard types as fallback
        if let tiffData = pasteboard.data(forType: .tiff) {
            return tiffData
        }
        if let pngData = pasteboard.data(forType: .png) {
            return pngData
        }

        return nil
        #else
        if let image = UIPasteboard.general.image,
           let data = image.pngData() {
            return data
        }
        if let url = UIPasteboard.general.url,
           let data = try? Data(contentsOf: url) {
            return data
        }
        return nil
        #endif
    }

    /// Check if the system clipboard contains an image.
    static func clipboardHasImage() -> Bool {
        #if os(macOS)
        let pasteboard = NSPasteboard.general
        return pasteboard.canReadObject(forClasses: [NSImage.self], options: nil)
            || pasteboard.data(forType: .tiff) != nil
            || pasteboard.data(forType: .png) != nil
        #else
        return UIPasteboard.general.hasImages
        #endif
    }

    /// Convert image data to PNG format.
    static func toPNG(data: Data) -> Data? {
        guard let cgImage = createCGImage(from: data) else { return nil }
        #if os(macOS)
        let rep = NSBitmapImageRep(cgImage: cgImage)
        return rep.representation(using: .png, properties: [:])
        #else
        guard let uiImage = UIImage(cgImage: cgImage) else { return nil }
        return uiImage.pngData()
        #endif
    }

    /// Read the user's preferred paste image format from settings.
    static var preferredFormat: PasteImageFormat {
        if let saved = UserDefaults.standard.string(forKey: Constants.pasteImageFormatKey),
           let format = PasteImageFormat(rawValue: saved) {
            return format
        }
        return .webp
    }

    /// Generate a paste-image filename with current timestamp and preferred format.
    static func pasteFilename(format: PasteImageFormat? = nil) -> String {
        let fmt = format ?? preferredFormat
        let df = DateFormatter()
        df.dateFormat = "yyyyMMdd-HHmmss"
        return "paste-image-\(df.string(from: .now)).\(fmt.fileExtension)"
    }
}
