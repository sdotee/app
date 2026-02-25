import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
final class FileUploadViewModel {
    var isLoading = false
    var uploadProgress: Double = 0
    var errorMessage: String?
    var successMessage: String?
    var domains: [String] = []
    var selectedDomain = ""

    func loadDomains() async {
        do {
            let response: APIResponse<DomainsResponse> = try await APIClient.shared.request(.getFileDomains)
            domains = response.data?.domains ?? []
            if selectedDomain.isEmpty, let first = domains.first {
                selectedDomain = UserDefaults.standard.string(forKey: Constants.defaultFileDomainKey) ?? first
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func uploadFile(data: Data, filename: String, context: ModelContext) async -> Bool {
        isLoading = true
        uploadProgress = 0
        defer { isLoading = false }

        do {
            let response = try await APIClient.shared.uploadFile(
                data,
                filename: filename,
                domain: selectedDomain.isEmpty ? nil : selectedDomain,
                progress: { [weak self] progress in
                    Task { @MainActor in
                        self?.uploadProgress = progress
                    }
                }
            )

            let file = UploadedFile(
                fileID: response.fileID,
                filename: response.filename,
                storename: response.storename,
                size: response.size,
                width: response.width,
                height: response.height,
                url: response.url,
                page: response.page,
                path: response.path,
                deleteHash: response.hash,
                deleteURL: response.delete
            )
            context.insert(file)
            ClipboardService.copy(response.url)
            successMessage = String(localized: "File uploaded and link copied!")
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteFile(_ file: UploadedFile, context: ModelContext) async -> Bool {
        isLoading = true
        defer { isLoading = false }

        do {
            try await APIClient.shared.requestNoBody(.deleteFile(hash: file.deleteHash))
            context.delete(file)
            successMessage = String(localized: "File deleted")
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}
