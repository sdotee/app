import SwiftUI
import SwiftData

// This view is integrated into FileUploadView.
// Keeping as a separate file for potential future standalone use.

struct UploadedFileListView: View {
    @Query(sort: \UploadedFile.createdAt, order: .reverse) private var files: [UploadedFile]
    var linkDisplayType: LinkDisplayType = .directLink

    var body: some View {
        List {
            ForEach(files) { file in
                UploadedFileRow(file: file, linkDisplayType: linkDisplayType) {
                    // Delete handled by parent
                }
            }
        }
        .navigationTitle(String(localized: "Uploaded Files"))
    }
}
