import SwiftUI
import SwiftData

struct ShortLinkListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ShortLink.createdAt, order: .reverse) private var links: [ShortLink]
    @State private var viewModel = ShortLinkViewModel()
    @State private var showingCreate = false
    @State private var searchText = ""
    @State private var linkToEdit: ShortLink?
    @State private var linkToDelete: ShortLink?
    @State private var currentPage = 1

    private var filteredLinks: [ShortLink] {
        if searchText.isEmpty { return links }
        let query = searchText.lowercased()
        return links.filter {
            $0.slug.lowercased().contains(query) ||
            $0.targetURL.lowercased().contains(query) ||
            $0.title.lowercased().contains(query) ||
            $0.domain.lowercased().contains(query)
        }
    }

    private var totalPages: Int { Pagination.totalPages(for: filteredLinks.count) }
    private var pagedLinks: [ShortLink] { Pagination.page(filteredLinks, page: currentPage) }

    var body: some View {
        Group {
            if links.isEmpty {
                EmptyStateView(
                    icon: "link",
                    title: String(localized: "No Short Links"),
                    message: String(localized: "Create your first short link to get started."),
                    buttonTitle: String(localized: "Create Short Link"),
                    action: { showingCreate = true }
                )
            } else {
                List {
                    ForEach(pagedLinks) { link in
                        ShortLinkRow(link: link) {
                            linkToEdit = link
                        } onDelete: {
                            linkToDelete = link
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
                .searchable(text: $searchText, prompt: String(localized: "Search links"))
                .onChange(of: searchText) { currentPage = 1 }
            }
        }
        .navigationTitle(String(localized: "Short Links"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { showingCreate = true }) {
                    Label(String(localized: "New Link"), systemImage: "plus")
                }
                .keyboardShortcut("n", modifiers: .command)
            }
        }
        .sheet(isPresented: $showingCreate) {
            CreateShortLinkView(viewModel: viewModel)
        }
        .sheet(item: $linkToEdit) { link in
            CreateShortLinkView(viewModel: viewModel, editingLink: link)
        }
        .alert(String(localized: "Delete Short Link?"), isPresented: .init(
            get: { linkToDelete != nil },
            set: { if !$0 { linkToDelete = nil } }
        )) {
            Button(String(localized: "Cancel"), role: .cancel) {}
            Button(String(localized: "Delete"), role: .destructive) {
                if let link = linkToDelete {
                    Task {
                        let _ = await viewModel.deleteShortLink(link, context: modelContext)
                    }
                }
            }
        } message: {
            Text(String(localized: "This action cannot be undone."))
        }
        .toast(message: $viewModel.successMessage)
        .toast(message: $viewModel.errorMessage, isError: true)
        .onReceive(NotificationCenter.default.publisher(for: .createShortLink)) { _ in
            showingCreate = true
        }
    }
}

// MARK: - Row

struct ShortLinkRow: View {
    let link: ShortLink
    let onEdit: () -> Void
    let onDelete: () -> Void
    @State private var showStats = false

    var body: some View {
        LinkRowView(
            shortURL: link.shortURL,
            title: link.title,
            subtitle: link.targetURL,
            date: link.createdAt,
            onCopy: { ClipboardService.copy(link.shortURL) },
            onEdit: onEdit,
            onDelete: onDelete,
            extraMenuItems: {
                AnyView(Group {
                    Button(String(localized: "Copy Target URL")) {
                        ClipboardService.copy(link.targetURL)
                    }
                    Divider()
                    Button(String(localized: "View Statistics")) {
                        showStats = true
                    }
                })
            }
        )
        .sheet(isPresented: $showStats) {
            LinkStatsView(link: link)
        }
    }
}
