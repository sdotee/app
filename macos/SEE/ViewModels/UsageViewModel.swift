import Foundation

@MainActor
@Observable
final class UsageViewModel {
    var usage: UsageResponse?
    var isLoading = false
    var errorMessage: String?
    var isAvailable = true

    func loadUsage() async {
        isLoading = true
        defer { isLoading = false }

        do {
            let response: APIResponse<UsageResponse> = try await APIClient.shared.request(.getUsage)
            usage = response.data
        } catch let error as APIError {
            // If 404 or not found, gracefully hide the feature
            if case .unexpectedStatusCode(let code, _) = error, code == 404 {
                isAvailable = false
            } else {
                errorMessage = error.localizedDescription
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
