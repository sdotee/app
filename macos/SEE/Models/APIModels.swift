import Foundation

// MARK: - Generic API Response

struct APIResponse<T: Decodable>: Decodable, @unchecked Sendable {
    let code: Int
    let message: String?
    let data: T?
}

// MARK: - Domains

struct DomainsResponse: Decodable, Sendable {
    let domains: [String]
}

// MARK: - Short URL

struct CreateShortURLRequest: Encodable, Sendable {
    let targetURL: String
    let domain: String
    var customSlug: String?
    var title: String?
    var password: String?
    var expireAt: Int?
    var expirationRedirectURL: String?
    var tagIDs: [Int]?

    enum CodingKeys: String, CodingKey {
        case targetURL = "target_url"
        case domain
        case customSlug = "custom_slug"
        case title
        case password
        case expireAt = "expire_at"
        case expirationRedirectURL = "expiration_redirect_url"
        case tagIDs = "tag_ids"
    }
}

struct UpdateShortURLRequest: Encodable, Sendable {
    let domain: String
    let slug: String
    let targetURL: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case domain, slug
        case targetURL = "target_url"
        case title
    }
}

struct DeleteShortURLRequest: Encodable, Sendable {
    let domain: String
    let slug: String
}

struct ShortURLResponse: Decodable, Sendable {
    let shortURL: String
    let slug: String
    let customSlug: String?

    enum CodingKeys: String, CodingKey {
        case shortURL = "short_url"
        case slug
        case customSlug = "custom_slug"
    }
}

struct VisitStatResponse: Decodable, Sendable {
    let visitCount: Int

    enum CodingKeys: String, CodingKey {
        case visitCount = "visit_count"
    }
}

// MARK: - Text Sharing

struct CreateTextRequest: Encodable, Sendable {
    let content: String
    let title: String
    var domain: String?
    var customSlug: String?
    var textType: String?
    var password: String?
    var expireAt: Int?
    var tagIDs: [Int]?

    enum CodingKeys: String, CodingKey {
        case content, title, domain
        case customSlug = "custom_slug"
        case textType = "text_type"
        case password
        case expireAt = "expire_at"
        case tagIDs = "tag_ids"
    }
}

struct UpdateTextRequest: Encodable, Sendable {
    let domain: String
    let slug: String
    let content: String
    let title: String
}

struct DeleteTextRequest: Encodable, Sendable {
    let domain: String
    let slug: String
}

struct TextResponse: Decodable, Sendable {
    let shortURL: String
    let slug: String
    let customSlug: String?

    enum CodingKeys: String, CodingKey {
        case shortURL = "short_url"
        case slug
        case customSlug = "custom_slug"
    }
}

// MARK: - File Upload

struct UploadFileResponse: Decodable, Sendable {
    let fileID: Int
    let filename: String
    let storename: String
    let size: Int64
    let width: Int?
    let height: Int?
    let url: String
    let page: String
    let path: String
    let hash: String
    let delete: String
    let uploadStatus: Int

    enum CodingKeys: String, CodingKey {
        case fileID = "file_id"
        case filename, storename, size, width, height, url, page, path, hash, delete
        case uploadStatus = "upload_status"
    }
}

// MARK: - Tags

struct TagsResponse: Decodable, Sendable {
    let tags: [Tag]
}

struct Tag: Decodable, Sendable, Identifiable, Hashable {
    let id: Int
    let name: String
}

// MARK: - Usage

struct UsageResponse: Decodable, Sendable {
    let apiCountDay: Int
    let apiCountDayLimit: Int
    let apiCountMonth: Int
    let apiCountMonthLimit: Int
    let linkCountDay: Int
    let linkCountDayLimit: Int
    let linkCountMonth: Int
    let linkCountMonthLimit: Int
    let qrcodeCountDay: Int
    let qrcodeCountDayLimit: Int
    let qrcodeCountMonth: Int
    let qrcodeCountMonthLimit: Int

    enum CodingKeys: String, CodingKey {
        case apiCountDay = "api_count_day"
        case apiCountDayLimit = "api_count_day_limit"
        case apiCountMonth = "api_count_month"
        case apiCountMonthLimit = "api_count_month_limit"
        case linkCountDay = "link_count_day"
        case linkCountDayLimit = "link_count_day_limit"
        case linkCountMonth = "link_count_month"
        case linkCountMonthLimit = "link_count_month_limit"
        case qrcodeCountDay = "qrcode_count_day"
        case qrcodeCountDayLimit = "qrcode_count_day_limit"
        case qrcodeCountMonth = "qrcode_count_month"
        case qrcodeCountMonthLimit = "qrcode_count_month_limit"
    }
}

// MARK: - Text Type Enum

enum TextType: String, CaseIterable, Sendable, Identifiable {
    case plainText = "plain_text"
    case sourceCode = "source_code"
    case markdown = "markdown"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .plainText: String(localized: "Plain Text")
        case .sourceCode: String(localized: "Source Code")
        case .markdown: String(localized: "Markdown")
        }
    }
}

// MARK: - Visit Stat Period

enum VisitStatPeriod: String, CaseIterable, Sendable, Identifiable {
    case daily
    case monthly
    case totally

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .daily: String(localized: "Today")
        case .monthly: String(localized: "This Month")
        case .totally: String(localized: "Total")
        }
    }
}
