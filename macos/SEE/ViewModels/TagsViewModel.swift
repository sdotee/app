import Foundation

@MainActor
@Observable
final class TagsViewModel {
    var tags: [Tag] = []
    var isLoading = false
    var errorMessage: String?

    func loadTags() async {
        isLoading = true
        defer { isLoading = false }

        do {
            let response: APIResponse<TagsResponse> = try await APIClient.shared.request(.getTags)
            tags = response.data?.tags ?? []
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
