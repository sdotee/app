import SwiftUI
import SwiftData
import UniformTypeIdentifiers
#if os(iOS)
import PhotosUI
#endif

enum LinkDisplayType: String, CaseIterable, Identifiable {
    case directLink = "Direct Link"
    case sharePage = "Share Page"
    case bbcode = "BBCode"
    case bbcodeWithLink = "BBCode w/ Link"
    case bbcodeDirectLink = "BBCode w/ Direct Link"
    case html = "HTML"
    case htmlWithLink = "HTML w/ Link"
    case htmlDirectLink = "HTML w/ Direct Link"
    case markdown = "Markdown"

    var id: String { rawValue }

    func formatted(file: UploadedFile) -> String {
        switch self {
        case .directLink:
            return file.url
        case .sharePage:
            return file.page
        case .bbcode:
            return LinkFormatter.bbcode(filename: file.filename, directURL: file.url)
        case .bbcodeWithLink:
            return LinkFormatter.bbcodeWithLink(filename: file.filename, pageURL: file.page, directURL: file.url)
        case .bbcodeDirectLink:
            return LinkFormatter.bbcodeDirectLink(filename: file.filename, directURL: file.url)
        case .html:
            return LinkFormatter.html(filename: file.filename, directURL: file.url)
        case .htmlWithLink:
            return LinkFormatter.htmlWithLink(filename: file.filename, pageURL: file.page, directURL: file.url)
        case .htmlDirectLink:
            return LinkFormatter.htmlDirectLink(filename: file.filename, directURL: file.url)
        case .markdown:
            return LinkFormatter.markdown(filename: file.filename, directURL: file.url)
        }
    }
}

