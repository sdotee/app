import SwiftUI
import SwiftData

#if os(macOS)
struct MenuBarView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.openWindow) private var openWindow
    @Query(sort: \ShortLink.createdAt, order: .reverse) private var recentLinks: [ShortLink]
    @Query(sort: \UploadedFile.createdAt, order: .reverse) private var recentFiles: [UploadedFile]
    @State private var targetURL = ""
    @State private var isLoading = false
    @State private var resultURL: String?
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            // MARK: - Shorten URL
            VStack(spacing: 8) {
                HStack(spacing: 6) {
                    TextField(String(localized: "Shorten a URL"), text: $targetURL)
                        .textFieldStyle(.roundedBorder)
                        .onSubmit { shorten() }

                    Button(action: shorten) {
                        if isLoading {
                            ProgressView()
                                .controlSize(.small)
                        } else {
                            Image(systemName: "arrow.right.circle.fill")
                                .font(.title3)
                        }
                    }
                    .disabled(targetURL.isEmpty || isLoading)
                    .buttonStyle(.borderless)
                }

                if let resultURL {
                    HStack(spacing: 6) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                            .font(.caption)
                        Text(resultURL)
                            .font(.caption)
                            .foregroundStyle(Color.accentColor)
                            .lineLimit(1)
                        Spacer()
                        Button {
                            ClipboardService.copy(resultURL)
                        } label: {
                            Image(systemName: "doc.on.doc")
                                .font(.caption)
                        }
                        .buttonStyle(.borderless)
                    }
                    .padding(6)
                    .background(Color.green.opacity(0.08), in: RoundedRectangle(cornerRadius: 6))
                }

                if let errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            .padding(12)

            Divider()

            // MARK: - Quick Actions
            VStack(spacing: 2) {
                MenuBarButton(title: String(localized: "Paste & Shorten from Clipboard"), icon: "doc.on.clipboard") {
                    if let clipboard = ClipboardService.getString() {
                        targetURL = clipboard
                        if clipboard.isValidURL {
                            shorten()
                        }
                    }
                }

                MenuBarButton(title: String(localized: "New Text Share"), icon: "doc.text") {
                    NSApp.activate(ignoringOtherApps: true)
                    NotificationCenter.default.post(name: .createTextShare, object: nil)
                }

                MenuBarButton(title: String(localized: "Upload from Clipboard"), icon: "photo.on.rectangle") {
                    uploadFromClipboard()
                }

                MenuBarButton(title: String(localized: "Upload File..."), icon: "arrow.up.doc") {
                    uploadFile()
                }
            }
            .padding(.vertical, 6)
            .padding(.horizontal, 6)

            // MARK: - Recent Items
            if !recentLinks.prefix(3).isEmpty || !recentFiles.prefix(2).isEmpty {
                Divider()

                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "Recent"))
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                        .padding(.horizontal, 10)
                        .padding(.top, 6)

                    ForEach(recentLinks.prefix(3)) { link in
                        MenuBarRecentItem(
                            title: link.shortURL,
                            subtitle: link.targetURL,
                            icon: "link"
                        ) {
                            ClipboardService.copy(link.shortURL)
                        }
                    }

                    ForEach(recentFiles.prefix(2)) { file in
                        MenuBarRecentItem(
                            title: file.filename,
                            subtitle: file.url,
                            icon: file.isImage ? "photo" : file.isVideo ? "film" : "doc"
                        ) {
                            ClipboardService.copy(file.url)
                        }
                    }
                }
                .padding(.vertical, 6)
                .padding(.horizontal, 6)
            }

            Divider()

            // MARK: - Footer
            VStack(spacing: 2) {
                MenuBarButton(title: String(localized: "Open S.EE"), icon: "macwindow") {
                    NSApp.activate(ignoringOtherApps: true)
                    if let window = NSApp.windows.first(where: { $0.title.contains("S.EE") || $0.identifier?.rawValue == "main" }) {
                        window.makeKeyAndOrderFront(nil)
                    } else {
                        openWindow(id: "main")
                    }
                }

                MenuBarButton(title: String(localized: "Settings..."), icon: "gearshape") {
                    NSApp.activate(ignoringOtherApps: true)
                    NSApp.sendAction(Selector(("showSettingsWindow:")), to: nil, from: nil)
                }

                Divider()
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)

                MenuBarButton(title: String(localized: "Quit S.EE"), icon: "power") {
                    NSApp.terminate(nil)
                }
            }
            .padding(.vertical, 6)
            .padding(.horizontal, 6)
        }
        .frame(width: 320)
        .onAppear {
            if let clipboard = ClipboardService.getString(), clipboard.isValidURL {
                targetURL = clipboard
            }
        }
    }

    // MARK: - Actions

    private func shorten() {
        guard !targetURL.isEmpty else { return }
        guard targetURL.isValidURL else {
            errorMessage = String(localized: "Please enter a valid URL")
            return
        }

        isLoading = true
        errorMessage = nil
        resultURL = nil

        let defaultDomain = UserDefaults.standard.string(forKey: Constants.defaultShortLinkDomainKey) ?? "s.ee"

        Task {
            do {
                let request = CreateShortURLRequest(
                    targetURL: targetURL,
                    domain: defaultDomain
                )
                let response: APIResponse<ShortURLResponse> = try await APIClient.shared.request(.createShortURL(request))
                if let data = response.data {
                    resultURL = data.shortURL
                    ClipboardService.copy(data.shortURL)

                    let link = ShortLink(
                        slug: data.slug,
                        domain: defaultDomain,
                        targetURL: targetURL,
                        shortURL: data.shortURL,
                        customSlug: data.customSlug
                    )
                    modelContext.insert(link)
                    targetURL = ""
                }
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }

    private func uploadFromClipboard() {
        guard let pasteboard = NSPasteboard.general.pasteboardItems?.first else { return }

        // Check for image data
        for imageType in [NSPasteboard.PasteboardType.tiff, .png] {
            if let data = pasteboard.data(forType: imageType) {
                uploadData(data, filename: "paste-image-\(Int(Date().timeIntervalSince1970)).png")
                return
            }
        }

        // Check for file URL
        if let urlString = pasteboard.string(forType: .fileURL),
           let url = URL(string: urlString),
           let data = try? Data(contentsOf: url) {
            uploadData(data, filename: url.lastPathComponent)
        }
    }

    private func uploadFile() {
        let panel = NSOpenPanel()
        panel.allowsMultipleSelection = false
        panel.canChooseDirectories = false
        if panel.runModal() == .OK, let url = panel.url,
           let data = try? Data(contentsOf: url) {
            uploadData(data, filename: url.lastPathComponent)
        }
    }

    private func uploadData(_ data: Data, filename: String) {
        isLoading = true
        errorMessage = nil
        resultURL = nil

        let defaultDomain = UserDefaults.standard.string(forKey: Constants.defaultFileDomainKey) ?? ""

        Task {
            do {
                let response = try await APIClient.shared.uploadFile(
                    data,
                    filename: filename,
                    domain: defaultDomain.isEmpty ? nil : defaultDomain,
                    progress: { _ in }
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
                modelContext.insert(file)

                resultURL = response.url
                ClipboardService.copy(response.url)
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}

// MARK: - Menu Bar Button

private struct MenuBarButton: View {
    let title: String
    let icon: String
    let action: () -> Void
    @State private var isHovered = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .frame(width: 16)
                    .foregroundStyle(.secondary)
                Text(title)
                    .font(.body)
                Spacer()
            }
            .contentShape(Rectangle())
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(isHovered ? Color.primary.opacity(0.1) : .clear, in: RoundedRectangle(cornerRadius: 4))
        }
        .buttonStyle(.plain)
        .onHover { isHovered = $0 }
    }
}

// MARK: - Recent Item Row

private struct MenuBarRecentItem: View {
    let title: String
    let subtitle: String
    let icon: String
    let onCopy: () -> Void
    @State private var isHovered = false
    @State private var copied = false

    var body: some View {
        Button {
            onCopy()
            copied = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                copied = false
            }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .frame(width: 16)
                    .foregroundStyle(.secondary)
                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(.caption)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .lineLimit(1)
                }
                Spacer()
                if copied {
                    Image(systemName: "checkmark")
                        .font(.caption2)
                        .foregroundStyle(.green)
                } else if isHovered {
                    Image(systemName: "doc.on.doc")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .contentShape(Rectangle())
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(isHovered ? Color.primary.opacity(0.1) : .clear, in: RoundedRectangle(cornerRadius: 4))
        }
        .buttonStyle(.plain)
        .onHover { isHovered = $0 }
    }
}
#endif
