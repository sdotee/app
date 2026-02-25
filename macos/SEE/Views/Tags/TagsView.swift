import SwiftUI

struct TagsView: View {
    @State private var viewModel = TagsViewModel()

    var body: some View {
        Group {
            if viewModel.tags.isEmpty && !viewModel.isLoading {
                EmptyStateView(
                    icon: "tag",
                    title: String(localized: "No Tags"),
                    message: String(localized: "Tags will appear here once created on the server."),
                    buttonTitle: String(localized: "Refresh"),
                    action: { Task { await viewModel.loadTags() } }
                )
            } else {
                List {
                    ForEach(viewModel.tags) { tag in
                        Label(tag.name, systemImage: "tag")
                    }
                }
            }
        }
        .navigationTitle(String(localized: "Tags"))
        .loadingOverlay(viewModel.isLoading)
        .toolbar {
            #if os(macOS)
            ToolbarItem(placement: .automatic) {
                Button(action: { Task { await viewModel.loadTags() } }) {
                    Label(String(localized: "Refresh"), systemImage: "arrow.clockwise")
                }
            }
            #endif
        }
        #if os(iOS)
        .refreshable {
            await viewModel.loadTags()
        }
        #endif
        .task {
            await viewModel.loadTags()
        }
    }
}