struct FileUploadView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \UploadedFile.createdAt, order: .reverse) private var files: [UploadedFile]
    @State private var viewModel = FileUploadViewModel()
    @State private var showFilePicker = false
    @State private var fileToDelete: UploadedFile?
    @State private var selectedFileIDs: Set<Int> = []
    @State private var showBatchLinks = false
    @State private var currentPage = 1
    @State private var linkDisplayType: LinkDisplayType = {
        if let saved = UserDefaults.standard.string(forKey: Constants.defaultFileLinkDisplayKey),
           let type = LinkDisplayType(rawValue: saved) {
            return type
        }
        return .sharePage
    }()

    #if os(iOS)
    @State private var showPhotoPicker = false
    @State private var selectedPhoto: PhotosPickerItem?
    #endif

    private var fileTotalPages: Int { Pagination.totalPages(for: files.count) }
    private var pagedFiles: [UploadedFile] { Pagination.page(files, page: currentPage) }

    private var selectedFiles: [UploadedFile] {
        files.filter { selectedFileIDs.contains($0.fileID) }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Upload area
            uploadArea
                .padding()

            Divider()

            // File list
            if files.isEmpty {
                EmptyStateView(
                    icon: "arrow.up.doc",
                    title: String(localized: "No Uploaded Files"),
                    message: String(localized: "Upload files to share them with a link.")
                )
            } else {
                List {
                    ForEach(pagedFiles) { file in
                        HStack(spacing: 8) {
                            Image(systemName: selectedFileIDs.contains(file.fileID) ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(selectedFileIDs.contains(file.fileID) ? Color.accentColor : .secondary.opacity(0.4))
                                .font(.body)
                                .onTapGesture {
                                    if selectedFileIDs.contains(file.fileID) {
                                        selectedFileIDs.remove(file.fileID)
                                    } else {
                                        selectedFileIDs.insert(file.fileID)
                                    }
                                }

                            UploadedFileRow(file: file, linkDisplayType: linkDisplayType) {
                                fileToDelete = file
                            }
                        }
                    }

                    if fileTotalPages > 1 {
                        PaginationView(currentPage: currentPage, totalPages: fileTotalPages) { page in
                            currentPage = page
                        }
                        .frame(maxWidth: .infinity)
                        .listRowSeparator(.hidden)
                    }
                }
            }
        }
        .navigationTitle(String(localized: "File Upload"))
        .toolbar {
            ToolbarItem(placement: .automatic) {
                Picker(String(localized: "Link Type"), selection: $linkDisplayType) {
                    ForEach(LinkDisplayType.allCases) { type in
                        Text(type.rawValue).tag(type)
                    }
                }
            }
            ToolbarItem(placement: .automatic) {
                Button(action: { showBatchLinks = true }) {
                    Label(
                        selectedFileIDs.isEmpty
                            ? String(localized: "Get Links")
                            : String(localized: "Get Links (\(selectedFileIDs.count))"),
                        systemImage: "link.badge.plus"
                    )
                }
                .disabled(selectedFileIDs.isEmpty)
            }
            #if os(macOS)
            ToolbarItem(placement: .automatic) {
                Button(action: handlePasteUpload) {
                    Label(String(localized: "Paste"), systemImage: "doc.on.clipboard")
                }
                .keyboardShortcut("v", modifiers: .command)
                .disabled(viewModel.isLoading)
            }
            #endif
        }
        .alert(String(localized: "Delete File?"), isPresented: .init(
            get: { fileToDelete != nil },
            set: { if !$0 { fileToDelete = nil } }
        )) {
            Button(String(localized: "Cancel"), role: .cancel) {}
            Button(String(localized: "Delete"), role: .destructive) {
                if let file = fileToDelete {
                    Task {
                        let _ = await viewModel.deleteFile(file, context: modelContext)
                    }
                }
            }
        } message: {
            Text(String(localized: "This will permanently delete the file."))
        }
        .sheet(isPresented: $showBatchLinks) {
            BatchLinksView(files: selectedFiles, linkDisplayType: linkDisplayType) {
                selectedFileIDs.removeAll()
                showBatchLinks = false
            }
        }
        .fileImporter(
            isPresented: $showFilePicker,
            allowedContentTypes: [.data],
            allowsMultipleSelection: true
        ) { result in
            handleFilePicker(result)
        }
        #if os(iOS)
        .photosPicker(isPresented: $showPhotoPicker, selection: $selectedPhoto)
        .onChange(of: selectedPhoto) { _, newValue in
            if let item = newValue {
                handlePhotoPicker(item)
            }
        }
        #endif
        .toast(message: $viewModel.successMessage)
        .toast(message: $viewModel.errorMessage, isError: true)
        .task {
            await viewModel.loadDomains()
        }
    }

    @ViewBuilder
    private var uploadArea: some View {
        #if os(macOS)
        DropZoneView(isLoading: viewModel.isLoading, progress: viewModel.uploadProgress) { urls in
            Task {
                for url in urls {
                    guard let data = try? Data(contentsOf: url) else { continue }
                    let _ = await viewModel.uploadFile(
                        data: data,
                        filename: url.lastPathComponent,
                        context: modelContext
                    )
                }
            }
        } onTap: {
            showFilePicker = true
        }
        #else
        VStack(spacing: 12) {
            if viewModel.isLoading {
                ProgressView(value: viewModel.uploadProgress) {
                    Text(String(localized: "Uploading..."))
                }
                .padding()
            } else {
                HStack(spacing: 16) {
                    Button(action: { showFilePicker = true }) {
                        Label(String(localized: "Choose File"), systemImage: "doc")
                    }
                    .buttonStyle(.borderedProminent)

                    Button(action: { showPhotoPicker = true }) {
                        Label(String(localized: "Choose Photo"), systemImage: "photo")
                    }
                    .buttonStyle(.bordered)

                    Button(action: handlePasteUpload) {
                        Label(String(localized: "Paste"), systemImage: "doc.on.clipboard")
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color.secondary.opacity(0.05), in: RoundedRectangle(cornerRadius: 12))
        #endif
    }

    private func handleFilePicker(_ result: Result<[URL], Error>) {
        guard case .success(let urls) = result, !urls.isEmpty else { return }
        Task {
            for url in urls {
                guard url.startAccessingSecurityScopedResource() else { continue }
                defer { url.stopAccessingSecurityScopedResource() }
                guard let data = try? Data(contentsOf: url) else { continue }
                let _ = await viewModel.uploadFile(
                    data: data,
                    filename: url.lastPathComponent,
                    context: modelContext
                )
            }
        }
    }

    private func handlePasteUpload() {
        guard let imageData = ImageConverter.clipboardImageData() else {
            viewModel.errorMessage = String(localized: "No image found in clipboard")
            return
        }
        Task {
            let (data, filename) = convertForUpload(imageData)
            let _ = await viewModel.uploadFile(data: data, filename: filename, context: modelContext)
        }
    }

    /// Convert image data to user's preferred format (WebP or PNG), with fallback.
    private func convertForUpload(_ imageData: Data) -> (Data, String) {
        let preferred = ImageConverter.preferredFormat

        switch preferred {
        case .webp:
            if let webpData = ImageConverter.toWebPLossless(data: imageData) {
                return (webpData, ImageConverter.pasteFilename(format: .webp))
            }
            // Fallback to PNG
            if let pngData = ImageConverter.toPNG(data: imageData) {
                return (pngData, ImageConverter.pasteFilename(format: .png))
            }
            return (imageData, ImageConverter.pasteFilename(format: .png))

        case .png:
            if let pngData = ImageConverter.toPNG(data: imageData) {
                return (pngData, ImageConverter.pasteFilename(format: .png))
            }
            return (imageData, ImageConverter.pasteFilename(format: .png))
        }
    }

    #if os(iOS)
    private func handlePhotoPicker(_ item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self) else { return }
            let (converted, filename) = convertForUpload(data)
            let _ = await viewModel.uploadFile(data: converted, filename: filename, context: modelContext)
            selectedPhoto = nil
        }
    }
    #endif
}

