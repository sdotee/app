import Foundation
import SwiftData

@Model
final class ShortLink {
    var slug: String
    var domain: String
    var targetURL: String
    var shortURL: String
    var title: String
    var customSlug: String?
    var hasPassword: Bool
    var expireAt: Date?
    var createdAt: Date
    var tagIDs: [Int]

    init(
        slug: String,
        domain: String,
        targetURL: String,
        shortURL: String,
        title: String = "",
        customSlug: String? = nil,
        hasPassword: Bool = false,
        expireAt: Date? = nil,
        createdAt: Date = .now,
        tagIDs: [Int] = []
    ) {
        self.slug = slug
        self.domain = domain
        self.targetURL = targetURL
        self.shortURL = shortURL
        self.title = title
        self.customSlug = customSlug
        self.hasPassword = hasPassword
        self.expireAt = expireAt
        self.createdAt = createdAt
        self.tagIDs = tagIDs
    }
}
