import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
final class ShortLinkViewModel {
    var isLoading = false
    var errorMessage: String?
    var successMessage: String?
    var domains: [String] = []
    var tags: [Tag] = []

    // Create/Edit form state
    var targetURL = ""
    var selectedDomain = ""
    var customSlug = ""
    var title = ""
    var password = ""
    var expireAt: Date?
    var enableExpiry = false
    var expirationRedirectURL = ""
    var selectedTagIDs: [Int] = []

    // Stats
    var dailyVisits: Int?
    var monthlyVisits: Int?
    var totalVisits: Int?

    func loadDomains() async {
        do {
            let response: APIResponse<DomainsResponse> = try await APIClient.shared.request(.getDomains)
            domains = response.data?.domains ?? []
            if selectedDomain.isEmpty, let first = domains.first {
                selectedDomain = UserDefaults.standard.string(forKey: Constants.defaultShortLinkDomainKey) ?? first
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func loadTags() async {
        do {
            let response: APIResponse<TagsResponse> = try await APIClient.shared.request(.getTags)
            tags = response.data?.tags ?? []
        } catch {
            // Non-critical, silently ignore
        }
    }

    func createShortLink(context: ModelContext) async -> Bool {
        guard !targetURL.isEmpty else {
            errorMessage = String(localized: "Target URL is required")
            return false
        }
        guard targetURL.isValidURL else {
            errorMessage = String(localized: "Please enter a valid URL")
            return false
        }

        isLoading = true
        defer { isLoading = false }

        var request = CreateShortURLRequest(
            targetURL: targetURL,
            domain: selectedDomain
        )
        if !customSlug.isEmpty { request.customSlug = customSlug }
        if !title.isEmpty { request.title = title }
        if !password.isEmpty { request.password = password }
        if enableExpiry, let expireAt {
            request.expireAt = Int(expireAt.timeIntervalSince1970)
        }
        if !expirationRedirectURL.isEmpty { request.expirationRedirectURL = expirationRedirectURL }
        if !selectedTagIDs.isEmpty { request.tagIDs = selectedTagIDs }

        do {
            let response: APIResponse<ShortURLResponse> = try await APIClient.shared.request(.createShortURL(request))
            if let data = response.data {
                let link = ShortLink(
                    slug: data.slug,
                    domain: selectedDomain,
                    targetURL: targetURL,
                    shortURL: data.shortURL,
                    title: title,
                    customSlug: data.customSlug,
                    hasPassword: !password.isEmpty,
                    expireAt: enableExpiry ? expireAt : nil,
                    tagIDs: selectedTagIDs
                )
                context.insert(link)
                ClipboardService.copy(data.shortURL)
                successMessage = String(localized: "Short link created and copied!")
                resetForm()
                return true
            }
            return false
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func updateShortLink(_ link: ShortLink, context: ModelContext) async -> Bool {
        guard !targetURL.isEmpty, targetURL.isValidURL else {
            errorMessage = String(localized: "Please enter a valid URL")
            return false
        }

        isLoading = true
        defer { isLoading = false }

        let request = UpdateShortURLRequest(
            domain: link.domain,
            slug: link.slug,
            targetURL: targetURL,
            title: title
        )

        do {
            let _: APIResponse<ShortURLResponse> = try await APIClient.shared.request(.updateShortURL(request))
            link.targetURL = targetURL
            link.title = title
            successMessage = String(localized: "Short link updated!")
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteShortLink(_ link: ShortLink, context: ModelContext) async -> Bool {
        isLoading = true
        defer { isLoading = false }

        let request = DeleteShortURLRequest(domain: link.domain, slug: link.slug)
        do {
            try await APIClient.shared.requestNoBody(.deleteShortURL(request))
            context.delete(link)
            successMessage = String(localized: "Short link deleted")
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func loadStats(domain: String, slug: String) async {
        do {
            let d: APIResponse<VisitStatResponse> = try await APIClient.shared.request(
                .getLinkVisitStat(domain: domain, slug: slug, period: "daily")
            )
            dailyVisits = d.data?.visitCount

            let m: APIResponse<VisitStatResponse> = try await APIClient.shared.request(
                .getLinkVisitStat(domain: domain, slug: slug, period: "monthly")
            )
            monthlyVisits = m.data?.visitCount

            let t: APIResponse<VisitStatResponse> = try await APIClient.shared.request(
                .getLinkVisitStat(domain: domain, slug: slug, period: "totally")
            )
            totalVisits = t.data?.visitCount
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func populateForm(from link: ShortLink) {
        targetURL = link.targetURL
        selectedDomain = link.domain
        customSlug = link.customSlug ?? ""
        title = link.title
        expireAt = link.expireAt
        enableExpiry = link.expireAt != nil
        selectedTagIDs = link.tagIDs
    }

    func resetForm() {
        targetURL = ""
        customSlug = ""
        title = ""
        password = ""
        expireAt = nil
        enableExpiry = false
        expirationRedirectURL = ""
        selectedTagIDs = []
    }
}

// Empty response helper
/// Accepts any JSON value (true, null, string, object, etc.) for endpoints
/// that return non-structured data fields.
struct EmptyData: Decodable, Sendable {
    init() {}
    init(from decoder: Decoder) throws {
        // Consume whatever value is present without storing it
    }
}