// MARK: - macOS Drop Zone

#if os(macOS)
struct DropZoneView: View {
    let isLoading: Bool
    let progress: Double
    let onDrop: ([URL]) -> Void
    let onTap: () -> Void
    @State private var isTargeted = false

    var body: some View {
        VStack(spacing: 12) {
            if isLoading {
                ProgressView(value: progress) {
                    Text(String(localized: "Uploading... \(Int(progress * 100))%"))
                }
            } else {
                Image(systemName: "arrow.up.doc")
                    .font(.system(size: 32))
                    .foregroundStyle(.secondary)
                Text(String(localized: "Drop files here, click to browse, or ⌘V to paste"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 120)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .stroke(style: StrokeStyle(lineWidth: 2, dash: [8]))
                .foregroundStyle(isTargeted ? Color.accentColor : .secondary.opacity(0.3))
        )
        .background(isTargeted ? Color.accentColor.opacity(0.05) : Color.clear, in: RoundedRectangle(cornerRadius: 12))
        .onTapGesture { onTap() }
        .dropDestination(for: URL.self) { urls, _ in
            onDrop(urls)
            return true
        } isTargeted: { targeted in
            isTargeted = targeted
        }
    }
}
#endif

// MARK: - Uploaded File Row

struct UploadedFileRow: View {
    let file: UploadedFile
    let linkDisplayType: LinkDisplayType
    let onDelete: () -> Void

    private var sizeInfo: String {
        var parts = [file.size.formattedFileSize]
        if let w = file.width, let h = file.height {
            parts.append("\(w)x\(h)")
        }
        return parts.joined(separator: " · ")
    }

    private var displayedLink: String {
        linkDisplayType.formatted(file: file)
    }

    private var isClickableURL: Bool {
        linkDisplayType == .directLink || linkDisplayType == .sharePage
    }

    var body: some View {
        HStack(spacing: 12) {
            // Thumbnail
            if file.isImage {
                CachedThumbnailView(originalURL: file.url, size: 40)
            } else {
                Image(systemName: "doc.fill")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                    .frame(width: 40, height: 40)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(file.filename)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                    Spacer()
                    Text(file.createdAt.relativeFormatted)
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }

                HStack {
                    if isClickableURL, let url = URL(string: displayedLink) {
                        Link(displayedLink, destination: url)
                            .font(.caption)
                            .lineLimit(1)
                    } else {
                        Text(displayedLink)
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .textSelection(.enabled)
                    }

                    CopyButton(text: displayedLink)

                    Spacer()

                    Text(sizeInfo)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
        .contextMenu {
            Button(String(localized: "Direct Link")) {
                ClipboardService.copy(file.url)
            }

            Button(String(localized: "Share Page")) {
                ClipboardService.copy(file.page)
            }

            Divider()

            Menu("BBCode") {
                Button("BBCode") {
                    ClipboardService.copy(
                        LinkFormatter.bbcode(filename: file.filename, directURL: file.url)
                    )
                }
                Button("BBCode w/ Link") {
                    ClipboardService.copy(
                        LinkFormatter.bbcodeWithLink(filename: file.filename, pageURL: file.page, directURL: file.url)
                    )
                }
                Button("BBCode w/ Direct Link") {
                    ClipboardService.copy(
                        LinkFormatter.bbcodeDirectLink(filename: file.filename, directURL: file.url)
                    )
                }
            }

            Menu("HTML") {
                Button("HTML") {
                    ClipboardService.copy(
                        LinkFormatter.html(filename: file.filename, directURL: file.url)
                    )
                }
                Button("HTML w/ Link") {
                    ClipboardService.copy(
                        LinkFormatter.htmlWithLink(filename: file.filename, pageURL: file.page, directURL: file.url)
                    )
                }
                Button("HTML w/ Direct Link") {
                    ClipboardService.copy(
                        LinkFormatter.htmlDirectLink(filename: file.filename, directURL: file.url)
                    )
                }
            }

            Menu("Markdown") {
                Button("Markdown") {
                    ClipboardService.copy(
                        LinkFormatter.markdown(filename: file.filename, directURL: file.url)
                    )
                }
            }

            Divider()

            Button(String(localized: "Open in Browser")) {
                if let url = URL(string: file.page) {
                    #if os(macOS)
                    NSWorkspace.shared.open(url)
                    #else
                    UIApplication.shared.open(url)
                    #endif
                }
            }

            Divider()

            Button(String(localized: "Delete"), role: .destructive) { onDelete() }
        }
    }
}

// MARK: - Batch Links View

struct BatchLinksView: View {
    let files: [UploadedFile]
    let linkDisplayType: LinkDisplayType
    let onDismiss: () -> Void
    @State private var batchType: LinkDisplayType
    @State private var copied = false

    init(files: [UploadedFile], linkDisplayType: LinkDisplayType, onDismiss: @escaping () -> Void) {
        self.files = files
        self.linkDisplayType = linkDisplayType
        self.onDismiss = onDismiss
        self._batchType = State(initialValue: linkDisplayType)
    }

    private var batchText: String {
        files.map { batchType.formatted(file: $0) }.joined(separator: "\n")
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text(String(localized: "Batch Copy Links"))
                    .font(.headline)
                Spacer()
                Text(String(localized: "\(files.count) files selected"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding()

            Divider()

            // Format picker
            HStack {
                Text(String(localized: "Format"))
                    .font(.subheadline.weight(.medium))
                Picker("", selection: $batchType) {
                    ForEach(LinkDisplayType.allCases) { type in
                        Text(type.rawValue).tag(type)
                    }
                }
                .labelsHidden()
            }
            .padding(.horizontal)
            .padding(.vertical, 8)

            // Links preview
            ScrollView {
                Text(batchText)
                    .font(.caption.monospaced())
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
            }
            .background(Color.secondary.opacity(0.05))

            Divider()

            // Actions
            HStack {
                Button(String(localized: "Clear Selection"), role: .destructive) {
                    onDismiss()
                }

                Spacer()

                Button(action: {
                    ClipboardService.copy(batchText)
                    copied = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        copied = false
                    }
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: copied ? "checkmark" : "doc.on.doc")
                            .contentTransition(.symbolEffect(.replace))
                        Text(copied ? String(localized: "Copied!") : String(localized: "Copy All"))
                    }
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
        }
        .frame(minWidth: 500, minHeight: 400)
    }
}
