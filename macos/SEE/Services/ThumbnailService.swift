import SwiftUI
import QuickLookThumbnailing
import UniformTypeIdentifiers

actor ThumbnailService {
    static let shared = ThumbnailService()

    private let cacheDirectory: URL
    private var memoryCache: [String: Image] = [:]

    init() {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        cacheDirectory = caches.appendingPathComponent("Thumbnails", isDirectory: true)
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }

    private func cacheKey(for identifier: String, size: Int) -> String {
        let raw = "\(identifier)_\(size)"
        var hash: UInt64 = 5381
        for byte in raw.utf8 {
            hash = 127 &* (hash & 0x00ff_ffff_ffff_ffff) &+ UInt64(byte)
        }
        return String(hash, radix: 16)
    }

    private func diskCachePath(for key: String) -> URL {
        cacheDirectory.appendingPathComponent(key + ".png")
    }

    // MARK: - Generate thumbnail from local file and cache it

    func generateAndCache(for fileURL: URL, identifier: String, size: Int) async {
        let key = cacheKey(for: identifier, size: size)
        let diskPath = diskCachePath(for: key)

        // Skip if already cached
        if memoryCache[key] != nil || FileManager.default.fileExists(atPath: diskPath.path) {
            return
        }

        let request = QLThumbnailGenerator.Request(
            fileAt: fileURL,
            size: CGSize(width: size, height: size),
            scale: 2.0,
            representationTypes: .thumbnail
        )

        do {
            try await QLThumbnailGenerator.shared.saveBestRepresentation(
                for: request,
                to: diskPath,
                as: UTType.png
            )
            // Notify views that a thumbnail is ready
            await MainActor.run {
                NotificationCenter.default.post(
                    name: .thumbnailCached,
                    object: nil,
                    userInfo: ["identifier": identifier]
                )
            }
        } catch {
            // QLThumbnailGenerator failed — no thumbnail for this file type
        }
    }

    // MARK: - Load cached thumbnail

    func loadThumbnail(for identifier: String, size: Int) async -> Image? {
        let key = cacheKey(for: identifier, size: size)

        if let cached = memoryCache[key] {
            return cached
        }

        let diskPath = diskCachePath(for: key)
        if FileManager.default.fileExists(atPath: diskPath.path) {
            if let image = loadImageFromDisk(diskPath) {
                memoryCache[key] = image
                return image
            }
        }

        return nil
    }

    private func loadImageFromDisk(_ url: URL) -> Image? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        #if os(macOS)
        guard let nsImage = NSImage(data: data) else { return nil }
        return Image(nsImage: nsImage)
        #else
        guard let uiImage = UIImage(data: data) else { return nil }
        return Image(uiImage: uiImage)
        #endif
    }

    func clearCache() {
        memoryCache.removeAll()
        try? FileManager.default.removeItem(at: cacheDirectory)
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }
}

extension Notification.Name {
    static let thumbnailCached = Notification.Name("thumbnailCached")
}
