import SwiftUI

actor ThumbnailService {
    static let shared = ThumbnailService()

    private let cdnBase = "https://cache.seecdn.net/"
    private let cacheDirectory: URL
    private var memoryCache: [String: Image] = [:]

    init() {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        cacheDirectory = caches.appendingPathComponent("Thumbnails", isDirectory: true)
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }

    func thumbnailURL(for originalURL: String, width: Int, height: Int) -> URL? {
        var components = URLComponents(string: cdnBase)
        components?.queryItems = [
            URLQueryItem(name: "url", value: originalURL),
            URLQueryItem(name: "w", value: String(width)),
            URLQueryItem(name: "h", value: String(height)),
            URLQueryItem(name: "fit", value: "cover"),
            URLQueryItem(name: "a", value: "attention"),
        ]
        return components?.url
    }

    private func cacheKey(for url: String, width: Int, height: Int) -> String {
        let raw = "\(url)_\(width)x\(height)"
        // Simple hash for filename safety
        var hash: UInt64 = 5381
        for byte in raw.utf8 {
            hash = 127 &* (hash & 0x00ff_ffff_ffff_ffff) &+ UInt64(byte)
        }
        return String(hash, radix: 16)
    }

    private func diskCachePath(for key: String) -> URL {
        cacheDirectory.appendingPathComponent(key)
    }

    func loadThumbnail(for originalURL: String, width: Int, height: Int) async -> Image? {
        let key = cacheKey(for: originalURL, width: width, height: height)

        // Check memory cache
        if let cached = memoryCache[key] {
            return cached
        }

        // Check disk cache
        let diskPath = diskCachePath(for: key)
        if FileManager.default.fileExists(atPath: diskPath.path) {
            if let image = loadImageFromDisk(diskPath) {
                memoryCache[key] = image
                return image
            }
        }

        // Fetch from CDN
        guard let cdnURL = thumbnailURL(for: originalURL, width: width, height: height) else {
            return nil
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: cdnURL)
            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200,
                  !data.isEmpty else {
                return nil
            }

            // Save to disk
            try? data.write(to: diskPath)

            // Create image
            if let image = createImage(from: data) {
                memoryCache[key] = image
                return image
            }
        } catch {
            // Silently fail - will show placeholder
        }

        return nil
    }

    private func loadImageFromDisk(_ url: URL) -> Image? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return createImage(from: data)
    }

    private func createImage(from data: Data) -> Image? {
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
