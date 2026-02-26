import Foundation

enum LinkFormatter {
    enum FileCategory {
        case image
        case audio
        case video
        case other
    }

    static func category(for filename: String) -> FileCategory {
        let ext = (filename as NSString).pathExtension.lowercased()
        if ["jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "avif", "ico", "tiff"].contains(ext) {
            return .image
        }
        if ["mp3", "wav", "flac", "aac", "ogg", "m4a", "wma"].contains(ext) {
            return .audio
        }
        if ["mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v"].contains(ext) {
            return .video
        }
        return .other
    }

    // MARK: - BBCode

    /// Plain BBCode tag only: [img]url[/img], [audio]url[/audio], [video]url[/video], or [url=directURL]filename[/url]
    static func bbcode(filename: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "[img]\(directURL)[/img]"
        case .audio:
            return "[audio]\(directURL)[/audio]"
        case .video:
            return "[video]\(directURL)[/video]"
        case .other:
            return "[url=\(directURL)]\(filename)[/url]"
        }
    }

    /// BBCode tag wrapped in page URL link (audio/video: bare tag, no wrapper)
    static func bbcodeWithLink(filename: String, pageURL: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "[url=\(pageURL)][img]\(directURL)[/img][/url]"
        case .audio:
            return "[audio]\(directURL)[/audio]"
        case .video:
            return "[video]\(directURL)[/video]"
        case .other:
            return "[url=\(pageURL)]\(filename)[/url]"
        }
    }

    /// BBCode tag wrapped in direct URL link (audio/video: bare tag, no wrapper)
    static func bbcodeDirectLink(filename: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "[url=\(directURL)][img]\(directURL)[/img][/url]"
        case .audio:
            return "[audio]\(directURL)[/audio]"
        case .video:
            return "[video]\(directURL)[/video]"
        case .other:
            return "[url=\(directURL)]\(filename)[/url]"
        }
    }

    // MARK: - HTML

    /// Plain HTML tag only: <img>, <audio>, <video>, or <a>
    static func html(filename: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "<img src=\"\(directURL)\" alt=\"\(filename)\" title=\"\(filename)\">"
        case .audio:
            return "<audio src=\"\(directURL)\" controls>\(filename)</audio>"
        case .video:
            return "<video src=\"\(directURL)\" controls>\(filename)</video>"
        case .other:
            return "<a href=\"\(directURL)\">\(filename)</a>"
        }
    }

    /// HTML tag wrapped in page URL anchor
    static func htmlWithLink(filename: String, pageURL: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "<a href=\"\(pageURL)\" target=\"_blank\"><img src=\"\(directURL)\" alt=\"\(filename)\" title=\"\(filename)\"></a>"
        case .audio:
            return "<audio src=\"\(directURL)\" controls>\(filename)</audio>"
        case .video:
            return "<video src=\"\(directURL)\" controls>\(filename)</video>"
        case .other:
            return "<a href=\"\(pageURL)\" target=\"_blank\">\(filename)</a>"
        }
    }

    /// HTML tag wrapped in direct URL anchor
    static func htmlDirectLink(filename: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "<a href=\"\(directURL)\" target=\"_blank\"><img src=\"\(directURL)\" alt=\"\(filename)\" title=\"\(filename)\"></a>"
        case .audio:
            return "<audio src=\"\(directURL)\" controls>\(filename)</audio>"
        case .video:
            return "<video src=\"\(directURL)\" controls>\(filename)</video>"
        case .other:
            return "<a href=\"\(directURL)\" target=\"_blank\">\(filename)</a>"
        }
    }

    // MARK: - Markdown

    /// Markdown: ![filename](directURL) for images, [filename](directURL) for others
    static func markdown(filename: String, directURL: String) -> String {
        switch category(for: filename) {
        case .image:
            return "![\(filename)](\(directURL))"
        case .audio, .video, .other:
            return "[\(filename)](\(directURL))"
        }
    }
}
