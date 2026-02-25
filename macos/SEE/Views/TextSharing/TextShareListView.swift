import SwiftUI
import SwiftData

struct TextShareListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \TextShare.createdAt, order: .reverse) private var shares: [TextShare]
    @State private var viewModel = TextShareViewModel()
    @State private var showingCreate = false
    @State private var searchText = ""
    @State private var shareToEdit: TextShare?
    @State private var shareToDelete: TextShare?
    @State private var currentPage = 1

    private var filteredShares: [TextShare] {
        if searchText.isEmpty { return shares }
        let query = searchText.lowercased()
        return shares.filter {
            $0.title.lowercased().contains(query) ||
            $0.content.lowercased().contains(query) ||
            $0.slug.lowercased().contains(query)
        }
    }

    private var totalPages: Int { Pagination.totalPages(for: filteredShares.count) }
    private var pagedShares: [TextShare] { Pagination.page(filteredShares, page: currentPage) }

    var body: some View {
        Group {
            if shares.isEmpty {
                EmptyStateView(
                    icon: "doc.text",
                    title: String(localized: "No Text Shares"),
                    message: String(localized: "Share text, code, or markdown with a link."),
                    buttonTitle: String(localized: "Create Text Share"),
                    action: { showingCreate = true }
                )
            } else {
                List {
                    ForEach(pagedShares) { share in
                        TextShareRow(share: share) {
                            shareToEdit = share
                        } onDelete: {
                            shareToDelete = share
                        }
                    }

                    if totalPages > 1 {
                        PaginationView(currentPage: currentPage, totalPages: totalPages) { page in
                            currentPage = page
                        }
                        .frame(maxWidth: .infinity)
                        .listRowSeparator(.hidden)
                    }
                }
                .searchable(text: $searchText, prompt: String(localized: "Search text shares"))
                .onChange(of: searchText) { currentPage = 1 }
            }
        }
        .navigationTitle(String(localized: "Text Sharing"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { showingCreate = true }) {
                    Label(String(localized: "New Text"), systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $showingCreate) {
            CreateTextShareView(viewModel: viewModel)
        }
        .sheet(item: $shareToEdit) { share in
            CreateTextShareView(viewModel: viewModel, editingShare: share)
        }
        .alert(String(localized: "Delete Text Share?"), isPresented: .init(
            get: { shareToDelete != nil },
            set: { if !$0 { shareToDelete = nil } }
        )) {
            Button(String(localized: "Cancel"), role: .cancel) {}
            Button(String(localized: "Delete"), role: .destructive) {
                if let share = shareToDelete {
                    Task {
                        let _ = await viewModel.deleteTextShare(share, context: modelContext)
                    }
                }
            }
        } message: {
            Text(String(localized: "This action cannot be undone."))
        }
        .toast(message: $viewModel.successMessage)
        .toast(message: $viewModel.errorMessage, isError: true)
        .onReceive(NotificationCenter.default.publisher(for: .createTextShare)) { _ in
            showingCreate = true
        }
    }
}

// MARK: - Row

struct TextShareRow: View {
    let share: TextShare
    let onEdit: () -> Void
    let onDelete: () -> Void

    private var badgeText: String {
        switch share.textType {
        case TextType.sourceCode.rawValue: "Code"
        case TextType.markdown.rawValue: "Markdown"
        default: "Text"
        }
    }

    var body: some View {
        LinkRowView(
            shortURL: share.shortURL,
            title: share.title,
            subtitle: share.content,
            badge: badgeText,
            date: share.createdAt,
            onCopy: { ClipboardService.copy(share.shortURL) },
            onEdit: onEdit,
            onDelete: onDelete
        )
    }
}
