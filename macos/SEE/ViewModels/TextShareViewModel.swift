import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
final class TextShareViewModel {
    var isLoading = false
    var errorMessage: String?
    var successMessage: String?
    var domains: [String] = []
    var tags: [Tag] = []

    // Form state
    var title = ""
    var content = ""
    var textType: TextType = .plainText
    var selectedDomain = ""
    var customSlug = ""
    var password = ""
    var expireAt: Date?
    var enableExpiry = false
    var selectedTagIDs: [Int] = []

    func loadDomains() async {
        do {
            let response: APIResponse<DomainsResponse> = try await APIClient.shared.request(.getTextDomains)
            domains = response.data?.domains ?? []
            if selectedDomain.isEmpty, let first = domains.first {
                selectedDomain = UserDefaults.standard.string(forKey: Constants.defaultTextDomainKey) ?? first
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
            // Non-critical
        }
    }

    func createTextShare(context: ModelContext) async -> Bool {
        guard !content.isEmpty else {
            errorMessage = String(localized: "Content is required")
            return false
        }

        isLoading = true
        defer { isLoading = false }

        let effectiveTitle = title.isEmpty ? "Untitled" : title

        var request = CreateTextRequest(
            content: content,
            title: effectiveTitle
        )
        if !selectedDomain.isEmpty { request.domain = selectedDomain }
        if !customSlug.isEmpty { request.customSlug = customSlug }
        request.textType = textType.rawValue
        if !password.isEmpty { request.password = password }
        if enableExpiry, let expireAt {
            request.expireAt = Int(expireAt.timeIntervalSince1970)
        }
        if !selectedTagIDs.isEmpty { request.tagIDs = selectedTagIDs }

        do {
            let response: APIResponse<TextResponse> = try await APIClient.shared.request(.createText(request))
            if let data = response.data {
                let share = TextShare(
                    slug: data.slug,
                    domain: selectedDomain.isEmpty ? "fs.to" : selectedDomain,
                    shortURL: data.shortURL,
                    title: effectiveTitle,
                    content: content,
                    textType: textType.rawValue,
                    customSlug: data.customSlug,
                    hasPassword: !password.isEmpty,
                    expireAt: enableExpiry ? expireAt : nil,
                    tagIDs: selectedTagIDs
                )
                context.insert(share)
                ClipboardService.copy(data.shortURL)
                successMessage = String(localized: "Text share created and link copied!")
                resetForm()
                return true
            }
            return false
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func updateTextShare(_ share: TextShare, context: ModelContext) async -> Bool {
        guard !content.isEmpty else {
            errorMessage = String(localized: "Content is required")
            return false
        }

        isLoading = true
        defer { isLoading = false }

        let effectiveTitle = title.isEmpty ? "Untitled" : title
        let request = UpdateTextRequest(
            domain: share.domain,
            slug: share.slug,
            content: content,
            title: effectiveTitle
        )

        do {
            let _: APIResponse<TextResponse> = try await APIClient.shared.request(.updateText(request))
            share.content = content
            share.title = effectiveTitle
            successMessage = String(localized: "Text share updated!")
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteTextShare(_ share: TextShare, context: ModelContext) async -> Bool {
        isLoading = true
        defer { isLoading = false }

        let request = DeleteTextRequest(domain: share.domain, slug: share.slug)
        do {
            try await APIClient.shared.requestNoBody(.deleteText(request))
            context.delete(share)
            successMessage = String(localized: "Text share deleted")
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func populateForm(from share: TextShare) {
        title = share.title
        content = share.content
        textType = TextType(rawValue: share.textType) ?? .plainText
        selectedDomain = share.domain
        customSlug = share.customSlug ?? ""
        expireAt = share.expireAt
        enableExpiry = share.expireAt != nil
        selectedTagIDs = share.tagIDs
    }

    func resetForm() {
        title = ""
        content = ""
        textType = .plainText
        customSlug = ""
        password = ""
        expireAt = nil
        enableExpiry = false
        selectedTagIDs = []
    }
}
