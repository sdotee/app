import Foundation
import SwiftData

@Model
final class TextShare {
    var slug: String
    var domain: String
    var shortURL: String
    var title: String
    var content: String
    var textType: String
    var customSlug: String?
    var hasPassword: Bool
    var expireAt: Date?
    var createdAt: Date
    var tagIDs: [Int]

    init(
        slug: String,
        domain: String,
        shortURL: String,
        title: String,
        content: String,
        textType: String = TextType.plainText.rawValue,
        customSlug: String? = nil,
        hasPassword: Bool = false,
        expireAt: Date? = nil,
        createdAt: Date = .now,
        tagIDs: [Int] = []
    ) {
        self.slug = slug
        self.domain = domain
        self.shortURL = shortURL
        self.title = title
        self.content = content
        self.textType = textType
        self.customSlug = customSlug
        self.hasPassword = hasPassword
        self.expireAt = expireAt
        self.createdAt = createdAt
        self.tagIDs = tagIDs
    }
}
