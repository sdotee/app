import SwiftUI

struct CachedThumbnailView: View {
    let originalURL: String
    let size: CGFloat

    @State private var image: Image?
    @State private var isLoading = true

    // Request 2x pixels for Retina displays
    private var pixelWidth: Int { Int(size * 2) }
    private var pixelHeight: Int { Int(size * 2) }

    var body: some View {
        Group {
            if let image {
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else if isLoading {
                ProgressView()
                    .controlSize(.small)
            } else {
                Image(systemName: "photo")
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .task(id: originalURL) {
            await loadThumbnail()
        }
    }

    private func loadThumbnail() async {
        isLoading = true
        image = await ThumbnailService.shared.loadThumbnail(
            for: originalURL,
            width: pixelWidth,
            height: pixelHeight
        )
        isLoading = false
    }
}
