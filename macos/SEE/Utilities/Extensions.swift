import Foundation
import SwiftUI

extension Date {
    var relativeFormatted: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: self, relativeTo: .now)
    }

    var shortFormatted: String {
        formatted(date: .abbreviated, time: .shortened)
    }
}

extension Int64 {
    var formattedFileSize: String {
        ByteCountFormatter.string(fromByteCount: self, countStyle: .file)
    }
}

extension URL {
    var isValid: Bool {
        guard let scheme = scheme?.lowercased() else { return false }
        return (scheme == "http" || scheme == "https") && host != nil
    }
}

extension String {
    var isValidURL: Bool {
        guard let url = URL(string: self) else { return false }
        return url.isValid
    }
}

#if os(macOS)
import AppKit

extension NSPasteboard {
    func setString(_ string: String) {
        clearContents()
        setString(string, forType: .string)
    }
}
#endif

#if os(iOS)
import UIKit
#endif

enum ClipboardService {
    static func copy(_ string: String) {
        #if os(macOS)
        NSPasteboard.general.setString(string)
        #elseif os(iOS)
        UIPasteboard.general.string = string
        #endif
    }

    static func getString() -> String? {
        #if os(macOS)
        return NSPasteboard.general.string(forType: .string)
        #elseif os(iOS)
        return UIPasteboard.general.string
        #endif
    }
}
