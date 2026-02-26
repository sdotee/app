import Foundation
import SwiftUI
import SwiftData
import AVFoundation

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

            // Extract local media metadata (video: dimensions + duration, audio: duration)
            var localWidth = response.width
            var localHeight = response.height
            var localDuration: Double? = nil

            let ext = (filename as NSString).pathExtension.lowercased()
            let videoExts = ["mp4", "mov", "m4v", "avi", "mkv", "webm", "3gp"]
            let audioExts = ["mp3", "m4a", "aac", "wav", "flac", "ogg", "wma", "aiff"]
            if videoExts.contains(ext) || audioExts.contains(ext) {
                let tempMediaURL = FileManager.default.temporaryDirectory
                    .appendingPathComponent(UUID().uuidString + "." + ext)
                if (try? data.write(to: tempMediaURL)) != nil {
                    let asset = AVURLAsset(url: tempMediaURL)
                    // Video: extract dimensions
                    if videoExts.contains(ext),
                       let track = try? await asset.loadTracks(withMediaType: .video).first {
                        let size = try? await track.load(.naturalSize)
                        let transform = try? await track.load(.preferredTransform)
                        if let size, let transform {
                            let transformed = size.applying(transform)
                            localWidth = Int(abs(transformed.width))
                            localHeight = Int(abs(transformed.height))
                        }
                    }
                    // Video + Audio: extract duration
                    let duration = try? await asset.load(.duration)
                    if let duration {
                        let seconds = CMTimeGetSeconds(duration)
                        if seconds.isFinite && seconds > 0 {
                            localDuration = seconds
                        }
                    }
                    try? FileManager.default.removeItem(at: tempMediaURL)
                }
            }

            let file = UploadedFile(
                fileID: response.fileID,
                filename: response.filename,
                storename: response.storename,
                size: response.size,
                width: localWidth,
                height: localHeight,
                duration: localDuration,
                url: response.url,
                page: response.page,
                path: response.path,
                deleteHash: response.hash,
                deleteURL: response.delete
            )
            context.insert(file)

            // Generate local thumbnail in background (don't block upload)
            let responseURL = response.url
            Task.detached(priority: .utility) {
                let tempURL = FileManager.default.temporaryDirectory
                    .appendingPathComponent(UUID().uuidString + "-" + filename)
                if (try? data.write(to: tempURL)) != nil {
                    await ThumbnailService.shared.generateAndCache(
                        for: tempURL,
                        identifier: responseURL,
                        size: 88
                    )
                    try? FileManager.default.removeItem(at: tempURL)
                }
            }

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
