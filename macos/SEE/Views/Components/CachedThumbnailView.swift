import SwiftUI

struct CachedThumbnailView: View {
    let identifier: String
    let size: CGFloat
    let fallbackIcon: String

    @State private var image: Image?
    @State private var isLoading = true

    private var pixelSize: Int { Int(size * 2) }

    init(identifier: String, size: CGFloat, fallbackIcon: String = "doc.fill") {
        self.identifier = identifier
        self.size = size
        self.fallbackIcon = fallbackIcon
    }

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
                Image(systemName: fallbackIcon)
                    .font(.title3)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .task(id: identifier) {
            await loadThumbnail()
        }
        .onReceive(NotificationCenter.default.publisher(for: .thumbnailCached)) { notification in
            if let cachedID = notification.userInfo?["identifier"] as? String,
               cachedID == identifier, image == nil {
                Task { await loadThumbnail() }
            }
        }
    }

    private func loadThumbnail() async {
        isLoading = true
        image = await ThumbnailService.shared.loadThumbnail(
            for: identifier,
            size: pixelSize
        )
        isLoading = false
    }
}
