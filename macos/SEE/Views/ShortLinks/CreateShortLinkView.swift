import SwiftUI
import SwiftData

struct CreateShortLinkView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Bindable var viewModel: ShortLinkViewModel
    var editingLink: ShortLink?

    private var isEditing: Bool { editingLink != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(String(localized: "Target URL"), text: $viewModel.targetURL)
                        .textFieldStyle(.roundedBorder)
                        #if os(iOS)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        #endif

                    if !viewModel.targetURL.isEmpty && !viewModel.targetURL.isValidURL {
                        Text(String(localized: "Please enter a valid URL"))
                            .font(.caption)
                            .foregroundStyle(.red)
                    }

                    if !viewModel.domains.isEmpty {
                        DomainPicker(
                            title: String(localized: "Domain"),
                            selection: $viewModel.selectedDomain,
                            domains: viewModel.domains
                        )
                    }

                    TextField(String(localized: "Custom Slug (optional)"), text: $viewModel.customSlug)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isEditing)

                    TextField(String(localized: "Title (optional)"), text: $viewModel.title)
                        .textFieldStyle(.roundedBorder)
                } header: {
                    Text(String(localized: "Link Details"))
                }

                if !isEditing {
                    Section {
                        SecureField(String(localized: "Password (optional)"), text: $viewModel.password)
                            .textFieldStyle(.roundedBorder)
                    } header: {
                        Text(String(localized: "Protection"))
                    }

                    Section {
                        Toggle(String(localized: "Set Expiration"), isOn: $viewModel.enableExpiry)
                        if viewModel.enableExpiry {
                            DatePicker(
                                String(localized: "Expire At"),
                                selection: Binding(
                                    get: { viewModel.expireAt ?? Date().addingTimeInterval(86400) },
                                    set: { viewModel.expireAt = $0 }
                                ),
                                in: Date()...,
                                displayedComponents: [.date, .hourAndMinute]
                            )

                            TextField(
                                String(localized: "Redirect URL after expiry (optional)"),
                                text: $viewModel.expirationRedirectURL
                            )
                            .textFieldStyle(.roundedBorder)
                        }
                    } header: {
                        Text(String(localized: "Expiration"))
                    }

                    if !viewModel.tags.isEmpty {
                        Section {
                            TagSelector(tags: viewModel.tags, selectedTagIDs: $viewModel.selectedTagIDs)
                        } header: {
                            Text(String(localized: "Tags"))
                        }
                    }
                }
            }
            .formStyle(.grouped)
            .navigationTitle(isEditing ? String(localized: "Edit Short Link") : String(localized: "New Short Link"))
            #if os(macOS)
            .frame(minWidth: 450, minHeight: 400)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { dismiss() }
                        .keyboardShortcut(.cancelAction)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? String(localized: "Save") : String(localized: "Create")) {
                        Task {
                            let success: Bool
                            if let link = editingLink {
                                success = await viewModel.updateShortLink(link, context: modelContext)
                            } else {
                                success = await viewModel.createShortLink(context: modelContext)
                            }
                            if success { dismiss() }
                        }
                    }
                    .disabled(viewModel.targetURL.isEmpty || viewModel.isLoading)
                    .keyboardShortcut(.defaultAction)
                }
            }
            .loadingOverlay(viewModel.isLoading)
            .toast(message: $viewModel.successMessage)
            .toast(message: $viewModel.errorMessage, isError: true)
        }
        .task {
            if isEditing, let link = editingLink {
                viewModel.populateForm(from: link)
            }
            await viewModel.loadDomains()
            await viewModel.loadTags()
        }
    }
}
